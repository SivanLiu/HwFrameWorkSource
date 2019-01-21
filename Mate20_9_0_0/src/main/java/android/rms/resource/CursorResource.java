package android.rms.resource;

import android.rms.HwSysCountRes;
import android.rms.utils.Utils;
import android.util.Log;

public final class CursorResource extends HwSysCountRes {
    private static final String TAG = "RMS.CursorResource";
    private static CursorResource mCursorResource = null;

    private CursorResource() {
        super(16, TAG);
    }

    public static synchronized CursorResource getInstance() {
        synchronized (CursorResource.class) {
            if (mCursorResource == null) {
                mCursorResource = new CursorResource();
                if (Utils.DEBUG) {
                    Log.d(TAG, "getInstance create new resource");
                }
            }
            if (mCursorResource.getConfig()) {
                if (Utils.DEBUG) {
                    Log.d(TAG, "getInstance getConfig");
                }
                CursorResource cursorResource = mCursorResource;
                return cursorResource;
            }
            if (Utils.DEBUG) {
                Log.d(TAG, "RMS not ready!");
            }
            return null;
        }
    }

    public int acquire(int callingUid, String pkg, int processTpye, int count) {
        int strategy = 1;
        int typeID = super.getTypeId(callingUid, 0, processTpye);
        if (!(this.mResourceConfig == null || !isResourceCountOverload(callingUid, pkg, typeID, count) || isInWhiteList(parsePackageNameFromToken(pkg), 0))) {
            String str;
            StringBuilder stringBuilder;
            strategy = this.mResourceConfig[typeID].getResourceStrategy();
            if (Utils.DEBUG) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("getOverloadStrategy CountOverload = ");
                stringBuilder.append(strategy);
                Log.d(str, stringBuilder.toString());
            }
            if (typeID == 2 && (Utils.DEBUG || Utils.HWFLOW)) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("process uid ");
                stringBuilder.append(callingUid);
                stringBuilder.append(" open too many cursor ");
                stringBuilder.append(pkg);
                Log.i(str, stringBuilder.toString());
            }
        }
        return strategy;
    }

    protected boolean needUpdateWhiteList() {
        return false;
    }

    private String parsePackageNameFromToken(String token) {
        String pkgName = "";
        if (token != null && token.contains(";") && token.contains("-")) {
            String[] list = token.split(";");
            int n = list[0].lastIndexOf("-");
            if (n > 0) {
                pkgName = list[0].substring(0, n);
            }
        }
        if (Utils.DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Current parsed pkgName:");
            stringBuilder.append(pkgName);
            stringBuilder.append(" from token:");
            stringBuilder.append(token);
            Log.d(str, stringBuilder.toString());
        }
        return pkgName;
    }
}
