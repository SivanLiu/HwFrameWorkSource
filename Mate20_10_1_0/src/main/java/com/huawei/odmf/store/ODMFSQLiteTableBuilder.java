package com.huawei.odmf.store;

import android.database.SQLException;
import com.huawei.odmf.database.DataBase;
import com.huawei.odmf.exception.ODMFRuntimeException;
import com.huawei.odmf.utils.LOG;
import com.huawei.odmf.utils.StringUtil;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class ODMFSQLiteTableBuilder {
    static final String ABORT = "ABORT";
    static final String BLOB = "BLOB";
    static final String BOOLEAN = "BOOLEAN";
    static final String IGNORE = "IGNORE";
    static final String INTEGER = "INTEGER";
    static final String REAL = "REAL";
    static final String REPLACE = "REPLACE";
    static final String TEXT = "TEXT";
    private ArrayList<Column> sqliteColumns = new ArrayList<>();
    private ArrayList<SQLiteTableConstraint> sqliteTableConstraints = new ArrayList<>();
    private String sqliteTableName;

    @Retention(RetentionPolicy.SOURCE)
    @interface ColumnType {
    }

    @Retention(RetentionPolicy.SOURCE)
    @interface ConflictAction {
    }

    private interface SQLiteColumnConstraint extends SQLiteConstraint {
    }

    private interface SQLiteConstraint {
        String toSql();
    }

    private interface SQLiteTableConstraint extends SQLiteConstraint {
    }

    ODMFSQLiteTableBuilder() {
    }

    /* access modifiers changed from: package-private */
    public ODMFSQLiteTableBuilder setSqliteTableName(String name) {
        this.sqliteTableName = name;
        return this;
    }

    /* access modifiers changed from: package-private */
    public ODMFSQLiteTableBuilder setPrimaryKey(String name, String type, boolean isAutoIncrement) {
        SQLitePrimaryKey SQLitePrimaryKeyConstraint = new SQLitePrimaryKey(isAutoIncrement);
        SQLitePrimaryKeyConstraint.isAutoIncrement = isAutoIncrement;
        Column column = new Column(name, type);
        column.constraints.add(SQLitePrimaryKeyConstraint);
        this.sqliteColumns.add(column);
        return this;
    }

    /* access modifiers changed from: package-private */
    public ODMFSQLiteTableBuilder addColumn(String name, String type) {
        this.sqliteColumns.add(new Column(name, type));
        return this;
    }

    /* access modifiers changed from: package-private */
    public ODMFSQLiteTableBuilder setNullable(boolean nullable) {
        getLastColumn().constraints.add(new SQLiteNullable(nullable));
        return this;
    }

    /* access modifiers changed from: package-private */
    public ODMFSQLiteTableBuilder setDefaultValue(String defaultValue) {
        getLastColumn().constraints.add(new SQLiteDefaultValue(defaultValue));
        return this;
    }

    /* access modifiers changed from: package-private */
    public ODMFSQLiteTableBuilder setUnique(String conflictAction) {
        getLastColumn().constraints.add(new SQLiteUnique(new ConflictClause(conflictAction)));
        return this;
    }

    /* access modifiers changed from: package-private */
    public ODMFSQLiteTableBuilder primaryKey(List<String> columns) {
        this.sqliteTableConstraints.add(new PrimaryKeySQLiteTableConstraint(columns));
        return this;
    }

    /* access modifiers changed from: package-private */
    public void createTable(DataBase db) {
        if (this.sqliteTableName == null) {
            throw new IllegalStateException("Execute createTable Failed : Table name not specified");
        }
        String sqlStatement = "CREATE TABLE " + this.sqliteTableName;
        if (this.sqliteColumns.isEmpty()) {
            throw new IllegalStateException("Execute createTable Failed : No columns specified");
        }
        String sqlStatement2 = sqlStatement + " (";
        ArrayList<String> columnsSqlStatements = new ArrayList<>();
        int columnSize = this.sqliteColumns.size();
        for (int i = 0; i < columnSize; i++) {
            columnsSqlStatements.add(this.sqliteColumns.get(i).toSql());
        }
        int constraintSize = this.sqliteTableConstraints.size();
        for (int i2 = 0; i2 < constraintSize; i2++) {
            columnsSqlStatements.add(this.sqliteTableConstraints.get(i2).toSql());
        }
        try {
            db.execSQL(sqlStatement2 + StringUtil.join(columnsSqlStatements, ", ") + ")");
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
                db.execSQL("ALTER TABLE " + this.sqliteTableName + " ADD COLUMN " + this.sqliteColumns.get(i).toSql());
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
            db.execSQL(String.format(Locale.ENGLISH, "ALTER TABLE %s RENAME TO %s;", tableName, newTableName));
        } catch (SQLException e) {
            LOG.logE("Execute alterTableName Failed : A SQLException occurred when alterTableName");
            throw new ODMFRuntimeException("Execute alterTableName Failed : " + e.toString());
        }
    }

    private Column getLastColumn() {
        if (!this.sqliteColumns.isEmpty()) {
            return this.sqliteColumns.get(this.sqliteColumns.size() - 1);
        }
        throw new IllegalStateException("Execute getLastColumn Failed : No column previously specified");
    }

    private static class Column {
        ArrayList<SQLiteColumnConstraint> constraints = new ArrayList<>();
        private String name;
        private String type;

        Column(String name2, String type2) {
            this.name = name2;
            this.type = type2;
        }

        /* access modifiers changed from: package-private */
        public String toSql() {
            String sqlStatement = "'" + this.name + "' " + this.type + " ";
            ArrayList<String> constraintSqlStatements = new ArrayList<>();
            int size = this.constraints.size();
            for (int i = 0; i < size; i++) {
                constraintSqlStatements.add(this.constraints.get(i).toSql());
            }
            return sqlStatement + StringUtil.join(constraintSqlStatements, " ");
        }
    }

    private static class ConflictClause {
        String conflictAction;

        ConflictClause(String conflictAction2) {
            this.conflictAction = conflictAction2;
        }

        /* access modifiers changed from: package-private */
        public String toSql() {
            return "ON CONFLICT " + this.conflictAction;
        }
    }

    private abstract class ColumnBasedSQLiteTableConstraint implements SQLiteTableConstraint {
        private ColumnBasedSQLiteTableConstraint() {
        }
    }

    private static class SQLitePrimaryKey implements SQLiteColumnConstraint {
        boolean isAutoIncrement;

        SQLitePrimaryKey(boolean isAutoIncrement2) {
            this.isAutoIncrement = isAutoIncrement2;
        }

        @Override // com.huawei.odmf.store.ODMFSQLiteTableBuilder.SQLiteConstraint
        public String toSql() {
            if (this.isAutoIncrement) {
                return "PRIMARY KEY" + " AUTOINCREMENT";
            }
            return "PRIMARY KEY";
        }
    }

    private class PrimaryKeySQLiteTableConstraint extends ColumnBasedSQLiteTableConstraint {
        private final List<String> columns;
        private final String constraintType = "PRIMARY KEY";

        PrimaryKeySQLiteTableConstraint(List<String> columns2) {
            super();
            this.columns = columns2;
        }

        @Override // com.huawei.odmf.store.ODMFSQLiteTableBuilder.SQLiteConstraint
        public String toSql() {
            return this.constraintType + "(" + StringUtil.join(this.columns, ",") + ")";
        }
    }

    private static class SQLiteNullable implements SQLiteColumnConstraint {
        boolean isNullable;

        SQLiteNullable(boolean isNullable2) {
            this.isNullable = isNullable2;
        }

        @Override // com.huawei.odmf.store.ODMFSQLiteTableBuilder.SQLiteConstraint
        public String toSql() {
            if (!this.isNullable) {
                return "" + " NOT NULL";
            }
            return "";
        }
    }

    private static class SQLiteDefaultValue implements SQLiteColumnConstraint {
        String defaultValue;

        SQLiteDefaultValue(String defaultValue2) {
            this.defaultValue = defaultValue2;
        }

        @Override // com.huawei.odmf.store.ODMFSQLiteTableBuilder.SQLiteConstraint
        public String toSql() {
            return "DEFAULT '" + this.defaultValue + "'";
        }
    }

    private static class SQLiteUnique implements SQLiteColumnConstraint {
        ConflictClause conflictClause;

        SQLiteUnique(ConflictClause conflictClause2) {
            this.conflictClause = conflictClause2;
        }

        @Override // com.huawei.odmf.store.ODMFSQLiteTableBuilder.SQLiteConstraint
        public String toSql() {
            return "UNIQUE " + this.conflictClause.toSql();
        }
    }
}
