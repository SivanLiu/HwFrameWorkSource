package android.media;

import android.app.ActivityThread;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.android.internal.app.IAppOpsService;
import com.android.internal.app.IAppOpsService.Stub;

public class HwAudioRecordImpl implements IHwAudioRecord {
    private static final String TAG = "HwAudioRecordImpl";
    private static IBinder mAudioService = null;
    private static IHwAudioRecord mHwAudioRecoder = new HwAudioRecordImpl();
    private IAppOpsService mAppOps;

    private HwAudioRecordImpl() {
        Log.i(TAG, TAG);
    }

    public static IHwAudioRecord getDefault() {
        return mHwAudioRecoder;
    }

    public void sendStateChangedIntent(int state) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sendStateChangedIntent, state=");
        stringBuilder.append(state);
        Log.i(str, stringBuilder.toString());
        IBinder b = getAudioService();
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        try {
            _data.writeInterfaceToken("android.media.IAudioService");
            _data.writeString(MediaRecorder.class.getSimpleName());
            _data.writeInt(state);
            _data.writeInt(Process.myPid());
            _data.writeString(ActivityThread.currentPackageName());
            if (b != null) {
                b.transact(1003, _data, _reply, 0);
            }
            _reply.readException();
        } catch (RemoteException e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("sendStateChangedIntent transact e: ");
            stringBuilder2.append(e);
            Log.e(str2, stringBuilder2.toString());
            e.printStackTrace();
        } catch (Throwable th) {
            _reply.recycle();
            _data.recycle();
        }
        _reply.recycle();
        _data.recycle();
    }

    private static IBinder getAudioService() {
        if (mAudioService != null) {
            return mAudioService;
        }
        mAudioService = ServiceManager.getService("audio");
        return mAudioService;
    }

    public boolean isAudioRecordAllowed() {
        String packageName = ActivityThread.currentPackageName();
        if (this.mAppOps == null) {
            this.mAppOps = Stub.asInterface(ServiceManager.getService("appops"));
        }
        boolean z = true;
        if (this.mAppOps == null) {
            return true;
        }
        try {
            if (this.mAppOps.noteOperation(27, Process.myUid(), packageName) != 0) {
                z = false;
            }
            return z;
        } catch (RemoteException e) {
            throw new SecurityException("Unable to noteOperation", e);
        }
    }

    public void showDisableMicrophoneToast() {
        Log.i(TAG, "showDisableMicrophoneToast");
        IBinder b = getAudioService();
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        try {
            _data.writeInterfaceToken("android.media.IAudioService");
            if (b != null) {
                b.transact(1005, _data, _reply, 0);
            }
            _reply.readException();
        } catch (RemoteException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("showDisableMicrophoneToast transact e: ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
        } catch (Throwable th) {
            _reply.recycle();
            _data.recycle();
        }
        _reply.recycle();
        _data.recycle();
    }
}
