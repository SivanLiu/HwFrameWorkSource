package com.huawei.android.view;

import android.graphics.Rect;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.IHwRotateObserver;
import java.util.List;

public interface IHwWindowManager extends IInterface {

    public static abstract class Stub extends Binder implements IHwWindowManager {
        private static final String DESCRIPTOR = "com.huawei.android.view.IHwWindowManager";
        static final int TRANSACTION_getAppUseNotchMode = 7;
        static final int TRANSACTION_getCurrFocusedWinInExtDisplay = 13;
        static final int TRANSACTION_getDeviceMaxRatio = 2;
        static final int TRANSACTION_getFocusWindowWidth = 10;
        static final int TRANSACTION_getForegroundTaskSnapshotWrapper = 16;
        static final int TRANSACTION_getNotchSystemApps = 6;
        static final int TRANSACTION_getTopAppDisplayBounds = 3;
        static final int TRANSACTION_getVisibleWindows = 9;
        static final int TRANSACTION_hasLighterViewInPCCastMode = 14;
        static final int TRANSACTION_isFullScreenDevice = 1;
        static final int TRANSACTION_registerRotateObserver = 4;
        static final int TRANSACTION_registerWMMonitorCallback = 8;
        static final int TRANSACTION_setGestureNavMode = 17;
        static final int TRANSACTION_shouldDropMotionEventForTouchPad = 15;
        static final int TRANSACTION_startNotifyWindowFocusChange = 11;
        static final int TRANSACTION_stopNotifyWindowFocusChange = 12;
        static final int TRANSACTION_unregisterRotateObserver = 5;

        private static class Proxy implements IHwWindowManager {
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

            public boolean isFullScreenDevice() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(1, _data, _reply, 0);
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

            public float getDeviceMaxRatio() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    float _result = _reply.readFloat();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public Rect getTopAppDisplayBounds(float appMaxRatio, int rotation) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Rect _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeFloat(appMaxRatio);
                    _data.writeInt(rotation);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Rect) Rect.CREATOR.createFromParcel(_reply);
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

            public void registerRotateObserver(IHwRotateObserver observer) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(observer != null ? observer.asBinder() : null);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void unregisterRotateObserver(IHwRotateObserver observer) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(observer != null ? observer.asBinder() : null);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<String> getNotchSystemApps() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getAppUseNotchMode(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean registerWMMonitorCallback(IHwWMDAMonitorCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    boolean z = false;
                    this.mRemote.transact(8, _data, _reply, 0);
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

            public List<Bundle> getVisibleWindows(int ops) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(ops);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    List<Bundle> _result = _reply.createTypedArrayList(Bundle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getFocusWindowWidth() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void startNotifyWindowFocusChange() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void stopNotifyWindowFocusChange() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void getCurrFocusedWinInExtDisplay(Bundle outBundle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        outBundle.readFromParcel(_reply);
                    }
                    _reply.recycle();
                    _data.recycle();
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean hasLighterViewInPCCastMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(14, _data, _reply, 0);
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

            public boolean shouldDropMotionEventForTouchPad(float x, float y) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeFloat(x);
                    _data.writeFloat(y);
                    boolean z = false;
                    this.mRemote.transact(15, _data, _reply, 0);
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

            public HwTaskSnapshotWrapper getForegroundTaskSnapshotWrapper(boolean refresh) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    HwTaskSnapshotWrapper _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(refresh);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (HwTaskSnapshotWrapper) HwTaskSnapshotWrapper.CREATOR.createFromParcel(_reply);
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

            public void setGestureNavMode(String packageName, int leftMode, int rightMode, int bottomMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(leftMode);
                    _data.writeInt(rightMode);
                    _data.writeInt(bottomMode);
                    this.mRemote.transact(17, _data, _reply, 0);
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

        public static IHwWindowManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IHwWindowManager)) {
                return new Proxy(obj);
            }
            return (IHwWindowManager) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != 1598968902) {
                boolean _result;
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        _result = isFullScreenDevice();
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        float _result2 = getDeviceMaxRatio();
                        reply.writeNoException();
                        reply.writeFloat(_result2);
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        Rect _result3 = getTopAppDisplayBounds(data.readFloat(), data.readInt());
                        reply.writeNoException();
                        if (_result3 != null) {
                            reply.writeInt(1);
                            _result3.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 4:
                        data.enforceInterface(descriptor);
                        registerRotateObserver(android.view.IHwRotateObserver.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 5:
                        data.enforceInterface(descriptor);
                        unregisterRotateObserver(android.view.IHwRotateObserver.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 6:
                        data.enforceInterface(descriptor);
                        List<String> _result4 = getNotchSystemApps();
                        reply.writeNoException();
                        reply.writeStringList(_result4);
                        return true;
                    case 7:
                        data.enforceInterface(descriptor);
                        int _result5 = getAppUseNotchMode(data.readString());
                        reply.writeNoException();
                        reply.writeInt(_result5);
                        return true;
                    case 8:
                        data.enforceInterface(descriptor);
                        boolean _result6 = registerWMMonitorCallback(com.huawei.android.view.IHwWMDAMonitorCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        reply.writeInt(_result6);
                        return true;
                    case 9:
                        data.enforceInterface(descriptor);
                        List<Bundle> _result7 = getVisibleWindows(data.readInt());
                        reply.writeNoException();
                        reply.writeTypedList(_result7);
                        return true;
                    case 10:
                        data.enforceInterface(descriptor);
                        int _result8 = getFocusWindowWidth();
                        reply.writeNoException();
                        reply.writeInt(_result8);
                        return true;
                    case 11:
                        data.enforceInterface(descriptor);
                        startNotifyWindowFocusChange();
                        reply.writeNoException();
                        return true;
                    case 12:
                        data.enforceInterface(descriptor);
                        stopNotifyWindowFocusChange();
                        reply.writeNoException();
                        return true;
                    case 13:
                        data.enforceInterface(descriptor);
                        Bundle _arg0 = new Bundle();
                        getCurrFocusedWinInExtDisplay(_arg0);
                        reply.writeNoException();
                        reply.writeInt(1);
                        _arg0.writeToParcel(reply, 1);
                        return true;
                    case 14:
                        data.enforceInterface(descriptor);
                        _result = hasLighterViewInPCCastMode();
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 15:
                        data.enforceInterface(descriptor);
                        boolean _result9 = shouldDropMotionEventForTouchPad(data.readFloat(), data.readFloat());
                        reply.writeNoException();
                        reply.writeInt(_result9);
                        return true;
                    case 16:
                        data.enforceInterface(descriptor);
                        HwTaskSnapshotWrapper _result10 = getForegroundTaskSnapshotWrapper(data.readInt() != 0);
                        reply.writeNoException();
                        if (_result10 != null) {
                            reply.writeInt(1);
                            _result10.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 17:
                        data.enforceInterface(descriptor);
                        setGestureNavMode(data.readString(), data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    int getAppUseNotchMode(String str) throws RemoteException;

    void getCurrFocusedWinInExtDisplay(Bundle bundle) throws RemoteException;

    float getDeviceMaxRatio() throws RemoteException;

    int getFocusWindowWidth() throws RemoteException;

    HwTaskSnapshotWrapper getForegroundTaskSnapshotWrapper(boolean z) throws RemoteException;

    List<String> getNotchSystemApps() throws RemoteException;

    Rect getTopAppDisplayBounds(float f, int i) throws RemoteException;

    List<Bundle> getVisibleWindows(int i) throws RemoteException;

    boolean hasLighterViewInPCCastMode() throws RemoteException;

    boolean isFullScreenDevice() throws RemoteException;

    void registerRotateObserver(IHwRotateObserver iHwRotateObserver) throws RemoteException;

    boolean registerWMMonitorCallback(IHwWMDAMonitorCallback iHwWMDAMonitorCallback) throws RemoteException;

    void setGestureNavMode(String str, int i, int i2, int i3) throws RemoteException;

    boolean shouldDropMotionEventForTouchPad(float f, float f2) throws RemoteException;

    void startNotifyWindowFocusChange() throws RemoteException;

    void stopNotifyWindowFocusChange() throws RemoteException;

    void unregisterRotateObserver(IHwRotateObserver iHwRotateObserver) throws RemoteException;
}
