package com.android.server.timezone;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

final class PackageStatus {
    static final int CHECK_COMPLETED_FAILURE = 3;
    static final int CHECK_COMPLETED_SUCCESS = 2;
    static final int CHECK_STARTED = 1;
    final int mCheckStatus;
    final PackageVersions mVersions;

    @Retention(RetentionPolicy.SOURCE)
    @interface CheckStatus {
    }

    PackageStatus(int checkStatus, PackageVersions versions) {
        this.mCheckStatus = checkStatus;
        if (checkStatus < 1 || checkStatus > 3) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown checkStatus ");
            stringBuilder.append(checkStatus);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (versions != null) {
            this.mVersions = versions;
        } else {
            throw new NullPointerException("versions == null");
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PackageStatus that = (PackageStatus) o;
        if (this.mCheckStatus != that.mCheckStatus) {
            return false;
        }
        return this.mVersions.equals(that.mVersions);
    }

    public int hashCode() {
        return (31 * this.mCheckStatus) + this.mVersions.hashCode();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PackageStatus{mCheckStatus=");
        stringBuilder.append(this.mCheckStatus);
        stringBuilder.append(", mVersions=");
        stringBuilder.append(this.mVersions);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
