package com.huawei.hsm;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

public class MediaTransactWrapperEx {
    private static final String TAG = "MediaTransactWrapperEx";

    public static boolean registerMusicObserver(IHsmMusicWatch observer) {
        if (observer != null) {
            return operateMusicObserver(observer);
        }
        Log.e(TAG, "registerMusicObserver ->> register null observer not allowed.");
        return false;
    }

    public static boolean unregisterMusicObserver() {
        return operateMusicObserver(null);
    }

    private static boolean operateMusicObserver(IHsmMusicWatch observer) {
        String str;
        StringBuilder stringBuilder;
        boolean retVal = true;
        boolean register = true;
        if (observer == null) {
            register = false;
        }
        Parcel data = null;
        Parcel reply = null;
        try {
            data = Parcel.obtain();
            reply = Parcel.obtain();
            IBinder b = getBinder();
            if (b != null) {
                data.writeInterfaceToken("com.huawei.hsm.IHsmCoreService");
                boolean z = true;
                data.writeInt(register ? 0 : 1);
                data.writeInt(Process.myPid());
                if (observer != null) {
                    data.writeStrongBinder(observer.asBinder());
                }
                b.transact(104, data, reply, 0);
                reply.readException();
                if (1 != reply.readInt()) {
                    z = false;
                }
                retVal = z;
            }
        } catch (RemoteException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("operateMusicObserver transact catch remote exception: ");
            stringBuilder.append(e.toString());
            stringBuilder.append("when register: ");
            stringBuilder.append(register);
            Log.e(str, stringBuilder.toString());
        } catch (Exception e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("operateMusicObserver transact catch exception: ");
            stringBuilder.append(e2.toString());
            stringBuilder.append("when register: ");
            stringBuilder.append(register);
            Log.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            recycleParcel(null);
            recycleParcel(null);
        }
        recycleParcel(data);
        recycleParcel(reply);
        return retVal;
    }

    private static synchronized IBinder getBinder() {
        IBinder service;
        synchronized (MediaTransactWrapperEx.class) {
            service = ServiceManager.getService("system.hsmcore");
        }
        return service;
    }

    private static void recycleParcel(Parcel p) {
        if (p != null) {
            p.recycle();
        }
    }
}
