package rocks.gravili.notquests.paper.structs.actions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.arguments.standard.FloatArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class PlaySoundAction extends Action {

  private String soundName = "";

  private String soundCategory = "master";

  private boolean stopOtherSounds = false;

  private boolean playForEveryoneAtSetLocation = false;

  private boolean playForEveryoneAtTheirLocation = false;

  private String worldName = null;
  private double locationX = -1;
  private double locationY = -1;
  private double locationZ = -1;
  private float pitch = -1;
  private float volume = -1;


  public PlaySoundAction(final NotQuests main) {
    super(main);
  }

  public static void handleCommands(
      NotQuests main,
      PaperCommandManager<CommandSender> manager,
      Command.Builder<CommandSender> builder,
      ActionFor actionFor) {

    final CommandFlag<Float> volumeFlag =
        CommandFlag.newBuilder("volume")
            .withArgument(FloatArgument.newBuilder("volume").withMin(0).build())
            .withDescription(ArgumentDescription.of("Sound volume (between 0 and 1)"))
            .build();
    final CommandFlag<Float> pitchFlag =
        CommandFlag.newBuilder("pitch")
            .withArgument(FloatArgument.newBuilder("pitch").withMin(0).build())
            .withDescription(ArgumentDescription.of("Sound pitch"))
            .build();

    final CommandFlag<String> soundCategoryFlag =
        CommandFlag.newBuilder("SoundCategory")
            .withArgument(StringArgument.<CommandSender>newBuilder("SoundCategory").withSuggestionsProvider(
                (context, lastString) -> {
                  final List<String> allArgs = context.getRawInput();
                  main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Sound category]", "");

                  final ArrayList<String> completions = new ArrayList<>();

                  for (final SoundCategory soundCategory : SoundCategory.values()) {
                    completions.add("" + soundCategory.name().toLowerCase());
                  }
                  return completions;
                }
            ).single().build())
            .withDescription(ArgumentDescription.of("Sound category. Default: master"))
            .build();
    manager.command(
        builder
            .argument(StringArgument.<CommandSender>newBuilder("Sound").withSuggestionsProvider(
                (context, lastString) -> {
                  final List<String> allArgs = context.getRawInput();
                  main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Sound name]", "");

                  final ArrayList<String> completions = new ArrayList<>();

                  for (final Sound sound : Sound.values()) {
                    completions.add("" + sound.getKey().asString());
                  }
                  return completions;
                }
            ).single().build(), ArgumentDescription.of("Name of the sound which should be played"))
            .flag(
                manager
                    .flagBuilder("stopOtherSounds")
                    .withDescription(
                        ArgumentDescription.of(
                            "Stops all other, currently playing sounds")))
            .flag(
                manager
                    .flagBuilder("playForEveryoneAtSetLocation")
                    .withDescription(
                        ArgumentDescription.of(
                            "All online players will hear the sound if they are close to set location")))
            .flag(
                manager
                    .flagBuilder("playForEveryoneAtTheirLocation")
                    .withDescription(
                        ArgumentDescription.of(
                            "Plays the sound for all online players at their own location")))
            .flag(main.getCommandManager().world)
            .flag(main.getCommandManager().locationX)
            .flag(main.getCommandManager().locationY)
            .flag(main.getCommandManager().locationZ)
            .flag(volumeFlag)
            .flag(pitchFlag)
            .flag(soundCategoryFlag)
            .handler(
                (context) -> {
                  final String soundName = context.get("Sound");

                  final boolean stopOtherSounds = context.flags().isPresent("stopOtherSounds");
                  final boolean playForEveryoneAtSetLocationFlagResult = context.flags().isPresent("playForEveryoneAtSetLocation");
                  final boolean playForEveryoneAtTheirLocationFlagResult = context.flags().isPresent("playForEveryoneAtTheirLocation");

                  final World world =
                      context
                          .flags()
                          .getValue(main.getCommandManager().world, null);

                  final double locationX =
                      context
                          .flags()
                          .getValue(main.getCommandManager().locationX, -1d);
                  final double locationY =
                      context
                          .flags()
                          .getValue(main.getCommandManager().locationX, -1d);
                  final double locationZ =
                      context
                          .flags()
                          .getValue(main.getCommandManager().locationX, -1d);

                  final float volume =
                      context
                          .flags()
                          .getValue(volumeFlag, -1f);
                  final float pitch =
                      context
                          .flags()
                          .getValue(pitchFlag, -1f);

                  final String soundCategoryValue =
                      context
                          .flags()
                          .getValue(soundCategoryFlag, "master");

                  final PlaySoundAction playSoundAction =
                      new PlaySoundAction(main);
                  playSoundAction.setSoundName(soundName);
                  playSoundAction.setStopOtherSounds(stopOtherSounds);
                  playSoundAction.setPlayForEveryoneAtTheirLocation(playForEveryoneAtTheirLocationFlagResult);
                  playSoundAction.setPlayForEveryoneAtSetLocation(playForEveryoneAtSetLocationFlagResult);
                  playSoundAction.setSoundCategory(soundCategoryValue);

                  if(world != null && locationX >= -0.02 && locationY >= -0.02 && locationZ >= -0.02){
                    playSoundAction.setWorldName(world.getName());
                    playSoundAction.setLocationX(locationX);
                    playSoundAction.setLocationY(locationY);
                    playSoundAction.setLocationZ(locationZ);
                  }
                  if(volume >= -0.02){
                    playSoundAction.setVolume(volume);
                  }
                  if(pitch >= -0.02){
                    playSoundAction.setPitch(pitch);
                  }


                  main.getActionManager().addAction(playSoundAction, context, actionFor);
                }));
  }

  public final String getSoundName() {
    return soundName;
  }

  public void setSoundName(final String soundName) {
    this.soundName = soundName;
  }

  public final boolean isStopOtherSounds() {
    return stopOtherSounds;
  }

  public void setStopOtherSounds(final boolean stopOtherSounds) {
    this.stopOtherSounds = stopOtherSounds;
  }

  @Override
  public void executeInternally(final QuestPlayer questPlayer, Object... objects) {
    final Player player = questPlayer.getPlayer();
    if(isStopOtherSounds()){
      player.stopAllSounds();
    }
    Location location = player.getLocation();
    if(worldName != null && locationX >= -0.02 && locationY >= -0.02 && locationZ >= -0.02){
      final World world = Bukkit.getWorld(worldName);
      if(world != null){
        location = new Location(world, locationX, locationY, locationZ);
      }
    }
    final SoundCategory soundCategoryObject = SoundCategory.valueOf(soundCategory.toUpperCase(Locale.ROOT));
    if(isPlayForEveryoneAtTheirLocation()){
      for (final Player onlinePlayer : Bukkit.getOnlinePlayers()) {
        onlinePlayer.playSound(onlinePlayer.getLocation(), soundName, soundCategoryObject,
            volume >= -0.02 ? volume : 1, pitch >= -0.02 ? pitch : 1);
      }
    } else if(isPlayForEveryoneAtSetLocation()) {
      location.getWorld().playSound(location, soundName, soundCategoryObject, volume >= -0.02 ? volume : 1, pitch >= -0.02 ? pitch : 1);
    } else {
      player.playSound(location, soundName, soundCategoryObject, volume >= -0.02 ? volume : 1, pitch >= -0.02 ? pitch : 1);
    }
  }

  @Override
  public void save(FileConfiguration configuration, String initialPath) {
    configuration.set(initialPath + ".specifics.soundName", getSoundName());
    configuration.set(initialPath + ".specifics.stopOtherSounds", isStopOtherSounds());
    if(getWorldName() != null){
      configuration.set(initialPath + ".specifics.worldName", getWorldName());
    }
    if(getLocationX() >= -0.02){
      configuration.set(initialPath + ".specifics.locationX", getLocationX());
    }
    if(getLocationY() >= -0.02){
      configuration.set(initialPath + ".specifics.locationY", getLocationY());
    }
    if(getLocationZ() >= -0.02){
      configuration.set(initialPath + ".specifics.locationZ", getLocationZ());
    }
    if(getVolume() >= -0.02){
      configuration.set(initialPath + ".specifics.volume", (double)getVolume());
    }
    if(getPitch() >= -0.02){
      configuration.set(initialPath + ".specifics.pitch", (double)getPitch());
    }

    configuration.set(initialPath + ".specifics.playForEveryoneAtTheirLocation", isPlayForEveryoneAtTheirLocation());
    configuration.set(initialPath + ".specifics.playForEveryoneAtSetLocation", isPlayForEveryoneAtSetLocation());
    configuration.set(initialPath + ".specifics.soundCategory", getSoundCategory());
  }

  @Override
  public void load(final FileConfiguration configuration, String initialPath) {
    this.soundName = configuration.getString(initialPath + ".specifics.soundName");
    this.stopOtherSounds = configuration.getBoolean(initialPath + ".specifics.stopOtherSounds");
    this.worldName = configuration.getString(initialPath + ".specifics.worldName", null);
    this.locationX = configuration.getDouble(initialPath + ".specifics.locationX", -1d);
    this.locationY = configuration.getDouble(initialPath + ".specifics.locationY", -1d);
    this.locationZ = configuration.getDouble(initialPath + ".specifics.locationZ", -1d);
    this.volume = (float)configuration.getDouble(initialPath + ".specifics.volume", -1d);
    this.pitch = (float)configuration.getDouble(initialPath + ".specifics.pitch", -1d);

    this.playForEveryoneAtTheirLocation = configuration.getBoolean(initialPath + ".specifics.playForEveryoneAtTheirLocation", false);
    this.playForEveryoneAtSetLocation = configuration.getBoolean(initialPath + ".specifics.playForEveryoneAtSetLocation", false);
    this.soundCategory = configuration.getString(initialPath + ".specifics.soundCategory", "master");

  }

  @Override
  public void deserializeFromSingleLineString(ArrayList<String> arguments) {
    this.soundName = arguments.get(0);
    if (arguments.size() >= 2) {
      this.stopOtherSounds =
          String.join(" ", arguments).toLowerCase(Locale.ROOT).contains("--stopothersounds");
      this.playForEveryoneAtSetLocation =
          String.join(" ", arguments).toLowerCase(Locale.ROOT).contains("--playforeveryoneatsetlocation");
      this.playForEveryoneAtTheirLocation =
          String.join(" ", arguments).toLowerCase(Locale.ROOT).contains("--playforeveryoneattheirlocation");
    } else {
      this.stopOtherSounds = false;
    }
  }

  @Override
  public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
    return "Plays Sound: " + getSoundName();
  }

  public String getWorldName() {
    return worldName;
  }

  public void setWorldName(String worldName) {
    this.worldName = worldName;
  }

  public double getLocationX() {
    return locationX;
  }

  public void setLocationX(double locationX) {
    this.locationX = locationX;
  }

  public double getLocationY() {
    return locationY;
  }

  public void setLocationY(double locationY) {
    this.locationY = locationY;
  }

  public double getLocationZ() {
    return locationZ;
  }

  public void setLocationZ(double locationZ) {
    this.locationZ = locationZ;
  }

  public float getPitch() {
    return pitch;
  }

  public void setPitch(float pitch) {
    this.pitch = pitch;
  }

  public float getVolume() {
    return volume;
  }

  public void setVolume(float volume) {
    this.volume = volume;
  }


  public boolean isPlayForEveryoneAtSetLocation() {
    return playForEveryoneAtSetLocation;
  }

  public void setPlayForEveryoneAtSetLocation(boolean playForEveryoneAtSetLocation) {
    this.playForEveryoneAtSetLocation = playForEveryoneAtSetLocation;
  }


  public boolean isPlayForEveryoneAtTheirLocation() {
    return playForEveryoneAtTheirLocation;
  }

  public void setPlayForEveryoneAtTheirLocation(boolean playForEveryoneAtTheirLocation) {
    this.playForEveryoneAtTheirLocation = playForEveryoneAtTheirLocation;
  }

  public String getSoundCategory() {
    return soundCategory;
  }

  public void setSoundCategory(String soundCategory) {
    this.soundCategory = soundCategory;
  }
}

