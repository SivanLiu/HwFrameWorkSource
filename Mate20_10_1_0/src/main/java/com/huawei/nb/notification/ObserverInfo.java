package com.huawei.nb.notification;

import android.os.Parcel;
import android.os.Process;
import android.support.annotation.NonNull;

public abstract class ObserverInfo {
    private static final int HASHCODE_RANDOM = 31;
    private Integer pid;
    private String pkgName;
    private Integer proxyId = null;
    private ObserverType type;
    private Integer uid;

    public ObserverInfo(@NonNull ObserverType type2, String pkgName2) {
        this.type = type2;
        this.pid = Integer.valueOf(Process.myPid());
        this.uid = Integer.valueOf(Process.myUid());
        this.pkgName = pkgName2;
    }

    public ObserverInfo(Parcel in) {
        this.pid = Integer.valueOf(in.readInt());
        this.uid = Integer.valueOf(in.readInt());
        if (in.readInt() == 1) {
            this.pkgName = in.readString();
        } else {
            this.pkgName = null;
        }
        this.type = ObserverType.values()[in.readInt()];
        this.proxyId = Integer.valueOf(in.readInt());
    }

    public Integer getPid() {
        return this.pid;
    }

    public Integer getUid() {
        return this.uid;
    }

    public String getPkgName() {
        return this.pkgName;
    }

    public void setProxyId(Integer proxyId2) {
        this.proxyId = proxyId2;
    }

    public ObserverType getType() {
        return this.type;
    }

    public boolean equals(Object obj) {
        boolean z = true;
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ObserverInfo that = (ObserverInfo) obj;
        if (this.pid != null) {
            if (!this.pid.equals(that.pid)) {
                return false;
            }
        } else if (that.pid != null) {
            return false;
        }
        if (this.uid != null) {
            if (!this.uid.equals(that.uid)) {
                return false;
            }
        } else if (that.uid != null) {
            return false;
        }
        if (this.proxyId != null) {
            if (!this.proxyId.equals(that.proxyId)) {
                return false;
            }
        } else if (that.proxyId != null) {
            return false;
        }
        if (this.pkgName != null) {
            if (!this.pkgName.equals(that.pkgName)) {
                return false;
            }
        } else if (that.pkgName != null) {
            return false;
        }
        if (this.type != that.type) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        int result;
        int i;
        int i2;
        int i3;
        int i4 = 0;
        if (this.pid != null) {
            result = this.pid.hashCode();
        } else {
            result = 0;
        }
        int i5 = result * HASHCODE_RANDOM;
        if (this.uid != null) {
            i = this.uid.hashCode();
        } else {
            i = 0;
        }
        int i6 = (i5 + i) * HASHCODE_RANDOM;
        if (this.type != null) {
            i2 = this.type.hashCode();
        } else {
            i2 = 0;
        }
        int i7 = (i6 + i2) * HASHCODE_RANDOM;
        if (this.proxyId != null) {
            i3 = this.proxyId.hashCode();
        } else {
            i3 = 0;
        }
        int i8 = (i7 + i3) * HASHCODE_RANDOM;
        if (this.pkgName != null) {
            i4 = this.pkgName.hashCode();
        }
        return i8 + i4;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.pid.intValue());
        dest.writeInt(this.uid.intValue());
        if (this.pkgName != null) {
            dest.writeInt(1);
            dest.writeString(this.pkgName);
        } else {
            dest.writeInt(0);
        }
        dest.writeInt(this.type.ordinal());
        dest.writeInt(this.proxyId.intValue());
    }

    public String toString() {
        return "ObserverInfo{pid=" + this.pid + ", uid=" + this.uid + ", type=" + this.type + ", proxyId=" + this.proxyId + '}';
    }
}
