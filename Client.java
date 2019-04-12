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
	public ArrayList<byte[]> DataChunks;


 	Uploader up = new Uploader();
 	public boolean sentHandshake = false;
 	public boolean theBoolean = false;

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
		if(DataChunks != null ){
			up.DataChunks = DataChunks;
			System.out.println("the data chunks size is :"+DataChunks.size());
		}
		
		
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
				// unChoke(unchokingInterval);
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

	public void unChoke(int unchokingInterval){
		updateChokeTimer();
		System.out.println(unchokingInterval + " seconds has passed normal unchoke/choke");

		System.out.println("Client: Unchoking peer list "+ peerLinkedList);
	}

	public void determinePrefferedNeighbors(){
		//
	}

	public void pickRandomNeighbor(){

		//get number of peers
        int numOfPeers = peerLinkedList.size();
        
        //pick rand from range of peers use rand for that
        int rand = new Random().nextInt(numOfPeers);

        //check that not already unchoked 
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

	public void optimisticUnchoke(int optimisticUnchokingInterval){
		updateOptTimer();
		System.out.println("Client: " + optimisticUnchokingInterval + " seconds has passed for optimistic Choke/Unchoke");

		try{
			//update my peer Linked List 
			peerLinkedList = up.peerLinkedList;
			connectionLinkedList = up.connectionLinkedList;
			System.out.println("Client: Opt unchoking peer list "+ peerLinkedList);
			System.out.println("Client: Opt unchoking connection list "+ connectionLinkedList);
			
			//if I am not alone
			if(peerLinkedList.size() != 0){
				//get current optimistic neighbor to change later 
				int oldNeighbor = 0; 
				for(int i = 0; i < peerLinkedList.size(); i++){
					if(peerLinkedList.get(i).prefferedNeighbor == true){
						oldNeighbor = i;
					}
				}

				if(peerLinkedList.size() == 1){
					//you only have one neighbor and should just keep as preffered unchoked neighbor
					//send choke message to old opt neighbor 
					peerLinkedList.get(0).prefferedNeighbor = true;

					//make old optimistic neighbor false 
					peerLinkedList.get(0).optimisticNeighbor = true;

					//send unchoke message using connection from connection list 
					connectionLinkedList.get(0).sendUnchokeMessage();
				}else{

					//select my peer to unchoke --> must be currently choked and interested 
					int optNeighborIndex =  pickRandomOptNeighbor();
					System.out.println("Client: Opt unchoking peer: "+ optNeighborIndex);
					System.out.println("Client: Opt choking connection: "+ oldNeighbor);

					//send unchoke message using connection from connection list 
					connectionLinkedList.get(optNeighborIndex).sendUnchokeMessage();
					
					//send choke message to old opt neighbor 
					connectionLinkedList.get(oldNeighbor).sendChokeMessage();

					//make old optimistic neighbor false 
					peerLinkedList.get(oldNeighbor).optimisticNeighbor = false;

					//propogate changes down stream too
					up.peerLinkedList = peerLinkedList;
					up.connectionLinkedList = connectionLinkedList;
				}
			}
		}catch(Exception e){
			System.out.print("optimisticUnchoke: "); 
			e.printStackTrace();
		}
	}

	public int pickRandomOptNeighbor(){

		//get number of peers
        int numOfPeers = peerLinkedList.size();
        
        if(numOfPeers != 0 && numOfPeers!= 1){
	        //pick rand from range of peers use rand for that
	        int rand = new Random().nextInt(numOfPeers);
	        System.out.println("Client: Test random neighbor: " + rand);
	        //check that not already unchoked & that it is interested 
	        if(peerLinkedList.get(rand).prefferedNeighbor == false && peerLinkedList.get(rand).optimisticNeighbor == false && peerLinkedList.get(rand).interested == true){
	        	peerLinkedList.get(rand).optimisticNeighbor = true;	
	        	return rand;
	        }else if(numOfPeers ==1){
	        	if(peerLinkedList.get(0).prefferedNeighbor == false && peerLinkedList.get(0).optimisticNeighbor == false && peerLinkedList.get(0).interested == true){
	        	peerLinkedList.get(0).optimisticNeighbor = true;	
				return 0;
		    	}
		    }
	        else{
	        	//try again with new random number
	        	pickRandomOptNeighbor();
	        }
	    }else{
	    	//we have no peers
	    }
	    return 0;
	}
}