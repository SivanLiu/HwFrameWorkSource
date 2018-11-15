package com.android.server.mtm.iaware.appmng.policy;

public abstract class Policy {
    String mPackageName;
    int mPolicy;
    String mReason;

    public String getPackageName() {
        return this.mPackageName;
    }

    public int getPolicy() {
        return this.mPolicy;
    }

    public String getReason() {
        return this.mReason;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        stringBuilder.append(this.mPackageName);
        stringBuilder.append(",");
        stringBuilder.append(this.mPolicy);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}
