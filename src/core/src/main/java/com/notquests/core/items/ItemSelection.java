package com.notquests.core.items;

import java.util.List;
import java.util.Map;

public interface ItemSelection {
    boolean includesMaterial(String materialId);

    String listedMaterials(String miniMessageTag);

    boolean any();

    List<String> materialIds();

    List<String> savedItemNames();

    /**
     * Lossless platform item payloads kept as YAML-safe maps. Core stores these maps but never
     * interprets their platform-specific data; the owning adapter turns them back into native
     * items when matching or rendering.
     */
    List<Map<String, Object>> exactItems();

    int amount();

    ItemSelection withAmount(int amount);
}
