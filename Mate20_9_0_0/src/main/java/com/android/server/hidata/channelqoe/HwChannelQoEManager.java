package com.android.server.hidata.channelqoe;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.IMonitor;
import android.util.IMonitor.EventStream;
import android.util.Log;
import com.android.server.gesture.GestureNavConst;
import com.android.server.mtm.iaware.brjob.scheduler.AwareJobSchedulerService;
import com.android.server.wifipro.WifiProCommonUtils;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.net.ssl.SSLContext;

public class HwChannelQoEManager {
    private static final int AVAILABLE_CONNECT_TIMEOUT = 3000;
    private static final int AVAILABLE_INPUT_WAIT = 2000;
    private static final int AVAILABLE_TIMEOUT = 5000;
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
    private static final int DNS_TIMEOUT = 1000;
    private static final int E909002054 = 909002054;
    private static final int E909009039 = 909009039;
    private static final int E909009040 = 909009040;
    private static final int GET_CURRENT_RTT = 0;
    private static final int GET_CURRENT_RTT_SCENCE = -1;
    private static final int GET_CURRENT_RTT_UID = -1;
    private static final boolean IS_CHINA = "CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""));
    private static final String LVl = "LVL";
    private static final int MESSAGE_AVAILABLE_EXCEPTION = 61447;
    private static final int MESSAGE_AVAILABLE_OK = 61446;
    private static final int MESSAGE_AVAILABLE_TIMEOUT = 61445;
    private static final int MESSAGE_BAIDU_RTT = 61444;
    private static final int MESSAGE_DNS_TIMEOUT = 61448;
    private static final int MESSAGE_EXCEPTION = 61440;
    private static final int MESSAGE_EXCEPTION_BAK = 61441;
    private static final int MESSAGE_HICLOUD_RTT = 61443;
    private static final int MESSAGE_START_QUERY = 61449;
    private static final int MESSAGE_TIMEOUT = 61442;
    private static final int NOT_GET_CURRENT_RTT = 1;
    private static final int SOCKET_CONNECT_TIMEOUT = 4000;
    private static final int SOCKET_INPUT_WAIT_TIME = 500;
    private static final int SOCKET_MAX_TIMEOUT = 999;
    private static final String TAG = "HiDATA_ChannelQoE";
    private static final String getBaidu = "GET http://www.baidu.com/ HTTP/1.1\r\nHost: www.baidu.com\r\nConnection: Keep-Alive\r\n\r\n";
    private static final String getGoogle = "GET http://www.google.com/ HTTP/1.1\r\nHost: www.google.com\r\nConnection: Keep-Alive\r\n\r\n";
    private static final String getGstatic = "GET /generate_204 HTTP/1.1\r\nHost: connectivitycheck.gstatic.com\r\nConnection: Keep-Alive\r\n\r\n";
    private static final String getHiCloud = "GET /generate_204 HTTP/1.1\r\nHost: connectivitycheck.platform.hi\r\nConnection: Keep-Alive\r\n\r\n";
    private static volatile HwChannelQoEManager mChannelQoEManager = null;
    private static final String urlBaidu = "www.baidu.com";
    private static final String urlGoogle = "www.google.com";
    private static final String urlGstatic = "connectivitycheck.gstatic.com";
    private static final String urlHiCloud = "connectivitycheck.platform.hicloud.com";
    private String getBak = getBaidu;
    private String getFirst = getHiCloud;
    PhoneStateListener listenerSim0;
    PhoneStateListener listenerSim1;
    private CHMeasureObject mCell;
    private HistoryMseasureInfo mCellHistoryMseasureInfo = null;
    private HwCHQciManager mChQciManager;
    private HwChannelQoEParmStatistics[] mChannelQoEParm = new HwChannelQoEParmStatistics[2];
    private Context mContext;
    private CurrentSignalState mCurrentSignalState = null;
    private int mGetCurrentRttFlag = 1;
    private long mLastUploadTime;
    private HwChannelQoEMonitor mQoEMonitor;
    private SignalStrength mSignalStrengthSim0 = null;
    private SignalStrength mSignalStrengthSim1 = null;
    private TelephonyManager mTelephonyManager;
    private CHMeasureObject mWifi;
    private HistoryMseasureInfo mWifiHistoryMseasureInfo = null;
    private String urlBak = urlBaidu;
    private String urlFirst = urlHiCloud;

    public class CHMeasureObject {
        private boolean isBakRunning = false;
        private IChannelQoECallback mAvailableCallBack = null;
        private CopyOnWriteArrayList<HwChannelQoEAppInfo> mCallbackList = new CopyOnWriteArrayList();
        private Handler mHandler;
        private int mMutex = 0;
        private String mName;
        private RttRecords mRttRecords = new RttRecords();
        private int netType;

        public CHMeasureObject(String name, int networkType) {
            this.mName = name;
            this.netType = networkType;
            this.mHandler = new Handler(HwChannelQoEManager.this) {
                public void handleMessage(Message msg) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(CHMeasureObject.this.mName);
                    stringBuilder.append(" handleMessage what is ");
                    stringBuilder.append(String.valueOf(msg.what));
                    HwChannelQoEManager.log(stringBuilder.toString());
                    switch (msg.what) {
                        case 61440:
                            if (!CHMeasureObject.this.isBakRunning) {
                                if (!CHMeasureObject.this.mRttRecords.hasValues()) {
                                    HwChannelQoEManager.log("exception but record has no value. will start bakprocess");
                                    CHMeasureObject.this.isBakRunning = true;
                                    CHMeasureObject.this.bakProcess(CHMeasureObject.this.netType);
                                    break;
                                }
                                HwChannelQoEManager.log("exception but record has values.");
                                removeMessages(HwChannelQoEManager.MESSAGE_TIMEOUT);
                                CHMeasureObject.this.processAll(CHMeasureObject.this.mRttRecords.getFinalRtt());
                                break;
                            }
                            HwChannelQoEManager.log("Bak is Running. Do not process hicloud exception anymore.");
                            return;
                        case HwChannelQoEManager.MESSAGE_EXCEPTION_BAK /*61441*/:
                            CHMeasureObject.this.isBakRunning = false;
                            if (CHMeasureObject.this.mMutex != 0) {
                                removeMessages(HwChannelQoEManager.MESSAGE_TIMEOUT);
                                CHMeasureObject.this.processAll(999);
                                break;
                            }
                            HwChannelQoEManager.this.logE("MESSAGE_EXCEPTION_BAK, there is no item in callbacklist.");
                            return;
                        case HwChannelQoEManager.MESSAGE_TIMEOUT /*61442*/:
                            CHMeasureObject.this.isBakRunning = false;
                            if (!CHMeasureObject.this.mRttRecords.hasValues()) {
                                HwChannelQoEManager.log("timeout but record has no value.");
                                CHMeasureObject.this.processAll(999);
                                break;
                            }
                            HwChannelQoEManager.log("timeout but record has values.");
                            CHMeasureObject.this.processAll(CHMeasureObject.this.mRttRecords.getFinalRtt());
                            break;
                        case HwChannelQoEManager.MESSAGE_HICLOUD_RTT /*61443*/:
                            if (!CHMeasureObject.this.isBakRunning) {
                                if (CHMeasureObject.this.mMutex != 0) {
                                    CHMeasureObject.this.mRttRecords.insert((long) msg.arg1);
                                    if (CHMeasureObject.this.mRttRecords.isReady()) {
                                        HwChannelQoEManager.log("all records are ready.");
                                        removeMessages(HwChannelQoEManager.MESSAGE_TIMEOUT);
                                        CHMeasureObject.this.processAll(CHMeasureObject.this.mRttRecords.getFinalRtt());
                                        break;
                                    }
                                }
                                HwChannelQoEManager.log("mMutex is 0.");
                                return;
                            }
                            HwChannelQoEManager.log("Bak is Running. Do not process hicloud anymore.");
                            return;
                            break;
                        case HwChannelQoEManager.MESSAGE_BAIDU_RTT /*61444*/:
                            CHMeasureObject.this.isBakRunning = false;
                            if (CHMeasureObject.this.mMutex != 0) {
                                removeMessages(HwChannelQoEManager.MESSAGE_TIMEOUT);
                                CHMeasureObject.this.processAll((long) msg.arg1);
                                break;
                            }
                            HwChannelQoEManager.this.logE("MESSAGE_BAIDU_RTT, there is no item in callbacklist.");
                            return;
                        case HwChannelQoEManager.MESSAGE_AVAILABLE_TIMEOUT /*61445*/:
                            if (CHMeasureObject.this.mAvailableCallBack != null) {
                                HwChannelQoEManager.log("onCellPSAvailable false, reason CONNECT_TIMEOUT.");
                                CHMeasureObject.this.mAvailableCallBack.onCellPSAvailable(false, 1);
                                CHMeasureObject.this.mAvailableCallBack = null;
                                break;
                            }
                            break;
                        case HwChannelQoEManager.MESSAGE_AVAILABLE_OK /*61446*/:
                            removeMessages(HwChannelQoEManager.MESSAGE_AVAILABLE_TIMEOUT);
                            if (CHMeasureObject.this.mAvailableCallBack != null) {
                                HwChannelQoEManager.log("onCellPSAvailable true, reason CONNECT_AVAILABLE.");
                                CHMeasureObject.this.mAvailableCallBack.onCellPSAvailable(true, 0);
                                CHMeasureObject.this.mAvailableCallBack = null;
                                break;
                            }
                            break;
                        case HwChannelQoEManager.MESSAGE_AVAILABLE_EXCEPTION /*61447*/:
                            removeMessages(HwChannelQoEManager.MESSAGE_AVAILABLE_TIMEOUT);
                            if (CHMeasureObject.this.mAvailableCallBack != null) {
                                HwChannelQoEManager.log("onCellPSAvailable false, reason exception.");
                                CHMeasureObject.this.mAvailableCallBack.onCellPSAvailable(false, 1);
                                CHMeasureObject.this.mAvailableCallBack = null;
                                break;
                            }
                            break;
                        case HwChannelQoEManager.MESSAGE_DNS_TIMEOUT /*61448*/:
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("MESSAGE_DNS_TIMEOUT with netType ");
                            stringBuilder.append(String.valueOf(CHMeasureObject.this.netType));
                            HwChannelQoEManager.log(stringBuilder.toString());
                            int[] iArr;
                            if (801 == CHMeasureObject.this.netType) {
                                iArr = HwChannelQoEManager.this.mChannelQoEParm[0].mRst.mRst;
                                iArr[3] = iArr[3] + 1;
                            } else if (800 == CHMeasureObject.this.netType) {
                                iArr = HwChannelQoEManager.this.mChannelQoEParm[1].mRst.mRst;
                                iArr[3] = iArr[3] + 1;
                            }
                            CHMeasureObject.this.isBakRunning = true;
                            CHMeasureObject.this.bakProcess(CHMeasureObject.this.netType);
                            break;
                        case HwChannelQoEManager.MESSAGE_START_QUERY /*61449*/:
                            HwChannelQoEManager.log("MESSAGE_START_QUERY enter.");
                            CHMeasureObject.this.queryQuality(msg.obj);
                            break;
                        default:
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("handler receive unknown message ");
                            stringBuilder.append(String.valueOf(msg.what));
                            HwChannelQoEManager.log(stringBuilder.toString());
                            break;
                    }
                }
            };
        }

        private void statisticsRttScope(int mChannelQoEParmIndex, int rtt) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("enter statisticsRttScope: rtt is ");
            stringBuilder.append(String.valueOf(rtt));
            HwChannelQoEManager.log(stringBuilder.toString());
            if (mChannelQoEParmIndex >= 2) {
                HwChannelQoEManager.log("statisticsRttScope index err.");
            } else if (rtt <= 0) {
                HwChannelQoEManager.log("statisticsRttScope: rtt <= 0");
            } else {
                int[] iArr;
                if (rtt < 50) {
                    iArr = HwChannelQoEManager.this.mChannelQoEParm[mChannelQoEParmIndex].mRtt.mRtt;
                    iArr[0] = iArr[0] + 1;
                } else if (rtt < 100) {
                    iArr = HwChannelQoEManager.this.mChannelQoEParm[mChannelQoEParmIndex].mRtt.mRtt;
                    iArr[1] = iArr[1] + 1;
                } else if (rtt < 150) {
                    int[] iArr2 = HwChannelQoEManager.this.mChannelQoEParm[mChannelQoEParmIndex].mRtt.mRtt;
                    iArr2[2] = iArr2[2] + 1;
                } else if (rtt < 200) {
                    iArr = HwChannelQoEManager.this.mChannelQoEParm[mChannelQoEParmIndex].mRtt.mRtt;
                    iArr[3] = iArr[3] + 1;
                } else if (rtt < GestureNavConst.GESTURE_MOVE_TIME_THRESHOLD_4) {
                    iArr = HwChannelQoEManager.this.mChannelQoEParm[mChannelQoEParmIndex].mRtt.mRtt;
                    iArr[4] = iArr[4] + 1;
                } else if (rtt < 300) {
                    iArr = HwChannelQoEManager.this.mChannelQoEParm[mChannelQoEParmIndex].mRtt.mRtt;
                    iArr[5] = iArr[5] + 1;
                } else if (rtt < 350) {
                    iArr = HwChannelQoEManager.this.mChannelQoEParm[mChannelQoEParmIndex].mRtt.mRtt;
                    iArr[6] = iArr[6] + 1;
                } else if (rtt < 400) {
                    iArr = HwChannelQoEManager.this.mChannelQoEParm[mChannelQoEParmIndex].mRtt.mRtt;
                    iArr[7] = iArr[7] + 1;
                } else if (rtt < 500) {
                    iArr = HwChannelQoEManager.this.mChannelQoEParm[mChannelQoEParmIndex].mRtt.mRtt;
                    iArr[8] = iArr[8] + 1;
                } else {
                    iArr = HwChannelQoEManager.this.mChannelQoEParm[mChannelQoEParmIndex].mRtt.mRtt;
                    iArr[9] = iArr[9] + 1;
                }
            }
        }

        private void processAll(long finalRtt) {
            HwChannelQoEManager hwChannelQoEManager = HwChannelQoEManager.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("processAll enter. mCallbackList size is ");
            stringBuilder.append(String.valueOf(this.mCallbackList.size()));
            hwChannelQoEManager.logE(stringBuilder.toString());
            hwChannelQoEManager = HwChannelQoEManager.this;
            stringBuilder = new StringBuilder();
            stringBuilder.append("finalRtt is ");
            stringBuilder.append(String.valueOf(finalRtt));
            hwChannelQoEManager.logE(stringBuilder.toString());
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("processAll():clt CHR Param statistics,networkType is ");
            stringBuilder2.append(String.valueOf(this.netType));
            HwChannelQoEManager.log(stringBuilder2.toString());
            if (801 == this.netType) {
                statisticsRttScope(0, (int) finalRtt);
                HwChannelQoEManager.this.mCellHistoryMseasureInfo.rttBef = (int) finalRtt;
            } else if (800 == this.netType) {
                statisticsRttScope(1, (int) finalRtt);
                HwChannelQoEManager.this.mWifiHistoryMseasureInfo.rttBef = (int) finalRtt;
            }
            int lable = 1;
            Iterator it = this.mCallbackList.iterator();
            while (it.hasNext()) {
                HwChannelQoEAppInfo appqoeInfo = (HwChannelQoEAppInfo) it.next();
                int[] iArr;
                if (HwCHQciManager.getInstance().getChQciConfig(appqoeInfo.mQci).mRtt == 0) {
                    if (801 == this.netType) {
                        iArr = HwChannelQoEManager.this.mChannelQoEParm[0].mRst.mRst;
                        iArr[4] = iArr[4] + 1;
                    } else if (800 == this.netType) {
                        iArr = HwChannelQoEManager.this.mChannelQoEParm[1].mRst.mRst;
                        iArr[4] = iArr[4] + 1;
                    }
                } else if (((long) HwCHQciManager.getInstance().getChQciConfig(appqoeInfo.mQci).mRtt) <= finalRtt) {
                    lable = 1;
                    if (801 == this.netType) {
                        iArr = HwChannelQoEManager.this.mChannelQoEParm[0].mRst.mRst;
                        iArr[2] = iArr[2] + 1;
                    } else if (800 == this.netType) {
                        iArr = HwChannelQoEManager.this.mChannelQoEParm[1].mRst.mRst;
                        iArr[2] = iArr[2] + 1;
                    }
                } else {
                    lable = 0;
                    if (801 == this.netType) {
                        iArr = HwChannelQoEManager.this.mChannelQoEParm[0].mRst.mRst;
                        iArr[0] = iArr[0] + 1;
                    } else if (800 == this.netType) {
                        iArr = HwChannelQoEManager.this.mChannelQoEParm[1].mRst.mRst;
                        iArr[0] = iArr[0] + 1;
                    }
                }
                appqoeInfo.callback.onChannelQuality(appqoeInfo.mUID, appqoeInfo.mScence, appqoeInfo.mNetwork, lable);
                HwChannelQoEManager hwChannelQoEManager2 = HwChannelQoEManager.this;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("label is ");
                stringBuilder3.append(String.valueOf(lable));
                hwChannelQoEManager2.logE(stringBuilder3.toString());
                if (-1 == appqoeInfo.mUID && HwChannelQoEManager.this.mGetCurrentRttFlag == 0) {
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("processAll(): get current rtt, rtt is ");
                    stringBuilder4.append(HwCHQciManager.getInstance().getChQciConfig(appqoeInfo.mQci).mRtt);
                    HwChannelQoEManager.log(stringBuilder4.toString());
                    appqoeInfo.callback.onCurrentRtt((int) finalRtt);
                    HwChannelQoEManager.this.mGetCurrentRttFlag = 1;
                }
            }
            this.mCallbackList.clear();
            this.mMutex = 0;
            this.mRttRecords.reset();
        }

        private void queryQuality(HwChannelQoEAppInfo appqoeInfo) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.mName);
            stringBuilder.append(" queryQuality.");
            HwChannelQoEManager.log(stringBuilder.toString());
            this.mCallbackList.add(appqoeInfo);
            if (this.mMutex > 0) {
                HwChannelQoEManager.this.logE("There is another RTT processing now.");
                return;
            }
            this.mMutex++;
            firstRttProcess(appqoeInfo.mNetwork);
            Message msg = Message.obtain();
            msg.what = HwChannelQoEManager.MESSAGE_TIMEOUT;
            this.mHandler.sendMessageDelayed(msg, 5500);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(this.mName);
            stringBuilder2.append(" send timer MESSAGE_TIMEOUT.");
            HwChannelQoEManager.log(stringBuilder2.toString());
        }

        public void queryAvailable(IChannelQoECallback callBack) {
            this.mAvailableCallBack = callBack;
            new CHAvailableThread(801, this.mHandler, HwChannelQoEManager.this.mContext).start();
            Message message = Message.obtain();
            message.what = HwChannelQoEManager.MESSAGE_AVAILABLE_TIMEOUT;
            this.mHandler.sendMessageDelayed(message, 5000);
        }

        private void firstRttProcess(int networkType) {
            HwChannelQoEManager.log("firstRttProcess enter.");
            new CHFirstThread(networkType, this.mHandler, HwChannelQoEManager.this.mContext).start();
        }

        private void bakProcess(int networkType) {
            HwChannelQoEManager.log("bakProcess enter.");
            new CHBakThread(networkType, this.mHandler, HwChannelQoEManager.this.mContext).start();
        }
    }

    public class CHThread extends Thread {
        private ConnectivityManager mCM;
        private Network mCellNetwork;
        private int mNetworkType;
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
            StringBuilder stringBuilder;
            try {
                if (this.mNetworkType == 801 && this.mCellNetwork != null) {
                    HwChannelQoEManager.log("mCellNetwork get dns.");
                    return new InetSocketAddress(this.mCellNetwork.getByName(host), port);
                } else if (this.mNetworkType != 800 || this.mWifiNetwork == null) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("network null error. network type is ");
                    stringBuilder2.append(String.valueOf(this.mNetworkType));
                    HwChannelQoEManager.log(stringBuilder2.toString());
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("network null error. network type is ");
                    stringBuilder.append(String.valueOf(this.mNetworkType));
                    throw new Exception(stringBuilder.toString());
                } else {
                    HwChannelQoEManager.log("mWifiNetwork get dns.");
                    return new InetSocketAddress(this.mWifiNetwork.getByName(host), port);
                }
            } catch (Exception e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("get dns Exception. ");
                stringBuilder.append(e.toString());
                HwChannelQoEManager.log(stringBuilder.toString());
                throw e;
            }
        }

        public void bindSocketToNetwork(Socket sk) throws Exception {
            Network[] networks = this.mCM.getAllNetworks();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("networks size is ");
            stringBuilder.append(String.valueOf(networks.length));
            HwChannelQoEManager.log(stringBuilder.toString());
            for (int i = 0; i < networks.length; i++) {
                NetworkInfo networkInfo = this.mCM.getNetworkInfo(networks[i]);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("networksInfo is ");
                stringBuilder2.append(networkInfo.toString());
                HwChannelQoEManager.log(stringBuilder2.toString());
                if (networkInfo.getType() == 0) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("networks TYPE_MOBILE ");
                    stringBuilder2.append(String.valueOf(i));
                    HwChannelQoEManager.log(stringBuilder2.toString());
                    this.mCellNetwork = networks[i];
                } else if (networkInfo.getType() == 1) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("networks[i].netId ");
                    stringBuilder2.append(networks[i].toString());
                    HwChannelQoEManager.log(stringBuilder2.toString());
                    this.mWifiNetwork = networks[i];
                }
            }
            if (this.mNetworkType == 801) {
                if (this.mCellNetwork != null) {
                    HwChannelQoEManager.log("mCellNetwork bind socket.");
                    this.mCellNetwork.bindSocket(sk);
                } else {
                    HwChannelQoEManager.log("mCellNetwork is null.");
                    throw new Exception("mCellNetwork is null.");
                }
            } else if (this.mWifiNetwork != null) {
                HwChannelQoEManager.log("mWifiNetwork bind socket.");
                this.mWifiNetwork.bindSocket(sk);
            } else {
                HwChannelQoEManager.log("mWifiNetwork is null.");
                throw new Exception("mWifiNetwork is null.");
            }
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("socket bind to network succ. socketfd is ");
            stringBuilder3.append(String.valueOf(sk.getFileDescriptor$().getInt$()));
            HwChannelQoEManager.log(stringBuilder3.toString());
        }
    }

    public static class CurrentSignalState {
        private int networkType = 0;
        private int sigLoad;
        private int sigPwr;
        private int sigQual;
        private int sigSnr;

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

    public static class HistoryMseasureInfo {
        private int rat = -1;
        private int rttBef = 0;
        private int sigLoad = 255;
        private int sigPwr = 255;
        private int sigQual = 255;
        private int sigSnr = 255;
        private int tupBef = 0;

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("tupBef:");
            stringBuilder.append(String.valueOf(this.tupBef));
            stringBuilder.append(",rttBef:");
            stringBuilder.append(String.valueOf(this.rttBef));
            stringBuilder.append(",sigPwr:");
            stringBuilder.append(String.valueOf(this.sigPwr));
            stringBuilder.append(",sigSnr:");
            stringBuilder.append(String.valueOf(this.sigSnr));
            stringBuilder.append(",sigQual:");
            stringBuilder.append(String.valueOf(this.sigQual));
            stringBuilder.append(",sigLoad:");
            stringBuilder.append(String.valueOf(this.sigLoad));
            stringBuilder.append(",rat:");
            stringBuilder.append(String.valueOf(this.rat));
            return stringBuilder.toString();
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
            while (i < 3) {
                if (this.channel_rtt[i] == 0) {
                    this.channel_rtt[i] = rtt;
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
            int i = 0;
            while (i < 3 && this.channel_rtt[i] != 0) {
                result += this.channel_rtt[i];
                count++;
                i++;
            }
            return result / ((long) count);
        }
    }

    public class CHAvailableThread extends CHThread {
        public CHAvailableThread(int networkType, Handler handler, Context context) {
            super(networkType, handler, context);
        }

        public void run() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CHAvailableThread thread start at .");
            stringBuilder.append(String.valueOf(System.currentTimeMillis()));
            HwChannelQoEManager.log(stringBuilder.toString());
            Socket sk = new Socket();
            IOException e;
            StringBuilder stringBuilder2;
            try {
                super.bindSocketToNetwork(sk);
                sk.connect(getByName(HwChannelQoEManager.this.urlFirst, 80), 3000);
                sk.setSoTimeout(2000);
                OutputStreamWriter osw = new OutputStreamWriter(sk.getOutputStream(), Charset.defaultCharset().name());
                osw.write(HwChannelQoEManager.this.getFirst.toCharArray());
                osw.flush();
                if (sk.getInputStream().read(new byte[1000]) >= 5) {
                    Message message = Message.obtain();
                    message.what = HwChannelQoEManager.MESSAGE_AVAILABLE_OK;
                    this.threadHandler.sendMessage(message);
                    if (sk.isConnected()) {
                        try {
                            sk.close();
                            HwChannelQoEManager.log("CHAvailableThread socket closed.");
                            return;
                        } catch (IOException e2) {
                            e = e2;
                            stringBuilder2 = new StringBuilder();
                        }
                    } else {
                        return;
                    }
                }
                throw new Exception("not invalid read length.");
                stringBuilder2.append("CHAvailableThread socket closed exception.");
                stringBuilder2.append(e.toString());
                HwChannelQoEManager.log(stringBuilder2.toString());
            } catch (Exception e3) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("CHAvailableThread thread exception. ");
                stringBuilder2.append(e3.toString());
                HwChannelQoEManager.log(stringBuilder2.toString());
                Message message2 = Message.obtain();
                message2.what = HwChannelQoEManager.MESSAGE_AVAILABLE_EXCEPTION;
                this.threadHandler.sendMessage(message2);
                if (sk.isConnected()) {
                    try {
                        sk.close();
                        HwChannelQoEManager.log("CHAvailableThread socket closed.");
                    } catch (IOException e4) {
                        e = e4;
                        stringBuilder2 = new StringBuilder();
                    }
                }
            } catch (Throwable th) {
                if (sk.isConnected()) {
                    try {
                        sk.close();
                        HwChannelQoEManager.log("CHAvailableThread socket closed.");
                    } catch (IOException e5) {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("CHAvailableThread socket closed exception.");
                        stringBuilder3.append(e5.toString());
                        HwChannelQoEManager.log(stringBuilder3.toString());
                    }
                }
            }
        }
    }

    public class CHBakThread extends CHThread {
        public CHBakThread(int networkType, Handler handler, Context context) {
            super(networkType, handler, context);
            int[] iArr;
            int[] iArr2;
            if (801 == networkType) {
                if (HwChannelQoEManager.this.isChina()) {
                    iArr = HwChannelQoEManager.this.mChannelQoEParm[0].mSvr.mSvr;
                    iArr[1] = iArr[1] + 1;
                    return;
                }
                iArr2 = HwChannelQoEManager.this.mChannelQoEParm[0].mSvr.mSvr;
                iArr2[3] = iArr2[3] + 1;
            } else if (800 != networkType) {
            } else {
                if (HwChannelQoEManager.this.isChina()) {
                    iArr = HwChannelQoEManager.this.mChannelQoEParm[1].mSvr.mSvr;
                    iArr[1] = iArr[1] + 1;
                    return;
                }
                iArr2 = HwChannelQoEManager.this.mChannelQoEParm[1].mSvr.mSvr;
                iArr2[3] = iArr2[3] + 1;
            }
        }

        public void run() {
            try {
                HwChannelQoEManager.log("CHBakThread run.");
                SSLContext context = SSLContext.getInstance("SSL");
                context.init(null, null, new SecureRandom());
                Socket sk = context.getSocketFactory().createSocket();
                super.bindSocketToNetwork(sk);
                sk.connect(getByName(HwChannelQoEManager.this.urlBak, 443), 4000);
                OutputStreamWriter osw = new OutputStreamWriter(sk.getOutputStream(), Charset.defaultCharset().name());
                long getstart = System.currentTimeMillis();
                osw.write(HwChannelQoEManager.this.getBak);
                osw.flush();
                if (sk.getInputStream().read(new byte[1000]) >= 10) {
                    long rtt = System.currentTimeMillis() - getstart;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("CHBakThread socket Rtt is ");
                    stringBuilder.append(String.valueOf(rtt));
                    HwChannelQoEManager.log(stringBuilder.toString());
                    context = Message.obtain();
                    context.what = HwChannelQoEManager.MESSAGE_BAIDU_RTT;
                    context.arg1 = (int) rtt;
                    this.threadHandler.sendMessage(context);
                    return;
                }
                SSLContext sSLContext = context;
                throw new Exception("not invalid read length.");
            } catch (Exception e) {
                Message message = Message.obtain();
                message.what = HwChannelQoEManager.MESSAGE_EXCEPTION_BAK;
                this.threadHandler.sendMessage(message);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("CHBakThread socket err ");
                stringBuilder2.append(e.toString());
                HwChannelQoEManager.log(stringBuilder2.toString());
            }
        }
    }

    public class CHFirstThread extends CHThread {
        public CHFirstThread(int networkType, Handler handler, Context context) {
            super(networkType, handler, context);
            HwChannelQoEManager.log("CHFirstThread create.");
            int[] iArr;
            int[] iArr2;
            if (801 == networkType) {
                if (HwChannelQoEManager.this.isChina()) {
                    iArr = HwChannelQoEManager.this.mChannelQoEParm[0].mSvr.mSvr;
                    iArr[0] = iArr[0] + 1;
                    return;
                }
                iArr2 = HwChannelQoEManager.this.mChannelQoEParm[0].mSvr.mSvr;
                iArr2[2] = iArr2[2] + 1;
            } else if (800 != networkType) {
            } else {
                if (HwChannelQoEManager.this.isChina()) {
                    iArr = HwChannelQoEManager.this.mChannelQoEParm[1].mSvr.mSvr;
                    iArr[0] = iArr[0] + 1;
                    return;
                }
                iArr2 = HwChannelQoEManager.this.mChannelQoEParm[1].mSvr.mSvr;
                iArr2[2] = iArr2[2] + 1;
            }
        }

        public void run() {
            StringBuilder stringBuilder;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("CHFirstThread thread start at .");
            stringBuilder2.append(String.valueOf(System.currentTimeMillis()));
            HwChannelQoEManager.log(stringBuilder2.toString());
            Socket sk = new Socket();
            IOException e;
            StringBuilder stringBuilder3;
            try {
                super.bindSocketToNetwork(sk);
                Message msg = Message.obtain();
                msg.what = HwChannelQoEManager.MESSAGE_DNS_TIMEOUT;
                this.threadHandler.sendMessageDelayed(msg, 1000);
                InetSocketAddress addr = getByName(HwChannelQoEManager.this.urlFirst, 80);
                this.threadHandler.removeMessages(HwChannelQoEManager.MESSAGE_DNS_TIMEOUT);
                sk.connect(addr, 4000);
                sk.setSoTimeout(500);
                OutputStreamWriter osw = new OutputStreamWriter(sk.getOutputStream(), Charset.defaultCharset().name());
                for (int i = 0; i < 3; i++) {
                    getHttpRtt(osw, sk);
                }
                try {
                    sk.close();
                    HwChannelQoEManager.log("First socket closed.");
                    return;
                } catch (IOException e2) {
                    e = e2;
                    stringBuilder3 = new StringBuilder();
                }
                stringBuilder3.append("First socket closed exception.");
                stringBuilder3.append(e.toString());
                HwChannelQoEManager.log(stringBuilder3.toString());
            } catch (Exception e3) {
                Message message = Message.obtain();
                message.what = 61440;
                message.obj = e3;
                this.threadHandler.sendMessage(message);
                stringBuilder = new StringBuilder();
                stringBuilder.append("First socket exception. ");
                stringBuilder.append(e3.toString());
                HwChannelQoEManager.log(stringBuilder.toString());
                try {
                    sk.close();
                    HwChannelQoEManager.log("First socket closed.");
                } catch (IOException e4) {
                    e = e4;
                    stringBuilder3 = new StringBuilder();
                }
            } catch (Throwable th) {
                try {
                    sk.close();
                    HwChannelQoEManager.log("First socket closed.");
                } catch (IOException e5) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("First socket closed exception.");
                    stringBuilder.append(e5.toString());
                    HwChannelQoEManager.log(stringBuilder.toString());
                }
                throw th;
            }
        }

        private void getHttpRtt(OutputStreamWriter osw, Socket sk) throws Exception {
            HwChannelQoEManager.log("getHttpRtt enter.");
            long getstart = System.currentTimeMillis();
            osw.write(HwChannelQoEManager.this.getFirst.toCharArray());
            osw.flush();
            byte[] buffer = new byte[1000];
            int len = sk.getInputStream().read(buffer);
            if (len >= 5) {
                String sCheck = new String(buffer, "UTF-8");
                if (sCheck.contains("HTTP/1.1 204 No Content")) {
                    long rtt = System.currentTimeMillis() - getstart;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("First socket Rtt is ");
                    stringBuilder.append(String.valueOf(rtt));
                    HwChannelQoEManager.log(stringBuilder.toString());
                    Message message = Message.obtain();
                    message.what = HwChannelQoEManager.MESSAGE_HICLOUD_RTT;
                    message.arg1 = (int) rtt;
                    this.threadHandler.sendMessage(message);
                    return;
                }
                HwChannelQoEManager.log("getHttpRtt read error!Socket input doesn't contain HTTP/1.1 204 No Content");
                HwChannelQoEManager.log(sCheck);
                throw new Exception("not invalid String.");
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getHttpRtt read length is");
            stringBuilder2.append(String.valueOf(len));
            HwChannelQoEManager.log(stringBuilder2.toString());
            throw new Exception("not invalid read length.");
        }
    }

    public static HwChannelQoEManager createInstance(Context context) {
        if (mChannelQoEManager == null) {
            synchronized (HwChannelQoEManager.class) {
                if (mChannelQoEManager == null) {
                    log("createInstance enter.");
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
        this.listenerSim0 = new PhoneStateListener(Integer.valueOf(0)) {
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                HwChannelQoEManager.this.mSignalStrengthSim0 = signalStrength;
            }
        };
        this.listenerSim1 = new PhoneStateListener(Integer.valueOf(1)) {
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                HwChannelQoEManager.this.mSignalStrengthSim1 = signalStrength;
            }
        };
        logE("HwChannelQoEManager enter.");
        this.mContext = context;
        this.mCell = new CHMeasureObject("mCell", 801);
        this.mWifi = new CHMeasureObject("mWifi", 800);
        this.mQoEMonitor = new HwChannelQoEMonitor(this.mContext);
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        this.mCurrentSignalState = new CurrentSignalState();
        regSignalListener();
        this.mCellHistoryMseasureInfo = new HistoryMseasureInfo();
        this.mWifiHistoryMseasureInfo = new HistoryMseasureInfo();
        this.mChannelQoEParm[0].mNetworkType = 801;
        this.mChannelQoEParm[1].mNetworkType = 800;
        this.mChQciManager = HwCHQciManager.getInstance();
        logE("HwChannelQoEManager create succ.");
    }

    private void logE(String info) {
        Log.e(TAG, info);
    }

    public static void log(String info) {
        Log.e(TAG, info);
    }

    public void startWifiLinkMonitor(int UID, int scence, int qci, IChannelQoECallback callback) {
        logE("startWifiLinkMonitor enter.");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startWifiLinkMonitor enter, UID is ");
        stringBuilder.append(UID);
        logE(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("startWifiLinkMonitor enter, scence is ");
        stringBuilder.append(scence);
        logE(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("startWifiLinkMonitor enter, qci is ");
        stringBuilder.append(qci);
        logE(stringBuilder.toString());
        HwCHQciConfig config = HwCHQciManager.getInstance().getChQciConfig(qci);
        this.mQoEMonitor.startMonitor(new HwChannelQoEAppInfo(UID, scence, 800, qci, callback));
    }

    public void stopWifiLinkMonitor(int UID, boolean stopAll) {
        if (stopAll) {
            log("stopWifiLinkMonitor stop all.");
            this.mQoEMonitor.stopAll();
        } else if (UID == -1) {
            log("stopWifiLinkMonitor invalid UID.");
        } else {
            this.mQoEMonitor.stopMonitor(UID);
        }
    }

    public void queryCellPSAvailable(IChannelQoECallback callBack) {
        logE("queryCellPSAvailable enter.");
        if (queryCellSignalLevel(0) == 0) {
            this.mCell.queryAvailable(callBack);
            return;
        }
        logE("onCellPSAvailable false, reason CONNECT_SIGNAL_POOR.");
        callBack.onCellPSAvailable(false, 2);
    }

    public void queryChannelQuality(int UID, int scence, int networkType, int qci, IChannelQoECallback callback) {
        int i = networkType;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("queryChannelQuality enter, UID:");
        stringBuilder.append(String.valueOf(UID));
        stringBuilder.append(", scence:");
        stringBuilder.append(String.valueOf(scence));
        stringBuilder.append(", networkType:");
        stringBuilder.append(String.valueOf(networkType));
        stringBuilder.append(", qci:");
        stringBuilder.append(String.valueOf(qci));
        logE(stringBuilder.toString());
        int i2 = qci;
        HwCHQciConfig config = HwCHQciManager.getInstance().getChQciConfig(i2);
        try {
            if (IS_CHINA) {
                if (isChina()) {
                    this.urlFirst = urlHiCloud;
                    this.getFirst = getHiCloud;
                    this.urlBak = urlBaidu;
                    this.getBak = getBaidu;
                    log("In China, using hicloud and baidu.");
                } else {
                    this.urlFirst = urlGstatic;
                    this.getFirst = getGstatic;
                    this.urlBak = urlGoogle;
                    this.getBak = getGoogle;
                    log("over sea, using gstatic and google.");
                }
            }
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("set url exception. ");
            stringBuilder2.append(e.toString());
            log(stringBuilder2.toString());
        }
        int i3;
        int i4;
        IChannelQoECallback iChannelQoECallback;
        if (801 == i) {
            this.mCellHistoryMseasureInfo.reset();
            this.mCellHistoryMseasureInfo.tupBef = -1;
            int signal_level = queryCellSignalLevel(config.mTput);
            Message startMessage;
            if (signal_level == 0) {
                logE("queryChannelQuality signal level good.");
                startMessage = Message.obtain();
                startMessage.what = MESSAGE_START_QUERY;
                startMessage.obj = new HwChannelQoEAppInfo(UID, scence, i, i2, callback);
                this.mCell.mHandler.sendMessage(startMessage);
            } else if (signal_level == 1) {
                logE("queryChannelQuality signal level MODERATE.");
                startMessage = Message.obtain();
                startMessage.what = MESSAGE_START_QUERY;
                startMessage.obj = new HwChannelQoEAppInfo(UID, scence, i, i2, callback);
                this.mCell.mHandler.sendMessage(startMessage);
            } else {
                logE("queryChannelQuality signal level bad.");
                int[] iArr = this.mChannelQoEParm[0].mRst.mRst;
                iArr[1] = iArr[1] + 1;
                callback.onChannelQuality(UID, scence, i, 1);
                return;
            }
            i3 = UID;
            i4 = scence;
            iChannelQoECallback = callback;
            return;
        }
        i3 = UID;
        i4 = scence;
        iChannelQoECallback = callback;
        if (800 == i) {
            this.mWifiHistoryMseasureInfo.reset();
            WifiManager mWManager = (WifiManager) this.mContext.getSystemService("wifi");
            WifiInfo info = mWManager.getConnectionInfo();
            if (info != null) {
                this.mWifiHistoryMseasureInfo.sigPwr = info.getRssi();
                this.mWifiHistoryMseasureInfo.sigSnr = info.getSnr();
                this.mWifiHistoryMseasureInfo.sigQual = info.getNoise();
                this.mWifiHistoryMseasureInfo.sigLoad = info.getChload();
                this.mWifiHistoryMseasureInfo.tupBef = -1;
            }
            Message startMessage2 = Message.obtain();
            startMessage2.what = MESSAGE_START_QUERY;
            HwChannelQoEAppInfo hwChannelQoEAppInfo = r2;
            HwChannelQoEAppInfo hwChannelQoEAppInfo2 = new HwChannelQoEAppInfo(i3, i4, i, i2, iChannelQoECallback);
            startMessage2.obj = hwChannelQoEAppInfo;
            this.mWifi.mHandler.sendMessage(startMessage2);
            return;
        }
        logE("queryChannelQuality networkType error");
    }

    private boolean isChina() {
        String operator = TelephonyManager.getDefault().getNetworkOperator();
        return (operator == null || operator.length() == 0 || !operator.startsWith("460")) ? false : true;
    }

    private void regSignalListener() {
        this.mTelephonyManager.listen(this.listenerSim0, 256);
        this.mTelephonyManager.listen(this.listenerSim1, 256);
    }

    public int queryCellSignalLevel(int tput_thresh) {
        SignalStrength signalStrength;
        int signal_level = 2;
        int subId = SubscriptionManager.getDefaultDataSubscriptionId();
        int net_type = this.mTelephonyManager.getNetworkType(subId);
        int RAT_class = TelephonyManager.getNetworkClass(net_type);
        if (subId == 0) {
            signalStrength = this.mSignalStrengthSim0;
        } else {
            signalStrength = this.mSignalStrengthSim1;
        }
        if (signalStrength == null) {
            return 2;
        }
        if (1 == RAT_class) {
            log("CellSignalLevel 2G");
            signal_level = 2;
        } else if (2 == RAT_class) {
            this.mCellHistoryMseasureInfo.rat = 2;
            if (tput_thresh > 2) {
                log("CellSignalLevel 3G");
                signal_level = 2;
            }
            int ecio = -1;
            if (net_type != 15) {
                switch (net_type) {
                    case 3:
                        break;
                    case 4:
                        ecio = signalStrength.getCdmaEcio();
                        break;
                    case 5:
                    case 6:
                        ecio = signalStrength.getEvdoEcio();
                        break;
                    default:
                        switch (net_type) {
                            case 8:
                            case 9:
                            case 10:
                                break;
                        }
                        break;
                }
            }
            ecio = signalStrength.getWcdmaEcio();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CellSignalLevel 3G = ");
            stringBuilder.append(signalStrength.getLevel());
            stringBuilder.append(" ecio = ");
            stringBuilder.append(ecio);
            log(stringBuilder.toString());
            this.mCellHistoryMseasureInfo.sigQual = ecio;
            this.mCellHistoryMseasureInfo.sigPwr = signalStrength.getWcdmaRscp();
            if (ecio == -1) {
                if (signalStrength.getLevel() >= 3) {
                    signal_level = 0;
                }
            } else if (signalStrength.getLevel() >= 2 && ecio > -12) {
                signal_level = 1;
            }
        } else if (3 == RAT_class) {
            this.mCellHistoryMseasureInfo.rat = 1;
            int rsrq = signalStrength.getLteRsrq();
            int sinr = signalStrength.getLteRssnr();
            this.mCellHistoryMseasureInfo.sigPwr = signalStrength.getLteRsrp();
            this.mCellHistoryMseasureInfo.sigSnr = sinr;
            this.mCellHistoryMseasureInfo.sigQual = rsrq;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("CellSignalLevel LTE = ");
            stringBuilder2.append(signalStrength.getLevel());
            stringBuilder2.append(" rsrq = ");
            stringBuilder2.append(rsrq);
            stringBuilder2.append(" sinr = ");
            stringBuilder2.append(sinr);
            log(stringBuilder2.toString());
            if (signalStrength.getLevel() >= 2) {
                if (rsrq <= -15 || sinr <= 3) {
                    signal_level = 1;
                } else {
                    signal_level = 0;
                }
            } else if (signalStrength.getLevel() != 1 || rsrq <= -12 || sinr <= 11) {
                signal_level = 2;
            } else {
                signal_level = 1;
            }
        } else {
            log("CellSignalLevel unknown RAT!");
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("signal_level is ");
        stringBuilder3.append(String.valueOf(signal_level));
        log(stringBuilder3.toString());
        return signal_level;
    }

    public HwChannelQoEParmStatistics[] getCHQoEParmStatistics() {
        return (HwChannelQoEParmStatistics[]) this.mChannelQoEParm.clone();
    }

    public HistoryMseasureInfo getHistoryMseasureInfo(int networkType) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("enter getHistoryMseasureInfo: networkType ");
        stringBuilder.append(String.valueOf(networkType));
        log(stringBuilder.toString());
        if (801 == networkType) {
            return this.mCellHistoryMseasureInfo;
        }
        return this.mWifiHistoryMseasureInfo;
    }

    public CurrentSignalState getCurrentSignalState(int networkType, boolean probeRTT, IChannelQoECallback callback) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("enter getCurrentSignalState: networkType probeRTT  ");
        stringBuilder.append(String.valueOf(networkType));
        stringBuilder.append(" ");
        stringBuilder.append(String.valueOf(probeRTT));
        log(stringBuilder.toString());
        if (801 == networkType) {
            SignalStrength signalStrength;
            int subId = SubscriptionManager.getDefaultDataSubscriptionId();
            int RAT_class = TelephonyManager.getNetworkClass(this.mTelephonyManager.getNetworkType(subId));
            if (subId == 0) {
                signalStrength = this.mSignalStrengthSim0;
            } else {
                signalStrength = this.mSignalStrengthSim1;
            }
            if (signalStrength == null) {
                return null;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getCurrentSignalState RAT is ");
            stringBuilder2.append(String.valueOf(RAT_class));
            log(stringBuilder2.toString());
            if (2 == RAT_class) {
                this.mCurrentSignalState.networkType = 2;
                this.mCurrentSignalState.sigPwr = signalStrength.getWcdmaRscp();
                this.mCurrentSignalState.sigSnr = -1;
                this.mCurrentSignalState.sigQual = signalStrength.getWcdmaEcio();
                this.mCurrentSignalState.sigLoad = -1;
            } else if (3 == RAT_class) {
                this.mCurrentSignalState.networkType = 1;
                this.mCurrentSignalState.sigPwr = signalStrength.getLteRsrp();
                this.mCurrentSignalState.sigSnr = signalStrength.getLteRssnr();
                this.mCurrentSignalState.sigQual = signalStrength.getLteRsrq();
                this.mCurrentSignalState.sigLoad = -1;
            } else {
                this.mCurrentSignalState.networkType = -1;
                this.mCurrentSignalState.sigPwr = -1;
                this.mCurrentSignalState.sigSnr = -1;
                this.mCurrentSignalState.sigQual = -1;
                this.mCurrentSignalState.sigLoad = -1;
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getCurrentSignalState sigPwr = ");
            stringBuilder2.append(this.mCurrentSignalState.sigPwr);
            stringBuilder2.append(" sigSnr = ");
            stringBuilder2.append(this.mCurrentSignalState.sigSnr);
            stringBuilder2.append(" sigQual = ");
            stringBuilder2.append(this.mCurrentSignalState.sigQual);
            stringBuilder2.append(" sigLoad = ");
            stringBuilder2.append(this.mCurrentSignalState.sigLoad);
            log(stringBuilder2.toString());
        } else if (800 == networkType) {
            WifiInfo info = ((WifiManager) this.mContext.getSystemService("wifi")).getConnectionInfo();
            if (info != null) {
                this.mCurrentSignalState.sigPwr = info.getRssi();
                this.mCurrentSignalState.sigSnr = info.getSnr();
                this.mCurrentSignalState.sigQual = info.getNoise();
                this.mCurrentSignalState.sigLoad = info.getChload();
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("getCurrentSignalState sigPwr = ");
                stringBuilder3.append(this.mCurrentSignalState.sigPwr);
                stringBuilder3.append(" sigSnr = ");
                stringBuilder3.append(this.mCurrentSignalState.sigSnr);
                stringBuilder3.append(" sigQual = ");
                stringBuilder3.append(this.mCurrentSignalState.sigQual);
                stringBuilder3.append(" sigLoad = ");
                stringBuilder3.append(this.mCurrentSignalState.sigLoad);
                log(stringBuilder3.toString());
            }
            this.mCurrentSignalState.networkType = 0;
        }
        if (true == probeRTT) {
            this.mGetCurrentRttFlag = 0;
            queryChannelQuality(-1, -1, networkType, 0, callback);
        }
        return this.mCurrentSignalState;
    }

    public void uploadChannelQoEParmStatistics(long uploadInterval) {
        if (this.mLastUploadTime == 0 || System.currentTimeMillis() - this.mLastUploadTime > uploadInterval) {
            try {
                log("enter uploadChannelQoEparamStatistics.");
                EventStream event_909002054 = IMonitor.openEventStream(E909002054);
                for (int i = 0; i < 2; i++) {
                    int rst;
                    EventStream event_E909009040 = IMonitor.openEventStream(E909009040);
                    event_E909009040.setParam("NET", this.mChannelQoEParm[i].mNetworkType);
                    EventStream event_E909009039 = IMonitor.openEventStream(E909009039);
                    for (rst = 0; rst < 10; rst++) {
                        event_E909009039.setParam(LVl, 101 + rst).setParam(Cnt, this.mChannelQoEParm[i].mRst.mRst[rst]);
                        event_E909009040.fillArrayParam("RST", event_E909009039);
                    }
                    for (rst = 0; rst < 5; rst++) {
                        event_E909009039.setParam(LVl, 201 + rst).setParam(Cnt, this.mChannelQoEParm[i].mSvr.mSvr[rst]);
                        event_E909009040.fillArrayParam("SVR", event_E909009039);
                    }
                    for (rst = 0; rst < 10; rst++) {
                        event_E909009039.setParam(LVl, 301 + rst).setParam(Cnt, this.mChannelQoEParm[i].mRtt.mRtt[rst]);
                        event_E909009040.fillArrayParam("RTT", event_E909009039);
                    }
                    for (rst = 0; rst < 10; rst++) {
                        event_E909009039.setParam(LVl, AwareJobSchedulerService.MSG_JOB_EXPIRED + rst).setParam(Cnt, this.mChannelQoEParm[i].mdRtt.reserved[rst]);
                        event_E909009040.fillArrayParam("DRTT", event_E909009039);
                    }
                    for (rst = 0; rst < 10; rst++) {
                        event_E909009039.setParam(LVl, 501 + rst).setParam(Cnt, this.mChannelQoEParm[i].mTpt.reserved[rst]);
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
                return;
            } catch (RuntimeException e) {
                log("uploadChannelQoEParmStatistics RuntimeException");
                throw e;
            } catch (Exception e2) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("uploadChannelQoEparamStatistics exception");
                stringBuilder.append(e2.toString());
                log(stringBuilder.toString());
                return;
            }
        }
        log("uploadChannelQoEParmStatistics: upload condition not allowed.");
    }
}
