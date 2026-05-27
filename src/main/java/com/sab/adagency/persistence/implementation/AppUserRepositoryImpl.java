package com.sab.adagency.persistence.implementation;

import com.sab.adagency.persistence.contract.AppUserRepository;
import com.sab.adagency.persistence.entity.AppUser;
import com.sab.adagency.persistence.entity.UserRole;
import com.sab.adagency.persistence.utility.PersistenceConfig;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AppUserRepositoryImpl implements AppUserRepository {

    private static final String SQL_FIND_BY_ID = "SELECT * FROM app_users WHERE id=?";
    private static final String SQL_FIND_BY_USERNAME = "SELECT * FROM app_users WHERE lower(username)=lower(?)";
    private static final String SQL_FIND_ALL = "SELECT * FROM app_users ORDER BY username";
    private static final String SQL_INSERT =
            "INSERT INTO app_users(username, password_hash, role, full_name) VALUES (?,?,?,?)";
    private static final String SQL_UPDATE =
            "UPDATE app_users SET username=?, password_hash=?, role=?, full_name=? WHERE id=?";
    private static final String SQL_DELETE = "DELETE FROM app_users WHERE id=?";

    @Override
    public Optional<AppUser> findById(int id) {
        try (Connection con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(SQL_FIND_BY_ID)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<AppUser> findByUsername(String username) {
        try (Connection con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(SQL_FIND_BY_USERNAME)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<AppUser> findAll() {
        List<AppUser> list = new ArrayList<>();
        try (Connection con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(SQL_FIND_ALL);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public AppUser save(AppUser user) {
        return user.getId() == 0 ? insert(user) : update(user);
    }

    @Override
    public boolean deleteById(int id) {
        try (Connection con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(SQL_DELETE)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private AppUser insert(AppUser user) {
        try (Connection con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole().name());
            ps.setString(4, user.getFullName());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return user.toBuilder().id(keys.getInt(1)).build();
                }
                throw new RuntimeException("Не отримано згенерований ID користувача");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private AppUser update(AppUser user) {
        try (Connection con = PersistenceConfig.getConnection();
                PreparedStatement ps = con.prepareStatement(SQL_UPDATE)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getRole().name());
            ps.setString(4, user.getFullName());
            ps.setInt(5, user.getId());
            ps.executeUpdate();
            return user;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private AppUser map(ResultSet rs) throws SQLException {
        return AppUser.builder()
                .id(rs.getInt("id"))
                .username(rs.getString("username"))
                .passwordHash(rs.getString("password_hash"))
                .role(UserRole.valueOf(rs.getString("role")))
                .fullName(rs.getString("full_name"))
                .build();
    }
}
