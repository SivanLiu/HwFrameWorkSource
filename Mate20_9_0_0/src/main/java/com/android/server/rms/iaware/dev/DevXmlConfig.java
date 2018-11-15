package com.android.server.rms.iaware.dev;

import android.os.IBinder;
import android.os.RemoteException;
import android.rms.iaware.AwareConfig;
import android.rms.iaware.AwareConfig.Item;
import android.rms.iaware.AwareConfig.SubItem;
import android.rms.iaware.AwareLog;
import android.rms.iaware.IAwareCMSManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DevXmlConfig {
    private static final String ATTR_IGNORE_APP = "ignore_app";
    private static final String ATTR_TYPE = "switch";
    private static final String CONFIG_SUBSWITCH = "sub_switch";
    private static final String DEV_FEATURE_NAME = "DevSchedFeature";
    public static final int INVALID_DEVICE_ID = -1;
    private static final String ITEM_DEVICE_ID = "dev_id";
    private static final String ITEM_EXCEPT_NAME = "name";
    public static final String ITEM_SCENE_ID = "scene_id";
    public static final String ITEM_SCENE_NAME = "scenename";
    private static final String ITEM_TYPE = "type";
    private static final String TAG = "DevXmlConfig";
    private static IBinder mCmsManager;

    public boolean readDevStrategy(Map<Integer, SceneInfo> sceneMap, String configName, Map<String, ArrayList<String>> sceneRullMap) {
        Map<String, ArrayList<String>> map = sceneRullMap;
        if (sceneMap == null || configName == null || map == null) {
            AwareLog.e(TAG, "NetLocationSchedFeature para null");
            return false;
        }
        List<Item> awareConfigItemList = getItemList(configName);
        if (awareConfigItemList == null) {
            AwareLog.e(TAG, "NetLocationSchedFeature read strategy config error!");
            return false;
        }
        for (Item item : awareConfigItemList) {
            if (item == null) {
                AwareLog.e(TAG, "NetLocationSchedFeature item null");
                return false;
            }
            Map<String, String> configPropertries = item.getProperties();
            if (configPropertries == null || configPropertries.isEmpty()) {
                AwareLog.e(TAG, "NetLocationSchedFeature configPropertries null");
                return false;
            }
            String sceneName = (String) configPropertries.get(ITEM_SCENE_NAME);
            if (sceneName == null || sceneName.isEmpty()) {
                AwareLog.e(TAG, "NetLocationSchedFeature sceneName null");
                return false;
            }
            ArrayList<String> ruleList = (ArrayList) map.get(sceneName);
            if (ruleList == null || ruleList.isEmpty()) {
                AwareLog.e(TAG, "NetLocationSchedFeature ruleList null");
                return false;
            }
            try {
                if (!createMapLocal(item, sceneName, Integer.parseInt((String) configPropertries.get(ITEM_SCENE_ID)), sceneMap, ruleList)) {
                    return false;
                }
            } catch (NumberFormatException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(" NumberFormatException, sceneid is ");
                stringBuilder.append((String) configPropertries.get(ITEM_SCENE_ID));
                AwareLog.e(str, stringBuilder.toString());
                return false;
            }
        }
        return true;
    }

    private boolean createMapLocal(Item item, String sceneName, int sceneId, Map<Integer, SceneInfo> sceneMap, List<String> ruleList) {
        SceneInfo sceneObj = new SceneInfo(sceneName, sceneId);
        List<SubItem> subItemList = item.getSubItemList();
        if (subItemList == null || subItemList.size() == 0) {
            AwareLog.e(TAG, "NetLocationSchedFeature subItemList is null");
            return false;
        } else if (sceneObj.fillSceneInfo(subItemList, ruleList)) {
            sceneMap.put(Integer.valueOf(sceneId), sceneObj);
            return true;
        } else {
            AwareLog.e(TAG, "NetLocationSchedFeature fillSceneInfo error");
            return false;
        }
    }

    public static void loadSubFeatureSwitch(Map<String, String> subFeatureSwitch) {
        if (subFeatureSwitch != null) {
            List<Item> itemList = getItemList(CONFIG_SUBSWITCH);
            if (itemList != null) {
                for (Item item : itemList) {
                    if (item != null) {
                        Map<String, String> configPropertries = item.getProperties();
                        if (configPropertries != null) {
                            String itemType = (String) configPropertries.get("type");
                            if (itemType != null) {
                                if (itemType.equals("switch")) {
                                    parseSubSwitchFromSubItem(item, subFeatureSwitch);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static List<Item> getItemList(String configName) {
        if (configName == null) {
            AwareLog.e(TAG, "configName is null");
            return null;
        }
        if (mCmsManager == null) {
            mCmsManager = IAwareCMSManager.getICMSManager();
        }
        AwareConfig awareConfig = null;
        try {
            if (mCmsManager != null) {
                awareConfig = IAwareCMSManager.getCustConfig(mCmsManager, DEV_FEATURE_NAME, configName);
                AwareLog.d(TAG, "DevXmlBaseConfiguration");
            }
            if (awareConfig == null) {
                return null;
            }
            return awareConfig.getConfigList();
        } catch (RemoteException e) {
            AwareLog.e(TAG, "awareConfig is null");
            return null;
        }
    }

    private static void parseSubSwitchFromSubItem(Item item, Map<String, String> subFeatureSwitch) {
        if (item != null && subFeatureSwitch != null) {
            List<SubItem> subItemList = item.getSubItemList();
            if (subItemList == null) {
                AwareLog.e(TAG, "can not get subswitch config subitem");
                return;
            }
            for (SubItem subItem : subItemList) {
                String itemName = subItem.getName();
                if (itemName != null && !itemName.isEmpty()) {
                    String itemValue = subItem.getValue();
                    if (itemValue != null) {
                        subFeatureSwitch.put(itemName.trim(), itemValue.trim());
                    }
                } else {
                    return;
                }
            }
        }
    }

    public void readSceneInfos(String deviceName, List<SceneInfo> sceneList) {
        if (sceneList != null) {
            sceneList.clear();
            List<Item> awareConfigItemList = getItemList(deviceName);
            if (awareConfigItemList == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("get ");
                stringBuilder.append(deviceName);
                stringBuilder.append(" sceneinfo error!");
                AwareLog.e(str, stringBuilder.toString());
                return;
            }
            for (Item item : awareConfigItemList) {
                if (item != null) {
                    Map<String, String> configPropertries = item.getProperties();
                    if (configPropertries != null) {
                        if (configPropertries.get(ITEM_SCENE_NAME) != null) {
                            sceneList.add(new SceneInfo(item));
                        }
                    }
                }
            }
        }
    }

    public void readExceptApps(String deviceName, List<String> exceptList) {
        if (exceptList != null) {
            exceptList.clear();
            List<Item> awareConfigItemList = getItemList(deviceName);
            if (awareConfigItemList == null) {
                AwareLog.e(TAG, "get except apps config error!");
                return;
            }
            for (Item item : awareConfigItemList) {
                if (item != null) {
                    Map<String, String> configPropertries = item.getProperties();
                    if (configPropertries != null) {
                        String name = (String) configPropertries.get("name");
                        if (name != null) {
                            if (ATTR_IGNORE_APP.equals(name)) {
                                List<SubItem> subItemList = item.getSubItemList();
                                if (subItemList != null) {
                                    for (SubItem subItem : subItemList) {
                                        String pkgName = subItem.getValue();
                                        if (!(pkgName == null || pkgName.isEmpty())) {
                                            exceptList.add(pkgName.trim());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public int readDeviceId(String deviceName) {
        List<Item> awareConfigItemList = getItemList(deviceName);
        if (awareConfigItemList == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("get ");
            stringBuilder.append(deviceName);
            stringBuilder.append(" device id error!");
            AwareLog.e(str, stringBuilder.toString());
            return -1;
        }
        for (Item item : awareConfigItemList) {
            if (item != null) {
                Map<String, String> configPropertries = item.getProperties();
                if (configPropertries != null) {
                    String devIdOrg = (String) configPropertries.get(ITEM_DEVICE_ID);
                    if (devIdOrg != null) {
                        try {
                            return Integer.parseInt(devIdOrg.trim());
                        } catch (NumberFormatException e) {
                            String str2 = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("NumberFormatException, device id :");
                            stringBuilder2.append(devIdOrg);
                            AwareLog.e(str2, stringBuilder2.toString());
                        }
                    }
                }
            }
        }
        return -1;
    }
}
