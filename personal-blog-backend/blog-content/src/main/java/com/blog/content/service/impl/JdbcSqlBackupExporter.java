package com.blog.content.service.impl;

import com.blog.content.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Slf4j
public final class JdbcSqlBackupExporter {

    private static final int INSERT_BATCH_SIZE = 200;

    private JdbcSqlBackupExporter() {
    }

    public static byte[] export(DataSource dataSource, String database) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(1024 * 1024);
        try (Connection conn = dataSource.getConnection();
             Writer out = new OutputStreamWriter(buffer, StandardCharsets.UTF_8)) {
            conn.setAutoCommit(false);
            try (Statement tx = conn.createStatement()) {
                tx.execute("START TRANSACTION READ ONLY");
            }
            writeLine(out, "-- personal-blog jdbc backup");
            writeLine(out, "-- database: " + database);
            writeLine(out, "-- generated: " + LocalDateTime.now());
            writeLine(out, "SET NAMES utf8mb4;");
            writeLine(out, "SET FOREIGN_KEY_CHECKS=0;");
            writeLine(out, "SET SQL_MODE='NO_AUTO_VALUE_ON_ZERO';");
            writeLine(out, "USE `" + escapeIdentifier(database) + "`;");

            List<String> tables = listTables(conn, database);
            for (String table : tables) {
                exportTable(conn, out, database, table);
            }

            writeLine(out, "SET FOREIGN_KEY_CHECKS=1;");
            conn.commit();
            out.flush();
            return buffer.toByteArray();
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException(500, "JDBC 备份失败: " + e.getMessage());
        }
    }

    private static List<String> listTables(Connection conn, String database) throws SQLException {
        List<String> tables = new ArrayList<>();
        String sql = """
                SELECT TABLE_NAME
                FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = ?
                  AND TABLE_TYPE = 'BASE TABLE'
                ORDER BY TABLE_NAME
                """;
        try (var ps = conn.prepareStatement(sql)) {
            ps.setString(1, database);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }
            }
        }
        return tables;
    }

    private static void exportTable(Connection conn, Writer out, String database, String table) throws Exception {
        writeLine(out, "");
        writeLine(out, "-- table: " + table);
        writeLine(out, "DROP TABLE IF EXISTS `" + escapeIdentifier(table) + "`;");
        writeLine(out, loadCreateTable(conn, database, table) + ";");

        String selectSql = "SELECT * FROM `" + escapeIdentifier(database) + "`.`" + escapeIdentifier(table) + "`";
        try (Statement stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            stmt.setFetchSize(Integer.MIN_VALUE);
            try (ResultSet rs = stmt.executeQuery(selectSql)) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                List<String> columns = new ArrayList<>(columnCount);
                for (int i = 1; i <= columnCount; i++) {
                    columns.add("`" + escapeIdentifier(meta.getColumnName(i)) + "`");
                }
                String columnList = String.join(", ", columns);
                List<String> batch = new ArrayList<>(INSERT_BATCH_SIZE);
                while (rs.next()) {
                    batch.add(buildRowValues(rs, meta, columnCount));
                    if (batch.size() >= INSERT_BATCH_SIZE) {
                        writeInsert(out, table, columnList, batch);
                        batch.clear();
                    }
                }
                if (!batch.isEmpty()) {
                    writeInsert(out, table, columnList, batch);
                }
            }
        }
    }

    private static String loadCreateTable(Connection conn, String database, String table) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE `" + escapeIdentifier(database)
                     + "`.`" + escapeIdentifier(table) + "`")) {
            if (!rs.next()) {
                throw new ServiceException(500, "无法读取表结构: " + table);
            }
            return rs.getString(2);
        }
    }

    private static void writeInsert(Writer out, String table, String columnList, List<String> rows) throws Exception {
        writeLine(out, "INSERT INTO `" + escapeIdentifier(table) + "` (" + columnList + ") VALUES");
        for (int i = 0; i < rows.size(); i++) {
            out.write("(");
            out.write(rows.get(i));
            out.write(")");
            out.write(i < rows.size() - 1 ? ",\n" : ";\n");
        }
    }

    private static String buildRowValues(ResultSet rs, ResultSetMetaData meta, int columnCount) throws SQLException {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= columnCount; i++) {
            if (i > 1) {
                sb.append(", ");
            }
            sb.append(formatValue(rs, meta, i));
        }
        return sb.toString();
    }

    private static String formatValue(ResultSet rs, ResultSetMetaData meta, int index) throws SQLException {
        Object value = rs.getObject(index);
        if (value == null || rs.wasNull()) {
            return "NULL";
        }
        int type = meta.getColumnType(index);
        return switch (type) {
            case Types.TINYINT, Types.SMALLINT, Types.INTEGER, Types.BIGINT,
                 Types.FLOAT, Types.REAL, Types.DOUBLE, Types.NUMERIC, Types.DECIMAL -> value.toString();
            case Types.BIT, Types.BOOLEAN -> {
                if (value instanceof Boolean bool) {
                    yield bool ? "1" : "0";
                }
                yield value.toString();
            }
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> {
                byte[] bytes = rs.getBytes(index);
                yield bytes == null ? "NULL" : "0x" + HexFormat.of().formatHex(bytes);
            }
            default -> "'" + escapeSqlString(String.valueOf(value)) + "'";
        };
    }

    private static String escapeSqlString(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\u0000", "\\0")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("'", "\\'")
                .replace("\"", "\\\"");
    }

    private static String escapeIdentifier(String name) {
        return name.replace("`", "``");
    }

    private static void writeLine(Writer out, String line) throws Exception {
        out.write(line);
        out.write('\n');
    }
}
