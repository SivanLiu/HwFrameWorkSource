package com.android.internal.telephony.test;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import com.android.internal.telephony.DriverCall;
import java.util.ArrayList;
import java.util.List;

class SimulatedGsmCallState extends Handler {
    static final int CONNECTING_PAUSE_MSEC = 500;
    static final int EVENT_PROGRESS_CALL_STATE = 1;
    static final int MAX_CALLS = 7;
    private boolean mAutoProgressConnecting = true;
    CallInfo[] mCalls = new CallInfo[7];
    private boolean mNextDialFailImmediately;

    public SimulatedGsmCallState(Looper looper) {
        super(looper);
    }

    public void handleMessage(Message msg) {
        synchronized (this) {
            if (msg.what == 1) {
                progressConnectingCallState();
            }
        }
    }

    /* JADX WARNING: Missing block: B:29:0x0056, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean triggerRing(String number) {
        synchronized (this) {
            boolean isCallWaiting = false;
            int empty = -1;
            for (int i = 0; i < this.mCalls.length; i++) {
                CallInfo call = this.mCalls[i];
                if (call == null && empty < 0) {
                    empty = i;
                } else if (call != null && (call.mState == State.INCOMING || call.mState == State.WAITING)) {
                    Rlog.w("ModelInterpreter", "triggerRing failed; phone already ringing");
                    return false;
                } else if (call != null) {
                    isCallWaiting = true;
                }
            }
            if (empty < 0) {
                Rlog.w("ModelInterpreter", "triggerRing failed; all full");
                return false;
            }
            this.mCalls[empty] = CallInfo.createIncomingCall(PhoneNumberUtils.extractNetworkPortion(number));
            if (isCallWaiting) {
                this.mCalls[empty].mState = State.WAITING;
            }
        }
    }

    public void progressConnectingCallState() {
        synchronized (this) {
            int i = 0;
            while (i < this.mCalls.length) {
                CallInfo call = this.mCalls[i];
                if (call == null || call.mState != State.DIALING) {
                    if (call != null && call.mState == State.ALERTING) {
                        call.mState = State.ACTIVE;
                        break;
                    }
                    i++;
                } else {
                    call.mState = State.ALERTING;
                    if (this.mAutoProgressConnecting) {
                        sendMessageDelayed(obtainMessage(1, call), 500);
                    }
                }
            }
        }
    }

    public void progressConnectingToActive() {
        synchronized (this) {
            for (CallInfo call : this.mCalls) {
                if (call != null && (call.mState == State.DIALING || call.mState == State.ALERTING)) {
                    call.mState = State.ACTIVE;
                    break;
                }
            }
        }
    }

    public void setAutoProgressConnectingCall(boolean b) {
        this.mAutoProgressConnecting = b;
    }

    public void setNextDialFailImmediately(boolean b) {
        this.mNextDialFailImmediately = b;
    }

    public boolean triggerHangupForeground() {
        boolean found;
        synchronized (this) {
            int i;
            int i2 = 0;
            found = false;
            for (i = 0; i < this.mCalls.length; i++) {
                CallInfo call = this.mCalls[i];
                if (call != null && (call.mState == State.INCOMING || call.mState == State.WAITING)) {
                    this.mCalls[i] = null;
                    found = true;
                }
            }
            while (true) {
                i = i2;
                if (i < this.mCalls.length) {
                    CallInfo call2 = this.mCalls[i];
                    if (call2 != null && (call2.mState == State.DIALING || call2.mState == State.ACTIVE || call2.mState == State.ALERTING)) {
                        this.mCalls[i] = null;
                        found = true;
                    }
                    i2 = i + 1;
                }
            }
        }
        return found;
    }

    public boolean triggerHangupBackground() {
        boolean found;
        synchronized (this) {
            found = false;
            for (int i = 0; i < this.mCalls.length; i++) {
                CallInfo call = this.mCalls[i];
                if (call != null && call.mState == State.HOLDING) {
                    this.mCalls[i] = null;
                    found = true;
                }
            }
        }
        return found;
    }

    public boolean triggerHangupAll() {
        boolean found;
        synchronized (this) {
            found = false;
            for (int i = 0; i < this.mCalls.length; i++) {
                CallInfo call = this.mCalls[i];
                if (this.mCalls[i] != null) {
                    found = true;
                }
                this.mCalls[i] = null;
            }
        }
        return found;
    }

    public boolean onAnswer() {
        synchronized (this) {
            int i = 0;
            while (i < this.mCalls.length) {
                CallInfo call = this.mCalls[i];
                if (call == null || !(call.mState == State.INCOMING || call.mState == State.WAITING)) {
                    i++;
                } else {
                    boolean switchActiveAndHeldOrWaiting = switchActiveAndHeldOrWaiting();
                    return switchActiveAndHeldOrWaiting;
                }
            }
            return false;
        }
    }

    public boolean onHangup() {
        boolean found = false;
        for (int i = 0; i < this.mCalls.length; i++) {
            CallInfo call = this.mCalls[i];
            if (!(call == null || call.mState == State.WAITING)) {
                this.mCalls[i] = null;
                found = true;
            }
        }
        return found;
    }

    public boolean onChld(char c0, char c1) {
        int callIndex = 0;
        boolean ret = false;
        if (c1 != 0) {
            callIndex = c1 - 49;
            if (callIndex < 0 || callIndex >= this.mCalls.length) {
                return false;
            }
        }
        switch (c0) {
            case '0':
                ret = releaseHeldOrUDUB();
                break;
            case '1':
                if (c1 > 0) {
                    if (this.mCalls[callIndex] != null) {
                        this.mCalls[callIndex] = null;
                        ret = true;
                        break;
                    }
                    ret = false;
                    break;
                }
                ret = releaseActiveAcceptHeldOrWaiting();
                break;
            case '2':
                if (c1 > 0) {
                    ret = separateCall(callIndex);
                    break;
                }
                ret = switchActiveAndHeldOrWaiting();
                break;
            case '3':
                ret = conference();
                break;
            case '4':
                ret = explicitCallTransfer();
                break;
            case '5':
                ret = false;
                break;
        }
        return ret;
    }

    public boolean releaseHeldOrUDUB() {
        boolean found = false;
        int i = 0;
        for (int i2 = 0; i2 < this.mCalls.length; i2++) {
            CallInfo c = this.mCalls[i2];
            if (c != null && c.isRinging()) {
                found = true;
                this.mCalls[i2] = null;
                break;
            }
        }
        if (!found) {
            while (i < this.mCalls.length) {
                CallInfo c2 = this.mCalls[i];
                if (c2 != null && c2.mState == State.HOLDING) {
                    this.mCalls[i] = null;
                }
                i++;
            }
        }
        return true;
    }

    public boolean releaseActiveAcceptHeldOrWaiting() {
        int i;
        CallInfo c;
        int i2 = 0;
        boolean foundActive = false;
        for (i = 0; i < this.mCalls.length; i++) {
            c = this.mCalls[i];
            if (c != null && c.mState == State.ACTIVE) {
                this.mCalls[i] = null;
                foundActive = true;
            }
        }
        if (!foundActive) {
            for (i = 0; i < this.mCalls.length; i++) {
                c = this.mCalls[i];
                if (c != null && (c.mState == State.DIALING || c.mState == State.ALERTING)) {
                    this.mCalls[i] = null;
                }
            }
        }
        boolean foundHeld = false;
        for (CallInfo c2 : this.mCalls) {
            if (c2 != null && c2.mState == State.HOLDING) {
                c2.mState = State.ACTIVE;
                foundHeld = true;
            }
        }
        if (foundHeld) {
            return true;
        }
        while (i2 < this.mCalls.length) {
            c2 = this.mCalls[i2];
            if (c2 == null || !c2.isRinging()) {
                i2++;
            } else {
                c2.mState = State.ACTIVE;
                return true;
            }
        }
        return true;
    }

    public boolean switchActiveAndHeldOrWaiting() {
        boolean hasHeld = false;
        int i = 0;
        for (CallInfo c : this.mCalls) {
            if (c != null && c.mState == State.HOLDING) {
                hasHeld = true;
                break;
            }
        }
        while (i < this.mCalls.length) {
            CallInfo c2 = this.mCalls[i];
            if (c2 != null) {
                if (c2.mState == State.ACTIVE) {
                    c2.mState = State.HOLDING;
                } else if (c2.mState == State.HOLDING) {
                    c2.mState = State.ACTIVE;
                } else if (!hasHeld && c2.isRinging()) {
                    c2.mState = State.ACTIVE;
                }
            }
            i++;
        }
        return true;
    }

    public boolean separateCall(int index) {
        try {
            CallInfo c = this.mCalls[index];
            if (!(c == null || c.isConnecting())) {
                if (countActiveLines() == 1) {
                    c.mState = State.ACTIVE;
                    c.mIsMpty = false;
                    for (int i = 0; i < this.mCalls.length; i++) {
                        int countHeld = 0;
                        int lastHeld = 0;
                        if (i != index) {
                            CallInfo cb = this.mCalls[i];
                            if (cb != null && cb.mState == State.ACTIVE) {
                                cb.mState = State.HOLDING;
                                countHeld = 0 + 1;
                                lastHeld = i;
                            }
                        }
                        if (countHeld == 1) {
                            this.mCalls[lastHeld].mIsMpty = false;
                        }
                    }
                    return true;
                }
            }
            return false;
        } catch (InvalidStateEx e) {
            return false;
        }
    }

    public boolean conference() {
        int i;
        int i2 = 0;
        int countCalls = 0;
        for (CallInfo c : this.mCalls) {
            if (c != null) {
                countCalls++;
                if (c.isConnecting()) {
                    return false;
                }
            }
        }
        while (true) {
            i = i2;
            if (i >= this.mCalls.length) {
                return true;
            }
            CallInfo c2 = this.mCalls[i];
            if (c2 != null) {
                c2.mState = State.ACTIVE;
                if (countCalls > 0) {
                    c2.mIsMpty = true;
                }
            }
            i2 = i + 1;
        }
    }

    public boolean explicitCallTransfer() {
        int countCalls = 0;
        for (CallInfo c : this.mCalls) {
            if (c != null) {
                countCalls++;
                if (c.isConnecting()) {
                    return false;
                }
            }
        }
        return triggerHangupAll();
    }

    public boolean onDial(String address) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SC> dial '");
        stringBuilder.append(address);
        stringBuilder.append("'");
        Rlog.d("GSM", stringBuilder.toString());
        if (this.mNextDialFailImmediately) {
            this.mNextDialFailImmediately = false;
            Rlog.d("GSM", "SC< dial fail (per request)");
            return false;
        }
        String phNum = PhoneNumberUtils.extractNetworkPortion(address);
        if (phNum.length() == 0) {
            Rlog.d("GSM", "SC< dial fail (invalid ph num)");
            return false;
        } else if (phNum.startsWith("*99") && phNum.endsWith("#")) {
            Rlog.d("GSM", "SC< dial ignored (gprs)");
            return true;
        } else {
            try {
                if (countActiveLines() > 1) {
                    Rlog.d("GSM", "SC< dial fail (invalid call state)");
                    return false;
                }
                int freeSlot = -1;
                int i = 0;
                while (i < this.mCalls.length) {
                    if (freeSlot < 0 && this.mCalls[i] == null) {
                        freeSlot = i;
                    }
                    if (this.mCalls[i] == null || this.mCalls[i].isActiveOrHeld()) {
                        if (this.mCalls[i] != null && this.mCalls[i].mState == State.ACTIVE) {
                            this.mCalls[i].mState = State.HOLDING;
                        }
                        i++;
                    } else {
                        Rlog.d("GSM", "SC< dial fail (invalid call state)");
                        return false;
                    }
                }
                if (freeSlot < 0) {
                    Rlog.d("GSM", "SC< dial fail (invalid call state)");
                    return false;
                }
                this.mCalls[freeSlot] = CallInfo.createOutgoingCall(phNum);
                if (this.mAutoProgressConnecting) {
                    sendMessageDelayed(obtainMessage(1, this.mCalls[freeSlot]), 500);
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("SC< dial (slot = ");
                stringBuilder.append(freeSlot);
                stringBuilder.append(")");
                Rlog.d("GSM", stringBuilder.toString());
                return true;
            } catch (InvalidStateEx e) {
                Rlog.d("GSM", "SC< dial fail (invalid call state)");
                return false;
            }
        }
    }

    public List<DriverCall> getDriverCalls() {
        ArrayList<DriverCall> ret = new ArrayList(this.mCalls.length);
        for (int i = 0; i < this.mCalls.length; i++) {
            CallInfo c = this.mCalls[i];
            if (c != null) {
                ret.add(c.toDriverCall(i + 1));
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("SC< getDriverCalls ");
        stringBuilder.append(ret);
        Rlog.d("GSM", stringBuilder.toString());
        return ret;
    }

    public List<String> getClccLines() {
        ArrayList<String> ret = new ArrayList(this.mCalls.length);
        for (int i = 0; i < this.mCalls.length; i++) {
            CallInfo c = this.mCalls[i];
            if (c != null) {
                ret.add(c.toCLCCLine(i + 1));
            }
        }
        return ret;
    }

    private int countActiveLines() throws InvalidStateEx {
        boolean hasHeld = false;
        boolean mptyIsHeld = false;
        boolean hasRinging = false;
        boolean hasConnecting = false;
        boolean hasActive = false;
        boolean hasMpty = false;
        for (CallInfo call : this.mCalls) {
            if (call != null) {
                int i = 1;
                if (!hasMpty && call.mIsMpty) {
                    mptyIsHeld = call.mState == State.HOLDING;
                } else if (call.mIsMpty && mptyIsHeld && call.mState == State.ACTIVE) {
                    Rlog.e("ModelInterpreter", "Invalid state");
                    throw new InvalidStateEx();
                } else if (!call.mIsMpty && hasMpty && mptyIsHeld && call.mState == State.HOLDING) {
                    Rlog.e("ModelInterpreter", "Invalid state");
                    throw new InvalidStateEx();
                }
                hasMpty |= call.mIsMpty;
                hasHeld |= call.mState == State.HOLDING ? 1 : 0;
                if (call.mState != State.ACTIVE) {
                    i = 0;
                }
                hasActive |= i;
                hasConnecting |= call.isConnecting();
                hasRinging |= call.isRinging();
            }
        }
        int i2 = 0;
        if (hasHeld) {
            i2 = 0 + 1;
        }
        if (hasActive) {
            i2++;
        }
        if (hasConnecting) {
            i2++;
        }
        if (hasRinging) {
            return i2 + 1;
        }
        return i2;
    }
}
