package com.android.server.intellicom.common;

import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Registrant;
import android.os.RegistrantList;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import com.huawei.android.os.SystemPropertiesEx;
import com.huawei.netassistant.service.INetAssistantService;
import java.util.HashMap;
import java.util.Map;

public class HwSettingsObserver extends ContentObserver {
    private static final String COLUMN_ICCID = "iccid";
    private static final String COLUMN_NO_LONGER_PROMPT = "no_longer_prompt";
    private static final String COLUMN_UID = "uid";
    private static final Object LOCK = new Object();
    private static final String NET_ASSISTANT_MONTH_LIMIT_BYTE_URI = "content://com.huawei.systemmanager.NetAssistantProvider/settinginfo";
    private static final String SETTING_NAME_AIRPLANE_MODE_ON = "airplane_mode_on";
    private static final String SETTING_NAME_INTELLIGENCE_CARD_SWITCH = "intelligence_card_switch";
    private static final String SETTING_NAME_MOBILE_DATA = "mobile_data";
    private static final String SETTING_NAME_PRIORITY_APP_URI = "content://com.huawei.dsdscardmanager.db.PriorityAppProvider/PriorityAppInfo";
    private static final String SETTING_NAME_SWITCH_DUAL_CARD_SLOTS = "switch_dual_card_slots";
    private static final String SETTING_NAME_WIFIPRO_NETWORK_VPN_STATE = "wifipro_network_vpn_state";
    private static final int SWITCH_OFF = 0;
    private static final int SWITCH_ON = 1;
    private static final String TAG = "HwSettingsObserver";
    private static HwSettingsObserver sHwSettingsObserver = null;
    private Context mContext = null;
    private Handler mHandler = null;
    private INetAssistantService mNetAssistantService = null;
    private Map<Integer, RegistrantList> mSettingDbChangeRegistrants = new HashMap(7);

    private HwSettingsObserver(Handler handler) {
        super(handler);
        this.mHandler = handler;
        this.mSettingDbChangeRegistrants.put(0, new RegistrantList());
        this.mSettingDbChangeRegistrants.put(1, new RegistrantList());
        this.mSettingDbChangeRegistrants.put(2, new RegistrantList());
        this.mSettingDbChangeRegistrants.put(3, new RegistrantList());
        this.mSettingDbChangeRegistrants.put(4, new RegistrantList());
        this.mSettingDbChangeRegistrants.put(5, new RegistrantList());
        this.mSettingDbChangeRegistrants.put(6, new RegistrantList());
    }

    public static HwSettingsObserver getInstance() {
        HwSettingsObserver hwSettingsObserver;
        synchronized (LOCK) {
            if (sHwSettingsObserver == null) {
                sHwSettingsObserver = new HwSettingsObserver(new Handler());
            }
            hwSettingsObserver = sHwSettingsObserver;
        }
        return hwSettingsObserver;
    }

    public void init(Context context) {
        this.mContext = context;
        startListener();
        initNetAssistantService();
    }

    public void registerForSettingDbChange(int event, Handler handler, int what, Object obj) {
        if (event >= 7 || event < 0) {
            Log.e(TAG, "registerForSettingDbChange arg error. event = " + event);
            return;
        }
        this.mSettingDbChangeRegistrants.get(Integer.valueOf(event)).add(new Registrant(handler, what, obj));
    }

    public void unregisterForSettingDbChange(int event, Handler h) {
        if (event >= 7 || event < 0) {
            Log.e(TAG, "unregisterForSettingDbChange arg error. event = " + event);
            return;
        }
        this.mSettingDbChangeRegistrants.get(Integer.valueOf(event)).remove(h);
    }

    public boolean isAirPlaneModeSwitchOn() {
        return isSwitchOn(SETTING_NAME_AIRPLANE_MODE_ON);
    }

    public boolean isMobileDataSwitchOn() {
        return isSwitchOn(SETTING_NAME_MOBILE_DATA);
    }

    public boolean isIntelligenceCardSwitchOn() {
        return isSwitchOn(SETTING_NAME_INTELLIGENCE_CARD_SWITCH);
    }

    public boolean isVpnModeSwitchOn() {
        return isSwitchOn("wifipro_network_vpn_state");
    }

    /* JADX WARNING: Code restructure failed: missing block: B:20:0x004c, code lost:
        r6 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x004d, code lost:
        if (r3 != null) goto L_0x004f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x004f, code lost:
        $closeResource(r4, r3);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:23:0x0052, code lost:
        throw r6;
     */
    public boolean isInFreeTrafficAppList(String iccid, int uid) {
        Context context = this.mContext;
        if (context == null || iccid == null) {
            Log.e(TAG, "isInFreeTrafficAppList, iccid or mContext is null");
            return false;
        }
        try {
            Cursor cursor = context.getContentResolver().query(Uri.parse(SETTING_NAME_PRIORITY_APP_URI), new String[]{"uid"}, "iccid = ? and uid = ?", new String[]{iccid, String.valueOf(uid)}, null);
            if (getLengthOfCursorList(cursor) == 0) {
                Log.i(TAG, "initListFromDb , result is empty");
                if (cursor != null) {
                    $closeResource(null, cursor);
                }
                return false;
            }
            if (cursor != null) {
                $closeResource(null, cursor);
            }
            return true;
        } catch (SQLiteException e) {
            Log.e(TAG, "isInFreeTrafficAppList, SQLiteException ");
            return false;
        } catch (IllegalArgumentException e2) {
            Log.e(TAG, "isInFreeTrafficAppList, IllegalArgumentException ");
            return false;
        }
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
            } catch (Throwable th) {
                x0.addSuppressed(th);
            }
        } else {
            x1.close();
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:16:0x0036, code lost:
        r6 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:17:0x0037, code lost:
        if (r3 != null) goto L_0x0039;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0039, code lost:
        $closeResource(r4, r3);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x003c, code lost:
        throw r6;
     */
    public int sizeOfFreeTrafficAppList(String iccid) {
        Context context;
        if (iccid == null || (context = this.mContext) == null) {
            Log.e(TAG, "sizeOfFreeTrafficAppList: imsi or mContext is null");
            return 0;
        }
        try {
            Cursor cursor = context.getContentResolver().query(Uri.parse(SETTING_NAME_PRIORITY_APP_URI), new String[]{"uid"}, "iccid = ?", new String[]{iccid}, null);
            int lengthOfCursorList = getLengthOfCursorList(cursor);
            if (cursor != null) {
                $closeResource(null, cursor);
            }
            return lengthOfCursorList;
        } catch (SQLiteException e) {
            Log.e(TAG, "sizeOfFreeTrafficAppList, SQLiteException ");
            return 0;
        } catch (IllegalArgumentException e2) {
            Log.e(TAG, "sizeOfFreeTrafficAppList, IllegalArgumentException ");
            return 0;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:25:0x005f, code lost:
        r6 = move-exception;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:0x0060, code lost:
        if (r3 != null) goto L_0x0062;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x0062, code lost:
        $closeResource(r4, r3);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x0065, code lost:
        throw r6;
     */
    public boolean isNeedNotifyPriorityApp(String iccid, int uid) {
        Context context = this.mContext;
        if (context == null || iccid == null) {
            Log.e(TAG, "isInFreeTrafficAppList, iccid or mContext is null");
            return false;
        }
        boolean z = true;
        try {
            Cursor cursor = context.getContentResolver().query(Uri.parse(SETTING_NAME_PRIORITY_APP_URI), new String[]{COLUMN_NO_LONGER_PROMPT}, "iccid = ? and uid = ?", new String[]{iccid, String.valueOf(uid)}, null);
            if (getLengthOfCursorList(cursor) == 0) {
                Log.i(TAG, "initListFromDb , result is empty");
                if (cursor != null) {
                    $closeResource(null, cursor);
                }
                return false;
            } else if (cursor.moveToFirst()) {
                if (cursor.getInt(cursor.getColumnIndex(COLUMN_NO_LONGER_PROMPT)) > 0) {
                    z = false;
                }
                $closeResource(null, cursor);
                return z;
            } else {
                $closeResource(null, cursor);
                return false;
            }
        } catch (SQLiteException e) {
            Log.e(TAG, "isNeedNotifyPriorityApp, SQLiteException ");
        } catch (IllegalArgumentException e2) {
            Log.e(TAG, "isNeedNotifyPriorityApp, llegalArgumentException ");
        }
    }

    public void setNotNotifyFlagForOnePriorityApp(String iccid, int uid) {
        if (iccid == null || this.mContext == null) {
            Log.e(TAG, "setNotNotifyFlagForPriorityApp: imsi or mContext is null");
            return;
        }
        ContentValues values = new ContentValues();
        values.put(COLUMN_NO_LONGER_PROMPT, (Boolean) true);
        try {
            this.mContext.getContentResolver().update(Uri.parse(SETTING_NAME_PRIORITY_APP_URI), values, "iccid = ? and uid = ?", new String[]{iccid, String.valueOf(uid)});
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "setNotNotifyFlagForOnePriorityApp, IllegalArgumentException");
        }
    }

    public void onChange(boolean isSelfChange, Uri uri) {
        super.onChange(isSelfChange, uri);
        if (uri == null || this.mContext == null) {
            Log.e(TAG, "onChange invalid params");
            return;
        }
        log("onChange uri:" + uri.toString());
        int event = -1;
        if (uri.equals(Settings.Global.getUriFor(SETTING_NAME_AIRPLANE_MODE_ON))) {
            event = 0;
        } else if (uri.equals(Settings.Global.getUriFor(SETTING_NAME_MOBILE_DATA))) {
            event = 1;
        } else if (uri.equals(Settings.Global.getUriFor(SETTING_NAME_INTELLIGENCE_CARD_SWITCH))) {
            event = 2;
        } else if (uri.equals(Settings.System.getUriFor("wifipro_network_vpn_state"))) {
            event = 3;
        } else if (uri.equals(Settings.System.getUriFor(SETTING_NAME_SWITCH_DUAL_CARD_SLOTS))) {
            event = 4;
        } else if (uri.equals(Uri.parse(SETTING_NAME_PRIORITY_APP_URI))) {
            event = 5;
        } else if (uri.equals(Uri.parse(NET_ASSISTANT_MONTH_LIMIT_BYTE_URI))) {
            event = 6;
        } else {
            log("onChange no equal uri" + uri);
        }
        if (event != -1) {
            log("onChange:" + event);
            this.mSettingDbChangeRegistrants.get(Integer.valueOf(event)).notifyRegistrants();
        }
    }

    private void startListener() {
        Context context = this.mContext;
        if (context == null) {
            Log.e(TAG, "startListener, mContext is null");
            return;
        }
        context.getContentResolver().registerContentObserver(Settings.Global.getUriFor(SETTING_NAME_AIRPLANE_MODE_ON), true, this);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(SETTING_NAME_MOBILE_DATA), true, this);
        this.mContext.getContentResolver().registerContentObserver(Settings.Global.getUriFor(SETTING_NAME_INTELLIGENCE_CARD_SWITCH), true, this);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor("wifipro_network_vpn_state"), false, this);
        this.mContext.getContentResolver().registerContentObserver(Settings.System.getUriFor(SETTING_NAME_SWITCH_DUAL_CARD_SLOTS), false, this);
        if (!"factory".equals(SystemPropertiesEx.get("ro.runmode", "normal"))) {
            this.mContext.getContentResolver().registerContentObserver(Uri.parse(SETTING_NAME_PRIORITY_APP_URI), false, this);
            this.mContext.getContentResolver().registerContentObserver(Uri.parse(NET_ASSISTANT_MONTH_LIMIT_BYTE_URI), false, this);
        }
    }

    private boolean isSwitchOn(String uri) {
        Context context = this.mContext;
        if (context == null || Settings.Global.getInt(context.getContentResolver(), uri, 0) != 1) {
            return false;
        }
        return true;
    }

    private int getLengthOfCursorList(Cursor cursor) {
        if (cursor == null) {
            return 0;
        }
        int listLength = cursor.getCount();
        if (listLength >= 0) {
            return listLength;
        }
        cursor.close();
        return 0;
    }

    private void initNetAssistantService() {
        IBinder binder;
        if (this.mNetAssistantService == null && (binder = ServiceManager.getService("com.huawei.netassistant.service.netassistantservice")) != null) {
            this.mNetAssistantService = INetAssistantService.Stub.asInterface(binder);
        }
    }

    public long getMonthLimitTraffic(String imsi) {
        if (TextUtils.isEmpty(imsi)) {
            return -100;
        }
        initNetAssistantService();
        INetAssistantService iNetAssistantService = this.mNetAssistantService;
        if (iNetAssistantService == null) {
            return -100;
        }
        try {
            return iNetAssistantService.getMonthlyTotalBytes(imsi);
        } catch (RemoteException e) {
            Log.e(TAG, "getMonthLimitTraffic, RemoteException");
            this.mNetAssistantService = null;
            return -100;
        }
    }

    public long getMonthTrafficConsumption(String imsi) {
        if (TextUtils.isEmpty(imsi)) {
            return -1;
        }
        initNetAssistantService();
        INetAssistantService iNetAssistantService = this.mNetAssistantService;
        if (iNetAssistantService == null) {
            return -1;
        }
        try {
            return iNetAssistantService.getMonthMobileTotalBytes(imsi);
        } catch (RemoteException e) {
            Log.e(TAG, "getMonthTrafficConsumption, RemoteException");
            this.mNetAssistantService = null;
            return -1;
        }
    }

    private void log(String msg) {
        Log.i(TAG, msg);
    }
}
