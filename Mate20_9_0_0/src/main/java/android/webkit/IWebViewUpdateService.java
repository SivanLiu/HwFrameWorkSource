package android.webkit;

import android.content.pm.PackageInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IWebViewUpdateService extends IInterface {

    public static abstract class Stub extends Binder implements IWebViewUpdateService {
        private static final String DESCRIPTOR = "android.webkit.IWebViewUpdateService";
        static final int TRANSACTION_changeProviderAndSetting = 3;
        static final int TRANSACTION_enableFallbackLogic = 9;
        static final int TRANSACTION_enableMultiProcess = 11;
        static final int TRANSACTION_getAllWebViewPackages = 5;
        static final int TRANSACTION_getCurrentWebViewPackage = 7;
        static final int TRANSACTION_getCurrentWebViewPackageName = 6;
        static final int TRANSACTION_getValidWebViewPackages = 4;
        static final int TRANSACTION_isFallbackPackage = 8;
        static final int TRANSACTION_isMultiProcessEnabled = 10;
        static final int TRANSACTION_notifyRelroCreationCompleted = 1;
        static final int TRANSACTION_waitForAndGetProvider = 2;

        private static class Proxy implements IWebViewUpdateService {
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

            public void notifyRelroCreationCompleted() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public WebViewProviderResponse waitForAndGetProvider() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    WebViewProviderResponse _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (WebViewProviderResponse) WebViewProviderResponse.CREATOR.createFromParcel(_reply);
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

            public String changeProviderAndSetting(String newProvider) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(newProvider);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public WebViewProviderInfo[] getValidWebViewPackages() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    WebViewProviderInfo[] _result = (WebViewProviderInfo[]) _reply.createTypedArray(WebViewProviderInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public WebViewProviderInfo[] getAllWebViewPackages() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    WebViewProviderInfo[] _result = (WebViewProviderInfo[]) _reply.createTypedArray(WebViewProviderInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getCurrentWebViewPackageName() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public PackageInfo getCurrentWebViewPackage() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    PackageInfo _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (PackageInfo) PackageInfo.CREATOR.createFromParcel(_reply);
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

            public boolean isFallbackPackage(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
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

            public void enableFallbackLogic(boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enable);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isMultiProcessEnabled() throws RemoteException {
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

            public void enableMultiProcess(boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enable);
                    this.mRemote.transact(11, _data, _reply, 0);
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

        public static IWebViewUpdateService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IWebViewUpdateService)) {
                return new Proxy(obj);
            }
            return (IWebViewUpdateService) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != IBinder.INTERFACE_TRANSACTION) {
                boolean _arg0 = false;
                WebViewProviderInfo[] _result;
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        notifyRelroCreationCompleted();
                        reply.writeNoException();
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        WebViewProviderResponse _result2 = waitForAndGetProvider();
                        reply.writeNoException();
                        if (_result2 != null) {
                            reply.writeInt(1);
                            _result2.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        String _result3 = changeProviderAndSetting(data.readString());
                        reply.writeNoException();
                        reply.writeString(_result3);
                        return true;
                    case 4:
                        data.enforceInterface(descriptor);
                        _result = getValidWebViewPackages();
                        reply.writeNoException();
                        reply.writeTypedArray(_result, 1);
                        return true;
                    case 5:
                        data.enforceInterface(descriptor);
                        _result = getAllWebViewPackages();
                        reply.writeNoException();
                        reply.writeTypedArray(_result, 1);
                        return true;
                    case 6:
                        data.enforceInterface(descriptor);
                        String _result4 = getCurrentWebViewPackageName();
                        reply.writeNoException();
                        reply.writeString(_result4);
                        return true;
                    case 7:
                        data.enforceInterface(descriptor);
                        PackageInfo _result5 = getCurrentWebViewPackage();
                        reply.writeNoException();
                        if (_result5 != null) {
                            reply.writeInt(1);
                            _result5.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 8:
                        data.enforceInterface(descriptor);
                        boolean _result6 = isFallbackPackage(data.readString());
                        reply.writeNoException();
                        reply.writeInt(_result6);
                        return true;
                    case 9:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = true;
                        }
                        enableFallbackLogic(_arg0);
                        reply.writeNoException();
                        return true;
                    case 10:
                        data.enforceInterface(descriptor);
                        _arg0 = isMultiProcessEnabled();
                        reply.writeNoException();
                        reply.writeInt(_arg0);
                        return true;
                    case 11:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = true;
                        }
                        enableMultiProcess(_arg0);
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

    String changeProviderAndSetting(String str) throws RemoteException;

    void enableFallbackLogic(boolean z) throws RemoteException;

    void enableMultiProcess(boolean z) throws RemoteException;

    WebViewProviderInfo[] getAllWebViewPackages() throws RemoteException;

    PackageInfo getCurrentWebViewPackage() throws RemoteException;

    String getCurrentWebViewPackageName() throws RemoteException;

    WebViewProviderInfo[] getValidWebViewPackages() throws RemoteException;

    boolean isFallbackPackage(String str) throws RemoteException;

    boolean isMultiProcessEnabled() throws RemoteException;

    void notifyRelroCreationCompleted() throws RemoteException;

    WebViewProviderResponse waitForAndGetProvider() throws RemoteException;
}
