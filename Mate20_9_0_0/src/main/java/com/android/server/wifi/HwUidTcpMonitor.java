package com.android.server.wifi;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import com.android.server.wifi.wifipro.HwAutoConnectManager;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

public class HwUidTcpMonitor {
    private static final int CMD_UPDATE_TCP_STATISTICS = 101;
    private static final int MAX_UID_DNS_SIZE = 8;
    private static final int MAX_UID_SIZE = 20;
    private static final String TAG = "HwUidTcpMonitor";
    private static String TCP_STAT_PATH = "proc/net/wifipro_tcp_stat";
    public static final String[] TCP_WEB_STAT_ITEM = new String[]{"UID", "WEBSENDSEGS", "WEBRESENDSEGS", "WEBRECVSEGS", "WEBRTTDURATION", "WEBRTTSEGS"};
    private static HwUidTcpMonitor hwUidTcpMonitor = null;
    private String DNS_SEPORATOR = "/";
    private String MOBILE_UID_TAG = "mobile UID state:";
    private String TCP_SEPORATOR = "\t";
    private String UID_COUNT_SEPORATOR = "-";
    private String WLAN_UID_TAG = "wlan UID state:";
    private Context mContext = null;
    private Handler mHandler;
    private int mLastDnsFailedCnt = -1;
    private int mLastTopUid = 0;
    private int mLastUidDnsFailedCnt = -1;
    private Object mTcpStatisticsLock = new Object();
    private HashMap<Integer, UidTcpStatInfo> mUidTcpStatInfo = new HashMap();
    private AtomicBoolean mWifiMonitorEnabled = new AtomicBoolean(false);

    static class UidTcpStatInfo {
        public long mLastUpdateTime = 0;
        public String mPacketName = "";
        public long mRcvSegs;
        public long mResendSegs;
        public long mRttDuration;
        public long mRttSegs;
        public long mSendSegs;
        public int mUid;

        public UidTcpStatInfo(int uid, long sendSegs, long resendSegs, long rcvSegs, long rttDuration, long rttSegs) {
            this.mUid = uid;
            this.mSendSegs = sendSegs;
            this.mResendSegs = resendSegs;
            this.mRcvSegs = rcvSegs;
            this.mRttDuration = rttDuration;
            this.mRttSegs = rttSegs;
        }

        public String toString() {
            StringBuilder sbuf = new StringBuilder();
            sbuf.append("[");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" mUid = ");
            stringBuilder.append(this.mUid);
            sbuf.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mPacketName = ");
            stringBuilder.append(this.mPacketName);
            sbuf.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mSendSegs = ");
            stringBuilder.append(this.mSendSegs);
            sbuf.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mResendSegs = ");
            stringBuilder.append(this.mResendSegs);
            sbuf.append(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(" mRcvSegs = ");
            stringBuilder.append(this.mRcvSegs);
            sbuf.append(stringBuilder.toString());
            sbuf.append(" ]");
            return sbuf.toString();
        }
    }

    public HwUidTcpMonitor(Context context) {
        this.mContext = context;
        init();
    }

    public static synchronized HwUidTcpMonitor getInstance(Context context) {
        HwUidTcpMonitor hwUidTcpMonitor;
        synchronized (HwUidTcpMonitor.class) {
            if (hwUidTcpMonitor == null) {
                hwUidTcpMonitor = new HwUidTcpMonitor(context);
            }
            hwUidTcpMonitor = hwUidTcpMonitor;
        }
        return hwUidTcpMonitor;
    }

    private void init() {
        HandlerThread handlerThread = new HandlerThread("wifipro_uid_tcp_monitor_handler_thread");
        handlerThread.start();
        this.mHandler = new Handler(handlerThread.getLooper()) {
            public void handleMessage(Message msg) {
                if (msg.what == 101) {
                    HwUidTcpMonitor.this.readAndParseTcpStatistics();
                }
                super.handleMessage(msg);
            }
        };
    }

    private Integer[] getSelectedColumnIdx(String columnNames) {
        Integer[] selectedColumnIdx = new Integer[TCP_WEB_STAT_ITEM.length];
        String[] cols = columnNames.split(this.TCP_SEPORATOR);
        for (int i = 0; i < TCP_WEB_STAT_ITEM.length; i++) {
            selectedColumnIdx[i] = Integer.valueOf(-1);
            String selectedColumnName = TCP_WEB_STAT_ITEM[i];
            for (int j = 0; j < cols.length; j++) {
                if (selectedColumnName.equals(cols[j])) {
                    selectedColumnIdx[i] = Integer.valueOf(j);
                    break;
                }
            }
        }
        return selectedColumnIdx;
    }

    private int getOldestUidUpdated(Map<Integer, UidTcpStatInfo> uidTcpStatInfo) {
        int oldestUid = -1;
        long oldestUpdatedTime = Long.MAX_VALUE;
        for (Entry entry : uidTcpStatInfo.entrySet()) {
            Integer currKey = (Integer) entry.getKey();
            UidTcpStatInfo tmp = (UidTcpStatInfo) entry.getValue();
            if (tmp != null && tmp.mLastUpdateTime < oldestUpdatedTime) {
                oldestUpdatedTime = tmp.mLastUpdateTime;
                oldestUid = currKey.intValue();
            }
        }
        return oldestUid;
    }

    private void checkWifiInternetCapability(int txCnt, int rxCnt, int reTxCnt) {
        int currDnsFailedCnt = HwSelfCureUtils.getCurrentDnsFailedCounter();
        HwSelfCureEngine.getInstance().notifyTcpStatResults(txCnt, rxCnt, reTxCnt, this.mLastDnsFailedCnt >= 0 ? currDnsFailedCnt - this.mLastDnsFailedCnt : 0);
        this.mLastDnsFailedCnt = currDnsFailedCnt;
    }

    /* JADX WARNING: Removed duplicated region for block: B:126:0x0214  */
    /* JADX WARNING: Removed duplicated region for block: B:121:0x01d0 A:{SYNTHETIC, Splitter:B:121:0x01d0} */
    /* JADX WARNING: Removed duplicated region for block: B:62:0x0117  */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x0110 A:{SYNTHETIC, Splitter:B:60:0x0110} */
    /* JADX WARNING: Removed duplicated region for block: B:70:0x0127  */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x011f A:{SYNTHETIC, Splitter:B:67:0x011f} */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x0137  */
    /* JADX WARNING: Removed duplicated region for block: B:75:0x012f A:{SYNTHETIC, Splitter:B:75:0x012f} */
    /* JADX WARNING: Removed duplicated region for block: B:89:0x0152  */
    /* JADX WARNING: Removed duplicated region for block: B:83:0x013f A:{SYNTHETIC, Splitter:B:83:0x013f} */
    /* JADX WARNING: Removed duplicated region for block: B:99:0x0165  */
    /* JADX WARNING: Removed duplicated region for block: B:94:0x015a A:{SYNTHETIC, Splitter:B:94:0x015a} */
    /* JADX WARNING: Removed duplicated region for block: B:107:0x017a  */
    /* JADX WARNING: Removed duplicated region for block: B:121:0x01d0 A:{SYNTHETIC, Splitter:B:121:0x01d0} */
    /* JADX WARNING: Removed duplicated region for block: B:126:0x0214  */
    /* JADX WARNING: Missing block: B:168:0x030e, code skipped:
            r43 = r3;
            r2 = r4;
            r39 = r12;
            r42 = r14;
            r45 = r15;
            r3 = r1;
            r14 = r11;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void parseWlanUidTcpStatistics(List<String> tcpStatLines) {
        Throwable uid;
        int topUid;
        Integer[] numArr;
        HwUidTcpMonitor hwUidTcpMonitor = this;
        List autoConnectManager = tcpStatLines;
        int topUid2 = -1;
        HwAutoConnectManager autoConnectManager2 = HwAutoConnectManager.getInstance();
        if (autoConnectManager2 != null) {
            topUid2 = autoConnectManager2.getCurrentTopUid();
        }
        int wlanUidIdx = getWlanUidStatLineNumber(tcpStatLines);
        int i;
        int i2;
        int i3;
        HwAutoConnectManager hwAutoConnectManager;
        if (wlanUidIdx == -1) {
            i = wlanUidIdx;
            i2 = topUid2;
            topUid2 = hwUidTcpMonitor;
            i3 = i2;
        } else if (tcpStatLines.size() <= wlanUidIdx + 2) {
            hwAutoConnectManager = autoConnectManager2;
            i2 = topUid2;
            topUid2 = hwUidTcpMonitor;
            i3 = i2;
        } else {
            Integer[] selectedColumnIdx = hwUidTcpMonitor.getSelectedColumnIdx((String) autoConnectManager.get(wlanUidIdx + 1));
            Object obj = hwUidTcpMonitor.mTcpStatisticsLock;
            synchronized (obj) {
                Object obj2;
                HwUidTcpMonitor hwUidTcpMonitor2;
                int deltaTxCnt = 0;
                int deltaRxCnt = 0;
                int deltaReTxCnt = 0;
                int i4 = wlanUidIdx + 2;
                boolean isNotifyByTcp = false;
                while (true) {
                    int i5 = i4;
                    Integer[] numArr2;
                    try {
                        if (i5 < tcpStatLines.size()) {
                            if (!((String) autoConnectManager.get(i5)).startsWith("custom ip")) {
                                if (((String) autoConnectManager.get(i5)).length() == 0) {
                                    break;
                                } else if (((String) autoConnectManager.get(i5)).startsWith(hwUidTcpMonitor.MOBILE_UID_TAG)) {
                                    break;
                                } else {
                                    Integer[] selectedTcpStatsValues;
                                    int j;
                                    List<String> list;
                                    int resndSeg;
                                    String[] tcpStatValues;
                                    int resndSeg2;
                                    int i6;
                                    boolean isNotifyByTcp2;
                                    int rttDur;
                                    int deltaTxCnt2;
                                    int deltaReTxCnt2;
                                    HwWifiConnectivityMonitor monitor;
                                    int resndSeg3;
                                    int rcvSeg;
                                    UidTcpStatInfo lastUidTcpStatInfo;
                                    Integer[] numArr3;
                                    String[] strArr;
                                    String[] tcpStatValues2 = ((String) autoConnectManager.get(i5)).split(hwUidTcpMonitor.TCP_SEPORATOR);
                                    Integer[] selectedTcpStatsValues2 = new Integer[selectedColumnIdx.length];
                                    int i7 = 0;
                                    int j2 = 0;
                                    while (true) {
                                        selectedTcpStatsValues = selectedTcpStatsValues2;
                                        j = j2;
                                        if (j >= selectedTcpStatsValues.length) {
                                            break;
                                        }
                                        try {
                                            selectedTcpStatsValues[j] = Integer.valueOf(i7);
                                            if (selectedColumnIdx[j].intValue() >= 0 && selectedColumnIdx[j].intValue() < tcpStatValues2.length) {
                                                selectedTcpStatsValues[j] = Integer.valueOf(Integer.parseInt(tcpStatValues2[selectedColumnIdx[j].intValue()]));
                                            }
                                            j2 = j + 1;
                                            selectedTcpStatsValues2 = selectedTcpStatsValues;
                                            i7 = 0;
                                            list = tcpStatLines;
                                        } catch (NumberFormatException e) {
                                            hwUidTcpMonitor.LOGD("parseWlanUidTcpStatistics NumberFormatException rcv!");
                                            return;
                                        } catch (Throwable th) {
                                            uid = th;
                                            i = wlanUidIdx;
                                            numArr2 = selectedColumnIdx;
                                            obj2 = obj;
                                        }
                                    }
                                    int uid2 = -1;
                                    int sndSeg;
                                    if (selectedTcpStatsValues.length > 0) {
                                        sndSeg = 0;
                                        if (selectedTcpStatsValues[0].intValue() > 0) {
                                            i7 = selectedTcpStatsValues[0].intValue();
                                            resndSeg = 0;
                                            if (selectedTcpStatsValues.length <= 1) {
                                                j = selectedTcpStatsValues[1].intValue();
                                            } else {
                                                j = sndSeg;
                                            }
                                            tcpStatValues = tcpStatValues2;
                                            if (selectedTcpStatsValues.length <= 2) {
                                                resndSeg2 = selectedTcpStatsValues[2].intValue();
                                            } else {
                                                resndSeg2 = resndSeg;
                                            }
                                            i6 = i5;
                                            if (selectedTcpStatsValues.length <= 3) {
                                                i5 = selectedTcpStatsValues[3].intValue();
                                            } else {
                                                i5 = 0;
                                            }
                                            isNotifyByTcp2 = isNotifyByTcp;
                                            if (selectedTcpStatsValues.length <= 4) {
                                                try {
                                                    rttDur = selectedTcpStatsValues[4].intValue();
                                                } catch (Throwable th2) {
                                                    uid = th2;
                                                    j = autoConnectManager2;
                                                    i = wlanUidIdx;
                                                    obj2 = obj;
                                                    i2 = topUid2;
                                                    hwUidTcpMonitor2 = hwUidTcpMonitor;
                                                    i3 = i2;
                                                    while (true) {
                                                        try {
                                                            break;
                                                        } catch (Throwable th3) {
                                                            uid = th3;
                                                        }
                                                    }
                                                    throw uid;
                                                }
                                            }
                                            rttDur = 0;
                                            i = wlanUidIdx;
                                            if (selectedTcpStatsValues.length <= 5) {
                                                try {
                                                    wlanUidIdx = selectedTcpStatsValues[5].intValue();
                                                } catch (Throwable th4) {
                                                    uid = th4;
                                                    hwAutoConnectManager = autoConnectManager2;
                                                    obj2 = obj;
                                                    i2 = topUid2;
                                                    hwUidTcpMonitor2 = hwUidTcpMonitor;
                                                    i3 = i2;
                                                    while (true) {
                                                        break;
                                                    }
                                                    throw uid;
                                                }
                                            }
                                            wlanUidIdx = 0;
                                            deltaTxCnt2 = deltaTxCnt + j;
                                            j2 = deltaRxCnt + i5;
                                            deltaReTxCnt2 = deltaReTxCnt + resndSeg2;
                                            if (i7 != -1 && topUid2 == i7) {
                                                monitor = HwWifiConnectivityMonitor.getInstance();
                                                if (monitor != null) {
                                                    deltaRxCnt = topUid2;
                                                    resndSeg3 = resndSeg2;
                                                    uid2 = i6;
                                                    rcvSeg = i5;
                                                    topUid = topUid2;
                                                    topUid2 = rttDur;
                                                    numArr2 = selectedColumnIdx;
                                                    try {
                                                        isNotifyByTcp = monitor.notifyTopUidTcpInfo(deltaRxCnt, j, i5, resndSeg2, rttDur, wlanUidIdx);
                                                        lastUidTcpStatInfo = (UidTcpStatInfo) hwUidTcpMonitor.mUidTcpStatInfo.get(Integer.valueOf(i7));
                                                        if (lastUidTcpStatInfo != null) {
                                                            try {
                                                                lastUidTcpStatInfo.mSendSegs += (long) j;
                                                                lastUidTcpStatInfo.mResendSegs += (long) resndSeg3;
                                                                lastUidTcpStatInfo.mRcvSegs += (long) rcvSeg;
                                                                lastUidTcpStatInfo.mRttDuration += (long) topUid2;
                                                                lastUidTcpStatInfo.mRttSegs += (long) wlanUidIdx;
                                                                lastUidTcpStatInfo.mLastUpdateTime = System.currentTimeMillis();
                                                                StringBuilder stringBuilder = new StringBuilder();
                                                                stringBuilder.append("parseWlanUidTcpStatistics lastUidTcpStatInfo = ");
                                                                stringBuilder.append(lastUidTcpStatInfo);
                                                                hwUidTcpMonitor.LOGD(stringBuilder.toString());
                                                                hwUidTcpMonitor2 = hwUidTcpMonitor;
                                                                hwAutoConnectManager = autoConnectManager2;
                                                                obj2 = obj;
                                                            } catch (Throwable th5) {
                                                                uid = th5;
                                                                hwUidTcpMonitor2 = hwUidTcpMonitor;
                                                                hwAutoConnectManager = autoConnectManager2;
                                                                obj2 = obj;
                                                                while (true) {
                                                                    break;
                                                                }
                                                                throw uid;
                                                            }
                                                        }
                                                        i4 = resndSeg3;
                                                        if (hwUidTcpMonitor.mUidTcpStatInfo.size() == 20) {
                                                            deltaRxCnt = hwUidTcpMonitor.getOldestUidUpdated(hwUidTcpMonitor.mUidTcpStatInfo);
                                                            hwUidTcpMonitor.mUidTcpStatInfo.remove(Integer.valueOf(deltaRxCnt));
                                                            StringBuilder stringBuilder2 = new StringBuilder();
                                                            stringBuilder2.append("parseWlanUidTcpStatistics rm oldestUid = ");
                                                            stringBuilder2.append(deltaRxCnt);
                                                            stringBuilder2.append(", current size = ");
                                                            stringBuilder2.append(hwUidTcpMonitor.mUidTcpStatInfo.size());
                                                            hwUidTcpMonitor.LOGD(stringBuilder2.toString());
                                                        }
                                                        if (i7 != -1) {
                                                            if (hwUidTcpMonitor.mUidTcpStatInfo.size() < 20) {
                                                                UidTcpStatInfo newUidTcpStatInfo;
                                                                obj2 = obj;
                                                                HwAutoConnectManager autoConnectManager3 = autoConnectManager2;
                                                                UidTcpStatInfo uidTcpStatInfo = uidTcpStatInfo;
                                                                try {
                                                                    uidTcpStatInfo = new UidTcpStatInfo(i7, (long) j, (long) i4, (long) rcvSeg, (long) topUid2, (long) wlanUidIdx);
                                                                    newUidTcpStatInfo = r44;
                                                                    if (autoConnectManager3 != null) {
                                                                        hwAutoConnectManager = autoConnectManager3;
                                                                        try {
                                                                            newUidTcpStatInfo.mPacketName = hwAutoConnectManager.getCurrentPackageName();
                                                                        } catch (Throwable th6) {
                                                                            uid = th6;
                                                                            i3 = topUid;
                                                                        }
                                                                    } else {
                                                                        hwAutoConnectManager = autoConnectManager3;
                                                                    }
                                                                } catch (Throwable th7) {
                                                                    uid = th7;
                                                                    hwAutoConnectManager = autoConnectManager3;
                                                                    i3 = topUid;
                                                                    while (true) {
                                                                        break;
                                                                    }
                                                                    throw uid;
                                                                }
                                                                try {
                                                                    newUidTcpStatInfo.mLastUpdateTime = System.currentTimeMillis();
                                                                    hwUidTcpMonitor2 = this;
                                                                    try {
                                                                        hwUidTcpMonitor2.mUidTcpStatInfo.put(Integer.valueOf(i7), newUidTcpStatInfo);
                                                                        StringBuilder stringBuilder3 = new StringBuilder();
                                                                        stringBuilder3.append("parseWlanUidTcpStatistics newUidTcpStatInfo = ");
                                                                        stringBuilder3.append(newUidTcpStatInfo);
                                                                        hwUidTcpMonitor2.LOGD(stringBuilder3.toString());
                                                                    } catch (Throwable th8) {
                                                                        uid = th8;
                                                                    }
                                                                } catch (Throwable th9) {
                                                                    uid = th9;
                                                                    while (true) {
                                                                        break;
                                                                    }
                                                                    throw uid;
                                                                }
                                                            }
                                                        }
                                                        hwUidTcpMonitor2 = hwUidTcpMonitor;
                                                        hwAutoConnectManager = autoConnectManager2;
                                                        obj2 = obj;
                                                        i4 = uid2 + 1;
                                                        autoConnectManager2 = hwAutoConnectManager;
                                                        hwUidTcpMonitor = hwUidTcpMonitor2;
                                                        deltaTxCnt = deltaTxCnt2;
                                                        deltaRxCnt = j2;
                                                        deltaReTxCnt = deltaReTxCnt2;
                                                        wlanUidIdx = i;
                                                        selectedColumnIdx = numArr2;
                                                        topUid2 = topUid;
                                                        obj = obj2;
                                                        list = tcpStatLines;
                                                    } catch (Throwable th10) {
                                                        uid = th10;
                                                        topUid2 = hwUidTcpMonitor;
                                                        obj2 = obj;
                                                        while (true) {
                                                            break;
                                                        }
                                                        throw uid;
                                                    }
                                                }
                                            }
                                            topUid = topUid2;
                                            resndSeg3 = resndSeg2;
                                            topUid2 = rttDur;
                                            numArr3 = selectedTcpStatsValues;
                                            numArr2 = selectedColumnIdx;
                                            strArr = tcpStatValues;
                                            uid2 = i6;
                                            rcvSeg = i5;
                                            isNotifyByTcp = isNotifyByTcp2;
                                            lastUidTcpStatInfo = (UidTcpStatInfo) hwUidTcpMonitor.mUidTcpStatInfo.get(Integer.valueOf(i7));
                                            if (lastUidTcpStatInfo != null) {
                                            }
                                            i4 = uid2 + 1;
                                            autoConnectManager2 = hwAutoConnectManager;
                                            hwUidTcpMonitor = hwUidTcpMonitor2;
                                            deltaTxCnt = deltaTxCnt2;
                                            deltaRxCnt = j2;
                                            deltaReTxCnt = deltaReTxCnt2;
                                            wlanUidIdx = i;
                                            selectedColumnIdx = numArr2;
                                            topUid2 = topUid;
                                            obj = obj2;
                                            list = tcpStatLines;
                                        }
                                    } else {
                                        sndSeg = 0;
                                    }
                                    i7 = uid2;
                                    resndSeg = 0;
                                    if (selectedTcpStatsValues.length <= 1) {
                                    }
                                    try {
                                        tcpStatValues = tcpStatValues2;
                                        if (selectedTcpStatsValues.length <= 2) {
                                        }
                                        i6 = i5;
                                        if (selectedTcpStatsValues.length <= 3) {
                                        }
                                        isNotifyByTcp2 = isNotifyByTcp;
                                        if (selectedTcpStatsValues.length <= 4) {
                                        }
                                    } catch (Throwable th11) {
                                        uid = th11;
                                        topUid = topUid2;
                                        hwAutoConnectManager = autoConnectManager2;
                                        i = wlanUidIdx;
                                        numArr2 = selectedColumnIdx;
                                        obj2 = obj;
                                        topUid2 = hwUidTcpMonitor;
                                        selectedColumnIdx = isNotifyByTcp;
                                        i3 = topUid;
                                        while (true) {
                                            break;
                                        }
                                        throw uid;
                                    }
                                    try {
                                        i = wlanUidIdx;
                                        if (selectedTcpStatsValues.length <= 5) {
                                        }
                                        deltaTxCnt2 = deltaTxCnt + j;
                                        j2 = deltaRxCnt + i5;
                                        deltaReTxCnt2 = deltaReTxCnt + resndSeg2;
                                    } catch (Throwable th12) {
                                        uid = th12;
                                        topUid = topUid2;
                                        hwAutoConnectManager = autoConnectManager2;
                                        i = wlanUidIdx;
                                        numArr2 = selectedColumnIdx;
                                        obj2 = obj;
                                        topUid2 = hwUidTcpMonitor;
                                        numArr = isNotifyByTcp2;
                                        i3 = topUid;
                                        while (true) {
                                            break;
                                        }
                                        throw uid;
                                    }
                                    try {
                                        monitor = HwWifiConnectivityMonitor.getInstance();
                                        if (monitor != null) {
                                        }
                                        topUid = topUid2;
                                        resndSeg3 = resndSeg2;
                                        topUid2 = rttDur;
                                        numArr3 = selectedTcpStatsValues;
                                        numArr2 = selectedColumnIdx;
                                        strArr = tcpStatValues;
                                        uid2 = i6;
                                        rcvSeg = i5;
                                        isNotifyByTcp = isNotifyByTcp2;
                                    } catch (Throwable th13) {
                                        uid = th13;
                                        numArr2 = selectedColumnIdx;
                                        j = autoConnectManager2;
                                        rttDur = isNotifyByTcp2;
                                        obj2 = obj;
                                        i2 = topUid2;
                                        topUid2 = hwUidTcpMonitor;
                                        i3 = i2;
                                        while (true) {
                                            break;
                                        }
                                        throw uid;
                                    }
                                    try {
                                        lastUidTcpStatInfo = (UidTcpStatInfo) hwUidTcpMonitor.mUidTcpStatInfo.get(Integer.valueOf(i7));
                                        if (lastUidTcpStatInfo != null) {
                                        }
                                        i4 = uid2 + 1;
                                        autoConnectManager2 = hwAutoConnectManager;
                                        hwUidTcpMonitor = hwUidTcpMonitor2;
                                        deltaTxCnt = deltaTxCnt2;
                                        deltaRxCnt = j2;
                                        deltaReTxCnt = deltaReTxCnt2;
                                        wlanUidIdx = i;
                                        selectedColumnIdx = numArr2;
                                        topUid2 = topUid;
                                        obj = obj2;
                                        list = tcpStatLines;
                                    } catch (Throwable th14) {
                                        uid = th14;
                                        topUid2 = hwUidTcpMonitor;
                                        hwAutoConnectManager = autoConnectManager2;
                                        obj2 = obj;
                                        i3 = topUid;
                                        while (true) {
                                            break;
                                        }
                                        throw uid;
                                    }
                                }
                            }
                            break;
                        }
                        break;
                    } catch (Throwable th15) {
                        uid = th15;
                        hwAutoConnectManager = autoConnectManager2;
                        i = wlanUidIdx;
                        numArr2 = selectedColumnIdx;
                        obj2 = obj;
                        selectedColumnIdx = isNotifyByTcp;
                        i2 = topUid2;
                        topUid2 = hwUidTcpMonitor;
                        i3 = i2;
                        while (true) {
                            break;
                        }
                        throw uid;
                    }
                }
                topUid = topUid2;
                obj2 = obj;
                hwUidTcpMonitor2 = hwUidTcpMonitor;
                selectedColumnIdx = isNotifyByTcp;
                try {
                    hwUidTcpMonitor2.parseWlanUidDnsStatistics(topUid, selectedColumnIdx);
                } catch (Throwable th16) {
                    uid = th16;
                    i3 = topUid;
                    numArr = selectedColumnIdx;
                    while (true) {
                        break;
                    }
                    throw uid;
                }
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:30:0x0066  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void parseWlanUidDnsStatistics(int topUid, boolean isNotifyByTcp) {
        String[] uidAndCount = new String[2];
        int deltaDnsFailCnt = 0;
        String dnsFailCountStr = SystemProperties.get("hw.wifipro.uid_dns_fail_count", "0");
        if (dnsFailCountStr != null) {
            String[] uidCountArray = dnsFailCountStr.split(this.DNS_SEPORATOR);
            if (uidCountArray.length <= 8) {
                HwWifiConnectivityMonitor monitor;
                int newDnsFailCount = 0;
                int newUid = 0;
                int i = 0;
                while (i < uidCountArray.length) {
                    uidAndCount = uidCountArray[i].split(this.UID_COUNT_SEPORATOR, 2);
                    if (uidAndCount.length == 2) {
                        try {
                            newUid = Integer.parseInt(uidAndCount[0]);
                            newDnsFailCount = Integer.parseInt(uidAndCount[1]);
                            if (newUid != topUid) {
                                i++;
                            } else {
                                int deltaDnsFailCnt2;
                                if (topUid != this.mLastTopUid || this.mLastTopUid == 0) {
                                    this.mLastTopUid = topUid;
                                    deltaDnsFailCnt2 = 0;
                                } else {
                                    deltaDnsFailCnt2 = newDnsFailCount - this.mLastUidDnsFailedCnt;
                                }
                                deltaDnsFailCnt = deltaDnsFailCnt2;
                                this.mLastUidDnsFailedCnt = newDnsFailCount;
                                if (deltaDnsFailCnt > 0 && !isNotifyByTcp) {
                                    monitor = HwWifiConnectivityMonitor.getInstance();
                                    if (monitor != null) {
                                        monitor.notifyTopUidDnsInfo(topUid, deltaDnsFailCnt);
                                    }
                                }
                            }
                        } catch (NumberFormatException e) {
                            return;
                        }
                    }
                    return;
                }
                monitor = HwWifiConnectivityMonitor.getInstance();
                if (monitor != null) {
                }
            }
        }
    }

    public static List<String> getFileResult(String fileName) {
        List<String> result = new ArrayList();
        FileInputStream f = null;
        BufferedReader dr = null;
        try {
            f = new FileInputStream(fileName);
            dr = new BufferedReader(new InputStreamReader(f, "US-ASCII"));
            String readLine = dr.readLine();
            while (true) {
                String line = readLine;
                if (line == null) {
                    break;
                }
                line = line.trim();
                if (!line.equals("")) {
                    result.add(line);
                }
                readLine = dr.readLine();
            }
            dr.close();
            f.close();
            try {
                dr.close();
            } catch (IOException e) {
                Log.e(TAG, "getFileResult throw IOException when close BufferedReader");
            }
            try {
                f.close();
            } catch (IOException e2) {
                Log.e(TAG, "getFileResult throw IOException when close FileInputStream");
            }
        } catch (FileNotFoundException e3) {
            Log.e(TAG, "getFileResult throw FileNotFoundException");
            if (dr != null) {
                try {
                    dr.close();
                } catch (IOException e4) {
                    Log.e(TAG, "getFileResult throw IOException when close BufferedReader");
                }
            }
            if (f != null) {
                f.close();
            }
        } catch (IOException e5) {
            Log.e(TAG, "getFileResult throw IOException");
            if (dr != null) {
                try {
                    dr.close();
                } catch (IOException e6) {
                    Log.e(TAG, "getFileResult throw IOException when close BufferedReader");
                }
            }
            if (f != null) {
                f.close();
            }
        } catch (Throwable th) {
            if (dr != null) {
                try {
                    dr.close();
                } catch (IOException e7) {
                    Log.e(TAG, "getFileResult throw IOException when close BufferedReader");
                }
            }
            if (f != null) {
                try {
                    f.close();
                } catch (IOException e8) {
                    Log.e(TAG, "getFileResult throw IOException when close FileInputStream");
                }
            }
        }
        return result;
    }

    private void readAndParseTcpStatistics() {
        List<String> tcpStatLines = getFileResult(TCP_STAT_PATH);
        if (tcpStatLines.size() != 0) {
            parseWlanUidTcpStatistics(tcpStatLines);
        }
    }

    public synchronized HashMap<Integer, UidTcpStatInfo> getUidTcpStatistics() {
        HashMap<Integer, UidTcpStatInfo> cloneMap;
        synchronized (this.mTcpStatisticsLock) {
            cloneMap = (HashMap) this.mUidTcpStatInfo.clone();
        }
        return cloneMap;
    }

    public synchronized boolean isAppAccessInternet(int appUid) {
        boolean z;
        synchronized (this.mTcpStatisticsLock) {
            UidTcpStatInfo matchedUid = null;
            if (appUid != -1) {
                try {
                    matchedUid = (UidTcpStatInfo) this.mUidTcpStatInfo.get(Integer.valueOf(appUid));
                } catch (Throwable th) {
                    while (true) {
                    }
                }
            }
            z = matchedUid != null;
        }
        return z;
    }

    public synchronized long getRttDuration(int appUid, int wifiState) {
        synchronized (this.mTcpStatisticsLock) {
            UidTcpStatInfo matchedUid = null;
            if (appUid != -1) {
                try {
                    matchedUid = (UidTcpStatInfo) this.mUidTcpStatInfo.get(Integer.valueOf(appUid));
                } catch (Throwable th) {
                    while (true) {
                    }
                }
            }
            if (matchedUid != null) {
                long j = matchedUid.mRttDuration;
                return j;
            }
            return 0;
        }
    }

    public synchronized long getRttSegs(int appUid, int wifiState) {
        synchronized (this.mTcpStatisticsLock) {
            UidTcpStatInfo matchedUid = null;
            if (appUid != -1) {
                try {
                    matchedUid = (UidTcpStatInfo) this.mUidTcpStatInfo.get(Integer.valueOf(appUid));
                } catch (Throwable th) {
                    while (true) {
                    }
                }
            }
            if (matchedUid != null) {
                long j = matchedUid.mRttSegs;
                return j;
            }
            return 0;
        }
    }

    public synchronized void updateUidTcpStatistics() {
        if (this.mWifiMonitorEnabled.get()) {
            this.mHandler.sendEmptyMessage(101);
        }
    }

    public synchronized void notifyWifiMonitorEnabled(boolean enabled) {
        this.mWifiMonitorEnabled.set(enabled);
        if (!this.mWifiMonitorEnabled.get()) {
            synchronized (this.mTcpStatisticsLock) {
                this.mUidTcpStatInfo.clear();
                this.mLastDnsFailedCnt = -1;
            }
        }
    }

    private int getWlanUidStatLineNumber(List<String> tcpStatLines) {
        for (int i = 0; i < tcpStatLines.size(); i++) {
            if (((String) tcpStatLines.get(i)).startsWith(this.WLAN_UID_TAG)) {
                return i;
            }
        }
        return -1;
    }

    private void LOGD(String msg) {
        Log.d(TAG, msg);
    }
}
