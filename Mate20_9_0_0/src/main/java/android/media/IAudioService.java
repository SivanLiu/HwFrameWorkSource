package android.media;

import android.bluetooth.BluetoothDevice;
import android.media.PlayerBase.PlayerIdCard;
import android.media.audiopolicy.AudioPolicyConfig;
import android.media.audiopolicy.IAudioPolicyCallback;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.util.List;

public interface IAudioService extends IInterface {

    public static abstract class Stub extends Binder implements IAudioService {
        private static final String DESCRIPTOR = "android.media.IAudioService";
        static final int TRANSACTION_abandonAudioFocus = 40;
        static final int TRANSACTION_addMixForPolicy = 65;
        static final int TRANSACTION_adjustStreamVolume = 6;
        static final int TRANSACTION_adjustSuggestedStreamVolume = 5;
        static final int TRANSACTION_avrcpSupportsAbsoluteVolume = 32;
        static final int TRANSACTION_disableRingtoneSync = 75;
        static final int TRANSACTION_disableSafeMediaVolume = 60;
        static final int TRANSACTION_dispatchFocusChange = 77;
        static final int TRANSACTION_forceRemoteSubmixFullVolume = 9;
        static final int TRANSACTION_forceVolumeControlStream = 46;
        static final int TRANSACTION_getActivePlaybackConfigurations = 74;
        static final int TRANSACTION_getActiveRecordingConfigurations = 71;
        static final int TRANSACTION_getCurrentAudioFocus = 42;
        static final int TRANSACTION_getFocusRampTimeMs = 76;
        static final int TRANSACTION_getHwInnerService = 84;
        static final int TRANSACTION_getLastAudibleStreamVolume = 15;
        static final int TRANSACTION_getMode = 26;
        static final int TRANSACTION_getRingerModeExternal = 19;
        static final int TRANSACTION_getRingerModeInternal = 20;
        static final int TRANSACTION_getRingtonePlayer = 48;
        static final int TRANSACTION_getStreamMaxVolume = 14;
        static final int TRANSACTION_getStreamMinVolume = 13;
        static final int TRANSACTION_getStreamVolume = 12;
        static final int TRANSACTION_getUiSoundsStreamType = 49;
        static final int TRANSACTION_getVibrateSetting = 23;
        static final int TRANSACTION_handleBluetoothA2dpDeviceConfigChange = 53;
        static final int TRANSACTION_isAudioServerRunning = 83;
        static final int TRANSACTION_isBluetoothA2dpOn = 38;
        static final int TRANSACTION_isBluetoothScoOn = 36;
        static final int TRANSACTION_isCameraSoundForced = 55;
        static final int TRANSACTION_isHdmiSystemAudioSupported = 62;
        static final int TRANSACTION_isMasterMute = 10;
        static final int TRANSACTION_isSpeakerphoneOn = 34;
        static final int TRANSACTION_isStreamAffectedByMute = 59;
        static final int TRANSACTION_isStreamAffectedByRingerMode = 58;
        static final int TRANSACTION_isStreamMute = 8;
        static final int TRANSACTION_isValidRingerMode = 21;
        static final int TRANSACTION_loadSoundEffects = 29;
        static final int TRANSACTION_notifyVolumeControllerVisible = 57;
        static final int TRANSACTION_playSoundEffect = 27;
        static final int TRANSACTION_playSoundEffectVolume = 28;
        static final int TRANSACTION_playerAttributes = 2;
        static final int TRANSACTION_playerEvent = 3;
        static final int TRANSACTION_playerHasOpPlayAudio = 78;
        static final int TRANSACTION_registerAudioPolicy = 63;
        static final int TRANSACTION_registerAudioServerStateDispatcher = 81;
        static final int TRANSACTION_registerPlaybackCallback = 72;
        static final int TRANSACTION_registerRecordingCallback = 69;
        static final int TRANSACTION_releasePlayer = 4;
        static final int TRANSACTION_reloadAudioSettings = 31;
        static final int TRANSACTION_removeMixForPolicy = 66;
        static final int TRANSACTION_requestAudioFocus = 39;
        static final int TRANSACTION_setBluetoothA2dpDeviceConnectionState = 52;
        static final int TRANSACTION_setBluetoothA2dpDeviceConnectionStateSuppressNoisyIntent = 79;
        static final int TRANSACTION_setBluetoothA2dpOn = 37;
        static final int TRANSACTION_setBluetoothScoOn = 35;
        static final int TRANSACTION_setFocusPropertiesForPolicy = 67;
        static final int TRANSACTION_setFocusRequestResultFromExtPolicy = 80;
        static final int TRANSACTION_setHdmiSystemAudioSupported = 61;
        static final int TRANSACTION_setHearingAidDeviceConnectionState = 51;
        static final int TRANSACTION_setMasterMute = 11;
        static final int TRANSACTION_setMicrophoneMute = 16;
        static final int TRANSACTION_setMode = 25;
        static final int TRANSACTION_setRingerModeExternal = 17;
        static final int TRANSACTION_setRingerModeInternal = 18;
        static final int TRANSACTION_setRingtonePlayer = 47;
        static final int TRANSACTION_setSpeakerphoneOn = 33;
        static final int TRANSACTION_setStreamVolume = 7;
        static final int TRANSACTION_setVibrateSetting = 22;
        static final int TRANSACTION_setVolumeController = 56;
        static final int TRANSACTION_setVolumePolicy = 68;
        static final int TRANSACTION_setWiredDeviceConnectionState = 50;
        static final int TRANSACTION_shouldVibrate = 24;
        static final int TRANSACTION_startBluetoothSco = 43;
        static final int TRANSACTION_startBluetoothScoVirtualCall = 44;
        static final int TRANSACTION_startWatchingRoutes = 54;
        static final int TRANSACTION_stopBluetoothSco = 45;
        static final int TRANSACTION_trackPlayer = 1;
        static final int TRANSACTION_unloadSoundEffects = 30;
        static final int TRANSACTION_unregisterAudioFocusClient = 41;
        static final int TRANSACTION_unregisterAudioPolicyAsync = 64;
        static final int TRANSACTION_unregisterAudioServerStateDispatcher = 82;
        static final int TRANSACTION_unregisterPlaybackCallback = 73;
        static final int TRANSACTION_unregisterRecordingCallback = 70;

        private static class Proxy implements IAudioService {
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

            public int trackPlayer(PlayerIdCard pic) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (pic != null) {
                        _data.writeInt(1);
                        pic.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void playerAttributes(int piid, AudioAttributes attr) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(piid);
                    if (attr != null) {
                        _data.writeInt(1);
                        attr.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void playerEvent(int piid, int event) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(piid);
                    _data.writeInt(event);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void releasePlayer(int piid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(piid);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void adjustSuggestedStreamVolume(int direction, int suggestedStreamType, int flags, String callingPackage, String caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(direction);
                    _data.writeInt(suggestedStreamType);
                    _data.writeInt(flags);
                    _data.writeString(callingPackage);
                    _data.writeString(caller);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void adjustStreamVolume(int streamType, int direction, int flags, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(streamType);
                    _data.writeInt(direction);
                    _data.writeInt(flags);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setStreamVolume(int streamType, int index, int flags, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(streamType);
                    _data.writeInt(index);
                    _data.writeInt(flags);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isStreamMute(int streamType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(streamType);
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

            public void forceRemoteSubmixFullVolume(boolean startForcing, IBinder cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(startForcing);
                    _data.writeStrongBinder(cb);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isMasterMute() throws RemoteException {
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

            public void setMasterMute(boolean mute, int flags, String callingPackage, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(mute);
                    _data.writeInt(flags);
                    _data.writeString(callingPackage);
                    _data.writeInt(userId);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getStreamVolume(int streamType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(streamType);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getStreamMinVolume(int streamType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(streamType);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getStreamMaxVolume(int streamType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(streamType);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getLastAudibleStreamVolume(int streamType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(streamType);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setMicrophoneMute(boolean on, String callingPackage, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(on);
                    _data.writeString(callingPackage);
                    _data.writeInt(userId);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setRingerModeExternal(int ringerMode, String caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(ringerMode);
                    _data.writeString(caller);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setRingerModeInternal(int ringerMode, String caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(ringerMode);
                    _data.writeString(caller);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getRingerModeExternal() throws RemoteException {
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

            public int getRingerModeInternal() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isValidRingerMode(int ringerMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(ringerMode);
                    boolean z = false;
                    this.mRemote.transact(21, _data, _reply, 0);
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

            public void setVibrateSetting(int vibrateType, int vibrateSetting) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(vibrateType);
                    _data.writeInt(vibrateSetting);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getVibrateSetting(int vibrateType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(vibrateType);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean shouldVibrate(int vibrateType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(vibrateType);
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

            public void setMode(int mode, IBinder cb, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(mode);
                    _data.writeStrongBinder(cb);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void playSoundEffect(int effectType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(effectType);
                    this.mRemote.transact(27, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void playSoundEffectVolume(int effectType, float volume) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(effectType);
                    _data.writeFloat(volume);
                    this.mRemote.transact(28, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public boolean loadSoundEffects() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(29, _data, _reply, 0);
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

            public void unloadSoundEffects() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(30, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void reloadAudioSettings() throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(31, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void avrcpSupportsAbsoluteVolume(String address, boolean support) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(address);
                    _data.writeInt(support);
                    this.mRemote.transact(32, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void setSpeakerphoneOn(boolean on) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(on);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isSpeakerphoneOn() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(34, _data, _reply, 0);
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

            public void setBluetoothScoOn(boolean on) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(on);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isBluetoothScoOn() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(36, _data, _reply, 0);
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

            public void setBluetoothA2dpOn(boolean on) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(on);
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isBluetoothA2dpOn() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(38, _data, _reply, 0);
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

            public int requestAudioFocus(AudioAttributes aa, int durationHint, IBinder cb, IAudioFocusDispatcher fd, String clientId, String callingPackageName, int flags, IAudioPolicyCallback pcb, int sdk) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (aa != null) {
                        _data.writeInt(1);
                        aa.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(durationHint);
                    _data.writeStrongBinder(cb);
                    IBinder iBinder = null;
                    _data.writeStrongBinder(fd != null ? fd.asBinder() : null);
                    _data.writeString(clientId);
                    _data.writeString(callingPackageName);
                    _data.writeInt(flags);
                    if (pcb != null) {
                        iBinder = pcb.asBinder();
                    }
                    _data.writeStrongBinder(iBinder);
                    _data.writeInt(sdk);
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int abandonAudioFocus(IAudioFocusDispatcher fd, String clientId, AudioAttributes aa, String callingPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(fd != null ? fd.asBinder() : null);
                    _data.writeString(clientId);
                    if (aa != null) {
                        _data.writeInt(1);
                        aa.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callingPackageName);
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void unregisterAudioFocusClient(String clientId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(clientId);
                    this.mRemote.transact(41, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getCurrentAudioFocus() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(42, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void startBluetoothSco(IBinder cb, int targetSdkVersion) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(cb);
                    _data.writeInt(targetSdkVersion);
                    this.mRemote.transact(43, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void startBluetoothScoVirtualCall(IBinder cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(cb);
                    this.mRemote.transact(44, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void stopBluetoothSco(IBinder cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(cb);
                    this.mRemote.transact(45, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void forceVolumeControlStream(int streamType, IBinder cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(streamType);
                    _data.writeStrongBinder(cb);
                    this.mRemote.transact(46, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setRingtonePlayer(IRingtonePlayer player) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(player != null ? player.asBinder() : null);
                    this.mRemote.transact(47, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IRingtonePlayer getRingtonePlayer() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(48, _data, _reply, 0);
                    _reply.readException();
                    IRingtonePlayer _result = android.media.IRingtonePlayer.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getUiSoundsStreamType() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(49, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setWiredDeviceConnectionState(int type, int state, String address, String name, String caller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(type);
                    _data.writeInt(state);
                    _data.writeString(address);
                    _data.writeString(name);
                    _data.writeString(caller);
                    this.mRemote.transact(50, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setHearingAidDeviceConnectionState(BluetoothDevice device, int state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (device != null) {
                        _data.writeInt(1);
                        device.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(state);
                    this.mRemote.transact(51, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int setBluetoothA2dpDeviceConnectionState(BluetoothDevice device, int state, int profile) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (device != null) {
                        _data.writeInt(1);
                        device.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(state);
                    _data.writeInt(profile);
                    this.mRemote.transact(52, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void handleBluetoothA2dpDeviceConfigChange(BluetoothDevice device) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (device != null) {
                        _data.writeInt(1);
                        device.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(53, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public AudioRoutesInfo startWatchingRoutes(IAudioRoutesObserver observer) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    AudioRoutesInfo _result = null;
                    _data.writeStrongBinder(observer != null ? observer.asBinder() : null);
                    this.mRemote.transact(54, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (AudioRoutesInfo) AudioRoutesInfo.CREATOR.createFromParcel(_reply);
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isCameraSoundForced() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(55, _data, _reply, 0);
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

            public void setVolumeController(IVolumeController controller) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(controller != null ? controller.asBinder() : null);
                    this.mRemote.transact(56, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void notifyVolumeControllerVisible(IVolumeController controller, boolean visible) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(controller != null ? controller.asBinder() : null);
                    _data.writeInt(visible);
                    this.mRemote.transact(57, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isStreamAffectedByRingerMode(int streamType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(streamType);
                    boolean z = false;
                    this.mRemote.transact(58, _data, _reply, 0);
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

            public boolean isStreamAffectedByMute(int streamType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(streamType);
                    boolean z = false;
                    this.mRemote.transact(59, _data, _reply, 0);
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

            public void disableSafeMediaVolume(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(60, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int setHdmiSystemAudioSupported(boolean on) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(on);
                    this.mRemote.transact(61, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isHdmiSystemAudioSupported() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(62, _data, _reply, 0);
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

            public String registerAudioPolicy(AudioPolicyConfig policyConfig, IAudioPolicyCallback pcb, boolean hasFocusListener, boolean isFocusPolicy, boolean isVolumeController) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (policyConfig != null) {
                        _data.writeInt(1);
                        policyConfig.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(pcb != null ? pcb.asBinder() : null);
                    _data.writeInt(hasFocusListener);
                    _data.writeInt(isFocusPolicy);
                    _data.writeInt(isVolumeController);
                    this.mRemote.transact(63, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void unregisterAudioPolicyAsync(IAudioPolicyCallback pcb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(pcb != null ? pcb.asBinder() : null);
                    this.mRemote.transact(64, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public int addMixForPolicy(AudioPolicyConfig policyConfig, IAudioPolicyCallback pcb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (policyConfig != null) {
                        _data.writeInt(1);
                        policyConfig.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(pcb != null ? pcb.asBinder() : null);
                    this.mRemote.transact(65, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int removeMixForPolicy(AudioPolicyConfig policyConfig, IAudioPolicyCallback pcb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (policyConfig != null) {
                        _data.writeInt(1);
                        policyConfig.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(pcb != null ? pcb.asBinder() : null);
                    this.mRemote.transact(66, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int setFocusPropertiesForPolicy(int duckingBehavior, IAudioPolicyCallback pcb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(duckingBehavior);
                    _data.writeStrongBinder(pcb != null ? pcb.asBinder() : null);
                    this.mRemote.transact(67, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setVolumePolicy(VolumePolicy policy) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (policy != null) {
                        _data.writeInt(1);
                        policy.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(68, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void registerRecordingCallback(IRecordingConfigDispatcher rcdb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(rcdb != null ? rcdb.asBinder() : null);
                    this.mRemote.transact(69, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void unregisterRecordingCallback(IRecordingConfigDispatcher rcdb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(rcdb != null ? rcdb.asBinder() : null);
                    this.mRemote.transact(70, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public List<AudioRecordingConfiguration> getActiveRecordingConfigurations() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(71, _data, _reply, 0);
                    _reply.readException();
                    List<AudioRecordingConfiguration> _result = _reply.createTypedArrayList(AudioRecordingConfiguration.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void registerPlaybackCallback(IPlaybackConfigDispatcher pcdb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(pcdb != null ? pcdb.asBinder() : null);
                    this.mRemote.transact(72, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void unregisterPlaybackCallback(IPlaybackConfigDispatcher pcdb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(pcdb != null ? pcdb.asBinder() : null);
                    this.mRemote.transact(73, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public List<AudioPlaybackConfiguration> getActivePlaybackConfigurations() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(74, _data, _reply, 0);
                    _reply.readException();
                    List<AudioPlaybackConfiguration> _result = _reply.createTypedArrayList(AudioPlaybackConfiguration.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void disableRingtoneSync(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(75, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getFocusRampTimeMs(int focusGain, AudioAttributes attr) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(focusGain);
                    if (attr != null) {
                        _data.writeInt(1);
                        attr.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(76, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int dispatchFocusChange(AudioFocusInfo afi, int focusChange, IAudioPolicyCallback pcb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (afi != null) {
                        _data.writeInt(1);
                        afi.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(focusChange);
                    _data.writeStrongBinder(pcb != null ? pcb.asBinder() : null);
                    this.mRemote.transact(77, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void playerHasOpPlayAudio(int piid, boolean hasOpPlayAudio) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(piid);
                    _data.writeInt(hasOpPlayAudio);
                    this.mRemote.transact(78, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public int setBluetoothA2dpDeviceConnectionStateSuppressNoisyIntent(BluetoothDevice device, int state, int profile, boolean suppressNoisyIntent, int a2dpVolume) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (device != null) {
                        _data.writeInt(1);
                        device.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(state);
                    _data.writeInt(profile);
                    _data.writeInt(suppressNoisyIntent);
                    _data.writeInt(a2dpVolume);
                    this.mRemote.transact(79, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setFocusRequestResultFromExtPolicy(AudioFocusInfo afi, int requestResult, IAudioPolicyCallback pcb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (afi != null) {
                        _data.writeInt(1);
                        afi.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(requestResult);
                    _data.writeStrongBinder(pcb != null ? pcb.asBinder() : null);
                    this.mRemote.transact(80, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void registerAudioServerStateDispatcher(IAudioServerStateDispatcher asd) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(asd != null ? asd.asBinder() : null);
                    this.mRemote.transact(81, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void unregisterAudioServerStateDispatcher(IAudioServerStateDispatcher asd) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(asd != null ? asd.asBinder() : null);
                    this.mRemote.transact(82, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public boolean isAudioServerRunning() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(83, _data, _reply, 0);
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

            public IBinder getHwInnerService() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(84, _data, _reply, 0);
                    _reply.readException();
                    IBinder _result = _reply.readStrongBinder();
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

        public static IAudioService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IAudioService)) {
                return new Proxy(obj);
            }
            return (IAudioService) iin;
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
                boolean _arg0 = false;
                PlayerIdCard _arg1 = null;
                int _result;
                int _arg02;
                AudioAttributes _arg12;
                boolean _result2;
                BluetoothDevice _arg03;
                int _result3;
                AudioPolicyConfig _arg04;
                int _result4;
                AudioFocusInfo _arg05;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = (PlayerIdCard) PlayerIdCard.CREATOR.createFromParcel(parcel);
                        }
                        _result = trackPlayer(_arg1);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg12 = (AudioAttributes) AudioAttributes.CREATOR.createFromParcel(parcel);
                        }
                        playerAttributes(_arg02, _arg12);
                        return true;
                    case 3:
                        parcel.enforceInterface(descriptor);
                        playerEvent(data.readInt(), data.readInt());
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        releasePlayer(data.readInt());
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        adjustSuggestedStreamVolume(data.readInt(), data.readInt(), data.readInt(), data.readString(), data.readString());
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        adjustStreamVolume(data.readInt(), data.readInt(), data.readInt(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        setStreamVolume(data.readInt(), data.readInt(), data.readInt(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        _result2 = isStreamMute(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = true;
                        }
                        forceRemoteSubmixFullVolume(_arg0, data.readStrongBinder());
                        reply.writeNoException();
                        return true;
                    case 10:
                        parcel.enforceInterface(descriptor);
                        _arg0 = isMasterMute();
                        reply.writeNoException();
                        parcel2.writeInt(_arg0);
                        return true;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = true;
                        }
                        setMasterMute(_arg0, data.readInt(), data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 12:
                        parcel.enforceInterface(descriptor);
                        _result = getStreamVolume(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 13:
                        parcel.enforceInterface(descriptor);
                        _result = getStreamMinVolume(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        _result = getStreamMaxVolume(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        _result = getLastAudibleStreamVolume(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 16:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = true;
                        }
                        setMicrophoneMute(_arg0, data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        setRingerModeExternal(data.readInt(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 18:
                        parcel.enforceInterface(descriptor);
                        setRingerModeInternal(data.readInt(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 19:
                        parcel.enforceInterface(descriptor);
                        _arg02 = getRingerModeExternal();
                        reply.writeNoException();
                        parcel2.writeInt(_arg02);
                        return true;
                    case 20:
                        parcel.enforceInterface(descriptor);
                        _arg02 = getRingerModeInternal();
                        reply.writeNoException();
                        parcel2.writeInt(_arg02);
                        return true;
                    case 21:
                        parcel.enforceInterface(descriptor);
                        _result2 = isValidRingerMode(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 22:
                        parcel.enforceInterface(descriptor);
                        setVibrateSetting(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 23:
                        parcel.enforceInterface(descriptor);
                        _result = getVibrateSetting(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 24:
                        parcel.enforceInterface(descriptor);
                        _result2 = shouldVibrate(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 25:
                        parcel.enforceInterface(descriptor);
                        setMode(data.readInt(), data.readStrongBinder(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 26:
                        parcel.enforceInterface(descriptor);
                        _arg02 = getMode();
                        reply.writeNoException();
                        parcel2.writeInt(_arg02);
                        return true;
                    case 27:
                        parcel.enforceInterface(descriptor);
                        playSoundEffect(data.readInt());
                        return true;
                    case 28:
                        parcel.enforceInterface(descriptor);
                        playSoundEffectVolume(data.readInt(), data.readFloat());
                        return true;
                    case 29:
                        parcel.enforceInterface(descriptor);
                        _arg0 = loadSoundEffects();
                        reply.writeNoException();
                        parcel2.writeInt(_arg0);
                        return true;
                    case 30:
                        parcel.enforceInterface(descriptor);
                        unloadSoundEffects();
                        return true;
                    case 31:
                        parcel.enforceInterface(descriptor);
                        reloadAudioSettings();
                        return true;
                    case 32:
                        parcel.enforceInterface(descriptor);
                        String _arg06 = data.readString();
                        if (data.readInt() != 0) {
                            _arg0 = true;
                        }
                        avrcpSupportsAbsoluteVolume(_arg06, _arg0);
                        return true;
                    case 33:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = true;
                        }
                        setSpeakerphoneOn(_arg0);
                        reply.writeNoException();
                        return true;
                    case 34:
                        parcel.enforceInterface(descriptor);
                        _arg0 = isSpeakerphoneOn();
                        reply.writeNoException();
                        parcel2.writeInt(_arg0);
                        return true;
                    case 35:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = true;
                        }
                        setBluetoothScoOn(_arg0);
                        reply.writeNoException();
                        return true;
                    case 36:
                        parcel.enforceInterface(descriptor);
                        _arg0 = isBluetoothScoOn();
                        reply.writeNoException();
                        parcel2.writeInt(_arg0);
                        return true;
                    case 37:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = true;
                        }
                        setBluetoothA2dpOn(_arg0);
                        reply.writeNoException();
                        return true;
                    case 38:
                        parcel.enforceInterface(descriptor);
                        _arg0 = isBluetoothA2dpOn();
                        reply.writeNoException();
                        parcel2.writeInt(_arg0);
                        return true;
                    case 39:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg12 = (AudioAttributes) AudioAttributes.CREATOR.createFromParcel(parcel);
                        }
                        _arg02 = requestAudioFocus(_arg12, data.readInt(), data.readStrongBinder(), android.media.IAudioFocusDispatcher.Stub.asInterface(data.readStrongBinder()), data.readString(), data.readString(), data.readInt(), android.media.audiopolicy.IAudioPolicyCallback.Stub.asInterface(data.readStrongBinder()), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg02);
                        return true;
                    case 40:
                        parcel.enforceInterface(descriptor);
                        IAudioFocusDispatcher _arg07 = android.media.IAudioFocusDispatcher.Stub.asInterface(data.readStrongBinder());
                        String _arg13 = data.readString();
                        if (data.readInt() != 0) {
                            _arg12 = (AudioAttributes) AudioAttributes.CREATOR.createFromParcel(parcel);
                        }
                        int _result5 = abandonAudioFocus(_arg07, _arg13, _arg12, data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case 41:
                        parcel.enforceInterface(descriptor);
                        unregisterAudioFocusClient(data.readString());
                        reply.writeNoException();
                        return true;
                    case 42:
                        parcel.enforceInterface(descriptor);
                        _arg02 = getCurrentAudioFocus();
                        reply.writeNoException();
                        parcel2.writeInt(_arg02);
                        return true;
                    case 43:
                        parcel.enforceInterface(descriptor);
                        startBluetoothSco(data.readStrongBinder(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 44:
                        parcel.enforceInterface(descriptor);
                        startBluetoothScoVirtualCall(data.readStrongBinder());
                        reply.writeNoException();
                        return true;
                    case 45:
                        parcel.enforceInterface(descriptor);
                        stopBluetoothSco(data.readStrongBinder());
                        reply.writeNoException();
                        return true;
                    case 46:
                        parcel.enforceInterface(descriptor);
                        forceVolumeControlStream(data.readInt(), data.readStrongBinder());
                        reply.writeNoException();
                        return true;
                    case 47:
                        parcel.enforceInterface(descriptor);
                        setRingtonePlayer(android.media.IRingtonePlayer.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 48:
                        IBinder _arg08;
                        parcel.enforceInterface(descriptor);
                        IRingtonePlayer _result6 = getRingtonePlayer();
                        reply.writeNoException();
                        if (_result6 != null) {
                            _arg08 = _result6.asBinder();
                        }
                        parcel2.writeStrongBinder(_arg08);
                        return true;
                    case 49:
                        parcel.enforceInterface(descriptor);
                        _arg02 = getUiSoundsStreamType();
                        reply.writeNoException();
                        parcel2.writeInt(_arg02);
                        return true;
                    case 50:
                        parcel.enforceInterface(descriptor);
                        setWiredDeviceConnectionState(data.readInt(), data.readInt(), data.readString(), data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 51:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (BluetoothDevice) BluetoothDevice.CREATOR.createFromParcel(parcel);
                        }
                        setHearingAidDeviceConnectionState(_arg03, data.readInt());
                        reply.writeNoException();
                        return true;
                    case 52:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (BluetoothDevice) BluetoothDevice.CREATOR.createFromParcel(parcel);
                        }
                        _result3 = setBluetoothA2dpDeviceConnectionState(_arg03, data.readInt(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 53:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (BluetoothDevice) BluetoothDevice.CREATOR.createFromParcel(parcel);
                        }
                        handleBluetoothA2dpDeviceConfigChange(_arg03);
                        reply.writeNoException();
                        return true;
                    case 54:
                        parcel.enforceInterface(descriptor);
                        AudioRoutesInfo _result7 = startWatchingRoutes(android.media.IAudioRoutesObserver.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        if (_result7 != null) {
                            parcel2.writeInt(1);
                            _result7.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 55:
                        parcel.enforceInterface(descriptor);
                        _arg0 = isCameraSoundForced();
                        reply.writeNoException();
                        parcel2.writeInt(_arg0);
                        return true;
                    case 56:
                        parcel.enforceInterface(descriptor);
                        setVolumeController(android.media.IVolumeController.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 57:
                        parcel.enforceInterface(descriptor);
                        IVolumeController _arg09 = android.media.IVolumeController.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg0 = true;
                        }
                        notifyVolumeControllerVisible(_arg09, _arg0);
                        reply.writeNoException();
                        return true;
                    case 58:
                        parcel.enforceInterface(descriptor);
                        _result2 = isStreamAffectedByRingerMode(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 59:
                        parcel.enforceInterface(descriptor);
                        _result2 = isStreamAffectedByMute(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 60:
                        parcel.enforceInterface(descriptor);
                        disableSafeMediaVolume(data.readString());
                        reply.writeNoException();
                        return true;
                    case 61:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = true;
                        }
                        _result = setHdmiSystemAudioSupported(_arg0);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 62:
                        parcel.enforceInterface(descriptor);
                        _arg0 = isHdmiSystemAudioSupported();
                        reply.writeNoException();
                        parcel2.writeInt(_arg0);
                        return true;
                    case 63:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg04 = (AudioPolicyConfig) AudioPolicyConfig.CREATOR.createFromParcel(parcel);
                        }
                        String _result8 = registerAudioPolicy(_arg04, android.media.audiopolicy.IAudioPolicyCallback.Stub.asInterface(data.readStrongBinder()), data.readInt() != 0, data.readInt() != 0, data.readInt() != 0);
                        reply.writeNoException();
                        parcel2.writeString(_result8);
                        return true;
                    case 64:
                        parcel.enforceInterface(descriptor);
                        unregisterAudioPolicyAsync(android.media.audiopolicy.IAudioPolicyCallback.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 65:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg04 = (AudioPolicyConfig) AudioPolicyConfig.CREATOR.createFromParcel(parcel);
                        }
                        _result4 = addMixForPolicy(_arg04, android.media.audiopolicy.IAudioPolicyCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 66:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg04 = (AudioPolicyConfig) AudioPolicyConfig.CREATOR.createFromParcel(parcel);
                        }
                        _result4 = removeMixForPolicy(_arg04, android.media.audiopolicy.IAudioPolicyCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 67:
                        parcel.enforceInterface(descriptor);
                        _result4 = setFocusPropertiesForPolicy(data.readInt(), android.media.audiopolicy.IAudioPolicyCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 68:
                        VolumePolicy _arg010;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg010 = (VolumePolicy) VolumePolicy.CREATOR.createFromParcel(parcel);
                        }
                        setVolumePolicy(_arg010);
                        reply.writeNoException();
                        return true;
                    case 69:
                        parcel.enforceInterface(descriptor);
                        registerRecordingCallback(android.media.IRecordingConfigDispatcher.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 70:
                        parcel.enforceInterface(descriptor);
                        unregisterRecordingCallback(android.media.IRecordingConfigDispatcher.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 71:
                        parcel.enforceInterface(descriptor);
                        List<AudioRecordingConfiguration> _result9 = getActiveRecordingConfigurations();
                        reply.writeNoException();
                        parcel2.writeTypedList(_result9);
                        return true;
                    case 72:
                        parcel.enforceInterface(descriptor);
                        registerPlaybackCallback(android.media.IPlaybackConfigDispatcher.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 73:
                        parcel.enforceInterface(descriptor);
                        unregisterPlaybackCallback(android.media.IPlaybackConfigDispatcher.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 74:
                        parcel.enforceInterface(descriptor);
                        List<AudioPlaybackConfiguration> _result10 = getActivePlaybackConfigurations();
                        reply.writeNoException();
                        parcel2.writeTypedList(_result10);
                        return true;
                    case 75:
                        parcel.enforceInterface(descriptor);
                        disableRingtoneSync(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 76:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg12 = (AudioAttributes) AudioAttributes.CREATOR.createFromParcel(parcel);
                        }
                        _result4 = getFocusRampTimeMs(_arg02, _arg12);
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 77:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg05 = (AudioFocusInfo) AudioFocusInfo.CREATOR.createFromParcel(parcel);
                        }
                        _result3 = dispatchFocusChange(_arg05, data.readInt(), android.media.audiopolicy.IAudioPolicyCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 78:
                        parcel.enforceInterface(descriptor);
                        _result = data.readInt();
                        if (data.readInt() != 0) {
                            _arg0 = true;
                        }
                        playerHasOpPlayAudio(_result, _arg0);
                        return true;
                    case 79:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (BluetoothDevice) BluetoothDevice.CREATOR.createFromParcel(parcel);
                        }
                        _arg02 = setBluetoothA2dpDeviceConnectionStateSuppressNoisyIntent(_arg03, data.readInt(), data.readInt(), data.readInt() != 0, data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg02);
                        return true;
                    case 80:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg05 = (AudioFocusInfo) AudioFocusInfo.CREATOR.createFromParcel(parcel);
                        }
                        setFocusRequestResultFromExtPolicy(_arg05, data.readInt(), android.media.audiopolicy.IAudioPolicyCallback.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 81:
                        parcel.enforceInterface(descriptor);
                        registerAudioServerStateDispatcher(android.media.IAudioServerStateDispatcher.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 82:
                        parcel.enforceInterface(descriptor);
                        unregisterAudioServerStateDispatcher(android.media.IAudioServerStateDispatcher.Stub.asInterface(data.readStrongBinder()));
                        return true;
                    case 83:
                        parcel.enforceInterface(descriptor);
                        _arg0 = isAudioServerRunning();
                        reply.writeNoException();
                        parcel2.writeInt(_arg0);
                        return true;
                    case 84:
                        parcel.enforceInterface(descriptor);
                        IBinder _result11 = getHwInnerService();
                        reply.writeNoException();
                        parcel2.writeStrongBinder(_result11);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            parcel2.writeString(descriptor);
            return true;
        }
    }

    int abandonAudioFocus(IAudioFocusDispatcher iAudioFocusDispatcher, String str, AudioAttributes audioAttributes, String str2) throws RemoteException;

    int addMixForPolicy(AudioPolicyConfig audioPolicyConfig, IAudioPolicyCallback iAudioPolicyCallback) throws RemoteException;

    void adjustStreamVolume(int i, int i2, int i3, String str) throws RemoteException;

    void adjustSuggestedStreamVolume(int i, int i2, int i3, String str, String str2) throws RemoteException;

    void avrcpSupportsAbsoluteVolume(String str, boolean z) throws RemoteException;

    void disableRingtoneSync(int i) throws RemoteException;

    void disableSafeMediaVolume(String str) throws RemoteException;

    int dispatchFocusChange(AudioFocusInfo audioFocusInfo, int i, IAudioPolicyCallback iAudioPolicyCallback) throws RemoteException;

    void forceRemoteSubmixFullVolume(boolean z, IBinder iBinder) throws RemoteException;

    void forceVolumeControlStream(int i, IBinder iBinder) throws RemoteException;

    List<AudioPlaybackConfiguration> getActivePlaybackConfigurations() throws RemoteException;

    List<AudioRecordingConfiguration> getActiveRecordingConfigurations() throws RemoteException;

    int getCurrentAudioFocus() throws RemoteException;

    int getFocusRampTimeMs(int i, AudioAttributes audioAttributes) throws RemoteException;

    IBinder getHwInnerService() throws RemoteException;

    int getLastAudibleStreamVolume(int i) throws RemoteException;

    int getMode() throws RemoteException;

    int getRingerModeExternal() throws RemoteException;

    int getRingerModeInternal() throws RemoteException;

    IRingtonePlayer getRingtonePlayer() throws RemoteException;

    int getStreamMaxVolume(int i) throws RemoteException;

    int getStreamMinVolume(int i) throws RemoteException;

    int getStreamVolume(int i) throws RemoteException;

    int getUiSoundsStreamType() throws RemoteException;

    int getVibrateSetting(int i) throws RemoteException;

    void handleBluetoothA2dpDeviceConfigChange(BluetoothDevice bluetoothDevice) throws RemoteException;

    boolean isAudioServerRunning() throws RemoteException;

    boolean isBluetoothA2dpOn() throws RemoteException;

    boolean isBluetoothScoOn() throws RemoteException;

    boolean isCameraSoundForced() throws RemoteException;

    boolean isHdmiSystemAudioSupported() throws RemoteException;

    boolean isMasterMute() throws RemoteException;

    boolean isSpeakerphoneOn() throws RemoteException;

    boolean isStreamAffectedByMute(int i) throws RemoteException;

    boolean isStreamAffectedByRingerMode(int i) throws RemoteException;

    boolean isStreamMute(int i) throws RemoteException;

    boolean isValidRingerMode(int i) throws RemoteException;

    boolean loadSoundEffects() throws RemoteException;

    void notifyVolumeControllerVisible(IVolumeController iVolumeController, boolean z) throws RemoteException;

    void playSoundEffect(int i) throws RemoteException;

    void playSoundEffectVolume(int i, float f) throws RemoteException;

    void playerAttributes(int i, AudioAttributes audioAttributes) throws RemoteException;

    void playerEvent(int i, int i2) throws RemoteException;

    void playerHasOpPlayAudio(int i, boolean z) throws RemoteException;

    String registerAudioPolicy(AudioPolicyConfig audioPolicyConfig, IAudioPolicyCallback iAudioPolicyCallback, boolean z, boolean z2, boolean z3) throws RemoteException;

    void registerAudioServerStateDispatcher(IAudioServerStateDispatcher iAudioServerStateDispatcher) throws RemoteException;

    void registerPlaybackCallback(IPlaybackConfigDispatcher iPlaybackConfigDispatcher) throws RemoteException;

    void registerRecordingCallback(IRecordingConfigDispatcher iRecordingConfigDispatcher) throws RemoteException;

    void releasePlayer(int i) throws RemoteException;

    void reloadAudioSettings() throws RemoteException;

    int removeMixForPolicy(AudioPolicyConfig audioPolicyConfig, IAudioPolicyCallback iAudioPolicyCallback) throws RemoteException;

    int requestAudioFocus(AudioAttributes audioAttributes, int i, IBinder iBinder, IAudioFocusDispatcher iAudioFocusDispatcher, String str, String str2, int i2, IAudioPolicyCallback iAudioPolicyCallback, int i3) throws RemoteException;

    int setBluetoothA2dpDeviceConnectionState(BluetoothDevice bluetoothDevice, int i, int i2) throws RemoteException;

    int setBluetoothA2dpDeviceConnectionStateSuppressNoisyIntent(BluetoothDevice bluetoothDevice, int i, int i2, boolean z, int i3) throws RemoteException;

    void setBluetoothA2dpOn(boolean z) throws RemoteException;

    void setBluetoothScoOn(boolean z) throws RemoteException;

    int setFocusPropertiesForPolicy(int i, IAudioPolicyCallback iAudioPolicyCallback) throws RemoteException;

    void setFocusRequestResultFromExtPolicy(AudioFocusInfo audioFocusInfo, int i, IAudioPolicyCallback iAudioPolicyCallback) throws RemoteException;

    int setHdmiSystemAudioSupported(boolean z) throws RemoteException;

    void setHearingAidDeviceConnectionState(BluetoothDevice bluetoothDevice, int i) throws RemoteException;

    void setMasterMute(boolean z, int i, String str, int i2) throws RemoteException;

    void setMicrophoneMute(boolean z, String str, int i) throws RemoteException;

    void setMode(int i, IBinder iBinder, String str) throws RemoteException;

    void setRingerModeExternal(int i, String str) throws RemoteException;

    void setRingerModeInternal(int i, String str) throws RemoteException;

    void setRingtonePlayer(IRingtonePlayer iRingtonePlayer) throws RemoteException;

    void setSpeakerphoneOn(boolean z) throws RemoteException;

    void setStreamVolume(int i, int i2, int i3, String str) throws RemoteException;

    void setVibrateSetting(int i, int i2) throws RemoteException;

    void setVolumeController(IVolumeController iVolumeController) throws RemoteException;

    void setVolumePolicy(VolumePolicy volumePolicy) throws RemoteException;

    void setWiredDeviceConnectionState(int i, int i2, String str, String str2, String str3) throws RemoteException;

    boolean shouldVibrate(int i) throws RemoteException;

    void startBluetoothSco(IBinder iBinder, int i) throws RemoteException;

    void startBluetoothScoVirtualCall(IBinder iBinder) throws RemoteException;

    AudioRoutesInfo startWatchingRoutes(IAudioRoutesObserver iAudioRoutesObserver) throws RemoteException;

    void stopBluetoothSco(IBinder iBinder) throws RemoteException;

    int trackPlayer(PlayerIdCard playerIdCard) throws RemoteException;

    void unloadSoundEffects() throws RemoteException;

    void unregisterAudioFocusClient(String str) throws RemoteException;

    void unregisterAudioPolicyAsync(IAudioPolicyCallback iAudioPolicyCallback) throws RemoteException;

    void unregisterAudioServerStateDispatcher(IAudioServerStateDispatcher iAudioServerStateDispatcher) throws RemoteException;

    void unregisterPlaybackCallback(IPlaybackConfigDispatcher iPlaybackConfigDispatcher) throws RemoteException;

    void unregisterRecordingCallback(IRecordingConfigDispatcher iRecordingConfigDispatcher) throws RemoteException;
}
