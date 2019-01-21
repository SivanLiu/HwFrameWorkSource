package android.os;

import java.util.List;

public interface IPowerManager extends IInterface {

    public static abstract class Stub extends Binder implements IPowerManager {
        private static final String DESCRIPTOR = "android.os.IPowerManager";
        static final int TRANSACTION_acquireWakeLock = 1;
        static final int TRANSACTION_acquireWakeLockWithUid = 2;
        static final int TRANSACTION_boostScreenBrightness = 24;
        static final int TRANSACTION_convertBrightnessToSeekbarPercentage = 48;
        static final int TRANSACTION_convertSeekbarProgressToBrightness = 47;
        static final int TRANSACTION_crash = 21;
        static final int TRANSACTION_getAodState = 44;
        static final int TRANSACTION_getCoverModeBrightnessFromLastScreenBrightness = 52;
        static final int TRANSACTION_getDisplayPanelType = 58;
        static final int TRANSACTION_getHwInnerService = 64;
        static final int TRANSACTION_getLastShutdownReason = 22;
        static final int TRANSACTION_getPowerSaveState = 14;
        static final int TRANSACTION_getWakeLockPackageName = 56;
        static final int TRANSACTION_goToSleep = 10;
        static final int TRANSACTION_hwBrightnessGetData = 60;
        static final int TRANSACTION_hwBrightnessRegisterCallback = 61;
        static final int TRANSACTION_hwBrightnessSetData = 59;
        static final int TRANSACTION_hwBrightnessUnregisterCallback = 62;
        static final int TRANSACTION_isDeviceIdleMode = 16;
        static final int TRANSACTION_isHighPrecision = 33;
        static final int TRANSACTION_isInteractive = 12;
        static final int TRANSACTION_isLightDeviceIdleMode = 17;
        static final int TRANSACTION_isPowerSaveMode = 13;
        static final int TRANSACTION_isScreenBrightnessBoosted = 25;
        static final int TRANSACTION_isUsingSkipWakeLock = 40;
        static final int TRANSACTION_isWakeLockLevelSupported = 7;
        static final int TRANSACTION_nap = 11;
        static final int TRANSACTION_onCoverModeChanged = 51;
        static final int TRANSACTION_powerHint = 5;
        static final int TRANSACTION_reboot = 18;
        static final int TRANSACTION_rebootSafeMode = 19;
        static final int TRANSACTION_regeditAodStateCallback = 45;
        static final int TRANSACTION_releaseWakeLock = 3;
        static final int TRANSACTION_setAodAlpmState = 41;
        static final int TRANSACTION_setAodState = 42;
        static final int TRANSACTION_setAttentionLight = 31;
        static final int TRANSACTION_setAuthSucceeded = 49;
        static final int TRANSACTION_setBrightnessAnimationTime = 50;
        static final int TRANSACTION_setBrightnessNoLimit = 54;
        static final int TRANSACTION_setColorTemperature = 29;
        static final int TRANSACTION_setDozeAfterScreenOff = 63;
        static final int TRANSACTION_setDozeOverrideFromAod = 43;
        static final int TRANSACTION_setMaxBrightnessFromThermal = 53;
        static final int TRANSACTION_setMirrorLinkPowerStatus = 39;
        static final int TRANSACTION_setModeToAutoNoClearOffsetEnable = 55;
        static final int TRANSACTION_setPowerSaveMode = 15;
        static final int TRANSACTION_setPowerState = 57;
        static final int TRANSACTION_setStartDreamFromOtherFlag = 36;
        static final int TRANSACTION_setStayOnSetting = 23;
        static final int TRANSACTION_setTemporaryScreenAutoBrightnessAdjustmentSettingOverride = 28;
        static final int TRANSACTION_setTemporaryScreenAutoBrightnessSettingOverride = 27;
        static final int TRANSACTION_setTemporaryScreenBrightnessSettingOverride = 26;
        static final int TRANSACTION_shutdown = 20;
        static final int TRANSACTION_startDream = 37;
        static final int TRANSACTION_startWakeUpReady = 34;
        static final int TRANSACTION_stopDream = 38;
        static final int TRANSACTION_stopWakeUpReady = 35;
        static final int TRANSACTION_unregeditAodStateCallback = 46;
        static final int TRANSACTION_updateBlockedUids = 32;
        static final int TRANSACTION_updateRgbGamma = 30;
        static final int TRANSACTION_updateWakeLockUids = 4;
        static final int TRANSACTION_updateWakeLockWorkSource = 6;
        static final int TRANSACTION_userActivity = 8;
        static final int TRANSACTION_wakeUp = 9;

        private static class Proxy implements IPowerManager {
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

            public void acquireWakeLock(IBinder lock, int flags, String tag, String packageName, WorkSource ws, String historyTag) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(lock);
                    _data.writeInt(flags);
                    _data.writeString(tag);
                    _data.writeString(packageName);
                    if (ws != null) {
                        _data.writeInt(1);
                        ws.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(historyTag);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void acquireWakeLockWithUid(IBinder lock, int flags, String tag, String packageName, int uidtoblame) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(lock);
                    _data.writeInt(flags);
                    _data.writeString(tag);
                    _data.writeString(packageName);
                    _data.writeInt(uidtoblame);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void releaseWakeLock(IBinder lock, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(lock);
                    _data.writeInt(flags);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void updateWakeLockUids(IBinder lock, int[] uids) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(lock);
                    _data.writeIntArray(uids);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void powerHint(int hintId, int data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(hintId);
                    _data.writeInt(data);
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public void updateWakeLockWorkSource(IBinder lock, WorkSource ws, String historyTag) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(lock);
                    if (ws != null) {
                        _data.writeInt(1);
                        ws.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(historyTag);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isWakeLockLevelSupported(int level) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(level);
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

            public void userActivity(long time, int event, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(time);
                    _data.writeInt(event);
                    _data.writeInt(flags);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void wakeUp(long time, String reason, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(time);
                    _data.writeString(reason);
                    _data.writeString(opPackageName);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void goToSleep(long time, int reason, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(time);
                    _data.writeInt(reason);
                    _data.writeInt(flags);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void nap(long time) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(time);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isInteractive() throws RemoteException {
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

            public boolean isPowerSaveMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(13, _data, _reply, 0);
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

            public PowerSaveState getPowerSaveState(int serviceType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    PowerSaveState _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(serviceType);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (PowerSaveState) PowerSaveState.CREATOR.createFromParcel(_reply);
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

            public boolean setPowerSaveMode(boolean mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(mode);
                    boolean z = false;
                    this.mRemote.transact(15, _data, _reply, 0);
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

            public boolean isDeviceIdleMode() throws RemoteException {
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

            public boolean isLightDeviceIdleMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
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

            public void reboot(boolean confirm, String reason, boolean wait) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(confirm);
                    _data.writeString(reason);
                    _data.writeInt(wait);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void rebootSafeMode(boolean confirm, boolean wait) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(confirm);
                    _data.writeInt(wait);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void shutdown(boolean confirm, String reason, boolean wait) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(confirm);
                    _data.writeString(reason);
                    _data.writeInt(wait);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void crash(String message) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(message);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getLastShutdownReason() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setStayOnSetting(int val) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(val);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void boostScreenBrightness(long time) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(time);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isScreenBrightnessBoosted() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(25, _data, _reply, 0);
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

            public void setTemporaryScreenBrightnessSettingOverride(int brightness) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(brightness);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setTemporaryScreenAutoBrightnessSettingOverride(int brightness) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(brightness);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setTemporaryScreenAutoBrightnessAdjustmentSettingOverride(float adj) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeFloat(adj);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int setColorTemperature(int colorTemper) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(colorTemper);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int updateRgbGamma(float red, float green, float blue) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeFloat(red);
                    _data.writeFloat(green);
                    _data.writeFloat(blue);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setAttentionLight(boolean on, int color) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(on);
                    _data.writeInt(color);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void updateBlockedUids(int uid, boolean isBlocked) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(isBlocked);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isHighPrecision() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(33, _data, _reply, 0);
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

            public void startWakeUpReady(long eventTime, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(eventTime);
                    _data.writeString(opPackageName);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void stopWakeUpReady(long eventTime, boolean enableBright, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(eventTime);
                    _data.writeInt(enableBright);
                    _data.writeString(opPackageName);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setStartDreamFromOtherFlag(boolean flag) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(flag);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean startDream() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(37, _data, _reply, 0);
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

            public boolean stopDream() throws RemoteException {
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

            public void setMirrorLinkPowerStatus(boolean status) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(status);
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isUsingSkipWakeLock(int uid, String tag) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeString(tag);
                    boolean z = false;
                    this.mRemote.transact(40, _data, _reply, 0);
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

            public void setAodAlpmState(int globalState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(globalState);
                    this.mRemote.transact(41, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setAodState(int globalState, int alpmMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(globalState);
                    _data.writeInt(alpmMode);
                    this.mRemote.transact(42, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setDozeOverrideFromAod(int screenState, int screenBrightness, IBinder binder) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(screenState);
                    _data.writeInt(screenBrightness);
                    _data.writeStrongBinder(binder);
                    this.mRemote.transact(43, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getAodState(String file) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(file);
                    this.mRemote.transact(44, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void regeditAodStateCallback(IAodStateCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(45, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void unregeditAodStateCallback(IAodStateCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(46, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int convertSeekbarProgressToBrightness(int progress) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(progress);
                    this.mRemote.transact(47, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public float convertBrightnessToSeekbarPercentage(float brightness) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeFloat(brightness);
                    this.mRemote.transact(48, _data, _reply, 0);
                    _reply.readException();
                    float _result = _reply.readFloat();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setAuthSucceeded() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(49, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setBrightnessAnimationTime(boolean animationEnabled, int millisecond) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(animationEnabled);
                    _data.writeInt(millisecond);
                    this.mRemote.transact(50, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void onCoverModeChanged(boolean status) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(status);
                    this.mRemote.transact(51, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getCoverModeBrightnessFromLastScreenBrightness() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(52, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setMaxBrightnessFromThermal(int brightness) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(brightness);
                    this.mRemote.transact(53, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setBrightnessNoLimit(int brightness, int time) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(brightness);
                    _data.writeInt(time);
                    this.mRemote.transact(54, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setModeToAutoNoClearOffsetEnable(boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enable);
                    this.mRemote.transact(55, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<String> getWakeLockPackageName() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(56, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setPowerState(boolean para) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(para);
                    this.mRemote.transact(57, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getDisplayPanelType() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(58, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int hwBrightnessSetData(String name, Bundle data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    if (data != null) {
                        _data.writeInt(1);
                        data.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(59, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int hwBrightnessGetData(String name, Bundle data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    this.mRemote.transact(60, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    if (_reply.readInt() != 0) {
                        data.readFromParcel(_reply);
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int hwBrightnessRegisterCallback(IHwBrightnessCallback cb, List<String> filter) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(cb != null ? cb.asBinder() : null);
                    _data.writeStringList(filter);
                    this.mRemote.transact(61, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int hwBrightnessUnregisterCallback(IHwBrightnessCallback cb) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(cb != null ? cb.asBinder() : null);
                    this.mRemote.transact(62, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setDozeAfterScreenOff(boolean on) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(on);
                    this.mRemote.transact(63, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IBinder getHwInnerService() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(64, _data, _reply, 0);
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

        public static IPowerManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IPowerManager)) {
                return new Proxy(obj);
            }
            return (IPowerManager) iin;
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
                WorkSource _arg1 = null;
                boolean _arg2 = false;
                boolean _result;
                String _arg12;
                int _result2;
                int _result3;
                int _result4;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(descriptor);
                        IBinder _arg0 = data.readStrongBinder();
                        int _arg13 = data.readInt();
                        String _arg22 = data.readString();
                        String _arg3 = data.readString();
                        if (data.readInt() != 0) {
                            _arg1 = (WorkSource) WorkSource.CREATOR.createFromParcel(parcel);
                        }
                        acquireWakeLock(_arg0, _arg13, _arg22, _arg3, _arg1, data.readString());
                        reply.writeNoException();
                        return true;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        acquireWakeLockWithUid(data.readStrongBinder(), data.readInt(), data.readString(), data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 3:
                        parcel.enforceInterface(descriptor);
                        releaseWakeLock(data.readStrongBinder(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        updateWakeLockUids(data.readStrongBinder(), data.createIntArray());
                        reply.writeNoException();
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        powerHint(data.readInt(), data.readInt());
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        IBinder _arg02 = data.readStrongBinder();
                        if (data.readInt() != 0) {
                            _arg1 = (WorkSource) WorkSource.CREATOR.createFromParcel(parcel);
                        }
                        updateWakeLockWorkSource(_arg02, _arg1, data.readString());
                        reply.writeNoException();
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        _arg2 = isWakeLockLevelSupported(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg2);
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        userActivity(data.readLong(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        wakeUp(data.readLong(), data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 10:
                        parcel.enforceInterface(descriptor);
                        goToSleep(data.readLong(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        nap(data.readLong());
                        reply.writeNoException();
                        return true;
                    case 12:
                        parcel.enforceInterface(descriptor);
                        _result = isInteractive();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 13:
                        parcel.enforceInterface(descriptor);
                        _result = isPowerSaveMode();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        PowerSaveState _result5 = getPowerSaveState(data.readInt());
                        reply.writeNoException();
                        if (_result5 != null) {
                            parcel2.writeInt(1);
                            _result5.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg2 = true;
                        }
                        _arg2 = setPowerSaveMode(_arg2);
                        reply.writeNoException();
                        parcel2.writeInt(_arg2);
                        return true;
                    case 16:
                        parcel.enforceInterface(descriptor);
                        _result = isDeviceIdleMode();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        _result = isLightDeviceIdleMode();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 18:
                        parcel.enforceInterface(descriptor);
                        _result = data.readInt() != 0;
                        _arg12 = data.readString();
                        if (data.readInt() != 0) {
                            _arg2 = true;
                        }
                        reboot(_result, _arg12, _arg2);
                        reply.writeNoException();
                        return true;
                    case 19:
                        parcel.enforceInterface(descriptor);
                        _result = data.readInt() != 0;
                        if (data.readInt() != 0) {
                            _arg2 = true;
                        }
                        rebootSafeMode(_result, _arg2);
                        reply.writeNoException();
                        return true;
                    case 20:
                        parcel.enforceInterface(descriptor);
                        _result = data.readInt() != 0;
                        _arg12 = data.readString();
                        if (data.readInt() != 0) {
                            _arg2 = true;
                        }
                        shutdown(_result, _arg12, _arg2);
                        reply.writeNoException();
                        return true;
                    case 21:
                        parcel.enforceInterface(descriptor);
                        crash(data.readString());
                        reply.writeNoException();
                        return true;
                    case 22:
                        parcel.enforceInterface(descriptor);
                        _result2 = getLastShutdownReason();
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 23:
                        parcel.enforceInterface(descriptor);
                        setStayOnSetting(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 24:
                        parcel.enforceInterface(descriptor);
                        boostScreenBrightness(data.readLong());
                        reply.writeNoException();
                        return true;
                    case 25:
                        parcel.enforceInterface(descriptor);
                        _result = isScreenBrightnessBoosted();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 26:
                        parcel.enforceInterface(descriptor);
                        setTemporaryScreenBrightnessSettingOverride(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 27:
                        parcel.enforceInterface(descriptor);
                        setTemporaryScreenAutoBrightnessSettingOverride(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 28:
                        parcel.enforceInterface(descriptor);
                        setTemporaryScreenAutoBrightnessAdjustmentSettingOverride(data.readFloat());
                        reply.writeNoException();
                        return true;
                    case 29:
                        parcel.enforceInterface(descriptor);
                        _result3 = setColorTemperature(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 30:
                        parcel.enforceInterface(descriptor);
                        int _result6 = updateRgbGamma(data.readFloat(), data.readFloat(), data.readFloat());
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return true;
                    case 31:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg2 = true;
                        }
                        setAttentionLight(_arg2, data.readInt());
                        reply.writeNoException();
                        return true;
                    case 32:
                        parcel.enforceInterface(descriptor);
                        _result2 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg2 = true;
                        }
                        updateBlockedUids(_result2, _arg2);
                        reply.writeNoException();
                        return true;
                    case 33:
                        parcel.enforceInterface(descriptor);
                        _result = isHighPrecision();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 34:
                        parcel.enforceInterface(descriptor);
                        startWakeUpReady(data.readLong(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 35:
                        parcel.enforceInterface(descriptor);
                        long _arg03 = data.readLong();
                        if (data.readInt() != 0) {
                            _arg2 = true;
                        }
                        stopWakeUpReady(_arg03, _arg2, data.readString());
                        reply.writeNoException();
                        return true;
                    case 36:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg2 = true;
                        }
                        setStartDreamFromOtherFlag(_arg2);
                        reply.writeNoException();
                        return true;
                    case 37:
                        parcel.enforceInterface(descriptor);
                        _result = startDream();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 38:
                        parcel.enforceInterface(descriptor);
                        _result = stopDream();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 39:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg2 = true;
                        }
                        setMirrorLinkPowerStatus(_arg2);
                        reply.writeNoException();
                        return true;
                    case 40:
                        parcel.enforceInterface(descriptor);
                        boolean _result7 = isUsingSkipWakeLock(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result7);
                        return true;
                    case 41:
                        parcel.enforceInterface(descriptor);
                        setAodAlpmState(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 42:
                        parcel.enforceInterface(descriptor);
                        setAodState(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 43:
                        parcel.enforceInterface(descriptor);
                        setDozeOverrideFromAod(data.readInt(), data.readInt(), data.readStrongBinder());
                        reply.writeNoException();
                        return true;
                    case 44:
                        parcel.enforceInterface(descriptor);
                        _result3 = getAodState(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 45:
                        parcel.enforceInterface(descriptor);
                        regeditAodStateCallback(android.os.IAodStateCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 46:
                        parcel.enforceInterface(descriptor);
                        unregeditAodStateCallback(android.os.IAodStateCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 47:
                        parcel.enforceInterface(descriptor);
                        _result3 = convertSeekbarProgressToBrightness(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 48:
                        parcel.enforceInterface(descriptor);
                        float _result8 = convertBrightnessToSeekbarPercentage(data.readFloat());
                        reply.writeNoException();
                        parcel2.writeFloat(_result8);
                        return true;
                    case 49:
                        parcel.enforceInterface(descriptor);
                        setAuthSucceeded();
                        reply.writeNoException();
                        return true;
                    case 50:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg2 = true;
                        }
                        setBrightnessAnimationTime(_arg2, data.readInt());
                        reply.writeNoException();
                        return true;
                    case 51:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg2 = true;
                        }
                        onCoverModeChanged(_arg2);
                        reply.writeNoException();
                        return true;
                    case 52:
                        parcel.enforceInterface(descriptor);
                        _result2 = getCoverModeBrightnessFromLastScreenBrightness();
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 53:
                        parcel.enforceInterface(descriptor);
                        setMaxBrightnessFromThermal(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 54:
                        parcel.enforceInterface(descriptor);
                        setBrightnessNoLimit(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 55:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg2 = true;
                        }
                        setModeToAutoNoClearOffsetEnable(_arg2);
                        reply.writeNoException();
                        return true;
                    case 56:
                        parcel.enforceInterface(descriptor);
                        List<String> _result9 = getWakeLockPackageName();
                        reply.writeNoException();
                        parcel2.writeStringList(_result9);
                        return true;
                    case 57:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg2 = true;
                        }
                        setPowerState(_arg2);
                        reply.writeNoException();
                        return true;
                    case 58:
                        parcel.enforceInterface(descriptor);
                        _result2 = getDisplayPanelType();
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 59:
                        Bundle _arg14;
                        parcel.enforceInterface(descriptor);
                        String _arg04 = data.readString();
                        if (data.readInt() != 0) {
                            _arg14 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        }
                        _result4 = hwBrightnessSetData(_arg04, _arg14);
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 60:
                        parcel.enforceInterface(descriptor);
                        String _arg05 = data.readString();
                        Bundle _arg15 = new Bundle();
                        _result4 = hwBrightnessGetData(_arg05, _arg15);
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        parcel2.writeInt(1);
                        _arg15.writeToParcel(parcel2, 1);
                        return true;
                    case 61:
                        parcel.enforceInterface(descriptor);
                        _result4 = hwBrightnessRegisterCallback(android.os.IHwBrightnessCallback.Stub.asInterface(data.readStrongBinder()), data.createStringArrayList());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 62:
                        parcel.enforceInterface(descriptor);
                        _result3 = hwBrightnessUnregisterCallback(android.os.IHwBrightnessCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 63:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg2 = true;
                        }
                        setDozeAfterScreenOff(_arg2);
                        reply.writeNoException();
                        return true;
                    case 64:
                        parcel.enforceInterface(descriptor);
                        IBinder _result10 = getHwInnerService();
                        reply.writeNoException();
                        parcel2.writeStrongBinder(_result10);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            parcel2.writeString(descriptor);
            return true;
        }
    }

    void acquireWakeLock(IBinder iBinder, int i, String str, String str2, WorkSource workSource, String str3) throws RemoteException;

    void acquireWakeLockWithUid(IBinder iBinder, int i, String str, String str2, int i2) throws RemoteException;

    void boostScreenBrightness(long j) throws RemoteException;

    float convertBrightnessToSeekbarPercentage(float f) throws RemoteException;

    int convertSeekbarProgressToBrightness(int i) throws RemoteException;

    void crash(String str) throws RemoteException;

    int getAodState(String str) throws RemoteException;

    int getCoverModeBrightnessFromLastScreenBrightness() throws RemoteException;

    int getDisplayPanelType() throws RemoteException;

    IBinder getHwInnerService() throws RemoteException;

    int getLastShutdownReason() throws RemoteException;

    PowerSaveState getPowerSaveState(int i) throws RemoteException;

    List<String> getWakeLockPackageName() throws RemoteException;

    void goToSleep(long j, int i, int i2) throws RemoteException;

    int hwBrightnessGetData(String str, Bundle bundle) throws RemoteException;

    int hwBrightnessRegisterCallback(IHwBrightnessCallback iHwBrightnessCallback, List<String> list) throws RemoteException;

    int hwBrightnessSetData(String str, Bundle bundle) throws RemoteException;

    int hwBrightnessUnregisterCallback(IHwBrightnessCallback iHwBrightnessCallback) throws RemoteException;

    boolean isDeviceIdleMode() throws RemoteException;

    boolean isHighPrecision() throws RemoteException;

    boolean isInteractive() throws RemoteException;

    boolean isLightDeviceIdleMode() throws RemoteException;

    boolean isPowerSaveMode() throws RemoteException;

    boolean isScreenBrightnessBoosted() throws RemoteException;

    boolean isUsingSkipWakeLock(int i, String str) throws RemoteException;

    boolean isWakeLockLevelSupported(int i) throws RemoteException;

    void nap(long j) throws RemoteException;

    void onCoverModeChanged(boolean z) throws RemoteException;

    void powerHint(int i, int i2) throws RemoteException;

    void reboot(boolean z, String str, boolean z2) throws RemoteException;

    void rebootSafeMode(boolean z, boolean z2) throws RemoteException;

    void regeditAodStateCallback(IAodStateCallback iAodStateCallback) throws RemoteException;

    void releaseWakeLock(IBinder iBinder, int i) throws RemoteException;

    void setAodAlpmState(int i) throws RemoteException;

    void setAodState(int i, int i2) throws RemoteException;

    void setAttentionLight(boolean z, int i) throws RemoteException;

    void setAuthSucceeded() throws RemoteException;

    void setBrightnessAnimationTime(boolean z, int i) throws RemoteException;

    void setBrightnessNoLimit(int i, int i2) throws RemoteException;

    int setColorTemperature(int i) throws RemoteException;

    void setDozeAfterScreenOff(boolean z) throws RemoteException;

    void setDozeOverrideFromAod(int i, int i2, IBinder iBinder) throws RemoteException;

    void setMaxBrightnessFromThermal(int i) throws RemoteException;

    void setMirrorLinkPowerStatus(boolean z) throws RemoteException;

    void setModeToAutoNoClearOffsetEnable(boolean z) throws RemoteException;

    boolean setPowerSaveMode(boolean z) throws RemoteException;

    void setPowerState(boolean z) throws RemoteException;

    void setStartDreamFromOtherFlag(boolean z) throws RemoteException;

    void setStayOnSetting(int i) throws RemoteException;

    void setTemporaryScreenAutoBrightnessAdjustmentSettingOverride(float f) throws RemoteException;

    void setTemporaryScreenAutoBrightnessSettingOverride(int i) throws RemoteException;

    void setTemporaryScreenBrightnessSettingOverride(int i) throws RemoteException;

    void shutdown(boolean z, String str, boolean z2) throws RemoteException;

    boolean startDream() throws RemoteException;

    void startWakeUpReady(long j, String str) throws RemoteException;

    boolean stopDream() throws RemoteException;

    void stopWakeUpReady(long j, boolean z, String str) throws RemoteException;

    void unregeditAodStateCallback(IAodStateCallback iAodStateCallback) throws RemoteException;

    void updateBlockedUids(int i, boolean z) throws RemoteException;

    int updateRgbGamma(float f, float f2, float f3) throws RemoteException;

    void updateWakeLockUids(IBinder iBinder, int[] iArr) throws RemoteException;

    void updateWakeLockWorkSource(IBinder iBinder, WorkSource workSource, String str) throws RemoteException;

    void userActivity(long j, int i, int i2) throws RemoteException;

    void wakeUp(long j, String str, String str2) throws RemoteException;
}
