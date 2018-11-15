package com.android.internal.telephony.dataconnection;

import java.util.HashSet;

public class DataConnectionReasons {
    private DataAllowedReasonType mDataAllowedReason = DataAllowedReasonType.NONE;
    private HashSet<DataDisallowedReasonType> mDataDisallowedReasonSet = new HashSet();

    enum DataAllowedReasonType {
        NONE,
        NORMAL,
        UNMETERED_APN,
        RESTRICTED_REQUEST,
        EMERGENCY_APN
    }

    public enum DataDisallowedReasonType {
        DATA_DISABLED(false),
        ROAMING_DISABLED(false),
        NOT_ATTACHED(true),
        RECORD_NOT_LOADED(true),
        INVALID_PHONE_STATE(true),
        CONCURRENT_VOICE_DATA_NOT_ALLOWED(true),
        PS_RESTRICTED(true),
        UNDESIRED_POWER_STATE(true),
        INTERNAL_DATA_DISABLED(true),
        DEFAULT_DATA_UNSELECTED(true),
        RADIO_DISABLED_BY_CARRIER(true),
        APN_NOT_CONNECTABLE(true),
        ON_IWLAN(true),
        IN_ECBM(true),
        PS_RESTRICTED_BY_FDN(true);
        
        private boolean mIsHardReason;

        boolean isHardReason() {
            return this.mIsHardReason;
        }

        private DataDisallowedReasonType(boolean isHardReason) {
            this.mIsHardReason = isHardReason;
        }
    }

    void add(DataDisallowedReasonType reason) {
        this.mDataAllowedReason = DataAllowedReasonType.NONE;
        this.mDataDisallowedReasonSet.add(reason);
    }

    void add(DataAllowedReasonType reason) {
        this.mDataDisallowedReasonSet.clear();
        if (reason.ordinal() > this.mDataAllowedReason.ordinal()) {
            this.mDataAllowedReason = reason;
        }
    }

    public String toString() {
        StringBuilder reasonStr = new StringBuilder();
        if (this.mDataDisallowedReasonSet.size() > 0) {
            reasonStr.append("Data disallowed, reasons:");
            for (DataDisallowedReasonType reason : this.mDataDisallowedReasonSet) {
                reasonStr.append(" ").append(reason);
            }
        } else {
            reasonStr.append("Data allowed, reason:");
            reasonStr.append(" ").append(this.mDataAllowedReason);
        }
        return reasonStr.toString();
    }

    void copyFrom(DataConnectionReasons reasons) {
        this.mDataDisallowedReasonSet = reasons.mDataDisallowedReasonSet;
        this.mDataAllowedReason = reasons.mDataAllowedReason;
    }

    boolean allowed() {
        return this.mDataDisallowedReasonSet.size() == 0;
    }

    boolean contains(DataDisallowedReasonType reason) {
        return this.mDataDisallowedReasonSet.contains(reason);
    }

    public boolean containsOnly(DataDisallowedReasonType reason) {
        return this.mDataDisallowedReasonSet.size() == 1 ? contains(reason) : false;
    }

    boolean contains(DataAllowedReasonType reason) {
        return reason == this.mDataAllowedReason;
    }

    boolean containsHardDisallowedReasons() {
        for (DataDisallowedReasonType reason : this.mDataDisallowedReasonSet) {
            if (reason.isHardReason()) {
                return true;
            }
        }
        return false;
    }
}
