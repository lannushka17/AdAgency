package com.sab.adagency.persistence.implementation;

import com.sab.adagency.persistence.contract.RentalContractRepository;
import com.sab.adagency.persistence.entity.RentalContract;
import com.sab.adagency.persistence.utility.PersistenceConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of RentalContractRepository using SQLite storage.
 */
public class RentalContractRepositoryImpl implements RentalContractRepository {
    private static final String SQL_FIND_ONE = "SELECT * FROM rental_contracts WHERE id=?";
    private static final String SQL_FIND_ALL = "SELECT * FROM rental_contracts";
    private static final String SQL_INSERT = "INSERT INTO rental_contracts(billboard_id, client_id, start_date, end_date, total_price, paid) VALUES (?,?,?,?,?,?)";
    private static final String SQL_UPDATE = "UPDATE rental_contracts SET billboard_id=?, client_id=?, start_date=?, end_date=?, total_price=?, paid=? WHERE id=?";
    private static final String SQL_DELETE = "DELETE FROM rental_contracts WHERE id=?";
    private static final String SQL_FIND_BY_BILLBOARD = "SELECT * FROM rental_contracts WHERE billboard_id=?";
    private static final String SQL_FIND_BY_CLIENT = "SELECT * FROM rental_contracts WHERE client_id=?";
    // Two ranges [a,b] and [c,d] overlap iff a <= d AND c <= b
    private static final String SQL_FIND_OVERLAPPING = "SELECT * FROM rental_contracts "
            + "WHERE billboard_id=? AND start_date <= ? AND end_date >= ? AND id != ?";
    private static final String SQL_EXISTS_OVERLAP = "SELECT COUNT(*) FROM rental_contracts "
            + "WHERE billboard_id=? AND start_date <= ? AND end_date >= ? AND id != ?";
    private static final String SQL_PAID_REVENUE = "SELECT COALESCE(SUM(total_price), 0) FROM rental_contracts WHERE paid=1";

    @Override
    public Optional<RentalContract> findOneById(int id) {
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
    public List<RentalContract> findAll() {
        List<RentalContract> list = new ArrayList<>();
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
    public RentalContract save(RentalContract rentalContract) {
        if (rentalContract.getId() == 0) {
            return insert(rentalContract);
        } else {
            return update(rentalContract);
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
    public List<RentalContract> findByBillboardId(int billboardId) {
        List<RentalContract> list = new ArrayList<>();
        try (Connection con = PersistenceConfig.getConnection()) {
            PreparedStatement ps = con.prepareStatement(SQL_FIND_BY_BILLBOARD);
            ps.setInt(1, billboardId);
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
    public List<RentalContract> findByClientId(int clientId) {
        List<RentalContract> list = new ArrayList<>();
        try (Connection con = PersistenceConfig.getConnection()) {
            PreparedStatement ps = con.prepareStatement(SQL_FIND_BY_CLIENT);
            ps.setInt(1, clientId);
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
    public List<RentalContract> findOverlapping(int billboardId, LocalDate startDate, LocalDate endDate, int excludeId) {
        List<RentalContract> list = new ArrayList<>();
        try (Connection con = PersistenceConfig.getConnection()) {
            PreparedStatement ps = con.prepareStatement(SQL_FIND_OVERLAPPING);
            ps.setInt(1, billboardId);
            ps.setString(2, endDate.toString());
            ps.setString(3, startDate.toString());
            ps.setInt(4, excludeId);
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
    public boolean existsOverlap(int billboardId, LocalDate startDate, LocalDate endDate, int excludeId) {
        try (Connection con = PersistenceConfig.getConnection()) {
            PreparedStatement ps = con.prepareStatement(SQL_EXISTS_OVERLAP);
            ps.setInt(1, billboardId);
            ps.setString(2, endDate.toString());
            ps.setString(3, startDate.toString());
            ps.setInt(4, excludeId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public double calculatePaidRevenue() {
        try (Connection con = PersistenceConfig.getConnection()) {
            PreparedStatement ps = con.prepareStatement(SQL_PAID_REVENUE);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getDouble(1);
            }
            return 0.0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private RentalContract insert(RentalContract contract) {
        try (Connection con = PersistenceConfig.getConnection()) {
            PreparedStatement ps = con.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, contract.getBillboardId());
            ps.setInt(2, contract.getClientId());
            ps.setString(3, contract.getStartDate().toString());
            ps.setString(4, contract.getEndDate().toString());
            ps.setDouble(5, contract.getTotalPrice());
            ps.setBoolean(6, contract.isPaid());
            ps.executeUpdate();

            ResultSet generatedKeys = ps.getGeneratedKeys();
            if (generatedKeys.next()) {
                return contract.toBuilder().id(generatedKeys.getInt(1)).build();
            }
            throw new RuntimeException("Failed to get generated id");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private RentalContract update(RentalContract contract) {
        try (Connection con = PersistenceConfig.getConnection()) {
            PreparedStatement ps = con.prepareStatement(SQL_UPDATE);
            ps.setInt(1, contract.getBillboardId());
            ps.setInt(2, contract.getClientId());
            ps.setString(3, contract.getStartDate().toString());
            ps.setString(4, contract.getEndDate().toString());
            ps.setDouble(5, contract.getTotalPrice());
            ps.setBoolean(6, contract.isPaid());
            ps.setInt(7, contract.getId());
            ps.executeUpdate();
            return contract;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private RentalContract mapRow(ResultSet rs) throws SQLException {
        return RentalContract.builder()
                .id(rs.getInt("id"))
                .billboardId(rs.getInt("billboard_id"))
                .clientId(rs.getInt("client_id"))
                .startDate(LocalDate.parse(rs.getString("start_date")))
                .endDate(LocalDate.parse(rs.getString("end_date")))
                .totalPrice(rs.getDouble("total_price"))
                .paid(rs.getBoolean("paid"))
                .build();
    }
}
