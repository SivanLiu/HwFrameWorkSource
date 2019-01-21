package android.media.session;

import android.content.ComponentName;
import android.media.IRemoteVolumeController;
import android.media.ISessionTokensListener;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.KeyEvent;
import java.util.List;

public interface ISessionManager extends IInterface {

    public static abstract class Stub extends Binder implements ISessionManager {
        private static final String DESCRIPTOR = "android.media.session.ISessionManager";
        static final int TRANSACTION_addSessionTokensListener = 17;
        static final int TRANSACTION_addSessionsListener = 6;
        static final int TRANSACTION_createSession = 1;
        static final int TRANSACTION_createSession2 = 14;
        static final int TRANSACTION_destroySession2 = 15;
        static final int TRANSACTION_dispatchAdjustVolume = 5;
        static final int TRANSACTION_dispatchMediaKeyEvent = 3;
        static final int TRANSACTION_dispatchVolumeKeyEvent = 4;
        static final int TRANSACTION_getSessionTokens = 16;
        static final int TRANSACTION_getSessions = 2;
        static final int TRANSACTION_isGlobalPriorityActive = 9;
        static final int TRANSACTION_isTrusted = 13;
        static final int TRANSACTION_removeSessionTokensListener = 18;
        static final int TRANSACTION_removeSessionsListener = 7;
        static final int TRANSACTION_setCallback = 10;
        static final int TRANSACTION_setOnMediaKeyListener = 12;
        static final int TRANSACTION_setOnVolumeKeyLongPressListener = 11;
        static final int TRANSACTION_setRemoteVolumeController = 8;

        private static class Proxy implements ISessionManager {
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

            public ISession createSession(String packageName, ISessionCallback cb, String tag, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeStrongBinder(cb != null ? cb.asBinder() : null);
                    _data.writeString(tag);
                    _data.writeInt(userId);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    ISession _result = android.media.session.ISession.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<IBinder> getSessions(ComponentName compName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (compName != null) {
                        _data.writeInt(1);
                        compName.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userId);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    List<IBinder> _result = _reply.createBinderArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void dispatchMediaKeyEvent(String packageName, boolean asSystemService, KeyEvent keyEvent, boolean needWakeLock) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(asSystemService);
                    if (keyEvent != null) {
                        _data.writeInt(1);
                        keyEvent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(needWakeLock);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void dispatchVolumeKeyEvent(String packageName, boolean asSystemService, KeyEvent keyEvent, int stream, boolean musicOnly) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(asSystemService);
                    if (keyEvent != null) {
                        _data.writeInt(1);
                        keyEvent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(stream);
                    _data.writeInt(musicOnly);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void dispatchAdjustVolume(String packageName, int suggestedStream, int delta, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(suggestedStream);
                    _data.writeInt(delta);
                    _data.writeInt(flags);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void addSessionsListener(IActiveSessionsListener listener, ComponentName compName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    if (compName != null) {
                        _data.writeInt(1);
                        compName.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userId);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeSessionsListener(IActiveSessionsListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setRemoteVolumeController(IRemoteVolumeController rvc) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(rvc != null ? rvc.asBinder() : null);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isGlobalPriorityActive() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(9, _data, _reply, 0);
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

            public void setCallback(ICallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setOnVolumeKeyLongPressListener(IOnVolumeKeyLongPressListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setOnMediaKeyListener(IOnMediaKeyListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isTrusted(String controllerPackageName, int controllerPid, int controllerUid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(controllerPackageName);
                    _data.writeInt(controllerPid);
                    _data.writeInt(controllerUid);
                    boolean z = false;
                    this.mRemote.transact(13, _data, _reply, 0);
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

            public boolean createSession2(Bundle sessionToken) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (sessionToken != null) {
                        _data.writeInt(1);
                        sessionToken.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void destroySession2(Bundle sessionToken) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (sessionToken != null) {
                        _data.writeInt(1);
                        sessionToken.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<Bundle> getSessionTokens(boolean activeSessionOnly, boolean sessionServiceOnly, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(activeSessionOnly);
                    _data.writeInt(sessionServiceOnly);
                    _data.writeString(packageName);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    List<Bundle> _result = _reply.createTypedArrayList(Bundle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void addSessionTokensListener(ISessionTokensListener listener, int userId, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    _data.writeInt(userId);
                    _data.writeString(packageName);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeSessionTokensListener(ISessionTokensListener listener, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    _data.writeString(packageName);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ISessionManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof ISessionManager)) {
                return new Proxy(obj);
            }
            return (ISessionManager) iin;
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
                boolean _arg1 = false;
                IBinder _arg0 = null;
                ComponentName _arg02;
                boolean _arg12;
                Bundle _arg03;
                boolean _result;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(descriptor);
                        ISession _result2 = createSession(data.readString(), android.media.session.ISessionCallback.Stub.asInterface(data.readStrongBinder()), data.readString(), data.readInt());
                        reply.writeNoException();
                        if (_result2 != null) {
                            _arg0 = _result2.asBinder();
                        }
                        parcel2.writeStrongBinder(_arg0);
                        return true;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        List<IBinder> _result3 = getSessions(_arg02, data.readInt());
                        reply.writeNoException();
                        parcel2.writeBinderList(_result3);
                        return true;
                    case 3:
                        KeyEvent _arg2;
                        parcel.enforceInterface(descriptor);
                        String _arg04 = data.readString();
                        _arg12 = data.readInt() != 0;
                        if (data.readInt() != 0) {
                            _arg2 = (KeyEvent) KeyEvent.CREATOR.createFromParcel(parcel);
                        }
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        dispatchMediaKeyEvent(_arg04, _arg12, _arg2, _arg1);
                        reply.writeNoException();
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        String _arg05 = data.readString();
                        boolean _arg13 = data.readInt() != 0;
                        if (data.readInt() != 0) {
                            _arg02 = (KeyEvent) KeyEvent.CREATOR.createFromParcel(parcel);
                        }
                        dispatchVolumeKeyEvent(_arg05, _arg13, _arg02, data.readInt(), data.readInt() != 0);
                        reply.writeNoException();
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        dispatchAdjustVolume(data.readString(), data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        IActiveSessionsListener _arg06 = android.media.session.IActiveSessionsListener.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg02 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        addSessionsListener(_arg06, _arg02, data.readInt());
                        reply.writeNoException();
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        removeSessionsListener(android.media.session.IActiveSessionsListener.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        setRemoteVolumeController(android.media.IRemoteVolumeController.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        _arg1 = isGlobalPriorityActive();
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 10:
                        parcel.enforceInterface(descriptor);
                        setCallback(android.media.session.ICallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        setOnVolumeKeyLongPressListener(android.media.session.IOnVolumeKeyLongPressListener.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 12:
                        parcel.enforceInterface(descriptor);
                        setOnMediaKeyListener(android.media.session.IOnMediaKeyListener.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 13:
                        parcel.enforceInterface(descriptor);
                        _arg12 = isTrusted(data.readString(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg12);
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        }
                        _result = createSession2(_arg03);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        }
                        destroySession2(_arg03);
                        reply.writeNoException();
                        return true;
                    case 16:
                        parcel.enforceInterface(descriptor);
                        _result = data.readInt() != 0;
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        List<Bundle> _result4 = getSessionTokens(_result, _arg1, data.readString());
                        reply.writeNoException();
                        parcel2.writeTypedList(_result4);
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        addSessionTokensListener(android.media.ISessionTokensListener.Stub.asInterface(data.readStrongBinder()), data.readInt(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 18:
                        parcel.enforceInterface(descriptor);
                        removeSessionTokensListener(android.media.ISessionTokensListener.Stub.asInterface(data.readStrongBinder()), data.readString());
                        reply.writeNoException();
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            parcel2.writeString(descriptor);
            return true;
        }
    }

    void addSessionTokensListener(ISessionTokensListener iSessionTokensListener, int i, String str) throws RemoteException;

    void addSessionsListener(IActiveSessionsListener iActiveSessionsListener, ComponentName componentName, int i) throws RemoteException;

    ISession createSession(String str, ISessionCallback iSessionCallback, String str2, int i) throws RemoteException;

    boolean createSession2(Bundle bundle) throws RemoteException;

    void destroySession2(Bundle bundle) throws RemoteException;

    void dispatchAdjustVolume(String str, int i, int i2, int i3) throws RemoteException;

    void dispatchMediaKeyEvent(String str, boolean z, KeyEvent keyEvent, boolean z2) throws RemoteException;

    void dispatchVolumeKeyEvent(String str, boolean z, KeyEvent keyEvent, int i, boolean z2) throws RemoteException;

    List<Bundle> getSessionTokens(boolean z, boolean z2, String str) throws RemoteException;

    List<IBinder> getSessions(ComponentName componentName, int i) throws RemoteException;

    boolean isGlobalPriorityActive() throws RemoteException;

    boolean isTrusted(String str, int i, int i2) throws RemoteException;

    void removeSessionTokensListener(ISessionTokensListener iSessionTokensListener, String str) throws RemoteException;

    void removeSessionsListener(IActiveSessionsListener iActiveSessionsListener) throws RemoteException;

    void setCallback(ICallback iCallback) throws RemoteException;

    void setOnMediaKeyListener(IOnMediaKeyListener iOnMediaKeyListener) throws RemoteException;

    void setOnVolumeKeyLongPressListener(IOnVolumeKeyLongPressListener iOnVolumeKeyLongPressListener) throws RemoteException;

    void setRemoteVolumeController(IRemoteVolumeController iRemoteVolumeController) throws RemoteException;
}
