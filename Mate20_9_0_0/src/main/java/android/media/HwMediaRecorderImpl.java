package android.media;

import android.app.ActivityThread;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

public class HwMediaRecorderImpl implements IHwMediaRecorder {
    private static final String TAG = "HwMediaRecorderImpl";
    private static IBinder mAudioService = null;
    private static IHwMediaRecorder mHwMediaRecoder = new HwMediaRecorderImpl();

    private static IBinder getAudioService() {
        if (mAudioService != null) {
            return mAudioService;
        }
        mAudioService = ServiceManager.getService("audio");
        return mAudioService;
    }

    private HwMediaRecorderImpl() {
        Log.i(TAG, TAG);
    }

    public static IHwMediaRecorder getDefault() {
        return mHwMediaRecoder;
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
