import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.stream.IntStream;
import java.util.Arrays;
import java.net.InetAddress; //for hostname
import java.net.UnknownHostException; //for hostname
import java.util.LinkedList;
import java.util.Timer; 
import java.util.TimerTask; 
import java.util.Random;

//Client is yourself. You as a client have an uploader , file handler and peerList 
public class Client implements Runnable{
	
	//base information 
	public int peerID;
	private String hostName;
	private String port;
	public boolean hasFile;

	//client's info 
	private byte bitfield[];
	LinkedList<Peer> PeerLinkedList = new LinkedList<Peer>();
	public int my_portNumber;
	public int numInPeerInfo;
	public int fileSize;
    public int pieceSize;
	public int unchokingInterval;
	public int optimisticUnchokingInterval;


 	Uploader up = new Uploader();

	//constructor
	public void Client(){}

	@Override
	public void run() {
		//start timers 
		initalizeTimer();
		
		System.out.println("Run in client with peerID "+ this.peerID+ " with num in PeerInfo"+ numInPeerInfo);
		
		//start our listener/ uploader 
		runUploader();
		
		//initalize our peerHashMap
		peerHashMap = new HashMap<Integer,Peer>();

        //add to Linked List 
		addPeers();
	}

	public void initalizeTimer(){
		//timer for choke and unchoke
		Timer unChokeTimer = new Timer();
		Timer optUnChokeTimer = new Timer();

		TimerTask unChoke = new TimerTask(){
			public void run(){
				unChoke(unchokingInterval);
			}
		};

		TimerTask optUnChoke = new TimerTask(){
			public void run(){
				optimisticUnchoke(optimisticUnchokingInterval);
			}
		};

		//timer is in miliseconds so we need to multiply by 1000
		long miliUnChoke = unchokingInterval *1000;
		long miliOptUnChoke = optimisticUnchokingInterval *1000;

		unChokeTimer.schedule(unChoke, miliUnChoke);
		optUnChokeTimer.schedule(optUnChoke,miliOptUnChoke);
	}

	public void runUploader(){
		System.out.println("Client: Calling run uploader");
		
		up.portNumber = my_portNumber;
		up.peerID = peerID;
		up.myBitfield = bitfield;
		up.fileSize = fileSize;
		up.pieceSize = pieceSize;
		up.hasFile = hasFile;
		up.peerLinkedList = PeerLinkedList; 
		Thread object = new Thread(up);
		try{		
			object.start();
		}catch(Exception e){
			System.out.println("Exception: "+ e);
		}
	}

	public void addPeers(){
		//get info from tracker
		System.out.println("In Adding to Peer Lists");
		PeerParser PP = new PeerParser();
		try{
	        if(PP.Parse("PeerInfo.cfg")){
	        	for(int i = 0; i < numInPeerInfo; i++){
	            // for(PeerParser.PeerInfo PI : PP.PeerInfos){
	            	PeerParser.PeerInfo PI = PP.PeerInfos.get(i);
	            	//place all info in a peer object
	            	Peer newPeer = new Peer();
	            	Connection newConnection = new Connection();
	            	newConnection.senderspeerID = peerID; 

	                // System.out.print(PI.peerID + " ");
	                newPeer.peerID = PI.PeerID; 
	                newConnection.peerID = PI.PeerID;
	                
	                // System.out.print(PI.HostName + " ");
	                newPeer.hostName = PI.HostName; 
	                
	                // System.out.print(PI.Port + " ");
	                newPeer.port = PI.Port; 

	                //you have peers to connect to 
	                if(numInPeerInfo !=0){
	                	connection = new Socket(PI.HostName, PI.Port);
	                	newConnection.connection = connection;
	                }
	                
	                // System.out.println(PI.HasFile);
	                newPeer.hasFile =  PI.HasFile; 

					
	                newPeer.interested = false; 
					newPeer.prefferedNeighbor = false;
					newPeer.optimisticNeighbor = false;

					byte[] emptyArray = new byte[32];
					newPeer.bitfield = emptyArray;

			        newConnection.numInPeerInfo = numInPeerInfo; 
			        newConnection.my_portNumber = my_portNumber;
			        newConnection.fileSize = fileSize;
			        newConnection.pieceSize = pieceSize;
			        newConnection.hasFile = hasFile;
			        newConnection.unchokingInterval = unchokingInterval;
			        newConnection.optimisticUnchokingInterval = optimisticUnchokingInterval;

					//add to Linked lists 
					peerHashMap.put(newPeer.peerID, newPeer);
					PeerLinkedList.add(newPeer);
					
					ConnectionLinkedList.add(newConnection);
					up.connectionLinkedList = ConnectionLinkedList;

	                if(numInPeerInfo !=0){
	                	newConnection.sendHandShake();
	                }			
	            }
	        }
	        else{
	            System.out.println("Could not read PeerInfo.cfg!");
	        }	
	    }catch(IOException ioException){
		 	System.out.println("There are no clients in adding peers: "+ ioException);
		}	
	}


	//preffered neighbor choke timer
	public void updateChokeTimer(){
		//timer for choke and unchoke
		Timer unChokeTimer = new Timer();

		TimerTask unChoke = new TimerTask(){
			public void run(){
				unChoke(unchokingInterval);
			}
		};

		//timer is in miliseconds so we need to multiply by 1000
		long miliUnChoke = unchokingInterval *1000;

		unChokeTimer.schedule(unChoke, miliUnChoke);
	}

	//optimistically picked neighbor timer
	public void updateOptTimer(){
		Timer optUnChokeTimer = new Timer();

		TimerTask optUnChoke = new TimerTask(){
			public void run(){
				optimisticUnchoke(optimisticUnchokingInterval);
			}
		};

		//timer is in miliseconds so we need to multiply by 1000
		long miliOptUnChoke = optimisticUnchokingInterval *1000;

		optUnChokeTimer.schedule(optUnChoke,miliOptUnChoke);
	}

	public void determinePrefferedNeighbors(){}

	public void pickRandomNeighbor(){

		//get number of peers
        int numOfPeers = PeerLinkedList.size();
        
        //pick rand from range of peers use rand for that
        int rand = new Random().nextInt(numOfPeers);

        //check that not already unchoked 
	}

	public void pickRandomOptNeighbor(){

		//get number of peers
        int numOfPeers = PeerLinkedList.size();
        
        if(numOfPeers !=0){
	        //pick rand from range of peers use rand for that
	        int rand = new Random().nextInt(numOfPeers);

	        //check that not already unchoked
	        if(PeerLinkedList.get(rand).prefferedNeighbor == false && PeerLinkedList.get(rand).optimisticNeighbor == false){
	        	PeerLinkedList.get(rand).optimisticNeighbor = true;	
	        }else{
	        	pickRandomOptNeighbor();
	        }
	    }else{
	    	//we have no peers
	    }
	}
}