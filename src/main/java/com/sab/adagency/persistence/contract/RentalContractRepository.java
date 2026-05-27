package com.sab.adagency.persistence.contract;

import com.sab.adagency.persistence.entity.RentalContract;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for working with billboard rental contracts.
 */
public interface RentalContractRepository {
    /**
     * Finds a contract by id.
     * @param id unique contract identifier
     * @return Optional with RentalContract if found, otherwise empty
     */
    Optional<RentalContract> findOneById(int id);

    /**
     * Retrieves all contracts.
     * @return list of all contracts
     */
    List<RentalContract> findAll();

    /**
     * Creates or updates a contract.
     * @param rentalContract entity to persist
     * @return persisted entity (with id)
     */
    RentalContract save(RentalContract rentalContract);

    /**
     * Deletes a contract by id.
     * @param id unique contract identifier
     * @return true if a row was deleted
     */
    boolean deleteById(int id);

    /**
     * Finds all contracts associated with a specific billboard.
     * @param billboardId billboard identifier
     * @return list of contracts for the billboard
     */
    List<RentalContract> findByBillboardId(int billboardId);

    /**
     * Finds all contracts of a specific client.
     * @param clientId client identifier
     * @return list of contracts for the client
     */
    List<RentalContract> findByClientId(int clientId);

    /**
     * Finds all contracts whose date range overlaps the supplied [start, end] interval.
     * @param billboardId billboard identifier
     * @param startDate start of interval
     * @param endDate end of interval
     * @param excludeId contract id to exclude (for update operations); use 0 to include all
     * @return list of overlapping contracts
     */
    List<RentalContract> findOverlapping(int billboardId, LocalDate startDate, LocalDate endDate, int excludeId);

    /**
     * Returns true if there is at least one contract for the billboard whose date range
     * intersects with the given range (excluding the contract with the supplied id).
     * @param billboardId billboard identifier
     * @param startDate start of interval
     * @param endDate end of interval
     * @param excludeId contract id to exclude (use 0 for none)
     * @return true if an overlap exists
     */
    boolean existsOverlap(int billboardId, LocalDate startDate, LocalDate endDate, int excludeId);

    /**
     * Calculates total revenue for paid contracts.
     * @return total revenue amount
     */
    double calculatePaidRevenue();
}
