package com.android.server.usb;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.hdm.HwDeviceManager;
import android.media.AudioAttributes;
import android.media.AudioAttributes.Builder;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.MediaStore.Audio.Media;
import android.provider.Settings.Global;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.util.Slog;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.ServiceThread;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import huawei.android.hardware.usb.HwUsbManagerEx;
import java.io.FileDescriptor;

public class HwUsbDeviceManager extends UsbDeviceManager {
    private static final String ACTION_USBLIQUID = "huawei.intent.action.USB_LIQUID";
    private static final String CLASS_NAME_USBLIQUID = "com.huawei.hwdetectrepair.smartnotify.eventlistener.USBLiquidReceiver";
    private static boolean DEBUG = false;
    private static final String FACTORY_VERSION = "factory";
    private static final String MSG_USBLIQUID_TYPE = "MSG_USBLIQUID_TYPE";
    private static final String PACKAGE_NAME_USBLIQUID = "com.huawei.hwdetectrepair";
    private static final String PERSIST_CMCC_USB_LIMIT = "persist.sys.cmcc_usb_limit";
    private static final String RUN_MODE_PROPERTY = "ro.runmode";
    private static final String SYS_CMCC_USB_LIMIT = "cmcc_usb_limit";
    private static final String TAG = HwUsbDeviceManager.class.getSimpleName();
    private static final String USB_STATE_PROPERTY = "sys.usb.state";
    private static final String WATER_WARNING_RINGTONE = "WaterWarning.ogg";
    private AudioAttributes mAudioAttributes;
    private Context mContext;
    private final Handler mHwUsbDeviceManagerHandler;
    private final HandlerThread mHwUsbDeviceManagerThread;
    private boolean mIsShowingDialog = false;
    private Ringtone mRingRingtone;
    private final BroadcastReceiver mSimStatusCompletedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if (HwUsbDeviceManager.DEBUG) {
                Slog.d(HwUsbDeviceManager.TAG, "sim status completed");
            }
            HwUsbDeviceManager.this.sendHandlerEmptyMessage(102);
        }
    };
    private Uri mUri;
    private AlertDialog mWarningDialog = null;

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        DEBUG = z;
    }

    public HwUsbDeviceManager(Context context, UsbAlsaManager alsaManager, UsbSettingsManager settingsManager) {
        super(context, alsaManager, settingsManager);
        setCMCCUsbLimit();
        setUsbConfig();
        registerSimStatusCompletedReceiver();
        this.mContext = context;
        this.mHwUsbDeviceManagerThread = new ServiceThread(TAG, -4, false);
        this.mHwUsbDeviceManagerThread.start();
        this.mHwUsbDeviceManagerHandler = new Handler(this.mHwUsbDeviceManagerThread.getLooper());
        this.mAudioAttributes = new Builder().setUsage(13).setContentType(4).build();
    }

    private String getCmccUsbLimit() {
        return SystemProperties.get(PERSIST_CMCC_USB_LIMIT, "0");
    }

    private String getDebuggleMode() {
        return SystemProperties.get("ro.debuggable", "0");
    }

    private void setCMCCUsbLimit() {
        String ro_debuggable = getDebuggleMode();
        String usb_limit = getCmccUsbLimit();
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setCMCCUsbLimit ro_debuggable ");
            stringBuilder.append(ro_debuggable);
            stringBuilder.append(" usb_limit ");
            stringBuilder.append(usb_limit);
            Slog.i(str, stringBuilder.toString());
        }
        if ("1".equals(ro_debuggable) && "1".equals(usb_limit)) {
            SystemProperties.set(PERSIST_CMCC_USB_LIMIT, "0");
            if (DEBUG) {
                Slog.i(TAG, "UsbDeviceManager new init in debug mode set  to 0 !");
            }
        }
    }

    private void setUsbConfig() {
        String cur_usb_config = SystemProperties.get("persist.sys.usb.config", "adb");
        String usb_limit = getCmccUsbLimit();
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setUsbConfig cur_usb_config ");
            stringBuilder.append(cur_usb_config);
            stringBuilder.append(" usb_limit ");
            stringBuilder.append(usb_limit);
            Slog.i(str, stringBuilder.toString());
        }
        if ("1".equals(usb_limit) && !containsFunctionOuter(cur_usb_config, "manufacture")) {
            boolean result = setUsbConfigEx("mass_storage");
            if (DEBUG) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("UsbDeviceManager new init setusbconfig result: ");
                stringBuilder2.append(result);
                Slog.i(str2, stringBuilder2.toString());
            }
        }
    }

    protected void registerSimStatusCompletedReceiver() {
        if (DEBUG) {
            Slog.d(TAG, "registerSimStatusCompletedReceiver");
        }
        if (getContext() != null) {
            getContext().registerReceiver(this.mSimStatusCompletedReceiver, new IntentFilter("android.intent.action.SIM_STATE_CHANGED"));
        }
    }

    protected void dueSimStatusCompletedMsg() {
        String usb_limit = getCmccUsbLimit();
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("simcardstate at receive sim_status_change usb_limit = ");
            stringBuilder.append(usb_limit);
            Slog.i(str, stringBuilder.toString());
        }
        if (!"0".equals(usb_limit)) {
            int simcardstate = 0;
            try {
                if (getContext() != null) {
                    simcardstate = ((TelephonyManager) getContext().getSystemService("phone")).getSimState();
                }
                if (DEBUG) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("simcardstate at boot completed is ");
                    stringBuilder2.append(simcardstate);
                    Slog.i(str2, stringBuilder2.toString());
                }
                if (!(simcardstate == 0 || simcardstate == 1 || simcardstate == 8 || simcardstate == 6)) {
                    Slog.i(TAG, "persist.sys.cmcc_usb_limit to 0 ");
                    SystemProperties.set(PERSIST_CMCC_USB_LIMIT, "0");
                    setEnabledFunctionsEx("hisuite,mtp,mass_storage", true);
                    if (getContext() != null && getUsbHandlerConnected()) {
                        if (DEBUG) {
                            Slog.i(TAG, "Secure SYS_CMCC_USB_LIMIT 0 ");
                        }
                        Secure.putInt(getContext().getContentResolver(), SYS_CMCC_USB_LIMIT, 0);
                    }
                }
            } catch (Exception e) {
                Slog.i(TAG, "dueSimStatusCompletedMsg error", e);
            }
        }
    }

    protected boolean interceptSetEnabledFunctions(String functions) {
        boolean isManufacturePort = false;
        String usb_limit = getCmccUsbLimit();
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("interceptSetEnabledFunctions  functions:");
            stringBuilder.append(functions);
            Slog.i(str, stringBuilder.toString());
        }
        if (functions != null) {
            isManufacturePort = containsFunctionOuter(functions, "manufacture");
        }
        if (SystemProperties.get(USB_STATE_PROPERTY, "").equals(functions) && isManufacturePort) {
            if (DEBUG) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("The current function: ");
                stringBuilder2.append(functions);
                stringBuilder2.append(" has been set, return!");
                Slog.i(str2, stringBuilder2.toString());
            }
            return true;
        } else if ("0".equals(usb_limit)) {
            return false;
        } else {
            int simcardstate = 0;
            try {
                if (getContext() != null) {
                    simcardstate = ((TelephonyManager) getContext().getSystemService("phone")).getSimState();
                }
                if (DEBUG) {
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("interceptSetEnabledFunctions simcardstate = ");
                    stringBuilder3.append(simcardstate);
                    stringBuilder3.append(" IsManufacturePort:");
                    stringBuilder3.append(isManufacturePort);
                    Slog.i(str3, stringBuilder3.toString());
                }
                if (!(simcardstate == 0 || simcardstate == 1 || simcardstate == 8 || simcardstate == 6)) {
                    if (DEBUG) {
                        Slog.i(TAG, "persist.sys.cmcc_usb_limit set to 0 at setenable  ");
                    }
                    SystemProperties.set(PERSIST_CMCC_USB_LIMIT, "0");
                    usb_limit = "0";
                }
            } catch (Exception e) {
                Slog.i(TAG, "interceptSetEnabledFunctions error", e);
            }
            if (!"1".equals(usb_limit) || isManufacturePort) {
                return false;
            }
            if (DEBUG) {
                Slog.i(TAG, "cmcc usb_limit return !");
            }
            return true;
        }
    }

    protected boolean isCmccUsbLimit() {
        if ("1".equals(SystemProperties.get(PERSIST_CMCC_USB_LIMIT, "0"))) {
            return true;
        }
        return false;
    }

    protected String removeAdbFunction(String functions, String function) {
        if (containsFunctionOuter(functions, "manufacture")) {
            return functions;
        }
        return super.removeAdbFunction(functions, function);
    }

    protected boolean isAdbDisabled() {
        if (!HwDeviceManager.disallowOp(11)) {
            return false;
        }
        Global.putInt(this.mContentResolver, "adb_enabled", 0);
        return true;
    }

    protected void sendUSBLiquidBroadcast(Context context, String msg) {
        Intent intent = new Intent(ACTION_USBLIQUID);
        intent.setClassName(PACKAGE_NAME_USBLIQUID, CLASS_NAME_USBLIQUID);
        intent.putExtra(MSG_USBLIQUID_TYPE, msg);
        context.sendBroadcastAsUser(intent, UserHandle.ALL, "android.permission.MANAGE_USB");
    }

    protected void usbWaterInNotification(boolean enable) {
        Slog.i(TAG, "usbWaterInNotification");
        if (!this.mIsShowingDialog) {
            this.mIsShowingDialog = true;
            if (enable) {
                playRing();
            } else {
                stopRing();
            }
            Message msg = Message.obtain(this.mHwUsbDeviceManagerHandler, new Runnable() {
                public void run() {
                    HwUsbDeviceManager.this.createWarningDialog();
                }
            });
            msg.setAsynchronous(true);
            this.mHwUsbDeviceManagerHandler.sendMessage(msg);
        }
    }

    protected void playRing() {
        this.mHwUsbDeviceManagerHandler.post(new Runnable() {
            public void run() {
                HwUsbDeviceManager.this.mUri = HwUsbDeviceManager.queryRingMusicUri(HwUsbDeviceManager.this.mContext, HwUsbDeviceManager.WATER_WARNING_RINGTONE);
                HwUsbDeviceManager.this.mRingRingtone = HwUsbDeviceManager.this.playRing(HwUsbDeviceManager.this.mUri);
            }
        });
    }

    protected void stopRing() {
        this.mHwUsbDeviceManagerHandler.post(new Runnable() {
            public void run() {
                HwUsbDeviceManager.this.stopRing(HwUsbDeviceManager.this.mRingRingtone);
            }
        });
    }

    private Ringtone playRing(Uri uri) {
        Ringtone ringtone = RingtoneManager.getRingtone(this.mContext, uri);
        if (ringtone != null) {
            ringtone.setAudioAttributes(this.mAudioAttributes);
            ringtone.play();
        }
        return ringtone;
    }

    private void stopRing(Ringtone ringtone) {
        if (ringtone != null) {
            ringtone.stop();
        }
    }

    private static Uri queryRingMusicUri(Context context, String fileName) {
        return queryRingMusicUri(context.getContentResolver(), fileName);
    }

    /* JADX WARNING: Missing block: B:13:0x0047, code:
            if (r9 != null) goto L_0x0049;
     */
    /* JADX WARNING: Missing block: B:14:0x0049, code:
            r9.close();
     */
    /* JADX WARNING: Missing block: B:19:0x0066, code:
            if (r9 == null) goto L_0x0069;
     */
    /* JADX WARNING: Missing block: B:20:0x0069, code:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static Uri queryRingMusicUri(ContentResolver resolver, String fileName) {
        if (fileName == null) {
            return null;
        }
        Uri uri = Media.INTERNAL_CONTENT_URI;
        String[] cols = new String[]{"_id"};
        StringBuilder where = new StringBuilder("_data like '%");
        where.append(fileName);
        where.append("'");
        Cursor cur = null;
        try {
            cur = resolver.query(uri, cols, where.toString(), null, null);
            if (cur != null && cur.moveToFirst()) {
                Uri withAppendedId = ContentUris.withAppendedId(uri, (long) cur.getInt(cur.getColumnIndex("_id")));
                if (cur != null) {
                    cur.close();
                }
                return withAppendedId;
            }
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("queryRingMusicUri query database exception: ");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            if (cur != null) {
                cur.close();
            }
        }
    }

    private void createWarningDialog() {
        this.mWarningDialog = new AlertDialog.Builder(this.mContext, 33947691).setTitle(33685900).setMessage(33685901).setIcon(17301543).setPositiveButton(33685902, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Slog.i(HwUsbDeviceManager.TAG, "dialog click, dismiss and stopRing");
                dialog.dismiss();
                HwUsbDeviceManager.this.stopRing();
                HwUsbDeviceManager.this.mIsShowingDialog = false;
            }
        }).create();
        this.mWarningDialog.getWindow().setType(HwArbitrationDEFS.MSG_MPLINK_UNBIND_SUCCESS);
        this.mWarningDialog.show();
        this.mWarningDialog.setCancelable(false);
        Slog.i(TAG, "createWarningDialog");
    }

    public void setHdbEnabledEx(boolean enable) {
        HwUsbManagerEx.getInstance().setHdbEnabled(enable);
    }

    public void dump(FileDescriptor fd, IndentingPrintWriter writer, String[] args) {
        HwUsbManagerEx.getInstance().dump(fd, args);
    }
}
