package com.android.server.security.eidservice;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.IHwBinder.DeathRecipient;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Log;
import com.android.server.security.core.IHwSecurityPlugin;
import com.android.server.security.core.IHwSecurityPlugin.Creator;
import huawei.android.security.IHwEidPlugin.Stub;
import vendor.huawei.hardware.eid.V1_0.CERTIFICATE_REQUEST_MESSAGE_INPUT_INFO_S;
import vendor.huawei.hardware.eid.V1_0.CERTIFICATE_REQUEST_MESSAGE_S;
import vendor.huawei.hardware.eid.V1_0.ENCRYPTION_FACTOR_S;
import vendor.huawei.hardware.eid.V1_0.FACE_CHANGE_INPUT_INFO_S;
import vendor.huawei.hardware.eid.V1_0.FACE_CHANGE_OUTPUT_INFO_S;
import vendor.huawei.hardware.eid.V1_0.IDENTITY_INFORMATION_INPUT_INFO_S;
import vendor.huawei.hardware.eid.V1_0.IDENTITY_INFORMATION_S;
import vendor.huawei.hardware.eid.V1_0.IEid;
import vendor.huawei.hardware.eid.V1_0.IEid.HWEidGetCertificateRequestMessageCallback;
import vendor.huawei.hardware.eid.V1_0.IEid.HWEidGetFaceIsChangedCallback;
import vendor.huawei.hardware.eid.V1_0.IEid.HWEidGetIdentityInformationCallback;
import vendor.huawei.hardware.eid.V1_0.IEid.HWEidGetImageCallback;
import vendor.huawei.hardware.eid.V1_0.IEid.HWEidGetInfoSignCallback;
import vendor.huawei.hardware.eid.V1_0.IEid.HWEidGetUnsecImageCallback;
import vendor.huawei.hardware.eid.V1_0.IMAGE_CONTAINER_S;
import vendor.huawei.hardware.eid.V1_0.INFO_SIGN_INPUT_INFO_S;
import vendor.huawei.hardware.eid.V1_0.INFO_SIGN_OUTPUT_INFO_S;
import vendor.huawei.hardware.eid.V1_0.INIT_TA_MSG_S;
import vendor.huawei.hardware.eid.V1_0.SEC_IMAGE_S;

public class HwEidPlugin extends Stub implements IHwSecurityPlugin {
    public static final Object BINDLOCK = new Object();
    public static final Creator CREATOR = new Creator() {
        public IHwSecurityPlugin createPlugin(Context context) {
            if (HwEidPlugin.HW_DEBUG) {
                Log.d(HwEidPlugin.TAG, "create HwEidPlugin");
            }
            return new HwEidPlugin(context);
        }

        public String getPluginPermission() {
            return HwEidPlugin.EID_MANAGER_PERMISSION;
        }
    };
    private static final String EID_HIDL_SERVICE_NAME = "eid";
    private static final String EID_MANAGER_PERMISSION = "huawei.android.permission.EID_PERMISSION";
    private static final String EID_VERSION = "1.0";
    private static final int ENCRY_SET_SECMODE = 3;
    private static final boolean HW_DEBUG;
    private static final int INPUT_MAX_TRANSPOT_LEN = 153600;
    private static final int INPUT_TRANSPOT_TIMES = 3;
    private static final int MAX_EID_HIDL_DEAMON_REGISTER_TIMES = 10;
    private static final int MSG_EID_HIDL_DEAMON_SERVIE_REGISTER = 1;
    private static final int OUTPUT_MAX_TRANSPOT_LEN = 163840;
    private static final int OUTPUT_TRANSPOT_TIMES = 3;
    private static final int RET_DEFAULT_ERROR_VALUE = -1001;
    private static final int RET_EID_HIDL_DEAMON_IS_NOT_READY = -1000;
    private static final int RET_EXCEPTION_WHEN_EID_CALL = -1002;
    private static final String TAG = "HwEidPlugin";
    private static final int TRY_GET_HIDL_DEAMON_DEALY_MILLIS = 1000;
    private static HwEidHidlHandler mHwEidHidlHandler;
    private static HandlerThread mHwEidThread;
    private int HWEidGetCertificateRequestMessageRetValue = -1001;
    private int HWEidGetFaceIsChangedRetValue = -1001;
    private int HWEidGetIdentityInformationRetValue = -1001;
    private int HWEidGetImageRetValue = -1001;
    private int HWEidGetInfoSignRetValue = -1001;
    private int HWEidGetUnsecImageRetValue = -1001;
    private Context mContext = null;
    private IEid mEid;
    private DeathRecipient mEidHidlDeamonDeathRecipient = new DeathRecipient() {
        public void serviceDied(long cookie) {
            if (HwEidPlugin.mHwEidHidlHandler != null) {
                Log.e(HwEidPlugin.TAG, "eid hidl deamon service has died, try to reconnect it later.");
                HwEidPlugin.this.mEid = null;
                HwEidPlugin.mHwEidHidlHandler.sendEmptyMessageDelayed(1, 1000);
            }
        }
    };
    private int mEidHidlDeamonRegisterTryTimes = 0;

    private final class HwEidHidlHandler extends Handler {
        public HwEidHidlHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    try {
                        HwEidPlugin.this.mEid = IEid.getService(HwEidPlugin.EID_HIDL_SERVICE_NAME);
                    } catch (Exception e) {
                        Log.e(HwEidPlugin.TAG, "Try get eid hidl deamon servcie failed in handler message.");
                    }
                    if (HwEidPlugin.this.mEid != null) {
                        HwEidPlugin.this.mEidHidlDeamonRegisterTryTimes = 0;
                        try {
                            HwEidPlugin.this.mEid.linkToDeath(HwEidPlugin.this.mEidHidlDeamonDeathRecipient, 0);
                        } catch (Exception e2) {
                            Log.e(HwEidPlugin.TAG, "Exception occured when linkToDeath in handle message");
                        }
                    } else {
                        HwEidPlugin hwEidPlugin = HwEidPlugin.this;
                        hwEidPlugin.mEidHidlDeamonRegisterTryTimes = hwEidPlugin.mEidHidlDeamonRegisterTryTimes + 1;
                        if (HwEidPlugin.this.mEidHidlDeamonRegisterTryTimes < 10) {
                            Log.e(HwEidPlugin.TAG, "eid hidl daemon service is not ready, try times : " + HwEidPlugin.this.mEidHidlDeamonRegisterTryTimes);
                            HwEidPlugin.mHwEidHidlHandler.sendEmptyMessageDelayed(1, 1000);
                        } else {
                            Log.e(HwEidPlugin.TAG, "eid hidl daemon service connection failed.");
                        }
                    }
                    if (HwEidPlugin.HW_DEBUG) {
                        Log.d(HwEidPlugin.TAG, "handler thread received request eid hidl deamon message.");
                        return;
                    }
                    return;
                default:
                    Log.e(HwEidPlugin.TAG, "handler thread received unknown message : " + msg.what);
                    return;
            }
        }
    }

    static {
        boolean isLoggable = (SystemProperties.get("ro.secure", "1").equals("0") || Log.HWINFO) ? true : Log.HWModuleLog ? Log.isLoggable(TAG, 4) : false;
        HW_DEBUG = isLoggable;
    }

    public HwEidPlugin(Context context) {
        this.mContext = context;
    }

    public IBinder asBinder() {
        checkPermission(EID_MANAGER_PERMISSION);
        return this;
    }

    public void onStart() {
        if (HW_DEBUG) {
            Log.d(TAG, "HwEidPlugin start");
        }
        mHwEidThread = new HandlerThread(TAG);
        mHwEidThread.start();
        mHwEidHidlHandler = new HwEidHidlHandler(mHwEidThread.getLooper());
    }

    public void onStop() {
        if (mHwEidHidlHandler != null) {
            mHwEidHidlHandler = null;
        }
        if (mHwEidThread != null) {
            mHwEidThread.quitSafely();
            mHwEidThread = null;
        }
        try {
            if (HW_DEBUG) {
                Log.d(TAG, "close HwEidPlugin");
            }
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "stop error" + e.toString());
        }
    }

    private void checkPermission(String permission) {
        this.mContext.enforceCallingOrSelfPermission(permission, "Must have " + permission + " permission.");
    }

    private int minArrayLen(int arrLenA, int arrLenB) {
        return arrLenA > arrLenB ? arrLenB : arrLenA;
    }

    public int eid_init(byte[] hw_aid, int hw_aid_len, byte[] eid_aid, int eid_aid_len, byte[] logo, int logo_size) {
        int ret;
        checkPermission(EID_MANAGER_PERMISSION);
        boolean hidlServiceErr = false;
        INIT_TA_MSG_S input = new INIT_TA_MSG_S();
        input.hw_aid_len = hw_aid_len;
        input.eid_aid_len = eid_aid_len;
        input.logo_size = logo_size;
        System.arraycopy(hw_aid, 0, input.hw_aid, 0, minArrayLen(hw_aid.length, input.hw_aid.length));
        System.arraycopy(eid_aid, 0, input.eid_aid, 0, minArrayLen(eid_aid.length, input.eid_aid.length));
        System.arraycopy(logo, 0, input.eid_logo, 0, minArrayLen(logo.length, input.eid_logo.length));
        if (this.mEid == null) {
            try {
                this.mEid = IEid.getService(EID_HIDL_SERVICE_NAME);
            } catch (Exception e) {
                Log.e(TAG, "Try get mEid hidl deamon servcie failed");
                hidlServiceErr = true;
            }
            if (!hidlServiceErr) {
                try {
                    this.mEid.linkToDeath(this.mEidHidlDeamonDeathRecipient, 0);
                } catch (Exception e2) {
                    Log.e(TAG, "Exception occured when linkToDeath in handle message");
                }
            }
        }
        if (this.mEid != null) {
            try {
                ret = this.mEid.HWEidInitTa(input);
                if (HW_DEBUG) {
                    Log.d(TAG, "eid init ret : " + ret);
                }
            } catch (RemoteException e3) {
                Log.e(TAG, "eid init from hidl failed.");
                Log.e(TAG, "RemoteException e:" + e3);
                return -1002;
            }
        }
        ret = -1000;
        mHwEidHidlHandler.sendEmptyMessageDelayed(1, 1000);
        Log.e(TAG, "eid hidl deamon is not ready when eid init");
        if (HW_DEBUG) {
            Log.d(TAG, "eid init from mEid hidl ret : " + ret);
        }
        return ret;
    }

    public int eid_finish() {
        int ret;
        checkPermission(EID_MANAGER_PERMISSION);
        if (this.mEid == null) {
            try {
                this.mEid = IEid.getService(EID_HIDL_SERVICE_NAME);
            } catch (Exception e) {
                Log.e(TAG, "Try get mEid hidl deamon servcie failed");
            }
        }
        if (this.mEid != null) {
            try {
                ret = this.mEid.HWEidFiniTa();
                if (HW_DEBUG) {
                    Log.d(TAG, "eid finish ta ret : " + ret);
                }
            } catch (RemoteException e2) {
                Log.e(TAG, "eid finish from mEid hidl failed.");
                return -1002;
            }
        }
        ret = -1000;
        mHwEidHidlHandler.sendEmptyMessageDelayed(1, 1000);
        Log.e(TAG, "eid hidl deamon is not ready when eid finish");
        if (HW_DEBUG) {
            Log.d(TAG, "eid finish from mEid hidl ret : " + ret);
        }
        return ret;
    }

    public int eid_get_image(int transpotCounter, int encryption_method, byte[] certificate, int certificate_len, byte[] image, int[] image_len, byte[] de_skey, int[] de_skey_len) {
        int ret;
        checkPermission(EID_MANAGER_PERMISSION);
        boolean hasExecption = false;
        if (this.mEid == null) {
            try {
                this.mEid = IEid.getService(EID_HIDL_SERVICE_NAME);
            } catch (Exception e) {
                Log.e(TAG, "Try get mEid hidl deamon servcie failed");
            }
        }
        if (this.mEid != null) {
            try {
                final ENCRYPTION_FACTOR_S factor = new ENCRYPTION_FACTOR_S();
                factor.encryptionMethod = encryption_method;
                factor.certificateLen = certificate_len;
                factor.splitTimes = transpotCounter;
                System.arraycopy(certificate, 0, factor.certificate, 0, minArrayLen(certificate.length, factor.certificate.length));
                final int i = encryption_method;
                final byte[] bArr = image;
                final byte[] bArr2 = de_skey;
                final int[] iArr = image_len;
                final int[] iArr2 = de_skey_len;
                this.mEid.HWEidGetImage(factor, new HWEidGetImageCallback() {
                    public void onValues(int HWEidGetImageRet, SEC_IMAGE_S secImage) {
                        if (HWEidGetImageRet == 0 && i != 3) {
                            System.arraycopy(secImage.image, 0, bArr, 0, HwEidPlugin.this.minArrayLen(secImage.image.length, bArr.length));
                            if (1 == factor.splitTimes) {
                                System.arraycopy(secImage.deSkey, 0, bArr2, 0, HwEidPlugin.this.minArrayLen(secImage.deSkeyLen, bArr2.length));
                                iArr[0] = secImage.len;
                                iArr2[0] = secImage.deSkeyLen;
                            }
                        }
                        if (HwEidPlugin.HW_DEBUG) {
                            Log.d(HwEidPlugin.TAG, "eid get image call counter: " + factor.splitTimes);
                        }
                        HwEidPlugin.this.HWEidGetImageRetValue = HWEidGetImageRet;
                        if (HwEidPlugin.HW_DEBUG) {
                            Log.d(HwEidPlugin.TAG, "eid get image HWEidGetImageRetValue : " + HwEidPlugin.this.HWEidGetImageRetValue + " HWEidGetImageRet:" + HWEidGetImageRet);
                        }
                    }
                });
            } catch (RemoteException e2) {
                hasExecption = true;
                Log.e(TAG, "eid get image from mEid hidl failed." + e2);
            }
            ret = hasExecption ? -1002 : this.HWEidGetImageRetValue;
        } else {
            ret = -1000;
            mHwEidHidlHandler.sendEmptyMessageDelayed(1, 1000);
            Log.e(TAG, "mEid hidl deamon is not ready when eid get image");
        }
        if (HW_DEBUG) {
            Log.d(TAG, "eid get image from mEid hidl ret : " + ret);
        }
        return ret;
    }

    public int eid_get_unsec_image(byte[] src_image, int src_image_len, int transpotCounter, int encryption_method, byte[] certificate, int certificate_len, byte[] image, int[] image_len, byte[] de_skey, int[] de_skey_len) {
        int ret;
        checkPermission(EID_MANAGER_PERMISSION);
        boolean hasExecption = false;
        if (this.mEid == null) {
            try {
                this.mEid = IEid.getService(EID_HIDL_SERVICE_NAME);
            } catch (Exception e) {
                Log.e(TAG, "Try get mEid hidl deamon servcie failed");
            }
        }
        if (this.mEid != null) {
            try {
                IMAGE_CONTAINER_S container = new IMAGE_CONTAINER_S();
                final ENCRYPTION_FACTOR_S factor = new ENCRYPTION_FACTOR_S();
                factor.splitTimes = transpotCounter;
                if (factor.splitTimes < 3) {
                    System.arraycopy(src_image, 0, container.image, 0, minArrayLen(src_image.length, container.image.length));
                } else if (factor.splitTimes == 3) {
                    container.len = src_image_len;
                    System.arraycopy(src_image, 0, container.image, 0, minArrayLen(src_image.length, container.image.length));
                    System.arraycopy(certificate, 0, factor.certificate, 0, minArrayLen(certificate.length, factor.certificate.length));
                    factor.encryptionMethod = encryption_method;
                    factor.certificateLen = certificate_len;
                }
                final byte[] bArr = de_skey;
                final int[] iArr = image_len;
                final int[] iArr2 = de_skey_len;
                final byte[] bArr2 = image;
                this.mEid.HWEidGetUnsecImage(container, factor, new HWEidGetUnsecImageCallback() {
                    public void onValues(int HWEidGetUnsecImageRet, SEC_IMAGE_S secImage) {
                        if (factor.splitTimes >= 3 && HWEidGetUnsecImageRet == 0) {
                            if (factor.splitTimes == 3) {
                                System.arraycopy(secImage.deSkey, 0, bArr, 0, HwEidPlugin.this.minArrayLen(secImage.deSkey.length, bArr.length));
                                iArr[0] = secImage.len;
                                iArr2[0] = secImage.deSkeyLen;
                            }
                            System.arraycopy(secImage.image, 0, bArr2, 0, HwEidPlugin.this.minArrayLen(secImage.image.length, bArr2.length));
                        }
                        if (HwEidPlugin.HW_DEBUG) {
                            Log.d(HwEidPlugin.TAG, "eid get unsec image call counter: " + factor.splitTimes);
                        }
                        HwEidPlugin.this.HWEidGetUnsecImageRetValue = HWEidGetUnsecImageRet;
                        if (HwEidPlugin.HW_DEBUG) {
                            Log.d(HwEidPlugin.TAG, "eid get unsec image HWEidGetUnsecImageRetValue : " + HwEidPlugin.this.HWEidGetUnsecImageRetValue + " HWEidGetUnsecImageRet:" + HWEidGetUnsecImageRet);
                        }
                    }
                });
            } catch (RemoteException e2) {
                hasExecption = true;
                Log.e(TAG, "eid get unsec image from mEid hidl failed.");
            }
            ret = hasExecption ? -1002 : this.HWEidGetUnsecImageRetValue;
        } else {
            ret = -1000;
            mHwEidHidlHandler.sendEmptyMessageDelayed(1, 1000);
            Log.e(TAG, "mEid hidl deamon is not ready when eid get unsec image");
        }
        if (HW_DEBUG) {
            Log.d(TAG, "eid get unsec image from mEid hidl ret : " + ret);
        }
        return ret;
    }

    public int eid_get_certificate_request_message(final byte[] request_message, final int[] message_len) {
        int ret;
        checkPermission(EID_MANAGER_PERMISSION);
        boolean hasExecption = false;
        CERTIFICATE_REQUEST_MESSAGE_INPUT_INFO_S input = new CERTIFICATE_REQUEST_MESSAGE_INPUT_INFO_S();
        if (this.mEid == null) {
            try {
                this.mEid = IEid.getService(EID_HIDL_SERVICE_NAME);
            } catch (Exception e) {
                Log.e(TAG, "Try get mEid hidl deamon servcie failed");
            }
        }
        if (this.mEid != null) {
            try {
                this.mEid.HWEidGetCertificateRequestMessage(input, new HWEidGetCertificateRequestMessageCallback() {
                    public void onValues(int HWEidGetCertificateRequestMessageRet, CERTIFICATE_REQUEST_MESSAGE_S certReqMsg) {
                        if (HWEidGetCertificateRequestMessageRet == 0) {
                            System.arraycopy(certReqMsg.message, 0, request_message, 0, HwEidPlugin.this.minArrayLen(certReqMsg.len, request_message.length));
                            message_len[0] = certReqMsg.len;
                            if (HwEidPlugin.HW_DEBUG) {
                                Log.d(HwEidPlugin.TAG, "eid get certificate request message message len : " + message_len[0] + " certReqMsg len:" + certReqMsg.len);
                            }
                        }
                        HwEidPlugin.this.HWEidGetCertificateRequestMessageRetValue = HWEidGetCertificateRequestMessageRet;
                        if (HwEidPlugin.HW_DEBUG) {
                            Log.d(HwEidPlugin.TAG, "eid get certificate request message HWEidGetCertificateRequestMessageRetValue : " + HwEidPlugin.this.HWEidGetCertificateRequestMessageRetValue + " HWEidGetCertificateRequestMessageRet:" + HWEidGetCertificateRequestMessageRet);
                        }
                    }
                });
            } catch (RemoteException e2) {
                hasExecption = true;
                Log.e(TAG, "eid get certificate request message from mEid hidl failed.");
            }
            ret = hasExecption ? -1002 : this.HWEidGetCertificateRequestMessageRetValue;
        } else {
            ret = -1000;
            mHwEidHidlHandler.sendEmptyMessageDelayed(1, 1000);
            Log.e(TAG, "mEid hidl deamon is not ready when eid get certificate request message");
        }
        if (HW_DEBUG) {
            Log.d(TAG, "eid get certificate request message from mEid hidl ret : " + ret);
        }
        return ret;
    }

    public int eid_sign_info(int transpotCounter, int encryption_method, byte[] info, int info_len, final byte[] sign, final int[] sign_len) {
        int ret;
        checkPermission(EID_MANAGER_PERMISSION);
        boolean hasExecption = false;
        if (this.mEid == null) {
            try {
                this.mEid = IEid.getService(EID_HIDL_SERVICE_NAME);
            } catch (Exception e) {
                Log.e(TAG, "Try get mEid hidl deamon servcie failed");
            }
        }
        if (this.mEid != null) {
            try {
                final INFO_SIGN_INPUT_INFO_S input = new INFO_SIGN_INPUT_INFO_S();
                input.encryptionMethod = encryption_method;
                input.infoLen = info_len;
                input.splitTimes = transpotCounter;
                System.arraycopy(info, 0, input.info, 0, minArrayLen(info.length, input.info.length));
                this.mEid.HWEidGetInfoSign(input, new HWEidGetInfoSignCallback() {
                    public void onValues(int HWEidGetInfoSignRet, INFO_SIGN_OUTPUT_INFO_S output) {
                        if (HWEidGetInfoSignRet == 0) {
                            if (1 == input.splitTimes) {
                                sign_len[0] = output.infoLen;
                            }
                            System.arraycopy(output.signInfo, 0, sign, 0, HwEidPlugin.this.minArrayLen(output.signInfo.length, sign.length));
                        }
                        if (HwEidPlugin.HW_DEBUG) {
                            Log.d(HwEidPlugin.TAG, "eid sign info call counter: " + input.splitTimes);
                        }
                        HwEidPlugin.this.HWEidGetInfoSignRetValue = HWEidGetInfoSignRet;
                        if (HwEidPlugin.HW_DEBUG) {
                            Log.d(HwEidPlugin.TAG, "eid sign info HWEidGetInfoSignRetValue : " + HwEidPlugin.this.HWEidGetInfoSignRetValue + " HWEidGetInfoSignRet:" + HWEidGetInfoSignRet);
                        }
                    }
                });
            } catch (RemoteException e2) {
                hasExecption = true;
                Log.e(TAG, "eid sign info from mEid hidl failed.");
            }
            ret = hasExecption ? -1002 : this.HWEidGetInfoSignRetValue;
        } else {
            ret = -1000;
            mHwEidHidlHandler.sendEmptyMessageDelayed(1, 1000);
            Log.e(TAG, "mEid hidl deamon is not ready when eid sign info");
        }
        if (HW_DEBUG) {
            Log.d(TAG, "eid sign info from mEid hidl ret : " + ret);
        }
        return ret;
    }

    public int eid_get_identity_information(final byte[] identity_info, final int[] identity_info_len) {
        int ret;
        checkPermission(EID_MANAGER_PERMISSION);
        boolean hasExecption = false;
        IDENTITY_INFORMATION_INPUT_INFO_S input = new IDENTITY_INFORMATION_INPUT_INFO_S();
        if (this.mEid == null) {
            try {
                this.mEid = IEid.getService(EID_HIDL_SERVICE_NAME);
            } catch (Exception e) {
                Log.e(TAG, "Try get mEid hidl deamon servcie failed");
            }
        }
        if (this.mEid != null) {
            try {
                this.mEid.HWEidGetIdentityInformation(input, new HWEidGetIdentityInformationCallback() {
                    public void onValues(int HWEidGetIdentityInformationRet, IDENTITY_INFORMATION_S idInfo) {
                        if (HWEidGetIdentityInformationRet == 0) {
                            System.arraycopy(idInfo.info, 0, identity_info, 0, HwEidPlugin.this.minArrayLen(idInfo.len, identity_info.length));
                            identity_info_len[0] = idInfo.len;
                        }
                        HwEidPlugin.this.HWEidGetIdentityInformationRetValue = HWEidGetIdentityInformationRet;
                        if (HwEidPlugin.HW_DEBUG) {
                            Log.d(HwEidPlugin.TAG, "eid get identity information HWEidGetIdentityInformationRetValue : " + HwEidPlugin.this.HWEidGetIdentityInformationRetValue + " HWEidGetIdentityInformationRet:" + HWEidGetIdentityInformationRet);
                        }
                    }
                });
            } catch (RemoteException e2) {
                hasExecption = true;
                Log.e(TAG, "eid get identity information from mEid hidl failed.");
            }
            ret = hasExecption ? -1002 : this.HWEidGetIdentityInformationRetValue;
        } else {
            ret = -1000;
            mHwEidHidlHandler.sendEmptyMessageDelayed(1, 1000);
            Log.e(TAG, "mEid hidl deamon is not ready when eid get identity information");
        }
        if (HW_DEBUG) {
            Log.d(TAG, "eid get identity information from mEid hidl ret : " + ret);
        }
        return ret;
    }

    public int eid_get_face_is_changed(int cmd_id) {
        int ret;
        checkPermission(EID_MANAGER_PERMISSION);
        boolean hasExecption = false;
        FACE_CHANGE_INPUT_INFO_S input = new FACE_CHANGE_INPUT_INFO_S();
        input.cmdID = cmd_id;
        if (this.mEid == null) {
            try {
                this.mEid = IEid.getService(EID_HIDL_SERVICE_NAME);
            } catch (Exception e) {
                Log.e(TAG, "Try get mEid hidl deamon servcie failed");
            }
        }
        if (this.mEid != null) {
            try {
                this.mEid.HWEidGetFaceIsChanged(input, new HWEidGetFaceIsChangedCallback() {
                    public void onValues(int HWEidGetFaceIsChangedRet, FACE_CHANGE_OUTPUT_INFO_S output) {
                        HwEidPlugin.this.HWEidGetFaceIsChangedRetValue = HWEidGetFaceIsChangedRet;
                        if (HwEidPlugin.HW_DEBUG) {
                            Log.d(HwEidPlugin.TAG, "eid get face is changed HWEidGetFaceIsChangedRetValue : " + HwEidPlugin.this.HWEidGetFaceIsChangedRetValue + " HWEidGetFaceIsChangedRet:" + HWEidGetFaceIsChangedRet);
                        }
                    }
                });
            } catch (RemoteException e2) {
                hasExecption = true;
                Log.e(TAG, "eid get face is changed from mEid hidl failed.");
            }
            ret = hasExecption ? -1002 : this.HWEidGetFaceIsChangedRetValue;
        } else {
            ret = -1000;
            mHwEidHidlHandler.sendEmptyMessageDelayed(1, 1000);
            Log.e(TAG, "mEid hidl deamon is not ready when eid get face is changed");
        }
        if (HW_DEBUG) {
            Log.d(TAG, "eid get face is changed from mEid hidl ret : " + ret);
        }
        return ret;
    }

    public String eid_get_version() {
        checkPermission(EID_MANAGER_PERMISSION);
        return "1.0";
    }
}
