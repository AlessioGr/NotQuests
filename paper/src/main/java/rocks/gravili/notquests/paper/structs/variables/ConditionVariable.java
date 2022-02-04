package rocks.gravili.notquests.paper.structs.variables;

import cloud.commandframework.arguments.standard.BooleanArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import redempt.crunch.CompiledExpression;
import redempt.crunch.Crunch;
import redempt.crunch.functional.EvaluationEnvironment;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;

public class ConditionVariable extends Variable<Boolean>{

    private CompiledExpression exp;
    private final EvaluationEnvironment env = new EvaluationEnvironment();
    private int variableCounter = 0;
    Variable<?> cachedVariable = null;
    private Player playerToEvaluate = null;
    private QuestPlayer questPlayerToEvaluate = null;

    public ConditionVariable(NotQuests main) {
        super(main);

        addRequiredString(
                StringArgument.<CommandSender>newBuilder("Conditions").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Conditions(s) expression]", "[...]");

                            ArrayList<String> suggestions = new ArrayList<>();
                            for(String conditionIdentifier : main.getConditionsYMLManager().getConditionsAndIdentifiers().keySet()){
                                if(lastString.endsWith(conditionIdentifier)){
                                    suggestions.add(lastString+"&");
                                    suggestions.add(lastString+"|");
                                }else{
                                    suggestions.add(conditionIdentifier);
                                }
                            }
                            return suggestions;

                        }
                ).single().build()
        );
    }

    public final String getExpression(){
        return getRequiredStringValue("Conditions");
    }

    @Override
    public Boolean getValue(Player player, Object... objects) {
        this.playerToEvaluate = player;
        this.questPlayerToEvaluate = main.getQuestPlayerManager().getQuestPlayer(playerToEvaluate.getUniqueId());
        initializeExpressionAndCachedVariable();

        return exp.evaluate() >= 0.98d;
    }


    public String getExpressionAndGenerateEnv(String expressions){
        boolean foundOne = false;
        for(String conditionIdentifier : main.getConditionsYMLManager().getConditionsAndIdentifiers().keySet()){
            if(!expressions.contains(conditionIdentifier)){
                continue;
            }
            foundOne = true;

            variableCounter++;
            expressions = expressions.replace(conditionIdentifier, "var" + variableCounter);
            env.addLazyVariable("var" + variableCounter, () -> {
                return main.getConditionsYMLManager().getCondition(conditionIdentifier).check(questPlayerToEvaluate).isBlank() ? 1 : 0;
            });
        }
        if(!foundOne){
            return expressions;
        }

        return getExpressionAndGenerateEnv(expressions);
    }

    public void initializeExpressionAndCachedVariable(){
        if(exp == null){
            String expression = getExpressionAndGenerateEnv(getExpression());
            exp = Crunch.compileExpression(expression, env);
        }
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
        return "Conditions";
    }

    @Override
    public String getSingular() {
        return "Condition";
    }
}
