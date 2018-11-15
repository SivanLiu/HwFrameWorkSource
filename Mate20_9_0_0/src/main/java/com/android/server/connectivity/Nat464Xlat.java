package com.android.server.connectivity;

import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.RouteInfo;
import android.os.INetworkManagementService;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.internal.util.ArrayUtils;
import com.android.server.net.BaseNetworkObserver;
import java.net.Inet4Address;
import java.util.Objects;

public class Nat464Xlat extends BaseNetworkObserver {
    private static final String CLAT_PREFIX = "v4-";
    private static final android.net.NetworkInfo.State[] NETWORK_STATES = new android.net.NetworkInfo.State[]{android.net.NetworkInfo.State.CONNECTED, android.net.NetworkInfo.State.SUSPENDED};
    private static final int[] NETWORK_TYPES = new int[]{0, 1, 9};
    private static final String TAG = Nat464Xlat.class.getSimpleName();
    private String mBaseIface;
    private String mIface;
    private final INetworkManagementService mNMService;
    private final NetworkAgentInfo mNetwork;
    private State mState = State.IDLE;

    private enum State {
        IDLE,
        STARTING,
        RUNNING,
        STOPPING
    }

    public Nat464Xlat(INetworkManagementService nmService, NetworkAgentInfo nai) {
        this.mNMService = nmService;
        this.mNetwork = nai;
    }

    public static boolean requiresClat(NetworkAgentInfo nai) {
        int netType = nai.networkInfo.getType();
        boolean supported = ArrayUtils.contains(NETWORK_TYPES, nai.networkInfo.getType());
        boolean connected = nai.networkInfo.isConnected();
        boolean hasIPv4Address = nai.linkProperties != null && nai.linkProperties.hasIPv4Address();
        boolean doXlat = SystemProperties.getBoolean("persist.net.doxlat", true);
        if (!doXlat) {
            Slog.i(TAG, "Android Xlat is disabled");
        }
        if (!SystemProperties.getBoolean("gsm.net.doxlat", true)) {
            Slog.i(TAG, "Android XCAP Xlat is disabled");
            doXlat = false;
        }
        if (!supported || !connected || hasIPv4Address) {
            return false;
        }
        if (netType != 0 || doXlat) {
            return true;
        }
        return false;
    }

    public boolean isStarted() {
        return this.mState != State.IDLE;
    }

    public boolean isStarting() {
        return this.mState == State.STARTING;
    }

    public boolean isRunning() {
        return this.mState == State.RUNNING;
    }

    public boolean isStopping() {
        return this.mState == State.STOPPING;
    }

    /* JADX WARNING: Removed duplicated region for block: B:4:0x000c A:{Splitter: B:2:0x0006, ExcHandler: android.os.RemoteException (r0_2 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:4:0x000c, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:5:0x000d, code:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("Error starting clatd on ");
            r2.append(r5);
            android.util.Slog.e(r1, r2.toString(), r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void enterStartingState(String baseIface) {
        try {
            this.mNMService.registerObserver(this);
            try {
                this.mNMService.startClatd(baseIface);
            } catch (Exception e) {
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(CLAT_PREFIX);
            stringBuilder.append(baseIface);
            this.mIface = stringBuilder.toString();
            this.mBaseIface = baseIface;
            this.mState = State.STARTING;
        } catch (RemoteException e2) {
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("startClat: Can't register interface observer for clat on ");
            stringBuilder2.append(this.mNetwork.name());
            Slog.e(str, stringBuilder2.toString());
        }
    }

    private void enterRunningState() {
        this.mState = State.RUNNING;
    }

    /* JADX WARNING: Removed duplicated region for block: B:2:0x0008 A:{Splitter: B:0:0x0000, ExcHandler: android.os.RemoteException (r0_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:2:0x0008, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:3:0x0009, code:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("Error stopping clatd on ");
            r2.append(r4.mBaseIface);
            android.util.Slog.e(r1, r2.toString(), r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void enterStoppingState() {
        try {
            this.mNMService.stopClatd(this.mBaseIface);
        } catch (Exception e) {
        }
        this.mState = State.STOPPING;
    }

    /* JADX WARNING: Removed duplicated region for block: B:2:0x0006 A:{Splitter: B:0:0x0000, ExcHandler: android.os.RemoteException (r0_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:2:0x0006, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:3:0x0007, code:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("Error unregistering clatd observer on ");
            r2.append(r4.mBaseIface);
            android.util.Slog.e(r1, r2.toString(), r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void enterIdleState() {
        try {
            this.mNMService.unregisterObserver(this);
        } catch (Exception e) {
        }
        this.mIface = null;
        this.mBaseIface = null;
        this.mState = State.IDLE;
    }

    public void start() {
        if (isStarted()) {
            Slog.e(TAG, "startClat: already started");
        } else if (this.mNetwork.linkProperties == null) {
            Slog.e(TAG, "startClat: Can't start clat with null LinkProperties");
        } else {
            String baseIface = this.mNetwork.linkProperties.getInterfaceName();
            if (baseIface == null) {
                Slog.e(TAG, "startClat: Can't start clat on null interface");
                return;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Starting clatd on ");
            stringBuilder.append(baseIface);
            Slog.i(str, stringBuilder.toString());
            enterStartingState(baseIface);
        }
    }

    public void stop() {
        if (isStarted()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Stopping clatd on ");
            stringBuilder.append(this.mBaseIface);
            Slog.i(str, stringBuilder.toString());
            boolean wasStarting = isStarting();
            enterStoppingState();
            if (wasStarting) {
                enterIdleState();
            }
        }
    }

    public void fixupLinkProperties(LinkProperties oldLp, LinkProperties lp) {
        if (isRunning() && lp != null && !lp.getAllInterfaceNames().contains(this.mIface)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("clatd running, updating NAI for ");
            stringBuilder.append(this.mIface);
            Slog.d(str, stringBuilder.toString());
            for (LinkProperties stacked : oldLp.getStackedLinks()) {
                if (Objects.equals(this.mIface, stacked.getInterfaceName())) {
                    lp.addStackedLink(stacked);
                    return;
                }
            }
        }
    }

    private LinkProperties makeLinkProperties(LinkAddress clatAddress) {
        LinkProperties stacked = new LinkProperties();
        stacked.setInterfaceName(this.mIface);
        stacked.addRoute(new RouteInfo(new LinkAddress(Inet4Address.ANY, 0), clatAddress.getAddress(), this.mIface));
        stacked.addLinkAddress(clatAddress);
        return stacked;
    }

    /* JADX WARNING: Removed duplicated region for block: B:3:0x000b A:{Splitter: B:0:0x0000, ExcHandler: android.os.RemoteException (r0_2 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:3:0x000b, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:4:0x000c, code:
            r1 = TAG;
            r2 = new java.lang.StringBuilder();
            r2.append("Error getting link properties: ");
            r2.append(r0);
            android.util.Slog.e(r1, r2.toString());
     */
    /* JADX WARNING: Missing block: B:5:0x0023, code:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private LinkAddress getLinkAddress(String iface) {
        try {
            return this.mNMService.getInterfaceConfig(iface).getLinkAddress();
        } catch (Exception e) {
        }
    }

    private void handleInterfaceLinkStateChanged(String iface, boolean up) {
        if (isStarting() && up && Objects.equals(this.mIface, iface)) {
            LinkAddress clatAddress = getLinkAddress(iface);
            if (clatAddress == null) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("clatAddress was null for stacked iface ");
                stringBuilder.append(iface);
                Slog.e(str, stringBuilder.toString());
                return;
            }
            Slog.i(TAG, String.format("interface %s is up, adding stacked link %s on top of %s", new Object[]{this.mIface, this.mIface, this.mBaseIface}));
            enterRunningState();
            LinkProperties lp = new LinkProperties(this.mNetwork.linkProperties);
            lp.addStackedLink(makeLinkProperties(clatAddress));
            this.mNetwork.connService().handleUpdateLinkProperties(this.mNetwork, lp);
        }
    }

    private void handleInterfaceRemoved(String iface) {
        if (!Objects.equals(this.mIface, iface)) {
            return;
        }
        if (isRunning() || isStopping()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("interface ");
            stringBuilder.append(iface);
            stringBuilder.append(" removed");
            Slog.i(str, stringBuilder.toString());
            if (!isStopping()) {
                enterStoppingState();
            }
            enterIdleState();
            LinkProperties lp = new LinkProperties(this.mNetwork.linkProperties);
            lp.removeStackedLink(iface);
            this.mNetwork.connService().handleUpdateLinkProperties(this.mNetwork, lp);
        }
    }

    public void interfaceLinkStateChanged(String iface, boolean up) {
        this.mNetwork.handler().post(new -$$Lambda$Nat464Xlat$40jKHQd7R0zgcegyEyc9zPHKXVA(this, iface, up));
    }

    public void interfaceRemoved(String iface) {
        this.mNetwork.handler().post(new -$$Lambda$Nat464Xlat$PACHOP9HoYvr_jzHtIwFDy31Ud4(this, iface));
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mBaseIface: ");
        stringBuilder.append(this.mBaseIface);
        stringBuilder.append(", mIface: ");
        stringBuilder.append(this.mIface);
        stringBuilder.append(", mState: ");
        stringBuilder.append(this.mState);
        return stringBuilder.toString();
    }
}
