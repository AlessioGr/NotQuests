package notquests.notquests.Managers;

import notquests.notquests.NotQuests;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class UpdateChecker {

    private NotQuests main;
    private int projectId;
    private URL checkURL;
    private String newVersion;

    public UpdateChecker(NotQuests main, int projectId) {
        this.main = main;
        this.newVersion = main.getDescription().getVersion();
        this.projectId = projectId;
        try {
            this.checkURL = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + projectId);
        } catch (MalformedURLException ignored) {
        }
    }

    public int getProjectId() {
        return projectId;
    }

    public String getLatestVersion() {
        return newVersion;
    }

    public String getResourceURL() {
        return "https://www.spigotmc.org/resources/" + projectId;
    }

    public boolean checkForUpdates() {
        URLConnection con = null;

        try {
            con = checkURL.openConnection();
            this.newVersion = new BufferedReader(new InputStreamReader(con.getInputStream())).readLine();
        } catch (Exception ignored) {
        }

        if (con == null || newVersion == null) return false;
        return !main.getDescription().getVersion().equalsIgnoreCase(newVersion);
    }

}
