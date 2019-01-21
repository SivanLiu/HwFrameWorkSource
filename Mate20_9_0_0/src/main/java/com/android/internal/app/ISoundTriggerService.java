package com.android.internal.app;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.hardware.soundtrigger.IRecognitionStatusCallback;
import android.hardware.soundtrigger.SoundTrigger.GenericSoundModel;
import android.hardware.soundtrigger.SoundTrigger.KeyphraseSoundModel;
import android.hardware.soundtrigger.SoundTrigger.RecognitionConfig;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelUuid;
import android.os.RemoteException;

public interface ISoundTriggerService extends IInterface {

    public static abstract class Stub extends Binder implements ISoundTriggerService {
        private static final String DESCRIPTOR = "com.android.internal.app.ISoundTriggerService";
        static final int TRANSACTION_deleteSoundModel = 3;
        static final int TRANSACTION_getSoundModel = 1;
        static final int TRANSACTION_isRecognitionActive = 12;
        static final int TRANSACTION_loadGenericSoundModel = 6;
        static final int TRANSACTION_loadKeyphraseSoundModel = 7;
        static final int TRANSACTION_startRecognition = 4;
        static final int TRANSACTION_startRecognitionForIntent = 8;
        static final int TRANSACTION_startRecognitionForService = 9;
        static final int TRANSACTION_stopRecognition = 5;
        static final int TRANSACTION_stopRecognitionForIntent = 10;
        static final int TRANSACTION_unloadSoundModel = 11;
        static final int TRANSACTION_updateSoundModel = 2;

        private static class Proxy implements ISoundTriggerService {
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

            public GenericSoundModel getSoundModel(ParcelUuid soundModelId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    GenericSoundModel _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (soundModelId != null) {
                        _data.writeInt(1);
                        soundModelId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (GenericSoundModel) GenericSoundModel.CREATOR.createFromParcel(_reply);
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

            public void updateSoundModel(GenericSoundModel soundModel) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (soundModel != null) {
                        _data.writeInt(1);
                        soundModel.writeToParcel(_data, 0);
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

            public void deleteSoundModel(ParcelUuid soundModelId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (soundModelId != null) {
                        _data.writeInt(1);
                        soundModelId.writeToParcel(_data, 0);
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

            public int startRecognition(ParcelUuid soundModelId, IRecognitionStatusCallback callback, RecognitionConfig config) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (soundModelId != null) {
                        _data.writeInt(1);
                        soundModelId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    if (config != null) {
                        _data.writeInt(1);
                        config.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int stopRecognition(ParcelUuid soundModelId, IRecognitionStatusCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (soundModelId != null) {
                        _data.writeInt(1);
                        soundModelId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int loadGenericSoundModel(GenericSoundModel soundModel) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (soundModel != null) {
                        _data.writeInt(1);
                        soundModel.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int loadKeyphraseSoundModel(KeyphraseSoundModel soundModel) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (soundModel != null) {
                        _data.writeInt(1);
                        soundModel.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int startRecognitionForIntent(ParcelUuid soundModelId, PendingIntent callbackIntent, RecognitionConfig config) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (soundModelId != null) {
                        _data.writeInt(1);
                        soundModelId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (callbackIntent != null) {
                        _data.writeInt(1);
                        callbackIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (config != null) {
                        _data.writeInt(1);
                        config.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int startRecognitionForService(ParcelUuid soundModelId, Bundle params, ComponentName callbackIntent, RecognitionConfig config) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (soundModelId != null) {
                        _data.writeInt(1);
                        soundModelId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (params != null) {
                        _data.writeInt(1);
                        params.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (callbackIntent != null) {
                        _data.writeInt(1);
                        callbackIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (config != null) {
                        _data.writeInt(1);
                        config.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int stopRecognitionForIntent(ParcelUuid soundModelId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (soundModelId != null) {
                        _data.writeInt(1);
                        soundModelId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int unloadSoundModel(ParcelUuid soundModelId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (soundModelId != null) {
                        _data.writeInt(1);
                        soundModelId.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isRecognitionActive(ParcelUuid parcelUuid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (parcelUuid != null) {
                        _data.writeInt(1);
                        parcelUuid.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(12, _data, _reply, 0);
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
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ISoundTriggerService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof ISoundTriggerService)) {
                return new Proxy(obj);
            }
            return (ISoundTriggerService) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            String descriptor = DESCRIPTOR;
            if (code != 1598968902) {
                ParcelUuid _arg0 = null;
                GenericSoundModel _arg02;
                ParcelUuid _arg03;
                RecognitionConfig _arg2;
                int _result;
                int _result2;
                switch (code) {
                    case 1:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ParcelUuid) ParcelUuid.CREATOR.createFromParcel(data);
                        }
                        GenericSoundModel _result3 = getSoundModel(_arg0);
                        reply.writeNoException();
                        if (_result3 != null) {
                            reply.writeInt(1);
                            _result3.writeToParcel(reply, 1);
                        } else {
                            reply.writeInt(0);
                        }
                        return true;
                    case 2:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (GenericSoundModel) GenericSoundModel.CREATOR.createFromParcel(data);
                        }
                        updateSoundModel(_arg02);
                        reply.writeNoException();
                        return true;
                    case 3:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ParcelUuid) ParcelUuid.CREATOR.createFromParcel(data);
                        }
                        deleteSoundModel(_arg0);
                        reply.writeNoException();
                        return true;
                    case 4:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (ParcelUuid) ParcelUuid.CREATOR.createFromParcel(data);
                        } else {
                            _arg03 = null;
                        }
                        IRecognitionStatusCallback _arg1 = android.hardware.soundtrigger.IRecognitionStatusCallback.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg2 = (RecognitionConfig) RecognitionConfig.CREATOR.createFromParcel(data);
                        }
                        _result = startRecognition(_arg03, _arg1, _arg2);
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 5:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ParcelUuid) ParcelUuid.CREATOR.createFromParcel(data);
                        }
                        int _result4 = stopRecognition(_arg0, android.hardware.soundtrigger.IRecognitionStatusCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        reply.writeInt(_result4);
                        return true;
                    case 6:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (GenericSoundModel) GenericSoundModel.CREATOR.createFromParcel(data);
                        }
                        _result2 = loadGenericSoundModel(_arg02);
                        reply.writeNoException();
                        reply.writeInt(_result2);
                        return true;
                    case 7:
                        KeyphraseSoundModel _arg04;
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg04 = (KeyphraseSoundModel) KeyphraseSoundModel.CREATOR.createFromParcel(data);
                        }
                        _result2 = loadKeyphraseSoundModel(_arg04);
                        reply.writeNoException();
                        reply.writeInt(_result2);
                        return true;
                    case 8:
                        PendingIntent _arg12;
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (ParcelUuid) ParcelUuid.CREATOR.createFromParcel(data);
                        } else {
                            _arg03 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg12 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(data);
                        } else {
                            _arg12 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg2 = (RecognitionConfig) RecognitionConfig.CREATOR.createFromParcel(data);
                        }
                        _result = startRecognitionForIntent(_arg03, _arg12, _arg2);
                        reply.writeNoException();
                        reply.writeInt(_result);
                        return true;
                    case 9:
                        Bundle _arg13;
                        ComponentName _arg22;
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (ParcelUuid) ParcelUuid.CREATOR.createFromParcel(data);
                        } else {
                            _arg03 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg13 = (Bundle) Bundle.CREATOR.createFromParcel(data);
                        } else {
                            _arg13 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg22 = (ComponentName) ComponentName.CREATOR.createFromParcel(data);
                        } else {
                            _arg22 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg2 = (RecognitionConfig) RecognitionConfig.CREATOR.createFromParcel(data);
                        }
                        int _result5 = startRecognitionForService(_arg03, _arg13, _arg22, _arg2);
                        reply.writeNoException();
                        reply.writeInt(_result5);
                        return true;
                    case 10:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ParcelUuid) ParcelUuid.CREATOR.createFromParcel(data);
                        }
                        _result2 = stopRecognitionForIntent(_arg0);
                        reply.writeNoException();
                        reply.writeInt(_result2);
                        return true;
                    case 11:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ParcelUuid) ParcelUuid.CREATOR.createFromParcel(data);
                        }
                        _result2 = unloadSoundModel(_arg0);
                        reply.writeNoException();
                        reply.writeInt(_result2);
                        return true;
                    case 12:
                        data.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ParcelUuid) ParcelUuid.CREATOR.createFromParcel(data);
                        }
                        boolean _result6 = isRecognitionActive(_arg0);
                        reply.writeNoException();
                        reply.writeInt(_result6);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            reply.writeString(descriptor);
            return true;
        }
    }

    void deleteSoundModel(ParcelUuid parcelUuid) throws RemoteException;

    GenericSoundModel getSoundModel(ParcelUuid parcelUuid) throws RemoteException;

    boolean isRecognitionActive(ParcelUuid parcelUuid) throws RemoteException;

    int loadGenericSoundModel(GenericSoundModel genericSoundModel) throws RemoteException;

    int loadKeyphraseSoundModel(KeyphraseSoundModel keyphraseSoundModel) throws RemoteException;

    int startRecognition(ParcelUuid parcelUuid, IRecognitionStatusCallback iRecognitionStatusCallback, RecognitionConfig recognitionConfig) throws RemoteException;

    int startRecognitionForIntent(ParcelUuid parcelUuid, PendingIntent pendingIntent, RecognitionConfig recognitionConfig) throws RemoteException;

    int startRecognitionForService(ParcelUuid parcelUuid, Bundle bundle, ComponentName componentName, RecognitionConfig recognitionConfig) throws RemoteException;

    int stopRecognition(ParcelUuid parcelUuid, IRecognitionStatusCallback iRecognitionStatusCallback) throws RemoteException;

    int stopRecognitionForIntent(ParcelUuid parcelUuid) throws RemoteException;

    int unloadSoundModel(ParcelUuid parcelUuid) throws RemoteException;

    void updateSoundModel(GenericSoundModel genericSoundModel) throws RemoteException;
}
