package com.android.server.security.trustcircle.utils;

import android.content.Context;
import com.android.server.security.trustcircle.auth.AuthPara.InitAuthInfo;
import com.android.server.security.trustcircle.auth.AuthPara.OnAuthAckInfo;
import com.android.server.security.trustcircle.auth.AuthPara.OnAuthSyncAckInfo;
import com.android.server.security.trustcircle.auth.AuthPara.OnAuthSyncInfo;
import com.android.server.security.trustcircle.auth.AuthPara.RecAckInfo;
import com.android.server.security.trustcircle.auth.AuthPara.RecAuthAckInfo;
import com.android.server.security.trustcircle.auth.AuthPara.RecAuthInfo;
import com.android.server.security.trustcircle.auth.AuthPara.ReqPkInfo;
import com.android.server.security.trustcircle.auth.AuthPara.RespPkInfo;
import com.android.server.security.trustcircle.auth.IOTController.KaInfoRequest;
import com.android.server.security.trustcircle.auth.IOTController.KaInfoResponse;
import com.android.server.security.trustcircle.jni.TcisJNI;
import com.android.server.security.trustcircle.tlv.command.auth.CMD_AUTH_ACK_RECV;
import com.android.server.security.trustcircle.tlv.command.auth.CMD_AUTH_CANCEL;
import com.android.server.security.trustcircle.tlv.command.auth.CMD_AUTH_MASTER_RECV_KEY;
import com.android.server.security.trustcircle.tlv.command.auth.CMD_AUTH_SLAVE_RECV_KEY;
import com.android.server.security.trustcircle.tlv.command.auth.CMD_AUTH_SYNC;
import com.android.server.security.trustcircle.tlv.command.auth.CMD_AUTH_SYNC_ACK_RECV;
import com.android.server.security.trustcircle.tlv.command.auth.CMD_AUTH_SYNC_RECV;
import com.android.server.security.trustcircle.tlv.command.auth.RET_AUTH_ACK_RECV;
import com.android.server.security.trustcircle.tlv.command.auth.RET_AUTH_SYNC;
import com.android.server.security.trustcircle.tlv.command.auth.RET_AUTH_SYNC_ACK_RECV;
import com.android.server.security.trustcircle.tlv.command.auth.RET_AUTH_SYNC_RECV;
import com.android.server.security.trustcircle.tlv.command.ka.CMD_KA;
import com.android.server.security.trustcircle.tlv.command.ka.RET_KA;
import com.android.server.security.trustcircle.tlv.command.query.CMD_GET_PK;
import com.android.server.security.trustcircle.tlv.command.query.RET_GET_PK;
import com.android.server.security.trustcircle.tlv.core.TLVEngine;
import com.android.server.security.trustcircle.tlv.core.TLVEngine.TLVResult;
import com.android.server.security.trustcircle.tlv.core.TLVTree;
import com.android.server.security.trustcircle.tlv.tree.AuthData;
import com.android.server.security.trustcircle.tlv.tree.AuthInfo;
import com.android.server.security.trustcircle.tlv.tree.AuthPkInfo;
import com.android.server.security.trustcircle.tlv.tree.AuthSyncData;
import com.android.server.security.trustcircle.tlv.tree.Cert;
import com.android.server.security.trustcircle.tlv.tree.KaInfo;
import com.android.server.security.trustcircle.utils.Status.TCIS_Result;

public class AuthUtils {
    private static final int FATS_AUTH = 1;
    private static final String TAG = "AuthUtils";

    public static OnAuthSyncInfo processAuthSync(InitAuthInfo info) {
        InitAuthInfo initAuthInfo = info;
        AuthInfo authTLV = new AuthInfo();
        authTLV.authType.setTLVStruct(Integer.valueOf(initAuthInfo.mAuthType));
        authTLV.authID.setTLVStruct(Long.valueOf(initAuthInfo.mAuthID));
        authTLV.policy.setTLVStruct(Integer.valueOf(initAuthInfo.mPolicy));
        authTLV.userID.setTLVStruct(Long.valueOf(initAuthInfo.mUserID));
        authTLV.encryptedAESKey.setTLVStruct(ByteUtil.boxbyteArray(initAuthInfo.mAESTmpKey));
        CMD_AUTH_SYNC cmd = new CMD_AUTH_SYNC();
        cmd.authVersion.setTLVStruct(Integer.valueOf(initAuthInfo.mAuthVersion));
        cmd.authInfo.setTLVStruct(authTLV);
        TLVResult<?> tlvResult = TLVEngine.decodeCmdTLV(TcisJNI.processCmd(null, TLVEngine.encode2CmdTLV(cmd)));
        int resultCode = tlvResult.getResultCode();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("processAuthSync result, resultCode: ");
        stringBuilder.append(Integer.toHexString(resultCode));
        LogHelper.d(str, stringBuilder.toString());
        TLVTree ret = tlvResult.getResultTLV();
        if (resultCode != 0) {
            return new OnAuthSyncInfo(initAuthInfo.mAuthID, new byte[0], 1, (short) 1, 1, (short) 1, new byte[0], new byte[0], resultCode);
        } else if (ret instanceof RET_AUTH_SYNC) {
            RET_AUTH_SYNC retInstance = (RET_AUTH_SYNC) ret;
            long authID = ((Long) retInstance.authID.getTLVStruct()).longValue();
            short taVersion = ((Short) retInstance.TAVersion.getTLVStruct()).shortValue();
            AuthData authData = (AuthData) retInstance.authData.getTLVStruct();
            Cert cert = (Cert) authData.cert.getTLVStruct();
            short authKeyAlgoEncode = ((Short) cert.authKeyAlgoEncode.getTLVStruct()).shortValue();
            byte[] authPKInfoSign = ByteUtil.unboxByteArray((Byte[]) cert.authPKInfoSign.getTLVStruct());
            byte[] authPkInfoByte = ByteUtil.unboxByteArray(cert.authPkInfo.encapsulate());
            AuthSyncData authSyncData = (AuthSyncData) authData.authSyncData.getTLVStruct();
            return new OnAuthSyncInfo(authID, ByteUtil.unboxByteArray((Byte[]) authSyncData.tcisID.getTLVStruct()), ((Integer) authSyncData.indexVersion.getTLVStruct()).intValue(), taVersion, ByteUtil.byteArrayToLongDirect((Byte[]) authSyncData.nonce.getTLVStruct()), authKeyAlgoEncode, authPkInfoByte, authPKInfoSign, resultCode);
        } else {
            int i = resultCode;
            return new OnAuthSyncInfo(initAuthInfo.mAuthID, new byte[0], 1, (short) 1, 1, (short) 1, new byte[0], new byte[0], 2046820353);
        }
    }

    public static OnAuthSyncAckInfo processAuthSyncRec(RecAuthInfo info) {
        RecAuthInfo recAuthInfo = info;
        Cert certTLV = new Cert();
        AuthPkInfo authPkInfo;
        if (recAuthInfo.mPolicy != 1) {
            authPkInfo = new AuthPkInfo();
            authPkInfo.indexVersion.setTLVStruct(Short.valueOf((short) 0));
            authPkInfo.userID.setTLVStruct(Long.valueOf(0));
            authPkInfo.tcisID.setTLVStruct(ByteUtil.boxbyteArray(new byte[10]));
            authPkInfo.authPK.setTLVStruct(ByteUtil.boxbyteArray(new byte[64]));
            certTLV.authKeyAlgoEncode.setTLVStruct(Short.valueOf((short) 0));
            certTLV.authPkInfo.setTLVStruct(authPkInfo);
            certTLV.authPKInfoSign.setTLVStruct(ByteUtil.boxbyteArray(new byte[64]));
        } else {
            authPkInfo = (AuthPkInfo) TLVEngine.decodeTLV(recAuthInfo.mAuthKeyInfo);
            certTLV.authKeyAlgoEncode.setTLVStruct(Short.valueOf(recAuthInfo.mAuthKeyAlgoType));
            certTLV.authPkInfo.setTLVStruct(authPkInfo);
            certTLV.authPKInfoSign.setTLVStruct(ByteUtil.boxbyteArray(recAuthInfo.mAuthKeyInfoSign));
        }
        AuthSyncData authSyncTLV = new AuthSyncData();
        authSyncTLV.tcisID.setTLVStruct(ByteUtil.boxbyteArray(recAuthInfo.mTcisId));
        authSyncTLV.indexVersion.setTLVStruct(Integer.valueOf(recAuthInfo.mPkVersion));
        authSyncTLV.nonce.setTLVStruct(ByteUtil.longToByteArray(recAuthInfo.mNonce));
        AuthData authDataTLV = new AuthData();
        authDataTLV.cert.setTLVStruct(certTLV);
        authDataTLV.authSyncData.setTLVStruct(authSyncTLV);
        AuthInfo authInfo = new AuthInfo();
        authInfo.authType.setTLVStruct(Integer.valueOf(recAuthInfo.mAuthType));
        authInfo.authID.setTLVStruct(Long.valueOf(recAuthInfo.mAuthID));
        authInfo.policy.setTLVStruct(Integer.valueOf(recAuthInfo.mPolicy));
        authInfo.userID.setTLVStruct(Long.valueOf(recAuthInfo.mUserID));
        authInfo.encryptedAESKey.setTLVStruct(ByteUtil.boxbyteArray(recAuthInfo.mAESTmpKey));
        CMD_AUTH_SYNC_RECV cmd = new CMD_AUTH_SYNC_RECV();
        cmd.authVersion.setTLVStruct(Integer.valueOf(recAuthInfo.mAuthVersion));
        cmd.authInfo.setTLVStruct(authInfo);
        cmd.authData.setTLVStruct(authDataTLV);
        cmd.TAVersion.setTLVStruct(Short.valueOf(recAuthInfo.mTAVersion));
        byte[] result = TcisJNI.processCmd(null, TLVEngine.encode2CmdTLV(cmd));
        TLVResult<?> tlvResult = TLVEngine.decodeCmdTLV(result);
        int resultCode = tlvResult.getResultCode();
        if (isNeedRequestPk(result)) {
            resultCode = 1;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("processAuthSyncRec result, resultCode: ");
        stringBuilder.append(Integer.toHexString(resultCode));
        LogHelper.d(str, stringBuilder.toString());
        TLVTree ret = tlvResult.getResultTLV();
        if (resultCode != 0) {
            return new OnAuthSyncAckInfo(recAuthInfo.mAuthID, new byte[0], 1, 1, new byte[0], (short) 1, new byte[0], new byte[0], resultCode);
        } else if (ret instanceof RET_AUTH_SYNC_RECV) {
            RET_AUTH_SYNC_RECV retInstance = (RET_AUTH_SYNC_RECV) ret;
            long authID = ((Long) retInstance.authID.getTLVStruct()).longValue();
            byte[] mac = ByteUtil.unboxByteArray((Byte[]) retInstance.mac.getTLVStruct());
            AuthData authData = (AuthData) retInstance.authData.getTLVStruct();
            Cert certRet = (Cert) authData.cert.getTLVStruct();
            short authKeyAlgoEncode = ((Short) certRet.authKeyAlgoEncode.getTLVStruct()).shortValue();
            byte[] authPKInfoSign = ByteUtil.unboxByteArray((Byte[]) certRet.authPKInfoSign.getTLVStruct());
            byte[] authPkInfoByte = ByteUtil.unboxByteArray(certRet.authPkInfo.encapsulate());
            AuthSyncData authSyncData = (AuthSyncData) authData.authSyncData.getTLVStruct();
            return new OnAuthSyncAckInfo(authID, ByteUtil.unboxByteArray((Byte[]) authSyncData.tcisID.getTLVStruct()), ((Integer) authSyncData.indexVersion.getTLVStruct()).intValue(), ByteUtil.byteArrayToLongDirect((Byte[]) authSyncData.nonce.getTLVStruct()), mac, authKeyAlgoEncode, authPkInfoByte, authPKInfoSign, resultCode);
        } else {
            return new OnAuthSyncAckInfo(recAuthInfo.mAuthID, new byte[0], 1, 1, new byte[0], (short) 1, new byte[0], new byte[0], 2046820353);
        }
    }

    public static OnAuthAckInfo processRecAuthSyncAck(RecAuthAckInfo info) {
        RecAuthAckInfo recAuthAckInfo = info;
        AuthPkInfo authPkInfo = (AuthPkInfo) TLVEngine.decodeTLV(recAuthAckInfo.mAuthKeyInfoSlave);
        Cert cert = new Cert();
        cert.authPkInfo.setTLVStruct(authPkInfo);
        cert.authKeyAlgoEncode.setTLVStruct(Short.valueOf(recAuthAckInfo.mAuthKeyAlgoTypeSlave));
        cert.authPKInfoSign.setTLVStruct(ByteUtil.boxbyteArray(recAuthAckInfo.mAuthKeyInfoSignSlave));
        AuthSyncData authSyncData = new AuthSyncData();
        authSyncData.tcisID.setTLVStruct(ByteUtil.boxbyteArray(recAuthAckInfo.mTcisIDSlave));
        authSyncData.nonce.setTLVStruct(ByteUtil.longToByteArray(recAuthAckInfo.mNonceSlave));
        authSyncData.indexVersion.setTLVStruct(Integer.valueOf(recAuthAckInfo.mPkVersionSlave));
        AuthData authData = new AuthData();
        authData.cert.setTLVStruct(cert);
        authData.authSyncData.setTLVStruct(authSyncData);
        CMD_AUTH_SYNC_ACK_RECV cmd = new CMD_AUTH_SYNC_ACK_RECV();
        cmd.authID.setTLVStruct(Long.valueOf(recAuthAckInfo.mAuthID));
        cmd.mac.setTLVStruct(ByteUtil.boxbyteArray(recAuthAckInfo.mMacSlave));
        cmd.authData.setTLVStruct(authData);
        byte[] result = TcisJNI.processCmd(null, TLVEngine.encode2CmdTLV(cmd));
        TLVResult<?> tlvResult = TLVEngine.decodeCmdTLV(result);
        int resultCode = tlvResult.getResultCode();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("processRecAuthSyncAck result, resultCode: ");
        stringBuilder.append(Integer.toHexString(resultCode));
        LogHelper.d(str, stringBuilder.toString());
        if (isNeedRequestPk(result)) {
            resultCode = 1;
        }
        TLVTree ret = tlvResult.getResultTLV();
        if (resultCode != 0) {
            return new OnAuthAckInfo(recAuthAckInfo.mAuthID, new byte[0], new byte[0], new byte[0], resultCode);
        } else if (ret instanceof RET_AUTH_SYNC_ACK_RECV) {
            RET_AUTH_SYNC_ACK_RECV retInstance = (RET_AUTH_SYNC_ACK_RECV) ret;
            long authID = ((Long) retInstance.authID.getTLVStruct()).longValue();
            byte[] mac = ByteUtil.unboxByteArray((Byte[]) retInstance.mac.getTLVStruct());
            return new OnAuthAckInfo(authID, ByteUtil.unboxByteArray((Byte[]) retInstance.iv.getTLVStruct()), ByteUtil.unboxByteArray((Byte[]) retInstance.sessionKey.getTLVStruct()), mac, resultCode);
        } else {
            return new OnAuthAckInfo(recAuthAckInfo.mAuthID, new byte[0], new byte[0], new byte[0], 2046820353);
        }
    }

    public static OnAuthAckInfo processRecPkMaster(RespPkInfo info) {
        RespPkInfo respPkInfo = info;
        AuthPkInfo authPkInfo = (AuthPkInfo) TLVEngine.decodeTLV(respPkInfo.mAuthKeyData);
        Cert cert = new Cert();
        cert.authPkInfo.setTLVStruct(authPkInfo);
        cert.authKeyAlgoEncode.setTLVStruct(Short.valueOf(respPkInfo.mAuthKeyAlgoType));
        cert.authPKInfoSign.setTLVStruct(ByteUtil.boxbyteArray(respPkInfo.mAuthKeyDataSign));
        CMD_AUTH_MASTER_RECV_KEY cmd = new CMD_AUTH_MASTER_RECV_KEY();
        cmd.authID.setTLVStruct(Long.valueOf(respPkInfo.mAuthID));
        cmd.cert.setTLVStruct(cert);
        TLVResult<?> tlvResult = TLVEngine.decodeCmdTLV(TcisJNI.processCmd(null, TLVEngine.encode2CmdTLV(cmd)));
        int resultCode = tlvResult.getResultCode();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("processRecPkMaster result, resultCode: ");
        stringBuilder.append(Integer.toHexString(resultCode));
        LogHelper.d(str, stringBuilder.toString());
        TLVTree ret = tlvResult.getResultTLV();
        if (resultCode != 0) {
            return new OnAuthAckInfo(respPkInfo.mAuthID, new byte[0], new byte[0], new byte[0], resultCode);
        }
        if (!(ret instanceof RET_AUTH_SYNC_ACK_RECV)) {
            return new OnAuthAckInfo(respPkInfo.mAuthID, new byte[0], new byte[0], new byte[0], 2046820353);
        }
        RET_AUTH_SYNC_ACK_RECV retInstance = (RET_AUTH_SYNC_ACK_RECV) ret;
        byte[] sessionKey = ByteUtil.unboxByteArray((Byte[]) retInstance.sessionKey.getTLVStruct());
        return new OnAuthAckInfo(respPkInfo.mAuthID, ByteUtil.unboxByteArray((Byte[]) retInstance.iv.getTLVStruct()), sessionKey, ByteUtil.unboxByteArray((Byte[]) retInstance.mac.getTLVStruct()), resultCode);
    }

    public static OnAuthSyncAckInfo processRecPkSlave(RespPkInfo info) {
        RespPkInfo respPkInfo = info;
        AuthPkInfo authPkInfo = (AuthPkInfo) TLVEngine.decodeTLV(respPkInfo.mAuthKeyData);
        Cert cert = new Cert();
        cert.authPkInfo.setTLVStruct(authPkInfo);
        cert.authKeyAlgoEncode.setTLVStruct(Short.valueOf(respPkInfo.mAuthKeyAlgoType));
        cert.authPKInfoSign.setTLVStruct(ByteUtil.boxbyteArray(respPkInfo.mAuthKeyDataSign));
        CMD_AUTH_SLAVE_RECV_KEY cmd = new CMD_AUTH_SLAVE_RECV_KEY();
        cmd.authID.setTLVStruct(Long.valueOf(respPkInfo.mAuthID));
        cmd.cert.setTLVStruct(cert);
        TLVResult<?> tlvResult = TLVEngine.decodeCmdTLV(TcisJNI.processCmd(null, TLVEngine.encode2CmdTLV(cmd)));
        int resultCode = tlvResult.getResultCode();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("processRecPkSlave result, resultCode: ");
        stringBuilder.append(Integer.toHexString(resultCode));
        LogHelper.d(str, stringBuilder.toString());
        TLVTree ret = tlvResult.getResultTLV();
        if (resultCode != 0) {
            return new OnAuthSyncAckInfo(respPkInfo.mAuthID, new byte[0], 1, 1, new byte[0], (short) 1, new byte[0], new byte[0], resultCode);
        } else if (ret instanceof RET_AUTH_SYNC_RECV) {
            RET_AUTH_SYNC_RECV retInstance = (RET_AUTH_SYNC_RECV) ret;
            long authID = ((Long) retInstance.authID.getTLVStruct()).longValue();
            byte[] mac = ByteUtil.unboxByteArray((Byte[]) retInstance.mac.getTLVStruct());
            AuthData authData = (AuthData) retInstance.authData.getTLVStruct();
            Cert certRet = (Cert) authData.cert.getTLVStruct();
            short authKeyAlgoEncode = ((Short) certRet.authKeyAlgoEncode.getTLVStruct()).shortValue();
            byte[] authPKInfoSign = ByteUtil.unboxByteArray((Byte[]) certRet.authPKInfoSign.getTLVStruct());
            byte[] authPkInfoByte = ByteUtil.unboxByteArray(certRet.authPkInfo.encapsulate());
            AuthSyncData authSyncData = (AuthSyncData) authData.authSyncData.getTLVStruct();
            return new OnAuthSyncAckInfo(authID, ByteUtil.unboxByteArray((Byte[]) authSyncData.tcisID.getTLVStruct()), ((Integer) authSyncData.indexVersion.getTLVStruct()).intValue(), ByteUtil.byteArrayToLongDirect((Byte[]) authSyncData.nonce.getTLVStruct()), mac, authKeyAlgoEncode, authPkInfoByte, authPKInfoSign, resultCode);
        } else {
            int i = resultCode;
            return new OnAuthSyncAckInfo(respPkInfo.mAuthID, new byte[0], 1, 1, new byte[0], (short) 1, new byte[0], new byte[0], 2046820353);
        }
    }

    public static OnAuthAckInfo processAckRec(RecAckInfo info) {
        RecAckInfo recAckInfo = info;
        CMD_AUTH_ACK_RECV cmd = new CMD_AUTH_ACK_RECV();
        cmd.authID.setTLVStruct(Long.valueOf(recAckInfo.mAuthID));
        cmd.mac.setTLVStruct(ByteUtil.boxbyteArray(recAckInfo.mMAC));
        TLVResult<?> tlvResult = TLVEngine.decodeCmdTLV(TcisJNI.processCmd(null, TLVEngine.encode2CmdTLV(cmd)));
        int resultCode = tlvResult.getResultCode();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("processAckRec result, resultCode: ");
        stringBuilder.append(Integer.toHexString(resultCode));
        LogHelper.d(str, stringBuilder.toString());
        TLVTree ret = tlvResult.getResultTLV();
        if (resultCode != 0) {
            return new OnAuthAckInfo(recAckInfo.mAuthID, new byte[0], new byte[0], new byte[0], resultCode);
        }
        if (!(ret instanceof RET_AUTH_ACK_RECV)) {
            return new OnAuthAckInfo(recAckInfo.mAuthID, new byte[0], new byte[0], new byte[0], 2046820353);
        }
        RET_AUTH_ACK_RECV retInstance = (RET_AUTH_ACK_RECV) ret;
        byte[] sessionKey = ByteUtil.unboxByteArray((Byte[]) retInstance.sessionKey.getTLVStruct());
        return new OnAuthAckInfo(recAckInfo.mAuthID, ByteUtil.unboxByteArray((Byte[]) retInstance.iv.getTLVStruct()), sessionKey, new byte[0], resultCode);
    }

    public static RespPkInfo processGetPk(ReqPkInfo info) {
        ReqPkInfo reqPkInfo = info;
        CMD_GET_PK cmd = new CMD_GET_PK();
        cmd.authID.setTLVStruct(Long.valueOf(reqPkInfo.mAuthID));
        cmd.userID.setTLVStruct(Long.valueOf(reqPkInfo.mUserID));
        TLVResult<?> tlvResult = TLVEngine.decodeCmdTLV(TcisJNI.processCmd(null, TLVEngine.encode2CmdTLV(cmd)));
        int resultCode = tlvResult.getResultCode();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("processGetPk result, resultCode: ");
        stringBuilder.append(Integer.toHexString(resultCode));
        LogHelper.d(str, stringBuilder.toString());
        TLVTree ret = tlvResult.getResultTLV();
        if (resultCode != 0) {
            return new RespPkInfo(reqPkInfo.mAuthID, (short) 1, new byte[0], new byte[0], resultCode);
        }
        if (!(ret instanceof RET_GET_PK)) {
            return new RespPkInfo(reqPkInfo.mAuthID, (short) 1, new byte[0], new byte[0], 2046820353);
        }
        Cert cert = (Cert) ((RET_GET_PK) ret).cert.getTLVStruct();
        byte[] authPkInfoByte = ByteUtil.unboxByteArray(cert.authPkInfo.encapsulate());
        return new RespPkInfo(reqPkInfo.mAuthID, ((Short) cert.authKeyAlgoEncode.getTLVStruct()).shortValue(), authPkInfoByte, ByteUtil.unboxByteArray((Byte[]) cert.authPKInfoSign.getTLVStruct()), resultCode);
    }

    public static int processCancelAuth(long authID) {
        CMD_AUTH_CANCEL cmd = new CMD_AUTH_CANCEL();
        cmd.authID.setTLVStruct(Long.valueOf(authID));
        return TLVEngine.decodeCmdTLV(TcisJNI.processCmd(null, TLVEngine.encode2CmdTLV(cmd))).getResultCode();
    }

    private static boolean isNeedRequestPk(byte[] result) {
        if (result != null && result.length >= 8 && result[4] == (byte) 0 && result[5] == (byte) 0 && result[6] == (byte) 0 && result[7] == (byte) 1) {
            return true;
        }
        return false;
    }

    public static KaInfoResponse processKaAuth(Context context, KaInfoRequest info) {
        CMD_KA cmdKa = new CMD_KA();
        cmdKa.userId.setTLVStruct(Long.valueOf(info.userId));
        cmdKa.kaVersion.setTLVStruct(Integer.valueOf(info.kaVersion));
        cmdKa.eeAesTmpKey.setTLVStruct(ByteUtil.boxbyteArray(info.aesTmpKey));
        TLVTree kaInfoTree = TLVEngine.decodeTLV(ByteUtil.serverHexString2ByteArray(info.kaInfo));
        if (kaInfoTree == null || !(kaInfoTree instanceof KaInfo)) {
            LogHelper.e(TAG, "ka info decode fail");
            return new KaInfoResponse(2046820355, new byte[0], new byte[0]);
        }
        cmdKa.kaInfo.setTLVStruct(kaInfoTree);
        TLVResult<?> tlvResult = TLVEngine.decodeCmdTLV(TcisJNI.processCmd(null, TLVEngine.encode2CmdTLV(cmdKa)));
        int resultCode = tlvResult.getResultCode();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("processKaAuth result, resultCode: ");
        stringBuilder.append(Integer.toHexString(resultCode));
        LogHelper.d(str, stringBuilder.toString());
        TLVTree ret = tlvResult.getResultTLV();
        if (resultCode != TCIS_Result.SUCCESS.value()) {
            return new KaInfoResponse(resultCode, new byte[0], new byte[0]);
        }
        if (!(ret instanceof RET_KA)) {
            return new KaInfoResponse(2046820353, new byte[0], new byte[0]);
        }
        RET_KA retKa = (RET_KA) ret;
        return new KaInfoResponse(0, ByteUtil.unboxByteArray((Byte[]) retKa.iv.getTLVStruct()), ByteUtil.unboxByteArray((Byte[]) retKa.payload.getTLVStruct()));
    }
}
