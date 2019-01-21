package android.nfc;

import android.app.PendingIntent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface INfcAdapter extends IInterface {

    public static abstract class Stub extends Binder implements INfcAdapter {
        private static final String DESCRIPTOR = "android.nfc.INfcAdapter";
        static final int TRANSACTION_addNfcUnlockHandler = 28;
        static final int TRANSACTION_disable = 8;
        static final int TRANSACTION_disableNdefPush = 11;
        static final int TRANSACTION_disablePolling = 5;
        static final int TRANSACTION_dispatch = 25;
        static final int TRANSACTION_enable = 9;
        static final int TRANSACTION_enableNdefPush = 10;
        static final int TRANSACTION_enablePolling = 4;
        static final int TRANSACTION_getFirmwareVersion = 30;
        static final int TRANSACTION_getNfcAdapterExtrasInterface = 3;
        static final int TRANSACTION_getNfcAdapterVendorInterface = 6;
        static final int TRANSACTION_getNfcCardEmulationInterface = 2;
        static final int TRANSACTION_getNfcDtaInterface = 34;
        static final int TRANSACTION_getNfcFCardEmulationInterface = 33;
        static final int TRANSACTION_getNfcTagInterface = 1;
        static final int TRANSACTION_getSelectedCardEmulation = 17;
        static final int TRANSACTION_getState = 7;
        static final int TRANSACTION_getSupportCardEmulation = 19;
        static final int TRANSACTION_ignore = 24;
        static final int TRANSACTION_invokeBeam = 22;
        static final int TRANSACTION_invokeBeamInternal = 23;
        static final int TRANSACTION_is2ndLevelMenuOn = 31;
        static final int TRANSACTION_isNdefPushEnabled = 12;
        static final int TRANSACTION_isRwP2pOn = 16;
        static final int TRANSACTION_pausePolling = 13;
        static final int TRANSACTION_removeNfcUnlockHandler = 29;
        static final int TRANSACTION_resumePolling = 14;
        static final int TRANSACTION_selectCardEmulation = 18;
        static final int TRANSACTION_set2ndLevelMenu = 32;
        static final int TRANSACTION_setAppCallback = 21;
        static final int TRANSACTION_setForegroundDispatch = 20;
        static final int TRANSACTION_setP2pModes = 27;
        static final int TRANSACTION_setReaderMode = 26;
        static final int TRANSACTION_verifyNfcPermission = 15;

        private static class Proxy implements INfcAdapter {
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

            public INfcTag getNfcTagInterface() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    INfcTag _result = android.nfc.INfcTag.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public INfcCardEmulation getNfcCardEmulationInterface() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    INfcCardEmulation _result = android.nfc.INfcCardEmulation.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public INfcAdapterExtras getNfcAdapterExtrasInterface(String pkg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    INfcAdapterExtras _result = android.nfc.INfcAdapterExtras.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void enablePolling() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void disablePolling() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IBinder getNfcAdapterVendorInterface(String vendor) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(vendor);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getState() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean disable(boolean saveState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(saveState);
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

            public boolean enable() throws RemoteException {
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

            public boolean enableNdefPush() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(10, _data, _reply, 0);
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

            public boolean disableNdefPush() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(11, _data, _reply, 0);
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

            public boolean isNdefPushEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(12, _data, _reply, 0);
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

            public void pausePolling(int timeoutInMs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(timeoutInMs);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void resumePolling() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void verifyNfcPermission() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isRwP2pOn() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(16, _data, _reply, 0);
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

            public int getSelectedCardEmulation() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void selectCardEmulation(int sub) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(sub);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getSupportCardEmulation() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setForegroundDispatch(PendingIntent intent, IntentFilter[] filters, TechListParcel techLists) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeTypedArray(filters, 0);
                    if (techLists != null) {
                        _data.writeInt(1);
                        techLists.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setAppCallback(IAppCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void invokeBeam() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(22, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void invokeBeamInternal(BeamShareData shareData) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (shareData != null) {
                        _data.writeInt(1);
                        shareData.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(23, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public boolean ignore(int nativeHandle, int debounceMs, ITagRemovedCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(nativeHandle);
                    _data.writeInt(debounceMs);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    boolean z = false;
                    this.mRemote.transact(24, _data, _reply, 0);
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

            public void dispatch(Tag tag) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (tag != null) {
                        _data.writeInt(1);
                        tag.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setReaderMode(IBinder b, IAppCallback callback, int flags, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(b);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeInt(flags);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setP2pModes(int initatorModes, int targetModes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(initatorModes);
                    _data.writeInt(targetModes);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void addNfcUnlockHandler(INfcUnlockHandler unlockHandler, int[] techList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(unlockHandler != null ? unlockHandler.asBinder() : null);
                    _data.writeIntArray(techList);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeNfcUnlockHandler(INfcUnlockHandler unlockHandler) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(unlockHandler != null ? unlockHandler.asBinder() : null);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getFirmwareVersion() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean is2ndLevelMenuOn() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(31, _data, _reply, 0);
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

            public void set2ndLevelMenu(boolean onOff) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(onOff);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public INfcFCardEmulation getNfcFCardEmulationInterface() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                    INfcFCardEmulation _result = android.nfc.INfcFCardEmulation.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public INfcDta getNfcDtaInterface(String pkg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                    INfcDta _result = android.nfc.INfcDta.Stub.asInterface(_reply.readStrongBinder());
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

        public static INfcAdapter asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof INfcAdapter)) {
                return new Proxy(obj);
            }
            return (INfcAdapter) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != IBinder.INTERFACE_TRANSACTION) {
                boolean _arg0 = false;
                IBinder _arg2 = null;
                int _result;
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        INfcTag _result2 = getNfcTagInterface();
                        reply.writeNoException();
                        if (_result2 != null) {
                            _arg2 = _result2.asBinder();
                        }
                        reply.writeStrongBinder(_arg2);
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        INfcCardEmulation _result3 = getNfcCardEmulationInterface();
                        reply.writeNoException();
                        if (_result3 != null) {
                            _arg2 = _result3.asBinder();
                        }
                        reply.writeStrongBinder(_arg2);
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        INfcAdapterExtras _result4 = getNfcAdapterExtrasInterface(data.readString());
                        reply.writeNoException();
                        if (_result4 != null) {
                            _arg2 = _result4.asBinder();
                        }
                        reply.writeStrongBinder(_arg2);
                        return true;
                    case 4:
                        data.enforceInterface(descriptor);
                        enablePolling();
                        reply.writeNoException();
                        return true;
                    case 5:
                        data.enforceInterface(descriptor);
                        disablePolling();
                        reply.writeNoException();
                        return true;
                    case 6:
                        data.enforceInterface(descriptor);
                        _arg2 = getNfcAdapterVendorInterface(data.readString());
                        reply.writeNoException();
                        reply.writeStrongBinder(_arg2);
                        return true;
                    case 7:
                        data.enforceInterface(descriptor);
                        _result = getState();
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 8:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = true;
                        }
                        boolean _result5 = disable(_arg0);
                        reply.writeNoException();
                        reply.writeInt(_result5);
                        return true;
                    case 9:
                        data.enforceInterface(descriptor);
                        _arg0 = enable();
                        reply.writeNoException();
                        reply.writeInt(_arg0);
                        return true;
                    case 10:
                        data.enforceInterface(descriptor);
                        _arg0 = enableNdefPush();
                        reply.writeNoException();
                        reply.writeInt(_arg0);
                        return true;
                    case 11:
                        data.enforceInterface(descriptor);
                        _arg0 = disableNdefPush();
                        reply.writeNoException();
                        reply.writeInt(_arg0);
                        return true;
                    case 12:
                        data.enforceInterface(descriptor);
                        _arg0 = isNdefPushEnabled();
                        reply.writeNoException();
                        reply.writeInt(_arg0);
                        return true;
                    case 13:
                        data.enforceInterface(descriptor);
                        pausePolling(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 14:
                        data.enforceInterface(descriptor);
                        resumePolling();
                        reply.writeNoException();
                        return true;
                    case 15:
                        data.enforceInterface(descriptor);
                        verifyNfcPermission();
                        reply.writeNoException();
                        return true;
                    case 16:
                        data.enforceInterface(descriptor);
                        _arg0 = isRwP2pOn();
                        reply.writeNoException();
                        reply.writeInt(_arg0);
                        return true;
                    case 17:
                        data.enforceInterface(descriptor);
                        _result = getSelectedCardEmulation();
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 18:
                        data.enforceInterface(descriptor);
                        selectCardEmulation(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 19:
                        data.enforceInterface(descriptor);
                        _result = getSupportCardEmulation();
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 20:
                        PendingIntent _arg02;
                        TechListParcel _arg22;
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(data);
                        } else {
                            _arg02 = null;
                        }
                        IntentFilter[] _arg1 = (IntentFilter[]) data.createTypedArray(IntentFilter.CREATOR);
                        if (data.readInt() != 0) {
                            _arg22 = (TechListParcel) TechListParcel.CREATOR.createFromParcel(data);
                        }
                        setForegroundDispatch(_arg02, _arg1, _arg22);
                        reply.writeNoException();
                        return true;
                    case 21:
                        data.enforceInterface(descriptor);
                        setAppCallback(android.nfc.IAppCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 22:
                        data.enforceInterface(descriptor);
                        invokeBeam();
                        return true;
                    case 23:
                        BeamShareData _arg03;
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (BeamShareData) BeamShareData.CREATOR.createFromParcel(data);
                        }
                        invokeBeamInternal(_arg03);
                        return true;
                    case 24:
                        data.enforceInterface(descriptor);
                        boolean _result6 = ignore(data.readInt(), data.readInt(), android.nfc.ITagRemovedCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        reply.writeInt(_result6);
                        return true;
                    case 25:
                        Tag _arg04;
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg04 = (Tag) Tag.CREATOR.createFromParcel(data);
                        }
                        dispatch(_arg04);
                        reply.writeNoException();
                        return true;
                    case 26:
                        Bundle _arg3;
                        data.enforceInterface(descriptor);
                        IBinder _arg05 = data.readStrongBinder();
                        IAppCallback _arg12 = android.nfc.IAppCallback.Stub.asInterface(data.readStrongBinder());
                        int _arg23 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg3 = (Bundle) Bundle.CREATOR.createFromParcel(data);
                        }
                        setReaderMode(_arg05, _arg12, _arg23, _arg3);
                        reply.writeNoException();
                        return true;
                    case 27:
                        data.enforceInterface(descriptor);
                        setP2pModes(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 28:
                        data.enforceInterface(descriptor);
                        addNfcUnlockHandler(android.nfc.INfcUnlockHandler.Stub.asInterface(data.readStrongBinder()), data.createIntArray());
                        reply.writeNoException();
                        return true;
                    case 29:
                        data.enforceInterface(descriptor);
                        removeNfcUnlockHandler(android.nfc.INfcUnlockHandler.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 30:
                        data.enforceInterface(descriptor);
                        String _result7 = getFirmwareVersion();
                        reply.writeNoException();
                        reply.writeString(_result7);
                        return true;
                    case 31:
                        data.enforceInterface(descriptor);
                        _arg0 = is2ndLevelMenuOn();
                        reply.writeNoException();
                        reply.writeInt(_arg0);
                        return true;
                    case 32:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = true;
                        }
                        set2ndLevelMenu(_arg0);
                        reply.writeNoException();
                        return true;
                    case 33:
                        data.enforceInterface(descriptor);
                        INfcFCardEmulation _result8 = getNfcFCardEmulationInterface();
                        reply.writeNoException();
                        if (_result8 != null) {
                            _arg2 = _result8.asBinder();
                        }
                        reply.writeStrongBinder(_arg2);
                        return true;
                    case 34:
                        data.enforceInterface(descriptor);
                        INfcDta _result9 = getNfcDtaInterface(data.readString());
                        reply.writeNoException();
                        if (_result9 != null) {
                            _arg2 = _result9.asBinder();
                        }
                        reply.writeStrongBinder(_arg2);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    void addNfcUnlockHandler(INfcUnlockHandler iNfcUnlockHandler, int[] iArr) throws RemoteException;

    boolean disable(boolean z) throws RemoteException;

    boolean disableNdefPush() throws RemoteException;

    void disablePolling() throws RemoteException;

    void dispatch(Tag tag) throws RemoteException;

    boolean enable() throws RemoteException;

    boolean enableNdefPush() throws RemoteException;

    void enablePolling() throws RemoteException;

    String getFirmwareVersion() throws RemoteException;

    INfcAdapterExtras getNfcAdapterExtrasInterface(String str) throws RemoteException;

    IBinder getNfcAdapterVendorInterface(String str) throws RemoteException;

    INfcCardEmulation getNfcCardEmulationInterface() throws RemoteException;

    INfcDta getNfcDtaInterface(String str) throws RemoteException;

    INfcFCardEmulation getNfcFCardEmulationInterface() throws RemoteException;

    INfcTag getNfcTagInterface() throws RemoteException;

    int getSelectedCardEmulation() throws RemoteException;

    int getState() throws RemoteException;

    int getSupportCardEmulation() throws RemoteException;

    boolean ignore(int i, int i2, ITagRemovedCallback iTagRemovedCallback) throws RemoteException;

    void invokeBeam() throws RemoteException;

    void invokeBeamInternal(BeamShareData beamShareData) throws RemoteException;

    boolean is2ndLevelMenuOn() throws RemoteException;

    boolean isNdefPushEnabled() throws RemoteException;

    boolean isRwP2pOn() throws RemoteException;

    void pausePolling(int i) throws RemoteException;

    void removeNfcUnlockHandler(INfcUnlockHandler iNfcUnlockHandler) throws RemoteException;

    void resumePolling() throws RemoteException;

    void selectCardEmulation(int i) throws RemoteException;

    void set2ndLevelMenu(boolean z) throws RemoteException;

    void setAppCallback(IAppCallback iAppCallback) throws RemoteException;

    void setForegroundDispatch(PendingIntent pendingIntent, IntentFilter[] intentFilterArr, TechListParcel techListParcel) throws RemoteException;

    void setP2pModes(int i, int i2) throws RemoteException;

    void setReaderMode(IBinder iBinder, IAppCallback iAppCallback, int i, Bundle bundle) throws RemoteException;

    void verifyNfcPermission() throws RemoteException;
}
