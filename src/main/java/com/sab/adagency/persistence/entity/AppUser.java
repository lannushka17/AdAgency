package com.sab.adagency.persistence.entity;

import lombok.Builder;
import lombok.Data;

/** Користувач інформаційної системи AdAgency. */
@Data
@Builder(toBuilder = true)
public class AppUser {
    int id;
    String username;
    String passwordHash;
    UserRole role;
    String fullName;
}
