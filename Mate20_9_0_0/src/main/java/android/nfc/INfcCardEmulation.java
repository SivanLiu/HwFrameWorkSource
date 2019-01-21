package android.nfc;

import android.content.ComponentName;
import android.nfc.cardemulation.AidGroup;
import android.nfc.cardemulation.ApduServiceInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;

public interface INfcCardEmulation extends IInterface {

    public static abstract class Stub extends Binder implements INfcCardEmulation {
        private static final String DESCRIPTOR = "android.nfc.INfcCardEmulation";
        static final int TRANSACTION_getAidGroupForService = 6;
        static final int TRANSACTION_getPaymentDefaultServiceInfo = 18;
        static final int TRANSACTION_getPaymentPriority = 17;
        static final int TRANSACTION_getServices = 8;
        static final int TRANSACTION_getUsedAidTableSizeInPercent = 15;
        static final int TRANSACTION_initializePaymentDefault = 16;
        static final int TRANSACTION_isDefaultServiceForAid = 2;
        static final int TRANSACTION_isDefaultServiceForCategory = 1;
        static final int TRANSACTION_isRegisteredService = 14;
        static final int TRANSACTION_registerAidGroupForService = 5;
        static final int TRANSACTION_registerService = 12;
        static final int TRANSACTION_removeAidGroupForService = 7;
        static final int TRANSACTION_setDefaultForNextTap = 4;
        static final int TRANSACTION_setDefaultServiceForCategory = 3;
        static final int TRANSACTION_setPreferredService = 9;
        static final int TRANSACTION_supportsAidPrefixRegistration = 11;
        static final int TRANSACTION_unregisterOtherService = 13;
        static final int TRANSACTION_unsetPreferredService = 10;

        private static class Proxy implements INfcCardEmulation {
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

            public boolean isDefaultServiceForCategory(int userHandle, ComponentName service, String category) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    boolean _result = true;
                    if (service != null) {
                        _data.writeInt(1);
                        service.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(category);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isDefaultServiceForAid(int userHandle, ComponentName service, String aid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    boolean _result = true;
                    if (service != null) {
                        _data.writeInt(1);
                        service.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(aid);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setDefaultServiceForCategory(int userHandle, ComponentName service, String category) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    boolean _result = true;
                    if (service != null) {
                        _data.writeInt(1);
                        service.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(category);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setDefaultForNextTap(int userHandle, ComponentName service) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    boolean _result = true;
                    if (service != null) {
                        _data.writeInt(1);
                        service.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean registerAidGroupForService(int userHandle, ComponentName service, AidGroup aidGroup) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    boolean _result = true;
                    if (service != null) {
                        _data.writeInt(1);
                        service.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (aidGroup != null) {
                        _data.writeInt(1);
                        aidGroup.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public AidGroup getAidGroupForService(int userHandle, ComponentName service, String category) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    AidGroup _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    if (service != null) {
                        _data.writeInt(1);
                        service.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(category);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (AidGroup) AidGroup.CREATOR.createFromParcel(_reply);
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

            public boolean removeAidGroupForService(int userHandle, ComponentName service, String category) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    boolean _result = true;
                    if (service != null) {
                        _data.writeInt(1);
                        service.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(category);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<ApduServiceInfo> getServices(int userHandle, String category) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    _data.writeString(category);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    List<ApduServiceInfo> _result = _reply.createTypedArrayList(ApduServiceInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setPreferredService(ComponentName service) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (service != null) {
                        _data.writeInt(1);
                        service.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean unsetPreferredService() throws RemoteException {
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

            public boolean supportsAidPrefixRegistration() throws RemoteException {
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

            public void registerService(int userHandle, ComponentName app, String category, int requester) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    if (app != null) {
                        _data.writeInt(1);
                        app.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(category);
                    _data.writeInt(requester);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void unregisterOtherService(int userHandle, ComponentName app) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    if (app != null) {
                        _data.writeInt(1);
                        app.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isRegisteredService(int userHandle, ComponentName app, String category) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    boolean _result = true;
                    if (app != null) {
                        _data.writeInt(1);
                        app.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(category);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getUsedAidTableSizeInPercent(int userHandle, String category) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    _data.writeString(category);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void initializePaymentDefault(int userHandle, int necessity) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    _data.writeInt(necessity);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public ComponentName getPaymentPriority(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    ComponentName _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ComponentName) ComponentName.CREATOR.createFromParcel(_reply);
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

            public ApduServiceInfo getPaymentDefaultServiceInfo(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    ApduServiceInfo _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ApduServiceInfo) ApduServiceInfo.CREATOR.createFromParcel(_reply);
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
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static INfcCardEmulation asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof INfcCardEmulation)) {
                return new Proxy(obj);
            }
            return (INfcCardEmulation) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != IBinder.INTERFACE_TRANSACTION) {
                ComponentName _arg1 = null;
                int _arg0;
                boolean _result;
                ComponentName _arg12;
                int _arg02;
                boolean _result2;
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        _arg0 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = (ComponentName) ComponentName.CREATOR.createFromParcel(data);
                        }
                        _result = isDefaultServiceForCategory(_arg0, _arg1, data.readString());
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        _arg0 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = (ComponentName) ComponentName.CREATOR.createFromParcel(data);
                        }
                        _result = isDefaultServiceForAid(_arg0, _arg1, data.readString());
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        _arg0 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = (ComponentName) ComponentName.CREATOR.createFromParcel(data);
                        }
                        _result = setDefaultServiceForCategory(_arg0, _arg1, data.readString());
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 4:
                        data.enforceInterface(descriptor);
                        _arg0 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = (ComponentName) ComponentName.CREATOR.createFromParcel(data);
                        }
                        boolean _result3 = setDefaultForNextTap(_arg0, _arg1);
                        reply.writeNoException();
                        reply.writeInt(_result3);
                        return true;
                    case 5:
                        AidGroup _arg2;
                        data.enforceInterface(descriptor);
                        _arg0 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg12 = (ComponentName) ComponentName.CREATOR.createFromParcel(data);
                        } else {
                            _arg12 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg2 = (AidGroup) AidGroup.CREATOR.createFromParcel(data);
                        }
                        _result = registerAidGroupForService(_arg0, _arg12, _arg2);
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 6:
                        data.enforceInterface(descriptor);
                        _arg02 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = (ComponentName) ComponentName.CREATOR.createFromParcel(data);
                        }
                        AidGroup _result4 = getAidGroupForService(_arg02, _arg1, data.readString());
                        reply.writeNoException();
                        if (_result4 != null) {
                            reply.writeInt(1);
                            _result4.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 7:
                        data.enforceInterface(descriptor);
                        _arg0 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = (ComponentName) ComponentName.CREATOR.createFromParcel(data);
                        }
                        _result = removeAidGroupForService(_arg0, _arg1, data.readString());
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 8:
                        data.enforceInterface(descriptor);
                        List<ApduServiceInfo> _result5 = getServices(data.readInt(), data.readString());
                        reply.writeNoException();
                        reply.writeTypedList(_result5);
                        return true;
                    case 9:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = (ComponentName) ComponentName.CREATOR.createFromParcel(data);
                        }
                        boolean _result6 = setPreferredService(_arg1);
                        reply.writeNoException();
                        reply.writeInt(_result6);
                        return true;
                    case 10:
                        data.enforceInterface(descriptor);
                        _result2 = unsetPreferredService();
                        reply.writeNoException();
                        reply.writeInt(_result2);
                        return true;
                    case 11:
                        data.enforceInterface(descriptor);
                        _result2 = supportsAidPrefixRegistration();
                        reply.writeNoException();
                        reply.writeInt(_result2);
                        return true;
                    case 12:
                        data.enforceInterface(descriptor);
                        _arg0 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = (ComponentName) ComponentName.CREATOR.createFromParcel(data);
                        }
                        registerService(_arg0, _arg1, data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 13:
                        data.enforceInterface(descriptor);
                        _arg0 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = (ComponentName) ComponentName.CREATOR.createFromParcel(data);
                        }
                        unregisterOtherService(_arg0, _arg1);
                        reply.writeNoException();
                        return true;
                    case 14:
                        data.enforceInterface(descriptor);
                        _arg0 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = (ComponentName) ComponentName.CREATOR.createFromParcel(data);
                        }
                        _result = isRegisteredService(_arg0, _arg1, data.readString());
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 15:
                        data.enforceInterface(descriptor);
                        _arg02 = getUsedAidTableSizeInPercent(data.readInt(), data.readString());
                        reply.writeNoException();
                        reply.writeInt(_arg02);
                        return true;
                    case 16:
                        data.enforceInterface(descriptor);
                        initializePaymentDefault(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 17:
                        data.enforceInterface(descriptor);
                        _arg12 = getPaymentPriority(data.readInt());
                        reply.writeNoException();
                        if (_arg12 != null) {
                            reply.writeInt(1);
                            _arg12.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 18:
                        data.enforceInterface(descriptor);
                        ApduServiceInfo _result7 = getPaymentDefaultServiceInfo(data.readInt());
                        reply.writeNoException();
                        if (_result7 != null) {
                            reply.writeInt(1);
                            _result7.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    AidGroup getAidGroupForService(int i, ComponentName componentName, String str) throws RemoteException;

    ApduServiceInfo getPaymentDefaultServiceInfo(int i) throws RemoteException;

    ComponentName getPaymentPriority(int i) throws RemoteException;

    List<ApduServiceInfo> getServices(int i, String str) throws RemoteException;

    int getUsedAidTableSizeInPercent(int i, String str) throws RemoteException;

    void initializePaymentDefault(int i, int i2) throws RemoteException;

    boolean isDefaultServiceForAid(int i, ComponentName componentName, String str) throws RemoteException;

    boolean isDefaultServiceForCategory(int i, ComponentName componentName, String str) throws RemoteException;

    boolean isRegisteredService(int i, ComponentName componentName, String str) throws RemoteException;

    boolean registerAidGroupForService(int i, ComponentName componentName, AidGroup aidGroup) throws RemoteException;

    void registerService(int i, ComponentName componentName, String str, int i2) throws RemoteException;

    boolean removeAidGroupForService(int i, ComponentName componentName, String str) throws RemoteException;

    boolean setDefaultForNextTap(int i, ComponentName componentName) throws RemoteException;

    boolean setDefaultServiceForCategory(int i, ComponentName componentName, String str) throws RemoteException;

    boolean setPreferredService(ComponentName componentName) throws RemoteException;

    boolean supportsAidPrefixRegistration() throws RemoteException;

    void unregisterOtherService(int i, ComponentName componentName) throws RemoteException;

    boolean unsetPreferredService() throws RemoteException;
}
