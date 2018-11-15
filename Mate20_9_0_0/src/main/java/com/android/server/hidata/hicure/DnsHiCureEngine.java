package com.android.server.hidata.hicure;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.LinkProperties;
import android.net.NetworkUtils;
import android.os.Handler;
import android.os.Message;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings.Global;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.SettingsObserver;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.ConnectivityService;
import com.android.server.connectivity.NetworkAgentInfo;
import com.android.server.security.deviceusage.ActivationMonitor;
import com.android.server.wifipro.WifiProCommonUtils;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DnsHiCureEngine extends StateMachine {
    private static final String CHR_BROADCAST_PERMISSION = "com.huawei.android.permission.GET_CHR_DATA";
    private static final int DELAY_TIME = 1000;
    private static final int DNS_FAILED_THRESHOLD = 2;
    public static final String DNS_MONITOR_FLAG = "hw.hicure.dns_fail_count";
    private static final String DORECOVERY_FLAG = "radio.data.stall.recovery.action";
    private static final int EVENT_DATA_CONNECTED = 102;
    private static final int EVENT_DATA_DISCONNECTED = 101;
    private static final int EVENT_DNS_HICURE = 104;
    private static final int EVENT_DNS_MONITOR = 103;
    private static final String EVENT_DNS_MONITOR_ACTION = "telephony.dataconnection.DNS_MONITOR_ACTION";
    private static final int EVENT_DNS_PUNISH = 108;
    private static final String EVENT_DNS_PUNISH_ACTION = "telephony.dataconnection.DNS_PUNISH_ACTION";
    private static final int EVENT_DNS_SET_BACK = 107;
    private static final int EVENT_DNS_SET_COMPLETE = 106;
    private static final int EVENT_PRIVATE_DNS_SETTINGS_CHANGED = 109;
    private static final int EVENT_SET_DNS = 105;
    private static final String[] GlobalDns = new String[]{"8.8.8.8", "208.67.222.222"};
    private static final String[] GlobalDomain = new String[]{"www.google.com", "www.youtube.com"};
    private static final String[] HomeDns = new String[]{"180.76.76.76", "223.5.5.5"};
    private static final String[] HomeDomain = new String[]{"www.baidu.com", "www.youku.com"};
    private static final String INTENT_DS_DNS_HICURE_RESULT = "com.android.intent.action.dns_hicure_result";
    private static final int ITERATION_TIME = 6000;
    private static final int PUNISH_TIME = 21600000;
    private static final String TAG = "DnsHiCureEngine";
    private static final int WAIT_TIME = 6000;
    private static DnsHiCureEngine mDnsHiCureEngine = null;
    private static final String[] mDnsWhiteList = new String[]{"10.8.2.1", "10.8.2.2"};
    private List<InetAddress> NetDns = new ArrayList();
    private String[] cellDnses = new String[2];
    private boolean dnsSetPunishFlag = false;
    private String[] dnses = new String[4];
    private String[] domains = new String[2];
    private boolean dsFlag = false;
    PhoneStateListener listenerSim0 = new PhoneStateListener(Integer.valueOf(0)) {
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            DnsHiCureEngine.this.mSignalStrengthSim0 = signalStrength;
        }

        public void onServiceStateChanged(ServiceState state) {
            DnsHiCureEngine.this.mServiceState0 = state;
        }
    };
    PhoneStateListener listenerSim1 = new PhoneStateListener(Integer.valueOf(1)) {
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            DnsHiCureEngine.this.mSignalStrengthSim1 = signalStrength;
        }

        public void onServiceStateChanged(ServiceState state) {
            DnsHiCureEngine.this.mServiceState1 = state;
        }
    };
    private final ContentResolver mContentResolver;
    private Context mContext;
    private State mDefaultState;
    private Handler mHandler;
    private State mHiCureState;
    private LinkProperties mLinkProperties = new LinkProperties();
    private State mMonitoredState;
    private int mNetId;
    private ServiceState mServiceState0 = null;
    private ServiceState mServiceState1 = null;
    private final SettingsObserver mSettingsObserver;
    private SignalStrength mSignalStrengthSim0 = null;
    private SignalStrength mSignalStrengthSim1 = null;
    private TelephonyManager mTelephonyManager;
    private State mUnmonitoredState;
    private String[] publicDns = new String[2];
    private boolean publicDnsFlag = false;
    private boolean useHostname = false;

    class DefaultState extends State {
        DefaultState() {
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            DnsHiCureEngine dnsHiCureEngine = DnsHiCureEngine.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DefaultState: ");
            stringBuilder.append(message.what);
            dnsHiCureEngine.log(stringBuilder.toString());
            return true;
        }
    }

    class HiCureState extends State {
        HiCureState() {
        }

        public void enter() {
            DnsHiCureEngine.this.log("HiCureState start");
            if (true == DnsHiCureEngine.this.publicDnsFlag) {
                DnsHiCureEngine.this.sendMessage(107);
            } else {
                DnsHiCureEngine.this.sendMessage(105);
            }
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 101) {
                DnsHiCureEngine.this.log("mHiCureState processMessage EVENT_DATA_DISCONNECTED");
                DnsHiCureEngine.this.transitionTo(DnsHiCureEngine.this.mUnmonitoredState);
                return true;
            } else if (i != 109) {
                switch (i) {
                    case 105:
                        DnsHiCureEngine.this.log("mHiCureState processMessage EVENT_SET_DNS");
                        DnsHiCureEngine.this.dnses[0] = DnsHiCureEngine.this.publicDns[0];
                        DnsHiCureEngine.this.dnses[1] = DnsHiCureEngine.this.cellDnses[0];
                        DnsHiCureEngine.this.dnses[2] = DnsHiCureEngine.this.publicDns[1];
                        DnsHiCureEngine.this.dnses[3] = DnsHiCureEngine.this.cellDnses[1];
                        DnsHiCureEngine.this.setDns(DnsHiCureEngine.this.dnses, DnsHiCureEngine.this.mLinkProperties, DnsHiCureEngine.this.mNetId);
                        DnsHiCureEngine.this.publicDnsFlag = true;
                        DnsHiCureEngine.this.sendMessageDelayed(106, 1000);
                        return true;
                    case 106:
                        DnsHiCureEngine.this.log("mHiCureState processMessage EVENT_DNS_SET_COMPLETE");
                        if (true == DnsHiCureEngine.this.isAddressReachable(DnsHiCureEngine.this.domains, 6000, DnsHiCureEngine.this.mNetId)) {
                            DnsHiCureEngine.this.notifyChrDnsHiCureResult(DnsHiCureEngine.this.cellDnses[0], DnsHiCureEngine.this.cellDnses[1], true);
                            DnsHiCureEngine.this.transitionTo(DnsHiCureEngine.this.mMonitoredState);
                            return true;
                        }
                        DnsHiCureEngine.this.notifyChrDnsHiCureResult(DnsHiCureEngine.this.cellDnses[0], DnsHiCureEngine.this.cellDnses[1], false);
                        DnsHiCureEngine.this.sendMessage(107);
                        return true;
                    case 107:
                        DnsHiCureEngine.this.log("mHiCureState processMessage EVENT_DNS_SET_BACK");
                        DnsHiCureEngine.this.dnses[0] = DnsHiCureEngine.this.cellDnses[0];
                        DnsHiCureEngine.this.dnses[1] = DnsHiCureEngine.this.cellDnses[1];
                        DnsHiCureEngine.this.setDns(DnsHiCureEngine.this.dnses, DnsHiCureEngine.this.mLinkProperties, DnsHiCureEngine.this.mNetId);
                        DnsHiCureEngine.this.publicDnsFlag = true;
                        DnsHiCureEngine.this.dnsSetPunishFlag = true;
                        DnsHiCureEngine.this.transitionTo(DnsHiCureEngine.this.mUnmonitoredState);
                        return true;
                    default:
                        DnsHiCureEngine dnsHiCureEngine = DnsHiCureEngine.this;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("mHiCureState: default message.what=");
                        stringBuilder.append(message.what);
                        dnsHiCureEngine.log(stringBuilder.toString());
                        return false;
                }
            } else {
                DnsHiCureEngine.this.log("mHiCureState processMessage EVENT_PRIVATE_DNS_SETTINGS_CHANGED");
                DnsHiCureEngine.this.updataprivateDNScfg();
                if (true != DnsHiCureEngine.this.useHostname) {
                    return true;
                }
                DnsHiCureEngine.this.transitionTo(DnsHiCureEngine.this.mUnmonitoredState);
                return true;
            }
        }
    }

    class MonitoredState extends State {
        Intent intent = new Intent(DnsHiCureEngine.EVENT_DNS_MONITOR_ACTION).setPackage(DnsHiCureEngine.this.mContext.getPackageName());
        AlarmManager mAlarmManager = ((AlarmManager) DnsHiCureEngine.this.mContext.getSystemService("alarm"));
        private int mCurrDnsFailedCounter;
        PendingIntent mDnsAlarmIntent = PendingIntent.getBroadcast(DnsHiCureEngine.this.mContext, 0, this.intent, 134217728);
        private int mLastDnsFailedCounter;

        MonitoredState() {
        }

        public void enter() {
            DnsHiCureEngine.this.log("MonitoredState start");
            this.mLastDnsFailedCounter = getCurrentDnsFailedCounter();
            this.mAlarmManager.set(3, SystemClock.elapsedRealtime() + 6000, this.mDnsAlarmIntent);
            DnsHiCureEngine.this.publicDns = DnsHiCureEngine.GlobalDns;
            DnsHiCureEngine.this.domains = DnsHiCureEngine.GlobalDomain;
            if (DnsHiCureEngine.this.mTelephonyManager == null) {
                DnsHiCureEngine.this.log("getSimOperator: mTelephonyManager is null, return!");
                return;
            }
            if (DnsHiCureEngine.this.mTelephonyManager.getSimOperator().startsWith(WifiProCommonUtils.COUNTRY_CODE_CN)) {
                DnsHiCureEngine.this.publicDns = DnsHiCureEngine.HomeDns;
                DnsHiCureEngine.this.domains = DnsHiCureEngine.HomeDomain;
            }
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 101) {
                DnsHiCureEngine.this.log("mMonitoredState processMessage EVENT_DATA_DISCONNECTED");
                DnsHiCureEngine.this.transitionTo(DnsHiCureEngine.this.mUnmonitoredState);
                return true;
            } else if (i != 109) {
                switch (i) {
                    case 103:
                        DnsHiCureEngine.this.log("mMonitoredState processMessage EVENT_DNS_MONITOR");
                        dnsMonitor();
                        return true;
                    case 104:
                        DnsHiCureEngine.this.log("mMonitoredState processMessage EVENT_DNS_HICURE");
                        DnsHiCureEngine.this.transitionTo(DnsHiCureEngine.this.mHiCureState);
                        return true;
                    default:
                        DnsHiCureEngine dnsHiCureEngine = DnsHiCureEngine.this;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("mMonitoredState: default message.what=");
                        stringBuilder.append(message.what);
                        dnsHiCureEngine.log(stringBuilder.toString());
                        return false;
                }
            } else {
                DnsHiCureEngine.this.log("mMonitoredState processMessage EVENT_PRIVATE_DNS_SETTINGS_CHANGED");
                DnsHiCureEngine.this.updataprivateDNScfg();
                if (!DnsHiCureEngine.this.useHostname) {
                    return true;
                }
                DnsHiCureEngine.this.transitionTo(DnsHiCureEngine.this.mUnmonitoredState);
                return true;
            }
        }

        public void exit() {
            this.mAlarmManager.cancel(this.mDnsAlarmIntent);
        }

        public void dnsMonitor() {
            this.mCurrDnsFailedCounter = getCurrentDnsFailedCounter();
            int deltaFailedDns = this.mCurrDnsFailedCounter - this.mLastDnsFailedCounter;
            this.mLastDnsFailedCounter = this.mCurrDnsFailedCounter;
            DnsHiCureEngine dnsHiCureEngine = DnsHiCureEngine.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("deltaFailedDns = ");
            stringBuilder.append(deltaFailedDns);
            dnsHiCureEngine.log(stringBuilder.toString());
            if (deltaFailedDns < 2 || !DnsHiCureEngine.this.isDnsCureSuitable() || DnsHiCureEngine.this.isAddressReachable(DnsHiCureEngine.this.domains, 6000, DnsHiCureEngine.this.mNetId) || DnsHiCureEngine.this.isRecoveryAction()) {
                this.mAlarmManager.set(3, SystemClock.elapsedRealtime() + 6000, this.mDnsAlarmIntent);
                return;
            }
            DnsHiCureEngine.this.log("isAddressReachable? no. SendMessage(EVENT_DNS_HICURE)");
            DnsHiCureEngine.this.sendMessage(104);
        }

        public int getCurrentDnsFailedCounter() {
            try {
                return Integer.parseInt(SystemProperties.get(DnsHiCureEngine.DNS_MONITOR_FLAG, "0"));
            } catch (NumberFormatException e) {
                DnsHiCureEngine.this.loge("NumberFormatException");
                return 0;
            }
        }
    }

    class UnmonitoredState extends State {
        Intent intent = new Intent(DnsHiCureEngine.EVENT_DNS_PUNISH_ACTION).setPackage(DnsHiCureEngine.this.mContext.getPackageName());
        AlarmManager mAlarmManager = ((AlarmManager) DnsHiCureEngine.this.mContext.getSystemService("alarm"));
        PendingIntent mDnsAlarmIntent = PendingIntent.getBroadcast(DnsHiCureEngine.this.mContext, 0, this.intent, 134217728);

        UnmonitoredState() {
        }

        public void enter() {
            DnsHiCureEngine.this.log("UnmonitoredState start");
            DnsHiCureEngine.this.updataprivateDNScfg();
            if (DnsHiCureEngine.this.dnsSetPunishFlag) {
                this.mAlarmManager.set(3, SystemClock.elapsedRealtime() + ActivationMonitor.SIX_HOURS_MS, this.mDnsAlarmIntent);
            }
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i != 102) {
                switch (i) {
                    case 108:
                        DnsHiCureEngine.this.log("mUnmonitoredState processMessagedefault EVENT_DNS_PUNISH");
                        DnsHiCureEngine.this.dnsSetPunishFlag = false;
                        judgeTransition();
                        return true;
                    case 109:
                        DnsHiCureEngine.this.log("mUnmonitoredState processMessage EVENT_PRIVATE_DNS_SETTINGS_CHANGED");
                        DnsHiCureEngine.this.updataprivateDNScfg();
                        judgeTransition();
                        return true;
                    default:
                        DnsHiCureEngine dnsHiCureEngine = DnsHiCureEngine.this;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("mUnmonitoredState: default message.what=");
                        stringBuilder.append(message.what);
                        dnsHiCureEngine.log(stringBuilder.toString());
                        return false;
                }
            }
            DnsHiCureEngine.this.log("mUnmonitoredState processMessage EVENT_DATA_CONNECTED");
            judgeTransition();
            return true;
        }

        private void judgeTransition() {
            if (!DnsHiCureEngine.this.useHostname && true == DnsHiCureEngine.this.dsFlag && !DnsHiCureEngine.this.dnsSetPunishFlag) {
                DnsHiCureEngine.this.transitionTo(DnsHiCureEngine.this.mMonitoredState);
            }
        }
    }

    public static synchronized DnsHiCureEngine getInstance(Context context) {
        DnsHiCureEngine dnsHiCureEngine;
        synchronized (DnsHiCureEngine.class) {
            if (mDnsHiCureEngine == null) {
                mDnsHiCureEngine = new DnsHiCureEngine(context);
            }
            dnsHiCureEngine = mDnsHiCureEngine;
        }
        return dnsHiCureEngine;
    }

    public static synchronized DnsHiCureEngine getInstance() {
        DnsHiCureEngine dnsHiCureEngine;
        synchronized (DnsHiCureEngine.class) {
            dnsHiCureEngine = mDnsHiCureEngine;
        }
        return dnsHiCureEngine;
    }

    private DnsHiCureEngine(Context context) {
        super(TAG);
        this.mContext = context;
        this.mContentResolver = this.mContext.getContentResolver();
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        registerListener();
        this.mMonitoredState = new MonitoredState();
        this.mUnmonitoredState = new UnmonitoredState();
        this.mHiCureState = new HiCureState();
        this.mDefaultState = new DefaultState();
        this.mHandler = getHandler();
        this.mSettingsObserver = new SettingsObserver(this.mContext, this.mHandler);
        registerPrivateDnsSettingsCallbacks();
        registerReceivers();
        addState(this.mDefaultState);
        addState(this.mMonitoredState, this.mDefaultState);
        addState(this.mUnmonitoredState, this.mDefaultState);
        addState(this.mHiCureState, this.mDefaultState);
        setInitialState(this.mUnmonitoredState);
        start();
        log("DnsHiCureEngine start!");
    }

    private void registerPrivateDnsSettingsCallbacks() {
        this.mSettingsObserver.observe(Global.getUriFor("private_dns_mode"), 109);
    }

    private void registerReceivers() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(EVENT_DNS_MONITOR_ACTION);
        intentFilter.addAction(EVENT_DNS_PUNISH_ACTION);
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                DnsHiCureEngine dnsHiCureEngine = DnsHiCureEngine.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onReceive: ");
                stringBuilder.append(intent.getAction());
                dnsHiCureEngine.log(stringBuilder.toString());
                if (DnsHiCureEngine.EVENT_DNS_MONITOR_ACTION.equals(intent.getAction())) {
                    DnsHiCureEngine.this.sendMessage(103);
                } else if (DnsHiCureEngine.EVENT_DNS_PUNISH_ACTION.equals(intent.getAction())) {
                    DnsHiCureEngine.this.sendMessage(108);
                }
            }
        }, intentFilter);
    }

    private void registerListener() {
        this.mTelephonyManager.listen(this.listenerSim0, 257);
        this.mTelephonyManager.listen(this.listenerSim1, 257);
    }

    private boolean isDnsCureSuitable() {
        int subId = SubscriptionManager.getDefaultDataSubscriptionId();
        boolean signalPoor = isSignalPoor(subId);
        boolean dataServiceIn = isDataServiceIn(subId);
        boolean isWhiteDns = isOneInWhiteList();
        boolean notCallState = isNotCalling();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isDnsCureSuitable ");
        stringBuilder.append(signalPoor);
        stringBuilder.append(",");
        stringBuilder.append(dataServiceIn);
        stringBuilder.append(",");
        stringBuilder.append(isWhiteDns);
        stringBuilder.append(",");
        stringBuilder.append(notCallState);
        log(stringBuilder.toString());
        return !signalPoor && dataServiceIn && !isWhiteDns && notCallState;
    }

    private boolean isSignalPoor(int subId) {
        SignalStrength signalStrength;
        boolean ret = true;
        int RAT_class = TelephonyManager.getNetworkClass(this.mTelephonyManager.getNetworkType(subId));
        if (subId == 0) {
            signalStrength = this.mSignalStrengthSim0;
        } else {
            signalStrength = this.mSignalStrengthSim1;
        }
        if (signalStrength == null) {
            log("signalStrength is null");
            return true;
        }
        if (1 != RAT_class && signalStrength.getLevel() >= 2) {
            ret = false;
        }
        return ret;
    }

    private boolean isDataServiceIn(int subId) {
        ServiceState serviceState;
        boolean ret = false;
        if (subId == 0) {
            serviceState = this.mServiceState0;
        } else {
            serviceState = this.mServiceState1;
        }
        if (serviceState == null) {
            log("serviceState is null");
            return false;
        }
        if (serviceState.getDataRegState() == 0) {
            ret = true;
        }
        return ret;
    }

    private boolean isNotCalling() {
        int callState0 = this.mTelephonyManager.getCallState(0);
        int callState1 = this.mTelephonyManager.getCallState(1);
        if (callState0 == 0 && callState1 == 0) {
            return true;
        }
        return false;
    }

    private boolean isOneInWhiteList() {
        return isDnsInWhiteList(this.cellDnses[0]) || isDnsInWhiteList(this.cellDnses[1]);
    }

    private boolean isDnsInWhiteList(String dns) {
        for (String wDns : mDnsWhiteList) {
            if (wDns.equals(dns)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAddressReachable(String[] domain, int time, int netid) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isAddressReachable starttime:");
        stringBuilder.append(SystemClock.elapsedRealtime());
        log(stringBuilder.toString());
        return new DnsProbe(time, netid).isDnsAvailable(domain);
    }

    private void setDns(String[] ndnses, LinkProperties lp, int netid) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setdns:publicDnsFlag=");
        stringBuilder.append(this.publicDnsFlag);
        log(stringBuilder.toString());
        Collection<InetAddress> newdnses = new ArrayList();
        for (String dnsAddr : ndnses) {
            String dnsAddr2;
            if (!(dnsAddr2 == null || dnsAddr2.isEmpty())) {
                dnsAddr2 = dnsAddr2.trim();
                try {
                    InetAddress ia = NetworkUtils.numericToInetAddress(dnsAddr2);
                    if (!ia.isAnyLocalAddress()) {
                        newdnses.add(ia);
                    }
                } catch (IllegalArgumentException e) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Non-numeric dns addr=");
                    stringBuilder2.append(dnsAddr2);
                    log(stringBuilder2.toString());
                }
            }
        }
        if (lp != null) {
            lp.setDnsServers(newdnses);
            setDnsConfigurationForNetwork(netid, lp, false);
        }
    }

    private void setDnsConfigurationForNetwork(int netid, LinkProperties newlinkproperties, boolean isdefaultnetwork) {
        try {
            ((ConnectivityService) ServiceManager.getService("connectivity")).getDnsManager().setDnsConfigurationForNetwork(netid, newlinkproperties, isdefaultnetwork);
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Exception in setDnsConfigurationForNetwork: ");
            stringBuilder.append(e);
            loge(stringBuilder.toString());
        }
    }

    private boolean isRecoveryAction() {
        int action = 0;
        try {
            action = System.getInt(this.mContentResolver, DORECOVERY_FLAG);
        } catch (SettingNotFoundException e) {
            log("Settings Exception Reading Dorecovery Values");
        }
        if (action > 0) {
            return true;
        }
        return false;
    }

    private void updataprivateDNScfg() {
        if ("hostname".equals(Global.getString(this.mContentResolver, "private_dns_mode"))) {
            this.useHostname = true;
        } else {
            this.useHostname = false;
        }
        this.publicDnsFlag = false;
    }

    public void notifyConnectedInfo(NetworkAgentInfo nai) {
        if (nai != null) {
            this.mLinkProperties = new LinkProperties(nai.linkProperties);
            this.mNetId = nai.network.netId;
            this.publicDnsFlag = false;
            this.NetDns = this.mLinkProperties.getDnsServers();
            if (this.NetDns == null) {
                log("NetDns = null");
            } else if (this.NetDns.size() < 2) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("NetDns.size() = ");
                stringBuilder.append(this.NetDns.size());
                log(stringBuilder.toString());
            } else {
                String temp = ((InetAddress) this.NetDns.get(0)).toString();
                this.cellDnses[0] = temp.substring(1, temp.length());
                String temp2 = ((InetAddress) this.NetDns.get(1)).toString();
                this.cellDnses[1] = temp2.substring(1, temp2.length());
                this.dsFlag = true;
                sendMessage(102);
            }
        }
    }

    public void notifyDisconnectedInfo() {
        this.mLinkProperties = null;
        this.mNetId = 0;
        this.dsFlag = false;
        sendMessage(101);
    }

    public void notifyChrDnsHiCureResult(String firstip, String secondip, boolean result) {
        Intent intent = new Intent(INTENT_DS_DNS_HICURE_RESULT);
        intent.putExtra("FirstDnsAddress", firstip);
        intent.putExtra("SecondDnsAddress", secondip);
        intent.putExtra("HiCureResult", result);
        log("sendBroadcast com.android.intent.action.dns_hicure_result");
        this.mContext.sendBroadcast(intent, CHR_BROADCAST_PERMISSION);
    }
}
