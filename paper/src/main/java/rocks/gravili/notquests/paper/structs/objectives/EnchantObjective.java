package rocks.gravili.notquests.paper.structs.objectives;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.Command;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.paper.PaperCommandManager;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.ItemStackSelection;
import rocks.gravili.notquests.paper.managers.expressions.NumberExpression;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.Map;

import static org.incendo.cloud.bukkit.parser.EnchantmentParser.enchantmentParser;
import org.incendo.cloud.component.TypedCommandComponent;
import org.incendo.cloud.parser.flag.CommandFlag;

import static rocks.gravili.notquests.paper.commands.arguments.ItemStackSelectionParser.itemStackSelectionParser;
import static rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueParser.numberVariableParser;

public class EnchantObjective extends Objective {
    private String enchantment;
    private NumberExpression minLevelExpression;
    private NumberExpression maxLevelExpression;
    private ItemStackSelection itemStackSelection;

    private int minLevel;
    private int maxLevel;


    public EnchantObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(
            NotQuests main,
            PaperCommandManager<CommandSender> manager,
            Command.Builder<CommandSender> addObjectiveBuilder,
            final int level) {
        manager.command(addObjectiveBuilder
                .required("enchantment", enchantmentParser(), Description.of("Enchantment which needs to be applied to the item"))
                .required("materials", itemStackSelectionParser(main), Description.of("Material of the item which needs to be enchanted"))
                .required("amount", numberVariableParser("amount", null), Description.of("Amount of times the item needs to be enchanted"))
                .flag(manager.flagBuilder("min").withComponent(TypedCommandComponent.builder("min", numberVariableParser("min", null))))
                .flag(manager.flagBuilder("max").withComponent(TypedCommandComponent.builder("max", numberVariableParser("max", null))))
                .handler(
                        (context) -> {
                            final Enchantment enchantment1 = context.get("enchantment");
                            final String amountExpression = context.get("amount");
                            final ItemStackSelection itemStackSelection = context.get("materials");

                            final String minExpression = context.flags().getValue("min", null);
                            final String maxExpression = context.flags().getValue("max", null);


                            EnchantObjective enchantObjective = new EnchantObjective(main);
                            enchantObjective.setProgressNeededExpression(amountExpression);
                            enchantObjective.setItemStackSelection(itemStackSelection);
                            enchantObjective.setEnchantment(enchantment1.getKey().getKey());
                            enchantObjective.setMinLevelExpression(minExpression != null ? new NumberExpression(main, minExpression) : null);
                            enchantObjective.setMaxLevelExpression(maxExpression != null ? new NumberExpression(main, maxExpression) : null);

                            main.getObjectiveManager().addObjective(enchantObjective, context, level);
                        }));
    }

    public final ItemStackSelection getItemStackSelection() {
        return itemStackSelection;
    }

    public void setItemStackSelection(final ItemStackSelection itemStackSelection) {
        this.itemStackSelection = itemStackSelection;
    }

    public final String getEnchantment() {
        return enchantment;
    }

    public void setEnchantment(final String enchantment) {
        this.enchantment = enchantment;
    }

    private NumberExpression getMinLevelExpression() {
        return minLevelExpression;
    }

    private void setMinLevelExpression(final NumberExpression minLevelExpression) {
        this.minLevelExpression = minLevelExpression;
    }

    private NumberExpression getMaxLevelExpression() {
        return maxLevelExpression;
    }

    private void setMaxLevelExpression(final NumberExpression maxLevelExpression) {
        this.maxLevelExpression = maxLevelExpression;
    }

    public final int getMinLevel() {
        return minLevel;
    }

    public final int getMaxLevel() {
        return maxLevel;
    }

    @Override
    public String getTaskDescriptionInternal(
            final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
        final Enchantment enchantment1 = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(getEnchantment()));

        String enchantmentString = enchantment1 != null ? ("<lang:" + enchantment1.getKey().getKey() + ">") : getEnchantment();


        if (getMinLevel() != 0 && getMaxLevel() != 100) {
            enchantmentString += " (" + getMinLevel() + "-" + getMaxLevel() + ")";
        } else if (getMinLevel() != 0) {
            enchantmentString += " (> " + (getMinLevel() - 1) + ")";
        } else if (getMaxLevel() != 100) {
            enchantmentString += " (< " + (getMaxLevel() + 1) + ")";
        }

        return main.getLanguageManager().getString("chat.objectives.taskDescription.enchant.base", questPlayer, activeObjective,
                        Map.of("%ITEMTOENCHANTTYPE%", getItemStackSelection().getAllMaterialsListedTranslated("main"),
                                "%ITEMTOENCHANTNAME%", "",
                                "%(%", "",
                                "%)%", "",
                                "%ENCHANTMENT%", enchantmentString));
    }

    @Override
    public void onObjectiveUnlock(
            final ActiveObjective activeObjective,
            final boolean unlockedDuringPluginStartupQuestLoadingProcess) {
        // Evaluate min and max level
        if (getMinLevelExpression() != null) {
            minLevel = (int) getMinLevelExpression().calculateValue(activeObjective.getQuestPlayer());
        } else {
            minLevel = 0;
        }

        if (getMaxLevelExpression() != null) {
            maxLevel = (int) getMaxLevelExpression().calculateValue(activeObjective.getQuestPlayer());
        } else {
            maxLevel = 100;
        }

    }

    @Override
    public void onObjectiveCompleteOrLock(
            final ActiveObjective activeObjective,
            final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess,
            final boolean completed) {
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        getItemStackSelection()
                .saveToFileConfiguration(configuration, initialPath + ".specifics.itemStackSelection");

        configuration.set(initialPath + ".specifics.enchantment", getEnchantment());
        configuration.set(initialPath + ".specifics.minLevelExpression", getMinLevelExpression() != null ? getMinLevelExpression().getRawExpression() : null);
        configuration.set(initialPath + ".specifics.maxLevelExpression", getMaxLevelExpression() != null ? getMaxLevelExpression().getRawExpression() : null);

    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        this.itemStackSelection = new ItemStackSelection(main);
        itemStackSelection.loadFromFileConfiguration(
                configuration, initialPath + ".specifics.itemStackSelection");

        this.enchantment = configuration.getString(initialPath + ".specifics.enchantment");

        if (configuration.getString(initialPath + ".specifics.minLevelExpression") != null) {
            this.minLevelExpression = new NumberExpression(main, configuration.getString(initialPath + ".specifics.minLevelExpression"));
        } else {
            this.minLevelExpression = null;
        }

        if (configuration.getString(initialPath + ".specifics.maxLevelExpression") != null) {
            this.maxLevelExpression = new NumberExpression(main, configuration.getString(initialPath + ".specifics.maxLevelExpression"));
        } else {
            this.maxLevelExpression = null;
        }

    }
}
