package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

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
    public Integer getValue(QuestPlayer questPlayer, Object... objects) {
        final Random r = new Random();

        main.getLogManager().debug("0");

        main.getLogManager().debug("AddNumArgs get: " + getAdditionalNumberArguments().get("min"));

        main.getLogManager().debug("reqnumbervalue: " + getRequiredNumberValue("min", questPlayer));


        int min = (int) Math.round(getRequiredNumberValue("min", questPlayer));
        main.getLogManager().debug("1");

        int max = (int) Math.round(getRequiredNumberValue("max", questPlayer));


        return (min==max) ? min : r.nextInt(max+1-min) + min;
    }

    @Override
    public boolean setValueInternally(Integer newValue, QuestPlayer questPlayer, Object... objects) {
        return false;
    }


    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
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
