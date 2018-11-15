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

    /* JADX WARNING: Missing block: B:43:0x00e6, code:
            r21 = -1;
     */
    /* JADX WARNING: Missing block: B:46:0x00f2, code:
            if (r13.length <= 0) goto L_0x0106;
     */
    /* JADX WARNING: Missing block: B:47:0x00f4, code:
            r22 = 0;
     */
    /* JADX WARNING: Missing block: B:50:0x00fd, code:
            if (r13[0].intValue() <= 0) goto L_0x0108;
     */
    /* JADX WARNING: Missing block: B:51:0x00ff, code:
            r0 = r13[0].intValue();
     */
    /* JADX WARNING: Missing block: B:52:0x0106, code:
            r22 = 0;
     */
    /* JADX WARNING: Missing block: B:53:0x0108, code:
            r0 = r21;
     */
    /* JADX WARNING: Missing block: B:56:0x010b, code:
            r35 = 0;
     */
    /* JADX WARNING: Missing block: B:57:0x010e, code:
            if (r13.length <= 1) goto L_0x0117;
     */
    /* JADX WARNING: Missing block: B:59:?, code:
            r2 = r13[1].intValue();
     */
    /* JADX WARNING: Missing block: B:60:0x0117, code:
            r2 = r22;
     */
    /* JADX WARNING: Missing block: B:63:0x011a, code:
            r36 = r9;
     */
    /* JADX WARNING: Missing block: B:64:0x011d, code:
            if (r13.length <= 2) goto L_0x0127;
     */
    /* JADX WARNING: Missing block: B:67:0x0125, code:
            r9 = r13[2].intValue();
     */
    /* JADX WARNING: Missing block: B:68:0x0127, code:
            r9 = r35;
     */
    /* JADX WARNING: Missing block: B:71:0x012a, code:
            r37 = r10;
     */
    /* JADX WARNING: Missing block: B:72:0x012d, code:
            if (r13.length <= 3) goto L_0x0137;
     */
    /* JADX WARNING: Missing block: B:75:0x0135, code:
            r10 = r13[3].intValue();
     */
    /* JADX WARNING: Missing block: B:76:0x0137, code:
            r10 = 0;
     */
    /* JADX WARNING: Missing block: B:79:0x013a, code:
            r38 = r11;
     */
    /* JADX WARNING: Missing block: B:80:0x013d, code:
            if (r13.length <= 4) goto L_0x0152;
     */
    /* JADX WARNING: Missing block: B:83:0x0145, code:
            r11 = r13[4].intValue();
     */
    /* JADX WARNING: Missing block: B:84:0x0147, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:85:0x0148, code:
            r2 = r4;
            r39 = r12;
     */
    /* JADX WARNING: Missing block: B:87:0x0152, code:
            r11 = 0;
     */
    /* JADX WARNING: Missing block: B:90:0x0155, code:
            r39 = r12;
     */
    /* JADX WARNING: Missing block: B:91:0x0158, code:
            if (r13.length <= 5) goto L_0x0165;
     */
    /* JADX WARNING: Missing block: B:94:0x0160, code:
            r12 = r13[5].intValue();
     */
    /* JADX WARNING: Missing block: B:95:0x0162, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:96:0x0163, code:
            r2 = r4;
     */
    /* JADX WARNING: Missing block: B:97:0x0165, code:
            r12 = 0;
     */
    /* JADX WARNING: Missing block: B:98:0x0167, code:
            r16 = r5 + r2;
            r17 = r6 + r10;
            r18 = r7 + r9;
     */
    /* JADX WARNING: Missing block: B:99:0x016e, code:
            if (r0 == -1) goto L_0x01b1;
     */
    /* JADX WARNING: Missing block: B:100:0x0170, code:
            if (r3 != r0) goto L_0x01b1;
     */
    /* JADX WARNING: Missing block: B:103:0x0176, code:
            r19 = com.android.server.wifi.HwWifiConnectivityMonitor.getInstance();
     */
    /* JADX WARNING: Missing block: B:104:0x0178, code:
            if (r19 == null) goto L_0x01b1;
     */
    /* JADX WARNING: Missing block: B:105:0x017a, code:
            r6 = r3;
            r40 = r9;
            r20 = r36;
            r41 = r13;
            r21 = r37;
            r13 = r10;
            r43 = r3;
            r3 = r11;
            r42 = r14;
     */
    /* JADX WARNING: Missing block: B:108:0x0195, code:
            r11 = r19.notifyTopUidTcpInfo(r6, r2, r10, r9, r11, r12);
     */
    /* JADX WARNING: Missing block: B:109:0x0197, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:110:0x0198, code:
            r3 = r1;
            r2 = r4;
            r11 = r38;
     */
    /* JADX WARNING: Missing block: B:113:0x01a1, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:114:0x01a2, code:
            r42 = r14;
            r2 = r4;
            r11 = r38;
            r45 = r15;
            r49 = r3;
            r3 = r1;
            r1 = r49;
     */
    /* JADX WARNING: Missing block: B:115:0x01b1, code:
            r43 = r3;
            r40 = r9;
            r3 = r11;
            r41 = r13;
            r42 = r14;
            r20 = r36;
            r21 = r37;
            r13 = r10;
            r11 = r38;
     */
    /* JADX WARNING: Missing block: B:117:?, code:
            r5 = (com.android.server.wifi.HwUidTcpMonitor.UidTcpStatInfo) r1.mUidTcpStatInfo.get(java.lang.Integer.valueOf(r0));
     */
    /* JADX WARNING: Missing block: B:118:0x01ce, code:
            if (r5 == null) goto L_0x0214;
     */
    /* JADX WARNING: Missing block: B:120:?, code:
            r5.mSendSegs += (long) r2;
            r5.mResendSegs += (long) r40;
            r5.mRcvSegs += (long) r13;
            r5.mRttDuration += (long) r3;
            r5.mRttSegs += (long) r12;
            r5.mLastUpdateTime = java.lang.System.currentTimeMillis();
            r6 = new java.lang.StringBuilder();
            r6.append("parseWlanUidTcpStatistics lastUidTcpStatInfo = ");
            r6.append(r5);
            r1.LOGD(r6.toString());
     */
    /* JADX WARNING: Missing block: B:121:0x020a, code:
            r3 = r1;
            r2 = r4;
            r45 = r15;
     */
    /* JADX WARNING: Missing block: B:122:0x0210, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:123:0x0211, code:
            r3 = r1;
            r2 = r4;
     */
    /* JADX WARNING: Missing block: B:124:0x0214, code:
            r8 = r40;
     */
    /* JADX WARNING: Missing block: B:128:0x021e, code:
            if (r1.mUidTcpStatInfo.size() != 20) goto L_0x0251;
     */
    /* JADX WARNING: Missing block: B:130:?, code:
            r6 = r1.getOldestUidUpdated(r1.mUidTcpStatInfo);
            r1.mUidTcpStatInfo.remove(java.lang.Integer.valueOf(r6));
            r9 = new java.lang.StringBuilder();
            r9.append("parseWlanUidTcpStatistics rm oldestUid = ");
            r9.append(r6);
            r9.append(", current size = ");
            r9.append(r1.mUidTcpStatInfo.size());
            r1.LOGD(r9.toString());
     */
    /* JADX WARNING: Missing block: B:132:0x0252, code:
            if (r0 == -1) goto L_0x02cb;
     */
    /* JADX WARNING: Missing block: B:135:0x025a, code:
            if (r1.mUidTcpStatInfo.size() >= 20) goto L_0x02cb;
     */
    /* JADX WARNING: Missing block: B:137:0x025e, code:
            r45 = r15;
            r46 = r2;
            r48 = r3;
            r47 = r4;
            r23 = r23;
     */
    /* JADX WARNING: Missing block: B:139:?, code:
            r23 = new com.android.server.wifi.HwUidTcpMonitor.UidTcpStatInfo(r0, (long) r2, (long) r8, (long) r13, (long) r3, (long) r12);
     */
    /* JADX WARNING: Missing block: B:140:0x027e, code:
            r1 = r44;
     */
    /* JADX WARNING: Missing block: B:141:0x0280, code:
            if (r47 == null) goto L_0x0292;
     */
    /* JADX WARNING: Missing block: B:142:0x0282, code:
            r2 = r47;
     */
    /* JADX WARNING: Missing block: B:144:?, code:
            r1.mPacketName = r2.getCurrentPackageName();
     */
    /* JADX WARNING: Missing block: B:145:0x028b, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:146:0x028c, code:
            r1 = r43;
            r3 = r50;
     */
    /* JADX WARNING: Missing block: B:147:0x0292, code:
            r2 = r47;
     */
    /* JADX WARNING: Missing block: B:149:?, code:
            r1.mLastUpdateTime = java.lang.System.currentTimeMillis();
     */
    /* JADX WARNING: Missing block: B:150:0x029a, code:
            r3 = r50;
     */
    /* JADX WARNING: Missing block: B:152:?, code:
            r3.mUidTcpStatInfo.put(java.lang.Integer.valueOf(r0), r1);
            r4 = new java.lang.StringBuilder();
            r4.append("parseWlanUidTcpStatistics newUidTcpStatInfo = ");
            r4.append(r1);
            r3.LOGD(r4.toString());
     */
    /* JADX WARNING: Missing block: B:153:0x02ba, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:154:0x02bd, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:155:0x02be, code:
            r3 = r50;
     */
    /* JADX WARNING: Missing block: B:156:0x02c2, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:157:0x02c3, code:
            r2 = r47;
            r3 = r50;
            r1 = r43;
     */
    /* JADX WARNING: Missing block: B:158:0x02cb, code:
            r3 = r1;
            r2 = r4;
            r45 = r15;
     */
    /* JADX WARNING: Missing block: B:160:0x02e6, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:161:0x02e7, code:
            r3 = r1;
            r2 = r4;
            r45 = r15;
            r1 = r43;
     */
    /* JADX WARNING: Missing block: B:162:0x02ee, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:163:0x02ef, code:
            r43 = r3;
            r2 = r4;
            r39 = r12;
            r42 = r14;
            r45 = r15;
            r3 = r1;
            r11 = r38;
            r1 = r43;
     */
    /* JADX WARNING: Missing block: B:164:0x02ff, code:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:165:0x0300, code:
            r43 = r3;
            r2 = r4;
            r39 = r12;
            r42 = r14;
            r45 = r15;
            r3 = r1;
            r14 = r11;
            r1 = r43;
     */
    /* JADX WARNING: Missing block: B:166:0x030e, code:
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
        int i;
        int i2;
        Object obj;
        Throwable uid;
        Object obj2;
        HwUidTcpMonitor hwUidTcpMonitor;
        HwUidTcpMonitor hwUidTcpMonitor2 = this;
        List autoConnectManager = tcpStatLines;
        int topUid = -1;
        HwAutoConnectManager autoConnectManager2 = HwAutoConnectManager.getInstance();
        if (autoConnectManager2 != null) {
            topUid = autoConnectManager2.getCurrentTopUid();
        }
        int wlanUidIdx = getWlanUidStatLineNumber(tcpStatLines);
        int i3;
        HwAutoConnectManager hwAutoConnectManager;
        if (wlanUidIdx == -1) {
            i3 = wlanUidIdx;
            i = topUid;
            topUid = hwUidTcpMonitor2;
            i2 = i;
        } else if (tcpStatLines.size() <= wlanUidIdx + 2) {
            hwAutoConnectManager = autoConnectManager2;
            i = topUid;
            topUid = hwUidTcpMonitor2;
            i2 = i;
        } else {
            Integer[] selectedColumnIdx = hwUidTcpMonitor2.getSelectedColumnIdx((String) autoConnectManager.get(wlanUidIdx + 1));
            obj = hwUidTcpMonitor2.mTcpStatisticsLock;
            synchronized (obj) {
                int deltaTxCnt = 0;
                int deltaRxCnt = 0;
                int deltaReTxCnt = 0;
                int i4 = wlanUidIdx + 2;
                boolean isNotifyByTcp = false;
                while (true) {
                    int i5 = i4;
                    Integer[] numArr;
                    try {
                        int j;
                        List<String> list;
                        if (i5 < tcpStatLines.size()) {
                            if (!((String) autoConnectManager.get(i5)).startsWith("custom ip")) {
                                if (((String) autoConnectManager.get(i5)).length() != 0) {
                                    if (!((String) autoConnectManager.get(i5)).startsWith(hwUidTcpMonitor2.MOBILE_UID_TAG)) {
                                        String[] tcpStatValues = ((String) autoConnectManager.get(i5)).split(hwUidTcpMonitor2.TCP_SEPORATOR);
                                        Integer[] selectedTcpStatsValues = new Integer[selectedColumnIdx.length];
                                        int i6 = 0;
                                        j = 0;
                                        while (true) {
                                            Integer[] selectedTcpStatsValues2 = selectedTcpStatsValues;
                                            int j2 = j;
                                            if (j2 >= selectedTcpStatsValues2.length) {
                                                break;
                                            }
                                            try {
                                                selectedTcpStatsValues2[j2] = Integer.valueOf(i6);
                                                if (selectedColumnIdx[j2].intValue() >= 0 && selectedColumnIdx[j2].intValue() < tcpStatValues.length) {
                                                    selectedTcpStatsValues2[j2] = Integer.valueOf(Integer.parseInt(tcpStatValues[selectedColumnIdx[j2].intValue()]));
                                                }
                                                j = j2 + 1;
                                                selectedTcpStatsValues = selectedTcpStatsValues2;
                                                i6 = 0;
                                                list = tcpStatLines;
                                            } catch (NumberFormatException e) {
                                                hwUidTcpMonitor2.LOGD("parseWlanUidTcpStatistics NumberFormatException rcv!");
                                                return;
                                            } catch (Throwable th) {
                                                uid = th;
                                                i3 = wlanUidIdx;
                                                numArr = selectedColumnIdx;
                                                obj2 = obj;
                                            }
                                        }
                                    } else {
                                        break;
                                    }
                                }
                                break;
                            }
                            break;
                        }
                        break;
                        i4 = i + 1;
                        autoConnectManager2 = hwAutoConnectManager;
                        hwUidTcpMonitor2 = hwUidTcpMonitor;
                        deltaTxCnt = deltaTxCnt;
                        deltaRxCnt = j;
                        deltaReTxCnt = deltaReTxCnt;
                        wlanUidIdx = i3;
                        selectedColumnIdx = numArr;
                        topUid = topUid;
                        obj = obj2;
                        list = tcpStatLines;
                    } catch (Throwable th2) {
                        uid = th2;
                        hwAutoConnectManager = autoConnectManager2;
                        i3 = wlanUidIdx;
                        numArr = selectedColumnIdx;
                        obj2 = obj;
                        selectedColumnIdx = isNotifyByTcp;
                        i = topUid;
                        topUid = hwUidTcpMonitor2;
                        i2 = i;
                    }
                }
                int topUid2 = topUid;
                obj2 = obj;
                hwUidTcpMonitor = hwUidTcpMonitor2;
                selectedColumnIdx = isNotifyByTcp;
                try {
                    hwUidTcpMonitor.parseWlanUidDnsStatistics(topUid2, selectedColumnIdx);
                    return;
                } catch (Throwable th3) {
                    uid = th3;
                    i2 = topUid2;
                    Integer[] numArr2 = selectedColumnIdx;
                    while (true) {
                        try {
                            break;
                        } catch (Throwable th4) {
                            uid = th4;
                        }
                    }
                    throw uid;
                }
            }
        }
        return;
        obj2 = obj;
        i = topUid;
        hwUidTcpMonitor = hwUidTcpMonitor2;
        i2 = i;
        while (true) {
            break;
        }
        throw uid;
        obj2 = obj;
        while (true) {
            break;
        }
        throw uid;
        while (true) {
            break;
        }
        throw uid;
        i = topUid;
        hwUidTcpMonitor = hwUidTcpMonitor2;
        i2 = i;
        while (true) {
            break;
        }
        throw uid;
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
                matchedUid = (UidTcpStatInfo) this.mUidTcpStatInfo.get(Integer.valueOf(appUid));
            }
            z = matchedUid != null;
        }
        return z;
    }

    public synchronized long getRttDuration(int appUid, int wifiState) {
        synchronized (this.mTcpStatisticsLock) {
            UidTcpStatInfo matchedUid = null;
            if (appUid != -1) {
                matchedUid = (UidTcpStatInfo) this.mUidTcpStatInfo.get(Integer.valueOf(appUid));
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
                matchedUid = (UidTcpStatInfo) this.mUidTcpStatInfo.get(Integer.valueOf(appUid));
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
