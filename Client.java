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
	public String hostName;
	public int port;
	public boolean hasFile;

	//client's info 
	public byte bitfield[];
	public LinkedList<Peer> peerLinkedList = new LinkedList<Peer>();
	public LinkedList<Connection> connectionLinkedList = new LinkedList<Connection>();
	public Socket connection; 
	
	//from commmon.pg
	public int numInPeerInfo;
	public int fileSize;
    public int pieceSize;
	public int unchokingInterval;
	public int optimisticUnchokingInterval;


 	Uploader up = new Uploader();
 	public boolean sentHandshake = false;

	//constructor
	public void Client(){}

	@Override
	public void run() {
		//start timers 
		initalizeTimer();
		
		System.out.println("Run in client with peerID "+ this.peerID+ " with num in PeerInfo "+ numInPeerInfo);
		
		//tracker initalization 
		addPeers();

		//start our listener/ uploader 
		runUploader();
	}
	
	public void runUploader(){
		System.out.println("Client: Calling run uploader");
		
		up.peerID = peerID;
		up.portNumber = port;
		up.hasFile = hasFile;
		up.myBitfield = bitfield;		
		up.fileSize = fileSize;
		up.pieceSize = pieceSize;
		up.unchokingInterval = unchokingInterval;
		up.optimisticUnchokingInterval = optimisticUnchokingInterval;
		up.numInPeerInfo = numInPeerInfo;
		up.peerLinkedList = peerLinkedList;
		up.connectionLinkedList = connectionLinkedList;
		
		
		Thread object = new Thread(up);
		try{		
			object.start();
		}catch(Exception e){
			System.out.println("Exception: "+ e);
		}
	}

	public void addPeers(){
		System.out.println("Client: In Adding to Peer Lists");

		//get info for tracker
		PeerParser PP = new PeerParser();
        if(PP.Parse("PeerInfo.cfg")){
        	for(int i = 0; i < numInPeerInfo; i++){
            	PeerParser.PeerInfo PI = PP.PeerInfos.get(i);

            	//place all info in a peer object
            	Peer newPeer = new Peer();
                newPeer.peerID = PI.PeerID; 
                newPeer.hostName = PI.HostName; 
                newPeer.port = PI.Port;            
                newPeer.hasFile =  PI.HasFile; 
                newPeer.interested = false; 
				newPeer.prefferedNeighbor = false;
				newPeer.optimisticNeighbor = false;

				byte[] emptyArray = new byte[32];
				newPeer.bitfield = emptyArray;

				//add to Linked lists 
				peerLinkedList.add(newPeer);

				/*
            	Connection newConnection = new Connection();
            	//their info 
            	newConnection.peerID = PI.PeerID;
            	newConnection.hostname = PI.HostName;
            	newConnection.portNumber = PI.Port;
            	newConnection.hasFile = PI.HasFile;

            	//my info 
            	newConnection.sendersPeerID = peerID;
            	newConnection.sendersHostName = hostName;
            	newConnection.sendersPort = port; // this is currently listener will change
            	newConnection.sendersHasFile = hasFile;  // this is currently listener will change

            	newConnection.alone = false;	
		        newConnection.fileSize = fileSize;
		        newConnection.pieceSize = pieceSize;
		        newConnection.unchokingInterval = unchokingInterval;
		        newConnection.optimisticUnchokingInterval = optimisticUnchokingInterval;

		        newConnection.numInPeerInfo = numInPeerInfo; 
		        newConnection.myBitfield = bitfield;

				connectionLinkedList.add(newConnection);

				newConnection.peerLinkedList = peerLinkedList;
				newConnection.connectionLinkedList = connectionLinkedList;
				*/
            }
        }
        else{
            System.out.println("Could not read PeerInfo.cfg!");
        }		
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
				// optimisticUnchoke(optimisticUnchokingInterval);
			}
		};

		//timer is in miliseconds so we need to multiply by 1000
		long miliOptUnChoke = optimisticUnchokingInterval *1000;
		optUnChokeTimer.schedule(optUnChoke,miliOptUnChoke);
	}

	public void unChoke(int unchokingInterval){
		updateChokeTimer();
		System.out.println(unchokingInterval + " seconds has passed normal unchoke/choke");
	}

	public void determinePrefferedNeighbors(){
		//
	}

	public void optimisticUnchoke(int optimisticUnchokingInterval){
		updateOptTimer();
		System.out.println(optimisticUnchokingInterval + " seconds has passed for optimistic Choke/Unchoke");
	}

	public void pickRandomNeighbor(){

		//get number of peers
        int numOfPeers = peerLinkedList.size();
        
        //pick rand from range of peers use rand for that
        int rand = new Random().nextInt(numOfPeers);

        //check that not already unchoked 
	}

	public void pickRandomOptNeighbor(){

		//get number of peers
        int numOfPeers = peerLinkedList.size();
        
        if(numOfPeers !=0){
	        //pick rand from range of peers use rand for that
	        int rand = new Random().nextInt(numOfPeers);

	        //check that not already unchoked
	        if(peerLinkedList.get(rand).prefferedNeighbor == false && peerLinkedList.get(rand).optimisticNeighbor == false){
	        	peerLinkedList.get(rand).optimisticNeighbor = true;	
	        }else{
	        	pickRandomOptNeighbor();
	        }
	    }else{
	    	//we have no peers
	    }
	}
}