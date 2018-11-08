package tmsdkobf;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.qq.taf.jce.a;
import java.util.ArrayList;
import java.util.List;
import tmsdk.common.TMSDKContext;
import tmsdk.common.tcc.TccCryptor;

public class pw {
    static pw KQ = null;
    private static Object KR = new Object();
    private SQLiteOpenHelper KS;
    private final String KT = "CREATE TABLE IF NOT EXISTS r_tb (a INTEGER PRIMARY KEY,f INTEGER,b INTEGER,c INTEGER,d INTEGER,e LONG,i TEXT,j TEXT,k INTEGER,l INTEGER)";

    private pw() {
        ps.g("DataManager-DataManager");
        this.KS = new SQLiteOpenHelper(this, TMSDKContext.getApplicaionContext(), "r.db", null, 10) {
            final /* synthetic */ pw KU;

            public void onCreate(SQLiteDatabase sQLiteDatabase) {
                ps.g("onCreate-db:[" + sQLiteDatabase + "]");
                this.KU.onCreate(sQLiteDatabase);
            }

            public void onDowngrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
                ps.g("onDowngrade-db:[" + sQLiteDatabase + "]oldVersion:[" + i + "]newVersion:[" + i2 + "]");
            }

            public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
                ps.g("onUpgrade-db:[" + sQLiteDatabase + "]oldVersion:[" + i + "]newVersion:[" + i2 + "]");
                this.KU.onUpgrade(sQLiteDatabase, i, i2);
            }
        };
        this.KS.getWritableDatabase().setLockingEnabled(false);
    }

    private long a(String str, ContentValues contentValues) {
        long insert;
        synchronized (KR) {
            insert = this.KS.getWritableDatabase().insert(str, null, contentValues);
        }
        return insert;
    }

    private Cursor al(String str) {
        Cursor rawQuery;
        synchronized (KR) {
            rawQuery = this.KS.getReadableDatabase().rawQuery(str, null);
        }
        return rawQuery;
    }

    private void close() {
        synchronized (KR) {
            this.KS.close();
        }
    }

    private int delete(String str, String str2, String[] strArr) {
        int delete;
        synchronized (KR) {
            delete = this.KS.getWritableDatabase().delete(str, str2, strArr);
        }
        return delete;
    }

    private ContentValues f(pv pvVar) {
        Object -l_2_R = new ContentValues();
        -l_2_R.put("a", Integer.valueOf(pvVar.KN.KV));
        -l_2_R.put("b", Integer.valueOf(pvVar.KN.KW));
        -l_2_R.put("c", Integer.valueOf(pvVar.KN.KX));
        -l_2_R.put("d", Integer.valueOf(pvVar.KN.KY));
        -l_2_R.put("e", Long.valueOf(pvVar.KN.KZ));
        -l_2_R.put("f", Integer.valueOf(pvVar.KN.La));
        -l_2_R.put("i", pvVar.KN.Lb);
        -l_2_R.put("j", a.c(TccCryptor.encrypt(pvVar.KN.Lc.getBytes(), null)));
        -l_2_R.put("k", Integer.valueOf(pvVar.KO));
        -l_2_R.put("l", Integer.valueOf(pvVar.KP));
        return -l_2_R;
    }

    private List<pv> f(Cursor cursor) {
        Object -l_3_R;
        Object -l_2_R = new ArrayList();
        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                -l_3_R = new pv();
                -l_3_R.KN = new px();
                -l_3_R.KN.KV = cursor.getInt(cursor.getColumnIndex("a"));
                -l_3_R.KN.KW = cursor.getInt(cursor.getColumnIndex("b"));
                -l_3_R.KN.KX = cursor.getInt(cursor.getColumnIndex("c"));
                -l_3_R.KN.KY = cursor.getInt(cursor.getColumnIndex("d"));
                -l_3_R.KN.KZ = cursor.getLong(cursor.getColumnIndex("e"));
                -l_3_R.KN.La = cursor.getInt(cursor.getColumnIndex("f"));
                -l_3_R.KN.Lb = cursor.getString(cursor.getColumnIndex("i"));
                -l_3_R.KN.Lc = new String(TccCryptor.decrypt(a.E(cursor.getString(cursor.getColumnIndex("j"))), null));
                -l_3_R.KO = cursor.getInt(cursor.getColumnIndex("k"));
                -l_3_R.KP = cursor.getInt(cursor.getColumnIndex("l"));
                -l_2_R.add(-l_3_R);
                cursor.moveToNext();
            }
        } catch (Object -l_3_R2) {
            ps.h("e:[" + -l_3_R2 + "]");
        }
        return -l_2_R.size() != 0 ? -l_2_R : null;
    }

    public static pw ih() {
        Object -l_0_R = pw.class;
        synchronized (pw.class) {
            if (KQ == null) {
                Object -l_1_R = pw.class;
                synchronized (pw.class) {
                    if (KQ == null) {
                        KQ = new pw();
                    }
                }
            }
            return KQ;
        }
    }

    private void onCreate(SQLiteDatabase sQLiteDatabase) {
        ps.g("execSQL:[CREATE TABLE IF NOT EXISTS r_tb (a INTEGER PRIMARY KEY,f INTEGER,b INTEGER,c INTEGER,d INTEGER,e LONG,i TEXT,j TEXT,k INTEGER,l INTEGER)]");
        sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS r_tb (a INTEGER PRIMARY KEY,f INTEGER,b INTEGER,c INTEGER,d INTEGER,e LONG,i TEXT,j TEXT,k INTEGER,l INTEGER)");
    }

    private void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
    }

    private int update(String str, ContentValues contentValues, String str2, String[] strArr) {
        int update;
        synchronized (KR) {
            update = this.KS.getWritableDatabase().update(str, contentValues, str2, strArr);
        }
        return update;
    }

    public pv bQ(int i) {
        Object -l_4_R;
        ps.g("getDataItem-id:[" + i + "]");
        pv pvVar = null;
        Cursor cursor = null;
        try {
            -l_4_R = new StringBuilder(120);
            -l_4_R.append("SELECT * FROM ");
            -l_4_R.append("r_tb");
            -l_4_R.append(" WHERE ");
            -l_4_R.append("a");
            -l_4_R.append("=");
            -l_4_R.append(i);
            cursor = al(-l_4_R.toString());
            if (cursor != null) {
                Object -l_5_R = f(cursor);
                if (-l_5_R != null && -l_5_R.size() > 0) {
                    pvVar = (pv) -l_5_R.get(0);
                }
            }
            ps.g("getDataItem-item:[" + pvVar + "]");
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Object -l_4_R2) {
                    ps.g("e:[" + -l_4_R2 + "]");
                }
            }
        } catch (Object -l_4_R22) {
            ps.g("e:[" + -l_4_R22 + "]");
        }
        return pvVar;
    }

    public void bR(int i) {
        ps.g("deleteDataItem-id:[" + i + "]");
        try {
            delete("r_tb", "a=" + i, null);
        } catch (Object -l_2_R) {
            ps.g("e:[" + -l_2_R + "]");
        }
    }

    public void d(pv pvVar) {
        ps.g("updateDataItem:[" + pvVar + "]");
        try {
            update("r_tb", f(pvVar), "a=" + pvVar.KN.KV, null);
        } catch (Object -l_2_R) {
            ps.g("e:[" + -l_2_R + "]");
        }
    }

    public void e(pv pvVar) {
        ps.g("insertDataItem:[" + pvVar + "]");
        try {
            a("r_tb", f(pvVar));
        } catch (Object -l_2_R) {
            ps.g("e:[" + -l_2_R + "]");
        }
    }

    public int getCount() {
        Object -l_3_R;
        int -l_1_I = 0;
        Cursor cursor = null;
        try {
            -l_3_R = new StringBuilder(120);
            -l_3_R.append("SELECT COUNT(*) FROM ");
            -l_3_R.append("r_tb");
            Object -l_2_R = al(-l_3_R.toString());
            if (-l_2_R != null && -l_2_R.moveToNext()) {
                -l_1_I = -l_2_R.getInt(0);
            }
            ps.g("getCount-size:[" + -l_1_I + "]");
            if (-l_2_R != null) {
                try {
                    -l_2_R.close();
                } catch (Object -l_3_R2) {
                    ps.g("e:[" + -l_3_R2 + "]");
                }
            }
        } catch (Object -l_3_R22) {
            ps.g("e:[" + -l_3_R22 + "]");
        }
        return -l_1_I;
    }

    public void ii() {
        ps.g("DataManager-freeInstance");
        close();
    }

    public List<pv> ij() {
        List<pv> -l_1_R = null;
        Cursor cursor = null;
        try {
            long -l_3_J = System.currentTimeMillis();
            Object -l_5_R = new StringBuilder(120);
            -l_5_R.append("SELECT * FROM ");
            -l_5_R.append("r_tb");
            -l_5_R.append(" WHERE ");
            -l_5_R.append("k");
            -l_5_R.append("=");
            -l_5_R.append(2);
            -l_5_R.append(" OR ");
            -l_5_R.append("e");
            -l_5_R.append("<");
            -l_5_R.append(-l_3_J);
            ps.g("getCleanItems-sql:[" + -l_5_R.toString() + "]");
            cursor = al(-l_5_R.toString());
            if (cursor != null) {
                -l_1_R = f(cursor);
            }
            ps.g("getCleanItems-size:[" + (-l_1_R == null ? 0 : -l_1_R.size()) + "]");
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Object -l_3_R) {
                    ps.g("e:[" + -l_3_R + "]");
                }
            }
        } catch (Object -l_3_R2) {
            ps.g("e:[" + -l_3_R2 + "]");
        }
        return -l_1_R;
    }

    public List<pv> ik() {
        List<pv> -l_1_R = null;
        Cursor cursor = null;
        Object -l_3_R;
        try {
            -l_3_R = new StringBuilder(120);
            -l_3_R.append("SELECT * FROM ");
            -l_3_R.append("r_tb");
            -l_3_R.append(" WHERE ");
            -l_3_R.append("l");
            -l_3_R.append("=");
            -l_3_R.append(1);
            -l_3_R.append(" OR ");
            -l_3_R.append("l");
            -l_3_R.append("=");
            -l_3_R.append(2);
            -l_3_R.append(" AND ");
            -l_3_R.append("k");
            -l_3_R.append("=");
            -l_3_R.append(1);
            ps.g("getNeedDownloadItems-sql:[" + -l_3_R.toString() + "]");
            cursor = al(-l_3_R.toString());
            if (cursor != null) {
                -l_1_R = f(cursor);
            }
            ps.g("getNeedDownloadItems-size:[" + (-l_1_R == null ? 0 : -l_1_R.size()) + "]");
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Object -l_3_R2) {
                    ps.g("e:[" + -l_3_R2 + "]");
                }
            }
        } catch (Object -l_3_R22) {
            ps.g("e:[" + -l_3_R22 + "]");
        }
        return -l_1_R;
    }

    public List<pv> il() {
        List<pv> -l_1_R = null;
        Cursor cursor = null;
        Object -l_3_R;
        try {
            -l_3_R = new StringBuilder(120);
            -l_3_R.append("SELECT * FROM ");
            -l_3_R.append("r_tb");
            -l_3_R.append(" WHERE ");
            -l_3_R.append("l");
            -l_3_R.append("=");
            -l_3_R.append(3);
            -l_3_R.append(" AND ");
            -l_3_R.append("d");
            -l_3_R.append("=");
            -l_3_R.append(1);
            ps.g("getAutoRunItems-sql:[" + -l_3_R.toString() + "]");
            cursor = al(-l_3_R.toString());
            if (cursor != null) {
                -l_1_R = f(cursor);
            }
            ps.g("getAutoRunItems-size:[" + (-l_1_R == null ? 0 : -l_1_R.size()) + "]");
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Object -l_3_R2) {
                    ps.g("e:[" + -l_3_R2 + "]");
                }
            }
        } catch (Object -l_3_R22) {
            ps.g("e:[" + -l_3_R22 + "]");
        }
        return -l_1_R;
    }
}
