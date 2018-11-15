package com.huawei.nb.notification;

import android.os.Parcel;
import android.os.Process;
import android.support.annotation.NonNull;

public abstract class ObserverInfo {
    private Integer pid = null;
    private String pkgName = null;
    private Integer proxyId = null;
    private ObserverType type = null;
    private Integer uid = null;

    public Integer getPid() {
        return this.pid;
    }

    public Integer getUid() {
        return this.uid;
    }

    public String getPkgName() {
        return this.pkgName;
    }

    public void setProxyId(Integer proxyId) {
        this.proxyId = proxyId;
    }

    public ObserverType getType() {
        return this.type;
    }

    public static boolean equals(Object obj1, Object obj2) {
        if (obj1 == null || obj2 == null) {
            if (obj1 == obj2) {
                return true;
            }
            return false;
        } else if ((obj1 instanceof ObserverInfo) && (obj2 instanceof ObserverInfo)) {
            return ((ObserverInfo) obj1).equals(obj2);
        } else {
            return false;
        }
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ObserverInfo that = (ObserverInfo) o;
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
        if (this.type != that.type) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        int result;
        int hashCode;
        int i = 0;
        if (this.pid != null) {
            result = this.pid.hashCode();
        } else {
            result = 0;
        }
        int i2 = result * 31;
        if (this.uid != null) {
            hashCode = this.uid.hashCode();
        } else {
            hashCode = 0;
        }
        i2 = (i2 + hashCode) * 31;
        if (this.type != null) {
            hashCode = this.type.hashCode();
        } else {
            hashCode = 0;
        }
        hashCode = (i2 + hashCode) * 31;
        if (this.proxyId != null) {
            i = this.proxyId.hashCode();
        }
        return hashCode + i;
    }

    public ObserverInfo(@NonNull ObserverType type, String pkgName) {
        this.type = type;
        this.pid = Integer.valueOf(Process.myPid());
        this.uid = Integer.valueOf(Process.myUid());
        this.pkgName = pkgName;
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
