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
import java.util.HashSet;
import java.util.Set;
import tmsdk.common.TMSDKContext;

public class jh extends ContentProvider {
    private final int PHONE = 1;
    private final String TAG = "PiDBProvider";
    private String mName;
    private int mType = 1;
    protected Set<String> ps;
    public final Object sS = new Object();
    private final int sT = 2;
    private SQLiteOpenHelper sU;
    private hg sV;
    private int sW;
    private a sX;
    private String sY;

    public interface a {
        void onCreate(SQLiteDatabase sQLiteDatabase);

        void onDowngrade(SQLiteDatabase sQLiteDatabase, int i, int i2);

        void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2);
    }

    public jh(String str, int i, a aVar) {
        this.mName = str;
        this.sW = i;
        this.sX = aVar;
    }

    protected int a(SQLiteDatabase sQLiteDatabase, String str, ContentValues contentValues, String str2, String[] strArr) {
        int -l_6_I = -1;
        if (sQLiteDatabase == null) {
            return -1;
        }
        try {
            return sQLiteDatabase.update(str, contentValues, str2, strArr);
        } catch (Object -l_7_R) {
            -l_7_R.printStackTrace();
            mb.o("PiDBProvider", "update fail!");
            return -l_6_I;
        }
    }

    protected int a(SQLiteDatabase sQLiteDatabase, String str, String str2, String[] strArr) {
        int -l_5_I = -1;
        if (sQLiteDatabase == null) {
            return -1;
        }
        try {
            return sQLiteDatabase.delete(str, str2, strArr);
        } catch (Object -l_6_R) {
            -l_6_R.printStackTrace();
            mb.o("PiDBProvider", "delete fail!");
            return -l_5_I;
        }
    }

    protected long a(SQLiteDatabase sQLiteDatabase, String str, ContentValues contentValues) {
        long -l_4_J = -1;
        if (sQLiteDatabase == null) {
            return -1;
        }
        try {
            return sQLiteDatabase.insert(str, null, contentValues);
        } catch (Object -l_6_R) {
            -l_6_R.printStackTrace();
            mb.o("PiDBProvider", "insert fail!");
            return -l_4_J;
        }
    }

    protected Cursor a(SQLiteDatabase sQLiteDatabase, String str) {
        Object -l_3_R = null;
        if (sQLiteDatabase == null) {
            return null;
        }
        try {
            return sQLiteDatabase.rawQuery(str, null);
        } catch (Object -l_4_R) {
            -l_4_R.printStackTrace();
            mb.o("PiDBProvider", "rawQuery fail!");
            return -l_3_R;
        }
    }

    protected Cursor a(SQLiteDatabase sQLiteDatabase, String str, String[] strArr, String str2, String[] strArr2, String str3) {
        Object -l_7_R = null;
        if (sQLiteDatabase == null) {
            return null;
        }
        try {
            return sQLiteDatabase.query(str, strArr, str2, strArr2, null, null, str3);
        } catch (Object -l_8_R) {
            -l_8_R.printStackTrace();
            mb.o("PiDBProvider", "query fail!");
            return -l_7_R;
        }
    }

    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> arrayList) throws OperationApplicationException {
        synchronized (this.sS) {
            ContentProviderResult[] contentProviderResultArr = null;
            Object -l_4_R = getDatabase();
            if (-l_4_R == null) {
                return null;
            }
            -l_4_R.beginTransaction();
            try {
                contentProviderResultArr = super.applyBatch(arrayList);
                -l_4_R.setTransactionSuccessful();
                if (-l_4_R.inTransaction()) {
                    -l_4_R.endTransaction();
                }
            } catch (Object -l_5_R) {
                -l_5_R.printStackTrace();
                if (-l_4_R.inTransaction()) {
                    -l_4_R.endTransaction();
                }
                return contentProviderResultArr;
            } catch (Throwable th) {
                if (-l_4_R.inTransaction()) {
                    -l_4_R.endTransaction();
                }
            }
        }
    }

    protected void aq(String str) {
        if (this.ps == null) {
            this.ps = new HashSet();
        }
        this.ps.add(str);
    }

    protected void ar(String str) {
        if (this.ps != null) {
            this.ps.remove(str);
        }
    }

    protected long b(SQLiteDatabase sQLiteDatabase, String str, ContentValues contentValues) {
        long -l_4_J = -1;
        if (sQLiteDatabase == null) {
            return -1;
        }
        try {
            return sQLiteDatabase.replace(str, null, contentValues);
        } catch (Object -l_6_R) {
            -l_6_R.printStackTrace();
            mb.o("PiDBProvider", "replace fail!");
            return -l_4_J;
        }
    }

    protected void b(SQLiteDatabase sQLiteDatabase, String str) {
        if (sQLiteDatabase != null) {
            try {
                sQLiteDatabase.execSQL(str);
            } catch (Object -l_3_R) {
                -l_3_R.printStackTrace();
                mb.o("PiDBProvider", "execSQL fail!");
            }
        }
    }

    protected void bg() {
        if (this.ps == null || this.ps.size() <= 0) {
            if (this.sU != null && this.mType == 1) {
                this.sU.close();
            } else if (this.sV != null && this.mType == 2) {
                this.sV.close();
            }
        }
    }

    public int delete(Uri uri, String str, String[] strArr) {
        synchronized (this.sS) {
            Object -l_5_R = uri.getPath();
            if ("/delete".equals(-l_5_R)) {
                int a = a(getDatabase(), uri.getQuery(), str, strArr);
                return a;
            } else if ("/execSQL".equals(-l_5_R)) {
                b(getDatabase(), uri.getQuery());
                return 0;
            } else if ("/closecursor".equals(-l_5_R)) {
                ar(uri.getQuery());
                return 0;
            } else if ("/close".equals(-l_5_R)) {
                bg();
                return 0;
            } else {
                mb.o("PiDBProvider", "error delete: " + uri.toString());
                throw new IllegalArgumentException("the uri " + uri.toString() + "is error");
            }
        }
    }

    protected SQLiteDatabase getDatabase() {
        Object -l_1_R = TMSDKContext.getApplicaionContext();
        if (-l_1_R == null) {
            mb.n("PiDBProvider", "ProviderUtil.getForeContext()： " + -l_1_R);
            return null;
        } else if (this.mType != 1) {
            if (this.sV == null) {
                this.sV = new hg(this, -l_1_R, this.mName, null, this.sW, this.sY) {
                    final /* synthetic */ jh sZ;

                    public void onCreate(SQLiteDatabase sQLiteDatabase) {
                        mb.n("DBService", "SDCardSQLiteDatabase|onCreate|name=" + this.sZ.mName + "|version=" + this.sZ.sW);
                        this.sZ.sX.onCreate(sQLiteDatabase);
                    }

                    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
                        mb.s("DBService", "SDCardSQLiteDatabase|onUpgrade|name=" + this.sZ.mName + "|oldversion=" + i + "|newVersion=" + i2);
                        this.sZ.sX.onUpgrade(sQLiteDatabase, i, i2);
                    }
                };
            }
            return this.sV.getWritableDatabase();
        } else {
            if (this.sU == null) {
                this.sU = new SQLiteOpenHelper(this, -l_1_R, this.mName, null, this.sW) {
                    final /* synthetic */ jh sZ;

                    public void onCreate(SQLiteDatabase sQLiteDatabase) {
                        mb.n("DBService", "SQLiteDatabase|onCreate|name=" + this.sZ.mName + "|version=" + this.sZ.sW);
                        this.sZ.sX.onCreate(sQLiteDatabase);
                    }

                    public void onDowngrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
                        mb.s("DBService", "SQLiteDatabase|onDowngrade|name=" + this.sZ.mName + "|oldversion=" + i + "|newVersion=" + i2);
                        this.sZ.sX.onDowngrade(sQLiteDatabase, i, i2);
                    }

                    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
                        mb.s("DBService", "SQLiteDatabase|onUpgrade|name=" + this.sZ.mName + "|oldversion=" + i + "|newVersion=" + i2);
                        this.sZ.sX.onUpgrade(sQLiteDatabase, i, i2);
                    }
                };
            }
            return this.sU.getWritableDatabase();
        }
    }

    public String getType(Uri uri) {
        return null;
    }

    public Uri insert(Uri uri, ContentValues contentValues) {
        synchronized (this.sS) {
            if ("/insert".equals(uri.getPath())) {
                Object -l_8_R = Uri.parse("content://" + uri.getAuthority() + "?" + a(getDatabase(), uri.getQuery(), contentValues));
                return -l_8_R;
            }
            if ("/replace".equals(uri.getPath())) {
                -l_8_R = Uri.parse("content://" + uri.getAuthority() + "?" + b(getDatabase(), uri.getQuery(), contentValues));
                return -l_8_R;
            }
            mb.o("PiDBProvider", "error insert: " + uri.toString());
            throw new IllegalArgumentException("the uri " + uri.toString() + "is error");
        }
    }

    public boolean onCreate() {
        return false;
    }

    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        synchronized (this.sS) {
            Object -l_7_R = uri.getPath();
            int -l_8_I = -l_7_R.indexOf("_");
            if (-l_8_I != -1) {
                aq(-l_7_R.substring(-l_8_I + 1));
                -l_7_R = -l_7_R.substring(0, -l_8_I);
            }
            Cursor a;
            if ("/query".equals(-l_7_R)) {
                a = a(getDatabase(), uri.getQuery(), strArr, str, strArr2, str2);
                return a;
            } else if ("/rawquery".equals(-l_7_R)) {
                a = a(getDatabase(), uri.getQuery());
                return a;
            } else {
                mb.o("PiDBProvider", "error query: " + uri.toString());
                throw new IllegalArgumentException("the uri " + uri.toString() + "is error");
            }
        }
    }

    public int update(Uri uri, ContentValues contentValues, String str, String[] strArr) {
        int a;
        synchronized (this.sS) {
            if ("/update".equals(uri.getPath())) {
                a = a(getDatabase(), uri.getQuery(), contentValues, str, strArr);
            } else {
                mb.o("PiDBProvider", "error update: " + uri.toString());
                throw new IllegalArgumentException("the uri " + uri.toString() + "is error");
            }
        }
        return a;
    }
}
