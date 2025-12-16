import javax.swing.*;
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
    private final int WIDTH = COLS * CELL_SIZE;
    private final int HEIGHT = ROWS * CELL_SIZE;

    // --- WARNA TEMA & WEIGHT (GELAP) ---
    private final Color BG_COLOR = new Color(0, 0, 30);
    private final Color WALL_COLOR_OUTER = new Color(25, 25, 165);
    private final Color WALL_COLOR_INNER = new Color(100, 100, 255);

    // Warna Node (Weight) Gelap sesuai permintaan
    private final Color COLOR_WEIGHT_1 = new Color(19, 32, 59); // Dark Blue
    private final Color COLOR_WEIGHT_5 = new Color(50, 50, 60); // Dark Grey (Sedikit diterangkan agar beda)
    private final Color COLOR_WEIGHT_10 = new Color(45, 20, 45); // Dark Purple

    // --- Struktur Data ---
    private Cell[][] grid;
    private Cell startCell, endCell;

    // --- Variabel Logika ---
    private Stack<Cell> stack;
    private Queue<Cell> queue;

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

    // --- UI Control ---
    private JButton btnNewMaze, btnClearPaths, btnBFS, btnDFS;

    // --- Images ---
    private BufferedImage pacmanImg;
    private BufferedImage dotImg;
    // GANTI PATH INI SESUAI KOMPUTER ANDA
    private String pacmanPath = "C:\\Users\\User\\Downloads\\pacman.png";
    private String dotPath = "C:\\Users\\User\\Downloads\\dot.png";

    public Gameplay() {
        this.setPreferredSize(new Dimension(WIDTH, HEIGHT + 60));
        this.setLayout(null);
        this.setBackground(BG_COLOR);

        loadImages();
        initButtons();
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

    private void initButtons() {
        int btnY = HEIGHT + 20;
        Color btnBg = new Color(50, 50, 50);
        Color btnFg = Color.WHITE;

        btnNewMaze = createStyledButton("New Maze", 10, btnY, 100, new Color(200, 50, 50), Color.WHITE);
        btnNewMaze.addActionListener(e -> generateMaze());

        btnClearPaths = createStyledButton("Clear Paths", 120, btnY, 100, new Color(200, 150, 0), Color.BLACK);
        btnClearPaths.addActionListener(e -> clearPaths());

        // Algoritma
        btnBFS = createStyledButton("BFS (Cyan)", 230, btnY, 100, btnBg, Color.CYAN);
        btnBFS.addActionListener(e -> startSolving("BFS"));

        btnDFS = createStyledButton("DFS (Pink)", 340, btnY, 100, btnBg, Color.PINK);
        btnDFS.addActionListener(e -> startSolving("DFS"));

        this.add(btnNewMaze);
        this.add(btnClearPaths);
        this.add(btnBFS);
        this.add(btnDFS);
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

        // Braid Maze (Multiple Paths)
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

        if (algo.equals("BFS")) solveBFS(); else solveDFS();

        isSolving = true;
        timer = new Timer(10, this); // Kecepatan animasi
        timer.start();
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

    private void backtrackPath() {
        Cell curr = endCell;
        while (curr != null) {
            currentAnimatingPath.add(curr);
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
    // 3. VISUALIZATION
    // ==========================================
    @Override
    public void actionPerformed(ActionEvent e) {
        if (animationIndex < visitedOrder.size()) {
            animationIndex++;
            repaint();
        } else {
            timer.stop();
            isSolving = false;
            // UPDATE WARNA PATH AKHIR
            // Karena background gelap, gunakan warna Cyan terang untuk BFS (jangan biru tua)
            Color pathColor = currentAlgo.equals("BFS") ? Color.CYAN : new Color(255, 105, 180); // Hot Pink
            pathHistory.add(new PathLayer(new ArrayList<>(currentAnimatingPath), pathColor, currentAlgo));
            repaint();
        }
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

        // 2. PATH HISTORY (Jejak yang sudah selesai)
        for (PathLayer layer : pathHistory) {
            Color c = layer.color;
            // Gunakan warna solid atau semi-transparan yang kuat
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 200));
            for (Cell cell : layer.cells) {
                // Gambar jalur sedikit lebih kecil dari kotak
                g2.fillRect(cell.col * CELL_SIZE + 10, cell.row * CELL_SIZE + 10, CELL_SIZE - 20, CELL_SIZE - 20);
                // Tambahkan outline agar lebih jelas
                g2.setColor(Color.WHITE);
                g2.drawRect(cell.col * CELL_SIZE + 10, cell.row * CELL_SIZE + 10, CELL_SIZE - 20, CELL_SIZE - 20);
                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 200)); // Balikin warna
            }
        }

        // 3. ANIMASI SCANNING (Proses Pencarian)
        if (isSolving) {
            // PERUBAHAN: Gunakan warna terang transparan (Neon) agar terlihat di background gelap
            if (currentAlgo.equals("BFS")) {
                g2.setColor(new Color(0, 255, 255, 120)); // Neon Cyan Transparan
            } else {
                g2.setColor(new Color(255, 105, 180, 120)); // Neon Pink Transparan
            }

            for (int i = 0; i < animationIndex && i < visitedOrder.size(); i++) {
                Cell c = visitedOrder.get(i);
                // Mengisi full kotak agar efek "scanning" terasa
                g2.fillRect(c.col * CELL_SIZE, c.row * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }

        // 4. START (PACMAN) & END
        int startX = startCell.col * CELL_SIZE;
        int startY = startCell.row * CELL_SIZE;
        if (pacmanImg != null) {
            // Gambar Pacman
            g2.drawImage(pacmanImg, startX + 2, startY + 2, CELL_SIZE - 4, CELL_SIZE - 4, null);
        } else {
            g2.setColor(Color.YELLOW);
            g2.fillArc(startX+2, startY+2, CELL_SIZE-4, CELL_SIZE-4, 30, 300);
        }

        // End Point (Merah Terang)
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