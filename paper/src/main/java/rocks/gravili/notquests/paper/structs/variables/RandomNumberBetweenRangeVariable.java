package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.Bukkit;
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
                NumberVariableValueArgument.<CommandSender>newBuilder("min", main, null).build()
        );
        addRequiredNumber(
                NumberVariableValueArgument.<CommandSender>newBuilder("max", main, null).build()
        );
    }

    @Override
    public Integer getValue(Player player, Object... objects) {
        final Random r = new Random();

        main.getLogManager().debug("0");

        main.getLogManager().debug("AddNumArgs get: " + getAdditionalNumberArguments().get("min"));

        main.getLogManager().debug("reqnumbervalue: " + getRequiredNumberValue("min", player));


        int min = (int) Math.round(getRequiredNumberValue("min", player));
        main.getLogManager().debug("1");

        int max = (int) Math.round(getRequiredNumberValue("max", player));


        return (min==max) ? min : r.nextInt(max+1-min) + min;
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
