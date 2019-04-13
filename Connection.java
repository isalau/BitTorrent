import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.net.InetAddress; //for hostname
import java.net.UnknownHostException; //for hostname

public class Connection extends Uploader implements Runnable{
	/**********************variables***********************/
	
	//If I am peer A this is peer B's info
	public  int peerID;
	public  String hostname;
	public  int portNumber;
	public  boolean hasFile;

	public boolean interested;
	public boolean preferredNeighbor;
	public boolean optimisticNeighbor;
	public byte[] peerBitfield; 

	//My Info 
	public static int sendersPeerID;
	public static String sendersHostName;
	public static int sendersPort;
	public static boolean sendersHasFile;
	public static byte[] myBitfield;

	public static boolean alone = true; 
	public static int fileSize;
    public static int pieceSize;
    public static int numOfPieces; 
    public static int numInPeerInfo;
    public static int unchokingInterval;
    public static int optimisticUnchokingInterval;
    public static ArrayList<byte[]> DataChunks;
    

	private byte[] message; 
	private byte[] bitfieldMessage; 
	private byte[] interestedMessage;
	private byte[] notInterestedMessage;
	private byte[] requestMessage;
	private byte[] haveMessage;    
	private byte[] chokeMessage;
	private byte[] unChokeMessage;
	private byte[] pieceMessage;
	private int PieceIndex;

	public static  LinkedList<Connection> connectionLinkedList = new LinkedList<Connection>();
	public static  LinkedList<Peer> peerLinkedList = new LinkedList<Peer>();

    public Socket connection;
    private ObjectInputStream in;	//stream read from the socket
    private ObjectOutputStream out;    //stream write to the socket
	public int no;		//The index number of the client
	public int chunksDownloaded; 
	public long connectionDownloadRate;
	public long startDownloadTime;
	public long stopDownloadTime;

	
	//handshake variables
	public static final int zerobits_size = 10;
	public static final int peerID_size = 4;
	public static final int header_size = 18;
	public static final int total_length = 32;
	public static boolean sentHandshake = false;
	public static boolean receivedHandshake = false;

	/********************** constructor ***********************/

	public void Connection (){}

	/********************** functions ***********************/
	
	//run establishes individual connection
	@Override
  	public void run() {
  		System.out.println("Connection: I am running");
  		if (alone == false){
  			System.out.println("Connection: I am not alone");
  			sendHandShake();
  		}
  		else{
			try{
				InetAddress myID = InetAddress.getLocalHost();
				String iP = myID.toString();
            	String myHostname = myID.getHostName();

				//initialize Input and Output streams
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush();
				in = new ObjectInputStream(connection.getInputStream());
			try{
				while(true){
					//receive the message sent from the client
					byte[] myObjects = (byte[])in.readObject();
					
					//show the message to the user
				    // String objectMessage = new String (myObjects);
				    // System.out.println("Message: " + objectMessage);

					//check what message you got
					checkMessage(myObjects);
				}
			}catch(ClassNotFoundException classnot){
				System.err.println("Data received in unknown format");
			}
			}catch(IOException ioException){
				System.out.println("Disconnect with Client " + no);
			}
		}

		// finally{
		// 	//Close connections
		// 	try{
		// 		if(in != null){
		// 			 in.close();
		// 		}
		// 		if(out != null){
		// 			 out.close();
		// 		}
		// 		if (connection != null){
		// 			 connection.close();
		// 		}	
		// 	}catch(IOException ioException){
		// 		System.out.println("Disconnect with Client " + no);
		// 	}
		// }
	}

	//send a message to the output stream
	public void sendMessage(byte[] msg){
		System.out.println("Connection: sending message: " + msg + " to Client " +no + " on port: "+ portNumber + " at addres: "+ hostname );
 		if(DataChunks != null){
 			System.out.println("My data chunks are of size:"+ DataChunks.size());
 		}
 		
 		
		try{
			//initialize Input and Output streams
				// out = new ObjectOutputStream(connection.getOutputStream());
				// out.flush();				
			out.writeObject(msg);
			out.flush();
			System.out.println("Connection: message sent");
		}
		catch(IOException ioException){
			ioException.printStackTrace();
		}
	}

	//check message type
	public void checkMessage(byte[] msg){
		byte messageValue = msg[4];
		System.out.println("Connection: message type: " + messageValue + " received from client");
		String message = new String (msg);
		
		while(true){
			switch (messageValue) {
		        case 0:
		        	System.out.println("Connection: received choke message");
		            
		            break;
		        case 1:
		           	//received an unchoke message
		        	System.out.println("Connection: received unchoke message");
		        	// create request 
	        		sendRequest();
		            break;
		        case 2:
		        	System.out.println("Connection: received interested message");
		        	receivedInterseted();
		            break;
		        case 3:
		            System.out.println("Connection: received not interested message");
		            receivedNotInterseted();
		            break;
		        case 4:
		            System.out.println("Connection: received have message");
					determineIfInterestedFromHave(msg);
		            break;
		        case 5:
		        	System.out.println("Connection: received bitfield message");
		        	System.out.println("Connection: bitfield: " +  message + " received from client");
		            determineIfInterestedFromBitfield(msg);
		            break;
		        case 6:
		            //got a requestMessage
		        	System.out.println("Connection: received request message");
		        	//create piece
		        	PieceIndex = (int) msg[5];
		        	sendPiece();
		            break;
		        case 7:
		        	System.out.println("Connection: received piece message");
		            receivedPiece();
		            for(int i =0; i < connectionLinkedList.size(); i++){
		            	if(connectionLinkedList.get(i).preferredNeighbor == true){
		            		sendHave();
		            	}
		            }
					stopDownloadTime = System.currentTimeMillis();
		            break;
		       	case 73:
		           	System.out.println("Connection: received handshake message");
		           	//check if we sent our handshake? 
		           	if (sentHandshake == false){
		           		receivedHandshake = true;
		           		sendHandShake();
		           		// listenerSendHandshake();
		           	}
		           	
		            break;
		        default:
		            System.out.println("Connection: Not a valid type");
		            break;
		        }
				return;
		}
	}

	public void sendHandShake(){
		sentHandshake = true;	

		String handshake_zerobits = "0000000000";
		String handshake_header = "P2PFILESHARINGPROJ";

		try{
			try{
				if (alone == false && receivedHandshake == false){
					connection = new Socket(hostname, portNumber);
				}
			}  catch (IllegalArgumentException exception) {
	            // Catch expected IllegalArgumentExceptions.
	            System.out.println("Connection: Hanshake exception 1: " + exception);
	        } catch (Exception exception) {
	            // Catch unexpected Exceptions.
	            System.out.println("Connection: Hanshake exception 2: " + exception);
	        }
	        if (alone == false){
				out = new ObjectOutputStream(connection.getOutputStream());
				out.flush(); //TODO ::: Do we need this?
				in = new ObjectInputStream(connection.getInputStream());
			}else{
				//need to add to peer and connection list
				addPeers();
			}
			
			sendersPort = connection.getLocalPort();
			sendersHostName = connection.getLocalAddress().toString();
			System.out.println("Connection: Sending Handshake from Connection to : " + hostname + " with port number "+ portNumber);
			System.out.println("Connection: My peerID is: " + sendersPeerID);
			System.out.println("Connection: My port number is: " + sendersPort);
			System.out.println("Connection: My hostname is: " + sendersHostName);
		
			//handshake
			message = new byte[32];
			byte[] peerIDArray = ByteBuffer.allocate(4).putInt(sendersPeerID).array();
			
			System.arraycopy(handshake_header.getBytes(), 0, message,0, header_size);
			// System.out.println("handshake_header: "+ handshake_header);
			try {
		         String Str2 = new String(handshake_header.getBytes( "UTF-8" ));
		         // System.out.println("handshake_header Value: " + Str2 );
		         Str2 = new String (message);
		         // System.out.println("Message: " + Str2 );
		    } catch ( UnsupportedEncodingException e) {
		         System.out.println("Unsupported character set");
		    }

			
			System.arraycopy(handshake_zerobits.getBytes(), 0, message,header_size, zerobits_size);
			// System.out.println("handshake_zerobits: "+ handshake_zerobits);
			try {
		         String Str3 = new String(handshake_zerobits.getBytes( "UTF-8" ));
		         // System.out.println("handshake_zerobits Value: " + Str3 );
		         Str3 = new String (message);
		         // System.out.println("Message " + Str3 );
		    } catch ( UnsupportedEncodingException e) {
		         System.out.println("Unsupported character set");
		    }
			
			String peerIDString = Integer.toString(sendersPeerID); 
			System.arraycopy(peerIDString.getBytes(), 0, message, header_size+zerobits_size, peerID_size);
			try {
		         String Str4 = new String(peerIDString.getBytes( "UTF-8" ));
		         // System.out.println("peerIDString Value: " + Str4 );
		         Str4 = new String (message);
		         // System.out.println("Message: " + Str4 );
		    } catch ( UnsupportedEncodingException e) {
		        System.out.println("Unsupported character set");
		    }

		    //send our messages
		    out.writeObject(message);
			out.flush();
		}catch(IOException ioException){
			System.out.println("Could not send handshake 1: "+ ioException);
		}

		sendBitfield();

		try{
			while(true){
				//receive the message sent from the client
				byte[] myObjects = (byte[])in.readObject();
				
				//check what message you got
				checkMessage(myObjects);
			}
		}catch(ClassNotFoundException classnot){
				System.err.println("Connection: Data received in unknown format");
			}catch(IOException ioException){
				System.out.println("Connection: Disconnect with Client " + no);
			}
		// finally{
		// 	//Close connections
		// 	try{
		// 		if(in != null){
		// 			 in.close();
		// 		}
		// 		if(out != null){
		// 			 out.close();
		// 		}
		// 		if (connection != null){
		// 			 connection.close();
		// 		}	
		//  }catch(IOException ioException){
		// 	System.out.println("Could not send handshake 2:"+ ioException);
		// 	 }
		// }
	}

	public void addPeers(){
		//place all info in a peer object
    	Peer newPeer = new Peer();
        newPeer.peerID = peerID; 
        newPeer.hostName = hostname; 
        newPeer.port = portNumber;            
        newPeer.hasFile =  false; //assume false until proven wrong by receiving bitfield or have message 
        newPeer.interested = false; 
		newPeer.preferredNeighbor = false;
		newPeer.optimisticNeighbor = false;

		//assume empyty bitfield until proven wrong by receiving bitfield or have message 
		byte[] emptyArray = new byte[numOfPieces];
		newPeer.bitfield = emptyArray;
        
        peerLinkedList.add(newPeer);

     	System.out.println("Connection: Adding Peers " + peerLinkedList);
	}

	public void sendBitfield(){
		//create payload
		//determine number of pieces from common.cfg
    	System.out.println("Connection: In sendBitfield.");

		//determine what parts of the file I have 
		int length = 4 + 1 + (numOfPieces/8); 
    	bitfieldMessage = new byte[length];
    	
		//create new bitfield message
		bitfieldMessage = ByteBuffer.allocate(length).putInt(length).array();
		bitfieldMessage[4] = 5;

    	if(sendersHasFile == true){
    		for (int i = 5; i< length; i++){
    			bitfieldMessage[i] =  1;
    			//send message to B
    		}
    		System.out.println("Connection: Sending Bitfield with " + numOfPieces + " pieces.");
    		sendMessage(bitfieldMessage);
    	}
	}

	public void receivedInterseted(){
		//update Peer to reflect that it is intersted in Peer List and Connection List
		
		//just to test
		for (int i = 0; i < peerLinkedList.size(); i++){
			System.out.println("Peer "+ peerLinkedList.get(i).peerID+ " is interested: "+ peerLinkedList.get(i).interested); 
		}

		//Peer List 
		for (int i =0; i < peerLinkedList.size(); i++){
			if( sendersPeerID == peerLinkedList.get(i).peerID){
				peerLinkedList.get(i).interested = true; 
			}
		}

		//just to test
		for (int i =0; i < peerLinkedList.size(); i++){
				System.out.println("Peer "+ peerLinkedList.get(i).peerID+ " is interested: "+ peerLinkedList.get(i).interested); 
		}
	}

	public void receivedNotInterseted(){
		//update Peer to reflect that it is intersted in Peer List and Connection List
		
		//just to test
		for (int i = 0; i < peerLinkedList.size(); i++){
			System.out.println("Peer "+ peerLinkedList.get(i).peerID+ " is interested: "+ peerLinkedList.get(i).interested); 
		}

		//Peer List 
		for (int i =0; i < peerLinkedList.size(); i++){
			if( sendersPeerID == peerLinkedList.get(i).peerID){
				peerLinkedList.get(i).interested = false; 
			}
		}

		//just to test
		for (int i =0; i < peerLinkedList.size(); i++){
				System.out.println("Peer "+ peerLinkedList.get(i).peerID+ " is interested: "+ peerLinkedList.get(i).interested); 
		}
	}

	public void sendChokeMessage(){
		System.out.println("Connection: Sending Choke Message");

		//create new bitfield message
		int length = 5;
		chokeMessage = new byte[length];
	
	 	//initalize
		chokeMessage = ByteBuffer.allocate(length).putInt(length).array();
		chokeMessage[4] = 0;

		sendMessage(chokeMessage);
	}

	public void sendUnchokeMessage(){
		System.out.println("Connection: Sending UnChoke Message");

		//create new bitfield message
		int length = 5;
		unChokeMessage = new byte[length];
	
	 	//initalize
		unChokeMessage = ByteBuffer.allocate(length).putInt(length).array();
		unChokeMessage[4] = 1;

		sendMessage(unChokeMessage);
	}

	public void determineIfInterestedFromHave(byte[] msg){
		System.out.println("Connection: Determining If Interested From Have");
		
		byte index = msg[5];
		boolean sendIntMes = false;
		
		//my bitfield 
		if(myBitfield[index] != 1){
			sendIntMes = true;  
		}
		//if B has 1 where I have 0 sendInterestedMessage()
		if(sendIntMes == true){
			//if B has 1 where I have 0 sendInterestedMessage()
			sendInterestedMessage();
		}else{
			//else send sendNotInterestedMessage()
			sendNotInterestedMessage();
		}
	}

	public void determineIfInterestedFromBitfield(byte[] msg){
		System.out.println("Connection: Determining If Interested From Bitfield");
		//determine if a neighbor has an interesting piece
		boolean interested = false;
		//compare our bitfields
		//msg will contain the other peer's bitfield
		for (int i = 5; i< msg.length; i++){
			// System.out.println("Bitfield: " + msg[i] + " from client " + no);
			//if my bitfield index == 0 && msg == 1
			int bitIndex=i-5; 
			peerBitfield[bitIndex] = msg[i];
			if(myBitfield[bitIndex] == 0 && msg[i] == 1){
				interested = true;  
			}
		}
			//if B has 1 where I have 0 sendInterestedMessage()
		if(interested == true){
			//if B has 1 where I have 0 sendInterestedMessage()
			sendInterestedMessage();
		}else{
			//else send sendNotInterestedMessage()
			sendNotInterestedMessage();
		}
	}

	public void sendInterestedMessage(){
		System.out.println("Connection: Sending Interested Message");

		//create new bitfield message
		int length = 5;
		interestedMessage = new byte[length];
	
	 	//initalize
		interestedMessage = ByteBuffer.allocate(length).putInt(length).array();
		interestedMessage[4] = 2;

		sendMessage(interestedMessage);
	}

	public void sendNotInterestedMessage(){
		System.out.println("Connection: Sending Interested Message");

		//create new bitfield message
		int length = 5;
		notInterestedMessage = new byte[length];
	
	 	//initalize
		notInterestedMessage = ByteBuffer.allocate(length).putInt(length).array();
		notInterestedMessage[4] = 3;

		sendMessage(notInterestedMessage);
	}

	public void sendRequest(){
		System.out.println("Connection: Sending Request Message");
		if(hasFile == false){
			//create new request message
		int length = 9; //4 for length, 1 for type, 4 for payload
		requestMessage = new byte[length];
	
	 	//initalize
		requestMessage = ByteBuffer.allocate(length).putInt(length).array();
		requestMessage[4] = 6; //type six

		//create payload 
		//check for 0's in myBitfield array 1 means we have it and 2 means we sent a request to another neighbor for it

		// int requestPieceIndex = 0; 
		/*
		for (int i = 0 ; i < myBitfield.length; i++){
			if(myBitfield[i] == 0){
				//we should check that they have the piece too 
				requestPieceIndex = i;
				myBitfield[i] = 2; 
				break;
			}
		}*/ 


		int rand = selectRandom(); 

		myBitfield[rand] = 2; 
		requestMessage[5] = (byte) rand;
		
		sendMessage(requestMessage);

		//start timer 
		startDownloadTime = System.currentTimeMillis();
		}
	}

	public int selectRandom(){
		System.out.println("Connection: Number of pieces: " + numOfPieces + " "+ myBitfield.length +" "+  peerBitfield.length);
		int r = new Random().nextInt(numOfPieces-1);
		if(myBitfield[r] == 0 && peerBitfield[r] == 1){
			return r;
		}else{
			selectRandom(); //keep calling until you find one you don't have 
		}
		return 0; 
	}

	public void sendPiece(){
		System.out.println("Connection: Sending Piece Message");

		//create new piece message
		int length = 4 + 1 + pieceSize; //4 for length, 1 for type, rest for piece content
		pieceMessage = new byte[length];
		byte[] data = new byte[pieceSize];
	 	//initalize
		pieceMessage = ByteBuffer.allocate(length).putInt(length).array();
		pieceMessage[4] = 7; //type seven

		
		if((DataChunks != null ) && (myBitfield[PieceIndex] !=0)){
			data = DataChunks.get(PieceIndex);
		}
			
		System.arraycopy(data, 0, pieceMessage,0, data.length);
		
		sendMessage(pieceMessage);

		// //start timer 
		// startDownloadTime = System.currentTimeMillis();
	}
	public void receivedPiece(){
		//update Peer to reflect that they get the piece 
		
		//just to test
		for (int i = 0; i < peerLinkedList.size(); i++){
			System.out.println("Peer "+ peerLinkedList.get(i).peerID+ " recieved the piece message : "); 
		}

		//Peer List 
		myBitfield[PieceIndex] = 1;
	}
	public void sendHave(){
		System.out.println("Connection: Sending Have Message");

		//create new bitfield message
		int length = 4 + 1+ 4;
		haveMessage = new byte[length];

	 	//initalize
		haveMessage = ByteBuffer.allocate(length).putInt(length).array();
		haveMessage[4] = 4;

		haveMessage[5] = (byte) PieceIndex;
		System.out.println("I have the piece at index "+ PieceIndex );

		sendMessage(haveMessage);
	}

	public void downloadChunks(){
		//download somehow....
		//determine rate
		DetermineRate();
	}

	public long DetermineRate(){
		connectionDownloadRate =  connectionDownloadRate /(startDownloadTime - stopDownloadTime);
		return connectionDownloadRate;
	}
}		