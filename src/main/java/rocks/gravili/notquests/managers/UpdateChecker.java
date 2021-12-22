/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.managers;

import rocks.gravili.notquests.NotQuests;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class UpdateChecker {

    private final NotQuests main;
    private final int projectId;
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
