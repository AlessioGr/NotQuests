package rocks.gravili.notquests.common;


public interface NotQuestsMainAbstract <ParsedMessage, Sender> {

    ParsedMessage parse(final String unparsedMessage);

    void sendMessage(final Sender sender, final String message);

    void sendMessage(final Sender sender, final ParsedMessage message);

}
