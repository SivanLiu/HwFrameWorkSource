package com.android.server.security.trustcircle.lifecycle;

import android.os.Message;
import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.security.trustcircle.lifecycle.TcisLifeCycleDispatcher.CancelData;
import com.android.server.security.trustcircle.lifecycle.TcisLifeCycleDispatcher.LoginServerRequestData;
import com.android.server.security.trustcircle.lifecycle.TcisLifeCycleDispatcher.LogoutData;
import com.android.server.security.trustcircle.lifecycle.TcisLifeCycleDispatcher.RegisterData;
import com.android.server.security.trustcircle.lifecycle.TcisLifeCycleDispatcher.UnregData;
import com.android.server.security.trustcircle.lifecycle.TcisLifeCycleDispatcher.UpdateData;
import com.android.server.security.trustcircle.lifecycle.TcisLifeCycleDispatcher.UpdateRequestData;
import com.android.server.security.trustcircle.utils.LogHelper;
import com.android.server.security.trustcircle.utils.Status.LifeCycleStauts;
import com.android.server.security.trustcircle.utils.Status.TCIS_Result;

public class LifeCycleStateMachine extends StateMachine {
    public static final int MSG_CANCEL = 5;
    public static final int MSG_CHECK_LOGIN_STATUS = 2;
    public static final int MSG_CHECK_REG_STATUS = 1;
    public static final int MSG_DELETE_ACCOUNT = 7;
    public static final int MSG_FINAL_REG = 3;
    public static final int MSG_LOGOUT = 6;
    public static final int MSG_SWITCH_USER = 9;
    public static final int MSG_TIME_OUT = 8;
    public static final int MSG_UPDATE = 4;
    private static final String TAG = LifeCycleStateMachine.class.getSimpleName();
    public static final int TIME_OUT_TIME = 10000;
    private State mIDLEState = new IDLEState();
    private State mLoginedState = new LoginedState();
    private State mLoginingState = new LoginingState();
    private State mRegisteringState = new RegisteringState();
    private State mSameUserState = new SameUserState();
    private State mSwitchingUserState = new SwitchingUserState();

    class IDLEState extends State {
        IDLEState() {
        }

        public void enter() {
            String access$000 = LifeCycleStateMachine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("enter ");
            stringBuilder.append(getName());
            LogHelper.d(access$000, stringBuilder.toString());
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    LifeCycleStateMachine.this.checkRegStatus(msg);
                    return true;
                case 2:
                    LifeCycleStateMachine.this.checkLoginStatus(msg);
                    return true;
                case 6:
                    LifeCycleStateMachine.this.logout(msg);
                    return true;
                case 7:
                    LifeCycleStateMachine.this.unregister(msg);
                    return true;
                case 9:
                    return false;
                default:
                    String access$000 = LifeCycleStateMachine.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("error:unexpceted action in ");
                    stringBuilder.append(getName());
                    stringBuilder.append(" : ");
                    stringBuilder.append(msg.what);
                    LogHelper.w(access$000, stringBuilder.toString());
                    return true;
            }
        }

        public void exit() {
            String access$000 = LifeCycleStateMachine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("exit ");
            stringBuilder.append(getName());
            LogHelper.d(access$000, stringBuilder.toString());
        }
    }

    class LoginedState extends State {
        LoginedState() {
        }

        public void enter() {
            String access$000 = LifeCycleStateMachine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("enter ");
            stringBuilder.append(getName());
            LogHelper.d(access$000, stringBuilder.toString());
            LifeCycleStateMachine.this.removeMessages(8);
            LifeCycleProcessor.updateTime(System.currentTimeMillis());
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    LifeCycleStateMachine.this.checkRegStatus(msg);
                    return true;
                case 2:
                    LifeCycleStateMachine.this.checkLoginStatus(msg);
                    return true;
                case 6:
                    LifeCycleStateMachine.this.logout(msg);
                    return true;
                case 7:
                    LifeCycleStateMachine.this.unregister(msg);
                    return true;
                case 9:
                    return false;
                default:
                    String access$000 = LifeCycleStateMachine.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("error:unexpceted action in ");
                    stringBuilder.append(getName());
                    stringBuilder.append(" : ");
                    stringBuilder.append(msg.what);
                    LogHelper.w(access$000, stringBuilder.toString());
                    return true;
            }
        }

        public void exit() {
            String access$000 = LifeCycleStateMachine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("exit ");
            stringBuilder.append(getName());
            LogHelper.d(access$000, stringBuilder.toString());
        }
    }

    class LoginingState extends State {
        LoginingState() {
        }

        public void enter() {
            String access$000 = LifeCycleStateMachine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("enter ");
            stringBuilder.append(getName());
            LogHelper.d(access$000, stringBuilder.toString());
        }

        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case 4:
                    LifeCycleStateMachine.this.removeMessages(8);
                    LifeCycleStateMachine.this.finalLogin(msg);
                    LifeCycleStateMachine.this.sendMessageDelayed(8, MemoryConstant.MIN_INTERVAL_OP_TIMEOUT);
                    return true;
                case 5:
                    LifeCycleStateMachine.this.removeMessages(8);
                    LifeCycleStateMachine.this.cancel(msg, this);
                    return true;
                case 6:
                    LifeCycleStateMachine.this.removeMessages(8);
                    LifeCycleStateMachine.this.logout(msg);
                    return true;
                case 7:
                    LifeCycleStateMachine.this.removeMessages(8);
                    LifeCycleStateMachine.this.unregister(msg);
                    return true;
                case 8:
                    LifeCycleStateMachine.this.processTimeout();
                    LifeCycleProcessor.onFinalLoginResult(TCIS_Result.TIMEOUT.value());
                    return true;
                case 9:
                    return false;
                default:
                    LogHelper.w(LifeCycleStateMachine.TAG, "error:LoginingState");
                    return true;
            }
        }

        public void exit() {
            String access$000 = LifeCycleStateMachine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("exit ");
            stringBuilder.append(getName());
            LogHelper.d(access$000, stringBuilder.toString());
        }
    }

    class RegisteringState extends State {
        RegisteringState() {
        }

        public void enter() {
            String access$000 = LifeCycleStateMachine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("enter ");
            stringBuilder.append(getName());
            LogHelper.d(access$000, stringBuilder.toString());
        }

        public boolean processMessage(Message msg) {
            int i = msg.what;
            if (i == 3) {
                LifeCycleStateMachine.this.removeMessages(8);
                LifeCycleStateMachine.this.finalRegister(msg);
                return true;
            } else if (i != 5) {
                switch (i) {
                    case 8:
                        LifeCycleStateMachine.this.processTimeout();
                        LifeCycleProcessor.onFinalRegisterResult(TCIS_Result.TIMEOUT.value());
                        return true;
                    case 9:
                        return false;
                    default:
                        String access$000 = LifeCycleStateMachine.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("error:unexpceted action in ");
                        stringBuilder.append(getName());
                        stringBuilder.append(" : ");
                        stringBuilder.append(msg.what);
                        LogHelper.w(access$000, stringBuilder.toString());
                        return true;
                }
            } else {
                LifeCycleStateMachine.this.removeMessages(8);
                LifeCycleStateMachine.this.cancel(msg, this);
                return true;
            }
        }

        public void exit() {
            String access$000 = LifeCycleStateMachine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("exit ");
            stringBuilder.append(getName());
            LogHelper.d(access$000, stringBuilder.toString());
        }
    }

    class SameUserState extends State {
        SameUserState() {
        }

        public void enter() {
            String access$000 = LifeCycleStateMachine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("enter ");
            stringBuilder.append(getName());
            LogHelper.d(access$000, stringBuilder.toString());
        }

        public boolean processMessage(Message msg) {
            if (msg.what != 9) {
                String access$000 = LifeCycleStateMachine.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("error:unexpceted action in ");
                stringBuilder.append(getName());
                stringBuilder.append(" : ");
                stringBuilder.append(msg.what);
                LogHelper.w(access$000, stringBuilder.toString());
                return true;
            }
            LifeCycleStateMachine.this.transitionTo(LifeCycleStateMachine.this.mSwitchingUserState);
            LifeCycleStateMachine.this.sendMessage(Message.obtain(msg));
            return true;
        }

        public void exit() {
            String access$000 = LifeCycleStateMachine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("exit ");
            stringBuilder.append(getName());
            LogHelper.d(access$000, stringBuilder.toString());
        }
    }

    class SwitchingUserState extends State {
        SwitchingUserState() {
        }

        public void enter() {
            String access$000 = LifeCycleStateMachine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("enter ");
            stringBuilder.append(getName());
            LogHelper.d(access$000, stringBuilder.toString());
            LifeCycleStateMachine.this.removeMessages(8);
            LifeCycleProcessor.updateTime(System.currentTimeMillis());
        }

        public boolean processMessage(Message msg) {
            if (msg.what != 9) {
                String access$000 = LifeCycleStateMachine.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("error:unexpceted action in ");
                stringBuilder.append(getName());
                stringBuilder.append(" : ");
                stringBuilder.append(msg.what);
                LogHelper.w(access$000, stringBuilder.toString());
                LifeCycleStateMachine.this.transitionTo(LifeCycleStateMachine.this.mIDLEState);
                return true;
            }
            LifeCycleStateMachine.this.switchUser(msg);
            return true;
        }

        public void exit() {
            String access$000 = LifeCycleStateMachine.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("exit ");
            stringBuilder.append(getName());
            LogHelper.d(access$000, stringBuilder.toString());
        }
    }

    public enum TcisState {
        IDLE(IDLEState.class),
        REG_ING(RegisteringState.class),
        LOGIN_ING(LoginingState.class),
        LOGIN_ED(LoginedState.class);
        
        private Class clazz;

        private TcisState(Class clazz) {
            this.clazz = clazz;
        }

        public Class getStateClass() {
            return this.clazz;
        }
    }

    public LifeCycleStateMachine() {
        super(TAG);
        addState(this.mSwitchingUserState);
        addState(this.mSameUserState);
        addState(this.mIDLEState, this.mSameUserState);
        addState(this.mRegisteringState, this.mSameUserState);
        addState(this.mLoginingState, this.mSameUserState);
        addState(this.mLoginedState, this.mSameUserState);
        setInitialState(this.mSwitchingUserState);
        start();
    }

    public int getCurrentLifeState() {
        IState currentState = super.getCurrentState();
        if (currentState == null) {
            return TcisState.IDLE.ordinal();
        }
        for (TcisState state : TcisState.values()) {
            if (state.getStateClass() == currentState.getClass()) {
                return state.ordinal();
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unknown current state : ");
        stringBuilder.append(currentState.getName());
        LogHelper.e(str, stringBuilder.toString());
        return TcisState.IDLE.ordinal();
    }

    public void checkRegisterStatus(Object obj) {
        sendMessage(1, obj);
    }

    public void getRegData(Object obj) {
        sendMessage(3, obj);
    }

    public void cancelRegOrLogin(Object obj) {
        sendMessage(5, obj);
    }

    public void checkLoginStatus(Object obj) {
        sendMessage(2, obj);
    }

    public void getUpdateData(Object obj) {
        sendMessage(4, obj);
    }

    public void logout(Object obj) {
        sendMessage(6, obj);
    }

    public void delAccount(Object obj) {
        sendMessage(7, obj);
    }

    public void timeout(Object obj) {
        sendMessage(8, obj);
    }

    public void switchUser(int newUserHandle) {
        sendMessage(9, newUserHandle);
    }

    private void checkRegStatus(Message msg) {
        if (msg == null || !(msg.obj instanceof LoginServerRequestData)) {
            LogHelper.e(TAG, "checkRegStatus - message is invalid");
            return;
        }
        LoginServerRequestData data = msg.obj;
        if (LifeCycleProcessor.checkNeedRegister(data.serverRegisterStatus, data.userID)) {
            LogHelper.i(TAG, "needs to register");
            if (LifeCycleProcessor.register(data.userID, data.sessionID)) {
                sendMessageDelayed(8, MemoryConstant.MIN_INTERVAL_OP_TIMEOUT);
                transitionTo(this.mRegisteringState);
            }
        } else {
            LogHelper.i(TAG, "already registered, will login");
            if (LifeCycleProcessor.loginAndUpdate(data.userID)) {
                LogHelper.i(TAG, "login successfully");
                sendMessageDelayed(8, MemoryConstant.MIN_INTERVAL_OP_TIMEOUT);
                transitionTo(this.mLoginingState);
            } else {
                LogHelper.w(TAG, "login failed");
                transitionTo(this.mIDLEState);
            }
        }
    }

    private void finalRegister(Message msg) {
        if (msg == null || !(msg.obj instanceof RegisterData)) {
            LogHelper.e(TAG, "finalRegister - message is invalid");
            return;
        }
        RegisterData data = msg.obj;
        if (LifeCycleProcessor.finalRegister(data.authPKInfo, data.authPKInfoSign, data.updateIndexInfo, data.updateIndexSignature)) {
            LogHelper.i(TAG, "register totally successfully");
            transitionTo(this.mLoginedState);
        } else {
            LogHelper.w(TAG, "register totally failed");
            transitionTo(this.mIDLEState);
        }
    }

    private void checkLoginStatus(Message msg) {
        if (msg == null || !(msg.obj instanceof UpdateRequestData)) {
            LogHelper.e(TAG, "checkLoginStatus - message is invalid");
            return;
        }
        UpdateRequestData updateReqData = msg.obj;
        if (LifeCycleProcessor.checkUserIDLogined(updateReqData.userID)) {
            LogHelper.i(TAG, "already login, will update");
            if (LifeCycleProcessor.updateOnly(updateReqData.userID)) {
                LogHelper.i(TAG, "login successfully");
                sendMessageDelayed(8, MemoryConstant.MIN_INTERVAL_OP_TIMEOUT);
                transitionTo(this.mLoginingState);
            } else {
                LogHelper.w(TAG, "login failed");
                transitionTo(this.mIDLEState);
            }
        } else {
            if (LifeCycleProcessor.checkTARegisterStatus(updateReqData.userID) == LifeCycleStauts.NOT_REGISTER.ordinal()) {
                LifeCycleProcessor.onUpdateResponse(TCIS_Result.NOT_REG.value(), -1, null);
                LogHelper.i(TAG, "needs to register");
            } else {
                LifeCycleProcessor.onUpdateResponse(TCIS_Result.NOT_LOGIN.value(), -1, null);
                LogHelper.i(TAG, "needs to login");
            }
            transitionTo(this.mIDLEState);
        }
    }

    private void finalLogin(Message msg) {
        if (msg == null || !(msg.obj instanceof UpdateData)) {
            LogHelper.e(TAG, "finalLogin - message is invalid");
            return;
        }
        UpdateData updateDate = msg.obj;
        if (LifeCycleProcessor.finalLogin(updateDate.updateResult, updateDate.updateIndexInfo, updateDate.updateIndexSignature)) {
            LogHelper.i(TAG, "finalLogin successfully");
            transitionTo(this.mLoginedState);
        } else {
            LogHelper.w(TAG, "finalLogin failed");
        }
    }

    private void logout(Message msg) {
        if (msg == null || !(msg.obj instanceof LogoutData)) {
            LogHelper.e(TAG, "logout - message is invalid");
            return;
        }
        if (LifeCycleProcessor.logout(msg.obj.userID)) {
            LifeCycleProcessor.updateTime(System.currentTimeMillis());
            LogHelper.i(TAG, "logout successfully");
            transitionTo(this.mIDLEState);
        } else {
            LogHelper.w(TAG, "logout failed");
        }
    }

    private void unregister(Message msg) {
        if (msg == null || !(msg.obj instanceof UnregData)) {
            LogHelper.e(TAG, "unregister - message is invalid");
            return;
        }
        if (LifeCycleProcessor.unregister(msg.obj.userID)) {
            LogHelper.i(TAG, "unregister successfully");
            transitionTo(this.mIDLEState);
        } else {
            LogHelper.i(TAG, "unregister failed");
        }
    }

    private void cancel(Message msg, State state) {
        if (msg == null || !(msg.obj instanceof CancelData)) {
            LogHelper.e(TAG, "cancel - message is invalid");
            return;
        }
        if (LifeCycleProcessor.cancelRegOrLogin(state, msg.obj.userID)) {
            LogHelper.i(TAG, "cancel login successfully");
        } else {
            LogHelper.w(TAG, "cancel login failed");
        }
        transitionTo(this.mIDLEState);
    }

    private void processTimeout() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("timeout in ");
        stringBuilder.append(getName());
        LogHelper.w(str, stringBuilder.toString());
        transitionTo(this.mIDLEState);
    }

    private void switchUser(Message msg) {
        if (msg == null) {
            LogHelper.e(TAG, "switchUser - message is null");
            return;
        }
        int newUserHandle = msg.arg1;
        String str;
        StringBuilder stringBuilder;
        if (LifeCycleProcessor.initAndroidUser(newUserHandle) != -1) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("load new tics user from user ");
            stringBuilder.append(newUserHandle);
            LogHelper.i(str, stringBuilder.toString());
            transitionTo(this.mLoginedState);
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("no tcis user logined in user ");
            stringBuilder.append(newUserHandle);
            LogHelper.w(str, stringBuilder.toString());
            transitionTo(this.mIDLEState);
        }
    }
}
