package android.os;

import android.app.job.JobParameters;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.net.wifi.WifiConfiguration.GroupCipher;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiScanLog;
import android.os.SystemProto.Misc;
import android.os.SystemProto.PowerUseSummary;
import android.os.UidProto.Network;
import android.provider.SettingsStringUtil;
import android.provider.Telephony.BaseMmsColumns;
import android.rms.AppAssociate;
import android.rms.HwSysResource;
import android.service.notification.ZenModeConfig;
import android.telephony.SignalStrength;
import android.telephony.SubscriptionPlan;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.util.ArrayMap;
import android.util.LongSparseArray;
import android.util.MutableBoolean;
import android.util.Pair;
import android.util.Printer;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.TimeUtils;
import android.util.proto.ProtoOutputStream;
import android.view.Menu;
import android.view.SurfaceControl;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatterySipper.DrainType;
import com.android.internal.os.BatteryStatsHelper;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public abstract class BatteryStats implements Parcelable {
    private static final String AGGREGATED_WAKELOCK_DATA = "awl";
    public static final int AGGREGATED_WAKE_TYPE_PARTIAL = 20;
    private static final String APK_DATA = "apk";
    private static final String AUDIO_DATA = "aud";
    public static final int AUDIO_TURNED_ON = 15;
    private static final String BATTERY_DATA = "bt";
    private static final String BATTERY_DISCHARGE_DATA = "dc";
    private static final String BATTERY_LEVEL_DATA = "lv";
    private static final int BATTERY_STATS_CHECKIN_VERSION = 9;
    private static final String BLUETOOTH_CONTROLLER_DATA = "ble";
    private static final String BLUETOOTH_MISC_DATA = "blem";
    public static final int BLUETOOTH_SCAN_ON = 19;
    public static final int BLUETOOTH_UNOPTIMIZED_SCAN_ON = 21;
    private static final long BYTES_PER_GB = 1073741824;
    private static final long BYTES_PER_KB = 1024;
    private static final long BYTES_PER_MB = 1048576;
    private static final String CAMERA_DATA = "cam";
    public static final int CAMERA_TURNED_ON = 17;
    private static final String CELLULAR_CONTROLLER_NAME = "Cellular";
    private static final String CHARGE_STEP_DATA = "csd";
    private static final String CHARGE_TIME_REMAIN_DATA = "ctr";
    static final int CHECKIN_VERSION = 32;
    private static final String CPU_DATA = "cpu";
    private static final String CPU_TIMES_AT_FREQ_DATA = "ctf";
    private static final String DATA_CONNECTION_COUNT_DATA = "dcc";
    static final String[] DATA_CONNECTION_NAMES = new String[]{"none", "gprs", "edge", "umts", "cdma", "evdo_0", "evdo_A", "1xrtt", "hsdpa", "hsupa", "hspa", "iden", "evdo_b", "lte", "ehrpd", "hspap", "gsm", "td_scdma", "iwlan", "lte_ca", "other"};
    public static final int DATA_CONNECTION_NONE = 0;
    public static final int DATA_CONNECTION_OTHER = 20;
    private static final String DATA_CONNECTION_TIME_DATA = "dct";
    public static final int DEVICE_IDLE_MODE_DEEP = 2;
    public static final int DEVICE_IDLE_MODE_LIGHT = 1;
    public static final int DEVICE_IDLE_MODE_OFF = 0;
    private static final String DISCHARGE_STEP_DATA = "dsd";
    private static final String DISCHARGE_TIME_REMAIN_DATA = "dtr";
    public static final int DUMP_CHARGED_ONLY = 2;
    public static final int DUMP_DAILY_ONLY = 4;
    public static final int DUMP_DEVICE_WIFI_ONLY = 64;
    public static final int DUMP_HISTORY_ONLY = 8;
    public static final int DUMP_INCLUDE_HISTORY = 16;
    public static final int DUMP_VERBOSE = 32;
    private static final String FLASHLIGHT_DATA = "fla";
    public static final int FLASHLIGHT_TURNED_ON = 16;
    public static final int FOREGROUND_ACTIVITY = 10;
    private static final String FOREGROUND_ACTIVITY_DATA = "fg";
    public static final int FOREGROUND_SERVICE = 22;
    private static final String FOREGROUND_SERVICE_DATA = "fgs";
    public static final int FULL_WIFI_LOCK = 5;
    private static final String GLOBAL_BLUETOOTH_CONTROLLER_DATA = "gble";
    private static final String GLOBAL_CPU_FREQ_DATA = "gcf";
    private static final String GLOBAL_MODEM_CONTROLLER_DATA = "gmcd";
    private static final String GLOBAL_NETWORK_DATA = "gn";
    private static final String GLOBAL_WIFI_CONTROLLER_DATA = "gwfcd";
    private static final String GLOBAL_WIFI_DATA = "gwfl";
    private static final String HISTORY_DATA = "h";
    public static final String[] HISTORY_EVENT_CHECKIN_NAMES = new String[]{"Enl", "Epr", "Efg", "Etp", "Esy", "Ewl", "Ejb", "Eur", "Euf", "Ecn", "Eac", "Epi", "Epu", "Eal", "Est", "Eai", "Eaa", "Etw", "Esw", "Ewa", "Elw", "Eec"};
    public static final IntToString[] HISTORY_EVENT_INT_FORMATTERS = new IntToString[]{sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sUidToString, sIntToString};
    public static final String[] HISTORY_EVENT_NAMES = new String[]{"null", "proc", FOREGROUND_ACTIVITY_DATA, "top", "sync", "wake_lock_in", "job", "user", "userfg", "conn", "active", "pkginst", "pkgunin", ZenModeConfig.IS_ALARM_PATH, "stats", "pkginactive", "pkgactive", "tmpwhitelist", "screenwake", "wakeupap", "longwake", "est_capacity"};
    public static final BitDescription[] HISTORY_STATE2_DESCRIPTIONS = new BitDescription[]{new BitDescription(Integer.MIN_VALUE, "power_save", "ps"), new BitDescription(1073741824, "video", BaseMmsColumns.MMS_VERSION), new BitDescription(536870912, "wifi_running", "Ww"), new BitDescription(268435456, "wifi", "W"), new BitDescription(134217728, "flashlight", "fl"), new BitDescription(HistoryItem.STATE2_DEVICE_IDLE_MASK, 25, "device_idle", "di", new String[]{"off", "light", "full", "???"}, new String[]{"off", "light", "full", "???"}), new BitDescription(16777216, "charging", "ch"), new BitDescription(262144, "usb_data", "Ud"), new BitDescription(8388608, "phone_in_call", "Pcl"), new BitDescription(4194304, "bluetooth", "b"), new BitDescription(112, 4, "wifi_signal_strength", "Wss", new String[]{WifiEnterpriseConfig.ENGINE_DISABLE, "1", WifiScanLog.EVENT_KEY2, WifiScanLog.EVENT_KEY3, WifiScanLog.EVENT_KEY4}, new String[]{WifiEnterpriseConfig.ENGINE_DISABLE, "1", WifiScanLog.EVENT_KEY2, WifiScanLog.EVENT_KEY3, WifiScanLog.EVENT_KEY4}), new BitDescription(15, 0, "wifi_suppl", "Wsp", WIFI_SUPPL_STATE_NAMES, WIFI_SUPPL_STATE_SHORT_NAMES), new BitDescription(2097152, "camera", "ca"), new BitDescription(1048576, "ble_scan", "bles"), new BitDescription(524288, "cellular_high_tx_power", "Chtp"), new BitDescription(128, 7, "gps_signal_quality", "Gss", new String[]{"poor", "good"}, new String[]{"poor", "good"})};
    public static final BitDescription[] HISTORY_STATE_DESCRIPTIONS = new BitDescription[]{new BitDescription(Integer.MIN_VALUE, "running", "r"), new BitDescription(1073741824, "wake_lock", "w"), new BitDescription(8388608, "sensor", "s"), new BitDescription(536870912, "gps", "g"), new BitDescription(268435456, "wifi_full_lock", "Wl"), new BitDescription(134217728, "wifi_scan", "Ws"), new BitDescription(65536, "wifi_multicast", "Wm"), new BitDescription(67108864, "wifi_radio", "Wr"), new BitDescription(33554432, "mobile_radio", "Pr"), new BitDescription(2097152, "phone_scanning", "Psc"), new BitDescription(4194304, "audio", "a"), new BitDescription(1048576, "screen", "S"), new BitDescription(524288, BatteryManager.EXTRA_PLUGGED, "BP"), new BitDescription(262144, "screen_doze", "Sd"), new BitDescription(HistoryItem.STATE_DATA_CONNECTION_MASK, 9, "data_conn", "Pcn", DATA_CONNECTION_NAMES, DATA_CONNECTION_NAMES), new BitDescription(448, 6, "phone_state", "Pst", new String[]{"in", "out", "emergency", "off"}, new String[]{"in", "out", "em", "off"}), new BitDescription(56, 3, "phone_signal_strength", "Pss", SignalStrength.SIGNAL_STRENGTH_NAMES, new String[]{WifiEnterpriseConfig.ENGINE_DISABLE, "1", WifiScanLog.EVENT_KEY2, WifiScanLog.EVENT_KEY3, WifiScanLog.EVENT_KEY4}), new BitDescription(7, 0, "brightness", "Sb", SCREEN_BRIGHTNESS_NAMES, SCREEN_BRIGHTNESS_SHORT_NAMES)};
    private static final String HISTORY_STRING_POOL = "hsp";
    public static final int JOB = 14;
    private static final String JOBS_DEFERRED_DATA = "jbd";
    private static final String JOB_COMPLETION_DATA = "jbc";
    private static final String JOB_DATA = "jb";
    public static final long[] JOB_FRESHNESS_BUCKETS = new long[]{DateUtils.HOUR_IN_MILLIS, 7200000, 14400000, 28800000, SubscriptionPlan.BYTES_UNLIMITED};
    private static final String KERNEL_WAKELOCK_DATA = "kwl";
    private static final boolean LOCAL_LOGV = false;
    public static final int MAX_TRACKED_SCREEN_STATE = 4;
    private static final String MISC_DATA = "m";
    private static final String MODEM_CONTROLLER_DATA = "mcd";
    public static final int NETWORK_BT_RX_DATA = 4;
    public static final int NETWORK_BT_TX_DATA = 5;
    private static final String NETWORK_DATA = "nt";
    public static final int NETWORK_MOBILE_BG_RX_DATA = 6;
    public static final int NETWORK_MOBILE_BG_TX_DATA = 7;
    public static final int NETWORK_MOBILE_RX_DATA = 0;
    public static final int NETWORK_MOBILE_TX_DATA = 1;
    public static final int NETWORK_WIFI_BG_RX_DATA = 8;
    public static final int NETWORK_WIFI_BG_TX_DATA = 9;
    public static final int NETWORK_WIFI_RX_DATA = 2;
    public static final int NETWORK_WIFI_TX_DATA = 3;
    public static final int NUM_DATA_CONNECTION_TYPES = 21;
    public static final int NUM_NETWORK_ACTIVITY_TYPES = 10;
    public static final int NUM_SCREEN_BRIGHTNESS_BINS = 5;
    public static final int NUM_WIFI_SIGNAL_STRENGTH_BINS = 5;
    public static final int NUM_WIFI_STATES = 8;
    public static final int NUM_WIFI_SUPPL_STATES = 13;
    private static final String POWER_USE_ITEM_DATA = "pwi";
    private static final String POWER_USE_SUMMARY_DATA = "pws";
    private static final String PROCESS_DATA = "pr";
    public static final int PROCESS_STATE = 12;
    private static final String RESOURCE_POWER_MANAGER_DATA = "rpm";
    public static final String RESULT_RECEIVER_CONTROLLER_KEY = "controller_activity";
    public static final int SCREEN_BRIGHTNESS_BRIGHT = 4;
    public static final int SCREEN_BRIGHTNESS_DARK = 0;
    private static final String SCREEN_BRIGHTNESS_DATA = "br";
    public static final int SCREEN_BRIGHTNESS_DIM = 1;
    public static final int SCREEN_BRIGHTNESS_LIGHT = 3;
    public static final int SCREEN_BRIGHTNESS_MEDIUM = 2;
    static final String[] SCREEN_BRIGHTNESS_NAMES = new String[]{"dark", "dim", "medium", "light", "bright"};
    static final String[] SCREEN_BRIGHTNESS_SHORT_NAMES = new String[]{WifiEnterpriseConfig.ENGINE_DISABLE, "1", WifiScanLog.EVENT_KEY2, WifiScanLog.EVENT_KEY3, WifiScanLog.EVENT_KEY4};
    protected static final boolean SCREEN_OFF_RPM_STATS_ENABLED = false;
    public static final int SENSOR = 3;
    private static final String SENSOR_DATA = "sr";
    public static final String SERVICE_NAME = "batterystats";
    private static final String SIGNAL_SCANNING_TIME_DATA = "sst";
    private static final String SIGNAL_STRENGTH_COUNT_DATA = "sgc";
    private static final String SIGNAL_STRENGTH_TIME_DATA = "sgt";
    private static final String STATE_TIME_DATA = "st";
    public static final int STATS_CURRENT = 1;
    public static final int STATS_SINCE_CHARGED = 0;
    public static final int STATS_SINCE_UNPLUGGED = 2;
    private static final String[] STAT_NAMES = new String[]{"l", "c", "u"};
    public static final long STEP_LEVEL_INITIAL_MODE_MASK = 71776119061217280L;
    public static final int STEP_LEVEL_INITIAL_MODE_SHIFT = 48;
    public static final long STEP_LEVEL_LEVEL_MASK = 280375465082880L;
    public static final int STEP_LEVEL_LEVEL_SHIFT = 40;
    public static final int[] STEP_LEVEL_MODES_OF_INTEREST = new int[]{7, 15, 11, 7, 7, 7, 7, 7, 15, 11};
    public static final int STEP_LEVEL_MODE_DEVICE_IDLE = 8;
    public static final String[] STEP_LEVEL_MODE_LABELS = new String[]{"screen off", "screen off power save", "screen off device idle", "screen on", "screen on power save", "screen doze", "screen doze power save", "screen doze-suspend", "screen doze-suspend power save", "screen doze-suspend device idle"};
    public static final int STEP_LEVEL_MODE_POWER_SAVE = 4;
    public static final int STEP_LEVEL_MODE_SCREEN_STATE = 3;
    public static final int[] STEP_LEVEL_MODE_VALUES = new int[]{0, 4, 8, 1, 5, 2, 6, 3, 7, 11};
    public static final long STEP_LEVEL_MODIFIED_MODE_MASK = -72057594037927936L;
    public static final int STEP_LEVEL_MODIFIED_MODE_SHIFT = 56;
    public static final long STEP_LEVEL_TIME_MASK = 1099511627775L;
    public static final int SYNC = 13;
    private static final String SYNC_DATA = "sy";
    private static final String TAG = "BatteryStats";
    private static final String UID_DATA = "uid";
    @VisibleForTesting
    public static final String UID_TIMES_TYPE_ALL = "A";
    private static final String USER_ACTIVITY_DATA = "ua";
    private static final String VERSION_DATA = "vers";
    private static final String VIBRATOR_DATA = "vib";
    public static final int VIBRATOR_ON = 9;
    private static final String VIDEO_DATA = "vid";
    public static final int VIDEO_TURNED_ON = 8;
    private static final String WAKELOCK_DATA = "wl";
    private static final String WAKEUP_ALARM_DATA = "wua";
    private static final String WAKEUP_REASON_DATA = "wr";
    public static final int WAKE_TYPE_DRAW = 18;
    public static final int WAKE_TYPE_FULL = 1;
    public static final int WAKE_TYPE_PARTIAL = 0;
    public static final int WAKE_TYPE_WINDOW = 2;
    public static final int WIFI_AGGREGATE_MULTICAST_ENABLED = 23;
    public static final int WIFI_BATCHED_SCAN = 11;
    private static final String WIFI_CONTROLLER_DATA = "wfcd";
    private static final String WIFI_CONTROLLER_NAME = "WiFi";
    private static final String WIFI_DATA = "wfl";
    private static final String WIFI_MULTICAST_DATA = "wmc";
    public static final int WIFI_MULTICAST_ENABLED = 7;
    private static final String WIFI_MULTICAST_TOTAL_DATA = "wmct";
    public static final int WIFI_RUNNING = 4;
    public static final int WIFI_SCAN = 6;
    private static final String WIFI_SIGNAL_STRENGTH_COUNT_DATA = "wsgc";
    private static final String WIFI_SIGNAL_STRENGTH_TIME_DATA = "wsgt";
    private static final String WIFI_STATE_COUNT_DATA = "wsc";
    static final String[] WIFI_STATE_NAMES = new String[]{"off", "scanning", "no_net", "disconn", "sta", "p2p", "sta_p2p", "soft_ap"};
    public static final int WIFI_STATE_OFF = 0;
    public static final int WIFI_STATE_OFF_SCANNING = 1;
    public static final int WIFI_STATE_ON_CONNECTED_P2P = 5;
    public static final int WIFI_STATE_ON_CONNECTED_STA = 4;
    public static final int WIFI_STATE_ON_CONNECTED_STA_P2P = 6;
    public static final int WIFI_STATE_ON_DISCONNECTED = 3;
    public static final int WIFI_STATE_ON_NO_NETWORKS = 2;
    public static final int WIFI_STATE_SOFT_AP = 7;
    private static final String WIFI_STATE_TIME_DATA = "wst";
    public static final int WIFI_SUPPL_STATE_ASSOCIATED = 7;
    public static final int WIFI_SUPPL_STATE_ASSOCIATING = 6;
    public static final int WIFI_SUPPL_STATE_AUTHENTICATING = 5;
    public static final int WIFI_SUPPL_STATE_COMPLETED = 10;
    private static final String WIFI_SUPPL_STATE_COUNT_DATA = "wssc";
    public static final int WIFI_SUPPL_STATE_DISCONNECTED = 1;
    public static final int WIFI_SUPPL_STATE_DORMANT = 11;
    public static final int WIFI_SUPPL_STATE_FOUR_WAY_HANDSHAKE = 8;
    public static final int WIFI_SUPPL_STATE_GROUP_HANDSHAKE = 9;
    public static final int WIFI_SUPPL_STATE_INACTIVE = 3;
    public static final int WIFI_SUPPL_STATE_INTERFACE_DISABLED = 2;
    public static final int WIFI_SUPPL_STATE_INVALID = 0;
    static final String[] WIFI_SUPPL_STATE_NAMES = new String[]{"invalid", "disconn", "disabled", "inactive", "scanning", "authenticating", "associating", "associated", "4-way-handshake", "group-handshake", "completed", "dormant", "uninit"};
    public static final int WIFI_SUPPL_STATE_SCANNING = 4;
    static final String[] WIFI_SUPPL_STATE_SHORT_NAMES = new String[]{"inv", "dsc", "dis", "inact", "scan", "auth", "ascing", "asced", "4-way", GroupCipher.varName, "compl", "dorm", "uninit"};
    private static final String WIFI_SUPPL_STATE_TIME_DATA = "wsst";
    public static final int WIFI_SUPPL_STATE_UNINITIALIZED = 12;
    private static final IntToString sIntToString = -$$Lambda$BatteryStats$q1UvBdLgHRZVzc68BxdksTmbuCw.INSTANCE;
    private static final IntToString sUidToString = -$$Lambda$IyvVQC-0mKtsfXbnO0kDL64hrk0.INSTANCE;
    private final StringBuilder mFormatBuilder = new StringBuilder(32);
    private final Formatter mFormatter = new Formatter(this.mFormatBuilder);

    /* renamed from: android.os.BatteryStats$2 */
    static /* synthetic */ class AnonymousClass2 {
        static final /* synthetic */ int[] $SwitchMap$com$android$internal$os$BatterySipper$DrainType = new int[DrainType.values().length];

        static {
            try {
                $SwitchMap$com$android$internal$os$BatterySipper$DrainType[DrainType.AMBIENT_DISPLAY.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$internal$os$BatterySipper$DrainType[DrainType.IDLE.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$internal$os$BatterySipper$DrainType[DrainType.CELL.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$internal$os$BatterySipper$DrainType[DrainType.PHONE.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$android$internal$os$BatterySipper$DrainType[DrainType.WIFI.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$android$internal$os$BatterySipper$DrainType[DrainType.BLUETOOTH.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$android$internal$os$BatterySipper$DrainType[DrainType.SCREEN.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$com$android$internal$os$BatterySipper$DrainType[DrainType.FLASHLIGHT.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$com$android$internal$os$BatterySipper$DrainType[DrainType.APP.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$com$android$internal$os$BatterySipper$DrainType[DrainType.USER.ordinal()] = 10;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$com$android$internal$os$BatterySipper$DrainType[DrainType.UNACCOUNTED.ordinal()] = 11;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$com$android$internal$os$BatterySipper$DrainType[DrainType.OVERCOUNTED.ordinal()] = 12;
            } catch (NoSuchFieldError e12) {
            }
            try {
                $SwitchMap$com$android$internal$os$BatterySipper$DrainType[DrainType.CAMERA.ordinal()] = 13;
            } catch (NoSuchFieldError e13) {
            }
            try {
                $SwitchMap$com$android$internal$os$BatterySipper$DrainType[DrainType.MEMORY.ordinal()] = 14;
            } catch (NoSuchFieldError e14) {
            }
        }
    }

    public static final class BitDescription {
        public final int mask;
        public final String name;
        public final int shift;
        public final String shortName;
        public final String[] shortValues;
        public final String[] values;

        public BitDescription(int mask, String name, String shortName) {
            this.mask = mask;
            this.shift = -1;
            this.name = name;
            this.shortName = shortName;
            this.values = null;
            this.shortValues = null;
        }

        public BitDescription(int mask, int shift, String name, String shortName, String[] values, String[] shortValues) {
            this.mask = mask;
            this.shift = shift;
            this.name = name;
            this.shortName = shortName;
            this.values = values;
            this.shortValues = shortValues;
        }
    }

    public static abstract class ControllerActivityCounter {
        public abstract LongCounter getIdleTimeCounter();

        public abstract LongCounter getPowerCounter();

        public abstract LongCounter getRxTimeCounter();

        public abstract LongCounter getScanTimeCounter();

        public abstract LongCounter getSleepTimeCounter();

        public abstract LongCounter[] getTxTimeCounters();
    }

    public static abstract class Counter {
        public abstract int getCountLocked(int i);

        public abstract void logState(Printer printer, String str);
    }

    public static final class DailyItem {
        public LevelStepTracker mChargeSteps;
        public LevelStepTracker mDischargeSteps;
        public long mEndTime;
        public ArrayList<PackageChange> mPackageChanges;
        public long mStartTime;
    }

    public static final class HistoryEventTracker {
        private final HashMap<String, SparseIntArray>[] mActiveEvents = new HashMap[22];

        public boolean updateState(int code, String name, int uid, int poolIdx) {
            int idx;
            HashMap<String, SparseIntArray> active;
            SparseIntArray uids;
            if ((32768 & code) != 0) {
                idx = code & HistoryItem.EVENT_TYPE_MASK;
                active = this.mActiveEvents[idx];
                if (active == null) {
                    active = new HashMap();
                    this.mActiveEvents[idx] = active;
                }
                uids = (SparseIntArray) active.get(name);
                if (uids == null) {
                    uids = new SparseIntArray();
                    active.put(name, uids);
                }
                if (uids.indexOfKey(uid) >= 0) {
                    return false;
                }
                uids.put(uid, poolIdx);
            } else if ((code & 16384) != 0) {
                active = this.mActiveEvents[code & HistoryItem.EVENT_TYPE_MASK];
                if (active == null) {
                    return false;
                }
                uids = (SparseIntArray) active.get(name);
                if (uids == null) {
                    return false;
                }
                idx = uids.indexOfKey(uid);
                if (idx < 0) {
                    return false;
                }
                uids.removeAt(idx);
                if (uids.size() <= 0) {
                    active.remove(name);
                }
            }
            return true;
        }

        public void removeEvents(int code) {
            this.mActiveEvents[HistoryItem.EVENT_TYPE_MASK & code] = null;
        }

        public HashMap<String, SparseIntArray> getStateForEvent(int code) {
            return this.mActiveEvents[code];
        }
    }

    public static class HistoryPrinter {
        long lastTime = -1;
        int oldChargeMAh = -1;
        int oldHealth = -1;
        int oldLevel = -1;
        int oldPlug = -1;
        int oldState = 0;
        int oldState2 = 0;
        int oldStatus = -1;
        int oldTemp = -1;
        int oldVolt = -1;

        void reset() {
            this.oldState2 = 0;
            this.oldState = 0;
            this.oldLevel = -1;
            this.oldStatus = -1;
            this.oldHealth = -1;
            this.oldPlug = -1;
            this.oldTemp = -1;
            this.oldVolt = -1;
            this.oldChargeMAh = -1;
        }

        public void printNextItem(PrintWriter pw, HistoryItem rec, long baseTime, boolean checkin, boolean verbose) {
            pw.print(printNextItem(rec, baseTime, checkin, verbose));
        }

        public void printNextItem(ProtoOutputStream proto, HistoryItem rec, long baseTime, boolean verbose) {
            for (String line : printNextItem(rec, baseTime, true, verbose).split("\n")) {
                proto.write(2237677961222L, line);
            }
        }

        private String printNextItem(HistoryItem rec, long baseTime, boolean checkin, boolean verbose) {
            StringBuilder item = new StringBuilder();
            if (checkin) {
                item.append(9);
                item.append(',');
                item.append(BatteryStats.HISTORY_DATA);
                item.append(',');
                if (this.lastTime < 0) {
                    item.append(rec.time - baseTime);
                } else {
                    item.append(rec.time - this.lastTime);
                }
                this.lastTime = rec.time;
            } else {
                item.append("  ");
                TimeUtils.formatDuration(rec.time - baseTime, item, 19);
                item.append(" (");
                item.append(rec.numReadInts);
                item.append(") ");
            }
            if (rec.cmd == (byte) 4) {
                if (checkin) {
                    item.append(SettingsStringUtil.DELIMITER);
                }
                item.append("START\n");
                reset();
            } else if (rec.cmd == (byte) 5 || rec.cmd == (byte) 7) {
                if (checkin) {
                    item.append(SettingsStringUtil.DELIMITER);
                }
                if (rec.cmd == (byte) 7) {
                    item.append("RESET:");
                    reset();
                }
                item.append("TIME:");
                if (checkin) {
                    item.append(rec.currentTime);
                    item.append("\n");
                } else {
                    item.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                    item.append(DateFormat.format((CharSequence) "yyyy-MM-dd-HH-mm-ss", rec.currentTime).toString());
                    item.append("\n");
                }
            } else if (rec.cmd == (byte) 8) {
                if (checkin) {
                    item.append(SettingsStringUtil.DELIMITER);
                }
                item.append("SHUTDOWN\n");
            } else if (rec.cmd == (byte) 6) {
                if (checkin) {
                    item.append(SettingsStringUtil.DELIMITER);
                }
                item.append("*OVERFLOW*\n");
            } else {
                int i;
                int idx;
                if (!checkin) {
                    if (rec.batteryLevel < (byte) 10) {
                        item.append("00");
                    } else if (rec.batteryLevel < (byte) 100) {
                        item.append(WifiEnterpriseConfig.ENGINE_DISABLE);
                    }
                    item.append(rec.batteryLevel);
                    if (verbose) {
                        item.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                        if (rec.states >= 0) {
                            if (rec.states < 16) {
                                item.append("0000000");
                            } else if (rec.states < 256) {
                                item.append("000000");
                            } else if (rec.states < 4096) {
                                item.append("00000");
                            } else if (rec.states < 65536) {
                                item.append("0000");
                            } else if (rec.states < 1048576) {
                                item.append("000");
                            } else if (rec.states < 16777216) {
                                item.append("00");
                            } else if (rec.states < 268435456) {
                                item.append(WifiEnterpriseConfig.ENGINE_DISABLE);
                            }
                        }
                        item.append(Integer.toHexString(rec.states));
                    }
                } else if (this.oldLevel != rec.batteryLevel) {
                    this.oldLevel = rec.batteryLevel;
                    item.append(",Bl=");
                    item.append(rec.batteryLevel);
                }
                if (this.oldStatus != rec.batteryStatus) {
                    this.oldStatus = rec.batteryStatus;
                    item.append(checkin ? ",Bs=" : " status=");
                    switch (this.oldStatus) {
                        case 1:
                            item.append(checkin ? "?" : "unknown");
                            break;
                        case 2:
                            item.append(checkin ? "c" : "charging");
                            break;
                        case 3:
                            item.append(checkin ? "d" : "discharging");
                            break;
                        case 4:
                            item.append(checkin ? "n" : "not-charging");
                            break;
                        case 5:
                            item.append(checkin ? "f" : "full");
                            break;
                        default:
                            item.append(this.oldStatus);
                            break;
                    }
                }
                if (this.oldHealth != rec.batteryHealth) {
                    this.oldHealth = rec.batteryHealth;
                    item.append(checkin ? ",Bh=" : " health=");
                    switch (this.oldHealth) {
                        case 1:
                            item.append(checkin ? "?" : "unknown");
                            break;
                        case 2:
                            item.append(checkin ? "g" : "good");
                            break;
                        case 3:
                            item.append(checkin ? BatteryStats.HISTORY_DATA : "overheat");
                            break;
                        case 4:
                            item.append(checkin ? "d" : "dead");
                            break;
                        case 5:
                            item.append(checkin ? BaseMmsColumns.MMS_VERSION : "over-voltage");
                            break;
                        case 6:
                            item.append(checkin ? "f" : "failure");
                            break;
                        case 7:
                            item.append(checkin ? "c" : "cold");
                            break;
                        default:
                            item.append(this.oldHealth);
                            break;
                    }
                }
                if (this.oldPlug != rec.batteryPlugType) {
                    this.oldPlug = rec.batteryPlugType;
                    item.append(checkin ? ",Bp=" : " plug=");
                    i = this.oldPlug;
                    if (i != 4) {
                        switch (i) {
                            case 0:
                                item.append(checkin ? "n" : "none");
                                break;
                            case 1:
                                item.append(checkin ? "a" : "ac");
                                break;
                            case 2:
                                item.append(checkin ? "u" : "usb");
                                break;
                            default:
                                item.append(this.oldPlug);
                                break;
                        }
                    }
                    item.append(checkin ? "w" : "wireless");
                }
                if (this.oldTemp != rec.batteryTemperature) {
                    this.oldTemp = rec.batteryTemperature;
                    item.append(checkin ? ",Bt=" : " temp=");
                    item.append(this.oldTemp);
                }
                if (this.oldVolt != rec.batteryVoltage) {
                    this.oldVolt = rec.batteryVoltage;
                    item.append(checkin ? ",Bv=" : " volt=");
                    item.append(this.oldVolt);
                }
                int chargeMAh = rec.batteryChargeUAh / 1000;
                if (this.oldChargeMAh != chargeMAh) {
                    this.oldChargeMAh = chargeMAh;
                    item.append(checkin ? ",Bcc=" : " charge=");
                    item.append(this.oldChargeMAh);
                }
                BatteryStats.printBitDescriptions(item, this.oldState, rec.states, rec.wakelockTag, BatteryStats.HISTORY_STATE_DESCRIPTIONS, checkin ^ 1);
                BatteryStats.printBitDescriptions(item, this.oldState2, rec.states2, null, BatteryStats.HISTORY_STATE2_DESCRIPTIONS, checkin ^ 1);
                if (rec.wakeReasonTag != null) {
                    if (checkin) {
                        item.append(",wr=");
                        item.append(rec.wakeReasonTag.poolIdx);
                    } else {
                        item.append(" wake_reason=");
                        item.append(rec.wakeReasonTag.uid);
                        item.append(":\"");
                        item.append(rec.wakeReasonTag.string);
                        item.append("\"");
                    }
                }
                if (rec.eventCode != 0) {
                    String[] eventNames;
                    item.append(checkin ? "," : WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                    if ((rec.eventCode & 32768) != 0) {
                        item.append("+");
                    } else if ((rec.eventCode & 16384) != 0) {
                        item.append("-");
                    }
                    if (checkin) {
                        eventNames = BatteryStats.HISTORY_EVENT_CHECKIN_NAMES;
                    } else {
                        eventNames = BatteryStats.HISTORY_EVENT_NAMES;
                    }
                    idx = rec.eventCode & HistoryItem.EVENT_TYPE_MASK;
                    if (idx < 0 || idx >= eventNames.length) {
                        item.append(checkin ? "Ev" : "event");
                        item.append(idx);
                    } else {
                        item.append(eventNames[idx]);
                    }
                    item.append("=");
                    if (checkin) {
                        item.append(rec.eventTag.poolIdx);
                    } else {
                        item.append(BatteryStats.HISTORY_EVENT_INT_FORMATTERS[idx].applyAsString(rec.eventTag.uid));
                        item.append(":\"");
                        item.append(rec.eventTag.string);
                        item.append("\"");
                    }
                }
                item.append("\n");
                if (rec.stepDetails != null) {
                    if (checkin) {
                        item.append(9);
                        item.append(',');
                        item.append(BatteryStats.HISTORY_DATA);
                        item.append(",0,Dcpu=");
                        item.append(rec.stepDetails.userTime);
                        item.append(SettingsStringUtil.DELIMITER);
                        item.append(rec.stepDetails.systemTime);
                        if (rec.stepDetails.appCpuUid1 >= 0) {
                            printStepCpuUidCheckinDetails(item, rec.stepDetails.appCpuUid1, rec.stepDetails.appCpuUTime1, rec.stepDetails.appCpuSTime1);
                            if (rec.stepDetails.appCpuUid2 >= 0) {
                                printStepCpuUidCheckinDetails(item, rec.stepDetails.appCpuUid2, rec.stepDetails.appCpuUTime2, rec.stepDetails.appCpuSTime2);
                            }
                            if (rec.stepDetails.appCpuUid3 >= 0) {
                                printStepCpuUidCheckinDetails(item, rec.stepDetails.appCpuUid3, rec.stepDetails.appCpuUTime3, rec.stepDetails.appCpuSTime3);
                            }
                        }
                        item.append("\n");
                        item.append(9);
                        item.append(',');
                        item.append(BatteryStats.HISTORY_DATA);
                        item.append(",0,Dpst=");
                        item.append(rec.stepDetails.statUserTime);
                        item.append(',');
                        item.append(rec.stepDetails.statSystemTime);
                        item.append(',');
                        item.append(rec.stepDetails.statIOWaitTime);
                        item.append(',');
                        item.append(rec.stepDetails.statIrqTime);
                        item.append(',');
                        item.append(rec.stepDetails.statSoftIrqTime);
                        item.append(',');
                        item.append(rec.stepDetails.statIdlTime);
                        item.append(',');
                        if (rec.stepDetails.statPlatformIdleState != null) {
                            item.append(rec.stepDetails.statPlatformIdleState);
                            if (rec.stepDetails.statSubsystemPowerState != null) {
                                item.append(',');
                            }
                        }
                        if (rec.stepDetails.statSubsystemPowerState != null) {
                            item.append(rec.stepDetails.statSubsystemPowerState);
                        }
                        item.append("\n");
                    } else {
                        item.append("                 Details: cpu=");
                        item.append(rec.stepDetails.userTime);
                        item.append("u+");
                        item.append(rec.stepDetails.systemTime);
                        item.append("s");
                        if (rec.stepDetails.appCpuUid1 >= 0) {
                            item.append(" (");
                            printStepCpuUidDetails(item, rec.stepDetails.appCpuUid1, rec.stepDetails.appCpuUTime1, rec.stepDetails.appCpuSTime1);
                            if (rec.stepDetails.appCpuUid2 >= 0) {
                                item.append(", ");
                                printStepCpuUidDetails(item, rec.stepDetails.appCpuUid2, rec.stepDetails.appCpuUTime2, rec.stepDetails.appCpuSTime2);
                            }
                            if (rec.stepDetails.appCpuUid3 >= 0) {
                                item.append(", ");
                                printStepCpuUidDetails(item, rec.stepDetails.appCpuUid3, rec.stepDetails.appCpuUTime3, rec.stepDetails.appCpuSTime3);
                            }
                            item.append(')');
                        }
                        item.append("\n");
                        item.append("                          /proc/stat=");
                        item.append(rec.stepDetails.statUserTime);
                        item.append(" usr, ");
                        item.append(rec.stepDetails.statSystemTime);
                        item.append(" sys, ");
                        item.append(rec.stepDetails.statIOWaitTime);
                        item.append(" io, ");
                        item.append(rec.stepDetails.statIrqTime);
                        item.append(" irq, ");
                        item.append(rec.stepDetails.statSoftIrqTime);
                        item.append(" sirq, ");
                        item.append(rec.stepDetails.statIdlTime);
                        item.append(" idle");
                        idx = rec.stepDetails.statIdlTime + ((((rec.stepDetails.statUserTime + rec.stepDetails.statSystemTime) + rec.stepDetails.statIOWaitTime) + rec.stepDetails.statIrqTime) + rec.stepDetails.statSoftIrqTime);
                        if (idx > 0) {
                            item.append(" (");
                            item.append(String.format("%.1f%%", new Object[]{Float.valueOf((((float) i) / ((float) idx)) * 100.0f)}));
                            item.append(" of ");
                            StringBuilder sb = new StringBuilder(64);
                            BatteryStats.formatTimeMsNoSpace(sb, (long) (idx * 10));
                            item.append(sb);
                            item.append(")");
                        }
                        item.append(", PlatformIdleStat ");
                        item.append(rec.stepDetails.statPlatformIdleState);
                        item.append("\n");
                        item.append(", SubsystemPowerState ");
                        item.append(rec.stepDetails.statSubsystemPowerState);
                        item.append("\n");
                    }
                }
                this.oldState = rec.states;
                this.oldState2 = rec.states2;
            }
            return item.toString();
        }

        private void printStepCpuUidDetails(StringBuilder sb, int uid, int utime, int stime) {
            UserHandle.formatUid(sb, uid);
            sb.append("=");
            sb.append(utime);
            sb.append("u+");
            sb.append(stime);
            sb.append("s");
        }

        private void printStepCpuUidCheckinDetails(StringBuilder sb, int uid, int utime, int stime) {
            sb.append('/');
            sb.append(uid);
            sb.append(SettingsStringUtil.DELIMITER);
            sb.append(utime);
            sb.append(SettingsStringUtil.DELIMITER);
            sb.append(stime);
        }
    }

    public static final class HistoryStepDetails {
        public int appCpuSTime1;
        public int appCpuSTime2;
        public int appCpuSTime3;
        public int appCpuUTime1;
        public int appCpuUTime2;
        public int appCpuUTime3;
        public int appCpuUid1;
        public int appCpuUid2;
        public int appCpuUid3;
        public int statIOWaitTime;
        public int statIdlTime;
        public int statIrqTime;
        public String statPlatformIdleState;
        public int statSoftIrqTime;
        public String statSubsystemPowerState;
        public int statSystemTime;
        public int statUserTime;
        public int systemTime;
        public int userTime;

        public HistoryStepDetails() {
            clear();
        }

        public void clear() {
            this.systemTime = 0;
            this.userTime = 0;
            this.appCpuUid3 = -1;
            this.appCpuUid2 = -1;
            this.appCpuUid1 = -1;
            this.appCpuSTime3 = 0;
            this.appCpuUTime3 = 0;
            this.appCpuSTime2 = 0;
            this.appCpuUTime2 = 0;
            this.appCpuSTime1 = 0;
            this.appCpuUTime1 = 0;
        }

        public void writeToParcel(Parcel out) {
            out.writeInt(this.userTime);
            out.writeInt(this.systemTime);
            out.writeInt(this.appCpuUid1);
            out.writeInt(this.appCpuUTime1);
            out.writeInt(this.appCpuSTime1);
            out.writeInt(this.appCpuUid2);
            out.writeInt(this.appCpuUTime2);
            out.writeInt(this.appCpuSTime2);
            out.writeInt(this.appCpuUid3);
            out.writeInt(this.appCpuUTime3);
            out.writeInt(this.appCpuSTime3);
            out.writeInt(this.statUserTime);
            out.writeInt(this.statSystemTime);
            out.writeInt(this.statIOWaitTime);
            out.writeInt(this.statIrqTime);
            out.writeInt(this.statSoftIrqTime);
            out.writeInt(this.statIdlTime);
            out.writeString(this.statPlatformIdleState);
            out.writeString(this.statSubsystemPowerState);
        }

        public void readFromParcel(Parcel in) {
            this.userTime = in.readInt();
            this.systemTime = in.readInt();
            this.appCpuUid1 = in.readInt();
            this.appCpuUTime1 = in.readInt();
            this.appCpuSTime1 = in.readInt();
            this.appCpuUid2 = in.readInt();
            this.appCpuUTime2 = in.readInt();
            this.appCpuSTime2 = in.readInt();
            this.appCpuUid3 = in.readInt();
            this.appCpuUTime3 = in.readInt();
            this.appCpuSTime3 = in.readInt();
            this.statUserTime = in.readInt();
            this.statSystemTime = in.readInt();
            this.statIOWaitTime = in.readInt();
            this.statIrqTime = in.readInt();
            this.statSoftIrqTime = in.readInt();
            this.statIdlTime = in.readInt();
            this.statPlatformIdleState = in.readString();
            this.statSubsystemPowerState = in.readString();
        }
    }

    public static final class HistoryTag {
        public int poolIdx;
        public String string;
        public int uid;

        public void setTo(HistoryTag o) {
            this.string = o.string != null ? o.string : "";
            this.uid = o.uid;
            this.poolIdx = o.poolIdx;
        }

        public void setTo(String _string, int _uid) {
            this.string = _string != null ? _string : "";
            this.uid = _uid;
            this.poolIdx = -1;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.string);
            dest.writeInt(this.uid);
        }

        public void readFromParcel(Parcel src) {
            this.string = src.readString();
            this.uid = src.readInt();
            this.poolIdx = -1;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            HistoryTag that = (HistoryTag) o;
            if (this.uid == that.uid && this.string.equals(that.string)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (31 * this.string.hashCode()) + this.uid;
        }
    }

    @FunctionalInterface
    public interface IntToString {
        String applyAsString(int i);
    }

    public static final class LevelStepTracker {
        public long mLastStepTime = -1;
        public int mNumStepDurations;
        public final long[] mStepDurations;

        public LevelStepTracker(int maxLevelSteps) {
            this.mStepDurations = new long[maxLevelSteps];
        }

        public LevelStepTracker(int numSteps, long[] steps) {
            this.mNumStepDurations = numSteps;
            this.mStepDurations = new long[numSteps];
            System.arraycopy(steps, 0, this.mStepDurations, 0, numSteps);
        }

        public long getDurationAt(int index) {
            return this.mStepDurations[index] & BatteryStats.STEP_LEVEL_TIME_MASK;
        }

        public int getLevelAt(int index) {
            return (int) ((this.mStepDurations[index] & BatteryStats.STEP_LEVEL_LEVEL_MASK) >> 40);
        }

        public int getInitModeAt(int index) {
            return (int) ((this.mStepDurations[index] & BatteryStats.STEP_LEVEL_INITIAL_MODE_MASK) >> 48);
        }

        public int getModModeAt(int index) {
            return (int) ((this.mStepDurations[index] & BatteryStats.STEP_LEVEL_MODIFIED_MODE_MASK) >> 56);
        }

        private void appendHex(long val, int topOffset, StringBuilder out) {
            boolean hasData = false;
            while (topOffset >= 0) {
                int digit = (int) ((val >> topOffset) & 15);
                topOffset -= 4;
                if (hasData || digit != 0) {
                    hasData = true;
                    if (digit < 0 || digit > 9) {
                        out.append((char) ((97 + digit) - 10));
                    } else {
                        out.append((char) (48 + digit));
                    }
                }
            }
        }

        public void encodeEntryAt(int index, StringBuilder out) {
            long item = this.mStepDurations[index];
            long duration = BatteryStats.STEP_LEVEL_TIME_MASK & item;
            int level = (int) ((BatteryStats.STEP_LEVEL_LEVEL_MASK & item) >> 40);
            int initMode = (int) ((BatteryStats.STEP_LEVEL_INITIAL_MODE_MASK & item) >> 48);
            int modMode = (int) ((BatteryStats.STEP_LEVEL_MODIFIED_MODE_MASK & item) >> 56);
            switch ((initMode & 3) + 1) {
                case 1:
                    out.append('f');
                    break;
                case 2:
                    out.append('o');
                    break;
                case 3:
                    out.append(DateFormat.DATE);
                    break;
                case 4:
                    out.append(DateFormat.TIME_ZONE);
                    break;
            }
            if ((initMode & 4) != 0) {
                out.append('p');
            }
            if ((initMode & 8) != 0) {
                out.append('i');
            }
            switch ((modMode & 3) + 1) {
                case 1:
                    out.append('F');
                    break;
                case 2:
                    out.append('O');
                    break;
                case 3:
                    out.append('D');
                    break;
                case 4:
                    out.append('Z');
                    break;
            }
            if ((modMode & 4) != 0) {
                out.append('P');
            }
            if ((modMode & 8) != 0) {
                out.append('I');
            }
            out.append('-');
            appendHex((long) level, 4, out);
            out.append('-');
            appendHex(duration, 36, out);
        }

        public void decodeEntryAt(int index, String value) {
            char c;
            char c2;
            String str = value;
            int N = value.length();
            int i = 0;
            long out = 0;
            while (true) {
                c = '-';
                if (i < N) {
                    char charAt = str.charAt(i);
                    char c3 = charAt;
                    if (charAt != '-') {
                        i++;
                        switch (c3) {
                            case 'D':
                                out |= 144115188075855872L;
                                break;
                            case 'F':
                                out |= 0;
                                break;
                            case 'I':
                                out |= 576460752303423488L;
                                break;
                            case 'O':
                                out |= 72057594037927936L;
                                break;
                            case 'P':
                                out |= 288230376151711744L;
                                break;
                            case 'Z':
                                out |= 216172782113783808L;
                                break;
                            case 'd':
                                out |= 562949953421312L;
                                break;
                            case 'f':
                                out |= 0;
                                break;
                            case 'i':
                                out |= 2251799813685248L;
                                break;
                            case 'o':
                                out |= 281474976710656L;
                                break;
                            case 'p':
                                out |= 1125899906842624L;
                                break;
                            case 'z':
                                out |= 844424930131968L;
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
            i++;
            long level = 0;
            while (true) {
                c2 = '0';
                if (i < N) {
                    char charAt2 = str.charAt(i);
                    char c4 = charAt2;
                    if (charAt2 != '-') {
                        i++;
                        level <<= 4;
                        charAt2 = c4;
                        if (charAt2 >= '0' && charAt2 <= '9') {
                            level += (long) (charAt2 - 48);
                        } else if (charAt2 >= DateFormat.AM_PM && charAt2 <= 'f') {
                            level += (long) ((charAt2 - 97) + 10);
                        } else if (charAt2 >= DateFormat.CAPITAL_AM_PM && charAt2 <= 'F') {
                            level += (long) ((charAt2 - 65) + 10);
                        }
                    }
                }
            }
            i++;
            long out2 = out | ((level << 40) & BatteryStats.STEP_LEVEL_LEVEL_MASK);
            long duration = 0;
            while (i < N) {
                char charAt3 = str.charAt(i);
                char c5 = charAt3;
                if (charAt3 != c) {
                    long level2;
                    i++;
                    duration <<= 4;
                    charAt3 = c5;
                    if (charAt3 < c2 || charAt3 > '9') {
                        level2 = level;
                        if (charAt3 >= DateFormat.AM_PM && charAt3 <= 'f') {
                            duration += (long) ((charAt3 - 97) + 10);
                        } else if (charAt3 >= DateFormat.CAPITAL_AM_PM && charAt3 <= 'F') {
                            duration += (long) ((charAt3 - 65) + 10);
                        }
                    } else {
                        level2 = level;
                        duration += (long) (charAt3 - 48);
                    }
                    level = level2;
                    c2 = '0';
                    c = '-';
                } else {
                    this.mStepDurations[index] = (duration & BatteryStats.STEP_LEVEL_TIME_MASK) | out2;
                }
            }
            this.mStepDurations[index] = (duration & BatteryStats.STEP_LEVEL_TIME_MASK) | out2;
        }

        public void init() {
            this.mLastStepTime = -1;
            this.mNumStepDurations = 0;
        }

        public void clearTime() {
            this.mLastStepTime = -1;
        }

        public long computeTimePerLevel() {
            long[] steps = this.mStepDurations;
            int numSteps = this.mNumStepDurations;
            if (numSteps <= 0) {
                return -1;
            }
            long total = 0;
            for (int i = 0; i < numSteps; i++) {
                total += steps[i] & BatteryStats.STEP_LEVEL_TIME_MASK;
            }
            return total / ((long) numSteps);
        }

        public long computeTimeEstimate(long modesOfInterest, long modeValues, int[] outNumOfInterest) {
            long[] steps = this.mStepDurations;
            int count = this.mNumStepDurations;
            if (count <= 0) {
                return -1;
            }
            int numOfInterest = 0;
            long total = 0;
            for (int i = 0; i < count; i++) {
                long initMode = (steps[i] & BatteryStats.STEP_LEVEL_INITIAL_MODE_MASK) >> 48;
                if ((((steps[i] & BatteryStats.STEP_LEVEL_MODIFIED_MODE_MASK) >> 56) & modesOfInterest) == 0 && (initMode & modesOfInterest) == modeValues) {
                    numOfInterest++;
                    total += steps[i] & BatteryStats.STEP_LEVEL_TIME_MASK;
                }
            }
            if (numOfInterest <= 0) {
                return -1;
            }
            if (outNumOfInterest != null) {
                outNumOfInterest[0] = numOfInterest;
            }
            return (total / ((long) numOfInterest)) * 100;
        }

        public void addLevelSteps(int numStepLevels, long modeBits, long elapsedRealtime) {
            int i = numStepLevels;
            long j = elapsedRealtime;
            int stepCount = this.mNumStepDurations;
            long lastStepTime = this.mLastStepTime;
            if (lastStepTime >= 0 && i > 0) {
                long[] steps = this.mStepDurations;
                long duration = j - lastStepTime;
                for (int i2 = 0; i2 < i; i2++) {
                    System.arraycopy(steps, 0, steps, 1, steps.length - 1);
                    long thisDuration = duration / ((long) (i - i2));
                    duration -= thisDuration;
                    if (thisDuration > BatteryStats.STEP_LEVEL_TIME_MASK) {
                        thisDuration = BatteryStats.STEP_LEVEL_TIME_MASK;
                    }
                    steps[0] = thisDuration | modeBits;
                }
                stepCount += i;
                if (stepCount > steps.length) {
                    stepCount = steps.length;
                }
            }
            this.mNumStepDurations = stepCount;
            this.mLastStepTime = j;
        }

        public void readFromParcel(Parcel in) {
            int N = in.readInt();
            if (N <= this.mStepDurations.length) {
                this.mNumStepDurations = N;
                for (int i = 0; i < N; i++) {
                    this.mStepDurations[i] = in.readLong();
                }
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("more step durations than available: ");
            stringBuilder.append(N);
            throw new ParcelFormatException(stringBuilder.toString());
        }

        public void writeToParcel(Parcel out) {
            int N = this.mNumStepDurations;
            out.writeInt(N);
            for (int i = 0; i < N; i++) {
                out.writeLong(this.mStepDurations[i]);
            }
        }
    }

    public static abstract class LongCounter {
        public abstract long getCountLocked(int i);

        public abstract void logState(Printer printer, String str);
    }

    public static abstract class LongCounterArray {
        public abstract long[] getCountsLocked(int i);

        public abstract void logState(Printer printer, String str);
    }

    public static final class PackageChange {
        public String mPackageName;
        public boolean mUpdate;
        public long mVersionCode;
    }

    public static abstract class Timer {
        public abstract int getCountLocked(int i);

        public abstract long getTimeSinceMarkLocked(long j);

        public abstract long getTotalTimeLocked(long j, int i);

        public abstract void logState(Printer printer, String str);

        public long getMaxDurationMsLocked(long elapsedRealtimeMs) {
            return -1;
        }

        public long getCurrentDurationMsLocked(long elapsedRealtimeMs) {
            return -1;
        }

        public long getTotalDurationMsLocked(long elapsedRealtimeMs) {
            return -1;
        }

        public Timer getSubTimer() {
            return null;
        }

        public boolean isRunningLocked() {
            return false;
        }
    }

    static final class TimerEntry {
        final int mId;
        final String mName;
        final long mTime;
        final Timer mTimer;

        TimerEntry(String name, int id, Timer timer, long time) {
            this.mName = name;
            this.mId = id;
            this.mTimer = timer;
            this.mTime = time;
        }
    }

    public static abstract class Uid {
        public static final int[] CRITICAL_PROC_STATES = new int[]{0, 1, 2};
        public static final int NUM_PROCESS_STATE = 7;
        public static final int NUM_USER_ACTIVITY_TYPES = 4;
        public static final int NUM_WIFI_BATCHED_SCAN_BINS = 5;
        public static final int PROCESS_STATE_BACKGROUND = 3;
        public static final int PROCESS_STATE_CACHED = 6;
        public static final int PROCESS_STATE_FOREGROUND = 2;
        public static final int PROCESS_STATE_FOREGROUND_SERVICE = 1;
        public static final int PROCESS_STATE_HEAVY_WEIGHT = 5;
        static final String[] PROCESS_STATE_NAMES = new String[]{"Top", "Fg Service", "Foreground", "Background", "Top Sleeping", "Heavy Weight", "Cached"};
        public static final int PROCESS_STATE_TOP = 0;
        public static final int PROCESS_STATE_TOP_SLEEPING = 4;
        @VisibleForTesting
        public static final String[] UID_PROCESS_TYPES = new String[]{HwSysResource.BIGDATA_SU_TOL, "FS", "F", "B", "TS", "HW", "C"};
        static final String[] USER_ACTIVITY_TYPES = new String[]{"other", "button", "touch", "accessibility"};

        public class Pid {
            public int mWakeNesting;
            public long mWakeStartMs;
            public long mWakeSumMs;
        }

        public static abstract class Pkg {

            public static abstract class Serv {
                public abstract int getLaunches(int i);

                public abstract long getStartTime(long j, int i);

                public abstract int getStarts(int i);
            }

            public abstract ArrayMap<String, ? extends Serv> getServiceStats();

            public abstract ArrayMap<String, ? extends Counter> getWakeupAlarmStats();
        }

        public static abstract class Proc {

            public static class ExcessivePower {
                public static final int TYPE_CPU = 2;
                public static final int TYPE_WAKE = 1;
                public long overTime;
                public int type;
                public long usedTime;
            }

            public abstract int countExcessivePowers();

            public abstract ExcessivePower getExcessivePower(int i);

            public abstract long getForegroundTime(int i);

            public abstract int getNumAnrs(int i);

            public abstract int getNumCrashes(int i);

            public abstract int getStarts(int i);

            public abstract long getSystemTime(int i);

            public abstract long getUserTime(int i);

            public abstract boolean isActive();
        }

        public static abstract class Sensor {
            public static final int GPS = -10000;

            public abstract int getHandle();

            public abstract Timer getSensorBackgroundTime();

            public abstract Timer getSensorTime();
        }

        public static abstract class Wakelock {
            public abstract Timer getWakeTime(int i);
        }

        public abstract Timer getAggregatedPartialWakelockTimer();

        public abstract Timer getAudioTurnedOnTimer();

        public abstract ControllerActivityCounter getBluetoothControllerActivity();

        public abstract Timer getBluetoothScanBackgroundTimer();

        public abstract Counter getBluetoothScanResultBgCounter();

        public abstract Counter getBluetoothScanResultCounter();

        public abstract Timer getBluetoothScanTimer();

        public abstract Timer getBluetoothUnoptimizedScanBackgroundTimer();

        public abstract Timer getBluetoothUnoptimizedScanTimer();

        public abstract Timer getCameraTurnedOnTimer();

        public abstract long getCpuActiveTime();

        public abstract long[] getCpuClusterTimes();

        public abstract long[] getCpuFreqTimes(int i);

        public abstract long[] getCpuFreqTimes(int i, int i2);

        public abstract void getDeferredJobsCheckinLineLocked(StringBuilder stringBuilder, int i);

        public abstract void getDeferredJobsLineLocked(StringBuilder stringBuilder, int i);

        public abstract Timer getFlashlightTurnedOnTimer();

        public abstract Timer getForegroundActivityTimer();

        public abstract Timer getForegroundServiceTimer();

        public abstract long getFullWifiLockTime(long j, int i);

        public abstract ArrayMap<String, SparseIntArray> getJobCompletionStats();

        public abstract ArrayMap<String, ? extends Timer> getJobStats();

        public abstract int getMobileRadioActiveCount(int i);

        public abstract long getMobileRadioActiveTime(int i);

        public abstract long getMobileRadioApWakeupCount(int i);

        public abstract ControllerActivityCounter getModemControllerActivity();

        public abstract Timer getMulticastWakelockStats();

        public abstract long getNetworkActivityBytes(int i, int i2);

        public abstract long getNetworkActivityPackets(int i, int i2);

        public abstract ArrayMap<String, ? extends Pkg> getPackageStats();

        public abstract SparseArray<? extends Pid> getPidStats();

        public abstract long getProcessStateTime(int i, long j, int i2);

        public abstract Timer getProcessStateTimer(int i);

        public abstract ArrayMap<String, ? extends Proc> getProcessStats();

        public abstract long[] getScreenOffCpuFreqTimes(int i);

        public abstract long[] getScreenOffCpuFreqTimes(int i, int i2);

        public abstract SparseArray<? extends Sensor> getSensorStats();

        public abstract ArrayMap<String, ? extends Timer> getSyncStats();

        public abstract long getSystemCpuTimeUs(int i);

        public abstract long getTimeAtCpuSpeed(int i, int i2, int i3);

        public abstract int getUid();

        public abstract int getUserActivityCount(int i, int i2);

        public abstract long getUserCpuTimeUs(int i);

        public abstract Timer getVibratorOnTimer();

        public abstract Timer getVideoTurnedOnTimer();

        public abstract ArrayMap<String, ? extends Wakelock> getWakelockStats();

        public abstract int getWifiBatchedScanCount(int i, int i2);

        public abstract long getWifiBatchedScanTime(int i, long j, int i2);

        public abstract ControllerActivityCounter getWifiControllerActivity();

        public abstract long getWifiMulticastTime(long j, int i);

        public abstract long getWifiRadioApWakeupCount(int i);

        public abstract long getWifiRunningTime(long j, int i);

        public abstract long getWifiScanActualTime(long j);

        public abstract int getWifiScanBackgroundCount(int i);

        public abstract long getWifiScanBackgroundTime(long j);

        public abstract Timer getWifiScanBackgroundTimer();

        public abstract int getWifiScanCount(int i);

        public abstract long getWifiScanTime(long j, int i);

        public abstract Timer getWifiScanTimer();

        public abstract boolean hasNetworkActivity();

        public abstract boolean hasUserActivity();

        public abstract void noteActivityPausedLocked(long j);

        public abstract void noteActivityResumedLocked(long j);

        public abstract void noteFullWifiLockAcquiredLocked(long j);

        public abstract void noteFullWifiLockReleasedLocked(long j);

        public abstract void noteUserActivityLocked(int i);

        public abstract void noteWifiBatchedScanStartedLocked(int i, long j);

        public abstract void noteWifiBatchedScanStoppedLocked(long j);

        public abstract void noteWifiMulticastDisabledLocked(long j);

        public abstract void noteWifiMulticastEnabledLocked(long j);

        public abstract void noteWifiRunningLocked(long j);

        public abstract void noteWifiScanStartedLocked(long j);

        public abstract void noteWifiScanStoppedLocked(long j);

        public abstract void noteWifiStoppedLocked(long j);
    }

    public static final class HistoryItem implements Parcelable {
        public static final byte CMD_CURRENT_TIME = (byte) 5;
        public static final byte CMD_NULL = (byte) -1;
        public static final byte CMD_OVERFLOW = (byte) 6;
        public static final byte CMD_RESET = (byte) 7;
        public static final byte CMD_SHUTDOWN = (byte) 8;
        public static final byte CMD_START = (byte) 4;
        public static final byte CMD_UPDATE = (byte) 0;
        public static final int EVENT_ACTIVE = 10;
        public static final int EVENT_ALARM = 13;
        public static final int EVENT_ALARM_FINISH = 16397;
        public static final int EVENT_ALARM_START = 32781;
        public static final int EVENT_COLLECT_EXTERNAL_STATS = 14;
        public static final int EVENT_CONNECTIVITY_CHANGED = 9;
        public static final int EVENT_COUNT = 22;
        public static final int EVENT_FLAG_FINISH = 16384;
        public static final int EVENT_FLAG_START = 32768;
        public static final int EVENT_FOREGROUND = 2;
        public static final int EVENT_FOREGROUND_FINISH = 16386;
        public static final int EVENT_FOREGROUND_START = 32770;
        public static final int EVENT_JOB = 6;
        public static final int EVENT_JOB_FINISH = 16390;
        public static final int EVENT_JOB_START = 32774;
        public static final int EVENT_LONG_WAKE_LOCK = 20;
        public static final int EVENT_LONG_WAKE_LOCK_FINISH = 16404;
        public static final int EVENT_LONG_WAKE_LOCK_START = 32788;
        public static final int EVENT_NONE = 0;
        public static final int EVENT_PACKAGE_ACTIVE = 16;
        public static final int EVENT_PACKAGE_INACTIVE = 15;
        public static final int EVENT_PACKAGE_INSTALLED = 11;
        public static final int EVENT_PACKAGE_UNINSTALLED = 12;
        public static final int EVENT_PROC = 1;
        public static final int EVENT_PROC_FINISH = 16385;
        public static final int EVENT_PROC_START = 32769;
        public static final int EVENT_SCREEN_WAKE_UP = 18;
        public static final int EVENT_SYNC = 4;
        public static final int EVENT_SYNC_FINISH = 16388;
        public static final int EVENT_SYNC_START = 32772;
        public static final int EVENT_TEMP_WHITELIST = 17;
        public static final int EVENT_TEMP_WHITELIST_FINISH = 16401;
        public static final int EVENT_TEMP_WHITELIST_START = 32785;
        public static final int EVENT_TOP = 3;
        public static final int EVENT_TOP_FINISH = 16387;
        public static final int EVENT_TOP_START = 32771;
        public static final int EVENT_TYPE_MASK = -49153;
        public static final int EVENT_USER_FOREGROUND = 8;
        public static final int EVENT_USER_FOREGROUND_FINISH = 16392;
        public static final int EVENT_USER_FOREGROUND_START = 32776;
        public static final int EVENT_USER_RUNNING = 7;
        public static final int EVENT_USER_RUNNING_FINISH = 16391;
        public static final int EVENT_USER_RUNNING_START = 32775;
        public static final int EVENT_WAKEUP_AP = 19;
        public static final int EVENT_WAKE_LOCK = 5;
        public static final int EVENT_WAKE_LOCK_FINISH = 16389;
        public static final int EVENT_WAKE_LOCK_START = 32773;
        public static final int MOST_INTERESTING_STATES = 1835008;
        public static final int MOST_INTERESTING_STATES2 = -1749024768;
        public static final int SETTLE_TO_ZERO_STATES = -1900544;
        public static final int SETTLE_TO_ZERO_STATES2 = 1748959232;
        public static final int STATE2_BLUETOOTH_ON_FLAG = 4194304;
        public static final int STATE2_BLUETOOTH_SCAN_FLAG = 1048576;
        public static final int STATE2_CAMERA_FLAG = 2097152;
        public static final int STATE2_CELLULAR_HIGH_TX_POWER_FLAG = 524288;
        public static final int STATE2_CHARGING_FLAG = 16777216;
        public static final int STATE2_DEVICE_IDLE_MASK = 100663296;
        public static final int STATE2_DEVICE_IDLE_SHIFT = 25;
        public static final int STATE2_FLASHLIGHT_FLAG = 134217728;
        public static final int STATE2_GPS_SIGNAL_QUALITY_MASK = 128;
        public static final int STATE2_GPS_SIGNAL_QUALITY_SHIFT = 7;
        public static final int STATE2_PHONE_IN_CALL_FLAG = 8388608;
        public static final int STATE2_POWER_SAVE_FLAG = Integer.MIN_VALUE;
        public static final int STATE2_USB_DATA_LINK_FLAG = 262144;
        public static final int STATE2_VIDEO_ON_FLAG = 1073741824;
        public static final int STATE2_WIFI_ON_FLAG = 268435456;
        public static final int STATE2_WIFI_RUNNING_FLAG = 536870912;
        public static final int STATE2_WIFI_SIGNAL_STRENGTH_MASK = 112;
        public static final int STATE2_WIFI_SIGNAL_STRENGTH_SHIFT = 4;
        public static final int STATE2_WIFI_SUPPL_STATE_MASK = 15;
        public static final int STATE2_WIFI_SUPPL_STATE_SHIFT = 0;
        public static final int STATE_AUDIO_ON_FLAG = 4194304;
        public static final int STATE_BATTERY_PLUGGED_FLAG = 524288;
        public static final int STATE_BRIGHTNESS_MASK = 7;
        public static final int STATE_BRIGHTNESS_SHIFT = 0;
        public static final int STATE_CPU_RUNNING_FLAG = Integer.MIN_VALUE;
        public static final int STATE_DATA_CONNECTION_MASK = 15872;
        public static final int STATE_DATA_CONNECTION_SHIFT = 9;
        public static final int STATE_GPS_ON_FLAG = 536870912;
        public static final int STATE_MOBILE_RADIO_ACTIVE_FLAG = 33554432;
        public static final int STATE_PHONE_SCANNING_FLAG = 2097152;
        public static final int STATE_PHONE_SIGNAL_STRENGTH_MASK = 56;
        public static final int STATE_PHONE_SIGNAL_STRENGTH_SHIFT = 3;
        public static final int STATE_PHONE_STATE_MASK = 448;
        public static final int STATE_PHONE_STATE_SHIFT = 6;
        private static final int STATE_RESERVED_0 = 16777216;
        public static final int STATE_SCREEN_DOZE_FLAG = 262144;
        public static final int STATE_SCREEN_ON_FLAG = 1048576;
        public static final int STATE_SENSOR_ON_FLAG = 8388608;
        public static final int STATE_WAKE_LOCK_FLAG = 1073741824;
        public static final int STATE_WIFI_FULL_LOCK_FLAG = 268435456;
        public static final int STATE_WIFI_MULTICAST_ON_FLAG = 65536;
        public static final int STATE_WIFI_RADIO_ACTIVE_FLAG = 67108864;
        public static final int STATE_WIFI_SCAN_FLAG = 134217728;
        public int batteryChargeUAh;
        public byte batteryHealth;
        public byte batteryLevel;
        public byte batteryPlugType;
        public byte batteryStatus;
        public short batteryTemperature;
        public char batteryVoltage;
        public byte cmd = (byte) -1;
        public long currentTime;
        public int eventCode;
        public HistoryTag eventTag;
        public final HistoryTag localEventTag = new HistoryTag();
        public final HistoryTag localWakeReasonTag = new HistoryTag();
        public final HistoryTag localWakelockTag = new HistoryTag();
        public HistoryItem next;
        public int numReadInts;
        public int states;
        public int states2;
        public HistoryStepDetails stepDetails;
        public long time;
        public HistoryTag wakeReasonTag;
        public HistoryTag wakelockTag;

        public boolean isDeltaData() {
            return this.cmd == (byte) 0;
        }

        public HistoryItem(long time, Parcel src) {
            this.time = time;
            this.numReadInts = 2;
            readFromParcel(src);
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(this.time);
            int i = 0;
            int bat = ((((((this.cmd & 255) | ((this.batteryLevel << 8) & 65280)) | ((this.batteryStatus << 16) & SurfaceControl.FX_SURFACE_MASK)) | ((this.batteryHealth << 20) & 15728640)) | ((this.batteryPlugType << 24) & 251658240)) | (this.wakelockTag != null ? 268435456 : 0)) | (this.wakeReasonTag != null ? 536870912 : 0);
            if (this.eventCode != 0) {
                i = 1073741824;
            }
            dest.writeInt(bat | i);
            dest.writeInt((this.batteryTemperature & 65535) | ((this.batteryVoltage << 16) & Menu.CATEGORY_MASK));
            dest.writeInt(this.batteryChargeUAh);
            dest.writeInt(this.states);
            dest.writeInt(this.states2);
            if (this.wakelockTag != null) {
                this.wakelockTag.writeToParcel(dest, flags);
            }
            if (this.wakeReasonTag != null) {
                this.wakeReasonTag.writeToParcel(dest, flags);
            }
            if (this.eventCode != 0) {
                dest.writeInt(this.eventCode);
                this.eventTag.writeToParcel(dest, flags);
            }
            if (this.cmd == (byte) 5 || this.cmd == (byte) 7) {
                dest.writeLong(this.currentTime);
            }
        }

        public void readFromParcel(Parcel src) {
            int start = src.dataPosition();
            int bat = src.readInt();
            this.cmd = (byte) (bat & 255);
            this.batteryLevel = (byte) ((bat >> 8) & 255);
            this.batteryStatus = (byte) ((bat >> 16) & 15);
            this.batteryHealth = (byte) ((bat >> 20) & 15);
            this.batteryPlugType = (byte) ((bat >> 24) & 15);
            int bat2 = src.readInt();
            this.batteryTemperature = (short) (bat2 & 65535);
            this.batteryVoltage = (char) (65535 & (bat2 >> 16));
            this.batteryChargeUAh = src.readInt();
            this.states = src.readInt();
            this.states2 = src.readInt();
            if ((268435456 & bat) != 0) {
                this.wakelockTag = this.localWakelockTag;
                this.wakelockTag.readFromParcel(src);
            } else {
                this.wakelockTag = null;
            }
            if ((536870912 & bat) != 0) {
                this.wakeReasonTag = this.localWakeReasonTag;
                this.wakeReasonTag.readFromParcel(src);
            } else {
                this.wakeReasonTag = null;
            }
            if ((1073741824 & bat) != 0) {
                this.eventCode = src.readInt();
                this.eventTag = this.localEventTag;
                this.eventTag.readFromParcel(src);
            } else {
                this.eventCode = 0;
                this.eventTag = null;
            }
            if (this.cmd == (byte) 5 || this.cmd == (byte) 7) {
                this.currentTime = src.readLong();
            } else {
                this.currentTime = 0;
            }
            this.numReadInts += (src.dataPosition() - start) / 4;
        }

        public void clear() {
            this.time = 0;
            this.cmd = (byte) -1;
            this.batteryLevel = (byte) 0;
            this.batteryStatus = (byte) 0;
            this.batteryHealth = (byte) 0;
            this.batteryPlugType = (byte) 0;
            this.batteryTemperature = (short) 0;
            this.batteryVoltage = 0;
            this.batteryChargeUAh = 0;
            this.states = 0;
            this.states2 = 0;
            this.wakelockTag = null;
            this.wakeReasonTag = null;
            this.eventCode = 0;
            this.eventTag = null;
        }

        public void setTo(HistoryItem o) {
            this.time = o.time;
            this.cmd = o.cmd;
            setToCommon(o);
        }

        public void setTo(long time, byte cmd, HistoryItem o) {
            this.time = time;
            this.cmd = cmd;
            setToCommon(o);
        }

        private void setToCommon(HistoryItem o) {
            this.batteryLevel = o.batteryLevel;
            this.batteryStatus = o.batteryStatus;
            this.batteryHealth = o.batteryHealth;
            this.batteryPlugType = o.batteryPlugType;
            this.batteryTemperature = o.batteryTemperature;
            this.batteryVoltage = o.batteryVoltage;
            this.batteryChargeUAh = o.batteryChargeUAh;
            this.states = o.states;
            this.states2 = o.states2;
            if (o.wakelockTag != null) {
                this.wakelockTag = this.localWakelockTag;
                this.wakelockTag.setTo(o.wakelockTag);
            } else {
                this.wakelockTag = null;
            }
            if (o.wakeReasonTag != null) {
                this.wakeReasonTag = this.localWakeReasonTag;
                this.wakeReasonTag.setTo(o.wakeReasonTag);
            } else {
                this.wakeReasonTag = null;
            }
            this.eventCode = o.eventCode;
            if (o.eventTag != null) {
                this.eventTag = this.localEventTag;
                this.eventTag.setTo(o.eventTag);
            } else {
                this.eventTag = null;
            }
            this.currentTime = o.currentTime;
        }

        public boolean sameNonEvent(HistoryItem o) {
            return this.batteryLevel == o.batteryLevel && this.batteryStatus == o.batteryStatus && this.batteryHealth == o.batteryHealth && this.batteryPlugType == o.batteryPlugType && this.batteryTemperature == o.batteryTemperature && this.batteryVoltage == o.batteryVoltage && this.batteryChargeUAh == o.batteryChargeUAh && this.states == o.states && this.states2 == o.states2 && this.currentTime == o.currentTime;
        }

        /* JADX WARNING: Missing block: B:13:0x0028, code skipped:
            return false;
     */
        /* JADX WARNING: Missing block: B:23:0x0043, code skipped:
            return false;
     */
        /* JADX WARNING: Missing block: B:33:0x005e, code skipped:
            return false;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean same(HistoryItem o) {
            if (!sameNonEvent(o) || this.eventCode != o.eventCode) {
                return false;
            }
            if (this.wakelockTag != o.wakelockTag && (this.wakelockTag == null || o.wakelockTag == null || !this.wakelockTag.equals(o.wakelockTag))) {
                return false;
            }
            if (this.wakeReasonTag != o.wakeReasonTag && (this.wakeReasonTag == null || o.wakeReasonTag == null || !this.wakeReasonTag.equals(o.wakeReasonTag))) {
                return false;
            }
            if (this.eventTag == o.eventTag || (this.eventTag != null && o.eventTag != null && this.eventTag.equals(o.eventTag))) {
                return true;
            }
            return false;
        }
    }

    public abstract void commitCurrentHistoryBatchLocked();

    public abstract long computeBatteryRealtime(long j, int i);

    public abstract long computeBatteryScreenOffRealtime(long j, int i);

    public abstract long computeBatteryScreenOffUptime(long j, int i);

    public abstract long computeBatteryTimeRemaining(long j);

    public abstract long computeBatteryUptime(long j, int i);

    public abstract long computeChargeTimeRemaining(long j);

    public abstract long computeRealtime(long j, int i);

    public abstract long computeUptime(long j, int i);

    public abstract void finishIteratingHistoryLocked();

    public abstract void finishIteratingOldHistoryLocked();

    public abstract long getBatteryRealtime(long j);

    public abstract long getBatteryUptime(long j);

    public abstract ControllerActivityCounter getBluetoothControllerActivity();

    public abstract long getBluetoothScanTime(long j, int i);

    public abstract long getCameraOnTime(long j, int i);

    public abstract LevelStepTracker getChargeLevelStepTracker();

    public abstract long[] getCpuFreqs();

    public abstract long getCurrentDailyStartTime();

    public abstract LevelStepTracker getDailyChargeLevelStepTracker();

    public abstract LevelStepTracker getDailyDischargeLevelStepTracker();

    public abstract DailyItem getDailyItemLocked(int i);

    public abstract ArrayList<PackageChange> getDailyPackageChanges();

    public abstract int getDeviceIdleModeCount(int i, int i2);

    public abstract long getDeviceIdleModeTime(int i, long j, int i2);

    public abstract int getDeviceIdlingCount(int i, int i2);

    public abstract long getDeviceIdlingTime(int i, long j, int i2);

    public abstract int getDischargeAmount(int i);

    public abstract int getDischargeAmountScreenDoze();

    public abstract int getDischargeAmountScreenDozeSinceCharge();

    public abstract int getDischargeAmountScreenOff();

    public abstract int getDischargeAmountScreenOffSinceCharge();

    public abstract int getDischargeAmountScreenOn();

    public abstract int getDischargeAmountScreenOnSinceCharge();

    public abstract int getDischargeCurrentLevel();

    public abstract LevelStepTracker getDischargeLevelStepTracker();

    public abstract int getDischargeStartLevel();

    public abstract String getEndPlatformVersion();

    public abstract int getEstimatedBatteryCapacity();

    public abstract long getFlashlightOnCount(int i);

    public abstract long getFlashlightOnTime(long j, int i);

    public abstract long getGlobalWifiRunningTime(long j, int i);

    public abstract long getGpsBatteryDrainMaMs();

    public abstract long getGpsSignalQualityTime(int i, long j, int i2);

    public abstract int getHighDischargeAmountSinceCharge();

    public abstract long getHistoryBaseTime();

    public abstract int getHistoryStringPoolBytes();

    public abstract int getHistoryStringPoolSize();

    public abstract String getHistoryTagPoolString(int i);

    public abstract int getHistoryTagPoolUid(int i);

    public abstract int getHistoryTotalSize();

    public abstract int getHistoryUsedSize();

    public abstract long getInteractiveTime(long j, int i);

    public abstract boolean getIsOnBattery();

    public abstract LongSparseArray<? extends Timer> getKernelMemoryStats();

    public abstract Map<String, ? extends Timer> getKernelWakelockStats();

    public abstract long getLongestDeviceIdleModeTime(int i);

    public abstract int getLowDischargeAmountSinceCharge();

    public abstract int getMaxLearnedBatteryCapacity();

    public abstract int getMinLearnedBatteryCapacity();

    public abstract long getMobileRadioActiveAdjustedTime(int i);

    public abstract int getMobileRadioActiveCount(int i);

    public abstract long getMobileRadioActiveTime(long j, int i);

    public abstract int getMobileRadioActiveUnknownCount(int i);

    public abstract long getMobileRadioActiveUnknownTime(int i);

    public abstract ControllerActivityCounter getModemControllerActivity();

    public abstract long getNetworkActivityBytes(int i, int i2);

    public abstract long getNetworkActivityPackets(int i, int i2);

    public abstract boolean getNextHistoryLocked(HistoryItem historyItem);

    public abstract long getNextMaxDailyDeadline();

    public abstract long getNextMinDailyDeadline();

    public abstract boolean getNextOldHistoryLocked(HistoryItem historyItem);

    public abstract int getNumConnectivityChange(int i);

    public abstract int getParcelVersion();

    public abstract int getPhoneDataConnectionCount(int i, int i2);

    public abstract long getPhoneDataConnectionTime(int i, long j, int i2);

    public abstract Timer getPhoneDataConnectionTimer(int i);

    public abstract int getPhoneOnCount(int i);

    public abstract long getPhoneOnTime(long j, int i);

    public abstract long getPhoneSignalScanningTime(long j, int i);

    public abstract Timer getPhoneSignalScanningTimer();

    public abstract int getPhoneSignalStrengthCount(int i, int i2);

    public abstract long getPhoneSignalStrengthTime(int i, long j, int i2);

    protected abstract Timer getPhoneSignalStrengthTimer(int i);

    public abstract int getPowerSaveModeEnabledCount(int i);

    public abstract long getPowerSaveModeEnabledTime(long j, int i);

    public abstract Map<String, ? extends Timer> getRpmStats();

    public abstract long getScreenBrightnessTime(int i, long j, int i2);

    public abstract Timer getScreenBrightnessTimer(int i);

    public abstract int getScreenDozeCount(int i);

    public abstract long getScreenDozeTime(long j, int i);

    public abstract Map<String, ? extends Timer> getScreenOffRpmStats();

    public abstract int getScreenOnCount(int i);

    public abstract long getScreenOnTime(long j, int i);

    public abstract long getStartClockTime();

    public abstract int getStartCount();

    public abstract String getStartPlatformVersion();

    public abstract long getUahDischarge(int i);

    public abstract long getUahDischargeDeepDoze(int i);

    public abstract long getUahDischargeLightDoze(int i);

    public abstract long getUahDischargeScreenDoze(int i);

    public abstract long getUahDischargeScreenOff(int i);

    public abstract SparseArray<? extends Uid> getUidStats();

    public abstract Map<String, ? extends Timer> getWakeupReasonStats();

    public abstract long getWifiActiveTime(long j, int i);

    public abstract ControllerActivityCounter getWifiControllerActivity();

    public abstract int getWifiMulticastWakelockCount(int i);

    public abstract long getWifiMulticastWakelockTime(long j, int i);

    public abstract long getWifiOnTime(long j, int i);

    public abstract int getWifiSignalStrengthCount(int i, int i2);

    public abstract long getWifiSignalStrengthTime(int i, long j, int i2);

    public abstract Timer getWifiSignalStrengthTimer(int i);

    public abstract int getWifiStateCount(int i, int i2);

    public abstract long getWifiStateTime(int i, long j, int i2);

    public abstract Timer getWifiStateTimer(int i);

    public abstract int getWifiSupplStateCount(int i, int i2);

    public abstract long getWifiSupplStateTime(int i, long j, int i2);

    public abstract Timer getWifiSupplStateTimer(int i);

    public abstract boolean hasBluetoothActivityReporting();

    public abstract boolean hasModemActivityReporting();

    public abstract boolean hasWifiActivityReporting();

    public abstract boolean startIteratingHistoryLocked();

    public abstract boolean startIteratingOldHistoryLocked();

    public abstract void writeToParcelWithoutUids(Parcel parcel, int i);

    public static int mapToInternalProcessState(int procState) {
        if (procState == 19) {
            return 19;
        }
        if (procState == 2) {
            return 0;
        }
        if (procState == 3) {
            return 1;
        }
        if (procState <= 5) {
            return 2;
        }
        if (procState <= 10) {
            return 3;
        }
        if (procState <= 11) {
            return 4;
        }
        if (procState <= 12) {
            return 5;
        }
        return 6;
    }

    private static final void formatTimeRaw(StringBuilder out, long seconds) {
        long days = seconds / 86400;
        if (days != 0) {
            out.append(days);
            out.append("d ");
        }
        long used = ((days * 60) * 60) * 24;
        long hours = (seconds - used) / 3600;
        if (!(hours == 0 && used == 0)) {
            out.append(hours);
            out.append("h ");
        }
        used += (hours * 60) * 60;
        long mins = (seconds - used) / 60;
        if (!(mins == 0 && used == 0)) {
            out.append(mins);
            out.append("m ");
        }
        used += 60 * mins;
        if (seconds != 0 || used != 0) {
            out.append(seconds - used);
            out.append("s ");
        }
    }

    public static final void formatTimeMs(StringBuilder sb, long time) {
        long sec = time / 1000;
        formatTimeRaw(sb, sec);
        sb.append(time - (1000 * sec));
        sb.append("ms ");
    }

    public static final void formatTimeMsNoSpace(StringBuilder sb, long time) {
        long sec = time / 1000;
        formatTimeRaw(sb, sec);
        sb.append(time - (1000 * sec));
        sb.append("ms");
    }

    public final String formatRatioLocked(long num, long den) {
        if (den == 0) {
            return "--%";
        }
        float perc = (((float) num) / ((float) den)) * 100.0f;
        this.mFormatBuilder.setLength(0);
        this.mFormatter.format("%.1f%%", new Object[]{Float.valueOf(perc)});
        return this.mFormatBuilder.toString();
    }

    final String formatBytesLocked(long bytes) {
        this.mFormatBuilder.setLength(0);
        if (bytes < 1024) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(bytes);
            stringBuilder.append("B");
            return stringBuilder.toString();
        } else if (bytes < 1048576) {
            this.mFormatter.format("%.2fKB", new Object[]{Double.valueOf(((double) bytes) / 1024.0d)});
            return this.mFormatBuilder.toString();
        } else if (bytes < BYTES_PER_GB) {
            this.mFormatter.format("%.2fMB", new Object[]{Double.valueOf(((double) bytes) / 1048576.0d)});
            return this.mFormatBuilder.toString();
        } else {
            this.mFormatter.format("%.2fGB", new Object[]{Double.valueOf(((double) bytes) / 1.073741824E9d)});
            return this.mFormatBuilder.toString();
        }
    }

    private static long roundUsToMs(long timeUs) {
        return (500 + timeUs) / 1000;
    }

    private static long computeWakeLock(Timer timer, long elapsedRealtimeUs, int which) {
        if (timer != null) {
            return (500 + timer.getTotalTimeLocked(elapsedRealtimeUs, which)) / 1000;
        }
        return 0;
    }

    private static final String printWakeLock(StringBuilder sb, Timer timer, long elapsedRealtimeUs, String name, int which, String linePrefix) {
        StringBuilder stringBuilder = sb;
        Timer timer2 = timer;
        long j = elapsedRealtimeUs;
        String str = name;
        int i = which;
        String str2 = linePrefix;
        if (timer2 != null) {
            long totalTimeMillis = computeWakeLock(timer2, j, i);
            int count = timer2.getCountLocked(i);
            if (totalTimeMillis != 0) {
                stringBuilder.append(str2);
                formatTimeMs(stringBuilder, totalTimeMillis);
                if (str != null) {
                    stringBuilder.append(str);
                    stringBuilder.append(' ');
                }
                stringBuilder.append('(');
                stringBuilder.append(count);
                stringBuilder.append(" times)");
                long maxDurationMs = timer2.getMaxDurationMsLocked(j / 1000);
                if (maxDurationMs >= 0) {
                    stringBuilder.append(" max=");
                    stringBuilder.append(maxDurationMs);
                }
                long totalDurMs = timer2.getTotalDurationMsLocked(j / 1000);
                if (totalDurMs > totalTimeMillis) {
                    stringBuilder.append(" actual=");
                    stringBuilder.append(totalDurMs);
                }
                if (timer.isRunningLocked()) {
                    long currentMs = timer2.getCurrentDurationMsLocked(j / 1000);
                    if (currentMs >= 0) {
                        stringBuilder.append(" (running for ");
                        stringBuilder.append(currentMs);
                        stringBuilder.append("ms)");
                    } else {
                        stringBuilder.append(" (running)");
                    }
                }
                return ", ";
            }
        }
        return str2;
    }

    private static final boolean printTimer(PrintWriter pw, StringBuilder sb, Timer timer, long rawRealtimeUs, int which, String prefix, String type) {
        StringBuilder stringBuilder = sb;
        Timer timer2 = timer;
        PrintWriter printWriter;
        if (timer2 != null) {
            long totalTimeMs = (timer.getTotalTimeLocked(rawRealtimeUs, which) + 500) / 1000;
            int count = timer2.getCountLocked(which);
            if (totalTimeMs != 0) {
                stringBuilder.setLength(0);
                stringBuilder.append(prefix);
                stringBuilder.append("    ");
                stringBuilder.append(type);
                stringBuilder.append(": ");
                formatTimeMs(stringBuilder, totalTimeMs);
                stringBuilder.append("realtime (");
                stringBuilder.append(count);
                stringBuilder.append(" times)");
                long maxDurationMs = timer2.getMaxDurationMsLocked(rawRealtimeUs / 1000);
                if (maxDurationMs >= 0) {
                    stringBuilder.append(" max=");
                    stringBuilder.append(maxDurationMs);
                }
                if (timer.isRunningLocked()) {
                    long currentMs = timer2.getCurrentDurationMsLocked(rawRealtimeUs / 1000);
                    if (currentMs >= 0) {
                        stringBuilder.append(" (running for ");
                        stringBuilder.append(currentMs);
                        stringBuilder.append("ms)");
                    } else {
                        stringBuilder.append(" (running)");
                    }
                }
                pw.println(sb.toString());
                return true;
            }
            printWriter = pw;
        } else {
            printWriter = pw;
            int i = which;
        }
        String str = prefix;
        String str2 = type;
        return false;
    }

    private static final String printWakeLockCheckin(StringBuilder sb, Timer timer, long elapsedRealtimeUs, String name, int which, String linePrefix) {
        long totalTimeMicros;
        String stringBuilder;
        StringBuilder stringBuilder2 = sb;
        Timer timer2 = timer;
        long j = elapsedRealtimeUs;
        String str = name;
        int i = which;
        int count = 0;
        long max = 0;
        long current = 0;
        long totalDuration = 0;
        if (timer2 != null) {
            long totalTimeMicros2 = timer2.getTotalTimeLocked(j, i);
            count = timer2.getCountLocked(i);
            totalTimeMicros = totalTimeMicros2;
            current = timer2.getCurrentDurationMsLocked(j / 1000);
            max = timer2.getMaxDurationMsLocked(j / 1000);
            totalDuration = timer2.getTotalDurationMsLocked(j / 1000);
        } else {
            totalTimeMicros = 0;
        }
        stringBuilder2.append(linePrefix);
        stringBuilder2.append((totalTimeMicros + 500) / 1000);
        stringBuilder2.append(',');
        if (str != null) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(str);
            stringBuilder3.append(",");
            stringBuilder = stringBuilder3.toString();
        } else {
            stringBuilder = "";
        }
        stringBuilder2.append(stringBuilder);
        stringBuilder2.append(count);
        stringBuilder2.append(',');
        stringBuilder2.append(current);
        stringBuilder2.append(',');
        stringBuilder2.append(max);
        if (str != null) {
            stringBuilder2.append(',');
            stringBuilder2.append(totalDuration);
        }
        return ",";
    }

    private static final void dumpLineHeader(PrintWriter pw, int uid, String category, String type) {
        pw.print(9);
        pw.print(',');
        pw.print(uid);
        pw.print(',');
        pw.print(category);
        pw.print(',');
        pw.print(type);
    }

    private static final void dumpLine(PrintWriter pw, int uid, String category, String type, Object... args) {
        dumpLineHeader(pw, uid, category, type);
        for (Object arg : args) {
            pw.print(',');
            pw.print(arg);
        }
        pw.println();
    }

    private static final void dumpTimer(PrintWriter pw, int uid, String category, String type, Timer timer, long rawRealtime, int which) {
        if (timer != null) {
            long totalTime = roundUsToMs(timer.getTotalTimeLocked(rawRealtime, which));
            int count = timer.getCountLocked(which);
            if (totalTime != 0 || count != 0) {
                dumpLine(pw, uid, category, type, Long.valueOf(totalTime), Integer.valueOf(count));
            }
        }
    }

    private static void dumpTimer(ProtoOutputStream proto, long fieldId, Timer timer, long rawRealtimeUs, int which) {
        ProtoOutputStream protoOutputStream = proto;
        Timer timer2 = timer;
        if (timer2 != null) {
            long timeMs = roundUsToMs(timer.getTotalTimeLocked(rawRealtimeUs, which));
            int count = timer2.getCountLocked(which);
            long maxDurationMs = timer2.getMaxDurationMsLocked(rawRealtimeUs / 1000);
            long curDurationMs = timer2.getCurrentDurationMsLocked(rawRealtimeUs / 1000);
            long totalDurationMs = timer2.getTotalDurationMsLocked(rawRealtimeUs / 1000);
            if (!(timeMs == 0 && count == 0 && maxDurationMs == -1 && curDurationMs == -1 && totalDurationMs == -1)) {
                long token = proto.start(fieldId);
                protoOutputStream.write(1112396529665L, timeMs);
                protoOutputStream.write(1112396529666L, count);
                if (maxDurationMs != -1) {
                    protoOutputStream.write(1112396529667L, maxDurationMs);
                }
                if (curDurationMs != -1) {
                    protoOutputStream.write(1112396529668L, curDurationMs);
                }
                if (totalDurationMs != -1) {
                    protoOutputStream.write(1112396529669L, totalDurationMs);
                }
                protoOutputStream.end(token);
            }
        }
    }

    private static boolean controllerActivityHasData(ControllerActivityCounter counter, int which) {
        if (counter == null) {
            return false;
        }
        if (counter.getIdleTimeCounter().getCountLocked(which) != 0 || counter.getRxTimeCounter().getCountLocked(which) != 0 || counter.getPowerCounter().getCountLocked(which) != 0) {
            return true;
        }
        for (LongCounter c : counter.getTxTimeCounters()) {
            if (c.getCountLocked(which) != 0) {
                return true;
            }
        }
        return false;
    }

    private static final void dumpControllerActivityLine(PrintWriter pw, int uid, String category, String type, ControllerActivityCounter counter, int which) {
        if (controllerActivityHasData(counter, which)) {
            dumpLineHeader(pw, uid, category, type);
            pw.print(",");
            pw.print(counter.getIdleTimeCounter().getCountLocked(which));
            pw.print(",");
            pw.print(counter.getRxTimeCounter().getCountLocked(which));
            pw.print(",");
            pw.print(counter.getPowerCounter().getCountLocked(which) / DateUtils.HOUR_IN_MILLIS);
            for (LongCounter c : counter.getTxTimeCounters()) {
                pw.print(",");
                pw.print(c.getCountLocked(which));
            }
            pw.println();
        }
    }

    private static void dumpControllerActivityProto(ProtoOutputStream proto, long fieldId, ControllerActivityCounter counter, int which) {
        if (controllerActivityHasData(counter, which)) {
            long cToken = proto.start(fieldId);
            proto.write(1112396529665L, counter.getIdleTimeCounter().getCountLocked(which));
            proto.write(1112396529666L, counter.getRxTimeCounter().getCountLocked(which));
            proto.write(1112396529667L, counter.getPowerCounter().getCountLocked(which) / DateUtils.HOUR_IN_MILLIS);
            LongCounter[] txCounters = counter.getTxTimeCounters();
            for (int i = 0; i < txCounters.length; i++) {
                LongCounter c = txCounters[i];
                long tToken = proto.start(2246267895812L);
                proto.write(1120986464257L, i);
                proto.write(1112396529666L, c.getCountLocked(which));
                proto.end(tToken);
            }
            proto.end(cToken);
        }
    }

    private final void printControllerActivityIfInteresting(PrintWriter pw, StringBuilder sb, String prefix, String controllerName, ControllerActivityCounter counter, int which) {
        if (controllerActivityHasData(counter, which)) {
            printControllerActivity(pw, sb, prefix, controllerName, counter, which);
        }
    }

    private final void printControllerActivity(PrintWriter pw, StringBuilder sb, String prefix, String controllerName, ControllerActivityCounter counter, int which) {
        int length;
        long scanTimeMs;
        String[] powerLevel;
        PrintWriter printWriter = pw;
        StringBuilder stringBuilder = sb;
        String str = controllerName;
        int i = which;
        long idleTimeMs = counter.getIdleTimeCounter().getCountLocked(i);
        long rxTimeMs = counter.getRxTimeCounter().getCountLocked(i);
        long powerDrainMaMs = counter.getPowerCounter().getCountLocked(i);
        long totalControllerActivityTimeMs = computeBatteryRealtime(SystemClock.elapsedRealtime() * 1000, i) / 1000;
        LongCounter[] txTimeCounters = counter.getTxTimeCounters();
        long totalTxTimeMs = 0;
        int i2 = 0;
        for (length = txTimeCounters.length; i2 < length; length = length) {
            totalTxTimeMs += txTimeCounters[i2].getCountLocked(i);
            i2++;
        }
        if (str.equals(WIFI_CONTROLLER_NAME)) {
            scanTimeMs = counter.getScanTimeCounter().getCountLocked(i);
            stringBuilder.setLength(0);
            sb.append(prefix);
            stringBuilder.append("     ");
            stringBuilder.append(str);
            stringBuilder.append(" Scan time:  ");
            formatTimeMs(stringBuilder, scanTimeMs);
            stringBuilder.append("(");
            stringBuilder.append(formatRatioLocked(scanTimeMs, totalControllerActivityTimeMs));
            stringBuilder.append(")");
            printWriter.println(sb.toString());
            scanTimeMs = totalControllerActivityTimeMs - ((idleTimeMs + rxTimeMs) + totalTxTimeMs);
            stringBuilder.setLength(0);
            sb.append(prefix);
            stringBuilder.append("     ");
            stringBuilder.append(str);
            stringBuilder.append(" Sleep time:  ");
            formatTimeMs(stringBuilder, scanTimeMs);
            stringBuilder.append("(");
            stringBuilder.append(formatRatioLocked(scanTimeMs, totalControllerActivityTimeMs));
            stringBuilder.append(")");
            printWriter.println(sb.toString());
        }
        if (str.equals(CELLULAR_CONTROLLER_NAME)) {
            scanTimeMs = counter.getSleepTimeCounter().getCountLocked(i);
            stringBuilder.setLength(0);
            sb.append(prefix);
            stringBuilder.append("     ");
            stringBuilder.append(str);
            stringBuilder.append(" Sleep time:  ");
            formatTimeMs(stringBuilder, scanTimeMs);
            stringBuilder.append("(");
            stringBuilder.append(formatRatioLocked(scanTimeMs, totalControllerActivityTimeMs));
            stringBuilder.append(")");
            printWriter.println(sb.toString());
        }
        stringBuilder.setLength(0);
        sb.append(prefix);
        stringBuilder.append("     ");
        stringBuilder.append(str);
        stringBuilder.append(" Idle time:   ");
        formatTimeMs(stringBuilder, idleTimeMs);
        stringBuilder.append("(");
        stringBuilder.append(formatRatioLocked(idleTimeMs, totalControllerActivityTimeMs));
        stringBuilder.append(")");
        printWriter.println(sb.toString());
        stringBuilder.setLength(0);
        sb.append(prefix);
        stringBuilder.append("     ");
        stringBuilder.append(str);
        stringBuilder.append(" Rx time:     ");
        formatTimeMs(stringBuilder, rxTimeMs);
        stringBuilder.append("(");
        stringBuilder.append(formatRatioLocked(rxTimeMs, totalControllerActivityTimeMs));
        stringBuilder.append(")");
        printWriter.println(sb.toString());
        stringBuilder.setLength(0);
        sb.append(prefix);
        stringBuilder.append("     ");
        stringBuilder.append(str);
        stringBuilder.append(" Tx time:     ");
        Object obj = -1;
        if (controllerName.hashCode() == -851952246 && str.equals(CELLULAR_CONTROLLER_NAME)) {
            obj = null;
        }
        if (obj != null) {
            powerLevel = new String[]{"[0]", "[1]", "[2]", "[3]", "[4]"};
        } else {
            powerLevel = new String[]{"   less than 0dBm: ", "   0dBm to 8dBm: ", "   8dBm to 15dBm: ", "   15dBm to 20dBm: ", "   above 20dBm: "};
        }
        int numTxLvls = Math.min(counter.getTxTimeCounters().length, powerLevel.length);
        if (numTxLvls > 1) {
            printWriter.println(sb.toString());
            length = 0;
            while (length < numTxLvls) {
                long txLvlTimeMs = counter.getTxTimeCounters()[length].getCountLocked(i);
                int numTxLvls2 = numTxLvls;
                stringBuilder.setLength(0);
                sb.append(prefix);
                stringBuilder.append("    ");
                stringBuilder.append(powerLevel[length]);
                stringBuilder.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                formatTimeMs(stringBuilder, txLvlTimeMs);
                stringBuilder.append("(");
                stringBuilder.append(formatRatioLocked(txLvlTimeMs, totalControllerActivityTimeMs));
                stringBuilder.append(")");
                printWriter.println(sb.toString());
                length++;
                numTxLvls = numTxLvls2;
            }
        } else {
            scanTimeMs = counter.getTxTimeCounters()[0].getCountLocked(i);
            formatTimeMs(stringBuilder, scanTimeMs);
            stringBuilder.append("(");
            stringBuilder.append(formatRatioLocked(scanTimeMs, totalControllerActivityTimeMs));
            stringBuilder.append(")");
            printWriter.println(sb.toString());
        }
        if (powerDrainMaMs > 0) {
            stringBuilder.setLength(0);
            sb.append(prefix);
            stringBuilder.append("     ");
            stringBuilder.append(str);
            stringBuilder.append(" Battery drain: ");
            stringBuilder.append(BatteryStatsHelper.makemAh(((double) powerDrainMaMs) / 3600000.0d));
            stringBuilder.append("mAh");
            printWriter.println(sb.toString());
        }
    }

    public final void dumpCheckinLocked(Context context, PrintWriter pw, int which, int reqUid) {
        dumpCheckinLocked(context, pw, which, reqUid, BatteryStatsHelper.checkWifiOnly(context));
    }

    /* JADX WARNING: Removed duplicated region for block: B:237:0x0e5f  */
    /* JADX WARNING: Removed duplicated region for block: B:201:0x0d1e  */
    /* JADX WARNING: Removed duplicated region for block: B:240:0x0e8f  */
    /* JADX WARNING: Removed duplicated region for block: B:256:0x0eea  */
    /* JADX WARNING: Removed duplicated region for block: B:251:0x0eb9  */
    /* JADX WARNING: Removed duplicated region for block: B:490:0x0fbd A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:260:0x0ef9  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public final void dumpCheckinLocked(Context context, PrintWriter pw, int which, int reqUid, boolean wifiOnly) {
        long batteryUptime;
        Integer valueOf;
        int i;
        int iw;
        Wakelock wl;
        int NU;
        long rawRealtimeMs;
        int multicastWakeLockCountTotal;
        Object[] args;
        int args2;
        Entry<String, ? extends Timer> ent;
        long batteryUptime2;
        StringBuilder stringBuilder;
        StringBuilder sb;
        Object[] objArr;
        long rawRealtimeMs2;
        Object[] objArr2;
        Map<String, ? extends Timer> screenOffRpmStats;
        List<BatterySipper> sippers;
        StringBuilder sb2;
        Object[] objArr3;
        PrintWriter printWriter = pw;
        int category = which;
        int i2 = reqUid;
        long rawUptime = SystemClock.uptimeMillis() * 1000;
        long rawRealtimeMs3 = SystemClock.elapsedRealtime();
        long rawRealtime = rawRealtimeMs3 * 1000;
        long batteryUptime3 = getBatteryUptime(rawUptime);
        long whichBatteryUptime = computeBatteryUptime(rawUptime, category);
        long whichBatteryRealtime = computeBatteryRealtime(rawRealtime, category);
        long whichBatteryScreenOffUptime = computeBatteryScreenOffUptime(rawUptime, category);
        long whichBatteryScreenOffRealtime = computeBatteryScreenOffRealtime(rawRealtime, category);
        long totalRealtime = computeRealtime(rawRealtime, category);
        long totalUptime = computeUptime(rawUptime, category);
        long screenOnTime = getScreenOnTime(rawRealtime, category);
        long screenDozeTime = getScreenDozeTime(rawRealtime, category);
        long interactiveTime = getInteractiveTime(rawRealtime, category);
        long powerSaveModeEnabledTime = getPowerSaveModeEnabledTime(rawRealtime, category);
        long deviceIdleModeLightTime = getDeviceIdleModeTime(1, rawRealtime, category);
        long deviceIdleModeFullTime = getDeviceIdleModeTime(2, rawRealtime, category);
        long deviceLightIdlingTime = getDeviceIdlingTime(1, rawRealtime, category);
        long deviceIdlingTime = getDeviceIdlingTime(2, rawRealtime, category);
        int connChanges = getNumConnectivityChange(category);
        long phoneOnTime = getPhoneOnTime(rawRealtime, category);
        long dischargeCount = getUahDischarge(category);
        long dischargeScreenOffCount = getUahDischargeScreenOff(category);
        long dischargeScreenDozeCount = getUahDischargeScreenDoze(category);
        long dischargeLightDozeCount = getUahDischargeLightDoze(category);
        long dischargeDeepDozeCount = getUahDischargeDeepDoze(category);
        StringBuilder sb3 = new StringBuilder(128);
        SparseArray<? extends Uid> uidStats = getUidStats();
        int NU2 = uidStats.size();
        String category2 = STAT_NAMES[category];
        String str = BATTERY_DATA;
        int connChanges2 = connChanges;
        Object[] objArr4 = new Object[12];
        if (category == 0) {
            batteryUptime = batteryUptime3;
            valueOf = Integer.valueOf(getStartCount());
        } else {
            batteryUptime = batteryUptime3;
            valueOf = "N/A";
        }
        objArr4[0] = valueOf;
        long rawRealtime2 = rawRealtime;
        objArr4[1] = Long.valueOf(whichBatteryRealtime / 1000);
        objArr4[2] = Long.valueOf(whichBatteryUptime / 1000);
        objArr4[3] = Long.valueOf(totalRealtime / 1000);
        objArr4[4] = Long.valueOf(totalUptime / 1000);
        objArr4[5] = Long.valueOf(getStartClockTime());
        objArr4[6] = Long.valueOf(whichBatteryScreenOffRealtime / 1000);
        objArr4[7] = Long.valueOf(whichBatteryScreenOffUptime / 1000);
        objArr4[8] = Integer.valueOf(getEstimatedBatteryCapacity());
        objArr4[9] = Integer.valueOf(getMinLearnedBatteryCapacity());
        objArr4[10] = Integer.valueOf(getMaxLearnedBatteryCapacity());
        objArr4[11] = Long.valueOf(screenDozeTime / 1000);
        dumpLine(printWriter, 0, category2, str, objArr4);
        long partialWakeLockTimeTotal = 0;
        connChanges = 0;
        long fullWakeLockTimeTotal = 0;
        while (connChanges < NU2) {
            ArrayMap<String, ? extends Wakelock> wakelocks = ((Uid) uidStats.valueAt(connChanges)).getWakelockStats();
            i = 1;
            iw = wakelocks.size() - 1;
            while (iw >= 0) {
                wl = (Wakelock) wakelocks.valueAt(iw);
                NU = NU2;
                NU2 = wl.getWakeTime(i);
                if (NU2 != 0) {
                    rawRealtimeMs = rawRealtimeMs3;
                    rawRealtimeMs3 = rawRealtime2;
                    fullWakeLockTimeTotal += NU2.getTotalTimeLocked(rawRealtimeMs3, category);
                } else {
                    rawRealtimeMs = rawRealtimeMs3;
                    rawRealtimeMs3 = rawRealtime2;
                }
                Timer fullWakeTimer = NU2;
                NU2 = wl.getWakeTime(0);
                if (NU2 != 0) {
                    partialWakeLockTimeTotal += NU2.getTotalTimeLocked(rawRealtimeMs3, category);
                }
                iw--;
                rawRealtime2 = rawRealtimeMs3;
                NU2 = NU;
                rawRealtimeMs3 = rawRealtimeMs;
                i = 1;
            }
            connChanges++;
            rawRealtimeMs3 = rawRealtimeMs3;
        }
        NU = NU2;
        rawRealtimeMs = rawRealtimeMs3;
        rawRealtimeMs3 = rawRealtime2;
        rawRealtime = getNetworkActivityBytes(0, category);
        long mobileTxTotalBytes = getNetworkActivityBytes(1, category);
        long wifiRxTotalBytes = getNetworkActivityBytes(2, category);
        long rawRealtime3 = rawRealtimeMs3;
        rawRealtimeMs3 = getNetworkActivityBytes(3, category);
        long mobileRxTotalBytes = rawRealtime;
        long mobileRxTotalPackets = getNetworkActivityPackets(0, category);
        StringBuilder sb4 = sb3;
        SparseArray<? extends Uid> uidStats2 = uidStats;
        long mobileTxTotalPackets = getNetworkActivityPackets(1, category);
        long wifiRxTotalPackets = getNetworkActivityPackets(2, category);
        String category3 = category2;
        long wifiTxTotalPackets = getNetworkActivityPackets(3, category);
        long btRxTotalBytes = getNetworkActivityBytes(4, category);
        long btTxTotalBytes = getNetworkActivityBytes(5, category);
        String str2 = GLOBAL_NETWORK_DATA;
        r15 = new Object[10];
        long wifiTxTotalBytes = rawRealtimeMs3;
        long mobileRxTotalBytes2 = mobileRxTotalBytes;
        r15[0] = Long.valueOf(mobileRxTotalBytes2);
        long mobileRxTotalBytes3 = mobileRxTotalBytes2;
        mobileRxTotalBytes2 = mobileTxTotalBytes;
        r15[1] = Long.valueOf(mobileRxTotalBytes2);
        r15[2] = Long.valueOf(wifiRxTotalBytes);
        long wifiRxTotalBytes2 = wifiRxTotalBytes;
        wifiRxTotalBytes = wifiTxTotalBytes;
        r15[3] = Long.valueOf(wifiRxTotalBytes);
        r15[4] = Long.valueOf(mobileRxTotalPackets);
        long wifiTxTotalBytes2 = wifiRxTotalBytes;
        wifiRxTotalBytes = mobileTxTotalPackets;
        r15[5] = Long.valueOf(wifiRxTotalBytes);
        r15[6] = Long.valueOf(wifiRxTotalPackets);
        long mobileTxTotalPackets2 = wifiRxTotalBytes;
        wifiRxTotalBytes = wifiTxTotalPackets;
        r15[7] = Long.valueOf(wifiRxTotalBytes);
        long wifiTxTotalPackets2 = wifiRxTotalBytes;
        wifiRxTotalBytes = btRxTotalBytes;
        r15[8] = Long.valueOf(wifiRxTotalBytes);
        r15[9] = Long.valueOf(btTxTotalBytes);
        long btRxTotalBytes2 = wifiRxTotalBytes;
        String category4 = category3;
        dumpLine(printWriter, 0, category4, str2, r15);
        long wifiRxTotalPackets2 = wifiRxTotalPackets;
        int i3 = 2;
        i2 = connChanges2;
        i3 = 1;
        int NU3 = NU;
        iw = 0;
        i3 = 5;
        long rawRealtime4 = rawRealtime3;
        rawUptime = batteryUptime;
        int i4 = 3;
        dumpControllerActivityLine(printWriter, 0, category4, GLOBAL_MODEM_CONTROLLER_DATA, getModemControllerActivity(), category);
        long wifiOnTime = getWifiOnTime(rawRealtime4, category);
        long wifiRunningTime = getGlobalWifiRunningTime(rawRealtime4, category);
        str2 = GLOBAL_WIFI_DATA;
        r3 = new Object[5];
        String category5 = category4;
        r3[0] = Long.valueOf(wifiOnTime / 1000);
        r3[1] = Long.valueOf(wifiRunningTime / 1000);
        r3[2] = Integer.valueOf(0);
        r3[3] = Integer.valueOf(0);
        r3[4] = Integer.valueOf(0);
        String category6 = category5;
        dumpLine(printWriter, 0, category6, str2, r3);
        PrintWriter printWriter2 = printWriter;
        String str3 = category6;
        String category7 = category6;
        int i5 = category;
        dumpControllerActivityLine(printWriter2, 0, str3, GLOBAL_WIFI_CONTROLLER_DATA, getWifiControllerActivity(), i5);
        dumpControllerActivityLine(printWriter2, 0, category7, GLOBAL_BLUETOOTH_CONTROLLER_DATA, getBluetoothControllerActivity(), i5);
        category4 = category7;
        dumpLine(printWriter, 0, category4, MISC_DATA, Long.valueOf(screenOnTime / 1000), Long.valueOf(phoneOnTime / 1000), Long.valueOf(fullWakeLockTimeTotal / 1000), Long.valueOf(partialWakeLockTimeTotal / 1000), Long.valueOf(getMobileRadioActiveTime(rawRealtime4, category) / 1000), Long.valueOf(getMobileRadioActiveAdjustedTime(category) / 1000), Long.valueOf(interactiveTime / 1000), Long.valueOf(powerSaveModeEnabledTime / 1000), Integer.valueOf(i2), Long.valueOf(deviceIdleModeFullTime / 1000), Integer.valueOf(getDeviceIdleModeCount(2, category)), Long.valueOf(deviceIdlingTime / 1000), Integer.valueOf(getDeviceIdlingCount(2, category)), Integer.valueOf(getMobileRadioActiveCount(category)), Long.valueOf(getMobileRadioActiveUnknownTime(category) / 1000), Long.valueOf(deviceIdleModeLightTime / 1000), Integer.valueOf(getDeviceIdleModeCount(1, category)), Long.valueOf(deviceLightIdlingTime / 1000), Integer.valueOf(getDeviceIdlingCount(1, category)), Long.valueOf(getLongestDeviceIdleModeTime(1)), Long.valueOf(getLongestDeviceIdleModeTime(2)));
        connChanges = 5;
        Object[] args3 = new Object[5];
        int i6 = 0;
        while (i6 < connChanges) {
            args3[i6] = Long.valueOf(getScreenBrightnessTime(i6, rawRealtime4, category) / 1000);
            i6++;
            connChanges = 5;
        }
        dumpLine(printWriter, 0, category4, SCREEN_BRIGHTNESS_DATA, args3);
        connChanges = 5;
        args3 = new Object[5];
        i6 = 0;
        while (i6 < connChanges) {
            args3[i6] = Long.valueOf(getPhoneSignalStrengthTime(i6, rawRealtime4, category) / 1000);
            i6++;
            connChanges = 5;
        }
        dumpLine(printWriter, 0, category4, SIGNAL_STRENGTH_TIME_DATA, args3);
        dumpLine(printWriter, 0, category4, SIGNAL_SCANNING_TIME_DATA, Long.valueOf(getPhoneSignalScanningTime(rawRealtime4, category) / 1000));
        for (connChanges = 0; connChanges < 5; connChanges++) {
            args3[connChanges] = Integer.valueOf(getPhoneSignalStrengthCount(connChanges, category));
        }
        dumpLine(printWriter, 0, category4, SIGNAL_STRENGTH_COUNT_DATA, args3);
        objArr4 = new Object[21];
        for (NU2 = 0; NU2 < 21; NU2++) {
            objArr4[NU2] = Long.valueOf(getPhoneDataConnectionTime(NU2, rawRealtime4, category) / 1000);
        }
        dumpLine(printWriter, 0, category4, DATA_CONNECTION_TIME_DATA, objArr4);
        for (NU2 = 0; NU2 < 21; NU2++) {
            objArr4[NU2] = Integer.valueOf(getPhoneDataConnectionCount(NU2, category));
        }
        dumpLine(printWriter, 0, category4, DATA_CONNECTION_COUNT_DATA, objArr4);
        NU2 = 8;
        objArr4 = new Object[8];
        i6 = 0;
        while (i6 < NU2) {
            objArr4[i6] = Long.valueOf(getWifiStateTime(i6, rawRealtime4, category) / 1000);
            i6++;
            NU2 = 8;
        }
        dumpLine(printWriter, 0, category4, WIFI_STATE_TIME_DATA, objArr4);
        for (NU2 = 0; NU2 < 8; NU2++) {
            objArr4[NU2] = Integer.valueOf(getWifiStateCount(NU2, category));
        }
        dumpLine(printWriter, 0, category4, WIFI_STATE_COUNT_DATA, objArr4);
        objArr4 = new Object[13];
        for (NU2 = 0; NU2 < 13; NU2++) {
            objArr4[NU2] = Long.valueOf(getWifiSupplStateTime(NU2, rawRealtime4, category) / 1000);
        }
        dumpLine(printWriter, 0, category4, WIFI_SUPPL_STATE_TIME_DATA, objArr4);
        for (NU2 = 0; NU2 < 13; NU2++) {
            objArr4[NU2] = Integer.valueOf(getWifiSupplStateCount(NU2, category));
        }
        dumpLine(printWriter, 0, category4, WIFI_SUPPL_STATE_COUNT_DATA, objArr4);
        NU2 = 5;
        Object[] args4 = new Object[5];
        connChanges = 0;
        while (connChanges < NU2) {
            args4[connChanges] = Long.valueOf(getWifiSignalStrengthTime(connChanges, rawRealtime4, category) / 1000);
            connChanges++;
            NU2 = 5;
        }
        dumpLine(printWriter, 0, category4, WIFI_SIGNAL_STRENGTH_TIME_DATA, args4);
        for (connChanges = 0; connChanges < 5; connChanges++) {
            args4[connChanges] = Integer.valueOf(getWifiSignalStrengthCount(connChanges, category));
        }
        dumpLine(printWriter, 0, category4, WIFI_SIGNAL_STRENGTH_COUNT_DATA, args4);
        rawRealtime3 = getWifiMulticastWakelockTime(rawRealtime4, category);
        i5 = getWifiMulticastWakelockCount(category);
        dumpLine(printWriter, 0, category4, WIFI_MULTICAST_TOTAL_DATA, Long.valueOf(rawRealtime3 / 1000), Integer.valueOf(i5));
        if (category == 2) {
            dumpLine(printWriter, 0, category4, BATTERY_LEVEL_DATA, Integer.valueOf(getDischargeStartLevel()), Integer.valueOf(getDischargeCurrentLevel()));
        }
        if (category == 2) {
            str2 = BATTERY_DISCHARGE_DATA;
            r3 = new Object[10];
            multicastWakeLockCountTotal = i5;
            args = args4;
            r3[4] = Long.valueOf(dischargeCount / 1000);
            r3[5] = Long.valueOf(dischargeScreenOffCount / 1000);
            r3[6] = Integer.valueOf(getDischargeAmountScreenDoze());
            r3[7] = Long.valueOf(dischargeScreenDozeCount / 1000);
            r3[8] = Long.valueOf(dischargeLightDozeCount / 1000);
            r3[9] = Long.valueOf(dischargeDeepDozeCount / 1000);
            dumpLine(printWriter, 0, category4, str2, r3);
            args2 = 3;
        } else {
            multicastWakeLockCountTotal = i5;
            args = args4;
            str2 = BATTERY_DISCHARGE_DATA;
            r3 = new Object[10];
            args2 = 3;
            r3[3] = Integer.valueOf(getDischargeAmountScreenOffSinceCharge());
            String category8 = category4;
            r3[4] = Long.valueOf(dischargeCount / 1000);
            r3[5] = Long.valueOf(dischargeScreenOffCount / 1000);
            r3[6] = Integer.valueOf(getDischargeAmountScreenDozeSinceCharge());
            r3[7] = Long.valueOf(dischargeScreenDozeCount / 1000);
            r3[8] = Long.valueOf(dischargeLightDozeCount / 1000);
            r3[9] = Long.valueOf(dischargeDeepDozeCount / 1000);
            category4 = category8;
            dumpLine(printWriter, 0, category4, str2, r3);
        }
        iw = reqUid;
        int i7;
        if (iw < 0) {
            int connChanges3;
            Map<String, ? extends Timer> kernelWakelocks = getKernelWakelockStats();
            if (kernelWakelocks.size() > 0) {
                Iterator it = kernelWakelocks.entrySet().iterator();
                while (it.hasNext()) {
                    ent = (Entry) it.next();
                    StringBuilder stringBuilder2 = sb4;
                    stringBuilder2.setLength(0);
                    StringBuilder sb5 = stringBuilder2;
                    Entry<String, ? extends Timer> ent2 = ent;
                    Map<String, ? extends Timer> kernelWakelocks2 = kernelWakelocks;
                    Iterator it2 = it;
                    i5 = iw;
                    connChanges3 = i2;
                    batteryUptime2 = rawUptime;
                    i2 = args2;
                    printWakeLockCheckin(stringBuilder2, (Timer) ent.getValue(), rawRealtime4, null, category, "");
                    str2 = KERNEL_WAKELOCK_DATA;
                    r3 = new Object[2];
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("\"");
                    stringBuilder.append((String) ent2.getKey());
                    stringBuilder.append("\"");
                    r3[0] = stringBuilder.toString();
                    sb = sb5;
                    r3[1] = sb.toString();
                    dumpLine(printWriter, 0, category4, str2, r3);
                    sb4 = sb;
                    kernelWakelocks = kernelWakelocks2;
                    it = it2;
                    rawUptime = batteryUptime2;
                    iw = reqUid;
                    args2 = i2;
                    i2 = connChanges3;
                }
            }
            connChanges3 = i2;
            batteryUptime2 = rawUptime;
            i2 = args2;
            rawUptime = rawRealtimeMs;
            sb = sb4;
            objArr = args;
            i7 = multicastWakeLockCountTotal;
            Map<String, ? extends Timer> wakeupReasons = getWakeupReasonStats();
            if (wakeupReasons.size() > 0) {
                Iterator it3 = wakeupReasons.entrySet().iterator();
                while (it3.hasNext()) {
                    Entry<String, ? extends Timer> ent3 = (Entry) it3.next();
                    mobileRxTotalPackets = ((Timer) ent3.getValue()).getTotalTimeLocked(rawRealtime4, category);
                    i5 = ((Timer) ent3.getValue()).getCountLocked(category);
                    Map<String, ? extends Timer> wakeupReasons2 = wakeupReasons;
                    str2 = WAKEUP_REASON_DATA;
                    Iterator it4 = it3;
                    args3 = new Object[i2];
                    StringBuilder stringBuilder3 = new StringBuilder();
                    rawRealtimeMs2 = rawUptime;
                    stringBuilder3.append("\"");
                    stringBuilder3.append((String) ent3.getKey());
                    stringBuilder3.append("\"");
                    args3[0] = stringBuilder3.toString();
                    args3[1] = Long.valueOf((mobileRxTotalPackets + 500) / 1000);
                    args3[2] = Integer.valueOf(i5);
                    dumpLine(printWriter, 0, category4, str2, args3);
                    wakeupReasons = wakeupReasons2;
                    it3 = it4;
                    rawUptime = rawRealtimeMs2;
                    i2 = 3;
                }
            }
            rawRealtimeMs2 = rawUptime;
        } else {
            batteryUptime2 = rawUptime;
            rawRealtimeMs2 = rawRealtimeMs;
            sb = sb4;
            objArr = args;
            i7 = multicastWakeLockCountTotal;
        }
        Map<String, ? extends Timer> rpmStats = getRpmStats();
        Map<String, ? extends Timer> screenOffRpmStats2 = getScreenOffRpmStats();
        if (rpmStats.size() > 0) {
            Iterator it5 = rpmStats.entrySet().iterator();
            while (it5.hasNext()) {
                ent = (Entry) it5.next();
                sb.setLength(0);
                Timer totalTimer = (Timer) ent.getValue();
                mobileRxTotalPackets = (totalTimer.getTotalTimeLocked(rawRealtime4, category) + 500) / 1000;
                i5 = totalTimer.getCountLocked(category);
                Timer screenOffTimer = (Timer) screenOffRpmStats2.get(ent.getKey());
                long totalTimeLocked;
                if (screenOffTimer != null) {
                    totalTimeLocked = (screenOffTimer.getTotalTimeLocked(rawRealtime4, category) + 500) / 1000;
                } else {
                    totalTimeLocked = 0;
                }
                if (screenOffTimer != null) {
                    int countLocked = screenOffTimer.getCountLocked(category);
                }
                Iterator it6 = it5;
                str2 = RESOURCE_POWER_MANAGER_DATA;
                Map<String, ? extends Timer> rpmStats2 = rpmStats;
                objArr2 = new Object[3];
                StringBuilder stringBuilder4 = new StringBuilder();
                screenOffRpmStats = screenOffRpmStats2;
                stringBuilder4.append("\"");
                stringBuilder4.append((String) ent.getKey());
                stringBuilder4.append("\"");
                objArr2[0] = stringBuilder4.toString();
                objArr2[1] = Long.valueOf(mobileRxTotalPackets);
                objArr2[2] = Integer.valueOf(i5);
                dumpLine(printWriter, 0, category4, str2, objArr2);
                it5 = it6;
                rpmStats = rpmStats2;
                screenOffRpmStats2 = screenOffRpmStats;
            }
        }
        screenOffRpmStats = screenOffRpmStats2;
        BatteryStatsHelper helper = new BatteryStatsHelper(context, false, wifiOnly);
        helper.create(this);
        helper.refreshStats(category, -1);
        List<BatterySipper> sippers2 = helper.getUsageList();
        BatteryStatsHelper batteryStatsHelper;
        if (sippers2 == null || sippers2.size() <= 0) {
            sippers = sippers2;
            sb2 = sb;
            batteryStatsHelper = helper;
        } else {
            str2 = POWER_USE_SUMMARY_DATA;
            args3 = new Object[4];
            sb2 = sb;
            args3[0] = BatteryStatsHelper.makemAh(helper.getPowerProfile().getBatteryCapacity());
            args3[1] = BatteryStatsHelper.makemAh(helper.getComputedPower());
            args3[2] = BatteryStatsHelper.makemAh(helper.getMinDrainedPower());
            args3[3] = BatteryStatsHelper.makemAh(helper.getMaxDrainedPower());
            dumpLine(printWriter, 0, category4, str2, args3);
            NU2 = 0;
            connChanges = 0;
            while (connChanges < sippers2.size()) {
                BatterySipper bs = (BatterySipper) sippers2.get(connChanges);
                switch (AnonymousClass2.$SwitchMap$com$android$internal$os$BatterySipper$DrainType[bs.drainType.ordinal()]) {
                    case 1:
                        category6 = "ambi";
                        break;
                    case 2:
                        category6 = "idle";
                        break;
                    case 3:
                        category6 = "cell";
                        break;
                    case 4:
                        category6 = "phone";
                        break;
                    case 5:
                        category6 = "wifi";
                        break;
                    case 6:
                        category6 = "blue";
                        break;
                    case 7:
                        category6 = "scrn";
                        break;
                    case 8:
                        category6 = "flashlight";
                        break;
                    case 9:
                        NU2 = bs.uidObj.getUid();
                        category6 = "uid";
                        break;
                    case 10:
                        NU2 = UserHandle.getUid(bs.userId, 0);
                        category6 = "user";
                        break;
                    case 11:
                        category6 = "unacc";
                        break;
                    case 12:
                        category6 = "over";
                        break;
                    case 13:
                        category6 = "camera";
                        break;
                    case 14:
                        category6 = "memory";
                        break;
                    default:
                        category6 = "???";
                        break;
                }
                String str4 = POWER_USE_ITEM_DATA;
                sippers = sippers2;
                objArr3 = new Object[5];
                batteryStatsHelper = helper;
                objArr3[1] = BatteryStatsHelper.makemAh(bs.totalPowerMah);
                objArr3[2] = Integer.valueOf(bs.shouldHide);
                objArr3[3] = BatteryStatsHelper.makemAh(bs.screenPowerMah);
                objArr3[4] = BatteryStatsHelper.makemAh(bs.proportionalSmearMah);
                dumpLine(printWriter, NU2, category4, str4, objArr3);
                connChanges++;
                sippers2 = sippers;
                helper = batteryStatsHelper;
                boolean z = wifiOnly;
            }
            sippers = sippers2;
            batteryStatsHelper = helper;
        }
        long[] cpuFreqs = getCpuFreqs();
        if (cpuFreqs != null) {
            sb = sb2;
            sb.setLength(0);
            connChanges = 0;
            while (connChanges < cpuFreqs.length) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(connChanges == 0 ? "" : ",");
                stringBuilder.append(cpuFreqs[connChanges]);
                sb.append(stringBuilder.toString());
                connChanges++;
            }
            dumpLine(printWriter, 0, category4, GLOBAL_CPU_FREQ_DATA, sb.toString());
        } else {
            sb = sb2;
        }
        connChanges = 0;
        while (true) {
            i = connChanges;
            i5 = NU3;
            List<BatterySipper> sippers3;
            SparseArray<? extends Uid> uidStats3;
            String category9;
            long rawRealtimeMs4;
            long rawRealtime5;
            StringBuilder sb6;
            long[] cpuFreqs2;
            if (i < i5) {
                int iu;
                int i8;
                PrintWriter printWriter3;
                SparseArray<? extends Uid> uidStats4 = uidStats2;
                int uid = uidStats4.keyAt(i);
                i6 = reqUid;
                if (i6 < 0 || uid == i6) {
                    String str5;
                    int i9;
                    int i10;
                    int uid2;
                    String category10;
                    PrintWriter printWriter4;
                    long rawRealtime6;
                    long fullWifiLockOnTime;
                    long j;
                    Uid u;
                    PrintWriter printWriter5;
                    String category11;
                    long rawRealtime7;
                    long rawRealtime8;
                    Timer bleTimer;
                    Uid u2;
                    Uid u3;
                    long rawRealtime9;
                    Timer bleTimer2;
                    ArrayMap<String, ? extends Wakelock> wakelocks2;
                    int iw2;
                    Timer bleTimer3;
                    long rawRealtimeMs5;
                    StringBuilder sb7;
                    Uid u4;
                    Uid u5 = (Uid) uidStats4.valueAt(i);
                    long[] cpuFreqs3 = cpuFreqs;
                    iu = i;
                    rawUptime = u5.getNetworkActivityBytes(0, category);
                    long rawRealtime10 = rawRealtime4;
                    wifiRxTotalPackets = u5.getNetworkActivityBytes(1, category);
                    int NU4 = i5;
                    StringBuilder sb8 = sb;
                    i5 = u5.getNetworkActivityBytes(2, category);
                    int uid3 = uid;
                    batteryUptime3 = u5.getNetworkActivityBytes(3, category);
                    String category12 = category4;
                    long mobilePacketsRx = u5.getNetworkActivityPackets(0, category);
                    long mobilePacketsTx = u5.getNetworkActivityPackets(1, category);
                    long mobileActiveTime = u5.getMobileRadioActiveTime(category);
                    int mobileActiveCount = u5.getMobileRadioActiveCount(category);
                    long mobileActiveTime2 = mobileActiveTime;
                    long mobileWakeup = u5.getMobileRadioApWakeupCount(category);
                    SparseArray<? extends Uid> uidStats5 = uidStats4;
                    long wifiPacketsRx = u5.getNetworkActivityPackets(2, category);
                    long wifiPacketsTx = u5.getNetworkActivityPackets(3, category);
                    long wifiWakeup = u5.getWifiRadioApWakeupCount(category);
                    long btBytesRx = u5.getNetworkActivityBytes(4, category);
                    long btBytesTx = u5.getNetworkActivityBytes(5, category);
                    long mobileBytesBgRx = u5.getNetworkActivityBytes(6, category);
                    long mobileBytesBgTx = u5.getNetworkActivityBytes(7, category);
                    long wifiBytesBgRx = u5.getNetworkActivityBytes(8, category);
                    long wifiBytesBgTx = u5.getNetworkActivityBytes(9, category);
                    long mobilePacketsBgRx = u5.getNetworkActivityPackets(6, category);
                    long mobilePacketsBgTx = u5.getNetworkActivityPackets(7, category);
                    long wifiPacketsBgRx = u5.getNetworkActivityPackets(8, category);
                    mobileActiveTime = u5.getNetworkActivityPackets(9, category);
                    long wifiBytesTx;
                    long wifiPacketsRx2;
                    long wifiPacketsTx2;
                    long mobileActiveTime3;
                    long mobilePacketsTx2;
                    long btBytesRx2;
                    long btBytesTx2;
                    long mobileWakeup2;
                    long wifiWakeup2;
                    long mobileBytesBgRx2;
                    long mobileBytesBgTx2;
                    long wifiBytesBgRx2;
                    long wifiBytesBgTx2;
                    long mobilePacketsBgRx2;
                    long mobilePacketsBgTx2;
                    long wifiPacketsBgTx;
                    if (rawUptime > 0 || wifiRxTotalPackets > 0 || i5 > 0 || batteryUptime3 > 0 || mobilePacketsRx > 0 || mobilePacketsTx > 0 || wifiPacketsRx > 0 || wifiPacketsTx > 0 || mobileActiveTime2 > 0 || mobileActiveCount > 0 || btBytesRx > 0 || btBytesTx > 0 || mobileWakeup > 0 || wifiWakeup > 0 || mobileBytesBgRx > 0 || mobileBytesBgTx > 0 || wifiBytesBgRx > 0 || wifiBytesBgTx > 0 || mobilePacketsBgRx > 0 || mobilePacketsBgTx > 0 || wifiPacketsBgRx > 0 || mobileActiveTime > 0) {
                        str5 = NETWORK_DATA;
                        Object[] objArr5 = new Object[22];
                        objArr5[0] = Long.valueOf(rawUptime);
                        objArr5[1] = Long.valueOf(wifiRxTotalPackets);
                        objArr5[2] = Long.valueOf(i5);
                        objArr5[3] = Long.valueOf(batteryUptime3);
                        wifiBytesTx = batteryUptime3;
                        batteryUptime3 = mobilePacketsRx;
                        i9 = 4;
                        objArr5[4] = Long.valueOf(batteryUptime3);
                        objArr5[5] = Long.valueOf(mobilePacketsTx);
                        long mobilePacketsRx2 = batteryUptime3;
                        batteryUptime3 = wifiPacketsRx;
                        objArr5[6] = Long.valueOf(batteryUptime3);
                        wifiPacketsRx2 = batteryUptime3;
                        batteryUptime3 = wifiPacketsTx;
                        objArr5[7] = Long.valueOf(batteryUptime3);
                        wifiPacketsTx2 = batteryUptime3;
                        batteryUptime3 = mobileActiveTime2;
                        i8 = 8;
                        objArr5[8] = Long.valueOf(batteryUptime3);
                        i10 = 9;
                        objArr5[9] = Integer.valueOf(mobileActiveCount);
                        mobileActiveTime3 = batteryUptime3;
                        batteryUptime3 = btBytesRx;
                        mobilePacketsTx2 = mobilePacketsTx;
                        objArr5[10] = Long.valueOf(batteryUptime3);
                        btBytesRx2 = batteryUptime3;
                        batteryUptime3 = btBytesTx;
                        objArr5[11] = Long.valueOf(batteryUptime3);
                        btBytesTx2 = batteryUptime3;
                        batteryUptime3 = mobileWakeup;
                        objArr5[12] = Long.valueOf(batteryUptime3);
                        mobileWakeup2 = batteryUptime3;
                        batteryUptime3 = wifiWakeup;
                        objArr5[13] = Long.valueOf(batteryUptime3);
                        wifiWakeup2 = batteryUptime3;
                        batteryUptime3 = mobileBytesBgRx;
                        objArr5[14] = Long.valueOf(batteryUptime3);
                        mobileBytesBgRx2 = batteryUptime3;
                        batteryUptime3 = mobileBytesBgTx;
                        objArr5[15] = Long.valueOf(batteryUptime3);
                        mobileBytesBgTx2 = batteryUptime3;
                        batteryUptime3 = wifiBytesBgRx;
                        objArr5[16] = Long.valueOf(batteryUptime3);
                        wifiBytesBgRx2 = batteryUptime3;
                        batteryUptime3 = wifiBytesBgTx;
                        objArr5[17] = Long.valueOf(batteryUptime3);
                        wifiBytesBgTx2 = batteryUptime3;
                        batteryUptime3 = mobilePacketsBgRx;
                        objArr5[18] = Long.valueOf(batteryUptime3);
                        mobilePacketsBgRx2 = batteryUptime3;
                        batteryUptime3 = mobilePacketsBgTx;
                        objArr5[19] = Long.valueOf(batteryUptime3);
                        mobilePacketsBgTx2 = batteryUptime3;
                        batteryUptime3 = wifiPacketsBgRx;
                        objArr5[20] = Long.valueOf(batteryUptime3);
                        objArr5[21] = Long.valueOf(mobileActiveTime);
                        wifiPacketsBgTx = mobileActiveTime;
                        uid2 = uid3;
                        category10 = category12;
                        printWriter4 = pw;
                        dumpLine(printWriter4, uid2, category10, str5, objArr5);
                    } else {
                        wifiPacketsBgTx = mobileActiveTime;
                        wifiBytesTx = batteryUptime3;
                        mobilePacketsTx2 = mobilePacketsTx;
                        uid2 = uid3;
                        category10 = category12;
                        long j2 = mobilePacketsRx;
                        mobileActiveTime3 = mobileActiveTime2;
                        mobileWakeup2 = mobileWakeup;
                        wifiPacketsRx2 = wifiPacketsRx;
                        wifiPacketsTx2 = wifiPacketsTx;
                        wifiWakeup2 = wifiWakeup;
                        btBytesRx2 = btBytesRx;
                        btBytesTx2 = btBytesTx;
                        mobileBytesBgRx2 = mobileBytesBgRx;
                        mobileBytesBgTx2 = mobileBytesBgTx;
                        wifiBytesBgRx2 = wifiBytesBgRx;
                        wifiBytesBgTx2 = wifiBytesBgTx;
                        mobilePacketsBgRx2 = mobilePacketsBgRx;
                        mobilePacketsBgTx2 = mobilePacketsBgTx;
                        batteryUptime3 = wifiPacketsBgRx;
                        printWriter4 = pw;
                        i9 = 4;
                        i8 = 8;
                        i10 = 9;
                    }
                    i3 = i9;
                    Uid u6 = u5;
                    int i11 = i8;
                    int i12 = i10;
                    sippers3 = sippers;
                    uidStats3 = uidStats5;
                    long wifiBytesRx = i5;
                    i8 = NU4;
                    category = which;
                    dumpControllerActivityLine(printWriter4, uid2, category10, MODEM_CONTROLLER_DATA, u5.getModemControllerActivity(), category);
                    i5 = rawRealtime10;
                    mobileRxTotalPackets = u6.getFullWifiLockTime(i5, category);
                    wifiRxTotalBytes = u6.getWifiScanTime(i5, category);
                    connChanges = u6.getWifiScanCount(category);
                    i3 = u6.getWifiScanBackgroundCount(category);
                    btTxTotalBytes = (u6.getWifiScanActualTime(i5) + 500) / 1000;
                    String category13 = category10;
                    long wifiScanActualTimeMsBg = (u6.getWifiScanBackgroundTime(i5) + 500) / 1000;
                    mobilePacketsTx = u6.getWifiRunningTime(i5, category);
                    if (mobileRxTotalPackets == 0 && wifiRxTotalBytes == 0 && connChanges == 0 && i3 == 0 && btTxTotalBytes == 0) {
                        rawRealtime6 = i5;
                        i5 = wifiScanActualTimeMsBg;
                        if (i5 == 0 && mobilePacketsTx == 0) {
                            long totalTime;
                            Timer unoptimizedScanTimerBg;
                            Object[] objArr6;
                            long unoptimizedScanTotalTimeBg;
                            Timer bgTimer;
                            fullWifiLockOnTime = mobileRxTotalPackets;
                            j = i5;
                            u = u6;
                            i5 = category13;
                            printWriter5 = pw;
                            u6 = u;
                            category11 = i5;
                            rawRealtime7 = rawRealtime6;
                            i = which;
                            dumpControllerActivityLine(printWriter5, uid2, i5, WIFI_CONTROLLER_DATA, u6.getWifiControllerActivity(), i);
                            i5 = u6.getBluetoothScanTimer();
                            long uidWifiRunningTime;
                            int i13;
                            long j3;
                            if (i5 == 0) {
                                mobileRxTotalPackets = rawRealtime7;
                                totalTime = (i5.getTotalTimeLocked(mobileRxTotalPackets, i) + 500) / 1000;
                                if (totalTime != 0) {
                                    i6 = i5.getCountLocked(i);
                                    Timer bleTimerBg = u6.getBluetoothScanBackgroundTimer();
                                    int countBg = bleTimerBg != null ? bleTimerBg.getCountLocked(i) : 0;
                                    rawRealtime8 = mobileRxTotalPackets;
                                    rawRealtime4 = rawRealtimeMs2;
                                    mobileRxTotalPackets = i5.getTotalDurationMsLocked(rawRealtime4);
                                    long actualTimeBg = bleTimerBg != null ? bleTimerBg.getTotalDurationMsLocked(rawRealtime4) : 0;
                                    int resultCount = u6.getBluetoothScanResultCounter() != null ? u6.getBluetoothScanResultCounter().getCountLocked(i) : 0;
                                    if (u6.getBluetoothScanResultBgCounter() != null) {
                                        bleTimer = i5;
                                        i5 = u6.getBluetoothScanResultBgCounter().getCountLocked(i);
                                    } else {
                                        bleTimer = i5;
                                        i5 = 0;
                                    }
                                    uidWifiRunningTime = mobilePacketsTx;
                                    Timer unoptimizedScanTimer = u6.getBluetoothUnoptimizedScanTimer();
                                    long unoptimizedScanTotalTime = unoptimizedScanTimer != null ? unoptimizedScanTimer.getTotalDurationMsLocked(rawRealtime4) : 0;
                                    long unoptimizedScanMaxTime = unoptimizedScanTimer != null ? unoptimizedScanTimer.getMaxDurationMsLocked(rawRealtime4) : 0;
                                    unoptimizedScanTimerBg = u6.getBluetoothUnoptimizedScanBackgroundTimer();
                                    long unoptimizedScanTotalTimeBg2 = unoptimizedScanTimerBg != null ? unoptimizedScanTimerBg.getTotalDurationMsLocked(rawRealtime4) : 0;
                                    long unoptimizedScanMaxTimeBg = unoptimizedScanTimerBg != null ? unoptimizedScanTimerBg.getMaxDurationMsLocked(rawRealtime4) : 0;
                                    mobilePacketsTx = BLUETOOTH_MISC_DATA;
                                    objArr6 = new Object[11];
                                    connChanges = countBg;
                                    objArr6[2] = Integer.valueOf(connChanges);
                                    objArr6[3] = Long.valueOf(mobileRxTotalPackets);
                                    totalTime = actualTimeBg;
                                    objArr6[4] = Long.valueOf(totalTime);
                                    objArr6[5] = Integer.valueOf(resultCount);
                                    objArr6[6] = Integer.valueOf(i5);
                                    totalTime = unoptimizedScanTotalTime;
                                    objArr6[7] = Long.valueOf(totalTime);
                                    u2 = u6;
                                    unoptimizedScanTotalTimeBg = unoptimizedScanTotalTimeBg2;
                                    objArr6[i11] = Long.valueOf(unoptimizedScanTotalTimeBg);
                                    totalTime = unoptimizedScanMaxTime;
                                    objArr6[i12] = Long.valueOf(totalTime);
                                    objArr6[10] = Long.valueOf(unoptimizedScanMaxTimeBg);
                                    long j4 = unoptimizedScanTotalTimeBg;
                                    category9 = category11;
                                    dumpLine(printWriter5, uid2, category9, mobilePacketsTx, objArr6);
                                } else {
                                    rawRealtime8 = mobileRxTotalPackets;
                                    bleTimer = i5;
                                    uidWifiRunningTime = mobilePacketsTx;
                                    u2 = u6;
                                    i13 = i3;
                                    j3 = btTxTotalBytes;
                                    rawRealtime4 = rawRealtimeMs2;
                                    category9 = category11;
                                }
                            } else {
                                bleTimer = i5;
                                uidWifiRunningTime = mobilePacketsTx;
                                u2 = u6;
                                i13 = i3;
                                j3 = btTxTotalBytes;
                                rawRealtime4 = rawRealtimeMs2;
                                rawRealtime8 = rawRealtime7;
                                category9 = category11;
                            }
                            u3 = u2;
                            rawRealtime9 = rawRealtime8;
                            bleTimer2 = bleTimer;
                            dumpControllerActivityLine(printWriter5, uid2, category9, BLUETOOTH_CONTROLLER_DATA, u3.getBluetoothControllerActivity(), i);
                            if (u3.hasUserActivity()) {
                                connChanges = 4;
                                args3 = new Object[4];
                                boolean hasData = false;
                                i6 = 0;
                                while (i6 < connChanges) {
                                    connChanges = u3.getUserActivityCount(i6, i);
                                    args3[i6] = Integer.valueOf(connChanges);
                                    if (connChanges != 0) {
                                        hasData = true;
                                    }
                                    i6++;
                                    connChanges = 4;
                                }
                                if (hasData) {
                                    dumpLine(printWriter5, uid2, category9, USER_ACTIVITY_DATA, args3);
                                }
                                objArr = args3;
                            }
                            if (u3.getAggregatedPartialWakelockTimer() == null) {
                                Timer timer = u3.getAggregatedPartialWakelockTimer();
                                wifiRxTotalBytes = timer.getTotalDurationMsLocked(rawRealtime4);
                                bgTimer = timer.getSubTimer();
                                rawRealtime = bgTimer != null ? bgTimer.getTotalDurationMsLocked(rawRealtime4) : 0;
                                dumpLine(printWriter5, uid2, category9, AGGREGATED_WAKELOCK_DATA, Long.valueOf(wifiRxTotalBytes), Long.valueOf(rawRealtime));
                            }
                            wakelocks2 = u3.getWakelockStats();
                            connChanges = wakelocks2.size() - 1;
                            while (true) {
                                iw2 = connChanges;
                                StringBuilder sb9;
                                String name;
                                if (iw2 < 0) {
                                    wl = (Wakelock) wakelocks2.valueAt(iw2);
                                    sb9 = sb8;
                                    sb9.setLength(0);
                                    batteryUptime3 = rawRealtime9;
                                    bleTimer3 = bleTimer2;
                                    Wakelock wl2 = wl;
                                    i5 = i;
                                    rawRealtimeMs5 = rawRealtime4;
                                    sb7 = sb9;
                                    printWriter3 = printWriter5;
                                    String linePrefix = printWakeLockCheckin(sb9, wl.getWakeTime(1), batteryUptime3, "f", i5, "");
                                    Timer pTimer = wl2.getWakeTime(0);
                                    u4 = u3;
                                    unoptimizedScanTimerBg = pTimer;
                                    linePrefix = printWakeLockCheckin(sb7, pTimer, batteryUptime3, "p", i5, linePrefix);
                                    batteryUptime3 = rawRealtime9;
                                    i5 = i;
                                    str2 = printWakeLockCheckin(sb7, wl2.getWakeTime(2), batteryUptime3, "w", i5, printWakeLockCheckin(sb7, unoptimizedScanTimerBg != null ? unoptimizedScanTimerBg.getSubTimer() : null, batteryUptime3, "bp", i5, linePrefix));
                                    if (sb7.length() > 0) {
                                        name = (String) wakelocks2.keyAt(iw2);
                                        if (name.indexOf(44) >= 0) {
                                            name = name.replace(',', '_');
                                        }
                                        if (name.indexOf(10) >= 0) {
                                            name = name.replace(10, '_');
                                        }
                                        if (name.indexOf(13) >= 0) {
                                            name = name.replace(13, '_');
                                        }
                                        dumpLine(printWriter3, uid2, category9, WAKELOCK_DATA, name, sb7.toString());
                                    }
                                    connChanges = iw2 - 1;
                                    printWriter5 = printWriter3;
                                    sb8 = sb7;
                                    bleTimer2 = bleTimer3;
                                    rawRealtime4 = rawRealtimeMs5;
                                    u3 = u4;
                                } else {
                                    Timer timer2;
                                    StringBuilder sb10;
                                    ArrayMap<String, ? extends Wakelock> wakelocks3;
                                    long totalDurationMsLocked;
                                    long rawRealtimeMs6;
                                    ArrayMap<String, ? extends Timer> syncs;
                                    long rawRealtime11;
                                    long rawRealtimeMs7;
                                    StringBuilder sb11;
                                    long rawRealtime12;
                                    long time;
                                    long[] cpuFreqs4;
                                    int NSE;
                                    int numAnrs;
                                    bleTimer3 = bleTimer2;
                                    rawRealtimeMs5 = rawRealtime4;
                                    sb7 = sb8;
                                    printWriter3 = printWriter5;
                                    bleTimer2 = u3.getMulticastWakelockStats();
                                    if (bleTimer2 != null) {
                                        rawRealtimeMs3 = rawRealtime9;
                                        totalTime = bleTimer2.getTotalTimeLocked(rawRealtimeMs3, i) / 1000;
                                        i6 = bleTimer2.getCountLocked(i);
                                        if (totalTime > 0) {
                                            dumpLine(printWriter3, uid2, category9, WIFI_MULTICAST_DATA, new Object[]{Long.valueOf(totalTime), Integer.valueOf(i6)});
                                        }
                                    } else {
                                        rawRealtimeMs3 = rawRealtime9;
                                    }
                                    i5 = u3.getSyncStats();
                                    connChanges = i5.size() - 1;
                                    while (connChanges >= 0) {
                                        timer2 = (Timer) i5.valueAt(connChanges);
                                        batteryUptime3 = (timer2.getTotalTimeLocked(rawRealtimeMs3, i) + 500) / 1000;
                                        iw = timer2.getCountLocked(i);
                                        Timer mcTimer = bleTimer2;
                                        bleTimer2 = timer2.getSubTimer();
                                        if (bleTimer2 != null) {
                                            sb10 = sb7;
                                            wakelocks3 = wakelocks2;
                                            btTxTotalBytes = rawRealtimeMs5;
                                            totalDurationMsLocked = bleTimer2.getTotalDurationMsLocked(btTxTotalBytes);
                                        } else {
                                            sb10 = sb7;
                                            wakelocks3 = wakelocks2;
                                            btTxTotalBytes = rawRealtimeMs5;
                                            totalDurationMsLocked = -1;
                                        }
                                        long bgTime = totalDurationMsLocked;
                                        int bgCount = bleTimer2 != null ? bleTimer2.getCountLocked(i) : -1;
                                        if (batteryUptime3 != 0) {
                                            name = SYNC_DATA;
                                            Timer bgTimer2 = bleTimer2;
                                            rawRealtimeMs6 = btTxTotalBytes;
                                            r13 = new Object[5];
                                            bleTimer2 = new StringBuilder();
                                            bleTimer2.append("\"");
                                            bleTimer2.append((String) i5.keyAt(connChanges));
                                            bleTimer2.append("\"");
                                            r13[0] = bleTimer2.toString();
                                            r13[1] = Long.valueOf(batteryUptime3);
                                            r13[2] = Integer.valueOf(iw);
                                            r13[3] = Long.valueOf(bgTime);
                                            r13[4] = Integer.valueOf(bgCount);
                                            dumpLine(printWriter3, uid2, category9, name, r13);
                                        } else {
                                            rawRealtimeMs6 = btTxTotalBytes;
                                        }
                                        connChanges--;
                                        bleTimer2 = mcTimer;
                                        wakelocks2 = wakelocks3;
                                        sb7 = sb10;
                                        rawRealtimeMs5 = rawRealtimeMs6;
                                    }
                                    sb10 = sb7;
                                    wakelocks3 = wakelocks2;
                                    rawRealtimeMs6 = rawRealtimeMs5;
                                    ArrayMap<String, ? extends Timer> jobs = u3.getJobStats();
                                    connChanges = jobs.size() - 1;
                                    while (connChanges >= 0) {
                                        timer2 = (Timer) jobs.valueAt(connChanges);
                                        batteryUptime3 = (timer2.getTotalTimeLocked(rawRealtimeMs3, i) + 500) / 1000;
                                        iw = timer2.getCountLocked(i);
                                        Timer bgTimer3 = timer2.getSubTimer();
                                        if (bgTimer3 != null) {
                                            syncs = i5;
                                            rawRealtime11 = rawRealtimeMs3;
                                            i5 = rawRealtimeMs6;
                                            totalDurationMsLocked = bgTimer3.getTotalDurationMsLocked(i5);
                                        } else {
                                            syncs = i5;
                                            rawRealtime11 = rawRealtimeMs3;
                                            i5 = rawRealtimeMs6;
                                            totalDurationMsLocked = -1;
                                        }
                                        long bgTime2 = totalDurationMsLocked;
                                        iw2 = bgTimer3 != null ? bgTimer3.getCountLocked(i) : -1;
                                        if (batteryUptime3 != 0) {
                                            category2 = JOB_DATA;
                                            rawRealtimeMs7 = i5;
                                            i5 = new Object[5];
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("\"");
                                            stringBuilder.append((String) jobs.keyAt(connChanges));
                                            stringBuilder.append("\"");
                                            i5[0] = stringBuilder.toString();
                                            i5[1] = Long.valueOf(batteryUptime3);
                                            i5[2] = Integer.valueOf(iw);
                                            i5[3] = Long.valueOf(bgTime2);
                                            i5[4] = Integer.valueOf(iw2);
                                            dumpLine(printWriter3, uid2, category9, category2, i5);
                                        } else {
                                            rawRealtimeMs7 = i5;
                                        }
                                        connChanges--;
                                        rawRealtimeMs3 = rawRealtime11;
                                        i5 = syncs;
                                        rawRealtimeMs6 = rawRealtimeMs7;
                                    }
                                    syncs = i5;
                                    rawRealtime11 = rawRealtimeMs3;
                                    rawRealtimeMs7 = rawRealtimeMs6;
                                    ArrayMap<String, SparseIntArray> completions = u3.getJobCompletionStats();
                                    for (connChanges = completions.size() - 1; connChanges >= 0; connChanges--) {
                                        SparseIntArray types = (SparseIntArray) completions.valueAt(connChanges);
                                        if (types != null) {
                                            str3 = JOB_COMPLETION_DATA;
                                            objArr3 = new Object[6];
                                            StringBuilder stringBuilder5 = new StringBuilder();
                                            stringBuilder5.append("\"");
                                            stringBuilder5.append((String) completions.keyAt(connChanges));
                                            stringBuilder5.append("\"");
                                            objArr3[0] = stringBuilder5.toString();
                                            objArr3[1] = Integer.valueOf(types.get(0, 0));
                                            objArr3[2] = Integer.valueOf(types.get(1, 0));
                                            objArr3[3] = Integer.valueOf(types.get(2, 0));
                                            objArr3[4] = Integer.valueOf(types.get(3, 0));
                                            objArr3[5] = Integer.valueOf(types.get(4, 0));
                                            dumpLine(printWriter3, uid2, category9, str3, objArr3);
                                        }
                                    }
                                    StringBuilder sb12 = sb10;
                                    u3.getDeferredJobsCheckinLineLocked(sb12, i);
                                    if (sb12.length() > 0) {
                                        str2 = JOBS_DEFERRED_DATA;
                                        r3 = new Object[1];
                                        iw2 = 0;
                                        r3[0] = sb12.toString();
                                        dumpLine(printWriter3, uid2, category9, str2, r3);
                                    } else {
                                        iw2 = 0;
                                    }
                                    printWriter2 = printWriter3;
                                    NU2 = uid2;
                                    str3 = category9;
                                    String category14 = category9;
                                    long rawRealtime13 = rawRealtime11;
                                    unoptimizedScanTotalTimeBg = rawRealtimeMs7;
                                    i5 = rawRealtime13;
                                    String category15 = category14;
                                    mobileActiveCount = iw2;
                                    iw2 = i;
                                    dumpTimer(printWriter2, NU2, str3, FLASHLIGHT_DATA, u3.getFlashlightTurnedOnTimer(), i5, iw2);
                                    str3 = category15;
                                    dumpTimer(printWriter2, NU2, str3, CAMERA_DATA, u3.getCameraTurnedOnTimer(), i5, iw2);
                                    dumpTimer(printWriter2, NU2, str3, VIDEO_DATA, u3.getVideoTurnedOnTimer(), i5, iw2);
                                    dumpTimer(printWriter2, NU2, str3, AUDIO_DATA, u3.getAudioTurnedOnTimer(), i5, iw2);
                                    SparseArray<? extends Sensor> sensors = u3.getSensorStats();
                                    i5 = sensors.size();
                                    connChanges = mobileActiveCount;
                                    while (connChanges < i5) {
                                        int NSE2;
                                        SparseArray<? extends Sensor> sensors2;
                                        Sensor se = (Sensor) sensors.valueAt(connChanges);
                                        i6 = sensors.keyAt(connChanges);
                                        bgTimer = se.getSensorTime();
                                        if (bgTimer != null) {
                                            sb11 = sb12;
                                            btTxTotalBytes = rawRealtime13;
                                            NSE2 = i5;
                                            if ((bgTimer.getTotalTimeLocked(btTxTotalBytes, i) + 500) / 1000 != 0) {
                                                i4 = bgTimer.getCountLocked(i);
                                                sensors2 = sensors;
                                                sensors = se.getSensorBackgroundTime();
                                                int bgCount2 = sensors != null ? sensors.getCountLocked(i) : 0;
                                                rawRealtime12 = btTxTotalBytes;
                                                btTxTotalBytes = bgTimer.getTotalDurationMsLocked(unoptimizedScanTotalTimeBg);
                                                long bgActualTime = sensors != null ? sensors.getTotalDurationMsLocked(unoptimizedScanTotalTimeBg) : 0;
                                                name = SENSOR_DATA;
                                                SparseArray<? extends Sensor> bgTimer4 = sensors;
                                                sensors = new Object[6];
                                                uid = bgCount2;
                                                sensors[3] = Integer.valueOf(uid);
                                                sensors[4] = Long.valueOf(btTxTotalBytes);
                                                sensors[5] = Long.valueOf(bgActualTime);
                                                rawRealtimeMs4 = unoptimizedScanTotalTimeBg;
                                                category9 = category15;
                                                dumpLine(printWriter3, uid2, category9, name, sensors);
                                            } else {
                                                sensors2 = sensors;
                                                rawRealtimeMs4 = unoptimizedScanTotalTimeBg;
                                                rawRealtime12 = btTxTotalBytes;
                                                category9 = category15;
                                            }
                                        } else {
                                            NSE2 = i5;
                                            sensors2 = sensors;
                                            rawRealtimeMs4 = unoptimizedScanTotalTimeBg;
                                            sb11 = sb12;
                                            rawRealtime12 = rawRealtime13;
                                            category9 = category15;
                                        }
                                        connChanges++;
                                        category15 = category9;
                                        sb12 = sb11;
                                        i5 = NSE2;
                                        sensors = sensors2;
                                        rawRealtime13 = rawRealtime12;
                                        unoptimizedScanTotalTimeBg = rawRealtimeMs4;
                                    }
                                    rawRealtimeMs4 = unoptimizedScanTotalTimeBg;
                                    sb11 = sb12;
                                    rawRealtime12 = rawRealtime13;
                                    category9 = category15;
                                    printWriter2 = printWriter3;
                                    NU2 = uid2;
                                    str3 = category9;
                                    i2 = i5;
                                    i5 = rawRealtime12;
                                    SparseArray<? extends Sensor> sensors3 = sensors;
                                    iw2 = i;
                                    dumpTimer(printWriter2, NU2, str3, VIBRATOR_DATA, u3.getVibratorOnTimer(), i5, iw2);
                                    dumpTimer(printWriter2, NU2, str3, FOREGROUND_ACTIVITY_DATA, u3.getForegroundActivityTimer(), i5, iw2);
                                    dumpTimer(printWriter2, NU2, str3, FOREGROUND_SERVICE_DATA, u3.getForegroundServiceTimer(), i5, iw2);
                                    connChanges = 7;
                                    args3 = new Object[7];
                                    mobileRxTotalPackets = 0;
                                    i6 = 0;
                                    while (i6 < connChanges) {
                                        time = u3.getProcessStateTime(i6, rawRealtime12, i);
                                        long totalStateTime = mobileRxTotalPackets + time;
                                        args3[i6] = Long.valueOf((time + 500) / 1000);
                                        i6++;
                                        mobileRxTotalPackets = totalStateTime;
                                        connChanges = 7;
                                    }
                                    rawRealtimeMs3 = rawRealtime12;
                                    if (mobileRxTotalPackets > 0) {
                                        dumpLine(printWriter3, uid2, category9, "st", args3);
                                    }
                                    time = u3.getUserCpuTimeUs(i);
                                    long systemCpuTimeUs = u3.getSystemCpuTimeUs(i);
                                    if (time > 0 || systemCpuTimeUs > 0) {
                                        str2 = CPU_DATA;
                                        i5 = new Object[3];
                                        i5[0] = Long.valueOf(time / 1000);
                                        i5[1] = Long.valueOf(systemCpuTimeUs / 1000);
                                        i5[2] = Integer.valueOf(0);
                                        dumpLine(printWriter3, uid2, category9, str2, i5);
                                    } else {
                                        Object[] objArr7 = args3;
                                        long j5 = mobileRxTotalPackets;
                                    }
                                    SparseArray<? extends Sensor> sensors4;
                                    if (cpuFreqs3 != null) {
                                        long[] cpuFreqTimeMs = u3.getCpuFreqTimes(i);
                                        if (cpuFreqTimeMs != null) {
                                            cpuFreqs4 = cpuFreqs3;
                                            if (cpuFreqTimeMs.length == cpuFreqs4.length) {
                                                stringBuilder = sb11;
                                                stringBuilder.setLength(0);
                                                uid = 0;
                                                while (uid < cpuFreqTimeMs.length) {
                                                    sb9 = new StringBuilder();
                                                    sb9.append(uid == 0 ? "" : ",");
                                                    rawRealtime5 = rawRealtimeMs3;
                                                    sb9.append(cpuFreqTimeMs[uid]);
                                                    stringBuilder.append(sb9.toString());
                                                    uid++;
                                                    rawRealtimeMs3 = rawRealtime5;
                                                }
                                                rawRealtime5 = rawRealtimeMs3;
                                                long[] screenOffCpuFreqTimeMs = u3.getScreenOffCpuFreqTimes(i);
                                                if (screenOffCpuFreqTimeMs != null) {
                                                    for (long rawRealtimeMs32 : screenOffCpuFreqTimeMs) {
                                                        i5 = new StringBuilder();
                                                        i5.append(",");
                                                        i5.append(rawRealtimeMs32);
                                                        stringBuilder.append(i5.toString());
                                                    }
                                                } else {
                                                    for (iw = 0; iw < cpuFreqTimeMs.length; iw++) {
                                                        stringBuilder.append(",0");
                                                    }
                                                }
                                                dumpLine(printWriter3, uid2, category9, CPU_TIMES_AT_FREQ_DATA, new Object[]{UID_TIMES_TYPE_ALL, Integer.valueOf(cpuFreqTimeMs.length), stringBuilder.toString()});
                                            } else {
                                                rawRealtime5 = rawRealtimeMs32;
                                                stringBuilder = sb11;
                                            }
                                        } else {
                                            rawRealtime5 = rawRealtimeMs32;
                                            cpuFreqs4 = cpuFreqs3;
                                            stringBuilder = sb11;
                                        }
                                        uid = 0;
                                        while (uid < 7) {
                                            long[] timesMs = u3.getCpuFreqTimes(i, uid);
                                            if (timesMs == null || timesMs.length != cpuFreqs.length) {
                                                sensors4 = sensors3;
                                            } else {
                                                stringBuilder.setLength(0);
                                                i5 = 0;
                                                while (i5 < timesMs.length) {
                                                    rawRealtimeMs32 = new StringBuilder();
                                                    rawRealtimeMs32.append(i5 == 0 ? "" : ",");
                                                    sensors4 = sensors3;
                                                    rawRealtimeMs32.append(timesMs[i5]);
                                                    stringBuilder.append(rawRealtimeMs32.toString());
                                                    i5++;
                                                    sensors3 = sensors4;
                                                }
                                                sensors4 = sensors3;
                                                i5 = u3.getScreenOffCpuFreqTimes(i, uid);
                                                if (i5 != 0) {
                                                    for (long btTxTotalBytes2 : i5) {
                                                        StringBuilder stringBuilder6 = new StringBuilder();
                                                        stringBuilder6.append(",");
                                                        stringBuilder6.append(btTxTotalBytes2);
                                                        stringBuilder.append(stringBuilder6.toString());
                                                    }
                                                } else {
                                                    for (rawRealtimeMs32 = null; rawRealtimeMs32 < timesMs.length; rawRealtimeMs32++) {
                                                        stringBuilder.append(",0");
                                                    }
                                                }
                                                dumpLine(printWriter3, uid2, category9, CPU_TIMES_AT_FREQ_DATA, Uid.UID_PROCESS_TYPES[uid], Integer.valueOf(timesMs.length), stringBuilder.toString());
                                            }
                                            uid++;
                                            sensors3 = sensors4;
                                        }
                                    } else {
                                        rawRealtime5 = rawRealtimeMs32;
                                        sensors4 = sensors3;
                                        cpuFreqs4 = cpuFreqs3;
                                        stringBuilder = sb11;
                                    }
                                    ArrayMap<String, ? extends Proc> processStats = u3.getProcessStats();
                                    uid = processStats.size() - 1;
                                    while (uid >= 0) {
                                        Proc ps = (Proc) processStats.valueAt(uid);
                                        i5 = ps.getUserTime(i);
                                        btTxTotalBytes2 = ps.getSystemTime(i);
                                        sb6 = stringBuilder;
                                        cpuFreqs2 = cpuFreqs4;
                                        wifiRxTotalBytes = ps.getForegroundTime(i);
                                        iw2 = ps.getStarts(i);
                                        NSE = i2;
                                        i2 = ps.getNumCrashes(i);
                                        Uid u7 = u3;
                                        numAnrs = ps.getNumAnrs(i);
                                        if (i5 == 0 && btTxTotalBytes2 == 0 && wifiRxTotalBytes == 0 && iw2 == 0 && numAnrs == 0 && i2 == 0) {
                                            iw = uid2;
                                        } else {
                                            str5 = PROCESS_DATA;
                                            int uid4 = uid2;
                                            r0 = new Object[7];
                                            StringBuilder stringBuilder7 = new StringBuilder();
                                            String str6 = str5;
                                            stringBuilder7.append("\"");
                                            stringBuilder7.append((String) processStats.keyAt(uid));
                                            stringBuilder7.append("\"");
                                            r0[0] = stringBuilder7.toString();
                                            r0[1] = Long.valueOf(i5);
                                            r0[2] = Long.valueOf(btTxTotalBytes2);
                                            r0[3] = Long.valueOf(wifiRxTotalBytes);
                                            r0[4] = Integer.valueOf(iw2);
                                            r0[5] = Integer.valueOf(numAnrs);
                                            r0[6] = Integer.valueOf(i2);
                                            iw = uid4;
                                            dumpLine(printWriter3, iw, category9, str6, r0);
                                        }
                                        uid--;
                                        uid2 = iw;
                                        cpuFreqs4 = cpuFreqs2;
                                        stringBuilder = sb6;
                                        i2 = NSE;
                                        u3 = u7;
                                        i = which;
                                    }
                                    iw = uid2;
                                    sb6 = stringBuilder;
                                    cpuFreqs2 = cpuFreqs4;
                                    NSE = i2;
                                    Uid u8 = u3;
                                    ArrayMap<String, ? extends Pkg> packageStats = u8.getPackageStats();
                                    i6 = packageStats.size() - 1;
                                    while (i6 >= 0) {
                                        Uid u9;
                                        Pkg ps2 = (Pkg) packageStats.valueAt(i6);
                                        i5 = 0;
                                        ArrayMap<String, ? extends Counter> alarms = ps2.getWakeupAlarmStats();
                                        iw2 = alarms.size() - 1;
                                        while (iw2 >= 0) {
                                            i5 += ((Counter) alarms.valueAt(iw2)).getCountLocked(which);
                                            String name2 = ((String) alarms.keyAt(iw2)).replace(',', '_');
                                            u9 = u8;
                                            dumpLine(printWriter3, iw, category9, WAKEUP_ALARM_DATA, name2, Integer.valueOf(numAnrs));
                                            iw2--;
                                            u8 = u9;
                                        }
                                        u9 = u8;
                                        i2 = which;
                                        ArrayMap<String, ? extends Serv> serviceStats = ps2.getServiceStats();
                                        iw2 = serviceStats.size() - 1;
                                        while (iw2 >= 0) {
                                            Pkg ps3;
                                            ArrayMap<String, ? extends Counter> alarms2;
                                            int isvc;
                                            Serv ss = (Serv) serviceStats.valueAt(iw2);
                                            btTxTotalBytes2 = batteryUptime2;
                                            long startTime = ss.getStartTime(btTxTotalBytes2, i2);
                                            i = ss.getStarts(i2);
                                            ArrayMap<String, ? extends Proc> processStats2 = processStats;
                                            processStats = ss.getLaunches(i2);
                                            if (startTime == 0 && i == 0 && processStats == null) {
                                                ps3 = ps2;
                                                alarms2 = alarms;
                                                isvc = iw2;
                                            } else {
                                                ps3 = ps2;
                                                String str7 = APK_DATA;
                                                alarms2 = alarms;
                                                objArr6 = new Object[6];
                                                isvc = iw2;
                                                objArr6[3] = Long.valueOf(startTime / 1000);
                                                objArr6[4] = Integer.valueOf(i);
                                                objArr6[5] = Integer.valueOf(processStats);
                                                dumpLine(printWriter3, iw, category9, str7, objArr6);
                                            }
                                            iw2 = isvc - 1;
                                            batteryUptime2 = btTxTotalBytes2;
                                            processStats = processStats2;
                                            ps2 = ps3;
                                            alarms = alarms2;
                                        }
                                        i6--;
                                        batteryUptime2 = batteryUptime2;
                                        u8 = u9;
                                    }
                                    btTxTotalBytes2 = batteryUptime2;
                                    i2 = which;
                                }
                            }
                        }
                    } else {
                        rawRealtime6 = i5;
                        i5 = wifiScanActualTimeMsBg;
                    }
                    str = WIFI_DATA;
                    u = u6;
                    objArr2 = new Object[10];
                    fullWifiLockOnTime = mobileRxTotalPackets;
                    objArr2[0] = Long.valueOf(mobileRxTotalPackets);
                    objArr2[1] = Long.valueOf(wifiRxTotalBytes);
                    objArr2[2] = Long.valueOf(mobilePacketsTx);
                    objArr2[3] = Integer.valueOf(connChanges);
                    objArr2[4] = Integer.valueOf(0);
                    objArr2[5] = Integer.valueOf(0);
                    objArr2[6] = Integer.valueOf(0);
                    objArr2[7] = Integer.valueOf(i3);
                    objArr2[i11] = Long.valueOf(btTxTotalBytes2);
                    objArr2[i12] = Long.valueOf(i5);
                    j = i5;
                    i5 = category13;
                    printWriter5 = pw;
                    dumpLine(printWriter5, uid2, i5, str, objArr2);
                    u6 = u;
                    category11 = i5;
                    rawRealtime7 = rawRealtime6;
                    i = which;
                    dumpControllerActivityLine(printWriter5, uid2, i5, WIFI_CONTROLLER_DATA, u6.getWifiControllerActivity(), i);
                    i5 = u6.getBluetoothScanTimer();
                    if (i5 == 0) {
                    }
                    u3 = u2;
                    rawRealtime9 = rawRealtime8;
                    bleTimer2 = bleTimer;
                    dumpControllerActivityLine(printWriter5, uid2, category9, BLUETOOTH_CONTROLLER_DATA, u3.getBluetoothControllerActivity(), i);
                    if (u3.hasUserActivity()) {
                    }
                    if (u3.getAggregatedPartialWakelockTimer() == null) {
                    }
                    wakelocks2 = u3.getWakelockStats();
                    connChanges = wakelocks2.size() - 1;
                    while (true) {
                        iw2 = connChanges;
                        if (iw2 < 0) {
                        }
                        connChanges = iw2 - 1;
                        printWriter5 = printWriter3;
                        sb8 = sb7;
                        bleTimer2 = bleTimer3;
                        rawRealtime4 = rawRealtimeMs5;
                        u3 = u4;
                    }
                } else {
                    uidStats3 = uidStats4;
                    i8 = i5;
                    sb6 = sb;
                    i2 = category;
                    rawRealtime5 = rawRealtime4;
                    cpuFreqs2 = cpuFreqs;
                    iu = i;
                    btTxTotalBytes2 = batteryUptime2;
                    rawRealtimeMs4 = rawRealtimeMs2;
                    sippers3 = sippers;
                    category9 = category4;
                    printWriter3 = printWriter;
                }
                connChanges = iu + 1;
                category4 = category9;
                category = i2;
                printWriter = printWriter3;
                batteryUptime2 = btTxTotalBytes2;
                uidStats2 = uidStats3;
                sippers = sippers3;
                NU3 = i8;
                rawRealtimeMs2 = rawRealtimeMs4;
                rawRealtime4 = rawRealtime5;
                cpuFreqs = cpuFreqs2;
                sb = sb6;
                Context context2 = context;
            } else {
                sb6 = sb;
                i2 = category;
                rawRealtime5 = rawRealtime4;
                cpuFreqs2 = cpuFreqs;
                uidStats3 = uidStats2;
                btTxTotalBytes2 = batteryUptime2;
                rawRealtimeMs4 = rawRealtimeMs2;
                sippers3 = sippers;
                category9 = category4;
                rawRealtime4 = printWriter;
                return;
            }
        }
    }

    private void printmAh(PrintWriter printer, double power) {
        printer.print(BatteryStatsHelper.makemAh(power));
    }

    private void printmAh(StringBuilder sb, double power) {
        sb.append(BatteryStatsHelper.makemAh(power));
    }

    public final void dumpLocked(Context context, PrintWriter pw, String prefix, int which, int reqUid) {
        dumpLocked(context, pw, prefix, which, reqUid, BatteryStatsHelper.checkWifiOnly(context));
    }

    /* JADX WARNING: Removed duplicated region for block: B:488:0x19ef  */
    /* JADX WARNING: Removed duplicated region for block: B:414:0x17e7  */
    /* JADX WARNING: Removed duplicated region for block: B:504:0x1a48  */
    /* JADX WARNING: Removed duplicated region for block: B:491:0x1a0a  */
    /* JADX WARNING: Removed duplicated region for block: B:517:0x1b57  */
    /* JADX WARNING: Removed duplicated region for block: B:507:0x1a5a  */
    /* JADX WARNING: Removed duplicated region for block: B:568:0x1c70  */
    /* JADX WARNING: Removed duplicated region for block: B:520:0x1b74  */
    /* JADX WARNING: Removed duplicated region for block: B:575:0x1cc8  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x1c89  */
    /* JADX WARNING: Removed duplicated region for block: B:578:0x1cd5  */
    /* JADX WARNING: Removed duplicated region for block: B:597:0x1d98  */
    /* JADX WARNING: Removed duplicated region for block: B:616:0x1e52  */
    /* JADX WARNING: Removed duplicated region for block: B:626:0x1eab  */
    /* JADX WARNING: Removed duplicated region for block: B:629:0x1f55  */
    /* JADX WARNING: Removed duplicated region for block: B:660:0x20a7  */
    /* JADX WARNING: Removed duplicated region for block: B:667:0x20f7  */
    /* JADX WARNING: Removed duplicated region for block: B:681:0x2188  */
    /* JADX WARNING: Removed duplicated region for block: B:676:0x2152  */
    /* JADX WARNING: Removed duplicated region for block: B:689:0x21c7  */
    /* JADX WARNING: Removed duplicated region for block: B:684:0x2191  */
    /* JADX WARNING: Removed duplicated region for block: B:693:0x21ce  */
    /* JADX WARNING: Removed duplicated region for block: B:764:0x2426  */
    /* JADX WARNING: Removed duplicated region for block: B:711:0x227f  */
    /* JADX WARNING: Removed duplicated region for block: B:788:0x256e  */
    /* JADX WARNING: Removed duplicated region for block: B:767:0x2440  */
    /* JADX WARNING: Removed duplicated region for block: B:404:0x1760  */
    /* JADX WARNING: Removed duplicated region for block: B:410:0x17bd  */
    /* JADX WARNING: Removed duplicated region for block: B:407:0x17b3  */
    /* JADX WARNING: Removed duplicated region for block: B:414:0x17e7  */
    /* JADX WARNING: Removed duplicated region for block: B:488:0x19ef  */
    /* JADX WARNING: Removed duplicated region for block: B:491:0x1a0a  */
    /* JADX WARNING: Removed duplicated region for block: B:504:0x1a48  */
    /* JADX WARNING: Removed duplicated region for block: B:507:0x1a5a  */
    /* JADX WARNING: Removed duplicated region for block: B:517:0x1b57  */
    /* JADX WARNING: Removed duplicated region for block: B:520:0x1b74  */
    /* JADX WARNING: Removed duplicated region for block: B:568:0x1c70  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x1c89  */
    /* JADX WARNING: Removed duplicated region for block: B:575:0x1cc8  */
    /* JADX WARNING: Removed duplicated region for block: B:578:0x1cd5  */
    /* JADX WARNING: Removed duplicated region for block: B:597:0x1d98  */
    /* JADX WARNING: Removed duplicated region for block: B:616:0x1e52  */
    /* JADX WARNING: Removed duplicated region for block: B:626:0x1eab  */
    /* JADX WARNING: Removed duplicated region for block: B:629:0x1f55  */
    /* JADX WARNING: Removed duplicated region for block: B:660:0x20a7  */
    /* JADX WARNING: Removed duplicated region for block: B:667:0x20f7  */
    /* JADX WARNING: Removed duplicated region for block: B:670:0x211d  */
    /* JADX WARNING: Removed duplicated region for block: B:676:0x2152  */
    /* JADX WARNING: Removed duplicated region for block: B:681:0x2188  */
    /* JADX WARNING: Removed duplicated region for block: B:684:0x2191  */
    /* JADX WARNING: Removed duplicated region for block: B:689:0x21c7  */
    /* JADX WARNING: Removed duplicated region for block: B:693:0x21ce  */
    /* JADX WARNING: Removed duplicated region for block: B:711:0x227f  */
    /* JADX WARNING: Removed duplicated region for block: B:764:0x2426  */
    /* JADX WARNING: Removed duplicated region for block: B:767:0x2440  */
    /* JADX WARNING: Removed duplicated region for block: B:788:0x256e  */
    /* JADX WARNING: Removed duplicated region for block: B:400:0x1660  */
    /* JADX WARNING: Removed duplicated region for block: B:382:0x15d8  */
    /* JADX WARNING: Removed duplicated region for block: B:404:0x1760  */
    /* JADX WARNING: Removed duplicated region for block: B:407:0x17b3  */
    /* JADX WARNING: Removed duplicated region for block: B:410:0x17bd  */
    /* JADX WARNING: Removed duplicated region for block: B:488:0x19ef  */
    /* JADX WARNING: Removed duplicated region for block: B:414:0x17e7  */
    /* JADX WARNING: Removed duplicated region for block: B:504:0x1a48  */
    /* JADX WARNING: Removed duplicated region for block: B:491:0x1a0a  */
    /* JADX WARNING: Removed duplicated region for block: B:517:0x1b57  */
    /* JADX WARNING: Removed duplicated region for block: B:507:0x1a5a  */
    /* JADX WARNING: Removed duplicated region for block: B:568:0x1c70  */
    /* JADX WARNING: Removed duplicated region for block: B:520:0x1b74  */
    /* JADX WARNING: Removed duplicated region for block: B:575:0x1cc8  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x1c89  */
    /* JADX WARNING: Removed duplicated region for block: B:578:0x1cd5  */
    /* JADX WARNING: Removed duplicated region for block: B:597:0x1d98  */
    /* JADX WARNING: Removed duplicated region for block: B:616:0x1e52  */
    /* JADX WARNING: Removed duplicated region for block: B:626:0x1eab  */
    /* JADX WARNING: Removed duplicated region for block: B:629:0x1f55  */
    /* JADX WARNING: Removed duplicated region for block: B:660:0x20a7  */
    /* JADX WARNING: Removed duplicated region for block: B:667:0x20f7  */
    /* JADX WARNING: Removed duplicated region for block: B:670:0x211d  */
    /* JADX WARNING: Removed duplicated region for block: B:681:0x2188  */
    /* JADX WARNING: Removed duplicated region for block: B:676:0x2152  */
    /* JADX WARNING: Removed duplicated region for block: B:689:0x21c7  */
    /* JADX WARNING: Removed duplicated region for block: B:684:0x2191  */
    /* JADX WARNING: Removed duplicated region for block: B:693:0x21ce  */
    /* JADX WARNING: Removed duplicated region for block: B:764:0x2426  */
    /* JADX WARNING: Removed duplicated region for block: B:711:0x227f  */
    /* JADX WARNING: Removed duplicated region for block: B:788:0x256e  */
    /* JADX WARNING: Removed duplicated region for block: B:767:0x2440  */
    /* JADX WARNING: Removed duplicated region for block: B:362:0x14e2  */
    /* JADX WARNING: Removed duplicated region for block: B:367:0x1529  */
    /* JADX WARNING: Removed duplicated region for block: B:366:0x1510  */
    /* JADX WARNING: Removed duplicated region for block: B:382:0x15d8  */
    /* JADX WARNING: Removed duplicated region for block: B:400:0x1660  */
    /* JADX WARNING: Removed duplicated region for block: B:404:0x1760  */
    /* JADX WARNING: Removed duplicated region for block: B:410:0x17bd  */
    /* JADX WARNING: Removed duplicated region for block: B:407:0x17b3  */
    /* JADX WARNING: Removed duplicated region for block: B:414:0x17e7  */
    /* JADX WARNING: Removed duplicated region for block: B:488:0x19ef  */
    /* JADX WARNING: Removed duplicated region for block: B:491:0x1a0a  */
    /* JADX WARNING: Removed duplicated region for block: B:504:0x1a48  */
    /* JADX WARNING: Removed duplicated region for block: B:507:0x1a5a  */
    /* JADX WARNING: Removed duplicated region for block: B:517:0x1b57  */
    /* JADX WARNING: Removed duplicated region for block: B:520:0x1b74  */
    /* JADX WARNING: Removed duplicated region for block: B:568:0x1c70  */
    /* JADX WARNING: Removed duplicated region for block: B:571:0x1c89  */
    /* JADX WARNING: Removed duplicated region for block: B:575:0x1cc8  */
    /* JADX WARNING: Removed duplicated region for block: B:578:0x1cd5  */
    /* JADX WARNING: Removed duplicated region for block: B:597:0x1d98  */
    /* JADX WARNING: Removed duplicated region for block: B:616:0x1e52  */
    /* JADX WARNING: Removed duplicated region for block: B:626:0x1eab  */
    /* JADX WARNING: Removed duplicated region for block: B:629:0x1f55  */
    /* JADX WARNING: Removed duplicated region for block: B:660:0x20a7  */
    /* JADX WARNING: Removed duplicated region for block: B:667:0x20f7  */
    /* JADX WARNING: Removed duplicated region for block: B:670:0x211d  */
    /* JADX WARNING: Removed duplicated region for block: B:676:0x2152  */
    /* JADX WARNING: Removed duplicated region for block: B:681:0x2188  */
    /* JADX WARNING: Removed duplicated region for block: B:684:0x2191  */
    /* JADX WARNING: Removed duplicated region for block: B:689:0x21c7  */
    /* JADX WARNING: Removed duplicated region for block: B:693:0x21ce  */
    /* JADX WARNING: Removed duplicated region for block: B:711:0x227f  */
    /* JADX WARNING: Removed duplicated region for block: B:764:0x2426  */
    /* JADX WARNING: Removed duplicated region for block: B:767:0x2440  */
    /* JADX WARNING: Removed duplicated region for block: B:788:0x256e  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public final void dumpLocked(Context context, PrintWriter pw, String prefix, int which, int reqUid, boolean wifiOnly) {
        long screenDozeTime;
        long whichBatteryScreenOffRealtime;
        int NU;
        long interactiveTime;
        int NU2;
        SparseArray<? extends Uid> uidStats;
        long powerSaveModeEnabledTime;
        long screenOnTime;
        int iw;
        long whichBatteryRealtime;
        int i;
        long btTxTotalBytes;
        long mobileTxTotalPackets;
        long mobileTxTotalPackets2;
        long mobileRxTotalPackets;
        long mobileTxTotalBytes;
        long rawRealtime;
        long wifiTxTotalPackets;
        String[] wifiRxSignalStrengthDescription;
        int numWifiRxBins;
        String[] gpsSignalQualityDescription;
        int i2;
        List<BatterySipper> sippers;
        long rawRealtime2;
        int i3;
        Iterator it;
        Timer timer;
        Entry<String, ? extends Timer> ent;
        int i4;
        ArrayList<TimerEntry> kernelWakelocks;
        StringBuilder sb;
        LongSparseArray<? extends Timer> mMemoryStats;
        long whichBatteryRealtime2;
        StringBuilder sb2;
        StringBuilder stringBuilder;
        PrintWriter printWriter;
        BatteryStats batteryStats = this;
        PrintWriter printWriter2 = pw;
        String str = prefix;
        int i5 = which;
        int i6 = reqUid;
        long rawUptime = SystemClock.uptimeMillis() * 1000;
        long rawRealtime3 = SystemClock.elapsedRealtime() * 1000;
        long rawRealtimeMs = (rawRealtime3 + 500) / 1000;
        long batteryUptime = batteryStats.getBatteryUptime(rawUptime);
        long rawRealtimeMs2 = rawRealtimeMs;
        rawRealtimeMs = batteryStats.computeBatteryUptime(rawUptime, i5);
        long batteryUptime2 = batteryUptime;
        batteryUptime = batteryStats.computeBatteryRealtime(rawRealtime3, i5);
        long totalRealtime = batteryStats.computeRealtime(rawRealtime3, i5);
        long totalUptime = batteryStats.computeUptime(rawUptime, i5);
        long whichBatteryUptime = rawRealtimeMs;
        rawRealtimeMs = batteryStats.computeBatteryScreenOffUptime(rawUptime, i5);
        rawUptime = batteryStats.computeBatteryScreenOffRealtime(rawRealtime3, i5);
        long batteryTimeRemaining = batteryStats.computeBatteryTimeRemaining(rawRealtime3);
        long chargeTimeRemaining = batteryStats.computeChargeTimeRemaining(rawRealtime3);
        long whichBatteryScreenOffUptime = rawRealtimeMs;
        rawRealtimeMs = batteryStats.getScreenDozeTime(rawRealtime3, i5);
        StringBuilder sb3 = new StringBuilder(128);
        SparseArray<? extends Uid> uidStats2 = getUidStats();
        long rawRealtime4 = rawRealtime3;
        int NU3 = uidStats2.size();
        int estimatedBatteryCapacity = getEstimatedBatteryCapacity();
        SparseArray<? extends Uid> uidStats3 = uidStats2;
        if (estimatedBatteryCapacity > 0) {
            sb3.setLength(0);
            sb3.append(str);
            sb3.append("  Estimated battery capacity: ");
            screenDozeTime = rawRealtimeMs;
            sb3.append(BatteryStatsHelper.makemAh((double) estimatedBatteryCapacity));
            sb3.append(" mAh");
            printWriter2.println(sb3.toString());
        } else {
            screenDozeTime = rawRealtimeMs;
        }
        int minLearnedBatteryCapacity = getMinLearnedBatteryCapacity();
        if (minLearnedBatteryCapacity > 0) {
            sb3.setLength(0);
            sb3.append(str);
            sb3.append("  Min learned battery capacity: ");
            sb3.append(BatteryStatsHelper.makemAh((double) (minLearnedBatteryCapacity / 1000)));
            sb3.append(" mAh");
            printWriter2.println(sb3.toString());
        }
        minLearnedBatteryCapacity = getMaxLearnedBatteryCapacity();
        if (minLearnedBatteryCapacity > 0) {
            sb3.setLength(0);
            sb3.append(str);
            sb3.append("  Max learned battery capacity: ");
            sb3.append(BatteryStatsHelper.makemAh((double) (minLearnedBatteryCapacity / 1000)));
            sb3.append(" mAh");
            printWriter2.println(sb3.toString());
        }
        sb3.setLength(0);
        sb3.append(str);
        sb3.append("  Time on battery: ");
        formatTimeMs(sb3, batteryUptime / 1000);
        sb3.append("(");
        sb3.append(batteryStats.formatRatioLocked(batteryUptime, totalRealtime));
        sb3.append(") realtime, ");
        formatTimeMs(sb3, whichBatteryUptime / 1000);
        sb3.append("(");
        rawRealtimeMs = whichBatteryUptime;
        sb3.append(batteryStats.formatRatioLocked(rawRealtimeMs, batteryUptime));
        sb3.append(") uptime");
        printWriter2.println(sb3.toString());
        sb3.setLength(0);
        sb3.append(str);
        sb3.append("  Time on battery screen off: ");
        long whichBatteryUptime2 = rawRealtimeMs;
        formatTimeMs(sb3, rawUptime / 1000);
        sb3.append("(");
        sb3.append(batteryStats.formatRatioLocked(rawUptime, batteryUptime));
        sb3.append(") realtime, ");
        formatTimeMs(sb3, whichBatteryScreenOffUptime / 1000);
        sb3.append("(");
        rawRealtimeMs = whichBatteryScreenOffUptime;
        sb3.append(batteryStats.formatRatioLocked(rawRealtimeMs, batteryUptime));
        sb3.append(") uptime");
        printWriter2.println(sb3.toString());
        sb3.setLength(0);
        sb3.append(str);
        sb3.append("  Time on battery screen doze: ");
        long whichBatteryScreenOffUptime2 = rawRealtimeMs;
        formatTimeMs(sb3, screenDozeTime / 1000);
        sb3.append("(");
        rawRealtimeMs = screenDozeTime;
        sb3.append(batteryStats.formatRatioLocked(rawRealtimeMs, batteryUptime));
        sb3.append(")");
        printWriter2.println(sb3.toString());
        sb3.setLength(0);
        sb3.append(str);
        sb3.append("  Total run time: ");
        long screenDozeTime2 = rawRealtimeMs;
        formatTimeMs(sb3, totalRealtime / 1000);
        sb3.append("realtime, ");
        formatTimeMs(sb3, totalUptime / 1000);
        sb3.append("uptime");
        printWriter2.println(sb3.toString());
        if (batteryTimeRemaining >= 0) {
            sb3.setLength(0);
            sb3.append(str);
            sb3.append("  Battery time remaining: ");
            formatTimeMs(sb3, batteryTimeRemaining / 1000);
            printWriter2.println(sb3.toString());
        }
        if (chargeTimeRemaining >= 0) {
            sb3.setLength(0);
            sb3.append(str);
            sb3.append("  Charge time remaining: ");
            formatTimeMs(sb3, chargeTimeRemaining / 1000);
            printWriter2.println(sb3.toString());
        }
        rawRealtimeMs = batteryStats.getUahDischarge(i5);
        if (rawRealtimeMs >= 0) {
            sb3.setLength(0);
            sb3.append(str);
            sb3.append("  Discharge: ");
            whichBatteryScreenOffRealtime = rawUptime;
            sb3.append(BatteryStatsHelper.makemAh(((double) rawRealtimeMs) / 1000.0d));
            sb3.append(" mAh");
            printWriter2.println(sb3.toString());
        } else {
            whichBatteryScreenOffRealtime = rawUptime;
        }
        rawUptime = batteryStats.getUahDischargeScreenOff(i5);
        if (rawUptime >= 0) {
            sb3.setLength(0);
            sb3.append(str);
            sb3.append("  Screen off discharge: ");
            sb3.append(BatteryStatsHelper.makemAh(((double) rawUptime) / 1000.0d));
            sb3.append(" mAh");
            printWriter2.println(sb3.toString());
        }
        totalRealtime = batteryStats.getUahDischargeScreenDoze(i5);
        if (totalRealtime >= 0) {
            sb3.setLength(0);
            sb3.append(str);
            sb3.append("  Screen doze discharge: ");
            NU = NU3;
            sb3.append(BatteryStatsHelper.makemAh(((double) totalRealtime) / 1000.0d));
            sb3.append(" mAh");
            printWriter2.println(sb3.toString());
        } else {
            NU = NU3;
            int i7 = estimatedBatteryCapacity;
        }
        rawRealtime3 = rawRealtimeMs - rawUptime;
        long dischargeCount;
        if (rawRealtime3 >= 0) {
            sb3.setLength(0);
            sb3.append(str);
            sb3.append("  Screen on discharge: ");
            dischargeCount = rawRealtimeMs;
            sb3.append(BatteryStatsHelper.makemAh(((double) rawRealtime3) / 1000.0d));
            sb3.append(" mAh");
            printWriter2.println(sb3.toString());
        } else {
            dischargeCount = rawRealtimeMs;
        }
        rawRealtimeMs = batteryStats.getUahDischargeLightDoze(i5);
        long dischargeScreenOnCount;
        if (rawRealtimeMs >= 0) {
            sb3.setLength(0);
            sb3.append(str);
            sb3.append("  Device light doze discharge: ");
            dischargeScreenOnCount = rawRealtime3;
            sb3.append(BatteryStatsHelper.makemAh(((double) rawRealtimeMs) / 1000.0d));
            sb3.append(" mAh");
            printWriter2.println(sb3.toString());
        } else {
            dischargeScreenOnCount = rawRealtime3;
        }
        rawRealtime3 = batteryStats.getUahDischargeDeepDoze(i5);
        long dischargeLightDozeCount;
        if (rawRealtime3 >= 0) {
            sb3.setLength(0);
            sb3.append(str);
            sb3.append("  Device deep doze discharge: ");
            dischargeLightDozeCount = rawRealtimeMs;
            sb3.append(BatteryStatsHelper.makemAh(((double) rawRealtime3) / 1000.0d));
            sb3.append(" mAh");
            printWriter2.println(sb3.toString());
        } else {
            dischargeLightDozeCount = rawRealtimeMs;
        }
        printWriter2.print("  Start clock time: ");
        printWriter2.println(DateFormat.format((CharSequence) "yyyy-MM-dd-HH-mm-ss", getStartClockTime()).toString());
        long dischargeScreenDozeCount = totalRealtime;
        rawRealtimeMs = rawRealtime4;
        totalRealtime = batteryStats.getScreenOnTime(rawRealtimeMs, i5);
        long dischargeScreenOffCount = rawUptime;
        rawUptime = batteryStats.getInteractiveTime(rawRealtimeMs, i5);
        long dischargeDeepDozeCount = rawRealtime3;
        long powerSaveModeEnabledTime2 = batteryStats.getPowerSaveModeEnabledTime(rawRealtimeMs, i5);
        long deviceIdleModeLightTime = batteryStats.getDeviceIdleModeTime(1, rawRealtimeMs, i5);
        long deviceIdleModeFullTime = batteryStats.getDeviceIdleModeTime(2, rawRealtimeMs, i5);
        long deviceLightIdlingTime = batteryStats.getDeviceIdlingTime(1, rawRealtimeMs, i5);
        long deviceIdlingTime = batteryStats.getDeviceIdlingTime(2, rawRealtimeMs, i5);
        rawRealtime3 = batteryStats.getPhoneOnTime(rawRealtimeMs, i5);
        whichBatteryScreenOffUptime = batteryStats.getGlobalWifiRunningTime(rawRealtimeMs, i5);
        rawRealtime4 = batteryStats.getWifiOnTime(rawRealtimeMs, i5);
        sb3.setLength(0);
        sb3.append(str);
        sb3.append("  Screen on: ");
        long phoneOnTime = rawRealtime3;
        formatTimeMs(sb3, totalRealtime / 1000);
        sb3.append("(");
        sb3.append(batteryStats.formatRatioLocked(totalRealtime, batteryUptime));
        sb3.append(") ");
        sb3.append(batteryStats.getScreenOnCount(i5));
        sb3.append("x, Interactive: ");
        formatTimeMs(sb3, rawUptime / 1000);
        sb3.append("(");
        sb3.append(batteryStats.formatRatioLocked(rawUptime, batteryUptime));
        sb3.append(")");
        printWriter2.println(sb3.toString());
        sb3.setLength(0);
        sb3.append(str);
        sb3.append("  Screen brightnesses:");
        boolean didOne = false;
        int i8 = 0;
        while (i8 < 5) {
            interactiveTime = rawUptime;
            rawUptime = batteryStats.getScreenBrightnessTime(i8, rawRealtimeMs, i5);
            if (rawUptime != 0) {
                sb3.append("\n    ");
                sb3.append(str);
                sb3.append(SCREEN_BRIGHTNESS_NAMES[i8]);
                sb3.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                formatTimeMs(sb3, rawUptime / true);
                sb3.append("(");
                sb3.append(batteryStats.formatRatioLocked(rawUptime, totalRealtime));
                sb3.append(")");
                didOne = true;
            }
            i8++;
            rawUptime = interactiveTime;
        }
        interactiveTime = rawUptime;
        if (!didOne) {
            sb3.append(" (no activity)");
        }
        printWriter2.println(sb3.toString());
        if (powerSaveModeEnabledTime2 != 0) {
            sb3.setLength(0);
            sb3.append(str);
            sb3.append("  Power save mode enabled: ");
            formatTimeMs(sb3, powerSaveModeEnabledTime2 / 1000);
            sb3.append("(");
            rawUptime = powerSaveModeEnabledTime2;
            sb3.append(batteryStats.formatRatioLocked(rawUptime, batteryUptime));
            sb3.append(")");
            printWriter2.println(sb3.toString());
        } else {
            rawUptime = powerSaveModeEnabledTime2;
        }
        long deviceLightIdlingTime2;
        if (deviceLightIdlingTime != 0) {
            sb3.setLength(0);
            sb3.append(str);
            sb3.append("  Device light idling: ");
            formatTimeMs(sb3, deviceLightIdlingTime / 1000);
            sb3.append("(");
            didOne = deviceLightIdlingTime;
            batteryStats = this;
            sb3.append(batteryStats.formatRatioLocked(didOne, batteryUptime));
            sb3.append(") ");
            deviceLightIdlingTime2 = didOne;
            sb3.append(batteryStats.getDeviceIdlingCount(1, i5));
            sb3.append("x");
            printWriter2.println(sb3.toString());
        } else {
            deviceLightIdlingTime2 = deviceLightIdlingTime;
        }
        long deviceIdleModeLightTime2;
        if (deviceIdleModeLightTime != 0) {
            sb3.setLength(0);
            sb3.append(str);
            sb3.append("  Idle mode light time: ");
            formatTimeMs(sb3, deviceIdleModeLightTime / 1000);
            sb3.append("(");
            rawRealtime3 = deviceIdleModeLightTime;
            sb3.append(batteryStats.formatRatioLocked(rawRealtime3, batteryUptime));
            sb3.append(") ");
            deviceIdleModeLightTime2 = rawRealtime3;
            sb3.append(batteryStats.getDeviceIdleModeCount(1, i5));
            sb3.append("x");
            sb3.append(" -- longest ");
            formatTimeMs(sb3, batteryStats.getLongestDeviceIdleModeTime(1));
            printWriter2.println(sb3.toString());
        } else {
            deviceIdleModeLightTime2 = deviceIdleModeLightTime;
        }
        long deviceIdlingTime2;
        if (deviceIdlingTime != 0) {
            sb3.setLength(0);
            sb3.append(str);
            sb3.append("  Device full idling: ");
            formatTimeMs(sb3, deviceIdlingTime / 1000);
            sb3.append("(");
            rawRealtime3 = deviceIdlingTime;
            sb3.append(batteryStats.formatRatioLocked(rawRealtime3, batteryUptime));
            sb3.append(") ");
            deviceIdlingTime2 = rawRealtime3;
            sb3.append(batteryStats.getDeviceIdlingCount(2, i5));
            sb3.append("x");
            printWriter2.println(sb3.toString());
        } else {
            deviceIdlingTime2 = deviceIdlingTime;
        }
        long deviceIdleModeFullTime2;
        if (deviceIdleModeFullTime != 0) {
            sb3.setLength(0);
            sb3.append(str);
            sb3.append("  Idle mode full time: ");
            formatTimeMs(sb3, deviceIdleModeFullTime / 1000);
            sb3.append("(");
            rawRealtime3 = deviceIdleModeFullTime;
            sb3.append(batteryStats.formatRatioLocked(rawRealtime3, batteryUptime));
            sb3.append(") ");
            deviceIdleModeFullTime2 = rawRealtime3;
            sb3.append(batteryStats.getDeviceIdleModeCount(2, i5));
            sb3.append("x");
            sb3.append(" -- longest ");
            formatTimeMs(sb3, batteryStats.getLongestDeviceIdleModeTime(2));
            printWriter2.println(sb3.toString());
        } else {
            deviceIdleModeFullTime2 = deviceIdleModeFullTime;
        }
        if (phoneOnTime != 0) {
            sb3.setLength(0);
            sb3.append(str);
            sb3.append("  Active phone call: ");
            formatTimeMs(sb3, phoneOnTime / 1000);
            sb3.append("(");
            rawRealtime3 = phoneOnTime;
            sb3.append(batteryStats.formatRatioLocked(rawRealtime3, batteryUptime));
            sb3.append(") ");
            sb3.append(batteryStats.getPhoneOnCount(i5));
            sb3.append("x");
        } else {
            rawRealtime3 = phoneOnTime;
        }
        i8 = batteryStats.getNumConnectivityChange(i5);
        long phoneOnTime2;
        if (i8 != 0) {
            pw.print(prefix);
            phoneOnTime2 = rawRealtime3;
            printWriter2.print("  Connectivity changes: ");
            printWriter2.println(i8);
        } else {
            phoneOnTime2 = rawRealtime3;
        }
        int connChanges = i8;
        ArrayList<TimerEntry> timers = new ArrayList();
        powerSaveModeEnabledTime2 = 0;
        screenDozeTime = 0;
        NU3 = 0;
        while (true) {
            estimatedBatteryCapacity = NU;
            if (NU3 >= estimatedBatteryCapacity) {
                break;
            }
            NU2 = estimatedBatteryCapacity;
            SparseArray<? extends Uid> uidStats4 = uidStats3;
            uidStats = uidStats4;
            Uid NU4 = (Uid) uidStats4.valueAt(NU3);
            powerSaveModeEnabledTime = rawUptime;
            ArrayMap<String, ? extends Wakelock> wakelocks = NU4.getWakelockStats();
            if (wakelocks != null) {
                screenOnTime = totalRealtime;
                i6 = 1;
                iw = wakelocks.size() - 1;
                while (iw >= 0) {
                    Wakelock wl = (Wakelock) wakelocks.valueAt(iw);
                    whichBatteryRealtime = batteryUptime;
                    batteryUptime = wl.getWakeTime(i6);
                    if (batteryUptime != null) {
                        screenDozeTime += batteryUptime.getTotalTimeLocked(rawRealtimeMs, i5);
                    }
                    Timer partialWakeTimer = wl.getWakeTime(0);
                    if (partialWakeTimer != null) {
                        deviceIdleModeLightTime = partialWakeTimer.getTotalTimeLocked(rawRealtimeMs, i5);
                        if (deviceIdleModeLightTime > 0) {
                            if (reqUid < 0) {
                                timers.add(new TimerEntry((String) wakelocks.keyAt(iw), NU4.getUid(), partialWakeTimer, deviceIdleModeLightTime));
                            }
                            powerSaveModeEnabledTime2 += deviceIdleModeLightTime;
                            iw--;
                            batteryUptime = whichBatteryRealtime;
                            i6 = 1;
                        }
                    }
                    i = reqUid;
                    iw--;
                    batteryUptime = whichBatteryRealtime;
                    i6 = 1;
                }
                whichBatteryRealtime = batteryUptime;
            } else {
                whichBatteryRealtime = batteryUptime;
                screenOnTime = totalRealtime;
            }
            i = reqUid;
            NU3++;
            NU = NU2;
            uidStats3 = uidStats;
            rawUptime = powerSaveModeEnabledTime;
            totalRealtime = screenOnTime;
            batteryUptime = whichBatteryRealtime;
        }
        whichBatteryRealtime = batteryUptime;
        NU2 = estimatedBatteryCapacity;
        powerSaveModeEnabledTime = rawUptime;
        screenOnTime = totalRealtime;
        uidStats = uidStats3;
        i = reqUid;
        totalRealtime = batteryStats.getNetworkActivityBytes(0, i5);
        rawUptime = batteryStats.getNetworkActivityBytes(1, i5);
        long rawRealtime5 = rawRealtimeMs;
        long wifiRxTotalBytes = batteryStats.getNetworkActivityBytes(2, i5);
        rawRealtimeMs = batteryStats.getNetworkActivityBytes(3, i5);
        rawRealtime3 = batteryStats.getNetworkActivityPackets(0, i5);
        long wifiTxTotalBytes = rawRealtimeMs;
        rawRealtimeMs = batteryStats.getNetworkActivityPackets(1, i5);
        ArrayList<TimerEntry> timers2 = timers;
        long wifiRxTotalPackets = batteryStats.getNetworkActivityPackets(2, i5);
        long wifiTxTotalPackets2 = batteryStats.getNetworkActivityPackets(3, i5);
        long btRxTotalBytes = batteryStats.getNetworkActivityBytes(4, i5);
        long btTxTotalBytes2 = batteryStats.getNetworkActivityBytes(5, i5);
        if (screenDozeTime != 0) {
            sb3.setLength(0);
            sb3.append(str);
            sb3.append("  Total full wakelock time: ");
            btTxTotalBytes = btTxTotalBytes2;
            formatTimeMsNoSpace(sb3, (screenDozeTime + 500) / 1000);
            printWriter2.println(sb3.toString());
        } else {
            btTxTotalBytes = btTxTotalBytes2;
        }
        if (powerSaveModeEnabledTime2 != 0) {
            sb3.setLength(0);
            sb3.append(str);
            sb3.append("  Total partial wakelock time: ");
            formatTimeMsNoSpace(sb3, (powerSaveModeEnabledTime2 + 500) / 1000);
            printWriter2.println(sb3.toString());
        }
        batteryUptime = rawRealtime5;
        long multicastWakeLockTimeTotalMicros = batteryStats.getWifiMulticastWakelockTime(batteryUptime, i5);
        i8 = batteryStats.getWifiMulticastWakelockCount(i5);
        if (multicastWakeLockTimeTotalMicros != 0) {
            mobileTxTotalPackets = rawRealtimeMs;
            sb3.setLength(0);
            sb3.append(str);
            sb3.append("  Total WiFi Multicast wakelock Count: ");
            sb3.append(i8);
            printWriter2.println(sb3.toString());
            sb3.setLength(0);
            sb3.append(str);
            sb3.append("  Total WiFi Multicast wakelock time: ");
            formatTimeMsNoSpace(sb3, (multicastWakeLockTimeTotalMicros + 500) / 1000);
            printWriter2.println(sb3.toString());
        } else {
            mobileTxTotalPackets = rawRealtimeMs;
        }
        printWriter2.println("");
        pw.print(prefix);
        sb3.setLength(0);
        sb3.append(str);
        sb3.append("  CONNECTIVITY POWER SUMMARY START");
        printWriter2.println(sb3.toString());
        pw.print(prefix);
        sb3.setLength(0);
        sb3.append(str);
        sb3.append("  Logging duration for connectivity statistics: ");
        formatTimeMs(sb3, whichBatteryRealtime / 1000);
        printWriter2.println(sb3.toString());
        sb3.setLength(0);
        sb3.append(str);
        sb3.append("  Cellular Statistics:");
        printWriter2.println(sb3.toString());
        pw.print(prefix);
        sb3.setLength(0);
        sb3.append(str);
        sb3.append("     Cellular kernel active time: ");
        rawRealtimeMs = batteryStats.getMobileRadioActiveTime(batteryUptime, i5);
        int multicastWakeLockCountTotal = i8;
        long rawRealtime6 = batteryUptime;
        formatTimeMs(sb3, rawRealtimeMs / 1000);
        sb3.append("(");
        batteryUptime = whichBatteryRealtime;
        sb3.append(batteryStats.formatRatioLocked(rawRealtimeMs, batteryUptime));
        sb3.append(")");
        printWriter2.println(sb3.toString());
        printWriter2.print("     Cellular data received: ");
        printWriter2.println(batteryStats.formatBytesLocked(totalRealtime));
        printWriter2.print("     Cellular data sent: ");
        printWriter2.println(batteryStats.formatBytesLocked(rawUptime));
        printWriter2.print("     Cellular packets received: ");
        printWriter2.println(rawRealtime3);
        printWriter2.print("     Cellular packets sent: ");
        long mobileActiveTime = rawRealtimeMs;
        rawRealtimeMs = mobileTxTotalPackets;
        printWriter2.println(rawRealtimeMs);
        sb3.setLength(0);
        sb3.append(str);
        sb3.append("     Cellular Radio Access Technology:");
        boolean didOne2 = false;
        i8 = 0;
        while (true) {
            mobileTxTotalPackets2 = rawRealtimeMs;
            if (i8 >= 21) {
                break;
            }
            mobileRxTotalPackets = rawRealtime3;
            rawRealtimeMs = rawRealtime6;
            rawRealtime3 = batteryStats.getPhoneDataConnectionTime(i8, rawRealtimeMs, i5);
            if (rawRealtime3 == 0) {
                mobileTxTotalBytes = rawUptime;
            } else {
                mobileTxTotalBytes = rawUptime;
                sb3.append("\n       ");
                sb3.append(str);
                sb3.append(DATA_CONNECTION_NAMES[i8]);
                sb3.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                formatTimeMs(sb3, rawRealtime3 / true);
                sb3.append("(");
                sb3.append(batteryStats.formatRatioLocked(rawRealtime3, batteryUptime));
                sb3.append(") ");
                didOne2 = true;
            }
            i8++;
            rawRealtime6 = rawRealtimeMs;
            rawRealtimeMs = mobileTxTotalPackets2;
            rawRealtime3 = mobileRxTotalPackets;
            rawUptime = mobileTxTotalBytes;
        }
        mobileRxTotalPackets = rawRealtime3;
        mobileTxTotalBytes = rawUptime;
        rawRealtimeMs = rawRealtime6;
        if (!didOne2) {
            sb3.append(" (no activity)");
        }
        printWriter2.println(sb3.toString());
        sb3.setLength(0);
        sb3.append(str);
        sb3.append("     Cellular Rx signal strength (RSRP):");
        String[] cellularRxSignalStrengthDescription = new String[]{"very poor (less than -128dBm): ", "poor (-128dBm to -118dBm): ", "moderate (-118dBm to -108dBm): ", "good (-108dBm to -98dBm): ", "great (greater than -98dBm): "};
        int numCellularRxBins = Math.min(5, cellularRxSignalStrengthDescription.length);
        didOne2 = false;
        i8 = 0;
        while (i8 < numCellularRxBins) {
            rawRealtime3 = batteryStats.getPhoneSignalStrengthTime(i8, rawRealtimeMs, i5);
            if (rawRealtime3 == 0) {
                rawRealtime = rawRealtimeMs;
            } else {
                rawRealtime = rawRealtimeMs;
                sb3.append("\n       ");
                sb3.append(str);
                sb3.append(cellularRxSignalStrengthDescription[i8]);
                sb3.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                formatTimeMs(sb3, rawRealtime3 / 1000);
                sb3.append("(");
                sb3.append(batteryStats.formatRatioLocked(rawRealtime3, batteryUptime));
                sb3.append(") ");
                didOne2 = 1;
            }
            i8++;
            rawRealtimeMs = rawRealtime;
        }
        rawRealtime = rawRealtimeMs;
        if (!didOne2) {
            sb3.append(" (no activity)");
        }
        printWriter2.println(sb3.toString());
        int numCellularRxBins2 = numCellularRxBins;
        String[] cellularRxSignalStrengthDescription2 = cellularRxSignalStrengthDescription;
        long mobileRxTotalBytes = totalRealtime;
        totalRealtime = wifiRxTotalPackets;
        long wifiTxTotalPackets3 = wifiTxTotalPackets2;
        long btRxTotalBytes2 = btRxTotalBytes;
        long btTxTotalBytes3 = btTxTotalBytes;
        long whichBatteryRealtime3 = batteryUptime;
        long batteryUptime3 = batteryUptime2;
        long wifiRxTotalBytes2 = wifiRxTotalBytes;
        ArrayList<TimerEntry> timers3 = timers2;
        long rawRealtimeMs3 = rawRealtimeMs2;
        long wifiTxTotalBytes2 = wifiTxTotalBytes;
        long mobileActiveTime2 = mobileActiveTime;
        long rawRealtime7 = rawRealtime;
        int NU5 = NU2;
        SparseArray<? extends Uid> uidStats5 = uidStats;
        batteryStats.printControllerActivity(printWriter2, sb3, str, CELLULAR_CONTROLLER_NAME, getModemControllerActivity(), i5);
        pw.print(prefix);
        sb3.setLength(0);
        sb3.append(str);
        sb3.append("  Wifi Statistics:");
        printWriter2.println(sb3.toString());
        pw.print(prefix);
        sb3.setLength(0);
        sb3.append(str);
        sb3.append("     Wifi kernel active time: ");
        rawRealtime3 = rawRealtime7;
        rawRealtimeMs = batteryStats.getWifiActiveTime(rawRealtime3, i5);
        formatTimeMs(sb3, rawRealtimeMs / 1000);
        sb3.append("(");
        batteryUptime = whichBatteryRealtime3;
        sb3.append(batteryStats.formatRatioLocked(rawRealtimeMs, batteryUptime));
        sb3.append(")");
        printWriter2.println(sb3.toString());
        printWriter2.print("     Wifi data received: ");
        rawUptime = wifiRxTotalBytes2;
        printWriter2.println(batteryStats.formatBytesLocked(rawUptime));
        printWriter2.print("     Wifi data sent: ");
        long wifiRxTotalBytes3 = rawUptime;
        rawUptime = wifiTxTotalBytes2;
        printWriter2.println(batteryStats.formatBytesLocked(rawUptime));
        printWriter2.print("     Wifi packets received: ");
        printWriter2.println(totalRealtime);
        printWriter2.print("     Wifi packets sent: ");
        long wifiTxTotalBytes3 = rawUptime;
        rawUptime = wifiTxTotalPackets3;
        printWriter2.println(rawUptime);
        sb3.setLength(0);
        sb3.append(str);
        sb3.append("     Wifi states:");
        didOne2 = false;
        i8 = 0;
        while (true) {
            long wifiActiveTime = rawRealtimeMs;
            if (i8 >= 8) {
                break;
            }
            rawRealtimeMs = batteryStats.getWifiStateTime(i8, rawRealtime3, i5);
            if (rawRealtimeMs == 0) {
                wifiTxTotalPackets = rawUptime;
            } else {
                wifiTxTotalPackets = rawUptime;
                sb3.append("\n       ");
                sb3.append(WIFI_STATE_NAMES[i8]);
                sb3.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                formatTimeMs(sb3, rawRealtimeMs / 1000);
                sb3.append("(");
                sb3.append(batteryStats.formatRatioLocked(rawRealtimeMs, batteryUptime));
                sb3.append(") ");
                didOne2 = 1;
            }
            i8++;
            rawRealtimeMs = wifiActiveTime;
            rawUptime = wifiTxTotalPackets;
        }
        wifiTxTotalPackets = rawUptime;
        if (!didOne2) {
            sb3.append(" (no activity)");
        }
        printWriter2.println(sb3.toString());
        sb3.setLength(0);
        sb3.append(str);
        sb3.append("     Wifi supplicant states:");
        boolean didOne3 = false;
        for (i8 = 0; i8 < 13; i8++) {
            rawUptime = batteryStats.getWifiSupplStateTime(i8, rawRealtime3, i5);
            if (rawUptime != 0) {
                sb3.append("\n       ");
                sb3.append(WIFI_SUPPL_STATE_NAMES[i8]);
                sb3.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                formatTimeMs(sb3, rawUptime / true);
                sb3.append("(");
                sb3.append(batteryStats.formatRatioLocked(rawUptime, batteryUptime));
                sb3.append(") ");
                didOne3 = true;
            }
        }
        if (!didOne3) {
            sb3.append(" (no activity)");
        }
        printWriter2.println(sb3.toString());
        sb3.setLength(0);
        sb3.append(str);
        sb3.append("     Wifi Rx signal strength (RSSI):");
        cellularRxSignalStrengthDescription = new String[]{"very poor (less than -88.75dBm): ", "poor (-88.75 to -77.5dBm): ", "moderate (-77.5dBm to -66.25dBm): ", "good (-66.25dBm to -55dBm): ", "great (greater than -55dBm): "};
        minLearnedBatteryCapacity = Math.min(5, cellularRxSignalStrengthDescription.length);
        didOne2 = false;
        i8 = 0;
        while (i8 < minLearnedBatteryCapacity) {
            wifiRxSignalStrengthDescription = cellularRxSignalStrengthDescription;
            rawUptime = batteryStats.getWifiSignalStrengthTime(i8, rawRealtime3, i5);
            if (rawUptime == 0) {
                numWifiRxBins = minLearnedBatteryCapacity;
            } else {
                sb3.append("\n    ");
                sb3.append(str);
                sb3.append("     ");
                sb3.append(wifiRxSignalStrengthDescription[i8]);
                numWifiRxBins = minLearnedBatteryCapacity;
                formatTimeMs(sb3, rawUptime / 1000);
                sb3.append("(");
                sb3.append(batteryStats.formatRatioLocked(rawUptime, batteryUptime));
                sb3.append(") ");
                didOne2 = true;
            }
            i8++;
            cellularRxSignalStrengthDescription = wifiRxSignalStrengthDescription;
            minLearnedBatteryCapacity = numWifiRxBins;
        }
        numWifiRxBins = minLearnedBatteryCapacity;
        wifiRxSignalStrengthDescription = cellularRxSignalStrengthDescription;
        if (!didOne2) {
            sb3.append(" (no activity)");
        }
        printWriter2.println(sb3.toString());
        long wifiRxTotalPackets2 = totalRealtime;
        long whichBatteryRealtime4 = batteryUptime;
        long rawRealtime8 = rawRealtime3;
        batteryStats.printControllerActivity(printWriter2, sb3, str, WIFI_CONTROLLER_NAME, getWifiControllerActivity(), i5);
        pw.print(prefix);
        sb3.setLength(0);
        sb3.append(str);
        sb3.append("  GPS Statistics:");
        printWriter2.println(sb3.toString());
        sb3.setLength(0);
        sb3.append(str);
        sb3.append("     GPS signal quality (Top 4 Average CN0):");
        String[] gpsSignalQualityDescription2 = new String[]{"poor (less than 20 dBHz): ", "good (greater than 20 dBHz): "};
        estimatedBatteryCapacity = Math.min(2, gpsSignalQualityDescription2.length);
        for (i8 = 0; i8 < estimatedBatteryCapacity; i8++) {
            batteryUptime = batteryStats.getGpsSignalQualityTime(i8, rawRealtime8, i5);
            sb3.append("\n    ");
            sb3.append(str);
            sb3.append("  ");
            sb3.append(gpsSignalQualityDescription2[i8]);
            formatTimeMs(sb3, batteryUptime / 1000);
            sb3.append("(");
            sb3.append(batteryStats.formatRatioLocked(batteryUptime, whichBatteryRealtime4));
            sb3.append(") ");
        }
        long whichBatteryRealtime5 = whichBatteryRealtime4;
        printWriter2.println(sb3.toString());
        long gpsBatteryDrainMaMs = getGpsBatteryDrainMaMs();
        if (gpsBatteryDrainMaMs > 0) {
            pw.print(prefix);
            sb3.setLength(0);
            sb3.append(str);
            sb3.append("     Battery Drain (mAh): ");
            sb3.append(Double.toString(((double) gpsBatteryDrainMaMs) / 3600000.0d));
            printWriter2.println(sb3.toString());
        }
        pw.print(prefix);
        sb3.setLength(0);
        sb3.append(str);
        sb3.append("  CONNECTIVITY POWER SUMMARY END");
        printWriter2.println(sb3.toString());
        printWriter2.println("");
        pw.print(prefix);
        printWriter2.print("  Bluetooth total received: ");
        btTxTotalBytes2 = btRxTotalBytes2;
        printWriter2.print(batteryStats.formatBytesLocked(btTxTotalBytes2));
        printWriter2.print(", sent: ");
        long whichBatteryRealtime6 = whichBatteryRealtime5;
        int numGpsSignalQualityBins = estimatedBatteryCapacity;
        rawRealtime3 = btTxTotalBytes3;
        printWriter2.println(batteryStats.formatBytesLocked(rawRealtime3));
        long rawRealtime9 = rawRealtime8;
        rawUptime = batteryStats.getBluetoothScanTime(rawRealtime8, i5) / 1000;
        sb3.setLength(0);
        sb3.append(str);
        sb3.append("  Bluetooth scan time: ");
        formatTimeMs(sb3, rawUptime);
        printWriter2.println(sb3.toString());
        long bluetoothScanTimeMs = rawUptime;
        rawUptime = whichBatteryRealtime6;
        batteryStats.printControllerActivity(printWriter2, sb3, str, "Bluetooth", getBluetoothControllerActivity(), i5);
        pw.println();
        if (i5 == 2) {
            if (getIsOnBattery()) {
                pw.print(prefix);
                printWriter2.println("  Device is currently unplugged");
                pw.print(prefix);
                printWriter2.print("    Discharge cycle start level: ");
                printWriter2.println(getDischargeStartLevel());
                pw.print(prefix);
                printWriter2.print("    Discharge cycle current level: ");
                printWriter2.println(getDischargeCurrentLevel());
            } else {
                pw.print(prefix);
                printWriter2.println("  Device is currently plugged into power");
                pw.print(prefix);
                printWriter2.print("    Last discharge cycle start level: ");
                printWriter2.println(getDischargeStartLevel());
                pw.print(prefix);
                printWriter2.print("    Last discharge cycle end level: ");
                printWriter2.println(getDischargeCurrentLevel());
            }
            pw.print(prefix);
            printWriter2.print("    Amount discharged while screen on: ");
            printWriter2.println(getDischargeAmountScreenOn());
            pw.print(prefix);
            printWriter2.print("    Amount discharged while screen off: ");
            printWriter2.println(getDischargeAmountScreenOff());
            pw.print(prefix);
            printWriter2.print("    Amount discharged while screen doze: ");
            printWriter2.println(getDischargeAmountScreenDoze());
            printWriter2.println(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        } else {
            pw.print(prefix);
            printWriter2.println("  Device battery use since last full charge");
            pw.print(prefix);
            printWriter2.print("    Amount discharged (lower bound): ");
            printWriter2.println(getLowDischargeAmountSinceCharge());
            pw.print(prefix);
            printWriter2.print("    Amount discharged (upper bound): ");
            printWriter2.println(getHighDischargeAmountSinceCharge());
            pw.print(prefix);
            printWriter2.print("    Amount discharged while screen on: ");
            printWriter2.println(getDischargeAmountScreenOnSinceCharge());
            pw.print(prefix);
            printWriter2.print("    Amount discharged while screen off: ");
            printWriter2.println(getDischargeAmountScreenOffSinceCharge());
            pw.print(prefix);
            printWriter2.print("    Amount discharged while screen doze: ");
            printWriter2.println(getDischargeAmountScreenDozeSinceCharge());
            pw.println();
        }
        BatteryStatsHelper helper = new BatteryStatsHelper(context, false, wifiOnly);
        helper.create(batteryStats);
        helper.refreshStats(i5, -1);
        List<BatterySipper> sippers2 = helper.getUsageList();
        if (sippers2 == null || sippers2.size() <= 0) {
            gpsSignalQualityDescription = gpsSignalQualityDescription2;
        } else {
            pw.print(prefix);
            printWriter2.println("  Estimated power use (mAh):");
            pw.print(prefix);
            printWriter2.print("    Capacity: ");
            batteryStats.printmAh(printWriter2, helper.getPowerProfile().getBatteryCapacity());
            printWriter2.print(", Computed drain: ");
            batteryStats.printmAh(printWriter2, helper.getComputedPower());
            printWriter2.print(", actual drain: ");
            batteryStats.printmAh(printWriter2, helper.getMinDrainedPower());
            if (helper.getMinDrainedPower() != helper.getMaxDrainedPower()) {
                printWriter2.print("-");
                batteryStats.printmAh(printWriter2, helper.getMaxDrainedPower());
            }
            pw.println();
            i2 = 0;
            while (i2 < sippers2.size()) {
                BatterySipper bs = (BatterySipper) sippers2.get(i2);
                pw.print(prefix);
                switch (AnonymousClass2.$SwitchMap$com$android$internal$os$BatterySipper$DrainType[bs.drainType.ordinal()]) {
                    case 1:
                        printWriter2.print("    Ambient display: ");
                        break;
                    case 2:
                        printWriter2.print("    Idle: ");
                        break;
                    case 3:
                        printWriter2.print("    Cell standby: ");
                        break;
                    case 4:
                        printWriter2.print("    Phone calls: ");
                        break;
                    case 5:
                        printWriter2.print("    Wifi: ");
                        break;
                    case 6:
                        printWriter2.print("    Bluetooth: ");
                        break;
                    case 7:
                        printWriter2.print("    Screen: ");
                        break;
                    case 8:
                        printWriter2.print("    Flashlight: ");
                        break;
                    case 9:
                        printWriter2.print("    Uid ");
                        UserHandle.formatUid(printWriter2, bs.uidObj.getUid());
                        printWriter2.print(": ");
                        break;
                    case 10:
                        printWriter2.print("    User ");
                        printWriter2.print(bs.userId);
                        printWriter2.print(": ");
                        break;
                    case 11:
                        printWriter2.print("    Unaccounted: ");
                        break;
                    case 12:
                        printWriter2.print("    Over-counted: ");
                        break;
                    case 13:
                        printWriter2.print("    Camera: ");
                        break;
                    default:
                        printWriter2.print("    ???: ");
                        break;
                }
                batteryStats.printmAh(printWriter2, bs.totalPowerMah);
                gpsSignalQualityDescription = gpsSignalQualityDescription2;
                if (bs.usagePowerMah != bs.totalPowerMah) {
                    printWriter2.print(" (");
                    if (bs.usagePowerMah != 0.0d) {
                        printWriter2.print(" usage=");
                        batteryStats.printmAh(printWriter2, bs.usagePowerMah);
                    }
                    if (bs.cpuPowerMah != 0.0d) {
                        printWriter2.print(" cpu=");
                        batteryStats.printmAh(printWriter2, bs.cpuPowerMah);
                    }
                    if (bs.wakeLockPowerMah != 0.0d) {
                        printWriter2.print(" wake=");
                        batteryStats.printmAh(printWriter2, bs.wakeLockPowerMah);
                    }
                    if (bs.mobileRadioPowerMah != 0.0d) {
                        printWriter2.print(" radio=");
                        batteryStats.printmAh(printWriter2, bs.mobileRadioPowerMah);
                    }
                    if (bs.wifiPowerMah != 0.0d) {
                        printWriter2.print(" wifi=");
                        batteryStats.printmAh(printWriter2, bs.wifiPowerMah);
                    }
                    if (bs.bluetoothPowerMah != 0.0d) {
                        printWriter2.print(" bt=");
                        batteryStats.printmAh(printWriter2, bs.bluetoothPowerMah);
                    }
                    if (bs.gpsPowerMah != 0.0d) {
                        printWriter2.print(" gps=");
                        batteryStats.printmAh(printWriter2, bs.gpsPowerMah);
                    }
                    if (bs.sensorPowerMah != 0.0d) {
                        printWriter2.print(" sensor=");
                        batteryStats.printmAh(printWriter2, bs.sensorPowerMah);
                    }
                    if (bs.cameraPowerMah != 0.0d) {
                        printWriter2.print(" camera=");
                        batteryStats.printmAh(printWriter2, bs.cameraPowerMah);
                    }
                    if (bs.flashlightPowerMah != 0.0d) {
                        printWriter2.print(" flash=");
                        batteryStats.printmAh(printWriter2, bs.flashlightPowerMah);
                    }
                    printWriter2.print(" )");
                }
                if (bs.totalSmearedPowerMah != bs.totalPowerMah) {
                    printWriter2.print(" Including smearing: ");
                    batteryStats.printmAh(printWriter2, bs.totalSmearedPowerMah);
                    printWriter2.print(" (");
                    if (bs.screenPowerMah != 0.0d) {
                        printWriter2.print(" screen=");
                        batteryStats.printmAh(printWriter2, bs.screenPowerMah);
                    }
                    if (bs.proportionalSmearMah != 0.0d) {
                        printWriter2.print(" proportional=");
                        batteryStats.printmAh(printWriter2, bs.proportionalSmearMah);
                    }
                    printWriter2.print(" )");
                }
                if (bs.shouldHide) {
                    printWriter2.print(" Excluded from smearing");
                }
                pw.println();
                i2++;
                gpsSignalQualityDescription2 = gpsSignalQualityDescription;
                didOne = wifiOnly;
                Context context2 = context;
            }
            gpsSignalQualityDescription = gpsSignalQualityDescription2;
            pw.println();
        }
        List<BatterySipper> sippers3 = helper.getMobilemsppList();
        if (sippers3 == null || sippers3.size() <= 0) {
            sippers = sippers3;
        } else {
            pw.print(prefix);
            printWriter2.println("  Per-app mobile ms per packet:");
            batteryUptime = 0;
            i8 = 0;
            while (i8 < sippers3.size()) {
                BatterySipper bs2 = (BatterySipper) sippers3.get(i8);
                sb3.setLength(0);
                sb3.append(str);
                sb3.append("    Uid ");
                UserHandle.formatUid(sb3, bs2.uidObj.getUid());
                sb3.append(": ");
                sb3.append(BatteryStatsHelper.makemAh(bs2.mobilemspp));
                sb3.append(" (");
                sippers = sippers3;
                sb3.append(bs2.mobileRxPackets + bs2.mobileTxPackets);
                sb3.append(" packets over ");
                formatTimeMsNoSpace(sb3, bs2.mobileActive);
                sb3.append(") ");
                sb3.append(bs2.mobileActiveCount);
                sb3.append("x");
                printWriter2.println(sb3.toString());
                batteryUptime += bs2.mobileActive;
                i8++;
                sippers3 = sippers;
            }
            sippers = sippers3;
            sb3.setLength(0);
            sb3.append(str);
            sb3.append("    TOTAL TIME: ");
            formatTimeMs(sb3, batteryUptime);
            sb3.append("(");
            sb3.append(batteryStats.formatRatioLocked(batteryUptime, rawUptime));
            sb3.append(")");
            printWriter2.println(sb3.toString());
            pw.println();
        }
        Comparator<TimerEntry> timerComparator = new Comparator<TimerEntry>() {
            public int compare(TimerEntry lhs, TimerEntry rhs) {
                long lhsTime = lhs.mTime;
                long rhsTime = rhs.mTime;
                if (lhsTime < rhsTime) {
                    return 1;
                }
                if (lhsTime > rhsTime) {
                    return -1;
                }
                return 0;
            }
        };
        if (i < 0) {
            ArrayList<TimerEntry> ktimers;
            ArrayList<TimerEntry> ktimers2;
            Map<String, ? extends Timer> kernelWakelocks2 = getKernelWakelockStats();
            if (kernelWakelocks2 == null || kernelWakelocks2.size() <= 0) {
                rawRealtime2 = rawRealtime9;
                i3 = -1;
            } else {
                BatteryStatsHelper helper2;
                ktimers = new ArrayList();
                for (Entry<String, ? extends Timer> ent2 : kernelWakelocks2.entrySet()) {
                    timer = (Timer) ent2.getValue();
                    helper2 = helper;
                    helper = rawRealtime9;
                    whichBatteryRealtime = computeWakeLock(timer, helper, i5);
                    if (whichBatteryRealtime > 0) {
                        ktimers.add(new TimerEntry((String) ent2.getKey(), 0, timer, whichBatteryRealtime));
                    }
                    rawRealtime9 = helper;
                    helper = helper2;
                }
                helper2 = helper;
                helper = rawRealtime9;
                BatteryStatsHelper helper3;
                if (ktimers.size() > 0) {
                    Collections.sort(ktimers, timerComparator);
                    pw.print(prefix);
                    printWriter2.println("  All kernel wake locks:");
                    i8 = 0;
                    while (true) {
                        i4 = i8;
                        if (i4 < ktimers.size()) {
                            TimerEntry timer2 = (TimerEntry) ktimers.get(i4);
                            sb3.setLength(0);
                            sb3.append(str);
                            sb3.append("  Kernel Wake lock ");
                            sb3.append(timer2.mName);
                            connChanges = i4;
                            rawRealtime2 = helper;
                            helper3 = helper2;
                            ktimers2 = ktimers;
                            if (!printWakeLock(sb3, timer2.mTimer, helper, null, i5, ": ").equals(": ")) {
                                sb3.append(" realtime");
                                printWriter2.println(sb3.toString());
                            }
                            i8 = connChanges + 1;
                            ktimers = ktimers2;
                            helper2 = helper3;
                            helper = rawRealtime2;
                        } else {
                            rawRealtime2 = helper;
                            ktimers2 = ktimers;
                            helper3 = helper2;
                            i3 = -1;
                            pw.println();
                        }
                    }
                } else {
                    rawRealtime2 = helper;
                    helper3 = helper2;
                    i3 = -1;
                }
            }
            ktimers = timers3;
            if (ktimers.size() > 0) {
                Collections.sort(ktimers, timerComparator);
                pw.print(prefix);
                printWriter2.println("  All partial wake locks:");
                i8 = 0;
                while (true) {
                    NU3 = i8;
                    if (NU3 < ktimers.size()) {
                        TimerEntry helper4 = (TimerEntry) ktimers.get(NU3);
                        sb3.setLength(0);
                        sb3.append("  Wake lock ");
                        UserHandle.formatUid(sb3, helper4.mId);
                        sb3.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                        sb3.append(helper4.mName);
                        TimerEntry timer3 = helper4;
                        int i9 = NU3;
                        Map<String, ? extends Timer> kernelWakelocks3 = kernelWakelocks2;
                        kernelWakelocks = ktimers;
                        printWakeLock(sb3, helper4.mTimer, rawRealtime2, null, i5, ": ");
                        sb3.append(" realtime");
                        printWriter2.println(sb3.toString());
                        i8 = i9 + 1;
                        ktimers = kernelWakelocks;
                        kernelWakelocks2 = kernelWakelocks3;
                    } else {
                        kernelWakelocks = ktimers;
                        kernelWakelocks.clear();
                        pw.println();
                    }
                }
            } else {
                kernelWakelocks = ktimers;
            }
            Map<String, ? extends Timer> wakeupReasons = getWakeupReasonStats();
            if (wakeupReasons != null && wakeupReasons.size() > 0) {
                pw.print(prefix);
                printWriter2.println("  All wakeup reasons:");
                ArrayList<TimerEntry> reasons = new ArrayList();
                it = wakeupReasons.entrySet().iterator();
                while (it.hasNext()) {
                    ent2 = (Entry) it.next();
                    timer = (Timer) ent2.getValue();
                    Iterator it2 = it;
                    reasons.add(new TimerEntry((String) ent2.getKey(), 0, timer, (long) timer.getCountLocked(i5)));
                    it = it2;
                }
                Collections.sort(reasons, timerComparator);
                i8 = 0;
                while (true) {
                    helper = i8;
                    Map<String, ? extends Timer> wakeupReasons2;
                    if (helper < reasons.size()) {
                        TimerEntry timer4 = (TimerEntry) reasons.get(helper);
                        String linePrefix = ": ";
                        sb3.setLength(0);
                        sb3.append(str);
                        sb3.append("  Wakeup reason ");
                        sb3.append(timer4.mName);
                        TimerEntry timerEntry = timer4;
                        int i10 = helper;
                        ktimers2 = reasons;
                        wakeupReasons2 = wakeupReasons;
                        printWakeLock(sb3, timer4.mTimer, rawRealtime2, null, i5, ": ");
                        sb3.append(" realtime");
                        printWriter2.println(sb3.toString());
                        i8 = i10 + 1;
                        reasons = ktimers2;
                        wakeupReasons = wakeupReasons2;
                    } else {
                        wakeupReasons2 = wakeupReasons;
                        pw.println();
                    }
                }
            }
        } else {
            kernelWakelocks = timers3;
            rawRealtime2 = rawRealtime9;
            i3 = -1;
        }
        LongSparseArray<? extends Timer> mMemoryStats2 = getKernelMemoryStats();
        if (mMemoryStats2.size() > 0) {
            printWriter2.println("  Memory Stats");
            for (i8 = 0; i8 < mMemoryStats2.size(); i8++) {
                sb3.setLength(0);
                sb3.append("  Bandwidth ");
                sb3.append(mMemoryStats2.keyAt(i8));
                sb3.append(" Time ");
                sb3.append(((Timer) mMemoryStats2.valueAt(i8)).getTotalTimeLocked(rawRealtime2, i5));
                printWriter2.println(sb3.toString());
            }
            rawRealtimeMs = rawRealtime2;
            pw.println();
        } else {
            rawRealtimeMs = rawRealtime2;
        }
        Map<String, ? extends Timer> rpmStats = getRpmStats();
        long interactiveTime2;
        Comparator<TimerEntry> timerComparator2;
        ArrayList<TimerEntry> timers4;
        String[] strArr;
        long j;
        long j2;
        String[] strArr2;
        long j3;
        long j4;
        String[] strArr3;
        List<BatterySipper> list;
        if (rpmStats.size() > 0) {
            pw.print(prefix);
            printWriter2.println("  Resource Power Manager Stats");
            if (rpmStats.size() > 0) {
                for (Entry<String, ? extends Timer> ent22 : rpmStats.entrySet()) {
                    sb = sb3;
                    mMemoryStats = mMemoryStats2;
                    interactiveTime2 = interactiveTime;
                    interactiveTime = dischargeScreenOffCount;
                    dischargeScreenOffCount = whichBatteryScreenOffRealtime;
                    mMemoryStats2 = null;
                    timerComparator2 = timerComparator;
                    timers4 = kernelWakelocks;
                    whichBatteryRealtime2 = rawUptime;
                    whichBatteryScreenOffRealtime = dischargeScreenDozeCount;
                    dischargeScreenDozeCount = screenOnTime;
                    printTimer(printWriter2, sb, (Timer) ent22.getValue(), rawRealtimeMs, which, prefix, (String) ent22.getKey());
                    printWriter2 = pw;
                    kernelWakelocks = timers4;
                    timerComparator = timerComparator2;
                    sb3 = sb;
                    mMemoryStats2 = mMemoryStats;
                    rawUptime = whichBatteryRealtime2;
                    timers4 = reqUid;
                    screenOnTime = dischargeScreenDozeCount;
                    dischargeScreenDozeCount = whichBatteryScreenOffRealtime;
                    whichBatteryScreenOffRealtime = dischargeScreenOffCount;
                    dischargeScreenOffCount = interactiveTime;
                    interactiveTime = interactiveTime2;
                }
            }
            mMemoryStats = mMemoryStats2;
            sb = sb3;
            whichBatteryRealtime2 = rawUptime;
            timerComparator2 = timerComparator;
            timers4 = kernelWakelocks;
            interactiveTime2 = interactiveTime;
            whichBatteryRealtime = mobileTxTotalBytes;
            strArr = cellularRxSignalStrengthDescription2;
            NU2 = numCellularRxBins2;
            rawRealtime5 = wifiRxTotalBytes3;
            j = wifiTxTotalBytes3;
            j2 = wifiTxTotalPackets;
            strArr2 = wifiRxSignalStrengthDescription;
            j3 = wifiRxTotalPackets2;
            j4 = bluetoothScanTimeMs;
            strArr3 = gpsSignalQualityDescription;
            list = sippers;
            estimatedBatteryCapacity = 0;
            interactiveTime = dischargeScreenOffCount;
            dischargeScreenOffCount = whichBatteryScreenOffRealtime;
            whichBatteryScreenOffRealtime = dischargeScreenDozeCount;
            dischargeScreenDozeCount = screenOnTime;
            screenOnTime = mobileRxTotalBytes;
            pw.println();
        } else {
            mMemoryStats = mMemoryStats2;
            sb = sb3;
            whichBatteryRealtime2 = rawUptime;
            timerComparator2 = timerComparator;
            timers4 = kernelWakelocks;
            interactiveTime2 = interactiveTime;
            whichBatteryRealtime = mobileTxTotalBytes;
            strArr = cellularRxSignalStrengthDescription2;
            NU2 = numCellularRxBins2;
            rawRealtime5 = wifiRxTotalBytes3;
            j = wifiTxTotalBytes3;
            j2 = wifiTxTotalPackets;
            strArr2 = wifiRxSignalStrengthDescription;
            j3 = wifiRxTotalPackets2;
            j4 = bluetoothScanTimeMs;
            strArr3 = gpsSignalQualityDescription;
            list = sippers;
            estimatedBatteryCapacity = 0;
        }
        long[] cpuFreqs = getCpuFreqs();
        if (cpuFreqs != null) {
            sb2 = sb;
            sb2.setLength(estimatedBatteryCapacity);
            sb2.append("  CPU freqs:");
            for (i8 = estimatedBatteryCapacity; i8 < cpuFreqs.length; i8++) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                stringBuilder.append(cpuFreqs[i8]);
                sb2.append(stringBuilder.toString());
            }
            printWriter = pw;
            printWriter.println(sb2.toString());
            pw.println();
        } else {
            printWriter = pw;
            sb2 = sb;
        }
        i8 = estimatedBatteryCapacity;
        while (true) {
            i6 = i8;
            int NU6 = NU5;
            Map<String, ? extends Timer> rpmStats2;
            LongSparseArray<? extends Timer> mMemoryStats3;
            SparseArray<? extends Uid> uidStats6;
            long[] cpuFreqs2;
            int NU7;
            long rawRealtimeMs4;
            PrintWriter printWriter3;
            String str2;
            if (i6 < NU6) {
                long rawRealtime10;
                int iu;
                long rawRealtime11;
                SparseArray<? extends Uid> uidStats7 = uidStats5;
                numCellularRxBins = uidStats7.keyAt(i6);
                int i11 = reqUid;
                if (i11 < 0 || numCellularRxBins == i11 || numCellularRxBins == 1000) {
                    long mobileTxBytes;
                    long wifiWakeup;
                    long wifiRxPackets;
                    BatteryStats batteryStats2;
                    long mobileRxBytes;
                    long mobileTxBytes2;
                    int wifiScanCountBg;
                    long packets;
                    long mobileActiveTime3;
                    long mobileTxPackets;
                    long mobileRxPackets;
                    String str3;
                    StringBuilder stringBuilder2;
                    BatteryStats batteryStats3;
                    StringBuilder sb4;
                    Uid u;
                    long mobileWakeup;
                    int uidMobileActiveCount;
                    long uidMobileActiveTime;
                    String str4;
                    PrintWriter printWriter4;
                    long rawRealtime12;
                    long wifiTxBytes;
                    long wifiWakeup2;
                    long wifiScanActualTime;
                    Uid u2;
                    long uidWifiRunningTime;
                    long whichBatteryRealtime7;
                    StringBuilder sb5;
                    long fullWifiLockOnTime;
                    StringBuilder stringBuilder3;
                    long wifiWakeup3;
                    Uid u3;
                    PrintWriter printWriter5;
                    Timer bleTimer;
                    ArrayMap<String, ? extends Wakelock> wakelocks2;
                    int iw2;
                    Uid u4;
                    long rawRealtimeMs5;
                    PrintWriter countWakelock;
                    boolean needComma;
                    Uid u5;
                    ArrayMap<String, ? extends Timer> syncs;
                    long totalPartialWakelock;
                    ArrayMap<String, ? extends Timer> jobs;
                    long rawRealtime13;
                    ArrayMap<String, SparseIntArray> completions;
                    int uid;
                    long rawRealtime14;
                    StringBuilder sb6;
                    SparseArray<? extends Sensor> sensors;
                    int NSE;
                    StringBuilder sb7;
                    int uid2;
                    long rawRealtime15;
                    PrintWriter printWriter6;
                    SparseArray<? extends Sensor> sensors2;
                    boolean uidActivity;
                    boolean uidActivity2;
                    long systemCpuTimeUs;
                    long[] cpuFreqTimes;
                    long[] screenOffCpuFreqTimes;
                    ArrayMap<String, ? extends Proc> processStats;
                    Uid u6;
                    Uid u7;
                    ArrayMap<String, ? extends Pkg> packageStats;
                    long wifiScanActualTimeBg;
                    Uid u8 = (Uid) uidStats7.valueAt(i6);
                    pw.print(prefix);
                    printWriter.print("  ");
                    UserHandle.formatUid(printWriter, numCellularRxBins);
                    printWriter.println(SettingsStringUtil.DELIMITER);
                    boolean uidActivity3 = false;
                    i2 = which;
                    int iu2 = i6;
                    int NU8 = NU6;
                    totalRealtime = u8.getNetworkActivityBytes(estimatedBatteryCapacity, i2);
                    long mobileTxBytes3 = u8.getNetworkActivityBytes(1, i2);
                    int uid3 = numCellularRxBins;
                    SparseArray<? extends Uid> uidStats8 = uidStats7;
                    long wifiRxBytes = u8.getNetworkActivityBytes(2, i2);
                    long wifiTxBytes2 = u8.getNetworkActivityBytes(3, i2);
                    long btRxBytes = u8.getNetworkActivityBytes(4, i2);
                    long btTxBytes = u8.getNetworkActivityBytes(5, i2);
                    long mobileRxPackets2 = u8.getNetworkActivityPackets(0, i2);
                    StringBuilder sb8 = sb2;
                    long[] cpuFreqs3 = cpuFreqs;
                    long mobileTxPackets2 = u8.getNetworkActivityPackets(1, i2);
                    long wifiRxPackets2 = u8.getNetworkActivityPackets(2, i2);
                    long wifiTxPackets = u8.getNetworkActivityPackets(3, i2);
                    long uidMobileActiveTime2 = u8.getMobileRadioActiveTime(i2);
                    int uidMobileActiveCount2 = u8.getMobileRadioActiveCount(i2);
                    long uidMobileActiveTime3 = uidMobileActiveTime2;
                    long fullWifiLockOnTime2 = u8.getFullWifiLockTime(rawRealtimeMs, i2);
                    uidMobileActiveTime2 = u8.getWifiScanTime(rawRealtimeMs, i2);
                    int wifiScanCount = u8.getWifiScanCount(i2);
                    iw = u8.getWifiScanBackgroundCount(i2);
                    long wifiScanTime = uidMobileActiveTime2;
                    long wifiScanActualTime2 = u8.getWifiScanActualTime(rawRealtimeMs);
                    long wifiScanActualTimeBg2 = u8.getWifiScanBackgroundTime(rawRealtimeMs);
                    long uidWifiRunningTime2 = u8.getWifiRunningTime(rawRealtimeMs, i2);
                    uidMobileActiveTime2 = u8.getMobileRadioApWakeupCount(i2);
                    long rawRealtime16 = rawRealtimeMs;
                    Map<String, ? extends Timer> rpmStats3 = rpmStats;
                    whichBatteryRealtime5 = u8.getWifiRadioApWakeupCount(i2);
                    if (totalRealtime > 0 || mobileTxBytes3 > 0 || mobileRxPackets2 > 0) {
                        mobileTxBytes = mobileTxBytes3;
                        mobileTxBytes3 = mobileTxPackets2;
                    } else {
                        mobileTxBytes = mobileTxBytes3;
                        mobileTxBytes3 = mobileTxPackets2;
                        if (mobileTxBytes3 <= 0) {
                            wifiWakeup = whichBatteryRealtime5;
                            wifiRxPackets = wifiRxPackets2;
                            whichBatteryRealtime5 = mobileTxBytes;
                            batteryStats2 = this;
                            rawRealtimeMs = pw;
                            mobileRxBytes = totalRealtime;
                            totalRealtime = uidMobileActiveTime3;
                            if (totalRealtime <= 0 || uidMobileActiveCount2 > 0) {
                                mobileTxBytes2 = whichBatteryRealtime5;
                                stringBuilder = sb8;
                                stringBuilder.setLength(0);
                                stringBuilder.append(prefix);
                                stringBuilder.append("    Mobile radio active: ");
                                formatTimeMs(stringBuilder, totalRealtime / 1000);
                                stringBuilder.append("(");
                                wifiScanCountBg = iw;
                                whichBatteryRealtime5 = mobileActiveTime2;
                                stringBuilder.append(batteryStats2.formatRatioLocked(totalRealtime, whichBatteryRealtime5));
                                stringBuilder.append(") ");
                                stringBuilder.append(uidMobileActiveCount2);
                                stringBuilder.append("x");
                                packets = mobileRxPackets2 + mobileTxBytes3;
                                if (packets == 0) {
                                    packets = 1;
                                }
                                mobileActiveTime3 = whichBatteryRealtime5;
                                whichBatteryRealtime5 = packets;
                                stringBuilder.append(" @ ");
                                mobileTxPackets = mobileTxBytes3;
                                mobileRxPackets = mobileRxPackets2;
                                stringBuilder.append(BatteryStatsHelper.makemAh(((double) (totalRealtime / 1000)) / ((double) whichBatteryRealtime5)));
                                stringBuilder.append(" mspp");
                                rawRealtimeMs.println(stringBuilder.toString());
                            } else {
                                mobileTxBytes2 = whichBatteryRealtime5;
                                mobileTxPackets = mobileTxBytes3;
                                mobileRxPackets = mobileRxPackets2;
                                wifiScanCountBg = iw;
                                mobileActiveTime3 = mobileActiveTime2;
                                stringBuilder = sb8;
                            }
                            if (uidMobileActiveTime2 <= 0) {
                                estimatedBatteryCapacity = 0;
                                stringBuilder.setLength(0);
                                str3 = prefix;
                                stringBuilder.append(str3);
                                stringBuilder.append("    Mobile radio AP wakeups: ");
                                stringBuilder.append(uidMobileActiveTime2);
                                rawRealtimeMs.println(stringBuilder.toString());
                            } else {
                                str3 = prefix;
                                estimatedBatteryCapacity = 0;
                            }
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append(str3);
                            stringBuilder2.append("  ");
                            batteryStats3 = batteryStats2;
                            sb4 = stringBuilder;
                            u = u8;
                            mobileWakeup = uidMobileActiveTime2;
                            uidMobileActiveCount = uidMobileActiveCount2;
                            rawRealtime10 = rawRealtime16;
                            uidMobileActiveTime = totalRealtime;
                            totalRealtime = wifiWakeup;
                            wifiTxTotalPackets2 = mobileActiveTime3;
                            str4 = str3;
                            rpmStats2 = rpmStats3;
                            i5 = estimatedBatteryCapacity;
                            mMemoryStats3 = mMemoryStats;
                            batteryStats2.printControllerActivityIfInteresting(rawRealtimeMs, sb4, stringBuilder2.toString(), CELLULAR_CONTROLLER_NAME, u8.getModemControllerActivity(), which);
                            if (wifiRxBytes <= 0 || wifiTxBytes2 > 0) {
                                rawRealtimeMs = wifiTxPackets;
                                rawRealtime3 = wifiRxPackets;
                            } else {
                                rawRealtime3 = wifiRxPackets;
                                if (rawRealtime3 <= 0) {
                                    rawRealtimeMs = wifiTxPackets;
                                    if (rawRealtimeMs <= 0) {
                                        printWriter4 = pw;
                                        rawRealtime12 = rawRealtime10;
                                        btTxTotalBytes2 = wifiRxBytes;
                                        uidMobileActiveTime2 = wifiTxBytes2;
                                        wifiTxBytes = uidMobileActiveTime2;
                                        uidMobileActiveTime2 = fullWifiLockOnTime2;
                                        long wifiRxPackets3;
                                        long wifiTxPackets2;
                                        long wifiRxBytes2;
                                        if (uidMobileActiveTime2 == 0) {
                                            wifiRxPackets3 = rawRealtime3;
                                            rawRealtime3 = wifiScanTime;
                                            if (rawRealtime3 == 0) {
                                                uidMobileActiveCount2 = wifiScanCount;
                                                if (uidMobileActiveCount2 == 0) {
                                                    i11 = wifiScanCountBg;
                                                    if (i11 == 0) {
                                                        wifiTxPackets2 = rawRealtimeMs;
                                                        rawRealtimeMs = wifiScanActualTime2;
                                                        if (rawRealtimeMs == 0) {
                                                            wifiRxBytes2 = btTxTotalBytes2;
                                                            btTxTotalBytes2 = wifiScanActualTimeBg2;
                                                            if (btTxTotalBytes2 == 0) {
                                                                wifiWakeup2 = totalRealtime;
                                                                totalRealtime = uidWifiRunningTime2;
                                                                if (totalRealtime == 0) {
                                                                    wifiScanActualTime = rawRealtimeMs;
                                                                    u2 = u;
                                                                    uidWifiRunningTime = totalRealtime;
                                                                    whichBatteryRealtime7 = whichBatteryRealtime2;
                                                                    sb5 = sb4;
                                                                    totalRealtime = rawRealtime12;
                                                                    rawRealtimeMs = printWriter4;
                                                                    fullWifiLockOnTime = uidMobileActiveTime2;
                                                                    uidMobileActiveTime2 = wifiWakeup2;
                                                                    if (uidMobileActiveTime2 > 0) {
                                                                        sb5.setLength(0);
                                                                        sb5.append(str4);
                                                                        sb5.append("    WiFi AP wakeups: ");
                                                                        sb5.append(uidMobileActiveTime2);
                                                                        rawRealtimeMs.println(sb5.toString());
                                                                    }
                                                                    stringBuilder3 = new StringBuilder();
                                                                    stringBuilder3.append(str4);
                                                                    stringBuilder3.append("  ");
                                                                    u8 = u2;
                                                                    mobileActiveTime = btTxTotalBytes2;
                                                                    mobileTxTotalPackets2 = whichBatteryRealtime7;
                                                                    wifiWakeup3 = uidMobileActiveTime2;
                                                                    u3 = u8;
                                                                    printWriter5 = rawRealtimeMs;
                                                                    mobileTxTotalBytes = wifiScanActualTime;
                                                                    batteryStats3.printControllerActivityIfInteresting(rawRealtimeMs, sb5, stringBuilder3.toString(), WIFI_CONTROLLER_NAME, u8.getWifiControllerActivity(), which);
                                                                    if (btRxBytes > 0) {
                                                                        rawRealtime3 = btTxBytes;
                                                                        if (rawRealtime3 <= 0) {
                                                                            long btRxBytes2;
                                                                            long btTxBytes2;
                                                                            long rawRealtime17;
                                                                            long rawRealtimeMs6;
                                                                            Timer bleTimer2;
                                                                            Timer unoptimizedScanTimer;
                                                                            long totalPartialWakelock2;
                                                                            long totalDrawWakelock;
                                                                            Timer pTimer;
                                                                            long rawRealtime18;
                                                                            long multicastWakeLockTimeMicros;
                                                                            long totalTime;
                                                                            boolean uidActivity4;
                                                                            boolean uidActivity5;
                                                                            gpsBatteryDrainMaMs = btRxBytes;
                                                                            bleTimer = u3.getBluetoothScanTimer();
                                                                            int wifiScanCount2;
                                                                            int wifiScanCountBg2;
                                                                            if (bleTimer != null) {
                                                                                i2 = which;
                                                                                btRxBytes2 = gpsBatteryDrainMaMs;
                                                                                gpsBatteryDrainMaMs = (bleTimer.getTotalTimeLocked(totalRealtime, i2) + 500) / 1000;
                                                                                if (gpsBatteryDrainMaMs != 0) {
                                                                                    long j5;
                                                                                    long j6;
                                                                                    i8 = bleTimer.getCountLocked(i2);
                                                                                    btTxBytes2 = rawRealtime3;
                                                                                    rawRealtime3 = u3.getBluetoothScanBackgroundTimer();
                                                                                    estimatedBatteryCapacity = rawRealtime3 != null ? rawRealtime3.getCountLocked(i2) : 0;
                                                                                    rawRealtime17 = totalRealtime;
                                                                                    wifiScanCount2 = uidMobileActiveCount2;
                                                                                    rawRealtime8 = rawRealtimeMs3;
                                                                                    rawRealtime10 = bleTimer.getTotalDurationMsLocked(rawRealtime8);
                                                                                    long actualTimeMsBg = rawRealtime3 != null ? rawRealtime3.getTotalDurationMsLocked(rawRealtime8) : 0;
                                                                                    NU6 = u3.getBluetoothScanResultCounter() != null ? u3.getBluetoothScanResultCounter().getCountLocked(i2) : 0;
                                                                                    if (u3.getBluetoothScanResultBgCounter() != null) {
                                                                                        wifiScanCountBg2 = i11;
                                                                                        i11 = u3.getBluetoothScanResultBgCounter().getCountLocked(i2);
                                                                                    } else {
                                                                                        wifiScanCountBg2 = i11;
                                                                                        i11 = 0;
                                                                                    }
                                                                                    Timer unoptimizedScanTimer2 = u3.getBluetoothUnoptimizedScanTimer();
                                                                                    long unoptimizedScanTotalTime = unoptimizedScanTimer2 != null ? unoptimizedScanTimer2.getTotalDurationMsLocked(rawRealtime8) : 0;
                                                                                    long unoptimizedScanMaxTime = unoptimizedScanTimer2 != null ? unoptimizedScanTimer2.getMaxDurationMsLocked(rawRealtime8) : 0;
                                                                                    Timer unoptimizedScanTimer3 = unoptimizedScanTimer2;
                                                                                    unoptimizedScanTimer2 = u3.getBluetoothUnoptimizedScanBackgroundTimer();
                                                                                    long unoptimizedScanTotalTimeBg = unoptimizedScanTimer2 != null ? unoptimizedScanTimer2.getTotalDurationMsLocked(rawRealtime8) : 0;
                                                                                    long unoptimizedScanMaxTimeBg = unoptimizedScanTimer2 != null ? unoptimizedScanTimer2.getMaxDurationMsLocked(rawRealtime8) : 0;
                                                                                    rawRealtimeMs6 = rawRealtime8;
                                                                                    sb5.setLength(0);
                                                                                    if (rawRealtime10 != gpsBatteryDrainMaMs) {
                                                                                        sb5.append(str4);
                                                                                        sb5.append("    Bluetooth Scan (total blamed realtime): ");
                                                                                        formatTimeMs(sb5, gpsBatteryDrainMaMs);
                                                                                        sb5.append(" (");
                                                                                        sb5.append(i8);
                                                                                        sb5.append(" times)");
                                                                                        if (bleTimer.isRunningLocked()) {
                                                                                            sb5.append(" (currently running)");
                                                                                        }
                                                                                        sb5.append("\n");
                                                                                    }
                                                                                    sb5.append(str4);
                                                                                    sb5.append("    Bluetooth Scan (total actual realtime): ");
                                                                                    formatTimeMs(sb5, rawRealtime10);
                                                                                    sb5.append(" (");
                                                                                    sb5.append(i8);
                                                                                    sb5.append(" times)");
                                                                                    if (bleTimer.isRunningLocked()) {
                                                                                        sb5.append(" (currently running)");
                                                                                    }
                                                                                    sb5.append("\n");
                                                                                    rawRealtime8 = actualTimeMsBg;
                                                                                    if (rawRealtime8 > 0 || estimatedBatteryCapacity > 0) {
                                                                                        sb5.append(str4);
                                                                                        sb5.append("    Bluetooth Scan (background realtime): ");
                                                                                        formatTimeMs(sb5, rawRealtime8);
                                                                                        sb5.append(" (");
                                                                                        sb5.append(estimatedBatteryCapacity);
                                                                                        sb5.append(" times)");
                                                                                        if (rawRealtime3 != null && rawRealtime3.isRunningLocked()) {
                                                                                            sb5.append(" (currently running in background)");
                                                                                        }
                                                                                        sb5.append("\n");
                                                                                    } else {
                                                                                        int i12 = i8;
                                                                                    }
                                                                                    sb5.append(str4);
                                                                                    sb5.append("    Bluetooth Scan Results: ");
                                                                                    sb5.append(NU6);
                                                                                    sb5.append(" (");
                                                                                    sb5.append(i11);
                                                                                    sb5.append(" in background)");
                                                                                    long totalTimeMs = gpsBatteryDrainMaMs;
                                                                                    gpsBatteryDrainMaMs = unoptimizedScanTotalTime;
                                                                                    if (gpsBatteryDrainMaMs <= 0) {
                                                                                        bleTimer2 = bleTimer;
                                                                                        Timer bleTimerBg = rawRealtime3;
                                                                                        bleTimer = unoptimizedScanTotalTimeBg;
                                                                                        if (bleTimer <= null) {
                                                                                            j5 = gpsBatteryDrainMaMs;
                                                                                            j6 = unoptimizedScanMaxTime;
                                                                                            Timer timer5 = unoptimizedScanTimer3;
                                                                                            gpsBatteryDrainMaMs = unoptimizedScanMaxTimeBg;
                                                                                            printWriter5 = pw;
                                                                                            printWriter5.println(sb5.toString());
                                                                                            uidActivity3 = true;
                                                                                        }
                                                                                    } else {
                                                                                        bleTimer2 = bleTimer;
                                                                                        Object obj = rawRealtime3;
                                                                                        bleTimer = unoptimizedScanTotalTimeBg;
                                                                                    }
                                                                                    sb5.append("\n");
                                                                                    sb5.append(str4);
                                                                                    sb5.append("    Unoptimized Bluetooth Scan (realtime): ");
                                                                                    formatTimeMs(sb5, gpsBatteryDrainMaMs);
                                                                                    sb5.append(" (max ");
                                                                                    j5 = gpsBatteryDrainMaMs;
                                                                                    gpsBatteryDrainMaMs = unoptimizedScanMaxTime;
                                                                                    formatTimeMs(sb5, gpsBatteryDrainMaMs);
                                                                                    sb5.append(")");
                                                                                    unoptimizedScanTimer = unoptimizedScanTimer3;
                                                                                    if (unoptimizedScanTimer == null || !unoptimizedScanTimer.isRunningLocked()) {
                                                                                    } else {
                                                                                        sb5.append(" (currently running unoptimized)");
                                                                                    }
                                                                                    if (unoptimizedScanTimer2 == null || bleTimer <= null) {
                                                                                        j6 = gpsBatteryDrainMaMs;
                                                                                        gpsBatteryDrainMaMs = unoptimizedScanMaxTimeBg;
                                                                                        printWriter5 = pw;
                                                                                        printWriter5.println(sb5.toString());
                                                                                        uidActivity3 = true;
                                                                                    } else {
                                                                                        sb5.append("\n");
                                                                                        sb5.append(str4);
                                                                                        sb5.append("    Unoptimized Bluetooth Scan (background realtime): ");
                                                                                        formatTimeMs(sb5, bleTimer);
                                                                                        sb5.append(" (max ");
                                                                                        j6 = gpsBatteryDrainMaMs;
                                                                                        formatTimeMs(sb5, unoptimizedScanMaxTimeBg);
                                                                                        sb5.append(")");
                                                                                        if (unoptimizedScanTimer2.isRunningLocked()) {
                                                                                            sb5.append(" (currently running unoptimized in background)");
                                                                                        }
                                                                                        printWriter5 = pw;
                                                                                        printWriter5.println(sb5.toString());
                                                                                        uidActivity3 = true;
                                                                                    }
                                                                                } else {
                                                                                    bleTimer2 = bleTimer;
                                                                                    btTxBytes2 = rawRealtime3;
                                                                                    wifiScanCountBg2 = i11;
                                                                                    rawRealtime17 = totalRealtime;
                                                                                    wifiScanCount2 = uidMobileActiveCount2;
                                                                                    rawRealtimeMs6 = rawRealtimeMs3;
                                                                                }
                                                                            } else {
                                                                                btRxBytes2 = gpsBatteryDrainMaMs;
                                                                                bleTimer2 = bleTimer;
                                                                                btTxBytes2 = rawRealtime3;
                                                                                wifiScanCountBg2 = i11;
                                                                                rawRealtime17 = totalRealtime;
                                                                                wifiScanCount2 = uidMobileActiveCount2;
                                                                                rawRealtimeMs6 = rawRealtimeMs3;
                                                                            }
                                                                            if (u3.hasUserActivity()) {
                                                                                boolean hasData = false;
                                                                                for (i8 = 0; i8 < 4; i8++) {
                                                                                    rawRealtimeMs = u3.getUserActivityCount(i8, which);
                                                                                    if (rawRealtimeMs != null) {
                                                                                        if (hasData) {
                                                                                            sb5.append(", ");
                                                                                        } else {
                                                                                            sb5.setLength(0);
                                                                                            sb5.append("    User activity: ");
                                                                                            hasData = true;
                                                                                        }
                                                                                        sb5.append(rawRealtimeMs);
                                                                                        sb5.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                                                                                        sb5.append(Uid.USER_ACTIVITY_TYPES[i8]);
                                                                                    }
                                                                                }
                                                                                i4 = which;
                                                                                if (hasData) {
                                                                                    printWriter5.println(sb5.toString());
                                                                                }
                                                                            } else {
                                                                                i4 = which;
                                                                            }
                                                                            wakelocks2 = u3.getWakelockStats();
                                                                            i11 = 0;
                                                                            Timer bleTimer3;
                                                                            ArrayMap<String, ? extends Wakelock> wakelocks3;
                                                                            if (wakelocks2 != null) {
                                                                                iw2 = wakelocks2.size() - 1;
                                                                                totalPartialWakelock2 = 0;
                                                                                mobileRxTotalBytes = 0;
                                                                                totalDrawWakelock = 0;
                                                                                totalRealtime = 0;
                                                                                while (true) {
                                                                                    estimatedBatteryCapacity = iw2;
                                                                                    if (estimatedBatteryCapacity >= 0) {
                                                                                        Wakelock wl2 = (Wakelock) wakelocks2.valueAt(estimatedBatteryCapacity);
                                                                                        sb5.setLength(0);
                                                                                        sb5.append(str4);
                                                                                        sb5.append("    Wake lock ");
                                                                                        sb5.append((String) wakelocks2.keyAt(estimatedBatteryCapacity));
                                                                                        btRxTotalBytes2 = btRxBytes2;
                                                                                        gpsBatteryDrainMaMs = rawRealtime17;
                                                                                        bleTimer3 = bleTimer2;
                                                                                        Wakelock wl3 = wl2;
                                                                                        btTxTotalBytes3 = btTxBytes2;
                                                                                        NU3 = which;
                                                                                        int iw3 = estimatedBatteryCapacity;
                                                                                        String linePrefix2 = printWakeLock(sb5, wl2.getWakeTime(1), gpsBatteryDrainMaMs, "full", NU3, ": ");
                                                                                        pTimer = wl3.getWakeTime(0);
                                                                                        wakelocks3 = wakelocks2;
                                                                                        Timer pTimer2 = pTimer;
                                                                                        linePrefix2 = printWakeLock(sb5, pTimer, gpsBatteryDrainMaMs, "partial", NU3, linePrefix2);
                                                                                        unoptimizedScanTimer = pTimer2 != null ? pTimer2.getSubTimer() : null;
                                                                                        gpsBatteryDrainMaMs = rawRealtime17;
                                                                                        NU3 = which;
                                                                                        StringBuilder linePrefix3 = sb5;
                                                                                        String linePrefix4 = printWakeLock(linePrefix3, wl3.getWakeTime(18), gpsBatteryDrainMaMs, "draw", NU3, printWakeLock(linePrefix3, wl3.getWakeTime(2), gpsBatteryDrainMaMs, AppAssociate.ASSOC_WINDOW, NU3, printWakeLock(sb5, unoptimizedScanTimer, gpsBatteryDrainMaMs, "background partial", NU3, linePrefix2)));
                                                                                        sb5.append(" realtime");
                                                                                        printWriter5.println(sb5.toString());
                                                                                        uidActivity3 = true;
                                                                                        i11++;
                                                                                        i2 = which;
                                                                                        rawRealtimeMs = rawRealtime17;
                                                                                        totalRealtime += computeWakeLock(wl3.getWakeTime(1), rawRealtimeMs, i2);
                                                                                        totalPartialWakelock2 += computeWakeLock(wl3.getWakeTime(0), rawRealtimeMs, i2);
                                                                                        mobileRxTotalBytes += computeWakeLock(wl3.getWakeTime(2), rawRealtimeMs, i2);
                                                                                        totalDrawWakelock += computeWakeLock(wl3.getWakeTime(18), rawRealtimeMs, i2);
                                                                                        iw2 = iw3 - 1;
                                                                                        i4 = i2;
                                                                                        bleTimer2 = bleTimer3;
                                                                                        btRxBytes2 = btRxTotalBytes2;
                                                                                        btTxBytes2 = btTxTotalBytes3;
                                                                                        wakelocks2 = wakelocks3;
                                                                                    } else {
                                                                                        i2 = i4;
                                                                                        wakelocks3 = wakelocks2;
                                                                                        btRxTotalBytes2 = btRxBytes2;
                                                                                        btTxTotalBytes3 = btTxBytes2;
                                                                                        bleTimer3 = bleTimer2;
                                                                                        i8 = i11;
                                                                                        whichBatteryRealtime5 = totalRealtime;
                                                                                        totalRealtime = totalPartialWakelock2;
                                                                                        batteryUptime = mobileRxTotalBytes;
                                                                                        rawRealtime10 = totalDrawWakelock;
                                                                                        rawRealtime18 = rawRealtime17;
                                                                                    }
                                                                                }
                                                                            } else {
                                                                                wakelocks3 = wakelocks2;
                                                                                btRxTotalBytes2 = btRxBytes2;
                                                                                btTxTotalBytes3 = btTxBytes2;
                                                                                bleTimer3 = bleTimer2;
                                                                                wakelocks2 = i4;
                                                                                rawRealtime10 = 0;
                                                                                rawRealtime18 = rawRealtime17;
                                                                                totalRealtime = 0;
                                                                                i8 = 0;
                                                                                batteryUptime = 0;
                                                                                whichBatteryRealtime5 = 0;
                                                                            }
                                                                            long j7;
                                                                            if (i8 > 1) {
                                                                                long totalDrawWakelock2;
                                                                                if (u3.getAggregatedPartialWakelockTimer() != null) {
                                                                                    rawRealtimeMs = u3.getAggregatedPartialWakelockTimer();
                                                                                    u4 = u3;
                                                                                    totalDrawWakelock2 = rawRealtime10;
                                                                                    u3 = rawRealtimeMs6;
                                                                                    totalPartialWakelock2 = rawRealtimeMs.getTotalDurationMsLocked(u3);
                                                                                    pTimer = rawRealtimeMs.getSubTimer();
                                                                                    totalDrawWakelock = pTimer != null ? pTimer.getTotalDurationMsLocked(u3) : 0;
                                                                                    rawRealtimeMs5 = u3;
                                                                                    u3 = totalPartialWakelock2;
                                                                                    mobileTxBytes3 = totalDrawWakelock;
                                                                                } else {
                                                                                    u4 = u3;
                                                                                    totalDrawWakelock2 = rawRealtime10;
                                                                                    u3 = 0;
                                                                                    mobileTxBytes3 = 0;
                                                                                    rawRealtimeMs5 = rawRealtimeMs6;
                                                                                }
                                                                                if (u3 == null && mobileTxBytes3 == 0 && whichBatteryRealtime5 == 0 && totalRealtime == 0 && batteryUptime == 0) {
                                                                                    int i13 = i8;
                                                                                    j7 = batteryUptime;
                                                                                    totalDrawWakelock = totalDrawWakelock2;
                                                                                    countWakelock = pw;
                                                                                    rawRealtimeMs = prefix;
                                                                                } else {
                                                                                    sb5.setLength(0);
                                                                                    rawRealtimeMs = prefix;
                                                                                    sb5.append(rawRealtimeMs);
                                                                                    sb5.append("    TOTAL wake: ");
                                                                                    needComma = false;
                                                                                    if (whichBatteryRealtime5 != 0) {
                                                                                        needComma = true;
                                                                                        formatTimeMs(sb5, whichBatteryRealtime5);
                                                                                        sb5.append("full");
                                                                                    }
                                                                                    if (totalRealtime != 0) {
                                                                                        if (needComma) {
                                                                                            sb5.append(", ");
                                                                                        }
                                                                                        needComma = true;
                                                                                        formatTimeMs(sb5, totalRealtime);
                                                                                        sb5.append("blamed partial");
                                                                                    }
                                                                                    if (u3 != null) {
                                                                                        if (needComma) {
                                                                                            sb5.append(", ");
                                                                                        }
                                                                                        needComma = true;
                                                                                        formatTimeMs(sb5, u3);
                                                                                        sb5.append("actual partial");
                                                                                    }
                                                                                    if (mobileTxBytes3 != 0) {
                                                                                        if (needComma) {
                                                                                            sb5.append(", ");
                                                                                        }
                                                                                        needComma = true;
                                                                                        formatTimeMs(sb5, mobileTxBytes3);
                                                                                        sb5.append("actual background partial");
                                                                                    }
                                                                                    if (batteryUptime != 0) {
                                                                                        if (needComma) {
                                                                                            sb5.append(", ");
                                                                                        }
                                                                                        needComma = true;
                                                                                        formatTimeMs(sb5, batteryUptime);
                                                                                        sb5.append(AppAssociate.ASSOC_WINDOW);
                                                                                    }
                                                                                    i8 = totalDrawWakelock2;
                                                                                    if (i8 != 0) {
                                                                                        if (needComma) {
                                                                                            sb5.append(",");
                                                                                        }
                                                                                        formatTimeMs(sb5, i8);
                                                                                        sb5.append("draw");
                                                                                    }
                                                                                    sb5.append(" realtime");
                                                                                    totalDrawWakelock = i8;
                                                                                    countWakelock = pw;
                                                                                    countWakelock.println(sb5.toString());
                                                                                }
                                                                            } else {
                                                                                j7 = batteryUptime;
                                                                                rawRealtimeMs = str4;
                                                                                u4 = u3;
                                                                                totalDrawWakelock = rawRealtime10;
                                                                                rawRealtimeMs5 = rawRealtimeMs6;
                                                                                countWakelock = pw;
                                                                            }
                                                                            u5 = u4;
                                                                            timer = u5.getMulticastWakelockStats();
                                                                            if (timer != null) {
                                                                                uidMobileActiveTime2 = rawRealtime18;
                                                                                estimatedBatteryCapacity = which;
                                                                                multicastWakeLockTimeMicros = timer.getTotalTimeLocked(uidMobileActiveTime2, estimatedBatteryCapacity);
                                                                                iw = timer.getCountLocked(estimatedBatteryCapacity);
                                                                                if (multicastWakeLockTimeMicros > 0) {
                                                                                    sb5.setLength(0);
                                                                                    sb5.append(rawRealtimeMs);
                                                                                    sb5.append("    WiFi Multicast Wakelock");
                                                                                    sb5.append(" count = ");
                                                                                    sb5.append(iw);
                                                                                    sb5.append(" time = ");
                                                                                    formatTimeMsNoSpace(sb5, (multicastWakeLockTimeMicros + 500) / 1000);
                                                                                    countWakelock.println(sb5.toString());
                                                                                }
                                                                            } else {
                                                                                uidMobileActiveTime2 = rawRealtime18;
                                                                                estimatedBatteryCapacity = which;
                                                                            }
                                                                            syncs = u5.getSyncStats();
                                                                            if (syncs != null) {
                                                                                NU3 = syncs.size() - 1;
                                                                                while (NU3 >= 0) {
                                                                                    Timer timer6 = (Timer) syncs.valueAt(NU3);
                                                                                    multicastWakeLockTimeMicros = (timer6.getTotalTimeLocked(uidMobileActiveTime2, estimatedBatteryCapacity) + 500) / 1000;
                                                                                    uidMobileActiveCount2 = timer6.getCountLocked(estimatedBatteryCapacity);
                                                                                    Timer mcTimer = timer;
                                                                                    timer = timer6.getSubTimer();
                                                                                    if (timer != null) {
                                                                                        totalPartialWakelock = totalRealtime;
                                                                                        rawRealtime8 = rawRealtimeMs5;
                                                                                        totalPartialWakelock2 = timer.getTotalDurationMsLocked(rawRealtime8);
                                                                                    } else {
                                                                                        totalPartialWakelock = totalRealtime;
                                                                                        rawRealtime8 = rawRealtimeMs5;
                                                                                        totalPartialWakelock2 = -1;
                                                                                    }
                                                                                    long bgTime = totalPartialWakelock2;
                                                                                    NU6 = timer != null ? timer.getCountLocked(estimatedBatteryCapacity) : i3;
                                                                                    sb5.setLength(null);
                                                                                    sb5.append(rawRealtimeMs);
                                                                                    sb5.append("    Sync ");
                                                                                    sb5.append((String) syncs.keyAt(NU3));
                                                                                    sb5.append(": ");
                                                                                    if (multicastWakeLockTimeMicros != 0) {
                                                                                        formatTimeMs(sb5, multicastWakeLockTimeMicros);
                                                                                        sb5.append("realtime (");
                                                                                        sb5.append(uidMobileActiveCount2);
                                                                                        sb5.append(" times)");
                                                                                        multicastWakeLockTimeMicros = bgTime;
                                                                                        if (multicastWakeLockTimeMicros > 0) {
                                                                                            sb5.append(", ");
                                                                                            formatTimeMs(sb5, multicastWakeLockTimeMicros);
                                                                                            sb5.append("background (");
                                                                                            sb5.append(NU6);
                                                                                            sb5.append(" times)");
                                                                                        }
                                                                                    } else {
                                                                                        sb5.append("(not used)");
                                                                                    }
                                                                                    countWakelock.println(sb5.toString());
                                                                                    uidActivity3 = true;
                                                                                    NU3--;
                                                                                    rawRealtimeMs5 = rawRealtime8;
                                                                                    timer = mcTimer;
                                                                                    totalRealtime = totalPartialWakelock;
                                                                                }
                                                                            }
                                                                            totalPartialWakelock = totalRealtime;
                                                                            rawRealtime8 = rawRealtimeMs5;
                                                                            jobs = u5.getJobStats();
                                                                            if (jobs != null) {
                                                                                NU3 = jobs.size() - 1;
                                                                                while (NU3 >= 0) {
                                                                                    Timer timer7 = (Timer) jobs.valueAt(NU3);
                                                                                    rawRealtime13 = uidMobileActiveTime2;
                                                                                    totalTime = (timer7.getTotalTimeLocked(uidMobileActiveTime2, estimatedBatteryCapacity) + 500) / 1000;
                                                                                    i11 = timer7.getCountLocked(estimatedBatteryCapacity);
                                                                                    Timer bgTimer = timer7.getSubTimer();
                                                                                    long bgTime2 = bgTimer != null ? bgTimer.getTotalDurationMsLocked(rawRealtime8) : -1;
                                                                                    uidMobileActiveCount2 = bgTimer != null ? bgTimer.getCountLocked(estimatedBatteryCapacity) : i3;
                                                                                    ArrayMap<String, ? extends Timer> syncs2 = syncs;
                                                                                    sb5.setLength(null);
                                                                                    sb5.append(rawRealtimeMs);
                                                                                    sb5.append("    Job ");
                                                                                    sb5.append((String) jobs.keyAt(NU3));
                                                                                    sb5.append(": ");
                                                                                    if (totalTime != 0) {
                                                                                        formatTimeMs(sb5, totalTime);
                                                                                        sb5.append("realtime (");
                                                                                        sb5.append(i11);
                                                                                        sb5.append(" times)");
                                                                                        timer7 = bgTime2;
                                                                                        if (timer7 > null) {
                                                                                            sb5.append(", ");
                                                                                            formatTimeMs(sb5, timer7);
                                                                                            sb5.append("background (");
                                                                                            sb5.append(uidMobileActiveCount2);
                                                                                            sb5.append(" times)");
                                                                                        }
                                                                                    } else {
                                                                                        int i14 = i11;
                                                                                        sb5.append("(not used)");
                                                                                    }
                                                                                    countWakelock.println(sb5.toString());
                                                                                    uidActivity3 = true;
                                                                                    NU3--;
                                                                                    uidMobileActiveTime2 = rawRealtime13;
                                                                                    syncs = syncs2;
                                                                                }
                                                                            }
                                                                            rawRealtime13 = uidMobileActiveTime2;
                                                                            completions = u5.getJobCompletionStats();
                                                                            for (NU3 = completions.size() - 1; NU3 >= 0; NU3--) {
                                                                                SparseIntArray types = (SparseIntArray) completions.valueAt(NU3);
                                                                                if (types != null) {
                                                                                    pw.print(prefix);
                                                                                    countWakelock.print("    Job Completions ");
                                                                                    countWakelock.print((String) completions.keyAt(NU3));
                                                                                    countWakelock.print(SettingsStringUtil.DELIMITER);
                                                                                    for (NU6 = 0; NU6 < types.size(); NU6++) {
                                                                                        countWakelock.print(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                                                                                        countWakelock.print(JobParameters.getReasonName(types.keyAt(NU6)));
                                                                                        countWakelock.print("(");
                                                                                        countWakelock.print(types.valueAt(NU6));
                                                                                        countWakelock.print("x)");
                                                                                    }
                                                                                    pw.println();
                                                                                }
                                                                            }
                                                                            u5.getDeferredJobsLineLocked(sb5, estimatedBatteryCapacity);
                                                                            if (sb5.length() > 0) {
                                                                                countWakelock.print("    Jobs deferred on launch ");
                                                                                countWakelock.println(sb5.toString());
                                                                            }
                                                                            iw2 = 1;
                                                                            uid = uid3;
                                                                            uidStats6 = uidStats8;
                                                                            btTxTotalBytes2 = rawRealtime8;
                                                                            iu = iu2;
                                                                            NU3 = NU8;
                                                                            wifiRxTotalBytes2 = rawRealtime13;
                                                                            totalRealtime = wifiRxTotalBytes2;
                                                                            u8 = u5;
                                                                            minLearnedBatteryCapacity = 0;
                                                                            i5 = estimatedBatteryCapacity;
                                                                            rawRealtime14 = wifiRxTotalBytes2;
                                                                            sb6 = sb5;
                                                                            str = rawRealtimeMs;
                                                                            cpuFreqs2 = cpuFreqs3;
                                                                            sb5 = sb6;
                                                                            totalRealtime = rawRealtime14;
                                                                            needComma = (((uidActivity3 | printTimer(countWakelock, sb5, u5.getFlashlightTurnedOnTimer(), totalRealtime, i5, str, "Flashlight")) | printTimer(pw, sb5, u8.getCameraTurnedOnTimer(), totalRealtime, i5, str, "Camera")) | printTimer(pw, sb5, u8.getVideoTurnedOnTimer(), totalRealtime, i5, str, "Video")) | printTimer(pw, sb5, u8.getAudioTurnedOnTimer(), totalRealtime, i5, str, "Audio");
                                                                            sensors = u8.getSensorStats();
                                                                            NSE = sensors.size();
                                                                            uidActivity3 = needComma;
                                                                            i11 = 0;
                                                                            while (i11 < NSE) {
                                                                                int NSE2;
                                                                                Sensor se = (Sensor) sensors.valueAt(i11);
                                                                                iw = sensors.keyAt(i11);
                                                                                sb7 = sb6;
                                                                                sb7.setLength(minLearnedBatteryCapacity);
                                                                                sb7.append(rawRealtimeMs);
                                                                                sb7.append("    Sensor ");
                                                                                i6 = se.getHandle();
                                                                                if (i6 == -10000) {
                                                                                    sb7.append("GPS");
                                                                                } else {
                                                                                    sb7.append(i6);
                                                                                }
                                                                                sb7.append(": ");
                                                                                Timer timer8 = se.getSensorTime();
                                                                                if (timer8 != null) {
                                                                                    NU7 = NU3;
                                                                                    whichBatteryRealtime5 = rawRealtime14;
                                                                                    iw = (timer8.getTotalTimeLocked(whichBatteryRealtime5, estimatedBatteryCapacity) + 500) / 1000;
                                                                                    uid2 = uid;
                                                                                    uid = timer8.getCountLocked(estimatedBatteryCapacity);
                                                                                    NSE2 = NSE;
                                                                                    NSE = se.getSensorBackgroundTime();
                                                                                    int bgCount = NSE != 0 ? NSE.getCountLocked(estimatedBatteryCapacity) : 0;
                                                                                    rawRealtime15 = whichBatteryRealtime5;
                                                                                    rawRealtimeMs = timer8.getTotalDurationMsLocked(btTxTotalBytes2);
                                                                                    long bgActualTime = NSE != 0 ? NSE.getTotalDurationMsLocked(btTxTotalBytes2) : 0;
                                                                                    long actualTime;
                                                                                    if (iw != 0) {
                                                                                        if (rawRealtimeMs != iw) {
                                                                                            formatTimeMs(sb7, iw);
                                                                                            sb7.append("blamed realtime, ");
                                                                                        }
                                                                                        formatTimeMs(sb7, rawRealtimeMs);
                                                                                        sb7.append("realtime (");
                                                                                        sb7.append(uid);
                                                                                        sb7.append(" times)");
                                                                                        rawRealtimeMs4 = btTxTotalBytes2;
                                                                                        btTxTotalBytes2 = bgActualTime;
                                                                                        if (btTxTotalBytes2 == 0) {
                                                                                            NU3 = bgCount;
                                                                                            if (NU3 <= 0) {
                                                                                            }
                                                                                        } else {
                                                                                            NU3 = bgCount;
                                                                                        }
                                                                                        actualTime = rawRealtimeMs;
                                                                                        sb7.append(", ");
                                                                                        formatTimeMs(sb7, btTxTotalBytes2);
                                                                                        sb7.append("background (");
                                                                                        sb7.append(NU3);
                                                                                        sb7.append(" times)");
                                                                                    } else {
                                                                                        rawRealtimeMs4 = btTxTotalBytes2;
                                                                                        actualTime = rawRealtimeMs;
                                                                                        NU3 = bgCount;
                                                                                        btTxTotalBytes2 = bgActualTime;
                                                                                        sb7.append("(not used)");
                                                                                    }
                                                                                } else {
                                                                                    rawRealtimeMs4 = btTxTotalBytes2;
                                                                                    NU7 = NU3;
                                                                                    uid2 = uid;
                                                                                    int i15 = iw;
                                                                                    int i16 = i6;
                                                                                    NSE2 = NSE;
                                                                                    rawRealtime15 = rawRealtime14;
                                                                                    sb7.append("(not used)");
                                                                                }
                                                                                pw.println(sb7.toString());
                                                                                uidActivity3 = true;
                                                                                i11++;
                                                                                sb6 = sb7;
                                                                                NU3 = NU7;
                                                                                uid = uid2;
                                                                                NSE = NSE2;
                                                                                rawRealtime14 = rawRealtime15;
                                                                                btTxTotalBytes2 = rawRealtimeMs4;
                                                                                rawRealtimeMs = prefix;
                                                                                minLearnedBatteryCapacity = 0;
                                                                            }
                                                                            rawRealtimeMs4 = btTxTotalBytes2;
                                                                            NU7 = NU3;
                                                                            uid2 = uid;
                                                                            rawRealtime15 = rawRealtime14;
                                                                            sb7 = sb6;
                                                                            printWriter3 = pw;
                                                                            printWriter6 = printWriter3;
                                                                            sb5 = sb7;
                                                                            totalRealtime = rawRealtime15;
                                                                            rawRealtimeMs = sb7;
                                                                            i5 = estimatedBatteryCapacity;
                                                                            NU3 = NSE;
                                                                            str2 = prefix;
                                                                            str = str2;
                                                                            sensors2 = sensors;
                                                                            sb5 = rawRealtimeMs;
                                                                            uidActivity = ((uidActivity3 | printTimer(printWriter6, sb5, u8.getVibratorOnTimer(), totalRealtime, i5, str, "Vibrator")) | printTimer(printWriter6, sb5, u8.getForegroundActivityTimer(), totalRealtime, i5, str, "Foreground activities")) | printTimer(printWriter3, sb5, u8.getForegroundServiceTimer(), totalRealtime, i5, str, "Foreground services");
                                                                            mobileRxPackets2 = 0;
                                                                            uidActivity2 = uidActivity;
                                                                            i8 = 0;
                                                                            while (i8 < 7) {
                                                                                SparseArray<? extends Sensor> sensors3;
                                                                                totalRealtime = rawRealtime15;
                                                                                uidMobileActiveTime2 = u8.getProcessStateTime(i8, totalRealtime, estimatedBatteryCapacity);
                                                                                if (uidMobileActiveTime2 > 0) {
                                                                                    mobileRxPackets2 += uidMobileActiveTime2;
                                                                                    rawRealtimeMs.setLength(0);
                                                                                    rawRealtimeMs.append(str2);
                                                                                    rawRealtimeMs.append("    ");
                                                                                    rawRealtimeMs.append(Uid.PROCESS_STATE_NAMES[i8]);
                                                                                    rawRealtimeMs.append(" for: ");
                                                                                    sensors3 = sensors2;
                                                                                    long totalStateTime = mobileRxPackets2;
                                                                                    formatTimeMs(rawRealtimeMs, (uidMobileActiveTime2 + 500) / 1000);
                                                                                    printWriter3.println(rawRealtimeMs.toString());
                                                                                    uidActivity2 = true;
                                                                                    mobileRxPackets2 = totalStateTime;
                                                                                } else {
                                                                                    sensors3 = sensors2;
                                                                                }
                                                                                i8++;
                                                                                rawRealtime15 = totalRealtime;
                                                                                sensors2 = sensors3;
                                                                            }
                                                                            totalRealtime = rawRealtime15;
                                                                            if (mobileRxPackets2 > 0) {
                                                                                rawRealtimeMs.setLength(0);
                                                                                rawRealtimeMs.append(str2);
                                                                                rawRealtimeMs.append("    Total running: ");
                                                                                formatTimeMs(rawRealtimeMs, (mobileRxPackets2 + 500) / 1000);
                                                                                printWriter3.println(rawRealtimeMs.toString());
                                                                            }
                                                                            uidMobileActiveTime2 = u8.getUserCpuTimeUs(estimatedBatteryCapacity);
                                                                            systemCpuTimeUs = u8.getSystemCpuTimeUs(estimatedBatteryCapacity);
                                                                            if (uidMobileActiveTime2 <= 0 || systemCpuTimeUs > 0) {
                                                                                rawRealtimeMs.setLength(0);
                                                                                rawRealtimeMs.append(str2);
                                                                                rawRealtimeMs.append("    Total cpu time: u=");
                                                                                formatTimeMs(rawRealtimeMs, uidMobileActiveTime2 / 1000);
                                                                                rawRealtimeMs.append("s=");
                                                                                formatTimeMs(rawRealtimeMs, systemCpuTimeUs / 1000);
                                                                                printWriter3.println(rawRealtimeMs.toString());
                                                                            } else {
                                                                                long j8 = mobileRxPackets2;
                                                                            }
                                                                            cpuFreqTimes = u8.getCpuFreqTimes(estimatedBatteryCapacity);
                                                                            if (cpuFreqTimes != null) {
                                                                                rawRealtimeMs.setLength(0);
                                                                                rawRealtimeMs.append("    Total cpu time per freq:");
                                                                                uid = 0;
                                                                                while (uid < cpuFreqTimes.length) {
                                                                                    sb3 = new StringBuilder();
                                                                                    sb3.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                                                                                    uidActivity4 = uidActivity2;
                                                                                    sb3.append(cpuFreqTimes[uid]);
                                                                                    rawRealtimeMs.append(sb3.toString());
                                                                                    uid++;
                                                                                    uidActivity2 = uidActivity4;
                                                                                }
                                                                                uidActivity4 = uidActivity2;
                                                                                printWriter3.println(rawRealtimeMs.toString());
                                                                            } else {
                                                                                uidActivity4 = uidActivity2;
                                                                            }
                                                                            screenOffCpuFreqTimes = u8.getScreenOffCpuFreqTimes(estimatedBatteryCapacity);
                                                                            if (screenOffCpuFreqTimes != null) {
                                                                                rawRealtimeMs.setLength(0);
                                                                                rawRealtimeMs.append("    Total screen-off cpu time per freq:");
                                                                                i11 = 0;
                                                                                while (i11 < screenOffCpuFreqTimes.length) {
                                                                                    sb5 = new StringBuilder();
                                                                                    sb5.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                                                                                    rawRealtime11 = totalRealtime;
                                                                                    sb5.append(screenOffCpuFreqTimes[i11]);
                                                                                    rawRealtimeMs.append(sb5.toString());
                                                                                    i11++;
                                                                                    totalRealtime = rawRealtime11;
                                                                                }
                                                                                rawRealtime11 = totalRealtime;
                                                                                printWriter3.println(rawRealtimeMs.toString());
                                                                            } else {
                                                                                rawRealtime11 = totalRealtime;
                                                                            }
                                                                            i11 = 0;
                                                                            while (i11 < 7) {
                                                                                StringBuilder stringBuilder4;
                                                                                long userCpuTimeUs;
                                                                                long[] cpuTimes = u8.getCpuFreqTimes(estimatedBatteryCapacity, i11);
                                                                                if (cpuTimes != null) {
                                                                                    rawRealtimeMs.setLength(0);
                                                                                    StringBuilder stringBuilder5 = new StringBuilder();
                                                                                    stringBuilder5.append("    Cpu times per freq at state ");
                                                                                    stringBuilder5.append(Uid.PROCESS_STATE_NAMES[i11]);
                                                                                    stringBuilder5.append(SettingsStringUtil.DELIMITER);
                                                                                    rawRealtimeMs.append(stringBuilder5.toString());
                                                                                    iw = 0;
                                                                                    while (iw < cpuTimes.length) {
                                                                                        stringBuilder4 = new StringBuilder();
                                                                                        stringBuilder4.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                                                                                        userCpuTimeUs = uidMobileActiveTime2;
                                                                                        stringBuilder4.append(cpuTimes[iw]);
                                                                                        rawRealtimeMs.append(stringBuilder4.toString());
                                                                                        iw++;
                                                                                        uidMobileActiveTime2 = userCpuTimeUs;
                                                                                    }
                                                                                    userCpuTimeUs = uidMobileActiveTime2;
                                                                                    printWriter3.println(rawRealtimeMs.toString());
                                                                                } else {
                                                                                    userCpuTimeUs = uidMobileActiveTime2;
                                                                                }
                                                                                long[] screenOffCpuTimes = u8.getScreenOffCpuFreqTimes(estimatedBatteryCapacity, i11);
                                                                                if (screenOffCpuTimes != null) {
                                                                                    rawRealtimeMs.setLength(0);
                                                                                    stringBuilder4 = new StringBuilder();
                                                                                    stringBuilder4.append("   Screen-off cpu times per freq at state ");
                                                                                    stringBuilder4.append(Uid.PROCESS_STATE_NAMES[i11]);
                                                                                    stringBuilder4.append(SettingsStringUtil.DELIMITER);
                                                                                    rawRealtimeMs.append(stringBuilder4.toString());
                                                                                    for (long uidMobileActiveTime22 : screenOffCpuTimes) {
                                                                                        StringBuilder stringBuilder6 = new StringBuilder();
                                                                                        stringBuilder6.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                                                                                        stringBuilder6.append(uidMobileActiveTime22);
                                                                                        rawRealtimeMs.append(stringBuilder6.toString());
                                                                                    }
                                                                                    printWriter3.println(rawRealtimeMs.toString());
                                                                                }
                                                                                i11++;
                                                                                uidMobileActiveTime22 = userCpuTimeUs;
                                                                            }
                                                                            processStats = u8.getProcessStats();
                                                                            int NSE3;
                                                                            long[] screenOffCpuFreqTimes2;
                                                                            ArrayMap<String, ? extends Proc> processStats2;
                                                                            if (processStats != null) {
                                                                                numCellularRxBins = processStats.size() - 1;
                                                                                uidActivity2 = uidActivity4;
                                                                                while (numCellularRxBins >= 0) {
                                                                                    int numExcessive;
                                                                                    int numCrashes;
                                                                                    Proc ps = (Proc) processStats.valueAt(numCellularRxBins);
                                                                                    totalTime = ps.getUserTime(estimatedBatteryCapacity);
                                                                                    rawRealtime10 = ps.getSystemTime(estimatedBatteryCapacity);
                                                                                    long[] cpuFreqTimes2 = cpuFreqTimes;
                                                                                    cpuFreqTimes = ps.getForegroundTime(estimatedBatteryCapacity);
                                                                                    NSE3 = NU3;
                                                                                    NU3 = ps.getStarts(estimatedBatteryCapacity);
                                                                                    screenOffCpuFreqTimes2 = screenOffCpuFreqTimes;
                                                                                    uid = ps.getNumCrashes(estimatedBatteryCapacity);
                                                                                    uidActivity5 = uidActivity2;
                                                                                    iw = ps.getNumAnrs(estimatedBatteryCapacity);
                                                                                    int numExcessive2 = estimatedBatteryCapacity == 0 ? ps.countExcessivePowers() : 0;
                                                                                    if (totalTime == 0 && rawRealtime10 == 0 && cpuFreqTimes == null && NU3 == 0) {
                                                                                        estimatedBatteryCapacity = numExcessive2;
                                                                                        if (estimatedBatteryCapacity == 0 && uid == 0 && iw == 0) {
                                                                                            u6 = u8;
                                                                                            processStats2 = processStats;
                                                                                            uidActivity2 = uidActivity5;
                                                                                            printWriter3 = pw;
                                                                                            numCellularRxBins--;
                                                                                            cpuFreqTimes = cpuFreqTimes2;
                                                                                            NU3 = NSE3;
                                                                                            screenOffCpuFreqTimes = screenOffCpuFreqTimes2;
                                                                                            u8 = u6;
                                                                                            processStats = processStats2;
                                                                                            estimatedBatteryCapacity = which;
                                                                                        }
                                                                                    } else {
                                                                                        estimatedBatteryCapacity = numExcessive2;
                                                                                    }
                                                                                    u6 = u8;
                                                                                    rawRealtimeMs.setLength(null);
                                                                                    rawRealtimeMs.append(str2);
                                                                                    rawRealtimeMs.append("    Proc ");
                                                                                    rawRealtimeMs.append((String) processStats.keyAt(numCellularRxBins));
                                                                                    rawRealtimeMs.append(":\n");
                                                                                    rawRealtimeMs.append(str2);
                                                                                    rawRealtimeMs.append("      CPU: ");
                                                                                    formatTimeMs(rawRealtimeMs, totalTime);
                                                                                    rawRealtimeMs.append("usr + ");
                                                                                    formatTimeMs(rawRealtimeMs, rawRealtime10);
                                                                                    rawRealtimeMs.append("krn ; ");
                                                                                    formatTimeMs(rawRealtimeMs, cpuFreqTimes);
                                                                                    rawRealtimeMs.append(FOREGROUND_ACTIVITY_DATA);
                                                                                    long foregroundTime;
                                                                                    if (NU3 == 0 && uid == 0 && iw == 0) {
                                                                                        foregroundTime = cpuFreqTimes;
                                                                                    } else {
                                                                                        rawRealtimeMs.append("\n");
                                                                                        rawRealtimeMs.append(str2);
                                                                                        rawRealtimeMs.append("      ");
                                                                                        boolean hasOne = false;
                                                                                        if (NU3 != 0) {
                                                                                            hasOne = true;
                                                                                            rawRealtimeMs.append(NU3);
                                                                                            foregroundTime = cpuFreqTimes;
                                                                                            rawRealtimeMs.append(" starts");
                                                                                        } else {
                                                                                            foregroundTime = cpuFreqTimes;
                                                                                        }
                                                                                        if (uid != 0) {
                                                                                            if (hasOne) {
                                                                                                rawRealtimeMs.append(", ");
                                                                                            }
                                                                                            hasOne = true;
                                                                                            rawRealtimeMs.append(uid);
                                                                                            rawRealtimeMs.append(" crashes");
                                                                                        }
                                                                                        if (iw != 0) {
                                                                                            if (hasOne) {
                                                                                                rawRealtimeMs.append(", ");
                                                                                            }
                                                                                            rawRealtimeMs.append(iw);
                                                                                            rawRealtimeMs.append(" anrs");
                                                                                        }
                                                                                    }
                                                                                    printWriter3 = pw;
                                                                                    printWriter3.println(rawRealtimeMs.toString());
                                                                                    cpuFreqTimes = null;
                                                                                    while (cpuFreqTimes < estimatedBatteryCapacity) {
                                                                                        int starts;
                                                                                        ExcessivePower ew = ps.getExcessivePower(cpuFreqTimes);
                                                                                        if (ew != null) {
                                                                                            pw.print(prefix);
                                                                                            starts = NU3;
                                                                                            printWriter3.print("      * Killed for ");
                                                                                            numExcessive = estimatedBatteryCapacity;
                                                                                            if (ew.type == 2) {
                                                                                                printWriter3.print(CPU_DATA);
                                                                                            } else {
                                                                                                printWriter3.print("unknown");
                                                                                            }
                                                                                            printWriter3.print(" use: ");
                                                                                            numCrashes = uid;
                                                                                            TimeUtils.formatDuration(ew.usedTime, printWriter3);
                                                                                            printWriter3.print(" over ");
                                                                                            TimeUtils.formatDuration(ew.overTime, printWriter3);
                                                                                            if (ew.overTime != 0) {
                                                                                                printWriter3.print(" (");
                                                                                                processStats2 = processStats;
                                                                                                printWriter3.print((ew.usedTime * 100) / ew.overTime);
                                                                                                printWriter3.println("%)");
                                                                                            } else {
                                                                                                processStats2 = processStats;
                                                                                            }
                                                                                        } else {
                                                                                            starts = NU3;
                                                                                            numExcessive = estimatedBatteryCapacity;
                                                                                            numCrashes = uid;
                                                                                            processStats2 = processStats;
                                                                                        }
                                                                                        cpuFreqTimes++;
                                                                                        NU3 = starts;
                                                                                        estimatedBatteryCapacity = numExcessive;
                                                                                        uid = numCrashes;
                                                                                        processStats = processStats2;
                                                                                    }
                                                                                    numExcessive = estimatedBatteryCapacity;
                                                                                    numCrashes = uid;
                                                                                    processStats2 = processStats;
                                                                                    uidActivity2 = true;
                                                                                    numCellularRxBins--;
                                                                                    cpuFreqTimes = cpuFreqTimes2;
                                                                                    NU3 = NSE3;
                                                                                    screenOffCpuFreqTimes = screenOffCpuFreqTimes2;
                                                                                    u8 = u6;
                                                                                    processStats = processStats2;
                                                                                    estimatedBatteryCapacity = which;
                                                                                }
                                                                                u6 = u8;
                                                                                NSE3 = NU3;
                                                                                screenOffCpuFreqTimes2 = screenOffCpuFreqTimes;
                                                                                processStats2 = processStats;
                                                                                uidActivity5 = uidActivity2;
                                                                            } else {
                                                                                u6 = u8;
                                                                                NSE3 = NU3;
                                                                                screenOffCpuFreqTimes2 = screenOffCpuFreqTimes;
                                                                                processStats2 = processStats;
                                                                                uidActivity5 = uidActivity4;
                                                                            }
                                                                            u7 = u6;
                                                                            packageStats = u7.getPackageStats();
                                                                            if (packageStats != null) {
                                                                                ArrayMap<String, ? extends Pkg> packageStats2;
                                                                                NU3 = packageStats.size() - 1;
                                                                                boolean uidActivity6 = uidActivity5;
                                                                                while (NU3 >= 0) {
                                                                                    Uid u9;
                                                                                    Pkg ps2;
                                                                                    ArrayMap<String, ? extends Counter> alarms;
                                                                                    pw.print(prefix);
                                                                                    printWriter3.print("    Apk ");
                                                                                    printWriter3.print((String) packageStats.keyAt(NU3));
                                                                                    printWriter3.println(SettingsStringUtil.DELIMITER);
                                                                                    boolean apkActivity = false;
                                                                                    Pkg ps3 = (Pkg) packageStats.valueAt(NU3);
                                                                                    ArrayMap<String, ? extends Counter> alarms2 = ps3.getWakeupAlarmStats();
                                                                                    for (iw = alarms2.size() - 1; iw >= 0; iw--) {
                                                                                        pw.print(prefix);
                                                                                        printWriter3.print("      Wakeup alarm ");
                                                                                        printWriter3.print((String) alarms2.keyAt(iw));
                                                                                        printWriter3.print(": ");
                                                                                        printWriter3.print(((Counter) alarms2.valueAt(iw)).getCountLocked(which));
                                                                                        printWriter3.println(" times");
                                                                                        apkActivity = true;
                                                                                    }
                                                                                    NU6 = which;
                                                                                    ArrayMap<String, ? extends Serv> serviceStats = ps3.getServiceStats();
                                                                                    i6 = serviceStats.size() - 1;
                                                                                    while (i6 >= 0) {
                                                                                        Serv ss = (Serv) serviceStats.valueAt(i6);
                                                                                        rawRealtime10 = batteryUptime3;
                                                                                        batteryUptime3 = ss.getStartTime(rawRealtime10, NU6);
                                                                                        u9 = u7;
                                                                                        u7 = ss.getStarts(NU6);
                                                                                        packageStats2 = packageStats;
                                                                                        i4 = ss.getLaunches(NU6);
                                                                                        if (batteryUptime3 == 0 && u7 == null && i4 == 0) {
                                                                                            ps2 = ps3;
                                                                                            alarms = alarms2;
                                                                                        } else {
                                                                                            ps2 = ps3;
                                                                                            rawRealtimeMs.setLength(null);
                                                                                            rawRealtimeMs.append(str2);
                                                                                            rawRealtimeMs.append("      Service ");
                                                                                            rawRealtimeMs.append((String) serviceStats.keyAt(i6));
                                                                                            rawRealtimeMs.append(":\n");
                                                                                            rawRealtimeMs.append(str2);
                                                                                            rawRealtimeMs.append("        Created for: ");
                                                                                            alarms = alarms2;
                                                                                            formatTimeMs(rawRealtimeMs, batteryUptime3 / 1000);
                                                                                            rawRealtimeMs.append("uptime\n");
                                                                                            rawRealtimeMs.append(str2);
                                                                                            rawRealtimeMs.append("        Starts: ");
                                                                                            rawRealtimeMs.append(u7);
                                                                                            rawRealtimeMs.append(", launches: ");
                                                                                            rawRealtimeMs.append(i4);
                                                                                            printWriter3.println(rawRealtimeMs.toString());
                                                                                            apkActivity = true;
                                                                                        }
                                                                                        i6--;
                                                                                        batteryUptime3 = rawRealtime10;
                                                                                        u7 = u9;
                                                                                        packageStats = packageStats2;
                                                                                        ps3 = ps2;
                                                                                        alarms2 = alarms;
                                                                                    }
                                                                                    u9 = u7;
                                                                                    packageStats2 = packageStats;
                                                                                    ps2 = ps3;
                                                                                    alarms = alarms2;
                                                                                    rawRealtime10 = batteryUptime3;
                                                                                    if (!apkActivity) {
                                                                                        pw.print(prefix);
                                                                                        printWriter3.println("      (nothing executed)");
                                                                                    }
                                                                                    uidActivity6 = true;
                                                                                    NU3--;
                                                                                    batteryUptime3 = rawRealtime10;
                                                                                    u7 = u9;
                                                                                    packageStats = packageStats2;
                                                                                }
                                                                                packageStats2 = packageStats;
                                                                                rawRealtime10 = batteryUptime3;
                                                                                NU6 = which;
                                                                                if (!uidActivity6) {
                                                                                    pw.print(prefix);
                                                                                    printWriter3.println("    (nothing executed)");
                                                                                }
                                                                            } else {
                                                                                rawRealtime10 = batteryUptime3;
                                                                                NU6 = which;
                                                                            }
                                                                        }
                                                                    } else {
                                                                        rawRealtime3 = btTxBytes;
                                                                    }
                                                                    pw.print(prefix);
                                                                    printWriter5.print("    Bluetooth network: ");
                                                                    gpsBatteryDrainMaMs = btRxBytes;
                                                                    printWriter5.print(batteryStats3.formatBytesLocked(gpsBatteryDrainMaMs));
                                                                    printWriter5.print(" received, ");
                                                                    printWriter5.print(batteryStats3.formatBytesLocked(rawRealtime3));
                                                                    printWriter5.println(" sent");
                                                                    bleTimer = u3.getBluetoothScanTimer();
                                                                    if (bleTimer != null) {
                                                                    }
                                                                    if (u3.hasUserActivity()) {
                                                                    }
                                                                    wakelocks2 = u3.getWakelockStats();
                                                                    i11 = 0;
                                                                    if (wakelocks2 != null) {
                                                                    }
                                                                    if (i8 > 1) {
                                                                    }
                                                                    u5 = u4;
                                                                    timer = u5.getMulticastWakelockStats();
                                                                    if (timer != null) {
                                                                    }
                                                                    syncs = u5.getSyncStats();
                                                                    if (syncs != null) {
                                                                    }
                                                                    totalPartialWakelock = totalRealtime;
                                                                    rawRealtime8 = rawRealtimeMs5;
                                                                    jobs = u5.getJobStats();
                                                                    if (jobs != null) {
                                                                    }
                                                                    rawRealtime13 = uidMobileActiveTime22;
                                                                    completions = u5.getJobCompletionStats();
                                                                    while (NU3 >= 0) {
                                                                    }
                                                                    u5.getDeferredJobsLineLocked(sb5, estimatedBatteryCapacity);
                                                                    if (sb5.length() > 0) {
                                                                    }
                                                                    iw2 = 1;
                                                                    uid = uid3;
                                                                    uidStats6 = uidStats8;
                                                                    btTxTotalBytes2 = rawRealtime8;
                                                                    iu = iu2;
                                                                    NU3 = NU8;
                                                                    wifiRxTotalBytes2 = rawRealtime13;
                                                                    totalRealtime = wifiRxTotalBytes2;
                                                                    u8 = u5;
                                                                    minLearnedBatteryCapacity = 0;
                                                                    i5 = estimatedBatteryCapacity;
                                                                    rawRealtime14 = wifiRxTotalBytes2;
                                                                    sb6 = sb5;
                                                                    str = rawRealtimeMs;
                                                                    cpuFreqs2 = cpuFreqs3;
                                                                    sb5 = sb6;
                                                                    totalRealtime = rawRealtime14;
                                                                    needComma = (((uidActivity3 | printTimer(countWakelock, sb5, u5.getFlashlightTurnedOnTimer(), totalRealtime, i5, str, "Flashlight")) | printTimer(pw, sb5, u8.getCameraTurnedOnTimer(), totalRealtime, i5, str, "Camera")) | printTimer(pw, sb5, u8.getVideoTurnedOnTimer(), totalRealtime, i5, str, "Video")) | printTimer(pw, sb5, u8.getAudioTurnedOnTimer(), totalRealtime, i5, str, "Audio");
                                                                    sensors = u8.getSensorStats();
                                                                    NSE = sensors.size();
                                                                    uidActivity3 = needComma;
                                                                    i11 = 0;
                                                                    while (i11 < NSE) {
                                                                    }
                                                                    rawRealtimeMs4 = btTxTotalBytes2;
                                                                    NU7 = NU3;
                                                                    uid2 = uid;
                                                                    rawRealtime15 = rawRealtime14;
                                                                    sb7 = sb6;
                                                                    printWriter3 = pw;
                                                                    printWriter6 = printWriter3;
                                                                    sb5 = sb7;
                                                                    totalRealtime = rawRealtime15;
                                                                    rawRealtimeMs = sb7;
                                                                    i5 = estimatedBatteryCapacity;
                                                                    NU3 = NSE;
                                                                    str2 = prefix;
                                                                    str = str2;
                                                                    sensors2 = sensors;
                                                                    sb5 = rawRealtimeMs;
                                                                    uidActivity = ((uidActivity3 | printTimer(printWriter6, sb5, u8.getVibratorOnTimer(), totalRealtime, i5, str, "Vibrator")) | printTimer(printWriter6, sb5, u8.getForegroundActivityTimer(), totalRealtime, i5, str, "Foreground activities")) | printTimer(printWriter3, sb5, u8.getForegroundServiceTimer(), totalRealtime, i5, str, "Foreground services");
                                                                    mobileRxPackets2 = 0;
                                                                    uidActivity2 = uidActivity;
                                                                    i8 = 0;
                                                                    while (i8 < 7) {
                                                                    }
                                                                    totalRealtime = rawRealtime15;
                                                                    if (mobileRxPackets2 > 0) {
                                                                    }
                                                                    uidMobileActiveTime22 = u8.getUserCpuTimeUs(estimatedBatteryCapacity);
                                                                    systemCpuTimeUs = u8.getSystemCpuTimeUs(estimatedBatteryCapacity);
                                                                    if (uidMobileActiveTime22 <= 0) {
                                                                    }
                                                                    rawRealtimeMs.setLength(0);
                                                                    rawRealtimeMs.append(str2);
                                                                    rawRealtimeMs.append("    Total cpu time: u=");
                                                                    formatTimeMs(rawRealtimeMs, uidMobileActiveTime22 / 1000);
                                                                    rawRealtimeMs.append("s=");
                                                                    formatTimeMs(rawRealtimeMs, systemCpuTimeUs / 1000);
                                                                    printWriter3.println(rawRealtimeMs.toString());
                                                                    cpuFreqTimes = u8.getCpuFreqTimes(estimatedBatteryCapacity);
                                                                    if (cpuFreqTimes != null) {
                                                                    }
                                                                    screenOffCpuFreqTimes = u8.getScreenOffCpuFreqTimes(estimatedBatteryCapacity);
                                                                    if (screenOffCpuFreqTimes != null) {
                                                                    }
                                                                    i11 = 0;
                                                                    while (i11 < 7) {
                                                                    }
                                                                    processStats = u8.getProcessStats();
                                                                    if (processStats != null) {
                                                                    }
                                                                    u7 = u6;
                                                                    packageStats = u7.getPackageStats();
                                                                    if (packageStats != null) {
                                                                    }
                                                                }
                                                            } else {
                                                                wifiWakeup2 = totalRealtime;
                                                                totalRealtime = uidWifiRunningTime2;
                                                            }
                                                        } else {
                                                            wifiRxBytes2 = btTxTotalBytes2;
                                                            wifiWakeup2 = totalRealtime;
                                                            btTxTotalBytes2 = wifiScanActualTimeBg2;
                                                            totalRealtime = uidWifiRunningTime2;
                                                        }
                                                    } else {
                                                        wifiRxBytes2 = btTxTotalBytes2;
                                                        wifiTxPackets2 = rawRealtimeMs;
                                                        wifiWakeup2 = totalRealtime;
                                                        rawRealtimeMs = wifiScanActualTime2;
                                                        btTxTotalBytes2 = wifiScanActualTimeBg2;
                                                        totalRealtime = uidWifiRunningTime2;
                                                    }
                                                } else {
                                                    wifiRxBytes2 = btTxTotalBytes2;
                                                    wifiTxPackets2 = rawRealtimeMs;
                                                    wifiWakeup2 = totalRealtime;
                                                    rawRealtimeMs = wifiScanActualTime2;
                                                    btTxTotalBytes2 = wifiScanActualTimeBg2;
                                                    totalRealtime = uidWifiRunningTime2;
                                                    i11 = wifiScanCountBg;
                                                }
                                            } else {
                                                wifiRxBytes2 = btTxTotalBytes2;
                                                wifiTxPackets2 = rawRealtimeMs;
                                                wifiWakeup2 = totalRealtime;
                                                uidMobileActiveCount2 = wifiScanCount;
                                                rawRealtimeMs = wifiScanActualTime2;
                                                btTxTotalBytes2 = wifiScanActualTimeBg2;
                                                totalRealtime = uidWifiRunningTime2;
                                                i11 = wifiScanCountBg;
                                            }
                                        } else {
                                            wifiRxBytes2 = btTxTotalBytes2;
                                            wifiTxPackets2 = rawRealtimeMs;
                                            wifiRxPackets3 = rawRealtime3;
                                            wifiWakeup2 = totalRealtime;
                                            uidMobileActiveCount2 = wifiScanCount;
                                            rawRealtime3 = wifiScanTime;
                                            rawRealtimeMs = wifiScanActualTime2;
                                            btTxTotalBytes2 = wifiScanActualTimeBg2;
                                            totalRealtime = uidWifiRunningTime2;
                                            i11 = wifiScanCountBg;
                                        }
                                        u2 = u;
                                        sb5 = sb4;
                                        sb5.setLength(0);
                                        sb5.append(str4);
                                        sb5.append("    Wifi Running: ");
                                        wifiScanActualTimeBg = btTxTotalBytes2;
                                        formatTimeMs(sb5, totalRealtime / 1000);
                                        sb5.append("(");
                                        batteryUptime = whichBatteryRealtime2;
                                        sb5.append(batteryStats3.formatRatioLocked(totalRealtime, batteryUptime));
                                        sb5.append(")\n");
                                        sb5.append(str4);
                                        sb5.append("    Full Wifi Lock: ");
                                        uidWifiRunningTime = totalRealtime;
                                        formatTimeMs(sb5, uidMobileActiveTime22 / 1000);
                                        sb5.append("(");
                                        sb5.append(batteryStats3.formatRatioLocked(uidMobileActiveTime22, batteryUptime));
                                        sb5.append(")\n");
                                        sb5.append(str4);
                                        sb5.append("    Wifi Scan (blamed): ");
                                        formatTimeMs(sb5, rawRealtime3 / 1000);
                                        sb5.append("(");
                                        sb5.append(batteryStats3.formatRatioLocked(rawRealtime3, batteryUptime));
                                        sb5.append(") ");
                                        sb5.append(uidMobileActiveCount2);
                                        sb5.append("x\n");
                                        sb5.append(str4);
                                        sb5.append("    Wifi Scan (actual): ");
                                        formatTimeMs(sb5, rawRealtimeMs / 1000);
                                        sb5.append("(");
                                        whichBatteryRealtime7 = batteryUptime;
                                        totalRealtime = rawRealtime12;
                                        sb5.append(batteryStats3.formatRatioLocked(rawRealtimeMs, batteryStats3.computeBatteryRealtime(totalRealtime, 0)));
                                        sb5.append(") ");
                                        sb5.append(uidMobileActiveCount2);
                                        sb5.append("x\n");
                                        sb5.append(str4);
                                        sb5.append("    Background Wifi Scan: ");
                                        wifiScanActualTime = rawRealtimeMs;
                                        btTxTotalBytes2 = wifiScanActualTimeBg;
                                        formatTimeMs(sb5, btTxTotalBytes2 / 1000);
                                        sb5.append("(");
                                        sb5.append(batteryStats3.formatRatioLocked(btTxTotalBytes2, batteryStats3.computeBatteryRealtime(totalRealtime, 0)));
                                        sb5.append(") ");
                                        sb5.append(i11);
                                        sb5.append("x");
                                        rawRealtimeMs = pw;
                                        rawRealtimeMs.println(sb5.toString());
                                        fullWifiLockOnTime = uidMobileActiveTime22;
                                        uidMobileActiveTime22 = wifiWakeup2;
                                        if (uidMobileActiveTime22 > 0) {
                                        }
                                        stringBuilder3 = new StringBuilder();
                                        stringBuilder3.append(str4);
                                        stringBuilder3.append("  ");
                                        u8 = u2;
                                        mobileActiveTime = btTxTotalBytes2;
                                        mobileTxTotalPackets2 = whichBatteryRealtime7;
                                        wifiWakeup3 = uidMobileActiveTime22;
                                        u3 = u8;
                                        printWriter5 = rawRealtimeMs;
                                        mobileTxTotalBytes = wifiScanActualTime;
                                        batteryStats3.printControllerActivityIfInteresting(rawRealtimeMs, sb5, stringBuilder3.toString(), WIFI_CONTROLLER_NAME, u8.getWifiControllerActivity(), which);
                                        if (btRxBytes > 0) {
                                        }
                                        pw.print(prefix);
                                        printWriter5.print("    Bluetooth network: ");
                                        gpsBatteryDrainMaMs = btRxBytes;
                                        printWriter5.print(batteryStats3.formatBytesLocked(gpsBatteryDrainMaMs));
                                        printWriter5.print(" received, ");
                                        printWriter5.print(batteryStats3.formatBytesLocked(rawRealtime3));
                                        printWriter5.println(" sent");
                                        bleTimer = u3.getBluetoothScanTimer();
                                        if (bleTimer != null) {
                                        }
                                        if (u3.hasUserActivity()) {
                                        }
                                        wakelocks2 = u3.getWakelockStats();
                                        i11 = 0;
                                        if (wakelocks2 != null) {
                                        }
                                        if (i8 > 1) {
                                        }
                                        u5 = u4;
                                        timer = u5.getMulticastWakelockStats();
                                        if (timer != null) {
                                        }
                                        syncs = u5.getSyncStats();
                                        if (syncs != null) {
                                        }
                                        totalPartialWakelock = totalRealtime;
                                        rawRealtime8 = rawRealtimeMs5;
                                        jobs = u5.getJobStats();
                                        if (jobs != null) {
                                        }
                                        rawRealtime13 = uidMobileActiveTime22;
                                        completions = u5.getJobCompletionStats();
                                        while (NU3 >= 0) {
                                        }
                                        u5.getDeferredJobsLineLocked(sb5, estimatedBatteryCapacity);
                                        if (sb5.length() > 0) {
                                        }
                                        iw2 = 1;
                                        uid = uid3;
                                        uidStats6 = uidStats8;
                                        btTxTotalBytes2 = rawRealtime8;
                                        iu = iu2;
                                        NU3 = NU8;
                                        wifiRxTotalBytes2 = rawRealtime13;
                                        totalRealtime = wifiRxTotalBytes2;
                                        u8 = u5;
                                        minLearnedBatteryCapacity = 0;
                                        i5 = estimatedBatteryCapacity;
                                        rawRealtime14 = wifiRxTotalBytes2;
                                        sb6 = sb5;
                                        str = rawRealtimeMs;
                                        cpuFreqs2 = cpuFreqs3;
                                        sb5 = sb6;
                                        totalRealtime = rawRealtime14;
                                        needComma = (((uidActivity3 | printTimer(countWakelock, sb5, u5.getFlashlightTurnedOnTimer(), totalRealtime, i5, str, "Flashlight")) | printTimer(pw, sb5, u8.getCameraTurnedOnTimer(), totalRealtime, i5, str, "Camera")) | printTimer(pw, sb5, u8.getVideoTurnedOnTimer(), totalRealtime, i5, str, "Video")) | printTimer(pw, sb5, u8.getAudioTurnedOnTimer(), totalRealtime, i5, str, "Audio");
                                        sensors = u8.getSensorStats();
                                        NSE = sensors.size();
                                        uidActivity3 = needComma;
                                        i11 = 0;
                                        while (i11 < NSE) {
                                        }
                                        rawRealtimeMs4 = btTxTotalBytes2;
                                        NU7 = NU3;
                                        uid2 = uid;
                                        rawRealtime15 = rawRealtime14;
                                        sb7 = sb6;
                                        printWriter3 = pw;
                                        printWriter6 = printWriter3;
                                        sb5 = sb7;
                                        totalRealtime = rawRealtime15;
                                        rawRealtimeMs = sb7;
                                        i5 = estimatedBatteryCapacity;
                                        NU3 = NSE;
                                        str2 = prefix;
                                        str = str2;
                                        sensors2 = sensors;
                                        sb5 = rawRealtimeMs;
                                        uidActivity = ((uidActivity3 | printTimer(printWriter6, sb5, u8.getVibratorOnTimer(), totalRealtime, i5, str, "Vibrator")) | printTimer(printWriter6, sb5, u8.getForegroundActivityTimer(), totalRealtime, i5, str, "Foreground activities")) | printTimer(printWriter3, sb5, u8.getForegroundServiceTimer(), totalRealtime, i5, str, "Foreground services");
                                        mobileRxPackets2 = 0;
                                        uidActivity2 = uidActivity;
                                        i8 = 0;
                                        while (i8 < 7) {
                                        }
                                        totalRealtime = rawRealtime15;
                                        if (mobileRxPackets2 > 0) {
                                        }
                                        uidMobileActiveTime22 = u8.getUserCpuTimeUs(estimatedBatteryCapacity);
                                        systemCpuTimeUs = u8.getSystemCpuTimeUs(estimatedBatteryCapacity);
                                        if (uidMobileActiveTime22 <= 0) {
                                        }
                                        rawRealtimeMs.setLength(0);
                                        rawRealtimeMs.append(str2);
                                        rawRealtimeMs.append("    Total cpu time: u=");
                                        formatTimeMs(rawRealtimeMs, uidMobileActiveTime22 / 1000);
                                        rawRealtimeMs.append("s=");
                                        formatTimeMs(rawRealtimeMs, systemCpuTimeUs / 1000);
                                        printWriter3.println(rawRealtimeMs.toString());
                                        cpuFreqTimes = u8.getCpuFreqTimes(estimatedBatteryCapacity);
                                        if (cpuFreqTimes != null) {
                                        }
                                        screenOffCpuFreqTimes = u8.getScreenOffCpuFreqTimes(estimatedBatteryCapacity);
                                        if (screenOffCpuFreqTimes != null) {
                                        }
                                        i11 = 0;
                                        while (i11 < 7) {
                                        }
                                        processStats = u8.getProcessStats();
                                        if (processStats != null) {
                                        }
                                        u7 = u6;
                                        packageStats = u7.getPackageStats();
                                        if (packageStats != null) {
                                        }
                                    }
                                } else {
                                    rawRealtimeMs = wifiTxPackets;
                                }
                            }
                            pw.print(prefix);
                            printWriter4 = pw;
                            printWriter4.print("    Wi-Fi network: ");
                            btTxTotalBytes2 = wifiRxBytes;
                            printWriter4.print(batteryStats3.formatBytesLocked(btTxTotalBytes2));
                            printWriter4.print(" received, ");
                            rawRealtime12 = rawRealtime10;
                            uidMobileActiveTime22 = wifiTxBytes2;
                            printWriter4.print(batteryStats3.formatBytesLocked(uidMobileActiveTime22));
                            printWriter4.print(" sent (packets ");
                            printWriter4.print(rawRealtime3);
                            printWriter4.print(" received, ");
                            printWriter4.print(rawRealtimeMs);
                            printWriter4.println(" sent)");
                            wifiTxBytes = uidMobileActiveTime22;
                            uidMobileActiveTime22 = fullWifiLockOnTime2;
                            if (uidMobileActiveTime22 == 0) {
                            }
                            u2 = u;
                            sb5 = sb4;
                            sb5.setLength(0);
                            sb5.append(str4);
                            sb5.append("    Wifi Running: ");
                            wifiScanActualTimeBg = btTxTotalBytes2;
                            formatTimeMs(sb5, totalRealtime / 1000);
                            sb5.append("(");
                            batteryUptime = whichBatteryRealtime2;
                            sb5.append(batteryStats3.formatRatioLocked(totalRealtime, batteryUptime));
                            sb5.append(")\n");
                            sb5.append(str4);
                            sb5.append("    Full Wifi Lock: ");
                            uidWifiRunningTime = totalRealtime;
                            formatTimeMs(sb5, uidMobileActiveTime22 / 1000);
                            sb5.append("(");
                            sb5.append(batteryStats3.formatRatioLocked(uidMobileActiveTime22, batteryUptime));
                            sb5.append(")\n");
                            sb5.append(str4);
                            sb5.append("    Wifi Scan (blamed): ");
                            formatTimeMs(sb5, rawRealtime3 / 1000);
                            sb5.append("(");
                            sb5.append(batteryStats3.formatRatioLocked(rawRealtime3, batteryUptime));
                            sb5.append(") ");
                            sb5.append(uidMobileActiveCount2);
                            sb5.append("x\n");
                            sb5.append(str4);
                            sb5.append("    Wifi Scan (actual): ");
                            formatTimeMs(sb5, rawRealtimeMs / 1000);
                            sb5.append("(");
                            whichBatteryRealtime7 = batteryUptime;
                            totalRealtime = rawRealtime12;
                            sb5.append(batteryStats3.formatRatioLocked(rawRealtimeMs, batteryStats3.computeBatteryRealtime(totalRealtime, 0)));
                            sb5.append(") ");
                            sb5.append(uidMobileActiveCount2);
                            sb5.append("x\n");
                            sb5.append(str4);
                            sb5.append("    Background Wifi Scan: ");
                            wifiScanActualTime = rawRealtimeMs;
                            btTxTotalBytes2 = wifiScanActualTimeBg;
                            formatTimeMs(sb5, btTxTotalBytes2 / 1000);
                            sb5.append("(");
                            sb5.append(batteryStats3.formatRatioLocked(btTxTotalBytes2, batteryStats3.computeBatteryRealtime(totalRealtime, 0)));
                            sb5.append(") ");
                            sb5.append(i11);
                            sb5.append("x");
                            rawRealtimeMs = pw;
                            rawRealtimeMs.println(sb5.toString());
                            fullWifiLockOnTime = uidMobileActiveTime22;
                            uidMobileActiveTime22 = wifiWakeup2;
                            if (uidMobileActiveTime22 > 0) {
                            }
                            stringBuilder3 = new StringBuilder();
                            stringBuilder3.append(str4);
                            stringBuilder3.append("  ");
                            u8 = u2;
                            mobileActiveTime = btTxTotalBytes2;
                            mobileTxTotalPackets2 = whichBatteryRealtime7;
                            wifiWakeup3 = uidMobileActiveTime22;
                            u3 = u8;
                            printWriter5 = rawRealtimeMs;
                            mobileTxTotalBytes = wifiScanActualTime;
                            batteryStats3.printControllerActivityIfInteresting(rawRealtimeMs, sb5, stringBuilder3.toString(), WIFI_CONTROLLER_NAME, u8.getWifiControllerActivity(), which);
                            if (btRxBytes > 0) {
                            }
                            pw.print(prefix);
                            printWriter5.print("    Bluetooth network: ");
                            gpsBatteryDrainMaMs = btRxBytes;
                            printWriter5.print(batteryStats3.formatBytesLocked(gpsBatteryDrainMaMs));
                            printWriter5.print(" received, ");
                            printWriter5.print(batteryStats3.formatBytesLocked(rawRealtime3));
                            printWriter5.println(" sent");
                            bleTimer = u3.getBluetoothScanTimer();
                            if (bleTimer != null) {
                            }
                            if (u3.hasUserActivity()) {
                            }
                            wakelocks2 = u3.getWakelockStats();
                            i11 = 0;
                            if (wakelocks2 != null) {
                            }
                            if (i8 > 1) {
                            }
                            u5 = u4;
                            timer = u5.getMulticastWakelockStats();
                            if (timer != null) {
                            }
                            syncs = u5.getSyncStats();
                            if (syncs != null) {
                            }
                            totalPartialWakelock = totalRealtime;
                            rawRealtime8 = rawRealtimeMs5;
                            jobs = u5.getJobStats();
                            if (jobs != null) {
                            }
                            rawRealtime13 = uidMobileActiveTime22;
                            completions = u5.getJobCompletionStats();
                            while (NU3 >= 0) {
                            }
                            u5.getDeferredJobsLineLocked(sb5, estimatedBatteryCapacity);
                            if (sb5.length() > 0) {
                            }
                            iw2 = 1;
                            uid = uid3;
                            uidStats6 = uidStats8;
                            btTxTotalBytes2 = rawRealtime8;
                            iu = iu2;
                            NU3 = NU8;
                            wifiRxTotalBytes2 = rawRealtime13;
                            totalRealtime = wifiRxTotalBytes2;
                            u8 = u5;
                            minLearnedBatteryCapacity = 0;
                            i5 = estimatedBatteryCapacity;
                            rawRealtime14 = wifiRxTotalBytes2;
                            sb6 = sb5;
                            str = rawRealtimeMs;
                            cpuFreqs2 = cpuFreqs3;
                            sb5 = sb6;
                            totalRealtime = rawRealtime14;
                            needComma = (((uidActivity3 | printTimer(countWakelock, sb5, u5.getFlashlightTurnedOnTimer(), totalRealtime, i5, str, "Flashlight")) | printTimer(pw, sb5, u8.getCameraTurnedOnTimer(), totalRealtime, i5, str, "Camera")) | printTimer(pw, sb5, u8.getVideoTurnedOnTimer(), totalRealtime, i5, str, "Video")) | printTimer(pw, sb5, u8.getAudioTurnedOnTimer(), totalRealtime, i5, str, "Audio");
                            sensors = u8.getSensorStats();
                            NSE = sensors.size();
                            uidActivity3 = needComma;
                            i11 = 0;
                            while (i11 < NSE) {
                            }
                            rawRealtimeMs4 = btTxTotalBytes2;
                            NU7 = NU3;
                            uid2 = uid;
                            rawRealtime15 = rawRealtime14;
                            sb7 = sb6;
                            printWriter3 = pw;
                            printWriter6 = printWriter3;
                            sb5 = sb7;
                            totalRealtime = rawRealtime15;
                            rawRealtimeMs = sb7;
                            i5 = estimatedBatteryCapacity;
                            NU3 = NSE;
                            str2 = prefix;
                            str = str2;
                            sensors2 = sensors;
                            sb5 = rawRealtimeMs;
                            uidActivity = ((uidActivity3 | printTimer(printWriter6, sb5, u8.getVibratorOnTimer(), totalRealtime, i5, str, "Vibrator")) | printTimer(printWriter6, sb5, u8.getForegroundActivityTimer(), totalRealtime, i5, str, "Foreground activities")) | printTimer(printWriter3, sb5, u8.getForegroundServiceTimer(), totalRealtime, i5, str, "Foreground services");
                            mobileRxPackets2 = 0;
                            uidActivity2 = uidActivity;
                            i8 = 0;
                            while (i8 < 7) {
                            }
                            totalRealtime = rawRealtime15;
                            if (mobileRxPackets2 > 0) {
                            }
                            uidMobileActiveTime22 = u8.getUserCpuTimeUs(estimatedBatteryCapacity);
                            systemCpuTimeUs = u8.getSystemCpuTimeUs(estimatedBatteryCapacity);
                            if (uidMobileActiveTime22 <= 0) {
                            }
                            rawRealtimeMs.setLength(0);
                            rawRealtimeMs.append(str2);
                            rawRealtimeMs.append("    Total cpu time: u=");
                            formatTimeMs(rawRealtimeMs, uidMobileActiveTime22 / 1000);
                            rawRealtimeMs.append("s=");
                            formatTimeMs(rawRealtimeMs, systemCpuTimeUs / 1000);
                            printWriter3.println(rawRealtimeMs.toString());
                            cpuFreqTimes = u8.getCpuFreqTimes(estimatedBatteryCapacity);
                            if (cpuFreqTimes != null) {
                            }
                            screenOffCpuFreqTimes = u8.getScreenOffCpuFreqTimes(estimatedBatteryCapacity);
                            if (screenOffCpuFreqTimes != null) {
                            }
                            i11 = 0;
                            while (i11 < 7) {
                            }
                            processStats = u8.getProcessStats();
                            if (processStats != null) {
                            }
                            u7 = u6;
                            packageStats = u7.getPackageStats();
                            if (packageStats != null) {
                            }
                        }
                    }
                    wifiRxPackets = wifiRxPackets2;
                    rawRealtimeMs = pw;
                    pw.print(prefix);
                    rawRealtimeMs.print("    Mobile network: ");
                    wifiWakeup = whichBatteryRealtime5;
                    whichBatteryRealtime5 = mobileTxBytes;
                    batteryStats2 = this;
                    rawRealtimeMs.print(batteryStats2.formatBytesLocked(totalRealtime));
                    rawRealtimeMs.print(" received, ");
                    rawRealtimeMs.print(batteryStats2.formatBytesLocked(whichBatteryRealtime5));
                    rawRealtimeMs.print(" sent (packets ");
                    rawRealtimeMs.print(mobileRxPackets2);
                    rawRealtimeMs.print(" received, ");
                    rawRealtimeMs.print(mobileTxBytes3);
                    rawRealtimeMs.println(" sent)");
                    mobileRxBytes = totalRealtime;
                    totalRealtime = uidMobileActiveTime3;
                    if (totalRealtime <= 0) {
                    }
                    mobileTxBytes2 = whichBatteryRealtime5;
                    stringBuilder = sb8;
                    stringBuilder.setLength(0);
                    stringBuilder.append(prefix);
                    stringBuilder.append("    Mobile radio active: ");
                    formatTimeMs(stringBuilder, totalRealtime / 1000);
                    stringBuilder.append("(");
                    wifiScanCountBg = iw;
                    whichBatteryRealtime5 = mobileActiveTime2;
                    stringBuilder.append(batteryStats2.formatRatioLocked(totalRealtime, whichBatteryRealtime5));
                    stringBuilder.append(") ");
                    stringBuilder.append(uidMobileActiveCount2);
                    stringBuilder.append("x");
                    packets = mobileRxPackets2 + mobileTxBytes3;
                    if (packets == 0) {
                    }
                    mobileActiveTime3 = whichBatteryRealtime5;
                    whichBatteryRealtime5 = packets;
                    stringBuilder.append(" @ ");
                    mobileTxPackets = mobileTxBytes3;
                    mobileRxPackets = mobileRxPackets2;
                    stringBuilder.append(BatteryStatsHelper.makemAh(((double) (totalRealtime / 1000)) / ((double) whichBatteryRealtime5)));
                    stringBuilder.append(" mspp");
                    rawRealtimeMs.println(stringBuilder.toString());
                    if (uidMobileActiveTime22 <= 0) {
                    }
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(str3);
                    stringBuilder2.append("  ");
                    batteryStats3 = batteryStats2;
                    sb4 = stringBuilder;
                    u = u8;
                    mobileWakeup = uidMobileActiveTime22;
                    uidMobileActiveCount = uidMobileActiveCount2;
                    rawRealtime10 = rawRealtime16;
                    uidMobileActiveTime = totalRealtime;
                    totalRealtime = wifiWakeup;
                    wifiTxTotalPackets2 = mobileActiveTime3;
                    str4 = str3;
                    rpmStats2 = rpmStats3;
                    i5 = estimatedBatteryCapacity;
                    mMemoryStats3 = mMemoryStats;
                    batteryStats2.printControllerActivityIfInteresting(rawRealtimeMs, sb4, stringBuilder2.toString(), CELLULAR_CONTROLLER_NAME, u8.getModemControllerActivity(), which);
                    if (wifiRxBytes <= 0) {
                    }
                    rawRealtimeMs = wifiTxPackets;
                    rawRealtime3 = wifiRxPackets;
                    pw.print(prefix);
                    printWriter4 = pw;
                    printWriter4.print("    Wi-Fi network: ");
                    btTxTotalBytes2 = wifiRxBytes;
                    printWriter4.print(batteryStats3.formatBytesLocked(btTxTotalBytes2));
                    printWriter4.print(" received, ");
                    rawRealtime12 = rawRealtime10;
                    uidMobileActiveTime22 = wifiTxBytes2;
                    printWriter4.print(batteryStats3.formatBytesLocked(uidMobileActiveTime22));
                    printWriter4.print(" sent (packets ");
                    printWriter4.print(rawRealtime3);
                    printWriter4.print(" received, ");
                    printWriter4.print(rawRealtimeMs);
                    printWriter4.println(" sent)");
                    wifiTxBytes = uidMobileActiveTime22;
                    uidMobileActiveTime22 = fullWifiLockOnTime2;
                    if (uidMobileActiveTime22 == 0) {
                    }
                    u2 = u;
                    sb5 = sb4;
                    sb5.setLength(0);
                    sb5.append(str4);
                    sb5.append("    Wifi Running: ");
                    wifiScanActualTimeBg = btTxTotalBytes2;
                    formatTimeMs(sb5, totalRealtime / 1000);
                    sb5.append("(");
                    batteryUptime = whichBatteryRealtime2;
                    sb5.append(batteryStats3.formatRatioLocked(totalRealtime, batteryUptime));
                    sb5.append(")\n");
                    sb5.append(str4);
                    sb5.append("    Full Wifi Lock: ");
                    uidWifiRunningTime = totalRealtime;
                    formatTimeMs(sb5, uidMobileActiveTime22 / 1000);
                    sb5.append("(");
                    sb5.append(batteryStats3.formatRatioLocked(uidMobileActiveTime22, batteryUptime));
                    sb5.append(")\n");
                    sb5.append(str4);
                    sb5.append("    Wifi Scan (blamed): ");
                    formatTimeMs(sb5, rawRealtime3 / 1000);
                    sb5.append("(");
                    sb5.append(batteryStats3.formatRatioLocked(rawRealtime3, batteryUptime));
                    sb5.append(") ");
                    sb5.append(uidMobileActiveCount2);
                    sb5.append("x\n");
                    sb5.append(str4);
                    sb5.append("    Wifi Scan (actual): ");
                    formatTimeMs(sb5, rawRealtimeMs / 1000);
                    sb5.append("(");
                    whichBatteryRealtime7 = batteryUptime;
                    totalRealtime = rawRealtime12;
                    sb5.append(batteryStats3.formatRatioLocked(rawRealtimeMs, batteryStats3.computeBatteryRealtime(totalRealtime, 0)));
                    sb5.append(") ");
                    sb5.append(uidMobileActiveCount2);
                    sb5.append("x\n");
                    sb5.append(str4);
                    sb5.append("    Background Wifi Scan: ");
                    wifiScanActualTime = rawRealtimeMs;
                    btTxTotalBytes2 = wifiScanActualTimeBg;
                    formatTimeMs(sb5, btTxTotalBytes2 / 1000);
                    sb5.append("(");
                    sb5.append(batteryStats3.formatRatioLocked(btTxTotalBytes2, batteryStats3.computeBatteryRealtime(totalRealtime, 0)));
                    sb5.append(") ");
                    sb5.append(i11);
                    sb5.append("x");
                    rawRealtimeMs = pw;
                    rawRealtimeMs.println(sb5.toString());
                    fullWifiLockOnTime = uidMobileActiveTime22;
                    uidMobileActiveTime22 = wifiWakeup2;
                    if (uidMobileActiveTime22 > 0) {
                    }
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(str4);
                    stringBuilder3.append("  ");
                    u8 = u2;
                    mobileActiveTime = btTxTotalBytes2;
                    mobileTxTotalPackets2 = whichBatteryRealtime7;
                    wifiWakeup3 = uidMobileActiveTime22;
                    u3 = u8;
                    printWriter5 = rawRealtimeMs;
                    mobileTxTotalBytes = wifiScanActualTime;
                    batteryStats3.printControllerActivityIfInteresting(rawRealtimeMs, sb5, stringBuilder3.toString(), WIFI_CONTROLLER_NAME, u8.getWifiControllerActivity(), which);
                    if (btRxBytes > 0) {
                    }
                    pw.print(prefix);
                    printWriter5.print("    Bluetooth network: ");
                    gpsBatteryDrainMaMs = btRxBytes;
                    printWriter5.print(batteryStats3.formatBytesLocked(gpsBatteryDrainMaMs));
                    printWriter5.print(" received, ");
                    printWriter5.print(batteryStats3.formatBytesLocked(rawRealtime3));
                    printWriter5.println(" sent");
                    bleTimer = u3.getBluetoothScanTimer();
                    if (bleTimer != null) {
                    }
                    if (u3.hasUserActivity()) {
                    }
                    wakelocks2 = u3.getWakelockStats();
                    i11 = 0;
                    if (wakelocks2 != null) {
                    }
                    if (i8 > 1) {
                    }
                    u5 = u4;
                    timer = u5.getMulticastWakelockStats();
                    if (timer != null) {
                    }
                    syncs = u5.getSyncStats();
                    if (syncs != null) {
                    }
                    totalPartialWakelock = totalRealtime;
                    rawRealtime8 = rawRealtimeMs5;
                    jobs = u5.getJobStats();
                    if (jobs != null) {
                    }
                    rawRealtime13 = uidMobileActiveTime22;
                    completions = u5.getJobCompletionStats();
                    while (NU3 >= 0) {
                    }
                    u5.getDeferredJobsLineLocked(sb5, estimatedBatteryCapacity);
                    if (sb5.length() > 0) {
                    }
                    iw2 = 1;
                    uid = uid3;
                    uidStats6 = uidStats8;
                    btTxTotalBytes2 = rawRealtime8;
                    iu = iu2;
                    NU3 = NU8;
                    wifiRxTotalBytes2 = rawRealtime13;
                    totalRealtime = wifiRxTotalBytes2;
                    u8 = u5;
                    minLearnedBatteryCapacity = 0;
                    i5 = estimatedBatteryCapacity;
                    rawRealtime14 = wifiRxTotalBytes2;
                    sb6 = sb5;
                    str = rawRealtimeMs;
                    cpuFreqs2 = cpuFreqs3;
                    sb5 = sb6;
                    totalRealtime = rawRealtime14;
                    needComma = (((uidActivity3 | printTimer(countWakelock, sb5, u5.getFlashlightTurnedOnTimer(), totalRealtime, i5, str, "Flashlight")) | printTimer(pw, sb5, u8.getCameraTurnedOnTimer(), totalRealtime, i5, str, "Camera")) | printTimer(pw, sb5, u8.getVideoTurnedOnTimer(), totalRealtime, i5, str, "Video")) | printTimer(pw, sb5, u8.getAudioTurnedOnTimer(), totalRealtime, i5, str, "Audio");
                    sensors = u8.getSensorStats();
                    NSE = sensors.size();
                    uidActivity3 = needComma;
                    i11 = 0;
                    while (i11 < NSE) {
                    }
                    rawRealtimeMs4 = btTxTotalBytes2;
                    NU7 = NU3;
                    uid2 = uid;
                    rawRealtime15 = rawRealtime14;
                    sb7 = sb6;
                    printWriter3 = pw;
                    printWriter6 = printWriter3;
                    sb5 = sb7;
                    totalRealtime = rawRealtime15;
                    rawRealtimeMs = sb7;
                    i5 = estimatedBatteryCapacity;
                    NU3 = NSE;
                    str2 = prefix;
                    str = str2;
                    sensors2 = sensors;
                    sb5 = rawRealtimeMs;
                    uidActivity = ((uidActivity3 | printTimer(printWriter6, sb5, u8.getVibratorOnTimer(), totalRealtime, i5, str, "Vibrator")) | printTimer(printWriter6, sb5, u8.getForegroundActivityTimer(), totalRealtime, i5, str, "Foreground activities")) | printTimer(printWriter3, sb5, u8.getForegroundServiceTimer(), totalRealtime, i5, str, "Foreground services");
                    mobileRxPackets2 = 0;
                    uidActivity2 = uidActivity;
                    i8 = 0;
                    while (i8 < 7) {
                    }
                    totalRealtime = rawRealtime15;
                    if (mobileRxPackets2 > 0) {
                    }
                    uidMobileActiveTime22 = u8.getUserCpuTimeUs(estimatedBatteryCapacity);
                    systemCpuTimeUs = u8.getSystemCpuTimeUs(estimatedBatteryCapacity);
                    if (uidMobileActiveTime22 <= 0) {
                    }
                    rawRealtimeMs.setLength(0);
                    rawRealtimeMs.append(str2);
                    rawRealtimeMs.append("    Total cpu time: u=");
                    formatTimeMs(rawRealtimeMs, uidMobileActiveTime22 / 1000);
                    rawRealtimeMs.append("s=");
                    formatTimeMs(rawRealtimeMs, systemCpuTimeUs / 1000);
                    printWriter3.println(rawRealtimeMs.toString());
                    cpuFreqTimes = u8.getCpuFreqTimes(estimatedBatteryCapacity);
                    if (cpuFreqTimes != null) {
                    }
                    screenOffCpuFreqTimes = u8.getScreenOffCpuFreqTimes(estimatedBatteryCapacity);
                    if (screenOffCpuFreqTimes != null) {
                    }
                    i11 = 0;
                    while (i11 < 7) {
                    }
                    processStats = u8.getProcessStats();
                    if (processStats != null) {
                    }
                    u7 = u6;
                    packageStats = u7.getPackageStats();
                    if (packageStats != null) {
                    }
                } else {
                    rawRealtime11 = rawRealtimeMs;
                    rpmStats2 = rpmStats;
                    uidStats6 = uidStats7;
                    iu = i6;
                    NU7 = NU6;
                    printWriter3 = printWriter;
                    rawRealtimeMs = sb2;
                    cpuFreqs2 = cpuFreqs;
                    rawRealtime10 = batteryUptime3;
                    rawRealtimeMs4 = rawRealtimeMs3;
                    wifiTxTotalPackets2 = mobileActiveTime2;
                    mMemoryStats3 = mMemoryStats;
                    mobileTxTotalPackets2 = whichBatteryRealtime2;
                    str2 = prefix;
                    NU6 = which;
                }
                i8 = iu + 1;
                printWriter = printWriter3;
                batteryUptime3 = rawRealtime10;
                mobileActiveTime2 = wifiTxTotalPackets2;
                rpmStats = rpmStats2;
                mMemoryStats = mMemoryStats3;
                whichBatteryRealtime2 = mobileTxTotalPackets2;
                uidStats5 = uidStats6;
                cpuFreqs = cpuFreqs2;
                NU5 = NU7;
                rawRealtimeMs3 = rawRealtimeMs4;
                estimatedBatteryCapacity = 0;
                sb2 = rawRealtimeMs;
                rawRealtimeMs = rawRealtime11;
            } else {
                rpmStats2 = rpmStats;
                NU7 = NU6;
                printWriter3 = printWriter;
                cpuFreqs2 = cpuFreqs;
                rawRealtimeMs4 = rawRealtimeMs3;
                wifiTxTotalPackets2 = mobileActiveTime2;
                uidStats6 = uidStats5;
                mMemoryStats3 = mMemoryStats;
                mobileTxTotalPackets2 = whichBatteryRealtime2;
                str2 = prefix;
                NU6 = which;
                return;
            }
        }
    }

    static void printBitDescriptions(StringBuilder sb, int oldval, int newval, HistoryTag wakelockTag, BitDescription[] descriptions, boolean longNames) {
        int diff = oldval ^ newval;
        if (diff != 0) {
            boolean didWake = false;
            for (BitDescription bd : descriptions) {
                if ((bd.mask & diff) != 0) {
                    sb.append(longNames ? WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER : ",");
                    if (bd.shift < 0) {
                        sb.append((bd.mask & newval) != 0 ? "+" : "-");
                        sb.append(longNames ? bd.name : bd.shortName);
                        if (bd.mask == 1073741824 && wakelockTag != null) {
                            didWake = true;
                            sb.append("=");
                            if (longNames) {
                                UserHandle.formatUid(sb, wakelockTag.uid);
                                sb.append(":\"");
                                sb.append(wakelockTag.string);
                                sb.append("\"");
                            } else {
                                sb.append(wakelockTag.poolIdx);
                            }
                        }
                    } else {
                        sb.append(longNames ? bd.name : bd.shortName);
                        sb.append("=");
                        int val = (bd.mask & newval) >> bd.shift;
                        if (bd.values == null || val < 0 || val >= bd.values.length) {
                            sb.append(val);
                        } else {
                            sb.append(longNames ? bd.values[val] : bd.shortValues[val]);
                        }
                    }
                }
            }
            if (!(didWake || wakelockTag == null)) {
                sb.append(longNames ? " wake_lock=" : ",w=");
                if (longNames) {
                    UserHandle.formatUid(sb, wakelockTag.uid);
                    sb.append(":\"");
                    sb.append(wakelockTag.string);
                    sb.append("\"");
                } else {
                    sb.append(wakelockTag.poolIdx);
                }
            }
        }
    }

    public void prepareForDumpLocked() {
    }

    private void printSizeValue(PrintWriter pw, long size) {
        float result = (float) size;
        String suffix = "";
        if (result >= 10240.0f) {
            suffix = "KB";
            result /= 1024.0f;
        }
        if (result >= 10240.0f) {
            suffix = "MB";
            result /= 1024.0f;
        }
        if (result >= 10240.0f) {
            suffix = "GB";
            result /= 1024.0f;
        }
        if (result >= 10240.0f) {
            suffix = "TB";
            result /= 1024.0f;
        }
        if (result >= 10240.0f) {
            suffix = "PB";
            result /= 1024.0f;
        }
        pw.print((int) result);
        pw.print(suffix);
    }

    private static boolean dumpTimeEstimate(PrintWriter pw, String label1, String label2, String label3, long estimatedTime) {
        if (estimatedTime < 0) {
            return false;
        }
        pw.print(label1);
        pw.print(label2);
        pw.print(label3);
        StringBuilder sb = new StringBuilder(64);
        formatTimeMs(sb, estimatedTime);
        pw.print(sb);
        pw.println();
        return true;
    }

    private static boolean dumpDurationSteps(PrintWriter pw, String prefix, String header, LevelStepTracker steps, boolean checkin) {
        PrintWriter printWriter = pw;
        String str = header;
        LevelStepTracker levelStepTracker = steps;
        int i = 0;
        if (levelStepTracker == null) {
            return false;
        }
        int count = levelStepTracker.mNumStepDurations;
        if (count <= 0) {
            return false;
        }
        if (!checkin) {
            printWriter.println(str);
        }
        String[] lineArgs = new String[5];
        int i2 = 0;
        while (i2 < count) {
            int count2;
            long duration = levelStepTracker.getDurationAt(i2);
            int level = levelStepTracker.getLevelAt(i2);
            long initMode = (long) levelStepTracker.getInitModeAt(i2);
            long modMode = (long) levelStepTracker.getModModeAt(i2);
            if (checkin) {
                lineArgs[i] = Long.toString(duration);
                lineArgs[1] = Integer.toString(level);
                if ((modMode & 3) == 0) {
                    count2 = count;
                    switch (((int) (initMode & 3)) + 1) {
                        case 1:
                            lineArgs[2] = "s-";
                            break;
                        case 2:
                            lineArgs[2] = "s+";
                            break;
                        case 3:
                            lineArgs[2] = "sd";
                            break;
                        case 4:
                            lineArgs[2] = "sds";
                            break;
                        default:
                            lineArgs[2] = "?";
                            break;
                    }
                }
                count2 = count;
                lineArgs[2] = "";
                if ((modMode & 4) == 0) {
                    lineArgs[3] = (initMode & 4) != 0 ? "p+" : "p-";
                } else {
                    lineArgs[3] = "";
                }
                if ((modMode & 8) == 0) {
                    lineArgs[4] = (8 & initMode) != 0 ? "i+" : "i-";
                } else {
                    lineArgs[4] = "";
                }
                dumpLine(printWriter, 0, "i", str, (Object[]) lineArgs);
            } else {
                count2 = count;
                pw.print(prefix);
                printWriter.print("#");
                printWriter.print(i2);
                printWriter.print(": ");
                TimeUtils.formatDuration(duration, printWriter);
                printWriter.print(" to ");
                printWriter.print(level);
                count = 0;
                if ((modMode & 3) == 0) {
                    printWriter.print(" (");
                    switch (((int) (initMode & 3)) + 1) {
                        case 1:
                            printWriter.print("screen-off");
                            break;
                        case 2:
                            printWriter.print("screen-on");
                            break;
                        case 3:
                            printWriter.print("screen-doze");
                            break;
                        case 4:
                            printWriter.print("screen-doze-suspend");
                            break;
                        default:
                            printWriter.print("screen-?");
                            break;
                    }
                    count = 1;
                }
                if ((modMode & 4) == 0) {
                    printWriter.print(count != 0 ? ", " : " (");
                    printWriter.print((initMode & 4) != 0 ? "power-save-on" : "power-save-off");
                    count = 1;
                }
                if ((modMode & 8) == 0) {
                    printWriter.print(count != 0 ? ", " : " (");
                    printWriter.print((initMode & 8) != 0 ? "device-idle-on" : "device-idle-off");
                    count = 1;
                }
                if (count != 0) {
                    printWriter.print(")");
                }
                pw.println();
            }
            i2++;
            count = count2;
            str = header;
            levelStepTracker = steps;
            i = 0;
        }
        return true;
    }

    private static void dumpDurationSteps(ProtoOutputStream proto, long fieldId, LevelStepTracker steps) {
        ProtoOutputStream protoOutputStream = proto;
        LevelStepTracker levelStepTracker = steps;
        if (levelStepTracker != null) {
            int count = levelStepTracker.mNumStepDurations;
            for (int i = 0; i < count; i++) {
                long token = proto.start(fieldId);
                protoOutputStream.write(1112396529665L, levelStepTracker.getDurationAt(i));
                protoOutputStream.write(1120986464258L, levelStepTracker.getLevelAt(i));
                long initMode = (long) levelStepTracker.getInitModeAt(i);
                long modMode = (long) levelStepTracker.getModModeAt(i);
                int ds = 0;
                int i2 = 1;
                if ((modMode & 3) == 0) {
                    switch (((int) (3 & initMode)) + 1) {
                        case 1:
                            ds = 2;
                            break;
                        case 2:
                            ds = 1;
                            break;
                        case 3:
                            ds = 3;
                            break;
                        case 4:
                            ds = 4;
                            break;
                        default:
                            ds = 5;
                            break;
                    }
                }
                protoOutputStream.write(1159641169923L, ds);
                int psm = 0;
                int i3 = 2;
                if ((modMode & 4) == 0) {
                    if ((4 & initMode) == 0) {
                        i2 = 2;
                    }
                    psm = i2;
                }
                protoOutputStream.write(1159641169924L, psm);
                int im = 0;
                if ((modMode & 8) == 0) {
                    if ((8 & initMode) == 0) {
                        i3 = 3;
                    }
                    im = i3;
                }
                protoOutputStream.write(1159641169925L, im);
                protoOutputStream.end(token);
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:61:0x0183  */
    /* JADX WARNING: Removed duplicated region for block: B:35:0x009a  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void dumpHistoryLocked(PrintWriter pw, int flags, long histStart, boolean checkin) {
        PrintWriter printWriter = pw;
        HistoryPrinter hprinter = new HistoryPrinter();
        HistoryItem rec = new HistoryItem();
        boolean printed = false;
        long baseTime = -1;
        long lastTime = -1;
        HistoryEventTracker tracker = null;
        while (true) {
            HistoryEventTracker tracker2 = tracker;
            if (!getNextHistoryLocked(rec)) {
                break;
            }
            lastTime = rec.time;
            if (baseTime < 0) {
                baseTime = lastTime;
            }
            long baseTime2 = baseTime;
            if (rec.time >= histStart) {
                long lastTime2;
                HistoryEventTracker tracker3;
                boolean printed2;
                if (histStart < 0 || printed) {
                    lastTime2 = lastTime;
                    tracker3 = tracker2;
                    printed2 = printed;
                } else {
                    HistoryEventTracker tracker4;
                    byte b;
                    if (rec.cmd == (byte) 5 || rec.cmd == (byte) 7 || rec.cmd == (byte) 4) {
                        lastTime2 = lastTime;
                        tracker4 = tracker2;
                    } else if (rec.cmd == (byte) 8) {
                        lastTime2 = lastTime;
                        tracker4 = tracker2;
                    } else {
                        tracker4 = tracker2;
                        if (rec.currentTime != 0) {
                            tracker2 = rec.cmd;
                            rec.cmd = (byte) 5;
                            lastTime2 = lastTime;
                            hprinter.printNextItem(printWriter, rec, baseTime2, checkin, (flags & 32) != 0);
                            rec.cmd = tracker2;
                            printed2 = true;
                            b = (byte) 0;
                        } else {
                            lastTime2 = lastTime;
                            printed2 = printed;
                            b = (byte) 0;
                        }
                        if (tracker4 == null) {
                            HistoryTag oldEventTag;
                            if (rec.cmd != (byte) 0) {
                                hprinter.printNextItem(printWriter, rec, baseTime2, checkin, (flags & 32) != 0 ? true : b);
                                rec.cmd = b;
                            }
                            tracker2 = rec.eventCode;
                            HistoryTag oldEventTag2 = rec.eventTag;
                            rec.eventTag = new HistoryTag();
                            int i = b;
                            while (true) {
                                int i2 = i;
                                if (i2 >= 22) {
                                    break;
                                }
                                HistoryEventTracker tracker5 = tracker4;
                                HashMap<String, SparseIntArray> active = tracker5.getStateForEvent(i2);
                                if (active != null) {
                                    Iterator it = active.entrySet().iterator();
                                    while (it.hasNext()) {
                                        Iterator it2;
                                        HashMap<String, SparseIntArray> active2;
                                        int i3;
                                        lastTime = (Entry) it.next();
                                        SparseIntArray uids = (SparseIntArray) lastTime.getValue();
                                        int j = b;
                                        while (true) {
                                            int j2 = j;
                                            if (j2 >= uids.size()) {
                                                break;
                                            }
                                            rec.eventCode = i2;
                                            Entry<String, SparseIntArray> ent = lastTime;
                                            rec.eventTag.string = (String) lastTime.getKey();
                                            rec.eventTag.uid = uids.keyAt(j2);
                                            rec.eventTag.poolIdx = uids.valueAt(j2);
                                            SparseIntArray uids2 = uids;
                                            Entry<String, SparseIntArray> ent2 = ent;
                                            it2 = it;
                                            tracker3 = tracker5;
                                            active2 = active;
                                            i3 = i2;
                                            oldEventTag = oldEventTag2;
                                            hprinter.printNextItem(printWriter, rec, baseTime2, checkin, (flags & 32) != null ? 1 : null);
                                            rec.wakeReasonTag = null;
                                            rec.wakelockTag = null;
                                            i = j2 + 1;
                                            oldEventTag2 = oldEventTag;
                                            lastTime = ent2;
                                            it = it2;
                                            tracker5 = tracker3;
                                            active = active2;
                                            i2 = i3;
                                            SparseIntArray sparseIntArray = uids2;
                                            j = i;
                                            uids = sparseIntArray;
                                        }
                                        it2 = it;
                                        tracker3 = tracker5;
                                        active2 = active;
                                        i3 = i2;
                                        oldEventTag = oldEventTag2;
                                        b = (byte) 0;
                                    }
                                }
                                i = i2 + 1;
                                oldEventTag2 = oldEventTag2;
                                tracker4 = tracker5;
                                b = (byte) 0;
                            }
                            oldEventTag = oldEventTag2;
                            tracker3 = tracker4;
                            rec.eventCode = tracker2;
                            rec.eventTag = oldEventTag;
                            tracker3 = null;
                        } else {
                            tracker3 = tracker4;
                        }
                    }
                    printed2 = true;
                    b = (byte) 0;
                    hprinter.printNextItem(printWriter, rec, baseTime2, checkin, (flags & 32) != 0);
                    rec.cmd = b;
                    if (tracker4 == null) {
                    }
                }
                baseTime = baseTime2;
                hprinter.printNextItem(printWriter, rec, baseTime, checkin, (flags & 32) != 0);
                lastTime = lastTime2;
                printed = printed2;
                tracker = tracker3;
            } else {
                baseTime = baseTime2;
                tracker = tracker2;
            }
        }
        if (histStart >= 0) {
            commitCurrentHistoryBatchLocked();
            printWriter.print(checkin ? "NEXT: " : "  NEXT: ");
            printWriter.println(1 + lastTime);
        }
    }

    private void dumpDailyLevelStepSummary(PrintWriter pw, String prefix, String label, LevelStepTracker steps, StringBuilder tmpSb, int[] tmpOutInt) {
        PrintWriter printWriter = pw;
        String str = label;
        StringBuilder stringBuilder = tmpSb;
        if (steps != null) {
            long timeRemaining = steps.computeTimeEstimate(0, 0, tmpOutInt);
            if (timeRemaining >= 0) {
                pw.print(prefix);
                printWriter.print(str);
                printWriter.print(" total time: ");
                stringBuilder.setLength(0);
                formatTimeMs(stringBuilder, timeRemaining);
                printWriter.print(stringBuilder);
                printWriter.print(" (from ");
                printWriter.print(tmpOutInt[0]);
                printWriter.println(" steps)");
            }
            int i = 0;
            while (true) {
                int i2 = i;
                if (i2 < STEP_LEVEL_MODES_OF_INTEREST.length) {
                    int i3 = i2;
                    long estimatedTime = steps.computeTimeEstimate((long) STEP_LEVEL_MODES_OF_INTEREST[i2], (long) STEP_LEVEL_MODE_VALUES[i2], tmpOutInt);
                    if (estimatedTime > 0) {
                        pw.print(prefix);
                        printWriter.print(str);
                        printWriter.print(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
                        printWriter.print(STEP_LEVEL_MODE_LABELS[i3]);
                        printWriter.print(" time: ");
                        stringBuilder.setLength(0);
                        formatTimeMs(stringBuilder, estimatedTime);
                        printWriter.print(stringBuilder);
                        printWriter.print(" (from ");
                        printWriter.print(tmpOutInt[0]);
                        printWriter.println(" steps)");
                    }
                    i = i3 + 1;
                } else {
                    return;
                }
            }
        }
    }

    private void dumpDailyPackageChanges(PrintWriter pw, String prefix, ArrayList<PackageChange> changes) {
        if (changes != null) {
            pw.print(prefix);
            pw.println("Package changes:");
            for (int i = 0; i < changes.size(); i++) {
                PackageChange pc = (PackageChange) changes.get(i);
                if (pc.mUpdate) {
                    pw.print(prefix);
                    pw.print("  Update ");
                    pw.print(pc.mPackageName);
                    pw.print(" vers=");
                    pw.println(pc.mVersionCode);
                } else {
                    pw.print(prefix);
                    pw.print("  Uninstall ");
                    pw.println(pc.mPackageName);
                }
            }
        }
    }

    public void dumpLocked(Context context, PrintWriter pw, int flags, int reqUid, long histStart) {
        Throwable th;
        PrintWriter printWriter = pw;
        prepareForDumpLocked();
        boolean filtering = (flags & 14) != 0;
        if (!((flags & 8) == 0 && filtering)) {
            long historyTotalSize = (long) getHistoryTotalSize();
            long historyUsedSize = (long) getHistoryUsedSize();
            if (startIteratingHistoryLocked()) {
                try {
                    printWriter.print("Battery History (");
                    printWriter.print((100 * historyUsedSize) / historyTotalSize);
                    printWriter.print("% used, ");
                    printSizeValue(printWriter, historyUsedSize);
                    printWriter.print(" used of ");
                    printSizeValue(printWriter, historyTotalSize);
                    printWriter.print(", ");
                    printWriter.print(getHistoryStringPoolSize());
                    printWriter.print(" strings using ");
                    printSizeValue(printWriter, (long) getHistoryStringPoolBytes());
                    printWriter.println("):");
                    try {
                        dumpHistoryLocked(printWriter, flags, histStart, null);
                        pw.println();
                        finishIteratingHistoryLocked();
                    } catch (Throwable th2) {
                        th = th2;
                        finishIteratingHistoryLocked();
                        throw th;
                    }
                } catch (Throwable th3) {
                    th = th3;
                    long j = historyUsedSize;
                    finishIteratingHistoryLocked();
                    throw th;
                }
            }
            if (startIteratingOldHistoryLocked()) {
                try {
                    HistoryItem rec = new HistoryItem();
                    printWriter.println("Old battery History:");
                    HistoryPrinter hprinter = new HistoryPrinter();
                    long baseTime = -1;
                    while (getNextOldHistoryLocked(rec)) {
                        if (baseTime < 0) {
                            baseTime = rec.time;
                        }
                        long baseTime2 = baseTime;
                        hprinter.printNextItem(printWriter, rec, baseTime2, false, (flags & 32) != 0);
                        baseTime = baseTime2;
                    }
                    pw.println();
                } finally {
                    finishIteratingOldHistoryLocked();
                }
            }
        }
        if (!filtering || (flags & 6) != 0) {
            int NU;
            boolean z;
            boolean z2;
            if (!filtering) {
                SparseArray<? extends Uid> uidStats = getUidStats();
                NU = uidStats.size();
                long nowRealtime = SystemClock.elapsedRealtime();
                boolean didPid = false;
                for (int i = 0; i < NU; i++) {
                    SparseArray<? extends Pid> pids = ((Uid) uidStats.valueAt(i)).getPidStats();
                    if (pids != null) {
                        boolean didPid2 = didPid;
                        for (int j2 = 0; j2 < pids.size(); j2++) {
                            Pid pid = (Pid) pids.valueAt(j2);
                            if (!didPid2) {
                                printWriter.println("Per-PID Stats:");
                                didPid2 = true;
                            }
                            long time = pid.mWakeSumMs + (pid.mWakeNesting > 0 ? nowRealtime - pid.mWakeStartMs : 0);
                            printWriter.print("  PID ");
                            printWriter.print(pids.keyAt(j2));
                            printWriter.print(" wake time: ");
                            TimeUtils.formatDuration(time, printWriter);
                            printWriter.println("");
                        }
                        didPid = didPid2;
                    }
                }
                if (didPid) {
                    pw.println();
                }
            }
            if (filtering && (flags & 2) == 0) {
                z = false;
            } else {
                if (dumpDurationSteps(printWriter, "  ", "Discharge step durations:", getDischargeLevelStepTracker(), false)) {
                    long timeRemaining = computeBatteryTimeRemaining(SystemClock.elapsedRealtime() * 1000);
                    if (timeRemaining >= 0) {
                        printWriter.print("  Estimated discharge time remaining: ");
                        TimeUtils.formatDuration(timeRemaining / 1000, printWriter);
                        pw.println();
                    }
                    LevelStepTracker steps = getDischargeLevelStepTracker();
                    NU = 0;
                    while (true) {
                        int i2 = NU;
                        if (i2 >= STEP_LEVEL_MODES_OF_INTEREST.length) {
                            break;
                        }
                        dumpTimeEstimate(printWriter, "  Estimated ", STEP_LEVEL_MODE_LABELS[i2], " time: ", steps.computeTimeEstimate((long) STEP_LEVEL_MODES_OF_INTEREST[i2], (long) STEP_LEVEL_MODE_VALUES[i2], null));
                        NU = i2 + 1;
                    }
                    pw.println();
                }
                z = false;
                if (dumpDurationSteps(printWriter, "  ", "Charge step durations:", getChargeLevelStepTracker(), false)) {
                    long timeRemaining2 = computeChargeTimeRemaining(SystemClock.elapsedRealtime() * 1000);
                    if (timeRemaining2 >= 0) {
                        printWriter.print("  Estimated charge time remaining: ");
                        TimeUtils.formatDuration(timeRemaining2 / 1000, printWriter);
                        pw.println();
                    }
                    pw.println();
                }
            }
            if (filtering && (flags & 4) == 0) {
                z2 = z;
            } else {
                int[] outInt;
                DailyItem dit;
                DailyItem dit2;
                printWriter.println("Daily stats:");
                printWriter.print("  Current start time: ");
                printWriter.println(DateFormat.format((CharSequence) "yyyy-MM-dd-HH-mm-ss", getCurrentDailyStartTime()).toString());
                printWriter.print("  Next min deadline: ");
                printWriter.println(DateFormat.format((CharSequence) "yyyy-MM-dd-HH-mm-ss", getNextMinDailyDeadline()).toString());
                printWriter.print("  Next max deadline: ");
                printWriter.println(DateFormat.format((CharSequence) "yyyy-MM-dd-HH-mm-ss", getNextMaxDailyDeadline()).toString());
                StringBuilder sb = new StringBuilder(64);
                int[] outInt2 = new int[1];
                LevelStepTracker dsteps = getDailyDischargeLevelStepTracker();
                LevelStepTracker csteps = getDailyChargeLevelStepTracker();
                ArrayList<PackageChange> pkgc = getDailyPackageChanges();
                int[] iArr;
                ArrayList<PackageChange> pkgc2;
                LevelStepTracker csteps2;
                LevelStepTracker dsteps2;
                if (dsteps.mNumStepDurations > 0 || csteps.mNumStepDurations > 0 || pkgc != null) {
                    if ((flags & 4) != 0) {
                        iArr = 1;
                        pkgc2 = pkgc;
                        csteps2 = csteps;
                        dsteps2 = dsteps;
                        z2 = z;
                        outInt = outInt2;
                    } else if (filtering) {
                        printWriter.println("  Current daily steps:");
                        dumpDailyLevelStepSummary(printWriter, "    ", "Discharge", dsteps, sb, outInt2);
                        z2 = z;
                        iArr = 1;
                        outInt = outInt2;
                        dumpDailyLevelStepSummary(printWriter, "    ", "Charge", csteps, sb, outInt2);
                    } else {
                        iArr = 1;
                        pkgc2 = pkgc;
                        csteps2 = csteps;
                        dsteps2 = dsteps;
                        z2 = z;
                        outInt = outInt2;
                    }
                    if (dumpDurationSteps(printWriter, "    ", "  Current daily discharge step durations:", dsteps2, z2)) {
                        dumpDailyLevelStepSummary(printWriter, "      ", "Discharge", dsteps2, sb, outInt);
                    }
                    if (dumpDurationSteps(printWriter, "    ", "  Current daily charge step durations:", csteps2, z2)) {
                        dumpDailyLevelStepSummary(printWriter, "      ", "Charge", csteps2, sb, outInt);
                    }
                    dumpDailyPackageChanges(printWriter, "    ", pkgc2);
                } else {
                    iArr = 1;
                    pkgc2 = pkgc;
                    csteps2 = csteps;
                    dsteps2 = dsteps;
                    z2 = z;
                    outInt = outInt2;
                }
                int curIndex = z2;
                while (true) {
                    DailyItem dailyItemLocked = getDailyItemLocked(curIndex);
                    dit = dailyItemLocked;
                    if (dailyItemLocked == null) {
                        break;
                    }
                    int curIndex2 = curIndex + 1;
                    if ((flags & 4) != 0) {
                        pw.println();
                    }
                    printWriter.print("  Daily from ");
                    printWriter.print(DateFormat.format((CharSequence) "yyyy-MM-dd-HH-mm-ss", dit.mStartTime).toString());
                    printWriter.print(" to ");
                    printWriter.print(DateFormat.format((CharSequence) "yyyy-MM-dd-HH-mm-ss", dit.mEndTime).toString());
                    printWriter.println(SettingsStringUtil.DELIMITER);
                    if ((flags & 4) != 0) {
                        dit2 = dit;
                    } else if (filtering) {
                        dsteps = dit.mDischargeSteps;
                        PrintWriter printWriter2 = printWriter;
                        StringBuilder stringBuilder = sb;
                        dit2 = dit;
                        outInt2 = outInt;
                        dumpDailyLevelStepSummary(printWriter2, "    ", "Discharge", dsteps, stringBuilder, outInt2);
                        dumpDailyLevelStepSummary(printWriter2, "    ", "Charge", dit2.mChargeSteps, stringBuilder, outInt2);
                        curIndex = curIndex2;
                    } else {
                        dit2 = dit;
                    }
                    if (dumpDurationSteps(printWriter, "      ", "    Discharge step durations:", dit2.mDischargeSteps, z2)) {
                        dumpDailyLevelStepSummary(printWriter, "        ", "Discharge", dit2.mDischargeSteps, sb, outInt);
                    }
                    if (dumpDurationSteps(printWriter, "      ", "    Charge step durations:", dit2.mChargeSteps, z2)) {
                        dumpDailyLevelStepSummary(printWriter, "        ", "Charge", dit2.mChargeSteps, sb, outInt);
                    }
                    dumpDailyPackageChanges(printWriter, "    ", dit2.mPackageChanges);
                    curIndex = curIndex2;
                }
                dit2 = dit;
                pw.println();
            }
            if (!(filtering && (flags & 2) == 0)) {
                printWriter.println("Statistics since last charge:");
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  System starts: ");
                stringBuilder2.append(getStartCount());
                stringBuilder2.append(", currently on battery: ");
                stringBuilder2.append(getIsOnBattery());
                printWriter.println(stringBuilder2.toString());
                dumpLocked(context, printWriter, "", 0, reqUid, (flags & 64) != 0 ? true : z2);
                pw.println();
            }
        }
    }

    public void dumpCheckinLocked(Context context, PrintWriter pw, List<ApplicationInfo> apps, int flags, long histStart) {
        PrintWriter printWriter = pw;
        List<ApplicationInfo> list = apps;
        prepareForDumpLocked();
        String str = VERSION_DATA;
        r2 = new Object[4];
        boolean z = false;
        r2[0] = Integer.valueOf(32);
        boolean z2 = true;
        r2[1] = Integer.valueOf(getParcelVersion());
        r2[2] = getStartPlatformVersion();
        r2[3] = getEndPlatformVersion();
        dumpLine(printWriter, 0, "i", str, r2);
        long now = getHistoryBaseTime() + SystemClock.elapsedRealtime();
        if ((flags & 24) != 0 && startIteratingHistoryLocked()) {
            int i = 0;
            while (i < getHistoryStringPoolSize()) {
                try {
                    printWriter.print(9);
                    printWriter.print(',');
                    printWriter.print(HISTORY_STRING_POOL);
                    printWriter.print(',');
                    printWriter.print(i);
                    printWriter.print(",");
                    printWriter.print(getHistoryTagPoolUid(i));
                    printWriter.print(",\"");
                    printWriter.print(getHistoryTagPoolString(i).replace("\\", "\\\\").replace("\"", "\\\""));
                    printWriter.print("\"");
                    pw.println();
                    i++;
                } finally {
                    finishIteratingHistoryLocked();
                }
            }
            dumpHistoryLocked(printWriter, flags, histStart, true);
        }
        if ((flags & 8) == 0) {
            if (list != null) {
                SparseArray<Pair<ArrayList<String>, MutableBoolean>> uids = new SparseArray();
                for (int i2 = 0; i2 < apps.size(); i2++) {
                    ApplicationInfo ai = (ApplicationInfo) list.get(i2);
                    Pair<ArrayList<String>, MutableBoolean> pkgs = (Pair) uids.get(UserHandle.getAppId(ai.uid));
                    if (pkgs == null) {
                        pkgs = new Pair(new ArrayList(), new MutableBoolean(false));
                        uids.put(UserHandle.getAppId(ai.uid), pkgs);
                    }
                    ((ArrayList) pkgs.first).add(ai.packageName);
                }
                SparseArray<? extends Uid> uidStats = getUidStats();
                int NU = uidStats.size();
                String[] lineArgs = new String[2];
                int i3 = 0;
                while (i3 < NU) {
                    int uid = UserHandle.getAppId(uidStats.keyAt(i3));
                    Pair<ArrayList<String>, MutableBoolean> pkgs2 = (Pair) uids.get(uid);
                    if (!(pkgs2 == null || ((MutableBoolean) pkgs2.second).value)) {
                        ((MutableBoolean) pkgs2.second).value = z2;
                        int j = 0;
                        while (j < ((ArrayList) pkgs2.first).size()) {
                            lineArgs[0] = Integer.toString(uid);
                            lineArgs[1] = (String) ((ArrayList) pkgs2.first).get(j);
                            SparseArray<Pair<ArrayList<String>, MutableBoolean>> uids2 = uids;
                            SparseArray<? extends Uid> uidStats2 = uidStats;
                            dumpLine(printWriter, 0, "i", "uid", (Object[]) lineArgs);
                            j++;
                            uids = uids2;
                            uidStats = uidStats2;
                        }
                    }
                    i3++;
                    uids = uids;
                    uidStats = uidStats;
                    z2 = true;
                }
            }
            if ((flags & 4) == 0) {
                dumpDurationSteps(printWriter, "", DISCHARGE_STEP_DATA, getDischargeLevelStepTracker(), true);
                String[] lineArgs2 = new String[1];
                long timeRemaining = computeBatteryTimeRemaining(SystemClock.elapsedRealtime() * 1000);
                if (timeRemaining >= 0) {
                    lineArgs2[0] = Long.toString(timeRemaining);
                    dumpLine(printWriter, 0, "i", DISCHARGE_TIME_REMAIN_DATA, (Object[]) lineArgs2);
                }
                dumpDurationSteps(printWriter, "", CHARGE_STEP_DATA, getChargeLevelStepTracker(), true);
                long timeRemaining2 = computeChargeTimeRemaining(SystemClock.elapsedRealtime() * 1000);
                if (timeRemaining2 >= 0) {
                    lineArgs2[0] = Long.toString(timeRemaining2);
                    dumpLine(printWriter, 0, "i", CHARGE_TIME_REMAIN_DATA, (Object[]) lineArgs2);
                }
                if ((flags & 64) != 0) {
                    z = true;
                }
                dumpCheckinLocked(context, printWriter, 0, -1, z);
            }
        }
    }

    public void dumpProtoLocked(Context context, FileDescriptor fd, List<ApplicationInfo> apps, int flags, long histStart) {
        ProtoOutputStream proto = new ProtoOutputStream(fd);
        prepareForDumpLocked();
        if ((flags & 24) != 0) {
            dumpProtoHistoryLocked(proto, flags, histStart);
            proto.flush();
            return;
        }
        long bToken = proto.start(1146756268033L);
        proto.write(1120986464257L, 32);
        proto.write(1112396529666L, getParcelVersion());
        proto.write(1138166333443L, getStartPlatformVersion());
        proto.write(1138166333444L, getEndPlatformVersion());
        if ((flags & 4) == 0) {
            BatteryStatsHelper helper = new BatteryStatsHelper(context, false, (flags & 64) != 0);
            helper.create(this);
            helper.refreshStats(0, -1);
            dumpProtoAppsLocked(proto, helper, apps);
            dumpProtoSystemLocked(proto, helper);
        }
        proto.end(bToken);
        proto.flush();
    }

    private void dumpProtoAppsLocked(ProtoOutputStream proto, BatteryStatsHelper helper, List<ApplicationInfo> apps) {
        int i;
        int aid;
        ArrayList<String> pkgs;
        List<BatterySipper> sippers;
        int which;
        ProtoOutputStream protoOutputStream = proto;
        List list = apps;
        int which2 = 0;
        long rawUptimeUs = SystemClock.uptimeMillis() * 1000;
        long rawRealtimeMs = SystemClock.elapsedRealtime();
        long rawRealtimeUs = rawRealtimeMs * 1000;
        long batteryUptimeUs = getBatteryUptime(rawUptimeUs);
        SparseArray<ArrayList<String>> aidToPackages = new SparseArray();
        if (list != null) {
            i = 0;
            while (i < apps.size()) {
                ArrayList<String> pkgs2;
                ApplicationInfo ai = (ApplicationInfo) list.get(i);
                aid = UserHandle.getAppId(ai.uid);
                pkgs = (ArrayList) aidToPackages.get(aid);
                if (pkgs == null) {
                    pkgs2 = new ArrayList();
                    aidToPackages.put(aid, pkgs2);
                } else {
                    pkgs2 = pkgs;
                }
                pkgs2.add(ai.packageName);
                i++;
                List<ApplicationInfo> list2 = apps;
            }
        }
        SparseArray<BatterySipper> uidToSipper = new SparseArray();
        List<BatterySipper> sippers2 = helper.getUsageList();
        if (sippers2 != null) {
            i = 0;
            while (i < sippers2.size()) {
                BatterySipper bs = (BatterySipper) sippers2.get(i);
                sippers = sippers2;
                which = which2;
                if (bs.drainType == DrainType.APP) {
                    uidToSipper.put(bs.uidObj.getUid(), bs);
                }
                i++;
                sippers2 = sippers;
                which2 = which;
            }
        }
        sippers = sippers2;
        which = which2;
        SparseArray<? extends Uid> uidStats = getUidStats();
        aid = uidStats.size();
        i = 0;
        while (true) {
            int iu = i;
            long uTkn;
            SparseArray<? extends Uid> uidStats2;
            long rawUptimeUs2;
            SparseArray<ArrayList<String>> aidToPackages2;
            List<BatterySipper> sippers3;
            long rawRealtimeMs2;
            SparseArray<BatterySipper> uidToSipper2;
            long rawRealtimeUs2;
            if (iu < aid) {
                ArrayList<String> pkgs3;
                ArrayList<String> pkgs4;
                String pkg;
                ArrayMap<String, ? extends Pkg> packageStats;
                SparseArray<ArrayList<String>> aidToPackages3;
                long batteryUptimeUs2;
                Uid u;
                SparseArray<BatterySipper> uidToSipper3;
                long rawRealtimeMs3;
                long rawRealtimeUs3;
                int starts;
                int launches;
                long sToken;
                long awToken;
                ProtoOutputStream protoOutputStream2;
                ArrayMap<String, ? extends Pkg> packageStats2;
                long cToken;
                int n;
                long uTkn2;
                long jcToken;
                int length;
                ArrayMap<String, SparseIntArray> completions;
                int ij;
                Timer bgTimer;
                ArrayMap<String, ? extends Proc> processStats;
                int uid;
                int uid2;
                ArrayMap<String, ? extends Timer> jobs;
                int n2 = aid;
                Uid u2 = (Uid) uidStats.valueAt(iu);
                uTkn = protoOutputStream.start(2246267895813L);
                aid = uidStats.keyAt(iu);
                int iu2 = iu;
                SparseArray<ArrayList<String>> aidToPackages4 = aidToPackages;
                protoOutputStream.write((long) 1, aid);
                aidToPackages = aidToPackages4;
                ArrayList<String> pkgs5 = (ArrayList) aidToPackages.get(UserHandle.getAppId(aid));
                if (pkgs5 == null) {
                    pkgs3 = new ArrayList();
                } else {
                    pkgs3 = pkgs5;
                }
                ArrayMap<String, ? extends Pkg> packageStats3 = u2.getPackageStats();
                int uid3 = aid;
                int ipkg = packageStats3.size() - 1;
                while (true) {
                    pkgs4 = pkgs3;
                    aid = ipkg;
                    if (aid < 0) {
                        break;
                    }
                    ArrayList<String> pkgs6;
                    pkg = (String) packageStats3.keyAt(aid);
                    packageStats = packageStats3;
                    packageStats3 = ((Pkg) packageStats3.valueAt(aid)).getServiceStats();
                    if (packageStats3.size() == 0) {
                        aidToPackages3 = aidToPackages;
                        batteryUptimeUs2 = batteryUptimeUs;
                        u = u2;
                        uidToSipper3 = uidToSipper;
                        uidStats2 = uidStats;
                        rawUptimeUs2 = rawUptimeUs;
                        rawRealtimeMs3 = rawRealtimeMs;
                        rawRealtimeUs3 = rawRealtimeUs;
                        pkgs6 = pkgs4;
                    } else {
                        uidStats2 = uidStats;
                        rawUptimeUs2 = rawUptimeUs;
                        rawRealtimeUs3 = rawRealtimeUs;
                        rawRealtimeUs = protoOutputStream.start(2);
                        protoOutputStream.write(1138166333441L, pkg);
                        ArrayList<String> pkgs7 = pkgs4;
                        pkgs7.remove(pkg);
                        int isvc = packageStats3.size() - 1;
                        while (isvc >= 0) {
                            Serv ss = (Serv) packageStats3.valueAt(isvc);
                            String pkg2 = pkg;
                            rawRealtimeMs3 = rawRealtimeMs;
                            rawRealtimeMs = roundUsToMs(ss.getStartTime(batteryUptimeUs, 0));
                            aidToPackages3 = aidToPackages;
                            starts = ss.getStarts(0);
                            batteryUptimeUs2 = batteryUptimeUs;
                            launches = ss.getLaunches(0);
                            if (rawRealtimeMs == 0 && starts == 0 && launches == 0) {
                                u = u2;
                                uidToSipper3 = uidToSipper;
                                pkgs6 = pkgs7;
                            } else {
                                int starts2 = starts;
                                u = u2;
                                sToken = protoOutputStream.start(2246267895810L);
                                uidToSipper3 = uidToSipper;
                                pkgs6 = pkgs7;
                                protoOutputStream.write((long) 1, (String) packageStats3.keyAt(isvc));
                                protoOutputStream.write(1112396529666L, rawRealtimeMs);
                                protoOutputStream.write(1120986464259L, starts2);
                                protoOutputStream.write(1120986464260L, launches);
                                protoOutputStream.end(sToken);
                            }
                            isvc--;
                            pkg = pkg2;
                            rawRealtimeMs = rawRealtimeMs3;
                            aidToPackages = aidToPackages3;
                            batteryUptimeUs = batteryUptimeUs2;
                            u2 = u;
                            uidToSipper = uidToSipper3;
                            pkgs7 = pkgs6;
                        }
                        aidToPackages3 = aidToPackages;
                        batteryUptimeUs2 = batteryUptimeUs;
                        u = u2;
                        uidToSipper3 = uidToSipper;
                        pkgs6 = pkgs7;
                        rawRealtimeMs3 = rawRealtimeMs;
                        protoOutputStream.end(rawRealtimeUs);
                    }
                    ipkg = aid - 1;
                    packageStats3 = packageStats;
                    rawUptimeUs = rawUptimeUs2;
                    uidStats = uidStats2;
                    rawRealtimeUs = rawRealtimeUs3;
                    rawRealtimeMs = rawRealtimeMs3;
                    aidToPackages = aidToPackages3;
                    batteryUptimeUs = batteryUptimeUs2;
                    u2 = u;
                    uidToSipper = uidToSipper3;
                    pkgs3 = pkgs6;
                }
                packageStats = packageStats3;
                aidToPackages3 = aidToPackages;
                batteryUptimeUs2 = batteryUptimeUs;
                u = u2;
                uidToSipper3 = uidToSipper;
                uidStats2 = uidStats;
                rawUptimeUs2 = rawUptimeUs;
                rawRealtimeMs3 = rawRealtimeMs;
                rawRealtimeUs3 = rawRealtimeUs;
                aid = pkgs4;
                Iterator it = aid.iterator();
                while (it.hasNext()) {
                    pkg = (String) it.next();
                    sToken = protoOutputStream.start(2246267895810L);
                    protoOutputStream.write(1138166333441L, pkg);
                    protoOutputStream.end(sToken);
                }
                u2 = u;
                if (u2.getAggregatedPartialWakelockTimer() != null) {
                    Timer timer = u2.getAggregatedPartialWakelockTimer();
                    rawRealtimeMs = rawRealtimeMs3;
                    rawRealtimeUs = timer.getTotalDurationMsLocked(rawRealtimeMs);
                    Timer bgTimer2 = timer.getSubTimer();
                    long bgTimeMs = bgTimer2 != null ? bgTimer2.getTotalDurationMsLocked(rawRealtimeMs) : 0;
                    awToken = protoOutputStream.start(1146756268056L);
                    protoOutputStream.write(1112396529665L, rawRealtimeUs);
                    protoOutputStream.write(1112396529666L, bgTimeMs);
                    protoOutputStream.end(awToken);
                } else {
                    rawRealtimeMs = rawRealtimeMs3;
                }
                rawRealtimeUs = uTkn;
                ArrayMap<String, ? extends Pkg> packageStats4 = packageStats;
                pkgs = aid;
                int n3 = n2;
                int iu3 = iu2;
                aidToPackages2 = aidToPackages3;
                int uid4 = uid3;
                uTkn = batteryUptimeUs2;
                sippers3 = sippers;
                Uid u3 = u2;
                dumpTimer(protoOutputStream, 1146756268040L, u2.getAudioTurnedOnTimer(), rawRealtimeUs3, null);
                dumpControllerActivityProto(protoOutputStream, 1146756268035L, u3.getBluetoothControllerActivity(), 0);
                Timer bleTimer = u3.getBluetoothScanTimer();
                if (bleTimer != null) {
                    protoOutputStream2 = protoOutputStream;
                    long bmToken = protoOutputStream.start(1146756268038L);
                    batteryUptimeUs = rawRealtimeUs3;
                    dumpTimer(protoOutputStream2, 1, bleTimer, batteryUptimeUs, 0);
                    dumpTimer(protoOutputStream2, 2, u3.getBluetoothScanBackgroundTimer(), batteryUptimeUs, 0);
                    dumpTimer(protoOutputStream2, 3, u3.getBluetoothUnoptimizedScanTimer(), batteryUptimeUs, 0);
                    dumpTimer(protoOutputStream2, 4, u3.getBluetoothUnoptimizedScanBackgroundTimer(), batteryUptimeUs, 0);
                    if (u3.getBluetoothScanResultCounter() != null) {
                        iu = u3.getBluetoothScanResultCounter().getCountLocked(0);
                    } else {
                        iu = 0;
                    }
                    protoOutputStream.write(1120986464261L, iu);
                    if (u3.getBluetoothScanResultBgCounter() != null) {
                        iu = u3.getBluetoothScanResultBgCounter().getCountLocked(0);
                    } else {
                        iu = 0;
                    }
                    protoOutputStream.write(1120986464262L, iu);
                    protoOutputStream.end(bmToken);
                }
                dumpTimer(protoOutputStream, 9, u3.getCameraTurnedOnTimer(), rawRealtimeUs3, 0);
                batteryUptimeUs = protoOutputStream.start(1146756268039L);
                protoOutputStream.write(1112396529665L, roundUsToMs(u3.getUserCpuTimeUs(0)));
                protoOutputStream.write(1112396529666L, roundUsToMs(u3.getSystemCpuTimeUs(0)));
                long[] cpuFreqs = getCpuFreqs();
                if (cpuFreqs != null) {
                    long[] cpuFreqTimeMs = u3.getCpuFreqTimes(0);
                    if (cpuFreqTimeMs != null && cpuFreqTimeMs.length == cpuFreqs.length) {
                        aid = u3.getScreenOffCpuFreqTimes(0);
                        if (aid == 0) {
                            aid = new long[cpuFreqTimeMs.length];
                        }
                        iu = 0;
                        while (iu < cpuFreqTimeMs.length) {
                            packageStats2 = packageStats4;
                            cToken = protoOutputStream.start(2246267895811L);
                            n = n3;
                            rawRealtimeMs2 = rawRealtimeMs;
                            protoOutputStream.write((long) 1, iu + 1);
                            uTkn2 = rawRealtimeUs;
                            protoOutputStream.write(1112396529666L, cpuFreqTimeMs[iu]);
                            protoOutputStream.write(1112396529667L, aid[iu]);
                            protoOutputStream.end(cToken);
                            iu++;
                            packageStats4 = packageStats2;
                            rawRealtimeMs = rawRealtimeMs2;
                            n3 = n;
                            rawRealtimeUs = uTkn2;
                        }
                    }
                }
                packageStats2 = packageStats4;
                n = n3;
                rawRealtimeMs2 = rawRealtimeMs;
                uTkn2 = rawRealtimeUs;
                i = 0;
                while (true) {
                    cToken = 1159641169921L;
                    if (i >= 7) {
                        break;
                    }
                    long cpuToken;
                    long[] timesMs = u3.getCpuFreqTimes(0, i);
                    if (timesMs == null || timesMs.length != cpuFreqs.length) {
                        cpuToken = batteryUptimeUs;
                    } else {
                        long[] screenOffTimesMs = u3.getScreenOffCpuFreqTimes(0, i);
                        if (screenOffTimesMs == null) {
                            screenOffTimesMs = new long[timesMs.length];
                        }
                        long procToken = protoOutputStream.start(2246267895812L);
                        protoOutputStream.write(1159641169921L, i);
                        aid = 0;
                        while (aid < timesMs.length) {
                            cToken = protoOutputStream.start(2246267895810L);
                            protoOutputStream.write(1120986464257L, aid + 1);
                            cpuToken = batteryUptimeUs;
                            protoOutputStream.write(1112396529666L, timesMs[aid]);
                            protoOutputStream.write(1112396529667L, screenOffTimesMs[aid]);
                            protoOutputStream.end(cToken);
                            aid++;
                            batteryUptimeUs = cpuToken;
                        }
                        cpuToken = batteryUptimeUs;
                        protoOutputStream.end(procToken);
                    }
                    i++;
                    batteryUptimeUs = cpuToken;
                }
                rawRealtimeMs = batteryUptimeUs;
                protoOutputStream.end(rawRealtimeMs);
                protoOutputStream2 = protoOutputStream;
                batteryUptimeUs = rawRealtimeUs3;
                long[] cpuFreqs2 = cpuFreqs;
                dumpTimer(protoOutputStream2, 10, u3.getFlashlightTurnedOnTimer(), batteryUptimeUs, null);
                dumpTimer(protoOutputStream2, 11, u3.getForegroundActivityTimer(), batteryUptimeUs, 0);
                dumpTimer(protoOutputStream2, 12, u3.getForegroundServiceTimer(), batteryUptimeUs, 0);
                ArrayMap<String, SparseIntArray> completions2 = u3.getJobCompletionStats();
                int[] reasons = new int[]{0, 1, 2, 3, 4};
                i = 0;
                while (i < completions2.size()) {
                    long[] cpuFreqs3;
                    SparseIntArray n4 = (SparseIntArray) completions2.valueAt(i);
                    if (n4 != null) {
                        jcToken = protoOutputStream.start(2246267895824L);
                        protoOutputStream.write(1138166333441L, (String) completions2.keyAt(i));
                        length = reasons.length;
                        n3 = 0;
                        while (n3 < length) {
                            int r = reasons[n3];
                            cpuFreqs3 = cpuFreqs2;
                            completions = completions2;
                            cpuFreqs2 = protoOutputStream.start(2246267895810L);
                            protoOutputStream.write(cToken, r);
                            protoOutputStream.write(1120986464258L, n4.get(r, 0));
                            protoOutputStream.end(cpuFreqs2);
                            n3++;
                            cpuFreqs2 = cpuFreqs3;
                            completions2 = completions;
                            cToken = 1159641169921L;
                        }
                        cpuFreqs3 = cpuFreqs2;
                        completions = completions2;
                        protoOutputStream.end(jcToken);
                    } else {
                        cpuFreqs3 = cpuFreqs2;
                        completions = completions2;
                    }
                    i++;
                    cpuFreqs2 = cpuFreqs3;
                    completions2 = completions;
                    cToken = 1159641169921L;
                }
                completions = completions2;
                ArrayMap<String, ? extends Timer> jobs2 = u3.getJobStats();
                i = jobs2.size() - 1;
                while (true) {
                    ij = i;
                    if (ij < 0) {
                        break;
                    }
                    Timer timer2 = (Timer) jobs2.valueAt(ij);
                    bgTimer = timer2.getSubTimer();
                    batteryUptimeUs = protoOutputStream.start(2246267895823L);
                    protoOutputStream.write((long) 1, (String) jobs2.keyAt(ij));
                    protoOutputStream2 = protoOutputStream;
                    cToken = batteryUptimeUs;
                    batteryUptimeUs = rawRealtimeUs3;
                    dumpTimer(protoOutputStream2, 2, timer2, batteryUptimeUs, 0);
                    dumpTimer(protoOutputStream2, 3, bgTimer, batteryUptimeUs, 0);
                    protoOutputStream.end(cToken);
                    i = ij - 1;
                }
                dumpControllerActivityProto(protoOutputStream, 1146756268036L, u3.getModemControllerActivity(), 0);
                cToken = protoOutputStream.start(1146756268049L);
                protoOutputStream.write(1112396529665L, u3.getNetworkActivityBytes(0, 0));
                protoOutputStream.write(1112396529666L, u3.getNetworkActivityBytes(1, 0));
                protoOutputStream.write(1112396529667L, u3.getNetworkActivityBytes(2, 0));
                protoOutputStream.write(1112396529668L, u3.getNetworkActivityBytes(3, 0));
                protoOutputStream.write(1112396529669L, u3.getNetworkActivityBytes(4, 0));
                protoOutputStream.write(1112396529670L, u3.getNetworkActivityBytes(5, 0));
                protoOutputStream.write(1112396529671L, u3.getNetworkActivityPackets(0, 0));
                protoOutputStream.write(1112396529672L, u3.getNetworkActivityPackets(1, 0));
                protoOutputStream.write(1112396529673L, u3.getNetworkActivityPackets(2, 0));
                protoOutputStream.write(1112396529674L, u3.getNetworkActivityPackets(3, 0));
                protoOutputStream.write(1112396529675L, roundUsToMs(u3.getMobileRadioActiveTime(0)));
                protoOutputStream.write(1120986464268L, u3.getMobileRadioActiveCount(0));
                protoOutputStream.write(1120986464269L, u3.getMobileRadioApWakeupCount(0));
                protoOutputStream.write((long) Network.WIFI_WAKEUP_COUNT, u3.getWifiRadioApWakeupCount(0));
                protoOutputStream.write((long) Network.MOBILE_BYTES_BG_RX, u3.getNetworkActivityBytes(6, 0));
                protoOutputStream.write(1112396529680L, u3.getNetworkActivityBytes(7, 0));
                protoOutputStream.write(1112396529681L, u3.getNetworkActivityBytes(8, 0));
                protoOutputStream.write((long) Network.WIFI_BYTES_BG_TX, u3.getNetworkActivityBytes(9, 0));
                protoOutputStream.write(1112396529683L, u3.getNetworkActivityPackets(6, 0));
                protoOutputStream.write(1112396529684L, u3.getNetworkActivityPackets(7, 0));
                protoOutputStream.write(1112396529685L, u3.getNetworkActivityPackets(8, 0));
                protoOutputStream.write((long) Network.WIFI_PACKETS_BG_TX, u3.getNetworkActivityPackets(9, 0));
                protoOutputStream.end(cToken);
                SparseArray<BatterySipper> uidToSipper4 = uidToSipper3;
                length = uid4;
                BatterySipper bs2 = (BatterySipper) uidToSipper4.get(length);
                if (bs2 != null) {
                    long bsToken = protoOutputStream.start(1146756268050L);
                    uidToSipper2 = uidToSipper4;
                    protoOutputStream.write(1103806595073L, bs2.totalPowerMah);
                    protoOutputStream.write(1133871366146L, bs2.shouldHide);
                    protoOutputStream.write(1103806595075L, bs2.screenPowerMah);
                    protoOutputStream.write(1103806595076L, bs2.proportionalSmearMah);
                    protoOutputStream.end(bsToken);
                } else {
                    uidToSipper2 = uidToSipper4;
                }
                ArrayMap<String, ? extends Proc> processStats2 = u3.getProcessStats();
                i = processStats2.size() - 1;
                while (i >= 0) {
                    Proc n5 = (Proc) processStats2.valueAt(i);
                    jcToken = protoOutputStream.start(2246267895827L);
                    processStats = processStats2;
                    protoOutputStream.write(1138166333441L, (String) processStats2.keyAt(i));
                    uid = length;
                    protoOutputStream.write(1112396529666L, n5.getUserTime(0));
                    protoOutputStream.write(1112396529667L, n5.getSystemTime(0));
                    protoOutputStream.write(1112396529668L, n5.getForegroundTime(0));
                    protoOutputStream.write(1120986464261L, n5.getStarts(0));
                    protoOutputStream.write(1120986464262L, n5.getNumAnrs(0));
                    protoOutputStream.write(1120986464263L, n5.getNumCrashes(0));
                    protoOutputStream.end(jcToken);
                    i--;
                    processStats2 = processStats;
                    length = uid;
                }
                uid = length;
                processStats = processStats2;
                SparseArray<? extends Sensor> sensors = u3.getSensorStats();
                i = 0;
                while (true) {
                    ij = i;
                    if (ij >= sensors.size()) {
                        break;
                    }
                    BatterySipper bs3;
                    Sensor se = (Sensor) sensors.valueAt(ij);
                    bgTimer = se.getSensorTime();
                    if (bgTimer == null) {
                        bs3 = bs2;
                        uid2 = uid;
                    } else {
                        Timer bgTimer3 = se.getSensorBackgroundTime();
                        length = sensors.keyAt(ij);
                        long seToken = protoOutputStream.start(UidProto.SENSORS);
                        protoOutputStream.write(1120986464257L, length);
                        protoOutputStream2 = protoOutputStream;
                        long j = 1;
                        long seToken2 = seToken;
                        bs3 = bs2;
                        batteryUptimeUs = rawRealtimeUs3;
                        uid2 = uid;
                        dumpTimer(protoOutputStream2, 1146756268034L, bgTimer, batteryUptimeUs, 0);
                        dumpTimer(protoOutputStream2, 3, bgTimer3, batteryUptimeUs, 0);
                        protoOutputStream.end(seToken2);
                    }
                    i = ij + 1;
                    bs2 = bs3;
                    uid = uid2;
                }
                uid2 = uid;
                i = 0;
                while (i < 7) {
                    long nToken;
                    rawRealtimeUs2 = rawRealtimeUs3;
                    jcToken = roundUsToMs(u3.getProcessStateTime(i, rawRealtimeUs2, 0));
                    if (jcToken == 0) {
                        nToken = cToken;
                    } else {
                        batteryUptimeUs = protoOutputStream.start(2246267895828L);
                        long durMs = jcToken;
                        protoOutputStream.write((long) 1, i);
                        nToken = cToken;
                        protoOutputStream.write(1112396529666L, durMs);
                        protoOutputStream.end(batteryUptimeUs);
                    }
                    i++;
                    rawRealtimeUs3 = rawRealtimeUs2;
                    cToken = nToken;
                }
                rawRealtimeUs2 = rawRealtimeUs3;
                ArrayMap<String, ? extends Timer> syncs = u3.getSyncStats();
                i = syncs.size() - 1;
                while (true) {
                    which2 = i;
                    if (which2 < 0) {
                        break;
                    }
                    Timer timer3 = (Timer) syncs.valueAt(which2);
                    bgTimer = timer3.getSubTimer();
                    batteryUptimeUs = protoOutputStream.start(2246267895830L);
                    protoOutputStream.write((long) 1, (String) syncs.keyAt(which2));
                    protoOutputStream2 = protoOutputStream;
                    long syToken = batteryUptimeUs;
                    batteryUptimeUs = rawRealtimeUs2;
                    dumpTimer(protoOutputStream2, 2, timer3, batteryUptimeUs, null);
                    dumpTimer(protoOutputStream2, 3, bgTimer, batteryUptimeUs, 0);
                    protoOutputStream.end(syToken);
                    i = which2 - 1;
                }
                if (u3.hasUserActivity()) {
                    i = 0;
                    while (i < 4) {
                        ArrayMap<String, ? extends Timer> syncs2;
                        iu = u3.getUserActivityCount(i, 0);
                        if (iu != 0) {
                            awToken = protoOutputStream.start(UidProto.USER_ACTIVITY);
                            protoOutputStream.write(1159641169921L, i);
                            syncs2 = syncs;
                            protoOutputStream.write((long) 2, iu);
                            protoOutputStream.end(awToken);
                        } else {
                            syncs2 = syncs;
                        }
                        i++;
                        syncs = syncs2;
                    }
                }
                protoOutputStream2 = protoOutputStream;
                batteryUptimeUs = rawRealtimeUs2;
                dumpTimer(protoOutputStream2, 13, u3.getVibratorOnTimer(), batteryUptimeUs, 0);
                dumpTimer(protoOutputStream2, 14, u3.getVideoTurnedOnTimer(), batteryUptimeUs, 0);
                ArrayMap<String, ? extends Wakelock> wakelocks = u3.getWakelockStats();
                i = wakelocks.size() - 1;
                while (true) {
                    launches = i;
                    if (launches < 0) {
                        break;
                    }
                    Wakelock wl = (Wakelock) wakelocks.valueAt(launches);
                    aid = protoOutputStream.start(2246267895833L);
                    protoOutputStream.write(1138166333441L, (String) wakelocks.keyAt(launches));
                    long wToken = aid;
                    int iw = launches;
                    Wakelock wl2 = wl;
                    ArrayMap<String, ? extends Wakelock> wakelocks2 = wakelocks;
                    dumpTimer(protoOutputStream, 1146756268034L, wl.getWakeTime(1), rawRealtimeUs2, null);
                    wakelocks = wl2.getWakeTime(0);
                    if (wakelocks != null) {
                        protoOutputStream2 = protoOutputStream;
                        batteryUptimeUs = rawRealtimeUs2;
                        jobs = jobs2;
                        jobs2 = wakelocks;
                        dumpTimer(protoOutputStream2, 3, wakelocks, batteryUptimeUs, null);
                        dumpTimer(protoOutputStream2, 4, jobs2.getSubTimer(), batteryUptimeUs, null);
                    } else {
                        jobs = jobs2;
                        ArrayMap<String, ? extends Wakelock> pTimer = wakelocks;
                    }
                    uid3 = 2;
                    dumpTimer(protoOutputStream, 5, wl2.getWakeTime(2), rawRealtimeUs2, null);
                    protoOutputStream.end(wToken);
                    i = iw - 1;
                    wakelocks = wakelocks2;
                    jobs2 = jobs;
                }
                jobs = jobs2;
                dumpTimer(protoOutputStream, 28, u3.getMulticastWakelockStats(), rawRealtimeUs2, 0);
                ArrayMap<String, ? extends Pkg> packageStats5 = packageStats2;
                for (i = packageStats5.size() - 1; i >= 0; i--) {
                    ArrayMap<String, ? extends Counter> alarms = ((Pkg) packageStats5.valueAt(i)).getWakeupAlarmStats();
                    for (starts = alarms.size() - 1; starts >= 0; starts--) {
                        long waToken = protoOutputStream.start(2246267895834L);
                        protoOutputStream.write(1138166333441L, (String) alarms.keyAt(starts));
                        protoOutputStream.write(1120986464258L, ((Counter) alarms.valueAt(starts)).getCountLocked(0));
                        protoOutputStream.end(waToken);
                    }
                }
                dumpControllerActivityProto(protoOutputStream, 1146756268037L, u3.getWifiControllerActivity(), 0);
                batteryUptimeUs = protoOutputStream.start(1146756268059L);
                protoOutputStream.write(1112396529665L, roundUsToMs(u3.getFullWifiLockTime(rawRealtimeUs2, 0)));
                long wToken2 = batteryUptimeUs;
                batteryUptimeUs = rawRealtimeUs2;
                dumpTimer(protoOutputStream, 3, u3.getWifiScanTimer(), batteryUptimeUs, 0);
                protoOutputStream.write(1112396529666L, roundUsToMs(u3.getWifiRunningTime(rawRealtimeUs2, 0)));
                dumpTimer(protoOutputStream, 4, u3.getWifiScanBackgroundTimer(), batteryUptimeUs, 0);
                protoOutputStream.end(wToken2);
                protoOutputStream.end(uTkn2);
                i = iu3 + 1;
                rawRealtimeUs = rawRealtimeUs2;
                aidToPackages = aidToPackages2;
                batteryUptimeUs = uTkn;
                rawUptimeUs = rawUptimeUs2;
                uidStats = uidStats2;
                sippers = sippers3;
                rawRealtimeMs = rawRealtimeMs2;
                aid = n;
                uidToSipper = uidToSipper2;
            } else {
                aidToPackages2 = aidToPackages;
                uTkn = batteryUptimeUs;
                uidToSipper2 = uidToSipper;
                uidStats2 = uidStats;
                rawUptimeUs2 = rawUptimeUs;
                rawRealtimeMs2 = rawRealtimeMs;
                rawRealtimeUs2 = rawRealtimeUs;
                sippers3 = sippers;
                return;
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:71:0x01e8 A:{Catch:{ all -> 0x0247 }} */
    /* JADX WARNING: Removed duplicated region for block: B:42:0x00f2 A:{Catch:{ all -> 0x0247 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void dumpProtoHistoryLocked(ProtoOutputStream proto, int flags, long histStart) {
        BatteryStats batteryStats = this;
        ProtoOutputStream protoOutputStream = proto;
        if (startIteratingHistoryLocked()) {
            protoOutputStream.write(1120986464257L, 32);
            protoOutputStream.write(1112396529666L, getParcelVersion());
            protoOutputStream.write(1138166333443L, getStartPlatformVersion());
            protoOutputStream.write(1138166333444L, getEndPlatformVersion());
            int i = 0;
            while (i < getHistoryStringPoolSize()) {
                try {
                    long token = protoOutputStream.start(2246267895813L);
                    protoOutputStream.write(1120986464257L, i);
                    protoOutputStream.write(1120986464258L, batteryStats.getHistoryTagPoolUid(i));
                    protoOutputStream.write(1138166333443L, batteryStats.getHistoryTagPoolString(i));
                    protoOutputStream.end(token);
                    i++;
                } catch (Throwable th) {
                    finishIteratingHistoryLocked();
                }
            }
            HistoryPrinter hprinter = new HistoryPrinter();
            HistoryItem rec = new HistoryItem();
            boolean printed = false;
            long baseTime = -1;
            long lastTime = -1;
            HistoryEventTracker tracker = null;
            while (true) {
                HistoryEventTracker tracker2 = tracker;
                if (!batteryStats.getNextHistoryLocked(rec)) {
                    break;
                }
                long lastTime2 = rec.time;
                if (baseTime < 0) {
                    baseTime = lastTime2;
                }
                if (rec.time >= histStart) {
                    long lastTime3;
                    HistoryEventTracker tracker3;
                    boolean printed2;
                    if (histStart < 0 || printed) {
                        lastTime3 = lastTime2;
                        tracker3 = tracker2;
                        printed2 = printed;
                    } else {
                        boolean printed3;
                        if (!(rec.cmd == (byte) 5 || rec.cmd == (byte) 7 || rec.cmd == (byte) 4)) {
                            if (rec.cmd != (byte) 8) {
                                if (rec.currentTime != 0) {
                                    byte cmd = rec.cmd;
                                    rec.cmd = (byte) 5;
                                    byte cmd2 = cmd;
                                    hprinter.printNextItem(protoOutputStream, rec, baseTime, (flags & 32) != 0);
                                    rec.cmd = cmd2;
                                    lastTime3 = lastTime2;
                                    lastTime2 = tracker2;
                                    printed3 = true;
                                } else {
                                    lastTime3 = lastTime2;
                                    printed3 = printed;
                                    lastTime2 = tracker2;
                                }
                                if (lastTime2 == null) {
                                    byte oldEventTag;
                                    HistoryTag oldEventTag2;
                                    if (rec.cmd != (byte) 0) {
                                        hprinter.printNextItem(protoOutputStream, rec, baseTime, (flags & 32) != 0);
                                        oldEventTag = (byte) 0;
                                        rec.cmd = (byte) 0;
                                    } else {
                                        oldEventTag = (byte) 0;
                                    }
                                    tracker2 = rec.eventCode;
                                    HistoryTag oldEventTag3 = rec.eventTag;
                                    rec.eventTag = new HistoryTag();
                                    boolean i2 = oldEventTag;
                                    while (true) {
                                        printed = i2;
                                        if (printed >= true) {
                                            break;
                                        }
                                        int i3;
                                        int oldEventCode;
                                        HashMap<String, SparseIntArray> active = lastTime2.getStateForEvent(printed);
                                        if (active == null) {
                                            tracker3 = lastTime2;
                                            oldEventTag2 = oldEventTag3;
                                            i3 = printed;
                                            oldEventCode = tracker2;
                                            printed2 = printed3;
                                        } else {
                                            Iterator it = active.entrySet().iterator();
                                            while (it.hasNext()) {
                                                Iterator it2;
                                                HashMap<String, SparseIntArray> active2;
                                                boolean i4;
                                                Entry<String, SparseIntArray> ent = (Entry) it.next();
                                                SparseIntArray uids = (SparseIntArray) ent.getValue();
                                                int j = oldEventTag;
                                                while (true) {
                                                    tracker3 = lastTime2;
                                                    SparseIntArray uids2 = uids;
                                                    printed2 = printed3;
                                                    int j2 = j;
                                                    if (j2 >= uids2.size()) {
                                                        break;
                                                    }
                                                    rec.eventCode = printed;
                                                    Entry<String, SparseIntArray> ent2 = ent;
                                                    rec.eventTag.string = (String) ent.getKey();
                                                    rec.eventTag.uid = uids2.keyAt(j2);
                                                    rec.eventTag.poolIdx = uids2.valueAt(j2);
                                                    Entry<String, SparseIntArray> ent3 = ent2;
                                                    it2 = it;
                                                    active2 = active;
                                                    SparseIntArray uids3 = uids2;
                                                    oldEventTag2 = oldEventTag3;
                                                    i4 = printed;
                                                    oldEventCode = tracker2;
                                                    hprinter.printNextItem(protoOutputStream, rec, baseTime, (boolean) (flags & 32) != null ? 1 : null);
                                                    rec.wakeReasonTag = null;
                                                    rec.wakelockTag = null;
                                                    int j3 = j2 + 1;
                                                    oldEventTag3 = oldEventTag2;
                                                    tracker2 = oldEventCode;
                                                    it = it2;
                                                    active = active2;
                                                    printed = i4;
                                                    lastTime2 = tracker3;
                                                    printed3 = printed2;
                                                    uids = uids3;
                                                    Entry<String, SparseIntArray> entry = ent3;
                                                    j = j3;
                                                    ent = entry;
                                                }
                                                it2 = it;
                                                active2 = active;
                                                oldEventTag2 = oldEventTag3;
                                                i4 = printed;
                                                HistoryEventTracker historyEventTracker = tracker2;
                                                lastTime2 = tracker3;
                                                printed3 = printed2;
                                                oldEventTag = (byte) 0;
                                            }
                                            tracker3 = lastTime2;
                                            oldEventTag2 = oldEventTag3;
                                            i3 = printed;
                                            oldEventCode = tracker2;
                                            printed2 = printed3;
                                        }
                                        i2 = i3 + 1;
                                        oldEventTag3 = oldEventTag2;
                                        tracker2 = oldEventCode;
                                        lastTime2 = tracker3;
                                        printed3 = printed2;
                                        oldEventTag = (byte) 0;
                                    }
                                    tracker3 = lastTime2;
                                    oldEventTag2 = oldEventTag3;
                                    printed2 = printed3;
                                    rec.eventCode = tracker2;
                                    rec.eventTag = oldEventTag2;
                                    tracker3 = null;
                                } else {
                                    tracker3 = lastTime2;
                                    printed2 = printed3;
                                }
                            }
                        }
                        printed3 = true;
                        lastTime3 = lastTime2;
                        lastTime2 = tracker2;
                        hprinter.printNextItem(protoOutputStream, rec, baseTime, (boolean) (flags & 32) != 0 ? 1 : null);
                        rec.cmd = (byte) 0;
                        if (lastTime2 == null) {
                        }
                    }
                    hprinter.printNextItem(protoOutputStream, rec, baseTime, (flags & 32) != 0);
                    lastTime = lastTime3;
                    tracker = tracker3;
                    printed = printed2;
                    batteryStats = this;
                } else {
                    lastTime = lastTime2;
                    tracker = tracker2;
                    batteryStats = this;
                }
            }
            if (histStart >= 0) {
                commitCurrentHistoryBatchLocked();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("NEXT: ");
                stringBuilder.append(lastTime + 1);
                HistoryPrinter historyPrinter = hprinter;
                protoOutputStream.write(2237677961222L, stringBuilder.toString());
            }
            finishIteratingHistoryLocked();
        }
    }

    /* JADX WARNING: Missing block: B:52:0x05cf, code skipped:
            r3 = r23;
     */
    /* JADX WARNING: Missing block: B:57:0x05de, code skipped:
            r3 = 0;
     */
    /* JADX WARNING: Missing block: B:68:0x060d, code skipped:
            r90 = r5;
            r4 = r8.start(android.os.SystemProto.POWER_USE_ITEM);
            r91 = r12;
            r8.write(1159641169921L, r6);
            r8.write(1120986464258L, r3);
            r94 = r6;
            r93 = r7;
            r8.write(1103806595075L, r2.totalPowerMah);
            r8.write(1133871366148L, r2.shouldHide);
            r8.write((long) android.os.SystemProto.PowerUseItem.SCREEN_POWER_MAH, r2.screenPowerMah);
            r8.write((long) android.os.SystemProto.PowerUseItem.PROPORTIONAL_SMEAR_MAH, r2.proportionalSmearMah);
            r8.end(r4);
     */
    /* JADX WARNING: Missing block: B:69:0x0659, code skipped:
            r1 = r1 + 1;
            r3 = r88;
            r5 = r90;
            r12 = r91;
            r7 = r93;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void dumpProtoSystemLocked(ProtoOutputStream proto, BatteryStatsHelper helper) {
        long timeRemainingUs;
        int isNone;
        int telephonyNetworkType;
        long bdToken;
        long gwToken;
        long rawRealtimeUs;
        Map<String, ? extends Timer> kernelWakelocks;
        long wmctToken;
        List<BatterySipper> sippers;
        int multicastWakeLockCountTotal;
        long multicastWakeLockTimeTotalUs;
        long rawRealtimeUs2;
        Iterator it;
        Map<String, ? extends Timer> rpmStats;
        ProtoOutputStream protoOutputStream = proto;
        BatteryStatsHelper batteryStatsHelper = helper;
        long sToken = protoOutputStream.start(1146756268038L);
        long rawUptimeUs = SystemClock.uptimeMillis() * 1000;
        long rawRealtimeUs3 = SystemClock.elapsedRealtime() * 1000;
        batteryStatsHelper.create(this);
        batteryStatsHelper.refreshStats(0, -1);
        int estimatedBatteryCapacity = (int) helper.getPowerProfile().getBatteryCapacity();
        long bToken = protoOutputStream.start(1146756268033L);
        int estimatedBatteryCapacity2 = estimatedBatteryCapacity;
        protoOutputStream.write(1112396529665L, getStartClockTime());
        protoOutputStream.write(1112396529666L, getStartCount());
        protoOutputStream.write(1112396529667L, computeRealtime(rawRealtimeUs3, 0) / 1000);
        protoOutputStream.write(1112396529668L, computeUptime(rawUptimeUs, 0) / 1000);
        protoOutputStream.write(1112396529669L, computeBatteryRealtime(rawRealtimeUs3, 0) / 1000);
        protoOutputStream.write(1112396529670L, computeBatteryUptime(rawUptimeUs, 0) / 1000);
        protoOutputStream.write(1112396529671L, computeBatteryScreenOffRealtime(rawRealtimeUs3, 0) / 1000);
        protoOutputStream.write(1112396529672L, computeBatteryScreenOffUptime(rawUptimeUs, 0) / 1000);
        protoOutputStream.write(1112396529673L, getScreenDozeTime(rawRealtimeUs3, 0) / 1000);
        estimatedBatteryCapacity = estimatedBatteryCapacity2;
        protoOutputStream.write(1112396529674L, estimatedBatteryCapacity);
        protoOutputStream.write(1112396529675L, getMinLearnedBatteryCapacity());
        protoOutputStream.write(1112396529676L, getMaxLearnedBatteryCapacity());
        long bToken2 = bToken;
        protoOutputStream.end(bToken2);
        long bdToken2 = protoOutputStream.start(1146756268034L);
        long bToken3 = bToken2;
        protoOutputStream.write(1120986464257L, getLowDischargeAmountSinceCharge());
        protoOutputStream.write(1120986464258L, getHighDischargeAmountSinceCharge());
        protoOutputStream.write(1120986464259L, getDischargeAmountScreenOnSinceCharge());
        protoOutputStream.write(1120986464260L, getDischargeAmountScreenOffSinceCharge());
        protoOutputStream.write(1120986464261L, getDischargeAmountScreenDozeSinceCharge());
        int estimatedBatteryCapacity3 = estimatedBatteryCapacity;
        protoOutputStream.write(1112396529670L, getUahDischarge(0) / 1000);
        protoOutputStream.write(1112396529671L, getUahDischargeScreenOff(0) / 1000);
        protoOutputStream.write(1112396529672L, getUahDischargeScreenDoze(0) / 1000);
        protoOutputStream.write(1112396529673L, getUahDischargeLightDoze(0) / 1000);
        protoOutputStream.write(1112396529674L, getUahDischargeDeepDoze(0) / 1000);
        protoOutputStream.end(bdToken2);
        long timeRemainingUs2 = computeChargeTimeRemaining(rawRealtimeUs3);
        if (timeRemainingUs2 >= 0) {
            protoOutputStream.write(1112396529667L, timeRemainingUs2 / 1000);
        } else {
            timeRemainingUs2 = computeBatteryTimeRemaining(rawRealtimeUs3);
            if (timeRemainingUs2 >= 0) {
                timeRemainingUs = timeRemainingUs2;
                protoOutputStream.write(1112396529668L, timeRemainingUs2 / 1000);
            } else {
                timeRemainingUs = timeRemainingUs2;
                protoOutputStream.write(1112396529668L, -1);
            }
            long j = timeRemainingUs;
        }
        dumpDurationSteps(protoOutputStream, 2246267895813L, getChargeLevelStepTracker());
        int i = 0;
        while (true) {
            estimatedBatteryCapacity = i;
            isNone = 1;
            if (estimatedBatteryCapacity >= 21) {
                break;
            }
            boolean isNone2;
            long pdcToken;
            if (estimatedBatteryCapacity != 0) {
                isNone2 = false;
            }
            i = estimatedBatteryCapacity;
            if (estimatedBatteryCapacity == 20) {
                i = 0;
            }
            telephonyNetworkType = i;
            long rawRealtimeUs4 = rawRealtimeUs3;
            rawRealtimeUs3 = protoOutputStream.start(2246267895816L);
            if (isNone2) {
                pdcToken = rawRealtimeUs3;
                protoOutputStream.write(1133871366146L, isNone2);
            } else {
                pdcToken = rawRealtimeUs3;
                protoOutputStream.write(1159641169921L, telephonyNetworkType);
            }
            int i2 = estimatedBatteryCapacity;
            long pdcToken2 = pdcToken;
            rawRealtimeUs3 = rawRealtimeUs4;
            bdToken = bdToken2;
            dumpTimer(protoOutputStream, 1146756268035L, getPhoneDataConnectionTimer(estimatedBatteryCapacity), rawRealtimeUs3, false);
            protoOutputStream.end(pdcToken2);
            i = i2 + 1;
            Object obj = null;
            bdToken2 = bdToken;
        }
        long rawRealtimeUs5 = rawRealtimeUs3;
        bdToken = bdToken2;
        long j2 = bToken3;
        int i3 = estimatedBatteryCapacity3;
        dumpDurationSteps(protoOutputStream, 2246267895814L, getDischargeLevelStepTracker());
        long[] cpuFreqs = getCpuFreqs();
        if (cpuFreqs != null) {
            for (long i4 : cpuFreqs) {
                protoOutputStream.write((long) SystemProto.CPU_FREQUENCY, i4);
            }
        }
        dumpControllerActivityProto(protoOutputStream, 1146756268041L, getBluetoothControllerActivity(), 0);
        dumpControllerActivityProto(protoOutputStream, 1146756268042L, getModemControllerActivity(), 0);
        rawRealtimeUs3 = protoOutputStream.start(1146756268044L);
        protoOutputStream.write(1112396529665L, getNetworkActivityBytes(0, 0));
        protoOutputStream.write(1112396529666L, getNetworkActivityBytes(1, 0));
        protoOutputStream.write(1112396529669L, getNetworkActivityPackets(0, 0));
        protoOutputStream.write(1112396529670L, getNetworkActivityPackets(1, 0));
        long gnToken = rawRealtimeUs3;
        protoOutputStream.write(1112396529667L, getNetworkActivityBytes(2, 0));
        protoOutputStream.write(1112396529668L, getNetworkActivityBytes(3, 0));
        protoOutputStream.write(1112396529671L, getNetworkActivityPackets(2, 0));
        protoOutputStream.write(1112396529672L, getNetworkActivityPackets(3, 0));
        protoOutputStream.write(1112396529673L, getNetworkActivityBytes(4, 0));
        protoOutputStream.write(1112396529674L, getNetworkActivityBytes(5, 0));
        timeRemainingUs2 = gnToken;
        protoOutputStream.end(timeRemainingUs2);
        dumpControllerActivityProto(protoOutputStream, 1146756268043L, getWifiControllerActivity(), 0);
        long gnToken2 = timeRemainingUs2;
        long gwToken2 = protoOutputStream.start(1146756268045L);
        bToken2 = rawRealtimeUs5;
        protoOutputStream.write(1112396529665L, getWifiOnTime(bToken2, 0) / 1000);
        protoOutputStream.write(1112396529666L, getGlobalWifiRunningTime(bToken2, 0) / 1000);
        long gwToken3 = gwToken2;
        protoOutputStream.end(gwToken3);
        Map<String, ? extends Timer> kernelWakelocks2 = getKernelWakelockStats();
        Iterator it2 = kernelWakelocks2.entrySet().iterator();
        while (it2.hasNext()) {
            Entry<String, ? extends Timer> ent = (Entry) it2.next();
            Iterator it3 = it2;
            long rawRealtimeUs6 = bToken2;
            long kwToken = protoOutputStream.start(2246267895822L);
            protoOutputStream = proto;
            protoOutputStream.write(1138166333441L, (String) ent.getKey());
            Iterator it4 = it3;
            gwToken = gwToken3;
            long kwToken2 = kwToken;
            rawRealtimeUs = rawRealtimeUs6;
            isNone = 2;
            kernelWakelocks = kernelWakelocks2;
            dumpTimer(protoOutputStream, 1146756268034L, (Timer) ent.getValue(), rawRealtimeUs, 0);
            protoOutputStream.end(kwToken2);
            isNone = 1;
            it2 = it4;
            gwToken3 = gwToken;
            kernelWakelocks2 = kernelWakelocks;
            bToken2 = rawRealtimeUs;
        }
        rawRealtimeUs = bToken2;
        gwToken = gwToken3;
        kernelWakelocks = kernelWakelocks2;
        int i5 = isNone;
        long j3 = gnToken2;
        SparseArray<? extends Uid> uidStats = getUidStats();
        long fullWakeLockTimeTotalUs = 0;
        bToken3 = 0;
        i = 0;
        while (i < uidStats.size()) {
            Uid u = (Uid) uidStats.valueAt(i);
            ArrayMap<String, ? extends Wakelock> wakelocks = u.getWakelockStats();
            estimatedBatteryCapacity = wakelocks.size() - i5;
            while (estimatedBatteryCapacity >= 0) {
                Uid u2;
                ArrayMap<String, ? extends Wakelock> wakelocks2;
                Wakelock wl = (Wakelock) wakelocks.valueAt(estimatedBatteryCapacity);
                Timer fullWakeTimer = wl.getWakeTime(i5);
                if (fullWakeTimer != null) {
                    u2 = u;
                    wakelocks2 = wakelocks;
                    bToken2 = rawRealtimeUs;
                    isNone = 0;
                    fullWakeLockTimeTotalUs += fullWakeTimer.getTotalTimeLocked(bToken2, 0);
                } else {
                    u2 = u;
                    wakelocks2 = wakelocks;
                    bToken2 = rawRealtimeUs;
                    isNone = 0;
                }
                Timer partialWakeTimer = wl.getWakeTime(isNone);
                if (partialWakeTimer != null) {
                    bToken3 += partialWakeTimer.getTotalTimeLocked(bToken2, isNone);
                }
                estimatedBatteryCapacity--;
                rawRealtimeUs = bToken2;
                u = u2;
                wakelocks = wakelocks2;
                i5 = 1;
            }
            i++;
            i5 = 1;
        }
        bToken2 = rawRealtimeUs;
        rawRealtimeUs3 = protoOutputStream.start(1146756268047L);
        long rawRealtimeUs7 = bToken2;
        protoOutputStream.write(1112396529665L, getScreenOnTime(bToken2, 0) / 1000);
        timeRemainingUs2 = rawRealtimeUs7;
        protoOutputStream.write(1112396529666L, getPhoneOnTime(timeRemainingUs2, 0) / 1000);
        protoOutputStream.write(1112396529667L, fullWakeLockTimeTotalUs / 1000);
        protoOutputStream.write(1112396529668L, bToken3 / 1000);
        protoOutputStream.write(1112396529669L, getMobileRadioActiveTime(timeRemainingUs2, 0) / 1000);
        protoOutputStream.write(1112396529670L, getMobileRadioActiveAdjustedTime(0) / 1000);
        protoOutputStream.write(1120986464263L, getMobileRadioActiveCount(0));
        protoOutputStream.write(1120986464264L, getMobileRadioActiveUnknownTime(0) / 1000);
        protoOutputStream.write(1112396529673L, getInteractiveTime(timeRemainingUs2, 0) / 1000);
        protoOutputStream.write(1112396529674L, getPowerSaveModeEnabledTime(timeRemainingUs2, 0) / 1000);
        protoOutputStream.write(1120986464267L, getNumConnectivityChange(0));
        protoOutputStream.write(1112396529676L, getDeviceIdleModeTime(2, timeRemainingUs2, 0) / 1000);
        protoOutputStream.write(1120986464269L, getDeviceIdleModeCount(2, 0));
        protoOutputStream.write((long) Misc.DEEP_DOZE_IDLING_DURATION_MS, getDeviceIdlingTime(2, timeRemainingUs2, 0) / 1000);
        protoOutputStream.write((long) Misc.DEEP_DOZE_IDLING_COUNT, getDeviceIdlingCount(2, 0));
        protoOutputStream.write(1112396529680L, getLongestDeviceIdleModeTime(2));
        protoOutputStream.write(1112396529681L, getDeviceIdleModeTime(1, timeRemainingUs2, 0) / 1000);
        protoOutputStream.write((long) Misc.LIGHT_DOZE_COUNT, getDeviceIdleModeCount(1, 0));
        protoOutputStream.write(1112396529683L, getDeviceIdlingTime(1, timeRemainingUs2, 0) / 1000);
        protoOutputStream.write((long) Misc.LIGHT_DOZE_IDLING_COUNT, getDeviceIdlingCount(1, 0));
        protoOutputStream.write(1112396529685L, getLongestDeviceIdleModeTime(1));
        protoOutputStream.end(rawRealtimeUs3);
        bdToken2 = getWifiMulticastWakelockTime(timeRemainingUs2, 0);
        isNone = getWifiMulticastWakelockCount(0);
        long i42 = protoOutputStream.start(1146756268055L);
        long rawRealtimeUs8 = timeRemainingUs2;
        long mToken = rawRealtimeUs3;
        protoOutputStream.write(1112396529665L, bdToken2 / 1000);
        protoOutputStream.write(1120986464258L, isNone);
        protoOutputStream.end(i42);
        List<BatterySipper> sippers2 = helper.getUsageList();
        if (sippers2 != null) {
            i = 0;
            while (i < sippers2.size()) {
                BatterySipper bs = (BatterySipper) sippers2.get(i);
                int n = 0;
                int uid = 0;
                wmctToken = i42;
                switch (AnonymousClass2.$SwitchMap$com$android$internal$os$BatterySipper$DrainType[bs.drainType.ordinal()]) {
                    case 1:
                        n = 13;
                        break;
                    case 2:
                        n = 1;
                        break;
                    case 3:
                        n = 2;
                        break;
                    case 4:
                        n = 3;
                        break;
                    case 5:
                        n = 4;
                        break;
                    case 6:
                        n = 5;
                        break;
                    case 7:
                        n = 7;
                        break;
                    case 8:
                        n = 6;
                        break;
                    case 9:
                        sippers = sippers2;
                        multicastWakeLockCountTotal = isNone;
                        multicastWakeLockTimeTotalUs = bdToken2;
                        break;
                    case 10:
                        n = 8;
                        uid = UserHandle.getUid(bs.userId, 0);
                        break;
                    case 11:
                        n = 9;
                        break;
                    case 12:
                        n = 10;
                        break;
                    case 13:
                        n = 11;
                        break;
                    case 14:
                        n = 12;
                        break;
                    default:
                        break;
                }
            }
        }
        wmctToken = i42;
        sippers = sippers2;
        multicastWakeLockCountTotal = isNone;
        multicastWakeLockTimeTotalUs = bdToken2;
        bdToken2 = protoOutputStream.start(1146756268050L);
        protoOutputStream.write(1103806595073L, helper.getPowerProfile().getBatteryCapacity());
        protoOutputStream.write((long) PowerUseSummary.COMPUTED_POWER_MAH, helper.getComputedPower());
        protoOutputStream.write(1103806595075L, helper.getMinDrainedPower());
        protoOutputStream.write(1103806595076L, helper.getMaxDrainedPower());
        protoOutputStream.end(bdToken2);
        Map<String, ? extends Timer> rpmStats2 = getRpmStats();
        Map<String, ? extends Timer> screenOffRpmStats = getScreenOffRpmStats();
        Iterator it5 = rpmStats2.entrySet().iterator();
        while (it5.hasNext()) {
            Entry<String, ? extends Timer> ent2 = (Entry) it5.next();
            long rpmToken = protoOutputStream.start(2246267895827L);
            protoOutputStream.write(1138166333441L, (String) ent2.getKey());
            rawRealtimeUs2 = rawRealtimeUs8;
            long rpmToken2 = rpmToken;
            Entry<String, ? extends Timer> ent3 = ent2;
            Map<String, ? extends Timer> screenOffRpmStats2 = screenOffRpmStats;
            it = it5;
            rpmStats = rpmStats2;
            dumpTimer(protoOutputStream, 1146756268034L, (Timer) ent2.getValue(), rawRealtimeUs2, null);
            Entry<String, ? extends Timer> ent4 = ent3;
            screenOffRpmStats = screenOffRpmStats2;
            Map<String, ? extends Timer> screenOffRpmStats3 = screenOffRpmStats;
            dumpTimer(protoOutputStream, 1146756268035L, (Timer) screenOffRpmStats.get(ent4.getKey()), rawRealtimeUs2, null);
            protoOutputStream.end(rpmToken2);
            it5 = it;
            rpmStats2 = rpmStats;
            screenOffRpmStats = screenOffRpmStats3;
        }
        rpmStats = rpmStats2;
        rawRealtimeUs2 = rawRealtimeUs8;
        long j4 = mToken;
        long j5 = wmctToken;
        List<BatterySipper> list = sippers;
        int i6 = multicastWakeLockCountTotal;
        int i7 = 0;
        i = 0;
        while (true) {
            isNone = i;
            int i8;
            if (isNone < 5) {
                bToken2 = protoOutputStream.start(2246267895828L);
                protoOutputStream.write(1159641169921L, isNone);
                long sbToken = bToken2;
                i8 = isNone;
                dumpTimer(protoOutputStream, 1146756268034L, getScreenBrightnessTimer(isNone), rawRealtimeUs2, 0);
                protoOutputStream.end(sbToken);
                i = i8 + 1;
            } else {
                dumpTimer(protoOutputStream, 1146756268053L, getPhoneSignalScanningTimer(), rawRealtimeUs2, 0);
                i = 0;
                while (true) {
                    isNone = i;
                    if (isNone < 5) {
                        bToken2 = protoOutputStream.start(2246267895824L);
                        protoOutputStream.write(1159641169921L, isNone);
                        long pssToken = bToken2;
                        i8 = isNone;
                        dumpTimer(protoOutputStream, 1146756268034L, getPhoneSignalStrengthTimer(isNone), rawRealtimeUs2, 0);
                        protoOutputStream.end(pssToken);
                        i = i8 + 1;
                    } else {
                        rpmStats2 = getWakeupReasonStats();
                        Iterator it6 = rpmStats2.entrySet().iterator();
                        while (it6.hasNext()) {
                            Entry<String, ? extends Timer> ent5 = (Entry) it6.next();
                            long wrToken = protoOutputStream.start(2246267895830L);
                            protoOutputStream.write(1138166333441L, (String) ent5.getKey());
                            long wrToken2 = wrToken;
                            it = it6;
                            Entry<String, ? extends Timer> entry = ent5;
                            Map<String, ? extends Timer> wakeupReasons = rpmStats2;
                            dumpTimer(protoOutputStream, 1146756268034L, (Timer) ent5.getValue(), rawRealtimeUs2, null);
                            protoOutputStream.end(wrToken2);
                            it6 = it;
                            rpmStats2 = wakeupReasons;
                        }
                        i = 0;
                        while (true) {
                            isNone = i;
                            if (isNone < 5) {
                                bToken2 = protoOutputStream.start(SystemProto.WIFI_SIGNAL_STRENGTH);
                                protoOutputStream.write(1159641169921L, isNone);
                                long wssToken = bToken2;
                                int i9 = isNone;
                                dumpTimer(protoOutputStream, 1146756268034L, getWifiSignalStrengthTimer(isNone), rawRealtimeUs2, 0);
                                protoOutputStream.end(wssToken);
                                i = i9 + 1;
                            } else {
                                i = 0;
                                while (true) {
                                    isNone = i;
                                    if (isNone < 8) {
                                        rawRealtimeUs3 = protoOutputStream.start(2246267895833L);
                                        protoOutputStream.write(1159641169921L, isNone);
                                        long wsToken = rawRealtimeUs3;
                                        i8 = isNone;
                                        dumpTimer(protoOutputStream, 1146756268034L, getWifiStateTimer(isNone), rawRealtimeUs2, 0);
                                        protoOutputStream.end(wsToken);
                                        i = i8 + 1;
                                    } else {
                                        while (true) {
                                            isNone = i7;
                                            if (isNone < 13) {
                                                rawRealtimeUs3 = protoOutputStream.start(2246267895834L);
                                                protoOutputStream.write(1159641169921L, isNone);
                                                timeRemainingUs = 1159641169921L;
                                                long wssToken2 = rawRealtimeUs3;
                                                i7 = isNone;
                                                dumpTimer(protoOutputStream, 1146756268034L, getWifiSupplStateTimer(isNone), rawRealtimeUs2, 0);
                                                protoOutputStream.end(wssToken2);
                                                i7++;
                                            } else {
                                                protoOutputStream.end(sToken);
                                                return;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
