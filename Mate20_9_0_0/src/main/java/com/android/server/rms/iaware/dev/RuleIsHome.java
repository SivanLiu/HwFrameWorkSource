package com.android.server.rms.iaware.dev;

import android.rms.iaware.AwareConfig.SubItem;
import android.rms.iaware.AwareLog;
import com.android.server.am.HwActivityManagerService;
import java.util.Map;

public class RuleIsHome extends RuleBase {
    private static final int MATCH_PARAM_NUM = 1;
    private static final String TAG = "RuleIsHome";

    public boolean fillRuleInfo(SubItem subItem) {
        if (subItem == null) {
            return false;
        }
        Map<String, String> properties = subItem.getProperties();
        if (properties == null) {
            return false;
        }
        this.mItemValue.putAll(properties);
        return true;
    }

    public boolean isMatch(Object... obj) {
        if (obj == null || obj.length < 1) {
            AwareLog.e(TAG, "invalid input params, error!");
            return false;
        }
        Object processName = obj[0];
        if (processName == null) {
            AwareLog.e(TAG, "procssName is null, error!");
            return false;
        } else if (processName instanceof String) {
            HwActivityManagerService hwAMS = HwActivityManagerService.self();
            if (hwAMS == null) {
                AwareLog.e(TAG, "hwAMS is null, fatal error!");
                return false;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("input processName :");
            stringBuilder.append((String) processName);
            AwareLog.d(str, stringBuilder.toString());
            return hwAMS.isLauncher((String) processName);
        } else {
            AwareLog.e(TAG, "procssName is not String, error!");
            return false;
        }
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("RuleIsHome, mItemValue : [ ");
        s.append(this.mItemValue);
        s.append(" ]");
        return s.toString();
    }
}
