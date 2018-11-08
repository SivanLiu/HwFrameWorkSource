package com.android.server.twilight;

import android.text.format.DateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TimeZone;

public final class TwilightState {
    private final long mSunriseTimeMillis;
    private final long mSunsetTimeMillis;

    public TwilightState(long sunriseTimeMillis, long sunsetTimeMillis) {
        this.mSunriseTimeMillis = sunriseTimeMillis;
        this.mSunsetTimeMillis = sunsetTimeMillis;
    }

    public long sunriseTimeMillis() {
        return this.mSunriseTimeMillis;
    }

    public LocalDateTime sunrise() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(this.mSunriseTimeMillis), TimeZone.getDefault().toZoneId());
    }

    public long sunsetTimeMillis() {
        return this.mSunsetTimeMillis;
    }

    public LocalDateTime sunset() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(this.mSunsetTimeMillis), TimeZone.getDefault().toZoneId());
    }

    public boolean isNight() {
        long now = System.currentTimeMillis();
        if (now < this.mSunsetTimeMillis || now >= this.mSunriseTimeMillis) {
            return false;
        }
        return true;
    }

    public boolean equals(Object o) {
        return o instanceof TwilightState ? equals((TwilightState) o) : false;
    }

    public boolean equals(TwilightState other) {
        if (other != null && this.mSunriseTimeMillis == other.mSunriseTimeMillis && this.mSunsetTimeMillis == other.mSunsetTimeMillis) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return Long.hashCode(this.mSunriseTimeMillis) ^ Long.hashCode(this.mSunsetTimeMillis);
    }

    public String toString() {
        return "TwilightState { sunrise=" + DateFormat.format("MM-dd HH:mm", this.mSunriseTimeMillis) + " sunset=" + DateFormat.format("MM-dd HH:mm", this.mSunsetTimeMillis) + " }";
    }
}
