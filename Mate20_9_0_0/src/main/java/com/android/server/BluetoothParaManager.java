package com.android.server;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings.Global;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.pm.auth.HwCertification;
import com.android.server.wifipro.WifiProCHRManager;
import com.huawei.msdp.devicestatus.BuildConfig;
import huawei.cust.HwCfgFilePolicy;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class BluetoothParaManager {
    private static final int BT_PARA_FILE_MAX_SIZE = 10485760;
    private static final String BT_PARA_FILE_WARNING = "BT_PARA_FILE_WARNING";
    private static final String BT_PARA_UPDATE_ACTION = "com.huawei.android.bluetooth.ACTION_BT_PARA_UPDATE";
    private static final String BT_PARA_UPDATE_PERMISSION = "com.huawei.android.bluetooth.permission.BT_PARA_UPDATE";
    private static final String EMCOM_PARA_READY_ACTION = "huawei.intent.action.ACTION_EMCOM_PARA_READY";
    private static final String EXTRA_EMCOM_PARA_READY_REC = "EXTRA_EMCOM_PARA_READY_REC";
    private static final int MASKBIT_PARATYPE_NONCELL_BT = 4;
    private static final int PARATYPE_NONCELL_BT = 16;
    private static final int PARA_PATHTYPE_COTA = 1;
    private static final int PARA_UPGRADE_FILE_NOTEXIST = 0;
    private static final int PARA_UPGRADE_RESPONSE_FILE_ERROR = 6;
    private static final int PARA_UPGRADE_RESPONSE_UPGRADE_ALREADY = 4;
    private static final int PARA_UPGRADE_RESPONSE_UPGRADE_FAILURE = 9;
    private static final int PARA_UPGRADE_RESPONSE_UPGRADE_PENDING = 7;
    private static final int PARA_UPGRADE_RESPONSE_UPGRADE_SUCCESS = 8;
    private static final int PARA_UPGRADE_RESPONSE_VERSION_MISMATCH = 5;
    private static final String RECEIVE_EMCOM_PARA_UPGRADE_PERMISSION = "huawei.permission.RECEIVE_EMCOM_PARA_UPGRADE";
    private static final String TAG = "BluetoothParaManager";
    private static String mCotaFilePath = "";
    private static final String mCotaParaCfgDir = "emcom/noncell";
    private static String mSysFilePath = "";
    private BluetoothAdapter mAdapter = null;
    private final ContentResolver mContentResolver;
    private Context mContext;
    private int mCotaConfigVersion = 0;
    private File mCotaFile;
    private String mCpSavedEmuiVersion = "";
    private int mSavedConfigVersion = 0;
    private boolean mSendBroadcastToApk = false;
    private int mSysConfigVersion = 0;
    private File mSysFile;
    private String mSystemEmuiVersion = "";
    private int mToBeSavedBtInteropVersion = 0;
    private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                HwLog.w(BluetoothParaManager.TAG, "BT_PARA onReceive: intent is null");
                return;
            }
            String action = intent.getAction();
            if (action == null) {
                HwLog.w(BluetoothParaManager.TAG, "BT_PARA onReceive: get null for intent.getAction()");
                return;
            }
            if (action.equals("huawei.intent.action.ACTION_EMCOM_PARA_READY")) {
                int cotaParaBitRec = intent.getIntExtra("EXTRA_EMCOM_PARA_READY_REC", 0);
                String str = BluetoothParaManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("BT_PARA onReceive: cotaParaBitRec:");
                stringBuilder.append(cotaParaBitRec);
                HwLog.d(str, stringBuilder.toString());
                if ((cotaParaBitRec & 16) == 0) {
                    HwLog.d(BluetoothParaManager.TAG, "BT_PARA onReceive: broadcast is not for bt");
                    return;
                }
                BluetoothParaManager.this.getCoteParaFilePath();
                str = BluetoothParaManager.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("BT_PARA broadcast mSysFilePath:");
                stringBuilder.append(BluetoothParaManager.mSysFilePath);
                HwLog.d(str, stringBuilder.toString());
                str = BluetoothParaManager.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("BT_PARA broadcast mCotaFilePath:");
                stringBuilder.append(BluetoothParaManager.mCotaFilePath);
                HwLog.d(str, stringBuilder.toString());
                BluetoothParaManager.this.mSysFile = new File(BluetoothParaManager.mSysFilePath);
                BluetoothParaManager.this.mCotaFile = new File(BluetoothParaManager.mCotaFilePath);
                new ParseBtInteropXmlThread(true).start();
            }
        }
    };

    class ParseBtInteropXmlThread extends Thread {
        boolean mIsCotaBroadcast;

        ParseBtInteropXmlThread(boolean cotaBroadcast) {
            this.mIsCotaBroadcast = cotaBroadcast;
        }

        /* JADX WARNING: Missing block: B:39:0x01d7, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public synchronized void run() {
            String str = BluetoothParaManager.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("BT_PARA start parse BtInterop config file: is cota ");
            stringBuilder.append(this.mIsCotaBroadcast);
            HwLog.d(str, stringBuilder.toString());
            BluetoothParaManager.this.mSysConfigVersion = BluetoothParaManager.this.getConfigVersion(BluetoothParaManager.this.mSysFile);
            str = BluetoothParaManager.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("BT_PARA mSysConfigVersion:");
            stringBuilder.append(BluetoothParaManager.this.mSysConfigVersion);
            HwLog.i(str, stringBuilder.toString());
            BluetoothParaManager.this.mCotaConfigVersion = BluetoothParaManager.this.getConfigVersion(BluetoothParaManager.this.mCotaFile);
            str = BluetoothParaManager.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("BT_PARA mCotaConfigVersion:");
            stringBuilder.append(BluetoothParaManager.this.mCotaConfigVersion);
            HwLog.i(str, stringBuilder.toString());
            BluetoothParaManager.this.mSavedConfigVersion = BluetoothParaManager.this.getSavedConfigVersion();
            str = BluetoothParaManager.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("BT_PARA mSavedConfigVersion:");
            stringBuilder.append(BluetoothParaManager.this.mSavedConfigVersion);
            HwLog.i(str, stringBuilder.toString());
            if (this.mIsCotaBroadcast) {
                BluetoothParaManager.this.mSendBroadcastToApk = false;
                int cotaFileLength = (int) BluetoothParaManager.this.mCotaFile.length();
                try {
                    StringBuilder stringBuilder2;
                    if (!BluetoothParaManager.this.mCotaFile.exists()) {
                        String str2 = BluetoothParaManager.TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("BT_PARA cotafile does not exist: ");
                        stringBuilder2.append(BluetoothParaManager.this.mCotaFile.exists());
                        HwLog.e(str2, stringBuilder2.toString());
                        BluetoothParaManager.this.responseForParaUpdate(0);
                    } else if (cotaFileLength >= BluetoothParaManager.BT_PARA_FILE_MAX_SIZE) {
                        BluetoothParaManager bluetoothParaManager = BluetoothParaManager.this;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("BT_PARA cotaFileLength is too large: ");
                        stringBuilder2.append(cotaFileLength);
                        bluetoothParaManager.btParaFileError(stringBuilder2.toString());
                        BluetoothParaManager.this.responseForParaUpdate(6);
                    } else if (BluetoothParaManager.this.getVersion(BluetoothParaManager.this.mCotaFile, "EMUI", "emui_version") == null) {
                        BluetoothParaManager.this.btParaFileError("BT_PARA get config file EMUI version failed.");
                        BluetoothParaManager.this.responseForParaUpdate(6);
                    } else if (BluetoothParaManager.this.mCotaConfigVersion > BluetoothParaManager.this.mSavedConfigVersion) {
                        HwLog.i(BluetoothParaManager.TAG, "BT_PARA cotaFile is newer than saved, need to parse");
                        BluetoothParaManager.this.mToBeSavedBtInteropVersion = BluetoothParaManager.this.mCotaConfigVersion;
                        BluetoothParaManager.this.updateBtInteropDataFromFile(BluetoothParaManager.this.mCotaFile, this.mIsCotaBroadcast);
                    } else {
                        HwLog.i(BluetoothParaManager.TAG, "BT_PARA cotaFile is not latest not need to parse ");
                        BluetoothParaManager.this.responseForParaUpdate(4);
                    }
                } catch (SecurityException e) {
                    HwLog.e(BluetoothParaManager.TAG, "BT_PARA mCotaFile exist or not exception");
                    BluetoothParaManager.this.responseForParaUpdate(0);
                }
            } else if (BluetoothParaManager.this.mSysConfigVersion <= BluetoothParaManager.this.mSavedConfigVersion && BluetoothParaManager.this.mCotaConfigVersion <= BluetoothParaManager.this.mSavedConfigVersion) {
                HwLog.i(BluetoothParaManager.TAG, "BT_PARA not need to parse xml");
            } else if (BluetoothParaManager.this.mSysConfigVersion >= BluetoothParaManager.this.mCotaConfigVersion) {
                HwLog.i(BluetoothParaManager.TAG, "BT_PARA need update system file");
                BluetoothParaManager.this.mToBeSavedBtInteropVersion = BluetoothParaManager.this.mSysConfigVersion;
                BluetoothParaManager.this.updateBtInteropDataFromFile(BluetoothParaManager.this.mSysFile, this.mIsCotaBroadcast);
            } else {
                HwLog.i(BluetoothParaManager.TAG, "BT_PARA need update cota file");
                BluetoothParaManager.this.mToBeSavedBtInteropVersion = BluetoothParaManager.this.mCotaConfigVersion;
                BluetoothParaManager.this.updateBtInteropDataFromFile(BluetoothParaManager.this.mCotaFile, this.mIsCotaBroadcast);
            }
        }
    }

    public BluetoothParaManager(Context context) {
        this.mContext = context;
        this.mContentResolver = this.mContext.getContentResolver();
        getCoteParaFilePath();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("BT_PARA mSysFilePath:");
        stringBuilder.append(mSysFilePath);
        HwLog.d(str, stringBuilder.toString());
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("BT_PARA mCotaFilePath:");
        stringBuilder.append(mCotaFilePath);
        HwLog.d(str, stringBuilder.toString());
        this.mSysFile = new File(mSysFilePath);
        this.mCotaFile = new File(mCotaFilePath);
        try {
            this.mSystemEmuiVersion = getProperty("ro.build.version.emui", "").substring(getProperty("ro.build.version.emui", "").lastIndexOf(Constant.RESULT_SEPERATE) + 1);
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("BT_PARA emuiVersionString:");
            stringBuilder.append(this.mSystemEmuiVersion);
            HwLog.i(str, stringBuilder.toString());
        } catch (Exception e) {
            this.mSystemEmuiVersion = "";
            HwLog.e(TAG, "BT_PARA: FATAL ERROR - get system EMUI version failed");
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("huawei.intent.action.ACTION_EMCOM_PARA_READY");
        this.mContext.registerReceiver(this.myReceiver, intentFilter, "huawei.permission.RECEIVE_EMCOM_PARA_UPGRADE", null);
        compareEmuiVersion();
    }

    private void getCoteParaFilePath() {
        HwLog.d(TAG, "BT_PARA getCoteParaFilePath() start");
        try {
            String[] cfgFileInfo = HwCfgFilePolicy.getDownloadCfgFile(mCotaParaCfgDir, "emcom/noncell/bt_interop.xml");
            String str;
            StringBuilder stringBuilder;
            if (cfgFileInfo == null) {
                HwLog.e(TAG, "BT_PARA Both default and cota config files not exist");
            } else if (cfgFileInfo[0].contains("/cota")) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("BT_PARA cota config file path is: ");
                stringBuilder.append(cfgFileInfo[0]);
                HwLog.i(str, stringBuilder.toString());
                mCotaFilePath = cfgFileInfo[0];
            } else {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("BT_PARA system config file path is: ");
                stringBuilder.append(cfgFileInfo[0]);
                HwLog.i(str, stringBuilder.toString());
                mSysFilePath = cfgFileInfo[0];
            }
        } catch (NoClassDefFoundError e) {
            HwLog.e(TAG, "BT_PARA getCoteParaFilePath NoClassDefFoundError exception");
        } catch (Exception e2) {
            HwLog.e(TAG, "BT_PARA getCoteParaFilePath other exception");
        }
    }

    private void compareEmuiVersion() {
        this.mCpSavedEmuiVersion = Global.getString(this.mContentResolver, "hw_bluetooth_interop_emui_version");
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("BT_PARA contentProvider saved Emui is:");
        stringBuilder.append(this.mCpSavedEmuiVersion);
        HwLog.d(str, stringBuilder.toString());
        if (this.mCpSavedEmuiVersion == null) {
            HwLog.i(TAG, "BT_PARA contentProvider saved Emui is null");
            new ParseBtInteropXmlThread(false).start();
        } else if (this.mCpSavedEmuiVersion.equals(this.mSystemEmuiVersion)) {
            HwLog.i(TAG, "BT_PARA contentProvider saved Emui is equals system emui version");
            new ParseBtInteropXmlThread(false).start();
        } else {
            str = getVersion(this.mSysFile, "EMUI", "emui_version");
            String cotaFileEmuiVersion = getVersion(this.mCotaFile, "EMUI", "emui_version");
            if (str == null && cotaFileEmuiVersion == null) {
                HwLog.i(TAG, "BT_PARA two files not exist");
                cleanContentProvider();
                return;
            }
            HwLog.i(TAG, "BT_PARA at least has one file exist");
            cleanContentProvider();
            new ParseBtInteropXmlThread(false).start();
        }
    }

    private void cleanContentProvider() {
        Global.putString(this.mContentResolver, "hw_bluetooth_interop_version", null);
        Global.putString(this.mContentResolver, "hw_bluetooth_interoperability_addr_list", null);
        Global.putString(this.mContentResolver, "hw_bluetooth_interoperability_name_list", null);
        Global.putString(this.mContentResolver, "hw_bluetooth_interoperability_manu_list", null);
        Global.putString(this.mContentResolver, "hw_bluetooth_interop_emui_version", this.mSystemEmuiVersion);
        HwLog.i(TAG, "BT_PARA clean ContentProvider success");
    }

    public void responseForParaUpdate(int result) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("BT_PARA response result: ");
        stringBuilder.append(result);
        HwLog.d(str, stringBuilder.toString());
        try {
            Class<?> class1 = Class.forName("android.emcom.EmcomManager");
            Object objInstance = class1.getDeclaredMethod(WifiProCHRManager.LOG_GET_INSTANCE_API_NAME, new Class[0]).invoke(class1, new Object[0]);
            if (objInstance != null) {
                class1.getDeclaredMethod("responseForParaUpgrade", new Class[]{Integer.TYPE, Integer.TYPE, Integer.TYPE}).invoke(objInstance, new Object[]{Integer.valueOf(16), Integer.valueOf(1), Integer.valueOf(result)});
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("BT_PARA response done with result: ");
                stringBuilder2.append(result);
                HwLog.i(str2, stringBuilder2.toString());
                return;
            }
            HwLog.w(TAG, "BT_PARA objInstance() is null");
        } catch (NoSuchMethodException e) {
            HwLog.e(TAG, "BT_PARA response exception: NoSuchMethod");
        } catch (IllegalAccessException e2) {
            HwLog.e(TAG, "BT_PARA response exception: IllegalAccessException");
        } catch (ClassNotFoundException e3) {
            HwLog.e(TAG, "BT_PARA response exception: EmcomManager not found");
        } catch (Exception e4) {
            HwLog.e(TAG, "BT_PARA response exception");
        }
    }

    private int getSavedConfigVersion() {
        int savedBtInteropVersion = 0;
        try {
            String savedConfigVersionString = Global.getString(this.mContentResolver, "hw_bluetooth_interop_version");
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("BT_PARA contentProvider saved config version is:");
            stringBuilder.append(savedConfigVersionString);
            HwLog.d(str, stringBuilder.toString());
            if (savedConfigVersionString != null) {
                savedBtInteropVersion = Integer.parseInt(savedConfigVersionString);
            } else {
                HwLog.d(TAG, "BT_PARA savedConfigVersionString is null");
            }
            return savedBtInteropVersion;
        } catch (NumberFormatException e) {
            HwLog.e(TAG, "BT_PARA savedConfigVersionString :invalid string:");
            return 0;
        } catch (Exception e2) {
            HwLog.e(TAG, "BT_PARA getSavedConfigVersion:invalid string:");
            return 0;
        } catch (Throwable th) {
            return 0;
        }
    }

    private int getConfigVersion(File file) {
        int btInteropVersion = 0;
        String emuiVersionString = "";
        try {
            if (file.exists()) {
                if (getVersion(file, "EMUI", "emui_version") == null) {
                    btParaFileError("BT_PARA get file EMUI is null");
                } else if (getVersion(file, "EMUI", "config_version") == null) {
                    btParaFileError("BT_PARA get file BT is null");
                } else {
                    String btVersionString = getVersion(file, "EMUI", "config_version").trim();
                    if ("".equals(btVersionString)) {
                        btParaFileError("BT_PARA btVersionString: empty string");
                    } else {
                        btInteropVersion = Integer.parseInt(btVersionString);
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("BT_PARA file btInteropVersion:");
                        stringBuilder.append(btInteropVersion);
                        HwLog.d(str, stringBuilder.toString());
                    }
                }
                return 0;
            }
            HwLog.i(TAG, "BT_PARA file not exist");
            return btInteropVersion;
        } catch (NumberFormatException e) {
            btParaFileError("BT_PARA btVersionString: invalid string");
            return 0;
        } catch (Exception e2) {
            btParaFileError("BT_PARA getConfigVersion Exception:");
            return 0;
        } catch (Throwable th) {
            return 0;
        }
    }

    private void updateBtInteropDataFromFile(File btInteropFile, boolean isCotaBroadcast) {
        InputStream btInteropIs = null;
        try {
            if (btInteropFile.exists()) {
                btInteropIs = new FileInputStream(btInteropFile);
                if (btInteropIs.available() == 0) {
                    if (isCotaBroadcast) {
                        responseForParaUpdate(6);
                    }
                    HwLog.e(TAG, "BT_PARA inputstream available() is 0");
                    try {
                        btInteropIs.close();
                    } catch (IOException e) {
                        HwLog.e(TAG, "BT_PARA btInteropFile close IOException:");
                    }
                    return;
                }
                parserXMLPULL(btInteropIs, isCotaBroadcast);
                if (isCotaBroadcast) {
                    if (this.mSendBroadcastToApk) {
                        this.mAdapter = BluetoothAdapter.getDefaultAdapter();
                        if (this.mAdapter.getState() == 12 || this.mAdapter.getState() == 11) {
                            Intent intent = new Intent();
                            intent.setAction(BT_PARA_UPDATE_ACTION);
                            this.mContext.sendBroadcast(intent, BT_PARA_UPDATE_PERMISSION);
                            HwLog.i(TAG, "BT_PARA send broadcast to app done");
                        } else {
                            HwLog.i(TAG, "BT_PARA bt state is not state on or state_turning_on not need to send broadcast");
                            responseForParaUpdate(7);
                        }
                        this.mSendBroadcastToApk = false;
                    } else {
                        btParaFileError("BT_PARA xml file format is incorrect");
                        responseForParaUpdate(6);
                    }
                }
                try {
                    btInteropIs.close();
                } catch (IOException e2) {
                    HwLog.e(TAG, "BT_PARA btInteropFile close IOException:");
                }
                return;
            }
            if (isCotaBroadcast) {
                HwLog.e(TAG, "BT_PARA ERROR - cota file does not exist");
                responseForParaUpdate(0);
            }
            if (btInteropIs != null) {
                try {
                    btInteropIs.close();
                } catch (IOException e3) {
                    HwLog.e(TAG, "BT_PARA btInteropFile close IOException:");
                }
            }
        } catch (FileNotFoundException e4) {
            if (isCotaBroadcast) {
                responseForParaUpdate(0);
            }
            btParaFileError("BT_PARA updateBtInteropDataFromFile FileNotFoundException");
            if (btInteropIs != null) {
                btInteropIs.close();
            }
        } catch (IOException e5) {
            if (isCotaBroadcast) {
                responseForParaUpdate(6);
            }
            btParaFileError("BT_PARA updateBtInteropDataFromFile IOException");
            if (btInteropIs != null) {
                btInteropIs.close();
            }
        } catch (Exception e6) {
            if (isCotaBroadcast) {
                responseForParaUpdate(6);
            }
            btParaFileError("BT_PARA updateBtInteropDataFromFile Exception");
            if (btInteropIs != null) {
                btInteropIs.close();
            }
        } catch (Throwable th) {
            if (btInteropIs != null) {
                try {
                    btInteropIs.close();
                } catch (IOException e7) {
                    HwLog.e(TAG, "BT_PARA btInteropFile close IOException:");
                }
            }
        }
    }

    private String getVersion(File file, String versionTag, String attributeTag) {
        String result = null;
        InputStream is = null;
        if (versionTag == null) {
            HwLog.d(TAG, "BT_PARA versionTag is null");
            return null;
        }
        try {
            if (file.exists()) {
                XmlPullParser versionParser = XmlPullParserFactory.newInstance().newPullParser();
                is = new FileInputStream(file);
                if (is.available() != 0) {
                    versionParser.setInput(is, "UTF-8");
                    for (int type = versionParser.getEventType(); type != 1; type = versionParser.next()) {
                        if (type == 2 && versionTag.equals(versionParser.getName())) {
                            result = versionParser.getAttributeValue(null, attributeTag);
                            break;
                        }
                    }
                } else {
                    HwLog.e(TAG, "BT_PARA getVersion: is.available() == 0");
                    try {
                        is.close();
                    } catch (IOException e) {
                        HwLog.e(TAG, "BT_PARA is close IOException:");
                    }
                    return null;
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e2) {
                    HwLog.e(TAG, "BT_PARA is close IOException:");
                }
            }
        } catch (XmlPullParserException e3) {
            btParaFileError("BT_PARA getVersion Parser Exception");
            if (is != null) {
                is.close();
            }
        } catch (IOException e4) {
            btParaFileError("BT_PARA getVersion IOException");
            if (is != null) {
                is.close();
            }
        } catch (Exception e5) {
            btParaFileError("BT_PARA getVersion Exception");
            if (is != null) {
                is.close();
            }
        } catch (Throwable th) {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e6) {
                    HwLog.e(TAG, "BT_PARA is close IOException:");
                }
            }
        }
        return result;
    }

    private void parserXMLPULL(InputStream is, boolean isCotaBroadcast) {
        IOException iOException;
        InputStream inputStream = is;
        boolean z = isCotaBroadcast;
        try {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("parserXMLPULL: isCota ");
            stringBuilder.append(z);
            HwLog.d(str, stringBuilder.toString());
            if (is.available() == 0) {
                if (z) {
                    responseForParaUpdate(6);
                }
                HwLog.e(TAG, "BT_PARA ERROR - file does not exist");
                if (inputStream != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        iOException = e;
                        HwLog.e(TAG, "BT_PARA is close IOException in parserXMLPULL:");
                    }
                }
                return;
            }
            String addr_str = null;
            String name_str = null;
            String manufacture_str = null;
            Object platformName = "";
            String tag_name = "";
            String databasesName = "";
            String releaseName = "";
            String configName = "";
            boolean firstHalfFormateCorrect = false;
            XmlPullParserFactory xpf = XmlPullParserFactory.newInstance();
            XmlPullParser parser = xpf.newPullParser();
            parser.setInput(inputStream, "UTF-8");
            int type = parser.getEventType();
            while (type != 1) {
                XmlPullParserFactory xpf2;
                String addr_str2;
                if (type != 0) {
                    switch (type) {
                        case 2:
                            xpf2 = xpf;
                            addr_str2 = addr_str;
                            xpf = parser.getName();
                            if ("databases".equals(xpf)) {
                                databasesName = "databases";
                            }
                            if (BuildConfig.BUILD_TYPE.equals(xpf) && "databases".equals(databasesName)) {
                                releaseName = BuildConfig.BUILD_TYPE;
                            }
                            if ("config".equals(xpf) && BuildConfig.BUILD_TYPE.equals(releaseName)) {
                                configName = "config";
                            }
                            if (HwCertification.SIGNATURE_PLATFORM.equals(xpf) && "config".equals(configName)) {
                                platformName = HwCertification.SIGNATURE_PLATFORM;
                                firstHalfFormateCorrect = true;
                            }
                            if (HwCertification.SIGNATURE_PLATFORM.equals(platformName)) {
                                if ("interop_addr".equals(xpf)) {
                                    addr_str2 = handleString(parser.nextText());
                                }
                                if ("interop_name".equals(xpf)) {
                                    name_str = handleString(parser.nextText());
                                }
                                if ("interop_manufacture".equals(xpf)) {
                                    manufacture_str = handleString(parser.nextText());
                                    addr_str = manufacture_str;
                                }
                            }
                            XmlPullParserFactory tag_name2 = xpf;
                            break;
                        case 3:
                            String platformName2;
                            String str2;
                            xpf2 = xpf;
                            if (!(HwCertification.SIGNATURE_PLATFORM.equals(parser.getName()) == null || HwCertification.SIGNATURE_PLATFORM.equals(platformName) == null)) {
                                platformName2 = "";
                            }
                            if (!("config".equals(parser.getName()) == null || !firstHalfFormateCorrect || "".equals(platformName2) == null)) {
                                configName = "";
                            }
                            if (!(BuildConfig.BUILD_TYPE.equals(parser.getName()) == null || !firstHalfFormateCorrect || "".equals(configName) == null)) {
                                releaseName = "";
                            }
                            if ("databases".equals(parser.getName()) == null || !firstHalfFormateCorrect || "".equals(releaseName) == null) {
                                addr_str2 = addr_str;
                                str2 = platformName2;
                            } else {
                                HwLog.d(TAG, "BT_PARA xml file formate is correct");
                                str2 = platformName2;
                                Global.putString(this.mContentResolver, "hw_bluetooth_interop_version", "0");
                                Global.putString(this.mContentResolver, "hw_bluetooth_interoperability_addr_list", addr_str);
                                Global.putString(this.mContentResolver, "hw_bluetooth_interoperability_name_list", name_str);
                                Global.putString(this.mContentResolver, "hw_bluetooth_interoperability_manu_list", manufacture_str);
                                Global.putString(this.mContentResolver, "hw_bluetooth_interop_emui_version", this.mSystemEmuiVersion);
                                StringBuilder stringBuilder2 = new StringBuilder();
                                addr_str2 = addr_str;
                                stringBuilder2.append(this.mToBeSavedBtInteropVersion);
                                stringBuilder2.append("");
                                Global.putString(this.mContentResolver, "hw_bluetooth_interop_version", stringBuilder2.toString());
                                xpf = TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("BT_PARA contentprovider saved xml config version is:");
                                stringBuilder3.append(this.mToBeSavedBtInteropVersion);
                                HwLog.w(xpf, stringBuilder3.toString());
                                xpf = "";
                                if (z) {
                                    this.mSendBroadcastToApk = true;
                                }
                                databasesName = xpf;
                            }
                            platformName = str2;
                            break;
                        case 4:
                            xpf2 = xpf;
                            break;
                        default:
                            xpf2 = xpf;
                            HwLog.w(TAG, "BT_PARA parse file - default clause, unexpected.");
                            break;
                    }
                    addr_str2 = addr_str;
                } else {
                    xpf2 = xpf;
                    addr_str2 = addr_str;
                }
                addr_str = addr_str2;
                type = parser.next();
                xpf = xpf2;
            }
            if (inputStream != null) {
                try {
                    is.close();
                } catch (IOException e2) {
                    iOException = e2;
                    HwLog.e(TAG, "BT_PARA is close IOException in parserXMLPULL:");
                }
            }
        } catch (XmlPullParserException e3) {
            HwLog.e(TAG, "BT_PARA parserXMLPULL exception: XmlPullParserException");
            if (inputStream != null) {
                is.close();
            }
        } catch (IOException e4) {
            HwLog.e(TAG, "BT_PARA parserXMLPULL exception: IOException");
            if (inputStream != null) {
                is.close();
            }
        } catch (Exception e5) {
            HwLog.e(TAG, "BT_PARA parserXMLPULL exception: Exception");
            if (inputStream != null) {
                is.close();
            }
        } catch (Throwable th) {
            Throwable th2 = th;
            if (inputStream != null) {
                try {
                    is.close();
                } catch (IOException e22) {
                    IOException iOException2 = e22;
                    HwLog.e(TAG, "BT_PARA is close IOException in parserXMLPULL:");
                }
            }
        }
    }

    private String handleString(String string) {
        String result = "";
        if (string != null) {
            try {
                for (String entry : string.split("\n")) {
                    String entry2 = entry2.trim();
                    if ("".equals(entry2)) {
                        HwLog.d(TAG, "BT_PARA handleString entry is null ");
                    } else if (entry2.charAt(entry2.length() - 1) == ';') {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(result);
                        stringBuilder.append(entry2);
                        result = stringBuilder.toString();
                    }
                }
            } catch (Exception e) {
                btParaFileError("BT_PARA handleString Exception");
            } catch (Throwable th) {
            }
        }
        return result;
    }

    private void btParaFileError(String msg) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("BT_PARA_FILE_WARNING ");
        stringBuilder.append(msg);
        HwLog.e(str, stringBuilder.toString());
    }

    public static String getProperty(String key, String defaultValue) {
        String value = defaultValue;
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            return (String) c.getMethod("get", new Class[]{String.class, String.class}).invoke(c, new Object[]{key, "unknown"});
        } catch (Exception e) {
            HwLog.e(TAG, "BT_PARA getProperty: e:");
        } catch (Throwable th) {
        }
        return value;
    }
}
