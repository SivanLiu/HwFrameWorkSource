package com.android.server.rms.iaware.dev;

import android.content.Context;
import android.rms.iaware.AppTypeRecoManager;
import android.rms.iaware.AwareConfig.Item;
import android.rms.iaware.AwareConfig.SubItem;
import android.rms.iaware.AwareLog;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ScreenOnWakelockSchedFeatureRT extends DevSchedFeatureBase {
    private static final String CONFIG_IGNORE_WAKELOCK = "ignoreWakelock";
    private static final String CONFIG_SCREENON_WAKELOCK = "screenOnWakelock";
    private static final String ITEM_APP = "app";
    private static final String ITEM_TAG = "tag";
    private static final String TAG = "ScreenOnWakelockSchedFeatureRT";
    private static final String TYPE_KEYWORD = "type";
    private Context mContext;
    private List<WakelockIgnoreInfo> mIgnoreWakelockList = new ArrayList();

    private static class WakelockIgnoreInfo {
        public String mPkgName;
        public String mTag;

        public WakelockIgnoreInfo(String pkgName, String tag) {
            this.mPkgName = pkgName;
            this.mTag = tag;
        }
    }

    public ScreenOnWakelockSchedFeatureRT(Context context, String name) {
        super(context);
        this.mContext = context;
        loadScreenOnWakelockConfig();
    }

    public boolean handleUpdateCustConfig() {
        AwareLog.d(TAG, "handleUpdateCustConfig!");
        this.mIgnoreWakelockList.clear();
        loadScreenOnWakelockConfig();
        return true;
    }

    public boolean handlerNaviStatus(boolean isInNavi) {
        return true;
    }

    private void parseSubItemList(List<SubItem> subItemList) {
        if (subItemList != null) {
            for (SubItem subItem : subItemList) {
                if (subItem != null) {
                    Map<String, String> properties = subItem.getProperties();
                    if (properties != null) {
                        String app = (String) properties.get(ITEM_APP);
                        if (app != null) {
                            if (!"".equals(app)) {
                                String tag = (String) properties.get(ITEM_TAG);
                                if (tag != null) {
                                    if (!"".equals(tag)) {
                                        String str = TAG;
                                        StringBuilder stringBuilder = new StringBuilder();
                                        stringBuilder.append("add wakelock ignore app ");
                                        stringBuilder.append(app);
                                        stringBuilder.append(" tag ");
                                        stringBuilder.append(tag);
                                        AwareLog.d(str, stringBuilder.toString());
                                        this.mIgnoreWakelockList.add(new WakelockIgnoreInfo(app, tag));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void loadScreenOnWakelockConfig() {
        List<Item> itemList = DevXmlConfig.getItemList(CONFIG_SCREENON_WAKELOCK);
        if (itemList == null) {
            AwareLog.i(TAG, "parse wakelock config error!");
            return;
        }
        for (Item item : itemList) {
            if (item != null) {
                Map<String, String> configPropertries = item.getProperties();
                if (configPropertries != null) {
                    if (CONFIG_IGNORE_WAKELOCK.equals((String) configPropertries.get("type"))) {
                        List<SubItem> subItemList = item.getSubItemList();
                        if (subItemList != null) {
                            if (subItemList.size() != 0) {
                                parseSubItemList(subItemList);
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isIgnoreWakelock(String pkgName, String tag) {
        for (WakelockIgnoreInfo info : this.mIgnoreWakelockList) {
            if (info != null) {
                if (pkgName.equals(info.mPkgName) && ("*".equals(info.mTag) || tag.equals(info.mTag))) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isAwarePreventWakelockScreenOn(String pkgName, String tag) {
        if (pkgName == null || tag == null) {
            AwareLog.e(TAG, "isAwarePreventWakelockScreenOn: pkgName or tag is null!");
            return false;
        } else if (isIgnoreWakelock(pkgName, tag)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isAwarePreventWakelockScreenOn: pkg ");
            stringBuilder.append(pkgName);
            stringBuilder.append(" tag ");
            stringBuilder.append(tag);
            stringBuilder.append(" match ignore list");
            AwareLog.d(str, stringBuilder.toString());
            return false;
        } else {
            int appAttr = AppTypeRecoManager.getInstance().getAppAttribute(pkgName);
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("isAwarePreventWakelockScreenOn: get pkg ");
            stringBuilder2.append(pkgName);
            stringBuilder2.append(" tag ");
            stringBuilder2.append(tag);
            stringBuilder2.append(" APPAttribute ");
            stringBuilder2.append(appAttr);
            AwareLog.d(str2, stringBuilder2.toString());
            if (appAttr != -1 && (appAttr & 8) == 8) {
                return true;
            }
            return false;
        }
    }
}
