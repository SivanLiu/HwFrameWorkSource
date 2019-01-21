package com.android.internal.telephony.fullnetwork;

import android.content.Context;
import android.os.Looper;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

public class HwFullNetworkStateMachine extends StateMachine {
    private static final String LOG_TAG = "HwFullNetworkSM";
    private static HwFullNetworkCheckStateBase mCheckStateBase;
    private static HwFullNetworkChipCommon mChipCommon;
    private static HwFullNetworkDefaultStateBase mDefaultStateBase;
    private static HwFullNetworkInitStateBase mInitStateBase;
    private static HwFullNetworkStateMachine mInstance = null;
    private static final Object mLock = new Object();
    private static HwFullNetworkSetStateBase mSetStateBase;
    private final DefaultState mDefaultState = new DefaultState();
    private final InitialState mInitialState = new InitialState();
    private final MainSlotCheckState mMainSlotCheckState = new MainSlotCheckState();
    private final MainSlotSetState mMainSlotSetState = new MainSlotSetState();

    private class DefaultState extends State {
        private DefaultState() {
        }

        public void enter() {
            HwFullNetworkStateMachine.this.logd("entering DefaultState");
        }

        public void exit() {
            HwFullNetworkStateMachine.this.logd("leaving DefaultState");
        }

        public boolean processMessage(Message msg) {
            HwFullNetworkStateMachine hwFullNetworkStateMachine = HwFullNetworkStateMachine.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DefaultState Received Msg ");
            stringBuilder.append(msg.what);
            hwFullNetworkStateMachine.logd(stringBuilder.toString());
            StringBuilder stringBuilder2;
            switch (msg.what) {
                case HwFullNetworkConstants.EVENT_CHECK_MAIN_SLOT /*201*/:
                case HwFullNetworkConstants.EVENT_CHECK_MAIN_SLOT_FOR_OPEATOR /*203*/:
                case HwFullNetworkConstants.EVENT_CHECK_MAIN_SLOT_FOR_MDM /*204*/:
                case HwFullNetworkConstants.EVENT_FORCE_CHECK_MAIN_SLOT_FOR_CMCC /*207*/:
                    if (HwFullNetworkStateMachine.this.getCurrentState().equals(HwFullNetworkStateMachine.this.mMainSlotSetState)) {
                        hwFullNetworkStateMachine = HwFullNetworkStateMachine.this;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("DefaultState deferred Msg ");
                        stringBuilder2.append(msg.what);
                        hwFullNetworkStateMachine.logd(stringBuilder2.toString());
                        HwFullNetworkStateMachine.this.deferMessage(msg);
                    } else {
                        HwFullNetworkStateMachine.this.transitionTo(HwFullNetworkStateMachine.this.mMainSlotCheckState);
                        HwFullNetworkStateMachine.this.deferMessage(msg);
                    }
                    return true;
                case HwFullNetworkConstants.EVENT_SET_MAIN_SLOT /*202*/:
                    HwFullNetworkStateMachine.this.logd("DefaultState Received EVENT_SET_MAIN_SLOT.");
                    HwFullNetworkStateMachine.this.transitionTo(HwFullNetworkStateMachine.this.mMainSlotSetState);
                    HwFullNetworkStateMachine.this.deferMessage(msg);
                    return true;
                case HwFullNetworkConstants.EVENT_CHECK_NETWORK_TYPE /*205*/:
                    HwFullNetworkStateMachine.this.logd("DefaultState Received EVENT_CHECK_NETWORK_TYPE.");
                    if (HwFullNetworkStateMachine.this.getCurrentState().equals(HwFullNetworkStateMachine.this.mMainSlotSetState)) {
                        hwFullNetworkStateMachine = HwFullNetworkStateMachine.this;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("DefaultState deferred Msg ");
                        stringBuilder2.append(msg.what);
                        hwFullNetworkStateMachine.logd(stringBuilder2.toString());
                        HwFullNetworkStateMachine.this.deferMessage(msg);
                    } else {
                        HwFullNetworkStateMachine.this.transitionTo(HwFullNetworkStateMachine.this.mMainSlotCheckState);
                        HwFullNetworkStateMachine.this.deferMessage(msg);
                    }
                    return true;
                default:
                    hwFullNetworkStateMachine = HwFullNetworkStateMachine.this;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("DefaultState.processMessage default:");
                    stringBuilder2.append(msg.what);
                    hwFullNetworkStateMachine.logd(stringBuilder2.toString());
                    return true;
            }
        }
    }

    private class InitialState extends State {
        private InitialState() {
        }

        public void enter() {
            HwFullNetworkStateMachine.this.logd("entering InitialState");
        }

        public void exit() {
            HwFullNetworkStateMachine.this.logd("leaving InitialState");
        }

        public boolean processMessage(Message msg) {
            int i = msg.what;
            HwFullNetworkStateMachine hwFullNetworkStateMachine = HwFullNetworkStateMachine.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("InitialState.processMessage default:");
            stringBuilder.append(msg.what);
            hwFullNetworkStateMachine.logd(stringBuilder.toString());
            return false;
        }
    }

    private class MainSlotCheckState extends State {
        private MainSlotCheckState() {
        }

        public void enter() {
            HwFullNetworkStateMachine.this.logd("entering MainSlotCheckState");
        }

        public void exit() {
            HwFullNetworkStateMachine.this.logd("leaving MainSlotCheckState");
        }

        public boolean processMessage(Message msg) {
            int i = msg.what;
            HwFullNetworkStateMachine hwFullNetworkStateMachine;
            if (i == HwFullNetworkConstants.EVENT_CHECK_MAIN_SLOT) {
                HwFullNetworkStateMachine.this.logd("MainSlotCheckState Received EVENT_CHECK_MAIN_SLOT.");
                if (HwFullNetworkStateMachine.mCheckStateBase.checkIfAllCardsReady(msg)) {
                    HwFullNetworkStateMachine.this.sendMessage(HwFullNetworkConstants.EVENT_GET_MAIN_SLOT);
                }
                return true;
            } else if (i == HwFullNetworkConstants.EVENT_FORCE_CHECK_MAIN_SLOT_FOR_CMCC) {
                HwFullNetworkStateMachine.this.logd("MainSlotCheckState Received EVENT_FORCE_CHECK_MAIN_SLOT_FOR_CMCC.");
                if (HwFullNetworkStateMachine.mCheckStateBase.judgeSetDefault4GSlotForCMCC(((Integer) msg.obj).intValue())) {
                    HwFullNetworkStateMachine.this.sendMessage(HwFullNetworkConstants.EVENT_GET_MAIN_SLOT);
                }
                return true;
            } else if (i != HwFullNetworkConstants.EVENT_GET_MAIN_SLOT) {
                switch (i) {
                    case HwFullNetworkConstants.EVENT_CHECK_MAIN_SLOT_FOR_OPEATOR /*203*/:
                        HwFullNetworkStateMachine.this.logd("MainSlotCheckState Received EVENT_CHECK_MAIN_SLOT_FOR_OPEATOR.");
                        return true;
                    case HwFullNetworkConstants.EVENT_CHECK_MAIN_SLOT_FOR_MDM /*204*/:
                        HwFullNetworkStateMachine.this.logd("MainSlotCheckState Received EVENT_CHECK_MAIN_SLOT_FOR_MDM.");
                        i = HwFullNetworkStateMachine.mChipCommon.getUserSwitchDualCardSlots();
                        if (HwFullNetworkStateMachine.mCheckStateBase.judgeDefaultMainSlotForMDM()) {
                            int mdmSlotId = HwFullNetworkStateMachine.mCheckStateBase.getDefaultMainSlot();
                            if (!(i == mdmSlotId || HwFullNetworkStateMachine.mChipCommon.getWaitingSwitchBalongSlot())) {
                                HwFullNetworkStateMachine hwFullNetworkStateMachine2 = HwFullNetworkStateMachine.this;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("MainSlotCheckState slotId:");
                                stringBuilder.append(mdmSlotId);
                                hwFullNetworkStateMachine2.logd(stringBuilder.toString());
                                HwFullNetworkStateMachine.this.sendMessage(HwFullNetworkConstants.EVENT_SET_MAIN_SLOT, mdmSlotId, 0);
                            }
                        }
                        return true;
                    case HwFullNetworkConstants.EVENT_CHECK_NETWORK_TYPE /*205*/:
                        HwFullNetworkStateMachine.this.logd("MainSlotCheckState Received EVENT_CHECK_NETWORK_TYPE.");
                        HwFullNetworkStateMachine.mCheckStateBase.checkNetworkType();
                        return true;
                    default:
                        hwFullNetworkStateMachine = HwFullNetworkStateMachine.this;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("MainSlotCheckState.processMessage default:");
                        stringBuilder2.append(msg.what);
                        hwFullNetworkStateMachine.logd(stringBuilder2.toString());
                        return false;
                }
            } else {
                HwFullNetworkStateMachine.this.logd("MainSlotCheckState Received EVENT_GET_MAIN_SLOT.");
                HwFullNetworkStateMachine.mChipCommon.default4GSlot = HwFullNetworkStateMachine.mCheckStateBase.getDefaultMainSlot();
                hwFullNetworkStateMachine = HwFullNetworkStateMachine.this;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("MainSlotCheckState slotId:");
                stringBuilder3.append(HwFullNetworkStateMachine.mChipCommon.default4GSlot);
                hwFullNetworkStateMachine.logd(stringBuilder3.toString());
                HwFullNetworkStateMachine.this.sendMessage(HwFullNetworkConstants.EVENT_SET_MAIN_SLOT, HwFullNetworkStateMachine.mChipCommon.default4GSlot, 0);
                return true;
            }
        }
    }

    private class MainSlotSetState extends State {
        private MainSlotSetState() {
        }

        public void enter() {
            HwFullNetworkStateMachine.this.logd("entering MainSlotSetState");
        }

        public void exit() {
            HwFullNetworkStateMachine.this.logd("leaving MainSlotSetState");
        }

        public boolean processMessage(Message msg) {
            int i = msg.what;
            if (i == HwFullNetworkConstants.EVENT_SET_MAIN_SLOT) {
                HwFullNetworkStateMachine.this.logd("MainSlotSetState Received EVENT_SET_MAIN_SLOT.");
                i = msg.arg1;
                Message response = null;
                if (msg.obj != null) {
                    response = msg.obj;
                }
                HwFullNetworkStateMachine.mSetStateBase.setMainSlot(i, response);
                return true;
            } else if (i != HwFullNetworkConstants.EVENT_TRANS_TO_DEFAULT) {
                HwFullNetworkStateMachine hwFullNetworkStateMachine = HwFullNetworkStateMachine.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("MainSlotSetState.processMessage default:");
                stringBuilder.append(msg.what);
                hwFullNetworkStateMachine.logd(stringBuilder.toString());
                return false;
            } else {
                HwFullNetworkStateMachine.this.transitionTo(HwFullNetworkStateMachine.this.mDefaultState);
                return true;
            }
        }
    }

    private HwFullNetworkStateMachine(Context c, CommandsInterface[] ci) {
        super(LOG_TAG, Looper.getMainLooper());
        mChipCommon = HwFullNetworkChipCommon.getInstance();
        addState(this.mDefaultState);
        addState(this.mInitialState, this.mDefaultState);
        addState(this.mMainSlotCheckState, this.mDefaultState);
        addState(this.mMainSlotSetState, this.mDefaultState);
        setInitialState(this.mInitialState);
        log("HwFullNetworkStateMachine construct finish!");
    }

    static HwFullNetworkStateMachine make(Context c, CommandsInterface[] ci) {
        HwFullNetworkStateMachine hwFullNetworkStateMachine;
        synchronized (mLock) {
            if (mInstance == null) {
                mInstance = new HwFullNetworkStateMachine(c, ci);
                mInstance.start();
                mDefaultStateBase = HwFullNetworkDefaultStateFactory.getHwFullNetworkDefaultState(c, ci, mInstance.getHandler());
                mInitStateBase = HwFullNetworkInitStateFactory.getHwFullNetworkInitState(c, ci, mInstance.getHandler());
                mCheckStateBase = HwFullNetworkCheckStateFactory.getHwFullNetworkCheckState(c, ci, mInstance.getHandler());
                mSetStateBase = HwFullNetworkSetStateFactory.getHwFullNetworkSetState(c, ci, mInstance.getHandler());
                hwFullNetworkStateMachine = mInstance;
            } else {
                throw new RuntimeException("HwFullNetworkStateMachine.make() should only be called once");
            }
        }
        return hwFullNetworkStateMachine;
    }

    HwFullNetworkDefaultStateBase getDefaultStateBase() {
        return mDefaultStateBase;
    }

    HwFullNetworkInitStateBase getInitStateBase() {
        return mInitStateBase;
    }

    public void dispose() {
        quit();
    }

    protected void logd(String s) {
        Rlog.d(getName(), s);
    }

    protected void loge(String s) {
        Rlog.e(getName(), s);
    }
}
