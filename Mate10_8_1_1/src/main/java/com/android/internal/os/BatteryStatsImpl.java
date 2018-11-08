package com.android.internal.os;

import android.app.ActivityManager;
import android.bluetooth.BluetoothActivityEnergyInfo;
import android.bluetooth.UidTraffic;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkStats;
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
import android.provider.Telephony.BaseMmsColumns;
import android.rms.HwSysResource;
import android.telephony.ModemActivityInfo;
import android.telephony.SignalStrength;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.ArrayMap;
import android.util.HwLog;
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
import android.util.TimeUtils;
import android.util.Xml;
import com.android.ims.ImsConfig;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.logging.EventLogTags;
import com.android.internal.net.NetworkStatsFactory;
import com.android.internal.os.KernelUidCpuTimeReader.Callback;
import com.android.internal.os.RpmStats.PowerStateElement;
import com.android.internal.os.RpmStats.PowerStatePlatformSleepState;
import com.android.internal.os.RpmStats.PowerStateSubsystem;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.JournaledFile;
import com.android.internal.util.Protocol;
import com.android.internal.util.XmlUtils;
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
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
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
    static final int MSG_REPORT_POWER_CHANGE = 2;
    static final int MSG_UPDATE_WAKELOCKS = 1;
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
    private static final boolean USE_OLD_HISTORY = false;
    private static final int VERSION = 167;
    final HistoryEventTracker mActiveEvents;
    int mActiveHistoryStates;
    int mActiveHistoryStates2;
    int mAudioOnNesting;
    StopwatchTimer mAudioOnTimer;
    final ArrayList<StopwatchTimer> mAudioTurnedOnTimers;
    ControllerActivityCounterImpl mBluetoothActivity;
    int mBluetoothScanNesting;
    final ArrayList<StopwatchTimer> mBluetoothScanOnTimers;
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
    private long[] mCpuFreqs;
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
    public final MyHandler mHandler;
    boolean mHasBluetoothReporting;
    boolean mHasModemReporting;
    boolean mHasWifiReporting;
    boolean mHaveBatteryLevel;
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
    final SparseIntArray mIsolatedUids;
    private boolean mIteratingHistory;
    private KernelCpuSpeedReader[] mKernelCpuSpeedReaders;
    private final KernelMemoryBandwidthStats mKernelMemoryBandwidthStats;
    private final LongSparseArray<SamplingTimer> mKernelMemoryStats;
    private final KernelUidCpuFreqTimeReader mKernelUidCpuFreqTimeReader;
    private final KernelUidCpuTimeReader mKernelUidCpuTimeReader;
    private final KernelWakelockReader mKernelWakelockReader;
    private final HashMap<String, SamplingTimer> mKernelWakelockStats;
    int mLastChargeStepLevel;
    int mLastChargingStateLevel;
    int mLastDischargeStepLevel;
    long mLastHistoryElapsedRealtime;
    HistoryStepDetails mLastHistoryStepDetails;
    byte mLastHistoryStepLevel;
    long mLastIdleTimeStart;
    @GuardedBy("mModemNetworkLock")
    private NetworkStats mLastModemNetworkStats;
    final ArrayList<StopwatchTimer> mLastPartialTimers;
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
    private int mNumConnectivityChange;
    int mNumHistoryItems;
    int mNumHistoryTagChars;
    boolean mOnBattery;
    boolean mOnBatteryInternal;
    protected final TimeBase mOnBatteryScreenOffTimeBase;
    protected final TimeBase mOnBatteryTimeBase;
    final ArrayList<StopwatchTimer> mPartialTimers;
    Parcel mPendingWrite;
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
    private PowerProfile mPowerProfile;
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
    boolean mRecordingHistory;
    private final HashMap<String, SamplingTimer> mRpmStats;
    int mScreenBrightnessBin;
    final StopwatchTimer[] mScreenBrightnessTimer;
    protected StopwatchTimer mScreenDozeTimer;
    private final HashMap<String, SamplingTimer> mScreenOffRpmStats;
    protected StopwatchTimer mScreenOnTimer;
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
    private UserInfoProvider mUserInfoProvider;
    int mVideoOnNesting;
    StopwatchTimer mVideoOnTimer;
    final ArrayList<StopwatchTimer> mVideoTurnedOnTimers;
    boolean mWakeLockImportant;
    int mWakeLockNesting;
    private final HashMap<String, SamplingTimer> mWakeupReasonStats;
    ControllerActivityCounterImpl mWifiActivity;
    final SparseArray<ArrayList<StopwatchTimer>> mWifiBatchedScanTimers;
    int mWifiFullLockNesting;
    @GuardedBy("mWifiNetworkLock")
    private String[] mWifiIfaces;
    int mWifiMulticastNesting;
    final ArrayList<StopwatchTimer> mWifiMulticastTimers;
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

    public interface TimeBaseObs {
        void onTimeStarted(long j, long j2, long j3);

        void onTimeStopped(long j, long j2, long j3);
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
            pw.println(prefix + "mCount=" + this.mCount + " mLoadedCount=" + this.mLoadedCount + " mLastCount=" + this.mLastCount + " mUnpluggedCount=" + this.mUnpluggedCount);
            pw.println(prefix + "mTotalTime=" + this.mTotalTime + " mLoadedTime=" + this.mLoadedTime);
            pw.println(prefix + "mLastTime=" + this.mLastTime + " mUnpluggedTime=" + this.mUnpluggedTime);
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
            pw.println(prefix + "mLastAddedTime=" + this.mLastAddedTime + " mLastAddedDuration=" + this.mLastAddedDuration);
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
            this.mLastAddedDuration = durationMillis * 1000;
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
            if (stillActive) {
                detachIfReset = false;
            }
            super.reset(detachIfReset);
            return stillActive ^ 1;
        }
    }

    public interface BatteryCallback {
        void batteryNeedsCpuUpdate();

        void batteryPowerChanged(boolean z);

        void batterySendBroadcast(Intent intent);
    }

    public interface Clocks {
        long elapsedRealtime();

        long uptimeMillis();
    }

    public static class ControllerActivityCounterImpl extends ControllerActivityCounter implements Parcelable {
        private final LongSamplingCounter mIdleTimeMillis;
        private final LongSamplingCounter mPowerDrainMaMs;
        private final LongSamplingCounter mRxTimeMillis;
        private final LongSamplingCounter[] mTxTimeMillis;

        public ControllerActivityCounterImpl(TimeBase timeBase, int numTxStates) {
            this.mIdleTimeMillis = new LongSamplingCounter(timeBase);
            this.mRxTimeMillis = new LongSamplingCounter(timeBase);
            this.mTxTimeMillis = new LongSamplingCounter[numTxStates];
            for (int i = 0; i < numTxStates; i++) {
                this.mTxTimeMillis[i] = new LongSamplingCounter(timeBase);
            }
            this.mPowerDrainMaMs = new LongSamplingCounter(timeBase);
        }

        public ControllerActivityCounterImpl(TimeBase timeBase, int numTxStates, Parcel in) {
            this.mIdleTimeMillis = new LongSamplingCounter(timeBase, in);
            this.mRxTimeMillis = new LongSamplingCounter(timeBase, in);
            if (in.readInt() != numTxStates) {
                throw new ParcelFormatException("inconsistent tx state lengths");
            }
            this.mTxTimeMillis = new LongSamplingCounter[numTxStates];
            for (int i = 0; i < numTxStates; i++) {
                this.mTxTimeMillis[i] = new LongSamplingCounter(timeBase, in);
            }
            this.mPowerDrainMaMs = new LongSamplingCounter(timeBase, in);
        }

        public void readSummaryFromParcel(Parcel in) {
            this.mIdleTimeMillis.readSummaryFromParcelLocked(in);
            this.mRxTimeMillis.readSummaryFromParcelLocked(in);
            if (in.readInt() != this.mTxTimeMillis.length) {
                throw new ParcelFormatException("inconsistent tx state lengths");
            }
            for (LongSamplingCounter counter : this.mTxTimeMillis) {
                counter.readSummaryFromParcelLocked(in);
            }
            this.mPowerDrainMaMs.readSummaryFromParcelLocked(in);
        }

        public int describeContents() {
            return 0;
        }

        public void writeSummaryToParcel(Parcel dest) {
            this.mIdleTimeMillis.writeSummaryFromParcelLocked(dest);
            this.mRxTimeMillis.writeSummaryFromParcelLocked(dest);
            dest.writeInt(this.mTxTimeMillis.length);
            for (LongSamplingCounter counter : this.mTxTimeMillis) {
                counter.writeSummaryFromParcelLocked(dest);
            }
            this.mPowerDrainMaMs.writeSummaryFromParcelLocked(dest);
        }

        public void writeToParcel(Parcel dest, int flags) {
            this.mIdleTimeMillis.writeToParcel(dest);
            this.mRxTimeMillis.writeToParcel(dest);
            dest.writeInt(this.mTxTimeMillis.length);
            for (LongSamplingCounter counter : this.mTxTimeMillis) {
                counter.writeToParcel(dest);
            }
            this.mPowerDrainMaMs.writeToParcel(dest);
        }

        public void reset(boolean detachIfReset) {
            this.mIdleTimeMillis.reset(detachIfReset);
            this.mRxTimeMillis.reset(detachIfReset);
            for (LongSamplingCounter counter : this.mTxTimeMillis) {
                counter.reset(detachIfReset);
            }
            this.mPowerDrainMaMs.reset(detachIfReset);
        }

        public void detach() {
            this.mIdleTimeMillis.detach();
            this.mRxTimeMillis.detach();
            for (LongSamplingCounter counter : this.mTxTimeMillis) {
                counter.detach();
            }
            this.mPowerDrainMaMs.detach();
        }

        public LongSamplingCounter getIdleTimeCounter() {
            return this.mIdleTimeMillis;
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
            pw.println(prefix + "mCount=" + this.mCount.get() + " mLoadedCount=" + this.mLoadedCount + " mUnpluggedCount=" + this.mUnpluggedCount + " mPluggedCount=" + this.mPluggedCount);
        }

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

        public void writeSummaryFromParcelLocked(Parcel out) {
            out.writeInt(this.mCount.get());
        }

        public void readSummaryFromParcelLocked(Parcel in) {
            this.mLoadedCount = in.readInt();
            this.mCount.set(this.mLoadedCount);
            int i = this.mLoadedCount;
            this.mPluggedCount = i;
            this.mUnpluggedCount = i;
        }
    }

    public static class StopwatchTimer extends Timer {
        long mAcquireTime = -1;
        boolean mInList;
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
            pw.println(prefix + "mNesting=" + this.mNesting + " mUpdateTime=" + this.mUpdateTime + " mAcquireTime=" + this.mAcquireTime);
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
                j = curBatteryRealtime - this.mUpdateTime;
                if (size <= 0) {
                    size = 1;
                }
                j /= (long) size;
            }
            return j + j2;
        }

        protected int computeCurrentCountLocked() {
            return this.mCount;
        }

        public boolean reset(boolean detachIfReset) {
            boolean canDetach = this.mNesting <= 0;
            if (!canDetach) {
                detachIfReset = false;
            }
            super.reset(detachIfReset);
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
            return ((this.mSubTimer.reset(false) ^ 1) | (super.reset(detachIfReset) ^ 1)) ^ 1;
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

    public interface ExternalStatsSync {
        public static final int UPDATE_ALL = 31;
        public static final int UPDATE_BT = 8;
        public static final int UPDATE_CPU = 1;
        public static final int UPDATE_RADIO = 4;
        public static final int UPDATE_RPM = 16;
        public static final int UPDATE_WIFI = 2;

        Future<?> scheduleCpuSyncDueToRemovedUid(int i);

        Future<?> scheduleSync(String str, int i);
    }

    public static class LongSamplingCounter extends LongCounter implements TimeBaseObs {
        long mCount;
        long mLoadedCount;
        long mPluggedCount;
        final TimeBase mTimeBase;
        long mUnpluggedCount;

        LongSamplingCounter(TimeBase timeBase, Parcel in) {
            this.mTimeBase = timeBase;
            this.mPluggedCount = in.readLong();
            this.mCount = this.mPluggedCount;
            this.mLoadedCount = in.readLong();
            this.mUnpluggedCount = in.readLong();
            timeBase.add(this);
        }

        LongSamplingCounter(TimeBase timeBase) {
            this.mTimeBase = timeBase;
            timeBase.add(this);
        }

        public void writeToParcel(Parcel out) {
            out.writeLong(this.mCount);
            out.writeLong(this.mLoadedCount);
            out.writeLong(this.mUnpluggedCount);
        }

        public void onTimeStarted(long elapsedRealtime, long baseUptime, long baseRealtime) {
            this.mUnpluggedCount = this.mPluggedCount;
        }

        public void onTimeStopped(long elapsedRealtime, long baseUptime, long baseRealtime) {
            this.mPluggedCount = this.mCount;
        }

        public long getCountLocked(int which) {
            long val = this.mTimeBase.isRunning() ? this.mCount : this.mPluggedCount;
            if (which == 2) {
                return val - this.mUnpluggedCount;
            }
            if (which != 0) {
                return val - this.mLoadedCount;
            }
            return val;
        }

        public void logState(Printer pw, String prefix) {
            pw.println(prefix + "mCount=" + this.mCount + " mLoadedCount=" + this.mLoadedCount + " mUnpluggedCount=" + this.mUnpluggedCount + " mPluggedCount=" + this.mPluggedCount);
        }

        void addCountLocked(long count) {
            if (this.mTimeBase.isRunning()) {
                this.mCount += count;
            }
        }

        void reset(boolean detachIfReset) {
            this.mCount = 0;
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

        void writeSummaryFromParcelLocked(Parcel out) {
            out.writeLong(this.mCount);
        }

        void readSummaryFromParcelLocked(Parcel in) {
            this.mLoadedCount = in.readLong();
            this.mCount = this.mLoadedCount;
            long j = this.mLoadedCount;
            this.mPluggedCount = j;
            this.mUnpluggedCount = j;
        }
    }

    public static class LongSamplingCounterArray extends LongCounterArray implements TimeBaseObs {
        public long[] mCounts;
        public long[] mLoadedCounts;
        public long[] mPluggedCounts;
        final TimeBase mTimeBase;
        public long[] mUnpluggedCounts;

        private LongSamplingCounterArray(TimeBase timeBase, Parcel in) {
            this.mTimeBase = timeBase;
            this.mPluggedCounts = in.createLongArray();
            this.mCounts = copyArray(this.mPluggedCounts, this.mCounts);
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
            this.mUnpluggedCounts = copyArray(this.mPluggedCounts, this.mUnpluggedCounts);
        }

        public void onTimeStopped(long elapsedRealtime, long baseUptime, long baseRealtime) {
            this.mPluggedCounts = copyArray(this.mCounts, this.mPluggedCounts);
        }

        public long[] getCountsLocked(int which) {
            long[] val = copyArray(this.mTimeBase.isRunning() ? this.mCounts : this.mPluggedCounts, null);
            if (which == 2) {
                subtract(val, this.mUnpluggedCounts);
            } else if (which != 0) {
                subtract(val, this.mLoadedCounts);
            }
            return val;
        }

        public void logState(Printer pw, String prefix) {
            pw.println(prefix + "mCounts=" + Arrays.toString(this.mCounts) + " mLoadedCounts=" + Arrays.toString(this.mLoadedCounts) + " mUnpluggedCounts=" + Arrays.toString(this.mUnpluggedCounts) + " mPluggedCounts=" + Arrays.toString(this.mPluggedCounts));
        }

        public void addCountLocked(long[] counts) {
            if (counts != null && this.mTimeBase.isRunning()) {
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
            fillArray(this.mPluggedCounts, 0);
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
            this.mPluggedCounts = copyArray(this.mCounts, this.mPluggedCounts);
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
            if (dest == null) {
                dest = new long[src.length];
            }
            System.arraycopy(src, 0, dest, 0, src.length);
            return dest;
        }
    }

    final class MyHandler extends Handler {
        public MyHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            boolean z = false;
            BatteryCallback cb = BatteryStatsImpl.this.mCallback;
            switch (msg.what) {
                case 1:
                    synchronized (BatteryStatsImpl.this) {
                        BatteryStatsImpl.this.updateCpuTimeLocked(false);
                    }
                    if (cb != null) {
                        cb.batteryNeedsCpuUpdate();
                        return;
                    }
                    return;
                case 2:
                    if (cb != null) {
                        if (msg.arg1 != 0) {
                            z = true;
                        }
                        cb.batteryPowerChanged(z);
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
                default:
                    return;
            }
        }
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
            this.mLastCleanupTime = SystemClock.elapsedRealtime();
            if (this.mActiveOverflowWeixin != null && this.mActiveOverflowWeixin.size() == 0) {
                this.mActiveOverflowWeixin = null;
            }
            if (this.mActiveOverflowWeixin == null) {
                if (this.mMap.containsKey(OVERFLOW_WEIXIN)) {
                    Slog.wtf(BatteryStatsImpl.TAG, "Cleaning up with no active overflow weixin, but have overflow entry " + this.mMap.get(OVERFLOW_WEIXIN));
                    this.mMap.remove(OVERFLOW_WEIXIN);
                }
                this.mCurOverflowWeixin = null;
            } else if (this.mCurOverflowWeixin == null || (this.mMap.containsKey(OVERFLOW_WEIXIN) ^ 1) != 0) {
                Slog.wtf(BatteryStatsImpl.TAG, "Cleaning up with active overflow weixin, but no overflow entry: cur=" + this.mCurOverflowWeixin + " map=" + this.mMap.get(OVERFLOW_WEIXIN));
            }
            if (this.mActiveOverflow != null && this.mActiveOverflow.size() == 0) {
                this.mActiveOverflow = null;
            }
            if (this.mActiveOverflow == null) {
                if (this.mMap.containsKey(OVERFLOW_NAME)) {
                    Slog.wtf(BatteryStatsImpl.TAG, "Cleaning up with no active overflow, but have overflow entry " + this.mMap.get(OVERFLOW_NAME));
                    this.mMap.remove(OVERFLOW_NAME);
                }
                this.mCurOverflow = null;
            } else if (this.mCurOverflow == null || (this.mMap.containsKey(OVERFLOW_NAME) ^ 1) != 0) {
                Slog.wtf(BatteryStatsImpl.TAG, "Cleaning up with active overflow, but no overflow entry: cur=" + this.mCurOverflow + " map=" + this.mMap.get(OVERFLOW_NAME));
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
            if (this.mActiveOverflowWeixin != null) {
                over = (MutableInt) this.mActiveOverflowWeixin.get(name);
                if (over != null) {
                    obj = this.mCurOverflowWeixin;
                    if (obj == null) {
                        Slog.wtf(BatteryStatsImpl.TAG, "Have active overflow " + name + " but null overflow weixin");
                        obj = instantiateObject();
                        this.mCurOverflowWeixin = obj;
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
                        obj = instantiateObject();
                        this.mCurOverflowWeixin = obj;
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
                        Slog.wtf(BatteryStatsImpl.TAG, "Have active overflow " + name + " but null overflow");
                        obj = instantiateObject();
                        this.mCurOverflow = obj;
                        this.mMap.put(OVERFLOW_NAME, obj);
                    }
                    over.value++;
                    return obj;
                }
            }
            if (this.mMap.size() >= BatteryStatsImpl.MAX_WAKELOCKS_PER_UID) {
                Slog.i(BatteryStatsImpl.TAG, "wakelocks more than 100, name: " + name);
                obj = this.mCurOverflow;
                if (obj == null) {
                    obj = instantiateObject();
                    this.mCurOverflow = obj;
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

    public static class SamplingTimer extends Timer {
        int mCurrentReportedCount;
        long mCurrentReportedTotalTime;
        boolean mTimeBaseRunning;
        boolean mTrackingReportedValues;
        int mUnpluggedReportedCount;
        long mUnpluggedReportedTotalTime;
        int mUpdateVersion;

        public SamplingTimer(Clocks clocks, TimeBase timeBase, Parcel in) {
            boolean z = true;
            super(clocks, 0, timeBase, in);
            this.mCurrentReportedCount = in.readInt();
            this.mUnpluggedReportedCount = in.readInt();
            this.mCurrentReportedTotalTime = in.readLong();
            this.mUnpluggedReportedTotalTime = in.readLong();
            if (in.readInt() != 1) {
                z = false;
            }
            this.mTrackingReportedValues = z;
            this.mTimeBaseRunning = timeBase.isRunning();
        }

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
            if (this.mTimeBaseRunning && (this.mTrackingReportedValues ^ 1) != 0) {
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
            pw.println(prefix + "mCurrentReportedCount=" + this.mCurrentReportedCount + " mUnpluggedReportedCount=" + this.mUnpluggedReportedCount + " mCurrentReportedTotalTime=" + this.mCurrentReportedTotalTime + " mUnpluggedReportedTotalTime=" + this.mUnpluggedReportedTotalTime);
        }

        protected long computeRunTimeLocked(long curBatteryRealtime) {
            long j = this.mTotalTime;
            long j2 = (this.mTimeBaseRunning && this.mTrackingReportedValues) ? this.mCurrentReportedTotalTime - this.mUnpluggedReportedTotalTime : 0;
            return j2 + j;
        }

        protected int computeCurrentCountLocked() {
            int i = this.mCount;
            int i2 = (this.mTimeBaseRunning && this.mTrackingReportedValues) ? this.mCurrentReportedCount - this.mUnpluggedReportedCount : 0;
            return i2 + i;
        }

        public void writeToParcel(Parcel out, long elapsedRealtimeUs) {
            super.writeToParcel(out, elapsedRealtimeUs);
            out.writeInt(this.mCurrentReportedCount);
            out.writeInt(this.mUnpluggedReportedCount);
            out.writeLong(this.mCurrentReportedTotalTime);
            out.writeLong(this.mUnpluggedReportedTotalTime);
            out.writeInt(this.mTrackingReportedValues ? 1 : 0);
        }

        public boolean reset(boolean detachIfReset) {
            super.reset(detachIfReset);
            this.mTrackingReportedValues = false;
            this.mUnpluggedReportedTotalTime = 0;
            this.mUnpluggedReportedCount = 0;
            return true;
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
            BatteryStatsImpl.formatTimeMs(sb, this.mUptime / 1000);
            pw.println(sb.toString());
            sb.setLength(0);
            sb.append(prefix);
            sb.append("mRealtime=");
            BatteryStatsImpl.formatTimeMs(sb, this.mRealtime / 1000);
            pw.println(sb.toString());
            sb.setLength(0);
            sb.append(prefix);
            sb.append("mPastUptime=");
            BatteryStatsImpl.formatTimeMs(sb, this.mPastUptime / 1000);
            sb.append("mUptimeStart=");
            BatteryStatsImpl.formatTimeMs(sb, this.mUptimeStart / 1000);
            sb.append("mUnpluggedUptime=");
            BatteryStatsImpl.formatTimeMs(sb, this.mUnpluggedUptime / 1000);
            pw.println(sb.toString());
            sb.setLength(0);
            sb.append(prefix);
            sb.append("mPastRealtime=");
            BatteryStatsImpl.formatTimeMs(sb, this.mPastRealtime / 1000);
            sb.append("mRealtimeStart=");
            BatteryStatsImpl.formatTimeMs(sb, this.mRealtimeStart / 1000);
            sb.append("mUnpluggedRealtime=");
            BatteryStatsImpl.formatTimeMs(sb, this.mUnpluggedRealtime / 1000);
            pw.println(sb.toString());
        }

        public synchronized void add(TimeBaseObs observer) {
            this.mObservers.add(observer);
        }

        public synchronized void remove(TimeBaseObs observer) {
            if (!this.mObservers.remove(observer)) {
                Slog.wtf(BatteryStatsImpl.TAG, "Removed unknown observer: " + observer);
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

        public synchronized boolean setRunning(boolean running, long uptime, long realtime) {
            if (this.mRunning == running) {
                return false;
            }
            this.mRunning = running;
            long batteryUptime;
            long batteryRealtime;
            int i;
            if (running) {
                this.mUptimeStart = uptime;
                this.mRealtimeStart = realtime;
                batteryUptime = getUptime(uptime);
                this.mUnpluggedUptime = batteryUptime;
                batteryRealtime = getRealtime(realtime);
                this.mUnpluggedRealtime = batteryRealtime;
                for (i = this.mObservers.size() - 1; i >= 0; i--) {
                    ((TimeBaseObs) this.mObservers.get(i)).onTimeStarted(realtime, batteryUptime, batteryRealtime);
                }
            } else {
                this.mPastUptime += uptime - this.mUptimeStart;
                this.mPastRealtime += realtime - this.mRealtimeStart;
                batteryUptime = getUptime(uptime);
                batteryRealtime = getRealtime(realtime);
                for (i = this.mObservers.size() - 1; i >= 0; i--) {
                    ((TimeBaseObs) this.mObservers.get(i)).onTimeStopped(realtime, batteryUptime, batteryRealtime);
                }
            }
            return true;
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
        LongSamplingCounter[][] mCpuClusterSpeedTimesUs;
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
        long mLastStepSystemTime;
        long mLastStepUserTime;
        LongSamplingCounter mMobileRadioActiveCount;
        LongSamplingCounter mMobileRadioActiveTime;
        private LongSamplingCounter mMobileRadioApWakeupCount;
        private ControllerActivityCounterImpl mModemControllerActivity;
        LongSamplingCounter[] mNetworkByteActivityCounters;
        LongSamplingCounter[] mNetworkPacketActivityCounters;
        public final TimeBase mOnBatteryBackgroundTimeBase;
        public final TimeBase mOnBatteryScreenOffBackgroundTimeBase;
        final ArrayMap<String, Pkg> mPackageStats = new ArrayMap();
        final SparseArray<Pid> mPids = new SparseArray();
        int mProcessState = 18;
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

                public Serv(com.android.internal.os.BatteryStatsImpl r2) {
                    /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                    /*
                    r1 = this;
                    r1.<init>();
                    r1.mBsi = r2;
                    r0 = r1.mBsi;
                    r0 = r0.mOnBatteryTimeBase;
                    r0.add(r1);
                    return;
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Pkg.Serv.<init>(com.android.internal.os.BatteryStatsImpl):void");
                }

                public void onTimeStarted(long r4, long r6, long r8) {
                    /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                    /*
                    r3 = this;
                    r0 = r3.getStartTimeToNowLocked(r6);
                    r3.mUnpluggedStartTime = r0;
                    r0 = r3.mStarts;
                    r3.mUnpluggedStarts = r0;
                    r0 = r3.mLaunches;
                    r3.mUnpluggedLaunches = r0;
                    return;
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Pkg.Serv.onTimeStarted(long, long, long):void");
                }

                public void onTimeStopped(long r1, long r3, long r5) {
                    /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                    /*
                    r0 = this;
                    return;
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Pkg.Serv.onTimeStopped(long, long, long):void");
                }

                public void detach() {
                    /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                    /*
                    r1 = this;
                    r0 = r1.mBsi;
                    r0 = r0.mOnBatteryTimeBase;
                    r0.remove(r1);
                    return;
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Pkg.Serv.detach():void");
                }

                public void readFromParcelLocked(android.os.Parcel r7) {
                    /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                    /*
                    r6 = this;
                    r1 = 1;
                    r2 = 0;
                    r4 = r7.readLong();
                    r6.mStartTime = r4;
                    r4 = r7.readLong();
                    r6.mRunningSince = r4;
                    r0 = r7.readInt();
                    if (r0 == 0) goto L_0x0064;
                L_0x0014:
                    r0 = r1;
                L_0x0015:
                    r6.mRunning = r0;
                    r0 = r7.readInt();
                    r6.mStarts = r0;
                    r4 = r7.readLong();
                    r6.mLaunchedTime = r4;
                    r4 = r7.readLong();
                    r6.mLaunchedSince = r4;
                    r0 = r7.readInt();
                    if (r0 == 0) goto L_0x0066;
                L_0x002f:
                    r6.mLaunched = r1;
                    r0 = r7.readInt();
                    r6.mLaunches = r0;
                    r0 = r7.readLong();
                    r6.mLoadedStartTime = r0;
                    r0 = r7.readInt();
                    r6.mLoadedStarts = r0;
                    r0 = r7.readInt();
                    r6.mLoadedLaunches = r0;
                    r0 = 0;
                    r6.mLastStartTime = r0;
                    r6.mLastStarts = r2;
                    r6.mLastLaunches = r2;
                    r0 = r7.readLong();
                    r6.mUnpluggedStartTime = r0;
                    r0 = r7.readInt();
                    r6.mUnpluggedStarts = r0;
                    r0 = r7.readInt();
                    r6.mUnpluggedLaunches = r0;
                    return;
                L_0x0064:
                    r0 = r2;
                    goto L_0x0015;
                L_0x0066:
                    r1 = r2;
                    goto L_0x002f;
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Pkg.Serv.readFromParcelLocked(android.os.Parcel):void");
                }

                public void writeToParcelLocked(android.os.Parcel r7) {
                    /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                    /*
                    r6 = this;
                    r1 = 1;
                    r2 = 0;
                    r4 = r6.mStartTime;
                    r7.writeLong(r4);
                    r4 = r6.mRunningSince;
                    r7.writeLong(r4);
                    r0 = r6.mRunning;
                    if (r0 == 0) goto L_0x004e;
                L_0x0010:
                    r0 = r1;
                L_0x0011:
                    r7.writeInt(r0);
                    r0 = r6.mStarts;
                    r7.writeInt(r0);
                    r4 = r6.mLaunchedTime;
                    r7.writeLong(r4);
                    r4 = r6.mLaunchedSince;
                    r7.writeLong(r4);
                    r0 = r6.mLaunched;
                    if (r0 == 0) goto L_0x0050;
                L_0x0027:
                    r7.writeInt(r1);
                    r0 = r6.mLaunches;
                    r7.writeInt(r0);
                    r0 = r6.mLoadedStartTime;
                    r7.writeLong(r0);
                    r0 = r6.mLoadedStarts;
                    r7.writeInt(r0);
                    r0 = r6.mLoadedLaunches;
                    r7.writeInt(r0);
                    r0 = r6.mUnpluggedStartTime;
                    r7.writeLong(r0);
                    r0 = r6.mUnpluggedStarts;
                    r7.writeInt(r0);
                    r0 = r6.mUnpluggedLaunches;
                    r7.writeInt(r0);
                    return;
                L_0x004e:
                    r0 = r2;
                    goto L_0x0011;
                L_0x0050:
                    r1 = r2;
                    goto L_0x0027;
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Pkg.Serv.writeToParcelLocked(android.os.Parcel):void");
                }

                public long getLaunchTimeToNowLocked(long r6) {
                    /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                    /*
                    r5 = this;
                    r0 = r5.mLaunched;
                    if (r0 != 0) goto L_0x0007;
                L_0x0004:
                    r0 = r5.mLaunchedTime;
                    return r0;
                L_0x0007:
                    r0 = r5.mLaunchedTime;
                    r0 = r0 + r6;
                    r2 = r5.mLaunchedSince;
                    r0 = r0 - r2;
                    return r0;
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Pkg.Serv.getLaunchTimeToNowLocked(long):long");
                }

                public long getStartTimeToNowLocked(long r6) {
                    /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                    /*
                    r5 = this;
                    r0 = r5.mRunning;
                    if (r0 != 0) goto L_0x0007;
                L_0x0004:
                    r0 = r5.mStartTime;
                    return r0;
                L_0x0007:
                    r0 = r5.mStartTime;
                    r0 = r0 + r6;
                    r2 = r5.mRunningSince;
                    r0 = r0 - r2;
                    return r0;
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Pkg.Serv.getStartTimeToNowLocked(long):long");
                }

                public void startLaunchedLocked() {
                    /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                    /*
                    r2 = this;
                    r0 = r2.mLaunched;
                    if (r0 != 0) goto L_0x0015;
                L_0x0004:
                    r0 = r2.mLaunches;
                    r0 = r0 + 1;
                    r2.mLaunches = r0;
                    r0 = r2.mBsi;
                    r0 = r0.getBatteryUptimeLocked();
                    r2.mLaunchedSince = r0;
                    r0 = 1;
                    r2.mLaunched = r0;
                L_0x0015:
                    return;
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Pkg.Serv.startLaunchedLocked():void");
                }

                public void stopLaunchedLocked() {
                    /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                    /*
                    r6 = this;
                    r2 = r6.mLaunched;
                    if (r2 == 0) goto L_0x001c;
                L_0x0004:
                    r2 = r6.mBsi;
                    r2 = r2.getBatteryUptimeLocked();
                    r4 = r6.mLaunchedSince;
                    r0 = r2 - r4;
                    r2 = 0;
                    r2 = (r0 > r2 ? 1 : (r0 == r2 ? 0 : -1));
                    if (r2 <= 0) goto L_0x001d;
                L_0x0014:
                    r2 = r6.mLaunchedTime;
                    r2 = r2 + r0;
                    r6.mLaunchedTime = r2;
                L_0x0019:
                    r2 = 0;
                    r6.mLaunched = r2;
                L_0x001c:
                    return;
                L_0x001d:
                    r2 = r6.mLaunches;
                    r2 = r2 + -1;
                    r6.mLaunches = r2;
                    goto L_0x0019;
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Pkg.Serv.stopLaunchedLocked():void");
                }

                public void startRunningLocked() {
                    /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                    /*
                    r2 = this;
                    r0 = r2.mRunning;
                    if (r0 != 0) goto L_0x0015;
                L_0x0004:
                    r0 = r2.mStarts;
                    r0 = r0 + 1;
                    r2.mStarts = r0;
                    r0 = r2.mBsi;
                    r0 = r0.getBatteryUptimeLocked();
                    r2.mRunningSince = r0;
                    r0 = 1;
                    r2.mRunning = r0;
                L_0x0015:
                    return;
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Pkg.Serv.startRunningLocked():void");
                }

                public void stopRunningLocked() {
                    /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                    /*
                    r6 = this;
                    r2 = r6.mRunning;
                    if (r2 == 0) goto L_0x001c;
                L_0x0004:
                    r2 = r6.mBsi;
                    r2 = r2.getBatteryUptimeLocked();
                    r4 = r6.mRunningSince;
                    r0 = r2 - r4;
                    r2 = 0;
                    r2 = (r0 > r2 ? 1 : (r0 == r2 ? 0 : -1));
                    if (r2 <= 0) goto L_0x001d;
                L_0x0014:
                    r2 = r6.mStartTime;
                    r2 = r2 + r0;
                    r6.mStartTime = r2;
                L_0x0019:
                    r2 = 0;
                    r6.mRunning = r2;
                L_0x001c:
                    return;
                L_0x001d:
                    r2 = r6.mStarts;
                    r2 = r2 + -1;
                    r6.mStarts = r2;
                    goto L_0x0019;
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Pkg.Serv.stopRunningLocked():void");
                }

                public com.android.internal.os.BatteryStatsImpl getBatteryStats() {
                    /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                    /*
                    r1 = this;
                    r0 = r1.mBsi;
                    return r0;
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Pkg.Serv.getBatteryStats():com.android.internal.os.BatteryStatsImpl");
                }

                public int getLaunches(int r3) {
                    /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                    /*
                    r2 = this;
                    r0 = r2.mLaunches;
                    r1 = 1;
                    if (r3 != r1) goto L_0x0009;
                L_0x0005:
                    r1 = r2.mLoadedLaunches;
                    r0 = r0 - r1;
                L_0x0008:
                    return r0;
                L_0x0009:
                    r1 = 2;
                    if (r3 != r1) goto L_0x0008;
                L_0x000c:
                    r1 = r2.mUnpluggedLaunches;
                    r0 = r0 - r1;
                    goto L_0x0008;
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Pkg.Serv.getLaunches(int):int");
                }

                public long getStartTime(long r6, int r8) {
                    /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                    /*
                    r5 = this;
                    r0 = r5.getStartTimeToNowLocked(r6);
                    r2 = 1;
                    if (r8 != r2) goto L_0x000b;
                L_0x0007:
                    r2 = r5.mLoadedStartTime;
                    r0 = r0 - r2;
                L_0x000a:
                    return r0;
                L_0x000b:
                    r2 = 2;
                    if (r8 != r2) goto L_0x000a;
                L_0x000e:
                    r2 = r5.mUnpluggedStartTime;
                    r0 = r0 - r2;
                    goto L_0x000a;
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Pkg.Serv.getStartTime(long, int):long");
                }

                public int getStarts(int r3) {
                    /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                    /*
                    r2 = this;
                    r0 = r2.mStarts;
                    r1 = 1;
                    if (r3 != r1) goto L_0x0009;
                L_0x0005:
                    r1 = r2.mLoadedStarts;
                    r0 = r0 - r1;
                L_0x0008:
                    return r0;
                L_0x0009:
                    r1 = 2;
                    if (r3 != r1) goto L_0x0008;
                L_0x000c:
                    r1 = r2.mUnpluggedStarts;
                    r0 = r0 - r1;
                    goto L_0x0008;
                    */
                    throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Pkg.Serv.getStarts(int):int");
                }
            }

            public Pkg(com.android.internal.os.BatteryStatsImpl r2) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r1 = this;
                r1.<init>();
                r0 = new android.util.ArrayMap;
                r0.<init>();
                r1.mWakeupAlarms = r0;
                r0 = new android.util.ArrayMap;
                r0.<init>();
                r1.mServiceStats = r0;
                r1.mBsi = r2;
                r0 = r1.mBsi;
                r0 = r0.mOnBatteryScreenOffTimeBase;
                r0.add(r1);
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Pkg.<init>(com.android.internal.os.BatteryStatsImpl):void");
            }

            public void onTimeStarted(long r1, long r3, long r5) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r0 = this;
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Pkg.onTimeStarted(long, long, long):void");
            }

            public void onTimeStopped(long r1, long r3, long r5) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r0 = this;
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Pkg.onTimeStopped(long, long, long):void");
            }

            void detach() {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r1 = this;
                r0 = r1.mBsi;
                r0 = r0.mOnBatteryScreenOffTimeBase;
                r0.remove(r1);
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Pkg.detach():void");
            }

            void readFromParcelLocked(android.os.Parcel r11) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r10 = this;
                r3 = r11.readInt();
                r7 = r10.mWakeupAlarms;
                r7.clear();
                r0 = 0;
            L_0x000a:
                if (r0 >= r3) goto L_0x0021;
            L_0x000c:
                r6 = r11.readString();
                r7 = r10.mWakeupAlarms;
                r8 = new com.android.internal.os.BatteryStatsImpl$Counter;
                r9 = r10.mBsi;
                r9 = r9.mOnBatteryScreenOffTimeBase;
                r8.<init>(r9, r11);
                r7.put(r6, r8);
                r0 = r0 + 1;
                goto L_0x000a;
            L_0x0021:
                r2 = r11.readInt();
                r7 = r10.mServiceStats;
                r7.clear();
                r1 = 0;
            L_0x002b:
                if (r1 >= r2) goto L_0x0043;
            L_0x002d:
                r5 = r11.readString();
                r4 = new com.android.internal.os.BatteryStatsImpl$Uid$Pkg$Serv;
                r7 = r10.mBsi;
                r4.<init>(r7);
                r7 = r10.mServiceStats;
                r7.put(r5, r4);
                r4.readFromParcelLocked(r11);
                r1 = r1 + 1;
                goto L_0x002b;
            L_0x0043:
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Pkg.readFromParcelLocked(android.os.Parcel):void");
            }

            void writeToParcelLocked(android.os.Parcel r6) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r5 = this;
                r4 = r5.mWakeupAlarms;
                r2 = r4.size();
                r6.writeInt(r2);
                r1 = 0;
            L_0x000a:
                if (r1 >= r2) goto L_0x0025;
            L_0x000c:
                r4 = r5.mWakeupAlarms;
                r4 = r4.keyAt(r1);
                r4 = (java.lang.String) r4;
                r6.writeString(r4);
                r4 = r5.mWakeupAlarms;
                r4 = r4.valueAt(r1);
                r4 = (com.android.internal.os.BatteryStatsImpl.Counter) r4;
                r4.writeToParcel(r6);
                r1 = r1 + 1;
                goto L_0x000a;
            L_0x0025:
                r4 = r5.mServiceStats;
                r0 = r4.size();
                r6.writeInt(r0);
                r1 = 0;
            L_0x002f:
                if (r1 >= r0) goto L_0x004a;
            L_0x0031:
                r4 = r5.mServiceStats;
                r4 = r4.keyAt(r1);
                r4 = (java.lang.String) r4;
                r6.writeString(r4);
                r4 = r5.mServiceStats;
                r3 = r4.valueAt(r1);
                r3 = (com.android.internal.os.BatteryStatsImpl.Uid.Pkg.Serv) r3;
                r3.writeToParcelLocked(r6);
                r1 = r1 + 1;
                goto L_0x002f;
            L_0x004a:
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Pkg.writeToParcelLocked(android.os.Parcel):void");
            }

            public android.util.ArrayMap<java.lang.String, ? extends android.os.BatteryStats.Counter> getWakeupAlarmStats() {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r1 = this;
                r0 = r1.mWakeupAlarms;
                return r0;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Pkg.getWakeupAlarmStats():android.util.ArrayMap<java.lang.String, ? extends android.os.BatteryStats$Counter>");
            }

            public void noteWakeupAlarmLocked(java.lang.String r3) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r2 = this;
                r1 = r2.mWakeupAlarms;
                r0 = r1.get(r3);
                r0 = (com.android.internal.os.BatteryStatsImpl.Counter) r0;
                if (r0 != 0) goto L_0x0018;
            L_0x000a:
                r0 = new com.android.internal.os.BatteryStatsImpl$Counter;
                r1 = r2.mBsi;
                r1 = r1.mOnBatteryScreenOffTimeBase;
                r0.<init>(r1);
                r1 = r2.mWakeupAlarms;
                r1.put(r3, r0);
            L_0x0018:
                r0.stepAtomic();
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Pkg.noteWakeupAlarmLocked(java.lang.String):void");
            }

            public android.util.ArrayMap<java.lang.String, ? extends android.os.BatteryStats.Uid.Pkg.Serv> getServiceStats() {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r1 = this;
                r0 = r1.mServiceStats;
                return r0;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Pkg.getServiceStats():android.util.ArrayMap<java.lang.String, ? extends android.os.BatteryStats$Uid$Pkg$Serv>");
            }

            final com.android.internal.os.BatteryStatsImpl.Uid.Pkg.Serv newServiceStatsLocked() {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r2 = this;
                r0 = new com.android.internal.os.BatteryStatsImpl$Uid$Pkg$Serv;
                r1 = r2.mBsi;
                r0.<init>(r1);
                return r0;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Pkg.newServiceStatsLocked():com.android.internal.os.BatteryStatsImpl$Uid$Pkg$Serv");
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

            public Proc(com.android.internal.os.BatteryStatsImpl r2, java.lang.String r3) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r1 = this;
                r1.<init>();
                r0 = 1;
                r1.mActive = r0;
                r1.mBsi = r2;
                r1.mName = r3;
                r0 = r1.mBsi;
                r0 = r0.mOnBatteryTimeBase;
                r0.add(r1);
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Proc.<init>(com.android.internal.os.BatteryStatsImpl, java.lang.String):void");
            }

            public void onTimeStarted(long r3, long r5, long r7) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r2 = this;
                r0 = r2.mUserTime;
                r2.mUnpluggedUserTime = r0;
                r0 = r2.mSystemTime;
                r2.mUnpluggedSystemTime = r0;
                r0 = r2.mForegroundTime;
                r2.mUnpluggedForegroundTime = r0;
                r0 = r2.mStarts;
                r2.mUnpluggedStarts = r0;
                r0 = r2.mNumCrashes;
                r2.mUnpluggedNumCrashes = r0;
                r0 = r2.mNumAnrs;
                r2.mUnpluggedNumAnrs = r0;
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Proc.onTimeStarted(long, long, long):void");
            }

            public void onTimeStopped(long r1, long r3, long r5) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r0 = this;
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Proc.onTimeStopped(long, long, long):void");
            }

            void detach() {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r1 = this;
                r0 = 0;
                r1.mActive = r0;
                r0 = r1.mBsi;
                r0 = r0.mOnBatteryTimeBase;
                r0.remove(r1);
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Proc.detach():void");
            }

            public int countExcessivePowers() {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r1 = this;
                r0 = r1.mExcessivePower;
                if (r0 == 0) goto L_0x000b;
            L_0x0004:
                r0 = r1.mExcessivePower;
                r0 = r0.size();
            L_0x000a:
                return r0;
            L_0x000b:
                r0 = 0;
                goto L_0x000a;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Proc.countExcessivePowers():int");
            }

            public android.os.BatteryStats.Uid.Proc.ExcessivePower getExcessivePower(int r3) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r2 = this;
                r1 = 0;
                r0 = r2.mExcessivePower;
                if (r0 == 0) goto L_0x000e;
            L_0x0005:
                r0 = r2.mExcessivePower;
                r0 = r0.get(r3);
                r0 = (android.os.BatteryStats.Uid.Proc.ExcessivePower) r0;
                return r0;
            L_0x000e:
                return r1;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Proc.getExcessivePower(int):android.os.BatteryStats$Uid$Proc$ExcessivePower");
            }

            public void addExcessiveCpu(long r4, long r6) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r3 = this;
                r1 = r3.mExcessivePower;
                if (r1 != 0) goto L_0x000b;
            L_0x0004:
                r1 = new java.util.ArrayList;
                r1.<init>();
                r3.mExcessivePower = r1;
            L_0x000b:
                r0 = new android.os.BatteryStats$Uid$Proc$ExcessivePower;
                r0.<init>();
                r1 = 2;
                r0.type = r1;
                r0.overTime = r4;
                r0.usedTime = r6;
                r1 = r3.mExcessivePower;
                r1.add(r0);
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Proc.addExcessiveCpu(long, long):void");
            }

            void writeExcessivePowerToParcelLocked(android.os.Parcel r7) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r6 = this;
                r3 = r6.mExcessivePower;
                if (r3 != 0) goto L_0x0009;
            L_0x0004:
                r3 = 0;
                r7.writeInt(r3);
                return;
            L_0x0009:
                r3 = r6.mExcessivePower;
                r0 = r3.size();
                r7.writeInt(r0);
                r2 = 0;
            L_0x0013:
                if (r2 >= r0) goto L_0x002f;
            L_0x0015:
                r3 = r6.mExcessivePower;
                r1 = r3.get(r2);
                r1 = (android.os.BatteryStats.Uid.Proc.ExcessivePower) r1;
                r3 = r1.type;
                r7.writeInt(r3);
                r4 = r1.overTime;
                r7.writeLong(r4);
                r4 = r1.usedTime;
                r7.writeLong(r4);
                r2 = r2 + 1;
                goto L_0x0013;
            L_0x002f:
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Proc.writeExcessivePowerToParcelLocked(android.os.Parcel):void");
            }

            void readExcessivePowerFromParcelLocked(android.os.Parcel r7) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r6 = this;
                r0 = r7.readInt();
                if (r0 != 0) goto L_0x000a;
            L_0x0006:
                r3 = 0;
                r6.mExcessivePower = r3;
                return;
            L_0x000a:
                r3 = 10000; // 0x2710 float:1.4013E-41 double:4.9407E-320;
                if (r0 <= r3) goto L_0x0028;
            L_0x000e:
                r3 = new android.os.ParcelFormatException;
                r4 = new java.lang.StringBuilder;
                r4.<init>();
                r5 = "File corrupt: too many excessive power entries ";
                r4 = r4.append(r5);
                r4 = r4.append(r0);
                r4 = r4.toString();
                r3.<init>(r4);
                throw r3;
            L_0x0028:
                r3 = new java.util.ArrayList;
                r3.<init>();
                r6.mExcessivePower = r3;
                r2 = 0;
            L_0x0030:
                if (r2 >= r0) goto L_0x0051;
            L_0x0032:
                r1 = new android.os.BatteryStats$Uid$Proc$ExcessivePower;
                r1.<init>();
                r3 = r7.readInt();
                r1.type = r3;
                r4 = r7.readLong();
                r1.overTime = r4;
                r4 = r7.readLong();
                r1.usedTime = r4;
                r3 = r6.mExcessivePower;
                r3.add(r1);
                r2 = r2 + 1;
                goto L_0x0030;
            L_0x0051:
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Proc.readExcessivePowerFromParcelLocked(android.os.Parcel):void");
            }

            void writeToParcelLocked(android.os.Parcel r3) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r2 = this;
                r0 = r2.mUserTime;
                r3.writeLong(r0);
                r0 = r2.mSystemTime;
                r3.writeLong(r0);
                r0 = r2.mForegroundTime;
                r3.writeLong(r0);
                r0 = r2.mStarts;
                r3.writeInt(r0);
                r0 = r2.mNumCrashes;
                r3.writeInt(r0);
                r0 = r2.mNumAnrs;
                r3.writeInt(r0);
                r0 = r2.mLoadedUserTime;
                r3.writeLong(r0);
                r0 = r2.mLoadedSystemTime;
                r3.writeLong(r0);
                r0 = r2.mLoadedForegroundTime;
                r3.writeLong(r0);
                r0 = r2.mLoadedStarts;
                r3.writeInt(r0);
                r0 = r2.mLoadedNumCrashes;
                r3.writeInt(r0);
                r0 = r2.mLoadedNumAnrs;
                r3.writeInt(r0);
                r0 = r2.mUnpluggedUserTime;
                r3.writeLong(r0);
                r0 = r2.mUnpluggedSystemTime;
                r3.writeLong(r0);
                r0 = r2.mUnpluggedForegroundTime;
                r3.writeLong(r0);
                r0 = r2.mUnpluggedStarts;
                r3.writeInt(r0);
                r0 = r2.mUnpluggedNumCrashes;
                r3.writeInt(r0);
                r0 = r2.mUnpluggedNumAnrs;
                r3.writeInt(r0);
                r2.writeExcessivePowerToParcelLocked(r3);
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Proc.writeToParcelLocked(android.os.Parcel):void");
            }

            void readFromParcelLocked(android.os.Parcel r3) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r2 = this;
                r0 = r3.readLong();
                r2.mUserTime = r0;
                r0 = r3.readLong();
                r2.mSystemTime = r0;
                r0 = r3.readLong();
                r2.mForegroundTime = r0;
                r0 = r3.readInt();
                r2.mStarts = r0;
                r0 = r3.readInt();
                r2.mNumCrashes = r0;
                r0 = r3.readInt();
                r2.mNumAnrs = r0;
                r0 = r3.readLong();
                r2.mLoadedUserTime = r0;
                r0 = r3.readLong();
                r2.mLoadedSystemTime = r0;
                r0 = r3.readLong();
                r2.mLoadedForegroundTime = r0;
                r0 = r3.readInt();
                r2.mLoadedStarts = r0;
                r0 = r3.readInt();
                r2.mLoadedNumCrashes = r0;
                r0 = r3.readInt();
                r2.mLoadedNumAnrs = r0;
                r0 = r3.readLong();
                r2.mUnpluggedUserTime = r0;
                r0 = r3.readLong();
                r2.mUnpluggedSystemTime = r0;
                r0 = r3.readLong();
                r2.mUnpluggedForegroundTime = r0;
                r0 = r3.readInt();
                r2.mUnpluggedStarts = r0;
                r0 = r3.readInt();
                r2.mUnpluggedNumCrashes = r0;
                r0 = r3.readInt();
                r2.mUnpluggedNumAnrs = r0;
                r2.readExcessivePowerFromParcelLocked(r3);
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Proc.readFromParcelLocked(android.os.Parcel):void");
            }

            public void addCpuTimeLocked(int r5, int r6) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r4 = this;
                r0 = r4.mUserTime;
                r2 = (long) r5;
                r0 = r0 + r2;
                r4.mUserTime = r0;
                r0 = r4.mSystemTime;
                r2 = (long) r6;
                r0 = r0 + r2;
                r4.mSystemTime = r0;
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Proc.addCpuTimeLocked(int, int):void");
            }

            public void addForegroundTimeLocked(long r4) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r3 = this;
                r0 = r3.mForegroundTime;
                r0 = r0 + r4;
                r3.mForegroundTime = r0;
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Proc.addForegroundTimeLocked(long):void");
            }

            public void incStartsLocked() {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r1 = this;
                r0 = r1.mStarts;
                r0 = r0 + 1;
                r1.mStarts = r0;
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Proc.incStartsLocked():void");
            }

            public void incNumCrashesLocked() {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r1 = this;
                r0 = r1.mNumCrashes;
                r0 = r0 + 1;
                r1.mNumCrashes = r0;
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Proc.incNumCrashesLocked():void");
            }

            public void incNumAnrsLocked() {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r1 = this;
                r0 = r1.mNumAnrs;
                r0 = r0 + 1;
                r1.mNumAnrs = r0;
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Proc.incNumAnrsLocked():void");
            }

            public boolean isActive() {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r1 = this;
                r0 = r1.mActive;
                return r0;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Proc.isActive():boolean");
            }

            public long getUserTime(int r5) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r4 = this;
                r0 = r4.mUserTime;
                r2 = 1;
                if (r5 != r2) goto L_0x0009;
            L_0x0005:
                r2 = r4.mLoadedUserTime;
                r0 = r0 - r2;
            L_0x0008:
                return r0;
            L_0x0009:
                r2 = 2;
                if (r5 != r2) goto L_0x0008;
            L_0x000c:
                r2 = r4.mUnpluggedUserTime;
                r0 = r0 - r2;
                goto L_0x0008;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Proc.getUserTime(int):long");
            }

            public long getSystemTime(int r5) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r4 = this;
                r0 = r4.mSystemTime;
                r2 = 1;
                if (r5 != r2) goto L_0x0009;
            L_0x0005:
                r2 = r4.mLoadedSystemTime;
                r0 = r0 - r2;
            L_0x0008:
                return r0;
            L_0x0009:
                r2 = 2;
                if (r5 != r2) goto L_0x0008;
            L_0x000c:
                r2 = r4.mUnpluggedSystemTime;
                r0 = r0 - r2;
                goto L_0x0008;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Proc.getSystemTime(int):long");
            }

            public long getForegroundTime(int r5) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r4 = this;
                r0 = r4.mForegroundTime;
                r2 = 1;
                if (r5 != r2) goto L_0x0009;
            L_0x0005:
                r2 = r4.mLoadedForegroundTime;
                r0 = r0 - r2;
            L_0x0008:
                return r0;
            L_0x0009:
                r2 = 2;
                if (r5 != r2) goto L_0x0008;
            L_0x000c:
                r2 = r4.mUnpluggedForegroundTime;
                r0 = r0 - r2;
                goto L_0x0008;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Proc.getForegroundTime(int):long");
            }

            public int getStarts(int r3) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r2 = this;
                r0 = r2.mStarts;
                r1 = 1;
                if (r3 != r1) goto L_0x0009;
            L_0x0005:
                r1 = r2.mLoadedStarts;
                r0 = r0 - r1;
            L_0x0008:
                return r0;
            L_0x0009:
                r1 = 2;
                if (r3 != r1) goto L_0x0008;
            L_0x000c:
                r1 = r2.mUnpluggedStarts;
                r0 = r0 - r1;
                goto L_0x0008;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Proc.getStarts(int):int");
            }

            public int getNumCrashes(int r3) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r2 = this;
                r0 = r2.mNumCrashes;
                r1 = 1;
                if (r3 != r1) goto L_0x0009;
            L_0x0005:
                r1 = r2.mLoadedNumCrashes;
                r0 = r0 - r1;
            L_0x0008:
                return r0;
            L_0x0009:
                r1 = 2;
                if (r3 != r1) goto L_0x0008;
            L_0x000c:
                r1 = r2.mUnpluggedNumCrashes;
                r0 = r0 - r1;
                goto L_0x0008;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Proc.getNumCrashes(int):int");
            }

            public int getNumAnrs(int r3) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r2 = this;
                r0 = r2.mNumAnrs;
                r1 = 1;
                if (r3 != r1) goto L_0x0009;
            L_0x0005:
                r1 = r2.mLoadedNumAnrs;
                r0 = r0 - r1;
            L_0x0008:
                return r0;
            L_0x0009:
                r1 = 2;
                if (r3 != r1) goto L_0x0008;
            L_0x000c:
                r1 = r2.mUnpluggedNumAnrs;
                r0 = r0 - r1;
                goto L_0x0008;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Proc.getNumAnrs(int):int");
            }
        }

        public static class Sensor extends android.os.BatteryStats.Uid.Sensor {
            protected BatteryStatsImpl mBsi;
            final int mHandle;
            DualTimer mTimer;
            protected Uid mUid;

            public Sensor(com.android.internal.os.BatteryStatsImpl r1, com.android.internal.os.BatteryStatsImpl.Uid r2, int r3) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r0 = this;
                r0.<init>();
                r0.mBsi = r1;
                r0.mUid = r2;
                r0.mHandle = r3;
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Sensor.<init>(com.android.internal.os.BatteryStatsImpl, com.android.internal.os.BatteryStatsImpl$Uid, int):void");
            }

            private com.android.internal.os.BatteryStatsImpl.DualTimer readTimersFromParcel(com.android.internal.os.BatteryStatsImpl.TimeBase r9, com.android.internal.os.BatteryStatsImpl.TimeBase r10, android.os.Parcel r11) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r8 = this;
                r1 = 0;
                r3 = 0;
                r0 = r11.readInt();
                if (r0 != 0) goto L_0x0009;
            L_0x0008:
                return r1;
            L_0x0009:
                r0 = r8.mBsi;
                r0 = r0.mSensorTimers;
                r1 = r8.mHandle;
                r4 = r0.get(r1);
                r4 = (java.util.ArrayList) r4;
                if (r4 != 0) goto L_0x0025;
            L_0x0017:
                r4 = new java.util.ArrayList;
                r4.<init>();
                r0 = r8.mBsi;
                r0 = r0.mSensorTimers;
                r1 = r8.mHandle;
                r0.put(r1, r4);
            L_0x0025:
                r0 = new com.android.internal.os.BatteryStatsImpl$DualTimer;
                r1 = r8.mBsi;
                r1 = r1.mClocks;
                r2 = r8.mUid;
                r5 = r9;
                r6 = r10;
                r7 = r11;
                r0.<init>(r1, r2, r3, r4, r5, r6, r7);
                return r0;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Sensor.readTimersFromParcel(com.android.internal.os.BatteryStatsImpl$TimeBase, com.android.internal.os.BatteryStatsImpl$TimeBase, android.os.Parcel):com.android.internal.os.BatteryStatsImpl$DualTimer");
            }

            boolean reset() {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r2 = this;
                r1 = 1;
                r0 = r2.mTimer;
                r0 = r0.reset(r1);
                if (r0 == 0) goto L_0x000d;
            L_0x0009:
                r0 = 0;
                r2.mTimer = r0;
                return r1;
            L_0x000d:
                r0 = 0;
                return r0;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Sensor.reset():boolean");
            }

            void readFromParcelLocked(com.android.internal.os.BatteryStatsImpl.TimeBase r2, com.android.internal.os.BatteryStatsImpl.TimeBase r3, android.os.Parcel r4) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r1 = this;
                r0 = r1.readTimersFromParcel(r2, r3, r4);
                r1.mTimer = r0;
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Sensor.readFromParcelLocked(com.android.internal.os.BatteryStatsImpl$TimeBase, com.android.internal.os.BatteryStatsImpl$TimeBase, android.os.Parcel):void");
            }

            void writeToParcelLocked(android.os.Parcel r3, long r4) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r2 = this;
                r0 = r2.mTimer;
                com.android.internal.os.BatteryStatsImpl.Timer.writeTimerToParcel(r3, r0, r4);
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Sensor.writeToParcelLocked(android.os.Parcel, long):void");
            }

            public com.android.internal.os.BatteryStatsImpl.Timer getSensorTime() {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r1 = this;
                r0 = r1.mTimer;
                return r0;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Sensor.getSensorTime():com.android.internal.os.BatteryStatsImpl$Timer");
            }

            public com.android.internal.os.BatteryStatsImpl.Timer getSensorBackgroundTime() {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r2 = this;
                r1 = 0;
                r0 = r2.mTimer;
                if (r0 != 0) goto L_0x0006;
            L_0x0005:
                return r1;
            L_0x0006:
                r0 = r2.mTimer;
                r0 = r0.getSubTimer();
                return r0;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Sensor.getSensorBackgroundTime():com.android.internal.os.BatteryStatsImpl$Timer");
            }

            public int getHandle() {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r1 = this;
                r0 = r1.mHandle;
                return r0;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Sensor.getHandle():int");
            }
        }

        public static class Wakelock extends android.os.BatteryStats.Uid.Wakelock {
            protected BatteryStatsImpl mBsi;
            StopwatchTimer mTimerDraw;
            StopwatchTimer mTimerFull;
            DualTimer mTimerPartial;
            StopwatchTimer mTimerWindow;
            protected Uid mUid;

            public Wakelock(com.android.internal.os.BatteryStatsImpl r1, com.android.internal.os.BatteryStatsImpl.Uid r2) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r0 = this;
                r0.<init>();
                r0.mBsi = r1;
                r0.mUid = r2;
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Wakelock.<init>(com.android.internal.os.BatteryStatsImpl, com.android.internal.os.BatteryStatsImpl$Uid):void");
            }

            private com.android.internal.os.BatteryStatsImpl.StopwatchTimer readStopwatchTimerFromParcel(int r8, java.util.ArrayList<com.android.internal.os.BatteryStatsImpl.StopwatchTimer> r9, com.android.internal.os.BatteryStatsImpl.TimeBase r10, android.os.Parcel r11) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r7 = this;
                r0 = r11.readInt();
                if (r0 != 0) goto L_0x0008;
            L_0x0006:
                r0 = 0;
                return r0;
            L_0x0008:
                r0 = new com.android.internal.os.BatteryStatsImpl$StopwatchTimer;
                r1 = r7.mBsi;
                r1 = r1.mClocks;
                r2 = r7.mUid;
                r3 = r8;
                r4 = r9;
                r5 = r10;
                r6 = r11;
                r0.<init>(r1, r2, r3, r4, r5, r6);
                return r0;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Wakelock.readStopwatchTimerFromParcel(int, java.util.ArrayList, com.android.internal.os.BatteryStatsImpl$TimeBase, android.os.Parcel):com.android.internal.os.BatteryStatsImpl$StopwatchTimer");
            }

            private com.android.internal.os.BatteryStatsImpl.DualTimer readDualTimerFromParcel(int r9, java.util.ArrayList<com.android.internal.os.BatteryStatsImpl.StopwatchTimer> r10, com.android.internal.os.BatteryStatsImpl.TimeBase r11, com.android.internal.os.BatteryStatsImpl.TimeBase r12, android.os.Parcel r13) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r8 = this;
                r0 = r13.readInt();
                if (r0 != 0) goto L_0x0008;
            L_0x0006:
                r0 = 0;
                return r0;
            L_0x0008:
                r0 = new com.android.internal.os.BatteryStatsImpl$DualTimer;
                r1 = r8.mBsi;
                r1 = r1.mClocks;
                r2 = r8.mUid;
                r3 = r9;
                r4 = r10;
                r5 = r11;
                r6 = r12;
                r7 = r13;
                r0.<init>(r1, r2, r3, r4, r5, r6, r7);
                return r0;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Wakelock.readDualTimerFromParcel(int, java.util.ArrayList, com.android.internal.os.BatteryStatsImpl$TimeBase, com.android.internal.os.BatteryStatsImpl$TimeBase, android.os.Parcel):com.android.internal.os.BatteryStatsImpl$DualTimer");
            }

            boolean reset() {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r4 = this;
                r3 = 0;
                r2 = 0;
                r0 = 0;
                r1 = r4.mTimerFull;
                if (r1 == 0) goto L_0x000f;
            L_0x0007:
                r1 = r4.mTimerFull;
                r1 = r1.reset(r3);
                r0 = r1 ^ 1;
            L_0x000f:
                r1 = r4.mTimerPartial;
                if (r1 == 0) goto L_0x001c;
            L_0x0013:
                r1 = r4.mTimerPartial;
                r1 = r1.reset(r3);
                r1 = r1 ^ 1;
                r0 = r0 | r1;
            L_0x001c:
                r1 = r4.mTimerWindow;
                if (r1 == 0) goto L_0x0029;
            L_0x0020:
                r1 = r4.mTimerWindow;
                r1 = r1.reset(r3);
                r1 = r1 ^ 1;
                r0 = r0 | r1;
            L_0x0029:
                r1 = r4.mTimerDraw;
                if (r1 == 0) goto L_0x0036;
            L_0x002d:
                r1 = r4.mTimerDraw;
                r1 = r1.reset(r3);
                r1 = r1 ^ 1;
                r0 = r0 | r1;
            L_0x0036:
                if (r0 != 0) goto L_0x0064;
            L_0x0038:
                r1 = r4.mTimerFull;
                if (r1 == 0) goto L_0x0043;
            L_0x003c:
                r1 = r4.mTimerFull;
                r1.detach();
                r4.mTimerFull = r2;
            L_0x0043:
                r1 = r4.mTimerPartial;
                if (r1 == 0) goto L_0x004e;
            L_0x0047:
                r1 = r4.mTimerPartial;
                r1.detach();
                r4.mTimerPartial = r2;
            L_0x004e:
                r1 = r4.mTimerWindow;
                if (r1 == 0) goto L_0x0059;
            L_0x0052:
                r1 = r4.mTimerWindow;
                r1.detach();
                r4.mTimerWindow = r2;
            L_0x0059:
                r1 = r4.mTimerDraw;
                if (r1 == 0) goto L_0x0064;
            L_0x005d:
                r1 = r4.mTimerDraw;
                r1.detach();
                r4.mTimerDraw = r2;
            L_0x0064:
                r1 = r0 ^ 1;
                return r1;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Wakelock.reset():boolean");
            }

            void readFromParcelLocked(com.android.internal.os.BatteryStatsImpl.TimeBase r7, com.android.internal.os.BatteryStatsImpl.TimeBase r8, com.android.internal.os.BatteryStatsImpl.TimeBase r9, android.os.Parcel r10) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r6 = this;
                r0 = r6.mBsi;
                r2 = r0.mPartialTimers;
                r1 = 0;
                r0 = r6;
                r3 = r8;
                r4 = r9;
                r5 = r10;
                r0 = r0.readDualTimerFromParcel(r1, r2, r3, r4, r5);
                r6.mTimerPartial = r0;
                r0 = r6.mBsi;
                r0 = r0.mFullTimers;
                r1 = 1;
                r0 = r6.readStopwatchTimerFromParcel(r1, r0, r7, r10);
                r6.mTimerFull = r0;
                r0 = r6.mBsi;
                r0 = r0.mWindowTimers;
                r1 = 2;
                r0 = r6.readStopwatchTimerFromParcel(r1, r0, r7, r10);
                r6.mTimerWindow = r0;
                r0 = r6.mBsi;
                r0 = r0.mDrawTimers;
                r1 = 18;
                r0 = r6.readStopwatchTimerFromParcel(r1, r0, r7, r10);
                r6.mTimerDraw = r0;
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Wakelock.readFromParcelLocked(com.android.internal.os.BatteryStatsImpl$TimeBase, com.android.internal.os.BatteryStatsImpl$TimeBase, com.android.internal.os.BatteryStatsImpl$TimeBase, android.os.Parcel):void");
            }

            void writeToParcelLocked(android.os.Parcel r3, long r4) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r2 = this;
                r0 = r2.mTimerPartial;
                com.android.internal.os.BatteryStatsImpl.Timer.writeTimerToParcel(r3, r0, r4);
                r0 = r2.mTimerFull;
                com.android.internal.os.BatteryStatsImpl.Timer.writeTimerToParcel(r3, r0, r4);
                r0 = r2.mTimerWindow;
                com.android.internal.os.BatteryStatsImpl.Timer.writeTimerToParcel(r3, r0, r4);
                r0 = r2.mTimerDraw;
                com.android.internal.os.BatteryStatsImpl.Timer.writeTimerToParcel(r3, r0, r4);
                return;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Wakelock.writeToParcelLocked(android.os.Parcel, long):void");
            }

            public com.android.internal.os.BatteryStatsImpl.Timer getWakeTime(int r4) {
                /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
                /*
                r3 = this;
                switch(r4) {
                    case 0: goto L_0x0020;
                    case 1: goto L_0x001d;
                    case 2: goto L_0x0023;
                    case 18: goto L_0x0026;
                    default: goto L_0x0003;
                };
            L_0x0003:
                r0 = new java.lang.IllegalArgumentException;
                r1 = new java.lang.StringBuilder;
                r1.<init>();
                r2 = "type = ";
                r1 = r1.append(r2);
                r1 = r1.append(r4);
                r1 = r1.toString();
                r0.<init>(r1);
                throw r0;
            L_0x001d:
                r0 = r3.mTimerFull;
                return r0;
            L_0x0020:
                r0 = r3.mTimerPartial;
                return r0;
            L_0x0023:
                r0 = r3.mTimerWindow;
                return r0;
            L_0x0026:
                r0 = r3.mTimerDraw;
                return r0;
                */
                throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.Wakelock.getWakeTime(int):com.android.internal.os.BatteryStatsImpl$Timer");
            }
        }

        public Uid(com.android.internal.os.BatteryStatsImpl r11, int r12) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r10 = this;
            r9 = 6;
            r8 = 5;
            r6 = 1000; // 0x3e8 float:1.401E-42 double:4.94E-321;
            r10.<init>();
            r0 = -1;
            r10.mWifiBatchedScanBinStarted = r0;
            r0 = 18;
            r10.mProcessState = r0;
            r0 = 0;
            r10.mInForegroundService = r0;
            r0 = new android.util.ArrayMap;
            r0.<init>();
            r10.mJobCompletions = r0;
            r0 = new android.util.SparseArray;
            r0.<init>();
            r10.mSensorStats = r0;
            r0 = new android.util.ArrayMap;
            r0.<init>();
            r10.mProcessStats = r0;
            r0 = new android.util.ArrayMap;
            r0.<init>();
            r10.mPackageStats = r0;
            r0 = new android.util.SparseArray;
            r0.<init>();
            r10.mPids = r0;
            r10.mBsi = r11;
            r10.mUid = r12;
            r0 = new com.android.internal.os.BatteryStatsImpl$TimeBase;
            r0.<init>();
            r10.mOnBatteryBackgroundTimeBase = r0;
            r0 = r10.mOnBatteryBackgroundTimeBase;
            r1 = r10.mBsi;
            r1 = r1.mClocks;
            r2 = r1.uptimeMillis();
            r2 = r2 * r6;
            r1 = r10.mBsi;
            r1 = r1.mClocks;
            r4 = r1.elapsedRealtime();
            r4 = r4 * r6;
            r0.init(r2, r4);
            r0 = new com.android.internal.os.BatteryStatsImpl$TimeBase;
            r0.<init>();
            r10.mOnBatteryScreenOffBackgroundTimeBase = r0;
            r0 = r10.mOnBatteryScreenOffBackgroundTimeBase;
            r1 = r10.mBsi;
            r1 = r1.mClocks;
            r2 = r1.uptimeMillis();
            r2 = r2 * r6;
            r1 = r10.mBsi;
            r1 = r1.mClocks;
            r4 = r1.elapsedRealtime();
            r4 = r4 * r6;
            r0.init(r2, r4);
            r0 = new com.android.internal.os.BatteryStatsImpl$LongSamplingCounter;
            r1 = r10.mBsi;
            r1 = r1.mOnBatteryTimeBase;
            r0.<init>(r1);
            r10.mUserCpuTime = r0;
            r0 = new com.android.internal.os.BatteryStatsImpl$LongSamplingCounter;
            r1 = r10.mBsi;
            r1 = r1.mOnBatteryTimeBase;
            r0.<init>(r1);
            r10.mSystemCpuTime = r0;
            r0 = new com.android.internal.os.BatteryStatsImpl$Uid$1;
            r1 = r10.mBsi;
            r1.getClass();
            r0.<init>(r1, r12);
            r10.mWakelockStats = r0;
            r0 = new com.android.internal.os.BatteryStatsImpl$Uid$2;
            r1 = r10.mBsi;
            r1.getClass();
            r0.<init>(r1, r12);
            r10.mSyncStats = r0;
            r0 = new com.android.internal.os.BatteryStatsImpl$Uid$3;
            r1 = r10.mBsi;
            r1.getClass();
            r0.<init>(r1, r12);
            r10.mJobStats = r0;
            r0 = new com.android.internal.os.BatteryStatsImpl$StopwatchTimer;
            r1 = r10.mBsi;
            r1 = r1.mClocks;
            r2 = r10.mBsi;
            r4 = r2.mWifiRunningTimers;
            r2 = r10.mBsi;
            r5 = r2.mOnBatteryTimeBase;
            r3 = 4;
            r2 = r10;
            r0.<init>(r1, r2, r3, r4, r5);
            r10.mWifiRunningTimer = r0;
            r0 = new com.android.internal.os.BatteryStatsImpl$StopwatchTimer;
            r1 = r10.mBsi;
            r1 = r1.mClocks;
            r2 = r10.mBsi;
            r4 = r2.mFullWifiLockTimers;
            r2 = r10.mBsi;
            r5 = r2.mOnBatteryTimeBase;
            r2 = r10;
            r3 = r8;
            r0.<init>(r1, r2, r3, r4, r5);
            r10.mFullWifiLockTimer = r0;
            r0 = new com.android.internal.os.BatteryStatsImpl$DualTimer;
            r1 = r10.mBsi;
            r1 = r1.mClocks;
            r2 = r10.mBsi;
            r4 = r2.mWifiScanTimers;
            r2 = r10.mBsi;
            r5 = r2.mOnBatteryTimeBase;
            r6 = r10.mOnBatteryBackgroundTimeBase;
            r2 = r10;
            r3 = r9;
            r0.<init>(r1, r2, r3, r4, r5, r6);
            r10.mWifiScanTimer = r0;
            r0 = new com.android.internal.os.BatteryStatsImpl.StopwatchTimer[r8];
            r10.mWifiBatchedScanTimer = r0;
            r0 = new com.android.internal.os.BatteryStatsImpl$StopwatchTimer;
            r1 = r10.mBsi;
            r1 = r1.mClocks;
            r2 = r10.mBsi;
            r4 = r2.mWifiMulticastTimers;
            r2 = r10.mBsi;
            r5 = r2.mOnBatteryTimeBase;
            r3 = 7;
            r2 = r10;
            r0.<init>(r1, r2, r3, r4, r5);
            r10.mWifiMulticastTimer = r0;
            r0 = new com.android.internal.os.BatteryStatsImpl.StopwatchTimer[r9];
            r10.mProcessStateTimer = r0;
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.<init>(com.android.internal.os.BatteryStatsImpl, int):void");
        }

        public long[] getCpuFreqTimes(int r8) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r7 = this;
            r6 = 0;
            r2 = r7.mCpuFreqTimeMs;
            if (r2 != 0) goto L_0x0006;
        L_0x0005:
            return r6;
        L_0x0006:
            r2 = r7.mCpuFreqTimeMs;
            r0 = r2.getCountsLocked(r8);
            if (r0 != 0) goto L_0x000f;
        L_0x000e:
            return r6;
        L_0x000f:
            r1 = 0;
        L_0x0010:
            r2 = r0.length;
            if (r1 >= r2) goto L_0x001f;
        L_0x0013:
            r2 = r0[r1];
            r4 = 0;
            r2 = (r2 > r4 ? 1 : (r2 == r4 ? 0 : -1));
            if (r2 == 0) goto L_0x001c;
        L_0x001b:
            return r0;
        L_0x001c:
            r1 = r1 + 1;
            goto L_0x0010;
        L_0x001f:
            return r6;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getCpuFreqTimes(int):long[]");
        }

        public long[] getScreenOffCpuFreqTimes(int r8) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r7 = this;
            r6 = 0;
            r2 = r7.mScreenOffCpuFreqTimeMs;
            if (r2 != 0) goto L_0x0006;
        L_0x0005:
            return r6;
        L_0x0006:
            r2 = r7.mScreenOffCpuFreqTimeMs;
            r0 = r2.getCountsLocked(r8);
            if (r0 != 0) goto L_0x000f;
        L_0x000e:
            return r6;
        L_0x000f:
            r1 = 0;
        L_0x0010:
            r2 = r0.length;
            if (r1 >= r2) goto L_0x001f;
        L_0x0013:
            r2 = r0[r1];
            r4 = 0;
            r2 = (r2 > r4 ? 1 : (r2 == r4 ? 0 : -1));
            if (r2 == 0) goto L_0x001c;
        L_0x001b:
            return r0;
        L_0x001c:
            r1 = r1 + 1;
            goto L_0x0010;
        L_0x001f:
            return r6;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getScreenOffCpuFreqTimes(int):long[]");
        }

        public com.android.internal.os.BatteryStatsImpl.Timer getAggregatedPartialWakelockTimer() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mAggregatedPartialWakelockTimer;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getAggregatedPartialWakelockTimer():com.android.internal.os.BatteryStatsImpl$Timer");
        }

        public android.util.ArrayMap<java.lang.String, ? extends android.os.BatteryStats.Uid.Wakelock> getWakelockStats() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mWakelockStats;
            r0 = r0.getMap();
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getWakelockStats():android.util.ArrayMap<java.lang.String, ? extends android.os.BatteryStats$Uid$Wakelock>");
        }

        public android.util.ArrayMap<java.lang.String, ? extends android.os.BatteryStats.Timer> getSyncStats() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mSyncStats;
            r0 = r0.getMap();
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getSyncStats():android.util.ArrayMap<java.lang.String, ? extends android.os.BatteryStats$Timer>");
        }

        public android.util.ArrayMap<java.lang.String, ? extends android.os.BatteryStats.Timer> getJobStats() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mJobStats;
            r0 = r0.getMap();
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getJobStats():android.util.ArrayMap<java.lang.String, ? extends android.os.BatteryStats$Timer>");
        }

        public android.util.ArrayMap<java.lang.String, android.util.SparseIntArray> getJobCompletionStats() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mJobCompletions;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getJobCompletionStats():android.util.ArrayMap<java.lang.String, android.util.SparseIntArray>");
        }

        public android.util.SparseArray<? extends android.os.BatteryStats.Uid.Sensor> getSensorStats() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mSensorStats;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getSensorStats():android.util.SparseArray<? extends android.os.BatteryStats$Uid$Sensor>");
        }

        public android.util.ArrayMap<java.lang.String, ? extends android.os.BatteryStats.Uid.Proc> getProcessStats() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mProcessStats;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getProcessStats():android.util.ArrayMap<java.lang.String, ? extends android.os.BatteryStats$Uid$Proc>");
        }

        public android.util.ArrayMap<java.lang.String, ? extends android.os.BatteryStats.Uid.Pkg> getPackageStats() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mPackageStats;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getPackageStats():android.util.ArrayMap<java.lang.String, ? extends android.os.BatteryStats$Uid$Pkg>");
        }

        public int getUid() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mUid;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getUid():int");
        }

        public void noteWifiRunningLocked(long r8) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r7 = this;
            r0 = r7.mWifiRunning;
            if (r0 != 0) goto L_0x0025;
        L_0x0004:
            r0 = 1;
            r7.mWifiRunning = r0;
            r0 = r7.mWifiRunningTimer;
            if (r0 != 0) goto L_0x0020;
        L_0x000b:
            r0 = new com.android.internal.os.BatteryStatsImpl$StopwatchTimer;
            r1 = r7.mBsi;
            r1 = r1.mClocks;
            r2 = r7.mBsi;
            r4 = r2.mWifiRunningTimers;
            r2 = r7.mBsi;
            r5 = r2.mOnBatteryTimeBase;
            r3 = 4;
            r2 = r7;
            r0.<init>(r1, r2, r3, r4, r5);
            r7.mWifiRunningTimer = r0;
        L_0x0020:
            r0 = r7.mWifiRunningTimer;
            r0.startRunningLocked(r8);
        L_0x0025:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteWifiRunningLocked(long):void");
        }

        public void noteWifiStoppedLocked(long r2) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mWifiRunning;
            if (r0 == 0) goto L_0x000c;
        L_0x0004:
            r0 = 0;
            r1.mWifiRunning = r0;
            r0 = r1.mWifiRunningTimer;
            r0.stopRunningLocked(r2);
        L_0x000c:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteWifiStoppedLocked(long):void");
        }

        public void noteFullWifiLockAcquiredLocked(long r8) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r7 = this;
            r0 = r7.mFullWifiLockOut;
            if (r0 != 0) goto L_0x0025;
        L_0x0004:
            r0 = 1;
            r7.mFullWifiLockOut = r0;
            r0 = r7.mFullWifiLockTimer;
            if (r0 != 0) goto L_0x0020;
        L_0x000b:
            r0 = new com.android.internal.os.BatteryStatsImpl$StopwatchTimer;
            r1 = r7.mBsi;
            r1 = r1.mClocks;
            r2 = r7.mBsi;
            r4 = r2.mFullWifiLockTimers;
            r2 = r7.mBsi;
            r5 = r2.mOnBatteryTimeBase;
            r3 = 5;
            r2 = r7;
            r0.<init>(r1, r2, r3, r4, r5);
            r7.mFullWifiLockTimer = r0;
        L_0x0020:
            r0 = r7.mFullWifiLockTimer;
            r0.startRunningLocked(r8);
        L_0x0025:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteFullWifiLockAcquiredLocked(long):void");
        }

        public void noteFullWifiLockReleasedLocked(long r2) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mFullWifiLockOut;
            if (r0 == 0) goto L_0x000c;
        L_0x0004:
            r0 = 0;
            r1.mFullWifiLockOut = r0;
            r0 = r1.mFullWifiLockTimer;
            r0.stopRunningLocked(r2);
        L_0x000c:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteFullWifiLockReleasedLocked(long):void");
        }

        public void noteWifiScanStartedLocked(long r8) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r7 = this;
            r0 = r7.mWifiScanStarted;
            if (r0 != 0) goto L_0x0027;
        L_0x0004:
            r0 = 1;
            r7.mWifiScanStarted = r0;
            r0 = r7.mWifiScanTimer;
            if (r0 != 0) goto L_0x0022;
        L_0x000b:
            r0 = new com.android.internal.os.BatteryStatsImpl$DualTimer;
            r1 = r7.mBsi;
            r1 = r1.mClocks;
            r2 = r7.mBsi;
            r4 = r2.mWifiScanTimers;
            r2 = r7.mBsi;
            r5 = r2.mOnBatteryTimeBase;
            r6 = r7.mOnBatteryBackgroundTimeBase;
            r3 = 6;
            r2 = r7;
            r0.<init>(r1, r2, r3, r4, r5, r6);
            r7.mWifiScanTimer = r0;
        L_0x0022:
            r0 = r7.mWifiScanTimer;
            r0.startRunningLocked(r8);
        L_0x0027:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteWifiScanStartedLocked(long):void");
        }

        public void noteWifiScanStoppedLocked(long r2) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mWifiScanStarted;
            if (r0 == 0) goto L_0x000c;
        L_0x0004:
            r0 = 0;
            r1.mWifiScanStarted = r0;
            r0 = r1.mWifiScanTimer;
            r0.stopRunningLocked(r2);
        L_0x000c:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteWifiScanStoppedLocked(long):void");
        }

        public void noteWifiBatchedScanStartedLocked(int r5, long r6) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r4 = this;
            r3 = 0;
            r0 = 0;
        L_0x0002:
            r1 = 8;
            if (r5 <= r1) goto L_0x000e;
        L_0x0006:
            r1 = 4;
            if (r0 >= r1) goto L_0x000e;
        L_0x0009:
            r5 = r5 >> 3;
            r0 = r0 + 1;
            goto L_0x0002;
        L_0x000e:
            r1 = r4.mWifiBatchedScanBinStarted;
            if (r1 != r0) goto L_0x0013;
        L_0x0012:
            return;
        L_0x0013:
            r1 = r4.mWifiBatchedScanBinStarted;
            r2 = -1;
            if (r1 == r2) goto L_0x0021;
        L_0x0018:
            r1 = r4.mWifiBatchedScanTimer;
            r2 = r4.mWifiBatchedScanBinStarted;
            r1 = r1[r2];
            r1.stopRunningLocked(r6);
        L_0x0021:
            r4.mWifiBatchedScanBinStarted = r0;
            r1 = r4.mWifiBatchedScanTimer;
            r1 = r1[r0];
            if (r1 != 0) goto L_0x002c;
        L_0x0029:
            r4.makeWifiBatchedScanBin(r0, r3);
        L_0x002c:
            r1 = r4.mWifiBatchedScanTimer;
            r1 = r1[r0];
            r1.startRunningLocked(r6);
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteWifiBatchedScanStartedLocked(int, long):void");
        }

        public void noteWifiBatchedScanStoppedLocked(long r4) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r3 = this;
            r2 = -1;
            r0 = r3.mWifiBatchedScanBinStarted;
            if (r0 == r2) goto L_0x0010;
        L_0x0005:
            r0 = r3.mWifiBatchedScanTimer;
            r1 = r3.mWifiBatchedScanBinStarted;
            r0 = r0[r1];
            r0.stopRunningLocked(r4);
            r3.mWifiBatchedScanBinStarted = r2;
        L_0x0010:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteWifiBatchedScanStoppedLocked(long):void");
        }

        public void noteWifiMulticastEnabledLocked(long r8) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r7 = this;
            r0 = r7.mWifiMulticastEnabled;
            if (r0 != 0) goto L_0x0025;
        L_0x0004:
            r0 = 1;
            r7.mWifiMulticastEnabled = r0;
            r0 = r7.mWifiMulticastTimer;
            if (r0 != 0) goto L_0x0020;
        L_0x000b:
            r0 = new com.android.internal.os.BatteryStatsImpl$StopwatchTimer;
            r1 = r7.mBsi;
            r1 = r1.mClocks;
            r2 = r7.mBsi;
            r4 = r2.mWifiMulticastTimers;
            r2 = r7.mBsi;
            r5 = r2.mOnBatteryTimeBase;
            r3 = 7;
            r2 = r7;
            r0.<init>(r1, r2, r3, r4, r5);
            r7.mWifiMulticastTimer = r0;
        L_0x0020:
            r0 = r7.mWifiMulticastTimer;
            r0.startRunningLocked(r8);
        L_0x0025:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteWifiMulticastEnabledLocked(long):void");
        }

        public void noteWifiMulticastDisabledLocked(long r2) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mWifiMulticastEnabled;
            if (r0 == 0) goto L_0x000c;
        L_0x0004:
            r0 = 0;
            r1.mWifiMulticastEnabled = r0;
            r0 = r1.mWifiMulticastTimer;
            r0.stopRunningLocked(r2);
        L_0x000c:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteWifiMulticastDisabledLocked(long):void");
        }

        public android.os.BatteryStats.ControllerActivityCounter getWifiControllerActivity() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mWifiControllerActivity;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getWifiControllerActivity():android.os.BatteryStats$ControllerActivityCounter");
        }

        public android.os.BatteryStats.ControllerActivityCounter getBluetoothControllerActivity() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mBluetoothControllerActivity;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getBluetoothControllerActivity():android.os.BatteryStats$ControllerActivityCounter");
        }

        public android.os.BatteryStats.ControllerActivityCounter getModemControllerActivity() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mModemControllerActivity;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getModemControllerActivity():android.os.BatteryStats$ControllerActivityCounter");
        }

        public com.android.internal.os.BatteryStatsImpl.ControllerActivityCounterImpl getOrCreateWifiControllerActivityLocked() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r3 = this;
            r0 = r3.mWifiControllerActivity;
            if (r0 != 0) goto L_0x0010;
        L_0x0004:
            r0 = new com.android.internal.os.BatteryStatsImpl$ControllerActivityCounterImpl;
            r1 = r3.mBsi;
            r1 = r1.mOnBatteryTimeBase;
            r2 = 1;
            r0.<init>(r1, r2);
            r3.mWifiControllerActivity = r0;
        L_0x0010:
            r0 = r3.mWifiControllerActivity;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getOrCreateWifiControllerActivityLocked():com.android.internal.os.BatteryStatsImpl$ControllerActivityCounterImpl");
        }

        public com.android.internal.os.BatteryStatsImpl.ControllerActivityCounterImpl getOrCreateBluetoothControllerActivityLocked() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r3 = this;
            r0 = r3.mBluetoothControllerActivity;
            if (r0 != 0) goto L_0x0010;
        L_0x0004:
            r0 = new com.android.internal.os.BatteryStatsImpl$ControllerActivityCounterImpl;
            r1 = r3.mBsi;
            r1 = r1.mOnBatteryTimeBase;
            r2 = 1;
            r0.<init>(r1, r2);
            r3.mBluetoothControllerActivity = r0;
        L_0x0010:
            r0 = r3.mBluetoothControllerActivity;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getOrCreateBluetoothControllerActivityLocked():com.android.internal.os.BatteryStatsImpl$ControllerActivityCounterImpl");
        }

        public com.android.internal.os.BatteryStatsImpl.ControllerActivityCounterImpl getOrCreateModemControllerActivityLocked() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r3 = this;
            r0 = r3.mModemControllerActivity;
            if (r0 != 0) goto L_0x0010;
        L_0x0004:
            r0 = new com.android.internal.os.BatteryStatsImpl$ControllerActivityCounterImpl;
            r1 = r3.mBsi;
            r1 = r1.mOnBatteryTimeBase;
            r2 = 5;
            r0.<init>(r1, r2);
            r3.mModemControllerActivity = r0;
        L_0x0010:
            r0 = r3.mModemControllerActivity;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getOrCreateModemControllerActivityLocked():com.android.internal.os.BatteryStatsImpl$ControllerActivityCounterImpl");
        }

        public com.android.internal.os.BatteryStatsImpl.StopwatchTimer createAudioTurnedOnTimerLocked() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r6 = this;
            r0 = r6.mAudioTurnedOnTimer;
            if (r0 != 0) goto L_0x001a;
        L_0x0004:
            r0 = new com.android.internal.os.BatteryStatsImpl$StopwatchTimer;
            r1 = r6.mBsi;
            r1 = r1.mClocks;
            r2 = r6.mBsi;
            r4 = r2.mAudioTurnedOnTimers;
            r2 = r6.mBsi;
            r5 = r2.mOnBatteryTimeBase;
            r3 = 15;
            r2 = r6;
            r0.<init>(r1, r2, r3, r4, r5);
            r6.mAudioTurnedOnTimer = r0;
        L_0x001a:
            r0 = r6.mAudioTurnedOnTimer;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.createAudioTurnedOnTimerLocked():com.android.internal.os.BatteryStatsImpl$StopwatchTimer");
        }

        public void noteAudioTurnedOnLocked(long r2) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.createAudioTurnedOnTimerLocked();
            r0.startRunningLocked(r2);
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteAudioTurnedOnLocked(long):void");
        }

        public void noteAudioTurnedOffLocked(long r2) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mAudioTurnedOnTimer;
            if (r0 == 0) goto L_0x0009;
        L_0x0004:
            r0 = r1.mAudioTurnedOnTimer;
            r0.stopRunningLocked(r2);
        L_0x0009:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteAudioTurnedOffLocked(long):void");
        }

        public void noteResetAudioLocked(long r2) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mAudioTurnedOnTimer;
            if (r0 == 0) goto L_0x0009;
        L_0x0004:
            r0 = r1.mAudioTurnedOnTimer;
            r0.stopAllRunningLocked(r2);
        L_0x0009:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteResetAudioLocked(long):void");
        }

        public com.android.internal.os.BatteryStatsImpl.StopwatchTimer createVideoTurnedOnTimerLocked() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r6 = this;
            r0 = r6.mVideoTurnedOnTimer;
            if (r0 != 0) goto L_0x001a;
        L_0x0004:
            r0 = new com.android.internal.os.BatteryStatsImpl$StopwatchTimer;
            r1 = r6.mBsi;
            r1 = r1.mClocks;
            r2 = r6.mBsi;
            r4 = r2.mVideoTurnedOnTimers;
            r2 = r6.mBsi;
            r5 = r2.mOnBatteryTimeBase;
            r3 = 8;
            r2 = r6;
            r0.<init>(r1, r2, r3, r4, r5);
            r6.mVideoTurnedOnTimer = r0;
        L_0x001a:
            r0 = r6.mVideoTurnedOnTimer;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.createVideoTurnedOnTimerLocked():com.android.internal.os.BatteryStatsImpl$StopwatchTimer");
        }

        public void noteVideoTurnedOnLocked(long r2) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.createVideoTurnedOnTimerLocked();
            r0.startRunningLocked(r2);
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteVideoTurnedOnLocked(long):void");
        }

        public void noteVideoTurnedOffLocked(long r2) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mVideoTurnedOnTimer;
            if (r0 == 0) goto L_0x0009;
        L_0x0004:
            r0 = r1.mVideoTurnedOnTimer;
            r0.stopRunningLocked(r2);
        L_0x0009:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteVideoTurnedOffLocked(long):void");
        }

        public void noteResetVideoLocked(long r2) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mVideoTurnedOnTimer;
            if (r0 == 0) goto L_0x0009;
        L_0x0004:
            r0 = r1.mVideoTurnedOnTimer;
            r0.stopAllRunningLocked(r2);
        L_0x0009:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteResetVideoLocked(long):void");
        }

        public com.android.internal.os.BatteryStatsImpl.StopwatchTimer createFlashlightTurnedOnTimerLocked() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r6 = this;
            r0 = r6.mFlashlightTurnedOnTimer;
            if (r0 != 0) goto L_0x001a;
        L_0x0004:
            r0 = new com.android.internal.os.BatteryStatsImpl$StopwatchTimer;
            r1 = r6.mBsi;
            r1 = r1.mClocks;
            r2 = r6.mBsi;
            r4 = r2.mFlashlightTurnedOnTimers;
            r2 = r6.mBsi;
            r5 = r2.mOnBatteryTimeBase;
            r3 = 16;
            r2 = r6;
            r0.<init>(r1, r2, r3, r4, r5);
            r6.mFlashlightTurnedOnTimer = r0;
        L_0x001a:
            r0 = r6.mFlashlightTurnedOnTimer;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.createFlashlightTurnedOnTimerLocked():com.android.internal.os.BatteryStatsImpl$StopwatchTimer");
        }

        public void noteFlashlightTurnedOnLocked(long r2) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.createFlashlightTurnedOnTimerLocked();
            r0.startRunningLocked(r2);
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteFlashlightTurnedOnLocked(long):void");
        }

        public void noteFlashlightTurnedOffLocked(long r2) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mFlashlightTurnedOnTimer;
            if (r0 == 0) goto L_0x0009;
        L_0x0004:
            r0 = r1.mFlashlightTurnedOnTimer;
            r0.stopRunningLocked(r2);
        L_0x0009:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteFlashlightTurnedOffLocked(long):void");
        }

        public void noteResetFlashlightLocked(long r2) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mFlashlightTurnedOnTimer;
            if (r0 == 0) goto L_0x0009;
        L_0x0004:
            r0 = r1.mFlashlightTurnedOnTimer;
            r0.stopAllRunningLocked(r2);
        L_0x0009:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteResetFlashlightLocked(long):void");
        }

        public com.android.internal.os.BatteryStatsImpl.StopwatchTimer createCameraTurnedOnTimerLocked() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r6 = this;
            r0 = r6.mCameraTurnedOnTimer;
            if (r0 != 0) goto L_0x001a;
        L_0x0004:
            r0 = new com.android.internal.os.BatteryStatsImpl$StopwatchTimer;
            r1 = r6.mBsi;
            r1 = r1.mClocks;
            r2 = r6.mBsi;
            r4 = r2.mCameraTurnedOnTimers;
            r2 = r6.mBsi;
            r5 = r2.mOnBatteryTimeBase;
            r3 = 17;
            r2 = r6;
            r0.<init>(r1, r2, r3, r4, r5);
            r6.mCameraTurnedOnTimer = r0;
        L_0x001a:
            r0 = r6.mCameraTurnedOnTimer;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.createCameraTurnedOnTimerLocked():com.android.internal.os.BatteryStatsImpl$StopwatchTimer");
        }

        public void noteCameraTurnedOnLocked(long r2) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.createCameraTurnedOnTimerLocked();
            r0.startRunningLocked(r2);
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteCameraTurnedOnLocked(long):void");
        }

        public void noteCameraTurnedOffLocked(long r2) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mCameraTurnedOnTimer;
            if (r0 == 0) goto L_0x0009;
        L_0x0004:
            r0 = r1.mCameraTurnedOnTimer;
            r0.stopRunningLocked(r2);
        L_0x0009:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteCameraTurnedOffLocked(long):void");
        }

        public void noteResetCameraLocked(long r2) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mCameraTurnedOnTimer;
            if (r0 == 0) goto L_0x0009;
        L_0x0004:
            r0 = r1.mCameraTurnedOnTimer;
            r0.stopAllRunningLocked(r2);
        L_0x0009:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteResetCameraLocked(long):void");
        }

        public com.android.internal.os.BatteryStatsImpl.StopwatchTimer createForegroundActivityTimerLocked() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r6 = this;
            r4 = 0;
            r0 = r6.mForegroundActivityTimer;
            if (r0 != 0) goto L_0x0017;
        L_0x0005:
            r0 = new com.android.internal.os.BatteryStatsImpl$StopwatchTimer;
            r1 = r6.mBsi;
            r1 = r1.mClocks;
            r2 = r6.mBsi;
            r5 = r2.mOnBatteryTimeBase;
            r3 = 10;
            r2 = r6;
            r0.<init>(r1, r2, r3, r4, r5);
            r6.mForegroundActivityTimer = r0;
        L_0x0017:
            r0 = r6.mForegroundActivityTimer;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.createForegroundActivityTimerLocked():com.android.internal.os.BatteryStatsImpl$StopwatchTimer");
        }

        public com.android.internal.os.BatteryStatsImpl.StopwatchTimer createForegroundServiceTimerLocked() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r6 = this;
            r4 = 0;
            r0 = r6.mForegroundServiceTimer;
            if (r0 != 0) goto L_0x0017;
        L_0x0005:
            r0 = new com.android.internal.os.BatteryStatsImpl$StopwatchTimer;
            r1 = r6.mBsi;
            r1 = r1.mClocks;
            r2 = r6.mBsi;
            r5 = r2.mOnBatteryTimeBase;
            r3 = 22;
            r2 = r6;
            r0.<init>(r1, r2, r3, r4, r5);
            r6.mForegroundServiceTimer = r0;
        L_0x0017:
            r0 = r6.mForegroundServiceTimer;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.createForegroundServiceTimerLocked():com.android.internal.os.BatteryStatsImpl$StopwatchTimer");
        }

        public com.android.internal.os.BatteryStatsImpl.DualTimer createAggregatedPartialWakelockTimerLocked() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r7 = this;
            r4 = 0;
            r0 = r7.mAggregatedPartialWakelockTimer;
            if (r0 != 0) goto L_0x0019;
        L_0x0005:
            r0 = new com.android.internal.os.BatteryStatsImpl$DualTimer;
            r1 = r7.mBsi;
            r1 = r1.mClocks;
            r2 = r7.mBsi;
            r5 = r2.mOnBatteryScreenOffTimeBase;
            r6 = r7.mOnBatteryScreenOffBackgroundTimeBase;
            r3 = 20;
            r2 = r7;
            r0.<init>(r1, r2, r3, r4, r5, r6);
            r7.mAggregatedPartialWakelockTimer = r0;
        L_0x0019:
            r0 = r7.mAggregatedPartialWakelockTimer;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.createAggregatedPartialWakelockTimerLocked():com.android.internal.os.BatteryStatsImpl$DualTimer");
        }

        public com.android.internal.os.BatteryStatsImpl.DualTimer createBluetoothScanTimerLocked() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r7 = this;
            r0 = r7.mBluetoothScanTimer;
            if (r0 != 0) goto L_0x001c;
        L_0x0004:
            r0 = new com.android.internal.os.BatteryStatsImpl$DualTimer;
            r1 = r7.mBsi;
            r1 = r1.mClocks;
            r2 = r7.mBsi;
            r4 = r2.mBluetoothScanOnTimers;
            r2 = r7.mBsi;
            r5 = r2.mOnBatteryTimeBase;
            r6 = r7.mOnBatteryBackgroundTimeBase;
            r3 = 19;
            r2 = r7;
            r0.<init>(r1, r2, r3, r4, r5, r6);
            r7.mBluetoothScanTimer = r0;
        L_0x001c:
            r0 = r7.mBluetoothScanTimer;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.createBluetoothScanTimerLocked():com.android.internal.os.BatteryStatsImpl$DualTimer");
        }

        public com.android.internal.os.BatteryStatsImpl.DualTimer createBluetoothUnoptimizedScanTimerLocked() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r7 = this;
            r4 = 0;
            r0 = r7.mBluetoothUnoptimizedScanTimer;
            if (r0 != 0) goto L_0x0019;
        L_0x0005:
            r0 = new com.android.internal.os.BatteryStatsImpl$DualTimer;
            r1 = r7.mBsi;
            r1 = r1.mClocks;
            r2 = r7.mBsi;
            r5 = r2.mOnBatteryTimeBase;
            r6 = r7.mOnBatteryBackgroundTimeBase;
            r3 = 21;
            r2 = r7;
            r0.<init>(r1, r2, r3, r4, r5, r6);
            r7.mBluetoothUnoptimizedScanTimer = r0;
        L_0x0019:
            r0 = r7.mBluetoothUnoptimizedScanTimer;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.createBluetoothUnoptimizedScanTimerLocked():com.android.internal.os.BatteryStatsImpl$DualTimer");
        }

        public void noteBluetoothScanStartedLocked(long r2, boolean r4) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.createBluetoothScanTimerLocked();
            r0.startRunningLocked(r2);
            if (r4 == 0) goto L_0x0010;
        L_0x0009:
            r0 = r1.createBluetoothUnoptimizedScanTimerLocked();
            r0.startRunningLocked(r2);
        L_0x0010:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteBluetoothScanStartedLocked(long, boolean):void");
        }

        public void noteBluetoothScanStoppedLocked(long r2, boolean r4) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mBluetoothScanTimer;
            if (r0 == 0) goto L_0x0009;
        L_0x0004:
            r0 = r1.mBluetoothScanTimer;
            r0.stopRunningLocked(r2);
        L_0x0009:
            if (r4 == 0) goto L_0x0014;
        L_0x000b:
            r0 = r1.mBluetoothUnoptimizedScanTimer;
            if (r0 == 0) goto L_0x0014;
        L_0x000f:
            r0 = r1.mBluetoothUnoptimizedScanTimer;
            r0.stopRunningLocked(r2);
        L_0x0014:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteBluetoothScanStoppedLocked(long, boolean):void");
        }

        public void noteResetBluetoothScanLocked(long r2) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mBluetoothScanTimer;
            if (r0 == 0) goto L_0x0009;
        L_0x0004:
            r0 = r1.mBluetoothScanTimer;
            r0.stopAllRunningLocked(r2);
        L_0x0009:
            r0 = r1.mBluetoothUnoptimizedScanTimer;
            if (r0 == 0) goto L_0x0012;
        L_0x000d:
            r0 = r1.mBluetoothUnoptimizedScanTimer;
            r0.stopAllRunningLocked(r2);
        L_0x0012:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteResetBluetoothScanLocked(long):void");
        }

        public com.android.internal.os.BatteryStatsImpl.Counter createBluetoothScanResultCounterLocked() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r2 = this;
            r0 = r2.mBluetoothScanResultCounter;
            if (r0 != 0) goto L_0x000f;
        L_0x0004:
            r0 = new com.android.internal.os.BatteryStatsImpl$Counter;
            r1 = r2.mBsi;
            r1 = r1.mOnBatteryTimeBase;
            r0.<init>(r1);
            r2.mBluetoothScanResultCounter = r0;
        L_0x000f:
            r0 = r2.mBluetoothScanResultCounter;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.createBluetoothScanResultCounterLocked():com.android.internal.os.BatteryStatsImpl$Counter");
        }

        public com.android.internal.os.BatteryStatsImpl.Counter createBluetoothScanResultBgCounterLocked() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r2 = this;
            r0 = r2.mBluetoothScanResultBgCounter;
            if (r0 != 0) goto L_0x000d;
        L_0x0004:
            r0 = new com.android.internal.os.BatteryStatsImpl$Counter;
            r1 = r2.mOnBatteryBackgroundTimeBase;
            r0.<init>(r1);
            r2.mBluetoothScanResultBgCounter = r0;
        L_0x000d:
            r0 = r2.mBluetoothScanResultBgCounter;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.createBluetoothScanResultBgCounterLocked():com.android.internal.os.BatteryStatsImpl$Counter");
        }

        public void noteBluetoothScanResultsLocked(int r2) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.createBluetoothScanResultCounterLocked();
            r0.addAtomic(r2);
            r0 = r1.createBluetoothScanResultBgCounterLocked();
            r0.addAtomic(r2);
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteBluetoothScanResultsLocked(int):void");
        }

        public void noteActivityResumedLocked(long r2) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.createForegroundActivityTimerLocked();
            r0.startRunningLocked(r2);
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteActivityResumedLocked(long):void");
        }

        public void noteActivityPausedLocked(long r2) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mForegroundActivityTimer;
            if (r0 == 0) goto L_0x0009;
        L_0x0004:
            r0 = r1.mForegroundActivityTimer;
            r0.stopRunningLocked(r2);
        L_0x0009:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteActivityPausedLocked(long):void");
        }

        public void noteForegroundServiceResumedLocked(long r2) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.createForegroundServiceTimerLocked();
            r0.startRunningLocked(r2);
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteForegroundServiceResumedLocked(long):void");
        }

        public void noteForegroundServicePausedLocked(long r2) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mForegroundServiceTimer;
            if (r0 == 0) goto L_0x0009;
        L_0x0004:
            r0 = r1.mForegroundServiceTimer;
            r0.stopRunningLocked(r2);
        L_0x0009:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteForegroundServicePausedLocked(long):void");
        }

        public com.android.internal.os.BatteryStatsImpl.BatchTimer createVibratorOnTimerLocked() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r4 = this;
            r0 = r4.mVibratorOnTimer;
            if (r0 != 0) goto L_0x0015;
        L_0x0004:
            r0 = new com.android.internal.os.BatteryStatsImpl$BatchTimer;
            r1 = r4.mBsi;
            r1 = r1.mClocks;
            r2 = r4.mBsi;
            r2 = r2.mOnBatteryTimeBase;
            r3 = 9;
            r0.<init>(r1, r4, r3, r2);
            r4.mVibratorOnTimer = r0;
        L_0x0015:
            r0 = r4.mVibratorOnTimer;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.createVibratorOnTimerLocked():com.android.internal.os.BatteryStatsImpl$BatchTimer");
        }

        public void noteVibratorOnLocked(long r4) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r3 = this;
            r0 = r3.createVibratorOnTimerLocked();
            r1 = r3.mBsi;
            r0.addDuration(r1, r4);
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteVibratorOnLocked(long):void");
        }

        public void noteVibratorOffLocked() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r2 = this;
            r0 = r2.mVibratorOnTimer;
            if (r0 == 0) goto L_0x000b;
        L_0x0004:
            r0 = r2.mVibratorOnTimer;
            r1 = r2.mBsi;
            r0.abortLastDuration(r1);
        L_0x000b:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteVibratorOffLocked():void");
        }

        public long getWifiRunningTime(long r4, int r6) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r3 = this;
            r0 = r3.mWifiRunningTimer;
            if (r0 != 0) goto L_0x0007;
        L_0x0004:
            r0 = 0;
            return r0;
        L_0x0007:
            r0 = r3.mWifiRunningTimer;
            r0 = r0.getTotalTimeLocked(r4, r6);
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getWifiRunningTime(long, int):long");
        }

        public long getFullWifiLockTime(long r4, int r6) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r3 = this;
            r0 = r3.mFullWifiLockTimer;
            if (r0 != 0) goto L_0x0007;
        L_0x0004:
            r0 = 0;
            return r0;
        L_0x0007:
            r0 = r3.mFullWifiLockTimer;
            r0 = r0.getTotalTimeLocked(r4, r6);
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getFullWifiLockTime(long, int):long");
        }

        public long getWifiScanTime(long r4, int r6) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r3 = this;
            r0 = r3.mWifiScanTimer;
            if (r0 != 0) goto L_0x0007;
        L_0x0004:
            r0 = 0;
            return r0;
        L_0x0007:
            r0 = r3.mWifiScanTimer;
            r0 = r0.getTotalTimeLocked(r4, r6);
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getWifiScanTime(long, int):long");
        }

        public int getWifiScanCount(int r2) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mWifiScanTimer;
            if (r0 != 0) goto L_0x0006;
        L_0x0004:
            r0 = 0;
            return r0;
        L_0x0006:
            r0 = r1.mWifiScanTimer;
            r0 = r0.getCountLocked(r2);
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getWifiScanCount(int):int");
        }

        public int getWifiScanBackgroundCount(int r2) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mWifiScanTimer;
            if (r0 == 0) goto L_0x000c;
        L_0x0004:
            r0 = r1.mWifiScanTimer;
            r0 = r0.getSubTimer();
            if (r0 != 0) goto L_0x000e;
        L_0x000c:
            r0 = 0;
            return r0;
        L_0x000e:
            r0 = r1.mWifiScanTimer;
            r0 = r0.getSubTimer();
            r0 = r0.getCountLocked(r2);
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getWifiScanBackgroundCount(int):int");
        }

        public long getWifiScanActualTime(long r8) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r7 = this;
            r4 = 1000; // 0x3e8 float:1.401E-42 double:4.94E-321;
            r2 = r7.mWifiScanTimer;
            if (r2 != 0) goto L_0x0009;
        L_0x0006:
            r2 = 0;
            return r2;
        L_0x0009:
            r2 = 500; // 0x1f4 float:7.0E-43 double:2.47E-321;
            r2 = r2 + r8;
            r0 = r2 / r4;
            r2 = r7.mWifiScanTimer;
            r2 = r2.getTotalDurationMsLocked(r0);
            r2 = r2 * r4;
            return r2;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getWifiScanActualTime(long):long");
        }

        public long getWifiScanBackgroundTime(long r8) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r7 = this;
            r4 = 1000; // 0x3e8 float:1.401E-42 double:4.94E-321;
            r2 = r7.mWifiScanTimer;
            if (r2 == 0) goto L_0x000e;
        L_0x0006:
            r2 = r7.mWifiScanTimer;
            r2 = r2.getSubTimer();
            if (r2 != 0) goto L_0x0011;
        L_0x000e:
            r2 = 0;
            return r2;
        L_0x0011:
            r2 = 500; // 0x1f4 float:7.0E-43 double:2.47E-321;
            r2 = r2 + r8;
            r0 = r2 / r4;
            r2 = r7.mWifiScanTimer;
            r2 = r2.getSubTimer();
            r2 = r2.getTotalDurationMsLocked(r0);
            r2 = r2 * r4;
            return r2;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getWifiScanBackgroundTime(long):long");
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public long getWifiBatchedScanTime(int r5, long r6, int r8) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r4 = this;
            r2 = 0;
            if (r5 < 0) goto L_0x0007;
        L_0x0004:
            r0 = 5;
            if (r5 < r0) goto L_0x0008;
        L_0x0007:
            return r2;
        L_0x0008:
            r0 = r4.mWifiBatchedScanTimer;
            r0 = r0[r5];
            if (r0 != 0) goto L_0x000f;
        L_0x000e:
            return r2;
        L_0x000f:
            r0 = r4.mWifiBatchedScanTimer;
            r0 = r0[r5];
            r0 = r0.getTotalTimeLocked(r6, r8);
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getWifiBatchedScanTime(int, long, int):long");
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public int getWifiBatchedScanCount(int r3, int r4) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r2 = this;
            r1 = 0;
            if (r3 < 0) goto L_0x0006;
        L_0x0003:
            r0 = 5;
            if (r3 < r0) goto L_0x0007;
        L_0x0006:
            return r1;
        L_0x0007:
            r0 = r2.mWifiBatchedScanTimer;
            r0 = r0[r3];
            if (r0 != 0) goto L_0x000e;
        L_0x000d:
            return r1;
        L_0x000e:
            r0 = r2.mWifiBatchedScanTimer;
            r0 = r0[r3];
            r0 = r0.getCountLocked(r4);
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getWifiBatchedScanCount(int, int):int");
        }

        public long getWifiMulticastTime(long r4, int r6) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r3 = this;
            r0 = r3.mWifiMulticastTimer;
            if (r0 != 0) goto L_0x0007;
        L_0x0004:
            r0 = 0;
            return r0;
        L_0x0007:
            r0 = r3.mWifiMulticastTimer;
            r0 = r0.getTotalTimeLocked(r4, r6);
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getWifiMulticastTime(long, int):long");
        }

        public com.android.internal.os.BatteryStatsImpl.Timer getAudioTurnedOnTimer() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mAudioTurnedOnTimer;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getAudioTurnedOnTimer():com.android.internal.os.BatteryStatsImpl$Timer");
        }

        public com.android.internal.os.BatteryStatsImpl.Timer getVideoTurnedOnTimer() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mVideoTurnedOnTimer;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getVideoTurnedOnTimer():com.android.internal.os.BatteryStatsImpl$Timer");
        }

        public com.android.internal.os.BatteryStatsImpl.Timer getFlashlightTurnedOnTimer() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mFlashlightTurnedOnTimer;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getFlashlightTurnedOnTimer():com.android.internal.os.BatteryStatsImpl$Timer");
        }

        public com.android.internal.os.BatteryStatsImpl.Timer getCameraTurnedOnTimer() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mCameraTurnedOnTimer;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getCameraTurnedOnTimer():com.android.internal.os.BatteryStatsImpl$Timer");
        }

        public com.android.internal.os.BatteryStatsImpl.Timer getForegroundActivityTimer() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mForegroundActivityTimer;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getForegroundActivityTimer():com.android.internal.os.BatteryStatsImpl$Timer");
        }

        public com.android.internal.os.BatteryStatsImpl.Timer getForegroundServiceTimer() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mForegroundServiceTimer;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getForegroundServiceTimer():com.android.internal.os.BatteryStatsImpl$Timer");
        }

        public com.android.internal.os.BatteryStatsImpl.Timer getBluetoothScanTimer() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mBluetoothScanTimer;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getBluetoothScanTimer():com.android.internal.os.BatteryStatsImpl$Timer");
        }

        public com.android.internal.os.BatteryStatsImpl.Timer getBluetoothScanBackgroundTimer() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r2 = this;
            r1 = 0;
            r0 = r2.mBluetoothScanTimer;
            if (r0 != 0) goto L_0x0006;
        L_0x0005:
            return r1;
        L_0x0006:
            r0 = r2.mBluetoothScanTimer;
            r0 = r0.getSubTimer();
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getBluetoothScanBackgroundTimer():com.android.internal.os.BatteryStatsImpl$Timer");
        }

        public com.android.internal.os.BatteryStatsImpl.Timer getBluetoothUnoptimizedScanTimer() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mBluetoothUnoptimizedScanTimer;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getBluetoothUnoptimizedScanTimer():com.android.internal.os.BatteryStatsImpl$Timer");
        }

        public com.android.internal.os.BatteryStatsImpl.Timer getBluetoothUnoptimizedScanBackgroundTimer() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r2 = this;
            r1 = 0;
            r0 = r2.mBluetoothUnoptimizedScanTimer;
            if (r0 != 0) goto L_0x0006;
        L_0x0005:
            return r1;
        L_0x0006:
            r0 = r2.mBluetoothUnoptimizedScanTimer;
            r0 = r0.getSubTimer();
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getBluetoothUnoptimizedScanBackgroundTimer():com.android.internal.os.BatteryStatsImpl$Timer");
        }

        public com.android.internal.os.BatteryStatsImpl.Counter getBluetoothScanResultCounter() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mBluetoothScanResultCounter;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getBluetoothScanResultCounter():com.android.internal.os.BatteryStatsImpl$Counter");
        }

        public com.android.internal.os.BatteryStatsImpl.Counter getBluetoothScanResultBgCounter() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mBluetoothScanResultBgCounter;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getBluetoothScanResultBgCounter():com.android.internal.os.BatteryStatsImpl$Counter");
        }

        void makeProcessState(int r9, android.os.Parcel r10) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r8 = this;
            r3 = 12;
            r4 = 0;
            if (r9 < 0) goto L_0x0008;
        L_0x0005:
            r0 = 6;
            if (r9 < r0) goto L_0x0009;
        L_0x0008:
            return;
        L_0x0009:
            if (r10 != 0) goto L_0x001e;
        L_0x000b:
            r6 = r8.mProcessStateTimer;
            r0 = new com.android.internal.os.BatteryStatsImpl$StopwatchTimer;
            r1 = r8.mBsi;
            r1 = r1.mClocks;
            r2 = r8.mBsi;
            r5 = r2.mOnBatteryTimeBase;
            r2 = r8;
            r0.<init>(r1, r2, r3, r4, r5);
            r6[r9] = r0;
        L_0x001d:
            return;
        L_0x001e:
            r7 = r8.mProcessStateTimer;
            r0 = new com.android.internal.os.BatteryStatsImpl$StopwatchTimer;
            r1 = r8.mBsi;
            r1 = r1.mClocks;
            r2 = r8.mBsi;
            r5 = r2.mOnBatteryTimeBase;
            r2 = r8;
            r6 = r10;
            r0.<init>(r1, r2, r3, r4, r5, r6);
            r7[r9] = r0;
            goto L_0x001d;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.makeProcessState(int, android.os.Parcel):void");
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public long getProcessStateTime(int r5, long r6, int r8) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r4 = this;
            r2 = 0;
            if (r5 < 0) goto L_0x0007;
        L_0x0004:
            r0 = 6;
            if (r5 < r0) goto L_0x0008;
        L_0x0007:
            return r2;
        L_0x0008:
            r0 = r4.mProcessStateTimer;
            r0 = r0[r5];
            if (r0 != 0) goto L_0x000f;
        L_0x000e:
            return r2;
        L_0x000f:
            r0 = r4.mProcessStateTimer;
            r0 = r0[r5];
            r0 = r0.getTotalTimeLocked(r6, r8);
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getProcessStateTime(int, long, int):long");
        }

        public com.android.internal.os.BatteryStatsImpl.Timer getProcessStateTimer(int r2) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            if (r2 < 0) goto L_0x0005;
        L_0x0002:
            r0 = 6;
            if (r2 < r0) goto L_0x0007;
        L_0x0005:
            r0 = 0;
            return r0;
        L_0x0007:
            r0 = r1.mProcessStateTimer;
            r0 = r0[r2];
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getProcessStateTimer(int):com.android.internal.os.BatteryStatsImpl$Timer");
        }

        public com.android.internal.os.BatteryStatsImpl.Timer getVibratorOnTimer() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mVibratorOnTimer;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getVibratorOnTimer():com.android.internal.os.BatteryStatsImpl$Timer");
        }

        public void noteUserActivityLocked(int r4) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r3 = this;
            r0 = r3.mUserActivityCounters;
            if (r0 != 0) goto L_0x0007;
        L_0x0004:
            r3.initUserActivityLocked();
        L_0x0007:
            if (r4 < 0) goto L_0x0014;
        L_0x0009:
            r0 = 4;
            if (r4 >= r0) goto L_0x0014;
        L_0x000c:
            r0 = r3.mUserActivityCounters;
            r0 = r0[r4];
            r0.stepAtomic();
        L_0x0013:
            return;
        L_0x0014:
            r0 = "BatteryStatsImpl";
            r1 = new java.lang.StringBuilder;
            r1.<init>();
            r2 = "Unknown user activity type ";
            r1 = r1.append(r2);
            r1 = r1.append(r4);
            r2 = " was specified.";
            r1 = r1.append(r2);
            r1 = r1.toString();
            r2 = new java.lang.Throwable;
            r2.<init>();
            android.util.Slog.w(r0, r1, r2);
            goto L_0x0013;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteUserActivityLocked(int):void");
        }

        public boolean hasUserActivity() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mUserActivityCounters;
            if (r0 == 0) goto L_0x0006;
        L_0x0004:
            r0 = 1;
        L_0x0005:
            return r0;
        L_0x0006:
            r0 = 0;
            goto L_0x0005;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.hasUserActivity():boolean");
        }

        public int getUserActivityCount(int r2, int r3) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mUserActivityCounters;
            if (r0 != 0) goto L_0x0006;
        L_0x0004:
            r0 = 0;
            return r0;
        L_0x0006:
            r0 = r1.mUserActivityCounters;
            r0 = r0[r2];
            r0 = r0.getCountLocked(r3);
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getUserActivityCount(int, int):int");
        }

        void makeWifiBatchedScanBin(int r9, android.os.Parcel r10) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r8 = this;
            r3 = 11;
            if (r9 < 0) goto L_0x0007;
        L_0x0004:
            r0 = 5;
            if (r9 < r0) goto L_0x0008;
        L_0x0007:
            return;
        L_0x0008:
            r0 = r8.mBsi;
            r0 = r0.mWifiBatchedScanTimers;
            r4 = r0.get(r9);
            r4 = (java.util.ArrayList) r4;
            if (r4 != 0) goto L_0x0020;
        L_0x0014:
            r4 = new java.util.ArrayList;
            r4.<init>();
            r0 = r8.mBsi;
            r0 = r0.mWifiBatchedScanTimers;
            r0.put(r9, r4);
        L_0x0020:
            if (r10 != 0) goto L_0x0035;
        L_0x0022:
            r6 = r8.mWifiBatchedScanTimer;
            r0 = new com.android.internal.os.BatteryStatsImpl$StopwatchTimer;
            r1 = r8.mBsi;
            r1 = r1.mClocks;
            r2 = r8.mBsi;
            r5 = r2.mOnBatteryTimeBase;
            r2 = r8;
            r0.<init>(r1, r2, r3, r4, r5);
            r6[r9] = r0;
        L_0x0034:
            return;
        L_0x0035:
            r7 = r8.mWifiBatchedScanTimer;
            r0 = new com.android.internal.os.BatteryStatsImpl$StopwatchTimer;
            r1 = r8.mBsi;
            r1 = r1.mClocks;
            r2 = r8.mBsi;
            r5 = r2.mOnBatteryTimeBase;
            r2 = r8;
            r6 = r10;
            r0.<init>(r1, r2, r3, r4, r5, r6);
            r7[r9] = r0;
            goto L_0x0034;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.makeWifiBatchedScanBin(int, android.os.Parcel):void");
        }

        void initUserActivityLocked() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r5 = this;
            r4 = 4;
            r1 = new com.android.internal.os.BatteryStatsImpl.Counter[r4];
            r5.mUserActivityCounters = r1;
            r0 = 0;
        L_0x0006:
            if (r0 >= r4) goto L_0x0018;
        L_0x0008:
            r1 = r5.mUserActivityCounters;
            r2 = new com.android.internal.os.BatteryStatsImpl$Counter;
            r3 = r5.mBsi;
            r3 = r3.mOnBatteryTimeBase;
            r2.<init>(r3);
            r1[r0] = r2;
            r0 = r0 + 1;
            goto L_0x0006;
        L_0x0018:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.initUserActivityLocked():void");
        }

        void noteNetworkActivityLocked(int r5, long r6, long r8) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r4 = this;
            r0 = r4.mNetworkByteActivityCounters;
            if (r0 != 0) goto L_0x0007;
        L_0x0004:
            r4.initNetworkActivityLocked();
        L_0x0007:
            if (r5 < 0) goto L_0x001c;
        L_0x0009:
            r0 = 10;
            if (r5 >= r0) goto L_0x001c;
        L_0x000d:
            r0 = r4.mNetworkByteActivityCounters;
            r0 = r0[r5];
            r0.addCountLocked(r6);
            r0 = r4.mNetworkPacketActivityCounters;
            r0 = r0[r5];
            r0.addCountLocked(r8);
        L_0x001b:
            return;
        L_0x001c:
            r0 = "BatteryStatsImpl";
            r1 = new java.lang.StringBuilder;
            r1.<init>();
            r2 = "Unknown network activity type ";
            r1 = r1.append(r2);
            r1 = r1.append(r5);
            r2 = " was specified.";
            r1 = r1.append(r2);
            r1 = r1.toString();
            r2 = new java.lang.Throwable;
            r2.<init>();
            android.util.Slog.w(r0, r1, r2);
            goto L_0x001b;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteNetworkActivityLocked(int, long, long):void");
        }

        void noteMobileRadioActiveTimeLocked(long r6) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r5 = this;
            r0 = r5.mNetworkByteActivityCounters;
            if (r0 != 0) goto L_0x0007;
        L_0x0004:
            r5.initNetworkActivityLocked();
        L_0x0007:
            r0 = r5.mMobileRadioActiveTime;
            r0.addCountLocked(r6);
            r0 = r5.mMobileRadioActiveCount;
            r2 = 1;
            r0.addCountLocked(r2);
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteMobileRadioActiveTimeLocked(long):void");
        }

        public boolean hasNetworkActivity() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mNetworkByteActivityCounters;
            if (r0 == 0) goto L_0x0006;
        L_0x0004:
            r0 = 1;
        L_0x0005:
            return r0;
        L_0x0006:
            r0 = 0;
            goto L_0x0005;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.hasNetworkActivity():boolean");
        }

        public long getNetworkActivityBytes(int r3, int r4) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r2 = this;
            r0 = r2.mNetworkByteActivityCounters;
            if (r0 == 0) goto L_0x0014;
        L_0x0004:
            if (r3 < 0) goto L_0x0014;
        L_0x0006:
            r0 = r2.mNetworkByteActivityCounters;
            r0 = r0.length;
            if (r3 >= r0) goto L_0x0014;
        L_0x000b:
            r0 = r2.mNetworkByteActivityCounters;
            r0 = r0[r3];
            r0 = r0.getCountLocked(r4);
            return r0;
        L_0x0014:
            r0 = 0;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getNetworkActivityBytes(int, int):long");
        }

        public long getNetworkActivityPackets(int r3, int r4) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r2 = this;
            r0 = r2.mNetworkPacketActivityCounters;
            if (r0 == 0) goto L_0x0014;
        L_0x0004:
            if (r3 < 0) goto L_0x0014;
        L_0x0006:
            r0 = r2.mNetworkPacketActivityCounters;
            r0 = r0.length;
            if (r3 >= r0) goto L_0x0014;
        L_0x000b:
            r0 = r2.mNetworkPacketActivityCounters;
            r0 = r0[r3];
            r0 = r0.getCountLocked(r4);
            return r0;
        L_0x0014:
            r0 = 0;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getNetworkActivityPackets(int, int):long");
        }

        public long getMobileRadioActiveTime(int r3) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r2 = this;
            r0 = r2.mMobileRadioActiveTime;
            if (r0 == 0) goto L_0x000b;
        L_0x0004:
            r0 = r2.mMobileRadioActiveTime;
            r0 = r0.getCountLocked(r3);
        L_0x000a:
            return r0;
        L_0x000b:
            r0 = 0;
            goto L_0x000a;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getMobileRadioActiveTime(int):long");
        }

        public int getMobileRadioActiveCount(int r3) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r2 = this;
            r0 = r2.mMobileRadioActiveCount;
            if (r0 == 0) goto L_0x000c;
        L_0x0004:
            r0 = r2.mMobileRadioActiveCount;
            r0 = r0.getCountLocked(r3);
            r0 = (int) r0;
        L_0x000b:
            return r0;
        L_0x000c:
            r0 = 0;
            goto L_0x000b;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getMobileRadioActiveCount(int):int");
        }

        public long getUserCpuTimeUs(int r3) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r2 = this;
            r0 = r2.mUserCpuTime;
            r0 = r0.getCountLocked(r3);
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getUserCpuTimeUs(int):long");
        }

        public long getSystemCpuTimeUs(int r3) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r2 = this;
            r0 = r2.mSystemCpuTime;
            r0 = r0.getCountLocked(r3);
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getSystemCpuTimeUs(int):long");
        }

        public long getTimeAtCpuSpeed(int r5, int r6, int r7) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r4 = this;
            r2 = r4.mCpuClusterSpeedTimesUs;
            if (r2 == 0) goto L_0x001f;
        L_0x0004:
            if (r5 < 0) goto L_0x001f;
        L_0x0006:
            r2 = r4.mCpuClusterSpeedTimesUs;
            r2 = r2.length;
            if (r5 >= r2) goto L_0x001f;
        L_0x000b:
            r2 = r4.mCpuClusterSpeedTimesUs;
            r1 = r2[r5];
            if (r1 == 0) goto L_0x001f;
        L_0x0011:
            if (r6 < 0) goto L_0x001f;
        L_0x0013:
            r2 = r1.length;
            if (r6 >= r2) goto L_0x001f;
        L_0x0016:
            r0 = r1[r6];
            if (r0 == 0) goto L_0x001f;
        L_0x001a:
            r2 = r0.getCountLocked(r7);
            return r2;
        L_0x001f:
            r2 = 0;
            return r2;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getTimeAtCpuSpeed(int, int, int):long");
        }

        public void noteMobileRadioApWakeupLocked() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r4 = this;
            r0 = r4.mMobileRadioApWakeupCount;
            if (r0 != 0) goto L_0x000f;
        L_0x0004:
            r0 = new com.android.internal.os.BatteryStatsImpl$LongSamplingCounter;
            r1 = r4.mBsi;
            r1 = r1.mOnBatteryTimeBase;
            r0.<init>(r1);
            r4.mMobileRadioApWakeupCount = r0;
        L_0x000f:
            r0 = r4.mMobileRadioApWakeupCount;
            r2 = 1;
            r0.addCountLocked(r2);
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteMobileRadioApWakeupLocked():void");
        }

        public long getMobileRadioApWakeupCount(int r3) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r2 = this;
            r0 = r2.mMobileRadioApWakeupCount;
            if (r0 == 0) goto L_0x000b;
        L_0x0004:
            r0 = r2.mMobileRadioApWakeupCount;
            r0 = r0.getCountLocked(r3);
            return r0;
        L_0x000b:
            r0 = 0;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getMobileRadioApWakeupCount(int):long");
        }

        public void noteWifiRadioApWakeupLocked() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r4 = this;
            r0 = r4.mWifiRadioApWakeupCount;
            if (r0 != 0) goto L_0x000f;
        L_0x0004:
            r0 = new com.android.internal.os.BatteryStatsImpl$LongSamplingCounter;
            r1 = r4.mBsi;
            r1 = r1.mOnBatteryTimeBase;
            r0.<init>(r1);
            r4.mWifiRadioApWakeupCount = r0;
        L_0x000f:
            r0 = r4.mWifiRadioApWakeupCount;
            r2 = 1;
            r0.addCountLocked(r2);
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteWifiRadioApWakeupLocked():void");
        }

        public long getWifiRadioApWakeupCount(int r3) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r2 = this;
            r0 = r2.mWifiRadioApWakeupCount;
            if (r0 == 0) goto L_0x000b;
        L_0x0004:
            r0 = r2.mWifiRadioApWakeupCount;
            r0 = r0.getCountLocked(r3);
            return r0;
        L_0x000b:
            r0 = 0;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getWifiRadioApWakeupCount(int):long");
        }

        void initNetworkActivityLocked() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r5 = this;
            r4 = 10;
            r1 = new com.android.internal.os.BatteryStatsImpl.LongSamplingCounter[r4];
            r5.mNetworkByteActivityCounters = r1;
            r1 = new com.android.internal.os.BatteryStatsImpl.LongSamplingCounter[r4];
            r5.mNetworkPacketActivityCounters = r1;
            r0 = 0;
        L_0x000b:
            if (r0 >= r4) goto L_0x002a;
        L_0x000d:
            r1 = r5.mNetworkByteActivityCounters;
            r2 = new com.android.internal.os.BatteryStatsImpl$LongSamplingCounter;
            r3 = r5.mBsi;
            r3 = r3.mOnBatteryTimeBase;
            r2.<init>(r3);
            r1[r0] = r2;
            r1 = r5.mNetworkPacketActivityCounters;
            r2 = new com.android.internal.os.BatteryStatsImpl$LongSamplingCounter;
            r3 = r5.mBsi;
            r3 = r3.mOnBatteryTimeBase;
            r2.<init>(r3);
            r1[r0] = r2;
            r0 = r0 + 1;
            goto L_0x000b;
        L_0x002a:
            r1 = new com.android.internal.os.BatteryStatsImpl$LongSamplingCounter;
            r2 = r5.mBsi;
            r2 = r2.mOnBatteryTimeBase;
            r1.<init>(r2);
            r5.mMobileRadioActiveTime = r1;
            r1 = new com.android.internal.os.BatteryStatsImpl$LongSamplingCounter;
            r2 = r5.mBsi;
            r2 = r2.mOnBatteryTimeBase;
            r1.<init>(r2);
            r5.mMobileRadioActiveCount = r1;
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.initNetworkActivityLocked():void");
        }

        public boolean reset(long r38, long r40) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r37 = this;
            r6 = 0;
            r0 = r37;
            r0 = r0.mOnBatteryBackgroundTimeBase;
            r30 = r0;
            r0 = r30;
            r1 = r38;
            r3 = r40;
            r0.init(r1, r3);
            r0 = r37;
            r0 = r0.mOnBatteryScreenOffBackgroundTimeBase;
            r30 = r0;
            r0 = r30;
            r1 = r38;
            r3 = r40;
            r0.init(r1, r3);
            r0 = r37;
            r0 = r0.mWifiRunningTimer;
            r30 = r0;
            if (r30 == 0) goto L_0x003d;
        L_0x0027:
            r0 = r37;
            r0 = r0.mWifiRunningTimer;
            r30 = r0;
            r31 = 0;
            r30 = r30.reset(r31);
            r6 = r30 ^ 1;
            r0 = r37;
            r0 = r0.mWifiRunning;
            r30 = r0;
            r6 = r6 | r30;
        L_0x003d:
            r0 = r37;
            r0 = r0.mFullWifiLockTimer;
            r30 = r0;
            if (r30 == 0) goto L_0x005d;
        L_0x0045:
            r0 = r37;
            r0 = r0.mFullWifiLockTimer;
            r30 = r0;
            r31 = 0;
            r30 = r30.reset(r31);
            r30 = r30 ^ 1;
            r6 = r6 | r30;
            r0 = r37;
            r0 = r0.mFullWifiLockOut;
            r30 = r0;
            r6 = r6 | r30;
        L_0x005d:
            r0 = r37;
            r0 = r0.mWifiScanTimer;
            r30 = r0;
            if (r30 == 0) goto L_0x007d;
        L_0x0065:
            r0 = r37;
            r0 = r0.mWifiScanTimer;
            r30 = r0;
            r31 = 0;
            r30 = r30.reset(r31);
            r30 = r30 ^ 1;
            r6 = r6 | r30;
            r0 = r37;
            r0 = r0.mWifiScanStarted;
            r30 = r0;
            r6 = r6 | r30;
        L_0x007d:
            r0 = r37;
            r0 = r0.mWifiBatchedScanTimer;
            r30 = r0;
            if (r30 == 0) goto L_0x00bd;
        L_0x0085:
            r9 = 0;
        L_0x0086:
            r30 = 5;
            r0 = r30;
            if (r9 >= r0) goto L_0x00ab;
        L_0x008c:
            r0 = r37;
            r0 = r0.mWifiBatchedScanTimer;
            r30 = r0;
            r30 = r30[r9];
            if (r30 == 0) goto L_0x00a8;
        L_0x0096:
            r0 = r37;
            r0 = r0.mWifiBatchedScanTimer;
            r30 = r0;
            r30 = r30[r9];
            r31 = 0;
            r30 = r30.reset(r31);
            r30 = r30 ^ 1;
            r6 = r6 | r30;
        L_0x00a8:
            r9 = r9 + 1;
            goto L_0x0086;
        L_0x00ab:
            r0 = r37;
            r0 = r0.mWifiBatchedScanBinStarted;
            r30 = r0;
            r31 = -1;
            r0 = r30;
            r1 = r31;
            if (r0 == r1) goto L_0x01c1;
        L_0x00b9:
            r30 = 1;
        L_0x00bb:
            r6 = r6 | r30;
        L_0x00bd:
            r0 = r37;
            r0 = r0.mWifiMulticastTimer;
            r30 = r0;
            if (r30 == 0) goto L_0x00dd;
        L_0x00c5:
            r0 = r37;
            r0 = r0.mWifiMulticastTimer;
            r30 = r0;
            r31 = 0;
            r30 = r30.reset(r31);
            r30 = r30 ^ 1;
            r6 = r6 | r30;
            r0 = r37;
            r0 = r0.mWifiMulticastEnabled;
            r30 = r0;
            r6 = r6 | r30;
        L_0x00dd:
            r0 = r37;
            r0 = r0.mAudioTurnedOnTimer;
            r30 = r0;
            r31 = 0;
            r30 = com.android.internal.os.BatteryStatsImpl.resetTimerIfNotNull(r30, r31);
            r30 = r30 ^ 1;
            r6 = r6 | r30;
            r0 = r37;
            r0 = r0.mVideoTurnedOnTimer;
            r30 = r0;
            r31 = 0;
            r30 = com.android.internal.os.BatteryStatsImpl.resetTimerIfNotNull(r30, r31);
            r30 = r30 ^ 1;
            r6 = r6 | r30;
            r0 = r37;
            r0 = r0.mFlashlightTurnedOnTimer;
            r30 = r0;
            r31 = 0;
            r30 = com.android.internal.os.BatteryStatsImpl.resetTimerIfNotNull(r30, r31);
            r30 = r30 ^ 1;
            r6 = r6 | r30;
            r0 = r37;
            r0 = r0.mCameraTurnedOnTimer;
            r30 = r0;
            r31 = 0;
            r30 = com.android.internal.os.BatteryStatsImpl.resetTimerIfNotNull(r30, r31);
            r30 = r30 ^ 1;
            r6 = r6 | r30;
            r0 = r37;
            r0 = r0.mForegroundActivityTimer;
            r30 = r0;
            r31 = 0;
            r30 = com.android.internal.os.BatteryStatsImpl.resetTimerIfNotNull(r30, r31);
            r30 = r30 ^ 1;
            r6 = r6 | r30;
            r0 = r37;
            r0 = r0.mForegroundServiceTimer;
            r30 = r0;
            r31 = 0;
            r30 = com.android.internal.os.BatteryStatsImpl.resetTimerIfNotNull(r30, r31);
            r30 = r30 ^ 1;
            r6 = r6 | r30;
            r0 = r37;
            r0 = r0.mAggregatedPartialWakelockTimer;
            r30 = r0;
            r31 = 0;
            r30 = com.android.internal.os.BatteryStatsImpl.resetTimerIfNotNull(r30, r31);
            r30 = r30 ^ 1;
            r6 = r6 | r30;
            r0 = r37;
            r0 = r0.mBluetoothScanTimer;
            r30 = r0;
            r31 = 0;
            r30 = com.android.internal.os.BatteryStatsImpl.resetTimerIfNotNull(r30, r31);
            r30 = r30 ^ 1;
            r6 = r6 | r30;
            r0 = r37;
            r0 = r0.mBluetoothUnoptimizedScanTimer;
            r30 = r0;
            r31 = 0;
            r30 = com.android.internal.os.BatteryStatsImpl.resetTimerIfNotNull(r30, r31);
            r30 = r30 ^ 1;
            r6 = r6 | r30;
            r0 = r37;
            r0 = r0.mBluetoothScanResultCounter;
            r30 = r0;
            if (r30 == 0) goto L_0x0180;
        L_0x0175:
            r0 = r37;
            r0 = r0.mBluetoothScanResultCounter;
            r30 = r0;
            r31 = 0;
            r30.reset(r31);
        L_0x0180:
            r0 = r37;
            r0 = r0.mBluetoothScanResultBgCounter;
            r30 = r0;
            if (r30 == 0) goto L_0x0193;
        L_0x0188:
            r0 = r37;
            r0 = r0.mBluetoothScanResultBgCounter;
            r30 = r0;
            r31 = 0;
            r30.reset(r31);
        L_0x0193:
            r0 = r37;
            r0 = r0.mProcessStateTimer;
            r30 = r0;
            if (r30 == 0) goto L_0x01d7;
        L_0x019b:
            r9 = 0;
        L_0x019c:
            r30 = 6;
            r0 = r30;
            if (r9 >= r0) goto L_0x01c5;
        L_0x01a2:
            r0 = r37;
            r0 = r0.mProcessStateTimer;
            r30 = r0;
            r30 = r30[r9];
            if (r30 == 0) goto L_0x01be;
        L_0x01ac:
            r0 = r37;
            r0 = r0.mProcessStateTimer;
            r30 = r0;
            r30 = r30[r9];
            r31 = 0;
            r30 = r30.reset(r31);
            r30 = r30 ^ 1;
            r6 = r6 | r30;
        L_0x01be:
            r9 = r9 + 1;
            goto L_0x019c;
        L_0x01c1:
            r30 = 0;
            goto L_0x00bb;
        L_0x01c5:
            r0 = r37;
            r0 = r0.mProcessState;
            r30 = r0;
            r31 = 18;
            r0 = r30;
            r1 = r31;
            if (r0 == r1) goto L_0x021d;
        L_0x01d3:
            r30 = 1;
        L_0x01d5:
            r6 = r6 | r30;
        L_0x01d7:
            r0 = r37;
            r0 = r0.mVibratorOnTimer;
            r30 = r0;
            if (r30 == 0) goto L_0x01fe;
        L_0x01df:
            r0 = r37;
            r0 = r0.mVibratorOnTimer;
            r30 = r0;
            r31 = 0;
            r30 = r30.reset(r31);
            if (r30 == 0) goto L_0x0220;
        L_0x01ed:
            r0 = r37;
            r0 = r0.mVibratorOnTimer;
            r30 = r0;
            r30.detach();
            r30 = 0;
            r0 = r30;
            r1 = r37;
            r1.mVibratorOnTimer = r0;
        L_0x01fe:
            r0 = r37;
            r0 = r0.mUserActivityCounters;
            r30 = r0;
            if (r30 == 0) goto L_0x0222;
        L_0x0206:
            r9 = 0;
        L_0x0207:
            r30 = 4;
            r0 = r30;
            if (r9 >= r0) goto L_0x0222;
        L_0x020d:
            r0 = r37;
            r0 = r0.mUserActivityCounters;
            r30 = r0;
            r30 = r30[r9];
            r31 = 0;
            r30.reset(r31);
            r9 = r9 + 1;
            goto L_0x0207;
        L_0x021d:
            r30 = 0;
            goto L_0x01d5;
        L_0x0220:
            r6 = 1;
            goto L_0x01fe;
        L_0x0222:
            r0 = r37;
            r0 = r0.mNetworkByteActivityCounters;
            r30 = r0;
            if (r30 == 0) goto L_0x0264;
        L_0x022a:
            r9 = 0;
        L_0x022b:
            r30 = 10;
            r0 = r30;
            if (r9 >= r0) goto L_0x024e;
        L_0x0231:
            r0 = r37;
            r0 = r0.mNetworkByteActivityCounters;
            r30 = r0;
            r30 = r30[r9];
            r31 = 0;
            r30.reset(r31);
            r0 = r37;
            r0 = r0.mNetworkPacketActivityCounters;
            r30 = r0;
            r30 = r30[r9];
            r31 = 0;
            r30.reset(r31);
            r9 = r9 + 1;
            goto L_0x022b;
        L_0x024e:
            r0 = r37;
            r0 = r0.mMobileRadioActiveTime;
            r30 = r0;
            r31 = 0;
            r30.reset(r31);
            r0 = r37;
            r0 = r0.mMobileRadioActiveCount;
            r30 = r0;
            r31 = 0;
            r30.reset(r31);
        L_0x0264:
            r0 = r37;
            r0 = r0.mWifiControllerActivity;
            r30 = r0;
            if (r30 == 0) goto L_0x0277;
        L_0x026c:
            r0 = r37;
            r0 = r0.mWifiControllerActivity;
            r30 = r0;
            r31 = 0;
            r30.reset(r31);
        L_0x0277:
            r0 = r37;
            r0 = r0.mBluetoothControllerActivity;
            r30 = r0;
            if (r30 == 0) goto L_0x028a;
        L_0x027f:
            r0 = r37;
            r0 = r0.mBluetoothControllerActivity;
            r30 = r0;
            r31 = 0;
            r30.reset(r31);
        L_0x028a:
            r0 = r37;
            r0 = r0.mModemControllerActivity;
            r30 = r0;
            if (r30 == 0) goto L_0x029d;
        L_0x0292:
            r0 = r37;
            r0 = r0.mModemControllerActivity;
            r30 = r0;
            r31 = 0;
            r30.reset(r31);
        L_0x029d:
            r0 = r37;
            r0 = r0.mUserCpuTime;
            r30 = r0;
            r31 = 0;
            r30.reset(r31);
            r0 = r37;
            r0 = r0.mSystemCpuTime;
            r30 = r0;
            r31 = 0;
            r30.reset(r31);
            r0 = r37;
            r0 = r0.mCpuClusterSpeedTimesUs;
            r30 = r0;
            if (r30 == 0) goto L_0x02f6;
        L_0x02bb:
            r0 = r37;
            r0 = r0.mCpuClusterSpeedTimesUs;
            r32 = r0;
            r30 = 0;
            r0 = r32;
            r0 = r0.length;
            r33 = r0;
            r31 = r30;
        L_0x02ca:
            r0 = r31;
            r1 = r33;
            if (r0 >= r1) goto L_0x02f6;
        L_0x02d0:
            r25 = r32[r31];
            if (r25 == 0) goto L_0x02f1;
        L_0x02d4:
            r30 = 0;
            r0 = r25;
            r0 = r0.length;
            r34 = r0;
        L_0x02db:
            r0 = r30;
            r1 = r34;
            if (r0 >= r1) goto L_0x02f1;
        L_0x02e1:
            r24 = r25[r30];
            if (r24 == 0) goto L_0x02ee;
        L_0x02e5:
            r35 = 0;
            r0 = r24;
            r1 = r35;
            r0.reset(r1);
        L_0x02ee:
            r30 = r30 + 1;
            goto L_0x02db;
        L_0x02f1:
            r30 = r31 + 1;
            r31 = r30;
            goto L_0x02ca;
        L_0x02f6:
            r0 = r37;
            r0 = r0.mCpuFreqTimeMs;
            r30 = r0;
            if (r30 == 0) goto L_0x0309;
        L_0x02fe:
            r0 = r37;
            r0 = r0.mCpuFreqTimeMs;
            r30 = r0;
            r31 = 0;
            r30.reset(r31);
        L_0x0309:
            r0 = r37;
            r0 = r0.mScreenOffCpuFreqTimeMs;
            r30 = r0;
            if (r30 == 0) goto L_0x031c;
        L_0x0311:
            r0 = r37;
            r0 = r0.mScreenOffCpuFreqTimeMs;
            r30 = r0;
            r31 = 0;
            r30.reset(r31);
        L_0x031c:
            r0 = r37;
            r0 = r0.mMobileRadioApWakeupCount;
            r30 = r0;
            r31 = 0;
            com.android.internal.os.BatteryStatsImpl.resetLongCounterIfNotNull(r30, r31);
            r0 = r37;
            r0 = r0.mWifiRadioApWakeupCount;
            r30 = r0;
            r31 = 0;
            com.android.internal.os.BatteryStatsImpl.resetLongCounterIfNotNull(r30, r31);
            r0 = r37;
            r0 = r0.mWakelockStats;
            r30 = r0;
            r28 = r30.getMap();
            r30 = r28.size();
            r16 = r30 + -1;
        L_0x0342:
            if (r16 < 0) goto L_0x0360;
        L_0x0344:
            r0 = r28;
            r1 = r16;
            r29 = r0.valueAt(r1);
            r29 = (com.android.internal.os.BatteryStatsImpl.Uid.Wakelock) r29;
            r30 = r29.reset();
            if (r30 == 0) goto L_0x035e;
        L_0x0354:
            r0 = r28;
            r1 = r16;
            r0.removeAt(r1);
        L_0x035b:
            r16 = r16 + -1;
            goto L_0x0342;
        L_0x035e:
            r6 = 1;
            goto L_0x035b;
        L_0x0360:
            r0 = r37;
            r0 = r0.mWakelockStats;
            r30 = r0;
            r30.cleanup();
            r0 = r37;
            r0 = r0.mSyncStats;
            r30 = r0;
            r26 = r30.getMap();
            r30 = r26.size();
            r12 = r30 + -1;
        L_0x0379:
            if (r12 < 0) goto L_0x039c;
        L_0x037b:
            r0 = r26;
            r27 = r0.valueAt(r12);
            r27 = (com.android.internal.os.BatteryStatsImpl.DualTimer) r27;
            r30 = 0;
            r0 = r27;
            r1 = r30;
            r30 = r0.reset(r1);
            if (r30 == 0) goto L_0x039a;
        L_0x038f:
            r0 = r26;
            r0.removeAt(r12);
            r27.detach();
        L_0x0397:
            r12 = r12 + -1;
            goto L_0x0379;
        L_0x039a:
            r6 = 1;
            goto L_0x0397;
        L_0x039c:
            r0 = r37;
            r0 = r0.mSyncStats;
            r30 = r0;
            r30.cleanup();
            r0 = r37;
            r0 = r0.mJobStats;
            r30 = r0;
            r17 = r30.getMap();
            r30 = r17.size();
            r10 = r30 + -1;
        L_0x03b5:
            if (r10 < 0) goto L_0x03d8;
        L_0x03b7:
            r0 = r17;
            r27 = r0.valueAt(r10);
            r27 = (com.android.internal.os.BatteryStatsImpl.DualTimer) r27;
            r30 = 0;
            r0 = r27;
            r1 = r30;
            r30 = r0.reset(r1);
            if (r30 == 0) goto L_0x03d6;
        L_0x03cb:
            r0 = r17;
            r0.removeAt(r10);
            r27.detach();
        L_0x03d3:
            r10 = r10 + -1;
            goto L_0x03b5;
        L_0x03d6:
            r6 = 1;
            goto L_0x03d3;
        L_0x03d8:
            r0 = r37;
            r0 = r0.mJobStats;
            r30 = r0;
            r30.cleanup();
            r0 = r37;
            r0 = r0.mJobCompletions;
            r30 = r0;
            r30.clear();
            r0 = r37;
            r0 = r0.mSensorStats;
            r30 = r0;
            r30 = r30.size();
            r13 = r30 + -1;
        L_0x03f6:
            if (r13 < 0) goto L_0x041c;
        L_0x03f8:
            r0 = r37;
            r0 = r0.mSensorStats;
            r30 = r0;
            r0 = r30;
            r22 = r0.valueAt(r13);
            r22 = (com.android.internal.os.BatteryStatsImpl.Uid.Sensor) r22;
            r30 = r22.reset();
            if (r30 == 0) goto L_0x041a;
        L_0x040c:
            r0 = r37;
            r0 = r0.mSensorStats;
            r30 = r0;
            r0 = r30;
            r0.removeAt(r13);
        L_0x0417:
            r13 = r13 + -1;
            goto L_0x03f6;
        L_0x041a:
            r6 = 1;
            goto L_0x0417;
        L_0x041c:
            r0 = r37;
            r0 = r0.mProcessStats;
            r30 = r0;
            r30 = r30.size();
            r11 = r30 + -1;
        L_0x0428:
            if (r11 < 0) goto L_0x043e;
        L_0x042a:
            r0 = r37;
            r0 = r0.mProcessStats;
            r30 = r0;
            r0 = r30;
            r21 = r0.valueAt(r11);
            r21 = (com.android.internal.os.BatteryStatsImpl.Uid.Proc) r21;
            r21.detach();
            r11 = r11 + -1;
            goto L_0x0428;
        L_0x043e:
            r0 = r37;
            r0 = r0.mProcessStats;
            r30 = r0;
            r30.clear();
            r0 = r37;
            r0 = r0.mPids;
            r30 = r0;
            r30 = r30.size();
            if (r30 <= 0) goto L_0x0487;
        L_0x0453:
            r0 = r37;
            r0 = r0.mPids;
            r30 = r0;
            r30 = r30.size();
            r9 = r30 + -1;
        L_0x045f:
            if (r9 < 0) goto L_0x0487;
        L_0x0461:
            r0 = r37;
            r0 = r0.mPids;
            r30 = r0;
            r0 = r30;
            r19 = r0.valueAt(r9);
            r19 = (android.os.BatteryStats.Uid.Pid) r19;
            r0 = r19;
            r0 = r0.mWakeNesting;
            r30 = r0;
            if (r30 <= 0) goto L_0x047b;
        L_0x0477:
            r6 = 1;
        L_0x0478:
            r9 = r9 + -1;
            goto L_0x045f;
        L_0x047b:
            r0 = r37;
            r0 = r0.mPids;
            r30 = r0;
            r0 = r30;
            r0.removeAt(r9);
            goto L_0x0478;
        L_0x0487:
            r0 = r37;
            r0 = r0.mPackageStats;
            r30 = r0;
            r30 = r30.size();
            if (r30 <= 0) goto L_0x04ef;
        L_0x0493:
            r0 = r37;
            r0 = r0.mPackageStats;
            r30 = r0;
            r30 = r30.entrySet();
            r14 = r30.iterator();
        L_0x04a1:
            r30 = r14.hasNext();
            if (r30 == 0) goto L_0x04e6;
        L_0x04a7:
            r20 = r14.next();
            r20 = (java.util.Map.Entry) r20;
            r18 = r20.getValue();
            r18 = (com.android.internal.os.BatteryStatsImpl.Uid.Pkg) r18;
            r18.detach();
            r0 = r18;
            r0 = r0.mServiceStats;
            r30 = r0;
            r30 = r30.size();
            if (r30 <= 0) goto L_0x04a1;
        L_0x04c2:
            r0 = r18;
            r0 = r0.mServiceStats;
            r30 = r0;
            r30 = r30.entrySet();
            r15 = r30.iterator();
        L_0x04d0:
            r30 = r15.hasNext();
            if (r30 == 0) goto L_0x04a1;
        L_0x04d6:
            r23 = r15.next();
            r23 = (java.util.Map.Entry) r23;
            r30 = r23.getValue();
            r30 = (com.android.internal.os.BatteryStatsImpl.Uid.Pkg.Serv) r30;
            r30.detach();
            goto L_0x04d0;
        L_0x04e6:
            r0 = r37;
            r0 = r0.mPackageStats;
            r30 = r0;
            r30.clear();
        L_0x04ef:
            r30 = 0;
            r0 = r30;
            r2 = r37;
            r2.mLastStepSystemTime = r0;
            r30 = 0;
            r0 = r30;
            r2 = r37;
            r2.mLastStepUserTime = r0;
            r30 = 0;
            r0 = r30;
            r2 = r37;
            r2.mCurStepSystemTime = r0;
            r30 = 0;
            r0 = r30;
            r2 = r37;
            r2.mCurStepUserTime = r0;
            if (r6 != 0) goto L_0x0789;
        L_0x0511:
            r0 = r37;
            r0 = r0.mWifiRunningTimer;
            r30 = r0;
            if (r30 == 0) goto L_0x0522;
        L_0x0519:
            r0 = r37;
            r0 = r0.mWifiRunningTimer;
            r30 = r0;
            r30.detach();
        L_0x0522:
            r0 = r37;
            r0 = r0.mFullWifiLockTimer;
            r30 = r0;
            if (r30 == 0) goto L_0x0533;
        L_0x052a:
            r0 = r37;
            r0 = r0.mFullWifiLockTimer;
            r30 = r0;
            r30.detach();
        L_0x0533:
            r0 = r37;
            r0 = r0.mWifiScanTimer;
            r30 = r0;
            if (r30 == 0) goto L_0x0544;
        L_0x053b:
            r0 = r37;
            r0 = r0.mWifiScanTimer;
            r30 = r0;
            r30.detach();
        L_0x0544:
            r9 = 0;
        L_0x0545:
            r30 = 5;
            r0 = r30;
            if (r9 >= r0) goto L_0x0563;
        L_0x054b:
            r0 = r37;
            r0 = r0.mWifiBatchedScanTimer;
            r30 = r0;
            r30 = r30[r9];
            if (r30 == 0) goto L_0x0560;
        L_0x0555:
            r0 = r37;
            r0 = r0.mWifiBatchedScanTimer;
            r30 = r0;
            r30 = r30[r9];
            r30.detach();
        L_0x0560:
            r9 = r9 + 1;
            goto L_0x0545;
        L_0x0563:
            r0 = r37;
            r0 = r0.mWifiMulticastTimer;
            r30 = r0;
            if (r30 == 0) goto L_0x0574;
        L_0x056b:
            r0 = r37;
            r0 = r0.mWifiMulticastTimer;
            r30 = r0;
            r30.detach();
        L_0x0574:
            r0 = r37;
            r0 = r0.mAudioTurnedOnTimer;
            r30 = r0;
            if (r30 == 0) goto L_0x058d;
        L_0x057c:
            r0 = r37;
            r0 = r0.mAudioTurnedOnTimer;
            r30 = r0;
            r30.detach();
            r30 = 0;
            r0 = r30;
            r1 = r37;
            r1.mAudioTurnedOnTimer = r0;
        L_0x058d:
            r0 = r37;
            r0 = r0.mVideoTurnedOnTimer;
            r30 = r0;
            if (r30 == 0) goto L_0x05a6;
        L_0x0595:
            r0 = r37;
            r0 = r0.mVideoTurnedOnTimer;
            r30 = r0;
            r30.detach();
            r30 = 0;
            r0 = r30;
            r1 = r37;
            r1.mVideoTurnedOnTimer = r0;
        L_0x05a6:
            r0 = r37;
            r0 = r0.mFlashlightTurnedOnTimer;
            r30 = r0;
            if (r30 == 0) goto L_0x05bf;
        L_0x05ae:
            r0 = r37;
            r0 = r0.mFlashlightTurnedOnTimer;
            r30 = r0;
            r30.detach();
            r30 = 0;
            r0 = r30;
            r1 = r37;
            r1.mFlashlightTurnedOnTimer = r0;
        L_0x05bf:
            r0 = r37;
            r0 = r0.mCameraTurnedOnTimer;
            r30 = r0;
            if (r30 == 0) goto L_0x05d8;
        L_0x05c7:
            r0 = r37;
            r0 = r0.mCameraTurnedOnTimer;
            r30 = r0;
            r30.detach();
            r30 = 0;
            r0 = r30;
            r1 = r37;
            r1.mCameraTurnedOnTimer = r0;
        L_0x05d8:
            r0 = r37;
            r0 = r0.mForegroundActivityTimer;
            r30 = r0;
            if (r30 == 0) goto L_0x05f1;
        L_0x05e0:
            r0 = r37;
            r0 = r0.mForegroundActivityTimer;
            r30 = r0;
            r30.detach();
            r30 = 0;
            r0 = r30;
            r1 = r37;
            r1.mForegroundActivityTimer = r0;
        L_0x05f1:
            r0 = r37;
            r0 = r0.mForegroundServiceTimer;
            r30 = r0;
            if (r30 == 0) goto L_0x060a;
        L_0x05f9:
            r0 = r37;
            r0 = r0.mForegroundServiceTimer;
            r30 = r0;
            r30.detach();
            r30 = 0;
            r0 = r30;
            r1 = r37;
            r1.mForegroundServiceTimer = r0;
        L_0x060a:
            r0 = r37;
            r0 = r0.mAggregatedPartialWakelockTimer;
            r30 = r0;
            if (r30 == 0) goto L_0x0623;
        L_0x0612:
            r0 = r37;
            r0 = r0.mAggregatedPartialWakelockTimer;
            r30 = r0;
            r30.detach();
            r30 = 0;
            r0 = r30;
            r1 = r37;
            r1.mAggregatedPartialWakelockTimer = r0;
        L_0x0623:
            r0 = r37;
            r0 = r0.mBluetoothScanTimer;
            r30 = r0;
            if (r30 == 0) goto L_0x063c;
        L_0x062b:
            r0 = r37;
            r0 = r0.mBluetoothScanTimer;
            r30 = r0;
            r30.detach();
            r30 = 0;
            r0 = r30;
            r1 = r37;
            r1.mBluetoothScanTimer = r0;
        L_0x063c:
            r0 = r37;
            r0 = r0.mBluetoothUnoptimizedScanTimer;
            r30 = r0;
            if (r30 == 0) goto L_0x0655;
        L_0x0644:
            r0 = r37;
            r0 = r0.mBluetoothUnoptimizedScanTimer;
            r30 = r0;
            r30.detach();
            r30 = 0;
            r0 = r30;
            r1 = r37;
            r1.mBluetoothUnoptimizedScanTimer = r0;
        L_0x0655:
            r0 = r37;
            r0 = r0.mBluetoothScanResultCounter;
            r30 = r0;
            if (r30 == 0) goto L_0x066e;
        L_0x065d:
            r0 = r37;
            r0 = r0.mBluetoothScanResultCounter;
            r30 = r0;
            r30.detach();
            r30 = 0;
            r0 = r30;
            r1 = r37;
            r1.mBluetoothScanResultCounter = r0;
        L_0x066e:
            r0 = r37;
            r0 = r0.mBluetoothScanResultBgCounter;
            r30 = r0;
            if (r30 == 0) goto L_0x0687;
        L_0x0676:
            r0 = r37;
            r0 = r0.mBluetoothScanResultBgCounter;
            r30 = r0;
            r30.detach();
            r30 = 0;
            r0 = r30;
            r1 = r37;
            r1.mBluetoothScanResultBgCounter = r0;
        L_0x0687:
            r0 = r37;
            r0 = r0.mUserActivityCounters;
            r30 = r0;
            if (r30 == 0) goto L_0x06a4;
        L_0x068f:
            r9 = 0;
        L_0x0690:
            r30 = 4;
            r0 = r30;
            if (r9 >= r0) goto L_0x06a4;
        L_0x0696:
            r0 = r37;
            r0 = r0.mUserActivityCounters;
            r30 = r0;
            r30 = r30[r9];
            r30.detach();
            r9 = r9 + 1;
            goto L_0x0690;
        L_0x06a4:
            r0 = r37;
            r0 = r0.mNetworkByteActivityCounters;
            r30 = r0;
            if (r30 == 0) goto L_0x06cc;
        L_0x06ac:
            r9 = 0;
        L_0x06ad:
            r30 = 10;
            r0 = r30;
            if (r9 >= r0) goto L_0x06cc;
        L_0x06b3:
            r0 = r37;
            r0 = r0.mNetworkByteActivityCounters;
            r30 = r0;
            r30 = r30[r9];
            r30.detach();
            r0 = r37;
            r0 = r0.mNetworkPacketActivityCounters;
            r30 = r0;
            r30 = r30[r9];
            r30.detach();
            r9 = r9 + 1;
            goto L_0x06ad;
        L_0x06cc:
            r0 = r37;
            r0 = r0.mWifiControllerActivity;
            r30 = r0;
            if (r30 == 0) goto L_0x06dd;
        L_0x06d4:
            r0 = r37;
            r0 = r0.mWifiControllerActivity;
            r30 = r0;
            r30.detach();
        L_0x06dd:
            r0 = r37;
            r0 = r0.mBluetoothControllerActivity;
            r30 = r0;
            if (r30 == 0) goto L_0x06ee;
        L_0x06e5:
            r0 = r37;
            r0 = r0.mBluetoothControllerActivity;
            r30 = r0;
            r30.detach();
        L_0x06ee:
            r0 = r37;
            r0 = r0.mModemControllerActivity;
            r30 = r0;
            if (r30 == 0) goto L_0x06ff;
        L_0x06f6:
            r0 = r37;
            r0 = r0.mModemControllerActivity;
            r30 = r0;
            r30.detach();
        L_0x06ff:
            r0 = r37;
            r0 = r0.mPids;
            r30 = r0;
            r30.clear();
            r0 = r37;
            r0 = r0.mUserCpuTime;
            r30 = r0;
            r30.detach();
            r0 = r37;
            r0 = r0.mSystemCpuTime;
            r30 = r0;
            r30.detach();
            r0 = r37;
            r0 = r0.mCpuClusterSpeedTimesUs;
            r30 = r0;
            if (r30 == 0) goto L_0x0755;
        L_0x0722:
            r0 = r37;
            r0 = r0.mCpuClusterSpeedTimesUs;
            r32 = r0;
            r30 = 0;
            r0 = r32;
            r0 = r0.length;
            r33 = r0;
            r31 = r30;
        L_0x0731:
            r0 = r31;
            r1 = r33;
            if (r0 >= r1) goto L_0x0755;
        L_0x0737:
            r8 = r32[r31];
            if (r8 == 0) goto L_0x0750;
        L_0x073b:
            r30 = 0;
            r0 = r8.length;
            r34 = r0;
        L_0x0740:
            r0 = r30;
            r1 = r34;
            if (r0 >= r1) goto L_0x0750;
        L_0x0746:
            r7 = r8[r30];
            if (r7 == 0) goto L_0x074d;
        L_0x074a:
            r7.detach();
        L_0x074d:
            r30 = r30 + 1;
            goto L_0x0740;
        L_0x0750:
            r30 = r31 + 1;
            r31 = r30;
            goto L_0x0731;
        L_0x0755:
            r0 = r37;
            r0 = r0.mCpuFreqTimeMs;
            r30 = r0;
            if (r30 == 0) goto L_0x0766;
        L_0x075d:
            r0 = r37;
            r0 = r0.mCpuFreqTimeMs;
            r30 = r0;
            r30.detach();
        L_0x0766:
            r0 = r37;
            r0 = r0.mScreenOffCpuFreqTimeMs;
            r30 = r0;
            if (r30 == 0) goto L_0x0777;
        L_0x076e:
            r0 = r37;
            r0 = r0.mScreenOffCpuFreqTimeMs;
            r30 = r0;
            r30.detach();
        L_0x0777:
            r0 = r37;
            r0 = r0.mMobileRadioApWakeupCount;
            r30 = r0;
            com.android.internal.os.BatteryStatsImpl.detachLongCounterIfNotNull(r30);
            r0 = r37;
            r0 = r0.mWifiRadioApWakeupCount;
            r30 = r0;
            com.android.internal.os.BatteryStatsImpl.detachLongCounterIfNotNull(r30);
        L_0x0789:
            r30 = r6 ^ 1;
            return r30;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.reset(long, long):boolean");
        }

        void writeJobCompletionsToParcelLocked(android.os.Parcel r7) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r6 = this;
            r5 = r6.mJobCompletions;
            r0 = r5.size();
            r7.writeInt(r0);
            r2 = 0;
        L_0x000a:
            if (r2 >= r0) goto L_0x003d;
        L_0x000c:
            r5 = r6.mJobCompletions;
            r5 = r5.keyAt(r2);
            r5 = (java.lang.String) r5;
            r7.writeString(r5);
            r5 = r6.mJobCompletions;
            r4 = r5.valueAt(r2);
            r4 = (android.util.SparseIntArray) r4;
            r1 = r4.size();
            r7.writeInt(r1);
            r3 = 0;
        L_0x0027:
            if (r3 >= r1) goto L_0x003a;
        L_0x0029:
            r5 = r4.keyAt(r3);
            r7.writeInt(r5);
            r5 = r4.valueAt(r3);
            r7.writeInt(r5);
            r3 = r3 + 1;
            goto L_0x0027;
        L_0x003a:
            r2 = r2 + 1;
            goto L_0x000a;
        L_0x003d:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.writeJobCompletionsToParcelLocked(android.os.Parcel):void");
        }

        void writeToParcelLocked(android.os.Parcel r35, long r36, long r38) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r34 = this;
            r0 = r34;
            r4 = r0.mOnBatteryBackgroundTimeBase;
            r5 = r35;
            r6 = r36;
            r8 = r38;
            r4.writeToParcel(r5, r6, r8);
            r0 = r34;
            r4 = r0.mOnBatteryScreenOffBackgroundTimeBase;
            r5 = r35;
            r6 = r36;
            r8 = r38;
            r4.writeToParcel(r5, r6, r8);
            r0 = r34;
            r4 = r0.mWakelockStats;
            r31 = r4.getMap();
            r14 = r31.size();
            r0 = r35;
            r0.writeInt(r14);
            r22 = 0;
        L_0x002d:
            r0 = r22;
            if (r0 >= r14) goto L_0x0056;
        L_0x0031:
            r0 = r31;
            r1 = r22;
            r4 = r0.keyAt(r1);
            r4 = (java.lang.String) r4;
            r0 = r35;
            r0.writeString(r4);
            r0 = r31;
            r1 = r22;
            r32 = r0.valueAt(r1);
            r32 = (com.android.internal.os.BatteryStatsImpl.Uid.Wakelock) r32;
            r0 = r32;
            r1 = r35;
            r2 = r38;
            r0.writeToParcelLocked(r1, r2);
            r22 = r22 + 1;
            goto L_0x002d;
        L_0x0056:
            r0 = r34;
            r4 = r0.mSyncStats;
            r29 = r4.getMap();
            r12 = r29.size();
            r0 = r35;
            r0.writeInt(r12);
            r20 = 0;
        L_0x0069:
            r0 = r20;
            if (r0 >= r12) goto L_0x0092;
        L_0x006d:
            r0 = r29;
            r1 = r20;
            r4 = r0.keyAt(r1);
            r4 = (java.lang.String) r4;
            r0 = r35;
            r0.writeString(r4);
            r0 = r29;
            r1 = r20;
            r30 = r0.valueAt(r1);
            r30 = (com.android.internal.os.BatteryStatsImpl.DualTimer) r30;
            r0 = r35;
            r1 = r30;
            r2 = r38;
            com.android.internal.os.BatteryStatsImpl.Timer.writeTimerToParcel(r0, r1, r2);
            r20 = r20 + 1;
            goto L_0x0069;
        L_0x0092:
            r0 = r34;
            r4 = r0.mJobStats;
            r23 = r4.getMap();
            r10 = r23.size();
            r0 = r35;
            r0.writeInt(r10);
            r18 = 0;
        L_0x00a5:
            r0 = r18;
            if (r0 >= r10) goto L_0x00ce;
        L_0x00a9:
            r0 = r23;
            r1 = r18;
            r4 = r0.keyAt(r1);
            r4 = (java.lang.String) r4;
            r0 = r35;
            r0.writeString(r4);
            r0 = r23;
            r1 = r18;
            r30 = r0.valueAt(r1);
            r30 = (com.android.internal.os.BatteryStatsImpl.DualTimer) r30;
            r0 = r35;
            r1 = r30;
            r2 = r38;
            com.android.internal.os.BatteryStatsImpl.Timer.writeTimerToParcel(r0, r1, r2);
            r18 = r18 + 1;
            goto L_0x00a5;
        L_0x00ce:
            r34.writeJobCompletionsToParcelLocked(r35);
            r0 = r34;
            r4 = r0.mSensorStats;
            r13 = r4.size();
            r0 = r35;
            r0.writeInt(r13);
            r21 = 0;
        L_0x00e0:
            r0 = r21;
            if (r0 >= r13) goto L_0x010b;
        L_0x00e4:
            r0 = r34;
            r4 = r0.mSensorStats;
            r0 = r21;
            r4 = r4.keyAt(r0);
            r0 = r35;
            r0.writeInt(r4);
            r0 = r34;
            r4 = r0.mSensorStats;
            r0 = r21;
            r28 = r4.valueAt(r0);
            r28 = (com.android.internal.os.BatteryStatsImpl.Uid.Sensor) r28;
            r0 = r28;
            r1 = r35;
            r2 = r38;
            r0.writeToParcelLocked(r1, r2);
            r21 = r21 + 1;
            goto L_0x00e0;
        L_0x010b:
            r0 = r34;
            r4 = r0.mProcessStats;
            r11 = r4.size();
            r0 = r35;
            r0.writeInt(r11);
            r19 = 0;
        L_0x011a:
            r0 = r19;
            if (r0 >= r11) goto L_0x0145;
        L_0x011e:
            r0 = r34;
            r4 = r0.mProcessStats;
            r0 = r19;
            r4 = r4.keyAt(r0);
            r4 = (java.lang.String) r4;
            r0 = r35;
            r0.writeString(r4);
            r0 = r34;
            r4 = r0.mProcessStats;
            r0 = r19;
            r27 = r4.valueAt(r0);
            r27 = (com.android.internal.os.BatteryStatsImpl.Uid.Proc) r27;
            r0 = r27;
            r1 = r35;
            r0.writeToParcelLocked(r1);
            r19 = r19 + 1;
            goto L_0x011a;
        L_0x0145:
            r0 = r34;
            r4 = r0.mPackageStats;
            r4 = r4.size();
            r0 = r35;
            r0.writeInt(r4);
            r0 = r34;
            r4 = r0.mPackageStats;
            r4 = r4.entrySet();
            r26 = r4.iterator();
        L_0x015e:
            r4 = r26.hasNext();
            if (r4 == 0) goto L_0x0183;
        L_0x0164:
            r25 = r26.next();
            r25 = (java.util.Map.Entry) r25;
            r4 = r25.getKey();
            r4 = (java.lang.String) r4;
            r0 = r35;
            r0.writeString(r4);
            r24 = r25.getValue();
            r24 = (com.android.internal.os.BatteryStatsImpl.Uid.Pkg) r24;
            r0 = r24;
            r1 = r35;
            r0.writeToParcelLocked(r1);
            goto L_0x015e;
        L_0x0183:
            r0 = r34;
            r4 = r0.mWifiRunningTimer;
            if (r4 == 0) goto L_0x01ed;
        L_0x0189:
            r4 = 1;
            r0 = r35;
            r0.writeInt(r4);
            r0 = r34;
            r4 = r0.mWifiRunningTimer;
            r0 = r35;
            r1 = r38;
            r4.writeToParcel(r0, r1);
        L_0x019a:
            r0 = r34;
            r4 = r0.mFullWifiLockTimer;
            if (r4 == 0) goto L_0x01f4;
        L_0x01a0:
            r4 = 1;
            r0 = r35;
            r0.writeInt(r4);
            r0 = r34;
            r4 = r0.mFullWifiLockTimer;
            r0 = r35;
            r1 = r38;
            r4.writeToParcel(r0, r1);
        L_0x01b1:
            r0 = r34;
            r4 = r0.mWifiScanTimer;
            if (r4 == 0) goto L_0x01fb;
        L_0x01b7:
            r4 = 1;
            r0 = r35;
            r0.writeInt(r4);
            r0 = r34;
            r4 = r0.mWifiScanTimer;
            r0 = r35;
            r1 = r38;
            r4.writeToParcel(r0, r1);
        L_0x01c8:
            r17 = 0;
        L_0x01ca:
            r4 = 5;
            r0 = r17;
            if (r0 >= r4) goto L_0x0209;
        L_0x01cf:
            r0 = r34;
            r4 = r0.mWifiBatchedScanTimer;
            r4 = r4[r17];
            if (r4 == 0) goto L_0x0202;
        L_0x01d7:
            r4 = 1;
            r0 = r35;
            r0.writeInt(r4);
            r0 = r34;
            r4 = r0.mWifiBatchedScanTimer;
            r4 = r4[r17];
            r0 = r35;
            r1 = r38;
            r4.writeToParcel(r0, r1);
        L_0x01ea:
            r17 = r17 + 1;
            goto L_0x01ca;
        L_0x01ed:
            r4 = 0;
            r0 = r35;
            r0.writeInt(r4);
            goto L_0x019a;
        L_0x01f4:
            r4 = 0;
            r0 = r35;
            r0.writeInt(r4);
            goto L_0x01b1;
        L_0x01fb:
            r4 = 0;
            r0 = r35;
            r0.writeInt(r4);
            goto L_0x01c8;
        L_0x0202:
            r4 = 0;
            r0 = r35;
            r0.writeInt(r4);
            goto L_0x01ea;
        L_0x0209:
            r0 = r34;
            r4 = r0.mWifiMulticastTimer;
            if (r4 == 0) goto L_0x033e;
        L_0x020f:
            r4 = 1;
            r0 = r35;
            r0.writeInt(r4);
            r0 = r34;
            r4 = r0.mWifiMulticastTimer;
            r0 = r35;
            r1 = r38;
            r4.writeToParcel(r0, r1);
        L_0x0220:
            r0 = r34;
            r4 = r0.mAudioTurnedOnTimer;
            if (r4 == 0) goto L_0x0346;
        L_0x0226:
            r4 = 1;
            r0 = r35;
            r0.writeInt(r4);
            r0 = r34;
            r4 = r0.mAudioTurnedOnTimer;
            r0 = r35;
            r1 = r38;
            r4.writeToParcel(r0, r1);
        L_0x0237:
            r0 = r34;
            r4 = r0.mVideoTurnedOnTimer;
            if (r4 == 0) goto L_0x034e;
        L_0x023d:
            r4 = 1;
            r0 = r35;
            r0.writeInt(r4);
            r0 = r34;
            r4 = r0.mVideoTurnedOnTimer;
            r0 = r35;
            r1 = r38;
            r4.writeToParcel(r0, r1);
        L_0x024e:
            r0 = r34;
            r4 = r0.mFlashlightTurnedOnTimer;
            if (r4 == 0) goto L_0x0356;
        L_0x0254:
            r4 = 1;
            r0 = r35;
            r0.writeInt(r4);
            r0 = r34;
            r4 = r0.mFlashlightTurnedOnTimer;
            r0 = r35;
            r1 = r38;
            r4.writeToParcel(r0, r1);
        L_0x0265:
            r0 = r34;
            r4 = r0.mCameraTurnedOnTimer;
            if (r4 == 0) goto L_0x035e;
        L_0x026b:
            r4 = 1;
            r0 = r35;
            r0.writeInt(r4);
            r0 = r34;
            r4 = r0.mCameraTurnedOnTimer;
            r0 = r35;
            r1 = r38;
            r4.writeToParcel(r0, r1);
        L_0x027c:
            r0 = r34;
            r4 = r0.mForegroundActivityTimer;
            if (r4 == 0) goto L_0x0366;
        L_0x0282:
            r4 = 1;
            r0 = r35;
            r0.writeInt(r4);
            r0 = r34;
            r4 = r0.mForegroundActivityTimer;
            r0 = r35;
            r1 = r38;
            r4.writeToParcel(r0, r1);
        L_0x0293:
            r0 = r34;
            r4 = r0.mForegroundServiceTimer;
            if (r4 == 0) goto L_0x036e;
        L_0x0299:
            r4 = 1;
            r0 = r35;
            r0.writeInt(r4);
            r0 = r34;
            r4 = r0.mForegroundServiceTimer;
            r0 = r35;
            r1 = r38;
            r4.writeToParcel(r0, r1);
        L_0x02aa:
            r0 = r34;
            r4 = r0.mAggregatedPartialWakelockTimer;
            if (r4 == 0) goto L_0x0376;
        L_0x02b0:
            r4 = 1;
            r0 = r35;
            r0.writeInt(r4);
            r0 = r34;
            r4 = r0.mAggregatedPartialWakelockTimer;
            r0 = r35;
            r1 = r38;
            r4.writeToParcel(r0, r1);
        L_0x02c1:
            r0 = r34;
            r4 = r0.mBluetoothScanTimer;
            if (r4 == 0) goto L_0x037e;
        L_0x02c7:
            r4 = 1;
            r0 = r35;
            r0.writeInt(r4);
            r0 = r34;
            r4 = r0.mBluetoothScanTimer;
            r0 = r35;
            r1 = r38;
            r4.writeToParcel(r0, r1);
        L_0x02d8:
            r0 = r34;
            r4 = r0.mBluetoothUnoptimizedScanTimer;
            if (r4 == 0) goto L_0x0386;
        L_0x02de:
            r4 = 1;
            r0 = r35;
            r0.writeInt(r4);
            r0 = r34;
            r4 = r0.mBluetoothUnoptimizedScanTimer;
            r0 = r35;
            r1 = r38;
            r4.writeToParcel(r0, r1);
        L_0x02ef:
            r0 = r34;
            r4 = r0.mBluetoothScanResultCounter;
            if (r4 == 0) goto L_0x038e;
        L_0x02f5:
            r4 = 1;
            r0 = r35;
            r0.writeInt(r4);
            r0 = r34;
            r4 = r0.mBluetoothScanResultCounter;
            r0 = r35;
            r4.writeToParcel(r0);
        L_0x0304:
            r0 = r34;
            r4 = r0.mBluetoothScanResultBgCounter;
            if (r4 == 0) goto L_0x0396;
        L_0x030a:
            r4 = 1;
            r0 = r35;
            r0.writeInt(r4);
            r0 = r34;
            r4 = r0.mBluetoothScanResultBgCounter;
            r0 = r35;
            r4.writeToParcel(r0);
        L_0x0319:
            r17 = 0;
        L_0x031b:
            r4 = 6;
            r0 = r17;
            if (r0 >= r4) goto L_0x03a5;
        L_0x0320:
            r0 = r34;
            r4 = r0.mProcessStateTimer;
            r4 = r4[r17];
            if (r4 == 0) goto L_0x039e;
        L_0x0328:
            r4 = 1;
            r0 = r35;
            r0.writeInt(r4);
            r0 = r34;
            r4 = r0.mProcessStateTimer;
            r4 = r4[r17];
            r0 = r35;
            r1 = r38;
            r4.writeToParcel(r0, r1);
        L_0x033b:
            r17 = r17 + 1;
            goto L_0x031b;
        L_0x033e:
            r4 = 0;
            r0 = r35;
            r0.writeInt(r4);
            goto L_0x0220;
        L_0x0346:
            r4 = 0;
            r0 = r35;
            r0.writeInt(r4);
            goto L_0x0237;
        L_0x034e:
            r4 = 0;
            r0 = r35;
            r0.writeInt(r4);
            goto L_0x024e;
        L_0x0356:
            r4 = 0;
            r0 = r35;
            r0.writeInt(r4);
            goto L_0x0265;
        L_0x035e:
            r4 = 0;
            r0 = r35;
            r0.writeInt(r4);
            goto L_0x027c;
        L_0x0366:
            r4 = 0;
            r0 = r35;
            r0.writeInt(r4);
            goto L_0x0293;
        L_0x036e:
            r4 = 0;
            r0 = r35;
            r0.writeInt(r4);
            goto L_0x02aa;
        L_0x0376:
            r4 = 0;
            r0 = r35;
            r0.writeInt(r4);
            goto L_0x02c1;
        L_0x037e:
            r4 = 0;
            r0 = r35;
            r0.writeInt(r4);
            goto L_0x02d8;
        L_0x0386:
            r4 = 0;
            r0 = r35;
            r0.writeInt(r4);
            goto L_0x02ef;
        L_0x038e:
            r4 = 0;
            r0 = r35;
            r0.writeInt(r4);
            goto L_0x0304;
        L_0x0396:
            r4 = 0;
            r0 = r35;
            r0.writeInt(r4);
            goto L_0x0319;
        L_0x039e:
            r4 = 0;
            r0 = r35;
            r0.writeInt(r4);
            goto L_0x033b;
        L_0x03a5:
            r0 = r34;
            r4 = r0.mVibratorOnTimer;
            if (r4 == 0) goto L_0x03dd;
        L_0x03ab:
            r4 = 1;
            r0 = r35;
            r0.writeInt(r4);
            r0 = r34;
            r4 = r0.mVibratorOnTimer;
            r0 = r35;
            r1 = r38;
            r4.writeToParcel(r0, r1);
        L_0x03bc:
            r0 = r34;
            r4 = r0.mUserActivityCounters;
            if (r4 == 0) goto L_0x03e4;
        L_0x03c2:
            r4 = 1;
            r0 = r35;
            r0.writeInt(r4);
            r17 = 0;
        L_0x03ca:
            r4 = 4;
            r0 = r17;
            if (r0 >= r4) goto L_0x03ea;
        L_0x03cf:
            r0 = r34;
            r4 = r0.mUserActivityCounters;
            r4 = r4[r17];
            r0 = r35;
            r4.writeToParcel(r0);
            r17 = r17 + 1;
            goto L_0x03ca;
        L_0x03dd:
            r4 = 0;
            r0 = r35;
            r0.writeInt(r4);
            goto L_0x03bc;
        L_0x03e4:
            r4 = 0;
            r0 = r35;
            r0.writeInt(r4);
        L_0x03ea:
            r0 = r34;
            r4 = r0.mNetworkByteActivityCounters;
            if (r4 == 0) goto L_0x04c6;
        L_0x03f0:
            r4 = 1;
            r0 = r35;
            r0.writeInt(r4);
            r17 = 0;
        L_0x03f8:
            r4 = 10;
            r0 = r17;
            if (r0 >= r4) goto L_0x0417;
        L_0x03fe:
            r0 = r34;
            r4 = r0.mNetworkByteActivityCounters;
            r4 = r4[r17];
            r0 = r35;
            r4.writeToParcel(r0);
            r0 = r34;
            r4 = r0.mNetworkPacketActivityCounters;
            r4 = r4[r17];
            r0 = r35;
            r4.writeToParcel(r0);
            r17 = r17 + 1;
            goto L_0x03f8;
        L_0x0417:
            r0 = r34;
            r4 = r0.mMobileRadioActiveTime;
            r0 = r35;
            r4.writeToParcel(r0);
            r0 = r34;
            r4 = r0.mMobileRadioActiveCount;
            r0 = r35;
            r4.writeToParcel(r0);
        L_0x0429:
            r0 = r34;
            r4 = r0.mWifiControllerActivity;
            if (r4 == 0) goto L_0x04ce;
        L_0x042f:
            r4 = 1;
            r0 = r35;
            r0.writeInt(r4);
            r0 = r34;
            r4 = r0.mWifiControllerActivity;
            r5 = 0;
            r0 = r35;
            r4.writeToParcel(r0, r5);
        L_0x043f:
            r0 = r34;
            r4 = r0.mBluetoothControllerActivity;
            if (r4 == 0) goto L_0x04d6;
        L_0x0445:
            r4 = 1;
            r0 = r35;
            r0.writeInt(r4);
            r0 = r34;
            r4 = r0.mBluetoothControllerActivity;
            r5 = 0;
            r0 = r35;
            r4.writeToParcel(r0, r5);
        L_0x0455:
            r0 = r34;
            r4 = r0.mModemControllerActivity;
            if (r4 == 0) goto L_0x04de;
        L_0x045b:
            r4 = 1;
            r0 = r35;
            r0.writeInt(r4);
            r0 = r34;
            r4 = r0.mModemControllerActivity;
            r5 = 0;
            r0 = r35;
            r4.writeToParcel(r0, r5);
        L_0x046b:
            r0 = r34;
            r4 = r0.mUserCpuTime;
            r0 = r35;
            r4.writeToParcel(r0);
            r0 = r34;
            r4 = r0.mSystemCpuTime;
            r0 = r35;
            r4.writeToParcel(r0);
            r0 = r34;
            r4 = r0.mCpuClusterSpeedTimesUs;
            if (r4 == 0) goto L_0x04f6;
        L_0x0483:
            r4 = 1;
            r0 = r35;
            r0.writeInt(r4);
            r0 = r34;
            r4 = r0.mCpuClusterSpeedTimesUs;
            r4 = r4.length;
            r0 = r35;
            r0.writeInt(r4);
            r0 = r34;
            r6 = r0.mCpuClusterSpeedTimesUs;
            r4 = 0;
            r7 = r6.length;
            r5 = r4;
        L_0x049a:
            if (r5 >= r7) goto L_0x04fc;
        L_0x049c:
            r16 = r6[r5];
            if (r16 == 0) goto L_0x04ec;
        L_0x04a0:
            r4 = 1;
            r0 = r35;
            r0.writeInt(r4);
            r0 = r16;
            r4 = r0.length;
            r0 = r35;
            r0.writeInt(r4);
            r4 = 0;
            r0 = r16;
            r8 = r0.length;
        L_0x04b2:
            if (r4 >= r8) goto L_0x04f2;
        L_0x04b4:
            r15 = r16[r4];
            if (r15 == 0) goto L_0x04e5;
        L_0x04b8:
            r9 = 1;
            r0 = r35;
            r0.writeInt(r9);
            r0 = r35;
            r15.writeToParcel(r0);
        L_0x04c3:
            r4 = r4 + 1;
            goto L_0x04b2;
        L_0x04c6:
            r4 = 0;
            r0 = r35;
            r0.writeInt(r4);
            goto L_0x0429;
        L_0x04ce:
            r4 = 0;
            r0 = r35;
            r0.writeInt(r4);
            goto L_0x043f;
        L_0x04d6:
            r4 = 0;
            r0 = r35;
            r0.writeInt(r4);
            goto L_0x0455;
        L_0x04de:
            r4 = 0;
            r0 = r35;
            r0.writeInt(r4);
            goto L_0x046b;
        L_0x04e5:
            r9 = 0;
            r0 = r35;
            r0.writeInt(r9);
            goto L_0x04c3;
        L_0x04ec:
            r4 = 0;
            r0 = r35;
            r0.writeInt(r4);
        L_0x04f2:
            r4 = r5 + 1;
            r5 = r4;
            goto L_0x049a;
        L_0x04f6:
            r4 = 0;
            r0 = r35;
            r0.writeInt(r4);
        L_0x04fc:
            r0 = r34;
            r4 = r0.mCpuFreqTimeMs;
            r0 = r35;
            com.android.internal.os.BatteryStatsImpl.LongSamplingCounterArray.writeToParcel(r0, r4);
            r0 = r34;
            r4 = r0.mScreenOffCpuFreqTimeMs;
            r0 = r35;
            com.android.internal.os.BatteryStatsImpl.LongSamplingCounterArray.writeToParcel(r0, r4);
            r0 = r34;
            r4 = r0.mMobileRadioApWakeupCount;
            if (r4 == 0) goto L_0x0539;
        L_0x0514:
            r4 = 1;
            r0 = r35;
            r0.writeInt(r4);
            r0 = r34;
            r4 = r0.mMobileRadioApWakeupCount;
            r0 = r35;
            r4.writeToParcel(r0);
        L_0x0523:
            r0 = r34;
            r4 = r0.mWifiRadioApWakeupCount;
            if (r4 == 0) goto L_0x0540;
        L_0x0529:
            r4 = 1;
            r0 = r35;
            r0.writeInt(r4);
            r0 = r34;
            r4 = r0.mWifiRadioApWakeupCount;
            r0 = r35;
            r4.writeToParcel(r0);
        L_0x0538:
            return;
        L_0x0539:
            r4 = 0;
            r0 = r35;
            r0.writeInt(r4);
            goto L_0x0523;
        L_0x0540:
            r4 = 0;
            r0 = r35;
            r0.writeInt(r4);
            goto L_0x0538;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.writeToParcelLocked(android.os.Parcel, long, long):void");
        }

        void readJobCompletionsFromParcelLocked(android.os.Parcel r10) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r9 = this;
            r4 = r10.readInt();
            r8 = r9.mJobCompletions;
            r8.clear();
            r1 = 0;
        L_0x000a:
            if (r1 >= r4) goto L_0x0034;
        L_0x000c:
            r2 = r10.readString();
            r5 = r10.readInt();
            if (r5 <= 0) goto L_0x0031;
        L_0x0016:
            r7 = new android.util.SparseIntArray;
            r7.<init>();
            r3 = 0;
        L_0x001c:
            if (r3 >= r5) goto L_0x002c;
        L_0x001e:
            r6 = r10.readInt();
            r0 = r10.readInt();
            r7.put(r6, r0);
            r3 = r3 + 1;
            goto L_0x001c;
        L_0x002c:
            r8 = r9.mJobCompletions;
            r8.put(r2, r7);
        L_0x0031:
            r1 = r1 + 1;
            goto L_0x000a;
        L_0x0034:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.readJobCompletionsFromParcelLocked(android.os.Parcel):void");
        }

        void readFromParcelLocked(com.android.internal.os.BatteryStatsImpl.TimeBase r39, com.android.internal.os.BatteryStatsImpl.TimeBase r40, android.os.Parcel r41) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r38 = this;
            r0 = r38;
            r4 = r0.mOnBatteryBackgroundTimeBase;
            r0 = r41;
            r4.readFromParcel(r0);
            r0 = r38;
            r4 = r0.mOnBatteryScreenOffBackgroundTimeBase;
            r0 = r41;
            r4.readFromParcel(r0);
            r26 = r41.readInt();
            r0 = r38;
            r4 = r0.mWakelockStats;
            r4.clear();
            r15 = 0;
        L_0x001e:
            r0 = r26;
            if (r15 >= r0) goto L_0x0050;
        L_0x0022:
            r36 = r41.readString();
            r35 = new com.android.internal.os.BatteryStatsImpl$Uid$Wakelock;
            r0 = r38;
            r4 = r0.mBsi;
            r0 = r35;
            r1 = r38;
            r0.<init>(r4, r1);
            r0 = r38;
            r4 = r0.mOnBatteryScreenOffBackgroundTimeBase;
            r0 = r35;
            r1 = r39;
            r2 = r40;
            r3 = r41;
            r0.readFromParcelLocked(r1, r2, r4, r3);
            r0 = r38;
            r4 = r0.mWakelockStats;
            r0 = r36;
            r1 = r35;
            r4.add(r0, r1);
            r15 = r15 + 1;
            goto L_0x001e;
        L_0x0050:
            r25 = r41.readInt();
            r0 = r38;
            r4 = r0.mSyncStats;
            r4.clear();
            r15 = 0;
        L_0x005c:
            r0 = r25;
            if (r15 >= r0) goto L_0x0096;
        L_0x0060:
            r34 = r41.readString();
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x0093;
        L_0x006a:
            r0 = r38;
            r0 = r0.mSyncStats;
            r37 = r0;
            r4 = new com.android.internal.os.BatteryStatsImpl$DualTimer;
            r0 = r38;
            r5 = r0.mBsi;
            r5 = r5.mClocks;
            r0 = r38;
            r6 = r0.mBsi;
            r9 = r6.mOnBatteryTimeBase;
            r0 = r38;
            r10 = r0.mOnBatteryBackgroundTimeBase;
            r7 = 13;
            r8 = 0;
            r6 = r38;
            r11 = r41;
            r4.<init>(r5, r6, r7, r8, r9, r10, r11);
            r0 = r37;
            r1 = r34;
            r0.add(r1, r4);
        L_0x0093:
            r15 = r15 + 1;
            goto L_0x005c;
        L_0x0096:
            r20 = r41.readInt();
            r0 = r38;
            r4 = r0.mJobStats;
            r4.clear();
            r15 = 0;
        L_0x00a2:
            r0 = r20;
            if (r15 >= r0) goto L_0x00dc;
        L_0x00a6:
            r16 = r41.readString();
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x00d9;
        L_0x00b0:
            r0 = r38;
            r0 = r0.mJobStats;
            r37 = r0;
            r4 = new com.android.internal.os.BatteryStatsImpl$DualTimer;
            r0 = r38;
            r5 = r0.mBsi;
            r5 = r5.mClocks;
            r0 = r38;
            r6 = r0.mBsi;
            r9 = r6.mOnBatteryTimeBase;
            r0 = r38;
            r10 = r0.mOnBatteryBackgroundTimeBase;
            r7 = 14;
            r8 = 0;
            r6 = r38;
            r11 = r41;
            r4.<init>(r5, r6, r7, r8, r9, r10, r11);
            r0 = r37;
            r1 = r16;
            r0.add(r1, r4);
        L_0x00d9:
            r15 = r15 + 1;
            goto L_0x00a2;
        L_0x00dc:
            r0 = r38;
            r1 = r41;
            r0.readJobCompletionsFromParcelLocked(r1);
            r23 = r41.readInt();
            r0 = r38;
            r4 = r0.mSensorStats;
            r4.clear();
            r17 = 0;
        L_0x00f0:
            r0 = r17;
            r1 = r23;
            if (r0 >= r1) goto L_0x0128;
        L_0x00f6:
            r32 = r41.readInt();
            r31 = new com.android.internal.os.BatteryStatsImpl$Uid$Sensor;
            r0 = r38;
            r4 = r0.mBsi;
            r0 = r31;
            r1 = r38;
            r2 = r32;
            r0.<init>(r4, r1, r2);
            r0 = r38;
            r4 = r0.mBsi;
            r4 = r4.mOnBatteryTimeBase;
            r0 = r38;
            r5 = r0.mOnBatteryBackgroundTimeBase;
            r0 = r31;
            r1 = r41;
            r0.readFromParcelLocked(r4, r5, r1);
            r0 = r38;
            r4 = r0.mSensorStats;
            r0 = r32;
            r1 = r31;
            r4.put(r0, r1);
            r17 = r17 + 1;
            goto L_0x00f0;
        L_0x0128:
            r22 = r41.readInt();
            r0 = r38;
            r4 = r0.mProcessStats;
            r4.clear();
            r17 = 0;
        L_0x0135:
            r0 = r17;
            r1 = r22;
            if (r0 >= r1) goto L_0x0161;
        L_0x013b:
            r30 = r41.readString();
            r29 = new com.android.internal.os.BatteryStatsImpl$Uid$Proc;
            r0 = r38;
            r4 = r0.mBsi;
            r0 = r29;
            r1 = r30;
            r0.<init>(r4, r1);
            r0 = r29;
            r1 = r41;
            r0.readFromParcelLocked(r1);
            r0 = r38;
            r4 = r0.mProcessStats;
            r0 = r30;
            r1 = r29;
            r4.put(r0, r1);
            r17 = r17 + 1;
            goto L_0x0135;
        L_0x0161:
            r21 = r41.readInt();
            r0 = r38;
            r4 = r0.mPackageStats;
            r4.clear();
            r18 = 0;
        L_0x016e:
            r0 = r18;
            r1 = r21;
            if (r0 >= r1) goto L_0x0198;
        L_0x0174:
            r27 = r41.readString();
            r28 = new com.android.internal.os.BatteryStatsImpl$Uid$Pkg;
            r0 = r38;
            r4 = r0.mBsi;
            r0 = r28;
            r0.<init>(r4);
            r0 = r28;
            r1 = r41;
            r0.readFromParcelLocked(r1);
            r0 = r38;
            r4 = r0.mPackageStats;
            r0 = r27;
            r1 = r28;
            r4.put(r0, r1);
            r18 = r18 + 1;
            goto L_0x016e;
        L_0x0198:
            r4 = 0;
            r0 = r38;
            r0.mWifiRunning = r4;
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x0236;
        L_0x01a3:
            r4 = new com.android.internal.os.BatteryStatsImpl$StopwatchTimer;
            r0 = r38;
            r5 = r0.mBsi;
            r5 = r5.mClocks;
            r0 = r38;
            r6 = r0.mBsi;
            r8 = r6.mWifiRunningTimers;
            r0 = r38;
            r6 = r0.mBsi;
            r9 = r6.mOnBatteryTimeBase;
            r7 = 4;
            r6 = r38;
            r10 = r41;
            r4.<init>(r5, r6, r7, r8, r9, r10);
            r0 = r38;
            r0.mWifiRunningTimer = r4;
        L_0x01c3:
            r4 = 0;
            r0 = r38;
            r0.mFullWifiLockOut = r4;
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x023c;
        L_0x01ce:
            r4 = new com.android.internal.os.BatteryStatsImpl$StopwatchTimer;
            r0 = r38;
            r5 = r0.mBsi;
            r5 = r5.mClocks;
            r0 = r38;
            r6 = r0.mBsi;
            r8 = r6.mFullWifiLockTimers;
            r0 = r38;
            r6 = r0.mBsi;
            r9 = r6.mOnBatteryTimeBase;
            r7 = 5;
            r6 = r38;
            r10 = r41;
            r4.<init>(r5, r6, r7, r8, r9, r10);
            r0 = r38;
            r0.mFullWifiLockTimer = r4;
        L_0x01ee:
            r4 = 0;
            r0 = r38;
            r0.mWifiScanStarted = r4;
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x0242;
        L_0x01f9:
            r4 = new com.android.internal.os.BatteryStatsImpl$DualTimer;
            r0 = r38;
            r5 = r0.mBsi;
            r5 = r5.mClocks;
            r0 = r38;
            r6 = r0.mBsi;
            r8 = r6.mWifiScanTimers;
            r0 = r38;
            r6 = r0.mBsi;
            r9 = r6.mOnBatteryTimeBase;
            r0 = r38;
            r10 = r0.mOnBatteryBackgroundTimeBase;
            r7 = 6;
            r6 = r38;
            r11 = r41;
            r4.<init>(r5, r6, r7, r8, r9, r10, r11);
            r0 = r38;
            r0.mWifiScanTimer = r4;
        L_0x021d:
            r4 = -1;
            r0 = r38;
            r0.mWifiBatchedScanBinStarted = r4;
            r14 = 0;
        L_0x0223:
            r4 = 5;
            if (r14 >= r4) goto L_0x0250;
        L_0x0226:
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x0248;
        L_0x022c:
            r0 = r38;
            r1 = r41;
            r0.makeWifiBatchedScanBin(r14, r1);
        L_0x0233:
            r14 = r14 + 1;
            goto L_0x0223;
        L_0x0236:
            r4 = 0;
            r0 = r38;
            r0.mWifiRunningTimer = r4;
            goto L_0x01c3;
        L_0x023c:
            r4 = 0;
            r0 = r38;
            r0.mFullWifiLockTimer = r4;
            goto L_0x01ee;
        L_0x0242:
            r4 = 0;
            r0 = r38;
            r0.mWifiScanTimer = r4;
            goto L_0x021d;
        L_0x0248:
            r0 = r38;
            r4 = r0.mWifiBatchedScanTimer;
            r5 = 0;
            r4[r14] = r5;
            goto L_0x0233;
        L_0x0250:
            r4 = 0;
            r0 = r38;
            r0.mWifiMulticastEnabled = r4;
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x0418;
        L_0x025b:
            r4 = new com.android.internal.os.BatteryStatsImpl$StopwatchTimer;
            r0 = r38;
            r5 = r0.mBsi;
            r5 = r5.mClocks;
            r0 = r38;
            r6 = r0.mBsi;
            r8 = r6.mWifiMulticastTimers;
            r0 = r38;
            r6 = r0.mBsi;
            r9 = r6.mOnBatteryTimeBase;
            r7 = 7;
            r6 = r38;
            r10 = r41;
            r4.<init>(r5, r6, r7, r8, r9, r10);
            r0 = r38;
            r0.mWifiMulticastTimer = r4;
        L_0x027b:
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x041f;
        L_0x0281:
            r4 = new com.android.internal.os.BatteryStatsImpl$StopwatchTimer;
            r0 = r38;
            r5 = r0.mBsi;
            r5 = r5.mClocks;
            r0 = r38;
            r6 = r0.mBsi;
            r8 = r6.mAudioTurnedOnTimers;
            r0 = r38;
            r6 = r0.mBsi;
            r9 = r6.mOnBatteryTimeBase;
            r7 = 15;
            r6 = r38;
            r10 = r41;
            r4.<init>(r5, r6, r7, r8, r9, r10);
            r0 = r38;
            r0.mAudioTurnedOnTimer = r4;
        L_0x02a2:
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x0426;
        L_0x02a8:
            r4 = new com.android.internal.os.BatteryStatsImpl$StopwatchTimer;
            r0 = r38;
            r5 = r0.mBsi;
            r5 = r5.mClocks;
            r0 = r38;
            r6 = r0.mBsi;
            r8 = r6.mVideoTurnedOnTimers;
            r0 = r38;
            r6 = r0.mBsi;
            r9 = r6.mOnBatteryTimeBase;
            r7 = 8;
            r6 = r38;
            r10 = r41;
            r4.<init>(r5, r6, r7, r8, r9, r10);
            r0 = r38;
            r0.mVideoTurnedOnTimer = r4;
        L_0x02c9:
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x042d;
        L_0x02cf:
            r4 = new com.android.internal.os.BatteryStatsImpl$StopwatchTimer;
            r0 = r38;
            r5 = r0.mBsi;
            r5 = r5.mClocks;
            r0 = r38;
            r6 = r0.mBsi;
            r8 = r6.mFlashlightTurnedOnTimers;
            r0 = r38;
            r6 = r0.mBsi;
            r9 = r6.mOnBatteryTimeBase;
            r7 = 16;
            r6 = r38;
            r10 = r41;
            r4.<init>(r5, r6, r7, r8, r9, r10);
            r0 = r38;
            r0.mFlashlightTurnedOnTimer = r4;
        L_0x02f0:
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x0434;
        L_0x02f6:
            r4 = new com.android.internal.os.BatteryStatsImpl$StopwatchTimer;
            r0 = r38;
            r5 = r0.mBsi;
            r5 = r5.mClocks;
            r0 = r38;
            r6 = r0.mBsi;
            r8 = r6.mCameraTurnedOnTimers;
            r0 = r38;
            r6 = r0.mBsi;
            r9 = r6.mOnBatteryTimeBase;
            r7 = 17;
            r6 = r38;
            r10 = r41;
            r4.<init>(r5, r6, r7, r8, r9, r10);
            r0 = r38;
            r0.mCameraTurnedOnTimer = r4;
        L_0x0317:
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x043b;
        L_0x031d:
            r4 = new com.android.internal.os.BatteryStatsImpl$StopwatchTimer;
            r0 = r38;
            r5 = r0.mBsi;
            r5 = r5.mClocks;
            r0 = r38;
            r6 = r0.mBsi;
            r9 = r6.mOnBatteryTimeBase;
            r7 = 10;
            r8 = 0;
            r6 = r38;
            r10 = r41;
            r4.<init>(r5, r6, r7, r8, r9, r10);
            r0 = r38;
            r0.mForegroundActivityTimer = r4;
        L_0x0339:
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x0442;
        L_0x033f:
            r4 = new com.android.internal.os.BatteryStatsImpl$StopwatchTimer;
            r0 = r38;
            r5 = r0.mBsi;
            r5 = r5.mClocks;
            r0 = r38;
            r6 = r0.mBsi;
            r9 = r6.mOnBatteryTimeBase;
            r7 = 22;
            r8 = 0;
            r6 = r38;
            r10 = r41;
            r4.<init>(r5, r6, r7, r8, r9, r10);
            r0 = r38;
            r0.mForegroundServiceTimer = r4;
        L_0x035b:
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x0449;
        L_0x0361:
            r4 = new com.android.internal.os.BatteryStatsImpl$DualTimer;
            r0 = r38;
            r5 = r0.mBsi;
            r5 = r5.mClocks;
            r0 = r38;
            r6 = r0.mBsi;
            r9 = r6.mOnBatteryScreenOffTimeBase;
            r0 = r38;
            r10 = r0.mOnBatteryScreenOffBackgroundTimeBase;
            r7 = 20;
            r8 = 0;
            r6 = r38;
            r11 = r41;
            r4.<init>(r5, r6, r7, r8, r9, r10, r11);
            r0 = r38;
            r0.mAggregatedPartialWakelockTimer = r4;
        L_0x0381:
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x0450;
        L_0x0387:
            r4 = new com.android.internal.os.BatteryStatsImpl$DualTimer;
            r0 = r38;
            r5 = r0.mBsi;
            r5 = r5.mClocks;
            r0 = r38;
            r6 = r0.mBsi;
            r8 = r6.mBluetoothScanOnTimers;
            r0 = r38;
            r6 = r0.mBsi;
            r9 = r6.mOnBatteryTimeBase;
            r0 = r38;
            r10 = r0.mOnBatteryBackgroundTimeBase;
            r7 = 19;
            r6 = r38;
            r11 = r41;
            r4.<init>(r5, r6, r7, r8, r9, r10, r11);
            r0 = r38;
            r0.mBluetoothScanTimer = r4;
        L_0x03ac:
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x0457;
        L_0x03b2:
            r4 = new com.android.internal.os.BatteryStatsImpl$DualTimer;
            r0 = r38;
            r5 = r0.mBsi;
            r5 = r5.mClocks;
            r0 = r38;
            r6 = r0.mBsi;
            r9 = r6.mOnBatteryTimeBase;
            r0 = r38;
            r10 = r0.mOnBatteryBackgroundTimeBase;
            r7 = 21;
            r8 = 0;
            r6 = r38;
            r11 = r41;
            r4.<init>(r5, r6, r7, r8, r9, r10, r11);
            r0 = r38;
            r0.mBluetoothUnoptimizedScanTimer = r4;
        L_0x03d2:
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x045e;
        L_0x03d8:
            r4 = new com.android.internal.os.BatteryStatsImpl$Counter;
            r0 = r38;
            r5 = r0.mBsi;
            r5 = r5.mOnBatteryTimeBase;
            r0 = r41;
            r4.<init>(r5, r0);
            r0 = r38;
            r0.mBluetoothScanResultCounter = r4;
        L_0x03e9:
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x0464;
        L_0x03ef:
            r4 = new com.android.internal.os.BatteryStatsImpl$Counter;
            r0 = r38;
            r5 = r0.mOnBatteryBackgroundTimeBase;
            r0 = r41;
            r4.<init>(r5, r0);
            r0 = r38;
            r0.mBluetoothScanResultBgCounter = r4;
        L_0x03fe:
            r4 = 18;
            r0 = r38;
            r0.mProcessState = r4;
            r14 = 0;
        L_0x0405:
            r4 = 6;
            if (r14 >= r4) goto L_0x0472;
        L_0x0408:
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x046a;
        L_0x040e:
            r0 = r38;
            r1 = r41;
            r0.makeProcessState(r14, r1);
        L_0x0415:
            r14 = r14 + 1;
            goto L_0x0405;
        L_0x0418:
            r4 = 0;
            r0 = r38;
            r0.mWifiMulticastTimer = r4;
            goto L_0x027b;
        L_0x041f:
            r4 = 0;
            r0 = r38;
            r0.mAudioTurnedOnTimer = r4;
            goto L_0x02a2;
        L_0x0426:
            r4 = 0;
            r0 = r38;
            r0.mVideoTurnedOnTimer = r4;
            goto L_0x02c9;
        L_0x042d:
            r4 = 0;
            r0 = r38;
            r0.mFlashlightTurnedOnTimer = r4;
            goto L_0x02f0;
        L_0x0434:
            r4 = 0;
            r0 = r38;
            r0.mCameraTurnedOnTimer = r4;
            goto L_0x0317;
        L_0x043b:
            r4 = 0;
            r0 = r38;
            r0.mForegroundActivityTimer = r4;
            goto L_0x0339;
        L_0x0442:
            r4 = 0;
            r0 = r38;
            r0.mForegroundServiceTimer = r4;
            goto L_0x035b;
        L_0x0449:
            r4 = 0;
            r0 = r38;
            r0.mAggregatedPartialWakelockTimer = r4;
            goto L_0x0381;
        L_0x0450:
            r4 = 0;
            r0 = r38;
            r0.mBluetoothScanTimer = r4;
            goto L_0x03ac;
        L_0x0457:
            r4 = 0;
            r0 = r38;
            r0.mBluetoothUnoptimizedScanTimer = r4;
            goto L_0x03d2;
        L_0x045e:
            r4 = 0;
            r0 = r38;
            r0.mBluetoothScanResultCounter = r4;
            goto L_0x03e9;
        L_0x0464:
            r4 = 0;
            r0 = r38;
            r0.mBluetoothScanResultBgCounter = r4;
            goto L_0x03fe;
        L_0x046a:
            r0 = r38;
            r4 = r0.mProcessStateTimer;
            r5 = 0;
            r4[r14] = r5;
            goto L_0x0415;
        L_0x0472:
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x04ba;
        L_0x0478:
            r4 = new com.android.internal.os.BatteryStatsImpl$BatchTimer;
            r0 = r38;
            r5 = r0.mBsi;
            r5 = r5.mClocks;
            r0 = r38;
            r6 = r0.mBsi;
            r8 = r6.mOnBatteryTimeBase;
            r7 = 9;
            r6 = r38;
            r9 = r41;
            r4.<init>(r5, r6, r7, r8, r9);
            r0 = r38;
            r0.mVibratorOnTimer = r4;
        L_0x0493:
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x04c0;
        L_0x0499:
            r4 = 4;
            r4 = new com.android.internal.os.BatteryStatsImpl.Counter[r4];
            r0 = r38;
            r0.mUserActivityCounters = r4;
            r14 = 0;
        L_0x04a1:
            r4 = 4;
            if (r14 >= r4) goto L_0x04c5;
        L_0x04a4:
            r0 = r38;
            r4 = r0.mUserActivityCounters;
            r5 = new com.android.internal.os.BatteryStatsImpl$Counter;
            r0 = r38;
            r6 = r0.mBsi;
            r6 = r6.mOnBatteryTimeBase;
            r0 = r41;
            r5.<init>(r6, r0);
            r4[r14] = r5;
            r14 = r14 + 1;
            goto L_0x04a1;
        L_0x04ba:
            r4 = 0;
            r0 = r38;
            r0.mVibratorOnTimer = r4;
            goto L_0x0493;
        L_0x04c0:
            r4 = 0;
            r0 = r38;
            r0.mUserActivityCounters = r4;
        L_0x04c5:
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x05c2;
        L_0x04cb:
            r4 = 10;
            r4 = new com.android.internal.os.BatteryStatsImpl.LongSamplingCounter[r4];
            r0 = r38;
            r0.mNetworkByteActivityCounters = r4;
            r4 = 10;
            r4 = new com.android.internal.os.BatteryStatsImpl.LongSamplingCounter[r4];
            r0 = r38;
            r0.mNetworkPacketActivityCounters = r4;
            r14 = 0;
        L_0x04dc:
            r4 = 10;
            if (r14 >= r4) goto L_0x0509;
        L_0x04e0:
            r0 = r38;
            r4 = r0.mNetworkByteActivityCounters;
            r5 = new com.android.internal.os.BatteryStatsImpl$LongSamplingCounter;
            r0 = r38;
            r6 = r0.mBsi;
            r6 = r6.mOnBatteryTimeBase;
            r0 = r41;
            r5.<init>(r6, r0);
            r4[r14] = r5;
            r0 = r38;
            r4 = r0.mNetworkPacketActivityCounters;
            r5 = new com.android.internal.os.BatteryStatsImpl$LongSamplingCounter;
            r0 = r38;
            r6 = r0.mBsi;
            r6 = r6.mOnBatteryTimeBase;
            r0 = r41;
            r5.<init>(r6, r0);
            r4[r14] = r5;
            r14 = r14 + 1;
            goto L_0x04dc;
        L_0x0509:
            r4 = new com.android.internal.os.BatteryStatsImpl$LongSamplingCounter;
            r0 = r38;
            r5 = r0.mBsi;
            r5 = r5.mOnBatteryTimeBase;
            r0 = r41;
            r4.<init>(r5, r0);
            r0 = r38;
            r0.mMobileRadioActiveTime = r4;
            r4 = new com.android.internal.os.BatteryStatsImpl$LongSamplingCounter;
            r0 = r38;
            r5 = r0.mBsi;
            r5 = r5.mOnBatteryTimeBase;
            r0 = r41;
            r4.<init>(r5, r0);
            r0 = r38;
            r0.mMobileRadioActiveCount = r4;
        L_0x052b:
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x05ce;
        L_0x0531:
            r4 = new com.android.internal.os.BatteryStatsImpl$ControllerActivityCounterImpl;
            r0 = r38;
            r5 = r0.mBsi;
            r5 = r5.mOnBatteryTimeBase;
            r6 = 1;
            r0 = r41;
            r4.<init>(r5, r6, r0);
            r0 = r38;
            r0.mWifiControllerActivity = r4;
        L_0x0543:
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x05d5;
        L_0x0549:
            r4 = new com.android.internal.os.BatteryStatsImpl$ControllerActivityCounterImpl;
            r0 = r38;
            r5 = r0.mBsi;
            r5 = r5.mOnBatteryTimeBase;
            r6 = 1;
            r0 = r41;
            r4.<init>(r5, r6, r0);
            r0 = r38;
            r0.mBluetoothControllerActivity = r4;
        L_0x055b:
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x05db;
        L_0x0561:
            r4 = new com.android.internal.os.BatteryStatsImpl$ControllerActivityCounterImpl;
            r0 = r38;
            r5 = r0.mBsi;
            r5 = r5.mOnBatteryTimeBase;
            r6 = 5;
            r0 = r41;
            r4.<init>(r5, r6, r0);
            r0 = r38;
            r0.mModemControllerActivity = r4;
        L_0x0573:
            r4 = new com.android.internal.os.BatteryStatsImpl$LongSamplingCounter;
            r0 = r38;
            r5 = r0.mBsi;
            r5 = r5.mOnBatteryTimeBase;
            r0 = r41;
            r4.<init>(r5, r0);
            r0 = r38;
            r0.mUserCpuTime = r4;
            r4 = new com.android.internal.os.BatteryStatsImpl$LongSamplingCounter;
            r0 = r38;
            r5 = r0.mBsi;
            r5 = r5.mOnBatteryTimeBase;
            r0 = r41;
            r4.<init>(r5, r0);
            r0 = r38;
            r0.mSystemCpuTime = r4;
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x064f;
        L_0x059b:
            r19 = r41.readInt();
            r0 = r38;
            r4 = r0.mBsi;
            r4 = r4.mPowerProfile;
            if (r4 == 0) goto L_0x05e1;
        L_0x05a9:
            r0 = r38;
            r4 = r0.mBsi;
            r4 = r4.mPowerProfile;
            r4 = r4.getNumCpuClusters();
            r0 = r19;
            if (r4 == r0) goto L_0x05e1;
        L_0x05b9:
            r4 = new android.os.ParcelFormatException;
            r5 = "Incompatible number of cpu clusters";
            r4.<init>(r5);
            throw r4;
        L_0x05c2:
            r4 = 0;
            r0 = r38;
            r0.mNetworkByteActivityCounters = r4;
            r4 = 0;
            r0 = r38;
            r0.mNetworkPacketActivityCounters = r4;
            goto L_0x052b;
        L_0x05ce:
            r4 = 0;
            r0 = r38;
            r0.mWifiControllerActivity = r4;
            goto L_0x0543;
        L_0x05d5:
            r4 = 0;
            r0 = r38;
            r0.mBluetoothControllerActivity = r4;
            goto L_0x055b;
        L_0x05db:
            r4 = 0;
            r0 = r38;
            r0.mModemControllerActivity = r4;
            goto L_0x0573;
        L_0x05e1:
            r0 = r19;
            r4 = new com.android.internal.os.BatteryStatsImpl.LongSamplingCounter[r0][];
            r0 = r38;
            r0.mCpuClusterSpeedTimesUs = r4;
            r12 = 0;
        L_0x05ea:
            r0 = r19;
            if (r12 >= r0) goto L_0x0654;
        L_0x05ee:
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x0645;
        L_0x05f4:
            r24 = r41.readInt();
            r0 = r38;
            r4 = r0.mBsi;
            r4 = r4.mPowerProfile;
            if (r4 == 0) goto L_0x061b;
        L_0x0602:
            r0 = r38;
            r4 = r0.mBsi;
            r4 = r4.mPowerProfile;
            r4 = r4.getNumSpeedStepsInCpuCluster(r12);
            r0 = r24;
            if (r4 == r0) goto L_0x061b;
        L_0x0612:
            r4 = new android.os.ParcelFormatException;
            r5 = "Incompatible number of cpu speeds";
            r4.<init>(r5);
            throw r4;
        L_0x061b:
            r0 = r24;
            r13 = new com.android.internal.os.BatteryStatsImpl.LongSamplingCounter[r0];
            r0 = r38;
            r4 = r0.mCpuClusterSpeedTimesUs;
            r4[r12] = r13;
            r33 = 0;
        L_0x0627:
            r0 = r33;
            r1 = r24;
            if (r0 >= r1) goto L_0x064c;
        L_0x062d:
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x0642;
        L_0x0633:
            r4 = new com.android.internal.os.BatteryStatsImpl$LongSamplingCounter;
            r0 = r38;
            r5 = r0.mBsi;
            r5 = r5.mOnBatteryTimeBase;
            r0 = r41;
            r4.<init>(r5, r0);
            r13[r33] = r4;
        L_0x0642:
            r33 = r33 + 1;
            goto L_0x0627;
        L_0x0645:
            r0 = r38;
            r4 = r0.mCpuClusterSpeedTimesUs;
            r5 = 0;
            r4[r12] = r5;
        L_0x064c:
            r12 = r12 + 1;
            goto L_0x05ea;
        L_0x064f:
            r4 = 0;
            r0 = r38;
            r0.mCpuClusterSpeedTimesUs = r4;
        L_0x0654:
            r0 = r38;
            r4 = r0.mBsi;
            r4 = r4.mOnBatteryTimeBase;
            r0 = r41;
            r4 = com.android.internal.os.BatteryStatsImpl.LongSamplingCounterArray.readFromParcel(r0, r4);
            r0 = r38;
            r0.mCpuFreqTimeMs = r4;
            r0 = r38;
            r4 = r0.mBsi;
            r4 = r4.mOnBatteryScreenOffTimeBase;
            r0 = r41;
            r4 = com.android.internal.os.BatteryStatsImpl.LongSamplingCounterArray.readFromParcel(r0, r4);
            r0 = r38;
            r0.mScreenOffCpuFreqTimeMs = r4;
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x06a3;
        L_0x067a:
            r4 = new com.android.internal.os.BatteryStatsImpl$LongSamplingCounter;
            r0 = r38;
            r5 = r0.mBsi;
            r5 = r5.mOnBatteryTimeBase;
            r0 = r41;
            r4.<init>(r5, r0);
            r0 = r38;
            r0.mMobileRadioApWakeupCount = r4;
        L_0x068b:
            r4 = r41.readInt();
            if (r4 == 0) goto L_0x06a9;
        L_0x0691:
            r4 = new com.android.internal.os.BatteryStatsImpl$LongSamplingCounter;
            r0 = r38;
            r5 = r0.mBsi;
            r5 = r5.mOnBatteryTimeBase;
            r0 = r41;
            r4.<init>(r5, r0);
            r0 = r38;
            r0.mWifiRadioApWakeupCount = r4;
        L_0x06a2:
            return;
        L_0x06a3:
            r4 = 0;
            r0 = r38;
            r0.mMobileRadioApWakeupCount = r4;
            goto L_0x068b;
        L_0x06a9:
            r4 = 0;
            r0 = r38;
            r0.mWifiRadioApWakeupCount = r4;
            goto L_0x06a2;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.readFromParcelLocked(com.android.internal.os.BatteryStatsImpl$TimeBase, com.android.internal.os.BatteryStatsImpl$TimeBase, android.os.Parcel):void");
        }

        public com.android.internal.os.BatteryStatsImpl.Uid.Proc getProcessStatsLocked(java.lang.String r3) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r2 = this;
            r1 = r2.mProcessStats;
            r0 = r1.get(r3);
            r0 = (com.android.internal.os.BatteryStatsImpl.Uid.Proc) r0;
            if (r0 != 0) goto L_0x0016;
        L_0x000a:
            r0 = new com.android.internal.os.BatteryStatsImpl$Uid$Proc;
            r1 = r2.mBsi;
            r0.<init>(r1, r3);
            r1 = r2.mProcessStats;
            r1.put(r3, r0);
        L_0x0016:
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getProcessStatsLocked(java.lang.String):com.android.internal.os.BatteryStatsImpl$Uid$Proc");
        }

        public void updateUidProcessStateLocked(int r13) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r12 = this;
            r9 = 0;
            r7 = 4;
            r8 = 18;
            r10 = 1000; // 0x3e8 float:1.401E-42 double:4.94E-321;
            if (r13 != r7) goto L_0x0016;
        L_0x0008:
            r3 = 1;
        L_0x0009:
            if (r13 != r8) goto L_0x0018;
        L_0x000b:
            r2 = 18;
        L_0x000d:
            r6 = r12.mProcessState;
            if (r6 != r2) goto L_0x0033;
        L_0x0011:
            r6 = r12.mInForegroundService;
            if (r3 != r6) goto L_0x0033;
        L_0x0015:
            return;
        L_0x0016:
            r3 = 0;
            goto L_0x0009;
        L_0x0018:
            r6 = 2;
            if (r13 != r6) goto L_0x001d;
        L_0x001b:
            r2 = 0;
            goto L_0x000d;
        L_0x001d:
            if (r13 > r7) goto L_0x0021;
        L_0x001f:
            r2 = 1;
            goto L_0x000d;
        L_0x0021:
            r6 = 5;
            if (r13 > r6) goto L_0x0026;
        L_0x0024:
            r2 = 2;
            goto L_0x000d;
        L_0x0026:
            r6 = 6;
            if (r13 > r6) goto L_0x002b;
        L_0x0029:
            r2 = 3;
            goto L_0x000d;
        L_0x002b:
            r6 = 12;
            if (r13 > r6) goto L_0x0031;
        L_0x002f:
            r2 = 4;
            goto L_0x000d;
        L_0x0031:
            r2 = 5;
            goto L_0x000d;
        L_0x0033:
            r6 = r12.mBsi;
            r6 = r6.mClocks;
            r0 = r6.elapsedRealtime();
            r6 = r12.mProcessState;
            if (r6 == r2) goto L_0x0076;
        L_0x003f:
            r6 = r12.mBsi;
            r6 = r6.mClocks;
            r4 = r6.uptimeMillis();
            r6 = r12.mProcessState;
            if (r6 == r8) goto L_0x0054;
        L_0x004b:
            r6 = r12.mProcessStateTimer;
            r7 = r12.mProcessState;
            r6 = r6[r7];
            r6.stopRunningLocked(r0);
        L_0x0054:
            r12.mProcessState = r2;
            if (r2 == r8) goto L_0x0068;
        L_0x0058:
            r6 = r12.mProcessStateTimer;
            r6 = r6[r2];
            if (r6 != 0) goto L_0x0061;
        L_0x005e:
            r12.makeProcessState(r2, r9);
        L_0x0061:
            r6 = r12.mProcessStateTimer;
            r6 = r6[r2];
            r6.startRunningLocked(r0);
        L_0x0068:
            r6 = r4 * r10;
            r8 = r0 * r10;
            r12.updateOnBatteryBgTimeBase(r6, r8);
            r6 = r4 * r10;
            r8 = r0 * r10;
            r12.updateOnBatteryScreenOffBgTimeBase(r6, r8);
        L_0x0076:
            r6 = r12.mInForegroundService;
            if (r3 == r6) goto L_0x0081;
        L_0x007a:
            if (r3 == 0) goto L_0x0082;
        L_0x007c:
            r12.noteForegroundServiceResumedLocked(r0);
        L_0x007f:
            r12.mInForegroundService = r3;
        L_0x0081:
            return;
        L_0x0082:
            r12.noteForegroundServicePausedLocked(r0);
            goto L_0x007f;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.updateUidProcessStateLocked(int):void");
        }

        public boolean isInBackground() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r2 = this;
            r0 = r2.mProcessState;
            r1 = 4;
            if (r0 < r1) goto L_0x0007;
        L_0x0005:
            r0 = 1;
        L_0x0006:
            return r0;
        L_0x0007:
            r0 = 0;
            goto L_0x0006;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.isInBackground():boolean");
        }

        public boolean updateOnBatteryBgTimeBase(long r8, long r10) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r7 = this;
            r0 = r7.mBsi;
            r0 = r0.mOnBatteryTimeBase;
            r0 = r0.isRunning();
            if (r0 == 0) goto L_0x0017;
        L_0x000a:
            r1 = r7.isInBackground();
        L_0x000e:
            r0 = r7.mOnBatteryBackgroundTimeBase;
            r2 = r8;
            r4 = r10;
            r0 = r0.setRunning(r1, r2, r4);
            return r0;
        L_0x0017:
            r1 = 0;
            goto L_0x000e;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.updateOnBatteryBgTimeBase(long, long):boolean");
        }

        public boolean updateOnBatteryScreenOffBgTimeBase(long r8, long r10) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r7 = this;
            r0 = r7.mBsi;
            r0 = r0.mOnBatteryScreenOffTimeBase;
            r0 = r0.isRunning();
            if (r0 == 0) goto L_0x0017;
        L_0x000a:
            r1 = r7.isInBackground();
        L_0x000e:
            r0 = r7.mOnBatteryScreenOffBackgroundTimeBase;
            r2 = r8;
            r4 = r10;
            r0 = r0.setRunning(r1, r2, r4);
            return r0;
        L_0x0017:
            r1 = 0;
            goto L_0x000e;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.updateOnBatteryScreenOffBgTimeBase(long, long):boolean");
        }

        public android.util.SparseArray<? extends android.os.BatteryStats.Uid.Pid> getPidStats() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mPids;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getPidStats():android.util.SparseArray<? extends android.os.BatteryStats$Uid$Pid>");
        }

        public android.os.BatteryStats.Uid.Pid getPidStatsLocked(int r3) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r2 = this;
            r1 = r2.mPids;
            r0 = r1.get(r3);
            r0 = (android.os.BatteryStats.Uid.Pid) r0;
            if (r0 != 0) goto L_0x0014;
        L_0x000a:
            r0 = new android.os.BatteryStats$Uid$Pid;
            r0.<init>(r2);
            r1 = r2.mPids;
            r1.put(r3, r0);
        L_0x0014:
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getPidStatsLocked(int):android.os.BatteryStats$Uid$Pid");
        }

        public com.android.internal.os.BatteryStatsImpl.Uid.Pkg getPackageStatsLocked(java.lang.String r3) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r2 = this;
            r1 = r2.mPackageStats;
            r0 = r1.get(r3);
            r0 = (com.android.internal.os.BatteryStatsImpl.Uid.Pkg) r0;
            if (r0 != 0) goto L_0x0016;
        L_0x000a:
            r0 = new com.android.internal.os.BatteryStatsImpl$Uid$Pkg;
            r1 = r2.mBsi;
            r0.<init>(r1);
            r1 = r2.mPackageStats;
            r1.put(r3, r0);
        L_0x0016:
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getPackageStatsLocked(java.lang.String):com.android.internal.os.BatteryStatsImpl$Uid$Pkg");
        }

        public com.android.internal.os.BatteryStatsImpl.Uid.Pkg.Serv getServiceStatsLocked(java.lang.String r4, java.lang.String r5) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r3 = this;
            r0 = r3.getPackageStatsLocked(r4);
            r2 = r0.mServiceStats;
            r1 = r2.get(r5);
            r1 = (com.android.internal.os.BatteryStatsImpl.Uid.Pkg.Serv) r1;
            if (r1 != 0) goto L_0x0017;
        L_0x000e:
            r1 = r0.newServiceStatsLocked();
            r2 = r0.mServiceStats;
            r2.put(r5, r1);
        L_0x0017:
            return r1;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getServiceStatsLocked(java.lang.String, java.lang.String):com.android.internal.os.BatteryStatsImpl$Uid$Pkg$Serv");
        }

        public void readSyncSummaryFromParcelLocked(java.lang.String r3, android.os.Parcel r4) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r2 = this;
            r1 = r2.mSyncStats;
            r0 = r1.instantiateObject();
            r0 = (com.android.internal.os.BatteryStatsImpl.DualTimer) r0;
            r0.readSummaryFromParcelLocked(r4);
            r1 = r2.mSyncStats;
            r1.add(r3, r0);
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.readSyncSummaryFromParcelLocked(java.lang.String, android.os.Parcel):void");
        }

        public void readJobSummaryFromParcelLocked(java.lang.String r3, android.os.Parcel r4) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r2 = this;
            r1 = r2.mJobStats;
            r0 = r1.instantiateObject();
            r0 = (com.android.internal.os.BatteryStatsImpl.DualTimer) r0;
            r0.readSummaryFromParcelLocked(r4);
            r1 = r2.mJobStats;
            r1.add(r3, r0);
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.readJobSummaryFromParcelLocked(java.lang.String, android.os.Parcel):void");
        }

        public void readWakeSummaryFromParcelLocked(java.lang.String r4, android.os.Parcel r5) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r3 = this;
            r2 = 0;
            r0 = new com.android.internal.os.BatteryStatsImpl$Uid$Wakelock;
            r1 = r3.mBsi;
            r0.<init>(r1, r3);
            r1 = r3.mWakelockStats;
            r1.add(r4, r0);
            r1 = r5.readInt();
            if (r1 == 0) goto L_0x001b;
        L_0x0013:
            r1 = 1;
            r1 = r3.getWakelockTimerLocked(r0, r1);
            r1.readSummaryFromParcelLocked(r5);
        L_0x001b:
            r1 = r5.readInt();
            if (r1 == 0) goto L_0x0028;
        L_0x0021:
            r1 = r3.getWakelockTimerLocked(r0, r2);
            r1.readSummaryFromParcelLocked(r5);
        L_0x0028:
            r1 = r5.readInt();
            if (r1 == 0) goto L_0x0036;
        L_0x002e:
            r1 = 2;
            r1 = r3.getWakelockTimerLocked(r0, r1);
            r1.readSummaryFromParcelLocked(r5);
        L_0x0036:
            r1 = r5.readInt();
            if (r1 == 0) goto L_0x0045;
        L_0x003c:
            r1 = 18;
            r1 = r3.getWakelockTimerLocked(r0, r1);
            r1.readSummaryFromParcelLocked(r5);
        L_0x0045:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.readWakeSummaryFromParcelLocked(java.lang.String, android.os.Parcel):void");
        }

        public com.android.internal.os.BatteryStatsImpl.DualTimer getSensorTimerLocked(int r9, boolean r10) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r8 = this;
            r2 = 0;
            r1 = r8.mSensorStats;
            r7 = r1.get(r9);
            r7 = (com.android.internal.os.BatteryStatsImpl.Uid.Sensor) r7;
            if (r7 != 0) goto L_0x001a;
        L_0x000b:
            if (r10 != 0) goto L_0x000e;
        L_0x000d:
            return r2;
        L_0x000e:
            r7 = new com.android.internal.os.BatteryStatsImpl$Uid$Sensor;
            r1 = r8.mBsi;
            r7.<init>(r1, r8, r9);
            r1 = r8.mSensorStats;
            r1.put(r9, r7);
        L_0x001a:
            r0 = r7.mTimer;
            if (r0 == 0) goto L_0x001f;
        L_0x001e:
            return r0;
        L_0x001f:
            r1 = r8.mBsi;
            r1 = r1.mSensorTimers;
            r4 = r1.get(r9);
            r4 = (java.util.ArrayList) r4;
            if (r4 != 0) goto L_0x0037;
        L_0x002b:
            r4 = new java.util.ArrayList;
            r4.<init>();
            r1 = r8.mBsi;
            r1 = r1.mSensorTimers;
            r1.put(r9, r4);
        L_0x0037:
            r0 = new com.android.internal.os.BatteryStatsImpl$DualTimer;
            r1 = r8.mBsi;
            r1 = r1.mClocks;
            r2 = r8.mBsi;
            r5 = r2.mOnBatteryTimeBase;
            r6 = r8.mOnBatteryBackgroundTimeBase;
            r3 = 3;
            r2 = r8;
            r0.<init>(r1, r2, r3, r4, r5, r6);
            r7.mTimer = r0;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getSensorTimerLocked(int, boolean):com.android.internal.os.BatteryStatsImpl$DualTimer");
        }

        public void noteStartSyncLocked(java.lang.String r3, long r4) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r2 = this;
            r1 = r2.mSyncStats;
            r0 = r1.startObject(r3);
            r0 = (com.android.internal.os.BatteryStatsImpl.DualTimer) r0;
            if (r0 == 0) goto L_0x000d;
        L_0x000a:
            r0.startRunningLocked(r4);
        L_0x000d:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteStartSyncLocked(java.lang.String, long):void");
        }

        public void noteStopSyncLocked(java.lang.String r3, long r4) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r2 = this;
            r1 = r2.mSyncStats;
            r0 = r1.stopObject(r3);
            r0 = (com.android.internal.os.BatteryStatsImpl.DualTimer) r0;
            if (r0 == 0) goto L_0x000d;
        L_0x000a:
            r0.stopRunningLocked(r4);
        L_0x000d:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteStopSyncLocked(java.lang.String, long):void");
        }

        public void noteStartJobLocked(java.lang.String r3, long r4) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r2 = this;
            r1 = r2.mJobStats;
            r0 = r1.startObject(r3);
            r0 = (com.android.internal.os.BatteryStatsImpl.DualTimer) r0;
            if (r0 == 0) goto L_0x000d;
        L_0x000a:
            r0.startRunningLocked(r4);
        L_0x000d:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteStartJobLocked(java.lang.String, long):void");
        }

        public void noteStopJobLocked(java.lang.String r5, long r6, int r8) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r4 = this;
            r3 = r4.mJobStats;
            r1 = r3.stopObject(r5);
            r1 = (com.android.internal.os.BatteryStatsImpl.DualTimer) r1;
            if (r1 == 0) goto L_0x000d;
        L_0x000a:
            r1.stopRunningLocked(r6);
        L_0x000d:
            r3 = r4.mBsi;
            r3 = r3.mOnBatteryTimeBase;
            r3 = r3.isRunning();
            if (r3 == 0) goto L_0x0035;
        L_0x0017:
            r3 = r4.mJobCompletions;
            r2 = r3.get(r5);
            r2 = (android.util.SparseIntArray) r2;
            if (r2 != 0) goto L_0x002b;
        L_0x0021:
            r2 = new android.util.SparseIntArray;
            r2.<init>();
            r3 = r4.mJobCompletions;
            r3.put(r5, r2);
        L_0x002b:
            r3 = 0;
            r0 = r2.get(r8, r3);
            r3 = r0 + 1;
            r2.put(r8, r3);
        L_0x0035:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteStopJobLocked(java.lang.String, long, int):void");
        }

        public com.android.internal.os.BatteryStatsImpl.StopwatchTimer getWakelockTimerLocked(com.android.internal.os.BatteryStatsImpl.Uid.Wakelock r8, int r9) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r7 = this;
            r2 = 0;
            if (r8 != 0) goto L_0x0004;
        L_0x0003:
            return r2;
        L_0x0004:
            switch(r9) {
                case 0: goto L_0x0021;
                case 1: goto L_0x003d;
                case 2: goto L_0x0057;
                case 18: goto L_0x0071;
                default: goto L_0x0007;
            };
        L_0x0007:
            r2 = new java.lang.IllegalArgumentException;
            r3 = new java.lang.StringBuilder;
            r3.<init>();
            r4 = "type=";
            r3 = r3.append(r4);
            r3 = r3.append(r9);
            r3 = r3.toString();
            r2.<init>(r3);
            throw r2;
        L_0x0021:
            r0 = r8.mTimerPartial;
            if (r0 != 0) goto L_0x003c;
        L_0x0025:
            r0 = new com.android.internal.os.BatteryStatsImpl$DualTimer;
            r2 = r7.mBsi;
            r1 = r2.mClocks;
            r2 = r7.mBsi;
            r4 = r2.mPartialTimers;
            r2 = r7.mBsi;
            r5 = r2.mOnBatteryScreenOffTimeBase;
            r6 = r7.mOnBatteryScreenOffBackgroundTimeBase;
            r3 = 0;
            r2 = r7;
            r0.<init>(r1, r2, r3, r4, r5, r6);
            r8.mTimerPartial = r0;
        L_0x003c:
            return r0;
        L_0x003d:
            r1 = r8.mTimerFull;
            if (r1 != 0) goto L_0x0056;
        L_0x0041:
            r1 = new com.android.internal.os.BatteryStatsImpl$StopwatchTimer;
            r2 = r7.mBsi;
            r2 = r2.mClocks;
            r3 = r7.mBsi;
            r5 = r3.mFullTimers;
            r3 = r7.mBsi;
            r6 = r3.mOnBatteryTimeBase;
            r4 = 1;
            r3 = r7;
            r1.<init>(r2, r3, r4, r5, r6);
            r8.mTimerFull = r1;
        L_0x0056:
            return r1;
        L_0x0057:
            r1 = r8.mTimerWindow;
            if (r1 != 0) goto L_0x0070;
        L_0x005b:
            r1 = new com.android.internal.os.BatteryStatsImpl$StopwatchTimer;
            r2 = r7.mBsi;
            r2 = r2.mClocks;
            r3 = r7.mBsi;
            r5 = r3.mWindowTimers;
            r3 = r7.mBsi;
            r6 = r3.mOnBatteryTimeBase;
            r4 = 2;
            r3 = r7;
            r1.<init>(r2, r3, r4, r5, r6);
            r8.mTimerWindow = r1;
        L_0x0070:
            return r1;
        L_0x0071:
            r1 = r8.mTimerDraw;
            if (r1 != 0) goto L_0x008b;
        L_0x0075:
            r1 = new com.android.internal.os.BatteryStatsImpl$StopwatchTimer;
            r2 = r7.mBsi;
            r2 = r2.mClocks;
            r3 = r7.mBsi;
            r5 = r3.mDrawTimers;
            r3 = r7.mBsi;
            r6 = r3.mOnBatteryTimeBase;
            r4 = 18;
            r3 = r7;
            r1.<init>(r2, r3, r4, r5, r6);
            r8.mTimerDraw = r1;
        L_0x008b:
            return r1;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getWakelockTimerLocked(com.android.internal.os.BatteryStatsImpl$Uid$Wakelock, int):com.android.internal.os.BatteryStatsImpl$StopwatchTimer");
        }

        public void noteStartWakeLocked(int r5, java.lang.String r6, int r7, long r8) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r4 = this;
            r2 = r4.mWakelockStats;
            r1 = r2.startObject(r6);
            r1 = (com.android.internal.os.BatteryStatsImpl.Uid.Wakelock) r1;
            if (r1 == 0) goto L_0x0011;
        L_0x000a:
            r2 = r4.getWakelockTimerLocked(r1, r7);
            r2.startRunningLocked(r8);
        L_0x0011:
            if (r7 != 0) goto L_0x002a;
        L_0x0013:
            r2 = r4.createAggregatedPartialWakelockTimerLocked();
            r2.startRunningLocked(r8);
            if (r5 < 0) goto L_0x002a;
        L_0x001c:
            r0 = r4.getPidStatsLocked(r5);
            r2 = r0.mWakeNesting;
            r3 = r2 + 1;
            r0.mWakeNesting = r3;
            if (r2 != 0) goto L_0x002a;
        L_0x0028:
            r0.mWakeStartMs = r8;
        L_0x002a:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteStartWakeLocked(int, java.lang.String, int, long):void");
        }

        public void noteStopWakeLocked(int r7, java.lang.String r8, int r9, long r10) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r6 = this;
            r2 = r6.mWakelockStats;
            r1 = r2.stopObject(r8);
            r1 = (com.android.internal.os.BatteryStatsImpl.Uid.Wakelock) r1;
            if (r1 == 0) goto L_0x0011;
        L_0x000a:
            r2 = r6.getWakelockTimerLocked(r1, r9);
            r2.stopRunningLocked(r10);
        L_0x0011:
            if (r9 != 0) goto L_0x0042;
        L_0x0013:
            r2 = r6.mAggregatedPartialWakelockTimer;
            if (r2 == 0) goto L_0x001c;
        L_0x0017:
            r2 = r6.mAggregatedPartialWakelockTimer;
            r2.stopRunningLocked(r10);
        L_0x001c:
            if (r7 < 0) goto L_0x0042;
        L_0x001e:
            r2 = r6.mPids;
            r0 = r2.get(r7);
            r0 = (android.os.BatteryStats.Uid.Pid) r0;
            if (r0 == 0) goto L_0x0042;
        L_0x0028:
            r2 = r0.mWakeNesting;
            if (r2 <= 0) goto L_0x0042;
        L_0x002c:
            r2 = r0.mWakeNesting;
            r3 = r2 + -1;
            r0.mWakeNesting = r3;
            r3 = 1;
            if (r2 != r3) goto L_0x0042;
        L_0x0035:
            r2 = r0.mWakeSumMs;
            r4 = r0.mWakeStartMs;
            r4 = r10 - r4;
            r2 = r2 + r4;
            r0.mWakeSumMs = r2;
            r2 = 0;
            r0.mWakeStartMs = r2;
        L_0x0042:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteStopWakeLocked(int, java.lang.String, int, long):void");
        }

        public void reportExcessiveCpuLocked(java.lang.String r3, long r4, long r6) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r2 = this;
            r0 = r2.getProcessStatsLocked(r3);
            if (r0 == 0) goto L_0x0009;
        L_0x0006:
            r0.addExcessiveCpu(r4, r6);
        L_0x0009:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.reportExcessiveCpuLocked(java.lang.String, long, long):void");
        }

        public void noteStartSensor(int r3, long r4) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r2 = this;
            r1 = 1;
            r0 = r2.getSensorTimerLocked(r3, r1);
            r0.startRunningLocked(r4);
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteStartSensor(int, long):void");
        }

        public void noteStopSensor(int r3, long r4) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r2 = this;
            r1 = 0;
            r0 = r2.getSensorTimerLocked(r3, r1);
            if (r0 == 0) goto L_0x000a;
        L_0x0007:
            r0.stopRunningLocked(r4);
        L_0x000a:
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteStopSensor(int, long):void");
        }

        public void noteStartGps(long r2) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = -10000; // 0xffffffffffffd8f0 float:NaN double:NaN;
            r1.noteStartSensor(r0, r2);
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteStartGps(long):void");
        }

        public void noteStopGps(long r2) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = -10000; // 0xffffffffffffd8f0 float:NaN double:NaN;
            r1.noteStopSensor(r0, r2);
            return;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.noteStopGps(long):void");
        }

        public com.android.internal.os.BatteryStatsImpl getBatteryStats() {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: SSA rename variables already executed
	at jadx.core.dex.visitors.ssa.SSATransform.renameVariables(SSATransform.java:120)
	at jadx.core.dex.visitors.ssa.SSATransform.process(SSATransform.java:52)
	at jadx.core.dex.visitors.ssa.SSATransform.visit(SSATransform.java:42)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:56)
	at jadx.core.ProcessClass.process(ProcessClass.java:39)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
*/
            /*
            r1 = this;
            r0 = r1.mBsi;
            return r0;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.internal.os.BatteryStatsImpl.Uid.getBatteryStats():com.android.internal.os.BatteryStatsImpl");
        }
    }

    public static abstract class UserInfoProvider {
        private int[] userIds;

        protected abstract int[] getUserIds();

        private final void refreshUserIds() {
            this.userIds = getUserIds();
        }

        private final boolean exists(int userId) {
            return this.userIds != null ? ArrayUtils.contains(this.userIds, userId) : true;
        }
    }

    static {
        if (ActivityManager.isLowRamDeviceStatic()) {
            MAX_HISTORY_ITEMS = 800;
            MAX_MAX_HISTORY_ITEMS = 1200;
            MAX_WAKELOCKS_PER_UID = 40;
            MAX_HISTORY_BUFFER = 98304;
            MAX_MAX_HISTORY_BUFFER = 131072;
        } else {
            MAX_HISTORY_ITEMS = 2000;
            MAX_MAX_HISTORY_ITEMS = 3000;
            MAX_WAKELOCKS_PER_UID = 100;
            MAX_HISTORY_BUFFER = 262144;
            MAX_MAX_HISTORY_BUFFER = Protocol.BASE_TETHERING;
        }
    }

    public LongSparseArray<SamplingTimer> getKernelMemoryStats() {
        return this.mKernelMemoryStats;
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

    public long getMahDischarge(int which) {
        return this.mDischargeCounter.getCountLocked(which);
    }

    public long getMahDischargeScreenOff(int which) {
        return this.mDischargeScreenOffCounter.getCountLocked(which);
    }

    public long getMahDischargeScreenDoze(int which) {
        return this.mDischargeScreenDozeCounter.getCountLocked(which);
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
        this.mKernelMemoryBandwidthStats = new KernelMemoryBandwidthStats();
        this.mKernelMemoryStats = new LongSparseArray();
        this.mTmpRpmStats = new RpmStats();
        this.mLastRpmStatsUpdateTimeMs = -1000;
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
        this.mPhoneSignalStrengthBin = -1;
        this.mPhoneSignalStrengthBinRaw = -1;
        this.mPhoneSignalStrengthsTimer = new StopwatchTimer[5];
        this.mPhoneDataConnectionType = -1;
        this.mPhoneDataConnectionsTimer = new StopwatchTimer[17];
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
        this.mPendingWrite = null;
        this.mWriteLock = new ReentrantLock();
        init(clocks);
        this.mFile = null;
        this.mCheckinFile = null;
        this.mDailyFile = null;
        this.mHandler = null;
        this.mPlatformIdleStateCallback = null;
        this.mUserInfoProvider = null;
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
        if (last == null || cur.cmd != (byte) 0) {
            dest.writeInt(DELTA_TIME_ABS);
            cur.writeToParcel(dest, 0);
            return;
        }
        int deltaTimeToken;
        long deltaTime = cur.time - last.time;
        int lastBatteryLevelInt = buildBatteryLevelInt(last);
        int lastStateInt = buildStateInt(last);
        if (deltaTime < 0 || deltaTime > 2147483647L) {
            deltaTimeToken = EventLogTags.SYSUI_VIEW_VISIBILITY;
        } else if (deltaTime >= 524285) {
            deltaTimeToken = DELTA_TIME_INT;
        } else {
            deltaTimeToken = (int) deltaTime;
        }
        int firstToken = deltaTimeToken | (cur.states & DELTA_STATE_MASK);
        int includeStepDetails = this.mLastHistoryStepLevel > cur.batteryLevel ? 1 : 0;
        boolean computeStepDetails = includeStepDetails == 0 ? this.mLastHistoryStepDetails == null : true;
        int batteryLevelInt = buildBatteryLevelInt(cur) | includeStepDetails;
        boolean batteryLevelIntChanged = batteryLevelInt != lastBatteryLevelInt;
        if (batteryLevelIntChanged) {
            firstToken |= 524288;
        }
        int stateInt = buildStateInt(cur);
        boolean stateIntChanged = stateInt != lastStateInt;
        if (stateIntChanged) {
            firstToken |= 1048576;
        }
        boolean state2IntChanged = cur.states2 != last.states2;
        if (state2IntChanged) {
            firstToken |= 2097152;
        }
        if (!(cur.wakelockTag == null && cur.wakeReasonTag == null)) {
            firstToken |= 4194304;
        }
        if (cur.eventCode != 0) {
            firstToken |= 8388608;
        }
        boolean batteryChargeChanged = cur.batteryChargeUAh != last.batteryChargeUAh;
        if (batteryChargeChanged) {
            firstToken |= 16777216;
        }
        dest.writeInt(firstToken);
        if (deltaTimeToken >= DELTA_TIME_INT) {
            if (deltaTimeToken == DELTA_TIME_INT) {
                dest.writeInt((int) deltaTime);
            } else {
                dest.writeLong(deltaTime);
            }
        }
        if (batteryLevelIntChanged) {
            dest.writeInt(batteryLevelInt);
        }
        if (stateIntChanged) {
            dest.writeInt(stateInt);
        }
        if (state2IntChanged) {
            dest.writeInt(cur.states2);
        }
        if (!(cur.wakelockTag == null && cur.wakeReasonTag == null)) {
            int wakeLockIndex;
            int wakeReasonIndex;
            if (cur.wakelockTag != null) {
                wakeLockIndex = writeHistoryTag(cur.wakelockTag);
            } else {
                wakeLockIndex = 65535;
            }
            if (cur.wakeReasonTag != null) {
                wakeReasonIndex = writeHistoryTag(cur.wakeReasonTag);
            } else {
                wakeReasonIndex = 65535;
            }
            dest.writeInt((wakeReasonIndex << 16) | wakeLockIndex);
        }
        if (cur.eventCode != 0) {
            dest.writeInt((cur.eventCode & 65535) | (writeHistoryTag(cur.eventTag) << 16));
        }
        if (computeStepDetails) {
            if (this.mPlatformIdleStateCallback != null) {
                this.mCurHistoryStepDetails.statPlatformIdleState = this.mPlatformIdleStateCallback.getPlatformLowPowerStats();
                this.mCurHistoryStepDetails.statSubsystemPowerState = this.mPlatformIdleStateCallback.getSubsystemLowPowerStats();
            }
            computeHistoryStepDetails(this.mCurHistoryStepDetails, this.mLastHistoryStepDetails);
            if (includeStepDetails != 0) {
                this.mCurHistoryStepDetails.writeToParcel(dest);
            }
            cur.stepDetails = this.mCurHistoryStepDetails;
            this.mLastHistoryStepDetails = this.mCurHistoryStepDetails;
        } else {
            cur.stepDetails = null;
        }
        if (this.mLastHistoryStepLevel < cur.batteryLevel) {
            this.mLastHistoryStepDetails = null;
        }
        this.mLastHistoryStepLevel = cur.batteryLevel;
        if (batteryChargeChanged) {
            dest.writeInt(cur.batteryChargeUAh);
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
        return ((((h.batteryStatus & 7) << 29) | ((h.batteryHealth & 7) << 26)) | ((plugType & 3) << 24)) | (h.states & 16777215);
    }

    private void computeHistoryStepDetails(HistoryStepDetails out, HistoryStepDetails last) {
        HistoryStepDetails tmp = last != null ? this.mTmpHistoryStepDetails : out;
        requestImmediateCpuUpdate();
        int NU;
        int i;
        if (last == null) {
            NU = this.mUidStats.size();
            for (i = 0; i < NU; i++) {
                Uid uid = (Uid) this.mUidStats.valueAt(i);
                uid.mLastStepUserTime = uid.mCurStepUserTime;
                uid.mLastStepSystemTime = uid.mCurStepSystemTime;
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
        for (i = 0; i < NU; i++) {
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
        int deltaTimeToken = firstToken & EventLogTags.SYSUI_VIEW_VISIBILITY;
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
        if ((1048576 & firstToken) != 0) {
            int stateInt = src.readInt();
            cur.states = (DELTA_STATE_MASK & firstToken) | (16777215 & stateInt);
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
            cur.states = (DELTA_STATE_MASK & firstToken) | (cur.states & 16777215);
        }
        if ((2097152 & firstToken) != 0) {
            cur.states2 = src.readInt();
        }
        if ((4194304 & firstToken) != 0) {
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
        if ((8388608 & firstToken) != 0) {
            cur.eventTag = cur.localEventTag;
            int codeAndIndex = src.readInt();
            cur.eventCode = 65535 & codeAndIndex;
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

    void addHistoryBufferLocked(long elapsedRealtimeMs, long uptimeMs, HistoryItem cur) {
        if (this.mHaveBatteryLevel && (this.mRecordingHistory ^ 1) == 0) {
            long timeDiff = (this.mHistoryBaseTime + elapsedRealtimeMs) - this.mHistoryLastWritten.time;
            int diffStates = this.mHistoryLastWritten.states ^ (cur.states & this.mActiveHistoryStates);
            int diffStates2 = this.mHistoryLastWritten.states2 ^ (cur.states2 & this.mActiveHistoryStates2);
            int lastDiffStates = this.mHistoryLastWritten.states ^ this.mHistoryLastLastWritten.states;
            int lastDiffStates2 = this.mHistoryLastWritten.states2 ^ this.mHistoryLastLastWritten.states2;
            if (this.mHistoryBufferLastPos >= 0 && this.mHistoryLastWritten.cmd == (byte) 0 && timeDiff < 1000 && (diffStates & lastDiffStates) == 0 && (diffStates2 & lastDiffStates2) == 0 && ((this.mHistoryLastWritten.wakelockTag == null || cur.wakelockTag == null) && ((this.mHistoryLastWritten.wakeReasonTag == null || cur.wakeReasonTag == null) && this.mHistoryLastWritten.stepDetails == null && ((this.mHistoryLastWritten.eventCode == 0 || cur.eventCode == 0) && this.mHistoryLastWritten.batteryLevel == cur.batteryLevel && this.mHistoryLastWritten.batteryStatus == cur.batteryStatus && this.mHistoryLastWritten.batteryHealth == cur.batteryHealth && this.mHistoryLastWritten.batteryPlugType == cur.batteryPlugType && this.mHistoryLastWritten.batteryTemperature == cur.batteryTemperature && this.mHistoryLastWritten.batteryVoltage == cur.batteryVoltage)))) {
                this.mHistoryBuffer.setDataSize(this.mHistoryBufferLastPos);
                this.mHistoryBuffer.setDataPosition(this.mHistoryBufferLastPos);
                this.mHistoryBufferLastPos = -1;
                elapsedRealtimeMs = this.mHistoryLastWritten.time - this.mHistoryBaseTime;
                if (this.mHistoryLastWritten.wakelockTag != null) {
                    cur.wakelockTag = cur.localWakelockTag;
                    cur.wakelockTag.setTo(this.mHistoryLastWritten.wakelockTag);
                }
                if (this.mHistoryLastWritten.wakeReasonTag != null) {
                    cur.wakeReasonTag = cur.localWakeReasonTag;
                    cur.wakeReasonTag.setTo(this.mHistoryLastWritten.wakeReasonTag);
                }
                if (this.mHistoryLastWritten.eventCode != 0) {
                    cur.eventCode = this.mHistoryLastWritten.eventCode;
                    cur.eventTag = cur.localEventTag;
                    cur.eventTag.setTo(this.mHistoryLastWritten.eventTag);
                }
                this.mHistoryLastWritten.setTo(this.mHistoryLastLastWritten);
            }
            boolean recordResetDueToOverflow = false;
            int dataSize = this.mHistoryBuffer.dataSize();
            if (dataSize >= MAX_MAX_HISTORY_BUFFER * 3) {
                resetAllStatsLocked();
                recordResetDueToOverflow = true;
            } else if (dataSize >= MAX_HISTORY_BUFFER) {
                if (this.mHistoryOverflow) {
                    int old;
                    int writeAnyway = 0;
                    int curStates = (cur.states & -1900544) & this.mActiveHistoryStates;
                    if (this.mHistoryLastWritten.states != curStates) {
                        old = this.mActiveHistoryStates;
                        this.mActiveHistoryStates &= 1900543 | curStates;
                        writeAnyway = old != this.mActiveHistoryStates ? 1 : 0;
                    }
                    int curStates2 = (cur.states2 & 1748959232) & this.mActiveHistoryStates2;
                    if (this.mHistoryLastWritten.states2 != curStates2) {
                        old = this.mActiveHistoryStates2;
                        this.mActiveHistoryStates2 &= -1748959233 | curStates2;
                        writeAnyway |= old != this.mActiveHistoryStates2 ? 1 : 0;
                    }
                    if (writeAnyway != 0 || this.mHistoryLastWritten.batteryLevel != cur.batteryLevel || (dataSize < MAX_MAX_HISTORY_BUFFER && ((this.mHistoryLastWritten.states ^ cur.states) & 1835008) != 0 && ((this.mHistoryLastWritten.states2 ^ cur.states2) & -1749024768) != 0)) {
                        addHistoryBufferLocked(elapsedRealtimeMs, uptimeMs, (byte) 0, cur);
                        return;
                    }
                    return;
                }
                this.mHistoryOverflow = true;
                addHistoryBufferLocked(elapsedRealtimeMs, uptimeMs, (byte) 0, cur);
                addHistoryBufferLocked(elapsedRealtimeMs, uptimeMs, (byte) 6, cur);
                return;
            }
            if (dataSize == 0 || recordResetDueToOverflow) {
                cur.currentTime = System.currentTimeMillis();
                if (recordResetDueToOverflow) {
                    addHistoryBufferLocked(elapsedRealtimeMs, uptimeMs, (byte) 6, cur);
                }
                addHistoryBufferLocked(elapsedRealtimeMs, uptimeMs, (byte) 7, cur);
            }
            addHistoryBufferLocked(elapsedRealtimeMs, uptimeMs, (byte) 0, cur);
        }
    }

    private void addHistoryBufferLocked(long elapsedRealtimeMs, long uptimeMs, byte cmd, HistoryItem cur) {
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
        HistoryItem historyItem;
        if (this.mTrackRunningHistoryElapsedRealtime != 0) {
            long diffElapsed = elapsedRealtimeMs - this.mTrackRunningHistoryElapsedRealtime;
            long diffUptime = uptimeMs - this.mTrackRunningHistoryUptime;
            if (diffUptime < diffElapsed - 20) {
                long wakeElapsedTime = elapsedRealtimeMs - (diffElapsed - diffUptime);
                this.mHistoryAddTmp.setTo(this.mHistoryLastWritten);
                this.mHistoryAddTmp.wakelockTag = null;
                this.mHistoryAddTmp.wakeReasonTag = null;
                this.mHistoryAddTmp.eventCode = 0;
                historyItem = this.mHistoryAddTmp;
                historyItem.states &= Integer.MAX_VALUE;
                addHistoryRecordInnerLocked(wakeElapsedTime, uptimeMs, this.mHistoryAddTmp);
            }
        }
        historyItem = this.mHistoryCur;
        historyItem.states |= Integer.MIN_VALUE;
        this.mTrackRunningHistoryElapsedRealtime = elapsedRealtimeMs;
        this.mTrackRunningHistoryUptime = uptimeMs;
        addHistoryRecordInnerLocked(elapsedRealtimeMs, uptimeMs, this.mHistoryCur);
    }

    void addHistoryRecordInnerLocked(long elapsedRealtimeMs, long uptimeMs, HistoryItem cur) {
        addHistoryBufferLocked(elapsedRealtimeMs, uptimeMs, cur);
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

    public void updateTimeBasesLocked(boolean unplugged, int screenState, long uptime, long realtime) {
        boolean isScreenDoze = !isScreenOff(screenState) ? isScreenDoze(screenState) : true;
        boolean updateOnBatteryTimeBase = unplugged != this.mOnBatteryTimeBase.isRunning();
        boolean updateOnBatteryScreenOffTimeBase = (unplugged ? isScreenDoze : false) != this.mOnBatteryScreenOffTimeBase.isRunning();
        if (updateOnBatteryScreenOffTimeBase || updateOnBatteryTimeBase) {
            int i;
            if (updateOnBatteryScreenOffTimeBase) {
                updateKernelWakelocksLocked();
                updateBatteryPropertiesLocked();
            }
            if (updateOnBatteryTimeBase) {
                updateRpmStatsLocked();
            }
            updateCpuTimeLocked(true);
            this.mOnBatteryTimeBase.setRunning(unplugged, uptime, realtime);
            if (updateOnBatteryTimeBase) {
                for (i = this.mUidStats.size() - 1; i >= 0; i--) {
                    ((Uid) this.mUidStats.valueAt(i)).updateOnBatteryBgTimeBase(uptime, realtime);
                }
            }
            if (updateOnBatteryScreenOffTimeBase) {
                this.mOnBatteryScreenOffTimeBase.setRunning(unplugged ? isScreenDoze : false, uptime, realtime);
                for (i = this.mUidStats.size() - 1; i >= 0; i--) {
                    ((Uid) this.mUidStats.valueAt(i)).updateOnBatteryScreenOffBgTimeBase(uptime, realtime);
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
    }

    public void scheduleRemoveIsolatedUidLocked(int isolatedUid, int appUid) {
        if (this.mIsolatedUids.get(isolatedUid, -1) == appUid && this.mExternalSync != null) {
            this.mExternalSync.scheduleCpuSyncDueToRemovedUid(isolatedUid);
        }
    }

    public void removeIsolatedUidLocked(int isolatedUid) {
        this.mIsolatedUids.delete(isolatedUid);
        this.mKernelUidCpuTimeReader.removeUid(isolatedUid);
        this.mKernelUidCpuFreqTimeReader.removeUid(isolatedUid);
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
        if (currentTime <= 31536000000L || this.mStartClockTime >= currentTime - 31536000000L) {
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
        if (this.mActiveEvents.updateState(16385, name, uid, 0) && this.mRecordAllHistory) {
            addHistoryEventLocked(this.mClocks.elapsedRealtime(), this.mClocks.uptimeMillis(), 16385, name, uid);
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
        if (this.mActiveEvents.updateState(16388, name, uid, 0)) {
            addHistoryEventLocked(elapsedRealtime, uptime, 16388, name, uid);
        }
    }

    public void noteJobStartLocked(String name, int uid) {
        uid = mapUid(uid);
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        getUidStatsLocked(uid).noteStartJobLocked(name, elapsedRealtime);
        if (this.mActiveEvents.updateState(32774, name, uid, 0)) {
            addHistoryEventLocked(elapsedRealtime, uptime, 32774, name, uid);
        }
    }

    public void noteJobFinishLocked(String name, int uid, int stopReason) {
        uid = mapUid(uid);
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        getUidStatsLocked(uid).noteStopJobLocked(name, elapsedRealtime, stopReason);
        if (this.mActiveEvents.updateState(16390, name, uid, 0)) {
            addHistoryEventLocked(elapsedRealtime, uptime, 16390, name, uid);
        }
    }

    public void noteAlarmStartLocked(String name, int uid) {
        if (this.mRecordAllHistory) {
            uid = mapUid(uid);
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            long uptime = this.mClocks.uptimeMillis();
            if (this.mActiveEvents.updateState(32781, name, uid, 0)) {
                addHistoryEventLocked(elapsedRealtime, uptime, 32781, name, uid);
            }
        }
    }

    public void noteAlarmFinishLocked(String name, int uid) {
        if (this.mRecordAllHistory) {
            uid = mapUid(uid);
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            long uptime = this.mClocks.uptimeMillis();
            if (this.mActiveEvents.updateState(16397, name, uid, 0)) {
                addHistoryEventLocked(elapsedRealtime, uptime, 16397, name, uid);
            }
        }
    }

    private void requestWakelockCpuUpdate() {
        if (!this.mHandler.hasMessages(1)) {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(1), 5000);
        }
    }

    private void requestImmediateCpuUpdate() {
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessage(1);
    }

    public void setRecordAllHistoryLocked(boolean enabled) {
        this.mRecordAllHistory = enabled;
        HashMap<String, SparseIntArray> active;
        long mSecRealtime;
        long mSecUptime;
        SparseIntArray uids;
        int j;
        if (enabled) {
            active = this.mActiveEvents.getStateForEvent(1);
            if (active != null) {
                mSecRealtime = this.mClocks.elapsedRealtime();
                mSecUptime = this.mClocks.uptimeMillis();
                for (Entry<String, SparseIntArray> ent : active.entrySet()) {
                    uids = (SparseIntArray) ent.getValue();
                    for (j = 0; j < uids.size(); j++) {
                        addHistoryEventLocked(mSecRealtime, mSecUptime, 32769, (String) ent.getKey(), uids.keyAt(j));
                    }
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
            for (Entry<String, SparseIntArray> ent2 : active.entrySet()) {
                uids = (SparseIntArray) ent2.getValue();
                for (j = 0; j < uids.size(); j++) {
                    addHistoryEventLocked(mSecRealtime, mSecUptime, 16385, (String) ent2.getKey(), uids.keyAt(j));
                }
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

    public void noteStartWakeLocked(int uid, int pid, String name, String historyName, int type, boolean unimportantForLogging, long elapsedRealtime, long uptime) {
        uid = mapUid(uid);
        if (type == 0) {
            aggregateLastWakeupUptimeLocked(uptime);
            if (historyName == null) {
                historyName = name;
            }
            if (this.mRecordAllHistory && this.mActiveEvents.updateState(32773, historyName, uid, 0)) {
                addHistoryEventLocked(elapsedRealtime, uptime, 32773, historyName, uid);
            }
            HistoryTag historyTag;
            if (this.mWakeLockNesting == 0) {
                HistoryItem historyItem = this.mHistoryCur;
                historyItem.states |= 1073741824;
                this.mHistoryCur.wakelockTag = this.mHistoryCur.localWakelockTag;
                historyTag = this.mHistoryCur.wakelockTag;
                this.mInitialAcquireWakeName = historyName;
                historyTag.string = historyName;
                historyTag = this.mHistoryCur.wakelockTag;
                this.mInitialAcquireWakeUid = uid;
                historyTag.uid = uid;
                this.mWakeLockImportant = unimportantForLogging ^ 1;
                addHistoryRecordLocked(elapsedRealtime, uptime);
            } else if (!(this.mWakeLockImportant || (unimportantForLogging ^ 1) == 0 || this.mHistoryLastWritten.cmd != (byte) 0)) {
                if (this.mHistoryLastWritten.wakelockTag != null) {
                    this.mHistoryLastWritten.wakelockTag = null;
                    this.mHistoryCur.wakelockTag = this.mHistoryCur.localWakelockTag;
                    historyTag = this.mHistoryCur.wakelockTag;
                    this.mInitialAcquireWakeName = historyName;
                    historyTag.string = historyName;
                    historyTag = this.mHistoryCur.wakelockTag;
                    this.mInitialAcquireWakeUid = uid;
                    historyTag.uid = uid;
                    addHistoryRecordLocked(elapsedRealtime, uptime);
                }
                this.mWakeLockImportant = true;
            }
            this.mWakeLockNesting++;
        }
        if (uid >= 0) {
            if (this.mOnBatteryScreenOffTimeBase.isRunning()) {
                requestWakelockCpuUpdate();
            }
            getUidStatsLocked(uid).noteStartWakeLocked(pid, name, type, elapsedRealtime);
        }
    }

    public void noteStopWakeLocked(int uid, int pid, String name, String historyName, int type, long elapsedRealtime, long uptime) {
        uid = mapUid(uid);
        if (type == 0) {
            this.mWakeLockNesting--;
            if (this.mRecordAllHistory) {
                if (historyName == null) {
                    historyName = name;
                }
                if (this.mActiveEvents.updateState(16389, historyName, uid, 0)) {
                    addHistoryEventLocked(elapsedRealtime, uptime, 16389, historyName, uid);
                }
            }
            if (this.mWakeLockNesting == 0) {
                HistoryItem historyItem = this.mHistoryCur;
                historyItem.states &= -1073741825;
                this.mInitialAcquireWakeName = null;
                this.mInitialAcquireWakeUid = -1;
                addHistoryRecordLocked(elapsedRealtime, uptime);
            }
        }
        if (uid >= 0) {
            if (this.mOnBatteryScreenOffTimeBase.isRunning()) {
                requestWakelockCpuUpdate();
            }
            getUidStatsLocked(uid).noteStopWakeLocked(pid, name, type, elapsedRealtime);
        }
    }

    public void noteStartWakeFromSourceLocked(WorkSource ws, int pid, String name, String historyName, int type, boolean unimportantForLogging) {
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteStartWakeLocked(ws.get(i), pid, name, historyName, type, unimportantForLogging, elapsedRealtime, uptime);
        }
    }

    public void noteChangeWakelockFromSourceLocked(WorkSource ws, int pid, String name, String historyName, int type, WorkSource newWs, int newPid, String newName, String newHistoryName, int newType, boolean newUnimportantForLogging) {
        int i;
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        int NN = newWs.size();
        for (i = 0; i < NN; i++) {
            noteStartWakeLocked(newWs.get(i), newPid, newName, newHistoryName, newType, newUnimportantForLogging, elapsedRealtime, uptime);
        }
        int NO = ws.size();
        for (i = 0; i < NO; i++) {
            noteStopWakeLocked(ws.get(i), pid, name, historyName, type, elapsedRealtime, uptime);
        }
    }

    public void noteStopWakeFromSourceLocked(WorkSource ws, int pid, String name, String historyName, int type) {
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteStopWakeLocked(ws.get(i), pid, name, historyName, type, elapsedRealtime, uptime);
        }
    }

    public void noteLongPartialWakelockStart(String name, String historyName, int uid) {
        uid = mapUid(uid);
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        if (historyName == null) {
            historyName = name;
        }
        if (this.mActiveEvents.updateState(32788, historyName, uid, 0)) {
            addHistoryEventLocked(elapsedRealtime, uptime, 32788, historyName, uid);
        }
    }

    public void noteLongPartialWakelockFinish(String name, String historyName, int uid) {
        uid = mapUid(uid);
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        if (historyName == null) {
            historyName = name;
        }
        if (this.mActiveEvents.updateState(16404, historyName, uid, 0)) {
            addHistoryEventLocked(elapsedRealtime, uptime, 16404, historyName, uid);
        }
    }

    void aggregateLastWakeupUptimeLocked(long uptimeMs) {
        if (this.mLastWakeupReason != null) {
            getWakeupReasonTimerLocked(this.mLastWakeupReason).add(1000 * (uptimeMs - this.mLastWakeupUptimeMs), 1);
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
        this.mHandler.removeMessages(1);
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
        long j = 0;
        Uid u = (Uid) this.mUidStats.get(mapUid(uid));
        if (u != null) {
            Pid p = (Pid) u.mPids.get(pid);
            if (p != null) {
                long j2 = p.mWakeSumMs;
                if (p.mWakeNesting > 0) {
                    j = realtime - p.mWakeStartMs;
                }
                return j + j2;
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
            historyItem.states |= 8388608;
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

    public void noteStartGpsLocked(int uid) {
        uid = mapUid(uid);
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        if (this.mGpsNesting == 0) {
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states |= 536870912;
            addHistoryRecordLocked(elapsedRealtime, uptime);
        }
        this.mGpsNesting++;
        getUidStatsLocked(uid).noteStartGps(elapsedRealtime);
    }

    public void noteStopGpsLocked(int uid) {
        uid = mapUid(uid);
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        this.mGpsNesting--;
        if (this.mGpsNesting == 0) {
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states &= -536870913;
            addHistoryRecordLocked(elapsedRealtime, uptime);
        }
        getUidStatsLocked(uid).noteStopGps(elapsedRealtime);
    }

    public void noteScreenStateLocked(int state) {
        if (this.mPretendScreenOff) {
            state = 1;
        }
        if (state > 4) {
            switch (state) {
                case 5:
                    state = 2;
                    break;
                default:
                    Slog.wtf(TAG, "Unknown screen state (not mapped): " + state);
                    break;
            }
        }
        if (this.mScreenState != state) {
            HistoryItem historyItem;
            recordDailyStatsIfNeededLocked(true);
            int oldState = this.mScreenState;
            this.mScreenState = state;
            if (state != 0) {
                int stepState = state - 1;
                if ((stepState & 3) == stepState) {
                    this.mModStepMode |= (this.mCurStepMode & 3) ^ stepState;
                    this.mCurStepMode = (this.mCurStepMode & -4) | stepState;
                } else {
                    Slog.wtf(TAG, "Unexpected screen state: " + state);
                }
            }
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            long uptime = this.mClocks.uptimeMillis();
            boolean updateHistory = false;
            if (isScreenDoze(state)) {
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
            if (isScreenOn(state)) {
                historyItem = this.mHistoryCur;
                historyItem.states |= 1048576;
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
            if (isScreenOn(state)) {
                updateTimeBasesLocked(this.mOnBatteryTimeBase.isRunning(), state, this.mClocks.uptimeMillis() * 1000, 1000 * elapsedRealtime);
                noteStartWakeLocked(-1, -1, "screen", null, 0, false, elapsedRealtime, uptime);
            } else if (isScreenOn(oldState)) {
                noteStopWakeLocked(-1, -1, "screen", "screen", 0, elapsedRealtime, uptime);
                updateTimeBasesLocked(this.mOnBatteryTimeBase.isRunning(), state, this.mClocks.uptimeMillis() * 1000, 1000 * elapsedRealtime);
            }
            if (this.mOnBatteryInternal) {
                updateDischargeScreenLevelsLocked(oldState, state);
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
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        if (this.mMobileRadioPowerState != powerState) {
            long realElapsedRealtimeMs;
            boolean active = powerState != 2 ? powerState == 3 : true;
            HistoryItem historyItem;
            if (active) {
                if (uid > 0) {
                    noteMobileRadioApWakeupLocked(elapsedRealtime, uptime, uid);
                }
                realElapsedRealtimeMs = timestampNs / TimeUtils.NANOS_PER_MS;
                this.mMobileRadioActiveStartTime = realElapsedRealtimeMs;
                historyItem = this.mHistoryCur;
                historyItem.states |= 33554432;
            } else {
                realElapsedRealtimeMs = timestampNs / TimeUtils.NANOS_PER_MS;
                long lastUpdateTimeMs = this.mMobileRadioActiveStartTime;
                if (realElapsedRealtimeMs < lastUpdateTimeMs) {
                    Slog.wtf(TAG, "Data connection inactive timestamp " + realElapsedRealtimeMs + " is before start time " + lastUpdateTimeMs);
                    realElapsedRealtimeMs = elapsedRealtime;
                } else if (realElapsedRealtimeMs < elapsedRealtime) {
                    this.mMobileRadioActiveAdjustedTime.addCountLocked(elapsedRealtime - realElapsedRealtimeMs);
                }
                historyItem = this.mHistoryCur;
                historyItem.states &= -33554433;
            }
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mMobileRadioPowerState = powerState;
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
            int stepState = enabled ? 4 : 0;
            this.mModStepMode |= (this.mCurStepMode & 4) ^ stepState;
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
        }
    }

    public void noteDeviceIdleModeLocked(int mode, String activeReason, int activeUid) {
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        boolean nowIdling = mode == 2;
        if (this.mDeviceIdling && (nowIdling ^ 1) != 0 && activeReason == null) {
            nowIdling = true;
        }
        boolean nowLightIdling = mode == 1;
        if (this.mDeviceLightIdling && (nowLightIdling ^ 1) != 0 && (nowIdling ^ 1) != 0 && activeReason == null) {
            nowLightIdling = true;
        }
        if (activeReason != null && (this.mDeviceIdling || this.mDeviceLightIdling)) {
            addHistoryEventLocked(elapsedRealtime, uptime, 10, activeReason, activeUid);
        }
        if (this.mDeviceIdling != nowIdling) {
            this.mDeviceIdling = nowIdling;
            int stepState = nowIdling ? 8 : 0;
            this.mModStepMode |= (this.mCurStepMode & 8) ^ stepState;
            this.mCurStepMode = (this.mCurStepMode & -9) | stepState;
            if (nowIdling) {
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
        if (this.mDeviceIdleMode != mode) {
            this.mHistoryCur.states2 = (this.mHistoryCur.states2 & -100663297) | (mode << 25);
            addHistoryRecordLocked(elapsedRealtime, uptime);
            long lastDuration = elapsedRealtime - this.mLastIdleTimeStart;
            this.mLastIdleTimeStart = elapsedRealtime;
            if (this.mDeviceIdleMode == 1) {
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
            if (mode == 1) {
                this.mDeviceIdleModeLightTimer.startRunningLocked(elapsedRealtime);
            } else if (mode == 2) {
                this.mDeviceIdleModeFullTimer.startRunningLocked(elapsedRealtime);
            }
            this.mDeviceIdleMode = mode;
        }
    }

    public void notePackageInstalledLocked(String pkgName, int versionCode) {
        addHistoryEventLocked(this.mClocks.elapsedRealtime(), this.mClocks.uptimeMillis(), 11, pkgName, versionCode);
        PackageChange pc = new PackageChange();
        pc.mPackageName = pkgName;
        pc.mUpdate = true;
        pc.mVersionCode = versionCode;
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

    public void notePhoneOnLocked() {
        if (!this.mPhoneOn) {
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            long uptime = this.mClocks.uptimeMillis();
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states2 |= 8388608;
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
                HistoryItem historyItem = this.mHistoryCur;
                historyItem.states |= 2097152;
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
            switch (dataType) {
                case 1:
                    bin = 1;
                    break;
                case 2:
                    bin = 2;
                    break;
                case 3:
                    bin = 3;
                    break;
                case 4:
                    bin = 4;
                    break;
                case 5:
                    bin = 5;
                    break;
                case 6:
                    bin = 6;
                    break;
                case 7:
                    bin = 7;
                    break;
                case 8:
                    bin = 8;
                    break;
                case 9:
                    bin = 9;
                    break;
                case 10:
                    bin = 10;
                    break;
                case 11:
                    bin = 11;
                    break;
                case 12:
                    bin = 12;
                    break;
                case 13:
                    bin = 13;
                    break;
                case 14:
                    bin = 14;
                    break;
                case 15:
                    bin = 15;
                    break;
                default:
                    bin = 16;
                    break;
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
            historyItem.states |= 4194304;
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
            this.mAudioOnNesting = 0;
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states &= -4194305;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mAudioOnTimer.stopAllRunningLocked(elapsedRealtime);
            for (int i = 0; i < this.mUidStats.size(); i++) {
                ((Uid) this.mUidStats.valueAt(i)).noteResetAudioLocked(elapsedRealtime);
            }
        }
    }

    public void noteResetVideoLocked() {
        if (this.mVideoOnNesting > 0) {
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            long uptime = this.mClocks.uptimeMillis();
            this.mAudioOnNesting = 0;
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states2 &= -1073741825;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mVideoOnTimer.stopAllRunningLocked(elapsedRealtime);
            for (int i = 0; i < this.mUidStats.size(); i++) {
                ((Uid) this.mUidStats.valueAt(i)).noteResetVideoLocked(elapsedRealtime);
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
            historyItem.states2 |= 2097152;
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
            this.mCameraOnNesting = 0;
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states2 &= -2097153;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mCameraOnTimer.stopAllRunningLocked(elapsedRealtime);
            for (int i = 0; i < this.mUidStats.size(); i++) {
                ((Uid) this.mUidStats.valueAt(i)).noteResetCameraLocked(elapsedRealtime);
            }
        }
    }

    public void noteResetFlashlightLocked() {
        if (this.mFlashlightOnNesting > 0) {
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            long uptime = this.mClocks.uptimeMillis();
            this.mFlashlightOnNesting = 0;
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states2 &= -134217729;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mFlashlightOnTimer.stopAllRunningLocked(elapsedRealtime);
            for (int i = 0; i < this.mUidStats.size(); i++) {
                ((Uid) this.mUidStats.valueAt(i)).noteResetFlashlightLocked(elapsedRealtime);
            }
        }
    }

    private void noteBluetoothScanStartedLocked(int uid, boolean isUnoptimized) {
        uid = mapUid(uid);
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        long uptime = this.mClocks.uptimeMillis();
        if (this.mBluetoothScanNesting == 0) {
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states2 |= 1048576;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mBluetoothScanTimer.startRunningLocked(elapsedRealtime);
        }
        this.mBluetoothScanNesting++;
        getUidStatsLocked(uid).noteBluetoothScanStartedLocked(elapsedRealtime, isUnoptimized);
    }

    public void noteBluetoothScanStartedFromSourceLocked(WorkSource ws, boolean isUnoptimized) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteBluetoothScanStartedLocked(ws.get(i), isUnoptimized);
        }
    }

    private void noteBluetoothScanStoppedLocked(int uid, boolean isUnoptimized) {
        uid = mapUid(uid);
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

    public void noteBluetoothScanStoppedFromSourceLocked(WorkSource ws, boolean isUnoptimized) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteBluetoothScanStoppedLocked(ws.get(i), isUnoptimized);
        }
    }

    public void noteResetBluetoothScanLocked() {
        if (this.mBluetoothScanNesting > 0) {
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            long uptime = this.mClocks.uptimeMillis();
            this.mBluetoothScanNesting = 0;
            HistoryItem historyItem = this.mHistoryCur;
            historyItem.states2 &= -1048577;
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mBluetoothScanTimer.stopAllRunningLocked(elapsedRealtime);
            for (int i = 0; i < this.mUidStats.size(); i++) {
                ((Uid) this.mUidStats.valueAt(i)).noteResetBluetoothScanLocked(elapsedRealtime);
            }
        }
    }

    public void noteBluetoothScanResultsFromSourceLocked(WorkSource ws, int numNewResults) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            getUidStatsLocked(mapUid(ws.get(i))).noteBluetoothScanResultsLocked(numNewResults);
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
            boolean active = powerState != 2 ? powerState == 3 : true;
            HistoryItem historyItem;
            if (active) {
                if (uid > 0) {
                    noteWifiRadioApWakeupLocked(elapsedRealtime, uptime, uid);
                }
                historyItem = this.mHistoryCur;
                historyItem.states |= 67108864;
            } else {
                historyItem = this.mHistoryCur;
                historyItem.states &= -67108865;
            }
            addHistoryRecordLocked(elapsedRealtime, uptime);
            this.mWifiRadioPowerState = powerState;
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
        for (int i = 0; i < N; i++) {
            getUidStatsLocked(mapUid(ws.get(i))).noteWifiRunningLocked(elapsedRealtime);
        }
        scheduleSyncExternalStatsLocked("wifi-running", 2);
    }

    public void noteWifiRunningChangedLocked(WorkSource oldWs, WorkSource newWs) {
        if (this.mGlobalWifiRunning) {
            int i;
            long elapsedRealtime = this.mClocks.elapsedRealtime();
            int N = oldWs.size();
            for (i = 0; i < N; i++) {
                getUidStatsLocked(mapUid(oldWs.get(i))).noteWifiStoppedLocked(elapsedRealtime);
            }
            N = newWs.size();
            for (i = 0; i < N; i++) {
                getUidStatsLocked(mapUid(newWs.get(i))).noteWifiRunningLocked(elapsedRealtime);
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
            this.mGlobalWifiRunning = false;
            this.mGlobalWifiRunningTimer.stopRunningLocked(elapsedRealtime);
            int N = ws.size();
            for (int i = 0; i < N; i++) {
                getUidStatsLocked(mapUid(ws.get(i))).noteWifiStoppedLocked(elapsedRealtime);
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
            this.mWifiSignalStrengthBin = strengthBin;
        }
    }

    public void noteFullWifiLockAcquiredLocked(int uid) {
        uid = mapUid(uid);
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
        uid = mapUid(uid);
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
        uid = mapUid(uid);
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
        uid = mapUid(uid);
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
            historyItem.states |= 65536;
            addHistoryRecordLocked(elapsedRealtime, uptime);
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
        }
        getUidStatsLocked(uid).noteWifiMulticastDisabledLocked(elapsedRealtime);
    }

    public void noteFullWifiLockAcquiredFromSourceLocked(WorkSource ws) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteFullWifiLockAcquiredLocked(ws.get(i));
        }
    }

    public void noteFullWifiLockReleasedFromSourceLocked(WorkSource ws) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteFullWifiLockReleasedLocked(ws.get(i));
        }
    }

    public void noteWifiScanStartedFromSourceLocked(WorkSource ws) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteWifiScanStartedLocked(ws.get(i));
        }
    }

    public void noteWifiScanStoppedFromSourceLocked(WorkSource ws) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteWifiScanStoppedLocked(ws.get(i));
        }
    }

    public void noteWifiBatchedScanStartedFromSourceLocked(WorkSource ws, int csph) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteWifiBatchedScanStartedLocked(ws.get(i), csph);
        }
    }

    public void noteWifiBatchedScanStoppedFromSourceLocked(WorkSource ws) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteWifiBatchedScanStoppedLocked(ws.get(i));
        }
    }

    public void noteWifiMulticastEnabledFromSourceLocked(WorkSource ws) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteWifiMulticastEnabledLocked(ws.get(i));
        }
    }

    public void noteWifiMulticastDisabledFromSourceLocked(WorkSource ws) {
        int N = ws.size();
        for (int i = 0; i < N; i++) {
            noteWifiMulticastDisabledLocked(ws.get(i));
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

    public int getPhoneSignalStrengthCount(int strengthBin, int which) {
        return this.mPhoneSignalStrengthsTimer[strengthBin].getCountLocked(which);
    }

    public long getPhoneDataConnectionTime(int dataType, long elapsedRealtimeUs, int which) {
        return this.mPhoneDataConnectionsTimer[dataType].getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    public int getPhoneDataConnectionCount(int dataType, int which) {
        return this.mPhoneDataConnectionsTimer[dataType].getCountLocked(which);
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

    public long getWifiOnTime(long elapsedRealtimeUs, int which) {
        return this.mWifiOnTimer.getTotalTimeLocked(elapsedRealtimeUs, which);
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

    public long getWifiSupplStateTime(int state, long elapsedRealtimeUs, int which) {
        return this.mWifiSupplStateTimer[state].getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    public int getWifiSupplStateCount(int state, int which) {
        return this.mWifiSupplStateTimer[state].getCountLocked(which);
    }

    public long getWifiSignalStrengthTime(int strengthBin, long elapsedRealtimeUs, int which) {
        return this.mWifiSignalStrengthsTimer[strengthBin].getTotalTimeLocked(elapsedRealtimeUs, which);
    }

    public int getWifiSignalStrengthCount(int strengthBin, int which) {
        return this.mWifiSignalStrengthsTimer[strengthBin].getCountLocked(which);
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
        return 167;
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
        this.mKernelWakelockReader = new KernelWakelockReader();
        this.mTmpWakelockStats = new KernelWakelockStats();
        this.mKernelUidCpuTimeReader = new KernelUidCpuTimeReader();
        this.mKernelUidCpuFreqTimeReader = new KernelUidCpuFreqTimeReader();
        this.mKernelMemoryBandwidthStats = new KernelMemoryBandwidthStats();
        this.mKernelMemoryStats = new LongSparseArray();
        this.mTmpRpmStats = new RpmStats();
        this.mLastRpmStatsUpdateTimeMs = -1000;
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
        this.mPhoneSignalStrengthBin = -1;
        this.mPhoneSignalStrengthBinRaw = -1;
        this.mPhoneSignalStrengthsTimer = new StopwatchTimer[5];
        this.mPhoneDataConnectionType = -1;
        this.mPhoneDataConnectionsTimer = new StopwatchTimer[17];
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
        this.mPendingWrite = null;
        this.mWriteLock = new ReentrantLock();
        init(clocks);
        if (systemDir != null) {
            this.mFile = new JournaledFile(new File(systemDir, "batterystats.bin"), new File(systemDir, "batterystats.bin.tmp"));
        } else {
            this.mFile = null;
        }
        this.mCheckinFile = new AtomicFile(new File(systemDir, "batterystats-checkin.bin"));
        this.mDailyFile = new AtomicFile(new File(systemDir, "batterystats-daily.xml"));
        this.mHandler = new MyHandler(handler.getLooper());
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
        for (i = 0; i < 17; i++) {
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
        this.mWifiOnTimer = new StopwatchTimer(this.mClocks, null, -4, null, this.mOnBatteryTimeBase);
        this.mGlobalWifiRunningTimer = new StopwatchTimer(this.mClocks, null, -5, null, this.mOnBatteryTimeBase);
        for (i = 0; i < 8; i++) {
            this.mWifiStateTimer[i] = new StopwatchTimer(this.mClocks, null, -600 - i, null, this.mOnBatteryTimeBase);
        }
        for (i = 0; i < 13; i++) {
            this.mWifiSupplStateTimer[i] = new StopwatchTimer(this.mClocks, null, -700 - i, null, this.mOnBatteryTimeBase);
        }
        for (i = 0; i < 5; i++) {
            this.mWifiSignalStrengthsTimer[i] = new StopwatchTimer(this.mClocks, null, -800 - i, null, this.mOnBatteryTimeBase);
        }
        this.mAudioOnTimer = new StopwatchTimer(this.mClocks, null, -7, null, this.mOnBatteryTimeBase);
        this.mVideoOnTimer = new StopwatchTimer(this.mClocks, null, -8, null, this.mOnBatteryTimeBase);
        this.mFlashlightOnTimer = new StopwatchTimer(this.mClocks, null, -9, null, this.mOnBatteryTimeBase);
        this.mCameraOnTimer = new StopwatchTimer(this.mClocks, null, -13, null, this.mOnBatteryTimeBase);
        this.mBluetoothScanTimer = new StopwatchTimer(this.mClocks, null, -14, null, this.mOnBatteryTimeBase);
        this.mDischargeScreenOffCounter = new LongSamplingCounter(this.mOnBatteryScreenOffTimeBase);
        this.mDischargeScreenDozeCounter = new LongSamplingCounter(this.mOnBatteryTimeBase);
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
        this.mKernelMemoryBandwidthStats = new KernelMemoryBandwidthStats();
        this.mKernelMemoryStats = new LongSparseArray();
        this.mTmpRpmStats = new RpmStats();
        this.mLastRpmStatsUpdateTimeMs = -1000;
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
        this.mPhoneSignalStrengthBin = -1;
        this.mPhoneSignalStrengthBinRaw = -1;
        this.mPhoneSignalStrengthsTimer = new StopwatchTimer[5];
        this.mPhoneDataConnectionType = -1;
        this.mPhoneDataConnectionsTimer = new StopwatchTimer[17];
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
        this.mPendingWrite = null;
        this.mWriteLock = new ReentrantLock();
        init(clocks);
        this.mFile = null;
        this.mCheckinFile = null;
        this.mDailyFile = null;
        this.mHandler = null;
        this.mExternalSync = null;
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
        } else if (currentTime < this.mDailyStartTime - DateUtils.DAY_IN_MILLIS) {
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
            this.mDailyItems.add(item);
            while (this.mDailyItems.size() > 10) {
                this.mDailyItems.remove(0);
            }
            final ByteArrayOutputStream memStream = new ByteArrayOutputStream();
            try {
                XmlSerializer out = new FastXmlSerializer();
                out.setOutput(memStream, StandardCharsets.UTF_8.name());
                writeDailyItemsLocked(out);
                BackgroundThread.getHandler().post(new Runnable() {
                    public void run() {
                        synchronized (BatteryStatsImpl.this.mCheckinFile) {
                            FileOutputStream fileOutputStream = null;
                            try {
                                fileOutputStream = BatteryStatsImpl.this.mDailyFile.startWrite();
                                memStream.writeTo(fileOutputStream);
                                fileOutputStream.flush();
                                FileUtils.sync(fileOutputStream);
                                fileOutputStream.close();
                                BatteryStatsImpl.this.mDailyFile.finishWrite(fileOutputStream);
                            } catch (IOException e) {
                                Slog.w("BatteryStats", "Error writing battery daily items", e);
                                BatteryStatsImpl.this.mDailyFile.failWrite(fileOutputStream);
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
            out.startTag(null, ImsConfig.EXTRA_CHANGED_ITEM);
            out.attribute(null, BaseMmsColumns.START, Long.toString(dit.mStartTime));
            out.attribute(null, "end", Long.toString(dit.mEndTime));
            writeDailyLevelSteps(out, "dis", dit.mDischargeSteps, sb);
            writeDailyLevelSteps(out, "chg", dit.mChargeSteps, sb);
            if (dit.mPackageChanges != null) {
                for (int j = 0; j < dit.mPackageChanges.size(); j++) {
                    PackageChange pc = (PackageChange) dit.mPackageChanges.get(j);
                    if (pc.mUpdate) {
                        out.startTag(null, "upd");
                        out.attribute(null, HwSysResource.KEY_PKG, pc.mPackageName);
                        out.attribute(null, "ver", Integer.toString(pc.mVersionCode));
                        out.endTag(null, "upd");
                    } else {
                        out.startTag(null, "rem");
                        out.attribute(null, HwSysResource.KEY_PKG, pc.mPackageName);
                        out.endTag(null, "rem");
                    }
                }
            }
            out.endTag(null, ImsConfig.EXTRA_CHANGED_ITEM);
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
                out.attribute(null, BaseMmsColumns.MMS_VERSION, tmpBuilder.toString());
                out.endTag(null, "s");
            }
            out.endTag(null, tag);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void readDailyStatsLocked() {
        Slog.d(TAG, "Reading daily items from " + this.mDailyFile.getBaseFile());
        this.mDailyItems.clear();
        try {
            FileInputStream stream = this.mDailyFile.openRead();
            return;
        } catch (FileNotFoundException e) {
            return;
        }
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(stream, StandardCharsets.UTF_8.name());
            readDailyItemsLocked(parser);
            try {
                stream.close();
            } catch (IOException e2) {
            }
        } catch (XmlPullParserException e3) {
        } catch (Throwable th) {
            try {
                stream.close();
            } catch (IOException e4) {
            }
        }
    }

    private void readDailyItemsLocked(XmlPullParser parser) {
        int type;
        do {
            try {
                type = parser.next();
                if (type == 2) {
                    break;
                }
            } catch (IllegalStateException e) {
                Slog.w(TAG, "Failed parsing daily " + e);
                return;
            } catch (NullPointerException e2) {
                Slog.w(TAG, "Failed parsing daily " + e2);
                return;
            } catch (NumberFormatException e3) {
                Slog.w(TAG, "Failed parsing daily " + e3);
                return;
            } catch (XmlPullParserException e4) {
                Slog.w(TAG, "Failed parsing daily " + e4);
                return;
            } catch (IOException e5) {
                Slog.w(TAG, "Failed parsing daily " + e5);
                return;
            } catch (IndexOutOfBoundsException e6) {
                Slog.w(TAG, "Failed parsing daily " + e6);
                return;
            }
        } while (type != 1);
        if (type != 2) {
            throw new IllegalStateException("no start tag found");
        }
        int outerDepth = parser.getDepth();
        while (true) {
            type = parser.next();
            if (type == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (!(type == 3 || type == 4)) {
                if (parser.getName().equals(ImsConfig.EXTRA_CHANGED_ITEM)) {
                    readDailyItemTagLocked(parser);
                } else {
                    Slog.w(TAG, "Unknown element under <daily-items>: " + parser.getName());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
    }

    void readDailyItemTagLocked(XmlPullParser parser) throws NumberFormatException, XmlPullParserException, IOException {
        DailyItem dit = new DailyItem();
        String attr = parser.getAttributeValue(null, BaseMmsColumns.START);
        if (attr != null) {
            dit.mStartTime = Long.parseLong(attr);
        }
        attr = parser.getAttributeValue(null, "end");
        if (attr != null) {
            dit.mEndTime = Long.parseLong(attr);
        }
        int outerDepth = parser.getDepth();
        while (true) {
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                this.mDailyItems.add(dit);
            } else if (!(type == 3 || type == 4)) {
                String tagName = parser.getName();
                if (tagName.equals("dis")) {
                    readDailyItemTagDetailsLocked(parser, dit, false, "dis");
                } else if (tagName.equals("chg")) {
                    readDailyItemTagDetailsLocked(parser, dit, true, "chg");
                } else if (tagName.equals("upd")) {
                    if (dit.mPackageChanges == null) {
                        dit.mPackageChanges = new ArrayList();
                    }
                    pc = new PackageChange();
                    pc.mUpdate = true;
                    pc.mPackageName = parser.getAttributeValue(null, HwSysResource.KEY_PKG);
                    String verStr = parser.getAttributeValue(null, "ver");
                    pc.mVersionCode = verStr != null ? Integer.parseInt(verStr) : 0;
                    dit.mPackageChanges.add(pc);
                    XmlUtils.skipCurrentTag(parser);
                } else if (tagName.equals("rem")) {
                    if (dit.mPackageChanges == null) {
                        dit.mPackageChanges = new ArrayList();
                    }
                    pc = new PackageChange();
                    pc.mUpdate = false;
                    pc.mPackageName = parser.getAttributeValue(null, HwSysResource.KEY_PKG);
                    dit.mPackageChanges.add(pc);
                    XmlUtils.skipCurrentTag(parser);
                } else {
                    Slog.w(TAG, "Unknown element under <item>: " + parser.getName());
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
        this.mDailyItems.add(dit);
    }

    void readDailyItemTagDetailsLocked(XmlPullParser parser, DailyItem dit, boolean isCharge, String tag) throws NumberFormatException, XmlPullParserException, IOException {
        String numAttr = parser.getAttributeValue(null, "n");
        if (numAttr == null) {
            Slog.w(TAG, "Missing 'n' attribute at " + parser.getPositionDescription());
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
            int type = parser.next();
            if (type == 1 || (type == 3 && parser.getDepth() <= outerDepth)) {
                steps.mNumStepDurations = i;
            } else if (!(type == 3 || type == 4)) {
                if (!"s".equals(parser.getName())) {
                    Slog.w(TAG, "Unknown element under <" + tag + ">: " + parser.getName());
                    XmlUtils.skipCurrentTag(parser);
                } else if (i < num) {
                    String valueAttr = parser.getAttributeValue(null, BaseMmsColumns.MMS_VERSION);
                    if (valueAttr != null) {
                        steps.decodeEntryAt(i, valueAttr);
                        i++;
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
            int i;
            readHistoryDelta(this.mHistoryBuffer, this.mHistoryReadTmp);
            boolean z = this.mReadOverflow;
            if (this.mHistoryReadTmp.cmd == (byte) 6) {
                i = 1;
            } else {
                i = 0;
            }
            this.mReadOverflow = i | z;
        }
        HistoryItem cur = this.mHistoryIterator;
        if (cur == null) {
            if (!(this.mReadOverflow || (end ^ 1) == 0)) {
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
                new HistoryPrinter().printNextItem(pw, out, 0, false, true);
                pw.println("New history:");
                new HistoryPrinter().printNextItem(pw, this.mHistoryReadTmp, 0, false, true);
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
        return state == 2;
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
        this.mDischargeCounter.reset(false);
    }

    public void resetAllStatsCmdLocked() {
        resetAllStatsLocked();
        long mSecUptime = this.mClocks.uptimeMillis();
        long uptime = mSecUptime * 1000;
        long mSecRealtime = this.mClocks.elapsedRealtime();
        long realtime = mSecRealtime * 1000;
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
        initTimes(1000 * uptimeMillis, 1000 * elapsedRealtimeMillis);
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
        for (i = 0; i < 17; i++) {
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
        this.mWifiActivity.reset(false);
        this.mBluetoothActivity.reset(false);
        this.mModemActivity.reset(false);
        this.mUnpluggedNumConnectivityChange = 0;
        this.mLoadedNumConnectivityChange = 0;
        this.mNumConnectivityChange = 0;
        i = 0;
        while (i < this.mUidStats.size()) {
            if (((Uid) this.mUidStats.valueAt(i)).reset(1000 * uptimeMillis, 1000 * elapsedRealtimeMillis)) {
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
            for (i = 0; i < this.mKernelMemoryStats.size(); i++) {
                this.mOnBatteryTimeBase.remove((TimeBaseObs) this.mKernelMemoryStats.valueAt(i));
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
        initDischarge();
        clearHistoryLocked();
        HwLog.dubaie("DUBAI_TAG_RESET_BATTERY_STAT", "");
    }

    private void initActiveHistoryEventsLocked(long elapsedRealtimeMs, long uptimeMs) {
        int i = 0;
        while (i < 22) {
            if (this.mRecordAllHistory || i != 1) {
                HashMap<String, SparseIntArray> active = this.mActiveEvents.getStateForEvent(i);
                if (active != null) {
                    for (Entry<String, SparseIntArray> ent : active.entrySet()) {
                        SparseIntArray uids = (SparseIntArray) ent.getValue();
                        for (int j = 0; j < uids.size(); j++) {
                            addHistoryEventLocked(elapsedRealtimeMs, uptimeMs, i, (String) ent.getKey(), uids.keyAt(j));
                        }
                    }
                }
            }
            i++;
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
            Slog.e(TAG, "failed to read network stats for ifaces: " + Arrays.toString(ifaces));
        }
        return null;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateWifiState(WifiActivityEnergyInfo info) {
        NetworkStats networkStats = null;
        synchronized (this.mWifiNetworkLock) {
            NetworkStats latestStats = readNetworkStatsLocked(this.mWifiIfaces);
            if (latestStats != null) {
                networkStats = NetworkStats.subtract(latestStats, this.mLastWifiNetworkStats, null, null, (NetworkStats) this.mNetworkStatsPool.acquire());
                this.mNetworkStatsPool.release(this.mLastWifiNetworkStats);
                this.mLastWifiNetworkStats = latestStats;
            }
        }
        synchronized (this) {
            if (this.mOnBatteryInternal) {
                int i;
                long elapsedRealtimeMs = this.mClocks.elapsedRealtime();
                SparseLongArray rxPackets = new SparseLongArray();
                SparseLongArray txPackets = new SparseLongArray();
                long totalTxPackets = 0;
                long totalRxPackets = 0;
                if (networkStats != null) {
                    NetworkStats.Entry entry = new NetworkStats.Entry();
                    int size = networkStats.size();
                    for (i = 0; i < size; i++) {
                        entry = networkStats.getValues(i, entry);
                        if (entry.rxBytes != 0 || entry.txBytes != 0) {
                            Uid u = getUidStatsLocked(mapUid(entry.uid));
                            if (entry.rxBytes != 0) {
                                u.noteNetworkActivityLocked(2, entry.rxBytes, entry.rxPackets);
                                if (entry.set == 0) {
                                    u.noteNetworkActivityLocked(8, entry.rxBytes, entry.rxPackets);
                                }
                                this.mNetworkByteActivityCounters[2].addCountLocked(entry.rxBytes);
                                this.mNetworkPacketActivityCounters[2].addCountLocked(entry.rxPackets);
                                rxPackets.put(u.getUid(), entry.rxPackets);
                                totalRxPackets += entry.rxPackets;
                            }
                            if (entry.txBytes != 0) {
                                u.noteNetworkActivityLocked(3, entry.txBytes, entry.txPackets);
                                if (entry.set == 0) {
                                    u.noteNetworkActivityLocked(9, entry.txBytes, entry.txPackets);
                                }
                                this.mNetworkByteActivityCounters[3].addCountLocked(entry.txBytes);
                                this.mNetworkPacketActivityCounters[3].addCountLocked(entry.txPackets);
                                txPackets.put(u.getUid(), entry.txPackets);
                                totalTxPackets += entry.txPackets;
                            }
                        }
                    }
                    this.mNetworkStatsPool.release(networkStats);
                }
                if (info != null) {
                    Uid uid;
                    this.mHasWifiReporting = true;
                    long txTimeMs = info.getControllerTxTimeMillis();
                    long rxTimeMs = info.getControllerRxTimeMillis();
                    long idleTimeMs = info.getControllerIdleTimeMillis();
                    long totalTimeMs = (txTimeMs + rxTimeMs) + idleTimeMs;
                    long leftOverRxTimeMs = rxTimeMs;
                    long leftOverTxTimeMs = txTimeMs;
                    long totalWifiLockTimeMs = 0;
                    long totalScanTimeMs = 0;
                    int uidStatsSize = this.mUidStats.size();
                    for (i = 0; i < uidStatsSize; i++) {
                        uid = (Uid) this.mUidStats.valueAt(i);
                        totalScanTimeMs += uid.mWifiScanTimer.getTimeSinceMarkLocked(1000 * elapsedRealtimeMs) / 1000;
                        totalWifiLockTimeMs += uid.mFullWifiLockTimer.getTimeSinceMarkLocked(1000 * elapsedRealtimeMs) / 1000;
                    }
                    for (i = 0; i < uidStatsSize; i++) {
                        uid = (Uid) this.mUidStats.valueAt(i);
                        long scanTimeSinceMarkMs = uid.mWifiScanTimer.getTimeSinceMarkLocked(1000 * elapsedRealtimeMs) / 1000;
                        if (scanTimeSinceMarkMs > 0) {
                            uid.mWifiScanTimer.setMark(elapsedRealtimeMs);
                            long scanRxTimeSinceMarkMs = scanTimeSinceMarkMs;
                            long scanTxTimeSinceMarkMs = scanTimeSinceMarkMs;
                            if (totalScanTimeMs > rxTimeMs) {
                                scanRxTimeSinceMarkMs = (rxTimeMs * scanTimeSinceMarkMs) / totalScanTimeMs;
                            }
                            if (totalScanTimeMs > txTimeMs) {
                                scanTxTimeSinceMarkMs = (txTimeMs * scanTimeSinceMarkMs) / totalScanTimeMs;
                            }
                            ControllerActivityCounterImpl activityCounter = uid.getOrCreateWifiControllerActivityLocked();
                            activityCounter.getRxTimeCounter().addCountLocked(scanRxTimeSinceMarkMs);
                            activityCounter.getTxTimeCounters()[0].addCountLocked(scanTxTimeSinceMarkMs);
                            leftOverRxTimeMs -= scanRxTimeSinceMarkMs;
                            leftOverTxTimeMs -= scanTxTimeSinceMarkMs;
                        }
                        long wifiLockTimeSinceMarkMs = uid.mFullWifiLockTimer.getTimeSinceMarkLocked(1000 * elapsedRealtimeMs) / 1000;
                        if (wifiLockTimeSinceMarkMs > 0) {
                            uid.mFullWifiLockTimer.setMark(elapsedRealtimeMs);
                            uid.getOrCreateWifiControllerActivityLocked().getIdleTimeCounter().addCountLocked((wifiLockTimeSinceMarkMs * idleTimeMs) / totalWifiLockTimeMs);
                        }
                    }
                    for (i = 0; i < txPackets.size(); i++) {
                        long myTxTimeMs = (txPackets.valueAt(i) * leftOverTxTimeMs) / totalTxPackets;
                        getUidStatsLocked(txPackets.keyAt(i)).getOrCreateWifiControllerActivityLocked().getTxTimeCounters()[0].addCountLocked(myTxTimeMs);
                    }
                    for (i = 0; i < rxPackets.size(); i++) {
                        long myRxTimeMs = (rxPackets.valueAt(i) * leftOverRxTimeMs) / totalRxPackets;
                        getUidStatsLocked(rxPackets.keyAt(i)).getOrCreateWifiControllerActivityLocked().getRxTimeCounter().addCountLocked(myRxTimeMs);
                    }
                    this.mWifiActivity.getRxTimeCounter().addCountLocked(info.getControllerRxTimeMillis());
                    this.mWifiActivity.getTxTimeCounters()[0].addCountLocked(info.getControllerTxTimeMillis());
                    this.mWifiActivity.getIdleTimeCounter().addCountLocked(info.getControllerIdleTimeMillis());
                    double opVolt = this.mPowerProfile.getAveragePower(PowerProfile.POWER_WIFI_CONTROLLER_OPERATING_VOLTAGE) / 1000.0d;
                    if (opVolt != 0.0d) {
                        this.mWifiActivity.getPowerCounter().addCountLocked((long) (((double) info.getControllerEnergyUsed()) / opVolt));
                    }
                }
            } else if (networkStats != null) {
                this.mNetworkStatsPool.release(networkStats);
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateMobileRadioState(ModemActivityInfo activityInfo) {
        NetworkStats networkStats = null;
        synchronized (this.mModemNetworkLock) {
            NetworkStats latestStats = readNetworkStatsLocked(this.mModemIfaces);
            if (latestStats != null) {
                networkStats = NetworkStats.subtract(latestStats, this.mLastModemNetworkStats, null, null, (NetworkStats) this.mNetworkStatsPool.acquire());
                this.mNetworkStatsPool.release(this.mLastModemNetworkStats);
                this.mLastModemNetworkStats = latestStats;
            }
        }
        synchronized (this) {
            if (this.mOnBatteryInternal) {
                int lvl;
                long elapsedRealtimeMs = this.mClocks.elapsedRealtime();
                long radioTime = this.mMobileRadioActivePerAppTimer.getTimeSinceMarkLocked(1000 * elapsedRealtimeMs);
                this.mMobileRadioActivePerAppTimer.setMark(elapsedRealtimeMs);
                long totalRxPackets = 0;
                long totalTxPackets = 0;
                if (networkStats != null) {
                    int i;
                    Uid u;
                    NetworkStats.Entry entry = new NetworkStats.Entry();
                    int size = networkStats.size();
                    for (i = 0; i < size; i++) {
                        entry = networkStats.getValues(i, entry);
                        if (entry.rxPackets != 0 || entry.txPackets != 0) {
                            totalRxPackets += entry.rxPackets;
                            totalTxPackets += entry.txPackets;
                            u = getUidStatsLocked(mapUid(entry.uid));
                            u.noteNetworkActivityLocked(0, entry.rxBytes, entry.rxPackets);
                            u.noteNetworkActivityLocked(1, entry.txBytes, entry.txPackets);
                            if (entry.set == 0) {
                                u.noteNetworkActivityLocked(6, entry.rxBytes, entry.rxPackets);
                                u.noteNetworkActivityLocked(7, entry.txBytes, entry.txPackets);
                            }
                            this.mNetworkByteActivityCounters[0].addCountLocked(entry.rxBytes);
                            this.mNetworkByteActivityCounters[1].addCountLocked(entry.txBytes);
                            this.mNetworkPacketActivityCounters[0].addCountLocked(entry.rxPackets);
                            this.mNetworkPacketActivityCounters[1].addCountLocked(entry.txPackets);
                        }
                    }
                    long totalPackets = totalRxPackets + totalTxPackets;
                    if (totalPackets > 0) {
                        for (i = 0; i < size; i++) {
                            entry = networkStats.getValues(i, entry);
                            if (entry.rxPackets != 0 || entry.txPackets != 0) {
                                u = getUidStatsLocked(mapUid(entry.uid));
                                long appPackets = entry.rxPackets + entry.txPackets;
                                long appRadioTime = (radioTime * appPackets) / totalPackets;
                                u.noteMobileRadioActiveTimeLocked(appRadioTime);
                                radioTime -= appRadioTime;
                                totalPackets -= appPackets;
                                if (activityInfo != null) {
                                    ControllerActivityCounterImpl activityCounter = u.getOrCreateModemControllerActivityLocked();
                                    if (totalRxPackets > 0 && entry.rxPackets > 0) {
                                        activityCounter.getRxTimeCounter().addCountLocked((entry.rxPackets * ((long) activityInfo.getRxTimeMillis())) / totalRxPackets);
                                    }
                                    if (totalTxPackets > 0 && entry.txPackets > 0) {
                                        for (lvl = 0; lvl < 5; lvl++) {
                                            activityCounter.getTxTimeCounters()[lvl].addCountLocked((entry.txPackets * ((long) activityInfo.getTxTimeMillis()[lvl])) / totalTxPackets);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (radioTime > 0) {
                        this.mMobileRadioActiveUnknownTime.addCountLocked(radioTime);
                        this.mMobileRadioActiveUnknownCount.addCountLocked(1);
                    }
                    this.mNetworkStatsPool.release(networkStats);
                }
                if (activityInfo != null) {
                    this.mHasModemReporting = true;
                    this.mModemActivity.getIdleTimeCounter().addCountLocked((long) activityInfo.getIdleTimeMillis());
                    this.mModemActivity.getRxTimeCounter().addCountLocked((long) activityInfo.getRxTimeMillis());
                    for (lvl = 0; lvl < 5; lvl++) {
                        this.mModemActivity.getTxTimeCounters()[lvl].addCountLocked((long) activityInfo.getTxTimeMillis()[lvl]);
                    }
                    double opVolt = this.mPowerProfile.getAveragePower(PowerProfile.POWER_MODEM_CONTROLLER_OPERATING_VOLTAGE) / 1000.0d;
                    if (opVolt != 0.0d) {
                        this.mModemActivity.getPowerCounter().addCountLocked((long) (((double) activityInfo.getEnergyUsed()) / opVolt));
                    }
                }
            } else if (networkStats != null) {
                this.mNetworkStatsPool.release(networkStats);
            }
        }
    }

    public void updateBluetoothStateLocked(BluetoothActivityEnergyInfo info) {
        if (info != null && (this.mOnBatteryInternal ^ 1) == 0) {
            int i;
            Uid u;
            ControllerActivityCounterImpl counter;
            UidTraffic traffic;
            this.mHasBluetoothReporting = true;
            long elapsedRealtimeMs = this.mClocks.elapsedRealtime();
            long rxTimeMs = info.getControllerRxTimeMillis();
            long txTimeMs = info.getControllerTxTimeMillis();
            long totalScanTimeMs = 0;
            int uidCount = this.mUidStats.size();
            for (i = 0; i < uidCount; i++) {
                u = (Uid) this.mUidStats.valueAt(i);
                if (u.mBluetoothScanTimer != null) {
                    totalScanTimeMs += u.mBluetoothScanTimer.getTimeSinceMarkLocked(1000 * elapsedRealtimeMs) / 1000;
                }
            }
            boolean normalizeScanRxTime = totalScanTimeMs > rxTimeMs;
            boolean normalizeScanTxTime = totalScanTimeMs > txTimeMs;
            long leftOverRxTimeMs = rxTimeMs;
            long leftOverTxTimeMs = txTimeMs;
            for (i = 0; i < uidCount; i++) {
                u = (Uid) this.mUidStats.valueAt(i);
                if (u.mBluetoothScanTimer != null) {
                    long scanTimeSinceMarkMs = u.mBluetoothScanTimer.getTimeSinceMarkLocked(1000 * elapsedRealtimeMs) / 1000;
                    if (scanTimeSinceMarkMs > 0) {
                        u.mBluetoothScanTimer.setMark(elapsedRealtimeMs);
                        long scanTimeRxSinceMarkMs = scanTimeSinceMarkMs;
                        long scanTimeTxSinceMarkMs = scanTimeSinceMarkMs;
                        if (normalizeScanRxTime) {
                            scanTimeRxSinceMarkMs = (rxTimeMs * scanTimeSinceMarkMs) / totalScanTimeMs;
                        }
                        if (normalizeScanTxTime) {
                            scanTimeTxSinceMarkMs = (txTimeMs * scanTimeSinceMarkMs) / totalScanTimeMs;
                        }
                        counter = u.getOrCreateBluetoothControllerActivityLocked();
                        counter.getRxTimeCounter().addCountLocked(scanTimeRxSinceMarkMs);
                        counter.getTxTimeCounters()[0].addCountLocked(scanTimeTxSinceMarkMs);
                        leftOverRxTimeMs -= scanTimeRxSinceMarkMs;
                        leftOverTxTimeMs -= scanTimeTxSinceMarkMs;
                    }
                }
            }
            long totalTxBytes = 0;
            long totalRxBytes = 0;
            UidTraffic[] uidTraffic = info.getUidTraffic();
            int numUids = uidTraffic != null ? uidTraffic.length : 0;
            for (i = 0; i < numUids; i++) {
                traffic = uidTraffic[i];
                this.mNetworkByteActivityCounters[4].addCountLocked(traffic.getRxBytes());
                this.mNetworkByteActivityCounters[5].addCountLocked(traffic.getTxBytes());
                u = getUidStatsLocked(mapUid(traffic.getUid()));
                u.noteNetworkActivityLocked(4, traffic.getRxBytes(), 0);
                u.noteNetworkActivityLocked(5, traffic.getTxBytes(), 0);
                totalTxBytes += traffic.getTxBytes();
                totalRxBytes += traffic.getRxBytes();
            }
            if (!((totalTxBytes == 0 && totalRxBytes == 0) || (leftOverRxTimeMs == 0 && leftOverTxTimeMs == 0))) {
                for (i = 0; i < numUids; i++) {
                    traffic = uidTraffic[i];
                    counter = getUidStatsLocked(mapUid(traffic.getUid())).getOrCreateBluetoothControllerActivityLocked();
                    if (totalRxBytes > 0 && traffic.getRxBytes() > 0) {
                        long timeRxMs = (traffic.getRxBytes() * leftOverRxTimeMs) / totalRxBytes;
                        counter.getRxTimeCounter().addCountLocked(timeRxMs);
                        leftOverRxTimeMs -= timeRxMs;
                    }
                    if (totalTxBytes > 0 && traffic.getTxBytes() > 0) {
                        long timeTxMs = (traffic.getTxBytes() * leftOverTxTimeMs) / totalTxBytes;
                        counter.getTxTimeCounters()[0].addCountLocked(timeTxMs);
                        leftOverTxTimeMs -= timeTxMs;
                    }
                }
            }
            this.mBluetoothActivity.getRxTimeCounter().addCountLocked(info.getControllerRxTimeMillis());
            this.mBluetoothActivity.getTxTimeCounters()[0].addCountLocked(info.getControllerTxTimeMillis());
            this.mBluetoothActivity.getIdleTimeCounter().addCountLocked(info.getControllerIdleTimeMillis());
            double opVolt = this.mPowerProfile.getAveragePower(PowerProfile.POWER_BLUETOOTH_CONTROLLER_OPERATING_VOLTAGE) / 1000.0d;
            if (opVolt != 0.0d) {
                this.mBluetoothActivity.getPowerCounter().addCountLocked((long) (((double) info.getControllerEnergyUsed()) / opVolt));
            }
        }
    }

    public void updateRpmStatsLocked() {
        if (this.mPlatformIdleStateCallback != null) {
            long now = SystemClock.elapsedRealtime();
            if (now - this.mLastRpmStatsUpdateTimeMs >= 1000) {
                this.mPlatformIdleStateCallback.fillLowPowerStats(this.mTmpRpmStats);
                this.mLastRpmStatsUpdateTimeMs = now;
            }
            for (Entry<String, PowerStatePlatformSleepState> pstate : this.mTmpRpmStats.mPlatformLowPowerStats.entrySet()) {
                String pName = (String) pstate.getKey();
                getRpmTimerLocked(pName).update(((PowerStatePlatformSleepState) pstate.getValue()).mTimeMs * 1000, ((PowerStatePlatformSleepState) pstate.getValue()).mCount);
                for (Entry<String, PowerStateElement> voter : ((PowerStatePlatformSleepState) pstate.getValue()).mVoters.entrySet()) {
                    String vName = pName + "." + ((String) voter.getKey());
                    getRpmTimerLocked(vName).update(((PowerStateElement) voter.getValue()).mTimeMs * 1000, ((PowerStateElement) voter.getValue()).mCount);
                }
            }
            for (Entry<String, PowerStateSubsystem> subsys : this.mTmpRpmStats.mSubsystemLowPowerStats.entrySet()) {
                String subsysName = (String) subsys.getKey();
                for (Entry<String, PowerStateElement> sstate : ((PowerStateSubsystem) subsys.getValue()).mStates.entrySet()) {
                    String name = subsysName + "." + ((String) sstate.getKey());
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
            Slog.w(TAG, "Read kernel wake lock stats duration: " + readKernelWakelockStatsDuration + " ms. ");
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
            Slog.wtf(TAG, "All kernel wakelocks were set stale. new version=" + wakelockStats.kernelWakelockVersion);
        }
    }

    public void updateKernelMemoryBandwidthLocked() {
        this.mKernelMemoryBandwidthStats.updateStats();
        LongSparseLongArray bandwidthEntries = this.mKernelMemoryBandwidthStats.getBandwidthEntries();
        int bandwidthEntryCount = bandwidthEntries.size();
        for (int i = 0; i < bandwidthEntryCount; i++) {
            SamplingTimer timer;
            int index = this.mKernelMemoryStats.indexOfKey(bandwidthEntries.keyAt(i));
            if (index >= 0) {
                timer = (SamplingTimer) this.mKernelMemoryStats.valueAt(index);
            } else {
                timer = new SamplingTimer(this.mClocks, this.mOnBatteryTimeBase);
                this.mKernelMemoryStats.put(bandwidthEntries.keyAt(i), timer);
            }
            timer.update(bandwidthEntries.valueAt(i), 1);
        }
    }

    public void updateCpuTimeLocked(boolean updateCpuFreqData) {
        if (this.mPowerProfile != null) {
            int i;
            StopwatchTimer timer;
            AnonymousClass3 anonymousClass3;
            Uid u;
            int cluster;
            int speed;
            int numWakelocks = 0;
            int numPartialTimers = this.mPartialTimers.size();
            if (this.mOnBatteryScreenOffTimeBase.isRunning()) {
                for (i = 0; i < numPartialTimers; i++) {
                    timer = (StopwatchTimer) this.mPartialTimers.get(i);
                    if (!(!timer.mInList || timer.mUid == null || timer.mUid.mUid == 1000)) {
                        numWakelocks++;
                    }
                }
            }
            int numWakelocksF = numWakelocks;
            this.mTempTotalCpuUserTimeUs = 0;
            this.mTempTotalCpuSystemTimeUs = 0;
            SparseLongArray updatedUids = new SparseLongArray();
            long startTimeMs = this.mClocks.uptimeMillis();
            this.mUserInfoProvider.refreshUserIds();
            KernelUidCpuTimeReader kernelUidCpuTimeReader = this.mKernelUidCpuTimeReader;
            if (this.mOnBatteryInternal) {
                final int i2 = numWakelocksF;
                final SparseLongArray sparseLongArray = updatedUids;
                AnonymousClass3 anonymousClass32 = new Callback() {
                    public void onUidCpuTime(int uid, long userTimeUs, long systemTimeUs) {
                        uid = BatteryStatsImpl.this.mapUid(uid);
                        if (Process.isIsolated(uid)) {
                            BatteryStatsImpl.this.mKernelUidCpuTimeReader.removeUid(uid);
                            Slog.d(BatteryStatsImpl.TAG, "Got readings for an isolated uid with no mapping to owning uid: " + uid);
                        } else if (BatteryStatsImpl.this.mUserInfoProvider.exists(UserHandle.getUserId(uid))) {
                            Uid u = BatteryStatsImpl.this.getUidStatsLocked(uid);
                            BatteryStatsImpl batteryStatsImpl = BatteryStatsImpl.this;
                            batteryStatsImpl.mTempTotalCpuUserTimeUs += userTimeUs;
                            batteryStatsImpl = BatteryStatsImpl.this;
                            batteryStatsImpl.mTempTotalCpuSystemTimeUs += systemTimeUs;
                            if (i2 > 0) {
                                userTimeUs = (userTimeUs * 50) / 100;
                                systemTimeUs = (systemTimeUs * 50) / 100;
                            }
                            u.mUserCpuTime.addCountLocked(userTimeUs);
                            u.mSystemCpuTime.addCountLocked(systemTimeUs);
                            sparseLongArray.put(u.getUid(), userTimeUs + systemTimeUs);
                        } else {
                            Slog.d(BatteryStatsImpl.TAG, "Got readings for an invalid user's uid " + uid);
                            BatteryStatsImpl.this.mKernelUidCpuTimeReader.removeUid(uid);
                        }
                    }
                };
            } else {
                anonymousClass3 = null;
            }
            kernelUidCpuTimeReader.readDelta(anonymousClass3);
            if (updateCpuFreqData) {
                readKernelUidCpuFreqTimesLocked();
            }
            long elapse = this.mClocks.uptimeMillis() - startTimeMs;
            if (elapse >= 100) {
                Slog.d(TAG, "Reading cpu stats took " + elapse + " ms");
            }
            if (this.mOnBatteryInternal && numWakelocks > 0) {
                this.mTempTotalCpuUserTimeUs = (this.mTempTotalCpuUserTimeUs * 50) / 100;
                this.mTempTotalCpuSystemTimeUs = (this.mTempTotalCpuSystemTimeUs * 50) / 100;
                for (i = 0; i < numPartialTimers; i++) {
                    timer = (StopwatchTimer) this.mPartialTimers.get(i);
                    if (!(!timer.mInList || timer.mUid == null || timer.mUid.mUid == 1000)) {
                        int userTimeUs = (int) (this.mTempTotalCpuUserTimeUs / ((long) numWakelocks));
                        int systemTimeUs = (int) (this.mTempTotalCpuSystemTimeUs / ((long) numWakelocks));
                        timer.mUid.mUserCpuTime.addCountLocked((long) userTimeUs);
                        timer.mUid.mSystemCpuTime.addCountLocked((long) systemTimeUs);
                        int uid = timer.mUid.getUid();
                        updatedUids.put(uid, (updatedUids.get(uid, 0) + ((long) userTimeUs)) + ((long) systemTimeUs));
                        timer.mUid.getProcessStatsLocked("*wakelock*").addCpuTimeLocked(userTimeUs / 1000, systemTimeUs / 1000);
                        this.mTempTotalCpuUserTimeUs -= (long) userTimeUs;
                        this.mTempTotalCpuSystemTimeUs -= (long) systemTimeUs;
                        numWakelocks--;
                    }
                }
                if (this.mTempTotalCpuUserTimeUs > 0 || this.mTempTotalCpuSystemTimeUs > 0) {
                    u = getUidStatsLocked(1000);
                    u.mUserCpuTime.addCountLocked(this.mTempTotalCpuUserTimeUs);
                    u.mSystemCpuTime.addCountLocked(this.mTempTotalCpuSystemTimeUs);
                    updatedUids.put(1000, (updatedUids.get(1000, 0) + this.mTempTotalCpuUserTimeUs) + this.mTempTotalCpuSystemTimeUs);
                    u.getProcessStatsLocked("*lost*").addCpuTimeLocked(((int) this.mTempTotalCpuUserTimeUs) / 1000, ((int) this.mTempTotalCpuSystemTimeUs) / 1000);
                }
            }
            long totalCpuClustersTimeMs = 0;
            long[][] clusterSpeedTimesMs = new long[this.mKernelCpuSpeedReaders.length][];
            for (cluster = 0; cluster < this.mKernelCpuSpeedReaders.length; cluster++) {
                clusterSpeedTimesMs[cluster] = this.mKernelCpuSpeedReaders[cluster].readDelta();
                if (clusterSpeedTimesMs[cluster] != null) {
                    for (speed = clusterSpeedTimesMs[cluster].length - 1; speed >= 0; speed--) {
                        totalCpuClustersTimeMs += clusterSpeedTimesMs[cluster][speed];
                    }
                }
            }
            if (totalCpuClustersTimeMs != 0) {
                int updatedUidsCount = updatedUids.size();
                for (i = 0; i < updatedUidsCount; i++) {
                    u = getUidStatsLocked(updatedUids.keyAt(i));
                    long appCpuTimeUs = updatedUids.valueAt(i);
                    int numClusters = this.mPowerProfile.getNumCpuClusters();
                    if (u.mCpuClusterSpeedTimesUs == null || u.mCpuClusterSpeedTimesUs.length != numClusters) {
                        u.mCpuClusterSpeedTimesUs = new LongSamplingCounter[numClusters][];
                    }
                    cluster = 0;
                    while (cluster < clusterSpeedTimesMs.length) {
                        int speedsInCluster = clusterSpeedTimesMs[cluster].length;
                        if (u.mCpuClusterSpeedTimesUs[cluster] == null || speedsInCluster != u.mCpuClusterSpeedTimesUs[cluster].length) {
                            u.mCpuClusterSpeedTimesUs[cluster] = new LongSamplingCounter[speedsInCluster];
                        }
                        LongSamplingCounter[] cpuSpeeds = u.mCpuClusterSpeedTimesUs[cluster];
                        for (speed = 0; speed < speedsInCluster; speed++) {
                            if (cpuSpeeds[speed] == null) {
                                cpuSpeeds[speed] = new LongSamplingCounter(this.mOnBatteryTimeBase);
                            }
                            cpuSpeeds[speed].addCountLocked((clusterSpeedTimesMs[cluster][speed] * appCpuTimeUs) / totalCpuClustersTimeMs);
                        }
                        cluster++;
                    }
                }
            }
            if (ArrayUtils.referenceEquals(this.mPartialTimers, this.mLastPartialTimers)) {
                for (i = 0; i < numPartialTimers; i++) {
                    ((StopwatchTimer) this.mPartialTimers.get(i)).mInList = true;
                }
            } else {
                int numLastPartialTimers = this.mLastPartialTimers.size();
                for (i = 0; i < numLastPartialTimers; i++) {
                    ((StopwatchTimer) this.mLastPartialTimers.get(i)).mInList = false;
                }
                this.mLastPartialTimers.clear();
                for (i = 0; i < numPartialTimers; i++) {
                    timer = (StopwatchTimer) this.mPartialTimers.get(i);
                    timer.mInList = true;
                    this.mLastPartialTimers.add(timer);
                }
            }
        }
    }

    void readKernelUidCpuFreqTimesLocked() {
        KernelUidCpuFreqTimeReader.Callback anonymousClass4;
        KernelUidCpuFreqTimeReader kernelUidCpuFreqTimeReader = this.mKernelUidCpuFreqTimeReader;
        if (this.mOnBatteryInternal) {
            anonymousClass4 = new KernelUidCpuFreqTimeReader.Callback() {
                public void onCpuFreqs(long[] cpuFreqs) {
                    BatteryStatsImpl.this.mCpuFreqs = cpuFreqs;
                }

                public void onUidCpuFreqTime(int uid, long[] cpuFreqTimeMs) {
                    uid = BatteryStatsImpl.this.mapUid(uid);
                    if (Process.isIsolated(uid)) {
                        BatteryStatsImpl.this.mKernelUidCpuFreqTimeReader.removeUid(uid);
                        Slog.d(BatteryStatsImpl.TAG, "Got freq readings for an isolated uid with no mapping to owning uid: " + uid);
                    } else if (BatteryStatsImpl.this.mUserInfoProvider.exists(UserHandle.getUserId(uid))) {
                        Uid u = BatteryStatsImpl.this.getUidStatsLocked(uid);
                        if (u.mCpuFreqTimeMs == null || u.mCpuFreqTimeMs.getSize() != cpuFreqTimeMs.length) {
                            u.mCpuFreqTimeMs = new LongSamplingCounterArray(BatteryStatsImpl.this.mOnBatteryTimeBase);
                        }
                        u.mCpuFreqTimeMs.addCountLocked(cpuFreqTimeMs);
                        if (u.mScreenOffCpuFreqTimeMs == null || u.mScreenOffCpuFreqTimeMs.getSize() != cpuFreqTimeMs.length) {
                            u.mScreenOffCpuFreqTimeMs = new LongSamplingCounterArray(BatteryStatsImpl.this.mOnBatteryScreenOffTimeBase);
                        }
                        u.mScreenOffCpuFreqTimeMs.addCountLocked(cpuFreqTimeMs);
                    } else {
                        Slog.d(BatteryStatsImpl.TAG, "Got readings for an invalid user's uid " + uid);
                        BatteryStatsImpl.this.mKernelUidCpuFreqTimeReader.removeUid(uid);
                    }
                }
            };
        } else {
            anonymousClass4 = null;
        }
        kernelUidCpuFreqTimeReader.readDelta(anonymousClass4);
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

    protected void setOnBatteryLocked(long mSecRealtime, long mSecUptime, boolean onBattery, int oldStatus, int level, int chargeUAh) {
        boolean doWrite = false;
        Message m = this.mHandler.obtainMessage(2);
        m.arg1 = onBattery ? 1 : 0;
        this.mHandler.sendMessage(m);
        long uptime = mSecUptime * 1000;
        long realtime = mSecRealtime * 1000;
        int screenState = this.mScreenState;
        HistoryItem historyItem;
        if (onBattery) {
            boolean reset = false;
            if (!this.mNoAutoReset) {
                if (oldStatus != 5 && level < 90 && (this.mDischargeCurrentLevel >= 20 || level < 80)) {
                    if (getHighDischargeAmountSinceCharge() >= 200 && this.mHistoryBuffer.dataSize() >= MAX_HISTORY_BUFFER) {
                    }
                }
                Slog.i(TAG, "Resetting battery stats: level=" + level + " status=" + oldStatus + " dischargeLevel=" + this.mDischargeCurrentLevel + " lowAmount=" + getLowDischargeAmountSinceCharge() + " highAmount=" + getHighDischargeAmountSinceCharge());
                if (getLowDischargeAmountSinceCharge() >= 20) {
                    Parcel parcel = Parcel.obtain();
                    writeSummaryToParcel(parcel, true);
                    final Parcel parcel2 = parcel;
                    BackgroundThread.getHandler().post(new Runnable() {
                        public void run() {
                            synchronized (BatteryStatsImpl.this.mCheckinFile) {
                                FileOutputStream fileOutputStream = null;
                                try {
                                    fileOutputStream = BatteryStatsImpl.this.mCheckinFile.startWrite();
                                    fileOutputStream.write(parcel2.marshall());
                                    fileOutputStream.flush();
                                    FileUtils.sync(fileOutputStream);
                                    fileOutputStream.close();
                                    BatteryStatsImpl.this.mCheckinFile.finishWrite(fileOutputStream);
                                    parcel2.recycle();
                                } catch (IOException e) {
                                    Slog.w("BatteryStats", "Error writing checkin battery statistics", e);
                                    BatteryStatsImpl.this.mCheckinFile.failWrite(fileOutputStream);
                                    parcel2.recycle();
                                } catch (Throwable th) {
                                    parcel2.recycle();
                                }
                            }
                        }
                    });
                }
                doWrite = true;
                resetAllStatsLocked();
                if (chargeUAh > 0 && level > 0) {
                    this.mEstimatedBatteryCapacity = (int) (((double) (chargeUAh / 1000)) / (((double) level) / 100.0d));
                }
                this.mDischargeStartLevel = level;
                reset = true;
                LogPower.push(190);
                this.mDischargeStepTracker.init();
            }
            if (this.mCharging) {
                setChargingLocked(false);
            }
            this.mLastChargingStateLevel = level;
            this.mOnBatteryInternal = true;
            this.mOnBattery = true;
            this.mLastDischargeStepLevel = level;
            this.mMinDischargeStepLevel = level;
            this.mDischargeStepTracker.clearTime();
            this.mDailyDischargeStepTracker.clearTime();
            this.mInitStepMode = this.mCurStepMode;
            this.mModStepMode = 0;
            pullPendingStateUpdatesLocked();
            this.mHistoryCur.batteryLevel = (byte) level;
            historyItem = this.mHistoryCur;
            historyItem.states &= -524289;
            if (reset) {
                this.mRecordingHistory = true;
                startRecordingHistory(mSecRealtime, mSecUptime, reset);
            }
            addHistoryRecordLocked(mSecRealtime, mSecUptime);
            this.mDischargeUnplugLevel = level;
            this.mDischargeCurrentLevel = level;
            if (isScreenOn(screenState)) {
                this.mDischargeScreenOnUnplugLevel = level;
                this.mDischargeScreenDozeUnplugLevel = 0;
                this.mDischargeScreenOffUnplugLevel = 0;
            } else if (isScreenDoze(screenState)) {
                this.mDischargeScreenOnUnplugLevel = 0;
                this.mDischargeScreenDozeUnplugLevel = level;
                this.mDischargeScreenOffUnplugLevel = 0;
            } else {
                this.mDischargeScreenOnUnplugLevel = 0;
                this.mDischargeScreenDozeUnplugLevel = 0;
                this.mDischargeScreenOffUnplugLevel = level;
            }
            this.mDischargeAmountScreenOn = 0;
            this.mDischargeAmountScreenDoze = 0;
            this.mDischargeAmountScreenOff = 0;
            updateTimeBasesLocked(true, screenState, uptime, realtime);
        } else {
            this.mLastChargingStateLevel = level;
            this.mOnBatteryInternal = false;
            this.mOnBattery = false;
            pullPendingStateUpdatesLocked();
            this.mHistoryCur.batteryLevel = (byte) level;
            historyItem = this.mHistoryCur;
            historyItem.states |= 524288;
            addHistoryRecordLocked(mSecRealtime, mSecUptime);
            this.mDischargePlugLevel = level;
            this.mDischargeCurrentLevel = level;
            if (level < this.mDischargeUnplugLevel) {
                this.mLowDischargeAmountSinceCharge += (this.mDischargeUnplugLevel - level) - 1;
                this.mHighDischargeAmountSinceCharge += this.mDischargeUnplugLevel - level;
            }
            updateDischargeScreenLevelsLocked(screenState, screenState);
            updateTimeBasesLocked(false, screenState, uptime, realtime);
            this.mChargeStepTracker.init();
            this.mLastChargeStepLevel = level;
            this.mMaxChargeStepLevel = level;
            this.mInitStepMode = this.mCurStepMode;
            this.mModStepMode = 0;
        }
        if ((doWrite || this.mLastWriteTime + DateUtils.MINUTE_IN_MILLIS < mSecRealtime) && this.mFile != null) {
            writeAsyncLocked();
        }
    }

    private void startRecordingHistory(long elapsedRealtimeMs, long uptimeMs, boolean reset) {
        this.mRecordingHistory = true;
        this.mHistoryCur.currentTime = System.currentTimeMillis();
        addHistoryBufferLocked(elapsedRealtimeMs, uptimeMs, reset ? (byte) 7 : (byte) 5, this.mHistoryCur);
        this.mHistoryCur.currentTime = 0;
        if (reset) {
            initActiveHistoryEventsLocked(elapsedRealtimeMs, uptimeMs);
        }
    }

    private void recordCurrentTimeChangeLocked(long currentTime, long elapsedRealtimeMs, long uptimeMs) {
        if (this.mRecordingHistory) {
            this.mHistoryCur.currentTime = currentTime;
            addHistoryBufferLocked(elapsedRealtimeMs, uptimeMs, (byte) 5, this.mHistoryCur);
            this.mHistoryCur.currentTime = 0;
        }
    }

    private void recordShutdownLocked(long elapsedRealtimeMs, long uptimeMs) {
        if (this.mRecordingHistory) {
            this.mHistoryCur.currentTime = System.currentTimeMillis();
            addHistoryBufferLocked(elapsedRealtimeMs, uptimeMs, (byte) 8, this.mHistoryCur);
            this.mHistoryCur.currentTime = 0;
        }
    }

    private void scheduleSyncExternalStatsLocked(String reason, int updateFlags) {
        if (this.mExternalSync != null) {
            this.mExternalSync.scheduleSync(reason, updateFlags);
        }
    }

    public void setBatteryStateLocked(int status, int health, int plugType, int level, int temp, int volt, int chargeUAh, int chargeFullUAh) {
        temp = Math.max(0, temp);
        boolean onBattery = plugType == 0;
        long uptime = this.mClocks.uptimeMillis();
        long elapsedRealtime = this.mClocks.elapsedRealtime();
        if (!this.mHaveBatteryLevel) {
            HistoryItem historyItem;
            this.mHaveBatteryLevel = true;
            if (onBattery == this.mOnBattery) {
                if (onBattery) {
                    historyItem = this.mHistoryCur;
                    historyItem.states &= -524289;
                } else {
                    historyItem = this.mHistoryCur;
                    historyItem.states |= 524288;
                }
            }
            historyItem = this.mHistoryCur;
            historyItem.states2 |= 16777216;
            this.mHistoryCur.batteryStatus = (byte) status;
            this.mHistoryCur.batteryLevel = (byte) level;
            this.mHistoryCur.batteryChargeUAh = chargeUAh;
            this.mLastDischargeStepLevel = level;
            this.mLastChargeStepLevel = level;
            this.mMinDischargeStepLevel = level;
            this.mMaxChargeStepLevel = level;
            this.mLastChargingStateLevel = level;
        } else if (!(this.mCurrentBatteryLevel == level && this.mOnBattery == onBattery)) {
            recordDailyStatsIfNeededLocked(level >= 100 ? onBattery : false);
        }
        int oldStatus = this.mHistoryCur.batteryStatus;
        if (onBattery) {
            this.mDischargeCurrentLevel = level;
            if (!this.mRecordingHistory) {
                this.mRecordingHistory = true;
                startRecordingHistory(elapsedRealtime, uptime, true);
            }
        } else if (level < 96 && !this.mRecordingHistory) {
            this.mRecordingHistory = true;
            startRecordingHistory(elapsedRealtime, uptime, true);
        }
        this.mCurrentBatteryLevel = level;
        if (this.mDischargePlugLevel < 0) {
            this.mDischargePlugLevel = level;
        }
        long chargeDiff;
        if (onBattery != this.mOnBattery) {
            this.mHistoryCur.batteryLevel = (byte) level;
            this.mHistoryCur.batteryStatus = (byte) status;
            this.mHistoryCur.batteryHealth = (byte) health;
            this.mHistoryCur.batteryPlugType = (byte) plugType;
            this.mHistoryCur.batteryTemperature = (short) temp;
            this.mHistoryCur.batteryVoltage = (char) volt;
            if (chargeUAh < this.mHistoryCur.batteryChargeUAh) {
                chargeDiff = (long) (this.mHistoryCur.batteryChargeUAh - chargeUAh);
                this.mDischargeCounter.addCountLocked(chargeDiff);
                this.mDischargeScreenOffCounter.addCountLocked(chargeDiff);
                if (isScreenDoze(this.mScreenState)) {
                    this.mDischargeScreenDozeCounter.addCountLocked(chargeDiff);
                }
            }
            this.mHistoryCur.batteryChargeUAh = chargeUAh;
            setOnBatteryLocked(elapsedRealtime, uptime, onBattery, oldStatus, level, chargeUAh);
        } else {
            boolean changed = false;
            if (this.mHistoryCur.batteryLevel != level) {
                this.mHistoryCur.batteryLevel = (byte) level;
                changed = true;
                scheduleSyncExternalStatsLocked("battery-level", 31);
            }
            if (this.mHistoryCur.batteryStatus != status) {
                this.mHistoryCur.batteryStatus = (byte) status;
                changed = true;
            }
            if (this.mHistoryCur.batteryHealth != health) {
                this.mHistoryCur.batteryHealth = (byte) health;
                changed = true;
            }
            if (this.mHistoryCur.batteryPlugType != plugType) {
                this.mHistoryCur.batteryPlugType = (byte) plugType;
                changed = true;
            }
            if (temp >= this.mHistoryCur.batteryTemperature + 10 || temp <= this.mHistoryCur.batteryTemperature - 10) {
                this.mHistoryCur.batteryTemperature = (short) temp;
                changed = true;
            }
            if (volt > this.mHistoryCur.batteryVoltage + 20 || volt < this.mHistoryCur.batteryVoltage - 20) {
                this.mHistoryCur.batteryVoltage = (char) volt;
                changed = true;
            }
            if (chargeUAh >= this.mHistoryCur.batteryChargeUAh + 10 || chargeUAh <= this.mHistoryCur.batteryChargeUAh - 10) {
                if (chargeUAh < this.mHistoryCur.batteryChargeUAh) {
                    chargeDiff = (long) (this.mHistoryCur.batteryChargeUAh - chargeUAh);
                    this.mDischargeCounter.addCountLocked(chargeDiff);
                    this.mDischargeScreenOffCounter.addCountLocked(chargeDiff);
                    if (isScreenDoze(this.mScreenState)) {
                        this.mDischargeScreenDozeCounter.addCountLocked(chargeDiff);
                    }
                }
                this.mHistoryCur.batteryChargeUAh = chargeUAh;
                changed = true;
            }
            long modeBits = ((((long) this.mInitStepMode) << 48) | (((long) this.mModStepMode) << 56)) | (((long) (level & 255)) << 40);
            if (onBattery) {
                changed |= setChargingLocked(false);
                if (this.mLastDischargeStepLevel != level && this.mMinDischargeStepLevel > level) {
                    this.mDischargeStepTracker.addLevelSteps(this.mLastDischargeStepLevel - level, modeBits, elapsedRealtime);
                    this.mDailyDischargeStepTracker.addLevelSteps(this.mLastDischargeStepLevel - level, modeBits, elapsedRealtime);
                    this.mLastDischargeStepLevel = level;
                    this.mMinDischargeStepLevel = level;
                    this.mInitStepMode = this.mCurStepMode;
                    this.mModStepMode = 0;
                }
            } else {
                if (level >= 90) {
                    changed |= setChargingLocked(true);
                    this.mLastChargeStepLevel = level;
                }
                if (this.mCharging) {
                    if (this.mLastChargeStepLevel > level) {
                        changed |= setChargingLocked(false);
                        this.mLastChargeStepLevel = level;
                    }
                } else if (this.mLastChargeStepLevel < level) {
                    changed |= setChargingLocked(true);
                    this.mLastChargeStepLevel = level;
                }
                if (this.mLastChargeStepLevel != level && this.mMaxChargeStepLevel < level) {
                    this.mChargeStepTracker.addLevelSteps(level - this.mLastChargeStepLevel, modeBits, elapsedRealtime);
                    this.mDailyChargeStepTracker.addLevelSteps(level - this.mLastChargeStepLevel, modeBits, elapsedRealtime);
                    this.mLastChargeStepLevel = level;
                    this.mMaxChargeStepLevel = level;
                    this.mInitStepMode = this.mCurStepMode;
                    this.mModStepMode = 0;
                }
            }
            if (changed) {
                addHistoryRecordLocked(elapsedRealtime, uptime);
            }
        }
        if (!onBattery && status == 5) {
            this.mRecordingHistory = false;
        }
        if (this.mMinLearnedBatteryCapacity == -1) {
            this.mMinLearnedBatteryCapacity = chargeFullUAh;
        } else {
            Math.min(this.mMinLearnedBatteryCapacity, chargeFullUAh);
        }
        this.mMaxLearnedBatteryCapacity = Math.max(this.mMaxLearnedBatteryCapacity, chargeFullUAh);
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
        Slog.wtf(TAG, "numSteps > steps.length, numSteps = " + numSteps + ",steps.length = " + steps.length);
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

    public void onCleanupUserLocked(int userId) {
        int firstUidForUser = UserHandle.getUid(userId, 0);
        int lastUidForUser = UserHandle.getUid(userId, 99999);
        this.mKernelUidCpuFreqTimeReader.removeUidsInRange(firstUidForUser, lastUidForUser);
        this.mKernelUidCpuTimeReader.removeUidsInRange(firstUidForUser, lastUidForUser);
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
        this.mKernelUidCpuTimeReader.removeUid(uid);
        this.mKernelUidCpuFreqTimeReader.removeUid(uid);
        this.mUidStats.remove(uid);
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

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void commitPendingDataToDisk() {
        synchronized (this) {
            Parcel next = this.mPendingWrite;
            this.mPendingWrite = null;
            if (next == null) {
            }
        }
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
                    addHistoryBufferLocked(elapsedRealtime, uptime, (byte) 4, this.mHistoryCur);
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
        long historyBaseTime = in.readLong();
        this.mHistoryBuffer.setDataSize(0);
        this.mHistoryBuffer.setDataPosition(0);
        this.mHistoryTagPool.clear();
        this.mNextHistoryTagIdx = 0;
        this.mNumHistoryTagChars = 0;
        int numTags = in.readInt();
        for (int i = 0; i < numTags; i++) {
            int idx = in.readInt();
            String str = in.readString();
            if (str == null) {
                throw new ParcelFormatException("null history tag string");
            }
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
        }
        int bufSize = in.readInt();
        int curPos = in.dataPosition();
        if (bufSize >= MAX_MAX_HISTORY_BUFFER * 3) {
            throw new ParcelFormatException("File corrupt: history data buffer too large " + bufSize);
        } else if ((bufSize & -4) != bufSize) {
            throw new ParcelFormatException("File corrupt: history data buffer not aligned " + bufSize);
        } else {
            this.mHistoryBuffer.appendFrom(in, curPos, bufSize);
            in.setDataPosition(curPos + bufSize);
            if (andOldHistory) {
                readOldHistory(in);
            }
            this.mHistoryBaseTime = historyBaseTime;
            if (this.mHistoryBaseTime > 0) {
                this.mHistoryBaseTime = (this.mHistoryBaseTime - this.mClocks.elapsedRealtime()) + 1;
            }
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
        int version = in.readInt();
        if (version != 167) {
            Slog.w("BatteryStats", "readFromParcel: version got " + version + ", expected " + 167 + "; erasing old stats");
            return;
        }
        int i;
        readHistory(in, true);
        this.mStartCount = in.readInt();
        this.mUptime = in.readLong();
        this.mRealtime = in.readLong();
        this.mStartClockTime = in.readLong();
        this.mStartPlatformVersion = in.readString();
        this.mEndPlatformVersion = in.readString();
        this.mOnBatteryTimeBase.readSummaryFromParcel(in);
        this.mOnBatteryScreenOffTimeBase.readSummaryFromParcel(in);
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
        this.mDischargeStepTracker.readFromParcel(in);
        this.mChargeStepTracker.readFromParcel(in);
        this.mDailyDischargeStepTracker.readFromParcel(in);
        this.mDailyChargeStepTracker.readFromParcel(in);
        this.mDischargeCounter.readSummaryFromParcelLocked(in);
        this.mDischargeScreenOffCounter.readSummaryFromParcelLocked(in);
        this.mDischargeScreenDozeCounter.readSummaryFromParcelLocked(in);
        int NPKG = in.readInt();
        if (NPKG > 0) {
            this.mDailyPackageChanges = new ArrayList(NPKG);
            while (NPKG > 0) {
                NPKG--;
                PackageChange pc = new PackageChange();
                pc.mPackageName = in.readString();
                pc.mUpdate = in.readInt() != 0;
                pc.mVersionCode = in.readInt();
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
        this.mScreenOnTimer.readSummaryFromParcelLocked(in);
        this.mScreenDozeTimer.readSummaryFromParcelLocked(in);
        for (i = 0; i < 5; i++) {
            this.mScreenBrightnessTimer[i].readSummaryFromParcelLocked(in);
        }
        this.mInteractive = false;
        this.mInteractiveTimer.readSummaryFromParcelLocked(in);
        this.mPhoneOn = false;
        this.mPowerSaveModeEnabledTimer.readSummaryFromParcelLocked(in);
        this.mLongestLightIdleTime = in.readLong();
        this.mLongestFullIdleTime = in.readLong();
        this.mDeviceIdleModeLightTimer.readSummaryFromParcelLocked(in);
        this.mDeviceIdleModeFullTimer.readSummaryFromParcelLocked(in);
        this.mDeviceLightIdlingTimer.readSummaryFromParcelLocked(in);
        this.mDeviceIdlingTimer.readSummaryFromParcelLocked(in);
        this.mPhoneOnTimer.readSummaryFromParcelLocked(in);
        for (i = 0; i < 5; i++) {
            this.mPhoneSignalStrengthsTimer[i].readSummaryFromParcelLocked(in);
        }
        this.mPhoneSignalScanningTimer.readSummaryFromParcelLocked(in);
        for (i = 0; i < 17; i++) {
            this.mPhoneDataConnectionsTimer[i].readSummaryFromParcelLocked(in);
        }
        for (i = 0; i < 10; i++) {
            this.mNetworkByteActivityCounters[i].readSummaryFromParcelLocked(in);
            this.mNetworkPacketActivityCounters[i].readSummaryFromParcelLocked(in);
        }
        this.mMobileRadioPowerState = 1;
        this.mMobileRadioActiveTimer.readSummaryFromParcelLocked(in);
        this.mMobileRadioActivePerAppTimer.readSummaryFromParcelLocked(in);
        this.mMobileRadioActiveAdjustedTime.readSummaryFromParcelLocked(in);
        this.mMobileRadioActiveUnknownTime.readSummaryFromParcelLocked(in);
        this.mMobileRadioActiveUnknownCount.readSummaryFromParcelLocked(in);
        this.mWifiRadioPowerState = 1;
        this.mWifiOn = false;
        this.mWifiOnTimer.readSummaryFromParcelLocked(in);
        this.mGlobalWifiRunning = false;
        this.mGlobalWifiRunningTimer.readSummaryFromParcelLocked(in);
        for (i = 0; i < 8; i++) {
            this.mWifiStateTimer[i].readSummaryFromParcelLocked(in);
        }
        for (i = 0; i < 13; i++) {
            this.mWifiSupplStateTimer[i].readSummaryFromParcelLocked(in);
        }
        for (i = 0; i < 5; i++) {
            this.mWifiSignalStrengthsTimer[i].readSummaryFromParcelLocked(in);
        }
        this.mWifiActivity.readSummaryFromParcel(in);
        this.mBluetoothActivity.readSummaryFromParcel(in);
        this.mModemActivity.readSummaryFromParcel(in);
        this.mHasWifiReporting = in.readInt() != 0;
        this.mHasBluetoothReporting = in.readInt() != 0;
        this.mHasModemReporting = in.readInt() != 0;
        int readInt = in.readInt();
        this.mLoadedNumConnectivityChange = readInt;
        this.mNumConnectivityChange = readInt;
        this.mFlashlightOnNesting = 0;
        this.mFlashlightOnTimer.readSummaryFromParcelLocked(in);
        this.mCameraOnNesting = 0;
        this.mCameraOnTimer.readSummaryFromParcelLocked(in);
        this.mBluetoothScanNesting = 0;
        this.mBluetoothScanTimer.readSummaryFromParcelLocked(in);
        int NRPMS = in.readInt();
        if (NRPMS > 10000) {
            throw new ParcelFormatException("File corrupt: too many rpm stats " + NRPMS);
        }
        int irpm;
        for (irpm = 0; irpm < NRPMS; irpm++) {
            if (in.readInt() != 0) {
                getRpmTimerLocked(in.readString()).readSummaryFromParcelLocked(in);
            }
        }
        int NSORPMS = in.readInt();
        if (NSORPMS > 10000) {
            throw new ParcelFormatException("File corrupt: too many screen-off rpm stats " + NSORPMS);
        }
        for (irpm = 0; irpm < NSORPMS; irpm++) {
            if (in.readInt() != 0) {
                getScreenOffRpmTimerLocked(in.readString()).readSummaryFromParcelLocked(in);
            }
        }
        int NKW = in.readInt();
        if (NKW > 10000) {
            throw new ParcelFormatException("File corrupt: too many kernel wake locks " + NKW);
        }
        for (int ikw = 0; ikw < NKW; ikw++) {
            if (in.readInt() != 0) {
                getKernelWakelockTimerLocked(in.readString()).readSummaryFromParcelLocked(in);
            }
        }
        int NWR = in.readInt();
        if (NWR > 10000) {
            throw new ParcelFormatException("File corrupt: too many wakeup reasons " + NWR);
        }
        for (int iwr = 0; iwr < NWR; iwr++) {
            if (in.readInt() != 0) {
                getWakeupReasonTimerLocked(in.readString()).readSummaryFromParcelLocked(in);
            }
        }
        int NMS = in.readInt();
        for (int ims = 0; ims < NMS; ims++) {
            if (in.readInt() != 0) {
                getKernelMemoryTimerLocked(in.readLong()).readSummaryFromParcelLocked(in);
            }
        }
        this.mCpuFreqs = in.createLongArray();
        int NU = in.readInt();
        if (NU > 10000) {
            throw new ParcelFormatException("File corrupt: too many uids " + NU);
        }
        for (int iu = 0; iu < NU; iu++) {
            int uid = in.readInt();
            Uid uid2 = new Uid(this, uid);
            this.mUidStats.put(uid, uid2);
            uid2.mOnBatteryBackgroundTimeBase.readSummaryFromParcel(in);
            uid2.mOnBatteryScreenOffBackgroundTimeBase.readSummaryFromParcel(in);
            uid2.mWifiRunning = false;
            if (in.readInt() != 0) {
                uid2.mWifiRunningTimer.readSummaryFromParcelLocked(in);
            }
            uid2.mFullWifiLockOut = false;
            if (in.readInt() != 0) {
                uid2.mFullWifiLockTimer.readSummaryFromParcelLocked(in);
            }
            uid2.mWifiScanStarted = false;
            if (in.readInt() != 0) {
                uid2.mWifiScanTimer.readSummaryFromParcelLocked(in);
            }
            uid2.mWifiBatchedScanBinStarted = -1;
            for (i = 0; i < 5; i++) {
                if (in.readInt() != 0) {
                    uid2.makeWifiBatchedScanBin(i, null);
                    uid2.mWifiBatchedScanTimer[i].readSummaryFromParcelLocked(in);
                }
            }
            uid2.mWifiMulticastEnabled = false;
            if (in.readInt() != 0) {
                uid2.mWifiMulticastTimer.readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                uid2.createAudioTurnedOnTimerLocked().readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                uid2.createVideoTurnedOnTimerLocked().readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                uid2.createFlashlightTurnedOnTimerLocked().readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                uid2.createCameraTurnedOnTimerLocked().readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                uid2.createForegroundActivityTimerLocked().readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                uid2.createForegroundServiceTimerLocked().readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                uid2.createAggregatedPartialWakelockTimerLocked().readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                uid2.createBluetoothScanTimerLocked().readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                uid2.createBluetoothUnoptimizedScanTimerLocked().readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                uid2.createBluetoothScanResultCounterLocked().readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                uid2.createBluetoothScanResultBgCounterLocked().readSummaryFromParcelLocked(in);
            }
            uid2.mProcessState = 18;
            for (i = 0; i < 6; i++) {
                if (in.readInt() != 0) {
                    uid2.makeProcessState(i, null);
                    uid2.mProcessStateTimer[i].readSummaryFromParcelLocked(in);
                }
            }
            if (in.readInt() != 0) {
                uid2.createVibratorOnTimerLocked().readSummaryFromParcelLocked(in);
            }
            if (in.readInt() != 0) {
                if (uid2.mUserActivityCounters == null) {
                    uid2.initUserActivityLocked();
                }
                for (i = 0; i < 4; i++) {
                    uid2.mUserActivityCounters[i].readSummaryFromParcelLocked(in);
                }
            }
            if (in.readInt() != 0) {
                if (uid2.mNetworkByteActivityCounters == null) {
                    uid2.initNetworkActivityLocked();
                }
                for (i = 0; i < 10; i++) {
                    uid2.mNetworkByteActivityCounters[i].readSummaryFromParcelLocked(in);
                    uid2.mNetworkPacketActivityCounters[i].readSummaryFromParcelLocked(in);
                }
                uid2.mMobileRadioActiveTime.readSummaryFromParcelLocked(in);
                uid2.mMobileRadioActiveCount.readSummaryFromParcelLocked(in);
            }
            uid2.mUserCpuTime.readSummaryFromParcelLocked(in);
            uid2.mSystemCpuTime.readSummaryFromParcelLocked(in);
            if (in.readInt() != 0) {
                int numClusters = in.readInt();
                if (this.mPowerProfile == null || this.mPowerProfile.getNumCpuClusters() == numClusters) {
                    uid2.mCpuClusterSpeedTimesUs = new LongSamplingCounter[numClusters][];
                    int cluster = 0;
                    while (cluster < numClusters) {
                        if (in.readInt() != 0) {
                            int NSB = in.readInt();
                            if (this.mPowerProfile == null || this.mPowerProfile.getNumSpeedStepsInCpuCluster(cluster) == NSB) {
                                uid2.mCpuClusterSpeedTimesUs[cluster] = new LongSamplingCounter[NSB];
                                for (int speed = 0; speed < NSB; speed++) {
                                    if (in.readInt() != 0) {
                                        uid2.mCpuClusterSpeedTimesUs[cluster][speed] = new LongSamplingCounter(this.mOnBatteryTimeBase);
                                        uid2.mCpuClusterSpeedTimesUs[cluster][speed].readSummaryFromParcelLocked(in);
                                    }
                                }
                            } else {
                                throw new ParcelFormatException("File corrupt: too many speed bins " + NSB);
                            }
                        }
                        uid2.mCpuClusterSpeedTimesUs[cluster] = null;
                        cluster++;
                    }
                } else {
                    throw new ParcelFormatException("Incompatible cpu cluster arrangement");
                }
            }
            uid2.mCpuClusterSpeedTimesUs = null;
            uid2.mCpuFreqTimeMs = LongSamplingCounterArray.readSummaryFromParcelLocked(in, this.mOnBatteryTimeBase);
            uid2.mScreenOffCpuFreqTimeMs = LongSamplingCounterArray.readSummaryFromParcelLocked(in, this.mOnBatteryScreenOffTimeBase);
            if (in.readInt() != 0) {
                uid2.mMobileRadioApWakeupCount = new LongSamplingCounter(this.mOnBatteryTimeBase);
                uid2.mMobileRadioApWakeupCount.readSummaryFromParcelLocked(in);
            } else {
                uid2.mMobileRadioApWakeupCount = null;
            }
            if (in.readInt() != 0) {
                uid2.mWifiRadioApWakeupCount = new LongSamplingCounter(this.mOnBatteryTimeBase);
                uid2.mWifiRadioApWakeupCount.readSummaryFromParcelLocked(in);
            } else {
                uid2.mWifiRadioApWakeupCount = null;
            }
            int NW = in.readInt();
            if (NW > MAX_WAKELOCKS_PER_UID + 1) {
                Slog.i(TAG, "NW > " + (MAX_WAKELOCKS_PER_UID + 1) + ", uid: " + uid);
                throw new ParcelFormatException("File corrupt: too many wake locks " + NW);
            }
            for (int iw = 0; iw < NW; iw++) {
                uid2.readWakeSummaryFromParcelLocked(in.readString(), in);
            }
            int NS = in.readInt();
            if (NS > MAX_WAKELOCKS_PER_UID + 1) {
                throw new ParcelFormatException("File corrupt: too many syncs " + NS);
            }
            int is;
            for (is = 0; is < NS; is++) {
                uid2.readSyncSummaryFromParcelLocked(in.readString(), in);
            }
            int NJ = in.readInt();
            if (NJ > MAX_WAKELOCKS_PER_UID + 1) {
                throw new ParcelFormatException("File corrupt: too many job timers " + NJ);
            }
            for (int ij = 0; ij < NJ; ij++) {
                uid2.readJobSummaryFromParcelLocked(in.readString(), in);
            }
            uid2.readJobCompletionsFromParcelLocked(in);
            int NP = in.readInt();
            if (NP > 1000) {
                throw new ParcelFormatException("File corrupt: too many sensors " + NP);
            }
            for (is = 0; is < NP; is++) {
                int seNumber = in.readInt();
                if (in.readInt() != 0) {
                    uid2.getSensorTimerLocked(seNumber, true).readSummaryFromParcelLocked(in);
                }
            }
            NP = in.readInt();
            if (NP > 1000) {
                throw new ParcelFormatException("File corrupt: too many processes " + NP);
            }
            int ip;
            for (ip = 0; ip < NP; ip++) {
                Proc p = uid2.getProcessStatsLocked(in.readString());
                long readLong = in.readLong();
                p.mLoadedUserTime = readLong;
                p.mUserTime = readLong;
                readLong = in.readLong();
                p.mLoadedSystemTime = readLong;
                p.mSystemTime = readLong;
                readLong = in.readLong();
                p.mLoadedForegroundTime = readLong;
                p.mForegroundTime = readLong;
                readInt = in.readInt();
                p.mLoadedStarts = readInt;
                p.mStarts = readInt;
                readInt = in.readInt();
                p.mLoadedNumCrashes = readInt;
                p.mNumCrashes = readInt;
                readInt = in.readInt();
                p.mLoadedNumAnrs = readInt;
                p.mNumAnrs = readInt;
                p.readExcessivePowerFromParcelLocked(in);
            }
            NP = in.readInt();
            if (NP > 10000) {
                throw new ParcelFormatException("File corrupt: too many packages " + NP);
            }
            for (ip = 0; ip < NP; ip++) {
                String pkgName = in.readString();
                Pkg p2 = uid2.getPackageStatsLocked(pkgName);
                int NWA = in.readInt();
                if (NWA > 1000) {
                    throw new ParcelFormatException("File corrupt: too many wakeup alarms " + NWA);
                }
                p2.mWakeupAlarms.clear();
                for (int iwa = 0; iwa < NWA; iwa++) {
                    String tag = in.readString();
                    Counter counter = new Counter(this.mOnBatteryScreenOffTimeBase);
                    counter.readSummaryFromParcelLocked(in);
                    p2.mWakeupAlarms.put(tag, counter);
                }
                NS = in.readInt();
                if (NS > 1000) {
                    throw new ParcelFormatException("File corrupt: too many services " + NS);
                }
                for (is = 0; is < NS; is++) {
                    Serv s = uid2.getServiceStatsLocked(pkgName, in.readString());
                    readLong = in.readLong();
                    s.mLoadedStartTime = readLong;
                    s.mStartTime = readLong;
                    readInt = in.readInt();
                    s.mLoadedStarts = readInt;
                    s.mStarts = readInt;
                    readInt = in.readInt();
                    s.mLoadedLaunches = readInt;
                    s.mLaunches = readInt;
                }
            }
        }
    }

    public void writeSummaryToParcel(Parcel out, boolean inclHistory) {
        int i;
        pullPendingStateUpdatesLocked();
        long startClockTime = getStartClockTime();
        long NOW_SYS = this.mClocks.uptimeMillis() * 1000;
        long NOWREAL_SYS = this.mClocks.elapsedRealtime() * 1000;
        out.writeInt(167);
        writeHistory(out, inclHistory, true);
        out.writeInt(this.mStartCount);
        out.writeLong(computeUptime(NOW_SYS, 0));
        out.writeLong(computeRealtime(NOWREAL_SYS, 0));
        out.writeLong(startClockTime);
        out.writeString(this.mStartPlatformVersion);
        out.writeString(this.mEndPlatformVersion);
        this.mOnBatteryTimeBase.writeSummaryToParcel(out, NOW_SYS, NOWREAL_SYS);
        this.mOnBatteryScreenOffTimeBase.writeSummaryToParcel(out, NOW_SYS, NOWREAL_SYS);
        out.writeInt(this.mDischargeUnplugLevel);
        out.writeInt(this.mDischargePlugLevel);
        out.writeInt(this.mDischargeCurrentLevel);
        out.writeInt(this.mCurrentBatteryLevel);
        out.writeInt(this.mEstimatedBatteryCapacity);
        out.writeInt(this.mMinLearnedBatteryCapacity);
        out.writeInt(this.mMaxLearnedBatteryCapacity);
        out.writeInt(getLowDischargeAmountSinceCharge());
        out.writeInt(getHighDischargeAmountSinceCharge());
        out.writeInt(getDischargeAmountScreenOnSinceCharge());
        out.writeInt(getDischargeAmountScreenOffSinceCharge());
        out.writeInt(getDischargeAmountScreenDozeSinceCharge());
        this.mDischargeStepTracker.writeToParcel(out);
        this.mChargeStepTracker.writeToParcel(out);
        this.mDailyDischargeStepTracker.writeToParcel(out);
        this.mDailyChargeStepTracker.writeToParcel(out);
        this.mDischargeCounter.writeSummaryFromParcelLocked(out);
        this.mDischargeScreenOffCounter.writeSummaryFromParcelLocked(out);
        this.mDischargeScreenDozeCounter.writeSummaryFromParcelLocked(out);
        if (this.mDailyPackageChanges != null) {
            int NPKG = this.mDailyPackageChanges.size();
            out.writeInt(NPKG);
            for (i = 0; i < NPKG; i++) {
                PackageChange pc = (PackageChange) this.mDailyPackageChanges.get(i);
                out.writeString(pc.mPackageName);
                out.writeInt(pc.mUpdate ? 1 : 0);
                out.writeInt(pc.mVersionCode);
            }
        } else {
            out.writeInt(0);
        }
        out.writeLong(this.mDailyStartTime);
        out.writeLong(this.mNextMinDailyDeadline);
        out.writeLong(this.mNextMaxDailyDeadline);
        this.mScreenOnTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        this.mScreenDozeTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        for (i = 0; i < 5; i++) {
            this.mScreenBrightnessTimer[i].writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        }
        this.mInteractiveTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        this.mPowerSaveModeEnabledTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        out.writeLong(this.mLongestLightIdleTime);
        out.writeLong(this.mLongestFullIdleTime);
        this.mDeviceIdleModeLightTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        this.mDeviceIdleModeFullTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        this.mDeviceLightIdlingTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        this.mDeviceIdlingTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        this.mPhoneOnTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        for (i = 0; i < 5; i++) {
            this.mPhoneSignalStrengthsTimer[i].writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        }
        this.mPhoneSignalScanningTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        for (i = 0; i < 17; i++) {
            this.mPhoneDataConnectionsTimer[i].writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        }
        for (i = 0; i < 10; i++) {
            this.mNetworkByteActivityCounters[i].writeSummaryFromParcelLocked(out);
            this.mNetworkPacketActivityCounters[i].writeSummaryFromParcelLocked(out);
        }
        this.mMobileRadioActiveTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        this.mMobileRadioActivePerAppTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        this.mMobileRadioActiveAdjustedTime.writeSummaryFromParcelLocked(out);
        this.mMobileRadioActiveUnknownTime.writeSummaryFromParcelLocked(out);
        this.mMobileRadioActiveUnknownCount.writeSummaryFromParcelLocked(out);
        this.mWifiOnTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        this.mGlobalWifiRunningTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        for (i = 0; i < 8; i++) {
            this.mWifiStateTimer[i].writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        }
        for (i = 0; i < 13; i++) {
            this.mWifiSupplStateTimer[i].writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        }
        for (i = 0; i < 5; i++) {
            this.mWifiSignalStrengthsTimer[i].writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        }
        this.mWifiActivity.writeSummaryToParcel(out);
        this.mBluetoothActivity.writeSummaryToParcel(out);
        this.mModemActivity.writeSummaryToParcel(out);
        out.writeInt(this.mHasWifiReporting ? 1 : 0);
        out.writeInt(this.mHasBluetoothReporting ? 1 : 0);
        out.writeInt(this.mHasModemReporting ? 1 : 0);
        out.writeInt(this.mNumConnectivityChange);
        this.mFlashlightOnTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        this.mCameraOnTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        this.mBluetoothScanTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
        out.writeInt(this.mRpmStats.size());
        for (Entry<String, SamplingTimer> ent : this.mRpmStats.entrySet()) {
            Timer rpmt = (Timer) ent.getValue();
            if (rpmt != null) {
                out.writeInt(1);
                out.writeString((String) ent.getKey());
                rpmt.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            } else {
                out.writeInt(0);
            }
        }
        out.writeInt(this.mScreenOffRpmStats.size());
        for (Entry<String, SamplingTimer> ent2 : this.mScreenOffRpmStats.entrySet()) {
            rpmt = (Timer) ent2.getValue();
            if (rpmt != null) {
                out.writeInt(1);
                out.writeString((String) ent2.getKey());
                rpmt.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            } else {
                out.writeInt(0);
            }
        }
        out.writeInt(this.mKernelWakelockStats.size());
        for (Entry<String, SamplingTimer> ent22 : this.mKernelWakelockStats.entrySet()) {
            Timer kwlt = (Timer) ent22.getValue();
            if (kwlt != null) {
                out.writeInt(1);
                out.writeString((String) ent22.getKey());
                kwlt.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            } else {
                out.writeInt(0);
            }
        }
        out.writeInt(this.mWakeupReasonStats.size());
        for (Entry<String, SamplingTimer> ent222 : this.mWakeupReasonStats.entrySet()) {
            SamplingTimer timer = (SamplingTimer) ent222.getValue();
            if (timer != null) {
                out.writeInt(1);
                out.writeString((String) ent222.getKey());
                timer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            } else {
                out.writeInt(0);
            }
        }
        out.writeInt(this.mKernelMemoryStats.size());
        for (i = 0; i < this.mKernelMemoryStats.size(); i++) {
            Timer kmt = (Timer) this.mKernelMemoryStats.valueAt(i);
            if (kmt != null) {
                out.writeInt(1);
                out.writeLong(this.mKernelMemoryStats.keyAt(i));
                kmt.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            } else {
                out.writeInt(0);
            }
        }
        out.writeLongArray(this.mCpuFreqs);
        int NU = this.mUidStats.size();
        out.writeInt(NU);
        for (int iu = 0; iu < NU; iu++) {
            int is;
            out.writeInt(this.mUidStats.keyAt(iu));
            Uid u = (Uid) this.mUidStats.valueAt(iu);
            u.mOnBatteryBackgroundTimeBase.writeSummaryToParcel(out, NOW_SYS, NOWREAL_SYS);
            u.mOnBatteryScreenOffBackgroundTimeBase.writeSummaryToParcel(out, NOW_SYS, NOWREAL_SYS);
            if (u.mWifiRunningTimer != null) {
                out.writeInt(1);
                u.mWifiRunningTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            } else {
                out.writeInt(0);
            }
            if (u.mFullWifiLockTimer != null) {
                out.writeInt(1);
                u.mFullWifiLockTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            } else {
                out.writeInt(0);
            }
            if (u.mWifiScanTimer != null) {
                out.writeInt(1);
                u.mWifiScanTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            } else {
                out.writeInt(0);
            }
            for (i = 0; i < 5; i++) {
                if (u.mWifiBatchedScanTimer[i] != null) {
                    out.writeInt(1);
                    u.mWifiBatchedScanTimer[i].writeSummaryFromParcelLocked(out, NOWREAL_SYS);
                } else {
                    out.writeInt(0);
                }
            }
            if (u.mWifiMulticastTimer != null) {
                out.writeInt(1);
                u.mWifiMulticastTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            } else {
                out.writeInt(0);
            }
            if (u.mAudioTurnedOnTimer != null) {
                out.writeInt(1);
                u.mAudioTurnedOnTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            } else {
                out.writeInt(0);
            }
            if (u.mVideoTurnedOnTimer != null) {
                out.writeInt(1);
                u.mVideoTurnedOnTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            } else {
                out.writeInt(0);
            }
            if (u.mFlashlightTurnedOnTimer != null) {
                out.writeInt(1);
                u.mFlashlightTurnedOnTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            } else {
                out.writeInt(0);
            }
            if (u.mCameraTurnedOnTimer != null) {
                out.writeInt(1);
                u.mCameraTurnedOnTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            } else {
                out.writeInt(0);
            }
            if (u.mForegroundActivityTimer != null) {
                out.writeInt(1);
                u.mForegroundActivityTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            } else {
                out.writeInt(0);
            }
            if (u.mForegroundServiceTimer != null) {
                out.writeInt(1);
                u.mForegroundServiceTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            } else {
                out.writeInt(0);
            }
            if (u.mAggregatedPartialWakelockTimer != null) {
                out.writeInt(1);
                u.mAggregatedPartialWakelockTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            } else {
                out.writeInt(0);
            }
            if (u.mBluetoothScanTimer != null) {
                out.writeInt(1);
                u.mBluetoothScanTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            } else {
                out.writeInt(0);
            }
            if (u.mBluetoothUnoptimizedScanTimer != null) {
                out.writeInt(1);
                u.mBluetoothUnoptimizedScanTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            } else {
                out.writeInt(0);
            }
            if (u.mBluetoothScanResultCounter != null) {
                out.writeInt(1);
                u.mBluetoothScanResultCounter.writeSummaryFromParcelLocked(out);
            } else {
                out.writeInt(0);
            }
            if (u.mBluetoothScanResultBgCounter != null) {
                out.writeInt(1);
                u.mBluetoothScanResultBgCounter.writeSummaryFromParcelLocked(out);
            } else {
                out.writeInt(0);
            }
            for (i = 0; i < 6; i++) {
                if (u.mProcessStateTimer[i] != null) {
                    out.writeInt(1);
                    u.mProcessStateTimer[i].writeSummaryFromParcelLocked(out, NOWREAL_SYS);
                } else {
                    out.writeInt(0);
                }
            }
            if (u.mVibratorOnTimer != null) {
                out.writeInt(1);
                u.mVibratorOnTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            } else {
                out.writeInt(0);
            }
            if (u.mUserActivityCounters == null) {
                out.writeInt(0);
            } else {
                out.writeInt(1);
                for (i = 0; i < 4; i++) {
                    u.mUserActivityCounters[i].writeSummaryFromParcelLocked(out);
                }
            }
            if (u.mNetworkByteActivityCounters == null) {
                out.writeInt(0);
            } else {
                out.writeInt(1);
                for (i = 0; i < 10; i++) {
                    u.mNetworkByteActivityCounters[i].writeSummaryFromParcelLocked(out);
                    u.mNetworkPacketActivityCounters[i].writeSummaryFromParcelLocked(out);
                }
                u.mMobileRadioActiveTime.writeSummaryFromParcelLocked(out);
                u.mMobileRadioActiveCount.writeSummaryFromParcelLocked(out);
            }
            u.mUserCpuTime.writeSummaryFromParcelLocked(out);
            u.mSystemCpuTime.writeSummaryFromParcelLocked(out);
            if (u.mCpuClusterSpeedTimesUs != null) {
                out.writeInt(1);
                out.writeInt(u.mCpuClusterSpeedTimesUs.length);
                for (LongSamplingCounter[] cpuSpeeds : u.mCpuClusterSpeedTimesUs) {
                    if (cpuSpeeds != null) {
                        out.writeInt(1);
                        out.writeInt(cpuSpeeds.length);
                        for (LongSamplingCounter c : cpuSpeeds) {
                            if (c != null) {
                                out.writeInt(1);
                                c.writeSummaryFromParcelLocked(out);
                            } else {
                                out.writeInt(0);
                            }
                        }
                    } else {
                        out.writeInt(0);
                    }
                }
            } else {
                out.writeInt(0);
            }
            LongSamplingCounterArray.writeSummaryToParcelLocked(out, u.mCpuFreqTimeMs);
            LongSamplingCounterArray.writeSummaryToParcelLocked(out, u.mScreenOffCpuFreqTimeMs);
            if (u.mMobileRadioApWakeupCount != null) {
                out.writeInt(1);
                u.mMobileRadioApWakeupCount.writeSummaryFromParcelLocked(out);
            } else {
                out.writeInt(0);
            }
            if (u.mWifiRadioApWakeupCount != null) {
                out.writeInt(1);
                u.mWifiRadioApWakeupCount.writeSummaryFromParcelLocked(out);
            } else {
                out.writeInt(0);
            }
            ArrayMap<String, Wakelock> wakeStats = u.mWakelockStats.getMap();
            int NW = wakeStats.size();
            out.writeInt(NW);
            for (int iw = 0; iw < NW; iw++) {
                out.writeString((String) wakeStats.keyAt(iw));
                Wakelock wl = (Wakelock) wakeStats.valueAt(iw);
                if (wl.mTimerFull != null) {
                    out.writeInt(1);
                    wl.mTimerFull.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
                } else {
                    out.writeInt(0);
                }
                if (wl.mTimerPartial != null) {
                    out.writeInt(1);
                    wl.mTimerPartial.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
                } else {
                    out.writeInt(0);
                }
                if (wl.mTimerWindow != null) {
                    out.writeInt(1);
                    wl.mTimerWindow.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
                } else {
                    out.writeInt(0);
                }
                if (wl.mTimerDraw != null) {
                    out.writeInt(1);
                    wl.mTimerDraw.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
                } else {
                    out.writeInt(0);
                }
            }
            ArrayMap<String, DualTimer> syncStats = u.mSyncStats.getMap();
            int NS = syncStats.size();
            out.writeInt(NS);
            for (is = 0; is < NS; is++) {
                out.writeString((String) syncStats.keyAt(is));
                ((DualTimer) syncStats.valueAt(is)).writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            }
            ArrayMap<String, DualTimer> jobStats = u.mJobStats.getMap();
            int NJ = jobStats.size();
            out.writeInt(NJ);
            for (int ij = 0; ij < NJ; ij++) {
                out.writeString((String) jobStats.keyAt(ij));
                ((DualTimer) jobStats.valueAt(ij)).writeSummaryFromParcelLocked(out, NOWREAL_SYS);
            }
            u.writeJobCompletionsToParcelLocked(out);
            int NSE = u.mSensorStats.size();
            out.writeInt(NSE);
            for (int ise = 0; ise < NSE; ise++) {
                out.writeInt(u.mSensorStats.keyAt(ise));
                Sensor se = (Sensor) u.mSensorStats.valueAt(ise);
                if (se.mTimer != null) {
                    out.writeInt(1);
                    se.mTimer.writeSummaryFromParcelLocked(out, NOWREAL_SYS);
                } else {
                    out.writeInt(0);
                }
            }
            int NP = u.mProcessStats.size();
            out.writeInt(NP);
            for (int ip = 0; ip < NP; ip++) {
                out.writeString((String) u.mProcessStats.keyAt(ip));
                Proc ps = (Proc) u.mProcessStats.valueAt(ip);
                out.writeLong(ps.mUserTime);
                out.writeLong(ps.mSystemTime);
                out.writeLong(ps.mForegroundTime);
                out.writeInt(ps.mStarts);
                out.writeInt(ps.mNumCrashes);
                out.writeInt(ps.mNumAnrs);
                ps.writeExcessivePowerToParcelLocked(out);
            }
            NP = u.mPackageStats.size();
            out.writeInt(NP);
            if (NP > 0) {
                for (Entry<String, Pkg> ent3 : u.mPackageStats.entrySet()) {
                    out.writeString((String) ent3.getKey());
                    Pkg ps2 = (Pkg) ent3.getValue();
                    int NWA = ps2.mWakeupAlarms.size();
                    out.writeInt(NWA);
                    for (int iwa = 0; iwa < NWA; iwa++) {
                        out.writeString((String) ps2.mWakeupAlarms.keyAt(iwa));
                        ((Counter) ps2.mWakeupAlarms.valueAt(iwa)).writeSummaryFromParcelLocked(out);
                    }
                    NS = ps2.mServiceStats.size();
                    out.writeInt(NS);
                    for (is = 0; is < NS; is++) {
                        out.writeString((String) ps2.mServiceStats.keyAt(is));
                        Serv ss = (Serv) ps2.mServiceStats.valueAt(is);
                        out.writeLong(ss.getStartTimeToNowLocked(this.mOnBatteryTimeBase.getUptime(NOW_SYS)));
                        out.writeInt(ss.mStarts);
                        out.writeInt(ss.mLaunches);
                    }
                }
            }
        }
    }

    public void readFromParcel(Parcel in) {
        readFromParcelLocked(in);
    }

    void readFromParcelLocked(Parcel in) {
        int magic = in.readInt();
        if (magic != MAGIC) {
            throw new ParcelFormatException("Bad magic number: #" + Integer.toHexString(magic));
        }
        int i;
        int irpm;
        readHistory(in, false);
        this.mStartCount = in.readInt();
        this.mStartClockTime = in.readLong();
        this.mStartPlatformVersion = in.readString();
        this.mEndPlatformVersion = in.readString();
        this.mUptime = in.readLong();
        this.mUptimeStart = in.readLong();
        this.mRealtime = in.readLong();
        this.mRealtimeStart = in.readLong();
        this.mOnBattery = in.readInt() != 0;
        this.mEstimatedBatteryCapacity = in.readInt();
        this.mMinLearnedBatteryCapacity = in.readInt();
        this.mMaxLearnedBatteryCapacity = in.readInt();
        this.mOnBatteryInternal = false;
        this.mOnBatteryTimeBase.readFromParcel(in);
        this.mOnBatteryScreenOffTimeBase.readFromParcel(in);
        this.mScreenState = 0;
        this.mScreenOnTimer = new StopwatchTimer(this.mClocks, null, -1, null, this.mOnBatteryTimeBase, in);
        this.mScreenDozeTimer = new StopwatchTimer(this.mClocks, null, -1, null, this.mOnBatteryTimeBase, in);
        for (i = 0; i < 5; i++) {
            this.mScreenBrightnessTimer[i] = new StopwatchTimer(this.mClocks, null, -100 - i, null, this.mOnBatteryTimeBase, in);
        }
        this.mInteractive = false;
        this.mInteractiveTimer = new StopwatchTimer(this.mClocks, null, -10, null, this.mOnBatteryTimeBase, in);
        this.mPhoneOn = false;
        this.mPowerSaveModeEnabledTimer = new StopwatchTimer(this.mClocks, null, -2, null, this.mOnBatteryTimeBase, in);
        this.mLongestLightIdleTime = in.readLong();
        this.mLongestFullIdleTime = in.readLong();
        this.mDeviceIdleModeLightTimer = new StopwatchTimer(this.mClocks, null, -14, null, this.mOnBatteryTimeBase, in);
        this.mDeviceIdleModeFullTimer = new StopwatchTimer(this.mClocks, null, -11, null, this.mOnBatteryTimeBase, in);
        this.mDeviceLightIdlingTimer = new StopwatchTimer(this.mClocks, null, -15, null, this.mOnBatteryTimeBase, in);
        this.mDeviceIdlingTimer = new StopwatchTimer(this.mClocks, null, -12, null, this.mOnBatteryTimeBase, in);
        this.mPhoneOnTimer = new StopwatchTimer(this.mClocks, null, -3, null, this.mOnBatteryTimeBase, in);
        for (i = 0; i < 5; i++) {
            this.mPhoneSignalStrengthsTimer[i] = new StopwatchTimer(this.mClocks, null, -200 - i, null, this.mOnBatteryTimeBase, in);
        }
        this.mPhoneSignalScanningTimer = new StopwatchTimer(this.mClocks, null, -199, null, this.mOnBatteryTimeBase, in);
        for (i = 0; i < 17; i++) {
            this.mPhoneDataConnectionsTimer[i] = new StopwatchTimer(this.mClocks, null, -300 - i, null, this.mOnBatteryTimeBase, in);
        }
        for (i = 0; i < 10; i++) {
            this.mNetworkByteActivityCounters[i] = new LongSamplingCounter(this.mOnBatteryTimeBase, in);
            this.mNetworkPacketActivityCounters[i] = new LongSamplingCounter(this.mOnBatteryTimeBase, in);
        }
        this.mMobileRadioPowerState = 1;
        this.mMobileRadioActiveTimer = new StopwatchTimer(this.mClocks, null, -400, null, this.mOnBatteryTimeBase, in);
        this.mMobileRadioActivePerAppTimer = new StopwatchTimer(this.mClocks, null, -401, null, this.mOnBatteryTimeBase, in);
        this.mMobileRadioActiveAdjustedTime = new LongSamplingCounter(this.mOnBatteryTimeBase, in);
        this.mMobileRadioActiveUnknownTime = new LongSamplingCounter(this.mOnBatteryTimeBase, in);
        this.mMobileRadioActiveUnknownCount = new LongSamplingCounter(this.mOnBatteryTimeBase, in);
        this.mWifiRadioPowerState = 1;
        this.mWifiOn = false;
        this.mWifiOnTimer = new StopwatchTimer(this.mClocks, null, -4, null, this.mOnBatteryTimeBase, in);
        this.mGlobalWifiRunning = false;
        this.mGlobalWifiRunningTimer = new StopwatchTimer(this.mClocks, null, -5, null, this.mOnBatteryTimeBase, in);
        for (i = 0; i < 8; i++) {
            this.mWifiStateTimer[i] = new StopwatchTimer(this.mClocks, null, -600 - i, null, this.mOnBatteryTimeBase, in);
        }
        for (i = 0; i < 13; i++) {
            this.mWifiSupplStateTimer[i] = new StopwatchTimer(this.mClocks, null, -700 - i, null, this.mOnBatteryTimeBase, in);
        }
        for (i = 0; i < 5; i++) {
            this.mWifiSignalStrengthsTimer[i] = new StopwatchTimer(this.mClocks, null, -800 - i, null, this.mOnBatteryTimeBase, in);
        }
        this.mWifiActivity = new ControllerActivityCounterImpl(this.mOnBatteryTimeBase, 1, in);
        this.mBluetoothActivity = new ControllerActivityCounterImpl(this.mOnBatteryTimeBase, 1, in);
        this.mModemActivity = new ControllerActivityCounterImpl(this.mOnBatteryTimeBase, 5, in);
        this.mHasWifiReporting = in.readInt() != 0;
        this.mHasBluetoothReporting = in.readInt() != 0;
        this.mHasModemReporting = in.readInt() != 0;
        this.mNumConnectivityChange = in.readInt();
        this.mLoadedNumConnectivityChange = in.readInt();
        this.mUnpluggedNumConnectivityChange = in.readInt();
        this.mAudioOnNesting = 0;
        this.mAudioOnTimer = new StopwatchTimer(this.mClocks, null, -7, null, this.mOnBatteryTimeBase);
        this.mVideoOnNesting = 0;
        this.mVideoOnTimer = new StopwatchTimer(this.mClocks, null, -8, null, this.mOnBatteryTimeBase);
        this.mFlashlightOnNesting = 0;
        this.mFlashlightOnTimer = new StopwatchTimer(this.mClocks, null, -9, null, this.mOnBatteryTimeBase, in);
        this.mCameraOnNesting = 0;
        this.mCameraOnTimer = new StopwatchTimer(this.mClocks, null, -13, null, this.mOnBatteryTimeBase, in);
        this.mBluetoothScanNesting = 0;
        this.mBluetoothScanTimer = new StopwatchTimer(this.mClocks, null, -14, null, this.mOnBatteryTimeBase, in);
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
        this.mDischargeStepTracker.readFromParcel(in);
        this.mChargeStepTracker.readFromParcel(in);
        this.mDischargeCounter = new LongSamplingCounter(this.mOnBatteryTimeBase, in);
        this.mDischargeScreenOffCounter = new LongSamplingCounter(this.mOnBatteryScreenOffTimeBase, in);
        this.mDischargeScreenDozeCounter = new LongSamplingCounter(this.mOnBatteryTimeBase, in);
        this.mLastWriteTime = in.readLong();
        this.mRpmStats.clear();
        int NRPMS = in.readInt();
        for (irpm = 0; irpm < NRPMS; irpm++) {
            if (in.readInt() != 0) {
                this.mRpmStats.put(in.readString(), new SamplingTimer(this.mClocks, this.mOnBatteryTimeBase, in));
            }
        }
        this.mScreenOffRpmStats.clear();
        int NSORPMS = in.readInt();
        for (irpm = 0; irpm < NSORPMS; irpm++) {
            if (in.readInt() != 0) {
                this.mScreenOffRpmStats.put(in.readString(), new SamplingTimer(this.mClocks, this.mOnBatteryScreenOffTimeBase, in));
            }
        }
        this.mKernelWakelockStats.clear();
        int NKW = in.readInt();
        for (int ikw = 0; ikw < NKW; ikw++) {
            if (in.readInt() != 0) {
                this.mKernelWakelockStats.put(in.readString(), new SamplingTimer(this.mClocks, this.mOnBatteryScreenOffTimeBase, in));
            }
        }
        this.mWakeupReasonStats.clear();
        int NWR = in.readInt();
        for (int iwr = 0; iwr < NWR; iwr++) {
            if (in.readInt() != 0) {
                this.mWakeupReasonStats.put(in.readString(), new SamplingTimer(this.mClocks, this.mOnBatteryTimeBase, in));
            }
        }
        this.mKernelMemoryStats.clear();
        int nmt = in.readInt();
        for (int imt = 0; imt < nmt; imt++) {
            if (in.readInt() != 0) {
                this.mKernelMemoryStats.put(Long.valueOf(in.readLong()).longValue(), new SamplingTimer(this.mClocks, this.mOnBatteryTimeBase, in));
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
        this.mCpuFreqs = in.createLongArray();
        int numUids = in.readInt();
        this.mUidStats.clear();
        for (i = 0; i < numUids; i++) {
            int uid = in.readInt();
            Uid uid2 = new Uid(this, uid);
            uid2.readFromParcelLocked(this.mOnBatteryTimeBase, this.mOnBatteryScreenOffTimeBase, in);
            this.mUidStats.append(uid, uid2);
        }
    }

    public void writeToParcel(Parcel out, int flags) {
        writeToParcelLocked(out, true, flags);
    }

    public void writeToParcelWithoutUids(Parcel out, int flags) {
        writeToParcelLocked(out, false, flags);
    }

    void writeToParcelLocked(Parcel out, boolean inclUids, int flags) {
        int i;
        pullPendingStateUpdatesLocked();
        long startClockTime = getStartClockTime();
        long uSecUptime = this.mClocks.uptimeMillis() * 1000;
        long uSecRealtime = this.mClocks.elapsedRealtime() * 1000;
        long batteryRealtime = this.mOnBatteryTimeBase.getRealtime(uSecRealtime);
        long batteryScreenOffRealtime = this.mOnBatteryScreenOffTimeBase.getRealtime(uSecRealtime);
        out.writeInt(MAGIC);
        writeHistory(out, true, false);
        out.writeInt(this.mStartCount);
        out.writeLong(startClockTime);
        out.writeString(this.mStartPlatformVersion);
        out.writeString(this.mEndPlatformVersion);
        out.writeLong(this.mUptime);
        out.writeLong(this.mUptimeStart);
        out.writeLong(this.mRealtime);
        out.writeLong(this.mRealtimeStart);
        out.writeInt(this.mOnBattery ? 1 : 0);
        out.writeInt(this.mEstimatedBatteryCapacity);
        out.writeInt(this.mMinLearnedBatteryCapacity);
        out.writeInt(this.mMaxLearnedBatteryCapacity);
        this.mOnBatteryTimeBase.writeToParcel(out, uSecUptime, uSecRealtime);
        this.mOnBatteryScreenOffTimeBase.writeToParcel(out, uSecUptime, uSecRealtime);
        this.mScreenOnTimer.writeToParcel(out, uSecRealtime);
        this.mScreenDozeTimer.writeToParcel(out, uSecRealtime);
        for (i = 0; i < 5; i++) {
            this.mScreenBrightnessTimer[i].writeToParcel(out, uSecRealtime);
        }
        this.mInteractiveTimer.writeToParcel(out, uSecRealtime);
        this.mPowerSaveModeEnabledTimer.writeToParcel(out, uSecRealtime);
        out.writeLong(this.mLongestLightIdleTime);
        out.writeLong(this.mLongestFullIdleTime);
        this.mDeviceIdleModeLightTimer.writeToParcel(out, uSecRealtime);
        this.mDeviceIdleModeFullTimer.writeToParcel(out, uSecRealtime);
        this.mDeviceLightIdlingTimer.writeToParcel(out, uSecRealtime);
        this.mDeviceIdlingTimer.writeToParcel(out, uSecRealtime);
        this.mPhoneOnTimer.writeToParcel(out, uSecRealtime);
        for (i = 0; i < 5; i++) {
            this.mPhoneSignalStrengthsTimer[i].writeToParcel(out, uSecRealtime);
        }
        this.mPhoneSignalScanningTimer.writeToParcel(out, uSecRealtime);
        for (i = 0; i < 17; i++) {
            this.mPhoneDataConnectionsTimer[i].writeToParcel(out, uSecRealtime);
        }
        for (i = 0; i < 10; i++) {
            this.mNetworkByteActivityCounters[i].writeToParcel(out);
            this.mNetworkPacketActivityCounters[i].writeToParcel(out);
        }
        this.mMobileRadioActiveTimer.writeToParcel(out, uSecRealtime);
        this.mMobileRadioActivePerAppTimer.writeToParcel(out, uSecRealtime);
        this.mMobileRadioActiveAdjustedTime.writeToParcel(out);
        this.mMobileRadioActiveUnknownTime.writeToParcel(out);
        this.mMobileRadioActiveUnknownCount.writeToParcel(out);
        this.mWifiOnTimer.writeToParcel(out, uSecRealtime);
        this.mGlobalWifiRunningTimer.writeToParcel(out, uSecRealtime);
        for (i = 0; i < 8; i++) {
            this.mWifiStateTimer[i].writeToParcel(out, uSecRealtime);
        }
        for (i = 0; i < 13; i++) {
            this.mWifiSupplStateTimer[i].writeToParcel(out, uSecRealtime);
        }
        for (i = 0; i < 5; i++) {
            this.mWifiSignalStrengthsTimer[i].writeToParcel(out, uSecRealtime);
        }
        this.mWifiActivity.writeToParcel(out, 0);
        this.mBluetoothActivity.writeToParcel(out, 0);
        this.mModemActivity.writeToParcel(out, 0);
        out.writeInt(this.mHasWifiReporting ? 1 : 0);
        out.writeInt(this.mHasBluetoothReporting ? 1 : 0);
        out.writeInt(this.mHasModemReporting ? 1 : 0);
        out.writeInt(this.mNumConnectivityChange);
        out.writeInt(this.mLoadedNumConnectivityChange);
        out.writeInt(this.mUnpluggedNumConnectivityChange);
        this.mFlashlightOnTimer.writeToParcel(out, uSecRealtime);
        this.mCameraOnTimer.writeToParcel(out, uSecRealtime);
        this.mBluetoothScanTimer.writeToParcel(out, uSecRealtime);
        out.writeInt(this.mDischargeUnplugLevel);
        out.writeInt(this.mDischargePlugLevel);
        out.writeInt(this.mDischargeCurrentLevel);
        out.writeInt(this.mCurrentBatteryLevel);
        out.writeInt(this.mLowDischargeAmountSinceCharge);
        out.writeInt(this.mHighDischargeAmountSinceCharge);
        out.writeInt(this.mDischargeAmountScreenOn);
        out.writeInt(this.mDischargeAmountScreenOnSinceCharge);
        out.writeInt(this.mDischargeAmountScreenOff);
        out.writeInt(this.mDischargeAmountScreenOffSinceCharge);
        out.writeInt(this.mDischargeAmountScreenDoze);
        out.writeInt(this.mDischargeAmountScreenDozeSinceCharge);
        this.mDischargeStepTracker.writeToParcel(out);
        this.mChargeStepTracker.writeToParcel(out);
        this.mDischargeCounter.writeToParcel(out);
        this.mDischargeScreenOffCounter.writeToParcel(out);
        this.mDischargeScreenDozeCounter.writeToParcel(out);
        out.writeLong(this.mLastWriteTime);
        out.writeInt(this.mRpmStats.size());
        for (Entry<String, SamplingTimer> ent : this.mRpmStats.entrySet()) {
            SamplingTimer rpmt = (SamplingTimer) ent.getValue();
            if (rpmt != null) {
                out.writeInt(1);
                out.writeString((String) ent.getKey());
                rpmt.writeToParcel(out, uSecRealtime);
            } else {
                out.writeInt(0);
            }
        }
        out.writeInt(this.mScreenOffRpmStats.size());
        for (Entry<String, SamplingTimer> ent2 : this.mScreenOffRpmStats.entrySet()) {
            rpmt = (SamplingTimer) ent2.getValue();
            if (rpmt != null) {
                out.writeInt(1);
                out.writeString((String) ent2.getKey());
                rpmt.writeToParcel(out, uSecRealtime);
            } else {
                out.writeInt(0);
            }
        }
        if (inclUids) {
            out.writeInt(this.mKernelWakelockStats.size());
            for (Entry<String, SamplingTimer> ent22 : this.mKernelWakelockStats.entrySet()) {
                SamplingTimer kwlt = (SamplingTimer) ent22.getValue();
                if (kwlt != null) {
                    out.writeInt(1);
                    out.writeString((String) ent22.getKey());
                    kwlt.writeToParcel(out, uSecRealtime);
                } else {
                    out.writeInt(0);
                }
            }
            out.writeInt(this.mWakeupReasonStats.size());
            for (Entry<String, SamplingTimer> ent222 : this.mWakeupReasonStats.entrySet()) {
                SamplingTimer timer = (SamplingTimer) ent222.getValue();
                if (timer != null) {
                    out.writeInt(1);
                    out.writeString((String) ent222.getKey());
                    timer.writeToParcel(out, uSecRealtime);
                } else {
                    out.writeInt(0);
                }
            }
        } else {
            out.writeInt(0);
        }
        out.writeInt(this.mKernelMemoryStats.size());
        for (i = 0; i < this.mKernelMemoryStats.size(); i++) {
            SamplingTimer kmt = (SamplingTimer) this.mKernelMemoryStats.valueAt(i);
            if (kmt != null) {
                out.writeInt(1);
                out.writeLong(this.mKernelMemoryStats.keyAt(i));
                kmt.writeToParcel(out, uSecRealtime);
            } else {
                out.writeInt(0);
            }
        }
        out.writeLongArray(this.mCpuFreqs);
        if (inclUids) {
            int size = this.mUidStats.size();
            out.writeInt(size);
            for (i = 0; i < size; i++) {
                out.writeInt(this.mUidStats.keyAt(i));
                ((Uid) this.mUidStats.valueAt(i)).writeToParcelLocked(out, uSecUptime, uSecRealtime);
            }
            return;
        }
        out.writeInt(0);
    }

    public void prepareForDumpLocked() {
        pullPendingStateUpdatesLocked();
        getStartClockTime();
    }

    public void dumpLocked(Context context, PrintWriter pw, int flags, int reqUid, long histStart) {
        super.dumpLocked(context, pw, flags, reqUid, histStart);
    }
}
