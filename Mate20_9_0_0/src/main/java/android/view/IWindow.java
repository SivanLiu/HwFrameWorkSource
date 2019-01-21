package android.view;

import android.graphics.Rect;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.MergedConfiguration;
import android.view.DisplayCutout.ParcelableWrapper;
import com.android.internal.os.IResultReceiver;

public interface IWindow extends IInterface {

    public static abstract class Stub extends Binder implements IWindow {
        private static final String DESCRIPTOR = "android.view.IWindow";
        static final int TRANSACTION_closeSystemDialogs = 7;
        static final int TRANSACTION_dispatchAppVisibility = 4;
        static final int TRANSACTION_dispatchDragEvent = 10;
        static final int TRANSACTION_dispatchGetNewSurface = 5;
        static final int TRANSACTION_dispatchPointerCaptureChanged = 15;
        static final int TRANSACTION_dispatchSystemUiVisibilityChanged = 12;
        static final int TRANSACTION_dispatchWallpaperCommand = 9;
        static final int TRANSACTION_dispatchWallpaperOffsets = 8;
        static final int TRANSACTION_dispatchWindowShown = 13;
        static final int TRANSACTION_executeCommand = 1;
        static final int TRANSACTION_moved = 3;
        static final int TRANSACTION_notifyFocusChanged = 18;
        static final int TRANSACTION_registerWindowObserver = 16;
        static final int TRANSACTION_requestAppKeyboardShortcuts = 14;
        static final int TRANSACTION_resized = 2;
        static final int TRANSACTION_unRegisterWindowObserver = 17;
        static final int TRANSACTION_updatePointerIcon = 11;
        static final int TRANSACTION_updateSurfaceStatus = 19;
        static final int TRANSACTION_windowFocusChanged = 6;

        private static class Proxy implements IWindow {
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

            public void executeCommand(String command, String parameters, ParcelFileDescriptor descriptor) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(command);
                    _data.writeString(parameters);
                    if (descriptor != null) {
                        _data.writeInt(1);
                        descriptor.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void resized(Rect frame, Rect overscanInsets, Rect contentInsets, Rect visibleInsets, Rect stableInsets, Rect outsets, boolean reportDraw, MergedConfiguration newMergedConfiguration, Rect backDropFrame, boolean forceLayout, boolean alwaysConsumeNavBar, int displayId, ParcelableWrapper displayCutout) throws RemoteException {
                Throwable th;
                boolean z;
                int i;
                boolean z2;
                Rect rect = frame;
                Rect rect2 = overscanInsets;
                Rect rect3 = contentInsets;
                Rect rect4 = visibleInsets;
                Rect rect5 = stableInsets;
                Rect rect6 = outsets;
                MergedConfiguration mergedConfiguration = newMergedConfiguration;
                Rect rect7 = backDropFrame;
                ParcelableWrapper parcelableWrapper = displayCutout;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (rect != null) {
                        _data.writeInt(1);
                        rect.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (rect2 != null) {
                        _data.writeInt(1);
                        rect2.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (rect3 != null) {
                        _data.writeInt(1);
                        rect3.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (rect4 != null) {
                        _data.writeInt(1);
                        rect4.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (rect5 != null) {
                        _data.writeInt(1);
                        rect5.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (rect6 != null) {
                        _data.writeInt(1);
                        rect6.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    try {
                        _data.writeInt(reportDraw);
                        if (mergedConfiguration != null) {
                            _data.writeInt(1);
                            mergedConfiguration.writeToParcel(_data, 0);
                        } else {
                            _data.writeInt(0);
                        }
                        if (rect7 != null) {
                            _data.writeInt(1);
                            rect7.writeToParcel(_data, 0);
                        } else {
                            _data.writeInt(0);
                        }
                        try {
                            _data.writeInt(forceLayout);
                        } catch (Throwable th2) {
                            th = th2;
                            z = alwaysConsumeNavBar;
                            i = displayId;
                            _data.recycle();
                            throw th;
                        }
                        try {
                            _data.writeInt(alwaysConsumeNavBar);
                            try {
                                _data.writeInt(displayId);
                                if (parcelableWrapper != null) {
                                    _data.writeInt(1);
                                    parcelableWrapper.writeToParcel(_data, 0);
                                } else {
                                    _data.writeInt(0);
                                }
                            } catch (Throwable th3) {
                                th = th3;
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            i = displayId;
                            _data.recycle();
                            throw th;
                        }
                        try {
                            this.mRemote.transact(2, _data, null, 1);
                            _data.recycle();
                        } catch (Throwable th5) {
                            th = th5;
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        z2 = forceLayout;
                        z = alwaysConsumeNavBar;
                        i = displayId;
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th7) {
                    th = th7;
                    boolean z3 = reportDraw;
                    z2 = forceLayout;
                    z = alwaysConsumeNavBar;
                    i = displayId;
                    _data.recycle();
                    throw th;
                }
            }

            public void moved(int newX, int newY) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(newX);
                    _data.writeInt(newY);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void dispatchAppVisibility(boolean visible) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(visible);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void dispatchGetNewSurface() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void windowFocusChanged(boolean hasFocus, boolean inTouchMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(hasFocus);
                    _data.writeInt(inTouchMode);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void closeSystemDialogs(String reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(reason);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void dispatchWallpaperOffsets(float x, float y, float xStep, float yStep, boolean sync) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeFloat(x);
                    _data.writeFloat(y);
                    _data.writeFloat(xStep);
                    _data.writeFloat(yStep);
                    _data.writeInt(sync);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void dispatchWallpaperCommand(String action, int x, int y, int z, Bundle extras, boolean sync) throws RemoteException {
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
                    _data.writeInt(sync);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void dispatchDragEvent(DragEvent event) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (event != null) {
                        _data.writeInt(1);
                        event.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void updatePointerIcon(float x, float y) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeFloat(x);
                    _data.writeFloat(y);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void dispatchSystemUiVisibilityChanged(int seq, int globalVisibility, int localValue, int localChanges) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(seq);
                    _data.writeInt(globalVisibility);
                    _data.writeInt(localValue);
                    _data.writeInt(localChanges);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void dispatchWindowShown() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void requestAppKeyboardShortcuts(IResultReceiver receiver, int deviceId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(receiver != null ? receiver.asBinder() : null);
                    _data.writeInt(deviceId);
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void dispatchPointerCaptureChanged(boolean hasCapture) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(hasCapture);
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void registerWindowObserver(IWindowLayoutObserver observer, long period) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(observer != null ? observer.asBinder() : null);
                    _data.writeLong(period);
                    this.mRemote.transact(16, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void unRegisterWindowObserver(IWindowLayoutObserver observer) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(observer != null ? observer.asBinder() : null);
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void notifyFocusChanged() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(18, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void updateSurfaceStatus(boolean status) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(status);
                    this.mRemote.transact(19, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IWindow asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IWindow)) {
                return new Proxy(obj);
            }
            return (IWindow) iin;
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
                ParcelFileDescriptor _arg0 = null;
                Parcel parcel2;
                switch (i) {
                    case 1:
                        parcel2 = parcel;
                        parcel2.enforceInterface(descriptor);
                        String _arg02 = data.readString();
                        String _arg12 = data.readString();
                        if (data.readInt() != 0) {
                            _arg0 = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(parcel2);
                        }
                        executeCommand(_arg02, _arg12, _arg0);
                        return true;
                    case 2:
                        Rect _arg03;
                        Rect _arg13;
                        Rect _arg2;
                        Rect _arg3;
                        Rect _arg4;
                        Rect _arg5;
                        MergedConfiguration _arg7;
                        Rect _arg8;
                        ParcelableWrapper _arg122;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (Rect) Rect.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg03 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg13 = (Rect) Rect.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg13 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg2 = (Rect) Rect.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg2 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg3 = (Rect) Rect.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg3 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg4 = (Rect) Rect.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg4 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg5 = (Rect) Rect.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg5 = null;
                        }
                        boolean _arg6 = data.readInt() != 0;
                        if (data.readInt() != 0) {
                            _arg7 = (MergedConfiguration) MergedConfiguration.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg7 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg8 = (Rect) Rect.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg8 = null;
                        }
                        boolean _arg9 = data.readInt() != 0;
                        boolean _arg10 = data.readInt() != 0;
                        int _arg11 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg122 = (ParcelableWrapper) ParcelableWrapper.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg122 = null;
                        }
                        ParcelableWrapper descriptor2 = _arg122;
                        parcel2 = parcel;
                        resized(_arg03, _arg13, _arg2, _arg3, _arg4, _arg5, _arg6, _arg7, _arg8, _arg9, _arg10, _arg11, descriptor2);
                        return true;
                    case 3:
                        parcel.enforceInterface(descriptor);
                        moved(data.readInt(), data.readInt());
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        dispatchAppVisibility(_arg1);
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        dispatchGetNewSurface();
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        boolean _arg04 = data.readInt() != 0;
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        windowFocusChanged(_arg04, _arg1);
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        closeSystemDialogs(data.readString());
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        dispatchWallpaperOffsets(data.readFloat(), data.readFloat(), data.readFloat(), data.readFloat(), data.readInt() != 0);
                        return true;
                    case 9:
                        Bundle _arg42;
                        parcel.enforceInterface(descriptor);
                        String _arg05 = data.readString();
                        int _arg14 = data.readInt();
                        int _arg22 = data.readInt();
                        int _arg32 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg42 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg42 = null;
                        }
                        dispatchWallpaperCommand(_arg05, _arg14, _arg22, _arg32, _arg42, data.readInt() != 0);
                        return true;
                    case 10:
                        DragEvent _arg06;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg06 = (DragEvent) DragEvent.CREATOR.createFromParcel(parcel);
                        }
                        dispatchDragEvent(_arg06);
                        return true;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        updatePointerIcon(data.readFloat(), data.readFloat());
                        return true;
                    case 12:
                        parcel.enforceInterface(descriptor);
                        dispatchSystemUiVisibilityChanged(data.readInt(), data.readInt(), data.readInt(), data.readInt());
                        return true;
                    case 13:
                        parcel.enforceInterface(descriptor);
                        dispatchWindowShown();
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        requestAppKeyboardShortcuts(com.android.internal.os.IResultReceiver.Stub.asInterface(data.readStrongBinder()), data.readInt());
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        dispatchPointerCaptureChanged(_arg1);
                        return true;
                    case 16:
                        parcel.enforceInterface(descriptor);
                        registerWindowObserver(android.view.IWindowLayoutObserver.Stub.asInterface(data.readStrongBinder()), data.readLong());
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        unRegisterWindowObserver(android.view.IWindowLayoutObserver.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 18:
                        parcel.enforceInterface(descriptor);
                        notifyFocusChanged();
                        return true;
                    case 19:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        updateSurfaceStatus(_arg1);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    void closeSystemDialogs(String str) throws RemoteException;

    void dispatchAppVisibility(boolean z) throws RemoteException;

    void dispatchDragEvent(DragEvent dragEvent) throws RemoteException;

    void dispatchGetNewSurface() throws RemoteException;

    void dispatchPointerCaptureChanged(boolean z) throws RemoteException;

    void dispatchSystemUiVisibilityChanged(int i, int i2, int i3, int i4) throws RemoteException;

    void dispatchWallpaperCommand(String str, int i, int i2, int i3, Bundle bundle, boolean z) throws RemoteException;

    void dispatchWallpaperOffsets(float f, float f2, float f3, float f4, boolean z) throws RemoteException;

    void dispatchWindowShown() throws RemoteException;

    void executeCommand(String str, String str2, ParcelFileDescriptor parcelFileDescriptor) throws RemoteException;

    void moved(int i, int i2) throws RemoteException;

    void notifyFocusChanged() throws RemoteException;

    void registerWindowObserver(IWindowLayoutObserver iWindowLayoutObserver, long j) throws RemoteException;

    void requestAppKeyboardShortcuts(IResultReceiver iResultReceiver, int i) throws RemoteException;

    void resized(Rect rect, Rect rect2, Rect rect3, Rect rect4, Rect rect5, Rect rect6, boolean z, MergedConfiguration mergedConfiguration, Rect rect7, boolean z2, boolean z3, int i, ParcelableWrapper parcelableWrapper) throws RemoteException;

    void unRegisterWindowObserver(IWindowLayoutObserver iWindowLayoutObserver) throws RemoteException;

    void updatePointerIcon(float f, float f2) throws RemoteException;

    void updateSurfaceStatus(boolean z) throws RemoteException;

    void windowFocusChanged(boolean z, boolean z2) throws RemoteException;
}
