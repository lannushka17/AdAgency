package com.sab.adagency.persistence.entity;

/**
 * Enumeration of cities where billboards are placed.
 */
public enum City {
    KYIV("Київ"),
    LVIV("Львів"),
    ODESA("Одеса"),
    KHARKIV("Харків"),
    DNIPRO("Дніпро"),
    VINNYTSIA("Вінниця");

    private final String displayName;

    City(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Converts string to City enum value.
     * @param option string representation
     * @return corresponding City
     */
    public static City fromString(String option) {
        for (City city : City.values()) {
            if (city.name().equalsIgnoreCase(option) || city.displayName.equals(option)) {
                return city;
            }
        }
        throw new IllegalArgumentException("Unknown city: " + option);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
