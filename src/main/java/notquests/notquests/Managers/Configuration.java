package notquests.notquests.Managers;

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
}
