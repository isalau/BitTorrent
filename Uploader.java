import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.net.InetAddress; //for hostname
import java.net.UnknownHostException; //for hostname

public class Uploader implements Runnable{
	public int portNumber;
	public static int peerID;
	public static int numInPeerInfo;
	public static String hostname;

	// handshake  variables
	public static final int zerobits_size = 10;
	public static final int peerID_size = 4;
	public static final int header_size = 18;
	public static final int total_length = 32;

	private static byte[] message;  //message send to the server
	private static byte interestedMessage[];
	private static byte notInterestedMessage[];
	private static byte[] requestMessage;         //message send to the server
	public static  LinkedList<Connection> connectionLinkedList = new LinkedList<Connection>();
	public static  LinkedList<Peer> peerLinkedList = new LinkedList<Peer>();

	public void Uploader (){}

	//private static final int sPort = Port;   //The server will be listening on this port number
	public static byte myBitfield[];

	public static boolean sentHandshake = false;
	public static int fileSize;
    public static int pieceSize;
    public static boolean hasFile;

    public static int clientNum;
    public static int unchokingInterval;
    public static int optimisticUnchokingInterval;

	@Override
	public void run() {
		try{
			start();
		}catch(Exception e){
			System.out.println(e);
		}
	}
  	
  	public void start() throws Exception {
    	System.out.println("Uploader: I am starting on port: "+ portNumber);

        ServerSocket listener = new ServerSocket(portNumber);
		clientNum = 1;

		
		//check if you need to send a handshake and I have peers before me
		if(sentHandshake == false && numInPeerInfo != 0 ){
			for (int i = 0; i < peerLinkedList.size(); i++){
				System.out.println("Uploader: Trying to connect to peerID " + peerLinkedList.get(i).peerID);
				System.out.println("Uploader: Trying to connect to hostName " + peerLinkedList.get(i).hostName);

				Connection newConnection = new Connection();
				newConnection.alone = false;
				newConnection.sendersPeerID = peerID;
				newConnection.peerID = peerLinkedList.get(i).peerID;
				newConnection.portNumber = peerLinkedList.get(i).port;				
				newConnection.hostname = peerLinkedList.get(i).hostName; 
				newConnection.connectionLinkedList = connectionLinkedList;
				newConnection.hasFile = hasFile;
				newConnection.fileSize = fileSize;
				newConnection.pieceSize = pieceSize;
				newConnection.numInPeerInfo = numInPeerInfo;
				newConnection.unchokingInterval = unchokingInterval;
				newConnection.optimisticUnchokingInterval= optimisticUnchokingInterval;
				newConnection.peerLinkedList = peerLinkedList;

      			Thread object = new Thread(newConnection);
        		object.start();
        		System.out.println("Thread 1 state: " + object.getState()); 
			}
			sentHandshake = true;
		}

    	try {
    		while(true) {
    			Socket peer = listener.accept();
    			new Handler(peer,clientNum).start();

    			
    			System.out.println("Client "  + clientNum + " is connected!");
    			System.out.println("Uploader accepted new connection from " + peer.getInetAddress() + " at port " + peer.getPort());
        		
				
				clientNum++;
    		}
    	} finally {
        	// listener.close();
    	}
	}

	/**
 	* A handler thread class.  Handlers are spawned from the listening
 	* loop and are responsible for dealing with a single client's requests.
 	*/
	private static class Handler extends Thread {
		private Socket connection;
        private ObjectInputStream in;	//stream read from the socket
        private ObjectOutputStream out;    //stream write to the socket
		private int no;		//The index number of the client

		public Handler(Socket connection, int no) {
        	this.connection = connection;
    		this.no = no;
    	}

      	public void run() {
      		System.out.println("Uploader: In handler run");
      		Connection newConnection = new Connection();
      		newConnection.connection = this.connection;
      		newConnection.hostname = connection.getRemoteSocketAddress().toString();
      		newConnection.portNumber = connection.getPort();
      		newConnection.sendersPeerID = peerID;
      		newConnection.hasFile = hasFile;
      		newConnection.connectionLinkedList = connectionLinkedList;
			newConnection.hasFile = hasFile;
			newConnection.fileSize = fileSize;
			newConnection.pieceSize = pieceSize;
			newConnection.numInPeerInfo = numInPeerInfo;
			newConnection.unchokingInterval = unchokingInterval;
			newConnection.optimisticUnchokingInterval= optimisticUnchokingInterval;
			newConnection.peerLinkedList = peerLinkedList;
			newConnection.no = this.no;

      		Thread object = new Thread(newConnection);
        	object.start();
        	System.out.println("Thread 2 state: " + object.getState()); 
		}
	}		
}
