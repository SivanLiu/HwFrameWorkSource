package com.android.internal.telephony;

import android.app.PendingIntent;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IMms extends IInterface {

    public static abstract class Stub extends Binder implements IMms {
        private static final String DESCRIPTOR = "com.android.internal.telephony.IMms";
        static final int TRANSACTION_addMultimediaMessageDraft = 11;
        static final int TRANSACTION_addTextMessageDraft = 10;
        static final int TRANSACTION_archiveStoredConversation = 9;
        static final int TRANSACTION_deleteStoredConversation = 7;
        static final int TRANSACTION_deleteStoredMessage = 6;
        static final int TRANSACTION_downloadMessage = 2;
        static final int TRANSACTION_getAutoPersisting = 14;
        static final int TRANSACTION_getCarrierConfigValues = 3;
        static final int TRANSACTION_importMultimediaMessage = 5;
        static final int TRANSACTION_importTextMessage = 4;
        static final int TRANSACTION_sendMessage = 1;
        static final int TRANSACTION_sendStoredMessage = 12;
        static final int TRANSACTION_setAutoPersisting = 13;
        static final int TRANSACTION_updateStoredMessageStatus = 8;

        private static class Proxy implements IMms {
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

            public void sendMessage(int subId, String callingPkg, Uri contentUri, String locationUrl, Bundle configOverrides, PendingIntent sentIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPkg);
                    if (contentUri != null) {
                        _data.writeInt(1);
                        contentUri.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(locationUrl);
                    if (configOverrides != null) {
                        _data.writeInt(1);
                        configOverrides.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (sentIntent != null) {
                        _data.writeInt(1);
                        sentIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void downloadMessage(int subId, String callingPkg, String locationUrl, Uri contentUri, Bundle configOverrides, PendingIntent downloadedIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPkg);
                    _data.writeString(locationUrl);
                    if (contentUri != null) {
                        _data.writeInt(1);
                        contentUri.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (configOverrides != null) {
                        _data.writeInt(1);
                        configOverrides.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (downloadedIntent != null) {
                        _data.writeInt(1);
                        downloadedIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public Bundle getCarrierConfigValues(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Bundle _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Bundle) Bundle.CREATOR.createFromParcel(_reply);
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

            public Uri importTextMessage(String callingPkg, String address, int type, String text, long timestampMillis, boolean seen, boolean read) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Uri _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPkg);
                    _data.writeString(address);
                    _data.writeInt(type);
                    _data.writeString(text);
                    _data.writeLong(timestampMillis);
                    _data.writeInt(seen);
                    _data.writeInt(read);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Uri) Uri.CREATOR.createFromParcel(_reply);
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

            public Uri importMultimediaMessage(String callingPkg, Uri contentUri, String messageId, long timestampSecs, boolean seen, boolean read) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Uri _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPkg);
                    if (contentUri != null) {
                        _data.writeInt(1);
                        contentUri.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(messageId);
                    _data.writeLong(timestampSecs);
                    _data.writeInt(seen);
                    _data.writeInt(read);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Uri) Uri.CREATOR.createFromParcel(_reply);
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

            public boolean deleteStoredMessage(String callingPkg, Uri messageUri) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPkg);
                    boolean _result = true;
                    if (messageUri != null) {
                        _data.writeInt(1);
                        messageUri.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, _reply, 0);
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

            public boolean deleteStoredConversation(String callingPkg, long conversationId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPkg);
                    _data.writeLong(conversationId);
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

            public boolean updateStoredMessageStatus(String callingPkg, Uri messageUri, ContentValues statusValues) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPkg);
                    boolean _result = true;
                    if (messageUri != null) {
                        _data.writeInt(1);
                        messageUri.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (statusValues != null) {
                        _data.writeInt(1);
                        statusValues.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(8, _data, _reply, 0);
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

            public boolean archiveStoredConversation(String callingPkg, long conversationId, boolean archived) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPkg);
                    _data.writeLong(conversationId);
                    _data.writeInt(archived);
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

            public Uri addTextMessageDraft(String callingPkg, String address, String text) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Uri _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPkg);
                    _data.writeString(address);
                    _data.writeString(text);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Uri) Uri.CREATOR.createFromParcel(_reply);
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

            public Uri addMultimediaMessageDraft(String callingPkg, Uri contentUri) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Uri _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPkg);
                    if (contentUri != null) {
                        _data.writeInt(1);
                        contentUri.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Uri) Uri.CREATOR.createFromParcel(_reply);
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

            public void sendStoredMessage(int subId, String callingPkg, Uri messageUri, Bundle configOverrides, PendingIntent sentIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPkg);
                    if (messageUri != null) {
                        _data.writeInt(1);
                        messageUri.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (configOverrides != null) {
                        _data.writeInt(1);
                        configOverrides.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (sentIntent != null) {
                        _data.writeInt(1);
                        sentIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setAutoPersisting(String callingPkg, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPkg);
                    _data.writeInt(enabled);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean getAutoPersisting() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(14, _data, _reply, 0);
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
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IMms asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IMms)) {
                return new Proxy(obj);
            }
            return (IMms) iin;
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
                boolean z = false;
                PendingIntent _arg1 = null;
                int _arg0;
                String _arg12;
                Uri _arg2;
                String _arg3;
                Bundle _arg4;
                Uri _arg13;
                String _arg02;
                boolean _result;
                Uri _arg14;
                String _arg03;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readInt();
                        _arg12 = data.readString();
                        if (data.readInt() != 0) {
                            _arg2 = (Uri) Uri.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg2 = null;
                        }
                        _arg3 = data.readString();
                        if (data.readInt() != 0) {
                            _arg4 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg4 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg1 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel);
                        }
                        sendMessage(_arg0, _arg12, _arg2, _arg3, _arg4, _arg1);
                        reply.writeNoException();
                        return true;
                    case 2:
                        Uri _arg32;
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readInt();
                        _arg12 = data.readString();
                        _arg3 = data.readString();
                        if (data.readInt() != 0) {
                            _arg32 = (Uri) Uri.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg32 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg4 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg4 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg13 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel);
                        }
                        downloadMessage(_arg0, _arg12, _arg3, _arg32, _arg4, _arg13);
                        reply.writeNoException();
                        return true;
                    case 3:
                        parcel.enforceInterface(descriptor);
                        Bundle _result2 = getCarrierConfigValues(data.readInt());
                        reply.writeNoException();
                        if (_result2 != null) {
                            parcel2.writeInt(1);
                            _result2.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        _arg13 = importTextMessage(data.readString(), data.readString(), data.readInt(), data.readString(), data.readLong(), data.readInt() != 0, data.readInt() != 0);
                        reply.writeNoException();
                        if (_arg13 != null) {
                            parcel2.writeInt(1);
                            _arg13.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        _arg12 = data.readString();
                        if (data.readInt() != 0) {
                            _arg13 = (Uri) Uri.CREATOR.createFromParcel(parcel);
                        }
                        _arg13 = importMultimediaMessage(_arg12, _arg13, data.readString(), data.readLong(), data.readInt() != 0, data.readInt() != 0);
                        reply.writeNoException();
                        if (_arg13 != null) {
                            parcel2.writeInt(1);
                            _arg13.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readString();
                        if (data.readInt() != 0) {
                            _arg13 = (Uri) Uri.CREATOR.createFromParcel(parcel);
                        }
                        boolean _result3 = deleteStoredMessage(_arg02, _arg13);
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        _result = deleteStoredConversation(data.readString(), data.readLong());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 8:
                        ContentValues _arg22;
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readString();
                        if (data.readInt() != 0) {
                            _arg14 = (Uri) Uri.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg14 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg22 = (ContentValues) ContentValues.CREATOR.createFromParcel(parcel);
                        }
                        _result = updateStoredMessageStatus(_arg02, _arg14, _arg22);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        _arg03 = data.readString();
                        long _arg15 = data.readLong();
                        if (data.readInt() != 0) {
                            z = true;
                        }
                        boolean _result4 = archiveStoredConversation(_arg03, _arg15, z);
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 10:
                        parcel.enforceInterface(descriptor);
                        _arg2 = addTextMessageDraft(data.readString(), data.readString(), data.readString());
                        reply.writeNoException();
                        if (_arg2 != null) {
                            parcel2.writeInt(1);
                            _arg2.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readString();
                        if (data.readInt() != 0) {
                            _arg13 = (Uri) Uri.CREATOR.createFromParcel(parcel);
                        }
                        _arg14 = addMultimediaMessageDraft(_arg02, _arg13);
                        reply.writeNoException();
                        if (_arg14 != null) {
                            parcel2.writeInt(1);
                            _arg14.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 12:
                        Bundle _arg33;
                        parcel.enforceInterface(descriptor);
                        int _arg04 = data.readInt();
                        String _arg16 = data.readString();
                        if (data.readInt() != 0) {
                            _arg2 = (Uri) Uri.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg2 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg33 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg33 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg1 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel);
                        }
                        sendStoredMessage(_arg04, _arg16, _arg2, _arg33, _arg1);
                        reply.writeNoException();
                        return true;
                    case 13:
                        parcel.enforceInterface(descriptor);
                        _arg03 = data.readString();
                        if (data.readInt() != 0) {
                            z = true;
                        }
                        setAutoPersisting(_arg03, z);
                        reply.writeNoException();
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        boolean _result5 = getAutoPersisting();
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            parcel2.writeString(descriptor);
            return true;
        }
    }

    Uri addMultimediaMessageDraft(String str, Uri uri) throws RemoteException;

    Uri addTextMessageDraft(String str, String str2, String str3) throws RemoteException;

    boolean archiveStoredConversation(String str, long j, boolean z) throws RemoteException;

    boolean deleteStoredConversation(String str, long j) throws RemoteException;

    boolean deleteStoredMessage(String str, Uri uri) throws RemoteException;

    void downloadMessage(int i, String str, String str2, Uri uri, Bundle bundle, PendingIntent pendingIntent) throws RemoteException;

    boolean getAutoPersisting() throws RemoteException;

    Bundle getCarrierConfigValues(int i) throws RemoteException;

    Uri importMultimediaMessage(String str, Uri uri, String str2, long j, boolean z, boolean z2) throws RemoteException;

    Uri importTextMessage(String str, String str2, int i, String str3, long j, boolean z, boolean z2) throws RemoteException;

    void sendMessage(int i, String str, Uri uri, String str2, Bundle bundle, PendingIntent pendingIntent) throws RemoteException;

    void sendStoredMessage(int i, String str, Uri uri, Bundle bundle, PendingIntent pendingIntent) throws RemoteException;

    void setAutoPersisting(String str, boolean z) throws RemoteException;

    boolean updateStoredMessageStatus(String str, Uri uri, ContentValues contentValues) throws RemoteException;
}
