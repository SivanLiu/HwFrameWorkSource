package com.huawei.nb.coordinator.helper.verify;

import android.content.Context;
import android.text.TextUtils;
import com.huawei.nb.coordinator.helper.http.HttpRequest;
import com.huawei.nb.utils.DeviceUtil;
import com.huawei.nb.utils.logger.DSLog;
import java.security.SecureRandom;

public class VerifySigature implements IVerify {
    private static final String CONTENT_TYPE = "content-type";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String ENCRYPTED_TYPE_BASE = "TEE";
    private static final int ORIGIN_CODE_LENGTH = 8;
    private static final String TAG = "VerifySigature";

    @Override // com.huawei.nb.coordinator.helper.verify.IVerify
    public boolean generateAuthorization(Context context, HttpRequest.Builder builder, String appID) {
        String serialNumber = DeviceUtil.getSerialNumber();
        if (TextUtils.isEmpty(serialNumber)) {
            DSLog.e("VerifySigature empty serialNumber.", new Object[0]);
            return false;
        }
        String originCode = getOriginCode();
        if (TextUtils.isEmpty(originCode)) {
            DSLog.e("VerifySigature: empty originCode.", new Object[0]);
            return false;
        }
        String encryptedCode = getEncryptedCode(context, originCode);
        if (TextUtils.isEmpty(encryptedCode)) {
            DSLog.e("VerifySigature: empty encryptedCode.", new Object[0]);
            return false;
        }
        String emmcID = DeviceUtil.getEMMCID();
        if (TextUtils.isEmpty(emmcID)) {
            DSLog.e("VerifySigature empty emmcID.", new Object[0]);
            return false;
        }
        String packageName = context.getPackageName();
        if (TextUtils.isEmpty(packageName)) {
            DSLog.e("VerifySigature empty packageName.", new Object[0]);
            return false;
        }
        builder.addRequestHeader(IVerifyVar.DEVICE_ID, serialNumber);
        builder.addRequestHeader(IVerifyVar.DEVICE_SN, serialNumber);
        builder.addRequestHeader(IVerifyVar.ENCRYPTEDCODE, encryptedCode);
        builder.addRequestHeader(IVerifyVar.ORIGINALCODE, packageName + originCode + emmcID + IVerifyVar.REQUEST_HEAD_ORIGINCODE_TAIL);
        builder.addRequestHeader(IVerifyVar.ENCRYPTED_TYPE, ENCRYPTED_TYPE_BASE);
        builder.addRequestHeader(CONTENT_TYPE, CONTENT_TYPE_JSON);
        return true;
    }

    @Override // com.huawei.nb.coordinator.helper.verify.IVerify
    public String verifyTokenHeader() {
        return null;
    }

    private String getOriginCode() {
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(new byte[8]);
        return secureRandom.nextLong() + "";
    }

    private String getEncryptedCode(Context context, String originCode) {
        return new VerifyViaHWMember().getSignature(originCode, context.getPackageName()).replace("\n", "");
    }
}
