import javax.swing.*;
import java.awt.*;

public class PixelDicePanel extends JPanel {

    private int value = 1;

    public PixelDicePanel() {
        setPreferredSize(new Dimension(120, 120));
    }

    public void setValue(int v) {
        this.value = v;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int size = 90;
        int x = 15, y = 15;

        g.setColor(Color.WHITE);
        g.fillRoundRect(x, y, size, size, 20, 20);

        g.setColor(Color.BLACK);
        g.drawRoundRect(x, y, size, size, 20, 20);

        drawDots(g, x, y, size, value);
    }

    private void drawDots(Graphics g, int x, int y, int size, int v) {
        int dot = 12;
        int off = 20;

        int cx = x + size / 2 - dot / 2;
        int cy = y + size / 2 - dot / 2;

        if (v == 1 || v == 3 || v == 5) g.fillOval(cx, cy, dot, dot);
        if (v >= 2) {
            g.fillOval(x + off, y + off, dot, dot);
            g.fillOval(x + size - off - dot, y + size - off - dot, dot, dot);
        }
        if (v >= 4) {
            g.fillOval(x + off, y + size - off - dot, dot, dot);
            g.fillOval(x + size - off - dot, y + off, dot, dot);
        }
        if (v == 6) {
            g.fillOval(cx, y + off, dot, dot);
            g.fillOval(cx, y + size - off - dot, dot, dot);
        }
    }
}
