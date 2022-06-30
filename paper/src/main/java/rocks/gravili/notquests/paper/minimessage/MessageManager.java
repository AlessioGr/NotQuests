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

package rocks.gravili.notquests.paper.minimessage;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import rocks.gravili.notquests.paper.NotQuests;

public class MessageManager {
  private final NotQuests main;
  private final MiniMessage miniMessage;
  private final TagResolver tagResolver;

  public MessageManager(final NotQuests main) {
    this.main = main;

    final TagResolver mainGradient = TagResolver.resolver("main", SimpleGradientTag::main);
    final TagResolver highlight = TagResolver.resolver("highlight", SimpleGradientTag::highlight);
    final TagResolver highlight2 =
        TagResolver.resolver("highlight2", SimpleGradientTag::highlight2);
    final TagResolver error = TagResolver.resolver("error", SimpleGradientTag::error);
    final TagResolver success = TagResolver.resolver("success", SimpleGradientTag::success);
    final TagResolver unimportant =
        TagResolver.resolver("unimportant", SimpleGradientTag::unimportant);
    final TagResolver warn = TagResolver.resolver("warn", SimpleGradientTag::warn);
    final TagResolver veryUnimportant =
        TagResolver.resolver("veryunimportant", SimpleGradientTag::veryUnimportant);
    final TagResolver negative = TagResolver.resolver("negative", SimpleGradientTag::negative);
    final TagResolver positive = TagResolver.resolver("positive", SimpleGradientTag::positive);

    final TagResolver tagResolver =
        TagResolver.builder()
            .resolvers(
                TagResolver.standard(),
                mainGradient,
                highlight,
                highlight2,
                error,
                success,
                unimportant,
                warn,
                veryUnimportant,
                negative,
                positive)
            .build();

    this.tagResolver = tagResolver;
    miniMessage = MiniMessage.builder().tags(tagResolver).build();
  }

  public final MiniMessage getMiniMessage() {
    return miniMessage;
  }

  public final TagResolver getTagResolver() {
    return tagResolver;
  }
}
