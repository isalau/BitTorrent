import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static java.util.Arrays.copyOf;

public class DataFile {
    ArrayList<byte[]> DataInChunks = new ArrayList<>();
    ArrayList<File> FileList = new ArrayList<>();

    int PieceSize;
    int FileSize;
    int numOfPieces = (int) Math.ceil((double)FileSize/PieceSize);
    DataFile(int pSize, int fSize){
        PieceSize = pSize;
        FileSize = fSize;
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
            return true;
        }
        catch(Exception e)
        {
            return false;
        }

    }

    public void CombineFiles(String fileName) {
        File object = new File(fileName);
        FileOutputStream FOutput;
        FileInputStream FInput;
        byte[] ReadBytes;
        int LastReadBytes = 0;
        for (int i = 0; i < numOfPieces; i++) {
            File tempFile= new File(fileName + i.toString());
            Filelist.add(tempFile);
        }
        try {
            FOutput = new FileOutputStream(ofile, true);
            for (File file : Filelist) {
                fis = new FileInputStream(file);
                ReadBytes = new byte[(int) file.length()];
                LastReadBytes = FInput.read(ReadBytes, 0, (int) file.length());
                FOutput.write(fileBytes);
                FOutput.flush();
                LastReadBytes = null;
                FInput.close();
                FInput = null;
            }
            fos.close();
            fos = null;
        } catch (Exception e) {
            System.out.println("Could not merge the file properly");
        }
    }

    public void WriteBytes(){
        try{
            // make the file name 
            OutputStream output = new FileOutputStream(fileName);

            //write the bytes into the file
            for (byte[] bytes in DataInChunks){
                output.write(bytes);
                System.out.println("Wrote into the file successfully!");
                ouput.close();

            }
        }catch(){
             System.out.println("Exception: " + e); 
        }
    }
}
