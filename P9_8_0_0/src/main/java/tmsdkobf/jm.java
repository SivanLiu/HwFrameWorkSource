package tmsdkobf;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import com.qq.taf.jce.JceStruct;
import java.util.ArrayList;
import tmsdk.common.utils.f;
import tmsdkobf.ji.a;

public class jm {
    private static Object lock = new Object();
    private static jm tj;
    private jv oA;

    private jm() {
        this.oA = null;
        this.oA = ((kf) fj.D(9)).ap("QQSecureProvider");
    }

    private ContentValues a(as asVar) {
        int i = 0;
        Object -l_2_R = new ContentValues();
        if (asVar == null) {
            return -l_2_R;
        }
        -l_2_R.put("b", Integer.valueOf(asVar.bR));
        -l_2_R.put("c", Integer.valueOf(asVar.valueType));
        switch (asVar.valueType) {
            case 1:
                -l_2_R.put("d", Integer.valueOf(asVar.i));
                break;
            case 2:
                -l_2_R.put("e", Long.valueOf(asVar.bS));
                break;
            case 3:
                -l_2_R.put("f", asVar.bT);
                break;
            case 4:
                -l_2_R.put("g", asVar.bU);
                break;
            case 5:
                String str = "h";
                if (asVar.bV) {
                    i = 1;
                }
                -l_2_R.put(str, Integer.valueOf(i));
                break;
            case 6:
                -l_2_R.put("i", Integer.valueOf(asVar.bW));
                break;
        }
        return -l_2_R;
    }

    public static void a(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS pf_key_value_profile_db_table_name (a INTEGER PRIMARY KEY,b INTEGER,c INTEGER,d INTEGER,e LONG,f TEXT,h INTEGER,i INTEGER,g BLOB)");
    }

    public static void a(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        if (i < 15) {
            a(sQLiteDatabase);
        }
    }

    private ArrayList<jp> aP(String str) {
        Object -l_4_R;
        Object -l_2_R = null;
        Object -l_3_R = new ArrayList();
        -l_2_R = this.oA.a("pf_key_value_profile_db_table_name", null, str, null, null);
        if (-l_2_R != null) {
            -l_2_R.moveToFirst();
            while (!-l_2_R.isAfterLast()) {
                -l_4_R = new as();
                -l_4_R.valueType = -l_2_R.getInt(-l_2_R.getColumnIndex("c"));
                -l_4_R.bR = -l_2_R.getInt(-l_2_R.getColumnIndex("b"));
                int -l_5_I = -l_2_R.getInt(-l_2_R.getColumnIndex("a"));
                switch (-l_4_R.valueType) {
                    case 1:
                        try {
                            -l_4_R.i = -l_2_R.getInt(-l_2_R.getColumnIndex("d"));
                            break;
                        } catch (Object -l_4_R2) {
                            f.e("KeyValueProfileDB", -l_4_R2.toString());
                            if (-l_2_R != null) {
                                try {
                                    -l_2_R.close();
                                    break;
                                } catch (Object -l_4_R22) {
                                    f.e("KeyValueProfileDB", "cursor.close() crash : " + -l_4_R22.toString());
                                    break;
                                }
                            }
                        } catch (Throwable th) {
                            if (-l_2_R != null) {
                                try {
                                    -l_2_R.close();
                                } catch (Object -l_7_R) {
                                    f.e("KeyValueProfileDB", "cursor.close() crash : " + -l_7_R.toString());
                                }
                            }
                        }
                        break;
                    case 2:
                        -l_4_R22.bS = -l_2_R.getLong(-l_2_R.getColumnIndex("e"));
                        break;
                    case 3:
                        -l_4_R22.bT = -l_2_R.getString(-l_2_R.getColumnIndex("f"));
                        break;
                    case 4:
                        -l_4_R22.bU = -l_2_R.getBlob(-l_2_R.getColumnIndex("g"));
                        break;
                    case 5:
                        -l_4_R22.bV = -l_2_R.getInt(-l_2_R.getColumnIndex("h")) == 1;
                        break;
                    case 6:
                        -l_4_R22.bW = (short) ((short) -l_2_R.getInt(-l_2_R.getColumnIndex("i")));
                        break;
                }
                -l_3_R.add(new jp(-l_4_R22, -l_5_I));
                -l_2_R.moveToNext();
            }
            if (-l_2_R != null) {
                try {
                    -l_2_R.close();
                } catch (Object -l_4_R222) {
                    f.e("KeyValueProfileDB", "cursor.close() crash : " + -l_4_R222.toString());
                }
            }
            return -l_3_R;
        }
        -l_4_R222 = -l_3_R;
        if (-l_2_R != null) {
            try {
                -l_2_R.close();
            } catch (Object -l_5_R) {
                f.e("KeyValueProfileDB", "cursor.close() crash : " + -l_5_R.toString());
            }
        }
        return -l_3_R;
    }

    public static void b(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS pf_key_value_profile_db_table_name");
    }

    public static void b(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        b(sQLiteDatabase);
        a(sQLiteDatabase);
    }

    public static jm cw() {
        if (tj == null) {
            synchronized (lock) {
                if (tj == null) {
                    tj = new jm();
                }
            }
        }
        return tj;
    }

    public boolean a(ArrayList<a> arrayList, ArrayList<Boolean> arrayList2) {
        if (arrayList == null || arrayList.size() <= 0) {
            return true;
        }
        Object -l_3_R = new ArrayList();
        Object -l_4_R = arrayList.iterator();
        while (-l_4_R.hasNext()) {
            a -l_5_R = (a) -l_4_R.next();
            if (!(-l_5_R == null || -l_5_R.ta == null || !(-l_5_R.ta instanceof as))) {
                as -l_6_R = (as) -l_5_R.ta;
                -l_3_R.add(ContentProviderOperation.newDelete(this.oA.an("pf_key_value_profile_db_table_name")).withSelection(String.format("%s = %s", new Object[]{"b", Integer.valueOf(-l_6_R.bR)}), null).build());
            }
        }
        -l_4_R = arrayList.iterator();
        while (-l_4_R.hasNext()) {
            -l_5_R = (a) -l_4_R.next();
            if (!(-l_5_R == null || -l_5_R.ta == null || !(-l_5_R.ta instanceof as))) {
                -l_3_R.add(ContentProviderOperation.newInsert(this.oA.am("pf_key_value_profile_db_table_name")).withValues(a((as) -l_5_R.ta)).build());
            }
        }
        if (-l_3_R != null && -l_3_R.size() > 0) {
            -l_4_R = this.oA.applyBatch(-l_3_R);
            if (-l_4_R == null || -l_4_R.length <= 0 || -l_4_R[0] == null) {
                gr.f("KeyValueProfileDB", "applyBatchOperation fail!!!");
                return false;
            } else if (arrayList2 != null) {
                int -l_5_I = -l_4_R.length / 2;
                for (int -l_6_I = 0; -l_6_I < -l_5_I; -l_6_I++) {
                    if (-l_4_R[-l_6_I] != null) {
                        arrayList2.add(Boolean.valueOf(-l_4_R[-l_6_I].count.intValue() > 0));
                    }
                }
            }
        }
        return true;
    }

    public int ai(int i) {
        return this.oA.delete("pf_key_value_profile_db_table_name", "b = " + i, null);
    }

    public ArrayList<JceStruct> getAll() {
        Object -l_1_R = new ArrayList();
        Object -l_2_R = aP(null);
        if (-l_2_R == null || -l_2_R.size() <= 0) {
            return -l_1_R;
        }
        Object -l_3_R = -l_2_R.iterator();
        while (-l_3_R.hasNext()) {
            -l_1_R.add(((jp) -l_3_R.next()).tr);
        }
        return -l_1_R;
    }
}
