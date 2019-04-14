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
    public static String fileName; 
    public static int numInPeerInfo;
    public static int unchokingInterval;
    public static int optimisticUnchokingInterval;
    public static ArrayList<byte[]> DataChunks;
    public static DataFile dataFile;

	private byte[] message; 
	private byte[] bitfieldMessage; 
	private byte[] interestedMessage;
	private byte[] notInterestedMessage;
	private byte[] requestMessage;
	private byte[] haveMessage;    
	private byte[] chokeMessage;
	private byte[] unChokeMessage;
	private byte[] pieceMessage;
	private byte PieceIndex;

	public static  LinkedList<Connection> connectionLinkedList = new LinkedList<Connection>();
	public static  LinkedList<Peer> peerLinkedList = new LinkedList<Peer>();

    public Socket connection;
    private ObjectInputStream in;	//stream read from the socket
    private ObjectOutputStream out;    //stream write to the socket

	private int chunksDownloaded = 0; 
	public long connectionDownloadRate;
	public long startDownloadTime;
	public long stopDownloadTime;

	
	//handshake variables
	public static final int zerobits_size = 10;
	public static final int peerID_size = 4;
	public static final int header_size = 18;
	public static final int total_length = 32;
	public boolean sentHandshake = false;
	public boolean receivedHandshake = false;

	private int lastRequestedIndex = 0; 

	static volatile boolean done = false; 

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
				System.out.println("Disconnect with Client " + peerID);
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
		System.out.println("Connection: sending message: " + msg + " to Client " + peerID + " on port: "+ connection.getPort() + " at addres: "+ connection.getInetAddress().toString());
 		// if(DataChunks != null){
 			
 		// }
 		
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
		System.out.println("Connection: message type: " + messageValue + " received from client: " + peerID);
		String message = new String (msg);
		
		while(true){
			switch (messageValue) {
		        case 0:
		        	System.out.println("Connection: received choke message received from client: " + peerID);
		            
		            break;
		        case 1:
		           	//received an unchoke message
		        	System.out.println("Connection: received unchoke message received from client: " + peerID);
		        	// create request 
	        		sendRequest();
		            break;
		        case 2:
		        	System.out.println("Connection: received interested message from peer: "+ peerID);
		        	receivedInterseted();
		            break;
		        case 3:
		            System.out.println("Connection: received not interested message received from client: " + peerID);
		            receivedNotInterseted();
		            break;
		        case 4:
		            System.out.println("Connection: received have message received from client: " + peerID);
					determineIfInterestedFromHave(msg);
		            break;
		        case 5:
		        	System.out.println("Connection: received bitfield message received from client: " + peerID);
		        	System.out.println("Connection: bitfield: " +  message + " received from client");
		            determineIfInterestedFromBitfield(msg);
		            break;
		        case 6:
		            //got a requestMessage
		        	System.out.println("Connection: received request message received from client: " + peerID);
		        	//create piece
		        	PieceIndex = msg[5];
		        	System.out.println("the piece index in checkMessage"+ PieceIndex);
		        	
		        	sendPiece(msg);
		            break;
		        case 7:
		        	System.out.println("Connection: received piece message received from client: " + peerID);
		            receivedPiece(msg);
		            // for(int i =0; i < connectionLinkedList.size(); i++){
		            // 	// if(connectionLinkedList.get(i).interested == true){
			           //  // sendHave(msg);
		            // 	// }
		            // }
					stopDownloadTime = System.currentTimeMillis();
		            break;
		       	case 73:
		           	System.out.println("Connection: received handshake message");
		           	//getpeerID if needed. 
		           	//check if we sent our handshake? 
		           	if (sentHandshake == false){
		           		receivedHandshake = true;
		           		getPeerID(msg);
		           		sendHandShake();
		           	}
		           	
		            break;
		        default:
		            System.out.println("Connection: Not a valid type");
		            break;
		        }
				return;
		}
	}

	public void getPeerID(byte[] msg){
		byte[] peerIDArray2 = new byte[4];
		peerIDArray2 = Arrays.copyOfRange(msg, 28, 32);
		// System.arraycopy(msg, 28, peerIDArray2, 0, 4);
		String msgString = new String(msg);
		String peerIDString = new String(peerIDArray2);
		System.out.println("Connection: msg: "+ msgString);
		System.out.println("Connection: PeerID: "+ peerIDString + " " + peerIDArray2.length);
		
		peerID = Integer.parseInt(peerIDString.trim());
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
				System.out.println("Connection: Disconnect with Client " + peerID);
			}

		sendUnchokeMessage();
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
	
	public void addPeers(){
		//place all info in a peer object
    	Peer newPeer = new Peer();
        newPeer.peerID = peerID; 
        newPeer.hostName = hostname; 
        newPeer.port = portNumber;            
        newPeer.hasFile =  false; //assume false until proven wrong by receiving bitfield or have message 
        newPeer.interested = true; 
		newPeer.preferredNeighbor = true;
		newPeer.optimisticNeighbor = true;

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

		//Peer List 
		for (int i = 0; i < peerLinkedList.size(); i++){
			if(peerID == peerLinkedList.get(i).peerID){
				peerLinkedList.get(i).interested = true;
			}
		}

		for (int i = 0; i < connectionLinkedList.size(); i++){
			if(peerID == connectionLinkedList.get(i).peerID){
				connectionLinkedList.get(i).interested = true; 
				System.out.println("Connection: Peer "+ peerID + " is interested: "+ connectionLinkedList.get(i).interested); 
			}
		}
	}

	public void receivedNotInterseted(){
		//update Peer to reflect that it is intersted in Peer List and Connection List
		
		//just to test
		for (int i = 0; i < peerLinkedList.size(); i++){
			System.out.println("Peer "+ peerLinkedList.get(i).peerID+ " is interested: "+ peerLinkedList.get(i).interested); 
		}

		//Peer List 
		for (int i = 0; i < peerLinkedList.size(); i++){
			if(peerID == peerLinkedList.get(i).peerID){
				peerLinkedList.get(i).interested = false;
			}
		}

		for (int i = 0; i < connectionLinkedList.size(); i++){
			if(peerID == connectionLinkedList.get(i).peerID){
				connectionLinkedList.get(i).interested = false; 
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
		peerBitfield[index] = 1; 
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
		boolean checkInterested = false;
		//compare our bitfields
		//msg will contain the other peer's bitfield
		for (int i = 5; i< msg.length; i++){
			// System.out.println("Bitfield: " + msg[i] + " from client " + no);
			//if my bitfield index == 0 && msg == 1
			int bitIndex=i-5; 
			peerBitfield[bitIndex] = msg[i];
			if(myBitfield[bitIndex] == 0 && msg[i] == 1){
				checkInterested = true;  
			}
		}
			//if B has 1 where I have 0 sendInterestedMessage()
		if(checkInterested == true){
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
		if(sendersHasFile == false){
			System.out.println("Connection: Sending Request Message");
			//create new request message
			int length = 9; //4 for length, 1 for type, 4 for payload
			requestMessage = new byte[length];
		
		 	//initalize
			requestMessage = ByteBuffer.allocate(length).putInt(length).array();
			requestMessage[4] = 6; //type six

			int rand = selectRandom(); 
			lastRequestedIndex = rand;
			System.out.println("Connection: Asking for piece: "+ rand);
			myBitfield[rand] = 2; 
			requestMessage[5] = (byte) rand;
			
			sendMessage(requestMessage);

			//start timer 
			startDownloadTime = System.currentTimeMillis();
		}
	}

	public int selectRandom(){
		// System.out.println("Connection: Number of pieces: " + numOfPieces + " "+ myBitfield.length +" "+  peerBitfield.length);
		int r;
		do{
			r = new Random().nextInt(numOfPieces-1);
		}while(myBitfield[r] != 0 && peerBitfield[r] != 1);

		return r;
		/*
		if(myBitfield[r] == 0 && peerBitfield[r] == 1){
			return r;
		}else{
			return selectRandom(); //keep calling until you find one you don't have 
		}
		*/
	}

	public void sendPiece(byte[] msg){
		System.out.println("Connection: Sending Piece Message");
		// for (int i=0; i< myBitfield.length; i++){
		// 	System.out.println("the bitfield is :"+ myBitfield[i]);
		// }
		int index = msg[5];
		if(msg[5] < 0){
			//128* 2 - the negative 
			index = 256 + msg[5];
		}

		System.out.println("Connection: the PieceIndex from send piece is :"+ index);

		//create new piece message
		int length = 4 + 1 + pieceSize; //4 for length, 1 for type, rest for piece content
		pieceMessage = new byte[length];
		byte[] data = new byte[pieceSize];
	 	//initalize
		pieceMessage = ByteBuffer.allocate(length).putInt(length).array();
		pieceMessage[4] = 7; //type seven

		if(myBitfield[index] == 1){
			System.out.println("Connection: I am in the if statement");
			data = DataChunks.get(index);
		}
		
		System.arraycopy(data, 0, pieceMessage,5, data.length);
		System.out.println("Connection: We are done with piece");
		sendMessage(pieceMessage);

		// //start timer 
		// startDownloadTime = System.currentTimeMillis();
	}

	public void receivedPiece(byte[] msg){
		chunksDownloaded++;
		System.out.println("Connection: I have: "+ chunksDownloaded + " chunks downloaded");
		//update Peer to reflect that they get the piece 
		byte[] data = new byte[pieceSize];
		// //just to test
		// for (int i = 0; i < peerLinkedList.size(); i++){
		// 	System.out.println("Peer "+ peerLinkedList.get(i).peerID+ " recieved the piece message"); 
		// }

		//Peer List 	
		System.out.println("Connection: The bitfield length is: " + myBitfield.length+ " and the index is: " + lastRequestedIndex);
		myBitfield[lastRequestedIndex] = 1;

		System.out.println("Connection: the msg is: "+ msg);

		System.arraycopy(msg, 5, data,0, pieceSize);
		System.out.println("Connection: the data is: "+ data);
		DataChunks.set(lastRequestedIndex,data);

		for(int i = 0; i < connectionLinkedList.size(); i++){
			connectionLinkedList.get(i).sendHave(lastRequestedIndex);
		}
		String fileName2 = "Final_File.txt";
		checkIfDone(fileName2);
	}

	public void checkIfDone(String fName){
		//check if chunksDownloaded is the number of pieces we want
		if(chunksDownloaded == numOfPieces){
			//if so change sendershasFile to true
			sendersHasFile = true;
			// DataFile df = new DataFile(pieceSize,fileSize);
			dataFile.WriteBytes(fName);
			System.out.println("Connection: FILE COMPLETE!");
		
			//check if all peers hasFile is true
			boolean allDone = true; 
			for(int i = 0; i < connectionLinkedList.size(); i++){
				if(connectionLinkedList.get(i).sendersHasFile == false){
					allDone = false; 
				}
			}
			
			//stop program
			if(allDone = true && connectionLinkedList.size() != 0){
				done = true;
				try{
					if(in != null){
						in.close();
					}
					if(out != null){
						out.close();
					}
					if (connection != null){
						connection.close();
					}	
				 }catch(IOException ioException){
					System.out.println("Connection: Problem in check if done"+ ioException);
				}
			}
		}
	}

	public void sendHave(int index){
		System.out.println("Connection: Sending Have Message");

		//create new bitfield message
		int length = 4 + 1+ 4;
		haveMessage = new byte[length];

	 	//initalize
		haveMessage = ByteBuffer.allocate(length).putInt(length).array();
		haveMessage[4] = 4;

		haveMessage[5] = (byte)index;
		System.out.println("Connection: I have the piece at index "+ haveMessage[5]);

		sendMessage(haveMessage);
	}

	public void downloadChunks(){
		//download somehow....
		//determine rate
		DetermineRate();
	}

	public long DetermineRate(){
		connectionDownloadRate =  pieceSize /(startDownloadTime - stopDownloadTime);
		return connectionDownloadRate;
	}
}		