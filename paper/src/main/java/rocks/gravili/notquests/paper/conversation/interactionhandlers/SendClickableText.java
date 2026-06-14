package rocks.gravili.notquests.paper.conversation.interactionhandlers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.conversation.Conversation;
import rocks.gravili.notquests.paper.conversation.ConversationLine;
import rocks.gravili.notquests.paper.conversation.ConversationPlayer;
import rocks.gravili.notquests.paper.conversation.Speaker;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.Map;

public class SendClickableText implements
    ConversationInteractionHandler {

  private final NotQuests main;

  public SendClickableText(final NotQuests main) {
    this.main = main;
  }

  @Override
  public void sendText(final String text, final Speaker speaker, final Player player, final
      QuestPlayer questPlayer, final
  Conversation conversation, final ConversationLine conversationLine, final boolean deletePrevious, final ConversationPlayer conversationPlayer) {


    final String lineString = main.getLanguageManager().getString("chat.conversations.speaker-line-format", questPlayer, Map.of(
        "%SPEAKERCOLOR%", speaker.getColor(),
        "%SPEAKER%", speaker.getSpeakerDisplayName(),
        "%MESSAGE%", main.getUtilManager().applyPlaceholders(text, player)
    ));
    final Component line =
        main.parse(lineString);
    if (deletePrevious) {
      main.getConversationManager().removeOldMessages(player);
    }

    if (main.getConfiguration().deletePreviousConversations) {
      final ArrayList<Component> hist =
          main.getConversationManager().getConversationChatHistory().getOrDefault(player.getUniqueId(), new ArrayList<>());
      hist.add(line);
      main.getConversationManager().getConversationChatHistory().put(player.getUniqueId(), hist);
    }

    player.sendMessage(line);
  }

  @Override
  public void sendOption(final String optionMessage, final Speaker speaker, final Player player, final QuestPlayer questPlayer, final
      Conversation conversation, final ConversationLine conversationLine, final ConversationPlayer conversationPlayer) {
    final int optionNumber = conversationPlayer.getCurrentPlayerLines().indexOf(conversationLine) + 1;
    final String toSendString = main.getLanguageManager().getString("chat.conversations.answer-option-line-format", questPlayer, Map.of(
        "%SPEAKERCOLOR%", speaker.getColor(),
        "%SPEAKER%", speaker.getSpeakerDisplayName(),
        "%MESSAGE%", main.getUtilManager().applyPlaceholders(optionMessage, player),
        "%OPTIONNUMBER%", ""+optionNumber
    ));

    final Component toSend =
        main.parse(toSendString)
            .clickEvent(
                // Server-side callback instead of a run_command: modern Minecraft shows a "Confirm
                // Command Execution" prompt for run_command clicks on permission-gated commands
                // (/notquests is permission-gated under native Brigadier). This advances the
                // conversation directly, exactly like the /notquests continueConversation command.
                ClickEvent.callback(audience -> {
                  if (main.getConversationManager() == null) {
                    return;
                  }
                  final QuestPlayer clickingQuestPlayer =
                      main.getQuestPlayerManager().getActiveQuestPlayer(player.getUniqueId());
                  if (clickingQuestPlayer == null) {
                    return;
                  }
                  final ConversationPlayer openConversationPlayer =
                      main.getConversationManager().getOpenConversation(player.getUniqueId());
                  if (openConversationPlayer == null) {
                    return;
                  }
                  final var currentLines = openConversationPlayer.getCurrentPlayerLines();
                  if (optionNumber >= 1 && optionNumber <= currentLines.size()) {
                    final ConversationLine chosen = currentLines.get(optionNumber - 1);
                    if (chosen != null) {
                      openConversationPlayer.chooseOption(chosen);
                    }
                  }
                }))
            .hoverEvent(
                HoverEvent.showText(
                    main.parse(
                        main.getLanguageManager()
                            .getString(
                                "chat.conversations.choose-answer-answer-hover-text",
                                player,
                                conversation,
                                conversationLine))));

    if (main.getConfiguration().deletePreviousConversations) {
      final ArrayList<Component> hist =
          main.getConversationManager().getConversationChatHistory().getOrDefault(player.getUniqueId(), new ArrayList<>());
      hist.add(toSend);
      main.getConversationManager().getConversationChatHistory().put(player.getUniqueId(), hist);
    }

    player.sendMessage(toSend);
  }
}
