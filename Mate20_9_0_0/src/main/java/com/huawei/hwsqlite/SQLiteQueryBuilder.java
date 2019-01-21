package com.huawei.hwsqlite;

import android.database.Cursor;
import android.os.CancellationSignal;
import android.text.TextUtils;
import android.util.Log;
import com.huawei.android.app.AppOpsManagerEx;
import com.huawei.android.util.JlogConstantsEx;
import com.huawei.hwsqlite.SQLiteDatabase.CursorFactory;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

public class SQLiteQueryBuilder {
    private static final String TAG = "SQLiteQueryBuilder";
    private static final Pattern sLimitPattern = Pattern.compile("\\s*\\d+\\s*(,\\s*\\d+\\s*)?");
    private boolean mDistinct = false;
    private CursorFactory mFactory = null;
    private Map<String, String> mProjectionMap = null;
    private boolean mStrict;
    private String mTables = "";
    private StringBuilder mWhereClause = null;

    public void setDistinct(boolean distinct) {
        this.mDistinct = distinct;
    }

    public String getTables() {
        return this.mTables;
    }

    public void setTables(String inTables) {
        this.mTables = inTables;
    }

    public void appendWhere(CharSequence inWhere) {
        if (this.mWhereClause == null) {
            this.mWhereClause = new StringBuilder(inWhere.length() + 16);
        }
        if (this.mWhereClause.length() == 0) {
            this.mWhereClause.append('(');
        }
        this.mWhereClause.append(inWhere);
    }

    public void appendWhereEscapeString(String inWhere) {
        if (this.mWhereClause == null) {
            this.mWhereClause = new StringBuilder(inWhere.length() + 16);
        }
        if (this.mWhereClause.length() == 0) {
            this.mWhereClause.append('(');
        }
        SQLiteDatabaseUtils.appendEscapedSQLString(this.mWhereClause, inWhere);
    }

    public void setProjectionMap(Map<String, String> columnMap) {
        this.mProjectionMap = columnMap;
    }

    public void setCursorFactory(CursorFactory factory) {
        this.mFactory = factory;
    }

    public void setStrict(boolean flag) {
        this.mStrict = flag;
    }

    public static String buildQueryString(boolean distinct, String tables, String[] columns, String where, String groupBy, String having, String orderBy, String limit) {
        if (TextUtils.isEmpty(groupBy) && !TextUtils.isEmpty(having)) {
            throw new IllegalArgumentException("HAVING clauses are only permitted when using a groupBy clause");
        } else if (TextUtils.isEmpty(limit) || sLimitPattern.matcher(limit).matches()) {
            StringBuilder query = new StringBuilder(JlogConstantsEx.JLID_DIALPAD_ONTOUCH_NOT_FIRST_DOWN);
            query.append("SELECT ");
            if (distinct) {
                query.append("DISTINCT ");
            }
            if (columns == null || columns.length == 0) {
                query.append("* ");
            } else {
                appendColumns(query, columns);
            }
            query.append("FROM ");
            query.append(tables);
            appendClause(query, " WHERE ", where);
            appendClause(query, " GROUP BY ", groupBy);
            appendClause(query, " HAVING ", having);
            appendClause(query, " ORDER BY ", orderBy);
            appendClause(query, " LIMIT ", limit);
            return query.toString();
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("invalid LIMIT clauses:");
            stringBuilder.append(limit);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private static void appendClause(StringBuilder s, String name, String clause) {
        if (!TextUtils.isEmpty(clause)) {
            s.append(name);
            s.append(clause);
        }
    }

    public static void appendColumns(StringBuilder s, String[] columns) {
        int n = columns.length;
        for (int i = 0; i < n; i++) {
            String column = columns[i];
            if (column != null) {
                if (i > 0) {
                    s.append(", ");
                }
                s.append(column);
            }
        }
        s.append(' ');
    }

    public Cursor query(SQLiteDatabase db, String[] projectionIn, String selection, String[] selectionArgs, String groupBy, String having, String sortOrder) {
        return query(db, projectionIn, selection, selectionArgs, groupBy, having, sortOrder, null, null);
    }

    public Cursor query(SQLiteDatabase db, String[] projectionIn, String selection, String[] selectionArgs, String groupBy, String having, String sortOrder, String limit) {
        return query(db, projectionIn, selection, selectionArgs, groupBy, having, sortOrder, limit, null);
    }

    public Cursor query(SQLiteDatabase db, String[] projectionIn, String selection, String[] selectionArgs, String groupBy, String having, String sortOrder, String limit, CancellationSignal cancellationSignal) {
        String str = selection;
        if (this.mTables == null) {
            return null;
        }
        SQLiteDatabase sQLiteDatabase;
        CancellationSignal cancellationSignal2;
        if (!this.mStrict || str == null || str.length() <= 0) {
            sQLiteDatabase = db;
            cancellationSignal2 = cancellationSignal;
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("(");
            stringBuilder.append(str);
            stringBuilder.append(")");
            sQLiteDatabase = db;
            cancellationSignal2 = cancellationSignal;
            sQLiteDatabase.validateSql(buildQuery(projectionIn, stringBuilder.toString(), groupBy, having, sortOrder, limit), cancellationSignal2);
        }
        String sql = buildQuery(projectionIn, str, groupBy, having, sortOrder, limit);
        if (Log.isLoggable(TAG, 3)) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Performing query: ");
            stringBuilder2.append(sql);
            Log.d(str2, stringBuilder2.toString());
        }
        return sQLiteDatabase.rawQueryWithFactory(this.mFactory, sql, selectionArgs, SQLiteDatabase.findEditTable(this.mTables), cancellationSignal2);
    }

    public String buildQuery(String[] projectionIn, String selection, String groupBy, String having, String sortOrder, String limit) {
        String str = selection;
        String[] projection = computeProjection(projectionIn);
        StringBuilder where = new StringBuilder();
        boolean z = this.mWhereClause != null && this.mWhereClause.length() > 0;
        boolean hasBaseWhereClause = z;
        if (hasBaseWhereClause) {
            where.append(this.mWhereClause.toString());
            where.append(')');
        }
        if (str != null && str.length() > 0) {
            if (hasBaseWhereClause) {
                where.append(" AND ");
            }
            where.append('(');
            where.append(str);
            where.append(')');
        }
        return buildQueryString(this.mDistinct, this.mTables, projection, where.toString(), groupBy, having, sortOrder, limit);
    }

    @Deprecated
    public String buildQuery(String[] projectionIn, String selection, String[] selectionArgs, String groupBy, String having, String sortOrder, String limit) {
        return buildQuery(projectionIn, selection, groupBy, having, sortOrder, limit);
    }

    public String buildUnionSubQuery(String typeDiscriminatorColumn, String[] unionColumns, Set<String> columnsPresentInTable, int computedColumnsOffset, String typeDiscriminatorValue, String selection, String groupBy, String having) {
        Set<String> set;
        int i;
        String str;
        String str2 = typeDiscriminatorColumn;
        String[] strArr = unionColumns;
        int unionColumnsCount = strArr.length;
        String[] projectionIn = new String[unionColumnsCount];
        for (int i2 = 0; i2 < unionColumnsCount; i2++) {
            String unionColumn = strArr[i2];
            StringBuilder stringBuilder;
            if (unionColumn.equals(str2)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("'");
                stringBuilder.append(typeDiscriminatorValue);
                stringBuilder.append("' AS ");
                stringBuilder.append(str2);
                projectionIn[i2] = stringBuilder.toString();
                set = columnsPresentInTable;
                i = computedColumnsOffset;
            } else {
                str = typeDiscriminatorValue;
                if (i2 <= computedColumnsOffset) {
                    set = columnsPresentInTable;
                } else if (!columnsPresentInTable.contains(unionColumn)) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("NULL AS ");
                    stringBuilder.append(unionColumn);
                    projectionIn[i2] = stringBuilder.toString();
                }
                projectionIn[i2] = unionColumn;
            }
        }
        set = columnsPresentInTable;
        i = computedColumnsOffset;
        str = typeDiscriminatorValue;
        return buildQuery(projectionIn, selection, groupBy, having, null, null);
    }

    @Deprecated
    public String buildUnionSubQuery(String typeDiscriminatorColumn, String[] unionColumns, Set<String> columnsPresentInTable, int computedColumnsOffset, String typeDiscriminatorValue, String selection, String[] selectionArgs, String groupBy, String having) {
        return buildUnionSubQuery(typeDiscriminatorColumn, unionColumns, columnsPresentInTable, computedColumnsOffset, typeDiscriminatorValue, selection, groupBy, having);
    }

    public String buildUnionQuery(String[] subQueries, String sortOrder, String limit) {
        StringBuilder query = new StringBuilder(AppOpsManagerEx.TYPE_MICROPHONE);
        int subQueryCount = subQueries.length;
        String unionOperator = this.mDistinct ? " UNION " : " UNION ALL ";
        for (int i = 0; i < subQueryCount; i++) {
            if (i > 0) {
                query.append(unionOperator);
            }
            query.append(subQueries[i]);
        }
        appendClause(query, " ORDER BY ", sortOrder);
        appendClause(query, " LIMIT ", limit);
        return query.toString();
    }

    private String[] computeProjection(String[] projectionIn) {
        int i = 0;
        if (projectionIn == null || projectionIn.length <= 0) {
            if (this.mProjectionMap == null) {
                return null;
            }
            Set<Entry<String, String>> entrySet = this.mProjectionMap.entrySet();
            String[] projection = new String[entrySet.size()];
            for (Entry<String, String> entry : entrySet) {
                if (!((String) entry.getKey()).equals("_count")) {
                    int i2 = i + 1;
                    projection[i] = (String) entry.getValue();
                    i = i2;
                }
            }
            return projection;
        } else if (this.mProjectionMap == null) {
            return projectionIn;
        } else {
            String[] projection2 = new String[projectionIn.length];
            int length = projectionIn.length;
            while (i < length) {
                String userColumn = projectionIn[i];
                String column = (String) this.mProjectionMap.get(userColumn);
                if (column != null) {
                    projection2[i] = column;
                } else if (this.mStrict || !(userColumn.contains(" AS ") || userColumn.contains(" as "))) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Invalid column ");
                    stringBuilder.append(projectionIn[i]);
                    throw new IllegalArgumentException(stringBuilder.toString());
                } else {
                    projection2[i] = userColumn;
                }
                i++;
            }
            return projection2;
        }
    }
}
