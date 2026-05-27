package com.sab.adagency.persistence.entity;

import lombok.Builder;
import lombok.Data;

/**
 * Billboard entity representing a single advertising structure in a city.
 */
@Data
@Builder(toBuilder = true)
public class Billboard {
    /**
     * Unique billboard identifier
     */
    int id;

    /**
     * Internal billboard code, e.g. "KY-001"
     */
    String code;

    /**
     * Street address where the billboard is located
     */
    String address;

    /**
     * City where the billboard is placed
     */
    City city;

    /**
     * Format / size of the billboard
     */
    BillboardSize size;

    /**
     * Whether the billboard is currently available for rent
     */
    @Builder.Default
    boolean available = true;

    /**
     * Returns formatted text description.
     * @return formatted billboard description
     */
    @Override
    public String toString() {
        return String.format("[%s] %s, %s — %s",
                code, address, city.getDisplayName(), size.getDisplayName());
    }
}
