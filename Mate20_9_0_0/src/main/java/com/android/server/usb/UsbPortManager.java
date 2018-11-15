package com.android.server.usb;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbPort;
import android.hardware.usb.UsbPortStatus;
import android.hardware.usb.V1_0.IUsb;
import android.hardware.usb.V1_0.PortRole;
import android.hardware.usb.V1_0.PortStatus;
import android.hardware.usb.V1_1.IUsbCallback;
import android.hardware.usb.V1_1.PortStatus_1_1;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification.Stub;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.RemoteException;
import android.util.ArrayMap;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.usb.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.dump.DualDumpOutputStream;
import com.android.server.FgThread;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class UsbPortManager {
    private static final int COMBO_SINK_DEVICE = UsbPort.combineRolesAsBit(2, 2);
    private static final int COMBO_SINK_HOST = UsbPort.combineRolesAsBit(2, 1);
    private static final int COMBO_SOURCE_DEVICE = UsbPort.combineRolesAsBit(1, 2);
    private static final int COMBO_SOURCE_HOST = UsbPort.combineRolesAsBit(1, 1);
    private static final int MSG_UPDATE_PORTS = 1;
    private static final String PORT_INFO = "port_info";
    private static final String TAG = "UsbPortManager";
    private static final int USB_HAL_DEATH_COOKIE = 1000;
    private final Context mContext;
    private HALCallback mHALCallback = new HALCallback(null, this);
    private final Handler mHandler = new Handler(FgThread.get().getLooper()) {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                ArrayList<RawPortInfo> PortInfo = msg.getData().getParcelableArrayList(UsbPortManager.PORT_INFO);
                synchronized (UsbPortManager.this.mLock) {
                    UsbPortManager.this.updatePortsLocked(null, PortInfo);
                }
            }
        }
    };
    private final Object mLock = new Object();
    private final ArrayMap<String, PortInfo> mPorts = new ArrayMap();
    @GuardedBy("mLock")
    private IUsb mProxy = null;
    private final ArrayMap<String, RawPortInfo> mSimulatedPorts = new ArrayMap();
    private boolean mSystemReady;

    private static final class PortInfo {
        public static final int DISPOSITION_ADDED = 0;
        public static final int DISPOSITION_CHANGED = 1;
        public static final int DISPOSITION_READY = 2;
        public static final int DISPOSITION_REMOVED = 3;
        public boolean mCanChangeDataRole;
        public boolean mCanChangeMode;
        public boolean mCanChangePowerRole;
        public int mDisposition;
        public final UsbPort mUsbPort;
        public UsbPortStatus mUsbPortStatus;

        public PortInfo(String portId, int supportedModes) {
            this.mUsbPort = new UsbPort(portId, supportedModes);
        }

        public boolean setStatus(int currentMode, boolean canChangeMode, int currentPowerRole, boolean canChangePowerRole, int currentDataRole, boolean canChangeDataRole, int supportedRoleCombinations) {
            this.mCanChangeMode = canChangeMode;
            this.mCanChangePowerRole = canChangePowerRole;
            this.mCanChangeDataRole = canChangeDataRole;
            if (this.mUsbPortStatus != null && this.mUsbPortStatus.getCurrentMode() == currentMode && this.mUsbPortStatus.getCurrentPowerRole() == currentPowerRole && this.mUsbPortStatus.getCurrentDataRole() == currentDataRole && this.mUsbPortStatus.getSupportedRoleCombinations() == supportedRoleCombinations) {
                return false;
            }
            this.mUsbPortStatus = new UsbPortStatus(currentMode, currentPowerRole, currentDataRole, supportedRoleCombinations);
            return true;
        }

        void dump(DualDumpOutputStream dump, String idName, long id) {
            long token = dump.start(idName, id);
            DumpUtils.writePort(dump, "port", 1146756268033L, this.mUsbPort);
            DumpUtils.writePortStatus(dump, "status", 1146756268034L, this.mUsbPortStatus);
            dump.write("can_change_mode", 1133871366147L, this.mCanChangeMode);
            dump.write("can_change_power_role", 1133871366148L, this.mCanChangePowerRole);
            dump.write("can_change_data_role", 1133871366149L, this.mCanChangeDataRole);
            dump.end(token);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("port=");
            stringBuilder.append(this.mUsbPort);
            stringBuilder.append(", status=");
            stringBuilder.append(this.mUsbPortStatus);
            stringBuilder.append(", canChangeMode=");
            stringBuilder.append(this.mCanChangeMode);
            stringBuilder.append(", canChangePowerRole=");
            stringBuilder.append(this.mCanChangePowerRole);
            stringBuilder.append(", canChangeDataRole=");
            stringBuilder.append(this.mCanChangeDataRole);
            return stringBuilder.toString();
        }
    }

    private static final class RawPortInfo implements Parcelable {
        public static final Creator<RawPortInfo> CREATOR = new Creator<RawPortInfo>() {
            public RawPortInfo createFromParcel(Parcel in) {
                return new RawPortInfo(in.readString(), in.readInt(), in.readInt(), in.readByte() != (byte) 0, in.readInt(), in.readByte() != (byte) 0, in.readInt(), in.readByte() != (byte) 0);
            }

            public RawPortInfo[] newArray(int size) {
                return new RawPortInfo[size];
            }
        };
        public boolean canChangeDataRole;
        public boolean canChangeMode;
        public boolean canChangePowerRole;
        public int currentDataRole;
        public int currentMode;
        public int currentPowerRole;
        public final String portId;
        public final int supportedModes;

        RawPortInfo(String portId, int supportedModes) {
            this.portId = portId;
            this.supportedModes = supportedModes;
        }

        RawPortInfo(String portId, int supportedModes, int currentMode, boolean canChangeMode, int currentPowerRole, boolean canChangePowerRole, int currentDataRole, boolean canChangeDataRole) {
            this.portId = portId;
            this.supportedModes = supportedModes;
            this.currentMode = currentMode;
            this.canChangeMode = canChangeMode;
            this.currentPowerRole = currentPowerRole;
            this.canChangePowerRole = canChangePowerRole;
            this.currentDataRole = currentDataRole;
            this.canChangeDataRole = canChangeDataRole;
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.portId);
            dest.writeInt(this.supportedModes);
            dest.writeInt(this.currentMode);
            dest.writeByte((byte) this.canChangeMode);
            dest.writeInt(this.currentPowerRole);
            dest.writeByte((byte) this.canChangePowerRole);
            dest.writeInt(this.currentDataRole);
            dest.writeByte((byte) this.canChangeDataRole);
        }
    }

    final class ServiceNotification extends Stub {
        ServiceNotification() {
        }

        public void onRegistration(String fqName, String name, boolean preexisting) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Usb hal service started ");
            stringBuilder.append(fqName);
            stringBuilder.append(" ");
            stringBuilder.append(name);
            UsbPortManager.logAndPrint(4, null, stringBuilder.toString());
            UsbPortManager.this.connectToProxy(null);
        }
    }

    final class DeathRecipient implements android.os.IHwBinder.DeathRecipient {
        public IndentingPrintWriter pw;

        DeathRecipient(IndentingPrintWriter pw) {
            this.pw = pw;
        }

        public void serviceDied(long cookie) {
            if (cookie == 1000) {
                IndentingPrintWriter indentingPrintWriter = this.pw;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Usb hal service died cookie: ");
                stringBuilder.append(cookie);
                UsbPortManager.logAndPrint(6, indentingPrintWriter, stringBuilder.toString());
                synchronized (UsbPortManager.this.mLock) {
                    UsbPortManager.this.mProxy = null;
                }
            }
        }
    }

    private static class HALCallback extends IUsbCallback.Stub {
        public UsbPortManager portManager;
        public IndentingPrintWriter pw;

        HALCallback(IndentingPrintWriter pw, UsbPortManager portManager) {
            this.pw = pw;
            this.portManager = portManager;
        }

        public void notifyPortStatusChange(ArrayList<PortStatus> currentPortStatus, int retval) {
            if (!this.portManager.mSystemReady) {
                return;
            }
            if (retval != 0) {
                UsbPortManager.logAndPrint(6, this.pw, "port status enquiry failed");
                return;
            }
            ArrayList<RawPortInfo> newPortInfo = new ArrayList();
            Iterator it = currentPortStatus.iterator();
            while (it.hasNext()) {
                PortStatus current = (PortStatus) it.next();
                newPortInfo.add(new RawPortInfo(current.portName, current.supportedModes, current.currentMode, current.canChangeMode, current.currentPowerRole, current.canChangePowerRole, current.currentDataRole, current.canChangeDataRole));
                IndentingPrintWriter indentingPrintWriter = this.pw;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ClientCallback: ");
                stringBuilder.append(current.portName);
                UsbPortManager.logAndPrint(4, indentingPrintWriter, stringBuilder.toString());
            }
            Message message = this.portManager.mHandler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList(UsbPortManager.PORT_INFO, newPortInfo);
            message.what = 1;
            message.setData(bundle);
            this.portManager.mHandler.sendMessage(message);
        }

        public void notifyPortStatusChange_1_1(ArrayList<PortStatus_1_1> currentPortStatus, int retval) {
            if (!this.portManager.mSystemReady) {
                return;
            }
            if (retval != 0) {
                UsbPortManager.logAndPrint(6, this.pw, "port status enquiry failed");
                return;
            }
            ArrayList<RawPortInfo> newPortInfo = new ArrayList();
            Iterator it = currentPortStatus.iterator();
            while (it.hasNext()) {
                PortStatus_1_1 current = (PortStatus_1_1) it.next();
                newPortInfo.add(new RawPortInfo(current.status.portName, current.supportedModes, current.currentMode, current.status.canChangeMode, current.status.currentPowerRole, current.status.canChangePowerRole, current.status.currentDataRole, current.status.canChangeDataRole));
                IndentingPrintWriter indentingPrintWriter = this.pw;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("ClientCallback: ");
                stringBuilder.append(current.status.portName);
                UsbPortManager.logAndPrint(4, indentingPrintWriter, stringBuilder.toString());
            }
            Message message = this.portManager.mHandler.obtainMessage();
            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList(UsbPortManager.PORT_INFO, newPortInfo);
            message.what = 1;
            message.setData(bundle);
            this.portManager.mHandler.sendMessage(message);
        }

        public void notifyRoleSwitchStatus(String portName, PortRole role, int retval) {
            IndentingPrintWriter indentingPrintWriter;
            StringBuilder stringBuilder;
            if (retval == 0) {
                indentingPrintWriter = this.pw;
                stringBuilder = new StringBuilder();
                stringBuilder.append(portName);
                stringBuilder.append(" role switch successful");
                UsbPortManager.logAndPrint(4, indentingPrintWriter, stringBuilder.toString());
                return;
            }
            indentingPrintWriter = this.pw;
            stringBuilder = new StringBuilder();
            stringBuilder.append(portName);
            stringBuilder.append(" role switch failed");
            UsbPortManager.logAndPrint(6, indentingPrintWriter, stringBuilder.toString());
        }
    }

    public UsbPortManager(Context context) {
        this.mContext = context;
        try {
            if (!IServiceManager.getService().registerForNotifications(IUsb.kInterfaceName, BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, new ServiceNotification())) {
                logAndPrint(6, null, "Failed to register service start notification");
            }
            connectToProxy(null);
        } catch (RemoteException e) {
            logAndPrintException(null, "Failed to register service start notification", e);
        }
    }

    public void systemReady() {
        this.mSystemReady = true;
        if (this.mProxy != null) {
            try {
                this.mProxy.queryPortStatus();
            } catch (RemoteException e) {
                logAndPrintException(null, "ServiceStart: Failed to query port status", e);
            }
        }
    }

    public UsbPort[] getPorts() {
        UsbPort[] result;
        synchronized (this.mLock) {
            int count = this.mPorts.size();
            result = new UsbPort[count];
            for (int i = 0; i < count; i++) {
                result[i] = ((PortInfo) this.mPorts.valueAt(i)).mUsbPort;
            }
        }
        return result;
    }

    public UsbPortStatus getPortStatus(String portId) {
        UsbPortStatus usbPortStatus;
        synchronized (this.mLock) {
            PortInfo portInfo = (PortInfo) this.mPorts.get(portId);
            usbPortStatus = portInfo != null ? portInfo.mUsbPortStatus : null;
        }
        return usbPortStatus;
    }

    /* JADX WARNING: Missing block: B:8:0x002f, code:
            return;
     */
    /* JADX WARNING: Missing block: B:20:0x0081, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setPortRoles(String portId, int newPowerRole, int newDataRole, IndentingPrintWriter pw) {
        StringBuilder stringBuilder;
        String str = portId;
        int i = newPowerRole;
        int i2 = newDataRole;
        IndentingPrintWriter indentingPrintWriter = pw;
        synchronized (this.mLock) {
            PortInfo portInfo = (PortInfo) this.mPorts.get(str);
            StringBuilder stringBuilder2;
            if (portInfo == null) {
                if (indentingPrintWriter != null) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("No such USB port: ");
                    stringBuilder2.append(str);
                    indentingPrintWriter.println(stringBuilder2.toString());
                }
            } else if (portInfo.mUsbPortStatus.isRoleCombinationSupported(i, i2)) {
                int currentDataRole = portInfo.mUsbPortStatus.getCurrentDataRole();
                int currentPowerRole = portInfo.mUsbPortStatus.getCurrentPowerRole();
                if (currentDataRole != i2 || currentPowerRole != i) {
                    int newMode;
                    boolean canChangeMode = portInfo.mCanChangeMode;
                    boolean canChangePowerRole = portInfo.mCanChangePowerRole;
                    boolean canChangeDataRole = portInfo.mCanChangeDataRole;
                    int currentMode = portInfo.mUsbPortStatus.getCurrentMode();
                    if ((canChangePowerRole || currentPowerRole == i) && (canChangeDataRole || currentDataRole == i2)) {
                        newMode = currentMode;
                    } else if (canChangeMode && i == 1 && i2 == 1) {
                        newMode = 2;
                    } else if (canChangeMode && i == 2 && i2 == 2) {
                        newMode = 1;
                    } else {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Found mismatch in supported USB role combinations while attempting to change role: ");
                        stringBuilder2.append(portInfo);
                        stringBuilder2.append(", newPowerRole=");
                        stringBuilder2.append(UsbPort.powerRoleToString(newPowerRole));
                        stringBuilder2.append(", newDataRole=");
                        stringBuilder2.append(UsbPort.dataRoleToString(newDataRole));
                        logAndPrint(6, indentingPrintWriter, stringBuilder2.toString());
                        return;
                    }
                    int newMode2 = newMode;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Setting USB port mode and role: portId=");
                    stringBuilder2.append(str);
                    stringBuilder2.append(", currentMode=");
                    stringBuilder2.append(UsbPort.modeToString(currentMode));
                    stringBuilder2.append(", currentPowerRole=");
                    stringBuilder2.append(UsbPort.powerRoleToString(currentPowerRole));
                    stringBuilder2.append(", currentDataRole=");
                    stringBuilder2.append(UsbPort.dataRoleToString(currentDataRole));
                    stringBuilder2.append(", newMode=");
                    int newMode3 = newMode2;
                    stringBuilder2.append(UsbPort.modeToString(newMode3));
                    stringBuilder2.append(", newPowerRole=");
                    stringBuilder2.append(UsbPort.powerRoleToString(newPowerRole));
                    stringBuilder2.append(", newDataRole=");
                    stringBuilder2.append(UsbPort.dataRoleToString(newDataRole));
                    logAndPrint(4, indentingPrintWriter, stringBuilder2.toString());
                    RawPortInfo sim = (RawPortInfo) this.mSimulatedPorts.get(str);
                    if (sim != null) {
                        sim.currentMode = newMode3;
                        sim.currentPowerRole = i;
                        sim.currentDataRole = i2;
                        updatePortsLocked(indentingPrintWriter, null);
                    } else if (this.mProxy != null) {
                        PortRole portInfo2;
                        int i3;
                        boolean z;
                        if (currentMode != newMode3) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Trying to set the USB port mode: portId=");
                            stringBuilder2.append(str);
                            stringBuilder2.append(", newMode=");
                            stringBuilder2.append(UsbPort.modeToString(newMode3));
                            logAndPrint(6, indentingPrintWriter, stringBuilder2.toString());
                            portInfo2 = new PortRole();
                            portInfo2.type = 2;
                            portInfo2.role = newMode3;
                            try {
                                this.mProxy.switchRole(str, portInfo2);
                                i3 = newMode3;
                                z = canChangeMode;
                            } catch (RemoteException e) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Failed to set the USB port mode: portId=");
                                stringBuilder.append(str);
                                stringBuilder.append(", newMode=");
                                stringBuilder.append(UsbPort.modeToString(portInfo2.role));
                                logAndPrintException(indentingPrintWriter, stringBuilder.toString(), e);
                            }
                        } else {
                            i3 = newMode3;
                            z = canChangeMode;
                            if (currentPowerRole != i) {
                                portInfo2 = new PortRole();
                                portInfo2.type = 1;
                                portInfo2.role = i;
                                try {
                                    this.mProxy.switchRole(str, portInfo2);
                                } catch (RemoteException e2) {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Failed to set the USB port power role: portId=");
                                    stringBuilder.append(str);
                                    stringBuilder.append(", newPowerRole=");
                                    stringBuilder.append(UsbPort.powerRoleToString(portInfo2.role));
                                    logAndPrintException(indentingPrintWriter, stringBuilder.toString(), e2);
                                    return;
                                }
                            }
                            if (currentDataRole != i2) {
                                portInfo2 = new PortRole();
                                portInfo2.type = 0;
                                portInfo2.role = i2;
                                try {
                                    this.mProxy.switchRole(str, portInfo2);
                                } catch (RemoteException e22) {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Failed to set the USB port data role: portId=");
                                    stringBuilder.append(str);
                                    stringBuilder.append(", newDataRole=");
                                    stringBuilder.append(UsbPort.dataRoleToString(portInfo2.role));
                                    logAndPrintException(indentingPrintWriter, stringBuilder.toString(), e22);
                                }
                            }
                        }
                    }
                } else if (indentingPrintWriter != null) {
                    indentingPrintWriter.println("No change.");
                }
            } else {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Attempted to set USB port into unsupported role combination: portId=");
                stringBuilder2.append(str);
                stringBuilder2.append(", newPowerRole=");
                stringBuilder2.append(UsbPort.powerRoleToString(newPowerRole));
                stringBuilder2.append(", newDataRole=");
                stringBuilder2.append(UsbPort.dataRoleToString(newDataRole));
                logAndPrint(6, indentingPrintWriter, stringBuilder2.toString());
                return;
            }
        }
    }

    public void addSimulatedPort(String portId, int supportedModes, IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            if (this.mSimulatedPorts.containsKey(portId)) {
                pw.println("Port with same name already exists.  Please remove it first.");
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Adding simulated port: portId=");
            stringBuilder.append(portId);
            stringBuilder.append(", supportedModes=");
            stringBuilder.append(UsbPort.modeToString(supportedModes));
            pw.println(stringBuilder.toString());
            this.mSimulatedPorts.put(portId, new RawPortInfo(portId, supportedModes));
            updatePortsLocked(pw, null);
        }
    }

    public void connectSimulatedPort(String portId, int mode, boolean canChangeMode, int powerRole, boolean canChangePowerRole, int dataRole, boolean canChangeDataRole, IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            RawPortInfo portInfo = (RawPortInfo) this.mSimulatedPorts.get(portId);
            StringBuilder stringBuilder;
            if (portInfo == null) {
                pw.println("Cannot connect simulated port which does not exist.");
            } else if (mode == 0 || powerRole == 0 || dataRole == 0) {
                pw.println("Cannot connect simulated port in null mode, power role, or data role.");
            } else if ((portInfo.supportedModes & mode) == 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Simulated port does not support mode: ");
                stringBuilder.append(UsbPort.modeToString(mode));
                pw.println(stringBuilder.toString());
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Connecting simulated port: portId=");
                stringBuilder.append(portId);
                stringBuilder.append(", mode=");
                stringBuilder.append(UsbPort.modeToString(mode));
                stringBuilder.append(", canChangeMode=");
                stringBuilder.append(canChangeMode);
                stringBuilder.append(", powerRole=");
                stringBuilder.append(UsbPort.powerRoleToString(powerRole));
                stringBuilder.append(", canChangePowerRole=");
                stringBuilder.append(canChangePowerRole);
                stringBuilder.append(", dataRole=");
                stringBuilder.append(UsbPort.dataRoleToString(dataRole));
                stringBuilder.append(", canChangeDataRole=");
                stringBuilder.append(canChangeDataRole);
                pw.println(stringBuilder.toString());
                portInfo.currentMode = mode;
                portInfo.canChangeMode = canChangeMode;
                portInfo.currentPowerRole = powerRole;
                portInfo.canChangePowerRole = canChangePowerRole;
                portInfo.currentDataRole = dataRole;
                portInfo.canChangeDataRole = canChangeDataRole;
                updatePortsLocked(pw, null);
            }
        }
    }

    public void disconnectSimulatedPort(String portId, IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            RawPortInfo portInfo = (RawPortInfo) this.mSimulatedPorts.get(portId);
            if (portInfo == null) {
                pw.println("Cannot disconnect simulated port which does not exist.");
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Disconnecting simulated port: portId=");
            stringBuilder.append(portId);
            pw.println(stringBuilder.toString());
            portInfo.currentMode = 0;
            portInfo.canChangeMode = false;
            portInfo.currentPowerRole = 0;
            portInfo.canChangePowerRole = false;
            portInfo.currentDataRole = 0;
            portInfo.canChangeDataRole = false;
            updatePortsLocked(pw, null);
        }
    }

    public void removeSimulatedPort(String portId, IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            int index = this.mSimulatedPorts.indexOfKey(portId);
            if (index < 0) {
                pw.println("Cannot remove simulated port which does not exist.");
                return;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Disconnecting simulated port: portId=");
            stringBuilder.append(portId);
            pw.println(stringBuilder.toString());
            this.mSimulatedPorts.removeAt(index);
            updatePortsLocked(pw, null);
        }
    }

    public void resetSimulation(IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            pw.println("Removing all simulated ports and ending simulation.");
            if (!this.mSimulatedPorts.isEmpty()) {
                this.mSimulatedPorts.clear();
                updatePortsLocked(pw, null);
            }
        }
    }

    public void dump(DualDumpOutputStream dump, String idName, long id) {
        long token = dump.start(idName, id);
        synchronized (this.mLock) {
            dump.write("is_simulation_active", 1133871366145L, this.mSimulatedPorts.isEmpty() ^ 1);
            for (PortInfo portInfo : this.mPorts.values()) {
                portInfo.dump(dump, "usb_ports", 2246267895810L);
            }
        }
        dump.end(token);
    }

    private void connectToProxy(IndentingPrintWriter pw) {
        synchronized (this.mLock) {
            if (this.mProxy != null) {
                return;
            }
            try {
                this.mProxy = IUsb.getService();
                this.mProxy.linkToDeath(new DeathRecipient(pw), 1000);
                this.mProxy.setCallback(this.mHALCallback);
                this.mProxy.queryPortStatus();
            } catch (NoSuchElementException e) {
                logAndPrintException(pw, "connectToProxy: usb hal service not found. Did the service fail to start?", e);
            } catch (RemoteException e2) {
                logAndPrintException(pw, "connectToProxy: usb hal service not responding", e2);
            }
        }
    }

    private void updatePortsLocked(IndentingPrintWriter pw, ArrayList<RawPortInfo> newPortInfo) {
        int i;
        IndentingPrintWriter indentingPrintWriter = pw;
        int i2 = this.mPorts.size();
        while (true) {
            i = i2 - 1;
            if (i2 <= 0) {
                break;
            }
            ((PortInfo) this.mPorts.valueAt(i)).mDisposition = 3;
            i2 = i;
        }
        if (!this.mSimulatedPorts.isEmpty()) {
            int count = this.mSimulatedPorts.size();
            i2 = 0;
            while (true) {
                int i3 = i2;
                if (i3 >= count) {
                    break;
                }
                RawPortInfo portInfo = (RawPortInfo) this.mSimulatedPorts.valueAt(i3);
                addOrUpdatePortLocked(portInfo.portId, portInfo.supportedModes, portInfo.currentMode, portInfo.canChangeMode, portInfo.currentPowerRole, portInfo.canChangePowerRole, portInfo.currentDataRole, portInfo.canChangeDataRole, indentingPrintWriter);
                i2 = i3 + 1;
            }
        } else {
            Iterator it = newPortInfo.iterator();
            while (it.hasNext()) {
                RawPortInfo currentPortInfo = (RawPortInfo) it.next();
                addOrUpdatePortLocked(currentPortInfo.portId, currentPortInfo.supportedModes, currentPortInfo.currentMode, currentPortInfo.canChangeMode, currentPortInfo.currentPowerRole, currentPortInfo.canChangePowerRole, currentPortInfo.currentDataRole, currentPortInfo.canChangeDataRole, indentingPrintWriter);
            }
        }
        i2 = this.mPorts.size();
        while (true) {
            i = i2 - 1;
            if (i2 > 0) {
                PortInfo portInfo2 = (PortInfo) this.mPorts.valueAt(i);
                int i4 = portInfo2.mDisposition;
                if (i4 != 3) {
                    switch (i4) {
                        case 0:
                            handlePortAddedLocked(portInfo2, indentingPrintWriter);
                            portInfo2.mDisposition = 2;
                            break;
                        case 1:
                            handlePortChangedLocked(portInfo2, indentingPrintWriter);
                            portInfo2.mDisposition = 2;
                            break;
                        default:
                            break;
                    }
                }
                this.mPorts.removeAt(i);
                portInfo2.mUsbPortStatus = null;
                handlePortRemovedLocked(portInfo2, indentingPrintWriter);
                i2 = i;
            } else {
                return;
            }
        }
    }

    private void addOrUpdatePortLocked(String portId, int supportedModes, int currentMode, boolean canChangeMode, int currentPowerRole, boolean canChangePowerRole, int currentDataRole, boolean canChangeDataRole, IndentingPrintWriter pw) {
        int currentMode2;
        boolean canChangeMode2;
        String str = portId;
        int i = supportedModes;
        int i2 = currentMode;
        int i3 = currentPowerRole;
        int i4 = currentDataRole;
        IndentingPrintWriter indentingPrintWriter = pw;
        if ((i & 3) != 3) {
            if (!(i2 == 0 || i2 == i)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Ignoring inconsistent current mode from USB port driver: supportedModes=");
                stringBuilder.append(UsbPort.modeToString(supportedModes));
                stringBuilder.append(", currentMode=");
                stringBuilder.append(UsbPort.modeToString(currentMode));
                logAndPrint(5, indentingPrintWriter, stringBuilder.toString());
                i2 = 0;
            }
            currentMode2 = i2;
            canChangeMode2 = false;
        } else {
            canChangeMode2 = canChangeMode;
            currentMode2 = i2;
        }
        i2 = UsbPort.combineRolesAsBit(i3, i4);
        if (!(currentMode2 == 0 || i3 == 0 || i4 == 0)) {
            if (canChangePowerRole && canChangeDataRole) {
                i2 |= ((COMBO_SOURCE_HOST | COMBO_SOURCE_DEVICE) | COMBO_SINK_HOST) | COMBO_SINK_DEVICE;
            } else if (canChangePowerRole) {
                i2 = (i2 | UsbPort.combineRolesAsBit(1, i4)) | UsbPort.combineRolesAsBit(2, i4);
            } else if (canChangeDataRole) {
                i2 = (i2 | UsbPort.combineRolesAsBit(i3, 1)) | UsbPort.combineRolesAsBit(i3, 2);
            } else if (canChangeMode2) {
                i2 |= COMBO_SOURCE_HOST | COMBO_SINK_DEVICE;
            }
        }
        int supportedRoleCombinations = i2;
        PortInfo portInfo = (PortInfo) this.mPorts.get(str);
        PortInfo portInfo2;
        if (portInfo == null) {
            PortInfo portInfo3 = new PortInfo(str, i);
            portInfo2 = portInfo3;
            portInfo3.setStatus(currentMode2, canChangeMode2, i3, canChangePowerRole, i4, canChangeDataRole, supportedRoleCombinations);
            this.mPorts.put(str, portInfo2);
            return;
        }
        if (i != portInfo.mUsbPort.getSupportedModes()) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Ignoring inconsistent list of supported modes from USB port driver (should be immutable): previous=");
            stringBuilder2.append(UsbPort.modeToString(portInfo.mUsbPort.getSupportedModes()));
            stringBuilder2.append(", current=");
            stringBuilder2.append(UsbPort.modeToString(supportedModes));
            logAndPrint(5, indentingPrintWriter, stringBuilder2.toString());
        }
        portInfo2 = portInfo;
        int i5 = 1;
        if (portInfo.setStatus(currentMode2, canChangeMode2, i3, canChangePowerRole, i4, canChangeDataRole, supportedRoleCombinations)) {
            portInfo2.mDisposition = i5;
        } else {
            portInfo2.mDisposition = 2;
        }
    }

    private void handlePortAddedLocked(PortInfo portInfo, IndentingPrintWriter pw) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("USB port added: ");
        stringBuilder.append(portInfo);
        logAndPrint(4, pw, stringBuilder.toString());
        sendPortChangedBroadcastLocked(portInfo);
    }

    private void handlePortChangedLocked(PortInfo portInfo, IndentingPrintWriter pw) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("USB port changed: ");
        stringBuilder.append(portInfo);
        logAndPrint(4, pw, stringBuilder.toString());
        sendPortChangedBroadcastLocked(portInfo);
    }

    private void handlePortRemovedLocked(PortInfo portInfo, IndentingPrintWriter pw) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("USB port removed: ");
        stringBuilder.append(portInfo);
        logAndPrint(4, pw, stringBuilder.toString());
        sendPortChangedBroadcastLocked(portInfo);
    }

    private void sendPortChangedBroadcastLocked(PortInfo portInfo) {
        Intent intent = new Intent("android.hardware.usb.action.USB_PORT_CHANGED");
        intent.addFlags(285212672);
        intent.putExtra("port", portInfo.mUsbPort);
        intent.putExtra("portStatus", portInfo.mUsbPortStatus);
        this.mHandler.post(new -$$Lambda$UsbPortManager$FUqGOOupcl6RrRkZBk-BnrRQyPI(this, intent));
    }

    private static void logAndPrint(int priority, IndentingPrintWriter pw, String msg) {
        Slog.println(priority, TAG, msg);
        if (pw != null) {
            pw.println(msg);
        }
    }

    private static void logAndPrintException(IndentingPrintWriter pw, String msg, Exception e) {
        Slog.e(TAG, msg, e);
        if (pw != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(msg);
            stringBuilder.append(e);
            pw.println(stringBuilder.toString());
        }
    }
}
