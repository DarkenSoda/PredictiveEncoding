import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.util.converter.IntegerStringConverter;

public class PredictiveController {
    // #region FXML Fields
    @FXML
    private TextField levelsText;

    @FXML
    private Button selectOriginalImageButton;

    @FXML
    private Button compressButton;

    @FXML
    private ImageView originalImage;

    @FXML
    private ImageView decompressedImage;

    @FXML
    private Button decompressButton;

    @FXML
    private Button saveDecompressedImage;

    @FXML
    private Button selectBinaryFileButton;

    @FXML
    private Label openedFileLabel;
    // #endregion

    private File fileToDecompress;

    // #region FXML Functions
    @FXML
    void onCompress(ActionEvent event) {
        if (originalImage.getImage() == null)
            return;

        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter textFilter = new FileChooser.ExtensionFilter("Binary Files", "*.bin");
        fileChooser.getExtensionFilters().add(textFilter);
        fileChooser.setTitle("Save Compressed Binary File");

        File file = fileChooser.showSaveDialog(null);
        if (file == null)
            return;

        int levels;
        try {
            levels = Integer.parseInt(levelsText.getText());
        } catch (Exception e) {
            System.out.println("Invalid input!");
            return;
        }

        // Compress originalImage and save as bin file
        BufferedImage image = SwingFXUtils.fromFXImage(originalImage.getImage(), null);
        PredictiveEncoding.Compress(image, levels, file);
    }

    @FXML
    void onDecompress(ActionEvent event) {
        // Decompress fileToDecompress.bin
        if (fileToDecompress == null)
            return;

        BufferedImage image = PredictiveEncoding.Decompress(fileToDecompress);
        if (image == null)
            return;

        // Show result in decompressedImage
        decompressedImage.setImage(SwingFXUtils.toFXImage(image, null));
    }

    @FXML
    void onSaveImage(ActionEvent event) {
        // Save decompressedImage.jpg
        if (decompressedImage.getImage() == null)
            return;

        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter textFilter = new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.png",
                "*.jpeg");
        fileChooser.getExtensionFilters().add(textFilter);
        fileChooser.setTitle("Save Image");

        File file = fileChooser.showSaveDialog(null);
        if (file == null)
            return;

        Image imageToBeSaved = decompressedImage.getImage();
        try {
            String extension = getFileExtension(file);

            BufferedImage image = new BufferedImage((int) imageToBeSaved.getWidth(), (int) imageToBeSaved.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            g2d.setColor(java.awt.Color.WHITE);
            g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
            g2d.drawImage(SwingFXUtils.fromFXImage(imageToBeSaved, null), 0, 0, null);
            g2d.dispose();

            ImageIO.write(image, extension.isEmpty() ? "jpg" : extension,
                    file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void onSelectBinFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter textFilter = new FileChooser.ExtensionFilter("Binary Files", "*.bin");
        fileChooser.getExtensionFilters().add(textFilter);
        fileChooser.setTitle("Select Binary File to Decompress");

        File file = fileChooser.showOpenDialog(null);
        if (file == null)
            return;

        openedFileLabel.setText("Opened File: " + file.getName());
        fileToDecompress = file;
    }

    @FXML
    void onSelectOriginalImage(ActionEvent event) throws IOException {
        FileChooser file = new FileChooser();
        FileChooser.ExtensionFilter imagFilter = new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg",
                "*.jpeg");
        file.getExtensionFilters().add(imagFilter);
        file.setTitle("Select Image to Compress");

        File selectedImage = file.showOpenDialog(null);
        if (selectedImage == null)
            return;

        InputStream stream = new FileInputStream(selectedImage.getAbsolutePath());
        Image image = new Image(stream);
        originalImage.setImage(image);
    }

    @FXML
    void initialize() {
        levelsText.setTextFormatter(new TextFormatter<>(new IntegerStringConverter()));
    }
    // #endregion

    private String getFileExtension(File file) {
        String name = file.getName();
        int i = name.lastIndexOf('.');
        if (i > 0) {
            return name.substring(i + 1);
        }

        return "";
    }
}
