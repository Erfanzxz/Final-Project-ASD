import javax.swing.*;
import java.awt.*;
import java.util.Random;

/**
 * GameBoardRightPanel.java
 *
 * 10x10 serpentine board (1..100) using the user's colorA/colorB.
 * Right-side control panel contains:
 *  - Turn label
 *  - Roll button
 *  - Dice result label
 *  - Pixel-art dice visualization (chosen option E)
 *
 * Features:
 *  - Players (2..4) start at tile 1
 *  - Prime-number tiles marked with ♥
 *  - Multiples-of-5 tiles marked with ★ and give an extra move when landed upon
 *  - Zig-zag numbering: bottom row left->right, next row right->left, etc.
 *
 * Usage: compile and run. (javac GameBoardRightPanel.java && java GameBoardRightPanel)
 */
public class GameBoardRightPanel extends JFrame {

    private static final int SIZE = 10;              // 10x10 board
    private static final int TOTAL_TILES = SIZE * SIZE;

    // Colors the user insisted on (not changed)
    private final Color colorA = new Color(100, 200, 255);
    private final Color colorB = new Color(255, 180, 120);

    private final JPanel boardPanel = new JPanel(new GridLayout(SIZE, SIZE));
    private final TileLabel[] tiles = new TileLabel[TOTAL_TILES];

    private final Random rand = new Random();

    // Players
    private int playerCount = 2;
    private final int[] playerPos = new int[4]; // supports up to 4 players; indices 0..playerCount-1
    private final Color[] playerColors = {
            new Color(30, 120, 220),   // P1
            new Color(220, 50, 50),    // P2
            new Color(60, 170, 80),    // P3
            new Color(200, 120, 30)    // P4
    };
    private int currentPlayer = 0; // 0-based player index

    // Probability settings (green = forward)
    private final double PROB_GREEN = 0.7;

    // Right control UI
    private final JLabel turnLabel = new JLabel("");
    private final JLabel diceResultLabel = new JLabel("Dice: -");
    private final PixelDicePanel pixelDicePanel = new PixelDicePanel();
    private final JButton rollButton = new JButton("ROLL");

    public GameBoardRightPanel() {
        askPlayerCount();
        initPlayers();
        initUI();
        redrawBoard();
        updateTurnLabel();
    }

    private void askPlayerCount() {
        String[] options = {"2", "3", "4"};
        String input = (String) JOptionPane.showInputDialog(
                this,
                "Berapa player? (2 - 4)",
                "Jumlah Pemain",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );
        if (input == null) System.exit(0);
        try {
            int n = Integer.parseInt(input);
            if (n < 2 || n > 4) n = 2;
            playerCount = n;
        } catch (Exception ex) {
            playerCount = 2;
        }
    }

    private void initPlayers() {
        for (int i = 0; i < playerPos.length; i++) playerPos[i] = 1; // start at tile 1
        currentPlayer = 0;
    }

    private void initUI() {
        setTitle("Ular Tangga 10x10 — Right-side Controls + Pixel Dice");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 980);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(12, 12));

        // BOARD (left)
        boardPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        int total = TOTAL_TILES;
        for (int i = 0; i < total; i++) {
            TileLabel tile = new TileLabel();
            tiles[i] = tile;
            boardPanel.add(tile);
        }

        // RIGHT-SIDE CONTROL PANEL
        JPanel rightPanel = new JPanel();
        rightPanel.setPreferredSize(new Dimension(320, 0));
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(20, 16, 20, 16));
        rightPanel.setBackground(new Color(42, 42, 46));

        // Turn label
        turnLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        turnLabel.setForeground(Color.WHITE);
        turnLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Dice result
        diceResultLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        diceResultLabel.setForeground(Color.WHITE);
        diceResultLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Pixel dice panel
        pixelDicePanel.setPreferredSize(new Dimension(180, 180));
        pixelDicePanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Roll button styling
        rollButton.setFont(new Font("SansSerif", Font.BOLD, 20));
        rollButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        rollButton.setFocusPainted(false);

        // Spacing & add components
        rightPanel.add(turnLabel);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 18)));
        rightPanel.add(pixelDicePanel);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 12)));
        rightPanel.add(diceResultLabel);
        rightPanel.add(Box.createRigidArea(new Dimension(0, 18)));
        rightPanel.add(rollButton);
        rightPanel.add(Box.createVerticalGlue());

        // Button action
        rollButton.addActionListener(e -> performRoll());

        // Add left and right to frame
        add(boardPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        setVisible(true);
    }

    // Perform a dice roll, update dice UI and move the current player
    private void performRoll() {
        rollButton.setEnabled(false);

        int dice = rand.nextInt(6) + 1;
        boolean isGreen = rand.nextDouble() < PROB_GREEN;
        diceResultLabel.setText("Dice: " + dice + "  (" + (isGreen ? "GREEN" : "RED") + ")");
        pixelDicePanel.setDice(dice, isGreen);

        int pos = playerPos[currentPlayer];

        if (!isGreen && pos == 1) {
            // cannot move back from 1
            JOptionPane.showMessageDialog(this, "Player " + (currentPlayer + 1) + " rolled RED but cannot move back from 1.");
            endTurn();
            return;
        }

        int target = isGreen ? Math.min(pos + dice, TOTAL_TILES) : Math.max(pos - dice, 1);

        // Move instantly (no per-cell animation required here). Update position.
        playerPos[currentPlayer] = target;
        redrawBoard();

        // Check win
        if (playerPos[currentPlayer] >= TOTAL_TILES) {
            JOptionPane.showMessageDialog(this, "Player " + (currentPlayer + 1) + " menang!");
            System.exit(0);
        }

        // If landed on multiple of 5 => extra turn
        if (target % 5 == 0) {
            JOptionPane.showMessageDialog(this, "Player " + (currentPlayer + 1) + " landed on multiple of 5 — EXTRA TURN!");
            // do not advance currentPlayer; allow re-roll
            rollButton.setEnabled(true);
            updateTurnLabel();
            return;
        }

        // Normal end-of-turn: switch player
        endTurn();
    }

    private void endTurn() {
        currentPlayer = (currentPlayer + 1) % playerCount;
        updateTurnLabel();
        rollButton.setEnabled(true);
    }

    private void updateTurnLabel() {
        turnLabel.setText("Turn: Player " + (currentPlayer + 1));
        turnLabel.setForeground(playerColors[currentPlayer]);
    }

    // Rebuild tile visuals and overlay players
    private void redrawBoard() {
        // fill tiles in grid order (index 0..99). But tile index mapping uses serpentine bottom-up
        for (int idx = 0; idx < TOTAL_TILES; idx++) {
            int boardNum = indexToBoardNumber(idx);
            TileLabel tile = tiles[idx];
            tile.setNumber(boardNum);
            // Set base color according to original colorA/colorB pattern
            // We must reproduce the same mapping user requested: colorA/colorB alternating per checkerboard
            // Use row and col in display coordinates:
            int row = idx / SIZE;
            int col = idx % SIZE;
            boolean light = ((row + col) % 2 == 0);
            tile.setBackground(light ? colorA : colorB);

            // prime marker and multiple-of-5 marker
            tile.setPrime(isPrime(boardNum));
            tile.setStar(boardNum % 5 == 0);
            tile.clearPlayers();
        }

        // overlay players
        for (int p = 0; p < playerCount; p++) {
            int boardNumber = playerPos[p];
            int idx = boardNumberToIndex(boardNumber);
            if (idx >= 0 && idx < TOTAL_TILES) {
                tiles[idx].addPlayer(p, playerColors[p]);
            }
        }

        boardPanel.revalidate();
        boardPanel.repaint();
        updateTurnLabel();
    }

    // Convert tile array index (0..99) -> board number (1..100) with serpentine bottom-up mapping.
    // idx maps left-to-right, top-to-bottom for components in grid (0..99). We want board numbering to:
    // bottom row = numbers 1..10 (left->right)
    // next row up = 11..20 (right->left)
    // ...
    private int indexToBoardNumber(int idx) {
        int displayRowFromTop = idx / SIZE;     // 0 = top row
        int displayColFromLeft = idx % SIZE;    // 0 = leftmost
        int rowFromBottom = SIZE - 1 - displayRowFromTop; // 0 = bottom
        boolean leftToRight = (rowFromBottom % 2 == 0);   // bottom row left->right

        int boardCol = leftToRight ? displayColFromLeft : (SIZE - 1 - displayColFromLeft);
        int number = rowFromBottom * SIZE + boardCol + 1;
        return number;
    }

    // Convert board number (1..100) to tile index in component order (0..99)
    private int boardNumberToIndex(int number) {
        if (number < 1) number = 1;
        if (number > TOTAL_TILES) number = TOTAL_TILES;
        int n = number - 1;
        int rowFromBottom = n / SIZE;   // 0 = bottom
        int colInRow = n % SIZE;
        boolean leftToRightRow = (rowFromBottom % 2 == 0);
        int col = leftToRightRow ? colInRow : (SIZE - 1 - colInRow);

        int displayRowFromTop = SIZE - 1 - rowFromBottom;
        int idx = displayRowFromTop * SIZE + col;
        return idx;
    }

    // Prime check
    private boolean isPrime(int n) {
        if (n <= 1) return false;
        if (n == 2) return true;
        if (n % 2 == 0) return false;
        for (int i = 3; i * i <= n; i += 2) {
            if (n % i == 0) return false;
        }
        return true;
    }

    // ------------------------
    // Custom TileLabel component
    // ------------------------
    static class TileLabel extends JPanel {
        private int number;
        private boolean isPrime;
        private boolean isStar;
        private java.util.List<PlayerMark> players = new java.util.ArrayList<>();

        public TileLabel() {
            setLayout(null); // we'll draw in paintComponent
            setPreferredSize(new Dimension(70, 70));
            setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
        }

        public void setNumber(int number) {
            this.number = number;
            repaint();
        }

        public void setPrime(boolean v) {
            this.isPrime = v;
            repaint();
        }

        public void setStar(boolean v) {
            this.isStar = v;
            repaint();
        }

        public void addPlayer(int index, Color color) {
            players.add(new PlayerMark(index, color));
            repaint();
        }

        public void clearPlayers() {
            players.clear();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics gg) {
            super.paintComponent(gg);
            Graphics2D g = (Graphics2D) gg.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // background already set by caller with setBackground()
            // draw rounded rect for nicer look
            int w = getWidth(), h = getHeight();
            g.setColor(getBackground());
            g.fillRoundRect(0, 0, w, h, 12, 12);

            // draw border
            g.setColor(new Color(80, 80, 90));
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);

            // draw number badge top-left
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(new Color(30, 30, 36));
            g.drawString(String.valueOf(number), 8, 16);

            // prime marker (heart) top-right if prime
            if (isPrime) {
                int size = 12;
                int px = w - size - 6;
                int py = 6;
                g.setColor(new Color(220, 30, 80));
                drawHeart(g, px, py, size, size);
            }

            // star marker (gold) slightly below top-right if multiple of 5
            if (isStar) {
                int size = 12;
                int sx = w - size - 6;
                int sy = 24;
                g.setColor(new Color(255, 215, 60));
                drawStar(g, sx + size / 2, sy + size / 2, size / 2 + 2, size / 3);
            }

            // draw big faded number in center
            g.setFont(new Font("SansSerif", Font.BOLD, Math.max(18, h / 3)));
            g.setColor(new Color(0, 0, 0, 25));
            FontMetrics fm = g.getFontMetrics();
            String s = String.valueOf(number);
            int sw = fm.stringWidth(s);
            g.drawString(s, (w - sw) / 2, h / 2 + fm.getAscent() / 2 - 4);

            // draw players as small circles aligned at bottom-left stacked
            int pxBase = 8;
            int pyBase = h - 18;
            int r = 12;
            for (PlayerMark pm : players) {
                g.setColor(pm.color);
                g.fillOval(pxBase, pyBase - r / 2, r, r);
                g.setColor(Color.white);
                g.setFont(new Font("SansSerif", Font.BOLD, 10));
                String lbl = "P" + (pm.index + 1);
                FontMetrics pfm = g.getFontMetrics();
                int tx = pxBase + (r - pfm.stringWidth(lbl)) / 2;
                int ty = pyBase + pfm.getAscent() / 2 - 2;
                g.drawString(lbl, tx, ty);
                pxBase += r + 6;
            }

            g.dispose();
        }

        private void drawHeart(Graphics2D g, int x, int y, int w, int h) {
            // simple heart using two circles + triangle
            int cx1 = x + w / 4;
            int cx2 = x + 3 * w / 4;
            int cy = y + h / 3;
            g.fillOval(cx1 - w / 4, y, w / 2, h / 2);
            g.fillOval(cx2 - w / 4, y, w / 2, h / 2);
            Polygon p = new Polygon();
            p.addPoint(x, y + h / 3);
            p.addPoint(x + w, y + h / 3);
            p.addPoint(x + w / 2, y + h);
            g.fillPolygon(p);
        }

        private void drawStar(Graphics2D g, int cx, int cy, int outerR, int innerR) {
            Polygon star = new Polygon();
            double angle = Math.PI / 5;
            for (int i = 0; i < 10; i++) {
                double r = (i % 2 == 0) ? outerR : innerR;
                double a = i * angle - Math.PI / 2;
                int x = cx + (int) (Math.cos(a) * r);
                int y = cy + (int) (Math.sin(a) * r);
                star.addPoint(x, y);
            }
            g.fill(star);
        }

        private static class PlayerMark {
            int index;
            Color color;
            PlayerMark(int idx, Color color) { this.index = idx; this.color = color; }
        }
    }

    // ------------------------
    // Pixel-art Dice Panel (style E)
    // ------------------------
    static class PixelDicePanel extends JPanel {
        private int diceValue = 1;
        private boolean isGreen = true;

        // Pixel grid (3x3 pips positions), we'll draw small squares as pips
        @Override
        protected void paintComponent(Graphics gg) {
            super.paintComponent(gg);
            Graphics2D g = (Graphics2D) gg.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // background
            g.setColor(new Color(28, 28, 30));
            g.fillRect(0, 0, w, h);

            // outer box colored by green/red
            int boxSize = Math.min(w, h) - 40;
            int bx = (w - boxSize) / 2;
            int by = 20;
            g.setStroke(new BasicStroke(6f));
            g.setColor(isGreen ? new Color(10, 150, 40) : new Color(170, 20, 20));
            g.drawRoundRect(bx, by, boxSize, boxSize, 12, 12);

            // inner white area
            g.setColor(Color.WHITE);
            g.fillRoundRect(bx + 6, by + 6, boxSize - 12, boxSize - 12, 8, 8);

            // pixel grid (3x3) positions for pips
            int cell = (boxSize - 24) / 5; // cell spacing
            int startX = bx + 12;
            int startY = by + 12;

            // define pip positions in a 3x3 grid (coords)
            Point[] pipCoords = new Point[]{
                    new Point(startX, startY),
                    new Point(startX + 2 * cell, startY),
                    new Point(startX + 4 * cell, startY),

                    new Point(startX, startY + 2 * cell),
                    new Point(startX + 2 * cell, startY + 2 * cell),
                    new Point(startX + 4 * cell, startY + 2 * cell),

                    new Point(startX, startY + 4 * cell),
                    new Point(startX + 2 * cell, startY + 4 * cell),
                    new Point(startX + 4 * cell, startY + 4 * cell)
            };

            // which pips to draw for each dice value (classic pip layout)
            boolean[] show = new boolean[9];
            switch (diceValue) {
                case 1: show[4] = true; break;
                case 2: show[0]=true; show[8]=true; break;
                case 3: show[0]=true; show[4]=true; show[8]=true; break;
                case 4: show[0]=true; show[2]=true; show[6]=true; show[8]=true; break;
                case 5: show[0]=true; show[2]=true; show[4]=true; show[6]=true; show[8]=true; break;
                case 6: show[0]=true; show[1]=true; show[2]=true; show[6]=true; show[7]=true; show[8]=true; break;
            }

            // draw pips as small squares ("pixel-art")
            g.setColor(Color.BLACK);
            int pipSize = Math.max(6, cell); // square side
            for (int i = 0; i < pipCoords.length; i++) {
                if (show[i]) {
                    Point p = pipCoords[i];
                    // draw a 3x3 block of small squares to make "pixel" feel
                    int block = Math.max(4, pipSize / 3);
                    for (int bxOff = 0; bxOff < 3; bxOff++) {
                        for (int byOff = 0; byOff < 3; byOff++) {
                            int sx = p.x + bxOff * block - block;
                            int sy = p.y + byOff * block - block;
                            g.fillRect(sx, sy, block, block);
                        }
                    }
                }
            }

            // draw numeric label below
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 20));
            String s = " " + diceValue + " ";
            FontMetrics fm = g.getFontMetrics();
            int tx = (w - fm.stringWidth(s)) / 2;
            g.drawString(s, tx, by + boxSize + 30);

            g.dispose();
        }

        public void setDice(int value, boolean isGreen) {
            this.diceValue = value;
            this.isGreen = isGreen;
            repaint();
        }
    }

    // Entry point
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameBoardRightPanel());
    }
}
