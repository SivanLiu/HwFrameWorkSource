package com.android.internal.statusbar;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import java.util.ArrayDeque;

public class NotificationVisibility implements Parcelable {
    public static final Creator<NotificationVisibility> CREATOR = new Creator<NotificationVisibility>() {
        public NotificationVisibility createFromParcel(Parcel parcel) {
            return NotificationVisibility.obtain(parcel);
        }

        public NotificationVisibility[] newArray(int size) {
            return new NotificationVisibility[size];
        }
    };
    private static final int MAX_POOL_SIZE = 25;
    private static final String TAG = "NoViz";
    private static int sNexrId = 0;
    private static ArrayDeque<NotificationVisibility> sPool = new ArrayDeque(25);
    public int count;
    int id;
    public String key;
    public int rank;
    public boolean visible;

    private NotificationVisibility() {
        this.visible = true;
        int i = sNexrId;
        sNexrId = i + 1;
        this.id = i;
    }

    private NotificationVisibility(String key, int rank, int count, boolean visibile) {
        this();
        this.key = key;
        this.rank = rank;
        this.count = count;
        this.visible = visibile;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("NotificationVisibility(id=");
        stringBuilder.append(this.id);
        stringBuilder.append(" key=");
        stringBuilder.append(this.key);
        stringBuilder.append(" rank=");
        stringBuilder.append(this.rank);
        stringBuilder.append(" count=");
        stringBuilder.append(this.count);
        stringBuilder.append(this.visible ? " visible" : "");
        stringBuilder.append(" )");
        return stringBuilder.toString();
    }

    public NotificationVisibility clone() {
        return obtain(this.key, this.rank, this.count, this.visible);
    }

    public int hashCode() {
        return this.key == null ? 0 : this.key.hashCode();
    }

    public boolean equals(Object that) {
        boolean z = false;
        if (!(that instanceof NotificationVisibility)) {
            return false;
        }
        NotificationVisibility thatViz = (NotificationVisibility) that;
        if ((this.key == null && thatViz.key == null) || this.key.equals(thatViz.key)) {
            z = true;
        }
        return z;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(this.key);
        out.writeInt(this.rank);
        out.writeInt(this.count);
        out.writeInt(this.visible);
    }

    private void readFromParcel(Parcel in) {
        this.key = in.readString();
        this.rank = in.readInt();
        this.count = in.readInt();
        this.visible = in.readInt() != 0;
    }

    public static NotificationVisibility obtain(String key, int rank, int count, boolean visible) {
        NotificationVisibility vo = obtain();
        vo.key = key;
        vo.rank = rank;
        vo.count = count;
        vo.visible = visible;
        return vo;
    }

    private static NotificationVisibility obtain(Parcel in) {
        NotificationVisibility vo = obtain();
        vo.readFromParcel(in);
        return vo;
    }

    private static NotificationVisibility obtain() {
        synchronized (sPool) {
            if (sPool.isEmpty()) {
                return new NotificationVisibility();
            }
            NotificationVisibility notificationVisibility = (NotificationVisibility) sPool.poll();
            return notificationVisibility;
        }
    }

    public void recycle() {
        if (this.key != null) {
            this.key = null;
            if (sPool.size() < 25) {
                synchronized (sPool) {
                    sPool.offer(this);
                }
            }
        }
    }
}
