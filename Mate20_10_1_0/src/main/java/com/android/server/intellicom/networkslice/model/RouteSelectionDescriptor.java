package com.android.server.intellicom.networkslice.model;

import android.os.Bundle;
import java.util.Objects;

public class RouteSelectionDescriptor {
    public static final int INVAILID_PDU_SESSION_TYPE = -1;
    private static final String RSD_DNN = "dnn";
    private static final String RSD_PDU_SESSION_TYPE = "pduSessionType";
    private static final String RSD_SNSSAI = "sNssai";
    private static final String RSD_SSCMODE = "sscMode";
    private final String mDnn;
    private int mPduSessionType;
    private final String mSnssai;
    private final byte mSscMode;

    private RouteSelectionDescriptor(Builder builder) {
        this.mPduSessionType = -1;
        this.mSscMode = builder.mSscMode;
        this.mDnn = builder.mDnn;
        this.mSnssai = builder.mSnssai;
        this.mPduSessionType = builder.mPduSessionType;
    }

    public static RouteSelectionDescriptor makeRouteSelectionDescriptor(Bundle data) {
        return new Builder().setSnssai(data.getString(RSD_SNSSAI, "")).setDnn(data.getString(RSD_DNN, "")).setSscMode(data.getByte(RSD_SSCMODE, (byte) 0).byteValue()).setPduSessionType(data.getInt(RSD_PDU_SESSION_TYPE, -1)).build();
    }

    public byte getSscMode() {
        return this.mSscMode;
    }

    public String getSnssai() {
        return this.mSnssai;
    }

    public String getDnn() {
        return this.mDnn;
    }

    public int getPduSessionType() {
        return this.mPduSessionType;
    }

    public static final class Builder {
        /* access modifiers changed from: private */
        public String mDnn;
        /* access modifiers changed from: private */
        public int mPduSessionType;
        /* access modifiers changed from: private */
        public String mSnssai;
        /* access modifiers changed from: private */
        public byte mSscMode;

        public Builder setSscMode(byte sscMode) {
            this.mSscMode = sscMode;
            return this;
        }

        public Builder setSnssai(String snssai) {
            this.mSnssai = snssai;
            return this;
        }

        public Builder setDnn(String dnn) {
            this.mDnn = dnn;
            return this;
        }

        public Builder setPduSessionType(int pduSessionType) {
            this.mPduSessionType = pduSessionType;
            return this;
        }

        public RouteSelectionDescriptor build() {
            return new RouteSelectionDescriptor(this);
        }
    }

    public String toString() {
        return "RouteSelectionDescriptor{mSscMode=" + ((int) this.mSscMode) + ", mDnn='" + this.mDnn + '\'' + ", mSnssai='" + this.mSnssai + '\'' + ", mPduSessionType='" + this.mPduSessionType + '\'' + '}';
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RouteSelectionDescriptor that = (RouteSelectionDescriptor) o;
        if (this.mSscMode != that.mSscMode || this.mPduSessionType != that.mPduSessionType || !Objects.equals(this.mDnn, that.mDnn) || !Objects.equals(this.mSnssai, that.mSnssai)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return Objects.hash(Byte.valueOf(this.mSscMode), this.mDnn, this.mSnssai, Integer.valueOf(this.mPduSessionType));
    }
}
