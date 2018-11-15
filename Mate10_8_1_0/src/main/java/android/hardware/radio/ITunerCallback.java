package android.hardware.radio;

import android.hardware.radio.RadioManager.BandConfig;
import android.hardware.radio.RadioManager.ProgramInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface ITunerCallback extends IInterface {

    public static abstract class Stub extends Binder implements ITunerCallback {
        private static final String DESCRIPTOR = "android.hardware.radio.ITunerCallback";
        static final int TRANSACTION_onAntennaState = 6;
        static final int TRANSACTION_onBackgroundScanAvailabilityChange = 7;
        static final int TRANSACTION_onBackgroundScanComplete = 8;
        static final int TRANSACTION_onConfigurationChanged = 2;
        static final int TRANSACTION_onCurrentProgramInfoChanged = 3;
        static final int TRANSACTION_onEmergencyAnnouncement = 5;
        static final int TRANSACTION_onError = 1;
        static final int TRANSACTION_onProgramListChanged = 9;
        static final int TRANSACTION_onTrafficAnnouncement = 4;

        private static class Proxy implements ITunerCallback {
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

            public void onError(int status) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(status);
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onConfigurationChanged(BandConfig config) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (config != null) {
                        _data.writeInt(1);
                        config.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onCurrentProgramInfoChanged(ProgramInfo info) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (info != null) {
                        _data.writeInt(1);
                        info.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onTrafficAnnouncement(boolean active) throws RemoteException {
                int i = 1;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (!active) {
                        i = 0;
                    }
                    _data.writeInt(i);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onEmergencyAnnouncement(boolean active) throws RemoteException {
                int i = 1;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (!active) {
                        i = 0;
                    }
                    _data.writeInt(i);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onAntennaState(boolean connected) throws RemoteException {
                int i = 1;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (!connected) {
                        i = 0;
                    }
                    _data.writeInt(i);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onBackgroundScanAvailabilityChange(boolean isAvailable) throws RemoteException {
                int i = 1;
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (!isAvailable) {
                        i = 0;
                    }
                    _data.writeInt(i);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onBackgroundScanComplete() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onProgramListChanged() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ITunerCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof ITunerCallback)) {
                return new Proxy(obj);
            }
            return (ITunerCallback) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    onError(data.readInt());
                    return true;
                case 2:
                    BandConfig bandConfig;
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        bandConfig = (BandConfig) BandConfig.CREATOR.createFromParcel(data);
                    } else {
                        bandConfig = null;
                    }
                    onConfigurationChanged(bandConfig);
                    return true;
                case 3:
                    ProgramInfo programInfo;
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        programInfo = (ProgramInfo) ProgramInfo.CREATOR.createFromParcel(data);
                    } else {
                        programInfo = null;
                    }
                    onCurrentProgramInfoChanged(programInfo);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    onTrafficAnnouncement(data.readInt() != 0);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    onEmergencyAnnouncement(data.readInt() != 0);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    onAntennaState(data.readInt() != 0);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    onBackgroundScanAvailabilityChange(data.readInt() != 0);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    onBackgroundScanComplete();
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    onProgramListChanged();
                    return true;
                case IBinder.INTERFACE_TRANSACTION /*1598968902*/:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }
    }

    void onAntennaState(boolean z) throws RemoteException;

    void onBackgroundScanAvailabilityChange(boolean z) throws RemoteException;

    void onBackgroundScanComplete() throws RemoteException;

    void onConfigurationChanged(BandConfig bandConfig) throws RemoteException;

    void onCurrentProgramInfoChanged(ProgramInfo programInfo) throws RemoteException;

    void onEmergencyAnnouncement(boolean z) throws RemoteException;

    void onError(int i) throws RemoteException;

    void onProgramListChanged() throws RemoteException;

    void onTrafficAnnouncement(boolean z) throws RemoteException;
}
