package com.android.server.rms.iaware.cpu;

import android.os.SystemProperties;
import android.rms.iaware.AwareConfig;
import android.rms.iaware.AwareLog;
import java.util.List;
import java.util.Map;

/* compiled from: CPUXmlConfiguration */
class SubSwitchConfig extends CPUCustBaseConfig {
    private static final int BIT_POS_MAX = 31;
    private static final int BIT_POS_MIN = 0;
    private static final String CONFIG_SUBSWITCH = "sub_switch";
    private static final int DEFAULT_SUBSWITCH = 0;
    private static final String ITEM_PROP_TYPE = "type";
    private static final String ITEM_TYPE_SWITCH = "switch";
    private static final String PROP_SUBSWITCH = "persist.sys.cpuset.subswitch";
    private static final String SUBITEM_PROP_BIT = "bit";
    private static final String TAG = "SubSwitchConfig";
    private int mSubSwitch;

    SubSwitchConfig() {
        this.mSubSwitch = 0;
        this.mSubSwitch = getSubSwitch();
    }

    @Override // com.android.server.rms.iaware.cpu.CPUCustBaseConfig
    public void setConfig(CPUFeature feature) {
        try {
            SystemProperties.set(PROP_SUBSWITCH, Integer.toString(this.mSubSwitch));
        } catch (IllegalArgumentException e) {
            AwareLog.e(TAG, "set subswitch failed," + e.toString());
        }
    }

    private int getSubSwitch() {
        String itemType;
        List<AwareConfig.Item> awareConfigItemList = getItemList(CONFIG_SUBSWITCH);
        if (awareConfigItemList == null) {
            AwareLog.d(TAG, "can not get subswitch config");
            return 0;
        }
        for (AwareConfig.Item item : awareConfigItemList) {
            Map<String, String> configPropertries = item.getProperties();
            if (configPropertries != null && (itemType = configPropertries.get("type")) != null && "switch".equals(itemType)) {
                return getSubSwitchFromSubItem(item);
            }
        }
        return 0;
    }

    private int getSubSwitchFromSubItem(AwareConfig.Item item) {
        Map<String, String> subItemProps;
        String bitPosStr;
        List<AwareConfig.SubItem> subItemList = getSubItem(item);
        if (subItemList == null) {
            AwareLog.d(TAG, "can not get subswitch config subitem");
            return 0;
        }
        int subSwitch = 0;
        for (AwareConfig.SubItem subItem : subItemList) {
            String itemValue = subItem.getValue();
            if (!(itemValue == null || (subItemProps = subItem.getProperties()) == null || (bitPosStr = subItemProps.get(SUBITEM_PROP_BIT)) == null)) {
                try {
                    int bitPos = Integer.parseInt(bitPosStr);
                    if (isInBitPosRange(bitPos)) {
                        subSwitch = setBit(subSwitch, itemValue, bitPos);
                    }
                } catch (NumberFormatException e) {
                    AwareLog.d(TAG, "parse bitpos failed:" + bitPosStr);
                }
            }
        }
        return subSwitch;
    }

    private int setBit(int subSwitch, String bitValue, int bitPos) {
        if (bitValue.equals("1")) {
            return (1 << bitPos) | subSwitch;
        }
        if (bitValue.equals("0")) {
            return (~(1 << bitPos)) & subSwitch;
        }
        return subSwitch;
    }

    private boolean isInBitPosRange(int bitPos) {
        return bitPos >= 0 && bitPos <= 31;
    }
}
