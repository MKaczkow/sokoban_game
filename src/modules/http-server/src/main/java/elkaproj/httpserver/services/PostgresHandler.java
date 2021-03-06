package elkaproj.httpserver.services;

import elkaproj.config.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Handles and manages a PostgreSQL connection.
 */
@Service(kind = ServiceKind.TRANSIENT)
public class PostgresHandler {

    private final PostgresConfigurationProvider configurationProvider;
    private Connection connection = null;

    private PostgresHandler(PostgresConfigurationProvider configurationProvider) {
        this.configurationProvider = configurationProvider;
    }

    /**
     * Opens the connection.
     *
     * @throws IOException  Exception occurred while loading configuration.
     * @throws SQLException Exception occurred while executing SQL statements.
     */
    public synchronized void open() throws IOException, SQLException {
        if (this.connection != null)
            return;

        String url = this.configurationProvider.getUrl();
        Properties props = this.configurationProvider.getProperties();
        this.connection = DriverManager.getConnection(url, props);
    }

    /**
     * Closes the connection.
     *
     * @throws SQLException Exception occurred while executing SQL statements.
     */
    public synchronized void close() throws SQLException {
        if (this.connection == null)
            return;

        this.connection.close();
        this.connection = null;
    }

    /**
     * Ensures the database is set up.
     *
     * @throws SQLException Exception occurred while executing SQL statements.
     */
    public synchronized void ensureDb() throws SQLException {
        try (Statement stmt = this.connection.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS highscores(" +
                    "player text not null," +
                    "level_pack text not null," +
                    "level int not null," +
                    "score int not null," +
                    "primary key (player, level_pack, level));");
        }
    }

    /**
     * Gets the total number of entries in the database.
     *
     * @return Total number of entries in the database.
     * @throws SQLException Exception occurred while executing SQL statements.
     */
    public int getTotalCount() throws SQLException {
        try (Statement stmt = this.connection.createStatement()) {
            try (ResultSet res = stmt.executeQuery("SELECT COUNT(*) FROM highscores;")) {
                if (res.next())
                    return res.getInt(1);
            }
        }

        return -1;
    }

    /**
     * Creates a new scoreboard entry.
     *
     * @param levelPack  ID of the level pack the entry is for.
     * @param level      Level the entry is for.
     * @param playerName Name of the player who made the entry.
     * @param score      Score achieved by the player.
     * @throws SQLException Exception occurred while executing SQL statements.
     */
    public void writeEntry(String levelPack, int level, String playerName, int score) throws SQLException {
        try (PreparedStatement stmt = this.connection.prepareStatement("INSERT INTO highscores VALUES(?, ?, ?, ?) ON CONFLICT (player, level_pack, level) DO UPDATE SET score = ?;")) {
            stmt.setString(1, playerName);
            stmt.setString(2, levelPack);
            stmt.setInt(3, level);
            stmt.setInt(4, score);
            stmt.setInt(5, score);

            stmt.execute();
        }
    }

    /**
     * Gets all scoreboard entries corresponding to given level pack.
     *
     * @param levelPack ID of the level pack to get entries for.
     * @return List of all entries matching criteria.
     * @throws SQLException Exception occurred while executing SQL statements.
     */
    public List<PostgresScoreboardEntry> getEntriesForPack(String levelPack) throws SQLException {
        try (PreparedStatement stmt = this.connection.prepareStatement("SELECT * FROM highscores WHERE level_pack = ?;")) {
            stmt.setString(1, levelPack);

            try (ResultSet res = stmt.executeQuery()) {
                return this.prepareEntries(res);
            }
        }
    }

    /**
     * Gets all scoreboard entries corresponding to given level pack and player.
     *
     * @param levelPack  ID of the level pack to get entries for.
     * @param playerName Name of the player to get entries for.
     * @return List of all entries matching criteria.
     * @throws SQLException Exception occurred while executing SQL statements.
     */
    public List<PostgresScoreboardEntry> getPlayerEntriesForPack(String levelPack, String playerName) throws SQLException {
        try (PreparedStatement stmt = this.connection.prepareStatement("SELECT * FROM highscores WHERE level_pack = ? AND player = ?;")) {
            stmt.setString(1, levelPack);
            stmt.setString(2, playerName);

            try (ResultSet res = stmt.executeQuery()) {
                return this.prepareEntries(res);
            }
        }
    }

    /**
     * Gets all scoreboard entries corresponding to level.
     *
     * @param levelPack ID of the level pack to get entries for.
     * @param level     Number of the level to get entries for.
     * @return List of all entries matching criteria.
     * @throws SQLException Exception occurred while executing SQL statements.
     */
    public List<PostgresScoreboardEntry> getEntriesForLevel(String levelPack, int level) throws SQLException {
        try (PreparedStatement stmt = this.connection.prepareStatement("SELECT * FROM highscores WHERE level_pack = ? AND level = ?;")) {
            stmt.setString(1, levelPack);
            stmt.setInt(2, level);

            try (ResultSet res = stmt.executeQuery()) {
                return this.prepareEntries(res);
            }
        }
    }

    /**
     * Gets a single entry corresponding to player on given level.
     *
     * @param playerName Name of the player to get the entry for.
     * @param levelPack  ID of the level pack to get entry for.
     * @param level      Number of the level to get entry for.
     * @return Entry matching criteria, or null.
     * @throws SQLException Exception occurred while executing SQL statements.
     */
    public PostgresScoreboardEntry getEntryFor(String playerName, String levelPack, int level) throws SQLException {
        try (PreparedStatement stmt = this.connection.prepareStatement("SELECT * FROM highscores WHERE player = ? AND level_pack = ? AND level = ? LIMIT 1;")) {
            stmt.setString(1, playerName);
            stmt.setString(2, levelPack);
            stmt.setInt(3, level);

            try (ResultSet res = stmt.executeQuery()) {
                if (res.next()) {
                    return new PostgresScoreboardEntry(
                            res.getString("player"),
                            res.getString("level_pack"),
                            res.getInt("level"),
                            res.getInt("score"));
                }
            }
        }

        return null;
    }

    private List<PostgresScoreboardEntry> prepareEntries(ResultSet res) throws SQLException {
        ArrayList<PostgresScoreboardEntry> entries = new ArrayList<>();
        while (res.next()) {
            entries.add(new PostgresScoreboardEntry(
                    res.getString("player"),
                    res.getString("level_pack"),
                    res.getInt("level"),
                    res.getInt("score")
            ));
        }
        return entries;
    }

    /**
     * Converts a database view of a scoreboard to a serializable scoreboard.
     *
     * @param packId  ID of the pack the scoreboard is for.
     * @param entries Entries to put on the scoreboard.
     * @return Constructed scoreboard.
     */
    public static IScoreboard createScoreboard(String packId, List<PostgresScoreboardEntry> entries) {
        return new PostgresXmlScoreboard(packId, entries);
    }

    /**
     * Converts a database view of a scoreboard to a serializable scoreboard.
     *
     * @param packId ID of the pack the scoreboard is for.
     * @param entry  Entry to put on the scoreboard.
     * @return Constructed scoreboard.
     */
    public static IScoreboard createScoreboard(String packId, PostgresScoreboardEntry entry) {
        return new PostgresXmlScoreboard(packId, entry);
    }

    /**
     * Represents a scoreboard entry.
     */
    public static class PostgresScoreboardEntry {

        private final String playerName, levelPackId;
        private final int level, score;

        private PostgresScoreboardEntry(String playerName, String levelPackId, int level, int score) {
            this.playerName = playerName;
            this.levelPackId = levelPackId;
            this.level = level;
            this.score = score;
        }

        /**
         * Gets the name of the player who made this entry.
         *
         * @return Name of the player who made this entry.
         */
        public String getPlayerName() {
            return playerName;
        }

        /**
         * Gets the ID of the level pack to which this entry corresponds.
         *
         * @return ID of the level pack to which this entry corresponds.
         */
        public String getLevelPackId() {
            return levelPackId;
        }

        /**
         * Gets the number of the level to which this entry corresponds.
         *
         * @return Number of the level to which this entry corresponds.
         */
        public int getLevel() {
            return level;
        }

        /**
         * Gets the score for this entry.
         *
         * @return Score for this entry.
         */
        public int getScore() {
            return score;
        }
    }

    @XmlRootElement(name = "entry")
    @XmlAccessorType(XmlAccessType.FIELD)
    private static class PostgresXmlScoreboardEntry implements IScoreboardEntry {

        @XmlElement(name = "player")
        public String playerName;

        @XmlElement(name = "level")
        public int levelNumber;

        @XmlElement(name = "score")
        public int score;

        public PostgresXmlScoreboardEntry() {

        }

        public PostgresXmlScoreboardEntry(PostgresScoreboardEntry entry) {
            this.playerName = entry.playerName;
            this.levelNumber = entry.level;
            this.score = entry.score;
        }

        @Override
        public String getPlayerName() {
            return this.playerName;
        }

        @Override
        public ILevel getLevel() {
            return null;
        }

        @Override
        public int getScore() {
            return this.score;
        }
    }

    @XmlRootElement(name = "scoreboard")
    @XmlAccessorType(XmlAccessType.FIELD)
    private static class PostgresXmlScoreboard implements IScoreboard {

        @XmlElement(name = "level-pack")
        public String levelPackId;

        @XmlElement(name = "entry")
        public PostgresXmlScoreboardEntry[] entries;

        public PostgresXmlScoreboard() {

        }

        public PostgresXmlScoreboard(String levelPackId, List<PostgresScoreboardEntry> entries) {
            this.levelPackId = levelPackId;
            this.entries = entries.stream()
                    .map(PostgresXmlScoreboardEntry::new)
                    .toArray(PostgresXmlScoreboardEntry[]::new);
        }

        public PostgresXmlScoreboard(String levelPackId, PostgresScoreboardEntry entry) {
            this.levelPackId = levelPackId;
            this.entries = entry != null ? new PostgresXmlScoreboardEntry[]{new PostgresXmlScoreboardEntry(entry)} : new PostgresXmlScoreboardEntry[0];
        }

        @Override
        public ILevelPack getLevelPack() {
            return null;
        }

        @Override
        public List<IScoreboardTotalEntry> getAllTotalEntries() {
            return null;
        }

        @Override
        public List<IScoreboardEntry> getLevelEntries(ILevel level) {
            return null;
        }

        @Override
        public void serialize(OutputStream os) throws IOException, JAXBException {
            JAXBContext jaxbctx = JAXBContext.newInstance(this.getClass());
            Marshaller jaxb = jaxbctx.createMarshaller();
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                jaxb.marshal(this, baos);
                os.write(baos.toByteArray());
            }
        }
    }
}
