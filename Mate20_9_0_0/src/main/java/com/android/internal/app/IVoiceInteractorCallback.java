package com.android.internal.app;

import android.app.VoiceInteractor.PickOptionRequest.Option;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IVoiceInteractorCallback extends IInterface {

    public static abstract class Stub extends Binder implements IVoiceInteractorCallback {
        private static final String DESCRIPTOR = "com.android.internal.app.IVoiceInteractorCallback";
        static final int TRANSACTION_deliverAbortVoiceResult = 4;
        static final int TRANSACTION_deliverCancel = 6;
        static final int TRANSACTION_deliverCommandResult = 5;
        static final int TRANSACTION_deliverCompleteVoiceResult = 3;
        static final int TRANSACTION_deliverConfirmationResult = 1;
        static final int TRANSACTION_deliverPickOptionResult = 2;

        private static class Proxy implements IVoiceInteractorCallback {
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

            public void deliverConfirmationResult(IVoiceInteractorRequest request, boolean confirmed, Bundle result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(request != null ? request.asBinder() : null);
                    _data.writeInt(confirmed);
                    if (result != null) {
                        _data.writeInt(1);
                        result.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void deliverPickOptionResult(IVoiceInteractorRequest request, boolean finished, Option[] selections, Bundle result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(request != null ? request.asBinder() : null);
                    _data.writeInt(finished);
                    _data.writeTypedArray(selections, 0);
                    if (result != null) {
                        _data.writeInt(1);
                        result.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void deliverCompleteVoiceResult(IVoiceInteractorRequest request, Bundle result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(request != null ? request.asBinder() : null);
                    if (result != null) {
                        _data.writeInt(1);
                        result.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void deliverAbortVoiceResult(IVoiceInteractorRequest request, Bundle result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(request != null ? request.asBinder() : null);
                    if (result != null) {
                        _data.writeInt(1);
                        result.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void deliverCommandResult(IVoiceInteractorRequest request, boolean finished, Bundle result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(request != null ? request.asBinder() : null);
                    _data.writeInt(finished);
                    if (result != null) {
                        _data.writeInt(1);
                        result.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void deliverCancel(IVoiceInteractorRequest request) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(request != null ? request.asBinder() : null);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IVoiceInteractorCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IVoiceInteractorCallback)) {
                return new Proxy(obj);
            }
            return (IVoiceInteractorCallback) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != 1598968902) {
                boolean _arg1 = false;
                Bundle _arg3 = null;
                IVoiceInteractorRequest _arg0;
                IVoiceInteractorRequest _arg02;
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        _arg0 = com.android.internal.app.IVoiceInteractorRequest.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        if (data.readInt() != 0) {
                            _arg3 = (Bundle) Bundle.CREATOR.createFromParcel(data);
                        }
                        deliverConfirmationResult(_arg0, _arg1, _arg3);
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        _arg0 = com.android.internal.app.IVoiceInteractorRequest.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        Option[] _arg2 = (Option[]) data.createTypedArray(Option.CREATOR);
                        if (data.readInt() != 0) {
                            _arg3 = (Bundle) Bundle.CREATOR.createFromParcel(data);
                        }
                        deliverPickOptionResult(_arg0, _arg1, _arg2, _arg3);
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        _arg02 = com.android.internal.app.IVoiceInteractorRequest.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg3 = (Bundle) Bundle.CREATOR.createFromParcel(data);
                        }
                        deliverCompleteVoiceResult(_arg02, _arg3);
                        return true;
                    case 4:
                        data.enforceInterface(descriptor);
                        _arg02 = com.android.internal.app.IVoiceInteractorRequest.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg3 = (Bundle) Bundle.CREATOR.createFromParcel(data);
                        }
                        deliverAbortVoiceResult(_arg02, _arg3);
                        return true;
                    case 5:
                        data.enforceInterface(descriptor);
                        _arg0 = com.android.internal.app.IVoiceInteractorRequest.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        if (data.readInt() != 0) {
                            _arg3 = (Bundle) Bundle.CREATOR.createFromParcel(data);
                        }
                        deliverCommandResult(_arg0, _arg1, _arg3);
                        return true;
                    case 6:
                        data.enforceInterface(descriptor);
                        deliverCancel(com.android.internal.app.IVoiceInteractorRequest.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    void deliverAbortVoiceResult(IVoiceInteractorRequest iVoiceInteractorRequest, Bundle bundle) throws RemoteException;

    void deliverCancel(IVoiceInteractorRequest iVoiceInteractorRequest) throws RemoteException;

    void deliverCommandResult(IVoiceInteractorRequest iVoiceInteractorRequest, boolean z, Bundle bundle) throws RemoteException;

    void deliverCompleteVoiceResult(IVoiceInteractorRequest iVoiceInteractorRequest, Bundle bundle) throws RemoteException;

    void deliverConfirmationResult(IVoiceInteractorRequest iVoiceInteractorRequest, boolean z, Bundle bundle) throws RemoteException;

    void deliverPickOptionResult(IVoiceInteractorRequest iVoiceInteractorRequest, boolean z, Option[] optionArr, Bundle bundle) throws RemoteException;
}
