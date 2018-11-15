package com.android.server.hidata.appqoe;

import android.emcom.EmcomManager;
import android.util.Xml;
import com.android.server.hidata.channelqoe.HwCHQciConfig;
import com.android.server.hidata.channelqoe.HwCHQciManager;
import com.android.server.hidata.mplink.HwMpLinkConfigInfo;
import com.android.server.hidata.mplink.HwMpLinkContentAware;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class HwAPPQoEResourceMangerImpl {
    private static final String CFG_FILE_NAME = "/hidata_config_cust.xml";
    private static final String CFG_VER_DIR = "emcom/noncell";
    private static final int PARA_UPGRADE_FILE_NOTEXIST = 0;
    private static final int PARA_UPGRADE_RESPONSE_FILE_ERROR = 6;
    private static final int PARA_UPGRADE_RESPONSE_UPGRADE_ALREADY = 4;
    private static final int PARA_UPGRADE_RESPONSE_UPGRADE_FAILURE = 9;
    private static final int PARA_UPGRADE_RESPONSE_UPGRADE_PENDING = 7;
    private static final int PARA_UPGRADE_RESPONSE_UPGRADE_SUCCESS = 8;
    private static final int PARA_UPGRADE_RESPONSE_VERSION_MISMATCH = 5;
    private static final String TAG = "HiData_HwAPPQoEResourceMangerImpl";
    private boolean isXmlLoadFinsh = false;
    private List<HwAPPQoEAPKConfig> mAPKConfigList = new ArrayList();
    private List<HwAPPQoEGameConfig> mGameConfigList = new ArrayList();
    private HwCHQciManager mHwCHQciManager;
    private HwMpLinkContentAware mHwMpLinkContentAware;
    private final Object mLock = new Object();

    public class MyRunnable implements Runnable {
        public void run() {
            HwAPPQoEUtils.logD(HwAPPQoEResourceMangerImpl.TAG, "Start read thread");
            HwAPPQoEResourceMangerImpl.this.readAppConfigList();
        }
    }

    public HwAPPQoEResourceMangerImpl() {
        init();
    }

    private void init() {
        this.mHwMpLinkContentAware = HwMpLinkContentAware.onlyGetInstance();
        this.mHwCHQciManager = HwCHQciManager.getInstance();
        new Thread(new MyRunnable()).start();
    }

    public void onConfigFilePathChanged() {
        new Thread(new MyRunnable()).start();
    }

    public void responseForParaUpdate(int result) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("appqoe response,  result: ");
        stringBuilder.append(result);
        HwAPPQoEUtils.logD(str, stringBuilder.toString());
        EmcomManager mEmcomManager = EmcomManager.getInstance();
        if (mEmcomManager != null) {
            mEmcomManager.responseForParaUpgrade(16, 1, result);
        }
    }

    public String getConfigFilePath() {
        HwAPPQoEUtils.logD(TAG, "getConfigFilePath, enter");
        try {
            String[] cfgFileInfo = HwCfgFilePolicy.getDownloadCfgFile(CFG_VER_DIR, "emcom/noncell/hidata_config_cust.xml");
            String str;
            StringBuilder stringBuilder;
            if (cfgFileInfo == null) {
                HwAPPQoEUtils.logE("Both default and cota config files not exist");
                responseForParaUpdate(0);
                return null;
            } else if (cfgFileInfo[0].contains("/cota")) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("cota config file path is: ");
                stringBuilder.append(cfgFileInfo[0]);
                stringBuilder.append(", version:");
                stringBuilder.append(cfgFileInfo[1]);
                HwAPPQoEUtils.logD(str, stringBuilder.toString());
                return cfgFileInfo[0];
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("system config file path is: ");
                stringBuilder.append(cfgFileInfo[0]);
                stringBuilder.append(", version:");
                stringBuilder.append(cfgFileInfo[1]);
                HwAPPQoEUtils.logD(str, stringBuilder.toString());
                return cfgFileInfo[0];
            }
        } catch (Exception e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getConfigFilePath exception:");
            stringBuilder2.append(e);
            HwAPPQoEUtils.logD(str2, stringBuilder2.toString());
            responseForParaUpdate(6);
            return null;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:111:0x02a6  */
    /* JADX WARNING: Missing block: B:33:0x00bf, code:
            r10 = r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void readAppConfigList() {
        StringBuilder stringBuilder;
        Exception e;
        Exception exception;
        String str;
        InputStream xmlPullParser;
        InputStream inputStream = null;
        XmlPullParser xmlPullParser2 = null;
        String configFilePath = getConfigFilePath();
        if (configFilePath == null) {
            HwAPPQoEUtils.logD(TAG, "readAppConfigList, configPath is null");
            return;
        }
        File configPath = new File(configFilePath);
        if (configPath.exists()) {
            HwAPPQoEAPKConfig mAPKConfig = null;
            HwAPPQoEGameConfig mGameConfig = null;
            HwMpLinkConfigInfo mHwMpLinkConfigInfo = null;
            HwCHQciConfig mQCIConfig = null;
            int i = 1;
            StringBuilder stringBuilder2;
            try {
                inputStream = new FileInputStream(configPath);
                xmlPullParser2 = Xml.newPullParser();
                xmlPullParser2.setInput(inputStream, "utf-8");
                ArrayList mUpGratedAPKConfigList = new ArrayList();
                ArrayList mUpGratedGameConfigList = new ArrayList();
                int eventType = xmlPullParser2.getEventType();
                while (true) {
                    int eventType2 = eventType;
                    if (eventType2 != i) {
                        if (eventType2 != 0) {
                            HwCHQciConfig mQCIConfig2;
                            switch (eventType2) {
                                case 2:
                                    String str2;
                                    if (!xmlPullParser2.getName().equals("APKInfo")) {
                                        if (!xmlPullParser2.getName().equals("GameInfo")) {
                                            if (!"mplink_version".equals(xmlPullParser2.getName())) {
                                                if (!"mplink_enable".equals(xmlPullParser2.getName())) {
                                                    if (!"vendor".equals(xmlPullParser2.getName())) {
                                                        if (!"QciInfo".equals(xmlPullParser2.getName())) {
                                                            if (mAPKConfig == null) {
                                                                if (mGameConfig == null) {
                                                                    if (mHwMpLinkConfigInfo == null) {
                                                                        if (mQCIConfig == null) {
                                                                            break;
                                                                        }
                                                                        fillQCIConfig(mQCIConfig, xmlPullParser2.getName(), xmlPullParser2.nextText());
                                                                        break;
                                                                    }
                                                                    fillMpLinkConfig(mHwMpLinkConfigInfo, xmlPullParser2.getName(), xmlPullParser2.nextText());
                                                                    break;
                                                                }
                                                                fillGameConfig(mGameConfig, xmlPullParser2.getName(), xmlPullParser2.nextText());
                                                                break;
                                                            }
                                                            fillAPKConfig(mAPKConfig, xmlPullParser2.getName(), xmlPullParser2.nextText());
                                                            break;
                                                        }
                                                        mQCIConfig2 = new HwCHQciConfig();
                                                    } else {
                                                        mHwMpLinkConfigInfo = new HwMpLinkConfigInfo();
                                                        mHwMpLinkConfigInfo.setmVendorOui(xmlPullParser2.getAttributeValue(0));
                                                        break;
                                                    }
                                                } else if (this.mHwMpLinkContentAware == null) {
                                                    break;
                                                } else {
                                                    this.mHwMpLinkContentAware.setMpLinkEnable(xmlPullParser2.nextText());
                                                    break;
                                                }
                                            } else if (this.mHwMpLinkContentAware == null) {
                                                break;
                                            } else {
                                                this.mHwMpLinkContentAware.setMpLinkVersion(xmlPullParser2.nextText());
                                                break;
                                            }
                                        }
                                        mGameConfig = new HwAPPQoEGameConfig();
                                        mGameConfig.mGameName = xmlPullParser2.getAttributeValue(0);
                                        str2 = TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("readAppConfigList, create GameConfig: ");
                                        stringBuilder.append(mGameConfig.mGameName);
                                        HwAPPQoEUtils.logD(str2, stringBuilder.toString());
                                        break;
                                    }
                                    mAPKConfig = new HwAPPQoEAPKConfig();
                                    mAPKConfig.packageName = xmlPullParser2.getAttributeValue(0);
                                    str2 = TAG;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("readAppConfigList, create APKConfig: ");
                                    stringBuilder2.append(mAPKConfig.packageName);
                                    HwAPPQoEUtils.logD(str2, stringBuilder2.toString());
                                    break;
                                case 3:
                                    if (xmlPullParser2.getName().equals("APKInfo")) {
                                        HwAPPQoEUtils.logD(TAG, "readAppConfigList, save APKInfo ");
                                        mUpGratedAPKConfigList.add(mAPKConfig);
                                        mAPKConfig = null;
                                        break;
                                    } else if (xmlPullParser2.getName().equals("GameInfo")) {
                                        HwAPPQoEUtils.logD(TAG, "readAppConfigList, save GameInfo ");
                                        mUpGratedGameConfigList.add(mGameConfig);
                                        mGameConfig = null;
                                        break;
                                    } else if ("vendor".equals(xmlPullParser2.getName())) {
                                        if (this.mHwMpLinkContentAware != null) {
                                            this.mHwMpLinkContentAware.addMpLinkDeviceAPP(mHwMpLinkConfigInfo);
                                        }
                                        mHwMpLinkConfigInfo = null;
                                        break;
                                    } else if (!"QciInfo".equals(xmlPullParser2.getName())) {
                                        break;
                                    } else {
                                        if (this.mHwCHQciManager != null) {
                                            this.mHwCHQciManager.addConfig(mQCIConfig);
                                        }
                                        mQCIConfig2 = null;
                                    }
                                default:
                                    break;
                            }
                        }
                        eventType = xmlPullParser2.next();
                        i = 1;
                    } else {
                        synchronized (this.mLock) {
                            this.mAPKConfigList.clear();
                            this.mAPKConfigList.addAll(mUpGratedAPKConfigList);
                            mUpGratedAPKConfigList.clear();
                            this.mGameConfigList.clear();
                            this.mGameConfigList.addAll(mUpGratedGameConfigList);
                            mUpGratedGameConfigList.clear();
                        }
                        responseForParaUpdate(8);
                        try {
                            inputStream.close();
                        } catch (Exception e2) {
                            e = e2;
                            exception = e;
                            str = TAG;
                            stringBuilder = new StringBuilder();
                        }
                        xmlPullParser = inputStream;
                        synchronized (this.mLock) {
                            this.isXmlLoadFinsh = true;
                        }
                    }
                }
            } catch (FileNotFoundException e3) {
                String str3 = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("readAppConfigList exception 1:");
                stringBuilder2.append(e3);
                HwAPPQoEUtils.logD(str3, stringBuilder2.toString());
                responseForParaUpdate(0);
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e4) {
                        e = e4;
                        exception = e;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                    }
                }
            } catch (IOException e5) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("readAppConfigList exception 2:");
                stringBuilder.append(e5);
                HwAPPQoEUtils.logD(str, stringBuilder.toString());
                responseForParaUpdate(6);
                e5.printStackTrace();
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e6) {
                        e = e6;
                        exception = e;
                        str = TAG;
                        stringBuilder = new StringBuilder();
                    }
                }
            } catch (XmlPullParserException e7) {
                try {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("readAppConfigList exception 3:");
                    stringBuilder.append(e7);
                    HwAPPQoEUtils.logD(str, stringBuilder.toString());
                    responseForParaUpdate(6);
                    e7.printStackTrace();
                    if (inputStream != null) {
                        try {
                            inputStream.close();
                        } catch (Exception e8) {
                            e = e8;
                            exception = e;
                            str = TAG;
                            stringBuilder = new StringBuilder();
                        }
                    }
                } catch (Throwable th) {
                    XmlPullParser xmlPullParser3 = xmlPullParser2;
                    InputStream inputStream2 = inputStream;
                    Throwable inputStream3 = th;
                    if (inputStream2 != null) {
                        try {
                            inputStream2.close();
                        } catch (Exception e9) {
                            Exception exception2 = e9;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("readAppConfigList exception 4:");
                            stringBuilder.append(e9);
                            HwAPPQoEUtils.logD(TAG, stringBuilder.toString());
                        }
                    }
                }
            }
        } else {
            responseForParaUpdate(0);
            HwAPPQoEUtils.logD(TAG, "readAppConfigList, configPath not exit");
            return;
        }
        stringBuilder.append("readAppConfigList exception 4:");
        stringBuilder.append(e9);
        HwAPPQoEUtils.logD(str, stringBuilder.toString());
        xmlPullParser = inputStream;
        synchronized (this.mLock) {
        }
    }

    public void fillAPKConfig(HwAPPQoEAPKConfig config, String elementName, String elementValue) {
        if (elementName == null || config == null) {
            HwAPPQoEUtils.logD(TAG, "fillAPKConfig, input error");
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("fillAPKConfig:");
        stringBuilder.append(elementName);
        stringBuilder.append(",");
        stringBuilder.append(elementValue);
        HwAPPQoEUtils.logD(str, stringBuilder.toString());
        Object obj = -1;
        switch (elementName.hashCode()) {
            case -2137635847:
                if (elementName.equals("mHistoryQoeBadTH")) {
                    obj = 10;
                    break;
                }
                break;
            case -1814569853:
                if (elementName.equals("mAggressiveStallTH")) {
                    obj = 12;
                    break;
                }
                break;
            case -1110914385:
                if (elementName.equals("mAppId")) {
                    obj = 1;
                    break;
                }
                break;
            case -923620903:
                if (elementName.equals("mGeneralStallTH")) {
                    obj = 11;
                    break;
                }
                break;
            case -836023979:
                if (elementName.equals("mAppPeriod")) {
                    obj = 5;
                    break;
                }
                break;
            case -154939613:
                if (elementName.equals("mScenceId")) {
                    obj = 3;
                    break;
                }
                break;
            case -154462957:
                if (elementName.equals("mScenseId")) {
                    obj = 2;
                    break;
                }
                break;
            case -90462973:
                if (elementName.equals("mAction")) {
                    obj = 9;
                    break;
                }
                break;
            case -9888733:
                if (elementName.equals("className")) {
                    obj = null;
                    break;
                }
                break;
            case 3328234:
                if (elementName.equals("mQci")) {
                    obj = 7;
                    break;
                }
                break;
            case 183978075:
                if (elementName.equals("mAppAlgorithm")) {
                    obj = 6;
                    break;
                }
                break;
            case 270217909:
                if (elementName.equals("mReserved")) {
                    obj = 13;
                    break;
                }
                break;
            case 292004483:
                if (elementName.equals("monitorUserLearning")) {
                    obj = 8;
                    break;
                }
                break;
            case 1427238722:
                if (elementName.equals("mScenceType")) {
                    obj = 4;
                    break;
                }
                break;
        }
        switch (obj) {
            case null:
                config.className = elementValue;
                break;
            case 1:
                config.mAppId = Integer.parseInt(elementValue);
                break;
            case 2:
            case 3:
                config.mScenceId = Integer.parseInt(elementValue);
                break;
            case 4:
                config.mScenceType = Integer.parseInt(elementValue);
                break;
            case 5:
                config.mAppPeriod = Integer.parseInt(elementValue);
                break;
            case 6:
                config.mAppAlgorithm = Integer.parseInt(elementValue);
                break;
            case 7:
                config.mQci = Integer.parseInt(elementValue);
                break;
            case 8:
                config.monitorUserLearning = Integer.parseInt(elementValue);
                break;
            case 9:
                config.mAction = Integer.parseInt(elementValue);
                break;
            case 10:
                config.mHistoryQoeBadTH = Float.parseFloat(elementValue);
                break;
            case 11:
                config.mGeneralStallTH = Integer.parseInt(elementValue);
                break;
            case 12:
                config.mAggressiveStallTH = Integer.parseInt(elementValue);
                break;
            case 13:
                config.mReserved = elementValue;
                break;
            default:
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("fillAPKConfig, invalid element name:");
                stringBuilder.append(elementName);
                HwAPPQoEUtils.logD(str, stringBuilder.toString());
                break;
        }
    }

    public void fillGameConfig(HwAPPQoEGameConfig config, String elementName, String elementValue) {
        if (elementName == null || config == null) {
            HwAPPQoEUtils.logD(TAG, "fillGameConfig, input error");
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("fillGameConfig:");
        stringBuilder.append(elementName);
        stringBuilder.append(",");
        stringBuilder.append(elementValue);
        HwAPPQoEUtils.logD(str, stringBuilder.toString());
        Object obj = -1;
        switch (elementName.hashCode()) {
            case -2137635847:
                if (elementName.equals("mHistoryQoeBadTH")) {
                    obj = 5;
                    break;
                }
                break;
            case -1838174908:
                if (elementName.equals("mGameKQI")) {
                    obj = 2;
                    break;
                }
                break;
            case -1838167053:
                if (elementName.equals("mGameRtt")) {
                    obj = 3;
                    break;
                }
                break;
            case -503942891:
                if (elementName.equals("mGameAction")) {
                    obj = 4;
                    break;
                }
                break;
            case -154939613:
                if (elementName.equals("mScenceId")) {
                    obj = 1;
                    break;
                }
                break;
            case 79251322:
                if (elementName.equals("mGameId")) {
                    obj = null;
                    break;
                }
                break;
            case 270217909:
                if (elementName.equals("mReserved")) {
                    obj = 6;
                    break;
                }
                break;
        }
        switch (obj) {
            case null:
                config.mGameId = Integer.parseInt(elementValue);
                break;
            case 1:
                config.mScenceId = Integer.parseInt(elementValue);
                break;
            case 2:
                config.mGameKQI = Integer.parseInt(elementValue);
                break;
            case 3:
                config.mGameRtt = Integer.parseInt(elementValue);
                break;
            case 4:
                config.mGameAction = Integer.parseInt(elementValue);
                break;
            case 5:
                config.mHistoryQoeBadTH = Float.parseFloat(elementValue);
                break;
            case 6:
                config.mReserved = elementValue;
                break;
            default:
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("fillGameConfig, invalid element name: ");
                stringBuilder.append(elementName);
                HwAPPQoEUtils.logD(str, stringBuilder.toString());
                break;
        }
    }

    public void fillQCIConfig(HwCHQciConfig config, String elementName, String elementValue) {
        if (elementName == null || config == null) {
            HwAPPQoEUtils.logD(TAG, "fillQCIConfig, input error");
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("fillQCIConfig:");
        stringBuilder.append(elementName);
        stringBuilder.append(",");
        stringBuilder.append(elementValue);
        HwAPPQoEUtils.logD(str, stringBuilder.toString());
        Object obj = -1;
        switch (elementName.hashCode()) {
            case 79991:
                if (elementName.equals("QCI")) {
                    obj = null;
                    break;
                }
                break;
            case 81490:
                if (elementName.equals("RTT")) {
                    obj = 1;
                    break;
                }
                break;
            case 2525271:
                if (elementName.equals("RSSI")) {
                    obj = 2;
                    break;
                }
                break;
            case 2582043:
                if (elementName.equals("TPUT")) {
                    obj = 4;
                    break;
                }
                break;
            case 1986988747:
                if (elementName.equals("CHLOAD")) {
                    obj = 3;
                    break;
                }
                break;
        }
        switch (obj) {
            case null:
                config.mQci = Integer.parseInt(elementValue);
                break;
            case 1:
                config.mRtt = Integer.parseInt(elementValue);
                break;
            case 2:
                config.mRssi = Integer.parseInt(elementValue);
                break;
            case 3:
                config.mChload = Integer.parseInt(elementValue);
                break;
            case 4:
                config.mTput = Integer.parseInt(elementValue);
                break;
            default:
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("fillQCIConfig, invalid element name: ");
                stringBuilder.append(elementName);
                HwAPPQoEUtils.logD(str, stringBuilder.toString());
                break;
        }
    }

    public void fillMpLinkConfig(HwMpLinkConfigInfo config, String elementName, String elementValue) {
        if (elementName == null || config == null) {
            HwAPPQoEUtils.logD(TAG, "fillGameConfig, input error");
            return;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("fillMpLinkConfig:");
        stringBuilder.append(elementName);
        stringBuilder.append(",");
        stringBuilder.append(elementValue);
        HwAPPQoEUtils.logD(str, stringBuilder.toString());
        Object obj = -1;
        switch (elementName.hashCode()) {
            case -861311717:
                if (elementName.equals("condition")) {
                    obj = 6;
                    break;
                }
                break;
            case -793183188:
                if (elementName.equals(MemoryConstant.MEM_POLICY_BIGAPPNAME)) {
                    obj = 1;
                    break;
                }
                break;
            case -434367618:
                if (elementName.equals("gatewaytype")) {
                    obj = 3;
                    break;
                }
                break;
            case -350385368:
                if (elementName.equals("reserved")) {
                    obj = 5;
                    break;
                }
                break;
            case 353371935:
                if (elementName.equals("encrypttype")) {
                    obj = 4;
                    break;
                }
                break;
            case 398021374:
                if (elementName.equals("multnetwork")) {
                    obj = 2;
                    break;
                }
                break;
            case 1127930396:
                if (elementName.equals("custmac")) {
                    obj = null;
                    break;
                }
                break;
        }
        switch (obj) {
            case null:
                config.setmCustMac(elementValue);
                break;
            case 1:
                config.setmAppName(elementValue);
                break;
            case 2:
                config.setmMultNetwork(elementValue);
                break;
            case 3:
                config.setmGatewayType(elementValue);
                break;
            case 4:
                config.setmEncryptType(elementValue);
                break;
            case 5:
                config.setmReserved(elementValue);
                break;
            case 6:
                config.setCondition(elementValue);
                break;
        }
    }

    /* JADX WARNING: Missing block: B:26:0x0068, code:
            if (r0 == null) goto L_0x0080;
     */
    /* JADX WARNING: Missing block: B:27:0x006a, code:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("checkIsMonitorAPKScence end:");
            r2.append(r0);
            com.android.server.hidata.appqoe.HwAPPQoEUtils.logD(r1, r2.toString());
     */
    /* JADX WARNING: Missing block: B:28:0x0080, code:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public HwAPPQoEAPKConfig checkIsMonitorAPKScence(String packageName, String className) {
        HwAPPQoEAPKConfig config = null;
        synchronized (this.mLock) {
            if (!this.isXmlLoadFinsh || this.mAPKConfigList.size() == 0 || packageName == null) {
                return null;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("checkIsMonitorAPKScence input :");
            stringBuilder.append(packageName);
            stringBuilder.append(",");
            stringBuilder.append(className);
            HwAPPQoEUtils.logD(str, stringBuilder.toString());
            for (HwAPPQoEAPKConfig apkConfig : this.mAPKConfigList) {
                if (className == null || !className.contains(apkConfig.className)) {
                    if (packageName.equals(apkConfig.packageName) && apkConfig.mScenceId % 1000 == 0) {
                        config = apkConfig;
                    }
                } else if (3 == apkConfig.mScenceType) {
                    config = null;
                } else {
                    config = apkConfig;
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:18:0x0051, code:
            if (r0 == null) goto L_0x0069;
     */
    /* JADX WARNING: Missing block: B:19:0x0053, code:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("checkIsMonitorVideoScence end:");
            r2.append(r0);
            com.android.server.hidata.appqoe.HwAPPQoEUtils.logD(r1, r2.toString());
     */
    /* JADX WARNING: Missing block: B:20:0x0069, code:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public HwAPPQoEAPKConfig checkIsMonitorVideoScence(String packageName, String className) {
        HwAPPQoEAPKConfig config = null;
        synchronized (this.mLock) {
            if (this.isXmlLoadFinsh && this.mAPKConfigList.size() != 0 && packageName != null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("checkIsMonitorVideoScence input :");
                stringBuilder.append(packageName);
                stringBuilder.append(",");
                stringBuilder.append(className);
                HwAPPQoEUtils.logD(str, stringBuilder.toString());
                for (HwAPPQoEAPKConfig apkConfig : this.mAPKConfigList) {
                    if (className != null && className.contains(apkConfig.className)) {
                        config = apkConfig;
                        break;
                    }
                }
            } else {
                return null;
            }
        }
    }

    /* JADX WARNING: Missing block: B:16:0x0047, code:
            if (r0 == null) goto L_0x005f;
     */
    /* JADX WARNING: Missing block: B:17:0x0049, code:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("checkIsMonitorGameScence end:");
            r2.append(r0);
            com.android.server.hidata.appqoe.HwAPPQoEUtils.logD(r1, r2.toString());
     */
    /* JADX WARNING: Missing block: B:18:0x005f, code:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public HwAPPQoEGameConfig checkIsMonitorGameScence(String packageName) {
        HwAPPQoEGameConfig config = null;
        synchronized (this.mLock) {
            if (!this.isXmlLoadFinsh || this.mGameConfigList.size() == 0 || packageName == null) {
                return null;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("checkIsMonitorGameScence input :");
            stringBuilder.append(packageName);
            HwAPPQoEUtils.logD(str, stringBuilder.toString());
            for (HwAPPQoEGameConfig gameConfig : this.mGameConfigList) {
                if (packageName.contains(gameConfig.mGameName)) {
                    config = gameConfig;
                    break;
                }
            }
        }
    }

    public HwAPPQoEGameConfig getGameScenceConfig(int appId) {
        synchronized (this.mLock) {
            if (!this.isXmlLoadFinsh || this.mGameConfigList.size() == 0) {
                return null;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getGameScenceConfig input :");
            stringBuilder.append(appId);
            HwAPPQoEUtils.logD(str, stringBuilder.toString());
            for (HwAPPQoEGameConfig gameConfig : this.mGameConfigList) {
                if (appId == gameConfig.mGameId) {
                    str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("getGameScenceConfig:");
                    stringBuilder2.append(gameConfig.toString());
                    HwAPPQoEUtils.logD(str, stringBuilder2.toString());
                    return gameConfig;
                }
            }
            HwAPPQoEUtils.logD(TAG, "getGameScenceConfig, not found");
            return null;
        }
    }

    /* JADX WARNING: Missing block: B:37:0x0066, code:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("getScenceAction, action:");
            r2.append(r0);
            com.android.server.hidata.appqoe.HwAPPQoEUtils.logD(r1, r2.toString());
     */
    /* JADX WARNING: Missing block: B:38:0x007c, code:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int getScenceAction(int appType, int appId, int scenceId) {
        int scenceAction = -1;
        synchronized (this.mLock) {
            if (this.isXmlLoadFinsh) {
                if ((1000 == appType || HwAPPQoEUtils.APP_TYPE_STREAMING == appType) && this.mAPKConfigList.size() != 0) {
                    for (HwAPPQoEAPKConfig apkConfig : this.mAPKConfigList) {
                        if (appId == apkConfig.mAppId && scenceId == apkConfig.mScenceId) {
                            scenceAction = apkConfig.mAction;
                            break;
                        }
                    }
                }
                if (2000 == appType && this.mGameConfigList.size() != 0) {
                    for (HwAPPQoEGameConfig gameConfig : this.mGameConfigList) {
                        if (appId == gameConfig.mGameId && scenceId == gameConfig.mScenceId) {
                            scenceAction = gameConfig.mGameAction;
                            break;
                        }
                    }
                }
            } else {
                return -1;
            }
        }
    }

    public HwAPPQoEAPKConfig getAPKScenceConfig(int scenceId) {
        synchronized (this.mLock) {
            if (!this.isXmlLoadFinsh || this.mAPKConfigList.size() == 0) {
                return null;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getAPKScenceConfig input :");
            stringBuilder.append(scenceId);
            HwAPPQoEUtils.logD(str, stringBuilder.toString());
            for (HwAPPQoEAPKConfig apkConfig : this.mAPKConfigList) {
                if (scenceId == apkConfig.mScenceId) {
                    str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("getAPKScenceConfig:");
                    stringBuilder2.append(apkConfig.toString());
                    HwAPPQoEUtils.logD(str, stringBuilder2.toString());
                    return apkConfig;
                }
            }
            HwAPPQoEUtils.logD(TAG, "getAPKScenceConfig, not found");
            return null;
        }
    }

    public List<HwAPPQoEAPKConfig> getAPKConfigList() {
        List<HwAPPQoEAPKConfig> list;
        synchronized (this.mLock) {
            list = this.mAPKConfigList;
        }
        return list;
    }

    public List<HwAPPQoEGameConfig> getGameConfigList() {
        List<HwAPPQoEGameConfig> list;
        synchronized (this.mLock) {
            list = this.mGameConfigList;
        }
        return list;
    }
}
