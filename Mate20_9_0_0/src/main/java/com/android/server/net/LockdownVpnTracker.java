package com.android.server.net;

import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.NetworkInfo.State;
import android.os.INetworkManagementService;
import android.security.KeyStore;
import android.text.TextUtils;
import android.util.Slog;
import com.android.internal.net.VpnConfig;
import com.android.internal.net.VpnProfile;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.Preconditions;
import com.android.server.ConnectivityService;
import com.android.server.EventLogTags;
import com.android.server.connectivity.Vpn;
import java.util.List;

public class LockdownVpnTracker {
    private static final String ACTION_LOCKDOWN_RESET = "com.android.server.action.LOCKDOWN_RESET";
    private static final int MAX_ERROR_COUNT = 4;
    private static final int ROOT_UID = 0;
    private static final String TAG = "LockdownVpnTracker";
    private String mAcceptedEgressIface;
    private String mAcceptedIface;
    private List<LinkAddress> mAcceptedSourceAddr;
    private final PendingIntent mConfigIntent;
    private final ConnectivityService mConnService;
    private final Context mContext;
    private int mErrorCount;
    private final INetworkManagementService mNetService;
    private final VpnProfile mProfile;
    private final PendingIntent mResetIntent;
    private BroadcastReceiver mResetReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            LockdownVpnTracker.this.reset();
        }
    };
    private final Object mStateLock = new Object();
    private final Vpn mVpn;

    public static boolean isEnabled() {
        return KeyStore.getInstance().contains("LOCKDOWN_VPN");
    }

    public LockdownVpnTracker(Context context, INetworkManagementService netService, ConnectivityService connService, Vpn vpn, VpnProfile profile) {
        this.mContext = (Context) Preconditions.checkNotNull(context);
        this.mNetService = (INetworkManagementService) Preconditions.checkNotNull(netService);
        this.mConnService = (ConnectivityService) Preconditions.checkNotNull(connService);
        this.mVpn = (Vpn) Preconditions.checkNotNull(vpn);
        this.mProfile = (VpnProfile) Preconditions.checkNotNull(profile);
        this.mConfigIntent = PendingIntent.getActivity(this.mContext, 0, new Intent("android.settings.VPN_SETTINGS"), 0);
        Intent resetIntent = new Intent(ACTION_LOCKDOWN_RESET);
        resetIntent.addFlags(1073741824);
        this.mResetIntent = PendingIntent.getBroadcast(this.mContext, 0, resetIntent, 0);
    }

    private void handleStateChangedLocked() {
        NetworkInfo egressInfo = this.mConnService.getActiveNetworkInfoUnfiltered();
        LinkProperties egressProp = this.mConnService.getActiveLinkProperties();
        NetworkInfo vpnInfo = this.mVpn.getNetworkInfo();
        VpnConfig vpnConfig = this.mVpn.getLegacyVpnConfig();
        boolean egressChanged = true;
        boolean egressDisconnected = egressInfo == null || State.DISCONNECTED.equals(egressInfo.getState());
        if (egressProp != null && TextUtils.equals(this.mAcceptedEgressIface, egressProp.getInterfaceName())) {
            egressChanged = false;
        }
        String egressTypeName = egressInfo == null ? null : ConnectivityManager.getNetworkTypeName(egressInfo.getType());
        String egressIface = egressProp == null ? null : egressProp.getInterfaceName();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleStateChanged: egress=");
        stringBuilder.append(egressTypeName);
        stringBuilder.append(" ");
        stringBuilder.append(this.mAcceptedEgressIface);
        stringBuilder.append("->");
        stringBuilder.append(egressIface);
        Slog.d(str, stringBuilder.toString());
        if (egressDisconnected || egressChanged) {
            this.mAcceptedEgressIface = null;
            this.mVpn.stopLegacyVpnPrivileged();
        }
        if (egressDisconnected) {
            hideNotification();
            return;
        }
        int egressType = egressInfo.getType();
        if (vpnInfo.getDetailedState() == DetailedState.FAILED) {
            EventLogTags.writeLockdownVpnError(egressType);
        }
        if (this.mErrorCount > 4) {
            showNotification(17041340, 17303739);
        } else if (!egressInfo.isConnected() || vpnInfo.isConnectedOrConnecting()) {
            if (vpnInfo.isConnected() && vpnConfig != null) {
                String iface = vpnConfig.interfaze;
                List<LinkAddress> sourceAddrs = vpnConfig.addresses;
                if (!TextUtils.equals(iface, this.mAcceptedIface) || !sourceAddrs.equals(this.mAcceptedSourceAddr)) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("VPN connected using iface=");
                    stringBuilder2.append(iface);
                    stringBuilder2.append(", sourceAddr=");
                    stringBuilder2.append(sourceAddrs.toString());
                    Slog.d(str2, stringBuilder2.toString());
                    EventLogTags.writeLockdownVpnConnected(egressType);
                    showNotification(17041337, 17303738);
                    NetworkInfo clone = new NetworkInfo(egressInfo);
                    augmentNetworkInfo(clone);
                    this.mConnService.sendConnectedBroadcast(clone);
                }
            }
        } else if (this.mProfile.isValidLockdownProfile()) {
            Slog.d(TAG, "Active network connected; starting VPN");
            EventLogTags.writeLockdownVpnConnecting(egressType);
            showNotification(17041338, 17303739);
            this.mAcceptedEgressIface = egressProp.getInterfaceName();
            try {
                this.mVpn.startLegacyVpnPrivileged(this.mProfile, KeyStore.getInstance(), egressProp);
            } catch (IllegalStateException e) {
                this.mAcceptedEgressIface = null;
                Slog.e(TAG, "Failed to start VPN", e);
                showNotification(17041340, 17303739);
            }
        } else {
            Slog.e(TAG, "Invalid VPN profile; requires IP-based server and DNS");
            showNotification(17041340, 17303739);
        }
    }

    public void init() {
        synchronized (this.mStateLock) {
            initLocked();
        }
    }

    private void initLocked() {
        Slog.d(TAG, "initLocked()");
        this.mVpn.setEnableTeardown(false);
        this.mVpn.setLockdown(true);
        this.mContext.registerReceiver(this.mResetReceiver, new IntentFilter(ACTION_LOCKDOWN_RESET), "android.permission.CONNECTIVITY_INTERNAL", null);
        handleStateChangedLocked();
    }

    public void shutdown() {
        synchronized (this.mStateLock) {
            shutdownLocked();
        }
    }

    private void shutdownLocked() {
        Slog.d(TAG, "shutdownLocked()");
        this.mAcceptedEgressIface = null;
        this.mErrorCount = 0;
        this.mVpn.stopLegacyVpnPrivileged();
        this.mVpn.setLockdown(false);
        hideNotification();
        this.mContext.unregisterReceiver(this.mResetReceiver);
        this.mVpn.setEnableTeardown(true);
    }

    public void reset() {
        Slog.d(TAG, "reset()");
        synchronized (this.mStateLock) {
            shutdownLocked();
            initLocked();
            handleStateChangedLocked();
        }
    }

    public void onNetworkInfoChanged() {
        synchronized (this.mStateLock) {
            handleStateChangedLocked();
        }
    }

    public void onVpnStateChanged(NetworkInfo info) {
        if (info.getDetailedState() == DetailedState.FAILED) {
            this.mErrorCount++;
        }
        synchronized (this.mStateLock) {
            handleStateChangedLocked();
        }
    }

    public void augmentNetworkInfo(NetworkInfo info) {
        if (info.isConnected()) {
            NetworkInfo vpnInfo = this.mVpn.getNetworkInfo();
            info.setDetailedState(vpnInfo.getDetailedState(), vpnInfo.getReason(), null);
        }
    }

    private void showNotification(int titleRes, int iconRes) {
        NotificationManager.from(this.mContext).notify(null, 20, new Builder(this.mContext, SystemNotificationChannels.VPN).setWhen(0).setSmallIcon(iconRes).setContentTitle(this.mContext.getString(titleRes)).setContentText(this.mContext.getString(17041336)).setContentIntent(this.mConfigIntent).setOngoing(true).addAction(17302680, this.mContext.getString(17040998), this.mResetIntent).setColor(this.mContext.getColor(17170784)).build());
    }

    private void hideNotification() {
        NotificationManager.from(this.mContext).cancel(null, 20);
    }
}
