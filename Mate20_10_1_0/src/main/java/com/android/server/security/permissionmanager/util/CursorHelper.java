package com.android.server.security.permissionmanager.util;

import android.database.Cursor;
import android.util.Slog;

public class CursorHelper {
    private static final String TAG = "CursorHelper";

    private CursorHelper() {
        Slog.i(TAG, "create helper");
    }

    public static boolean checkCursorValid(Cursor cursor) {
        if (cursor == null || cursor.getCount() <= 0) {
            return false;
        }
        return true;
    }

    public static void closeCursor(Cursor cursor) {
        if (cursor != null) {
            cursor.close();
        }
    }
}
