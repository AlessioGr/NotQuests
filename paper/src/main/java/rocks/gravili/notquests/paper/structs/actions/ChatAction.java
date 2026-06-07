package rocks.gravili.notquests.paper.structs.actions;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.suggestion.Suggestion;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static org.incendo.cloud.parser.standard.StringParser.quotedStringParser;

public class ChatAction extends Action {
    private String chatMessage = "";

    public ChatAction(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            PaperCommandManager<CommandSender> manager,
            Command.Builder<CommandSender> builder,
            ActionFor actionFor) {
        manager.command(builder.required("Chat Message", quotedStringParser(), Description.of("Message which will be sent / chatted from the player's perspective."), (context, lastString) -> {
                    main.getUtilManager().sendFancyCommandCompletion(context.sender(), lastString.input().split(" "), "<enter chat message. Wrap in \"\" to use spaces>", "");
                    ArrayList<Suggestion> completions = new ArrayList<>();
                    completions.add(Suggestion.suggestion("<enter chat message. Wrap in \"\" to use spaces>"));
                    return CompletableFuture.completedFuture(completions);
                })
                .handler((context) -> {
                    final String chatMessage = context.get("Chat Message");
                    final ChatAction chatAction = new ChatAction(main);
                    chatAction.setChatMessage(chatMessage);
                    main.getActionManager().addAction(chatAction, context, actionFor);
                }));
    }

    @Override
    public void executeInternally(final QuestPlayer questPlayer, Object... objects) {
        if (chatMessage.isBlank()) {
            main.getLogManager()
                    .warn("Tried to execute PlayerChat action with invalid player chat message.");
            return;
        }

        final Player player = questPlayer.getPlayer();
        if (player == null) {
            main.getLogManager()
                    .warn("Tried to execute PlayerChat action with invalid player.");
            return;
        }

        final String chatMessageWithPlaceholdersReplaced = //TODO: add option to disable placeholders
                main.getUtilManager()
                        .applyPlaceholders(
                                chatMessage, questPlayer.getPlayer(), questPlayer, getObjective(), objects);


        if (Bukkit.isPrimaryThread()) {
            player.chat(chatMessageWithPlaceholdersReplaced);
        } else {
            Bukkit.getScheduler()
                    .runTask(main.getMain(), () -> player.chat(chatMessageWithPlaceholdersReplaced));
        }
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.chatMessage", getChatMessage());
    }

    @Override
    public void load(final FileConfiguration configuration, String initialPath) {
        this.chatMessage = configuration.getString(initialPath + ".specifics.chatMessage");
    }

    @Override
    public void deserializeFromSingleLineString(ArrayList<String> arguments) {
        this.chatMessage = String.join(" ", arguments);
    }

    public final String getChatMessage() {
        return chatMessage;
    }

    public void setChatMessage(final String chatMessage) {
        this.chatMessage = chatMessage;
    }

    @Override
    public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
        return "Player Chat Message: " + getChatMessage();
    }
}
