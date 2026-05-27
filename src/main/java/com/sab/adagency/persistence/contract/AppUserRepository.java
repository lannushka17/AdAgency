package com.sab.adagency.persistence.contract;

import com.sab.adagency.persistence.entity.AppUser;
import java.util.List;
import java.util.Optional;

public interface AppUserRepository {
    Optional<AppUser> findById(int id);

    Optional<AppUser> findByUsername(String username);

    List<AppUser> findAll();

    AppUser save(AppUser user);

    boolean deleteById(int id);
}
