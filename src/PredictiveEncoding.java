import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.Math;
public class PredictiveEncoding {
    public static void Compress(BufferedImage image, File outputFile) {
        // int[][][] originalRGBImage = RWCompression.ReadImageRGB(image);
        int[][] originalGrayScaleImage = RWCompression.ReadImageGrayScale(image);
        int originalHeight = image.getHeight();
        int originalWidth = image.getWidth();

        
        // originalHeight, originalWidth, quantizedDiff arr[][],
        // quantizer length, quantizer table
        try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream)) {

            dataOutputStream.writeInt(originalWidth);
            dataOutputStream.writeInt(originalHeight);
            // int [][] decodedImage=new int[originalHeight][originalWidth];
            int [][] predicted=new int[originalHeight][originalWidth];
            int [][] difference=new int[originalHeight][originalWidth];
            int [][] quantizedDifference=new int[originalHeight][originalWidth];
            // int [][] dequantzedDifference=new int[originalHeight][originalWidth];
            
            
            for (int i = 0; i < originalHeight; i++) {
                for (int j = 0; j < originalWidth; j++) {
                    if(i==0||j==0){
                        predicted[i][j]=originalGrayScaleImage[i][j];
                        difference[i][j]=originalGrayScaleImage[i][j];
                        quantizedDifference[i][j]=originalGrayScaleImage[i][j];
                    }else{
                        int b=originalGrayScaleImage[i-1][j-1];
                        int a=originalGrayScaleImage[i][j-1];
                        int c=originalGrayScaleImage[i-1][j];
                        if(b<=Math.min(a,c)){
                            predicted[i][j]=Math.max(a,c);
                        }else if(b>=Math.max(a,c)){
                            predicted[i][j]=Math.min(a,c);
                        }else{
                            predicted[i][j]=a+c-b;
                        }
                        difference[i][j]=originalGrayScaleImage[i][j]-predicted[i][j];
                    }
                }
            }
            Table quantizerTable=generateQuantizer(difference,originalHeight,originalWidth,8);
            for (int i = 1; i < originalHeight; i++) {
                for (int j = 1; j < originalWidth; j++) {
                    Row row=getRowByNumber(difference[i][j],quantizerTable);
                    quantizedDifference[i][j]=row.getQ();
                }
            }
            for (int i = 0; i < originalHeight; i++) {
                for (int j = 0; j < originalWidth; j++) {
                    dataOutputStream.writeInt(quantizedDifference[i][j]);
                }
            }
            dataOutputStream.writeInt(quantizerTable.getRows().size());
            for (Row row : quantizerTable.getRows()) {
                dataOutputStream.writeInt(row.getQ());
                dataOutputStream.writeInt(row.getQ_());
            }
           

            dataOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static Row getRowByNumber(int number,Table table){
        for (Row row : table.getRows()) {
            int start=row.getStart();
            int end=row.getEnd();
            if(start<=number&&number<=end){
                return row;
            }
        }
        return null;

    }
    private static Row getRowByQ(int q,Table table){
        for (Row row : table.getRows()) {
            if(q==row.getQ()){
                return row;
            }
        }
        return null;

    }
    
    
    private static Table generateQuantizer(int [][]diff,int height,int width,int bits){
        int max=getMax(diff, height, width);
        int min=getMin(diff, height, width);
        double levels=Math.pow(2, bits);
        int step=(int)Math.ceil((double)(max-min-1)/levels);
        Table table=new Table();
        int start=min;
        for(int i = 0; i < levels; i++){
            Row row=new Row();
            row.setStart(start);
            row.setEnd((start + step-1));
            row.setQ_((int)Math.ceil(((double)(start+(start+step-1)))/2));
            row.setQ(i);
            table.setRow(row);
            start+=step;
        }
        return table;
    }
    private static int getMax(int [][] arr,int height,int width){
        //return max int the the array
        int max = Integer.MIN_VALUE;
        for(int i = 1; i < height; i++){
            for(int j = 1; j < width; j++){
                if(arr[i][j] > max){
                    max = arr[i][j];
                }
            }
        }
        return max;
    }
    private static int getMin(int [][] arr,int height,int width){
        //return min int the the array
        int min = Integer.MAX_VALUE;
        for(int i = 1; i < height; i++){
            for(int j = 1; j < width; j++){
                if(arr[i][j] < min){
                    min = arr[i][j];
                }
            }
        }
        return min;
    }

    public static BufferedImage Decompress(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file);
                DataInputStream dataInputStream = new DataInputStream(fileInputStream)) {

            int width = dataInputStream.readInt();
            int height = dataInputStream.readInt();
            int [][] quantizedDifference=new int[height][width];
            int [][] dequantizedDifference=new int[height][width];
            int [][] decodeImage=new int[height][width];
           for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    int x=dataInputStream.readInt();
                    if(i==0||j==0){
                        dequantizedDifference[i][j]=x;
                    }
                    quantizedDifference[i][j]=x;
                }
            }
            Table quantizerTable=new Table();
            int size=dataInputStream.readInt();
            for (int i = 0; i < size; i++) {
                Row row=new Row();
                row.setQ(dataInputStream.readInt());
                row.setQ_(dataInputStream.readInt());
                quantizerTable.setRow(row);
            }
            for (int i = 1; i < height; i++) {
                for (int j = 1; j < width; j++) {
                    dequantizedDifference[i][j]=getRowByQ(quantizedDifference[i][j], quantizerTable).getQ_();
                }
            }
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    if(i==0||j==0){
                        decodeImage[i][j]=quantizedDifference[i][j];
                    }else{
                        int b=decodeImage[i-1][j-1];
                        int a=decodeImage[i][j-1];
                        int c=decodeImage[i-1][j];
                        int x;
                        if(b<=Math.min(a,c)){
                            x=Math.max(a,c);
                        }else if(b>=Math.max(a,c)){
                            x=Math.min(a,c);
                        }else{
                            x=a+c-b;
                        }
                        decodeImage[i][j]=x+dequantizedDifference[i][j];
                    }
                    
                }
                
            }

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    int rgb = (decodeImage[i][j] << 16) | (decodeImage[i][j] << 8) | decodeImage[i][j];
                    image.setRGB(j, i, rgb);
                }
            }
            
            
            return image;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
