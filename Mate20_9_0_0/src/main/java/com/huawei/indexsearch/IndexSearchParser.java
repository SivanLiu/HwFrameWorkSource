package com.huawei.indexsearch;

import android.database.Cursor;
import android.os.SystemProperties;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class IndexSearchParser implements IIndexSearchParser {
    private static final String HWINDEXSEARCHSERVICE_APK_NAME = "com.huawei.nb.service";
    private static final boolean IS_SUPPORT_FULL_TEXT_SEARCH = SystemProperties.get("ro.config.hw_globalSearch", "true").equals("true");
    private static final String TAG = "IndexSearchParser";
    private static volatile IndexSearchParser mInstance = null;
    private String mPkgName;
    private String[] mTables;

    public static synchronized void createIndexSearchParser(String pkgName, String[] tables) {
        synchronized (IndexSearchParser.class) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("createIndexSearchParser pkgName ");
            stringBuilder.append(pkgName);
            Log.i(str, stringBuilder.toString());
            if (isHwIndexSearchServiceExist() && mInstance == null) {
                mInstance = new IndexSearchParser(pkgName, tables);
            }
        }
    }

    public static IndexSearchParser getInstance() {
        return mInstance;
    }

    private IndexSearchParser(String pkgName, String[] tables) {
        this.mPkgName = pkgName;
        this.mTables = tables;
        for (String table : tables) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("table=");
            stringBuilder.append(table);
            Log.i(str, stringBuilder.toString());
        }
    }

    public void notifyIndexSearchService(Cursor c, int operator) {
        if (c == null) {
            Log.i(TAG, "notifyIndexSearchService(Cursor c, int operator) : cursor is null, return.");
            return;
        }
        ArrayList<String> idList = new ArrayList();
        while (c.moveToNext()) {
            idList.add(Long.toString(c.getLong(0)));
        }
        if (idList.size() > 0) {
            IndexSearchObserverManager.getInstance().buildIndex(this.mPkgName, idList, operator);
        }
    }

    public void notifyIndexSearchService(long id, int operator) {
        List idList = new ArrayList();
        idList.add(Long.valueOf(id));
        notifyIndexSearchService(idList, operator);
    }

    public void notifyIndexSearchService(List<Long> list, int operator) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyIndexSearchService begin operator: ");
        stringBuilder.append(operator);
        Log.i(str, stringBuilder.toString());
        List<String> idList = new ArrayList();
        for (Long id : list) {
            idList.add(Long.toString(id.longValue()));
        }
        if (this.mPkgName != null) {
            IndexSearchObserverManager.getInstance().buildIndex(this.mPkgName, idList, operator);
        }
        Log.i(TAG, "notifyIndexSearchService end");
    }

    public boolean isValidTable(String table) {
        for (String equals : this.mTables) {
            if (equals.equals(table)) {
                updateTable(table);
                return true;
            }
        }
        return false;
    }

    private void updateTable(String table) {
        if ("pdu".equals(table)) {
            this.mPkgName = "com.android.providers.telephony";
        } else if ("sms".equals(table)) {
            this.mPkgName = "com.android.mms";
        }
    }

    /* JADX WARNING: Missing block: B:7:0x0012, code skipped:
            if (android.app.ActivityThread.getPackageManager().getPackageInfo(HWINDEXSEARCHSERVICE_APK_NAME, 0, 0) == null) goto L_0x0014;
     */
    /* JADX WARNING: Missing block: B:8:0x0014, code skipped:
            android.util.Log.e(TAG, "IndexSearchService not exist");
     */
    /* JADX WARNING: Missing block: B:9:0x001b, code skipped:
            return false;
     */
    /* JADX WARNING: Missing block: B:15:0x0027, code skipped:
            if (null != null) goto L_0x002a;
     */
    /* JADX WARNING: Missing block: B:17:0x002b, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean isHwIndexSearchServiceExist() {
        if (!IS_SUPPORT_FULL_TEXT_SEARCH) {
            return false;
        }
        try {
        } catch (Exception e) {
            Log.e(TAG, "IndexSearchService packageInfo is null");
        } catch (Throwable th) {
            if (null != null) {
            }
        }
    }

    public static void destroy() {
        if (mInstance != null) {
            mInstance = null;
        }
    }
}
