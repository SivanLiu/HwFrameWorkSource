package com.huawei.systemmanager.notificationmanager;

import android.app.INotificationManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.ParceledListSlice;
import android.os.Bundle;
import android.os.RemoteException;
import java.util.List;

public class HwNotificationManagerImpl implements IHwNotificationManager {
    private static volatile IHwNotificationManager mInstance = null;
    private INotificationManager mImpl;

    public static synchronized IHwNotificationManager getInstance() {
        IHwNotificationManager iHwNotificationManager;
        synchronized (HwNotificationManagerImpl.class) {
            if (mInstance == null) {
                mInstance = new HwNotificationManagerImpl();
            }
            iHwNotificationManager = mInstance;
        }
        return iHwNotificationManager;
    }

    private HwNotificationManagerImpl() {
        this.mImpl = null;
        this.mImpl = NotificationManager.getService();
    }

    public boolean canShowBadge(String pkg, int uid) throws RemoteException {
        return this.mImpl.canShowBadge(pkg, uid);
    }

    public boolean areNotificationsEnabledForPackage(String pkg, int uid) throws RemoteException {
        return this.mImpl.areNotificationsEnabledForPackage(pkg, uid);
    }

    public void setShowBadge(String pkg, int uid, boolean showBadge) throws RemoteException {
        this.mImpl.setShowBadge(pkg, uid, showBadge);
    }

    public void setNotificationsEnabledForPackage(String pkg, int uid, boolean enabled) throws RemoteException {
        this.mImpl.setNotificationsEnabledForPackage(pkg, uid, enabled);
    }

    public List<NotificationChannel> getNotificationChannelsForPackage(String pkg, int uid, boolean includeDeleted) throws RemoteException {
        ParceledListSlice parceledListSlice = this.mImpl.getNotificationChannelsForPackage(pkg, uid, includeDeleted);
        if (parceledListSlice != null) {
            return parceledListSlice.getList();
        }
        return null;
    }

    public void updateNotificationChannelForPackage(String pkg, int uid, NotificationChannel channel) throws RemoteException {
        this.mImpl.updateNotificationChannelForPackage(pkg, uid, channel);
    }

    public NotificationChannel getNotificationChannelForPackage(String pkg, int uid, String channelId, boolean includeDeleted) throws RemoteException {
        return this.mImpl.getNotificationChannelForPackage(pkg, uid, channelId, includeDeleted);
    }

    public boolean matchesCallFilter(Bundle extras) throws RemoteException {
        return this.mImpl.matchesCallFilter(extras);
    }
}
