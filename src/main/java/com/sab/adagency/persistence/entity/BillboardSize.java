package com.sab.adagency.persistence.entity;

/**
 * Enumeration of billboard sizes/formats with daily rental rates.
 */
public enum BillboardSize {
    SMALL("Малий 3x6 м", 250.0),
    MEDIUM("Середній 6x12 м", 600.0),
    LARGE("Великий 8x16 м", 1100.0),
    DIGITAL("Цифровий екран", 1800.0),
    CITYLIGHT("Сітілайт 1.2x1.8 м", 180.0);

    private final String displayName;
    private final double dailyPrice;

    BillboardSize(String displayName, double dailyPrice) {
        this.displayName = displayName;
        this.dailyPrice = dailyPrice;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getDailyPrice() {
        return dailyPrice;
    }

    /**
     * Converts string to BillboardSize enum value.
     * @param option string representation
     * @return corresponding BillboardSize
     */
    public static BillboardSize fromString(String option) {
        for (BillboardSize size : BillboardSize.values()) {
            if (size.name().equalsIgnoreCase(option) || size.displayName.equals(option)) {
                return size;
            }
        }
        throw new IllegalArgumentException("Unknown billboard size: " + option);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
