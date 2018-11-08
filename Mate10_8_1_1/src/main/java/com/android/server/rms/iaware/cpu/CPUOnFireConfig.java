package com.android.server.rms.iaware.cpu;

import android.rms.iaware.AwareConfig.Item;
import android.rms.iaware.AwareConfig.SubItem;
import android.rms.iaware.AwareLog;
import java.util.List;

/* compiled from: CPUXmlConfiguration */
class CPUOnFireConfig extends CPUCustBaseConfig {
    private static final String CONFIG_ON_FIRE = "on_fire";
    private static final String CONFIG_ON_FIRE_DURATION = "on_fire_duration";
    private static final String TAG = "CPUOnfireConfig";
    private int onFireDuration = 10000;

    public CPUOnFireConfig() {
        init();
    }

    public int getOnFireDuration() {
        return this.onFireDuration;
    }

    private void init() {
        obtainOnFireParams();
    }

    private void obtainOnFireParams() {
        List<Item> awareConfigItemList = getItemList(CONFIG_ON_FIRE);
        if (awareConfigItemList != null) {
            for (Item item : awareConfigItemList) {
                List<SubItem> subItemList = getSubItem(item);
                if (subItemList != null) {
                    for (SubItem subItem : subItemList) {
                        String itemName = subItem.getName();
                        String tempItemValue = subItem.getValue();
                        if (!(itemName == null || tempItemValue == null || (itemName.equals(CONFIG_ON_FIRE_DURATION) ^ 1) != 0)) {
                            try {
                                this.onFireDuration = Integer.parseInt(tempItemValue);
                            } catch (NumberFormatException e) {
                                AwareLog.e(TAG, "itemValue string to int error!");
                            }
                        }
                    }
                }
            }
        }
    }
}
