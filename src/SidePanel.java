import javax.swing.*;
import java.awt.*;

public class SidePanel extends JPanel {

    private JButton rollButton;
    private JLabel turnLabel;
    private PixelDicePanel dicePanel;
    private Runnable onRoll;

    public SidePanel(int playerCount) {
        setPreferredSize(new Dimension(260, 600));
        setLayout(new FlowLayout());

        turnLabel = new JLabel("Turn Player 1");
        rollButton = new JButton("Roll Dice");
        dicePanel = new PixelDicePanel();

        add(turnLabel);
        add(dicePanel);
        add(rollButton);

        rollButton.addActionListener(e -> {
            if (onRoll != null) onRoll.run();
        });
    }

    public void setTurn(int p) {
        turnLabel.setText("Turn Player " + p);
    }

    public void setDiceValue(int v) {
        dicePanel.setValue(v);
    }

    public void setOnRollListener(Runnable r) {
        this.onRoll = r;
    }
}