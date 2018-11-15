package com.android.server.wifi;

import android.os.SystemClock;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.wifi.nano.WifiMetricsProto.WifiWakeStats;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class WifiWakeMetrics {
    @VisibleForTesting
    static final int MAX_RECORDED_SESSIONS = 10;
    @GuardedBy("mLock")
    private Session mCurrentSession;
    private int mIgnoredStarts = 0;
    private boolean mIsInSession = false;
    private final Object mLock = new Object();
    @GuardedBy("mLock")
    private final List<Session> mSessions = new ArrayList();
    private int mTotalSessions = 0;
    private int mTotalWakeups = 0;

    public static class Event {
        public final long mElapsedTime;
        public final int mNumScans;

        public Event(int numScans, long elapsedTime) {
            this.mNumScans = numScans;
            this.mElapsedTime = elapsedTime;
        }

        public com.android.server.wifi.nano.WifiMetricsProto.WifiWakeStats.Session.Event buildProto() {
            com.android.server.wifi.nano.WifiMetricsProto.WifiWakeStats.Session.Event eventProto = new com.android.server.wifi.nano.WifiMetricsProto.WifiWakeStats.Session.Event();
            eventProto.elapsedScans = this.mNumScans;
            eventProto.elapsedTimeMillis = this.mElapsedTime;
            return eventProto;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("{ mNumScans: ");
            stringBuilder.append(this.mNumScans);
            stringBuilder.append(", elapsedTime: ");
            stringBuilder.append(this.mElapsedTime);
            stringBuilder.append(" }");
            return stringBuilder.toString();
        }
    }

    public static class Session {
        @VisibleForTesting
        Event mInitEvent;
        private int mInitializeNetworks = 0;
        @VisibleForTesting
        Event mResetEvent;
        private final int mStartNetworks;
        private final long mStartTimestamp;
        @VisibleForTesting
        Event mUnlockEvent;
        @VisibleForTesting
        Event mWakeupEvent;

        public Session(int numNetworks, long timestamp) {
            this.mStartNetworks = numNetworks;
            this.mStartTimestamp = timestamp;
        }

        public void recordInitializeEvent(int numScans, int numNetworks, long timestamp) {
            if (this.mInitEvent == null) {
                this.mInitializeNetworks = numNetworks;
                this.mInitEvent = new Event(numScans, timestamp - this.mStartTimestamp);
            }
        }

        public void recordUnlockEvent(int numScans, long timestamp) {
            if (this.mUnlockEvent == null) {
                this.mUnlockEvent = new Event(numScans, timestamp - this.mStartTimestamp);
            }
        }

        public void recordWakeupEvent(int numScans, long timestamp) {
            if (this.mWakeupEvent == null) {
                this.mWakeupEvent = new Event(numScans, timestamp - this.mStartTimestamp);
            }
        }

        public boolean hasWakeupTriggered() {
            return this.mWakeupEvent != null;
        }

        public void recordResetEvent(int numScans, long timestamp) {
            if (this.mResetEvent == null) {
                this.mResetEvent = new Event(numScans, timestamp - this.mStartTimestamp);
            }
        }

        public com.android.server.wifi.nano.WifiMetricsProto.WifiWakeStats.Session buildProto() {
            com.android.server.wifi.nano.WifiMetricsProto.WifiWakeStats.Session sessionProto = new com.android.server.wifi.nano.WifiMetricsProto.WifiWakeStats.Session();
            sessionProto.startTimeMillis = this.mStartTimestamp;
            sessionProto.lockedNetworksAtStart = this.mStartNetworks;
            if (this.mInitEvent != null) {
                sessionProto.lockedNetworksAtInitialize = this.mInitializeNetworks;
                sessionProto.initializeEvent = this.mInitEvent.buildProto();
            }
            if (this.mUnlockEvent != null) {
                sessionProto.unlockEvent = this.mUnlockEvent.buildProto();
            }
            if (this.mWakeupEvent != null) {
                sessionProto.wakeupEvent = this.mWakeupEvent.buildProto();
            }
            if (this.mResetEvent != null) {
                sessionProto.resetEvent = this.mResetEvent.buildProto();
            }
            return sessionProto;
        }

        public void dump(PrintWriter pw) {
            pw.println("WifiWakeMetrics.Session:");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mStartTimestamp: ");
            stringBuilder.append(this.mStartTimestamp);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("mStartNetworks: ");
            stringBuilder.append(this.mStartNetworks);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("mInitializeNetworks: ");
            stringBuilder.append(this.mInitializeNetworks);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("mInitEvent: ");
            stringBuilder.append(this.mInitEvent == null ? "{}" : this.mInitEvent.toString());
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("mUnlockEvent: ");
            stringBuilder.append(this.mUnlockEvent == null ? "{}" : this.mUnlockEvent.toString());
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("mWakeupEvent: ");
            stringBuilder.append(this.mWakeupEvent == null ? "{}" : this.mWakeupEvent.toString());
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("mResetEvent: ");
            stringBuilder.append(this.mResetEvent == null ? "{}" : this.mResetEvent.toString());
            pw.println(stringBuilder.toString());
        }
    }

    public void recordStartEvent(int numNetworks) {
        synchronized (this.mLock) {
            this.mCurrentSession = new Session(numNetworks, SystemClock.elapsedRealtime());
            this.mIsInSession = true;
        }
    }

    public void recordInitializeEvent(int numScans, int numNetworks) {
        synchronized (this.mLock) {
            if (this.mIsInSession) {
                this.mCurrentSession.recordInitializeEvent(numScans, numNetworks, SystemClock.elapsedRealtime());
                return;
            }
        }
    }

    public void recordUnlockEvent(int numScans) {
        synchronized (this.mLock) {
            if (this.mIsInSession) {
                this.mCurrentSession.recordUnlockEvent(numScans, SystemClock.elapsedRealtime());
                return;
            }
        }
    }

    public void recordWakeupEvent(int numScans) {
        synchronized (this.mLock) {
            if (this.mIsInSession) {
                this.mCurrentSession.recordWakeupEvent(numScans, SystemClock.elapsedRealtime());
                return;
            }
        }
    }

    public void recordResetEvent(int numScans) {
        synchronized (this.mLock) {
            if (this.mIsInSession) {
                this.mCurrentSession.recordResetEvent(numScans, SystemClock.elapsedRealtime());
                if (this.mCurrentSession.hasWakeupTriggered()) {
                    this.mTotalWakeups++;
                }
                this.mTotalSessions++;
                if (this.mSessions.size() < 10) {
                    this.mSessions.add(this.mCurrentSession);
                }
                this.mIsInSession = false;
                return;
            }
        }
    }

    public void recordIgnoredStart() {
        this.mIgnoredStarts++;
    }

    public WifiWakeStats buildProto() {
        WifiWakeStats proto = new WifiWakeStats();
        proto.numSessions = this.mTotalSessions;
        proto.numWakeups = this.mTotalWakeups;
        proto.numIgnoredStarts = this.mIgnoredStarts;
        proto.sessions = new com.android.server.wifi.nano.WifiMetricsProto.WifiWakeStats.Session[this.mSessions.size()];
        for (int i = 0; i < this.mSessions.size(); i++) {
            proto.sessions[i] = ((Session) this.mSessions.get(i)).buildProto();
        }
        return proto;
    }

    public void dump(PrintWriter pw) {
        synchronized (this.mLock) {
            pw.println("-------WifiWake metrics-------");
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mTotalSessions: ");
            stringBuilder.append(this.mTotalSessions);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("mTotalWakeups: ");
            stringBuilder.append(this.mTotalWakeups);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("mIgnoredStarts: ");
            stringBuilder.append(this.mIgnoredStarts);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("mIsInSession: ");
            stringBuilder.append(this.mIsInSession);
            pw.println(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append("Stored Sessions: ");
            stringBuilder.append(this.mSessions.size());
            pw.println(stringBuilder.toString());
            for (Session session : this.mSessions) {
                session.dump(pw);
            }
            if (this.mCurrentSession != null) {
                pw.println("Current Session: ");
                this.mCurrentSession.dump(pw);
            }
            pw.println("----end of WifiWake metrics----");
        }
    }

    public void clear() {
        synchronized (this.mLock) {
            this.mSessions.clear();
            this.mTotalSessions = 0;
            this.mTotalWakeups = 0;
            this.mIgnoredStarts = 0;
        }
    }
}
