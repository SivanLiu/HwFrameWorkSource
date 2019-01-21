package com.android.internal.telephony.dataconnection;

import android.os.AsyncResult;
import android.os.Message;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.dataconnection.HwVSimDcSwitchAsyncChannel.RequestInfo;
import com.android.internal.telephony.vsim.HwVSimLog;
import com.android.internal.util.AsyncChannel;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

public class HwVSimDcSwitchStateMachine extends StateMachine {
    private static final int BASE = 274432;
    private static final int CMD_RETRY_ATTACH = 274434;
    private static final int EVENT_CONNECTED = 274432;
    private static final int EVENT_DATA_ALLOWED = 274433;
    private static final int EVENT_DATA_DISALLOWED = 274435;
    private static final String LOG_TAG = "VSimDcSwitchSM";
    private AsyncChannel mAc;
    private AttachedState mAttachedState = new AttachedState();
    private AttachingState mAttachingState = new AttachingState();
    private DefaultState mDefaultState = new DefaultState();
    private DetachingState mDetachingState = new DetachingState();
    private int mId;
    private IdleState mIdleState = new IdleState();
    private Phone mPhone;
    private HwVSimDctController mVSimDctController;

    private class AttachedState extends State {
        private AttachedState() {
        }

        public void enter() {
            HwVSimDcSwitchStateMachine.this.logd("AttachedState: enter");
            HwVSimDcSwitchStateMachine.this.mVSimDctController.executeAllRequests(HwVSimDcSwitchStateMachine.this.mId);
        }

        public boolean processMessage(Message msg) {
            int i = msg.what;
            if (i == 278528) {
                RequestInfo apnRequest = msg.obj;
                apnRequest.log("DcSwitchStateMachine.AttachedState: REQ_CONNECT");
                HwVSimDcSwitchStateMachine hwVSimDcSwitchStateMachine = HwVSimDcSwitchStateMachine.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("AttachedState: REQ_CONNECT, apnRequest=");
                stringBuilder.append(apnRequest);
                hwVSimDcSwitchStateMachine.logd(stringBuilder.toString());
                HwVSimDcSwitchStateMachine.this.mVSimDctController.executeRequest(apnRequest);
                return true;
            } else if (i == 278530) {
                HwVSimDcSwitchStateMachine.this.logd("AttachedState: REQ_DISCONNECT_ALL");
                HwVSimDcSwitchStateMachine.this.mVSimDctController.releaseAllRequests(HwVSimDcSwitchStateMachine.this.mId);
                HwVSimDcSwitchStateMachine.this.transitionTo(HwVSimDcSwitchStateMachine.this.mDetachingState);
                return true;
            } else if (i != 278536) {
                HwVSimDcSwitchStateMachine hwVSimDcSwitchStateMachine2 = HwVSimDcSwitchStateMachine.this;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("AttachedState: nothandled msg.what=0x");
                stringBuilder2.append(Integer.toHexString(msg.what));
                hwVSimDcSwitchStateMachine2.logv(stringBuilder2.toString());
                return false;
            } else {
                HwVSimDcSwitchStateMachine.this.logd("AttachedState: EVENT_DATA_DETACHED");
                HwVSimDcSwitchStateMachine.this.transitionTo(HwVSimDcSwitchStateMachine.this.mAttachingState);
                return true;
            }
        }
    }

    private class AttachingState extends State {
        private int mCurrentAllowedSequence;

        private AttachingState() {
            this.mCurrentAllowedSequence = 0;
        }

        public void enter() {
            HwVSimDcSwitchStateMachine.this.log("AttachingState: enter");
            doEnter();
        }

        private void doEnter() {
            CommandsInterface commandsInterface = HwVSimDcSwitchStateMachine.this.mPhone.mCi;
            HwVSimDcSwitchStateMachine hwVSimDcSwitchStateMachine = HwVSimDcSwitchStateMachine.this;
            int i = this.mCurrentAllowedSequence + 1;
            this.mCurrentAllowedSequence = i;
            commandsInterface.setDataAllowed(true, hwVSimDcSwitchStateMachine.obtainMessage(HwVSimDcSwitchStateMachine.EVENT_DATA_ALLOWED, i, 0));
        }

        public boolean processMessage(Message msg) {
            boolean retVal;
            int i = msg.what;
            HwVSimDcSwitchStateMachine hwVSimDcSwitchStateMachine;
            StringBuilder stringBuilder;
            if (i == HwVSimDcSwitchStateMachine.EVENT_DATA_ALLOWED) {
                AsyncResult retVal2 = (AsyncResult) msg.obj;
                if (this.mCurrentAllowedSequence != msg.arg1) {
                    hwVSimDcSwitchStateMachine = HwVSimDcSwitchStateMachine.this;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("EVENT_DATA_ALLOWED ignored arg1=");
                    stringBuilder.append(msg.arg1);
                    stringBuilder.append(", seq=");
                    stringBuilder.append(this.mCurrentAllowedSequence);
                    hwVSimDcSwitchStateMachine.loge(stringBuilder.toString());
                } else if (retVal2.exception != null) {
                    if (retVal2.exception instanceof CommandException) {
                        retVal = true;
                    } else {
                        hwVSimDcSwitchStateMachine = HwVSimDcSwitchStateMachine.this;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("EVENT_DATA_ALLOWED failed, ");
                        stringBuilder.append(retVal2.exception);
                        hwVSimDcSwitchStateMachine.loge(stringBuilder.toString());
                        HwVSimDcSwitchStateMachine.this.transitionTo(HwVSimDcSwitchStateMachine.this.mIdleState);
                    }
                }
                HwVSimDcSwitchStateMachine.this.transitionTo(HwVSimDcSwitchStateMachine.this.mAttachedState);
                retVal = true;
            } else if (i != 278535) {
                switch (i) {
                    case 278528:
                        RequestInfo apnRequest = msg.obj;
                        apnRequest.log("DcSwitchStateMachine.AttachingState: REQ_CONNECT");
                        hwVSimDcSwitchStateMachine = HwVSimDcSwitchStateMachine.this;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("AttachingState: REQ_CONNECT, apnRequest=");
                        stringBuilder.append(apnRequest);
                        hwVSimDcSwitchStateMachine.logd(stringBuilder.toString());
                        retVal = true;
                        break;
                    case 278529:
                        HwVSimDcSwitchStateMachine.this.logd("AttachingState going to retry");
                        doEnter();
                        return true;
                    case 278530:
                        HwVSimDcSwitchStateMachine.this.logd("AttachingState: REQ_DISCONNECT_ALL");
                        HwVSimDcSwitchStateMachine.this.deferMessage(msg);
                        return true;
                    default:
                        HwVSimDcSwitchStateMachine hwVSimDcSwitchStateMachine2 = HwVSimDcSwitchStateMachine.this;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("AttachingState: nothandled msg.what=0x");
                        stringBuilder2.append(Integer.toHexString(msg.what));
                        hwVSimDcSwitchStateMachine2.logv(stringBuilder2.toString());
                        return false;
                }
            } else {
                HwVSimDcSwitchStateMachine.this.logd("AttachingState: EVENT_DATA_ATTACHED");
                HwVSimDcSwitchStateMachine.this.transitionTo(HwVSimDcSwitchStateMachine.this.mAttachedState);
                return true;
            }
            return retVal;
        }
    }

    private class DefaultState extends State {
        private DefaultState() {
        }

        public boolean processMessage(Message msg) {
            int i = 0;
            HwVSimDcSwitchStateMachine hwVSimDcSwitchStateMachine;
            StringBuilder stringBuilder;
            boolean val;
            HwVSimDcSwitchStateMachine hwVSimDcSwitchStateMachine2;
            StringBuilder stringBuilder2;
            AsyncChannel access$1300;
            switch (msg.what) {
                case 69633:
                    if (HwVSimDcSwitchStateMachine.this.mAc == null) {
                        HwVSimDcSwitchStateMachine.this.mAc = new AsyncChannel();
                        HwVSimDcSwitchStateMachine.this.mAc.connected(null, HwVSimDcSwitchStateMachine.this.getHandler(), msg.replyTo);
                        HwVSimDcSwitchStateMachine.this.logv("DcDefaultState: FULL_CONNECTION reply connected");
                        HwVSimDcSwitchStateMachine.this.mAc.replyToMessage(msg, 69634, 0, HwVSimDcSwitchStateMachine.this.mId, "hi");
                        break;
                    }
                    hwVSimDcSwitchStateMachine = HwVSimDcSwitchStateMachine.this;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Disconnecting to previous connection mAc=");
                    stringBuilder.append(HwVSimDcSwitchStateMachine.this.mAc);
                    hwVSimDcSwitchStateMachine.logv(stringBuilder.toString());
                    HwVSimDcSwitchStateMachine.this.mAc.replyToMessage(msg, 69634, 3);
                    break;
                case 69635:
                    HwVSimDcSwitchStateMachine.this.logv("CMD_CHANNEL_DISCONNECT");
                    HwVSimDcSwitchStateMachine.this.mAc.disconnect();
                    break;
                case 69636:
                    HwVSimDcSwitchStateMachine.this.logv("CMD_CHANNEL_DISCONNECTED");
                    HwVSimDcSwitchStateMachine.this.mAc = null;
                    break;
                case 278531:
                    val = HwVSimDcSwitchStateMachine.this.getCurrentState() == HwVSimDcSwitchStateMachine.this.mIdleState;
                    hwVSimDcSwitchStateMachine2 = HwVSimDcSwitchStateMachine.this;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("REQ_IS_IDLE_STATE  isIdle=");
                    stringBuilder2.append(val);
                    hwVSimDcSwitchStateMachine2.logv(stringBuilder2.toString());
                    access$1300 = HwVSimDcSwitchStateMachine.this.mAc;
                    if (val) {
                        i = 1;
                    }
                    access$1300.replyToMessage(msg, 278532, i);
                    break;
                case 278533:
                    val = HwVSimDcSwitchStateMachine.this.getCurrentState() == HwVSimDcSwitchStateMachine.this.mIdleState || HwVSimDcSwitchStateMachine.this.getCurrentState() == HwVSimDcSwitchStateMachine.this.mDetachingState;
                    hwVSimDcSwitchStateMachine2 = HwVSimDcSwitchStateMachine.this;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("REQ_IS_IDLE_OR_DETACHING_STATE  isIdleDetaching=");
                    stringBuilder2.append(val);
                    hwVSimDcSwitchStateMachine2.logv(stringBuilder2.toString());
                    access$1300 = HwVSimDcSwitchStateMachine.this.mAc;
                    if (val) {
                        i = 1;
                    }
                    access$1300.replyToMessage(msg, 278534, i);
                    break;
                default:
                    hwVSimDcSwitchStateMachine = HwVSimDcSwitchStateMachine.this;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("DefaultState: shouldn't happen but ignore msg.what=0x");
                    stringBuilder.append(Integer.toHexString(msg.what));
                    hwVSimDcSwitchStateMachine.logd(stringBuilder.toString());
                    break;
            }
            return true;
        }
    }

    private class DetachingState extends State {
        private int mCurrentDisallowedSequence;

        private DetachingState() {
            this.mCurrentDisallowedSequence = 0;
        }

        public void enter() {
            HwVSimDcSwitchStateMachine.this.logd("DetachingState: enter");
            CommandsInterface commandsInterface = HwVSimDcSwitchStateMachine.this.mPhone.mCi;
            HwVSimDcSwitchStateMachine hwVSimDcSwitchStateMachine = HwVSimDcSwitchStateMachine.this;
            int i = this.mCurrentDisallowedSequence + 1;
            this.mCurrentDisallowedSequence = i;
            commandsInterface.setDataAllowed(false, hwVSimDcSwitchStateMachine.obtainMessage(HwVSimDcSwitchStateMachine.EVENT_DATA_DISALLOWED, i, 0));
        }

        public boolean processMessage(Message msg) {
            boolean retVal;
            int i = msg.what;
            HwVSimDcSwitchStateMachine hwVSimDcSwitchStateMachine;
            StringBuilder stringBuilder;
            if (i == HwVSimDcSwitchStateMachine.EVENT_DATA_DISALLOWED) {
                AsyncResult retVal2 = (AsyncResult) msg.obj;
                if (this.mCurrentDisallowedSequence != msg.arg1) {
                    hwVSimDcSwitchStateMachine = HwVSimDcSwitchStateMachine.this;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("EVENT_DATA_DISALLOWED ignored arg1=");
                    stringBuilder.append(msg.arg1);
                    stringBuilder.append(", seq=");
                    stringBuilder.append(this.mCurrentDisallowedSequence);
                    hwVSimDcSwitchStateMachine.loge(stringBuilder.toString());
                } else if (retVal2.exception != null) {
                    hwVSimDcSwitchStateMachine = HwVSimDcSwitchStateMachine.this;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("EVENT_DATA_DISALLOWED failed, ");
                    stringBuilder.append(retVal2.exception);
                    hwVSimDcSwitchStateMachine.loge(stringBuilder.toString());
                    HwVSimDcSwitchStateMachine.this.transitionTo(HwVSimDcSwitchStateMachine.this.mAttachedState);
                }
                HwVSimDcSwitchStateMachine.this.transitionTo(HwVSimDcSwitchStateMachine.this.mIdleState);
                retVal = true;
            } else if (i == 278528) {
                RequestInfo apnRequest = msg.obj;
                apnRequest.log("DcSwitchStateMachine.DetachingState: REQ_CONNECT");
                hwVSimDcSwitchStateMachine = HwVSimDcSwitchStateMachine.this;
                stringBuilder = new StringBuilder();
                stringBuilder.append("DetachingState: REQ_CONNECT, apnRequest=");
                stringBuilder.append(apnRequest);
                hwVSimDcSwitchStateMachine.logd(stringBuilder.toString());
                HwVSimDcSwitchStateMachine.this.deferMessage(msg);
                retVal = true;
            } else if (i == 278530) {
                HwVSimDcSwitchStateMachine.this.logd("DetachingState: REQ_DISCONNECT_ALL, already detaching");
                return true;
            } else if (i != 278536) {
                HwVSimDcSwitchStateMachine hwVSimDcSwitchStateMachine2 = HwVSimDcSwitchStateMachine.this;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("DetachingState: nothandled msg.what=0x");
                stringBuilder2.append(Integer.toHexString(msg.what));
                hwVSimDcSwitchStateMachine2.logv(stringBuilder2.toString());
                return false;
            } else {
                HwVSimDcSwitchStateMachine.this.logd("DetachingState: EVENT_DATA_DETACHED");
                HwVSimDcSwitchStateMachine.this.transitionTo(HwVSimDcSwitchStateMachine.this.mIdleState);
                return true;
            }
            return retVal;
        }
    }

    private class IdleState extends State {
        private IdleState() {
        }

        public void enter() {
            HwVSimDcSwitchStateMachine.this.logd("IdleState: enter");
            try {
                HwVSimDcSwitchStateMachine.this.mVSimDctController.processRequests();
            } catch (RuntimeException e) {
                HwVSimDcSwitchStateMachine.this.loge("DctController is not ready");
            }
        }

        public boolean processMessage(Message msg) {
            int i = msg.what;
            if (i == 274432) {
                HwVSimDcSwitchStateMachine.this.logd("IdleState: Receive invalid event EVENT_CONNECTED!");
                return true;
            } else if (i == 278528) {
                RequestInfo apnRequest = msg.obj;
                apnRequest.log("DcSwitchStateMachine.IdleState: REQ_CONNECT");
                HwVSimDcSwitchStateMachine hwVSimDcSwitchStateMachine = HwVSimDcSwitchStateMachine.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("IdleState: REQ_CONNECT, apnRequest=");
                stringBuilder.append(apnRequest);
                hwVSimDcSwitchStateMachine.logd(stringBuilder.toString());
                HwVSimDcSwitchStateMachine.this.transitionTo(HwVSimDcSwitchStateMachine.this.mAttachingState);
                return true;
            } else if (i == 278530) {
                HwVSimDcSwitchStateMachine.this.logd("AttachingState: REQ_DISCONNECT_ALL");
                HwVSimDcSwitchStateMachine.this.mVSimDctController.releaseAllRequests(HwVSimDcSwitchStateMachine.this.mId);
                return true;
            } else if (i != 278535) {
                HwVSimDcSwitchStateMachine hwVSimDcSwitchStateMachine2 = HwVSimDcSwitchStateMachine.this;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("IdleState: nothandled msg.what=0x");
                stringBuilder2.append(Integer.toHexString(msg.what));
                hwVSimDcSwitchStateMachine2.logv(stringBuilder2.toString());
                return false;
            } else {
                HwVSimDcSwitchStateMachine.this.logd("IdleState: EVENT_DATA_ATTACHED");
                HwVSimDcSwitchStateMachine.this.transitionTo(HwVSimDcSwitchStateMachine.this.mAttachedState);
                return true;
            }
        }
    }

    protected HwVSimDcSwitchStateMachine(HwVSimDctController vimDctController, Phone phone, String name, int id) {
        super(name);
        logd("DcSwitchState constructor E");
        this.mPhone = phone;
        this.mId = id;
        this.mVSimDctController = vimDctController;
        addState(this.mDefaultState);
        addState(this.mIdleState, this.mDefaultState);
        addState(this.mAttachingState, this.mDefaultState);
        addState(this.mAttachedState, this.mDefaultState);
        addState(this.mDetachingState, this.mDefaultState);
        setInitialState(this.mIdleState);
        logd("DcSwitchState constructor X");
    }

    protected void logd(String s) {
        HwVSimLog.VSimLogD(LOG_TAG, s);
    }

    protected void loge(String s) {
        HwVSimLog.VSimLogD(LOG_TAG, s);
    }

    protected void logv(String s) {
        HwVSimLog.VSimLogD(LOG_TAG, s);
    }
}
