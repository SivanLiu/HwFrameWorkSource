package com.android.server.am;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Environment;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;
import com.android.internal.os.ProcessCpuTracker;
import com.android.server.hidata.wavemapping.cons.Constant;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class HwPowerInfoService implements IHwPowerInfoService {
    private static String BIGZIPFILEPATH = "log/sleeplog";
    private static String CONFIG_FILE = "PowerInfoImpl.xml";
    private static final int MAX_CPU_INDEX = 8;
    private static String PRODUCT_ETC_DIR = "/product/etc/";
    private static String SYSTEM_ETC_DIR = "/system/etc/";
    private static final String TAG = "HwPowerInfoService";
    private static final boolean debugOn = true;
    private static String logFileName = "power_stats-log";
    private static Context mContext = null;
    private static final Object mLock = new Object();
    private static final Object mLockInit = new Object();
    private static final Object mLock_wakelock = new Object();
    private static LogInfo mLogInfo;
    private static PowerInfoServiceReceiver mReceiver = null;
    private static HwPowerInfoService mSingleInstance = null;
    private static int musicValume = 0;
    private String BAT_TEMP;
    private String BOARD_TEMP;
    private String CPU0_TEMP;
    private String CPU1_TEMP;
    private String CPU_FREQ_HEAD;
    private String CPU_FREQ_TAIL;
    private String CPU_MAX_FREQ_TAIL;
    private String CPU_ONLINE;
    private String CURRENT;
    private String CURRENT_LIMIT;
    private String PA_TEMP;
    private String SOC_RM;
    private int TOP_PROCESS_NUM;
    private int WAKELOCK_NUM;
    private String WAKEUPSOURCE;
    private int WAKEUPSOURCE_NUM;
    private SimpleDateFormat dateFormate;
    private int hasCreateBigZip;
    private boolean isThreadAlive;
    private ProcessCpuTracker mCpuTracker;
    private String mLastTopAppName;
    private int mLastTopAppUid;
    private boolean mSuspendState;
    private int mWakeLockNumber;
    private WorkerThread mWorker;
    private int timestep;
    private int zipEndHour;
    private int zipFileMax;
    private int zipStartHour;

    static class PowerInfoServiceReceiver extends BroadcastReceiver {
        PowerInfoServiceReceiver() {
        }

        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onReceive(Context context, Intent intent) {
            int i;
            String action = intent.getAction();
            switch (action.hashCode()) {
                case -1940635523:
                    if (action.equals("android.media.VOLUME_CHANGED_ACTION")) {
                        i = 6;
                        break;
                    }
                case -1875733435:
                    if (action.equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                        i = 2;
                        break;
                    }
                case -1676458352:
                    if (action.equals("android.intent.action.HEADSET_PLUG")) {
                        i = 0;
                        break;
                    }
                case -1530327060:
                    if (action.equals("android.bluetooth.adapter.action.STATE_CHANGED")) {
                        i = 4;
                        break;
                    }
                case 409953495:
                    if (action.equals("android.net.wifi.WIFI_AP_STATE_CHANGED")) {
                        i = 3;
                        break;
                    }
                case 1123270207:
                    if (action.equals("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED")) {
                        i = 5;
                        break;
                    }
                case 1943044864:
                    if (action.equals("android.nfc.action.ADAPTER_STATE_CHANGED")) {
                        i = 1;
                        break;
                    }
                default:
                    i = -1;
                    break;
            }
            switch (i) {
                case 0:
                    i = intent.getIntExtra("state", 0);
                    if (i == 1) {
                        HwPowerInfoService.mLogInfo.mHeadSet = 1;
                        return;
                    } else if (i == 0) {
                        HwPowerInfoService.mLogInfo.mHeadSet = 0;
                        return;
                    } else {
                        return;
                    }
                case 1:
                    i = intent.getIntExtra("android.nfc.extra.ADAPTER_STATE", 1);
                    if (i == 3 || i == 2) {
                        HwPowerInfoService.mLogInfo.mNFCOn = 1;
                        return;
                    } else {
                        HwPowerInfoService.mLogInfo.mNFCOn = 0;
                        return;
                    }
                case 2:
                    i = intent.getIntExtra("wifi_state", 1);
                    if (i == 3 || i == 2) {
                        HwPowerInfoService.mLogInfo.mWifiStatus = 1;
                        return;
                    } else if (i == 1 || i == 0) {
                        HwPowerInfoService.mLogInfo.mWifiStatus = 0;
                        return;
                    } else {
                        return;
                    }
                case 3:
                    i = intent.getIntExtra("wifi_state", 11);
                    if (i == 13 || i == 12) {
                        HwPowerInfoService.mLogInfo.mWifiAPStatus = 3;
                        return;
                    } else if (i == 11 || i == 10) {
                        HwPowerInfoService.mLogInfo.mWifiAPStatus = 0;
                        return;
                    } else {
                        return;
                    }
                case 4:
                    i = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", Integer.MIN_VALUE);
                    if (i == 12 || i == 11) {
                        HwPowerInfoService.mLogInfo.mBTState = 1;
                        return;
                    } else if (i == 10 || i == 13) {
                        HwPowerInfoService.mLogInfo.mBTState = 0;
                        return;
                    } else {
                        return;
                    }
                case 5:
                    i = intent.getIntExtra("android.bluetooth.adapter.extra.CONNECTION_STATE", Integer.MIN_VALUE);
                    if (i == 2) {
                        HwPowerInfoService.mLogInfo.mBTState = 2;
                        return;
                    } else if (i == 0) {
                        HwPowerInfoService.mLogInfo.mBTState = 1;
                        return;
                    } else {
                        return;
                    }
                case 6:
                    if (intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1) == 3) {
                        HwPowerInfoService.musicValume = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    private class WorkerThread extends Thread {
        public WorkerThread(String name) {
            super(name);
        }

        public void run() {
            synchronized (HwPowerInfoService.mLock) {
                while (HwPowerInfoService.mSingleInstance != null && HwPowerInfoService.this.isThreadAlive) {
                    try {
                        HwPowerInfoService.mLock.wait((long) HwPowerInfoService.this.timestep);
                        if (SystemProperties.getBoolean("persist.sys.huawei.debug.on", false) || SystemProperties.getBoolean("hwlog.remotedebug", false)) {
                            HwPowerInfoService.this.enter();
                            HwPowerInfoService.mLogInfo.mAlarmName = null;
                            HwPowerInfoService.mLogInfo.mWakeupReason = null;
                        }
                    } catch (InterruptedException e) {
                        Log.i(HwPowerInfoService.TAG, "InterruptedException error");
                    }
                }
                Log.i(HwPowerInfoService.TAG, "WorkerThread Exit");
            }
        }
    }

    private void parseNode(Node node) {
        if (node != null) {
            NodeList nodeList = node.getChildNodes();
            int size = nodeList.getLength();
            for (int i = 0; i < size; i++) {
                Node child = nodeList.item(i);
                if (child instanceof Element) {
                    String childName = child.getNodeName();
                    String text = child.getFirstChild().getNodeValue();
                    if (childName.equals("CPU_freq")) {
                        this.CPU_FREQ_HEAD = text;
                    } else if (childName.equals("CPU0_temp")) {
                        this.CPU0_TEMP = text;
                    } else if (childName.equals("CPU1_temp")) {
                        this.CPU1_TEMP = text;
                    } else if (childName.equals("Board_temp")) {
                        this.BOARD_TEMP = text;
                    } else if (childName.equals("PA_temp")) {
                        this.PA_TEMP = text;
                    } else if (childName.equals("Battery_temp")) {
                        this.BAT_TEMP = text;
                    } else if (childName.equals("CPU_online")) {
                        this.CPU_ONLINE = text;
                    } else if (childName.equals("WakeupSource")) {
                        this.WAKEUPSOURCE = text;
                    } else if (childName.equals("Current")) {
                        this.CURRENT = text;
                    } else if (childName.equals("Current_limit")) {
                        this.CURRENT_LIMIT = text;
                    } else if (childName.equals("Capacity_rm")) {
                        this.SOC_RM = text;
                    } else if (childName.equals("TimeStep")) {
                        this.timestep = Integer.parseInt(text);
                    } else if (childName.equals("ZipFileMax")) {
                        this.zipFileMax = Integer.parseInt(text);
                    } else if (childName.equals("Log_path")) {
                        BIGZIPFILEPATH = text;
                    } else if (childName.equals("Log_name")) {
                        logFileName = text;
                    } else {
                        int i2 = 3;
                        if (childName.equals("WakeLock_num")) {
                            if (text != null) {
                                i2 = Integer.parseInt(text);
                            }
                            this.WAKELOCK_NUM = i2;
                        } else if (childName.equals("WakeupSource_num")) {
                            if (text != null) {
                                i2 = Integer.parseInt(text);
                            }
                            this.WAKEUPSOURCE_NUM = i2;
                        } else if (childName.equals("Top_process_num")) {
                            if (text != null) {
                                i2 = Integer.parseInt(text);
                            }
                            this.TOP_PROCESS_NUM = i2;
                        }
                    }
                }
            }
        }
    }

    private void parseXML() {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(PRODUCT_ETC_DIR);
            stringBuilder.append(CONFIG_FILE);
            File file = new File(stringBuilder.toString());
            int i = 0;
            if (!file.exists()) {
                String str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(PRODUCT_ETC_DIR);
                stringBuilder2.append(CONFIG_FILE);
                stringBuilder2.append("is not exists");
                Log.i(str, stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(SYSTEM_ETC_DIR);
                stringBuilder2.append(CONFIG_FILE);
                file = new File(stringBuilder2.toString());
                if (!file.exists()) {
                    str = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(SYSTEM_ETC_DIR);
                    stringBuilder2.append(CONFIG_FILE);
                    stringBuilder2.append("is not exists");
                    Log.i(str, stringBuilder2.toString());
                    this.isThreadAlive = false;
                    return;
                }
            }
            if (builder != null) {
                NodeList nodeList = builder.parse(file).getDocumentElement().getChildNodes();
                int size = nodeList.getLength();
                while (i < size) {
                    Node child = nodeList.item(i);
                    if (child instanceof Element) {
                        parseNode(child);
                    }
                    i++;
                }
            }
        } catch (ParserConfigurationException e) {
            Log.i(TAG, "ParserConfigurationException error");
        } catch (SAXException e2) {
            Log.i(TAG, "SAXException error");
        } catch (IOException e3) {
            Log.i(TAG, "IOException error");
        }
    }

    private void enter() {
        Calendar now = Calendar.getInstance();
        int hours = now.get(11);
        if (hours < this.zipStartHour || hours >= this.zipEndHour || this.hasCreateBigZip != 0) {
            if (hours >= this.zipEndHour) {
                this.hasCreateBigZip = 0;
            }
            beginWriteFile();
            return;
        }
        long nowTime = now.getTimeInMillis();
        Log.i(TAG, "zipFile");
        try {
            beginWriteFile();
            zipFile(nowTime);
        } catch (Exception e) {
            Log.i(TAG, "failed to zipFile");
        }
    }

    private String writeWakeLockToFile(LogInfo Data) {
        StringBuffer result = new StringBuffer();
        String tempString = null;
        int index = 0;
        synchronized (mLock_wakelock) {
            int i;
            try {
                if (mLogInfo.mWakeupReason != null) {
                    for (i = 0; i < this.WAKELOCK_NUM; i++) {
                        result.append(String.format("%-41s", new Object[]{"NA"}));
                    }
                } else {
                    i = 0;
                    String tempString2 = null;
                    int i2 = 0;
                    while (i2 < this.WAKELOCK_NUM) {
                        try {
                            if (i2 < mLogInfo.mWakeLocks.size()) {
                                WakeLock wakelock = (WakeLock) mLogInfo.mWakeLocks.get(i2);
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append(wakelock.mWakeLockPID);
                                stringBuilder.append("/");
                                stringBuilder.append(wakelock.mWakeLockName);
                                tempString2 = stringBuilder.toString();
                                if (tempString2.length() > 40) {
                                    tempString2 = tempString2.substring(0, 40);
                                }
                                result.append(String.format("%-41s", new Object[]{tempString2}));
                                i++;
                            }
                            i2++;
                        } catch (Throwable th) {
                            tempString = th;
                            throw tempString;
                        }
                    }
                    while (i < this.WAKELOCK_NUM) {
                        result.append(String.format("%-41s", new Object[]{"NA"}));
                        i++;
                    }
                    tempString = tempString2;
                    index = i;
                }
                return result.toString();
            } catch (Throwable th2) {
                int i3 = index;
                tempString = th2;
                i = i3;
                throw tempString;
            }
        }
    }

    private void getCurrentTime(LogInfo Data) {
        Data.mTime = this.dateFormate.format(new Date(System.currentTimeMillis()));
    }

    /* JADX WARNING: Unknown top exception splitter block from list: {B:59:0x0100=Splitter:B:59:0x0100, B:45:0x00e4=Splitter:B:45:0x00e4, B:73:0x011c=Splitter:B:73:0x011c} */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x0125 A:{SYNTHETIC, Splitter:B:76:0x0125} */
    /* JADX WARNING: Removed duplicated region for block: B:80:0x012c A:{SYNTHETIC, Splitter:B:80:0x012c} */
    /* JADX WARNING: Removed duplicated region for block: B:104:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:84:0x0133 A:{SYNTHETIC, Splitter:B:84:0x0133} */
    /* JADX WARNING: Removed duplicated region for block: B:62:0x0109 A:{SYNTHETIC, Splitter:B:62:0x0109} */
    /* JADX WARNING: Removed duplicated region for block: B:66:0x0110 A:{SYNTHETIC, Splitter:B:66:0x0110} */
    /* JADX WARNING: Removed duplicated region for block: B:103:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:70:0x0117 A:{SYNTHETIC, Splitter:B:70:0x0117} */
    /* JADX WARNING: Removed duplicated region for block: B:48:0x00ed A:{SYNTHETIC, Splitter:B:48:0x00ed} */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x00f4 A:{SYNTHETIC, Splitter:B:52:0x00f4} */
    /* JADX WARNING: Removed duplicated region for block: B:102:? A:{SYNTHETIC, RETURN, ORIG_RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x00fb A:{SYNTHETIC, Splitter:B:56:0x00fb} */
    /* JADX WARNING: Removed duplicated region for block: B:87:0x013a A:{SYNTHETIC, Splitter:B:87:0x013a} */
    /* JADX WARNING: Removed duplicated region for block: B:91:0x0141 A:{SYNTHETIC, Splitter:B:91:0x0141} */
    /* JADX WARNING: Removed duplicated region for block: B:95:0x0148 A:{SYNTHETIC, Splitter:B:95:0x0148} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void getDataFromWakeupSource(LogInfo Data) {
        Throwable th;
        FileInputStream fInputStream = null;
        InputStreamReader inputReader = null;
        BufferedReader read = null;
        int counter = 0;
        int index = 0;
        try {
            fInputStream = new FileInputStream(this.WAKEUPSOURCE);
            inputReader = new InputStreamReader(fInputStream, "utf-8");
            read = new BufferedReader(inputReader);
            for (String tempString = read.readLine(); tempString != null && counter < this.WAKEUPSOURCE_NUM; tempString = read.readLine()) {
                if (tempString.startsWith("Active resource:")) {
                    index = tempString.indexOf("\t");
                    String temp = tempString.substring(17, index);
                    if (temp.indexOf(" ") == -1) {
                        ((WakeupSource) Data.mWakeupSources.get(counter)).mWakeupSourceName = temp;
                    } else {
                        ((WakeupSource) Data.mWakeupSources.get(counter)).mWakeupSourceName = temp.substring(0, temp.indexOf(" "));
                    }
                    int index2 = tempString.indexOf("\t\t", index + 1);
                    index = 0;
                    while (index < 3) {
                        try {
                            index2 = tempString.indexOf("\t\t", index2 + 2);
                            index++;
                        } catch (FileNotFoundException e) {
                            index = index2;
                            Log.e(TAG, "not found the wakeupsource");
                            if (inputReader != null) {
                            }
                            if (read != null) {
                            }
                            if (fInputStream == null) {
                            }
                        } catch (UnsupportedEncodingException e2) {
                            index = index2;
                            Log.e(TAG, "not support utf-8");
                            if (inputReader != null) {
                            }
                            if (read != null) {
                            }
                            if (fInputStream == null) {
                            }
                        } catch (IOException e3) {
                            index = index2;
                            try {
                                Log.e(TAG, "read wakeupsource failed");
                                if (inputReader != null) {
                                }
                                if (read != null) {
                                }
                                if (fInputStream == null) {
                                }
                            } catch (Throwable th2) {
                                th = th2;
                                if (inputReader != null) {
                                    try {
                                        inputReader.close();
                                    } catch (Exception e4) {
                                    }
                                }
                                if (read != null) {
                                    try {
                                        read.close();
                                    } catch (Exception e5) {
                                    }
                                }
                                if (fInputStream != null) {
                                    try {
                                        fInputStream.close();
                                    } catch (IOException e6) {
                                    }
                                }
                                throw th;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            index = index2;
                            if (inputReader != null) {
                            }
                            if (read != null) {
                            }
                            if (fInputStream != null) {
                            }
                            throw th;
                        }
                    }
                    ((WakeupSource) Data.mWakeupSources.get(counter)).mActiveTime = tempString.substring(index2 + 2, tempString.indexOf("\t\t", index2 + 2));
                    counter++;
                }
            }
            while (counter < this.WAKEUPSOURCE_NUM) {
                ((WakeupSource) Data.mWakeupSources.get(counter)).mWakeupSourceName = null;
                ((WakeupSource) Data.mWakeupSources.get(counter)).mActiveTime = null;
                counter++;
            }
            try {
                inputReader.close();
            } catch (Exception e7) {
            }
            try {
                read.close();
            } catch (Exception e8) {
            }
            try {
                fInputStream.close();
            } catch (IOException e9) {
            }
        } catch (FileNotFoundException e10) {
            Log.e(TAG, "not found the wakeupsource");
            if (inputReader != null) {
                try {
                    inputReader.close();
                } catch (Exception e11) {
                }
            }
            if (read != null) {
                try {
                    read.close();
                } catch (Exception e12) {
                }
            }
            if (fInputStream == null) {
                fInputStream.close();
            }
        } catch (UnsupportedEncodingException e13) {
            Log.e(TAG, "not support utf-8");
            if (inputReader != null) {
                try {
                    inputReader.close();
                } catch (Exception e14) {
                }
            }
            if (read != null) {
                try {
                    read.close();
                } catch (Exception e15) {
                }
            }
            if (fInputStream == null) {
                fInputStream.close();
            }
        } catch (IOException e16) {
            Log.e(TAG, "read wakeupsource failed");
            if (inputReader != null) {
                try {
                    inputReader.close();
                } catch (Exception e17) {
                }
            }
            if (read != null) {
                try {
                    read.close();
                } catch (Exception e18) {
                }
            }
            if (fInputStream == null) {
                fInputStream.close();
            }
        }
    }

    private String getOneLineString(String path) {
        String str;
        StringBuilder stringBuilder;
        BufferedReader bReader = null;
        if (path == null) {
            return null;
        }
        String tempString = null;
        FileInputStream fInputStream = null;
        InputStreamReader inputReader = null;
        try {
            fInputStream = new FileInputStream(path);
            inputReader = new InputStreamReader(fInputStream, "utf-8");
            bReader = new BufferedReader(inputReader);
            tempString = bReader.readLine();
            try {
                bReader.close();
            } catch (IOException e) {
            }
            try {
                inputReader.close();
            } catch (Exception e2) {
            }
            try {
                fInputStream.close();
            } catch (IOException e3) {
            }
        } catch (FileNotFoundException e4) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("not found the ");
            stringBuilder.append(path);
            Log.e(str, stringBuilder.toString());
            if (bReader != null) {
                try {
                    bReader.close();
                } catch (IOException e5) {
                }
            }
            if (inputReader != null) {
                try {
                    inputReader.close();
                } catch (Exception e6) {
                }
            }
            if (fInputStream != null) {
                fInputStream.close();
            }
        } catch (UnsupportedEncodingException e7) {
            Log.e(TAG, "not support utf-8");
            if (bReader != null) {
                try {
                    bReader.close();
                } catch (IOException e8) {
                }
            }
            if (inputReader != null) {
                try {
                    inputReader.close();
                } catch (Exception e9) {
                }
            }
            if (fInputStream != null) {
                fInputStream.close();
            }
        } catch (IOException e10) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("read ");
            stringBuilder.append(path);
            stringBuilder.append(" failed");
            Log.e(str, stringBuilder.toString());
            if (bReader != null) {
                try {
                    bReader.close();
                } catch (IOException e11) {
                }
            }
            if (inputReader != null) {
                try {
                    inputReader.close();
                } catch (Exception e12) {
                }
            }
            if (fInputStream != null) {
                fInputStream.close();
            }
        } catch (Throwable th) {
            if (bReader != null) {
                try {
                    bReader.close();
                } catch (IOException e13) {
                }
            }
            if (inputReader != null) {
                try {
                    inputReader.close();
                } catch (Exception e14) {
                }
            }
            if (fInputStream != null) {
                try {
                    fInputStream.close();
                } catch (IOException e15) {
                }
            }
        }
        return tempString;
    }

    private void getCpuFreq() {
        StringBuilder stringBuilder;
        String oneLineString;
        StringBuilder stringBuilder2;
        String oneLineString2;
        String oneLineString3;
        int i = 0;
        while (i < 4) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.CPU_FREQ_HEAD);
            stringBuilder.append(i);
            stringBuilder.append(this.CPU_FREQ_TAIL);
            if (true == new File(stringBuilder.toString()).exists()) {
                break;
            }
            i++;
        }
        LogInfo logInfo = mLogInfo;
        if (i < 4) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.CPU_FREQ_HEAD);
            stringBuilder.append(i);
            stringBuilder.append(this.CPU_FREQ_TAIL);
            oneLineString = getOneLineString(stringBuilder.toString());
        } else {
            oneLineString = "NA";
        }
        logInfo.mCPU0Freq = oneLineString;
        logInfo = mLogInfo;
        if (i < 4) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(this.CPU_FREQ_HEAD);
            stringBuilder2.append(i);
            stringBuilder2.append(this.CPU_MAX_FREQ_TAIL);
            oneLineString2 = getOneLineString(stringBuilder2.toString());
        } else {
            oneLineString2 = "NA";
        }
        logInfo.mCPU0Freq_Max = oneLineString2;
        i = 4;
        while (i < 8) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.CPU_FREQ_HEAD);
            stringBuilder.append(i);
            stringBuilder.append(this.CPU_FREQ_TAIL);
            if (true == new File(stringBuilder.toString()).exists()) {
                break;
            }
            i++;
        }
        LogInfo logInfo2 = mLogInfo;
        if (i < 8) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(this.CPU_FREQ_HEAD);
            stringBuilder3.append(i);
            stringBuilder3.append(this.CPU_FREQ_TAIL);
            oneLineString3 = getOneLineString(stringBuilder3.toString());
        } else {
            oneLineString3 = "NA";
        }
        logInfo2.mCPU4Freq = oneLineString3;
        logInfo2 = mLogInfo;
        if (i < 8) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(this.CPU_FREQ_HEAD);
            stringBuilder2.append(i);
            stringBuilder2.append(this.CPU_MAX_FREQ_TAIL);
            oneLineString2 = getOneLineString(stringBuilder2.toString());
        } else {
            oneLineString2 = "NA";
        }
        logInfo2.mCPU4Freq_Max = oneLineString2;
    }

    private void beginWriteFile() {
        getCurrentTime(mLogInfo);
        mLogInfo.mCPUOnLine = getOneLineString(this.CPU_ONLINE);
        getCpuFreq();
        String s = getOneLineString(this.CURRENT);
        mLogInfo.mCurrent = s == null ? "0" : s;
        s = getOneLineString(this.CURRENT_LIMIT);
        mLogInfo.mCurrentLimit = s == null ? "0" : s;
        s = getOneLineString(this.SOC_RM);
        mLogInfo.mSOC_rm = s == null ? "NA" : s;
        s = getOneLineString(this.CPU0_TEMP);
        mLogInfo.mCPU0Temp = s == null ? "0" : s;
        s = getOneLineString(this.CPU1_TEMP);
        mLogInfo.mCPU1Temp = s == null ? "0" : s;
        s = getOneLineString(this.BOARD_TEMP);
        mLogInfo.mBoardTemp = s == null ? "0" : s;
        s = getOneLineString(this.PA_TEMP);
        mLogInfo.mPA_temp = s == null ? "0" : s;
        s = getOneLineString(this.BAT_TEMP);
        mLogInfo.mBattery_temp = s == null ? "0" : s;
        try {
            getCpuInfo(mLogInfo);
        } catch (Exception e) {
            Log.w(TAG, "failed to getCpuInfo");
        }
        if (mLogInfo.mWakeupReason != null) {
            for (WakeupSource wakeupsource : mLogInfo.mWakeupSources) {
                wakeupsource.mWakeupSourceName = null;
            }
        } else {
            try {
                getDataFromWakeupSource(mLogInfo);
            } catch (Exception e2) {
                Log.w(TAG, "failed to getDataFromWakeupSource");
            }
        }
        writeFile();
    }

    private void writeTitleToFile(PrintWriter printWriter) {
        int i;
        Object[] objArr;
        StringBuilder stringBuilder;
        StringBuffer writeData = new StringBuffer();
        writeData.append(String.format("%-11s", new Object[]{"Date"}));
        writeData.append(String.format("%-15s", new Object[]{"Time"}));
        writeData.append(String.format("%-7s", new Object[]{"Bright"}));
        writeData.append(String.format("%-5s", new Object[]{"SOC"}));
        writeData.append(String.format("%-7s", new Object[]{"SOC_rm"}));
        writeData.append(String.format("%-10s", new Object[]{"Current"}));
        writeData.append(String.format("%-10s", new Object[]{"Cur_Limit"}));
        writeData.append(String.format("%-9s", new Object[]{"Charging"}));
        writeData.append(String.format("%-6s", new Object[]{"Modem"}));
        writeData.append(String.format("%-11s", new Object[]{"SignalType"}));
        writeData.append(String.format("%-15s", new Object[]{"SignalStrength"}));
        writeData.append(String.format("%-15s", new Object[]{"DateConnection"}));
        writeData.append(String.format("%-5s", new Object[]{Constant.USERDB_APP_NAME_WIFI}));
        writeData.append(String.format("%-4s", new Object[]{"BT"}));
        writeData.append(String.format("%-4s", new Object[]{"GPS"}));
        writeData.append(String.format("%-7s", new Object[]{"Camera"}));
        writeData.append(String.format("%-4s", new Object[]{"NFC"}));
        writeData.append(String.format("%-8s", new Object[]{"Headset"}));
        writeData.append(String.format("%-9s", new Object[]{"musicVal"}));
        writeData.append(String.format("%-41s", new Object[]{"FrontAPP"}));
        for (i = 1; i <= this.WAKELOCK_NUM; i++) {
            objArr = new Object[1];
            stringBuilder = new StringBuilder();
            stringBuilder.append("PID");
            stringBuilder.append(i);
            stringBuilder.append("/WakeLock");
            stringBuilder.append(i);
            objArr[0] = stringBuilder.toString();
            writeData.append(String.format("%-41s", objArr));
        }
        for (i = 1; i <= this.WAKEUPSOURCE_NUM; i++) {
            objArr = new Object[1];
            stringBuilder = new StringBuilder();
            stringBuilder.append("ActiveWakeSource");
            stringBuilder.append(i);
            stringBuilder.append("/Time");
            stringBuilder.append(i);
            objArr[0] = stringBuilder.toString();
            writeData.append(String.format("%-46s", objArr));
        }
        writeData.append(String.format("%-13s", new Object[]{"SuspendState"}));
        writeData.append(String.format("%-9s", new Object[]{"CPU0Temp"}));
        writeData.append(String.format("%-9s", new Object[]{"CPU1Temp"}));
        writeData.append(String.format("%-10s", new Object[]{"BoardTemp"}));
        writeData.append(String.format("%-9s", new Object[]{"PA_Temp"}));
        writeData.append(String.format("%-9s", new Object[]{"BAT_TEMP"}));
        writeData.append(String.format("%-11s", new Object[]{"CPULoading"}));
        for (i = 1; i <= this.TOP_PROCESS_NUM; i++) {
            objArr = new Object[1];
            stringBuilder = new StringBuilder();
            stringBuilder.append("cpu_load");
            stringBuilder.append(i);
            stringBuilder.append("/PID");
            stringBuilder.append(i);
            stringBuilder.append("/TOP_process");
            stringBuilder.append(i);
            objArr[0] = stringBuilder.toString();
            writeData.append(String.format("%-41s", objArr));
        }
        writeData.append(String.format("%-10s", new Object[]{"CPU0Freq"}));
        writeData.append(String.format("%-10s", new Object[]{"CPU0_Max"}));
        writeData.append(String.format("%-10s", new Object[]{"CPU4Freq"}));
        writeData.append(String.format("%-10s", new Object[]{"CPU4_Max"}));
        writeData.append(String.format("%-10s", new Object[]{"CPUOnline"}));
        writeData.append(String.format("%-71s", new Object[]{"WakeupReason"}));
        writeData.append("AlarmName");
        printWriter.println(writeData.toString());
    }

    private void writeDataToFile(PrintWriter printWriter, LogInfo Data) {
        String str;
        StringBuilder stringBuilder;
        StringBuffer writeData = new StringBuffer();
        writeData.append(String.format("%-26s", new Object[]{Data.mTime}));
        writeData.append(String.format("%-7d", new Object[]{Integer.valueOf(Data.mBrightness)}));
        writeData.append(String.format("%-5d", new Object[]{Integer.valueOf(Data.mBatteryLevel)}));
        writeData.append(String.format("%-7s", new Object[]{Data.mSOC_rm}));
        writeData.append(String.format("%-10s", new Object[]{Data.mCurrent}));
        writeData.append(String.format("%-10s", new Object[]{Data.mCurrentLimit}));
        writeData.append(String.format("%-9d", new Object[]{Integer.valueOf(Data.mChargeStatus)}));
        writeData.append(String.format("%-6s", new Object[]{SystemProperties.get("persist.sys.logsystem.modem", "NA")}));
        writeData.append(String.format("%-11d", new Object[]{Integer.valueOf(Data.mConnectionStatus)}));
        writeData.append(String.format("%-15d", new Object[]{Integer.valueOf(Data.mSignalStrength)}));
        writeData.append(String.format("%-15d", new Object[]{Integer.valueOf(Data.mDataConnection)}));
        writeData.append(String.format("%-5d", new Object[]{Integer.valueOf(Data.mWifiStatus + Data.mWifiAPStatus)}));
        writeData.append(String.format("%-4d", new Object[]{Integer.valueOf(Data.mBTState)}));
        writeData.append(String.format("%-4d", new Object[]{Integer.valueOf(Data.mGPSStatus)}));
        writeData.append(String.format("%-7d", new Object[]{Integer.valueOf(Data.mCameraState)}));
        writeData.append(String.format("%-4d", new Object[]{Integer.valueOf(Data.mNFCOn)}));
        writeData.append(String.format("%-8d", new Object[]{Integer.valueOf(Data.mHeadSet)}));
        writeData.append(String.format("%-9d", new Object[]{Integer.valueOf(musicValume)}));
        if (Data.mTopAppName == null) {
            str = "NA";
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(Data.mTopAppUID);
            stringBuilder2.append("/");
            stringBuilder2.append(Data.mTopAppName);
            str = stringBuilder2.toString();
        }
        String tempString = str;
        if (tempString.length() > 40) {
            tempString = tempString.substring(0, 40);
        }
        writeData.append(String.format("%-41s", new Object[]{tempString}));
        writeData.append(writeWakeLockToFile(Data));
        for (WakeupSource wakeupsource : Data.mWakeupSources) {
            if (wakeupsource.mWakeupSourceName != null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(wakeupsource.mWakeupSourceName);
                stringBuilder.append("/");
                stringBuilder.append(wakeupsource.mActiveTime);
                tempString = stringBuilder.toString();
                if (tempString.length() > 45) {
                    tempString = tempString.substring(0, 45);
                }
                writeData.append(String.format("%-46s", new Object[]{tempString}));
            } else {
                writeData.append(String.format("%-46s", new Object[]{"NA"}));
            }
        }
        int i = (this.mSuspendState && this.mWakeLockNumber == 0) ? 1 : 0;
        int enterAsleep = i;
        writeData.append(String.format("%-13d", new Object[]{Integer.valueOf(enterAsleep)}));
        writeData.append(String.format("%-9s", new Object[]{Data.mCPU0Temp}));
        writeData.append(String.format("%-9s", new Object[]{Data.mCPU1Temp}));
        writeData.append(String.format("%-10s", new Object[]{Data.mBoardTemp}));
        writeData.append(String.format("%-9s", new Object[]{Data.mPA_temp}));
        writeData.append(String.format("%-9s", new Object[]{Data.mBattery_temp}));
        writeData.append(String.format("%-11d", new Object[]{Integer.valueOf(Data.mCpuTotalLoad)}));
        for (CpuTopLoad cputopload : Data.mCpuTopLoads) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(cputopload.mCpuTopLoad);
            stringBuilder.append("/");
            stringBuilder.append(cputopload.mCpuTopPid);
            stringBuilder.append("/");
            stringBuilder.append(cputopload.mCpuTopName);
            tempString = stringBuilder.toString();
            if (tempString.length() > 40) {
                tempString = tempString.substring(0, 40);
            }
            writeData.append(String.format("%-41s", new Object[]{tempString}));
        }
        tempString = Data.mWakeupReason == null ? "NA" : Data.mWakeupReason;
        if (tempString.length() > 70) {
            tempString = tempString.substring(0, 70);
        }
        writeData.append(String.format("%-10s", new Object[]{Data.mCPU0Freq}));
        writeData.append(String.format("%-10s", new Object[]{Data.mCPU0Freq_Max}));
        writeData.append(String.format("%-10s", new Object[]{Data.mCPU4Freq}));
        writeData.append(String.format("%-10s", new Object[]{Data.mCPU4Freq_Max}));
        writeData.append(String.format("%-10s", new Object[]{Data.mCPUOnLine}));
        writeData.append(String.format("%-71s", new Object[]{tempString}));
        writeData.append(Data.mAlarmName == null ? "NA" : Data.mAlarmName);
        printWriter.println(writeData.toString());
    }

    private void writeFile() {
        int flag = 0;
        File dataDirectory = Environment.getDataDirectory();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(BIGZIPFILEPATH);
        stringBuilder.append(File.separator);
        stringBuilder.append(logFileName);
        File powerinfologtxt = new File(dataDirectory, stringBuilder.toString());
        FileOutputStream fOutputStream = null;
        OutputStreamWriter outWriter = null;
        PrintWriter pw = null;
        try {
            if (!(powerinfologtxt.getParentFile().exists() || powerinfologtxt.getParentFile().mkdirs())) {
                Log.w(TAG, "fail to create dir");
            }
            if (!powerinfologtxt.exists()) {
                if (!powerinfologtxt.createNewFile()) {
                    Log.w(TAG, "fail to create new file");
                }
                powerinfologtxt.setReadable(true, false);
                flag = 1;
            }
            fOutputStream = new FileOutputStream(powerinfologtxt, true);
            outWriter = new OutputStreamWriter(fOutputStream, "utf-8");
            pw = new PrintWriter(outWriter);
            if (flag == 1) {
                writeTitleToFile(pw);
            }
            writeDataToFile(pw, mLogInfo);
            pw.flush();
            pw.close();
            try {
                outWriter.close();
            } catch (IOException e) {
            }
            try {
                fOutputStream.close();
            } catch (IOException e2) {
            }
        } catch (FileNotFoundException e3) {
            Log.e(TAG, "not found log file");
            if (pw != null) {
                pw.close();
            }
            if (outWriter != null) {
                try {
                    outWriter.close();
                } catch (IOException e4) {
                }
            }
            if (fOutputStream != null) {
                fOutputStream.close();
            }
        } catch (UnsupportedEncodingException e5) {
            Log.e(TAG, "not support utf-8");
            if (pw != null) {
                pw.close();
            }
            if (outWriter != null) {
                try {
                    outWriter.close();
                } catch (IOException e6) {
                }
            }
            if (fOutputStream != null) {
                fOutputStream.close();
            }
        } catch (IOException e7) {
            Log.e(TAG, "IOException.");
            if (pw != null) {
                pw.close();
            }
            if (outWriter != null) {
                try {
                    outWriter.close();
                } catch (IOException e8) {
                }
            }
            if (fOutputStream != null) {
                fOutputStream.close();
            }
        } catch (Throwable th) {
            if (pw != null) {
                pw.close();
            }
            if (outWriter != null) {
                try {
                    outWriter.close();
                } catch (IOException e9) {
                }
            }
            if (fOutputStream != null) {
                try {
                    fOutputStream.close();
                } catch (IOException e10) {
                }
            }
        }
    }

    private void zipFile(long num) {
        File zipfilei;
        File dataDirectory = Environment.getDataDirectory();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(BIGZIPFILEPATH);
        stringBuilder.append(File.separator);
        stringBuilder.append(logFileName);
        stringBuilder.append("-");
        stringBuilder.append(this.zipFileMax);
        stringBuilder.append(".tar.gz");
        File zipfile = new File(dataDirectory, stringBuilder.toString());
        if (zipfile.exists() && !zipfile.delete()) {
            Log.w(TAG, "delete zip file failed! ");
        }
        for (int i = this.zipFileMax - 1; i >= 0; i--) {
            File dataDirectory2 = Environment.getDataDirectory();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(BIGZIPFILEPATH);
            stringBuilder2.append(File.separator);
            stringBuilder2.append(logFileName);
            stringBuilder2.append("-");
            stringBuilder2.append(i);
            stringBuilder2.append(".tar.gz");
            zipfilei = new File(dataDirectory2, stringBuilder2.toString());
            if (zipfilei.exists()) {
                int j = i + 1;
                File dataDirectory3 = Environment.getDataDirectory();
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(BIGZIPFILEPATH);
                stringBuilder3.append(File.separator);
                stringBuilder3.append(logFileName);
                stringBuilder3.append("-");
                stringBuilder3.append(j);
                stringBuilder3.append(".tar.gz");
                if (!zipfilei.renameTo(new File(dataDirectory3, stringBuilder3.toString()))) {
                    Log.w(TAG, "failed rename file! ");
                }
            }
        }
        zipfilei = Environment.getDataDirectory();
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append(BIGZIPFILEPATH);
        stringBuilder4.append(File.separator);
        stringBuilder4.append(logFileName);
        stringBuilder4.append("-0.tar.gz");
        dataDirectory = new File(zipfilei, stringBuilder4.toString());
        if (dataDirectory.exists()) {
            this.hasCreateBigZip = 1;
            return;
        }
        FileOutputStream fOutputStream = null;
        ZipOutputStream out = null;
        FileInputStream in = null;
        try {
            byte[] buf = new byte[1024];
            fOutputStream = new FileOutputStream(dataDirectory);
            out = new ZipOutputStream(fOutputStream);
            File dataDirectory4 = Environment.getDataDirectory();
            StringBuilder stringBuilder5 = new StringBuilder();
            stringBuilder5.append(BIGZIPFILEPATH);
            stringBuilder5.append(File.separator);
            stringBuilder5.append(logFileName);
            File powerinfologtxt = new File(dataDirectory4, stringBuilder5.toString());
            if (!powerinfologtxt.exists()) {
                Log.w(TAG, "No txt file to zip ");
            }
            File dataDirectory5 = Environment.getDataDirectory();
            StringBuilder stringBuilder6 = new StringBuilder();
            stringBuilder6.append(BIGZIPFILEPATH);
            stringBuilder6.append(File.separator);
            stringBuilder6.append(logFileName);
            stringBuilder6.append("-0");
            dataDirectory4 = new File(dataDirectory5, stringBuilder6.toString());
            if (!powerinfologtxt.renameTo(dataDirectory4)) {
                Log.w(TAG, "failed rename file! ");
            }
            in = new FileInputStream(dataDirectory4);
            out.putNextEntry(new ZipEntry(dataDirectory4.getName()));
            int len = 0;
            while (true) {
                int read = in.read(buf);
                len = read;
                if (read <= 0) {
                    break;
                }
                out.write(buf, 0, len);
            }
            out.closeEntry();
            if (!dataDirectory4.delete()) {
                Log.w(TAG, "delete powerLog file failed!");
            }
            dataDirectory.setReadable(true, false);
            this.hasCreateBigZip = 1;
            try {
                in.close();
            } catch (IOException e) {
            }
            try {
                out.close();
            } catch (IOException e2) {
            }
            try {
                fOutputStream.close();
            } catch (IOException e3) {
            }
        } catch (FileNotFoundException e4) {
            Log.e(TAG, "not found file when ZIP file");
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e5) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e6) {
                }
            }
            if (fOutputStream != null) {
                fOutputStream.close();
            }
        } catch (IOException e7) {
            Log.e(TAG, "zip file failed");
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e8) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e9) {
                }
            }
            if (fOutputStream != null) {
                fOutputStream.close();
            }
        } catch (Throwable th) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e10) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e11) {
                }
            }
            if (fOutputStream != null) {
                try {
                    fOutputStream.close();
                } catch (IOException e12) {
                }
            }
        }
    }

    private void initArrayList() {
        int i = 0;
        for (int i2 = 0; i2 < this.WAKEUPSOURCE_NUM; i2++) {
            mLogInfo.mWakeupSources.add(new WakeupSource());
        }
        while (i < this.TOP_PROCESS_NUM) {
            mLogInfo.mCpuTopLoads.add(new CpuTopLoad());
            i++;
        }
    }

    private static void setLogInfo(LogInfo loginfo) {
        mLogInfo = loginfo;
    }

    private static void setReceiver(PowerInfoServiceReceiver receiver) {
        mReceiver = receiver;
    }

    private HwPowerInfoService() {
        this.CPU_FREQ_HEAD = "/sys/devices/system/cpu/cpu";
        this.CPU_FREQ_TAIL = "/cpufreq/scaling_cur_freq";
        this.CPU_MAX_FREQ_TAIL = "/cpufreq/scaling_max_freq";
        this.CPU0_TEMP = "";
        this.CPU1_TEMP = "";
        this.BOARD_TEMP = "";
        this.PA_TEMP = "";
        this.BAT_TEMP = "";
        this.CPU_ONLINE = "/sys/devices/system/cpu/online";
        this.WAKEUPSOURCE = "/sys/kernel/debug/wakeup_sources";
        this.CURRENT = "";
        this.CURRENT_LIMIT = "/sys/class/hw_power/charger/charge_data/iin_thermal";
        this.SOC_RM = "/sys/class/power_supply/Battery/capacity_rm";
        this.WAKELOCK_NUM = 3;
        this.WAKEUPSOURCE_NUM = 3;
        this.TOP_PROCESS_NUM = 3;
        this.timestep = 5000;
        this.zipFileMax = 9;
        this.hasCreateBigZip = 0;
        this.zipStartHour = 0;
        this.zipEndHour = 12;
        this.dateFormate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        this.mLastTopAppName = null;
        this.mLastTopAppUid = 0;
        this.isThreadAlive = true;
        this.mWakeLockNumber = 0;
        this.mSuspendState = false;
        this.isThreadAlive = true;
        this.hasCreateBigZip = 0;
        setLogInfo(new LogInfo());
        parseXML();
        initArrayList();
        this.mCpuTracker = new ProcessCpuTracker(false);
        this.mCpuTracker.init();
        setReceiver(new PowerInfoServiceReceiver());
        this.mWorker = new WorkerThread("PowerInfoService");
        this.mWorker.start();
        Log.i(TAG, "construct the powerinfoservice");
    }

    private static void registerMyReceiver(Context context) {
        mContext = context;
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.HEADSET_PLUG");
        filter.addAction("android.nfc.action.ADAPTER_STATE_CHANGED");
        filter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        filter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
        filter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        filter.addAction("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED");
        filter.addAction("android.media.VOLUME_CHANGED_ACTION");
        mContext.registerReceiver(mReceiver, filter);
        musicValume = ((AudioManager) mContext.getSystemService("audio")).getStreamVolume(3);
    }

    private static void unInstance() {
        if (mReceiver != null) {
            mContext.unregisterReceiver(mReceiver);
            mReceiver = null;
        }
        mSingleInstance = null;
    }

    public static HwPowerInfoService getInstance(Context context, boolean isSystemReady) {
        synchronized (mLockInit) {
            HwPowerInfoService hwPowerInfoService;
            if (isSystemReady) {
                try {
                    if (mSingleInstance == null) {
                        mSingleInstance = new HwPowerInfoService();
                        if (context != null) {
                            registerMyReceiver(context);
                        }
                    } else {
                        hwPowerInfoService = mSingleInstance;
                        return hwPowerInfoService;
                    }
                } finally {
                }
            } else if (!(context == null || mSingleInstance == null)) {
                Log.i(TAG, "unInstance");
                unInstance();
            }
            hwPowerInfoService = mSingleInstance;
            return hwPowerInfoService;
        }
    }

    public void notePowerInfoBrightness(int brightness) {
        if (brightness == 0) {
            this.mLastTopAppName = mLogInfo.mTopAppName;
            this.mLastTopAppUid = mLogInfo.mTopAppUID;
            mLogInfo.mTopAppName = null;
            mLogInfo.mTopAppUID = 0;
        } else if (mLogInfo.mTopAppName == null) {
            mLogInfo.mTopAppName = this.mLastTopAppName;
            mLogInfo.mTopAppUID = this.mLastTopAppUid;
        }
        mLogInfo.mBrightness = brightness;
    }

    public void notePowerInfoBatteryState(int plugType, int level) {
        mLogInfo.mChargeStatus = plugType;
        mLogInfo.mBatteryLevel = level;
    }

    public void notePowerInfoConnectionState(int dataType, boolean hasData) {
        mLogInfo.mConnectionStatus = dataType;
        mLogInfo.mDataConnection = hasData;
    }

    public void notePowerInfoSignalStrength(int strengthLevel) {
        mLogInfo.mSignalStrength = strengthLevel;
    }

    public void noteStartCamera() {
        mLogInfo.mCameraState = 1;
    }

    public void noteStopCamera() {
        mLogInfo.mCameraState = 0;
    }

    public void notePowerInfoWifiState(int supplState) {
        if (1 == supplState) {
            mLogInfo.mWifiStatus = 1;
        } else if (10 == supplState) {
            mLogInfo.mWifiStatus = 2;
        }
    }

    public void notePowerInfoGPSStatus(int status) {
        mLogInfo.mGPSStatus = status;
    }

    public void notePowerInfoTopApp(String packageName, int uid) {
        mLogInfo.mTopAppName = packageName;
        mLogInfo.mTopAppUID = uid;
    }

    public void notePowerInfoAcquireWakeLock(String name, int pid) {
        synchronized (mLock_wakelock) {
            WakeLock wakelock = new WakeLock();
            wakelock.mWakeLockName = name;
            wakelock.mWakeLockPID = pid;
            mLogInfo.mWakeLocks.add(wakelock);
            this.mWakeLockNumber++;
        }
    }

    public void notePowerInfoChangeWakeLock(String name, int pid, String newName, int newPid) {
        notePowerInfoReleaseWakeLock(name, pid);
        notePowerInfoAcquireWakeLock(newName, newPid);
    }

    public void notePowerInfoReleaseWakeLock(String name, int pid) {
        synchronized (mLock_wakelock) {
            for (WakeLock wakelock : mLogInfo.mWakeLocks) {
                if (name.equals(wakelock.mWakeLockName)) {
                    mLogInfo.mWakeLocks.remove(wakelock);
                    break;
                }
            }
            this.mWakeLockNumber--;
        }
    }

    public void notePowerInfoStartAlarm(String name, int uid) {
        if (name.charAt(1) == 'w') {
            name = name.substring(name.indexOf(58) + 1);
            if (mLogInfo.mAlarmName == null) {
                LogInfo logInfo = mLogInfo;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(uid);
                stringBuilder.append("/");
                stringBuilder.append(name);
                logInfo.mAlarmName = stringBuilder.toString();
            } else {
                StringBuilder stringBuilder2 = new StringBuilder();
                LogInfo logInfo2 = mLogInfo;
                stringBuilder2.append(logInfo2.mAlarmName);
                stringBuilder2.append(" | ");
                stringBuilder2.append(uid);
                stringBuilder2.append("/");
                stringBuilder2.append(name);
                logInfo2.mAlarmName = stringBuilder2.toString();
            }
        }
    }

    public void notePowerInfoWakeupReason(String reason) {
        synchronized (mLock) {
            mLogInfo.mWakeupReason = reason;
            mLock.notifyAll();
        }
    }

    public void notePowerInfoSuspendState(boolean enable) {
        this.mSuspendState = enable;
    }

    private void getCpuInfo(LogInfo Data) {
        long now = SystemClock.uptimeMillis();
        this.mCpuTracker.update();
        String[] cpuInfoItem = this.mCpuTracker.printCurrentState(now).split("\n");
        if (cpuInfoItem.length > 5) {
            int indexStart = 0;
            String temp = null;
            for (int i = 1; i < this.TOP_PROCESS_NUM + 1; i++) {
                int indexEnd = cpuInfoItem[i].indexOf("%");
                CpuTopLoad cpuTopLoad = (CpuTopLoad) Data.mCpuTopLoads.get(i - 1);
                cpuTopLoad.mCpuTopLoad = (int) Math.rint((double) Float.valueOf(cpuInfoItem[i].substring(2, indexEnd)).floatValue());
                indexStart = indexEnd + 2;
                indexEnd = cpuInfoItem[i].indexOf("/", indexStart);
                cpuTopLoad = (CpuTopLoad) Data.mCpuTopLoads.get(i - 1);
                cpuTopLoad.mCpuTopPid = Integer.parseInt(cpuInfoItem[i].substring(indexStart, indexEnd));
                indexStart = indexEnd + 1;
                cpuTopLoad = (CpuTopLoad) Data.mCpuTopLoads.get(i - 1);
                cpuTopLoad.mCpuTopName = cpuInfoItem[i].substring(indexStart, cpuInfoItem[i].indexOf(":", indexStart));
            }
            int num = cpuInfoItem.length;
            String temp2 = cpuInfoItem[num - 1].substring(0, cpuInfoItem[num - 1].indexOf("%"));
            if (temp2.contains("-")) {
                temp2 = temp2.replace("-", "");
            }
            Data.mCpuTotalLoad = (int) Math.rint((double) Float.valueOf(temp2).floatValue());
        }
    }

    public void noteShutdown() {
        Log.i(TAG, "System begin to shutdown,write the remainder to file");
        this.isThreadAlive = false;
    }
}
