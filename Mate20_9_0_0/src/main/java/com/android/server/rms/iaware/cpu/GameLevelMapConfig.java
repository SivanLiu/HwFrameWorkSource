package com.android.server.rms.iaware.cpu;

import android.rms.iaware.AwareConfig.Item;
import android.rms.iaware.AwareConfig.SubItem;
import android.rms.iaware.AwareLog;
import android.util.ArrayMap;
import com.android.server.rms.iaware.cpu.IAwareMode.LevelCmdId;
import java.util.List;
import java.util.Map;

/* compiled from: CPUXmlConfiguration */
class GameLevelMapConfig extends CPUCustBaseConfig {
    private static final String GAME_ENTER_CONFIG_NAME = "game_enter_level_map";
    private static final String GAME_SCENE_CONFIG_NAME = "game_scene_level_map";
    private static final String LEVEL = "level";
    private static final String TAG = "GameLevelMapConfig";
    private static final String TYPE = "type";

    GameLevelMapConfig() {
    }

    public void setConfig(CPUFeature feature) {
        loadConfig(GAME_SCENE_CONFIG_NAME);
        loadConfig(GAME_ENTER_CONFIG_NAME);
    }

    private void loadConfig(String configName) {
        GameLevelMapConfig gameLevelMapConfig = this;
        String str = configName;
        List<Item> awareConfigItemList = getItemList(configName);
        if (awareConfigItemList == null) {
            AwareLog.w(TAG, "loadConfig config prop is null!");
            return;
        }
        int size = awareConfigItemList.size();
        int i = 0;
        while (i < size) {
            List<Item> awareConfigItemList2;
            Item item = (Item) awareConfigItemList.get(i);
            if (item == null) {
                AwareLog.w(TAG, "can not find game level item");
            } else {
                Map<String, String> itemProps = item.getProperties();
                if (itemProps == null) {
                    AwareLog.w(TAG, "can not find game level property");
                } else {
                    String levelType = (String) itemProps.get("type");
                    List<SubItem> subItemList = gameLevelMapConfig.getSubItem(item);
                    if (subItemList == null) {
                        String str2 = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("get subItem failed type:");
                        stringBuilder.append(levelType);
                        AwareLog.w(str2, stringBuilder.toString());
                    } else {
                        Map<Integer, LevelCmdId> levelMap = new ArrayMap();
                        for (SubItem subItem : subItemList) {
                            Map<String, String> subItemProps = subItem.getProperties();
                            if (subItemProps != null) {
                                int level = gameLevelMapConfig.parseInt((String) subItemProps.get("level"));
                                if (level != -1) {
                                    LevelCmdId cmdId = gameLevelMapConfig.parseLevel(subItem.getValue());
                                    if (cmdId == null) {
                                        String str3 = TAG;
                                        StringBuilder stringBuilder2 = new StringBuilder();
                                        awareConfigItemList2 = awareConfigItemList;
                                        stringBuilder2.append("get level failed:");
                                        stringBuilder2.append(subItem.getValue());
                                        AwareLog.w(str3, stringBuilder2.toString());
                                        awareConfigItemList = awareConfigItemList2;
                                    } else {
                                        awareConfigItemList2 = awareConfigItemList;
                                        levelMap.put(Integer.valueOf(level), cmdId);
                                    }
                                    gameLevelMapConfig = this;
                                }
                            }
                        }
                        awareConfigItemList2 = awareConfigItemList;
                        if (GAME_SCENE_CONFIG_NAME.equals(str)) {
                            IAwareMode.getInstance().initLevelMap(0, levelType, levelMap);
                        } else if (GAME_ENTER_CONFIG_NAME.equals(str)) {
                            IAwareMode.getInstance().initLevelMap(1, levelType, levelMap);
                        }
                        i++;
                        awareConfigItemList = awareConfigItemList2;
                        gameLevelMapConfig = this;
                    }
                }
            }
            awareConfigItemList2 = awareConfigItemList;
            i++;
            awareConfigItemList = awareConfigItemList2;
            gameLevelMapConfig = this;
        }
    }

    private LevelCmdId parseLevel(String levelStr) {
        if (levelStr == null) {
            return null;
        }
        String[] strParts = levelStr.split(",");
        if (strParts.length != 2) {
            return null;
        }
        LevelCmdId cmdid = new LevelCmdId();
        int longtermCmdId = parseInt(strParts[0]);
        int i = -1;
        cmdid.mLongTermCmdId = longtermCmdId < -1 ? -1 : longtermCmdId;
        int boostCmdId = parseInt(strParts[1]);
        if (boostCmdId >= -1) {
            i = boostCmdId;
        }
        cmdid.mBoostCmdId = i;
        return cmdid;
    }

    private int parseInt(String intStr) {
        try {
            return Integer.parseInt(intStr);
        } catch (NumberFormatException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("parse level failed:");
            stringBuilder.append(intStr);
            AwareLog.e(str, stringBuilder.toString());
            return -1;
        }
    }
}
