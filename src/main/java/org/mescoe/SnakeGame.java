package org.mescoe;
import javax.swing.*;
import java.awt.*;

public class SnakeGame extends JFrame {

	 private final CardLayout cards = new CardLayout();
	    private final JPanel root = new JPanel(cards);
	    private Board board;

	    SnakeGame() {
	        super("Snake Game");
	        setDefaultCloseOperation(EXIT_ON_CLOSE);

	        root.add(new MenuPanel(this::startGame), "menu");
	        add(root);
	        pack();                       // sizes the frame to fit the panels
	        setLocationRelativeTo(null);  // centre on screen
	        setResizable(false);
	    }

	    private void startGame(String playerName) {
	        if (board != null) {
	            board.stop();
	            root.remove(board);
	        }
	        board = new Board(playerName, this::showMenu);
	        root.add(board, "game");
	        cards.show(root, "game");
	        board.requestFocusInWindow();
	    }

	    private void showMenu() {
	        if (board != null) board.stop();
	        cards.show(root, "menu");
	    }

	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		SwingUtilities.invokeLater(() -> {
            try {
                // cross-platform look so button colours render the same on every OS
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) { }
            new SnakeGame().setVisible(true);
        });

	}

}
