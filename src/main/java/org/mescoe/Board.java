package org.mescoe;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Board extends JPanel implements ActionListener {

    static final int CELL = 20, COLS = 25, ROWS = 25, HUD = 50;
    static final int W = COLS * CELL, H = ROWS * CELL + HUD;

    private enum State { READY, RUNNING, PAUSED, GAME_OVER }

    private static class Particle {
        float x, y, vx, vy, life = 1f;
        Particle(float x, float y, float vx, float vy) { this.x = x; this.y = y; this.vx = vx; this.vy = vy; }
    }

    private final LinkedList<Point> snake = new LinkedList<>();
    private final Point apple = new Point();
    private final List<Particle> particles = new ArrayList<>();
    private final Random rnd = new Random();

    private int dx, dy, nextDx, nextDy;
    private int score, bestScore, level;
    private long frame;
    private State state;

    private List<DatabaseManager.ScoreEntry> topScores = new ArrayList<>();
    private boolean dbOk = true, scoresLoaded = false;

    private final String playerName;
    private final Runnable onExitToMenu;
    private final Timer gameTimer;   // game logic
    private final Timer animTimer;   // smooth animation (~60fps)

    Board(String playerName, Runnable onExitToMenu) {
        this.playerName = playerName;
        this.onExitToMenu = onExitToMenu;

        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        addKeyListener(new TAdapter());

        gameTimer = new Timer(150, this);
        animTimer = new Timer(16, e -> {
            frame++;
            for (Iterator<Particle> it = particles.iterator(); it.hasNext(); ) {
                Particle p = it.next();
                p.x += p.vx; p.y += p.vy; p.life -= 0.035f;
                if (p.life <= 0) it.remove();
            }
            repaint();
        });

        resetGame();
        animTimer.start();
        loadBestScore();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        requestFocusInWindow();
    }

    public void stop() {
        gameTimer.stop();
        animTimer.stop();
    }

    // ---------- game logic ----------

    private void resetGame() {
        snake.clear();
        snake.add(new Point(5, 12));
        snake.add(new Point(4, 12));
        snake.add(new Point(3, 12));
        dx = nextDx = 1;
        dy = nextDy = 0;
        score = 0;
        level = 1;
        particles.clear();
        scoresLoaded = false;
        gameTimer.stop();
        gameTimer.setDelay(delayForLevel());
        spawnApple();
        state = State.READY;
    }

    private int delayForLevel() {
        return Math.max(60, 150 - (level - 1) * 10);
    }

    private void spawnApple() {
        boolean onSnake;
        do {
            apple.setLocation(rnd.nextInt(COLS), rnd.nextInt(ROWS));
            onSnake = snake.contains(apple);
        } while (onSnake);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (state != State.RUNNING) return;

        dx = nextDx;
        dy = nextDy;
        Point head = snake.getFirst();
        int nx = head.x + dx, ny = head.y + dy;
        boolean eating = (nx == apple.x && ny == apple.y);

        // wall collision
        if (nx < 0 || ny < 0 || nx >= COLS || ny >= ROWS) { gameOver(); return; }

        // self collision (the tail cell frees up unless we are growing)
        int limit = eating ? snake.size() : snake.size() - 1;
        for (int i = 0; i < limit; i++) {
            if (snake.get(i).x == nx && snake.get(i).y == ny) { gameOver(); return; }
        }

        snake.addFirst(new Point(nx, ny));
        if (eating) {
            score += 10;
            burst(apple.x * CELL + CELL / 2f, apple.y * CELL + CELL / 2f);
            spawnApple();
            level = score / 50 + 1;
            gameTimer.setDelay(delayForLevel());
        } else {
            snake.removeLast();
        }
    }

    private void burst(float cx, float cy) {
        for (int i = 0; i < 14; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            float s = 1f + rnd.nextFloat() * 2.5f;
            particles.add(new Particle(cx, cy, (float) Math.cos(a) * s, (float) Math.sin(a) * s));
        }
    }

    // ---------- database (runs off the UI thread) ----------

    private void loadBestScore() {
        new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() throws Exception {
                return DatabaseManager.getBestScore();
            }
            @Override protected void done() {
                try { bestScore = Math.max(bestScore, get()); }
                catch (Exception ex) { dbOk = false; }
            }
        }.execute();
    }

    private void gameOver() {
        state = State.GAME_OVER;
        gameTimer.stop();
        bestScore = Math.max(bestScore, score);
        final int finalScore = score;

        new SwingWorker<List<DatabaseManager.ScoreEntry>, Void>() {
            @Override protected List<DatabaseManager.ScoreEntry> doInBackground() throws Exception {
                DatabaseManager.saveScore(playerName, finalScore);
                return DatabaseManager.getTopScores(5);
            }
            @Override protected void done() {
                try { topScores = get(); dbOk = true; }
                catch (Exception ex) { dbOk = false; }
                scoresLoaded = true;
            }
        }.execute();
    }

    // ---------- rendering ----------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setPaint(new GradientPaint(0, 0, new Color(15, 23, 42), 0, H, new Color(2, 6, 23)));
        g2.fillRect(0, 0, W, H);

        drawHud(g2);

        g2.translate(0, HUD);
        drawGrid(g2);
        drawApple(g2);
        drawSnake(g2);
        drawParticles(g2);
        g2.translate(0, -HUD);

        if (state == State.READY) {
            overlay(g2, 120);
            center(g2, "SNAKE", new Font("SansSerif", Font.BOLD, 48), 230, new Color(74, 222, 128));
            center(g2, "Press an arrow key / WASD to start", new Font("SansSerif", Font.PLAIN, 16), 280, Color.WHITE);
            center(g2, "P = pause    ESC = menu", new Font("SansSerif", Font.PLAIN, 13), 310, new Color(148, 163, 184));
        } else if (state == State.PAUSED) {
            overlay(g2, 150);
            center(g2, "PAUSED", new Font("SansSerif", Font.BOLD, 40), 290, Color.WHITE);
            center(g2, "Press P to resume", new Font("SansSerif", Font.PLAIN, 15), 325, new Color(148, 163, 184));
        } else if (state == State.GAME_OVER) {
            drawGameOver(g2);
        }
        g2.dispose();
    }

    private void drawHud(Graphics2D g2) {
        g2.setColor(new Color(30, 41, 59));
        g2.fillRect(0, 0, W, HUD - 6);
        g2.setColor(new Color(74, 222, 128));
        g2.fillRect(0, HUD - 6, W, 2);

        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2.setColor(new Color(226, 232, 240));
        g2.drawString(playerName, 14, 28);
        String mid = "Score  " + score + "    Lv " + level;
        g2.drawString(mid, (W - g2.getFontMetrics().stringWidth(mid)) / 2, 28);
        g2.setColor(new Color(250, 204, 21));
        String best = "Best  " + bestScore;
        g2.drawString(best, W - g2.getFontMetrics().stringWidth(best) - 14, 28);
    }

    private void drawGrid(Graphics2D g2) {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                g2.setColor((r + c) % 2 == 0 ? new Color(30, 41, 59, 140) : new Color(15, 23, 42, 140));
                g2.fillRect(c * CELL, r * CELL, CELL, CELL);
            }
        }
    }

    private void drawApple(Graphics2D g2) {
        float cx = apple.x * CELL + CELL / 2f, cy = apple.y * CELL + CELL / 2f;
        float pulse = 1f + 0.10f * (float) Math.sin(frame * 0.15);
        float r = 7f * pulse;

        // glow
        g2.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), 18,
                new float[]{0f, 1f}, new Color[]{new Color(239, 68, 68, 110), new Color(239, 68, 68, 0)}));
        g2.fill(new Ellipse2D.Float(cx - 18, cy - 18, 36, 36));

        // body
        g2.setPaint(new RadialGradientPaint(new Point2D.Float(cx - 2, cy - 2), r + 2,
                new float[]{0f, 1f}, new Color[]{new Color(252, 165, 165), new Color(220, 38, 38)}));
        g2.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));

        // leaf + stem
        g2.setColor(new Color(34, 197, 94));
        g2.fill(new Ellipse2D.Float(cx + 1, cy - r - 3, 6, 4));
        g2.setColor(new Color(120, 53, 15));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine((int) cx, (int) (cy - r + 1), (int) cx, (int) (cy - r - 3));
    }

    private void drawSnake(Graphics2D g2) {
        int n = snake.size();
        for (int i = n - 1; i >= 0; i--) {
            Point p = snake.get(i);
            float t = n == 1 ? 0 : (float) i / (n - 1);
            Color body = blend(new Color(74, 222, 128), new Color(21, 128, 61), t);
            int x = p.x * CELL, y = p.y * CELL;

            g2.setColor(new Color(0, 0, 0, 60)); // soft shadow
            g2.fill(new RoundRectangle2D.Float(x + 2, y + 3, CELL - 2, CELL - 2, 10, 10));

            g2.setColor(i == 0 ? new Color(134, 239, 172) : body);
            g2.fill(new RoundRectangle2D.Float(x + 1, y + 1, CELL - 2, CELL - 2, 10, 10));

            if (i == 0) drawEyes(g2, x + CELL / 2, y + CELL / 2);
        }
    }

    private void drawEyes(Graphics2D g2, int cx, int cy) {
        int fx = dx * 4, fy = dy * 4;      // forward offset
        int px = -dy * 4, py = dx * 4;     // sideways offset
        for (int s = -1; s <= 1; s += 2) {
            int ex = cx + fx + s * px, ey = cy + fy + s * py;
            g2.setColor(Color.WHITE);
            g2.fillOval(ex - 3, ey - 3, 6, 6);
            g2.setColor(Color.BLACK);
            g2.fillOval(ex - 1 + dx, ey - 1 + dy, 3, 3);
        }
    }

    private void drawParticles(Graphics2D g2) {
        for (Particle p : particles) {
            g2.setColor(new Color(250, 204, 21, Math.max(0, (int) (p.life * 255))));
            float s = 2f + 3f * p.life;
            g2.fill(new Ellipse2D.Float(p.x - s / 2, p.y - s / 2, s, s));
        }
    }

    private void drawGameOver(Graphics2D g2) {
        overlay(g2, 190);
        center(g2, "GAME OVER", new Font("SansSerif", Font.BOLD, 40), 160, new Color(248, 113, 113));
        center(g2, "Score: " + score, new Font("SansSerif", Font.BOLD, 20), 195, Color.WHITE);

        Font f = new Font("SansSerif", Font.PLAIN, 15);
        if (!scoresLoaded) {
            center(g2, "Saving score...", f, 250, new Color(148, 163, 184));
        } else if (!dbOk) {
            center(g2, "Database offline - score not saved", f, 250, new Color(251, 146, 60));
        } else {
            center(g2, "TOP 5", new Font("SansSerif", Font.BOLD, 16), 240, new Color(250, 204, 21));
            int y = 270;
            for (int i = 0; i < topScores.size(); i++) {
                DatabaseManager.ScoreEntry e = topScores.get(i);
                center(g2, (i + 1) + ".  " + e.name + "  -  " + e.score, f, y, Color.WHITE);
                y += 24;
            }
        }
        center(g2, "ENTER = play again     ESC = menu", new Font("SansSerif", Font.PLAIN, 14), H - 50,
                new Color(148, 163, 184));
    }

    private void overlay(Graphics2D g2, int alpha) {
        g2.setColor(new Color(2, 6, 23, alpha));
        g2.fillRect(0, HUD, W, H - HUD);
    }

    private void center(Graphics2D g2, String text, Font font, int y, Color color) {
        g2.setFont(font);
        g2.setColor(color);
        g2.drawString(text, (W - g2.getFontMetrics().stringWidth(text)) / 2, y);
    }

    private static Color blend(Color a, Color b, float t) {
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                         (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                         (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    // ---------- input ----------

    private class TAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            int k = e.getKeyCode();

            if (k == KeyEvent.VK_ESCAPE) { onExitToMenu.run(); return; }
            if (k == KeyEvent.VK_P && (state == State.RUNNING || state == State.PAUSED)) {
                if (state == State.RUNNING) { state = State.PAUSED; gameTimer.stop(); }
                else { state = State.RUNNING; gameTimer.start(); }
                return;
            }
            if (k == KeyEvent.VK_ENTER && state == State.GAME_OVER) { resetGame(); return; }

            int ndx = 0, ndy = 0;
            if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A) ndx = -1;
            else if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) ndx = 1;
            else if (k == KeyEvent.VK_UP || k == KeyEvent.VK_W) ndy = -1;
            else if (k == KeyEvent.VK_DOWN || k == KeyEvent.VK_S) ndy = 1;
            else return;

            // can't reverse into yourself (compared to the direction actually moved last tick)
            if (ndx == -dx && ndy == -dy) return;
            nextDx = ndx;
            nextDy = ndy;

            if (state == State.READY) { state = State.RUNNING; gameTimer.start(); }
        }
    }
}
