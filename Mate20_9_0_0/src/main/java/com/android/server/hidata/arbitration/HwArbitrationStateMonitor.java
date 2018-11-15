package com.android.server.hidata.arbitration;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.os.Handler;
import android.provider.Settings.Global;
import android.provider.Settings.System;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.telephony.PhoneConstants.DataState;
import com.android.server.mtm.iaware.brjob.AwareJobSchedulerConstants;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import huawei.android.net.hwmplink.HwHiDataCommonUtils;

public class HwArbitrationStateMonitor {
    private static final String TAG = "HiData_HwArbitrationStateMonitor";
    private static HwArbitrationStateMonitor mHwArbitrationStateMonitor = null;
    private IntentFilter intentFilter = new IntentFilter();
    AirPlaneModeObserver mAirPlaneModeObserver;
    private BroadcastReceiver mBroadcastReceiver = new StateBroadcastReceiver(this, null);
    private ConnectivityManager mCM;
    private Context mContext;
    private Handler mHandler;
    private NetworkCallback mHwArbitrationNetworkCallback;
    private PhoneStateListener[] mPhoneStateListener;
    private ContentResolver mResolver;
    UserDataEnableObserver mUserDataEnableObserver;

    /* renamed from: com.android.server.hidata.arbitration.HwArbitrationStateMonitor$5 */
    static /* synthetic */ class AnonymousClass5 {
        static final /* synthetic */ int[] $SwitchMap$com$android$internal$telephony$PhoneConstants$DataState = new int[DataState.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$telephony$PhoneConstants$DataState[DataState.DISCONNECTED.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$telephony$PhoneConstants$DataState[DataState.CONNECTED.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    private class AirPlaneModeObserver extends ContentObserver {
        public AirPlaneModeObserver(Handler handler) {
            super(handler);
            HwArbitrationStateMonitor.this.mResolver = HwArbitrationStateMonitor.this.mContext.getContentResolver();
        }

        public void register() {
            HwArbitrationStateMonitor.this.mResolver.registerContentObserver(Global.getUriFor("airplane_mode_on"), false, this);
        }

        public void unregister() {
            HwArbitrationStateMonitor.this.mResolver.unregisterContentObserver(this);
        }

        public void onChange(boolean selfChange) {
            boolean airplaneMode = HwArbitrationStateMonitor.this.isAirplaneModeOn();
            String str = HwArbitrationStateMonitor.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("AirPlaneMode change to ");
            stringBuilder.append(airplaneMode);
            HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
            if (airplaneMode) {
                HwArbitrationStateMonitor.this.mHandler.sendEmptyMessage(HwArbitrationDEFS.MSG_AIRPLANE_MODE_ON);
            } else {
                HwArbitrationStateMonitor.this.mHandler.sendEmptyMessage(HwArbitrationDEFS.MSG_AIRPLANE_MODE_OFF);
            }
        }
    }

    private class StateBroadcastReceiver extends BroadcastReceiver {
        private NetworkInfo mActiveNetworkInfo;
        private DataState[] mApnState;
        private int mConnectivityType;
        private int mCurrentActiveNetwork;
        private boolean mCurrentDataRoamingState;
        private int mCurrentDataTechType;
        private int mCurrentServiceState;
        private int mDefaultDataSubId;
        private boolean[] mFristTimeRecvApnStateFlag;
        private boolean mIsDataTechSuitable;
        private boolean mMobileConnectState;
        private boolean mWifiConnectState;

        private StateBroadcastReceiver() {
            this.mCurrentActiveNetwork = 802;
            this.mCurrentServiceState = 1;
            this.mCurrentDataTechType = 0;
            this.mIsDataTechSuitable = false;
            this.mCurrentDataRoamingState = false;
            this.mDefaultDataSubId = 0;
            this.mMobileConnectState = false;
            this.mWifiConnectState = false;
            this.mConnectivityType = 802;
            this.mFristTimeRecvApnStateFlag = new boolean[]{false, false};
            this.mApnState = new DataState[2];
        }

        /* synthetic */ StateBroadcastReceiver(HwArbitrationStateMonitor x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.net.wifi.WIFI_STATE_CHANGED".equals(action)) {
                int wifistate = intent.getIntExtra("wifi_state", 4);
                if (wifistate == 1) {
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMonitor.TAG, "MSG_WIFI_STATE_DISABLE");
                } else if (wifistate == 3) {
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMonitor.TAG, "MSG_WIFI_STATE_ENABLE");
                }
            } else if ("android.net.wifi.STATE_CHANGE".equals(action)) {
                NetworkInfo netInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                if (netInfo == null) {
                    return;
                }
                if (netInfo.getState() == State.DISCONNECTED) {
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMonitor.TAG, "WifiManager:Wifi disconnected");
                    if (this.mWifiConnectState) {
                        this.mWifiConnectState = false;
                        HwArbitrationStateMonitor.this.mHandler.sendEmptyMessage(HwArbitrationDEFS.MSG_WIFI_DISCONNECTED_FOR_HICURE);
                    }
                } else if (netInfo.getState() == State.CONNECTED) {
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMonitor.TAG, "WifiManager:Wifi connected");
                    if (!this.mWifiConnectState) {
                        this.mWifiConnectState = true;
                    }
                }
            } else if ("android.net.conn.CONNECTIVITY_CHANGE".equals(action)) {
                HwArbitrationCommonUtils.logD(HwArbitrationStateMonitor.TAG, "connectivity Changed");
                onConnectivityNetworkChange();
            } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
                HwArbitrationCommonUtils.logD(HwArbitrationStateMonitor.TAG, "MSG_SCREEN_IS_TURNOFF");
                HwArbitrationFunction.setScreenState(false);
                HwArbitrationStateMonitor.this.mHandler.sendEmptyMessage(1012);
            } else if ("android.intent.action.SCREEN_ON".equals(action)) {
                HwArbitrationCommonUtils.logD(HwArbitrationStateMonitor.TAG, "MSG_SCREEN_IS_ON");
                HwArbitrationFunction.setScreenState(true);
                HwArbitrationStateMonitor.this.mHandler.sendEmptyMessage(1017);
            } else if ("android.intent.action.SERVICE_STATE".equals(action)) {
                handleTelephonyServiceStateChanged(ServiceState.newFromBundle(intent.getExtras()), intent.getIntExtra("subscription", -1));
            } else if (action.equals("android.intent.action.ANY_DATA_STATE")) {
                logD(HwArbitrationStateMonitor.TAG, "onReceive: ACTION_ANY_DATA_CONNECTION_STATE_CHANGED");
                handleTelephonyDataConnectionChanged(intent.getStringExtra("state"), intent.getIntExtra("subscription", -1));
            } else if ("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED".equals(action)) {
                handleDataSubChange(intent.getIntExtra("subscription", -1));
            } else if ("huawei.intent.action.HICURE_RESULT".equals(action)) {
                HwArbitrationCommonUtils.logD(HwArbitrationStateMonitor.TAG, "onReceive:ARBITRATION_NOTIFY_HICURE_RESULT_ACTION");
                HwArbitrationStateMachine.getInstance(HwArbitrationStateMonitor.this.mContext).receiveCureResult(intent.getIntExtra("extra_result", 3), intent.getIntExtra("extra_timer_result", 30), intent.getIntExtra("extra_diagnose_result", -1), intent.getIntExtra("extra_method", -1));
            } else if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
                HwArbitrationCommonUtils.logD(HwArbitrationStateMonitor.TAG, "BOOT_COMPLETED");
                if (HwArbitrationStateMonitor.this.mHwArbitrationNetworkCallback == null) {
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMonitor.TAG, "registerDefaultNetworkCallback have not been registered");
                    HwArbitrationStateMonitor.this.registerNetworkChangeCallback();
                }
                HwArbitrationStateMonitor.this.mHandler.sendEmptyMessage(HwArbitrationDEFS.MSG_DEVICE_BOOT_COMPLETED);
            } else if (HwArbitrationDEFS.ACTION_HiData_DATA_ROVE_IN.equals(action)) {
                HwArbitrationCommonUtils.logD(HwArbitrationStateMonitor.TAG, "Big Mobile Data Alert trigger closing MPLink");
                HwArbitrationStateMonitor.this.mHandler.sendEmptyMessage(HwArbitrationDEFS.MSG_Stop_MPLink_By_Notification);
            }
        }

        private void onConnectivityNetworkChange() {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) HwArbitrationStateMonitor.this.mContext.getSystemService("connectivity");
            if (mConnectivityManager != null) {
                this.mActiveNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
                int i = 801;
                String str;
                StringBuilder stringBuilder;
                if (this.mActiveNetworkInfo == null) {
                    str = HwArbitrationStateMonitor.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("onConnectivityNetworkChange-prev_type is:");
                    stringBuilder2.append(this.mConnectivityType);
                    HwArbitrationCommonUtils.logD(str, stringBuilder2.toString());
                    if (800 == this.mConnectivityType) {
                        HwArbitrationCommonUtils.logD(HwArbitrationStateMonitor.TAG, "onConnectivityNetworkChange, MSG_WIFI_STATE_DISCONNECT");
                        if (802 != this.mCurrentActiveNetwork) {
                            if (!this.mMobileConnectState) {
                                i = 802;
                            }
                            this.mCurrentActiveNetwork = i;
                            HwArbitrationStateMonitor.this.mHandler.sendMessage(HwArbitrationStateMonitor.this.mHandler.obtainMessage(HwArbitrationDEFS.MSG_NOTIFY_CURRENT_NETWORK, this.mCurrentActiveNetwork, 0));
                        }
                        HwArbitrationStateMonitor.this.mHandler.sendEmptyMessage(1006);
                    } else if (801 == this.mConnectivityType) {
                        HwArbitrationCommonUtils.logD(HwArbitrationStateMonitor.TAG, "onConnectivityNetworkChange, MSG_CELL_STATE_DISCONNECT");
                        HwArbitrationStateMonitor.this.mHandler.sendEmptyMessage(1010);
                    }
                    this.mConnectivityType = 802;
                } else if (1 == this.mActiveNetworkInfo.getType()) {
                    if (this.mActiveNetworkInfo.isConnected()) {
                        HwArbitrationCommonUtils.logD(HwArbitrationStateMonitor.TAG, "onConnectivityNetworkChange, MSG_WIFI_STATE_CONNECTED");
                        if (800 != this.mCurrentActiveNetwork) {
                            this.mCurrentActiveNetwork = 800;
                            HwArbitrationStateMonitor.this.mHandler.sendMessage(HwArbitrationStateMonitor.this.mHandler.obtainMessage(HwArbitrationDEFS.MSG_NOTIFY_CURRENT_NETWORK, this.mCurrentActiveNetwork, 0));
                        }
                        HwArbitrationStateMonitor.this.mHandler.sendEmptyMessage(1005);
                        this.mConnectivityType = 800;
                    } else {
                        str = HwArbitrationStateMonitor.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("onConnectivityNetworkChange-Wifi:");
                        stringBuilder.append(this.mActiveNetworkInfo.getState());
                        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                    }
                } else if (this.mActiveNetworkInfo.getType() == 0) {
                    if (this.mActiveNetworkInfo.isConnected()) {
                        HwArbitrationCommonUtils.logD(HwArbitrationStateMonitor.TAG, "onConnectivityNetworkChange, MSG_CELL_STATE_CONNECTED");
                        HwArbitrationStateMonitor.this.mHandler.sendEmptyMessage(1009);
                        this.mConnectivityType = 801;
                    } else {
                        str = HwArbitrationStateMonitor.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("onConnectivityNetworkChange-Cell:");
                        stringBuilder.append(this.mActiveNetworkInfo.getState());
                        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                    }
                }
            }
        }

        public DataState getApnState(int subId) {
            return this.mApnState[subId];
        }

        private void onDataConnected(int slotId) {
            String str = HwArbitrationStateMonitor.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onDataConnected Enter, slotId is ");
            stringBuilder.append(slotId);
            HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
            HwArbitrationStateMonitor.this.mHandler.sendMessage(HwArbitrationStateMonitor.this.mHandler.obtainMessage(0, slotId, 0));
        }

        private void onDataConnectionDisconnected(DataState oldMobileDataState, int slotId) {
            String str;
            StringBuilder stringBuilder;
            if (DataState.CONNECTED.equals(oldMobileDataState) || DataState.SUSPENDED.equals(oldMobileDataState)) {
                str = HwArbitrationStateMonitor.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("onDataConnectionDisconnected: EVENT_DEFAULT_DATA_DISCONNECTED_FAILURE on slotId ");
                stringBuilder.append(slotId);
                logD(str, stringBuilder.toString());
                HwArbitrationStateMonitor.this.mHandler.sendMessage(HwArbitrationStateMonitor.this.mHandler.obtainMessage(2, slotId, 0));
                HwArbitrationCommonUtils.logD(HwArbitrationStateMonitor.TAG, "MSG_DEFAULT_DATA_DISCONNECTED");
                return;
            }
            str = HwArbitrationStateMonitor.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("onDataConnectionDisconnected: EVENT_DEFAULT_DATA_SETUP_FAILURE on slotId ");
            stringBuilder.append(slotId);
            logD(str, stringBuilder.toString());
            HwArbitrationStateMonitor.this.mHandler.sendMessage(HwArbitrationStateMonitor.this.mHandler.obtainMessage(3, slotId, 0));
        }

        private boolean isDefaultApnType(String apnType) {
            return MemoryConstant.MEM_SCENE_DEFAULT.equals(apnType);
        }

        private void onReceiveDataStateChanged(Intent intent) {
            String apnType = intent.getStringExtra("apnType");
            if (isDefaultApnType(apnType)) {
                int slotId = intent.getIntExtra("subscription", -1);
                DataState state = (DataState) Enum.valueOf(DataState.class, intent.getStringExtra("state"));
                String str;
                StringBuilder stringBuilder;
                if (isSlotIdValid(slotId)) {
                    str = HwArbitrationStateMonitor.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("onReceiveStateChanged: slotId = ");
                    stringBuilder.append(slotId);
                    stringBuilder.append(",state = ");
                    stringBuilder.append(state);
                    stringBuilder.append(",apnType = ");
                    stringBuilder.append(apnType);
                    logD(str, stringBuilder.toString());
                    if (!this.mFristTimeRecvApnStateFlag[slotId]) {
                        logD(HwArbitrationStateMonitor.TAG, "mFristTimeRecvApnStateFlag ");
                        this.mApnState[slotId] = state;
                        this.mFristTimeRecvApnStateFlag[slotId] = true;
                    } else if (!state.equals(this.mApnState[slotId])) {
                        str = HwArbitrationStateMonitor.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("apnState changed,oldMobileDataState = ");
                        stringBuilder.append(this.mApnState[slotId]);
                        stringBuilder.append(",state = ");
                        stringBuilder.append(state);
                        logD(str, stringBuilder.toString());
                        switch (AnonymousClass5.$SwitchMap$com$android$internal$telephony$PhoneConstants$DataState[state.ordinal()]) {
                            case 1:
                                onDataConnectionDisconnected(this.mApnState[slotId], slotId);
                                break;
                            case 2:
                                onDataConnected(slotId);
                                break;
                        }
                        this.mApnState[slotId] = state;
                    }
                    return;
                }
                str = HwArbitrationStateMonitor.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("onReceiveStateChanged: param is invalid,slotId = ");
                stringBuilder.append(slotId);
                stringBuilder.append(",state = ");
                stringBuilder.append(state);
                logE(str, stringBuilder.toString());
            }
        }

        public boolean isSlotIdValid(int slotId) {
            return slotId >= 0 && 2 > slotId;
        }

        private void handleTelephonyServiceStateChanged(ServiceState serviceState, int subId) {
            String str = HwArbitrationStateMonitor.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ACTION_SERVICE_STATE_CHANGED subId:");
            stringBuilder.append(subId);
            HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
            if (subId != -1 && subId == this.mDefaultDataSubId && serviceState != null) {
                int newServiceState = serviceState.getState();
                String str2 = HwArbitrationStateMonitor.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("newServiceState:");
                stringBuilder2.append(newServiceState);
                HwArbitrationCommonUtils.logD(str2, stringBuilder2.toString());
                if (this.mCurrentServiceState != newServiceState) {
                    this.mCurrentServiceState = newServiceState;
                    HwArbitrationFunction.setServiceState(this.mCurrentServiceState);
                    if (this.mCurrentServiceState == 0) {
                        HwArbitrationStateMonitor.this.mHandler.sendEmptyMessage(1019);
                    } else {
                        HwArbitrationStateMonitor.this.mHandler.sendEmptyMessage(1020);
                    }
                }
                boolean newRoamingState = serviceState.getDataRoaming();
                String str3 = HwArbitrationStateMonitor.TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("newRoammingState :");
                stringBuilder3.append(newRoamingState);
                HwArbitrationCommonUtils.logD(str3, stringBuilder3.toString());
                if (this.mCurrentDataRoamingState != newRoamingState) {
                    this.mCurrentDataRoamingState = newRoamingState;
                    HwArbitrationFunction.setDataRoamingState(this.mCurrentDataRoamingState);
                    if (this.mCurrentDataRoamingState) {
                        HwArbitrationStateMonitor.this.mHandler.sendEmptyMessage(1015);
                    } else {
                        HwArbitrationStateMonitor.this.mHandler.sendEmptyMessage(1021);
                    }
                }
                int newDataTechType = serviceState.getDataNetworkType();
                String str4 = HwArbitrationStateMonitor.TAG;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("newDataTechType:");
                stringBuilder4.append(newDataTechType);
                HwArbitrationCommonUtils.logD(str4, stringBuilder4.toString());
                if (this.mCurrentDataTechType != newDataTechType) {
                    this.mCurrentDataTechType = newDataTechType;
                    handleDataTechTypeChange(newDataTechType);
                }
            }
        }

        private void handleDataTechTypeChange(int dataTech) {
            String str = HwArbitrationStateMonitor.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("handlerDataTechTypeChange dataTech :");
            stringBuilder.append(dataTech);
            HwArbitrationCommonUtils.logI(str, stringBuilder.toString());
            HwArbitrationFunction.setDataTech(dataTech);
            boolean z = dataTech == 13 || dataTech == 19;
            boolean newDataTechSuitable = z;
            if (this.mIsDataTechSuitable != newDataTechSuitable) {
                this.mIsDataTechSuitable = newDataTechSuitable;
                HwArbitrationFunction.setDataTechSuitable(newDataTechSuitable);
                if (newDataTechSuitable) {
                    HwArbitrationStateMonitor.this.mHandler.sendEmptyMessage(1022);
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMonitor.TAG, "MSG_DATA_TECHTYPE_SUITABLE");
                    return;
                }
                HwArbitrationStateMonitor.this.mHandler.sendEmptyMessage(1023);
                HwArbitrationCommonUtils.logD(HwArbitrationStateMonitor.TAG, "MSG_DATA_TECHTYPE_NOT_SUITABLE");
            }
        }

        private void handleDataSubChange(int subId) {
            String str = HwArbitrationStateMonitor.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DataSub Change, new subId:");
            stringBuilder.append(subId);
            HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
            if (subId != -1 && this.mDefaultDataSubId != subId) {
                this.mDefaultDataSubId = subId;
            }
        }

        private void handleTelephonyDataConnectionChanged(String state, int subId) {
            String str = HwArbitrationStateMonitor.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ACTION_ANY_DATA_CONNECTION_STATE_CHANGED subId:");
            stringBuilder.append(subId);
            stringBuilder.append(",state:");
            stringBuilder.append(state);
            HwArbitrationCommonUtils.logI(str, stringBuilder.toString());
            if (subId != this.mDefaultDataSubId) {
                return;
            }
            if (AwareJobSchedulerConstants.SERVICES_STATUS_CONNECTED.equals(state)) {
                this.mMobileConnectState = true;
                HwArbitrationCommonUtils.logD(HwArbitrationStateMonitor.TAG, "mobile_data_connected");
                HwArbitrationStateMonitor.this.mHandler.sendEmptyMessage(6);
                if (802 == this.mCurrentActiveNetwork) {
                    this.mCurrentActiveNetwork = 801;
                    HwArbitrationStateMonitor.this.mHandler.sendMessage(HwArbitrationStateMonitor.this.mHandler.obtainMessage(HwArbitrationDEFS.MSG_NOTIFY_CURRENT_NETWORK, this.mCurrentActiveNetwork, 0));
                }
            } else if ("DISCONNECTED".equals(state)) {
                this.mMobileConnectState = false;
                HwArbitrationCommonUtils.logD(HwArbitrationStateMonitor.TAG, "mobile_data_disconnected");
                HwArbitrationStateMonitor.this.mHandler.sendEmptyMessage(5);
                if (801 == this.mCurrentActiveNetwork) {
                    this.mCurrentActiveNetwork = 802;
                    HwArbitrationStateMonitor.this.mHandler.sendMessage(HwArbitrationStateMonitor.this.mHandler.obtainMessage(HwArbitrationDEFS.MSG_NOTIFY_CURRENT_NETWORK, this.mCurrentActiveNetwork, 0));
                }
            }
        }

        private void logD(String TAG, String debugInfo) {
            Log.d(TAG, debugInfo);
        }

        private void logE(String TAG, String debugInfo) {
            Log.e(TAG, debugInfo);
        }
    }

    private class UserDataEnableObserver extends ContentObserver {
        public UserDataEnableObserver(Handler handler) {
            super(handler);
            HwArbitrationStateMonitor.this.mResolver = HwArbitrationStateMonitor.this.mContext.getContentResolver();
        }

        public void register() {
            HwArbitrationStateMonitor.this.mResolver.registerContentObserver(Global.getUriFor("mobile_data"), false, this);
        }

        public void unregister() {
            HwArbitrationStateMonitor.this.mResolver.unregisterContentObserver(this);
        }

        public void onChange(boolean selfChange) {
            boolean state = HwArbitrationStateMonitor.this.isUserDataEnabled();
            if (state) {
                HwArbitrationStateMonitor.this.mHandler.sendEmptyMessage(1007);
            } else {
                HwArbitrationStateMonitor.this.mHandler.sendEmptyMessage(1008);
            }
            String str = HwArbitrationStateMonitor.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("User change Data service state = ");
            stringBuilder.append(state);
            HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
        }
    }

    private HwArbitrationStateMonitor(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
        HwHiDataCommonUtils.logD(TAG, "HwArbitrationStateMonitor create success!");
    }

    public static HwArbitrationStateMonitor createHwArbitrationStateMonitor(Context context, Handler handler) {
        if (mHwArbitrationStateMonitor == null) {
            mHwArbitrationStateMonitor = new HwArbitrationStateMonitor(context, handler);
        }
        return mHwArbitrationStateMonitor;
    }

    public void startMonitor() {
        registerBroadcastReceiver();
        registerForVpnSettingsChanges();
        registerForSettingsChanges();
        registDatabaseObserver();
        startPhoneStateListener();
        registerNetworkChangeCallback();
    }

    private void registerBroadcastReceiver() {
        HwArbitrationCommonUtils.logD(TAG, "start Monitoring intent");
        this.intentFilter.addAction("android.net.wifi.WIFI_STATE_CHANGED");
        this.intentFilter.addAction("android.net.wifi.STATE_CHANGE");
        this.intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        this.intentFilter.addAction("android.intent.action.SCREEN_OFF");
        this.intentFilter.addAction("android.intent.action.SCREEN_ON");
        this.intentFilter.addAction("huawei.intent.action.HICURE_RESULT");
        this.intentFilter.addAction("android.intent.action.SERVICE_STATE");
        this.intentFilter.addAction("android.intent.action.BOOT_COMPLETED");
        this.intentFilter.addAction("android.intent.action.ANY_DATA_STATE");
        this.intentFilter.addAction("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED");
        this.intentFilter.addAction(HwArbitrationDEFS.ACTION_HiData_DATA_ROVE_IN);
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.intentFilter);
    }

    private void registerForVpnSettingsChanges() {
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor("wifipro_network_vpn_state"), false, new ContentObserver(null) {
            public void onChange(boolean selfChange) {
                if (HwArbitrationStateMonitor.getSettingsSystemBoolean(HwArbitrationStateMonitor.this.mContext.getContentResolver(), "wifipro_network_vpn_state", false)) {
                    HwArbitrationStateMonitor.this.mHandler.sendEmptyMessage(HwArbitrationDEFS.MSG_VPN_STATE_OPEN);
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMonitor.TAG, "registerForVpnSettingsChanges VPN Open");
                    return;
                }
                HwArbitrationStateMonitor.this.mHandler.sendEmptyMessage(HwArbitrationDEFS.MSG_VPN_STATE_CLOSE);
                HwArbitrationCommonUtils.logD(HwArbitrationStateMonitor.TAG, "registerForVpnSettingsChanges VPN Close");
            }
        });
    }

    private void registerForSettingsChanges() {
        this.mContext.getContentResolver().registerContentObserver(System.getUriFor("smart_network_switching"), false, new ContentObserver(new Handler()) {
            public void onChange(boolean selfChange) {
                boolean isWiFiProEnabled = HwArbitrationStateMonitor.getSettingsSystemBoolean(HwArbitrationStateMonitor.this.mContext.getContentResolver(), "smart_network_switching", false);
                String str = HwArbitrationStateMonitor.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Wifi pro setting has changed, WiFiProEnabled == ");
                stringBuilder.append(isWiFiProEnabled);
                HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                if (isWiFiProEnabled) {
                    HwArbitrationStateMonitor.this.mHandler.sendEmptyMessage(HwArbitrationDEFS.MSG_WIFI_PLUS_ENABLE);
                } else {
                    HwArbitrationStateMonitor.this.mHandler.sendEmptyMessage(HwArbitrationDEFS.MSG_WIFI_PLUS_DISABLE);
                }
            }
        });
    }

    private static boolean getSettingsSystemBoolean(ContentResolver cr, String name, boolean def) {
        return System.getInt(cr, name, def) == 1;
    }

    private void registDatabaseObserver() {
        observeAirplaneMode();
        observeUserDataEnableStatus();
    }

    private void observeAirplaneMode() {
        this.mAirPlaneModeObserver = new AirPlaneModeObserver(this.mHandler);
        this.mAirPlaneModeObserver.register();
    }

    public boolean isAirplaneModeOn() {
        return Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) != 0;
    }

    private void observeUserDataEnableStatus() {
        this.mUserDataEnableObserver = new UserDataEnableObserver(this.mHandler);
        this.mUserDataEnableObserver.register();
    }

    public boolean isUserDataEnabled() {
        return Global.getInt(this.mContext.getContentResolver(), "mobile_data", 0) != 0;
    }

    private void startPhoneStateListener() {
        TelephonyManager telephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        if (telephonyManager == null) {
            HwArbitrationCommonUtils.logE(TAG, "SlotStateListener: mTelephonyManager is null, return!");
            return;
        }
        int numPhones = telephonyManager.getPhoneCount();
        String str;
        StringBuilder stringBuilder;
        if (numPhones != 2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("SlotStateListener numPhones = ");
            stringBuilder.append(numPhones);
            HwArbitrationCommonUtils.logE(str, stringBuilder.toString());
            return;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("SlotStateListener numPhones is ");
        stringBuilder.append(numPhones);
        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
        this.mPhoneStateListener = new PhoneStateListener[numPhones];
        for (int i = 0; i < numPhones; i++) {
            this.mPhoneStateListener[i] = getPhoneStateListener(i);
            telephonyManager.listen(this.mPhoneStateListener[i], 33);
        }
    }

    private PhoneStateListener getPhoneStateListener(int i) {
        return new PhoneStateListener(Integer.valueOf(i)) {
            public void onServiceStateChanged(ServiceState state) {
                String str;
                StringBuilder stringBuilder;
                if (state == null) {
                    HwArbitrationCommonUtils.logE(HwArbitrationStateMonitor.TAG, "SlotStateListener onServiceStateChanged: state is null,return");
                } else if (HwArbitrationCommonUtils.isSlotIdValid(this.mSubId.intValue())) {
                    HwArbitrationStateMonitor.this.mHandler.sendMessage(HwArbitrationStateMonitor.this.mHandler.obtainMessage(4, this.mSubId.intValue(), 0, state));
                    str = HwArbitrationStateMonitor.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("SlotStateListener onServiceStateChanged on mSubId = ");
                    stringBuilder.append(this.mSubId);
                    stringBuilder.append(", NetworkType is: ");
                    stringBuilder.append(state.getDataNetworkType());
                    HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                } else {
                    str = HwArbitrationStateMonitor.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("SlotStateListener onServiceStateChanged: invalid mSubId = ");
                    stringBuilder.append(this.mSubId);
                    HwArbitrationCommonUtils.logE(str, stringBuilder.toString());
                }
            }

            public void onCallStateChanged(int state, String incomingNumber) {
                if (HwArbitrationCommonUtils.isSlotIdValid(this.mSubId.intValue())) {
                    String str;
                    StringBuilder stringBuilder;
                    switch (state) {
                        case 0:
                            str = HwArbitrationStateMonitor.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("CALL_STATE_IDLE:mSubId:");
                            stringBuilder.append(this.mSubId);
                            HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                            HwArbitrationStateMonitor.this.mHandler.sendMessage(HwArbitrationStateMonitor.this.mHandler.obtainMessage(7, this.mSubId.intValue(), 0));
                            break;
                        case 1:
                        case 2:
                            str = HwArbitrationStateMonitor.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("CALL_STATE_OFFHOOK or CALL_STATE_RINGING:mSubId:");
                            stringBuilder.append(this.mSubId);
                            HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                            HwArbitrationStateMonitor.this.mHandler.sendMessage(HwArbitrationStateMonitor.this.mHandler.obtainMessage(8, this.mSubId.intValue(), 0));
                            break;
                    }
                    return;
                }
                String str2 = HwArbitrationStateMonitor.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("SlotStateListener onCallStateChanged: invalid mSubId = ");
                stringBuilder2.append(this.mSubId);
                HwArbitrationCommonUtils.logE(str2, stringBuilder2.toString());
            }
        };
    }

    private void registerNetworkChangeCallback() {
        this.mCM = (ConnectivityManager) this.mContext.getSystemService("connectivity");
        if (this.mCM != null) {
            this.mHwArbitrationNetworkCallback = new NetworkCallback() {
                private int mActiveNetworkType = 802;
                private Network mDefaultNetwork;
                private Network mLastNetwork;
                private NetworkCapabilities mLastNetworkCapabilities;

                public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMonitor.TAG, "ConnectivityManager.NetworkCallback : onCapabilitiesChanged");
                    if (network != null && networkCapabilities != null) {
                        boolean lastValidated = this.mLastNetworkCapabilities != null && this.mLastNetworkCapabilities.hasCapability(16);
                        boolean validated = networkCapabilities.hasCapability(16);
                        String str = HwArbitrationStateMonitor.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("network:");
                        stringBuilder.append(network.toString());
                        stringBuilder.append(", lastValidated:");
                        stringBuilder.append(lastValidated);
                        stringBuilder.append(", validated:");
                        stringBuilder.append(validated);
                        HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                        if (!network.equals(this.mLastNetwork) || !networkCapabilities.equalsTransportTypes(this.mLastNetworkCapabilities) || validated != lastValidated) {
                            this.mLastNetwork = network;
                            this.mLastNetworkCapabilities = networkCapabilities;
                            String str2;
                            StringBuilder stringBuilder2;
                            if (networkCapabilities.hasTransport(1) && validated) {
                                str2 = HwArbitrationStateMonitor.TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("networkType:TRANSPORT_WIFI, network: ");
                                stringBuilder2.append(network.toString());
                                HwArbitrationCommonUtils.logD(str2, stringBuilder2.toString());
                                HwArbitrationCommonUtils.logD(HwArbitrationStateMonitor.TAG, "onNetworkCallback, MSG_WIFI_STATE_CONNECTED");
                                HwArbitrationStateMonitor.this.mHandler.sendEmptyMessage(1005);
                                this.mDefaultNetwork = network;
                                this.mActiveNetworkType = 800;
                            } else if (networkCapabilities.hasTransport(0) && validated) {
                                str2 = HwArbitrationStateMonitor.TAG;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("networkType:TRANSPORT_CELLULAR, network: ");
                                stringBuilder2.append(network.toString());
                                HwArbitrationCommonUtils.logD(str2, stringBuilder2.toString());
                                HwArbitrationCommonUtils.logD(HwArbitrationStateMonitor.TAG, "onNetworkCallback, MSG_CELL_STATE_CONNECTED");
                                HwArbitrationStateMonitor.this.mHandler.sendEmptyMessage(1009);
                                this.mDefaultNetwork = network;
                                this.mActiveNetworkType = 801;
                            }
                        }
                    }
                }

                public void onLost(Network network) {
                    HwArbitrationCommonUtils.logD(HwArbitrationStateMonitor.TAG, "onNetworkCallback:onLost");
                    if (network != null) {
                        if (network.equals(this.mDefaultNetwork)) {
                            String str = HwArbitrationStateMonitor.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("onNetworkCallback-prev_type is:");
                            stringBuilder.append(this.mActiveNetworkType);
                            stringBuilder.append(", network:");
                            stringBuilder.append(network.toString());
                            HwArbitrationCommonUtils.logD(str, stringBuilder.toString());
                            if (801 == this.mActiveNetworkType) {
                                HwArbitrationCommonUtils.logD(HwArbitrationStateMonitor.TAG, "onNetworkCallback:MSG_CELL_STATE_DISCONNECT");
                                HwArbitrationStateMonitor.this.mHandler.sendEmptyMessage(1010);
                            } else if (800 == this.mActiveNetworkType) {
                                HwArbitrationCommonUtils.logD(HwArbitrationStateMonitor.TAG, "onNetworkCallback:MSG_WIFI_STATE_DISCONNECT");
                                HwArbitrationStateMonitor.this.mHandler.sendEmptyMessage(1006);
                            }
                            this.mActiveNetworkType = 802;
                            return;
                        }
                        HwArbitrationCommonUtils.logD(HwArbitrationStateMonitor.TAG, "lost network not equal to defaultNetwork");
                    }
                }
            };
            this.mCM.registerDefaultNetworkCallback(this.mHwArbitrationNetworkCallback, this.mHandler);
        }
    }
}
