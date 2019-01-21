package com.android.server.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.IpConfiguration.IpAssignment;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkUtils;
import android.net.RouteInfo;
import android.net.arp.HWArpPeer;
import android.net.arp.HWMultiGW;
import android.net.netlink.NetlinkMessage;
import android.net.netlink.NetlinkSocket;
import android.net.netlink.RtNetlinkMessage;
import android.net.netlink.StructNlMsgHdr;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.INetworkManagementService;
import android.os.INetworkManagementService.Stub;
import android.os.Looper;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.HwServiceFactory;
import com.android.server.wifi.HwQoE.HidataWechatTraffic;
import com.android.server.wifi.HwQoE.HwQoEService;
import com.android.server.wifipro.WifiProCommonUtils;
import com.huawei.ncdft.HwNcDftConnManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;
import libcore.io.IoUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class HwArpVerifier {
    private static final String ACTION_ARP_RECONNECT_WIFI = "android.net.wifi.ARP_RECONNECT_WIFI";
    private static final int ARP_REASSOC_OK = 8;
    private static final String BCM_ROAMING_FLAG_FILE = "";
    private static final boolean BETA_VER;
    private static final boolean DBG = true;
    private static final int DEFAULT_ARP_NUM = 1;
    private static final int DEFAULT_ARP_PING_TIMEOUT_MS = 100;
    private static final int DEFAULT_FUL_PING_TIMEOUT_MS = 5000;
    private static final int DEFAULT_GARP_TIMEOUT_MS = 1000;
    private static final int DEFAULT_MIN_ARP_RESPONSES = 1;
    private static final int DEFAULT_MIN_RESPONSE = 1;
    private static final int DEFAULT_NUM_ARP_PINGS = 2;
    private static final int DEFAULT_SIG_PING_TIMEOUT_MS = 1000;
    private static final String DIAGNOSE_COMPLETE_ACTION = "com.huawei.network.DIAGNOSE_COMPLETE";
    private static final int DYNAMIC_ARP_CHECK = 2;
    private static final int FULL_ARP_CHECK = 1;
    private static final String HI1102_ROAMING_FLAG_FILE = "/sys/hisys/hmac/vap/roam_status";
    private static final String HI110X_ROAMING_FLAG_FILE = "/sys/hi110x/roam_status";
    private static final int HTTP_ACCESS_OK = 200;
    private static final int HTTP_ACCESS_TIMEOUT_RESP = 599;
    private static final String IFACE = "wlan0";
    private static final long LONG_ARP_FAIL_DURATION = 86400000;
    private static final int LONG_ARP_FAIL_TIMES_THRESHOLD = 6;
    private static final int MAX_ARP_FAIL_COUNT = 15;
    public static final int MSG_DUMP_LOG = 1010;
    private static final int MSG_WIFI_ARP_FAILED = 14;
    private static final String PACKAGE_NAME = "HwArpVerifier";
    private static final long SHORT_ARP_FAIL_DURATION = 3600000;
    private static final int SHORT_ARP_FAIL_TIMES_THRESHOLD = 2;
    private static final int SINGLE_ARP_CHECK = 0;
    private static final int SUCT_TIME_COUNT = 3;
    private static final String TAG = "HwArpVerifier";
    private static final int WEAK_SIGNAL_THRESHOLD = -83;
    public static final String WEB_BAIDU = "http://www.baidu.com";
    public static final String WEB_CHINAZ_GETIP = "http://ip.chinaz.com/getip.aspx";
    private static final String WIFI_ARP_TIMEOUT = "/sys/devices/platform/bcmdhd_wlan.1/wifi_arp_timeout";
    private static final int WIFI_STATE_CONNECTED = 1;
    private static final int WIFI_STATE_DISCONNECTED = -1;
    private static final int WIFI_STATE_INITIALED = 0;
    private static final String WIFI_WRONG_ACTION_FLAG = "/sys/devices/platform/bcmdhd_wlan.1/wifi_wrong_action_flag";
    private static HwArpVerifier arp_instance = null;
    private static final int[] dynamicPings = new int[]{1, 2, 4, 5, 5};
    private static ReentrantLock mLock = new ReentrantLock();
    private static int mRSSI = 0;
    private int errCode = 0;
    private boolean isMobileDateActive = false;
    private boolean isRouteRepareSwitchEnabled = SystemProperties.getBoolean("ro.config.hw_route_repare", false);
    private AccessWebStatus mAccessWebStatus = new AccessWebStatus();
    private ArrayList<ArpItem> mArpBlacklist = new ArrayList();
    private ArrayList<ArpItem> mArpItems = new ArrayList();
    private ConnectivityManager mCM = null;
    private int mCheckStateToken = 0;
    private ClientHandler mClientHandler = null;
    private Context mContext = null;
    private int mCurrentWiFiState = 0;
    private boolean mFirstDetect = true;
    private String mGateway = null;
    private HWGatewayVerifier mHWGatewayVerifier = new HWGatewayVerifier(this, null);
    private HwWiFiLogUtils mHwLogUtils = null;
    private HwQoEService mHwQoEService;
    private HwWifiCHRService mHwWifiCHRService = null;
    private int mLastNetworkId = -1;
    private String mLastSSID = null;
    private long mLongDurationStartTime = 0;
    private int mLongTriggerCnt = 0;
    private HWNetstatManager mNetstatManager = null;
    private NetworkInfo mNetworkInfo = new NetworkInfo(1, 0, "WIFI", BCM_ROAMING_FLAG_FILE);
    private INetworkManagementService mNwService;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        /* JADX WARNING: Missing block: B:15:0x005d, code skipped:
            if (com.android.server.wifi.HwArpVerifier.access$200(r4.this$0) == null) goto L_0x0068;
     */
        /* JADX WARNING: Missing block: B:16:0x005f, code skipped:
            com.android.server.wifi.HwArpVerifier.access$200(r4.this$0).monitorWifiNetworkState();
     */
        /* JADX WARNING: Missing block: B:17:0x0068, code skipped:
            com.android.server.wifi.HwArpVerifier.access$302(r4.this$0, (android.net.wifi.WifiManager) com.android.server.wifi.HwArpVerifier.access$400(r4.this$0).getSystemService("wifi"));
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                if (action.equals("android.net.wifi.STATE_CHANGE")) {
                    HwArpVerifier.this.mNetworkInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                    if (HwArpVerifier.this.mNetworkInfo != null) {
                        switch (AnonymousClass2.$SwitchMap$android$net$NetworkInfo$DetailedState[HwArpVerifier.this.mNetworkInfo.getDetailedState().ordinal()]) {
                            case 1:
                                if (!intent.hasExtra("linkProperties")) {
                                    HwArpVerifier.this.mRevLinkProperties = null;
                                    break;
                                } else {
                                    HwArpVerifier.this.mRevLinkProperties = (LinkProperties) intent.getParcelableExtra("linkProperties");
                                    break;
                                }
                            case 2:
                                break;
                        }
                    }
                } else if (action.equals("android.net.wifi.WIFI_STATE_CHANGED")) {
                    HwArpVerifier.this.handleWifiSwitchChanged(intent.getIntExtra("wifi_state", 4));
                } else if (action.equals("android.intent.action.SCREEN_ON")) {
                    if (HwArpVerifier.this.isConnectedToWifi()) {
                        HwArpVerifier.this.stopWifiRouteCheck();
                        HwArpVerifier.this.startWifiRouteCheck();
                    }
                } else if (action.equals("android.intent.action.ANY_DATA_STATE")) {
                    String dataState = intent.getStringExtra("state");
                    if ("CONNECTED".equalsIgnoreCase(dataState)) {
                        HwArpVerifier.this.isMobileDateActive = true;
                    } else if ("DISCONNECTED".equalsIgnoreCase(dataState)) {
                        HwArpVerifier.this.isMobileDateActive = false;
                    }
                }
            }
        }
    };
    private boolean mRegisterReceiver = false;
    private LinkProperties mRevLinkProperties = null;
    private String mRoamingFlagFile = null;
    private int mRouteDetectCnt = 0;
    private Handler mServiceHandler = null;
    private long mShortDurationStartTime = 0;
    private int mShortTriggerCnt = 0;
    private int mSpendTime = 0;
    private HandlerThread mThread = null;
    private WifiManager mWM = null;
    private WifiNative mWifiNative = null;

    /* renamed from: com.android.server.wifi.HwArpVerifier$2 */
    static /* synthetic */ class AnonymousClass2 {
        static final /* synthetic */ int[] $SwitchMap$android$net$NetworkInfo$DetailedState = new int[DetailedState.values().length];

        static {
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[DetailedState.CONNECTED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$net$NetworkInfo$DetailedState[DetailedState.DISCONNECTED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    protected static class AccessWebStatus {
        int mRttArp;
        int mRttBaidu;
        String mStrChinazAddr;
        String mStrChinazIp;

        public AccessWebStatus() {
            reset();
        }

        public void setChinazIp(String ip) {
            this.mStrChinazIp = ip;
        }

        public String getChinazAddr() {
            return this.mStrChinazAddr;
        }

        public void setChinazAddr(String addr) {
            this.mStrChinazAddr = addr;
        }

        public int getRTTArp() {
            return this.mRttArp;
        }

        public void setRTTArp(int arp) {
            this.mRttArp = arp;
        }

        public int getRTTBaidu() {
            return this.mRttBaidu;
        }

        public void setRTTBaidu(int baidu) {
            this.mRttBaidu = baidu;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mStrChinazIp = ");
            stringBuilder.append(this.mStrChinazIp);
            stringBuilder.append(", mStrChinazAddr = ");
            stringBuilder.append(this.mStrChinazAddr);
            stringBuilder.append(", mRttArp = ");
            stringBuilder.append(this.mRttArp);
            stringBuilder.append(", mRttBaidu = ");
            stringBuilder.append(this.mRttBaidu);
            return stringBuilder.toString();
        }

        public void reset() {
            Log.d("HwArpVerifier", "AccessWebStatus : reset all");
        }
    }

    private class ArpItem {
        private static final int ATF_COM = 2;
        private static final int ATF_PERM = 4;
        public static final int MAX_FAIL_CNT = 10;
        public String device;
        private int failcnt;
        public int flag;
        public String hwaddr;
        public String ipaddr;

        public ArpItem(String ip, String mac, int flag, String ifname) {
            this.failcnt = 0;
            this.ipaddr = ip;
            this.hwaddr = mac.toLowerCase(Locale.ENGLISH);
            this.device = ifname;
            this.flag = flag;
        }

        public ArpItem(String mac, int failcnt) {
            this.failcnt = 0;
            this.ipaddr = HwArpVerifier.BCM_ROAMING_FLAG_FILE;
            this.hwaddr = mac.toLowerCase(Locale.ENGLISH);
            this.device = HwArpVerifier.BCM_ROAMING_FLAG_FILE;
            this.flag = 0;
            this.failcnt = failcnt;
        }

        public boolean matchMaxRetried() {
            return this.failcnt >= 10;
        }

        public void putFail() {
            this.failcnt++;
        }

        public boolean sameIpaddress(String ip) {
            return !TextUtils.isEmpty(ip) && ip.equals(this.ipaddr);
        }

        public boolean isStaticArp() {
            return (this.flag & 4) == 4;
        }

        public boolean sameMacAddress(String mac) {
            return mac != null && mac.toLowerCase(Locale.ENGLISH).equals(this.hwaddr);
        }

        public boolean isValid() {
            boolean validFlags = (this.flag & 2) == 2;
            boolean validWlanDevice = HwArpVerifier.IFACE.equals(this.device);
            boolean validMac = this.hwaddr.length() == 17;
            if (validFlags && validWlanDevice && validMac) {
                return true;
            }
            return false;
        }

        public String toString() {
            return String.format(Locale.ENGLISH, "%s %d %s %s", new Object[]{this.ipaddr, Integer.valueOf(this.flag), this.hwaddr, this.device});
        }
    }

    private enum ArpState {
        DONT_CHECK,
        HEART_CHECK,
        NORMAL_CHECK,
        CONFIRM_CHECK,
        DEAD_CHECK
    }

    protected class ClientHandler extends Handler {
        public static final int DEFAULT_WIFI_ROUTE_CHECK_CNT = 5;
        public static final int DEFAULT_WIFI_ROUTE_CHECK_TIME = 50000;
        public static final int DEFAULT_WIFI_ROUTE_CHECK_TIME_FIRST = 5000;
        public static final int DEFAULT_WIFI_ROUTE_CHECK_TIME_QUICK = 15000;
        private static final int MSG_CHECK_WIFI_STATE = 124;
        private static final int MSG_DO_ARP_ASYNC = 125;
        public static final int MSG_DO_ROUTE_CHECK = 126;
        private static final int SOCKET_TIMEOUT_MS = 8000;
        private static final int STATIC_IP_DUP = 8;
        private static final int STATIC_IP_OPTIMIZE = 4;
        private static final int STATIC_IP_UNKNOWN = 0;
        private static final int STATIC_IP_UNUSED = 1;
        private static final int STATIC_IP_USER = 2;
        private static final int THRESHOLD_NORMAL_CHECK_FAIL = 5;
        private static final int TIME_CONFIRM_CHECK = 5000;
        private static final int TIME_DEAD_CHECK = 120000;
        private static final int TIME_FIRST_CHECK = 2000;
        private static final int TIME_HEART_CHECK = 60000;
        private static final int TIME_NORMAL_CHECK = 1000;
        private static final int TIME_POLL_AFTER_CONNECT_DELAYED = 2000;
        private static final int TIME_POLL_AFTER_DISCONNECT_DELAYED = 1500;
        private static final int TIME_POLL_TRAFFIC_STATS_INTERVAL = 5000;
        private static final int TRAFFIC_STATS_POLL = 111;
        private static final int TRAFFIC_STATS_POLL_START = 110;
        private static final int TRAFFIC_STATS_POLL_STOP = 112;
        private boolean mArpRunning = false;
        private ArpState mArpState = ArpState.HEART_CHECK;
        private boolean mArpSuccLeastOnce = false;
        private boolean mLinkLayerLogRunning = false;
        private int mNormalArpFail = 0;
        private int mStaticIpStatus = 0;
        private int mTrafficStatsPollToken = 0;

        public ClientHandler(Looper looper) {
            super(looper);
        }

        private void createAndSendMsgDelayed(int what, int arg1, int arg2, long delayMillis) {
            sendMessageDelayed(Message.obtain(this, what, arg1, arg2), delayMillis);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("msg what=");
            stringBuilder.append(what);
            stringBuilder.append(" arg1(token)=");
            stringBuilder.append(arg1);
            stringBuilder.append(" arg2(mode)=");
            stringBuilder.append(arg2);
            stringBuilder.append(" delay=");
            stringBuilder.append(delayMillis);
            Log.d("HwArpVerifier", stringBuilder.toString());
        }

        public void monitorWifiNetworkState() {
            if (HwArpVerifier.this.isConnectedToWifi() && HwArpVerifier.this.mCurrentWiFiState == 1) {
                Log.d("HwArpVerifier", "dont handle monitorWifiNetworkState started becauseof running");
                return;
            }
            HwArpVerifier.this.mCheckStateToken = HwArpVerifier.this.mCheckStateToken + 1;
            HwArpVerifier.this.mArpBlacklist.clear();
            HwArpVerifier.this.mFirstDetect = true;
            this.mTrafficStatsPollToken++;
            this.mArpSuccLeastOnce = false;
            if (HwArpVerifier.this.isConnectedToWifi()) {
                HwArpVerifier.this.mCurrentWiFiState = 1;
                if (HwArpVerifier.this.isEnableChecker()) {
                    Log.d("HwArpVerifier", "monitorWifiNetworkState: started.");
                    this.mStaticIpStatus = 0;
                    sendMessageDelayed(Message.obtain(this, 110, this.mTrafficStatsPollToken, 0), 2000);
                    HwArpVerifier.this.updateDurationControlParamsIfNeed();
                    transmitState(ArpState.HEART_CHECK);
                    createAndSendMsgDelayed(124, HwArpVerifier.this.mCheckStateToken, 0, 2000);
                }
                HwArpVerifier.this.startWifiRouteCheck();
                HwArpVerifier.this.handleWiFiDnsStats(0);
            } else {
                Log.d("HwArpVerifier", "monitorWifiNetworkState: stopped.");
                HwArpVerifier.this.mCurrentWiFiState = -1;
                this.mStaticIpStatus = 0;
                transmitState(ArpState.DONT_CHECK);
                sendMessageDelayed(Message.obtain(this, 112, this.mTrafficStatsPollToken, 0), 1500);
                HwArpVerifier.this.stopWifiRouteCheck();
                HwArpVerifier.this.handleWiFiDnsStats(HwArpVerifier.this.mLastNetworkId);
            }
        }

        private void setConnectionProperty(HttpURLConnection conn) {
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(SOCKET_TIMEOUT_MS);
            conn.setReadTimeout(SOCKET_TIMEOUT_MS);
            conn.setUseCaches(false);
            conn.setRequestProperty("Charsert", "UTF-8");
            conn.setRequestProperty("Accept-Charset", "utf-8");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("contentType", "utf-8");
        }

        /* JADX WARNING: Missing block: B:42:0x010f, code skipped:
            if (r1 == null) goto L_0x0191;
     */
        /* JADX WARNING: Missing block: B:43:0x0111, code skipped:
            r1.disconnect();
     */
        /* JADX WARNING: Missing block: B:63:0x018e, code skipped:
            if (r1 == null) goto L_0x0191;
     */
        /* JADX WARNING: Missing block: B:64:0x0191, code skipped:
            return r3;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        protected int accessWeb(String dlUrl) {
            StringBuilder stringBuilder;
            StringBuilder stringBuilder2;
            HttpURLConnection urlConn = null;
            InputStream inStream = null;
            int respCode = HwArpVerifier.HTTP_ACCESS_TIMEOUT_RESP;
            try {
                URLConnection conn = new URL(dlUrl).openConnection();
                if (conn instanceof HttpURLConnection) {
                    long lStart = SystemClock.elapsedRealtime();
                    urlConn = (HttpURLConnection) conn;
                    setConnectionProperty(urlConn);
                    inStream = urlConn.getInputStream();
                    respCode = urlConn.getResponseCode();
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("accessWeb, respCode = ");
                    stringBuilder3.append(respCode);
                    stringBuilder3.append(", url=");
                    stringBuilder3.append(dlUrl);
                    Log.d("HwArpVerifier", stringBuilder3.toString());
                    if (dlUrl.equals(HwArpVerifier.WEB_CHINAZ_GETIP) && respCode == 200) {
                        String strBody = readWebBody(inStream);
                        JSONObject obj = null;
                        String strIP = HwArpVerifier.BCM_ROAMING_FLAG_FILE;
                        String strAddr = HwArpVerifier.BCM_ROAMING_FLAG_FILE;
                        try {
                            obj = new JSONObject(strBody);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (obj != null) {
                            try {
                                strIP = obj.getString("ip");
                                strAddr = obj.getString("address");
                            } catch (JSONException e2) {
                                e2.printStackTrace();
                            }
                        }
                        HwArpVerifier.this.mAccessWebStatus.setChinazIp(strIP);
                        HwArpVerifier.this.mAccessWebStatus.setChinazAddr(strAddr);
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("accessWeb, ip = ");
                        stringBuilder4.append(strIP);
                        stringBuilder4.append(", addr = ");
                        stringBuilder4.append(strAddr);
                        Log.d("HwArpVerifier", stringBuilder4.toString());
                    } else if (dlUrl.equals(HwArpVerifier.WEB_BAIDU)) {
                        HwArpVerifier.this.mAccessWebStatus.setRTTBaidu((int) (SystemClock.elapsedRealtime() - lStart));
                    }
                    if (inStream != null) {
                        try {
                            inStream.close();
                        } catch (IOException e3) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("exception of close, msg = ");
                            stringBuilder.append(e3.getMessage());
                            Log.d("HwArpVerifier", stringBuilder.toString());
                        }
                    }
                } else {
                    if (inStream != null) {
                        try {
                            inStream.close();
                        } catch (IOException e4) {
                            StringBuilder stringBuilder5 = new StringBuilder();
                            stringBuilder5.append("exception of close, msg = ");
                            stringBuilder5.append(e4.getMessage());
                            Log.d("HwArpVerifier", stringBuilder5.toString());
                        }
                    }
                    if (urlConn != null) {
                        urlConn.disconnect();
                    }
                    return HwArpVerifier.HTTP_ACCESS_TIMEOUT_RESP;
                }
            } catch (IOException e32) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("IOException, msg = ");
                stringBuilder.append(e32.getMessage());
                Log.d("HwArpVerifier", stringBuilder.toString());
                String msg = e32.getMessage();
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("accessWeb, IOException, msg = ");
                stringBuilder2.append(msg);
                Log.d("HwArpVerifier", stringBuilder2.toString());
                if (dlUrl != null && dlUrl.equals(HwArpVerifier.WEB_BAIDU) && msg != null && (msg.contains("ECONNREFUSED") || msg.contains("ECONNRESET"))) {
                    respCode = 200;
                }
                if (inStream != null) {
                    try {
                        inStream.close();
                    } catch (IOException e322) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("exception of close, msg = ");
                        stringBuilder.append(e322.getMessage());
                        Log.d("HwArpVerifier", stringBuilder.toString());
                    }
                }
            } catch (Throwable th) {
                if (inStream != null) {
                    try {
                        inStream.close();
                    } catch (IOException e5) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("exception of close, msg = ");
                        stringBuilder2.append(e5.getMessage());
                        Log.d("HwArpVerifier", stringBuilder2.toString());
                    }
                }
                if (urlConn != null) {
                    urlConn.disconnect();
                }
            }
        }

        private String readWebBody(InputStream ins) {
            StringBuffer strBody = new StringBuffer();
            int readBytes = 0;
            int totalBytes = 0;
            byte[] buffer = new byte[512];
            if (ins == null) {
                return HwArpVerifier.BCM_ROAMING_FLAG_FILE;
            }
            while (totalBytes < 512) {
                Arrays.fill(buffer, (byte) 0);
                try {
                    readBytes = ins.read(buffer, 0, buffer.length);
                } catch (IOException e) {
                    Log.d("HwArpVerifier", "route_cmd instream, IOException");
                    e.printStackTrace();
                }
                if (readBytes <= 0) {
                    break;
                }
                totalBytes += readBytes;
                if (totalBytes < 512) {
                    strBody.append(new String(buffer, Charset.defaultCharset()).trim());
                }
            }
            return strBody.toString();
        }

        private String getIpRouteTable() {
            ErrnoException e;
            InterruptedIOException e2;
            SocketException e3;
            byte[] bArr;
            Throwable th;
            String msgSnippet = "getIpRouteTable";
            int errno = -OsConstants.EPROTO;
            String route = HwArpVerifier.BCM_ROAMING_FLAG_FILE;
            FileDescriptor fd = null;
            Log.d("HwArpVerifier", "getIpRouteTable");
            byte[] msg = RtNetlinkMessage.newNewGetRouteMessage();
            try {
                fd = NetlinkSocket.forProto(OsConstants.NETLINK_ROUTE);
                NetlinkSocket.connectToKernel(fd);
                try {
                    NetlinkSocket.sendMessage(fd, msg, 0, msg.length, 300);
                    int doneMessageCount = 0;
                    while (doneMessageCount == 0) {
                        ByteBuffer response = NetlinkSocket.recvMessage(fd, 8192, 500);
                        if (response != null) {
                            while (response.remaining() > 0) {
                                NetlinkMessage resmsg = NetlinkMessage.parse(response);
                                if (resmsg != null) {
                                    StructNlMsgHdr hdr = resmsg.getHeader();
                                    if (hdr == null) {
                                        IoUtils.closeQuietly(fd);
                                        return null;
                                    } else if (hdr.nlmsg_type == (short) 3) {
                                        doneMessageCount++;
                                    } else if (hdr.nlmsg_type == (short) 24 || hdr.nlmsg_type == (short) 26) {
                                        StringBuilder stringBuilder = new StringBuilder();
                                        stringBuilder.append(route);
                                        stringBuilder.append(resmsg.toString());
                                        route = stringBuilder.toString();
                                    }
                                }
                            }
                            continue;
                        }
                    }
                    IoUtils.closeQuietly(fd);
                    return route;
                } catch (ErrnoException e4) {
                    e = e4;
                    Log.e("HwArpVerifier", "Error getIpRouteTable", e);
                    IoUtils.closeQuietly(fd);
                    Log.e("HwArpVerifier", "fail getIpRouteTable");
                    return null;
                } catch (InterruptedIOException e5) {
                    e2 = e5;
                    Log.e("HwArpVerifier", "Error getIpRouteTable", e2);
                    IoUtils.closeQuietly(fd);
                    Log.e("HwArpVerifier", "fail getIpRouteTable");
                    return null;
                } catch (SocketException e6) {
                    e3 = e6;
                    Log.e("HwArpVerifier", "Error getIpRouteTable", e3);
                    IoUtils.closeQuietly(fd);
                    Log.e("HwArpVerifier", "fail getIpRouteTable");
                    return null;
                }
            } catch (ErrnoException e7) {
                e = e7;
                bArr = msg;
                Log.e("HwArpVerifier", "Error getIpRouteTable", e);
                IoUtils.closeQuietly(fd);
                Log.e("HwArpVerifier", "fail getIpRouteTable");
                return null;
            } catch (InterruptedIOException e8) {
                e2 = e8;
                bArr = msg;
                Log.e("HwArpVerifier", "Error getIpRouteTable", e2);
                IoUtils.closeQuietly(fd);
                Log.e("HwArpVerifier", "fail getIpRouteTable");
                return null;
            } catch (SocketException e9) {
                e3 = e9;
                bArr = msg;
                Log.e("HwArpVerifier", "Error getIpRouteTable", e3);
                IoUtils.closeQuietly(fd);
                Log.e("HwArpVerifier", "fail getIpRouteTable");
                return null;
            } catch (Throwable th2) {
                th = th2;
                IoUtils.closeQuietly(fd);
                throw th;
            }
        }

        public boolean doArpTestAsync(int arpNum, int minResponse, int timeout) {
            Log.d("HwArpVerifier", "doArpTestAsync");
            if (this.mArpRunning) {
                return false;
            }
            sendMessage(obtainMessage(125, new MsgItem(arpNum, minResponse, timeout)));
            return true;
        }

        public void handleMessage(Message msg) {
            if (msg.what == 124) {
                int token = msg.arg1;
                int mode = msg.arg2;
                if (msg.arg1 != HwArpVerifier.this.mCheckStateToken) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("ignore msg MSG_CHECK_WIFI_STATE, msg token = ");
                    stringBuilder.append(token);
                    stringBuilder.append(", expected token = ");
                    stringBuilder.append(HwArpVerifier.this.mCheckStateToken);
                    Log.w("HwArpVerifier", stringBuilder.toString());
                    return;
                }
                checkWifiNetworkState(token, mode);
            } else if (msg.what == 125) {
                this.mArpRunning = true;
                MsgItem msgItem = msg.obj;
                if (msgItem != null) {
                    boolean result = HwArpVerifier.this.doArp(msgItem.arpNum, msgItem.minResponse, msgItem.timeout);
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("MSG_DO_ARP_ASYNC:");
                    stringBuilder2.append(result);
                    Log.d("HwArpVerifier", stringBuilder2.toString());
                    if (!result) {
                        Intent intent = new Intent(HwArpVerifier.DIAGNOSE_COMPLETE_ACTION);
                        intent.putExtra("MSG_CODE", 14);
                        intent.putExtra("MaxTime", HwArpVerifier.this.mSpendTime);
                        intent.putExtra("PackageName", "HwArpVerifier");
                        HwArpVerifier.this.mContext.sendBroadcast(intent);
                    }
                }
                this.mArpRunning = false;
            } else if (msg.what == 110 || msg.what == 111) {
                if (msg.arg1 == this.mTrafficStatsPollToken) {
                    removeMessages(this.mTrafficStatsPollToken);
                    if (msg.what == 110) {
                        HwWifiCHRNative.setTcpMonitorStat(1);
                        HwArpVerifier.this.mNetstatManager.resetNetstats();
                    }
                    Log.d("HwArpVerifier", "performPollAndLog:");
                    HwArpVerifier.this.mNetstatManager.performPollAndLog();
                    sendMessageDelayed(Message.obtain(this, 111, this.mTrafficStatsPollToken, 0), 5000);
                } else {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("ignore msg ");
                    stringBuilder3.append(msg.what);
                    stringBuilder3.append(", current token = ");
                    stringBuilder3.append(msg.arg1);
                    stringBuilder3.append(", expected token = ");
                    stringBuilder3.append(this.mTrafficStatsPollToken);
                    Log.d("HwArpVerifier", stringBuilder3.toString());
                }
            } else if (msg.what == 112) {
                if (msg.arg1 == this.mTrafficStatsPollToken) {
                    Log.d("HwArpVerifier", "disconnected, trafficStats:");
                    HwArpVerifier.this.mNetstatManager.resetNetstats();
                    HwWifiCHRNative.setTcpMonitorStat(0);
                    removeMessages(110);
                    removeMessages(111);
                    HwArpVerifier.this.mAccessWebStatus.reset();
                }
            } else if (msg.what != 126) {
                HwArpVerifier.this.mHWGatewayVerifier.handleMultiGatewayMessage(msg);
            } else if (HwArpVerifier.this.isConnectedToWifi() && HwArpVerifier.this.mRouteDetectCnt <= 5) {
                try {
                    if (HwArpVerifier.this.isWifiDefaultRouteExist()) {
                        HwArpVerifier.this.mRouteDetectCnt = 0;
                        sendEmptyMessageDelayed(126, 50000);
                    } else {
                        HwArpVerifier.this.mRouteDetectCnt = HwArpVerifier.this.mRouteDetectCnt + 1;
                        if (!HwArpVerifier.this.isMobileDateActive && HwArpVerifier.this.isRouteRepareSwitchEnabled) {
                            Log.e("HwArpVerifier", "try to reparier wifi default route");
                            HwArpVerifier.this.wifiRepairRoute();
                        }
                        sendEmptyMessageDelayed(126, 15000);
                    }
                } catch (Exception e) {
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("exception in wifi route check: ");
                    stringBuilder4.append(e);
                    Log.e("HwArpVerifier", stringBuilder4.toString());
                }
            }
        }

        private boolean isNeedCheck() {
            return (this.mArpState == ArpState.DONT_CHECK || this.mArpState == ArpState.DEAD_CHECK) ? false : true;
        }

        private void transmitState(ArpState state) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("from ");
            stringBuilder.append(strState(this.mArpState));
            stringBuilder.append(" transmit to state:");
            stringBuilder.append(strState(state));
            Log.d("HwArpVerifier", stringBuilder.toString());
            if (this.mArpState == ArpState.CONFIRM_CHECK && state == ArpState.HEART_CHECK && HwArpVerifier.this.mHwWifiCHRService != null) {
                HwArpVerifier.this.mHwWifiCHRService.updateWifiException(87, "ARP_REASSOC_OK");
            }
            this.mArpState = state;
            if (this.mArpState == ArpState.NORMAL_CHECK) {
                this.mNormalArpFail = 0;
            } else if ((this.mArpState == ArpState.DONT_CHECK || this.mArpState == ArpState.HEART_CHECK) && this.mLinkLayerLogRunning) {
                HwArpVerifier.this.mHwLogUtils.stopLinkLayerLog();
                this.mLinkLayerLogRunning = false;
            }
        }

        private String strState(ArpState state) {
            if (state == ArpState.DONT_CHECK) {
                return "DONT_CHECK";
            }
            if (state == ArpState.HEART_CHECK) {
                return "HEART_CHECK";
            }
            if (state == ArpState.NORMAL_CHECK) {
                return "NORMAL_CHECK";
            }
            if (state == ArpState.CONFIRM_CHECK) {
                return "CONFIRM_CHECK";
            }
            if (state == ArpState.DEAD_CHECK) {
                return "DEAD_CHECK";
            }
            return HwArpVerifier.BCM_ROAMING_FLAG_FILE;
        }

        private int genDynamicCheckPings() {
            int index;
            if (this.mNormalArpFail < 0 || this.mNormalArpFail >= HwArpVerifier.dynamicPings.length) {
                index = HwArpVerifier.dynamicPings.length - 1;
            } else {
                index = this.mNormalArpFail;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mNormalArpFail: ");
            stringBuilder.append(this.mNormalArpFail);
            stringBuilder.append(", dynamicPings[");
            stringBuilder.append(index);
            stringBuilder.append("]:");
            stringBuilder.append(HwArpVerifier.dynamicPings[index]);
            Log.d("HwArpVerifier", stringBuilder.toString());
            return HwArpVerifier.dynamicPings[index];
        }

        private void doCheckWifiNetworkState(int token) {
            if (this.mArpState == ArpState.DONT_CHECK) {
                HwArpVerifier.this.mCheckStateToken = HwArpVerifier.this.mCheckStateToken + 1;
            } else if (this.mArpState == ArpState.HEART_CHECK) {
                createAndSendMsgDelayed(124, token, 0, HidataWechatTraffic.MIN_VALID_TIME);
            } else if (this.mArpState == ArpState.NORMAL_CHECK) {
                createAndSendMsgDelayed(124, token, 2, 1000);
            } else if (this.mArpState == ArpState.CONFIRM_CHECK) {
                createAndSendMsgDelayed(124, token, 1, 5000);
            } else if (this.mArpState == ArpState.DEAD_CHECK) {
                createAndSendMsgDelayed(124, token, 0, 120000);
            }
        }

        private void handleCheckResult(int token, boolean result) {
            if (this.mArpState == ArpState.HEART_CHECK || this.mArpState == ArpState.NORMAL_CHECK || this.mArpState == ArpState.CONFIRM_CHECK) {
                if (isUsingStaticIp()) {
                    Log.d("HwArpVerifier", "dhcp result not ok, maybe static IP, go on Arp heart check");
                    result = true;
                }
                if (result) {
                    transmitState(ArpState.HEART_CHECK);
                } else if (this.mArpState == ArpState.HEART_CHECK) {
                    transmitState(ArpState.NORMAL_CHECK);
                } else if (this.mArpState == ArpState.CONFIRM_CHECK) {
                    if (!this.mArpSuccLeastOnce) {
                        Log.d("HwArpVerifier", "all Arp test failed in this network, disable Arp check");
                        transmitState(ArpState.DONT_CHECK);
                    } else if (!HwArpVerifier.this.isNeedTriggerReconnectWifi() || HwArpVerifier.this.isIgnoreArpCheck()) {
                        transmitState(ArpState.HEART_CHECK);
                    } else {
                        HwArpVerifier.this.reconnectWifiNetwork();
                    }
                } else if (this.mArpState == ArpState.NORMAL_CHECK) {
                    this.mNormalArpFail++;
                    if (this.mNormalArpFail >= 2 && !this.mLinkLayerLogRunning) {
                        HwArpVerifier.this.mHwLogUtils.startLinkLayerLog();
                        this.mLinkLayerLogRunning = true;
                    }
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Notify wifi network is down for NO.");
                    stringBuilder.append(this.mNormalArpFail);
                    Log.d("HwArpVerifier", stringBuilder.toString());
                    if (this.mNormalArpFail >= 5 && !HwArpVerifier.this.isIgnoreArpCheck()) {
                        if (!this.mArpSuccLeastOnce) {
                            Log.d("HwArpVerifier", "all Arp test failed in this network, disable Arp check");
                            transmitState(ArpState.DONT_CHECK);
                        } else if (HwArpVerifier.this.isNeedTriggerReconnectWifi()) {
                            if (!WifiProCommonUtils.isWifiSelfCuring()) {
                                HwArpVerifier.this.recoveryWifiNetwork();
                            }
                            HwArpVerifier.this.notifyNetworkUnreachable();
                            transmitState(ArpState.CONFIRM_CHECK);
                        } else if (this.mNormalArpFail > 15) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Arp failed reach to ");
                            stringBuilder.append(this.mNormalArpFail);
                            stringBuilder.append(" in this network, disable Arp check");
                            Log.d("HwArpVerifier", stringBuilder.toString());
                            transmitState(ArpState.DONT_CHECK);
                        }
                    }
                }
            }
            doCheckWifiNetworkState(token);
        }

        private void checkWifiNetworkState(int token, int mode) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("check_wifi_state_mode = ");
            stringBuilder.append(mode);
            stringBuilder.append(" mCheckStateToken=");
            stringBuilder.append(HwArpVerifier.this.mCheckStateToken);
            stringBuilder.append(" token");
            stringBuilder.append(token);
            Log.d("HwArpVerifier", stringBuilder.toString());
            if (!HwArpVerifier.this.isConnectedToWifi()) {
                Log.d("HwArpVerifier", "Notify network is not connected, need not to do ARP test.");
                HwArpVerifier.this.mCurrentWiFiState = -1;
                transmitState(ArpState.DONT_CHECK);
            }
            if (isNeedCheck()) {
                boolean ret;
                if (this.mArpState == ArpState.HEART_CHECK && HwArpVerifier.this.mHWGatewayVerifier.isEnableDectction()) {
                    HwArpVerifier.this.readArpFromFile();
                    HwArpVerifier.this.mHWGatewayVerifier.getGateWayARPResponses();
                    int gatewayNumber = HwArpVerifier.this.mHWGatewayVerifier.mGW.getGWNum();
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("There are ");
                    stringBuilder2.append(gatewayNumber);
                    stringBuilder2.append(" mac address for gateway ");
                    Log.d("HwArpVerifier", stringBuilder2.toString());
                    if (HwArpVerifier.this.needToDetectGateway()) {
                        Message.obtain(HwArpVerifier.this.mClientHandler, 1004, token, 1).sendToTarget();
                        return;
                    }
                    ret = gatewayNumber > 0;
                } else {
                    ret = HwArpVerifier.this.doArpTest(mode);
                }
                if (!ret) {
                    HwArpVerifier.this.doGratuitousArp(1000);
                    ret = HwArpVerifier.this.pingGateway(2000);
                }
                if (!this.mArpSuccLeastOnce && ret) {
                    this.mArpSuccLeastOnce = true;
                }
                handleCheckResult(token, ret);
            }
        }

        private boolean isUsingStaticIp() {
            if (this.mStaticIpStatus == 0) {
                getStaticIpStatus();
            }
            return (this.mStaticIpStatus & 2) > 0;
        }

        private void getStaticIpStatus() {
            if (isUsingStaticIpFromConfig()) {
                this.mStaticIpStatus = 2;
            } else {
                String dhcpIface = new StringBuilder();
                dhcpIface.append("dhcp.");
                dhcpIface.append(SystemProperties.get("wifi.interface", HwArpVerifier.IFACE));
                dhcpIface.append(".result");
                this.mStaticIpStatus = "ok".equals(SystemProperties.get(dhcpIface.toString(), HwArpVerifier.BCM_ROAMING_FLAG_FILE)) ? 1 : 4;
            }
            if (this.mStaticIpStatus == 4) {
                HwArpVerifier.this.mHwWifiCHRService.setIpType(2);
            }
            if (this.mStaticIpStatus == 2) {
                HwArpVerifier.this.mHwWifiCHRService.setIpType(1);
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getStaticIpStatus:");
            stringBuilder.append(this.mStaticIpStatus);
            Log.d("HwArpVerifier", stringBuilder.toString());
        }

        private boolean isUsingStaticIpFromConfig() {
            if (HwArpVerifier.this.mWM == null) {
                HwArpVerifier.this.mWM = (WifiManager) HwArpVerifier.this.mContext.getSystemService("wifi");
            }
            List<WifiConfiguration> configuredNetworks = HwArpVerifier.this.mWM.getConfiguredNetworks();
            if (configuredNetworks != null) {
                int netid = -1;
                WifiInfo info = HwArpVerifier.this.mWM.getConnectionInfo();
                if (info != null) {
                    netid = info.getNetworkId();
                }
                if (netid == -1) {
                    return false;
                }
                for (WifiConfiguration config : configuredNetworks) {
                    if (config != null && config.networkId == netid && config.getIpAssignment() == IpAssignment.STATIC) {
                        return true;
                    }
                }
            }
            return false;
        }

        private void doStaticIpHandler(int result) {
            if (this.mStaticIpStatus == 0) {
                getStaticIpStatus();
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("doStaticIpHandler mStaticIpStatus");
            stringBuilder.append(this.mStaticIpStatus);
            Log.d("HwArpVerifier", stringBuilder.toString());
            if (this.mStaticIpStatus == 4) {
                this.mStaticIpStatus |= 8;
            }
        }
    }

    private class HWGatewayVerifier {
        private static final String COUNTRY_CODE_CN = "CN";
        private static final int DEFAULT_ARP_NUMBER = 1;
        private static final int DEFAULT_ARP_TIMEOUT_MS = 1000;
        private static final int DEFAULT_GATEWAY_NUMBER = 1;
        private static final int DETECTION_TIMEOUT = 10000;
        private static final int MSG_NET_ACCESS_DETECT = 1001;
        private static final int MSG_NET_ACCESS_DETECT_END = 1003;
        private static final int MSG_NET_ACCESS_DETECT_FAILED = 1002;
        private static final int MSG_NET_ACCESS_DETECT_REAL = 1004;
        private static final int NET_ACCESS_DETECT_DELAY = 2000;
        private static final int TIME_CLEAR_DNS = 1000;
        private String mCurrentMac;
        private int mEnableAccessDetect;
        private HWMultiGW mGW;

        private HWGatewayVerifier() {
            this.mEnableAccessDetect = -1;
            this.mGW = new HWMultiGW();
            this.mCurrentMac = HwArpVerifier.BCM_ROAMING_FLAG_FILE;
        }

        /* synthetic */ HWGatewayVerifier(HwArpVerifier x0, AnonymousClass1 x1) {
            this();
        }

        private void handleMultiGatewayMessage(Message msg) {
            int token = msg.arg1;
            if (token != HwArpVerifier.this.mCheckStateToken) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ignore msg ");
                stringBuilder.append(msg.what);
                stringBuilder.append(", current token = ");
                stringBuilder.append(token);
                stringBuilder.append(", expected token = ");
                stringBuilder.append(HwArpVerifier.this.mCheckStateToken);
                Log.d("HwArpVerifier", stringBuilder.toString());
                return;
            }
            String mac;
            switch (msg.what) {
                case MSG_NET_ACCESS_DETECT /*1001*/:
                    mac = this.mGW.getNextGWMACAddr();
                    String gateway = this.mGW.getGWIPAddr();
                    if (mac != null && gateway != null) {
                        this.mCurrentMac = mac;
                        HwArpVerifier.this.mWifiNative.setStaticARP(gateway, mac);
                        HwArpVerifier.this.flushNetworkDnsCache();
                        HwArpVerifier.this.flushVmDnsCache();
                        HwArpVerifier.this.mClientHandler.sendMessageDelayed(Message.obtain(HwArpVerifier.this.mClientHandler, MSG_NET_ACCESS_DETECT_REAL, token, 0), 1000);
                        break;
                    }
                    Message.obtain(HwArpVerifier.this.mClientHandler, MSG_NET_ACCESS_DETECT_END, token, 0).sendToTarget();
                    break;
                    break;
                case MSG_NET_ACCESS_DETECT_FAILED /*1002*/:
                    if (!TextUtils.isEmpty(this.mCurrentMac)) {
                        HwArpVerifier.this.mCheckStateToken = HwArpVerifier.this.mCheckStateToken + 1;
                        mac = this.mGW.getGWIPAddr();
                        if (!TextUtils.isEmpty(mac)) {
                            addToBlacklist();
                            HwArpVerifier.this.mWifiNative.delStaticARP(mac);
                            Message.obtain(HwArpVerifier.this.mClientHandler, MSG_NET_ACCESS_DETECT, HwArpVerifier.this.mCheckStateToken, 0).sendToTarget();
                            break;
                        }
                    }
                    Message.obtain(HwArpVerifier.this.mClientHandler, MSG_NET_ACCESS_DETECT, HwArpVerifier.this.mCheckStateToken, 0).sendToTarget();
                    break;
                    break;
                case MSG_NET_ACCESS_DETECT_END /*1003*/:
                    HwArpVerifier.this.mClientHandler.doStaticIpHandler(msg.arg2);
                    HwArpVerifier.this.mClientHandler.handleCheckResult(HwArpVerifier.this.mCheckStateToken, true);
                    break;
                case MSG_NET_ACCESS_DETECT_REAL /*1004*/:
                    if (msg.arg2 == 1) {
                        this.mCurrentMac = HwArpVerifier.BCM_ROAMING_FLAG_FILE;
                    }
                    startNetAccessDetection(token);
                    break;
            }
        }

        private void getGateWayARPResponses() {
            getGateWayARPResponses(1, 1000);
        }

        /* JADX WARNING: Missing block: B:19:0x009c, code skipped:
            if (r0 != null) goto L_0x009e;
     */
        /* JADX WARNING: Missing block: B:20:0x009e, code skipped:
            r0.close();
     */
        /* JADX WARNING: Missing block: B:26:0x00aa, code skipped:
            if (r0 == null) goto L_0x00ad;
     */
        /* JADX WARNING: Missing block: B:27:0x00ad, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void getGateWayARPResponses(int arpNum, int timeout) {
            this.mGW.clearGW();
            HWArpPeer peer = null;
            try {
                peer = HwArpVerifier.this.constructArpPeer();
                if (peer == null) {
                    if (peer != null) {
                        peer.close();
                    }
                    return;
                }
                this.mGW.setGWIPAddr(HwArpVerifier.this.mGateway);
                for (int i = 0; i < arpNum; i++) {
                    boolean isSucc = false;
                    HWMultiGW multiGW = peer.getGateWayARPResponses(timeout);
                    if (multiGW != null) {
                        HwArpVerifier.this.mAccessWebStatus.setRTTArp((int) multiGW.getArpRTT());
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("getGateWayARPResponses: arp rtt = ");
                        stringBuilder.append(multiGW.getArpRTT());
                        Log.d("HwArpVerifier", stringBuilder.toString());
                        Iterator it = multiGW.getGWMACAddrList().iterator();
                        while (it.hasNext()) {
                            this.mGW.setGWMACAddr((String) it.next());
                            isSucc = true;
                        }
                        HwArpVerifier.this.mHwWifiCHRService.updateMultiGWCount((byte) this.mGW.getGWNum());
                        HwArpVerifier.this.mHwWifiCHRService.updateArpSummery(isSucc, (int) multiGW.getArpRTT(), HwArpVerifier.mRSSI);
                        HwArpVerifier.this.updateArpResult(isSucc, (int) multiGW.getArpRTT());
                    }
                }
            } catch (Exception e) {
            } catch (Throwable th) {
                if (peer != null) {
                    peer.close();
                }
            }
        }

        private void startNetAccessDetection(int token) {
            new WebDetectThread(token).start();
            Log.d("HwArpVerifier", "access internet timeout:10000");
            HwArpVerifier.this.mClientHandler.sendMessageDelayed(Message.obtain(HwArpVerifier.this.mClientHandler, MSG_NET_ACCESS_DETECT_FAILED, token, 0), 10000);
        }

        private boolean isEnableDectction() {
            if (this.mEnableAccessDetect < 0) {
                String countryCode = SystemProperties.get("ro.product.locale.region", HwArpVerifier.BCM_ROAMING_FLAG_FILE);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("local region:");
                stringBuilder.append(countryCode);
                Log.d("HwArpVerifier", stringBuilder.toString());
                if (COUNTRY_CODE_CN.equalsIgnoreCase(countryCode)) {
                    this.mEnableAccessDetect = 1;
                    HwCHRWebDetectThread.setEnableCheck(true);
                } else {
                    this.mEnableAccessDetect = 0;
                }
            }
            if (this.mEnableAccessDetect == 1) {
                return true;
            }
            return false;
        }

        private void addToBlacklist() {
            if (!TextUtils.isEmpty(this.mCurrentMac)) {
                Iterator it = HwArpVerifier.this.mArpBlacklist.iterator();
                while (it.hasNext()) {
                    ArpItem item = (ArpItem) it.next();
                    if (item.sameMacAddress(this.mCurrentMac)) {
                        item.putFail();
                        return;
                    }
                }
                HwArpVerifier.this.mArpBlacklist.add(new ArpItem(this.mCurrentMac, 1));
            }
        }
    }

    private static class MsgItem {
        public int arpNum;
        public int minResponse;
        public int timeout;

        public MsgItem(int arpNum, int minResponse, int timeout) {
            this.arpNum = arpNum;
            this.minResponse = minResponse;
            this.timeout = timeout;
        }
    }

    private class WebDetectThread extends Thread {
        private HwCHRWebDetectThread detect = new HwCHRWebDetectThread(0);
        private int mInnerToken;

        public WebDetectThread(int token) {
            this.mInnerToken = token;
        }

        public void run() {
            boolean ret = this.detect.isInternetConnected();
            if (this.mInnerToken == HwArpVerifier.this.mCheckStateToken) {
                HwArpVerifier.this.mClientHandler.removeMessages(1002);
                if (ret) {
                    if (!HwNcDftConnManager.isCommercialUser()) {
                        HwArpVerifier.this.mClientHandler.accessWeb(HwArpVerifier.WEB_CHINAZ_GETIP);
                    }
                    Message.obtain(HwArpVerifier.this.mClientHandler, 1003, this.mInnerToken, 1).sendToTarget();
                    return;
                }
                Log.d("HwArpVerifier", "browse web failed, will del static ARP item");
                Message.obtain(HwArpVerifier.this.mClientHandler, 1002, this.mInnerToken, 0).sendToTarget();
                HwArpVerifier.this.errCode = this.detect.getErrorCode();
            }
        }
    }

    private static native void class_init_native();

    private native int nativeReadArpDetail();

    private native void native_init();

    static {
        boolean z = true;
        if (SystemProperties.getInt("ro.logsystem.usertype", 1) != 3) {
            z = false;
        }
        BETA_VER = z;
        System.loadLibrary("huaweiwifi-service");
        class_init_native();
        Log.d("HwArpVerifier", "load huaweiwifi-service ");
    }

    public HwArpVerifier(Context context) {
        this.mContext = context;
        this.mWifiNative = WifiInjector.getInstance().getWifiNative();
        this.mNetstatManager = new HWNetstatManager(this.mContext);
        startArpChecker();
        registerForBroadcasts();
        initRoamingFlagFile();
        this.mNwService = Stub.asInterface(ServiceManager.getService("network_management"));
        this.mHwWifiCHRService = HwWifiCHRServiceImpl.getInstance();
        HwWiFiLogUtils.init(this.mWifiNative);
        this.mHwLogUtils = HwWiFiLogUtils.getDefault();
        native_init();
    }

    public static HwArpVerifier getDefault() {
        mLock.lock();
        HwArpVerifier instance = arp_instance;
        mLock.unlock();
        return instance;
    }

    public static synchronized HwArpVerifier newInstance(Context context) {
        HwArpVerifier instance;
        synchronized (HwArpVerifier.class) {
            ReentrantLock reentrantLock;
            mLock.lock();
            instance = arp_instance;
            try {
                if (arp_instance == null) {
                    arp_instance = new HwArpVerifier(context);
                    instance = arp_instance;
                }
                reentrantLock = mLock;
            } catch (RuntimeException e) {
                reentrantLock = mLock;
            } catch (Throwable th) {
                mLock.unlock();
            }
            reentrantLock.unlock();
        }
        return instance;
    }

    public static void setRssi(int rssi) {
        mRSSI = rssi;
    }

    public AccessWebStatus getAccessWebStatus() {
        return this.mAccessWebStatus;
    }

    public void startArpChecker() {
        Log.d("HwArpVerifier", "startArpChecker 1");
        if (isEnableChecker()) {
            startLooper();
        }
    }

    public void stopArpChecker() {
        Log.d("HwArpVerifier", "stopArpChecker");
        stopLooper();
    }

    public boolean doArpTest(int arpNum, int minResponse, int timeout, boolean async) {
        if (arpNum <= 0) {
            arpNum = 1;
        }
        if (minResponse > arpNum || minResponse <= 0) {
            minResponse = 1;
        }
        if (timeout <= 0) {
            timeout = 100;
        }
        if (!async) {
            return doArp(arpNum, minResponse, timeout);
        }
        if (!isLooperRunning()) {
            startLooper();
        }
        if (this.mClientHandler != null) {
            return this.mClientHandler.doArpTestAsync(arpNum, minResponse, timeout);
        }
        return true;
    }

    private void startLooper() {
        if (this.mThread == null) {
            this.mThread = new HandlerThread("WiFiArpStateMachine");
            this.mThread.start();
            this.mClientHandler = new ClientHandler(this.mThread.getLooper());
            Log.d("HwArpVerifier", "startLooper");
        }
    }

    private void stopLooper() {
        if (this.mThread != null) {
            this.mThread.quit();
            this.mClientHandler = null;
            this.mThread = null;
            Log.d("HwArpVerifier", "stopLooper");
        }
    }

    private boolean isLooperRunning() {
        return this.mThread != null;
    }

    private boolean isEnableChecker() {
        return true;
    }

    private void registerForBroadcasts() {
        if (!this.mRegisterReceiver) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.SCREEN_ON");
            intentFilter.addAction("android.intent.action.SCREEN_OFF");
            intentFilter.addAction("android.net.wifi.STATE_CHANGE");
            intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
            intentFilter.addCategory("android.net.wifi.WIFI_STATE_CHANGED@hwBrExpand@WifiStatus=WIFIENABLED");
            intentFilter.addAction("android.intent.action.ANY_DATA_STATE");
            this.mContext.registerReceiver(this.mReceiver, intentFilter);
        }
    }

    public void unregisterForBroadcasts() {
        if (this.mRegisterReceiver) {
            this.mContext.unregisterReceiver(this.mReceiver);
        }
    }

    private void initRoamingFlagFile() {
        String chipType = SystemProperties.get("ro.connectivity.chiptype", BCM_ROAMING_FLAG_FILE);
        if (chipType != null && chipType.equalsIgnoreCase("hi110x")) {
            this.mRoamingFlagFile = HI110X_ROAMING_FLAG_FILE;
        } else if (chipType == null || !chipType.equalsIgnoreCase("hisi")) {
            this.mRoamingFlagFile = BCM_ROAMING_FLAG_FILE;
        } else {
            this.mRoamingFlagFile = HI1102_ROAMING_FLAG_FILE;
        }
    }

    private boolean readRoamingFlag() {
        boolean ret = false;
        String roam_flag = "roam_status=";
        BufferedReader in = null;
        try {
            if (this.mRoamingFlagFile != null) {
                if (!this.mRoamingFlagFile.isEmpty()) {
                    File file = new File(this.mRoamingFlagFile);
                    if (file.exists()) {
                        in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                        String s = in.readLine();
                        if (s != null) {
                            int pos = s.indexOf("roam_status=");
                            if (pos >= 0 && "roam_status=".length() + pos < s.length()) {
                                String flag = s.substring("roam_status=".length() + pos);
                                if (flag != null) {
                                    ret = "1".equals(flag);
                                }
                            }
                        }
                        try {
                            in.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return ret;
                    }
                    if (in != null) {
                        try {
                            in.close();
                        } catch (Exception e2) {
                            e2.printStackTrace();
                        }
                    }
                    return false;
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e3) {
                    e3.printStackTrace();
                }
            }
            return false;
        } catch (Exception e32) {
            e32.printStackTrace();
            if (in != null) {
                in.close();
            }
        } catch (Throwable th) {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e4) {
                    e4.printStackTrace();
                }
            }
        }
    }

    private boolean doArpTest(int mode) {
        if (mode == 0) {
            return doArp(1, 1, 1000);
        }
        if (mode == 1) {
            return doArp(2, 1, 5000);
        }
        if (mode != 2 || this.mClientHandler == null) {
            return true;
        }
        return doArp(this.mClientHandler.genDynamicCheckPings(), 1, 1000);
    }

    /* JADX WARNING: Missing block: B:26:0x008d, code skipped:
            if (r2 != null) goto L_0x008f;
     */
    /* JADX WARNING: Missing block: B:31:0x0097, code skipped:
            if (r2 == null) goto L_0x00d0;
     */
    /* JADX WARNING: Missing block: B:40:0x00cd, code skipped:
            if (r2 == null) goto L_0x00d0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean doArp(int arpNum, int minResponse, int timeout) {
        boolean retArp;
        boolean z = false;
        this.mSpendTime = 0;
        HWArpPeer peer = null;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("doArp() arpnum:");
        stringBuilder.append(arpNum);
        stringBuilder.append(", minResponse:");
        stringBuilder.append(minResponse);
        stringBuilder.append(", timeout:");
        stringBuilder.append(timeout);
        Log.d("HwArpVerifier", stringBuilder.toString());
        try {
            peer = constructArpPeer();
            if (peer == null) {
                if (peer != null) {
                    peer.close();
                }
                return true;
            }
            int responses = 0;
            for (int i = 0; i < arpNum; i++) {
                long startTimestamp = System.currentTimeMillis();
                if (isIgnoreArpCheck()) {
                    Log.d("HwArpVerifier", "isIgnoreArpCheck is ture, ignore ARP check");
                    responses++;
                } else if (peer.doArp(timeout) != null) {
                    responses++;
                }
                int spendTime = (int) (System.currentTimeMillis() - startTimestamp);
                if (spendTime > this.mSpendTime) {
                    this.mSpendTime = spendTime;
                }
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("ARP test result: ");
            stringBuilder2.append(responses);
            stringBuilder2.append("/");
            stringBuilder2.append(arpNum);
            Log.d("HwArpVerifier", stringBuilder2.toString());
            if (responses >= minResponse) {
                z = true;
            }
            retArp = z;
        } catch (SocketException se) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("exception in ARP test: ");
            stringBuilder.append(se);
            Log.e("HwArpVerifier", stringBuilder.toString());
            retArp = true;
        } catch (IllegalArgumentException ae) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("exception in ARP test:");
            stringBuilder.append(ae);
            Log.e("HwArpVerifier", stringBuilder.toString());
            retArp = true;
            if (peer != null) {
                peer.close();
            }
            this.mHwWifiCHRService.updateArpSummery(retArp, this.mSpendTime, mRSSI);
            updateArpResult(retArp, this.mSpendTime);
            return retArp;
        } catch (Exception e) {
            retArp = false;
        } catch (Throwable th) {
            if (peer != null) {
                peer.close();
            }
        }
    }

    /* JADX WARNING: Missing block: B:13:0x0022, code skipped:
            if (r1 != null) goto L_0x0024;
     */
    /* JADX WARNING: Missing block: B:14:0x0024, code skipped:
            r1.close();
     */
    /* JADX WARNING: Missing block: B:19:0x0041, code skipped:
            if (r1 == null) goto L_0x0044;
     */
    /* JADX WARNING: Missing block: B:20:0x0044, code skipped:
            if (r0 == null) goto L_0x009e;
     */
    /* JADX WARNING: Missing block: B:22:0x0048, code skipped:
            if (r0.length != 6) goto L_0x009e;
     */
    /* JADX WARNING: Missing block: B:23:0x004a, code skipped:
            r4 = new java.lang.StringBuilder();
            r4.append(java.lang.String.format("%02x:%02x:%02x:%02x:%02x:%02x", new java.lang.Object[]{java.lang.Byte.valueOf(r0[0]), java.lang.Byte.valueOf(r0[1]), java.lang.Byte.valueOf(r0[2]), java.lang.Byte.valueOf(r0[3]), java.lang.Byte.valueOf(r0[4]), java.lang.Byte.valueOf(r0[5])}));
            r4.append("alse use My IP(IP conflict detected)");
            android.util.Log.w("HwArpVerifier", r4.toString());
     */
    /* JADX WARNING: Missing block: B:24:0x009e, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void doGratuitousArp(int timeout) {
        byte[] rspMac = null;
        HWArpPeer peer = null;
        try {
            peer = constructArpPeer();
            if (peer == null) {
                if (peer != null) {
                    peer.close();
                }
            } else if (isIgnoreArpCheck()) {
                Log.d("HwArpVerifier", "isIgnoreArpCheck is ture, ignore doGratuitousArp");
            } else {
                rspMac = peer.doGratuitousArp(timeout);
            }
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("exception in GARP test:");
            stringBuilder.append(e);
            Log.e("HwArpVerifier", stringBuilder.toString());
        } catch (Throwable th) {
            if (peer != null) {
                peer.close();
            }
        }
    }

    private LinkProperties getCurrentLinkProperties() {
        if (this.mCM == null) {
            this.mCM = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        }
        return this.mCM.getLinkProperties(1);
    }

    private boolean isConnectedToWifi() {
        boolean z = false;
        if (this.mNetworkInfo == null) {
            return false;
        }
        DetailedState state = this.mNetworkInfo.getDetailedState();
        if (state == DetailedState.CONNECTED || state == DetailedState.VERIFYING_POOR_LINK) {
            z = true;
        }
        return z;
    }

    private HWArpPeer constructArpPeer() throws SocketException {
        if (this.mWM == null) {
            this.mWM = (WifiManager) this.mContext.getSystemService("wifi");
        }
        WifiInfo wifiInfo = this.mWM.getConnectionInfo();
        LinkProperties linkProperties = getCurrentLinkProperties();
        String linkIFName = linkProperties != null ? linkProperties.getInterfaceName() : IFACE;
        String macAddr = null;
        InetAddress linkAddr = null;
        if (wifiInfo != null) {
            macAddr = wifiInfo.getMacAddress();
            linkAddr = NetworkUtils.intToInetAddress(wifiInfo.getIpAddress());
        }
        InetAddress gateway = null;
        DhcpInfo dhcpInfo = this.mWM.getDhcpInfo();
        if (!(dhcpInfo == null || dhcpInfo.gateway == 0)) {
            gateway = NetworkUtils.intToInetAddress(dhcpInfo.gateway);
        }
        if (gateway == null) {
            return null;
        }
        this.mGateway = gateway.getHostAddress();
        return new HWArpPeer(linkIFName, linkAddr, macAddr, gateway);
    }

    private void handleWifiSwitchChanged(int state) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleWifiSwitchChanged state:");
        stringBuilder.append(state);
        Log.d("HwArpVerifier", stringBuilder.toString());
        if (state == 3) {
            resetDurationControlParams();
        }
    }

    private void resetDurationControlParams() {
        this.mShortDurationStartTime = 0;
        this.mLongDurationStartTime = 0;
        this.mShortTriggerCnt = 0;
        this.mLongTriggerCnt = 0;
        this.mLastSSID = null;
    }

    private void updateDurationControlParamsIfNeed() {
        String ssid = getCurrentSsid();
        if (ssid == null || "<unknown ssid>".equals(ssid)) {
            Log.e("HwArpVerifier", "current SSID is empty.");
            return;
        }
        if (this.mLastSSID == null) {
            this.mLastSSID = ssid;
        } else if (!ssid.equals(this.mLastSSID)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("connected SSID ");
            stringBuilder.append(this.mLastSSID);
            stringBuilder.append(" changed to ");
            stringBuilder.append(ssid);
            Log.d("HwArpVerifier", stringBuilder.toString());
            resetDurationControlParams();
            this.mLastSSID = ssid;
        }
    }

    private WifiInfo getCurrentWifiInfo() {
        if (this.mWM == null) {
            this.mWM = (WifiManager) this.mContext.getSystemService("wifi");
        }
        return this.mWM.getConnectionInfo();
    }

    private String getCurrentSsid() {
        WifiInfo curWifi = getCurrentWifiInfo();
        if (curWifi != null) {
            return curWifi.getSSID();
        }
        Log.e("HwArpVerifier", "fail to get current wifi info in getCurrentSsid");
        return null;
    }

    private boolean isPassDurationControl() {
        boolean ret;
        long now = System.currentTimeMillis();
        if (this.mShortDurationStartTime == 0) {
            this.mShortDurationStartTime = now;
        }
        if (this.mLongDurationStartTime == 0) {
            this.mLongDurationStartTime = now;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("short duration: [now:");
        stringBuilder.append(now);
        stringBuilder.append(", lastTime:");
        stringBuilder.append(this.mShortDurationStartTime);
        stringBuilder.append(", duration:");
        stringBuilder.append(SHORT_ARP_FAIL_DURATION);
        stringBuilder.append(", failTimes:");
        stringBuilder.append(this.mShortTriggerCnt);
        stringBuilder.append(", failThreshold:");
        stringBuilder.append(2);
        Log.d("HwArpVerifier", stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("long duration: [now:");
        stringBuilder.append(now);
        stringBuilder.append(", lastTime:");
        stringBuilder.append(this.mLongDurationStartTime);
        stringBuilder.append(", duration:");
        stringBuilder.append(LONG_ARP_FAIL_DURATION);
        stringBuilder.append(", failTimes:");
        stringBuilder.append(this.mLongTriggerCnt);
        stringBuilder.append(", failThreshold:");
        stringBuilder.append(6);
        Log.d("HwArpVerifier", stringBuilder.toString());
        if (now - this.mShortDurationStartTime > SHORT_ARP_FAIL_DURATION) {
            this.mShortTriggerCnt = 0;
            this.mShortDurationStartTime = now;
            ret = true;
        } else if (this.mShortTriggerCnt >= 2) {
            ret = false;
        } else {
            ret = true;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("short duration control ret is:");
        stringBuilder2.append(ret);
        Log.d("HwArpVerifier", stringBuilder2.toString());
        if (!ret) {
            return false;
        }
        this.mShortTriggerCnt++;
        if (now - this.mLongDurationStartTime > LONG_ARP_FAIL_DURATION) {
            this.mLongTriggerCnt = 0;
            this.mLongDurationStartTime = now;
            ret = true;
        } else if (this.mLongTriggerCnt >= 6) {
            ret = false;
        } else {
            ret = true;
        }
        if (ret) {
            this.mLongTriggerCnt++;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("long duration control ret is:");
        stringBuilder.append(ret);
        Log.d("HwArpVerifier", stringBuilder.toString());
        return ret;
    }

    private boolean isNeedTriggerReconnectWifi() {
        if (isPassDurationControl()) {
            return true;
        }
        Log.e("HwArpVerifier", "don't pass duration control, skip Wifi reconnect.");
        return false;
    }

    private boolean isIgnoreArpCheck() {
        if (isWeakSignal()) {
            return true;
        }
        if (!readRoamingFlag()) {
            return false;
        }
        Log.d("HwArpVerifier", "It's WiFi roaming now, ignore arp check");
        return true;
    }

    private boolean isWeakSignal() {
        WifiInfo curWifi = getCurrentWifiInfo();
        if (curWifi == null) {
            Log.e("HwArpVerifier", "fail to get current wifi info in isWeakSignal.");
            return false;
        }
        int rssi = curWifi.getRssi();
        if (rssi > WEAK_SIGNAL_THRESHOLD) {
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("current WIFI rssi:");
        stringBuilder.append(rssi);
        stringBuilder.append(" is weak");
        Log.e("HwArpVerifier", stringBuilder.toString());
        return true;
    }

    private void recoveryWifiNetwork() {
        if (!isSupplicantStopped() && hasWrongAction()) {
            triggerDisableNMode();
        }
    }

    private void reconnectWifiNetwork() {
        Log.d("HwArpVerifier", "Atfer reassociate, network is still broken");
        WifiInfo wifiInfo = this.mWM.getConnectionInfo();
        Intent intent = new Intent(ACTION_ARP_RECONNECT_WIFI);
        if (wifiInfo != null) {
            intent.putExtra("ssid", wifiInfo.getSSID());
        }
        this.mContext.sendBroadcast(intent, "android.permission.ACCESS_WIFI_STATE");
    }

    private void notifyNetworkUnreachable() {
        this.mHwWifiCHRService.updateWifiException(87, "ARP_UNREACHABLE");
    }

    public static String readFileByChars(String fileName) {
        File file = new File(fileName);
        if (!file.exists() || !file.canRead()) {
            return BCM_ROAMING_FLAG_FILE;
        }
        InputStreamReader reader = null;
        char[] tempChars = new char[512];
        StringBuilder sb = new StringBuilder();
        try {
            if (Charset.isSupported("UTF-8")) {
                reader = new InputStreamReader(new FileInputStream(fileName), "UTF-8");
            } else if (Charset.isSupported("US-ASCII")) {
                reader = new InputStreamReader(new FileInputStream(fileName), "US-ASCII");
            } else {
                reader = new InputStreamReader(new FileInputStream(fileName), Charset.defaultCharset());
            }
            while (true) {
                int read = reader.read(tempChars);
                int charRead = read;
                if (read == -1) {
                    break;
                }
                sb.append(tempChars, 0, charRead);
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        } catch (IOException e1) {
            e1.printStackTrace();
            if (reader != null) {
                reader.close();
            }
        } catch (Throwable th) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e2) {
                }
            }
        }
        return sb.toString();
    }

    public static String writeFile(String fileName, String ctrl) {
        String result = "success";
        File file = new File(fileName);
        if (file.exists() && file.canWrite()) {
            OutputStream out = null;
            try {
                out = new FileOutputStream(file);
                out.write(ctrl.getBytes(Charset.defaultCharset()));
                out.flush();
                try {
                    out.close();
                } catch (IOException e) {
                }
            } catch (IOException ie) {
                result = "IOException occured";
                ie.printStackTrace();
                if (out != null) {
                    out.close();
                }
            } catch (Throwable th) {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e2) {
                    }
                }
            }
            return result;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("file is exists ");
        stringBuilder.append(file.exists());
        stringBuilder.append(" file can write ");
        stringBuilder.append(file.canWrite());
        Log.d("HwArpVerifier", stringBuilder.toString());
        return BCM_ROAMING_FLAG_FILE;
    }

    private final boolean pingGateway(int timeout) {
        if (this.mGateway == null) {
            return false;
        }
        if (!isIgnoreArpCheck()) {
            return NetWorkisReachable(this.mGateway, timeout);
        }
        Log.d("HwArpVerifier", "isIgnoreArpCheck is ture, ignore ping gateway");
        return true;
    }

    private final boolean NetWorkisReachable(String ipAddress, int timeout) {
        boolean ret = false;
        Log.d("HwArpVerifier", "NetWorkisReachable  enter");
        try {
            ret = Inet4Address.getByName(ipAddress).isReachable(timeout);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("NetWorkisReachable  ipAddress =");
            stringBuilder.append(ipAddress);
            stringBuilder.append(",ret=");
            stringBuilder.append(ret);
            Log.d("HwArpVerifier", stringBuilder.toString());
        } catch (Exception e) {
            Log.e("HwArpVerifier", "NetWorkisReachable fail");
        }
        Log.d("HwArpVerifier", "NetWorkisReachable");
        return ret;
    }

    private boolean isSupplicantStopped() {
        String suppcantStatus = BCM_ROAMING_FLAG_FILE;
        String chipType = SystemProperties.get("ro.connectivity.chiptype", BCM_ROAMING_FLAG_FILE);
        if (chipType == null || !chipType.equalsIgnoreCase("hi110x")) {
            suppcantStatus = SystemProperties.get("init.svc.p2p_supplicant", "running");
        } else {
            suppcantStatus = SystemProperties.get("init.svc.wpa_supplicant", "running");
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("wpa_supplicant state:");
        stringBuilder.append(suppcantStatus);
        Log.d("HwArpVerifier", stringBuilder.toString());
        return "stopped".equals(suppcantStatus);
    }

    private boolean hasWrongAction() {
        String value = readFileByChars(WIFI_WRONG_ACTION_FLAG);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("hasWrongAction:");
        stringBuilder.append(value);
        Log.d("HwArpVerifier", stringBuilder.toString());
        return "1".equals(value.trim());
    }

    private void triggerDisableNMode() {
        Log.d("HwArpVerifier", "triggerDisableNMode enter");
        writeFile(WIFI_ARP_TIMEOUT, "1");
    }

    private void reportArpDetail(String ipaddr, String hwaddr, int flag, String device) {
        this.mArpItems.add(new ArpItem(ipaddr, hwaddr, flag, device));
    }

    private void readArpFromFile() {
        this.mArpItems.clear();
        nativeReadArpDetail();
    }

    /* JADX WARNING: Removed duplicated region for block: B:52:0x009a A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:36:0x0095  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean needToDetectGateway() {
        HwCHRWebDetectThread.setFirstDetect(this.mFirstDetect);
        boolean z = false;
        if (this.mFirstDetect) {
            this.mFirstDetect = false;
            return true;
        }
        ArrayList<String> unfoundlist = new ArrayList();
        ArrayList<String> maclist = this.mHWGatewayVerifier.mGW.getGWMACAddrList();
        for (int i = maclist.size() - 1; i >= 0; i--) {
            Iterator it = this.mArpBlacklist.iterator();
            while (it.hasNext()) {
                ArpItem blackitem = (ArpItem) it.next();
                if (blackitem.matchMaxRetried()) {
                    if (blackitem.sameMacAddress((String) maclist.get(i))) {
                        maclist.remove(i);
                        break;
                    }
                }
            }
        }
        Iterator it2 = this.mArpItems.iterator();
        while (it2.hasNext()) {
            ArpItem arpitem = (ArpItem) it2.next();
            boolean found = true;
            Iterator it3 = maclist.iterator();
            while (it3.hasNext()) {
                String mac = (String) it3.next();
                if (arpitem.isValid() && arpitem.sameIpaddress(this.mGateway)) {
                    if (!arpitem.sameMacAddress(mac)) {
                        found = false;
                    } else if (arpitem.isStaticArp()) {
                        return false;
                    } else {
                        found = true;
                        if (found) {
                            unfoundlist.add(arpitem.hwaddr);
                        }
                    }
                }
            }
            if (found) {
            }
        }
        maclist.addAll(unfoundlist);
        if (maclist.size() > 1) {
            z = true;
        }
        return z;
    }

    public void startWifiRouteCheck() {
        if (this.mClientHandler != null) {
            Log.d("HwArpVerifier", "startWifiRouteCheck");
            if (this.mClientHandler.hasMessages(126)) {
                this.mClientHandler.removeMessages(126);
            }
            this.mClientHandler.sendEmptyMessageDelayed(126, 5000);
        }
    }

    public void stopWifiRouteCheck() {
        if (this.mClientHandler != null && this.mClientHandler.hasMessages(126)) {
            this.mClientHandler.removeMessages(126);
        }
    }

    private boolean isWifiDefaultRouteExist() {
        if (this.mRevLinkProperties == null) {
            throw new NullPointerException("mRevLinkProperties is null");
        } else if (this.mClientHandler == null) {
            return false;
        } else {
            String wifiRoutes = this.mClientHandler.getIpRouteTable();
            if (BETA_VER) {
                Log.d("HwArpVerifier", "---------  wifi route notify -------");
            }
            if (BETA_VER) {
                Log.d("HwArpVerifier", wifiRoutes);
            }
            if (BETA_VER) {
                Log.d("HwArpVerifier", "------------------------------------");
            }
            if (wifiRoutes != null) {
                String[] tok = wifiRoutes.toString().split("\n");
                if (tok == null) {
                    Log.e("HwArpVerifier", "wifi default route is not exist, tok==null");
                    return false;
                }
                int length = tok.length;
                int i = 0;
                while (i < length) {
                    String routeline = tok[i];
                    if (routeline.length() <= 10 || !routeline.startsWith("default") || routeline.indexOf(IFACE) < 0) {
                        i++;
                    } else {
                        Log.d("HwArpVerifier", "Notify wifi default route is ok");
                        return true;
                    }
                }
            }
            Log.e("HwArpVerifier", "wifi default route is not exist!");
            return false;
        }
    }

    private boolean wifiRepairRoute() {
        StringBuilder stringBuilder;
        int netid = -1;
        if (((ConnectivityManager) this.mContext.getSystemService("connectivity")) != null) {
            Network network = HwServiceFactory.getHwConnectivityManager().getNetworkForTypeWifi();
            if (network == null) {
                Log.e("HwArpVerifier", "wifiRepairRoute, network is null");
                return false;
            }
            netid = network.netId;
            stringBuilder = new StringBuilder();
            stringBuilder.append("netid = ");
            stringBuilder.append(netid);
            Log.d("HwArpVerifier", stringBuilder.toString());
        }
        Log.e("HwArpVerifier", "Enter wifiReparierRoute");
        if (this.mNwService == null || this.mRevLinkProperties == null) {
            Log.e("HwArpVerifier", "Repair wifi default Route failed, mNwService mRevLinkProperties is null");
            return false;
        } else if (!isConnectedToWifi()) {
            return false;
        } else {
            for (RouteInfo r : this.mRevLinkProperties.getRoutes()) {
                if (r.isDefaultRoute()) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("mRevLinkProperties=");
                    stringBuilder.append(this.mRevLinkProperties);
                    Log.d("HwArpVerifier", stringBuilder.toString());
                    if (netid > 0) {
                        try {
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("ifacename=");
                            stringBuilder2.append(this.mRevLinkProperties.getInterfaceName());
                            Log.d("HwArpVerifier", stringBuilder2.toString());
                            this.mNwService.addRoute(netid, r);
                            this.mNwService.setDefaultNetId(netid);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Log.d("HwArpVerifier", "RouteDetect addRoute finish");
                        return true;
                    }
                    Log.d("HwArpVerifier", "netid is not available");
                    return false;
                }
            }
            Log.d("HwArpVerifier", "Repair wifi default failed, no default route in mRevLinkProperties");
            return false;
        }
    }

    private void flushNetworkDnsCache() {
        if (this.mCM == null || this.mNwService == null) {
            Log.d("HwArpVerifier", "flushNetworkDnsCache failed: mCM or mNwService is null");
        } else if (isConnectedToWifi()) {
            Network network = HwServiceFactory.getHwConnectivityManager().getNetworkForTypeWifi();
            int netid = network == null ? -1 : network.netId;
            this.mLastNetworkId = netid;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("flushNetworkDnsCache netid:");
            stringBuilder.append(netid);
            Log.d("HwArpVerifier", stringBuilder.toString());
        }
    }

    private void flushVmDnsCache() {
        Intent intent = new Intent("android.intent.action.CLEAR_DNS_CACHE");
        intent.addFlags(536870912);
        intent.addFlags(67108864);
        long ident = Binder.clearCallingIdentity();
        try {
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    private void handleWiFiDnsStats(int networkid) {
        this.mHwWifiCHRService.handleWiFiDnsStats(networkid);
    }

    /* JADX WARNING: Missing block: B:16:0x0025, code skipped:
            if (r0 != null) goto L_0x0027;
     */
    /* JADX WARNING: Missing block: B:17:0x0027, code skipped:
            r0.close();
     */
    /* JADX WARNING: Missing block: B:23:0x0033, code skipped:
            if (r0 == null) goto L_0x0036;
     */
    /* JADX WARNING: Missing block: B:24:0x0036, code skipped:
            return -1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public long getGateWayArpRTT(int timeout) {
        HWArpPeer peer = null;
        try {
            peer = constructArpPeer();
            if (peer == null) {
                if (peer != null) {
                    peer.close();
                }
                return -1;
            }
            HWMultiGW multiGW = peer.getGateWayARPResponses(timeout);
            if (multiGW != null) {
                long arpRtt = multiGW.getArpRTT();
                if (peer != null) {
                    peer.close();
                }
                return arpRtt;
            }
        } catch (SocketException e) {
        } catch (Throwable th) {
            if (peer != null) {
                peer.close();
            }
        }
    }

    public boolean mssGatewayVerifier() {
        boolean arpRet = false;
        int i = 0;
        if (this.mGateway == null) {
            Log.d("HwArpVerifier", "HwMSSHandler: mGateway is null");
            return false;
        }
        while (true) {
            int i2 = i;
            if (i2 >= 3) {
                break;
            }
            long ret = getGateWayArpRTT(1000);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("HwMSSHandler: mssGatewayVerifier ret:");
            stringBuilder.append(ret);
            Log.d("HwArpVerifier", stringBuilder.toString());
            if (ret != -1) {
                arpRet = true;
                break;
            }
            i = i2 + 1;
        }
        return arpRet;
    }

    public void updateArpResult(boolean success, int arpRtt) {
        if (this.mHwQoEService == null) {
            this.mHwQoEService = HwQoEService.getInstance();
        }
        if (this.mHwQoEService != null) {
            this.mHwQoEService.updateArpResult(success, arpRtt);
        }
    }
}
