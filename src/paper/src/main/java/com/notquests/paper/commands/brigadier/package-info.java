/**
 * Paper adapter for NotQuests' core command tree. The platform-neutral command layouts,
 * handlers, messages, and exported metadata live in {@code com.notquests.core.commands}; this
 * package only compiles that tree to Paper's native Brigadier command API
 * ({@code io.papermc.paper.command.brigadier}) and registers it through Paper's
 * {@code LifecycleEvents.COMMANDS} lifecycle event.
 *
 * <p>This package exists so NotQuests can depend on first-party, maintained Paper APIs instead of the
 * removed Cloud dependency. The guiding principle is to keep command behavior in core, and keep only
 * Paper's native node registration and Adventure rendering here:
 *
 * <ul>
 *   <li>{@link com.notquests.paper.commands.brigadier.PaperCoreCommandCompiler} — compiler that
 *       turns core command registrations into Paper Brigadier nodes.</li>
 * </ul>
 */
package com.notquests.paper.commands.brigadier;
