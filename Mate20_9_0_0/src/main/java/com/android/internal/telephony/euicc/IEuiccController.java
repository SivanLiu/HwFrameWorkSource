package com.android.internal.telephony.euicc;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telephony.euicc.DownloadableSubscription;
import android.telephony.euicc.EuiccInfo;

public interface IEuiccController extends IInterface {

    public static abstract class Stub extends Binder implements IEuiccController {
        private static final String DESCRIPTOR = "com.android.internal.telephony.euicc.IEuiccController";
        static final int TRANSACTION_continueOperation = 1;
        static final int TRANSACTION_deleteSubscription = 8;
        static final int TRANSACTION_downloadSubscription = 6;
        static final int TRANSACTION_eraseSubscriptions = 11;
        static final int TRANSACTION_getDefaultDownloadableSubscriptionList = 3;
        static final int TRANSACTION_getDownloadableSubscriptionMetadata = 2;
        static final int TRANSACTION_getEid = 4;
        static final int TRANSACTION_getEuiccInfo = 7;
        static final int TRANSACTION_getOtaStatus = 5;
        static final int TRANSACTION_retainSubscriptionsForFactoryReset = 12;
        static final int TRANSACTION_switchToSubscription = 9;
        static final int TRANSACTION_updateSubscriptionNickname = 10;

        private static class Proxy implements IEuiccController {
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

            public void continueOperation(Intent resolutionIntent, Bundle resolutionExtras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (resolutionIntent != null) {
                        _data.writeInt(1);
                        resolutionIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (resolutionExtras != null) {
                        _data.writeInt(1);
                        resolutionExtras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void getDownloadableSubscriptionMetadata(DownloadableSubscription subscription, String callingPackage, PendingIntent callbackIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (subscription != null) {
                        _data.writeInt(1);
                        subscription.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callingPackage);
                    if (callbackIntent != null) {
                        _data.writeInt(1);
                        callbackIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void getDefaultDownloadableSubscriptionList(String callingPackage, PendingIntent callbackIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    if (callbackIntent != null) {
                        _data.writeInt(1);
                        callbackIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public String getEid() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getOtaStatus() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void downloadSubscription(DownloadableSubscription subscription, boolean switchAfterDownload, String callingPackage, PendingIntent callbackIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (subscription != null) {
                        _data.writeInt(1);
                        subscription.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(switchAfterDownload);
                    _data.writeString(callingPackage);
                    if (callbackIntent != null) {
                        _data.writeInt(1);
                        callbackIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public EuiccInfo getEuiccInfo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    EuiccInfo _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (EuiccInfo) EuiccInfo.CREATOR.createFromParcel(_reply);
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

            public void deleteSubscription(int subscriptionId, String callingPackage, PendingIntent callbackIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subscriptionId);
                    _data.writeString(callingPackage);
                    if (callbackIntent != null) {
                        _data.writeInt(1);
                        callbackIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void switchToSubscription(int subscriptionId, String callingPackage, PendingIntent callbackIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subscriptionId);
                    _data.writeString(callingPackage);
                    if (callbackIntent != null) {
                        _data.writeInt(1);
                        callbackIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void updateSubscriptionNickname(int subscriptionId, String nickname, PendingIntent callbackIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subscriptionId);
                    _data.writeString(nickname);
                    if (callbackIntent != null) {
                        _data.writeInt(1);
                        callbackIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void eraseSubscriptions(PendingIntent callbackIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (callbackIntent != null) {
                        _data.writeInt(1);
                        callbackIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void retainSubscriptionsForFactoryReset(PendingIntent callbackIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (callbackIntent != null) {
                        _data.writeInt(1);
                        callbackIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IEuiccController asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IEuiccController)) {
                return new Proxy(obj);
            }
            return (IEuiccController) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != 1598968902) {
                boolean _arg1 = false;
                Bundle _arg2 = null;
                String _arg12;
                PendingIntent _arg22;
                String _arg0;
                int _result;
                switch (code) {
                    case 1:
                        Intent _arg02;
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (Intent) Intent.CREATOR.createFromParcel(data);
                        } else {
                            _arg02 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg2 = (Bundle) Bundle.CREATOR.createFromParcel(data);
                        }
                        continueOperation(_arg02, _arg2);
                        return true;
                    case 2:
                        DownloadableSubscription _arg03;
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (DownloadableSubscription) DownloadableSubscription.CREATOR.createFromParcel(data);
                        } else {
                            _arg03 = null;
                        }
                        _arg12 = data.readString();
                        if (data.readInt() != 0) {
                            _arg22 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(data);
                        }
                        getDownloadableSubscriptionMetadata(_arg03, _arg12, _arg22);
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        _arg0 = data.readString();
                        if (data.readInt() != 0) {
                            _arg22 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(data);
                        }
                        getDefaultDownloadableSubscriptionList(_arg0, _arg22);
                        return true;
                    case 4:
                        data.enforceInterface(descriptor);
                        _arg0 = getEid();
                        reply.writeNoException();
                        reply.writeString(_arg0);
                        return true;
                    case 5:
                        data.enforceInterface(descriptor);
                        _result = getOtaStatus();
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 6:
                        DownloadableSubscription _arg04;
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg04 = (DownloadableSubscription) DownloadableSubscription.CREATOR.createFromParcel(data);
                        } else {
                            _arg04 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        String _arg23 = data.readString();
                        if (data.readInt() != 0) {
                            _arg22 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(data);
                        }
                        downloadSubscription(_arg04, _arg1, _arg23, _arg22);
                        return true;
                    case 7:
                        data.enforceInterface(descriptor);
                        EuiccInfo _result2 = getEuiccInfo();
                        reply.writeNoException();
                        if (_result2 != null) {
                            reply.writeInt(1);
                            _result2.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 8:
                        data.enforceInterface(descriptor);
                        _result = data.readInt();
                        _arg12 = data.readString();
                        if (data.readInt() != 0) {
                            _arg22 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(data);
                        }
                        deleteSubscription(_result, _arg12, _arg22);
                        return true;
                    case 9:
                        data.enforceInterface(descriptor);
                        _result = data.readInt();
                        _arg12 = data.readString();
                        if (data.readInt() != 0) {
                            _arg22 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(data);
                        }
                        switchToSubscription(_result, _arg12, _arg22);
                        return true;
                    case 10:
                        data.enforceInterface(descriptor);
                        _result = data.readInt();
                        _arg12 = data.readString();
                        if (data.readInt() != 0) {
                            _arg22 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(data);
                        }
                        updateSubscriptionNickname(_result, _arg12, _arg22);
                        return true;
                    case 11:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg22 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(data);
                        }
                        eraseSubscriptions(_arg22);
                        return true;
                    case 12:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg22 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(data);
                        }
                        retainSubscriptionsForFactoryReset(_arg22);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    void continueOperation(Intent intent, Bundle bundle) throws RemoteException;

    void deleteSubscription(int i, String str, PendingIntent pendingIntent) throws RemoteException;

    void downloadSubscription(DownloadableSubscription downloadableSubscription, boolean z, String str, PendingIntent pendingIntent) throws RemoteException;

    void eraseSubscriptions(PendingIntent pendingIntent) throws RemoteException;

    void getDefaultDownloadableSubscriptionList(String str, PendingIntent pendingIntent) throws RemoteException;

    void getDownloadableSubscriptionMetadata(DownloadableSubscription downloadableSubscription, String str, PendingIntent pendingIntent) throws RemoteException;

    String getEid() throws RemoteException;

    EuiccInfo getEuiccInfo() throws RemoteException;

    int getOtaStatus() throws RemoteException;

    void retainSubscriptionsForFactoryReset(PendingIntent pendingIntent) throws RemoteException;

    void switchToSubscription(int i, String str, PendingIntent pendingIntent) throws RemoteException;

    void updateSubscriptionNickname(int i, String str, PendingIntent pendingIntent) throws RemoteException;
}
