import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import java.awt.image.*;

public class HideSteg {
    // number of image bytes required to store one stego byte
    private static final int DATA_SIZE = 8;
    private static final int MAX_INT_LEN = 4;
    public static boolean hide(String secretfname, String imgfname){
        //Read the trademark secret
        String textToHide = readTextFile(secretfname);
        if ((textToHide == null) || (textToHide.length() == 0)) {
            return false;
        }

        //Convert text to byte array
        byte[] stego = buildStego(textToHide);

        //Read image as byte array
        BufferedImage img = loadImage(imgfname);
        if (img == null) {
            return false;
        }
        byte imgBytes[] = accessBytes(img);

        //Try modify image with stego message
        if(!singleHide(imgBytes,stego)){
            return false;
        }

        //Save the stegoimage in stg_<imgname>
        String newname = "stg_"+imgfname;
        
        return writeImageToFile(newname,img);

    }

    private static boolean writeImageToFile(String newname, BufferedImage img) {
        try {
            File outputfile = new File(newname);
            return ImageIO.write(img, "png", outputfile);
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean singleHide(byte[] imgBytes, byte[] stego) {
        int imgLen = imgBytes.length;
        int totalLen = stego.length;

        //check that stego can fit into image
        if ((totalLen*DATA_SIZE) > imgLen){
            //image not big enough
            return false;
        }

        hideStego(imgBytes,stego,0);
        return true;
    }

    private static void hideStego(byte[] imgBytes, byte[] stego, int offset) {
        for (int i = 0; i < stego.length; i++) {
            int byteVal = stego[i];
            for (int j = 7; j >= 0; j--) {
                int bitVal = (byteVal >>> j) & 1;
                //change the last bit
                imgBytes[offset] = (byte)((imgBytes[offset] & 0xFF) | bitVal);
                offset++;
            }
        }
    }

    private static byte[] accessBytes(BufferedImage img) {
        WritableRaster raster = img.getRaster();
        DataBufferByte buffer = (DataBufferByte) raster.getDataBuffer();
        return buffer.getData();
    }

    private static BufferedImage loadImage(String imgfname) {
        File imgFile = new File(imgfname);
        try {
            return ImageIO.read(imgFile);
        } catch (IOException e) {
            return null;
        }
    }

    private static byte[] buildStego(String textToHide) {
        //Databyte to ByteArrays
        byte[] msgBytes = textToHide.getBytes();
        byte[] msglen = intToBytes(msgBytes.length);

        int totalLen = msglen.length + msgBytes.length;
        byte[] stego = new byte[totalLen];

        //combine message length and message
        System.arraycopy(msglen, 0, stego, 0, msglen.length);
        System.arraycopy(msgBytes, 0, stego, msglen.length, msgBytes.length);

        return stego;
    }

    private static byte[] intToBytes(int length) {
        byte[] integerB = new byte[MAX_INT_LEN];
        integerB[0] = (byte) ((length >> 24) & 0xFF);
        integerB[1] = (byte) ((length >> 16) & 0xFF);
        integerB[2] = (byte) ((length >> 8) & 0xFF);
        integerB[3] = (byte) (length & 0xFF);
        return integerB;
    }

    private static String readTextFile(String secretfname) {
        Path filePath = Path.of(secretfname);
        try {
            return Files.readString(filePath);
        } catch (IOException e) {
            return null;
        }
    }
}
