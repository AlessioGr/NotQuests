package com.notquests.neoforge;

import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

final class NeoForgeArgumentTypes {
    private static final DeferredRegister<ArgumentTypeInfo<?, ?>> COMMAND_ARGUMENT_TYPES =
            DeferredRegister.create(Registries.COMMAND_ARGUMENT_TYPE, NotQuestsNeoForge.MOD_ID);
    @SuppressWarnings("unused")
    private static final DeferredHolder<ArgumentTypeInfo<?, ?>, ?> WHITESPACE_STRING =
            COMMAND_ARGUMENT_TYPES.register("whitespace_string", () -> ArgumentTypeInfos.registerByClass(
                    NeoForgeArguments.WhitespaceTerminatedStringArgument.class,
                    SingletonArgumentInfo.contextFree(NeoForgeArguments::commaToken)));
    private NeoForgeArgumentTypes() {}

    static void register(final IEventBus modBus) {
        COMMAND_ARGUMENT_TYPES.register(modBus);
    }
}
