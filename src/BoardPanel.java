import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class BoardPanel extends JPanel {

    private MultiPositionProvider mpProvider;
    private Map<Integer, List<Integer>> trail;
    private Map<Integer, Integer> ladders;

    public BoardPanel(MultiPositionProvider mp, Map<Integer, List<Integer>> trail, Map<Integer, Integer> ladders) {
        this.mpProvider = mp;
        this.trail = trail;
        this.ladders = ladders;
        setPreferredSize(new Dimension(600, 600));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int rows = 10, cols = 10;
        int tileSize = getWidth() / cols;

        Color a = new Color(220, 220, 220);
        Color b = new Color(180, 180, 180);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {

                int number = computeNumber(r, c);

                Color tileColor = ((r + c) % 2 == 0) ? a : b;
                g.setColor(tileColor);
                g.fillRect(c * tileSize, r * tileSize, tileSize, tileSize);

                g.setColor(Color.BLACK);
                g.drawRect(c * tileSize, r * tileSize, tileSize, tileSize);

                if (isPrime(number)) {
                    g.setColor(Color.RED);
                    g.drawString("♥", c * tileSize + 5, r * tileSize + 15);
                }

                if (number % 5 == 0) {
                    g.setColor(Color.BLUE);
                    g.drawString("★", c * tileSize + tileSize - 15, r * tileSize + 15);
                }

                g.setColor(Color.BLACK);
                g.drawString(String.valueOf(number), c * tileSize + 10, r * tileSize + 30);
            }
        }

        // render LADDER
        g.setColor(Color.GREEN.darker());
        for (Map.Entry<Integer, Integer> e : ladders.entrySet()) {
            int from = e.getKey();
            int to = e.getValue();

            Point p1 = getTileCenter(from, tileSize);
            Point p2 = getTileCenter(to, tileSize);

            Graphics2D g2 = (Graphics2D) g;
            g2.setStroke(new BasicStroke(4));
            g2.drawLine(p1.x, p1.y, p2.x, p2.y);
        }

        // JEJAK (trail)
        for (int p = 0; p < trail.size(); p++) {
            List<Integer> steps = trail.get(p);

            for (int i = 1; i < steps.size(); i++) {
                int from = steps.get(i - 1);
                int to = steps.get(i);

                Point f = getTileCenter(from, tileSize);
                Point t = getTileCenter(to, tileSize);

                Graphics2D g2 = (Graphics2D) g;
                g2.setStroke(new BasicStroke(6));

                if (to > from) g2.setColor(new Color(0, 255, 0, 80));     // hijau soft
                else g2.setColor(new Color(255, 0, 0, 80));               // merah soft

                g2.drawLine(f.x, f.y, t.x, t.y);
            }
        }

        int[] pos = mpProvider.getPositions();
        Color[] colors = {Color.ORANGE, Color.CYAN, Color.MAGENTA, Color.GREEN};

        for (int i = 0; i < pos.length; i++) {
            drawPlayer(g, pos[i], tileSize, colors[i]);
        }
    }

    private Point getTileCenter(int number, int tileSize) {
        int idx = number - 1;
        int row = idx / 10;
        int col = idx % 10;

        row = 9 - row;
        boolean leftToRight = ((9 - row) % 2 == 0);
        if (!leftToRight) col = 9 - col;

        return new Point(col * tileSize + tileSize / 2, row * tileSize + tileSize / 2);
    }

    private int computeNumber(int row, int col) {
        int base = (9 - row) * 10;
        boolean leftToRight = ((9 - row) % 2 == 0);
        if (leftToRight) return base + col + 1;
        else return base + (10 - col);
    }

    private void drawPlayer(Graphics g, int pos, int tileSize, Color color) {
        Point p = getTileCenter(pos, tileSize);
        g.setColor(color);
        g.fillOval(p.x - 10, p.y - 10, 20, 20);
    }

    private boolean isPrime(int n) {
        if (n < 2) return false;
        for (int i = 2; i * i <= n; i++)
            if (n % i == 0) return false;
        return true;
    }
}
