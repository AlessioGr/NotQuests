/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.paper.managers.tags;

import java.util.Locale;
import org.jetbrains.annotations.NotNull;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;

public class Tag {
  private final NotQuests main;
  private final TagType tagType;
  private final String tagName;
  private Category category;

  public Tag(
      @NotNull final NotQuests main,
      @NotNull final String tagName,
      @NotNull final TagType tagType) {
    this.main = main;
    this.tagName = tagName.toLowerCase(Locale.ROOT);
    this.tagType = tagType;
    category = main.getDataManager().getDefaultCategory();
  }

  public final TagType getTagType() {
    return tagType;
  }

  public final String getTagName() {
    return tagName;
  }

  public final Category getCategory() {
    return category;
  }

  public void setCategory(final Category category) {
    this.category = category;
  }
}
