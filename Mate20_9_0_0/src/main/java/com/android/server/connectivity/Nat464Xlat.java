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

    private void enterStartingState(String baseIface) {
        String str;
        StringBuilder stringBuilder;
        try {
            this.mNMService.registerObserver(this);
            try {
                this.mNMService.startClatd(baseIface);
            } catch (RemoteException | IllegalStateException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Error starting clatd on ");
                stringBuilder.append(baseIface);
                Slog.e(str, stringBuilder.toString(), e);
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(CLAT_PREFIX);
            stringBuilder2.append(baseIface);
            this.mIface = stringBuilder2.toString();
            this.mBaseIface = baseIface;
            this.mState = State.STARTING;
        } catch (RemoteException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("startClat: Can't register interface observer for clat on ");
            stringBuilder.append(this.mNetwork.name());
            Slog.e(str, stringBuilder.toString());
        }
    }

    private void enterRunningState() {
        this.mState = State.RUNNING;
    }

    private void enterStoppingState() {
        try {
            this.mNMService.stopClatd(this.mBaseIface);
        } catch (RemoteException | IllegalStateException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error stopping clatd on ");
            stringBuilder.append(this.mBaseIface);
            Slog.e(str, stringBuilder.toString(), e);
        }
        this.mState = State.STOPPING;
    }

    private void enterIdleState() {
        try {
            this.mNMService.unregisterObserver(this);
        } catch (RemoteException | IllegalStateException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error unregistering clatd observer on ");
            stringBuilder.append(this.mBaseIface);
            Slog.e(str, stringBuilder.toString(), e);
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

    private LinkAddress getLinkAddress(String iface) {
        try {
            return this.mNMService.getInterfaceConfig(iface).getLinkAddress();
        } catch (RemoteException | IllegalStateException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Error getting link properties: ");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
            return null;
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
