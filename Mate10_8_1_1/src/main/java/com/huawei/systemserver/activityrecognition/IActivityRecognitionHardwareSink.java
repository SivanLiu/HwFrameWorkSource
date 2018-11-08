package com.huawei.systemserver.activityrecognition;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IActivityRecognitionHardwareSink extends IInterface {

    public static abstract class Stub extends Binder implements IActivityRecognitionHardwareSink {
        private static final String DESCRIPTOR = "com.huawei.systemserver.activityrecognition.IActivityRecognitionHardwareSink";
        static final int TRANSACTION_onActivityChanged = 1;
        static final int TRANSACTION_onActivityExtendChanged = 2;
        static final int TRANSACTION_onEnvironmentChanged = 3;

        private static class Proxy implements IActivityRecognitionHardwareSink {
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

            public void onActivityChanged(HwActivityChangedEvent event) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (event == null) {
                        _data.writeInt(0);
                    } else {
                        _data.writeInt(1);
                        event.writeToParcel(_data, 0);
                    }
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onActivityExtendChanged(HwActivityChangedExtendEvent event) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (event == null) {
                        _data.writeInt(0);
                    } else {
                        _data.writeInt(1);
                        event.writeToParcel(_data, 0);
                    }
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onEnvironmentChanged(HwEnvironmentChangedEvent event) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (event == null) {
                        _data.writeInt(0);
                    } else {
                        _data.writeInt(1);
                        event.writeToParcel(_data, 0);
                    }
                    this.mRemote.transact(3, _data, _reply, 0);
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

        public static IActivityRecognitionHardwareSink asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IActivityRecognitionHardwareSink)) {
                return (IActivityRecognitionHardwareSink) iin;
            }
            return new Proxy(obj);
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case 1:
                    HwActivityChangedEvent hwActivityChangedEvent;
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() == 0) {
                        hwActivityChangedEvent = null;
                    } else {
                        hwActivityChangedEvent = (HwActivityChangedEvent) HwActivityChangedEvent.CREATOR.createFromParcel(data);
                    }
                    onActivityChanged(hwActivityChangedEvent);
                    reply.writeNoException();
                    return true;
                case 2:
                    HwActivityChangedExtendEvent hwActivityChangedExtendEvent;
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() == 0) {
                        hwActivityChangedExtendEvent = null;
                    } else {
                        hwActivityChangedExtendEvent = (HwActivityChangedExtendEvent) HwActivityChangedExtendEvent.CREATOR.createFromParcel(data);
                    }
                    onActivityExtendChanged(hwActivityChangedExtendEvent);
                    reply.writeNoException();
                    return true;
                case 3:
                    HwEnvironmentChangedEvent hwEnvironmentChangedEvent;
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() == 0) {
                        hwEnvironmentChangedEvent = null;
                    } else {
                        hwEnvironmentChangedEvent = (HwEnvironmentChangedEvent) HwEnvironmentChangedEvent.CREATOR.createFromParcel(data);
                    }
                    onEnvironmentChanged(hwEnvironmentChangedEvent);
                    reply.writeNoException();
                    return true;
                case 1598968902:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }
    }

    void onActivityChanged(HwActivityChangedEvent hwActivityChangedEvent) throws RemoteException;

    void onActivityExtendChanged(HwActivityChangedExtendEvent hwActivityChangedExtendEvent) throws RemoteException;

    void onEnvironmentChanged(HwEnvironmentChangedEvent hwEnvironmentChangedEvent) throws RemoteException;
}
