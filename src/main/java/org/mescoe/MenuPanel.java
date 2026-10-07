package org.mescoe;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

public class MenuPanel extends JPanel {

    private final JTextField nameField = new JTextField("Player", 14);

    MenuPanel(Consumer<String> onPlay) {
        setLayout(new GridBagLayout());
        setPreferredSize(new Dimension(Board.W, Board.H));

        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("SNAKE");
        title.setFont(new Font("SansSerif", Font.BOLD, 64));
        title.setForeground(new Color(74, 222, 128));
        title.setAlignmentX(CENTER_ALIGNMENT);

        JLabel sub = new JLabel("Enter your name");
        sub.setForeground(new Color(148, 163, 184));
        sub.setAlignmentX(CENTER_ALIGNMENT);

        nameField.setHorizontalAlignment(JTextField.CENTER);
        nameField.setFont(new Font("SansSerif", Font.BOLD, 16));
        nameField.setMaximumSize(new Dimension(220, 36));
        nameField.setAlignmentX(CENTER_ALIGNMENT);

        JButton play = styledButton("PLAY", new Color(34, 197, 94));
        JButton scores = styledButton("HIGH SCORES", new Color(59, 130, 246));
        JButton exit = styledButton("EXIT", new Color(239, 68, 68));

        play.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) name = "Player";
            if (name.length() > 30) name = name.substring(0, 30);
            onPlay.accept(name);
        });
        scores.addActionListener(e -> showHighScores());
        exit.addActionListener(e -> System.exit(0));

        box.add(title);
        box.add(Box.createVerticalStrut(30));
        box.add(sub);
        box.add(Box.createVerticalStrut(8));
        box.add(nameField);
        box.add(Box.createVerticalStrut(24));
        box.add(play);
        box.add(Box.createVerticalStrut(12));
        box.add(scores);
        box.add(Box.createVerticalStrut(12));
        box.add(exit);

        add(box);
    }

    private JButton styledButton(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 15));
        b.setForeground(Color.WHITE);
        b.setBackground(bg);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(220, 42));
        b.setPreferredSize(new Dimension(220, 42));
        return b;
    }

    private void showHighScores() {
        new SwingWorker<List<DatabaseManager.ScoreEntry>, Void>() {
            @Override protected List<DatabaseManager.ScoreEntry> doInBackground() throws Exception {
                return DatabaseManager.getTopScores(10);
            }
            @Override protected void done() {
                String msg;
                try {
                    List<DatabaseManager.ScoreEntry> list = get();
                    if (list.isEmpty()) {
                        msg = "No scores yet - be the first!";
                    } else {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < list.size(); i++) {
                            DatabaseManager.ScoreEntry e = list.get(i);
                            sb.append(String.format("%2d.  %-15s %5d   %s%n",
                                    i + 1, e.name, e.score, e.playedAt.toString().substring(0, 16)));
                        }
                        msg = sb.toString();
                    }
                } catch (Exception ex) {
                    msg = "Could not reach the database.\nCheck URL/user/password in DatabaseManager.";
                }
                JTextArea area = new JTextArea(msg);
                area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
                area.setEditable(false);
                JOptionPane.showMessageDialog(MenuPanel.this, area, "High Scores", JOptionPane.PLAIN_MESSAGE);
            }
        }.execute();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setPaint(new GradientPaint(0, 0, new Color(15, 23, 42), 0, getHeight(), new Color(2, 6, 23)));
        g2.fillRect(0, 0, getWidth(), getHeight());
    }
}
