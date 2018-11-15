package android.support.v4.media;

import android.annotation.TargetApi;
import android.media.MediaTimestamp;
import android.support.annotation.RestrictTo;
import android.support.annotation.RestrictTo.Scope;

public final class MediaTimestamp2 {
    public static final MediaTimestamp2 TIMESTAMP_UNKNOWN = new MediaTimestamp2(-1, -1, 0.0f);
    private final float mClockRate;
    private final long mMediaTimeUs;
    private final long mNanoTime;

    public long getAnchorMediaTimeUs() {
        return this.mMediaTimeUs;
    }

    public long getAnchorSytemNanoTime() {
        return this.mNanoTime;
    }

    public float getMediaClockRate() {
        return this.mClockRate;
    }

    @TargetApi(23)
    @RestrictTo({Scope.LIBRARY_GROUP})
    MediaTimestamp2(MediaTimestamp timestamp) {
        this.mMediaTimeUs = timestamp.getAnchorMediaTimeUs();
        this.mNanoTime = timestamp.getAnchorSytemNanoTime();
        this.mClockRate = timestamp.getMediaClockRate();
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    MediaTimestamp2(long mediaUs, long systemNs, float rate) {
        this.mMediaTimeUs = mediaUs;
        this.mNanoTime = systemNs;
        this.mClockRate = rate;
    }

    @RestrictTo({Scope.LIBRARY_GROUP})
    MediaTimestamp2() {
        this.mMediaTimeUs = 0;
        this.mNanoTime = 0;
        this.mClockRate = 1.0f;
    }

    public boolean equals(Object obj) {
        boolean z = true;
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        MediaTimestamp2 that = (MediaTimestamp2) obj;
        if (!(this.mMediaTimeUs == that.mMediaTimeUs && this.mNanoTime == that.mNanoTime && this.mClockRate == that.mClockRate)) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return (int) (((float) (31 * ((int) (((long) (31 * Long.valueOf(this.mMediaTimeUs).hashCode())) + this.mNanoTime)))) + this.mClockRate);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getClass().getName());
        stringBuilder.append("{AnchorMediaTimeUs=");
        stringBuilder.append(this.mMediaTimeUs);
        stringBuilder.append(" AnchorSystemNanoTime=");
        stringBuilder.append(this.mNanoTime);
        stringBuilder.append(" ClockRate=");
        stringBuilder.append(this.mClockRate);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}
