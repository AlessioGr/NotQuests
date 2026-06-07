package rocks.gravili.notquests.paper.structs.actions;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.component.TypedCommandComponent;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.suggestion.Suggestion;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static org.incendo.cloud.parser.standard.FloatParser.floatParser;
import static org.incendo.cloud.parser.standard.StringParser.stringParser;

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

        final CommandFlag<Float> volumeFlag = CommandFlag.builder("volume")
                .withComponent(TypedCommandComponent.builder("volume", floatParser(0)))
                .withDescription(Description.of("Sound volume (between 0 and 1)"))
                .build();

        final CommandFlag<Float> pitchFlag = CommandFlag.builder("pitch")
                .withComponent(TypedCommandComponent.builder("pitch", floatParser(0)))
                .withDescription(Description.of("Sound pitch"))
                .build();

        final CommandFlag<String> soundCategoryFlag = CommandFlag.builder("SoundCategory")
                .withComponent(TypedCommandComponent.builder("SoundCategory", stringParser()).suggestionProvider((context, lastString) -> {
                    main.getUtilManager().sendFancyCommandCompletion((CommandSender) context.sender(), lastString.input().split(" "), "[Sound category]", "");
                    final ArrayList<Suggestion> completions = new ArrayList<>();
                    for (final SoundCategory soundCategory : SoundCategory.values()) {
                        completions.add(Suggestion.suggestion(soundCategory.name().toLowerCase()));
                    }
                    return CompletableFuture.completedFuture(completions);
                }))
                .withDescription(Description.of("Sound category. Default: master"))
                .build();

        manager.command(builder.required("Sound", stringParser(), Description.of("Name of the sound which should be played"), (context, lastString) -> {
                            main.getUtilManager().sendFancyCommandCompletion(context.sender(), lastString.input().split(" "), "[Sound name]", "");
                            final ArrayList<Suggestion> completions = new ArrayList<>();
                            final var soundRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.SOUND_EVENT);
                            for (final Sound sound : soundRegistry) {
                                completions.add(Suggestion.suggestion(soundRegistry.getKeyOrThrow(sound).asString()));
                            }
                            return CompletableFuture.completedFuture(completions);
                        }
                )
                .flag(manager.flagBuilder("stopOtherSounds").withDescription(Description.of("Stops all other, currently playing sounds")))
                .flag(manager.flagBuilder("playForEveryoneAtSetLocation").withDescription(Description.of("All online players will hear the sound if they are close to set location")))
                .flag(manager.flagBuilder("playForEveryoneAtTheirLocation").withDescription(Description.of("Plays the sound for all online players at their own location")))
                .flag(main.getCommandManager().world)
                .flag(main.getCommandManager().locationX)
                .flag(main.getCommandManager().locationY)
                .flag(main.getCommandManager().locationZ)
                .flag(volumeFlag)
                .flag(pitchFlag)
                .flag(soundCategoryFlag)
                .handler((context) -> {
                            final String soundName = context.get("Sound");

                            final boolean stopOtherSounds = context.flags().isPresent("stopOtherSounds");
                            final boolean playForEveryoneAtSetLocationFlagResult = context.flags().isPresent("playForEveryoneAtSetLocation");
                            final boolean playForEveryoneAtTheirLocationFlagResult = context.flags().isPresent("playForEveryoneAtTheirLocation");

                            final World world = context.flags().getValue(main.getCommandManager().world, null);

                            final double locationX = context.flags().getValue(main.getCommandManager().locationX, -1d);
                            final double locationY = context.flags().getValue(main.getCommandManager().locationY, -1d);
                            final double locationZ = context.flags().getValue(main.getCommandManager().locationZ, -1d);

                            final float volume = context.flags().getValue(volumeFlag, -1f);
                            final float pitch = context.flags().getValue(pitchFlag, -1f);

                            final String soundCategoryValue = context.flags().getValue(soundCategoryFlag, "master");

                            final PlaySoundAction playSoundAction =
                                    new PlaySoundAction(main);
                            playSoundAction.setSoundName(soundName);
                            playSoundAction.setStopOtherSounds(stopOtherSounds);
                            playSoundAction.setPlayForEveryoneAtTheirLocation(playForEveryoneAtTheirLocationFlagResult);
                            playSoundAction.setPlayForEveryoneAtSetLocation(playForEveryoneAtSetLocationFlagResult);
                            playSoundAction.setSoundCategory(soundCategoryValue);

                            if (world != null && locationX >= -0.02 && locationY >= -0.02 && locationZ >= -0.02) {
                                playSoundAction.setWorldName(world.getName());
                                playSoundAction.setLocationX(locationX);
                                playSoundAction.setLocationY(locationY);
                                playSoundAction.setLocationZ(locationZ);
                            }
                            if (volume >= -0.02) {
                                playSoundAction.setVolume(volume);
                            }
                            if (pitch >= -0.02) {
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
        if (isStopOtherSounds()) {
            player.stopAllSounds();
        }
        Location location = player.getLocation();
        if (worldName != null && locationX >= -0.02 && locationY >= -0.02 && locationZ >= -0.02) {
            final World world = Bukkit.getWorld(worldName);
            if (world != null) {
                location = new Location(world, locationX, locationY, locationZ);
            }
        }
        final SoundCategory soundCategoryObject = SoundCategory.valueOf(soundCategory.toUpperCase(Locale.ROOT));
        if (isPlayForEveryoneAtTheirLocation()) {
            for (final Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.playSound(onlinePlayer.getLocation(), soundName, soundCategoryObject,
                        volume >= -0.02 ? volume : 1, pitch >= -0.02 ? pitch : 1);
            }
        } else if (isPlayForEveryoneAtSetLocation()) {
            location.getWorld().playSound(location, soundName, soundCategoryObject, volume >= -0.02 ? volume : 1, pitch >= -0.02 ? pitch : 1);
        } else {
            player.playSound(location, soundName, soundCategoryObject, volume >= -0.02 ? volume : 1, pitch >= -0.02 ? pitch : 1);
        }
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.soundName", getSoundName());
        configuration.set(initialPath + ".specifics.stopOtherSounds", isStopOtherSounds());
        if (getWorldName() != null) {
            configuration.set(initialPath + ".specifics.worldName", getWorldName());
        }
        if (getLocationX() >= -0.02) {
            configuration.set(initialPath + ".specifics.locationX", getLocationX());
        }
        if (getLocationY() >= -0.02) {
            configuration.set(initialPath + ".specifics.locationY", getLocationY());
        }
        if (getLocationZ() >= -0.02) {
            configuration.set(initialPath + ".specifics.locationZ", getLocationZ());
        }
        if (getVolume() >= -0.02) {
            configuration.set(initialPath + ".specifics.volume", (double) getVolume());
        }
        if (getPitch() >= -0.02) {
            configuration.set(initialPath + ".specifics.pitch", (double) getPitch());
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
        this.volume = (float) configuration.getDouble(initialPath + ".specifics.volume", -1d);
        this.pitch = (float) configuration.getDouble(initialPath + ".specifics.pitch", -1d);

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

