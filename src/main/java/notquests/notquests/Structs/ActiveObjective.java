package notquests.notquests.Structs;


import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.trait.FollowTrait;
import notquests.notquests.NotQuests;
import notquests.notquests.Structs.Objectives.EscortNPCObjective;
import notquests.notquests.Structs.Objectives.Objective;
import notquests.notquests.Structs.Objectives.OtherQuestObjective;
import notquests.notquests.Structs.Triggers.ActiveTrigger;
import notquests.notquests.Structs.Triggers.TriggerTypes.TriggerType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.logging.Level;

public class ActiveObjective {
    private final NotQuests main;
    private final Objective objective;
    private final ActiveQuest activeQuest;
    private final int objectiveID;
    private long currentProgress;
    private boolean unlocked = false;
    private boolean hasBeenCompleted = false;

    public ActiveObjective(final NotQuests main, final int objectiveID, final Objective objective, final ActiveQuest activeQuest) {
        this.main = main;
        this.objectiveID = objectiveID;
        this.objective = objective;
        this.activeQuest = activeQuest;
        currentProgress = 0;

    }

    public final void setUnlocked(final boolean unlocked, final boolean notifyPlayer, final boolean triggerAcceptQuestTrigger) {
        if (this.unlocked != unlocked) {
            this.unlocked = unlocked;
            if (unlocked) {
                if (triggerAcceptQuestTrigger) {
                    for (final ActiveTrigger activeTrigger : getActiveQuest().getActiveTriggers()) {
                        if (activeTrigger.getTrigger().getTriggerType().equals(TriggerType.BEGIN)) { //Start the quest
                            if (activeTrigger.getTrigger().getApplyOn() >= 1) { //Objective and not Quest
                                if (getObjectiveID() == activeTrigger.getTrigger().getApplyOn()) {
                                    if (activeTrigger.getTrigger().getWorldName().equalsIgnoreCase("ALL")) {
                                        activeTrigger.addAndCheckTrigger(activeQuest);
                                    } else {
                                        final Player player = Bukkit.getPlayer(getQuestPlayer().getUUID());
                                        if (player != null && player.getWorld().getName().equalsIgnoreCase(activeTrigger.getTrigger().getWorldName())) {
                                            activeTrigger.addAndCheckTrigger(activeQuest);
                                        }
                                    }


                                }
                            }
                        }
                    }
                }


                if (objective instanceof EscortNPCObjective) {
                    final int npcToEscortID = ((EscortNPCObjective) objective).getNpcToEscortID();
                    final int destinationNPCID = ((EscortNPCObjective) objective).getNpcToEscortToID();
                    final NPC npcToEscort = CitizensAPI.getNPCRegistry().getById(npcToEscortID);
                    final NPC destinationNPC = CitizensAPI.getNPCRegistry().getById(destinationNPCID);
                    if (npcToEscort != null && destinationNPC != null) {
                        FollowTrait followerTrait = null;
                        for (final Trait trait : npcToEscort.getTraits()) {
                            if (trait.getName().toLowerCase().contains("follow")) {
                                followerTrait = (FollowTrait) trait;
                            }
                        }
                        if (followerTrait == null) {
                            followerTrait = new FollowTrait();
                            npcToEscort.addTrait(followerTrait);
                        }

                        if (followerTrait != null) {
                            final Player player = Bukkit.getPlayer(activeQuest.getQuestPlayer().getUUID());
                            if (player != null) {
                                if (!npcToEscort.isSpawned()) {
                                    npcToEscort.spawn(player.getLocation());
                                }

                                if (followerTrait.getFollowingPlayer() == null || !followerTrait.getFollowingPlayer().equals(player)) {
                                    if (!Bukkit.isPrimaryThread()) {
                                        final FollowTrait finalFollowerTrait = followerTrait;
                                        Bukkit.getScheduler().runTask(main, () -> {
                                            finalFollowerTrait.toggle(player, false);
                                        });
                                    } else {
                                        followerTrait.toggle(player, false);
                                    }
                                }


                                player.sendMessage("§aEscort quest started! Please escort §b" + npcToEscort.getName() + " §ato §b" + destinationNPC.getName() + "§a.");
                            } else {
                                main.getLogger().log(Level.WARNING, "§cNotQuests > Error: The escort objective could not be started, because the player with the UUID §b" + activeQuest.getQuestPlayer().getActiveQuests() + " §cwas not found!");


                            }
                        } else {
                            final Player player = Bukkit.getPlayer(activeQuest.getQuestPlayer().getUUID());
                            if (player != null) {
                                player.sendMessage("§cNotQuests > The NPC you have to escort is not configured properly. Please consult an admin.");
                            }
                            main.getLogger().log(Level.WARNING, "§cNotQuests > Error: The escort NPC with the ID §b" + npcToEscortID + " §cis not configured properly (Follow trait not found)!");

                        }
                    } else {
                        if (destinationNPC == null) {
                            final Player player = Bukkit.getPlayer(activeQuest.getQuestPlayer().getUUID());
                            if (player != null) {
                                player.sendMessage("§cNotQuests > The Destination NPC does not exist. Please consult an admin.");
                            }
                            main.getLogger().log(Level.WARNING, "§cNotQuests > Error: The destination NPC with the ID §b" + npcToEscortID + " §cwas not found!");

                        }
                        if (npcToEscort == null) {
                            final Player player = Bukkit.getPlayer(activeQuest.getQuestPlayer().getUUID());
                            if (player != null) {
                                player.sendMessage("§cNotQuests > The NPC you have to escort does not exist. Please consult an admin.");
                            }
                            main.getLogger().log(Level.WARNING, "§cNotQuests > Error: The escort NPC with the ID §b" + npcToEscortID + " §cwas not found!");

                        }

                    }
                }


                //TODO: What?
                if (objective instanceof OtherQuestObjective) {
                    if (((OtherQuestObjective) objective).isCountPreviousCompletions()) {
                        for (CompletedQuest completedQuest : getQuestPlayer().getCompletedQuests()) {
                            if (completedQuest.getQuest().equals(((OtherQuestObjective) objective).getOtherQuest())) {
                                addProgress(1, -1);
                            }
                        }
                    }
                }
                if (notifyPlayer) {
                    final Player player = Bukkit.getPlayer(getQuestPlayer().getUUID());
                    if (player != null) {
                        main.getQuestManager().sendActiveObjective(player, this);
                    }
                }

            }
        }

    }

    public final boolean isUnlocked() {
        return unlocked;
    }

    public void updateUnlocked(final boolean notifyPlayer, final boolean triggerAcceptQuestTrigger) {

        boolean foundStillDependant = false;
        for (final Objective dependantObjective : objective.getDependantObjectives()) {
            for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                if (activeObjective.getObjectiveID() == dependantObjective.getObjectiveID()) {
                    foundStillDependant = true;
                    if (!isUnlocked()) {
                        setUnlocked(false, notifyPlayer, triggerAcceptQuestTrigger);
                    }

                    break;
                }
            }
            if (foundStillDependant) {
                break;
            }
        }
        if (!foundStillDependant) {
            setUnlocked(true, notifyPlayer, triggerAcceptQuestTrigger);

        }


    }

    public final ArrayList<ActiveObjective> getObjectivesWhichStillNeedToBeCompletedBeforeUnlock() {
        final ArrayList<ActiveObjective> stillDependantObjectives = new ArrayList<>();
        for (final Objective dependantObjective : objective.getDependantObjectives()) {
            for (final ActiveObjective activeObjective : activeQuest.getActiveObjectives()) {
                if (activeObjective.getObjectiveID() == dependantObjective.getObjectiveID()) {
                    stillDependantObjectives.add(activeObjective);
                    break;
                }
            }


        }

        return stillDependantObjectives;
    }

    public final Objective getObjective() {
        return objective;
    }

    public final long getProgressNeeded() {
        return objective.getProgressNeeded();
    }

    public final long getCurrentProgress() {
        return currentProgress;
    }

    public void addProgress(long i, final int NPCID) {
        currentProgress += i;
        if (isCompleted(NPCID)) {
            setHasBeenCompleted(true);
            activeQuest.notifyActiveObjectiveCompleted(this, false, NPCID);
        }
    }

    public void addProgressSilent(long i, final int NPCID) {
        currentProgress += i;
        if (isCompleted(NPCID)) {
            setHasBeenCompleted(true);
            activeQuest.notifyActiveObjectiveCompleted(this, true, NPCID);
        }
    }

    public void removeProgress(int i, boolean capAtZero) {

        if (capAtZero) {
            if (currentProgress - i < 0) {
                if (currentProgress > 0) {
                    currentProgress = 0;
                }
            } else {
                currentProgress -= i;
            }
        } else {
            currentProgress -= i;
        }


    }

    public final boolean isCompleted(final int NPCID) {
        if (getObjective().getCompletionNPCID() == -1 || getObjective().getCompletionNPCID() == NPCID) {
            return currentProgress >= objective.getProgressNeeded();
        } else {
            return false;
        }

    }

    public final QuestPlayer getQuestPlayer() {
        return activeQuest.getQuestPlayer();
    }

    public final ActiveQuest getActiveQuest() {
        return activeQuest;
    }

    public final int getObjectiveID() {
        return objectiveID;
    }

    public final boolean hasBeenCompleted() {
        return hasBeenCompleted;
    }

    public void setHasBeenCompleted(final boolean hasBeenCompleted) {
        // System.out.println("§4§lSet has been completed to: §b" + hasBeenCompleted + " §cfor objective with ID §b" + getObjectiveID());
        this.hasBeenCompleted = hasBeenCompleted;
    }
}
