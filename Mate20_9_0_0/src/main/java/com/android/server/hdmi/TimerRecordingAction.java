package com.android.server.hdmi;

import android.util.Slog;
import java.util.Arrays;

public class TimerRecordingAction extends HdmiCecFeatureAction {
    private static final int STATE_WAITING_FOR_TIMER_STATUS = 1;
    private static final String TAG = "TimerRecordingAction";
    private static final int TIMER_STATUS_TIMEOUT_MS = 120000;
    private final byte[] mRecordSource;
    private final int mRecorderAddress;
    private final int mSourceType;

    TimerRecordingAction(HdmiCecLocalDevice source, int recorderAddress, int sourceType, byte[] recordSource) {
        super(source);
        this.mRecorderAddress = recorderAddress;
        this.mSourceType = sourceType;
        this.mRecordSource = recordSource;
    }

    boolean start() {
        sendTimerMessage();
        return true;
    }

    private void sendTimerMessage() {
        HdmiCecMessage message;
        switch (this.mSourceType) {
            case 1:
                message = HdmiCecMessageBuilder.buildSetDigitalTimer(getSourceAddress(), this.mRecorderAddress, this.mRecordSource);
                break;
            case 2:
                message = HdmiCecMessageBuilder.buildSetAnalogueTimer(getSourceAddress(), this.mRecorderAddress, this.mRecordSource);
                break;
            case 3:
                message = HdmiCecMessageBuilder.buildSetExternalTimer(getSourceAddress(), this.mRecorderAddress, this.mRecordSource);
                break;
            default:
                tv().announceTimerRecordingResult(this.mRecorderAddress, 2);
                finish();
                return;
        }
        sendCommand(message, new SendMessageCallback() {
            public void onSendCompleted(int error) {
                if (error != 0) {
                    TimerRecordingAction.this.tv().announceTimerRecordingResult(TimerRecordingAction.this.mRecorderAddress, 1);
                    TimerRecordingAction.this.finish();
                    return;
                }
                TimerRecordingAction.this.mState = 1;
                TimerRecordingAction.this.addTimer(TimerRecordingAction.this.mState, TimerRecordingAction.TIMER_STATUS_TIMEOUT_MS);
            }
        });
    }

    boolean processCommand(HdmiCecMessage cmd) {
        if (this.mState != 1 || cmd.getSource() != this.mRecorderAddress) {
            return false;
        }
        int opcode = cmd.getOpcode();
        if (opcode == 0) {
            return handleFeatureAbort(cmd);
        }
        if (opcode != 53) {
            return false;
        }
        return handleTimerStatus(cmd);
    }

    private boolean handleTimerStatus(HdmiCecMessage cmd) {
        byte[] timerStatusData = cmd.getParams();
        String str;
        StringBuilder stringBuilder;
        if (timerStatusData.length == 1 || timerStatusData.length == 3) {
            tv().announceTimerRecordingResult(this.mRecorderAddress, bytesToInt(timerStatusData));
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Received [Timer Status Data]:");
            stringBuilder.append(Arrays.toString(timerStatusData));
            Slog.i(str, stringBuilder.toString());
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid [Timer Status Data]:");
            stringBuilder.append(Arrays.toString(timerStatusData));
            Slog.w(str, stringBuilder.toString());
        }
        finish();
        return true;
    }

    private boolean handleFeatureAbort(HdmiCecMessage cmd) {
        byte[] params = cmd.getParams();
        int messageType = params[0] & 255;
        if (messageType != 52 && messageType != 151 && messageType != 162) {
            return false;
        }
        int reason = params[1] & 255;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[Feature Abort] for ");
        stringBuilder.append(messageType);
        stringBuilder.append(" reason:");
        stringBuilder.append(reason);
        Slog.i(str, stringBuilder.toString());
        tv().announceTimerRecordingResult(this.mRecorderAddress, 1);
        finish();
        return true;
    }

    private static int bytesToInt(byte[] data) {
        if (data.length <= 4) {
            int result = 0;
            for (int i = 0; i < data.length; i++) {
                result |= (data[i] & 255) << ((3 - i) * 8);
            }
            return result;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Invalid data size:");
        stringBuilder.append(Arrays.toString(data));
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    void handleTimerEvent(int state) {
        if (this.mState != state) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Timeout in invalid state:[Expected:");
            stringBuilder.append(this.mState);
            stringBuilder.append(", Actual:");
            stringBuilder.append(state);
            stringBuilder.append("]");
            Slog.w(str, stringBuilder.toString());
            return;
        }
        tv().announceTimerRecordingResult(this.mRecorderAddress, 1);
        finish();
    }
}
