package com.huawei.hwsqlite;

import android.database.CursorWindow;
import android.os.CancellationSignal;
import android.util.Log;
import com.huawei.hwsqlite.SQLiteConnection.PreparedStatement;

public final class SQLiteQuery extends SQLiteProgram {
    private static final String TAG = "SQLiteQuery";
    private final CancellationSignal mCancellationSignal;

    SQLiteQuery(SQLiteDatabase db, String query, CancellationSignal cancellationSignal) {
        super(db, query, null, cancellationSignal);
        this.mCancellationSignal = cancellationSignal;
    }

    int fillWindow(CursorWindow window, int startPos, int requiredPos, boolean countAllRows) {
        acquireReference();
        try {
            window.acquireReference();
            int numRows = getSession().executeForCursorWindow(getSql(), getBindArgs(), window, startPos, requiredPos, countAllRows, getConnectionFlags(), this.mCancellationSignal);
            window.releaseReference();
            releaseReference();
            return numRows;
        } catch (SQLiteDatabaseCorruptException ex) {
            onCorruption();
            throw ex;
        } catch (SQLiteException ex2) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("exception: ");
            stringBuilder.append(ex2.getMessage());
            stringBuilder.append("; query: ");
            stringBuilder.append(getSql());
            Log.e(str, stringBuilder.toString());
            throw ex2;
        } catch (Throwable th) {
            releaseReference();
        }
    }

    PreparedStatement beginStepQuery() {
        return getSession().beginStepQuery(getSql(), getConnectionFlags(), this.mCancellationSignal);
    }

    void endStepQuery(PreparedStatement statement) {
        getSession().endStepQuery(statement);
    }

    int fillStep() {
        acquireReference();
        try {
            int executeForStepQuery = getSession().executeForStepQuery(getSql(), getBindArgs(), this.mCancellationSignal);
            releaseReference();
            return executeForStepQuery;
        } catch (SQLiteDatabaseCorruptException ex) {
            onCorruption();
            throw ex;
        } catch (SQLiteException ex2) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("exception: ");
            stringBuilder.append(ex2.getMessage());
            stringBuilder.append("; query: ");
            stringBuilder.append(getSql());
            Log.e(str, stringBuilder.toString());
            throw ex2;
        } catch (Throwable th) {
            releaseReference();
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SQLiteQuery: ");
        stringBuilder.append(getSql());
        return stringBuilder.toString();
    }
}
