package com.android.server.rms.iaware.cpu;

import android.rms.iaware.AwareConfig.Item;
import android.rms.iaware.AwareConfig.SubItem;
import android.rms.iaware.AwareLog;
import android.util.ArrayMap;
import java.util.List;
import java.util.Map;

/* compiled from: CPUXmlConfiguration */
class OnDemandBoostConfig extends CPUCustBaseConfig {
    private static final String ON_DEMAND_BOOST_CONFIG_NAME = "on_demand_boost";
    private static final String TAG = "OnDemandBoostConfig";
    private Map<String, Integer> mOnDemandParaMap = new ArrayMap();

    OnDemandBoostConfig() {
    }

    public void setConfig(CPUFeature feature) {
        loadConfig(ON_DEMAND_BOOST_CONFIG_NAME);
    }

    private void loadConfig(String configName) {
        List<Item> awareConfigItemList = getItemList(configName);
        if (awareConfigItemList == null) {
            AwareLog.w(TAG, "loadConfig config prop is null!");
            return;
        }
        for (Item item : awareConfigItemList) {
            List<SubItem> subItemList = getSubItem(item);
            if (subItemList != null) {
                for (SubItem subItem : subItemList) {
                    String itemName = subItem.getName();
                    String tempItemValue = subItem.getValue();
                    if (itemName != null) {
                        if (tempItemValue != null) {
                            try {
                                this.mOnDemandParaMap.put(itemName, Integer.valueOf(Integer.parseInt(tempItemValue)));
                            } catch (NumberFormatException e) {
                                AwareLog.e(TAG, "itemValue string to int error!");
                            }
                        }
                    }
                }
            }
        }
        OnDemandBoost.getInstance().setParams(this.mOnDemandParaMap);
    }
}
