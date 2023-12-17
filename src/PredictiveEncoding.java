import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.Math;
import java.util.ArrayList;
import java.util.List;

import javafx.util.Pair;

public class PredictiveEncoding {
    public static void Compress(BufferedImage image, int levels, File outputFile) {
        int[][][] originalRGBImage = RWCompression.ReadImageRGB(image);
        int originalHeight = image.getHeight();
        int originalWidth = image.getWidth();

        int[][][] predicted = new int[originalHeight][originalWidth][3];
        int[][][] difference = new int[originalHeight][originalWidth][3];
        int[][][] quantizedDifference = new int[originalHeight][originalWidth][3];
        for (int i = 0; i < originalHeight; i++) {
            for (int j = 0; j < originalWidth; j++) {
                if (i == 0 || j == 0) {
                    for (int k = 0; k < 3; k++) {
                        predicted[i][j][k] = originalRGBImage[i][j][k];
                        difference[i][j][k] = originalRGBImage[i][j][k];
                        quantizedDifference[i][j][k] = originalRGBImage[i][j][k];
                    }
                } else {
                    for (int k = 0; k < 3; k++) {
                        int b = originalRGBImage[i - 1][j - 1][k];
                        int a = originalRGBImage[i - 1][j][k];
                        int c = originalRGBImage[i][j - 1][k];
                        if (b <= Math.min(a, c)) {
                            predicted[i][j][k] = Math.max(a, c);
                        } else if (b >= Math.max(a, c)) {
                            predicted[i][j][k] = Math.min(a, c);
                        } else {
                            predicted[i][j][k] = a + c - b;
                        }
                        difference[i][j][k] = originalRGBImage[i][j][k] - predicted[i][j][k];
                    }
                }
            }
        }

        Table quantizerTable = generateQuantizer(difference, originalHeight, originalWidth, levels);
        for (int i = 1; i < originalHeight; i++) {
            for (int j = 1; j < originalWidth; j++) {
                for (int k = 0; k < 3; k++) {
                    quantizedDifference[i][j][k] = getRowByNumber(difference[i][j][k], quantizerTable, k).Q[k];
                }
            }
        }

        try (FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream)) {
            dataOutputStream.writeInt(originalWidth);
            dataOutputStream.writeInt(originalHeight);
            for (int i = 0; i < originalHeight; i++) {
                for (int j = 0; j < originalWidth; j++) {
                    for (int k = 0; k < 3; k++) {
                        dataOutputStream.writeInt(quantizedDifference[i][j][k]);
                    }
                }
            }
            dataOutputStream.writeInt(quantizerTable.getRows().size());
            for (Row row : quantizerTable.getRows()) {
                for (int k = 0; k < 3; k++) {
                    dataOutputStream.writeInt(row.Q[k]);
                    dataOutputStream.writeInt(row.Q_[k]);
                }
            }

            dataOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Row getRowByNumber(int number, Table table, int RGB) {
        for (Row row : table.getRows()) {
            int start = row.start[RGB];
            int end = row.end[RGB];
            if (start <= number && number <= end) {
                return row;
            }
        }
        return null;
    }

    private static Row getRowByQ(int q, Table table, int RGB) {
        for (Row row : table.getRows()) {
            if (q == row.Q[RGB]) {
                return row;
            }
        }
        return null;
    }

    private static Table generateQuantizer(int[][][] diff, int height, int width, int levels) {
        List<Pair<Integer, Integer>> min_max = new ArrayList<Pair<Integer, Integer>>();
        int[] steps = new int[3];
        int[] starts = new int[3];
        for (int i = 0; i < 3; i++) {
            min_max.add(getMinMax(diff, height, width, i));
            int min = min_max.get(i).getKey();
            int max = min_max.get(i).getValue();
            steps[i] = (int) Math.ceil((double) (max - min + 1) / levels);
            starts[i] = min;
        }

        Table table = new Table();
        for (int i = 0; i < levels; i++) {
            Row row = new Row();
            for (int j = 0; j < 3; j++) {
                row.start[j] = starts[j];
                row.end[j] = starts[j] + steps[j] - 1;
                row.Q_[j] = (int) Math.ceil((double) (starts[j] + row.end[j]) / 2);
                row.Q[j] = i;
                starts[j] += steps[j];
            }
            table.addRow(row);
        }
        return table;
    }

    private static Pair<Integer, Integer> getMinMax(int[][][] arr, int height, int width, int RGB) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int i = 1; i < height; i++) {
            for (int j = 1; j < width; j++) {
                if (arr[i][j][RGB] > max) {
                    max = arr[i][j][RGB];
                }
                if (arr[i][j][RGB] < min) {
                    min = arr[i][j][RGB];
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
            int[][][] quantizedDifference = new int[height][width][3];
            int[][][] dequantizedDifference = new int[height][width][3];
            int[][][] decodeImage = new int[height][width][3];
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    for (int k = 0; k < 3; k++) {
                        int x = dataInputStream.readInt();
                        if (i == 0 || j == 0) {
                            dequantizedDifference[i][j][k] = x;
                        }
                        quantizedDifference[i][j][k] = x;
                    }
                }
            }

            Table quantizerTable = new Table();
            int size = dataInputStream.readInt();
            for (int i = 0; i < size; i++) {
                Row row = new Row();
                for (int k = 0; k < 3; k++) {
                    row.Q[k] = dataInputStream.readInt();
                    row.Q_[k] = dataInputStream.readInt();
                }
                quantizerTable.addRow(row);
            }

            for (int i = 1; i < height; i++) {
                for (int j = 1; j < width; j++) {
                    for (int k = 0; k < 3; k++) {
                        dequantizedDifference[i][j][k] = getRowByQ(quantizedDifference[i][j][k], quantizerTable, k).Q_[k];
                    }
                }
            }

            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    for (int k = 0; k < 3; k++) {
                        if (i == 0 || j == 0) {
                            decodeImage[i][j][k] = quantizedDifference[i][j][k];
                        } else {
                            int b = decodeImage[i - 1][j - 1][k];
                            int a = decodeImage[i - 1][j][k];
                            int c = decodeImage[i][j - 1][k];
                            int x;
                            if (b <= Math.min(a, c)) {
                                x = Math.max(a, c);
                            } else if (b >= Math.max(a, c)) {
                                x = Math.min(a, c);
                            } else {
                                x = a + c - b;
                            }

                            decodeImage[i][j][k] = x + dequantizedDifference[i][j][k];
                        }
                    }
                }
            }

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    int red = decodeImage[i][j][0] & 0xFF;
                    int green = decodeImage[i][j][1] & 0xFF;
                    int blue = decodeImage[i][j][2] & 0xFF;

                    int rgb = (red << 16) | (green << 8) | blue;
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
