import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Developer {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Maze Generator & Solver (Prim's + BFS/DFS)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            // Menambahkan Panel Gameplay
            Gameplay gameplay = new Gameplay();
            frame.add(gameplay);

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}