package com.android.internal.telephony;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.text.TextUtils;
import android.util.LocalLog;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class CarrierSignalAgent extends Handler {
    private static final String CARRIER_SIGNAL_DELIMITER = "\\s*,\\s*";
    private static final String COMPONENT_NAME_DELIMITER = "\\s*:\\s*";
    private static final boolean DBG = true;
    private static final int EVENT_REGISTER_DEFAULT_NETWORK_AVAIL = 0;
    private static final String LOG_TAG = CarrierSignalAgent.class.getSimpleName();
    private static final boolean NO_WAKE = false;
    private static final boolean VDBG = Rlog.isLoggable(LOG_TAG, 2);
    private static final boolean WAKE = true;
    private Map<String, Set<ComponentName>> mCachedNoWakeSignalConfigs = new HashMap();
    private Map<String, Set<ComponentName>> mCachedWakeSignalConfigs = new HashMap();
    private final Set<String> mCarrierSignalList = new HashSet(Arrays.asList(new String[]{"com.android.internal.telephony.CARRIER_SIGNAL_PCO_VALUE", "com.android.internal.telephony.CARRIER_SIGNAL_REDIRECTED", "com.android.internal.telephony.CARRIER_SIGNAL_REQUEST_NETWORK_FAILED", "com.android.internal.telephony.CARRIER_SIGNAL_RESET", "com.android.internal.telephony.CARRIER_SIGNAL_DEFAULT_NETWORK_AVAILABLE"}));
    private boolean mDefaultNetworkAvail;
    private final LocalLog mErrorLocalLog = new LocalLog(20);
    private NetworkCallback mNetworkCallback;
    private final Phone mPhone;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            CarrierSignalAgent.this.log("CarrierSignalAgent receiver action: " + action);
            if (action != null && action.equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                CarrierSignalAgent.this.loadCarrierConfig();
            }
        }
    };

    public CarrierSignalAgent(Phone phone) {
        this.mPhone = phone;
        loadCarrierConfig();
        this.mPhone.getContext().registerReceiver(this.mReceiver, new IntentFilter("android.telephony.action.CARRIER_CONFIG_CHANGED"));
        this.mPhone.getCarrierActionAgent().registerForCarrierAction(3, this, 0, null, false);
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 0:
                AsyncResult ar = msg.obj;
                if (ar.exception == null) {
                    ConnectivityManager connectivityMgr = ConnectivityManager.from(this.mPhone.getContext());
                    if (!((Boolean) ar.result).booleanValue()) {
                        if (this.mNetworkCallback != null) {
                            connectivityMgr.unregisterNetworkCallback(this.mNetworkCallback);
                            this.mNetworkCallback = null;
                            this.mDefaultNetworkAvail = false;
                            log("unregister default network");
                            break;
                        }
                    }
                    this.mNetworkCallback = new NetworkCallback() {
                        public void onAvailable(Network network) {
                            if (!CarrierSignalAgent.this.mDefaultNetworkAvail) {
                                CarrierSignalAgent.this.log("Default network available: " + network);
                                Intent intent = new Intent("com.android.internal.telephony.CARRIER_SIGNAL_DEFAULT_NETWORK_AVAILABLE");
                                intent.putExtra("defaultNetworkAvailable", true);
                                CarrierSignalAgent.this.notifyCarrierSignalReceivers(intent);
                                CarrierSignalAgent.this.mDefaultNetworkAvail = true;
                            }
                        }

                        public void onLost(Network network) {
                            CarrierSignalAgent.this.log("Default network lost: " + network);
                            Intent intent = new Intent("com.android.internal.telephony.CARRIER_SIGNAL_DEFAULT_NETWORK_AVAILABLE");
                            intent.putExtra("defaultNetworkAvailable", false);
                            CarrierSignalAgent.this.notifyCarrierSignalReceivers(intent);
                            CarrierSignalAgent.this.mDefaultNetworkAvail = false;
                        }
                    };
                    connectivityMgr.registerDefaultNetworkCallback(this.mNetworkCallback, this.mPhone);
                    log("Register default network");
                    break;
                }
                Rlog.e(LOG_TAG, "Register default network exception: " + ar.exception);
                return;
                break;
        }
    }

    private void loadCarrierConfig() {
        CarrierConfigManager configManager = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
        PersistableBundle b = null;
        if (configManager != null) {
            b = configManager.getConfig();
        }
        if (b != null) {
            Map<String, Set<ComponentName>> config;
            synchronized (this.mCachedWakeSignalConfigs) {
                log("Loading carrier config: carrier_app_wake_signal_config");
                config = parseAndCache(b.getStringArray("carrier_app_wake_signal_config"));
                if (!(this.mCachedWakeSignalConfigs.isEmpty() || (config.equals(this.mCachedWakeSignalConfigs) ^ 1) == 0)) {
                    if (VDBG) {
                        log("carrier config changed, reset receivers from old config");
                    }
                    this.mPhone.getCarrierActionAgent().sendEmptyMessage(2);
                }
                this.mCachedWakeSignalConfigs = config;
            }
            synchronized (this.mCachedNoWakeSignalConfigs) {
                log("Loading carrier config: carrier_app_no_wake_signal_config");
                config = parseAndCache(b.getStringArray("carrier_app_no_wake_signal_config"));
                if (!(this.mCachedNoWakeSignalConfigs.isEmpty() || (config.equals(this.mCachedNoWakeSignalConfigs) ^ 1) == 0)) {
                    if (VDBG) {
                        log("carrier config changed, reset receivers from old config");
                    }
                    this.mPhone.getCarrierActionAgent().sendEmptyMessage(2);
                }
                this.mCachedNoWakeSignalConfigs = config;
            }
        }
    }

    private Map<String, Set<ComponentName>> parseAndCache(String[] configs) {
        Map<String, Set<ComponentName>> newCachedWakeSignalConfigs = new HashMap();
        if (!ArrayUtils.isEmpty(configs)) {
            for (String config : configs) {
                if (!TextUtils.isEmpty(config)) {
                    String[] splitStr = config.trim().split(COMPONENT_NAME_DELIMITER, 2);
                    if (splitStr.length == 2) {
                        ComponentName componentName = ComponentName.unflattenFromString(splitStr[0]);
                        if (componentName == null) {
                            loge("Invalid component name: " + splitStr[0]);
                        } else {
                            for (String s : splitStr[1].split(CARRIER_SIGNAL_DELIMITER)) {
                                if (this.mCarrierSignalList.contains(s)) {
                                    Set<ComponentName> componentList = (Set) newCachedWakeSignalConfigs.get(s);
                                    if (componentList == null) {
                                        componentList = new HashSet();
                                        newCachedWakeSignalConfigs.put(s, componentList);
                                    }
                                    componentList.add(componentName);
                                    if (VDBG) {
                                        logv("Add config {signal: " + s + " componentName: " + componentName + "}");
                                    }
                                } else {
                                    loge("Invalid signal name: " + s);
                                }
                            }
                        }
                    } else {
                        loge("invalid config format: " + config);
                    }
                }
            }
        }
        return newCachedWakeSignalConfigs;
    }

    public boolean hasRegisteredReceivers(String action) {
        if (this.mCachedWakeSignalConfigs.containsKey(action)) {
            return true;
        }
        return this.mCachedNoWakeSignalConfigs.containsKey(action);
    }

    private void broadcast(Intent intent, Set<ComponentName> receivers, boolean wakeup) {
        PackageManager packageManager = this.mPhone.getContext().getPackageManager();
        for (ComponentName name : receivers) {
            Intent signal = new Intent(intent);
            signal.setComponent(name);
            if (wakeup && packageManager.queryBroadcastReceivers(signal, 65536).isEmpty()) {
                loge("Carrier signal receivers are configured but unavailable: " + signal.getComponent());
                return;
            } else if (wakeup || (packageManager.queryBroadcastReceivers(signal, 65536).isEmpty() ^ 1) == 0) {
                signal.putExtra("subscription", this.mPhone.getSubId());
                signal.addFlags(268435456);
                if (!wakeup) {
                    signal.setFlags(16);
                }
                try {
                    this.mPhone.getContext().sendBroadcast(signal);
                    log("Sending signal " + signal.getAction() + (signal.getComponent() != null ? " to the carrier signal receiver: " + signal.getComponent() : ""));
                } catch (ActivityNotFoundException e) {
                    loge("Send broadcast failed: " + e);
                }
            } else {
                loge("Runtime signals shouldn't be configured in Manifest: " + signal.getComponent());
                return;
            }
        }
    }

    public void notifyCarrierSignalReceivers(Intent intent) {
        synchronized (this.mCachedWakeSignalConfigs) {
            Set<ComponentName> receiverSet = (Set) this.mCachedWakeSignalConfigs.get(intent.getAction());
            if (!ArrayUtils.isEmpty(receiverSet)) {
                broadcast(intent, receiverSet, true);
            }
        }
        synchronized (this.mCachedNoWakeSignalConfigs) {
            receiverSet = (Set) this.mCachedNoWakeSignalConfigs.get(intent.getAction());
            if (!ArrayUtils.isEmpty(receiverSet)) {
                broadcast(intent, receiverSet, false);
            }
        }
    }

    private void log(String s) {
        Rlog.d(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + s);
    }

    private void loge(String s) {
        this.mErrorLocalLog.log(s);
        Rlog.e(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + s);
    }

    private void logv(String s) {
        Rlog.v(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + s);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
        pw.println("mCachedWakeSignalConfigs:");
        ipw.increaseIndent();
        for (Entry<String, Set<ComponentName>> entry : this.mCachedWakeSignalConfigs.entrySet()) {
            pw.println("signal: " + ((String) entry.getKey()) + " componentName list: " + entry.getValue());
        }
        ipw.decreaseIndent();
        pw.println("mCachedNoWakeSignalConfigs:");
        ipw.increaseIndent();
        for (Entry<String, Set<ComponentName>> entry2 : this.mCachedNoWakeSignalConfigs.entrySet()) {
            pw.println("signal: " + ((String) entry2.getKey()) + " componentName list: " + entry2.getValue());
        }
        ipw.decreaseIndent();
        pw.println("mDefaultNetworkAvail: " + this.mDefaultNetworkAvail);
        pw.println("error log:");
        ipw.increaseIndent();
        this.mErrorLocalLog.dump(fd, pw, args);
        ipw.decreaseIndent();
    }
}
