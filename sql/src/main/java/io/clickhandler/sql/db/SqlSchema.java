package io.clickhandler.sql.db;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 *
 */
public class SqlSchema {
    private static final Logger LOG = LoggerFactory.getLogger(SqlSchema.class);

    public final boolean supportsBatchUpdates;
    public final boolean supportsGetGeneratedKeys;
    public final String databaseProductName;
    public final String databaseProductVersion;
    public final int databaseMajorVersion;
    public final int databaseMinorVersion;
    public final String identifierQuoteString;
    public final String sqlKeywords;
    public final String numericFunctions;
    public final String stringFunctions;
    public final String systemFunctions;
    public final String timeDateFunctions;
    public final String searchStringEscape;
    public final String extraNameCharacters;
    public final String driverVersion;
    public final long elapsedMs;
    public final Map<String, DbTable> tables = new LinkedHashMap<String, DbTable>();

    public SqlSchema(Connection connection, String catalog, String schema) throws SQLException {
        long start = System.currentTimeMillis();
        final DatabaseMetaData metaData = connection.getMetaData();

        supportsBatchUpdates = metaData.supportsBatchUpdates();
        supportsGetGeneratedKeys = metaData.supportsGetGeneratedKeys();
        databaseProductName = metaData.getDatabaseProductName();
        databaseProductVersion = metaData.getDatabaseProductVersion();
        driverVersion = metaData.getDriverVersion();
        databaseMajorVersion = metaData.getDatabaseMajorVersion();
        databaseMinorVersion = metaData.getDatabaseMinorVersion();
        identifierQuoteString = metaData.getIdentifierQuoteString();
        sqlKeywords = metaData.getSQLKeywords();
        numericFunctions = metaData.getNumericFunctions();
        stringFunctions = metaData.getStringFunctions();
        systemFunctions = metaData.getSystemFunctions();
        timeDateFunctions = metaData.getTimeDateFunctions();
        extraNameCharacters = metaData.getExtraNameCharacters();
        searchStringEscape = metaData.getSearchStringEscape();

        String[] tableTypes = new String[]{"TABLE"};

        // Schema pattern.
        catalog = Strings.nullToEmpty(catalog).trim().toLowerCase();
        schema = Strings.nullToEmpty(schema).trim().toLowerCase();
        String schemaPattern = schema.isEmpty() ? null : schema;
        if (databaseProductName.toLowerCase().contains("oracle")) {
            schemaPattern = "%"; // oracle expects a pattern such as "%" to work
        }

        try (final ResultSet rs = metaData.getTables(catalog.isEmpty() ? null : catalog, schemaPattern, null, tableTypes)) {
            while (rs.next()) {
                final DbTable table = new DbTable(
                        rs.getString(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getString(5)
                );
                if (!"INFORMATION_SCHEMA".equalsIgnoreCase(table.schema)) {
                    tables.put(table.name, table);
                }
            }
            rs.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String pattern = null;
        if (metaData.getDatabaseProductName().toLowerCase().contains("oracle")) {
            pattern = "%";
        }

        try (ResultSet rs = metaData.getColumns(null, pattern, pattern, null)) {
            while (rs.next()) {
                final String tableName = Strings.nullToEmpty(rs.getString(3)).trim().toLowerCase();

                DbTable table = tables.get(tableName);

                if (table == null) {
                    continue;
                }

                final String cat = Strings.nullToEmpty(rs.getString(1)).toLowerCase();
                final String sch = Strings.nullToEmpty(rs.getString(2)).toLowerCase();
                final String name = Strings.nullToEmpty(rs.getString(4)).trim().toLowerCase();

                final int dataType = rs.getInt(5);
                final String typeName = rs.getString(6);
                final int columnSize = rs.getInt(7);
                final int decimalDigits = rs.getInt(9);
                final boolean nullable = rs.getInt(11) != 0;
                final String remarks = rs.getString(12);
                final String defaultValue = rs.getString(13);
                final int ordinalPosition = rs.getInt(17);
                final DbColumn column = new DbColumn(
                        name,
                        cat,
                        sch,
                        tableName,
                        table,
                        dataType,
                        typeName,
                        columnSize,
                        decimalDigits,
                        nullable,
                        remarks,
                        defaultValue,
                        ordinalPosition
                );

                if (sch.equalsIgnoreCase("INFORMATION_SCHEMA")) {
                    continue;
                }

                table.columns.put(name, column);
            }

            rs.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        final Map<String, DbIndex> indexMap = new HashMap<>();

        for (DbTable table : tables.values()) {
            try (ResultSet rs = metaData.getIndexInfo(table.catalog, table.schema, table.name, false, false)) {
                while (rs.next()) {
                    final String cat = Strings.nullToEmpty(rs.getString(1)).trim().toLowerCase();
                    final String sch = Strings.nullToEmpty(rs.getString(2)).trim().toLowerCase();
                    final String tableName = Strings.nullToEmpty(rs.getString(3)).trim().toLowerCase();
                    final boolean unique = !rs.getBoolean(4);
                    final String qualifier = Strings.nullToEmpty(rs.getString(5)).trim().toLowerCase();
                    final String name = Strings.nullToEmpty(rs.getString(6)).trim().toLowerCase();
                    final short type = rs.getShort(7);
                    final short ordinalPosition = rs.getShort(8);
                    final String columnName = Strings.nullToEmpty(rs.getString(9)).trim().toLowerCase();
                    final String ascStr = Strings.nullToEmpty(rs.getString(10)).trim();
                    final Boolean asc = ascStr.isEmpty() ? null : ascStr.equalsIgnoreCase("A");
                    final long cardinality = rs.getInt(11);
                    final long pages = rs.getInt(12);

                    final DbColumn column = table.getColumn(columnName);

                    DbIndex index = indexMap.get(name);
                    if (index == null) {
                        index = new DbIndex(cat, sch, tableName, unique, qualifier, name, type, cardinality, pages);
                        indexMap.put(name, index);
                        table.indexes.put(name, index);

                        if (column != null) {
                            column.indexes.add(index);
                        }
                    }
                    final DbIndexColumn indexColumn = new DbIndexColumn(
                            column,
                            ordinalPosition,
                            columnName,
                            asc
                    );

                    if (column != null) {
                        column.indexColumns.add(indexColumn);
                    }

                    index.columns.add(indexColumn);
                }
                rs.close();
            } catch (Exception e) {
                LOG.error("Unexpected exception while running metaData.getIndexInfo", e);
            }
        }

        this.elapsedMs = System.currentTimeMillis() - start;

        LOG.warn("SqlSchema took " + elapsedMs + "ms to init.");
    }

    public static SqlSchema create(Connection connection, String catalog, String schema) throws SQLException {
        return new SqlSchema(connection, catalog, schema);
    }

    public DbTable getTable(String tableName) {
        return tables.get(Strings.nullToEmpty(tableName).trim().toLowerCase());
    }

    /**
     *
     */
    public static final class DbTable {
        public final String catalog;
        public final String schema;
        public final String name;
        public final String type;
        public final String remarks;
        public final Map<String, DbColumn> columns = new LinkedHashMap<>();
        public final Map<String, DbIndex> indexes = new LinkedHashMap<>();

        public DbTable(String catalog,
                       String schema,
                       String name,
                       String type,
                       String remarks) {
            this.catalog = Strings.nullToEmpty(catalog).trim().toLowerCase();
            this.schema = Strings.nullToEmpty(schema).trim().toLowerCase();
            this.name = Strings.nullToEmpty(name).trim().toLowerCase();
            this.type = type;
            this.remarks = remarks;
        }

        public DbColumn getColumn(String name) {
            return columns.get(name);
        }

        public DbColumn[] getColumns() {
            return columns.values().toArray(new DbColumn[columns.size()]);
        }
    }

    public static class DbColumn {
        public final String name;
        public final String catalog;
        public final String schema;
        public final String tableName;
        public final DbTable table;
        public final int dataType;
        public final String typeName;
        public final int columnSize;
        public final int decimalDigits;
        public final boolean nullable;
        public final String remarks;
        public final String defaultValue;
        public final int ordinalPosition;
        public final List<DbIndex> indexes = new ArrayList<>();
        public final List<DbIndexColumn> indexColumns = new ArrayList<>();

        public DbColumn(String name,
                        String catalog,
                        String schema,
                        String tableName,
                        DbTable table,
                        int dataType,
                        String typeName,
                        int columnSize,
                        int decimalDigits,
                        boolean nullable,
                        String remarks,
                        String defaultValue,
                        int ordinalPosition) {
            this.name = name;
            this.catalog = catalog;
            this.schema = schema;
            this.tableName = tableName;
            this.table = table;
            this.dataType = dataType;
            this.typeName = typeName;
            this.columnSize = columnSize;
            this.decimalDigits = decimalDigits;
            this.nullable = nullable;
            this.remarks = remarks;
            this.defaultValue = defaultValue;
            this.ordinalPosition = ordinalPosition;
        }
    }

    public static class DbIndex {
        public final String catalog;
        public final String schema;
        public final String tableName;
        public final boolean unique;
        public final String qualifier;
        public final String name;
        public final short type;
        public final long cardinality;
        public final long pages;

        public final List<DbIndexColumn> columns = new ArrayList<>();

        public DbIndex(String catalog,
                       String schema,
                       String tableName,
                       boolean unique,
                       String qualifier,
                       String name,
                       short type,
                       long cardinality,
                       long pages) {
            this.catalog = catalog;
            this.schema = schema;
            this.tableName = tableName;
            this.unique = unique;
            this.qualifier = qualifier;
            this.name = name;
            this.type = type;
            this.cardinality = cardinality;
            this.pages = pages;
        }
    }

    public static class DbIndexColumn {
        public final DbColumn column;
        public final short ordinalPosition;
        public final String columnName;
        public final Boolean asc;

        public DbIndexColumn(DbColumn column, short ordinalPosition, String columnName, Boolean asc) {
            this.column = column;
            this.ordinalPosition = ordinalPosition;
            this.columnName = columnName;
            this.asc = asc;
        }
    }
}
