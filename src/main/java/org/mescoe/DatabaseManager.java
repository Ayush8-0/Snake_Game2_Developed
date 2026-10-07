package org.mescoe;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private static final String URL =
            "jdbc:mysql://localhost:3306/snake_game?serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASSWORD = "admin"; 
    
    private static boolean tableReady = false;

    public static class ScoreEntry {
        public final String name;
        public final int score;
        public final Timestamp playedAt;

        public ScoreEntry(String name, int score, Timestamp playedAt) {
            this.name = name;
            this.score = score;
            this.playedAt = playedAt;
        }
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    private static synchronized void ensureTable() throws SQLException {
        if (tableReady) return;
        String sql = "CREATE TABLE IF NOT EXISTS scores ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "player_name VARCHAR(30) NOT NULL, "
                + "score INT NOT NULL, "
                + "played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        try (Connection c = connect(); Statement st = c.createStatement()) {
            st.executeUpdate(sql);
        }
        tableReady = true;
    }

    public static void saveScore(String player, int score) throws SQLException {
        ensureTable();
        String sql = "INSERT INTO scores (player_name, score) VALUES (?, ?)";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, player);
            ps.setInt(2, score);
            ps.executeUpdate();
        }
    }

    public static List<ScoreEntry> getTopScores(int limit) throws SQLException {
        ensureTable();
        String sql = "SELECT player_name, score, played_at FROM scores ORDER BY score DESC, played_at ASC LIMIT ?";
        List<ScoreEntry> list = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ScoreEntry(rs.getString(1), rs.getInt(2), rs.getTimestamp(3)));
                }
            }
        }
        return list;
    }

    public static int getBestScore() throws SQLException {
        ensureTable();
        try (Connection c = connect();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COALESCE(MAX(score), 0) FROM scores")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
} 
