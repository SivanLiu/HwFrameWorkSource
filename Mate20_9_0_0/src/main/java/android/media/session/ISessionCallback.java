package android.media.session;

import android.content.Intent;
import android.media.Rating;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;

public interface ISessionCallback extends IInterface {

    public static abstract class Stub extends Binder implements ISessionCallback {
        private static final String DESCRIPTOR = "android.media.session.ISessionCallback";
        static final int TRANSACTION_onAdjustVolume = 22;
        static final int TRANSACTION_onCommand = 1;
        static final int TRANSACTION_onCustomAction = 21;
        static final int TRANSACTION_onFastForward = 17;
        static final int TRANSACTION_onMediaButton = 2;
        static final int TRANSACTION_onMediaButtonFromController = 3;
        static final int TRANSACTION_onNext = 15;
        static final int TRANSACTION_onPause = 13;
        static final int TRANSACTION_onPlay = 8;
        static final int TRANSACTION_onPlayFromMediaId = 9;
        static final int TRANSACTION_onPlayFromSearch = 10;
        static final int TRANSACTION_onPlayFromUri = 11;
        static final int TRANSACTION_onPrepare = 4;
        static final int TRANSACTION_onPrepareFromMediaId = 5;
        static final int TRANSACTION_onPrepareFromSearch = 6;
        static final int TRANSACTION_onPrepareFromUri = 7;
        static final int TRANSACTION_onPrevious = 16;
        static final int TRANSACTION_onRate = 20;
        static final int TRANSACTION_onRewind = 18;
        static final int TRANSACTION_onSeekTo = 19;
        static final int TRANSACTION_onSetVolumeTo = 23;
        static final int TRANSACTION_onSkipToTrack = 12;
        static final int TRANSACTION_onStop = 14;

        private static class Proxy implements ISessionCallback {
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

            public void onCommand(String packageName, int pid, int uid, ISessionControllerCallback caller, String command, Bundle args, ResultReceiver cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(command);
                    if (args != null) {
                        _data.writeInt(1);
                        args.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (cb != null) {
                        _data.writeInt(1);
                        cb.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onMediaButton(String packageName, int pid, int uid, Intent mediaButtonIntent, int sequenceNumber, ResultReceiver cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    if (mediaButtonIntent != null) {
                        _data.writeInt(1);
                        mediaButtonIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(sequenceNumber);
                    if (cb != null) {
                        _data.writeInt(1);
                        cb.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onMediaButtonFromController(String packageName, int pid, int uid, ISessionControllerCallback caller, Intent mediaButtonIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    if (mediaButtonIntent != null) {
                        _data.writeInt(1);
                        mediaButtonIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onPrepare(String packageName, int pid, int uid, ISessionControllerCallback caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onPrepareFromMediaId(String packageName, int pid, int uid, ISessionControllerCallback caller, String mediaId, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(mediaId);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onPrepareFromSearch(String packageName, int pid, int uid, ISessionControllerCallback caller, String query, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(query);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onPrepareFromUri(String packageName, int pid, int uid, ISessionControllerCallback caller, Uri uri, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    if (uri != null) {
                        _data.writeInt(1);
                        uri.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onPlay(String packageName, int pid, int uid, ISessionControllerCallback caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onPlayFromMediaId(String packageName, int pid, int uid, ISessionControllerCallback caller, String mediaId, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(mediaId);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onPlayFromSearch(String packageName, int pid, int uid, ISessionControllerCallback caller, String query, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(query);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onPlayFromUri(String packageName, int pid, int uid, ISessionControllerCallback caller, Uri uri, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    if (uri != null) {
                        _data.writeInt(1);
                        uri.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onSkipToTrack(String packageName, int pid, int uid, ISessionControllerCallback caller, long id) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeLong(id);
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onPause(String packageName, int pid, int uid, ISessionControllerCallback caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onStop(String packageName, int pid, int uid, ISessionControllerCallback caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onNext(String packageName, int pid, int uid, ISessionControllerCallback caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onPrevious(String packageName, int pid, int uid, ISessionControllerCallback caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(16, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onFastForward(String packageName, int pid, int uid, ISessionControllerCallback caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(17, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onRewind(String packageName, int pid, int uid, ISessionControllerCallback caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    this.mRemote.transact(18, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onSeekTo(String packageName, int pid, int uid, ISessionControllerCallback caller, long pos) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeLong(pos);
                    this.mRemote.transact(19, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onRate(String packageName, int pid, int uid, ISessionControllerCallback caller, Rating rating) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    if (rating != null) {
                        _data.writeInt(1);
                        rating.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(20, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onCustomAction(String packageName, int pid, int uid, ISessionControllerCallback caller, String action, Bundle args) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeString(action);
                    if (args != null) {
                        _data.writeInt(1);
                        args.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(21, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onAdjustVolume(String packageName, int pid, int uid, ISessionControllerCallback caller, int direction) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeInt(direction);
                    this.mRemote.transact(22, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void onSetVolumeTo(String packageName, int pid, int uid, ISessionControllerCallback caller, int value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(pid);
                    _data.writeInt(uid);
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeInt(value);
                    this.mRemote.transact(23, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ISessionCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof ISessionCallback)) {
                return new Proxy(obj);
            }
            return (ISessionCallback) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            String descriptor = DESCRIPTOR;
            if (i != 1598968902) {
                ResultReceiver resultReceiver = null;
                int _arg1;
                int _arg2;
                String _arg0;
                int _arg12;
                String _arg02;
                int _arg13;
                ISessionControllerCallback _arg3;
                ISessionControllerCallback _arg32;
                String _arg4;
                Uri _arg42;
                switch (i) {
                    case 1:
                        Bundle _arg5;
                        parcel.enforceInterface(descriptor);
                        String _arg03 = data.readString();
                        _arg1 = data.readInt();
                        _arg2 = data.readInt();
                        ISessionControllerCallback _arg33 = android.media.session.ISessionControllerCallback.Stub.asInterface(data.readStrongBinder());
                        String _arg43 = data.readString();
                        if (data.readInt() != 0) {
                            _arg5 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg5 = null;
                        }
                        if (data.readInt() != 0) {
                            resultReceiver = (ResultReceiver) ResultReceiver.CREATOR.createFromParcel(parcel);
                        }
                        onCommand(_arg03, _arg1, _arg2, _arg33, _arg43, _arg5, resultReceiver);
                        return true;
                    case 2:
                        Intent _arg34;
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readString();
                        _arg12 = data.readInt();
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg34 = (Intent) Intent.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg34 = null;
                        }
                        _arg2 = data.readInt();
                        if (data.readInt() != 0) {
                            resultReceiver = (ResultReceiver) ResultReceiver.CREATOR.createFromParcel(parcel);
                        }
                        onMediaButton(_arg0, _arg12, _arg1, _arg34, _arg2, resultReceiver);
                        return true;
                    case 3:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readString();
                        _arg13 = data.readInt();
                        _arg12 = data.readInt();
                        _arg3 = android.media.session.ISessionControllerCallback.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            resultReceiver = (Intent) Intent.CREATOR.createFromParcel(parcel);
                        }
                        onMediaButtonFromController(_arg02, _arg13, _arg12, _arg3, resultReceiver);
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        onPrepare(data.readString(), data.readInt(), data.readInt(), android.media.session.ISessionControllerCallback.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readString();
                        _arg12 = data.readInt();
                        _arg1 = data.readInt();
                        _arg32 = android.media.session.ISessionControllerCallback.Stub.asInterface(data.readStrongBinder());
                        _arg4 = data.readString();
                        if (data.readInt() != 0) {
                            resultReceiver = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        }
                        onPrepareFromMediaId(_arg0, _arg12, _arg1, _arg32, _arg4, resultReceiver);
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readString();
                        _arg12 = data.readInt();
                        _arg1 = data.readInt();
                        _arg32 = android.media.session.ISessionControllerCallback.Stub.asInterface(data.readStrongBinder());
                        _arg4 = data.readString();
                        if (data.readInt() != 0) {
                            resultReceiver = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        }
                        onPrepareFromSearch(_arg0, _arg12, _arg1, _arg32, _arg4, resultReceiver);
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readString();
                        _arg12 = data.readInt();
                        _arg1 = data.readInt();
                        _arg32 = android.media.session.ISessionControllerCallback.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg42 = (Uri) Uri.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg42 = null;
                        }
                        if (data.readInt() != 0) {
                            resultReceiver = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        }
                        onPrepareFromUri(_arg0, _arg12, _arg1, _arg32, _arg42, resultReceiver);
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        onPlay(data.readString(), data.readInt(), data.readInt(), android.media.session.ISessionControllerCallback.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readString();
                        _arg12 = data.readInt();
                        _arg1 = data.readInt();
                        _arg32 = android.media.session.ISessionControllerCallback.Stub.asInterface(data.readStrongBinder());
                        _arg4 = data.readString();
                        if (data.readInt() != 0) {
                            resultReceiver = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        }
                        onPlayFromMediaId(_arg0, _arg12, _arg1, _arg32, _arg4, resultReceiver);
                        return true;
                    case 10:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readString();
                        _arg12 = data.readInt();
                        _arg1 = data.readInt();
                        _arg32 = android.media.session.ISessionControllerCallback.Stub.asInterface(data.readStrongBinder());
                        _arg4 = data.readString();
                        if (data.readInt() != 0) {
                            resultReceiver = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        }
                        onPlayFromSearch(_arg0, _arg12, _arg1, _arg32, _arg4, resultReceiver);
                        return true;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readString();
                        _arg12 = data.readInt();
                        _arg1 = data.readInt();
                        _arg32 = android.media.session.ISessionControllerCallback.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg42 = (Uri) Uri.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg42 = null;
                        }
                        if (data.readInt() != 0) {
                            resultReceiver = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        }
                        onPlayFromUri(_arg0, _arg12, _arg1, _arg32, _arg42, resultReceiver);
                        return true;
                    case 12:
                        parcel.enforceInterface(descriptor);
                        onSkipToTrack(data.readString(), data.readInt(), data.readInt(), android.media.session.ISessionControllerCallback.Stub.asInterface(data.readStrongBinder()), data.readLong());
                        return true;
                    case 13:
                        parcel.enforceInterface(descriptor);
                        onPause(data.readString(), data.readInt(), data.readInt(), android.media.session.ISessionControllerCallback.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        onStop(data.readString(), data.readInt(), data.readInt(), android.media.session.ISessionControllerCallback.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        onNext(data.readString(), data.readInt(), data.readInt(), android.media.session.ISessionControllerCallback.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 16:
                        parcel.enforceInterface(descriptor);
                        onPrevious(data.readString(), data.readInt(), data.readInt(), android.media.session.ISessionControllerCallback.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        onFastForward(data.readString(), data.readInt(), data.readInt(), android.media.session.ISessionControllerCallback.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 18:
                        parcel.enforceInterface(descriptor);
                        onRewind(data.readString(), data.readInt(), data.readInt(), android.media.session.ISessionControllerCallback.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 19:
                        parcel.enforceInterface(descriptor);
                        onSeekTo(data.readString(), data.readInt(), data.readInt(), android.media.session.ISessionControllerCallback.Stub.asInterface(data.readStrongBinder()), data.readLong());
                        return true;
                    case 20:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readString();
                        _arg13 = data.readInt();
                        _arg12 = data.readInt();
                        _arg3 = android.media.session.ISessionControllerCallback.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            resultReceiver = (Rating) Rating.CREATOR.createFromParcel(parcel);
                        }
                        onRate(_arg02, _arg13, _arg12, _arg3, resultReceiver);
                        return true;
                    case 21:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readString();
                        _arg12 = data.readInt();
                        _arg1 = data.readInt();
                        _arg32 = android.media.session.ISessionControllerCallback.Stub.asInterface(data.readStrongBinder());
                        _arg4 = data.readString();
                        if (data.readInt() != 0) {
                            resultReceiver = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        }
                        onCustomAction(_arg0, _arg12, _arg1, _arg32, _arg4, resultReceiver);
                        return true;
                    case 22:
                        parcel.enforceInterface(descriptor);
                        onAdjustVolume(data.readString(), data.readInt(), data.readInt(), android.media.session.ISessionControllerCallback.Stub.asInterface(data.readStrongBinder()), data.readInt());
                        return true;
                    case 23:
                        parcel.enforceInterface(descriptor);
                        onSetVolumeTo(data.readString(), data.readInt(), data.readInt(), android.media.session.ISessionControllerCallback.Stub.asInterface(data.readStrongBinder()), data.readInt());
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    void onAdjustVolume(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, int i3) throws RemoteException;

    void onCommand(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle, ResultReceiver resultReceiver) throws RemoteException;

    void onCustomAction(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) throws RemoteException;

    void onFastForward(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) throws RemoteException;

    void onMediaButton(String str, int i, int i2, Intent intent, int i3, ResultReceiver resultReceiver) throws RemoteException;

    void onMediaButtonFromController(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, Intent intent) throws RemoteException;

    void onNext(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) throws RemoteException;

    void onPause(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) throws RemoteException;

    void onPlay(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) throws RemoteException;

    void onPlayFromMediaId(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) throws RemoteException;

    void onPlayFromSearch(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) throws RemoteException;

    void onPlayFromUri(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, Uri uri, Bundle bundle) throws RemoteException;

    void onPrepare(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) throws RemoteException;

    void onPrepareFromMediaId(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) throws RemoteException;

    void onPrepareFromSearch(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, String str2, Bundle bundle) throws RemoteException;

    void onPrepareFromUri(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, Uri uri, Bundle bundle) throws RemoteException;

    void onPrevious(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) throws RemoteException;

    void onRate(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, Rating rating) throws RemoteException;

    void onRewind(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) throws RemoteException;

    void onSeekTo(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, long j) throws RemoteException;

    void onSetVolumeTo(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, int i3) throws RemoteException;

    void onSkipToTrack(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback, long j) throws RemoteException;

    void onStop(String str, int i, int i2, ISessionControllerCallback iSessionControllerCallback) throws RemoteException;
}
