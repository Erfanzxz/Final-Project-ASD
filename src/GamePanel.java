import javax.swing.*;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class GamePanel extends JPanel {

    private BoardPanel boardPanel;
    private SidePanel sidePanel;

    private int[] playerPos;
    private int currentPlayer = 0;
    private int playerCount;

    private Map<Integer, List<Integer>> trail;   // Jejak per player
    private Map<Integer, Integer> ladders;       // Tangga

    public GamePanel(int playerCount) {
        this.playerCount = playerCount;
        this.playerPos = new int[playerCount];
        this.trail = new HashMap<>();

        // init posisi awal & trail
        for (int i = 0; i < playerCount; i++) {
            playerPos[i] = 1;
            trail.put(i, new ArrayList<>());
            trail.get(i).add(1);
        }

        // definisi tangga bebas
        ladders = new HashMap<>();
        ladders.put(4, 14);
        ladders.put(22, 41);
        ladders.put(55, 78);

        setLayout(new BorderLayout());

        // pake lambda, supaya cocok dgn MultiPositionProvider
        boardPanel = new BoardPanel(() -> playerPos, trail, ladders);
        sidePanel = new SidePanel(playerCount);

        // event roll dice
        sidePanel.setOnRollListener(() -> handleRoll());

        add(boardPanel, BorderLayout.CENTER);
        add(sidePanel, BorderLayout.EAST);
    }

    private void handleRoll() {
        int roll = Dice.roll();
        sidePanel.setDiceValue(roll);

        int start = playerPos[currentPlayer];
        int target = Math.min(100, start + roll);

        // kalau start adalah angka prima
        if (isPrime(start)) {
            // kalau di depan ada tangga
            if (ladders.containsKey(start + 1)) {
                target = ladders.get(start + 1) + (roll - 1);
            }
        }

        boolean forward = target > start;
        updateTrail(currentPlayer, start, target, forward);

        playerPos[currentPlayer] = target;

        // next turn
        currentPlayer = (currentPlayer + 1) % playerCount;

        boardPanel.repaint();
        sidePanel.setTurn(currentPlayer + 1);
    }

    private void updateTrail(int player, int from, int to, boolean forward) {
        List<Integer> t = trail.get(player);

        if (forward) {
            for (int i = from + 1; i <= to; i++) t.add(i);
        } else {
            for (int i = from - 1; i >= to; i--) t.add(i);
        }
    }

    private boolean isPrime(int n) {
        if (n < 2) return false;
        for (int i = 2; i * i <= n; i++) {
            if (n % i == 0) return false;
        }
        return true;
    }
} //adaperubahan
