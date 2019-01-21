package android.emcom;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IEmcomManager extends IInterface {

    public static abstract class Stub extends Binder implements IEmcomManager {
        private static final String DESCRIPTOR = "android.emcom.IEmcomManager";
        static final int TRANSACTION_accelerate = 6;
        static final int TRANSACTION_accelerateWithMainCardServiceStatus = 8;
        static final int TRANSACTION_activeSlice = 13;
        static final int TRANSACTION_deactiveSlice = 14;
        static final int TRANSACTION_getAppInfo = 5;
        static final int TRANSACTION_getRuntimeInfo = 16;
        static final int TRANSACTION_getSmartcareData = 11;
        static final int TRANSACTION_isSmartMpEnable = 22;
        static final int TRANSACTION_notifyAppData = 4;
        static final int TRANSACTION_notifyEmailData = 2;
        static final int TRANSACTION_notifyHandoffDataEvent = 20;
        static final int TRANSACTION_notifyHandoffServiceStart = 18;
        static final int TRANSACTION_notifyHandoffServiceStop = 23;
        static final int TRANSACTION_notifyHandoffStateChg = 19;
        static final int TRANSACTION_notifyHwAppData = 3;
        static final int TRANSACTION_notifyRunningStatus = 10;
        static final int TRANSACTION_notifySmartMp = 21;
        static final int TRANSACTION_notifyVideoData = 1;
        static final int TRANSACTION_registerAppCallback = 12;
        static final int TRANSACTION_registerHandoff = 17;
        static final int TRANSACTION_responseForParaUpgrade = 7;
        static final int TRANSACTION_updateAppExperienceStatus = 9;
        static final int TRANSACTION_updateAppInfo = 15;

        private static class Proxy implements IEmcomManager {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            public void notifyVideoData(VideoInfo info) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (info != null) {
                        _data.writeInt(1);
                        info.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyEmailData(EmailInfo info) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (info != null) {
                        _data.writeInt(1);
                        info.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyHwAppData(String module, String pkgName, String info) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(module);
                    _data.writeString(pkgName);
                    _data.writeString(info);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyAppData(String info) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(info);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public XEngineAppInfo getAppInfo(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    XEngineAppInfo _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (XEngineAppInfo) XEngineAppInfo.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void accelerate(String packageName, int grade) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(grade);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void responseForParaUpgrade(int paratype, int pathtype, int result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(paratype);
                    _data.writeInt(pathtype);
                    _data.writeInt(result);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void accelerateWithMainCardServiceStatus(String packageName, int grade, int mainCardPsStatus) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(grade);
                    _data.writeInt(mainCardPsStatus);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void updateAppExperienceStatus(int uid, int experience, int rrt) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(experience);
                    _data.writeInt(rrt);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyRunningStatus(int type, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(type);
                    _data.writeString(packageName);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getSmartcareData(String module, String pkgName, String jsonStr) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(module);
                    _data.writeString(pkgName);
                    _data.writeString(jsonStr);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int registerAppCallback(String packageName, ISliceSdkCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void activeSlice(String packageName, String version, int sessionNumber, String serverList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(version);
                    _data.writeInt(sessionNumber);
                    _data.writeString(serverList);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void deactiveSlice(String packageName, String version, int sessionNumber, String saId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(version);
                    _data.writeInt(sessionNumber);
                    _data.writeString(saId);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void updateAppInfo(String packageName, String version, int sessionNumber, String saId, String appInfoJson) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(version);
                    _data.writeInt(sessionNumber);
                    _data.writeString(saId);
                    _data.writeString(appInfoJson);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getRuntimeInfo(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int registerHandoff(String packageName, int dataType, IHandoffSdkCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(dataType);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int notifyHandoffServiceStart(IHandoffServiceCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyHandoffStateChg(int state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(state);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int notifyHandoffDataEvent(String packageName, String para) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(para);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifySmartMp(int status) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(status);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isSmartMpEnable() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int notifyHandoffServiceStop() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IEmcomManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IEmcomManager)) {
                return new Proxy(obj);
            }
            return (IEmcomManager) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            Parcel parcel2 = reply;
            String descriptor = DESCRIPTOR;
            if (i != 1598968902) {
                VideoInfo _arg0 = null;
                int _result;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (VideoInfo) VideoInfo.CREATOR.createFromParcel(parcel);
                        }
                        notifyVideoData(_arg0);
                        reply.writeNoException();
                        return true;
                    case 2:
                        EmailInfo _arg02;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (EmailInfo) EmailInfo.CREATOR.createFromParcel(parcel);
                        }
                        notifyEmailData(_arg02);
                        reply.writeNoException();
                        return true;
                    case 3:
                        parcel.enforceInterface(descriptor);
                        notifyHwAppData(data.readString(), data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        notifyAppData(data.readString());
                        reply.writeNoException();
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        XEngineAppInfo _result2 = getAppInfo(data.readString());
                        reply.writeNoException();
                        if (_result2 != null) {
                            parcel2.writeInt(1);
                            _result2.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        accelerate(data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        responseForParaUpgrade(data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        accelerateWithMainCardServiceStatus(data.readString(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        updateAppExperienceStatus(data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 10:
                        parcel.enforceInterface(descriptor);
                        notifyRunningStatus(data.readInt(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        String _result3 = getSmartcareData(data.readString(), data.readString(), data.readString());
                        reply.writeNoException();
                        parcel2.writeString(_result3);
                        return true;
                    case 12:
                        parcel.enforceInterface(descriptor);
                        _result = registerAppCallback(data.readString(), android.emcom.ISliceSdkCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 13:
                        parcel.enforceInterface(descriptor);
                        activeSlice(data.readString(), data.readString(), data.readInt(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        deactiveSlice(data.readString(), data.readString(), data.readInt(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        updateAppInfo(data.readString(), data.readString(), data.readInt(), data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 16:
                        parcel.enforceInterface(descriptor);
                        String _result4 = getRuntimeInfo(data.readString());
                        reply.writeNoException();
                        parcel2.writeString(_result4);
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        int _result5 = registerHandoff(data.readString(), data.readInt(), android.emcom.IHandoffSdkCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case 18:
                        parcel.enforceInterface(descriptor);
                        int _result6 = notifyHandoffServiceStart(android.emcom.IHandoffServiceCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return true;
                    case 19:
                        parcel.enforceInterface(descriptor);
                        notifyHandoffStateChg(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 20:
                        parcel.enforceInterface(descriptor);
                        _result = notifyHandoffDataEvent(data.readString(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 21:
                        parcel.enforceInterface(descriptor);
                        notifySmartMp(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 22:
                        parcel.enforceInterface(descriptor);
                        boolean _result7 = isSmartMpEnable();
                        reply.writeNoException();
                        parcel2.writeInt(_result7);
                        return true;
                    case 23:
                        parcel.enforceInterface(descriptor);
                        int _result8 = notifyHandoffServiceStop();
                        reply.writeNoException();
                        parcel2.writeInt(_result8);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            parcel2.writeString(descriptor);
            return true;
        }
    }

    void accelerate(String str, int i) throws RemoteException;

    void accelerateWithMainCardServiceStatus(String str, int i, int i2) throws RemoteException;

    void activeSlice(String str, String str2, int i, String str3) throws RemoteException;

    void deactiveSlice(String str, String str2, int i, String str3) throws RemoteException;

    XEngineAppInfo getAppInfo(String str) throws RemoteException;

    String getRuntimeInfo(String str) throws RemoteException;

    String getSmartcareData(String str, String str2, String str3) throws RemoteException;

    boolean isSmartMpEnable() throws RemoteException;

    void notifyAppData(String str) throws RemoteException;

    void notifyEmailData(EmailInfo emailInfo) throws RemoteException;

    int notifyHandoffDataEvent(String str, String str2) throws RemoteException;

    int notifyHandoffServiceStart(IHandoffServiceCallback iHandoffServiceCallback) throws RemoteException;

    int notifyHandoffServiceStop() throws RemoteException;

    void notifyHandoffStateChg(int i) throws RemoteException;

    void notifyHwAppData(String str, String str2, String str3) throws RemoteException;

    void notifyRunningStatus(int i, String str) throws RemoteException;

    void notifySmartMp(int i) throws RemoteException;

    void notifyVideoData(VideoInfo videoInfo) throws RemoteException;

    int registerAppCallback(String str, ISliceSdkCallback iSliceSdkCallback) throws RemoteException;

    int registerHandoff(String str, int i, IHandoffSdkCallback iHandoffSdkCallback) throws RemoteException;

    void responseForParaUpgrade(int i, int i2, int i3) throws RemoteException;

    void updateAppExperienceStatus(int i, int i2, int i3) throws RemoteException;

    void updateAppInfo(String str, String str2, int i, String str3, String str4) throws RemoteException;
}
