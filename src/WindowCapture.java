import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinDef;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class WindowCapture {
    public static Mat BufferedImage2Mat(BufferedImage image) throws IOException {

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", byteArrayOutputStream);
        byteArrayOutputStream.flush();
        return Imgcodecs.imdecode(new MatOfByte(byteArrayOutputStream.toByteArray()), Imgcodecs.IMREAD_UNCHANGED);

    }
    public Mat getScreenshot(int[] boundingBox) throws AWTException, IOException {
        Rectangle bound = new Rectangle();
        bound.setRect(boundingBox[0], boundingBox[1], boundingBox[2] - boundingBox[0], boundingBox[3] - boundingBox[1]);
        BufferedImage img = new Robot().createScreenCapture(bound);
        return BufferedImage2Mat(img);
    }
}