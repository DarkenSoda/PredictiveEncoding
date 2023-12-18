import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.Math;
import javafx.util.Pair;

public class PredictiveEncoding {
    public static void Compress(BufferedImage image, int levels, File outputFile) {
        int[][] originalGrayScaleImage = RWCompression.ReadImageGrayScale(image);
        int originalHeight = image.getHeight();
        int originalWidth = image.getWidth();

        int[][] predicted = new int[originalHeight][originalWidth];
        int[][] difference = new int[originalHeight][originalWidth];
        int[][] quantizedDifference = new int[originalHeight][originalWidth];
        for (int i = 0; i < originalHeight; i++) {
            for (int j = 0; j < originalWidth; j++) {
                if (i == 0 || j == 0) {
                    predicted[i][j] = originalGrayScaleImage[i][j];
                    difference[i][j] = originalGrayScaleImage[i][j];
                    quantizedDifference[i][j] = originalGrayScaleImage[i][j];
                } else {
                    int b = originalGrayScaleImage[i - 1][j - 1];
                    int a = originalGrayScaleImage[i - 1][j];
                    int c = originalGrayScaleImage[i][j - 1];
                    if (b <= Math.min(a, c)) {
                        predicted[i][j] = Math.max(a, c);
                    } else if (b >= Math.max(a, c)) {
                        predicted[i][j] = Math.min(a, c);
                    } else {
                        predicted[i][j] = a + c - b;
                    }
                    difference[i][j] = originalGrayScaleImage[i][j] - predicted[i][j];
                }
            }
        }

        Table quantizerTable = generateQuantizer(difference, originalHeight, originalWidth, levels);
        for (int i = 1; i < originalHeight; i++) {
            for (int j = 1; j < originalWidth; j++) {
                Row row = getRowByNumber(difference[i][j], quantizerTable);
                quantizedDifference[i][j] = row.getQ();
            }
        }

        try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream)) {
            dataOutputStream.writeInt(originalWidth);
            dataOutputStream.writeInt(originalHeight);
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

    private static Row getRowByNumber(int number, Table table) {
        for (Row row : table.getRows()) {
            int start = row.getStart();
            int end = row.getEnd();
            if (start <= number && number <= end) {
                return row;
            }
        }
        return null;
    }

    private static Row getRowByQ(int q, Table table) {
        for (Row row : table.getRows()) {
            if (q == row.getQ()) {
                return row;
            }
        }
        return null;
    }

    private static Table generateQuantizer(int[][] diff, int height, int width, int levels) {
        Pair<Integer, Integer> min_max = getMinMax(diff, height, width);
        int min = min_max.getKey();
        int max = min_max.getValue();

        int step = (int) Math.ceil((double) (max - min) / levels);
        Table table = new Table();
        int start = min;
        for (int i = 0; i < levels; i++) {
            Row row = new Row();
            if (i + 1 == levels) {
                row.setEnd(min_max.getValue());
                row.setQ_((int) Math.ceil((double) (start + min_max.getValue()) / 2));
            } else {
                row.setEnd(start + step - 1);
                row.setQ_((int) Math.ceil((double) (start + row.getEnd()) / 2));
            }
            row.setStart(start);
            row.setQ(i);
            table.addRow(row);
            start += step;
        }
        return table;
    }

    private static Pair<Integer, Integer> getMinMax(int[][] arr, int height, int width) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int i = 1; i < height; i++) {
            for (int j = 1; j < width; j++) {
                if (arr[i][j] > max) {
                    max = arr[i][j];
                }
                if (arr[i][j] < min) {
                    min = arr[i][j];
                }
            }
        }
        return new Pair<Integer, Integer>(min, max);
    }

    public static BufferedImage Decompress(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file);
                DataInputStream dataInputStream = new DataInputStream(fileInputStream)) {
            int width = dataInputStream.readInt();
            int height = dataInputStream.readInt();
            int[][] quantizedDifference = new int[height][width];
            int[][] dequantizedDifference = new int[height][width];
            int[][] decodeImage = new int[height][width];
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    int x = dataInputStream.readInt();
                    if (i == 0 || j == 0) {
                        dequantizedDifference[i][j] = x;
                    }
                    quantizedDifference[i][j] = x;
                }
            }

            Table quantizerTable = new Table();
            int size = dataInputStream.readInt();
            for (int i = 0; i < size; i++) {
                Row row = new Row();
                row.setQ(dataInputStream.readInt());
                row.setQ_(dataInputStream.readInt());
                quantizerTable.addRow(row);
            }

            for (int i = 1; i < height; i++) {
                for (int j = 1; j < width; j++) {
                    dequantizedDifference[i][j] = getRowByQ(quantizedDifference[i][j], quantizerTable).getQ_();
                }
            }

            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    if (i == 0 || j == 0) {
                        decodeImage[i][j] = quantizedDifference[i][j];
                    } else {
                        int b = decodeImage[i - 1][j - 1];
                        int a = decodeImage[i - 1][j];
                        int c = decodeImage[i][j - 1];
                        int x;
                        if (b <= Math.min(a, c)) {
                            x = Math.max(a, c);
                        } else if (b >= Math.max(a, c)) {
                            x = Math.min(a, c);
                        } else {
                            x = a + c - b;
                        }

                        decodeImage[i][j] = x + dequantizedDifference[i][j];
                        if (decodeImage[i][j] < 0)
                            decodeImage[i][j] = 0;
                        if (decodeImage[i][j] > 255)
                            decodeImage[i][j] = 255;
                    }
                }
            }

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    int rgb = 0xff000000 | (decodeImage[i][j] << 16) | (decodeImage[i][j] << 8) | decodeImage[i][j];
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
