package rocks.gravili.notquests.paper.commands.category.admin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.BaseCommand;
import rocks.gravili.notquests.paper.commands.framework.NQArguments;
import rocks.gravili.notquests.paper.commands.framework.NQCommandBuilder;
import rocks.gravili.notquests.paper.commands.framework.NQCommandManager;
import rocks.gravili.notquests.paper.commands.framework.NQDescription;
import rocks.gravili.notquests.paper.commands.framework.NQFlag;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;

import static rocks.gravili.notquests.paper.commands.arguments.LocationArgument.locationArgument;

public class DebugCommand extends BaseCommand {
    public DebugCommand(NotQuests notQuests, NQCommandBuilder builder) {
        super(notQuests, builder);
    }

    @Override
    public void apply(NQCommandManager commandManager) {
        commandManager.command(builder.literal("debug", NQDescription.of("Toggles debug mode for yourself."))
                .senderType(Player.class)
                .handler((context) -> {

                    if (notQuests.getQuestManager().isDebugEnabledPlayer(((Player) context.sender()).getUniqueId())) {
                        notQuests.getQuestManager().removeDebugEnabledPlayer(((Player) context.sender()).getUniqueId());
                        context.sender().sendMessage(notQuests.parse("<success>Your debug mode has been disabled."));
                    } else {
                        notQuests.getQuestManager().addDebugEnabledPlayer(((Player) context.sender()).getUniqueId());
                        context.sender().sendMessage(notQuests.parse("<success>Your debug mode has been enabled."));
                    }

                }));

        commandManager.command(builder.commandDescription(NQDescription.of("Clears your own chat"))
                .literal("debug")
                .literal("clearOwnChat")
                .handler((context) -> {
                    final Component componentToSend = Component.text("").append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline()).append(Component.newline());
                    context.sender().sendMessage(componentToSend);
                }));

        commandManager.command(builder.commandDescription(NQDescription.of("Shows you information about the current world"))
                .literal("debug")
                .literal("worldInfo")
                .senderType(Player.class)
                .handler((context) -> {
                    context.sender().sendMessage(Component.empty());
                    Player player = (Player) context.sender();
                    player.sendMessage(notQuests.parse(
                            "<main>Current world name: <highlight>" + player.getWorld().getName() + "\n" +
                                    "<main>Current world UUD: <highlight>" + player.getWorld().getUID().toString()

                    ));
                }));


        commandManager.command(builder.commandDescription(NQDescription.of("Calls the dataManager.reloadData() method. This starts loading all Config-, Quest-, and Player Data. Reload = Load"))
                .literal("debug")
                .literal("loadDataManagerUnsafe")
                .handler((context) -> {
                    context.sender().sendMessage(Component.empty());
                    context.sender().sendMessage(notQuests.parse(
                            "<main>Reloading DataManager..."
                    ));
                    notQuests.getDataManager().reloadData(false);
                    context.sender().sendMessage(notQuests.parse(
                            "<success>DataManager has been reloaded!"

                    ));
                }));

        commandManager.command(builder.commandDescription(NQDescription.of("Disables NotQuests, saving & loading"))
                .literal("debug")
                .literal("disablePluginAndSaving")
                .required("reason", NQArguments.stringArgument(), NQDescription.of("Reason for disabling the plugin"))
                .handler((context) -> {
                    context.sender().sendMessage(Component.empty());

                    if (notQuests.getDataManager().isDisabled()) {
                        context.sender().sendMessage(notQuests.parse(
                                "<error>Error: NotQuests is already disabled"
                        ));
                        return;
                    }

                    final String reason = context.get("reason");
                    context.sender().sendMessage(notQuests.parse(
                            "<main>Disabling NotQuests..."
                    ));
                    notQuests.getDataManager().disablePluginAndSaving(reason);

                }));

        commandManager.command(builder.commandDescription(NQDescription.of("Shows the current errors and warnings NotQuests collected"))
                .literal("debug")
                .literal("showErrorsAndWarnings")
                .flag(NQFlag.presence("printToConsole", NQDescription.of("Prints the output to the console")))
                .handler((context) -> {
                    final boolean printToConsole = context.flags().contains("printToConsole");


                    if (!printToConsole) {
                        context.sender().sendMessage(Component.empty());
                        notQuests.getDataManager().sendErrorsAndWarnings(context.sender());
                    } else {
                        notQuests.getMain().getServer().getConsoleSender().sendMessage(Component.empty());
                        notQuests.getDataManager().sendErrorsAndWarnings(notQuests.getMain().getServer().getConsoleSender());
                        context.sender().sendMessage(
                                notQuests.parse(
                                        "<success>Error and warnings have been printed to console successfully!"
                                )
                        );
                    }
                }));

        commandManager.command(builder.commandDescription(NQDescription.of("Enables NotQuests, saving & loading"))
                .literal("debug")
                .literal("enablePluginAndSaving")
                .required("reason", NQArguments.stringArgument(), NQDescription.of("Reason for enabling the plugin"))
                .handler((context) -> {
                    context.sender().sendMessage(Component.empty());

                    if (!notQuests.getDataManager().isDisabled()) {
                        context.sender().sendMessage(notQuests.parse(
                                "<error>Error: NotQuests is already enabled"
                        ));
                        return;
                    }

                    final String reason = context.get("reason");
                    context.sender().sendMessage(notQuests.parse(
                            "<main>Enabling NotQuests..."
                    ));
                    notQuests.getDataManager().enablePluginAndSaving(reason);

                }));

        commandManager.command(builder.commandDescription(NQDescription.of("You can probably ignore this."))
                .literal("debug")
                .literal("testcommand")
                .senderType(Player.class)
                .handler((context) -> {
                    context.sender().sendMessage(Component.empty());
                    Player player = (Player) context.sender();
                    ArrayList<Component> history = notQuests.getConversationManager().getChatHistory().get(player.getUniqueId());
                    if (history != null) {
                        Component collectiveComponent = Component.text("");
                        for (Component component : history) {
                            if (component != null) {
                                // audience.sendMessage(component.append(Component.text("fg9023zf729ofz")));
                                collectiveComponent = collectiveComponent.append(component).append(Component.newline());
                            }
                        }

                        context.sender().sendMessage(collectiveComponent);

                    } else {
                        context.sender().sendMessage(notQuests.parse("<error>No chat history!"));
                    }

                }));


        commandManager.command(builder.commandDescription(NQDescription.of("You can probably ignore this."))
                .literal("debug")
                .literal("testcommand2")
                .senderType(Player.class)
                .handler((context) -> {
                    context.sender().sendMessage(Component.empty());
                    Player player = (Player) context.sender();
                    ArrayList<Component> history = notQuests.getConversationManager().getChatHistory().get(player.getUniqueId());
                    if (history != null) {
                        Component collectiveComponent = Component.text("");
                        for (int i = 0; i < history.size(); i++) {
                            Component component = history.get(i);
                            if (component != null) {
                                // audience.sendMessage(component.append(Component.text("fg9023zf729ofz")));
                                collectiveComponent = collectiveComponent.append(Component.text(i + ".", NamedTextColor.RED).append(component)).append(Component.newline());
                            }
                        }

                        context.sender().sendMessage(collectiveComponent);

                    } else {
                        context.sender().sendMessage(notQuests.parse("<error>No chat history!"));
                    }

                }));

        commandManager.command(builder.commandDescription(NQDescription.of("Spawns a beacon beam"))
                .literal("debug")
                .literal("beaconBeam")
                .required("player", NQArguments.playerArgument(), NQDescription.of("Player name"))
                .required("location-name", NQArguments.stringArgument(), NQDescription.of("Location name"))
                .required("location", locationArgument())
                .handler((context) -> {
                    final Player player = context.get("player");
                    final String locationName = context.get("location-name");
                    final Location location = context.get("location");

                    /*if(notQuests.getPacketManager().isModern()){
                        notQuests.getPacketManager().getModernPacketInjector().spawnBeaconBeam(player, location);
                        notQuests.sendMessage(player, "<success>Beacon beam spawned successfully!");
                    }*/


                    final QuestPlayer questPlayer = notQuests.getQuestPlayerManager().getOrCreateQuestPlayer(player.getUniqueId());

                    questPlayer.getLocationsAndBeacons().put(locationName, location);
                    questPlayer.updateBeaconLocations(player);

                    notQuests.sendMessage(context.sender(), "<success>Beacon beam spawned successfully!");
                }));
    }
}
