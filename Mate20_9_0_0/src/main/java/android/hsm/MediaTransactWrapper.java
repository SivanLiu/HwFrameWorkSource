package android.hsm;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import java.lang.ref.SoftReference;
import java.util.HashSet;
import java.util.Set;

public class MediaTransactWrapper {
    private static final boolean DEBUG = false;
    private static final String TAG = "MediaTransactWrapper";
    private static SoftReference<IBinder> mBinder = null;

    public static void musicPlaying(int uid, int pid) {
        String str;
        StringBuilder stringBuilder;
        Parcel data = null;
        Parcel reply = null;
        try {
            data = Parcel.obtain();
            reply = Parcel.obtain();
            IBinder b = getBinder();
            if (b != null) {
                data.writeInterfaceToken(HsmTransactExt.INTERFACE_DESCRIPTOR);
                data.writeInt(0);
                data.writeInt(uid);
                data.writeInt(pid);
                b.transact(102, data, reply, 0);
                reply.readException();
                int readInt = reply.readInt();
            }
        } catch (RemoteException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("musicPlaying transact catch remote exception: ");
            stringBuilder.append(e.toString());
            Log.e(str, stringBuilder.toString());
        } catch (Exception e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("musicPlaying transact catch exception: ");
            stringBuilder.append(e2.toString());
            Log.e(str, stringBuilder.toString());
            e2.printStackTrace();
        } catch (Throwable th) {
            recycleParcel(null);
            recycleParcel(null);
        }
        recycleParcel(data);
        recycleParcel(reply);
    }

    public static void musicPausedOrStopped(int uid, int pid) {
        String str;
        StringBuilder stringBuilder;
        Parcel data = null;
        Parcel reply = null;
        try {
            data = Parcel.obtain();
            reply = Parcel.obtain();
            IBinder b = getBinder();
            if (b != null) {
                data.writeInterfaceToken(HsmTransactExt.INTERFACE_DESCRIPTOR);
                data.writeInt(1);
                data.writeInt(uid);
                data.writeInt(pid);
                b.transact(102, data, reply, 0);
                reply.readException();
                int readInt = reply.readInt();
            }
        } catch (RemoteException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("musicPausedOrStopped transact catch remote exception: ");
            stringBuilder.append(e.toString());
            Log.e(str, stringBuilder.toString());
        } catch (Exception e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("musicPausedOrStopped transact catch exception: ");
            stringBuilder.append(e2.toString());
            Log.e(str, stringBuilder.toString());
            e2.printStackTrace();
        } catch (Throwable th) {
            recycleParcel(null);
            recycleParcel(null);
        }
        recycleParcel(data);
        recycleParcel(reply);
    }

    public static Set<Integer> playingMusicUidSet() {
        Set<Integer> result = new HashSet();
        String strUids = playingMusicUidStr();
        if (!(strUids == null || strUids.isEmpty())) {
            String[] ids = strUids.split("\\|");
            if (ids != null) {
                for (String valueOf : ids) {
                    result.add(Integer.valueOf(valueOf));
                }
            }
        }
        return result;
    }

    private static String playingMusicUidStr() {
        String str;
        StringBuilder stringBuilder;
        Parcel data = null;
        Parcel reply = null;
        try {
            data = Parcel.obtain();
            reply = Parcel.obtain();
            IBinder b = getBinder();
            if (b != null) {
                data.writeInterfaceToken(HsmTransactExt.INTERFACE_DESCRIPTOR);
                b.transact(103, data, reply, 0);
                reply.readException();
                String result = reply.readString();
                recycleParcel(data);
                recycleParcel(reply);
                return result;
            }
        } catch (RemoteException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("musicPausedOrStopped transact catch remote exception: ");
            stringBuilder.append(e.toString());
            Log.e(str, stringBuilder.toString());
        } catch (Exception e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("musicPausedOrStopped transact catch exception: ");
            stringBuilder.append(e2.toString());
            Log.e(str, stringBuilder.toString());
            e2.printStackTrace();
        } catch (Throwable th) {
            recycleParcel(null);
            recycleParcel(reply);
        }
        recycleParcel(data);
        recycleParcel(reply);
        return null;
    }

    private static synchronized IBinder getBinder() {
        IBinder iBinder;
        synchronized (MediaTransactWrapper.class) {
            if (mBinder == null || mBinder.get() == null) {
                mBinder = new SoftReference(ServiceManager.getService(HsmTransactExt.SERVICE_NAME));
            }
            iBinder = (IBinder) mBinder.get();
        }
        return iBinder;
    }

    private static void recycleParcel(Parcel p) {
        if (p != null) {
            p.recycle();
        }
    }
}
