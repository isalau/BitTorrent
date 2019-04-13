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

	//My Info 
	public static int sendersPeerID;
	public static String sendersHostName;
	public static int sendersPort;
	public static boolean sendersHasFile;

	public static boolean alone = true; 
	public static int fileSize;
    public static int pieceSize;
    public static int numInPeerInfo;
    public static int unchokingInterval;
    public static int optimisticUnchokingInterval;
    public static ArrayList<byte[]> DataChunks;
    

	public static byte[] myBitfield;
	public static byte [] peerBitfield; 
	private static byte[] message;  
	private static byte[] interestedMessage;
	private static byte[] notInterestedMessage;
	private static byte[] requestMessage;
	private static byte[] haveMessage;    
	private static byte[] chokeMessage;
	private static byte[] unChokeMessage;


	public static  LinkedList<Connection> connectionLinkedList = new LinkedList<Connection>();
	public static  LinkedList<Peer> peerLinkedList = new LinkedList<Peer>();

    public Socket connection;
    private ObjectInputStream in;	//stream read from the socket
    private ObjectOutputStream out;    //stream write to the socket
	public int no;		//The index number of the client

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
					determineIfInterestedFromHave();
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
		        	//sendPiece();
		            break;
		        case 7:
		        	System.out.println("Connection: received piece message");
		            //createRequestMessage();
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
	            System.out.println("Hanshake exception 1: " + exception);
	        } catch (Exception exception) {
	            // Catch unexpected Exceptions.
	            System.out.println("Hanshake exception 2: " + exception);
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
			System.out.println("My peerID is: " + sendersPeerID);
			System.out.println("My port number is: " + sendersPort);
			System.out.println("My hostname is: " + sendersHostName);
		
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
				System.err.println("Data received in unknown format");
			}catch(IOException ioException){
				System.out.println("Disconnect with Client " + no);
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
		newPeer.prefferedNeighbor = false;
		newPeer.optimisticNeighbor = false;

		//assume empyty bitfield until proven wrong by receiving bitfield or have message 
		byte[] emptyArray = new byte[32];
		newPeer.bitfield = emptyArray;
        
        peerLinkedList.add(newPeer);

     	System.out.println("Connection: Adding Peers " + peerLinkedList);

     	Connection newConnection2 = new Connection();
    	//their info 
    	newConnection2.peerID = peerID;
    	newConnection2.hostname = hostname;
    	newConnection2.portNumber = portNumber;
    	newConnection2.hasFile = hasFile;

    	//my info 
    	newConnection2.sendersPeerID = sendersPeerID;
    	newConnection2.sendersHostName = sendersHostName;
    	newConnection2.sendersPort = sendersPort; // this is currently listener will change
    	newConnection2.sendersHasFile = sendersHasFile;  // this is currently listener will change

    	newConnection2.alone = false;	
        newConnection2.fileSize = fileSize;
        newConnection2.pieceSize = pieceSize;
        newConnection2.unchokingInterval = unchokingInterval;
        newConnection2.optimisticUnchokingInterval = optimisticUnchokingInterval;

        newConnection2.numInPeerInfo = numInPeerInfo; 
        newConnection2.myBitfield = myBitfield;

		// connectionLinkedList.add(newConnection2);

		newConnection2.peerLinkedList = peerLinkedList;
		newConnection2.connectionLinkedList = connectionLinkedList;

     	// connectionLinkedList.add(newConnection2);
     	System.out.println("Connection: Adding Connections " + connectionLinkedList);
	}

	public void sendBitfield(){
		//create payload
		//determine number of pieces from common.cfg
    	int numOfPieces = (int) Math.ceil((double)fileSize/pieceSize);
    	System.out.println("Connection: In sendBitfield.");

		//determine what parts of the file I have 
		int length = 4 + 1 + (numOfPieces/8); 
    	myBitfield = new byte[length];
    	
		//create new bitfield message
		myBitfield = ByteBuffer.allocate(length).putInt(length).array();
		myBitfield[4] = 5;

    	if(sendersHasFile == true){
    		for (int i = 5; i< length; i++){
    			myBitfield[i] =  1;
    			//send message to B
    		}
    		System.out.println("Connection: Sending Bitfield with " + numOfPieces + " pieces.");
    		sendMessage(myBitfield);
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

	public void determineIfInterestedFromHave(){
		System.out.println("Connection: Determining If Interested From Have");
		//determine if a neighbor has an interesting piece
		//compare our bitfields
		boolean interested = false;
		//my bitfield 
		if(myBitfield[5] == 0){
			interested = true;  
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

	public void determineIfInterestedFromBitfield(byte[] msg){
		System.out.println("Connection: Determining If Interested From Bitfield");
		//determine if a neighbor has an interesting piece
		//compare our bitfields
		boolean interested = false;
		//my bitfield 
		//msg will contain the other peer's bitfield
		for (int i = 0; i< msg.length; i++){
			System.out.println("Bitfield: " + msg[i] + " from client " + no);
			//if my bitfield index == 0 && msg == 1
			if(myBitfield[i] == 0 && msg[i] == 1){
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

	public void sendHave(){
		System.out.println("Connection: Sending Have Message");

		//create new bitfield message
		int length = 5;
		haveMessage = new byte[length];
	
	 	//initalize
		haveMessage = ByteBuffer.allocate(length).putInt(length).array();
		haveMessage[4] = 3;

		sendMessage(haveMessage);
	}

	public void sendRequest(){
		System.out.println("Connection: Sending Request Message");

		//create new request message
		int length = 9; //4 for length, 1 for type, 4 for payload
		requestMessage = new byte[length];
	
	 	//initalize
		requestMessage = ByteBuffer.allocate(length).putInt(length).array();
		requestMessage[4] = 6; //type six

		//create payload 
		//check for 0's in myBitfield array 1 means we have it and 2 means we sent a request to another neighbor for it

		int requestPieceIndex = 0; 
		for (int i = 0 ; i < myBitfield.length; i++){
			if(myBitfield[i] == 0){
				//we should check that they have the piece too 
				requestPieceIndex = i;
				myBitfield[i] = 2; 
				break;
			}
		}

		requestMessage[5] = (byte) requestPieceIndex;
		
		sendMessage(requestMessage);
	}

	public void sendPiece(){
		System.out.println("Connection: Sending Piece Message");

		//create new piece message
		int length = 4+1 +pieceSize; //4 for length, 1 for type, 4 for payload
		pieceMessage = new byte[length];
	
	 	//initalize
		pieceMessage = ByteBuffer.allocate(length).putInt(length).array();
		pieceMessage[4] = 7; //type six

		//create payload 
		//check for 0's in myBitfield array 1 means we have it and 2 means we sent a request to another neighbor for it

		int requestPieceIndex = 0; 
		for (int i = 0 ; i < myBitfield.length; i++){
			if(myBitfield[i] == 0){
				//we should check that they have the piece too 
				requestPieceIndex = i;
				myBitfield[i] = 2; 
				break;
			}
		}

		requestMessage[5] = (byte) requestPieceIndex;
		
		sendMessage(requestMessage);
	}
}		