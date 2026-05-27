package com.sab.adagency.persistence.utility;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Локальна БД SQLite {@code adagency.db}: білборди, клієнти, договори оренди, користувачі.
 */
public final class PersistenceConfig {

    private static final HikariDataSource DS;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + resolveDbPath());
        config.addDataSourceProperty("cachePrepStmts", "true");
        DS = new HikariDataSource(config);
    }

    private PersistenceConfig() {}

    private static String resolveDbPath() {
        String[] candidates = {
            "./resources/adagency.db", "src/main/resources/adagency.db", "./adagency.db"
        };
        for (String path : candidates) {
            File f = new File(path);
            File parent = f.getParentFile();
            if (parent != null && parent.exists()) {
                return path;
            }
        }
        return "./adagency.db";
    }

    public static Connection getConnection() throws SQLException {
        return DS.getConnection();
    }

    public static void initSchema() {
        try (Connection con = getConnection();
                Statement st = con.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");

            st.execute(
                    "CREATE TABLE IF NOT EXISTS app_users ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "username TEXT NOT NULL UNIQUE,"
                            + "password_hash TEXT NOT NULL,"
                            + "role TEXT NOT NULL,"
                            + "full_name TEXT)");

            st.execute(
                    "CREATE TABLE IF NOT EXISTS billboards ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "code TEXT NOT NULL UNIQUE,"
                            + "address TEXT NOT NULL,"
                            + "city TEXT NOT NULL,"
                            + "size TEXT NOT NULL,"
                            + "available INTEGER NOT NULL DEFAULT 1)");

            st.execute(
                    "CREATE TABLE IF NOT EXISTS clients ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "name TEXT NOT NULL,"
                            + "phone TEXT NOT NULL,"
                            + "email TEXT,"
                            + "vip INTEGER NOT NULL DEFAULT 0)");

            st.execute(
                    "CREATE TABLE IF NOT EXISTS rental_contracts ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + "billboard_id INTEGER NOT NULL,"
                            + "client_id INTEGER NOT NULL,"
                            + "start_date TEXT NOT NULL,"
                            + "end_date TEXT NOT NULL,"
                            + "total_price REAL NOT NULL,"
                            + "paid INTEGER NOT NULL DEFAULT 0,"
                            + "FOREIGN KEY(billboard_id) REFERENCES billboards(id),"
                            + "FOREIGN KEY(client_id) REFERENCES clients(id))");

            st.execute("CREATE INDEX IF NOT EXISTS idx_billboards_city ON billboards(city)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_contracts_billboard ON rental_contracts(billboard_id)");
            st.execute("CREATE INDEX IF NOT EXISTS idx_contracts_client ON rental_contracts(client_id)");

            seedIfEmpty(con);
            seedDefaultAdminIfMissing(con);
        } catch (SQLException e) {
            throw new RuntimeException("Не вдалося ініціалізувати базу даних adagency", e);
        }
    }

    private static void seedDefaultAdminIfMissing(Connection con) throws SQLException {
        try (PreparedStatement chk = con.prepareStatement(
                "SELECT 1 FROM app_users WHERE lower(username)=lower(?)")) {
            chk.setString(1, "admin");
            try (var rs = chk.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }
        try (PreparedStatement ps = con.prepareStatement(
                "INSERT INTO app_users(username, password_hash, role, full_name) VALUES(?,?,?,?)")) {
            ps.setString(1, "admin");
            ps.setString(2, PasswordUtil.sha256("admin123"));
            ps.setString(3, "ADMIN");
            ps.setString(4, "Адміністратор системи");
            ps.executeUpdate();
        }
    }

    private static void seedIfEmpty(Connection con) throws SQLException {
        try (var chk = con.createStatement();
                var rs = chk.executeQuery("SELECT COUNT(*) FROM billboards")) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }

        try (Statement st = con.createStatement()) {
            st.executeUpdate(
                    "INSERT INTO billboards(code, address, city, size, available) "
                            + "VALUES ('BB-KY-01', 'вул. Хрещатик, 22', 'KYIV', 'STANDARD_6X3', 1)");
            st.executeUpdate(
                    "INSERT INTO billboards(code, address, city, size, available) "
                            + "VALUES ('BB-LV-02', 'просп. Свободи, 45', 'LVIV', 'LARGE_12X4', 1)");
            st.executeUpdate(
                    "INSERT INTO billboards(code, address, city, size, available) "
                            + "VALUES ('BB-OD-03', 'вул. Дерибасівська, 10', 'ODESA', 'STANDARD_6X3', 0)");

            st.executeUpdate(
                    "INSERT INTO clients(name, phone, email, vip) "
                            + "VALUES ('ТОВ «Світло Реклами»', '+380501112233', 'info@svitlo.ua', 1)");
            st.executeUpdate(
                    "INSERT INTO clients(name, phone, email, vip) "
                            + "VALUES ('ПП «Медіа Плюс»', '+380671234567', 'contact@mediaplus.ua', 0)");

            st.executeUpdate(
                    "INSERT INTO rental_contracts(billboard_id, client_id, start_date, end_date, total_price, paid) "
                            + "VALUES (3, 1, '2026-05-01', '2026-08-31', 125000.0, 1)");
            st.executeUpdate(
                    "INSERT INTO rental_contracts(billboard_id, client_id, start_date, end_date, total_price, paid) "
                            + "VALUES (1, 2, '2026-06-15', '2026-09-15', 78000.0, 0)");
        }
    }
}
