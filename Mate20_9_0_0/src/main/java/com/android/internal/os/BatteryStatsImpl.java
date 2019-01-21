package com.android.internal.os;

import android.app.ActivityManager;
import android.bluetooth.BluetoothActivityEnergyInfo;
import android.bluetooth.UidTraffic;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.NetworkStats;
import android.net.Uri;
import android.net.wifi.WifiActivityEnergyInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryStats;
import android.os.BatteryStats.ControllerActivityCounter;
import android.os.BatteryStats.DailyItem;
import android.os.BatteryStats.HistoryEventTracker;
import android.os.BatteryStats.HistoryItem;
import android.os.BatteryStats.HistoryPrinter;
import android.os.BatteryStats.HistoryStepDetails;
import android.os.BatteryStats.HistoryTag;
import android.os.BatteryStats.LevelStepTracker;
import android.os.BatteryStats.LongCounter;
import android.os.BatteryStats.LongCounterArray;
import android.os.BatteryStats.PackageChange;
import android.os.BatteryStats.Uid.Pid;
import android.os.BatteryStats.Uid.Proc.ExcessivePower;
import android.os.Build;
import android.os.FileUtils;
import android.os.Handler;
import android.os.IBatteryPropertiesRegistrar;
import android.os.IBatteryPropertiesRegistrar.Stub;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.ParcelFormatException;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.os.WorkSource;
import android.os.WorkSource.WorkChain;
import android.os.connectivity.CellularBatteryStats;
import android.os.connectivity.GpsBatteryStats;
import android.os.connectivity.WifiBatteryStats;
import android.provider.Settings.Global;
import android.telephony.ModemActivityInfo;
import android.telephony.SignalStrength;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.HwLog;
import android.util.IntArray;
import android.util.KeyValueListParser;
import android.util.Log;
import android.util.LogWriter;
import android.util.LongSparseArray;
import android.util.LongSparseLongArray;
import android.util.MutableInt;
import android.util.Pools.Pool;
import android.util.Pools.SynchronizedPool;
import android.util.Printer;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.SparseLongArray;
import android.util.StatsLog;
import android.util.TimeUtils;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.annotations.VisibleForTesting.Visibility;
import com.android.internal.logging.EventLogTags;
import com.android.internal.net.NetworkStatsFactory;
import com.android.internal.os.RpmStats.PowerStateElement;
import com.android.internal.os.RpmStats.PowerStatePlatformSleepState;
import com.android.internal.os.RpmStats.PowerStateSubsystem;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.AsyncService;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.JournaledFile;
import com.android.internal.util.Protocol;
import com.android.internal.util.XmlUtils;
import com.huawei.pgmng.PGAction;
import com.huawei.pgmng.log.LogPower;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11ExtensionPack;
import libcore.util.EmptyArray;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class BatteryStatsImpl extends BatteryStats {
    static final int BATTERY_DELTA_LEVEL_FLAG = 1;
    public static final int BATTERY_PLUGGED_NONE = 0;
    public static final Creator<BatteryStatsImpl> CREATOR = new Creator<BatteryStatsImpl>() {
        public BatteryStatsImpl createFromParcel(Parcel in) {
            return new BatteryStatsImpl(in);
        }

        public BatteryStatsImpl[] newArray(int size) {
            return new BatteryStatsImpl[size];
        }
    };
    private static final boolean DEBUG = false;
    public static final boolean DEBUG_ENERGY = false;
    private static final boolean DEBUG_ENERGY_CPU = false;
    private static final boolean DEBUG_HISTORY = false;
    private static final boolean DEBUG_MEMORY = false;
    static final long DELAY_UPDATE_WAKELOCKS = 5000;
    static final int DELTA_BATTERY_CHARGE_FLAG = 16777216;
    static final int DELTA_BATTERY_LEVEL_FLAG = 524288;
    static final int DELTA_EVENT_FLAG = 8388608;
    static final int DELTA_STATE2_FLAG = 2097152;
    static final int DELTA_STATE_FLAG = 1048576;
    static final int DELTA_STATE_MASK = -33554432;
    static final int DELTA_TIME_ABS = 524285;
    static final int DELTA_TIME_INT = 524286;
    static final int DELTA_TIME_LONG = 524287;
    static final int DELTA_TIME_MASK = 524287;
    static final int DELTA_WAKELOCK_FLAG = 4194304;
    private static final int MAGIC = -1166707595;
    static final int MAX_DAILY_ITEMS = 10;
    static final int MAX_HISTORY_BUFFER;
    private static final int MAX_HISTORY_ITEMS;
    static final int MAX_LEVEL_STEPS = 200;
    static final int MAX_MAX_HISTORY_BUFFER;
    private static final int MAX_MAX_HISTORY_ITEMS;
    private static final int MAX_WAKELOCKS_PER_UID;
    private static final int MAX_WAKERLOCKS_WEIXIN = 60;
    static final int MSG_REPORT_CHARGING = 3;
    static final int MSG_REPORT_CPU_UPDATE_NEEDED = 1;
    static final int MSG_REPORT_POWER_CHANGE = 2;
    static final int MSG_REPORT_RESET_STATS = 4;
    private static final int NUM_BT_TX_LEVELS = 1;
    private static final int NUM_WIFI_TX_LEVELS = 1;
    private static final int READ_KERNEL_WAKELOCK_STATS_MAX_TIMEOUT = 500;
    private static final long RPM_STATS_UPDATE_FREQ_MS = 1000;
    static final int STATE_BATTERY_HEALTH_MASK = 7;
    static final int STATE_BATTERY_HEALTH_SHIFT = 26;
    static final int STATE_BATTERY_MASK = -16777216;
    static final int STATE_BATTERY_PLUG_MASK = 3;
    static final int STATE_BATTERY_PLUG_SHIFT = 24;
    static final int STATE_BATTERY_STATUS_MASK = 7;
    static final int STATE_BATTERY_STATUS_SHIFT = 29;
    private static final String TAG = "BatteryStatsImpl";
    private static final int USB_DATA_CONNECTED = 2;
    private static final int USB_DATA_DISCONNECTED = 1;
    private static final int USB_DATA_UNKNOWN = 0;
    private static final boolean USE_OLD_HISTORY = false;
    private static final int VERSION = 177;
    @VisibleForTesting
    public static final int WAKE_LOCK_WEIGHT = 50;
    final HistoryEventTracker mActiveEvents;
    int mActiveHistoryStates;
    int mActiveHistoryStates2;
    int mAudioOnNesting;
    StopwatchTimer mAudioOnTimer;
    final ArrayList<StopwatchTimer> mAudioTurnedOnTimers;
    ControllerActivityCounterImpl mBluetoothActivity;
    int mBluetoothScanNesting;
    final ArrayList<StopwatchTimer> mBluetoothScanOnTimers;
    @VisibleForTesting(visibility = Visibility.PACKAGE)
    protected StopwatchTimer mBluetoothScanTimer;
    private BatteryCallback mCallback;
    int mCameraOnNesting;
    StopwatchTimer mCameraOnTimer;
    final ArrayList<StopwatchTimer> mCameraTurnedOnTimers;
    int mChangedStates;
    int mChangedStates2;
    final LevelStepTracker mChargeStepTracker;
    boolean mCharging;
    public final AtomicFile mCheckinFile;
    protected Clocks mClocks;
    @GuardedBy("this")
    private final Constants mConstants;
    private long[] mCpuFreqs;
    @GuardedBy("this")
    private long mCpuTimeReadsTrackingStartTime;
    final HistoryStepDetails mCurHistoryStepDetails;
    long mCurStepCpuSystemTime;
    long mCurStepCpuUserTime;
    int mCurStepMode;
    long mCurStepStatIOWaitTime;
    long mCurStepStatIdleTime;
    long mCurStepStatIrqTime;
    long mCurStepStatSoftIrqTime;
    long mCurStepStatSystemTime;
    long mCurStepStatUserTime;
    int mCurrentBatteryLevel;
    final LevelStepTracker mDailyChargeStepTracker;
    final LevelStepTracker mDailyDischargeStepTracker;
    public final AtomicFile mDailyFile;
    final ArrayList<DailyItem> mDailyItems;
    ArrayList<PackageChange> mDailyPackageChanges;
    long mDailyStartTime;
    int mDeviceIdleMode;
    StopwatchTimer mDeviceIdleModeFullTimer;
    StopwatchTimer mDeviceIdleModeLightTimer;
    boolean mDeviceIdling;
    StopwatchTimer mDeviceIdlingTimer;
    boolean mDeviceLightIdling;
    StopwatchTimer mDeviceLightIdlingTimer;
    int mDischargeAmountScreenDoze;
    int mDischargeAmountScreenDozeSinceCharge;
    int mDischargeAmountScreenOff;
    int mDischargeAmountScreenOffSinceCharge;
    int mDischargeAmountScreenOn;
    int mDischargeAmountScreenOnSinceCharge;
    private LongSamplingCounter mDischargeCounter;
    int mDischargeCurrentLevel;
    private LongSamplingCounter mDischargeDeepDozeCounter;
    private LongSamplingCounter mDischargeLightDozeCounter;
    int mDischargePlugLevel;
    private LongSamplingCounter mDischargeScreenDozeCounter;
    int mDischargeScreenDozeUnplugLevel;
    private LongSamplingCounter mDischargeScreenOffCounter;
    int mDischargeScreenOffUnplugLevel;
    int mDischargeScreenOnUnplugLevel;
    int mDischargeStartLevel;
    final LevelStepTracker mDischargeStepTracker;
    int mDischargeUnplugLevel;
    boolean mDistributeWakelockCpu;
    final ArrayList<StopwatchTimer> mDrawTimers;
    String mEndPlatformVersion;
    private int mEstimatedBatteryCapacity;
    private ExternalStatsSync mExternalSync;
    private final JournaledFile mFile;
    int mFlashlightOnNesting;
    StopwatchTimer mFlashlightOnTimer;
    final ArrayList<StopwatchTimer> mFlashlightTurnedOnTimers;
    final ArrayList<StopwatchTimer> mFullTimers;
    final ArrayList<StopwatchTimer> mFullWifiLockTimers;
    boolean mGlobalWifiRunning;
    StopwatchTimer mGlobalWifiRunningTimer;
    int mGpsNesting;
    int mGpsSignalQualityBin;
    @VisibleForTesting(visibility = Visibility.PACKAGE)
    protected final StopwatchTimer[] mGpsSignalQualityTimer;
    public Handler mHandler;
    boolean mHasBluetoothReporting;
    boolean mHasModemReporting;
    boolean mHasWifiReporting;
    protected boolean mHaveBatteryLevel;
    int mHighDischargeAmountSinceCharge;
    HistoryItem mHistory;
    final HistoryItem mHistoryAddTmp;
    long mHistoryBaseTime;
    final Parcel mHistoryBuffer;
    int mHistoryBufferLastPos;
    HistoryItem mHistoryCache;
    final HistoryItem mHistoryCur;
    HistoryItem mHistoryEnd;
    private HistoryItem mHistoryIterator;
    HistoryItem mHistoryLastEnd;
    final HistoryItem mHistoryLastLastWritten;
    final HistoryItem mHistoryLastWritten;
    boolean mHistoryOverflow;
    final HistoryItem mHistoryReadTmp;
    final HashMap<HistoryTag, Integer> mHistoryTagPool;
    int mInitStepMode;
    private String mInitialAcquireWakeName;
    private int mInitialAcquireWakeUid;
    boolean mInteractive;
    StopwatchTimer mInteractiveTimer;
    boolean mIsCellularTxPowerHigh;
    final SparseIntArray mIsolatedUids;
    private boolean mIteratingHistory;
    @VisibleForTesting
    protected KernelCpuSpeedReader[] mKernelCpuSpeedReaders;
    private final KernelMemoryBandwidthStats mKernelMemoryBandwidthStats;
    private final LongSparseArray<SamplingTimer> mKernelMemoryStats;
    @VisibleForTesting
    protected KernelSingleUidTimeReader mKernelSingleUidTimeReader;
    @VisibleForTesting
    protected KernelUidCpuActiveTimeReader mKernelUidCpuActiveTimeReader;
    @VisibleForTesting
    protected KernelUidCpuClusterTimeReader mKernelUidCpuClusterTimeReader;
    @VisibleForTesting
    protected KernelUidCpuFreqTimeReader mKernelUidCpuFreqTimeReader;
    @VisibleForTesting
    protected KernelUidCpuTimeReader mKernelUidCpuTimeReader;
    private final KernelWakelockReader mKernelWakelockReader;
    private final HashMap<String, SamplingTimer> mKernelWakelockStats;
    private final BluetoothActivityInfoCache mLastBluetoothActivityInfo;
    int mLastChargeStepLevel;
    int mLastChargingStateLevel;
    int mLastDischargeStepLevel;
    long mLastHistoryElapsedRealtime;
    HistoryStepDetails mLastHistoryStepDetails;
    byte mLastHistoryStepLevel;
    long mLastIdleTimeStart;
    private ModemActivityInfo mLastModemActivityInfo;
    @GuardedBy("mModemNetworkLock")
    private NetworkStats mLastModemNetworkStats;
    @VisibleForTesting
    protected ArrayList<StopwatchTimer> mLastPartialTimers;
    private long mLastRpmStatsUpdateTimeMs;
    long mLastStepCpuSystemTime;
    long mLastStepCpuUserTime;
    long mLastStepStatIOWaitTime;
    long mLastStepStatIdleTime;
    long mLastStepStatIrqTime;
    long mLastStepStatSoftIrqTime;
    long mLastStepStatSystemTime;
    long mLastStepStatUserTime;
    String mLastWakeupReason;
    long mLastWakeupUptimeMs;
    @GuardedBy("mWifiNetworkLock")
    private NetworkStats mLastWifiNetworkStats;
    long mLastWriteTime;
    private int mLoadedNumConnectivityChange;
    long mLongestFullIdleTime;
    long mLongestLightIdleTime;
    int mLowDischargeAmountSinceCharge;
    int mMaxChargeStepLevel;
    private int mMaxLearnedBatteryCapacity;
    int mMinDischargeStepLevel;
    private int mMinLearnedBatteryCapacity;
    LongSamplingCounter mMobileRadioActiveAdjustedTime;
    StopwatchTimer mMobileRadioActivePerAppTimer;
    long mMobileRadioActiveStartTime;
    StopwatchTimer mMobileRadioActiveTimer;
    LongSamplingCounter mMobileRadioActiveUnknownCount;
    LongSamplingCounter mMobileRadioActiveUnknownTime;
    int mMobileRadioPowerState;
    int mModStepMode;
    ControllerActivityCounterImpl mModemActivity;
    @GuardedBy("mModemNetworkLock")
    private String[] mModemIfaces;
    private final Object mModemNetworkLock;
    final LongSamplingCounter[] mNetworkByteActivityCounters;
    final LongSamplingCounter[] mNetworkPacketActivityCounters;
    private final NetworkStatsFactory mNetworkStatsFactory;
    private final Pool<NetworkStats> mNetworkStatsPool;
    int mNextHistoryTagIdx;
    long mNextMaxDailyDeadline;
    long mNextMinDailyDeadline;
    boolean mNoAutoReset;
    @GuardedBy("this")
    private int mNumAllUidCpuTimeReads;
    @GuardedBy("this")
    private long mNumBatchedSingleUidCpuTimeReads;
    private int mNumConnectivityChange;
    int mNumHistoryItems;
    int mNumHistoryTagChars;
    @GuardedBy("this")
    private long mNumSingleUidCpuTimeReads;
    @GuardedBy("this")
    private int mNumUidsRemoved;
    boolean mOnBattery;
    @VisibleForTesting
    protected boolean mOnBatteryInternal;
    protected final TimeBase mOnBatteryScreenOffTimeBase;
    protected final TimeBase mOnBatteryTimeBase;
    @VisibleForTesting
    protected ArrayList<StopwatchTimer> mPartialTimers;
    @GuardedBy("this")
    @VisibleForTesting(visibility = Visibility.PACKAGE)
    protected Queue<UidToRemove> mPendingRemovedUids;
    @GuardedBy("this")
    @VisibleForTesting
    protected final SparseIntArray mPendingUids;
    Parcel mPendingWrite;
    @GuardedBy("this")
    public boolean mPerProcStateCpuTimesAvailable;
    int mPhoneDataConnectionType;
    final StopwatchTimer[] mPhoneDataConnectionsTimer;
    boolean mPhoneOn;
    StopwatchTimer mPhoneOnTimer;
    private int mPhoneServiceState;
    private int mPhoneServiceStateRaw;
    StopwatchTimer mPhoneSignalScanningTimer;
    int mPhoneSignalStrengthBin;
    int mPhoneSignalStrengthBinRaw;
    final StopwatchTimer[] mPhoneSignalStrengthsTimer;
    private int mPhoneSimStateRaw;
    private final PlatformIdleStateCallback mPlatformIdleStateCallback;
    @VisibleForTesting
    protected PowerProfile mPowerProfile;
    boolean mPowerSaveModeEnabled;
    StopwatchTimer mPowerSaveModeEnabledTimer;
    boolean mPretendScreenOff;
    int mReadHistoryChars;
    final HistoryStepDetails mReadHistoryStepDetails;
    String[] mReadHistoryStrings;
    int[] mReadHistoryUids;
    private boolean mReadOverflow;
    long mRealtime;
    long mRealtimeStart;
    public boolean mRecordAllHistory;
    protected boolean mRecordingHistory;
    private final HashMap<String, SamplingTimer> mRpmStats;
    int mScreenBrightnessBin;
    final StopwatchTimer[] mScreenBrightnessTimer;
    @VisibleForTesting(visibility = Visibility.PACKAGE)
    protected StopwatchTimer mScreenDozeTimer;
    private final HashMap<String, SamplingTimer> mScreenOffRpmStats;
    @VisibleForTesting(visibility = Visibility.PACKAGE)
    protected StopwatchTimer mScreenOnTimer;
    @VisibleForTesting(visibility = Visibility.PACKAGE)
    protected int mScreenState;
    int mSensorNesting;
    final SparseArray<ArrayList<StopwatchTimer>> mSensorTimers;
    boolean mShuttingDown;
    long mStartClockTime;
    int mStartCount;
    String mStartPlatformVersion;
    long mTempTotalCpuSystemTimeUs;
    long mTempTotalCpuUserTimeUs;
    final HistoryStepDetails mTmpHistoryStepDetails;
    private final RpmStats mTmpRpmStats;
    private final KernelWakelockStats mTmpWakelockStats;
    long mTrackRunningHistoryElapsedRealtime;
    long mTrackRunningHistoryUptime;
    final SparseArray<Uid> mUidStats;
    private int mUnpluggedNumConnectivityChange;
    long mUptime;
    long mUptimeStart;
    int mUsbDataState;
    @VisibleForTesting
    protected UserInfoProvider mUserInfoProvider;
    int mVideoOnNesting;
    StopwatchTimer mVideoOnTimer;
    final ArrayList<StopwatchTimer> mVideoTurnedOnTimers;
    long[][] mWakeLockAllocationsUs;
    boolean mWakeLockImportant;
    int mWakeLockNesting;
    private final HashMap<String, SamplingTimer> mWakeupReasonStats;
    StopwatchTimer mWifiActiveTimer;
    ControllerActivityCounterImpl mWifiActivity;
    final SparseArray<ArrayList<StopwatchTimer>> mWifiBatchedScanTimers;
    int mWifiFullLockNesting;
    @GuardedBy("mWifiNetworkLock")
    private String[] mWifiIfaces;
    int mWifiMulticastNesting;
    final ArrayList<StopwatchTimer> mWifiMulticastTimers;
    StopwatchTimer mWifiMulticastWakelockTimer;
    private final Object mWifiNetworkLock;
    boolean mWifiOn;
    StopwatchTimer mWifiOnTimer;
    int mWifiRadioPowerState;
    final ArrayList<StopwatchTimer> mWifiRunningTimers;
    int mWifiScanNesting;
    final ArrayList<StopwatchTimer> mWifiScanTimers;
    int mWifiSignalStrengthBin;
    final StopwatchTimer[] mWifiSignalStrengthsTimer;
    int mWifiState;
    final StopwatchTimer[] mWifiStateTimer;
    int mWifiSupplState;
    final StopwatchTimer[] mWifiSupplStateTimer;
    final ArrayList<StopwatchTimer> mWindowTimers;
    final ReentrantLock mWriteLock;

    public interface BatteryCallback {
        void batteryNeedsCpuUpdate();

        void batteryPowerChanged(boolean z);

        void batterySendBroadcast(Intent intent);

        void batteryStatsReset();
    }

    private final class BluetoothActivityInfoCache {
        long energy;
        long idleTimeMs;
        long rxTimeMs;
        long txTimeMs;
        SparseLongArray uidRxBytes;
        SparseLongArray uidTxBytes;

        private BluetoothActivityInfoCache() {
            this.uidRxBytes = new SparseLongArray();
            this.uidTxBytes = new SparseLongArray();
        }

        /* synthetic */ BluetoothActivityInfoCache(BatteryStatsImpl x0, AnonymousClass1 x1) {
            this();
        }

        void set(BluetoothActivityEnergyInfo info) {
            this.idleTimeMs = info.getControllerIdleTimeMillis();
            this.rxTimeMs = info.getControllerRxTimeMillis();
            this.txTimeMs = info.getControllerTxTimeMillis();
            this.energy = info.getControllerEnergyUsed();
            if (info.getUidTraffic() != null) {
                for (UidTraffic traffic : info.getUidTraffic()) {
                    this.uidRxBytes.put(traffic.getUid(), traffic.getRxBytes());
                    this.uidTxBytes.put(traffic.getUid(), traffic.getTxBytes());
                }
            }
        }
    }

    public interface Clocks {
        long elapsedRealtime();

        long uptimeMillis();
    }

    public interface ExternalStatsSync {
        public static final int UPDATE_ALL = 31;
        public static final int UPDATE_BT = 8;
        public static final int UPDATE_CPU = 1;
        public static final int UPDATE_RADIO = 4;
        public static final int UPDATE_RPM = 16;
        public static final int UPDATE_WIFI = 2;

        void cancelCpuSyncDueToWakelockChange();

        Future<?> scheduleCopyFromAllUidsCpuTimes(boolean z, boolean z2);

        Future<?> scheduleCpuSyncDueToRemovedUid(int i);

        Future<?> scheduleCpuSyncDueToScreenStateChange(boolean z, boolean z2);

        Future<?> scheduleCpuSyncDueToSettingChange();

        Future<?> scheduleCpuSyncDueToWakelockChange(long j);

        Future<?> scheduleReadProcStateCpuTimes(boolean z, boolean z2, long j);

        Future<?> scheduleSync(String str, int i);

        Future<?> scheduleSyncDueToBatteryLevelChange(long j);
    }

    public abstract class OverflowArrayMap<T> {
        private static final String OVERFLOW_NAME = "*overflow*";
        private static final String OVERFLOW_WEIXIN = "WakerLock:overflow";
        int M = 0;
        ArrayMap<String, MutableInt> mActiveOverflow;
        ArrayMap<String, MutableInt> mActiveOverflowWeixin;
        T mCurOverflow;
        T mCurOverflowWeixin;
        long mLastCleanupTime;
        long mLastClearTime;
        long mLastOverflowFinishTime;
        long mLastOverflowTime;
        final ArrayMap<String, T> mMap = new ArrayMap();
        final int mUid;

        public abstract T instantiateObject();

        public OverflowArrayMap(int uid) {
            this.mUid = uid;
        }

        public ArrayMap<String, T> getMap() {
            return this.mMap;
        }

        public void clear() {
            this.mLastClearTime = SystemClock.elapsedRealtime();
            this.mMap.clear();
            this.mCurOverflow = null;
            this.mActiveOverflow = null;
            this.mCurOverflowWeixin = null;
            this.mActiveOverflowWeixin = null;
        }

        public void add(String name, T obj) {
            if (name == null) {
                name = "";
            }
            this.mMap.put(name, obj);
            if (OVERFLOW_NAME.equals(name)) {
                this.mCurOverflow = obj;
            } else if (OVERFLOW_WEIXIN.equals(name)) {
                this.mCurOverflowWeixin = obj;
            }
            if (name.startsWith("WakerLock:")) {
                this.M++;
            }
        }

        public void cleanup() {
            String str;
            StringBuilder stringBuilder;
            this.mLastCleanupTime = SystemClock.elapsedRealtime();
            if (this.mActiveOverflowWeixin != null && this.mActiveOverflowWeixin.size() == 0) {
                this.mActiveOverflowWeixin = null;
            }
            if (this.mActiveOverflowWeixin == null) {
                if (this.mMap.containsKey(OVERFLOW_WEIXIN)) {
                    str = BatteryStatsImpl.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Cleaning up with no active overflow weixin, but have overflow entry ");
                    stringBuilder.append(this.mMap.get(OVERFLOW_WEIXIN));
                    Slog.wtf(str, stringBuilder.toString());
                    this.mMap.remove(OVERFLOW_WEIXIN);
                }
                this.mCurOverflowWeixin = null;
            } else if (this.mCurOverflowWeixin == null || !this.mMap.containsKey(OVERFLOW_WEIXIN)) {
                str = BatteryStatsImpl.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Cleaning up with active overflow weixin, but no overflow entry: cur=");
                stringBuilder.append(this.mCurOverflowWeixin);
                stringBuilder.append(" map=");
                stringBuilder.append(this.mMap.get(OVERFLOW_WEIXIN));
                Slog.wtf(str, stringBuilder.toString());
            }
            if (this.mActiveOverflow != null && this.mActiveOverflow.size() == 0) {
                this.mActiveOverflow = null;
            }
            if (this.mActiveOverflow == null) {
                if (this.mMap.containsKey(OVERFLOW_NAME)) {
                    str = BatteryStatsImpl.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Cleaning up with no active overflow, but have overflow entry ");
                    stringBuilder.append(this.mMap.get(OVERFLOW_NAME));
                    Slog.wtf(str, stringBuilder.toString());
                    this.mMap.remove(OVERFLOW_NAME);
                }
                this.mCurOverflow = null;
            } else if (this.mCurOverflow == null || !this.mMap.containsKey(OVERFLOW_NAME)) {
                str = BatteryStatsImpl.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Cleaning up with active overflow, but no overflow entry: cur=");
                stringBuilder2.append(this.mCurOverflow);
                stringBuilder2.append(" map=");
                stringBuilder2.append(this.mMap.get(OVERFLOW_NAME));
                Slog.wtf(str, stringBuilder2.toString());
            }
        }

        public T startObject(String name) {
            if (name == null) {
                name = "";
            }
            T obj = this.mMap.get(name);
            if (obj != null) {
                return obj;
            }
            MutableInt over;
            String str;
            StringBuilder stringBuilder;
            T instantiateObject;
            if (this.mActiveOverflowWeixin != null) {
                over = (MutableInt) this.mActiveOverflowWeixin.get(name);
                if (over != null) {
                    obj = this.mCurOverflowWeixin;
                    if (obj == null) {
                        str = BatteryStatsImpl.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Have active overflow ");
                        stringBuilder.append(name);
                        stringBuilder.append(" but null overflow weixin");
                        Slog.wtf(str, stringBuilder.toString());
                        instantiateObject = instantiateObject();
                        this.mCurOverflowWeixin = instantiateObject;
                        obj = instantiateObject;
                        this.mMap.put(OVERFLOW_WEIXIN, obj);
                    }
                    over.value++;
                    return obj;
                }
            }
            if (name.startsWith("WakerLock:")) {
                this.M++;
                if (this.M > 60) {
                    obj = this.mCurOverflowWeixin;
                    if (obj == null) {
                        T instantiateObject2 = instantiateObject();
                        this.mCurOverflowWeixin = instantiateObject2;
                        obj = instantiateObject2;
                        this.mMap.put(OVERFLOW_WEIXIN, obj);
                    }
                    if (this.mActiveOverflowWeixin == null) {
                        this.mActiveOverflowWeixin = new ArrayMap();
                    }
                    this.mActiveOverflowWeixin.put(name, new MutableInt(1));
                    return obj;
                }
            }
            if (this.mActiveOverflow != null) {
                over = (MutableInt) this.mActiveOverflow.get(name);
                if (over != null) {
                    obj = this.mCurOverflow;
                    if (obj == null) {
                        str = BatteryStatsImpl.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Have active overflow ");
                        stringBuilder.append(name);
                        stringBuilder.append(" but null overflow");
                        Slog.wtf(str, stringBuilder.toString());
                        instantiateObject = instantiateObject();
                        this.mCurOverflow = instantiateObject;
                        obj = instantiateObject;
                        this.mMap.put(OVERFLOW_NAME, obj);
                    }
                    over.value++;
                    return obj;
                }
            }
            if (this.mMap.size() >= BatteryStatsImpl.MAX_WAKELOCKS_PER_UID) {
                str = BatteryStatsImpl.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("wakelocks more than 100, name: ");
                stringBuilder.append(name);
                Slog.i(str, stringBuilder.toString());
                obj = this.mCurOverflow;
                if (obj == null) {
                    instantiateObject = instantiateObject();
                    this.mCurOverflow = instantiateObject;
                    obj = instantiateObject;
                    this.mMap.put(OVERFLOW_NAME, obj);
                }
                if (this.mActiveOverflow == null) {
                    this.mActiveOverflow = new ArrayMap();
                }
                this.mActiveOverflow.put(name, new MutableInt(1));
                this.mLastOverflowTime = SystemClock.elapsedRealtime();
                return obj;
            }
            obj = instantiateObject();
            this.mMap.put(name, obj);
            return obj;
        }

        public T stopObject(String name) {
            if (name == null) {
                name = "";
            }
            T obj = this.mMap.get(name);
            if (obj != null) {
                return obj;
            }
            MutableInt over;
            if (this.mActiveOverflowWeixin != null) {
                over = (MutableInt) this.mActiveOverflowWeixin.get(name);
                if (over != null) {
                    obj = this.mCurOverflowWeixin;
                    if (obj != null) {
                        over.value--;
                        if (over.value <= 0) {
                            this.mActiveOverflowWeixin.remove(name);
                        }
                        return obj;
                    }
                }
            }
            if (this.mActiveOverflow != null) {
                over = (MutableInt) this.mActiveOverflow.get(name);
                if (over != null) {
                    obj = this.mCurOverflow;
                    if (obj != null) {
                        over.value--;
                        if (over.value <= 0) {
                            this.mActiveOverflow.remove(name);
                            this.mLastOverflowFinishTime = SystemClock.elapsedRealtime();
                        }
                        return obj;
                    }
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Unable to find object for ");
            sb.append(name);
            sb.append(" in uid ");
            sb.append(this.mUid);
            sb.append(" mapsize=");
            sb.append(this.mMap.size());
            sb.append(" activeoverflow=");
            sb.append(this.mActiveOverflow);
            sb.append(" curoverflow=");
            sb.append(this.mCurOverflow);
            long now = SystemClock.elapsedRealtime();
            if (this.mLastOverflowTime != 0) {
                sb.append(" lastOverflowTime=");
                TimeUtils.formatDuration(this.mLastOverflowTime - now, sb);
            }
            if (this.mLastOverflowFinishTime != 0) {
                sb.append(" lastOverflowFinishTime=");
                TimeUtils.formatDuration(this.mLastOverflowFinishTime - now, sb);
            }
            if (this.mLastClearTime != 0) {
                sb.append(" lastClearTime=");
                TimeUtils.formatDuration(this.mLastClearTime - now, sb);
            }
            if (this.mLastCleanupTime != 0) {
                sb.append(" lastCleanupTime=");
                TimeUtils.formatDuration(this.mLastCleanupTime - now, sb);
            }
            Slog.wtf(BatteryStatsImpl.TAG, sb.toString());
            return null;
        }
    }

    public interface PlatformIdleStateCallback {
        void fillLowPowerStats(RpmStats rpmStats);

        String getPlatformLowPowerStats();

        String getSubsystemLowPowerStats();
    }

    public static class TimeBase {
        protected final ArrayList<TimeBaseObs> mObservers = new ArrayList();
        protected long mPastRealtime;
        protected long mPastUptime;
        protected long mRealtime;
        protected long mRealtimeStart;
        protected boolean mRunning;
        protected long mUnpluggedRealtime;
        protected long mUnpluggedUptime;
        protected long mUptime;
        protected long mUptimeStart;

        public void dump(PrintWriter pw, String prefix) {
            StringBuilder sb = new StringBuilder(128);
            pw.print(prefix);
            pw.print("mRunning=");
            pw.println(this.mRunning);
            sb.setLength(0);
            sb.append(prefix);
            sb.append("mUptime=");
            BatteryStats.formatTimeMs(sb, this.mUptime / 1000);
            pw.println(sb.toString());
            sb.setLength(0);
            sb.append(prefix);
            sb.append("mRealtime=");
            BatteryStats.formatTimeMs(sb, this.mRealtime / 1000);
            pw.println(sb.toString());
            sb.setLength(0);
            sb.append(prefix);
            sb.append("mPastUptime=");
            BatteryStats.formatTimeMs(sb, this.mPastUptime / 1000);
            sb.append("mUptimeStart=");
            BatteryStats.formatTimeMs(sb, this.mUptimeStart / 1000);
            sb.append("mUnpluggedUptime=");
            BatteryStats.formatTimeMs(sb, this.mUnpluggedUptime / 1000);
            pw.println(sb.toString());
            sb.setLength(0);
            sb.append(prefix);
            sb.append("mPastRealtime=");
            BatteryStats.formatTimeMs(sb, this.mPastRealtime / 1000);
            sb.append("mRealtimeStart=");
            BatteryStats.formatTimeMs(sb, this.mRealtimeStart / 1000);
            sb.append("mUnpluggedRealtime=");
            BatteryStats.formatTimeMs(sb, this.mUnpluggedRealtime / 1000);
            pw.println(sb.toString());
        }

        public synchronized void add(TimeBaseObs observer) {
            this.mObservers.add(observer);
        }

        public synchronized void remove(TimeBaseObs observer) {
            if (!this.mObservers.remove(observer)) {
                String str = BatteryStatsImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Removed unknown observer: ");
                stringBuilder.append(observer);
                Slog.wtf(str, stringBuilder.toString());
            }
        }

        public synchronized boolean hasObserver(TimeBaseObs observer) {
            return this.mObservers.contains(observer);
        }

        public void init(long uptime, long realtime) {
            this.mRealtime = 0;
            this.mUptime = 0;
            this.mPastUptime = 0;
            this.mPastRealtime = 0;
            this.mUptimeStart = uptime;
            this.mRealtimeStart = realtime;
            this.mUnpluggedUptime = getUptime(this.mUptimeStart);
            this.mUnpluggedRealtime = getRealtime(this.mRealtimeStart);
        }

        public void reset(long uptime, long realtime) {
            if (this.mRunning) {
                this.mUptimeStart = uptime;
                this.mRealtimeStart = realtime;
                this.mUnpluggedUptime = getUptime(uptime);
                this.mUnpluggedRealtime = getRealtime(realtime);
                return;
            }
            this.mPastUptime = 0;
            this.mPastRealtime = 0;
        }

        public long computeUptime(long curTime, int which) {
            switch (which) {
                case 0:
                    return this.mUptime + getUptime(curTime);
                case 1:
                    return getUptime(curTime);
                case 2:
                    return getUptime(curTime) - this.mUnpluggedUptime;
                default:
                    return 0;
            }
        }

        public long computeRealtime(long curTime, int which) {
            switch (which) {
                case 0:
                    return this.mRealtime + getRealtime(curTime);
                case 1:
                    return getRealtime(curTime);
                case 2:
                    return getRealtime(curTime) - this.mUnpluggedRealtime;
                default:
                    return 0;
            }
        }

        public long getUptime(long curTime) {
            long time = this.mPastUptime;
            if (this.mRunning) {
                return time + (curTime - this.mUptimeStart);
            }
            return time;
        }

        public long getRealtime(long curTime) {
            long time = this.mPastRealtime;
            if (this.mRunning) {
                return time + (curTime - this.mRealtimeStart);
            }
            return time;
        }

        public long getUptimeStart() {
            return this.mUptimeStart;
        }

        public long getRealtimeStart() {
            return this.mRealtimeStart;
        }

        public boolean isRunning() {
            return this.mRunning;
        }

        /* JADX WARNING: Missing block: B:16:0x0068, code skipped:
            return true;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public synchronized boolean setRunning(boolean running, long uptime, long realtime) {
            if (this.mRunning == running) {
                return false;
            }
            this.mRunning = running;
            long batteryRealtime;
            int i;
            if (running) {
                this.mUptimeStart = uptime;
                this.mRealtimeStart = realtime;
                long batteryUptime = getUptime(uptime);
                this.mUnpluggedUptime = batteryUptime;
                batteryRealtime = getRealtime(realtime);
                this.mUnpluggedRealtime = batteryRealtime;
                i = this.mObservers.size() - 1;
                while (true) {
                    int i2 = i;
                    if (i2 < 0) {
                        break;
                    }
                    ((TimeBaseObs) this.mObservers.get(i2)).onTimeStarted(realtime, batteryUptime, batteryRealtime);
                    i = i2 - 1;
                }
            } else {
                this.mPastUptime += uptime - this.mUptimeStart;
                this.mPastRealtime += realtime - this.mRealtimeStart;
                batteryRealtime = getUptime(uptime);
                long batteryRealtime2 = getRealtime(realtime);
                for (i = this.mObservers.size() - 1; i >= 0; i--) {
                    ((TimeBaseObs) this.mObservers.get(i)).onTimeStopped(realtime, batteryRealtime, batteryRealtime2);
                }
            }
        }

        public void readSummaryFromParcel(Parcel in) {
            this.mUptime = in.readLong();
            this.mRealtime = in.readLong();
        }

        public void writeSummaryToParcel(Parcel out, long uptime, long realtime) {
            out.writeLong(computeUptime(uptime, 0));
            out.writeLong(computeRealtime(realtime, 0));
        }

        public void readFromParcel(Parcel in) {
            this.mRunning = false;
            this.mUptime = in.readLong();
            this.mPastUptime = in.readLong();
            this.mUptimeStart = in.readLong();
            this.mRealtime = in.readLong();
            this.mPastRealtime = in.readLong();
            this.mRealtimeStart = in.readLong();
            this.mUnpluggedUptime = in.readLong();
            this.mUnpluggedRealtime = in.readLong();
        }

        public void writeToParcel(Parcel out, long uptime, long realtime) {
            long runningUptime = getUptime(uptime);
            long runningRealtime = getRealtime(realtime);
            out.writeLong(this.mUptime);
            out.writeLong(runningUptime);
            out.writeLong(this.mUptimeStart);
            out.writeLong(this.mRealtime);
            out.writeLong(runningRealtime);
            out.writeLong(this.mRealtimeStart);
            out.writeLong(this.mUnpluggedUptime);
            out.writeLong(this.mUnpluggedRealtime);
        }
    }

    public interface TimeBaseObs {
        void onTimeStarted(long j, long j2, long j3);

        void onTimeStopped(long j, long j2, long j3);
    }

    @VisibleForTesting
    public final class UidToRemove {
        int endUid;
        int startUid;
        long timeAddedInQueue;

        public UidToRemove(BatteryStatsImpl this$0, int uid, long timestamp) {
            this(uid, uid, timestamp);
        }

        public UidToRemove(int startUid, int endUid, long timestamp) {
            this.startUid = startUid;
            this.endUid = endUid;
            this.timeAddedInQueue = timestamp;
        }

        void remove() {
            if (this.startUid == this.endUid) {
                BatteryStatsImpl.this.mKernelUidCpuTimeReader.removeUid(this.startUid);
                BatteryStatsImpl.this.mKernelUidCpuFreqTimeReader.removeUid(this.startUid);
                if (BatteryStatsImpl.this.mConstants.TRACK_CPU_ACTIVE_CLUSTER_TIME) {
                    BatteryStatsImpl.this.mKernelUidCpuActiveTimeReader.removeUid(this.startUid);
                    BatteryStatsImpl.this.mKernelUidCpuClusterTimeReader.removeUid(this.startUid);
                }
                if (BatteryStatsImpl.this.mKernelSingleUidTimeReader != null) {
                    BatteryStatsImpl.this.mKernelSingleUidTimeReader.removeUid(this.startUid);
                }
                BatteryStatsImpl.this.mNumUidsRemoved = BatteryStatsImpl.this.mNumUidsRemoved + 1;
            } else if (this.startUid < this.endUid) {
                BatteryStatsImpl.this.mKernelUidCpuFreqTimeReader.removeUidsInRange(this.startUid, this.endUid);
                BatteryStatsImpl.this.mKernelUidCpuTimeReader.removeUidsInRange(this.startUid, this.endUid);
                if (BatteryStatsImpl.this.mConstants.TRACK_CPU_ACTIVE_CLUSTER_TIME) {
                    BatteryStatsImpl.this.mKernelUidCpuActiveTimeReader.removeUidsInRange(this.startUid, this.endUid);
                    BatteryStatsImpl.this.mKernelUidCpuClusterTimeReader.removeUidsInRange(this.startUid, this.endUid);
                }
                if (BatteryStatsImpl.this.mKernelSingleUidTimeReader != null) {
                    BatteryStatsImpl.this.mKernelSingleUidTimeReader.removeUidsInRange(this.startUid, this.endUid);
                }
                BatteryStatsImpl.this.mNumUidsRemoved = BatteryStatsImpl.this.mNumUidsRemoved + 1;
            } else {
                String str = BatteryStatsImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("End UID ");
                stringBuilder.append(this.endUid);
                stringBuilder.append(" is smaller than start UID ");
                stringBuilder.append(this.startUid);
                Slog.w(str, stringBuilder.toString());
            }
        }
    }

    public static abstract class UserInfoProvider {
        private int[] userIds;

        protected abstract int[] getUserIds();

        @VisibleForTesting
        public final void refreshUserIds() {
            this.userIds = getUserIds();
        }

        @VisibleForTesting
        public boolean exists(int userId) {
            return this.userIds != null ? ArrayUtils.contains(this.userIds, userId) : true;
        }
    }

    @VisibleForTesting
    public final class Constants extends ContentObserver {
        private static final long DEFAULT_BATTERY_LEVEL_COLLECTION_DELAY_MS = 300000;
        private static final long DEFAULT_EXTERNAL_STATS_COLLECTION_RATE_LIMIT_MS = 600000;
        private static final long DEFAULT_KERNEL_UID_READERS_THROTTLE_TIME = 10000;
        private static final long DEFAULT_PROC_STATE_CPU_TIMES_READ_DELAY_MS = 5000;
        private static final boolean DEFAULT_TRACK_CPU_ACTIVE_CLUSTER_TIME = true;
        private static final boolean DEFAULT_TRACK_CPU_TIMES_BY_PROC_STATE = true;
        private static final long DEFAULT_UID_REMOVE_DELAY_MS = 300000;
        public static final String KEY_BATTERY_LEVEL_COLLECTION_DELAY_MS = "battery_level_collection_delay_ms";
        public static final String KEY_EXTERNAL_STATS_COLLECTION_RATE_LIMIT_MS = "external_stats_collection_rate_limit_ms";
        public static final String KEY_KERNEL_UID_READERS_THROTTLE_TIME = "kernel_uid_readers_throttle_time";
        public static final String KEY_PROC_STATE_CPU_TIMES_READ_DELAY_MS = "proc_state_cpu_times_read_delay_ms";
        public static final String KEY_TRACK_CPU_ACTIVE_CLUSTER_TIME = "track_cpu_active_cluster_time";
        public static final String KEY_TRACK_CPU_TIMES_BY_PROC_STATE = "track_cpu_times_by_proc_state";
        public static final String KEY_UID_REMOVE_DELAY_MS = "uid_remove_delay_ms";
        public long BATTERY_LEVEL_COLLECTION_DELAY_MS = 300000;
        public long EXTERNAL_STATS_COLLECTION_RATE_LIMIT_MS = DEFAULT_EXTERNAL_STATS_COLLECTION_RATE_LIMIT_MS;
        public long KERNEL_UID_READERS_THROTTLE_TIME = DEFAULT_KERNEL_UID_READERS_THROTTLE_TIME;
        public long PROC_STATE_CPU_TIMES_READ_DELAY_MS = DEFAULT_PROC_STATE_CPU_TIMES_READ_DELAY_MS;
        public boolean TRACK_CPU_ACTIVE_CLUSTER_TIME = true;
        public boolean TRACK_CPU_TIMES_BY_PROC_STATE = true;
        public long UID_REMOVE_DELAY_MS = 300000;
        private final KeyValueListParser mParser = new KeyValueListParser(',');
        private ContentResolver mResolver;

        public Constants(Handler handler) {
            super(handler);
        }

        public void startObserving(ContentResolver resolver) {
            this.mResolver = resolver;
            this.mResolver.registerContentObserver(Global.getUriFor("battery_stats_constants"), false, this);
            updateConstants();
        }

        public void onChange(boolean selfChange, Uri uri) {
            updateConstants();
        }

        private void updateConstants() {
            synchronized (BatteryStatsImpl.this) {
                try {
                    this.mParser.setString(Global.getString(this.mResolver, "battery_stats_constants"));
                } catch (IllegalArgumentException e) {
                    Slog.e(BatteryStatsImpl.TAG, "Bad batterystats settings", e);
                }
                updateTrackCpuTimesByProcStateLocked(this.TRACK_CPU_TIMES_BY_PROC_STATE, this.mParser.getBoolean(KEY_TRACK_CPU_TIMES_BY_PROC_STATE, true));
                this.TRACK_CPU_ACTIVE_CLUSTER_TIME = this.mParser.getBoolean(KEY_TRACK_CPU_ACTIVE_CLUSTER_TIME, true);
                updateProcStateCpuTimesReadDelayMs(this.PROC_STATE_CPU_TIMES_READ_DELAY_MS, this.mParser.getLong(KEY_PROC_STATE_CPU_TIMES_READ_DELAY_MS, DEFAULT_PROC_STATE_CPU_TIMES_READ_DELAY_MS));
                updateKernelUidReadersThrottleTime(this.KERNEL_UID_READERS_THROTTLE_TIME, this.mParser.getLong(KEY_KERNEL_UID_READERS_THROTTLE_TIME, DEFAULT_KERNEL_UID_READERS_THROTTLE_TIME));
                updateUidRemoveDelay(this.mParser.getLong(KEY_UID_REMOVE_DELAY_MS, 300000));
                this.EXTERNAL_STATS_COLLECTION_RATE_LIMIT_MS = this.mParser.getLong(KEY_EXTERNAL_STATS_COLLECTION_RATE_LIMIT_MS, DEFAULT_EXTERNAL_STATS_COLLECTION_RATE_LIMIT_MS);
                this.BATTERY_LEVEL_COLLECTION_DELAY_MS = this.mParser.getLong(KEY_BATTERY_LEVEL_COLLECTION_DELAY_MS, 300000);
            }
        }

        private void updateTrackCpuTimesByProcStateLocked(boolean wasEnabled, boolean isEnabled) {
            this.TRACK_CPU_TIMES_BY_PROC_STATE = isEnabled;
            if (isEnabled && !wasEnabled) {
                BatteryStatsImpl.this.mKernelSingleUidTimeReader.markDataAsStale(true);
                BatteryStatsImpl.this.mExternalSync.scheduleCpuSyncDueToSettingChange();
                BatteryStatsImpl.this.mNumSingleUidCpuTimeReads = 0;
                BatteryStatsImpl.this.mNumBatchedSingleUidCpuTimeReads = 0;
                BatteryStatsImpl.this.mCpuTimeReadsTrackingStartTime = BatteryStatsImpl.this.mClocks.uptimeMillis();
            }
        }

        private void updateProcStateCpuTimesReadDelayMs(long oldDelayMillis, long newDelayMillis) {
            this.PROC_STATE_CPU_TIMES_READ_DELAY_MS = newDelayMillis;
            if (oldDelayMillis != newDelayMillis) {
                BatteryStatsImpl.this.mNumSingleUidCpuTimeReads = 0;
                BatteryStatsImpl.this.mNumBatchedSingleUidCpuTimeReads = 0;
                BatteryStatsImpl.this.mCpuTimeReadsTrackingStartTime = BatteryStatsImpl.this.mClocks.uptimeMillis();
            }
        }

        private void updateKernelUidReadersThrottleTime(long oldTimeMs, long newTimeMs) {
            this.KERNEL_UID_READERS_THROTTLE_TIME = newTimeMs;
            if (oldTimeMs != newTimeMs) {
                BatteryStatsImpl.this.mKernelUidCpuTimeReader.setThrottleInterval(this.KERNEL_UID_READERS_THROTTLE_TIME);
                BatteryStatsImpl.this.mKernelUidCpuFreqTimeReader.setThrottleInterval(this.KERNEL_UID_READERS_THROTTLE_TIME);
                BatteryStatsImpl.this.mKernelUidCpuActiveTimeReader.setThrottleInterval(this.KERNEL_UID_READERS_THROTTLE_TIME);
                BatteryStatsImpl.this.mKernelUidCpuClusterTimeReader.setThrottleInterval(this.KERNEL_UID_READERS_THROTTLE_TIME);
            }
        }

        private void updateUidRemoveDelay(long newTimeMs) {
            this.UID_REMOVE_DELAY_MS = newTimeMs;
            BatteryStatsImpl.this.clearPendingRemovedUids();
        }

        public void dumpLocked(PrintWriter pw) {
            pw.print(KEY_TRACK_CPU_TIMES_BY_PROC_STATE);
            pw.print("=");
            pw.println(this.TRACK_CPU_TIMES_BY_PROC_STATE);
            pw.print(KEY_TRACK_CPU_ACTIVE_CLUSTER_TIME);
            pw.print("=");
            pw.println(this.TRACK_CPU_ACTIVE_CLUSTER_TIME);
            pw.print(KEY_PROC_STATE_CPU_TIMES_READ_DELAY_MS);
            pw.print("=");
            pw.println(this.PROC_STATE_CPU_TIMES_READ_DELAY_MS);
            pw.print(KEY_KERNEL_UID_READERS_THROTTLE_TIME);
            pw.print("=");
            pw.println(this.KERNEL_UID_READERS_THROTTLE_TIME);
            pw.print(KEY_EXTERNAL_STATS_COLLECTION_RATE_LIMIT_MS);
            pw.print("=");
            pw.println(this.EXTERNAL_STATS_COLLECTION_RATE_LIMIT_MS);
            pw.print(KEY_BATTERY_LEVEL_COLLECTION_DELAY_MS);
            pw.print("=");
            pw.println(this.BATTERY_LEVEL_COLLECTION_DELAY_MS);
        }
    }

    public static class ControllerActivityCounterImpl extends ControllerActivityCounter implements Parcelable {
        private final LongSamplingCounter mIdleTimeMillis;
        private final LongSamplingCounter mPowerDrainMaMs;
        private final LongSamplingCounter mRxTimeMillis;
        private final LongSamplingCounter mScanTimeMillis;
        private final LongSamplingCounter mSleepTimeMillis;
        private final LongSamplingCounter[] mTxTimeMillis;

        public ControllerActivityCounterImpl(TimeBase timeBase, int numTxStates) {
            this.mIdleTimeMillis = new LongSamplingCounter(timeBase);
            this.mScanTimeMillis = new LongSamplingCounter(timeBase);
            this.mSleepTimeMillis = new LongSamplingCounter(timeBase);
            this.mRxTimeMillis = new LongSamplingCounter(timeBase);
            this.mTxTimeMillis = new LongSamplingCounter[numTxStates];
            for (int i = 0; i < numTxStates; i++) {
                this.mTxTimeMillis[i] = new LongSamplingCounter(timeBase);
            }
            this.mPowerDrainMaMs = new LongSamplingCounter(timeBase);
        }

        public ControllerActivityCounterImpl(TimeBase timeBase, int numTxStates, Parcel in) {
            this.mIdleTimeMillis = new LongSamplingCounter(timeBase, in);
            this.mScanTimeMillis = new LongSamplingCounter(timeBase, in);
            this.mSleepTimeMillis = new LongSamplingCounter(timeBase, in);
            this.mRxTimeMillis = new LongSamplingCounter(timeBase, in);
            if (in.readInt() == numTxStates) {
                this.mTxTimeMillis = new LongSamplingCounter[numTxStates];
                for (int i = 0; i < numTxStates; i++) {
                    this.mTxTimeMillis[i] = new LongSamplingCounter(timeBase, in);
                }
                this.mPowerDrainMaMs = new LongSamplingCounter(timeBase, in);
                return;
            }
            throw new ParcelFormatException("inconsistent tx state lengths");
        }

        public void readSummaryFromParcel(Parcel in) {
            this.mIdleTimeMillis.readSummaryFromParcelLocked(in);
            this.mScanTimeMillis.readSummaryFromParcelLocked(in);
            this.mSleepTimeMillis.readSummaryFromParcelLocked(in);
            this.mRxTimeMillis.readSummaryFromParcelLocked(in);
            if (in.readInt() == this.mTxTimeMillis.length) {
                for (LongSamplingCounter counter : this.mTxTimeMillis) {
                    counter.readSummaryFromParcelLocked(in);
                }
                this.mPowerDrainMaMs.readSummaryFromParcelLocked(in);
                return;
            }
            throw new ParcelFormatException("inconsistent tx state lengths");
        }

        public int describeContents() {
            return 0;
        }

        public void writeSummaryToParcel(Parcel dest) {
            this.mIdleTimeMillis.writeSummaryFromParcelLocked(dest);
            this.mScanTimeMillis.writeSummaryFromParcelLocked(dest);
            this.mSleepTimeMillis.writeSummaryFromParcelLocked(dest);
            this.mRxTimeMillis.writeSummaryFromParcelLocked(dest);
            dest.writeInt(this.mTxTimeMillis.length);
            for (LongSamplingCounter counter : this.mTxTimeMillis) {
                counter.writeSummaryFromParcelLocked(dest);
            }
            this.mPowerDrainMaMs.writeSummaryFromParcelLocked(dest);
        }

        public void writeToParcel(Parcel dest, int flags) {
            this.mIdleTimeMillis.writeToParcel(dest);
            this.mScanTimeMillis.writeToParcel(dest);
            this.mSleepTimeMillis.writeToParcel(dest);
            this.mRxTimeMillis.writeToParcel(dest);
            dest.writeInt(this.mTxTimeMillis.length);
            for (LongSamplingCounter counter : this.mTxTimeMillis) {
                counter.writeToParcel(dest);
            }
            this.mPowerDrainMaMs.writeToParcel(dest);
        }

        public void reset(boolean detachIfReset) {
            this.mIdleTimeMillis.reset(detachIfReset);
            this.mScanTimeMillis.reset(detachIfReset);
            this.mSleepTimeMillis.reset(detachIfReset);
            this.mRxTimeMillis.reset(detachIfReset);
            for (LongSamplingCounter counter : this.mTxTimeMillis) {
                counter.reset(detachIfReset);
            }
            this.mPowerDrainMaMs.reset(detachIfReset);
        }

        public void detach() {
            this.mIdleTimeMillis.detach();
            this.mScanTimeMillis.detach();
            this.mSleepTimeMillis.detach();
            this.mRxTimeMillis.detach();
            for (LongSamplingCounter counter : this.mTxTimeMillis) {
                counter.detach();
            }
            this.mPowerDrainMaMs.detach();
        }

        public LongSamplingCounter getIdleTimeCounter() {
            return this.mIdleTimeMillis;
        }

        public LongSamplingCounter getScanTimeCounter() {
            return this.mScanTimeMillis;
        }

        public LongSamplingCounter getSleepTimeCounter() {
            return this.mSleepTimeMillis;
        }

        public LongSamplingCounter getRxTimeCounter() {
            return this.mRxTimeMillis;
        }

        public LongSamplingCounter[] getTxTimeCounters() {
            return this.mTxTimeMillis;
        }

        public LongSamplingCounter getPowerCounter() {
            return this.mPowerDrainMaMs;
        }
    }

    public static class Counter extends android.os.BatteryStats.Counter implements TimeBaseObs {
        final AtomicInteger mCount = new AtomicInteger();
        int mLoadedCount;
        int mPluggedCount;
        final TimeBase mTimeBase;
        int mUnpluggedCount;

        public Counter(TimeBase timeBase, Parcel in) {
            this.mTimeBase = timeBase;
            this.mPluggedCount = in.readInt();
            this.mCount.set(this.mPluggedCount);
            this.mLoadedCount = in.readInt();
            this.mUnpluggedCount = in.readInt();
            timeBase.add(this);
        }

        public Counter(TimeBase timeBase) {
            this.mTimeBase = timeBase;
            timeBase.add(this);
        }

        public void writeToParcel(Parcel out) {
            out.writeInt(this.mCount.get());
            out.writeInt(this.mLoadedCount);
            out.writeInt(this.mUnpluggedCount);
        }

        public void onTimeStarted(long elapsedRealtime, long baseUptime, long baseRealtime) {
            this.mUnpluggedCount = this.mPluggedCount;
        }

        public void onTimeStopped(long elapsedRealtime, long baseUptime, long baseRealtime) {
            this.mPluggedCount = this.mCount.get();
        }

        public static void writeCounterToParcel(Parcel out, Counter counter) {
            if (counter == null) {
                out.writeInt(0);
                return;
            }
            out.writeInt(1);
            counter.writeToParcel(out);
        }

        public static Counter readCounterFromParcel(TimeBase timeBase, Parcel in) {
            if (in.readInt() == 0) {
                return null;
            }
            return new Counter(timeBase, in);
        }

        public int getCountLocked(int which) {
            int val = this.mCount.get();
            if (which == 2) {
                return val - this.mUnpluggedCount;
            }
            if (which != 0) {
                return val - this.mLoadedCount;
            }
            return val;
        }

        public void logState(Printer pw, String prefix) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mCount=");
            stringBuilder.append(this.mCount.get());
            stringBuilder.append(" mLoadedCount=");
            stringBuilder.append(this.mLoadedCount);
            stringBuilder.append(" mUnpluggedCount=");
            stringBuilder.append(this.mUnpluggedCount);
            stringBuilder.append(" mPluggedCount=");
            stringBuilder.append(this.mPluggedCount);
            pw.println(stringBuilder.toString());
        }

        @VisibleForTesting(visibility = Visibility.PACKAGE)
        public void stepAtomic() {
            if (this.mTimeBase.isRunning()) {
                this.mCount.incrementAndGet();
            }
        }

        void addAtomic(int delta) {
            if (this.mTimeBase.isRunning()) {
                this.mCount.addAndGet(delta);
            }
        }

        void reset(boolean detachIfReset) {
            this.mCount.set(0);
            this.mUnpluggedCount = 0;
            this.mPluggedCount = 0;
            this.mLoadedCount = 0;
            if (detachIfReset) {
                detach();
            }
        }

        void detach() {
            this.mTimeBase.remove(this);
        }

        @VisibleForTesting(visibility = Visibility.PACKAGE)
        public void writeSummaryFromParcelLocked(Parcel out) {
            out.writeInt(this.mCount.get());
        }

        @VisibleForTesting(visibility = Visibility.PACKAGE)
        public void readSummaryFromParcelLocked(Parcel in) {
            this.mLoadedCount = in.readInt();
            this.mCount.set(this.mLoadedCount);
            int i = this.mLoadedCount;
            this.mPluggedCount = i;
            this.mUnpluggedCount = i;
        }
    }

    @VisibleForTesting
    public static class LongSamplingCounter extends LongCounter implements TimeBaseObs {
        public long mCount;
        public long mCurrentCount;
        public long mLoadedCount;
        final TimeBase mTimeBase;
        public long mUnpluggedCount;

        public LongSamplingCounter(TimeBase timeBase, Parcel in) {
            this.mTimeBase = timeBase;
            this.mCount = in.readLong();
            this.mCurrentCount = in.readLong();
            this.mLoadedCount = in.readLong();
            this.mUnpluggedCount = in.readLong();
            timeBase.add(this);
        }

        public LongSamplingCounter(TimeBase timeBase) {
            this.mTimeBase = timeBase;
            timeBase.add(this);
        }

        public void writeToParcel(Parcel out) {
            out.writeLong(this.mCount);
            out.writeLong(this.mCurrentCount);
            out.writeLong(this.mLoadedCount);
            out.writeLong(this.mUnpluggedCount);
        }

        public void onTimeStarted(long elapsedRealtime, long baseUptime, long baseRealtime) {
            this.mUnpluggedCount = this.mCount;
        }

        public void onTimeStopped(long elapsedRealtime, long baseUptime, long baseRealtime) {
        }

        public long getCountLocked(int which) {
            long val = this.mCount;
            if (which == 2) {
                return val - this.mUnpluggedCount;
            }
            if (which != 0) {
                return val - this.mLoadedCount;
            }
            return val;
        }

        public void logState(Printer pw, String prefix) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mCount=");
            stringBuilder.append(this.mCount);
            stringBuilder.append(" mCurrentCount=");
            stringBuilder.append(this.mCurrentCount);
            stringBuilder.append(" mLoadedCount=");
            stringBuilder.append(this.mLoadedCount);
            stringBuilder.append(" mUnpluggedCount=");
            stringBuilder.append(this.mUnpluggedCount);
            pw.println(stringBuilder.toString());
        }

        public void addCountLocked(long count) {
            update(this.mCurrentCount + count, this.mTimeBase.isRunning());
        }

        public void addCountLocked(long count, boolean isRunning) {
            update(this.mCurrentCount + count, isRunning);
        }

        public void update(long count) {
            update(count, this.mTimeBase.isRunning());
        }

        public void update(long count, boolean isRunning) {
            if (count < this.mCurrentCount) {
                this.mCurrentCount = 0;
            }
            if (isRunning) {
                this.mCount += count - this.mCurrentCount;
            }
            this.mCurrentCount = count;
        }

        public void reset(boolean detachIfReset) {
            this.mCount = 0;
            this.mUnpluggedCount = 0;
            this.mLoadedCount = 0;
            if (detachIfReset) {
                detach();
            }
        }

        public void detach() {
            this.mTimeBase.remove(this);
        }

        public void writeSummaryFromParcelLocked(Parcel out) {
            out.writeLong(this.mCount);
        }

        public void readSummaryFromParcelLocked(Parcel in) {
            long readLong = in.readLong();
            this.mLoadedCount = readLong;
            this.mUnpluggedCount = readLong;
            this.mCount = readLong;
        }
    }

    @VisibleForTesting
    public static class LongSamplingCounterArray extends LongCounterArray implements TimeBaseObs {
        public long[] mCounts;
        public long[] mLoadedCounts;
        final TimeBase mTimeBase;
        public long[] mUnpluggedCounts;

        /* synthetic */ LongSamplingCounterArray(TimeBase x0, Parcel x1, AnonymousClass1 x2) {
            this(x0, x1);
        }

        private LongSamplingCounterArray(TimeBase timeBase, Parcel in) {
            this.mTimeBase = timeBase;
            this.mCounts = in.createLongArray();
            this.mLoadedCounts = in.createLongArray();
            this.mUnpluggedCounts = in.createLongArray();
            timeBase.add(this);
        }

        public LongSamplingCounterArray(TimeBase timeBase) {
            this.mTimeBase = timeBase;
            timeBase.add(this);
        }

        private void writeToParcel(Parcel out) {
            out.writeLongArray(this.mCounts);
            out.writeLongArray(this.mLoadedCounts);
            out.writeLongArray(this.mUnpluggedCounts);
        }

        public void onTimeStarted(long elapsedRealTime, long baseUptime, long baseRealtime) {
            this.mUnpluggedCounts = copyArray(this.mCounts, this.mUnpluggedCounts);
        }

        public void onTimeStopped(long elapsedRealtime, long baseUptime, long baseRealtime) {
        }

        public long[] getCountsLocked(int which) {
            long[] val = copyArray(this.mCounts, null);
            if (which == 2) {
                subtract(val, this.mUnpluggedCounts);
            } else if (which != 0) {
                subtract(val, this.mLoadedCounts);
            }
            return val;
        }

        public void logState(Printer pw, String prefix) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mCounts=");
            stringBuilder.append(Arrays.toString(this.mCounts));
            stringBuilder.append(" mLoadedCounts=");
            stringBuilder.append(Arrays.toString(this.mLoadedCounts));
            stringBuilder.append(" mUnpluggedCounts=");
            stringBuilder.append(Arrays.toString(this.mUnpluggedCounts));
            pw.println(stringBuilder.toString());
        }

        public void addCountLocked(long[] counts) {
            addCountLocked(counts, this.mTimeBase.isRunning());
        }

        public void addCountLocked(long[] counts, boolean isRunning) {
            if (counts != null && isRunning) {
                if (this.mCounts == null) {
                    this.mCounts = new long[counts.length];
                }
                for (int i = 0; i < counts.length; i++) {
                    long[] jArr = this.mCounts;
                    jArr[i] = jArr[i] + counts[i];
                }
            }
        }

        public int getSize() {
            return this.mCounts == null ? 0 : this.mCounts.length;
        }

        public void reset(boolean detachIfReset) {
            fillArray(this.mCounts, 0);
            fillArray(this.mLoadedCounts, 0);
            fillArray(this.mUnpluggedCounts, 0);
            if (detachIfReset) {
                detach();
            }
        }

        public void detach() {
            this.mTimeBase.remove(this);
        }

        private void writeSummaryToParcelLocked(Parcel out) {
            out.writeLongArray(this.mCounts);
        }

        private void readSummaryFromParcelLocked(Parcel in) {
            this.mCounts = in.createLongArray();
            this.mLoadedCounts = copyArray(this.mCounts, this.mLoadedCounts);
            this.mUnpluggedCounts = copyArray(this.mCounts, this.mUnpluggedCounts);
        }

        public static void writeToParcel(Parcel out, LongSamplingCounterArray counterArray) {
            if (counterArray != null) {
                out.writeInt(1);
                counterArray.writeToParcel(out);
                return;
            }
            out.writeInt(0);
        }

        public static LongSamplingCounterArray readFromParcel(Parcel in, TimeBase timeBase) {
            if (in.readInt() != 0) {
                return new LongSamplingCounterArray(timeBase, in);
            }
            return null;
        }

        public static void writeSummaryToParcelLocked(Parcel out, LongSamplingCounterArray counterArray) {
            if (counterArray != null) {
                out.writeInt(1);
                counterArray.writeSummaryToParcelLocked(out);
                return;
            }
            out.writeInt(0);
        }

        public static LongSamplingCounterArray readSummaryFromParcelLocked(Parcel in, TimeBase timeBase) {
            if (in.readInt() == 0) {
                return null;
            }
            LongSamplingCounterArray counterArray = new LongSamplingCounterArray(timeBase);
            counterArray.readSummaryFromParcelLocked(in);
            return counterArray;
        }

        private static void fillArray(long[] a, long val) {
            if (a != null) {
                Arrays.fill(a, val);
            }
        }

        private static void subtract(long[] val, long[] toSubtract) {
            if (toSubtract != null) {
                for (int i = 0; i < val.length; i++) {
                    val[i] = val[i] - toSubtract[i];
                }
            }
        }

        private static long[] copyArray(long[] src, long[] dest) {
            if (src == null) {
                return null;
            }
            Object dest2;
            if (dest2 == null) {
                dest2 = new long[src.length];
            }
            System.arraycopy(src, 0, dest2, 0, src.length);
            return dest2;
        }
    }

    final class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            BatteryCallback cb = BatteryStatsImpl.this.mCallback;
            switch (msg.what) {
                case 1:
                    if (cb != null) {
                        cb.batteryNeedsCpuUpdate();
                        return;
                    }
                    return;
                case 2:
                    if (cb != null) {
                        cb.batteryPowerChanged(msg.arg1 != 0);
                        return;
                    }
                    return;
                case 3:
                    if (cb != null) {
                        String action;
                        synchronized (BatteryStatsImpl.this) {
                            if (BatteryStatsImpl.this.mCharging) {
                                action = "android.os.action.CHARGING";
                            } else {
                                action = "android.os.action.DISCHARGING";
                            }
                        }
                        Intent intent = new Intent(action);
                        intent.addFlags(67108864);
                        cb.batterySendBroadcast(intent);
                        return;
                    }
                    return;
                case 4:
                    if (cb != null) {
                        cb.batteryStatsReset();
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    public static class SystemClocks implements Clocks {
        public long elapsedRealtime() {
            return SystemClock.elapsedRealtime();
        }

        public long uptimeMillis() {
            return SystemClock.uptimeMillis();
        }
    }

    public static abstract class Timer extends android.os.BatteryStats.Timer implements TimeBaseObs {
        protected final Clocks mClocks;
        protected int mCount;
        protected int mLastCount;
        protected long mLastTime;
        protected int mLoadedCount;
        protected long mLoadedTime;
        protected final TimeBase mTimeBase;
        protected long mTimeBeforeMark;
        protected long mTotalTime;
        protected final int mType;
        protected int mUnpluggedCount;
        protected long mUnpluggedTime;

        protected abstract int computeCurrentCountLocked();

        protected abstract long computeRunTimeLocked(long j);

        public Timer(Clocks clocks, int type, TimeBase timeBase, Parcel in) {
            this.mClocks = clocks;
            this.mType = type;
            this.mTimeBase = timeBase;
            this.mCount = in.readInt();
            this.mLoadedCount = in.readInt();
            this.mLastCount = 0;
            this.mUnpluggedCount = in.readInt();
            this.mTotalTime = in.readLong();
            this.mLoadedTime = in.readLong();
            this.mLastTime = 0;
            this.mUnpluggedTime = in.readLong();
            this.mTimeBeforeMark = in.readLong();
            timeBase.add(this);
        }

        public Timer(Clocks clocks, int type, TimeBase timeBase) {
            this.mClocks = clocks;
            this.mType = type;
            this.mTimeBase = timeBase;
            timeBase.add(this);
        }

        public boolean reset(boolean detachIfReset) {
            this.mTimeBeforeMark = 0;
            this.mLastTime = 0;
            this.mLoadedTime = 0;
            this.mTotalTime = 0;
            this.mLastCount = 0;
            this.mLoadedCount = 0;
            this.mCount = 0;
            if (detachIfReset) {
                detach();
            }
            return true;
        }

        public void detach() {
            this.mTimeBase.remove(this);
        }

        public void writeToParcel(Parcel out, long elapsedRealtimeUs) {
            if (this.mTimeBase == null) {
                Slog.w(BatteryStatsImpl.TAG, "writeToParcel, mTimeBase is not exit");
                return;
            }
            out.writeInt(computeCurrentCountLocked());
            out.writeInt(this.mLoadedCount);
            out.writeInt(this.mUnpluggedCount);
            out.writeLong(computeRunTimeLocked(this.mTimeBase.getRealtime(elapsedRealtimeUs)));
            out.writeLong(this.mLoadedTime);
            out.writeLong(this.mUnpluggedTime);
            out.writeLong(this.mTimeBeforeMark);
        }

        public void onTimeStarted(long elapsedRealtime, long timeBaseUptime, long baseRealtime) {
            this.mUnpluggedTime = computeRunTimeLocked(baseRealtime);
            this.mUnpluggedCount = computeCurrentCountLocked();
        }

        public void onTimeStopped(long elapsedRealtime, long baseUptime, long baseRealtime) {
            this.mTotalTime = computeRunTimeLocked(baseRealtime);
            this.mCount = computeCurrentCountLocked();
        }

        public static void writeTimerToParcel(Parcel out, Timer timer, long elapsedRealtimeUs) {
            if (timer == null) {
                out.writeInt(0);
                return;
            }
            out.writeInt(1);
            timer.writeToParcel(out, elapsedRealtimeUs);
        }

        public long getTotalTimeLocked(long elapsedRealtimeUs, int which) {
            long val = computeRunTimeLocked(this.mTimeBase.getRealtime(elapsedRealtimeUs));
            if (which == 2) {
                return val - this.mUnpluggedTime;
            }
            if (which != 0) {
                return val - this.mLoadedTime;
            }
            return val;
        }

        public int getCountLocked(int which) {
            int val = computeCurrentCountLocked();
            if (which == 2) {
                return val - this.mUnpluggedCount;
            }
            if (which != 0) {
                return val - this.mLoadedCount;
            }
            return val;
        }

        public long getTimeSinceMarkLocked(long elapsedRealtimeUs) {
            return computeRunTimeLocked(this.mTimeBase.getRealtime(elapsedRealtimeUs)) - this.mTimeBeforeMark;
        }

        public void logState(Printer pw, String prefix) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mCount=");
            stringBuilder.append(this.mCount);
            stringBuilder.append(" mLoadedCount=");
            stringBuilder.append(this.mLoadedCount);
            stringBuilder.append(" mLastCount=");
            stringBuilder.append(this.mLastCount);
            stringBuilder.append(" mUnpluggedCount=");
            stringBuilder.append(this.mUnpluggedCount);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mTotalTime=");
            stringBuilder.append(this.mTotalTime);
            stringBuilder.append(" mLoadedTime=");
            stringBuilder.append(this.mLoadedTime);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mLastTime=");
            stringBuilder.append(this.mLastTime);
            stringBuilder.append(" mUnpluggedTime=");
            stringBuilder.append(this.mUnpluggedTime);
            pw.println(stringBuilder.toString());
        }

        public void writeSummaryFromParcelLocked(Parcel out, long elapsedRealtimeUs) {
            out.writeLong(computeRunTimeLocked(this.mTimeBase.getRealtime(elapsedRealtimeUs)));
            out.writeInt(computeCurrentCountLocked());
        }

        public void readSummaryFromParcelLocked(Parcel in) {
            long readLong = in.readLong();
            this.mLoadedTime = readLong;
            this.mTotalTime = readLong;
            this.mLastTime = 0;
            this.mUnpluggedTime = this.mTotalTime;
            int readInt = in.readInt();
            this.mLoadedCount = readInt;
            this.mCount = readInt;
            this.mLastCount = 0;
            this.mUnpluggedCount = this.mCount;
            this.mTimeBeforeMark = this.mTotalTime;
        }
    }

    public static class Uid extends android.os.BatteryStats.Uid {
        static final int NO_BATCHED_SCAN_STARTED = -1;
        DualTimer mAggregatedPartialWakelockTimer;
        StopwatchTimer mAudioTurnedOnTimer;
        private ControllerActivityCounterImpl mBluetoothControllerActivity;
        Counter mBluetoothScanResultBgCounter;
        Counter mBluetoothScanResultCounter;
        DualTimer mBluetoothScanTimer;
        DualTimer mBluetoothUnoptimizedScanTimer;
        protected BatteryStatsImpl mBsi;
        StopwatchTimer mCameraTurnedOnTimer;
        IntArray mChildUids;
        LongSamplingCounter mCpuActiveTimeMs;
        LongSamplingCounter[][] mCpuClusterSpeedTimesUs;
        LongSamplingCounterArray mCpuClusterTimesMs;
        LongSamplingCounterArray mCpuFreqTimeMs;
        long mCurStepSystemTime;
        long mCurStepUserTime;
        StopwatchTimer mFlashlightTurnedOnTimer;
        StopwatchTimer mForegroundActivityTimer;
        StopwatchTimer mForegroundServiceTimer;
        boolean mFullWifiLockOut;
        StopwatchTimer mFullWifiLockTimer;
        boolean mInForegroundService = false;
        final ArrayMap<String, SparseIntArray> mJobCompletions = new ArrayMap();
        final OverflowArrayMap<DualTimer> mJobStats;
        Counter mJobsDeferredCount;
        Counter mJobsDeferredEventCount;
        final Counter[] mJobsFreshnessBuckets;
        LongSamplingCounter mJobsFreshnessTimeMs;
        long mLastStepSystemTime;
        long mLastStepUserTime;
        LongSamplingCounter mMobileRadioActiveCount;
        LongSamplingCounter mMobileRadioActiveTime;
        private LongSamplingCounter mMobileRadioApWakeupCount;
        private ControllerActivityCounterImpl mModemControllerActivity;
        LongSamplingCounter[] mNetworkByteActivityCounters;
        LongSamplingCounter[] mNetworkPacketActivityCounters;
        @VisibleForTesting(visibility = Visibility.PACKAGE)
        public final TimeBase mOnBatteryBackgroundTimeBase;
        @VisibleForTesting(visibility = Visibility.PACKAGE)
        public final TimeBase mOnBatteryScreenOffBackgroundTimeBase;
        final ArrayMap<String, Pkg> mPackageStats = new ArrayMap();
        final SparseArray<Pid> mPids = new SparseArray();
        LongSamplingCounterArray[] mProcStateScreenOffTimeMs;
        LongSamplingCounterArray[] mProcStateTimeMs;
        int mProcessState = 19;
        StopwatchTimer[] mProcessStateTimer;
        final ArrayMap<String, Proc> mProcessStats = new ArrayMap();
        LongSamplingCounterArray mScreenOffCpuFreqTimeMs;
        final SparseArray<Sensor> mSensorStats = new SparseArray();
        final OverflowArrayMap<DualTimer> mSyncStats;
        LongSamplingCounter mSystemCpuTime;
        final int mUid;
        Counter[] mUserActivityCounters;
        LongSamplingCounter mUserCpuTime;
        BatchTimer mVibratorOnTimer;
        StopwatchTimer mVideoTurnedOnTimer;
        final OverflowArrayMap<Wakelock> mWakelockStats;
        int mWifiBatchedScanBinStarted = -1;
        StopwatchTimer[] mWifiBatchedScanTimer;
        private ControllerActivityCounterImpl mWifiControllerActivity;
        boolean mWifiMulticastEnabled;
        StopwatchTimer mWifiMulticastTimer;
        private LongSamplingCounter mWifiRadioApWakeupCount;
        boolean mWifiRunning;
        StopwatchTimer mWifiRunningTimer;
        boolean mWifiScanStarted;
        DualTimer mWifiScanTimer;

        /* renamed from: com.android.internal.os.BatteryStatsImpl$Uid$1 */
        class AnonymousClass1 extends OverflowArrayMap<Wakelock> {
            AnonymousClass1(BatteryStatsImpl x0, int uid) {
                Objects.requireNonNull(x0);
                super(uid);
            }

            public Wakelock instantiateObject() {
                return new Wakelock(Uid.this.mBsi, Uid.this);
            }
        }

        /* renamed from: com.android.internal.os.BatteryStatsImpl$Uid$2 */
        class AnonymousClass2 extends OverflowArrayMap<DualTimer> {
            AnonymousClass2(BatteryStatsImpl x0, int uid) {
                Objects.requireNonNull(x0);
                super(uid);
            }

            public DualTimer instantiateObject() {
                return new DualTimer(Uid.this.mBsi.mClocks, Uid.this, 13, null, Uid.this.mBsi.mOnBatteryTimeBase, Uid.this.mOnBatteryBackgroundTimeBase);
            }
        }

        /* renamed from: com.android.internal.os.BatteryStatsImpl$Uid$3 */
        class AnonymousClass3 extends OverflowArrayMap<DualTimer> {
            AnonymousClass3(BatteryStatsImpl x0, int uid) {
                Objects.requireNonNull(x0);
                super(uid);
            }

            public DualTimer instantiateObject() {
                return new DualTimer(Uid.this.mBsi.mClocks, Uid.this, 14, null, Uid.this.mBsi.mOnBatteryTimeBase, Uid.this.mOnBatteryBackgroundTimeBase);
            }
        }

        public static class Pkg extends android.os.BatteryStats.Uid.Pkg implements TimeBaseObs {
            protected BatteryStatsImpl mBsi;
            final ArrayMap<String, Serv> mServiceStats = new ArrayMap();
            ArrayMap<String, Counter> mWakeupAlarms = new ArrayMap();

            public static class Serv extends android.os.BatteryStats.Uid.Pkg.Serv implements TimeBaseObs {
                protected BatteryStatsImpl mBsi;
                protected int mLastLaunches;
                protected long mLastStartTime;
                protected int mLastStarts;
                protected boolean mLaunched;
                protected long mLaunchedSince;
                protected long mLaunchedTime;
                protected int mLaunches;
                protected int mLoadedLaunches;
                protected long mLoadedStartTime;
                protected int mLoadedStarts;
                protected Pkg mPkg;
                protected boolean mRunning;
                protected long mRunningSince;
                protected long mStartTime;
                protected int mStarts;
                protected int mUnpluggedLaunches;
                protected long mUnpluggedStartTime;
                protected int mUnpluggedStarts;

                public Serv(BatteryStatsImpl bsi) {
                    this.mBsi = bsi;
                    this.mBsi.mOnBatteryTimeBase.add(this);
                }

                public void onTimeStarted(long elapsedRealtime, long baseUptime, long baseRealtime) {
                    this.mUnpluggedStartTime = getStartTimeToNowLocked(baseUptime);
                    this.mUnpluggedStarts = this.mStarts;
                    this.mUnpluggedLaunches = this.mLaunches;
                }

                public void onTimeStopped(long elapsedRealtime, long baseUptime, long baseRealtime) {
                }

                public void detach() {
                    this.mBsi.mOnBatteryTimeBase.remove(this);
                }

                public void readFromParcelLocked(Parcel in) {
                    this.mStartTime = in.readLong();
                    this.mRunningSince = in.readLong();
                    boolean z = true;
                    this.mRunning = in.readInt() != 0;
                    this.mStarts = in.readInt();
                    this.mLaunchedTime = in.readLong();
                    this.mLaunchedSince = in.readLong();
                    if (in.readInt() == 0) {
                        z = false;
                    }
                    this.mLaunched = z;
                    this.mLaunches = in.readInt();
                    this.mLoadedStartTime = in.readLong();
                    this.mLoadedStarts = in.readInt();
                    this.mLoadedLaunches = in.readInt();
                    this.mLastStartTime = 0;
                    this.mLastStarts = 0;
                    this.mLastLaunches = 0;
                    this.mUnpluggedStartTime = in.readLong();
                    this.mUnpluggedStarts = in.readInt();
                    this.mUnpluggedLaunches = in.readInt();
                }

                public void writeToParcelLocked(Parcel out) {
                    out.writeLong(this.mStartTime);
                    out.writeLong(this.mRunningSince);
                    out.writeInt(this.mRunning);
                    out.writeInt(this.mStarts);
                    out.writeLong(this.mLaunchedTime);
                    out.writeLong(this.mLaunchedSince);
                    out.writeInt(this.mLaunched);
                    out.writeInt(this.mLaunches);
                    out.writeLong(this.mLoadedStartTime);
                    out.writeInt(this.mLoadedStarts);
                    out.writeInt(this.mLoadedLaunches);
                    out.writeLong(this.mUnpluggedStartTime);
                    out.writeInt(this.mUnpluggedStarts);
                    out.writeInt(this.mUnpluggedLaunches);
                }

                public long getLaunchTimeToNowLocked(long batteryUptime) {
                    if (this.mLaunched) {
                        return (this.mLaunchedTime + batteryUptime) - this.mLaunchedSince;
                    }
                    return this.mLaunchedTime;
                }

                public long getStartTimeToNowLocked(long batteryUptime) {
                    if (this.mRunning) {
                        return (this.mStartTime + batteryUptime) - this.mRunningSince;
                    }
                    return this.mStartTime;
                }

                public void startLaunchedLocked() {
                    if (!this.mLaunched) {
                        this.mLaunches++;
                        this.mLaunchedSince = this.mBsi.getBatteryUptimeLocked();
                        this.mLaunched = true;
                    }
                }

                public void stopLaunchedLocked() {
                    if (this.mLaunched) {
                        long time = this.mBsi.getBatteryUptimeLocked() - this.mLaunchedSince;
                        if (time > 0) {
                            this.mLaunchedTime += time;
                        } else {
                            this.mLaunches--;
                        }
                        this.mLaunched = false;
                    }
                }

                public void startRunningLocked() {
                    if (!this.mRunning) {
                        this.mStarts++;
                        this.mRunningSince = this.mBsi.getBatteryUptimeLocked();
                        this.mRunning = true;
                    }
                }

                public void stopRunningLocked() {
                    if (this.mRunning) {
                        long time = this.mBsi.getBatteryUptimeLocked() - this.mRunningSince;
                        if (time > 0) {
                            this.mStartTime += time;
                        } else {
                            this.mStarts--;
                        }
                        this.mRunning = false;
                    }
                }

                public BatteryStatsImpl getBatteryStats() {
                    return this.mBsi;
                }

                public int getLaunches(int which) {
                    int val = this.mLaunches;
                    if (which == 1) {
                        return val - this.mLoadedLaunches;
                    }
                    if (which == 2) {
                        return val - this.mUnpluggedLaunches;
                    }
                    return val;
                }

                public long getStartTime(long now, int which) {
                    long val = getStartTimeToNowLocked(now);
                    if (which == 1) {
                        return val - this.mLoadedStartTime;
                    }
                    if (which == 2) {
                        return val - this.mUnpluggedStartTime;
                    }
                    return val;
                }

                public int getStarts(int which) {
                    int val = this.mStarts;
                    if (which == 1) {
                        return val - this.mLoadedStarts;
                    }
                    if (which == 2) {
                        return val - this.mUnpluggedStarts;
                    }
                    return val;
                }
            }

            public Pkg(BatteryStatsImpl bsi) {
                this.mBsi = bsi;
                this.mBsi.mOnBatteryScreenOffTimeBase.add(this);
            }

            public void onTimeStarted(long elapsedRealtime, long baseUptime, long baseRealtime) {
            }

            public void onTimeStopped(long elapsedRealtime, long baseUptime, long baseRealtime) {
            }

            void detach() {
                this.mBsi.mOnBatteryScreenOffTimeBase.remove(this);
            }

            void readFromParcelLocked(Parcel in) {
                int i;
                int numWA = in.readInt();
                this.mWakeupAlarms.clear();
                int m = 0;
                for (i = 0; i < numWA; i++) {
                    this.mWakeupAlarms.put(in.readString(), new Counter(this.mBsi.mOnBatteryScreenOffTimeBase, in));
                }
                i = in.readInt();
                this.mServiceStats.clear();
                while (m < i) {
                    String serviceName = in.readString();
                    Serv serv = new Serv(this.mBsi);
                    this.mServiceStats.put(serviceName, serv);
                    serv.readFromParcelLocked(in);
                    m++;
                }
            }

            void writeToParcelLocked(Parcel out) {
                int i;
                int numWA = this.mWakeupAlarms.size();
                out.writeInt(numWA);
                int i2 = 0;
                for (i = 0; i < numWA; i++) {
                    out.writeString((String) this.mWakeupAlarms.keyAt(i));
                    ((Counter) this.mWakeupAlarms.valueAt(i)).writeToParcel(out);
                }
                i = this.mServiceStats.size();
                out.writeInt(i);
                while (i2 < i) {
                    out.writeString((String) this.mServiceStats.keyAt(i2));
                    ((Serv) this.mServiceStats.valueAt(i2)).writeToParcelLocked(out);
                    i2++;
                }
            }

            public ArrayMap<String, ? extends android.os.BatteryStats.Counter> getWakeupAlarmStats() {
                return this.mWakeupAlarms;
            }

            public void noteWakeupAlarmLocked(String tag) {
                Counter c = (Counter) this.mWakeupAlarms.get(tag);
                if (c == null) {
                    c = new Counter(this.mBsi.mOnBatteryScreenOffTimeBase);
                    this.mWakeupAlarms.put(tag, c);
                }
                c.stepAtomic();
            }

            public ArrayMap<String, ? extends android.os.BatteryStats.Uid.Pkg.Serv> getServiceStats() {
                return this.mServiceStats;
            }

            final Serv newServiceStatsLocked() {
                return new Serv(this.mBsi);
            }
        }

        public static class Proc extends android.os.BatteryStats.Uid.Proc implements TimeBaseObs {
            boolean mActive = true;
            protected BatteryStatsImpl mBsi;
            ArrayList<ExcessivePower> mExcessivePower;
            long mForegroundTime;
            long mLoadedForegroundTime;
            int mLoadedNumAnrs;
            int mLoadedNumCrashes;
            int mLoadedStarts;
            long mLoadedSystemTime;
            long mLoadedUserTime;
            final String mName;
            int mNumAnrs;
            int mNumCrashes;
            int mStarts;
            long mSystemTime;
            long mUnpluggedForegroundTime;
            int mUnpluggedNumAnrs;
            int mUnpluggedNumCrashes;
            int mUnpluggedStarts;
            long mUnpluggedSystemTime;
            long mUnpluggedUserTime;
            long mUserTime;

            public Proc(BatteryStatsImpl bsi, String name) {
                this.mBsi = bsi;
                this.mName = name;
                this.mBsi.mOnBatteryTimeBase.add(this);
            }

            public void onTimeStarted(long elapsedRealtime, long baseUptime, long baseRealtime) {
                this.mUnpluggedUserTime = this.mUserTime;
                this.mUnpluggedSystemTime = this.mSystemTime;
                this.mUnpluggedForegroundTime = this.mForegroundTime;
                this.mUnpluggedStarts = this.mStarts;
                this.mUnpluggedNumCrashes = this.mNumCrashes;
                this.mUnpluggedNumAnrs = this.mNumAnrs;
            }

            public void onTimeStopped(long elapsedRealtime, long baseUptime, long baseRealtime) {
            }

            void detach() {
                this.mActive = false;
                this.mBsi.mOnBatteryTimeBase.remove(this);
            }

            public int countExcessivePowers() {
                return this.mExcessivePower != null ? this.mExcessivePower.size() : 0;
            }

            public ExcessivePower getExcessivePower(int i) {
                if (this.mExcessivePower != null) {
                    return (ExcessivePower) this.mExcessivePower.get(i);
                }
                return null;
            }

            public void addExcessiveCpu(long overTime, long usedTime) {
                if (this.mExcessivePower == null) {
                    this.mExcessivePower = new ArrayList();
                }
                ExcessivePower ew = new ExcessivePower();
                ew.type = 2;
                ew.overTime = overTime;
                ew.usedTime = usedTime;
                this.mExcessivePower.add(ew);
            }

            void writeExcessivePowerToParcelLocked(Parcel out) {
                int i = 0;
                if (this.mExcessivePower == null) {
                    out.writeInt(0);
                    return;
                }
                int N = this.mExcessivePower.size();
                out.writeInt(N);
                while (i < N) {
                    ExcessivePower ew = (ExcessivePower) this.mExcessivePower.get(i);
                    out.writeInt(ew.type);
                    out.writeLong(ew.overTime);
                    out.writeLong(ew.usedTime);
                    i++;
                }
            }

            void readExcessivePowerFromParcelLocked(Parcel in) {
                int N = in.readInt();
                if (N == 0) {
                    this.mExcessivePower = null;
                } else if (N <= PGAction.PG_ID_DEFAULT_FRONT) {
                    this.mExcessivePower = new ArrayList();
                    for (int i = 0; i < N; i++) {
                        ExcessivePower ew = new ExcessivePower();
                        ew.type = in.readInt();
                        ew.overTime = in.readLong();
                        ew.usedTime = in.readLong();
                        this.mExcessivePower.add(ew);
                    }
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("File corrupt: too many excessive power entries ");
                    stringBuilder.append(N);
                    throw new ParcelFormatException(stringBuilder.toString());
                }
            }

            void writeToParcelLocked(Parcel out) {
                out.writeLong(this.mUserTime);
                out.writeLong(this.mSystemTime);
                out.writeLong(this.mForegroundTime);
                out.writeInt(this.mStarts);
                out.writeInt(this.mNumCrashes);
                out.writeInt(this.mNumAnrs);
                out.writeLong(this.mLoadedUserTime);
                out.writeLong(this.mLoadedSystemTime);
                out.writeLong(this.mLoadedForegroundTime);
                out.writeInt(this.mLoadedStarts);
                out.writeInt(this.mLoadedNumCrashes);
                out.writeInt(this.mLoadedNumAnrs);
                out.writeLong(this.mUnpluggedUserTime);
                out.writeLong(this.mUnpluggedSystemTime);
                out.writeLong(this.mUnpluggedForegroundTime);
                out.writeInt(this.mUnpluggedStarts);
                out.writeInt(this.mUnpluggedNumCrashes);
                out.writeInt(this.mUnpluggedNumAnrs);
                writeExcessivePowerToParcelLocked(out);
            }

            void readFromParcelLocked(Parcel in) {
                this.mUserTime = in.readLong();
                this.mSystemTime = in.readLong();
                this.mForegroundTime = in.readLong();
                this.mStarts = in.readInt();
                this.mNumCrashes = in.readInt();
                this.mNumAnrs = in.readInt();
                this.mLoadedUserTime = in.readLong();
                this.mLoadedSystemTime = in.readLong();
                this.mLoadedForegroundTime = in.readLong();
                this.mLoadedStarts = in.readInt();
                this.mLoadedNumCrashes = in.readInt();
                this.mLoadedNumAnrs = in.readInt();
                this.mUnpluggedUserTime = in.readLong();
                this.mUnpluggedSystemTime = in.readLong();
                this.mUnpluggedForegroundTime = in.readLong();
                this.mUnpluggedStarts = in.readInt();
                this.mUnpluggedNumCrashes = in.readInt();
                this.mUnpluggedNumAnrs = in.readInt();
                readExcessivePowerFromParcelLocked(in);
            }

            public void addCpuTimeLocked(int utime, int stime) {
                addCpuTimeLocked(utime, stime, this.mBsi.mOnBatteryTimeBase.isRunning());
            }

            public void addCpuTimeLocked(int utime, int stime, boolean isRunning) {
                if (isRunning) {
                    this.mUserTime += (long) utime;
                    this.mSystemTime += (long) stime;
                }
            }

            public void addForegroundTimeLocked(long ttime) {
                this.mForegroundTime += ttime;
            }

            public void incStartsLocked() {
                this.mStarts++;
            }

            public void incNumCrashesLocked() {
                this.mNumCrashes++;
            }

            public void incNumAnrsLocked() {
                this.mNumAnrs++;
            }

            public boolean isActive() {
                return this.mActive;
            }

            public long getUserTime(int which) {
                long val = this.mUserTime;
                if (which == 1) {
                    return val - this.mLoadedUserTime;
                }
                if (which == 2) {
                    return val - this.mUnpluggedUserTime;
                }
                return val;
            }

            public long getSystemTime(int which) {
                long val = this.mSystemTime;
                if (which == 1) {
                    return val - this.mLoadedSystemTime;
                }
                if (which == 2) {
                    return val - this.mUnpluggedSystemTime;
                }
                return val;
            }

            public long getForegroundTime(int which) {
                long val = this.mForegroundTime;
                if (which == 1) {
                    return val - this.mLoadedForegroundTime;
                }
                if (which == 2) {
                    return val - this.mUnpluggedForegroundTime;
                }
                return val;
            }

            public int getStarts(int which) {
                int val = this.mStarts;
                if (which == 1) {
                    return val - this.mLoadedStarts;
                }
                if (which == 2) {
                    return val - this.mUnpluggedStarts;
                }
                return val;
            }

            public int getNumCrashes(int which) {
                int val = this.mNumCrashes;
                if (which == 1) {
                    return val - this.mLoadedNumCrashes;
                }
                if (which == 2) {
                    return val - this.mUnpluggedNumCrashes;
                }
                return val;
            }

            public int getNumAnrs(int which) {
                int val = this.mNumAnrs;
                if (which == 1) {
                    return val - this.mLoadedNumAnrs;
                }
                if (which == 2) {
                    return val - this.mUnpluggedNumAnrs;
                }
                return val;
            }
        }

        public static class Sensor extends android.os.BatteryStats.Uid.Sensor {
            protected BatteryStatsImpl mBsi;
            final int mHandle;
            DualTimer mTimer;
            protected Uid mUid;

            public Sensor(BatteryStatsImpl bsi, Uid uid, int handle) {
                this.mBsi = bsi;
                this.mUid = uid;
                this.mHandle = handle;
            }

            private DualTimer readTimersFromParcel(TimeBase timeBase, TimeBase bgTimeBase, Parcel in) {
                if (in.readInt() == 0) {
                    return null;
                }
                ArrayList<StopwatchTimer> pool = (ArrayList) this.mBsi.mSensorTimers.get(this.mHandle);
                if (pool == null) {
                    pool = new ArrayList();
                    this.mBsi.mSensorTimers.put(this.mHandle, pool);
                }
                return new DualTimer(this.mBsi.mClocks, this.mUid, 0, pool, timeBase, bgTimeBase, in);
            }

            boolean reset() {
                if (!this.mTimer.reset(true)) {
                    return false;
                }
                this.mTimer = null;
                return true;
            }

            void readFromParcelLocked(TimeBase timeBase, TimeBase bgTimeBase, Parcel in) {
                this.mTimer = readTimersFromParcel(timeBase, bgTimeBase, in);
            }

            void writeToParcelLocked(Parcel out, long elapsedRealtimeUs) {
                Timer.writeTimerToParcel(out, this.mTimer, elapsedRealtimeUs);
            }

            public Timer getSensorTime() {
                return this.mTimer;
            }

            public Timer getSensorBackgroundTime() {
                if (this.mTimer == null) {
                    return null;
                }
                return this.mTimer.getSubTimer();
            }

            public int getHandle() {
                return this.mHandle;
            }
        }

        public static class Wakelock extends android.os.BatteryStats.Uid.Wakelock {
            protected BatteryStatsImpl mBsi;
            StopwatchTimer mTimerDraw;
            StopwatchTimer mTimerFull;
            DualTimer mTimerPartial;
            StopwatchTimer mTimerWindow;
            protected Uid mUid;

            public Wakelock(BatteryStatsImpl bsi, Uid uid) {
                this.mBsi = bsi;
                this.mUid = uid;
            }

            private StopwatchTimer readStopwatchTimerFromParcel(int type, ArrayList<StopwatchTimer> pool, TimeBase timeBase, Parcel in) {
                if (in.readInt() == 0) {
                    return null;
                }
                return new StopwatchTimer(this.mBsi.mClocks, this.mUid, type, pool, timeBase, in);
            }

            private DualTimer readDualTimerFromParcel(int type, ArrayList<StopwatchTimer> pool, TimeBase timeBase, TimeBase bgTimeBase, Parcel in) {
                if (in.readInt() == 0) {
                    return null;
                }
                return new DualTimer(this.mBsi.mClocks, this.mUid, type, pool, timeBase, bgTimeBase, in);
            }

            boolean reset() {
                boolean wlactive = false;
                if (this.mTimerFull != null) {
                    wlactive = false | (this.mTimerFull.reset(false) ^ 1);
                }
                if (this.mTimerPartial != null) {
                    wlactive |= this.mTimerPartial.reset(false) ^ 1;
                }
                if (this.mTimerWindow != null) {
                    wlactive |= this.mTimerWindow.reset(false) ^ 1;
                }
                if (this.mTimerDraw != null) {
                    wlactive |= this.mTimerDraw.reset(false) ^ 1;
                }
                if (!wlactive) {
                    if (this.mTimerFull != null) {
                        this.mTimerFull.detach();
                        this.mTimerFull = null;
                    }
                    if (this.mTimerPartial != null) {
                        this.mTimerPartial.detach();
                        this.mTimerPartial = null;
                    }
                    if (this.mTimerWindow != null) {
                        this.mTimerWindow.detach();
                        this.mTimerWindow = null;
                    }
                    if (this.mTimerDraw != null) {
                        this.mTimerDraw.detach();
                        this.mTimerDraw = null;
                    }
                }
                if (wlactive) {
                    return false;
                }
                return true;
            }

            void readFromParcelLocked(TimeBase timeBase, TimeBase screenOffTimeBase, TimeBase screenOffBgTimeBase, Parcel in) {
                this.mTimerPartial = readDualTimerFromParcel(0, this.mBsi.mPartialTimers, screenOffTimeBase, screenOffBgTimeBase, in);
                this.mTimerFull = readStopwatchTimerFromParcel(1, this.mBsi.mFullTimers, timeBase, in);
                this.mTimerWindow = readStopwatchTimerFromParcel(2, this.mBsi.mWindowTimers, timeBase, in);
                this.mTimerDraw = readStopwatchTimerFromParcel(18, this.mBsi.mDrawTimers, timeBase, in);
            }

            void writeToParcelLocked(Parcel out, long elapsedRealtimeUs) {
                Timer.writeTimerToParcel(out, this.mTimerPartial, elapsedRealtimeUs);
                Timer.writeTimerToParcel(out, this.mTimerFull, elapsedRealtimeUs);
                Timer.writeTimerToParcel(out, this.mTimerWindow, elapsedRealtimeUs);
                Timer.writeTimerToParcel(out, this.mTimerDraw, elapsedRealtimeUs);
            }

            public Timer getWakeTime(int type) {
                if (type == 18) {
                    return this.mTimerDraw;
                }
                switch (type) {
                    case 0:
                        return this.mTimerPartial;
                    case 1:
                        return this.mTimerFull;
                    case 2:
                        return this.mTimerWindow;
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("type = ");
                        stringBuilder.append(type);
                        throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
        }

        public Uid(BatteryStatsImpl bsi, int uid) {
            this.mBsi = bsi;
            this.mUid = uid;
            this.mOnBatteryBackgroundTimeBase = new TimeBase();
            this.mOnBatteryBackgroundTimeBase.init(this.mBsi.mClocks.uptimeMillis() * 1000, this.mBsi.mClocks.elapsedRealtime() * 1000);
            this.mOnBatteryScreenOffBackgroundTimeBase = new TimeBase();
            this.mOnBatteryScreenOffBackgroundTimeBase.init(this.mBsi.mClocks.uptimeMillis() * 1000, this.mBsi.mClocks.elapsedRealtime() * 1000);
            this.mUserCpuTime = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase);
            this.mSystemCpuTime = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase);
            this.mCpuActiveTimeMs = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase);
            this.mCpuClusterTimesMs = new LongSamplingCounterArray(this.mBsi.mOnBatteryTimeBase);
            BatteryStatsImpl batteryStatsImpl = this.mBsi;
            Objects.requireNonNull(batteryStatsImpl);
            this.mWakelockStats = new AnonymousClass1(batteryStatsImpl, uid);
            batteryStatsImpl = this.mBsi;
            Objects.requireNonNull(batteryStatsImpl);
            this.mSyncStats = new AnonymousClass2(batteryStatsImpl, uid);
            batteryStatsImpl = this.mBsi;
            Objects.requireNonNull(batteryStatsImpl);
            this.mJobStats = new AnonymousClass3(batteryStatsImpl, uid);
            this.mWifiRunningTimer = new StopwatchTimer(this.mBsi.mClocks, this, 4, this.mBsi.mWifiRunningTimers, this.mBsi.mOnBatteryTimeBase);
            this.mFullWifiLockTimer = new StopwatchTimer(this.mBsi.mClocks, this, 5, this.mBsi.mFullWifiLockTimers, this.mBsi.mOnBatteryTimeBase);
            this.mWifiScanTimer = new DualTimer(this.mBsi.mClocks, this, 6, this.mBsi.mWifiScanTimers, this.mBsi.mOnBatteryTimeBase, this.mOnBatteryBackgroundTimeBase);
            this.mWifiBatchedScanTimer = new StopwatchTimer[5];
            this.mWifiMulticastTimer = new StopwatchTimer(this.mBsi.mClocks, this, 7, this.mBsi.mWifiMulticastTimers, this.mBsi.mOnBatteryTimeBase);
            this.mProcessStateTimer = new StopwatchTimer[7];
            this.mJobsDeferredEventCount = new Counter(this.mBsi.mOnBatteryTimeBase);
            this.mJobsDeferredCount = new Counter(this.mBsi.mOnBatteryTimeBase);
            this.mJobsFreshnessTimeMs = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase);
            this.mJobsFreshnessBuckets = new Counter[BatteryStats.JOB_FRESHNESS_BUCKETS.length];
        }

        @VisibleForTesting
        public void setProcessStateForTest(int procState) {
            this.mProcessState = procState;
        }

        public long[] getCpuFreqTimes(int which) {
            return nullIfAllZeros(this.mCpuFreqTimeMs, which);
        }

        public long[] getScreenOffCpuFreqTimes(int which) {
            return nullIfAllZeros(this.mScreenOffCpuFreqTimeMs, which);
        }

        public long getCpuActiveTime() {
            return this.mCpuActiveTimeMs.getCountLocked(0);
        }

        public long[] getCpuClusterTimes() {
            return nullIfAllZeros(this.mCpuClusterTimesMs, 0);
        }

        /* JADX WARNING: Missing block: B:13:0x001e, code skipped:
            return null;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public long[] getCpuFreqTimes(int which, int procState) {
            if (which < 0 || which >= 7 || this.mProcStateTimeMs == null) {
                return null;
            }
            if (this.mBsi.mPerProcStateCpuTimesAvailable) {
                return nullIfAllZeros(this.mProcStateTimeMs[procState], which);
            }
            this.mProcStateTimeMs = null;
            return null;
        }

        /* JADX WARNING: Missing block: B:13:0x001e, code skipped:
            return null;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public long[] getScreenOffCpuFreqTimes(int which, int procState) {
            if (which < 0 || which >= 7 || this.mProcStateScreenOffTimeMs == null) {
                return null;
            }
            if (this.mBsi.mPerProcStateCpuTimesAvailable) {
                return nullIfAllZeros(this.mProcStateScreenOffTimeMs[procState], which);
            }
            this.mProcStateScreenOffTimeMs = null;
            return null;
        }

        public void addIsolatedUid(int isolatedUid) {
            if (this.mChildUids == null) {
                this.mChildUids = new IntArray();
            } else if (this.mChildUids.indexOf(isolatedUid) >= 0) {
                return;
            }
            this.mChildUids.add(isolatedUid);
        }

        public void removeIsolatedUid(int isolatedUid) {
            int idx = this.mChildUids == null ? -1 : this.mChildUids.indexOf(isolatedUid);
            if (idx >= 0) {
                this.mChildUids.remove(idx);
            }
        }

        private long[] nullIfAllZeros(LongSamplingCounterArray cpuTimesMs, int which) {
            if (cpuTimesMs == null) {
                return null;
            }
            long[] counts = cpuTimesMs.getCountsLocked(which);
            if (counts == null) {
                return null;
            }
            for (int i = counts.length - 1; i >= 0; i--) {
                if (counts[i] != 0) {
                    return counts;
                }
            }
            return null;
        }

        private void addProcStateTimesMs(int procState, long[] cpuTimesMs, boolean onBattery) {
            if (this.mProcStateTimeMs == null) {
                this.mProcStateTimeMs = new LongSamplingCounterArray[7];
            }
            if (this.mProcStateTimeMs[procState] == null || this.mProcStateTimeMs[procState].getSize() != cpuTimesMs.length) {
                this.mProcStateTimeMs[procState] = new LongSamplingCounterArray(this.mBsi.mOnBatteryTimeBase);
            }
            this.mProcStateTimeMs[procState].addCountLocked(cpuTimesMs, onBattery);
        }

        private void addProcStateScreenOffTimesMs(int procState, long[] cpuTimesMs, boolean onBatteryScreenOff) {
            if (this.mProcStateScreenOffTimeMs == null) {
                this.mProcStateScreenOffTimeMs = new LongSamplingCounterArray[7];
            }
            if (this.mProcStateScreenOffTimeMs[procState] == null || this.mProcStateScreenOffTimeMs[procState].getSize() != cpuTimesMs.length) {
                this.mProcStateScreenOffTimeMs[procState] = new LongSamplingCounterArray(this.mBsi.mOnBatteryScreenOffTimeBase);
            }
            this.mProcStateScreenOffTimeMs[procState].addCountLocked(cpuTimesMs, onBatteryScreenOff);
        }

        public Timer getAggregatedPartialWakelockTimer() {
            return this.mAggregatedPartialWakelockTimer;
        }

        public ArrayMap<String, ? extends android.os.BatteryStats.Uid.Wakelock> getWakelockStats() {
            return this.mWakelockStats.getMap();
        }

        public Timer getMulticastWakelockStats() {
            return this.mWifiMulticastTimer;
        }

        public ArrayMap<String, ? extends android.os.BatteryStats.Timer> getSyncStats() {
            return this.mSyncStats.getMap();
        }

        public ArrayMap<String, ? extends android.os.BatteryStats.Timer> getJobStats() {
            return this.mJobStats.getMap();
        }

        public ArrayMap<String, SparseIntArray> getJobCompletionStats() {
            return this.mJobCompletions;
        }

        public SparseArray<? extends android.os.BatteryStats.Uid.Sensor> getSensorStats() {
            return this.mSensorStats;
        }

        public ArrayMap<String, ? extends android.os.BatteryStats.Uid.Proc> getProcessStats() {
            return this.mProcessStats;
        }

        public ArrayMap<String, ? extends android.os.BatteryStats.Uid.Pkg> getPackageStats() {
            return this.mPackageStats;
        }

        public int getUid() {
            return this.mUid;
        }

        public void noteWifiRunningLocked(long elapsedRealtimeMs) {
            if (!this.mWifiRunning) {
                this.mWifiRunning = true;
                if (this.mWifiRunningTimer == null) {
                    this.mWifiRunningTimer = new StopwatchTimer(this.mBsi.mClocks, this, 4, this.mBsi.mWifiRunningTimers, this.mBsi.mOnBatteryTimeBase);
                }
                this.mWifiRunningTimer.startRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteWifiStoppedLocked(long elapsedRealtimeMs) {
            if (this.mWifiRunning) {
                this.mWifiRunning = false;
                this.mWifiRunningTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteFullWifiLockAcquiredLocked(long elapsedRealtimeMs) {
            if (!this.mFullWifiLockOut) {
                this.mFullWifiLockOut = true;
                if (this.mFullWifiLockTimer == null) {
                    this.mFullWifiLockTimer = new StopwatchTimer(this.mBsi.mClocks, this, 5, this.mBsi.mFullWifiLockTimers, this.mBsi.mOnBatteryTimeBase);
                }
                this.mFullWifiLockTimer.startRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteFullWifiLockReleasedLocked(long elapsedRealtimeMs) {
            if (this.mFullWifiLockOut) {
                this.mFullWifiLockOut = false;
                this.mFullWifiLockTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteWifiScanStartedLocked(long elapsedRealtimeMs) {
            if (!this.mWifiScanStarted) {
                this.mWifiScanStarted = true;
                if (this.mWifiScanTimer == null) {
                    this.mWifiScanTimer = new DualTimer(this.mBsi.mClocks, this, 6, this.mBsi.mWifiScanTimers, this.mBsi.mOnBatteryTimeBase, this.mOnBatteryBackgroundTimeBase);
                }
                this.mWifiScanTimer.startRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteWifiScanStoppedLocked(long elapsedRealtimeMs) {
            if (this.mWifiScanStarted) {
                this.mWifiScanStarted = false;
                this.mWifiScanTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteWifiBatchedScanStartedLocked(int csph, long elapsedRealtimeMs) {
            int bin = 0;
            while (csph > 8 && bin < 4) {
                csph >>= 3;
                bin++;
            }
            if (this.mWifiBatchedScanBinStarted != bin) {
                if (this.mWifiBatchedScanBinStarted != -1) {
                    this.mWifiBatchedScanTimer[this.mWifiBatchedScanBinStarted].stopRunningLocked(elapsedRealtimeMs);
                }
                this.mWifiBatchedScanBinStarted = bin;
                if (this.mWifiBatchedScanTimer[bin] == null) {
                    makeWifiBatchedScanBin(bin, null);
                }
                this.mWifiBatchedScanTimer[bin].startRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteWifiBatchedScanStoppedLocked(long elapsedRealtimeMs) {
            if (this.mWifiBatchedScanBinStarted != -1) {
                this.mWifiBatchedScanTimer[this.mWifiBatchedScanBinStarted].stopRunningLocked(elapsedRealtimeMs);
                this.mWifiBatchedScanBinStarted = -1;
            }
        }

        public void noteWifiMulticastEnabledLocked(long elapsedRealtimeMs) {
            if (!this.mWifiMulticastEnabled) {
                this.mWifiMulticastEnabled = true;
                if (this.mWifiMulticastTimer == null) {
                    this.mWifiMulticastTimer = new StopwatchTimer(this.mBsi.mClocks, this, 7, this.mBsi.mWifiMulticastTimers, this.mBsi.mOnBatteryTimeBase);
                }
                this.mWifiMulticastTimer.startRunningLocked(elapsedRealtimeMs);
                StatsLog.write_non_chained(53, getUid(), null, 1);
            }
        }

        public void noteWifiMulticastDisabledLocked(long elapsedRealtimeMs) {
            if (this.mWifiMulticastEnabled) {
                this.mWifiMulticastEnabled = false;
                this.mWifiMulticastTimer.stopRunningLocked(elapsedRealtimeMs);
                StatsLog.write_non_chained(53, getUid(), null, 0);
            }
        }

        public ControllerActivityCounter getWifiControllerActivity() {
            return this.mWifiControllerActivity;
        }

        public ControllerActivityCounter getBluetoothControllerActivity() {
            return this.mBluetoothControllerActivity;
        }

        public ControllerActivityCounter getModemControllerActivity() {
            return this.mModemControllerActivity;
        }

        public ControllerActivityCounterImpl getOrCreateWifiControllerActivityLocked() {
            if (this.mWifiControllerActivity == null) {
                this.mWifiControllerActivity = new ControllerActivityCounterImpl(this.mBsi.mOnBatteryTimeBase, 1);
            }
            return this.mWifiControllerActivity;
        }

        public ControllerActivityCounterImpl getOrCreateBluetoothControllerActivityLocked() {
            if (this.mBluetoothControllerActivity == null) {
                this.mBluetoothControllerActivity = new ControllerActivityCounterImpl(this.mBsi.mOnBatteryTimeBase, 1);
            }
            return this.mBluetoothControllerActivity;
        }

        public ControllerActivityCounterImpl getOrCreateModemControllerActivityLocked() {
            if (this.mModemControllerActivity == null) {
                this.mModemControllerActivity = new ControllerActivityCounterImpl(this.mBsi.mOnBatteryTimeBase, 5);
            }
            return this.mModemControllerActivity;
        }

        public StopwatchTimer createAudioTurnedOnTimerLocked() {
            if (this.mAudioTurnedOnTimer == null) {
                this.mAudioTurnedOnTimer = new StopwatchTimer(this.mBsi.mClocks, this, 15, this.mBsi.mAudioTurnedOnTimers, this.mBsi.mOnBatteryTimeBase);
            }
            return this.mAudioTurnedOnTimer;
        }

        public void noteAudioTurnedOnLocked(long elapsedRealtimeMs) {
            createAudioTurnedOnTimerLocked().startRunningLocked(elapsedRealtimeMs);
        }

        public void noteAudioTurnedOffLocked(long elapsedRealtimeMs) {
            if (this.mAudioTurnedOnTimer != null) {
                this.mAudioTurnedOnTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteResetAudioLocked(long elapsedRealtimeMs) {
            if (this.mAudioTurnedOnTimer != null) {
                this.mAudioTurnedOnTimer.stopAllRunningLocked(elapsedRealtimeMs);
            }
        }

        public StopwatchTimer createVideoTurnedOnTimerLocked() {
            if (this.mVideoTurnedOnTimer == null) {
                this.mVideoTurnedOnTimer = new StopwatchTimer(this.mBsi.mClocks, this, 8, this.mBsi.mVideoTurnedOnTimers, this.mBsi.mOnBatteryTimeBase);
            }
            return this.mVideoTurnedOnTimer;
        }

        public void noteVideoTurnedOnLocked(long elapsedRealtimeMs) {
            createVideoTurnedOnTimerLocked().startRunningLocked(elapsedRealtimeMs);
        }

        public void noteVideoTurnedOffLocked(long elapsedRealtimeMs) {
            if (this.mVideoTurnedOnTimer != null) {
                this.mVideoTurnedOnTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteResetVideoLocked(long elapsedRealtimeMs) {
            if (this.mVideoTurnedOnTimer != null) {
                this.mVideoTurnedOnTimer.stopAllRunningLocked(elapsedRealtimeMs);
            }
        }

        public StopwatchTimer createFlashlightTurnedOnTimerLocked() {
            if (this.mFlashlightTurnedOnTimer == null) {
                this.mFlashlightTurnedOnTimer = new StopwatchTimer(this.mBsi.mClocks, this, 16, this.mBsi.mFlashlightTurnedOnTimers, this.mBsi.mOnBatteryTimeBase);
            }
            return this.mFlashlightTurnedOnTimer;
        }

        public void noteFlashlightTurnedOnLocked(long elapsedRealtimeMs) {
            createFlashlightTurnedOnTimerLocked().startRunningLocked(elapsedRealtimeMs);
        }

        public void noteFlashlightTurnedOffLocked(long elapsedRealtimeMs) {
            if (this.mFlashlightTurnedOnTimer != null) {
                this.mFlashlightTurnedOnTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteResetFlashlightLocked(long elapsedRealtimeMs) {
            if (this.mFlashlightTurnedOnTimer != null) {
                this.mFlashlightTurnedOnTimer.stopAllRunningLocked(elapsedRealtimeMs);
            }
        }

        public StopwatchTimer createCameraTurnedOnTimerLocked() {
            if (this.mCameraTurnedOnTimer == null) {
                this.mCameraTurnedOnTimer = new StopwatchTimer(this.mBsi.mClocks, this, 17, this.mBsi.mCameraTurnedOnTimers, this.mBsi.mOnBatteryTimeBase);
            }
            return this.mCameraTurnedOnTimer;
        }

        public void noteCameraTurnedOnLocked(long elapsedRealtimeMs) {
            createCameraTurnedOnTimerLocked().startRunningLocked(elapsedRealtimeMs);
        }

        public void noteCameraTurnedOffLocked(long elapsedRealtimeMs) {
            if (this.mCameraTurnedOnTimer != null) {
                this.mCameraTurnedOnTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteResetCameraLocked(long elapsedRealtimeMs) {
            if (this.mCameraTurnedOnTimer != null) {
                this.mCameraTurnedOnTimer.stopAllRunningLocked(elapsedRealtimeMs);
            }
        }

        public StopwatchTimer createForegroundActivityTimerLocked() {
            if (this.mForegroundActivityTimer == null) {
                this.mForegroundActivityTimer = new StopwatchTimer(this.mBsi.mClocks, this, 10, null, this.mBsi.mOnBatteryTimeBase);
            }
            return this.mForegroundActivityTimer;
        }

        public StopwatchTimer createForegroundServiceTimerLocked() {
            if (this.mForegroundServiceTimer == null) {
                this.mForegroundServiceTimer = new StopwatchTimer(this.mBsi.mClocks, this, 22, null, this.mBsi.mOnBatteryTimeBase);
            }
            return this.mForegroundServiceTimer;
        }

        public DualTimer createAggregatedPartialWakelockTimerLocked() {
            if (this.mAggregatedPartialWakelockTimer == null) {
                this.mAggregatedPartialWakelockTimer = new DualTimer(this.mBsi.mClocks, this, 20, null, this.mBsi.mOnBatteryScreenOffTimeBase, this.mOnBatteryScreenOffBackgroundTimeBase);
            }
            return this.mAggregatedPartialWakelockTimer;
        }

        public DualTimer createBluetoothScanTimerLocked() {
            if (this.mBluetoothScanTimer == null) {
                this.mBluetoothScanTimer = new DualTimer(this.mBsi.mClocks, this, 19, this.mBsi.mBluetoothScanOnTimers, this.mBsi.mOnBatteryTimeBase, this.mOnBatteryBackgroundTimeBase);
            }
            return this.mBluetoothScanTimer;
        }

        public DualTimer createBluetoothUnoptimizedScanTimerLocked() {
            if (this.mBluetoothUnoptimizedScanTimer == null) {
                this.mBluetoothUnoptimizedScanTimer = new DualTimer(this.mBsi.mClocks, this, 21, null, this.mBsi.mOnBatteryTimeBase, this.mOnBatteryBackgroundTimeBase);
            }
            return this.mBluetoothUnoptimizedScanTimer;
        }

        public void noteBluetoothScanStartedLocked(long elapsedRealtimeMs, boolean isUnoptimized) {
            createBluetoothScanTimerLocked().startRunningLocked(elapsedRealtimeMs);
            if (isUnoptimized) {
                createBluetoothUnoptimizedScanTimerLocked().startRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteBluetoothScanStoppedLocked(long elapsedRealtimeMs, boolean isUnoptimized) {
            if (this.mBluetoothScanTimer != null) {
                this.mBluetoothScanTimer.stopRunningLocked(elapsedRealtimeMs);
            }
            if (isUnoptimized && this.mBluetoothUnoptimizedScanTimer != null) {
                this.mBluetoothUnoptimizedScanTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteResetBluetoothScanLocked(long elapsedRealtimeMs) {
            if (this.mBluetoothScanTimer != null) {
                this.mBluetoothScanTimer.stopAllRunningLocked(elapsedRealtimeMs);
            }
            if (this.mBluetoothUnoptimizedScanTimer != null) {
                this.mBluetoothUnoptimizedScanTimer.stopAllRunningLocked(elapsedRealtimeMs);
            }
        }

        public Counter createBluetoothScanResultCounterLocked() {
            if (this.mBluetoothScanResultCounter == null) {
                this.mBluetoothScanResultCounter = new Counter(this.mBsi.mOnBatteryTimeBase);
            }
            return this.mBluetoothScanResultCounter;
        }

        public Counter createBluetoothScanResultBgCounterLocked() {
            if (this.mBluetoothScanResultBgCounter == null) {
                this.mBluetoothScanResultBgCounter = new Counter(this.mOnBatteryBackgroundTimeBase);
            }
            return this.mBluetoothScanResultBgCounter;
        }

        public void noteBluetoothScanResultsLocked(int numNewResults) {
            createBluetoothScanResultCounterLocked().addAtomic(numNewResults);
            createBluetoothScanResultBgCounterLocked().addAtomic(numNewResults);
        }

        public void noteActivityResumedLocked(long elapsedRealtimeMs) {
            createForegroundActivityTimerLocked().startRunningLocked(elapsedRealtimeMs);
        }

        public void noteActivityPausedLocked(long elapsedRealtimeMs) {
            if (this.mForegroundActivityTimer != null) {
                this.mForegroundActivityTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteForegroundServiceResumedLocked(long elapsedRealtimeMs) {
            createForegroundServiceTimerLocked().startRunningLocked(elapsedRealtimeMs);
        }

        public void noteForegroundServicePausedLocked(long elapsedRealtimeMs) {
            if (this.mForegroundServiceTimer != null) {
                this.mForegroundServiceTimer.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        public BatchTimer createVibratorOnTimerLocked() {
            if (this.mVibratorOnTimer == null) {
                this.mVibratorOnTimer = new BatchTimer(this.mBsi.mClocks, this, 9, this.mBsi.mOnBatteryTimeBase);
            }
            return this.mVibratorOnTimer;
        }

        public void noteVibratorOnLocked(long durationMillis) {
            createVibratorOnTimerLocked().addDuration(this.mBsi, durationMillis);
        }

        public void noteVibratorOffLocked() {
            if (this.mVibratorOnTimer != null) {
                this.mVibratorOnTimer.abortLastDuration(this.mBsi);
            }
        }

        public long getWifiRunningTime(long elapsedRealtimeUs, int which) {
            if (this.mWifiRunningTimer == null) {
                return 0;
            }
            return this.mWifiRunningTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
        }

        public long getFullWifiLockTime(long elapsedRealtimeUs, int which) {
            if (this.mFullWifiLockTimer == null) {
                return 0;
            }
            return this.mFullWifiLockTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
        }

        public long getWifiScanTime(long elapsedRealtimeUs, int which) {
            if (this.mWifiScanTimer == null) {
                return 0;
            }
            return this.mWifiScanTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
        }

        public int getWifiScanCount(int which) {
            if (this.mWifiScanTimer == null) {
                return 0;
            }
            return this.mWifiScanTimer.getCountLocked(which);
        }

        public Timer getWifiScanTimer() {
            return this.mWifiScanTimer;
        }

        public int getWifiScanBackgroundCount(int which) {
            if (this.mWifiScanTimer == null || this.mWifiScanTimer.getSubTimer() == null) {
                return 0;
            }
            return this.mWifiScanTimer.getSubTimer().getCountLocked(which);
        }

        public long getWifiScanActualTime(long elapsedRealtimeUs) {
            if (this.mWifiScanTimer == null) {
                return 0;
            }
            return this.mWifiScanTimer.getTotalDurationMsLocked((500 + elapsedRealtimeUs) / 1000) * 1000;
        }

        public long getWifiScanBackgroundTime(long elapsedRealtimeUs) {
            if (this.mWifiScanTimer == null || this.mWifiScanTimer.getSubTimer() == null) {
                return 0;
            }
            return this.mWifiScanTimer.getSubTimer().getTotalDurationMsLocked((500 + elapsedRealtimeUs) / 1000) * 1000;
        }

        public Timer getWifiScanBackgroundTimer() {
            if (this.mWifiScanTimer == null) {
                return null;
            }
            return this.mWifiScanTimer.getSubTimer();
        }

        /* JADX WARNING: Missing block: B:9:0x0018, code skipped:
            return 0;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public long getWifiBatchedScanTime(int csphBin, long elapsedRealtimeUs, int which) {
            if (csphBin < 0 || csphBin >= 5 || this.mWifiBatchedScanTimer[csphBin] == null) {
                return 0;
            }
            return this.mWifiBatchedScanTimer[csphBin].getTotalTimeLocked(elapsedRealtimeUs, which);
        }

        /* JADX WARNING: Missing block: B:9:0x0017, code skipped:
            return 0;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public int getWifiBatchedScanCount(int csphBin, int which) {
            if (csphBin < 0 || csphBin >= 5 || this.mWifiBatchedScanTimer[csphBin] == null) {
                return 0;
            }
            return this.mWifiBatchedScanTimer[csphBin].getCountLocked(which);
        }

        public long getWifiMulticastTime(long elapsedRealtimeUs, int which) {
            if (this.mWifiMulticastTimer == null) {
                return 0;
            }
            return this.mWifiMulticastTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
        }

        public Timer getAudioTurnedOnTimer() {
            return this.mAudioTurnedOnTimer;
        }

        public Timer getVideoTurnedOnTimer() {
            return this.mVideoTurnedOnTimer;
        }

        public Timer getFlashlightTurnedOnTimer() {
            return this.mFlashlightTurnedOnTimer;
        }

        public Timer getCameraTurnedOnTimer() {
            return this.mCameraTurnedOnTimer;
        }

        public Timer getForegroundActivityTimer() {
            return this.mForegroundActivityTimer;
        }

        public Timer getForegroundServiceTimer() {
            return this.mForegroundServiceTimer;
        }

        public Timer getBluetoothScanTimer() {
            return this.mBluetoothScanTimer;
        }

        public Timer getBluetoothScanBackgroundTimer() {
            if (this.mBluetoothScanTimer == null) {
                return null;
            }
            return this.mBluetoothScanTimer.getSubTimer();
        }

        public Timer getBluetoothUnoptimizedScanTimer() {
            return this.mBluetoothUnoptimizedScanTimer;
        }

        public Timer getBluetoothUnoptimizedScanBackgroundTimer() {
            if (this.mBluetoothUnoptimizedScanTimer == null) {
                return null;
            }
            return this.mBluetoothUnoptimizedScanTimer.getSubTimer();
        }

        public Counter getBluetoothScanResultCounter() {
            return this.mBluetoothScanResultCounter;
        }

        public Counter getBluetoothScanResultBgCounter() {
            return this.mBluetoothScanResultBgCounter;
        }

        void makeProcessState(int i, Parcel in) {
            if (i >= 0 && i < 7) {
                if (in == null) {
                    this.mProcessStateTimer[i] = new StopwatchTimer(this.mBsi.mClocks, this, 12, null, this.mBsi.mOnBatteryTimeBase);
                } else {
                    this.mProcessStateTimer[i] = new StopwatchTimer(this.mBsi.mClocks, this, 12, null, this.mBsi.mOnBatteryTimeBase, in);
                }
            }
        }

        /* JADX WARNING: Missing block: B:9:0x0018, code skipped:
            return 0;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public long getProcessStateTime(int state, long elapsedRealtimeUs, int which) {
            if (state < 0 || state >= 7 || this.mProcessStateTimer[state] == null) {
                return 0;
            }
            return this.mProcessStateTimer[state].getTotalTimeLocked(elapsedRealtimeUs, which);
        }

        public Timer getProcessStateTimer(int state) {
            if (state < 0 || state >= 7) {
                return null;
            }
            return this.mProcessStateTimer[state];
        }

        public Timer getVibratorOnTimer() {
            return this.mVibratorOnTimer;
        }

        public void noteUserActivityLocked(int type) {
            if (this.mUserActivityCounters == null) {
                initUserActivityLocked();
            }
            if (type < 0 || type >= 4) {
                String str = BatteryStatsImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown user activity type ");
                stringBuilder.append(type);
                stringBuilder.append(" was specified.");
                Slog.w(str, stringBuilder.toString(), new Throwable());
                return;
            }
            this.mUserActivityCounters[type].stepAtomic();
        }

        public boolean hasUserActivity() {
            return this.mUserActivityCounters != null;
        }

        public int getUserActivityCount(int type, int which) {
            if (this.mUserActivityCounters == null) {
                return 0;
            }
            return this.mUserActivityCounters[type].getCountLocked(which);
        }

        void makeWifiBatchedScanBin(int i, Parcel in) {
            if (i >= 0 && i < 5) {
                ArrayList<StopwatchTimer> collected = (ArrayList) this.mBsi.mWifiBatchedScanTimers.get(i);
                if (collected == null) {
                    collected = new ArrayList();
                    this.mBsi.mWifiBatchedScanTimers.put(i, collected);
                }
                if (in == null) {
                    this.mWifiBatchedScanTimer[i] = new StopwatchTimer(this.mBsi.mClocks, this, 11, collected, this.mBsi.mOnBatteryTimeBase);
                } else {
                    this.mWifiBatchedScanTimer[i] = new StopwatchTimer(this.mBsi.mClocks, this, 11, collected, this.mBsi.mOnBatteryTimeBase, in);
                }
            }
        }

        void initUserActivityLocked() {
            this.mUserActivityCounters = new Counter[4];
            for (int i = 0; i < 4; i++) {
                this.mUserActivityCounters[i] = new Counter(this.mBsi.mOnBatteryTimeBase);
            }
        }

        void noteNetworkActivityLocked(int type, long deltaBytes, long deltaPackets) {
            if (this.mNetworkByteActivityCounters == null) {
                initNetworkActivityLocked();
            }
            if (type < 0 || type >= 10) {
                String str = BatteryStatsImpl.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown network activity type ");
                stringBuilder.append(type);
                stringBuilder.append(" was specified.");
                Slog.w(str, stringBuilder.toString(), new Throwable());
                return;
            }
            this.mNetworkByteActivityCounters[type].addCountLocked(deltaBytes);
            this.mNetworkPacketActivityCounters[type].addCountLocked(deltaPackets);
        }

        void noteMobileRadioActiveTimeLocked(long batteryUptime) {
            if (this.mNetworkByteActivityCounters == null) {
                initNetworkActivityLocked();
            }
            this.mMobileRadioActiveTime.addCountLocked(batteryUptime);
            this.mMobileRadioActiveCount.addCountLocked(1);
        }

        public boolean hasNetworkActivity() {
            return this.mNetworkByteActivityCounters != null;
        }

        public long getNetworkActivityBytes(int type, int which) {
            if (this.mNetworkByteActivityCounters == null || type < 0 || type >= this.mNetworkByteActivityCounters.length) {
                return 0;
            }
            return this.mNetworkByteActivityCounters[type].getCountLocked(which);
        }

        public long getNetworkActivityPackets(int type, int which) {
            if (this.mNetworkPacketActivityCounters == null || type < 0 || type >= this.mNetworkPacketActivityCounters.length) {
                return 0;
            }
            return this.mNetworkPacketActivityCounters[type].getCountLocked(which);
        }

        public long getMobileRadioActiveTime(int which) {
            return this.mMobileRadioActiveTime != null ? this.mMobileRadioActiveTime.getCountLocked(which) : 0;
        }

        public int getMobileRadioActiveCount(int which) {
            return this.mMobileRadioActiveCount != null ? (int) this.mMobileRadioActiveCount.getCountLocked(which) : 0;
        }

        public long getUserCpuTimeUs(int which) {
            return this.mUserCpuTime.getCountLocked(which);
        }

        public long getSystemCpuTimeUs(int which) {
            return this.mSystemCpuTime.getCountLocked(which);
        }

        public long getTimeAtCpuSpeed(int cluster, int step, int which) {
            if (this.mCpuClusterSpeedTimesUs != null && cluster >= 0 && cluster < this.mCpuClusterSpeedTimesUs.length) {
                LongSamplingCounter[] cpuSpeedTimesUs = this.mCpuClusterSpeedTimesUs[cluster];
                if (cpuSpeedTimesUs != null && step >= 0 && step < cpuSpeedTimesUs.length) {
                    LongSamplingCounter c = cpuSpeedTimesUs[step];
                    if (c != null) {
                        return c.getCountLocked(which);
                    }
                }
            }
            return 0;
        }

        public void noteMobileRadioApWakeupLocked() {
            if (this.mMobileRadioApWakeupCount == null) {
                this.mMobileRadioApWakeupCount = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase);
            }
            this.mMobileRadioApWakeupCount.addCountLocked(1);
        }

        public long getMobileRadioApWakeupCount(int which) {
            if (this.mMobileRadioApWakeupCount != null) {
                return this.mMobileRadioApWakeupCount.getCountLocked(which);
            }
            return 0;
        }

        public void noteWifiRadioApWakeupLocked() {
            if (this.mWifiRadioApWakeupCount == null) {
                this.mWifiRadioApWakeupCount = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase);
            }
            this.mWifiRadioApWakeupCount.addCountLocked(1);
        }

        public long getWifiRadioApWakeupCount(int which) {
            if (this.mWifiRadioApWakeupCount != null) {
                return this.mWifiRadioApWakeupCount.getCountLocked(which);
            }
            return 0;
        }

        public void getDeferredJobsCheckinLineLocked(StringBuilder sb, int which) {
            int i = 0;
            sb.setLength(0);
            int deferredEventCount = this.mJobsDeferredEventCount.getCountLocked(which);
            if (deferredEventCount != 0) {
                int deferredCount = this.mJobsDeferredCount.getCountLocked(which);
                long totalLatency = this.mJobsFreshnessTimeMs.getCountLocked(which);
                sb.append(deferredEventCount);
                sb.append(',');
                sb.append(deferredCount);
                sb.append(',');
                sb.append(totalLatency);
                while (i < BatteryStats.JOB_FRESHNESS_BUCKETS.length) {
                    if (this.mJobsFreshnessBuckets[i] == null) {
                        sb.append(",0");
                    } else {
                        sb.append(",");
                        sb.append(this.mJobsFreshnessBuckets[i].getCountLocked(which));
                    }
                    i++;
                }
            }
        }

        public void getDeferredJobsLineLocked(StringBuilder sb, int which) {
            int i = 0;
            sb.setLength(0);
            int deferredEventCount = this.mJobsDeferredEventCount.getCountLocked(which);
            if (deferredEventCount != 0) {
                int deferredCount = this.mJobsDeferredCount.getCountLocked(which);
                long totalLatency = this.mJobsFreshnessTimeMs.getCountLocked(which);
                sb.append("times=");
                sb.append(deferredEventCount);
                sb.append(", ");
                sb.append("count=");
                sb.append(deferredCount);
                sb.append(", ");
                sb.append("totalLatencyMs=");
                sb.append(totalLatency);
                sb.append(", ");
                while (i < BatteryStats.JOB_FRESHNESS_BUCKETS.length) {
                    sb.append("<");
                    sb.append(BatteryStats.JOB_FRESHNESS_BUCKETS[i]);
                    sb.append("ms=");
                    if (this.mJobsFreshnessBuckets[i] == null) {
                        sb.append("0");
                    } else {
                        sb.append(this.mJobsFreshnessBuckets[i].getCountLocked(which));
                    }
                    sb.append(" ");
                    i++;
                }
            }
        }

        void initNetworkActivityLocked() {
            this.mNetworkByteActivityCounters = new LongSamplingCounter[10];
            this.mNetworkPacketActivityCounters = new LongSamplingCounter[10];
            for (int i = 0; i < 10; i++) {
                this.mNetworkByteActivityCounters[i] = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase);
                this.mNetworkPacketActivityCounters[i] = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase);
            }
            this.mMobileRadioActiveTime = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase);
            this.mMobileRadioActiveCount = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase);
        }

        @VisibleForTesting(visibility = Visibility.PACKAGE)
        public boolean reset(long uptime, long realtime) {
            boolean active;
            int i;
            int i2;
            int size;
            int size2;
            int size3;
            long j = uptime;
            long j2 = realtime;
            boolean active2 = false;
            this.mOnBatteryBackgroundTimeBase.init(j, j2);
            this.mOnBatteryScreenOffBackgroundTimeBase.init(j, j2);
            if (this.mWifiRunningTimer != null) {
                active2 = (false | (this.mWifiRunningTimer.reset(false) ^ 1)) | this.mWifiRunning;
            }
            if (this.mFullWifiLockTimer != null) {
                active2 = (active2 | (this.mFullWifiLockTimer.reset(false) ^ 1)) | this.mFullWifiLockOut;
            }
            if (this.mWifiScanTimer != null) {
                active2 = (active2 | (this.mWifiScanTimer.reset(false) ^ 1)) | this.mWifiScanStarted;
            }
            if (this.mWifiBatchedScanTimer != null) {
                active = active2;
                for (i = 0; i < 5; i++) {
                    if (this.mWifiBatchedScanTimer[i] != null) {
                        active |= this.mWifiBatchedScanTimer[i].reset(false) ^ 1;
                    }
                }
                active2 = (this.mWifiBatchedScanBinStarted != -1) | active;
            }
            if (this.mWifiMulticastTimer != null) {
                active2 = (active2 | (this.mWifiMulticastTimer.reset(false) ^ 1)) | this.mWifiMulticastEnabled;
            }
            active2 = ((((((((active2 | (BatteryStatsImpl.resetTimerIfNotNull((Timer) this.mAudioTurnedOnTimer, false) ^ 1)) | (BatteryStatsImpl.resetTimerIfNotNull((Timer) this.mVideoTurnedOnTimer, false) ^ 1)) | (BatteryStatsImpl.resetTimerIfNotNull((Timer) this.mFlashlightTurnedOnTimer, false) ^ 1)) | (BatteryStatsImpl.resetTimerIfNotNull((Timer) this.mCameraTurnedOnTimer, false) ^ 1)) | (BatteryStatsImpl.resetTimerIfNotNull((Timer) this.mForegroundActivityTimer, false) ^ 1)) | (BatteryStatsImpl.resetTimerIfNotNull((Timer) this.mForegroundServiceTimer, false) ^ 1)) | (BatteryStatsImpl.resetTimerIfNotNull(this.mAggregatedPartialWakelockTimer, false) ^ 1)) | (BatteryStatsImpl.resetTimerIfNotNull(this.mBluetoothScanTimer, false) ^ 1)) | (BatteryStatsImpl.resetTimerIfNotNull(this.mBluetoothUnoptimizedScanTimer, false) ^ 1);
            if (this.mBluetoothScanResultCounter != null) {
                this.mBluetoothScanResultCounter.reset(false);
            }
            if (this.mBluetoothScanResultBgCounter != null) {
                this.mBluetoothScanResultBgCounter.reset(false);
            }
            if (this.mProcessStateTimer != null) {
                active = active2;
                for (i = 0; i < 7; i++) {
                    if (this.mProcessStateTimer[i] != null) {
                        active |= this.mProcessStateTimer[i].reset(false) ^ 1;
                    }
                }
                active2 = (this.mProcessState != 19) | active;
            }
            if (this.mVibratorOnTimer != null) {
                if (this.mVibratorOnTimer.reset(false)) {
                    this.mVibratorOnTimer.detach();
                    this.mVibratorOnTimer = null;
                } else {
                    active2 = true;
                }
            }
            if (this.mUserActivityCounters != null) {
                for (i2 = 0; i2 < 4; i2++) {
                    this.mUserActivityCounters[i2].reset(false);
                }
            }
            if (this.mNetworkByteActivityCounters != null) {
                for (i2 = 0; i2 < 10; i2++) {
                    this.mNetworkByteActivityCounters[i2].reset(false);
                    this.mNetworkPacketActivityCounters[i2].reset(false);
                }
                this.mMobileRadioActiveTime.reset(false);
                this.mMobileRadioActiveCount.reset(false);
            }
            if (this.mWifiControllerActivity != null) {
                this.mWifiControllerActivity.reset(false);
            }
            if (this.mBluetoothControllerActivity != null) {
                this.mBluetoothControllerActivity.reset(false);
            }
            if (this.mModemControllerActivity != null) {
                this.mModemControllerActivity.reset(false);
            }
            this.mUserCpuTime.reset(false);
            this.mSystemCpuTime.reset(false);
            if (this.mCpuClusterSpeedTimesUs != null) {
                for (LongSamplingCounter[] speeds : this.mCpuClusterSpeedTimesUs) {
                    if (speeds != null) {
                        for (LongSamplingCounter speed : speeds) {
                            if (speed != null) {
                                speed.reset(false);
                            }
                        }
                    }
                }
            }
            if (this.mCpuFreqTimeMs != null) {
                this.mCpuFreqTimeMs.reset(false);
            }
            if (this.mScreenOffCpuFreqTimeMs != null) {
                this.mScreenOffCpuFreqTimeMs.reset(false);
            }
            this.mCpuActiveTimeMs.reset(false);
            this.mCpuClusterTimesMs.reset(false);
            if (this.mProcStateTimeMs != null) {
                for (LongSamplingCounterArray counters : this.mProcStateTimeMs) {
                    if (counters != null) {
                        counters.reset(false);
                    }
                }
            }
            if (this.mProcStateScreenOffTimeMs != null) {
                for (LongSamplingCounterArray counters2 : this.mProcStateScreenOffTimeMs) {
                    if (counters2 != null) {
                        counters2.reset(false);
                    }
                }
            }
            BatteryStatsImpl.resetLongCounterIfNotNull(this.mMobileRadioApWakeupCount, false);
            BatteryStatsImpl.resetLongCounterIfNotNull(this.mWifiRadioApWakeupCount, false);
            ArrayMap<String, Wakelock> wakeStats = this.mWakelockStats.getMap();
            for (size3 = wakeStats.size() - 1; size3 >= 0; size3--) {
                if (((Wakelock) wakeStats.valueAt(size3)).reset()) {
                    wakeStats.removeAt(size3);
                } else {
                    active2 = true;
                }
            }
            this.mWakelockStats.cleanup();
            ArrayMap<String, DualTimer> syncStats = this.mSyncStats.getMap();
            for (size = syncStats.size() - 1; size >= 0; size--) {
                DualTimer timer = (DualTimer) syncStats.valueAt(size);
                if (timer.reset(false)) {
                    syncStats.removeAt(size);
                    timer.detach();
                } else {
                    active2 = true;
                }
            }
            this.mSyncStats.cleanup();
            ArrayMap<String, DualTimer> jobStats = this.mJobStats.getMap();
            for (size2 = jobStats.size() - 1; size2 >= 0; size2--) {
                DualTimer timer2 = (DualTimer) jobStats.valueAt(size2);
                if (timer2.reset(false)) {
                    jobStats.removeAt(size2);
                    timer2.detach();
                } else {
                    active2 = true;
                }
            }
            this.mJobStats.cleanup();
            this.mJobCompletions.clear();
            this.mJobsDeferredEventCount.reset(false);
            this.mJobsDeferredCount.reset(false);
            this.mJobsFreshnessTimeMs.reset(false);
            for (size2 = 0; size2 < BatteryStats.JOB_FRESHNESS_BUCKETS.length; size2++) {
                if (this.mJobsFreshnessBuckets[size2] != null) {
                    this.mJobsFreshnessBuckets[size2].reset(false);
                }
            }
            for (size2 = this.mSensorStats.size() - 1; size2 >= 0; size2--) {
                if (((Sensor) this.mSensorStats.valueAt(size2)).reset()) {
                    this.mSensorStats.removeAt(size2);
                } else {
                    active2 = true;
                }
            }
            for (size2 = this.mProcessStats.size() - 1; size2 >= 0; size2--) {
                ((Proc) this.mProcessStats.valueAt(size2)).detach();
            }
            this.mProcessStats.clear();
            if (this.mPids.size() > 0) {
                for (size2 = this.mPids.size() - 1; size2 >= 0; size2--) {
                    if (((Pid) this.mPids.valueAt(size2)).mWakeNesting > 0) {
                        active2 = true;
                    } else {
                        this.mPids.removeAt(size2);
                    }
                }
            }
            if (this.mPackageStats.size() > 0) {
                for (Entry<String, Pkg> pkgEntry : this.mPackageStats.entrySet()) {
                    Pkg p = (Pkg) pkgEntry.getValue();
                    p.detach();
                    if (p.mServiceStats.size() > 0) {
                        for (Entry<String, Serv> servEntry : p.mServiceStats.entrySet()) {
                            ((Serv) servEntry.getValue()).detach();
                        }
                    }
                }
                this.mPackageStats.clear();
            }
            this.mLastStepSystemTime = 0;
            this.mLastStepUserTime = 0;
            this.mCurStepSystemTime = 0;
            this.mCurStepUserTime = 0;
            if (!active2) {
                int i3;
                StopwatchTimer stopwatchTimer;
                if (this.mWifiRunningTimer != null) {
                    this.mWifiRunningTimer.detach();
                }
                if (this.mFullWifiLockTimer != null) {
                    this.mFullWifiLockTimer.detach();
                }
                if (this.mWifiScanTimer != null) {
                    this.mWifiScanTimer.detach();
                }
                for (i3 = 0; i3 < 5; i3++) {
                    if (this.mWifiBatchedScanTimer[i3] != null) {
                        this.mWifiBatchedScanTimer[i3].detach();
                    }
                }
                if (this.mWifiMulticastTimer != null) {
                    this.mWifiMulticastTimer.detach();
                }
                if (this.mAudioTurnedOnTimer != null) {
                    this.mAudioTurnedOnTimer.detach();
                    stopwatchTimer = null;
                    this.mAudioTurnedOnTimer = null;
                } else {
                    stopwatchTimer = null;
                }
                if (this.mVideoTurnedOnTimer != null) {
                    this.mVideoTurnedOnTimer.detach();
                    this.mVideoTurnedOnTimer = stopwatchTimer;
                }
                if (this.mFlashlightTurnedOnTimer != null) {
                    this.mFlashlightTurnedOnTimer.detach();
                    this.mFlashlightTurnedOnTimer = stopwatchTimer;
                }
                if (this.mCameraTurnedOnTimer != null) {
                    this.mCameraTurnedOnTimer.detach();
                    this.mCameraTurnedOnTimer = stopwatchTimer;
                }
                if (this.mForegroundActivityTimer != null) {
                    this.mForegroundActivityTimer.detach();
                    this.mForegroundActivityTimer = stopwatchTimer;
                }
                if (this.mForegroundServiceTimer != null) {
                    this.mForegroundServiceTimer.detach();
                    this.mForegroundServiceTimer = stopwatchTimer;
                }
                if (this.mAggregatedPartialWakelockTimer != null) {
                    this.mAggregatedPartialWakelockTimer.detach();
                    this.mAggregatedPartialWakelockTimer = stopwatchTimer;
                }
                if (this.mBluetoothScanTimer != null) {
                    this.mBluetoothScanTimer.detach();
                    this.mBluetoothScanTimer = stopwatchTimer;
                }
                if (this.mBluetoothUnoptimizedScanTimer != null) {
                    this.mBluetoothUnoptimizedScanTimer.detach();
                    this.mBluetoothUnoptimizedScanTimer = stopwatchTimer;
                }
                if (this.mBluetoothScanResultCounter != null) {
                    this.mBluetoothScanResultCounter.detach();
                    this.mBluetoothScanResultCounter = stopwatchTimer;
                }
                if (this.mBluetoothScanResultBgCounter != null) {
                    this.mBluetoothScanResultBgCounter.detach();
                    this.mBluetoothScanResultBgCounter = stopwatchTimer;
                }
                if (this.mUserActivityCounters != null) {
                    for (i3 = 0; i3 < 4; i3++) {
                        this.mUserActivityCounters[i3].detach();
                    }
                }
                if (this.mNetworkByteActivityCounters != null) {
                    for (i3 = 0; i3 < 10; i3++) {
                        this.mNetworkByteActivityCounters[i3].detach();
                        this.mNetworkPacketActivityCounters[i3].detach();
                    }
                }
                if (this.mWifiControllerActivity != null) {
                    this.mWifiControllerActivity.detach();
                }
                if (this.mBluetoothControllerActivity != null) {
                    this.mBluetoothControllerActivity.detach();
                }
                if (this.mModemControllerActivity != null) {
                    this.mModemControllerActivity.detach();
                }
                this.mPids.clear();
                this.mUserCpuTime.detach();
                this.mSystemCpuTime.detach();
                if (this.mCpuClusterSpeedTimesUs != null) {
                    for (LongSamplingCounter[] cpuSpeeds : this.mCpuClusterSpeedTimesUs) {
                        if (cpuSpeeds != null) {
                            for (LongSamplingCounter c : cpuSpeeds) {
                                if (c != null) {
                                    c.detach();
                                }
                            }
                        }
                    }
                }
                if (this.mCpuFreqTimeMs != null) {
                    this.mCpuFreqTimeMs.detach();
                }
                if (this.mScreenOffCpuFreqTimeMs != null) {
                    this.mScreenOffCpuFreqTimeMs.detach();
                }
                this.mCpuActiveTimeMs.detach();
                this.mCpuClusterTimesMs.detach();
                if (this.mProcStateTimeMs != null) {
                    for (LongSamplingCounterArray counters22 : this.mProcStateTimeMs) {
                        if (counters22 != null) {
                            counters22.detach();
                        }
                    }
                }
                if (this.mProcStateScreenOffTimeMs != null) {
                    for (LongSamplingCounterArray counters222 : this.mProcStateScreenOffTimeMs) {
                        if (counters222 != null) {
                            counters222.detach();
                        }
                    }
                }
                BatteryStatsImpl.detachLongCounterIfNotNull(this.mMobileRadioApWakeupCount);
                BatteryStatsImpl.detachLongCounterIfNotNull(this.mWifiRadioApWakeupCount);
            }
            return !active2;
        }

        void writeJobCompletionsToParcelLocked(Parcel out) {
            int NJC = this.mJobCompletions.size();
            out.writeInt(NJC);
            for (int ijc = 0; ijc < NJC; ijc++) {
                out.writeString((String) this.mJobCompletions.keyAt(ijc));
                SparseIntArray types = (SparseIntArray) this.mJobCompletions.valueAt(ijc);
                int NT = types.size();
                out.writeInt(NT);
                for (int it = 0; it < NT; it++) {
                    out.writeInt(types.keyAt(it));
                    out.writeInt(types.valueAt(it));
                }
            }
        }

        void writeToParcelLocked(Parcel out, long uptimeUs, long elapsedRealtimeUs) {
            int ij;
            int ise;
            int ip;
            Parcel parcel = out;
            long j = elapsedRealtimeUs;
            Parcel parcel2 = parcel;
            long j2 = uptimeUs;
            long j3 = j;
            this.mOnBatteryBackgroundTimeBase.writeToParcel(parcel2, j2, j3);
            this.mOnBatteryScreenOffBackgroundTimeBase.writeToParcel(parcel2, j2, j3);
            ArrayMap<String, Wakelock> wakeStats = this.mWakelockStats.getMap();
            int NW = wakeStats.size();
            parcel.writeInt(NW);
            int i = 0;
            for (int iw = 0; iw < NW; iw++) {
                parcel.writeString((String) wakeStats.keyAt(iw));
                ((Wakelock) wakeStats.valueAt(iw)).writeToParcelLocked(parcel, j);
            }
            ArrayMap<String, DualTimer> syncStats = this.mSyncStats.getMap();
            int NS = syncStats.size();
            parcel.writeInt(NS);
            for (int is = 0; is < NS; is++) {
                parcel.writeString((String) syncStats.keyAt(is));
                Timer.writeTimerToParcel(parcel, (DualTimer) syncStats.valueAt(is), j);
            }
            ArrayMap<String, DualTimer> jobStats = this.mJobStats.getMap();
            int NJ = jobStats.size();
            parcel.writeInt(NJ);
            for (ij = 0; ij < NJ; ij++) {
                parcel.writeString((String) jobStats.keyAt(ij));
                Timer.writeTimerToParcel(parcel, (DualTimer) jobStats.valueAt(ij), j);
            }
            writeJobCompletionsToParcelLocked(out);
            this.mJobsDeferredEventCount.writeToParcel(parcel);
            this.mJobsDeferredCount.writeToParcel(parcel);
            this.mJobsFreshnessTimeMs.writeToParcel(parcel);
            for (ij = 0; ij < BatteryStats.JOB_FRESHNESS_BUCKETS.length; ij++) {
                Counter.writeCounterToParcel(parcel, this.mJobsFreshnessBuckets[ij]);
            }
            ij = this.mSensorStats.size();
            parcel.writeInt(ij);
            for (ise = 0; ise < ij; ise++) {
                parcel.writeInt(this.mSensorStats.keyAt(ise));
                ((Sensor) this.mSensorStats.valueAt(ise)).writeToParcelLocked(parcel, j);
            }
            ise = this.mProcessStats.size();
            parcel.writeInt(ise);
            for (ip = 0; ip < ise; ip++) {
                parcel.writeString((String) this.mProcessStats.keyAt(ip));
                ((Proc) this.mProcessStats.valueAt(ip)).writeToParcelLocked(parcel);
            }
            parcel.writeInt(this.mPackageStats.size());
            for (Entry<String, Pkg> pkgEntry : this.mPackageStats.entrySet()) {
                parcel.writeString((String) pkgEntry.getKey());
                ((Pkg) pkgEntry.getValue()).writeToParcelLocked(parcel);
            }
            if (this.mWifiRunningTimer != null) {
                parcel.writeInt(1);
                this.mWifiRunningTimer.writeToParcel(parcel, j);
            } else {
                parcel.writeInt(0);
            }
            if (this.mFullWifiLockTimer != null) {
                parcel.writeInt(1);
                this.mFullWifiLockTimer.writeToParcel(parcel, j);
            } else {
                parcel.writeInt(0);
            }
            if (this.mWifiScanTimer != null) {
                parcel.writeInt(1);
                this.mWifiScanTimer.writeToParcel(parcel, j);
            } else {
                parcel.writeInt(0);
            }
            for (ip = 0; ip < 5; ip++) {
                if (this.mWifiBatchedScanTimer[ip] != null) {
                    parcel.writeInt(1);
                    this.mWifiBatchedScanTimer[ip].writeToParcel(parcel, j);
                } else {
                    parcel.writeInt(0);
                }
            }
            if (this.mWifiMulticastTimer != null) {
                parcel.writeInt(1);
                this.mWifiMulticastTimer.writeToParcel(parcel, j);
            } else {
                parcel.writeInt(0);
            }
            if (this.mAudioTurnedOnTimer != null) {
                parcel.writeInt(1);
                this.mAudioTurnedOnTimer.writeToParcel(parcel, j);
            } else {
                parcel.writeInt(0);
            }
            if (this.mVideoTurnedOnTimer != null) {
                parcel.writeInt(1);
                this.mVideoTurnedOnTimer.writeToParcel(parcel, j);
            } else {
                parcel.writeInt(0);
            }
            if (this.mFlashlightTurnedOnTimer != null) {
                parcel.writeInt(1);
                this.mFlashlightTurnedOnTimer.writeToParcel(parcel, j);
            } else {
                parcel.writeInt(0);
            }
            if (this.mCameraTurnedOnTimer != null) {
                parcel.writeInt(1);
                this.mCameraTurnedOnTimer.writeToParcel(parcel, j);
            } else {
                parcel.writeInt(0);
            }
            if (this.mForegroundActivityTimer != null) {
                parcel.writeInt(1);
                this.mForegroundActivityTimer.writeToParcel(parcel, j);
            } else {
                parcel.writeInt(0);
            }
            if (this.mForegroundServiceTimer != null) {
                parcel.writeInt(1);
                this.mForegroundServiceTimer.writeToParcel(parcel, j);
            } else {
                parcel.writeInt(0);
            }
            if (this.mAggregatedPartialWakelockTimer != null) {
                parcel.writeInt(1);
                this.mAggregatedPartialWakelockTimer.writeToParcel(parcel, j);
            } else {
                parcel.writeInt(0);
            }
            if (this.mBluetoothScanTimer != null) {
                parcel.writeInt(1);
                this.mBluetoothScanTimer.writeToParcel(parcel, j);
            } else {
                parcel.writeInt(0);
            }
            if (this.mBluetoothUnoptimizedScanTimer != null) {
                parcel.writeInt(1);
                this.mBluetoothUnoptimizedScanTimer.writeToParcel(parcel, j);
            } else {
                parcel.writeInt(0);
            }
            if (this.mBluetoothScanResultCounter != null) {
                parcel.writeInt(1);
                this.mBluetoothScanResultCounter.writeToParcel(parcel);
            } else {
                parcel.writeInt(0);
            }
            if (this.mBluetoothScanResultBgCounter != null) {
                parcel.writeInt(1);
                this.mBluetoothScanResultBgCounter.writeToParcel(parcel);
            } else {
                parcel.writeInt(0);
            }
            for (ip = 0; ip < 7; ip++) {
                if (this.mProcessStateTimer[ip] != null) {
                    parcel.writeInt(1);
                    this.mProcessStateTimer[ip].writeToParcel(parcel, j);
                } else {
                    parcel.writeInt(0);
                }
            }
            if (this.mVibratorOnTimer != null) {
                parcel.writeInt(1);
                this.mVibratorOnTimer.writeToParcel(parcel, j);
            } else {
                parcel.writeInt(0);
            }
            if (this.mUserActivityCounters != null) {
                parcel.writeInt(1);
                for (ip = 0; ip < 4; ip++) {
                    this.mUserActivityCounters[ip].writeToParcel(parcel);
                }
            } else {
                parcel.writeInt(0);
            }
            if (this.mNetworkByteActivityCounters != null) {
                parcel.writeInt(1);
                for (ip = 0; ip < 10; ip++) {
                    this.mNetworkByteActivityCounters[ip].writeToParcel(parcel);
                    this.mNetworkPacketActivityCounters[ip].writeToParcel(parcel);
                }
                this.mMobileRadioActiveTime.writeToParcel(parcel);
                this.mMobileRadioActiveCount.writeToParcel(parcel);
            } else {
                parcel.writeInt(0);
            }
            if (this.mWifiControllerActivity != null) {
                parcel.writeInt(1);
                this.mWifiControllerActivity.writeToParcel(parcel, 0);
            } else {
                parcel.writeInt(0);
            }
            if (this.mBluetoothControllerActivity != null) {
                parcel.writeInt(1);
                this.mBluetoothControllerActivity.writeToParcel(parcel, 0);
            } else {
                parcel.writeInt(0);
            }
            if (this.mModemControllerActivity != null) {
                parcel.writeInt(1);
                this.mModemControllerActivity.writeToParcel(parcel, 0);
            } else {
                parcel.writeInt(0);
            }
            this.mUserCpuTime.writeToParcel(parcel);
            this.mSystemCpuTime.writeToParcel(parcel);
            int NW2;
            ArrayMap<String, DualTimer> syncStats2;
            if (this.mCpuClusterSpeedTimesUs != null) {
                parcel.writeInt(1);
                parcel.writeInt(this.mCpuClusterSpeedTimesUs.length);
                LongSamplingCounter[][] longSamplingCounterArr = this.mCpuClusterSpeedTimesUs;
                int length = longSamplingCounterArr.length;
                while (i < length) {
                    ArrayMap<String, Wakelock> wakeStats2;
                    LongSamplingCounter[] cpuSpeeds = longSamplingCounterArr[i];
                    if (cpuSpeeds != null) {
                        wakeStats2 = wakeStats;
                        parcel.writeInt(1);
                        parcel.writeInt(cpuSpeeds.length);
                        wakeStats = cpuSpeeds.length;
                        NW2 = NW;
                        NW = 0;
                        while (NW < wakeStats) {
                            ArrayMap<String, Wakelock> arrayMap = wakeStats;
                            wakeStats = cpuSpeeds[NW];
                            if (wakeStats != null) {
                                syncStats2 = syncStats;
                                parcel.writeInt(1);
                                wakeStats.writeToParcel(parcel);
                            } else {
                                syncStats2 = syncStats;
                                parcel.writeInt(0);
                            }
                            NW++;
                            wakeStats = arrayMap;
                            syncStats = syncStats2;
                        }
                        syncStats2 = syncStats;
                    } else {
                        wakeStats2 = wakeStats;
                        NW2 = NW;
                        syncStats2 = syncStats;
                        parcel.writeInt(null);
                    }
                    i++;
                    wakeStats = wakeStats2;
                    NW = NW2;
                    syncStats = syncStats2;
                }
                NW2 = NW;
                syncStats2 = syncStats;
            } else {
                NW2 = NW;
                syncStats2 = syncStats;
                parcel.writeInt(null);
            }
            LongSamplingCounterArray.writeToParcel(parcel, this.mCpuFreqTimeMs);
            LongSamplingCounterArray.writeToParcel(parcel, this.mScreenOffCpuFreqTimeMs);
            this.mCpuActiveTimeMs.writeToParcel(parcel);
            this.mCpuClusterTimesMs.writeToParcel(parcel);
            if (this.mProcStateTimeMs != null) {
                parcel.writeInt(this.mProcStateTimeMs.length);
                for (LongSamplingCounterArray counters : this.mProcStateTimeMs) {
                    LongSamplingCounterArray.writeToParcel(parcel, counters);
                }
            } else {
                parcel.writeInt(0);
            }
            if (this.mProcStateScreenOffTimeMs != null) {
                parcel.writeInt(this.mProcStateScreenOffTimeMs.length);
                for (LongSamplingCounterArray counters2 : this.mProcStateScreenOffTimeMs) {
                    LongSamplingCounterArray.writeToParcel(parcel, counters2);
                }
            } else {
                parcel.writeInt(0);
            }
            if (this.mMobileRadioApWakeupCount != null) {
                parcel.writeInt(1);
                this.mMobileRadioApWakeupCount.writeToParcel(parcel);
            } else {
                parcel.writeInt(0);
            }
            if (this.mWifiRadioApWakeupCount != null) {
                parcel.writeInt(1);
                this.mWifiRadioApWakeupCount.writeToParcel(parcel);
                return;
            }
            parcel.writeInt(0);
        }

        void readJobCompletionsFromParcelLocked(Parcel in) {
            int numJobCompletions = in.readInt();
            this.mJobCompletions.clear();
            for (int j = 0; j < numJobCompletions; j++) {
                String jobName = in.readString();
                int numTypes = in.readInt();
                if (numTypes > 0) {
                    SparseIntArray types = new SparseIntArray();
                    for (int k = 0; k < numTypes; k++) {
                        types.put(in.readInt(), in.readInt());
                    }
                    this.mJobCompletions.put(jobName, types);
                }
            }
        }

        void readFromParcelLocked(TimeBase timeBase, TimeBase screenOffTimeBase, Parcel in) {
            int j;
            String wakelockName;
            int j2;
            DualTimer dualTimer;
            int j3;
            int sensorNumber;
            StopwatchTimer stopwatchTimer;
            StopwatchTimer stopwatchTimer2;
            StopwatchTimer stopwatchTimer3;
            int procState;
            Parcel parcel = in;
            this.mOnBatteryBackgroundTimeBase.readFromParcel(parcel);
            this.mOnBatteryScreenOffBackgroundTimeBase.readFromParcel(parcel);
            int numWakelocks = in.readInt();
            this.mWakelockStats.clear();
            for (j = 0; j < numWakelocks; j++) {
                wakelockName = in.readString();
                Wakelock wakelock = new Wakelock(this.mBsi, this);
                wakelock.readFromParcelLocked(timeBase, screenOffTimeBase, this.mOnBatteryScreenOffBackgroundTimeBase, parcel);
                this.mWakelockStats.add(wakelockName, wakelock);
            }
            TimeBase timeBase2 = timeBase;
            TimeBase timeBase3 = screenOffTimeBase;
            int numSyncs = in.readInt();
            this.mSyncStats.clear();
            j = 0;
            while (true) {
                j2 = j;
                if (j2 >= numSyncs) {
                    break;
                }
                int numWakelocks2;
                String syncName = in.readString();
                if (in.readInt() != 0) {
                    OverflowArrayMap overflowArrayMap = this.mSyncStats;
                    DualTimer dualTimer2 = dualTimer;
                    OverflowArrayMap overflowArrayMap2 = overflowArrayMap;
                    numWakelocks2 = numWakelocks;
                    numWakelocks = syncName;
                    dualTimer = new DualTimer(this.mBsi.mClocks, this, 13, null, this.mBsi.mOnBatteryTimeBase, this.mOnBatteryBackgroundTimeBase, parcel);
                    overflowArrayMap2.add(numWakelocks, dualTimer2);
                } else {
                    numWakelocks2 = numWakelocks;
                }
                j = j2 + 1;
                numWakelocks = numWakelocks2;
            }
            numWakelocks = in.readInt();
            this.mJobStats.clear();
            j = 0;
            while (true) {
                j3 = j;
                if (j3 >= numWakelocks) {
                    break;
                }
                int numJobs;
                String jobName = in.readString();
                if (in.readInt() != 0) {
                    OverflowArrayMap overflowArrayMap3 = this.mJobStats;
                    DualTimer dualTimer3 = dualTimer;
                    numJobs = numWakelocks;
                    OverflowArrayMap overflowArrayMap4 = overflowArrayMap3;
                    dualTimer = new DualTimer(this.mBsi.mClocks, this, 14, null, this.mBsi.mOnBatteryTimeBase, this.mOnBatteryBackgroundTimeBase, parcel);
                    overflowArrayMap4.add(jobName, dualTimer3);
                } else {
                    numJobs = numWakelocks;
                }
                j = j3 + 1;
                numWakelocks = numJobs;
            }
            readJobCompletionsFromParcelLocked(parcel);
            this.mJobsDeferredEventCount = new Counter(this.mBsi.mOnBatteryTimeBase, parcel);
            this.mJobsDeferredCount = new Counter(this.mBsi.mOnBatteryTimeBase, parcel);
            this.mJobsFreshnessTimeMs = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase, parcel);
            for (j = 0; j < BatteryStats.JOB_FRESHNESS_BUCKETS.length; j++) {
                this.mJobsFreshnessBuckets[j] = Counter.readCounterFromParcel(this.mBsi.mOnBatteryTimeBase, parcel);
            }
            numWakelocks = in.readInt();
            this.mSensorStats.clear();
            for (j = 0; j < numWakelocks; j++) {
                sensorNumber = in.readInt();
                Sensor sensor = new Sensor(this.mBsi, this, sensorNumber);
                sensor.readFromParcelLocked(this.mBsi.mOnBatteryTimeBase, this.mOnBatteryBackgroundTimeBase, parcel);
                this.mSensorStats.put(sensorNumber, sensor);
            }
            j3 = in.readInt();
            this.mProcessStats.clear();
            for (j = 0; j < j3; j++) {
                wakelockName = in.readString();
                Proc proc = new Proc(this.mBsi, wakelockName);
                proc.readFromParcelLocked(parcel);
                this.mProcessStats.put(wakelockName, proc);
            }
            j2 = in.readInt();
            this.mPackageStats.clear();
            for (j = 0; j < j2; j++) {
                wakelockName = in.readString();
                Pkg pkg = new Pkg(this.mBsi);
                pkg.readFromParcelLocked(parcel);
                this.mPackageStats.put(wakelockName, pkg);
            }
            this.mWifiRunning = false;
            if (in.readInt() != 0) {
                stopwatchTimer = stopwatchTimer2;
                stopwatchTimer2 = new StopwatchTimer(this.mBsi.mClocks, this, 4, this.mBsi.mWifiRunningTimers, this.mBsi.mOnBatteryTimeBase, parcel);
                this.mWifiRunningTimer = stopwatchTimer;
            } else {
                this.mWifiRunningTimer = null;
            }
            this.mFullWifiLockOut = false;
            if (in.readInt() != 0) {
                this.mFullWifiLockTimer = new StopwatchTimer(this.mBsi.mClocks, this, 5, this.mBsi.mFullWifiLockTimers, this.mBsi.mOnBatteryTimeBase, parcel);
                stopwatchTimer = null;
            } else {
                stopwatchTimer = null;
                this.mFullWifiLockTimer = null;
            }
            this.mWifiScanStarted = false;
            if (in.readInt() != 0) {
                DualTimer dualTimer4 = dualTimer;
                stopwatchTimer3 = stopwatchTimer;
                dualTimer = new DualTimer(this.mBsi.mClocks, this, 6, this.mBsi.mWifiScanTimers, this.mBsi.mOnBatteryTimeBase, this.mOnBatteryBackgroundTimeBase, parcel);
                this.mWifiScanTimer = dualTimer4;
            } else {
                stopwatchTimer3 = stopwatchTimer;
                this.mWifiScanTimer = stopwatchTimer3;
            }
            this.mWifiBatchedScanBinStarted = -1;
            for (j = 0; j < 5; j++) {
                if (in.readInt() != 0) {
                    makeWifiBatchedScanBin(j, parcel);
                } else {
                    this.mWifiBatchedScanTimer[j] = stopwatchTimer3;
                }
            }
            this.mWifiMulticastEnabled = false;
            if (in.readInt() != 0) {
                Clocks clocks = this.mBsi.mClocks;
                ArrayList arrayList = this.mBsi.mWifiMulticastTimers;
                stopwatchTimer = stopwatchTimer2;
                TimeBase timeBase4 = this.mBsi.mOnBatteryTimeBase;
                procState = false;
                stopwatchTimer2 = new StopwatchTimer(clocks, this, 7, arrayList, timeBase4, parcel);
                this.mWifiMulticastTimer = stopwatchTimer;
            } else {
                procState = false;
                this.mWifiMulticastTimer = stopwatchTimer3;
            }
            if (in.readInt() != 0) {
                this.mAudioTurnedOnTimer = new StopwatchTimer(this.mBsi.mClocks, this, 15, this.mBsi.mAudioTurnedOnTimers, this.mBsi.mOnBatteryTimeBase, parcel);
            } else {
                this.mAudioTurnedOnTimer = stopwatchTimer3;
            }
            if (in.readInt() != 0) {
                this.mVideoTurnedOnTimer = new StopwatchTimer(this.mBsi.mClocks, this, 8, this.mBsi.mVideoTurnedOnTimers, this.mBsi.mOnBatteryTimeBase, parcel);
            } else {
                this.mVideoTurnedOnTimer = stopwatchTimer3;
            }
            if (in.readInt() != 0) {
                this.mFlashlightTurnedOnTimer = new StopwatchTimer(this.mBsi.mClocks, this, 16, this.mBsi.mFlashlightTurnedOnTimers, this.mBsi.mOnBatteryTimeBase, parcel);
            } else {
                this.mFlashlightTurnedOnTimer = stopwatchTimer3;
            }
            if (in.readInt() != 0) {
                this.mCameraTurnedOnTimer = new StopwatchTimer(this.mBsi.mClocks, this, 17, this.mBsi.mCameraTurnedOnTimers, this.mBsi.mOnBatteryTimeBase, parcel);
            } else {
                this.mCameraTurnedOnTimer = stopwatchTimer3;
            }
            if (in.readInt() != 0) {
                this.mForegroundActivityTimer = new StopwatchTimer(this.mBsi.mClocks, this, 10, null, this.mBsi.mOnBatteryTimeBase, parcel);
            } else {
                this.mForegroundActivityTimer = stopwatchTimer3;
            }
            if (in.readInt() != 0) {
                this.mForegroundServiceTimer = new StopwatchTimer(this.mBsi.mClocks, this, 22, null, this.mBsi.mOnBatteryTimeBase, parcel);
            } else {
                this.mForegroundServiceTimer = stopwatchTimer3;
            }
            if (in.readInt() != 0) {
                DualTimer dualTimer5 = dualTimer;
                j3 = 5;
                dualTimer = new DualTimer(this.mBsi.mClocks, this, 20, null, this.mBsi.mOnBatteryScreenOffTimeBase, this.mOnBatteryScreenOffBackgroundTimeBase, parcel);
                this.mAggregatedPartialWakelockTimer = dualTimer5;
            } else {
                j3 = 5;
                this.mAggregatedPartialWakelockTimer = null;
            }
            if (in.readInt() != 0) {
                this.mBluetoothScanTimer = new DualTimer(this.mBsi.mClocks, this, 19, this.mBsi.mBluetoothScanOnTimers, this.mBsi.mOnBatteryTimeBase, this.mOnBatteryBackgroundTimeBase, parcel);
            } else {
                this.mBluetoothScanTimer = null;
            }
            if (in.readInt() != 0) {
                this.mBluetoothUnoptimizedScanTimer = new DualTimer(this.mBsi.mClocks, this, 21, null, this.mBsi.mOnBatteryTimeBase, this.mOnBatteryBackgroundTimeBase, parcel);
            } else {
                this.mBluetoothUnoptimizedScanTimer = null;
            }
            if (in.readInt() != 0) {
                this.mBluetoothScanResultCounter = new Counter(this.mBsi.mOnBatteryTimeBase, parcel);
            } else {
                this.mBluetoothScanResultCounter = null;
            }
            if (in.readInt() != 0) {
                this.mBluetoothScanResultBgCounter = new Counter(this.mOnBatteryBackgroundTimeBase, parcel);
            } else {
                this.mBluetoothScanResultBgCounter = null;
            }
            this.mProcessState = 19;
            for (j = procState; j < 7; j++) {
                if (in.readInt() != 0) {
                    makeProcessState(j, parcel);
                } else {
                    this.mProcessStateTimer[j] = null;
                }
            }
            if (in.readInt() != 0) {
                this.mVibratorOnTimer = new BatchTimer(this.mBsi.mClocks, this, 9, this.mBsi.mOnBatteryTimeBase, parcel);
            } else {
                this.mVibratorOnTimer = null;
            }
            if (in.readInt() != 0) {
                this.mUserActivityCounters = new Counter[4];
                for (sensorNumber = procState; sensorNumber < 4; sensorNumber++) {
                    this.mUserActivityCounters[sensorNumber] = new Counter(this.mBsi.mOnBatteryTimeBase, parcel);
                }
            } else {
                this.mUserActivityCounters = null;
            }
            if (in.readInt() != 0) {
                this.mNetworkByteActivityCounters = new LongSamplingCounter[10];
                this.mNetworkPacketActivityCounters = new LongSamplingCounter[10];
                for (sensorNumber = procState; sensorNumber < 10; sensorNumber++) {
                    this.mNetworkByteActivityCounters[sensorNumber] = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase, parcel);
                    this.mNetworkPacketActivityCounters[sensorNumber] = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase, parcel);
                }
                this.mMobileRadioActiveTime = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase, parcel);
                this.mMobileRadioActiveCount = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase, parcel);
            } else {
                this.mNetworkByteActivityCounters = null;
                this.mNetworkPacketActivityCounters = null;
            }
            if (in.readInt() != 0) {
                this.mWifiControllerActivity = new ControllerActivityCounterImpl(this.mBsi.mOnBatteryTimeBase, 1, parcel);
            } else {
                this.mWifiControllerActivity = null;
            }
            if (in.readInt() != 0) {
                this.mBluetoothControllerActivity = new ControllerActivityCounterImpl(this.mBsi.mOnBatteryTimeBase, 1, parcel);
            } else {
                this.mBluetoothControllerActivity = null;
            }
            if (in.readInt() != 0) {
                this.mModemControllerActivity = new ControllerActivityCounterImpl(this.mBsi.mOnBatteryTimeBase, j3, parcel);
            } else {
                this.mModemControllerActivity = null;
            }
            this.mUserCpuTime = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase, parcel);
            this.mSystemCpuTime = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase, parcel);
            if (in.readInt() != 0) {
                j = in.readInt();
                if (this.mBsi.mPowerProfile == null || this.mBsi.mPowerProfile.getNumCpuClusters() == j) {
                    this.mCpuClusterSpeedTimesUs = new LongSamplingCounter[j][];
                    sensorNumber = procState;
                    while (sensorNumber < j) {
                        if (in.readInt() != 0) {
                            int numSpeeds = in.readInt();
                            if (this.mBsi.mPowerProfile == null || this.mBsi.mPowerProfile.getNumSpeedStepsInCpuCluster(sensorNumber) == numSpeeds) {
                                LongSamplingCounter[] cpuSpeeds = new LongSamplingCounter[numSpeeds];
                                this.mCpuClusterSpeedTimesUs[sensorNumber] = cpuSpeeds;
                                for (int speed = procState; speed < numSpeeds; speed++) {
                                    if (in.readInt() != 0) {
                                        cpuSpeeds[speed] = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase, parcel);
                                    }
                                }
                            } else {
                                throw new ParcelFormatException("Incompatible number of cpu speeds");
                            }
                        }
                        this.mCpuClusterSpeedTimesUs[sensorNumber] = null;
                        sensorNumber++;
                    }
                } else {
                    throw new ParcelFormatException("Incompatible number of cpu clusters");
                }
            }
            this.mCpuClusterSpeedTimesUs = null;
            this.mCpuFreqTimeMs = LongSamplingCounterArray.readFromParcel(parcel, this.mBsi.mOnBatteryTimeBase);
            this.mScreenOffCpuFreqTimeMs = LongSamplingCounterArray.readFromParcel(parcel, this.mBsi.mOnBatteryScreenOffTimeBase);
            this.mCpuActiveTimeMs = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase, parcel);
            this.mCpuClusterTimesMs = new LongSamplingCounterArray(this.mBsi.mOnBatteryTimeBase, parcel, null);
            j = in.readInt();
            if (j == 7) {
                this.mProcStateTimeMs = new LongSamplingCounterArray[j];
                for (sensorNumber = procState; sensorNumber < j; sensorNumber++) {
                    this.mProcStateTimeMs[sensorNumber] = LongSamplingCounterArray.readFromParcel(parcel, this.mBsi.mOnBatteryTimeBase);
                }
            } else {
                this.mProcStateTimeMs = null;
            }
            j = in.readInt();
            if (j == 7) {
                this.mProcStateScreenOffTimeMs = new LongSamplingCounterArray[j];
                while (true) {
                    sensorNumber = procState;
                    if (sensorNumber >= j) {
                        break;
                    }
                    this.mProcStateScreenOffTimeMs[sensorNumber] = LongSamplingCounterArray.readFromParcel(parcel, this.mBsi.mOnBatteryScreenOffTimeBase);
                    procState = sensorNumber + 1;
                }
            } else {
                this.mProcStateScreenOffTimeMs = null;
            }
            if (in.readInt() != 0) {
                this.mMobileRadioApWakeupCount = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase, parcel);
            } else {
                this.mMobileRadioApWakeupCount = null;
            }
            if (in.readInt() != 0) {
                this.mWifiRadioApWakeupCount = new LongSamplingCounter(this.mBsi.mOnBatteryTimeBase, parcel);
            } else {
                this.mWifiRadioApWakeupCount = null;
            }
        }

        public void noteJobsDeferredLocked(int numDeferred, long sinceLast) {
            this.mJobsDeferredEventCount.addAtomic(1);
            this.mJobsDeferredCount.addAtomic(numDeferred);
            if (sinceLast != 0) {
                this.mJobsFreshnessTimeMs.addCountLocked(sinceLast);
                for (int i = 0; i < BatteryStats.JOB_FRESHNESS_BUCKETS.length; i++) {
                    if (sinceLast < BatteryStats.JOB_FRESHNESS_BUCKETS[i]) {
                        if (this.mJobsFreshnessBuckets[i] == null) {
                            this.mJobsFreshnessBuckets[i] = new Counter(this.mBsi.mOnBatteryTimeBase);
                        }
                        this.mJobsFreshnessBuckets[i].addAtomic(1);
                        return;
                    }
                }
            }
        }

        public Proc getProcessStatsLocked(String name) {
            Proc ps = (Proc) this.mProcessStats.get(name);
            if (ps != null) {
                return ps;
            }
            ps = new Proc(this.mBsi, name);
            this.mProcessStats.put(name, ps);
            return ps;
        }

        @GuardedBy("mBsi")
        public void updateUidProcessStateLocked(int procState) {
            boolean userAwareService = procState == 3;
            int uidRunningState = BatteryStats.mapToInternalProcessState(procState);
            if (this.mProcessState != uidRunningState || userAwareService != this.mInForegroundService) {
                long elapsedRealtimeMs = this.mBsi.mClocks.elapsedRealtime();
                if (this.mProcessState != uidRunningState) {
                    long uptimeMs = this.mBsi.mClocks.uptimeMillis();
                    if (this.mProcessState != 19) {
                        this.mProcessStateTimer[this.mProcessState].stopRunningLocked(elapsedRealtimeMs);
                        if (this.mBsi.trackPerProcStateCpuTimes()) {
                            if (this.mBsi.mPendingUids.size() == 0) {
                                this.mBsi.mExternalSync.scheduleReadProcStateCpuTimes(this.mBsi.mOnBatteryTimeBase.isRunning(), this.mBsi.mOnBatteryScreenOffTimeBase.isRunning(), this.mBsi.mConstants.PROC_STATE_CPU_TIMES_READ_DELAY_MS);
                                this.mBsi.mNumSingleUidCpuTimeReads = 1 + this.mBsi.mNumSingleUidCpuTimeReads;
                            } else {
                                this.mBsi.mNumBatchedSingleUidCpuTimeReads = 1 + this.mBsi.mNumBatchedSingleUidCpuTimeReads;
                            }
                            if (this.mBsi.mPendingUids.indexOfKey(this.mUid) < 0 || ArrayUtils.contains(CRITICAL_PROC_STATES, this.mProcessState)) {
                                this.mBsi.mPendingUids.put(this.mUid, this.mProcessState);
                            }
                        } else {
                            this.mBsi.mPendingUids.clear();
                        }
                    }
                    this.mProcessState = uidRunningState;
                    if (uidRunningState != 19) {
                        if (this.mProcessStateTimer[uidRunningState] == null) {
                            makeProcessState(uidRunningState, null);
                        }
                        this.mProcessStateTimer[uidRunningState].startRunningLocked(elapsedRealtimeMs);
                    }
                    updateOnBatteryBgTimeBase(uptimeMs * 1000, elapsedRealtimeMs * 1000);
                    updateOnBatteryScreenOffBgTimeBase(uptimeMs * 1000, 1000 * elapsedRealtimeMs);
                }
                if (userAwareService != this.mInForegroundService) {
                    if (userAwareService) {
                        noteForegroundServiceResumedLocked(elapsedRealtimeMs);
                    } else {
                        noteForegroundServicePausedLocked(elapsedRealtimeMs);
                    }
                    this.mInForegroundService = userAwareService;
                }
            }
        }

        public boolean isInBackground() {
            return this.mProcessState >= 3;
        }

        public boolean updateOnBatteryBgTimeBase(long uptimeUs, long realtimeUs) {
            boolean z = this.mBsi.mOnBatteryTimeBase.isRunning() && isInBackground();
            return this.mOnBatteryBackgroundTimeBase.setRunning(z, uptimeUs, realtimeUs);
        }

        public boolean updateOnBatteryScreenOffBgTimeBase(long uptimeUs, long realtimeUs) {
            boolean z = this.mBsi.mOnBatteryScreenOffTimeBase.isRunning() && isInBackground();
            return this.mOnBatteryScreenOffBackgroundTimeBase.setRunning(z, uptimeUs, realtimeUs);
        }

        public SparseArray<? extends Pid> getPidStats() {
            return this.mPids;
        }

        public Pid getPidStatsLocked(int pid) {
            Pid p = (Pid) this.mPids.get(pid);
            if (p != null) {
                return p;
            }
            p = new Pid(this);
            this.mPids.put(pid, p);
            return p;
        }

        public Pkg getPackageStatsLocked(String name) {
            Pkg ps = (Pkg) this.mPackageStats.get(name);
            if (ps != null) {
                return ps;
            }
            ps = new Pkg(this.mBsi);
            this.mPackageStats.put(name, ps);
            return ps;
        }

        public Serv getServiceStatsLocked(String pkg, String serv) {
            Pkg ps = getPackageStatsLocked(pkg);
            Serv ss = (Serv) ps.mServiceStats.get(serv);
            if (ss != null) {
                return ss;
            }
            ss = ps.newServiceStatsLocked();
            ps.mServiceStats.put(serv, ss);
            return ss;
        }

        public void readSyncSummaryFromParcelLocked(String name, Parcel in) {
            DualTimer timer = (DualTimer) this.mSyncStats.instantiateObject();
            timer.readSummaryFromParcelLocked(in);
            this.mSyncStats.add(name, timer);
        }

        public void readJobSummaryFromParcelLocked(String name, Parcel in) {
            DualTimer timer = (DualTimer) this.mJobStats.instantiateObject();
            timer.readSummaryFromParcelLocked(in);
            this.mJobStats.add(name, timer);
        }

        public void readWakeSummaryFromParcelLocked(String wlName, Parcel in) {
            Wakelock wl = new Wakelock(this.mBsi, this);
            this.mWakelockStats.add(wlName, wl);
            if (in.readInt() != 0) {
                getWakelockTimerLocked(wl, 1).readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                getWakelockTimerLocked(wl, 0).readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                getWakelockTimerLocked(wl, 2).readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                getWakelockTimerLocked(wl, 18).readSummaryFromParcelLocked(in);
            }
        }

        public DualTimer getSensorTimerLocked(int sensor, boolean create) {
            Sensor se = (Sensor) this.mSensorStats.get(sensor);
            if (se == null) {
                if (!create) {
                    return null;
                }
                se = new Sensor(this.mBsi, this, sensor);
                this.mSensorStats.put(sensor, se);
            }
            DualTimer t = se.mTimer;
            if (t != null) {
                return t;
            }
            ArrayList<StopwatchTimer> timers = (ArrayList) this.mBsi.mSensorTimers.get(sensor);
            if (timers == null) {
                timers = new ArrayList();
                this.mBsi.mSensorTimers.put(sensor, timers);
            }
            t = new DualTimer(this.mBsi.mClocks, this, 3, timers, this.mBsi.mOnBatteryTimeBase, this.mOnBatteryBackgroundTimeBase);
            se.mTimer = t;
            return t;
        }

        public void noteStartSyncLocked(String name, long elapsedRealtimeMs) {
            DualTimer t = (DualTimer) this.mSyncStats.startObject(name);
            if (t != null) {
                t.startRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteStopSyncLocked(String name, long elapsedRealtimeMs) {
            DualTimer t = (DualTimer) this.mSyncStats.stopObject(name);
            if (t != null) {
                t.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteStartJobLocked(String name, long elapsedRealtimeMs) {
            DualTimer t = (DualTimer) this.mJobStats.startObject(name);
            if (t != null) {
                t.startRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteStopJobLocked(String name, long elapsedRealtimeMs, int stopReason) {
            DualTimer t = (DualTimer) this.mJobStats.stopObject(name);
            if (t != null) {
                t.stopRunningLocked(elapsedRealtimeMs);
            }
            if (this.mBsi.mOnBatteryTimeBase.isRunning()) {
                SparseIntArray types = (SparseIntArray) this.mJobCompletions.get(name);
                if (types == null) {
                    types = new SparseIntArray();
                    this.mJobCompletions.put(name, types);
                }
                types.put(stopReason, types.get(stopReason, 0) + 1);
            }
        }

        public StopwatchTimer getWakelockTimerLocked(Wakelock wl, int type) {
            if (wl == null) {
                return null;
            }
            StopwatchTimer t;
            if (type != 18) {
                switch (type) {
                    case 0:
                        DualTimer t2 = wl.mTimerPartial;
                        if (t2 == null) {
                            t2 = new DualTimer(this.mBsi.mClocks, this, 0, this.mBsi.mPartialTimers, this.mBsi.mOnBatteryScreenOffTimeBase, this.mOnBatteryScreenOffBackgroundTimeBase);
                            wl.mTimerPartial = t2;
                        }
                        return t2;
                    case 1:
                        t = wl.mTimerFull;
                        if (t == null) {
                            t = new StopwatchTimer(this.mBsi.mClocks, this, 1, this.mBsi.mFullTimers, this.mBsi.mOnBatteryTimeBase);
                            wl.mTimerFull = t;
                        }
                        return t;
                    case 2:
                        t = wl.mTimerWindow;
                        if (t == null) {
                            t = new StopwatchTimer(this.mBsi.mClocks, this, 2, this.mBsi.mWindowTimers, this.mBsi.mOnBatteryTimeBase);
                            wl.mTimerWindow = t;
                        }
                        return t;
                    default:
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("type=");
                        stringBuilder.append(type);
                        throw new IllegalArgumentException(stringBuilder.toString());
                }
            }
            t = wl.mTimerDraw;
            if (t == null) {
                t = new StopwatchTimer(this.mBsi.mClocks, this, 18, this.mBsi.mDrawTimers, this.mBsi.mOnBatteryTimeBase);
                wl.mTimerDraw = t;
            }
            return t;
        }

        public void noteStartWakeLocked(int pid, String name, int type, long elapsedRealtimeMs) {
            Wakelock wl = (Wakelock) this.mWakelockStats.startObject(name);
            if (wl != null) {
                getWakelockTimerLocked(wl, type).startRunningLocked(elapsedRealtimeMs);
            }
            if (type == 0) {
                createAggregatedPartialWakelockTimerLocked().startRunningLocked(elapsedRealtimeMs);
                if (pid >= 0) {
                    Pid p = getPidStatsLocked(pid);
                    int i = p.mWakeNesting;
                    p.mWakeNesting = i + 1;
                    if (i == 0) {
                        p.mWakeStartMs = elapsedRealtimeMs;
                    }
                }
            }
        }

        public void noteStopWakeLocked(int pid, String name, int type, long elapsedRealtimeMs) {
            Wakelock wl = (Wakelock) this.mWakelockStats.stopObject(name);
            if (wl != null) {
                getWakelockTimerLocked(wl, type).stopRunningLocked(elapsedRealtimeMs);
            }
            if (type == 0) {
                if (this.mAggregatedPartialWakelockTimer != null) {
                    this.mAggregatedPartialWakelockTimer.stopRunningLocked(elapsedRealtimeMs);
                }
                if (pid >= 0) {
                    Pid p = (Pid) this.mPids.get(pid);
                    if (p != null && p.mWakeNesting > 0) {
                        int i = p.mWakeNesting;
                        p.mWakeNesting = i - 1;
                        if (i == 1) {
                            p.mWakeSumMs += elapsedRealtimeMs - p.mWakeStartMs;
                            p.mWakeStartMs = 0;
                        }
                    }
                }
            }
        }

        public void reportExcessiveCpuLocked(String proc, long overTime, long usedTime) {
            Proc p = getProcessStatsLocked(proc);
            if (p != null) {
                p.addExcessiveCpu(overTime, usedTime);
            }
        }

        public void noteStartSensor(int sensor, long elapsedRealtimeMs) {
            getSensorTimerLocked(sensor, true).startRunningLocked(elapsedRealtimeMs);
        }

        public void noteStopSensor(int sensor, long elapsedRealtimeMs) {
            DualTimer t = getSensorTimerLocked(sensor, null);
            if (t != null) {
                t.stopRunningLocked(elapsedRealtimeMs);
            }
        }

        public void noteStartGps(long elapsedRealtimeMs) {
            noteStartSensor(-10000, elapsedRealtimeMs);
        }

        public void noteStopGps(long elapsedRealtimeMs) {
            noteStopSensor(-10000, elapsedRealtimeMs);
        }

        public BatteryStatsImpl getBatteryStats() {
            return this.mBsi;
        }
    }

    public static class BatchTimer extends Timer {
        boolean mInDischarge;
        long mLastAddedDuration;
        long mLastAddedTime;
        final Uid mUid;

        BatchTimer(Clocks clocks, Uid uid, int type, TimeBase timeBase, Parcel in) {
            super(clocks, type, timeBase, in);
            this.mUid = uid;
            this.mLastAddedTime = in.readLong();
            this.mLastAddedDuration = in.readLong();
            this.mInDischarge = timeBase.isRunning();
        }

        BatchTimer(Clocks clocks, Uid uid, int type, TimeBase timeBase) {
            super(clocks, type, timeBase);
            this.mUid = uid;
            this.mInDischarge = timeBase.isRunning();
        }

        public void writeToParcel(Parcel out, long elapsedRealtimeUs) {
            super.writeToParcel(out, elapsedRealtimeUs);
            out.writeLong(this.mLastAddedTime);
            out.writeLong(this.mLastAddedDuration);
        }

        public void onTimeStopped(long elapsedRealtime, long baseUptime, long baseRealtime) {
            recomputeLastDuration(this.mClocks.elapsedRealtime() * 1000, false);
            this.mInDischarge = false;
            super.onTimeStopped(elapsedRealtime, baseUptime, baseRealtime);
        }

        public void onTimeStarted(long elapsedRealtime, long baseUptime, long baseRealtime) {
            recomputeLastDuration(elapsedRealtime, false);
            this.mInDischarge = true;
            if (this.mLastAddedTime == elapsedRealtime) {
                this.mTotalTime += this.mLastAddedDuration;
            }
            super.onTimeStarted(elapsedRealtime, baseUptime, baseRealtime);
        }

        public void logState(Printer pw, String prefix) {
            super.logState(pw, prefix);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mLastAddedTime=");
            stringBuilder.append(this.mLastAddedTime);
            stringBuilder.append(" mLastAddedDuration=");
            stringBuilder.append(this.mLastAddedDuration);
            pw.println(stringBuilder.toString());
        }

        private long computeOverage(long curTime) {
            if (this.mLastAddedTime > 0) {
                return (this.mLastTime + this.mLastAddedDuration) - curTime;
            }
            return 0;
        }

        private void recomputeLastDuration(long curTime, boolean abort) {
            long overage = computeOverage(curTime);
            if (overage > 0) {
                if (this.mInDischarge) {
                    this.mTotalTime -= overage;
                }
                if (abort) {
                    this.mLastAddedTime = 0;
                    return;
                }
                this.mLastAddedTime = curTime;
                this.mLastAddedDuration -= overage;
            }
        }

        public void addDuration(BatteryStatsImpl stats, long durationMillis) {
            long now = this.mClocks.elapsedRealtime() * 1000;
            recomputeLastDuration(now, true);
            this.mLastAddedTime = now;
            this.mLastAddedDuration = 1000 * durationMillis;
            if (this.mInDischarge) {
                this.mTotalTime += this.mLastAddedDuration;
                this.mCount++;
            }
        }

        public void abortLastDuration(BatteryStatsImpl stats) {
            recomputeLastDuration(this.mClocks.elapsedRealtime() * 1000, true);
        }

        protected int computeCurrentCountLocked() {
            return this.mCount;
        }

        protected long computeRunTimeLocked(long curBatteryRealtime) {
            long overage = computeOverage(this.mClocks.elapsedRealtime() * 1000);
            if (overage <= 0) {
                return this.mTotalTime;
            }
            this.mTotalTime = overage;
            return overage;
        }

        public boolean reset(boolean detachIfReset) {
            long now = this.mClocks.elapsedRealtime() * 1000;
            recomputeLastDuration(now, true);
            boolean stillActive = this.mLastAddedTime == now;
            boolean z = !stillActive && detachIfReset;
            super.reset(z);
            if (stillActive) {
                return false;
            }
            return true;
        }
    }

    public static class SamplingTimer extends Timer {
        int mCurrentReportedCount;
        long mCurrentReportedTotalTime;
        boolean mTimeBaseRunning;
        boolean mTrackingReportedValues;
        int mUnpluggedReportedCount;
        long mUnpluggedReportedTotalTime;
        int mUpdateVersion;

        @VisibleForTesting
        public SamplingTimer(Clocks clocks, TimeBase timeBase, Parcel in) {
            boolean z = false;
            super(clocks, 0, timeBase, in);
            this.mCurrentReportedCount = in.readInt();
            this.mUnpluggedReportedCount = in.readInt();
            this.mCurrentReportedTotalTime = in.readLong();
            this.mUnpluggedReportedTotalTime = in.readLong();
            if (in.readInt() == 1) {
                z = true;
            }
            this.mTrackingReportedValues = z;
            this.mTimeBaseRunning = timeBase.isRunning();
        }

        @VisibleForTesting
        public SamplingTimer(Clocks clocks, TimeBase timeBase) {
            super(clocks, 0, timeBase);
            this.mTrackingReportedValues = false;
            this.mTimeBaseRunning = timeBase.isRunning();
        }

        public void endSample() {
            this.mTotalTime = computeRunTimeLocked(0);
            this.mCount = computeCurrentCountLocked();
            this.mCurrentReportedTotalTime = 0;
            this.mUnpluggedReportedTotalTime = 0;
            this.mCurrentReportedCount = 0;
            this.mUnpluggedReportedCount = 0;
        }

        public void setUpdateVersion(int version) {
            this.mUpdateVersion = version;
        }

        public int getUpdateVersion() {
            return this.mUpdateVersion;
        }

        public void update(long totalTime, int count) {
            if (this.mTimeBaseRunning && !this.mTrackingReportedValues) {
                this.mUnpluggedReportedTotalTime = totalTime;
                this.mUnpluggedReportedCount = count;
            }
            this.mTrackingReportedValues = true;
            if (totalTime < this.mCurrentReportedTotalTime || count < this.mCurrentReportedCount) {
                endSample();
            }
            this.mCurrentReportedTotalTime = totalTime;
            this.mCurrentReportedCount = count;
        }

        public void add(long deltaTime, int deltaCount) {
            update(this.mCurrentReportedTotalTime + deltaTime, this.mCurrentReportedCount + deltaCount);
        }

        public void onTimeStarted(long elapsedRealtime, long baseUptime, long baseRealtime) {
            super.onTimeStarted(elapsedRealtime, baseUptime, baseRealtime);
            if (this.mTrackingReportedValues) {
                this.mUnpluggedReportedTotalTime = this.mCurrentReportedTotalTime;
                this.mUnpluggedReportedCount = this.mCurrentReportedCount;
            }
            this.mTimeBaseRunning = true;
        }

        public void onTimeStopped(long elapsedRealtime, long baseUptime, long baseRealtime) {
            super.onTimeStopped(elapsedRealtime, baseUptime, baseRealtime);
            this.mTimeBaseRunning = false;
        }

        public void logState(Printer pw, String prefix) {
            super.logState(pw, prefix);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mCurrentReportedCount=");
            stringBuilder.append(this.mCurrentReportedCount);
            stringBuilder.append(" mUnpluggedReportedCount=");
            stringBuilder.append(this.mUnpluggedReportedCount);
            stringBuilder.append(" mCurrentReportedTotalTime=");
            stringBuilder.append(this.mCurrentReportedTotalTime);
            stringBuilder.append(" mUnpluggedReportedTotalTime=");
            stringBuilder.append(this.mUnpluggedReportedTotalTime);
            pw.println(stringBuilder.toString());
        }

        protected long computeRunTimeLocked(long curBatteryRealtime) {
            long j = this.mTotalTime;
            long j2 = (this.mTimeBaseRunning && this.mTrackingReportedValues) ? this.mCurrentReportedTotalTime - this.mUnpluggedReportedTotalTime : 0;
            return j + j2;
        }

        protected int computeCurrentCountLocked() {
            int i = this.mCount;
            int i2 = (this.mTimeBaseRunning && this.mTrackingReportedValues) ? this.mCurrentReportedCount - this.mUnpluggedReportedCount : 0;
            return i + i2;
        }

        public void writeToParcel(Parcel out, long elapsedRealtimeUs) {
            super.writeToParcel(out, elapsedRealtimeUs);
            out.writeInt(this.mCurrentReportedCount);
            out.writeInt(this.mUnpluggedReportedCount);
            out.writeLong(this.mCurrentReportedTotalTime);
            out.writeLong(this.mUnpluggedReportedTotalTime);
            out.writeInt(this.mTrackingReportedValues);
        }

        public boolean reset(boolean detachIfReset) {
            super.reset(detachIfReset);
            this.mTrackingReportedValues = false;
            this.mUnpluggedReportedTotalTime = 0;
            this.mUnpluggedReportedCount = 0;
            return true;
        }
    }

    public static class StopwatchTimer extends Timer {
        long mAcquireTime = -1;
        @VisibleForTesting
        public boolean mInList;
        int mNesting;
        long mTimeout;
        final ArrayList<StopwatchTimer> mTimerPool;
        final Uid mUid;
        long mUpdateTime;

        public StopwatchTimer(Clocks clocks, Uid uid, int type, ArrayList<StopwatchTimer> timerPool, TimeBase timeBase, Parcel in) {
            super(clocks, type, timeBase, in);
            this.mUid = uid;
            this.mTimerPool = timerPool;
            this.mUpdateTime = in.readLong();
        }

        public StopwatchTimer(Clocks clocks, Uid uid, int type, ArrayList<StopwatchTimer> timerPool, TimeBase timeBase) {
            super(clocks, type, timeBase);
            this.mUid = uid;
            this.mTimerPool = timerPool;
        }

        public void setTimeout(long timeout) {
            this.mTimeout = timeout;
        }

        public void writeToParcel(Parcel out, long elapsedRealtimeUs) {
            super.writeToParcel(out, elapsedRealtimeUs);
            out.writeLong(this.mUpdateTime);
        }

        public void onTimeStopped(long elapsedRealtime, long baseUptime, long baseRealtime) {
            if (this.mNesting > 0) {
                super.onTimeStopped(elapsedRealtime, baseUptime, baseRealtime);
                this.mUpdateTime = baseRealtime;
            }
        }

        public void logState(Printer pw, String prefix) {
            super.logState(pw, prefix);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("mNesting=");
            stringBuilder.append(this.mNesting);
            stringBuilder.append(" mUpdateTime=");
            stringBuilder.append(this.mUpdateTime);
            stringBuilder.append(" mAcquireTime=");
            stringBuilder.append(this.mAcquireTime);
            pw.println(stringBuilder.toString());
        }

        public void startRunningLocked(long elapsedRealtimeMs) {
            int i = this.mNesting;
            this.mNesting = i + 1;
            if (i == 0) {
                long batteryRealtime = this.mTimeBase.getRealtime(1000 * elapsedRealtimeMs);
                this.mUpdateTime = batteryRealtime;
                if (this.mTimerPool != null) {
                    refreshTimersLocked(batteryRealtime, this.mTimerPool, null);
                    this.mTimerPool.add(this);
                }
                if (this.mTimeBase.isRunning()) {
                    this.mCount++;
                    this.mAcquireTime = this.mTotalTime;
                    return;
                }
                this.mAcquireTime = -1;
            }
        }

        public boolean isRunningLocked() {
            return this.mNesting > 0;
        }

        public void stopRunningLocked(long elapsedRealtimeMs) {
            if (this.mNesting != 0) {
                int i = this.mNesting - 1;
                this.mNesting = i;
                if (i == 0) {
                    long batteryRealtime = this.mTimeBase.getRealtime(1000 * elapsedRealtimeMs);
                    if (this.mTimerPool != null) {
                        refreshTimersLocked(batteryRealtime, this.mTimerPool, null);
                        this.mTimerPool.remove(this);
                    } else {
                        this.mNesting = 1;
                        this.mTotalTime = computeRunTimeLocked(batteryRealtime);
                        this.mNesting = 0;
                    }
                    if (this.mAcquireTime >= 0 && this.mTotalTime == this.mAcquireTime) {
                        this.mCount--;
                    }
                }
            }
        }

        public void stopAllRunningLocked(long elapsedRealtimeMs) {
            if (this.mNesting > 0) {
                this.mNesting = 1;
                stopRunningLocked(elapsedRealtimeMs);
            }
        }

        private static long refreshTimersLocked(long batteryRealtime, ArrayList<StopwatchTimer> pool, StopwatchTimer self) {
            long selfTime = 0;
            int N = pool.size();
            for (int i = N - 1; i >= 0; i--) {
                StopwatchTimer t = (StopwatchTimer) pool.get(i);
                long heldTime = batteryRealtime - t.mUpdateTime;
                if (heldTime > 0) {
                    long myTime = heldTime / ((long) N);
                    if (t == self) {
                        selfTime = myTime;
                    }
                    t.mTotalTime += myTime;
                }
                t.mUpdateTime = batteryRealtime;
            }
            return selfTime;
        }

        protected long computeRunTimeLocked(long curBatteryRealtime) {
            long j = 0;
            if (this.mTimeout > 0 && curBatteryRealtime > this.mUpdateTime + this.mTimeout) {
                curBatteryRealtime = this.mUpdateTime + this.mTimeout;
            }
            int size = 0;
            if (this.mTimerPool != null) {
                size = this.mTimerPool.size();
            }
            long j2 = this.mTotalTime;
            if (this.mNesting > 0) {
                j = (curBatteryRealtime - this.mUpdateTime) / ((long) (size > 0 ? size : 1));
            }
            return j2 + j;
        }

        protected int computeCurrentCountLocked() {
            return this.mCount;
        }

        public boolean reset(boolean detachIfReset) {
            boolean z = false;
            boolean canDetach = this.mNesting <= 0;
            if (canDetach && detachIfReset) {
                z = true;
            }
            super.reset(z);
            if (this.mNesting > 0) {
                this.mUpdateTime = this.mTimeBase.getRealtime(this.mClocks.elapsedRealtime() * 1000);
            }
            this.mAcquireTime = -1;
            return canDetach;
        }

        public void detach() {
            super.detach();
            if (this.mTimerPool != null) {
                this.mTimerPool.remove(this);
            }
        }

        public void readSummaryFromParcelLocked(Parcel in) {
            super.readSummaryFromParcelLocked(in);
            this.mNesting = 0;
        }

        public void setMark(long elapsedRealtimeMs) {
            long batteryRealtime = this.mTimeBase.getRealtime(1000 * elapsedRealtimeMs);
            if (this.mNesting > 0) {
                if (this.mTimerPool != null) {
                    refreshTimersLocked(batteryRealtime, this.mTimerPool, this);
                } else {
                    this.mTotalTime += batteryRealtime - this.mUpdateTime;
                    this.mUpdateTime = batteryRealtime;
                }
            }
            this.mTimeBeforeMark = this.mTotalTime;
        }
    }

    public static class DurationTimer extends StopwatchTimer {
        long mCurrentDurationMs;
        long mMaxDurationMs;
        long mStartTimeMs = -1;
        long mTotalDurationMs;

        public DurationTimer(Clocks clocks, Uid uid, int type, ArrayList<StopwatchTimer> timerPool, TimeBase timeBase, Parcel in) {
            super(clocks, uid, type, timerPool, timeBase, in);
            this.mMaxDurationMs = in.readLong();
            this.mTotalDurationMs = in.readLong();
            this.mCurrentDurationMs = in.readLong();
        }

        public DurationTimer(Clocks clocks, Uid uid, int type, ArrayList<StopwatchTimer> timerPool, TimeBase timeBase) {
            super(clocks, uid, type, timerPool, timeBase);
        }

        public void writeToParcel(Parcel out, long elapsedRealtimeUs) {
            super.writeToParcel(out, elapsedRealtimeUs);
            out.writeLong(getMaxDurationMsLocked(elapsedRealtimeUs / 1000));
            out.writeLong(this.mTotalDurationMs);
            out.writeLong(getCurrentDurationMsLocked(elapsedRealtimeUs / 1000));
        }

        public void writeSummaryFromParcelLocked(Parcel out, long elapsedRealtimeUs) {
            super.writeSummaryFromParcelLocked(out, elapsedRealtimeUs);
            out.writeLong(getMaxDurationMsLocked(elapsedRealtimeUs / 1000));
            out.writeLong(getTotalDurationMsLocked(elapsedRealtimeUs / 1000));
        }

        public void readSummaryFromParcelLocked(Parcel in) {
            super.readSummaryFromParcelLocked(in);
            this.mMaxDurationMs = in.readLong();
            this.mTotalDurationMs = in.readLong();
            this.mStartTimeMs = -1;
            this.mCurrentDurationMs = 0;
        }

        public void onTimeStarted(long elapsedRealtimeUs, long baseUptime, long baseRealtime) {
            super.onTimeStarted(elapsedRealtimeUs, baseUptime, baseRealtime);
            if (this.mNesting > 0) {
                this.mStartTimeMs = baseRealtime / 1000;
            }
        }

        public void onTimeStopped(long elapsedRealtimeUs, long baseUptime, long baseRealtimeUs) {
            super.onTimeStopped(elapsedRealtimeUs, baseUptime, baseRealtimeUs);
            if (this.mNesting > 0) {
                this.mCurrentDurationMs += (baseRealtimeUs / 1000) - this.mStartTimeMs;
            }
            this.mStartTimeMs = -1;
        }

        public void logState(Printer pw, String prefix) {
            super.logState(pw, prefix);
        }

        public void startRunningLocked(long elapsedRealtimeMs) {
            super.startRunningLocked(elapsedRealtimeMs);
            if (this.mNesting == 1 && this.mTimeBase.isRunning()) {
                this.mStartTimeMs = this.mTimeBase.getRealtime(elapsedRealtimeMs * 1000) / 1000;
            }
        }

        public void stopRunningLocked(long elapsedRealtimeMs) {
            if (this.mNesting == 1) {
                long durationMs = getCurrentDurationMsLocked(elapsedRealtimeMs);
                this.mTotalDurationMs += durationMs;
                if (durationMs > this.mMaxDurationMs) {
                    this.mMaxDurationMs = durationMs;
                }
                this.mStartTimeMs = -1;
                this.mCurrentDurationMs = 0;
            }
            super.stopRunningLocked(elapsedRealtimeMs);
        }

        public boolean reset(boolean detachIfReset) {
            boolean result = super.reset(detachIfReset);
            this.mMaxDurationMs = 0;
            this.mTotalDurationMs = 0;
            this.mCurrentDurationMs = 0;
            if (this.mNesting > 0) {
                this.mStartTimeMs = this.mTimeBase.getRealtime(this.mClocks.elapsedRealtime() * 1000) / 1000;
            } else {
                this.mStartTimeMs = -1;
            }
            return result;
        }

        public long getMaxDurationMsLocked(long elapsedRealtimeMs) {
            if (this.mNesting > 0) {
                long durationMs = getCurrentDurationMsLocked(elapsedRealtimeMs);
                if (durationMs > this.mMaxDurationMs) {
                    return durationMs;
                }
            }
            return this.mMaxDurationMs;
        }

        public long getCurrentDurationMsLocked(long elapsedRealtimeMs) {
            long durationMs = this.mCurrentDurationMs;
            if (this.mNesting <= 0 || !this.mTimeBase.isRunning()) {
                return durationMs;
            }
            return durationMs + ((this.mTimeBase.getRealtime(elapsedRealtimeMs * 1000) / 1000) - this.mStartTimeMs);
        }

        public long getTotalDurationMsLocked(long elapsedRealtimeMs) {
            return this.mTotalDurationMs + getCurrentDurationMsLocked(elapsedRealtimeMs);
        }
    }

    public static class DualTimer extends DurationTimer {
        private final DurationTimer mSubTimer;

        public DualTimer(Clocks clocks, Uid uid, int type, ArrayList<StopwatchTimer> timerPool, TimeBase timeBase, TimeBase subTimeBase, Parcel in) {
            super(clocks, uid, type, timerPool, timeBase, in);
            this.mSubTimer = new DurationTimer(clocks, uid, type, null, subTimeBase, in);
        }

        public DualTimer(Clocks clocks, Uid uid, int type, ArrayList<StopwatchTimer> timerPool, TimeBase timeBase, TimeBase subTimeBase) {
            super(clocks, uid, type, timerPool, timeBase);
            this.mSubTimer = new DurationTimer(clocks, uid, type, null, subTimeBase);
        }

        public DurationTimer getSubTimer() {
            return this.mSubTimer;
        }

        public void startRunningLocked(long elapsedRealtimeMs) {
            super.startRunningLocked(elapsedRealtimeMs);
            this.mSubTimer.startRunningLocked(elapsedRealtimeMs);
        }

        public void stopRunningLocked(long elapsedRealtimeMs) {
            super.stopRunningLocked(elapsedRealtimeMs);
            this.mSubTimer.stopRunningLocked(elapsedRealtimeMs);
        }

        public void stopAllRunningLocked(long elapsedRealtimeMs) {
            super.stopAllRunningLocked(elapsedRealtimeMs);
            this.mSubTimer.stopAllRunningLocked(elapsedRealtimeMs);
        }

        public boolean reset(boolean detachIfReset) {
            if ((false | (this.mSubTimer.reset(false) ^ 1)) | (super.reset(detachIfReset) ^ 1)) {
                return false;
            }
            return true;
        }

        public void detach() {
            this.mSubTimer.detach();
            super.detach();
        }

        public void writeToParcel(Parcel out, long elapsedRealtimeUs) {
            super.writeToParcel(out, elapsedRealtimeUs);
            this.mSubTimer.writeToParcel(out, elapsedRealtimeUs);
        }

        public void writeSummaryFromParcelLocked(Parcel out, long elapsedRealtimeUs) {
            super.writeSummaryFromParcelLocked(out, elapsedRealtimeUs);
            this.mSubTimer.writeSummaryFromParcelLocked(out, elapsedRealtimeUs);
        }

        public void readSummaryFromParcelLocked(Parcel in) {
            super.readSummaryFromParcelLocked(in);
            this.mSubTimer.readSummaryFromParcelLocked(in);
        }
    }

    static {
        if (ActivityManager.isLowRamDeviceStatic()) {
            MAX_HISTORY_ITEMS = 800;
            MAX_MAX_HISTORY_ITEMS = 1200;
            MAX_WAKELOCKS_PER_UID = 40;
            MAX_HISTORY_BUFFER = 98304;
            MAX_MAX_HISTORY_BUFFER = Protocol.BASE_WIFI;
        } else {
            MAX_HISTORY_ITEMS = 4000;
            MAX_MAX_HISTORY_ITEMS = 6000;
            MAX_WAKELOCKS_PER_UID = 200;
            MAX_HISTORY_BUFFER = 524288;
            MAX_MAX_HISTORY_BUFFER = 655360;
        }
    }

    public LongSparseArray<SamplingTimer> getKernelMemoryStats() {
        return this.mKernelMemoryStats;
    }

    public void postBatteryNeedsCpuUpdateMsg() {
        this.mHandler.sendEmptyMessage(1);
    }

    /* JADX WARNING: Missing block: B:21:0x0036, code skipped:
            r1 = r0.size() - 1;
     */
    /* JADX WARNING: Missing block: B:22:0x003c, code skipped:
            if (r1 < 0) goto L_0x009e;
     */
    /* JADX WARNING: Missing block: B:23:0x003e, code skipped:
            r2 = r0.keyAt(r1);
            r3 = r0.valueAt(r1);
     */
    /* JADX WARNING: Missing block: B:24:0x0046, code skipped:
            monitor-enter(r10);
     */
    /* JADX WARNING: Missing block: B:26:?, code skipped:
            r4 = getAvailableUidStatsLocked(r2);
     */
    /* JADX WARNING: Missing block: B:27:0x004b, code skipped:
            if (r4 != null) goto L_0x004f;
     */
    /* JADX WARNING: Missing block: B:28:0x004d, code skipped:
            monitor-exit(r10);
     */
    /* JADX WARNING: Missing block: B:31:0x0051, code skipped:
            if (r4.mChildUids != null) goto L_0x0055;
     */
    /* JADX WARNING: Missing block: B:32:0x0053, code skipped:
            r5 = null;
     */
    /* JADX WARNING: Missing block: B:33:0x0055, code skipped:
            r5 = r4.mChildUids.toArray();
            r6 = r5.length - 1;
     */
    /* JADX WARNING: Missing block: B:34:0x005e, code skipped:
            if (r6 < 0) goto L_0x006b;
     */
    /* JADX WARNING: Missing block: B:35:0x0060, code skipped:
            r5[r6] = r4.mChildUids.get(r6);
            r6 = r6 - 1;
     */
    /* JADX WARNING: Missing block: B:36:0x006b, code skipped:
            monitor-exit(r10);
     */
    /* JADX WARNING: Missing block: B:37:0x006c, code skipped:
            r6 = r10.mKernelSingleUidTimeReader.readDeltaMs(r2);
     */
    /* JADX WARNING: Missing block: B:38:0x0072, code skipped:
            if (r5 == null) goto L_0x0088;
     */
    /* JADX WARNING: Missing block: B:39:0x0074, code skipped:
            r7 = r5.length - 1;
     */
    /* JADX WARNING: Missing block: B:40:0x0077, code skipped:
            if (r7 < 0) goto L_0x0088;
     */
    /* JADX WARNING: Missing block: B:41:0x0079, code skipped:
            r6 = addCpuTimes(r6, r10.mKernelSingleUidTimeReader.readDeltaMs(r5[r7]));
            r7 = r7 - 1;
     */
    /* JADX WARNING: Missing block: B:42:0x0088, code skipped:
            if (r11 == false) goto L_0x0098;
     */
    /* JADX WARNING: Missing block: B:43:0x008a, code skipped:
            if (r6 == null) goto L_0x0098;
     */
    /* JADX WARNING: Missing block: B:44:0x008c, code skipped:
            monitor-enter(r10);
     */
    /* JADX WARNING: Missing block: B:46:?, code skipped:
            com.android.internal.os.BatteryStatsImpl.Uid.access$300(r4, r3, r6, r11);
            com.android.internal.os.BatteryStatsImpl.Uid.access$400(r4, r3, r6, r12);
     */
    /* JADX WARNING: Missing block: B:47:0x0093, code skipped:
            monitor-exit(r10);
     */
    /* JADX WARNING: Missing block: B:52:0x0098, code skipped:
            r1 = r1 - 1;
     */
    /* JADX WARNING: Missing block: B:57:0x009e, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateProcStateCpuTimes(boolean onBattery, boolean onBatteryScreenOff) {
        synchronized (this) {
            if (!this.mConstants.TRACK_CPU_TIMES_BY_PROC_STATE) {
            } else if (!initKernelSingleUidTimeReaderLocked()) {
            } else if (this.mKernelSingleUidTimeReader.hasStaleData()) {
                this.mPendingUids.clear();
            } else if (this.mPendingUids.size() == 0) {
            } else {
                SparseIntArray uidStates = this.mPendingUids.clone();
                this.mPendingUids.clear();
            }
        }
    }

    public void clearPendingRemovedUids() {
        long cutOffTime = this.mClocks.elapsedRealtime() - this.mConstants.UID_REMOVE_DELAY_MS;
        while (!this.mPendingRemovedUids.isEmpty() && ((UidToRemove) this.mPendingRemovedUids.peek()).timeAddedInQueue < cutOffTime) {
            ((UidToRemove) this.mPendingRemovedUids.poll()).remove();
        }
    }

    public void copyFromAllUidsCpuTimes() {
        synchronized (this) {
            copyFromAllUidsCpuTimes(this.mOnBatteryTimeBase.isRunning(), this.mOnBatteryScreenOffTimeBase.isRunning());
        }
    }

    public void copyFromAllUidsCpuTimes(boolean onBattery, boolean onBatteryScreenOff) {
        synchronized (this) {
            if (!this.mConstants.TRACK_CPU_TIMES_BY_PROC_STATE) {
            } else if (initKernelSingleUidTimeReaderLocked()) {
                SparseArray<long[]> allUidCpuFreqTimesMs = this.mKernelUidCpuFreqTimeReader.getAllUidCpuFreqTimeMs();
                if (this.mKernelSingleUidTimeReader.hasStaleData()) {
                    this.mKernelSingleUidTimeReader.setAllUidsCpuTimesMs(allUidCpuFreqTimesMs);
                    this.mKernelSingleUidTimeReader.markDataAsStale(false);
                    this.mPendingUids.clear();
                    return;
                }
                for (int i = allUidCpuFreqTimesMs.size() - 1; i >= 0; i--) {
                    int uid = allUidCpuFreqTimesMs.keyAt(i);
                    Uid u = getAvailableUidStatsLocked(mapUid(uid));
                    if (u != null) {
                        long[] cpuTimesMs = (long[]) allUidCpuFreqTimesMs.valueAt(i);
                        if (cpuTimesMs != null) {
                            long[] deltaTimesMs = this.mKernelSingleUidTimeReader.computeDelta(uid, (long[]) cpuTimesMs.clone());
                            if (onBattery && deltaTimesMs != null) {
                                int procState;
                                int idx = this.mPendingUids.indexOfKey(uid);
                                if (idx >= 0) {
                                    procState = this.mPendingUids.valueAt(idx);
                                    this.mPendingUids.removeAt(idx);
                                } else {
                                    procState = u.mProcessState;
                                }
                                if (procState >= 0 && procState < 7) {
                                    u.addProcStateTimesMs(procState, deltaTimesMs, onBattery);
                                    u.addProcStateScreenOffTimesMs(procState, deltaTimesMs, onBatteryScreenOff);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @VisibleForTesting
    public long[] addCpuTimes(long[] timesA, long[] timesB) {
        if (timesA == null || timesB == null) {
            long[] jArr = timesA == null ? timesB == null ? null : timesB : timesA;
            return jArr;
        }
        for (int i = timesA.length - 1; i >= 0; i--) {
            timesA[i] = timesA[i] + timesB[i];
        }
        return timesA;
    }

    @GuardedBy("this")
    private boolean initKernelSingleUidTimeReaderLocked() {
        boolean z = false;
        if (this.mKernelSingleUidTimeReader == null) {
            if (this.mPowerProfile == null) {
                return false;
            }
            if (this.mCpuFreqs == null) {
                this.mCpuFreqs = this.mKernelUidCpuFreqTimeReader.readFreqs(this.mPowerProfile);
            }
            if (this.mCpuFreqs != null) {
                this.mKernelSingleUidTimeReader = new KernelSingleUidTimeReader(this.mCpuFreqs.length);
            } else {
                this.mPerProcStateCpuTimesAvailable = this.mKernelUidCpuFreqTimeReader.allUidTimesAvailable();
                return false;
            }
        }
        if (this.mKernelUidCpuFreqTimeReader.allUidTimesAvailable() && this.mKernelSingleUidTimeReader.singleUidCpuTimesAvailable()) {
            z = true;
        }
        this.mPerProcStateCpuTimesAvailable = z;
        return true;
    }

    public Map<String, ? extends Timer> getRpmStats() {
        return this.mRpmStats;
    }

    public Map<String, ? extends Timer> getScreenOffRpmStats() {
        return this.mScreenOffRpmStats;
    }

    public Map<String, ? extends Timer> getKernelWakelockStats() {
        return this.mKernelWakelockStats;
    }

    public Map<String, ? extends Timer> getWakeupReasonStats() {
        return this.mWakeupReasonStats;
    }

    public long getUahDischarge(int which) {
        return this.mDischargeCounter.getCountLocked(which);
    }

    public long getUahDischargeScreenOff(int which) {
        return this.mDischargeScreenOffCounter.getCountLocked(which);
    }

    public long getUahDischargeScreenDoze(int which) {
        return this.mDischargeScreenDozeCounter.getCountLocked(which);
    }

    public long getUahDischargeLightDoze(int which) {
        return this.mDischargeLightDozeCounter.getCountLocked(which);
    }

    public long getUahDischargeDeepDoze(int which) {
        return this.mDischargeDeepDozeCounter.getCountLocked(which);
    }

    public int getEstimatedBatteryCapacity() {
        return this.mEstimatedBatteryCapacity;
    }

    public int getMinLearnedBatteryCapacity() {
        return this.mMinLearnedBatteryCapacity;
    }

    public int getMaxLearnedBatteryCapacity() {
        return this.mMaxLearnedBatteryCapacity;
    }

    public BatteryStatsImpl() {
        this(new SystemClocks());
    }

    public BatteryStatsImpl(Clocks clocks) {
        this.mKernelWakelockReader = new KernelWakelockReader();
        this.mTmpWakelockStats = new KernelWakelockStats();
        this.mKernelUidCpuTimeReader = new KernelUidCpuTimeReader();
        this.mKernelUidCpuFreqTimeReader = new KernelUidCpuFreqTimeReader();
        this.mKernelUidCpuActiveTimeReader = new KernelUidCpuActiveTimeReader();
        this.mKernelUidCpuClusterTimeReader = new KernelUidCpuClusterTimeReader();
        this.mKernelMemoryBandwidthStats = new KernelMemoryBandwidthStats();
        this.mKernelMemoryStats = new LongSparseArray();
        this.mPerProcStateCpuTimesAvailable = true;
        this.mPendingUids = new SparseIntArray();
        this.mCpuTimeReadsTrackingStartTime = SystemClock.uptimeMillis();
        this.mTmpRpmStats = new RpmStats();
        this.mLastRpmStatsUpdateTimeMs = -1000;
        this.mPendingRemovedUids = new LinkedList();
        this.mExternalSync = null;
        this.mUserInfoProvider = null;
        this.mIsolatedUids = new SparseIntArray();
        this.mUidStats = new SparseArray();
        this.mPartialTimers = new ArrayList();
        this.mFullTimers = new ArrayList();
        this.mWindowTimers = new ArrayList();
        this.mDrawTimers = new ArrayList();
        this.mSensorTimers = new SparseArray();
        this.mWifiRunningTimers = new ArrayList();
        this.mFullWifiLockTimers = new ArrayList();
        this.mWifiMulticastTimers = new ArrayList();
        this.mWifiScanTimers = new ArrayList();
        this.mWifiBatchedScanTimers = new SparseArray();
        this.mAudioTurnedOnTimers = new ArrayList();
        this.mVideoTurnedOnTimers = new ArrayList();
        this.mFlashlightTurnedOnTimers = new ArrayList();
        this.mCameraTurnedOnTimers = new ArrayList();
        this.mBluetoothScanOnTimers = new ArrayList();
        this.mLastPartialTimers = new ArrayList();
        this.mOnBatteryTimeBase = new TimeBase();
        this.mOnBatteryScreenOffTimeBase = new TimeBase();
        this.mActiveEvents = new HistoryEventTracker();
        this.mHaveBatteryLevel = false;
        this.mRecordingHistory = false;
        this.mHistoryBuffer = Parcel.obtain();
        this.mHistoryLastWritten = new HistoryItem();
        this.mHistoryLastLastWritten = new HistoryItem();
        this.mHistoryReadTmp = new HistoryItem();
        this.mHistoryAddTmp = new HistoryItem();
        this.mHistoryTagPool = new HashMap();
        this.mNextHistoryTagIdx = 0;
        this.mNumHistoryTagChars = 0;
        this.mHistoryBufferLastPos = -1;
        this.mHistoryOverflow = false;
        this.mActiveHistoryStates = -1;
        this.mActiveHistoryStates2 = -1;
        this.mLastHistoryElapsedRealtime = 0;
        this.mTrackRunningHistoryElapsedRealtime = 0;
        this.mTrackRunningHistoryUptime = 0;
        this.mHistoryCur = new HistoryItem();
        this.mLastHistoryStepDetails = null;
        this.mLastHistoryStepLevel = (byte) 0;
        this.mCurHistoryStepDetails = new HistoryStepDetails();
        this.mReadHistoryStepDetails = new HistoryStepDetails();
        this.mTmpHistoryStepDetails = new HistoryStepDetails();
        this.mScreenState = 0;
        this.mScreenBrightnessBin = -1;
        this.mScreenBrightnessTimer = new StopwatchTimer[5];
        this.mUsbDataState = 0;
        this.mGpsSignalQualityBin = -1;
        this.mGpsSignalQualityTimer = new StopwatchTimer[2];
        this.mPhoneSignalStrengthBin = -1;
        this.mPhoneSignalStrengthBinRaw = -1;
        this.mPhoneSignalStrengthsTimer = new StopwatchTimer[5];
        this.mPhoneDataConnectionType = -1;
        this.mPhoneDataConnectionsTimer = new StopwatchTimer[21];
        this.mNetworkByteActivityCounters = new LongSamplingCounter[10];
        this.mNetworkPacketActivityCounters = new LongSamplingCounter[10];
        this.mHasWifiReporting = false;
        this.mHasBluetoothReporting = false;
        this.mHasModemReporting = false;
        this.mWifiState = -1;
        this.mWifiStateTimer = new StopwatchTimer[8];
        this.mWifiSupplState = -1;
        this.mWifiSupplStateTimer = new StopwatchTimer[13];
        this.mWifiSignalStrengthBin = -1;
        this.mWifiSignalStrengthsTimer = new StopwatchTimer[5];
        this.mIsCellularTxPowerHigh = false;
        this.mMobileRadioPowerState = 1;
        this.mWifiRadioPowerState = 1;
        this.mCharging = true;
        this.mInitStepMode = 0;
        this.mCurStepMode = 0;
        this.mModStepMode = 0;
        this.mDischargeStepTracker = new LevelStepTracker(200);
        this.mDailyDischargeStepTracker = new LevelStepTracker(400);
        this.mChargeStepTracker = new LevelStepTracker(200);
        this.mDailyChargeStepTracker = new LevelStepTracker(400);
        this.mDailyStartTime = 0;
        this.mNextMinDailyDeadline = 0;
        this.mNextMaxDailyDeadline = 0;
        this.mDailyItems = new ArrayList();
        this.mLastWriteTime = 0;
        this.mPhoneServiceState = -1;
        this.mPhoneServiceStateRaw = -1;
        this.mPhoneSimStateRaw = -1;
        this.mEstimatedBatteryCapacity = -1;
        this.mMinLearnedBatteryCapacity = -1;
        this.mMaxLearnedBatteryCapacity = -1;
        this.mRpmStats = new HashMap();
        this.mScreenOffRpmStats = new HashMap();
        this.mKernelWakelockStats = new HashMap();
        this.mLastWakeupReason = null;
        this.mLastWakeupUptimeMs = 0;
        this.mWakeupReasonStats = new HashMap();
        this.mChangedStates = 0;
        this.mChangedStates2 = 0;
        this.mInitialAcquireWakeUid = -1;
        this.mWifiFullLockNesting = 0;
        this.mWifiScanNesting = 0;
        this.mWifiMulticastNesting = 0;
        this.mNetworkStatsFactory = new NetworkStatsFactory();
        this.mNetworkStatsPool = new SynchronizedPool(6);
        this.mWifiNetworkLock = new Object();
        this.mWifiIfaces = EmptyArray.STRING;
        this.mLastWifiNetworkStats = new NetworkStats(0, -1);
        this.mModemNetworkLock = new Object();
        this.mModemIfaces = EmptyArray.STRING;
        this.mLastModemNetworkStats = new NetworkStats(0, -1);
        this.mLastModemActivityInfo = new ModemActivityInfo(0, 0, 0, new int[0], 0, 0);
        this.mLastBluetoothActivityInfo = new BluetoothActivityInfoCache(this, null);
        this.mPendingWrite = null;
        this.mWriteLock = new ReentrantLock();
        init(clocks);
        this.mFile = null;
        this.mCheckinFile = null;
        this.mDailyFile = null;
        this.mHandler = null;
        this.mPlatformIdleStateCallback = null;
        this.mUserInfoProvider = null;
        this.mConstants = new Constants(this.mHandler);
        clearHistoryLocked();
    }

    private void init(Clocks clocks) {
        this.mClocks = clocks;
    }

    public SamplingTimer getRpmTimerLocked(String name) {
        SamplingTimer rpmt = (SamplingTimer) this.mRpmStats.get(name);
        if (rpmt != null) {
            return rpmt;
        }
        rpmt = new SamplingTimer(this.mClocks, this.mOnBatteryTimeBase);
        this.mRpmStats.put(name, rpmt);
        return rpmt;
    }

    public SamplingTimer getScreenOffRpmTimerLocked(String name) {
        SamplingTimer rpmt = (SamplingTimer) this.mScreenOffRpmStats.get(name);
        if (rpmt != null) {
            return rpmt;
        }
        rpmt = new SamplingTimer(this.mClocks, this.mOnBatteryScreenOffTimeBase);
        this.mScreenOffRpmStats.put(name, rpmt);
        return rpmt;
    }

    public SamplingTimer getWakeupReasonTimerLocked(String name) {
        SamplingTimer timer = (SamplingTimer) this.mWakeupReasonStats.get(name);
        if (timer != null) {
            return timer;
        }
        timer = new SamplingTimer(this.mClocks, this.mOnBatteryTimeBase);
        this.mWakeupReasonStats.put(name, timer);
        return timer;
    }

    public SamplingTimer getKernelWakelockTimerLocked(String name) {
        SamplingTimer kwlt = (SamplingTimer) this.mKernelWakelockStats.get(name);
        if (kwlt != null) {
            return kwlt;
        }
        kwlt = new SamplingTimer(this.mClocks, this.mOnBatteryScreenOffTimeBase);
        this.mKernelWakelockStats.put(name, kwlt);
        return kwlt;
    }

    public SamplingTimer getKernelMemoryTimerLocked(long bucket) {
        SamplingTimer kmt = (SamplingTimer) this.mKernelMemoryStats.get(bucket);
        if (kmt != null) {
            return kmt;
        }
        kmt = new SamplingTimer(this.mClocks, this.mOnBatteryTimeBase);
        this.mKernelMemoryStats.put(bucket, kmt);
        return kmt;
    }

    private int writeHistoryTag(HistoryTag tag) {
        Integer idxObj = (Integer) this.mHistoryTagPool.get(tag);
        if (idxObj != null) {
            return idxObj.intValue();
        }
        int idx = this.mNextHistoryTagIdx;
        HistoryTag key = new HistoryTag();
        key.setTo(tag);
        tag.poolIdx = idx;
        this.mHistoryTagPool.put(key, Integer.valueOf(idx));
        this.mNextHistoryTagIdx++;
        this.mNumHistoryTagChars += key.string.length() + 1;
        return idx;
    }

    private void readHistoryTag(int index, HistoryTag tag) {
        if (index >= this.mReadHistoryStrings.length) {
            Slog.w(TAG, "readHistoryTag, index >= mReadHistoryStrings.length");
        } else if (index >= this.mReadHistoryUids.length) {
            Slog.w(TAG, "readHistoryTag, index >= mReadHistoryUids.length");
        } else {
            tag.string = this.mReadHistoryStrings[index];
            tag.uid = this.mReadHistoryUids[index];
            tag.poolIdx = index;
        }
    }

    public void writeHistoryDelta(Parcel dest, HistoryItem cur, HistoryItem last) {
        Parcel parcel = dest;
        HistoryItem historyItem = cur;
        HistoryItem historyItem2 = last;
        if (historyItem2 == null || historyItem.cmd != (byte) 0) {
            parcel.writeInt(DELTA_TIME_ABS);
            historyItem.writeToParcel(parcel, 0);
            return;
        }
        int deltaTimeToken;
        long deltaTime = historyItem.time - historyItem2.time;
        int lastBatteryLevelInt = buildBatteryLevelInt(historyItem2);
        int lastStateInt = buildStateInt(historyItem2);
        if (deltaTime < 0 || deltaTime > 2147483647L) {
            deltaTimeToken = EventLogTags.SYSUI_VIEW_VISIBILITY;
        } else if (deltaTime >= 524285) {
            deltaTimeToken = DELTA_TIME_INT;
        } else {
            deltaTimeToken = (int) deltaTime;
        }
        int firstToken = (historyItem.states & DELTA_STATE_MASK) | deltaTimeToken;
        int includeStepDetails = this.mLastHistoryStepLevel > historyItem.batteryLevel ? 1 : 0;
        boolean computeStepDetails = includeStepDetails != 0 || this.mLastHistoryStepDetails == null;
        int batteryLevelInt = buildBatteryLevelInt(historyItem) | includeStepDetails;
        boolean batteryLevelIntChanged = batteryLevelInt != lastBatteryLevelInt;
        if (batteryLevelIntChanged) {
            firstToken |= 524288;
        }
        int stateInt = buildStateInt(historyItem);
        boolean stateIntChanged = stateInt != lastStateInt;
        if (stateIntChanged) {
            firstToken |= DELTA_STATE_FLAG;
        }
        boolean state2IntChanged = historyItem.states2 != historyItem2.states2;
        if (state2IntChanged) {
            firstToken |= DELTA_STATE2_FLAG;
        }
        if (!(historyItem.wakelockTag == null && historyItem.wakeReasonTag == null)) {
            firstToken |= DELTA_WAKELOCK_FLAG;
        }
        if (historyItem.eventCode != 0) {
            firstToken |= DELTA_EVENT_FLAG;
        }
        boolean batteryChargeChanged = historyItem.batteryChargeUAh != historyItem2.batteryChargeUAh;
        if (batteryChargeChanged) {
            firstToken |= 16777216;
        }
        parcel.writeInt(firstToken);
        if (deltaTimeToken >= DELTA_TIME_INT) {
            if (deltaTimeToken == DELTA_TIME_INT) {
                parcel.writeInt((int) deltaTime);
            } else {
                parcel.writeLong(deltaTime);
            }
        }
        if (batteryLevelIntChanged) {
            parcel.writeInt(batteryLevelInt);
        }
        if (stateIntChanged) {
            parcel.writeInt(stateInt);
        }
        if (state2IntChanged) {
            parcel.writeInt(historyItem.states2);
        }
        if (!(historyItem.wakelockTag == null && historyItem.wakeReasonTag == null)) {
            int wakeReasonIndex;
            if (historyItem.wakelockTag != null) {
                lastStateInt = writeHistoryTag(historyItem.wakelockTag);
            } else {
                lastStateInt = 65535;
            }
            if (historyItem.wakeReasonTag != null) {
                wakeReasonIndex = writeHistoryTag(historyItem.wakeReasonTag);
            } else {
                wakeReasonIndex = 65535;
            }
            parcel.writeInt((wakeReasonIndex << 16) | lastStateInt);
        }
        if (historyItem.eventCode != 0) {
            parcel.writeInt((historyItem.eventCode & 65535) | (writeHistoryTag(historyItem.eventTag) << 16));
        }
        if (computeStepDetails) {
            if (this.mPlatformIdleStateCallback != null) {
                this.mCurHistoryStepDetails.statPlatformIdleState = this.mPlatformIdleStateCallback.getPlatformLowPowerStats();
                this.mCurHistoryStepDetails.statSubsystemPowerState = this.mPlatformIdleStateCallback.getSubsystemLowPowerStats();
            }
            computeHistoryStepDetails(this.mCurHistoryStepDetails, this.mLastHistoryStepDetails);
            if (includeStepDetails != 0) {
                this.mCurHistoryStepDetails.writeToParcel(parcel);
            }
            historyItem.stepDetails = this.mCurHistoryStepDetails;
            this.mLastHistoryStepDetails = this.mCurHistoryStepDetails;
        } else {
            historyItem.stepDetails = null;
        }
        if (this.mLastHistoryStepLevel < historyItem.batteryLevel) {
            this.mLastHistoryStepDetails = null;
        }
        this.mLastHistoryStepLevel = historyItem.batteryLevel;
        if (batteryChargeChanged) {
            parcel.writeInt(historyItem.batteryChargeUAh);
        }
    }

    private int buildBatteryLevelInt(HistoryItem h) {
        return (((h.batteryLevel << 25) & DELTA_STATE_MASK) | ((h.batteryTemperature << 15) & 33521664)) | ((h.batteryVoltage << 1) & 32766);
    }

    private void readBatteryLevelInt(int batteryLevelInt, HistoryItem out) {
        out.batteryLevel = (byte) ((DELTA_STATE_MASK & batteryLevelInt) >>> 25);
        out.batteryTemperature = (short) ((33521664 & batteryLevelInt) >>> 15);
        out.batteryVoltage = (char) ((batteryLevelInt & 32766) >>> 1);
    }

    private int buildStateInt(HistoryItem h) {
        int plugType = 0;
        if ((h.batteryPlugType & 1) != 0) {
            plugType = 1;
        } else if ((h.batteryPlugType & 2) != 0) {
            plugType = 2;
        } else if ((h.batteryPlugType & 4) != 0) {
            plugType = 3;
        }
        return ((((h.batteryStatus & 7) << 29) | ((h.batteryHealth & 7) << 26)) | ((plugType & 3) << 24)) | (h.states & AsyncService.CMD_ASYNC_SERVICE_ON_START_INTENT);
    }

    private void computeHistoryStepDetails(HistoryStepDetails out, HistoryStepDetails last) {
        HistoryStepDetails tmp = last != null ? this.mTmpHistoryStepDetails : out;
        requestImmediateCpuUpdate();
        int i = 0;
        int NU;
        Uid uid;
        if (last == null) {
            NU = this.mUidStats.size();
            while (i < NU) {
                uid = (Uid) this.mUidStats.valueAt(i);
                uid.mLastStepUserTime = uid.mCurStepUserTime;
                uid.mLastStepSystemTime = uid.mCurStepSystemTime;
                i++;
            }
            this.mLastStepCpuUserTime = this.mCurStepCpuUserTime;
            this.mLastStepCpuSystemTime = this.mCurStepCpuSystemTime;
            this.mLastStepStatUserTime = this.mCurStepStatUserTime;
            this.mLastStepStatSystemTime = this.mCurStepStatSystemTime;
            this.mLastStepStatIOWaitTime = this.mCurStepStatIOWaitTime;
            this.mLastStepStatIrqTime = this.mCurStepStatIrqTime;
            this.mLastStepStatSoftIrqTime = this.mCurStepStatSoftIrqTime;
            this.mLastStepStatIdleTime = this.mCurStepStatIdleTime;
            tmp.clear();
            return;
        }
        out.userTime = (int) (this.mCurStepCpuUserTime - this.mLastStepCpuUserTime);
        out.systemTime = (int) (this.mCurStepCpuSystemTime - this.mLastStepCpuSystemTime);
        out.statUserTime = (int) (this.mCurStepStatUserTime - this.mLastStepStatUserTime);
        out.statSystemTime = (int) (this.mCurStepStatSystemTime - this.mLastStepStatSystemTime);
        out.statIOWaitTime = (int) (this.mCurStepStatIOWaitTime - this.mLastStepStatIOWaitTime);
        out.statIrqTime = (int) (this.mCurStepStatIrqTime - this.mLastStepStatIrqTime);
        out.statSoftIrqTime = (int) (this.mCurStepStatSoftIrqTime - this.mLastStepStatSoftIrqTime);
        out.statIdlTime = (int) (this.mCurStepStatIdleTime - this.mLastStepStatIdleTime);
        out.appCpuUid3 = -1;
        out.appCpuUid2 = -1;
        out.appCpuUid1 = -1;
        out.appCpuUTime3 = 0;
        out.appCpuUTime2 = 0;
        out.appCpuUTime1 = 0;
        out.appCpuSTime3 = 0;
        out.appCpuSTime2 = 0;
        out.appCpuSTime1 = 0;
        NU = this.mUidStats.size();
        while (i < NU) {
            uid = (Uid) this.mUidStats.valueAt(i);
            int totalUTime = (int) (uid.mCurStepUserTime - uid.mLastStepUserTime);
            int totalSTime = (int) (uid.mCurStepSystemTime - uid.mLastStepSystemTime);
            int totalTime = totalUTime + totalSTime;
            uid.mLastStepUserTime = uid.mCurStepUserTime;
            uid.mLastStepSystemTime = uid.mCurStepSystemTime;
            if (totalTime > out.appCpuUTime3 + out.appCpuSTime3) {
                if (totalTime <= out.appCpuUTime2 + out.appCpuSTime2) {
                    out.appCpuUid3 = uid.mUid;
                    out.appCpuUTime3 = totalUTime;
                    out.appCpuSTime3 = totalSTime;
                } else {
                    out.appCpuUid3 = out.appCpuUid2;
                    out.appCpuUTime3 = out.appCpuUTime2;
                    out.appCpuSTime3 = out.appCpuSTime2;
                    if (totalTime <= out.appCpuUTime1 + out.appCpuSTime1) {
                        out.appCpuUid2 = uid.mUid;
                        out.appCpuUTime2 = totalUTime;
                        out.appCpuSTime2 = totalSTime;
                    } else {
                        out.appCpuUid2 = out.appCpuUid1;
                        out.appCpuUTime2 = out.appCpuUTime1;
                        out.appCpuSTime2 = out.appCpuSTime1;
                        out.appCpuUid1 = uid.mUid;
                        out.appCpuUTime1 = totalUTime;
                        out.appCpuSTime1 = totalSTime;
                    }
                }
            }
            i++;
        }
        this.mLastStepCpuUserTime = this.mCurStepCpuUserTime;
        this.mLastStepCpuSystemTime = this.mCurStepCpuSystemTime;
        this.mLastStepStatUserTime = this.mCurStepStatUserTime;
        this.mLastStepStatSystemTime = this.mCurStepStatSystemTime;
        this.mLastStepStatIOWaitTime = this.mCurStepStatIOWaitTime;
        this.mLastStepStatIrqTime = this.mCurStepStatIrqTime;
        this.mLastStepStatSoftIrqTime = this.mCurStepStatSoftIrqTime;
        this.mLastStepStatIdleTime = this.mCurStepStatIdleTime;
    }

    public void readHistoryDelta(Parcel src, HistoryItem cur) {
        int batteryLevelInt;
        int firstToken = src.readInt();
        int deltaTimeToken = EventLogTags.SYSUI_VIEW_VISIBILITY & firstToken;
        cur.cmd = (byte) 0;
        cur.numReadInts = 1;
        if (deltaTimeToken < DELTA_TIME_ABS) {
            cur.time += (long) deltaTimeToken;
        } else if (deltaTimeToken == DELTA_TIME_ABS) {
            cur.time = src.readLong();
            cur.numReadInts += 2;
            cur.readFromParcel(src);
            return;
        } else if (deltaTimeToken == DELTA_TIME_INT) {
            cur.time += (long) src.readInt();
            cur.numReadInts++;
        } else {
            cur.time += src.readLong();
            cur.numReadInts += 2;
        }
        if ((524288 & firstToken) != 0) {
            batteryLevelInt = src.readInt();
            readBatteryLevelInt(batteryLevelInt, cur);
            cur.numReadInts++;
        } else {
            batteryLevelInt = 0;
        }
        if ((DELTA_STATE_FLAG & firstToken) != 0) {
            int stateInt = src.readInt();
            cur.states = (AsyncService.CMD_ASYNC_SERVICE_ON_START_INTENT & stateInt) | (DELTA_STATE_MASK & firstToken);
            cur.batteryStatus = (byte) ((stateInt >> 29) & 7);
            cur.batteryHealth = (byte) ((stateInt >> 26) & 7);
            cur.batteryPlugType = (byte) ((stateInt >> 24) & 3);
            switch (cur.batteryPlugType) {
                case (byte) 1:
                    cur.batteryPlugType = (byte) 1;
                    break;
                case (byte) 2:
                    cur.batteryPlugType = (byte) 2;
                    break;
                case (byte) 3:
                    cur.batteryPlugType = (byte) 4;
                    break;
            }
            cur.numReadInts++;
        } else {
            cur.states = (firstToken & DELTA_STATE_MASK) | (cur.states & AsyncService.CMD_ASYNC_SERVICE_ON_START_INTENT);
        }
        if ((DELTA_STATE2_FLAG & firstToken) != 0) {
            cur.states2 = src.readInt();
        }
        if ((DELTA_WAKELOCK_FLAG & firstToken) != 0) {
            int indexes = src.readInt();
            int wakeLockIndex = indexes & 65535;
            int wakeReasonIndex = (indexes >> 16) & 65535;
            if (wakeLockIndex != 65535) {
                cur.wakelockTag = cur.localWakelockTag;
                readHistoryTag(wakeLockIndex, cur.wakelockTag);
            } else {
                cur.wakelockTag = null;
            }
            if (wakeReasonIndex != 65535) {
                cur.wakeReasonTag = cur.localWakeReasonTag;
                readHistoryTag(wakeReasonIndex, cur.wakeReasonTag);
            } else {
                cur.wakeReasonTag = null;
            }
            cur.numReadInts++;
        } else {
            cur.wakelockTag = null;
            cur.wakeReasonTag = null;
        }
        if ((DELTA_EVENT_FLAG & firstToken) != 0) {
            cur.eventTag = cur.localEventTag;
            int codeAndIndex = src.readInt();
            cur.eventCode = codeAndIndex & 65535;
            readHistoryTag((codeAndIndex >> 16) & 65535, cur.eventTag);
            cur.numReadInts++;
        } else {
            cur.eventCode = 0;
        }
        if ((batteryLevelInt & 1) != 0) {
            cur.stepDetails = this.mReadHistoryStepDetails;
            cur.stepDetails.readFromParcel(src);
        } else {
            cur.stepDetails = null;
        }
        if ((16777216 & firstToken) != 0) {
            cur.batteryChargeUAh = src.readInt();
        }
    }

    public void commitCurrentHistoryBatchLocked() {
        this.mHistoryLastWritten.cmd = (byte) -1;
    }

    void addHistoryBufferLocked(long elapsedRealtimeMs, HistoryItem cur) {
        HistoryItem historyItem = cur;
        if (this.mHaveBatteryLevel && this.mRecordingHistory) {
            long elapsedRealtimeMs2;
            long timeDiff = (this.mHistoryBaseTime + elapsedRealtimeMs) - this.mHistoryLastWritten.time;
            int diffStates = this.mHistoryLastWritten.states ^ (historyItem.states & this.mActiveHistoryStates);
            int diffStates2 = this.mHistoryLastWritten.states2 ^ (historyItem.states2 & this.mActiveHistoryStates2);
            int lastDiffStates = this.mHistoryLastWritten.states ^ this.mHistoryLastLastWritten.states;
            int lastDiffStates2 = this.mHistoryLastWritten.states2 ^ this.mHistoryLastLastWritten.states2;
            if (this.mHistoryBufferLastPos >= 0 && this.mHistoryLastWritten.cmd == (byte) 0 && timeDiff < 1000 && (diffStates & lastDiffStates) == 0 && (diffStates2 & lastDiffStates2) == 0 && ((this.mHistoryLastWritten.wakelockTag == null || historyItem.wakelockTag == null) && ((this.mHistoryLastWritten.wakeReasonTag == null || historyItem.wakeReasonTag == null) && this.mHistoryLastWritten.stepDetails == null && ((this.mHistoryLastWritten.eventCode == 0 || historyItem.eventCode == 0) && this.mHistoryLastWritten.batteryLevel == historyItem.batteryLevel && this.mHistoryLastWritten.batteryStatus == historyItem.batteryStatus && this.mHistoryLastWritten.batteryHealth == historyItem.batteryHealth && this.mHistoryLastWritten.batteryPlugType == historyItem.batteryPlugType && this.mHistoryLastWritten.batteryTemperature == historyItem.batteryTemperature && this.mHistoryLastWritten.batteryVoltage == historyItem.batteryVoltage)))) {
                this.mHistoryBuffer.setDataSize(this.mHistoryBufferLastPos);
                this.mHistoryBuffer.setDataPosition(this.mHistoryBufferLastPos);
                this.mHistoryBufferLastPos = -1;
                elapsedRealtimeMs2 = this.mHistoryLastWritten.time - this.mHistoryBaseTime;
                if (this.mHistoryLastWritten.wakelockTag != null) {
                    historyItem.wakelockTag = historyItem.localWakelockTag;
                    historyItem.wakelockTag.setTo(this.mHistoryLastWritten.wakelockTag);
                }
                if (this.mHistoryLastWritten.wakeReasonTag != null) {
                    historyItem.wakeReasonTag = historyItem.localWakeReasonTag;
                    historyItem.wakeReasonTag.setTo(this.mHistoryLastWritten.wakeReasonTag);
                }
                if (this.mHistoryLastWritten.eventCode != 0) {
                    historyItem.eventCode = this.mHistoryLastWritten.eventCode;
                    historyItem.eventTag = historyItem.localEventTag;
                    historyItem.eventTag.setTo(this.mHistoryLastWritten.eventTag);
                }
                this.mHistoryLastWritten.setTo(this.mHistoryLastLastWritten);
            } else {
                elapsedRealtimeMs2 = elapsedRealtimeMs;
            }
            boolean recordResetDueToOverflow = false;
            int dataSize = this.mHistoryBuffer.dataSize();
            if (dataSize >= MAX_MAX_HISTORY_BUFFER * 3) {
                resetAllStatsLocked();
                recordResetDueToOverflow = true;
                long j = timeDiff;
            } else if (dataSize < MAX_HISTORY_BUFFER) {
            } else if (this.mHistoryOverflow) {
                int old;
                boolean writeAnyway = false;
                int curStates = (historyItem.states & -1900544) & this.mActiveHistoryStates;
                if (this.mHistoryLastWritten.states != curStates) {
                    old = this.mActiveHistoryStates;
                    this.mActiveHistoryStates &= curStates | 1900543;
                    writeAnyway = false | (old != this.mActiveHistoryStates ? 1 : 0);
                }
                int curStates2 = (historyItem.states2 & 1748959232) & this.mActiveHistoryStates2;
                if (this.mHistoryLastWritten.states2 != curStates2) {
                    old = this.mActiveHistoryStates2;
                    this.mActiveHistoryStates2 &= -1748959233 | curStates2;
                    writeAnyway |= old != this.mActiveHistoryStates2 ? 1 : 0;
                }
                if (writeAnyway || this.mHistoryLastWritten.batteryLevel != historyItem.batteryLevel || (dataSize < MAX_MAX_HISTORY_BUFFER && ((this.mHistoryLastWritten.states ^ historyItem.states) & 1835008) != 0 && ((this.mHistoryLastWritten.states2 ^ historyItem.states2) & -1749024768) != 0)) {
                    addHistoryBufferLocked(elapsedRealtimeMs2, (byte) 0, historyItem);
                    return;
                }
                return;
            } else {
                this.mHistoryOverflow = true;
                addHistoryBufferLocked(elapsedRealtimeMs2, (byte) 0, historyItem);
                addHistoryBufferLocked(elapsedRealtimeMs2, (byte) 6, historyItem);
                return;
            }
            if (dataSize == 0 || recordResetDueToOverflow) {
                historyItem.currentTime = System.currentTimeMillis();
                if (recordResetDueToOverflow) {
                    addHistoryBufferLocked(elapsedRealtimeMs2, (byte) 6, historyItem);
                }
                addHistoryBufferLocked(elapsedRealtimeMs2, (byte) 7, historyItem);
            }
            addHistoryBufferLocked(elapsedRealtimeMs2, (byte) 0, historyItem);
        }
    }

    private void addHistoryBufferLocked(long elapsedRealtimeMs, byte cmd, HistoryItem cur) {
        if (this.mIteratingHistory) {
            throw new IllegalStateException("Can't do this while iterating history!");
        }
        this.mHistoryBufferLastPos = this.mHistoryBuffer.dataPosition();
        this.mHistoryLastLastWritten.setTo(this.mHistoryLastWritten);
        this.mHistoryLastWritten.setTo(this.mHistoryBaseTime + elapsedRealtimeMs, cmd, cur);
        HistoryItem historyItem = this.mHistoryLastWritten;
        historyItem.states &= this.mActiveHistoryStates;
        historyItem = this.mHistoryLastWritten;
        historyItem.states2 &= this.mActiveHistoryStates2;
        writeHistoryDelta(this.mHistoryBuffer, this.mHistoryLastWritten, this.mHistoryLastLastWritten);
        this.mLastHistoryElapsedRealtime = elapsedRealtimeMs;
        cur.wakelockTag = null;
        cur.wakeReasonTag = null;
        cur.eventCode = 0;
        cur.eventTag = null;
    }

    void addHistoryRecordLocked(long elapsedRealtimeMs, long uptimeMs) {
        if (this.mTrackRunningHistoryElapsedRealtime != 0) {
            long diffElapsed = elapsedRealtimeMs - this.mTrackRunningHistoryElapsedRealtime;
            long diffUptime = uptimeMs - this.mTrackRunningHistoryUptime;
            if (diffUptime < diffElapsed - 20) {
                long wakeElapsedTime = elapsedRealtimeMs - (diffElapsed - diffUptime);
                this.mHistoryAddTmp.setTo(this.mHistoryLastWritten);
                this.mHistoryAddTmp.wakelockTag = null;
                this.mHistoryAddTmp.wakeReasonTag = null;
                this.mHistoryAddTmp.eventCode = 0;
                HistoryItem historyItem = this.mHistoryAddTmp;
                historyItem.states &= Integer.MAX_VALUE;
                addHistoryRecordInnerLocked(wakeElapsedTime, this.mHistoryAddTmp);
            }
        }
        HistoryItem historyItem2 = this.mHistoryCur;
        historyItem2.states |= Integer.MIN_VALUE;
        this.mTrackRunningHistoryElapsedRealtime = elapsedRealtimeMs;
        this.mTrackRunningHistoryUptime = uptimeMs;
        addHistoryRecordInnerLocked(elapsedRealtimeMs, this.mHistoryCur);
    }

    void addHistoryRecordInnerLocked(long elapsedRealtimeMs, HistoryItem cur) {
        addHistoryBufferLocked(elapsedRealtimeMs, cur);
    }

    public void addHistoryEventLocked(long elapsedRealtimeMs, long uptimeMs, int code, String name, int uid) {
        this.mHistoryCur.eventCode = code;
        this.mHistoryCur.eventTag = this.mHistoryCur.localEventTag;
        this.mHistoryCur.eventTag.string = name;
        this.mHistoryCur.eventTag.uid = uid;
        addHistoryRecordLocked(elapsedRealtimeMs, uptimeMs);
    }

    void addHistoryRecordLocked(long elapsedRealtimeMs, long uptimeMs, byte cmd, HistoryItem cur) {
        HistoryItem rec = this.mHistoryCache;
        if (rec != null) {
            this.mHistoryCache = rec.next;
        } else {
            rec = new HistoryItem();
        }
        rec.setTo(this.mHistoryBaseTime + elapsedRealtimeMs, cmd, cur);
        addHistoryRecordLocked(rec);
    }

    void addHistoryRecordLocked(HistoryItem rec) {
        this.mNumHistoryItems++;
        rec.next = null;
        this.mHistoryLastEnd = this.mHistoryEnd;
        if (this.mHistoryEnd != null) {
            this.mHistoryEnd.next = rec;
            this.mHistoryEnd = rec;
            return;
        }
        this.mHistoryEnd = rec;
        this.mHistory = rec;
    }

    void clearHistoryLocked() {
        this.mHistoryBaseTime = 0;
        this.mLastHistoryElapsedRealtime = 0;
        this.mTrackRunningHistoryElapsedRealtime = 0;
        this.mTrackRunningHistoryUptime = 0;
        this.mHistoryBuffer.setDataSize(0);
        this.mHistoryBuffer.setDataPosition(0);
        this.mHistoryBuffer.setDataCapacity(MAX_HISTORY_BUFFER / 2);
        this.mHistoryLastLastWritten.clear();
        this.mHistoryLastWritten.clear();
        this.mHistoryTagPool.clear();
        this.mNextHistoryTagIdx = 0;
        this.mNumHistoryTagChars = 0;
        this.mHistoryBufferLastPos = -1;
        this.mHistoryOverflow = false;
        this.mActiveHistoryStates = -1;
        this.mActiveHistoryStates2 = -1;
    }

    @GuardedBy("this")
    public void updateTimeBasesLocked(boolean unplugged, int screenState, long uptime, long realtime) {
        boolean z = unplugged;
        long j = uptime;
        long j2 = realtime;
        boolean screenOff = isScreenOn(screenState) ^ 1;
        boolean updateOnBatteryTimeBase = z != this.mOnBatteryTimeBase.isRunning();
        boolean z2 = z && screenOff;
        boolean updateOnBatteryScreenOffTimeBase = z2 != this.mOnBatteryScreenOffTimeBase.isRunning();
        if (updateOnBatteryScreenOffTimeBase || updateOnBatteryTimeBase) {
            int i;
            if (updateOnBatteryScreenOffTimeBase) {
                updateKernelWakelocksLocked();
                updateBatteryPropertiesLocked();
            }
            if (updateOnBatteryTimeBase) {
                updateRpmStatsLocked();
            }
            this.mOnBatteryTimeBase.setRunning(z, j, j2);
            if (updateOnBatteryTimeBase) {
                for (i = this.mUidStats.size() - 1; i >= 0; i--) {
                    ((Uid) this.mUidStats.valueAt(i)).updateOnBatteryBgTimeBase(j, j2);
                }
            }
            if (updateOnBatteryScreenOffTimeBase) {
                TimeBase timeBase = this.mOnBatteryScreenOffTimeBase;
                boolean z3 = z && screenOff;
                timeBase.setRunning(z3, j, j2);
                for (i = this.mUidStats.size() - 1; i >= 0; i--) {
                    ((Uid) this.mUidStats.valueAt(i)).updateOnBatteryScreenOffBgTimeBase(j, j2);
                }
            }
        }
    }

    private void updateBatteryPropertiesLocked() {
        try {
            IBatteryPropertiesRegistrar registrar = Stub.asInterface(ServiceManager.getService("batteryproperties"));
            if (registrar != null) {
                registrar.scheduleUpdate();
            }
        } catch (RemoteException e) {
        }
    }

    public void addIsolatedUidLocked(int isolatedUid, int appUid) {
        this.mIsolatedUids.put(isolatedUid, appUid);
        StatsLog.write(43, appUid, isolatedUid, 1);
        getUidStatsLocked(appUid).addIsolatedUid(isolatedUid);
    }

    public void scheduleRemoveIsolatedUidLocked(int isolatedUid, int appUid) {
        if (this.mIsolatedUids.get(isolatedUid, -1) == appUid && this.mExternalSync != null) {
            this.mExternalSync.scheduleCpuSyncDueToRemovedUid(isolatedUid);
        }
    }

    @GuardedBy("this")
    public void removeIsolatedUidLocked(int isolatedUid) {
        StatsLog.write(43, this.mIsolatedUids.get(isolatedUid, -1), isolatedUid, 0);
        int idx = this.mIsolatedUids.indexOfKey(isolatedUid);
        if (idx >= 0) {
            getUidStatsLocked(this.mIsolatedUids.valueAt(idx)).removeIsolatedUid(isolatedUid);
            this.mIsolatedUids.removeAt(idx);
        }
        this.mPendingRemovedUids.add(new UidToRemove(this, isolatedUid, this.mClocks.elapsedRealtime()));
    }

    public int mapUid(int uid) {
        int isolated = this.mIsolatedUids.get(uid, -1);
        return isolated > 0 ? isolated : uid;
    }

    public void noteEventLocked(int code, String name, int uid) {
        uid = mapUid(uid);
        if (this.mActiveEvents.updateState(code, name, uid, 0)) {
            addHistoryEventLocked(this.mClocks.elapsedRealtime(), this.mClocks.uptimeMillis(), code, name, uid);
        }
    }

    boolean ensureStartClockTime(long currentTime) {
        if ((currentTime <= 31536000000L || this.mStartClockTime >= currentTime - 31536000000L) && this.mStartClockTime <= currentTime) {
            return false;
        }
        this.mStartClockTime = currentTime - (this.mClocks.elapsedRealtime() - (this.mRealtimeStart / 1000));
        return true;
    }

    public void noteCurrentTimeChangedLocked() {
        long currentTime = System.currentTimeMillis();
        recordCurrentTimeChangeLocked(currentTime, this.mClocks.elapsedRealtime(), this.mClocks.uptimeMillis());
        ensureStartClockTime(currentTime);
    }

    public void noteProcessStartLocked(String name, int uid) {
        uid = mapUid(uid);
        if (isOnBattery()) {
            getUidStatsLocked(uid).getProcessStatsLocked(name).incStartsLocked();
        }
        if (this.mActiveEvents.updateState(32769, name, uid, 0) && this.mRecordAllHistory) {
            addHistoryEventLocked(this.mClocks.elapsedRealtime(), this.mClocks.uptimeMillis(), 32769, name, uid);
        }
    }

    public void noteProcessCrashLocked(String name, int uid) {
        uid = mapUid(uid);
        if (isOnBattery()) {
            getUidStatsLocked(uid).getProcessStatsLocked(name).incNumCrashesLocked();
        }
    }

    public void noteProcessAnrLocked(String name, int uid) {
        uid = mapUid(uid);
        if (isOnBattery()) {
            getUidStatsLocked(uid).getProcessStatsLocked(name).incNumAnrsLocked();
        }
    }

    public void noteUidProcessStateLocked(int uid, int state) {
        if (uid == mapUid(uid)) {
            getUidStatsLocked(uid).updateUidProcessStateLocked(state);
        }
    }

    public void noteProcessFinishLocked(String name, int uid) {
        uid = mapUid(uid);
        if (this.mActiveEvents.updateState(GL10.GL_LIGHT1, name, uid, 0) && this.mRecordAllHistory) {
            addHistoryEventLocked(this.mClocks.elapsedRealtime(), this.mClocks.uptimeMillis(), GL10.GL_LIGHT1, name, uid);
        }
    }

    public void noteSyncStartLocked(String name, int uid) {
        uid = mapUid(uid);
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        getUidStatsLocked(uid).noteStartSyncLocked(name, elapsedRealtime);
        if (this.mActiveEvents.updateState(32772, name, uid, 0)) {
            addHistoryEventLocked(elapsedRealtime, uptime, 32772, name, uid);
        }
    }

    public void noteSyncFinishLocked(String name, int uid) {
        uid = mapUid(uid);
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        getUidStatsLocked(uid).noteStopSyncLocked(name, elapsedRealtime);
        if (this.mActiveEvents.updateState(GL10.GL_LIGHT4, name, uid, 0)) {
            addHistoryEventLocked(elapsedRealtime, uptime, GL10.GL_LIGHT4, name, uid);
        }
    }

    public void noteJobStartLocked(String name, int uid) {
        uid = mapUid(uid);
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        getUidStatsLocked(uid).noteStartJobLocked(name, elapsedRealtime);
        if (this.mActiveEvents.updateState(GL11ExtensionPack.GL_FUNC_ADD, name, uid, 0)) {
            addHistoryEventLocked(elapsedRealtime, uptime, GL11ExtensionPack.GL_FUNC_ADD, name, uid);
        }
    }

    public void noteJobFinishLocked(String name, int uid, int stopReason) {
        uid = mapUid(uid);
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        getUidStatsLocked(uid).noteStopJobLocked(name, elapsedRealtime, stopReason);
        if (this.mActiveEvents.updateState(GL10.GL_LIGHT6, name, uid, 0)) {
            addHistoryEventLocked(elapsedRealtime, uptime, GL10.GL_LIGHT6, name, uid);
        }
    }

    public void noteJobsDeferredLocked(int uid, int numDeferred, long sinceLast) {
        getUidStatsLocked(mapUid(uid)).noteJobsDeferredLocked(numDeferred, sinceLast);
    }

    public void noteAlarmStartLocked(String name, WorkSource workSource, int uid) {
        noteAlarmStartOrFinishLocked(32781, name, workSource, uid);
    }

    public void noteAlarmFinishLocked(String name, WorkSource workSource, int uid) {
        noteAlarmStartOrFinishLocked(16397, name, workSource, uid);
    }

    private void noteAlarmStartOrFinishLocked(int historyItem, String name, WorkSource workSource, int uid) {
        int i = historyItem;
        String str = name;
        WorkSource workSource2 = workSource;
        if (this.mRecordAllHistory) {
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            long uptime = this.mClocks.uptimeMillis();
            int i2 = 0;
            long uptime2;
            int i3;
            int i4;
            if (workSource2 != null) {
                int i5;
                int uid2;
                int i6;
                int uid3 = uid;
                int i7 = 0;
                while (true) {
                    i5 = i7;
                    if (i5 >= workSource.size()) {
                        break;
                    }
                    int uid4 = mapUid(workSource2.get(i5));
                    if (this.mActiveEvents.updateState(i, str, uid4, i2)) {
                        uid2 = uid4;
                        i6 = i5;
                        uptime2 = uptime;
                        uptime = i2;
                        addHistoryEventLocked(elapsedRealtime, uptime, i, str, uid2);
                    } else {
                        uid2 = uid4;
                        i6 = i5;
                        uptime2 = uptime;
                        uptime = i2;
                    }
                    i7 = i6 + 1;
                    i2 = uptime;
                    uid3 = uid2;
                    uptime = uptime2;
                }
                uptime2 = uptime;
                i3 = i2;
                List<WorkChain> workChains = workSource.getWorkChains();
                if (workChains != null) {
                    i7 = i3;
                    while (true) {
                        i2 = i7;
                        if (i2 >= workChains.size()) {
                            break;
                        }
                        i5 = mapUid(((WorkChain) workChains.get(i2)).getAttributionUid());
                        if (this.mActiveEvents.updateState(i, str, i5, i3)) {
                            uid2 = i5;
                            i6 = i2;
                            addHistoryEventLocked(elapsedRealtime, uptime2, i, str, uid2);
                        } else {
                            uid2 = i5;
                            i6 = i2;
                        }
                        i7 = i6 + 1;
                        uid3 = uid2;
                    }
                }
                i4 = uid3;
            } else {
                uptime2 = uptime;
                i3 = 0;
                i4 = mapUid(uid);
                if (this.mActiveEvents.updateState(i, str, i4, i3)) {
                    addHistoryEventLocked(elapsedRealtime, uptime2, i, str, i4);
                }
            }
        }
    }

    public void noteWakupAlarmLocked(String packageName, int uid, WorkSource workSource, String tag) {
        if (workSource != null) {
            int i = 0;
            int uid2 = uid;
            for (uid = 0; uid < workSource.size(); uid++) {
                uid2 = workSource.get(uid);
                String workSourceName = workSource.getName(uid);
                if (isOnBattery()) {
                    getPackageStatsLocked(uid2, workSourceName != null ? workSourceName : packageName).noteWakeupAlarmLocked(tag);
                }
                StatsLog.write_non_chained(35, workSource.get(uid), workSource.getName(uid), tag);
            }
            ArrayList<WorkChain> workChains = workSource.getWorkChains();
            if (workChains != null) {
                while (i < workChains.size()) {
                    WorkChain wc = (WorkChain) workChains.get(i);
                    uid2 = wc.getAttributionUid();
                    if (isOnBattery()) {
                        getPackageStatsLocked(uid2, packageName).noteWakeupAlarmLocked(tag);
                    }
                    StatsLog.write(35, wc.getUids(), wc.getTags(), tag);
                    i++;
                }
            }
            return;
        }
        if (isOnBattery()) {
            getPackageStatsLocked(uid, packageName).noteWakeupAlarmLocked(tag);
        }
        StatsLog.write_non_chained(35, uid, null, tag);
    }

    private void requestWakelockCpuUpdate() {
        this.mExternalSync.scheduleCpuSyncDueToWakelockChange(DELAY_UPDATE_WAKELOCKS);
    }

    private void requestImmediateCpuUpdate() {
        this.mExternalSync.scheduleCpuSyncDueToWakelockChange(0);
    }

    public void setRecordAllHistoryLocked(boolean enabled) {
        boolean z = enabled;
        this.mRecordAllHistory = z;
        HashMap<String, SparseIntArray> active;
        long mSecRealtime;
        long mSecUptime;
        Iterator it;
        Entry<String, SparseIntArray> ent;
        SparseIntArray uids;
        int j;
        int j2;
        String str;
        int j3;
        SparseIntArray uids2;
        Entry<String, SparseIntArray> ent2;
        String str2;
        Iterator it2;
        if (z) {
            active = this.mActiveEvents.getStateForEvent(1);
            if (active != null) {
                mSecRealtime = this.mClocks.elapsedRealtime();
                mSecUptime = this.mClocks.uptimeMillis();
                it = active.entrySet().iterator();
                while (it.hasNext()) {
                    ent = (Entry) it.next();
                    uids = (SparseIntArray) ent.getValue();
                    j = 0;
                    while (true) {
                        j2 = j;
                        if (j2 >= uids.size()) {
                            break;
                        }
                        str = (String) ent.getKey();
                        j3 = j2;
                        uids2 = uids;
                        ent2 = ent;
                        str2 = str;
                        it2 = it;
                        addHistoryEventLocked(mSecRealtime, mSecUptime, 32769, str2, uids.keyAt(j2));
                        j = j3 + 1;
                        ent = ent2;
                        it = it2;
                        uids = uids2;
                    }
                    it2 = it;
                }
                return;
            }
            return;
        }
        this.mActiveEvents.removeEvents(5);
        this.mActiveEvents.removeEvents(13);
        active = this.mActiveEvents.getStateForEvent(1);
        if (active != null) {
            mSecRealtime = this.mClocks.elapsedRealtime();
            mSecUptime = this.mClocks.uptimeMillis();
            it = active.entrySet().iterator();
            while (it.hasNext()) {
                ent = (Entry) it.next();
                uids = (SparseIntArray) ent.getValue();
                j = 0;
                while (true) {
                    j2 = j;
                    if (j2 >= uids.size()) {
                        break;
                    }
                    str = (String) ent.getKey();
                    j3 = j2;
                    uids2 = uids;
                    ent2 = ent;
                    str2 = str;
                    it2 = it;
                    addHistoryEventLocked(mSecRealtime, mSecUptime, GL10.GL_LIGHT1, str2, uids.keyAt(j2));
                    j = j3 + 1;
                    ent = ent2;
                    it = it2;
                    uids = uids2;
                }
                it2 = it;
            }
        }
    }

    public void setNoAutoReset(boolean enabled) {
        this.mNoAutoReset = enabled;
    }

    public void setPretendScreenOff(boolean pretendScreenOff) {
        if (this.mPretendScreenOff != pretendScreenOff) {
            this.mPretendScreenOff = pretendScreenOff;
            noteScreenStateLocked(pretendScreenOff ? 1 : 2);
        }
    }

    public void noteStartWakeLocked(int uid, int pid, WorkChain wc, String name, String historyName, int type, boolean unimportantForLogging, long elapsedRealtime, long uptime) {
        int i = type;
        long j = elapsedRealtime;
        long j2 = uptime;
        int uid2 = mapUid(uid);
        if (i == 0) {
            String historyName2;
            aggregateLastWakeupUptimeLocked(j2);
            String historyName3 = historyName == null ? name : historyName;
            if (this.mRecordAllHistory && this.mActiveEvents.updateState(32773, historyName3, uid2, 0)) {
                historyName2 = historyName3;
                addHistoryEventLocked(j, j2, 32773, historyName3, uid2);
            } else {
                historyName2 = historyName3;
            }
            HistoryTag historyTag;
            if (this.mWakeLockNesting == 0) {
                HistoryItem historyItem = this.mHistoryCur;
                historyItem.states |= 1073741824;
                this.mHistoryCur.wakelockTag = this.mHistoryCur.localWakelockTag;
                historyTag = this.mHistoryCur.wakelockTag;
                this.mInitialAcquireWakeName = historyName2;
                historyTag.string = historyName2;
                historyTag = this.mHistoryCur.wakelockTag;
                this.mInitialAcquireWakeUid = uid2;
                historyTag.uid = uid2;
                this.mWakeLockImportant = unimportantForLogging ^ 1;
                addHistoryRecordLocked(j, j2);
            } else if (!(this.mWakeLockImportant || unimportantForLogging || this.mHistoryLastWritten.cmd != (byte) 0)) {
                if (this.mHistoryLastWritten.wakelockTag != null) {
                    this.mHistoryLastWritten.wakelockTag = null;
                    this.mHistoryCur.wakelockTag = this.mHistoryCur.localWakelockTag;
                    historyTag = this.mHistoryCur.wakelockTag;
                    this.mInitialAcquireWakeName = historyName2;
                    historyTag.string = historyName2;
                    historyTag = this.mHistoryCur.wakelockTag;
                    this.mInitialAcquireWakeUid = uid2;
                    historyTag.uid = uid2;
                    addHistoryRecordLocked(j, j2);
                }
                this.mWakeLockImportant = true;
            }
            this.mWakeLockNesting++;
        }
        int i2;
        if (uid2 >= 0) {
            if (this.mOnBatteryScreenOffTimeBase.isRunning()) {
                requestWakelockCpuUpdate();
            }
            i2 = type;
            getUidStatsLocked(uid2).noteStartWakeLocked(pid, name, i2, j);
            if (wc != null) {
                StatsLog.write(10, wc.getUids(), wc.getTags(), getPowerManagerWakeLockLevel(i2), name, 1);
                return;
            } else {
                StatsLog.write_non_chained(10, uid2, null, getPowerManagerWakeLockLevel(i2), name, 1);
                return;
            }
        }
        i2 = type;
    }

    public void noteStopWakeLocked(int uid, int pid, WorkChain wc, String name, String historyName, int type, long elapsedRealtime, long uptime) {
        long j;
        int i = type;
        int uid2 = mapUid(uid);
        String historyName2;
        long j2;
        if (i == 0) {
            this.mWakeLockNesting--;
            if (this.mRecordAllHistory) {
                historyName2 = historyName == null ? name : historyName;
                if (this.mActiveEvents.updateState(GL10.GL_LIGHT5, historyName2, uid2, 0)) {
                    addHistoryEventLocked(elapsedRealtime, uptime, GL10.GL_LIGHT5, historyName2, uid2);
                }
            }
            if (this.mWakeLockNesting == 0) {
                HistoryItem historyItem = this.mHistoryCur;
                historyItem.states &= -1073741825;
                this.mInitialAcquireWakeName = null;
                this.mInitialAcquireWakeUid = -1;
                j = elapsedRealtime;
                addHistoryRecordLocked(j, uptime);
            } else {
                j = elapsedRealtime;
                j2 = uptime;
            }
        } else {
            j = elapsedRealtime;
            j2 = uptime;
            historyName2 = historyName;
        }
        if (uid2 >= 0) {
            if (this.mOnBatteryScreenOffTimeBase.isRunning()) {
                requestWakelockCpuUpdate();
            }
            getUidStatsLocked(uid2).noteStopWakeLocked(pid, name, i, j);
            if (wc != null) {
                StatsLog.write(10, wc.getUids(), wc.getTags(), getPowerManagerWakeLockLevel(i), name, 0);
            } else {
                StatsLog.write_non_chained(10, uid2, null, getPowerManagerWakeLockLevel(i), name, 0);
            }
        }
    }

    private int getPowerManagerWakeLockLevel(int battertStatsWakelockType) {
        if (battertStatsWakelockType == 18) {
            return 128;
        }
        switch (battertStatsWakelockType) {
            case 0:
                return 1;
            case 1:
                return 26;
            case 2:
                Slog.e(TAG, "Illegal window wakelock type observed in batterystats.");
                return -1;
            default:
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Illegal wakelock type in batterystats: ");
                stringBuilder.append(battertStatsWakelockType);
                Slog.e(str, stringBuilder.toString());
                return -1;
        }
    }

    public void noteStartWakeFromSourceLocked(WorkSource ws, int pid, String name, String historyName, int type, boolean unimportantForLogging) {
        int i;
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        int N = ws.size();
        int i2 = 0;
        int i3 = 0;
        while (true) {
            i = i3;
            if (i >= N) {
                break;
            }
            int N2 = N;
            int i4 = i;
            noteStartWakeLocked(ws.get(i), pid, null, name, historyName, type, unimportantForLogging, elapsedRealtime, uptime);
            i3 = i4 + 1;
            N = N2;
        }
        List<WorkChain> wcs = ws.getWorkChains();
        if (wcs != null) {
            while (true) {
                i = i2;
                if (i >= wcs.size()) {
                    break;
                }
                WorkChain wc = (WorkChain) wcs.get(i);
                List<WorkChain> wcs2 = wcs;
                int i5 = i;
                noteStartWakeLocked(wc.getAttributionUid(), pid, wc, name, historyName, type, unimportantForLogging, elapsedRealtime, uptime);
                i2 = i5 + 1;
                wcs = wcs2;
            }
        }
    }

    public void noteChangeWakelockFromSourceLocked(WorkSource ws, int pid, String name, String historyName, int type, WorkSource newWs, int newPid, String newName, String newHistoryName, int newType, boolean newUnimportantForLogging) {
        int i;
        int i2;
        WorkSource workSource = ws;
        WorkSource workSource2 = newWs;
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        List<WorkChain>[] wcs = WorkSource.diffChains(workSource, workSource2);
        int NN = newWs.size();
        int i3 = 0;
        int i4 = 0;
        while (true) {
            i = i4;
            if (i >= NN) {
                break;
            }
            int NN2 = NN;
            i2 = i;
            noteStartWakeLocked(workSource2.get(i), newPid, null, newName, newHistoryName, newType, newUnimportantForLogging, elapsedRealtime, uptime);
            i4 = i2 + 1;
            NN = NN2;
        }
        if (wcs != null) {
            List<WorkChain> newChains = wcs[0];
            if (newChains != null) {
                i4 = 0;
                while (true) {
                    i = i4;
                    if (i >= newChains.size()) {
                        break;
                    }
                    WorkChain newChain = (WorkChain) newChains.get(i);
                    List<WorkChain> newChains2 = newChains;
                    int i5 = i;
                    noteStartWakeLocked(newChain.getAttributionUid(), newPid, newChain, newName, newHistoryName, newType, newUnimportantForLogging, elapsedRealtime, uptime);
                    i4 = i5 + 1;
                    newChains = newChains2;
                }
            }
        }
        i = ws.size();
        i4 = 0;
        while (true) {
            int i6 = i4;
            if (i6 >= i) {
                break;
            }
            i2 = i6;
            noteStopWakeLocked(workSource.get(i6), pid, null, name, historyName, type, elapsedRealtime, uptime);
            i4 = i2 + 1;
        }
        if (wcs != null) {
            List<WorkChain> goneChains = wcs[1];
            if (goneChains != null) {
                while (true) {
                    NN = i3;
                    if (NN < goneChains.size()) {
                        WorkChain goneChain = (WorkChain) goneChains.get(NN);
                        List<WorkChain> goneChains2 = goneChains;
                        int i7 = NN;
                        noteStopWakeLocked(goneChain.getAttributionUid(), pid, goneChain, name, historyName, type, elapsedRealtime, uptime);
                        i3 = i7 + 1;
                        goneChains = goneChains2;
                    } else {
                        return;
                    }
                }
            }
        }
    }

    public void noteStopWakeFromSourceLocked(WorkSource ws, int pid, String name, String historyName, int type) {
        int i;
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        int N = ws.size();
        int i2 = 0;
        int i3 = 0;
        while (true) {
            i = i3;
            if (i >= N) {
                break;
            }
            int N2 = N;
            int i4 = i;
            noteStopWakeLocked(ws.get(i), pid, null, name, historyName, type, elapsedRealtime, uptime);
            i3 = i4 + 1;
            N = N2;
        }
        List<WorkChain> wcs = ws.getWorkChains();
        if (wcs != null) {
            while (true) {
                i = i2;
                if (i >= wcs.size()) {
                    break;
                }
                WorkChain wc = (WorkChain) wcs.get(i);
                List<WorkChain> wcs2 = wcs;
                int i5 = i;
                noteStopWakeLocked(wc.getAttributionUid(), pid, wc, name, historyName, type, elapsedRealtime, uptime);
                i2 = i5 + 1;
                wcs = wcs2;
            }
        }
    }

    public void noteLongPartialWakelockStart(String name, String historyName, int uid) {
        StatsLog.write_non_chained(11, uid, null, name, historyName, 1);
        noteLongPartialWakeLockStartInternal(name, historyName, mapUid(uid));
    }

    public void noteLongPartialWakelockStartFromSource(String name, String historyName, WorkSource workSource) {
        int N = workSource.size();
        int i = 0;
        for (int i2 = 0; i2 < N; i2++) {
            noteLongPartialWakeLockStartInternal(name, historyName, mapUid(workSource.get(i2)));
            StatsLog.write_non_chained(11, workSource.get(i2), workSource.getName(i2), name, historyName, 1);
        }
        ArrayList<WorkChain> workChains = workSource.getWorkChains();
        if (workChains != null) {
            while (i < workChains.size()) {
                WorkChain workChain = (WorkChain) workChains.get(i);
                noteLongPartialWakeLockStartInternal(name, historyName, workChain.getAttributionUid());
                StatsLog.write(11, workChain.getUids(), workChain.getTags(), name, historyName, 1);
                i++;
            }
        }
    }

    private void noteLongPartialWakeLockStartInternal(String name, String historyName, int uid) {
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        String historyName2 = historyName == null ? name : historyName;
        int i = uid;
        if (this.mActiveEvents.updateState(32788, historyName2, i, 0)) {
            addHistoryEventLocked(elapsedRealtime, uptime, 32788, historyName2, i);
        }
    }

    public void noteLongPartialWakelockFinish(String name, String historyName, int uid) {
        StatsLog.write_non_chained(11, uid, null, name, historyName, 0);
        noteLongPartialWakeLockFinishInternal(name, historyName, mapUid(uid));
    }

    public void noteLongPartialWakelockFinishFromSource(String name, String historyName, WorkSource workSource) {
        int N = workSource.size();
        int i = 0;
        for (int i2 = 0; i2 < N; i2++) {
            noteLongPartialWakeLockFinishInternal(name, historyName, mapUid(workSource.get(i2)));
            StatsLog.write_non_chained(11, workSource.get(i2), workSource.getName(i2), name, historyName, 0);
        }
        ArrayList<WorkChain> workChains = workSource.getWorkChains();
        if (workChains != null) {
            while (i < workChains.size()) {
                WorkChain workChain = (WorkChain) workChains.get(i);
                noteLongPartialWakeLockFinishInternal(name, historyName, workChain.getAttributionUid());
                StatsLog.write(11, workChain.getUids(), workChain.getTags(), name, historyName, 0);
                i++;
            }
        }
    }

    private void noteLongPartialWakeLockFinishInternal(String name, String historyName, int uid) {
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        String historyName2 = historyName == null ? name : historyName;
        int i = uid;
        if (this.mActiveEvents.updateState(16404, historyName2, i, 0)) {
            addHistoryEventLocked(elapsedRealtime, uptime, 16404, historyName2, i);
        }
    }

    void aggregateLastWakeupUptimeLocked(long uptimeMs) {
        if (this.mLastWakeupReason != null) {
            long deltaUptime = uptimeMs - this.mLastWakeupUptimeMs;
            getWakeupReasonTimerLocked(this.mLastWakeupReason).add(deltaUptime * 1000, 1);
            StatsLog.write(36, this.mLastWakeupReason, 1000 * deltaUptime);
            this.mLastWakeupReason = null;
        }
    }

    public void noteWakeupReasonLocked(String reason) {
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        aggregateLastWakeupUptimeLocked(uptime);
        this.mHistoryCur.wakeReasonTag = this.mHistoryCur.localWakeReasonTag;
        this.mHistoryCur.wakeReasonTag.string = reason;
        this.mHistoryCur.wakeReasonTag.uid = 0;
        this.mLastWakeupReason = reason;
        this.mLastWakeupUptimeMs = uptime;
        addHistoryRecordLocked(elapsedRealtime, uptime);
    }

    public boolean startAddingCpuLocked() {
        this.mExternalSync.cancelCpuSyncDueToWakelockChange();
        return this.mOnBatteryInternal;
    }

    public void finishAddingCpuLocked(int totalUTime, int totalSTime, int statUserTime, int statSystemTime, int statIOWaitTime, int statIrqTime, int statSoftIrqTime, int statIdleTime) {
        this.mCurStepCpuUserTime += (long) totalUTime;
        this.mCurStepCpuSystemTime += (long) totalSTime;
        this.mCurStepStatUserTime += (long) statUserTime;
        this.mCurStepStatSystemTime += (long) statSystemTime;
        this.mCurStepStatIOWaitTime += (long) statIOWaitTime;
        this.mCurStepStatIrqTime += (long) statIrqTime;
        this.mCurStepStatSoftIrqTime += (long) statSoftIrqTime;
        this.mCurStepStatIdleTime += (long) statIdleTime;
    }

    public void noteProcessDiedLocked(int uid, int pid) {
        Uid u = (Uid) this.mUidStats.get(mapUid(uid));
        if (u != null) {
            u.mPids.remove(pid);
        }
    }

    public long getProcessWakeTime(int uid, int pid, long realtime) {
        Uid u = (Uid) this.mUidStats.get(mapUid(uid));
        long j = 0;
        if (u != null) {
            Pid p = (Pid) u.mPids.get(pid);
            if (p != null) {
                long j2 = p.mWakeSumMs;
                if (p.mWakeNesting > 0) {
                    j = realtime - p.mWakeStartMs;
                }
                return j2 + j;
            }
        }
        return 0;
    }

    public void reportExcessiveCpuLocked(int uid, String proc, long overTime, long usedTime) {
        Uid u = (Uid) this.mUidStats.get(mapUid(uid));
        if (u != null) {
            u.reportExcessiveCpuLocked(proc, overTime, usedTime);
        }
    }

    public void noteStartSensorLocked(int uid, int sensor) {
        uid = mapUid(uid);
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        if (this.mSensorNesting == 0) {
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states |= DELTA_EVENT_FLAG;
            addHistoryRecordLocked(elapsedRealtime, uptime);
        }
        this.mSensorNesting++;
        getUidStatsLocked(uid).noteStartSensor(sensor, elapsedRealtime);
    }

    public void noteStopSensorLocked(int uid, int sensor) {
        uid = mapUid(uid);
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        this.mSensorNesting--;
        if (this.mSensorNesting == 0) {
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states &= -8388609;
            addHistoryRecordLocked(elapsedRealtime, uptime);
        }
        getUidStatsLocked(uid).noteStopSensor(sensor, elapsedRealtime);
    }

    public void noteGpsChangedLocked(WorkSource oldWs, WorkSource newWs) {
        int i;
        int i2 = 0;
        for (i = 0; i < newWs.size(); i++) {
            noteStartGpsLocked(newWs.get(i), null);
        }
        for (i = 0; i < oldWs.size(); i++) {
            noteStopGpsLocked(oldWs.get(i), null);
        }
        List<WorkChain>[] wcs = WorkSource.diffChains(oldWs, newWs);
        if (wcs != null) {
            List<WorkChain> newChains;
            if (wcs[0] != null) {
                newChains = wcs[0];
                for (int i3 = 0; i3 < newChains.size(); i3++) {
                    noteStartGpsLocked(-1, (WorkChain) newChains.get(i3));
                }
            }
            if (wcs[1] != null) {
                newChains = wcs[1];
                while (i2 < newChains.size()) {
                    noteStopGpsLocked(-1, (WorkChain) newChains.get(i2));
                    i2++;
                }
            }
        }
    }

    private void noteStartGpsLocked(int uid, WorkChain workChain) {
        uid = getAttributionUid(uid, workChain);
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        if (this.mGpsNesting == 0) {
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states |= 536870912;
            addHistoryRecordLocked(elapsedRealtime, uptime);
        }
        this.mGpsNesting++;
        if (workChain == null) {
            StatsLog.write_non_chained(6, uid, null, 1);
        } else {
            StatsLog.write(6, workChain.getUids(), workChain.getTags(), 1);
        }
        getUidStatsLocked(uid).noteStartGps(elapsedRealtime);
    }

    private void noteStopGpsLocked(int uid, WorkChain workChain) {
        uid = getAttributionUid(uid, workChain);
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        this.mGpsNesting--;
        if (this.mGpsNesting == 0) {
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states &= -536870913;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            stopAllGpsSignalQualityTimersLocked(-1);
            this.mGpsSignalQualityBin = -1;
        }
        if (workChain == null) {
            StatsLog.write_non_chained(6, uid, null, 0);
        } else {
            StatsLog.write(6, workChain.getUids(), workChain.getTags(), 0);
        }
        getUidStatsLocked(uid).noteStopGps(elapsedRealtime);
    }

    public void noteGpsSignalQualityLocked(int signalLevel) {
        if (this.mGpsNesting != 0) {
            if (signalLevel < 0 || signalLevel >= 2) {
                stopAllGpsSignalQualityTimersLocked(-1);
                return;
            }
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            long uptime = this.mClocks.uptimeMillis();
            if (this.mGpsSignalQualityBin != signalLevel) {
                if (this.mGpsSignalQualityBin >= 0) {
                    this.mGpsSignalQualityTimer[this.mGpsSignalQualityBin].stopRunningLocked(elapsedRealtime);
                }
                if (!this.mGpsSignalQualityTimer[signalLevel].isRunningLocked()) {
                    this.mGpsSignalQualityTimer[signalLevel].startRunningLocked(elapsedRealtime);
                }
                this.mHistoryCur.states2 = (this.mHistoryCur.states2 & -129) | (signalLevel << 7);
                addHistoryRecordLocked(elapsedRealtime, uptime);
                this.mGpsSignalQualityBin = signalLevel;
            }
        }
    }

    @GuardedBy("this")
    public void noteScreenStateLocked(int state) {
        int state2 = this.mPretendScreenOff ? 1 : state;
        if (state2 > 4) {
            if (state2 != 5) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown screen state (not mapped): ");
                stringBuilder.append(state2);
                Slog.wtf(str, stringBuilder.toString());
            } else {
                state2 = 2;
            }
        }
        int state3 = state2;
        if (this.mScreenState != state3) {
            HistoryItem historyItem;
            recordDailyStatsIfNeededLocked(true);
            int oldState = this.mScreenState;
            this.mScreenState = state3;
            if (state3 != 0) {
                state2 = state3 - 1;
                if ((state2 & 3) == state2) {
                    this.mModStepMode |= (this.mCurStepMode & 3) ^ state2;
                    this.mCurStepMode = (this.mCurStepMode & -4) | state2;
                } else {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unexpected screen state: ");
                    stringBuilder2.append(state3);
                    Slog.wtf(str2, stringBuilder2.toString());
                }
            }
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            long uptime = this.mClocks.uptimeMillis();
            boolean updateHistory = false;
            if (isScreenDoze(state3)) {
                historyItem = this.mHistoryCur;
                historyItem.states |= 262144;
                this.mScreenDozeTimer.startRunningLocked(elapsedRealtime);
                updateHistory = true;
            } else if (isScreenDoze(oldState)) {
                historyItem = this.mHistoryCur;
                historyItem.states &= -262145;
                this.mScreenDozeTimer.stopRunningLocked(elapsedRealtime);
                updateHistory = true;
            }
            if (isScreenOn(state3)) {
                historyItem = this.mHistoryCur;
                historyItem.states |= DELTA_STATE_FLAG;
                this.mScreenOnTimer.startRunningLocked(elapsedRealtime);
                if (this.mScreenBrightnessBin >= 0) {
                    this.mScreenBrightnessTimer[this.mScreenBrightnessBin].startRunningLocked(elapsedRealtime);
                }
                updateHistory = true;
            } else if (isScreenOn(oldState)) {
                historyItem = this.mHistoryCur;
                historyItem.states &= -1048577;
                this.mScreenOnTimer.stopRunningLocked(elapsedRealtime);
                if (this.mScreenBrightnessBin >= 0) {
                    this.mScreenBrightnessTimer[this.mScreenBrightnessBin].stopRunningLocked(elapsedRealtime);
                }
                updateHistory = true;
            }
            if (updateHistory) {
                addHistoryRecordLocked(elapsedRealtime, uptime);
            }
            this.mExternalSync.scheduleCpuSyncDueToScreenStateChange(this.mOnBatteryTimeBase.isRunning(), this.mOnBatteryScreenOffTimeBase.isRunning());
            if (isScreenOn(state3)) {
                updateTimeBasesLocked(this.mOnBatteryTimeBase.isRunning(), state3, this.mClocks.uptimeMillis() * 1000, elapsedRealtime * 1000);
                noteStartWakeLocked(-1, -1, null, "screen", null, 0, false, elapsedRealtime, uptime);
            } else {
                long uptime2 = uptime;
                long elapsedRealtime2 = elapsedRealtime;
                if (isScreenOn(oldState)) {
                    noteStopWakeLocked(-1, -1, null, "screen", "screen", 0, elapsedRealtime2, uptime2);
                    updateTimeBasesLocked(this.mOnBatteryTimeBase.isRunning(), state3, this.mClocks.uptimeMillis() * 1000, elapsedRealtime2 * 1000);
                }
            }
            if (this.mOnBatteryInternal) {
                updateDischargeScreenLevelsLocked(oldState, state3);
            }
        }
    }

    public void noteScreenBrightnessLocked(int brightness) {
        int bin = brightness / 51;
        if (bin < 0) {
            bin = 0;
        } else if (bin >= 5) {
            bin = 4;
        }
        if (this.mScreenBrightnessBin != bin) {
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            long uptime = this.mClocks.uptimeMillis();
            this.mHistoryCur.states = (this.mHistoryCur.states & -8) | (bin << 0);
            addHistoryRecordLocked(elapsedRealtime, uptime);
            if (this.mScreenState == 2) {
                if (this.mScreenBrightnessBin >= 0) {
                    this.mScreenBrightnessTimer[this.mScreenBrightnessBin].stopRunningLocked(elapsedRealtime);
                }
                this.mScreenBrightnessTimer[bin].startRunningLocked(elapsedRealtime);
            }
            this.mScreenBrightnessBin = bin;
        }
    }

    public void noteUserActivityLocked(int uid, int event) {
        if (this.mOnBatteryInternal) {
            getUidStatsLocked(mapUid(uid)).noteUserActivityLocked(event);
        }
    }

    public void noteWakeUpLocked(String reason, int reasonUid) {
        addHistoryEventLocked(this.mClocks.elapsedRealtime(), this.mClocks.uptimeMillis(), 18, reason, reasonUid);
    }

    public void noteInteractiveLocked(boolean interactive) {
        if (this.mInteractive != interactive) {
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            this.mInteractive = interactive;
            if (interactive) {
                this.mInteractiveTimer.startRunningLocked(elapsedRealtime);
            } else {
                this.mInteractiveTimer.stopRunningLocked(elapsedRealtime);
            }
        }
    }

    public void noteConnectivityChangedLocked(int type, String extra) {
        addHistoryEventLocked(this.mClocks.elapsedRealtime(), this.mClocks.uptimeMillis(), 9, extra, type);
        this.mNumConnectivityChange++;
    }

    private void noteMobileRadioApWakeupLocked(long elapsedRealtimeMillis, long uptimeMillis, int uid) {
        uid = mapUid(uid);
        addHistoryEventLocked(elapsedRealtimeMillis, uptimeMillis, 19, "", uid);
        getUidStatsLocked(uid).noteMobileRadioApWakeupLocked();
    }

    public boolean noteMobileRadioPowerStateLocked(int powerState, long timestampNs, int uid) {
        int i = powerState;
        int i2 = uid;
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        if (this.mMobileRadioPowerState != i) {
            long realElapsedRealtimeMs;
            boolean z = i == 2 || i == 3;
            boolean active = z;
            long j;
            HistoryItem historyItem;
            if (active) {
                if (i2 > 0) {
                    noteMobileRadioApWakeupLocked(elapsedRealtime, uptime, i2);
                }
                j = timestampNs / 1000000;
                realElapsedRealtimeMs = j;
                this.mMobileRadioActiveStartTime = j;
                historyItem = this.mHistoryCur;
                historyItem.states |= 33554432;
            } else {
                j = timestampNs / 1000000;
                realElapsedRealtimeMs = this.mMobileRadioActiveStartTime;
                long lastUpdateTimeMs;
                if (j < realElapsedRealtimeMs) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Data connection inactive timestamp ");
                    stringBuilder.append(j);
                    stringBuilder.append(" is before start time ");
                    stringBuilder.append(realElapsedRealtimeMs);
                    Slog.wtf(str, stringBuilder.toString());
                    j = elapsedRealtime;
                    lastUpdateTimeMs = realElapsedRealtimeMs;
                } else if (j < elapsedRealtime) {
                    lastUpdateTimeMs = realElapsedRealtimeMs;
                    this.mMobileRadioActiveAdjustedTime.addCountLocked(elapsedRealtime - j);
                } else {
                    lastUpdateTimeMs = realElapsedRealtimeMs;
                }
                realElapsedRealtimeMs = j;
                historyItem = this.mHistoryCur;
                historyItem.states &= -33554433;
            }
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mMobileRadioPowerState = i;
            StatsLog.write_non_chained(12, i2, null, i);
            if (active) {
                this.mMobileRadioActiveTimer.startRunningLocked(elapsedRealtime);
                this.mMobileRadioActivePerAppTimer.startRunningLocked(elapsedRealtime);
                LogPower.push(191, Integer.toString(powerState), Long.toString(timestampNs));
            } else {
                this.mMobileRadioActiveTimer.stopRunningLocked(realElapsedRealtimeMs);
                this.mMobileRadioActivePerAppTimer.stopRunningLocked(realElapsedRealtimeMs);
                return true;
            }
        }
        return false;
    }

    public void notePowerSaveModeLocked(boolean enabled) {
        if (this.mPowerSaveModeEnabled != enabled) {
            int i = 0;
            int stepState = enabled ? 4 : 0;
            this.mModStepMode = ((4 & this.mCurStepMode) ^ stepState) | this.mModStepMode;
            this.mCurStepMode = (this.mCurStepMode & -5) | stepState;
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            long uptime = this.mClocks.uptimeMillis();
            this.mPowerSaveModeEnabled = enabled;
            HistoryItem historyItem;
            if (enabled) {
                historyItem = this.mHistoryCur;
                historyItem.states2 |= Integer.MIN_VALUE;
                this.mPowerSaveModeEnabledTimer.startRunningLocked(elapsedRealtime);
            } else {
                historyItem = this.mHistoryCur;
                historyItem.states2 &= Integer.MAX_VALUE;
                this.mPowerSaveModeEnabledTimer.stopRunningLocked(elapsedRealtime);
            }
            addHistoryRecordLocked(elapsedRealtime, uptime);
            if (enabled) {
                i = 1;
            }
            StatsLog.write(20, i);
        }
    }

    public void noteDeviceIdleModeLocked(int mode, String activeReason, int activeUid) {
        boolean nowLightIdling;
        boolean nowIdling;
        int i;
        int i2 = mode;
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        int i3 = 0;
        boolean nowIdling2 = i2 == 2;
        if (this.mDeviceIdling && !nowIdling2 && activeReason == null) {
            nowIdling2 = true;
        }
        boolean nowIdling3 = nowIdling2;
        nowIdling2 = i2 == 1;
        if (this.mDeviceLightIdling && !nowIdling2 && !nowIdling3 && activeReason == null) {
            nowIdling2 = true;
        }
        boolean nowLightIdling2 = nowIdling2;
        if (activeReason == null) {
            nowLightIdling = nowLightIdling2;
            nowIdling = nowIdling3;
            i = 1;
        } else if (this.mDeviceIdling || this.mDeviceLightIdling) {
            nowLightIdling = nowLightIdling2;
            nowIdling = nowIdling3;
            i = 1;
            addHistoryEventLocked(elapsedRealtime, uptime, 10, activeReason, activeUid);
        } else {
            nowLightIdling = nowLightIdling2;
            nowIdling = nowIdling3;
            i = 1;
        }
        boolean nowIdling4 = nowIdling;
        if (!(this.mDeviceIdling == nowIdling4 && this.mDeviceLightIdling == nowLightIdling)) {
            int statsmode;
            if (nowIdling4) {
                statsmode = 2;
            } else if (nowLightIdling) {
                statsmode = 1;
            } else {
                statsmode = 0;
            }
            StatsLog.write(22, statsmode);
        }
        if (this.mDeviceIdling != nowIdling4) {
            this.mDeviceIdling = nowIdling4;
            if (nowIdling4) {
                i3 = 8;
            }
            int stepState = i3;
            this.mModStepMode = ((8 & this.mCurStepMode) ^ stepState) | this.mModStepMode;
            this.mCurStepMode = (this.mCurStepMode & -9) | stepState;
            if (nowIdling4) {
                this.mDeviceIdlingTimer.startRunningLocked(elapsedRealtime);
            } else {
                this.mDeviceIdlingTimer.stopRunningLocked(elapsedRealtime);
            }
        }
        if (this.mDeviceLightIdling != nowLightIdling) {
            this.mDeviceLightIdling = nowLightIdling;
            if (nowLightIdling) {
                this.mDeviceLightIdlingTimer.startRunningLocked(elapsedRealtime);
            } else {
                this.mDeviceLightIdlingTimer.stopRunningLocked(elapsedRealtime);
            }
        }
        if (this.mDeviceIdleMode != i2) {
            this.mHistoryCur.states2 = (this.mHistoryCur.states2 & -100663297) | (i2 << 25);
            addHistoryRecordLocked(elapsedRealtime, uptime);
            long lastDuration = elapsedRealtime - this.mLastIdleTimeStart;
            this.mLastIdleTimeStart = elapsedRealtime;
            if (this.mDeviceIdleMode == i) {
                if (lastDuration > this.mLongestLightIdleTime) {
                    this.mLongestLightIdleTime = lastDuration;
                }
                this.mDeviceIdleModeLightTimer.stopRunningLocked(elapsedRealtime);
            } else if (this.mDeviceIdleMode == 2) {
                if (lastDuration > this.mLongestFullIdleTime) {
                    this.mLongestFullIdleTime = lastDuration;
                }
                this.mDeviceIdleModeFullTimer.stopRunningLocked(elapsedRealtime);
            }
            if (i2 == i) {
                this.mDeviceIdleModeLightTimer.startRunningLocked(elapsedRealtime);
            } else if (i2 == 2) {
                this.mDeviceIdleModeFullTimer.startRunningLocked(elapsedRealtime);
            }
            this.mDeviceIdleMode = i2;
            StatsLog.write(21, i2);
        }
    }

    public void notePackageInstalledLocked(String pkgName, long versionCode) {
        long j = versionCode;
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        addHistoryEventLocked(elapsedRealtime, this.mClocks.uptimeMillis(), 11, pkgName, (int) j);
        PackageChange pc = new PackageChange();
        pc.mPackageName = pkgName;
        pc.mUpdate = true;
        pc.mVersionCode = j;
        addPackageChange(pc);
    }

    public void notePackageUninstalledLocked(String pkgName) {
        addHistoryEventLocked(this.mClocks.elapsedRealtime(), this.mClocks.uptimeMillis(), 12, pkgName, 0);
        PackageChange pc = new PackageChange();
        pc.mPackageName = pkgName;
        pc.mUpdate = true;
        addPackageChange(pc);
    }

    private void addPackageChange(PackageChange pc) {
        if (this.mDailyPackageChanges == null) {
            this.mDailyPackageChanges = new ArrayList();
        }
        this.mDailyPackageChanges.add(pc);
    }

    void stopAllGpsSignalQualityTimersLocked(int except) {
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        for (int i = 0; i < 2; i++) {
            if (i != except) {
                while (this.mGpsSignalQualityTimer[i].isRunningLocked()) {
                    this.mGpsSignalQualityTimer[i].stopRunningLocked(elapsedRealtime);
                }
            }
        }
    }

    public void notePhoneOnLocked() {
        if (!this.mPhoneOn) {
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            long uptime = this.mClocks.uptimeMillis();
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states2 |= DELTA_EVENT_FLAG;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mPhoneOn = true;
            this.mPhoneOnTimer.startRunningLocked(elapsedRealtime);
        }
    }

    public void notePhoneOffLocked() {
        if (this.mPhoneOn) {
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            long uptime = this.mClocks.uptimeMillis();
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states2 &= -8388609;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mPhoneOn = false;
            this.mPhoneOnTimer.stopRunningLocked(elapsedRealtime);
        }
    }

    private void registerUsbStateReceiver(Context context) {
        IntentFilter usbStateFilter = new IntentFilter();
        usbStateFilter.addAction("android.hardware.usb.action.USB_STATE");
        context.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                boolean state = intent.getBooleanExtra("connected", false);
                synchronized (BatteryStatsImpl.this) {
                    BatteryStatsImpl.this.noteUsbConnectionStateLocked(state);
                }
            }
        }, usbStateFilter);
        synchronized (this) {
            if (this.mUsbDataState == 0) {
                Intent usbState = context.registerReceiver(null, usbStateFilter);
                boolean initState = false;
                if (usbState != null && usbState.getBooleanExtra("connected", false)) {
                    initState = true;
                }
                noteUsbConnectionStateLocked(initState);
            }
        }
    }

    private void noteUsbConnectionStateLocked(boolean connected) {
        int newState = connected ? 2 : 1;
        if (this.mUsbDataState != newState) {
            this.mUsbDataState = newState;
            HistoryItem historyItem;
            if (connected) {
                historyItem = this.mHistoryCur;
                historyItem.states2 |= 262144;
            } else {
                historyItem = this.mHistoryCur;
                historyItem.states2 &= -262145;
            }
            addHistoryRecordLocked(this.mClocks.elapsedRealtime(), this.mClocks.uptimeMillis());
        }
    }

    void stopAllPhoneSignalStrengthTimersLocked(int except) {
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        for (int i = 0; i < 5; i++) {
            if (i != except) {
                while (this.mPhoneSignalStrengthsTimer[i].isRunningLocked()) {
                    this.mPhoneSignalStrengthsTimer[i].stopRunningLocked(elapsedRealtime);
                }
            }
        }
    }

    private int fixPhoneServiceState(int state, int signalBin) {
        if (this.mPhoneSimStateRaw == 1 && state == 1 && signalBin > 0) {
            return 0;
        }
        return state;
    }

    private void updateAllPhoneStateLocked(int state, int simState, int strengthBin) {
        HistoryItem historyItem;
        boolean scanning = false;
        boolean newHistory = false;
        this.mPhoneServiceStateRaw = state;
        this.mPhoneSimStateRaw = simState;
        this.mPhoneSignalStrengthBinRaw = strengthBin;
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        if (simState == 1 && state == 1 && strengthBin > 0) {
            state = 0;
        }
        if (state == 3) {
            strengthBin = -1;
        } else if (state != 0 && state == 1) {
            scanning = true;
            strengthBin = 0;
            if (!this.mPhoneSignalScanningTimer.isRunningLocked()) {
                historyItem = this.mHistoryCur;
                historyItem.states |= DELTA_STATE2_FLAG;
                newHistory = true;
                this.mPhoneSignalScanningTimer.startRunningLocked(elapsedRealtime);
            }
        }
        if (!scanning && this.mPhoneSignalScanningTimer.isRunningLocked()) {
            historyItem = this.mHistoryCur;
            historyItem.states &= -2097153;
            newHistory = true;
            this.mPhoneSignalScanningTimer.stopRunningLocked(elapsedRealtime);
        }
        if (this.mPhoneServiceState != state) {
            this.mHistoryCur.states = (this.mHistoryCur.states & -449) | (state << 6);
            newHistory = true;
            this.mPhoneServiceState = state;
        }
        if (this.mPhoneSignalStrengthBin != strengthBin) {
            if (this.mPhoneSignalStrengthBin >= 0) {
                this.mPhoneSignalStrengthsTimer[this.mPhoneSignalStrengthBin].stopRunningLocked(elapsedRealtime);
            }
            if (strengthBin >= 0) {
                if (!this.mPhoneSignalStrengthsTimer[strengthBin].isRunningLocked()) {
                    this.mPhoneSignalStrengthsTimer[strengthBin].startRunningLocked(elapsedRealtime);
                }
                this.mHistoryCur.states = (this.mHistoryCur.states & -57) | (strengthBin << 3);
                newHistory = true;
                StatsLog.write(40, strengthBin);
            } else {
                stopAllPhoneSignalStrengthTimersLocked(-1);
            }
            this.mPhoneSignalStrengthBin = strengthBin;
        }
        if (newHistory) {
            addHistoryRecordLocked(elapsedRealtime, uptime);
        }
    }

    public void notePhoneStateLocked(int state, int simState) {
        updateAllPhoneStateLocked(state, simState, this.mPhoneSignalStrengthBinRaw);
    }

    public void notePhoneSignalStrengthLocked(SignalStrength signalStrength) {
        int bin = signalStrength.getLevel();
        if (bin >= this.mPhoneSignalStrengthsTimer.length) {
            bin = this.mPhoneSignalStrengthsTimer.length - 1;
        }
        updateAllPhoneStateLocked(this.mPhoneServiceStateRaw, this.mPhoneSimStateRaw, bin);
    }

    public void notePhoneDataConnectionStateLocked(int dataType, boolean hasData) {
        int bin = 0;
        if (hasData) {
            if (dataType <= 0 || dataType > 19) {
                bin = 20;
            } else {
                bin = dataType;
            }
        }
        if (this.mPhoneDataConnectionType != bin) {
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            long uptime = this.mClocks.uptimeMillis();
            this.mHistoryCur.states = (this.mHistoryCur.states & -15873) | (bin << 9);
            addHistoryRecordLocked(elapsedRealtime, uptime);
            if (this.mPhoneDataConnectionType >= 0) {
                this.mPhoneDataConnectionsTimer[this.mPhoneDataConnectionType].stopRunningLocked(elapsedRealtime);
            }
            this.mPhoneDataConnectionType = bin;
            this.mPhoneDataConnectionsTimer[bin].startRunningLocked(elapsedRealtime);
        }
    }

    public void noteWifiOnLocked() {
        if (!this.mWifiOn) {
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            long uptime = this.mClocks.uptimeMillis();
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states2 |= 268435456;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mWifiOn = true;
            this.mWifiOnTimer.startRunningLocked(elapsedRealtime);
            scheduleSyncExternalStatsLocked("wifi-off", 2);
        }
    }

    public void noteWifiOffLocked() {
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        if (this.mWifiOn) {
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states2 &= -268435457;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mWifiOn = false;
            this.mWifiOnTimer.stopRunningLocked(elapsedRealtime);
            scheduleSyncExternalStatsLocked("wifi-on", 2);
        }
    }

    public void noteAudioOnLocked(int uid) {
        uid = mapUid(uid);
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        if (this.mAudioOnNesting == 0) {
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states |= DELTA_WAKELOCK_FLAG;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mAudioOnTimer.startRunningLocked(elapsedRealtime);
        }
        this.mAudioOnNesting++;
        getUidStatsLocked(uid).noteAudioTurnedOnLocked(elapsedRealtime);
    }

    public void noteAudioOffLocked(int uid) {
        if (this.mAudioOnNesting != 0) {
            uid = mapUid(uid);
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            long uptime = this.mClocks.uptimeMillis();
            int i = this.mAudioOnNesting - 1;
            this.mAudioOnNesting = i;
            if (i == 0) {
                HistoryItem historyItem = this.mHistoryCur;
                historyItem.states &= -4194305;
                addHistoryRecordLocked(elapsedRealtime, uptime);
                this.mAudioOnTimer.stopRunningLocked(elapsedRealtime);
            }
            getUidStatsLocked(uid).noteAudioTurnedOffLocked(elapsedRealtime);
        }
    }

    public void noteVideoOnLocked(int uid) {
        uid = mapUid(uid);
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        if (this.mVideoOnNesting == 0) {
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states2 |= 1073741824;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mVideoOnTimer.startRunningLocked(elapsedRealtime);
        }
        this.mVideoOnNesting++;
        getUidStatsLocked(uid).noteVideoTurnedOnLocked(elapsedRealtime);
    }

    public void noteVideoOffLocked(int uid) {
        if (this.mVideoOnNesting != 0) {
            uid = mapUid(uid);
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            long uptime = this.mClocks.uptimeMillis();
            int i = this.mVideoOnNesting - 1;
            this.mVideoOnNesting = i;
            if (i == 0) {
                HistoryItem historyItem = this.mHistoryCur;
                historyItem.states2 &= -1073741825;
                addHistoryRecordLocked(elapsedRealtime, uptime);
                this.mVideoOnTimer.stopRunningLocked(elapsedRealtime);
            }
            getUidStatsLocked(uid).noteVideoTurnedOffLocked(elapsedRealtime);
        }
    }

    public void noteResetAudioLocked() {
        if (this.mAudioOnNesting > 0) {
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            long uptime = this.mClocks.uptimeMillis();
            int i = 0;
            this.mAudioOnNesting = 0;
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states &= -4194305;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mAudioOnTimer.stopAllRunningLocked(elapsedRealtime);
            while (i < this.mUidStats.size()) {
                ((Uid) this.mUidStats.valueAt(i)).noteResetAudioLocked(elapsedRealtime);
                i++;
            }
        }
    }

    public void noteResetVideoLocked() {
        if (this.mVideoOnNesting > 0) {
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            long uptime = this.mClocks.uptimeMillis();
            int i = 0;
            this.mAudioOnNesting = 0;
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states2 &= -1073741825;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mVideoOnTimer.stopAllRunningLocked(elapsedRealtime);
            while (i < this.mUidStats.size()) {
                ((Uid) this.mUidStats.valueAt(i)).noteResetVideoLocked(elapsedRealtime);
                i++;
            }
        }
    }

    public void noteActivityResumedLocked(int uid) {
        getUidStatsLocked(mapUid(uid)).noteActivityResumedLocked(this.mClocks.elapsedRealtime());
    }

    public void noteActivityPausedLocked(int uid) {
        getUidStatsLocked(mapUid(uid)).noteActivityPausedLocked(this.mClocks.elapsedRealtime());
    }

    public void noteVibratorOnLocked(int uid, long durationMillis) {
        getUidStatsLocked(mapUid(uid)).noteVibratorOnLocked(durationMillis);
    }

    public void noteVibratorOffLocked(int uid) {
        getUidStatsLocked(mapUid(uid)).noteVibratorOffLocked();
    }

    public void noteFlashlightOnLocked(int uid) {
        uid = mapUid(uid);
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        int i = this.mFlashlightOnNesting;
        this.mFlashlightOnNesting = i + 1;
        if (i == 0) {
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states2 |= 134217728;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mFlashlightOnTimer.startRunningLocked(elapsedRealtime);
        }
        getUidStatsLocked(uid).noteFlashlightTurnedOnLocked(elapsedRealtime);
    }

    public void noteFlashlightOffLocked(int uid) {
        if (this.mFlashlightOnNesting != 0) {
            uid = mapUid(uid);
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            long uptime = this.mClocks.uptimeMillis();
            int i = this.mFlashlightOnNesting - 1;
            this.mFlashlightOnNesting = i;
            if (i == 0) {
                HistoryItem historyItem = this.mHistoryCur;
                historyItem.states2 &= -134217729;
                addHistoryRecordLocked(elapsedRealtime, uptime);
                this.mFlashlightOnTimer.stopRunningLocked(elapsedRealtime);
            }
            getUidStatsLocked(uid).noteFlashlightTurnedOffLocked(elapsedRealtime);
        }
    }

    public void noteCameraOnLocked(int uid) {
        uid = mapUid(uid);
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        int i = this.mCameraOnNesting;
        this.mCameraOnNesting = i + 1;
        if (i == 0) {
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states2 |= DELTA_STATE2_FLAG;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mCameraOnTimer.startRunningLocked(elapsedRealtime);
        }
        getUidStatsLocked(uid).noteCameraTurnedOnLocked(elapsedRealtime);
    }

    public void noteCameraOffLocked(int uid) {
        if (this.mCameraOnNesting != 0) {
            uid = mapUid(uid);
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            long uptime = this.mClocks.uptimeMillis();
            int i = this.mCameraOnNesting - 1;
            this.mCameraOnNesting = i;
            if (i == 0) {
                HistoryItem historyItem = this.mHistoryCur;
                historyItem.states2 &= -2097153;
                addHistoryRecordLocked(elapsedRealtime, uptime);
                this.mCameraOnTimer.stopRunningLocked(elapsedRealtime);
            }
            getUidStatsLocked(uid).noteCameraTurnedOffLocked(elapsedRealtime);
        }
    }

    public void noteResetCameraLocked() {
        if (this.mCameraOnNesting > 0) {
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            long uptime = this.mClocks.uptimeMillis();
            int i = 0;
            this.mCameraOnNesting = 0;
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states2 &= -2097153;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mCameraOnTimer.stopAllRunningLocked(elapsedRealtime);
            while (i < this.mUidStats.size()) {
                ((Uid) this.mUidStats.valueAt(i)).noteResetCameraLocked(elapsedRealtime);
                i++;
            }
        }
    }

    public void noteResetFlashlightLocked() {
        if (this.mFlashlightOnNesting > 0) {
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            long uptime = this.mClocks.uptimeMillis();
            int i = 0;
            this.mFlashlightOnNesting = 0;
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states2 &= -134217729;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mFlashlightOnTimer.stopAllRunningLocked(elapsedRealtime);
            while (i < this.mUidStats.size()) {
                ((Uid) this.mUidStats.valueAt(i)).noteResetFlashlightLocked(elapsedRealtime);
                i++;
            }
        }
    }

    private void noteBluetoothScanStartedLocked(WorkChain workChain, int uid, boolean isUnoptimized) {
        uid = getAttributionUid(uid, workChain);
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        if (this.mBluetoothScanNesting == 0) {
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states2 |= DELTA_STATE_FLAG;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mBluetoothScanTimer.startRunningLocked(elapsedRealtime);
        }
        this.mBluetoothScanNesting++;
        getUidStatsLocked(uid).noteBluetoothScanStartedLocked(elapsedRealtime, isUnoptimized);
    }

    public void noteBluetoothScanStartedFromSourceLocked(WorkSource ws, boolean isUnoptimized) {
        int N = ws.size();
        int i = 0;
        for (int i2 = 0; i2 < N; i2++) {
            noteBluetoothScanStartedLocked(null, ws.get(i2), isUnoptimized);
        }
        List<WorkChain> workChains = ws.getWorkChains();
        if (workChains != null) {
            while (i < workChains.size()) {
                noteBluetoothScanStartedLocked((WorkChain) workChains.get(i), -1, isUnoptimized);
                i++;
            }
        }
    }

    private void noteBluetoothScanStoppedLocked(WorkChain workChain, int uid, boolean isUnoptimized) {
        uid = getAttributionUid(uid, workChain);
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        this.mBluetoothScanNesting--;
        if (this.mBluetoothScanNesting == 0) {
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states2 &= -1048577;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mBluetoothScanTimer.stopRunningLocked(elapsedRealtime);
        }
        getUidStatsLocked(uid).noteBluetoothScanStoppedLocked(elapsedRealtime, isUnoptimized);
    }

    private int getAttributionUid(int uid, WorkChain workChain) {
        if (workChain != null) {
            return mapUid(workChain.getAttributionUid());
        }
        return mapUid(uid);
    }

    public void noteBluetoothScanStoppedFromSourceLocked(WorkSource ws, boolean isUnoptimized) {
        int N = ws.size();
        int i = 0;
        for (int i2 = 0; i2 < N; i2++) {
            noteBluetoothScanStoppedLocked(null, ws.get(i2), isUnoptimized);
        }
        List<WorkChain> workChains = ws.getWorkChains();
        if (workChains != null) {
            while (i < workChains.size()) {
                noteBluetoothScanStoppedLocked((WorkChain) workChains.get(i), -1, isUnoptimized);
                i++;
            }
        }
    }

    public void noteResetBluetoothScanLocked() {
        if (this.mBluetoothScanNesting > 0) {
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            long uptime = this.mClocks.uptimeMillis();
            int i = 0;
            this.mBluetoothScanNesting = 0;
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states2 &= -1048577;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mBluetoothScanTimer.stopAllRunningLocked(elapsedRealtime);
            while (i < this.mUidStats.size()) {
                ((Uid) this.mUidStats.valueAt(i)).noteResetBluetoothScanLocked(elapsedRealtime);
                i++;
            }
        }
    }

    public void noteBluetoothScanResultsFromSourceLocked(WorkSource ws, int numNewResults) {
        int N = ws.size();
        int i = 0;
        for (int i2 = 0; i2 < N; i2++) {
            getUidStatsLocked(mapUid(ws.get(i2))).noteBluetoothScanResultsLocked(numNewResults);
            StatsLog.write_non_chained(4, ws.get(i2), ws.getName(i2), numNewResults);
        }
        List<WorkChain> workChains = ws.getWorkChains();
        if (workChains != null) {
            while (i < workChains.size()) {
                WorkChain wc = (WorkChain) workChains.get(i);
                getUidStatsLocked(mapUid(wc.getAttributionUid())).noteBluetoothScanResultsLocked(numNewResults);
                StatsLog.write(4, wc.getUids(), wc.getTags(), numNewResults);
                i++;
            }
        }
    }

    private void noteWifiRadioApWakeupLocked(long elapsedRealtimeMillis, long uptimeMillis, int uid) {
        uid = mapUid(uid);
        addHistoryEventLocked(elapsedRealtimeMillis, uptimeMillis, 19, "", uid);
        getUidStatsLocked(uid).noteWifiRadioApWakeupLocked();
    }

    public void noteWifiRadioPowerState(int powerState, long timestampNs, int uid) {
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        if (this.mWifiRadioPowerState != powerState) {
            boolean active = powerState == 2 || powerState == 3;
            HistoryItem historyItem;
            if (active) {
                if (uid > 0) {
                    noteWifiRadioApWakeupLocked(elapsedRealtime, uptime, uid);
                }
                historyItem = this.mHistoryCur;
                historyItem.states |= 67108864;
                this.mWifiActiveTimer.startRunningLocked(elapsedRealtime);
            } else {
                historyItem = this.mHistoryCur;
                historyItem.states &= -67108865;
                this.mWifiActiveTimer.stopRunningLocked(timestampNs / 1000000);
            }
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mWifiRadioPowerState = powerState;
            StatsLog.write_non_chained(13, uid, null, powerState);
        }
    }

    public void noteWifiRunningLocked(WorkSource ws) {
        if (this.mGlobalWifiRunning) {
            Log.w(TAG, "noteWifiRunningLocked -- called while WIFI running");
            return;
        }
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        HistoryItem historyItem = this.mHistoryCur;
        historyItem.states2 |= 536870912;
        addHistoryRecordLocked(elapsedRealtime, uptime);
        this.mGlobalWifiRunning = true;
        this.mGlobalWifiRunningTimer.startRunningLocked(elapsedRealtime);
        int N = ws.size();
        int i = 0;
        for (int i2 = 0; i2 < N; i2++) {
            getUidStatsLocked(mapUid(ws.get(i2))).noteWifiRunningLocked(elapsedRealtime);
        }
        List<WorkChain> workChains = ws.getWorkChains();
        if (workChains != null) {
            while (i < workChains.size()) {
                getUidStatsLocked(mapUid(((WorkChain) workChains.get(i)).getAttributionUid())).noteWifiRunningLocked(elapsedRealtime);
                i++;
            }
        }
        scheduleSyncExternalStatsLocked("wifi-running", 2);
    }

    public void noteWifiRunningChangedLocked(WorkSource oldWs, WorkSource newWs) {
        if (this.mGlobalWifiRunning) {
            int i;
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            int N = oldWs.size();
            int i2 = 0;
            for (int i3 = 0; i3 < N; i3++) {
                getUidStatsLocked(mapUid(oldWs.get(i3))).noteWifiStoppedLocked(elapsedRealtime);
            }
            List<WorkChain> workChains = oldWs.getWorkChains();
            if (workChains != null) {
                for (i = 0; i < workChains.size(); i++) {
                    getUidStatsLocked(mapUid(((WorkChain) workChains.get(i)).getAttributionUid())).noteWifiStoppedLocked(elapsedRealtime);
                }
            }
            N = newWs.size();
            for (i = 0; i < N; i++) {
                getUidStatsLocked(mapUid(newWs.get(i))).noteWifiRunningLocked(elapsedRealtime);
            }
            workChains = newWs.getWorkChains();
            if (workChains != null) {
                while (i2 < workChains.size()) {
                    getUidStatsLocked(mapUid(((WorkChain) workChains.get(i2)).getAttributionUid())).noteWifiRunningLocked(elapsedRealtime);
                    i2++;
                }
                return;
            }
            return;
        }
        Log.w(TAG, "noteWifiRunningChangedLocked -- called while WIFI not running");
    }

    public void noteWifiStoppedLocked(WorkSource ws) {
        if (this.mGlobalWifiRunning) {
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            long uptime = this.mClocks.uptimeMillis();
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states2 &= -536870913;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            int i = 0;
            this.mGlobalWifiRunning = false;
            this.mGlobalWifiRunningTimer.stopRunningLocked(elapsedRealtime);
            int N = ws.size();
            for (int i2 = 0; i2 < N; i2++) {
                getUidStatsLocked(mapUid(ws.get(i2))).noteWifiStoppedLocked(elapsedRealtime);
            }
            List<WorkChain> workChains = ws.getWorkChains();
            if (workChains != null) {
                while (i < workChains.size()) {
                    getUidStatsLocked(mapUid(((WorkChain) workChains.get(i)).getAttributionUid())).noteWifiStoppedLocked(elapsedRealtime);
                    i++;
                }
            }
            scheduleSyncExternalStatsLocked("wifi-stopped", 2);
            return;
        }
        Log.w(TAG, "noteWifiStoppedLocked -- called while WIFI not running");
    }

    public void noteWifiStateLocked(int wifiState, String accessPoint) {
        if (this.mWifiState != wifiState) {
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            if (this.mWifiState >= 0) {
                this.mWifiStateTimer[this.mWifiState].stopRunningLocked(elapsedRealtime);
            }
            this.mWifiState = wifiState;
            this.mWifiStateTimer[wifiState].startRunningLocked(elapsedRealtime);
            scheduleSyncExternalStatsLocked("wifi-state", 2);
        }
    }

    public void noteWifiSupplicantStateChangedLocked(int supplState, boolean failedAuth) {
        if (this.mWifiSupplState != supplState) {
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            long uptime = this.mClocks.uptimeMillis();
            if (this.mWifiSupplState >= 0) {
                this.mWifiSupplStateTimer[this.mWifiSupplState].stopRunningLocked(elapsedRealtime);
            }
            this.mWifiSupplState = supplState;
            this.mWifiSupplStateTimer[supplState].startRunningLocked(elapsedRealtime);
            this.mHistoryCur.states2 = (this.mHistoryCur.states2 & -16) | (supplState << 0);
            addHistoryRecordLocked(elapsedRealtime, uptime);
        }
    }

    void stopAllWifiSignalStrengthTimersLocked(int except) {
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        for (int i = 0; i < 5; i++) {
            if (i != except) {
                while (this.mWifiSignalStrengthsTimer[i].isRunningLocked()) {
                    this.mWifiSignalStrengthsTimer[i].stopRunningLocked(elapsedRealtime);
                }
            }
        }
    }

    public void noteWifiRssiChangedLocked(int newRssi) {
        int strengthBin = WifiManager.calculateSignalLevel(newRssi, 5);
        if (this.mWifiSignalStrengthBin != strengthBin) {
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            long uptime = this.mClocks.uptimeMillis();
            if (this.mWifiSignalStrengthBin >= 0) {
                this.mWifiSignalStrengthsTimer[this.mWifiSignalStrengthBin].stopRunningLocked(elapsedRealtime);
            }
            if (strengthBin >= 0) {
                if (!this.mWifiSignalStrengthsTimer[strengthBin].isRunningLocked()) {
                    this.mWifiSignalStrengthsTimer[strengthBin].startRunningLocked(elapsedRealtime);
                }
                this.mHistoryCur.states2 = (this.mHistoryCur.states2 & -113) | (strengthBin << 4);
                addHistoryRecordLocked(elapsedRealtime, uptime);
            } else {
                stopAllWifiSignalStrengthTimersLocked(-1);
            }
            StatsLog.write(38, strengthBin);
            this.mWifiSignalStrengthBin = strengthBin;
        }
    }

    public void noteFullWifiLockAcquiredLocked(int uid) {
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        if (this.mWifiFullLockNesting == 0) {
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states |= 268435456;
            addHistoryRecordLocked(elapsedRealtime, uptime);
        }
        this.mWifiFullLockNesting++;
        getUidStatsLocked(uid).noteFullWifiLockAcquiredLocked(elapsedRealtime);
    }

    public void noteFullWifiLockReleasedLocked(int uid) {
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        this.mWifiFullLockNesting--;
        if (this.mWifiFullLockNesting == 0) {
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states &= -268435457;
            addHistoryRecordLocked(elapsedRealtime, uptime);
        }
        getUidStatsLocked(uid).noteFullWifiLockReleasedLocked(elapsedRealtime);
    }

    public void noteWifiScanStartedLocked(int uid) {
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        if (this.mWifiScanNesting == 0) {
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states |= 134217728;
            addHistoryRecordLocked(elapsedRealtime, uptime);
        }
        this.mWifiScanNesting++;
        getUidStatsLocked(uid).noteWifiScanStartedLocked(elapsedRealtime);
        LogPower.push(158, Integer.toString(uid));
    }

    public void noteWifiScanStoppedLocked(int uid) {
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        this.mWifiScanNesting--;
        if (this.mWifiScanNesting == 0) {
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states &= -134217729;
            addHistoryRecordLocked(elapsedRealtime, uptime);
        }
        getUidStatsLocked(uid).noteWifiScanStoppedLocked(elapsedRealtime);
        LogPower.push(159, Integer.toString(uid));
    }

    public void noteWifiBatchedScanStartedLocked(int uid, int csph) {
        uid = mapUid(uid);
        getUidStatsLocked(uid).noteWifiBatchedScanStartedLocked(csph, this.mClocks.elapsedRealtime());
    }

    public void noteWifiBatchedScanStoppedLocked(int uid) {
        uid = mapUid(uid);
        getUidStatsLocked(uid).noteWifiBatchedScanStoppedLocked(this.mClocks.elapsedRealtime());
    }

    public void noteWifiMulticastEnabledLocked(int uid) {
        uid = mapUid(uid);
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        if (this.mWifiMulticastNesting == 0) {
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states |= Protocol.BASE_SYSTEM_RESERVED;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            if (!this.mWifiMulticastWakelockTimer.isRunningLocked()) {
                this.mWifiMulticastWakelockTimer.startRunningLocked(elapsedRealtime);
            }
        }
        this.mWifiMulticastNesting++;
        getUidStatsLocked(uid).noteWifiMulticastEnabledLocked(elapsedRealtime);
    }

    public void noteWifiMulticastDisabledLocked(int uid) {
        uid = mapUid(uid);
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        this.mWifiMulticastNesting--;
        if (this.mWifiMulticastNesting == 0) {
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states &= -65537;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            if (this.mWifiMulticastWakelockTimer.isRunningLocked()) {
                this.mWifiMulticastWakelockTimer.stopRunningLocked(elapsedRealtime);
            }
        }
        getUidStatsLocked(uid).noteWifiMulticastDisabledLocked(elapsedRealtime);
    }

    public void noteFullWifiLockAcquiredFromSourceLocked(WorkSource ws) {
        int N = ws.size();
        int i = 0;
        for (int i2 = 0; i2 < N; i2++) {
            noteFullWifiLockAcquiredLocked(mapUid(ws.get(i2)));
            StatsLog.write_non_chained(37, ws.get(i2), ws.getName(i2), 1);
        }
        List<WorkChain> workChains = ws.getWorkChains();
        if (workChains != null) {
            while (i < workChains.size()) {
                WorkChain workChain = (WorkChain) workChains.get(i);
                noteFullWifiLockAcquiredLocked(mapUid(workChain.getAttributionUid()));
                StatsLog.write(37, workChain.getUids(), workChain.getTags(), 1);
                i++;
            }
        }
    }

    public void noteFullWifiLockReleasedFromSourceLocked(WorkSource ws) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteFullWifiLockReleasedLocked(mapUid(ws.get(i)));
            StatsLog.write_non_chained(37, ws.get(i), ws.getName(i), 0);
        }
        List<WorkChain> workChains = ws.getWorkChains();
        if (workChains != null) {
            for (int i2 = 0; i2 < workChains.size(); i2++) {
                WorkChain workChain = (WorkChain) workChains.get(i2);
                noteFullWifiLockReleasedLocked(mapUid(workChain.getAttributionUid()));
                StatsLog.write(37, workChain.getUids(), workChain.getTags(), 0);
            }
        }
    }

    public void noteWifiScanStartedFromSourceLocked(WorkSource ws) {
        int N = ws.size();
        int i = 0;
        for (int i2 = 0; i2 < N; i2++) {
            noteWifiScanStartedLocked(mapUid(ws.get(i2)));
            StatsLog.write_non_chained(39, ws.get(i2), ws.getName(i2), 1);
        }
        List<WorkChain> workChains = ws.getWorkChains();
        if (workChains != null) {
            while (i < workChains.size()) {
                WorkChain workChain = (WorkChain) workChains.get(i);
                noteWifiScanStartedLocked(mapUid(workChain.getAttributionUid()));
                StatsLog.write(39, workChain.getUids(), workChain.getTags(), 1);
                i++;
            }
        }
    }

    public void noteWifiScanStoppedFromSourceLocked(WorkSource ws) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteWifiScanStoppedLocked(mapUid(ws.get(i)));
            StatsLog.write_non_chained(39, ws.get(i), ws.getName(i), 0);
        }
        List<WorkChain> workChains = ws.getWorkChains();
        if (workChains != null) {
            for (int i2 = 0; i2 < workChains.size(); i2++) {
                WorkChain workChain = (WorkChain) workChains.get(i2);
                noteWifiScanStoppedLocked(mapUid(workChain.getAttributionUid()));
                StatsLog.write(39, workChain.getUids(), workChain.getTags(), 0);
            }
        }
    }

    public void noteWifiBatchedScanStartedFromSourceLocked(WorkSource ws, int csph) {
        int N = ws.size();
        int i = 0;
        for (int i2 = 0; i2 < N; i2++) {
            noteWifiBatchedScanStartedLocked(ws.get(i2), csph);
        }
        List<WorkChain> workChains = ws.getWorkChains();
        if (workChains != null) {
            while (i < workChains.size()) {
                noteWifiBatchedScanStartedLocked(((WorkChain) workChains.get(i)).getAttributionUid(), csph);
                i++;
            }
        }
    }

    public void noteWifiBatchedScanStoppedFromSourceLocked(WorkSource ws) {
        int N = ws.size();
        int i = 0;
        for (int i2 = 0; i2 < N; i2++) {
            noteWifiBatchedScanStoppedLocked(ws.get(i2));
        }
        List<WorkChain> workChains = ws.getWorkChains();
        if (workChains != null) {
            while (i < workChains.size()) {
                noteWifiBatchedScanStoppedLocked(((WorkChain) workChains.get(i)).getAttributionUid());
                i++;
            }
        }
    }

    private static String[] includeInStringArray(String[] array, String str) {
        if (ArrayUtils.indexOf(array, str) >= 0) {
            return array;
        }
        String[] newArray = new String[(array.length + 1)];
        System.arraycopy(array, 0, newArray, 0, array.length);
        newArray[array.length] = str;
        return newArray;
    }

    private static String[] excludeFromStringArray(String[] array, String str) {
        int index = ArrayUtils.indexOf(array, str);
        if (index < 0) {
            return array;
        }
        String[] newArray = new String[(array.length - 1)];
        if (index > 0) {
            System.arraycopy(array, 0, newArray, 0, index);
        }
        if (index < array.length - 1) {
            System.arraycopy(array, index + 1, newArray, index, (array.length - index) - 1);
        }
        return newArray;
    }

    public void noteNetworkInterfaceTypeLocked(String iface, int networkType) {
        if (!TextUtils.isEmpty(iface)) {
            synchronized (this.mModemNetworkLock) {
                if (ConnectivityManager.isNetworkTypeMobile(networkType)) {
                    this.mModemIfaces = includeInStringArray(this.mModemIfaces, iface);
                } else {
                    this.mModemIfaces = excludeFromStringArray(this.mModemIfaces, iface);
                }
            }
            synchronized (this.mWifiNetworkLock) {
                if (ConnectivityManager.isNetworkTypeWifi(networkType)) {
                    this.mWifiIfaces = includeInStringArray(this.mWifiIfaces, iface);
                } else {
                    this.mWifiIfaces = excludeFromStringArray(this.mWifiIfaces, iface);
                }
            }
        }
    }

    public String[] getWifiIfaces() {
        String[] strArr;
        synchronized (this.mWifiNetworkLock) {
            strArr = this.mWifiIfaces;
        }
        return strArr;
    }

    public String[] getMobileIfaces() {
        String[] strArr;
        synchronized (this.mModemNetworkLock) {
            strArr = this.mModemIfaces;
        }
        return strArr;
    }

    public long getScreenOnTime(long elapsedRealtimeUs, int which) {
        if (this.mScreenOnTimer != null) {
            return this.mScreenOnTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
        }
        return 0;
    }

    public int getScreenOnCount(int which) {
        if (this.mScreenOnTimer != null) {
            return this.mScreenOnTimer.getCountLocked(which);
        }
        return 0;
    }

    public long getScreenDozeTime(long elapsedRealtimeUs, int which) {
        return this.mScreenDozeTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    public int getScreenDozeCount(int which) {
        return this.mScreenDozeTimer.getCountLocked(which);
    }

    public long getScreenBrightnessTime(int brightnessBin, long elapsedRealtimeUs, int which) {
        if (this.mScreenBrightnessTimer[brightnessBin] != null) {
            return this.mScreenBrightnessTimer[brightnessBin].getTotalTimeLocked(elapsedRealtimeUs, which);
        }
        return 0;
    }

    public Timer getScreenBrightnessTimer(int brightnessBin) {
        return this.mScreenBrightnessTimer[brightnessBin];
    }

    public long getInteractiveTime(long elapsedRealtimeUs, int which) {
        if (this.mInteractiveTimer != null) {
            return this.mInteractiveTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
        }
        return 0;
    }

    public long getPowerSaveModeEnabledTime(long elapsedRealtimeUs, int which) {
        if (this.mPowerSaveModeEnabledTimer != null) {
            return this.mPowerSaveModeEnabledTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
        }
        return 0;
    }

    public int getPowerSaveModeEnabledCount(int which) {
        if (this.mPowerSaveModeEnabledTimer != null) {
            return this.mPowerSaveModeEnabledTimer.getCountLocked(which);
        }
        return 0;
    }

    public long getDeviceIdleModeTime(int mode, long elapsedRealtimeUs, int which) {
        switch (mode) {
            case 1:
                return this.mDeviceIdleModeLightTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
            case 2:
                return this.mDeviceIdleModeFullTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
            default:
                return 0;
        }
    }

    public int getDeviceIdleModeCount(int mode, int which) {
        switch (mode) {
            case 1:
                return this.mDeviceIdleModeLightTimer.getCountLocked(which);
            case 2:
                return this.mDeviceIdleModeFullTimer.getCountLocked(which);
            default:
                return 0;
        }
    }

    public long getLongestDeviceIdleModeTime(int mode) {
        switch (mode) {
            case 1:
                return this.mLongestLightIdleTime;
            case 2:
                return this.mLongestFullIdleTime;
            default:
                return 0;
        }
    }

    public long getDeviceIdlingTime(int mode, long elapsedRealtimeUs, int which) {
        switch (mode) {
            case 1:
                return this.mDeviceLightIdlingTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
            case 2:
                return this.mDeviceIdlingTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
            default:
                return 0;
        }
    }

    public int getDeviceIdlingCount(int mode, int which) {
        switch (mode) {
            case 1:
                return this.mDeviceLightIdlingTimer.getCountLocked(which);
            case 2:
                return this.mDeviceIdlingTimer.getCountLocked(which);
            default:
                return 0;
        }
    }

    public int getNumConnectivityChange(int which) {
        int val = this.mNumConnectivityChange;
        if (which == 1) {
            return val - this.mLoadedNumConnectivityChange;
        }
        if (which == 2) {
            return val - this.mUnpluggedNumConnectivityChange;
        }
        return val;
    }

    public long getGpsSignalQualityTime(int strengthBin, long elapsedRealtimeUs, int which) {
        if (strengthBin < 0 || strengthBin >= 2) {
            return 0;
        }
        return this.mGpsSignalQualityTimer[strengthBin].getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    public long getGpsBatteryDrainMaMs() {
        if (this.mPowerProfile.getAveragePower(PowerProfile.POWER_GPS_OPERATING_VOLTAGE) / 1000.0d == 0.0d) {
            return 0;
        }
        long rawRealtime = SystemClock.elapsedRealtime() * 1000;
        int i = 0;
        double energyUsedMaMs = 0.0d;
        int i2 = 0;
        while (i2 < 2) {
            energyUsedMaMs += this.mPowerProfile.getAveragePower(PowerProfile.POWER_GPS_SIGNAL_QUALITY_BASED, i2) * ((double) (getGpsSignalQualityTime(i2, rawRealtime, i) / 1000));
            i2++;
            i = 0;
        }
        return (long) energyUsedMaMs;
    }

    public long getPhoneOnTime(long elapsedRealtimeUs, int which) {
        if (this.mPhoneOnTimer != null) {
            return this.mPhoneOnTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
        }
        return 0;
    }

    public int getPhoneOnCount(int which) {
        if (this.mPhoneOnTimer != null) {
            return this.mPhoneOnTimer.getCountLocked(which);
        }
        return 0;
    }

    public long getPhoneSignalStrengthTime(int strengthBin, long elapsedRealtimeUs, int which) {
        return this.mPhoneSignalStrengthsTimer[strengthBin].getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    public long getPhoneSignalScanningTime(long elapsedRealtimeUs, int which) {
        return this.mPhoneSignalScanningTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    public Timer getPhoneSignalScanningTimer() {
        return this.mPhoneSignalScanningTimer;
    }

    public int getPhoneSignalStrengthCount(int strengthBin, int which) {
        return this.mPhoneSignalStrengthsTimer[strengthBin].getCountLocked(which);
    }

    public Timer getPhoneSignalStrengthTimer(int strengthBin) {
        return this.mPhoneSignalStrengthsTimer[strengthBin];
    }

    public long getPhoneDataConnectionTime(int dataType, long elapsedRealtimeUs, int which) {
        return this.mPhoneDataConnectionsTimer[dataType].getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    public int getPhoneDataConnectionCount(int dataType, int which) {
        return this.mPhoneDataConnectionsTimer[dataType].getCountLocked(which);
    }

    public Timer getPhoneDataConnectionTimer(int dataType) {
        return this.mPhoneDataConnectionsTimer[dataType];
    }

    public long getMobileRadioActiveTime(long elapsedRealtimeUs, int which) {
        return this.mMobileRadioActiveTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    public int getMobileRadioActiveCount(int which) {
        return this.mMobileRadioActiveTimer.getCountLocked(which);
    }

    public long getMobileRadioActiveAdjustedTime(int which) {
        return this.mMobileRadioActiveAdjustedTime.getCountLocked(which);
    }

    public long getMobileRadioActiveUnknownTime(int which) {
        return this.mMobileRadioActiveUnknownTime.getCountLocked(which);
    }

    public int getMobileRadioActiveUnknownCount(int which) {
        return (int) this.mMobileRadioActiveUnknownCount.getCountLocked(which);
    }

    public long getWifiMulticastWakelockTime(long elapsedRealtimeUs, int which) {
        return this.mWifiMulticastWakelockTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    public int getWifiMulticastWakelockCount(int which) {
        return this.mWifiMulticastWakelockTimer.getCountLocked(which);
    }

    public long getWifiOnTime(long elapsedRealtimeUs, int which) {
        return this.mWifiOnTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    public long getWifiActiveTime(long elapsedRealtimeUs, int which) {
        return this.mWifiActiveTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    public long getGlobalWifiRunningTime(long elapsedRealtimeUs, int which) {
        if (this.mGlobalWifiRunningTimer != null) {
            return this.mGlobalWifiRunningTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
        }
        return 0;
    }

    public long getWifiStateTime(int wifiState, long elapsedRealtimeUs, int which) {
        return this.mWifiStateTimer[wifiState].getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    public int getWifiStateCount(int wifiState, int which) {
        return this.mWifiStateTimer[wifiState].getCountLocked(which);
    }

    public Timer getWifiStateTimer(int wifiState) {
        return this.mWifiStateTimer[wifiState];
    }

    public long getWifiSupplStateTime(int state, long elapsedRealtimeUs, int which) {
        return this.mWifiSupplStateTimer[state].getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    public int getWifiSupplStateCount(int state, int which) {
        return this.mWifiSupplStateTimer[state].getCountLocked(which);
    }

    public Timer getWifiSupplStateTimer(int state) {
        return this.mWifiSupplStateTimer[state];
    }

    public long getWifiSignalStrengthTime(int strengthBin, long elapsedRealtimeUs, int which) {
        return this.mWifiSignalStrengthsTimer[strengthBin].getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    public int getWifiSignalStrengthCount(int strengthBin, int which) {
        return this.mWifiSignalStrengthsTimer[strengthBin].getCountLocked(which);
    }

    public Timer getWifiSignalStrengthTimer(int strengthBin) {
        return this.mWifiSignalStrengthsTimer[strengthBin];
    }

    public ControllerActivityCounter getBluetoothControllerActivity() {
        return this.mBluetoothActivity;
    }

    public ControllerActivityCounter getWifiControllerActivity() {
        return this.mWifiActivity;
    }

    public ControllerActivityCounter getModemControllerActivity() {
        return this.mModemActivity;
    }

    public boolean hasBluetoothActivityReporting() {
        return this.mHasBluetoothReporting;
    }

    public boolean hasWifiActivityReporting() {
        return this.mHasWifiReporting;
    }

    public boolean hasModemActivityReporting() {
        return this.mHasModemReporting;
    }

    public long getFlashlightOnTime(long elapsedRealtimeUs, int which) {
        return this.mFlashlightOnTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    public long getFlashlightOnCount(int which) {
        return (long) this.mFlashlightOnTimer.getCountLocked(which);
    }

    public long getCameraOnTime(long elapsedRealtimeUs, int which) {
        return this.mCameraOnTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    public long getBluetoothScanTime(long elapsedRealtimeUs, int which) {
        return this.mBluetoothScanTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    public long getNetworkActivityBytes(int type, int which) {
        if (type < 0 || type >= this.mNetworkByteActivityCounters.length) {
            return 0;
        }
        return this.mNetworkByteActivityCounters[type].getCountLocked(which);
    }

    public long getNetworkActivityPackets(int type, int which) {
        if (type < 0 || type >= this.mNetworkPacketActivityCounters.length) {
            return 0;
        }
        return this.mNetworkPacketActivityCounters[type].getCountLocked(which);
    }

    public long getStartClockTime() {
        long currentTime = System.currentTimeMillis();
        if (ensureStartClockTime(currentTime)) {
            recordCurrentTimeChangeLocked(currentTime, this.mClocks.elapsedRealtime(), this.mClocks.uptimeMillis());
        }
        return this.mStartClockTime;
    }

    public String getStartPlatformVersion() {
        return this.mStartPlatformVersion;
    }

    public String getEndPlatformVersion() {
        return this.mEndPlatformVersion;
    }

    public int getParcelVersion() {
        return 177;
    }

    public boolean getIsOnBattery() {
        return this.mOnBattery;
    }

    public SparseArray<? extends android.os.BatteryStats.Uid> getUidStats() {
        return this.mUidStats;
    }

    private static void detachTimerIfNotNull(Timer timer) {
        if (timer != null) {
            timer.detach();
        }
    }

    private static boolean resetTimerIfNotNull(Timer timer, boolean detachIfReset) {
        if (timer != null) {
            return timer.reset(detachIfReset);
        }
        return true;
    }

    private static boolean resetTimerIfNotNull(DualTimer timer, boolean detachIfReset) {
        if (timer != null) {
            return timer.reset(detachIfReset);
        }
        return true;
    }

    private static void detachLongCounterIfNotNull(LongSamplingCounter counter) {
        if (counter != null) {
            counter.detach();
        }
    }

    private static void resetLongCounterIfNotNull(LongSamplingCounter counter, boolean detachIfReset) {
        if (counter != null) {
            counter.reset(detachIfReset);
        }
    }

    public long[] getCpuFreqs() {
        return this.mCpuFreqs;
    }

    public BatteryStatsImpl(File systemDir, Handler handler, PlatformIdleStateCallback cb, UserInfoProvider userInfoProvider) {
        this(new SystemClocks(), systemDir, handler, cb, userInfoProvider);
    }

    private BatteryStatsImpl(Clocks clocks, File systemDir, Handler handler, PlatformIdleStateCallback cb, UserInfoProvider userInfoProvider) {
        int i;
        int i2;
        File file = systemDir;
        this.mKernelWakelockReader = new KernelWakelockReader();
        this.mTmpWakelockStats = new KernelWakelockStats();
        this.mKernelUidCpuTimeReader = new KernelUidCpuTimeReader();
        this.mKernelUidCpuFreqTimeReader = new KernelUidCpuFreqTimeReader();
        this.mKernelUidCpuActiveTimeReader = new KernelUidCpuActiveTimeReader();
        this.mKernelUidCpuClusterTimeReader = new KernelUidCpuClusterTimeReader();
        this.mKernelMemoryBandwidthStats = new KernelMemoryBandwidthStats();
        this.mKernelMemoryStats = new LongSparseArray();
        this.mPerProcStateCpuTimesAvailable = true;
        this.mPendingUids = new SparseIntArray();
        this.mCpuTimeReadsTrackingStartTime = SystemClock.uptimeMillis();
        this.mTmpRpmStats = new RpmStats();
        this.mLastRpmStatsUpdateTimeMs = -1000;
        this.mPendingRemovedUids = new LinkedList();
        this.mExternalSync = null;
        this.mUserInfoProvider = null;
        this.mIsolatedUids = new SparseIntArray();
        this.mUidStats = new SparseArray();
        this.mPartialTimers = new ArrayList();
        this.mFullTimers = new ArrayList();
        this.mWindowTimers = new ArrayList();
        this.mDrawTimers = new ArrayList();
        this.mSensorTimers = new SparseArray();
        this.mWifiRunningTimers = new ArrayList();
        this.mFullWifiLockTimers = new ArrayList();
        this.mWifiMulticastTimers = new ArrayList();
        this.mWifiScanTimers = new ArrayList();
        this.mWifiBatchedScanTimers = new SparseArray();
        this.mAudioTurnedOnTimers = new ArrayList();
        this.mVideoTurnedOnTimers = new ArrayList();
        this.mFlashlightTurnedOnTimers = new ArrayList();
        this.mCameraTurnedOnTimers = new ArrayList();
        this.mBluetoothScanOnTimers = new ArrayList();
        this.mLastPartialTimers = new ArrayList();
        this.mOnBatteryTimeBase = new TimeBase();
        this.mOnBatteryScreenOffTimeBase = new TimeBase();
        this.mActiveEvents = new HistoryEventTracker();
        this.mHaveBatteryLevel = false;
        this.mRecordingHistory = false;
        this.mHistoryBuffer = Parcel.obtain();
        this.mHistoryLastWritten = new HistoryItem();
        this.mHistoryLastLastWritten = new HistoryItem();
        this.mHistoryReadTmp = new HistoryItem();
        this.mHistoryAddTmp = new HistoryItem();
        this.mHistoryTagPool = new HashMap();
        this.mNextHistoryTagIdx = 0;
        this.mNumHistoryTagChars = 0;
        this.mHistoryBufferLastPos = -1;
        this.mHistoryOverflow = false;
        this.mActiveHistoryStates = -1;
        this.mActiveHistoryStates2 = -1;
        this.mLastHistoryElapsedRealtime = 0;
        this.mTrackRunningHistoryElapsedRealtime = 0;
        this.mTrackRunningHistoryUptime = 0;
        this.mHistoryCur = new HistoryItem();
        this.mLastHistoryStepDetails = null;
        this.mLastHistoryStepLevel = (byte) 0;
        this.mCurHistoryStepDetails = new HistoryStepDetails();
        this.mReadHistoryStepDetails = new HistoryStepDetails();
        this.mTmpHistoryStepDetails = new HistoryStepDetails();
        this.mScreenState = 0;
        this.mScreenBrightnessBin = -1;
        this.mScreenBrightnessTimer = new StopwatchTimer[5];
        this.mUsbDataState = 0;
        this.mGpsSignalQualityBin = -1;
        this.mGpsSignalQualityTimer = new StopwatchTimer[2];
        this.mPhoneSignalStrengthBin = -1;
        this.mPhoneSignalStrengthBinRaw = -1;
        this.mPhoneSignalStrengthsTimer = new StopwatchTimer[5];
        this.mPhoneDataConnectionType = -1;
        this.mPhoneDataConnectionsTimer = new StopwatchTimer[21];
        this.mNetworkByteActivityCounters = new LongSamplingCounter[10];
        this.mNetworkPacketActivityCounters = new LongSamplingCounter[10];
        this.mHasWifiReporting = false;
        this.mHasBluetoothReporting = false;
        this.mHasModemReporting = false;
        this.mWifiState = -1;
        this.mWifiStateTimer = new StopwatchTimer[8];
        this.mWifiSupplState = -1;
        this.mWifiSupplStateTimer = new StopwatchTimer[13];
        this.mWifiSignalStrengthBin = -1;
        this.mWifiSignalStrengthsTimer = new StopwatchTimer[5];
        this.mIsCellularTxPowerHigh = false;
        this.mMobileRadioPowerState = 1;
        this.mWifiRadioPowerState = 1;
        this.mCharging = true;
        this.mInitStepMode = 0;
        this.mCurStepMode = 0;
        this.mModStepMode = 0;
        this.mDischargeStepTracker = new LevelStepTracker(200);
        this.mDailyDischargeStepTracker = new LevelStepTracker(400);
        this.mChargeStepTracker = new LevelStepTracker(200);
        this.mDailyChargeStepTracker = new LevelStepTracker(400);
        this.mDailyStartTime = 0;
        this.mNextMinDailyDeadline = 0;
        this.mNextMaxDailyDeadline = 0;
        this.mDailyItems = new ArrayList();
        this.mLastWriteTime = 0;
        this.mPhoneServiceState = -1;
        this.mPhoneServiceStateRaw = -1;
        this.mPhoneSimStateRaw = -1;
        this.mEstimatedBatteryCapacity = -1;
        this.mMinLearnedBatteryCapacity = -1;
        this.mMaxLearnedBatteryCapacity = -1;
        this.mRpmStats = new HashMap();
        this.mScreenOffRpmStats = new HashMap();
        this.mKernelWakelockStats = new HashMap();
        this.mLastWakeupReason = null;
        this.mLastWakeupUptimeMs = 0;
        this.mWakeupReasonStats = new HashMap();
        this.mChangedStates = 0;
        this.mChangedStates2 = 0;
        this.mInitialAcquireWakeUid = -1;
        this.mWifiFullLockNesting = 0;
        this.mWifiScanNesting = 0;
        this.mWifiMulticastNesting = 0;
        this.mNetworkStatsFactory = new NetworkStatsFactory();
        this.mNetworkStatsPool = new SynchronizedPool(6);
        this.mWifiNetworkLock = new Object();
        this.mWifiIfaces = EmptyArray.STRING;
        this.mLastWifiNetworkStats = new NetworkStats(0, -1);
        this.mModemNetworkLock = new Object();
        this.mModemIfaces = EmptyArray.STRING;
        this.mLastModemNetworkStats = new NetworkStats(0, -1);
        this.mLastModemActivityInfo = new ModemActivityInfo(0, 0, 0, new int[0], 0, 0);
        this.mLastBluetoothActivityInfo = new BluetoothActivityInfoCache(this, null);
        this.mPendingWrite = null;
        this.mWriteLock = new ReentrantLock();
        init(clocks);
        if (file != null) {
            this.mFile = new JournaledFile(new File(file, "batterystats.bin"), new File(file, "batterystats.bin.tmp"));
        } else {
            this.mFile = null;
        }
        this.mCheckinFile = new AtomicFile(new File(file, "batterystats-checkin.bin"));
        this.mDailyFile = new AtomicFile(new File(file, "batterystats-daily.xml"));
        this.mHandler = new MyHandler(handler.getLooper());
        this.mConstants = new Constants(this.mHandler);
        this.mStartCount++;
        this.mScreenOnTimer = new StopwatchTimer(this.mClocks, null, -1, null, this.mOnBatteryTimeBase);
        this.mScreenDozeTimer = new StopwatchTimer(this.mClocks, null, -1, null, this.mOnBatteryTimeBase);
        for (i = 0; i < 5; i++) {
            this.mScreenBrightnessTimer[i] = new StopwatchTimer(this.mClocks, null, -100 - i, null, this.mOnBatteryTimeBase);
        }
        this.mInteractiveTimer = new StopwatchTimer(this.mClocks, null, -10, null, this.mOnBatteryTimeBase);
        this.mPowerSaveModeEnabledTimer = new StopwatchTimer(this.mClocks, null, -2, null, this.mOnBatteryTimeBase);
        this.mDeviceIdleModeLightTimer = new StopwatchTimer(this.mClocks, null, -11, null, this.mOnBatteryTimeBase);
        this.mDeviceIdleModeFullTimer = new StopwatchTimer(this.mClocks, null, -14, null, this.mOnBatteryTimeBase);
        this.mDeviceLightIdlingTimer = new StopwatchTimer(this.mClocks, null, -15, null, this.mOnBatteryTimeBase);
        this.mDeviceIdlingTimer = new StopwatchTimer(this.mClocks, null, -12, null, this.mOnBatteryTimeBase);
        this.mPhoneOnTimer = new StopwatchTimer(this.mClocks, null, -3, null, this.mOnBatteryTimeBase);
        for (i = 0; i < 5; i++) {
            this.mPhoneSignalStrengthsTimer[i] = new StopwatchTimer(this.mClocks, null, -200 - i, null, this.mOnBatteryTimeBase);
        }
        this.mPhoneSignalScanningTimer = new StopwatchTimer(this.mClocks, null, -199, null, this.mOnBatteryTimeBase);
        for (i = 0; i < 21; i++) {
            this.mPhoneDataConnectionsTimer[i] = new StopwatchTimer(this.mClocks, null, -300 - i, null, this.mOnBatteryTimeBase);
        }
        for (i = 0; i < 10; i++) {
            this.mNetworkByteActivityCounters[i] = new LongSamplingCounter(this.mOnBatteryTimeBase);
            this.mNetworkPacketActivityCounters[i] = new LongSamplingCounter(this.mOnBatteryTimeBase);
        }
        this.mWifiActivity = new ControllerActivityCounterImpl(this.mOnBatteryTimeBase, 1);
        this.mBluetoothActivity = new ControllerActivityCounterImpl(this.mOnBatteryTimeBase, 1);
        this.mModemActivity = new ControllerActivityCounterImpl(this.mOnBatteryTimeBase, 5);
        this.mMobileRadioActiveTimer = new StopwatchTimer(this.mClocks, null, -400, null, this.mOnBatteryTimeBase);
        this.mMobileRadioActivePerAppTimer = new StopwatchTimer(this.mClocks, null, -401, null, this.mOnBatteryTimeBase);
        this.mMobileRadioActiveAdjustedTime = new LongSamplingCounter(this.mOnBatteryTimeBase);
        this.mMobileRadioActiveUnknownTime = new LongSamplingCounter(this.mOnBatteryTimeBase);
        this.mMobileRadioActiveUnknownCount = new LongSamplingCounter(this.mOnBatteryTimeBase);
        this.mWifiMulticastWakelockTimer = new StopwatchTimer(this.mClocks, null, 23, null, this.mOnBatteryTimeBase);
        this.mWifiOnTimer = new StopwatchTimer(this.mClocks, null, -4, null, this.mOnBatteryTimeBase);
        this.mGlobalWifiRunningTimer = new StopwatchTimer(this.mClocks, null, -5, null, this.mOnBatteryTimeBase);
        for (i2 = 0; i2 < 8; i2++) {
            this.mWifiStateTimer[i2] = new StopwatchTimer(this.mClocks, null, -600 - i2, null, this.mOnBatteryTimeBase);
        }
        for (i2 = 0; i2 < 13; i2++) {
            this.mWifiSupplStateTimer[i2] = new StopwatchTimer(this.mClocks, null, -700 - i2, null, this.mOnBatteryTimeBase);
        }
        for (i2 = 0; i2 < 5; i2++) {
            this.mWifiSignalStrengthsTimer[i2] = new StopwatchTimer(this.mClocks, null, -800 - i2, null, this.mOnBatteryTimeBase);
        }
        this.mWifiActiveTimer = new StopwatchTimer(this.mClocks, null, -900, null, this.mOnBatteryTimeBase);
        for (i2 = 0; i2 < 2; i2++) {
            this.mGpsSignalQualityTimer[i2] = new StopwatchTimer(this.mClocks, null, -1000 - i2, null, this.mOnBatteryTimeBase);
        }
        this.mAudioOnTimer = new StopwatchTimer(this.mClocks, null, -7, null, this.mOnBatteryTimeBase);
        this.mVideoOnTimer = new StopwatchTimer(this.mClocks, null, -8, null, this.mOnBatteryTimeBase);
        this.mFlashlightOnTimer = new StopwatchTimer(this.mClocks, null, -9, null, this.mOnBatteryTimeBase);
        this.mCameraOnTimer = new StopwatchTimer(this.mClocks, null, -13, null, this.mOnBatteryTimeBase);
        this.mBluetoothScanTimer = new StopwatchTimer(this.mClocks, null, -14, null, this.mOnBatteryTimeBase);
        this.mDischargeScreenOffCounter = new LongSamplingCounter(this.mOnBatteryScreenOffTimeBase);
        this.mDischargeScreenDozeCounter = new LongSamplingCounter(this.mOnBatteryTimeBase);
        this.mDischargeLightDozeCounter = new LongSamplingCounter(this.mOnBatteryTimeBase);
        this.mDischargeDeepDozeCounter = new LongSamplingCounter(this.mOnBatteryTimeBase);
        this.mDischargeCounter = new LongSamplingCounter(this.mOnBatteryTimeBase);
        this.mOnBatteryInternal = false;
        this.mOnBattery = false;
        initTimes(this.mClocks.uptimeMillis() * 1000, this.mClocks.elapsedRealtime() * 1000);
        String str = Build.ID;
        this.mEndPlatformVersion = str;
        this.mStartPlatformVersion = str;
        this.mDischargeStartLevel = 0;
        this.mDischargeUnplugLevel = 0;
        this.mDischargePlugLevel = -1;
        this.mDischargeCurrentLevel = 0;
        this.mCurrentBatteryLevel = 0;
        initDischarge();
        clearHistoryLocked();
        updateDailyDeadlineLocked();
        this.mPlatformIdleStateCallback = cb;
        this.mUserInfoProvider = userInfoProvider;
    }

    public BatteryStatsImpl(Parcel p) {
        this(new SystemClocks(), p);
    }

    public BatteryStatsImpl(Clocks clocks, Parcel p) {
        this.mKernelWakelockReader = new KernelWakelockReader();
        this.mTmpWakelockStats = new KernelWakelockStats();
        this.mKernelUidCpuTimeReader = new KernelUidCpuTimeReader();
        this.mKernelUidCpuFreqTimeReader = new KernelUidCpuFreqTimeReader();
        this.mKernelUidCpuActiveTimeReader = new KernelUidCpuActiveTimeReader();
        this.mKernelUidCpuClusterTimeReader = new KernelUidCpuClusterTimeReader();
        this.mKernelMemoryBandwidthStats = new KernelMemoryBandwidthStats();
        this.mKernelMemoryStats = new LongSparseArray();
        this.mPerProcStateCpuTimesAvailable = true;
        this.mPendingUids = new SparseIntArray();
        this.mCpuTimeReadsTrackingStartTime = SystemClock.uptimeMillis();
        this.mTmpRpmStats = new RpmStats();
        this.mLastRpmStatsUpdateTimeMs = -1000;
        this.mPendingRemovedUids = new LinkedList();
        this.mExternalSync = null;
        this.mUserInfoProvider = null;
        this.mIsolatedUids = new SparseIntArray();
        this.mUidStats = new SparseArray();
        this.mPartialTimers = new ArrayList();
        this.mFullTimers = new ArrayList();
        this.mWindowTimers = new ArrayList();
        this.mDrawTimers = new ArrayList();
        this.mSensorTimers = new SparseArray();
        this.mWifiRunningTimers = new ArrayList();
        this.mFullWifiLockTimers = new ArrayList();
        this.mWifiMulticastTimers = new ArrayList();
        this.mWifiScanTimers = new ArrayList();
        this.mWifiBatchedScanTimers = new SparseArray();
        this.mAudioTurnedOnTimers = new ArrayList();
        this.mVideoTurnedOnTimers = new ArrayList();
        this.mFlashlightTurnedOnTimers = new ArrayList();
        this.mCameraTurnedOnTimers = new ArrayList();
        this.mBluetoothScanOnTimers = new ArrayList();
        this.mLastPartialTimers = new ArrayList();
        this.mOnBatteryTimeBase = new TimeBase();
        this.mOnBatteryScreenOffTimeBase = new TimeBase();
        this.mActiveEvents = new HistoryEventTracker();
        this.mHaveBatteryLevel = false;
        this.mRecordingHistory = false;
        this.mHistoryBuffer = Parcel.obtain();
        this.mHistoryLastWritten = new HistoryItem();
        this.mHistoryLastLastWritten = new HistoryItem();
        this.mHistoryReadTmp = new HistoryItem();
        this.mHistoryAddTmp = new HistoryItem();
        this.mHistoryTagPool = new HashMap();
        this.mNextHistoryTagIdx = 0;
        this.mNumHistoryTagChars = 0;
        this.mHistoryBufferLastPos = -1;
        this.mHistoryOverflow = false;
        this.mActiveHistoryStates = -1;
        this.mActiveHistoryStates2 = -1;
        this.mLastHistoryElapsedRealtime = 0;
        this.mTrackRunningHistoryElapsedRealtime = 0;
        this.mTrackRunningHistoryUptime = 0;
        this.mHistoryCur = new HistoryItem();
        this.mLastHistoryStepDetails = null;
        this.mLastHistoryStepLevel = (byte) 0;
        this.mCurHistoryStepDetails = new HistoryStepDetails();
        this.mReadHistoryStepDetails = new HistoryStepDetails();
        this.mTmpHistoryStepDetails = new HistoryStepDetails();
        this.mScreenState = 0;
        this.mScreenBrightnessBin = -1;
        this.mScreenBrightnessTimer = new StopwatchTimer[5];
        this.mUsbDataState = 0;
        this.mGpsSignalQualityBin = -1;
        this.mGpsSignalQualityTimer = new StopwatchTimer[2];
        this.mPhoneSignalStrengthBin = -1;
        this.mPhoneSignalStrengthBinRaw = -1;
        this.mPhoneSignalStrengthsTimer = new StopwatchTimer[5];
        this.mPhoneDataConnectionType = -1;
        this.mPhoneDataConnectionsTimer = new StopwatchTimer[21];
        this.mNetworkByteActivityCounters = new LongSamplingCounter[10];
        this.mNetworkPacketActivityCounters = new LongSamplingCounter[10];
        this.mHasWifiReporting = false;
        this.mHasBluetoothReporting = false;
        this.mHasModemReporting = false;
        this.mWifiState = -1;
        this.mWifiStateTimer = new StopwatchTimer[8];
        this.mWifiSupplState = -1;
        this.mWifiSupplStateTimer = new StopwatchTimer[13];
        this.mWifiSignalStrengthBin = -1;
        this.mWifiSignalStrengthsTimer = new StopwatchTimer[5];
        this.mIsCellularTxPowerHigh = false;
        this.mMobileRadioPowerState = 1;
        this.mWifiRadioPowerState = 1;
        this.mCharging = true;
        this.mInitStepMode = 0;
        this.mCurStepMode = 0;
        this.mModStepMode = 0;
        this.mDischargeStepTracker = new LevelStepTracker(200);
        this.mDailyDischargeStepTracker = new LevelStepTracker(400);
        this.mChargeStepTracker = new LevelStepTracker(200);
        this.mDailyChargeStepTracker = new LevelStepTracker(400);
        this.mDailyStartTime = 0;
        this.mNextMinDailyDeadline = 0;
        this.mNextMaxDailyDeadline = 0;
        this.mDailyItems = new ArrayList();
        this.mLastWriteTime = 0;
        this.mPhoneServiceState = -1;
        this.mPhoneServiceStateRaw = -1;
        this.mPhoneSimStateRaw = -1;
        this.mEstimatedBatteryCapacity = -1;
        this.mMinLearnedBatteryCapacity = -1;
        this.mMaxLearnedBatteryCapacity = -1;
        this.mRpmStats = new HashMap();
        this.mScreenOffRpmStats = new HashMap();
        this.mKernelWakelockStats = new HashMap();
        this.mLastWakeupReason = null;
        this.mLastWakeupUptimeMs = 0;
        this.mWakeupReasonStats = new HashMap();
        this.mChangedStates = 0;
        this.mChangedStates2 = 0;
        this.mInitialAcquireWakeUid = -1;
        this.mWifiFullLockNesting = 0;
        this.mWifiScanNesting = 0;
        this.mWifiMulticastNesting = 0;
        this.mNetworkStatsFactory = new NetworkStatsFactory();
        this.mNetworkStatsPool = new SynchronizedPool(6);
        this.mWifiNetworkLock = new Object();
        this.mWifiIfaces = EmptyArray.STRING;
        this.mLastWifiNetworkStats = new NetworkStats(0, -1);
        this.mModemNetworkLock = new Object();
        this.mModemIfaces = EmptyArray.STRING;
        this.mLastModemNetworkStats = new NetworkStats(0, -1);
        this.mLastModemActivityInfo = new ModemActivityInfo(0, 0, 0, new int[0], 0, 0);
        this.mLastBluetoothActivityInfo = new BluetoothActivityInfoCache(this, null);
        this.mPendingWrite = null;
        this.mWriteLock = new ReentrantLock();
        init(clocks);
        this.mFile = null;
        this.mCheckinFile = null;
        this.mDailyFile = null;
        this.mHandler = null;
        this.mExternalSync = null;
        this.mConstants = new Constants(this.mHandler);
        clearHistoryLocked();
        readFromParcel(p);
        this.mPlatformIdleStateCallback = null;
    }

    public void setPowerProfileLocked(PowerProfile profile) {
        this.mPowerProfile = profile;
        int numClusters = this.mPowerProfile.getNumCpuClusters();
        this.mKernelCpuSpeedReaders = new KernelCpuSpeedReader[numClusters];
        int firstCpuOfCluster = 0;
        for (int i = 0; i < numClusters; i++) {
            this.mKernelCpuSpeedReaders[i] = new KernelCpuSpeedReader(firstCpuOfCluster, this.mPowerProfile.getNumSpeedStepsInCpuCluster(i));
            firstCpuOfCluster += this.mPowerProfile.getNumCoresInCpuCluster(i);
        }
        if (this.mEstimatedBatteryCapacity == -1) {
            this.mEstimatedBatteryCapacity = (int) this.mPowerProfile.getBatteryCapacity();
        }
    }

    public void setCallback(BatteryCallback cb) {
        this.mCallback = cb;
    }

    public void setRadioScanningTimeoutLocked(long timeout) {
        if (this.mPhoneSignalScanningTimer != null) {
            this.mPhoneSignalScanningTimer.setTimeout(timeout);
        }
    }

    public void setExternalStatsSyncLocked(ExternalStatsSync sync) {
        this.mExternalSync = sync;
    }

    public void updateDailyDeadlineLocked() {
        long currentTime = System.currentTimeMillis();
        this.mDailyStartTime = currentTime;
        Calendar calDeadline = Calendar.getInstance();
        calDeadline.setTimeInMillis(currentTime);
        calDeadline.set(6, calDeadline.get(6) + 1);
        calDeadline.set(14, 0);
        calDeadline.set(13, 0);
        calDeadline.set(12, 0);
        calDeadline.set(11, 1);
        this.mNextMinDailyDeadline = calDeadline.getTimeInMillis();
        calDeadline.set(11, 3);
        this.mNextMaxDailyDeadline = calDeadline.getTimeInMillis();
    }

    public void recordDailyStatsIfNeededLocked(boolean settled) {
        long currentTime = System.currentTimeMillis();
        if (currentTime >= this.mNextMaxDailyDeadline) {
            recordDailyStatsLocked();
        } else if (settled && currentTime >= this.mNextMinDailyDeadline) {
            recordDailyStatsLocked();
        } else if (currentTime < this.mDailyStartTime - 86400000) {
            recordDailyStatsLocked();
        }
    }

    public void recordDailyStatsLocked() {
        DailyItem item = new DailyItem();
        item.mStartTime = this.mDailyStartTime;
        item.mEndTime = System.currentTimeMillis();
        boolean hasData = false;
        if (this.mDailyDischargeStepTracker.mNumStepDurations > 0) {
            hasData = true;
            item.mDischargeSteps = new LevelStepTracker(this.mDailyDischargeStepTracker.mNumStepDurations, this.mDailyDischargeStepTracker.mStepDurations);
        }
        if (this.mDailyChargeStepTracker.mNumStepDurations > 0) {
            hasData = true;
            item.mChargeSteps = new LevelStepTracker(this.mDailyChargeStepTracker.mNumStepDurations, this.mDailyChargeStepTracker.mStepDurations);
        }
        if (this.mDailyPackageChanges != null) {
            hasData = true;
            item.mPackageChanges = this.mDailyPackageChanges;
            this.mDailyPackageChanges = null;
        }
        this.mDailyDischargeStepTracker.init();
        this.mDailyChargeStepTracker.init();
        updateDailyDeadlineLocked();
        if (hasData) {
            long startTime = SystemClock.uptimeMillis();
            this.mDailyItems.add(item);
            while (this.mDailyItems.size() > 10) {
                this.mDailyItems.remove(0);
            }
            final ByteArrayOutputStream memStream = new ByteArrayOutputStream();
            try {
                XmlSerializer out = new FastXmlSerializer();
                out.setOutput(memStream, StandardCharsets.UTF_8.name());
                writeDailyItemsLocked(out);
                final long initialTime = SystemClock.uptimeMillis() - startTime;
                BackgroundThread.getHandler().post(new Runnable() {
                    public void run() {
                        synchronized (BatteryStatsImpl.this.mCheckinFile) {
                            long startTime2 = SystemClock.uptimeMillis();
                            FileOutputStream stream = null;
                            try {
                                stream = BatteryStatsImpl.this.mDailyFile.startWrite();
                                memStream.writeTo(stream);
                                stream.flush();
                                FileUtils.sync(stream);
                                stream.close();
                                BatteryStatsImpl.this.mDailyFile.finishWrite(stream);
                                EventLogTags.writeCommitSysConfigFile("batterystats-daily", (initialTime + SystemClock.uptimeMillis()) - startTime2);
                            } catch (IOException e) {
                                Slog.w("BatteryStats", "Error writing battery daily items", e);
                                BatteryStatsImpl.this.mDailyFile.failWrite(stream);
                            }
                        }
                    }
                });
            } catch (IOException e) {
            }
        }
    }

    private void writeDailyItemsLocked(XmlSerializer out) throws IOException {
        StringBuilder sb = new StringBuilder(64);
        out.startDocument(null, Boolean.valueOf(true));
        out.startTag(null, "daily-items");
        for (int i = 0; i < this.mDailyItems.size(); i++) {
            DailyItem dit = (DailyItem) this.mDailyItems.get(i);
            out.startTag(null, "item");
            out.attribute(null, "start", Long.toString(dit.mStartTime));
            out.attribute(null, "end", Long.toString(dit.mEndTime));
            writeDailyLevelSteps(out, "dis", dit.mDischargeSteps, sb);
            writeDailyLevelSteps(out, "chg", dit.mChargeSteps, sb);
            if (dit.mPackageChanges != null) {
                for (int j = 0; j < dit.mPackageChanges.size(); j++) {
                    PackageChange pc = (PackageChange) dit.mPackageChanges.get(j);
                    if (pc.mUpdate) {
                        out.startTag(null, "upd");
                        out.attribute(null, "pkg", pc.mPackageName);
                        out.attribute(null, "ver", Long.toString(pc.mVersionCode));
                        out.endTag(null, "upd");
                    } else {
                        out.startTag(null, "rem");
                        out.attribute(null, "pkg", pc.mPackageName);
                        out.endTag(null, "rem");
                    }
                }
            }
            out.endTag(null, "item");
        }
        out.endTag(null, "daily-items");
        out.endDocument();
    }

    private void writeDailyLevelSteps(XmlSerializer out, String tag, LevelStepTracker steps, StringBuilder tmpBuilder) throws IOException {
        if (steps != null) {
            out.startTag(null, tag);
            out.attribute(null, "n", Integer.toString(steps.mNumStepDurations));
            for (int i = 0; i < steps.mNumStepDurations; i++) {
                out.startTag(null, "s");
                tmpBuilder.setLength(0);
                steps.encodeEntryAt(i, tmpBuilder);
                out.attribute(null, "v", tmpBuilder.toString());
                out.endTag(null, "s");
            }
            out.endTag(null, tag);
        }
    }

    public void readDailyStatsLocked() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Reading daily items from ");
        stringBuilder.append(this.mDailyFile.getBaseFile());
        Slog.d(str, stringBuilder.toString());
        this.mDailyItems.clear();
        try {
            FileInputStream stream = this.mDailyFile.openRead();
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(stream, StandardCharsets.UTF_8.name());
                readDailyItemsLocked(parser);
                try {
                    stream.close();
                } catch (IOException e) {
                }
            } catch (XmlPullParserException e2) {
                stream.close();
            } catch (Throwable th) {
                try {
                    stream.close();
                } catch (IOException e3) {
                }
                throw th;
            }
        } catch (FileNotFoundException e4) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:22:0x0056 A:{Catch:{ IllegalStateException -> 0x00d7, NullPointerException -> 0x00bf, NumberFormatException -> 0x00a7, XmlPullParserException -> 0x008f, IOException -> 0x0077, IndexOutOfBoundsException -> 0x005e }} */
    /* JADX WARNING: Removed duplicated region for block: B:6:0x000e A:{Catch:{ IllegalStateException -> 0x00d7, NullPointerException -> 0x00bf, NumberFormatException -> 0x00a7, XmlPullParserException -> 0x008f, IOException -> 0x0077, IndexOutOfBoundsException -> 0x005e }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void readDailyItemsLocked(XmlPullParser parser) {
        int type;
        String str;
        StringBuilder stringBuilder;
        while (true) {
            try {
                int next = parser.next();
                type = next;
                if (next == 2 || type == 1) {
                    if (type != 2) {
                        next = parser.getDepth();
                        while (true) {
                            int next2 = parser.next();
                            type = next2;
                            if (next2 == 1) {
                                return;
                            }
                            if (type == 3 && parser.getDepth() <= next) {
                                return;
                            }
                            if (type != 3) {
                                if (type != 4) {
                                    if (parser.getName().equals("item")) {
                                        readDailyItemTagLocked(parser);
                                    } else {
                                        String str2 = TAG;
                                        StringBuilder stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("Unknown element under <daily-items>: ");
                                        stringBuilder2.append(parser.getName());
                                        Slog.w(str2, stringBuilder2.toString());
                                        XmlUtils.skipCurrentTag(parser);
                                    }
                                }
                            }
                        }
                    } else {
                        throw new IllegalStateException("no start tag found");
                    }
                }
            } catch (IllegalStateException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed parsing daily ");
                stringBuilder.append(e);
                Slog.w(str, stringBuilder.toString());
                return;
            } catch (NullPointerException e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed parsing daily ");
                stringBuilder.append(e2);
                Slog.w(str, stringBuilder.toString());
                return;
            } catch (NumberFormatException e3) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed parsing daily ");
                stringBuilder.append(e3);
                Slog.w(str, stringBuilder.toString());
                return;
            } catch (XmlPullParserException e4) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed parsing daily ");
                stringBuilder.append(e4);
                Slog.w(str, stringBuilder.toString());
                return;
            } catch (IOException e5) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed parsing daily ");
                stringBuilder.append(e5);
                Slog.w(str, stringBuilder.toString());
                return;
            } catch (IndexOutOfBoundsException e6) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Failed parsing daily ");
                stringBuilder.append(e6);
                Slog.w(str, stringBuilder.toString());
                return;
            }
        }
        if (type != 2) {
        }
    }

    void readDailyItemTagLocked(XmlPullParser parser) throws NumberFormatException, XmlPullParserException, IOException {
        DailyItem dit = new DailyItem();
        String attr = parser.getAttributeValue(null, "start");
        if (attr != null) {
            dit.mStartTime = Long.parseLong(attr);
        }
        attr = parser.getAttributeValue(null, "end");
        if (attr != null) {
            dit.mEndTime = Long.parseLong(attr);
        }
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                this.mDailyItems.add(dit);
            } else if (type != 3) {
                if (type != 4) {
                    String tagName = parser.getName();
                    String verStr;
                    if (tagName.equals("dis")) {
                        readDailyItemTagDetailsLocked(parser, dit, false, "dis");
                    } else if (tagName.equals("chg")) {
                        readDailyItemTagDetailsLocked(parser, dit, true, "chg");
                    } else if (tagName.equals("upd")) {
                        if (dit.mPackageChanges == null) {
                            dit.mPackageChanges = new ArrayList();
                        }
                        PackageChange pc = new PackageChange();
                        pc.mUpdate = true;
                        pc.mPackageName = parser.getAttributeValue(null, "pkg");
                        verStr = parser.getAttributeValue(null, "ver");
                        pc.mVersionCode = verStr != null ? Long.parseLong(verStr) : 0;
                        dit.mPackageChanges.add(pc);
                        XmlUtils.skipCurrentTag(parser);
                    } else if (tagName.equals("rem")) {
                        if (dit.mPackageChanges == null) {
                            dit.mPackageChanges = new ArrayList();
                        }
                        PackageChange pc2 = new PackageChange();
                        pc2.mUpdate = false;
                        pc2.mPackageName = parser.getAttributeValue(null, "pkg");
                        dit.mPackageChanges.add(pc2);
                        XmlUtils.skipCurrentTag(parser);
                    } else {
                        verStr = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Unknown element under <item>: ");
                        stringBuilder.append(parser.getName());
                        Slog.w(verStr, stringBuilder.toString());
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
            }
        }
        this.mDailyItems.add(dit);
    }

    void readDailyItemTagDetailsLocked(XmlPullParser parser, DailyItem dit, boolean isCharge, String tag) throws NumberFormatException, XmlPullParserException, IOException {
        String numAttr = parser.getAttributeValue(null, "n");
        if (numAttr == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Missing 'n' attribute at ");
            stringBuilder.append(parser.getPositionDescription());
            Slog.w(str, stringBuilder.toString());
            XmlUtils.skipCurrentTag(parser);
            return;
        }
        int num = Integer.parseInt(numAttr);
        LevelStepTracker steps = new LevelStepTracker(num);
        if (isCharge) {
            dit.mChargeSteps = steps;
        } else {
            dit.mDischargeSteps = steps;
        }
        int i = 0;
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                steps.mNumStepDurations = i;
            } else if (type != 3) {
                if (type != 4) {
                    String str2;
                    if (!"s".equals(parser.getName())) {
                        str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Unknown element under <");
                        stringBuilder2.append(tag);
                        stringBuilder2.append(">: ");
                        stringBuilder2.append(parser.getName());
                        Slog.w(str2, stringBuilder2.toString());
                        XmlUtils.skipCurrentTag(parser);
                    } else if (i < num) {
                        str2 = parser.getAttributeValue(null, "v");
                        if (str2 != null) {
                            steps.decodeEntryAt(i, str2);
                            i++;
                        }
                    }
                }
            }
        }
        steps.mNumStepDurations = i;
    }

    public DailyItem getDailyItemLocked(int daysAgo) {
        int index = (this.mDailyItems.size() - 1) - daysAgo;
        return index >= 0 ? (DailyItem) this.mDailyItems.get(index) : null;
    }

    public long getCurrentDailyStartTime() {
        return this.mDailyStartTime;
    }

    public long getNextMinDailyDeadline() {
        return this.mNextMinDailyDeadline;
    }

    public long getNextMaxDailyDeadline() {
        return this.mNextMaxDailyDeadline;
    }

    public boolean startIteratingOldHistoryLocked() {
        HistoryItem historyItem = this.mHistory;
        this.mHistoryIterator = historyItem;
        if (historyItem == null) {
            return false;
        }
        this.mHistoryBuffer.setDataPosition(0);
        this.mHistoryReadTmp.clear();
        this.mReadOverflow = false;
        this.mIteratingHistory = true;
        return true;
    }

    public boolean getNextOldHistoryLocked(HistoryItem out) {
        boolean end = this.mHistoryBuffer.dataPosition() >= this.mHistoryBuffer.dataSize();
        if (!end) {
            readHistoryDelta(this.mHistoryBuffer, this.mHistoryReadTmp);
            this.mReadOverflow |= this.mHistoryReadTmp.cmd == (byte) 6 ? 1 : 0;
        }
        HistoryItem cur = this.mHistoryIterator;
        if (cur == null) {
            if (!(this.mReadOverflow || end)) {
                Slog.w(TAG, "Old history ends before new history!");
            }
            return false;
        }
        out.setTo(cur);
        this.mHistoryIterator = cur.next;
        if (!this.mReadOverflow) {
            if (end) {
                Slog.w(TAG, "New history ends before old history!");
            } else if (!out.same(this.mHistoryReadTmp)) {
                PrintWriter pw = new FastPrintWriter(new LogWriter(5, TAG));
                pw.println("Histories differ!");
                pw.println("Old history:");
                PrintWriter printWriter = pw;
                new HistoryPrinter().printNextItem(printWriter, out, 0, false, true);
                pw.println("New history:");
                new HistoryPrinter().printNextItem(printWriter, this.mHistoryReadTmp, 0, false, true);
                pw.flush();
            }
        }
        return true;
    }

    public void finishIteratingOldHistoryLocked() {
        this.mIteratingHistory = false;
        this.mHistoryBuffer.setDataPosition(this.mHistoryBuffer.dataSize());
        this.mHistoryIterator = null;
    }

    public int getHistoryTotalSize() {
        return MAX_HISTORY_BUFFER;
    }

    public int getHistoryUsedSize() {
        return this.mHistoryBuffer.dataSize();
    }

    public boolean startIteratingHistoryLocked() {
        if (this.mHistoryBuffer.dataSize() <= 0) {
            return false;
        }
        this.mHistoryBuffer.setDataPosition(0);
        this.mReadOverflow = false;
        this.mIteratingHistory = true;
        this.mReadHistoryStrings = new String[this.mHistoryTagPool.size()];
        this.mReadHistoryUids = new int[this.mHistoryTagPool.size()];
        this.mReadHistoryChars = 0;
        for (Entry<HistoryTag, Integer> ent : this.mHistoryTagPool.entrySet()) {
            HistoryTag tag = (HistoryTag) ent.getKey();
            int idx = ((Integer) ent.getValue()).intValue();
            this.mReadHistoryStrings[idx] = tag.string;
            this.mReadHistoryUids[idx] = tag.uid;
            this.mReadHistoryChars += tag.string.length() + 1;
        }
        return true;
    }

    public int getHistoryStringPoolSize() {
        return this.mReadHistoryStrings.length;
    }

    public int getHistoryStringPoolBytes() {
        return (this.mReadHistoryStrings.length * 12) + (this.mReadHistoryChars * 2);
    }

    public String getHistoryTagPoolString(int index) {
        return this.mReadHistoryStrings[index];
    }

    public int getHistoryTagPoolUid(int index) {
        return this.mReadHistoryUids[index];
    }

    public boolean getNextHistoryLocked(HistoryItem out) {
        int pos = this.mHistoryBuffer.dataPosition();
        if (pos == 0) {
            out.clear();
        }
        if (pos >= this.mHistoryBuffer.dataSize()) {
            return false;
        }
        long lastRealtime = out.time;
        long lastWalltime = out.currentTime;
        readHistoryDelta(this.mHistoryBuffer, out);
        if (!(out.cmd == (byte) 5 || out.cmd == (byte) 7 || lastWalltime == 0)) {
            out.currentTime = (out.time - lastRealtime) + lastWalltime;
        }
        return true;
    }

    public void finishIteratingHistoryLocked() {
        this.mIteratingHistory = false;
        this.mHistoryBuffer.setDataPosition(this.mHistoryBuffer.dataSize());
        this.mReadHistoryStrings = null;
    }

    public long getHistoryBaseTime() {
        return this.mHistoryBaseTime;
    }

    public int getStartCount() {
        return this.mStartCount;
    }

    public boolean isOnBattery() {
        return this.mOnBattery;
    }

    public boolean isCharging() {
        return this.mCharging;
    }

    public boolean isScreenOn() {
        return this.mScreenState == 2;
    }

    public boolean isScreenOn(int state) {
        return state == 2 || state == 5 || state == 6;
    }

    public boolean isScreenOff(int state) {
        return state == 1;
    }

    public boolean isScreenDoze(int state) {
        return state == 3 || state == 4;
    }

    void initTimes(long uptime, long realtime) {
        this.mStartClockTime = System.currentTimeMillis();
        this.mOnBatteryTimeBase.init(uptime, realtime);
        this.mOnBatteryScreenOffTimeBase.init(uptime, realtime);
        this.mRealtime = 0;
        this.mUptime = 0;
        this.mRealtimeStart = realtime;
        this.mUptimeStart = uptime;
    }

    void initDischarge() {
        this.mLowDischargeAmountSinceCharge = 0;
        this.mHighDischargeAmountSinceCharge = 0;
        this.mDischargeAmountScreenOn = 0;
        this.mDischargeAmountScreenOnSinceCharge = 0;
        this.mDischargeAmountScreenOff = 0;
        this.mDischargeAmountScreenOffSinceCharge = 0;
        this.mDischargeAmountScreenDoze = 0;
        this.mDischargeAmountScreenDozeSinceCharge = 0;
        this.mDischargeStepTracker.init();
        this.mChargeStepTracker.init();
        this.mDischargeScreenOffCounter.reset(false);
        this.mDischargeScreenDozeCounter.reset(false);
        this.mDischargeLightDozeCounter.reset(false);
        this.mDischargeDeepDozeCounter.reset(false);
        this.mDischargeCounter.reset(false);
    }

    public void resetAllStatsCmdLocked() {
        resetAllStatsLocked();
        long mSecUptime = this.mClocks.uptimeMillis();
        long uptime = mSecUptime * 1000;
        long mSecRealtime = this.mClocks.elapsedRealtime();
        long realtime = 1000 * mSecRealtime;
        this.mDischargeStartLevel = this.mHistoryCur.batteryLevel;
        pullPendingStateUpdatesLocked();
        addHistoryRecordLocked(mSecRealtime, mSecUptime);
        byte b = this.mHistoryCur.batteryLevel;
        this.mCurrentBatteryLevel = b;
        this.mDischargePlugLevel = b;
        this.mDischargeUnplugLevel = b;
        this.mDischargeCurrentLevel = b;
        this.mOnBatteryTimeBase.reset(uptime, realtime);
        this.mOnBatteryScreenOffTimeBase.reset(uptime, realtime);
        if ((this.mHistoryCur.states & 524288) == 0) {
            if (isScreenOn(this.mScreenState)) {
                this.mDischargeScreenOnUnplugLevel = this.mHistoryCur.batteryLevel;
                this.mDischargeScreenDozeUnplugLevel = 0;
                this.mDischargeScreenOffUnplugLevel = 0;
            } else if (isScreenDoze(this.mScreenState)) {
                this.mDischargeScreenOnUnplugLevel = 0;
                this.mDischargeScreenDozeUnplugLevel = this.mHistoryCur.batteryLevel;
                this.mDischargeScreenOffUnplugLevel = 0;
            } else {
                this.mDischargeScreenOnUnplugLevel = 0;
                this.mDischargeScreenDozeUnplugLevel = 0;
                this.mDischargeScreenOffUnplugLevel = this.mHistoryCur.batteryLevel;
            }
            this.mDischargeAmountScreenOn = 0;
            this.mDischargeAmountScreenOff = 0;
            this.mDischargeAmountScreenDoze = 0;
        }
        initActiveHistoryEventsLocked(mSecRealtime, mSecUptime);
    }

    private void resetAllStatsLocked() {
        int i;
        long uptimeMillis = this.mClocks.uptimeMillis();
        long elapsedRealtimeMillis = this.mClocks.elapsedRealtime();
        this.mStartCount = 0;
        initTimes(uptimeMillis * 1000, elapsedRealtimeMillis * 1000);
        this.mScreenOnTimer.reset(false);
        this.mScreenDozeTimer.reset(false);
        for (i = 0; i < 5; i++) {
            this.mScreenBrightnessTimer[i].reset(false);
        }
        if (this.mPowerProfile != null) {
            this.mEstimatedBatteryCapacity = (int) this.mPowerProfile.getBatteryCapacity();
        } else {
            this.mEstimatedBatteryCapacity = -1;
        }
        this.mMinLearnedBatteryCapacity = -1;
        this.mMaxLearnedBatteryCapacity = -1;
        this.mInteractiveTimer.reset(false);
        this.mPowerSaveModeEnabledTimer.reset(false);
        this.mLastIdleTimeStart = elapsedRealtimeMillis;
        this.mLongestLightIdleTime = 0;
        this.mLongestFullIdleTime = 0;
        this.mDeviceIdleModeLightTimer.reset(false);
        this.mDeviceIdleModeFullTimer.reset(false);
        this.mDeviceLightIdlingTimer.reset(false);
        this.mDeviceIdlingTimer.reset(false);
        this.mPhoneOnTimer.reset(false);
        this.mAudioOnTimer.reset(false);
        this.mVideoOnTimer.reset(false);
        this.mFlashlightOnTimer.reset(false);
        this.mCameraOnTimer.reset(false);
        this.mBluetoothScanTimer.reset(false);
        for (i = 0; i < 5; i++) {
            this.mPhoneSignalStrengthsTimer[i].reset(false);
        }
        this.mPhoneSignalScanningTimer.reset(false);
        for (i = 0; i < 21; i++) {
            this.mPhoneDataConnectionsTimer[i].reset(false);
        }
        for (i = 0; i < 10; i++) {
            this.mNetworkByteActivityCounters[i].reset(false);
            this.mNetworkPacketActivityCounters[i].reset(false);
        }
        this.mMobileRadioActiveTimer.reset(false);
        this.mMobileRadioActivePerAppTimer.reset(false);
        this.mMobileRadioActiveAdjustedTime.reset(false);
        this.mMobileRadioActiveUnknownTime.reset(false);
        this.mMobileRadioActiveUnknownCount.reset(false);
        this.mWifiOnTimer.reset(false);
        this.mGlobalWifiRunningTimer.reset(false);
        for (i = 0; i < 8; i++) {
            this.mWifiStateTimer[i].reset(false);
        }
        for (i = 0; i < 13; i++) {
            this.mWifiSupplStateTimer[i].reset(false);
        }
        for (i = 0; i < 5; i++) {
            this.mWifiSignalStrengthsTimer[i].reset(false);
        }
        this.mWifiMulticastWakelockTimer.reset(false);
        this.mWifiActiveTimer.reset(false);
        this.mWifiActivity.reset(false);
        for (i = 0; i < 2; i++) {
            this.mGpsSignalQualityTimer[i].reset(false);
        }
        this.mBluetoothActivity.reset(false);
        this.mModemActivity.reset(false);
        this.mUnpluggedNumConnectivityChange = 0;
        this.mLoadedNumConnectivityChange = 0;
        this.mNumConnectivityChange = 0;
        i = 0;
        while (i < this.mUidStats.size()) {
            if (((Uid) this.mUidStats.valueAt(i)).reset(uptimeMillis * 1000, elapsedRealtimeMillis * 1000)) {
                this.mUidStats.remove(this.mUidStats.keyAt(i));
                i--;
            }
            i++;
        }
        if (this.mRpmStats.size() > 0) {
            for (SamplingTimer timer : this.mRpmStats.values()) {
                this.mOnBatteryTimeBase.remove(timer);
            }
            this.mRpmStats.clear();
        }
        if (this.mScreenOffRpmStats.size() > 0) {
            for (SamplingTimer timer2 : this.mScreenOffRpmStats.values()) {
                this.mOnBatteryScreenOffTimeBase.remove(timer2);
            }
            this.mScreenOffRpmStats.clear();
        }
        if (this.mKernelWakelockStats.size() > 0) {
            for (SamplingTimer timer22 : this.mKernelWakelockStats.values()) {
                this.mOnBatteryScreenOffTimeBase.remove(timer22);
            }
            this.mKernelWakelockStats.clear();
        }
        if (this.mKernelMemoryStats.size() > 0) {
            for (int i2 = 0; i2 < this.mKernelMemoryStats.size(); i2++) {
                this.mOnBatteryTimeBase.remove((TimeBaseObs) this.mKernelMemoryStats.valueAt(i2));
            }
            this.mKernelMemoryStats.clear();
        }
        if (this.mWakeupReasonStats.size() > 0) {
            for (SamplingTimer timer222 : this.mWakeupReasonStats.values()) {
                this.mOnBatteryTimeBase.remove(timer222);
            }
            this.mWakeupReasonStats.clear();
        }
        this.mLastHistoryStepDetails = null;
        this.mLastStepCpuSystemTime = 0;
        this.mLastStepCpuUserTime = 0;
        this.mCurStepCpuSystemTime = 0;
        this.mCurStepCpuUserTime = 0;
        this.mCurStepCpuUserTime = 0;
        this.mLastStepCpuUserTime = 0;
        this.mCurStepCpuSystemTime = 0;
        this.mLastStepCpuSystemTime = 0;
        this.mCurStepStatUserTime = 0;
        this.mLastStepStatUserTime = 0;
        this.mCurStepStatSystemTime = 0;
        this.mLastStepStatSystemTime = 0;
        this.mCurStepStatIOWaitTime = 0;
        this.mLastStepStatIOWaitTime = 0;
        this.mCurStepStatIrqTime = 0;
        this.mLastStepStatIrqTime = 0;
        this.mCurStepStatSoftIrqTime = 0;
        this.mLastStepStatSoftIrqTime = 0;
        this.mCurStepStatIdleTime = 0;
        this.mLastStepStatIdleTime = 0;
        this.mNumAllUidCpuTimeReads = 0;
        this.mNumUidsRemoved = 0;
        initDischarge();
        clearHistoryLocked();
        HwLog.dubaie("DUBAI_TAG_RESET_BATTERY_STAT", "");
        this.mHandler.sendEmptyMessage(4);
    }

    private void initActiveHistoryEventsLocked(long elapsedRealtimeMs, long uptimeMs) {
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 < 22) {
                if (this.mRecordAllHistory || i2 != 1) {
                    HashMap<String, SparseIntArray> active = this.mActiveEvents.getStateForEvent(i2);
                    if (active != null) {
                        for (Entry<String, SparseIntArray> ent : active.entrySet()) {
                            SparseIntArray uids = (SparseIntArray) ent.getValue();
                            i = 0;
                            while (true) {
                                int j = i;
                                if (j >= uids.size()) {
                                    break;
                                }
                                addHistoryEventLocked(elapsedRealtimeMs, uptimeMs, i2, (String) ent.getKey(), uids.keyAt(j));
                                i = j + 1;
                            }
                        }
                    }
                }
                i = i2 + 1;
            } else {
                return;
            }
        }
    }

    void updateDischargeScreenLevelsLocked(int oldState, int newState) {
        updateOldDischargeScreenLevelLocked(oldState);
        updateNewDischargeScreenLevelLocked(newState);
    }

    private void updateOldDischargeScreenLevelLocked(int state) {
        int diff;
        if (isScreenOn(state)) {
            diff = this.mDischargeScreenOnUnplugLevel - this.mDischargeCurrentLevel;
            if (diff > 0) {
                this.mDischargeAmountScreenOn += diff;
                this.mDischargeAmountScreenOnSinceCharge += diff;
            }
        } else if (isScreenDoze(state)) {
            diff = this.mDischargeScreenDozeUnplugLevel - this.mDischargeCurrentLevel;
            if (diff > 0) {
                this.mDischargeAmountScreenDoze += diff;
                this.mDischargeAmountScreenDozeSinceCharge += diff;
            }
        } else if (isScreenOff(state)) {
            diff = this.mDischargeScreenOffUnplugLevel - this.mDischargeCurrentLevel;
            if (diff > 0) {
                this.mDischargeAmountScreenOff += diff;
                this.mDischargeAmountScreenOffSinceCharge += diff;
            }
        }
    }

    private void updateNewDischargeScreenLevelLocked(int state) {
        if (isScreenOn(state)) {
            this.mDischargeScreenOnUnplugLevel = this.mDischargeCurrentLevel;
            this.mDischargeScreenOffUnplugLevel = 0;
            this.mDischargeScreenDozeUnplugLevel = 0;
        } else if (isScreenDoze(state)) {
            this.mDischargeScreenOnUnplugLevel = 0;
            this.mDischargeScreenDozeUnplugLevel = this.mDischargeCurrentLevel;
            this.mDischargeScreenOffUnplugLevel = 0;
        } else if (isScreenOff(state)) {
            this.mDischargeScreenOnUnplugLevel = 0;
            this.mDischargeScreenDozeUnplugLevel = 0;
            this.mDischargeScreenOffUnplugLevel = this.mDischargeCurrentLevel;
        }
    }

    public void pullPendingStateUpdatesLocked() {
        if (this.mOnBatteryInternal) {
            updateDischargeScreenLevelsLocked(this.mScreenState, this.mScreenState);
        }
    }

    private NetworkStats readNetworkStatsLocked(String[] ifaces) {
        try {
            if (!ArrayUtils.isEmpty((Object[]) ifaces)) {
                return this.mNetworkStatsFactory.readNetworkStatsDetail(-1, ifaces, 0, (NetworkStats) this.mNetworkStatsPool.acquire());
            }
        } catch (IOException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("failed to read network stats for ifaces: ");
            stringBuilder.append(Arrays.toString(ifaces));
            Slog.e(str, stringBuilder.toString());
        }
        return null;
    }

    /* JADX WARNING: Missing block: B:14:0x0035, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:82:0x02dd, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateWifiState(WifiActivityEnergyInfo info) {
        Throwable th;
        NetworkStats delta = null;
        synchronized (this.mWifiNetworkLock) {
            NetworkStats latestStats = readNetworkStatsLocked(this.mWifiIfaces);
            if (latestStats != null) {
                delta = NetworkStats.subtract(latestStats, this.mLastWifiNetworkStats, null, null, (NetworkStats) this.mNetworkStatsPool.acquire());
                this.mNetworkStatsPool.release(this.mLastWifiNetworkStats);
                this.mLastWifiNetworkStats = latestStats;
            }
        }
        synchronized (this) {
            try {
                if (this.mOnBatteryInternal) {
                    long totalRxPackets;
                    long elapsedRealtimeMs;
                    long elapsedRealtimeMs2 = this.mClocks.elapsedRealtime();
                    SparseLongArray rxPackets = new SparseLongArray();
                    SparseLongArray txPackets = new SparseLongArray();
                    long totalTxPackets = 0;
                    if (delta != null) {
                        NetworkStats.Entry entry = new NetworkStats.Entry();
                        int size = delta.size();
                        totalRxPackets = 0;
                        long totalTxPackets2 = 0;
                        int i = 0;
                        while (i < size) {
                            entry = delta.getValues(i, entry);
                            if (entry.rxBytes == 0 && entry.txBytes == 0) {
                                elapsedRealtimeMs = elapsedRealtimeMs2;
                            } else {
                                Uid u = getUidStatsLocked(mapUid(entry.uid));
                                if (entry.rxBytes != 0) {
                                    elapsedRealtimeMs = elapsedRealtimeMs2;
                                    u.noteNetworkActivityLocked(2, entry.rxBytes, entry.rxPackets);
                                    if (entry.set == 0) {
                                        u.noteNetworkActivityLocked(8, entry.rxBytes, entry.rxPackets);
                                    }
                                    this.mNetworkByteActivityCounters[2].addCountLocked(entry.rxBytes);
                                    this.mNetworkPacketActivityCounters[2].addCountLocked(entry.rxPackets);
                                    rxPackets.put(u.getUid(), entry.rxPackets);
                                    totalRxPackets += entry.rxPackets;
                                } else {
                                    elapsedRealtimeMs = elapsedRealtimeMs2;
                                }
                                if (entry.txBytes != 0) {
                                    u.noteNetworkActivityLocked(3, entry.txBytes, entry.txPackets);
                                    if (entry.set == 0) {
                                        u.noteNetworkActivityLocked(9, entry.txBytes, entry.txPackets);
                                    }
                                    this.mNetworkByteActivityCounters[3].addCountLocked(entry.txBytes);
                                    this.mNetworkPacketActivityCounters[3].addCountLocked(entry.txPackets);
                                    txPackets.put(u.getUid(), entry.txPackets);
                                    totalTxPackets2 += entry.txPackets;
                                }
                            }
                            i++;
                            elapsedRealtimeMs2 = elapsedRealtimeMs;
                        }
                        elapsedRealtimeMs = elapsedRealtimeMs2;
                        this.mNetworkStatsPool.release(delta);
                        delta = null;
                        totalTxPackets = totalTxPackets2;
                    } else {
                        elapsedRealtimeMs = elapsedRealtimeMs2;
                        totalRxPackets = 0;
                    }
                    if (info != null) {
                        NetworkStats delta2;
                        try {
                            long rxTimeMs;
                            long txTimeMs;
                            long myTxTimeMs;
                            this.mHasWifiReporting = true;
                            long txTimeMs2 = info.getControllerTxTimeMillis();
                            long rxTimeMs2 = info.getControllerRxTimeMillis();
                            long scanTimeMs = info.getControllerScanTimeMillis();
                            long idleTimeMs = info.getControllerIdleTimeMillis();
                            long totalTimeMs = (txTimeMs2 + rxTimeMs2) + idleTimeMs;
                            long leftOverRxTimeMs = rxTimeMs2;
                            long leftOverTxTimeMs = txTimeMs2;
                            long totalScanTimeMs = 0;
                            int uidStatsSize = this.mUidStats.size();
                            long totalWifiLockTimeMs = 0;
                            int i2 = 0;
                            while (true) {
                                int i3 = i2;
                                if (i3 >= uidStatsSize) {
                                    break;
                                }
                                delta2 = delta;
                                Uid delta3 = (Uid) this.mUidStats.valueAt(i3);
                                totalScanTimeMs += delta3.mWifiScanTimer.getTimeSinceMarkLocked(elapsedRealtimeMs * 1000) / 1000;
                                totalWifiLockTimeMs += delta3.mFullWifiLockTimer.getTimeSinceMarkLocked(elapsedRealtimeMs * 1000) / 1000;
                                i2 = i3 + 1;
                                delta = delta2;
                                scanTimeMs = scanTimeMs;
                                totalRxPackets = totalRxPackets;
                            }
                            long j = scanTimeMs;
                            long totalRxPackets2 = totalRxPackets;
                            int i4 = 0;
                            while (i4 < uidStatsSize) {
                                int uidStatsSize2;
                                Uid uid = (Uid) this.mUidStats.valueAt(i4);
                                scanTimeMs = uid.mWifiScanTimer.getTimeSinceMarkLocked(elapsedRealtimeMs * 1000) / 1000;
                                if (scanTimeMs > 0) {
                                    uidStatsSize2 = uidStatsSize;
                                    uidStatsSize = elapsedRealtimeMs;
                                    uid.mWifiScanTimer.setMark(uidStatsSize);
                                    totalRxPackets = scanTimeMs;
                                    elapsedRealtimeMs = scanTimeMs;
                                    if (totalScanTimeMs > rxTimeMs2) {
                                        totalRxPackets = (rxTimeMs2 * totalRxPackets) / totalScanTimeMs;
                                    }
                                    rxTimeMs = rxTimeMs2;
                                    rxTimeMs2 = totalRxPackets;
                                    if (totalScanTimeMs > txTimeMs2) {
                                        elapsedRealtimeMs = (txTimeMs2 * elapsedRealtimeMs) / totalScanTimeMs;
                                    }
                                    txTimeMs = txTimeMs2;
                                    txTimeMs2 = elapsedRealtimeMs;
                                    ControllerActivityCounterImpl activityCounter = uid.getOrCreateWifiControllerActivityLocked();
                                    activityCounter.getRxTimeCounter().addCountLocked(rxTimeMs2);
                                    activityCounter.getTxTimeCounters()[0].addCountLocked(txTimeMs2);
                                    leftOverRxTimeMs -= rxTimeMs2;
                                    leftOverTxTimeMs -= txTimeMs2;
                                } else {
                                    uidStatsSize2 = uidStatsSize;
                                    txTimeMs = txTimeMs2;
                                    rxTimeMs = rxTimeMs2;
                                    long j2 = scanTimeMs;
                                    uidStatsSize = elapsedRealtimeMs;
                                }
                                txTimeMs2 = uid.mFullWifiLockTimer.getTimeSinceMarkLocked(uidStatsSize * 1000) / 1000;
                                if (txTimeMs2 > 0) {
                                    uid.mFullWifiLockTimer.setMark(uidStatsSize);
                                    uid.getOrCreateWifiControllerActivityLocked().getIdleTimeCounter().addCountLocked((txTimeMs2 * idleTimeMs) / totalWifiLockTimeMs);
                                }
                                i4++;
                                elapsedRealtimeMs = uidStatsSize;
                                uidStatsSize = uidStatsSize2;
                                rxTimeMs2 = rxTimeMs;
                                txTimeMs2 = txTimeMs;
                            }
                            txTimeMs = txTimeMs2;
                            rxTimeMs = rxTimeMs2;
                            for (i4 = 0; i4 < txPackets.size(); i4++) {
                                myTxTimeMs = (txPackets.valueAt(i4) * leftOverTxTimeMs) / totalTxPackets;
                                getUidStatsLocked(txPackets.keyAt(i4)).getOrCreateWifiControllerActivityLocked().getTxTimeCounters()[0].addCountLocked(myTxTimeMs);
                            }
                            for (i4 = 0; i4 < rxPackets.size(); i4++) {
                                myTxTimeMs = (rxPackets.valueAt(i4) * leftOverRxTimeMs) / totalRxPackets2;
                                getUidStatsLocked(rxPackets.keyAt(i4)).getOrCreateWifiControllerActivityLocked().getRxTimeCounter().addCountLocked(myTxTimeMs);
                            }
                            this.mWifiActivity.getRxTimeCounter().addCountLocked(info.getControllerRxTimeMillis());
                            this.mWifiActivity.getTxTimeCounters()[0].addCountLocked(info.getControllerTxTimeMillis());
                            this.mWifiActivity.getScanTimeCounter().addCountLocked(info.getControllerScanTimeMillis());
                            this.mWifiActivity.getIdleTimeCounter().addCountLocked(info.getControllerIdleTimeMillis());
                            double opVolt = this.mPowerProfile.getAveragePower(PowerProfile.POWER_WIFI_CONTROLLER_OPERATING_VOLTAGE) / 1000.0d;
                            if (opVolt != 0.0d) {
                                this.mWifiActivity.getPowerCounter().addCountLocked((long) (((double) info.getControllerEnergyUsed()) / opVolt));
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            delta = delta2;
                            throw th;
                        }
                    }
                } else if (delta != null) {
                    this.mNetworkStatsPool.release(delta);
                }
            } catch (Throwable th3) {
                th = th3;
                throw th;
            }
        }
    }

    private ModemActivityInfo getDeltaModemActivityInfo(ModemActivityInfo activityInfo) {
        if (activityInfo == null) {
            return null;
        }
        int[] txTimeMs = new int[5];
        for (int i = 0; i < 5; i++) {
            txTimeMs[i] = activityInfo.getTxTimeMillis()[i] - this.mLastModemActivityInfo.getTxTimeMillis()[i];
        }
        ModemActivityInfo modemActivityInfo = new ModemActivityInfo(activityInfo.getTimestamp(), activityInfo.getSleepTimeMillis() - this.mLastModemActivityInfo.getSleepTimeMillis(), activityInfo.getIdleTimeMillis() - this.mLastModemActivityInfo.getIdleTimeMillis(), txTimeMs, activityInfo.getRxTimeMillis() - this.mLastModemActivityInfo.getRxTimeMillis(), activityInfo.getEnergyUsed() - this.mLastModemActivityInfo.getEnergyUsed());
        this.mLastModemActivityInfo = activityInfo;
        return modemActivityInfo;
    }

    /* JADX WARNING: Missing block: B:11:0x0035, code skipped:
            monitor-enter(r47);
     */
    /* JADX WARNING: Missing block: B:14:0x0038, code skipped:
            if (r1.mOnBatteryInternal != false) goto L_0x0049;
     */
    /* JADX WARNING: Missing block: B:15:0x003a, code skipped:
            if (r3 == null) goto L_0x0047;
     */
    /* JADX WARNING: Missing block: B:17:?, code skipped:
            r1.mNetworkStatsPool.release(r3);
     */
    /* JADX WARNING: Missing block: B:18:0x0042, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:19:0x0043, code skipped:
            r44 = r2;
     */
    /* JADX WARNING: Missing block: B:20:0x0047, code skipped:
            monitor-exit(r47);
     */
    /* JADX WARNING: Missing block: B:21:0x0048, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:23:0x004c, code skipped:
            if (r2 == null) goto L_0x00fa;
     */
    /* JADX WARNING: Missing block: B:24:0x004e, code skipped:
            r1.mHasModemReporting = true;
            r1.mModemActivity.getIdleTimeCounter().addCountLocked((long) r2.getIdleTimeMillis());
            r1.mModemActivity.getSleepTimeCounter().addCountLocked((long) r2.getSleepTimeMillis());
            r1.mModemActivity.getRxTimeCounter().addCountLocked((long) r2.getRxTimeMillis());
            r6 = 0;
     */
    /* JADX WARNING: Missing block: B:25:0x007b, code skipped:
            if (r6 >= 5) goto L_0x0092;
     */
    /* JADX WARNING: Missing block: B:26:0x007d, code skipped:
            r1.mModemActivity.getTxTimeCounters()[r6].addCountLocked((long) r2.getTxTimeMillis()[r6]);
            r6 = r6 + 1;
     */
    /* JADX WARNING: Missing block: B:28:0x00a4, code skipped:
            if ((r1.mPowerProfile.getAveragePower(com.android.internal.os.PowerProfile.POWER_MODEM_CONTROLLER_OPERATING_VOLTAGE) / 1000.0d) == 0.0d) goto L_0x00fa;
     */
    /* JADX WARNING: Missing block: B:29:0x00a6, code skipped:
            r8 = ((((double) r2.getSleepTimeMillis()) * r1.mPowerProfile.getAveragePower(com.android.internal.os.PowerProfile.POWER_MODEM_CONTROLLER_SLEEP)) + (((double) r2.getIdleTimeMillis()) * r1.mPowerProfile.getAveragePower(com.android.internal.os.PowerProfile.POWER_MODEM_CONTROLLER_IDLE))) + (((double) r2.getRxTimeMillis()) * r1.mPowerProfile.getAveragePower(com.android.internal.os.PowerProfile.POWER_MODEM_CONTROLLER_RX));
            r10 = r2.getTxTimeMillis();
            r11 = r8;
            r8 = 0;
     */
    /* JADX WARNING: Missing block: B:31:0x00de, code skipped:
            if (r8 >= java.lang.Math.min(r10.length, 5)) goto L_0x00f0;
     */
    /* JADX WARNING: Missing block: B:32:0x00e0, code skipped:
            r11 = r11 + (((double) r10[r8]) * r1.mPowerProfile.getAveragePower(com.android.internal.os.PowerProfile.POWER_MODEM_CONTROLLER_TX, r8));
            r8 = r8 + 1;
     */
    /* JADX WARNING: Missing block: B:33:0x00f0, code skipped:
            r1.mModemActivity.getPowerCounter().addCountLocked((long) r11);
     */
    /* JADX WARNING: Missing block: B:35:?, code skipped:
            r6 = r1.mClocks.elapsedRealtime();
            r8 = r1.mMobileRadioActivePerAppTimer.getTimeSinceMarkLocked(1000 * r6);
            r1.mMobileRadioActivePerAppTimer.setMark(r6);
     */
    /* JADX WARNING: Missing block: B:36:0x0112, code skipped:
            if (r3 == null) goto L_0x029d;
     */
    /* JADX WARNING: Missing block: B:37:0x0114, code skipped:
            r14 = new android.net.NetworkStats.Entry();
            r15 = r3.size();
     */
    /* JADX WARNING: Missing block: B:38:0x011d, code skipped:
            r16 = 0;
            r11 = 0;
            r10 = 0;
     */
    /* JADX WARNING: Missing block: B:40:0x0123, code skipped:
            if (r10 >= r15) goto L_0x01c1;
     */
    /* JADX WARNING: Missing block: B:42:?, code skipped:
            r14 = r3.getValues(r10, r14);
            r20 = r6;
     */
    /* JADX WARNING: Missing block: B:43:0x0130, code skipped:
            if (r14.rxPackets != 0) goto L_0x013c;
     */
    /* JADX WARNING: Missing block: B:45:0x0136, code skipped:
            if (r14.txPackets != 0) goto L_0x013c;
     */
    /* JADX WARNING: Missing block: B:47:0x013c, code skipped:
            r11 = r11 + r14.rxPackets;
            r16 = r16 + r14.txPackets;
            r22 = r1.getUidStatsLocked(r1.mapUid(r14.uid));
     */
    /* JADX WARNING: Missing block: B:49:?, code skipped:
            r22.noteNetworkActivityLocked(0, r14.rxBytes, r14.rxPackets);
            r22.noteNetworkActivityLocked(1, r14.txBytes, r14.txPackets);
     */
    /* JADX WARNING: Missing block: B:50:0x016b, code skipped:
            if (r14.set != 0) goto L_0x018b;
     */
    /* JADX WARNING: Missing block: B:51:0x016d, code skipped:
            r22.noteNetworkActivityLocked(6, r14.rxBytes, r14.rxPackets);
            r22.noteNetworkActivityLocked(7, r14.txBytes, r14.txPackets);
     */
    /* JADX WARNING: Missing block: B:52:0x018b, code skipped:
            r1 = r47;
     */
    /* JADX WARNING: Missing block: B:54:?, code skipped:
            r1.mNetworkByteActivityCounters[0].addCountLocked(r14.rxBytes);
            r1.mNetworkByteActivityCounters[1].addCountLocked(r14.txBytes);
            r1.mNetworkPacketActivityCounters[0].addCountLocked(r14.rxPackets);
            r1.mNetworkPacketActivityCounters[1].addCountLocked(r14.txPackets);
     */
    /* JADX WARNING: Missing block: B:55:0x01b3, code skipped:
            r10 = r10 + 1;
            r6 = r20;
     */
    /* JADX WARNING: Missing block: B:56:0x01ba, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:57:0x01bb, code skipped:
            r44 = r2;
     */
    /* JADX WARNING: Missing block: B:58:0x01bd, code skipped:
            r1 = r47;
     */
    /* JADX WARNING: Missing block: B:59:0x01c1, code skipped:
            r20 = r6;
            r5 = r11 + r16;
     */
    /* JADX WARNING: Missing block: B:60:0x01c7, code skipped:
            if (r5 <= 0) goto L_0x027b;
     */
    /* JADX WARNING: Missing block: B:61:0x01c9, code skipped:
            r0 = 0;
     */
    /* JADX WARNING: Missing block: B:62:0x01ca, code skipped:
            if (r0 >= r15) goto L_0x0276;
     */
    /* JADX WARNING: Missing block: B:64:?, code skipped:
            r14 = r3.getValues(r0, r14);
            r36 = r5;
     */
    /* JADX WARNING: Missing block: B:66:0x01d7, code skipped:
            if (r14.rxPackets != 0) goto L_0x01e8;
     */
    /* JADX WARNING: Missing block: B:70:0x01dd, code skipped:
            if (r14.txPackets != 0) goto L_0x01e8;
     */
    /* JADX WARNING: Missing block: B:71:0x01df, code skipped:
            r38 = r0;
            r44 = r2;
            r5 = r36;
     */
    /* JADX WARNING: Missing block: B:73:?, code skipped:
            r4 = r1.getUidStatsLocked(r1.mapUid(r14.uid));
     */
    /* JADX WARNING: Missing block: B:74:0x01f4, code skipped:
            r38 = r0;
     */
    /* JADX WARNING: Missing block: B:76:?, code skipped:
            r5 = r14.rxPackets + r14.txPackets;
            r0 = (r8 * r5) / r36;
            r4.noteMobileRadioActiveTimeLocked(r0);
            r8 = r8 - r0;
            r22 = r36 - r5;
     */
    /* JADX WARNING: Missing block: B:77:0x0203, code skipped:
            if (r2 == null) goto L_0x0263;
     */
    /* JADX WARNING: Missing block: B:78:0x0205, code skipped:
            r7 = r4.getOrCreateModemControllerActivityLocked();
     */
    /* JADX WARNING: Missing block: B:80:0x020c, code skipped:
            if (r11 <= 0) goto L_0x0230;
     */
    /* JADX WARNING: Missing block: B:81:0x020e, code skipped:
            r39 = r0;
     */
    /* JADX WARNING: Missing block: B:84:0x0214, code skipped:
            if (r14.rxPackets <= 0) goto L_0x022b;
     */
    /* JADX WARNING: Missing block: B:85:0x0216, code skipped:
            r41 = r4;
            r42 = r5;
            r7.getRxTimeCounter().addCountLocked((r14.rxPackets * ((long) r2.getRxTimeMillis())) / r11);
     */
    /* JADX WARNING: Missing block: B:86:0x022b, code skipped:
            r41 = r4;
            r42 = r5;
     */
    /* JADX WARNING: Missing block: B:87:0x0230, code skipped:
            r39 = r0;
            r41 = r4;
            r42 = r5;
     */
    /* JADX WARNING: Missing block: B:89:0x0238, code skipped:
            if (r16 <= 0) goto L_0x0263;
     */
    /* JADX WARNING: Missing block: B:92:0x023e, code skipped:
            if (r14.txPackets <= 0) goto L_0x0263;
     */
    /* JADX WARNING: Missing block: B:93:0x0240, code skipped:
            r0 = 0;
     */
    /* JADX WARNING: Missing block: B:95:0x0242, code skipped:
            if (r0 >= 5) goto L_0x0263;
     */
    /* JADX WARNING: Missing block: B:97:0x024c, code skipped:
            r44 = r2;
     */
    /* JADX WARNING: Missing block: B:99:?, code skipped:
            r7.getTxTimeCounters()[r0].addCountLocked((r14.txPackets * ((long) r2.getTxTimeMillis()[r0])) / r16);
     */
    /* JADX WARNING: Missing block: B:100:0x025b, code skipped:
            r0 = r0 + 1;
            r2 = r44;
     */
    /* JADX WARNING: Missing block: B:101:0x0260, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:102:0x0263, code skipped:
            r44 = r2;
            r5 = r22;
     */
    /* JADX WARNING: Missing block: B:103:0x0267, code skipped:
            r0 = r38 + 1;
            r2 = r44;
            r1 = r47;
     */
    /* JADX WARNING: Missing block: B:104:0x0270, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:105:0x0271, code skipped:
            r44 = r2;
            r1 = r47;
     */
    /* JADX WARNING: Missing block: B:106:0x0276, code skipped:
            r44 = r2;
            r36 = r5;
     */
    /* JADX WARNING: Missing block: B:107:0x027b, code skipped:
            r44 = r2;
     */
    /* JADX WARNING: Missing block: B:109:0x027f, code skipped:
            if (r8 <= 0) goto L_0x0292;
     */
    /* JADX WARNING: Missing block: B:110:0x0281, code skipped:
            r1 = r47;
     */
    /* JADX WARNING: Missing block: B:112:?, code skipped:
            r1.mMobileRadioActiveUnknownTime.addCountLocked(r8);
            r45 = r5;
            r1.mMobileRadioActiveUnknownCount.addCountLocked(1);
     */
    /* JADX WARNING: Missing block: B:113:0x0292, code skipped:
            r45 = r5;
            r1 = r47;
     */
    /* JADX WARNING: Missing block: B:114:0x0296, code skipped:
            r1.mNetworkStatsPool.release(r3);
     */
    /* JADX WARNING: Missing block: B:115:0x029d, code skipped:
            r44 = r2;
     */
    /* JADX WARNING: Missing block: B:116:0x029f, code skipped:
            monitor-exit(r47);
     */
    /* JADX WARNING: Missing block: B:117:0x02a0, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:118:0x02a1, code skipped:
            r0 = th;
     */
    /* JADX WARNING: Missing block: B:119:0x02a2, code skipped:
            r44 = r2;
     */
    /* JADX WARNING: Missing block: B:120:0x02a4, code skipped:
            monitor-exit(r47);
     */
    /* JADX WARNING: Missing block: B:121:0x02a5, code skipped:
            throw r0;
     */
    /* JADX WARNING: Missing block: B:122:0x02a6, code skipped:
            r0 = th;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateMobileRadioState(ModemActivityInfo activityInfo) {
        Throwable th;
        ModemActivityInfo modemActivityInfo;
        BatteryStatsImpl batteryStatsImpl = this;
        ModemActivityInfo deltaInfo = getDeltaModemActivityInfo(activityInfo);
        batteryStatsImpl.addModemTxPowerToHistory(deltaInfo);
        NetworkStats delta = null;
        synchronized (batteryStatsImpl.mModemNetworkLock) {
            try {
                NetworkStats latestStats = batteryStatsImpl.readNetworkStatsLocked(batteryStatsImpl.mModemIfaces);
                if (latestStats != null) {
                    try {
                        delta = NetworkStats.subtract(latestStats, batteryStatsImpl.mLastModemNetworkStats, null, null, (NetworkStats) batteryStatsImpl.mNetworkStatsPool.acquire());
                        batteryStatsImpl.mNetworkStatsPool.release(batteryStatsImpl.mLastModemNetworkStats);
                        batteryStatsImpl.mLastModemNetworkStats = latestStats;
                    } catch (Throwable th2) {
                        th = th2;
                        modemActivityInfo = deltaInfo;
                    }
                }
            } catch (Throwable th3) {
                th = th3;
                modemActivityInfo = deltaInfo;
                while (true) {
                    try {
                        break;
                    } catch (Throwable th4) {
                        th = th4;
                    }
                }
                throw th;
            }
        }
    }

    /* JADX WARNING: Missing block: B:23:0x0043, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:29:0x0059, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:31:0x005b, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized void addModemTxPowerToHistory(ModemActivityInfo activityInfo) {
        if (activityInfo != null) {
            int[] txTimeMs = activityInfo.getTxTimeMillis();
            if (txTimeMs != null) {
                if (txTimeMs.length == 5) {
                    long elapsedRealtime = this.mClocks.elapsedRealtime();
                    long uptime = this.mClocks.uptimeMillis();
                    int levelMaxTimeSpent = 0;
                    for (int i = 1; i < txTimeMs.length; i++) {
                        if (txTimeMs[i] > txTimeMs[levelMaxTimeSpent]) {
                            levelMaxTimeSpent = i;
                        }
                    }
                    HistoryItem historyItem;
                    if (levelMaxTimeSpent == 4) {
                        if (!this.mIsCellularTxPowerHigh) {
                            historyItem = this.mHistoryCur;
                            historyItem.states2 |= 524288;
                            addHistoryRecordLocked(elapsedRealtime, uptime);
                            this.mIsCellularTxPowerHigh = true;
                        }
                    } else if (this.mIsCellularTxPowerHigh) {
                        historyItem = this.mHistoryCur;
                        historyItem.states2 &= -524289;
                        addHistoryRecordLocked(elapsedRealtime, uptime);
                        this.mIsCellularTxPowerHigh = false;
                    }
                }
            }
        }
    }

    public void updateBluetoothStateLocked(BluetoothActivityEnergyInfo info) {
        BatteryStatsImpl batteryStatsImpl = this;
        BatteryStatsImpl batteryStatsImpl2;
        if (info == null || !batteryStatsImpl.mOnBatteryInternal) {
            batteryStatsImpl2 = batteryStatsImpl;
            return;
        }
        int uidCount;
        long idleTimeMs;
        long elapsedRealtimeMs;
        UidTraffic traffic;
        boolean normalizeScanTxTime;
        long txTimeMs;
        long rxTimeMs;
        batteryStatsImpl.mHasBluetoothReporting = true;
        long elapsedRealtimeMs2 = batteryStatsImpl.mClocks.elapsedRealtime();
        long rxTimeMs2 = info.getControllerRxTimeMillis() - batteryStatsImpl.mLastBluetoothActivityInfo.rxTimeMs;
        long txTimeMs2 = info.getControllerTxTimeMillis() - batteryStatsImpl.mLastBluetoothActivityInfo.txTimeMs;
        long idleTimeMs2 = info.getControllerIdleTimeMillis() - batteryStatsImpl.mLastBluetoothActivityInfo.idleTimeMs;
        int uidCount2 = batteryStatsImpl.mUidStats.size();
        long totalScanTimeMs = 0;
        for (int i = 0; i < uidCount2; i++) {
            Uid u = (Uid) batteryStatsImpl.mUidStats.valueAt(i);
            if (u.mBluetoothScanTimer != null) {
                totalScanTimeMs += u.mBluetoothScanTimer.getTimeSinceMarkLocked(elapsedRealtimeMs2 * 1000) / 1000;
            }
        }
        long totalScanTimeMs2 = totalScanTimeMs;
        boolean normalizeScanRxTime = totalScanTimeMs2 > rxTimeMs2;
        boolean normalizeScanTxTime2 = totalScanTimeMs2 > txTimeMs2;
        long leftOverRxTimeMs = rxTimeMs2;
        long leftOverTxTimeMs = txTimeMs2;
        int i2 = 0;
        while (i2 < uidCount2) {
            boolean normalizeScanRxTime2;
            uidCount = uidCount2;
            Uid u2 = (Uid) batteryStatsImpl.mUidStats.valueAt(i2);
            idleTimeMs = idleTimeMs2;
            if (u2.mBluetoothScanTimer == null) {
                normalizeScanRxTime2 = normalizeScanRxTime;
                elapsedRealtimeMs = elapsedRealtimeMs2;
            } else {
                long scanTimeTxSinceMarkMs = u2.mBluetoothScanTimer.getTimeSinceMarkLocked(elapsedRealtimeMs2 * 1000) / 1000;
                if (scanTimeTxSinceMarkMs > 0) {
                    u2.mBluetoothScanTimer.setMark(elapsedRealtimeMs2);
                    idleTimeMs2 = scanTimeTxSinceMarkMs;
                    long scanTimeTxSinceMarkMs2 = scanTimeTxSinceMarkMs;
                    if (normalizeScanRxTime) {
                        idleTimeMs2 = (rxTimeMs2 * idleTimeMs2) / totalScanTimeMs2;
                    }
                    if (normalizeScanTxTime2) {
                        scanTimeTxSinceMarkMs2 = (txTimeMs2 * scanTimeTxSinceMarkMs2) / totalScanTimeMs2;
                    }
                    long scanTimeSinceMarkMs = scanTimeTxSinceMarkMs;
                    scanTimeTxSinceMarkMs = scanTimeTxSinceMarkMs2;
                    normalizeScanRxTime2 = normalizeScanRxTime;
                    normalizeScanRxTime = u2.getOrCreateBluetoothControllerActivityLocked();
                    elapsedRealtimeMs = elapsedRealtimeMs2;
                    normalizeScanRxTime.getRxTimeCounter().addCountLocked(idleTimeMs2);
                    normalizeScanRxTime.getTxTimeCounters()[0].addCountLocked(scanTimeTxSinceMarkMs);
                    leftOverRxTimeMs -= idleTimeMs2;
                    leftOverTxTimeMs -= scanTimeTxSinceMarkMs;
                } else {
                    normalizeScanRxTime2 = normalizeScanRxTime;
                    elapsedRealtimeMs = elapsedRealtimeMs2;
                }
            }
            i2++;
            uidCount2 = uidCount;
            idleTimeMs2 = idleTimeMs;
            normalizeScanRxTime = normalizeScanRxTime2;
            elapsedRealtimeMs2 = elapsedRealtimeMs;
            batteryStatsImpl = this;
            BluetoothActivityEnergyInfo bluetoothActivityEnergyInfo = info;
        }
        elapsedRealtimeMs = elapsedRealtimeMs2;
        idleTimeMs = idleTimeMs2;
        uidCount = uidCount2;
        long totalRxBytes = 0;
        BluetoothActivityEnergyInfo bluetoothActivityEnergyInfo2 = info;
        UidTraffic[] uidTraffic = info.getUidTraffic();
        int numUids = uidTraffic != null ? uidTraffic.length : 0;
        long totalTxBytes = 0;
        int i3 = 0;
        while (i3 < numUids) {
            traffic = uidTraffic[i3];
            normalizeScanTxTime = normalizeScanTxTime2;
            txTimeMs = txTimeMs2;
            txTimeMs2 = traffic.getRxBytes() - this.mLastBluetoothActivityInfo.uidRxBytes.get(traffic.getUid());
            rxTimeMs = rxTimeMs2;
            long txBytes = traffic.getTxBytes() - this.mLastBluetoothActivityInfo.uidTxBytes.get(traffic.getUid());
            this.mNetworkByteActivityCounters[4].addCountLocked(txTimeMs2);
            this.mNetworkByteActivityCounters[5].addCountLocked(txBytes);
            Uid uidStatsLocked = getUidStatsLocked(mapUid(traffic.getUid()));
            uidStatsLocked.noteNetworkActivityLocked(4, txTimeMs2, 0);
            uidStatsLocked.noteNetworkActivityLocked(5, txBytes, 0);
            totalRxBytes += txTimeMs2;
            totalTxBytes += txBytes;
            i3++;
            normalizeScanTxTime2 = normalizeScanTxTime;
            txTimeMs2 = txTimeMs;
            rxTimeMs2 = rxTimeMs;
            bluetoothActivityEnergyInfo2 = info;
        }
        rxTimeMs = rxTimeMs2;
        txTimeMs = txTimeMs2;
        normalizeScanTxTime = normalizeScanTxTime2;
        if (!((totalTxBytes == 0 && totalRxBytes == 0) || (leftOverRxTimeMs == 0 && leftOverTxTimeMs == 0))) {
            for (i3 = 0; i3 < numUids; i3++) {
                traffic = uidTraffic[i3];
                int uid = traffic.getUid();
                rxTimeMs2 = traffic.getRxBytes() - this.mLastBluetoothActivityInfo.uidRxBytes.get(uid);
                txTimeMs2 = traffic.getTxBytes() - this.mLastBluetoothActivityInfo.uidTxBytes.get(uid);
                Uid u3 = getUidStatsLocked(mapUid(uid));
                ControllerActivityCounterImpl counter = u3.getOrCreateBluetoothControllerActivityLocked();
                if (totalRxBytes <= 0 || rxTimeMs2 <= 0) {
                    long j = rxTimeMs2;
                } else {
                    rxTimeMs2 = (leftOverRxTimeMs * rxTimeMs2) / totalRxBytes;
                    counter.getRxTimeCounter().addCountLocked(rxTimeMs2);
                    leftOverRxTimeMs -= rxTimeMs2;
                }
                if (totalTxBytes > 0 && txTimeMs2 > 0) {
                    rxTimeMs2 = (leftOverTxTimeMs * txTimeMs2) / totalTxBytes;
                    counter.getTxTimeCounters()[0].addCountLocked(rxTimeMs2);
                    leftOverTxTimeMs -= rxTimeMs2;
                }
            }
        }
        this.mBluetoothActivity.getRxTimeCounter().addCountLocked(rxTimeMs);
        this.mBluetoothActivity.getTxTimeCounters()[0].addCountLocked(txTimeMs);
        long idleTimeMs3 = idleTimeMs;
        this.mBluetoothActivity.getIdleTimeCounter().addCountLocked(idleTimeMs3);
        double opVolt = this.mPowerProfile.getAveragePower(PowerProfile.POWER_BLUETOOTH_CONTROLLER_OPERATING_VOLTAGE) / 1000.0d;
        if (opVolt != 0.0d) {
            this.mBluetoothActivity.getPowerCounter().addCountLocked((long) (((double) (info.getControllerEnergyUsed() - this.mLastBluetoothActivityInfo.energy)) / opVolt));
        }
        this.mLastBluetoothActivityInfo.set(info);
    }

    public void updateRpmStatsLocked() {
        if (this.mPlatformIdleStateCallback != null) {
            long now = SystemClock.elapsedRealtime();
            long j = 1000;
            if (now - this.mLastRpmStatsUpdateTimeMs >= 1000) {
                this.mPlatformIdleStateCallback.fillLowPowerStats(this.mTmpRpmStats);
                this.mLastRpmStatsUpdateTimeMs = now;
            }
            for (Entry<String, PowerStatePlatformSleepState> pstate : this.mTmpRpmStats.mPlatformLowPowerStats.entrySet()) {
                String pName = (String) pstate.getKey();
                getRpmTimerLocked(pName).update(((PowerStatePlatformSleepState) pstate.getValue()).mTimeMs * j, ((PowerStatePlatformSleepState) pstate.getValue()).mCount);
                for (Entry<String, PowerStateElement> voter : ((PowerStatePlatformSleepState) pstate.getValue()).mVoters.entrySet()) {
                    String vName = new StringBuilder();
                    vName.append(pName);
                    vName.append(".");
                    vName.append((String) voter.getKey());
                    vName = vName.toString();
                    getRpmTimerLocked(vName).update(((PowerStateElement) voter.getValue()).mTimeMs * j, ((PowerStateElement) voter.getValue()).mCount);
                    j = 1000;
                }
                j = 1000;
            }
            for (Entry<String, PowerStateSubsystem> subsys : this.mTmpRpmStats.mSubsystemLowPowerStats.entrySet()) {
                String subsysName = (String) subsys.getKey();
                for (Entry<String, PowerStateElement> sstate : ((PowerStateSubsystem) subsys.getValue()).mStates.entrySet()) {
                    String name = new StringBuilder();
                    name.append(subsysName);
                    name.append(".");
                    name.append((String) sstate.getKey());
                    name = name.toString();
                    getRpmTimerLocked(name).update(((PowerStateElement) sstate.getValue()).mTimeMs * 1000, ((PowerStateElement) sstate.getValue()).mCount);
                }
            }
        }
    }

    public void updateKernelWakelocksLocked() {
        long startTime = SystemClock.elapsedRealtime();
        KernelWakelockStats wakelockStats = this.mKernelWakelockReader.readKernelWakelockStats(this.mTmpWakelockStats);
        long readKernelWakelockStatsDuration = SystemClock.elapsedRealtime() - startTime;
        if (readKernelWakelockStatsDuration > 500) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Read kernel wake lock stats duration: ");
            stringBuilder.append(readKernelWakelockStatsDuration);
            stringBuilder.append(" ms. ");
            Slog.w(str, stringBuilder.toString());
        }
        if (wakelockStats == null) {
            Slog.w(TAG, "Couldn't get kernel wake lock stats");
            return;
        }
        for (Entry<String, KernelWakelockStats.Entry> ent : wakelockStats.entrySet()) {
            String name = (String) ent.getKey();
            KernelWakelockStats.Entry kws = (KernelWakelockStats.Entry) ent.getValue();
            SamplingTimer kwlt = (SamplingTimer) this.mKernelWakelockStats.get(name);
            if (kwlt == null) {
                kwlt = new SamplingTimer(this.mClocks, this.mOnBatteryScreenOffTimeBase);
                this.mKernelWakelockStats.put(name, kwlt);
            }
            kwlt.update(kws.mTotalTime, kws.mCount);
            kwlt.setUpdateVersion(kws.mVersion);
        }
        int numWakelocksSetStale = 0;
        for (Entry<String, SamplingTimer> ent2 : this.mKernelWakelockStats.entrySet()) {
            SamplingTimer st = (SamplingTimer) ent2.getValue();
            if (st.getUpdateVersion() != wakelockStats.kernelWakelockVersion) {
                st.endSample();
                numWakelocksSetStale++;
            }
        }
        if (wakelockStats.isEmpty()) {
            Slog.wtf(TAG, "All kernel wakelocks had time of zero");
        }
        if (numWakelocksSetStale == this.mKernelWakelockStats.size()) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("All kernel wakelocks were set stale. new version=");
            stringBuilder2.append(wakelockStats.kernelWakelockVersion);
            Slog.wtf(str2, stringBuilder2.toString());
        }
    }

    public void updateKernelMemoryBandwidthLocked() {
        this.mKernelMemoryBandwidthStats.updateStats();
        LongSparseLongArray bandwidthEntries = this.mKernelMemoryBandwidthStats.getBandwidthEntries();
        int bandwidthEntryCount = bandwidthEntries.size();
        for (int i = 0; i < bandwidthEntryCount; i++) {
            SamplingTimer timer;
            int indexOfKey = this.mKernelMemoryStats.indexOfKey(bandwidthEntries.keyAt(i));
            int index = indexOfKey;
            if (indexOfKey >= 0) {
                timer = (SamplingTimer) this.mKernelMemoryStats.valueAt(index);
            } else {
                timer = new SamplingTimer(this.mClocks, this.mOnBatteryTimeBase);
                this.mKernelMemoryStats.put(bandwidthEntries.keyAt(i), timer);
            }
            timer.update(bandwidthEntries.valueAt(i), 1);
        }
    }

    public boolean isOnBatteryLocked() {
        return this.mOnBatteryTimeBase.isRunning();
    }

    public boolean isOnBatteryScreenOffLocked() {
        return this.mOnBatteryScreenOffTimeBase.isRunning();
    }

    @GuardedBy("this")
    public void updateCpuTimeLocked(boolean onBattery, boolean onBatteryScreenOff) {
        if (this.mPowerProfile != null) {
            int i;
            if (this.mCpuFreqs == null) {
                this.mCpuFreqs = this.mKernelUidCpuFreqTimeReader.readFreqs(this.mPowerProfile);
            }
            ArrayList<StopwatchTimer> partialTimersToConsider = null;
            if (onBatteryScreenOff) {
                partialTimersToConsider = new ArrayList();
                for (i = this.mPartialTimers.size() - 1; i >= 0; i--) {
                    StopwatchTimer timer = (StopwatchTimer) this.mPartialTimers.get(i);
                    if (!(!timer.mInList || timer.mUid == null || timer.mUid.mUid == 1000)) {
                        partialTimersToConsider.add(timer);
                    }
                }
            }
            markPartialTimersAsEligible();
            SparseLongArray updatedUids = null;
            if (onBattery) {
                this.mUserInfoProvider.refreshUserIds();
                if (!this.mKernelUidCpuFreqTimeReader.perClusterTimesAvailable()) {
                    updatedUids = new SparseLongArray();
                }
                readKernelUidCpuTimesLocked(partialTimersToConsider, updatedUids, onBattery);
                if (updatedUids != null) {
                    updateClusterSpeedTimes(updatedUids, onBattery);
                }
                readKernelUidCpuFreqTimesLocked(partialTimersToConsider, onBattery, onBatteryScreenOff);
                this.mNumAllUidCpuTimeReads += 2;
                if (this.mConstants.TRACK_CPU_ACTIVE_CLUSTER_TIME) {
                    readKernelUidCpuActiveTimesLocked(onBattery);
                    readKernelUidCpuClusterTimesLocked(onBattery);
                    this.mNumAllUidCpuTimeReads += 2;
                }
                return;
            }
            this.mKernelUidCpuTimeReader.readDelta(null);
            this.mKernelUidCpuFreqTimeReader.readDelta(null);
            this.mNumAllUidCpuTimeReads += 2;
            if (this.mConstants.TRACK_CPU_ACTIVE_CLUSTER_TIME) {
                this.mKernelUidCpuActiveTimeReader.readDelta(null);
                this.mKernelUidCpuClusterTimeReader.readDelta(null);
                this.mNumAllUidCpuTimeReads += 2;
            }
            for (i = this.mKernelCpuSpeedReaders.length - 1; i >= 0; i--) {
                this.mKernelCpuSpeedReaders[i].readDelta();
            }
        }
    }

    @VisibleForTesting
    public void markPartialTimersAsEligible() {
        int i;
        if (ArrayUtils.referenceEquals(this.mPartialTimers, this.mLastPartialTimers)) {
            for (i = this.mPartialTimers.size() - 1; i >= 0; i--) {
                ((StopwatchTimer) this.mPartialTimers.get(i)).mInList = true;
            }
            return;
        }
        int i2;
        i = this.mLastPartialTimers.size() - 1;
        while (true) {
            i2 = 0;
            if (i < 0) {
                break;
            }
            ((StopwatchTimer) this.mLastPartialTimers.get(i)).mInList = false;
            i--;
        }
        this.mLastPartialTimers.clear();
        i = this.mPartialTimers.size();
        while (i2 < i) {
            StopwatchTimer timer = (StopwatchTimer) this.mPartialTimers.get(i2);
            timer.mInList = true;
            this.mLastPartialTimers.add(timer);
            i2++;
        }
    }

    @VisibleForTesting
    public void updateClusterSpeedTimes(SparseLongArray updatedUids, boolean onBattery) {
        int cluster;
        int speed;
        long[][] clusterSpeedTimesMs;
        boolean z;
        BatteryStatsImpl batteryStatsImpl = this;
        SparseLongArray sparseLongArray = updatedUids;
        long[][] clusterSpeedTimesMs2 = new long[batteryStatsImpl.mKernelCpuSpeedReaders.length][];
        long totalCpuClustersTimeMs = 0;
        for (cluster = 0; cluster < batteryStatsImpl.mKernelCpuSpeedReaders.length; cluster++) {
            clusterSpeedTimesMs2[cluster] = batteryStatsImpl.mKernelCpuSpeedReaders[cluster].readDelta();
            if (clusterSpeedTimesMs2[cluster] != null) {
                for (speed = clusterSpeedTimesMs2[cluster].length - 1; speed >= 0; speed--) {
                    totalCpuClustersTimeMs += clusterSpeedTimesMs2[cluster][speed];
                }
            }
        }
        if (totalCpuClustersTimeMs != 0) {
            cluster = updatedUids.size();
            speed = 0;
            while (speed < cluster) {
                Uid u = batteryStatsImpl.getUidStatsLocked(sparseLongArray.keyAt(speed));
                long appCpuTimeUs = sparseLongArray.valueAt(speed);
                int numClusters = batteryStatsImpl.mPowerProfile.getNumCpuClusters();
                if (u.mCpuClusterSpeedTimesUs == null || u.mCpuClusterSpeedTimesUs.length != numClusters) {
                    u.mCpuClusterSpeedTimesUs = new LongSamplingCounter[numClusters][];
                }
                int cluster2 = 0;
                while (cluster2 < clusterSpeedTimesMs2.length) {
                    int speedsInCluster = clusterSpeedTimesMs2[cluster2].length;
                    if (u.mCpuClusterSpeedTimesUs[cluster2] == null || speedsInCluster != u.mCpuClusterSpeedTimesUs[cluster2].length) {
                        u.mCpuClusterSpeedTimesUs[cluster2] = new LongSamplingCounter[speedsInCluster];
                    }
                    LongSamplingCounter[] cpuSpeeds = u.mCpuClusterSpeedTimesUs[cluster2];
                    int speed2 = 0;
                    while (speed2 < speedsInCluster) {
                        if (cpuSpeeds[speed2] == null) {
                            cpuSpeeds[speed2] = new LongSamplingCounter(batteryStatsImpl.mOnBatteryTimeBase);
                        }
                        clusterSpeedTimesMs = clusterSpeedTimesMs2;
                        cpuSpeeds[speed2].addCountLocked((clusterSpeedTimesMs2[cluster2][speed2] * appCpuTimeUs) / totalCpuClustersTimeMs, onBattery);
                        speed2++;
                        clusterSpeedTimesMs2 = clusterSpeedTimesMs;
                        batteryStatsImpl = this;
                        sparseLongArray = updatedUids;
                    }
                    z = onBattery;
                    clusterSpeedTimesMs = clusterSpeedTimesMs2;
                    cluster2++;
                    batteryStatsImpl = this;
                    sparseLongArray = updatedUids;
                }
                z = onBattery;
                clusterSpeedTimesMs = clusterSpeedTimesMs2;
                speed++;
                batteryStatsImpl = this;
                sparseLongArray = updatedUids;
            }
        }
        z = onBattery;
        clusterSpeedTimesMs = clusterSpeedTimesMs2;
    }

    @VisibleForTesting
    public void readKernelUidCpuTimesLocked(ArrayList<StopwatchTimer> partialTimers, SparseLongArray updatedUids, boolean onBattery) {
        long startTimeMs;
        ArrayList<StopwatchTimer> arrayList = partialTimers;
        SparseLongArray sparseLongArray = updatedUids;
        boolean z = onBattery;
        this.mTempTotalCpuSystemTimeUs = 0;
        this.mTempTotalCpuUserTimeUs = 0;
        int numWakelocks = arrayList == null ? 0 : partialTimers.size();
        long startTimeMs2 = this.mClocks.uptimeMillis();
        this.mKernelUidCpuTimeReader.readDelta(new -$$Lambda$BatteryStatsImpl$cVkGM5pv4uMLFgnMwqPRDhEl5a0(this, numWakelocks, z, sparseLongArray));
        long elapsedTimeMs = this.mClocks.uptimeMillis() - startTimeMs2;
        if (elapsedTimeMs >= 100) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Reading cpu stats took ");
            stringBuilder.append(elapsedTimeMs);
            stringBuilder.append("ms");
            Slog.d(str, stringBuilder.toString());
        }
        if (numWakelocks > 0) {
            this.mTempTotalCpuUserTimeUs = (this.mTempTotalCpuUserTimeUs * 50) / 100;
            this.mTempTotalCpuSystemTimeUs = (this.mTempTotalCpuSystemTimeUs * 50) / 100;
            int i = 0;
            while (true) {
                int i2 = i;
                if (i2 >= numWakelocks) {
                    break;
                }
                StopwatchTimer timer = (StopwatchTimer) arrayList.get(i2);
                int userTimeUs = (int) (this.mTempTotalCpuUserTimeUs / ((long) (numWakelocks - i2)));
                int numWakelocks2 = numWakelocks;
                startTimeMs = startTimeMs2;
                int systemTimeUs = (int) (this.mTempTotalCpuSystemTimeUs / ((long) (numWakelocks - i2)));
                timer.mUid.mUserCpuTime.addCountLocked((long) userTimeUs, z);
                timer.mUid.mSystemCpuTime.addCountLocked((long) systemTimeUs, z);
                if (sparseLongArray != null) {
                    numWakelocks = timer.mUid.getUid();
                    sparseLongArray.put(numWakelocks, (sparseLongArray.get(numWakelocks, 0) + ((long) userTimeUs)) + ((long) systemTimeUs));
                }
                timer.mUid.getProcessStatsLocked("*wakelock*").addCpuTimeLocked(userTimeUs / 1000, systemTimeUs / 1000, z);
                this.mTempTotalCpuUserTimeUs -= (long) userTimeUs;
                this.mTempTotalCpuSystemTimeUs -= (long) systemTimeUs;
                i = i2 + 1;
                numWakelocks = numWakelocks2;
                startTimeMs2 = startTimeMs;
            }
        }
        startTimeMs = startTimeMs2;
    }

    public static /* synthetic */ void lambda$readKernelUidCpuTimesLocked$0(BatteryStatsImpl batteryStatsImpl, int numWakelocks, boolean onBattery, SparseLongArray updatedUids, int uid, long userTimeUs, long systemTimeUs) {
        BatteryStatsImpl batteryStatsImpl2 = batteryStatsImpl;
        boolean z = onBattery;
        SparseLongArray sparseLongArray = updatedUids;
        int uid2 = batteryStatsImpl2.mapUid(uid);
        String str;
        StringBuilder stringBuilder;
        if (Process.isIsolated(uid2)) {
            batteryStatsImpl2.mKernelUidCpuTimeReader.removeUid(uid2);
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Got readings for an isolated uid with no mapping: ");
            stringBuilder.append(uid2);
            Slog.d(str, stringBuilder.toString());
        } else if (batteryStatsImpl2.mUserInfoProvider.exists(UserHandle.getUserId(uid2))) {
            long userTimeUs2;
            long systemTimeUs2;
            Uid u = batteryStatsImpl2.getUidStatsLocked(uid2);
            batteryStatsImpl2.mTempTotalCpuUserTimeUs += userTimeUs;
            batteryStatsImpl2.mTempTotalCpuSystemTimeUs += systemTimeUs;
            stringBuilder = null;
            if (numWakelocks > 0) {
                userTimeUs2 = (userTimeUs * 50) / 100;
                systemTimeUs2 = (50 * systemTimeUs) / 100;
            } else {
                userTimeUs2 = userTimeUs;
                systemTimeUs2 = systemTimeUs;
            }
            if (stringBuilder != null) {
                stringBuilder.append("  adding to uid=");
                stringBuilder.append(u.mUid);
                stringBuilder.append(": u=");
                TimeUtils.formatDuration(userTimeUs2 / 1000, stringBuilder);
                stringBuilder.append(" s=");
                TimeUtils.formatDuration(systemTimeUs2 / 1000, stringBuilder);
                Slog.d(TAG, stringBuilder.toString());
            }
            u.mUserCpuTime.addCountLocked(userTimeUs2, z);
            u.mSystemCpuTime.addCountLocked(systemTimeUs2, z);
            if (sparseLongArray != null) {
                sparseLongArray.put(u.getUid(), userTimeUs2 + systemTimeUs2);
            }
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Got readings for an invalid user's uid ");
            stringBuilder.append(uid2);
            Slog.d(str, stringBuilder.toString());
            batteryStatsImpl2.mKernelUidCpuTimeReader.removeUid(uid2);
        }
    }

    @VisibleForTesting
    public void readKernelUidCpuFreqTimesLocked(ArrayList<StopwatchTimer> partialTimers, boolean onBattery, boolean onBatteryScreenOff) {
        boolean perClusterTimesAvailable;
        ArrayList arrayList = partialTimers;
        boolean perClusterTimesAvailable2 = this.mKernelUidCpuFreqTimeReader.perClusterTimesAvailable();
        int numWakelocks = arrayList == null ? 0 : partialTimers.size();
        int numClusters = this.mPowerProfile.getNumCpuClusters();
        this.mWakeLockAllocationsUs = null;
        long startTimeMs = this.mClocks.uptimeMillis();
        KernelUidCpuFreqTimeReader kernelUidCpuFreqTimeReader = this.mKernelUidCpuFreqTimeReader;
        -$$Lambda$BatteryStatsImpl$qYIdEyLMO9XI4FHBl_g5LWknDZQ -__lambda_batterystatsimpl_qyideylmo9xi4fhbl_g5lwkndzq = r0;
        -$$Lambda$BatteryStatsImpl$qYIdEyLMO9XI4FHBl_g5LWknDZQ -__lambda_batterystatsimpl_qyideylmo9xi4fhbl_g5lwkndzq2 = new -$$Lambda$BatteryStatsImpl$qYIdEyLMO9XI4FHBl_g5LWknDZQ(this, onBattery, onBatteryScreenOff, perClusterTimesAvailable2, numClusters, numWakelocks);
        kernelUidCpuFreqTimeReader.readDelta(-__lambda_batterystatsimpl_qyideylmo9xi4fhbl_g5lwkndzq);
        long elapsedTimeMs = this.mClocks.uptimeMillis() - startTimeMs;
        if (elapsedTimeMs >= 100) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Reading cpu freq times took ");
            stringBuilder.append(elapsedTimeMs);
            stringBuilder.append("ms");
            Slog.d(str, stringBuilder.toString());
        }
        if (this.mWakeLockAllocationsUs != null) {
            int i = 0;
            while (i < numWakelocks) {
                ArrayList<StopwatchTimer> arrayList2;
                Uid u = ((StopwatchTimer) arrayList.get(i)).mUid;
                if (u.mCpuClusterSpeedTimesUs == null || u.mCpuClusterSpeedTimesUs.length != numClusters) {
                    u.mCpuClusterSpeedTimesUs = new LongSamplingCounter[numClusters][];
                }
                int cluster = 0;
                while (cluster < numClusters) {
                    int speedsInCluster = this.mPowerProfile.getNumSpeedStepsInCpuCluster(cluster);
                    if (u.mCpuClusterSpeedTimesUs[cluster] == null || u.mCpuClusterSpeedTimesUs[cluster].length != speedsInCluster) {
                        u.mCpuClusterSpeedTimesUs[cluster] = new LongSamplingCounter[speedsInCluster];
                    }
                    LongSamplingCounter[] cpuTimeUs = u.mCpuClusterSpeedTimesUs[cluster];
                    int speed = 0;
                    while (speed < speedsInCluster) {
                        long elapsedTimeMs2;
                        if (cpuTimeUs[speed] == null) {
                            elapsedTimeMs2 = elapsedTimeMs;
                            cpuTimeUs[speed] = new LongSamplingCounter(this.mOnBatteryTimeBase);
                        } else {
                            elapsedTimeMs2 = elapsedTimeMs;
                        }
                        perClusterTimesAvailable = perClusterTimesAvailable2;
                        elapsedTimeMs = this.mWakeLockAllocationsUs[cluster][speed] / ((long) (numWakelocks - i));
                        cpuTimeUs[speed].addCountLocked(elapsedTimeMs, onBattery);
                        long[] jArr = this.mWakeLockAllocationsUs[cluster];
                        jArr[speed] = jArr[speed] - elapsedTimeMs;
                        speed++;
                        elapsedTimeMs = elapsedTimeMs2;
                        perClusterTimesAvailable2 = perClusterTimesAvailable;
                        arrayList2 = partialTimers;
                    }
                    perClusterTimesAvailable = perClusterTimesAvailable2;
                    perClusterTimesAvailable2 = onBattery;
                    cluster++;
                    perClusterTimesAvailable2 = perClusterTimesAvailable;
                    arrayList2 = partialTimers;
                }
                perClusterTimesAvailable = perClusterTimesAvailable2;
                perClusterTimesAvailable2 = onBattery;
                i++;
                perClusterTimesAvailable2 = perClusterTimesAvailable;
                arrayList2 = partialTimers;
            }
        }
        perClusterTimesAvailable = perClusterTimesAvailable2;
        perClusterTimesAvailable2 = onBattery;
    }

    public static /* synthetic */ void lambda$readKernelUidCpuFreqTimesLocked$1(BatteryStatsImpl batteryStatsImpl, boolean onBattery, boolean onBatteryScreenOff, boolean perClusterTimesAvailable, int numClusters, int numWakelocks, int uid, long[] cpuFreqTimeMs) {
        BatteryStatsImpl batteryStatsImpl2 = batteryStatsImpl;
        boolean z = onBattery;
        int i = numClusters;
        long[] jArr = cpuFreqTimeMs;
        int uid2 = batteryStatsImpl2.mapUid(uid);
        String str;
        StringBuilder stringBuilder;
        if (Process.isIsolated(uid2)) {
            batteryStatsImpl2.mKernelUidCpuFreqTimeReader.removeUid(uid2);
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Got freq readings for an isolated uid with no mapping: ");
            stringBuilder.append(uid2);
            Slog.d(str, stringBuilder.toString());
        } else if (batteryStatsImpl2.mUserInfoProvider.exists(UserHandle.getUserId(uid2))) {
            Uid u = batteryStatsImpl2.getUidStatsLocked(uid2);
            if (u.mCpuFreqTimeMs == null || u.mCpuFreqTimeMs.getSize() != jArr.length) {
                u.mCpuFreqTimeMs = new LongSamplingCounterArray(batteryStatsImpl2.mOnBatteryTimeBase);
            }
            u.mCpuFreqTimeMs.addCountLocked(jArr, z);
            if (u.mScreenOffCpuFreqTimeMs == null || u.mScreenOffCpuFreqTimeMs.getSize() != jArr.length) {
                u.mScreenOffCpuFreqTimeMs = new LongSamplingCounterArray(batteryStatsImpl2.mOnBatteryScreenOffTimeBase);
            }
            u.mScreenOffCpuFreqTimeMs.addCountLocked(jArr, onBatteryScreenOff);
            if (perClusterTimesAvailable) {
                if (u.mCpuClusterSpeedTimesUs == null || u.mCpuClusterSpeedTimesUs.length != i) {
                    u.mCpuClusterSpeedTimesUs = new LongSamplingCounter[i][];
                }
                if (numWakelocks > 0 && batteryStatsImpl2.mWakeLockAllocationsUs == null) {
                    batteryStatsImpl2.mWakeLockAllocationsUs = new long[i][];
                }
                int freqIndex = 0;
                int cluster = 0;
                while (cluster < i) {
                    int speedsInCluster = batteryStatsImpl2.mPowerProfile.getNumSpeedStepsInCpuCluster(cluster);
                    if (u.mCpuClusterSpeedTimesUs[cluster] == null || u.mCpuClusterSpeedTimesUs[cluster].length != speedsInCluster) {
                        u.mCpuClusterSpeedTimesUs[cluster] = new LongSamplingCounter[speedsInCluster];
                    }
                    if (numWakelocks > 0 && batteryStatsImpl2.mWakeLockAllocationsUs[cluster] == null) {
                        batteryStatsImpl2.mWakeLockAllocationsUs[cluster] = new long[speedsInCluster];
                    }
                    LongSamplingCounter[] cpuTimesUs = u.mCpuClusterSpeedTimesUs[cluster];
                    int freqIndex2 = freqIndex;
                    freqIndex = 0;
                    while (freqIndex < speedsInCluster) {
                        long appAllocationUs;
                        if (cpuTimesUs[freqIndex] == null) {
                            cpuTimesUs[freqIndex] = new LongSamplingCounter(batteryStatsImpl2.mOnBatteryTimeBase);
                        }
                        if (batteryStatsImpl2.mWakeLockAllocationsUs != null) {
                            appAllocationUs = ((jArr[freqIndex2] * 1000) * 50) / 100;
                            long[] jArr2 = batteryStatsImpl2.mWakeLockAllocationsUs[cluster];
                            jArr2[freqIndex] = jArr2[freqIndex] + ((jArr[freqIndex2] * 1000) - appAllocationUs);
                        } else {
                            appAllocationUs = jArr[freqIndex2] * 1000;
                        }
                        cpuTimesUs[freqIndex].addCountLocked(appAllocationUs, z);
                        freqIndex2++;
                        freqIndex++;
                        i = numClusters;
                    }
                    cluster++;
                    freqIndex = freqIndex2;
                    i = numClusters;
                }
            }
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Got freq readings for an invalid user's uid ");
            stringBuilder.append(uid2);
            Slog.d(str, stringBuilder.toString());
            batteryStatsImpl2.mKernelUidCpuFreqTimeReader.removeUid(uid2);
        }
    }

    @VisibleForTesting
    public void readKernelUidCpuActiveTimesLocked(boolean onBattery) {
        long startTimeMs = this.mClocks.uptimeMillis();
        this.mKernelUidCpuActiveTimeReader.readDelta(new -$$Lambda$BatteryStatsImpl$mMCK0IbpOZu45KINuNCoRayjoDU(this, onBattery));
        long elapsedTimeMs = this.mClocks.uptimeMillis() - startTimeMs;
        if (elapsedTimeMs >= 100) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Reading cpu active times took ");
            stringBuilder.append(elapsedTimeMs);
            stringBuilder.append("ms");
            Slog.d(str, stringBuilder.toString());
        }
    }

    public static /* synthetic */ void lambda$readKernelUidCpuActiveTimesLocked$2(BatteryStatsImpl batteryStatsImpl, boolean onBattery, int uid, long cpuActiveTimesMs) {
        uid = batteryStatsImpl.mapUid(uid);
        String str;
        StringBuilder stringBuilder;
        if (Process.isIsolated(uid)) {
            batteryStatsImpl.mKernelUidCpuActiveTimeReader.removeUid(uid);
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Got active times for an isolated uid with no mapping: ");
            stringBuilder.append(uid);
            Slog.w(str, stringBuilder.toString());
        } else if (batteryStatsImpl.mUserInfoProvider.exists(UserHandle.getUserId(uid))) {
            batteryStatsImpl.getUidStatsLocked(uid).mCpuActiveTimeMs.addCountLocked(cpuActiveTimesMs, onBattery);
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Got active times for an invalid user's uid ");
            stringBuilder.append(uid);
            Slog.w(str, stringBuilder.toString());
            batteryStatsImpl.mKernelUidCpuActiveTimeReader.removeUid(uid);
        }
    }

    @VisibleForTesting
    public void readKernelUidCpuClusterTimesLocked(boolean onBattery) {
        long startTimeMs = this.mClocks.uptimeMillis();
        this.mKernelUidCpuClusterTimeReader.readDelta(new -$$Lambda$BatteryStatsImpl$WJBQdQHGlhcwV7yfM8vNEWWvVp0(this, onBattery));
        long elapsedTimeMs = this.mClocks.uptimeMillis() - startTimeMs;
        if (elapsedTimeMs >= 100) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Reading cpu cluster times took ");
            stringBuilder.append(elapsedTimeMs);
            stringBuilder.append("ms");
            Slog.d(str, stringBuilder.toString());
        }
    }

    public static /* synthetic */ void lambda$readKernelUidCpuClusterTimesLocked$3(BatteryStatsImpl batteryStatsImpl, boolean onBattery, int uid, long[] cpuClusterTimesMs) {
        uid = batteryStatsImpl.mapUid(uid);
        String str;
        StringBuilder stringBuilder;
        if (Process.isIsolated(uid)) {
            batteryStatsImpl.mKernelUidCpuClusterTimeReader.removeUid(uid);
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Got cluster times for an isolated uid with no mapping: ");
            stringBuilder.append(uid);
            Slog.w(str, stringBuilder.toString());
        } else if (batteryStatsImpl.mUserInfoProvider.exists(UserHandle.getUserId(uid))) {
            batteryStatsImpl.getUidStatsLocked(uid).mCpuClusterTimesMs.addCountLocked(cpuClusterTimesMs, onBattery);
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Got cluster times for an invalid user's uid ");
            stringBuilder.append(uid);
            Slog.w(str, stringBuilder.toString());
            batteryStatsImpl.mKernelUidCpuClusterTimeReader.removeUid(uid);
        }
    }

    boolean setChargingLocked(boolean charging) {
        if (this.mCharging == charging) {
            return false;
        }
        this.mCharging = charging;
        HistoryItem historyItem;
        if (charging) {
            historyItem = this.mHistoryCur;
            historyItem.states2 |= 16777216;
        } else {
            historyItem = this.mHistoryCur;
            historyItem.states2 &= -16777217;
        }
        this.mHandler.sendEmptyMessage(3);
        return true;
    }

    @GuardedBy("this")
    protected void setOnBatteryLocked(long mSecRealtime, long mSecUptime, boolean onBattery, int oldStatus, int level, int chargeUAh) {
        boolean z = onBattery;
        int i = oldStatus;
        int i2 = level;
        int i3 = chargeUAh;
        boolean doWrite = false;
        Message m = this.mHandler.obtainMessage(2);
        m.arg1 = z;
        this.mHandler.sendMessage(m);
        long uptime = mSecUptime * 1000;
        long realtime = mSecRealtime * 1000;
        int screenState = this.mScreenState;
        HistoryItem historyItem;
        if (z) {
            boolean reset;
            int i4;
            boolean reset2 = false;
            if (this.mNoAutoReset) {
                reset = false;
            } else {
                if (i == 5 || i2 >= 90 || ((this.mDischargeCurrentLevel < 20 && i2 >= 80) || (getHighDischargeAmountSinceCharge() >= 200 && this.mHistoryBuffer.dataSize() >= MAX_HISTORY_BUFFER))) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Resetting battery stats: level=");
                    stringBuilder.append(i2);
                    stringBuilder.append(" status=");
                    stringBuilder.append(i);
                    stringBuilder.append(" dischargeLevel=");
                    stringBuilder.append(this.mDischargeCurrentLevel);
                    stringBuilder.append(" lowAmount=");
                    stringBuilder.append(getLowDischargeAmountSinceCharge());
                    stringBuilder.append(" highAmount=");
                    stringBuilder.append(getHighDischargeAmountSinceCharge());
                    Slog.i(str, stringBuilder.toString());
                    if (getLowDischargeAmountSinceCharge() >= 20) {
                        long startTime = SystemClock.uptimeMillis();
                        final Parcel parcel = Parcel.obtain();
                        writeSummaryToParcel(parcel, true);
                        reset = false;
                        final long initialTime = SystemClock.uptimeMillis() - startTime;
                        BackgroundThread.getHandler().post(new Runnable() {
                            public void run() {
                                synchronized (BatteryStatsImpl.this.mCheckinFile) {
                                    Parcel parcel;
                                    long startTime2 = SystemClock.uptimeMillis();
                                    FileOutputStream stream = null;
                                    try {
                                        stream = BatteryStatsImpl.this.mCheckinFile.startWrite();
                                        stream.write(parcel.marshall());
                                        stream.flush();
                                        FileUtils.sync(stream);
                                        stream.close();
                                        BatteryStatsImpl.this.mCheckinFile.finishWrite(stream);
                                        EventLogTags.writeCommitSysConfigFile("batterystats-checkin", (initialTime + SystemClock.uptimeMillis()) - startTime2);
                                        parcel = parcel;
                                    } catch (IOException e) {
                                        try {
                                            Slog.w("BatteryStats", "Error writing checkin battery statistics", e);
                                            BatteryStatsImpl.this.mCheckinFile.failWrite(stream);
                                            parcel = parcel;
                                        } catch (Throwable th) {
                                            parcel.recycle();
                                        }
                                    }
                                    parcel.recycle();
                                }
                            }
                        });
                    } else {
                        reset = false;
                    }
                    resetAllStatsLocked();
                    if (i3 > 0 && i2 > 0) {
                        this.mEstimatedBatteryCapacity = (int) (((double) (i3 / 1000)) / (((double) i2) / 100.0d));
                    }
                    this.mDischargeStartLevel = i2;
                    reset2 = true;
                    LogPower.push(190);
                    this.mDischargeStepTracker.init();
                    doWrite = true;
                }
                reset = reset2;
            }
            if (this.mCharging) {
                setChargingLocked(false);
            }
            this.mLastChargingStateLevel = i2;
            this.mOnBatteryInternal = true;
            this.mOnBattery = true;
            this.mLastDischargeStepLevel = i2;
            this.mMinDischargeStepLevel = i2;
            this.mDischargeStepTracker.clearTime();
            this.mDailyDischargeStepTracker.clearTime();
            this.mInitStepMode = this.mCurStepMode;
            this.mModStepMode = 0;
            pullPendingStateUpdatesLocked();
            this.mHistoryCur.batteryLevel = (byte) i2;
            historyItem = this.mHistoryCur;
            historyItem.states &= -524289;
            if (reset) {
                this.mRecordingHistory = true;
                i4 = 0;
                startRecordingHistory(mSecRealtime, mSecUptime, reset);
            } else {
                i4 = 0;
            }
            addHistoryRecordLocked(mSecRealtime, mSecUptime);
            this.mDischargeUnplugLevel = i2;
            this.mDischargeCurrentLevel = i2;
            if (isScreenOn(screenState)) {
                this.mDischargeScreenOnUnplugLevel = i2;
                this.mDischargeScreenDozeUnplugLevel = i4;
                this.mDischargeScreenOffUnplugLevel = i4;
            } else if (isScreenDoze(screenState)) {
                this.mDischargeScreenOnUnplugLevel = i4;
                this.mDischargeScreenDozeUnplugLevel = i2;
                this.mDischargeScreenOffUnplugLevel = i4;
            } else {
                this.mDischargeScreenOnUnplugLevel = i4;
                this.mDischargeScreenDozeUnplugLevel = i4;
                this.mDischargeScreenOffUnplugLevel = i2;
            }
            this.mDischargeAmountScreenOn = i4;
            this.mDischargeAmountScreenDoze = i4;
            this.mDischargeAmountScreenOff = i4;
            i4 = screenState;
            updateTimeBasesLocked(true, screenState, uptime, realtime);
            int i5 = i4;
        } else {
            int screenState2 = screenState;
            this.mLastChargingStateLevel = i2;
            this.mOnBatteryInternal = false;
            this.mOnBattery = false;
            pullPendingStateUpdatesLocked();
            this.mHistoryCur.batteryLevel = (byte) i2;
            historyItem = this.mHistoryCur;
            historyItem.states |= 524288;
            addHistoryRecordLocked(mSecRealtime, mSecUptime);
            this.mDischargePlugLevel = i2;
            this.mDischargeCurrentLevel = i2;
            if (i2 < this.mDischargeUnplugLevel) {
                this.mLowDischargeAmountSinceCharge += (this.mDischargeUnplugLevel - i2) - 1;
                this.mHighDischargeAmountSinceCharge += this.mDischargeUnplugLevel - i2;
            }
            updateDischargeScreenLevelsLocked(screenState2, screenState2);
            updateTimeBasesLocked(false, screenState2, uptime, realtime);
            this.mChargeStepTracker.init();
            this.mLastChargeStepLevel = i2;
            this.mMaxChargeStepLevel = i2;
            this.mInitStepMode = this.mCurStepMode;
            this.mModStepMode = 0;
        }
        if ((doWrite || this.mLastWriteTime + 60000 < mSecRealtime) && this.mFile != null) {
            writeAsyncLocked();
        }
    }

    private void startRecordingHistory(long elapsedRealtimeMs, long uptimeMs, boolean reset) {
        this.mRecordingHistory = true;
        this.mHistoryCur.currentTime = System.currentTimeMillis();
        addHistoryBufferLocked(elapsedRealtimeMs, reset ? (byte) 7 : (byte) 5, this.mHistoryCur);
        this.mHistoryCur.currentTime = 0;
        if (reset) {
            initActiveHistoryEventsLocked(elapsedRealtimeMs, uptimeMs);
        }
    }

    private void recordCurrentTimeChangeLocked(long currentTime, long elapsedRealtimeMs, long uptimeMs) {
        if (this.mRecordingHistory) {
            this.mHistoryCur.currentTime = currentTime;
            addHistoryBufferLocked(elapsedRealtimeMs, (byte) 5, this.mHistoryCur);
            this.mHistoryCur.currentTime = 0;
        }
    }

    private void recordShutdownLocked(long elapsedRealtimeMs, long uptimeMs) {
        if (this.mRecordingHistory) {
            this.mHistoryCur.currentTime = System.currentTimeMillis();
            addHistoryBufferLocked(elapsedRealtimeMs, (byte) 8, this.mHistoryCur);
            this.mHistoryCur.currentTime = 0;
        }
    }

    private void scheduleSyncExternalStatsLocked(String reason, int updateFlags) {
        if (this.mExternalSync != null) {
            this.mExternalSync.scheduleSync(reason, updateFlags);
        }
    }

    @GuardedBy("this")
    public void setBatteryStateLocked(int status, int health, int plugType, int level, int temp, int volt, int chargeUAh, int chargeFullUAh) {
        boolean z;
        long elapsedRealtime;
        long uptime;
        boolean onBattery;
        int i;
        byte b = status;
        byte b2 = health;
        byte b3 = plugType;
        byte b4 = level;
        int i2 = volt;
        int i3 = chargeUAh;
        int i4 = chargeFullUAh;
        int temp2 = Math.max(0, temp);
        reportChangesToStatsLog(this.mHaveBatteryLevel ? this.mHistoryCur : null, b, b3, b4);
        boolean onBattery2 = isOnBattery(b3, b);
        long uptime2 = this.mClocks.uptimeMillis();
        long elapsedRealtime2 = this.mClocks.elapsedRealtime();
        if (!this.mHaveBatteryLevel) {
            HistoryItem historyItem;
            this.mHaveBatteryLevel = true;
            if (onBattery2 == this.mOnBattery) {
                if (onBattery2) {
                    historyItem = this.mHistoryCur;
                    historyItem.states &= -524289;
                } else {
                    historyItem = this.mHistoryCur;
                    historyItem.states |= 524288;
                }
            }
            historyItem = this.mHistoryCur;
            historyItem.states2 |= 16777216;
            this.mHistoryCur.batteryStatus = (byte) b;
            this.mHistoryCur.batteryLevel = (byte) b4;
            this.mHistoryCur.batteryChargeUAh = i3;
            this.mLastDischargeStepLevel = b4;
            this.mLastChargeStepLevel = b4;
            this.mMinDischargeStepLevel = b4;
            this.mMaxChargeStepLevel = b4;
            this.mLastChargingStateLevel = b4;
        } else if (!(this.mCurrentBatteryLevel == b4 && this.mOnBattery == onBattery2)) {
            z = b4 >= (byte) 100 && onBattery2;
            recordDailyStatsIfNeededLocked(z);
        }
        int oldStatus = this.mHistoryCur.batteryStatus;
        if (onBattery2) {
            this.mDischargeCurrentLevel = b4;
            if (this.mRecordingHistory) {
                elapsedRealtime = elapsedRealtime2;
                uptime = uptime2;
                onBattery = onBattery2;
            } else {
                this.mRecordingHistory = true;
                elapsedRealtime = elapsedRealtime2;
                uptime = uptime2;
                onBattery = onBattery2;
                startRecordingHistory(elapsedRealtime2, uptime2, true);
            }
        } else {
            elapsedRealtime = elapsedRealtime2;
            uptime = uptime2;
            onBattery = onBattery2;
            if (!(b4 >= (byte) 96 || b == (byte) 1 || this.mRecordingHistory)) {
                this.mRecordingHistory = true;
                startRecordingHistory(elapsedRealtime, uptime, true);
            }
        }
        this.mCurrentBatteryLevel = b4;
        if (this.mDischargePlugLevel < 0) {
            this.mDischargePlugLevel = b4;
        }
        onBattery2 = onBattery;
        long j;
        int i5;
        if (onBattery2 != this.mOnBattery) {
            this.mHistoryCur.batteryLevel = (byte) b4;
            this.mHistoryCur.batteryStatus = (byte) b;
            this.mHistoryCur.batteryHealth = (byte) b2;
            this.mHistoryCur.batteryPlugType = (byte) b3;
            this.mHistoryCur.batteryTemperature = (short) temp2;
            this.mHistoryCur.batteryVoltage = (char) i2;
            if (i3 < this.mHistoryCur.batteryChargeUAh) {
                long chargeDiff = (long) (this.mHistoryCur.batteryChargeUAh - i3);
                this.mDischargeCounter.addCountLocked(chargeDiff);
                this.mDischargeScreenOffCounter.addCountLocked(chargeDiff);
                if (isScreenDoze(this.mScreenState)) {
                    this.mDischargeScreenDozeCounter.addCountLocked(chargeDiff);
                }
                if (this.mDeviceIdleMode == 1) {
                    this.mDischargeLightDozeCounter.addCountLocked(chargeDiff);
                } else if (this.mDeviceIdleMode == 2) {
                    this.mDischargeDeepDozeCounter.addCountLocked(chargeDiff);
                }
            }
            this.mHistoryCur.batteryChargeUAh = i3;
            onBattery = onBattery2;
            i2 = temp2;
            setOnBatteryLocked(elapsedRealtime, uptime, onBattery2, oldStatus, b4, i3);
            int i6 = i2;
            long j2 = elapsedRealtime;
            j = uptime;
            i5 = volt;
        } else {
            boolean z2;
            onBattery = onBattery2;
            i2 = temp2;
            int i7 = oldStatus;
            z = false;
            if (this.mHistoryCur.batteryLevel != b4) {
                this.mHistoryCur.batteryLevel = (byte) b4;
                z = true;
                this.mExternalSync.scheduleSyncDueToBatteryLevelChange(this.mConstants.BATTERY_LEVEL_COLLECTION_DELAY_MS);
            }
            if (this.mHistoryCur.batteryStatus != b) {
                this.mHistoryCur.batteryStatus = (byte) b;
                z = true;
            }
            if (this.mHistoryCur.batteryHealth != b2) {
                this.mHistoryCur.batteryHealth = (byte) b2;
                z = true;
            }
            if (this.mHistoryCur.batteryPlugType != b3) {
                this.mHistoryCur.batteryPlugType = (byte) b3;
                z = true;
            }
            if (i2 >= this.mHistoryCur.batteryTemperature + 10 || i2 <= this.mHistoryCur.batteryTemperature - 10) {
                this.mHistoryCur.batteryTemperature = (short) i2;
                z = true;
            }
            i5 = volt;
            if (i5 > this.mHistoryCur.batteryVoltage + 20 || i5 < this.mHistoryCur.batteryVoltage - 20) {
                this.mHistoryCur.batteryVoltage = (char) i5;
                z = true;
            }
            if (i3 >= this.mHistoryCur.batteryChargeUAh + 10 || i3 <= this.mHistoryCur.batteryChargeUAh - 10) {
                if (i3 < this.mHistoryCur.batteryChargeUAh) {
                    j = (long) (this.mHistoryCur.batteryChargeUAh - i3);
                    this.mDischargeCounter.addCountLocked(j);
                    this.mDischargeScreenOffCounter.addCountLocked(j);
                    if (isScreenDoze(this.mScreenState)) {
                        this.mDischargeScreenDozeCounter.addCountLocked(j);
                    }
                    z2 = true;
                    if (this.mDeviceIdleMode == 1) {
                        this.mDischargeLightDozeCounter.addCountLocked(j);
                    } else if (this.mDeviceIdleMode == 2) {
                        this.mDischargeDeepDozeCounter.addCountLocked(j);
                    }
                } else {
                    z2 = true;
                }
                this.mHistoryCur.batteryChargeUAh = i3;
                z = true;
            } else {
                z2 = true;
            }
            elapsedRealtime2 = ((((long) this.mInitStepMode) << 48) | (((long) this.mModStepMode) << 56)) | (((long) (b4 & 255)) << 40);
            long j3;
            long j4;
            if (onBattery) {
                z |= setChargingLocked(false);
                if (this.mLastDischargeStepLevel != b4 && this.mMinDischargeStepLevel > b4) {
                    j3 = elapsedRealtime2;
                    j4 = elapsedRealtime;
                    this.mDischargeStepTracker.addLevelSteps(this.mLastDischargeStepLevel - b4, j3, j4);
                    this.mDailyDischargeStepTracker.addLevelSteps(this.mLastDischargeStepLevel - b4, j3, j4);
                    this.mLastDischargeStepLevel = b4;
                    this.mMinDischargeStepLevel = b4;
                    this.mInitStepMode = this.mCurStepMode;
                    this.mModStepMode = 0;
                }
            } else {
                if (b4 >= (byte) 90) {
                    z |= setChargingLocked(z2);
                    this.mLastChargeStepLevel = b4;
                }
                if (this.mCharging) {
                    if (this.mLastChargeStepLevel > b4) {
                        z |= setChargingLocked(false);
                        this.mLastChargeStepLevel = b4;
                    }
                } else if (this.mLastChargeStepLevel < b4) {
                    z |= setChargingLocked(z2);
                    this.mLastChargeStepLevel = b4;
                }
                if (this.mLastChargeStepLevel != b4 && this.mMaxChargeStepLevel < b4) {
                    j3 = elapsedRealtime2;
                    j4 = elapsedRealtime;
                    this.mChargeStepTracker.addLevelSteps(b4 - this.mLastChargeStepLevel, j3, j4);
                    this.mDailyChargeStepTracker.addLevelSteps(b4 - this.mLastChargeStepLevel, j3, j4);
                    this.mLastChargeStepLevel = b4;
                    this.mMaxChargeStepLevel = b4;
                    this.mInitStepMode = this.mCurStepMode;
                    this.mModStepMode = 0;
                }
            }
            if (z) {
                addHistoryRecordLocked(elapsedRealtime, uptime);
            } else {
                j = uptime;
            }
        }
        if (!onBattery && (b == (byte) 5 || b == (byte) 1)) {
            this.mRecordingHistory = false;
        }
        if (this.mMinLearnedBatteryCapacity == -1) {
            i = chargeFullUAh;
            this.mMinLearnedBatteryCapacity = i;
        } else {
            i = chargeFullUAh;
            Math.min(this.mMinLearnedBatteryCapacity, i);
        }
        this.mMaxLearnedBatteryCapacity = Math.max(this.mMaxLearnedBatteryCapacity, i);
    }

    public static boolean isOnBattery(int plugType, int status) {
        return plugType == 0 && status != 1;
    }

    private void reportChangesToStatsLog(HistoryItem recentPast, int status, int plugType, int level) {
        if (recentPast == null || recentPast.batteryStatus != status) {
            StatsLog.write(31, status);
        }
        if (recentPast == null || recentPast.batteryPlugType != plugType) {
            StatsLog.write(32, plugType);
        }
        if (recentPast == null || recentPast.batteryLevel != level) {
            StatsLog.write(30, level);
        }
    }

    public long getAwakeTimeBattery() {
        return computeBatteryUptime(getBatteryUptimeLocked(), 1);
    }

    public long getAwakeTimePlugged() {
        return (this.mClocks.uptimeMillis() * 1000) - getAwakeTimeBattery();
    }

    public long computeUptime(long curTime, int which) {
        switch (which) {
            case 0:
                return this.mUptime + (curTime - this.mUptimeStart);
            case 1:
                return curTime - this.mUptimeStart;
            case 2:
                return curTime - this.mOnBatteryTimeBase.getUptimeStart();
            default:
                return 0;
        }
    }

    public long computeRealtime(long curTime, int which) {
        switch (which) {
            case 0:
                return this.mRealtime + (curTime - this.mRealtimeStart);
            case 1:
                return curTime - this.mRealtimeStart;
            case 2:
                return curTime - this.mOnBatteryTimeBase.getRealtimeStart();
            default:
                return 0;
        }
    }

    public long computeBatteryUptime(long curTime, int which) {
        if (this.mOnBatteryTimeBase != null) {
            return this.mOnBatteryTimeBase.computeUptime(curTime, which);
        }
        return 0;
    }

    public long computeBatteryRealtime(long curTime, int which) {
        if (this.mOnBatteryTimeBase != null) {
            return this.mOnBatteryTimeBase.computeRealtime(curTime, which);
        }
        return 0;
    }

    public long computeBatteryScreenOffUptime(long curTime, int which) {
        if (this.mOnBatteryScreenOffTimeBase != null) {
            return this.mOnBatteryScreenOffTimeBase.computeUptime(curTime, which);
        }
        return 0;
    }

    public long computeBatteryScreenOffRealtime(long curTime, int which) {
        if (this.mOnBatteryScreenOffTimeBase != null) {
            return this.mOnBatteryScreenOffTimeBase.computeRealtime(curTime, which);
        }
        return 0;
    }

    private long computeTimePerLevel(long[] steps, int numSteps) {
        if (numSteps <= 0 || steps == null) {
            return -1;
        }
        if (steps == null || numSteps <= steps.length) {
            long total = 0;
            for (int i = 0; i < numSteps; i++) {
                total += steps[i] & 1099511627775L;
            }
            return total / ((long) numSteps);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("numSteps > steps.length, numSteps = ");
        stringBuilder.append(numSteps);
        stringBuilder.append(",steps.length = ");
        stringBuilder.append(steps.length);
        Slog.wtf(str, stringBuilder.toString());
        return -1;
    }

    public long computeBatteryTimeRemaining(long curTime) {
        if (!this.mOnBattery || this.mDischargeStepTracker.mNumStepDurations < 1) {
            return -1;
        }
        long msPerLevel = this.mDischargeStepTracker.computeTimePerLevel();
        if (msPerLevel <= 0) {
            return -1;
        }
        return (((long) this.mCurrentBatteryLevel) * msPerLevel) * 1000;
    }

    public LevelStepTracker getDischargeLevelStepTracker() {
        return this.mDischargeStepTracker;
    }

    public LevelStepTracker getDailyDischargeLevelStepTracker() {
        return this.mDailyDischargeStepTracker;
    }

    public long computeChargeTimeRemaining(long curTime) {
        if (this.mOnBattery || this.mChargeStepTracker.mNumStepDurations < 1) {
            return -1;
        }
        long msPerLevel = this.mChargeStepTracker.computeTimePerLevel();
        if (msPerLevel <= 0) {
            return -1;
        }
        return (((long) (100 - this.mCurrentBatteryLevel)) * msPerLevel) * 1000;
    }

    public CellularBatteryStats getCellularBatteryStats() {
        long[] timeInRatMs;
        int i;
        CellularBatteryStats s = new CellularBatteryStats();
        int which = 0;
        long rawRealTime = SystemClock.elapsedRealtime() * 1000;
        ControllerActivityCounter counter = getModemControllerActivity();
        long sleepTimeMs = counter.getSleepTimeCounter().getCountLocked(0);
        long idleTimeMs = counter.getIdleTimeCounter().getCountLocked(0);
        long rxTimeMs = counter.getRxTimeCounter().getCountLocked(0);
        long energyConsumedMaMs = counter.getPowerCounter().getCountLocked(0);
        long[] timeInRatMs2 = new long[21];
        int i2 = 0;
        while (true) {
            int which2 = which;
            which = i2;
            if (which >= timeInRatMs2.length) {
                break;
            }
            timeInRatMs2[which] = getPhoneDataConnectionTime(which, rawRealTime, 0) / 1000;
            i2 = which + 1;
            which = which2;
        }
        long[] timeInRxSignalStrengthLevelMs = new long[5];
        i2 = 0;
        while (true) {
            timeInRatMs = timeInRatMs2;
            i = i2;
            if (i >= timeInRxSignalStrengthLevelMs.length) {
                break;
            }
            timeInRxSignalStrengthLevelMs[i] = getPhoneSignalStrengthTime(i, rawRealTime, 0) / 1000;
            i2 = i + 1;
            timeInRatMs2 = timeInRatMs;
        }
        long[] txTimeMs = new long[Math.min(5, counter.getTxTimeCounters().length)];
        long totalTxTimeMs = 0;
        i = 0;
        while (true) {
            long[] timeInRxSignalStrengthLevelMs2 = timeInRxSignalStrengthLevelMs;
            if (i < txTimeMs.length) {
                ControllerActivityCounter counter2 = counter;
                txTimeMs[i] = counter.getTxTimeCounters()[i].getCountLocked(null);
                totalTxTimeMs += txTimeMs[i];
                i++;
                timeInRxSignalStrengthLevelMs = timeInRxSignalStrengthLevelMs2;
                counter = counter2;
            } else {
                s.setLoggingDurationMs(computeBatteryRealtime(rawRealTime, 0) / 1000);
                s.setKernelActiveTimeMs(getMobileRadioActiveTime(rawRealTime, 0) / 1000);
                s.setNumPacketsTx(getNetworkActivityPackets(1, 0));
                s.setNumBytesTx(getNetworkActivityBytes(1, 0));
                s.setNumPacketsRx(getNetworkActivityPackets(0, 0));
                s.setNumBytesRx(getNetworkActivityBytes(0, 0));
                s.setSleepTimeMs(sleepTimeMs);
                s.setIdleTimeMs(idleTimeMs);
                s.setRxTimeMs(rxTimeMs);
                s.setEnergyConsumedMaMs(energyConsumedMaMs);
                s.setTimeInRatMs(timeInRatMs);
                s.setTimeInRxSignalStrengthLevelMs(timeInRxSignalStrengthLevelMs2);
                s.setTxTimeMs(txTimeMs);
                return s;
            }
        }
    }

    public WifiBatteryStats getWifiBatteryStats() {
        int i;
        WifiBatteryStats s = new WifiBatteryStats();
        int which = 0;
        long rawRealTime = SystemClock.elapsedRealtime() * 1000;
        ControllerActivityCounter counter = getWifiControllerActivity();
        long idleTimeMs = counter.getIdleTimeCounter().getCountLocked(0);
        long scanTimeMs = counter.getScanTimeCounter().getCountLocked(0);
        long rxTimeMs = counter.getRxTimeCounter().getCountLocked(0);
        long txTimeMs = counter.getTxTimeCounters()[0].getCountLocked(0);
        long scanTimeMs2 = scanTimeMs;
        scanTimeMs = computeBatteryRealtime(SystemClock.elapsedRealtime() * 1000, 0) / 1000;
        long idleTimeMs2 = idleTimeMs;
        long sleepTimeMs = scanTimeMs - ((idleTimeMs + rxTimeMs) + txTimeMs);
        long energyConsumedMaMs = counter.getPowerCounter().getCountLocked(0);
        scanTimeMs = 0;
        int i2 = 0;
        while (true) {
            int which2 = which;
            if (i2 >= this.mUidStats.size()) {
                break;
            }
            scanTimeMs += (long) ((Uid) this.mUidStats.valueAt(i2)).mWifiScanTimer.getCountLocked(0);
            i2++;
            which = which2;
            energyConsumedMaMs = energyConsumedMaMs;
        }
        long energyConsumedMaMs2 = energyConsumedMaMs;
        long[] timeInStateMs = new long[8];
        for (i = 0; i < 8; i++) {
            timeInStateMs[i] = getWifiStateTime(i, rawRealTime, 0) / 1000;
        }
        which = 13;
        long[] timeInSupplStateMs = new long[13];
        i2 = 0;
        while (i2 < which) {
            timeInSupplStateMs[i2] = getWifiSupplStateTime(i2, rawRealTime, 0) / 1000;
            i2++;
            which = 13;
        }
        which = 5;
        long[] timeSignalStrengthTimeMs = new long[5];
        int i3 = 0;
        while (true) {
            long[] timeInSupplStateMs2 = timeInSupplStateMs;
            i = i3;
            if (i < which) {
                timeSignalStrengthTimeMs[i] = getWifiSignalStrengthTime(i, rawRealTime, 0) / 1000;
                i3 = i + 1;
                timeInSupplStateMs = timeInSupplStateMs2;
                which = 5;
            } else {
                long rawRealTime2 = rawRealTime;
                s.setLoggingDurationMs(computeBatteryRealtime(rawRealTime, 0) / 1000);
                rawRealTime = rawRealTime2;
                s.setKernelActiveTimeMs(getWifiActiveTime(rawRealTime, 0) / 1000);
                long[] timeInStateMs2 = timeInStateMs;
                s.setNumPacketsTx(getNetworkActivityPackets(3, 0));
                s.setNumBytesTx(getNetworkActivityBytes(3, 0));
                s.setNumPacketsRx(getNetworkActivityPackets(2, 0));
                s.setNumBytesRx(getNetworkActivityBytes(2, 0));
                s.setSleepTimeMs(sleepTimeMs);
                long idleTimeMs3 = idleTimeMs2;
                s.setIdleTimeMs(idleTimeMs3);
                s.setRxTimeMs(rxTimeMs);
                s.setTxTimeMs(txTimeMs);
                s.setScanTimeMs(scanTimeMs2);
                s.setEnergyConsumedMaMs(energyConsumedMaMs2);
                s.setNumAppScanRequest(scanTimeMs);
                s.setTimeInStateMs(timeInStateMs2);
                s.setTimeInSupplicantStateMs(timeInSupplStateMs2);
                s.setTimeInRxSignalStrengthLevelMs(timeSignalStrengthTimeMs);
                return s;
            }
        }
    }

    public GpsBatteryStats getGpsBatteryStats() {
        GpsBatteryStats s = new GpsBatteryStats();
        long rawRealTime = SystemClock.elapsedRealtime() * 1000;
        s.setLoggingDurationMs(computeBatteryRealtime(rawRealTime, 0) / 1000);
        s.setEnergyConsumedMaMs(getGpsBatteryDrainMaMs());
        long[] time = new long[2];
        for (int i = 0; i < time.length; i++) {
            time[i] = getGpsSignalQualityTime(i, rawRealTime, 0) / 1000;
        }
        s.setTimeInGpsSignalQualityLevel(time);
        return s;
    }

    public LevelStepTracker getChargeLevelStepTracker() {
        return this.mChargeStepTracker;
    }

    public LevelStepTracker getDailyChargeLevelStepTracker() {
        return this.mDailyChargeStepTracker;
    }

    public ArrayList<PackageChange> getDailyPackageChanges() {
        return this.mDailyPackageChanges;
    }

    protected long getBatteryUptimeLocked() {
        if (this.mOnBatteryTimeBase != null) {
            return this.mOnBatteryTimeBase.getUptime(this.mClocks.uptimeMillis() * 1000);
        }
        return 0;
    }

    public long getBatteryUptime(long curTime) {
        if (this.mOnBatteryTimeBase != null) {
            return this.mOnBatteryTimeBase.getUptime(curTime);
        }
        return 0;
    }

    public long getBatteryRealtime(long curTime) {
        if (this.mOnBatteryTimeBase != null) {
            return this.mOnBatteryTimeBase.getRealtime(curTime);
        }
        return 0;
    }

    public int getDischargeStartLevel() {
        int dischargeStartLevelLocked;
        synchronized (this) {
            dischargeStartLevelLocked = getDischargeStartLevelLocked();
        }
        return dischargeStartLevelLocked;
    }

    public int getDischargeStartLevelLocked() {
        return this.mDischargeUnplugLevel;
    }

    public int getDischargeCurrentLevel() {
        int dischargeCurrentLevelLocked;
        synchronized (this) {
            dischargeCurrentLevelLocked = getDischargeCurrentLevelLocked();
        }
        return dischargeCurrentLevelLocked;
    }

    public int getDischargeCurrentLevelLocked() {
        return this.mDischargeCurrentLevel;
    }

    public int getLowDischargeAmountSinceCharge() {
        int val;
        synchronized (this) {
            val = this.mLowDischargeAmountSinceCharge;
            if (this.mOnBattery && this.mDischargeCurrentLevel < this.mDischargeUnplugLevel) {
                val += (this.mDischargeUnplugLevel - this.mDischargeCurrentLevel) - 1;
            }
        }
        return val;
    }

    public int getHighDischargeAmountSinceCharge() {
        int val;
        synchronized (this) {
            val = this.mHighDischargeAmountSinceCharge;
            if (this.mOnBattery && this.mDischargeCurrentLevel < this.mDischargeUnplugLevel) {
                val += this.mDischargeUnplugLevel - this.mDischargeCurrentLevel;
            }
        }
        return val;
    }

    public int getDischargeAmount(int which) {
        int dischargeAmount;
        if (which == 0) {
            dischargeAmount = getHighDischargeAmountSinceCharge();
        } else {
            dischargeAmount = getDischargeStartLevel() - getDischargeCurrentLevel();
        }
        if (dischargeAmount < 0) {
            return 0;
        }
        return dischargeAmount;
    }

    public int getDischargeAmountScreenOn() {
        int val;
        synchronized (this) {
            val = this.mDischargeAmountScreenOn;
            if (this.mOnBattery && isScreenOn(this.mScreenState) && this.mDischargeCurrentLevel < this.mDischargeScreenOnUnplugLevel) {
                val += this.mDischargeScreenOnUnplugLevel - this.mDischargeCurrentLevel;
            }
        }
        return val;
    }

    public int getDischargeAmountScreenOnSinceCharge() {
        int val;
        synchronized (this) {
            val = this.mDischargeAmountScreenOnSinceCharge;
            if (this.mOnBattery && isScreenOn(this.mScreenState) && this.mDischargeCurrentLevel < this.mDischargeScreenOnUnplugLevel) {
                val += this.mDischargeScreenOnUnplugLevel - this.mDischargeCurrentLevel;
            }
        }
        return val;
    }

    public int getDischargeAmountScreenOff() {
        int dischargeAmountScreenDoze;
        synchronized (this) {
            int val = this.mDischargeAmountScreenOff;
            if (this.mOnBattery && isScreenOff(this.mScreenState) && this.mDischargeCurrentLevel < this.mDischargeScreenOffUnplugLevel) {
                val += this.mDischargeScreenOffUnplugLevel - this.mDischargeCurrentLevel;
            }
            dischargeAmountScreenDoze = getDischargeAmountScreenDoze() + val;
        }
        return dischargeAmountScreenDoze;
    }

    public int getDischargeAmountScreenOffSinceCharge() {
        int dischargeAmountScreenDozeSinceCharge;
        synchronized (this) {
            int val = this.mDischargeAmountScreenOffSinceCharge;
            if (this.mOnBattery && isScreenOff(this.mScreenState) && this.mDischargeCurrentLevel < this.mDischargeScreenOffUnplugLevel) {
                val += this.mDischargeScreenOffUnplugLevel - this.mDischargeCurrentLevel;
            }
            dischargeAmountScreenDozeSinceCharge = getDischargeAmountScreenDozeSinceCharge() + val;
        }
        return dischargeAmountScreenDozeSinceCharge;
    }

    public int getDischargeAmountScreenDoze() {
        int val;
        synchronized (this) {
            val = this.mDischargeAmountScreenDoze;
            if (this.mOnBattery && isScreenDoze(this.mScreenState) && this.mDischargeCurrentLevel < this.mDischargeScreenDozeUnplugLevel) {
                val += this.mDischargeScreenDozeUnplugLevel - this.mDischargeCurrentLevel;
            }
        }
        return val;
    }

    public int getDischargeAmountScreenDozeSinceCharge() {
        int val;
        synchronized (this) {
            val = this.mDischargeAmountScreenDozeSinceCharge;
            if (this.mOnBattery && isScreenDoze(this.mScreenState) && this.mDischargeCurrentLevel < this.mDischargeScreenDozeUnplugLevel) {
                val += this.mDischargeScreenDozeUnplugLevel - this.mDischargeCurrentLevel;
            }
        }
        return val;
    }

    public Uid getUidStatsLocked(int uid) {
        Uid u = (Uid) this.mUidStats.get(uid);
        if (u != null) {
            return u;
        }
        u = new Uid(this, uid);
        this.mUidStats.put(uid, u);
        return u;
    }

    public Uid getAvailableUidStatsLocked(int uid) {
        return (Uid) this.mUidStats.get(uid);
    }

    public void onCleanupUserLocked(int userId) {
        int firstUidForUser = UserHandle.getUid(userId, 0);
        this.mPendingRemovedUids.add(new UidToRemove(firstUidForUser, UserHandle.getUid(userId, 99999), this.mClocks.elapsedRealtime()));
    }

    public void onUserRemovedLocked(int userId) {
        int firstUidForUser = UserHandle.getUid(userId, 0);
        int lastUidForUser = UserHandle.getUid(userId, 99999);
        this.mUidStats.put(firstUidForUser, null);
        this.mUidStats.put(lastUidForUser, null);
        int firstIndex = this.mUidStats.indexOfKey(firstUidForUser);
        this.mUidStats.removeAtRange(firstIndex, (this.mUidStats.indexOfKey(lastUidForUser) - firstIndex) + 1);
    }

    public void removeUidStatsLocked(int uid) {
        this.mUidStats.remove(uid);
        this.mPendingRemovedUids.add(new UidToRemove(this, uid, this.mClocks.elapsedRealtime()));
    }

    public Proc getProcessStatsLocked(int uid, String name) {
        return getUidStatsLocked(mapUid(uid)).getProcessStatsLocked(name);
    }

    public Pkg getPackageStatsLocked(int uid, String pkg) {
        return getUidStatsLocked(mapUid(uid)).getPackageStatsLocked(pkg);
    }

    public Serv getServiceStatsLocked(int uid, String pkg, String name) {
        return getUidStatsLocked(mapUid(uid)).getServiceStatsLocked(pkg, name);
    }

    public void shutdownLocked() {
        recordShutdownLocked(this.mClocks.elapsedRealtime(), this.mClocks.uptimeMillis());
        writeSyncLocked();
        this.mShuttingDown = true;
    }

    public boolean trackPerProcStateCpuTimes() {
        return this.mConstants.TRACK_CPU_TIMES_BY_PROC_STATE && this.mPerProcStateCpuTimesAvailable;
    }

    public void systemServicesReady(Context context) {
        this.mConstants.startObserving(context.getContentResolver());
        registerUsbStateReceiver(context);
    }

    public long getExternalStatsCollectionRateLimitMs() {
        long j;
        synchronized (this) {
            j = this.mConstants.EXTERNAL_STATS_COLLECTION_RATE_LIMIT_MS;
        }
        return j;
    }

    @GuardedBy("this")
    public void dumpConstantsLocked(PrintWriter pw) {
        this.mConstants.dumpLocked(pw);
    }

    @GuardedBy("this")
    public void dumpCpuStatsLocked(PrintWriter pw) {
        int i;
        int u;
        Uid uid;
        long[] times;
        int size = this.mUidStats.size();
        pw.println("Per UID CPU user & system time in ms:");
        for (i = 0; i < size; i++) {
            u = this.mUidStats.keyAt(i);
            uid = (Uid) this.mUidStats.get(u);
            pw.print("  ");
            pw.print(u);
            pw.print(": ");
            pw.print(uid.getUserCpuTimeUs(0) / 1000);
            pw.print(" ");
            pw.println(uid.getSystemCpuTimeUs(0) / 1000);
        }
        pw.println("Per UID CPU active time in ms:");
        for (i = 0; i < size; i++) {
            u = this.mUidStats.keyAt(i);
            uid = (Uid) this.mUidStats.get(u);
            if (uid.getCpuActiveTime() > 0) {
                pw.print("  ");
                pw.print(u);
                pw.print(": ");
                pw.println(uid.getCpuActiveTime());
            }
        }
        pw.println("Per UID CPU cluster time in ms:");
        for (i = 0; i < size; i++) {
            u = this.mUidStats.keyAt(i);
            times = ((Uid) this.mUidStats.get(u)).getCpuClusterTimes();
            if (times != null) {
                pw.print("  ");
                pw.print(u);
                pw.print(": ");
                pw.println(Arrays.toString(times));
            }
        }
        pw.println("Per UID CPU frequency time in ms:");
        for (i = 0; i < size; i++) {
            u = this.mUidStats.keyAt(i);
            times = ((Uid) this.mUidStats.get(u)).getCpuFreqTimes(0);
            if (times != null) {
                pw.print("  ");
                pw.print(u);
                pw.print(": ");
                pw.println(Arrays.toString(times));
            }
        }
    }

    public void writeAsyncLocked() {
        writeLocked(false);
    }

    public void writeSyncLocked() {
        writeLocked(true);
    }

    void writeLocked(boolean sync) {
        if (this.mFile == null) {
            Slog.w("BatteryStats", "writeLocked: no file associated with this instance");
        } else if (!this.mShuttingDown) {
            Parcel out = Parcel.obtain();
            writeSummaryToParcel(out, true);
            this.mLastWriteTime = this.mClocks.elapsedRealtime();
            if (this.mPendingWrite != null) {
                this.mPendingWrite.recycle();
            }
            this.mPendingWrite = out;
            if (sync) {
                commitPendingDataToDisk();
            } else {
                BackgroundThread.getHandler().post(new Runnable() {
                    public void run() {
                        BatteryStatsImpl.this.commitPendingDataToDisk();
                    }
                });
            }
        }
    }

    /* JADX WARNING: Missing block: B:7:0x000b, code skipped:
            r7.mWriteLock.lock();
     */
    /* JADX WARNING: Missing block: B:9:?, code skipped:
            r1 = android.os.SystemClock.uptimeMillis();
            r3 = new java.io.FileOutputStream(r7.mFile.chooseForWrite());
            r3.write(r0.marshall());
            r3.flush();
            android.os.FileUtils.sync(r3);
            r3.close();
            r7.mFile.commit();
            com.android.internal.logging.EventLogTags.writeCommitSysConfigFile("batterystats", android.os.SystemClock.uptimeMillis() - r1);
     */
    /* JADX WARNING: Missing block: B:11:0x0041, code skipped:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:13:?, code skipped:
            android.util.Slog.w("BatteryStats", "Error writing battery statistics", r1);
            r7.mFile.rollback();
     */
    /* JADX WARNING: Missing block: B:16:0x0058, code skipped:
            r0.recycle();
            r7.mWriteLock.unlock();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void commitPendingDataToDisk() {
        Parcel next;
        synchronized (this) {
            next = this.mPendingWrite;
            this.mPendingWrite = null;
            if (next == null) {
                return;
            }
        }
        next.recycle();
        this.mWriteLock.unlock();
    }

    public void readLocked() {
        if (this.mDailyFile != null) {
            readDailyStatsLocked();
        }
        if (this.mFile == null) {
            Slog.w("BatteryStats", "readLocked: no file associated with this instance");
            return;
        }
        this.mUidStats.clear();
        try {
            File file = this.mFile.chooseForRead();
            if (file.exists()) {
                FileInputStream stream = new FileInputStream(file);
                byte[] raw = BatteryStatsHelper.readFully(stream);
                Parcel in = Parcel.obtain();
                in.unmarshall(raw, 0, raw.length);
                in.setDataPosition(0);
                stream.close();
                readSummaryFromParcel(in);
                this.mEndPlatformVersion = Build.ID;
                if (this.mHistoryBuffer.dataPosition() > 0) {
                    this.mRecordingHistory = true;
                    long elapsedRealtime = this.mClocks.elapsedRealtime();
                    long uptime = this.mClocks.uptimeMillis();
                    addHistoryBufferLocked(elapsedRealtime, (byte) 4, this.mHistoryCur);
                    startRecordingHistory(elapsedRealtime, uptime, false);
                }
                recordDailyStatsIfNeededLocked(false);
            }
        } catch (Exception e) {
            Slog.e("BatteryStats", "Error reading battery statistics", e);
            resetAllStatsLocked();
        }
    }

    public int describeContents() {
        return 0;
    }

    void readHistory(Parcel in, boolean andOldHistory) throws ParcelFormatException {
        int idx;
        long historyBaseTime = in.readLong();
        int i = 0;
        this.mHistoryBuffer.setDataSize(0);
        this.mHistoryBuffer.setDataPosition(0);
        this.mHistoryTagPool.clear();
        this.mNextHistoryTagIdx = 0;
        this.mNumHistoryTagChars = 0;
        int numTags = in.readInt();
        while (i < numTags) {
            idx = in.readInt();
            String str = in.readString();
            if (str != null) {
                int uid = in.readInt();
                HistoryTag tag = new HistoryTag();
                tag.string = str;
                tag.uid = uid;
                tag.poolIdx = idx;
                this.mHistoryTagPool.put(tag, Integer.valueOf(idx));
                if (idx >= this.mNextHistoryTagIdx) {
                    this.mNextHistoryTagIdx = idx + 1;
                }
                this.mNumHistoryTagChars += tag.string.length() + 1;
                i++;
            } else {
                throw new ParcelFormatException("null history tag string");
            }
        }
        i = in.readInt();
        idx = in.dataPosition();
        StringBuilder stringBuilder;
        if (i >= MAX_MAX_HISTORY_BUFFER * 3) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("File corrupt: history data buffer too large ");
            stringBuilder.append(i);
            throw new ParcelFormatException(stringBuilder.toString());
        } else if ((i & -4) == i) {
            this.mHistoryBuffer.appendFrom(in, idx, i);
            in.setDataPosition(idx + i);
            if (andOldHistory) {
                readOldHistory(in);
            }
            this.mHistoryBaseTime = historyBaseTime;
            if (this.mHistoryBaseTime > 0) {
                this.mHistoryBaseTime = (this.mHistoryBaseTime - this.mClocks.elapsedRealtime()) + 1;
            }
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("File corrupt: history data buffer not aligned ");
            stringBuilder.append(i);
            throw new ParcelFormatException(stringBuilder.toString());
        }
    }

    void readOldHistory(Parcel in) {
    }

    void writeHistory(Parcel out, boolean inclData, boolean andOldHistory) {
        out.writeLong(this.mHistoryBaseTime + this.mLastHistoryElapsedRealtime);
        if (inclData) {
            out.writeInt(this.mHistoryTagPool.size());
            for (Entry<HistoryTag, Integer> ent : this.mHistoryTagPool.entrySet()) {
                HistoryTag tag = (HistoryTag) ent.getKey();
                out.writeInt(((Integer) ent.getValue()).intValue());
                out.writeString(tag.string);
                out.writeInt(tag.uid);
            }
            out.writeInt(this.mHistoryBuffer.dataSize());
            out.appendFrom(this.mHistoryBuffer, 0, this.mHistoryBuffer.dataSize());
            if (andOldHistory) {
                writeOldHistory(out);
            }
            return;
        }
        out.writeInt(0);
        out.writeInt(0);
    }

    void writeOldHistory(Parcel out) {
    }

    public void readSummaryFromParcel(Parcel in) throws ParcelFormatException {
        Parcel parcel = in;
        int version = in.readInt();
        if (version != 177) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("readFromParcel: version got ");
            stringBuilder.append(version);
            stringBuilder.append(", expected ");
            stringBuilder.append(177);
            stringBuilder.append("; erasing old stats");
            Slog.w("BatteryStats", stringBuilder.toString());
            return;
        }
        int i;
        readHistory(parcel, true);
        this.mStartCount = in.readInt();
        this.mUptime = in.readLong();
        this.mRealtime = in.readLong();
        this.mStartClockTime = in.readLong();
        this.mStartPlatformVersion = in.readString();
        this.mEndPlatformVersion = in.readString();
        this.mOnBatteryTimeBase.readSummaryFromParcel(parcel);
        this.mOnBatteryScreenOffTimeBase.readSummaryFromParcel(parcel);
        this.mDischargeUnplugLevel = in.readInt();
        this.mDischargePlugLevel = in.readInt();
        this.mDischargeCurrentLevel = in.readInt();
        this.mCurrentBatteryLevel = in.readInt();
        this.mEstimatedBatteryCapacity = in.readInt();
        this.mMinLearnedBatteryCapacity = in.readInt();
        this.mMaxLearnedBatteryCapacity = in.readInt();
        this.mLowDischargeAmountSinceCharge = in.readInt();
        this.mHighDischargeAmountSinceCharge = in.readInt();
        this.mDischargeAmountScreenOnSinceCharge = in.readInt();
        this.mDischargeAmountScreenOffSinceCharge = in.readInt();
        this.mDischargeAmountScreenDozeSinceCharge = in.readInt();
        this.mDischargeStepTracker.readFromParcel(parcel);
        this.mChargeStepTracker.readFromParcel(parcel);
        this.mDailyDischargeStepTracker.readFromParcel(parcel);
        this.mDailyChargeStepTracker.readFromParcel(parcel);
        this.mDischargeCounter.readSummaryFromParcelLocked(parcel);
        this.mDischargeScreenOffCounter.readSummaryFromParcelLocked(parcel);
        this.mDischargeScreenDozeCounter.readSummaryFromParcelLocked(parcel);
        this.mDischargeLightDozeCounter.readSummaryFromParcelLocked(parcel);
        this.mDischargeDeepDozeCounter.readSummaryFromParcelLocked(parcel);
        int NPKG = in.readInt();
        Parcel NS = null;
        boolean z = false;
        if (NPKG > 0) {
            this.mDailyPackageChanges = new ArrayList(NPKG);
            while (NPKG > 0) {
                NPKG--;
                PackageChange pc = new PackageChange();
                pc.mPackageName = in.readString();
                pc.mUpdate = in.readInt() != 0;
                pc.mVersionCode = in.readLong();
                this.mDailyPackageChanges.add(pc);
            }
        } else {
            this.mDailyPackageChanges = null;
        }
        this.mDailyStartTime = in.readLong();
        this.mNextMinDailyDeadline = in.readLong();
        this.mNextMaxDailyDeadline = in.readLong();
        this.mStartCount++;
        this.mScreenState = 0;
        this.mScreenOnTimer.readSummaryFromParcelLocked(parcel);
        this.mScreenDozeTimer.readSummaryFromParcelLocked(parcel);
        for (i = 0; i < 5; i++) {
            this.mScreenBrightnessTimer[i].readSummaryFromParcelLocked(parcel);
        }
        this.mInteractive = false;
        this.mInteractiveTimer.readSummaryFromParcelLocked(parcel);
        this.mPhoneOn = false;
        this.mPowerSaveModeEnabledTimer.readSummaryFromParcelLocked(parcel);
        this.mLongestLightIdleTime = in.readLong();
        this.mLongestFullIdleTime = in.readLong();
        this.mDeviceIdleModeLightTimer.readSummaryFromParcelLocked(parcel);
        this.mDeviceIdleModeFullTimer.readSummaryFromParcelLocked(parcel);
        this.mDeviceLightIdlingTimer.readSummaryFromParcelLocked(parcel);
        this.mDeviceIdlingTimer.readSummaryFromParcelLocked(parcel);
        this.mPhoneOnTimer.readSummaryFromParcelLocked(parcel);
        for (i = 0; i < 5; i++) {
            this.mPhoneSignalStrengthsTimer[i].readSummaryFromParcelLocked(parcel);
        }
        this.mPhoneSignalScanningTimer.readSummaryFromParcelLocked(parcel);
        for (i = 0; i < 21; i++) {
            this.mPhoneDataConnectionsTimer[i].readSummaryFromParcelLocked(parcel);
        }
        for (i = 0; i < 10; i++) {
            this.mNetworkByteActivityCounters[i].readSummaryFromParcelLocked(parcel);
            this.mNetworkPacketActivityCounters[i].readSummaryFromParcelLocked(parcel);
        }
        this.mMobileRadioPowerState = 1;
        this.mMobileRadioActiveTimer.readSummaryFromParcelLocked(parcel);
        this.mMobileRadioActivePerAppTimer.readSummaryFromParcelLocked(parcel);
        this.mMobileRadioActiveAdjustedTime.readSummaryFromParcelLocked(parcel);
        this.mMobileRadioActiveUnknownTime.readSummaryFromParcelLocked(parcel);
        this.mMobileRadioActiveUnknownCount.readSummaryFromParcelLocked(parcel);
        this.mWifiMulticastWakelockTimer.readSummaryFromParcelLocked(parcel);
        this.mWifiRadioPowerState = 1;
        this.mWifiOn = false;
        this.mWifiOnTimer.readSummaryFromParcelLocked(parcel);
        this.mGlobalWifiRunning = false;
        this.mGlobalWifiRunningTimer.readSummaryFromParcelLocked(parcel);
        for (i = 0; i < 8; i++) {
            this.mWifiStateTimer[i].readSummaryFromParcelLocked(parcel);
        }
        for (i = 0; i < 13; i++) {
            this.mWifiSupplStateTimer[i].readSummaryFromParcelLocked(parcel);
        }
        for (i = 0; i < 5; i++) {
            this.mWifiSignalStrengthsTimer[i].readSummaryFromParcelLocked(parcel);
        }
        this.mWifiActiveTimer.readSummaryFromParcelLocked(parcel);
        this.mWifiActivity.readSummaryFromParcel(parcel);
        for (i = 0; i < 2; i++) {
            this.mGpsSignalQualityTimer[i].readSummaryFromParcelLocked(parcel);
        }
        this.mBluetoothActivity.readSummaryFromParcel(parcel);
        this.mModemActivity.readSummaryFromParcel(parcel);
        this.mHasWifiReporting = in.readInt() != 0;
        this.mHasBluetoothReporting = in.readInt() != 0;
        this.mHasModemReporting = in.readInt() != 0;
        i = in.readInt();
        this.mLoadedNumConnectivityChange = i;
        this.mNumConnectivityChange = i;
        this.mFlashlightOnNesting = 0;
        this.mFlashlightOnTimer.readSummaryFromParcelLocked(parcel);
        this.mCameraOnNesting = 0;
        this.mCameraOnTimer.readSummaryFromParcelLocked(parcel);
        this.mBluetoothScanNesting = 0;
        this.mBluetoothScanTimer.readSummaryFromParcelLocked(parcel);
        this.mIsCellularTxPowerHigh = false;
        i = in.readInt();
        int NPKG2;
        StringBuilder stringBuilder2;
        int NRPMS;
        if (i <= PGAction.PG_ID_DEFAULT_FRONT) {
            int irpm;
            for (irpm = 0; irpm < i; irpm++) {
                if (in.readInt() != 0) {
                    getRpmTimerLocked(in.readString()).readSummaryFromParcelLocked(parcel);
                }
            }
            irpm = in.readInt();
            int NSORPMS;
            if (irpm <= PGAction.PG_ID_DEFAULT_FRONT) {
                int irpm2;
                for (irpm2 = 0; irpm2 < irpm; irpm2++) {
                    if (in.readInt() != 0) {
                        getScreenOffRpmTimerLocked(in.readString()).readSummaryFromParcelLocked(parcel);
                    }
                }
                irpm2 = in.readInt();
                int NKW;
                if (irpm2 <= PGAction.PG_ID_DEFAULT_FRONT) {
                    int ikw;
                    for (ikw = 0; ikw < irpm2; ikw++) {
                        if (in.readInt() != 0) {
                            getKernelWakelockTimerLocked(in.readString()).readSummaryFromParcelLocked(parcel);
                        }
                    }
                    ikw = in.readInt();
                    int NWR;
                    if (ikw <= PGAction.PG_ID_DEFAULT_FRONT) {
                        int iwr;
                        for (iwr = 0; iwr < ikw; iwr++) {
                            if (in.readInt() != 0) {
                                getWakeupReasonTimerLocked(in.readString()).readSummaryFromParcelLocked(parcel);
                            }
                        }
                        int NMS = in.readInt();
                        for (iwr = 0; iwr < NMS; iwr++) {
                            if (in.readInt() != 0) {
                                getKernelMemoryTimerLocked(in.readLong()).readSummaryFromParcelLocked(parcel);
                            }
                        }
                        int NU = in.readInt();
                        int NMS2;
                        int NU2;
                        if (NU <= PGAction.PG_ID_DEFAULT_FRONT) {
                            int iu = 0;
                            while (iu < NU) {
                                int i2;
                                int i3;
                                int NSB;
                                int version2;
                                iwr = in.readInt();
                                Uid u = new Uid(this, iwr);
                                this.mUidStats.put(iwr, u);
                                u.mOnBatteryBackgroundTimeBase.readSummaryFromParcel(parcel);
                                u.mOnBatteryScreenOffBackgroundTimeBase.readSummaryFromParcel(parcel);
                                u.mWifiRunning = z;
                                if (in.readInt() != 0) {
                                    u.mWifiRunningTimer.readSummaryFromParcelLocked(parcel);
                                }
                                u.mFullWifiLockOut = z;
                                if (in.readInt() != 0) {
                                    u.mFullWifiLockTimer.readSummaryFromParcelLocked(parcel);
                                }
                                u.mWifiScanStarted = z;
                                if (in.readInt() != 0) {
                                    u.mWifiScanTimer.readSummaryFromParcelLocked(parcel);
                                }
                                u.mWifiBatchedScanBinStarted = -1;
                                for (i2 = z; i2 < 5; i2++) {
                                    if (in.readInt() != 0) {
                                        u.makeWifiBatchedScanBin(i2, NS);
                                        u.mWifiBatchedScanTimer[i2].readSummaryFromParcelLocked(parcel);
                                    }
                                }
                                u.mWifiMulticastEnabled = false;
                                if (in.readInt() != 0) {
                                    u.mWifiMulticastTimer.readSummaryFromParcelLocked(parcel);
                                }
                                if (in.readInt() != 0) {
                                    u.createAudioTurnedOnTimerLocked().readSummaryFromParcelLocked(parcel);
                                }
                                if (in.readInt() != 0) {
                                    u.createVideoTurnedOnTimerLocked().readSummaryFromParcelLocked(parcel);
                                }
                                if (in.readInt() != 0) {
                                    u.createFlashlightTurnedOnTimerLocked().readSummaryFromParcelLocked(parcel);
                                }
                                if (in.readInt() != 0) {
                                    u.createCameraTurnedOnTimerLocked().readSummaryFromParcelLocked(parcel);
                                }
                                if (in.readInt() != 0) {
                                    u.createForegroundActivityTimerLocked().readSummaryFromParcelLocked(parcel);
                                }
                                if (in.readInt() != 0) {
                                    u.createForegroundServiceTimerLocked().readSummaryFromParcelLocked(parcel);
                                }
                                if (in.readInt() != 0) {
                                    u.createAggregatedPartialWakelockTimerLocked().readSummaryFromParcelLocked(parcel);
                                }
                                if (in.readInt() != 0) {
                                    u.createBluetoothScanTimerLocked().readSummaryFromParcelLocked(parcel);
                                }
                                if (in.readInt() != 0) {
                                    u.createBluetoothUnoptimizedScanTimerLocked().readSummaryFromParcelLocked(parcel);
                                }
                                if (in.readInt() != 0) {
                                    u.createBluetoothScanResultCounterLocked().readSummaryFromParcelLocked(parcel);
                                }
                                if (in.readInt() != 0) {
                                    u.createBluetoothScanResultBgCounterLocked().readSummaryFromParcelLocked(parcel);
                                }
                                u.mProcessState = 19;
                                for (i2 = 0; i2 < 7; i2++) {
                                    if (in.readInt() != 0) {
                                        u.makeProcessState(i2, NS);
                                        u.mProcessStateTimer[i2].readSummaryFromParcelLocked(parcel);
                                    }
                                }
                                if (in.readInt() != 0) {
                                    u.createVibratorOnTimerLocked().readSummaryFromParcelLocked(parcel);
                                }
                                if (in.readInt() != 0) {
                                    if (u.mUserActivityCounters == null) {
                                        u.initUserActivityLocked();
                                    }
                                    for (i2 = 0; i2 < 4; i2++) {
                                        u.mUserActivityCounters[i2].readSummaryFromParcelLocked(parcel);
                                    }
                                }
                                if (in.readInt() != 0) {
                                    if (u.mNetworkByteActivityCounters == null) {
                                        u.initNetworkActivityLocked();
                                    }
                                    for (i3 = 0; i3 < 10; i3++) {
                                        u.mNetworkByteActivityCounters[i3].readSummaryFromParcelLocked(parcel);
                                        u.mNetworkPacketActivityCounters[i3].readSummaryFromParcelLocked(parcel);
                                    }
                                    u.mMobileRadioActiveTime.readSummaryFromParcelLocked(parcel);
                                    u.mMobileRadioActiveCount.readSummaryFromParcelLocked(parcel);
                                }
                                u.mUserCpuTime.readSummaryFromParcelLocked(parcel);
                                u.mSystemCpuTime.readSummaryFromParcelLocked(parcel);
                                if (in.readInt() != 0) {
                                    i3 = in.readInt();
                                    if (this.mPowerProfile == null || this.mPowerProfile.getNumCpuClusters() == i3) {
                                        u.mCpuClusterSpeedTimesUs = new LongSamplingCounter[i3][];
                                        i2 = 0;
                                        while (i2 < i3) {
                                            Parcel parcel2;
                                            if (in.readInt() != 0) {
                                                NSB = in.readInt();
                                                version2 = version;
                                                if (this.mPowerProfile == 0) {
                                                    NMS2 = NMS;
                                                    NPKG2 = NPKG;
                                                } else if (this.mPowerProfile.getNumSpeedStepsInCpuCluster(i2) == NSB) {
                                                    NMS2 = NMS;
                                                    NPKG2 = NPKG;
                                                } else {
                                                    stringBuilder2 = new StringBuilder();
                                                    stringBuilder2.append("File corrupt: too many speed bins ");
                                                    stringBuilder2.append(NSB);
                                                    throw new ParcelFormatException(stringBuilder2.toString());
                                                }
                                                u.mCpuClusterSpeedTimesUs[i2] = new LongSamplingCounter[NSB];
                                                version = 0;
                                                while (version < NSB) {
                                                    int NSB2;
                                                    if (in.readInt() != 0) {
                                                        NSB2 = NSB;
                                                        u.mCpuClusterSpeedTimesUs[i2][version] = new LongSamplingCounter(this.mOnBatteryTimeBase);
                                                        u.mCpuClusterSpeedTimesUs[i2][version].readSummaryFromParcelLocked(parcel);
                                                    } else {
                                                        NSB2 = NSB;
                                                    }
                                                    version++;
                                                    NSB = NSB2;
                                                }
                                                parcel2 = null;
                                            } else {
                                                version2 = version;
                                                NMS2 = NMS;
                                                NPKG2 = NPKG;
                                                parcel2 = null;
                                                u.mCpuClusterSpeedTimesUs[i2] = null;
                                            }
                                            i2++;
                                            NS = parcel2;
                                            version = version2;
                                            NMS = NMS2;
                                            NPKG = NPKG2;
                                        }
                                        version2 = version;
                                        NMS2 = NMS;
                                        NPKG2 = NPKG;
                                        NMS = NS;
                                    } else {
                                        throw new ParcelFormatException("Incompatible cpu cluster arrangement");
                                    }
                                }
                                version2 = version;
                                NMS2 = NMS;
                                NPKG2 = NPKG;
                                u.mCpuClusterSpeedTimesUs = NS;
                                u.mCpuFreqTimeMs = LongSamplingCounterArray.readSummaryFromParcelLocked(parcel, this.mOnBatteryTimeBase);
                                u.mScreenOffCpuFreqTimeMs = LongSamplingCounterArray.readSummaryFromParcelLocked(parcel, this.mOnBatteryScreenOffTimeBase);
                                u.mCpuActiveTimeMs.readSummaryFromParcelLocked(parcel);
                                u.mCpuClusterTimesMs.readSummaryFromParcelLocked(parcel);
                                version = in.readInt();
                                if (version == 7) {
                                    u.mProcStateTimeMs = new LongSamplingCounterArray[version];
                                    for (NMS = 0; NMS < version; NMS++) {
                                        u.mProcStateTimeMs[NMS] = LongSamplingCounterArray.readSummaryFromParcelLocked(parcel, this.mOnBatteryTimeBase);
                                    }
                                } else {
                                    u.mProcStateTimeMs = null;
                                }
                                version = in.readInt();
                                if (version == 7) {
                                    u.mProcStateScreenOffTimeMs = new LongSamplingCounterArray[version];
                                    for (NMS = 0; NMS < version; NMS++) {
                                        u.mProcStateScreenOffTimeMs[NMS] = LongSamplingCounterArray.readSummaryFromParcelLocked(parcel, this.mOnBatteryScreenOffTimeBase);
                                    }
                                } else {
                                    u.mProcStateScreenOffTimeMs = null;
                                }
                                if (in.readInt() != 0) {
                                    u.mMobileRadioApWakeupCount = new LongSamplingCounter(this.mOnBatteryTimeBase);
                                    u.mMobileRadioApWakeupCount.readSummaryFromParcelLocked(parcel);
                                } else {
                                    u.mMobileRadioApWakeupCount = null;
                                }
                                if (in.readInt() != 0) {
                                    u.mWifiRadioApWakeupCount = new LongSamplingCounter(this.mOnBatteryTimeBase);
                                    u.mWifiRadioApWakeupCount.readSummaryFromParcelLocked(parcel);
                                } else {
                                    u.mWifiRadioApWakeupCount = null;
                                }
                                NPKG = in.readInt();
                                int length;
                                if (NPKG <= MAX_WAKELOCKS_PER_UID + 1) {
                                    for (NSB = 0; NSB < NPKG; NSB++) {
                                        u.readWakeSummaryFromParcelLocked(in.readString(), parcel);
                                    }
                                    NSB = in.readInt();
                                    if (NSB <= MAX_WAKELOCKS_PER_UID + 1) {
                                        for (i3 = 0; i3 < NSB; i3++) {
                                            u.readSyncSummaryFromParcelLocked(in.readString(), parcel);
                                        }
                                        i3 = in.readInt();
                                        if (i3 <= MAX_WAKELOCKS_PER_UID + 1) {
                                            for (i2 = 0; i2 < i3; i2++) {
                                                u.readJobSummaryFromParcelLocked(in.readString(), parcel);
                                            }
                                            u.readJobCompletionsFromParcelLocked(parcel);
                                            u.mJobsDeferredEventCount.readSummaryFromParcelLocked(parcel);
                                            u.mJobsDeferredCount.readSummaryFromParcelLocked(parcel);
                                            u.mJobsFreshnessTimeMs.readSummaryFromParcelLocked(parcel);
                                            NMS = 0;
                                            while (NMS < JOB_FRESHNESS_BUCKETS.length) {
                                                if (in.readInt() != 0) {
                                                    length = version;
                                                    NRPMS = i;
                                                    u.mJobsFreshnessBuckets[NMS] = new Counter(u.mBsi.mOnBatteryTimeBase);
                                                    u.mJobsFreshnessBuckets[NMS].readSummaryFromParcelLocked(parcel);
                                                } else {
                                                    length = version;
                                                    NRPMS = i;
                                                }
                                                NMS++;
                                                version = length;
                                                i = NRPMS;
                                            }
                                            length = version;
                                            NRPMS = i;
                                            version = in.readInt();
                                            if (version <= 1000) {
                                                i = 0;
                                                while (i < version) {
                                                    i2 = in.readInt();
                                                    if (in.readInt() != 0) {
                                                        NSORPMS = irpm;
                                                        u.getSensorTimerLocked(i2, true).readSummaryFromParcelLocked(parcel);
                                                    } else {
                                                        NSORPMS = irpm;
                                                    }
                                                    i++;
                                                    irpm = NSORPMS;
                                                }
                                                NSORPMS = irpm;
                                                version = in.readInt();
                                                StringBuilder stringBuilder3;
                                                if (version <= 1000) {
                                                    NMS = 0;
                                                    while (NMS < version) {
                                                        Proc p = u.getProcessStatsLocked(in.readString());
                                                        NKW = irpm2;
                                                        long readLong = in.readLong();
                                                        p.mLoadedUserTime = readLong;
                                                        p.mUserTime = readLong;
                                                        readLong = in.readLong();
                                                        p.mLoadedSystemTime = readLong;
                                                        p.mSystemTime = readLong;
                                                        readLong = in.readLong();
                                                        p.mLoadedForegroundTime = readLong;
                                                        p.mForegroundTime = readLong;
                                                        irpm = in.readInt();
                                                        p.mLoadedStarts = irpm;
                                                        p.mStarts = irpm;
                                                        irpm = in.readInt();
                                                        p.mLoadedNumCrashes = irpm;
                                                        p.mNumCrashes = irpm;
                                                        irpm = in.readInt();
                                                        p.mLoadedNumAnrs = irpm;
                                                        p.mNumAnrs = irpm;
                                                        p.readExcessivePowerFromParcelLocked(parcel);
                                                        NMS++;
                                                        irpm2 = NKW;
                                                    }
                                                    NKW = irpm2;
                                                    version = in.readInt();
                                                    if (version <= PGAction.PG_ID_DEFAULT_FRONT) {
                                                        i = NSB;
                                                        NSB = 0;
                                                        while (NSB < version) {
                                                            String pkgName = in.readString();
                                                            Pkg p2 = u.getPackageStatsLocked(pkgName);
                                                            irpm2 = in.readInt();
                                                            int NS2;
                                                            String pkgName2;
                                                            Pkg p3;
                                                            if (irpm2 <= 1000) {
                                                                p2.mWakeupAlarms.clear();
                                                                NMS = 0;
                                                                while (NMS < irpm2) {
                                                                    NS2 = i;
                                                                    i = in.readString();
                                                                    NWR = ikw;
                                                                    NU2 = NU;
                                                                    Counter c = new Counter(this.mOnBatteryScreenOffTimeBase);
                                                                    c.readSummaryFromParcelLocked(parcel);
                                                                    p2.mWakeupAlarms.put(i, c);
                                                                    NMS++;
                                                                    i = NS2;
                                                                    ikw = NWR;
                                                                    NU = NU2;
                                                                }
                                                                NU2 = NU;
                                                                NWR = ikw;
                                                                i = in.readInt();
                                                                if (i <= 1000) {
                                                                    NU = 0;
                                                                    while (NU < i) {
                                                                        Serv s = u.getServiceStatsLocked(pkgName, in.readString());
                                                                        pkgName2 = pkgName;
                                                                        p3 = p2;
                                                                        long readLong2 = in.readLong();
                                                                        s.mLoadedStartTime = readLong2;
                                                                        s.mStartTime = readLong2;
                                                                        i2 = in.readInt();
                                                                        s.mLoadedStarts = i2;
                                                                        s.mStarts = i2;
                                                                        i2 = in.readInt();
                                                                        s.mLoadedLaunches = i2;
                                                                        s.mLaunches = i2;
                                                                        NU++;
                                                                        pkgName = pkgName2;
                                                                        p2 = p3;
                                                                    }
                                                                    NSB++;
                                                                    ikw = NWR;
                                                                    NU = NU2;
                                                                } else {
                                                                    pkgName2 = pkgName;
                                                                    p3 = p2;
                                                                    NU = new StringBuilder();
                                                                    NU.append("File corrupt: too many services ");
                                                                    NU.append(i);
                                                                    throw new ParcelFormatException(NU.toString());
                                                                }
                                                            }
                                                            NS2 = i;
                                                            NU2 = NU;
                                                            pkgName2 = pkgName;
                                                            p3 = p2;
                                                            NWR = ikw;
                                                            stringBuilder3 = new StringBuilder();
                                                            stringBuilder3.append("File corrupt: too many wakeup alarms ");
                                                            stringBuilder3.append(irpm2);
                                                            throw new ParcelFormatException(stringBuilder3.toString());
                                                        }
                                                        NWR = ikw;
                                                        iu++;
                                                        version = version2;
                                                        NMS = NMS2;
                                                        NPKG = NPKG2;
                                                        i = NRPMS;
                                                        irpm = NSORPMS;
                                                        irpm2 = NKW;
                                                        NS = null;
                                                        z = false;
                                                    } else {
                                                        NWR = ikw;
                                                        stringBuilder3 = new StringBuilder();
                                                        stringBuilder3.append("File corrupt: too many packages ");
                                                        stringBuilder3.append(version);
                                                        throw new ParcelFormatException(stringBuilder3.toString());
                                                    }
                                                }
                                                NKW = irpm2;
                                                NWR = ikw;
                                                stringBuilder3 = new StringBuilder();
                                                stringBuilder3.append("File corrupt: too many processes ");
                                                stringBuilder3.append(version);
                                                throw new ParcelFormatException(stringBuilder3.toString());
                                            }
                                            NSORPMS = irpm;
                                            NKW = irpm2;
                                            NWR = ikw;
                                            i = new StringBuilder();
                                            i.append("File corrupt: too many sensors ");
                                            i.append(version);
                                            throw new ParcelFormatException(i.toString());
                                        }
                                        length = version;
                                        NRPMS = i;
                                        NU2 = NU;
                                        NSORPMS = irpm;
                                        NKW = irpm2;
                                        NWR = ikw;
                                        stringBuilder2 = new StringBuilder();
                                        stringBuilder2.append("File corrupt: too many job timers ");
                                        stringBuilder2.append(i3);
                                        throw new ParcelFormatException(stringBuilder2.toString());
                                    }
                                    length = version;
                                    NRPMS = i;
                                    NU2 = NU;
                                    NSORPMS = irpm;
                                    NKW = irpm2;
                                    NWR = ikw;
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("File corrupt: too many syncs ");
                                    stringBuilder2.append(NSB);
                                    throw new ParcelFormatException(stringBuilder2.toString());
                                }
                                length = version;
                                NRPMS = i;
                                NU2 = NU;
                                NSORPMS = irpm;
                                NKW = irpm2;
                                NWR = ikw;
                                version = new StringBuilder();
                                version.append("NW > ");
                                version.append(MAX_WAKELOCKS_PER_UID + 1);
                                version.append(", uid: ");
                                version.append(iwr);
                                Slog.i(TAG, version.toString());
                                NMS = new StringBuilder();
                                NMS.append("File corrupt: too many wake locks ");
                                NMS.append(NPKG);
                                throw new ParcelFormatException(NMS.toString());
                            }
                            NMS2 = NMS;
                            NPKG2 = NPKG;
                            NRPMS = i;
                            NU2 = NU;
                            NSORPMS = irpm;
                            NKW = irpm2;
                            NWR = ikw;
                            return;
                        }
                        NMS2 = NMS;
                        NPKG2 = NPKG;
                        NRPMS = i;
                        NU2 = NU;
                        NSORPMS = irpm;
                        NKW = irpm2;
                        NWR = ikw;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("File corrupt: too many uids ");
                        stringBuilder2.append(NU2);
                        throw new ParcelFormatException(stringBuilder2.toString());
                    }
                    NPKG2 = NPKG;
                    NRPMS = i;
                    NSORPMS = irpm;
                    NKW = irpm2;
                    NWR = ikw;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("File corrupt: too many wakeup reasons ");
                    stringBuilder2.append(NWR);
                    throw new ParcelFormatException(stringBuilder2.toString());
                }
                NPKG2 = NPKG;
                NRPMS = i;
                NSORPMS = irpm;
                NKW = irpm2;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("File corrupt: too many kernel wake locks ");
                stringBuilder2.append(NKW);
                throw new ParcelFormatException(stringBuilder2.toString());
            }
            NPKG2 = NPKG;
            NRPMS = i;
            NSORPMS = irpm;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("File corrupt: too many screen-off rpm stats ");
            stringBuilder2.append(NSORPMS);
            throw new ParcelFormatException(stringBuilder2.toString());
        }
        NPKG2 = NPKG;
        NRPMS = i;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("File corrupt: too many rpm stats ");
        stringBuilder2.append(NRPMS);
        throw new ParcelFormatException(stringBuilder2.toString());
    }

    public void writeSummaryToParcel(Parcel out, boolean inclHistory) {
        int NPKG;
        int i;
        int i2;
        int i3;
        Timer rpmt;
        BatteryStatsImpl batteryStatsImpl = this;
        Parcel parcel = out;
        pullPendingStateUpdatesLocked();
        long startClockTime = getStartClockTime();
        long NOW_SYS = batteryStatsImpl.mClocks.uptimeMillis() * 1000;
        long NOWREAL_SYS = batteryStatsImpl.mClocks.elapsedRealtime() * 1000;
        parcel.writeInt(177);
        batteryStatsImpl.writeHistory(parcel, inclHistory, true);
        parcel.writeInt(batteryStatsImpl.mStartCount);
        parcel.writeLong(batteryStatsImpl.computeUptime(NOW_SYS, 0));
        parcel.writeLong(batteryStatsImpl.computeRealtime(NOWREAL_SYS, 0));
        parcel.writeLong(startClockTime);
        parcel.writeString(batteryStatsImpl.mStartPlatformVersion);
        parcel.writeString(batteryStatsImpl.mEndPlatformVersion);
        Parcel parcel2 = parcel;
        long j = NOW_SYS;
        int i4 = 0;
        long j2 = NOWREAL_SYS;
        batteryStatsImpl.mOnBatteryTimeBase.writeSummaryToParcel(parcel2, j, j2);
        batteryStatsImpl.mOnBatteryScreenOffTimeBase.writeSummaryToParcel(parcel2, j, j2);
        parcel.writeInt(batteryStatsImpl.mDischargeUnplugLevel);
        parcel.writeInt(batteryStatsImpl.mDischargePlugLevel);
        parcel.writeInt(batteryStatsImpl.mDischargeCurrentLevel);
        parcel.writeInt(batteryStatsImpl.mCurrentBatteryLevel);
        parcel.writeInt(batteryStatsImpl.mEstimatedBatteryCapacity);
        parcel.writeInt(batteryStatsImpl.mMinLearnedBatteryCapacity);
        parcel.writeInt(batteryStatsImpl.mMaxLearnedBatteryCapacity);
        parcel.writeInt(getLowDischargeAmountSinceCharge());
        parcel.writeInt(getHighDischargeAmountSinceCharge());
        parcel.writeInt(getDischargeAmountScreenOnSinceCharge());
        parcel.writeInt(getDischargeAmountScreenOffSinceCharge());
        parcel.writeInt(getDischargeAmountScreenDozeSinceCharge());
        batteryStatsImpl.mDischargeStepTracker.writeToParcel(parcel);
        batteryStatsImpl.mChargeStepTracker.writeToParcel(parcel);
        batteryStatsImpl.mDailyDischargeStepTracker.writeToParcel(parcel);
        batteryStatsImpl.mDailyChargeStepTracker.writeToParcel(parcel);
        batteryStatsImpl.mDischargeCounter.writeSummaryFromParcelLocked(parcel);
        batteryStatsImpl.mDischargeScreenOffCounter.writeSummaryFromParcelLocked(parcel);
        batteryStatsImpl.mDischargeScreenDozeCounter.writeSummaryFromParcelLocked(parcel);
        batteryStatsImpl.mDischargeLightDozeCounter.writeSummaryFromParcelLocked(parcel);
        batteryStatsImpl.mDischargeDeepDozeCounter.writeSummaryFromParcelLocked(parcel);
        if (batteryStatsImpl.mDailyPackageChanges != null) {
            NPKG = batteryStatsImpl.mDailyPackageChanges.size();
            parcel.writeInt(NPKG);
            for (i = i4; i < NPKG; i++) {
                PackageChange pc = (PackageChange) batteryStatsImpl.mDailyPackageChanges.get(i);
                parcel.writeString(pc.mPackageName);
                parcel.writeInt(pc.mUpdate);
                parcel.writeLong(pc.mVersionCode);
            }
        } else {
            parcel.writeInt(i4);
        }
        parcel.writeLong(batteryStatsImpl.mDailyStartTime);
        parcel.writeLong(batteryStatsImpl.mNextMinDailyDeadline);
        parcel.writeLong(batteryStatsImpl.mNextMaxDailyDeadline);
        batteryStatsImpl.mScreenOnTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
        batteryStatsImpl.mScreenDozeTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
        NPKG = i4;
        while (true) {
            i2 = 5;
            if (NPKG >= 5) {
                break;
            }
            batteryStatsImpl.mScreenBrightnessTimer[NPKG].writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
            NPKG++;
        }
        batteryStatsImpl.mInteractiveTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
        batteryStatsImpl.mPowerSaveModeEnabledTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
        parcel.writeLong(batteryStatsImpl.mLongestLightIdleTime);
        parcel.writeLong(batteryStatsImpl.mLongestFullIdleTime);
        batteryStatsImpl.mDeviceIdleModeLightTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
        batteryStatsImpl.mDeviceIdleModeFullTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
        batteryStatsImpl.mDeviceLightIdlingTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
        batteryStatsImpl.mDeviceIdlingTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
        batteryStatsImpl.mPhoneOnTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
        for (NPKG = i4; NPKG < 5; NPKG++) {
            batteryStatsImpl.mPhoneSignalStrengthsTimer[NPKG].writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
        }
        batteryStatsImpl.mPhoneSignalScanningTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
        for (NPKG = i4; NPKG < 21; NPKG++) {
            batteryStatsImpl.mPhoneDataConnectionsTimer[NPKG].writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
        }
        NPKG = i4;
        while (true) {
            i3 = 10;
            if (NPKG >= 10) {
                break;
            }
            batteryStatsImpl.mNetworkByteActivityCounters[NPKG].writeSummaryFromParcelLocked(parcel);
            batteryStatsImpl.mNetworkPacketActivityCounters[NPKG].writeSummaryFromParcelLocked(parcel);
            NPKG++;
        }
        batteryStatsImpl.mMobileRadioActiveTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
        batteryStatsImpl.mMobileRadioActivePerAppTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
        batteryStatsImpl.mMobileRadioActiveAdjustedTime.writeSummaryFromParcelLocked(parcel);
        batteryStatsImpl.mMobileRadioActiveUnknownTime.writeSummaryFromParcelLocked(parcel);
        batteryStatsImpl.mMobileRadioActiveUnknownCount.writeSummaryFromParcelLocked(parcel);
        batteryStatsImpl.mWifiMulticastWakelockTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
        batteryStatsImpl.mWifiOnTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
        batteryStatsImpl.mGlobalWifiRunningTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
        for (NPKG = i4; NPKG < 8; NPKG++) {
            batteryStatsImpl.mWifiStateTimer[NPKG].writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
        }
        for (NPKG = i4; NPKG < 13; NPKG++) {
            batteryStatsImpl.mWifiSupplStateTimer[NPKG].writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
        }
        for (NPKG = i4; NPKG < 5; NPKG++) {
            batteryStatsImpl.mWifiSignalStrengthsTimer[NPKG].writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
        }
        batteryStatsImpl.mWifiActiveTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
        batteryStatsImpl.mWifiActivity.writeSummaryToParcel(parcel);
        for (NPKG = i4; NPKG < 2; NPKG++) {
            batteryStatsImpl.mGpsSignalQualityTimer[NPKG].writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
        }
        batteryStatsImpl.mBluetoothActivity.writeSummaryToParcel(parcel);
        batteryStatsImpl.mModemActivity.writeSummaryToParcel(parcel);
        parcel.writeInt(batteryStatsImpl.mHasWifiReporting);
        parcel.writeInt(batteryStatsImpl.mHasBluetoothReporting);
        parcel.writeInt(batteryStatsImpl.mHasModemReporting);
        parcel.writeInt(batteryStatsImpl.mNumConnectivityChange);
        batteryStatsImpl.mFlashlightOnTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
        batteryStatsImpl.mCameraOnTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
        batteryStatsImpl.mBluetoothScanTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
        parcel.writeInt(batteryStatsImpl.mRpmStats.size());
        for (Entry<String, SamplingTimer> ent : batteryStatsImpl.mRpmStats.entrySet()) {
            rpmt = (Timer) ent.getValue();
            if (rpmt != null) {
                parcel.writeInt(1);
                parcel.writeString((String) ent.getKey());
                rpmt.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
            } else {
                parcel.writeInt(i4);
            }
        }
        parcel.writeInt(batteryStatsImpl.mScreenOffRpmStats.size());
        for (Entry<String, SamplingTimer> ent2 : batteryStatsImpl.mScreenOffRpmStats.entrySet()) {
            rpmt = (Timer) ent2.getValue();
            if (rpmt != null) {
                parcel.writeInt(1);
                parcel.writeString((String) ent2.getKey());
                rpmt.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
            } else {
                parcel.writeInt(i4);
            }
        }
        parcel.writeInt(batteryStatsImpl.mKernelWakelockStats.size());
        for (Entry<String, SamplingTimer> ent22 : batteryStatsImpl.mKernelWakelockStats.entrySet()) {
            rpmt = (Timer) ent22.getValue();
            if (rpmt != null) {
                parcel.writeInt(1);
                parcel.writeString((String) ent22.getKey());
                rpmt.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
            } else {
                parcel.writeInt(i4);
            }
        }
        parcel.writeInt(batteryStatsImpl.mWakeupReasonStats.size());
        for (Entry<String, SamplingTimer> ent222 : batteryStatsImpl.mWakeupReasonStats.entrySet()) {
            SamplingTimer timer = (SamplingTimer) ent222.getValue();
            if (timer != null) {
                parcel.writeInt(1);
                parcel.writeString((String) ent222.getKey());
                timer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
            } else {
                parcel.writeInt(i4);
            }
        }
        parcel.writeInt(batteryStatsImpl.mKernelMemoryStats.size());
        for (NPKG = i4; NPKG < batteryStatsImpl.mKernelMemoryStats.size(); NPKG++) {
            Timer kmt = (Timer) batteryStatsImpl.mKernelMemoryStats.valueAt(NPKG);
            if (kmt != null) {
                parcel.writeInt(1);
                parcel.writeLong(batteryStatsImpl.mKernelMemoryStats.keyAt(NPKG));
                kmt.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
            } else {
                parcel.writeInt(i4);
            }
        }
        int NU = batteryStatsImpl.mUidStats.size();
        parcel.writeInt(NU);
        NPKG = i4;
        while (true) {
            int iu = NPKG;
            long startClockTime2;
            if (iu < NU) {
                int NW;
                ArrayMap<String, DualTimer> syncStats;
                int NS;
                parcel.writeInt(batteryStatsImpl.mUidStats.keyAt(iu));
                Uid u = (Uid) batteryStatsImpl.mUidStats.valueAt(iu);
                TimeBase timeBase = u.mOnBatteryBackgroundTimeBase;
                Uid u2 = u;
                parcel2 = parcel;
                int NU2 = NU;
                int iu2 = iu;
                NU = NOW_SYS;
                startClockTime2 = startClockTime;
                int i5 = i2;
                int i6 = i3;
                j2 = NOWREAL_SYS;
                timeBase.writeSummaryToParcel(parcel2, NU, j2);
                u2.mOnBatteryScreenOffBackgroundTimeBase.writeSummaryToParcel(parcel2, NU, j2);
                if (u2.mWifiRunningTimer != null) {
                    parcel.writeInt(1);
                    u2.mWifiRunningTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
                } else {
                    parcel.writeInt(0);
                }
                if (u2.mFullWifiLockTimer != null) {
                    parcel.writeInt(1);
                    u2.mFullWifiLockTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
                } else {
                    parcel.writeInt(0);
                }
                if (u2.mWifiScanTimer != null) {
                    parcel.writeInt(1);
                    u2.mWifiScanTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
                } else {
                    parcel.writeInt(0);
                }
                for (NPKG = 0; NPKG < i5; NPKG++) {
                    if (u2.mWifiBatchedScanTimer[NPKG] != null) {
                        parcel.writeInt(1);
                        u2.mWifiBatchedScanTimer[NPKG].writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
                    } else {
                        parcel.writeInt(0);
                    }
                }
                if (u2.mWifiMulticastTimer != null) {
                    parcel.writeInt(1);
                    u2.mWifiMulticastTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
                } else {
                    parcel.writeInt(0);
                }
                if (u2.mAudioTurnedOnTimer != null) {
                    parcel.writeInt(1);
                    u2.mAudioTurnedOnTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
                } else {
                    parcel.writeInt(0);
                }
                if (u2.mVideoTurnedOnTimer != null) {
                    parcel.writeInt(1);
                    u2.mVideoTurnedOnTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
                } else {
                    parcel.writeInt(0);
                }
                if (u2.mFlashlightTurnedOnTimer != null) {
                    parcel.writeInt(1);
                    u2.mFlashlightTurnedOnTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
                } else {
                    parcel.writeInt(0);
                }
                if (u2.mCameraTurnedOnTimer != null) {
                    parcel.writeInt(1);
                    u2.mCameraTurnedOnTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
                } else {
                    parcel.writeInt(0);
                }
                if (u2.mForegroundActivityTimer != null) {
                    parcel.writeInt(1);
                    u2.mForegroundActivityTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
                } else {
                    parcel.writeInt(0);
                }
                if (u2.mForegroundServiceTimer != null) {
                    parcel.writeInt(1);
                    u2.mForegroundServiceTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
                } else {
                    parcel.writeInt(0);
                }
                if (u2.mAggregatedPartialWakelockTimer != null) {
                    parcel.writeInt(1);
                    u2.mAggregatedPartialWakelockTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
                } else {
                    parcel.writeInt(0);
                }
                if (u2.mBluetoothScanTimer != null) {
                    parcel.writeInt(1);
                    u2.mBluetoothScanTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
                } else {
                    parcel.writeInt(0);
                }
                if (u2.mBluetoothUnoptimizedScanTimer != null) {
                    parcel.writeInt(1);
                    u2.mBluetoothUnoptimizedScanTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
                } else {
                    parcel.writeInt(0);
                }
                if (u2.mBluetoothScanResultCounter != null) {
                    parcel.writeInt(1);
                    u2.mBluetoothScanResultCounter.writeSummaryFromParcelLocked(parcel);
                } else {
                    parcel.writeInt(0);
                }
                if (u2.mBluetoothScanResultBgCounter != null) {
                    parcel.writeInt(1);
                    u2.mBluetoothScanResultBgCounter.writeSummaryFromParcelLocked(parcel);
                } else {
                    parcel.writeInt(0);
                }
                for (NPKG = 0; NPKG < 7; NPKG++) {
                    if (u2.mProcessStateTimer[NPKG] != null) {
                        parcel.writeInt(1);
                        u2.mProcessStateTimer[NPKG].writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
                    } else {
                        parcel.writeInt(0);
                    }
                }
                if (u2.mVibratorOnTimer != null) {
                    parcel.writeInt(1);
                    u2.mVibratorOnTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
                    NPKG = 0;
                } else {
                    NPKG = 0;
                    parcel.writeInt(0);
                }
                if (u2.mUserActivityCounters == null) {
                    parcel.writeInt(NPKG);
                } else {
                    parcel.writeInt(1);
                    for (NPKG = 0; NPKG < 4; NPKG++) {
                        u2.mUserActivityCounters[NPKG].writeSummaryFromParcelLocked(parcel);
                    }
                }
                if (u2.mNetworkByteActivityCounters == null) {
                    parcel.writeInt(0);
                } else {
                    parcel.writeInt(1);
                    for (NPKG = 0; NPKG < i6; NPKG++) {
                        u2.mNetworkByteActivityCounters[NPKG].writeSummaryFromParcelLocked(parcel);
                        u2.mNetworkPacketActivityCounters[NPKG].writeSummaryFromParcelLocked(parcel);
                    }
                    u2.mMobileRadioActiveTime.writeSummaryFromParcelLocked(parcel);
                    u2.mMobileRadioActiveCount.writeSummaryFromParcelLocked(parcel);
                }
                u2.mUserCpuTime.writeSummaryFromParcelLocked(parcel);
                u2.mSystemCpuTime.writeSummaryFromParcelLocked(parcel);
                if (u2.mCpuClusterSpeedTimesUs != null) {
                    parcel.writeInt(1);
                    parcel.writeInt(u2.mCpuClusterSpeedTimesUs.length);
                    for (LongSamplingCounter[] cpuSpeeds : u2.mCpuClusterSpeedTimesUs) {
                        if (cpuSpeeds != null) {
                            parcel.writeInt(1);
                            parcel.writeInt(cpuSpeeds.length);
                            for (LongSamplingCounter c : cpuSpeeds) {
                                if (c != null) {
                                    parcel.writeInt(1);
                                    c.writeSummaryFromParcelLocked(parcel);
                                } else {
                                    parcel.writeInt(0);
                                }
                            }
                        } else {
                            parcel.writeInt(0);
                        }
                    }
                } else {
                    parcel.writeInt(0);
                }
                LongSamplingCounterArray.writeSummaryToParcelLocked(parcel, u2.mCpuFreqTimeMs);
                LongSamplingCounterArray.writeSummaryToParcelLocked(parcel, u2.mScreenOffCpuFreqTimeMs);
                u2.mCpuActiveTimeMs.writeSummaryFromParcelLocked(parcel);
                u2.mCpuClusterTimesMs.writeSummaryToParcelLocked(parcel);
                if (u2.mProcStateTimeMs != null) {
                    parcel.writeInt(u2.mProcStateTimeMs.length);
                    for (LongSamplingCounterArray counters : u2.mProcStateTimeMs) {
                        LongSamplingCounterArray.writeSummaryToParcelLocked(parcel, counters);
                    }
                } else {
                    parcel.writeInt(0);
                }
                if (u2.mProcStateScreenOffTimeMs != null) {
                    parcel.writeInt(u2.mProcStateScreenOffTimeMs.length);
                    for (LongSamplingCounterArray counters2 : u2.mProcStateScreenOffTimeMs) {
                        LongSamplingCounterArray.writeSummaryToParcelLocked(parcel, counters2);
                    }
                } else {
                    parcel.writeInt(0);
                }
                if (u2.mMobileRadioApWakeupCount != null) {
                    parcel.writeInt(1);
                    u2.mMobileRadioApWakeupCount.writeSummaryFromParcelLocked(parcel);
                } else {
                    parcel.writeInt(0);
                }
                if (u2.mWifiRadioApWakeupCount != null) {
                    parcel.writeInt(1);
                    u2.mWifiRadioApWakeupCount.writeSummaryFromParcelLocked(parcel);
                } else {
                    parcel.writeInt(0);
                }
                ArrayMap<String, Wakelock> wakeStats = u2.mWakelockStats.getMap();
                i = wakeStats.size();
                parcel.writeInt(i);
                for (NU = 0; NU < i; NU++) {
                    parcel.writeString((String) wakeStats.keyAt(NU));
                    Wakelock wl = (Wakelock) wakeStats.valueAt(NU);
                    if (wl.mTimerFull != null) {
                        parcel.writeInt(1);
                        wl.mTimerFull.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
                    } else {
                        parcel.writeInt(0);
                    }
                    if (wl.mTimerPartial != null) {
                        parcel.writeInt(1);
                        wl.mTimerPartial.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
                    } else {
                        parcel.writeInt(0);
                    }
                    if (wl.mTimerWindow != null) {
                        parcel.writeInt(1);
                        wl.mTimerWindow.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
                    } else {
                        parcel.writeInt(0);
                    }
                    if (wl.mTimerDraw != null) {
                        parcel.writeInt(1);
                        wl.mTimerDraw.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
                    } else {
                        parcel.writeInt(0);
                    }
                }
                NU = u2.mSyncStats.getMap();
                iu = NU.size();
                parcel.writeInt(iu);
                for (i2 = 0; i2 < iu; i2++) {
                    parcel.writeString((String) NU.keyAt(i2));
                    ((DualTimer) NU.valueAt(i2)).writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
                }
                ArrayMap<String, DualTimer> jobStats = u2.mJobStats.getMap();
                i3 = jobStats.size();
                parcel.writeInt(i3);
                for (i5 = 0; i5 < i3; i5++) {
                    parcel.writeString((String) jobStats.keyAt(i5));
                    ((DualTimer) jobStats.valueAt(i5)).writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
                }
                u2.writeJobCompletionsToParcelLocked(parcel);
                u2.mJobsDeferredEventCount.writeSummaryFromParcelLocked(parcel);
                u2.mJobsDeferredCount.writeSummaryFromParcelLocked(parcel);
                u2.mJobsFreshnessTimeMs.writeSummaryFromParcelLocked(parcel);
                for (i5 = 0; i5 < JOB_FRESHNESS_BUCKETS.length; i5++) {
                    if (u2.mJobsFreshnessBuckets[i5] != null) {
                        parcel.writeInt(1);
                        u2.mJobsFreshnessBuckets[i5].writeSummaryFromParcelLocked(parcel);
                    } else {
                        parcel.writeInt(0);
                    }
                }
                i5 = u2.mSensorStats.size();
                parcel.writeInt(i5);
                i6 = 0;
                while (i6 < i5) {
                    ArrayMap<String, Wakelock> wakeStats2 = wakeStats;
                    parcel.writeInt(u2.mSensorStats.keyAt(i6));
                    Sensor wakeStats3 = (Sensor) u2.mSensorStats.valueAt(i6);
                    NW = i;
                    if (wakeStats3.mTimer != 0) {
                        parcel.writeInt(1);
                        wakeStats3.mTimer.writeSummaryFromParcelLocked(parcel, NOWREAL_SYS);
                    } else {
                        parcel.writeInt(0);
                    }
                    i6++;
                    wakeStats = wakeStats2;
                    i = NW;
                }
                NW = i;
                NPKG = u2.mProcessStats.size();
                parcel.writeInt(NPKG);
                i6 = 0;
                while (i6 < NPKG) {
                    parcel.writeString((String) u2.mProcessStats.keyAt(i6));
                    Proc ps = (Proc) u2.mProcessStats.valueAt(i6);
                    syncStats = NU;
                    NS = iu;
                    parcel.writeLong(ps.mUserTime);
                    parcel.writeLong(ps.mSystemTime);
                    parcel.writeLong(ps.mForegroundTime);
                    parcel.writeInt(ps.mStarts);
                    parcel.writeInt(ps.mNumCrashes);
                    parcel.writeInt(ps.mNumAnrs);
                    ps.writeExcessivePowerToParcelLocked(parcel);
                    i6++;
                    NU = syncStats;
                    iu = NS;
                }
                syncStats = NU;
                NS = iu;
                NPKG = u2.mPackageStats.size();
                parcel.writeInt(NPKG);
                if (NPKG > 0) {
                    Iterator it = u2.mPackageStats.entrySet().iterator();
                    while (it.hasNext() != 0) {
                        int NP;
                        Iterator it2;
                        int NS2;
                        NU = (Entry) it.next();
                        parcel.writeString((String) NU.getKey());
                        Pkg ps2 = (Pkg) NU.getValue();
                        i6 = ps2.mWakeupAlarms.size();
                        parcel.writeInt(i6);
                        int iwa = 0;
                        while (true) {
                            NP = NPKG;
                            NPKG = iwa;
                            if (NPKG >= i6) {
                                break;
                            }
                            it2 = it;
                            parcel.writeString((String) ps2.mWakeupAlarms.keyAt(NPKG));
                            ((Counter) ps2.mWakeupAlarms.valueAt(NPKG)).writeSummaryFromParcelLocked(parcel);
                            iwa = NPKG + 1;
                            NPKG = NP;
                            it = it2;
                        }
                        it2 = it;
                        NPKG = ps2.mServiceStats.size();
                        parcel.writeInt(NPKG);
                        i = 0;
                        while (i < NPKG) {
                            NS2 = NPKG;
                            parcel.writeString((String) ps2.mServiceStats.keyAt(i));
                            Serv NS3 = (Serv) ps2.mServiceStats.valueAt(i);
                            Entry<String, Pkg> ent3 = NU;
                            Pkg ps3 = ps2;
                            parcel.writeLong(NS3.getStartTimeToNowLocked(batteryStatsImpl.mOnBatteryTimeBase.getUptime(NOW_SYS)));
                            parcel.writeInt(NS3.mStarts);
                            parcel.writeInt(NS3.mLaunches);
                            i++;
                            NPKG = NS2;
                            NU = ent3;
                            ps2 = ps3;
                            batteryStatsImpl = this;
                        }
                        NS2 = NPKG;
                        NPKG = NP;
                        it = it2;
                        NS = NS2;
                        batteryStatsImpl = this;
                    }
                }
                NPKG = iu2 + 1;
                NU = NU2;
                startClockTime = startClockTime2;
                batteryStatsImpl = this;
                i2 = 5;
                i3 = 10;
            } else {
                startClockTime2 = startClockTime;
                return;
            }
        }
    }

    public void readFromParcel(Parcel in) {
        readFromParcelLocked(in);
    }

    void readFromParcelLocked(Parcel in) {
        Parcel parcel = in;
        int magic = in.readInt();
        if (magic == MAGIC) {
            int i;
            int irpm;
            int irpm2;
            int ikw;
            int iwr;
            int imt;
            int i2 = 0;
            readHistory(parcel, false);
            this.mStartCount = in.readInt();
            this.mStartClockTime = in.readLong();
            this.mStartPlatformVersion = in.readString();
            this.mEndPlatformVersion = in.readString();
            this.mUptime = in.readLong();
            this.mUptimeStart = in.readLong();
            this.mRealtime = in.readLong();
            this.mRealtimeStart = in.readLong();
            boolean z = true;
            this.mOnBattery = in.readInt() != 0;
            this.mEstimatedBatteryCapacity = in.readInt();
            this.mMinLearnedBatteryCapacity = in.readInt();
            this.mMaxLearnedBatteryCapacity = in.readInt();
            this.mOnBatteryInternal = false;
            this.mOnBatteryTimeBase.readFromParcel(parcel);
            this.mOnBatteryScreenOffTimeBase.readFromParcel(parcel);
            this.mScreenState = 0;
            Parcel parcel2 = parcel;
            this.mScreenOnTimer = new StopwatchTimer(this.mClocks, null, -1, null, this.mOnBatteryTimeBase, parcel2);
            this.mScreenDozeTimer = new StopwatchTimer(this.mClocks, null, -1, null, this.mOnBatteryTimeBase, parcel2);
            int i3 = 0;
            while (true) {
                i = i3;
                if (i >= 5) {
                    break;
                }
                this.mScreenBrightnessTimer[i] = new StopwatchTimer(this.mClocks, null, -100 - i, null, this.mOnBatteryTimeBase, parcel);
                i3 = i + 1;
            }
            this.mInteractive = false;
            parcel2 = parcel;
            this.mInteractiveTimer = new StopwatchTimer(this.mClocks, null, -10, null, this.mOnBatteryTimeBase, parcel2);
            this.mPhoneOn = false;
            this.mPowerSaveModeEnabledTimer = new StopwatchTimer(this.mClocks, null, -2, null, this.mOnBatteryTimeBase, parcel2);
            this.mLongestLightIdleTime = in.readLong();
            this.mLongestFullIdleTime = in.readLong();
            this.mDeviceIdleModeLightTimer = new StopwatchTimer(this.mClocks, null, -14, null, this.mOnBatteryTimeBase, parcel2);
            this.mDeviceIdleModeFullTimer = new StopwatchTimer(this.mClocks, null, -11, null, this.mOnBatteryTimeBase, parcel2);
            this.mDeviceLightIdlingTimer = new StopwatchTimer(this.mClocks, null, -15, null, this.mOnBatteryTimeBase, parcel2);
            this.mDeviceIdlingTimer = new StopwatchTimer(this.mClocks, null, -12, null, this.mOnBatteryTimeBase, parcel2);
            this.mPhoneOnTimer = new StopwatchTimer(this.mClocks, null, -3, null, this.mOnBatteryTimeBase, parcel2);
            i3 = 0;
            while (true) {
                i = i3;
                if (i >= 5) {
                    break;
                }
                this.mPhoneSignalStrengthsTimer[i] = new StopwatchTimer(this.mClocks, null, -200 - i, null, this.mOnBatteryTimeBase, parcel);
                i3 = i + 1;
            }
            this.mPhoneSignalScanningTimer = new StopwatchTimer(this.mClocks, null, -199, null, this.mOnBatteryTimeBase, parcel);
            i3 = 0;
            while (true) {
                i = i3;
                if (i >= 21) {
                    break;
                }
                this.mPhoneDataConnectionsTimer[i] = new StopwatchTimer(this.mClocks, null, -300 - i, null, this.mOnBatteryTimeBase, parcel);
                i3 = i + 1;
            }
            for (i3 = 0; i3 < 10; i3++) {
                this.mNetworkByteActivityCounters[i3] = new LongSamplingCounter(this.mOnBatteryTimeBase, parcel);
                this.mNetworkPacketActivityCounters[i3] = new LongSamplingCounter(this.mOnBatteryTimeBase, parcel);
            }
            this.mMobileRadioPowerState = 1;
            parcel2 = parcel;
            this.mMobileRadioActiveTimer = new StopwatchTimer(this.mClocks, null, -400, null, this.mOnBatteryTimeBase, parcel2);
            this.mMobileRadioActivePerAppTimer = new StopwatchTimer(this.mClocks, null, -401, null, this.mOnBatteryTimeBase, parcel2);
            this.mMobileRadioActiveAdjustedTime = new LongSamplingCounter(this.mOnBatteryTimeBase, parcel);
            this.mMobileRadioActiveUnknownTime = new LongSamplingCounter(this.mOnBatteryTimeBase, parcel);
            this.mMobileRadioActiveUnknownCount = new LongSamplingCounter(this.mOnBatteryTimeBase, parcel);
            this.mWifiMulticastWakelockTimer = new StopwatchTimer(this.mClocks, null, -4, null, this.mOnBatteryTimeBase, parcel2);
            this.mWifiRadioPowerState = 1;
            this.mWifiOn = false;
            this.mWifiOnTimer = new StopwatchTimer(this.mClocks, null, -4, null, this.mOnBatteryTimeBase, parcel2);
            this.mGlobalWifiRunning = false;
            this.mGlobalWifiRunningTimer = new StopwatchTimer(this.mClocks, null, -5, null, this.mOnBatteryTimeBase, parcel2);
            i3 = 0;
            while (true) {
                i = i3;
                if (i >= 8) {
                    break;
                }
                this.mWifiStateTimer[i] = new StopwatchTimer(this.mClocks, null, -600 - i, null, this.mOnBatteryTimeBase, parcel);
                i3 = i + 1;
            }
            i3 = 0;
            while (true) {
                i = i3;
                if (i >= 13) {
                    break;
                }
                this.mWifiSupplStateTimer[i] = new StopwatchTimer(this.mClocks, null, -700 - i, null, this.mOnBatteryTimeBase, parcel);
                i3 = i + 1;
            }
            i3 = 0;
            while (true) {
                i = i3;
                if (i >= 5) {
                    break;
                }
                this.mWifiSignalStrengthsTimer[i] = new StopwatchTimer(this.mClocks, null, -800 - i, null, this.mOnBatteryTimeBase, parcel);
                i3 = i + 1;
            }
            this.mWifiActiveTimer = new StopwatchTimer(this.mClocks, null, -900, null, this.mOnBatteryTimeBase, parcel);
            this.mWifiActivity = new ControllerActivityCounterImpl(this.mOnBatteryTimeBase, 1, parcel);
            i3 = 0;
            while (true) {
                i = i3;
                if (i >= 2) {
                    break;
                }
                this.mGpsSignalQualityTimer[i] = new StopwatchTimer(this.mClocks, null, -1000 - i, null, this.mOnBatteryTimeBase, parcel);
                i3 = i + 1;
            }
            this.mBluetoothActivity = new ControllerActivityCounterImpl(this.mOnBatteryTimeBase, 1, parcel);
            this.mModemActivity = new ControllerActivityCounterImpl(this.mOnBatteryTimeBase, 5, parcel);
            this.mHasWifiReporting = in.readInt() != 0;
            this.mHasBluetoothReporting = in.readInt() != 0;
            if (in.readInt() == 0) {
                z = false;
            }
            this.mHasModemReporting = z;
            this.mNumConnectivityChange = in.readInt();
            this.mLoadedNumConnectivityChange = in.readInt();
            this.mUnpluggedNumConnectivityChange = in.readInt();
            this.mAudioOnNesting = 0;
            this.mAudioOnTimer = new StopwatchTimer(this.mClocks, null, -7, null, this.mOnBatteryTimeBase);
            this.mVideoOnNesting = 0;
            this.mVideoOnTimer = new StopwatchTimer(this.mClocks, null, -8, null, this.mOnBatteryTimeBase);
            this.mFlashlightOnNesting = 0;
            parcel2 = parcel;
            this.mFlashlightOnTimer = new StopwatchTimer(this.mClocks, null, -9, null, this.mOnBatteryTimeBase, parcel2);
            this.mCameraOnNesting = 0;
            this.mCameraOnTimer = new StopwatchTimer(this.mClocks, null, -13, null, this.mOnBatteryTimeBase, parcel2);
            this.mBluetoothScanNesting = 0;
            this.mBluetoothScanTimer = new StopwatchTimer(this.mClocks, null, -14, null, this.mOnBatteryTimeBase, parcel2);
            this.mIsCellularTxPowerHigh = false;
            this.mDischargeUnplugLevel = in.readInt();
            this.mDischargePlugLevel = in.readInt();
            this.mDischargeCurrentLevel = in.readInt();
            this.mCurrentBatteryLevel = in.readInt();
            this.mLowDischargeAmountSinceCharge = in.readInt();
            this.mHighDischargeAmountSinceCharge = in.readInt();
            this.mDischargeAmountScreenOn = in.readInt();
            this.mDischargeAmountScreenOnSinceCharge = in.readInt();
            this.mDischargeAmountScreenOff = in.readInt();
            this.mDischargeAmountScreenOffSinceCharge = in.readInt();
            this.mDischargeAmountScreenDoze = in.readInt();
            this.mDischargeAmountScreenDozeSinceCharge = in.readInt();
            this.mDischargeStepTracker.readFromParcel(parcel);
            this.mChargeStepTracker.readFromParcel(parcel);
            this.mDischargeCounter = new LongSamplingCounter(this.mOnBatteryTimeBase, parcel);
            this.mDischargeScreenOffCounter = new LongSamplingCounter(this.mOnBatteryScreenOffTimeBase, parcel);
            this.mDischargeScreenDozeCounter = new LongSamplingCounter(this.mOnBatteryTimeBase, parcel);
            this.mDischargeLightDozeCounter = new LongSamplingCounter(this.mOnBatteryTimeBase, parcel);
            this.mDischargeDeepDozeCounter = new LongSamplingCounter(this.mOnBatteryTimeBase, parcel);
            this.mLastWriteTime = in.readLong();
            this.mRpmStats.clear();
            i3 = in.readInt();
            for (irpm = 0; irpm < i3; irpm++) {
                if (in.readInt() != 0) {
                    this.mRpmStats.put(in.readString(), new SamplingTimer(this.mClocks, this.mOnBatteryTimeBase, parcel));
                }
            }
            this.mScreenOffRpmStats.clear();
            irpm = in.readInt();
            for (irpm2 = 0; irpm2 < irpm; irpm2++) {
                if (in.readInt() != 0) {
                    this.mScreenOffRpmStats.put(in.readString(), new SamplingTimer(this.mClocks, this.mOnBatteryScreenOffTimeBase, parcel));
                }
            }
            this.mKernelWakelockStats.clear();
            irpm2 = in.readInt();
            for (ikw = 0; ikw < irpm2; ikw++) {
                if (in.readInt() != 0) {
                    this.mKernelWakelockStats.put(in.readString(), new SamplingTimer(this.mClocks, this.mOnBatteryScreenOffTimeBase, parcel));
                }
            }
            this.mWakeupReasonStats.clear();
            ikw = in.readInt();
            for (iwr = 0; iwr < ikw; iwr++) {
                if (in.readInt() != 0) {
                    this.mWakeupReasonStats.put(in.readString(), new SamplingTimer(this.mClocks, this.mOnBatteryTimeBase, parcel));
                }
            }
            this.mKernelMemoryStats.clear();
            iwr = in.readInt();
            for (imt = 0; imt < iwr; imt++) {
                if (in.readInt() != 0) {
                    this.mKernelMemoryStats.put(Long.valueOf(in.readLong()).longValue(), new SamplingTimer(this.mClocks, this.mOnBatteryTimeBase, parcel));
                }
            }
            this.mPartialTimers.clear();
            this.mFullTimers.clear();
            this.mWindowTimers.clear();
            this.mWifiRunningTimers.clear();
            this.mFullWifiLockTimers.clear();
            this.mWifiScanTimers.clear();
            this.mWifiBatchedScanTimers.clear();
            this.mWifiMulticastTimers.clear();
            this.mAudioTurnedOnTimers.clear();
            this.mVideoTurnedOnTimers.clear();
            this.mFlashlightTurnedOnTimers.clear();
            this.mCameraTurnedOnTimers.clear();
            imt = in.readInt();
            this.mUidStats.clear();
            while (true) {
                int i4 = i2;
                if (i4 < imt) {
                    i2 = in.readInt();
                    Uid u = new Uid(this, i2);
                    u.readFromParcelLocked(this.mOnBatteryTimeBase, this.mOnBatteryScreenOffTimeBase, parcel);
                    this.mUidStats.append(i2, u);
                    i2 = i4 + 1;
                } else {
                    return;
                }
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Bad magic number: #");
        stringBuilder.append(Integer.toHexString(magic));
        throw new ParcelFormatException(stringBuilder.toString());
    }

    public void writeToParcel(Parcel out, int flags) {
        writeToParcelLocked(out, true, flags);
    }

    public void writeToParcelWithoutUids(Parcel out, int flags) {
        writeToParcelLocked(out, false, flags);
    }

    void writeToParcelLocked(Parcel out, boolean inclUids, int flags) {
        int i;
        SamplingTimer rpmt;
        Parcel parcel = out;
        pullPendingStateUpdatesLocked();
        long startClockTime = getStartClockTime();
        long uSecUptime = this.mClocks.uptimeMillis() * 1000;
        long uSecRealtime = this.mClocks.elapsedRealtime() * 1000;
        long batteryRealtime = this.mOnBatteryTimeBase.getRealtime(uSecRealtime);
        long batteryScreenOffRealtime = this.mOnBatteryScreenOffTimeBase.getRealtime(uSecRealtime);
        parcel.writeInt(MAGIC);
        writeHistory(parcel, true, false);
        parcel.writeInt(this.mStartCount);
        parcel.writeLong(startClockTime);
        parcel.writeString(this.mStartPlatformVersion);
        parcel.writeString(this.mEndPlatformVersion);
        parcel.writeLong(this.mUptime);
        parcel.writeLong(this.mUptimeStart);
        parcel.writeLong(this.mRealtime);
        parcel.writeLong(this.mRealtimeStart);
        parcel.writeInt(this.mOnBattery);
        parcel.writeInt(this.mEstimatedBatteryCapacity);
        parcel.writeInt(this.mMinLearnedBatteryCapacity);
        parcel.writeInt(this.mMaxLearnedBatteryCapacity);
        Parcel parcel2 = parcel;
        long j = uSecUptime;
        int i2 = 1;
        int i3 = 0;
        long j2 = uSecRealtime;
        this.mOnBatteryTimeBase.writeToParcel(parcel2, j, j2);
        this.mOnBatteryScreenOffTimeBase.writeToParcel(parcel2, j, j2);
        this.mScreenOnTimer.writeToParcel(parcel, uSecRealtime);
        this.mScreenDozeTimer.writeToParcel(parcel, uSecRealtime);
        for (i = i3; i < 5; i++) {
            this.mScreenBrightnessTimer[i].writeToParcel(parcel, uSecRealtime);
        }
        this.mInteractiveTimer.writeToParcel(parcel, uSecRealtime);
        this.mPowerSaveModeEnabledTimer.writeToParcel(parcel, uSecRealtime);
        parcel.writeLong(this.mLongestLightIdleTime);
        parcel.writeLong(this.mLongestFullIdleTime);
        this.mDeviceIdleModeLightTimer.writeToParcel(parcel, uSecRealtime);
        this.mDeviceIdleModeFullTimer.writeToParcel(parcel, uSecRealtime);
        this.mDeviceLightIdlingTimer.writeToParcel(parcel, uSecRealtime);
        this.mDeviceIdlingTimer.writeToParcel(parcel, uSecRealtime);
        this.mPhoneOnTimer.writeToParcel(parcel, uSecRealtime);
        for (i = i3; i < 5; i++) {
            this.mPhoneSignalStrengthsTimer[i].writeToParcel(parcel, uSecRealtime);
        }
        this.mPhoneSignalScanningTimer.writeToParcel(parcel, uSecRealtime);
        for (i = i3; i < 21; i++) {
            this.mPhoneDataConnectionsTimer[i].writeToParcel(parcel, uSecRealtime);
        }
        for (i = i3; i < 10; i++) {
            this.mNetworkByteActivityCounters[i].writeToParcel(parcel);
            this.mNetworkPacketActivityCounters[i].writeToParcel(parcel);
        }
        this.mMobileRadioActiveTimer.writeToParcel(parcel, uSecRealtime);
        this.mMobileRadioActivePerAppTimer.writeToParcel(parcel, uSecRealtime);
        this.mMobileRadioActiveAdjustedTime.writeToParcel(parcel);
        this.mMobileRadioActiveUnknownTime.writeToParcel(parcel);
        this.mMobileRadioActiveUnknownCount.writeToParcel(parcel);
        this.mWifiMulticastWakelockTimer.writeToParcel(parcel, uSecRealtime);
        this.mWifiOnTimer.writeToParcel(parcel, uSecRealtime);
        this.mGlobalWifiRunningTimer.writeToParcel(parcel, uSecRealtime);
        for (i = i3; i < 8; i++) {
            this.mWifiStateTimer[i].writeToParcel(parcel, uSecRealtime);
        }
        for (i = i3; i < 13; i++) {
            this.mWifiSupplStateTimer[i].writeToParcel(parcel, uSecRealtime);
        }
        for (i = i3; i < 5; i++) {
            this.mWifiSignalStrengthsTimer[i].writeToParcel(parcel, uSecRealtime);
        }
        this.mWifiActiveTimer.writeToParcel(parcel, uSecRealtime);
        this.mWifiActivity.writeToParcel(parcel, i3);
        for (i = i3; i < 2; i++) {
            this.mGpsSignalQualityTimer[i].writeToParcel(parcel, uSecRealtime);
        }
        this.mBluetoothActivity.writeToParcel(parcel, i3);
        this.mModemActivity.writeToParcel(parcel, i3);
        parcel.writeInt(this.mHasWifiReporting);
        parcel.writeInt(this.mHasBluetoothReporting);
        parcel.writeInt(this.mHasModemReporting);
        parcel.writeInt(this.mNumConnectivityChange);
        parcel.writeInt(this.mLoadedNumConnectivityChange);
        parcel.writeInt(this.mUnpluggedNumConnectivityChange);
        this.mFlashlightOnTimer.writeToParcel(parcel, uSecRealtime);
        this.mCameraOnTimer.writeToParcel(parcel, uSecRealtime);
        this.mBluetoothScanTimer.writeToParcel(parcel, uSecRealtime);
        parcel.writeInt(this.mDischargeUnplugLevel);
        parcel.writeInt(this.mDischargePlugLevel);
        parcel.writeInt(this.mDischargeCurrentLevel);
        parcel.writeInt(this.mCurrentBatteryLevel);
        parcel.writeInt(this.mLowDischargeAmountSinceCharge);
        parcel.writeInt(this.mHighDischargeAmountSinceCharge);
        parcel.writeInt(this.mDischargeAmountScreenOn);
        parcel.writeInt(this.mDischargeAmountScreenOnSinceCharge);
        parcel.writeInt(this.mDischargeAmountScreenOff);
        parcel.writeInt(this.mDischargeAmountScreenOffSinceCharge);
        parcel.writeInt(this.mDischargeAmountScreenDoze);
        parcel.writeInt(this.mDischargeAmountScreenDozeSinceCharge);
        this.mDischargeStepTracker.writeToParcel(parcel);
        this.mChargeStepTracker.writeToParcel(parcel);
        this.mDischargeCounter.writeToParcel(parcel);
        this.mDischargeScreenOffCounter.writeToParcel(parcel);
        this.mDischargeScreenDozeCounter.writeToParcel(parcel);
        this.mDischargeLightDozeCounter.writeToParcel(parcel);
        this.mDischargeDeepDozeCounter.writeToParcel(parcel);
        parcel.writeLong(this.mLastWriteTime);
        parcel.writeInt(this.mRpmStats.size());
        for (Entry<String, SamplingTimer> ent : this.mRpmStats.entrySet()) {
            rpmt = (SamplingTimer) ent.getValue();
            if (rpmt != null) {
                parcel.writeInt(i2);
                parcel.writeString((String) ent.getKey());
                rpmt.writeToParcel(parcel, uSecRealtime);
            } else {
                parcel.writeInt(i3);
            }
        }
        parcel.writeInt(this.mScreenOffRpmStats.size());
        for (Entry<String, SamplingTimer> ent2 : this.mScreenOffRpmStats.entrySet()) {
            rpmt = (SamplingTimer) ent2.getValue();
            if (rpmt != null) {
                parcel.writeInt(i2);
                parcel.writeString((String) ent2.getKey());
                rpmt.writeToParcel(parcel, uSecRealtime);
            } else {
                parcel.writeInt(i3);
            }
        }
        if (inclUids) {
            parcel.writeInt(this.mKernelWakelockStats.size());
            for (Entry<String, SamplingTimer> ent22 : this.mKernelWakelockStats.entrySet()) {
                rpmt = (SamplingTimer) ent22.getValue();
                if (rpmt != null) {
                    parcel.writeInt(i2);
                    parcel.writeString((String) ent22.getKey());
                    rpmt.writeToParcel(parcel, uSecRealtime);
                } else {
                    parcel.writeInt(i3);
                }
            }
            parcel.writeInt(this.mWakeupReasonStats.size());
            for (Entry<String, SamplingTimer> ent222 : this.mWakeupReasonStats.entrySet()) {
                rpmt = (SamplingTimer) ent222.getValue();
                if (rpmt != null) {
                    parcel.writeInt(i2);
                    parcel.writeString((String) ent222.getKey());
                    rpmt.writeToParcel(parcel, uSecRealtime);
                } else {
                    parcel.writeInt(i3);
                }
            }
        } else {
            parcel.writeInt(i3);
            parcel.writeInt(i3);
        }
        parcel.writeInt(this.mKernelMemoryStats.size());
        for (i = i3; i < this.mKernelMemoryStats.size(); i++) {
            SamplingTimer kmt = (SamplingTimer) this.mKernelMemoryStats.valueAt(i);
            if (kmt != null) {
                parcel.writeInt(i2);
                parcel.writeLong(this.mKernelMemoryStats.keyAt(i));
                kmt.writeToParcel(parcel, uSecRealtime);
            } else {
                parcel.writeInt(i3);
            }
        }
        if (inclUids) {
            i2 = this.mUidStats.size();
            parcel.writeInt(i2);
            while (i3 < i2) {
                parcel.writeInt(this.mUidStats.keyAt(i3));
                ((Uid) this.mUidStats.valueAt(i3)).writeToParcelLocked(parcel, uSecUptime, uSecRealtime);
                i3++;
            }
            return;
        }
        parcel.writeInt(i3);
    }

    public void prepareForDumpLocked() {
        pullPendingStateUpdatesLocked();
        getStartClockTime();
    }

    public void dumpLocked(Context context, PrintWriter pw, int flags, int reqUid, long histStart) {
        super.dumpLocked(context, pw, flags, reqUid, histStart);
        pw.print("Total cpu time reads: ");
        pw.println(this.mNumSingleUidCpuTimeReads);
        pw.print("Batched cpu time reads: ");
        pw.println(this.mNumBatchedSingleUidCpuTimeReads);
        pw.print("Batching Duration (min): ");
        pw.println((this.mClocks.uptimeMillis() - this.mCpuTimeReadsTrackingStartTime) / 60000);
        pw.print("All UID cpu time reads since the later of device start or stats reset: ");
        pw.println(this.mNumAllUidCpuTimeReads);
        pw.print("UIDs removed since the later of device start or stats reset: ");
        pw.println(this.mNumUidsRemoved);
    }
}
