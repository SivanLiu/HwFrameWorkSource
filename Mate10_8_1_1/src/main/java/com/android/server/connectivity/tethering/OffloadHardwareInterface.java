package com.android.server.connectivity.tethering;

import android.hardware.tetheroffload.control.V1_0.IOffloadControl;
import android.hardware.tetheroffload.control.V1_0.ITetheringOffloadCallback.Stub;
import android.hardware.tetheroffload.control.V1_0.NatTimeoutUpdate;
import android.net.util.SharedLog;
import android.os.Handler;
import android.os.RemoteException;
import android.system.OsConstants;
import com.android.internal.util.BitUtils;
import com.android.server.connectivity.tethering.-$Lambda$LVMU292iEsklodYmav2xkNUv4MU.AnonymousClass1;
import com.android.server.connectivity.tethering.-$Lambda$LVMU292iEsklodYmav2xkNUv4MU.AnonymousClass2;
import com.android.server.connectivity.tethering.-$Lambda$LVMU292iEsklodYmav2xkNUv4MU.AnonymousClass3;
import com.android.server.connectivity.tethering.-$Lambda$LVMU292iEsklodYmav2xkNUv4MU.AnonymousClass4;
import com.android.server.connectivity.tethering.-$Lambda$LVMU292iEsklodYmav2xkNUv4MU.AnonymousClass5;
import com.android.server.connectivity.tethering.-$Lambda$LVMU292iEsklodYmav2xkNUv4MU.AnonymousClass6;
import com.android.server.connectivity.tethering.-$Lambda$LVMU292iEsklodYmav2xkNUv4MU.AnonymousClass7;
import com.android.server.connectivity.tethering.-$Lambda$LVMU292iEsklodYmav2xkNUv4MU.AnonymousClass8;
import com.android.server.connectivity.tethering.-$Lambda$LVMU292iEsklodYmav2xkNUv4MU.AnonymousClass9;
import java.util.ArrayList;

public class OffloadHardwareInterface {
    private static final int DEFAULT_TETHER_OFFLOAD_DISABLED = 0;
    private static final String NO_INTERFACE_NAME = "";
    private static final String NO_IPV4_ADDRESS = "";
    private static final String NO_IPV4_GATEWAY = "";
    private static final String TAG = OffloadHardwareInterface.class.getSimpleName();
    private static final String YIELDS = " -> ";
    private ControlCallback mControlCallback;
    private final Handler mHandler;
    private final SharedLog mLog;
    private IOffloadControl mOffloadControl;
    private TetheringOffloadCallback mTetheringOffloadCallback;

    public static class ControlCallback {
        public void onStarted() {
        }

        public void onStoppedError() {
        }

        public void onStoppedUnsupported() {
        }

        public void onSupportAvailable() {
        }

        public void onStoppedLimitReached() {
        }

        public void onNatTimeoutUpdate(int proto, String srcAddr, int srcPort, String dstAddr, int dstPort) {
        }
    }

    private static class CbResults {
        String errMsg;
        boolean success;

        private CbResults() {
        }

        public String toString() {
            if (this.success) {
                return "ok";
            }
            return "fail: " + this.errMsg;
        }
    }

    public static class ForwardedStats {
        public long rxBytes = 0;
        public long txBytes = 0;

        public void add(ForwardedStats other) {
            this.rxBytes += other.rxBytes;
            this.txBytes += other.txBytes;
        }

        public String toString() {
            return String.format("rx:%s tx:%s", new Object[]{Long.valueOf(this.rxBytes), Long.valueOf(this.txBytes)});
        }
    }

    private static class TetheringOffloadCallback extends Stub {
        public final ControlCallback controlCb;
        public final Handler handler;
        public final SharedLog log;

        public TetheringOffloadCallback(Handler h, ControlCallback cb, SharedLog sharedLog) {
            this.handler = h;
            this.controlCb = cb;
            this.log = sharedLog;
        }

        public void onEvent(int event) {
            this.handler.post(new AnonymousClass9(event, this));
        }

        /* synthetic */ void lambda$-com_android_server_connectivity_tethering_OffloadHardwareInterface$TetheringOffloadCallback_10626(int event) {
            switch (event) {
                case 1:
                    this.controlCb.onStarted();
                    return;
                case 2:
                    this.controlCb.onStoppedError();
                    return;
                case 3:
                    this.controlCb.onStoppedUnsupported();
                    return;
                case 4:
                    this.controlCb.onSupportAvailable();
                    return;
                case 5:
                    this.controlCb.onStoppedLimitReached();
                    return;
                default:
                    this.log.e("Unsupported OffloadCallbackEvent: " + event);
                    return;
            }
        }

        public void updateTimeout(NatTimeoutUpdate params) {
            this.handler.post(new AnonymousClass8(this, params));
        }

        /* synthetic */ void lambda$-com_android_server_connectivity_tethering_OffloadHardwareInterface$TetheringOffloadCallback_11706(NatTimeoutUpdate params) {
            this.controlCb.onNatTimeoutUpdate(OffloadHardwareInterface.networkProtocolToOsConstant(params.proto), params.src.addr, BitUtils.uint16(params.src.port), params.dst.addr, BitUtils.uint16(params.dst.port));
        }
    }

    private static native boolean configOffload();

    public OffloadHardwareInterface(Handler h, SharedLog log) {
        this.mHandler = h;
        this.mLog = log.forSubComponent(TAG);
    }

    public int getDefaultTetherOffloadDisabled() {
        return 0;
    }

    public boolean initOffloadConfig() {
        return configOffload();
    }

    public boolean initOffloadControl(ControlCallback controlCb) {
        String str;
        this.mControlCallback = controlCb;
        if (this.mOffloadControl == null) {
            try {
                this.mOffloadControl = IOffloadControl.getService();
                if (this.mOffloadControl == null) {
                    this.mLog.e("tethering IOffloadControl.getService() returned null");
                    return false;
                }
            } catch (RemoteException e) {
                this.mLog.e("tethering offload control not supported: " + e);
                return false;
            }
        }
        String str2 = "initOffloadControl(%s)";
        Object[] objArr = new Object[1];
        if (controlCb == null) {
            str = "null";
        } else {
            str = "0x" + Integer.toHexString(System.identityHashCode(controlCb));
        }
        objArr[0] = str;
        String logmsg = String.format(str2, objArr);
        this.mTetheringOffloadCallback = new TetheringOffloadCallback(this.mHandler, this.mControlCallback, this.mLog);
        CbResults results = new CbResults();
        try {
            this.mOffloadControl.initOffload(this.mTetheringOffloadCallback, new AnonymousClass2(results));
            record(logmsg, results);
            return results.success;
        } catch (Throwable e2) {
            record(logmsg, e2);
            return false;
        }
    }

    static /* synthetic */ void lambda$-com_android_server_connectivity_tethering_OffloadHardwareInterface_4483(CbResults results, boolean success, String errMsg) {
        results.success = success;
        results.errMsg = errMsg;
    }

    public void stopOffloadControl() {
        if (this.mOffloadControl != null) {
            try {
                this.mOffloadControl.stopOffload(new AnonymousClass7(this));
            } catch (RemoteException e) {
                this.mLog.e("failed to stopOffload: " + e);
            }
        }
        this.mOffloadControl = null;
        this.mTetheringOffloadCallback = null;
        this.mControlCallback = null;
        this.mLog.log("stopOffloadControl()");
    }

    /* synthetic */ void lambda$-com_android_server_connectivity_tethering_OffloadHardwareInterface_4988(boolean success, String errMsg) {
        if (!success) {
            this.mLog.e("stopOffload failed: " + errMsg);
        }
    }

    public ForwardedStats getForwardedStats(String upstream) {
        String logmsg = String.format("getForwardedStats(%s)", new Object[]{upstream});
        ForwardedStats stats = new ForwardedStats();
        try {
            this.mOffloadControl.getForwardedStats(upstream, new AnonymousClass1(stats));
            this.mLog.log(logmsg + YIELDS + stats);
            return stats;
        } catch (Throwable e) {
            record(logmsg, e);
            return stats;
        }
    }

    static /* synthetic */ void lambda$-com_android_server_connectivity_tethering_OffloadHardwareInterface_5729(ForwardedStats stats, long rxBytes, long txBytes) {
        if (rxBytes <= 0) {
            rxBytes = 0;
        }
        stats.rxBytes = rxBytes;
        if (txBytes <= 0) {
            txBytes = 0;
        }
        stats.txBytes = txBytes;
    }

    public boolean setLocalPrefixes(ArrayList<String> localPrefixes) {
        String logmsg = String.format("setLocalPrefixes([%s])", new Object[]{String.join(",", localPrefixes)});
        CbResults results = new CbResults();
        try {
            this.mOffloadControl.setLocalPrefixes(localPrefixes, new AnonymousClass5(results));
            record(logmsg, results);
            return results.success;
        } catch (Throwable e) {
            record(logmsg, e);
            return false;
        }
    }

    static /* synthetic */ void lambda$-com_android_server_connectivity_tethering_OffloadHardwareInterface_6440(CbResults results, boolean success, String errMsg) {
        results.success = success;
        results.errMsg = errMsg;
    }

    public boolean setDataLimit(String iface, long limit) {
        String logmsg = String.format("setDataLimit(%s, %d)", new Object[]{iface, Long.valueOf(limit)});
        CbResults results = new CbResults();
        try {
            this.mOffloadControl.setDataLimit(iface, limit, new AnonymousClass4(results));
            record(logmsg, results);
            return results.success;
        } catch (Throwable e) {
            record(logmsg, e);
            return false;
        }
    }

    static /* synthetic */ void lambda$-com_android_server_connectivity_tethering_OffloadHardwareInterface_7086(CbResults results, boolean success, String errMsg) {
        results.success = success;
        results.errMsg = errMsg;
    }

    public boolean setUpstreamParameters(String iface, String v4addr, String v4gateway, ArrayList<String> v6gws) {
        if (iface == null) {
            iface = "";
        }
        if (v4addr == null) {
            v4addr = "";
        }
        if (v4gateway == null) {
            v4gateway = "";
        }
        if (v6gws == null) {
            v6gws = new ArrayList();
        }
        String logmsg = String.format("setUpstreamParameters(%s, %s, %s, [%s])", new Object[]{iface, v4addr, v4gateway, String.join(",", v6gws)});
        CbResults results = new CbResults();
        try {
            this.mOffloadControl.setUpstreamParameters(iface, v4addr, v4gateway, v6gws, new AnonymousClass6(results));
            record(logmsg, results);
            return results.success;
        } catch (Throwable e) {
            record(logmsg, e);
            return false;
        }
    }

    static /* synthetic */ void lambda$-com_android_server_connectivity_tethering_OffloadHardwareInterface_8155(CbResults results, boolean success, String errMsg) {
        results.success = success;
        results.errMsg = errMsg;
    }

    public boolean addDownstreamPrefix(String ifname, String prefix) {
        String logmsg = String.format("addDownstreamPrefix(%s, %s)", new Object[]{ifname, prefix});
        CbResults results = new CbResults();
        try {
            this.mOffloadControl.addDownstream(ifname, prefix, new -$Lambda$LVMU292iEsklodYmav2xkNUv4MU(results));
            record(logmsg, results);
            return results.success;
        } catch (Throwable e) {
            record(logmsg, e);
            return false;
        }
    }

    static /* synthetic */ void lambda$-com_android_server_connectivity_tethering_OffloadHardwareInterface_8802(CbResults results, boolean success, String errMsg) {
        results.success = success;
        results.errMsg = errMsg;
    }

    public boolean removeDownstreamPrefix(String ifname, String prefix) {
        String logmsg = String.format("removeDownstreamPrefix(%s, %s)", new Object[]{ifname, prefix});
        CbResults results = new CbResults();
        try {
            this.mOffloadControl.removeDownstream(ifname, prefix, new AnonymousClass3(results));
            record(logmsg, results);
            return results.success;
        } catch (Throwable e) {
            record(logmsg, e);
            return false;
        }
    }

    static /* synthetic */ void lambda$-com_android_server_connectivity_tethering_OffloadHardwareInterface_9458(CbResults results, boolean success, String errMsg) {
        results.success = success;
        results.errMsg = errMsg;
    }

    private void record(String msg, Throwable t) {
        this.mLog.e(msg + YIELDS + "exception: " + t);
    }

    private void record(String msg, CbResults results) {
        String logmsg = msg + YIELDS + results;
        if (results.success) {
            this.mLog.log(logmsg);
        } else {
            this.mLog.e(logmsg);
        }
    }

    private static int networkProtocolToOsConstant(int proto) {
        switch (proto) {
            case 6:
                return OsConstants.IPPROTO_TCP;
            case 17:
                return OsConstants.IPPROTO_UDP;
            default:
                return -Math.abs(proto);
        }
    }
}
