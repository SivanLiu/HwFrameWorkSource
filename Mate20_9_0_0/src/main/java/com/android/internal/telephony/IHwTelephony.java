package com.android.internal.telephony;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.telephony.UiccAuthResponse;

public interface IHwTelephony extends IInterface {

    public static abstract class Stub extends Binder implements IHwTelephony {
        private static final String DESCRIPTOR = "com.android.internal.telephony.IHwTelephony";
        static final int TRANSACTION_bindSimToProfile = 98;
        static final int TRANSACTION_changeSimPinCode = 84;
        static final int TRANSACTION_closeRrc = 6;
        static final int TRANSACTION_cmdForECInfo = 78;
        static final int TRANSACTION_get2GServiceAbility = 23;
        static final int TRANSACTION_getAntiFakeBaseStation = 105;
        static final int TRANSACTION_getCallForwardingOption = 93;
        static final int TRANSACTION_getCardTrayInfo = 108;
        static final int TRANSACTION_getCdmaGsmImsi = 39;
        static final int TRANSACTION_getCdmaGsmImsiForSubId = 99;
        static final int TRANSACTION_getCdmaMlplVersion = 42;
        static final int TRANSACTION_getCdmaMsplVersion = 43;
        static final int TRANSACTION_getCellLocation = 41;
        static final int TRANSACTION_getDataStateForSubscriber = 12;
        static final int TRANSACTION_getDefault4GSlotId = 26;
        static final int TRANSACTION_getDemoString = 1;
        static final int TRANSACTION_getImsDomain = 63;
        static final int TRANSACTION_getImsDomainByPhoneId = 73;
        static final int TRANSACTION_getImsImpu = 95;
        static final int TRANSACTION_getImsSwitch = 59;
        static final int TRANSACTION_getImsSwitchByPhoneId = 69;
        static final int TRANSACTION_getLaaDetailedState = 87;
        static final int TRANSACTION_getLine1NumberFromImpu = 96;
        static final int TRANSACTION_getLteServiceAbility = 14;
        static final int TRANSACTION_getLteServiceAbilityForSubId = 15;
        static final int TRANSACTION_getMeidForSubscriber = 2;
        static final int TRANSACTION_getNVESN = 11;
        static final int TRANSACTION_getOnDemandDataSubId = 37;
        static final int TRANSACTION_getPesnForSubscriber = 3;
        static final int TRANSACTION_getPreferredDataSubscription = 38;
        static final int TRANSACTION_getSpecCardType = 29;
        static final int TRANSACTION_getSubState = 4;
        static final int TRANSACTION_getUiccAppType = 62;
        static final int TRANSACTION_getUiccAppTypeByPhoneId = 72;
        static final int TRANSACTION_getUiccCardType = 40;
        static final int TRANSACTION_getUniqueDeviceId = 44;
        static final int TRANSACTION_getWaitingSwitchBalongSlot = 36;
        static final int TRANSACTION_handleMapconImsaReq = 61;
        static final int TRANSACTION_handleMapconImsaReqByPhoneId = 71;
        static final int TRANSACTION_handleUiccAuth = 64;
        static final int TRANSACTION_handleUiccAuthByPhoneId = 74;
        static final int TRANSACTION_informModemTetherStatusToChangeGRO = 102;
        static final int TRANSACTION_invokeOemRilRequestRaw = 90;
        static final int TRANSACTION_is4RMimoEnabled = 104;
        static final int TRANSACTION_isCTCdmaCardInGsmMode = 7;
        static final int TRANSACTION_isCardUimLocked = 30;
        static final int TRANSACTION_isCspPlmnEnabled = 91;
        static final int TRANSACTION_isCtSimCard = 79;
        static final int TRANSACTION_isDomesticCard = 47;
        static final int TRANSACTION_isImsRegisteredForSubId = 17;
        static final int TRANSACTION_isLTESupported = 45;
        static final int TRANSACTION_isNeedToRadioPowerOn = 25;
        static final int TRANSACTION_isRadioAvailable = 57;
        static final int TRANSACTION_isRadioAvailableByPhoneId = 67;
        static final int TRANSACTION_isRadioOn = 31;
        static final int TRANSACTION_isSecondaryCardGsmOnly = 97;
        static final int TRANSACTION_isSetDefault4GSlotIdEnabled = 34;
        static final int TRANSACTION_isSubDeactivedByPowerOff = 24;
        static final int TRANSACTION_isVideoTelephonyAvailableForSubId = 20;
        static final int TRANSACTION_isVolteAvailableForSubId = 19;
        static final int TRANSACTION_isWifiCallingAvailableForSubId = 18;
        static final int TRANSACTION_notifyCModemStatus = 80;
        static final int TRANSACTION_notifyCellularCommParaReady = 82;
        static final int TRANSACTION_notifyDeviceState = 81;
        static final int TRANSACTION_queryServiceCellBand = 50;
        static final int TRANSACTION_registerCommonImsaToMapconInfo = 55;
        static final int TRANSACTION_registerForAntiFakeBaseStation = 106;
        static final int TRANSACTION_registerForCallAltSrv = 88;
        static final int TRANSACTION_registerForPhoneEvent = 65;
        static final int TRANSACTION_registerForRadioAvailable = 51;
        static final int TRANSACTION_registerForRadioNotAvailable = 53;
        static final int TRANSACTION_registerForWirelessState = 75;
        static final int TRANSACTION_sendLaaCmd = 86;
        static final int TRANSACTION_sendPseudocellCellInfo = 85;
        static final int TRANSACTION_sendSimMatchedOperatorInfo = 103;
        static final int TRANSACTION_set2GServiceAbility = 22;
        static final int TRANSACTION_setCallForwardingOption = 92;
        static final int TRANSACTION_setCellTxPower = 49;
        static final int TRANSACTION_setDataEnabledWithoutPromp = 9;
        static final int TRANSACTION_setDataRoamingEnabledWithoutPromp = 10;
        static final int TRANSACTION_setDeepNoDisturbState = 101;
        static final int TRANSACTION_setDefault4GSlotId = 33;
        static final int TRANSACTION_setDefaultDataSlotId = 27;
        static final int TRANSACTION_setDefaultMobileEnable = 8;
        static final int TRANSACTION_setISMCOEX = 46;
        static final int TRANSACTION_setImsDomainConfig = 60;
        static final int TRANSACTION_setImsDomainConfigByPhoneId = 70;
        static final int TRANSACTION_setImsRegistrationStateForSubId = 21;
        static final int TRANSACTION_setImsSwitch = 58;
        static final int TRANSACTION_setImsSwitchByPhoneId = 68;
        static final int TRANSACTION_setLine1Number = 100;
        static final int TRANSACTION_setLteServiceAbility = 13;
        static final int TRANSACTION_setLteServiceAbilityForSubId = 16;
        static final int TRANSACTION_setMaxTxPower = 77;
        static final int TRANSACTION_setPinLockEnabled = 83;
        static final int TRANSACTION_setPreferredNetworkType = 32;
        static final int TRANSACTION_setSubscription = 94;
        static final int TRANSACTION_setUserPrefDataSlotId = 5;
        static final int TRANSACTION_setWifiTxPower = 48;
        static final int TRANSACTION_unregisterCommonImsaToMapconInfo = 56;
        static final int TRANSACTION_unregisterForAntiFakeBaseStation = 107;
        static final int TRANSACTION_unregisterForCallAltSrv = 89;
        static final int TRANSACTION_unregisterForPhoneEvent = 66;
        static final int TRANSACTION_unregisterForRadioAvailable = 52;
        static final int TRANSACTION_unregisterForRadioNotAvailable = 54;
        static final int TRANSACTION_unregisterForWirelessState = 76;
        static final int TRANSACTION_updateCrurrentPhone = 28;
        static final int TRANSACTION_waitingSetDefault4GSlotDone = 35;

        private static class Proxy implements IHwTelephony {
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

            public String getDemoString() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getMeidForSubscriber(int slotId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getPesnForSubscriber(int slotId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getSubState(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setUserPrefDataSlotId(int slotId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void closeRrc() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isCTCdmaCardInGsmMode() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
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

            public void setDefaultMobileEnable(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enabled);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setDataEnabledWithoutPromp(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enabled);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setDataRoamingEnabledWithoutPromp(boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enabled);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getNVESN() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getDataStateForSubscriber(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setLteServiceAbility(int ability) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(ability);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getLteServiceAbility() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getLteServiceAbilityForSubId(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setLteServiceAbilityForSubId(int subId, int ability) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(ability);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isImsRegisteredForSubId(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
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

            public boolean isWifiCallingAvailableForSubId(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
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

            public boolean isVolteAvailableForSubId(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
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

            public boolean isVideoTelephonyAvailableForSubId(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    boolean z = false;
                    this.mRemote.transact(20, _data, _reply, 0);
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

            public void setImsRegistrationStateForSubId(int subId, boolean registered) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(registered);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void set2GServiceAbility(int ability) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(ability);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int get2GServiceAbility() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isSubDeactivedByPowerOff(long sub) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(sub);
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

            public boolean isNeedToRadioPowerOn(long sub) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(sub);
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

            public int getDefault4GSlotId() throws RemoteException {
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

            public void setDefaultDataSlotId(int slotId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void updateCrurrentPhone(int lteSlot) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(lteSlot);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getSpecCardType(int slotId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isCardUimLocked(int slotId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    boolean z = false;
                    this.mRemote.transact(30, _data, _reply, 0);
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

            public boolean isRadioOn(int slotId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    boolean z = false;
                    this.mRemote.transact(31, _data, _reply, 0);
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

            public void setPreferredNetworkType(int nwMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(nwMode);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setDefault4GSlotId(int slotId, Message msg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    if (msg != null) {
                        _data.writeInt(1);
                        msg.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isSetDefault4GSlotIdEnabled() throws RemoteException {
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

            public void waitingSetDefault4GSlotDone(boolean waiting) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(waiting);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean getWaitingSwitchBalongSlot() throws RemoteException {
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

            public int getOnDemandDataSubId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getPreferredDataSubscription() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(38, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getCdmaGsmImsi() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getUiccCardType(int slotId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public Bundle getCellLocation(int slotId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Bundle _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    this.mRemote.transact(41, _data, _reply, 0);
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

            public String getCdmaMlplVersion() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(42, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getCdmaMsplVersion() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(43, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getUniqueDeviceId(int scope) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(scope);
                    this.mRemote.transact(Stub.TRANSACTION_getUniqueDeviceId, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isLTESupported() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(Stub.TRANSACTION_isLTESupported, _data, _reply, 0);
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

            public boolean setISMCOEX(String setISMCoex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(setISMCoex);
                    boolean z = false;
                    this.mRemote.transact(Stub.TRANSACTION_setISMCOEX, _data, _reply, 0);
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

            public boolean isDomesticCard(int slotId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    boolean z = false;
                    this.mRemote.transact(Stub.TRANSACTION_isDomesticCard, _data, _reply, 0);
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

            public boolean setWifiTxPower(int power) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(power);
                    boolean z = false;
                    this.mRemote.transact(48, _data, _reply, 0);
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

            public boolean setCellTxPower(int power) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(power);
                    boolean z = false;
                    this.mRemote.transact(Stub.TRANSACTION_setCellTxPower, _data, _reply, 0);
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

            public String[] queryServiceCellBand() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(50, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean registerForRadioAvailable(IPhoneCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    boolean z = false;
                    this.mRemote.transact(Stub.TRANSACTION_registerForRadioAvailable, _data, _reply, 0);
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

            public boolean unregisterForRadioAvailable(IPhoneCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    boolean z = false;
                    this.mRemote.transact(Stub.TRANSACTION_unregisterForRadioAvailable, _data, _reply, 0);
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

            public boolean registerForRadioNotAvailable(IPhoneCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    boolean z = false;
                    this.mRemote.transact(Stub.TRANSACTION_registerForRadioNotAvailable, _data, _reply, 0);
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

            public boolean unregisterForRadioNotAvailable(IPhoneCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    boolean z = false;
                    this.mRemote.transact(Stub.TRANSACTION_unregisterForRadioNotAvailable, _data, _reply, 0);
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

            public boolean registerCommonImsaToMapconInfo(IPhoneCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    boolean z = false;
                    this.mRemote.transact(Stub.TRANSACTION_registerCommonImsaToMapconInfo, _data, _reply, 0);
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

            public boolean unregisterCommonImsaToMapconInfo(IPhoneCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    boolean z = false;
                    this.mRemote.transact(Stub.TRANSACTION_unregisterCommonImsaToMapconInfo, _data, _reply, 0);
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

            public boolean isRadioAvailable() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(Stub.TRANSACTION_isRadioAvailable, _data, _reply, 0);
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

            public void setImsSwitch(boolean value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(value);
                    this.mRemote.transact(Stub.TRANSACTION_setImsSwitch, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean getImsSwitch() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(Stub.TRANSACTION_getImsSwitch, _data, _reply, 0);
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

            public void setImsDomainConfig(int domainType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(domainType);
                    this.mRemote.transact(Stub.TRANSACTION_setImsDomainConfig, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean handleMapconImsaReq(byte[] Msg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(Msg);
                    boolean z = false;
                    this.mRemote.transact(Stub.TRANSACTION_handleMapconImsaReq, _data, _reply, 0);
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

            public int getUiccAppType() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getUiccAppType, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getImsDomain() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getImsDomain, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public UiccAuthResponse handleUiccAuth(int auth_type, byte[] rand, byte[] auth) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    UiccAuthResponse _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(auth_type);
                    _data.writeByteArray(rand);
                    _data.writeByteArray(auth);
                    this.mRemote.transact(64, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (UiccAuthResponse) UiccAuthResponse.CREATOR.createFromParcel(_reply);
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

            public boolean registerForPhoneEvent(int phoneId, IPhoneCallback callback, int events) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(phoneId);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeInt(events);
                    boolean z = false;
                    this.mRemote.transact(65, _data, _reply, 0);
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

            public void unregisterForPhoneEvent(IPhoneCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(66, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isRadioAvailableByPhoneId(int phoneId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(phoneId);
                    boolean z = false;
                    this.mRemote.transact(67, _data, _reply, 0);
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

            public void setImsSwitchByPhoneId(int phoneId, boolean value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(phoneId);
                    _data.writeInt(value);
                    this.mRemote.transact(68, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean getImsSwitchByPhoneId(int phoneId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(phoneId);
                    boolean z = false;
                    this.mRemote.transact(69, _data, _reply, 0);
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

            public void setImsDomainConfigByPhoneId(int phoneId, int domainType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(phoneId);
                    _data.writeInt(domainType);
                    this.mRemote.transact(Stub.TRANSACTION_setImsDomainConfigByPhoneId, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean handleMapconImsaReqByPhoneId(int phoneId, byte[] Msg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(phoneId);
                    _data.writeByteArray(Msg);
                    boolean z = false;
                    this.mRemote.transact(71, _data, _reply, 0);
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

            public int getUiccAppTypeByPhoneId(int phoneId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(phoneId);
                    this.mRemote.transact(Stub.TRANSACTION_getUiccAppTypeByPhoneId, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getImsDomainByPhoneId(int phoneId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(phoneId);
                    this.mRemote.transact(Stub.TRANSACTION_getImsDomainByPhoneId, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public UiccAuthResponse handleUiccAuthByPhoneId(int phoneId, int auth_type, byte[] rand, byte[] auth) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    UiccAuthResponse _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(phoneId);
                    _data.writeInt(auth_type);
                    _data.writeByteArray(rand);
                    _data.writeByteArray(auth);
                    this.mRemote.transact(Stub.TRANSACTION_handleUiccAuthByPhoneId, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (UiccAuthResponse) UiccAuthResponse.CREATOR.createFromParcel(_reply);
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

            public boolean registerForWirelessState(int type, int slotId, IPhoneCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(type);
                    _data.writeInt(slotId);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    boolean z = false;
                    this.mRemote.transact(Stub.TRANSACTION_registerForWirelessState, _data, _reply, 0);
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

            public boolean unregisterForWirelessState(int type, int slotId, IPhoneCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(type);
                    _data.writeInt(slotId);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    boolean z = false;
                    this.mRemote.transact(Stub.TRANSACTION_unregisterForWirelessState, _data, _reply, 0);
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

            public boolean setMaxTxPower(int type, int power) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(type);
                    _data.writeInt(power);
                    boolean z = false;
                    this.mRemote.transact(Stub.TRANSACTION_setMaxTxPower, _data, _reply, 0);
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

            public boolean cmdForECInfo(int event, int action, byte[] buf) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(event);
                    _data.writeInt(action);
                    _data.writeByteArray(buf);
                    boolean z = false;
                    this.mRemote.transact(Stub.TRANSACTION_cmdForECInfo, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.readByteArray(buf);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isCtSimCard(int slotId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    boolean z = false;
                    this.mRemote.transact(Stub.TRANSACTION_isCtSimCard, _data, _reply, 0);
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

            public void notifyCModemStatus(int status, IPhoneCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(status);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(80, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean notifyDeviceState(String device, String state, String extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(device);
                    _data.writeString(state);
                    _data.writeString(extras);
                    boolean z = false;
                    this.mRemote.transact(81, _data, _reply, 0);
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

            public void notifyCellularCommParaReady(int paratype, int pathtype, Message response) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(paratype);
                    _data.writeInt(pathtype);
                    if (response != null) {
                        _data.writeInt(1);
                        response.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(82, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setPinLockEnabled(boolean enablePinLock, String password, int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enablePinLock);
                    _data.writeString(password);
                    _data.writeInt(subId);
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

            public boolean changeSimPinCode(String oldPinCode, String newPinCode, int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(oldPinCode);
                    _data.writeString(newPinCode);
                    _data.writeInt(subId);
                    boolean z = false;
                    this.mRemote.transact(84, _data, _reply, 0);
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

            public boolean sendPseudocellCellInfo(int type, int lac, int cid, int radioTech, String plmn, int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(type);
                    _data.writeInt(lac);
                    _data.writeInt(cid);
                    _data.writeInt(radioTech);
                    _data.writeString(plmn);
                    _data.writeInt(subId);
                    boolean z = false;
                    this.mRemote.transact(85, _data, _reply, 0);
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

            public boolean sendLaaCmd(int cmd, String reserved, Message response) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(cmd);
                    _data.writeString(reserved);
                    boolean _result = true;
                    if (response != null) {
                        _data.writeInt(1);
                        response.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(86, _data, _reply, 0);
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

            public boolean getLaaDetailedState(String reserved, Message response) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(reserved);
                    boolean _result = true;
                    if (response != null) {
                        _data.writeInt(1);
                        response.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(87, _data, _reply, 0);
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

            public void registerForCallAltSrv(int subId, IPhoneCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(Stub.TRANSACTION_registerForCallAltSrv, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void unregisterForCallAltSrv(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(Stub.TRANSACTION_unregisterForCallAltSrv, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int invokeOemRilRequestRaw(int phoneId, byte[] oemReq, byte[] oemResp) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(phoneId);
                    _data.writeByteArray(oemReq);
                    _data.writeByteArray(oemResp);
                    this.mRemote.transact(90, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    _reply.readByteArray(oemResp);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isCspPlmnEnabled(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    boolean z = false;
                    this.mRemote.transact(91, _data, _reply, 0);
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

            public void setCallForwardingOption(int subId, int commandInterfaceCFAction, int commandInterfaceCFReason, String dialingNumber, int timerSeconds, Message response) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(commandInterfaceCFAction);
                    _data.writeInt(commandInterfaceCFReason);
                    _data.writeString(dialingNumber);
                    _data.writeInt(timerSeconds);
                    if (response != null) {
                        _data.writeInt(1);
                        response.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(92, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void getCallForwardingOption(int subId, int commandInterfaceCFReason, Message response) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(commandInterfaceCFReason);
                    if (response != null) {
                        _data.writeInt(1);
                        response.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(93, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setSubscription(int subId, boolean activate, Message response) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(activate);
                    boolean _result = true;
                    if (response != null) {
                        _data.writeInt(1);
                        response.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(94, _data, _reply, 0);
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

            public String getImsImpu(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(95, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getLine1NumberFromImpu(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(96, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isSecondaryCardGsmOnly() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(97, _data, _reply, 0);
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

            public boolean bindSimToProfile(int slotId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    boolean z = false;
                    this.mRemote.transact(98, _data, _reply, 0);
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

            public String getCdmaGsmImsiForSubId(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(99, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setLine1Number(int subId, String alphaTag, String number, Message onComplete) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(alphaTag);
                    _data.writeString(number);
                    boolean _result = true;
                    if (onComplete != null) {
                        _data.writeInt(1);
                        onComplete.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(100, _data, _reply, 0);
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

            public boolean setDeepNoDisturbState(int slotId, int state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    _data.writeInt(state);
                    boolean z = false;
                    this.mRemote.transact(101, _data, _reply, 0);
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

            public void informModemTetherStatusToChangeGRO(int enable, String faceName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enable);
                    _data.writeString(faceName);
                    this.mRemote.transact(102, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean sendSimMatchedOperatorInfo(int slotId, String opKey, String opName, int state, String reserveField) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    _data.writeString(opKey);
                    _data.writeString(opName);
                    _data.writeInt(state);
                    _data.writeString(reserveField);
                    boolean z = false;
                    this.mRemote.transact(103, _data, _reply, 0);
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

            public boolean is4RMimoEnabled(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    boolean z = false;
                    this.mRemote.transact(104, _data, _reply, 0);
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

            public boolean getAntiFakeBaseStation(Message response) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (response != null) {
                        _data.writeInt(1);
                        response.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(Stub.TRANSACTION_getAntiFakeBaseStation, _data, _reply, 0);
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

            public boolean registerForAntiFakeBaseStation(IPhoneCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    boolean z = false;
                    this.mRemote.transact(106, _data, _reply, 0);
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

            public boolean unregisterForAntiFakeBaseStation() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(Stub.TRANSACTION_unregisterForAntiFakeBaseStation, _data, _reply, 0);
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

            public byte[] getCardTrayInfo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_getCardTrayInfo, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
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

        public static IHwTelephony asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IHwTelephony)) {
                return new Proxy(obj);
            }
            return (IHwTelephony) iin;
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
                Message _arg2 = null;
                boolean _arg1 = false;
                String _result;
                String _result2;
                int _result3;
                boolean _result4;
                int _result5;
                boolean _result6;
                boolean _result7;
                byte[] _arg22;
                int _arg12;
                String _arg13;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(descriptor);
                        _result = getDemoString();
                        reply.writeNoException();
                        parcel2.writeString(_result);
                        return true;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        _result2 = getMeidForSubscriber(data.readInt());
                        reply.writeNoException();
                        parcel2.writeString(_result2);
                        return true;
                    case 3:
                        parcel.enforceInterface(descriptor);
                        _result2 = getPesnForSubscriber(data.readInt());
                        reply.writeNoException();
                        parcel2.writeString(_result2);
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        _result3 = getSubState(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        setUserPrefDataSlotId(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        closeRrc();
                        reply.writeNoException();
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        _result4 = isCTCdmaCardInGsmMode();
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        setDefaultMobileEnable(_arg1);
                        reply.writeNoException();
                        return true;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        setDataEnabledWithoutPromp(_arg1);
                        reply.writeNoException();
                        return true;
                    case 10:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        setDataRoamingEnabledWithoutPromp(_arg1);
                        reply.writeNoException();
                        return true;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        _result = getNVESN();
                        reply.writeNoException();
                        parcel2.writeString(_result);
                        return true;
                    case 12:
                        parcel.enforceInterface(descriptor);
                        _result3 = getDataStateForSubscriber(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 13:
                        parcel.enforceInterface(descriptor);
                        setLteServiceAbility(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        _result5 = getLteServiceAbility();
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        _result3 = getLteServiceAbilityForSubId(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 16:
                        parcel.enforceInterface(descriptor);
                        setLteServiceAbilityForSubId(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        _arg1 = isImsRegisteredForSubId(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 18:
                        parcel.enforceInterface(descriptor);
                        _arg1 = isWifiCallingAvailableForSubId(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 19:
                        parcel.enforceInterface(descriptor);
                        _arg1 = isVolteAvailableForSubId(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 20:
                        parcel.enforceInterface(descriptor);
                        _arg1 = isVideoTelephonyAvailableForSubId(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 21:
                        parcel.enforceInterface(descriptor);
                        _result5 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        setImsRegistrationStateForSubId(_result5, _arg1);
                        reply.writeNoException();
                        return true;
                    case 22:
                        parcel.enforceInterface(descriptor);
                        set2GServiceAbility(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 23:
                        parcel.enforceInterface(descriptor);
                        _result5 = get2GServiceAbility();
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case 24:
                        parcel.enforceInterface(descriptor);
                        _result6 = isSubDeactivedByPowerOff(data.readLong());
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return true;
                    case 25:
                        parcel.enforceInterface(descriptor);
                        _result6 = isNeedToRadioPowerOn(data.readLong());
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return true;
                    case 26:
                        parcel.enforceInterface(descriptor);
                        _result5 = getDefault4GSlotId();
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case 27:
                        parcel.enforceInterface(descriptor);
                        setDefaultDataSlotId(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 28:
                        parcel.enforceInterface(descriptor);
                        updateCrurrentPhone(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 29:
                        parcel.enforceInterface(descriptor);
                        _result3 = getSpecCardType(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 30:
                        parcel.enforceInterface(descriptor);
                        _arg1 = isCardUimLocked(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 31:
                        parcel.enforceInterface(descriptor);
                        _arg1 = isRadioOn(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 32:
                        parcel.enforceInterface(descriptor);
                        setPreferredNetworkType(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 33:
                        parcel.enforceInterface(descriptor);
                        _result3 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg2 = (Message) Message.CREATOR.createFromParcel(parcel);
                        }
                        setDefault4GSlotId(_result3, _arg2);
                        reply.writeNoException();
                        return true;
                    case 34:
                        parcel.enforceInterface(descriptor);
                        _result4 = isSetDefault4GSlotIdEnabled();
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 35:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        waitingSetDefault4GSlotDone(_arg1);
                        reply.writeNoException();
                        return true;
                    case 36:
                        parcel.enforceInterface(descriptor);
                        _result4 = getWaitingSwitchBalongSlot();
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 37:
                        parcel.enforceInterface(descriptor);
                        _result5 = getOnDemandDataSubId();
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case 38:
                        parcel.enforceInterface(descriptor);
                        _result5 = getPreferredDataSubscription();
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case 39:
                        parcel.enforceInterface(descriptor);
                        _result = getCdmaGsmImsi();
                        reply.writeNoException();
                        parcel2.writeString(_result);
                        return true;
                    case 40:
                        parcel.enforceInterface(descriptor);
                        _result3 = getUiccCardType(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 41:
                        parcel.enforceInterface(descriptor);
                        Bundle _result8 = getCellLocation(data.readInt());
                        reply.writeNoException();
                        if (_result8 != null) {
                            parcel2.writeInt(1);
                            _result8.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 42:
                        parcel.enforceInterface(descriptor);
                        _result = getCdmaMlplVersion();
                        reply.writeNoException();
                        parcel2.writeString(_result);
                        return true;
                    case 43:
                        parcel.enforceInterface(descriptor);
                        _result = getCdmaMsplVersion();
                        reply.writeNoException();
                        parcel2.writeString(_result);
                        return true;
                    case TRANSACTION_getUniqueDeviceId /*44*/:
                        parcel.enforceInterface(descriptor);
                        _result2 = getUniqueDeviceId(data.readInt());
                        reply.writeNoException();
                        parcel2.writeString(_result2);
                        return true;
                    case TRANSACTION_isLTESupported /*45*/:
                        parcel.enforceInterface(descriptor);
                        _result4 = isLTESupported();
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case TRANSACTION_setISMCOEX /*46*/:
                        parcel.enforceInterface(descriptor);
                        _arg1 = setISMCOEX(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case TRANSACTION_isDomesticCard /*47*/:
                        parcel.enforceInterface(descriptor);
                        _arg1 = isDomesticCard(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 48:
                        parcel.enforceInterface(descriptor);
                        _arg1 = setWifiTxPower(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case TRANSACTION_setCellTxPower /*49*/:
                        parcel.enforceInterface(descriptor);
                        _arg1 = setCellTxPower(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 50:
                        parcel.enforceInterface(descriptor);
                        String[] _result9 = queryServiceCellBand();
                        reply.writeNoException();
                        parcel2.writeStringArray(_result9);
                        return true;
                    case TRANSACTION_registerForRadioAvailable /*51*/:
                        parcel.enforceInterface(descriptor);
                        _arg1 = registerForRadioAvailable(com.android.internal.telephony.IPhoneCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case TRANSACTION_unregisterForRadioAvailable /*52*/:
                        parcel.enforceInterface(descriptor);
                        _arg1 = unregisterForRadioAvailable(com.android.internal.telephony.IPhoneCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case TRANSACTION_registerForRadioNotAvailable /*53*/:
                        parcel.enforceInterface(descriptor);
                        _arg1 = registerForRadioNotAvailable(com.android.internal.telephony.IPhoneCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case TRANSACTION_unregisterForRadioNotAvailable /*54*/:
                        parcel.enforceInterface(descriptor);
                        _arg1 = unregisterForRadioNotAvailable(com.android.internal.telephony.IPhoneCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case TRANSACTION_registerCommonImsaToMapconInfo /*55*/:
                        parcel.enforceInterface(descriptor);
                        _arg1 = registerCommonImsaToMapconInfo(com.android.internal.telephony.IPhoneCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case TRANSACTION_unregisterCommonImsaToMapconInfo /*56*/:
                        parcel.enforceInterface(descriptor);
                        _arg1 = unregisterCommonImsaToMapconInfo(com.android.internal.telephony.IPhoneCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case TRANSACTION_isRadioAvailable /*57*/:
                        parcel.enforceInterface(descriptor);
                        _result4 = isRadioAvailable();
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case TRANSACTION_setImsSwitch /*58*/:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        setImsSwitch(_arg1);
                        reply.writeNoException();
                        return true;
                    case TRANSACTION_getImsSwitch /*59*/:
                        parcel.enforceInterface(descriptor);
                        _result4 = getImsSwitch();
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case TRANSACTION_setImsDomainConfig /*60*/:
                        parcel.enforceInterface(descriptor);
                        setImsDomainConfig(data.readInt());
                        reply.writeNoException();
                        return true;
                    case TRANSACTION_handleMapconImsaReq /*61*/:
                        parcel.enforceInterface(descriptor);
                        _arg1 = handleMapconImsaReq(data.createByteArray());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case TRANSACTION_getUiccAppType /*62*/:
                        parcel.enforceInterface(descriptor);
                        _result5 = getUiccAppType();
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case TRANSACTION_getImsDomain /*63*/:
                        parcel.enforceInterface(descriptor);
                        _result5 = getImsDomain();
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case 64:
                        parcel.enforceInterface(descriptor);
                        UiccAuthResponse _result10 = handleUiccAuth(data.readInt(), data.createByteArray(), data.createByteArray());
                        reply.writeNoException();
                        if (_result10 != null) {
                            parcel2.writeInt(1);
                            _result10.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 65:
                        parcel.enforceInterface(descriptor);
                        _result7 = registerForPhoneEvent(data.readInt(), com.android.internal.telephony.IPhoneCallback.Stub.asInterface(data.readStrongBinder()), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result7);
                        return true;
                    case 66:
                        parcel.enforceInterface(descriptor);
                        unregisterForPhoneEvent(com.android.internal.telephony.IPhoneCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 67:
                        parcel.enforceInterface(descriptor);
                        _arg1 = isRadioAvailableByPhoneId(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 68:
                        parcel.enforceInterface(descriptor);
                        _result5 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        setImsSwitchByPhoneId(_result5, _arg1);
                        reply.writeNoException();
                        return true;
                    case 69:
                        parcel.enforceInterface(descriptor);
                        _arg1 = getImsSwitchByPhoneId(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case TRANSACTION_setImsDomainConfigByPhoneId /*70*/:
                        parcel.enforceInterface(descriptor);
                        setImsDomainConfigByPhoneId(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 71:
                        parcel.enforceInterface(descriptor);
                        _result6 = handleMapconImsaReqByPhoneId(data.readInt(), data.createByteArray());
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return true;
                    case TRANSACTION_getUiccAppTypeByPhoneId /*72*/:
                        parcel.enforceInterface(descriptor);
                        _result3 = getUiccAppTypeByPhoneId(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case TRANSACTION_getImsDomainByPhoneId /*73*/:
                        parcel.enforceInterface(descriptor);
                        _result3 = getImsDomainByPhoneId(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case TRANSACTION_handleUiccAuthByPhoneId /*74*/:
                        parcel.enforceInterface(descriptor);
                        UiccAuthResponse _result11 = handleUiccAuthByPhoneId(data.readInt(), data.readInt(), data.createByteArray(), data.createByteArray());
                        reply.writeNoException();
                        if (_result11 != null) {
                            parcel2.writeInt(1);
                            _result11.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case TRANSACTION_registerForWirelessState /*75*/:
                        parcel.enforceInterface(descriptor);
                        _result7 = registerForWirelessState(data.readInt(), data.readInt(), com.android.internal.telephony.IPhoneCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_result7);
                        return true;
                    case TRANSACTION_unregisterForWirelessState /*76*/:
                        parcel.enforceInterface(descriptor);
                        _result7 = unregisterForWirelessState(data.readInt(), data.readInt(), com.android.internal.telephony.IPhoneCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_result7);
                        return true;
                    case TRANSACTION_setMaxTxPower /*77*/:
                        parcel.enforceInterface(descriptor);
                        _result6 = setMaxTxPower(data.readInt(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return true;
                    case TRANSACTION_cmdForECInfo /*78*/:
                        parcel.enforceInterface(descriptor);
                        _result5 = data.readInt();
                        _result3 = data.readInt();
                        _arg22 = data.createByteArray();
                        _result7 = cmdForECInfo(_result5, _result3, _arg22);
                        reply.writeNoException();
                        parcel2.writeInt(_result7);
                        parcel2.writeByteArray(_arg22);
                        return true;
                    case TRANSACTION_isCtSimCard /*79*/:
                        parcel.enforceInterface(descriptor);
                        _arg1 = isCtSimCard(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 80:
                        parcel.enforceInterface(descriptor);
                        notifyCModemStatus(data.readInt(), com.android.internal.telephony.IPhoneCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 81:
                        parcel.enforceInterface(descriptor);
                        _result7 = notifyDeviceState(data.readString(), data.readString(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result7);
                        return true;
                    case 82:
                        parcel.enforceInterface(descriptor);
                        _result3 = data.readInt();
                        _arg12 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg2 = (Message) Message.CREATOR.createFromParcel(parcel);
                        }
                        notifyCellularCommParaReady(_result3, _arg12, _arg2);
                        reply.writeNoException();
                        return true;
                    case 83:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        _result7 = setPinLockEnabled(_arg1, data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result7);
                        return true;
                    case 84:
                        parcel.enforceInterface(descriptor);
                        _result7 = changeSimPinCode(data.readString(), data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result7);
                        return true;
                    case 85:
                        parcel.enforceInterface(descriptor);
                        _result4 = sendPseudocellCellInfo(data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 86:
                        parcel.enforceInterface(descriptor);
                        _result3 = data.readInt();
                        _arg13 = data.readString();
                        if (data.readInt() != 0) {
                            _arg2 = (Message) Message.CREATOR.createFromParcel(parcel);
                        }
                        _result7 = sendLaaCmd(_result3, _arg13, _arg2);
                        reply.writeNoException();
                        parcel2.writeInt(_result7);
                        return true;
                    case 87:
                        parcel.enforceInterface(descriptor);
                        _result2 = data.readString();
                        if (data.readInt() != 0) {
                            _arg2 = (Message) Message.CREATOR.createFromParcel(parcel);
                        }
                        _result6 = getLaaDetailedState(_result2, _arg2);
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return true;
                    case TRANSACTION_registerForCallAltSrv /*88*/:
                        parcel.enforceInterface(descriptor);
                        registerForCallAltSrv(data.readInt(), com.android.internal.telephony.IPhoneCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case TRANSACTION_unregisterForCallAltSrv /*89*/:
                        parcel.enforceInterface(descriptor);
                        unregisterForCallAltSrv(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 90:
                        parcel.enforceInterface(descriptor);
                        _result5 = data.readInt();
                        byte[] _arg14 = data.createByteArray();
                        _arg22 = data.createByteArray();
                        int _result12 = invokeOemRilRequestRaw(_result5, _arg14, _arg22);
                        reply.writeNoException();
                        parcel2.writeInt(_result12);
                        parcel2.writeByteArray(_arg22);
                        return true;
                    case 91:
                        parcel.enforceInterface(descriptor);
                        _arg1 = isCspPlmnEnabled(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 92:
                        parcel.enforceInterface(descriptor);
                        int _arg0 = data.readInt();
                        int _arg15 = data.readInt();
                        int _arg23 = data.readInt();
                        String _arg3 = data.readString();
                        int _arg4 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg2 = (Message) Message.CREATOR.createFromParcel(parcel);
                        }
                        setCallForwardingOption(_arg0, _arg15, _arg23, _arg3, _arg4, _arg2);
                        reply.writeNoException();
                        return true;
                    case 93:
                        parcel.enforceInterface(descriptor);
                        _result3 = data.readInt();
                        _arg12 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg2 = (Message) Message.CREATOR.createFromParcel(parcel);
                        }
                        getCallForwardingOption(_result3, _arg12, _arg2);
                        reply.writeNoException();
                        return true;
                    case 94:
                        parcel.enforceInterface(descriptor);
                        _arg12 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        if (data.readInt() != 0) {
                            _arg2 = (Message) Message.CREATOR.createFromParcel(parcel);
                        }
                        _result7 = setSubscription(_arg12, _arg1, _arg2);
                        reply.writeNoException();
                        parcel2.writeInt(_result7);
                        return true;
                    case 95:
                        parcel.enforceInterface(descriptor);
                        _result2 = getImsImpu(data.readInt());
                        reply.writeNoException();
                        parcel2.writeString(_result2);
                        return true;
                    case 96:
                        parcel.enforceInterface(descriptor);
                        _result2 = getLine1NumberFromImpu(data.readInt());
                        reply.writeNoException();
                        parcel2.writeString(_result2);
                        return true;
                    case 97:
                        parcel.enforceInterface(descriptor);
                        _result4 = isSecondaryCardGsmOnly();
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 98:
                        parcel.enforceInterface(descriptor);
                        _arg1 = bindSimToProfile(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 99:
                        parcel.enforceInterface(descriptor);
                        _result2 = getCdmaGsmImsiForSubId(data.readInt());
                        reply.writeNoException();
                        parcel2.writeString(_result2);
                        return true;
                    case 100:
                        parcel.enforceInterface(descriptor);
                        _result3 = data.readInt();
                        _arg13 = data.readString();
                        String _arg24 = data.readString();
                        if (data.readInt() != 0) {
                            _arg2 = (Message) Message.CREATOR.createFromParcel(parcel);
                        }
                        boolean _result13 = setLine1Number(_result3, _arg13, _arg24, _arg2);
                        reply.writeNoException();
                        parcel2.writeInt(_result13);
                        return true;
                    case 101:
                        parcel.enforceInterface(descriptor);
                        _result6 = setDeepNoDisturbState(data.readInt(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return true;
                    case 102:
                        parcel.enforceInterface(descriptor);
                        informModemTetherStatusToChangeGRO(data.readInt(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 103:
                        parcel.enforceInterface(descriptor);
                        _result4 = sendSimMatchedOperatorInfo(data.readInt(), data.readString(), data.readString(), data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 104:
                        parcel.enforceInterface(descriptor);
                        _arg1 = is4RMimoEnabled(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case TRANSACTION_getAntiFakeBaseStation /*105*/:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg2 = (Message) Message.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = getAntiFakeBaseStation(_arg2);
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 106:
                        parcel.enforceInterface(descriptor);
                        _arg1 = registerForAntiFakeBaseStation(com.android.internal.telephony.IPhoneCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case TRANSACTION_unregisterForAntiFakeBaseStation /*107*/:
                        parcel.enforceInterface(descriptor);
                        _result4 = unregisterForAntiFakeBaseStation();
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case TRANSACTION_getCardTrayInfo /*108*/:
                        parcel.enforceInterface(descriptor);
                        byte[] _result14 = getCardTrayInfo();
                        reply.writeNoException();
                        parcel2.writeByteArray(_result14);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            parcel2.writeString(descriptor);
            return true;
        }
    }

    boolean bindSimToProfile(int i) throws RemoteException;

    boolean changeSimPinCode(String str, String str2, int i) throws RemoteException;

    void closeRrc() throws RemoteException;

    boolean cmdForECInfo(int i, int i2, byte[] bArr) throws RemoteException;

    int get2GServiceAbility() throws RemoteException;

    boolean getAntiFakeBaseStation(Message message) throws RemoteException;

    void getCallForwardingOption(int i, int i2, Message message) throws RemoteException;

    byte[] getCardTrayInfo() throws RemoteException;

    String getCdmaGsmImsi() throws RemoteException;

    String getCdmaGsmImsiForSubId(int i) throws RemoteException;

    String getCdmaMlplVersion() throws RemoteException;

    String getCdmaMsplVersion() throws RemoteException;

    Bundle getCellLocation(int i) throws RemoteException;

    int getDataStateForSubscriber(int i) throws RemoteException;

    int getDefault4GSlotId() throws RemoteException;

    String getDemoString() throws RemoteException;

    int getImsDomain() throws RemoteException;

    int getImsDomainByPhoneId(int i) throws RemoteException;

    String getImsImpu(int i) throws RemoteException;

    boolean getImsSwitch() throws RemoteException;

    boolean getImsSwitchByPhoneId(int i) throws RemoteException;

    boolean getLaaDetailedState(String str, Message message) throws RemoteException;

    String getLine1NumberFromImpu(int i) throws RemoteException;

    int getLteServiceAbility() throws RemoteException;

    int getLteServiceAbilityForSubId(int i) throws RemoteException;

    String getMeidForSubscriber(int i) throws RemoteException;

    String getNVESN() throws RemoteException;

    int getOnDemandDataSubId() throws RemoteException;

    String getPesnForSubscriber(int i) throws RemoteException;

    int getPreferredDataSubscription() throws RemoteException;

    int getSpecCardType(int i) throws RemoteException;

    int getSubState(int i) throws RemoteException;

    int getUiccAppType() throws RemoteException;

    int getUiccAppTypeByPhoneId(int i) throws RemoteException;

    int getUiccCardType(int i) throws RemoteException;

    String getUniqueDeviceId(int i) throws RemoteException;

    boolean getWaitingSwitchBalongSlot() throws RemoteException;

    boolean handleMapconImsaReq(byte[] bArr) throws RemoteException;

    boolean handleMapconImsaReqByPhoneId(int i, byte[] bArr) throws RemoteException;

    UiccAuthResponse handleUiccAuth(int i, byte[] bArr, byte[] bArr2) throws RemoteException;

    UiccAuthResponse handleUiccAuthByPhoneId(int i, int i2, byte[] bArr, byte[] bArr2) throws RemoteException;

    void informModemTetherStatusToChangeGRO(int i, String str) throws RemoteException;

    int invokeOemRilRequestRaw(int i, byte[] bArr, byte[] bArr2) throws RemoteException;

    boolean is4RMimoEnabled(int i) throws RemoteException;

    boolean isCTCdmaCardInGsmMode() throws RemoteException;

    boolean isCardUimLocked(int i) throws RemoteException;

    boolean isCspPlmnEnabled(int i) throws RemoteException;

    boolean isCtSimCard(int i) throws RemoteException;

    boolean isDomesticCard(int i) throws RemoteException;

    boolean isImsRegisteredForSubId(int i) throws RemoteException;

    boolean isLTESupported() throws RemoteException;

    boolean isNeedToRadioPowerOn(long j) throws RemoteException;

    boolean isRadioAvailable() throws RemoteException;

    boolean isRadioAvailableByPhoneId(int i) throws RemoteException;

    boolean isRadioOn(int i) throws RemoteException;

    boolean isSecondaryCardGsmOnly() throws RemoteException;

    boolean isSetDefault4GSlotIdEnabled() throws RemoteException;

    boolean isSubDeactivedByPowerOff(long j) throws RemoteException;

    boolean isVideoTelephonyAvailableForSubId(int i) throws RemoteException;

    boolean isVolteAvailableForSubId(int i) throws RemoteException;

    boolean isWifiCallingAvailableForSubId(int i) throws RemoteException;

    void notifyCModemStatus(int i, IPhoneCallback iPhoneCallback) throws RemoteException;

    void notifyCellularCommParaReady(int i, int i2, Message message) throws RemoteException;

    boolean notifyDeviceState(String str, String str2, String str3) throws RemoteException;

    String[] queryServiceCellBand() throws RemoteException;

    boolean registerCommonImsaToMapconInfo(IPhoneCallback iPhoneCallback) throws RemoteException;

    boolean registerForAntiFakeBaseStation(IPhoneCallback iPhoneCallback) throws RemoteException;

    void registerForCallAltSrv(int i, IPhoneCallback iPhoneCallback) throws RemoteException;

    boolean registerForPhoneEvent(int i, IPhoneCallback iPhoneCallback, int i2) throws RemoteException;

    boolean registerForRadioAvailable(IPhoneCallback iPhoneCallback) throws RemoteException;

    boolean registerForRadioNotAvailable(IPhoneCallback iPhoneCallback) throws RemoteException;

    boolean registerForWirelessState(int i, int i2, IPhoneCallback iPhoneCallback) throws RemoteException;

    boolean sendLaaCmd(int i, String str, Message message) throws RemoteException;

    boolean sendPseudocellCellInfo(int i, int i2, int i3, int i4, String str, int i5) throws RemoteException;

    boolean sendSimMatchedOperatorInfo(int i, String str, String str2, int i2, String str3) throws RemoteException;

    void set2GServiceAbility(int i) throws RemoteException;

    void setCallForwardingOption(int i, int i2, int i3, String str, int i4, Message message) throws RemoteException;

    boolean setCellTxPower(int i) throws RemoteException;

    void setDataEnabledWithoutPromp(boolean z) throws RemoteException;

    void setDataRoamingEnabledWithoutPromp(boolean z) throws RemoteException;

    boolean setDeepNoDisturbState(int i, int i2) throws RemoteException;

    void setDefault4GSlotId(int i, Message message) throws RemoteException;

    void setDefaultDataSlotId(int i) throws RemoteException;

    void setDefaultMobileEnable(boolean z) throws RemoteException;

    boolean setISMCOEX(String str) throws RemoteException;

    void setImsDomainConfig(int i) throws RemoteException;

    void setImsDomainConfigByPhoneId(int i, int i2) throws RemoteException;

    void setImsRegistrationStateForSubId(int i, boolean z) throws RemoteException;

    void setImsSwitch(boolean z) throws RemoteException;

    void setImsSwitchByPhoneId(int i, boolean z) throws RemoteException;

    boolean setLine1Number(int i, String str, String str2, Message message) throws RemoteException;

    void setLteServiceAbility(int i) throws RemoteException;

    void setLteServiceAbilityForSubId(int i, int i2) throws RemoteException;

    boolean setMaxTxPower(int i, int i2) throws RemoteException;

    boolean setPinLockEnabled(boolean z, String str, int i) throws RemoteException;

    void setPreferredNetworkType(int i) throws RemoteException;

    boolean setSubscription(int i, boolean z, Message message) throws RemoteException;

    void setUserPrefDataSlotId(int i) throws RemoteException;

    boolean setWifiTxPower(int i) throws RemoteException;

    boolean unregisterCommonImsaToMapconInfo(IPhoneCallback iPhoneCallback) throws RemoteException;

    boolean unregisterForAntiFakeBaseStation() throws RemoteException;

    void unregisterForCallAltSrv(int i) throws RemoteException;

    void unregisterForPhoneEvent(IPhoneCallback iPhoneCallback) throws RemoteException;

    boolean unregisterForRadioAvailable(IPhoneCallback iPhoneCallback) throws RemoteException;

    boolean unregisterForRadioNotAvailable(IPhoneCallback iPhoneCallback) throws RemoteException;

    boolean unregisterForWirelessState(int i, int i2, IPhoneCallback iPhoneCallback) throws RemoteException;

    void updateCrurrentPhone(int i) throws RemoteException;

    void waitingSetDefault4GSlotDone(boolean z) throws RemoteException;
}
