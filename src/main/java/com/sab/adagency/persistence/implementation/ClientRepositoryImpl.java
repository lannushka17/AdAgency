package com.sab.adagency.persistence.implementation;

import com.sab.adagency.persistence.contract.ClientRepository;
import com.sab.adagency.persistence.entity.Client;
import com.sab.adagency.persistence.utility.PersistenceConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of ClientRepository using SQLite storage.
 */
public class ClientRepositoryImpl implements ClientRepository {
    private static final String SQL_FIND_ONE = "SELECT * FROM clients WHERE id=?";
    private static final String SQL_FIND_ALL = "SELECT * FROM clients";
    private static final String SQL_INSERT = "INSERT INTO clients(name, phone, email, vip) VALUES (?,?,?,?)";
    private static final String SQL_UPDATE = "UPDATE clients SET name=?, phone=?, email=?, vip=? WHERE id=?";
    private static final String SQL_DELETE = "DELETE FROM clients WHERE id=?";
    private static final String SQL_FIND_BY_NAME = "SELECT * FROM clients WHERE name LIKE ?";
    private static final String SQL_FIND_BY_VIP = "SELECT * FROM clients WHERE vip=?";

    @Override
    public Optional<Client> findOneById(int id) {
        try (Connection con = PersistenceConfig.getConnection()) {
            PreparedStatement ps = con.prepareStatement(SQL_FIND_ONE);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Client> findAll() {
        List<Client> list = new ArrayList<>();
        try (Connection con = PersistenceConfig.getConnection()) {
            PreparedStatement ps = con.prepareStatement(SQL_FIND_ALL);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public Client save(Client client) {
        if (client.getId() == 0) {
            return insert(client);
        } else {
            return update(client);
        }
    }

    @Override
    public boolean deleteById(int id) {
        try (Connection con = PersistenceConfig.getConnection()) {
            PreparedStatement ps = con.prepareStatement(SQL_DELETE);
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<Client> findByNameContaining(String name) {
        List<Client> list = new ArrayList<>();
        try (Connection con = PersistenceConfig.getConnection()) {
            PreparedStatement ps = con.prepareStatement(SQL_FIND_BY_NAME);
            ps.setString(1, "%" + name + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public List<Client> findByVip(boolean vip) {
        List<Client> list = new ArrayList<>();
        try (Connection con = PersistenceConfig.getConnection()) {
            PreparedStatement ps = con.prepareStatement(SQL_FIND_BY_VIP);
            ps.setBoolean(1, vip);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    private Client insert(Client client) {
        try (Connection con = PersistenceConfig.getConnection()) {
            PreparedStatement ps = con.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, client.getName());
            ps.setString(2, client.getPhone());
            ps.setString(3, client.getEmail());
            ps.setBoolean(4, client.isVip());
            ps.executeUpdate();

            ResultSet generatedKeys = ps.getGeneratedKeys();
            if (generatedKeys.next()) {
                return client.toBuilder().id(generatedKeys.getInt(1)).build();
            }
            throw new RuntimeException("Failed to get generated id");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Client update(Client client) {
        try (Connection con = PersistenceConfig.getConnection()) {
            PreparedStatement ps = con.prepareStatement(SQL_UPDATE);
            ps.setString(1, client.getName());
            ps.setString(2, client.getPhone());
            ps.setString(3, client.getEmail());
            ps.setBoolean(4, client.isVip());
            ps.setInt(5, client.getId());
            ps.executeUpdate();
            return client;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Client mapRow(ResultSet rs) throws SQLException {
        return Client.builder()
                .id(rs.getInt("id"))
                .name(rs.getString("name"))
                .phone(rs.getString("phone"))
                .email(rs.getString("email"))
                .vip(rs.getBoolean("vip"))
                .build();
    }
}
