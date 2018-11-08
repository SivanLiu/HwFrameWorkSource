package tmsdkobf;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import tmsdk.common.utils.f;
import tmsdkobf.ji.a;

public class jr {
    private static Object lock = new Object();
    private static jr tt;
    private jv oA;

    private jr() {
        this.oA = null;
        this.oA = ((kf) fj.D(9)).ap("QQSecureProvider");
    }

    private ContentValues a(av avVar) {
        int i = 0;
        if (avVar == null) {
            return null;
        }
        Object -l_2_R = new ContentValues();
        -l_2_R.put("b", avVar.packageName);
        -l_2_R.put("c", avVar.softName);
        -l_2_R.put("d", avVar.bZ);
        -l_2_R.put("e", Long.valueOf(avVar.ca));
        String str = "f";
        if (avVar.cb) {
            i = 1;
        }
        -l_2_R.put(str, Integer.valueOf(i));
        -l_2_R.put("h", avVar.aS);
        -l_2_R.put("i", Long.valueOf(avVar.cd));
        -l_2_R.put("j", avVar.version);
        -l_2_R.put("g", avVar.cc);
        -l_2_R.put("k", Long.valueOf(avVar.ce));
        return -l_2_R;
    }

    public static void a(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS pf_soft_list_profile_db_table_name (a INTEGER PRIMARY KEY,b TEXT,c TEXT,d TEXT,e LONG,f INTEGER,h TEXT,i LONG,g TEXT,j TEXT,k LONG)");
    }

    public static void a(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        f.f("SoftListProfileDB", "upgradeDB");
        if (i < 15) {
            a(sQLiteDatabase);
        }
        if (i >= 15 && i < 18) {
            try {
                sQLiteDatabase.execSQL("ALTER TABLE pf_soft_list_profile_db_table_name ADD COLUMN k LONG");
            } catch (Exception e) {
            }
        }
    }

    public static void b(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS pf_soft_list_profile_db_table_name");
    }

    public static void b(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        f.f("SoftListProfileDB", "downgradeDB");
        b(sQLiteDatabase);
        a(sQLiteDatabase);
    }

    public static jr cB() {
        if (tt == null) {
            synchronized (lock) {
                if (tt == null) {
                    tt = new jr();
                }
            }
        }
        return tt;
    }

    private ArrayList<jq> cD() {
        Object -l_1_R = new ArrayList();
        Cursor cursor = null;
        Object -l_3_R;
        try {
            cursor = this.oA.a("pf_soft_list_profile_db_table_name", null, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    -l_3_R = new av();
                    -l_3_R.ca = cursor.getLong(cursor.getColumnIndex("e"));
                    -l_3_R.bZ = cursor.getString(cursor.getColumnIndex("d"));
                    -l_3_R.aS = cursor.getString(cursor.getColumnIndex("h"));
                    -l_3_R.cb = cursor.getInt(cursor.getColumnIndex("f")) == 1;
                    -l_3_R.packageName = cursor.getString(cursor.getColumnIndex("b"));
                    -l_3_R.softName = cursor.getString(cursor.getColumnIndex("c"));
                    -l_3_R.version = cursor.getString(cursor.getColumnIndex("j"));
                    -l_3_R.cd = cursor.getLong(cursor.getColumnIndex("i"));
                    -l_3_R.cc = cursor.getString(cursor.getColumnIndex("g"));
                    -l_3_R.ce = cursor.getLong(cursor.getColumnIndex("k"));
                    -l_1_R.add(new jq(-l_3_R, cursor.getInt(cursor.getColumnIndex("a"))));
                    cursor.moveToNext();
                }
                if (cursor != null) {
                    try {
                        cursor.close();
                    } catch (Object -l_3_R2) {
                        f.e("SoftListProfileDB", "cursor.close() crash : " + -l_3_R2.toString());
                    }
                }
                return -l_1_R;
            }
            -l_3_R2 = -l_1_R;
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Object -l_4_R) {
                    f.e("SoftListProfileDB", "cursor.close() crash : " + -l_4_R.toString());
                }
            }
            return -l_1_R;
        } catch (Object -l_3_R22) {
            f.e("SoftListProfileDB", -l_3_R22.toString());
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Object -l_3_R222) {
                    f.e("SoftListProfileDB", "cursor.close() crash : " + -l_3_R222.toString());
                }
            }
        } catch (Throwable th) {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Object -l_6_R) {
                    f.e("SoftListProfileDB", "cursor.close() crash : " + -l_6_R.toString());
                }
            }
        }
    }

    private void clear() {
        this.oA.delete("pf_soft_list_profile_db_table_name", null, null);
    }

    public ArrayList<av> cC() {
        f.f("SoftListProfileDB", "getAllSoftImage");
        Object -l_1_R = new ArrayList();
        Object -l_2_R = cD();
        if (-l_2_R != null && -l_2_R.size() > 0) {
            Object -l_3_R = -l_2_R.iterator();
            while (-l_3_R.hasNext()) {
                jq -l_4_R = (jq) -l_3_R.next();
                if (!(-l_4_R == null || -l_4_R.ts == null)) {
                    -l_1_R.add(-l_4_R.ts);
                }
            }
        }
        return -l_1_R;
    }

    public boolean l(ArrayList<a> arrayList) {
        if (arrayList == null || arrayList.size() <= 0) {
            return true;
        }
        Object -l_2_R = new ArrayList();
        int -l_3_I = 0;
        Object -l_4_R = arrayList.iterator();
        while (-l_4_R.hasNext()) {
            a -l_5_R = (a) -l_4_R.next();
            if (!(-l_5_R == null || -l_5_R.ta == null || !(-l_5_R.ta instanceof av))) {
                av -l_6_R = (av) -l_5_R.ta;
                switch (-l_5_R.action) {
                    case 0:
                        if (-l_3_I == 0) {
                            -l_3_I = 1;
                            clear();
                            break;
                        }
                        break;
                    case 1:
                        break;
                    case 2:
                        -l_2_R.add(ContentProviderOperation.newDelete(this.oA.an("pf_soft_list_profile_db_table_name")).withSelection(String.format("%s = '%s'", new Object[]{"b", -l_6_R.packageName}), null).build());
                        continue;
                    case 3:
                        -l_2_R.add(ContentProviderOperation.newUpdate(this.oA.ao("pf_soft_list_profile_db_table_name")).withValues(a(-l_6_R)).withSelection(String.format("%s = '%s'", new Object[]{"b", -l_6_R.packageName}), null).build());
                        continue;
                    default:
                        continue;
                }
                -l_2_R.add(ContentProviderOperation.newInsert(this.oA.am("pf_soft_list_profile_db_table_name")).withValues(a(-l_6_R)).build());
            }
        }
        if (-l_2_R != null && -l_2_R.size() > 0) {
            -l_4_R = this.oA.applyBatch(-l_2_R);
            if (-l_4_R == null || -l_4_R.length <= 0 || -l_4_R[0] == null) {
                gr.f("SoftListProfileDB", "applyBatchOperation fail!!!");
                return false;
            }
        }
        return true;
    }
}
