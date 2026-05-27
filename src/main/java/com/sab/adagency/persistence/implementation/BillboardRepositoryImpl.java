package com.sab.adagency.persistence.implementation;

import com.sab.adagency.persistence.contract.BillboardRepository;
import com.sab.adagency.persistence.entity.Billboard;
import com.sab.adagency.persistence.entity.BillboardSize;
import com.sab.adagency.persistence.entity.City;
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
 * Implementation of BillboardRepository using SQLite storage.
 */
public class BillboardRepositoryImpl implements BillboardRepository {
    private static final String SQL_FIND_ONE = "SELECT * FROM billboards WHERE id=?";
    private static final String SQL_FIND_ALL = "SELECT * FROM billboards";
    private static final String SQL_INSERT = "INSERT INTO billboards(code, address, city, size, available) VALUES (?,?,?,?,?)";
    private static final String SQL_UPDATE = "UPDATE billboards SET code=?, address=?, city=?, size=?, available=? WHERE id=?";
    private static final String SQL_DELETE = "DELETE FROM billboards WHERE id=?";
    private static final String SQL_FIND_BY_CITY = "SELECT * FROM billboards WHERE city=?";
    private static final String SQL_FIND_BY_AVAILABLE = "SELECT * FROM billboards WHERE available=?";
    private static final String SQL_SEARCH = "SELECT * FROM billboards WHERE code LIKE ? OR address LIKE ?";

    @Override
    public Optional<Billboard> findOneById(int id) {
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
    public List<Billboard> findAll() {
        List<Billboard> list = new ArrayList<>();
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
    public Billboard save(Billboard billboard) {
        if (billboard.getId() == 0) {
            return insert(billboard);
        } else {
            return update(billboard);
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
    public List<Billboard> findByCity(City city) {
        List<Billboard> list = new ArrayList<>();
        try (Connection con = PersistenceConfig.getConnection()) {
            PreparedStatement ps = con.prepareStatement(SQL_FIND_BY_CITY);
            ps.setString(1, city.name());
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
    public List<Billboard> findByAvailable(boolean available) {
        List<Billboard> list = new ArrayList<>();
        try (Connection con = PersistenceConfig.getConnection()) {
            PreparedStatement ps = con.prepareStatement(SQL_FIND_BY_AVAILABLE);
            ps.setBoolean(1, available);
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
    public List<Billboard> searchByCodeOrAddress(String query) {
        List<Billboard> list = new ArrayList<>();
        try (Connection con = PersistenceConfig.getConnection()) {
            PreparedStatement ps = con.prepareStatement(SQL_SEARCH);
            String q = "%" + query + "%";
            ps.setString(1, q);
            ps.setString(2, q);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    private Billboard insert(Billboard billboard) {
        try (Connection con = PersistenceConfig.getConnection()) {
            PreparedStatement ps = con.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, billboard.getCode());
            ps.setString(2, billboard.getAddress());
            ps.setString(3, billboard.getCity().name());
            ps.setString(4, billboard.getSize().name());
            ps.setBoolean(5, billboard.isAvailable());
            ps.executeUpdate();

            ResultSet generatedKeys = ps.getGeneratedKeys();
            if (generatedKeys.next()) {
                return billboard.toBuilder().id(generatedKeys.getInt(1)).build();
            }
            throw new RuntimeException("Failed to get generated id");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Billboard update(Billboard billboard) {
        try (Connection con = PersistenceConfig.getConnection()) {
            PreparedStatement ps = con.prepareStatement(SQL_UPDATE);
            ps.setString(1, billboard.getCode());
            ps.setString(2, billboard.getAddress());
            ps.setString(3, billboard.getCity().name());
            ps.setString(4, billboard.getSize().name());
            ps.setBoolean(5, billboard.isAvailable());
            ps.setInt(6, billboard.getId());
            ps.executeUpdate();
            return billboard;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Billboard mapRow(ResultSet rs) throws SQLException {
        return Billboard.builder()
                .id(rs.getInt("id"))
                .code(rs.getString("code"))
                .address(rs.getString("address"))
                .city(City.valueOf(rs.getString("city")))
                .size(BillboardSize.valueOf(rs.getString("size")))
                .available(rs.getBoolean("available"))
                .build();
    }
}
