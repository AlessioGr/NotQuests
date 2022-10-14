package rocks.gravili.notquests.paper.structs.objectives;

import org.bukkit.configuration.file.FileConfiguration;

public interface ObjectiveHolder {

  FileConfiguration getConfig();

  String getInitialConfigPath();

  String getName();

  void saveConfig();
}
