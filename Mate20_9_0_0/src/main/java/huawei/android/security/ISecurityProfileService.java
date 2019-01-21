package huawei.android.security;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import huawei.android.security.securityprofile.ApkDigest;
import java.util.List;

public interface ISecurityProfileService extends IInterface {

    public static abstract class Stub extends Binder implements ISecurityProfileService {
        private static final String DESCRIPTOR = "huawei.android.security.ISecurityProfileService";
        static final int TRANSACTION_addDomainPolicy = 5;
        static final int TRANSACTION_checkAccess = 4;
        static final int TRANSACTION_getInstallerPackageName = 8;
        static final int TRANSACTION_getLabels = 6;
        static final int TRANSACTION_isBlackApp = 2;
        static final int TRANSACTION_isPackageSigned = 7;
        static final int TRANSACTION_requestAccess = 3;
        static final int TRANSACTION_updateBlackApp = 1;

        private static class Proxy implements ISecurityProfileService {
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

            public void updateBlackApp(List packageList, int action) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeList(packageList);
                    _data.writeInt(action);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isBlackApp(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
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

            public boolean requestAccess(String subject, String object, String subsystem, List<String> subsystemParameters, String operation, int timeout) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(subject);
                    _data.writeString(object);
                    _data.writeString(subsystem);
                    _data.writeStringList(subsystemParameters);
                    _data.writeString(operation);
                    _data.writeInt(timeout);
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

            public boolean checkAccess(String subject, String object, String subsystem, List<String> subsystemParameters, String operation) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(subject);
                    _data.writeString(object);
                    _data.writeString(subsystem);
                    _data.writeStringList(subsystemParameters);
                    _data.writeString(operation);
                    boolean z = false;
                    this.mRemote.transact(4, _data, _reply, 0);
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

            public int addDomainPolicy(byte[] domainPolicy) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(domainPolicy);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<String> getLabels(String packageName, ApkDigest digest) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    if (digest != null) {
                        _data.writeInt(1);
                        digest.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isPackageSigned(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    boolean z = false;
                    this.mRemote.transact(7, _data, _reply, 0);
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

            public String getInstallerPackageName(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
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

        public static ISecurityProfileService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof ISecurityProfileService)) {
                return new Proxy(obj);
            }
            return (ISecurityProfileService) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            Parcel parcel2 = reply;
            String descriptor = DESCRIPTOR;
            if (i != 1598968902) {
                boolean _result;
                boolean _result2;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(descriptor);
                        updateBlackApp(parcel.readArrayList(getClass().getClassLoader()), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        _result = isBlackApp(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 3:
                        parcel.enforceInterface(descriptor);
                        _result2 = requestAccess(data.readString(), data.readString(), data.readString(), data.createStringArrayList(), data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        _result2 = checkAccess(data.readString(), data.readString(), data.readString(), data.createStringArrayList(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        int _result3 = addDomainPolicy(data.createByteArray());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 6:
                        ApkDigest _arg1;
                        parcel.enforceInterface(descriptor);
                        String _arg0 = data.readString();
                        if (data.readInt() != 0) {
                            _arg1 = (ApkDigest) ApkDigest.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg1 = null;
                        }
                        List<String> _result4 = getLabels(_arg0, _arg1);
                        reply.writeNoException();
                        parcel2.writeStringList(_result4);
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        _result = isPackageSigned(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        String _result5 = getInstallerPackageName(data.readString());
                        reply.writeNoException();
                        parcel2.writeString(_result5);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            parcel2.writeString(descriptor);
            return true;
        }
    }

    int addDomainPolicy(byte[] bArr) throws RemoteException;

    boolean checkAccess(String str, String str2, String str3, List<String> list, String str4) throws RemoteException;

    String getInstallerPackageName(String str) throws RemoteException;

    List<String> getLabels(String str, ApkDigest apkDigest) throws RemoteException;

    boolean isBlackApp(String str) throws RemoteException;

    boolean isPackageSigned(String str) throws RemoteException;

    boolean requestAccess(String str, String str2, String str3, List<String> list, String str4, int i) throws RemoteException;

    void updateBlackApp(List list, int i) throws RemoteException;
}
