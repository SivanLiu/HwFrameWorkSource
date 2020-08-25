package com.android.server.hidata.appqoe;

import android.emcom.EmcomManager;
import android.net.wifi.ScanResult;
import android.util.Xml;
import com.android.server.hidata.channelqoe.HwCHQciConfig;
import com.android.server.hidata.channelqoe.HwCHQciManager;
import com.android.server.hidata.mplink.HwMpLinkConfigInfo;
import com.android.server.hidata.mplink.HwMpLinkContentAware;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.huawei.hiai.awareness.AwarenessInnerConstants;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class HwAPPQoEResourceMangerImpl {
    private static final String CFG_APP_REGION = "mAppRegion";
    private static final String CFG_FILE_NAME = "/hidata_config_cust.xml";
    private static final String CFG_GAME_SPECIALINFO_SOURCES = "mGameSpecialInfoSources";
    private static final String CFG_VER_DIR = "emcom/noncell";
    private static final int FEATURE_ID = 3;
    private static final int FORMAT_OUI_VENDOR_LENGTH = 3;
    private static final int HEX_FLAG = 16;
    private static final int HI110X_MASK = 255;
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
    private List<HwAppQoeBlackListConfig> mBlackListConfigList = new ArrayList();
    private List<HwAPPQoEGameConfig> mGameConfigList = new ArrayList();
    private HwCHQciManager mHwCHQciManager;
    private HwMpLinkContentAware mHwMpLinkContentAware;
    private final Object mLock = new Object();
    private List<HwAppQoeOuiBlackListConfig> mOuiBlackListConfigList = new ArrayList();
    private List<HwAppQoeWhiteListConfig> mWhiteListConfigList = new ArrayList();

    public HwAPPQoEResourceMangerImpl() {
        init();
    }

    private void init() {
        this.mHwMpLinkContentAware = HwMpLinkContentAware.onlyGetInstance();
        this.mHwCHQciManager = HwCHQciManager.getInstance();
        new Thread(new MyRunnable()).start();
    }

    public class MyRunnable implements Runnable {
        public MyRunnable() {
        }

        public void run() {
            HwAPPQoEUtils.logD(HwAPPQoEResourceMangerImpl.TAG, false, "Start read thread", new Object[0]);
            HwAPPQoEResourceMangerImpl.this.readAppConfigList();
        }
    }

    public void onConfigFilePathChanged() {
        new Thread(new MyRunnable()).start();
    }

    public void responseForParaUpdate(int result) {
        HwAPPQoEUtils.logD(TAG, false, "appqoe response,  result: %{public}d", Integer.valueOf(result));
        EmcomManager mEmcomManager = EmcomManager.getInstance();
        if (mEmcomManager != null) {
            mEmcomManager.responseForParaUpgrade(16, 1, result);
        }
    }

    public String getConfigFilePath() {
        HwAPPQoEUtils.logD(TAG, false, "getConfigFilePath, enter", new Object[0]);
        try {
            String[] cfgFileInfo = HwCfgFilePolicy.getDownloadCfgFile(CFG_VER_DIR, "emcom/noncell/hidata_config_cust.xml");
            if (cfgFileInfo == null) {
                HwAPPQoEUtils.logE(HwAPPQoEUtils.TAG, false, "Both default and cota config files not exist", new Object[0]);
                responseForParaUpdate(0);
                return null;
            } else if (cfgFileInfo[0].contains("/cota")) {
                HwAPPQoEUtils.logD(TAG, false, "cota config file path is: %{public}s, version:%{public}s", cfgFileInfo[0], cfgFileInfo[1]);
                return cfgFileInfo[0];
            } else {
                HwAPPQoEUtils.logD(TAG, false, "system config file path is: %{public}s, version:%{public}s", cfgFileInfo[0], cfgFileInfo[1]);
                return cfgFileInfo[0];
            }
        } catch (Exception e) {
            HwAPPQoEUtils.logE(TAG, false, "getConfigFilePath failed by Exception", new Object[0]);
            responseForParaUpdate(6);
            return null;
        }
    }

    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:310:0x0471 */
    /* JADX DEBUG: Failed to insert an additional move for type inference into block B:26:0x0093 */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX INFO: Multiple debug info for r20v20 java.io.InputStream: [D('inputStream' java.io.InputStream), D('upgradedWhiteListConfigList' java.util.List<com.android.server.hidata.appqoe.HwAppQoeWhiteListConfig>)] */
    /* JADX WARN: Type inference failed for: r20v25 */
    /* JADX WARN: Type inference failed for: r20v26 */
    /* JADX WARN: Type inference failed for: r20v27 */
    /* JADX WARN: Type inference failed for: r20v28 */
    /* JADX WARN: Type inference failed for: r20v37 */
    /* JADX WARN: Type inference failed for: r20v38 */
    /* JADX WARN: Type inference failed for: r20v39 */
    /* JADX WARN: Type inference failed for: r20v44 */
    /* JADX WARN: Type inference failed for: r20v45 */
    /* JADX WARN: Type inference failed for: r20v50 */
    /* JADX WARN: Type inference failed for: r20v51 */
    /* JADX WARNING: Code restructure failed: missing block: B:216:?, code lost:
        responseForParaUpdate(8);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:218:?, code lost:
        r20.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:220:0x050b, code lost:
        com.android.server.hidata.appqoe.HwAPPQoEUtils.logE(com.android.server.hidata.appqoe.HwAPPQoEResourceMangerImpl.TAG, false, "readAppConfigList failed by Exception", new java.lang.Object[0]);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:226:0x052d, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:227:0x052e, code lost:
        r2 = r0;
        r20 = r20;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:228:0x0533, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:229:0x0534, code lost:
        r2 = r0;
        r7 = r8;
        r8 = r9;
        r9 = r10;
        r10 = r11;
        r11 = r13;
        r3 = r3;
        r13 = r5;
        r20 = r20;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:230:0x053f, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:231:0x0540, code lost:
        r2 = r0;
        r7 = r8;
        r8 = r9;
        r9 = r10;
        r10 = r11;
        r11 = r13;
        r3 = r3;
        r13 = r5;
        r20 = r20;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:232:0x054b, code lost:
        r0 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:233:0x054c, code lost:
        r2 = r0;
        r7 = r8;
        r8 = r9;
        r9 = r10;
        r10 = r11;
        r11 = r13;
        r3 = r3;
        r13 = r5;
        r20 = r20;
     */
    /* JADX WARNING: Removed duplicated region for block: B:268:0x0616 A[SYNTHETIC, Splitter:B:268:0x0616] */
    /* JADX WARNING: Removed duplicated region for block: B:276:0x063d A[SYNTHETIC, Splitter:B:276:0x063d] */
    /* JADX WARNING: Removed duplicated region for block: B:284:0x0663 A[SYNTHETIC, Splitter:B:284:0x0663] */
    /* JADX WARNING: Removed duplicated region for block: B:292:0x0686  */
    /* JADX WARNING: Removed duplicated region for block: B:304:0x0699 A[SYNTHETIC, Splitter:B:304:0x0699] */
    public void readAppConfigList() {
        InputStream inputStream;
        Throwable th;
        InputStream inputStream2;
        FileNotFoundException e;
        InputStream inputStream3;
        IOException e2;
        InputStream inputStream4;
        XmlPullParserException e3;
        Throwable th2;
        String configFilePath;
        File configPath;
        ArrayList arrayList;
        List<HwAppQoeBlackListConfig> upGratedBlackListConfigList;
        List<HwAPPQoEGameConfig> mUpGratedGameConfigList;
        XmlPullParser xmlPullParser = null;
        xmlPullParser = null;
        xmlPullParser = null;
        xmlPullParser = null;
        xmlPullParser = null;
        xmlPullParser = null;
        String configFilePath2 = getConfigFilePath();
        if (configFilePath2 == null) {
            HwAPPQoEUtils.logD(TAG, false, "readAppConfigList, configPath is null", new Object[0]);
            return;
        }
        File configPath2 = new File(configFilePath2);
        if (!configPath2.exists()) {
            responseForParaUpdate(0);
            HwAPPQoEUtils.logD(TAG, false, "readAppConfigList, configPath not exit", new Object[0]);
            return;
        }
        HwAPPQoEAPKConfig mAPKConfig = null;
        mAPKConfig = null;
        mAPKConfig = null;
        mAPKConfig = null;
        mAPKConfig = null;
        mAPKConfig = null;
        mAPKConfig = null;
        HwAPPQoEAPKConfig mAPKConfig2 = null;
        HwAPPQoEAPKConfig mAPKConfig3 = null;
        HwAPPQoEGameConfig mGameConfig = null;
        mGameConfig = null;
        mGameConfig = null;
        mGameConfig = null;
        mGameConfig = null;
        mGameConfig = null;
        mGameConfig = null;
        HwAPPQoEGameConfig mGameConfig2 = null;
        HwAPPQoEGameConfig mGameConfig3 = null;
        HwAppQoeBlackListConfig blackListConfig = null;
        blackListConfig = null;
        blackListConfig = null;
        blackListConfig = null;
        blackListConfig = null;
        blackListConfig = null;
        blackListConfig = null;
        HwAppQoeBlackListConfig blackListConfig2 = null;
        HwAppQoeBlackListConfig blackListConfig3 = null;
        HwAppQoeWhiteListConfig whiteListConfig = null;
        whiteListConfig = null;
        whiteListConfig = null;
        whiteListConfig = null;
        whiteListConfig = null;
        whiteListConfig = null;
        whiteListConfig = null;
        HwAppQoeWhiteListConfig whiteListConfig2 = null;
        HwAppQoeWhiteListConfig whiteListConfig3 = null;
        HwAppQoeOuiBlackListConfig ouiBlackListConfig = null;
        ouiBlackListConfig = null;
        ouiBlackListConfig = null;
        ouiBlackListConfig = null;
        ouiBlackListConfig = null;
        ouiBlackListConfig = null;
        ouiBlackListConfig = null;
        HwAppQoeOuiBlackListConfig ouiBlackListConfig2 = null;
        HwAppQoeOuiBlackListConfig ouiBlackListConfig3 = null;
        HwMpLinkConfigInfo mHwMpLinkConfigInfo = null;
        mHwMpLinkConfigInfo = null;
        mHwMpLinkConfigInfo = null;
        mHwMpLinkConfigInfo = null;
        mHwMpLinkConfigInfo = null;
        mHwMpLinkConfigInfo = null;
        mHwMpLinkConfigInfo = null;
        mHwMpLinkConfigInfo = null;
        mHwMpLinkConfigInfo = null;
        mHwMpLinkConfigInfo = null;
        HwCHQciConfig mQCIConfig = null;
        mQCIConfig = null;
        mQCIConfig = null;
        mQCIConfig = null;
        mQCIConfig = null;
        mQCIConfig = null;
        mQCIConfig = null;
        HwCHQciConfig mQCIConfig2 = null;
        HwCHQciConfig mQCIConfig3 = null;
        int i = 1;
        try {
            InputStream inputStream5 = new FileInputStream(configPath2);
            try {
                xmlPullParser = Xml.newPullParser();
            } catch (FileNotFoundException e4) {
                inputStream3 = inputStream5;
                e = e4;
                HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 1:%{public}s", e.getMessage());
                responseForParaUpdate(0);
                if (inputStream2 != null) {
                }
                synchronized (this.mLock) {
                }
            } catch (IOException e5) {
                inputStream4 = inputStream5;
                e2 = e5;
                HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 2:%{public}s", e2.getMessage());
                responseForParaUpdate(6);
                if (inputStream2 != null) {
                }
                synchronized (this.mLock) {
                }
            } catch (XmlPullParserException e6) {
                inputStream2 = inputStream5;
                e3 = e6;
                try {
                    HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 3:%{public}s", e3.getMessage());
                    responseForParaUpdate(6);
                    if (inputStream2 != null) {
                    }
                    synchronized (this.mLock) {
                    }
                } catch (Throwable th3) {
                    th = th3;
                    inputStream = inputStream2;
                    if (inputStream != null) {
                    }
                    throw th;
                }
            } catch (Throwable th4) {
                inputStream = inputStream5;
                th = th4;
                if (inputStream != null) {
                }
                throw th;
            }
            try {
                xmlPullParser.setInput(inputStream5, "utf-8");
                List<HwAPPQoEAPKConfig> mUpGratedAPKConfigList = new ArrayList<>();
                List<HwAPPQoEGameConfig> mUpGratedGameConfigList2 = new ArrayList<>();
                List<HwAppQoeBlackListConfig> upGratedBlackListConfigList2 = new ArrayList<>();
                InputStream inputStream6 = new ArrayList();
                List<HwAppQoeOuiBlackListConfig> upGratedOuiBlackListConfigList = new ArrayList<>();
                HwCHQciConfig mQCIConfig4 = null;
                HwAppQoeOuiBlackListConfig ouiBlackListConfig4 = null;
                HwAppQoeWhiteListConfig whiteListConfig4 = null;
                HwAppQoeBlackListConfig blackListConfig4 = null;
                HwAPPQoEGameConfig mGameConfig4 = null;
                HwAPPQoEAPKConfig mAPKConfig4 = null;
                int eventType = xmlPullParser.getEventType();
                while (eventType != i) {
                    if (eventType == 0) {
                        configFilePath = configFilePath2;
                        mUpGratedGameConfigList = mUpGratedGameConfigList2;
                        configPath = configPath2;
                        upGratedBlackListConfigList = upGratedBlackListConfigList2;
                        arrayList = inputStream6;
                        inputStream6 = inputStream5;
                    } else if (eventType == 2) {
                        configFilePath = configFilePath2;
                        mUpGratedGameConfigList = mUpGratedGameConfigList2;
                        configPath = configPath2;
                        upGratedBlackListConfigList = upGratedBlackListConfigList2;
                        arrayList = inputStream6;
                        inputStream6 = inputStream5;
                        if ("APKInfo".equals(xmlPullParser.getName())) {
                            HwAPPQoEAPKConfig mAPKConfig5 = new HwAPPQoEAPKConfig();
                            try {
                                mAPKConfig5.packageName = xmlPullParser.getAttributeValue(0);
                                mAPKConfig4 = mAPKConfig5;
                            } catch (FileNotFoundException e7) {
                                mAPKConfig = mAPKConfig5;
                                mGameConfig = mGameConfig4;
                                blackListConfig = blackListConfig4;
                                whiteListConfig = whiteListConfig4;
                                ouiBlackListConfig = ouiBlackListConfig4;
                                e = e7;
                                mQCIConfig = mQCIConfig4;
                                inputStream3 = inputStream6;
                                HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 1:%{public}s", e.getMessage());
                                responseForParaUpdate(0);
                                if (inputStream2 != null) {
                                }
                                synchronized (this.mLock) {
                                }
                            } catch (IOException e8) {
                                mAPKConfig3 = mAPKConfig5;
                                mGameConfig3 = mGameConfig4;
                                blackListConfig3 = blackListConfig4;
                                whiteListConfig3 = whiteListConfig4;
                                ouiBlackListConfig3 = ouiBlackListConfig4;
                                e2 = e8;
                                mQCIConfig3 = mQCIConfig4;
                                inputStream4 = inputStream6;
                                HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 2:%{public}s", e2.getMessage());
                                responseForParaUpdate(6);
                                if (inputStream2 != null) {
                                }
                                synchronized (this.mLock) {
                                }
                            } catch (XmlPullParserException e9) {
                                mAPKConfig2 = mAPKConfig5;
                                mGameConfig2 = mGameConfig4;
                                blackListConfig2 = blackListConfig4;
                                whiteListConfig2 = whiteListConfig4;
                                ouiBlackListConfig2 = ouiBlackListConfig4;
                                e3 = e9;
                                mQCIConfig2 = mQCIConfig4;
                                inputStream2 = inputStream6;
                                HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 3:%{public}s", e3.getMessage());
                                responseForParaUpdate(6);
                                if (inputStream2 != null) {
                                }
                                synchronized (this.mLock) {
                                }
                            } catch (Throwable th5) {
                                th = th5;
                                inputStream = inputStream6;
                                if (inputStream != null) {
                                }
                                throw th;
                            }
                        } else if ("GameInfo".equals(xmlPullParser.getName())) {
                            HwAPPQoEGameConfig mGameConfig5 = new HwAPPQoEGameConfig();
                            try {
                                mGameConfig5.mGameName = xmlPullParser.getAttributeValue(0);
                                mGameConfig4 = mGameConfig5;
                            } catch (FileNotFoundException e10) {
                                mAPKConfig = mAPKConfig4;
                                blackListConfig = blackListConfig4;
                                whiteListConfig = whiteListConfig4;
                                ouiBlackListConfig = ouiBlackListConfig4;
                                mGameConfig = mGameConfig5;
                                mQCIConfig = mQCIConfig4;
                                e = e10;
                                inputStream3 = inputStream6;
                                HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 1:%{public}s", e.getMessage());
                                responseForParaUpdate(0);
                                if (inputStream2 != null) {
                                }
                                synchronized (this.mLock) {
                                }
                            } catch (IOException e11) {
                                mAPKConfig3 = mAPKConfig4;
                                blackListConfig3 = blackListConfig4;
                                whiteListConfig3 = whiteListConfig4;
                                ouiBlackListConfig3 = ouiBlackListConfig4;
                                mGameConfig3 = mGameConfig5;
                                mQCIConfig3 = mQCIConfig4;
                                e2 = e11;
                                inputStream4 = inputStream6;
                                HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 2:%{public}s", e2.getMessage());
                                responseForParaUpdate(6);
                                if (inputStream2 != null) {
                                }
                                synchronized (this.mLock) {
                                }
                            } catch (XmlPullParserException e12) {
                                mAPKConfig2 = mAPKConfig4;
                                blackListConfig2 = blackListConfig4;
                                whiteListConfig2 = whiteListConfig4;
                                ouiBlackListConfig2 = ouiBlackListConfig4;
                                mGameConfig2 = mGameConfig5;
                                mQCIConfig2 = mQCIConfig4;
                                e3 = e12;
                                inputStream2 = inputStream6;
                                HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 3:%{public}s", e3.getMessage());
                                responseForParaUpdate(6);
                                if (inputStream2 != null) {
                                }
                                synchronized (this.mLock) {
                                }
                            } catch (Throwable th6) {
                                th = th6;
                                inputStream = inputStream6;
                                if (inputStream != null) {
                                }
                                throw th;
                            }
                        } else if ("mplink_version".equals(xmlPullParser.getName())) {
                            if (this.mHwMpLinkContentAware != null) {
                                this.mHwMpLinkContentAware.setMpLinkVersion(xmlPullParser.nextText());
                            }
                        } else if ("mplink_enable".equals(xmlPullParser.getName())) {
                            if (this.mHwMpLinkContentAware != null) {
                                this.mHwMpLinkContentAware.setMpLinkEnable(xmlPullParser.nextText());
                            }
                        } else if ("vendor".equals(xmlPullParser.getName())) {
                            mHwMpLinkConfigInfo = new HwMpLinkConfigInfo();
                            mHwMpLinkConfigInfo.setmVendorOui(xmlPullParser.getAttributeValue(0));
                        } else if ("QciInfo".equals(xmlPullParser.getName())) {
                            mQCIConfig4 = new HwCHQciConfig();
                        } else if ("AppBlackList".equals(xmlPullParser.getName())) {
                            HwAppQoeBlackListConfig blackListConfig5 = new HwAppQoeBlackListConfig();
                            try {
                                blackListConfig5.setPackageName(xmlPullParser.getAttributeValue(0));
                                blackListConfig4 = blackListConfig5;
                            } catch (FileNotFoundException e13) {
                                mAPKConfig = mAPKConfig4;
                                mGameConfig = mGameConfig4;
                                whiteListConfig = whiteListConfig4;
                                ouiBlackListConfig = ouiBlackListConfig4;
                                blackListConfig = blackListConfig5;
                                mQCIConfig = mQCIConfig4;
                                e = e13;
                                inputStream3 = inputStream6;
                                HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 1:%{public}s", e.getMessage());
                                responseForParaUpdate(0);
                                if (inputStream2 != null) {
                                }
                                synchronized (this.mLock) {
                                }
                            } catch (IOException e14) {
                                mAPKConfig3 = mAPKConfig4;
                                mGameConfig3 = mGameConfig4;
                                whiteListConfig3 = whiteListConfig4;
                                ouiBlackListConfig3 = ouiBlackListConfig4;
                                blackListConfig3 = blackListConfig5;
                                mQCIConfig3 = mQCIConfig4;
                                e2 = e14;
                                inputStream4 = inputStream6;
                                HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 2:%{public}s", e2.getMessage());
                                responseForParaUpdate(6);
                                if (inputStream2 != null) {
                                }
                                synchronized (this.mLock) {
                                }
                            } catch (XmlPullParserException e15) {
                                mAPKConfig2 = mAPKConfig4;
                                mGameConfig2 = mGameConfig4;
                                whiteListConfig2 = whiteListConfig4;
                                ouiBlackListConfig2 = ouiBlackListConfig4;
                                blackListConfig2 = blackListConfig5;
                                mQCIConfig2 = mQCIConfig4;
                                e3 = e15;
                                inputStream2 = inputStream6;
                                HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 3:%{public}s", e3.getMessage());
                                responseForParaUpdate(6);
                                if (inputStream2 != null) {
                                }
                                synchronized (this.mLock) {
                                }
                            } catch (Throwable th7) {
                                th = th7;
                                inputStream = inputStream6;
                                if (inputStream != null) {
                                }
                                throw th;
                            }
                        } else if ("AppWhiteList".equals(xmlPullParser.getName())) {
                            HwAppQoeWhiteListConfig whiteListConfig5 = new HwAppQoeWhiteListConfig();
                            try {
                                whiteListConfig5.setPackageName(xmlPullParser.getAttributeValue(0));
                                whiteListConfig4 = whiteListConfig5;
                            } catch (FileNotFoundException e16) {
                                mAPKConfig = mAPKConfig4;
                                mGameConfig = mGameConfig4;
                                blackListConfig = blackListConfig4;
                                ouiBlackListConfig = ouiBlackListConfig4;
                                whiteListConfig = whiteListConfig5;
                                mQCIConfig = mQCIConfig4;
                                e = e16;
                                inputStream3 = inputStream6;
                                HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 1:%{public}s", e.getMessage());
                                responseForParaUpdate(0);
                                if (inputStream2 != null) {
                                }
                                synchronized (this.mLock) {
                                }
                            } catch (IOException e17) {
                                mAPKConfig3 = mAPKConfig4;
                                mGameConfig3 = mGameConfig4;
                                blackListConfig3 = blackListConfig4;
                                ouiBlackListConfig3 = ouiBlackListConfig4;
                                whiteListConfig3 = whiteListConfig5;
                                mQCIConfig3 = mQCIConfig4;
                                e2 = e17;
                                inputStream4 = inputStream6;
                                HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 2:%{public}s", e2.getMessage());
                                responseForParaUpdate(6);
                                if (inputStream2 != null) {
                                }
                                synchronized (this.mLock) {
                                }
                            } catch (XmlPullParserException e18) {
                                mAPKConfig2 = mAPKConfig4;
                                mGameConfig2 = mGameConfig4;
                                blackListConfig2 = blackListConfig4;
                                ouiBlackListConfig2 = ouiBlackListConfig4;
                                whiteListConfig2 = whiteListConfig5;
                                mQCIConfig2 = mQCIConfig4;
                                e3 = e18;
                                inputStream2 = inputStream6;
                                HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 3:%{public}s", e3.getMessage());
                                responseForParaUpdate(6);
                                if (inputStream2 != null) {
                                }
                                synchronized (this.mLock) {
                                }
                            } catch (Throwable th8) {
                                th = th8;
                                inputStream = inputStream6;
                                if (inputStream != null) {
                                }
                                throw th;
                            }
                        } else if ("OuiBlackList".equals(xmlPullParser.getName())) {
                            HwAppQoeOuiBlackListConfig ouiBlackListConfig5 = new HwAppQoeOuiBlackListConfig();
                            try {
                                ouiBlackListConfig5.setOuiName(xmlPullParser.getAttributeValue(0));
                                ouiBlackListConfig4 = ouiBlackListConfig5;
                            } catch (FileNotFoundException e19) {
                                mQCIConfig = mQCIConfig4;
                                mAPKConfig = mAPKConfig4;
                                mGameConfig = mGameConfig4;
                                blackListConfig = blackListConfig4;
                                whiteListConfig = whiteListConfig4;
                                ouiBlackListConfig = ouiBlackListConfig5;
                                e = e19;
                                inputStream3 = inputStream6;
                                HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 1:%{public}s", e.getMessage());
                                responseForParaUpdate(0);
                                if (inputStream2 != null) {
                                }
                                synchronized (this.mLock) {
                                }
                            } catch (IOException e20) {
                                mQCIConfig3 = mQCIConfig4;
                                mAPKConfig3 = mAPKConfig4;
                                mGameConfig3 = mGameConfig4;
                                blackListConfig3 = blackListConfig4;
                                whiteListConfig3 = whiteListConfig4;
                                ouiBlackListConfig3 = ouiBlackListConfig5;
                                e2 = e20;
                                inputStream4 = inputStream6;
                                HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 2:%{public}s", e2.getMessage());
                                responseForParaUpdate(6);
                                if (inputStream2 != null) {
                                }
                                synchronized (this.mLock) {
                                }
                            } catch (XmlPullParserException e21) {
                                mQCIConfig2 = mQCIConfig4;
                                mAPKConfig2 = mAPKConfig4;
                                mGameConfig2 = mGameConfig4;
                                blackListConfig2 = blackListConfig4;
                                whiteListConfig2 = whiteListConfig4;
                                ouiBlackListConfig2 = ouiBlackListConfig5;
                                e3 = e21;
                                inputStream2 = inputStream6;
                                HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 3:%{public}s", e3.getMessage());
                                responseForParaUpdate(6);
                                if (inputStream2 != null) {
                                }
                                synchronized (this.mLock) {
                                }
                            } catch (Throwable th9) {
                                th = th9;
                                inputStream = inputStream6;
                                if (inputStream != null) {
                                }
                                throw th;
                            }
                        } else if (mAPKConfig4 != null) {
                            fillAPKConfig(mAPKConfig4, xmlPullParser.getName(), xmlPullParser.nextText());
                        } else if (mGameConfig4 != null) {
                            fillGameConfig(mGameConfig4, xmlPullParser.getName(), xmlPullParser.nextText());
                        } else if (mHwMpLinkConfigInfo != null) {
                            fillMpLinkConfig(mHwMpLinkConfigInfo, xmlPullParser.getName(), xmlPullParser.nextText());
                        } else if (mQCIConfig4 != null) {
                            fillQCIConfig(mQCIConfig4, xmlPullParser.getName(), xmlPullParser.nextText());
                        } else if (blackListConfig4 != null) {
                            fillBlackListConfig(blackListConfig4, xmlPullParser.getName(), xmlPullParser.nextText());
                        } else if (whiteListConfig4 != null) {
                            fillWhiteListConfig(whiteListConfig4, xmlPullParser.getName(), xmlPullParser.nextText());
                        } else if (ouiBlackListConfig4 != null) {
                            fillOuiBlackListConfig(ouiBlackListConfig4, xmlPullParser.getName(), xmlPullParser.nextText());
                        }
                    } else if (eventType != 3) {
                        configFilePath = configFilePath2;
                        mUpGratedGameConfigList = mUpGratedGameConfigList2;
                        configPath = configPath2;
                        upGratedBlackListConfigList = upGratedBlackListConfigList2;
                        arrayList = inputStream6;
                        inputStream6 = inputStream5;
                    } else {
                        configFilePath = configFilePath2;
                        try {
                            if ("APKInfo".equals(xmlPullParser.getName())) {
                                try {
                                    mUpGratedAPKConfigList.add(mAPKConfig4);
                                    mAPKConfig4 = null;
                                    mUpGratedGameConfigList = mUpGratedGameConfigList2;
                                    configPath = configPath2;
                                    upGratedBlackListConfigList = upGratedBlackListConfigList2;
                                    arrayList = inputStream6;
                                    inputStream6 = inputStream5;
                                } catch (FileNotFoundException e22) {
                                    inputStream3 = inputStream5;
                                    mAPKConfig = mAPKConfig4;
                                    mGameConfig = mGameConfig4;
                                    blackListConfig = blackListConfig4;
                                    whiteListConfig = whiteListConfig4;
                                    ouiBlackListConfig = ouiBlackListConfig4;
                                    e = e22;
                                    mQCIConfig = mQCIConfig4;
                                    HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 1:%{public}s", e.getMessage());
                                    responseForParaUpdate(0);
                                    if (inputStream2 != null) {
                                    }
                                    synchronized (this.mLock) {
                                    }
                                } catch (IOException e23) {
                                    inputStream4 = inputStream5;
                                    mAPKConfig3 = mAPKConfig4;
                                    mGameConfig3 = mGameConfig4;
                                    blackListConfig3 = blackListConfig4;
                                    whiteListConfig3 = whiteListConfig4;
                                    ouiBlackListConfig3 = ouiBlackListConfig4;
                                    e2 = e23;
                                    mQCIConfig3 = mQCIConfig4;
                                    HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 2:%{public}s", e2.getMessage());
                                    responseForParaUpdate(6);
                                    if (inputStream2 != null) {
                                    }
                                    synchronized (this.mLock) {
                                    }
                                } catch (XmlPullParserException e24) {
                                    inputStream2 = inputStream5;
                                    mAPKConfig2 = mAPKConfig4;
                                    mGameConfig2 = mGameConfig4;
                                    blackListConfig2 = blackListConfig4;
                                    whiteListConfig2 = whiteListConfig4;
                                    ouiBlackListConfig2 = ouiBlackListConfig4;
                                    e3 = e24;
                                    mQCIConfig2 = mQCIConfig4;
                                    HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 3:%{public}s", e3.getMessage());
                                    responseForParaUpdate(6);
                                    if (inputStream2 != null) {
                                    }
                                    synchronized (this.mLock) {
                                    }
                                } catch (Throwable th10) {
                                    inputStream = inputStream5;
                                    th = th10;
                                    if (inputStream != null) {
                                    }
                                    throw th;
                                }
                            } else if ("GameInfo".equals(xmlPullParser.getName())) {
                                mUpGratedGameConfigList = mUpGratedGameConfigList2;
                                mUpGratedGameConfigList.add(mGameConfig4);
                                mGameConfig4 = null;
                                configPath = configPath2;
                                upGratedBlackListConfigList = upGratedBlackListConfigList2;
                                arrayList = inputStream6;
                                inputStream6 = inputStream5;
                            } else {
                                mUpGratedGameConfigList = mUpGratedGameConfigList2;
                                configPath = configPath2;
                                try {
                                    if ("vendor".equals(xmlPullParser.getName())) {
                                        try {
                                            if (this.mHwMpLinkContentAware != null) {
                                                this.mHwMpLinkContentAware.addMpLinkDeviceApp(mHwMpLinkConfigInfo);
                                            }
                                            mHwMpLinkConfigInfo = null;
                                            upGratedBlackListConfigList = upGratedBlackListConfigList2;
                                            arrayList = inputStream6;
                                            inputStream6 = inputStream5;
                                        } catch (FileNotFoundException e25) {
                                            inputStream3 = inputStream5;
                                            mAPKConfig = mAPKConfig4;
                                            mGameConfig = mGameConfig4;
                                            blackListConfig = blackListConfig4;
                                            whiteListConfig = whiteListConfig4;
                                            ouiBlackListConfig = ouiBlackListConfig4;
                                            e = e25;
                                            mQCIConfig = mQCIConfig4;
                                            HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 1:%{public}s", e.getMessage());
                                            responseForParaUpdate(0);
                                            if (inputStream2 != null) {
                                            }
                                            synchronized (this.mLock) {
                                            }
                                        } catch (IOException e26) {
                                            inputStream4 = inputStream5;
                                            mAPKConfig3 = mAPKConfig4;
                                            mGameConfig3 = mGameConfig4;
                                            blackListConfig3 = blackListConfig4;
                                            whiteListConfig3 = whiteListConfig4;
                                            ouiBlackListConfig3 = ouiBlackListConfig4;
                                            e2 = e26;
                                            mQCIConfig3 = mQCIConfig4;
                                            HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 2:%{public}s", e2.getMessage());
                                            responseForParaUpdate(6);
                                            if (inputStream2 != null) {
                                            }
                                            synchronized (this.mLock) {
                                            }
                                        } catch (XmlPullParserException e27) {
                                            inputStream2 = inputStream5;
                                            mAPKConfig2 = mAPKConfig4;
                                            mGameConfig2 = mGameConfig4;
                                            blackListConfig2 = blackListConfig4;
                                            whiteListConfig2 = whiteListConfig4;
                                            ouiBlackListConfig2 = ouiBlackListConfig4;
                                            e3 = e27;
                                            mQCIConfig2 = mQCIConfig4;
                                            HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 3:%{public}s", e3.getMessage());
                                            responseForParaUpdate(6);
                                            if (inputStream2 != null) {
                                            }
                                            synchronized (this.mLock) {
                                            }
                                        } catch (Throwable th11) {
                                            inputStream = inputStream5;
                                            th = th11;
                                            if (inputStream != null) {
                                            }
                                            throw th;
                                        }
                                    } else if ("AppBlackList".equals(xmlPullParser.getName())) {
                                        upGratedBlackListConfigList = upGratedBlackListConfigList2;
                                        upGratedBlackListConfigList.add(blackListConfig4);
                                        blackListConfig4 = null;
                                        arrayList = inputStream6;
                                        inputStream6 = inputStream5;
                                    } else {
                                        upGratedBlackListConfigList = upGratedBlackListConfigList2;
                                        if ("AppWhiteList".equals(xmlPullParser.getName())) {
                                            arrayList = inputStream6;
                                            arrayList.add(whiteListConfig4);
                                            whiteListConfig4 = null;
                                            inputStream6 = inputStream5;
                                        } else {
                                            arrayList = inputStream6;
                                            inputStream6 = inputStream5;
                                            try {
                                                if ("OuiBlackList".equals(xmlPullParser.getName())) {
                                                    upGratedOuiBlackListConfigList.add(ouiBlackListConfig4);
                                                    ouiBlackListConfig4 = null;
                                                    upGratedOuiBlackListConfigList = upGratedOuiBlackListConfigList;
                                                } else {
                                                    upGratedOuiBlackListConfigList = upGratedOuiBlackListConfigList;
                                                    if ("QciInfo".equals(xmlPullParser.getName())) {
                                                        if (this.mHwCHQciManager != null) {
                                                            this.mHwCHQciManager.addConfig(mQCIConfig4);
                                                        }
                                                        mQCIConfig4 = null;
                                                    }
                                                }
                                            } catch (FileNotFoundException e28) {
                                                e = e28;
                                                mAPKConfig = mAPKConfig4;
                                                mGameConfig = mGameConfig4;
                                                blackListConfig = blackListConfig4;
                                                whiteListConfig = whiteListConfig4;
                                                ouiBlackListConfig = ouiBlackListConfig4;
                                                mQCIConfig = mQCIConfig4;
                                                inputStream3 = inputStream6;
                                                HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 1:%{public}s", e.getMessage());
                                                responseForParaUpdate(0);
                                                if (inputStream2 != null) {
                                                }
                                                synchronized (this.mLock) {
                                                }
                                            } catch (IOException e29) {
                                                e2 = e29;
                                                mAPKConfig3 = mAPKConfig4;
                                                mGameConfig3 = mGameConfig4;
                                                blackListConfig3 = blackListConfig4;
                                                whiteListConfig3 = whiteListConfig4;
                                                ouiBlackListConfig3 = ouiBlackListConfig4;
                                                mQCIConfig3 = mQCIConfig4;
                                                inputStream4 = inputStream6;
                                                HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 2:%{public}s", e2.getMessage());
                                                responseForParaUpdate(6);
                                                if (inputStream2 != null) {
                                                }
                                                synchronized (this.mLock) {
                                                }
                                            } catch (XmlPullParserException e30) {
                                                e3 = e30;
                                                mAPKConfig2 = mAPKConfig4;
                                                mGameConfig2 = mGameConfig4;
                                                blackListConfig2 = blackListConfig4;
                                                whiteListConfig2 = whiteListConfig4;
                                                ouiBlackListConfig2 = ouiBlackListConfig4;
                                                mQCIConfig2 = mQCIConfig4;
                                                inputStream2 = inputStream6;
                                                HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 3:%{public}s", e3.getMessage());
                                                responseForParaUpdate(6);
                                                if (inputStream2 != null) {
                                                }
                                                synchronized (this.mLock) {
                                                }
                                            } catch (Throwable th12) {
                                                th = th12;
                                                inputStream = inputStream6;
                                                if (inputStream != null) {
                                                }
                                                throw th;
                                            }
                                        }
                                    }
                                } catch (FileNotFoundException e31) {
                                    inputStream3 = inputStream5;
                                    e = e31;
                                    mAPKConfig = mAPKConfig4;
                                    mGameConfig = mGameConfig4;
                                    blackListConfig = blackListConfig4;
                                    whiteListConfig = whiteListConfig4;
                                    ouiBlackListConfig = ouiBlackListConfig4;
                                    mQCIConfig = mQCIConfig4;
                                    HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 1:%{public}s", e.getMessage());
                                    responseForParaUpdate(0);
                                    if (inputStream2 != null) {
                                    }
                                    synchronized (this.mLock) {
                                    }
                                } catch (IOException e32) {
                                    inputStream4 = inputStream5;
                                    e2 = e32;
                                    mAPKConfig3 = mAPKConfig4;
                                    mGameConfig3 = mGameConfig4;
                                    blackListConfig3 = blackListConfig4;
                                    whiteListConfig3 = whiteListConfig4;
                                    ouiBlackListConfig3 = ouiBlackListConfig4;
                                    mQCIConfig3 = mQCIConfig4;
                                    HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 2:%{public}s", e2.getMessage());
                                    responseForParaUpdate(6);
                                    if (inputStream2 != null) {
                                    }
                                    synchronized (this.mLock) {
                                    }
                                } catch (XmlPullParserException e33) {
                                    inputStream2 = inputStream5;
                                    e3 = e33;
                                    mAPKConfig2 = mAPKConfig4;
                                    mGameConfig2 = mGameConfig4;
                                    blackListConfig2 = blackListConfig4;
                                    whiteListConfig2 = whiteListConfig4;
                                    ouiBlackListConfig2 = ouiBlackListConfig4;
                                    mQCIConfig2 = mQCIConfig4;
                                    HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 3:%{public}s", e3.getMessage());
                                    responseForParaUpdate(6);
                                    if (inputStream2 != null) {
                                    }
                                    synchronized (this.mLock) {
                                    }
                                } catch (Throwable th13) {
                                    inputStream = inputStream5;
                                    th = th13;
                                    if (inputStream != null) {
                                    }
                                    throw th;
                                }
                            }
                        } catch (FileNotFoundException e34) {
                            inputStream3 = inputStream5;
                            e = e34;
                            mAPKConfig = mAPKConfig4;
                            mGameConfig = mGameConfig4;
                            blackListConfig = blackListConfig4;
                            whiteListConfig = whiteListConfig4;
                            ouiBlackListConfig = ouiBlackListConfig4;
                            mQCIConfig = mQCIConfig4;
                            HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 1:%{public}s", e.getMessage());
                            responseForParaUpdate(0);
                            if (inputStream2 != null) {
                                try {
                                    inputStream2.close();
                                } catch (Exception e35) {
                                }
                            }
                            synchronized (this.mLock) {
                                this.isXmlLoadFinsh = true;
                            }
                            return;
                        } catch (IOException e36) {
                            inputStream4 = inputStream5;
                            e2 = e36;
                            mAPKConfig3 = mAPKConfig4;
                            mGameConfig3 = mGameConfig4;
                            blackListConfig3 = blackListConfig4;
                            whiteListConfig3 = whiteListConfig4;
                            ouiBlackListConfig3 = ouiBlackListConfig4;
                            mQCIConfig3 = mQCIConfig4;
                            HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 2:%{public}s", e2.getMessage());
                            responseForParaUpdate(6);
                            if (inputStream2 != null) {
                                try {
                                    inputStream2.close();
                                } catch (Exception e37) {
                                }
                            }
                            synchronized (this.mLock) {
                            }
                        } catch (XmlPullParserException e38) {
                            inputStream2 = inputStream5;
                            e3 = e38;
                            mAPKConfig2 = mAPKConfig4;
                            mGameConfig2 = mGameConfig4;
                            blackListConfig2 = blackListConfig4;
                            whiteListConfig2 = whiteListConfig4;
                            ouiBlackListConfig2 = ouiBlackListConfig4;
                            mQCIConfig2 = mQCIConfig4;
                            HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 3:%{public}s", e3.getMessage());
                            responseForParaUpdate(6);
                            if (inputStream2 != null) {
                                try {
                                    inputStream2.close();
                                } catch (Exception e39) {
                                }
                            }
                            synchronized (this.mLock) {
                            }
                        } catch (Throwable th14) {
                            inputStream = inputStream5;
                            th = th14;
                            if (inputStream != null) {
                                try {
                                    inputStream.close();
                                } catch (Exception e40) {
                                    HwAPPQoEUtils.logE(TAG, false, "readAppConfigList failed by Exception", new Object[0]);
                                }
                            }
                            throw th;
                        }
                    }
                    upGratedBlackListConfigList2 = upGratedBlackListConfigList;
                    configPath2 = configPath;
                    i = 1;
                    mUpGratedGameConfigList2 = mUpGratedGameConfigList;
                    configFilePath2 = configFilePath;
                    eventType = xmlPullParser.next();
                    inputStream5 = inputStream6;
                    inputStream6 = arrayList;
                }
                InputStream inputStream7 = inputStream5;
                try {
                    synchronized (this.mLock) {
                        try {
                            this.mAPKConfigList.clear();
                            this.mAPKConfigList.addAll(mUpGratedAPKConfigList);
                            mUpGratedAPKConfigList.clear();
                            this.mGameConfigList.clear();
                            this.mGameConfigList.addAll(mUpGratedGameConfigList2);
                            mUpGratedGameConfigList2.clear();
                            this.mBlackListConfigList.clear();
                            this.mBlackListConfigList.addAll(upGratedBlackListConfigList2);
                            upGratedBlackListConfigList2.clear();
                            this.mWhiteListConfigList.clear();
                            this.mWhiteListConfigList.addAll(inputStream6);
                            inputStream6.clear();
                            this.mOuiBlackListConfigList.clear();
                            try {
                                this.mOuiBlackListConfigList.addAll(upGratedOuiBlackListConfigList);
                                upGratedOuiBlackListConfigList.clear();
                            } catch (Throwable th15) {
                                th2 = th15;
                                throw th2;
                            }
                        } catch (Throwable th16) {
                            th2 = th16;
                            throw th2;
                        }
                    }
                } catch (FileNotFoundException e41) {
                    e = e41;
                    mAPKConfig = mAPKConfig4;
                    mGameConfig = mGameConfig4;
                    blackListConfig = blackListConfig4;
                    whiteListConfig = whiteListConfig4;
                    ouiBlackListConfig = ouiBlackListConfig4;
                    mQCIConfig = mQCIConfig4;
                    inputStream3 = inputStream7;
                    HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 1:%{public}s", e.getMessage());
                    responseForParaUpdate(0);
                    if (inputStream2 != null) {
                    }
                    synchronized (this.mLock) {
                    }
                } catch (IOException e42) {
                    e2 = e42;
                    mAPKConfig3 = mAPKConfig4;
                    mGameConfig3 = mGameConfig4;
                    blackListConfig3 = blackListConfig4;
                    whiteListConfig3 = whiteListConfig4;
                    ouiBlackListConfig3 = ouiBlackListConfig4;
                    mQCIConfig3 = mQCIConfig4;
                    inputStream4 = inputStream7;
                    HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 2:%{public}s", e2.getMessage());
                    responseForParaUpdate(6);
                    if (inputStream2 != null) {
                    }
                    synchronized (this.mLock) {
                    }
                } catch (XmlPullParserException e43) {
                    e3 = e43;
                    mAPKConfig2 = mAPKConfig4;
                    mGameConfig2 = mGameConfig4;
                    blackListConfig2 = blackListConfig4;
                    whiteListConfig2 = whiteListConfig4;
                    ouiBlackListConfig2 = ouiBlackListConfig4;
                    mQCIConfig2 = mQCIConfig4;
                    inputStream2 = inputStream7;
                    HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 3:%{public}s", e3.getMessage());
                    responseForParaUpdate(6);
                    if (inputStream2 != null) {
                    }
                    synchronized (this.mLock) {
                    }
                } catch (Throwable th17) {
                    th = th17;
                    inputStream = inputStream7;
                    if (inputStream != null) {
                    }
                    throw th;
                }
            } catch (FileNotFoundException e44) {
                inputStream3 = inputStream5;
                e = e44;
                HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 1:%{public}s", e.getMessage());
                responseForParaUpdate(0);
                if (inputStream2 != null) {
                }
                synchronized (this.mLock) {
                }
            } catch (IOException e45) {
                inputStream4 = inputStream5;
                e2 = e45;
                HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 2:%{public}s", e2.getMessage());
                responseForParaUpdate(6);
                if (inputStream2 != null) {
                }
                synchronized (this.mLock) {
                }
            } catch (XmlPullParserException e46) {
                inputStream2 = inputStream5;
                e3 = e46;
                HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 3:%{public}s", e3.getMessage());
                responseForParaUpdate(6);
                if (inputStream2 != null) {
                }
                synchronized (this.mLock) {
                }
            } catch (Throwable th18) {
                inputStream = inputStream5;
                th = th18;
                if (inputStream != null) {
                }
                throw th;
            }
        } catch (FileNotFoundException e47) {
            inputStream3 = null;
            e = e47;
            HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 1:%{public}s", e.getMessage());
            responseForParaUpdate(0);
            if (inputStream2 != null) {
            }
            synchronized (this.mLock) {
            }
        } catch (IOException e48) {
            inputStream4 = null;
            e2 = e48;
            HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 2:%{public}s", e2.getMessage());
            responseForParaUpdate(6);
            if (inputStream2 != null) {
            }
            synchronized (this.mLock) {
            }
        } catch (XmlPullParserException e49) {
            inputStream2 = null;
            e3 = e49;
            HwAPPQoEUtils.logD(TAG, false, "readAppConfigList exception 3:%{public}s", e3.getMessage());
            responseForParaUpdate(6);
            if (inputStream2 != null) {
            }
            synchronized (this.mLock) {
            }
        } catch (Throwable th19) {
            inputStream = null;
            th = th19;
            if (inputStream != null) {
            }
            throw th;
        }
        synchronized (this.mLock) {
        }
        HwAPPQoEUtils.logE(TAG, false, "readAppConfigList failed by Exception", new Object[0]);
        synchronized (this.mLock) {
        }
    }

    /* JADX INFO: Can't fix incorrect switch cases order, some code will duplicate */
    public void fillAPKConfig(HwAPPQoEAPKConfig config, String elementName, String elementValue) {
        if (elementName == null || config == null) {
            HwAPPQoEUtils.logD(TAG, false, "fillAPKConfig, input error", new Object[0]);
            return;
        }
        char c = 65535;
        try {
            switch (elementName.hashCode()) {
                case -2137635847:
                    if (elementName.equals("mHistoryQoeBadTH")) {
                        c = '\n';
                        break;
                    }
                    break;
                case -1814569853:
                    if (elementName.equals("mAggressiveStallTH")) {
                        c = '\f';
                        break;
                    }
                    break;
                case -1110914385:
                    if (elementName.equals("mAppId")) {
                        c = 1;
                        break;
                    }
                    break;
                case -923620903:
                    if (elementName.equals("mGeneralStallTH")) {
                        c = 11;
                        break;
                    }
                    break;
                case -836023979:
                    if (elementName.equals("mAppPeriod")) {
                        c = 5;
                        break;
                    }
                    break;
                case -779093368:
                    if (elementName.equals(CFG_APP_REGION)) {
                        c = '\r';
                        break;
                    }
                    break;
                case -154939613:
                    if (elementName.equals("mScenceId")) {
                        c = 3;
                        break;
                    }
                    break;
                case -154462957:
                    if (elementName.equals("mScenseId")) {
                        c = 2;
                        break;
                    }
                    break;
                case -90462973:
                    if (elementName.equals("mAction")) {
                        c = '\t';
                        break;
                    }
                    break;
                case -9888733:
                    if (elementName.equals("className")) {
                        c = 0;
                        break;
                    }
                    break;
                case 3328234:
                    if (elementName.equals("mQci")) {
                        c = 7;
                        break;
                    }
                    break;
                case 183978075:
                    if (elementName.equals("mAppAlgorithm")) {
                        c = 6;
                        break;
                    }
                    break;
                case 186353520:
                    if (elementName.equals("mPlayActivity")) {
                        c = 14;
                        break;
                    }
                    break;
                case 270217909:
                    if (elementName.equals("mReserved")) {
                        c = 15;
                        break;
                    }
                    break;
                case 292004483:
                    if (elementName.equals("monitorUserLearning")) {
                        c = '\b';
                        break;
                    }
                    break;
                case 1427238722:
                    if (elementName.equals("mScenceType")) {
                        c = 4;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
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
                case '\b':
                    config.monitorUserLearning = Integer.parseInt(elementValue);
                    break;
                case '\t':
                    config.mAction = Integer.parseInt(elementValue);
                    break;
                case '\n':
                    config.mHistoryQoeBadTH = Float.parseFloat(elementValue);
                    break;
                case 11:
                    config.mGeneralStallTH = Integer.parseInt(elementValue);
                    break;
                case '\f':
                    config.mAggressiveStallTH = Integer.parseInt(elementValue);
                    break;
                case '\r':
                    config.setAppRegion(Integer.parseInt(elementValue));
                    break;
                case 14:
                    config.setPlayActivity(Integer.parseInt(elementValue));
                    break;
                case 15:
                    config.mReserved = elementValue;
                    break;
                default:
                    HwAPPQoEUtils.logD(TAG, false, "fillAPKConfig, invalid element name:%{public}s", elementName);
                    break;
            }
        } catch (NumberFormatException e) {
            HwAPPQoEUtils.logE(TAG, false, "fillAPKConfig NumberFormatException name: %{public}s", elementName);
        }
        fillAppConfigByWifiPro(config, elementName, elementValue);
    }

    /* JADX INFO: Can't fix incorrect switch cases order, some code will duplicate */
    private void fillAppConfigByWifiPro(HwAPPQoEAPKConfig config, String elementName, String elementValue) {
        if (elementName == null || config == null) {
            HwAPPQoEUtils.logD(TAG, false, "fillAPKConfig, input error", new Object[0]);
            return;
        }
        char c = 65535;
        try {
            switch (elementName.hashCode()) {
                case -1794263281:
                    if (elementName.equals("mTcpResendRate")) {
                        c = 7;
                        break;
                    }
                    break;
                case -1733421470:
                    if (elementName.equals("mBadContinuousCnt")) {
                        c = 6;
                        break;
                    }
                    break;
                case -1642512042:
                    if (elementName.equals("mDetectCycle")) {
                        c = '\b';
                        break;
                    }
                    break;
                case -1560918121:
                    if (elementName.equals("mBadCount")) {
                        c = 4;
                        break;
                    }
                    break;
                case -947980069:
                    if (elementName.equals("mSwitchType")) {
                        c = 1;
                        break;
                    }
                    break;
                case -885057435:
                    if (elementName.equals("mGoodCount")) {
                        c = 5;
                        break;
                    }
                    break;
                case 3329733:
                    if (elementName.equals("mRtt")) {
                        c = 2;
                        break;
                    }
                    break;
                case 270217909:
                    if (elementName.equals("mReserved")) {
                        c = '\t';
                        break;
                    }
                    break;
                case 513355390:
                    if (elementName.equals("mThreshold")) {
                        c = 3;
                        break;
                    }
                    break;
                case 1803858441:
                    if (elementName.equals("mWlanPlus")) {
                        c = 0;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    config.setWlanPlus(Integer.parseInt(elementValue));
                    break;
                case 1:
                    config.setSwitchType(Integer.parseInt(elementValue));
                    break;
                case 2:
                    config.setRtt(Float.parseFloat(elementValue));
                    break;
                case 3:
                    config.setThreshlod(Integer.parseInt(elementValue));
                    break;
                case 4:
                    config.setBadCount(Integer.parseInt(elementValue));
                    break;
                case 5:
                    config.setGoodCount(Integer.parseInt(elementValue));
                    break;
                case 6:
                    config.setBadContinuousCnt(Integer.parseInt(elementValue));
                    break;
                case 7:
                    config.setTcpResendRate(Float.parseFloat(elementValue));
                    break;
                case '\b':
                    config.setDetectCycle(Integer.parseInt(elementValue));
                    break;
                case '\t':
                    config.mReserved = elementValue;
                    break;
                default:
                    HwAPPQoEUtils.logD(TAG, false, "fillAPKConfig, invalid element name:%{public}s", elementName);
                    break;
            }
        } catch (NumberFormatException e) {
            HwAPPQoEUtils.logE(TAG, false, "fillAPKConfig NumberFormatException name: %{public}s", elementName);
        }
        fillAppConfigByWifiProForOta(config, elementName, elementValue);
    }

    /* JADX INFO: Can't fix incorrect switch cases order, some code will duplicate */
    private void fillAppConfigByWifiProForOta(HwAPPQoEAPKConfig config, String elementName, String elementValue) {
        if (elementName == null || config == null) {
            HwAPPQoEUtils.logD(TAG, false, "fillAPKConfig, input error", new Object[0]);
            return;
        }
        char c = 65535;
        try {
            switch (elementName.hashCode()) {
                case -2106982590:
                    if (elementName.equals("mTxGoodTh")) {
                        c = 7;
                        break;
                    }
                    break;
                case -1223386122:
                    if (elementName.equals("mNoise2gTh")) {
                        c = 0;
                        break;
                    }
                    break;
                case -1223296749:
                    if (elementName.equals("mNoise5gTh")) {
                        c = 1;
                        break;
                    }
                    break;
                case 36855169:
                    if (elementName.equals("mChLoad2gTh")) {
                        c = 4;
                        break;
                    }
                    break;
                case 36944542:
                    if (elementName.equals("mChLoad5gTh")) {
                        c = 5;
                        break;
                    }
                    break;
                case 270217909:
                    if (elementName.equals("mReserved")) {
                        c = '\n';
                        break;
                    }
                    break;
                case 1285856841:
                    if (elementName.equals("mLinkSpeed2gTh")) {
                        c = 2;
                        break;
                    }
                    break;
                case 1285946214:
                    if (elementName.equals("mLinkSpeed5gTh")) {
                        c = 3;
                        break;
                    }
                    break;
                case 1728102408:
                    if (elementName.equals("mTxBadTh")) {
                        c = '\b';
                        break;
                    }
                    break;
                case 1877454002:
                    if (elementName.equals("mTcpRttTh")) {
                        c = '\t';
                        break;
                    }
                    break;
                case 1931657155:
                    if (elementName.equals("mOtaRateTh")) {
                        c = 6;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    config.setNoise2gTh(Integer.parseInt(elementValue));
                    return;
                case 1:
                    config.setNoise5gTh(Integer.parseInt(elementValue));
                    return;
                case 2:
                    config.setLinkSpeed2gTh(Integer.parseInt(elementValue));
                    return;
                case 3:
                    config.setLinkSpeed5gTh(Integer.parseInt(elementValue));
                    return;
                case 4:
                    config.setChLoad2gTh(Integer.parseInt(elementValue));
                    return;
                case 5:
                    config.setChLoad5gTh(Integer.parseInt(elementValue));
                    return;
                case 6:
                    config.setOtaRateTh(Float.parseFloat(elementValue));
                    return;
                case 7:
                    config.setTxGoodTh(Float.parseFloat(elementValue));
                    return;
                case '\b':
                    config.setTxBadTh(Float.parseFloat(elementValue));
                    return;
                case '\t':
                    config.setTcpRttTh(Float.parseFloat(elementValue));
                    return;
                case '\n':
                    config.mReserved = elementValue;
                    return;
                default:
                    HwAPPQoEUtils.logD(TAG, false, "fillAPKConfig, invalid element name:%{public}s", elementName);
                    return;
            }
        } catch (NumberFormatException e) {
            HwAPPQoEUtils.logE(TAG, false, "fillAPKConfig NumberFormatException name: %{public}s", elementName);
        }
    }

    /* JADX INFO: Can't fix incorrect switch cases order, some code will duplicate */
    public void fillGameConfig(HwAPPQoEGameConfig config, String elementName, String elementValue) {
        if (elementName == null || config == null) {
            HwAPPQoEUtils.logD(TAG, false, "fillGameConfig, input error", new Object[0]);
            return;
        }
        char c = 65535;
        try {
            switch (elementName.hashCode()) {
                case -2137635847:
                    if (elementName.equals("mHistoryQoeBadTH")) {
                        c = 5;
                        break;
                    }
                    break;
                case -1838174908:
                    if (elementName.equals("mGameKQI")) {
                        c = 2;
                        break;
                    }
                    break;
                case -1838167053:
                    if (elementName.equals("mGameRtt")) {
                        c = 3;
                        break;
                    }
                    break;
                case -503942891:
                    if (elementName.equals("mGameAction")) {
                        c = 4;
                        break;
                    }
                    break;
                case -331393776:
                    if (elementName.equals(CFG_GAME_SPECIALINFO_SOURCES)) {
                        c = 6;
                        break;
                    }
                    break;
                case -154939613:
                    if (elementName.equals("mScenceId")) {
                        c = 1;
                        break;
                    }
                    break;
                case 79251322:
                    if (elementName.equals("mGameId")) {
                        c = 0;
                        break;
                    }
                    break;
                case 270217909:
                    if (elementName.equals("mReserved")) {
                        c = 7;
                        break;
                    }
                    break;
            }
            switch (c) {
                case 0:
                    config.mGameId = Integer.parseInt(elementValue);
                    return;
                case 1:
                    config.mScenceId = Integer.parseInt(elementValue);
                    return;
                case 2:
                    config.mGameKQI = Integer.parseInt(elementValue);
                    return;
                case 3:
                    config.mGameRtt = Integer.parseInt(elementValue);
                    return;
                case 4:
                    config.mGameAction = Integer.parseInt(elementValue);
                    return;
                case 5:
                    config.mHistoryQoeBadTH = Float.parseFloat(elementValue);
                    return;
                case 6:
                    config.setGameSpecialInfoSources(Integer.parseInt(elementValue));
                    return;
                case 7:
                    config.mReserved = elementValue;
                    return;
                default:
                    HwAPPQoEUtils.logD(TAG, false, "fillGameConfig, invalid element name: %{public}s", elementName);
                    return;
            }
        } catch (NumberFormatException e) {
            HwAPPQoEUtils.logE(TAG, false, "fillGameConfig NumberFormatException name: %{public}s", elementName);
        }
    }

    private void fillBlackListConfig(HwAppQoeBlackListConfig config, String elementName, String elementValue) {
        if (elementName == null || config == null) {
            HwAPPQoEUtils.logD(TAG, false, "fillBlackListConfig, input error", new Object[0]);
        } else {
            HwAPPQoEUtils.logD(TAG, false, "fillBlackListConfig, no feature id now", new Object[0]);
        }
    }

    private void fillWhiteListConfig(HwAppQoeWhiteListConfig config, String elementName, String elementValue) {
        if (elementName == null || config == null) {
            HwAPPQoEUtils.logD(TAG, false, "fillWhiteListConfig, input error", new Object[0]);
        } else {
            HwAPPQoEUtils.logD(TAG, false, "fillWhiteListConfig, no feature id now", new Object[0]);
        }
    }

    private void fillOuiBlackListConfig(HwAppQoeOuiBlackListConfig config, String elementName, String elementValue) {
        if (elementName == null || config == null) {
            HwAPPQoEUtils.logD(TAG, false, "fillOuiBlackListConfig, input error", new Object[0]);
            return;
        }
        char c = 65535;
        try {
            if (elementName.hashCode() == 1638328516 && elementName.equals("mFeatureId")) {
                c = 0;
            }
            if (c != 0) {
                HwAPPQoEUtils.logD(TAG, false, "fillOuiBlackListConfig, invalid element name: %{public}s", elementName);
                return;
            }
            config.setFeatureId(Integer.parseInt(elementValue));
        } catch (NumberFormatException e) {
            HwAPPQoEUtils.logE(TAG, false, "fillOuiBlackListConfig NumberFormatException name: %{public}s", elementName);
        }
    }

    /* JADX INFO: Can't fix incorrect switch cases order, some code will duplicate */
    public void fillQCIConfig(HwCHQciConfig config, String elementName, String elementValue) {
        if (elementName == null || config == null) {
            HwAPPQoEUtils.logD(TAG, false, "fillQCIConfig, input error", new Object[0]);
            return;
        }
        char c = 65535;
        try {
            switch (elementName.hashCode()) {
                case 79991:
                    if (elementName.equals("QCI")) {
                        c = 0;
                        break;
                    }
                    break;
                case 81490:
                    if (elementName.equals("RTT")) {
                        c = 1;
                        break;
                    }
                    break;
                case 2525271:
                    if (elementName.equals("RSSI")) {
                        c = 2;
                        break;
                    }
                    break;
                case 2582043:
                    if (elementName.equals("TPUT")) {
                        c = 4;
                        break;
                    }
                    break;
                case 1986988747:
                    if (elementName.equals("CHLOAD")) {
                        c = 3;
                        break;
                    }
                    break;
            }
            if (c == 0) {
                config.mQci = Integer.parseInt(elementValue);
            } else if (c == 1) {
                config.mRtt = Integer.parseInt(elementValue);
            } else if (c == 2) {
                config.mRssi = Integer.parseInt(elementValue);
            } else if (c == 3) {
                config.mChload = Integer.parseInt(elementValue);
            } else if (c != 4) {
                HwAPPQoEUtils.logD(TAG, false, "fillQCIConfig, invalid element name: %{public}s", elementName);
            } else {
                config.mTput = Float.parseFloat(elementValue);
            }
        } catch (NumberFormatException e) {
            HwAPPQoEUtils.logE(TAG, false, "fillQciConfig NumberFormatException name: %{public}s", elementName);
        }
    }

    public void fillMpLinkConfig(HwMpLinkConfigInfo config, String elementName, String elementValue) {
        if (elementName == null || config == null) {
            HwAPPQoEUtils.logD(TAG, false, "fillGameConfig, input error", new Object[0]);
            return;
        }
        char c = 65535;
        switch (elementName.hashCode()) {
            case -861311717:
                if (elementName.equals("condition")) {
                    c = 6;
                    break;
                }
                break;
            case -793183188:
                if (elementName.equals(MemoryConstant.MEM_POLICY_BIGAPPNAME)) {
                    c = 1;
                    break;
                }
                break;
            case -434367618:
                if (elementName.equals("gatewaytype")) {
                    c = 3;
                    break;
                }
                break;
            case -350385368:
                if (elementName.equals("reserved")) {
                    c = 5;
                    break;
                }
                break;
            case 353371935:
                if (elementName.equals("encrypttype")) {
                    c = 4;
                    break;
                }
                break;
            case 398021374:
                if (elementName.equals("multnetwork")) {
                    c = 2;
                    break;
                }
                break;
            case 1127930396:
                if (elementName.equals("custmac")) {
                    c = 0;
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
                config.setmCustMac(elementValue);
                return;
            case 1:
                config.setmAppName(elementValue);
                return;
            case 2:
                config.setmMultiNetwork(elementValue);
                return;
            case 3:
                config.setmGatewayType(elementValue);
                return;
            case 4:
                config.setmEncryptType(elementValue);
                return;
            case 5:
                config.setmReserved(elementValue);
                return;
            case 6:
                config.setCondition(elementValue);
                return;
            default:
                HwAPPQoEUtils.logD(TAG, false, "fillMpLinkConfig, invalid element name: %{public}s", elementName);
                return;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:27:0x0059, code lost:
        if (r0 == null) goto L_0x006a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x005b, code lost:
        com.android.server.hidata.appqoe.HwAPPQoEUtils.logD(com.android.server.hidata.appqoe.HwAPPQoEResourceMangerImpl.TAG, false, "checkIsMonitorAPKScence end:%{public}s", r0.toString());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x006a, code lost:
        return r0;
     */
    public HwAPPQoEAPKConfig checkIsMonitorAPKScence(String packageName, String className) {
        HwAPPQoEAPKConfig config = null;
        synchronized (this.mLock) {
            if (this.isXmlLoadFinsh && this.mAPKConfigList.size() != 0) {
                if (packageName != null) {
                    HwAPPQoEUtils.logD(TAG, false, "checkIsMonitorAPKScence input :%{public}s,%{public}s", packageName, className);
                    Iterator<HwAPPQoEAPKConfig> it = this.mAPKConfigList.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        HwAPPQoEAPKConfig apkConfig = it.next();
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
            return null;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:21:0x004b, code lost:
        if (r0 == null) goto L_0x005c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x004d, code lost:
        com.android.server.hidata.appqoe.HwAPPQoEUtils.logD(com.android.server.hidata.appqoe.HwAPPQoEResourceMangerImpl.TAG, false, "checkIsMonitorVideoScence end:%{public}s", r0.toString());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x005c, code lost:
        return r0;
     */
    public HwAPPQoEAPKConfig checkIsMonitorVideoScence(String packageName, String className) {
        HwAPPQoEAPKConfig config = null;
        synchronized (this.mLock) {
            if (this.isXmlLoadFinsh && this.mAPKConfigList.size() != 0) {
                if (packageName != null) {
                    HwAPPQoEUtils.logD(TAG, false, "checkIsMonitorVideoScence input :%{public}s,%{public}s", packageName, className);
                    Iterator<HwAPPQoEAPKConfig> it = this.mAPKConfigList.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        HwAPPQoEAPKConfig apkConfig = it.next();
                        if (className != null && className.contains(apkConfig.className) && packageName.contains(apkConfig.packageName)) {
                            config = apkConfig;
                            break;
                        }
                    }
                }
            }
            return null;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:17:0x003e, code lost:
        if (r0 == null) goto L_0x004f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0040, code lost:
        com.android.server.hidata.appqoe.HwAPPQoEUtils.logD(com.android.server.hidata.appqoe.HwAPPQoEResourceMangerImpl.TAG, false, "checkIsMonitorGameScence end:%{public}s", r0.toString());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x004f, code lost:
        return r0;
     */
    public HwAPPQoEGameConfig checkIsMonitorGameScence(String packageName) {
        HwAPPQoEGameConfig config = null;
        synchronized (this.mLock) {
            if (this.isXmlLoadFinsh && this.mGameConfigList.size() != 0) {
                if (packageName != null) {
                    HwAPPQoEUtils.logD(TAG, false, "checkIsMonitorGameScence input :%{public}s", packageName);
                    Iterator<HwAPPQoEGameConfig> it = this.mGameConfigList.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        HwAPPQoEGameConfig gameConfig = it.next();
                        if (packageName.contains(gameConfig.mGameName)) {
                            config = gameConfig;
                            break;
                        }
                    }
                }
            }
            return null;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0042, code lost:
        if (r0 == null) goto L_0x0053;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0044, code lost:
        com.android.server.hidata.appqoe.HwAPPQoEUtils.logD(com.android.server.hidata.appqoe.HwAPPQoEResourceMangerImpl.TAG, false, "checkIsMonitorBlackListScence end:%{public}s", r0.toString());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x0053, code lost:
        return r1;
     */
    public boolean isInBlackListScene(String packageName) {
        HwAppQoeBlackListConfig config = null;
        boolean isInBlackList = false;
        synchronized (this.mLock) {
            if (this.isXmlLoadFinsh && !this.mBlackListConfigList.isEmpty()) {
                if (packageName != null) {
                    HwAPPQoEUtils.logD(TAG, false, "checkIsMonitorBlackListScence input :%{public}s", packageName);
                    Iterator<HwAppQoeBlackListConfig> it = this.mBlackListConfigList.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        HwAppQoeBlackListConfig blackListConfig = it.next();
                        if (packageName.contains(blackListConfig.getPackageName())) {
                            isInBlackList = true;
                            config = blackListConfig;
                            break;
                        }
                    }
                }
            }
            return false;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0042, code lost:
        if (r0 == null) goto L_0x0053;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0044, code lost:
        com.android.server.hidata.appqoe.HwAPPQoEUtils.logD(com.android.server.hidata.appqoe.HwAPPQoEResourceMangerImpl.TAG, false, "checkIsMonitorWhiteListScence end:%{public}s", r0.toString());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x0053, code lost:
        return r1;
     */
    public boolean isInWhiteListScene(String packageName) {
        HwAppQoeWhiteListConfig config = null;
        boolean isInWhiteList = false;
        synchronized (this.mLock) {
            if (this.isXmlLoadFinsh && !this.mWhiteListConfigList.isEmpty()) {
                if (packageName != null) {
                    HwAPPQoEUtils.logD(TAG, false, "checkIsMonitorWhiteListScence input :%{public}s", packageName);
                    Iterator<HwAppQoeWhiteListConfig> it = this.mWhiteListConfigList.iterator();
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        HwAppQoeWhiteListConfig whiteListConfig = it.next();
                        if (packageName.contains(whiteListConfig.getPackageName())) {
                            isInWhiteList = true;
                            config = whiteListConfig;
                            break;
                        }
                    }
                }
            }
            return false;
        }
    }

    private boolean isInOuiBlackList(ScanResult.InformationElement ie, String[] ouiBlackListString) {
        if (ie.bytes == null || ouiBlackListString == null || ie.bytes.length <= 0) {
            return false;
        }
        int index = 0;
        while (index < 3) {
            try {
                if ((ie.bytes[index] & 255) != Integer.parseInt(ouiBlackListString[index], 16)) {
                    return false;
                }
                index++;
            } catch (NumberFormatException e) {
                HwAPPQoEUtils.logE(TAG, false, "parse ouiblacklist string fail", new Object[0]);
                return true;
            }
        }
        return true;
    }

    private boolean isInFeatureIdBlackList(ScanResult.InformationElement ie, int featureId) {
        if (ie.bytes == null || featureId < 0 || ie.bytes.length < 4 || (ie.bytes[3] & 255) != featureId) {
            return false;
        }
        return true;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:27:0x006e, code lost:
        return false;
     */
    public boolean isInRouterBlackList(ScanResult.InformationElement ie) {
        boolean isInOuiBlackList = false;
        boolean isBlackListFeatureId = false;
        synchronized (this.mLock) {
            if (this.isXmlLoadFinsh && !this.mOuiBlackListConfigList.isEmpty()) {
                if (ie.bytes != null) {
                    HwAPPQoEUtils.logD(TAG, false, "checkIsInOuiBlackList", new Object[0]);
                    Iterator<HwAppQoeOuiBlackListConfig> it = this.mOuiBlackListConfigList.iterator();
                    while (true) {
                        if (it.hasNext()) {
                            HwAppQoeOuiBlackListConfig ouiBlackListConfig = it.next();
                            if (ouiBlackListConfig != null) {
                                if (ouiBlackListConfig.getOuiName() != null) {
                                    String[] tempStringArray = ouiBlackListConfig.getOuiName().split(AwarenessInnerConstants.COLON_KEY);
                                    if (tempStringArray.length == 3) {
                                        isInOuiBlackList = isInOuiBlackList(ie, tempStringArray);
                                    }
                                    isBlackListFeatureId = isInFeatureIdBlackList(ie, ouiBlackListConfig.getFeatureId());
                                    if (isInOuiBlackList && isBlackListFeatureId) {
                                        HwAPPQoEUtils.logD(TAG, false, "checkOuiBlackList is :%{public}s", ouiBlackListConfig.getOuiName());
                                        break;
                                    }
                                } else {
                                    break;
                                }
                            } else {
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    return isInOuiBlackList & isBlackListFeatureId;
                }
            }
            return false;
        }
    }

    public HwAPPQoEGameConfig getGameScenceConfig(int appId) {
        synchronized (this.mLock) {
            if (this.isXmlLoadFinsh) {
                if (this.mGameConfigList.size() != 0) {
                    HwAPPQoEUtils.logD(TAG, false, "getGameScenceConfig input :%{public}d", Integer.valueOf(appId));
                    for (HwAPPQoEGameConfig gameConfig : this.mGameConfigList) {
                        if (appId == gameConfig.mGameId) {
                            HwAPPQoEUtils.logD(TAG, false, "getGameScenceConfig:%{public}s", gameConfig.toString());
                            return gameConfig;
                        }
                    }
                    HwAPPQoEUtils.logD(TAG, false, "getGameScenceConfig, not found", new Object[0]);
                    return null;
                }
            }
            return null;
        }
    }

    public int getScenceAction(int appType, int appId, int scenceId) {
        int scenceAction = -1;
        synchronized (this.mLock) {
            if (!this.isXmlLoadFinsh) {
                return -1;
            }
            if ((1000 == appType || 4000 == appType) && this.mAPKConfigList.size() != 0) {
                Iterator<HwAPPQoEAPKConfig> it = this.mAPKConfigList.iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    HwAPPQoEAPKConfig apkConfig = it.next();
                    if (appId == apkConfig.mAppId && scenceId == apkConfig.mScenceId) {
                        scenceAction = apkConfig.mAction;
                        break;
                    }
                }
            }
            if (2000 == appType && this.mGameConfigList.size() != 0) {
                Iterator<HwAPPQoEGameConfig> it2 = this.mGameConfigList.iterator();
                while (true) {
                    if (!it2.hasNext()) {
                        break;
                    }
                    HwAPPQoEGameConfig gameConfig = it2.next();
                    if (appId == gameConfig.mGameId && scenceId == gameConfig.mScenceId) {
                        scenceAction = gameConfig.mGameAction;
                        break;
                    }
                }
            }
            HwAPPQoEUtils.logD(TAG, false, "getScenceAction, action:%{public}d", Integer.valueOf(scenceAction));
            return scenceAction;
        }
    }

    public HwAPPQoEAPKConfig getAPKScenceConfig(int scenceId) {
        synchronized (this.mLock) {
            if (this.isXmlLoadFinsh) {
                if (this.mAPKConfigList.size() != 0) {
                    HwAPPQoEUtils.logD(TAG, false, "getAPKScenceConfig input :%{public}d", Integer.valueOf(scenceId));
                    for (HwAPPQoEAPKConfig apkConfig : this.mAPKConfigList) {
                        if (scenceId == apkConfig.mScenceId) {
                            HwAPPQoEUtils.logD(TAG, false, "getAPKScenceConfig:%{public}s", apkConfig.toString());
                            return apkConfig;
                        }
                    }
                    HwAPPQoEUtils.logD(TAG, false, "getAPKScenceConfig, not found", new Object[0]);
                    return null;
                }
            }
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
