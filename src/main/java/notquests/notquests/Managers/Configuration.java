package notquests.notquests.Managers;

import org.bukkit.Particle;

/**
 * This is the Configuration Class which contains the settings which can be configured in the General.conf
 *
 * @author Alessio Gravili
 */
public class Configuration {

    /**
     * MYSQL Database Connection Information
     */
    private String host, database, username, password;
    /**
     * MYSQL Database Connection Information
     */
    private int port;

    private boolean questPreviewUseGUI = true;
    private boolean userCommandsUseGUI = true;
    private boolean mySQLEnabled = false;

    public String placeholder_player_active_quests_list_horizontal_separator = " | ";

    public int placeholder_player_active_quests_list_horizontal_limit = -1;
    public int placeholder_player_active_quests_list_vertical_limit = -1;

    public boolean placeholder_player_active_quests_list_horizontal_use_displayname_if_available = true;
    public boolean placeholder_player_active_quests_list_vertical_use_displayname_if_available = true;

    private int maxActiveQuestsPerPlayer = -1;

    private int questGiverIndicatorParticleSpawnInterval = 10;
    private int questGiverIndicatorParticleCount = 1;

    private Particle questGiverIndicatorParticleType = Particle.VILLAGER_ANGRY;

    private boolean questGiverIndicatorParticleEnabled = true;


    public Configuration(){

    }

    public final String getDatabaseHost() {
        return host;
    }

    public void setDatabaseHost(final String host) {
        this.host = host;
    }

    public final int getDatabasePort() {
        return port;
    }

    public void setDatabasePort(final int port) {
        this.port = port;
    }

    public final String getDatabaseName() {
        return database;
    }

    public void setDatabaseName(final String database) {
        this.database = database;
    }

    public final String getDatabaseUsername() {
        return username;
    }

    public void setDatabaseUsername(final String username) {
        this.username = username;
    }

    public final String getDatabasePassword() {
        return password;
    }

    public void setDatabasePassword(final String password) {
        this.password = password;
    }

    public final boolean isQuestPreviewUseGUI() {
        return questPreviewUseGUI;
    }

    public void setQuestPreviewUseGUI(final boolean questPreviewUseGUI) {
        this.questPreviewUseGUI = questPreviewUseGUI;
    }

    public final boolean isMySQLEnabled(){
        return mySQLEnabled;
    }

    public void setMySQLEnabled(final boolean mySQLEnabled){
        this.mySQLEnabled = mySQLEnabled;
    }


    public final boolean isUserCommandsUseGUI() {
        return userCommandsUseGUI;
    }

    public void setUserCommandsUseGUI(final boolean userCommandsUseGUI) {
        this.userCommandsUseGUI = userCommandsUseGUI;
    }

    public final int getMaxActiveQuestsPerPlayer() {
        return maxActiveQuestsPerPlayer;
    }

    public void setMaxActiveQuestsPerPlayer(int maxActiveQuestsPerPlayer) {
        this.maxActiveQuestsPerPlayer = maxActiveQuestsPerPlayer;
    }

    public final int getQuestGiverIndicatorParticleSpawnInterval() {
        return questGiverIndicatorParticleSpawnInterval;
    }

    public void setQuestGiverIndicatorParticleSpawnInterval(final int questGiverIndicatorParticleSpawnInterval) {
        this.questGiverIndicatorParticleSpawnInterval = questGiverIndicatorParticleSpawnInterval;
    }

    public final Particle getQuestGiverIndicatorParticleType() {
        return questGiverIndicatorParticleType;
    }

    public void setQuestGiverIndicatorParticleType(final Particle questGiverIndicatorParticleType) {
        this.questGiverIndicatorParticleType = questGiverIndicatorParticleType;
    }

    public final int getQuestGiverIndicatorParticleCount() {
        return questGiverIndicatorParticleCount;
    }

    public void setQuestGiverIndicatorParticleCount(final int questGiverIndicatorParticleCount) {
        this.questGiverIndicatorParticleCount = questGiverIndicatorParticleCount;
    }

    public final boolean isQuestGiverIndicatorParticleEnabled() {
        return questGiverIndicatorParticleEnabled;
    }

    public final void setQuestGiverIndicatorParticleEnabled(boolean questGiverIndicatorParticleEnabled) {
        this.questGiverIndicatorParticleEnabled = questGiverIndicatorParticleEnabled;
    }
}
