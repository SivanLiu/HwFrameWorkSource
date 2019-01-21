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
            CarrierSignalAgent carrierSignalAgent = CarrierSignalAgent.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("CarrierSignalAgent receiver action: ");
            stringBuilder.append(action);
            carrierSignalAgent.log(stringBuilder.toString());
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
        if (msg.what == 0) {
            AsyncResult ar = msg.obj;
            if (ar.exception != null) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Register default network exception: ");
                stringBuilder.append(ar.exception);
                Rlog.e(str, stringBuilder.toString());
                return;
            }
            ConnectivityManager connectivityMgr = ConnectivityManager.from(this.mPhone.getContext());
            if (((Boolean) ar.result).booleanValue()) {
                this.mNetworkCallback = new NetworkCallback() {
                    public void onAvailable(Network network) {
                        if (!CarrierSignalAgent.this.mDefaultNetworkAvail) {
                            CarrierSignalAgent carrierSignalAgent = CarrierSignalAgent.this;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Default network available: ");
                            stringBuilder.append(network);
                            carrierSignalAgent.log(stringBuilder.toString());
                            Intent intent = new Intent("com.android.internal.telephony.CARRIER_SIGNAL_DEFAULT_NETWORK_AVAILABLE");
                            intent.putExtra("defaultNetworkAvailable", true);
                            CarrierSignalAgent.this.notifyCarrierSignalReceivers(intent);
                            CarrierSignalAgent.this.mDefaultNetworkAvail = true;
                        }
                    }

                    public void onLost(Network network) {
                        CarrierSignalAgent carrierSignalAgent = CarrierSignalAgent.this;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Default network lost: ");
                        stringBuilder.append(network);
                        carrierSignalAgent.log(stringBuilder.toString());
                        Intent intent = new Intent("com.android.internal.telephony.CARRIER_SIGNAL_DEFAULT_NETWORK_AVAILABLE");
                        intent.putExtra("defaultNetworkAvailable", false);
                        CarrierSignalAgent.this.notifyCarrierSignalReceivers(intent);
                        CarrierSignalAgent.this.mDefaultNetworkAvail = false;
                    }
                };
                connectivityMgr.registerDefaultNetworkCallback(this.mNetworkCallback, this.mPhone);
                log("Register default network");
            } else if (this.mNetworkCallback != null) {
                connectivityMgr.unregisterNetworkCallback(this.mNetworkCallback);
                this.mNetworkCallback = null;
                this.mDefaultNetworkAvail = false;
                log("unregister default network");
            }
        }
    }

    private void loadCarrierConfig() {
        CarrierConfigManager configManager = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
        PersistableBundle b = null;
        if (configManager != null) {
            b = configManager.getConfig();
        }
        if (b != null) {
            synchronized (this.mCachedWakeSignalConfigs) {
                log("Loading carrier config: carrier_app_wake_signal_config");
                Map<String, Set<ComponentName>> config = parseAndCache(b.getStringArray("carrier_app_wake_signal_config"));
                if (!(this.mCachedWakeSignalConfigs.isEmpty() || config.equals(this.mCachedWakeSignalConfigs))) {
                    if (VDBG) {
                        log("carrier config changed, reset receivers from old config");
                    }
                    this.mPhone.getCarrierActionAgent().sendEmptyMessage(2);
                }
                this.mCachedWakeSignalConfigs = config;
            }
            synchronized (this.mCachedNoWakeSignalConfigs) {
                log("Loading carrier config: carrier_app_no_wake_signal_config");
                Map<String, Set<ComponentName>> config2 = parseAndCache(b.getStringArray("carrier_app_no_wake_signal_config"));
                if (!(this.mCachedNoWakeSignalConfigs.isEmpty() || config2.equals(this.mCachedNoWakeSignalConfigs))) {
                    if (VDBG) {
                        log("carrier config changed, reset receivers from old config");
                    }
                    this.mPhone.getCarrierActionAgent().sendEmptyMessage(2);
                }
                this.mCachedNoWakeSignalConfigs = config2;
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
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Invalid component name: ");
                            stringBuilder.append(splitStr[0]);
                            loge(stringBuilder.toString());
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
                                        StringBuilder stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("Add config {signal: ");
                                        stringBuilder2.append(s);
                                        stringBuilder2.append(" componentName: ");
                                        stringBuilder2.append(componentName);
                                        stringBuilder2.append("}");
                                        logv(stringBuilder2.toString());
                                    }
                                } else {
                                    StringBuilder stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("Invalid signal name: ");
                                    stringBuilder3.append(s);
                                    loge(stringBuilder3.toString());
                                }
                            }
                        }
                    } else {
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("invalid config format: ");
                        stringBuilder4.append(config);
                        loge(stringBuilder4.toString());
                    }
                }
            }
        }
        return newCachedWakeSignalConfigs;
    }

    public boolean hasRegisteredReceivers(String action) {
        return this.mCachedWakeSignalConfigs.containsKey(action) || this.mCachedNoWakeSignalConfigs.containsKey(action);
    }

    private void broadcast(Intent intent, Set<ComponentName> receivers, boolean wakeup) {
        PackageManager packageManager = this.mPhone.getContext().getPackageManager();
        for (ComponentName name : receivers) {
            Intent signal = new Intent(intent);
            signal.setComponent(name);
            StringBuilder stringBuilder;
            if (wakeup && packageManager.queryBroadcastReceivers(signal, 65536).isEmpty()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Carrier signal receivers are configured but unavailable: ");
                stringBuilder.append(signal.getComponent());
                loge(stringBuilder.toString());
                return;
            } else if (wakeup || packageManager.queryBroadcastReceivers(signal, 65536).isEmpty()) {
                signal.putExtra("subscription", this.mPhone.getSubId());
                signal.addFlags(268435456);
                if (!wakeup) {
                    signal.setFlags(16);
                }
                StringBuilder stringBuilder2;
                try {
                    String stringBuilder3;
                    this.mPhone.getContext().sendBroadcast(signal);
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("Sending signal ");
                    stringBuilder4.append(signal.getAction());
                    if (signal.getComponent() != null) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(" to the carrier signal receiver: ");
                        stringBuilder2.append(signal.getComponent());
                        stringBuilder3 = stringBuilder2.toString();
                    } else {
                        stringBuilder3 = "";
                    }
                    stringBuilder4.append(stringBuilder3);
                    log(stringBuilder4.toString());
                } catch (ActivityNotFoundException e) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Send broadcast failed: ");
                    stringBuilder2.append(e);
                    loge(stringBuilder2.toString());
                }
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Runtime signals shouldn't be configured in Manifest: ");
                stringBuilder.append(signal.getComponent());
                loge(stringBuilder.toString());
                return;
            }
        }
    }

    public void notifyCarrierSignalReceivers(Intent intent) {
        Set<ComponentName> receiverSet;
        synchronized (this.mCachedWakeSignalConfigs) {
            receiverSet = (Set) this.mCachedWakeSignalConfigs.get(intent.getAction());
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
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(this.mPhone.getPhoneId());
        stringBuilder.append("]");
        stringBuilder.append(s);
        Rlog.d(str, stringBuilder.toString());
    }

    private void loge(String s) {
        this.mErrorLocalLog.log(s);
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(this.mPhone.getPhoneId());
        stringBuilder.append("]");
        stringBuilder.append(s);
        Rlog.e(str, stringBuilder.toString());
    }

    private void logv(String s) {
        String str = LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        stringBuilder.append(this.mPhone.getPhoneId());
        stringBuilder.append("]");
        stringBuilder.append(s);
        Rlog.v(str, stringBuilder.toString());
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        StringBuilder stringBuilder;
        IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
        pw.println("mCachedWakeSignalConfigs:");
        ipw.increaseIndent();
        for (Entry<String, Set<ComponentName>> entry : this.mCachedWakeSignalConfigs.entrySet()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("signal: ");
            stringBuilder.append((String) entry.getKey());
            stringBuilder.append(" componentName list: ");
            stringBuilder.append(entry.getValue());
            pw.println(stringBuilder.toString());
        }
        ipw.decreaseIndent();
        pw.println("mCachedNoWakeSignalConfigs:");
        ipw.increaseIndent();
        for (Entry<String, Set<ComponentName>> entry2 : this.mCachedNoWakeSignalConfigs.entrySet()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("signal: ");
            stringBuilder.append((String) entry2.getKey());
            stringBuilder.append(" componentName list: ");
            stringBuilder.append(entry2.getValue());
            pw.println(stringBuilder.toString());
        }
        ipw.decreaseIndent();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("mDefaultNetworkAvail: ");
        stringBuilder2.append(this.mDefaultNetworkAvail);
        pw.println(stringBuilder2.toString());
        pw.println("error log:");
        ipw.increaseIndent();
        this.mErrorLocalLog.dump(fd, pw, args);
        ipw.decreaseIndent();
    }
}
