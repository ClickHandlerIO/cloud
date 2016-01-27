package io.clickhandler.sql.db;

import com.google.common.base.Preconditions;
import io.clickhandler.sql.evolution.ChangeType;
import org.jooq.DataType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Compares the Entity Schema against a SQL SqlDatabase Schema.
 * It generates Change Operations in order to synchronize
 * the differences in the Entity Schema to the SQL SqlDatabase.
 * Persist Schema is always defined using Entities.
 */
public class SchemaInspector {
    private final SqlPlatform platform;
    private final Map<Class, TableMapping> tableMappings;
    private final List<Change> changes = new ArrayList<>();

    private SchemaInspector(final SqlPlatform platform,
                            final Map<Class, TableMapping> tableMappings) {
        Preconditions.checkNotNull(platform, "dbPlatform must be set.");
        Preconditions.checkNotNull(tableMappings, "tableMappings must be set.");
        this.platform = platform;
        this.tableMappings = tableMappings;
    }

    public static List<Change> inspect(final SqlPlatform platform,
                                       final Map<Class, TableMapping> tableMappings) throws SQLException {
        return new SchemaInspector(platform, tableMappings).inspect();
    }

    private List<Change> inspect() {
        for (TableMapping mapping : tableMappings.values()) {
            checkTable(mapping);
        }
        return changes;
    }

    /**
     * @param mapping
     */
    private void checkTable(TableMapping mapping) {
        final Mapping.Property[] properties = mapping.getProperties();

        if (mapping.schemaTable == null) {
            changes.add(new CreateTable(mapping, false));
            changes.add(new CreatePrimaryKey(mapping, false));

            for (TableMapping.Index index : mapping.getIndexes()) {
                changes.add(new CreateIndex(mapping, index));
            }
        } else {
            for (Mapping.Property property : properties) {
                // Add new column.
                if (!property.isMapped()) {
                    changes.add(new AddColumn(mapping, property, false));
                } else {
                    checkField(mapping, property, property.column, false);
                }
            }

            for (SqlSchema.DbColumn column : mapping.schemaTable.getColumns()) {
                Mapping.Property property = mapping.getProperty(column.name);

                if (property == null) {
                    changes.add(new DropColumn(mapping, column, false));
                }
            }


            for (TableMapping.Index index : mapping.getIndexes()) {
                if (index.dbIndex == null) {
                    changes.add(new CreateIndex(mapping, index));
                } else {
                    if (index.dbIndex.columns.size() != index.properties.length) {
                        // Rebuild Index.
                        changes.add(new DropIndex(mapping, index.dbIndex));
                        changes.add(new CreateIndex(mapping, index));
                    } else {
                        boolean changed = false;
                        for (TableMapping.IndexProperty indexProperty : index.properties) {
                            if (indexProperty.dbIndexColumn == null) {
                                changed = true;
                                break;
                            }
                        }
                        if (changed) {
                            // Rebuild Index.
                            changes.add(new DropIndex(mapping, index.dbIndex));
                            changes.add(new CreateIndex(mapping, index));
                        }
                    }
                }
            }

            // Drop Extra Indexes.
//            for (SqlSchema.DbIndex dbIndex : mapping.schemaTable.indexes.values()) {
//                if (mapping.getIndex(dbIndex.name) == null) {
//                    changes.add(new DropIndex(mapping, dbIndex));
//                }
//            }
        }

        if (mapping.isJournaling()) {
            SqlSchema.DbTable journalTable = mapping.schemaJournalTable;

            if (journalTable == null) {
                changes.add(new CreateTable(mapping, true));
                changes.add(new CreatePrimaryKey(mapping, true));

                for (TableMapping.Index index : mapping.getJournalIndexes()) {
                    changes.add(new CreateIndex(mapping, index));
                }
            } else {
                for (Mapping.Property property : properties) {
                    boolean found = false;
                    for (SqlSchema.DbColumn column : journalTable.getColumns()) {
                        if (property.columnName.equals(column.name)) {
                            found = true;
                            checkField(mapping, property, column, true);
                            break;
                        }
                    }

                    if (!found) {
                        changes.add(new AddColumn(mapping, property, true));
                    }
                }

                for (SqlSchema.DbColumn column : journalTable.getColumns()) {
                    Mapping.Property property = mapping.getProperty(column.name);

                    if (property == null) {
                        changes.add(new DropColumn(mapping, column, true));
                    }
                }

                for (TableMapping.Index index : mapping.getJournalIndexes()) {
                    if (index.dbIndex == null) {
                        changes.add(new CreateIndex(mapping, index));
                    } else {
                        if (index.dbIndex.columns.size() != index.properties.length) {
                            // Rebuild Index.
                            changes.add(new DropIndex(mapping, index.dbIndex));
                            changes.add(new CreateIndex(mapping, index));
                        } else {
                            boolean changed = false;
                            for (TableMapping.IndexProperty indexProperty : index.properties) {
                                if (indexProperty.dbIndexColumn == null) {
                                    changed = true;
                                    break;
                                }
                            }
                            if (changed) {
                                // Rebuild Index.
                                changes.add(new DropIndex(mapping, index.dbIndex));
                                changes.add(new CreateIndex(mapping, index));
                            }
                        }
                    }
                }

                // Drop Extra Indexes.
//                for (SqlSchema.DbIndex dbIndex : mapping.schemaJournalTable.indexes.values()) {
//                    if (mapping.getJournalIndex(dbIndex.name) == null) {
//                        changes.add(new DropIndex(mapping, dbIndex));
//                    }
//                }
            }
        } else if (mapping.schemaJournalTable != null) {
            changes.add(new DropTable(mapping, true));
        }
    }

    private void checkField(TableMapping mapping,
                            Mapping.Property property,
                            SqlSchema.DbColumn column,
                            boolean journal) {
        final DataType columnType = property.columnDataType();
        final DataType fieldType = property.fieldDataType();

        if (columnType == null) {
            throw new PersistException("Could not make a DataType for [" + column.tableName + "." + column.name + "] DBType [" + column.dataType + "][" + column.typeName + "]");
        }

        if (columnType.getType() != fieldType.getType()
                || ((platform.isLengthBased(column.dataType) || platform.isLengthBased(property.dbType)) && columnType.length() != fieldType.length())
                || columnType.nullable() != fieldType.nullable()) {
            changes.add(new ModifyColumn(mapping, property, journal));
        }
    }

    private void checkIndex(TableMapping mapping, TableMapping.Index index) {

    }

    /**
     *
     */
    public static abstract class Change {
        public abstract ChangeType type();

        public abstract String ddl(SqlPlatform platform);
    }

    /**
     * "DROP TABLE" statement
     */
    public static final class DropTable extends Change {
        public final TableMapping mapping;
        public final boolean journal;

        public DropTable(TableMapping mapping, boolean journal) {
            this.mapping = mapping;
            this.journal = journal;
        }

        @Override
        public ChangeType type() {
            return ChangeType.DROP_TABLE;
        }

        @Override
        public String ddl(SqlPlatform platform) {
            return platform.ddlDropTable(mapping, journal);
        }
    }

    /**
     * "CREATE TABLE" statement
     */
    public static final class CreateTable extends Change {
        public final TableMapping mapping;
        public final boolean journal;

        public CreateTable(TableMapping mapping, boolean journal) {
            this.mapping = mapping;
            this.journal = journal;
        }

        @Override
        public ChangeType type() {
            return ChangeType.CREATE_TABLE;
        }

        @Override
        public String ddl(SqlPlatform platform) {
            return platform.ddlCreateTable(mapping, journal);
        }
    }

    /**
     * "ALTER TABLE ADD COLUMN" statement
     */
    public static final class AddColumn extends Change {
        public final TableMapping mapping;
        public final Mapping.Property property;
        public final boolean journal;

        public AddColumn(TableMapping mapping, Mapping.Property property, boolean journal) {
            this.mapping = mapping;
            this.property = property;
            this.journal = journal;
        }

        @Override
        public ChangeType type() {
            return ChangeType.ADD_COLUMN;
        }

        @Override
        public String ddl(SqlPlatform platform) {
            return platform.ddlAddColumn(mapping, property, journal);
        }
    }

    /**
     * "ALTER TABLE DROP COLUMN" statement
     */
    public static final class DropColumn extends Change {
        public final TableMapping mapping;
        public final SqlSchema.DbColumn column;
        public final boolean forJournal;

        public DropColumn(TableMapping mapping, SqlSchema.DbColumn column, boolean forJournal) {
            this.mapping = mapping;
            this.column = column;
            this.forJournal = forJournal;
        }

        @Override
        public ChangeType type() {
            return ChangeType.DROP_COLUMN;
        }

        @Override
        public String ddl(SqlPlatform platform) {
            return platform.ddlDropColumn(mapping, column, forJournal);
        }
    }

    /**
     * "ALTER TABLE MODIFY COLUMN" statement
     */
    public static final class ModifyColumn extends Change {
        public final TableMapping mapping;
        public final Mapping.Property property;
        public final boolean journal;

        public ModifyColumn(TableMapping mapping, Mapping.Property property, boolean journal) {
            this.mapping = mapping;
            this.property = property;
            this.journal = journal;
        }

        @Override
        public ChangeType type() {
            return ChangeType.MODIFY_COLUMN;
        }

        @Override
        public String ddl(SqlPlatform platform) {
            return platform.ddlModifyColumn(mapping, property, journal);
        }
    }

    /**
     * "ALTER TABLE ADD INDEX" statement
     */
    public static final class CreatePrimaryKey extends Change {
        public final TableMapping mapping;
        public final boolean journal;

        public CreatePrimaryKey(TableMapping mapping, boolean journal) {
            this.mapping = mapping;
            this.journal = journal;
        }

        @Override
        public ChangeType type() {
            return ChangeType.CREATE_PRIMARY_KEY;
        }

        @Override
        public String ddl(SqlPlatform platform) {
            return platform.ddlPrimaryKey(mapping, journal);
        }
    }

    /**
     * "ALTER TABLE ADD INDEX" statement
     */
    public static final class CreateIndex extends Change {
        public final TableMapping mapping;
        public final TableMapping.Index index;

        public CreateIndex(TableMapping mapping, TableMapping.Index index) {
            this.mapping = mapping;
            this.index = index;
        }

        @Override
        public ChangeType type() {
            return index.unique ? ChangeType.CREATE_UNIQUE_INDEX : ChangeType.CREATE_INDEX;
        }

        @Override
        public String ddl(SqlPlatform platform) {
            return platform.ddlCreateIndex(mapping, index);
        }
    }

    /**
     * "ALTER TABLE DROP INDEX" statement
     */
    public static final class DropIndex extends Change {
        public final TableMapping mapping;
        public final SqlSchema.DbIndex index;

        public DropIndex(TableMapping mapping, SqlSchema.DbIndex index) {
            this.mapping = mapping;
            this.index = index;
        }

        @Override
        public ChangeType type() {
            return index.unique ? ChangeType.DROP_UNIQUE_INDEX : ChangeType.DROP_INDEX;
        }

        @Override
        public String ddl(SqlPlatform platform) {
            return platform.ddlDropIndex(index);
        }
    }
}
