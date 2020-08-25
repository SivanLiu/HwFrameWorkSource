package com.android.server.intellicom.networkslice.css;

import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.data.ApnSetting;
import android.text.TextUtils;
import android.util.Log;
import com.android.server.intellicom.common.IntellicomUtils;
import com.android.server.intellicom.common.SmartDualCardConsts;
import com.android.server.intellicom.networkslice.HwNetworkSliceManager;
import com.android.server.intellicom.networkslice.model.OsAppId;
import com.huawei.server.security.securitydiagnose.HwSecDiagnoseConstant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class HwNetworkSliceSettingsObserver {
    private static final int AIRPLANE_MODE_OFF = 0;
    private static final int AIRPLANE_MODE_ON = 1;
    private static final int APN_ENABLED = 1;
    private static final String APN_SUBID_PATH = "filtered/subId/";
    private static final boolean DBG = true;
    private static final int MOBILE_DATA_DISABLED = 0;
    private static final int MOBILE_DATA_ENABLED = 1;
    private static final Uri MSIM_TELEPHONY_CARRIERS_URI = Uri.parse("content://telephony/carriers/subId");
    private static final int QUERY_TOKEN = 0;
    private static final String SEPARATOR_FOR_NORMAL_DATA = ",";
    private static final String SETTINGS_SYSTEM_APPID = "5g_slice_appId";
    private static final String SETTINGS_SYSTEM_DNN = "5g_slice_dnn";
    private static final String SETTINGS_SYSTEM_FQDN = "5g_slice_fqdn";
    private static final String SETTINGS_SYSTEM_SWITCH_DUAL_CARD_SLOT = "switch_dual_card_slots";
    private static final String SETTING_SYSTEM_VPN_ON = "wifipro_network_vpn_state";
    private static final String TAG = "HwNetworkSliceSettingsObserver";
    private static final int VPN_OFF = 0;
    private static final int VPN_ON = 1;
    private ContentObserver mAirplaneModeObserver;
    private List<ApnObject> mApnObjects;
    private ContentObserver mApnObserver;
    private ContentObserver mAppIdObserver;
    private BroadcastReceiver mBroadcastReceiver;
    private Context mContext;
    private ContentObserver mDnnObserver;
    private ContentObserver mFqdnObserver;
    /* access modifiers changed from: private */
    public boolean mIsAirplaneModeOn;
    /* access modifiers changed from: private */
    public boolean mIsMobileDataEnabled;
    /* access modifiers changed from: private */
    public boolean mIsVpnOn;
    /* access modifiers changed from: private */
    public int mMainSlotId;
    private ContentObserver mMobileDataObserver;
    private QueryHandler mQueryHandler;
    private ContentObserver mSwitchDualCardSlotsObserver;
    private ContentObserver mVpnObserver;
    private List<String> mWhiteListForDnn;
    private List<String> mWhiteListForFqdn;
    private List<OsAppId> mWhiteListForOsAppId;

    public void initWhitelist(Context context) {
        if (context == null) {
            log("context is null, fail to init Whitelist");
            return;
        }
        Context context2 = this.mContext;
        if (context2 == null) {
            context2 = context;
        }
        this.mContext = context2;
        ContentResolver cr = this.mContext.getContentResolver();
        readAppIdWhiteList();
        readDnnWhiteList();
        readFqdnWhiteList();
        cr.registerContentObserver(Settings.System.getUriFor(SETTINGS_SYSTEM_APPID), true, this.mAppIdObserver);
        cr.registerContentObserver(Settings.System.getUriFor(SETTINGS_SYSTEM_DNN), true, this.mDnnObserver);
        cr.registerContentObserver(Settings.System.getUriFor(SETTINGS_SYSTEM_FQDN), true, this.mFqdnObserver);
        log("finish whitelist init.");
    }

    public void init(Context context) {
        if (context == null) {
            log("context is null, fail to init HwNetworkSliceSettingsObserver");
            return;
        }
        Context context2 = this.mContext;
        if (context2 == null) {
            context2 = context;
        }
        this.mContext = context2;
        readAirplaneMode();
        readMobileData();
        readMainSlotId();
        readVpnSwitch();
        ContentResolver cr = this.mContext.getContentResolver();
        cr.registerContentObserver(Settings.Global.getUriFor("mobile_data"), true, this.mMobileDataObserver);
        cr.registerContentObserver(Settings.Global.getUriFor("airplane_mode_on"), true, this.mAirplaneModeObserver);
        cr.registerContentObserver(Settings.System.getUriFor(SETTINGS_SYSTEM_SWITCH_DUAL_CARD_SLOT), true, this.mSwitchDualCardSlotsObserver);
        cr.registerContentObserver(Settings.System.getUriFor("wifipro_network_vpn_state"), true, this.mVpnObserver);
        this.mQueryHandler = new QueryHandler(cr);
        registerBroadcast();
    }

    public boolean isNeedToRequestSliceForFqdn(String fqdn) {
        if (!this.mWhiteListForFqdn.contains(fqdn)) {
            return false;
        }
        log("fqdn in whitelist");
        return true;
    }

    public boolean isNeedToRequestSliceForDnn(String dnn) {
        logd("isNeedToRequestSlice ,dnn = " + dnn);
        if (!this.mWhiteListForDnn.contains(dnn)) {
            return false;
        }
        log("dnn in whitelist dnn=" + dnn);
        return true;
    }

    public boolean isNeedToRequestSliceForAppId(String appId) {
        if (!this.mWhiteListForOsAppId.contains(OsAppId.create(HwNetworkSliceManager.OS_ID + appId))) {
            return false;
        }
        log("appid in whitelist");
        return true;
    }

    public static HwNetworkSliceSettingsObserver getInstance() {
        return SingletonInstance.INSTANCE;
    }

    public boolean isAirplaneModeOn() {
        return this.mIsAirplaneModeOn;
    }

    public boolean isMobileDataEnabled() {
        return this.mIsMobileDataEnabled;
    }

    public int getMainSlotId() {
        return this.mMainSlotId;
    }

    public boolean isVpnOn() {
        return this.mIsVpnOn;
    }

    public List<ApnObject> getApnObjects() {
        return this.mApnObjects;
    }

    private static class SingletonInstance {
        /* access modifiers changed from: private */
        public static final HwNetworkSliceSettingsObserver INSTANCE = new HwNetworkSliceSettingsObserver();

        private SingletonInstance() {
        }
    }

    private HwNetworkSliceSettingsObserver() {
        this.mWhiteListForOsAppId = new ArrayList();
        this.mWhiteListForDnn = new ArrayList();
        this.mWhiteListForFqdn = new ArrayList();
        this.mApnObjects = new ArrayList();
        this.mAppIdObserver = new ContentObserver(new Handler()) {
            /* class com.android.server.intellicom.networkslice.css.HwNetworkSliceSettingsObserver.AnonymousClass1 */

            public void onChange(boolean selfChange) {
                HwNetworkSliceSettingsObserver.this.log("mAppIdObserver_onChange");
                HwNetworkSliceSettingsObserver.this.readAppIdWhiteList();
            }
        };
        this.mDnnObserver = new ContentObserver(new Handler()) {
            /* class com.android.server.intellicom.networkslice.css.HwNetworkSliceSettingsObserver.AnonymousClass2 */

            public void onChange(boolean selfChange) {
                HwNetworkSliceSettingsObserver.this.log("mDnnObserver_onChange");
                HwNetworkSliceSettingsObserver.this.readDnnWhiteList();
            }
        };
        this.mMobileDataObserver = new ContentObserver(new Handler()) {
            /* class com.android.server.intellicom.networkslice.css.HwNetworkSliceSettingsObserver.AnonymousClass3 */

            public void onChange(boolean selfChange) {
                HwNetworkSliceSettingsObserver.this.readMobileData();
                if (HwNetworkSliceSettingsObserver.this.mIsMobileDataEnabled) {
                    HwNetworkSliceManager.getInstance().requestMatchAllSlice();
                }
                HwNetworkSliceSettingsObserver hwNetworkSliceSettingsObserver = HwNetworkSliceSettingsObserver.this;
                hwNetworkSliceSettingsObserver.log("mMobileDataObserver_onChange, MOBILE_DATA_ENABLE = " + HwNetworkSliceSettingsObserver.this.mIsMobileDataEnabled);
            }
        };
        this.mSwitchDualCardSlotsObserver = new ContentObserver(new Handler()) {
            /* class com.android.server.intellicom.networkslice.css.HwNetworkSliceSettingsObserver.AnonymousClass4 */

            public void onChange(boolean selfChange) {
                HwNetworkSliceSettingsObserver.this.readMainSlotId();
                HwNetworkSliceSettingsObserver hwNetworkSliceSettingsObserver = HwNetworkSliceSettingsObserver.this;
                hwNetworkSliceSettingsObserver.log("mSwitchDualCardSlotsObserver_onChange, MainSlotId = " + HwNetworkSliceSettingsObserver.this.mMainSlotId);
            }
        };
        this.mAirplaneModeObserver = new ContentObserver(new Handler()) {
            /* class com.android.server.intellicom.networkslice.css.HwNetworkSliceSettingsObserver.AnonymousClass5 */

            public void onChange(boolean selfChange) {
                HwNetworkSliceSettingsObserver.this.readAirplaneMode();
                if (!HwNetworkSliceSettingsObserver.this.mIsAirplaneModeOn) {
                    HwNetworkSliceManager.getInstance().requestMatchAllSlice();
                }
                HwNetworkSliceSettingsObserver hwNetworkSliceSettingsObserver = HwNetworkSliceSettingsObserver.this;
                hwNetworkSliceSettingsObserver.log("mAirplaneModeObserver_onChange, AIRPLANE_MODE_ON = " + HwNetworkSliceSettingsObserver.this.mIsAirplaneModeOn);
            }
        };
        this.mVpnObserver = new ContentObserver(new Handler()) {
            /* class com.android.server.intellicom.networkslice.css.HwNetworkSliceSettingsObserver.AnonymousClass6 */

            public void onChange(boolean selfChange) {
                HwNetworkSliceSettingsObserver.this.readVpnSwitch();
                HwNetworkSliceSettingsObserver hwNetworkSliceSettingsObserver = HwNetworkSliceSettingsObserver.this;
                hwNetworkSliceSettingsObserver.log("mVpnObserver_onChange, VPN_ON = " + HwNetworkSliceSettingsObserver.this.mIsVpnOn);
                if (!HwNetworkSliceSettingsObserver.this.mIsVpnOn) {
                    HwNetworkSliceManager.getInstance().requestMatchAllSlice();
                }
            }
        };
        this.mApnObserver = new ContentObserver(new Handler()) {
            /* class com.android.server.intellicom.networkslice.css.HwNetworkSliceSettingsObserver.AnonymousClass7 */

            public void onChange(boolean selfChange) {
                HwNetworkSliceSettingsObserver.this.queryApnName();
            }
        };
        this.mFqdnObserver = new ContentObserver(new Handler()) {
            /* class com.android.server.intellicom.networkslice.css.HwNetworkSliceSettingsObserver.AnonymousClass8 */

            public void onChange(boolean selfChange) {
                HwNetworkSliceSettingsObserver.this.log("mFqdnObserver_onChange");
                HwNetworkSliceSettingsObserver.this.readFqdnWhiteList();
            }
        };
        this.mBroadcastReceiver = new BroadcastReceiver() {
            /* class com.android.server.intellicom.networkslice.css.HwNetworkSliceSettingsObserver.AnonymousClass9 */

            public void onReceive(Context context, Intent intent) {
                if (intent == null || intent.getAction() == null) {
                    HwNetworkSliceSettingsObserver.this.log("intent or intent.getAction is null.");
                    return;
                }
                String action = intent.getAction();
                char c = 65535;
                if (action.hashCode() == -229777127 && action.equals(SmartDualCardConsts.SYSTEM_STATE_NAME_SIM_STATE_CHANGED)) {
                    c = 0;
                }
                if (c != 0) {
                    HwNetworkSliceSettingsObserver hwNetworkSliceSettingsObserver = HwNetworkSliceSettingsObserver.this;
                    hwNetworkSliceSettingsObserver.log("BroadcastReceiver error: " + action);
                    return;
                }
                int slotId = intent.getIntExtra("phone", -1000);
                String simState = intent.getStringExtra("ss");
                HwNetworkSliceSettingsObserver hwNetworkSliceSettingsObserver2 = HwNetworkSliceSettingsObserver.this;
                hwNetworkSliceSettingsObserver2.log("Receive ACTION_SIM_STATE_CHANGED, slotId=" + slotId + ", simState=" + simState);
                if ("LOADED".equals(simState) && slotId == HwNetworkSliceSettingsObserver.this.mMainSlotId) {
                    HwNetworkSliceSettingsObserver.this.registerApnContentObserver();
                }
            }
        };
    }

    /* access modifiers changed from: private */
    public void registerApnContentObserver() {
        Uri apnUri;
        queryApnName();
        ContentResolver cr = this.mContext.getContentResolver();
        cr.unregisterContentObserver(this.mApnObserver);
        int subId = IntellicomUtils.getSubId(this.mMainSlotId);
        if (subId == -1) {
            log("subId invalid, registerApnContentObserver fail");
            return;
        }
        String subscriptionId = Long.toString((long) subId);
        if (IntellicomUtils.isMultiSimEnabled()) {
            apnUri = Uri.withAppendedPath(MSIM_TELEPHONY_CARRIERS_URI, subscriptionId);
        } else {
            Uri uri = Telephony.Carriers.SIM_APN_URI;
            apnUri = Uri.withAppendedPath(uri, APN_SUBID_PATH + subscriptionId);
        }
        cr.registerContentObserver(apnUri, true, this.mApnObserver);
    }

    /* access modifiers changed from: private */
    public void readApnName(Cursor cursor) {
        if (cursor == null) {
            log("Cursor is null apn query failed.");
            return;
        }
        List<ApnObject> apnObjects = new ArrayList<>();
        while (cursor.moveToNext()) {
            try {
                if (cursor.getInt(cursor.getColumnIndexOrThrow("carrier_enabled")) == 1) {
                    apnObjects.add(new ApnObject(cursor.getString(cursor.getColumnIndexOrThrow("apn")), ApnSetting.getApnTypesBitmaskFromString(cursor.getString(cursor.getColumnIndexOrThrow(HwSecDiagnoseConstant.ANTIMAL_APK_TYPE)))));
                }
            } catch (IllegalArgumentException e) {
                log("readApnNames occurs IllegalArgumentException");
            } catch (Throwable th) {
                cursor.close();
                throw th;
            }
        }
        cursor.close();
        this.mApnObjects = apnObjects;
        log("readApnName mApn=" + this.mApnObjects);
    }

    /* access modifiers changed from: private */
    public void queryApnName() {
        if (this.mQueryHandler == null) {
            log("mQueryHandler is null, can not query anything.");
            return;
        }
        String operator = IntellicomUtils.getOperator(this.mMainSlotId);
        log("queryApnName operator = " + operator + "mMainSlotId = " + this.mMainSlotId);
        if (!TextUtils.isEmpty(operator)) {
            String selection = "numeric = '" + operator + "'";
            String subscriptionId = Long.toString((long) IntellicomUtils.getSubId(this.mMainSlotId));
            if (IntellicomUtils.isMultiSimEnabled()) {
                this.mQueryHandler.startQuery(0, null, Uri.withAppendedPath(MSIM_TELEPHONY_CARRIERS_URI, subscriptionId), null, selection, null, "_id");
                return;
            }
            this.mQueryHandler.startQuery(0, null, Uri.withAppendedPath(Telephony.Carriers.SIM_APN_URI, APN_SUBID_PATH + subscriptionId), null, null, null, "_id");
        }
    }

    /* access modifiers changed from: private */
    public void readAppIdWhiteList() {
        String appIds = Settings.System.getString(this.mContext.getContentResolver(), SETTINGS_SYSTEM_APPID);
        if (appIds != null) {
            this.mWhiteListForOsAppId = parseWhiteListOsAppId(parseWhiteListData(appIds));
        }
    }

    /* access modifiers changed from: private */
    public void readVpnSwitch() {
        boolean z = false;
        if (Settings.System.getInt(this.mContext.getContentResolver(), "wifipro_network_vpn_state", 0) == 1) {
            z = true;
        }
        this.mIsVpnOn = z;
        log("mIsVpnOn = " + this.mIsVpnOn);
    }

    /* access modifiers changed from: private */
    public void readMainSlotId() {
        this.mMainSlotId = Settings.System.getInt(this.mContext.getContentResolver(), SETTINGS_SYSTEM_SWITCH_DUAL_CARD_SLOT, -1);
        log("mMainSlotId = " + this.mMainSlotId);
    }

    /* access modifiers changed from: private */
    public void readAirplaneMode() {
        boolean z = false;
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) == 1) {
            z = true;
        }
        this.mIsAirplaneModeOn = z;
        log("mIsAirplaneModeOn = " + this.mIsAirplaneModeOn);
    }

    /* access modifiers changed from: private */
    public void readMobileData() {
        boolean z = false;
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "mobile_data", 0) == 1) {
            z = true;
        }
        this.mIsMobileDataEnabled = z;
        log("mIsMobileDataEnabled = " + this.mIsMobileDataEnabled);
    }

    /* access modifiers changed from: private */
    public void readDnnWhiteList() {
        String dnns = Settings.System.getString(this.mContext.getContentResolver(), SETTINGS_SYSTEM_DNN);
        if (dnns != null) {
            this.mWhiteListForDnn = parseWhiteListData(dnns);
            log("mWhiteListForDnn = " + this.mWhiteListForDnn);
        }
    }

    /* access modifiers changed from: private */
    public void readFqdnWhiteList() {
        String fqdns = Settings.System.getString(this.mContext.getContentResolver(), SETTINGS_SYSTEM_FQDN);
        if (fqdns != null) {
            this.mWhiteListForFqdn = parseWhiteListData(fqdns);
        }
    }

    private List<String> parseWhiteListData(String whiteListData) {
        List<String> tempList = new ArrayList<>();
        if (whiteListData == null) {
            return tempList;
        }
        Collections.addAll(tempList, whiteListData.split(","));
        return tempList;
    }

    private List<OsAppId> parseWhiteListOsAppId(List<String> osAppIds) {
        List<OsAppId> temp = new ArrayList<>();
        if (osAppIds == null || osAppIds.size() == 0) {
            return temp;
        }
        return (List) osAppIds.stream().map($$Lambda$HwNetworkSliceSettingsObserver$qzv9F90HVXuudhHKNMx6tK08oVo.INSTANCE).filter($$Lambda$HwNetworkSliceSettingsObserver$Lioft6RZaSiJKPaB73WTiJolKKg.INSTANCE).collect(Collectors.toList());
    }

    static /* synthetic */ boolean lambda$parseWhiteListOsAppId$1(OsAppId osAppId) {
        return osAppId != null;
    }

    private void registerBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(SmartDualCardConsts.SYSTEM_STATE_NAME_SIM_STATE_CHANGED);
        this.mContext.registerReceiver(this.mBroadcastReceiver, filter);
    }

    private final class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        /* access modifiers changed from: protected */
        public void onQueryComplete(int token, Object cookie, Cursor cursor) {
            super.onQueryComplete(token, cookie, cursor);
            HwNetworkSliceSettingsObserver.this.readApnName(cursor);
        }
    }

    public static class ApnObject {
        String apnName;
        int apnTypesBitmask;

        ApnObject(String apnName2, int apnTypesBitmask2) {
            this.apnName = apnName2;
            this.apnTypesBitmask = apnTypesBitmask2;
        }

        public String getApnName() {
            return this.apnName;
        }

        public int getApnTypesBitmask() {
            return this.apnTypesBitmask;
        }

        public String toString() {
            return "ApnObject{apnName='" + this.apnName + '\'' + ", apnTypesBitmask=" + this.apnTypesBitmask + '}';
        }
    }

    /* access modifiers changed from: private */
    public void log(String msg) {
        Log.i(TAG, msg);
    }

    private void logd(String msg) {
        Log.d(TAG, msg);
    }
}
