package com.android.server.hdmi;

import android.hardware.hdmi.HdmiDeviceInfo;
import android.net.util.NetworkConstants;
import android.util.Slog;
import com.android.internal.util.Preconditions;
import com.android.server.power.IHwShutdownThread;
import com.android.server.usb.UsbAudioDevice;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class DeviceDiscoveryAction extends HdmiCecFeatureAction {
    private static final int STATE_WAITING_FOR_DEVICE_POLLING = 1;
    private static final int STATE_WAITING_FOR_OSD_NAME = 3;
    private static final int STATE_WAITING_FOR_PHYSICAL_ADDRESS = 2;
    private static final int STATE_WAITING_FOR_VENDOR_ID = 4;
    private static final String TAG = "DeviceDiscoveryAction";
    private final DeviceDiscoveryCallback mCallback;
    private final ArrayList<DeviceInfo> mDevices = new ArrayList();
    private int mProcessedDeviceCount = 0;
    private int mTimeoutRetry = 0;

    interface DeviceDiscoveryCallback {
        void onDeviceDiscoveryDone(List<HdmiDeviceInfo> list);
    }

    private static final class DeviceInfo {
        private int mDeviceType;
        private String mDisplayName;
        private final int mLogicalAddress;
        private int mPhysicalAddress;
        private int mPortId;
        private int mVendorId;

        /* synthetic */ DeviceInfo(int x0, AnonymousClass1 x1) {
            this(x0);
        }

        private DeviceInfo(int logicalAddress) {
            this.mPhysicalAddress = NetworkConstants.ARP_HWTYPE_RESERVED_HI;
            this.mPortId = -1;
            this.mVendorId = UsbAudioDevice.kAudioDeviceClassMask;
            this.mDisplayName = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            this.mDeviceType = -1;
            this.mLogicalAddress = logicalAddress;
        }

        private HdmiDeviceInfo toHdmiDeviceInfo() {
            return new HdmiDeviceInfo(this.mLogicalAddress, this.mPhysicalAddress, this.mPortId, this.mDeviceType, this.mVendorId, this.mDisplayName);
        }
    }

    DeviceDiscoveryAction(HdmiCecLocalDevice source, DeviceDiscoveryCallback callback) {
        super(source);
        this.mCallback = (DeviceDiscoveryCallback) Preconditions.checkNotNull(callback);
    }

    boolean start() {
        this.mDevices.clear();
        this.mState = 1;
        pollDevices(new DevicePollingCallback() {
            public void onPollingFinished(List<Integer> ackedAddress) {
                if (ackedAddress.isEmpty()) {
                    Slog.v(DeviceDiscoveryAction.TAG, "No device is detected.");
                    DeviceDiscoveryAction.this.wrapUpAndFinish();
                    return;
                }
                String str = DeviceDiscoveryAction.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Device detected: ");
                stringBuilder.append(ackedAddress);
                Slog.v(str, stringBuilder.toString());
                DeviceDiscoveryAction.this.allocateDevices(ackedAddress);
                DeviceDiscoveryAction.this.startPhysicalAddressStage();
            }
        }, 131073, 1);
        return true;
    }

    private void allocateDevices(List<Integer> addresses) {
        for (Integer i : addresses) {
            this.mDevices.add(new DeviceInfo(i.intValue(), null));
        }
    }

    private void startPhysicalAddressStage() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Start [Physical Address Stage]:");
        stringBuilder.append(this.mDevices.size());
        Slog.v(str, stringBuilder.toString());
        this.mProcessedDeviceCount = 0;
        this.mState = 2;
        checkAndProceedStage();
    }

    private boolean verifyValidLogicalAddress(int address) {
        return address >= 0 && address < 15;
    }

    private void queryPhysicalAddress(int address) {
        if (verifyValidLogicalAddress(address)) {
            this.mActionTimer.clearTimerMessage();
            if (!mayProcessMessageIfCached(address, 132)) {
                sendCommand(HdmiCecMessageBuilder.buildGivePhysicalAddress(getSourceAddress(), address));
                addTimer(this.mState, IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME);
                return;
            }
            return;
        }
        checkAndProceedStage();
    }

    private void startOsdNameStage() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Start [Osd Name Stage]:");
        stringBuilder.append(this.mDevices.size());
        Slog.v(str, stringBuilder.toString());
        this.mProcessedDeviceCount = 0;
        this.mState = 3;
        checkAndProceedStage();
    }

    private void queryOsdName(int address) {
        if (verifyValidLogicalAddress(address)) {
            this.mActionTimer.clearTimerMessage();
            if (!mayProcessMessageIfCached(address, 71)) {
                sendCommand(HdmiCecMessageBuilder.buildGiveOsdNameCommand(getSourceAddress(), address));
                addTimer(this.mState, IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME);
                return;
            }
            return;
        }
        checkAndProceedStage();
    }

    private void startVendorIdStage() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Start [Vendor Id Stage]:");
        stringBuilder.append(this.mDevices.size());
        Slog.v(str, stringBuilder.toString());
        this.mProcessedDeviceCount = 0;
        this.mState = 4;
        checkAndProceedStage();
    }

    private void queryVendorId(int address) {
        if (verifyValidLogicalAddress(address)) {
            this.mActionTimer.clearTimerMessage();
            if (!mayProcessMessageIfCached(address, NetworkConstants.ICMPV6_NEIGHBOR_SOLICITATION)) {
                sendCommand(HdmiCecMessageBuilder.buildGiveDeviceVendorIdCommand(getSourceAddress(), address));
                addTimer(this.mState, IHwShutdownThread.SHUTDOWN_ANIMATION_WAIT_TIME);
                return;
            }
            return;
        }
        checkAndProceedStage();
    }

    private boolean mayProcessMessageIfCached(int address, int opcode) {
        HdmiCecMessage message = getCecMessageCache().getMessage(address, opcode);
        if (message == null) {
            return false;
        }
        processCommand(message);
        return true;
    }

    boolean processCommand(HdmiCecMessage cmd) {
        switch (this.mState) {
            case 2:
                if (cmd.getOpcode() != 132) {
                    return false;
                }
                handleReportPhysicalAddress(cmd);
                return true;
            case 3:
                if (cmd.getOpcode() == 71) {
                    handleSetOsdName(cmd);
                    return true;
                } else if (cmd.getOpcode() != 0 || (cmd.getParams()[0] & 255) != 70) {
                    return false;
                } else {
                    handleSetOsdName(cmd);
                    return true;
                }
            case 4:
                if (cmd.getOpcode() == NetworkConstants.ICMPV6_NEIGHBOR_SOLICITATION) {
                    handleVendorId(cmd);
                    return true;
                } else if (cmd.getOpcode() != 0 || (cmd.getParams()[0] & 255) != 140) {
                    return false;
                } else {
                    handleVendorId(cmd);
                    return true;
                }
            default:
                return false;
        }
    }

    private void handleReportPhysicalAddress(HdmiCecMessage cmd) {
        Preconditions.checkState(this.mProcessedDeviceCount < this.mDevices.size());
        DeviceInfo current = (DeviceInfo) this.mDevices.get(this.mProcessedDeviceCount);
        if (current.mLogicalAddress != cmd.getSource()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unmatched address[expected:");
            stringBuilder.append(current.mLogicalAddress);
            stringBuilder.append(", actual:");
            stringBuilder.append(cmd.getSource());
            Slog.w(str, stringBuilder.toString());
            return;
        }
        byte[] params = cmd.getParams();
        current.mPhysicalAddress = HdmiUtils.twoBytesToInt(params);
        current.mPortId = getPortId(current.mPhysicalAddress);
        current.mDeviceType = params[2] & 255;
        tv().updateCecSwitchInfo(current.mLogicalAddress, current.mDeviceType, current.mPhysicalAddress);
        increaseProcessedDeviceCount();
        checkAndProceedStage();
    }

    private int getPortId(int physicalAddress) {
        return tv().getPortId(physicalAddress);
    }

    private void handleSetOsdName(HdmiCecMessage cmd) {
        Preconditions.checkState(this.mProcessedDeviceCount < this.mDevices.size());
        DeviceInfo current = (DeviceInfo) this.mDevices.get(this.mProcessedDeviceCount);
        String str;
        if (current.mLogicalAddress != cmd.getSource()) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unmatched address[expected:");
            stringBuilder.append(current.mLogicalAddress);
            stringBuilder.append(", actual:");
            stringBuilder.append(cmd.getSource());
            Slog.w(str, stringBuilder.toString());
            return;
        }
        try {
            if (cmd.getOpcode() == 0) {
                str = HdmiUtils.getDefaultDeviceName(current.mLogicalAddress);
            } else {
                str = new String(cmd.getParams(), "US-ASCII");
            }
        } catch (UnsupportedEncodingException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Failed to decode display name: ");
            stringBuilder2.append(cmd.toString());
            Slog.w(str2, stringBuilder2.toString());
            str = HdmiUtils.getDefaultDeviceName(current.mLogicalAddress);
        }
        current.mDisplayName = str;
        increaseProcessedDeviceCount();
        checkAndProceedStage();
    }

    private void handleVendorId(HdmiCecMessage cmd) {
        Preconditions.checkState(this.mProcessedDeviceCount < this.mDevices.size());
        DeviceInfo current = (DeviceInfo) this.mDevices.get(this.mProcessedDeviceCount);
        if (current.mLogicalAddress != cmd.getSource()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unmatched address[expected:");
            stringBuilder.append(current.mLogicalAddress);
            stringBuilder.append(", actual:");
            stringBuilder.append(cmd.getSource());
            Slog.w(str, stringBuilder.toString());
            return;
        }
        if (cmd.getOpcode() != 0) {
            current.mVendorId = HdmiUtils.threeBytesToInt(cmd.getParams());
        }
        increaseProcessedDeviceCount();
        checkAndProceedStage();
    }

    private void increaseProcessedDeviceCount() {
        this.mProcessedDeviceCount++;
        this.mTimeoutRetry = 0;
    }

    private void removeDevice(int index) {
        this.mDevices.remove(index);
    }

    private void wrapUpAndFinish() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("---------Wrap up Device Discovery:[");
        stringBuilder.append(this.mDevices.size());
        stringBuilder.append("]---------");
        Slog.v(str, stringBuilder.toString());
        ArrayList<HdmiDeviceInfo> result = new ArrayList();
        Iterator it = this.mDevices.iterator();
        while (it.hasNext()) {
            HdmiDeviceInfo cecDeviceInfo = ((DeviceInfo) it.next()).toHdmiDeviceInfo();
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" DeviceInfo: ");
            stringBuilder2.append(cecDeviceInfo);
            Slog.v(str2, stringBuilder2.toString());
            result.add(cecDeviceInfo);
        }
        Slog.v(TAG, "--------------------------------------------");
        this.mCallback.onDeviceDiscoveryDone(result);
        finish();
        tv().processAllDelayedMessages();
    }

    private void checkAndProceedStage() {
        if (this.mDevices.isEmpty()) {
            wrapUpAndFinish();
        } else if (this.mProcessedDeviceCount == this.mDevices.size()) {
            this.mProcessedDeviceCount = 0;
            switch (this.mState) {
                case 2:
                    startOsdNameStage();
                    return;
                case 3:
                    startVendorIdStage();
                    return;
                case 4:
                    wrapUpAndFinish();
                    return;
                default:
                    return;
            }
        } else {
            sendQueryCommand();
        }
    }

    private void sendQueryCommand() {
        int address = ((DeviceInfo) this.mDevices.get(this.mProcessedDeviceCount)).mLogicalAddress;
        switch (this.mState) {
            case 2:
                queryPhysicalAddress(address);
                return;
            case 3:
                queryOsdName(address);
                return;
            case 4:
                queryVendorId(address);
                break;
        }
    }

    void handleTimerEvent(int state) {
        if (this.mState != 0 && this.mState == state) {
            int i = this.mTimeoutRetry + 1;
            this.mTimeoutRetry = i;
            if (i < 5) {
                sendQueryCommand();
                return;
            }
            this.mTimeoutRetry = 0;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Timeout[State=");
            stringBuilder.append(this.mState);
            stringBuilder.append(", Processed=");
            stringBuilder.append(this.mProcessedDeviceCount);
            Slog.v(str, stringBuilder.toString());
            removeDevice(this.mProcessedDeviceCount);
            checkAndProceedStage();
        }
    }
}
