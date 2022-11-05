package rocks.gravili.notquests.paper.structs.actions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.paper.PaperCommandManager;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

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
    manager.command(
        builder
            .argument(
                StringArgument.<CommandSender>newBuilder("Chat Message").withSuggestionsProvider(
                    (context, lastString) -> {
                      final List<String> allArgs = context.getRawInput();
                      main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "<enter chat message. Wrap in \"\" to use spaces>", "");

                      ArrayList<String> completions = new ArrayList<>();
                      completions.add("<enter chat message. Wrap in \"\" to use spaces>");
                      return completions;
                    }
                ).quoted().withDefaultDescription(ArgumentDescription.of(
                    "Message which will be sent / chatted from the player's perspective.")).build())
            .handler(
                (context) -> {
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
    if(player == null) {
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
