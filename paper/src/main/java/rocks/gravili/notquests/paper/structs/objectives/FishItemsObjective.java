package rocks.gravili.notquests.paper.structs.objectives;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.paper.PaperCommandManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.MaterialOrHandArgument;
import rocks.gravili.notquests.paper.commands.arguments.variables.NumberVariableValueArgument;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.MaterialOrHand;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.Map;

public class FishItemsObjective extends Objective{


    private ItemStack itemToFish = null;

    private boolean fishAnyItem = false;
    private String nqItemName = "";

    public FishItemsObjective(NotQuests main) {
        super(main);
    }

    public void setNQItem(final String nqItemName){
        this.nqItemName = nqItemName;
    }
    public final String getNQItem(){
        return nqItemName;
    }


    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        manager.command(addObjectiveBuilder
                .argument(MaterialOrHandArgument.of("material", main), ArgumentDescription.of("Material of the item which needs to be fished."))
                .argument(NumberVariableValueArgument.newBuilder("amount", main, null), ArgumentDescription.of("Amount of items which need to be fished"))
                .handler((context) -> {
                    final String amountExpression = context.get("amount");

                    boolean fishAnyItem = false;

                    final MaterialOrHand materialOrHand = context.get("material");
                    ItemStack itemToFish;
                    if (materialOrHand.material.equalsIgnoreCase("any")) {
                        fishAnyItem = true;
                        itemToFish = null;
                    } else {
                        itemToFish = main.getItemsManager().getItemStack(materialOrHand);
                    }

                    FishItemsObjective fishItemsObjective = new FishItemsObjective(main);


                    if(main.getItemsManager().getItem(materialOrHand.material) != null){
                        fishItemsObjective.setNQItem(main.getItemsManager().getItem(materialOrHand.material).getItemName());
                    }else{
                        fishItemsObjective.setItemToFish(itemToFish);
                    }



                    fishItemsObjective.setFishAnyItem(fishAnyItem);
                    fishItemsObjective.setProgressNeededExpression(amountExpression);

                    main.getObjectiveManager().addObjective(fishItemsObjective, context);
                }));
    }

    public final boolean isFishAnyItem() {
        return fishAnyItem;
    }

    public void setFishAnyItem(final boolean fishAnyItem) {
        this.fishAnyItem = fishAnyItem;
    }

    public void setItemToFish(final ItemStack itemToFish) {
        this.itemToFish = itemToFish;
    }

    @Override
    public String getObjectiveTaskDescription(final QuestPlayer questPlayer, final @Nullable ActiveObjective activeObjective) {
        final String displayName;
        if (!isFishAnyItem()) {
            if (getItemToFish().getItemMeta() != null) {
                displayName = getItemToFish().getItemMeta().getDisplayName();
            } else {
                displayName = getItemToFish().getType().name();
            }
        } else {
            displayName = "Any";
        }

        String itemType = isFishAnyItem() ? "Any" : getItemToFish().getType().name();

        if (!displayName.isBlank()) {
            return main.getLanguageManager().getString("chat.objectives.taskDescription.fishItems.base", questPlayer, activeObjective, Map.of(
                    "%ITEMTOFISHTYPE%", itemType,
                    "%ITEMTOFISHNAME%", displayName,
                    "%(%", "(",
                    "%)%", "<RESET>)"
            ));
        } else {
            return main.getLanguageManager().getString("chat.objectives.taskDescription.fishItems.base", questPlayer, activeObjective, Map.of(
                    "%ITEMTOFISHTYPE%", itemType,
                    "%ITEMTOFISHNAME%", "",
                    "%(%", "",
                    "%)%", ""
            ));
        }

    }


    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        if(!getNQItem().isBlank()){
            configuration.set(initialPath + ".specifics.nqitem", getNQItem());
        }else {
            configuration.set(initialPath + ".specifics.itemToFish.itemstack", getItemToFish());
        }

        configuration.set(initialPath + ".specifics.fishAnyItem", isFishAnyItem());
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        this.nqItemName = configuration.getString(initialPath + ".specifics.nqitem", "");
        if(nqItemName.isBlank()){
            itemToFish = configuration.getItemStack(initialPath + ".specifics.itemToFish.itemstack");
        }
        fishAnyItem = configuration.getBoolean(initialPath + ".specifics.fishAnyItem", false);
    }

    public final ItemStack getItemToFish() {
        if(!getNQItem().isBlank()){
            return main.getItemsManager().getItem(getNQItem()).getItemStack().clone();
        }else{
            return itemToFish;
        }
    }

    @Override
    public void onObjectiveUnlock(final ActiveObjective activeObjective, final boolean unlockedDuringPluginStartupQuestLoadingProcess) {
    }
    @Override
    public void onObjectiveCompleteOrLock(final ActiveObjective activeObjective, final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess, final boolean completed) {
    }

}
