import javax.swing.*;
import java.awt.*;

public class GameFrame extends JFrame {

    public GameFrame(int playerCount) {
        setTitle("Snakes & Ladders Game");
        setSize(900, 640);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        GamePanel panel = new GamePanel(playerCount);

        add(panel);
        setVisible(true);
    }
} //Ada perubahan
