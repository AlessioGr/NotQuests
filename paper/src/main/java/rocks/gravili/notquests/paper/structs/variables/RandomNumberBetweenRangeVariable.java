package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;

import java.util.List;
import java.util.Random;

public class RandomNumberBetweenRangeVariable extends Variable<Integer>{
    public RandomNumberBetweenRangeVariable(NotQuests main) {
        super(main);

        addRequiredNumber(
                NumberVariableValueArgument.<CommandSender>newBuilder("min", main, this).build()
        );
        addRequiredNumber(
                NumberVariableValueArgument.<CommandSender>newBuilder("max", main, this).build()
        );
    }

    @Override
    public Integer getValue(Player player, Object... objects) {
        if (player != null) {
            final Random r = new Random();

            int min = (int) Math.round(getRequiredNumberValue("min", player));
            int max = (int) Math.round(getRequiredNumberValue("max", player));

            return (min==max) ? min : r.nextInt(max+1-min) + min;
        } else {
            return null;
        }
    }

    @Override
    public boolean setValueInternally(Integer newValue, Player player, Object... objects) {
        return false;
    }


    @Override
    public List<String> getPossibleValues(Player player, Object... objects) {
        return null;
    }

    @Override
    public String getPlural() {
        return "Random numbers";
    }

    @Override
    public String getSingular() {
        return "Random number";
    }
}
