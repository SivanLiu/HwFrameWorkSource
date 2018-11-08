package tmsdkobf;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;
import tmsdk.common.TMSDKContext;

public class gm {
    private jv om = ((kf) fj.D(9)).ap("MeriExtProvider");

    public static class a {
        public String mData;
        public int mType;

        public String toString() {
            return this.mData != null ? "mType: " + this.mType + " mData: " + this.mData : "null";
        }
    }

    private a N(int i) {
        a aVar = null;
        Cursor cursor = null;
        try {
            cursor = this.om.a("gd_info", null, String.format("%s='%s'", new Object[]{"type", Integer.valueOf(i)}), null, null);
            if (cursor != null && cursor.moveToNext()) {
                aVar = a(cursor);
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
        return aVar;
    }

    private a a(Cursor cursor) {
        Object -l_2_R = new a();
        -l_2_R.mType = cursor.getInt(1);
        -l_2_R.mData = cursor.getString(2);
        return -l_2_R;
    }

    private boolean a(a aVar) {
        long -l_2_J = this.om.a("gd_info", d(aVar));
        this.om.close();
        return !((-l_2_J > 0 ? 1 : (-l_2_J == 0 ? 0 : -1)) <= 0);
    }

    private boolean b(a aVar) {
        if (aVar != null) {
            return N(aVar.mType) != null ? c(aVar) : a(aVar);
        } else {
            return false;
        }
    }

    private boolean c(a aVar) {
        int -l_3_I = this.om.update("gd_info", d(aVar), "type=?", new String[]{Long.toString((long) aVar.mType)});
        this.om.close();
        return -l_3_I > 0;
    }

    private ContentValues d(a aVar) {
        Object -l_2_R = new ContentValues();
        -l_2_R.put("type", Integer.valueOf(aVar.mType));
        -l_2_R.put("data", aVar.mData);
        return -l_2_R;
    }

    public String L(int i) {
        Object -l_2_R = N(i);
        return (-l_2_R == null || TextUtils.isEmpty(-l_2_R.mData)) ? null : kk.d(TMSDKContext.getApplicaionContext(), -l_2_R.mData);
    }

    public byte[] M(int i) {
        Object -l_2_R = N(i);
        return (-l_2_R == null || TextUtils.isEmpty(-l_2_R.mData)) ? null : com.tencent.tcuser.util.a.at(-l_2_R.mData);
    }

    public boolean a(int i, byte[] bArr) {
        Object -l_3_R = com.tencent.tcuser.util.a.bytesToHexString(bArr);
        Object -l_4_R = new a();
        -l_4_R.mType = i;
        -l_4_R.mData = -l_3_R;
        return b(-l_4_R);
    }

    public boolean b(int i, String str) {
        Object -l_3_R = kk.c(TMSDKContext.getApplicaionContext(), str);
        Object -l_4_R = new a();
        -l_4_R.mType = i;
        -l_4_R.mData = -l_3_R;
        return b(-l_4_R);
    }

    public int g(int i, int i2) {
        Object -l_3_R = N(i);
        return (-l_3_R == null || TextUtils.isEmpty(-l_3_R.mData)) ? i2 : Integer.parseInt(kk.d(TMSDKContext.getApplicaionContext(), -l_3_R.mData));
    }
}
