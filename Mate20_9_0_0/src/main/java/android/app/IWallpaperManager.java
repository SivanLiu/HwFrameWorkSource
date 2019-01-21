package android.app;

import android.content.ComponentName;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

public interface IWallpaperManager extends IInterface {

    public static abstract class Stub extends Binder implements IWallpaperManager {
        private static final String DESCRIPTOR = "android.app.IWallpaperManager";
        static final int TRANSACTION_clearWallpaper = 7;
        static final int TRANSACTION_getBlurWallpaper = 12;
        static final int TRANSACTION_getCurrOffsets = 24;
        static final int TRANSACTION_getHeightHint = 11;
        static final int TRANSACTION_getName = 14;
        static final int TRANSACTION_getWallpaper = 4;
        static final int TRANSACTION_getWallpaperColors = 20;
        static final int TRANSACTION_getWallpaperIdForUser = 5;
        static final int TRANSACTION_getWallpaperInfo = 6;
        static final int TRANSACTION_getWallpaperUserId = 27;
        static final int TRANSACTION_getWidthHint = 10;
        static final int TRANSACTION_hasNamedWallpaper = 8;
        static final int TRANSACTION_isSetWallpaperAllowed = 17;
        static final int TRANSACTION_isWallpaperBackupEligible = 18;
        static final int TRANSACTION_isWallpaperSupported = 16;
        static final int TRANSACTION_registerWallpaperColorsCallback = 21;
        static final int TRANSACTION_scaleWallpaperBitmapToScreenSize = 28;
        static final int TRANSACTION_setCurrOffsets = 25;
        static final int TRANSACTION_setDimensionHints = 9;
        static final int TRANSACTION_setDisplayPadding = 13;
        static final int TRANSACTION_setInAmbientMode = 23;
        static final int TRANSACTION_setLockWallpaperCallback = 19;
        static final int TRANSACTION_setNextOffsets = 26;
        static final int TRANSACTION_setWallpaper = 1;
        static final int TRANSACTION_setWallpaperComponent = 3;
        static final int TRANSACTION_setWallpaperComponentChecked = 2;
        static final int TRANSACTION_settingsRestored = 15;
        static final int TRANSACTION_unregisterWallpaperColorsCallback = 22;

        private static class Proxy implements IWallpaperManager {
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

            public ParcelFileDescriptor setWallpaper(String name, String callingPackage, Rect cropHint, boolean allowBackup, Bundle extras, int which, IWallpaperManagerCallback completion, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    _data.writeString(callingPackage);
                    if (cropHint != null) {
                        _data.writeInt(1);
                        cropHint.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(allowBackup);
                    _data.writeInt(which);
                    ParcelFileDescriptor _result = null;
                    _data.writeStrongBinder(completion != null ? completion.asBinder() : null);
                    _data.writeInt(userId);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(_reply);
                    }
                    ParcelFileDescriptor _result2 = _result;
                    if (_reply.readInt() != 0) {
                        extras.readFromParcel(_reply);
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result2;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setWallpaperComponentChecked(ComponentName name, String callingPackage, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (name != null) {
                        _data.writeInt(1);
                        name.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callingPackage);
                    _data.writeInt(userId);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setWallpaperComponent(ComponentName name) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (name != null) {
                        _data.writeInt(1);
                        name.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public ParcelFileDescriptor getWallpaper(String callingPkg, IWallpaperManagerCallback cb, int which, Bundle outParams, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPkg);
                    ParcelFileDescriptor _result = null;
                    _data.writeStrongBinder(cb != null ? cb.asBinder() : null);
                    _data.writeInt(which);
                    _data.writeInt(userId);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(_reply);
                    }
                    if (_reply.readInt() != 0) {
                        outParams.readFromParcel(_reply);
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getWallpaperIdForUser(int which, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(which);
                    _data.writeInt(userId);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public WallpaperInfo getWallpaperInfo(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    WallpaperInfo _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (WallpaperInfo) WallpaperInfo.CREATOR.createFromParcel(_reply);
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

            public void clearWallpaper(String callingPackage, int which, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(which);
                    _data.writeInt(userId);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean hasNamedWallpaper(String name) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
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

            public void setDimensionHints(int width, int height, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(width);
                    _data.writeInt(height);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getWidthHint() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getHeightHint() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public ParcelFileDescriptor getBlurWallpaper(IWallpaperManagerCallback cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    ParcelFileDescriptor _result = null;
                    _data.writeStrongBinder(cb != null ? cb.asBinder() : null);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(_reply);
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setDisplayPadding(Rect padding, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (padding != null) {
                        _data.writeInt(1);
                        padding.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callingPackage);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getName() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void settingsRestored() throws RemoteException {
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

            public boolean isWallpaperSupported(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
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

            public boolean isSetWallpaperAllowed(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    boolean z = false;
                    this.mRemote.transact(17, _data, _reply, 0);
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

            public boolean isWallpaperBackupEligible(int which, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(which);
                    _data.writeInt(userId);
                    boolean z = false;
                    this.mRemote.transact(18, _data, _reply, 0);
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

            public boolean setLockWallpaperCallback(IWallpaperManagerCallback cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(cb != null ? cb.asBinder() : null);
                    boolean z = false;
                    this.mRemote.transact(19, _data, _reply, 0);
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

            public WallpaperColors getWallpaperColors(int which, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    WallpaperColors _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(which);
                    _data.writeInt(userId);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (WallpaperColors) WallpaperColors.CREATOR.createFromParcel(_reply);
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

            public void registerWallpaperColorsCallback(IWallpaperManagerCallback cb, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(cb != null ? cb.asBinder() : null);
                    _data.writeInt(userId);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void unregisterWallpaperColorsCallback(IWallpaperManagerCallback cb, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(cb != null ? cb.asBinder() : null);
                    _data.writeInt(userId);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setInAmbientMode(boolean inAmbientMode, boolean animated) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(inAmbientMode);
                    _data.writeInt(animated);
                    this.mRemote.transact(23, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public int[] getCurrOffsets() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setCurrOffsets(int[] offsets) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeIntArray(offsets);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setNextOffsets(int[] offsets) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeIntArray(offsets);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getWallpaperUserId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public Bitmap scaleWallpaperBitmapToScreenSize(Bitmap bitmap) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Bitmap _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (bitmap != null) {
                        _data.writeInt(1);
                        bitmap.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Bitmap) Bitmap.CREATOR.createFromParcel(_reply);
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

        public static IWallpaperManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IWallpaperManager)) {
                return new Proxy(obj);
            }
            return (IWallpaperManager) iin;
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
                Rect _arg0 = null;
                boolean z = false;
                ParcelFileDescriptor _result;
                ComponentName _arg02;
                boolean _result2;
                int _result3;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(descriptor);
                        String _arg03 = data.readString();
                        String _arg1 = data.readString();
                        if (data.readInt() != 0) {
                            _arg0 = (Rect) Rect.CREATOR.createFromParcel(parcel);
                        }
                        Rect _arg2 = _arg0;
                        boolean _arg3 = data.readInt() != 0;
                        Bundle _arg4 = new Bundle();
                        Bundle _arg42 = _arg4;
                        _result = setWallpaper(_arg03, _arg1, _arg2, _arg3, _arg4, data.readInt(), android.app.IWallpaperManagerCallback.Stub.asInterface(data.readStrongBinder()), data.readInt());
                        reply.writeNoException();
                        if (_result != null) {
                            parcel2.writeInt(1);
                            _result.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        parcel2.writeInt(1);
                        _arg42.writeToParcel(parcel2, 1);
                        return true;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        setWallpaperComponentChecked(_arg02, data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 3:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        setWallpaperComponent(_arg02);
                        reply.writeNoException();
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        String _arg04 = data.readString();
                        IWallpaperManagerCallback _arg12 = android.app.IWallpaperManagerCallback.Stub.asInterface(data.readStrongBinder());
                        int _arg22 = data.readInt();
                        Bundle _arg32 = new Bundle();
                        Bundle _arg33 = _arg32;
                        _result = getWallpaper(_arg04, _arg12, _arg22, _arg32, data.readInt());
                        reply.writeNoException();
                        if (_result != null) {
                            parcel2.writeInt(1);
                            _result.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        parcel2.writeInt(1);
                        _arg33.writeToParcel(parcel2, 1);
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        int _result4 = getWallpaperIdForUser(data.readInt(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        WallpaperInfo _result5 = getWallpaperInfo(data.readInt());
                        reply.writeNoException();
                        if (_result5 != null) {
                            parcel2.writeInt(1);
                            _result5.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        clearWallpaper(data.readString(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        _result2 = hasNamedWallpaper(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        setDimensionHints(data.readInt(), data.readInt(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 10:
                        parcel.enforceInterface(descriptor);
                        _result3 = getWidthHint();
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        _result3 = getHeightHint();
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 12:
                        parcel.enforceInterface(descriptor);
                        ParcelFileDescriptor _result6 = getBlurWallpaper(android.app.IWallpaperManagerCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        if (_result6 != null) {
                            parcel2.writeInt(1);
                            _result6.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 13:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (Rect) Rect.CREATOR.createFromParcel(parcel);
                        }
                        setDisplayPadding(_arg0, data.readString());
                        reply.writeNoException();
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        String _result7 = getName();
                        reply.writeNoException();
                        parcel2.writeString(_result7);
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        settingsRestored();
                        reply.writeNoException();
                        return true;
                    case 16:
                        parcel.enforceInterface(descriptor);
                        _result2 = isWallpaperSupported(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        _result2 = isSetWallpaperAllowed(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 18:
                        parcel.enforceInterface(descriptor);
                        boolean _result8 = isWallpaperBackupEligible(data.readInt(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result8);
                        return true;
                    case 19:
                        parcel.enforceInterface(descriptor);
                        _result2 = setLockWallpaperCallback(android.app.IWallpaperManagerCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 20:
                        parcel.enforceInterface(descriptor);
                        WallpaperColors _result9 = getWallpaperColors(data.readInt(), data.readInt());
                        reply.writeNoException();
                        if (_result9 != null) {
                            parcel2.writeInt(1);
                            _result9.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 21:
                        parcel.enforceInterface(descriptor);
                        registerWallpaperColorsCallback(android.app.IWallpaperManagerCallback.Stub.asInterface(data.readStrongBinder()), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 22:
                        parcel.enforceInterface(descriptor);
                        unregisterWallpaperColorsCallback(android.app.IWallpaperManagerCallback.Stub.asInterface(data.readStrongBinder()), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 23:
                        parcel.enforceInterface(descriptor);
                        boolean _arg05 = data.readInt() != 0;
                        if (data.readInt() != 0) {
                            z = true;
                        }
                        setInAmbientMode(_arg05, z);
                        return true;
                    case 24:
                        parcel.enforceInterface(descriptor);
                        int[] _result10 = getCurrOffsets();
                        reply.writeNoException();
                        parcel2.writeIntArray(_result10);
                        return true;
                    case 25:
                        parcel.enforceInterface(descriptor);
                        setCurrOffsets(data.createIntArray());
                        reply.writeNoException();
                        return true;
                    case 26:
                        parcel.enforceInterface(descriptor);
                        setNextOffsets(data.createIntArray());
                        reply.writeNoException();
                        return true;
                    case 27:
                        parcel.enforceInterface(descriptor);
                        _result3 = getWallpaperUserId();
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 28:
                        Bitmap _arg06;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg06 = (Bitmap) Bitmap.CREATOR.createFromParcel(parcel);
                        }
                        Bitmap _result11 = scaleWallpaperBitmapToScreenSize(_arg06);
                        reply.writeNoException();
                        if (_result11 != null) {
                            parcel2.writeInt(1);
                            _result11.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            parcel2.writeString(descriptor);
            return true;
        }
    }

    void clearWallpaper(String str, int i, int i2) throws RemoteException;

    ParcelFileDescriptor getBlurWallpaper(IWallpaperManagerCallback iWallpaperManagerCallback) throws RemoteException;

    int[] getCurrOffsets() throws RemoteException;

    int getHeightHint() throws RemoteException;

    String getName() throws RemoteException;

    ParcelFileDescriptor getWallpaper(String str, IWallpaperManagerCallback iWallpaperManagerCallback, int i, Bundle bundle, int i2) throws RemoteException;

    WallpaperColors getWallpaperColors(int i, int i2) throws RemoteException;

    int getWallpaperIdForUser(int i, int i2) throws RemoteException;

    WallpaperInfo getWallpaperInfo(int i) throws RemoteException;

    int getWallpaperUserId() throws RemoteException;

    int getWidthHint() throws RemoteException;

    boolean hasNamedWallpaper(String str) throws RemoteException;

    boolean isSetWallpaperAllowed(String str) throws RemoteException;

    boolean isWallpaperBackupEligible(int i, int i2) throws RemoteException;

    boolean isWallpaperSupported(String str) throws RemoteException;

    void registerWallpaperColorsCallback(IWallpaperManagerCallback iWallpaperManagerCallback, int i) throws RemoteException;

    Bitmap scaleWallpaperBitmapToScreenSize(Bitmap bitmap) throws RemoteException;

    void setCurrOffsets(int[] iArr) throws RemoteException;

    void setDimensionHints(int i, int i2, String str) throws RemoteException;

    void setDisplayPadding(Rect rect, String str) throws RemoteException;

    void setInAmbientMode(boolean z, boolean z2) throws RemoteException;

    boolean setLockWallpaperCallback(IWallpaperManagerCallback iWallpaperManagerCallback) throws RemoteException;

    void setNextOffsets(int[] iArr) throws RemoteException;

    ParcelFileDescriptor setWallpaper(String str, String str2, Rect rect, boolean z, Bundle bundle, int i, IWallpaperManagerCallback iWallpaperManagerCallback, int i2) throws RemoteException;

    void setWallpaperComponent(ComponentName componentName) throws RemoteException;

    void setWallpaperComponentChecked(ComponentName componentName, String str, int i) throws RemoteException;

    void settingsRestored() throws RemoteException;

    void unregisterWallpaperColorsCallback(IWallpaperManagerCallback iWallpaperManagerCallback, int i) throws RemoteException;
}
