package handling.login;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import client.player.PlayerJob;
import database.DatabaseConnection;

/**
 * @author Matze
 * @author Quit
 * @author Ronan
 */

public class RankingWorker implements Runnable {

    private Connection con;
    private long lastUpdate = System.currentTimeMillis();

    @Override
    public void run() {
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);
            
            updateRanking(null);
            for (int i = 0; i < 3; i += 2) {
                for (int j = 1; j < 6; j++) {
                    updateRanking(PlayerJob.getById(i * 500 + 100 * j));
                }
            }
            con.commit();
            con.setAutoCommit(true);
            lastUpdate = System.currentTimeMillis();
        } catch (SQLException ex) {
            try {
                con.rollback();
                con.setAutoCommit(true);
            } catch (SQLException ex2) {
            }
        }
    } 

    private void updateRanking(PlayerJob job) throws SQLException {
        String sqlCharSelect = "SELECT c.id, " + (job != null ? "c.jobRank, c.jobRankMove" : "c.rank, c.rankMove") + ", a.lastlogin AS lastlogin, a.loggedin FROM characters AS c LEFT JOIN accounts AS a ON c.accountid = a.id WHERE c.gm = 0 ";
        if (job != null) {
            sqlCharSelect += "AND c.job DIV 100 = ? ";
        }
        sqlCharSelect += "ORDER BY c.level DESC , c.exp DESC , c.fame DESC , c.meso DESC";
        PreparedStatement ps;
        try (PreparedStatement charSelect = con.prepareStatement(sqlCharSelect)) {
            if (job != null) {
                charSelect.setInt(1, job.getId() / 100);
            } try (ResultSet rs = charSelect.executeQuery()) {
                ps = con.prepareStatement("UPDATE characters SET " + (job != null ? "jobRank = ?, jobRankMove = ? " : "rank = ?, rankMove = ? ") + "WHERE id = ?");
                int rank = 0;
                while (rs.next()) {
                    int rankMove = 0;
                    rank++;
                    if (rs.getLong("lastlogin") < lastUpdate || rs.getInt("loggedin") > 0) {
                        rankMove = rs.getInt((job != null ? "jobRankMove" : "rankMove"));
                    }
                    rankMove += rs.getInt((job != null ? "jobRank" : "rank")) - rank;
                    ps.setInt(1, rank);
                    ps.setInt(2, rankMove);
                    ps.setInt(3, rs.getInt("id"));
                    ps.executeUpdate();
                }
            }
        }
        ps.close();
    }
}
