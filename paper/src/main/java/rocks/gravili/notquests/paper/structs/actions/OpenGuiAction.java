package rocks.gravili.notquests.paper.structs.actions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.bukkit.arguments.selector.SinglePlayerSelector;
import cloud.commandframework.bukkit.parsers.selector.SinglePlayerSelectorArgument;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;

import rocks.gravili.notquests.paper.gui.GuiContext;
import rocks.gravili.notquests.paper.managers.FlagParser;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.managers.npc.NQNPCID;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;

public class OpenGuiAction extends Action {
    private String guiName = "";
    private GuiContext guiContext;
    public OpenGuiAction(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ActionFor actionFor) {
        manager.command(
                builder.argument(
                        StringArgument.<CommandSender>builder("guiName").withSuggestionsProvider((objectCommandContext, s) -> {
                            var completions = main.getGuiService().getGuis().keySet();
                            return completions.stream().toList();

                        }).build(), ArgumentDescription.of("Opens a gui for the player"))
                        .flag(manager.flagBuilder("targetPlayer").withArgument(SinglePlayerSelectorArgument.of("targetPlayer")).build())
                        .flag(manager.flagBuilder("quest").withArgument(StringArgument.of("quest")).build())
                        .flag(manager.flagBuilder(("npc")).withArgument(IntegerArgument.of("npc")).build())
                        .flag(manager.flagBuilder(("category")).withArgument(StringArgument.of("category")).build())

                .handler(commandContext -> {
                    var guiName = (String) commandContext.get("guiName");
                    var questName = (String) commandContext.flags().get("quest");

                    var quest = questName == null ? null : main.getQuestManager().getQuest(questName);


                    var playerSelector = (SinglePlayerSelector) commandContext.flags().get("targetPlayer");
                    var targetPlayer = playerSelector == null ? null : playerSelector.getPlayer();
                    var npcId = commandContext.flags().get("npc") == null ? null : commandContext.flags().get("npc");

                    var categoryName = commandContext.flags().get("category") == null ? null : commandContext.flags().get("category");

                    var guiContext = new GuiContext();
                    guiContext.setPlayer(targetPlayer);
                    guiContext.setQuest(quest);

                    if (npcId != null) {
                        var npc = main.getNPCManager().getOrCreateNQNpc("Citizens", NQNPCID.fromInteger(Integer.parseInt(String.valueOf(npcId))));
                        guiContext.setNqnpc(npc);
                    }

                    if (categoryName != null) {
                        var category = main.getDataManager().getCategory(String.valueOf(categoryName));
                        guiContext.setCategory(category);
                    }


                    var openGuiAction = new OpenGuiAction(main);
                    openGuiAction.setGuiName(guiName);
                    openGuiAction.setGuiContext(guiContext);

                    main.getActionManager().addAction(openGuiAction, commandContext, actionFor);
                }));
    }


    @Override
    public String getActionDescription(QuestPlayer questPlayer, Object... objects) {
        return "Opens a gui with given name for the player";
    }

    @Override
    protected void executeInternally(QuestPlayer questPlayer, Object... objects) {
        final var player = questPlayer.getPlayer();
        if(player == null) {
            main.getLogManager().warn("Tried to execute PlayerCommand action with invalid player.");
            return;
        }

        if (guiContext.getPlayer() == null) {
            guiContext.setPlayer(questPlayer.getPlayer());
        }


        if (Bukkit.isPrimaryThread()) {
            main.getGuiService().showGui(guiName, player, guiContext);
        } else {
            Bukkit.getScheduler().runTask(
                    main.getMain(), () -> main.getGuiService().showGui(guiName, player, guiContext)
            );
        }
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.guiName", getGuiName());
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        this.guiName = configuration.getString(initialPath + ".specifics.guiName");
    }

    @Override
    public void deserializeFromSingleLineString(ArrayList<String> arguments) {
        this.guiName = arguments.get(0);
        this.guiContext = new GuiContext();

        var flags = FlagParser.parseFlags(String.join(" ", arguments));
        flags.forEach((key, value) -> main.getLogManager().debug(key  + " " + value));

        var questName = (String) flags.get("quest");
        if (questName != null) {
            this.guiContext.setQuest(main.getQuestManager().getQuest(questName));
        }

        var npcId = (String) flags.get("npc");
        if (npcId != null) {
            this.guiContext.setNqnpc(main.getNPCManager().getOrCreateNQNpc("Citizens", NQNPCID.fromInteger(Integer.parseInt(npcId))));
        }

        var categoryName = (String) flags.get("category");
        if (categoryName != null) {
            var category = main.getDataManager().getCategory(categoryName);
            guiContext.setCategory(category);
        }

        var targetPlayer = (String) flags.get("targetPlayer");
        if (targetPlayer != null) {
            var player = main.getMain().getServer().getPlayer(targetPlayer);
            if (player != null) {
                this.guiContext.setPlayer(player);
            }
        }
    }

    public final String getGuiName() {
        return guiName;
    }

    public void setGuiName(String guiName) {
        this.guiName = guiName;
    }

    public void setGuiContext(GuiContext guiContext) {
        this.guiContext = guiContext;
    }

    public GuiContext getGuiContext() {
        return guiContext;
    }
}
