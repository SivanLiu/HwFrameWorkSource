package com.android.server.hidata.channelqoe;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.IMonitor;
import android.util.wifi.HwHiLog;
import com.android.server.hidata.arbitration.HwArbitrationCommonUtils;
import com.android.server.hidata.arbitration.HwArbitrationManager;
import com.android.server.mtm.iaware.brjob.scheduler.AwareJobSchedulerService;
import com.android.server.rms.iaware.feature.DevSchedFeatureRT;
import com.android.server.wifipro.WifiProCommonUtils;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.net.ssl.SSLContext;

public class HwChannelQoEManager {
    private static final int BYTE_TO_BIT = 8;
    private static final int CELL_SIGNAL_LEVEL_GOOD = 0;
    private static final int CELL_SIGNAL_LEVEL_MODERATE = 1;
    private static final int CELL_SIGNAL_LEVEL_POOR = 2;
    private static final int CHQOE_RTT_GET_TIMES = 3;
    private static final int CHQOE_TIMEOUT = 5500;
    private static final int CHR_CELL_PARAMETERS = 0;
    private static final int CHR_PARAMETERS_ARRAY_MAX_LEN = 2;
    private static final int CHR_WIFI_PARAMETERS = 1;
    private static final String COUNTRY_CODE_CHINA = "460";
    private static final String Cnt = "CNT";
    private static final int DECIMAL_SCALE = 2;
    private static final int DNS_EXCEPTION_WAIT_TIME = 1000;
    private static final int DNS_TIMEOUT = 1000;
    private static final int E909002054 = 909002054;
    private static final int E909009039 = 909009039;
    private static final int E909009040 = 909009040;
    private static final float FLOAT_ZERO = 1.0E-6f;
    private static final int GET_CURRENT_RTT = 0;
    private static final int GET_CURRENT_RTT_SCENCE = -1;
    private static final int GET_CURRENT_RTT_UID = -1;
    private static final String GET_GENERATE = "GET /generate_204 HTTP/1.1\r\n";
    private static final String GET_HTTP = "GET http://";
    private static final int HOST_HICLOUD_LENGTH = 29;
    private static final String HTTP_CONNECTION = "Connection: Keep-Alive\r\n\r\n";
    private static final int HTTP_CONNECT_TIMEOUT = 500;
    private static final String HTTP_HOST = "Host: ";
    private static final String HTTP_PROTOCOL = "/ HTTP/1.1\r\n";
    private static final int HTTP_READ_TIMEOUT = 500;
    private static final boolean IS_CHINA = "CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""));
    private static final int KILOBYTE = 1024;
    private static final String LINE_BREAK = "\r\n";
    private static final String LVl = "LVL";
    private static final int MESSAGE_BAK_RTT = 61444;
    private static final int MESSAGE_DNS_TIMEOUT = 61448;
    private static final int MESSAGE_EXCEPTION = 61440;
    private static final int MESSAGE_EXCEPTION_BAK = 61441;
    private static final int MESSAGE_FIRST_RTT = 61443;
    private static final int MESSAGE_START_QUERY = 61449;
    private static final int MESSAGE_START_QUERY_FOR_THROUGHPUT = 61450;
    private static final int MESSAGE_THROUGHPUT_TEST_FINISHED = 61451;
    private static final int MESSAGE_TIMEOUT = 61442;
    private static final int NOT_GET_CURRENT_RTT = 1;
    private static final int NR_HIGH_LEVEL_RSRQ_THRESHOLD = -15;
    private static final int NR_HIGH_LEVEL_SINR_THRESHOLD = 3;
    private static final int NR_LOW_LEVEL_RSRQ_THRESHOLD = -12;
    private static final int NR_LOW_LEVEL_SINR_THRESHOLD = 11;
    private static final int NR_SIGNAL_HIGH_LEVEL = 2;
    private static final int NR_SIGNAL_LOW_LEVEL = 1;
    private static final String RTT_URL_KEY = "wifi_hidata_rtt_url";
    private static final int SOCKET_CONNECT_TIMEOUT = 4000;
    private static final int SOCKET_INPUT_WAIT_TIME = 500;
    private static final int SOCKET_MAX_TIMEOUT = 999;
    private static final String SPLITTER = "\\|";
    private static final String TAG = "HiDATA_ChannelQoE";
    private static final int TOTAL_URL_NUMBERS = 4;
    private static final float TPT_LEVEL_1 = 0.5f;
    private static final float TPT_LEVEL_2 = 1.0f;
    private static final float TPT_LEVEL_3 = 2.0f;
    private static final float TPT_LEVEL_4 = 3.0f;
    private static final float TPT_LEVEL_5 = 10.0f;
    private static final float TPT_LEVEL_6 = 20.0f;
    private static final float TPT_LEVEL_7 = 30.0f;
    private static final float TPT_LEVEL_8 = 40.0f;
    private static final float TPT_LEVEL_9 = 50.0f;
    private static final int TPUT_TEST_TIMEOUT = 1500;
    private static final float TPUT_THRESHOLD_OF_3G = 2.0f;
    private static final int URL_BAIDU_INDEX = 1;
    private static final int URL_GOOGLE_INDEX = 3;
    private static final int URL_GSTATIC_INDEX = 2;
    private static final int URL_HICLOUD_INDEX = 0;
    private static volatile HwChannelQoEManager mChannelQoEManager;
    private String getBaidu;
    /* access modifiers changed from: private */
    public String getBak;
    /* access modifiers changed from: private */
    public String getFirst;
    private String getGoogle;
    private String getGstatic;
    private String getHiCloud;
    private String hostHiCloud;
    private CHMeasureObject mCell;
    /* access modifiers changed from: private */
    public HistoryMseasureInfo mCellHistoryMseasureInfo = null;
    private HwCHQciManager mChQciManager;
    /* access modifiers changed from: private */
    public HwChannelQoEParmStatistics[] mChannelQoEParm = new HwChannelQoEParmStatistics[2];
    /* access modifiers changed from: private */
    public Context mContext;
    private CurrentSignalState mCurrentSignalState = null;
    /* access modifiers changed from: private */
    public int mGetCurrentRttFlag = 1;
    private long mLastUploadTime;
    private HwChannelQoEMonitor mQoEMonitor;
    private SignalStrength mSignalStrengthSim0 = null;
    private SignalStrength mSignalStrengthSim1 = null;
    private TelephonyManager mTelephonyManager;
    private CHMeasureObject mWifi;
    /* access modifiers changed from: private */
    public HistoryMseasureInfo mWifiHistoryMseasureInfo = null;
    private String urlBaidu;
    /* access modifiers changed from: private */
    public String urlBak;
    /* access modifiers changed from: private */
    public String urlCdn;
    /* access modifiers changed from: private */
    public String urlFirst;
    private String urlGoogle;
    private String urlGstatic;
    private String urlHiCloud;

    public class CHMeasureObject {
        /* access modifiers changed from: private */
        public boolean isBakRunning = false;
        private CopyOnWriteArrayList<HwChannelQoEAppInfo> mCallbackList = new CopyOnWriteArrayList<>();
        /* access modifiers changed from: private */
        public Handler mHandler;
        /* access modifiers changed from: private */
        public int mMutex = 0;
        /* access modifiers changed from: private */
        public String mName;
        /* access modifiers changed from: private */
        public RttRecords mRttRecords = new RttRecords();
        /* access modifiers changed from: private */
        public int netType;

        public CHMeasureObject(String name, int networkType) {
            this.mName = name;
            this.netType = networkType;
            this.mHandler = new Handler(HwChannelQoEManager.this) {
                /* class com.android.server.hidata.channelqoe.HwChannelQoEManager.CHMeasureObject.AnonymousClass1 */

                public void handleMessage(Message msg) {
                    HwChannelQoEManager.log(false, "%{public}s handleMessage what is %{public}d", CHMeasureObject.this.mName, Integer.valueOf(msg.what));
                    switch (msg.what) {
                        case 61440:
                            if (CHMeasureObject.this.isBakRunning) {
                                HwChannelQoEManager.log(false, "Bak is Running. Do not process hicloud exception anymore.", new Object[0]);
                                return;
                            } else if (CHMeasureObject.this.mMutex == 0) {
                                HwChannelQoEManager.log(false, "MESSAGE_EXCEPTION: mMutex is 0, will do nothing.", new Object[0]);
                                return;
                            } else if (CHMeasureObject.this.mRttRecords.hasValues()) {
                                HwChannelQoEManager.log(false, "exception but record has values.", new Object[0]);
                                removeMessages(HwChannelQoEManager.MESSAGE_TIMEOUT);
                                CHMeasureObject cHMeasureObject = CHMeasureObject.this;
                                cHMeasureObject.processAll(cHMeasureObject.mRttRecords.getFinalRtt());
                                return;
                            } else {
                                HwChannelQoEManager.log(false, "exception but record has no value. will start bakprocess", new Object[0]);
                                boolean unused = CHMeasureObject.this.isBakRunning = true;
                                CHMeasureObject cHMeasureObject2 = CHMeasureObject.this;
                                cHMeasureObject2.bakProcess(cHMeasureObject2.netType);
                                return;
                            }
                        case HwChannelQoEManager.MESSAGE_EXCEPTION_BAK /*{ENCODED_INT: 61441}*/:
                            boolean unused2 = CHMeasureObject.this.isBakRunning = false;
                            if (CHMeasureObject.this.mMutex == 0) {
                                HwChannelQoEManager.this.logE(false, "MESSAGE_EXCEPTION_BAK, there is no item in callbacklist.", new Object[0]);
                                return;
                            }
                            removeMessages(HwChannelQoEManager.MESSAGE_TIMEOUT);
                            CHMeasureObject.this.processAll(999);
                            return;
                        case HwChannelQoEManager.MESSAGE_TIMEOUT /*{ENCODED_INT: 61442}*/:
                            boolean unused3 = CHMeasureObject.this.isBakRunning = false;
                            if (CHMeasureObject.this.mMutex == 0) {
                                HwChannelQoEManager.log(false, "MESSAGE_TIMEOUT: mMutex is 0, will do nothing.", new Object[0]);
                                return;
                            } else if (CHMeasureObject.this.mRttRecords.hasValues()) {
                                HwChannelQoEManager.log(false, "timeout but record has values.", new Object[0]);
                                CHMeasureObject cHMeasureObject3 = CHMeasureObject.this;
                                cHMeasureObject3.processAll(cHMeasureObject3.mRttRecords.getFinalRtt());
                                return;
                            } else {
                                HwChannelQoEManager.log(false, "timeout but record has no value.", new Object[0]);
                                CHMeasureObject.this.processAll(999);
                                return;
                            }
                        case HwChannelQoEManager.MESSAGE_FIRST_RTT /*{ENCODED_INT: 61443}*/:
                            if (CHMeasureObject.this.isBakRunning) {
                                HwChannelQoEManager.log(false, "Bak is Running. Do not process hicloud anymore.", new Object[0]);
                                return;
                            } else if (CHMeasureObject.this.mMutex == 0) {
                                HwChannelQoEManager.log(false, "MESSAGE_FIRST_RTT: mMutex is 0, do nothing.", new Object[0]);
                                return;
                            } else {
                                CHMeasureObject.this.mRttRecords.insert((long) msg.arg1);
                                if (CHMeasureObject.this.mRttRecords.isReady()) {
                                    HwChannelQoEManager.log(false, "all records are ready.", new Object[0]);
                                    removeMessages(HwChannelQoEManager.MESSAGE_TIMEOUT);
                                    CHMeasureObject cHMeasureObject4 = CHMeasureObject.this;
                                    cHMeasureObject4.processAll(cHMeasureObject4.mRttRecords.getFinalRtt());
                                    return;
                                }
                                return;
                            }
                        case HwChannelQoEManager.MESSAGE_BAK_RTT /*{ENCODED_INT: 61444}*/:
                            boolean unused4 = CHMeasureObject.this.isBakRunning = false;
                            if (CHMeasureObject.this.mMutex == 0) {
                                HwChannelQoEManager.this.logE(false, "MESSAGE_BAK_RTT, there is no item in callbacklist.", new Object[0]);
                                return;
                            }
                            removeMessages(HwChannelQoEManager.MESSAGE_TIMEOUT);
                            CHMeasureObject.this.processAll((long) msg.arg1);
                            return;
                        case 61445:
                        case 61446:
                        case 61447:
                        default:
                            HwChannelQoEManager.log(false, "handler receive unknown message %{public}d", Integer.valueOf(msg.what));
                            return;
                        case HwChannelQoEManager.MESSAGE_DNS_TIMEOUT /*{ENCODED_INT: 61448}*/:
                            HwChannelQoEManager.log(false, "MESSAGE_DNS_TIMEOUT with netType %{public}d", String.valueOf(CHMeasureObject.this.netType));
                            if (801 == CHMeasureObject.this.netType) {
                                int[] iArr = HwChannelQoEManager.this.mChannelQoEParm[0].mRst.mRst;
                                iArr[3] = iArr[3] + 1;
                            } else if (800 == CHMeasureObject.this.netType) {
                                int[] iArr2 = HwChannelQoEManager.this.mChannelQoEParm[1].mRst.mRst;
                                iArr2[3] = iArr2[3] + 1;
                            }
                            boolean unused5 = CHMeasureObject.this.isBakRunning = true;
                            CHMeasureObject cHMeasureObject5 = CHMeasureObject.this;
                            cHMeasureObject5.bakProcess(cHMeasureObject5.netType);
                            return;
                        case HwChannelQoEManager.MESSAGE_START_QUERY /*{ENCODED_INT: 61449}*/:
                            HwChannelQoEManager.log(false, "MESSAGE_START_QUERY enter.", new Object[0]);
                            HwChannelQoEAppInfo appQoeInfo = (HwChannelQoEAppInfo) msg.obj;
                            if (appQoeInfo == null) {
                                HwArbitrationCommonUtils.logE(HwChannelQoEManager.TAG, false, "appQoeInfo is null, just break.", new Object[0]);
                                return;
                            } else {
                                CHMeasureObject.this.queryQuality(appQoeInfo);
                                return;
                            }
                        case HwChannelQoEManager.MESSAGE_START_QUERY_FOR_THROUGHPUT /*{ENCODED_INT: 61450}*/:
                            HwChannelQoEAppInfo appQoeInfo2 = (HwChannelQoEAppInfo) msg.obj;
                            if (appQoeInfo2 != null) {
                                CHMeasureObject.this.detectThroughput(appQoeInfo2);
                                return;
                            }
                            return;
                        case HwChannelQoEManager.MESSAGE_THROUGHPUT_TEST_FINISHED /*{ENCODED_INT: 61451}*/:
                            if (!(msg.obj instanceof Float)) {
                                HwArbitrationCommonUtils.logE(HwChannelQoEManager.TAG, false, "object of message is not Float, break.", new Object[0]);
                                return;
                            }
                            float finalTputResult = ((Float) msg.obj).floatValue();
                            if (finalTputResult <= 0.0f) {
                                if (CHMeasureObject.this.netType == 801) {
                                    int[] iArr3 = HwChannelQoEManager.this.mChannelQoEParm[0].mRst.mRst;
                                    iArr3[6] = iArr3[6] + 1;
                                } else if (CHMeasureObject.this.netType == 800) {
                                    int[] iArr4 = HwChannelQoEManager.this.mChannelQoEParm[1].mRst.mRst;
                                    iArr4[6] = iArr4[6] + 1;
                                }
                            }
                            CHMeasureObject.this.processTputResult(finalTputResult);
                            return;
                    }
                }
            };
        }

        private void statisticsRttScope(int mChannelQoEParmIndex, int rtt) {
            HwChannelQoEManager.log(false, "enter statisticsRttScope: rtt is " + String.valueOf(rtt), new Object[0]);
            if (mChannelQoEParmIndex >= 2) {
                HwChannelQoEManager.log(false, "statisticsRttScope index err.", new Object[0]);
            } else if (rtt <= 0) {
                HwChannelQoEManager.log(false, "statisticsRttScope: rtt <= 0", new Object[0]);
            } else if (rtt < 50) {
                int[] iArr = HwChannelQoEManager.this.mChannelQoEParm[mChannelQoEParmIndex].mRtt.mRtt;
                iArr[0] = iArr[0] + 1;
            } else if (rtt < 100) {
                int[] iArr2 = HwChannelQoEManager.this.mChannelQoEParm[mChannelQoEParmIndex].mRtt.mRtt;
                iArr2[1] = iArr2[1] + 1;
            } else if (rtt < 150) {
                int[] iArr3 = HwChannelQoEManager.this.mChannelQoEParm[mChannelQoEParmIndex].mRtt.mRtt;
                iArr3[2] = iArr3[2] + 1;
            } else if (rtt < 200) {
                int[] iArr4 = HwChannelQoEManager.this.mChannelQoEParm[mChannelQoEParmIndex].mRtt.mRtt;
                iArr4[3] = iArr4[3] + 1;
            } else if (rtt < 250) {
                int[] iArr5 = HwChannelQoEManager.this.mChannelQoEParm[mChannelQoEParmIndex].mRtt.mRtt;
                iArr5[4] = iArr5[4] + 1;
            } else if (rtt < 300) {
                int[] iArr6 = HwChannelQoEManager.this.mChannelQoEParm[mChannelQoEParmIndex].mRtt.mRtt;
                iArr6[5] = iArr6[5] + 1;
            } else if (rtt < 350) {
                int[] iArr7 = HwChannelQoEManager.this.mChannelQoEParm[mChannelQoEParmIndex].mRtt.mRtt;
                iArr7[6] = iArr7[6] + 1;
            } else if (rtt < 400) {
                int[] iArr8 = HwChannelQoEManager.this.mChannelQoEParm[mChannelQoEParmIndex].mRtt.mRtt;
                iArr8[7] = iArr8[7] + 1;
            } else if (rtt < 500) {
                int[] iArr9 = HwChannelQoEManager.this.mChannelQoEParm[mChannelQoEParmIndex].mRtt.mRtt;
                iArr9[8] = iArr9[8] + 1;
            } else {
                int[] iArr10 = HwChannelQoEManager.this.mChannelQoEParm[mChannelQoEParmIndex].mRtt.mRtt;
                iArr10[9] = iArr10[9] + 1;
            }
        }

        private void calculateTputScope(int channelQoEParamIndex, float finalTputResult) {
            HwArbitrationCommonUtils.logD(HwChannelQoEManager.TAG, false, "enter calculateTputScope: tpt is " + String.valueOf(finalTputResult), new Object[0]);
            if (channelQoEParamIndex >= 2) {
                HwArbitrationCommonUtils.logE(HwChannelQoEManager.TAG, false, "calculateTputScope index error.", new Object[0]);
            } else if (finalTputResult <= 0.0f) {
                HwArbitrationCommonUtils.logE(HwChannelQoEManager.TAG, false, "calculateTputScope: tput <= 0", new Object[0]);
            } else if (finalTputResult - 0.5f < HwChannelQoEManager.FLOAT_ZERO) {
                int[] iArr = HwChannelQoEManager.this.mChannelQoEParm[channelQoEParamIndex].mTput.mTput;
                iArr[0] = iArr[0] + 1;
            } else if (finalTputResult - 1.0f < HwChannelQoEManager.FLOAT_ZERO) {
                int[] iArr2 = HwChannelQoEManager.this.mChannelQoEParm[channelQoEParamIndex].mTput.mTput;
                iArr2[1] = iArr2[1] + 1;
            } else if (finalTputResult - 2.0f < HwChannelQoEManager.FLOAT_ZERO) {
                int[] iArr3 = HwChannelQoEManager.this.mChannelQoEParm[channelQoEParamIndex].mTput.mTput;
                iArr3[2] = iArr3[2] + 1;
            } else if (finalTputResult - 3.0f < HwChannelQoEManager.FLOAT_ZERO) {
                int[] iArr4 = HwChannelQoEManager.this.mChannelQoEParm[channelQoEParamIndex].mTput.mTput;
                iArr4[3] = iArr4[3] + 1;
            } else if (finalTputResult - HwChannelQoEManager.TPT_LEVEL_5 < HwChannelQoEManager.FLOAT_ZERO) {
                int[] iArr5 = HwChannelQoEManager.this.mChannelQoEParm[channelQoEParamIndex].mTput.mTput;
                iArr5[4] = iArr5[4] + 1;
            } else if (finalTputResult - HwChannelQoEManager.TPT_LEVEL_6 < HwChannelQoEManager.FLOAT_ZERO) {
                int[] iArr6 = HwChannelQoEManager.this.mChannelQoEParm[channelQoEParamIndex].mTput.mTput;
                iArr6[5] = iArr6[5] + 1;
            } else if (finalTputResult - HwChannelQoEManager.TPT_LEVEL_7 < HwChannelQoEManager.FLOAT_ZERO) {
                int[] iArr7 = HwChannelQoEManager.this.mChannelQoEParm[channelQoEParamIndex].mTput.mTput;
                iArr7[6] = iArr7[6] + 1;
            } else if (finalTputResult - HwChannelQoEManager.TPT_LEVEL_8 < HwChannelQoEManager.FLOAT_ZERO) {
                int[] iArr8 = HwChannelQoEManager.this.mChannelQoEParm[channelQoEParamIndex].mTput.mTput;
                iArr8[7] = iArr8[7] + 1;
            } else if (finalTputResult - HwChannelQoEManager.TPT_LEVEL_9 < HwChannelQoEManager.FLOAT_ZERO) {
                int[] iArr9 = HwChannelQoEManager.this.mChannelQoEParm[channelQoEParamIndex].mTput.mTput;
                iArr9[8] = iArr9[8] + 1;
            } else {
                int[] iArr10 = HwChannelQoEManager.this.mChannelQoEParm[channelQoEParamIndex].mTput.mTput;
                iArr10[9] = iArr10[9] + 1;
            }
        }

        /* access modifiers changed from: private */
        public void processTputResult(float finalTputResult) {
            HwArbitrationCommonUtils.logD(HwChannelQoEManager.TAG, false, "Enter processTputResult.", new Object[0]);
            calculateTputScope(1, finalTputResult);
            int channelQoeResult = 0;
            Iterator<HwChannelQoEAppInfo> it = this.mCallbackList.iterator();
            while (it.hasNext()) {
                HwChannelQoEAppInfo appQoeInfo = it.next();
                if (HwCHQciManager.getInstance().getChQciConfig(appQoeInfo.mQci).mTput - finalTputResult > HwChannelQoEManager.FLOAT_ZERO) {
                    HwArbitrationCommonUtils.logD(HwChannelQoEManager.TAG, false, "Tested throughput is lower than the threshold, set lable to BAD", new Object[0]);
                    channelQoeResult = 1;
                    int[] iArr = HwChannelQoEManager.this.mChannelQoEParm[1].mRst.mRst;
                    iArr[5] = iArr[5] + 1;
                } else {
                    int[] iArr2 = HwChannelQoEManager.this.mChannelQoEParm[1].mRst.mRst;
                    iArr2[0] = iArr2[0] + 1;
                }
                appQoeInfo.callback.onChannelQuality(appQoeInfo.mUID, appQoeInfo.mScence, appQoeInfo.mNetwork, channelQoeResult);
            }
            this.mCallbackList.clear();
        }

        /* access modifiers changed from: private */
        public void processAll(long finalRtt) {
            HwChannelQoEManager hwChannelQoEManager = HwChannelQoEManager.this;
            hwChannelQoEManager.logE(false, "processAll enter. mCallbackList size is " + String.valueOf(this.mCallbackList.size()), new Object[0]);
            HwChannelQoEManager hwChannelQoEManager2 = HwChannelQoEManager.this;
            hwChannelQoEManager2.logE(false, "finalRtt is " + String.valueOf(finalRtt), new Object[0]);
            HwChannelQoEManager.log(false, "processAll():clt CHR Param statistics,networkType is %{public}d", Integer.valueOf(this.netType));
            int i = this.netType;
            if (801 == i) {
                statisticsRttScope(0, (int) finalRtt);
                int unused = HwChannelQoEManager.this.mCellHistoryMseasureInfo.rttBef = (int) finalRtt;
            } else if (800 == i) {
                statisticsRttScope(1, (int) finalRtt);
                int unused2 = HwChannelQoEManager.this.mWifiHistoryMseasureInfo.rttBef = (int) finalRtt;
            }
            Iterator<HwChannelQoEAppInfo> it = this.mCallbackList.iterator();
            while (it.hasNext()) {
                HwChannelQoEAppInfo appqoeInfo = it.next();
                if (HwCHQciManager.getInstance().getChQciConfig(appqoeInfo.mQci).mRtt == 0) {
                    int i2 = this.netType;
                    if (801 == i2) {
                        int[] iArr = HwChannelQoEManager.this.mChannelQoEParm[0].mRst.mRst;
                        iArr[4] = iArr[4] + 1;
                    } else if (800 == i2) {
                        int[] iArr2 = HwChannelQoEManager.this.mChannelQoEParm[1].mRst.mRst;
                        iArr2[4] = iArr2[4] + 1;
                    }
                } else if (((long) HwCHQciManager.getInstance().getChQciConfig(appqoeInfo.mQci).mRtt) <= finalRtt) {
                    int i3 = this.netType;
                    if (801 == i3) {
                        int[] iArr3 = HwChannelQoEManager.this.mChannelQoEParm[0].mRst.mRst;
                        iArr3[2] = iArr3[2] + 1;
                    } else if (800 == i3) {
                        int[] iArr4 = HwChannelQoEManager.this.mChannelQoEParm[1].mRst.mRst;
                        iArr4[2] = iArr4[2] + 1;
                    }
                    appqoeInfo.callback.onChannelQuality(appqoeInfo.mUID, appqoeInfo.mScence, appqoeInfo.mNetwork, 1);
                    this.mCallbackList.remove(appqoeInfo);
                } else if (Math.abs(HwCHQciManager.getInstance().getChQciConfig(appqoeInfo.mQci).mTput) <= HwChannelQoEManager.FLOAT_ZERO || !HwChannelQoEManager.this.isChina() || !appqoeInfo.needTputTest) {
                    int i4 = this.netType;
                    if (801 == i4) {
                        int[] iArr5 = HwChannelQoEManager.this.mChannelQoEParm[0].mRst.mRst;
                        iArr5[0] = iArr5[0] + 1;
                    } else if (800 == i4) {
                        int[] iArr6 = HwChannelQoEManager.this.mChannelQoEParm[1].mRst.mRst;
                        iArr6[0] = iArr6[0] + 1;
                    }
                    appqoeInfo.callback.onChannelQuality(appqoeInfo.mUID, appqoeInfo.mScence, appqoeInfo.mNetwork, 0);
                    this.mCallbackList.remove(appqoeInfo);
                } else {
                    Message msg = Message.obtain();
                    msg.what = HwChannelQoEManager.MESSAGE_START_QUERY_FOR_THROUGHPUT;
                    msg.obj = appqoeInfo;
                    this.mHandler.sendMessage(msg);
                }
                if (-1 == appqoeInfo.mUID && HwChannelQoEManager.this.mGetCurrentRttFlag == 0) {
                    HwChannelQoEManager.log(false, "processAll(): get current rtt, rtt is %{public}s", "" + HwCHQciManager.getInstance().getChQciConfig(appqoeInfo.mQci).mRtt);
                    appqoeInfo.callback.onCurrentRtt((int) finalRtt);
                    int unused3 = HwChannelQoEManager.this.mGetCurrentRttFlag = 1;
                    this.mCallbackList.remove(appqoeInfo);
                }
            }
            this.mMutex = 0;
            this.mRttRecords.reset();
        }

        /* access modifiers changed from: private */
        public void queryQuality(HwChannelQoEAppInfo appqoeInfo) {
            HwChannelQoEManager.log(false, "%{public}s queryQuality.", this.mName);
            this.mCallbackList.add(appqoeInfo);
            int i = this.mMutex;
            if (i > 0) {
                HwChannelQoEManager.this.logE(false, "There is another RTT processing now.", new Object[0]);
                return;
            }
            this.mMutex = i + 1;
            firstRttProcess(appqoeInfo.mNetwork);
            Message msg = Message.obtain();
            msg.what = HwChannelQoEManager.MESSAGE_TIMEOUT;
            this.mHandler.sendMessageDelayed(msg, 5500);
            HwChannelQoEManager.log(false, "%{public}s send timer MESSAGE_TIMEOUT.", this.mName);
        }

        /* access modifiers changed from: private */
        public void detectThroughput(HwChannelQoEAppInfo appQoeInfo) {
            HwArbitrationCommonUtils.logD(HwChannelQoEManager.TAG, false, "%{public}s detectThroughput.", this.mName);
            startTputTestThread(appQoeInfo.mNetwork);
        }

        private void firstRttProcess(int networkType) {
            HwChannelQoEManager.log(false, "firstRttProcess enter.", new Object[0]);
            HwChannelQoEManager hwChannelQoEManager = HwChannelQoEManager.this;
            new CHFirstThread(networkType, this.mHandler, hwChannelQoEManager.mContext).start();
        }

        private void startTputTestThread(int networkType) {
            HwArbitrationCommonUtils.logD(HwChannelQoEManager.TAG, false, "startTputTestThread enter.", new Object[0]);
            HwChannelQoEManager hwChannelQoEManager = HwChannelQoEManager.this;
            new TputTestThread(networkType, this.mHandler, hwChannelQoEManager.mContext).start();
        }

        /* access modifiers changed from: private */
        public void bakProcess(int networkType) {
            HwChannelQoEManager.log(false, "bakProcess enter.", new Object[0]);
            HwChannelQoEManager hwChannelQoEManager = HwChannelQoEManager.this;
            new CHBakThread(networkType, this.mHandler, hwChannelQoEManager.mContext).start();
        }
    }

    public static class HistoryMseasureInfo {
        /* access modifiers changed from: private */
        public int rat = -1;
        /* access modifiers changed from: private */
        public int rttBef = 0;
        /* access modifiers changed from: private */
        public int sigLoad = 255;
        /* access modifiers changed from: private */
        public int sigPwr = 255;
        /* access modifiers changed from: private */
        public int sigQual = 255;
        /* access modifiers changed from: private */
        public int sigSnr = 255;
        /* access modifiers changed from: private */
        public int tupBef = 0;

        public String toString() {
            return "tupBef:" + String.valueOf(this.tupBef) + ",rttBef:" + String.valueOf(this.rttBef) + ",sigPwr:" + String.valueOf(this.sigPwr) + ",sigSnr:" + String.valueOf(this.sigSnr) + ",sigQual:" + String.valueOf(this.sigQual) + ",sigLoad:" + String.valueOf(this.sigLoad) + ",rat:" + String.valueOf(this.rat);
        }

        public void reset() {
            this.tupBef = 0;
            this.rttBef = 0;
            this.sigPwr = 255;
            this.sigSnr = 255;
            this.sigQual = 255;
            this.sigLoad = 255;
            this.rat = -1;
        }

        public int getRttBef() {
            return this.rttBef;
        }

        public int getTupBef() {
            return this.tupBef;
        }

        public int getPwr() {
            return this.sigPwr;
        }

        public int getSnr() {
            return this.sigSnr;
        }

        public int getQual() {
            return this.sigQual;
        }

        public int getLoad() {
            return this.sigLoad;
        }

        public int getRat() {
            return this.rat;
        }
    }

    public static class CurrentSignalState {
        /* access modifiers changed from: private */
        public int networkType = 0;
        /* access modifiers changed from: private */
        public int sigLoad;
        /* access modifiers changed from: private */
        public int sigPwr;
        /* access modifiers changed from: private */
        public int sigQual;
        /* access modifiers changed from: private */
        public int sigSnr;

        public int getNetwork() {
            return this.networkType;
        }

        public int getSigPwr() {
            return this.sigPwr;
        }

        public int getSigSnr() {
            return this.sigSnr;
        }

        public int getSigQual() {
            return this.sigQual;
        }

        public int getSigLoad() {
            return this.sigLoad;
        }
    }

    public static HwChannelQoEManager createInstance(Context context) {
        if (mChannelQoEManager == null) {
            synchronized (HwChannelQoEManager.class) {
                if (mChannelQoEManager == null) {
                    log(false, "createInstance enter.", new Object[0]);
                    mChannelQoEManager = new HwChannelQoEManager(context);
                }
            }
        }
        return mChannelQoEManager;
    }

    public static HwChannelQoEManager getInstance() {
        return mChannelQoEManager;
    }

    private HwChannelQoEManager(Context context) {
        this.mChannelQoEParm[0] = new HwChannelQoEParmStatistics();
        this.mChannelQoEParm[1] = new HwChannelQoEParmStatistics();
        this.mLastUploadTime = 0;
        this.urlHiCloud = "";
        this.urlGstatic = "";
        this.urlBaidu = "";
        this.urlGoogle = "";
        this.urlCdn = "";
        this.hostHiCloud = "";
        this.getHiCloud = "";
        this.getGstatic = "";
        this.getBaidu = "";
        this.getGoogle = "";
        this.urlFirst = "";
        this.getFirst = "";
        this.urlBak = "";
        this.getBak = "";
        logE(false, "HwChannelQoEManager enter.", new Object[0]);
        this.mContext = context;
        this.mCell = new CHMeasureObject("mCell", 801);
        this.mWifi = new CHMeasureObject("mWifi", 800);
        this.mQoEMonitor = new HwChannelQoEMonitor(this.mContext);
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        this.mCurrentSignalState = new CurrentSignalState();
        this.mCellHistoryMseasureInfo = new HistoryMseasureInfo();
        this.mWifiHistoryMseasureInfo = new HistoryMseasureInfo();
        HwChannelQoEParmStatistics[] hwChannelQoEParmStatisticsArr = this.mChannelQoEParm;
        hwChannelQoEParmStatisticsArr[0].mNetworkType = 801;
        hwChannelQoEParmStatisticsArr[1].mNetworkType = 800;
        this.mChQciManager = HwCHQciManager.getInstance();
        String rttUrl = Settings.Global.getString(context.getContentResolver(), RTT_URL_KEY);
        if (!TextUtils.isEmpty(rttUrl)) {
            String[] urls = rttUrl.split("\\|");
            if (urls.length == 4) {
                this.urlHiCloud = urls[0];
                this.urlGstatic = urls[2];
                this.urlBaidu = urls[1];
                this.urlGoogle = urls[3];
                if (this.urlHiCloud.length() >= 29) {
                    this.hostHiCloud = this.urlHiCloud.substring(0, 29);
                    this.getHiCloud = "GET /generate_204 HTTP/1.1\r\nHost: " + this.hostHiCloud + LINE_BREAK + HTTP_CONNECTION;
                } else {
                    logE(false, "HwChannelQoEManager Hidata hicloud url length less than HOST_HICLOUD_LENGTH", new Object[0]);
                }
                this.getGstatic = "GET /generate_204 HTTP/1.1\r\nHost: " + this.urlGstatic + LINE_BREAK + HTTP_CONNECTION;
                this.getBaidu = GET_HTTP + this.urlBaidu + HTTP_PROTOCOL + HTTP_HOST + this.urlBaidu + LINE_BREAK + HTTP_CONNECTION;
                this.getGoogle = GET_HTTP + this.urlGoogle + HTTP_PROTOCOL + HTTP_HOST + this.urlGoogle + LINE_BREAK + HTTP_CONNECTION;
                this.urlFirst = this.urlHiCloud;
                this.getFirst = this.getHiCloud;
                this.urlBak = this.urlBaidu;
                this.getBak = this.getBaidu;
            } else {
                logE(false, "HwChannelQoEManager Hidata total rtt urls less than TOTAL_URL_NUMBERS", new Object[0]);
            }
        } else {
            logE(false, "HwChannelQoEManager Hidata rtt url read error", new Object[0]);
        }
        HwArbitrationCommonUtils.logI(TAG, false, "HwChannelQoEManager create succ.", new Object[0]);
    }

    /* access modifiers changed from: private */
    public void logE(boolean isFmtStrPrivate, String info, Object... args) {
        HwHiLog.e(TAG, isFmtStrPrivate, info, args);
    }

    public static void log(boolean isFmtStrPrivate, String info, Object... args) {
        HwHiLog.e(TAG, isFmtStrPrivate, info, args);
    }

    public void startWifiLinkMonitor(int uid, int scence, int qci, boolean needTputTest, IChannelQoECallback callback) {
        HwArbitrationCommonUtils.logD(TAG, false, "startWifiLinkMonitor enter, uid: %{public}d, scence: %{public}d, qci: %{public}d, needTputTest: %{public}s", Integer.valueOf(uid), Integer.valueOf(scence), Integer.valueOf(qci), String.valueOf(needTputTest));
        this.mQoEMonitor.startMonitor(new HwChannelQoEAppInfo(uid, scence, 800, qci, callback), needTputTest);
    }

    public void stopWifiLinkMonitor(int uid, boolean stopAll) {
        if (stopAll) {
            log(false, "stopWifiLinkMonitor stop all.", new Object[0]);
            this.mQoEMonitor.stopAll();
        } else if (uid == -1) {
            log(false, "stopWifiLinkMonitor invalid uid.", new Object[0]);
        } else {
            this.mQoEMonitor.stopMonitor(uid);
        }
    }

    public void queryChannelQuality(int uid, int networkType, int qci, boolean needTputTest, IChannelQoECallback callback) {
        HwArbitrationCommonUtils.logD(TAG, false, "queryChannelQuality enter, uid:%{public}d, networkType:%{public}d, qci:%{public}d, needTputTest:%{public}s", Integer.valueOf(uid), Integer.valueOf(networkType), Integer.valueOf(qci), String.valueOf(needTputTest));
        HwCHQciConfig config = HwCHQciManager.getInstance().getChQciConfig(qci);
        try {
            if (IS_CHINA) {
                if (isChina()) {
                    this.urlFirst = this.urlHiCloud;
                    this.getFirst = this.getHiCloud;
                    this.urlBak = this.urlBaidu;
                    this.getBak = this.getBaidu;
                    log(false, "In China, using hicloud and baidu.", new Object[0]);
                } else {
                    this.urlFirst = this.urlGstatic;
                    this.getFirst = this.getGstatic;
                    this.urlBak = this.urlGoogle;
                    this.getBak = this.getGoogle;
                    log(false, "over sea, using gstatic and google.", new Object[0]);
                }
            }
        } catch (Exception e) {
            log(false, "set url exception", new Object[0]);
        }
        if (801 == networkType) {
            this.mCellHistoryMseasureInfo.reset();
            int unused = this.mCellHistoryMseasureInfo.tupBef = -1;
            int signal_level = queryCellSignalLevel(config.mTput);
            if (signal_level == 0) {
                logE(false, "queryChannelQuality signal level good.", new Object[0]);
                Message startMessage = Message.obtain();
                startMessage.what = MESSAGE_START_QUERY;
                HwChannelQoEAppInfo appInfo = new HwChannelQoEAppInfo(uid, -1, networkType, qci, callback);
                appInfo.needTputTest = needTputTest;
                startMessage.obj = appInfo;
                this.mCell.mHandler.sendMessage(startMessage);
            } else if (signal_level == 1) {
                logE(false, "queryChannelQuality signal level MODERATE.", new Object[0]);
                Message startMessage2 = Message.obtain();
                startMessage2.what = MESSAGE_START_QUERY;
                HwChannelQoEAppInfo appInfo2 = new HwChannelQoEAppInfo(uid, -1, networkType, qci, callback);
                appInfo2.needTputTest = needTputTest;
                startMessage2.obj = appInfo2;
                this.mCell.mHandler.sendMessage(startMessage2);
            } else {
                logE(false, "queryChannelQuality signal level bad.", new Object[0]);
                int[] iArr = this.mChannelQoEParm[0].mRst.mRst;
                iArr[1] = iArr[1] + 1;
                callback.onChannelQuality(uid, -1, networkType, 1);
            }
        } else if (800 == networkType) {
            this.mWifiHistoryMseasureInfo.reset();
            WifiInfo info = ((WifiManager) this.mContext.getSystemService(DevSchedFeatureRT.WIFI_FEATURE)).getConnectionInfo();
            if (info != null) {
                int unused2 = this.mWifiHistoryMseasureInfo.sigPwr = info.getRssi();
                int unused3 = this.mWifiHistoryMseasureInfo.sigSnr = info.getSnr();
                int unused4 = this.mWifiHistoryMseasureInfo.sigQual = info.getNoise();
                int unused5 = this.mWifiHistoryMseasureInfo.sigLoad = info.getChload();
                int unused6 = this.mWifiHistoryMseasureInfo.tupBef = -1;
            }
            Message startMessage3 = Message.obtain();
            startMessage3.what = MESSAGE_START_QUERY;
            HwChannelQoEAppInfo appInfo3 = new HwChannelQoEAppInfo(uid, -1, networkType, qci, callback);
            appInfo3.needTputTest = needTputTest;
            startMessage3.obj = appInfo3;
            this.mWifi.mHandler.sendMessage(startMessage3);
        } else {
            logE(false, "queryChannelQuality networkType error", new Object[0]);
        }
    }

    /* access modifiers changed from: private */
    public boolean isChina() {
        String operator = TelephonyManager.getDefault().getNetworkOperator();
        return (operator == null || operator.length() == 0 || !operator.startsWith("460")) ? false : true;
    }

    /* JADX WARNING: Removed duplicated region for block: B:28:0x0096  */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x009e  */
    public int queryCellSignalLevel(float tputThreshold) {
        int signalLevel = 2;
        int subId = SubscriptionManager.getDefaultDataSubscriptionId();
        int netType = this.mTelephonyManager.getNetworkType(subId);
        int netClass = TelephonyManager.getNetworkClass(netType);
        HwArbitrationManager hwArbitrationMgr = HwArbitrationManager.getInstance();
        SignalStrength signalStrength = null;
        if (hwArbitrationMgr != null) {
            signalStrength = hwArbitrationMgr.getSignalStrength(subId);
        }
        if (signalStrength == null) {
            return 2;
        }
        if (netClass == 1) {
            log(false, "CellSignalLevel 2G", new Object[0]);
            signalLevel = 2;
        } else if (netClass == 2) {
            int unused = this.mCellHistoryMseasureInfo.rat = 2;
            if (tputThreshold - 2.0f > FLOAT_ZERO) {
                log(false, "CellSignalLevel 3G", new Object[0]);
                signalLevel = 2;
            }
            int ecio = -1;
            if (netType != 3) {
                if (netType == 4) {
                    ecio = signalStrength.getCdmaEcio();
                } else if (netType == 5 || netType == 6) {
                    ecio = signalStrength.getEvdoEcio();
                } else if (netType != 15) {
                    switch (netType) {
                    }
                }
                log(false, "CellSignalLevel 3G = %{public}d ecio = %{public}d", Integer.valueOf(signalStrength.getLevel()), Integer.valueOf(ecio));
                int unused2 = this.mCellHistoryMseasureInfo.sigQual = ecio;
                int unused3 = this.mCellHistoryMseasureInfo.sigPwr = signalStrength.getWcdmaRscp();
                if (ecio != -1) {
                    if (signalStrength.getLevel() >= 3) {
                        signalLevel = 0;
                    }
                } else if (signalStrength.getLevel() >= 2 && ecio > -12) {
                    signalLevel = 1;
                }
            }
            ecio = signalStrength.getWcdmaEcio();
            log(false, "CellSignalLevel 3G = %{public}d ecio = %{public}d", Integer.valueOf(signalStrength.getLevel()), Integer.valueOf(ecio));
            int unused4 = this.mCellHistoryMseasureInfo.sigQual = ecio;
            int unused5 = this.mCellHistoryMseasureInfo.sigPwr = signalStrength.getWcdmaRscp();
            if (ecio != -1) {
            }
        } else if (netClass == 3) {
            int unused6 = this.mCellHistoryMseasureInfo.rat = 1;
            int rsrq = signalStrength.getLteRsrq();
            int sinr = signalStrength.getLteRssnr();
            int unused7 = this.mCellHistoryMseasureInfo.sigPwr = signalStrength.getLteRsrp();
            int unused8 = this.mCellHistoryMseasureInfo.sigSnr = sinr;
            int unused9 = this.mCellHistoryMseasureInfo.sigQual = rsrq;
            log(false, "CellSignalLevel LTE = %{public}d rsrq = %{public}d sinr = %{public}d", Integer.valueOf(signalStrength.getLevel()), Integer.valueOf(rsrq), Integer.valueOf(sinr));
            if (signalStrength.getLevel() >= 2) {
                if (rsrq <= -15 || sinr <= 3) {
                    signalLevel = 1;
                } else {
                    signalLevel = 0;
                }
            } else if (signalStrength.getLevel() != 1 || rsrq <= -12 || sinr <= 11) {
                signalLevel = 2;
            } else {
                signalLevel = 1;
            }
        } else if (netClass == 4) {
            int unused10 = this.mCellHistoryMseasureInfo.rat = 3;
            int nrRsrq = signalStrength.getNrRsrq();
            int nrSinr = signalStrength.getNrRssnr();
            int unused11 = this.mCellHistoryMseasureInfo.sigPwr = signalStrength.getNrRsrp();
            int unused12 = this.mCellHistoryMseasureInfo.sigSnr = nrSinr;
            int unused13 = this.mCellHistoryMseasureInfo.sigQual = nrRsrq;
            log(false, "CellSignalLevel SA = %{public}d nrRsrq = %{public}d nrSinr = %{public}d", Integer.valueOf(signalStrength.getLevel()), Integer.valueOf(nrRsrq), Integer.valueOf(nrSinr));
            if (signalStrength.getLevel() >= 2) {
                if (nrRsrq <= -15 || nrSinr <= 3) {
                    signalLevel = 1;
                } else {
                    signalLevel = 0;
                }
            } else if (signalStrength.getLevel() != 1 || nrRsrq <= -12 || nrSinr <= 11) {
                signalLevel = 2;
            } else {
                signalLevel = 1;
            }
        } else {
            log(false, "CellSignalLevel unknown RAT!", new Object[0]);
        }
        log(false, "signalLevel is %{public}d", Integer.valueOf(signalLevel));
        return signalLevel;
    }

    public HwChannelQoEParmStatistics[] getCHQoEParmStatistics() {
        return (HwChannelQoEParmStatistics[]) this.mChannelQoEParm.clone();
    }

    public HistoryMseasureInfo getHistoryMseasureInfo(int networkType) {
        log(false, "enter getHistoryMseasureInfo: networkType %{public}d", Integer.valueOf(networkType));
        if (801 == networkType) {
            return this.mCellHistoryMseasureInfo;
        }
        return this.mWifiHistoryMseasureInfo;
    }

    public CurrentSignalState getCurrentSignalState(int networkType, boolean probeRTT, IChannelQoECallback callback) {
        log(false, "enter getCurrentSignalState: networkType %{public}d probeRTT %{public}s", Integer.valueOf(networkType), String.valueOf(probeRTT));
        if (801 == networkType) {
            int subId = SubscriptionManager.getDefaultDataSubscriptionId();
            int netClass = TelephonyManager.getNetworkClass(this.mTelephonyManager.getNetworkType(subId));
            SignalStrength signalStrength = null;
            HwArbitrationManager hwArbitrationMgr = HwArbitrationManager.getInstance();
            if (hwArbitrationMgr != null) {
                signalStrength = hwArbitrationMgr.getSignalStrength(subId);
            }
            if (signalStrength == null) {
                return null;
            }
            log(false, "getCurrentSignalState NET is %{public}d", Integer.valueOf(netClass));
            if (netClass == 2) {
                int unused = this.mCurrentSignalState.networkType = 2;
                int unused2 = this.mCurrentSignalState.sigPwr = signalStrength.getWcdmaRscp();
                int unused3 = this.mCurrentSignalState.sigSnr = -1;
                int unused4 = this.mCurrentSignalState.sigQual = signalStrength.getWcdmaEcio();
                int unused5 = this.mCurrentSignalState.sigLoad = -1;
            } else if (netClass == 3) {
                int unused6 = this.mCurrentSignalState.networkType = 1;
                int unused7 = this.mCurrentSignalState.sigPwr = signalStrength.getLteRsrp();
                int unused8 = this.mCurrentSignalState.sigSnr = signalStrength.getLteRssnr();
                int unused9 = this.mCurrentSignalState.sigQual = signalStrength.getLteRsrq();
                int unused10 = this.mCurrentSignalState.sigLoad = -1;
            } else if (netClass == 4) {
                int unused11 = this.mCurrentSignalState.networkType = 3;
                int unused12 = this.mCurrentSignalState.sigPwr = signalStrength.getNrRsrp();
                int unused13 = this.mCurrentSignalState.sigSnr = signalStrength.getNrRssnr();
                int unused14 = this.mCurrentSignalState.sigQual = signalStrength.getNrRsrq();
            } else {
                int unused15 = this.mCurrentSignalState.networkType = -1;
                int unused16 = this.mCurrentSignalState.sigPwr = -1;
                int unused17 = this.mCurrentSignalState.sigSnr = -1;
                int unused18 = this.mCurrentSignalState.sigQual = -1;
                int unused19 = this.mCurrentSignalState.sigLoad = -1;
            }
            log(false, "getCurrentSignalState sigPwr = %{public}d sigSnr = %{public}d sigQual = %{public}d sigLoad = %{public}d", Integer.valueOf(this.mCurrentSignalState.sigPwr), Integer.valueOf(this.mCurrentSignalState.sigSnr), Integer.valueOf(this.mCurrentSignalState.sigQual), Integer.valueOf(this.mCurrentSignalState.sigLoad));
        } else if (800 == networkType) {
            WifiInfo info = ((WifiManager) this.mContext.getSystemService(DevSchedFeatureRT.WIFI_FEATURE)).getConnectionInfo();
            if (info != null) {
                int unused20 = this.mCurrentSignalState.sigPwr = info.getRssi();
                int unused21 = this.mCurrentSignalState.sigSnr = info.getSnr();
                int unused22 = this.mCurrentSignalState.sigQual = info.getNoise();
                int unused23 = this.mCurrentSignalState.sigLoad = info.getChload();
                log(false, "getCurrentSignalState sigPwr = %{public}d sigSnr = %{public}d sigQual = %{public}d sigLoad = %{public}d", Integer.valueOf(this.mCurrentSignalState.sigPwr), Integer.valueOf(this.mCurrentSignalState.sigSnr), Integer.valueOf(this.mCurrentSignalState.sigQual), Integer.valueOf(this.mCurrentSignalState.sigLoad));
            }
            int unused24 = this.mCurrentSignalState.networkType = 0;
        }
        if (true == probeRTT) {
            this.mGetCurrentRttFlag = 0;
            queryChannelQuality(-1, networkType, 0, false, callback);
        }
        return this.mCurrentSignalState;
    }

    public void uploadChannelQoEParmStatistics(long uploadInterval) {
        if (this.mLastUploadTime == 0 || System.currentTimeMillis() - this.mLastUploadTime > uploadInterval) {
            try {
                log(false, "enter uploadChannelQoEparamStatistics.", new Object[0]);
                IMonitor.EventStream event_909002054 = IMonitor.openEventStream((int) E909002054);
                for (int i = 0; i < 2; i++) {
                    IMonitor.EventStream event_E909009040 = IMonitor.openEventStream((int) E909009040);
                    event_E909009040.setParam("NET", this.mChannelQoEParm[i].mNetworkType);
                    IMonitor.EventStream event_E909009039 = IMonitor.openEventStream((int) E909009039);
                    for (int rst = 0; rst < 10; rst++) {
                        event_E909009039.setParam(LVl, rst + 101).setParam(Cnt, this.mChannelQoEParm[i].mRst.mRst[rst]);
                        event_E909009040.fillArrayParam("RST", event_E909009039);
                    }
                    for (int svr = 0; svr < 5; svr++) {
                        event_E909009039.setParam(LVl, svr + 201).setParam(Cnt, this.mChannelQoEParm[i].mSvr.mSvr[svr]);
                        event_E909009040.fillArrayParam("SVR", event_E909009039);
                    }
                    for (int rtt = 0; rtt < 10; rtt++) {
                        event_E909009039.setParam(LVl, rtt + 301).setParam(Cnt, this.mChannelQoEParm[i].mRtt.mRtt[rtt]);
                        event_E909009040.fillArrayParam("RTT", event_E909009039);
                    }
                    for (int drtt = 0; drtt < 10; drtt++) {
                        event_E909009039.setParam(LVl, drtt + AwareJobSchedulerService.MSG_JOB_EXPIRED).setParam(Cnt, this.mChannelQoEParm[i].mdRtt.reserved[drtt]);
                        event_E909009040.fillArrayParam("DRTT", event_E909009039);
                    }
                    for (int tpt = 0; tpt < 10; tpt++) {
                        event_E909009039.setParam(LVl, tpt + 501).setParam(Cnt, this.mChannelQoEParm[i].mTput.mTput[tpt]);
                        event_E909009040.fillArrayParam("TPT", event_E909009039);
                    }
                    IMonitor.closeEventStream(event_E909009039);
                    event_909002054.fillArrayParam("CHQOEINFO", event_E909009040);
                    IMonitor.closeEventStream(event_E909009040);
                }
                IMonitor.sendEvent(event_909002054);
                IMonitor.closeEventStream(event_909002054);
                this.mChannelQoEParm[0].reset();
                this.mChannelQoEParm[1].reset();
                this.mLastUploadTime = System.currentTimeMillis();
            } catch (RuntimeException e) {
                log(false, "uploadChannelQoEParmStatistics RuntimeException", new Object[0]);
                throw e;
            } catch (Exception e2) {
                log(false, "Exception happened in uploadChannelQoEparamStatistics", new Object[0]);
            }
        } else {
            log(false, "uploadChannelQoEParmStatistics: upload condition not allowed.", new Object[0]);
        }
    }

    public static class RttRecords {
        private long[] channel_rtt = new long[3];
        private boolean ready;

        public RttRecords() {
            initValues();
        }

        public void reset() {
            initValues();
        }

        private void initValues() {
            for (int i = 0; i < 3; i++) {
                this.channel_rtt[i] = 0;
            }
            this.ready = false;
        }

        public void insert(long rtt) {
            int i = 0;
            while (true) {
                if (i >= 3) {
                    break;
                }
                long[] jArr = this.channel_rtt;
                if (jArr[i] == 0) {
                    jArr[i] = rtt;
                    break;
                }
                i++;
            }
            if (i == 2) {
                this.ready = true;
            }
        }

        public boolean isReady() {
            return this.ready;
        }

        public boolean hasValues() {
            for (int i = 0; i < 3; i++) {
                if (this.channel_rtt[i] != 0) {
                    return true;
                }
            }
            return false;
        }

        public long getFinalRtt() {
            long result = 0;
            int count = 0;
            for (int i = 0; i < 3; i++) {
                long[] jArr = this.channel_rtt;
                if (jArr[i] == 0) {
                    break;
                }
                result += jArr[i];
                count++;
            }
            return result / ((long) count);
        }
    }

    public class CHThread extends Thread {
        private ConnectivityManager mCM;
        private Network mCellNetwork;
        /* access modifiers changed from: private */
        public int mNetworkType;
        private Network mWifiNetwork;
        public Handler threadHandler;

        public CHThread(int networkType, Handler handler, Context context) {
            this.threadHandler = handler;
            this.mCM = (ConnectivityManager) context.getSystemService("connectivity");
            this.mNetworkType = networkType;
        }

        public void run() {
        }

        public InetSocketAddress getByName(String host, int port) throws Exception {
            try {
                if (this.mNetworkType == 801 && this.mCellNetwork != null) {
                    HwChannelQoEManager.log(false, "mCellNetwork get dns.", new Object[0]);
                    return new InetSocketAddress(this.mCellNetwork.getByName(host), port);
                } else if (this.mNetworkType != 800 || this.mWifiNetwork == null) {
                    HwChannelQoEManager.log(false, "network null error. network type is %{public}d", Integer.valueOf(this.mNetworkType));
                    throw new Exception("network null error. network type is " + this.mNetworkType);
                } else {
                    HwChannelQoEManager.log(false, "mWifiNetwork get dns.", new Object[0]);
                    return new InetSocketAddress(this.mWifiNetwork.getByName(host), port);
                }
            } catch (Exception e) {
                HwChannelQoEManager.log(false, "get dns Exception", new Object[0]);
                throw e;
            }
        }

        public Network getNetworkByType(int networkType) {
            int networkTypeIndex;
            Network[] networks = this.mCM.getAllNetworks();
            if (networkType == 800) {
                networkTypeIndex = 1;
            } else {
                networkTypeIndex = 0;
            }
            Network targetNetwork = null;
            for (Network network : networks) {
                NetworkInfo networkInfo = this.mCM.getNetworkInfo(network);
                if (networkInfo != null && networkInfo.getType() == networkTypeIndex) {
                    targetNetwork = network;
                    HwArbitrationCommonUtils.logD(HwChannelQoEManager.TAG, false, "get network, result is: %{public}d", Integer.valueOf(networkTypeIndex));
                }
            }
            return targetNetwork;
        }

        public void bindSocketToNetwork(Socket sk) throws Exception {
            if (this.mNetworkType == 801) {
                this.mCellNetwork = getNetworkByType(801);
                if (this.mCellNetwork != null) {
                    HwChannelQoEManager.log(false, "mCellNetwork bind socket.", new Object[0]);
                    this.mCellNetwork.bindSocket(sk);
                } else {
                    HwChannelQoEManager.log(false, "mCellNetwork is null.", new Object[0]);
                    throw new Exception("mCellNetwork is null.");
                }
            } else {
                this.mWifiNetwork = getNetworkByType(800);
                if (this.mWifiNetwork != null) {
                    HwChannelQoEManager.log(false, "mWifiNetwork bind socket.", new Object[0]);
                    this.mWifiNetwork.bindSocket(sk);
                } else {
                    HwChannelQoEManager.log(false, "mWifiNetwork is null.", new Object[0]);
                    throw new Exception("mWifiNetwork is null.");
                }
            }
            HwChannelQoEManager.log(false, "socket bind to network succ. socketfd is %{public}d", Integer.valueOf(sk.getFileDescriptor$().getInt$()));
        }
    }

    public class CHBakThread extends CHThread {
        public CHBakThread(int networkType, Handler handler, Context context) {
            super(networkType, handler, context);
            if (801 == networkType) {
                if (HwChannelQoEManager.this.isChina()) {
                    int[] iArr = HwChannelQoEManager.this.mChannelQoEParm[0].mSvr.mSvr;
                    iArr[1] = iArr[1] + 1;
                    return;
                }
                int[] iArr2 = HwChannelQoEManager.this.mChannelQoEParm[0].mSvr.mSvr;
                iArr2[3] = iArr2[3] + 1;
            } else if (800 != networkType) {
            } else {
                if (HwChannelQoEManager.this.isChina()) {
                    int[] iArr3 = HwChannelQoEManager.this.mChannelQoEParm[1].mSvr.mSvr;
                    iArr3[1] = iArr3[1] + 1;
                    return;
                }
                int[] iArr4 = HwChannelQoEManager.this.mChannelQoEParm[1].mSvr.mSvr;
                iArr4[3] = iArr4[3] + 1;
            }
        }

        @Override // com.android.server.hidata.channelqoe.HwChannelQoEManager.CHThread
        public void run() {
            Socket sk = null;
            try {
                HwChannelQoEManager.log(false, "CHBakThread run.", new Object[0]);
                SSLContext context = SSLContext.getInstance("SSL");
                context.init(null, null, new SecureRandom());
                Socket sk2 = context.getSocketFactory().createSocket();
                super.bindSocketToNetwork(sk2);
                sk2.connect(getByName(HwChannelQoEManager.this.urlBak, 443), 4000);
                OutputStreamWriter osw = new OutputStreamWriter(sk2.getOutputStream(), Charset.defaultCharset().name());
                long getstart = System.currentTimeMillis();
                osw.write(HwChannelQoEManager.this.getBak);
                osw.flush();
                if (sk2.getInputStream().read(new byte[1000]) >= 10) {
                    long rtt = System.currentTimeMillis() - getstart;
                    HwChannelQoEManager.log(false, "CHBakThread socket Rtt is %{public}s", String.valueOf(rtt));
                    Message message = Message.obtain();
                    message.what = HwChannelQoEManager.MESSAGE_BAK_RTT;
                    message.arg1 = (int) rtt;
                    ((CHThread) this).threadHandler.sendMessage(message);
                    try {
                        sk2.close();
                    } catch (IOException e) {
                        HwChannelQoEManager.log(false, "Exception happened when close socket in CHBakThread.", new Object[0]);
                    }
                } else {
                    throw new Exception("not invalid read length.");
                }
            } catch (Exception e2) {
                Message message2 = Message.obtain();
                message2.what = HwChannelQoEManager.MESSAGE_EXCEPTION_BAK;
                ((CHThread) this).threadHandler.sendMessage(message2);
                HwChannelQoEManager.log(false, "Exception happened in CHBakThread", new Object[0]);
                if (0 != 0) {
                    sk.close();
                }
            } catch (Throwable e3) {
                if (0 != 0) {
                    try {
                        sk.close();
                    } catch (IOException e4) {
                        HwChannelQoEManager.log(false, "Exception happened when close socket in CHBakThread.", new Object[0]);
                    }
                }
                throw e3;
            }
        }
    }

    public class CHFirstThread extends CHThread {
        public CHFirstThread(int networkType, Handler handler, Context context) {
            super(networkType, handler, context);
            HwChannelQoEManager.log(false, "CHFirstThread create.", new Object[0]);
            if (801 == networkType) {
                if (HwChannelQoEManager.this.isChina()) {
                    int[] iArr = HwChannelQoEManager.this.mChannelQoEParm[0].mSvr.mSvr;
                    iArr[0] = iArr[0] + 1;
                    return;
                }
                int[] iArr2 = HwChannelQoEManager.this.mChannelQoEParm[0].mSvr.mSvr;
                iArr2[2] = iArr2[2] + 1;
            } else if (800 != networkType) {
            } else {
                if (HwChannelQoEManager.this.isChina()) {
                    int[] iArr3 = HwChannelQoEManager.this.mChannelQoEParm[1].mSvr.mSvr;
                    iArr3[0] = iArr3[0] + 1;
                    return;
                }
                int[] iArr4 = HwChannelQoEManager.this.mChannelQoEParm[1].mSvr.mSvr;
                iArr4[2] = iArr4[2] + 1;
            }
        }

        @Override // com.android.server.hidata.channelqoe.HwChannelQoEManager.CHThread
        public void run() {
            Object[] objArr;
            HwChannelQoEManager.log(false, "CHFirstThread thread start at .%{public}s", String.valueOf(System.currentTimeMillis()));
            Socket sk = new Socket();
            try {
                super.bindSocketToNetwork(sk);
                Message msg = Message.obtain();
                msg.what = HwChannelQoEManager.MESSAGE_DNS_TIMEOUT;
                ((CHThread) this).threadHandler.sendMessageDelayed(msg, 1000);
                InetSocketAddress addr = getByName(HwChannelQoEManager.this.urlFirst, 80);
                ((CHThread) this).threadHandler.removeMessages(HwChannelQoEManager.MESSAGE_DNS_TIMEOUT);
                sk.connect(addr, 4000);
                sk.setSoTimeout(500);
                OutputStreamWriter osw = new OutputStreamWriter(sk.getOutputStream(), Charset.defaultCharset().name());
                for (int i = 0; i < 3; i++) {
                    getHttpRtt(osw, sk);
                }
                try {
                    sk.close();
                    HwChannelQoEManager.log(false, "First socket closed.", new Object[0]);
                    return;
                } catch (IOException e) {
                    objArr = new Object[]{e.getMessage()};
                }
                HwChannelQoEManager.log(false, "First socket closed exception.%{public}s", objArr);
            } catch (Exception e2) {
                HwChannelQoEManager.log(false, "First socket exception", new Object[0]);
                Message message = Message.obtain();
                message.what = 61440;
                message.obj = e2;
                if (((CHThread) this).threadHandler.hasMessages(HwChannelQoEManager.MESSAGE_DNS_TIMEOUT)) {
                    HwChannelQoEManager.log(false, "DNS exception. MESSAGE_DNS_TIMEOUT will be deleted.", new Object[0]);
                    ((CHThread) this).threadHandler.removeMessages(HwChannelQoEManager.MESSAGE_DNS_TIMEOUT);
                    ((CHThread) this).threadHandler.sendMessageDelayed(message, 1000);
                } else {
                    ((CHThread) this).threadHandler.sendMessage(message);
                }
                try {
                    sk.close();
                    HwChannelQoEManager.log(false, "First socket closed.", new Object[0]);
                } catch (IOException e3) {
                    objArr = new Object[]{e3.getMessage()};
                }
            } catch (Throwable th) {
                try {
                    sk.close();
                    HwChannelQoEManager.log(false, "First socket closed.", new Object[0]);
                } catch (IOException e4) {
                    HwChannelQoEManager.log(false, "First socket closed exception.%{public}s", e4.getMessage());
                }
                throw th;
            }
        }

        private void getHttpRtt(OutputStreamWriter osw, Socket sk) throws Exception {
            HwChannelQoEManager.log(false, "getHttpRtt enter.", new Object[0]);
            long getstart = System.currentTimeMillis();
            osw.write(HwChannelQoEManager.this.getFirst.toCharArray());
            osw.flush();
            byte[] buffer = new byte[1000];
            int len = sk.getInputStream().read(buffer);
            if (len >= 5) {
                String sCheck = new String(buffer, "UTF-8");
                if (sCheck.contains("HTTP/1.1 204 No Content")) {
                    long rtt = System.currentTimeMillis() - getstart;
                    HwChannelQoEManager.log(false, "First socket Rtt is %{public}s", String.valueOf(rtt));
                    Message message = Message.obtain();
                    message.what = HwChannelQoEManager.MESSAGE_FIRST_RTT;
                    message.arg1 = (int) rtt;
                    ((CHThread) this).threadHandler.sendMessage(message);
                    return;
                }
                HwChannelQoEManager.log(false, "getHttpRtt read error!Socket input doesn't contain HTTP/1.1 204 No Content", new Object[0]);
                HwChannelQoEManager.log(false, sCheck, new Object[0]);
                throw new Exception("not invalid String.");
            }
            HwChannelQoEManager.log(false, "getHttpRtt read length is" + String.valueOf(len), new Object[0]);
            throw new Exception("not invalid read length.");
        }
    }

    public class TputTestThread extends CHThread {
        public TputTestThread(int networkType, Handler handler, Context context) {
            super(networkType, handler, context);
            if (networkType == 801) {
                int[] iArr = HwChannelQoEManager.this.mChannelQoEParm[0].mSvr.mSvr;
                iArr[4] = iArr[4] + 1;
            } else if (networkType == 800) {
                int[] iArr2 = HwChannelQoEManager.this.mChannelQoEParm[1].mSvr.mSvr;
                iArr2[4] = iArr2[4] + 1;
            }
        }

        private Float calculateTputResult(int fileSize, long downloadTime) {
            Float throughputResult = new Float(0.0f);
            if (downloadTime <= 0) {
                return throughputResult;
            }
            Float throughputResult2 = new Float(new BigDecimal((double) ((((float) fileSize) * 8.0f) / ((float) downloadTime))).setScale(2, 4).floatValue());
            HwArbitrationCommonUtils.logD(HwChannelQoEManager.TAG, false, "read: %{public}s ms, size: %{public}dKB, speed: %{public}s Mbps", String.valueOf(downloadTime), Integer.valueOf(fileSize), throughputResult2);
            return throughputResult2;
        }

        private void handleTputTestFinished(Float result) {
            Message message = Message.obtain();
            message.what = HwChannelQoEManager.MESSAGE_THROUGHPUT_TEST_FINISHED;
            message.obj = result;
            ((CHThread) this).threadHandler.sendMessage(message);
        }

        /* JADX WARNING: Code restructure failed: missing block: B:13:0x007b, code lost:
            r4 = new java.lang.Object[1];
            r19 = r0;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:15:?, code lost:
            r4[0] = 1500;
            com.android.server.hidata.arbitration.HwArbitrationCommonUtils.logD(com.android.server.hidata.channelqoe.HwChannelQoEManager.TAG, false, "test timeout: %{public}d ms", r4);
         */
        /* JADX WARNING: Code restructure failed: missing block: B:31:0x00e6, code lost:
            r0 = move-exception;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:32:0x00e7, code lost:
            r4 = r0;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:34:0x00ea, code lost:
            r4 = r17;
         */
        /* JADX WARNING: Removed duplicated region for block: B:43:0x0103 A[SYNTHETIC, Splitter:B:43:0x0103] */
        /* JADX WARNING: Removed duplicated region for block: B:48:0x0114  */
        /* JADX WARNING: Removed duplicated region for block: B:55:0x0125 A[SYNTHETIC, Splitter:B:55:0x0125] */
        /* JADX WARNING: Removed duplicated region for block: B:60:0x0136  */
        @Override // com.android.server.hidata.channelqoe.HwChannelQoEManager.CHThread
        public void run() {
            BufferedInputStream downloadStream;
            Throwable th;
            Float throughputResult;
            Float throughputResult2;
            BufferedInputStream downloadStream2 = null;
            HttpURLConnection httpURLConnection = null;
            Float throughputResult3 = new Float(0.0f);
            try {
                long downloadStartTimeStamp = SystemClock.elapsedRealtime();
                httpURLConnection = (HttpURLConnection) getNetworkByType(this.mNetworkType).openConnection(new URL(HwChannelQoEManager.this.urlCdn));
                httpURLConnection.setRequestProperty("Charset", "UTF-8");
                httpURLConnection.setConnectTimeout(500);
                httpURLConnection.setReadTimeout(500);
                httpURLConnection.setUseCaches(false);
                httpURLConnection.connect();
                int statusCode = httpURLConnection.getResponseCode();
                long connectFinishedTimeStamp = SystemClock.elapsedRealtime();
                int downloadedSize = 0;
                byte[] fileBuffer = new byte[1024];
                if (statusCode == 200) {
                    downloadStream = null;
                    try {
                        BufferedInputStream downloadStream3 = new BufferedInputStream(httpURLConnection.getInputStream());
                        while (true) {
                            int readedSize = downloadStream3.read(fileBuffer);
                            downloadStream = downloadStream3;
                            if (readedSize == -1) {
                                break;
                            }
                            downloadedSize += readedSize;
                            if (SystemClock.elapsedRealtime() - downloadStartTimeStamp > 1500) {
                                break;
                            }
                            downloadStream3 = downloadStream;
                        }
                        throughputResult = calculateTputResult(downloadedSize / 1024, SystemClock.elapsedRealtime() - connectFinishedTimeStamp);
                    } catch (IOException e) {
                        throughputResult2 = throughputResult3;
                        downloadStream2 = null;
                        try {
                            HwArbitrationCommonUtils.logE(HwChannelQoEManager.TAG, false, "IOException happened in the throughput test.", new Object[0]);
                            if (downloadStream2 != null) {
                            }
                            if (httpURLConnection != null) {
                            }
                            throughputResult = throughputResult2;
                            handleTputTestFinished(throughputResult);
                        } catch (Throwable th2) {
                            downloadStream = downloadStream2;
                            th = th2;
                            if (downloadStream != null) {
                            }
                            if (httpURLConnection != null) {
                            }
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        if (downloadStream != null) {
                        }
                        if (httpURLConnection != null) {
                        }
                        throw th;
                    }
                } else {
                    downloadStream = null;
                    HwArbitrationCommonUtils.logE(HwChannelQoEManager.TAG, false, "Http response status code is: " + statusCode, new Object[0]);
                    throughputResult = throughputResult3;
                }
                if (downloadStream != null) {
                    try {
                        downloadStream.close();
                    } catch (IOException e2) {
                        HwArbitrationCommonUtils.logE(HwChannelQoEManager.TAG, false, "Exception happened when close the download stream.", new Object[0]);
                    }
                }
                httpURLConnection.disconnect();
            } catch (IOException e3) {
                throughputResult2 = throughputResult3;
                HwArbitrationCommonUtils.logE(HwChannelQoEManager.TAG, false, "IOException happened in the throughput test.", new Object[0]);
                if (downloadStream2 != null) {
                    try {
                        downloadStream2.close();
                    } catch (IOException e4) {
                        HwArbitrationCommonUtils.logE(HwChannelQoEManager.TAG, false, "Exception happened when close the download stream.", new Object[0]);
                    }
                }
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
                throughputResult = throughputResult2;
                handleTputTestFinished(throughputResult);
            } catch (Throwable th4) {
                downloadStream = null;
                th = th4;
                if (downloadStream != null) {
                    try {
                        downloadStream.close();
                    } catch (IOException e5) {
                        HwArbitrationCommonUtils.logE(HwChannelQoEManager.TAG, false, "Exception happened when close the download stream.", new Object[0]);
                    }
                }
                if (httpURLConnection != null) {
                    httpURLConnection.disconnect();
                }
                throw th;
            }
            handleTputTestFinished(throughputResult);
        }
    }
}
