package com.huawei.iconnect.config;

import android.content.Context;
import com.huawei.iconnect.config.btconfig.BtBodyConfigItem;
import com.huawei.iconnect.config.guideconfig.GuideBodyConfigItem;
import com.huawei.iconnect.hwutil.HwLog;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.util.List;

public class ConfigFileStruct {
    private static final String TAG = ConfigFileStruct.class.getSimpleName();
    private static ConfigFileStruct mCfgStruct;
    private List<BtBodyConfigItem> btBodyItems;
    private List<BtBodyConfigItem> fastInquiryItems;
    private List<GuideBodyConfigItem> guideBodyItems;
    private ConfigHeader header;

    public static synchronized ConfigFileStruct getCfgFileStruct(Context context, boolean forceUpdate) {
        ConfigFileStruct configFileStruct;
        synchronized (ConfigFileStruct.class) {
            if (mCfgStruct == null) {
                mCfgStruct = createCfgFileStruct(context);
            } else if (forceUpdate) {
                mCfgStruct = createCfgFileStruct(context);
            }
            configFileStruct = mCfgStruct;
        }
        return configFileStruct;
    }

    private static ConfigFileStruct createCfgFileStruct(Context context) {
        ConfigFileStruct configFileStruct;
        ConfigFileStruct configFileStruct2;
        ConfigFileStruct assetCfgStruct = ConfigFileParser.parseConfigFile(context, ConfigFileParser.DEFAULT_CONFIG_FILE_OF_DEVICE_GUIDE, true);
        HwLog.d(TAG, "assetCfgStruct" + assetCfgStruct);
        String[] cfgFileInfo = HwCfgFilePolicy.getDownloadCfgFile(ConfigFileParser.CONFIG_FILE_REL_PATH, ConfigFileParser.CONFIG_FILE_REL_NAME);
        if (cfgFileInfo == null || cfgFileInfo.length <= 0) {
            HwLog.e(TAG, "Invalid cfgFileInfo");
            configFileStruct = null;
        } else {
            String path = cfgFileInfo[0];
            if (checkCotaFileAvailability(path)) {
                configFileStruct = ConfigFileParser.parseConfigFile(context, path, false);
            } else {
                configFileStruct = null;
            }
        }
        if (assetCfgStruct == null && configFileStruct != null) {
            configFileStruct2 = configFileStruct;
        } else if (assetCfgStruct != null && configFileStruct == null) {
            configFileStruct2 = assetCfgStruct;
        } else if (assetCfgStruct != null) {
            ConfigHeader assetHeader = assetCfgStruct.getHeader();
            ConfigHeader cotaHeader = configFileStruct.getHeader();
            if (assetHeader == null || cotaHeader == null) {
                return null;
            }
            String assetFileVersion = assetHeader.getFileVersion();
            String assetSpecVersion = assetHeader.getSpecVersion();
            String cotaFileVersion = cotaHeader.getFileVersion();
            String cotaSpecVersion = cotaHeader.getSpecVersion();
            if (ConfigFileParser.versionCompare(cotaSpecVersion, assetSpecVersion) > 0) {
                configFileStruct2 = assetCfgStruct;
            } else if (ConfigFileParser.versionCompare(assetSpecVersion, ConfigFileParser.CURRENT_SUPPORT_CONFIG_VERSION) == 0 && ConfigFileParser.versionCompare(cotaSpecVersion, assetSpecVersion) < 0) {
                configFileStruct2 = assetCfgStruct;
            } else if (ConfigFileParser.versionCompare(cotaSpecVersion, assetSpecVersion) == 0) {
                switch (ConfigFileParser.versionCompare(assetFileVersion, cotaFileVersion)) {
                    case ConfigFileParser.CMP_LESS /*-1*/:
                        configFileStruct2 = configFileStruct;
                        break;
                    case 0:
                    case 1:
                        configFileStruct2 = assetCfgStruct;
                        break;
                    default:
                        configFileStruct2 = null;
                        break;
                }
            } else {
                HwLog.e(TAG, "Unable to handle this case on this version");
                configFileStruct2 = null;
            }
        } else {
            HwLog.e(TAG, "Both config files are invalid, exit");
            configFileStruct2 = null;
        }
        return configFileStruct2;
    }

    private static boolean checkCotaFileAvailability(String path) {
        return new File(path).isFile();
    }

    public List<GuideBodyConfigItem> getGuideBodyItems() {
        return this.guideBodyItems;
    }

    public void setGuideBodyItems(List<GuideBodyConfigItem> guideBodyItems) {
        this.guideBodyItems = guideBodyItems;
    }

    public List<BtBodyConfigItem> getFastInquiryItems() {
        return this.fastInquiryItems;
    }

    public void setFastInquiryItems(List<BtBodyConfigItem> fastInquiryItems) {
        this.fastInquiryItems = fastInquiryItems;
    }

    public List<BtBodyConfigItem> getBtBodyItems() {
        return this.btBodyItems;
    }

    public void setBtBodyItems(List<BtBodyConfigItem> btBodyItems) {
        this.btBodyItems = btBodyItems;
    }

    public ConfigHeader getHeader() {
        return this.header;
    }

    public void setHeader(ConfigHeader header) {
        this.header = header;
    }
}
