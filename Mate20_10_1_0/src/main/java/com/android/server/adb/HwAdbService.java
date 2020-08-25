package com.android.server.adb;

import android.content.Context;
import android.database.ContentObserver;
import android.debug.HdbManagerInternal;
import android.debug.IHdbTransport;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Flog;
import android.util.Slog;
import com.android.server.HwBluetoothManagerServiceEx;
import com.android.server.LocalServices;
import com.android.server.wifipro.WifiProCommonUtils;
import huawei.android.hardware.usb.HwUsbManagerEx;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class HwAdbService extends AdbService {
    private static final String ALLOW_CHARGING_ADB = "allow_charging_adb";
    private static final String KEY_CONTENT_SUITESTATE = "suitestate";
    private static final String SUITE_STATE_FILE = "android_usb/f_mass_storage/suitestate";
    private static final String SUITE_STATE_PATH = "/sys/class";
    private static final String TAG = "HwAdbService";
    private boolean mChargingOnlySelected = true;
    /* access modifiers changed from: private */
    public boolean mHdbEnabled;
    /* access modifiers changed from: private */
    public final ArrayMap<IBinder, IHdbTransport> mHdbTransports = new ArrayMap<>();

    public HwAdbService(Context context) {
        super(context);
    }

    /* access modifiers changed from: protected */
    public void onInitHandle() {
        try {
            if (SystemProperties.getInt("ro.debuggable", 0) == 1) {
                Flog.i(1306, "HwAdbServicedevice is root, enable adb");
                this.mAdbEnabled = true;
                Settings.Global.putInt(this.mContentResolver, ALLOW_CHARGING_ADB, 1);
            } else if (SystemProperties.get("ro.product.custom", HwBluetoothManagerServiceEx.DEFAULT_PACKAGE_NAME).contains("docomo")) {
                Settings.Global.putInt(this.mContentResolver, ALLOW_CHARGING_ADB, 1);
            }
        } catch (Exception e) {
            Slog.e(TAG, "Error initializing onInitHandle", e);
        }
    }

    public void systemReady() {
        HwAdbService.super.systemReady();
        if (SystemProperties.get("persist.service.hdb.enable", "false").equals("true")) {
            LocalServices.addService(HdbManagerInternal.class, new HdbManagerInternalImpl());
            this.mHdbEnabled = Settings.System.getInt(this.mContentResolver, "hdb_enabled", 0) > 0;
            Slog.i(TAG, "device support hdb feature, mHdbEnabled:" + this.mHdbEnabled);
            this.mContentResolver.registerContentObserver(Settings.System.getUriFor("hdb_enabled"), false, new HdbSettingsObserver(this.mHandler));
            if (Settings.System.getInt(this.mContentResolver, "hdb_enabled", -1) < 0) {
                if (this.mHandler.containsFunction(SystemProperties.get("ro.default.userportmode", "null"), "hdb")) {
                    try {
                        Flog.i(1306, "HwAdbService ro.default.userportmode:" + SystemProperties.get("ro.default.userportmode", "null"));
                        Settings.System.putInt(this.mContentResolver, "hdb_enabled", 1);
                    } catch (Exception e) {
                        Flog.e(1306, "HwAdbService systemReady ro.default.userportmode set KEY_CONTENT_HDB_ALLOWED failed");
                    }
                } else if (SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, "null").equals("CN")) {
                    try {
                        Flog.i(1306, "HwAdbService ro.product.locale.region:" + SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, "null"));
                        Settings.System.putInt(this.mContentResolver, "hdb_enabled", 1);
                    } catch (Exception e2) {
                        Flog.w(1306, "HwAdbService systemReady ro.product.locale.region set KEY_CONTENT_HDB_ALLOWED failed");
                    }
                } else {
                    try {
                        Flog.i(1306, "HwAdbService System.KEY_CONTENT_HDB_ALLOWED : 0");
                        Settings.System.putInt(this.mContentResolver, "hdb_enabled", 0);
                    } catch (Exception e3) {
                        Flog.w(1306, "HwAdbService systemReady set KEY_CONTENT_HDB_ALLOWED failed");
                    }
                }
            }
        } else {
            Slog.i(TAG, "device not support hdb feature");
        }
        this.mContentResolver.registerContentObserver(Settings.Secure.getUriFor(KEY_CONTENT_SUITESTATE), false, new SuitestateObserver(this.mHandler));
    }

    public void bootCompleted() {
        HwAdbService.super.bootCompleted();
        HwUsbManagerEx.getInstance().setHdbEnabled(this.mHdbEnabled);
    }

    /* access modifiers changed from: protected */
    public void handleUserSwtiched(int newUserId) {
        if (newUserId == 127) {
            this.mContentResolver.registerContentObserver(Settings.System.getUriFor("hdb_enabled"), false, new HdbSettingsObserver(this.mHandler), 127);
            if (SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, "null").equals("CN")) {
                try {
                    Settings.System.putIntForUser(this.mContentResolver, "hdb_enabled", 1, 127);
                } catch (Exception e) {
                    Flog.w(1306, "HwAdbService handleUserSwtiched set KEY_CONTENT_HDB_ALLOWED failed");
                }
            }
        }
    }

    private class HdbSettingsObserver extends ContentObserver {
        public HdbSettingsObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange, Uri uri, int userId) {
            boolean enable = false;
            if (Settings.System.getIntForUser(HwAdbService.this.mContentResolver, "hdb_enabled", 0, userId) > 0) {
                enable = true;
            }
            Flog.i(1306, "HwAdbService Hdb Settings enable:" + enable);
            HwAdbService.this.mHandler.sendMessage(101, enable);
        }
    }

    /* access modifiers changed from: protected */
    public void setHdbEnabled(boolean enable) {
        if (DEBUG) {
            Slog.d(TAG, "setHdbEnabled(" + enable + "), mHdbEnabled=" + this.mHdbEnabled);
        }
        if (enable != this.mHdbEnabled) {
            this.mHdbEnabled = enable;
            for (IHdbTransport transport : this.mHdbTransports.values()) {
                try {
                    transport.onHdbEnabled(enable);
                } catch (RemoteException e) {
                    Slog.w(TAG, "Unable to send onHdbEnabled to transport " + transport.toString());
                }
            }
            HwUsbManagerEx.getInstance().setHdbEnabled(enable);
        }
    }

    private class SuitestateObserver extends ContentObserver {
        public SuitestateObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange) {
            HwAdbService.writeSuitestate();
        }
    }

    public static void writeSuitestate() {
        OutputStreamWriter osw = null;
        FileOutputStream fos = null;
        try {
            File newfile = new File(SUITE_STATE_PATH, SUITE_STATE_FILE);
            if (newfile.exists()) {
                fos = new FileOutputStream(newfile);
                osw = new OutputStreamWriter(fos, "UTF-8");
                osw.write("0");
                osw.flush();
            }
            if (osw != null) {
                try {
                    osw.close();
                } catch (IOException e) {
                    Slog.e(TAG, "IOException in close fw");
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e2) {
                    Slog.e(TAG, "IOException in close fos");
                }
            }
        } catch (IOException ex) {
            Slog.e(TAG, "IOException in writeCommand hisuite", ex);
            if (0 != 0) {
                try {
                    osw.close();
                } catch (IOException e3) {
                    Slog.e(TAG, "IOException in close fw");
                }
            }
            if (0 != 0) {
                fos.close();
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    osw.close();
                } catch (IOException e4) {
                    Slog.e(TAG, "IOException in close fw");
                }
            }
            if (0 != 0) {
                try {
                    fos.close();
                } catch (IOException e5) {
                    Slog.e(TAG, "IOException in close fos");
                }
            }
            throw th;
        }
    }

    private class HdbManagerInternalImpl extends HdbManagerInternal {
        private HdbManagerInternalImpl() {
        }

        public void registerTransport(IHdbTransport transport) {
            HwAdbService.this.mHdbTransports.put(transport.asBinder(), transport);
        }

        public void unregisterTransport(IHdbTransport transport) {
            HwAdbService.this.mHdbTransports.remove(transport.asBinder());
        }

        public boolean isHdbEnabled() {
            return HwAdbService.this.mHdbEnabled;
        }
    }
}
