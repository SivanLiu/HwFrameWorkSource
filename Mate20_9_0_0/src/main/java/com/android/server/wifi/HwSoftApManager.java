package com.android.server.wifi;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager.SoftApCallback;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.telephony.HwTelephonyManagerInner;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.server.wifi.util.ApConfigUtil;
import com.android.server.wifi.util.WifiCommonUtils;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import libcore.io.IoUtils;

public class HwSoftApManager extends SoftApManager {
    private static final String ACTION_WIFI_AP_STA_JOIN = "android.net.wifi.WIFI_AP_STA_JOIN";
    private static final String ACTION_WIFI_AP_STA_LEAVE = "android.net.wifi.WIFI_AP_STA_LEAVE";
    private static final String ANONYMOUS_MAC = "**:**:**:**";
    private static final int ANONYMOUS_MAC_INDEX = 11;
    private static final int BROADCAST_SSID_MENU_DISPLAY = 1;
    private static final int BROADCAST_SSID_MENU_HIDE = 0;
    private static boolean DBG = HWFLOW;
    private static final int DEFAULT_ISMCOEX_WIFI_AP_CHANNEL = 11;
    private static final int DEFAULT_WIFI_AP_CHANNEL = 0;
    private static final int DEFAULT_WIFI_AP_MAXSCB = 8;
    private static final String EXTRA_CURRENT_TIME = "currentTime";
    private static final String EXTRA_STA_COUNT = "staCount";
    private static final String EXTRA_STA_INFO = "macInfo";
    protected static final boolean HWFLOW;
    private static final String ISM_COEX_ON = "ro.config.hw_ismcoex";
    private static final int MAX_AP_LINKED_COUNT = 8;
    private static final int MIN_DHCPLEASE_LENGTH = 4;
    private static final int NT_CHINA_CMCC = 3;
    private static final int NT_CHINA_UT = 2;
    private static final int NT_FOREIGN = 1;
    private static final int NT_UNREG = 0;
    private static final String PATH_DHCP_FILE = "/data/misc/dhcp/dnsmasq.leases";
    private static final int SEND_BROADCAST = 0;
    private static final int STA_JOIN_HANDLE_DELAY = 5000;
    private static final String TAG = "HwSoftApManager";
    private Handler mApLinkedStaChangedHandler = new Handler() {
        public void handleMessage(Message msg) {
            String str;
            StringBuilder stringBuilder;
            String action = null;
            long mCurrentTime = System.currentTimeMillis();
            Bundle bundle = msg.getData();
            String event = bundle.getString("event_key");
            String macAddress = bundle.getString("mac_key").toLowerCase();
            if ("STA_JOIN".equals(event)) {
                action = HwSoftApManager.ACTION_WIFI_AP_STA_JOIN;
                if (HwSoftApManager.this.mMacList.contains(macAddress)) {
                    str = HwSoftApManager.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(HwSoftApManager.this.anonyMac(macAddress));
                    stringBuilder.append(" had been added, but still get event ");
                    stringBuilder.append(event);
                    Log.e(str, stringBuilder.toString());
                    HwSoftApManager.this.updateLinkedInfo();
                } else {
                    HwSoftApManager.this.mMacList.add(macAddress);
                    HwSoftApManager.this.mLinkedStaCount = HwSoftApManager.this.mLinkedStaCount + 1;
                }
            } else if ("STA_LEAVE".equals(event)) {
                action = HwSoftApManager.ACTION_WIFI_AP_STA_LEAVE;
                if (HwSoftApManager.this.mApLinkedStaChangedHandler.hasMessages(msg.what, "STA_JOIN") || HwSoftApManager.this.mMacList.contains(macAddress)) {
                    if (HwSoftApManager.this.mApLinkedStaChangedHandler.hasMessages(msg.what, "STA_JOIN")) {
                        HwSoftApManager.this.mApLinkedStaChangedHandler.removeMessages(msg.what, "STA_JOIN");
                        str = HwSoftApManager.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("event=");
                        stringBuilder.append(event);
                        stringBuilder.append(", remove STA_JOIN message, mac=");
                        stringBuilder.append(HwSoftApManager.this.anonyMac(macAddress));
                        Log.d(str, stringBuilder.toString());
                    }
                    if (HwSoftApManager.this.mMacList.contains(macAddress)) {
                        HwSoftApManager.this.mMacList.remove(macAddress);
                        HwSoftApManager.this.mLinkedStaCount = HwSoftApManager.this.mLinkedStaCount - 1;
                    }
                } else {
                    str = HwSoftApManager.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(HwSoftApManager.this.anonyMac(macAddress));
                    stringBuilder.append(" had been removed, but still get event ");
                    stringBuilder.append(event);
                    Log.e(str, stringBuilder.toString());
                    HwSoftApManager.this.updateLinkedInfo();
                }
            }
            str = HwSoftApManager.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("ApLinkedStaChanged message handled, event=");
            stringBuilder.append(event);
            stringBuilder.append(" mac=");
            stringBuilder.append(HwSoftApManager.this.anonyMac(macAddress));
            stringBuilder.append(", mLinkedStaCount=");
            stringBuilder.append(HwSoftApManager.this.mLinkedStaCount);
            Log.d(str, stringBuilder.toString());
            if (HwSoftApManager.this.mLinkedStaCount < 0 || HwSoftApManager.this.mLinkedStaCount > 8 || HwSoftApManager.this.mLinkedStaCount != HwSoftApManager.this.mMacList.size()) {
                str = HwSoftApManager.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("mLinkedStaCount over flow, need synchronize. mLinkedStaCount=");
                stringBuilder.append(HwSoftApManager.this.mLinkedStaCount);
                stringBuilder.append(", mMacList.size()=");
                stringBuilder.append(HwSoftApManager.this.mMacList.size());
                Log.e(str, stringBuilder.toString());
                HwSoftApManager.this.updateLinkedInfo();
            }
            str = String.format("MAC=%s TIME=%d STACNT=%d", new Object[]{HwSoftApManager.this.anonyMac(macAddress), Long.valueOf(mCurrentTime), Integer.valueOf(HwSoftApManager.this.mLinkedStaCount)});
            String str2 = HwSoftApManager.TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Send broadcast: ");
            stringBuilder2.append(action);
            stringBuilder2.append(", event=");
            stringBuilder2.append(event);
            stringBuilder2.append(", extraInfo: ");
            stringBuilder2.append(str);
            Log.d(str2, stringBuilder2.toString());
            Intent broadcast = new Intent(action);
            broadcast.addFlags(16777216);
            broadcast.putExtra(HwSoftApManager.EXTRA_STA_INFO, macAddress);
            broadcast.putExtra(HwSoftApManager.EXTRA_CURRENT_TIME, mCurrentTime);
            broadcast.putExtra(HwSoftApManager.EXTRA_STA_COUNT, HwSoftApManager.this.mLinkedStaCount);
            HwSoftApManager.this.mContext.sendBroadcast(broadcast, "android.permission.ACCESS_WIFI_STATE");
        }
    };
    private Context mContext;
    private int mDataSub = -1;
    private HwWifiCHRService mHwWifiCHRService;
    private int mLinkedStaCount = 0;
    private List<String> mMacList = new ArrayList();
    private String mOperatorNumericSub0 = null;
    private String mOperatorNumericSub1 = null;
    private PhoneStateListener[] mPhoneStateListener;
    private int mServiceStateSub0 = 1;
    private int mServiceStateSub1 = 1;
    private TelephonyManager mTelephonyManager;
    private WifiChannelXmlParse mWifiChannelXmlParse = null;

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        HWFLOW = z;
    }

    public HwSoftApManager(Context context, Looper looper, FrameworkFacade frameworkFacade, WifiNative wifiNative, String countryCode, SoftApCallback callback, WifiApConfigStore wifiApConfigStore, SoftApModeConfiguration config, WifiMetrics wifiMetrics) {
        super(context, looper, frameworkFacade, wifiNative, countryCode, callback, wifiApConfigStore, config, wifiMetrics);
        this.mContext = context;
        this.mHwWifiCHRService = HwWifiServiceFactory.getHwWifiCHRService();
    }

    private void registerPhoneStateListener(Context context) {
        this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
        this.mPhoneStateListener = new PhoneStateListener[2];
        for (int i = 0; i < 2; i++) {
            this.mPhoneStateListener[i] = getPhoneStateListener(i);
            this.mTelephonyManager.listen(this.mPhoneStateListener[i], 1);
        }
    }

    private PhoneStateListener getPhoneStateListener(int subId) {
        return new PhoneStateListener(Integer.valueOf(subId)) {
            public void onServiceStateChanged(ServiceState state) {
                if (state != null) {
                    if (HwSoftApManager.DBG) {
                        String str = HwSoftApManager.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("PhoneStateListener ");
                        stringBuilder.append(this.mSubId);
                        Log.d(str, stringBuilder.toString());
                    }
                    if (this.mSubId.intValue() == 0) {
                        HwSoftApManager.this.mServiceStateSub0 = state.getDataRegState();
                        HwSoftApManager.this.mOperatorNumericSub0 = state.getOperatorNumeric();
                    } else if (this.mSubId.intValue() == 1) {
                        HwSoftApManager.this.mServiceStateSub1 = state.getDataRegState();
                        HwSoftApManager.this.mOperatorNumericSub1 = state.getOperatorNumeric();
                    }
                }
            }
        };
    }

    private int getRegistedNetworkType() {
        int serviceState;
        String numeric;
        if (this.mDataSub == 0) {
            serviceState = this.mServiceStateSub0;
            numeric = this.mOperatorNumericSub0;
        } else if (this.mDataSub != 1) {
            return 0;
        } else {
            serviceState = this.mServiceStateSub0;
            numeric = this.mOperatorNumericSub0;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isRegistedNetworkType mDataSub ");
        stringBuilder.append(this.mDataSub);
        stringBuilder.append(", serviceState ");
        stringBuilder.append(serviceState);
        stringBuilder.append(" , numeric ");
        stringBuilder.append(numeric);
        Log.d(str, stringBuilder.toString());
        if (serviceState != 0 || (numeric != null && numeric.length() >= 5 && numeric.substring(0, 5).equals("99999"))) {
            return 0;
        }
        if (numeric == null || numeric.length() < 3 || !numeric.substring(0, 3).equals("460")) {
            return (numeric == null || numeric.equals("")) ? 0 : 1;
        } else {
            if ("46000".equals(this.mOperatorNumericSub0) || "46002".equals(this.mOperatorNumericSub0) || "46007".equals(this.mOperatorNumericSub0)) {
                return 3;
            }
            return 2;
        }
    }

    private String getCurrentBand() {
        String ret = null;
        String[] bandrst = HwTelephonyManagerInner.getDefault().queryServiceCellBand();
        if (bandrst != null) {
            if (bandrst.length < 2) {
                if (DBG) {
                    Log.d(TAG, "getCurrentBand bandrst error.");
                }
                return null;
            } else if ("GSM".equals(bandrst[0])) {
                switch (Integer.parseInt(bandrst[1])) {
                    case 0:
                        ret = "GSM850";
                        break;
                    case 1:
                        ret = "GSM900";
                        break;
                    case 2:
                        ret = "GSM1800";
                        break;
                    case 3:
                        ret = "GSM1900";
                        break;
                    default:
                        Log.e(TAG, "should not be here.");
                        break;
                }
            } else if ("CDMA".equals(bandrst[0])) {
                ret = "BC0";
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(bandrst[0]);
                stringBuilder.append(bandrst[1]);
                ret = stringBuilder.toString();
            }
        }
        if (DBG) {
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getCurrentBand rst is ");
            stringBuilder2.append(ret);
            Log.d(str, stringBuilder2.toString());
        }
        return ret;
    }

    private ArrayList<Integer> getAllowed2GChannels(ArrayList<Integer> allowedChannels) {
        int networkType = getRegistedNetworkType();
        ArrayList<Integer> intersectChannels = new ArrayList();
        if (allowedChannels == null) {
            return null;
        }
        if (networkType == 3) {
            intersectChannels.add(Integer.valueOf(6));
        } else if (networkType == 2) {
            intersectChannels.add(Integer.valueOf(1));
            intersectChannels.add(Integer.valueOf(6));
        } else if (networkType == 1) {
            this.mWifiChannelXmlParse = WifiChannelXmlParse.getInstance();
            ArrayList<Integer> vaildChannels = this.mWifiChannelXmlParse.getValidChannels(getCurrentBand(), true);
            intersectChannels = (ArrayList) allowedChannels.clone();
            if (vaildChannels != null) {
                intersectChannels.retainAll(vaildChannels);
            }
            if (intersectChannels.size() == 0) {
                intersectChannels = allowedChannels;
            }
        } else {
            intersectChannels = allowedChannels;
        }
        if (DBG) {
            StringBuilder sb = new StringBuilder();
            sb.append("channels: ");
            Iterator it = intersectChannels.iterator();
            while (it.hasNext()) {
                Integer channel = (Integer) it.next();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(channel.toString());
                stringBuilder.append(",");
                sb.append(stringBuilder.toString());
            }
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("2G ");
            stringBuilder2.append(sb);
            Log.d(str, stringBuilder2.toString());
        }
        return intersectChannels;
    }

    /* JADX WARNING: Removed duplicated region for block: B:31:0x009c  */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x004f  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int[] getAllowed5GChannels(WifiNative wifiNative) {
        int[] allowedChannels = wifiNative.getChannelsForBand(2);
        if (allowedChannels == null || allowedChannels.length <= 1) {
            return allowedChannels;
        }
        int counter;
        int counter2;
        int[] values = new int[allowedChannels.length];
        this.mWifiChannelXmlParse = WifiChannelXmlParse.getInstance();
        int i = 0;
        ArrayList<Integer> vaildChannels = this.mWifiChannelXmlParse.getValidChannels(getCurrentBand(), false);
        int counter3 = 0;
        if (vaildChannels != null) {
            counter = counter3;
            counter3 = 0;
            while (counter3 < allowedChannels.length) {
                try {
                    if (vaildChannels.contains(Integer.valueOf(ApConfigUtil.convertFrequencyToChannel(allowedChannels[counter3])))) {
                        counter2 = counter + 1;
                        try {
                            values[counter] = allowedChannels[counter3];
                            counter = counter2;
                        } catch (Exception e) {
                            counter3 = e;
                            counter = counter2;
                        }
                    }
                    counter3++;
                } catch (Exception e2) {
                    counter3 = e2;
                    counter3.printStackTrace();
                    if (counter != 0) {
                    }
                }
            }
        } else {
            counter = counter3;
        }
        StringBuilder stringBuilder;
        String str;
        if (counter != 0) {
            Log.d(TAG, "5G counter is 0");
            if (DBG) {
                StringBuilder sb = new StringBuilder();
                sb.append("allowedChannels channels: ");
                while (i < allowedChannels.length) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(ApConfigUtil.convertFrequencyToChannel(allowedChannels[i]));
                    stringBuilder.append(",");
                    sb.append(stringBuilder.toString());
                    i++;
                }
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("5G ");
                stringBuilder.append(sb);
                Log.d(str, stringBuilder.toString());
            }
            return allowedChannels;
        }
        int[] intersectChannels = new int[counter];
        for (counter2 = 0; counter2 < counter; counter2++) {
            intersectChannels[counter2] = values[counter2];
        }
        if (DBG) {
            StringBuilder stringBuilder2;
            stringBuilder = new StringBuilder();
            stringBuilder.append("allowedChannels channels: ");
            for (int convertFrequencyToChannel : allowedChannels) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(ApConfigUtil.convertFrequencyToChannel(convertFrequencyToChannel));
                stringBuilder3.append(",");
                stringBuilder.append(stringBuilder3.toString());
            }
            stringBuilder.append("intersectChannels channels: ");
            while (i < counter) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(ApConfigUtil.convertFrequencyToChannel(intersectChannels[i]));
                stringBuilder2.append(",");
                stringBuilder.append(stringBuilder2.toString());
                i++;
            }
            str = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("5G ");
            stringBuilder2.append(stringBuilder.toString());
            Log.d(str, stringBuilder2.toString());
        }
        return intersectChannels;
    }

    public void start() {
        super.start();
        WifiStateMachine wsm = WifiInjector.getInstance().getWifiStateMachine();
        if (wsm instanceof HwWifiStateMachine) {
            ((HwWifiStateMachine) wsm).registHwSoftApManager(this);
        }
    }

    public void stop() {
        super.stop();
        WifiStateMachine wsm = WifiInjector.getInstance().getWifiStateMachine();
        if (wsm instanceof HwWifiStateMachine) {
            ((HwWifiStateMachine) wsm).clearHwSoftApManager();
        }
    }

    public int updateApChannelConfig(WifiNative wifiNative, String countryCode, ArrayList<Integer> allowed2GChannels, WifiConfiguration config) {
        if (!wifiNative.isHalStarted()) {
            config.apBand = 0;
            config.apChannel = 6;
            return 0;
        } else if (config.apBand == 1 && countryCode == null) {
            Log.e(TAG, "5GHz band is not allowed without country code");
            return 2;
        } else {
            if (config.apChannel == 0) {
                config.apChannel = ApConfigUtil.chooseApChannel(config.apBand, getAllowed2GChannels(allowed2GChannels), getAllowed5GChannels(wifiNative));
                if (config.apChannel == -1) {
                    config.apBand = 0;
                    config.apChannel = 6;
                }
            }
            if (this.mHwWifiCHRService != null) {
                Bundle data = new Bundle();
                data.putInt("apBand", config.apBand);
                data.putString("apRat", getCurrentBand());
                data.putInt("apChannel", config.apChannel);
                this.mHwWifiCHRService.uploadDFTEvent(2, data);
            }
            if (DBG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("updateApChannelConfig apChannel: ");
                stringBuilder.append(config.apChannel);
                Log.d(str, stringBuilder.toString());
            }
            return 0;
        }
    }

    public int getApChannel(WifiConfiguration config) {
        int apChannel;
        if (config.apBand == 0 && SystemProperties.getBoolean(ISM_COEX_ON, false)) {
            apChannel = 11;
        } else {
            apChannel = Secure.getInt(this.mContext.getContentResolver(), "wifi_ap_channel", 0);
            if (apChannel == 0 || ((config.apBand == 0 && apChannel > 14) || (config.apBand == 1 && apChannel < 34))) {
                apChannel = config.apChannel;
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("softap channel=");
        stringBuilder.append(apChannel);
        Log.d(str, stringBuilder.toString());
        return apChannel;
    }

    public void notifyApLinkedStaListChange(Bundle bundle) {
        if (bundle == null) {
            Log.e(TAG, "notifyApLinkedStaListChange: get bundle is null");
            return;
        }
        int macHashCode = 0;
        String macAddress = bundle.getString("mac_key");
        if (macAddress != null) {
            macHashCode = macAddress.hashCode();
        }
        String event = bundle.getString("event_key");
        Message msg = new Message();
        msg.what = macHashCode;
        msg.obj = event;
        msg.setData(bundle);
        if ("STA_JOIN".equals(event)) {
            this.mApLinkedStaChangedHandler.sendMessageDelayed(msg, 5000);
        } else if ("STA_LEAVE".equals(event)) {
            this.mApLinkedStaChangedHandler.sendMessage(msg);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Message sent to ApLinkedStaChangedHandler,event= ");
        stringBuilder.append(event);
        stringBuilder.append(" , mac=");
        stringBuilder.append(anonyMac(macAddress));
        Log.d(str, stringBuilder.toString());
    }

    private String[] getApLinkedMacListByNative() {
        Log.d(TAG, "getApLinkedMacListByNative is called");
        String softapClients = WifiInjector.getInstance().getWifiNative().getSoftapClientsHw();
        if (softapClients != null && !softapClients.isEmpty()) {
            return softapClients.split("\\n");
        }
        Log.e(TAG, "getApLinkedMacListByNative Error: getSoftapClientsHw return NULL or empyt string");
        return null;
    }

    private void updateLinkedInfo() {
        String[] macList = getApLinkedMacListByNative();
        this.mMacList = new ArrayList();
        int i = 0;
        if (macList == null) {
            this.mLinkedStaCount = 0;
            return;
        }
        int length = macList.length;
        while (i < length) {
            String mac = macList[i];
            if (mac == null) {
                Log.e(TAG, "get mac from macList is null");
            } else {
                this.mMacList.add(mac.toLowerCase());
            }
            i++;
        }
        this.mLinkedStaCount = this.mMacList.size();
    }

    public List<String> getApLinkedStaList() {
        Log.d(TAG, "getApLinkedStaList is called");
        if (this.mMacList == null || this.mMacList.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> dhcpList = readSoftapStaDhcpInfo();
        List<String> infoList = new ArrayList();
        int macListSize = this.mMacList.size();
        for (int index = 0; index < macListSize; index++) {
            infoList.add(getApLinkedStaInfo((String) this.mMacList.get(index), dhcpList));
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getApLinkedStaList: info size=");
        stringBuilder.append(infoList.size());
        Log.d(str, stringBuilder.toString());
        return infoList;
    }

    @Deprecated
    private List<String> getApLinkedDhcpList() {
        Log.d(TAG, "getApLinkedDhcpList: softap getdhcplease");
        String softapDhcpLease = WifiInjector.getInstance().getWifiNative().readSoftapDhcpLeaseFileHw();
        if (softapDhcpLease == null || softapDhcpLease.isEmpty()) {
            Log.e(TAG, "getApLinkedDhcpList Error: readSoftapDhcpLeaseFileHw return NULL or empty string");
            return null;
        }
        String[] dhcpleaseList = softapDhcpLease.split("\\n");
        List<String> dhcpList = new ArrayList();
        for (String dhcplease : dhcpleaseList) {
            dhcpList.add(dhcplease);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getApLinkedDhcpList: mDhcpList size=");
        stringBuilder.append(dhcpList.size());
        Log.d(str, stringBuilder.toString());
        return dhcpList;
    }

    private List<String> readSoftapStaDhcpInfo() {
        StringBuilder stringBuilder;
        List<String> dhcpInfos = new ArrayList();
        FileReader fileReader = null;
        BufferedReader bufferedReader = null;
        String readLine;
        try {
            fileReader = new FileReader(PATH_DHCP_FILE);
            bufferedReader = new BufferedReader(fileReader);
            String line = "";
            while (true) {
                readLine = bufferedReader.readLine();
                line = readLine;
                if (readLine == null) {
                    break;
                }
                dhcpInfos.add(line);
            }
        } catch (FileNotFoundException e) {
            readLine = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to read file /data/misc/dhcp/dnsmasq.leases, message: ");
            stringBuilder.append(e.getMessage());
            Log.e(readLine, stringBuilder.toString());
        } catch (IOException e2) {
            readLine = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to read softap sta dhcp info: ");
            stringBuilder.append(e2.getMessage());
            Log.e(readLine, stringBuilder.toString());
        } catch (Throwable th) {
            IoUtils.closeQuietly(null);
            IoUtils.closeQuietly(null);
        }
        IoUtils.closeQuietly(bufferedReader);
        IoUtils.closeQuietly(fileReader);
        return dhcpInfos;
    }

    private String getApLinkedStaInfo(String mac, List<String> dhcpList) {
        String apLinkedStaInfo = String.format("MAC=%s", new Object[]{mac});
        mac = mac.toLowerCase();
        if (dhcpList != null) {
            for (String dhcplease : dhcpList) {
                if (dhcplease.contains(mac)) {
                    if (4 <= dhcplease.split(" ").length) {
                        Log.d(TAG, "getApLinkedStaInfo: dhcplease token");
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(apLinkedStaInfo);
                        stringBuilder.append(" IP=%s DEVICE=%s");
                        apLinkedStaInfo = String.format(stringBuilder.toString(), new Object[]{tokens[2], tokens[3]});
                    }
                }
            }
        }
        return apLinkedStaInfo;
    }

    private String anonyMac(String mac) {
        if (mac == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(mac);
        sb.replace(0, 11, ANONYMOUS_MAC);
        return sb.toString();
    }

    public boolean isHideBroadcastSsid() {
        boolean isHideSsid = false;
        if (this.mContext == null) {
            Log.e(TAG, "error mContext is null");
            return false;
        }
        boolean z = true;
        if (1 == System.getInt(this.mContext.getContentResolver(), "show_broadcast_ssid_config", 0)) {
            if (Secure.getInt(this.mContext.getContentResolver(), "wifi_ap_ignorebroadcastssid", 0) == 0) {
                z = false;
            }
            isHideSsid = z;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isHideSsid = ");
            stringBuilder.append(isHideSsid);
            Log.i(str, stringBuilder.toString());
        }
        return isHideSsid;
    }

    public void clearCallbacksAndMessages() {
        this.mApLinkedStaChangedHandler.removeCallbacksAndMessages(null);
    }

    protected void updateApState(int newState, int currentState, int reason) {
        if (newState == 13) {
            handleSetWifiApConfigurationHw();
        }
        super.updateApState(newState, currentState, reason);
    }

    private void handleSetWifiApConfigurationHw() {
        String apChannel = String.valueOf(getApChannel(this.mApConfig));
        int maxScb = Secure.getInt(this.mContext.getContentResolver(), "wifi_ap_maxscb", 8);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HandleSetWifiApConfigurationHw is called, channel:");
        stringBuilder.append(apChannel);
        stringBuilder.append(" maxScb: ");
        stringBuilder.append(maxScb);
        Log.d(str, stringBuilder.toString());
        if (!WifiInjector.getInstance().getWifiNative().setSoftapHw(apChannel, String.valueOf(maxScb))) {
            Log.e(TAG, "Failed to setSoftapHw");
        }
    }

    void setSoftApDisassociateSta(String mac) {
        if (!WifiInjector.getInstance().getWifiNative().disassociateSoftapStaHw(mac)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to disassociateSoftapStaHw, mac: ");
            stringBuilder.append(mac);
            Log.e(str, stringBuilder.toString());
        }
    }

    void setSoftapMacFilter(String macFilter) {
        if (!WifiInjector.getInstance().getWifiNative().setSoftapMacFltrHw(macFilter)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to setSoftapMacFltrHw, macFilter: ");
            stringBuilder.append(macFilter);
            Log.e(str, stringBuilder.toString());
        }
    }

    static int[] getSoftApChannelListFor5G() {
        int[] channels = WifiInjector.getInstance().getWifiNative().getChannelsForBand(2);
        if (channels != null && channels.length > 0) {
            for (int i = 0; i < channels.length; i++) {
                channels[i] = WifiCommonUtils.convertFrequencyToChannelNumber(channels[i]);
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Got channels for 5G band: ");
        stringBuilder.append(Arrays.toString(channels));
        Log.d(str, stringBuilder.toString());
        return channels;
    }
}
