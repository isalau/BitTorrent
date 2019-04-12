import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static java.util.Arrays.copyOf;

public class DataFile {
    ArrayList<byte[]> DataInChunks;

    int PieceSize;

    DataFile(int pSize){
        PieceSize = pSize;
    }

    boolean ReadFileIntoChunks(String fileName){
        try {

            InputStream IS = new FileInputStream(fileName);
            DataInChunks = new ArrayList<>();
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

            return true;
        }
        catch(Exception e)
        {
            return false;
        }

    }
}
