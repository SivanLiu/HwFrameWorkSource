package com.android.server.mtm.iaware.appmng.rule;

import android.app.mtm.iaware.appmng.AppMngConstant.BroadcastSource;
import android.content.Intent;
import android.util.ArrayMap;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import com.android.server.mtm.iaware.appmng.rule.RuleNode.XmlValue;
import com.android.server.mtm.iaware.appmng.rule.RuleParserUtil.BroadcastTag;
import com.android.server.mtm.iaware.srms.AwareBroadcastPolicy.BrCtrlType;

public class BroadcastMngRule extends Config {
    private static final int DEFAULT_POLICY = 0;
    private static final String TAG = "BroadcastMngRule";
    private static final int UNINIT_VALUE = -1;

    public BroadcastMngRule(ArrayMap<String, String> prop, RuleNode rules) {
        super(prop, rules);
    }

    public int apply(BroadcastSource source, String id, Intent intent, AwareProcessInfo processInfo) {
        if (processInfo == null) {
            return BrCtrlType.NONE.ordinal();
        }
        return toApply(source, id, intent, processInfo, this.mRules, AppMngRule.getTriState((String) processInfo.mProcInfo.mPackageName.get(0)));
    }

    public ArrayMap<String, RuleNode> getBrRules() {
        RuleNode node = this.mRules;
        if (node == null || !node.hasChild()) {
            return null;
        }
        return node.getChilds().getStringMap();
    }

    public int updateApply(BroadcastSource source, String id, Intent intent, AwareProcessInfo processInfo, RuleNode node) {
        if (processInfo == null) {
            return BrCtrlType.NONE.ordinal();
        }
        return toApply(source, id, intent, processInfo, node, AppMngRule.getTriState((String) processInfo.mProcInfo.mPackageName.get(0)));
    }

    private int toApply(BroadcastSource source, String id, Intent intent, AwareProcessInfo processInfo, RuleNode node, int tristate) {
        BroadcastTag childType;
        XmlValue value = null;
        while (node != null && node.hasChild()) {
            childType = (BroadcastTag) node.getChildType();
            if (childType != null) {
                node = childType.getAppliedValue(node.getChilds(), source, id, intent, processInfo, tristate);
                if (node != null) {
                    value = node.getValue();
                }
            } else {
                node = null;
            }
        }
        if (!(node == null || value == null)) {
            childType = (BroadcastTag) node.getCurrentType();
            if (childType != null && childType.equals(BroadcastTag.POLICY)) {
                return value.getIntValue();
            }
        }
        return BrCtrlType.NONE.ordinal();
    }
}
