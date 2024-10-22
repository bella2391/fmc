package velocity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class MineStatus {
    private final Logger logger;
    private final DatabaseInterface db;
    private final Provider<SocketSwitch> sswProvider;
    @Inject
    public MineStatus(Logger logger, DatabaseInterface db, Provider<SocketSwitch> sswProvider) {
        this.logger = logger;
        this.db = db;
        this.sswProvider = sswProvider;
    }

    public void UpdateJoinPlayers(String playerName, String currentServerName) {
        UpdatePlayers(playerName, null, currentServerName);
    }

    public void UpdateQuitPlayers(String playerName, String beforeServerName) {
        UpdatePlayers(playerName, beforeServerName, null);
    }

    public void UpdateMovePlayers(String playerName, String beforeServerName, String currentServerName) {
        UpdatePlayers(playerName, beforeServerName, currentServerName);
    }

    private void UpdatePlayers(String playerName, String beforeServerName, String currentServerName) {
        String query = "SELECT * FROM status;";
        try (Connection conn = db.getConnection();
            PreparedStatement ps = conn.prepareStatement(query)) {
            try (ResultSet rs = ps.executeQuery()) {
                List<Integer> rsAffecteds2 = new ArrayList<>();
                while (rs.next()) {
                    String serverName = rs.getString("name");
                    String players = rs.getString("player_name");
                    String[] playerArray = players.split(",\\s*");
                    List<String> playersList = Arrays.asList(playerArray);
                    if (serverName != null) {
                        if (beforeServerName != null) {
                            if (serverName.equals(beforeServerName)) {
                                playersList.remove(playerName);
                                playersList.sort(String.CASE_INSENSITIVE_ORDER);
                                String updatePlayers = String.join(", ", playersList);
                                String query2 = "UPDATE status SET player_list=? WHERE name=?;";
                                try (PreparedStatement ps2 = conn.prepareStatement(query2)) {
                                    ps2.setString(1, updatePlayers);
                                    ps2.setString(2, beforeServerName);
                                    int rsAffected2 = ps2.executeUpdate();
                                    rsAffecteds2.add(rsAffected2);
                                    if (rsAffected2 > 0) {
                                        logger.info("Player {} has been removed from server {}", playerName, beforeServerName);
                                    }
                                }
                            }
                        } else if (currentServerName != null) {
                            if (serverName.equals(currentServerName)) {
                                playersList.add(playerName);
                                playersList.sort(String.CASE_INSENSITIVE_ORDER);
                                String updatePlayers = String.join(", ", playersList);
                                String query2 = "UPDATE status SET player_list=? WHERE name=?;";
                                try (PreparedStatement ps2 = conn.prepareStatement(query2)) {
                                    ps2.setString(1, updatePlayers);
                                    ps2.setString(2, currentServerName);
                                    int rsAffected2 = ps2.executeUpdate();
                                    rsAffecteds2.add(rsAffected2);
                                    if (rsAffected2 > 0) {
                                        logger.info("Player {} has been added to server {}", playerName, currentServerName);
                                    }
                                }
                            }
                        }
                    }
                }
                if (rsAffecteds2.stream().anyMatch(rs2 -> rs2 > 0)) {
                    SocketSwitch ssw = sswProvider.get();
                    ssw.sendSpigotServer("MineStatusSync");
                }
            }
        } catch (SQLException | ClassNotFoundException e) {
            logger.error("Error while updating player status", e.getMessage());
            for (StackTraceElement ste : e.getStackTrace()) {
                logger.error(ste.toString());
            }
        }
    }
}
