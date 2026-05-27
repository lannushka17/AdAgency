package com.sab.adagency.persistence.contract;

import com.sab.adagency.persistence.entity.Billboard;
import com.sab.adagency.persistence.entity.City;
import java.util.List;
import java.util.Optional;

/**
 * Repository for working with billboards.
 */
public interface BillboardRepository {
    /**
     * Finds a billboard by id.
     * @param id unique billboard identifier
     * @return Optional with Billboard if found, otherwise empty
     */
    Optional<Billboard> findOneById(int id);

    /**
     * Retrieves all billboards.
     * @return list of all billboards
     */
    List<Billboard> findAll();

    /**
     * Creates or updates a billboard.
     * @param billboard entity to persist
     * @return persisted entity (with id)
     */
    Billboard save(Billboard billboard);

    /**
     * Deletes a billboard by id.
     * @param id unique billboard identifier
     * @return true if a row was deleted
     */
    boolean deleteById(int id);

    /**
     * Finds billboards located in a particular city.
     * @param city city to filter by
     * @return list of billboards in the city
     */
    List<Billboard> findByCity(City city);

    /**
     * Finds billboards by availability flag.
     * @param available availability flag
     * @return list of billboards with specified availability
     */
    List<Billboard> findByAvailable(boolean available);

    /**
     * Finds billboards whose code or address contains the search query.
     * @param query partial search string
     * @return list of matching billboards
     */
    List<Billboard> searchByCodeOrAddress(String query);
}
