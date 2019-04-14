import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.io.File;
import static java.util.Arrays.copyOf;

public class DataFile {
    ArrayList<byte[]> DataInChunks = new ArrayList<>();
    ArrayList<File> FileList = new ArrayList<>();
    File file;

    int PieceSize;
    int FileSize;
    int numOfPieces = (int) Math.ceil((double)FileSize/PieceSize);
    DataFile(int pSize, int fSize){
        PieceSize = pSize;
        FileSize = fSize;
        numOfPieces = (int) Math.ceil((double)FileSize/PieceSize);
    }

    boolean ReadFileIntoChunks(String fileName){
        try {
            InputStream IS = new FileInputStream(fileName);
            int readBytes = 0;
            do{
                byte[] b = new byte[PieceSize];
                if( (readBytes = IS.read(b))  == PieceSize)
                    DataInChunks.add(b);
                else{
                    if(readBytes != -1)
                        DataInChunks.add(copyOf(b,readBytes));
                }
            } while(readBytes != -1);
            if(numOfPieces > DataInChunks.size()){
                for (int empties = DataInChunks.size(); empties < numOfPieces; empties++){
                    byte[] blanks = new byte[PieceSize];
                    DataInChunks.add(blanks);
                }
            }
            System.out.println("DataFile: The size of Data Array is: "+ DataInChunks.size());
            return true;
        }
        catch(Exception e)
        {
            return false;
        }
    }

    public void makeEmpty(){
        System.out.println("DataFile: numOfPieces = " + numOfPieces);
        for (int empties = 0; empties < numOfPieces; empties++){
            byte[] blanks = new byte[PieceSize];
            DataInChunks.add(blanks);
        }
        System.out.println("DataFile: The size of Data Array is: "+ DataInChunks.size());
    }

    public void CombineFiles(String fileName) {
        File object = new File(fileName);
        FileOutputStream FOutput;
        FileInputStream FInput;
        byte[] ReadBytes;
        int LastReadBytes = 0;
        for (int i = 0; i < numOfPieces; i++) {
            String newName = fileName + Integer.toString(i); 
            File tempFile= new File(newName);
            FileList.add(tempFile);
        }
        try {
            FOutput = new FileOutputStream(object, true);
            for (File file : FileList) {
                FInput = new FileInputStream(file);
                ReadBytes = new byte[(int) file.length()];
                LastReadBytes = FInput.read(ReadBytes, 0, (int) file.length());
                FOutput.write(ReadBytes);
                FOutput.flush();
                LastReadBytes = 0;
                FInput.close();
                FInput = null;
            }
            FOutput.close();
            FOutput = null;
        } catch (Exception e) {
            System.out.println("Could not merge the file properly");
        }
    }

    public void WriteBytes(String fileName){
        int i =0;
        try{
            String path = "/Users/kianamac/Documents/GitHub/BitTorrent/Test/" + fileName;
            file = new File(path);
            FileOutputStream output = new FileOutputStream(file);
            System.out.println("I am in write bytes before the loop");
            //write the bytes into the file
            System.out.println("the data chunks size is :"+ DataInChunks.size());
            for (int j = 0; j < DataInChunks.size();j++){
                output.write(DataInChunks.get(j));
                System.out.println("Wrote into the file successfully!");
                System.out.println("I am in write bytes in the loop");
                output.flush();

            }
            i++;
            System.out.println("I am in write bytes after the loop");
        }catch(Exception e){
             System.out.println("Exception: " + e); 
        }
    }
}
