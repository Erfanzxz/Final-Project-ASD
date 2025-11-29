import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;

public class GameBoard extends JFrame {

    // 1. FONT
    private static final String PATH_FONT = "C:\\Intellij Idea\\Final Project SEM 3\\FP ASD\\FONT\\Clash Royale.ttf";

    // 2. BACKGROUND BOARD

    private static final String PATH_BG_IMG = "C:\\Users\\USER\\IdeaProjects\\ASD FINAL BOSS\\Background\\3d-fantasy-scene.jpg";

    // 3. AUDIO
    private static final String PATH_BGM = "C:\\Intellij Idea\\Final Project SEM 3\\FP ASD\\Backsound Game\\Orerbugh City (Backsound Game).wav";
    private static final String PATH_SFX_MOVE = "C:\\Intellij Idea\\Final Project SEM 3\\FP ASD\\Backsound Game\\move.wav";
    private static final String PATH_SFX_COIN = "C:\\Intellij Idea\\Final Project SEM 3\\FP ASD\\Backsound Game\\Mario Coin Sound - Sound Effect (HD) - Gaming Sound FX.wav";

    // 4. GAMBAR KOIN
    private static final String PATH_COIN_IMG = "C:\\Intellij Idea\\Final Project SEM 3\\FP ASD\\Player Character\\coinMario.png";

    // 5. GAMBAR KARAKTER
    private static final String[] CHAR_FILES = {
            "C:\\Intellij Idea\\Final Project SEM 3\\FP ASD\\Player Character\\mario.png",
            "C:\\Intellij Idea\\Final Project SEM 3\\FP ASD\\Player Character\\luigi.png",
            "C:\\Intellij Idea\\Final Project SEM 3\\FP ASD\\Player Character\\waluigi.png",
            "C:\\Intellij Idea\\Final Project SEM 3\\FP ASD\\Player Character\\yossi.png"
    };

    private static final String[] CHAR_NAMES = {"Mario", "Luigi", "Waluigi", "Yossi"};

    // --- FONT LOADING ---
    private static Font POPS_BOLD;
    private static Font POPS_REGULAR;

    static {
        try {
            File fontFile = new File(PATH_FONT);
            if (fontFile.exists()) {
                Font customFont = Font.createFont(Font.TRUETYPE_FONT, fontFile);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(customFont);
                POPS_BOLD = customFont.deriveFont(Font.BOLD, 14f);
                POPS_REGULAR = customFont.deriveFont(Font.PLAIN, 12f);
            } else {
                POPS_BOLD = new Font("SansSerif", Font.BOLD, 14);
                POPS_REGULAR = new Font("SansSerif", Font.PLAIN, 12);
            }
        } catch (Exception e) {
            POPS_BOLD = new Font("SansSerif", Font.BOLD, 14);
            POPS_REGULAR = new Font("SansSerif", Font.PLAIN, 12);
        }
    }

    // --- KONFIGURASI PAPAN ---
    private static final int ROWS = 10;
    private static final int COLS = 10;
    private static final int TILE_COUNT = ROWS * COLS;

    // --- UKURAN VISUAL ---
    private static final int NODE_DIAM = 65;
    private static final int GAP = 15;
    private static final int PAD_LEFT = 30;
    private static final int PAD_TOP = 30;
    private static final int RIGHT_WIDTH = 380;
    private static final int CHAR_WIDTH = 45;
    private static final int CHAR_HEIGHT = 45;
    private static final int COIN_ICON_SIZE = 40; // Ukuran Koin

    // --- COLORS ---
    private static final Color BG_BLUE_DARK = new Color(0, 66, 69);
    private static final Color BG_BLUE_MEDIUM = new Color(2, 104, 105);
    private static final Color TILE_COLOR_A = new Color(255, 245, 180);
    private static final Color TILE_COLOR_B = new Color(200, 230, 255);
    private static final Color TRAIL_FORWARD = new Color(70, 200, 100, 150);
    private static final Color TRAIL_BACKWARD = new Color(255, 100, 100, 150);
    private static final Color SHORTCUT_COLOR = new Color(255, 200, 50, 220);
    private static final Color EDGE_COLOR = new Color(100, 130, 200, 180);
    private static final Color PRIME_OUTLINE = new Color(0, 255, 255);
    private static final Color MULT5_OUTLINE = new Color(255, 165, 0);

    private static final Color[] PLAYER_COLORS = new Color[]{
            new Color(255, 80, 80), new Color(80, 150, 255),
            new Color(80, 220, 120), new Color(255, 230, 80)
    };

    // --- VARIABLES ---
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

    private final Image[] playerImages = new Image[4];
    private String[] selectedCharFiles = new String[4];

    // --- COMPONENTS ---
    private final BoardCanvas boardCanvas;
    private final DicePanel dicePanel = new DicePanel();
    private final StatsPanel statsPanel = new StatsPanel();
    private final PlayerTurnPanel turnPanel = new PlayerTurnPanel();
    private final JTextArea logArea = new JTextArea();

    // Kontrol Tombol
    private JPanel rollTogglePanel;
    private CardLayout cardLayout;
    private JButton rollButton;
    private JButton resetButton;
    private JButton nextGameButton;

    // Audio & Animasi
    private javax.swing.Timer stepTimer;
    private List<Integer> stepQueue = new ArrayList<>();
    private int stepIndex = 0;
    private boolean animating = false;
    private Clip moveClip;
    private Clip bgmClip;
    private Clip coinClip;
    private FloatControl gainControl;
    private boolean isMuted = false;
    private float previousVolume = -20.0f;
    private final Random rnd = new Random();

    // ================= CONSTRUCTOR =================
    public GameBoard() {
        super("FUN FAMILY GAME NIGHT");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Init Buttons
        rollButton = createRoundedButton("ROLL DICE", new Color(255, 108, 54), Color.WHITE);
        resetButton = createRoundedButton("RESET", new Color(255, 210, 110), Color.BLACK);
        nextGameButton = createRoundedButton("NEXT GAME", new Color(100, 180, 100), Color.WHITE);

        askPlayerDetailsAndCharacters();
        reloadPlayerImages();

        boardCanvas = new BoardCanvas();

        initScores();
        initState();
        generateShortcuts();

        loadSounds();
        playBackgroundMusic();

        initUI();

        pack();
        setResizable(false);
        setLocationRelativeTo(null);
        setVisible(true);

        log("Game started! " + playerNames[0] + "'s turn.");
        updateTurnPanelUI();
    }

    // --- IMAGE LOADING HELPER (SAFE) ---
    private Image safeLoadImage(String path) {
        if (path == null || path.isEmpty()) return null;
        try {
            File f = new File(path);
            if (f.exists()) {
                return ImageIO.read(f);
            }
        } catch (IOException e) {
            // Silent error to prevent crash
        }
        return null;
    }

    private void reloadPlayerImages() {
        Arrays.fill(playerImages, null);
        for (int i = 0; i < playerCount; i++) {
            playerImages[i] = safeLoadImage(selectedCharFiles[i]);
        }
    }

    private void updateTurnPanelUI() {
        turnPanel.updateTurn(currentPlayer, playerNames[currentPlayer], playerScores[currentPlayer]);
    }

    // --- INIT UI ---
    private void initUI() {
        int canvasW = 2 * PAD_LEFT + COLS * NODE_DIAM + (COLS - 1) * GAP;
        int canvasH = 2 * PAD_TOP + ROWS * NODE_DIAM + (ROWS - 1) * GAP;
        boardCanvas.setPreferredSize(new Dimension(canvasW, canvasH));

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(RIGHT_WIDTH, canvasH));
        rightPanel.setBackground(BG_BLUE_DARK);

        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setBackground(BG_BLUE_DARK);
        dicePanel.setPreferredSize(new Dimension(RIGHT_WIDTH, 160));
        statsPanel.setPreferredSize(new Dimension(RIGHT_WIDTH, 130));
        topSection.add(dicePanel);
        topSection.add(statsPanel);
        rightPanel.add(topSection, BorderLayout.NORTH);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setBackground(BG_BLUE_MEDIUM);
        controlPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        controlPanel.add(turnPanel);
        controlPanel.add(Box.createVerticalStrut(20));
        controlPanel.add(createSoundControlPanel());
        controlPanel.add(Box.createVerticalStrut(10));

        cardLayout = new CardLayout();
        rollTogglePanel = new JPanel(cardLayout);
        rollTogglePanel.setBackground(BG_BLUE_MEDIUM);
        rollTogglePanel.setOpaque(false);
        rollTogglePanel.add(rollButton, "Roll");
        rollTogglePanel.add(nextGameButton, "NextGame");

        rollButton.addActionListener(e -> onRoll());
        nextGameButton.addActionListener(e -> onNextGame());
        resetButton.addActionListener(e -> resetGame());

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonRow.setBackground(BG_BLUE_MEDIUM);
        Dimension btnDim = new Dimension(140, 45);
        rollTogglePanel.setPreferredSize(btnDim);
        resetButton.setPreferredSize(btnDim);
        buttonRow.add(rollTogglePanel);
        buttonRow.add(resetButton);
        controlPanel.add(buttonRow);

        rightPanel.add(controlPanel, BorderLayout.CENTER);

        logArea.setEditable(false);
        logArea.setBackground(new Color(0, 43, 45));
        logArea.setForeground(Color.WHITE);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setMargin(new Insets(5, 5, 5, 5));
        JScrollPane scrollLog = new JScrollPane(logArea);
        scrollLog.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(100, 150, 255)), "GAME HISTORY",
                0, 0, POPS_BOLD.deriveFont(12f), Color.BLACK));
        scrollLog.setPreferredSize(new Dimension(RIGHT_WIDTH, 180));
        rightPanel.add(scrollLog, BorderLayout.SOUTH);

        JPanel mainContainer = new JPanel(new BorderLayout(10, 10));
        mainContainer.setBackground(BG_BLUE_MEDIUM);
        mainContainer.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainContainer.add(boardCanvas, BorderLayout.WEST);
        mainContainer.add(rightPanel, BorderLayout.EAST);

        setContentPane(mainContainer);

        cardLayout.show(rollTogglePanel, "Roll");
        rollButton.setEnabled(true);
    }

    // --- SETUP DIALOG ---
    private void askPlayerDetailsAndCharacters() {
        String[] options = {"2", "3", "4"};
        UIManager.put("OptionPane.messageFont", POPS_REGULAR);
        UIManager.put("OptionPane.buttonFont", POPS_BOLD.deriveFont(12f));

        int choice = JOptionPane.showOptionDialog(null, "Berapa banyak pemain?", "Setup Game",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (choice == -1) System.exit(0);
        playerCount = choice + 2;

        JDialog dialog = new JDialog((Frame) null, "Setup Player & Character", true);
        dialog.setLayout(new BorderLayout());
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(BG_BLUE_MEDIUM);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Pilih Identitas");
        title.setFont(POPS_BOLD.deriveFont(20f));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(title);
        mainPanel.add(Box.createVerticalStrut(20));

        JTextField[] nameFields = new JTextField[playerCount];
        JComboBox<String>[] charCombos = new JComboBox[playerCount];

        for (int i = 0; i < playerCount; i++) {
            JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
            rowPanel.setBackground(new Color(0, 0, 0, 50));

            JLabel lbl = new JLabel("P" + (i + 1));
            lbl.setFont(POPS_BOLD);
            lbl.setForeground(PLAYER_COLORS[i]);
            lbl.setPreferredSize(new Dimension(30, 30));

            nameFields[i] = new JTextField("Player " + (i + 1), 8);
            nameFields[i].setFont(POPS_REGULAR);

            charCombos[i] = new JComboBox<>(CHAR_NAMES);
            charCombos[i].setFont(POPS_REGULAR);
            charCombos[i].setSelectedIndex(i);

            JLabel imagePreview = new JLabel();
            imagePreview.setPreferredSize(new Dimension(50, 50));
            imagePreview.setHorizontalAlignment(SwingConstants.CENTER);
            imagePreview.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1));

            final int pIndex = i;
            charCombos[i].addActionListener(e -> updatePreview(imagePreview, (String) charCombos[pIndex].getSelectedItem()));
            updatePreview(imagePreview, CHAR_NAMES[i]);

            rowPanel.add(lbl);
            rowPanel.add(nameFields[i]);
            rowPanel.add(charCombos[i]);
            rowPanel.add(imagePreview);

            mainPanel.add(rowPanel);
            mainPanel.add(Box.createVerticalStrut(5));
        }

        JButton btnStart = createRoundedButton("START GAME", new Color(100, 180, 100), Color.WHITE);
        btnStart.setPreferredSize(new Dimension(200, 40));
        btnStart.addActionListener(e -> dialog.dispose());

        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(BG_BLUE_MEDIUM);
        btnPanel.add(btnStart);

        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        for (int i = 0; i < playerCount; i++) {
            playerNames[i] = nameFields[i].getText().trim();
            if (playerNames[i].isEmpty()) playerNames[i] = "Player " + (i + 1);

            String selectedName = (String) charCombos[i].getSelectedItem();
            for (int k = 0; k < CHAR_NAMES.length; k++) {
                if (CHAR_NAMES[k].equals(selectedName)) {
                    selectedCharFiles[i] = CHAR_FILES[k];
                    break;
                }
            }
        }
    }

    private void updatePreview(JLabel label, String charName) {
        String fileName = "";
        for (int k = 0; k < CHAR_NAMES.length; k++) {
            if (CHAR_NAMES[k].equals(charName)) {
                fileName = CHAR_FILES[k];
                break;
            }
        }
        Image img = safeLoadImage(fileName);
        if (img != null) {
            label.setIcon(new ImageIcon(img.getScaledInstance(45, 45, Image.SCALE_SMOOTH)));
            label.setText("");
        } else {
            label.setIcon(null);
            label.setText("?");
            label.setForeground(Color.WHITE);
        }
    }

    private JPanel createSoundControlPanel() {
        JPanel soundPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        soundPanel.setBackground(BG_BLUE_MEDIUM);
        soundPanel.setBorder(new EmptyBorder(0, 0, 10, 0));

        JButton muteButton = new JButton("🔊");
        muteButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        muteButton.setFocusPainted(false);
        muteButton.setBackground(Color.WHITE);
        muteButton.setPreferredSize(new Dimension(50, 30));

        JSlider volumeSlider = new JSlider(-60, 6, -20);
        volumeSlider.setPreferredSize(new Dimension(150, 30));
        volumeSlider.setBackground(BG_BLUE_MEDIUM);
        volumeSlider.setForeground(Color.WHITE);

        volumeSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (gainControl != null) {
                    float value = volumeSlider.getValue();
                    if (value == -60) {
                        gainControl.setValue(gainControl.getMinimum());
                        muteButton.setText("🔇");
                        isMuted = true;
                    } else {
                        gainControl.setValue(value);
                        muteButton.setText("🔊");
                        isMuted = false;
                        previousVolume = value;
                    }
                }
            }
        });

        muteButton.addActionListener(e -> {
            if (gainControl != null) {
                if (isMuted) {
                    gainControl.setValue(previousVolume);
                    volumeSlider.setValue((int) previousVolume);
                    muteButton.setText("🔊");
                    isMuted = false;
                } else {
                    previousVolume = volumeSlider.getValue();
                    gainControl.setValue(gainControl.getMinimum());
                    volumeSlider.setValue(-60);
                    muteButton.setText("🔇");
                    isMuted = true;
                }
            }
        });

        soundPanel.add(new JLabel("Music: ") {{
            setForeground(Color.WHITE);
            setFont(POPS_REGULAR);
        }});
        soundPanel.add(muteButton);
        soundPanel.add(volumeSlider);

        return soundPanel;
    }

    private JButton createRoundedButton(String text, Color bgColor, Color fgColor) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                if (getModel().isArmed()) g.setColor(bgColor.darker());
                else if (!isEnabled()) g.setColor(Color.GRAY);
                else g.setColor(bgColor);
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

    // ================= AUDIO LOGIC =================
    private void loadSounds() {
        try {
            if (!PATH_SFX_MOVE.isEmpty()) {
                File fMove = new File(PATH_SFX_MOVE);
                if (fMove.exists()) {
                    AudioInputStream ais = AudioSystem.getAudioInputStream(fMove);
                    moveClip = AudioSystem.getClip();
                    moveClip.open(ais);
                }
            }

            if (!PATH_SFX_COIN.isEmpty()) {
                File fCoin = new File(PATH_SFX_COIN);
                if (fCoin.exists()) {
                    AudioInputStream ais = AudioSystem.getAudioInputStream(fCoin);
                    coinClip = AudioSystem.getClip();
                    coinClip.open(ais);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playCoinSound() {
        if (coinClip != null) {
            coinClip.setFramePosition(0);
            coinClip.start();
        }
    }

    private void playBackgroundMusic() {
        if (PATH_BGM.isEmpty()) return;
        try {
            File f = new File(PATH_BGM);
            if (f.exists()) {
                AudioInputStream ais = AudioSystem.getAudioInputStream(f);
                bgmClip = AudioSystem.getClip();
                bgmClip.open(ais);
                try {
                    gainControl = (FloatControl) bgmClip.getControl(FloatControl.Type.MASTER_GAIN);
                    gainControl.setValue(-25.0f);
                } catch (Exception ex) {}
                bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
                bgmClip.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= GAME LOGIC =================
    private void onRoll() {
        if (animating) return;
        rollButton.setEnabled(false);
        final int[] tick = {0};
        javax.swing.Timer diceTimer = new javax.swing.Timer(80, null);
        diceTimer.addActionListener(e -> {
            tick[0]++;
            boolean fwd = rnd.nextBoolean();
            dicePanel.show(rnd.nextInt(6) + 1, fwd);
            if (tick[0] > 10) {
                diceTimer.stop();
                finishRoll();
            }
        });
        diceTimer.start();
    }

    private void finishRoll() {
        int roll = rnd.nextInt(6) + 1;
        boolean forward = rnd.nextDouble() < 0.85;
        dicePanel.show(roll, forward);
        log(playerNames[currentPlayer] + " rolls " + roll + " (" + (forward ? "Forward" : "Backward") + ")");
        calculateMove(roll, forward);
    }

    private void calculateMove(int roll, boolean forward) {
        stepQueue.clear();
        int start = playerPos[currentPlayer];

        if (!forward || !unlocked[currentPlayer]) {
            int curr = start;
            int dir = forward ? 1 : -1;
            int target = start + (dir * roll);
            if (target < 1) target = 1;
            if (target > TILE_COUNT) target = TILE_COUNT;
            while (curr != target) {
                curr += dir;
                stepQueue.add(curr);
            }
        } else {
            class PathNode {
                int tile;
                List<Integer> path;

                PathNode(int t, List<Integer> p) {
                    tile = t;
                    path = new ArrayList<>(p);
                }
            }
            Queue<PathNode> queue = new LinkedList<>();
            queue.add(new PathNode(start, new ArrayList<>()));
            List<Integer> bestPath = null;
            int maxTileReached = -1;
            int steps = 0;
            while (steps < roll && !queue.isEmpty()) {
                int size = queue.size();
                for (int i = 0; i < size; i++) {
                    PathNode current = queue.poll();
                    int u = current.tile;
                    if (u + 1 <= TILE_COUNT) {
                        List<Integer> nextPath = new ArrayList<>(current.path);
                        nextPath.add(u + 1);
                        queue.add(new PathNode(u + 1, nextPath));
                    }
                    if (shortcuts.containsKey(u)) {
                        int v = shortcuts.get(u);
                        List<Integer> nextPath = new ArrayList<>(current.path);
                        nextPath.add(v);
                        queue.add(new PathNode(v, nextPath));
                    }
                }
                steps++;
            }
            for (PathNode node : queue) {
                if (node.tile > maxTileReached) {
                    maxTileReached = node.tile;
                    bestPath = node.path;
                }
            }
            if (bestPath != null) {
                stepQueue.addAll(bestPath);
                if (maxTileReached > start + roll) log("⚡ Shortcut taken! Skipping ahead!");
            } else {
                for (int i = 1; i <= roll; i++) if (start + i <= TILE_COUNT) stepQueue.add(start + i);
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
                ((javax.swing.Timer) e.getSource()).stop();
                finishMove();
                return;
            }
            int nextTile = stepQueue.get(stepIndex);
            if (moveClip != null) {
                moveClip.setFramePosition(0);
                moveClip.start();
            }
            int prevTile = playerPos[currentPlayer];
            playerPos[currentPlayer] = nextTile;
            boardCanvas.repaint();
            if (Math.abs(nextTile - prevTile) == 1) {
                boardCanvas.setTrail(nextTile, forward ? TRAIL_FORWARD : TRAIL_BACKWARD);
            }
            stepIndex++;
        });
        stepTimer.start();
    }

    private void finishMove() {
        animating = false;
        int pos = playerPos[currentPlayer];
        String pName = playerNames[currentPlayer];

        if (coinValues.containsKey(pos) && !coinCollected.get(pos)) {
            int val = coinValues.get(pos);
            playerScores[currentPlayer] += val;
            CUMULATIVE_SCORES.put(pName, CUMULATIVE_SCORES.get(pName) + val);
            coinCollected.put(pos, true);
            playCoinSound();
            log("💰 " + pName + " got " + val + " coins!");
            boardCanvas.repaint();
        }
        if (isPrime(pos)) {
            if (!unlocked[currentPlayer]) {
                unlocked[currentPlayer] = true;
                log("🔓 " + pName + " unlocked shortcuts!");
            }
        }
        if (pos >= TILE_COUNT) {
            log("🏆 " + pName + " WINS!");
            WIN_SCORES.put(pName, WIN_SCORES.get(pName) + 1);
            statsPanel.repaint();
            JOptionPane.showMessageDialog(this, pName + " Menang! Skor: " + playerScores[currentPlayer]);
            cardLayout.show(rollTogglePanel, "NextGame");
            rollButton.setEnabled(false);
            return;
        }
        if (pos % 5 == 0) {
            log("✨ " + pName + " gets extra turn!");
            rollButton.setEnabled(true);
            updateTurnPanelUI();
            return;
        }
        currentPlayer = (currentPlayer + 1) % playerCount;
        updateTurnPanelUI();
        rollButton.setEnabled(true);
    }

    private void onNextGame() {
        resetGame();
    }

    private void resetGame() {
        if (stepTimer != null) stepTimer.stop();
        animating = false;
        askPlayerDetailsAndCharacters();
        reloadPlayerImages();
        initState();
        generateShortcuts();
        currentPlayer = 0;
        boardCanvas.clearTrails();
        boardCanvas.repaint();
        updateTurnPanelUI();
        dicePanel.show(1, true);
        cardLayout.show(rollTogglePanel, "Roll");
        rollButton.setEnabled(true);
        log("\n--- GAME RESET ---");
        log(playerNames[0] + "'s turn.");
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
            nodes.add(rnd.nextInt(TILE_COUNT - 5) + 3);
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
            if (e < TILE_COUNT && (s - 1) / COLS != (e - 1) / COLS && !starts.contains(s)) {
                shortcuts.put(s, e);
                starts.add(s);
            }
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

    private boolean isPrime(int n) {
        if (n < 2) return false;
        if (n == 2) return true;
        if (n % 2 == 0) return false;
        for (int i = 3; i * i <= n; i += 2) if (n % i == 0) return false;
        return true;
    }

    private void log(String txt) {
        logArea.append(txt + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    // --- INNER CLASSES ---

    private class BoardCanvas extends JPanel {
        private final Color[] trails = new Color[TILE_COUNT + 1];
        private Image backgroundImage;
        private Image coinImage;

        BoardCanvas() {
            setBackground(BG_BLUE_MEDIUM);
            setPreferredSize(new Dimension(
                    2 * PAD_LEFT + COLS * NODE_DIAM + (COLS - 1) * GAP,
                    2 * PAD_TOP + ROWS * NODE_DIAM + (ROWS - 1) * GAP));

            backgroundImage = safeLoadImage(PATH_BG_IMG);
            coinImage = safeLoadImage(PATH_COIN_IMG);
        }

        void clearTrails() {
            Arrays.fill(trails, null);
        }

        void setTrail(int tile, Color c) {
            if (tile >= 1 && tile <= TILE_COUNT) trails[tile] = c;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (backgroundImage != null) g2.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            else {
                g2.setColor(BG_BLUE_DARK);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }

            g2.setColor(EDGE_COLOR);
            g2.setStroke(new BasicStroke(2));
            for (int n = 1; n < TILE_COUNT; n++) {
                Point a = getTileCenter(n);
                Point b = getTileCenter(n + 1);
                g2.drawLine(a.x, a.y, b.x, b.y);
            }

            g2.setColor(SHORTCUT_COLOR);
            g2.setStroke(new BasicStroke(4));
            for (Map.Entry<Integer, Integer> e : shortcuts.entrySet()) {
                Point a = getTileCenter(e.getKey());
                Point b = getTileCenter(e.getValue());
                g2.drawLine(a.x, a.y, b.x, b.y);
            }

            for (int node = 1; node <= TILE_COUNT; node++) {
                Point p = getTileCenter(node);
                int r = NODE_DIAM / 2;

                Color baseColor;
                int logicalRow = (node - 1) / COLS;
                int logicalCol = (node - 1) % COLS;
                boolean leftToRight = (logicalRow % 2 == 0);
                int drawCol = leftToRight ? logicalCol : (COLS - 1 - logicalCol);
                if ((drawCol + logicalRow) % 2 == 0) baseColor = TILE_COLOR_A;
                else baseColor = TILE_COLOR_B;

                g2.setColor(baseColor);
                g2.fillOval(p.x - r, p.y - r, NODE_DIAM, NODE_DIAM);

                if (trails[node] != null) {
                    g2.setColor(trails[node]);
                    g2.fillOval(p.x - r, p.y - r, NODE_DIAM, NODE_DIAM);
                }

                if (coinValues.containsKey(node) && !coinCollected.get(node)) {
                    if (coinImage != null) {
                        int cx = p.x + (NODE_DIAM / 2) - (COIN_ICON_SIZE / 2) - 5;
                        int cy = p.y - (NODE_DIAM / 2) - (COIN_ICON_SIZE / 2) + 5;
                        g2.drawImage(coinImage, cx, cy, COIN_ICON_SIZE, COIN_ICON_SIZE, null);
                    } else {
                        // Fallback jika tidak ada gambar koin: lingkaran kuning
                        g2.setColor(Color.YELLOW);
                        g2.fillOval(p.x + r - 15, p.y - r, 15, 15);
                    }
                }

                g2.setStroke(new BasicStroke(3));
                if (isPrime(node)) {
                    g2.setColor(PRIME_OUTLINE);
                    g2.drawOval(p.x - r, p.y - r, NODE_DIAM, NODE_DIAM);
                } else if (node % 5 == 0) {
                    g2.setColor(MULT5_OUTLINE);
                    g2.drawOval(p.x - r, p.y - r, NODE_DIAM, NODE_DIAM);
                } else {
                    g2.setColor(Color.GRAY);
                    g2.setStroke(new BasicStroke(1));
                    g2.drawOval(p.x - r, p.y - r, NODE_DIAM, NODE_DIAM);
                }

                g2.setColor(Color.BLACK);
                g2.setFont(POPS_BOLD.deriveFont(12f));
                String label = String.valueOf(node);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(label, p.x - fm.stringWidth(label) / 2, p.y + fm.getAscent() / 2 - 2);
            }

            Map<Integer, List<Integer>> nodePlayers = new HashMap<>();
            for (int p = 0; p < playerCount; p++) {
                nodePlayers.computeIfAbsent(playerPos[p], k -> new ArrayList<>()).add(p);
            }

            for (Map.Entry<Integer, List<Integer>> entry : nodePlayers.entrySet()) {
                int node = entry.getKey();
                List<Integer> players = entry.getValue();
                int count = players.size();
                Point center = getTileCenter(node);

                for (int i = 0; i < count; i++) {
                    int pIndex = players.get(i);
                    int ox = 0, oy = 0;
                    if (count == 2) {
                        ox = (i == 0) ? -12 : 12;
                    } else if (count == 3) {
                        if (i == 0) { ox = -12; oy = -10; }
                        else if (i == 1) { ox = 12; oy = -10; }
                        else { oy = 12; }
                    } else if (count >= 4) {
                        if (i == 0) { ox = -12; oy = -12; }
                        else if (i == 1) { ox = 12; oy = -12; }
                        else if (i == 2) { ox = -12; oy = 12; }
                        else { ox = 12; oy = 12; }
                    }

                    int drawX = center.x + ox - CHAR_WIDTH / 2;
                    int drawY = center.y + oy - CHAR_HEIGHT / 2;

                    if (playerImages[pIndex] != null) {
                        g2.drawImage(playerImages[pIndex], drawX, drawY, CHAR_WIDTH, CHAR_HEIGHT, null);
                    } else {
                        g2.setColor(PLAYER_COLORS[pIndex]);
                        g2.fillOval(center.x + ox - 8, center.y + oy - 8, 16, 16);
                        g2.setColor(Color.BLACK);
                        g2.drawOval(center.x + ox - 8, center.y + oy - 8, 16, 16);
                    }
                }
            }
            g2.dispose();
        }

        private Point getTileCenter(int node) {
            int idx = node - 1;
            int r = idx / COLS;
            int c = idx % COLS;
            if (r % 2 != 0) c = COLS - 1 - c;
            return new Point(
                    PAD_LEFT + c * (NODE_DIAM + GAP) + NODE_DIAM / 2,
                    PAD_TOP + r * (NODE_DIAM + GAP) + NODE_DIAM / 2
            );
        }
    }

    private class PlayerTurnPanel extends JPanel {
        private final JLabel lblTitle = new JLabel("PLAYER TURN");
        private final JLabel lblImage = new JLabel();
        private final JLabel lblInfo = new JLabel();
        private final Color maroonColor = new Color(124, 89, 239);

        public PlayerTurnPanel() {

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 10, 5, 10);

            lblTitle.setFont(POPS_BOLD.deriveFont(16f));
            lblTitle.setForeground(Color.BLACK);
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.WEST;
            add(lblTitle, gbc);

            lblImage.setPreferredSize(new Dimension(50, 50));
            lblImage.setHorizontalAlignment(SwingConstants.CENTER);
            gbc.gridx = 0; gbc.gridy = 1;
            gbc.gridwidth = 1;
            gbc.anchor = GridBagConstraints.CENTER;
            add(lblImage, gbc);

            lblInfo.setFont(POPS_REGULAR.deriveFont(14f)); //Nentuin size player yang ada pada section kanan
            lblInfo.setForeground(maroonColor);
            gbc.gridx = 1; gbc.gridy = 1;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.weightx = 1.0;
            add(lblInfo, gbc);
        }

        public void updateTurn(int playerIndex, String playerName, int score) {
            if (playerIndex >= 0 && playerIndex < playerImages.length && playerImages[playerIndex] != null) {
                ImageIcon icon = new ImageIcon(playerImages[playerIndex].getScaledInstance(50, 50, Image.SCALE_SMOOTH));
                lblImage.setIcon(icon);
            } else {
                lblImage.setIcon(null);
            }
            lblInfo.setText(playerName + " : (Score : " + score + ")");
            revalidate();
            repaint();
        }
    }

    private class DicePanel extends JPanel {
        int val = 1;
        boolean fwd = true;

        DicePanel() {
            setBackground(BG_BLUE_DARK);
        }

        void show(int v, boolean f) {
            val = v;
            fwd = f;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int sz = Math.min(getWidth(), getHeight()) - 40;
            int x = (getWidth() - sz) / 2;
            int y = 20;
            g2.setColor(fwd ? new Color(60, 180, 60) : new Color(200, 60, 60));
            g2.fillRoundRect(x, y, sz, sz, 20, 20);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(x + 10, y + 10, sz - 20, sz - 20, 15, 15);
            g2.setColor(Color.BLACK);
            int pip = sz / 6;
            if (val % 2 != 0) fillPip(g2, x + sz / 2, y + sz / 2, pip);
            if (val > 1) { fillPip(g2, x + sz / 4, y + sz / 4, pip); fillPip(g2, x + 3 * sz / 4, y + 3 * sz / 4, pip); }
            if (val > 3) { fillPip(g2, x + 3 * sz / 4, y + sz / 4, pip); fillPip(g2, x + sz / 4, y + 3 * sz / 4, pip); }
            if (val == 6) { fillPip(g2, x + sz / 4, y + sz / 2, pip); fillPip(g2, x + 3 * sz / 4, y + sz / 2, pip); }
        }

        void fillPip(Graphics2D g, int x, int y, int s) {
            g.fillOval(x - s / 2, y - s / 2, s, s);
        }
    }

    private class StatsPanel extends JPanel {
        StatsPanel() {
            setBackground(BG_BLUE_DARK);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.WHITE);
            g.setFont(POPS_BOLD.deriveFont(13f));
            g.drawString("TOP WINS", 10, 20);
            g.drawString("TOP SCORES", 200, 20);
            g.drawLine(10, 25, 360, 25);
            g.setFont(POPS_REGULAR.deriveFont(10f));
            int y = 45;
            List<Map.Entry<String, Integer>> wins = new ArrayList<>(WIN_SCORES.entrySet());
            wins.sort((a, b) -> b.getValue() - a.getValue());
            for (int i = 0; i < Math.min(3, wins.size()); i++) {
                g.drawString((i + 1) + ". " + wins.get(i).getKey() + " (" + wins.get(i).getValue() + ")", 10, y + i * 20);
            }
            List<Map.Entry<String, Integer>> coins = new ArrayList<>(CUMULATIVE_SCORES.entrySet());
            coins.sort((a, b) -> b.getValue() - a.getValue());
            for (int i = 0; i < Math.min(3, coins.size()); i++) {
                g.drawString((i + 1) + ". " + coins.get(i).getKey() + " (" + coins.get(i).getValue() + ")", 200, y + i * 20);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameBoard::new);
    }
}