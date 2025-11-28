import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class GameBoard extends JFrame {

    // --- Font Definitions ---
    // Menggunakan SansSerif dengan gaya modern sebagai pengganti Poppins agar 100% jalan
    private static final String FONT_FAMILY = "SansSerif";
    private static final Font POPS_BOLD = new Font(FONT_FAMILY, Font.BOLD, 18);
    private static final Font POPS_REGULAR = new Font(FONT_FAMILY, Font.PLAIN, 14);

    // Board layout
    private static final int ROWS = 10;
    private static final int COLS = 10;
    private static final int TILE_COUNT = ROWS * COLS;

    // Visual sizes
    private static final int NODE_DIAM = 55; // Ukuran Node Diperbesar
    private static final int GAP = 18;
    private static final int PAD_LEFT = 30;
    private static final int PAD_TOP = 30;

    // Right panel width
    private static final int RIGHT_WIDTH = 380;

    // Colors
    private static final Color BG_BLUE_DARK = new Color(40, 60, 140);
    private static final Color BG_BLUE_MEDIUM = new Color(60, 90, 200);

    private static final Color TILE_COLOR_A = new Color(255, 245, 180);
    private static final Color TILE_COLOR_B = new Color(200, 230, 255);

    private static final Color TRAIL_FORWARD = new Color(70, 200, 100, 150);
    private static final Color TRAIL_BACKWARD = new Color(255, 100, 100, 150);
    private static final Color SHORTCUT_COLOR = new Color(255, 200, 50, 220);
    private static final Color EDGE_COLOR = new Color(80, 120, 200, 220);
    private static final Color COIN_COLOR = new Color(255, 215, 0); // Emas Polos

    private static final Color PRIME_OUTLINE = new Color(0, 255, 255);
    private static final Color MULT5_OUTLINE = new Color(255, 165, 0);

    private static final Color[] PLAYER_COLORS = new Color[]{
            new Color(255, 80, 80),
            new Color(80, 150, 255),
            new Color(80, 220, 120),
            new Color(255, 230, 80)
    };

    // Data Structures
    private static final Map<String, Integer> WIN_SCORES = new HashMap<>();
    private static final Map<String, Integer> CUMULATIVE_SCORES = new HashMap<>();

    private int playerCount = 2;
    private int currentPlayer = 0;
    private final String[] playerNames = new String[4];
    private final int[] playerPos = new int[4];
    private final boolean[] unlocked = new boolean[4];
    private final int[] playerScores = new int[4];

    private final Map<Integer, Integer> coinValues = new HashMap<>();
    private final Map<Integer, Boolean> coinCollected = new HashMap<>();
    private final Map<Integer, Integer> shortcuts = new HashMap<>();

    // UI Components
    private final BoardCanvas boardCanvas = new BoardCanvas();
    private final DicePanel dicePanel = new DicePanel();
    private final StatsPanel statsPanel = new StatsPanel();
    private final JLabel turnLabel = new JLabel();
    private final JTextArea logArea = new JTextArea();

    // Buttons & Layout Control
    private JPanel rollTogglePanel; // Panel untuk swap tombol Roll/Next
    private CardLayout cardLayout;  // Layout manager untuk swap

    private JButton rollButton;
    private JButton resetButton;
    private JButton nextGameButton;

    // Animation & Sound
    private javax.swing.Timer stepTimer;
    private List<Integer> stepQueue = new ArrayList<>();
    private int stepIndex = 0;
    private boolean animating = false;
    private Clip moveClip;
    private final Random rnd = new Random();

    public GameBoard() {
        super("FUN FAMILY GAME NIGHT");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Inisialisasi Tombol (Dibuat di sini agar final field aman)
        rollButton = createRoundedButton("ROLL DICE", new Color(255, 120, 70), Color.WHITE);
        resetButton = createRoundedButton("RESET GAME", new Color(255, 190, 50), Color.BLACK);
        nextGameButton = createRoundedButton("NEXT GAME", new Color(100, 180, 100), Color.WHITE);

        // Setup Awal
        askPlayerDetails();
        initScores();
        initState();
        generateShortcuts();
        loadSound();

        // Setup UI
        initUI();

        // Finalisasi Frame
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);

        log("Game started! " + playerNames[0] + "'s turn.");
        updateTurnLabel();
    }

    // --- TOMBOL BULAT ---
    private JButton createRoundedButton(String text, Color bgColor, Color fgColor) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                if (getModel().isArmed()) {
                    g.setColor(bgColor.darker());
                } else if (!isEnabled()) {
                    g.setColor(Color.GRAY);
                } else {
                    g.setColor(bgColor);
                }

                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

                super.paintComponent(g);
            }
        };
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setForeground(fgColor);
        button.setFont(POPS_BOLD);
        return button;
    }

    private void askPlayerDetails() {
        String[] options = {"1", "2", "3", "4"};
        String sel = (String) JOptionPane.showInputDialog(
                null, "Pilih jumlah pemain:", "Setup",
                JOptionPane.QUESTION_MESSAGE, null, options, "2");

        if (sel == null) System.exit(0);
        playerCount = Integer.parseInt(sel);

        // Panel Input Nama
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.setBackground(BG_BLUE_MEDIUM);
        inputPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JTextField[] nameFields = new JTextField[playerCount];

        JLabel title = new JLabel("Nama Pemain:");
        title.setFont(POPS_BOLD);
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        inputPanel.add(title);
        inputPanel.add(Box.createVerticalStrut(10));

        for (int i = 0; i < playerCount; i++) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
            row.setBackground(BG_BLUE_MEDIUM);

            JPanel colorDot = new JPanel();
            colorDot.setPreferredSize(new Dimension(20, 20));
            colorDot.setBackground(PLAYER_COLORS[i]);
            colorDot.setBorder(BorderFactory.createLineBorder(Color.WHITE, 2));

            nameFields[i] = new JTextField("Player " + (i + 1), 12);
            nameFields[i].setFont(POPS_REGULAR);
            nameFields[i].setForeground(Color.BLACK);
            nameFields[i].setBackground(Color.WHITE); // Input PUTIH
            nameFields[i].setCaretColor(Color.BLACK);

            row.add(colorDot);
            row.add(nameFields[i]);
            inputPanel.add(row);
        }

        UIManager.put("OptionPane.background", BG_BLUE_MEDIUM);
        UIManager.put("Panel.background", BG_BLUE_MEDIUM);

        int result = JOptionPane.showConfirmDialog(null, inputPanel, "Identitas Pemain",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) System.exit(0);

        for (int i = 0; i < playerCount; i++) {
            String txt = nameFields[i].getText().trim();
            playerNames[i] = txt.isEmpty() ? "Player " + (i+1) : txt;
        }
    }

    private void initScores() {
        WIN_SCORES.clear();
        CUMULATIVE_SCORES.clear();
        for (int i = 0; i < playerCount; i++) {
            WIN_SCORES.put(playerNames[i], 0);
            CUMULATIVE_SCORES.put(playerNames[i], 0);
        }
    }

    private void initUI() {
        // Setup Canvas
        int canvasW = 2 * PAD_LEFT + COLS * NODE_DIAM + (COLS - 1) * GAP;
        int canvasH = 2 * PAD_TOP + ROWS * NODE_DIAM + (ROWS - 1) * GAP;
        boardCanvas.setPreferredSize(new Dimension(canvasW, canvasH));

        // Panel Kanan Utama
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(RIGHT_WIDTH, canvasH));
        rightPanel.setBackground(BG_BLUE_DARK);

        // --- TOP: Dice & Stats ---
        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setBackground(BG_BLUE_DARK);

        dicePanel.setPreferredSize(new Dimension(RIGHT_WIDTH, 160));
        statsPanel.setPreferredSize(new Dimension(RIGHT_WIDTH, 130));

        topSection.add(dicePanel);
        topSection.add(statsPanel);
        rightPanel.add(topSection, BorderLayout.NORTH);

        // --- CENTER: Controls ---
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBackground(BG_BLUE_MEDIUM);
        controlPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Label Turn
        turnLabel.setForeground(Color.WHITE);
        turnLabel.setFont(POPS_BOLD);
        turnLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        controlPanel.add(turnLabel);
        controlPanel.add(Box.createVerticalStrut(20));

        // --- SETUP TOMBOL (CardLayout + FlowLayout) ---
        // 1. Panel Swap untuk Roll / Next Game
        cardLayout = new CardLayout();
        rollTogglePanel = new JPanel(cardLayout);
        rollTogglePanel.setBackground(BG_BLUE_MEDIUM);
        rollTogglePanel.setOpaque(false);

        rollTogglePanel.add(rollButton, "Roll");
        rollTogglePanel.add(nextGameButton, "NextGame");

        // Action Listeners
        rollButton.addActionListener(e -> onRoll());
        nextGameButton.addActionListener(e -> onNextGame());
        resetButton.addActionListener(e -> resetGame());

        // 2. Baris Tombol (Roll/Next di kiri, Reset di kanan)
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonRow.setBackground(BG_BLUE_MEDIUM);
        buttonRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Ukuran tombol
        Dimension btnDim = new Dimension(140, 45);
        rollTogglePanel.setPreferredSize(btnDim);
        resetButton.setPreferredSize(btnDim);

        buttonRow.add(rollTogglePanel);
        buttonRow.add(resetButton);

        controlPanel.add(buttonRow);
        rightPanel.add(controlPanel, BorderLayout.CENTER);

        // --- BOTTOM: Log ---
        logArea.setEditable(false);
        logArea.setBackground(new Color(30, 40, 90));
        logArea.setForeground(Color.WHITE);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setMargin(new Insets(5,5,5,5));

        JScrollPane scrollLog = new JScrollPane(logArea);
        scrollLog.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 150, 255)), "Game Log",
                0, 0, POPS_BOLD, Color.WHITE));
        scrollLog.setPreferredSize(new Dimension(RIGHT_WIDTH, 180));
        rightPanel.add(scrollLog, BorderLayout.SOUTH);

        // Main Container
        JPanel mainContainer = new JPanel(new BorderLayout(10, 10));
        mainContainer.setBackground(BG_BLUE_MEDIUM);
        mainContainer.setBorder(new EmptyBorder(10, 10, 10, 10));

        mainContainer.add(boardCanvas, BorderLayout.WEST);
        mainContainer.add(rightPanel, BorderLayout.EAST);

        setContentPane(mainContainer);

        // PASTIKAN TOMBOL AKTIF
        cardLayout.show(rollTogglePanel, "Roll");
        rollButton.setEnabled(true);
    }

    private void initState() {
        Arrays.fill(playerPos, 1);
        Arrays.fill(unlocked, false);
        Arrays.fill(playerScores, 0);
        generateCoins();
    }

    private void generateCoins() {
        coinValues.clear();
        coinCollected.clear();
        Set<Integer> nodes = new HashSet<>();
        while (nodes.size() < 15) {
            int n = rnd.nextInt(TILE_COUNT - 5) + 3;
            nodes.add(n);
        }
        for (int n : nodes) {
            coinValues.put(n, rnd.nextInt(20) + 10);
            coinCollected.put(n, false);
        }
    }

    private void generateShortcuts() {
        shortcuts.clear();
        Set<Integer> starts = new HashSet<>();
        while (shortcuts.size() < 5) {
            int s = rnd.nextInt(TILE_COUNT - 15) + 2;
            int e = s + rnd.nextInt(20) + 5;
            if (e >= TILE_COUNT) continue;
            if ((s-1)/COLS == (e-1)/COLS) continue; // beda baris
            if (!starts.contains(s)) {
                shortcuts.put(s, e);
                starts.add(s);
            }
        }
    }

    // --- CORE LOGIC: Path Finding (Updated) ---
    private void onRoll() {
        if (animating) return;
        rollButton.setEnabled(false); // Disable saat animasi dadu

        // Animasi Dadu
        final int[] tick = {0};
        javax.swing.Timer diceTimer = new javax.swing.Timer(80, null);
        diceTimer.addActionListener(e -> {
            tick[0]++;
            boolean fwd = rnd.nextBoolean();
            dicePanel.show(rnd.nextInt(6)+1, fwd);

            if (tick[0] > 10) {
                diceTimer.stop();
                finishRoll();
            }
        });
        diceTimer.start();
    }

    private void finishRoll() {
        int roll = rnd.nextInt(6) + 1;
        boolean forward = rnd.nextDouble() < 0.85; // 85% chance maju

        dicePanel.show(roll, forward);
        log(playerNames[currentPlayer] + " rolls " + roll + " (" + (forward?"Forward":"Backward") + ")");

        calculateMove(roll, forward);
    }

    // LOGIKA DIJKSTRA / BEST PATH
    // Mencari jalur yang memaksimalkan tile akhir dalam 'roll' langkah
    private void calculateMove(int roll, boolean forward) {
        stepQueue.clear();
        int start = playerPos[currentPlayer];

        // Jika mundur atau belum unlock shortcut: Gerak Linear
        if (!forward || !unlocked[currentPlayer]) {
            int curr = start;
            int dir = forward ? 1 : -1;
            int target = start + (dir * roll);

            // Clamp target
            if (target < 1) target = 1;
            if (target > TILE_COUNT) target = TILE_COUNT;

            while (curr != target) {
                curr += dir;
                stepQueue.add(curr);
            }
        }
        else {
            // Jika MAJU dan UNLOCKED: Cari jalur terbaik (terjauh)
            // BFS untuk mencari node terjauh yang bisa dicapai dalam 'roll' langkah
            // Node menyimpan: currentTile, pathHistory

            class PathNode {
                int tile;
                List<Integer> path;
                PathNode(int t, List<Integer> p) { tile=t; path=new ArrayList<>(p); }
            }

            Queue<PathNode> queue = new LinkedList<>();
            queue.add(new PathNode(start, new ArrayList<>()));

            List<Integer> bestPath = null;
            int maxTileReached = -1;

            int steps = 0;
            while (steps < roll && !queue.isEmpty()) {
                int size = queue.size();
                for (int i=0; i<size; i++) {
                    PathNode current = queue.poll();
                    int u = current.tile;

                    // Possible moves from u:
                    // 1. Linear (u + 1)
                    if (u + 1 <= TILE_COUNT) {
                        List<Integer> nextPath = new ArrayList<>(current.path);
                        nextPath.add(u + 1);
                        queue.add(new PathNode(u + 1, nextPath));
                    }

                    // 2. Shortcut (u -> v)
                    if (shortcuts.containsKey(u)) {
                        int v = shortcuts.get(u);
                        List<Integer> nextPath = new ArrayList<>(current.path);
                        nextPath.add(v);
                        queue.add(new PathNode(v, nextPath));
                    }
                }
                steps++;
            }

            // Setelah 'roll' langkah, cari node dengan tile terbesar di queue
            for (PathNode node : queue) {
                if (node.tile > maxTileReached) {
                    maxTileReached = node.tile;
                    bestPath = node.path;
                }
            }

            if (bestPath != null) {
                stepQueue.addAll(bestPath);
                // Cek shortcut usage untuk log
                for (int i=0; i<bestPath.size()-1; i++) {
                    // Start path mungkin perlu dicek relative ke node sebelumnya
                    // Tapi di sini logikanya sudah path murni
                }
                if (maxTileReached > start + roll) {
                    log("⚡ Shortcut taken! Skipping ahead!");
                }
            } else {
                // Fallback if blocked (shouldn't happen)
                for(int i=1; i<=roll; i++) if(start+i <= TILE_COUNT) stepQueue.add(start+i);
            }
        }

        startAnimation(forward);
    }

    private void startAnimation(boolean forward) {
        if (stepQueue.isEmpty()) {
            rollButton.setEnabled(true);
            return;
        }
        animating = true;
        stepIndex = 0;
        boardCanvas.clearTrails();

        stepTimer = new javax.swing.Timer(300, e -> {
            if (stepIndex >= stepQueue.size()) {
                ((javax.swing.Timer)e.getSource()).stop();
                finishMove();
                return;
            }

            int nextTile = stepQueue.get(stepIndex);

            // Play Sound
            if (moveClip != null) {
                moveClip.setFramePosition(0);
                moveClip.start();
            }

            // Update Posisi
            int prevTile = playerPos[currentPlayer];
            playerPos[currentPlayer] = nextTile;

            // Trail logic
            boolean isJump = Math.abs(nextTile - prevTile) > 1;
            if (!isJump) {
                boardCanvas.setTrail(nextTile, forward ? TRAIL_FORWARD : TRAIL_BACKWARD);
            }

            boardCanvas.repaint();
            stepIndex++;
        });
        stepTimer.start();
    }

    private void finishMove() {
        animating = false;
        int pos = playerPos[currentPlayer];
        String pName = playerNames[currentPlayer];

        // 1. Cek Koin
        if (coinValues.containsKey(pos) && !coinCollected.get(pos)) {
            int val = coinValues.get(pos);
            playerScores[currentPlayer] += val;
            CUMULATIVE_SCORES.put(pName, CUMULATIVE_SCORES.get(pName) + val);
            coinCollected.put(pos, true);
            log("💰 " + pName + " got " + val + " coins!");
            boardCanvas.repaint();
        }

        // 2. Cek Prime (Unlock)
        if (isPrime(pos)) {
            if (!unlocked[currentPlayer]) {
                unlocked[currentPlayer] = true;
                log("🔓 " + pName + " unlocked shortcuts (Prime Node)!");
            }
        }

        // 3. Cek Menang
        if (pos >= TILE_COUNT) {
            log("🏆 " + pName + " WINS!");
            WIN_SCORES.put(pName, WIN_SCORES.get(pName) + 1);
            statsPanel.repaint();

            JOptionPane.showMessageDialog(this, pName + " Menang! Skor Akhir: " + playerScores[currentPlayer]);

            // Switch tombol ke Next Game
            cardLayout.show(rollTogglePanel, "NextGame");
            rollButton.setEnabled(false);
            return;
        }

        // 4. Cek Kelipatan 5 (Extra Turn)
        if (pos % 5 == 0) {
            log("✨ " + pName + " gets extra turn (Multiple of 5)!");
            rollButton.setEnabled(true);
            updateTurnLabel();
            return;
        }

        // Next Player
        currentPlayer = (currentPlayer + 1) % playerCount;
        updateTurnLabel();
        rollButton.setEnabled(true);
    }

    private void onNextGame() {
        resetGame();
    }

    private void resetGame() {
        if (stepTimer != null) stepTimer.stop();
        animating = false;

        initState();
        generateShortcuts();
        currentPlayer = 0;

        boardCanvas.clearTrails();
        boardCanvas.repaint();
        updateTurnLabel();
        dicePanel.show(1, true);

        // RESET TOMBOL KE ROLL
        cardLayout.show(rollTogglePanel, "Roll");
        rollButton.setEnabled(true); // Pastikan aktif

        log("\n--- GAME RESET ---");
        log(playerNames[0] + "'s turn.");
    }

    private void updateTurnLabel() {
        turnLabel.setText("GILIRAN: " + playerNames[currentPlayer] + " (Score: " + playerScores[currentPlayer] + ")");
    }

    private void log(String txt) {
        logArea.append(txt + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void loadSound() {
        try {
            File f = new File("C:\\Intellij Idea\\Final Project SEM 3\\FP ASD\\Backsound Game\\move.wav");
            if (f.exists()) {
                AudioInputStream ais = AudioSystem.getAudioInputStream(f);
                moveClip = AudioSystem.getClip();
                moveClip.open(ais);
            }
        } catch (Exception e) {}
    }

    private boolean isPrime(int n) {
        if (n < 2) return false;
        if (n == 2) return true;
        if (n % 2 == 0) return false;
        for (int i=3; i*i<=n; i+=2) if (n%i==0) return false;
        return true;
    }

    // --- INNER CLASSES ---

    private class BoardCanvas extends JPanel {
        private final Color[] trails = new Color[TILE_COUNT+1];

        void clearTrails() { Arrays.fill(trails, null); }
        void setTrail(int i, Color c) { if (i<=TILE_COUNT) trails[i] = c; }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw Edges
            g2.setColor(EDGE_COLOR);
            g2.setStroke(new BasicStroke(2));
            for (int i=1; i<TILE_COUNT; i++) {
                Point p1 = getTileCenter(i);
                Point p2 = getTileCenter(i+1);
                g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            }

            // Draw Shortcuts
            g2.setColor(SHORTCUT_COLOR);
            g2.setStroke(new BasicStroke(4));
            for (Map.Entry<Integer, Integer> e : shortcuts.entrySet()) {
                Point p1 = getTileCenter(e.getKey());
                Point p2 = getTileCenter(e.getValue());
                g2.drawLine(p1.x, p1.y, p2.x, p2.y);
            }

            // Draw Nodes
            for (int i=1; i<=TILE_COUNT; i++) {
                Point p = getTileCenter(i);
                int r = NODE_DIAM / 2;

                // Color pattern
                int row = (i-1)/COLS;
                int col = (i-1)%COLS;
                if (row%2!=0) col = COLS-1-col;
                g2.setColor(((row+col)%2==0) ? TILE_COLOR_A : TILE_COLOR_B);
                g2.fillOval(p.x-r, p.y-r, NODE_DIAM, NODE_DIAM);

                // Trail
                if (trails[i] != null) {
                    g2.setColor(trails[i]);
                    g2.fillOval(p.x-r, p.y-r, NODE_DIAM, NODE_DIAM);
                }

                // Coin (Polos Kuning)
                if (coinValues.containsKey(i) && !coinCollected.get(i)) {
                    g2.setColor(COIN_COLOR);
                    int cr = r - 8;
                    g2.fillOval(p.x-cr, p.y-cr, cr*2, cr*2);
                }

                // Outlines
                g2.setStroke(new BasicStroke(3));
                if (isPrime(i)) {
                    g2.setColor(PRIME_OUTLINE);
                    g2.drawOval(p.x-r, p.y-r, NODE_DIAM, NODE_DIAM);
                } else if (i%5==0) {
                    g2.setColor(MULT5_OUTLINE);
                    g2.drawOval(p.x-r, p.y-r, NODE_DIAM, NODE_DIAM);
                } else {
                    g2.setColor(Color.GRAY);
                    g2.setStroke(new BasicStroke(1));
                    g2.drawOval(p.x-r, p.y-r, NODE_DIAM, NODE_DIAM);
                }

                // Text
                g2.setColor(Color.BLACK);
                g2.setFont(POPS_BOLD.deriveFont(12f));
                String s = String.valueOf(i);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(s, p.x - fm.stringWidth(s)/2, p.y + fm.getAscent()/2 - 2);
            }

            // Draw Players
            for (int i=0; i<playerCount; i++) {
                Point p = getTileCenter(playerPos[i]);
                int offX = (i%2==0 ? -10 : 10);
                int offY = (i/2==0 ? -10 : 10);

                g2.setColor(PLAYER_COLORS[i]);
                g2.fillOval(p.x+offX-8, p.y+offY-8, 16, 16);
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(1));
                g2.drawOval(p.x+offX-8, p.y+offY-8, 16, 16);
            }
        }

        private Point getTileCenter(int i) {
            int idx = i - 1;
            int r = idx / COLS;
            int c = idx % COLS;
            if (r % 2 != 0) c = COLS - 1 - c;

            return new Point(
                    PAD_LEFT + c * (NODE_DIAM + GAP) + NODE_DIAM/2,
                    PAD_TOP + r * (NODE_DIAM + GAP) + NODE_DIAM/2
            );
        }
    }

    private class DicePanel extends JPanel {
        int val = 1; boolean fwd = true;
        void show(int v, boolean f) { val=v; fwd=f; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int sz = Math.min(getWidth(), getHeight()) - 40;
            int x = (getWidth()-sz)/2;
            int y = 20;

            g2.setColor(fwd ? new Color(60, 180, 60) : new Color(200, 60, 60));
            g2.fillRoundRect(x, y, sz, sz, 20, 20);

            g2.setColor(Color.WHITE);
            g2.fillRoundRect(x+10, y+10, sz-20, sz-20, 15, 15);

            g2.setColor(Color.BLACK);
            int pip = sz/6;
            // Simple logic for pips
            if(val%2!=0) fillPip(g2, x+sz/2, y+sz/2, pip);
            if(val>1) { fillPip(g2, x+sz/4, y+sz/4, pip); fillPip(g2, x+3*sz/4, y+3*sz/4, pip); }
            if(val>3) { fillPip(g2, x+3*sz/4, y+sz/4, pip); fillPip(g2, x+sz/4, y+3*sz/4, pip); }
            if(val==6) { fillPip(g2, x+sz/4, y+sz/2, pip); fillPip(g2, x+3*sz/4, y+sz/2, pip); }
        }
        void fillPip(Graphics2D g, int x, int y, int s) { g.fillOval(x-s/2, y-s/2, s, s); }
    }

    private class StatsPanel extends JPanel {
        StatsPanel() { setBackground(BG_BLUE_DARK); }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.WHITE);
            g.setFont(POPS_BOLD.deriveFont(13f));
            g.drawString("TOP WINS", 10, 20);
            g.drawString("TOP COINS", 200, 20);
            g.drawLine(10, 25, 360, 25);

            g.setFont(POPS_REGULAR.deriveFont(12f));
            int y = 45;
            // Tampilkan Data Realtime
            List<Map.Entry<String, Integer>> wins = new ArrayList<>(WIN_SCORES.entrySet());
            wins.sort((a,b) -> b.getValue() - a.getValue());

            for(int i=0; i<Math.min(3, wins.size()); i++) {
                g.drawString((i+1)+". "+wins.get(i).getKey()+" ("+wins.get(i).getValue()+")", 10, y+i*20);
            }

            List<Map.Entry<String, Integer>> coins = new ArrayList<>(CUMULATIVE_SCORES.entrySet());
            coins.sort((a,b) -> b.getValue() - a.getValue());
            for(int i=0; i<Math.min(3, coins.size()); i++) {
                g.drawString((i+1)+". "+coins.get(i).getKey()+" ("+coins.get(i).getValue()+")", 200, y+i*20);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameBoard::new);
    }
}