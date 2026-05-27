package com.sab.adagency.persistence.entity;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Data;

/**
 * Rental contract entity that links a client to a billboard for a specific date range.
 */
@Data
@Builder(toBuilder = true)
public class RentalContract {
    /**
     * Unique contract identifier
     */
    int id;

    /**
     * Billboard reference
     */
    int billboardId;

    /**
     * Client reference
     */
    int clientId;

    /**
     * Rental start date (inclusive)
     */
    LocalDate startDate;

    /**
     * Rental end date (inclusive)
     */
    LocalDate endDate;

    /**
     * Total contract amount
     */
    double totalPrice;

    /**
     * Whether the contract is fully paid
     */
    @Builder.Default
    boolean paid = false;

    /**
     * Returns the rental duration in days (inclusive).
     * @return number of rented days
     */
    public long getDurationDays() {
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    /**
     * Returns formatted text description of the contract.
     * @return formatted contract description
     */
    @Override
    public String toString() {
        return String.format("Договір #%d: %s - %s (%.2f грн) [%s]",
                id, startDate, endDate, totalPrice, paid ? "Оплачено" : "Не оплачено");
    }
}
