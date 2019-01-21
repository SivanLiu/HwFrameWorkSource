package com.android.internal.os;

public class FuseUnavailableMountException extends Exception {
    public FuseUnavailableMountException(int mountId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("AppFuse mount point ");
        stringBuilder.append(mountId);
        stringBuilder.append(" is unavailable");
        super(stringBuilder.toString());
    }
}
