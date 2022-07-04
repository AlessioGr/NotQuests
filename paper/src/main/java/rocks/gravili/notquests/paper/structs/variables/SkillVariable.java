package rocks.gravili.notquests.paper.structs.variables;

import cloud.commandframework.arguments.standard.StringArgument;
import java.util.List;
import com.neostorm.neostorm.Api;
import rocks.gravili.notquests.paper.structs.QuestPlayer;
import java.util.ArrayList;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.NotQuests;

public class SkillVariable extends Variable<Integer>
{
    public SkillVariable(final NotQuests main) {
        super(main);
        this.addRequiredString(StringArgument.<CommandSender>newBuilder("Skill").withSuggestionsProvider((context, lastString) -> {
            final List<String> allArgs = context.getRawInput();
            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Skill]", "[...]");
            final ArrayList<String> suggestions = new ArrayList<>();
            suggestions.add("<Enter Skill>");
            return suggestions;
        }).single().build());
    }

    @Override
    public Integer getValue(final QuestPlayer questPlayer, final Object... objects) {
        return Api.getStats(questPlayer.getPlayer(), this.getRequiredStringValue("Skill"));
    }

    @Override
    public boolean setValueInternally(final Integer newValue, final QuestPlayer questPlayer, final Object... objects) {
        return true;
    }

    @Override
    public List<String> getPossibleValues(final QuestPlayer questPlayer, final Object... objects) {
        return List.of(Api.getStatTable());
    }

    @Override
    public String getPlural() {
        return "Skills";
    }

    @Override
    public String getSingular() {
        return "Skill";
    }
}