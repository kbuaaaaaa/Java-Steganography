import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MaximizeAction;

import java.awt.image.*;

public class RevealSteg {
    // number of image bytes required to store one stego byte
    private static final int DATA_SIZE = 8;
    private static final int MAX_INT_LEN = 4;
    public static boolean reveal(String imgfname){
        //Get image as byte array
        BufferedImage img = loadImage(imgfname);
        if (img == null){
            return false;
        }
        byte[] imgBytes = accessBytes(img);

        //Get message length
        int msgLen = getMsgLength(imgBytes,0);
        if(msgLen == -1){
            return false;
        }

        //Get message after the length
        String msg = getMessage(imgBytes,msgLen, MAX_INT_LEN*DATA_SIZE);
        if (msg != null) {
            String newname = imgfname+"msg.txt";
            return writeStringToFile(newname,msg);
        }
        else{
            return false;
        }
    }
    
    private static boolean writeStringToFile(String newname, String msg) {
        try {
            File newTextFile = new File(newname);
            FileWriter fw = new FileWriter(newTextFile);
            fw.write(msg);
            fw.close();
            return true;

        } catch (IOException iox) {
            //do stuff with exception
            return false;
        }
    }

    private static String getMessage(byte[] imgBytes, int msgLen, int offset) {
        byte[] msgBytes = extractHiddenBytes(imgBytes, msgLen, offset);
        if (msgBytes == null){
            return null;
        }

        String msg = new String(msgBytes);

        if (msg.matches("\\A\\p{ASCII}*\\z")){
            return msg;
        }
        else{
            return null;
        }
    }

    private static int getMsgLength(byte[] imgBytes, int offset) {
        //Get the binary msglength as byte array
        byte[] lenBytes = extractHiddenBytes(imgBytes, MAX_INT_LEN, offset);
        if (lenBytes == null){
            return -1;
        }

        int msgLen = ByteToInt(lenBytes);
        if ((msgLen <= 0) || (msgLen > imgBytes.length)) {
            return -1;
        }
        return msgLen;
    }

    private static byte[] extractHiddenBytes(byte[] imgBytes, int size, int offset) {
        int finalPos = offset + (size*DATA_SIZE);
        if (finalPos > imgBytes.length) {
            return null;
        }

        byte[] hiddenBytes = new byte[size];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < DATA_SIZE; j++) {
                //make one hidden byte from image byte
                hiddenBytes[i] = (byte) ((hiddenBytes[i] << 1)|
                                            imgBytes[offset] & 1);
            //shift existing bits left and store LSB imgbyte on the right
            offset++;
            }          
        }
        return hiddenBytes;
    }

    private static int ByteToInt(byte[] lenBytes) {
        int msgLen = ((lenBytes[0] & 0xff) << 24) |
                    ((lenBytes[1] & 0xff) << 16) |
                    ((lenBytes[2] & 0xff) << 8) |
                    ((lenBytes[3] & 0xff));
        return msgLen;
    }

    private static BufferedImage loadImage(String imgfname) {
        File imgFile = new File(imgfname);
        try {
            return ImageIO.read(imgFile);
        } catch (IOException e) {
            return null;
        }
    }

    private static byte[] accessBytes(BufferedImage img) {
        WritableRaster raster = img.getRaster();
        DataBufferByte buffer = (DataBufferByte) raster.getDataBuffer();
        return buffer.getData();
    }
}
