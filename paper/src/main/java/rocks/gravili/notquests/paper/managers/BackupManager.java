/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
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

package rocks.gravili.notquests.paper.managers;

import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BackupManager {
    private final NotQuests main;
    private final SimpleDateFormat backupFileDateFormat;
    private File backupFolder = null;

    public BackupManager(final NotQuests main) {
        this.main = main;
        backupFileDateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    }


    public final boolean prepareBackupFolder() {
        //Create the Data Folder if it does not exist yet (the NotQuests folder)
        main.getDataManager().prepareDataFolder();
        if (backupFolder == null) {
            backupFolder = new File(main.getMain().getDataFolder().getPath() + "/backups/");
        }

        if (!backupFolder.exists()) {
            main.getLogManager().info("Backup Folder not found. Creating a new one...");

            if (!backupFolder.mkdirs()) {
                main.getDataManager().disablePluginAndSaving("There was an error creating the NotQuests backup folder.");
                return false;
            }
        }
        return true;
    }

    public void backupQuests(final Category category) {
        if (!prepareBackupFolder() || category.getQuestsConfig() == null) {
            return;
        }
        File newQuestsBackupFile = new File(main.getMain().getDataFolder().getPath() + "/backups/" + "quests-backup-" + category.getCategoryName() + "-" + backupFileDateFormat.format(new Date(System.currentTimeMillis())) + ".yml");

        if (!newQuestsBackupFile.exists()) {
            try {
                if (!newQuestsBackupFile.createNewFile()) {
                    main.getLogManager().warn("There was an error creating the backup file for your quests.yml.");
                    return;
                }

            } catch (Exception e) {
                main.getLogManager().warn("There was an error creating the backup file for your quests.yml. Error:");
                e.printStackTrace();
                return;
            }


            //Now save
            try {
                category.getQuestsConfig().save(newQuestsBackupFile);
                main.getLogManager().info("Your quests.yml of category <highlight>" + category.getCategoryName() + "</highlight> has been successfully backed up to <highlight2>" + newQuestsBackupFile.getPath());
            } catch (Exception e) {
                main.getLogManager().warn("There was an error saving the backup file for your quests.yml. Error:");
                e.printStackTrace();
            }

        }
    }
}
