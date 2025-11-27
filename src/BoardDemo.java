import java.awt.*;
import javax.swing.*;

/**
 * BoardDemo.java
 *
 * Visualisasi board 10x10 serpentine (1..100).
 * - Baris 1 (paling bawah): kiri -> kanan
 * - Baris 2: kanan -> kiri
 * - Dan seterusnya
 *
 * Jalankan: javac BoardDemo.java && java BoardDemo
 */
public class BoardDemo {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Snakes & Ladders - Board (10x10)");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            BoardPanel board = new BoardPanel(10, 10, 64); // cols, rows, cellSize(px)
            board.setPreferredSize(new Dimension(board.getWidthPixels(), board.getHeightPixels()));

            // Tambahkan sedikit padding di frame
            JPanel container = new JPanel(new BorderLayout());
            container.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            container.add(board, BorderLayout.CENTER);

            f.setContentPane(container);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}

/**
 * BoardPanel - menggambar papan grid NxM dengan penomoran serpentine.
 */
class BoardPanel extends JPanel {
    private final int cols;
    private final int rows;
    private final int cellSize;
    private final int padding = 4;
    private final Font numberFont;

    public BoardPanel(int cols, int rows, int cellSize) {
        this.cols = cols;
        this.rows = rows;
        this.cellSize = cellSize;
        this.numberFont = new Font("SansSerif", Font.BOLD, Math.max(12, cellSize / 4));
        setBackground(new Color(0xE8F1D6)); // latar belakang lembut
        setOpaque(true);
    }

    public int getWidthPixels() {
        return cols * cellSize + padding * 2;
    }

    public int getHeightPixels() {
        return rows * cellSize + padding * 2;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(getWidthPixels(), getHeightPixels());
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        // kualitas rendering
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        // board origin
        int ox = padding;
        int oy = padding;

        // draw cells
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x = ox + c * cellSize;
                int y = oy + r * cellSize;

                // alternating color pattern like the sample (two-tone)
                boolean light = ((r + c) % 2 == 0);
                Color fill = light ? new Color(0xFFF7E6) : new Color(0xCFF0E1);
                g.setColor(fill);
                g.fillRect(x, y, cellSize, cellSize);

                // cell border
                g.setColor(new Color(0xC3C3C3));
                g.drawRect(x, y, cellSize, cellSize);
            }
        }

        // draw numbers with serpentine mapping
        g.setFont(numberFont);
        FontMetrics fm = g.getFontMetrics();

        for (int rowFromTop = 0; rowFromTop < rows; rowFromTop++) {
            int rowFromBottom = rows - 1 - rowFromTop; // 0 = bottom row
            boolean leftToRight = (rowFromBottom % 2 == 0);

            for (int col = 0; col < cols; col++) {
                int boardCol = leftToRight ? col : (cols - 1 - col);
                int number = rowFromBottom * cols + boardCol + 1;

                int x = ox + col * cellSize;
                int y = oy + rowFromTop * cellSize;

                // draw number at top-left (small badge)
                String s = Integer.toString(number);
                int tw = fm.stringWidth(s);
                int th = fm.getAscent();

                // small translucent badge background for readability
                int badgeW = Math.max(24, tw + 10);
                int badgeH = Math.max(18, th + 6);
                int bx = x + 6;
                int by = y + 6;
                g.setColor(new Color(255, 255, 255, 220));
                g.fillRoundRect(bx - 2, by - 2, badgeW, badgeH, 8, 8);
                g.setColor(new Color(160, 160, 160, 200));
                g.drawRoundRect(bx - 2, by - 2, badgeW, badgeH, 8, 8);

                // number
                g.setColor(new Color(40, 40, 40));
                int sx = bx + 6;
                int sy = by + (badgeH + th) / 2 - 6;
                g.drawString(s, sx, sy);

                // Optional: draw faint center number big and translucent (like sample)
                g.setColor(new Color(0, 0, 0, 20));
                float bigSize = cellSize / 2.4f;
                Font bigFont = numberFont.deriveFont(Font.BOLD, bigSize);
                g.setFont(bigFont);
                FontMetrics bf = g.getFontMetrics();
                String big = s;
                int bsw = bf.stringWidth(big);
                int bsh = bf.getAscent();
                int bxCenter = x + (cellSize - bsw) / 2;
                int byCenter = y + (cellSize + bsh) / 2 - 6;
                g.drawString(big, bxCenter, byCenter);

                // reset font
                g.setFont(numberFont);
            }
        }

        // optional legend/title
        g.setColor(new Color(30, 30, 30));
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        String title = "Board 10x10 — Serpentine numbers 1..100";
        g.drawString(title, ox, oy + rows * cellSize + 18);

        g.dispose();
    }
}
