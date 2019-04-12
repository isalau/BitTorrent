import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.net.InetAddress; //for hostname
import java.net.UnknownHostException; //for hostname

public class Uploader implements Runnable{
	
	public static int peerID;
	public static String hostname;
	public int portNumber;
	public static boolean hasFile;

	private static byte[] message;  //message send to the server
	private static byte interestedMessage[];
	private static byte notInterestedMessage[];
	private static byte[] requestMessage;         //message send to the server
	public static  LinkedList<Connection> connectionLinkedList = new LinkedList<Connection>();
	public static  LinkedList<Peer> peerLinkedList = new LinkedList<Peer>();

	public void Uploader (){}

	public static byte myBitfield[];

	public static boolean sentHandshake = false;
	public static int fileSize;
    public static int pieceSize;
    public static int numInPeerInfo;
   

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
				//their info 
				newConnection.peerID = peerLinkedList.get(i).peerID;
				newConnection.hostname = peerLinkedList.get(i).hostName;
				newConnection.portNumber = peerLinkedList.get(i).port;
				newConnection.hasFile = peerLinkedList.get(i).hasFile;

				//my info 
				newConnection.sendersPeerID = peerID;
				newConnection.sendersHostName = hostname;
            	newConnection.sendersPort = portNumber; // this is currently listener will change
            	newConnection.sendersHasFile = hasFile;  // this is currently listener will change
            	
            	newConnection.alone = false;
            	newConnection.fileSize = fileSize;
				newConnection.pieceSize = pieceSize;
				newConnection.unchokingInterval = unchokingInterval;
				newConnection.optimisticUnchokingInterval= optimisticUnchokingInterval;

				newConnection.numInPeerInfo = numInPeerInfo;
								
				newConnection.peerLinkedList = peerLinkedList;
				connectionLinkedList.add(newConnection);
				newConnection.connectionLinkedList = connectionLinkedList;

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
      		
      		//their info 
      		newConnection.hostname = this.connection.getRemoteSocketAddress().toString();
      		newConnection.portNumber = this.connection.getPort();

      		//my info 
      		newConnection.sendersPeerID = peerID;
      		newConnection.sendersPort = connection.getLocalPort();
      		newConnection.sendersHostName = connection.getLocalAddress().toString();
      		newConnection.sendersHasFile = hasFile;
			newConnection.hasFile = hasFile;
			newConnection.fileSize = fileSize;
			newConnection.pieceSize = pieceSize;
			newConnection.numInPeerInfo = numInPeerInfo;
			newConnection.unchokingInterval = unchokingInterval;
			newConnection.optimisticUnchokingInterval= optimisticUnchokingInterval;
			newConnection.connectionLinkedList = connectionLinkedList;
			newConnection.peerLinkedList = peerLinkedList;
			newConnection.no = this.no;

      		Thread object = new Thread(newConnection);
        	object.start();
        	System.out.println("Thread 2 state: " + object.getState()); 
		}
	}		
}
