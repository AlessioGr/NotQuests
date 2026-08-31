package com.notquests.paper.integrations.betonquest;

import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.api.BetonQuestApi;
import org.betonquest.betonquest.api.BetonQuestApiService;
import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.config.quest.QuestPackage;
import org.betonquest.betonquest.api.identifier.ActionIdentifier;
import org.betonquest.betonquest.api.identifier.ConditionIdentifier;
import org.betonquest.betonquest.api.instruction.Instruction;
import org.betonquest.betonquest.api.profile.Profile;
import org.betonquest.betonquest.api.quest.FeatureTypeRegistry;
import org.betonquest.betonquest.api.quest.TypeFactory;
import org.betonquest.betonquest.kernel.processor.adapter.ActionAdapter;
import org.betonquest.betonquest.kernel.registry.feature.InterceptorRegistry;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;

import com.notquests.paper.NotQuests;
import com.notquests.paper.PaperPlayer;
import com.notquests.paper.integrations.betonquest.conditions.BQConditionsCondition;
import com.notquests.paper.integrations.betonquest.conversationInterceptors.NotQuestsInterceptorFactory;
import com.notquests.paper.integrations.betonquest.events.BQAbortQuestEvent;
import com.notquests.paper.integrations.betonquest.events.BQActionEvent;
import com.notquests.paper.integrations.betonquest.events.BQFailQuestEvent;
import com.notquests.paper.integrations.betonquest.events.BQQuestPointsEvent;
import com.notquests.paper.integrations.betonquest.events.BQStartQuestEvent;
import com.notquests.paper.integrations.betonquest.events.BQTriggerObjectiveEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

public class BetonQuestIntegration {
  private final NotQuests main;
  private BetonQuestApi api;
  private boolean registered = false;

  public BetonQuestIntegration(final NotQuests main) {
    this.main = main;
  }

  public boolean enable() {
    try {
      final Optional<BetonQuestApiService> apiService = BetonQuestApiService.get();
      register(
          apiService
              .map(provider -> provider.api(main.getMain()))
              .orElseGet(() -> BetonQuest.getInstance().getBetonQuestApi()));
      return registered;
    } catch (final QuestException | RuntimeException exception) {
      main.getCorePlugin().betonQuestIntegrationActivationFailed(exception);
      return false;
    }
  }

  private void register(final BetonQuestApi api) throws QuestException {
    this.api = api;

    api.actions().registry().register("nq_action", new BQActionEvent(main));
    api.actions().registry().register("nq_triggerobjective", new BQTriggerObjectiveEvent(main));
    api.actions().registry().register("nq_startquest", new BQStartQuestEvent(main));
    api.actions().registry().register("nq_failquest", new BQFailQuestEvent(main));
    api.actions().registry().register("nq_abortquest", new BQAbortQuestEvent(main));
    api.actions().registry().register("nq_questpoints", new BQQuestPointsEvent(main));
    api.conditions().registry().register("nq_condition", new BQConditionsCondition(main));

    if (registerInterceptor()) {
      main.getCorePlugin().betonQuestInterceptorRegistered();
    }
    registered = true;
  }

  private boolean registerInterceptor() {
    final Optional<InterceptorRegistry> registry = BetonQuest.getInstance()
        .getComponentLoader()
        .getOptional(InterceptorRegistry.class);
    if (registry.isEmpty()) {
      return false;
    }
    registry.get().register("notquests", new NotQuestsInterceptorFactory(main));
    return true;
  }

  public boolean isRegistered() {
    return registered;
  }

  public BetonQuestApi api() throws QuestException {
    if (api == null || !registered) {
      throw new QuestException("BetonQuest support is not enabled.");
    }
    return api;
  }

  public List<String> packageNames() {
    try {
      return new ArrayList<>(api().packages().getPackages().keySet());
    } catch (final QuestException exception) {
      return List.of();
    }
  }

  public List<String> conditionNames(final String packageName) {
    return configKeys(packageName, "conditions");
  }

  public List<String> allConditionNames() {
    final LinkedHashSet<String> names = new LinkedHashSet<>();
    for (final String packageName : packageNames()) {
      names.addAll(conditionNames(packageName));
    }
    return List.copyOf(names);
  }

  private List<String> configKeys(final String packageName, final String... sections) {
    try {
      final QuestPackage questPackage = questPackage(packageName);
      for (final String sectionName : sections) {
        final ConfigurationSection section =
            questPackage.getConfig().getConfigurationSection(sectionName);
        if (section != null) {
          return new ArrayList<>(section.getKeys(false));
        }
      }
    } catch (final QuestException ignored) {
      // Missing package/section means no completions.
    }
    return List.of();
  }

  public QuestPackage questPackage(final String packageName) throws QuestException {
    final QuestPackage questPackage = api().packages().getPackage(packageName);
    if (questPackage == null) {
      throw new QuestException("BetonQuest package '" + packageName + "' does not exist.");
    }
    return questPackage;
  }

  public ActionIdentifier actionIdentifier(final String packageName, final String actionName)
      throws QuestException {
    return api()
        .identifiers()
        .getFactory(ActionIdentifier.class)
        .parseIdentifier(questPackage(packageName), actionName);
  }

  public ConditionIdentifier conditionIdentifier(final String packageName, final String conditionName)
      throws QuestException {
    return api()
        .identifiers()
        .getFactory(ConditionIdentifier.class)
        .parseIdentifier(questPackage(packageName), conditionName);
  }

  public Profile profileFor(final PaperPlayer questPlayer) throws QuestException {
    final OfflinePlayer player =
        questPlayer.getPlayer() != null
            ? questPlayer.getPlayer()
            : Bukkit.getOfflinePlayer(questPlayer.getUniqueId());
    return api().profiles().getProfile(player);
  }

  public void runAction(final PaperPlayer questPlayer, final String packageName, final String actionName)
      throws QuestException {
    api().actions().manager().run(profileFor(questPlayer), actionIdentifier(packageName, actionName));
  }

  public boolean testCondition(
      final PaperPlayer questPlayer, final String packageName, final String conditionName)
      throws QuestException {
    return questPlayer != null
        && api()
            .conditions()
            .manager()
            .test(profileFor(questPlayer), conditionIdentifier(packageName, conditionName));
  }

  @SuppressWarnings("unchecked")
  public void runInlineAction(final PaperPlayer questPlayer, final String actionInstruction)
      throws QuestException {
    final String trimmed = actionInstruction.trim();
    final String type = trimmed.contains(" ") ? trimmed.substring(0, trimmed.indexOf(' ')) : trimmed;
    final String rest = trimmed.length() == type.length() ? "" : trimmed.substring(type.length()).trim();
    final QuestPackage questPackage =
        api().packages().getPackages().values().stream()
            .findFirst()
            .orElseThrow();
    final Instruction instruction = api().instructions().create(questPackage, rest);
    final FeatureTypeRegistry<ActionAdapter> registry =
        (FeatureTypeRegistry<ActionAdapter>) api().actions().registry();
    final TypeFactory<ActionAdapter> factory = registry.getFactory(type);
    factory.parseInstruction(instruction).fire(profileFor(questPlayer));
  }
}
