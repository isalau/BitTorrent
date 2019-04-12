import java.net.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

public class FileManager{
	public static int fileSize;
	public static int pieceSize; 
	public static int maxPieceSize; 
	public static int windowSize; 
	public static String fileName;

	public FileManager(){}

	public static void determineSizes(){
		//check to make sure pieceSize is not too large i.e. > 65536 bits in java because Babak told us. 
        maxPieceSize = 65536*8;
        if(pieceSize > maxPieceSize){
        	//make pieceSize equal to the max
        	pieceSize = maxPieceSize - 1; //minus one because we want a buffer space
        }

        windowSize = ;
	}

	public static void splitFile() throws IOException{		 
		//get the file from the string
		Path currentRelativePath = Paths.get("");
		File file = new File(currentRelativePath.toAbsolutePath().toString()+"/"+fileName);

		//fileChunks are of size windowSize
		int num = fileSize; 
		int lastByteRead = 0;
		int i= 0; //where we are in the array of bytes

		//read in the file
		try{
			FileInputStream fileInputStream = new FileInputStream(file);
			while(num > 0){
				if (num <= 5){
					windowSize = num;
				}
				byte[] fileChunkArray = new byte[windowSize];
				lastByteRead = fileInputStream.read(fileChunkArray, 0, windowSize);
				num = num - lastByteRead; 
				i++;
				String name = file.getParent()+file.getName()+ i;
				FileOutputStream newFile  = new FileOutputStream(new File(name));
				newFile.write(fileChunkArray);
				newFile.flush();
				newFile.close();
			}
			fileInputStream.close();	
		}catch(IOException ioe){
			System.out.println("Could not split file: " + ioe);
		}
	}
}