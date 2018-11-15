package com.android.server.rms.iaware.dev;

import android.rms.iaware.AwareConfig.SubItem;
import android.rms.iaware.AwareLog;
import java.util.Map;

public class RuleDefault extends RuleBase {
    private static final String TAG = "RuleDefault";

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
        AwareLog.d(TAG, "Default match success.");
        return true;
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("RuleDefault, mItemValue : [ ");
        s.append(this.mItemValue);
        s.append(" ]");
        return s.toString();
    }
}
