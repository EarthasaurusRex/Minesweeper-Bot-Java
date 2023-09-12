import com.github.kwhat.jnativehook.NativeHookException;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Scanner;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

public class Main {

    Robot robot = new Robot();

    static {
        System.loadLibrary("opencv_java460");
    }

    public Main() throws AWTException {
    }

    public static Board init() throws Exception {
        System.out.println("This minesweeper bot is for Google minesweeper.");

        // Board sizes
        int[] easySize = {10, 8};
        int[] mediumSize = {18, 14};
        int[] hardSize = {24, 20};
        int[][] sizes = {easySize, mediumSize, hardSize};

        int[] boardSize;
        Scanner scanner = new Scanner(System.in);
        while (true) {
            try {
                System.out.println("What mode are you playing on?\n0: Easy\n1: Medium\n2: Hard\n");
                int sizeSelect = Integer.parseInt(scanner.nextLine());
                if (0 <= sizeSelect && sizeSelect < 3) {
                    boardSize = sizes[sizeSelect];
                    break;
                } else {
                    System.out.println("That is not a valid mode.");
                }
            } catch (Exception e) {
                System.out.println("That is not a valid mode.");
            }
        }

        // Get bounding box of board
        int[] boundingBox = {0, 0, 0, 0};

        System.out.println("Move your mouse to the top left corner and press Enter");
        scanner.nextLine();
        boundingBox[0] = MouseInfo.getPointerInfo().getLocation().x;
        boundingBox[1] = MouseInfo.getPointerInfo().getLocation().y;

        System.out.println("Move your mouse to the bottom right corner and press Enter");
        scanner.nextLine();
        boundingBox[2] = MouseInfo.getPointerInfo().getLocation().x;
        boundingBox[3] = MouseInfo.getPointerInfo().getLocation().y;

        Board board = new Board(boardSize, boundingBox, 150);
        board.randomClick();
        return board;
    }

    public static Mat BufferedImage2Mat(BufferedImage image) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", byteArrayOutputStream);
        byteArrayOutputStream.flush();
        return Imgcodecs.imdecode(new MatOfByte(byteArrayOutputStream.toByteArray()), Imgcodecs.IMREAD_UNCHANGED);
    }

    public static void listWindowNames() {
        User32 user32 = User32.INSTANCE;
        user32.EnumWindows(new WinUser.WNDENUMPROC() {
            public boolean callback(WinDef.HWND hwnd, Pointer pointer) {
                User32 user32 = User32.INSTANCE;
                if (user32.IsWindowVisible(hwnd)) {
                    char[] title = new char[user32.GetWindowTextLength(hwnd) + 1];
                    user32.GetWindowText(hwnd, title, title.length);
                    String wTitle = Native.toString(title);
                    System.out.println(hwnd + " " + wTitle);
                }
                return true;
            }
        }, null);
    }

    public static void main(String args[]) throws Exception {
        KillSwitch.main();
        Board board = init();

        while (true) {
            System.out.println("Updating");
            board.update();
        }
    }
}

