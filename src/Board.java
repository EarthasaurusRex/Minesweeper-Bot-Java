import org.opencv.core.Mat;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Board {
    private final int[] boardSize;
    private final int[] bounding_box;
    private final int[] image_size;
    private final Tile[][] tiles;
    private Mat board;
    private final long timeout;
    private Mat prevBoard;
    private int normalIdle;
    private WindowCapture windowCapture = new WindowCapture();

    // Initialize
    public Board(int[] boardSize, int[] bounding_box, long timeout) throws Exception {
        this.timeout = timeout;
        this.prevBoard = null;
        this.normalIdle = 0;
        this.boardSize = boardSize;
        this.bounding_box = bounding_box;
        Mat image = windowCapture.getScreenshot(this.bounding_box);
        this.board = image;

//        HighGui.imshow("board", this.board);
//        HighGui.waitKey(0);
//        HighGui.destroyWindow("board");

        this.image_size = new int[]{bounding_box[2] - bounding_box[0], bounding_box[3] - bounding_box[1]};
        this.tiles = new Tile[boardSize[0]][boardSize[1]];
        for (int x = 0; x < boardSize[0]; x++) {
            for (int y = 0; y < boardSize[1]; y++) {
                this.tiles[x][y] = new Tile(this.image_size, this.boardSize, new int[]{x, y}, 10);
            }
        }
    }

    // Click a specific tile
    public void click(int[] coord, String click_type) throws AWTException, InterruptedException {
        Tile tile = this.tiles[coord[0]][coord[1]];
        tile.click(this.bounding_box, click_type);
    }

    // Click a random unknown tile
    public void randomClick() throws AWTException, InterruptedException, IOException {
        this.updateBoard();
        while (true) {
            Tile tile = this.tiles[ThreadLocalRandom.current().nextInt(0, this.boardSize[0])][ThreadLocalRandom.current().nextInt(0, this.boardSize[1])];
            if (tile.dataType == 10) {
                tile.click(this.bounding_box, "left");
                break;
            }
        }
        TimeUnit.MILLISECONDS.sleep(1000);
        this.update();
    }

    // Get neighbors of tile
    class Neighbors {
        ArrayList<Tile> neighborTiles;
        int flaggedCount;
        int unknownCount;
    }
    public Neighbors getNeighbors(Tile tile, boolean adj) {
        int[][] neighborOffsets;
        if (adj) {
            neighborOffsets = new int[][]{{0, -1}, {-1, 0}, {1, 0}, {0, 1}};
        } else {
            neighborOffsets = new int[][]{{-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1}};
        }
        ArrayList<Tile> neighborTiles = new ArrayList<Tile>();
        for (int[] offset : neighborOffsets) {
            try {
                int[] neighbor = {tile.coord[0] + offset[0], tile.coord[1] + offset[1]};
                for (int num : neighbor) {
                    if (num < 0) {
                        throw new Exception();
                    }
                }
                neighborTiles.add(this.tiles[neighbor[0]][neighbor[1]]);
            } catch (Exception e) {
            }
        }
        ArrayList<Integer> dataTypes = new ArrayList<Integer>();
        for (Tile neighborTile : neighborTiles) {
            dataTypes.add(neighborTile.dataType);
        }

        // Check how many unknown tiles in neighbors
        int unknownCount = 0;
        int flaggedCount = 0;
        for (int data : dataTypes) {
            if (data == 9) {
                flaggedCount += 1;
            } else if (data == 10) {
                unknownCount += 1;
            }
        }
        Neighbors neighbors = new Neighbors();
        neighbors.neighborTiles = neighborTiles;
        neighbors.flaggedCount = flaggedCount;
        neighbors.unknownCount = unknownCount;
        return neighbors;
    }

    // Set tile as flagged
    public void flag(int[] coord) throws AWTException, InterruptedException {
        Tile tile = this.tiles[coord[0]][coord[1]];
        tile.dataType = 9;
        tile.click(this.bounding_box, "right");
        this.normalIdle = 0;
    }

    // Mine a tile
    public void mine(int[] coord) throws AWTException, InterruptedException {
        Tile tile = this.tiles[coord[0]][coord[1]];
        tile.click(this.bounding_box, "left");
        this.normalIdle = 0;
    }

    // Update image
    public void updateBoard() throws AWTException, IOException {
        this.prevBoard = this.board;
        this.board = windowCapture.getScreenshot(this.bounding_box);
//        HighGui.imshow("PrevBoard", this.prevBoard);
//        HighGui.imshow("Board", this.board);
//        HighGui.waitKey(0);
//        HighGui.destroyAllWindows();
        // cv2.imshow("Previous", this.prev_board);
        // cv2.imshow("Current", this.board);
        // cv2.waitKey(0);
        // cv2.destroyAllWindows();
    }

    // Update all tiles
    public void updateDataTypes() throws AWTException, InterruptedException, IOException {
        this.updateBoard();
        ArrayList<Thread> procs = new ArrayList<Thread>();
        for (Tile[] column : this.tiles) {
            for (Tile tile : column) {
                Thread p = new Thread(() -> tile.updateDataType(this.board, this.prevBoard));
                procs.add(p);
                p.start();
            }
        }
        for (Thread proc : procs) {
            proc.join();
        }
//        for (Tile[] column : this.tiles) {
//            for (Tile tile : column) {
//                tile.updateDataType(this.board, this.prevBoard);
//            }
//        }
    }

    // Update board
    public void update() throws InterruptedException, AWTException, IOException {
        this.updateDataTypes();
        this.normalIdle += 1;
        // Normal logic
        for (Tile[] column : this.tiles) {
            for (Tile tile : column) {
                if (tile.dataType == 0 || tile.dataType >= 9) {
                    continue;
                }
                // Get tile neighbors
                Neighbors neighborInfo = this.getNeighbors(tile, false);
                ArrayList<Tile> neighborTiles = neighborInfo.neighborTiles;
                int flaggedCount = neighborInfo.flaggedCount;
                int unknownCount = neighborInfo.unknownCount;

                ArrayList<Integer> neighborDataTypes = new ArrayList<Integer>();
                for (Tile neighborTile: neighborTiles) {
                    neighborDataTypes.add(neighborTile.dataType);
                }

                // Flag mines
                // If unknowns equal to data type, click all unknowns
                if (tile.dataType - flaggedCount == unknownCount) {
                    for (Tile neighborTile : neighborTiles) {
                        if (neighborTile.dataType == 10) {
                            this.flag(neighborTile.coord);
                            flaggedCount += 1;
                            System.out.println("Flagging...");
                            System.out.println("Current tile: " + Arrays.toString(tile.coord));
                            System.out.println("Current tile type: " + tile.dataType + "\tNeighbors: " + neighborDataTypes.toString());
                            System.out.println("Flagged tile " + Arrays.toString(neighborTile.coord) + "\n");
                            this.normalIdle = 0;
                        }
                    }
                }

                // Click non-mines
                if (tile.dataType - flaggedCount == 0) {
                    for (Tile neighborTile : neighborTiles) {
                        if (neighborTile.dataType == 10) {
                            this.mine(neighborTile.coord);
                            neighborTile.updateDataType(this.board, this.prevBoard);
                            System.out.println("Mining...");
                            System.out.println("Current tile: " + Arrays.toString(tile.coord));
                            System.out.println("Current tile type: " + tile.dataType + "\tNeighbors: " + neighborDataTypes.toString());
                            System.out.println("Clicked tile " + Arrays.toString(neighborTile.coord) + "\n");
                            this.normalIdle = 0;
                        }
                    }
                }
                TimeUnit.MILLISECONDS.sleep(this.timeout);
            }
        }

        // CSP
        if (this.normalIdle > 0) {
            this.updateDataTypes();
            HashSet<int[]> fringeTiles = new HashSet();
            ArrayList<int[]> adjTiles = new ArrayList<>();
            boolean term = false;
            for (Tile[] column : this.tiles) {
                for (Tile tile : column) {
                    if (tile.dataType < 9) {
                        // Get tile neighbors
                        Neighbors neighborTiles = this.getNeighbors(tile, false);

                        // Check if current tile is fringe tile
                        boolean is_adj = false;
                        for (Tile neighbor_tile : neighborTiles.neighborTiles) {
                            if (neighbor_tile.dataType == 10) {
                                is_adj = true;
//                                if (fringeTiles.size() >= 16) {
//                                    term = true;
//                                    break;
//                                }
                                fringeTiles.addAll(neighborTiles.neighborTiles.stream()
                                        .filter(i -> i.dataType == 10)
                                        .map(i -> i.coord)
                                        .collect(Collectors.toList()));
                            }
                        }
                        if (is_adj) {
                            adjTiles.add(tile.coord);
//                            if (adjTiles.size() >= 16) {
//                                term = true;
//                                break;
//                            }
                        }
                    }
                    if (term) {
                        break;
                    }
                }
            }
            if (fringeTiles.size() >= 16) {
                int rand_num = ThreadLocalRandom.current().nextInt(0, len(fringeTiles));
                fringeTiles.
                List<List<Integer>> fringe_array = new ArrayList<>(fringeTiles.subList(0, round(fringeTiles.size() / 2)));
            } else {
                List<List<Integer>> fringe_array = new ArrayList<>(fringeTiles);
            }

            Map<int[], Integer> fringeVar = new HashMap<>();
            for (int[] fringeTile : fringeTiles) {
                fringeVar.put(fringeTile, 0);
            }

            System.out.println(fringeTiles.size() + " Fringe Variables: " + fringeVar);

            // Generate permutations
            System.out.println("Generating permutations...");
            List<String> permutations = new ArrayList<>();
            for (int i = 0; i < Math.pow(2, fringeTiles.size()); i++) {
                permutations.add(String.format("%1$" + fringeTiles.size() + "s", Integer.toBinaryString(i)).replace(' ', '0'));
            }

            // Test permutations
            System.out.println("Testing permutations...");

            List<String> validPermutations = new ArrayList<>();

            for (String permutation : permutations) {
                System.out.println(((double)permutations.indexOf(permutation)) / permutations.size() * 100 + "% done " + permutation);
                for (int i = 0; i < permutation.length(); i++) {
                    fringeVar.replace((int[]) fringeTiles.toArray()[i], Integer.parseInt(permutation.substring(i, i + 1)));
                }
                boolean valid = true;
                for (int[] adjTile : adjTiles) {
                    Neighbors neighbors = this.getNeighbors(this.tiles[adjTile[0]][adjTile[1]], false);
                    List<int[]> n_array = new ArrayList<>();
                    for (Tile neighbor : neighbors.neighborTiles) {
                        n_array.add(neighbor.coord);
                    }
                    List<int[]> intersect = n_array.stream()
                            .filter(fringeTiles::contains)
                            .collect(Collectors.toList());
                    int mineSum = intersect.stream()
                            .mapToInt(fringeVar::get)
                            .sum();
                    if (this.tiles[adjTile[0]][adjTile[1]].dataType != mineSum + neighbors.flaggedCount) {
                        valid = false;
                        break;
                    }
                }
                if (valid) {
                    validPermutations.add(permutation);
                }
            }

            System.out.println("100% done");
            if (validPermutations.size() < 1) {
                return;
            }

            List<List<Integer>> intValidPerms = new ArrayList<>();

            for (int i = 0; i < validPermutations.size(); i++) {
                intValidPerms.set(i, Arrays.asList(validPermutations.get(i).split("")).stream().map(Integer::parseInt).collect(Collectors.toList()));
            }

            boolean[] flag_truth = new boolean[fringeTiles.size()];
            Arrays.fill(flag_truth, true);
            for (List<Integer> permutation : intValidPerms) {
                for (int i = 0; i < permutation.size(); i++) {
                    if (permutation.get(i) != 1) {
                        flag_truth[i] = false;
                    }
                }
            }

            boolean[] open_truth = new boolean[fringeTiles.size()];
            Arrays.fill(open_truth, true);
            for (List<Integer> permutation : intValidPerms) {
                for (int i = 0; i < permutation.size(); i++) {
                    if (permutation.get(i) != 0) {
                        open_truth[i] = false;
                    }
                }
            }

            for (int i = 0; i < fringeTiles.size(); i++) {
                if (flag_truth[i]) {
                    if (this.tiles[((int[]) fringeTiles.toArray()[i])[0]][((int[]) fringeTiles.toArray()[i])[1]].dataType == 9) {
                        continue;
                    }
                    this.flag((int[]) fringeTiles.toArray()[i]);
                    System.out.println("Flagging...");
                    System.out.println("Flagged tile " + Arrays.toString((int[]) fringeTiles.toArray()[i]) + "\n");
                }
                if (open_truth[i]) {
                    this.mine((int[]) fringeTiles.toArray()[i]);
                    System.out.println("Mining...");
                    System.out.println("Clicked tile " + Arrays.toString((int[]) fringeTiles.toArray()[i]) + "\n");
                }
            }
        }
    }
}