package com.android.server.emcom.daemon;

import android.os.Message;
import android.os.Parcel;
import com.android.server.emcom.xengine.XEngineConfigInfo.HicomMpipInfo;
import java.util.ArrayList;

public class DaemonCommand implements CommandsInterface {
    static final boolean DEBUG = false;
    static final String TAG = "DaemonCommand";
    private static DaemonCommand sInstance;
    private DaemonClientThread mDaemonClient = new DaemonClientThread();

    public interface DaemonReportCallback {
        void onReportDevFail();

        void onUpdateAppList(Parcel parcel);

        void onUpdateBrowserInfo(Parcel parcel);

        void onUpdateHttpInfo(Parcel parcel);

        void onUpdatePageId(int i);

        void onUpdateSampleWinStat(boolean z);

        void onUpdateTcpStatusInfo(Parcel parcel);
    }

    private DaemonCommand() {
    }

    public static synchronized DaemonCommand getInstance() {
        DaemonCommand daemonCommand;
        synchronized (DaemonCommand.class) {
            if (sInstance == null) {
                sInstance = new DaemonCommand();
            }
            daemonCommand = sInstance;
        }
        return daemonCommand;
    }

    public void execCloseSampleWin(Message result) {
        this.mDaemonClient.send(DaemonRequest.obtain(513, result));
    }

    public void exeBootComplete(Message result) {
        this.mDaemonClient.send(DaemonRequest.obtain(1, result));
    }

    public void exeAppForeground(int type, int uid, Message result) {
        DaemonRequest rr = DaemonRequest.obtain(2, result);
        rr.mParcel.writeInt(2);
        rr.mParcel.writeInt(type);
        rr.mParcel.writeInt(uid);
        this.mDaemonClient.send(rr);
    }

    public void exePackageChanged(int type, String packageName, Message result) {
        DaemonRequest rr = DaemonRequest.obtain(4, result);
        rr.mParcel.writeInt(type);
        rr.mParcel.writeString(packageName);
        this.mDaemonClient.send(rr);
    }

    public void exeScreenStatus(int type, Message result) {
        DaemonRequest rr = DaemonRequest.obtain(3, result);
        rr.mParcel.writeInt(1);
        rr.mParcel.writeInt(type);
        this.mDaemonClient.send(rr);
    }

    public void exeStopAccelerate(int uid, Message result) {
        DaemonRequest rr = DaemonRequest.obtain(CommandsInterface.EMCOM_SD_XENGINE_STOP_ACC, result);
        if (rr != null) {
            rr.mParcel.writeInt(1);
            rr.mParcel.writeInt(uid);
            this.mDaemonClient.send(rr);
        }
    }

    public void exeStartAccelerate(int uid, int actionGrade, int mainCardPsStatus, Message result) {
        DaemonRequest rr = DaemonRequest.obtain(CommandsInterface.EMCOM_SD_XENGINE_START_ACC, result);
        if (rr != null) {
            rr.mParcel.writeInt(3);
            rr.mParcel.writeInt(uid);
            rr.mParcel.writeInt(actionGrade);
            rr.mParcel.writeInt(mainCardPsStatus);
            this.mDaemonClient.send(rr);
        }
    }

    public void exeConfigMpip(ArrayList<HicomMpipInfo> multiPathInfoList, Message result) {
        DaemonRequest rr = DaemonRequest.obtain(CommandsInterface.EMCOM_SD_XENGINE_CONFIG_MPIP, result);
        int size = multiPathInfoList.size();
        if (rr != null) {
            if (size > 0) {
                rr.mParcel.writeInt(size * 2);
                for (int i = 0; i < size; i++) {
                    rr.mParcel.writeInt(((HicomMpipInfo) multiPathInfoList.get(i)).mUid);
                    rr.mParcel.writeInt(((HicomMpipInfo) multiPathInfoList.get(i)).multiPathType);
                }
            } else {
                rr.mParcel.writeInt(2);
                rr.mParcel.writeInt(0);
                rr.mParcel.writeInt(0);
            }
            this.mDaemonClient.send(rr);
        }
    }

    public void exeStartMpip(String ifname, Message result) {
        DaemonRequest rr = DaemonRequest.obtain(CommandsInterface.EMCOM_SD_XENGINE_START_MPIP, result);
        if (rr != null) {
            rr.mParcel.writeString(ifname);
            this.mDaemonClient.send(rr);
        }
    }

    public void exeStopMpip(Message result) {
        DaemonRequest rr = DaemonRequest.obtain(CommandsInterface.EMCOM_SD_XENGINE_STOP_MPIP, result);
        if (rr != null) {
            this.mDaemonClient.send(rr);
        }
    }

    public void registerDaemonCallback(DaemonReportCallback cb) {
        this.mDaemonClient.registerDaemonCallback(cb);
    }

    public void unRegisterDaemonCallback(DaemonReportCallback cb) {
        this.mDaemonClient.unRegisterDaemonCallback(cb);
    }

    public void exeConfigUpdate(Message result) {
        this.mDaemonClient.send(DaemonRequest.obtain(5, result));
    }

    public void exeSpeedCtrl(int uid, int size, Message result) {
        DaemonRequest rr = DaemonRequest.obtain(CommandsInterface.EMCOM_SD_XENGINE_SPEED_CTRL, result);
        if (rr != null) {
            rr.mParcel.writeInt(2);
            rr.mParcel.writeInt(uid);
            rr.mParcel.writeInt(size);
            this.mDaemonClient.send(rr);
        }
    }

    public void exeUdpAcc(int uid, Message result) {
        DaemonRequest rr = DaemonRequest.obtain(260, result);
        if (rr != null) {
            rr.mParcel.writeInt(1);
            rr.mParcel.writeInt(uid);
            this.mDaemonClient.send(rr);
        }
    }

    public void exeUdpStop(int uid, Message result) {
        DaemonRequest rr = DaemonRequest.obtain(CommandsInterface.EMCOM_SD_STOP_UDP_RETRAN, result);
        if (rr != null) {
            rr.mParcel.writeInt(1);
            rr.mParcel.writeInt(uid);
            this.mDaemonClient.send(rr);
        }
    }

    public void exeAddConfigProxy(String pkgName, String version, int iUid, int iPort, int iMultiFlowNum, int iHttpAccMode, boolean bEnableMpIp, boolean bRandomMpip, float fThresholdLen, Message result) {
        DaemonRequest rr = DaemonRequest.obtain(CommandsInterface.EMCOM_SD_XENGINE_ADD_PROXY_CONFIG, result);
        int enableMpip = bEnableMpIp ? 1 : 0;
        int randomMpip = bRandomMpip ? 1 : 0;
        if (rr != null) {
            rr.mParcel.writeString(pkgName);
            rr.mParcel.writeString(version);
            rr.mParcel.writeFloat(fThresholdLen);
            rr.mParcel.writeInt(6);
            rr.mParcel.writeInt(iUid);
            rr.mParcel.writeInt(iPort);
            rr.mParcel.writeInt(iMultiFlowNum);
            rr.mParcel.writeInt(iHttpAccMode);
            rr.mParcel.writeInt(enableMpip);
            rr.mParcel.writeInt(randomMpip);
            this.mDaemonClient.send(rr);
        }
    }

    public void exeRemoveConfigProxy(String pkgName, String version, int iUid, int iPort, int iMultiFlowNum, int iHttpAccMode, boolean bEnableMpIp, boolean bRandomMpip, float fThresholdLen, Message result) {
        DaemonRequest rr = DaemonRequest.obtain(CommandsInterface.EMCOM_SD_XENGINE_REMOVE_PROXY_CONFIG, result);
        int enableMpip = bEnableMpIp ? 1 : 0;
        int randomMpip = bRandomMpip ? 1 : 0;
        if (rr != null) {
            rr.mParcel.writeString(pkgName);
            rr.mParcel.writeString(version);
            rr.mParcel.writeFloat(fThresholdLen);
            rr.mParcel.writeInt(6);
            rr.mParcel.writeInt(iUid);
            rr.mParcel.writeInt(iPort);
            rr.mParcel.writeInt(iMultiFlowNum);
            rr.mParcel.writeInt(iHttpAccMode);
            rr.mParcel.writeInt(enableMpip);
            rr.mParcel.writeInt(randomMpip);
            this.mDaemonClient.send(rr);
        }
    }
}
