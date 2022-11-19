package rocks.gravili.notquests.common;



public abstract class NotQuestsMainAbstract <ParsedMessage, Sender> {
    public NotQuestsMainAbstract(){

    }

    public abstract ParsedMessage parse(final String unparsedMessage);

    public abstract void sendMessage(final Sender sender, final String message);

    public abstract void sendMessage(final Sender sender, final ParsedMessage message);

}
