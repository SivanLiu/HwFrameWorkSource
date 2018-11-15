package com.android.server.hdmi;

import android.hardware.hdmi.HdmiDeviceInfo;
import java.util.ArrayList;
import java.util.Iterator;

final class DelayedMessageBuffer {
    private final ArrayList<HdmiCecMessage> mBuffer = new ArrayList();
    private final HdmiCecLocalDevice mDevice;

    DelayedMessageBuffer(HdmiCecLocalDevice device) {
        this.mDevice = device;
    }

    /* JADX WARNING: Removed duplicated region for block: B:12:? A:{SYNTHETIC, RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:10:0x0024  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void add(HdmiCecMessage message) {
        boolean buffered = true;
        int opcode = message.getOpcode();
        if (opcode != 114) {
            if (opcode == 130) {
                removeActiveSource();
                this.mBuffer.add(message);
            } else if (opcode != 192) {
                buffered = false;
            }
            if (!buffered) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Buffering message:");
                stringBuilder.append(message);
                HdmiLogger.debug(stringBuilder.toString(), new Object[0]);
                return;
            }
            return;
        }
        this.mBuffer.add(message);
        if (!buffered) {
        }
    }

    private void removeActiveSource() {
        Iterator<HdmiCecMessage> iter = this.mBuffer.iterator();
        while (iter.hasNext()) {
            if (((HdmiCecMessage) iter.next()).getOpcode() == 130) {
                iter.remove();
            }
        }
    }

    boolean isBuffered(int opcode) {
        Iterator it = this.mBuffer.iterator();
        while (it.hasNext()) {
            if (((HdmiCecMessage) it.next()).getOpcode() == opcode) {
                return true;
            }
        }
        return false;
    }

    void processAllMessages() {
        ArrayList<HdmiCecMessage> copiedBuffer = new ArrayList(this.mBuffer);
        this.mBuffer.clear();
        Iterator it = copiedBuffer.iterator();
        while (it.hasNext()) {
            HdmiCecMessage message = (HdmiCecMessage) it.next();
            this.mDevice.onMessage(message);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Processing message:");
            stringBuilder.append(message);
            HdmiLogger.debug(stringBuilder.toString(), new Object[0]);
        }
    }

    void processMessagesForDevice(int address) {
        ArrayList<HdmiCecMessage> copiedBuffer = new ArrayList(this.mBuffer);
        this.mBuffer.clear();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Checking message for address:");
        stringBuilder.append(address);
        HdmiLogger.debug(stringBuilder.toString(), new Object[0]);
        Iterator it = copiedBuffer.iterator();
        while (it.hasNext()) {
            HdmiCecMessage message = (HdmiCecMessage) it.next();
            if (message.getSource() != address) {
                this.mBuffer.add(message);
            } else if (message.getOpcode() != 130 || this.mDevice.isInputReady(HdmiDeviceInfo.idForCecDevice(address))) {
                this.mDevice.onMessage(message);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Processing message:");
                stringBuilder2.append(message);
                HdmiLogger.debug(stringBuilder2.toString(), new Object[0]);
            } else {
                this.mBuffer.add(message);
            }
        }
    }

    void processActiveSource(int address) {
        ArrayList<HdmiCecMessage> copiedBuffer = new ArrayList(this.mBuffer);
        this.mBuffer.clear();
        Iterator it = copiedBuffer.iterator();
        while (it.hasNext()) {
            HdmiCecMessage message = (HdmiCecMessage) it.next();
            if (message.getOpcode() == 130 && message.getSource() == address) {
                this.mDevice.onMessage(message);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Processing message:");
                stringBuilder.append(message);
                HdmiLogger.debug(stringBuilder.toString(), new Object[0]);
            } else {
                this.mBuffer.add(message);
            }
        }
    }
}
