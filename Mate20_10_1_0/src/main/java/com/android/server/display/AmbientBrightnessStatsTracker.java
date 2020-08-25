package com.android.server.display;

import android.hardware.display.AmbientBrightnessDayStats;
import android.os.SystemClock;
import android.os.UserManager;
import android.util.Xml;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.devicepolicy.HwLog;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class AmbientBrightnessStatsTracker {
    @VisibleForTesting
    static final float[] BUCKET_BOUNDARIES_FOR_NEW_STATS = {0.0f, 0.1f, 0.3f, 1.0f, 3.0f, 10.0f, 30.0f, 100.0f, 300.0f, 1000.0f, 3000.0f, 10000.0f};
    private static final boolean DEBUG = false;
    @VisibleForTesting
    static final int MAX_DAYS_TO_TRACK = 7;
    private static final String TAG = "AmbientBrightnessStatsTracker";
    private final AmbientBrightnessStats mAmbientBrightnessStats;
    private float mCurrentAmbientBrightness;
    private int mCurrentUserId;
    /* access modifiers changed from: private */
    public final Injector mInjector;
    private final Timer mTimer;
    /* access modifiers changed from: private */
    public final UserManager mUserManager;

    @VisibleForTesting
    interface Clock {
        long elapsedTimeMillis();
    }

    public AmbientBrightnessStatsTracker(UserManager userManager, Injector injector) {
        this.mUserManager = userManager;
        if (injector != null) {
            this.mInjector = injector;
        } else {
            this.mInjector = new Injector();
        }
        this.mAmbientBrightnessStats = new AmbientBrightnessStats();
        this.mTimer = new Timer(new Clock() {
            /* class com.android.server.display.$$Lambda$AmbientBrightnessStatsTracker$vQZYn_dAhbvzTUn4vvpuyIATII */

            @Override // com.android.server.display.AmbientBrightnessStatsTracker.Clock
            public final long elapsedTimeMillis() {
                return AmbientBrightnessStatsTracker.this.lambda$new$0$AmbientBrightnessStatsTracker();
            }
        });
        this.mCurrentAmbientBrightness = -1.0f;
    }

    public /* synthetic */ long lambda$new$0$AmbientBrightnessStatsTracker() {
        return this.mInjector.elapsedRealtimeMillis();
    }

    public synchronized void start() {
        this.mTimer.reset();
        this.mTimer.start();
    }

    public synchronized void stop() {
        if (this.mTimer.isRunning()) {
            this.mAmbientBrightnessStats.log(this.mCurrentUserId, this.mInjector.getLocalDate(), this.mCurrentAmbientBrightness, this.mTimer.totalDurationSec());
        }
        this.mTimer.reset();
        this.mCurrentAmbientBrightness = -1.0f;
    }

    public synchronized void add(int userId, float newAmbientBrightness) {
        if (this.mTimer.isRunning()) {
            if (userId == this.mCurrentUserId) {
                this.mAmbientBrightnessStats.log(this.mCurrentUserId, this.mInjector.getLocalDate(), this.mCurrentAmbientBrightness, this.mTimer.totalDurationSec());
            } else {
                this.mCurrentUserId = userId;
            }
            this.mTimer.reset();
            this.mTimer.start();
            this.mCurrentAmbientBrightness = newAmbientBrightness;
        }
    }

    public synchronized void writeStats(OutputStream stream) throws IOException {
        this.mAmbientBrightnessStats.writeToXML(stream);
    }

    public synchronized void readStats(InputStream stream) throws IOException {
        this.mAmbientBrightnessStats.readFromXML(stream);
    }

    public synchronized ArrayList<AmbientBrightnessDayStats> getUserStats(int userId) {
        return this.mAmbientBrightnessStats.getUserStats(userId);
    }

    public synchronized void dump(PrintWriter pw) {
        pw.println("AmbientBrightnessStats:");
        pw.print(this.mAmbientBrightnessStats);
    }

    class AmbientBrightnessStats {
        private static final String ATTR_BUCKET_BOUNDARIES = "bucket-boundaries";
        private static final String ATTR_BUCKET_STATS = "bucket-stats";
        private static final String ATTR_LOCAL_DATE = "local-date";
        private static final String ATTR_USER = "user";
        private static final String TAG_AMBIENT_BRIGHTNESS_DAY_STATS = "ambient-brightness-day-stats";
        private static final String TAG_AMBIENT_BRIGHTNESS_STATS = "ambient-brightness-stats";
        private Map<Integer, Deque<AmbientBrightnessDayStats>> mStats = new HashMap();

        public AmbientBrightnessStats() {
        }

        public void log(int userId, LocalDate localDate, float ambientBrightness, float durationSec) {
            getOrCreateDayStats(getOrCreateUserStats(this.mStats, userId), localDate).log(ambientBrightness, durationSec);
        }

        public ArrayList<AmbientBrightnessDayStats> getUserStats(int userId) {
            if (this.mStats.containsKey(Integer.valueOf(userId))) {
                return new ArrayList<>(this.mStats.get(Integer.valueOf(userId)));
            }
            return null;
        }

        public void writeToXML(OutputStream stream) throws IOException {
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(stream, StandardCharsets.UTF_8.name());
            out.startDocument(null, true);
            out.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            LocalDate cutOffDate = AmbientBrightnessStatsTracker.this.mInjector.getLocalDate().minusDays(7);
            out.startTag(null, TAG_AMBIENT_BRIGHTNESS_STATS);
            for (Map.Entry<Integer, Deque<AmbientBrightnessDayStats>> entry : this.mStats.entrySet()) {
                for (AmbientBrightnessDayStats userDayStats : entry.getValue()) {
                    int userSerialNumber = AmbientBrightnessStatsTracker.this.mInjector.getUserSerialNumber(AmbientBrightnessStatsTracker.this.mUserManager, entry.getKey().intValue());
                    if (userSerialNumber != -1 && userDayStats.getLocalDate().isAfter(cutOffDate)) {
                        out.startTag(null, TAG_AMBIENT_BRIGHTNESS_DAY_STATS);
                        out.attribute(null, ATTR_USER, Integer.toString(userSerialNumber));
                        out.attribute(null, ATTR_LOCAL_DATE, userDayStats.getLocalDate().toString());
                        StringBuilder bucketBoundariesValues = new StringBuilder();
                        StringBuilder timeSpentValues = new StringBuilder();
                        for (int i = 0; i < userDayStats.getBucketBoundaries().length; i++) {
                            if (i > 0) {
                                bucketBoundariesValues.append(",");
                                timeSpentValues.append(",");
                            }
                            bucketBoundariesValues.append(userDayStats.getBucketBoundaries()[i]);
                            timeSpentValues.append(userDayStats.getStats()[i]);
                        }
                        out.attribute(null, ATTR_BUCKET_BOUNDARIES, bucketBoundariesValues.toString());
                        out.attribute(null, ATTR_BUCKET_STATS, timeSpentValues.toString());
                        out.endTag(null, TAG_AMBIENT_BRIGHTNESS_DAY_STATS);
                    }
                }
            }
            out.endTag(null, TAG_AMBIENT_BRIGHTNESS_STATS);
            out.endDocument();
            stream.flush();
        }

        /* JADX WARNING: Code restructure failed: missing block: B:48:0x010e, code lost:
            r18.mStats = r2;
         */
        /* JADX WARNING: Code restructure failed: missing block: B:49:0x0111, code lost:
            return;
         */
        public void readFromXML(InputStream stream) throws IOException {
            int i;
            XmlPullParser parser;
            String str;
            String str2 = ",";
            try {
                Map<Integer, Deque<AmbientBrightnessDayStats>> parsedStats = new HashMap<>();
                XmlPullParser parser2 = Xml.newPullParser();
                try {
                    parser2.setInput(stream, StandardCharsets.UTF_8.name());
                    while (true) {
                        int type = parser2.next();
                        i = 1;
                        if (type == 1 || type == 2) {
                            String tag = parser2.getName();
                        }
                    }
                    String tag2 = parser2.getName();
                    if (TAG_AMBIENT_BRIGHTNESS_STATS.equals(tag2)) {
                        LocalDate cutOffDate = AmbientBrightnessStatsTracker.this.mInjector.getLocalDate().minusDays(7);
                        parser2.next();
                        int outerDepth = parser2.getDepth();
                        while (true) {
                            int type2 = parser2.next();
                            if (type2 == i) {
                                break;
                            }
                            if (type2 == 3) {
                                if (parser2.getDepth() <= outerDepth) {
                                    break;
                                }
                            }
                            if (type2 == 3) {
                                str = str2;
                                parser = parser2;
                            } else if (type2 == 4) {
                                str = str2;
                                parser = parser2;
                            } else if (TAG_AMBIENT_BRIGHTNESS_DAY_STATS.equals(parser2.getName())) {
                                String userSerialNumber = parser2.getAttributeValue(null, ATTR_USER);
                                LocalDate localDate = LocalDate.parse(parser2.getAttributeValue(null, ATTR_LOCAL_DATE));
                                String[] bucketBoundaries = parser2.getAttributeValue(null, ATTR_BUCKET_BOUNDARIES).split(str2);
                                String[] bucketStats = parser2.getAttributeValue(null, ATTR_BUCKET_STATS).split(str2);
                                if (bucketBoundaries.length == bucketStats.length && bucketBoundaries.length >= i) {
                                    float[] parsedBucketBoundaries = new float[bucketBoundaries.length];
                                    float[] parsedBucketStats = new float[bucketStats.length];
                                    int i2 = 0;
                                    while (true) {
                                        str = str2;
                                        if (i2 >= bucketBoundaries.length) {
                                            break;
                                        }
                                        parsedBucketBoundaries[i2] = Float.parseFloat(bucketBoundaries[i2]);
                                        parsedBucketStats[i2] = Float.parseFloat(bucketStats[i2]);
                                        i2++;
                                        str2 = str;
                                    }
                                    parser = parser2;
                                    int userId = AmbientBrightnessStatsTracker.this.mInjector.getUserId(AmbientBrightnessStatsTracker.this.mUserManager, Integer.parseInt(userSerialNumber));
                                    if (userId != -1 && localDate.isAfter(cutOffDate)) {
                                        getOrCreateUserStats(parsedStats, userId).offer(new AmbientBrightnessDayStats(localDate, parsedBucketBoundaries, parsedBucketStats));
                                    }
                                }
                            } else {
                                str = str2;
                                parser = parser2;
                            }
                            str2 = str;
                            parser2 = parser;
                            i = 1;
                        }
                        throw new IOException("Invalid brightness stats string.");
                    }
                    throw new XmlPullParserException("Ambient brightness stats not found in tracker file " + tag2);
                } catch (IOException | NullPointerException | NumberFormatException | DateTimeParseException | XmlPullParserException e) {
                    e = e;
                    throw new IOException("Failed to parse brightness stats file.", e);
                }
            } catch (IOException | NullPointerException | NumberFormatException | DateTimeParseException | XmlPullParserException e2) {
                e = e2;
                throw new IOException("Failed to parse brightness stats file.", e);
            }
        }

        public String toString() {
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<Integer, Deque<AmbientBrightnessDayStats>> entry : this.mStats.entrySet()) {
                for (AmbientBrightnessDayStats dayStats : entry.getValue()) {
                    builder.append("  ");
                    builder.append(entry.getKey());
                    builder.append(HwLog.PREFIX);
                    builder.append(dayStats);
                    builder.append("\n");
                }
            }
            return builder.toString();
        }

        private Deque<AmbientBrightnessDayStats> getOrCreateUserStats(Map<Integer, Deque<AmbientBrightnessDayStats>> stats, int userId) {
            if (!stats.containsKey(Integer.valueOf(userId))) {
                stats.put(Integer.valueOf(userId), new ArrayDeque());
            }
            return stats.get(Integer.valueOf(userId));
        }

        private AmbientBrightnessDayStats getOrCreateDayStats(Deque<AmbientBrightnessDayStats> userStats, LocalDate localDate) {
            AmbientBrightnessDayStats lastBrightnessStats = userStats.peekLast();
            if (lastBrightnessStats != null && lastBrightnessStats.getLocalDate().equals(localDate)) {
                return lastBrightnessStats;
            }
            AmbientBrightnessDayStats dayStats = new AmbientBrightnessDayStats(localDate, AmbientBrightnessStatsTracker.BUCKET_BOUNDARIES_FOR_NEW_STATS);
            if (userStats.size() == 7) {
                userStats.poll();
            }
            userStats.offer(dayStats);
            return dayStats;
        }
    }

    @VisibleForTesting
    static class Timer {
        private final Clock clock;
        private long startTimeMillis;
        private boolean started;

        public Timer(Clock clock2) {
            this.clock = clock2;
        }

        public void reset() {
            this.started = false;
        }

        public void start() {
            if (!this.started) {
                this.startTimeMillis = this.clock.elapsedTimeMillis();
                this.started = true;
            }
        }

        public boolean isRunning() {
            return this.started;
        }

        public float totalDurationSec() {
            if (this.started) {
                return (float) (((double) (this.clock.elapsedTimeMillis() - this.startTimeMillis)) / 1000.0d);
            }
            return 0.0f;
        }
    }

    /* access modifiers changed from: package-private */
    @VisibleForTesting
    public static class Injector {
        Injector() {
        }

        public long elapsedRealtimeMillis() {
            return SystemClock.elapsedRealtime();
        }

        public int getUserSerialNumber(UserManager userManager, int userId) {
            return userManager.getUserSerialNumber(userId);
        }

        public int getUserId(UserManager userManager, int userSerialNumber) {
            return userManager.getUserHandle(userSerialNumber);
        }

        public LocalDate getLocalDate() {
            return LocalDate.now();
        }
    }
}
