package com.android.internal.telephony.imsphone;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.Rlog;

public class HwImsPhoneMmiCode {
    private static final int CODE_IS_UNSUPPORT_MMI_CODE = 3001;
    private static final String DESCRIPTOR = "com.android.ims.internal.IImsConfig";
    private static final String TAG = "HwImsPhoneMmiCode";

    public static void isUnSupportMMICode(String mmiCode, int phoneId) {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = ServiceManager.getService("ims_config");
        Rlog.d(TAG, "isUnSupportMMICode");
        boolean z = false;
        boolean isUnSupport = false;
        if (b != null) {
            try {
                _data.writeInterfaceToken(DESCRIPTOR);
                _data.writeInt(phoneId);
                _data.writeString(mmiCode);
                b.transact(CODE_IS_UNSUPPORT_MMI_CODE, _data, _reply, 0);
                _reply.readException();
                if (_reply.readInt() == 1) {
                    z = true;
                }
                isUnSupport = z;
            } catch (RemoteException localRemoteException) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("RemoteException is ");
                stringBuilder.append(localRemoteException);
                Rlog.d(str, stringBuilder.toString());
            } catch (Throwable th) {
                _reply.recycle();
                _data.recycle();
            }
        } else {
            Rlog.e(TAG, "isUnSupportMMICode - can't get ims_config service");
        }
        _reply.recycle();
        _data.recycle();
        if (isUnSupport) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Not Support MMI Code=");
            stringBuilder2.append(mmiCode);
            Rlog.d(TAG, stringBuilder2.toString());
            throw new RuntimeException("Invalid or Unsupported MMI Code");
        }
    }
}
