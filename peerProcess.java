import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class peerProcess{
	public static int fileSize;
	public static int pieceSize;
    public static int numInPeerInfo;
    public static int portNum;
    public static boolean hasFile;
    public static int unchokingInterval;
    public static int optimisticUnchokingInterval;
    public static String fileName;
    public static ArrayList<byte[]> DataChunks;


	public static void main(String args[] ){
        String peerIDString = "";

		System.out.println("Starting Peer Process");

		// check for peerID 
        if (args.length > 0) { 
            for (String val:args) {
                System.out.println("PeerID: " + val);
                peerIDString = val; 
            }
        } 
        else
            System.out.println("No peerID provided"); 
            boolean readFile = false;
            //read in files
            CommonParser CP = new CommonParser();
        if(CP.Parse("Common.cfg")){
            System.out.println(CP.NumberOfPreferredNeighbors);
            System.out.println(CP.UnchokingInterval);
            unchokingInterval = CP.UnchokingInterval;
            System.out.println(CP.OptimisticUnchokingInterval);
            optimisticUnchokingInterval = CP.OptimisticUnchokingInterval;
            System.out.println(CP.DataFileName);
            fileName = CP.DataFileName;
            System.out.println(CP.FileSize);
            fileSize = CP.FileSize;
            System.out.println(CP.PieceSize);
            pieceSize = CP.PieceSize;
        }
        else
        {
            System.out.println("Could not read Common.cfg file!");
        }


        
        // separate file into chunks
        // FileManager fileManager = new FileManager();
        // fileManager.fileSize = fileSize;
        // fileManager.pieceSize = pieceSize; 
        // fileManager.fileName = fileName;
        // fileManager.determineSizes();
        // try{
        // 	fileManager.splitFile();
        // }catch(IOException ioe){
        // 	System.out.print(" Could not split file from peer process: " + ioe);
        // }
        

        //read tracker 
        
        PeerParser PP = new PeerParser();
        numInPeerInfo = 0;
        int i = 0; 
        if(PP.Parse("PeerInfo.cfg")){
            for(PeerParser.PeerInfo PI : PP.PeerInfos){
                System.out.print(PI.PeerID + " ");
                System.out.print(PI.HostName + " ");
                System.out.print(PI.Port + " ");
                System.out.println(PI.HasFile);
                
                if (PI.PeerID == Integer.parseInt(args[0])) {
                    readFile = PI.HasFile;
                    numInPeerInfo = i; 
                    portNum = PI.Port;
                    hasFile = PI.HasFile;
                }
                i++;
            }
        }
        else{
            System.out.println("Could not read PeerInfo.cfg!");
        }

        if(readFile){
            DataFile DF = new DataFile(CP.PieceSize, CP.FileSize);
            if(DF.ReadFileIntoChunks(CP.DataFileName)){
                DataChunks = DF.DataInChunks;
                // System.out.println("the data chunks size is :"+DF.DataInChunks.size());
                // System.out.println(DF.DataInChunks.get(DF.DataInChunks.size() - 1).length);
            }
            else{
                System.out.println("Failed to load the data file!");
            }
        }
        int peerID = Integer.parseInt(peerIDString);
        int numOfPieces = (int) Math.ceil((double)fileSize/pieceSize);
        Client client = new Client();
        client.peerID = peerID;
        client.numInPeerInfo = numInPeerInfo; 
        client.port = portNum;
        client.fileSize = fileSize;
        client.pieceSize = pieceSize;
        client.hasFile = hasFile;
        client.unchokingInterval = unchokingInterval;
        client.optimisticUnchokingInterval = optimisticUnchokingInterval;



        if(DataChunks!= null){
            System.out.println("the data chunks is :"+DataChunks);
            client.DataChunks = DataChunks; 
            for(int j=0; j <  numOfPieces;j++){
                System.out.println("the data chunks are:"+DataChunks.get(j));
            }
        }
        
        Thread object = new Thread(client);
        object.start();
    }
}