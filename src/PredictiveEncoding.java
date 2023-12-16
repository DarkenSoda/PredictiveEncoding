import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PredictiveEncoding {
    public static void Compress(BufferedImage image, File outputFile) {
        int[][][] originalRGBImage = RWCompression.ReadImageRGB(image);
        int[][] originalGrayScaleImage = RWCompression.ReadImageGrayScale(image);
        int originalHeight = image.getHeight();
        int originalWidth = image.getWidth();

        
        // originalHeight, originalWidth, quantizedDiff arr[][],
        // quantizer length, quantizer table
        try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream)) {

            dataOutputStream.writeInt(originalWidth);
            dataOutputStream.writeInt(originalHeight);

            for (int i = 0; i < originalHeight; i++) {
                for (int j = 0; j < originalWidth; j++) {
                    // dataOutputStream.writeInt(quantizedDiff[i][j]));
                }
            }

            dataOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static BufferedImage Decompress(File fileToDecompress) {
        return null;
    }
}
