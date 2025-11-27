import javax.swing.*;
import java.awt.*;

public class PlayerSelectFrame extends JFrame {

    public PlayerSelectFrame() {
        setTitle("Select Number of Players");
        setSize(300, 150);
        setLayout(new FlowLayout());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel label = new JLabel("Jumlah pemain:");
        Integer[] players = {2, 3, 4};
        JComboBox<Integer> dropdown = new JComboBox<>(players);

        JButton startBtn = new JButton("Start Game");

        add(label);
        add(dropdown);
        add(startBtn);

        startBtn.addActionListener(e -> {
            int selected = (int) dropdown.getSelectedItem();
            new GameFrame(selected);
            dispose();
        });

        setVisible(true);
    }
}
