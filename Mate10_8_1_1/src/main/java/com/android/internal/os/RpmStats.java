package com.android.internal.os;

import android.util.ArrayMap;
import java.util.Map;

public final class RpmStats {
    public Map<String, PowerStatePlatformSleepState> mPlatformLowPowerStats = new ArrayMap();
    public Map<String, PowerStateSubsystem> mSubsystemLowPowerStats = new ArrayMap();

    public static class PowerStateElement {
        public int mCount;
        public long mTimeMs;

        private PowerStateElement(long timeMs, int count) {
            this.mTimeMs = timeMs;
            this.mCount = count;
        }
    }

    public static class PowerStatePlatformSleepState {
        public int mCount;
        public long mTimeMs;
        public Map<String, PowerStateElement> mVoters = new ArrayMap();

        public void putVoter(String name, long timeMs, int count) {
            PowerStateElement e = (PowerStateElement) this.mVoters.get(name);
            if (e == null) {
                this.mVoters.put(name, new PowerStateElement(timeMs, count));
                return;
            }
            e.mTimeMs = timeMs;
            e.mCount = count;
        }
    }

    public static class PowerStateSubsystem {
        public Map<String, PowerStateElement> mStates = new ArrayMap();

        public void putState(String name, long timeMs, int count) {
            PowerStateElement e = (PowerStateElement) this.mStates.get(name);
            if (e == null) {
                this.mStates.put(name, new PowerStateElement(timeMs, count));
                return;
            }
            e.mTimeMs = timeMs;
            e.mCount = count;
        }
    }

    public PowerStatePlatformSleepState getAndUpdatePlatformState(String name, long timeMs, int count) {
        PowerStatePlatformSleepState e = (PowerStatePlatformSleepState) this.mPlatformLowPowerStats.get(name);
        if (e == null) {
            e = new PowerStatePlatformSleepState();
            this.mPlatformLowPowerStats.put(name, e);
        }
        e.mTimeMs = timeMs;
        e.mCount = count;
        return e;
    }

    public PowerStateSubsystem getSubsystem(String name) {
        PowerStateSubsystem e = (PowerStateSubsystem) this.mSubsystemLowPowerStats.get(name);
        if (e != null) {
            return e;
        }
        e = new PowerStateSubsystem();
        this.mSubsystemLowPowerStats.put(name, e);
        return e;
    }
}
