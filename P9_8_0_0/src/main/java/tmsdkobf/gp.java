package tmsdkobf;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;
import tmsdk.common.utils.f;

public class gp {
    private static Object lock = new Object();
    private static gp oB;
    private jv oA;
    byte oC;

    public static class a {
        public int bK = 0;
        byte[] data;
        public int oD = -1;
        private ar oE = null;
        public int ox = -1;

        public a(byte[] bArr, int i, int i2, int i3) {
            this.ox = i2;
            this.data = bArr;
            this.oD = i;
            this.bK = i3;
        }

        public ar aY() {
            if (this.oE == null && this.oD == 0 && this.data != null && this.data.length > 0) {
                try {
                    this.oE = gr.f(this.data);
                } catch (Object -l_1_R) {
                    f.e("ProfileQueue", -l_1_R);
                }
            }
            return this.oE;
        }
    }

    private gp() {
        this.oA = null;
        this.oC = (byte) 0;
        this.oA = ((kf) fj.D(9)).ap("QQSecureProvider");
    }

    private String Q(int i) {
        return String.format("%s = %s", new Object[]{"c", Integer.valueOf(i)});
    }

    public static void a(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS profile_fifo_upload_queue (a INTEGER PRIMARY KEY,c INTEGER,d INTEGER,e INTEGER,b BLOB)");
    }

    public static void a(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        if (i < 15) {
            a(sQLiteDatabase);
        }
    }

    public static gp aV() {
        if (oB == null) {
            synchronized (lock) {
                if (oB == null) {
                    oB = new gp();
                }
            }
        }
        return oB;
    }

    private int b(byte[] bArr, int i, int i2) {
        int -l_6_I = 1;
        Object -l_4_R = new ContentValues();
        if (bArr != null && bArr.length > 0) {
            -l_4_R.put("b", bArr);
        }
        -l_4_R.put("e", Integer.valueOf(i));
        if (i2 > 0 && i2 < 5) {
            -l_4_R.put("c", Integer.valueOf(i2));
        }
        int -l_5_I = gq.aZ().ba();
        -l_4_R.put("d", Integer.valueOf(-l_5_I));
        if ((this.oA.a("profile_fifo_upload_queue", -l_4_R) < 0 ? 1 : 0) != 0) {
            -l_6_I = 0;
        }
        if (-l_6_I == 0) {
            return -1;
        }
        gq.aZ().bb();
        return -l_5_I;
    }

    public static void b(SQLiteDatabase sQLiteDatabase) {
        sQLiteDatabase.execSQL("DROP TABLE IF EXISTS profile_fifo_upload_queue");
    }

    public static void b(SQLiteDatabase sQLiteDatabase, int i, int i2) {
        b(sQLiteDatabase);
        a(sQLiteDatabase);
    }

    private a c(String str, String str2) {
        Object -l_3_R = e(str, str2);
        return (-l_3_R != null && -l_3_R.size() > 0) ? (a) -l_3_R.get(0) : null;
    }

    private ArrayList<a> e(String str, String str2) {
        Object -l_5_R;
        Cursor cursor = null;
        Object -l_4_R = new ArrayList();
        try {
            cursor = this.oA.a("profile_fifo_upload_queue", null, str, null, str2);
            if (cursor != null) {
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    -l_4_R.add(new a(cursor.getBlob(cursor.getColumnIndex("b")), cursor.getInt(cursor.getColumnIndex("e")), cursor.getInt(cursor.getColumnIndex("d")), cursor.getInt(cursor.getColumnIndex("c"))));
                    cursor.moveToNext();
                }
                if (cursor != null) {
                    try {
                        cursor.close();
                    } catch (Object -l_5_R2) {
                        f.e("ProfileQueue", "cursor.close() crash : " + -l_5_R2.toString());
                    }
                }
                return -l_4_R;
            }
            -l_5_R2 = -l_4_R;
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Object -l_6_R) {
                    f.e("ProfileQueue", "cursor.close() crash : " + -l_6_R.toString());
                }
            }
            return -l_4_R;
        } catch (Object -l_5_R22) {
            f.e("ProfileQueue", -l_5_R22.toString());
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Object -l_5_R222) {
                    f.e("ProfileQueue", "cursor.close() crash : " + -l_5_R222.toString());
                }
            }
        } catch (Throwable th) {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Object -l_10_R) {
                    f.e("ProfileQueue", "cursor.close() crash : " + -l_10_R.toString());
                }
            }
        }
    }

    public boolean O(int i) {
        return this.oA.delete("profile_fifo_upload_queue", Q(i), null) > 0;
    }

    public byte[] P(int i) {
        Object -l_2_R = "d = " + i;
        Object -l_3_R = c(-l_2_R, null);
        if (-l_3_R == null) {
            return null;
        }
        int -l_4_I = this.oA.delete("profile_fifo_upload_queue", -l_2_R, null);
        if (-l_4_I > 1) {
            gr.f("ProfileUpload", "delete error! 多于一行被delete了！！");
        } else if (-l_4_I == 0) {
            return null;
        }
        return -l_3_R.data;
    }

    public int a(byte[] bArr, int i) {
        return b(bArr, 0, i);
    }

    public int aW() {
        return b(null, 1, 0);
    }

    public List<a> aX() {
        return e(null, "d");
    }
}
