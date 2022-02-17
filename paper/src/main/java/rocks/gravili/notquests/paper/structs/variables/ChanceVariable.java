package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;

import java.util.List;

public class ChanceVariable extends Variable<Boolean>{

    public ChanceVariable(NotQuests main) {
        super(main);

        addRequiredNumber(
                NumberVariableValueArgument.<CommandSender>newBuilder("chance", main, null).build()
        );
    }

    @Override
    public Boolean getValue(Player player, Object... objects) {
        double chanceToHave = getRequiredNumberValue("chance", player);

        double random = Math.random() * 100;
        return random < chanceToHave;
    }

    @Override
    public boolean setValueInternally(Boolean newValue, Player player, Object... objects) {
        return false;
    }


    @Override
    public List<String> getPossibleValues(Player player, Object... objects) {
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
