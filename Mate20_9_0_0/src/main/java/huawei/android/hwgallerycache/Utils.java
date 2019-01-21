package huawei.android.hwgallerycache;

import android.util.Log;

public class Utils {
    private static final String TAG = "Utils";

    public static boolean versionInRange(int checkedVersion, String versionRanage) {
        if (versionRanage == null) {
            return false;
        }
        int checkedVersionEnd;
        int versionIndex = versionRanage.indexOf(";");
        String versionPreRange;
        if (versionIndex >= 0) {
            versionPreRange = versionRanage.substring(0, versionIndex);
        } else {
            versionPreRange = versionRanage;
        }
        for (String[] VersionStartAndEnd : versionPreRange.split(",")) {
            String[] VersionStartAndEnd2 = VersionStartAndEnd2.split("-");
            if (VersionStartAndEnd2.length >= 2) {
                try {
                    int checkedVersionStart = Integer.parseInt(VersionStartAndEnd2[0]);
                    checkedVersionEnd = Integer.parseInt(VersionStartAndEnd2[1]);
                    if (checkedVersion >= checkedVersionStart && checkedVersion <= checkedVersionEnd) {
                        return true;
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "version number format error");
                    return false;
                }
            }
        }
        if (versionIndex >= 0) {
            String[] versionPostArray = versionRanage.substring(versionIndex + 1).split(",");
            checkedVersionEnd = versionPostArray.length;
            int i = 0;
            while (i < checkedVersionEnd) {
                int specialVersion = 0;
                try {
                    if (checkedVersion == Integer.parseInt(versionPostArray[i])) {
                        return true;
                    }
                    i++;
                } catch (NumberFormatException e2) {
                    Log.e(TAG, "version number format error");
                    return false;
                }
            }
        }
        return false;
    }
}
