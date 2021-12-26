package rocks.gravili.notquests.spigot.managers;

import rocks.gravili.notquests.spigot.NotQuests;

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

    public void backupQuests() {
        if (!prepareBackupFolder()) {
            return;
        }
        File newQuestsBackupFile = new File(main.getMain().getDataFolder().getPath() + "/backups/" + "quests-backup-" + backupFileDateFormat.format(new Date(System.currentTimeMillis())) + ".yml");

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
                main.getDataManager().getQuestsConfig().save(newQuestsBackupFile);
                main.getLogManager().info("Your quests.yml has been successfully backed up to <AQUA>" + newQuestsBackupFile.getPath());
            } catch (Exception e) {
                main.getLogManager().warn("There was an error saving the backup file for your quests.yml. Error:");
                e.printStackTrace();
            }

        }
    }
}
