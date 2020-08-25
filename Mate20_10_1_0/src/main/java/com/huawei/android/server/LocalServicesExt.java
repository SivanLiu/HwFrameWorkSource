package com.huawei.android.server;

import com.android.server.LocalServices;

public class LocalServicesExt {
    public static <T> T getService(Class<T> type) {
        return (T) LocalServices.getService(type);
    }
}
