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
            try {
                int numRows = getSession().executeForCursorWindow(getSql(), getBindArgs(), window, startPos, requiredPos, countAllRows, getConnectionFlags(), this.mCancellationSignal);
                window.releaseReference();
                return numRows;
            } catch (SQLiteDatabaseCorruptException ex) {
                onCorruption();
                throw ex;
            } catch (SQLiteException ex2) {
                Log.e(TAG, "exception: " + ex2.getMessage() + "; query: " + getSql());
                throw ex2;
            } catch (Throwable th) {
                window.releaseReference();
            }
        } finally {
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
            Log.e(TAG, "exception: " + ex2.getMessage() + "; query: " + getSql());
            throw ex2;
        } catch (Throwable th) {
            releaseReference();
        }
    }

    public String toString() {
        return "SQLiteQuery: " + getSql();
    }
}
