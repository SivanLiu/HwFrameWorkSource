package com.huawei.server.camera;

import android.content.Context;
import android.util.Slog;
import com.android.server.camera.IHwCameraServiceProxy;

public class HwCameraServiceFactory {
    private static final Object LOCK = new Object();
    private static final String TAG = "HwCameraServiceFactory";
    private static volatile HwCameraServiceFactory obj = null;
    private DefaultHwCameraServiceProxy hwCameraServiceProxy;

    public static HwCameraServiceFactory getHwCameraServiceFactory() {
        HwCameraServiceFactory hwCameraServiceFactory;
        synchronized (LOCK) {
            if (obj == null) {
                obj = new HwCameraServiceFactory();
                obj = obj;
            }
            hwCameraServiceFactory = obj;
        }
        return hwCameraServiceFactory;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r1v1, resolved type: com.huawei.server.camera.DefaultHwCameraServiceProxy */
    /* JADX DEBUG: Multi-variable search result rejected for r1v2, resolved type: com.huawei.server.camera.DefaultHwCameraServiceProxy */
    /* JADX DEBUG: Multi-variable search result rejected for r1v3, resolved type: com.huawei.server.camera.DefaultHwCameraServiceProxy */
    /* JADX DEBUG: Multi-variable search result rejected for r1v4, resolved type: com.huawei.server.camera.DefaultHwCameraServiceProxy */
    /* JADX DEBUG: Multi-variable search result rejected for r1v5, resolved type: com.huawei.server.camera.DefaultHwCameraServiceProxy */
    /* JADX DEBUG: Multi-variable search result rejected for r1v6, resolved type: com.huawei.server.camera.DefaultHwCameraServiceProxy */
    /* JADX WARN: Multi-variable type inference failed */
    public IHwCameraServiceProxy getHwCameraServiceProxy(Context context) {
        Slog.i(TAG, "getHwCameraServiceProxy");
        Object hwCameraServiceFactory = null;
        hwCameraServiceFactory = null;
        hwCameraServiceFactory = null;
        hwCameraServiceFactory = null;
        try {
            hwCameraServiceFactory = Class.forName("com.huawei.server.camera.HwCameraServiceProxy").newInstance();
        } catch (ClassNotFoundException e) {
            Slog.e(TAG, "ClassNotFoundException");
        } catch (InstantiationException e2) {
            Slog.e(TAG, "InstantiationException");
        } catch (IllegalAccessException e3) {
            Slog.e(TAG, "IllegalAccessException");
        } catch (Exception e4) {
            Slog.e(TAG, "Other exception.");
        }
        if (hwCameraServiceFactory instanceof DefaultHwCameraServiceProxy) {
            this.hwCameraServiceProxy = (DefaultHwCameraServiceProxy) hwCameraServiceFactory;
        } else {
            Slog.i(TAG, "Use default hwCameraServiceProxy");
            this.hwCameraServiceProxy = new DefaultHwCameraServiceProxy();
        }
        this.hwCameraServiceProxy.initHwCameraServiceProxyParams(context);
        return this.hwCameraServiceProxy;
    }
}
