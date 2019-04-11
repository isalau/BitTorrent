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

	@Override
	public void run() {
		try{
			start();
		}catch(Exception e){
			System.out.println(e);
		}
	}
  	
  	public void start() throws Exception {
    	System.out.println("Uploader: I am starting");

        ServerSocket listener = new ServerSocket(portNumber);
		int clientNum = 1;
    	try {
    		while(true) {
        		new Handler(listener.accept(),clientNum).start();
				System.out.println("Client "  + clientNum + " is connected!");
				clientNum++;
    		}
    	} finally {
        	listener.close();
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
      		//pass connection to connection somehow
		}
	}		
}
