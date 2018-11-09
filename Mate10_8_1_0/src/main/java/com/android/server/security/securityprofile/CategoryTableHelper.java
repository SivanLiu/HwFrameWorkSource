package com.android.server.security.securityprofile;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Slog;
import java.util.Iterator;
import java.util.List;

/* compiled from: DatabaseHelper */
class CategoryTableHelper {
    public static final String CATEGORY_TABLE = "category";
    private static final String[] COLUMNS_FOR_QUERY = new String[]{"package", "category"};
    public static final String COLUMN_CATEGORY = "category";
    public static final String COLUMN_PACKAGE = "package";
    private SQLiteOpenHelper mOpenHelper = null;

    public java.util.Hashtable<java.lang.String, java.lang.Integer> readDatabase() {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Can't find block by offset: 0x006d in list []
	at jadx.core.utils.BlockUtils.getBlockByOffset(BlockUtils.java:43)
	at jadx.core.dex.instructions.IfNode.initBlocks(IfNode.java:60)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.initBlocksInIfNodes(BlockFinish.java:48)
	at jadx.core.dex.visitors.blocksmaker.BlockFinish.visit(BlockFinish.java:33)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
        /*
        r14 = this;
        r13 = new java.util.Hashtable;
        r13.<init>();
        r0 = 0;
        r1 = r14.mOpenHelper;	 Catch:{ SQLiteException -> 0x002c }
        r0 = r1.getReadableDatabase();	 Catch:{ SQLiteException -> 0x002c }
        r9 = 0;
        r1 = "category";	 Catch:{ IllegalArgumentException -> 0x005b, all -> 0x0074 }
        r2 = COLUMNS_FOR_QUERY;	 Catch:{ IllegalArgumentException -> 0x005b, all -> 0x0074 }
        r3 = 0;	 Catch:{ IllegalArgumentException -> 0x005b, all -> 0x0074 }
        r4 = 0;	 Catch:{ IllegalArgumentException -> 0x005b, all -> 0x0074 }
        r5 = 0;	 Catch:{ IllegalArgumentException -> 0x005b, all -> 0x0074 }
        r6 = 0;	 Catch:{ IllegalArgumentException -> 0x005b, all -> 0x0074 }
        r7 = 0;	 Catch:{ IllegalArgumentException -> 0x005b, all -> 0x0074 }
        r9 = r0.query(r1, r2, r3, r4, r5, r6, r7);	 Catch:{ IllegalArgumentException -> 0x005b, all -> 0x0074 }
        if (r9 != 0) goto L_0x0037;	 Catch:{ IllegalArgumentException -> 0x005b, all -> 0x0074 }
    L_0x001d:
        r1 = "SecurityProfileDB";	 Catch:{ IllegalArgumentException -> 0x005b, all -> 0x0074 }
        r2 = "can not read database.";	 Catch:{ IllegalArgumentException -> 0x005b, all -> 0x0074 }
        android.util.Slog.e(r1, r2);	 Catch:{ IllegalArgumentException -> 0x005b, all -> 0x0074 }
        if (r9 == 0) goto L_0x002b;
    L_0x0028:
        r9.close();
    L_0x002b:
        return r13;
    L_0x002c:
        r10 = move-exception;
        r1 = "SecurityProfileDB";
        r2 = "can not open readable database.";
        android.util.Slog.e(r1, r2);
        return r13;
    L_0x0037:
        r1 = r9.moveToNext();	 Catch:{ IllegalArgumentException -> 0x005b, all -> 0x0074 }
        if (r1 == 0) goto L_0x006e;	 Catch:{ IllegalArgumentException -> 0x005b, all -> 0x0074 }
    L_0x003d:
        r1 = "package";	 Catch:{ IllegalArgumentException -> 0x005b, all -> 0x0074 }
        r1 = r9.getColumnIndexOrThrow(r1);	 Catch:{ IllegalArgumentException -> 0x005b, all -> 0x0074 }
        r12 = r9.getString(r1);	 Catch:{ IllegalArgumentException -> 0x005b, all -> 0x0074 }
        r1 = "category";	 Catch:{ IllegalArgumentException -> 0x005b, all -> 0x0074 }
        r1 = r9.getColumnIndexOrThrow(r1);	 Catch:{ IllegalArgumentException -> 0x005b, all -> 0x0074 }
        r8 = r9.getInt(r1);	 Catch:{ IllegalArgumentException -> 0x005b, all -> 0x0074 }
        r1 = java.lang.Integer.valueOf(r8);	 Catch:{ IllegalArgumentException -> 0x005b, all -> 0x0074 }
        r13.put(r12, r1);	 Catch:{ IllegalArgumentException -> 0x005b, all -> 0x0074 }
        goto L_0x0037;
    L_0x005b:
        r11 = move-exception;
        r1 = "SecurityProfileDB";	 Catch:{ IllegalArgumentException -> 0x005b, all -> 0x0074 }
        r2 = "find format errors in database.";	 Catch:{ IllegalArgumentException -> 0x005b, all -> 0x0074 }
        android.util.Slog.e(r1, r2);	 Catch:{ IllegalArgumentException -> 0x005b, all -> 0x0074 }
        r13.clear();	 Catch:{ IllegalArgumentException -> 0x005b, all -> 0x0074 }
        if (r9 == 0) goto L_0x006d;
    L_0x006a:
        r9.close();
    L_0x006d:
        return r13;
    L_0x006e:
        if (r9 == 0) goto L_0x006d;
    L_0x0070:
        r9.close();
        goto L_0x006d;
    L_0x0074:
        r1 = move-exception;
        if (r9 == 0) goto L_0x007a;
    L_0x0077:
        r9.close();
    L_0x007a:
        throw r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.server.security.securityprofile.CategoryTableHelper.readDatabase():java.util.Hashtable<java.lang.String, java.lang.Integer>");
    }

    public CategoryTableHelper(SQLiteOpenHelper helper) {
        this.mOpenHelper = helper;
    }

    public void removeCategoryFromDatabase(List<String> packageList, int category) {
        try {
            SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
            Iterator packageName$iterator = packageList.iterator();
            while (packageName$iterator.hasNext()) {
                db.delete("category", "package=? and category=?", new String[]{(String) packageName$iterator.next(), String.valueOf(category)});
            }
        } catch (SQLiteException e) {
            Slog.e(DatabaseHelper.TAG, "can not open writable database.");
        }
    }

    public void storeCategoryToDatabase(List<String> packageNameList, int category) {
        try {
            SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                for (String packageName : packageNameList) {
                    ContentValues cv = new ContentValues();
                    cv.put("package", packageName);
                    cv.put("category", Integer.valueOf(category));
                    db.delete("category", "package=?", new String[]{packageName});
                    db.insert("category", null, cv);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (SQLiteException e) {
            Slog.e(DatabaseHelper.TAG, "can not open writable database.");
        }
    }

    public void eraseBlacklistedFromDatabase(int category) {
        try {
            this.mOpenHelper.getWritableDatabase().delete("category", "category=?", new String[]{String.valueOf(category)});
        } catch (SQLiteException e) {
            Slog.e(DatabaseHelper.TAG, "can not open writable database.");
        }
    }
}
