package tmsdkobf;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import tmsdk.common.TMSDKContext;
import tmsdk.common.utils.f;

public class he extends ContentProvider {
    private static final Map<Integer, Integer> pt = new HashMap();
    private final Object mLock = new Object();
    private SQLiteOpenHelper pq = null;
    private SQLiteDatabase pr = null;
    private Set<String> ps;

    private ContentValues a(ContentValues contentValues) {
        Object -l_2_R = new ContentValues();
        if (contentValues.containsKey("xa")) {
            -l_2_R.put("xa", contentValues.getAsInteger("xa"));
        }
        if (contentValues.containsKey("xf")) {
            -l_2_R.put("xf", contentValues.getAsInteger("xf"));
        }
        if (contentValues.containsKey("xg")) {
            -l_2_R.put("xg", contentValues.getAsInteger("xg"));
        }
        if (contentValues.containsKey("xh")) {
            -l_2_R.put("xh", contentValues.getAsInteger("xh"));
        }
        if (contentValues.containsKey("xb")) {
            -l_2_R.put("xb", contentValues.getAsInteger("xb"));
        }
        if (contentValues.containsKey("xc")) {
            -l_2_R.put("xc", contentValues.getAsInteger("xc"));
        }
        if (contentValues.containsKey("xd")) {
            -l_2_R.put("xd", contentValues.getAsInteger("xd"));
        }
        if (contentValues.containsKey("xe")) {
            -l_2_R.put("xe", contentValues.getAsInteger("xe"));
        }
        if (contentValues.containsKey("xi")) {
            -l_2_R.put("xi", contentValues.getAsString("xi"));
        }
        if (contentValues.containsKey("xm")) {
            -l_2_R.put("xm", contentValues.getAsString("xm"));
        }
        if (contentValues.containsKey("xj")) {
            -l_2_R.put("xj", contentValues.getAsString("xj"));
        }
        if (contentValues.containsKey("xk")) {
            -l_2_R.put("xk", contentValues.getAsString("xk"));
        }
        if (contentValues.containsKey("xl")) {
            -l_2_R.put("xl", contentValues.getAsString("xl"));
        }
        return -l_2_R;
    }

    private void a(ContentValues contentValues, String str) {
        Object -l_3_R = getDatabase();
        int -l_4_I = contentValues.getAsInteger("xa").intValue();
        contentValues.remove("xa");
        if (-l_3_R.update("xml_pi_info_table", contentValues, str, null) <= 0) {
            contentValues.put("xa", Integer.valueOf(-l_4_I));
            -l_3_R.insert("xml_pi_info_table", null, contentValues);
        }
    }

    private void a(ContentValues contentValues, String str, String str2) {
        if (contentValues != null && str2 != null) {
            if ("both_pi_info_table".equals(str2) || "xml_pi_info_table".equals(str2)) {
                a(a(contentValues), str);
            }
            if ("both_pi_info_table".equals(str2) || "loc_pi_info_table".equals(str2)) {
                b(b(contentValues), str);
            }
        }
    }

    private int aa(int i) {
        bf();
        return !pt.containsKey(Integer.valueOf(i)) ? 0 : ((Integer) pt.get(Integer.valueOf(i))).intValue();
    }

    private void aq(String str) {
        if (this.ps == null) {
            this.ps = new HashSet();
        }
        this.ps.add(str);
    }

    private void ar(String str) {
        if (this.ps != null) {
            this.ps.remove(str);
        }
    }

    private ContentValues b(ContentValues contentValues) {
        Object -l_2_R = new ContentValues();
        if (contentValues.containsKey("xa")) {
            -l_2_R.put("xa", contentValues.getAsInteger("xa"));
        }
        if (contentValues.containsKey("lh")) {
            -l_2_R.put("lh", contentValues.getAsInteger("lh"));
        }
        if (contentValues.containsKey("la")) {
            -l_2_R.put("la", contentValues.getAsInteger("la"));
        }
        if (contentValues.containsKey("lb")) {
            -l_2_R.put("lb", contentValues.getAsInteger("lb"));
        }
        if (contentValues.containsKey("lc")) {
            -l_2_R.put("lc", contentValues.getAsInteger("lc"));
        }
        if (contentValues.containsKey("le")) {
            -l_2_R.put("le", contentValues.getAsString("le"));
        }
        if (contentValues.containsKey("ld")) {
            -l_2_R.put("ld", contentValues.getAsString("ld"));
        }
        if (contentValues.containsKey("lf")) {
            -l_2_R.put("lf", contentValues.getAsString("lf"));
        }
        if (contentValues.containsKey("lg")) {
            -l_2_R.put("lg", contentValues.getAsString("lg"));
        }
        return -l_2_R;
    }

    private void b(ContentValues contentValues, String str) {
        Object -l_3_R = getDatabase();
        int -l_4_I = contentValues.getAsInteger("xa").intValue();
        contentValues.remove("xa");
        if (-l_3_R.update("loc_pi_info_table", contentValues, str, null) <= 0) {
            contentValues.put("xa", Integer.valueOf(-l_4_I));
            -l_3_R.insert("loc_pi_info_table", null, contentValues);
        }
    }

    private void b(ContentValues contentValues, String str, String str2) {
        Object -l_4_R = getDatabase();
        int -l_5_I = contentValues.getAsInteger("xa").intValue();
        contentValues.remove("xa");
        if (-l_4_R.update("pi_compat_table", contentValues, str, null) <= 0) {
            contentValues.put("xa", Integer.valueOf(-l_5_I));
            -l_4_R.insert("pi_compat_table", null, contentValues);
        }
    }

    private void bf() {
        if (pt.isEmpty()) {
            Cursor -l_2_R = null;
            try {
                -l_2_R = getDatabase().query("loc_pi_info_table", new String[]{"xa", "lc"}, null, null, null, null, null);
                -l_2_R.moveToFirst();
                while (!-l_2_R.isAfterLast()) {
                    pt.put(Integer.valueOf(-l_2_R.getInt(-l_2_R.getColumnIndex("xa"))), Integer.valueOf(-l_2_R.getInt(-l_2_R.getColumnIndex("lc"))));
                    -l_2_R.moveToNext();
                }
                if (-l_2_R != null) {
                    -l_2_R.close();
                }
            } catch (Object -l_3_R) {
                f.e("ConfigProvider", "ensureStateMap err: " + -l_3_R.getMessage());
                if (-l_2_R != null) {
                    -l_2_R.close();
                }
            } catch (Throwable th) {
                if (-l_2_R != null) {
                    -l_2_R.close();
                }
            }
        }
    }

    private void bg() {
        if (this.ps == null || this.ps.size() <= 0) {
            if (this.pr != null) {
                this.pr.close();
                this.pr = null;
            }
        }
    }

    private void g(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS xml_pi_info_table (xa INTEGER PRIMARY KEY,xb INTEGER,xc INTEGER,xd INTEGER,xe INTEGER,xf INTEGER,xg INTEGER,xh INTEGER,xi TEXT,xm TEXT,xj TEXT,xk TEXT,xl TEXT)");
        sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS loc_pi_info_table (xa INTEGER PRIMARY KEY,lh INTEGER,la INTEGER,lb INTEGER,lc INTEGER,ld TEXT,le TEXT,lf INTEGER,lg INTEGER)");
        sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS pi_compat_table (xa INTEGER PRIMARY KEY,pa INTEGER,xb INTEGER)");
    }

    private SQLiteDatabase getDatabase() {
        Object -l_1_R = TMSDKContext.getApplicaionContext();
        if (-l_1_R != null) {
            if (this.pr == null) {
                if (this.pq == null) {
                    f.f("ConfigProvider", "context: " + -l_1_R);
                    this.pq = new SQLiteOpenHelper(this, -l_1_R, "pi_config.db", null, 6) {
                        final /* synthetic */ he pu;

                        public void onCreate(SQLiteDatabase sQLiteDatabase) {
                            f.f("ConfigProvider", "onCreate");
                            this.pu.g(sQLiteDatabase);
                        }

                        public void onDowngrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
                            f.f("ConfigProvider", "onDowngrade");
                            this.pu.j(sQLiteDatabase, i, i2);
                        }

                        public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
                            f.f("ConfigProvider", "onUpgrade");
                            this.pu.i(sQLiteDatabase, i, i2);
                        }
                    };
                }
                this.pr = this.pq.getWritableDatabase();
            }
            return this.pr;
        }
        throw new IllegalStateException("context is null,maybe process has crashed. please check former log!");
    }

    private void h(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS xml_pi_info_table");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS loc_pi_info_table");
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS pi_compat_table");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void i(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        switch (i) {
            case 1:
                sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS pi_compat_table (xa INTEGER PRIMARY KEY,pa INTEGER,xb INTEGER)");
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                break;
            case 5:
                break;
            default:
                return;
        }
    }

    private void j(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        h(sQLiteDatabase);
        g(sQLiteDatabase);
    }

    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> arrayList) throws OperationApplicationException {
        ContentProviderResult[] contentProviderResultArr;
        synchronized (this.mLock) {
            contentProviderResultArr = null;
            Object -l_4_R = getDatabase();
            -l_4_R.beginTransaction();
            try {
                contentProviderResultArr = super.applyBatch(arrayList);
                -l_4_R.setTransactionSuccessful();
                -l_4_R.endTransaction();
            } catch (Object -l_5_R) {
                f.b("ConfigProvider", "applyBatch, err: " + -l_5_R.getMessage(), -l_5_R);
                -l_4_R.endTransaction();
            } catch (Throwable th) {
                -l_4_R.endTransaction();
            }
        }
        return contentProviderResultArr;
    }

    public int delete(Uri uri, String str, String[] strArr) {
        int -l_5_I;
        synchronized (this.mLock) {
            -l_5_I = -1;
            Object -l_6_R = uri.getPath();
            if ("/delete".equals(-l_6_R)) {
                Object -l_7_R = uri.getQuery();
                if (strArr != null && strArr.length == 1) {
                    if ("vo_init".equals(strArr[0])) {
                        Object -l_8_R;
                        if ("xml_pi_info_table".equals(-l_7_R)) {
                            -l_8_R = getDatabase();
                            -l_8_R.execSQL("DROP TABLE IF EXISTS " + -l_7_R);
                            -l_8_R.execSQL("CREATE TABLE IF NOT EXISTS xml_pi_info_table (xa INTEGER PRIMARY KEY,xb INTEGER,xc INTEGER,xd INTEGER,xe INTEGER,xf INTEGER,xg INTEGER,xh INTEGER,xi TEXT,xm TEXT,xj TEXT,xk TEXT,xl TEXT)");
                        } else if ("loc_pi_info_table".equals(-l_7_R)) {
                            -l_8_R = getDatabase();
                            -l_8_R.execSQL("DROP TABLE IF EXISTS " + -l_7_R);
                            -l_8_R.execSQL("CREATE TABLE IF NOT EXISTS loc_pi_info_table (xa INTEGER PRIMARY KEY,lh INTEGER,la INTEGER,lb INTEGER,lc INTEGER,ld TEXT,le TEXT,lf INTEGER,lg INTEGER)");
                        } else if ("pi_compat_table".equals(-l_7_R)) {
                            -l_8_R = getDatabase();
                            -l_8_R.execSQL("DROP TABLE IF EXISTS " + -l_7_R);
                            -l_8_R.execSQL("CREATE TABLE IF NOT EXISTS pi_compat_table (xa INTEGER PRIMARY KEY,pa INTEGER,xb INTEGER)");
                        }
                        -l_5_I = 0;
                    }
                }
                -l_5_I = !"vt_ps".equals(-l_7_R) ? getDatabase().delete(-l_7_R, str, strArr) : aa(Integer.parseInt(str));
            } else {
                if ("/closecursor".equals(-l_6_R)) {
                    ar(uri.getQuery());
                } else if ("/close".equals(-l_6_R)) {
                    bg();
                }
                -l_5_I = 0;
            }
        }
        return -l_5_I;
    }

    public String getType(Uri uri) {
        return null;
    }

    public Uri insert(Uri uri, ContentValues contentValues) {
        return null;
    }

    public boolean onCreate() {
        bf();
        return true;
    }

    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        synchronized (this.mLock) {
            Cursor query;
            Object -l_7_R = uri.getPath();
            int -l_8_I = -l_7_R.indexOf("_");
            if (-l_8_I != -1) {
                aq(-l_7_R.substring(-l_8_I + 1));
            }
            Object -l_9_R = uri.getQuery();
            if (!"xml_pi_info_table".equals(-l_9_R)) {
                if (!"loc_pi_info_table".equals(-l_9_R)) {
                    if ("pi_compat_table".equals(-l_9_R)) {
                        query = getDatabase().query(-l_9_R, strArr, str, strArr2, null, null, str2);
                        return query;
                    }
                    return null;
                }
            }
            query = getDatabase().query(-l_9_R, strArr, str, strArr2, null, null, str2);
            return query;
        }
    }

    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        int -l_6_I;
        synchronized (this.mLock) {
            -l_6_I = -1;
            Object -l_7_R = uri.getQuery();
            if ("both_pi_info_table".equals(-l_7_R) || "xml_pi_info_table".equals(-l_7_R) || "loc_pi_info_table".equals(-l_7_R)) {
                a(contentValues, str, -l_7_R);
                if (!"both_pi_info_table".equals(-l_7_R)) {
                    if (!"loc_pi_info_table".equals(-l_7_R)) {
                    }
                }
                pt.clear();
                bf();
            } else {
                if ("pi_compat_table".equals(-l_7_R)) {
                    b(contentValues, str, -l_7_R);
                }
            }
            -l_6_I = 0;
        }
        return -l_6_I;
    }
}
