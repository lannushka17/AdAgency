package com.sab.adagency.persistence.contract;

import com.sab.adagency.persistence.entity.Client;
import java.util.List;
import java.util.Optional;

/**
 * Repository for working with clients.
 */
public interface ClientRepository {
    /**
     * Finds a client by id.
     * @param id unique client identifier
     * @return Optional with Client if found, otherwise empty
     */
    Optional<Client> findOneById(int id);

    /**
     * Retrieves all clients.
     * @return list of all clients
     */
    List<Client> findAll();

    /**
     * Creates or updates a client.
     * @param client entity to persist
     * @return persisted entity (with id)
     */
    Client save(Client client);

    /**
     * Deletes a client by id.
     * @param id unique client identifier
     * @return true if a row was deleted
     */
    boolean deleteById(int id);

    /**
     * Finds clients by partial name match (case-insensitive).
     * @param name name or part of name
     * @return list of clients matching the name
     */
    List<Client> findByNameContaining(String name);

    /**
     * Finds clients by VIP status.
     * @param vip whether the clients are VIP
     * @return list of clients with specified VIP status
     */
    List<Client> findByVip(boolean vip);
}
