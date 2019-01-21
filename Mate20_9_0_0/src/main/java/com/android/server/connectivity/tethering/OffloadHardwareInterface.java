package com.android.server.connectivity.tethering;

import android.hardware.tetheroffload.control.V1_0.IOffloadControl;
import android.hardware.tetheroffload.control.V1_0.ITetheringOffloadCallback.Stub;
import android.hardware.tetheroffload.control.V1_0.NatTimeoutUpdate;
import android.net.util.SharedLog;
import android.os.Handler;
import android.os.RemoteException;
import android.system.OsConstants;
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

    private static class CbResults {
        String errMsg;
        boolean success;

        private CbResults() {
        }

        public String toString() {
            if (this.success) {
                return "ok";
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("fail: ");
            stringBuilder.append(this.errMsg);
            return stringBuilder.toString();
        }
    }

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
            this.handler.post(new -$$Lambda$OffloadHardwareInterface$TetheringOffloadCallback$nv6rlSkSWXyiDHH-quQiDc8IaU0(this, event));
        }

        public static /* synthetic */ void lambda$onEvent$0(TetheringOffloadCallback tetheringOffloadCallback, int event) {
            switch (event) {
                case 1:
                    tetheringOffloadCallback.controlCb.onStarted();
                    return;
                case 2:
                    tetheringOffloadCallback.controlCb.onStoppedError();
                    return;
                case 3:
                    tetheringOffloadCallback.controlCb.onStoppedUnsupported();
                    return;
                case 4:
                    tetheringOffloadCallback.controlCb.onSupportAvailable();
                    return;
                case 5:
                    tetheringOffloadCallback.controlCb.onStoppedLimitReached();
                    return;
                default:
                    SharedLog sharedLog = tetheringOffloadCallback.log;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unsupported OffloadCallbackEvent: ");
                    stringBuilder.append(event);
                    sharedLog.e(stringBuilder.toString());
                    return;
            }
        }

        public void updateTimeout(NatTimeoutUpdate params) {
            this.handler.post(new -$$Lambda$OffloadHardwareInterface$TetheringOffloadCallback$iUwkHUaFse6usZpm7pExz3WDNoQ(this, params));
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
        StringBuilder stringBuilder;
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
                SharedLog sharedLog = this.mLog;
                stringBuilder = new StringBuilder();
                stringBuilder.append("tethering offload control not supported: ");
                stringBuilder.append(e);
                sharedLog.e(stringBuilder.toString());
                return false;
            }
        }
        String logmsg = "initOffloadControl(%s)";
        Object[] objArr = new Object[1];
        if (controlCb == null) {
            str = "null";
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("0x");
            stringBuilder.append(Integer.toHexString(System.identityHashCode(controlCb)));
            str = stringBuilder.toString();
        }
        objArr[0] = str;
        logmsg = String.format(logmsg, objArr);
        this.mTetheringOffloadCallback = new TetheringOffloadCallback(this.mHandler, this.mControlCallback, this.mLog);
        CbResults results = new CbResults();
        try {
            this.mOffloadControl.initOffload(this.mTetheringOffloadCallback, new -$$Lambda$OffloadHardwareInterface$324leYOM3BvGJiK4Wade-B0d5jE(results));
            record(logmsg, results);
            return results.success;
        } catch (RemoteException e2) {
            record(logmsg, e2);
            return false;
        }
    }

    static /* synthetic */ void lambda$initOffloadControl$0(CbResults results, boolean success, String errMsg) {
        results.success = success;
        results.errMsg = errMsg;
    }

    public void stopOffloadControl() {
        if (this.mOffloadControl != null) {
            try {
                this.mOffloadControl.stopOffload(new -$$Lambda$OffloadHardwareInterface$AOzzTRw82KskEfgGFRGSy26wGv8(this));
            } catch (RemoteException e) {
                SharedLog sharedLog = this.mLog;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("failed to stopOffload: ");
                stringBuilder.append(e);
                sharedLog.e(stringBuilder.toString());
            }
        }
        this.mOffloadControl = null;
        this.mTetheringOffloadCallback = null;
        this.mControlCallback = null;
        this.mLog.log("stopOffloadControl()");
    }

    public static /* synthetic */ void lambda$stopOffloadControl$1(OffloadHardwareInterface offloadHardwareInterface, boolean success, String errMsg) {
        if (!success) {
            SharedLog sharedLog = offloadHardwareInterface.mLog;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("stopOffload failed: ");
            stringBuilder.append(errMsg);
            sharedLog.e(stringBuilder.toString());
        }
    }

    public ForwardedStats getForwardedStats(String upstream) {
        String logmsg = String.format("getForwardedStats(%s)", new Object[]{upstream});
        ForwardedStats stats = new ForwardedStats();
        try {
            this.mOffloadControl.getForwardedStats(upstream, new -$$Lambda$OffloadHardwareInterface$nu77bP4WbZU9UPvjulauQE3Dm30(stats));
            SharedLog sharedLog = this.mLog;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(logmsg);
            stringBuilder.append(YIELDS);
            stringBuilder.append(stats);
            sharedLog.log(stringBuilder.toString());
            return stats;
        } catch (RemoteException e) {
            record(logmsg, e);
            return stats;
        }
    }

    static /* synthetic */ void lambda$getForwardedStats$2(ForwardedStats stats, long rxBytes, long txBytes) {
        long j = 0;
        stats.rxBytes = rxBytes > 0 ? rxBytes : 0;
        if (txBytes > 0) {
            j = txBytes;
        }
        stats.txBytes = j;
    }

    public boolean setLocalPrefixes(ArrayList<String> localPrefixes) {
        String logmsg = String.format("setLocalPrefixes([%s])", new Object[]{String.join(",", localPrefixes)});
        CbResults results = new CbResults();
        try {
            this.mOffloadControl.setLocalPrefixes(localPrefixes, new -$$Lambda$OffloadHardwareInterface$IpWViosH4sGe7yz1VTujaEKIDNQ(results));
            record(logmsg, results);
            return results.success;
        } catch (RemoteException e) {
            record(logmsg, e);
            return false;
        }
    }

    static /* synthetic */ void lambda$setLocalPrefixes$3(CbResults results, boolean success, String errMsg) {
        results.success = success;
        results.errMsg = errMsg;
    }

    public boolean setDataLimit(String iface, long limit) {
        String logmsg = String.format("setDataLimit(%s, %d)", new Object[]{iface, Long.valueOf(limit)});
        CbResults results = new CbResults();
        try {
            this.mOffloadControl.setDataLimit(iface, limit, new -$$Lambda$OffloadHardwareInterface$4gz9PGx-iHz6VaJglXvPXV_YCTo(results));
            record(logmsg, results);
            return results.success;
        } catch (RemoteException e) {
            record(logmsg, e);
            return false;
        }
    }

    static /* synthetic */ void lambda$setDataLimit$4(CbResults results, boolean success, String errMsg) {
        results.success = success;
        results.errMsg = errMsg;
    }

    public boolean setUpstreamParameters(String iface, String v4addr, String v4gateway, ArrayList<String> v6gws) {
        String logmsg = String.format("setUpstreamParameters(%s, %s, %s, [%s])", new Object[]{iface != null ? iface : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, v4addr != null ? v4addr : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, v4gateway != null ? v4gateway : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, String.join(",", v6gws != null ? v6gws : new ArrayList())});
        CbResults results = new CbResults();
        try {
            this.mOffloadControl.setUpstreamParameters(iface != null ? iface : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, v4addr != null ? v4addr : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, v4gateway != null ? v4gateway : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, v6gws, new -$$Lambda$OffloadHardwareInterface$2RWDK-fyqU5SThZDqBkZ1L_XSJA(results));
            record(logmsg, results);
            return results.success;
        } catch (RemoteException e) {
            record(logmsg, e);
            return false;
        }
    }

    static /* synthetic */ void lambda$setUpstreamParameters$5(CbResults results, boolean success, String errMsg) {
        results.success = success;
        results.errMsg = errMsg;
    }

    public boolean addDownstreamPrefix(String ifname, String prefix) {
        String logmsg = String.format("addDownstreamPrefix(%s, %s)", new Object[]{ifname, prefix});
        CbResults results = new CbResults();
        try {
            this.mOffloadControl.addDownstream(ifname, prefix, new -$$Lambda$OffloadHardwareInterface$GhKYJ09_bq-n9xoRpQeCc3ZpQPU(results));
            record(logmsg, results);
            return results.success;
        } catch (RemoteException e) {
            record(logmsg, e);
            return false;
        }
    }

    static /* synthetic */ void lambda$addDownstreamPrefix$6(CbResults results, boolean success, String errMsg) {
        results.success = success;
        results.errMsg = errMsg;
    }

    public boolean removeDownstreamPrefix(String ifname, String prefix) {
        String logmsg = String.format("removeDownstreamPrefix(%s, %s)", new Object[]{ifname, prefix});
        CbResults results = new CbResults();
        try {
            this.mOffloadControl.removeDownstream(ifname, prefix, new -$$Lambda$OffloadHardwareInterface$w6w__dI5-bH4oSI_P9WIdOzlG28(results));
            record(logmsg, results);
            return results.success;
        } catch (RemoteException e) {
            record(logmsg, e);
            return false;
        }
    }

    static /* synthetic */ void lambda$removeDownstreamPrefix$7(CbResults results, boolean success, String errMsg) {
        results.success = success;
        results.errMsg = errMsg;
    }

    private void record(String msg, Throwable t) {
        SharedLog sharedLog = this.mLog;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(msg);
        stringBuilder.append(YIELDS);
        stringBuilder.append("exception: ");
        stringBuilder.append(t);
        sharedLog.e(stringBuilder.toString());
    }

    private void record(String msg, CbResults results) {
        String logmsg = new StringBuilder();
        logmsg.append(msg);
        logmsg.append(YIELDS);
        logmsg.append(results);
        logmsg = logmsg.toString();
        if (results.success) {
            this.mLog.log(logmsg);
        } else {
            this.mLog.e(logmsg);
        }
    }

    private static int networkProtocolToOsConstant(int proto) {
        if (proto == 6) {
            return OsConstants.IPPROTO_TCP;
        }
        if (proto != 17) {
            return -Math.abs(proto);
        }
        return OsConstants.IPPROTO_UDP;
    }
}
