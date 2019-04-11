import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.net.InetAddress; //for hostname
import java.net.UnknownHostException; //for hostname

public class Connection implements Runnable{
	/**********************variables***********************/
	
	public static int portNumber;
	public static int peerID;

	public static final int zerobits_size = 10;
	public static final int peerID_size = 4;
	public static final int header_size = 18;
	public static final int total_length = 32;

	public static boolean sentHandshake = false;
	public static int fileSize;
    public static int pieceSize;
    public static boolean hasFile;

	public static byte[] myBitfield;
	private static byte[] message;  
	private static byte[] interestedMessage;
	private static byte[] notInterestedMessage;
	private static byte[] requestMessage;
	private static byte[] haveMessage;       

	public static  LinkedList<Connection> connectionLinkedList = new LinkedList<Connection>();
	public static  LinkedList<Peer> peerLinkedList = new LinkedList<Peer>();


    private Socket connection;
    private ObjectInputStream in;	//stream read from the socket
    private ObjectOutputStream out;    //stream write to the socket
	private int no;		//The index number of the client

	/********************** constructor ***********************/

	public void Connection (){}

	/********************** functions ***********************/
	//run establishes individual connection
	@Override
  	public void run() {
  		System.out.println("Connection: I am running");
			try{
				InetAddress myID = InetAddress.getLocalHost();
				String iP = myID.toString();
            	String myHostname = myID.getHostName();

				//initialize Input and Output streams
				out = new ObjectOutputStream(connection.getOutputStream());
				//out.flush();
				in = new ObjectInputStream(connection.getInputStream());
			try{
				while(true){
					//receive the message sent from the client
					byte[] myObjects = (byte[])in.readObject();
					
					//show the message to the user
				    String objectMessage = new String (myObjects);
				    System.out.println("Message: " + objectMessage );

					//check what message you got
					checkMessage(myObjects);
				}
			}catch(ClassNotFoundException classnot){
				System.err.println("Data received in unknown format");
			}
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
		// 	}catch(IOException ioException){
		// 		System.out.println("Disconnect with Client " + no);
		// 	}
		// }
	}

	//send a message to the output stream
	public void sendMessage(byte[] msg){
		System.out.println("Connection: sending message: " + msg + " to Client " +no);
		try{
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

		switch (messageValue) {
	        case 0:
	        	System.out.println("Connection: received choke message");
	            
	            break;
	        case 1:
	           	//received an unchoke message
	        	System.out.println("Connection: received unchoke message");
	        	// create request 
        		request();
	            break;
	        case 2:
	        	System.out.println("Connection: received interested message");
	            break;
	        case 3:
	            System.out.println("Connection: received not interested message");
	            break;
	        case 4:
	            System.out.println("Connection: received have message");
				determineIfInterestedFromHave();
	            break;
	        case 5:
	        	System.out.println("Connection: received bitfield message");
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
	           		sendHandShake(msg);
	           	}
	           	sendBitfield();
	            break;
	        default:
	            System.out.println("Connection: Not a valid type");
	            break;
	        }
			return;
	}

	public void sendHandShake(byte[] msg){				
		System.out.println("Connection: In sendHandShake function");
		sentHandshake = true;
		//get the connection you want to send the handshake too
		//need to get the correct peerID from msg 
		String handshakeString = new String (msg);
		String connectionPeerID = handshakeString.substring(28);
		int conPeerID = Integer.parseInt(connectionPeerID);
		System.out.println("Connection: My PeerID: " + connectionPeerID);
		for(int i = 0; i <  connectionLinkedList.size(); i++){
			if(connectionLinkedList.get(i).peerID == conPeerID ){
				connectionLinkedList.get(i).sendHandShake();
			}
		}
	}

	public void sendBitfield(){
		//create payload
		//determine number of pieces from common.cfg
    	int numOfPieces = (int) Math.ceil((double)fileSize/pieceSize);
    	System.out.println("Uploader: Sending Bitfield with " + numOfPieces " pieces.");

		//determine what parts of the file I have 
		int length = 4 + 1 + (numOfPieces/8); 
    	myBitfield = new byte[length];
    	
		//create new bitfield message
		myBitfield = ByteBuffer.allocate(length).putInt(length).array();
		myBitfield[4] = 5;

    	if(hasFile == true){
    		for (int i = 5; i< length; i++){
    			myBitfield[i] =  1;
    			//send message to B
    		}
    		sendMessage(myBitfield);
    	}
	}

	public void sendChokeMessage(){}

	public void sendUnchokeMessage(){}

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

	public void sendPiece(){}
}		