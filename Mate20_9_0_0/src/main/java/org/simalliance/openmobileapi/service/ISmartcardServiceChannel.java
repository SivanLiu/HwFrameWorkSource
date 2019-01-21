package org.simalliance.openmobileapi.service;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface ISmartcardServiceChannel extends IInterface {

    public static abstract class Stub extends Binder implements ISmartcardServiceChannel {
        private static final String DESCRIPTOR = "org.simalliance.openmobileapi.service.ISmartcardServiceChannel";
        static final int TRANSACTION_close = 1;
        static final int TRANSACTION_getSelectResponse = 4;
        static final int TRANSACTION_getSession = 5;
        static final int TRANSACTION_isBasicChannel = 3;
        static final int TRANSACTION_isClosed = 2;
        static final int TRANSACTION_selectNext = 7;
        static final int TRANSACTION_setTransmitBehaviour = 8;
        static final int TRANSACTION_transmit = 6;

        private static class Proxy implements ISmartcardServiceChannel {
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

            public void close(SmartcardError error) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        error.readFromParcel(_reply);
                    }
                    _reply.recycle();
                    _data.recycle();
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isClosed() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(2, _data, _reply, 0);
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

            public boolean isBasicChannel() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(3, _data, _reply, 0);
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

            public byte[] getSelectResponse() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public ISmartcardServiceSession getSession() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    ISmartcardServiceSession _result = org.simalliance.openmobileapi.service.ISmartcardServiceSession.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public byte[] transmit(byte[] command, SmartcardError error) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(command);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    if (_reply.readInt() != 0) {
                        error.readFromParcel(_reply);
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean selectNext(SmartcardError error) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    if (_reply.readInt() != 0) {
                        error.readFromParcel(_reply);
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setTransmitBehaviour(boolean expectDataWithWarningSW) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(expectDataWithWarningSW);
                    this.mRemote.transact(8, _data, _reply, 0);
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

        public static ISmartcardServiceChannel asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof ISmartcardServiceChannel)) {
                return new Proxy(obj);
            }
            return (ISmartcardServiceChannel) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != 1598968902) {
                SmartcardError _arg0;
                boolean _result;
                byte[] _result2;
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        _arg0 = new SmartcardError();
                        close(_arg0);
                        reply.writeNoException();
                        reply.writeInt(1);
                        _arg0.writeToParcel(reply, 1);
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        _result = isClosed();
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        _result = isBasicChannel();
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 4:
                        data.enforceInterface(descriptor);
                        _result2 = getSelectResponse();
                        reply.writeNoException();
                        reply.writeByteArray(_result2);
                        return true;
                    case 5:
                        data.enforceInterface(descriptor);
                        ISmartcardServiceSession _result3 = getSession();
                        reply.writeNoException();
                        reply.writeStrongBinder(_result3 != null ? _result3.asBinder() : null);
                        return true;
                    case 6:
                        data.enforceInterface(descriptor);
                        _result2 = data.createByteArray();
                        SmartcardError _arg1 = new SmartcardError();
                        byte[] _result4 = transmit(_result2, _arg1);
                        reply.writeNoException();
                        reply.writeByteArray(_result4);
                        reply.writeInt(1);
                        _arg1.writeToParcel(reply, 1);
                        return true;
                    case 7:
                        data.enforceInterface(descriptor);
                        _arg0 = new SmartcardError();
                        boolean _result5 = selectNext(_arg0);
                        reply.writeNoException();
                        reply.writeInt(_result5);
                        reply.writeInt(1);
                        _arg0.writeToParcel(reply, 1);
                        return true;
                    case 8:
                        data.enforceInterface(descriptor);
                        setTransmitBehaviour(data.readInt() != 0);
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

    void close(SmartcardError smartcardError) throws RemoteException;

    byte[] getSelectResponse() throws RemoteException;

    ISmartcardServiceSession getSession() throws RemoteException;

    boolean isBasicChannel() throws RemoteException;

    boolean isClosed() throws RemoteException;

    boolean selectNext(SmartcardError smartcardError) throws RemoteException;

    void setTransmitBehaviour(boolean z) throws RemoteException;

    byte[] transmit(byte[] bArr, SmartcardError smartcardError) throws RemoteException;
}
