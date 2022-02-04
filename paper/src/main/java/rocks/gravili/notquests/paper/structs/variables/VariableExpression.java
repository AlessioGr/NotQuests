package rocks.gravili.notquests.paper.structs.variables;

import org.bukkit.entity.Player;
import redempt.crunch.CompiledExpression;
import redempt.crunch.functional.EvaluationEnvironment;

import java.util.regex.Pattern;

public class VariableExpression {
    private static Pattern placeholderPattern = Pattern.compile("%([^% ]+)_([^% ]+)%");

    private CompiledExpression exp;
    private Player player;

    public VariableExpression(String str, EvaluationEnvironment env) {
       /* Matcher matcher = placeholderPattern.matcher(str);
        LocalExpansionManager manager = PlaceholderAPIPlugin.getInstance().getLocalExpansionManager();
        while (matcher.find()) {
            String placeholder = matcher.group();
            String identifier = matcher.group(1);
            String arg = matcher.group(2);
            PlaceholderExpansion expansion = manager.getExpansion(identifier);
            if (expansion == null) {
                continue;
            }
            env.addLazyVariable(placeholder, () -> {
                String replaced = expansion.onRequest(player, arg);
                if (replaced == null) {
                    return 0;
                }
                return FastNumberParsing.parseDouble(replaced);
            });
        }
        exp = Crunch.compileExpression(str, env);*/
    }

  /*  public PlaceholderExpression(String str) {
        this(str, new EvaluationEnvironment());
    }

    public double evaluate(Player player) {
        this.player = player;
        return exp.evaluate();
    }*/
}
