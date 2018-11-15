package com.android.server.rms.iaware.dev;

import android.rms.iaware.AwareConfig.Item;
import android.rms.iaware.AwareConfig.SubItem;
import android.rms.iaware.AwareLog;
import android.util.ArrayMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SceneInfo {
    public static final int INVALID_VALUE = -1;
    private static final String ITEM_RULE = "rule";
    public static final String ITEM_RULE_ALLOW = "allow";
    private static final String ITEM_RULE_DEFAULT = "Default";
    private static final String ITEM_RULE_IS_HOME = "IsHome";
    public static final String ITEM_RULE_NOT_ALLOW = "not_allow";
    private static final String ITEM_RULE_PRE_RECOG = "Pre_Recog";
    public static final String ITEM_RULE_SERVICE = "Service_Rule";
    private static final String TAG = "SceneInfo";
    private static final Map<String, Class<?>> mRuleObjMap = new ArrayMap();
    private RuleBase mDefaultRule = null;
    private final Map<String, String> mItemValues = new ArrayMap();
    private final List<RuleBase> mRulesList = new ArrayList();
    private int mSceneId;
    private String mSceneName;

    static {
        mRuleObjMap.put(ITEM_RULE_ALLOW, RuleAllow.class);
        mRuleObjMap.put(ITEM_RULE_SERVICE, RuleService.class);
        mRuleObjMap.put(ITEM_RULE_NOT_ALLOW, RuleNotAllow.class);
        mRuleObjMap.put(ITEM_RULE_PRE_RECOG, RulePreRecog.class);
        mRuleObjMap.put(ITEM_RULE_IS_HOME, RuleIsHome.class);
        mRuleObjMap.put(ITEM_RULE_DEFAULT, RuleDefault.class);
    }

    public SceneInfo(String sceneName, int sceneId) {
        this.mSceneName = sceneName;
        this.mSceneId = sceneId;
    }

    public SceneInfo(Item item) {
        init(item);
    }

    private void init(Item item) {
        if (item != null) {
            Map<String, String> configPropertries = item.getProperties();
            if (configPropertries != null) {
                this.mItemValues.putAll(configPropertries);
            }
            List<SubItem> ruleItemList = item.getSubItemList();
            if (ruleItemList != null) {
                parseRuleList(ruleItemList);
            }
            this.mSceneName = (String) this.mItemValues.get(DevXmlConfig.ITEM_SCENE_NAME);
            this.mSceneId = getSceneId();
        }
    }

    public boolean fillSceneInfo(List<SubItem> subItemList, List<String> ruleList) {
        if (subItemList == null) {
            return false;
        }
        for (SubItem subItem : subItemList) {
            if (subItem == null) {
                AwareLog.e(TAG, "WifiSchedFeature subItem null");
                return false;
            }
            Map<String, String> properties = subItem.getProperties();
            if (properties == null || properties.isEmpty()) {
                AwareLog.e(TAG, "WifiSchedFeature properties null");
                return false;
            }
            String rule = (String) properties.get(ITEM_RULE);
            if (rule == null || !ruleList.contains(rule)) {
                AwareLog.e(TAG, "WifiSchedFeature ruleList not contains");
                return false;
            }
            RuleBase ruleObj = createRuleBaseObj(rule);
            if (ruleObj == null) {
                AwareLog.e(TAG, "WifiSchedFeature createRuleBaseObj error");
                return false;
            } else if (ruleObj.fillRuleInfo(subItem)) {
                insertRuleList(ruleObj);
            } else {
                AwareLog.e(TAG, "WifiSchedFeature fillRuleInfo error");
                return false;
            }
        }
        return true;
    }

    private void insertRuleList(RuleBase ruleObj) {
        int prio = ruleObj.getPriority();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("prio is ");
        stringBuilder.append(prio);
        AwareLog.d(str, stringBuilder.toString());
        int size = this.mRulesList.size();
        int i = 0;
        while (i < size && ((RuleBase) this.mRulesList.get(i)).mPriority <= prio) {
            i++;
        }
        this.mRulesList.add(i, ruleObj);
    }

    public int isMatch(Object... obj) {
        if (obj == null) {
            return -1;
        }
        int size = this.mRulesList.size();
        for (int i = 0; i < size; i++) {
            if (((RuleBase) this.mRulesList.get(i)).isMatch(obj)) {
                return i;
            }
        }
        return -1;
    }

    public long getMode(int index) {
        if (index < 0 || index >= this.mRulesList.size()) {
            return -1;
        }
        RuleBase rule = (RuleBase) this.mRulesList.get(index);
        if (rule == null) {
            return -1;
        }
        return rule.getMode();
    }

    private RuleBase createRuleBaseObj(String rule) {
        if (rule == null) {
            return null;
        }
        Class<?> classObj = (Class) mRuleObjMap.get(rule);
        if (classObj == null) {
            return null;
        }
        RuleBase sceneObj = null;
        try {
            return (RuleBase) classObj.newInstance();
        } catch (InstantiationException e) {
            AwareLog.e(TAG, " InstantiationException, createRuleBaseObj error!");
            return null;
        } catch (IllegalAccessException e2) {
            AwareLog.e(TAG, " IllegalAccessException, createRuleBaseObj error!");
            return null;
        }
    }

    public boolean parseRuleList(List<SubItem> ruleItemList) {
        if (ruleItemList == null) {
            return false;
        }
        for (SubItem subItem : ruleItemList) {
            if (subItem != null) {
                Map<String, String> properties = subItem.getProperties();
                if (properties != null) {
                    String rule = (String) properties.get(ITEM_RULE);
                    if (rule != null) {
                        RuleBase ruleObj = createRuleBaseObj(rule);
                        if (ruleObj != null) {
                            if (ruleObj.fillRuleInfo(subItem)) {
                                if (rule.equals(ITEM_RULE_DEFAULT)) {
                                    this.mDefaultRule = ruleObj;
                                } else {
                                    this.mRulesList.add(ruleObj);
                                }
                            }
                        }
                    }
                }
            }
        }
        if (this.mDefaultRule != null) {
            this.mRulesList.add(this.mDefaultRule);
            this.mDefaultRule = null;
        }
        return true;
    }

    public RuleBase getRuleBase(int index) {
        if (index < 0 || index >= this.mRulesList.size()) {
            return null;
        }
        return (RuleBase) this.mRulesList.get(index);
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("scene name : ");
        s.append(this.mSceneName);
        s.append(", scene id : ");
        s.append(this.mSceneId);
        s.append(", rule num : ");
        s.append(this.mRulesList.size());
        s.append(",mItemValues [ ");
        s.append(this.mItemValues.toString());
        s.append(" ]");
        s.append(",mRulesList [ ");
        s.append(this.mRulesList.toString());
        s.append(" ]");
        return s.toString();
    }

    public int getSceneId() {
        String sceneIdOrg = (String) this.mItemValues.get(DevXmlConfig.ITEM_SCENE_ID);
        try {
            return Integer.parseInt(sceneIdOrg);
        } catch (NumberFormatException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("NumberFormatException, scene id :");
            stringBuilder.append(sceneIdOrg);
            AwareLog.e(str, stringBuilder.toString());
            return -1;
        }
    }

    public String getRuleItemValue(String ruleItem, int index) {
        if (ruleItem == null) {
            AwareLog.e(TAG, "ruleItem is null, error!");
            return null;
        } else if (index < 0 || index >= this.mRulesList.size()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("index is illegal, index:");
            stringBuilder.append(index);
            stringBuilder.append(", list size:");
            stringBuilder.append(this.mRulesList.size());
            AwareLog.e(str, stringBuilder.toString());
            return null;
        } else {
            RuleBase ruleObject = (RuleBase) this.mRulesList.get(index);
            if (ruleObject != null) {
                return ruleObject.getItemValue(ruleItem);
            }
            AwareLog.e(TAG, "ruleObject is null, error!");
            return null;
        }
    }

    public List<String> getRuleItemValues(String ruleItem) {
        if (ruleItem == null) {
            AwareLog.e(TAG, "ruleItem is null, error!");
            return null;
        }
        List<String> res = new ArrayList();
        for (RuleBase ruleObject : this.mRulesList) {
            if (ruleObject != null) {
                String itemValue = ruleObject.getItemValue(ruleItem);
                if (itemValue != null) {
                    res.add(itemValue);
                }
            }
        }
        return res;
    }
}
