package com.hwawei.application.hwpay;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import com.huawei.security.hccm.ClientCertificateManager;
import com.huawei.security.hccm.EnrollmentException;
import com.huawei.security.hccm.common.callback.EnrollCertificateCallback;
import com.huawei.security.hccm.common.connection.exception.MalFormedPKIMessageException;
import com.huawei.security.hccm.param.CredentialSpec;
import com.huawei.security.hccm.param.CredentialSpec.Builder;
import com.huawei.security.hccm.param.EnrollmentContext;
import com.huawei.security.hccm.param.EnrollmentParamsSpec;
import com.huawei.security.hccm.param.ProtocolParamCMP;
import java.net.URL;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import org.bouncycastle.asn1.x500.X500Name;
import org.json.JSONObject;

public class HWPayServiceManager {
    private static final String MIN_HCCM_SDK_VERSION = "9.0.0.2";
    private static final String TAG = "HWPayServiceManager";
    private final int MAX_ALIAS_LEN = 89;
    private final int MAX_TOKEN_LEN = 512;
    private final int MAX_URL_LEN = 4096;
    private final int MIN_ALIAS_LEN = 1;
    private final int MIN_TOKEN_LEN = 1;
    private final int MIN_URL_LEN = 1;
    private String keyStoreProvider = "HwUniversalKeyStoreProvider";
    private String keyStoreType = "HwKeyStore";
    private String mAlias;
    private EnrollCertificateCallback mCallback;
    private JSONObject mConSettings;
    private String mErrMsg;
    private ClientCertificateManager mHccm;
    private X509Certificate mRaCertificate;
    private X509Certificate mRootCertificate;
    private String mSigPadding;
    private X500Name mSubject;
    private TokenInfo mToken;
    private String mUrl;

    @SuppressLint({"StaticFieldLeak"})
    private class CertEnrollTask extends AsyncTask<Void, Void, Integer> {
        private CertEnrollTask() {
        }

        protected Integer doInBackground(Void... voids) {
            int retCode = 0;
            try {
                HWPayServiceManager.this.enrollCertInternal();
            } catch (EnrollmentException e) {
                String str = HWPayServiceManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("enroll cert failed ");
                stringBuilder.append(e.getMessage());
                stringBuilder.append(" and the error code is ");
                stringBuilder.append(e.getErrorCode());
                Log.e(str, stringBuilder.toString());
                retCode = e.getErrorCode();
            } catch (Exception e2) {
                Log.e(HWPayServiceManager.TAG, "enroll cert failed with unknown exception");
                retCode = -9;
            }
            return Integer.valueOf(retCode);
        }

        protected void onPostExecute(Integer result) {
            HWPayServiceManager.this.showResult(result.intValue());
        }
    }

    public HWPayServiceManager() throws EnrollmentException {
        try {
            this.mHccm = ClientCertificateManager.getInstance(this.keyStoreType, this.keyStoreProvider);
            if (this.mHccm != null) {
                int errorCode = checkVersion();
                if (errorCode != 0) {
                    Log.e(TAG, "version not match");
                    throw new EnrollmentException("version ont support", errorCode);
                }
                return;
            }
            Log.e(TAG, "initialize HWPayServiceManager failed");
            throw new EnrollmentException("hccm is null", -10);
        } catch (EnrollmentException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("get instance of ClientCertificateManager failed because ");
            stringBuilder.append(e.getMessage());
            this.mErrMsg = stringBuilder.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.mErrMsg);
            stringBuilder.append(" and the error code is ");
            stringBuilder.append(e.getErrorCode());
            Log.e(TAG, stringBuilder.toString());
            throw e;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:24:0x015a A:{Splitter: B:7:0x006f, ExcHandler: java.lang.NullPointerException (r6_18 'e' java.lang.RuntimeException)} */
    /* JADX WARNING: Missing block: B:24:0x015a, code:
            r6 = move-exception;
     */
    /* JADX WARNING: Missing block: B:25:0x015b, code:
            r7 = new java.lang.StringBuilder();
            r7.append("invalid params encountered \n");
            r7.append(r6.getMessage());
            r14.mErrMsg = r7.toString();
     */
    /* JADX WARNING: Missing block: B:26:0x0179, code:
            throw new com.huawei.security.hccm.EnrollmentException(r14.mErrMsg, -1);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void enrollCertInternal() throws Exception {
        StringBuilder stringBuilder;
        if (checkCertEnrollParams()) {
            JSONObject authHeader;
            CredentialSpec userCredential = new Builder(MessageDigest.getInstance("SHA-256").digest(this.mToken.getCredential().getBytes("UTF-8"))).build();
            if (this.mConSettings.optJSONObject("user_settings") == null) {
                authHeader = new JSONObject();
            } else {
                authHeader = this.mConSettings.optJSONObject("user_settings");
            }
            authHeader.put("Authorization", this.mToken.getToken());
            this.mConSettings.put("user_settings", authHeader);
            ProtocolParamCMP cmpParam = (ProtocolParamCMP) new ProtocolParamCMP().getInstance(1);
            cmpParam.setRaCertificate(this.mRaCertificate);
            cmpParam.setRootCertificate(this.mRootCertificate);
            EnrollmentParamsSpec enrollmentParamsSpec = null;
            try {
                EnrollmentParamsSpec enrollmentParamsSpec2 = new EnrollmentParamsSpec.Builder(this.mAlias, this.mSubject, new URL(this.mUrl), "ENROLLMENT_PROTOCOL_CMP", cmpParam).setConnectionSettings(this.mConSettings).setUserCredential(userCredential).setSigAlgPadding(this.mSigPadding).build();
                try {
                    Certificate[] chain = this.mHccm.enroll(enrollmentParamsSpec2);
                    try {
                        EnrollmentContext enrollmentContext = new EnrollmentContext(enrollmentParamsSpec2);
                        enrollmentContext.setClientCertificateChain(chain);
                        this.mHccm.store(enrollmentContext);
                        Log.d(TAG, "application certificate was stored successfully!");
                    } catch (EnrollmentException e) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("encounter EnrollmentException during store ");
                        stringBuilder2.append(e.getMessage());
                        this.mErrMsg = stringBuilder2.toString();
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(this.mErrMsg);
                        stringBuilder2.append(" error code is ");
                        stringBuilder2.append(e.getErrorCode());
                        Log.e(TAG, stringBuilder2.toString());
                        throw new EnrollmentException(this.mErrMsg, e.getErrorCode());
                    }
                } catch (MalFormedPKIMessageException e2) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("encounter malformed pki message exception");
                    stringBuilder.append(e2.getMessage());
                    this.mErrMsg = stringBuilder.toString();
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(this.mErrMsg);
                    stringBuilder.append(" errorCode is ");
                    stringBuilder.append(e2.getErrorCode());
                    Log.e(TAG, stringBuilder.toString());
                    throw new EnrollmentException(e2.getMessage(), -2);
                } catch (EnrollmentException e3) {
                    this.mErrMsg = "encounter EnrollmentException during enroll";
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(this.mErrMsg);
                    stringBuilder.append(" errorCode is ");
                    stringBuilder.append(e3.getErrorCode());
                    Log.e(TAG, stringBuilder.toString());
                    throw e3;
                }
            } catch (RuntimeException e4) {
            }
        } else {
            Log.e(TAG, "invalid parameter for certificate enrollment.");
            throw new EnrollmentException("invalid parameters", -1);
        }
    }

    public void initializeCertParams(@NonNull X500Name subject, @NonNull String alias, @NonNull TokenInfo token, @NonNull JSONObject connectSetting, @NonNull String url, X509Certificate raCertificate, @NonNull X509Certificate rootCertificate, String padding, @NonNull EnrollCertificateCallback callback) {
        this.mSubject = subject;
        this.mAlias = alias;
        this.mToken = token;
        this.mConSettings = connectSetting;
        this.mUrl = url;
        this.mRaCertificate = raCertificate;
        this.mRootCertificate = rootCertificate;
        this.mSigPadding = padding;
        this.mCallback = callback;
    }

    public void enrollAppCertificate() throws Exception {
        Log.d(TAG, "enrollAppCertificate sync");
        try {
            enrollCertInternal();
            showResult(0);
        } catch (EnrollmentException e) {
            Log.e(TAG, "enroll cert failed", e);
            showResult(e.getErrorCode());
            throw e;
        } catch (Exception e2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("enroll cert failed for unknown exception, ");
            stringBuilder.append(e2.getMessage());
            Log.e(TAG, stringBuilder.toString(), e2);
            showResult(-9);
            throw new EnrollmentException("enrollAppCertificate failed", -9);
        }
    }

    public void enrollAppCertificateAsync() throws Exception {
        Log.d(TAG, "enrollAppCertificate Async");
        new CertEnrollTask().execute(new Void[0]);
    }

    private boolean checkCertEnrollParams() {
        if (this.mAlias == null || this.mCallback == null || this.mToken == null || this.mToken.getType() == null || this.mToken.getCredential() == null || this.mSubject == null || this.mUrl == null || this.mRootCertificate == null) {
            Log.e(TAG, "params shouldn't be null");
            return false;
        } else if (!this.mToken.getType().equals(TokenInfo.TOKEN_TYPE_ACCESS_TOKEN)) {
            Log.e(TAG, "token type is invalid");
            return false;
        } else if (this.mUrl.length() < 1 || this.mUrl.length() > 4096) {
            Log.e(TAG, "the length of url is invalid");
            return false;
        } else if (this.mToken.getCredential().length() < 1 || this.mToken.getCredential().length() > 512) {
            Log.e(TAG, "the length of token is invalid");
            return false;
        } else if (this.mAlias.length() >= 1 && this.mAlias.length() <= 89) {
            return true;
        } else {
            Log.e(TAG, "the length of alias is invalid");
            return false;
        }
    }

    private void showResult(int errorCode) {
        if (errorCode == 0) {
            this.mCallback.onSuccess();
        } else {
            this.mCallback.onError(errorCode);
        }
    }

    private static int checkVersion() {
        String currentHccmSdkVer = ClientCertificateManager.getHccmSdkVersion();
        if (currentHccmSdkVer == null) {
            return -6;
        }
        if (ClientCertificateManager.isCurrentVersionSupport(currentHccmSdkVer, MIN_HCCM_SDK_VERSION)) {
            return 0;
        }
        Log.e(TAG, "current HUKS version does not support hccm");
        return -7;
    }
}
