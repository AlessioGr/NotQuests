package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.List;

public class ChanceVariable extends Variable<Boolean>{

    public ChanceVariable(NotQuests main) {
        super(main);

        addRequiredNumber(
                NumberVariableValueArgument.<CommandSender>newBuilder("chance", main, null).build()
        );
    }

    @Override
    public Boolean getValue(QuestPlayer questPlayer, Object... objects) {
        double chanceToHave = getRequiredNumberValue("chance", questPlayer);

        double random = Math.random() * 100;
        return random < chanceToHave;
    }

    @Override
    public boolean setValueInternally(Boolean newValue, QuestPlayer questPlayer, Object... objects) {
        return false;
    }


    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return "Chances";
    }

    @Override
    public String getSingular() {
        return "Chance";
    }
}
