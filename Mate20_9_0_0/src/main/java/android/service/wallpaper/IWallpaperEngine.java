package android.service.wallpaper;

import android.graphics.Rect;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.MotionEvent;

public interface IWallpaperEngine extends IInterface {

    public static abstract class Stub extends Binder implements IWallpaperEngine {
        private static final String DESCRIPTOR = "android.service.wallpaper.IWallpaperEngine";
        static final int TRANSACTION_destroy = 8;
        static final int TRANSACTION_dispatchPointer = 5;
        static final int TRANSACTION_dispatchWallpaperCommand = 6;
        static final int TRANSACTION_requestWallpaperColors = 7;
        static final int TRANSACTION_setDesiredSize = 1;
        static final int TRANSACTION_setDisplayPadding = 2;
        static final int TRANSACTION_setInAmbientMode = 4;
        static final int TRANSACTION_setVisibility = 3;

        private static class Proxy implements IWallpaperEngine {
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

            public void setDesiredSize(int width, int height) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(width);
                    _data.writeInt(height);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void setDisplayPadding(Rect padding) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (padding != null) {
                        _data.writeInt(1);
                        padding.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void setVisibility(boolean visible) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(visible);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void setInAmbientMode(boolean inAmbientDisplay, boolean animated) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(inAmbientDisplay);
                    _data.writeInt(animated);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void dispatchPointer(MotionEvent event) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (event != null) {
                        _data.writeInt(1);
                        event.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void dispatchWallpaperCommand(String action, int x, int y, int z, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(action);
                    _data.writeInt(x);
                    _data.writeInt(y);
                    _data.writeInt(z);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void requestWallpaperColors() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void destroy() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IWallpaperEngine asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IWallpaperEngine)) {
                return new Proxy(obj);
            }
            return (IWallpaperEngine) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            String descriptor = DESCRIPTOR;
            if (i != 1598968902) {
                boolean _arg1 = false;
                Rect _arg0 = null;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(descriptor);
                        setDesiredSize(data.readInt(), data.readInt());
                        return true;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (Rect) Rect.CREATOR.createFromParcel(parcel);
                        }
                        setDisplayPadding(_arg0);
                        return true;
                    case 3:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        setVisibility(_arg1);
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        boolean _arg02 = data.readInt() != 0;
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        setInAmbientMode(_arg02, _arg1);
                        return true;
                    case 5:
                        MotionEvent _arg03;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (MotionEvent) MotionEvent.CREATOR.createFromParcel(parcel);
                        }
                        dispatchPointer(_arg03);
                        return true;
                    case 6:
                        Bundle _arg4;
                        parcel.enforceInterface(descriptor);
                        String _arg04 = data.readString();
                        int _arg12 = data.readInt();
                        int _arg2 = data.readInt();
                        int _arg3 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg4 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg4 = null;
                        }
                        dispatchWallpaperCommand(_arg04, _arg12, _arg2, _arg3, _arg4);
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        requestWallpaperColors();
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        destroy();
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    void destroy() throws RemoteException;

    void dispatchPointer(MotionEvent motionEvent) throws RemoteException;

    void dispatchWallpaperCommand(String str, int i, int i2, int i3, Bundle bundle) throws RemoteException;

    void requestWallpaperColors() throws RemoteException;

    void setDesiredSize(int i, int i2) throws RemoteException;

    void setDisplayPadding(Rect rect) throws RemoteException;

    void setInAmbientMode(boolean z, boolean z2) throws RemoteException;

    void setVisibility(boolean z) throws RemoteException;
}
