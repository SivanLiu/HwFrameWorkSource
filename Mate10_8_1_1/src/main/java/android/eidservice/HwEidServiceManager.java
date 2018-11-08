package android.eidservice;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import huawei.android.security.IHwEidPlugin;
import huawei.android.security.IHwSecurityService;
import huawei.android.security.IHwSecurityService.Stub;

public class HwEidServiceManager {
    private static final int DEVICE_SECURE_DIAGNOSE_ID = 2;
    private static final int HW_EID_PLUGIN_ID = 15;
    private static final int RET_DEFAULT_ERROR_VALUE = -2001;
    private static final int RET_EXCEPTION_WHEN_EID_FINISH_CALL = -2003;
    private static final int RET_EXCEPTION_WHEN_EID_INIT_CALL = -2002;
    private static final int RET_EXCEPTION_WHEN_GET_CERTIFICATE_CALL = -2006;
    private static final int RET_EXCEPTION_WHEN_GET_FACE_CHANGED_CALL = -2009;
    private static final int RET_EXCEPTION_WHEN_GET_IDENTITY_CALL = -2008;
    private static final int RET_EXCEPTION_WHEN_GET_IMAGE_CALL = -2004;
    private static final int RET_EXCEPTION_WHEN_GET_INFO_SIGN_CALL = -2007;
    private static final int RET_EXCEPTION_WHEN_GET_UNSEC_IMAGE_CALL = -2005;
    private static final String SECURITY_SERVICE = "securityserver";
    private static final String TAG = "HwEidServiceManager";
    private static IHwEidPlugin mIHwEidPlugin;
    private static volatile HwEidServiceManager sInstance = null;
    private IHwSecurityService mSecurityService = Stub.asInterface(ServiceManager.getService(SECURITY_SERVICE));

    private HwEidServiceManager() {
        if (this.mSecurityService == null) {
            Log.e(TAG, "error, securityservice was null");
        }
    }

    public static HwEidServiceManager getInstance() {
        if (sInstance == null) {
            synchronized (HwEidServiceManager.class) {
                if (sInstance == null) {
                    sInstance = new HwEidServiceManager();
                }
            }
        }
        return sInstance;
    }

    private IHwEidPlugin getHwEidPlugin() {
        if (mIHwEidPlugin != null) {
            return mIHwEidPlugin;
        }
        if (this.mSecurityService != null) {
            try {
                mIHwEidPlugin = IHwEidPlugin.Stub.asInterface(this.mSecurityService.querySecurityInterface(15));
                if (mIHwEidPlugin == null) {
                    Log.e(TAG, "error, IHwEidPlugin is null");
                }
                return mIHwEidPlugin;
            } catch (RemoteException e) {
                Log.e(TAG, "Get getHwEidPlugin failed!");
            }
        }
        return null;
    }

    public int eid_init(byte[] hw_aid, int hw_aid_len, byte[] eid_aid, int eid_aid_len, byte[] logo, int logo_size) {
        int ret = RET_DEFAULT_ERROR_VALUE;
        if (getHwEidPlugin() != null) {
            try {
                ret = mIHwEidPlugin.eid_init(hw_aid, hw_aid_len, eid_aid, eid_aid_len, logo, logo_size);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException when eid init jnis is invoked");
                return RET_EXCEPTION_WHEN_EID_INIT_CALL;
            }
        }
        return ret;
    }

    public int eid_finish() {
        int ret = RET_DEFAULT_ERROR_VALUE;
        if (getHwEidPlugin() != null) {
            try {
                ret = mIHwEidPlugin.eid_finish();
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException when eid finish jni is invoked");
                return RET_EXCEPTION_WHEN_EID_FINISH_CALL;
            }
        }
        return ret;
    }

    public int eid_get_image(int transpotCounter, int encryption_method, byte[] certificate, int certificate_len, byte[] image, int[] image_len, byte[] de_skey, int[] de_skey_len) {
        int ret = RET_DEFAULT_ERROR_VALUE;
        if (getHwEidPlugin() != null) {
            try {
                ret = mIHwEidPlugin.eid_get_image(transpotCounter, encryption_method, certificate, certificate_len, image, image_len, de_skey, de_skey_len);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException when eid get image jni is invoked");
                return RET_EXCEPTION_WHEN_GET_IMAGE_CALL;
            }
        }
        return ret;
    }

    public int eid_get_unsec_image(byte[] src_image, int src_image_len, int transpotCounter, int encryption_method, byte[] certificate, int certificate_len, byte[] image, int[] image_len, byte[] de_skey, int[] de_skey_len) {
        int ret = RET_DEFAULT_ERROR_VALUE;
        if (getHwEidPlugin() != null) {
            try {
                ret = mIHwEidPlugin.eid_get_unsec_image(src_image, src_image_len, transpotCounter, encryption_method, certificate, certificate_len, image, image_len, de_skey, de_skey_len);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException when eid_get unsec image is invoked" + e);
                e.printStackTrace();
                return RET_EXCEPTION_WHEN_GET_UNSEC_IMAGE_CALL;
            }
        }
        return ret;
    }

    public int eid_get_certificate_request_message(byte[] request_message, int[] message_len) {
        int ret = RET_DEFAULT_ERROR_VALUE;
        if (getHwEidPlugin() != null) {
            try {
                ret = mIHwEidPlugin.eid_get_certificate_request_message(request_message, message_len);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException when eid get certificate request message jni is invoked");
                return RET_EXCEPTION_WHEN_GET_CERTIFICATE_CALL;
            }
        }
        return ret;
    }

    public int eid_sign_info(int transpotCounter, int encryption_method, byte[] info, int info_len, byte[] sign, int[] sign_len) {
        int ret = RET_DEFAULT_ERROR_VALUE;
        if (getHwEidPlugin() != null) {
            try {
                ret = mIHwEidPlugin.eid_sign_info(transpotCounter, encryption_method, info, info_len, sign, sign_len);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException when eid sign info is invoked");
                return RET_EXCEPTION_WHEN_GET_INFO_SIGN_CALL;
            }
        }
        return ret;
    }

    public int eid_get_identity_information(byte[] identity_info, int[] identity_info_len) {
        int ret = RET_DEFAULT_ERROR_VALUE;
        if (getHwEidPlugin() != null) {
            try {
                ret = mIHwEidPlugin.eid_get_identity_information(identity_info, identity_info_len);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException when eid get identity information jni is invoked");
                return RET_EXCEPTION_WHEN_GET_IDENTITY_CALL;
            }
        }
        return ret;
    }

    public int eid_get_face_is_changed(int cmd_id) {
        int ret = RET_DEFAULT_ERROR_VALUE;
        if (getHwEidPlugin() != null) {
            try {
                ret = mIHwEidPlugin.eid_get_face_is_changed(cmd_id);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException when eid get face is changed jni is invoked");
                return RET_EXCEPTION_WHEN_GET_FACE_CHANGED_CALL;
            }
        }
        return ret;
    }

    public String eid_get_version() {
        String ret = null;
        if (getHwEidPlugin() != null) {
            try {
                ret = mIHwEidPlugin.eid_get_version();
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException when eid get version");
                return null;
            }
        }
        return ret;
    }
}
