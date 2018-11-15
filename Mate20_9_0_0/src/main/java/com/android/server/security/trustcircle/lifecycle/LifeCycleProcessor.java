package com.android.server.security.trustcircle.lifecycle;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.TextUtils;
import com.android.internal.util.State;
import com.android.server.security.trustcircle.CallbackManager;
import com.android.server.security.trustcircle.jni.TcisJNI;
import com.android.server.security.trustcircle.tlv.command.login.CMD_LOGIN_CANCEL;
import com.android.server.security.trustcircle.tlv.command.login.CMD_LOGIN_REQ;
import com.android.server.security.trustcircle.tlv.command.login.CMD_LOGIN_RESULT_UPDATE;
import com.android.server.security.trustcircle.tlv.command.login.RET_LOGIN_REQ;
import com.android.server.security.trustcircle.tlv.command.logout.CMD_LOGOUT_REQ;
import com.android.server.security.trustcircle.tlv.command.query.CMD_GET_LOGIN_STATUS;
import com.android.server.security.trustcircle.tlv.command.query.CMD_GET_TCIS_ID;
import com.android.server.security.trustcircle.tlv.command.query.CMD_INIT_REQ;
import com.android.server.security.trustcircle.tlv.command.query.DATA_TCIS_ERROR_STEP;
import com.android.server.security.trustcircle.tlv.command.query.RET_GET_LOGIN_STATUS;
import com.android.server.security.trustcircle.tlv.command.query.RET_GET_TCIS_ID;
import com.android.server.security.trustcircle.tlv.command.query.RET_INIT_REQ;
import com.android.server.security.trustcircle.tlv.command.register.CMD_CHECK_REG_STATUS;
import com.android.server.security.trustcircle.tlv.command.register.CMD_REG_CANCEL;
import com.android.server.security.trustcircle.tlv.command.register.CMD_REG_REQ;
import com.android.server.security.trustcircle.tlv.command.register.CMD_REG_RESULT;
import com.android.server.security.trustcircle.tlv.command.register.CMD_UNREG_REQ;
import com.android.server.security.trustcircle.tlv.command.register.RET_REG_REQ;
import com.android.server.security.trustcircle.tlv.core.TLVEngine;
import com.android.server.security.trustcircle.tlv.core.TLVEngine.TLVResult;
import com.android.server.security.trustcircle.tlv.core.TLVTree;
import com.android.server.security.trustcircle.tlv.core.TLVTree.TLVRootTree;
import com.android.server.security.trustcircle.tlv.tree.AuthPkInfo;
import com.android.server.security.trustcircle.tlv.tree.StatesInfo;
import com.android.server.security.trustcircle.tlv.tree.UpdateIndexInfo;
import com.android.server.security.trustcircle.utils.ByteUtil;
import com.android.server.security.trustcircle.utils.LogHelper;
import com.android.server.security.trustcircle.utils.Status.ExceptionStep;
import com.android.server.security.trustcircle.utils.Status.LifeCycleStauts;
import com.android.server.security.trustcircle.utils.Status.TCIS_Result;
import com.android.server.security.trustcircle.utils.Status.UpdateStauts;
import com.android.server.security.trustcircle.utils.Utils;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class LifeCycleProcessor {
    public static final long INVALID_USER_ID = -1;
    private static final String KEY_TCIS_STATUS = "tcis_error_step";
    public static final byte LOGINED = (byte) 1;
    private static final String SERVICE_NAME = "com.huawei.trustcircle.tcis.TCUpdateService";
    public static final String TAG = LifeCycleProcessor.class.getSimpleName();
    private static final String TCIS_PKG_NAME = "com.huawei.trustcircle";
    private static volatile long currentUserId = -1;

    public static class UsersState {
        private int state;
        private long userHandle;

        public UsersState(long userHandle, int state) {
            this.userHandle = userHandle;
            this.state = state;
        }

        public long getUserId() {
            return this.userHandle;
        }

        public void setUserHandle(int userHandle) {
            this.userHandle = (long) userHandle;
        }

        public int getState() {
            return this.state;
        }

        public void setState(int state) {
            this.state = state;
        }

        public String getString() {
            short status = (short) this.state;
            StringBuffer sb = new StringBuffer();
            sb.append(ByteUtil.long2StrictHexString(this.userHandle));
            sb.append(ByteUtil.short2HexString(status));
            return sb.toString();
        }
    }

    public static void updateTime(long time) {
        NetworkChangeReceiver.updateTime(time);
    }

    public static long initAndroidUser(int userHandle) {
        CMD_INIT_REQ reqTLV = new CMD_INIT_REQ();
        reqTLV.userHandle.setTLVStruct(Integer.valueOf(userHandle));
        TLVResult<?> result = sendCmd(reqTLV);
        TLVTree respTree = result.getResultTLV();
        int resultCode = result.getResultCode();
        long userID = -1;
        boolean z = true;
        if (respTree != null && (respTree instanceof RET_INIT_REQ) && resultCode == TCIS_Result.SUCCESS.value()) {
            RET_INIT_REQ respTLV = (RET_INIT_REQ) respTree;
            Byte[] loginState = (Byte[]) respTLV.loginState.getTLVStruct();
            if (loginState != null && loginState.length == 1 && loginState[0].byteValue() == (byte) 1) {
                userID = ((Long) respTLV.userID.getTLVStruct()).longValue();
            }
        }
        if (userID == -1) {
            z = false;
        }
        TcisLifeCycleDispatcher.setIotEnable(z);
        updateExceptionStepOfCurrentUser(resultCode, ExceptionStep.SWITCH_USER, false);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("initAndroidUser-resultCode: ");
        stringBuilder.append(resultCode);
        LogHelper.d(str, stringBuilder.toString());
        return userID;
    }

    public static Bundle getTcisInfo() {
        TLVResult<?> result = sendCmd(new CMD_GET_TCIS_ID());
        TLVTree respTree = result.getResultTLV();
        int resultCode = result.getResultCode();
        Bundle tcisInfo = null;
        if (respTree != null && (respTree instanceof RET_GET_TCIS_ID) && resultCode == TCIS_Result.SUCCESS.value()) {
            RET_GET_TCIS_ID respTLV = (RET_GET_TCIS_ID) respTree;
            String tcisIDString = ByteUtil.byteArray2ServerHexString((Byte[]) respTLV.tcisID.getTLVStruct());
            short taVersion = ((Short) respTLV.TAVersion.getTLVStruct()).shortValue();
            tcisInfo = new Bundle();
            tcisInfo.putString("tcisID", tcisIDString);
            tcisInfo.putShort("TAVersion", taVersion);
            long hwUserId = getLoginedUserID();
            tcisInfo.putString("hwUserId", hwUserId <= 0 ? null : String.valueOf(hwUserId));
        }
        return tcisInfo;
    }

    public static boolean checkNeedRegister(int serverRegisterStatus, long userID) {
        return serverRegisterStatus == LifeCycleStauts.NOT_REGISTER.ordinal() || checkTARegisterStatus(userID) == LifeCycleStauts.NOT_REGISTER.ordinal();
    }

    public static boolean checkUserIDLogined(long userID) {
        if (getLoginedUserID() == userID) {
            return true;
        }
        return false;
    }

    public static long getLoginedUserID() {
        TLVResult<?> result = sendCmd(new CMD_GET_LOGIN_STATUS());
        TLVTree respTree = result.getResultTLV();
        int resultCode = result.getResultCode();
        long userID = -1;
        if (respTree != null && (respTree instanceof RET_GET_LOGIN_STATUS) && resultCode == TCIS_Result.SUCCESS.value()) {
            userID = ((Long) ((RET_GET_LOGIN_STATUS) respTree).userID.getTLVStruct()).longValue();
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getLoginedUser-resultCode: ");
        stringBuilder.append(resultCode);
        LogHelper.d(str, stringBuilder.toString());
        return userID;
    }

    public static int checkTARegisterStatus(long userID) {
        CMD_CHECK_REG_STATUS reqTLV = new CMD_CHECK_REG_STATUS();
        reqTLV.userID.setTLVStruct(Long.valueOf(userID));
        int resultCode = sendCmd(reqTLV).getResultCode();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("loginServerRequest-TARegisterStatus: ");
        stringBuilder.append(resultCode);
        LogHelper.d(str, stringBuilder.toString());
        if (resultCode == TCIS_Result.SUCCESS.value()) {
            return LifeCycleStauts.REGISTERED.ordinal();
        }
        return LifeCycleStauts.NOT_REGISTER.ordinal();
    }

    public static boolean register(long userID, String sessionID) {
        TcisLifeCycleDispatcher.setIotEnable(false);
        currentUserId = userID;
        boolean globalKeyID = false;
        CMD_REG_REQ reqTLV = new CMD_REG_REQ();
        reqTLV.userID.setTLVStruct(Long.valueOf(userID));
        reqTLV.sessionID.setTLVStruct(ByteUtil.hexString2ByteArray(sessionID));
        TLVResult<?> result = sendCmd(reqTLV);
        TLVTree respTree = result.getResultTLV();
        int resultCode = result.getResultCode();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("register-resultCode: ");
        stringBuilder.append(resultCode);
        LogHelper.d(str, stringBuilder.toString());
        int globalKeyID2 = -1;
        short authKeyAlgoEncode = (short) -1;
        String regAuthKeyData = null;
        String regAuthKeyDataSign = null;
        String clientChallenge = null;
        if (respTree != null && (respTree instanceof RET_REG_REQ) && resultCode == TCIS_Result.SUCCESS.value()) {
            RET_REG_REQ respTLV = (RET_REG_REQ) respTree;
            globalKeyID = true;
            globalKeyID2 = ((Integer) respTLV.glogbalKeyID.getTLVStruct()).intValue();
            authKeyAlgoEncode = ((Short) respTLV.authKeyAlgoEncode.getTLVStruct()).shortValue();
            regAuthKeyData = ByteUtil.byteArray2ServerHexString(respTLV.regAuthKeyData.encapsulate());
            regAuthKeyDataSign = respTLV.regAuthKeyDataSign.byteArray2ServerHexString();
            clientChallenge = respTLV.clientChallenge.byteArray2ServerHexString();
        }
        boolean valid = globalKeyID;
        if (onRegisterResponse(resultCode, globalKeyID2, authKeyAlgoEncode, regAuthKeyData, regAuthKeyDataSign, clientChallenge) && valid) {
            return true;
        }
        return false;
    }

    public static boolean onRegisterResponse(int resultCode, int globalKeyID, int authKeyAlgoType, String regAuthKeyData, String regAuthKeyDataSign, String clientChallenge) {
        updateExceptionStepOfCurrentUser(resultCode, ExceptionStep.REG, false);
        try {
            if (CallbackManager.getInstance().isILifeCycleCallbackValid()) {
                CallbackManager.getInstance().getILifeCycleCallback().onRegisterResponse(resultCode, globalKeyID, (short) authKeyAlgoType, regAuthKeyData, regAuthKeyDataSign, clientChallenge);
                return true;
            }
            CallbackManager.getInstance().unregisterILifeCycleCallback();
            return false;
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("RemoteException in onRegisterResponse: ");
            stringBuilder.append(e.getMessage());
            LogHelper.e(str, stringBuilder.toString());
            CallbackManager.getInstance().unregisterILifeCycleCallback();
            return false;
        }
    }

    public static boolean finalRegister(String authPKInfo, String authPKInfoSign, String updateIndexInfo, String updateIndexSignature) {
        boolean valid = false;
        TLVTree authPkInfoTree = TLVEngine.decodeTLV(ByteUtil.serverHexString2ByteArray(authPKInfo));
        Byte[] authPKInfoSignBytes = ByteUtil.serverHexString2ByteArray(authPKInfoSign);
        TLVTree updateIndexInfoTree = TLVEngine.decodeTLV(ByteUtil.serverHexString2ByteArray(updateIndexInfo));
        Byte[] updateIndexSignBytes = ByteUtil.serverHexString2ByteArray(updateIndexSignature);
        boolean z = false;
        if ((authPkInfoTree instanceof AuthPkInfo) && (updateIndexInfoTree instanceof UpdateIndexInfo)) {
            CMD_REG_RESULT reqTLV = new CMD_REG_RESULT();
            reqTLV.authPkInfo.setTLVStruct(authPkInfoTree);
            reqTLV.authPKInfoSign.setTLVStruct(authPKInfoSignBytes);
            reqTLV.updateIndexInfo.setTLVStruct(updateIndexInfoTree);
            reqTLV.updateIndexInfoSign.setTLVStruct(updateIndexSignBytes);
            int resultCode = sendCmd(reqTLV).getResultCode();
            if (resultCode == TCIS_Result.SUCCESS.value()) {
                LogHelper.i(TAG, "register tcis to TA successfully");
                valid = true;
            } else {
                LogHelper.w(TAG, "register tcis to TA failed");
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("finalRegister-resultCode: ");
            stringBuilder.append(resultCode);
            LogHelper.d(str, stringBuilder.toString());
            if (onFinalRegisterResult(resultCode) && valid) {
                z = true;
            }
            return z;
        }
        LogHelper.e(TAG, "error decode in finalRegister");
        onFinalRegisterResult(TCIS_Result.UNKNOWN_CMD.value());
        return false;
    }

    public static boolean onFinalRegisterResult(int resultCode) {
        TcisLifeCycleDispatcher.setIotEnableByResultCode(resultCode);
        updateExceptionStepOfCurrentUser(resultCode, ExceptionStep.REG, false);
        boolean result = false;
        try {
            if (CallbackManager.getInstance().isILifeCycleCallbackValid()) {
                CallbackManager.getInstance().getILifeCycleCallback().onFinalRegisterResult(resultCode);
                result = true;
            } else {
                result = false;
            }
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("RemoteException in onFinalRegisterResult: ");
            stringBuilder.append(e.getMessage());
            LogHelper.e(str, stringBuilder.toString());
            result = false;
        }
        CallbackManager.getInstance().unregisterILifeCycleCallback();
        return result;
    }

    public static boolean loginAndUpdate(long userID) {
        TcisLifeCycleDispatcher.setIotEnable(false);
        currentUserId = userID;
        boolean valid = false;
        TLVResult<?> result = updateCmd(userID);
        TLVTree respTree = result.getResultTLV();
        int resultCode = result.getResultCode();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("loginAndUpdate-resultCode: ");
        stringBuilder.append(resultCode);
        LogHelper.d(str, stringBuilder.toString());
        int indexVersion = -1;
        String clientChallenge = null;
        if (respTree != null && (respTree instanceof RET_LOGIN_REQ) && resultCode == TCIS_Result.SUCCESS.value()) {
            RET_LOGIN_REQ respTLV = (RET_LOGIN_REQ) respTree;
            valid = true;
            indexVersion = ((Integer) respTLV.indexVersion.getTLVStruct()).intValue();
            clientChallenge = respTLV.clientChallenge.byteArray2ServerHexString();
        }
        if (onLoginResponse(resultCode, indexVersion, clientChallenge) && valid) {
            return true;
        }
        return false;
    }

    private static TLVResult<?> updateCmd(long userID) {
        CMD_LOGIN_REQ reqTLV = new CMD_LOGIN_REQ();
        reqTLV.userID.setTLVStruct(Long.valueOf(userID));
        return sendCmd(reqTLV);
    }

    public static boolean onLoginResponse(int resultCode, int indexVersion, String clientChallenge) {
        TcisLifeCycleDispatcher.setIotEnableByResultCode(resultCode);
        updateExceptionStepOfCurrentUser(resultCode, ExceptionStep.LOGIN, false);
        try {
            if (CallbackManager.getInstance().isILifeCycleCallbackValid()) {
                CallbackManager.getInstance().getILifeCycleCallback().onLoginResponse(resultCode, indexVersion, clientChallenge);
                return true;
            }
            CallbackManager.getInstance().unregisterILifeCycleCallback();
            return false;
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("RemoteException in onLoginResponse: ");
            stringBuilder.append(e.getMessage());
            LogHelper.e(str, stringBuilder.toString());
            CallbackManager.getInstance().unregisterILifeCycleCallback();
            return false;
        }
    }

    public static boolean updateOnly(long userID) {
        boolean valid = false;
        currentUserId = userID;
        TLVResult<?> result = updateCmd(userID);
        TLVTree respTree = result.getResultTLV();
        int resultCode = result.getResultCode();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateServerRequest-updateOnly-resultCode: ");
        stringBuilder.append(resultCode);
        LogHelper.d(str, stringBuilder.toString());
        int indexVersion = -1;
        String clientChallenge = null;
        if (respTree != null && (respTree instanceof RET_LOGIN_REQ) && resultCode == TCIS_Result.SUCCESS.value()) {
            RET_LOGIN_REQ respTLV = (RET_LOGIN_REQ) respTree;
            valid = true;
            indexVersion = ((Integer) respTLV.indexVersion.getTLVStruct()).intValue();
            clientChallenge = respTLV.clientChallenge.byteArray2ServerHexString();
        }
        return onUpdateResponse(resultCode, indexVersion, clientChallenge) && valid;
    }

    public static boolean onUpdateResponse(int resultCode, int indexVersion, String clientChallenge) {
        updateExceptionStepOfCurrentUser(resultCode, ExceptionStep.UPDATE, false);
        try {
            if (CallbackManager.getInstance().isILifeCycleCallbackValid()) {
                CallbackManager.getInstance().getILifeCycleCallback().onUpdateResponse(resultCode, indexVersion, clientChallenge);
                return true;
            }
            CallbackManager.getInstance().unregisterILifeCycleCallback();
            return false;
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("RemoteException in onUpdateResponse: ");
            stringBuilder.append(e.getMessage());
            LogHelper.e(str, stringBuilder.toString());
            CallbackManager.getInstance().unregisterILifeCycleCallback();
            return false;
        }
    }

    public static boolean finalLogin(int updateResult, String updateIndexInfo, String updateIndexSignature) {
        boolean valid = false;
        int resultCode = TCIS_Result.BAD_PARAM.value();
        if (updateResult == UpdateStauts.NO_NEED_UPDATE.ordinal()) {
            LogHelper.i(TAG, "finalLogin-nothing to update");
            resultCode = TCIS_Result.SUCCESS.value();
            valid = true;
        } else if (TextUtils.isEmpty(updateIndexInfo) || TextUtils.isEmpty(updateIndexSignature)) {
            LogHelper.w(TAG, "error:finalLogin-update data is not complete");
            resultCode = TCIS_Result.BAD_PARAM.value();
        } else {
            LogHelper.i(TAG, "finalLogin-need to update tcis data");
            TLVTree updateIndexInfoTree = TLVEngine.decodeTLV(ByteUtil.serverHexString2ByteArray(updateIndexInfo));
            if (updateIndexInfoTree instanceof UpdateIndexInfo) {
                Byte[] updateIndexSignBytes = ByteUtil.serverHexString2ByteArray(updateIndexSignature);
                CMD_LOGIN_RESULT_UPDATE reqTLV = new CMD_LOGIN_RESULT_UPDATE();
                reqTLV.updateIndexInfo.setTLVStruct(updateIndexInfoTree);
                reqTLV.updateIndexInfoSign.setTLVStruct(updateIndexSignBytes);
                resultCode = sendCmd(reqTLV).getResultCode();
                if (resultCode == TCIS_Result.SUCCESS.value()) {
                    valid = true;
                }
            } else {
                LogHelper.e(TAG, "error decode in finalLogin");
                onFinalLoginResult(TCIS_Result.BAD_PARAM.value());
                return false;
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("finalLogin-resultCode: ");
        stringBuilder.append(resultCode);
        LogHelper.d(str, stringBuilder.toString());
        boolean z = onFinalLoginResult(resultCode) && valid;
        return z;
    }

    public static boolean onFinalLoginResult(int resultCode) {
        updateExceptionStepOfCurrentUser(resultCode, ExceptionStep.LOGIN, false);
        boolean result = false;
        try {
            if (CallbackManager.getInstance().isILifeCycleCallbackValid()) {
                CallbackManager.getInstance().getILifeCycleCallback().onFinalLoginResult(resultCode);
                result = true;
            } else {
                result = false;
            }
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("RemoteException in onFinalLoginResult: ");
            stringBuilder.append(e.getMessage());
            LogHelper.e(str, stringBuilder.toString());
            result = false;
        }
        CallbackManager.getInstance().unregisterILifeCycleCallback();
        return result;
    }

    public static boolean cancelRegOrLogin(State state, long userID) {
        if (state == null) {
            LogHelper.w(TAG, "nothing to cancel");
            return false;
        } else if (RegisteringState.class == state.getClass()) {
            return cancelReg(userID);
        } else {
            if (LoginingState.class == state.getClass()) {
                return cancelLogin(userID);
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unknown cancel state: ");
            stringBuilder.append(state.getName());
            LogHelper.w(str, stringBuilder.toString());
            return false;
        }
    }

    private static boolean cancelReg(long userID) {
        TcisLifeCycleDispatcher.setIotEnable(false);
        boolean valid = false;
        CMD_REG_CANCEL reqTLV = new CMD_REG_CANCEL();
        reqTLV.userID.setTLVStruct(Long.valueOf(userID));
        int resultCode = sendCmd(reqTLV).getResultCode();
        if (resultCode == TCIS_Result.SUCCESS.value()) {
            currentUserId = -1;
            valid = true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("cancelRegister-resultCode: ");
        stringBuilder.append(resultCode);
        LogHelper.d(str, stringBuilder.toString());
        CallbackManager.getInstance().unregisterILifeCycleCallback();
        return valid;
    }

    private static boolean cancelLogin(long userID) {
        TcisLifeCycleDispatcher.setIotEnable(false);
        boolean valid = false;
        CMD_LOGIN_CANCEL reqTLV = new CMD_LOGIN_CANCEL();
        reqTLV.userID.setTLVStruct(Long.valueOf(userID));
        int resultCode = sendCmd(reqTLV).getResultCode();
        if (resultCode == TCIS_Result.SUCCESS.value()) {
            currentUserId = -1;
            valid = true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("cancelLogin-resultCode: ");
        stringBuilder.append(resultCode);
        LogHelper.d(str, stringBuilder.toString());
        CallbackManager.getInstance().unregisterILifeCycleCallback();
        return valid;
    }

    public static boolean logout(long userID) {
        boolean valid = false;
        CMD_LOGOUT_REQ reqTLV = new CMD_LOGOUT_REQ();
        reqTLV.userID.setTLVStruct(Long.valueOf(userID));
        int resultCode = sendCmd(reqTLV).getResultCode();
        if (resultCode == TCIS_Result.SUCCESS.value()) {
            currentUserId = -1;
            valid = true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("logout-resultCode: ");
        stringBuilder.append(resultCode);
        LogHelper.d(str, stringBuilder.toString());
        return onLogoutResult(resultCode) && valid;
    }

    public static boolean onLogoutResult(int resultCode) {
        TcisLifeCycleDispatcher.setIotEnableByResultCodeReverse(resultCode);
        updateExceptionStepOfCurrentUser(resultCode, ExceptionStep.LOGOUT, false);
        try {
            if (CallbackManager.getInstance().isILifeCycleCallbackValid()) {
                CallbackManager.getInstance().getILifeCycleCallback().onLogoutResult(resultCode);
                CallbackManager.getInstance().unregisterILifeCycleCallback();
                return true;
            }
            CallbackManager.getInstance().unregisterILifeCycleCallback();
            return false;
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("RemoteException in onLogoutResult: ");
            stringBuilder.append(e.getMessage());
            LogHelper.e(str, stringBuilder.toString());
            CallbackManager.getInstance().unregisterILifeCycleCallback();
            return false;
        }
    }

    public static boolean unregister(long userID) {
        boolean valid = false;
        currentUserId = userID;
        CMD_UNREG_REQ reqTLV = new CMD_UNREG_REQ();
        reqTLV.userID.setTLVStruct(Long.valueOf(userID));
        int resultCode = sendCmd(reqTLV).getResultCode();
        if (resultCode == TCIS_Result.SUCCESS.value()) {
            valid = true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("unregister-resultCode: ");
        stringBuilder.append(resultCode);
        LogHelper.d(str, stringBuilder.toString());
        return onUnregisterResult(resultCode) && valid;
    }

    public static boolean onUnregisterResult(int resultCode) {
        TcisLifeCycleDispatcher.setIotEnableByResultCodeReverse(resultCode);
        updateExceptionStepOfCurrentUser(resultCode, ExceptionStep.UNREG, true);
        if (resultCode == TCIS_Result.SUCCESS.value()) {
            currentUserId = -1;
        }
        try {
            if (CallbackManager.getInstance().isILifeCycleCallbackValid()) {
                CallbackManager.getInstance().getILifeCycleCallback().onUnregisterResult(resultCode);
                CallbackManager.getInstance().unregisterILifeCycleCallback();
                return true;
            }
            CallbackManager.getInstance().unregisterILifeCycleCallback();
            return false;
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("RemoteException in onUnregisterResult: ");
            stringBuilder.append(e.getMessage());
            LogHelper.e(str, stringBuilder.toString());
            CallbackManager.getInstance().unregisterILifeCycleCallback();
            return false;
        }
    }

    public static <T extends TLVRootTree> TLVResult<T> sendCmd(T reqTLV) {
        return TLVEngine.decodeCmdTLV(processCmd(TLVEngine.encode2CmdTLV(reqTLV)));
    }

    private static byte[] processCmd(byte[] request) {
        return TcisJNI.processCmd(null, request);
    }

    public static ComponentName startTcisService(Context context) {
        if (context == null) {
            LogHelper.e(TAG, "error- startTcisService: Context is null");
            return null;
        }
        Intent serviceIntent = new Intent();
        serviceIntent.setComponent(new ComponentName(TCIS_PKG_NAME, SERVICE_NAME));
        serviceIntent.putExtra("taLoginedUserId", getLoginedUserID());
        return context.startServiceAsUser(serviceIntent, UserHandle.of(Utils.getCurrentUserId()));
    }

    public static void updateExceptionStepOfCurrentUser(int resultCode, ExceptionStep step, boolean delete) {
        if (step != null && currentUserId != -1) {
            UsersState newState;
            StringBuffer sb = new StringBuffer();
            if (resultCode != TCIS_Result.SUCCESS.value()) {
                newState = new UsersState(currentUserId, step.ordinal());
            } else {
                newState = new UsersState(currentUserId, ExceptionStep.NO_EXCEPTION.ordinal());
            }
            UsersState[] states = getUsersState();
            int number = states.length;
            boolean hasData = false;
            for (int j = 0; j < states.length; j++) {
                if (states[j].getUserId() == currentUserId) {
                    hasData = true;
                    if (delete) {
                    } else {
                        states[j] = newState;
                    }
                }
                sb.append(states[j].getString());
            }
            if (!(hasData || delete)) {
                sb.append(newState.getString());
                number++;
            }
            Utils.setProperty(TcisLifeCycleDispatcher.getInstance().getContext(), KEY_TCIS_STATUS, ByteUtil.byteArrayOri2HexString(TLVEngine.encode2TLV(new DATA_TCIS_ERROR_STEP((short) number, new StatesInfo(ByteUtil.hexString2ByteArray(sb.toString()))))));
        }
    }

    public static String getExceptionSteps() {
        return Utils.getProperty(TcisLifeCycleDispatcher.getInstance().getContext(), KEY_TCIS_STATUS);
    }

    public static UsersState[] getUsersState() {
        int i = 0;
        UsersState[] userInfos = new UsersState[0];
        TLVTree oriData = TLVEngine.decodeTLV(ByteUtil.hexString2byteArray(getExceptionSteps()));
        if (oriData != null && (oriData instanceof DATA_TCIS_ERROR_STEP)) {
            DATA_TCIS_ERROR_STEP data = (DATA_TCIS_ERROR_STEP) oriData;
            int number = ((Short) data.numbers.getTLVStruct()).shortValue();
            byte[] infos = ByteUtil.unboxByteArray((Byte[]) ((StatesInfo) data.info.getTLVStruct()).infos.getTLVStruct());
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("tcis number: ");
            stringBuilder.append(number);
            LogHelper.i(str, stringBuilder.toString());
            if (number != 0 && infos.length == 10 * number) {
                userInfos = new UsersState[number];
                ByteBuffer buffer = ByteBuffer.wrap(infos);
                buffer.order(ByteOrder.BIG_ENDIAN);
                while (buffer.remaining() >= 10 && i <= number) {
                    userInfos[i] = new UsersState(buffer.getLong(), buffer.getShort());
                    i++;
                }
            }
        }
        return userInfos;
    }

    public static int getExceptionStepOfCurrentUserId() {
        UsersState[] states = getUsersState();
        for (int j = 0; j < states.length; j++) {
            if (states[j].getUserId() == currentUserId) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("tcis user ");
                stringBuilder.append(currentUserId);
                stringBuilder.append(" ,exception step: ");
                stringBuilder.append(states[j].getState());
                LogHelper.i(str, stringBuilder.toString());
                return states[j].getState();
            }
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("no exception data in tcis user ");
        stringBuilder2.append(currentUserId);
        LogHelper.w(str2, stringBuilder2.toString());
        return ExceptionStep.NO_EXCEPTION.ordinal();
    }
}
