package com.android.internal.telephony;

import com.android.internal.telephony.PhoneConstants.DataState;
import com.android.internal.telephony.PhoneConstants.State;

public class PhoneConstantConversions {
    public static int convertCallState(State state) {
        switch (state) {
            case RINGING:
                return 1;
            case OFFHOOK:
                return 2;
            default:
                return 0;
        }
    }

    public static State convertCallState(int state) {
        switch (state) {
            case 1:
                return State.RINGING;
            case 2:
                return State.OFFHOOK;
            default:
                return State.IDLE;
        }
    }

    public static int convertDataState(DataState state) {
        switch (state) {
            case CONNECTING:
                return 1;
            case CONNECTED:
                return 2;
            case SUSPENDED:
                return 3;
            default:
                return 0;
        }
    }

    public static DataState convertDataState(int state) {
        switch (state) {
            case 1:
                return DataState.CONNECTING;
            case 2:
                return DataState.CONNECTED;
            case 3:
                return DataState.SUSPENDED;
            default:
                return DataState.DISCONNECTED;
        }
    }
}
