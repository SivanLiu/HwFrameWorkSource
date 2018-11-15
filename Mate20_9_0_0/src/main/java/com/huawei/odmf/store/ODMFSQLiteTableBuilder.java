package com.huawei.odmf.store;

import android.database.SQLException;
import com.huawei.odmf.database.DataBase;
import com.huawei.odmf.exception.ODMFRuntimeException;
import com.huawei.odmf.utils.LOG;
import com.huawei.odmf.utils.StringUtil;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

class ODMFSQLiteTableBuilder {
    static final String ABORT = "ABORT";
    static final String BLOB = "BLOB";
    static final String BOOLEAN = "BOOLEAN";
    static final String IGNORE = "IGNORE";
    static final String INTEGER = "INTEGER";
    static final String REAL = "REAL";
    static final String REPLACE = "REPLACE";
    static final String TEXT = "TEXT";
    private ArrayList<Column> sqliteColumns = new ArrayList();
    private ArrayList<SQLiteTableConstraint> sqliteTableConstraints = new ArrayList();
    private String sqliteTableName;

    private static class Column {
        ArrayList<SQLiteColumnConstraint> constraints = new ArrayList();
        public String name;
        public String type;

        Column(String name, String type) {
            this.name = name;
            this.type = type;
        }

        String toSql() {
            String sqlStatement = "'" + this.name + "' " + this.type + " ";
            Collection constraintSqlStatements = new ArrayList();
            int size = this.constraints.size();
            for (int i = 0; i < size; i++) {
                constraintSqlStatements.add(((SQLiteColumnConstraint) this.constraints.get(i)).toSql());
            }
            return sqlStatement + StringUtil.join(constraintSqlStatements, " ");
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @interface ColumnType {
    }

    @Retention(RetentionPolicy.SOURCE)
    @interface ConflictAction {
    }

    private static class ConflictClause {
        String conflictAction;

        ConflictClause(String conflictAction) {
            this.conflictAction = conflictAction;
        }

        String toSql() {
            return "ON CONFLICT " + this.conflictAction;
        }
    }

    private interface SQLiteConstraint {
        String toSql();
    }

    private interface SQLiteColumnConstraint extends SQLiteConstraint {
    }

    private interface SQLiteTableConstraint extends SQLiteConstraint {
    }

    private abstract class ColumnBasedSQLiteTableConstraint implements SQLiteTableConstraint {
        private ColumnBasedSQLiteTableConstraint() {
        }
    }

    private static class SQLiteDefaultValue implements SQLiteColumnConstraint {
        String defaultValue;

        SQLiteDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String toSql() {
            return "DEFAULT '" + this.defaultValue + "'";
        }
    }

    private static class SQLiteNullable implements SQLiteColumnConstraint {
        boolean nullable;

        SQLiteNullable(boolean nullable) {
            this.nullable = nullable;
        }

        public String toSql() {
            String sqlStatement = "";
            if (this.nullable) {
                return sqlStatement;
            }
            return sqlStatement + " NOT NULL";
        }
    }

    private static class SQLitePrimaryKey implements SQLiteColumnConstraint {
        boolean autoIncrement;

        SQLitePrimaryKey(boolean autoIncrement) {
            this.autoIncrement = autoIncrement;
        }

        public String toSql() {
            String sqlStatement = "PRIMARY KEY";
            if (this.autoIncrement) {
                return sqlStatement + " AUTOINCREMENT";
            }
            return sqlStatement;
        }
    }

    private static class SQLiteUnique implements SQLiteColumnConstraint {
        ConflictClause conflictClause;

        SQLiteUnique(ConflictClause conflictClause) {
            this.conflictClause = conflictClause;
        }

        public String toSql() {
            return "UNIQUE " + this.conflictClause.toSql();
        }
    }

    private class PrimaryKeySQLiteTableConstraint extends ColumnBasedSQLiteTableConstraint {
        private final List<String> columns;
        private final String constraintType = "PRIMARY KEY";

        PrimaryKeySQLiteTableConstraint(List<String> columns) {
            super();
            this.columns = columns;
        }

        public String toSql() {
            return this.constraintType + "(" + StringUtil.join(this.columns, ",") + ")";
        }
    }

    ODMFSQLiteTableBuilder() {
    }

    ODMFSQLiteTableBuilder setSqliteTableName(String name) {
        this.sqliteTableName = name;
        return this;
    }

    ODMFSQLiteTableBuilder setPrimaryKey(String name, String type, boolean autoIncrement) {
        SQLitePrimaryKey SQLitePrimaryKeyConstraint = new SQLitePrimaryKey(autoIncrement);
        SQLitePrimaryKeyConstraint.autoIncrement = autoIncrement;
        Column column = new Column(name, type);
        column.constraints.add(SQLitePrimaryKeyConstraint);
        this.sqliteColumns.add(column);
        return this;
    }

    ODMFSQLiteTableBuilder addColumn(String name, String type) {
        this.sqliteColumns.add(new Column(name, type));
        return this;
    }

    ODMFSQLiteTableBuilder setNullable(boolean nullable) {
        getLastColumn().constraints.add(new SQLiteNullable(nullable));
        return this;
    }

    ODMFSQLiteTableBuilder setDefaultValue(String defaultValue) {
        getLastColumn().constraints.add(new SQLiteDefaultValue(defaultValue));
        return this;
    }

    ODMFSQLiteTableBuilder setUnique(String conflictAction) {
        getLastColumn().constraints.add(new SQLiteUnique(new ConflictClause(conflictAction)));
        return this;
    }

    ODMFSQLiteTableBuilder primaryKey(List<String> columns) {
        this.sqliteTableConstraints.add(new PrimaryKeySQLiteTableConstraint(columns));
        return this;
    }

    void createTable(DataBase db) {
        if (this.sqliteTableName == null) {
            throw new IllegalStateException("Execute createTable Failed : Table name not specified");
        }
        String sqlStatement = "CREATE TABLE " + this.sqliteTableName;
        if (this.sqliteColumns.isEmpty()) {
            throw new IllegalStateException("Execute createTable Failed : No columns specified");
        }
        int i;
        sqlStatement = sqlStatement + " (";
        Collection columnsSqlStatements = new ArrayList();
        int columnSize = this.sqliteColumns.size();
        for (i = 0; i < columnSize; i++) {
            columnsSqlStatements.add(((Column) this.sqliteColumns.get(i)).toSql());
        }
        int constraintSize = this.sqliteTableConstraints.size();
        for (i = 0; i < constraintSize; i++) {
            columnsSqlStatements.add(((SQLiteTableConstraint) this.sqliteTableConstraints.get(i)).toSql());
        }
        try {
            db.execSQL(sqlStatement + StringUtil.join(columnsSqlStatements, ", ") + ")");
        } catch (SQLException e) {
            LOG.logE("Execute createTable Failed : A SQLException occurred when createTable");
            throw new ODMFRuntimeException("Execute createTable Failed : " + e.toString());
        }
    }

    public void alterTableAddColumn(DataBase db) {
        if (this.sqliteTableName == null) {
            throw new IllegalStateException("Execute alterTableAddColumn Failed : Table name not specified");
        }
        try {
            int size = this.sqliteColumns.size();
            for (int i = 0; i < size; i++) {
                db.execSQL("ALTER TABLE " + this.sqliteTableName + " ADD COLUMN " + ((Column) this.sqliteColumns.get(i)).toSql());
            }
        } catch (SQLException e) {
            LOG.logE("Execute alterTableAddColumn Failed : A SQLException occurred when alterTableAddColumn");
            throw new ODMFRuntimeException("Execute alterTableAddColumn Failed : " + e.toString());
        }
    }

    public static void alterTableName(DataBase db, String tableName, String newTableName) {
        if (tableName == null || newTableName == null) {
            throw new IllegalStateException("Execute alterTableName Failed : Table name not specified");
        }
        try {
            db.execSQL(String.format("ALTER TABLE %s RENAME TO %s;", new Object[]{tableName, newTableName}));
        } catch (SQLException e) {
            LOG.logE("Execute alterTableName Failed : A SQLException occurred when alterTableName");
            throw new ODMFRuntimeException("Execute alterTableName Failed : " + e.toString());
        }
    }

    private Column getLastColumn() {
        if (!this.sqliteColumns.isEmpty()) {
            return (Column) this.sqliteColumns.get(this.sqliteColumns.size() - 1);
        }
        throw new IllegalStateException("Execute getLastColumn Failed : No column previously specified");
    }
}
