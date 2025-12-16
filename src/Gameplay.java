import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import javax.swing.Timer;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class Gameplay extends JPanel implements ActionListener {

    // --- Konfigurasi Maze ---
    private final int CELL_SIZE = 30;
    private final int COLS = 20;
    private final int ROWS = 20;
    private final int MAZE_WIDTH = COLS * CELL_SIZE;  // 600px
    private final int MAZE_HEIGHT = ROWS * CELL_SIZE; // 600px

    // --- Konfigurasi LOG Area ---
    private final int LOG_WIDTH = 250;
    private final int TOTAL_WIDTH = MAZE_WIDTH + LOG_WIDTH + 40; // Extra padding
    private final int TOTAL_HEIGHT = MAZE_HEIGHT + 100;

    // --- WARNA TEMA & WEIGHT ---
    private final Color BG_COLOR = new Color(0, 0, 30);
    private final Color WALL_COLOR_OUTER = new Color(25, 25, 165);
    private final Color WALL_COLOR_INNER = new Color(100, 100, 255);

    // Warna Node (Weight) Gelap
    private final Color COLOR_WEIGHT_1 = new Color(19, 32, 59); // Dark Blue
    private final Color COLOR_WEIGHT_5 = new Color(50, 50, 60); // Dark Grey
    private final Color COLOR_WEIGHT_10 = new Color(45, 20, 45); // Dark Purple

    // --- Struktur Data ---
    private Cell[][] grid;
    private Cell startCell, endCell;

    // --- Variabel Logika ---
    private Stack<Cell> stack;
    private Queue<Cell> queue;
    private PriorityQueue<Cell> priorityQueue;

    // Class untuk menyimpan data tiap path (History)
    private class PathLayer {
        ArrayList<Cell> cells;
        Color color;
        String algoName;

        public PathLayer(ArrayList<Cell> cells, Color color, String algoName) {
            this.cells = cells;
            this.color = color;
            this.algoName = algoName;
        }
    }

    private ArrayList<PathLayer> pathHistory = new ArrayList<>();
    private ArrayList<Cell> currentAnimatingPath;
    private ArrayList<Cell> visitedOrder;

    private Timer timer;
    private int animationIndex = 0;
    private boolean isSolving = false;
    private String currentAlgo = "";

    // VARIABEL STATISTIK
    private double executionTimeMs = 0;
    private int pathTotalWeight = 0;

    // --- UI Control ---
    private JButton btnNewMaze, btnClearPaths, btnBFS, btnDFS, btnDijkstra, btnAStar, btnClearLog;
    private JTextArea logArea; // LOG COMPONENT
    private JScrollPane logScrollPane;

    // --- Images ---
    private BufferedImage pacmanImg;
    private BufferedImage dotImg;

    // GANTI PATH INI SESUAI KOMPUTER ANDA
    private String pacmanPath = "C:\\Users\\User\\Downloads\\pacman.png";
    private String dotPath = "C:\\Users\\User\\Downloads\\dot.png";

    public Gameplay() {
        this.setPreferredSize(new Dimension(TOTAL_WIDTH, TOTAL_HEIGHT));
        this.setLayout(null);
        this.setBackground(BG_COLOR);

        loadImages();
        initUIComponents();
        generateMaze();
    }

    private void loadImages() {
        try {
            pacmanImg = ImageIO.read(new File(pacmanPath));
            dotImg = ImageIO.read(new File(dotPath));
        } catch (IOException e) {
            // Fallback handled in paint
        }
    }

    private void initUIComponents() {
        // --- 1. SETUP LOG AREA (Sebelah Kanan) ---
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(10, 10, 20)); // Hampir hitam
        logArea.setForeground(new Color(0, 255, 0)); // Text hijau terminal
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        logArea.setMargin(new Insets(10, 10, 10, 10));

        logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBounds(MAZE_WIDTH + 20, 10, LOG_WIDTH, MAZE_HEIGHT);
        logScrollPane.setBorder(new LineBorder(WALL_COLOR_OUTER));

        // Header Log Awal
        logArea.append("=== SYSTEM READY ===\n");
        logArea.append("Waiting for inputs...\n\n");

        this.add(logScrollPane);

        // --- 2. SETUP TOMBOL (Di Bawah Maze) ---
        int btnY = MAZE_HEIGHT + 20;
        int btnY2 = MAZE_HEIGHT + 55;
        Color btnBg = new Color(50, 50, 50);

        // Row 1
        btnNewMaze = createStyledButton("New Maze", 10, btnY, 100, new Color(200, 50, 50), Color.WHITE);
        btnNewMaze.addActionListener(e -> generateMaze());

        btnClearPaths = createStyledButton("Clear Paths", 120, btnY, 100, new Color(200, 150, 0), Color.BLACK);
        btnClearPaths.addActionListener(e -> clearPaths());

        btnBFS = createStyledButton("BFS", 230, btnY, 80, btnBg, Color.CYAN);
        btnBFS.addActionListener(e -> startSolving("BFS"));

        btnDFS = createStyledButton("DFS", 320, btnY, 80, btnBg, Color.PINK);
        btnDFS.addActionListener(e -> startSolving("DFS"));

        // Row 2
        btnDijkstra = createStyledButton("Dijkstra", 230, btnY2, 80, btnBg, Color.GREEN);
        btnDijkstra.addActionListener(e -> startSolving("Dijkstra"));

        btnAStar = createStyledButton("A* Star", 320, btnY2, 80, btnBg, Color.ORANGE);
        btnAStar.addActionListener(e -> startSolving("AStar"));

        // Tombol Clear Log (Kecil di bawah log)
        btnClearLog = createStyledButton("Clear Log", MAZE_WIDTH + 20, MAZE_HEIGHT + 10, LOG_WIDTH, Color.DARK_GRAY, Color.WHITE);
        btnClearLog.addActionListener(e -> {
            logArea.setText("");
            logArea.append("=== LOG CLEARED ===\n\n");
        });

        this.add(btnNewMaze);
        this.add(btnClearPaths);
        this.add(btnBFS);
        this.add(btnDFS);
        this.add(btnDijkstra);
        this.add(btnAStar);
        this.add(btnClearLog);
    }

    private JButton createStyledButton(String text, int x, int y, int w, Color bg, Color fg) {
        JButton btn = new JButton(text);
        btn.setBounds(x, y, w, 30);
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Arial", Font.BOLD, 11));
        return btn;
    }

    // ==========================================
    // 1. GENERATE MAZE
    // ==========================================
    private void generateMaze() {
        if (timer != null) timer.stop();
        clearPaths();

        // Log System
        logArea.append("--------------------------\n");
        logArea.append("Generating New Maze...\n");

        grid = new Cell[COLS][ROWS];
        for (int x = 0; x < COLS; x++) {
            for (int y = 0; y < ROWS; y++) {
                grid[x][y] = new Cell(x, y);
            }
        }

        // Prim's Algorithm
        ArrayList<Cell> frontier = new ArrayList<>();
        Random rand = new Random();
        Cell start = grid[0][0];
        start.visited = true;
        addFrontier(start, frontier);

        while (!frontier.isEmpty()) {
            Cell current = frontier.remove(rand.nextInt(frontier.size()));
            current.visited = true;
            ArrayList<Cell> neighbors = getNeighbors(current);
            ArrayList<Cell> inMazeNeighbors = new ArrayList<>();
            for (Cell n : neighbors) if (n.visited) inMazeNeighbors.add(n);

            if (!inMazeNeighbors.isEmpty()) {
                Cell target = inMazeNeighbors.get(rand.nextInt(inMazeNeighbors.size()));
                removeWalls(current, target);
            }
            addFrontier(current, frontier);
        }

        // Braid Maze
        int extraPaths = 50;
        for (int i = 0; i < extraPaths; i++) {
            int cx = rand.nextInt(COLS);
            int cy = rand.nextInt(ROWS);
            Cell current = grid[cx][cy];
            int direction = rand.nextInt(4);
            if (direction == 0 && cy > 0) removeWalls(current, grid[cx][cy-1]);
            else if (direction == 1 && cx < COLS - 1) removeWalls(current, grid[cx+1][cy]);
            else if (direction == 2 && cy < ROWS - 1) removeWalls(current, grid[cx][cy+1]);
            else if (direction == 3 && cx > 0) removeWalls(current, grid[cx-1][cy]);
        }

        resetCellsForSolving();
        startCell = grid[0][0];
        endCell = grid[COLS - 1][ROWS - 1];

        logArea.append("Maze Generated!\nWeights Randomized.\n\n");
        repaint();
    }

    private void clearPaths() {
        if (timer != null) timer.stop();
        isSolving = false;
        pathHistory.clear();
        visitedOrder = new ArrayList<>();
        currentAnimatingPath = null;
        resetCellsForSolving();
        repaint();
    }

    private void resetCellsForSolving() {
        if (grid == null) return;
        for (int x = 0; x < COLS; x++) {
            for (int y = 0; y < ROWS; y++) {
                grid[x][y].visited = false;
                grid[x][y].parent = null;
                grid[x][y].gCost = Integer.MAX_VALUE;
                grid[x][y].fCost = Integer.MAX_VALUE;
            }
        }
    }

    private void addFrontier(Cell cell, ArrayList<Cell> frontier) {
        int[] dx = {0, 0, 1, -1};
        int[] dy = {-1, 1, 0, 0};
        for (int i = 0; i < 4; i++) {
            int nx = cell.col + dx[i];
            int ny = cell.row + dy[i];
            if (nx >= 0 && nx < COLS && ny >= 0 && ny < ROWS) {
                Cell neighbor = grid[nx][ny];
                if (!neighbor.visited && !frontier.contains(neighbor)) {
                    frontier.add(neighbor);
                }
            }
        }
    }

    private void removeWalls(Cell a, Cell b) {
        int dx = a.col - b.col;
        int dy = a.row - b.row;
        if (dx == 1) { a.walls[3] = false; b.walls[1] = false; }
        if (dx == -1) { a.walls[1] = false; b.walls[3] = false; }
        if (dy == 1) { a.walls[0] = false; b.walls[2] = false; }
        if (dy == -1) { a.walls[2] = false; b.walls[0] = false; }
    }

    // ==========================================
    // 2. SOLVING LOGIC
    // ==========================================
    private void startSolving(String algo) {
        if (isSolving) return;
        resetCellsForSolving();
        visitedOrder = new ArrayList<>();
        currentAnimatingPath = new ArrayList<>();
        animationIndex = 0;
        currentAlgo = algo;
        pathTotalWeight = 0;

        // TIMING START
        long startTime = System.nanoTime();

        switch (algo) {
            case "BFS": solveBFS(); break;
            case "DFS": solveDFS(); break;
            case "Dijkstra": solveDijkstra(); break;
            case "AStar": solveAStar(); break;
        }

        // TIMING END
        long endTime = System.nanoTime();
        executionTimeMs = (endTime - startTime) / 1_000_000.0;

        isSolving = true;
        timer = new Timer(15, this);
        timer.start();

        logArea.append("> Running " + algo + "...\n");
    }

    private void solveBFS() {
        queue = new LinkedList<>();
        queue.add(startCell);
        startCell.visited = true;
        while (!queue.isEmpty()) {
            Cell current = queue.poll();
            visitedOrder.add(current);
            if (current == endCell) { backtrackPath(); return; }
            for (Cell neighbor : getConnectedNeighbors(current)) {
                if (!neighbor.visited) {
                    neighbor.visited = true;
                    neighbor.parent = current;
                    queue.add(neighbor);
                }
            }
        }
    }

    private void solveDFS() {
        stack = new Stack<>();
        stack.push(startCell);
        startCell.visited = true;
        while (!stack.isEmpty()) {
            Cell current = stack.pop();
            visitedOrder.add(current);
            if (current == endCell) { backtrackPath(); return; }
            for (Cell neighbor : getConnectedNeighbors(current)) {
                if (!neighbor.visited) {
                    neighbor.visited = true;
                    neighbor.parent = current;
                    stack.push(neighbor);
                }
            }
        }
    }

    private void solveDijkstra() {
        priorityQueue = new PriorityQueue<>(Comparator.comparingInt(c -> c.gCost));
        startCell.gCost = 0;
        priorityQueue.add(startCell);

        while (!priorityQueue.isEmpty()) {
            Cell current = priorityQueue.poll();
            if (current.visited) continue;
            current.visited = true;
            visitedOrder.add(current);

            if (current == endCell) { backtrackPath(); return; }

            for (Cell neighbor : getConnectedNeighbors(current)) {
                if (!neighbor.visited) {
                    int newCost = current.gCost + neighbor.weight;
                    if (newCost < neighbor.gCost) {
                        neighbor.gCost = newCost;
                        neighbor.parent = current;
                        priorityQueue.add(neighbor);
                    }
                }
            }
        }
    }

    private void solveAStar() {
        priorityQueue = new PriorityQueue<>(Comparator.comparingInt(c -> c.fCost));
        startCell.gCost = 0;
        startCell.fCost = heuristic(startCell, endCell);
        priorityQueue.add(startCell);

        while (!priorityQueue.isEmpty()) {
            Cell current = priorityQueue.poll();
            if (current.visited) continue;
            current.visited = true;
            visitedOrder.add(current);

            if (current == endCell) { backtrackPath(); return; }

            for (Cell neighbor : getConnectedNeighbors(current)) {
                if (!neighbor.visited) {
                    int tentativeGCost = current.gCost + neighbor.weight;
                    if (tentativeGCost < neighbor.gCost) {
                        neighbor.parent = current;
                        neighbor.gCost = tentativeGCost;
                        neighbor.hCost = heuristic(neighbor, endCell);
                        neighbor.fCost = neighbor.gCost + neighbor.hCost;
                        priorityQueue.add(neighbor);
                    }
                }
            }
        }
    }

    private int heuristic(Cell a, Cell b) {
        return Math.abs(a.col - b.col) + Math.abs(a.row - b.row);
    }

    private void backtrackPath() {
        Cell curr = endCell;
        while (curr != null) {
            currentAnimatingPath.add(curr);
            if (curr != startCell) {
                pathTotalWeight += curr.weight;
            }
            curr = curr.parent;
        }
        if(currentAnimatingPath.size() > 0) currentAnimatingPath.remove(currentAnimatingPath.size()-1);
        Collections.reverse(currentAnimatingPath);
    }

    private ArrayList<Cell> getConnectedNeighbors(Cell c) {
        ArrayList<Cell> list = new ArrayList<>();
        if (!c.walls[0] && c.row > 0) list.add(grid[c.col][c.row - 1]);
        if (!c.walls[1] && c.col < COLS - 1) list.add(grid[c.col + 1][c.row]);
        if (!c.walls[2] && c.row < ROWS - 1) list.add(grid[c.col][c.row + 1]);
        if (!c.walls[3] && c.col > 0) list.add(grid[c.col - 1][c.row]);
        return list;
    }

    private ArrayList<Cell> getNeighbors(Cell c) {
        ArrayList<Cell> list = new ArrayList<>();
        if (c.row > 0) list.add(grid[c.col][c.row - 1]);
        if (c.col < COLS - 1) list.add(grid[c.col + 1][c.row]);
        if (c.row < ROWS - 1) list.add(grid[c.col][c.row + 1]);
        if (c.col > 0) list.add(grid[c.col - 1][c.row]);
        return list;
    }

    // ==========================================
    // 3. VISUALIZATION & LOGGING
    // ==========================================
    @Override
    public void actionPerformed(ActionEvent e) {
        if (animationIndex < visitedOrder.size()) {
            animationIndex++;
            repaint();
        } else {
            timer.stop();
            isSolving = false;

            Color pathColor;
            switch(currentAlgo) {
                case "BFS": pathColor = Color.CYAN; break;
                case "DFS": pathColor = new Color(255, 105, 180); break;
                case "Dijkstra": pathColor = Color.GREEN; break;
                case "AStar": pathColor = Color.ORANGE; break;
                default: pathColor = Color.WHITE;
            }

            pathHistory.add(new PathLayer(new ArrayList<>(currentAnimatingPath), pathColor, currentAlgo));
            repaint();

            // --- CATAT KE LOG ---
            logResult();
        }
    }

    private void logResult() {
        // Format Teks agar rapi
        String timeStr = String.format("%.3f ms", executionTimeMs);

        logArea.append("Done: " + currentAlgo + "\n");
        logArea.append(" Time : " + timeStr + "\n");

        if (currentAlgo.equals("Dijkstra") || currentAlgo.equals("AStar")) {
            logArea.append(" Cost : " + pathTotalWeight + "\n");
        } else {
            logArea.append(" Steps: " + currentAnimatingPath.size() + "\n");
        }
        logArea.append("----------------\n");

        // Auto scroll ke bawah
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. GRID & NODE WEIGHT COLORING
        for (int x = 0; x < COLS; x++) {
            for (int y = 0; y < ROWS; y++) {
                g2.setColor(grid[x][y].nodeColor);
                g2.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }

        // 2. PATH HISTORY
        for (PathLayer layer : pathHistory) {
            Color c = layer.color;
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 200));
            for (Cell cell : layer.cells) {
                g2.fillRect(cell.col * CELL_SIZE + 10, cell.row * CELL_SIZE + 10, CELL_SIZE - 20, CELL_SIZE - 20);
                g2.setColor(Color.WHITE);
                g2.drawRect(cell.col * CELL_SIZE + 10, cell.row * CELL_SIZE + 10, CELL_SIZE - 20, CELL_SIZE - 20);
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 200));
            }
        }

        // 3. ANIMASI SCANNING
        if (isSolving) {
            if (currentAlgo.equals("BFS")) g2.setColor(new Color(0, 255, 255, 120));
            else if (currentAlgo.equals("DFS")) g2.setColor(new Color(255, 105, 180, 120));
            else if (currentAlgo.equals("Dijkstra")) g2.setColor(new Color(0, 255, 0, 120));
            else if (currentAlgo.equals("AStar")) g2.setColor(new Color(255, 165, 0, 120));

            for (int i = 0; i < animationIndex && i < visitedOrder.size(); i++) {
                Cell c = visitedOrder.get(i);
                g2.fillRect(c.col * CELL_SIZE, c.row * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }

        // 4. START & END
        int startX = startCell.col * CELL_SIZE;
        int startY = startCell.row * CELL_SIZE;
        if (pacmanImg != null) {
            g2.drawImage(pacmanImg, startX + 2, startY + 2, CELL_SIZE - 4, CELL_SIZE - 4, null);
        } else {
            g2.setColor(Color.YELLOW);
            g2.fillArc(startX+2, startY+2, CELL_SIZE-4, CELL_SIZE-4, 30, 300);
        }

        g2.setColor(new Color(255, 50, 50));
        g2.fillRect(endCell.col * CELL_SIZE + 5, endCell.row * CELL_SIZE + 5, CELL_SIZE - 10, CELL_SIZE - 10);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(endCell.col * CELL_SIZE + 5, endCell.row * CELL_SIZE + 5, CELL_SIZE - 10, CELL_SIZE - 10);

        // 5. WALLS
        for (int x = 0; x < COLS; x++) {
            for (int y = 0; y < ROWS; y++) {
                grid[x][y].drawWalls(g2, CELL_SIZE);
            }
        }
    }

    // === INNER CLASS CELL ===
    private class Cell {
        int col, row;
        boolean[] walls = {true, true, true, true};
        boolean visited = false;
        Cell parent = null;
        int weight;
        Color nodeColor;

        int gCost = Integer.MAX_VALUE;
        int hCost = 0;
        int fCost = Integer.MAX_VALUE;

        public Cell(int col, int row) {
            this.col = col;
            this.row = row;
            assignRandomWeight();
        }

        private void assignRandomWeight() {
            double r = Math.random();
            if (r < 0.33) {
                this.weight = 1;
                this.nodeColor = COLOR_WEIGHT_1;
            } else if (r < 0.66) {
                this.weight = 5;
                this.nodeColor = COLOR_WEIGHT_5;
            } else {
                this.weight = 10;
                this.nodeColor = COLOR_WEIGHT_10;
            }
        }

        public void drawWalls(Graphics2D g2, int size) {
            int x = col * size;
            int y = row * size;
            drawWallLines(g2, x, y, size, WALL_COLOR_OUTER, 4);
        }

        private void drawWallLines(Graphics2D g2, int x, int y, int size, Color color, int thickness) {
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            if (walls[0]) g2.drawLine(x, y, x + size, y);
            if (walls[1]) g2.drawLine(x + size, y, x + size, y + size);
            if (walls[2]) g2.drawLine(x + size, y + size, x, y + size);
            if (walls[3]) g2.drawLine(x, y + size, x, y);
        }
    }
}