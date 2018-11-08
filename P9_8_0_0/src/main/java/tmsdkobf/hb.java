package tmsdkobf;

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import java.util.ArrayList;

final class hb implements jv {
    private long mr;
    private ContentProvider pi;
    private String pj;
    private String pk;

    public hb(long j, ContentProvider contentProvider, String str) {
        this.mr = j;
        this.pj = str;
        this.pk = "content://" + str;
        this.pi = contentProvider;
    }

    private void a(Exception exception, int i) {
        mb.o("RawDBService", exception.getMessage());
    }

    public long a(String str, ContentValues contentValues) {
        mb.d("RawDBService", "insert|caller=" + this.mr + "|authority=" + this.pj + "|table=" + str);
        long -l_4_J = -1;
        try {
            Object -l_6_R = this.pi.insert(Uri.parse(this.pk + "/insert" + "?" + str), contentValues);
            if (-l_6_R != null) {
                -l_4_J = Long.parseLong(-l_6_R.getQuery());
            }
        } catch (Exception -l_6_R2) {
            a(-l_6_R2, 2);
        }
        return -l_4_J;
    }

    public Cursor a(String str, String[] strArr, String str2, String[] strArr2, String str3) {
        mb.d("RawDBService", "query|caller=" + this.mr + "|authority=" + this.pj + "|table=" + str);
        Cursor -l_8_R = null;
        try {
            -l_8_R = this.pi.query(Uri.parse(this.pk + "/query" + "_" + "1-" + "?" + str), strArr, str2, strArr2, str3);
        } catch (Exception -l_9_R) {
            a(-l_9_R, 1);
        }
        return -l_8_R == null ? null : new je(-l_8_R);
    }

    public Cursor al(String str) {
        mb.d("RawDBService", "query|caller=" + this.mr + "|authority=" + this.pj + "|sql=" + str);
        Cursor -l_4_R = null;
        try {
            -l_4_R = this.pi.query(Uri.parse(this.pk + "/rawquery" + "_" + "1-" + "?" + Uri.encode(str)), null, null, null, null);
        } catch (Exception -l_5_R) {
            a(-l_5_R, 1);
        }
        return -l_4_R == null ? null : new je(-l_4_R);
    }

    public Uri am(String str) {
        return Uri.parse("content://" + this.pj + "/insert" + "?" + str);
    }

    public Uri an(String str) {
        return Uri.parse("content://" + this.pj + "/delete" + "?" + str);
    }

    public Uri ao(String str) {
        return Uri.parse("content://" + this.pj + "/update" + "?" + str);
    }

    public ContentProviderResult[] applyBatch(ArrayList<ContentProviderOperation> arrayList) {
        mb.n("RawDBService", "applyBatch|caller=" + this.mr + "|authority=" + this.pj);
        Object -l_2_R = null;
        try {
            -l_2_R = this.pi.applyBatch(arrayList);
        } catch (Exception -l_3_R) {
            a(-l_3_R, 7);
        }
        return -l_2_R;
    }

    public void close() {
    }

    public int delete(String str, String str2, String[] strArr) {
        mb.d("RawDBService", "delete|caller=" + this.mr + "|authority=" + this.pj + "|table=" + str);
        int -l_5_I = 0;
        try {
            -l_5_I = this.pi.delete(Uri.parse(this.pk + "/delete" + "?" + str), str2, strArr);
        } catch (Exception -l_6_R) {
            a(-l_6_R, 3);
        }
        return -l_5_I;
    }

    public int update(String str, ContentValues contentValues, String str2, String[] strArr) {
        mb.d("RawDBService", "update|caller=" + this.mr + "|authority=" + this.pj + "|table=" + str);
        int -l_6_I = 0;
        try {
            -l_6_I = this.pi.update(Uri.parse(this.pk + "/update" + "?" + str), contentValues, str2, strArr);
        } catch (Exception -l_7_R) {
            a(-l_7_R, 4);
        }
        return -l_6_I;
    }
}
