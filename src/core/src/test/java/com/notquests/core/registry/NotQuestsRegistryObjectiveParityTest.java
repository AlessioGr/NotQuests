package com.notquests.core.registry;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import com.notquests.core.registry.NotQuestsRegistry.Objectives;

import java.lang.reflect.Method;

class NotQuestsRegistryObjectiveParityTest {
    @Test
    void entityEventFactsMustBeImplementedByEveryPlatform() {
        for (final Method method : Objectives.EntityEvent.class.getDeclaredMethods()) {
            assertFalse(method.isDefault(), method.getName());
        }
    }
}
