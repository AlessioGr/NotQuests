package com.notquests.paper.events.hooks;

import com.palmergames.bukkit.towny.event.NationAddTownEvent;
import com.palmergames.bukkit.towny.event.NationRemoveTownEvent;
import com.palmergames.bukkit.towny.event.TownAddResidentEvent;
import com.palmergames.bukkit.towny.event.TownRemoveResidentEvent;
import com.palmergames.bukkit.towny.object.Resident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.notquests.paper.NotQuests;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class TownyEvents implements Listener {
  private final NotQuests main;

  public TownyEvents(NotQuests main) {
    this.main = main;
  }

  @EventHandler
  public void onTownAddToNation(NationAddTownEvent e) {
    main.getCorePlugin().townAddedToNation(residentIdentifiers(e.getNation().getResidents()));
  }

  @EventHandler
  public void onTownRemoveFromNation(NationRemoveTownEvent e) {
    main.getCorePlugin().townRemovedFromNation(residentIdentifiers(e.getNation().getResidents()));
  }

  @EventHandler
  public void onResidentAdd(TownAddResidentEvent e) {
    main.getCorePlugin().townResidentAdded(residentIdentifiers(e.getTown().getResidents()));
  }

  @EventHandler
  public void onResidentRemove(TownRemoveResidentEvent e) {
    main.getCorePlugin().townResidentRemoved(residentIdentifiers(e.getTown().getResidents()));
  }

  private static List<String> residentIdentifiers(
      final Collection<Resident> residents) {
    return residents.stream()
        .filter(Objects::nonNull)
        .map(Resident::getUUID)
        .filter(Objects::nonNull)
        .map(UUID::toString)
        .toList();
  }
}
