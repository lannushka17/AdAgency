package com.sab.adagency.persistence.entity;

import lombok.Builder;
import lombok.Data;

/**
 * Client (advertiser) entity for the advertising agency.
 */
@Data
@Builder(toBuilder = true)
public class Client {
    /**
     * Unique client identifier
     */
    int id;

    /**
     * Company or person name
     */
    String name;

    /**
     * Contact phone number
     */
    String phone;

    /**
     * Contact e-mail address
     */
    String email;

    /**
     * Whether the client has VIP status (gets discount)
     */
    @Builder.Default
    boolean vip = false;

    /**
     * Returns formatted string representation.
     * @return formatted client description
     */
    @Override
    public String toString() {
        return String.format("%s%s (%s)", vip ? "★ " : "", name, phone);
    }
}
