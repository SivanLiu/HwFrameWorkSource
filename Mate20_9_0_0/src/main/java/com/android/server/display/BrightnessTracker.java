package com.android.server.display;

import android.app.ActivityManager;
import android.app.ActivityManager.StackInfo;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ParceledListSlice;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.AmbientBrightnessDayStats;
import android.hardware.display.BrightnessChangeEvent;
import android.hardware.display.BrightnessChangeEvent.Builder;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserManager;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.RingBuffer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class BrightnessTracker {
    private static final String AMBIENT_BRIGHTNESS_STATS_FILE = "ambient_brightness_stats.xml";
    private static final String ATTR_BATTERY_LEVEL = "batteryLevel";
    private static final String ATTR_COLOR_TEMPERATURE = "colorTemperature";
    private static final String ATTR_DEFAULT_CONFIG = "defaultConfig";
    private static final String ATTR_LAST_NITS = "lastNits";
    private static final String ATTR_LUX = "lux";
    private static final String ATTR_LUX_TIMESTAMPS = "luxTimestamps";
    private static final String ATTR_NIGHT_MODE = "nightMode";
    private static final String ATTR_NITS = "nits";
    private static final String ATTR_PACKAGE_NAME = "packageName";
    private static final String ATTR_POWER_SAVE = "powerSaveFactor";
    private static final String ATTR_TIMESTAMP = "timestamp";
    private static final String ATTR_USER = "user";
    private static final String ATTR_USER_POINT = "userPoint";
    static final boolean DEBUG = false;
    private static final String EVENTS_FILE = "brightness_events.xml";
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
    private static final long LUX_EVENT_HORIZON = TimeUnit.SECONDS.toNanos(10);
    private static final int MAX_EVENTS = 100;
    private static final long MAX_EVENT_AGE = TimeUnit.DAYS.toMillis(30);
    private static final int MSG_BACKGROUND_START = 0;
    private static final int MSG_BRIGHTNESS_CHANGED = 1;
    private static final int MSG_START_SENSOR_LISTENER = 3;
    private static final int MSG_STOP_SENSOR_LISTENER = 2;
    static final String TAG = "BrightnessTracker";
    private static final String TAG_EVENT = "event";
    private static final String TAG_EVENTS = "events";
    private AmbientBrightnessStatsTracker mAmbientBrightnessStatsTracker;
    private final Handler mBgHandler;
    private BroadcastReceiver mBroadcastReceiver;
    private final ContentResolver mContentResolver;
    private final Context mContext;
    private int mCurrentUserId = -10000;
    private final Object mDataCollectionLock = new Object();
    @GuardedBy("mEventsLock")
    private RingBuffer<BrightnessChangeEvent> mEvents = new RingBuffer(BrightnessChangeEvent.class, 100);
    @GuardedBy("mEventsLock")
    private boolean mEventsDirty;
    private final Object mEventsLock = new Object();
    private final Injector mInjector;
    @GuardedBy("mDataCollectionLock")
    private float mLastBatteryLevel = Float.NaN;
    @GuardedBy("mDataCollectionLock")
    private float mLastBrightness = -1.0f;
    @GuardedBy("mDataCollectionLock")
    private Deque<LightData> mLastSensorReadings = new ArrayDeque();
    private SensorListener mSensorListener;
    private boolean mSensorRegistered;
    private SettingsObserver mSettingsObserver;
    @GuardedBy("mDataCollectionLock")
    private boolean mStarted;
    private final UserManager mUserManager;
    private volatile boolean mWriteBrightnessTrackerStateScheduled;

    private static class BrightnessChangeValues {
        final float brightness;
        final boolean isDefaultBrightnessConfig;
        final boolean isUserSetBrightness;
        final float powerBrightnessFactor;
        final long timestamp;

        BrightnessChangeValues(float brightness, float powerBrightnessFactor, boolean isUserSetBrightness, boolean isDefaultBrightnessConfig, long timestamp) {
            this.brightness = brightness;
            this.powerBrightnessFactor = powerBrightnessFactor;
            this.isUserSetBrightness = isUserSetBrightness;
            this.isDefaultBrightnessConfig = isDefaultBrightnessConfig;
            this.timestamp = timestamp;
        }
    }

    @VisibleForTesting
    static class Injector {
        Injector() {
        }

        public void registerSensorListener(Context context, SensorEventListener sensorListener, Handler handler) {
            SensorManager sensorManager = (SensorManager) context.getSystemService(SensorManager.class);
            sensorManager.registerListener(sensorListener, sensorManager.getDefaultSensor(5), 3, handler);
        }

        public void unregisterSensorListener(Context context, SensorEventListener sensorListener) {
            ((SensorManager) context.getSystemService(SensorManager.class)).unregisterListener(sensorListener);
        }

        public void registerBrightnessModeObserver(ContentResolver resolver, ContentObserver settingsObserver) {
            resolver.registerContentObserver(System.getUriFor("screen_brightness_mode"), false, settingsObserver, -1);
        }

        public void unregisterBrightnessModeObserver(Context context, ContentObserver settingsObserver) {
            context.getContentResolver().unregisterContentObserver(settingsObserver);
        }

        public void registerReceiver(Context context, BroadcastReceiver receiver, IntentFilter filter) {
            context.registerReceiver(receiver, filter);
        }

        public void unregisterReceiver(Context context, BroadcastReceiver receiver) {
            context.unregisterReceiver(receiver);
        }

        public Handler getBackgroundHandler() {
            return BackgroundThread.getHandler();
        }

        public boolean isBrightnessModeAutomatic(ContentResolver resolver) {
            return System.getIntForUser(resolver, "screen_brightness_mode", 0, -2) == 1;
        }

        public int getSecureIntForUser(ContentResolver resolver, String setting, int defaultValue, int userId) {
            return Secure.getIntForUser(resolver, setting, defaultValue, userId);
        }

        public AtomicFile getFile(String filename) {
            return new AtomicFile(new File(Environment.getDataSystemDeDirectory(), filename));
        }

        public long currentTimeMillis() {
            return System.currentTimeMillis();
        }

        public long elapsedRealtimeNanos() {
            return SystemClock.elapsedRealtimeNanos();
        }

        public int getUserSerialNumber(UserManager userManager, int userId) {
            return userManager.getUserSerialNumber(userId);
        }

        public int getUserId(UserManager userManager, int userSerialNumber) {
            return userManager.getUserHandle(userSerialNumber);
        }

        public int[] getProfileIds(UserManager userManager, int userId) {
            if (userManager != null) {
                return userManager.getProfileIds(userId, false);
            }
            return new int[]{userId};
        }

        public StackInfo getFocusedStack() throws RemoteException {
            return ActivityManager.getService().getFocusedStackInfo();
        }

        public void scheduleIdleJob(Context context) {
            BrightnessIdleJob.scheduleJob(context);
        }

        public void cancelIdleJob(Context context) {
            BrightnessIdleJob.cancelJob(context);
        }

        public boolean isInteractive(Context context) {
            return ((PowerManager) context.getSystemService(PowerManager.class)).isInteractive();
        }
    }

    private static class LightData {
        public float lux;
        public long timestamp;

        private LightData() {
        }
    }

    private final class Receiver extends BroadcastReceiver {
        private Receiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.ACTION_SHUTDOWN".equals(action)) {
                BrightnessTracker.this.stop();
                BrightnessTracker.this.scheduleWriteBrightnessTrackerState();
            } else if ("android.intent.action.BATTERY_CHANGED".equals(action)) {
                int level = intent.getIntExtra("level", -1);
                int scale = intent.getIntExtra("scale", 0);
                if (level != -1 && scale != 0) {
                    BrightnessTracker.this.batteryLevelChanged(level, scale);
                }
            } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
                BrightnessTracker.this.mBgHandler.obtainMessage(2).sendToTarget();
            } else if ("android.intent.action.SCREEN_ON".equals(action)) {
                BrightnessTracker.this.mBgHandler.obtainMessage(3).sendToTarget();
            }
        }
    }

    private final class SensorListener implements SensorEventListener {
        private SensorListener() {
        }

        public void onSensorChanged(SensorEvent event) {
            BrightnessTracker.this.recordSensorEvent(event);
            BrightnessTracker.this.recordAmbientBrightnessStats(event);
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    private final class SettingsObserver extends ContentObserver {
        public SettingsObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange, Uri uri) {
            if (BrightnessTracker.this.mInjector.isBrightnessModeAutomatic(BrightnessTracker.this.mContentResolver)) {
                BrightnessTracker.this.mBgHandler.obtainMessage(3).sendToTarget();
            } else {
                BrightnessTracker.this.mBgHandler.obtainMessage(2).sendToTarget();
            }
        }
    }

    private final class TrackerHandler extends Handler {
        public TrackerHandler(Looper looper) {
            super(looper, null, true);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    BrightnessTracker.this.backgroundStart(((Float) msg.obj).floatValue());
                    return;
                case 1:
                    BrightnessChangeValues values = msg.obj;
                    boolean z = true;
                    if (msg.arg1 != 1) {
                        z = false;
                    }
                    BrightnessTracker.this.handleBrightnessChanged(values.brightness, z, values.powerBrightnessFactor, values.isUserSetBrightness, values.isDefaultBrightnessConfig, values.timestamp);
                    return;
                case 2:
                    BrightnessTracker.this.stopSensorListener();
                    return;
                case 3:
                    BrightnessTracker.this.startSensorListener();
                    return;
                default:
                    return;
            }
        }
    }

    public BrightnessTracker(Context context, Injector injector) {
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        if (injector != null) {
            this.mInjector = injector;
        } else {
            this.mInjector = new Injector();
        }
        this.mBgHandler = new TrackerHandler(this.mInjector.getBackgroundHandler().getLooper());
        this.mUserManager = (UserManager) this.mContext.getSystemService(UserManager.class);
    }

    public void start(float initialBrightness) {
        this.mCurrentUserId = ActivityManager.getCurrentUser();
        this.mBgHandler.obtainMessage(0, Float.valueOf(initialBrightness)).sendToTarget();
    }

    private void backgroundStart(float initialBrightness) {
        readEvents();
        readAmbientBrightnessStats();
        this.mSensorListener = new SensorListener();
        this.mSettingsObserver = new SettingsObserver(this.mBgHandler);
        this.mInjector.registerBrightnessModeObserver(this.mContentResolver, this.mSettingsObserver);
        startSensorListener();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.ACTION_SHUTDOWN");
        intentFilter.addAction("android.intent.action.BATTERY_CHANGED");
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        this.mBroadcastReceiver = new Receiver();
        this.mInjector.registerReceiver(this.mContext, this.mBroadcastReceiver, intentFilter);
        this.mInjector.scheduleIdleJob(this.mContext);
        synchronized (this.mDataCollectionLock) {
            this.mLastBrightness = initialBrightness;
            this.mStarted = true;
        }
    }

    @VisibleForTesting
    void stop() {
        this.mBgHandler.removeMessages(0);
        stopSensorListener();
        this.mInjector.unregisterSensorListener(this.mContext, this.mSensorListener);
        this.mInjector.unregisterBrightnessModeObserver(this.mContext, this.mSettingsObserver);
        this.mInjector.unregisterReceiver(this.mContext, this.mBroadcastReceiver);
        this.mInjector.cancelIdleJob(this.mContext);
        synchronized (this.mDataCollectionLock) {
            this.mStarted = false;
        }
    }

    public void onSwitchUser(int newUserId) {
        this.mCurrentUserId = newUserId;
    }

    public ParceledListSlice<BrightnessChangeEvent> getEvents(int userId, boolean includePackage) {
        BrightnessChangeEvent[] events;
        synchronized (this.mEventsLock) {
            events = (BrightnessChangeEvent[]) this.mEvents.toArray();
        }
        int[] profiles = this.mInjector.getProfileIds(this.mUserManager, userId);
        Map<Integer, Boolean> toRedact = new HashMap();
        int i = 0;
        int i2 = 0;
        while (true) {
            boolean redact = true;
            if (i2 >= profiles.length) {
                break;
            }
            int profileId = profiles[i2];
            if (includePackage && profileId == userId) {
                redact = false;
            }
            toRedact.put(Integer.valueOf(profiles[i2]), Boolean.valueOf(redact));
            i2++;
        }
        ArrayList<BrightnessChangeEvent> out = new ArrayList(events.length);
        while (i < events.length) {
            Boolean redact2 = (Boolean) toRedact.get(Integer.valueOf(events[i].userId));
            if (redact2 != null) {
                if (redact2.booleanValue()) {
                    out.add(new BrightnessChangeEvent(events[i], true));
                } else {
                    out.add(events[i]);
                }
            }
            i++;
        }
        return new ParceledListSlice(out);
    }

    public void persistBrightnessTrackerState() {
        scheduleWriteBrightnessTrackerState();
    }

    public void notifyBrightnessChanged(float brightness, boolean userInitiated, float powerBrightnessFactor, boolean isUserSetBrightness, boolean isDefaultBrightnessConfig) {
        this.mBgHandler.obtainMessage(1, userInitiated, 0, new BrightnessChangeValues(brightness, powerBrightnessFactor, isUserSetBrightness, isDefaultBrightnessConfig, this.mInjector.currentTimeMillis())).sendToTarget();
    }

    /* JADX WARNING: Missing block: B:30:0x008d, code:
            r2 = r5;
     */
    /* JADX WARNING: Missing block: B:32:?, code:
            r0 = r1.mInjector.getFocusedStack();
     */
    /* JADX WARNING: Missing block: B:33:0x0094, code:
            if (r0 == null) goto L_0x00e0;
     */
    /* JADX WARNING: Missing block: B:35:0x0098, code:
            if (r0.topActivity == null) goto L_0x00e0;
     */
    /* JADX WARNING: Missing block: B:36:0x009a, code:
            r2.setUserId(r0.userId);
            r2.setPackageName(r0.topActivity.getPackageName());
     */
    /* JADX WARNING: Missing block: B:38:0x00b7, code:
            if (r1.mInjector.getSecureIntForUser(r1.mContentResolver, "night_display_activated", 0, -2) != 1) goto L_0x00bb;
     */
    /* JADX WARNING: Missing block: B:39:0x00b9, code:
            r0 = true;
     */
    /* JADX WARNING: Missing block: B:40:0x00bb, code:
            r0 = false;
     */
    /* JADX WARNING: Missing block: B:41:0x00bc, code:
            r2.setNightMode(r0);
            r2.setColorTemperature(r1.mInjector.getSecureIntForUser(r1.mContentResolver, "night_display_color_temperature", 0, -2));
            r4 = r2.build();
            r5 = r1.mEventsLock;
     */
    /* JADX WARNING: Missing block: B:42:0x00d3, code:
            monitor-enter(r5);
     */
    /* JADX WARNING: Missing block: B:44:?, code:
            r1.mEventsDirty = true;
            r1.mEvents.append(r4);
     */
    /* JADX WARNING: Missing block: B:45:0x00db, code:
            monitor-exit(r5);
     */
    /* JADX WARNING: Missing block: B:46:0x00dc, code:
            return;
     */
    /* JADX WARNING: Missing block: B:50:0x00e0, code:
            return;
     */
    /* JADX WARNING: Missing block: B:52:0x00e2, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handleBrightnessChanged(float brightness, boolean userInitiated, float powerBrightnessFactor, boolean isUserSetBrightness, boolean isDefaultBrightnessConfig, long timestamp) {
        Throwable th;
        boolean z;
        boolean z2;
        float f = brightness;
        synchronized (this.mDataCollectionLock) {
            try {
                if (this.mStarted) {
                    float previousBrightness = this.mLastBrightness;
                    this.mLastBrightness = f;
                    if (userInitiated) {
                        Builder builder = new Builder();
                        builder.setBrightness(f);
                        builder.setTimeStamp(timestamp);
                        try {
                            builder.setPowerBrightnessFactor(powerBrightnessFactor);
                        } catch (Throwable th2) {
                            th = th2;
                            z = isUserSetBrightness;
                            z2 = isDefaultBrightnessConfig;
                            throw th;
                        }
                        try {
                            builder.setUserBrightnessPoint(isUserSetBrightness);
                            try {
                                builder.setIsDefaultBrightnessConfig(isDefaultBrightnessConfig);
                                int readingCount = this.mLastSensorReadings.size();
                                if (readingCount == 0) {
                                    return;
                                }
                                float[] luxValues = new float[readingCount];
                                long[] luxTimestamps = new long[readingCount];
                                int pos = 0;
                                long currentTimeMillis = this.mInjector.currentTimeMillis();
                                long elapsedTimeNanos = this.mInjector.elapsedRealtimeNanos();
                                Iterator it = this.mLastSensorReadings.iterator();
                                while (it.hasNext()) {
                                    Iterator it2 = it;
                                    LightData reading = (LightData) it.next();
                                    luxValues[pos] = reading.lux;
                                    luxTimestamps[pos] = currentTimeMillis - TimeUnit.NANOSECONDS.toMillis(elapsedTimeNanos - reading.timestamp);
                                    pos++;
                                    it = it2;
                                    long j = timestamp;
                                }
                                builder.setLuxValues(luxValues);
                                builder.setLuxTimestamps(luxTimestamps);
                                builder.setBatteryLevel(this.mLastBatteryLevel);
                                builder.setLastBrightness(previousBrightness);
                            } catch (Throwable th3) {
                                th = th3;
                                throw th;
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            z2 = isDefaultBrightnessConfig;
                            throw th;
                        }
                    }
                    return;
                }
            } catch (Throwable th5) {
                th = th5;
                float f2 = powerBrightnessFactor;
                z = isUserSetBrightness;
                z2 = isDefaultBrightnessConfig;
                throw th;
            }
        }
    }

    private void startSensorListener() {
        if (!this.mSensorRegistered && this.mInjector.isInteractive(this.mContext) && this.mInjector.isBrightnessModeAutomatic(this.mContentResolver)) {
            this.mAmbientBrightnessStatsTracker.start();
            this.mSensorRegistered = true;
            this.mInjector.registerSensorListener(this.mContext, this.mSensorListener, this.mInjector.getBackgroundHandler());
        }
    }

    private void stopSensorListener() {
        if (this.mSensorRegistered) {
            this.mAmbientBrightnessStatsTracker.stop();
            this.mInjector.unregisterSensorListener(this.mContext, this.mSensorListener);
            this.mSensorRegistered = false;
        }
    }

    private void scheduleWriteBrightnessTrackerState() {
        if (!this.mWriteBrightnessTrackerStateScheduled) {
            this.mBgHandler.post(new -$$Lambda$BrightnessTracker$fmx2Mcw7OCEtRi9DwxxGQgA74fg(this));
            this.mWriteBrightnessTrackerStateScheduled = true;
        }
    }

    public static /* synthetic */ void lambda$scheduleWriteBrightnessTrackerState$0(BrightnessTracker brightnessTracker) {
        brightnessTracker.mWriteBrightnessTrackerStateScheduled = false;
        brightnessTracker.writeEvents();
        brightnessTracker.writeAmbientBrightnessStats();
    }

    private void writeEvents() {
        synchronized (this.mEventsLock) {
            if (this.mEventsDirty) {
                AtomicFile writeTo = this.mInjector.getFile(EVENTS_FILE);
                if (writeTo == null) {
                    return;
                } else if (this.mEvents.isEmpty()) {
                    if (writeTo.exists()) {
                        writeTo.delete();
                    }
                    this.mEventsDirty = false;
                } else {
                    FileOutputStream output = null;
                    try {
                        output = writeTo.startWrite();
                        writeEventsLocked(output);
                        writeTo.finishWrite(output);
                        this.mEventsDirty = false;
                    } catch (IOException e) {
                        writeTo.failWrite(output);
                        Slog.e(TAG, "Failed to write change mEvents.", e);
                    }
                }
            } else {
                return;
            }
        }
    }

    private void writeAmbientBrightnessStats() {
        AtomicFile writeTo = this.mInjector.getFile(AMBIENT_BRIGHTNESS_STATS_FILE);
        if (writeTo != null) {
            FileOutputStream output = null;
            try {
                output = writeTo.startWrite();
                this.mAmbientBrightnessStatsTracker.writeStats(output);
                writeTo.finishWrite(output);
            } catch (IOException e) {
                writeTo.failWrite(output);
                Slog.e(TAG, "Failed to write ambient brightness stats.", e);
            }
        }
    }

    private void readEvents() {
        synchronized (this.mEventsLock) {
            this.mEventsDirty = true;
            this.mEvents.clear();
            AtomicFile readFrom = this.mInjector.getFile(EVENTS_FILE);
            if (readFrom != null && readFrom.exists()) {
                FileInputStream input = null;
                try {
                    input = readFrom.openRead();
                    readEventsLocked(input);
                    IoUtils.closeQuietly(input);
                } catch (IOException e) {
                    try {
                        readFrom.delete();
                        Slog.e(TAG, "Failed to read change mEvents.", e);
                    } finally {
                        IoUtils.closeQuietly(input);
                    }
                }
            }
        }
    }

    private void readAmbientBrightnessStats() {
        this.mAmbientBrightnessStatsTracker = new AmbientBrightnessStatsTracker(this.mUserManager, null);
        AtomicFile readFrom = this.mInjector.getFile(AMBIENT_BRIGHTNESS_STATS_FILE);
        if (readFrom != null && readFrom.exists()) {
            FileInputStream input = null;
            try {
                input = readFrom.openRead();
                this.mAmbientBrightnessStatsTracker.readStats(input);
            } catch (IOException e) {
                readFrom.delete();
                Slog.e(TAG, "Failed to read ambient brightness stats.", e);
            } catch (Throwable th) {
                IoUtils.closeQuietly(input);
            }
            IoUtils.closeQuietly(input);
        }
    }

    @GuardedBy("mEventsLock")
    @VisibleForTesting
    void writeEventsLocked(OutputStream stream) throws IOException {
        XmlSerializer out = new FastXmlSerializer();
        out.setOutput(stream, StandardCharsets.UTF_8.name());
        out.startDocument(null, Boolean.valueOf(true));
        out.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        out.startTag(null, TAG_EVENTS);
        BrightnessChangeEvent[] toWrite = (BrightnessChangeEvent[]) this.mEvents.toArray();
        this.mEvents.clear();
        long timeCutOff = this.mInjector.currentTimeMillis() - MAX_EVENT_AGE;
        int i = 0;
        while (i < toWrite.length) {
            int userSerialNo = this.mInjector.getUserSerialNumber(this.mUserManager, toWrite[i].userId);
            if (userSerialNo != -1 && toWrite[i].timeStamp > timeCutOff) {
                this.mEvents.append(toWrite[i]);
                out.startTag(null, "event");
                out.attribute(null, ATTR_NITS, Float.toString(toWrite[i].brightness));
                out.attribute(null, "timestamp", Long.toString(toWrite[i].timeStamp));
                out.attribute(null, "packageName", toWrite[i].packageName);
                out.attribute(null, ATTR_USER, Integer.toString(userSerialNo));
                out.attribute(null, ATTR_BATTERY_LEVEL, Float.toString(toWrite[i].batteryLevel));
                out.attribute(null, ATTR_NIGHT_MODE, Boolean.toString(toWrite[i].nightMode));
                out.attribute(null, ATTR_COLOR_TEMPERATURE, Integer.toString(toWrite[i].colorTemperature));
                out.attribute(null, ATTR_LAST_NITS, Float.toString(toWrite[i].lastBrightness));
                out.attribute(null, ATTR_DEFAULT_CONFIG, Boolean.toString(toWrite[i].isDefaultBrightnessConfig));
                out.attribute(null, ATTR_POWER_SAVE, Float.toString(toWrite[i].powerBrightnessFactor));
                out.attribute(null, ATTR_USER_POINT, Boolean.toString(toWrite[i].isUserSetBrightness));
                StringBuilder luxValues = new StringBuilder();
                StringBuilder luxTimestamps = new StringBuilder();
                for (int j = 0; j < toWrite[i].luxValues.length; j++) {
                    if (j > 0) {
                        luxValues.append(',');
                        luxTimestamps.append(',');
                    }
                    luxValues.append(Float.toString(toWrite[i].luxValues[j]));
                    luxTimestamps.append(Long.toString(toWrite[i].luxTimestamps[j]));
                }
                out.attribute(null, ATTR_LUX, luxValues.toString());
                out.attribute(null, ATTR_LUX_TIMESTAMPS, luxTimestamps.toString());
                out.endTag(null, "event");
            }
            i++;
        }
        out.endTag(null, TAG_EVENTS);
        out.endDocument();
        stream.flush();
    }

    /* JADX WARNING: Removed duplicated region for block: B:53:0x01d3 A:{Splitter: B:1:0x0002, ExcHandler: java.lang.NullPointerException (r0_9 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x01d3 A:{Splitter: B:1:0x0002, ExcHandler: java.lang.NullPointerException (r0_9 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x01d3 A:{Splitter: B:1:0x0002, ExcHandler: java.lang.NullPointerException (r0_9 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:53:0x01d3, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:54:0x01d4, code:
            r1.mEvents = new com.android.internal.util.RingBuffer(android.hardware.display.BrightnessChangeEvent.class, 100);
            android.util.Slog.e(TAG, "Failed to parse brightness event", r0);
     */
    /* JADX WARNING: Missing block: B:55:0x01ed, code:
            throw new java.io.IOException("failed to parse file", r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    @GuardedBy("mEventsLock")
    @VisibleForTesting
    void readEventsLocked(InputStream stream) throws IOException {
        try {
            int type;
            int i;
            String tag;
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(stream, StandardCharsets.UTF_8.name());
            while (true) {
                int next = parser.next();
                type = next;
                i = 1;
                if (next == 1 || type == 2) {
                    tag = parser.getName();
                }
            }
            tag = parser.getName();
            XmlPullParser xmlPullParser;
            if (TAG_EVENTS.equals(tag)) {
                long timeCutOff = this.mInjector.currentTimeMillis() - MAX_EVENT_AGE;
                parser.next();
                int outerDepth = parser.getDepth();
                while (true) {
                    int next2 = parser.next();
                    type = next2;
                    if (next2 == i) {
                        return;
                    }
                    if (type != 3 || parser.getDepth() > outerDepth) {
                        int outerDepth2;
                        int type2;
                        if (type == 3) {
                            xmlPullParser = parser;
                            type2 = type;
                            outerDepth2 = outerDepth;
                        } else if (type == 4) {
                            xmlPullParser = parser;
                            type2 = type;
                            outerDepth2 = outerDepth;
                        } else {
                            tag = parser.getName();
                            String tag2;
                            if ("event".equals(tag)) {
                                Builder builder = new Builder();
                                String brightness = parser.getAttributeValue(null, ATTR_NITS);
                                builder.setBrightness(Float.parseFloat(brightness));
                                builder.setTimeStamp(Long.parseLong(parser.getAttributeValue(null, "timestamp")));
                                builder.setPackageName(parser.getAttributeValue(null, "packageName"));
                                builder.setUserId(this.mInjector.getUserId(this.mUserManager, Integer.parseInt(parser.getAttributeValue(null, ATTR_USER))));
                                String batteryLevel = parser.getAttributeValue(null, ATTR_BATTERY_LEVEL);
                                builder.setBatteryLevel(Float.parseFloat(batteryLevel));
                                builder.setNightMode(Boolean.parseBoolean(parser.getAttributeValue(null, ATTR_NIGHT_MODE)));
                                builder.setColorTemperature(Integer.parseInt(parser.getAttributeValue(null, ATTR_COLOR_TEMPERATURE)));
                                tag2 = tag;
                                tag = parser.getAttributeValue(null, ATTR_LAST_NITS);
                                builder.setLastBrightness(Float.parseFloat(tag));
                                String luxValue = parser.getAttributeValue(null, ATTR_LUX);
                                tag = parser.getAttributeValue(null, ATTR_LUX_TIMESTAMPS);
                                String[] luxValuesStrings = luxValue.split(",");
                                type2 = type;
                                type = tag.split(",");
                                if (luxValuesStrings.length != type.length) {
                                    xmlPullParser = parser;
                                    outerDepth2 = outerDepth;
                                } else {
                                    float[] luxValues = new float[luxValuesStrings.length];
                                    long[] luxTimestamps = new long[luxValuesStrings.length];
                                    int i2 = 0;
                                    while (true) {
                                        outerDepth2 = outerDepth;
                                        String brightness2 = brightness;
                                        int i3 = i2;
                                        if (i3 >= luxValues.length) {
                                            break;
                                        }
                                        luxValues[i3] = Float.parseFloat(luxValuesStrings[i3]);
                                        luxTimestamps[i3] = Long.parseLong(type[i3]);
                                        i2 = i3 + 1;
                                        outerDepth = outerDepth2;
                                        brightness = brightness2;
                                    }
                                    builder.setLuxValues(luxValues);
                                    builder.setLuxTimestamps(luxTimestamps);
                                    outerDepth = parser.getAttributeValue(null, ATTR_DEFAULT_CONFIG);
                                    if (outerDepth != 0) {
                                        builder.setIsDefaultBrightnessConfig(Boolean.parseBoolean(outerDepth));
                                    }
                                    tag = parser.getAttributeValue(null, ATTR_POWER_SAVE);
                                    if (tag != null) {
                                        builder.setPowerBrightnessFactor(Float.parseFloat(tag));
                                    } else {
                                        builder.setPowerBrightnessFactor(1.0f);
                                    }
                                    tag = parser.getAttributeValue(null, ATTR_USER_POINT);
                                    if (tag != null) {
                                        builder.setUserBrightnessPoint(Boolean.parseBoolean(tag));
                                    }
                                    BrightnessChangeEvent event = builder.build();
                                    xmlPullParser = parser;
                                    if (event.userId != -1) {
                                        if (event.timeStamp > timeCutOff && event.luxValues.length > 0) {
                                            this.mEvents.append(event);
                                        }
                                    }
                                }
                            } else {
                                xmlPullParser = parser;
                                tag2 = tag;
                                type2 = type;
                                outerDepth2 = outerDepth;
                            }
                        }
                        outerDepth = outerDepth2;
                        parser = xmlPullParser;
                        InputStream inputStream = stream;
                        i = 1;
                    } else {
                        return;
                    }
                }
            }
            xmlPullParser = parser;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Events not found in brightness tracker file ");
            stringBuilder.append(tag);
            throw new XmlPullParserException(stringBuilder.toString());
        } catch (Exception e) {
        }
    }

    public void dump(PrintWriter pw) {
        StringBuilder stringBuilder;
        pw.println("BrightnessTracker state:");
        synchronized (this.mDataCollectionLock) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mStarted=");
            stringBuilder2.append(this.mStarted);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mLastBatteryLevel=");
            stringBuilder2.append(this.mLastBatteryLevel);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mLastBrightness=");
            stringBuilder2.append(this.mLastBrightness);
            pw.println(stringBuilder2.toString());
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  mLastSensorReadings.size=");
            stringBuilder2.append(this.mLastSensorReadings.size());
            pw.println(stringBuilder2.toString());
            if (!this.mLastSensorReadings.isEmpty()) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  mLastSensorReadings time span ");
                stringBuilder2.append(((LightData) this.mLastSensorReadings.peekFirst()).timestamp);
                stringBuilder2.append("->");
                stringBuilder2.append(((LightData) this.mLastSensorReadings.peekLast()).timestamp);
                pw.println(stringBuilder2.toString());
            }
        }
        synchronized (this.mEventsLock) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mEventsDirty=");
            stringBuilder.append(this.mEventsDirty);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("  mEvents.size=");
            stringBuilder.append(this.mEvents.size());
            pw.println(stringBuilder.toString());
            BrightnessChangeEvent[] events = (BrightnessChangeEvent[]) this.mEvents.toArray();
            for (int i = 0; i < events.length; i++) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("    ");
                stringBuilder3.append(FORMAT.format(new Date(events[i].timeStamp)));
                pw.print(stringBuilder3.toString());
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(", userId=");
                stringBuilder3.append(events[i].userId);
                pw.print(stringBuilder3.toString());
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(", ");
                stringBuilder3.append(events[i].lastBrightness);
                stringBuilder3.append("->");
                stringBuilder3.append(events[i].brightness);
                pw.print(stringBuilder3.toString());
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(", isUserSetBrightness=");
                stringBuilder3.append(events[i].isUserSetBrightness);
                pw.print(stringBuilder3.toString());
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(", powerBrightnessFactor=");
                stringBuilder3.append(events[i].powerBrightnessFactor);
                pw.print(stringBuilder3.toString());
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(", isDefaultBrightnessConfig=");
                stringBuilder3.append(events[i].isDefaultBrightnessConfig);
                pw.print(stringBuilder3.toString());
                pw.print(" {");
                for (int j = 0; j < events[i].luxValues.length; j++) {
                    if (j != 0) {
                        pw.print(", ");
                    }
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("(");
                    stringBuilder4.append(events[i].luxValues[j]);
                    stringBuilder4.append(",");
                    stringBuilder4.append(events[i].luxTimestamps[j]);
                    stringBuilder4.append(")");
                    pw.print(stringBuilder4.toString());
                }
                pw.println("}");
            }
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("  mWriteBrightnessTrackerStateScheduled=");
        stringBuilder.append(this.mWriteBrightnessTrackerStateScheduled);
        pw.println(stringBuilder.toString());
        this.mBgHandler.runWithScissors(new -$$Lambda$BrightnessTracker$_S_g5htVKYYPRPZzYSZzGdy7hM0(this, pw), 1000);
        if (this.mAmbientBrightnessStatsTracker != null) {
            pw.println();
            this.mAmbientBrightnessStatsTracker.dump(pw);
        }
    }

    private void dumpLocal(PrintWriter pw) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("  mSensorRegistered=");
        stringBuilder.append(this.mSensorRegistered);
        pw.println(stringBuilder.toString());
    }

    public ParceledListSlice<AmbientBrightnessDayStats> getAmbientBrightnessStats(int userId) {
        if (this.mAmbientBrightnessStatsTracker != null) {
            ArrayList<AmbientBrightnessDayStats> stats = this.mAmbientBrightnessStatsTracker.getUserStats(userId);
            if (stats != null) {
                return new ParceledListSlice(stats);
            }
        }
        return ParceledListSlice.emptyList();
    }

    private void recordSensorEvent(SensorEvent event) {
        long horizon = this.mInjector.elapsedRealtimeNanos() - LUX_EVENT_HORIZON;
        synchronized (this.mDataCollectionLock) {
            if (this.mLastSensorReadings.isEmpty() || event.timestamp >= ((LightData) this.mLastSensorReadings.getLast()).timestamp) {
                LightData data = null;
                while (!this.mLastSensorReadings.isEmpty() && ((LightData) this.mLastSensorReadings.getFirst()).timestamp < horizon) {
                    data = (LightData) this.mLastSensorReadings.removeFirst();
                }
                if (data != null) {
                    this.mLastSensorReadings.addFirst(data);
                }
                LightData data2 = new LightData();
                data2.timestamp = event.timestamp;
                data2.lux = event.values[0];
                this.mLastSensorReadings.addLast(data2);
                return;
            }
        }
    }

    private void recordAmbientBrightnessStats(SensorEvent event) {
        this.mAmbientBrightnessStatsTracker.add(this.mCurrentUserId, event.values[0]);
    }

    private void batteryLevelChanged(int level, int scale) {
        synchronized (this.mDataCollectionLock) {
            this.mLastBatteryLevel = ((float) level) / ((float) scale);
        }
    }
}
