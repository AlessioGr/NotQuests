package rocks.gravili.notquests.paper.structs.actions;

import org.incendo.cloud.Command;
import org.incendo.cloud.bukkit.data.SinglePlayerSelector;
import org.incendo.cloud.bukkit.parser.selector.SinglePlayerSelectorParser;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import static org.incendo.cloud.parser.standard.IntegerParser.integerParser;
import static org.incendo.cloud.parser.standard.StringParser.stringParser;
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

    public static void handleCommands(NotQuests main, LegacyPaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ActionFor actionFor) {
        manager.command(
                builder.required("guiName", stringParser(), Description.of("Opens a gui for the player"))
                        .flag(manager.flagBuilder("targetPlayer").withComponent(SinglePlayerSelectorParser.singlePlayerSelectorParser()).build())
                        .flag(manager.flagBuilder("quest").withComponent(stringParser()).build())
                        .flag(manager.flagBuilder("npc").withComponent(integerParser()).build())
                        .flag(manager.flagBuilder("category").withComponent(stringParser()).build())

                .handler(commandContext -> {
                    String guiName = commandContext.get("guiName");
                    String questName = commandContext.flags().getValue("quest", null);

                    var quest = questName == null ? null : main.getQuestManager().getQuest(questName);


                    SinglePlayerSelector playerSelector = commandContext.flags().getValue("targetPlayer", null);
                    var targetPlayer = playerSelector == null ? null : playerSelector.single();
                    Integer npcId = commandContext.flags().getValue("npc", null);

                    String categoryName = commandContext.flags().getValue("category", null);

                    var guiContext = new GuiContext();
                    guiContext.setPlayer(targetPlayer);
                    guiContext.setQuest(quest);

                    if (npcId != null) {
                        var npc = main.getNPCManager().getOrCreateNQNpc("Citizens", NQNPCID.fromInteger(npcId));
                        guiContext.setNqnpc(npc);
                    }

                    if (categoryName != null) {
                        var category = main.getDataManager().getCategory(categoryName);
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
        // Skip unreplaced placeholders ("%NPCID%") and non-integer ids (FancyNPCs/armor stand use
        // String/UUID ids) instead of crashing on Integer.parseInt. The NPC context is optional here.
        if (npcId != null && !npcId.contains("%")) {
            try {
                this.guiContext.setNqnpc(main.getNPCManager().getOrCreateNQNpc("Citizens", NQNPCID.fromInteger(Integer.parseInt(npcId))));
            } catch (final NumberFormatException e) {
                main.getLogManager().debug("OpenGuiAction: NPC id '" + npcId + "' is not a Citizens integer id; skipping NPC context.");
            }
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
