package com.android.internal.telephony.cat;

import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.uicc.IccFileHandler;
import com.android.internal.telephony.uicc.IccUtils;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

class RilMessageDecoder extends StateMachine {
    private static final int CMD_PARAMS_READY = 2;
    private static final int CMD_START = 1;
    private static RilMessageDecoder[] mInstance = null;
    private static int mSimCount = 0;
    private Handler mCaller = null;
    private CommandParamsFactory mCmdParamsFactory = null;
    private RilMessage mCurrentRilMessage = null;
    private StateCmdParamsReady mStateCmdParamsReady = new StateCmdParamsReady();
    private StateStart mStateStart = new StateStart();

    private class StateCmdParamsReady extends State {
        private StateCmdParamsReady() {
        }

        public boolean processMessage(Message msg) {
            if (msg.what == 2) {
                RilMessageDecoder.this.mCurrentRilMessage.mResCode = ResultCode.fromInt(msg.arg1);
                RilMessageDecoder.this.mCurrentRilMessage.mData = msg.obj;
                RilMessageDecoder.this.sendCmdForExecution(RilMessageDecoder.this.mCurrentRilMessage);
                RilMessageDecoder.this.transitionTo(RilMessageDecoder.this.mStateStart);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("StateCmdParamsReady expecting CMD_PARAMS_READY=2 got ");
                stringBuilder.append(msg.what);
                CatLog.d((Object) this, stringBuilder.toString());
                RilMessageDecoder.this.deferMessage(msg);
            }
            return true;
        }
    }

    private class StateStart extends State {
        private StateStart() {
        }

        public boolean processMessage(Message msg) {
            if (msg.what != 1) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("StateStart unexpected expecting START=1 got ");
                stringBuilder.append(msg.what);
                CatLog.d((Object) this, stringBuilder.toString());
            } else if (RilMessageDecoder.this.decodeMessageParams((RilMessage) msg.obj)) {
                RilMessageDecoder.this.transitionTo(RilMessageDecoder.this.mStateCmdParamsReady);
            }
            return true;
        }
    }

    public static synchronized RilMessageDecoder getInstance(Handler caller, IccFileHandler fh, int slotId) {
        synchronized (RilMessageDecoder.class) {
            if (mInstance == null) {
                mSimCount = TelephonyManager.getDefault().getSimCount();
                mInstance = new RilMessageDecoder[mSimCount];
                for (int i = 0; i < mSimCount; i++) {
                    mInstance[i] = null;
                }
            }
            if (slotId == -1 || slotId >= mSimCount) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("invaild slot id: ");
                stringBuilder.append(slotId);
                CatLog.d("RilMessageDecoder", stringBuilder.toString());
                return null;
            }
            if (mInstance[slotId] == null) {
                mInstance[slotId] = new RilMessageDecoder(caller, fh);
            }
            RilMessageDecoder rilMessageDecoder = mInstance[slotId];
            return rilMessageDecoder;
        }
    }

    public void sendStartDecodingMessageParams(RilMessage rilMsg) {
        Message msg = obtainMessage(1);
        msg.obj = rilMsg;
        sendMessage(msg);
    }

    public void sendMsgParamsDecoded(ResultCode resCode, CommandParams cmdParams) {
        Message msg = obtainMessage(2);
        msg.arg1 = resCode.value();
        msg.obj = cmdParams;
        sendMessage(msg);
    }

    private void sendCmdForExecution(RilMessage rilMsg) {
        this.mCaller.obtainMessage(10, new RilMessage(rilMsg)).sendToTarget();
    }

    private RilMessageDecoder(Handler caller, IccFileHandler fh) {
        super("RilMessageDecoder");
        addState(this.mStateStart);
        addState(this.mStateCmdParamsReady);
        setInitialState(this.mStateStart);
        this.mCaller = caller;
        this.mCmdParamsFactory = CommandParamsFactory.getInstance(this, fh);
    }

    private RilMessageDecoder() {
        super("RilMessageDecoder");
    }

    private boolean decodeMessageParams(RilMessage rilMsg) {
        this.mCurrentRilMessage = rilMsg;
        switch (rilMsg.mId) {
            case 1:
            case 4:
                this.mCurrentRilMessage.mResCode = ResultCode.OK;
                sendCmdForExecution(this.mCurrentRilMessage);
                return false;
            case 2:
            case 3:
            case 5:
                byte[] rawData = null;
                try {
                    try {
                        this.mCmdParamsFactory.make(BerTlv.decode(IccUtils.hexStringToBytes((String) rilMsg.mData)));
                        return true;
                    } catch (ResultException e) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("decodeMessageParams: caught ResultException e=");
                        stringBuilder.append(e);
                        CatLog.d((Object) this, stringBuilder.toString());
                        this.mCurrentRilMessage.mResCode = e.result();
                        this.mCurrentRilMessage.mData = null;
                        sendCmdForExecution(this.mCurrentRilMessage);
                        return false;
                    }
                } catch (Exception e2) {
                    CatLog.d((Object) this, "decodeMessageParams dropping zombie messages");
                    return false;
                }
            default:
                return false;
        }
    }

    public void dispose() {
        quitNow();
        this.mStateStart = null;
        this.mStateCmdParamsReady = null;
        this.mCmdParamsFactory.dispose();
        this.mCmdParamsFactory = null;
        this.mCurrentRilMessage = null;
        this.mCaller = null;
        mInstance = null;
    }
}
