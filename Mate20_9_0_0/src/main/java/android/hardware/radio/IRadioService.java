package android.hardware.radio;

import android.hardware.radio.RadioManager.BandConfig;
import android.hardware.radio.RadioManager.ModuleProperties;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;

public interface IRadioService extends IInterface {

    public static abstract class Stub extends Binder implements IRadioService {
        private static final String DESCRIPTOR = "android.hardware.radio.IRadioService";
        static final int TRANSACTION_addAnnouncementListener = 3;
        static final int TRANSACTION_listModules = 1;
        static final int TRANSACTION_openTuner = 2;

        private static class Proxy implements IRadioService {
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

            public List<ModuleProperties> listModules() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    List<ModuleProperties> _result = _reply.createTypedArrayList(ModuleProperties.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public ITuner openTuner(int moduleId, BandConfig bandConfig, boolean withAudio, ITunerCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(moduleId);
                    if (bandConfig != null) {
                        _data.writeInt(1);
                        bandConfig.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(withAudio);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    ITuner _result = android.hardware.radio.ITuner.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public ICloseHandle addAnnouncementListener(int[] enabledTypes, IAnnouncementListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeIntArray(enabledTypes);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    ICloseHandle _result = android.hardware.radio.ICloseHandle.Stub.asInterface(_reply.readStrongBinder());
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

        public static IRadioService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IRadioService)) {
                return new Proxy(obj);
            }
            return (IRadioService) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != 1598968902) {
                IBinder iBinder = null;
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        List<ModuleProperties> _result = listModules();
                        reply.writeNoException();
                        reply.writeTypedList(_result);
                        return true;
                    case 2:
                        BandConfig _arg1;
                        data.enforceInterface(descriptor);
                        int _arg0 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = (BandConfig) BandConfig.CREATOR.createFromParcel(data);
                        } else {
                            _arg1 = null;
                        }
                        ITuner _result2 = openTuner(_arg0, _arg1, data.readInt() != 0, android.hardware.radio.ITunerCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        if (_result2 != null) {
                            iBinder = _result2.asBinder();
                        }
                        reply.writeStrongBinder(iBinder);
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        ICloseHandle _result3 = addAnnouncementListener(data.createIntArray(), android.hardware.radio.IAnnouncementListener.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        if (_result3 != null) {
                            iBinder = _result3.asBinder();
                        }
                        reply.writeStrongBinder(iBinder);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    ICloseHandle addAnnouncementListener(int[] iArr, IAnnouncementListener iAnnouncementListener) throws RemoteException;

    List<ModuleProperties> listModules() throws RemoteException;

    ITuner openTuner(int i, BandConfig bandConfig, boolean z, ITunerCallback iTunerCallback) throws RemoteException;
}
