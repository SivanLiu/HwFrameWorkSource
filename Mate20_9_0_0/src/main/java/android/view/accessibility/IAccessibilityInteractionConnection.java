package android.view.accessibility;

import android.graphics.Region;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.view.MagnificationSpec;

public interface IAccessibilityInteractionConnection extends IInterface {

    public static abstract class Stub extends Binder implements IAccessibilityInteractionConnection {
        private static final String DESCRIPTOR = "android.view.accessibility.IAccessibilityInteractionConnection";
        static final int TRANSACTION_findAccessibilityNodeInfoByAccessibilityId = 1;
        static final int TRANSACTION_findAccessibilityNodeInfosByText = 3;
        static final int TRANSACTION_findAccessibilityNodeInfosByViewId = 2;
        static final int TRANSACTION_findFocus = 4;
        static final int TRANSACTION_focusSearch = 5;
        static final int TRANSACTION_performAccessibilityAction = 6;

        private static class Proxy implements IAccessibilityInteractionConnection {
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

            public void findAccessibilityNodeInfoByAccessibilityId(long accessibilityNodeId, Region bounds, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec, Bundle arguments) throws RemoteException {
                Throwable th;
                long j;
                int i;
                int i2;
                int i3;
                Region region = bounds;
                MagnificationSpec magnificationSpec = spec;
                Bundle bundle = arguments;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    try {
                        _data.writeLong(accessibilityNodeId);
                        if (region != null) {
                            _data.writeInt(1);
                            region.writeToParcel(_data, 0);
                        } else {
                            _data.writeInt(0);
                        }
                        try {
                            _data.writeInt(interactionId);
                            _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                            try {
                                _data.writeInt(flags);
                                try {
                                    _data.writeInt(interrogatingPid);
                                } catch (Throwable th2) {
                                    th = th2;
                                    j = interrogatingTid;
                                    _data.recycle();
                                    throw th;
                                }
                            } catch (Throwable th3) {
                                th = th3;
                                i = interrogatingPid;
                                j = interrogatingTid;
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            i2 = flags;
                            i = interrogatingPid;
                            j = interrogatingTid;
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th5) {
                        th = th5;
                        i3 = interactionId;
                        i2 = flags;
                        i = interrogatingPid;
                        j = interrogatingTid;
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeLong(interrogatingTid);
                        if (magnificationSpec != null) {
                            _data.writeInt(1);
                            magnificationSpec.writeToParcel(_data, 0);
                        } else {
                            _data.writeInt(0);
                        }
                        if (bundle != null) {
                            _data.writeInt(1);
                            bundle.writeToParcel(_data, 0);
                        } else {
                            _data.writeInt(0);
                        }
                        try {
                            this.mRemote.transact(1, _data, null, 1);
                            _data.recycle();
                        } catch (Throwable th6) {
                            th = th6;
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th7) {
                        th = th7;
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th8) {
                    th = th8;
                    long j2 = accessibilityNodeId;
                    i3 = interactionId;
                    i2 = flags;
                    i = interrogatingPid;
                    j = interrogatingTid;
                    _data.recycle();
                    throw th;
                }
            }

            public void findAccessibilityNodeInfosByViewId(long accessibilityNodeId, String viewId, Region bounds, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec) throws RemoteException {
                Throwable th;
                long j;
                int i;
                int i2;
                int i3;
                String str;
                Region region = bounds;
                MagnificationSpec magnificationSpec = spec;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    try {
                        _data.writeLong(accessibilityNodeId);
                        try {
                            _data.writeString(viewId);
                            if (region != null) {
                                _data.writeInt(1);
                                region.writeToParcel(_data, 0);
                            } else {
                                _data.writeInt(0);
                            }
                            try {
                                _data.writeInt(interactionId);
                                _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                                try {
                                    _data.writeInt(flags);
                                    try {
                                        _data.writeInt(interrogatingPid);
                                    } catch (Throwable th2) {
                                        th = th2;
                                        j = interrogatingTid;
                                        _data.recycle();
                                        throw th;
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                    i = interrogatingPid;
                                    j = interrogatingTid;
                                    _data.recycle();
                                    throw th;
                                }
                            } catch (Throwable th4) {
                                th = th4;
                                i2 = flags;
                                i = interrogatingPid;
                                j = interrogatingTid;
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th5) {
                            th = th5;
                            i3 = interactionId;
                            i2 = flags;
                            i = interrogatingPid;
                            j = interrogatingTid;
                            _data.recycle();
                            throw th;
                        }
                        try {
                            _data.writeLong(interrogatingTid);
                            if (magnificationSpec != null) {
                                _data.writeInt(1);
                                magnificationSpec.writeToParcel(_data, 0);
                            } else {
                                _data.writeInt(0);
                            }
                            try {
                                this.mRemote.transact(2, _data, null, 1);
                                _data.recycle();
                            } catch (Throwable th6) {
                                th = th6;
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th7) {
                            th = th7;
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th8) {
                        th = th8;
                        str = viewId;
                        i3 = interactionId;
                        i2 = flags;
                        i = interrogatingPid;
                        j = interrogatingTid;
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th9) {
                    th = th9;
                    long j2 = accessibilityNodeId;
                    str = viewId;
                    i3 = interactionId;
                    i2 = flags;
                    i = interrogatingPid;
                    j = interrogatingTid;
                    _data.recycle();
                    throw th;
                }
            }

            public void findAccessibilityNodeInfosByText(long accessibilityNodeId, String text, Region bounds, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec) throws RemoteException {
                Throwable th;
                long j;
                int i;
                int i2;
                int i3;
                String str;
                Region region = bounds;
                MagnificationSpec magnificationSpec = spec;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    try {
                        _data.writeLong(accessibilityNodeId);
                        try {
                            _data.writeString(text);
                            if (region != null) {
                                _data.writeInt(1);
                                region.writeToParcel(_data, 0);
                            } else {
                                _data.writeInt(0);
                            }
                            try {
                                _data.writeInt(interactionId);
                                _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                                try {
                                    _data.writeInt(flags);
                                    try {
                                        _data.writeInt(interrogatingPid);
                                    } catch (Throwable th2) {
                                        th = th2;
                                        j = interrogatingTid;
                                        _data.recycle();
                                        throw th;
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                    i = interrogatingPid;
                                    j = interrogatingTid;
                                    _data.recycle();
                                    throw th;
                                }
                            } catch (Throwable th4) {
                                th = th4;
                                i2 = flags;
                                i = interrogatingPid;
                                j = interrogatingTid;
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th5) {
                            th = th5;
                            i3 = interactionId;
                            i2 = flags;
                            i = interrogatingPid;
                            j = interrogatingTid;
                            _data.recycle();
                            throw th;
                        }
                        try {
                            _data.writeLong(interrogatingTid);
                            if (magnificationSpec != null) {
                                _data.writeInt(1);
                                magnificationSpec.writeToParcel(_data, 0);
                            } else {
                                _data.writeInt(0);
                            }
                            try {
                                this.mRemote.transact(3, _data, null, 1);
                                _data.recycle();
                            } catch (Throwable th6) {
                                th = th6;
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th7) {
                            th = th7;
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th8) {
                        th = th8;
                        str = text;
                        i3 = interactionId;
                        i2 = flags;
                        i = interrogatingPid;
                        j = interrogatingTid;
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th9) {
                    th = th9;
                    long j2 = accessibilityNodeId;
                    str = text;
                    i3 = interactionId;
                    i2 = flags;
                    i = interrogatingPid;
                    j = interrogatingTid;
                    _data.recycle();
                    throw th;
                }
            }

            public void findFocus(long accessibilityNodeId, int focusType, Region bounds, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec) throws RemoteException {
                Throwable th;
                long j;
                int i;
                int i2;
                int i3;
                int i4;
                Region region = bounds;
                MagnificationSpec magnificationSpec = spec;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    try {
                        _data.writeLong(accessibilityNodeId);
                        try {
                            _data.writeInt(focusType);
                            if (region != null) {
                                _data.writeInt(1);
                                region.writeToParcel(_data, 0);
                            } else {
                                _data.writeInt(0);
                            }
                            try {
                                _data.writeInt(interactionId);
                                _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                                try {
                                    _data.writeInt(flags);
                                    try {
                                        _data.writeInt(interrogatingPid);
                                    } catch (Throwable th2) {
                                        th = th2;
                                        j = interrogatingTid;
                                        _data.recycle();
                                        throw th;
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                    i = interrogatingPid;
                                    j = interrogatingTid;
                                    _data.recycle();
                                    throw th;
                                }
                            } catch (Throwable th4) {
                                th = th4;
                                i2 = flags;
                                i = interrogatingPid;
                                j = interrogatingTid;
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th5) {
                            th = th5;
                            i3 = interactionId;
                            i2 = flags;
                            i = interrogatingPid;
                            j = interrogatingTid;
                            _data.recycle();
                            throw th;
                        }
                        try {
                            _data.writeLong(interrogatingTid);
                            if (magnificationSpec != null) {
                                _data.writeInt(1);
                                magnificationSpec.writeToParcel(_data, 0);
                            } else {
                                _data.writeInt(0);
                            }
                            try {
                                this.mRemote.transact(4, _data, null, 1);
                                _data.recycle();
                            } catch (Throwable th6) {
                                th = th6;
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th7) {
                            th = th7;
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th8) {
                        th = th8;
                        i4 = focusType;
                        i3 = interactionId;
                        i2 = flags;
                        i = interrogatingPid;
                        j = interrogatingTid;
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th9) {
                    th = th9;
                    long j2 = accessibilityNodeId;
                    i4 = focusType;
                    i3 = interactionId;
                    i2 = flags;
                    i = interrogatingPid;
                    j = interrogatingTid;
                    _data.recycle();
                    throw th;
                }
            }

            public void focusSearch(long accessibilityNodeId, int direction, Region bounds, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid, MagnificationSpec spec) throws RemoteException {
                Throwable th;
                long j;
                int i;
                int i2;
                int i3;
                int i4;
                Region region = bounds;
                MagnificationSpec magnificationSpec = spec;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    try {
                        _data.writeLong(accessibilityNodeId);
                        try {
                            _data.writeInt(direction);
                            if (region != null) {
                                _data.writeInt(1);
                                region.writeToParcel(_data, 0);
                            } else {
                                _data.writeInt(0);
                            }
                            try {
                                _data.writeInt(interactionId);
                                _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                                try {
                                    _data.writeInt(flags);
                                    try {
                                        _data.writeInt(interrogatingPid);
                                    } catch (Throwable th2) {
                                        th = th2;
                                        j = interrogatingTid;
                                        _data.recycle();
                                        throw th;
                                    }
                                } catch (Throwable th3) {
                                    th = th3;
                                    i = interrogatingPid;
                                    j = interrogatingTid;
                                    _data.recycle();
                                    throw th;
                                }
                            } catch (Throwable th4) {
                                th = th4;
                                i2 = flags;
                                i = interrogatingPid;
                                j = interrogatingTid;
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th5) {
                            th = th5;
                            i3 = interactionId;
                            i2 = flags;
                            i = interrogatingPid;
                            j = interrogatingTid;
                            _data.recycle();
                            throw th;
                        }
                        try {
                            _data.writeLong(interrogatingTid);
                            if (magnificationSpec != null) {
                                _data.writeInt(1);
                                magnificationSpec.writeToParcel(_data, 0);
                            } else {
                                _data.writeInt(0);
                            }
                            try {
                                this.mRemote.transact(5, _data, null, 1);
                                _data.recycle();
                            } catch (Throwable th6) {
                                th = th6;
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th7) {
                            th = th7;
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th8) {
                        th = th8;
                        i4 = direction;
                        i3 = interactionId;
                        i2 = flags;
                        i = interrogatingPid;
                        j = interrogatingTid;
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th9) {
                    th = th9;
                    long j2 = accessibilityNodeId;
                    i4 = direction;
                    i3 = interactionId;
                    i2 = flags;
                    i = interrogatingPid;
                    j = interrogatingTid;
                    _data.recycle();
                    throw th;
                }
            }

            public void performAccessibilityAction(long accessibilityNodeId, int action, Bundle arguments, int interactionId, IAccessibilityInteractionConnectionCallback callback, int flags, int interrogatingPid, long interrogatingTid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(accessibilityNodeId);
                    _data.writeInt(action);
                    if (arguments != null) {
                        _data.writeInt(1);
                        arguments.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(interactionId);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeInt(flags);
                    _data.writeInt(interrogatingPid);
                    _data.writeLong(interrogatingTid);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IAccessibilityInteractionConnection asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IAccessibilityInteractionConnection)) {
                return new Proxy(obj);
            }
            return (IAccessibilityInteractionConnection) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            String descriptor = DESCRIPTOR;
            if (i != 1598968902) {
                Bundle bundle = null;
                long _arg0;
                int _arg2;
                int _arg5;
                String _arg1;
                Region _arg22;
                int _arg3;
                IAccessibilityInteractionConnectionCallback _arg4;
                int _arg6;
                long _arg7;
                switch (i) {
                    case 1:
                        Region _arg12;
                        MagnificationSpec _arg72;
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readLong();
                        if (data.readInt() != 0) {
                            _arg12 = (Region) Region.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg12 = null;
                        }
                        _arg2 = data.readInt();
                        IAccessibilityInteractionConnectionCallback _arg32 = android.view.accessibility.IAccessibilityInteractionConnectionCallback.Stub.asInterface(data.readStrongBinder());
                        int _arg42 = data.readInt();
                        _arg5 = data.readInt();
                        long _arg62 = data.readLong();
                        if (data.readInt() != 0) {
                            _arg72 = (MagnificationSpec) MagnificationSpec.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg72 = null;
                        }
                        if (data.readInt() != 0) {
                            bundle = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        }
                        findAccessibilityNodeInfoByAccessibilityId(_arg0, _arg12, _arg2, _arg32, _arg42, _arg5, _arg62, _arg72, bundle);
                        return true;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readLong();
                        _arg1 = data.readString();
                        if (data.readInt() != 0) {
                            _arg22 = (Region) Region.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg22 = null;
                        }
                        _arg3 = data.readInt();
                        _arg4 = android.view.accessibility.IAccessibilityInteractionConnectionCallback.Stub.asInterface(data.readStrongBinder());
                        _arg5 = data.readInt();
                        _arg6 = data.readInt();
                        _arg7 = data.readLong();
                        if (data.readInt() != 0) {
                            bundle = (MagnificationSpec) MagnificationSpec.CREATOR.createFromParcel(parcel);
                        }
                        findAccessibilityNodeInfosByViewId(_arg0, _arg1, _arg22, _arg3, _arg4, _arg5, _arg6, _arg7, bundle);
                        return true;
                    case 3:
                        Region _arg23;
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readLong();
                        _arg1 = data.readString();
                        if (data.readInt() != 0) {
                            _arg23 = (Region) Region.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg23 = null;
                        }
                        _arg3 = data.readInt();
                        _arg4 = android.view.accessibility.IAccessibilityInteractionConnectionCallback.Stub.asInterface(data.readStrongBinder());
                        _arg5 = data.readInt();
                        _arg6 = data.readInt();
                        _arg7 = data.readLong();
                        if (data.readInt() != 0) {
                            bundle = (MagnificationSpec) MagnificationSpec.CREATOR.createFromParcel(parcel);
                        }
                        findAccessibilityNodeInfosByText(_arg0, _arg1, _arg23, _arg3, _arg4, _arg5, _arg6, _arg7, bundle);
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readLong();
                        _arg2 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg22 = (Region) Region.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg22 = null;
                        }
                        _arg3 = data.readInt();
                        _arg4 = android.view.accessibility.IAccessibilityInteractionConnectionCallback.Stub.asInterface(data.readStrongBinder());
                        _arg5 = data.readInt();
                        _arg6 = data.readInt();
                        _arg7 = data.readLong();
                        if (data.readInt() != 0) {
                            bundle = (MagnificationSpec) MagnificationSpec.CREATOR.createFromParcel(parcel);
                        }
                        findFocus(_arg0, _arg2, _arg22, _arg3, _arg4, _arg5, _arg6, _arg7, bundle);
                        return true;
                    case 5:
                        Region _arg24;
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readLong();
                        _arg2 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg24 = (Region) Region.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg24 = null;
                        }
                        _arg3 = data.readInt();
                        _arg4 = android.view.accessibility.IAccessibilityInteractionConnectionCallback.Stub.asInterface(data.readStrongBinder());
                        _arg5 = data.readInt();
                        _arg6 = data.readInt();
                        _arg7 = data.readLong();
                        if (data.readInt() != 0) {
                            bundle = (MagnificationSpec) MagnificationSpec.CREATOR.createFromParcel(parcel);
                        }
                        focusSearch(_arg0, _arg2, _arg24, _arg3, _arg4, _arg5, _arg6, _arg7, bundle);
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        long _arg02 = data.readLong();
                        int _arg13 = data.readInt();
                        if (data.readInt() != 0) {
                            bundle = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        }
                        Bundle _arg25 = bundle;
                        performAccessibilityAction(_arg02, _arg13, _arg25, data.readInt(), android.view.accessibility.IAccessibilityInteractionConnectionCallback.Stub.asInterface(data.readStrongBinder()), data.readInt(), data.readInt(), data.readLong());
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    void findAccessibilityNodeInfoByAccessibilityId(long j, Region region, int i, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i2, int i3, long j2, MagnificationSpec magnificationSpec, Bundle bundle) throws RemoteException;

    void findAccessibilityNodeInfosByText(long j, String str, Region region, int i, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i2, int i3, long j2, MagnificationSpec magnificationSpec) throws RemoteException;

    void findAccessibilityNodeInfosByViewId(long j, String str, Region region, int i, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i2, int i3, long j2, MagnificationSpec magnificationSpec) throws RemoteException;

    void findFocus(long j, int i, Region region, int i2, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i3, int i4, long j2, MagnificationSpec magnificationSpec) throws RemoteException;

    void focusSearch(long j, int i, Region region, int i2, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i3, int i4, long j2, MagnificationSpec magnificationSpec) throws RemoteException;

    void performAccessibilityAction(long j, int i, Bundle bundle, int i2, IAccessibilityInteractionConnectionCallback iAccessibilityInteractionConnectionCallback, int i3, int i4, long j2) throws RemoteException;
}
