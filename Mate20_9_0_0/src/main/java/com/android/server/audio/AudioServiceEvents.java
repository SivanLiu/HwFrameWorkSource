package com.android.server.audio;

import android.media.AudioManager;
import android.media.AudioSystem;
import com.android.server.audio.AudioEventLogger.Event;

public class AudioServiceEvents {

    static final class ForceUseEvent extends Event {
        final int mConfig;
        final String mReason;
        final int mUsage;

        ForceUseEvent(int usage, int config, String reason) {
            this.mUsage = usage;
            this.mConfig = config;
            this.mReason = reason;
        }

        public String eventToString() {
            StringBuilder stringBuilder = new StringBuilder("setForceUse(");
            stringBuilder.append(AudioSystem.forceUseUsageToString(this.mUsage));
            stringBuilder.append(", ");
            stringBuilder.append(AudioSystem.forceUseConfigToString(this.mConfig));
            stringBuilder.append(") due to ");
            stringBuilder.append(this.mReason);
            return stringBuilder.toString();
        }
    }

    static final class PhoneStateEvent extends Event {
        final int mActualMode;
        final int mOwnerPid;
        final String mPackage;
        final int mRequestedMode;
        final int mRequesterPid;

        PhoneStateEvent(String callingPackage, int requesterPid, int requestedMode, int ownerPid, int actualMode) {
            this.mPackage = callingPackage;
            this.mRequesterPid = requesterPid;
            this.mRequestedMode = requestedMode;
            this.mOwnerPid = ownerPid;
            this.mActualMode = actualMode;
        }

        public String eventToString() {
            StringBuilder stringBuilder = new StringBuilder("setMode(");
            stringBuilder.append(AudioSystem.modeToString(this.mRequestedMode));
            stringBuilder.append(") from package=");
            stringBuilder.append(this.mPackage);
            stringBuilder.append(" pid=");
            stringBuilder.append(this.mRequesterPid);
            stringBuilder.append(" selected mode=");
            stringBuilder.append(AudioSystem.modeToString(this.mActualMode));
            stringBuilder.append(" by pid=");
            stringBuilder.append(this.mOwnerPid);
            return stringBuilder.toString();
        }
    }

    static final class VolumeEvent extends Event {
        static final int VOL_ADJUST_STREAM_VOL = 1;
        static final int VOL_ADJUST_SUGG_VOL = 0;
        static final int VOL_SET_STREAM_VOL = 2;
        final String mCaller;
        final int mOp;
        final int mStream;
        final int mVal1;
        final int mVal2;

        VolumeEvent(int op, int stream, int val1, int val2, String caller) {
            this.mOp = op;
            this.mStream = stream;
            this.mVal1 = val1;
            this.mVal2 = val2;
            this.mCaller = caller;
        }

        public String eventToString() {
            StringBuilder stringBuilder;
            switch (this.mOp) {
                case 0:
                    stringBuilder = new StringBuilder("adjustSuggestedStreamVolume(sugg:");
                    stringBuilder.append(AudioSystem.streamToString(this.mStream));
                    stringBuilder.append(" dir:");
                    stringBuilder.append(AudioManager.adjustToString(this.mVal1));
                    stringBuilder.append(" flags:0x");
                    stringBuilder.append(Integer.toHexString(this.mVal2));
                    stringBuilder.append(") from ");
                    stringBuilder.append(this.mCaller);
                    return stringBuilder.toString();
                case 1:
                    stringBuilder = new StringBuilder("adjustStreamVolume(stream:");
                    stringBuilder.append(AudioSystem.streamToString(this.mStream));
                    stringBuilder.append(" dir:");
                    stringBuilder.append(AudioManager.adjustToString(this.mVal1));
                    stringBuilder.append(" flags:0x");
                    stringBuilder.append(Integer.toHexString(this.mVal2));
                    stringBuilder.append(") from ");
                    stringBuilder.append(this.mCaller);
                    return stringBuilder.toString();
                case 2:
                    stringBuilder = new StringBuilder("setStreamVolume(stream:");
                    stringBuilder.append(AudioSystem.streamToString(this.mStream));
                    stringBuilder.append(" index:");
                    stringBuilder.append(this.mVal1);
                    stringBuilder.append(" flags:0x");
                    stringBuilder.append(Integer.toHexString(this.mVal2));
                    stringBuilder.append(") from ");
                    stringBuilder.append(this.mCaller);
                    return stringBuilder.toString();
                default:
                    stringBuilder = new StringBuilder("FIXME invalid op:");
                    stringBuilder.append(this.mOp);
                    return stringBuilder.toString();
            }
        }
    }

    static final class WiredDevConnectEvent extends Event {
        final WiredDeviceConnectionState mState;

        WiredDevConnectEvent(WiredDeviceConnectionState state) {
            this.mState = state;
        }

        public String eventToString() {
            StringBuilder stringBuilder = new StringBuilder("setWiredDeviceConnectionState(");
            stringBuilder.append(" type:");
            stringBuilder.append(Integer.toHexString(this.mState.mType));
            stringBuilder.append(" state:");
            stringBuilder.append(AudioSystem.deviceStateToString(this.mState.mState));
            stringBuilder.append(" addr:");
            stringBuilder.append(this.mState.mAddress);
            stringBuilder.append(" name:");
            stringBuilder.append(this.mState.mName);
            stringBuilder.append(") from ");
            stringBuilder.append(this.mState.mCaller);
            return stringBuilder.toString();
        }
    }
}
