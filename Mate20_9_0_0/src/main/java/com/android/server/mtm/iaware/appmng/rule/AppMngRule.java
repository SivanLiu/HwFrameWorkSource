package com.android.server.mtm.iaware.appmng.rule;

import android.app.mtm.iaware.HwAppStartupSetting;
import android.app.mtm.iaware.appmng.AppMngConstant.CleanReason;
import android.util.ArrayMap;
import com.android.server.mtm.iaware.appmng.AwareProcessBlockInfo;
import com.android.server.mtm.iaware.appmng.AwareProcessInfo;
import com.android.server.mtm.iaware.appmng.appstart.AwareAppStartupPolicy;
import com.android.server.mtm.iaware.appmng.rule.RuleNode.XmlValue;
import com.android.server.mtm.iaware.appmng.rule.RuleParserUtil.AppMngTag;
import com.android.server.mtm.iaware.appmng.rule.RuleParserUtil.TagEnum;
import com.android.server.mtm.taskstatus.ProcessCleaner.CleanType;

public class AppMngRule extends Config {
    private static final int MAX_DEPTH = 7;
    private static final String TAG = "AppMngRule";
    private static final String TAG_SCOPE = "scope";
    private static final String VALUE_TRI_ONLY = "tri_only";
    private boolean mIsScopeAll;

    public AppMngRule(ArrayMap<String, String> prop, RuleNode rules) {
        super(prop, rules);
        if (this.mProperties != null) {
            if (VALUE_TRI_ONLY.equals((String) this.mProperties.get("scope"))) {
                this.mIsScopeAll = false;
            } else {
                this.mIsScopeAll = true;
            }
        }
    }

    private static int getTriState(HwAppStartupSetting setting) {
        if (1 == setting.getPolicy(0)) {
            if (1 != setting.getPolicy(3)) {
                return 1;
            }
            if (2 == setting.getModifier(0)) {
                return 4;
            }
            return 3;
        } else if (1 == setting.getPolicy(3)) {
            return 2;
        } else {
            return 0;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:25:0x00a9  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x0088  */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x00bf  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public AwareProcessBlockInfo apply(AwareProcessInfo processInfo, String packageName, int level, boolean shouldConsiderList) {
        int tristate;
        XmlValue value;
        RuleNode node;
        AppMngTag childType;
        RuleNode node2;
        AwareProcessInfo awareProcessInfo = processInfo;
        RuleNode ruleNode = this.mRules;
        StringBuilder reason = new StringBuilder();
        ArrayMap<String, Integer> detailedReason = new ArrayMap(7);
        String str;
        if (shouldConsiderList) {
            HwAppStartupSetting setting = null;
            AwareAppStartupPolicy policy = AwareAppStartupPolicy.self();
            if (policy != null) {
                str = packageName;
                setting = policy.getAppStartupSetting(str);
            } else {
                str = packageName;
            }
            HwAppStartupSetting setting2 = setting;
            if (setting2 != null) {
                tristate = getTriState(setting2);
                value = null;
                node = ruleNode;
                while (node != null && node.hasChild()) {
                    childType = (AppMngTag) node.getChildType();
                    if (childType == null) {
                        node2 = childType.getAppliedValue(node.getChilds(), awareProcessInfo, level, str, tristate);
                        if (node2 != null) {
                            XmlValue value2 = node2.getValue();
                            collectReason(reason, detailedReason, node2.getCurrentType(), value2);
                            node = node2;
                            value = value2;
                        }
                    } else {
                        node2 = null;
                    }
                    node = node2;
                }
                if (!(node == null || value == null)) {
                    childType = (AppMngTag) node.getCurrentType();
                    if (AppMngTag.POLICY.equals(childType)) {
                        return new AwareProcessBlockInfo(reason.toString(), awareProcessInfo.mProcInfo.mUid, awareProcessInfo, value.getIntValue(), detailedReason, value.getWeight());
                    }
                }
                detailedReason.put(AppMngTag.POLICY.getDesc(), Integer.valueOf(CleanType.NONE.ordinal()));
                detailedReason.put("spec", Integer.valueOf(CleanReason.CONFIG_INVALID.ordinal()));
                return new AwareProcessBlockInfo(CleanReason.CONFIG_INVALID.getCode(), awareProcessInfo.mProcInfo.mUid, awareProcessInfo, CleanType.NONE.ordinal(), detailedReason);
            } else if (!this.mIsScopeAll) {
                detailedReason.put(AppMngTag.POLICY.getDesc(), Integer.valueOf(CleanType.NONE.ordinal()));
                detailedReason.put("spec", Integer.valueOf(CleanReason.OUT_OF_SCOPE.ordinal()));
                return new AwareProcessBlockInfo(CleanReason.OUT_OF_SCOPE.getCode(), awareProcessInfo.mProcInfo.mUid, awareProcessInfo, CleanType.NONE.ordinal(), detailedReason);
            }
        }
        str = packageName;
        tristate = -1;
        value = null;
        node = ruleNode;
        while (node != null) {
            childType = (AppMngTag) node.getChildType();
            if (childType == null) {
            }
            node = node2;
        }
        childType = (AppMngTag) node.getCurrentType();
        if (AppMngTag.POLICY.equals(childType)) {
        }
        detailedReason.put(AppMngTag.POLICY.getDesc(), Integer.valueOf(CleanType.NONE.ordinal()));
        detailedReason.put("spec", Integer.valueOf(CleanReason.CONFIG_INVALID.ordinal()));
        return new AwareProcessBlockInfo(CleanReason.CONFIG_INVALID.getCode(), awareProcessInfo.mProcInfo.mUid, awareProcessInfo, CleanType.NONE.ordinal(), detailedReason);
    }

    private void collectReason(StringBuilder reason, ArrayMap<String, Integer> detailedReason, TagEnum type, XmlValue value) {
        if (type != null && value != null) {
            reason.append(type);
            reason.append(":");
            reason.append(value);
            reason.append(" ");
            detailedReason.put(type.getDesc(), Integer.valueOf(value.getIntValue()));
        }
    }

    public static int getTriState(String pkgName) {
        AwareAppStartupPolicy policy = AwareAppStartupPolicy.self();
        HwAppStartupSetting setting = null;
        if (policy != null) {
            setting = policy.getAppStartupSetting(pkgName);
        }
        if (setting != null) {
            return getTriState(setting);
        }
        return -1;
    }
}
