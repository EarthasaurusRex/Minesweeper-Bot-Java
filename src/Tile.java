import org.opencv.core.Mat;
import org.opencv.core.Core;
import org.opencv.core.Scalar;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.round;

public class Tile {

    private final int[] tileSize;
    public final int[] coord;
    public int dataType;
    private boolean updated;

    // Initialize
    public Tile(int[] imageSize, int[] boardSize, int[] coord, int dataType) {
        // Available data types: 0 - 8 (# of mines in neighbors), 9 (Flagged), 10 (Unknown), 11 (Updated)
        this.dataType = dataType;
        this.updated = true;
        this.coord = coord;
        this.tileSize = new int[] {(int) (((double) imageSize[0]) / boardSize[0]), (int) (((double) imageSize[1]) / boardSize[1])};
    }

    // Clicks the tile
    public void click(int[] boundingBox, String clickType) throws AWTException, InterruptedException {
        Robot robot = new Robot();
        int[] tilePosition = new int[] {(int) (this.tileSize[0] * (this.coord[0] + 1.0 / 2)) + boundingBox[0],
                (int) (this.tileSize[1] * ( this.coord[1] + 1.0 / 2)) + boundingBox[1]};
        robot.mouseMove(tilePosition[0], tilePosition[1]);
        if (clickType.equalsIgnoreCase("left")) {
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        }
        else if (clickType.equalsIgnoreCase("right")) {
            robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
            robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
        }
//         TimeUnit.MILLISECONDS.sleep(50);
//         robot.mouseMove(0, 0);
    }

    // Check if item in array
    public boolean check(double[][] array, double[] toCheck) {
        List<double[]> list = Arrays.asList(array);
        boolean contains = false;
        for (double[] arr : list) {
            if (Arrays.equals(arr, toCheck)) {
                contains = true;
            }
        }
//        Arrays.sort(array, Comparator.comparingDouble(a -> a[0]));
//        int res = Arrays.binarySearch(array, toCheck);
//        boolean test = res > 0 ? true : false;
//        boolean test = Arrays.asList(array).contains(toCheck);
        return contains;
    }

    // Update data type
    public void updateDataType(Mat board, Mat prevBoard) {
        if (this.dataType == 9) {
            return;
        }
        // Get Tile
        Mat tileImage = board.submat(
                this.tileSize[1] * this.coord[1] + 5,
                this.tileSize[1] * (this.coord[1] + 1) - 5,
                this.tileSize[0] * this.coord[0] + 5,
                this.tileSize[0] * (this.coord[0] + 1) - 5
        );
        Mat prevTileImage = prevBoard.submat(
                this.tileSize[1] * this.coord[1] + 5,
                this.tileSize[1] * (this.coord[1] + 1) - 5,
                this.tileSize[0] * this.coord[0] + 5,
                this.tileSize[0] * (this.coord[0] + 1) - 5
        );

        // Check if image is updated
//        Mat diff = new Mat();
//        Core.subtract(tileImage, prevTileImage, diff);
//        System.out.println(Arrays.toString(diff.get(0,0)));
//        boolean eq = (diff.get(1, 0)[0] == 0) && (diff.get(0, 0)[1] == 0) && (diff.get(0, 0)[2] == 0);
//        if (eq) {
//            return;
//        }

        // Create mask to get number only
        Scalar light = new Scalar(159, 194, 229);
        Scalar dark = new Scalar(153, 184, 215);
        Mat mask = new Mat();
        Core.inRange(tileImage, dark, light, mask);
        Core.bitwise_not(mask, mask);
        Mat masked = new Mat();
        Core.bitwise_and(tileImage, tileImage, masked, mask);

//        HighGui.imshow("Masked", masked);
//        HighGui.waitKey(0);
//        HighGui.destroyAllWindows();

        // Get most dominant color
//         num_copy = num.copy();
//         unique, counts = numpy.unique(num_copy.reshape(-1,   3), axis=0, return_counts=True);
//         try:
//         	num_copy[:, :, 0], num_copy[:, :, 1], num_copy[:, :, 2] = unique[numpy.argsort(counts)[-2]];
//         except:
//         	num_copy[:, :, 0], num_copy[:, :, 1], num_copy[:, :, 2] = unique[numpy.argsort(counts)[-1]];
//         bgr = num_copy[round(num_copy.shape[0] / 2), round(num_copy.shape[1] / 2), :];

        // Get all pixel data
        double[][] pixels = new double[this.tileSize[0] * this.tileSize[1]][3];
        for (int row = 0; row < tileSize[0]; row++) {
            for (int col = 0; col < tileSize[1]; col++) {
                try {
                    double[] pixel = masked.get(row, col);
                    pixels[row * this.tileSize[0] + col] = pixel;
                } catch (Exception e) {}
            }
        }

//        System.out.println("Tile: (" + tileSize[0] + ", " + tileSize[1] + ")");
//        for (double[] arr : pixels) {
//            System.out.println(Arrays.toString(arr));
//        }

        // Identify data type
        if (check(pixels, new double[] {210, 118, 25})) {
            this.dataType = 1;
        }
		else if (check(pixels, new double[] {60, 142, 56})) {
            this.dataType = 2;
        }
		else if (check(pixels, new double[] {47, 47, 211})) {
            this.dataType = 3;
        }
		else if (check(pixels, new double[] {162, 31, 123})) {
            this.dataType = 4;
        }
		else if (check(pixels, new double[] {0, 143, 255})) {
            this.dataType = 5;
        }
		else if (check(pixels, new double[] {167, 151, 0})){
            this.dataType = 6;
        }
//        else if (check(pixels, new double[] {7, 54, 242})) {
//             this.data_type = 9;
//        }
		else if (check(pixels, new double[] {81, 215, 170}) || check(pixels, new double[] {73, 209, 162})) {
            this.dataType = 10;
        }
		else if (check(pixels, new double[] {0, 0, 0})) {
            this.dataType = 0;
        }
    }
}