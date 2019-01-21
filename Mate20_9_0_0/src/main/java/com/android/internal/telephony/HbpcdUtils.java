package com.android.internal.telephony;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.telephony.Rlog;
import com.android.internal.telephony.HbpcdLookup.ArbitraryMccSidMatch;
import com.android.internal.telephony.HbpcdLookup.MccIdd;
import com.android.internal.telephony.HbpcdLookup.MccLookup;
import com.android.internal.telephony.HbpcdLookup.MccSidConflicts;
import com.android.internal.telephony.HbpcdLookup.MccSidRange;

public final class HbpcdUtils {
    private static final boolean DBG = false;
    private static final String LOG_TAG = "HbpcdUtils";
    private ContentResolver resolver = null;

    public HbpcdUtils(Context context) {
        this.resolver = context.getContentResolver();
    }

    public int getMcc(int sid, int tz, int DSTflag, boolean isNitzTimeZone) {
        int tmpMcc;
        int i = sid;
        int i2 = tz;
        int i3 = DSTflag;
        String[] projection2 = new String[]{"MCC"};
        ContentResolver contentResolver = this.resolver;
        Uri uri = ArbitraryMccSidMatch.CONTENT_URI;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SID=");
        stringBuilder.append(i);
        Cursor c2 = contentResolver.query(uri, projection2, stringBuilder.toString(), null, null);
        if (c2 != null) {
            if (c2.getCount() == 1) {
                c2.moveToFirst();
                tmpMcc = c2.getInt(0);
                c2.close();
                return tmpMcc;
            }
            c2.close();
        }
        String[] projection3 = new String[]{"MCC"};
        ContentResolver contentResolver2 = this.resolver;
        Uri uri2 = MccSidConflicts.CONTENT_URI;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("SID_Conflict=");
        stringBuilder2.append(i);
        stringBuilder2.append(" and (((");
        stringBuilder2.append(MccLookup.GMT_OFFSET_LOW);
        stringBuilder2.append("<=");
        stringBuilder2.append(i2);
        stringBuilder2.append(") and (");
        stringBuilder2.append(i2);
        stringBuilder2.append("<=");
        stringBuilder2.append(MccLookup.GMT_OFFSET_HIGH);
        stringBuilder2.append(") and (0=");
        stringBuilder2.append(i3);
        stringBuilder2.append(")) or ((");
        stringBuilder2.append(MccLookup.GMT_DST_LOW);
        stringBuilder2.append("<=");
        stringBuilder2.append(i2);
        stringBuilder2.append(") and (");
        stringBuilder2.append(i2);
        stringBuilder2.append("<=");
        stringBuilder2.append(MccLookup.GMT_DST_HIGH);
        stringBuilder2.append(") and (1=");
        stringBuilder2.append(i3);
        stringBuilder2.append(")))");
        Cursor c3 = contentResolver2.query(uri2, projection3, stringBuilder2.toString(), null, null);
        if (c3 != null) {
            int c3Counter = c3.getCount();
            if (c3Counter > 0) {
                if (c3Counter > 1) {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("something wrong, get more results for 1 conflict SID: ");
                    stringBuilder3.append(c3);
                    Rlog.w(str, stringBuilder3.toString());
                }
                c3.moveToFirst();
                tmpMcc = c3.getInt(0);
                if (!isNitzTimeZone) {
                    tmpMcc = 0;
                }
                c3.close();
                return tmpMcc;
            }
            c3.close();
            c3.close();
        }
        String[] projection5 = new String[]{"MCC"};
        ContentResolver contentResolver3 = this.resolver;
        Uri uri3 = MccSidRange.CONTENT_URI;
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("SID_Range_Low<=");
        stringBuilder4.append(i);
        stringBuilder4.append(" and ");
        stringBuilder4.append(MccSidRange.RANGE_HIGH);
        stringBuilder4.append(">=");
        stringBuilder4.append(i);
        Cursor c5 = contentResolver3.query(uri3, projection5, stringBuilder4.toString(), null, null);
        if (c5 != null) {
            if (c5.getCount() > 0) {
                c5.moveToFirst();
                tmpMcc = c5.getInt(0);
                c5.close();
                return tmpMcc;
            }
            c5.close();
        }
        return 0;
    }

    public String getIddByMcc(int mcc) {
        String idd = "";
        Cursor c = null;
        String[] projection = new String[]{MccIdd.IDD};
        ContentResolver contentResolver = this.resolver;
        Uri uri = MccIdd.CONTENT_URI;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("MCC=");
        stringBuilder.append(mcc);
        Cursor cur = contentResolver.query(uri, projection, stringBuilder.toString(), null, null);
        if (cur != null) {
            if (cur.getCount() > 0) {
                cur.moveToFirst();
                idd = cur.getString(0);
            }
            cur.close();
        }
        if (c != null) {
            c.close();
        }
        return idd;
    }
}
