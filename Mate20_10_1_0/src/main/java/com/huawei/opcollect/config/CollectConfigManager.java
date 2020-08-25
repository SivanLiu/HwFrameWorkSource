package com.huawei.opcollect.config;

import com.huawei.opcollect.utils.OPCollectLog;
import java.util.HashMap;
import java.util.Map;

public class CollectConfigManager {
    private static final String ITEM_MODULE_TAG = "HwUserExperience";
    private static final String ITEM_PARAM_NAME = "name";
    private static final String ITEM_PARAM_VALUE = "value";
    private static final Object LOCK = new Object();
    private static final int PROP_DEFVALUE_IS_MODULE_OPEN = 1;
    private static final String PROP_NAME_IS_HWUSER_OPEN = "IsOpcollectOpen";
    private static final String TAG = "CollectConfigManager";
    private static final String XML_ETC_HIVIEW_SYS_CONFIG = "/system/etc/hiview/hiview_sysconfig.xml";
    private static CollectConfigManager instance;
    private Map<String, String> sysPropMap = new HashMap();

    private CollectConfigManager() {
        loadConfig();
    }

    public static CollectConfigManager getInstance() {
        CollectConfigManager collectConfigManager;
        synchronized (LOCK) {
            if (instance == null) {
                instance = new CollectConfigManager();
            }
            collectConfigManager = instance;
        }
        return collectConfigManager;
    }

    private void loadConfig() {
        XmlItem systemXml = new XmpParserGenerator(XML_ETC_HIVIEW_SYS_CONFIG).getChildItem(ITEM_MODULE_TAG);
        if (systemXml == null || systemXml.getChildItemList() == null) {
            OPCollectLog.e(TAG, "read nothing from the hiview system config xml");
            return;
        }
        for (XmlItem item : systemXml.getChildItemList()) {
            String name = item.getProp("name");
            String value = item.getProp(ITEM_PARAM_VALUE);
            if (!(name == null || value == null)) {
                this.sysPropMap.put(name, value);
            }
        }
    }

    public boolean isModuleCanBeStarted() {
        boolean isCanBeStarted = getPlusSysProp(PROP_NAME_IS_HWUSER_OPEN, 1) == 1;
        OPCollectLog.r(TAG, "Current status is " + isCanBeStarted);
        return isCanBeStarted;
    }

    private int getPlusSysProp(String name, int defValue) {
        try {
            String value = this.sysPropMap.get(name);
            if (value == null || "".equals(value)) {
                return defValue;
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            OPCollectLog.e(TAG, "GetSysProp has NumberFormatException , SysPropName is " + name);
            return defValue;
        }
    }
}
