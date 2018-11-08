package tmsdkobf;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class jg implements kg {
    private ContentResolver mContentResolver;
    private long mr;

    public jg(Context context, long j) {
        this.mContentResolver = context.getContentResolver();
        this.mr = j;
    }

    private void a(Exception exception) {
        mb.o("SysDBService", exception.getMessage());
    }

    public int delete(Uri uri, String str, String[] strArr) {
        mb.d("SysDBService", "delete|caller=" + this.mr + "|uri=" + uri.toString());
        int -l_4_I = 0;
        try {
            -l_4_I = this.mContentResolver.delete(uri, str, strArr);
        } catch (Object -l_5_R) {
            a(-l_5_R);
        }
        return -l_4_I;
    }

    public Uri insert(Uri uri, ContentValues contentValues) {
        mb.d("SysDBService", "insert|caller=" + this.mr + "|uri=" + uri.toString());
        Object -l_3_R = null;
        try {
            -l_3_R = this.mContentResolver.insert(uri, contentValues);
        } catch (Object -l_4_R) {
            a(-l_4_R);
        }
        return -l_3_R;
    }

    public Cursor query(Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        mb.d("SysDBService", "query|caller=" + this.mr + "|uri=" + uri.toString());
        Cursor -l_6_R = null;
        try {
            -l_6_R = this.mContentResolver.query(uri, strArr, str, strArr2, str2);
        } catch (Object -l_7_R) {
            a(-l_7_R);
        }
        return -l_6_R == null ? null : new je(-l_6_R);
    }
}
