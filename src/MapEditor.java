import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.imageio.ImageIO;
import java.util.Arrays;

public class MapEditor extends JFrame {

    // --- PATH KONFIGURASI (SESUAIKAN JIKA PERLU) ---
    private static final String ROOT_PATH = "C:\\Intellij Idea\\Final Project SEM 3\\FP ASD\\";
    private static final String PATH_BG_IMG = ROOT_PATH + "C:\\Intellij Idea\\Final Project SEM 3\\FP ASD\\Background Game\\Game Background.png";
    private static final String SAVE_FILE_PATH = "coordinates.txt"; // File penyimpanan otomatis

    private static final int TILE_COUNT = 100;
    private static final int NODE_DIAM = 35;
    private static final int GAP = 15;
    private static final int COLS = 10;

    // Canvas & Data
    private EditorCanvas canvas;
    private Point[] nodeCoordinates = new Point[TILE_COUNT + 1];
    private int draggedNode = -1;

    public MapEditor() {
        super("MAP EDITOR - FINAL PROJECT ASD");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Init Default Grid Positions
        for (int i = 1; i <= TILE_COUNT; i++) {
            nodeCoordinates[i] = calculateGridPosition(i);
        }

        // Coba load data lama jika ada
        loadCoordinates();

        // Setup UI
        canvas = new EditorCanvas();
        add(canvas, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        JButton btnSave = new JButton("SAVE COORDINATES");
        JButton btnLoadBg = new JButton("CHANGE BACKGROUND");

        btnSave.setBackground(Color.ORANGE);
        btnSave.addActionListener(e -> saveCoordinates());

        btnLoadBg.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(ROOT_PATH);
            fc.setFileFilter(new FileNameExtensionFilter("Images", "jpg", "png"));
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                canvas.setBackgroundImage(fc.getSelectedFile());
            }
        });

        controlPanel.add(btnLoadBg);
        controlPanel.add(btnSave);
        add(controlPanel, BorderLayout.SOUTH);

        setSize(900, 900);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private Point calculateGridPosition(int node) {
        int idx = node - 1;
        int r = idx / COLS;
        int c = idx % COLS;
        if (r % 2 != 0) c = COLS - 1 - c;
        return new Point(30 + c * (NODE_DIAM + GAP) + NODE_DIAM / 2, 30 + r * (NODE_DIAM + GAP) + NODE_DIAM / 2);
    }

    // --- FITUR SAVE KE FILE ---
    private void saveCoordinates() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(SAVE_FILE_PATH))) {
            for (int i = 1; i <= TILE_COUNT; i++) {
                writer.write(nodeCoordinates[i].x + "," + nodeCoordinates[i].y);
                writer.newLine();
            }
            JOptionPane.showMessageDialog(this, "Posisi Berhasil Disimpan ke " + SAVE_FILE_PATH + "!");
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal menyimpan: " + e.getMessage());
        }
    }

    // --- FITUR LOAD DARI FILE ---
    private void loadCoordinates() {
        File f = new File(SAVE_FILE_PATH);
        if (!f.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
            String line;
            int idx = 1;
            while ((line = reader.readLine()) != null && idx <= TILE_COUNT) {
                String[] parts = line.split(",");
                nodeCoordinates[idx] = new Point(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                idx++;
            }
        } catch (Exception e) {
            System.out.println("Gagal load koordinat lama.");
        }
    }

    // --- CANVAS EDITOR ---
    private class EditorCanvas extends JPanel {
        private Image bgImage;

        EditorCanvas() {
            try {
                File f = new File(PATH_BG_IMG);
                if (f.exists()) bgImage = ImageIO.read(f);
            } catch (IOException e) {}

            // Drag Logic
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    for (int i = 1; i <= TILE_COUNT; i++) {
                        if (nodeCoordinates[i].distance(e.getPoint()) < NODE_DIAM / 2.0) {
                            draggedNode = i;
                            break;
                        }
                    }
                }
                @Override
                public void mouseReleased(MouseEvent e) { draggedNode = -1; }
            });

            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (draggedNode != -1) {
                        nodeCoordinates[draggedNode] = e.getPoint();
                        repaint();
                    }
                }
            });
        }

        public void setBackgroundImage(File f) {
            try { bgImage = ImageIO.read(f); repaint(); } catch(IOException e){}
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (bgImage != null) g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
            else { g.setColor(Color.DARK_GRAY); g.fillRect(0, 0, getWidth(), getHeight()); }

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw Lines
            g2.setColor(new Color(255, 255, 255, 100));
            g2.setStroke(new BasicStroke(2));
            for (int i = 1; i < TILE_COUNT; i++) {
                g2.drawLine(nodeCoordinates[i].x, nodeCoordinates[i].y, nodeCoordinates[i+1].x, nodeCoordinates[i+1].y);
            }

            // Draw Nodes
            for (int i = 1; i <= TILE_COUNT; i++) {
                Point p = nodeCoordinates[i];
                g2.setColor(new Color(255, 255, 0, 180)); // Kuning Transparan
                g2.fillOval(p.x - NODE_DIAM/2, p.y - NODE_DIAM/2, NODE_DIAM, NODE_DIAM);

                g2.setColor(Color.BLACK);
                g2.drawOval(p.x - NODE_DIAM/2, p.y - NODE_DIAM/2, NODE_DIAM, NODE_DIAM);
                String label = String.valueOf(i);
                g2.drawString(label, p.x - 5, p.y + 5);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MapEditor::new);
    }
}