package com.huawei.android.hwaps;

import android.app.ActivityThread;
import android.app.HwApsInterface;
import android.aps.ApsColorInfo;
import android.aps.IApsManager;
import android.common.HwFrameworkFactory;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemProperties;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class ColorPlusController implements IColorPlusController {
    private static final String QUERY_RESULT_COLOR_LIST = "content://com.huawei.android.hwaps.ApsProvider/QueryResultColorPlusInfo";
    private static final String TAG = "ColorPlusController";
    private boolean isFirstCalling = true;
    private boolean isSupportApsColorPlus = false;

    public boolean isSupportApsColorPlus(String pkgName) {
        if (!this.isFirstCalling) {
            return this.isSupportApsColorPlus;
        }
        this.isFirstCalling = false;
        if (1 == SystemProperties.getInt("sys.aps.pstheme", 0) && 16777216 == (SystemProperties.getInt("sys.aps.support", 0) & 16777216)) {
            IApsManager mApsManager = HwFrameworkFactory.getApsManager();
            if (!(pkgName == null || mApsManager == null || !mApsManager.isSupportApsColorPlus(pkgName))) {
                String str;
                StringBuilder stringBuilder;
                try {
                    ArrayList<ApsColorInfo> apsColorInfoList = new ArrayList(getQueryResultColorList(ActivityThread.currentApplication()));
                    if (!apsColorInfoList.isEmpty()) {
                        HwApsInterface.nativeSetColorPlusList(apsColorInfoList, pkgName);
                    }
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("ColorPlus init, ");
                    stringBuilder.append(pkgName);
                    stringBuilder.append(" is ");
                    stringBuilder.append(apsColorInfoList.isEmpty() ? "not support" : "support");
                    Log.i(str, stringBuilder.toString());
                } catch (Exception e) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("isSupportApsColorPlus error, ");
                    stringBuilder.append(e);
                    Log.e(str, stringBuilder.toString());
                }
                this.isSupportApsColorPlus = true;
            }
            return this.isSupportApsColorPlus;
        }
        this.isSupportApsColorPlus = false;
        return this.isSupportApsColorPlus;
    }

    /* JADX WARNING: Missing block: B:10:0x0051, code skipped:
            if (r1 != null) goto L_0x0053;
     */
    /* JADX WARNING: Missing block: B:11:0x0053, code skipped:
            r1.close();
     */
    /* JADX WARNING: Missing block: B:16:0x0074, code skipped:
            if (r1 == null) goto L_0x0077;
     */
    /* JADX WARNING: Missing block: B:17:0x0077, code skipped:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static List<ApsColorInfo> getQueryResultColorList(Context context) {
        String[] projection = new String[]{"type", "srcColor", "dstColor", "actName"};
        List<ApsColorInfo> apsColorList = new ArrayList();
        Cursor cursor = null;
        try {
            ContentResolver resolver = context.getContentResolver();
            if (resolver != null) {
                cursor = resolver.query(Uri.parse(QUERY_RESULT_COLOR_LIST), projection, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    do {
                        apsColorList.add(new ApsColorInfo(cursor.getInt(0), cursor.getInt(1), cursor.getInt(2), cursor.getString(3)));
                    } while (cursor.moveToNext());
                }
            }
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getQueryResultColorList e: ");
            stringBuilder.append(e.getMessage());
            Log.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
