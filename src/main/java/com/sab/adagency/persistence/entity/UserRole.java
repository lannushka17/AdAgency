package com.sab.adagency.persistence.entity;

/** Ролі користувачів у системі AdAgency. */
public enum UserRole {
    ADMIN("Адміністратор"),
    MANAGER("Менеджер з продажу"),
    SALES("Менеджер з клієнтів");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
