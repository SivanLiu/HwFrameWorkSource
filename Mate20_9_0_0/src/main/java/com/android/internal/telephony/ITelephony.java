package com.android.internal.telephony;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.NetworkStats;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Messenger;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.service.carrier.CarrierIdentifier;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telephony.CellInfo;
import android.telephony.ClientRequestStats;
import android.telephony.IccOpenLogicalChannelResponse;
import android.telephony.NeighboringCellInfo;
import android.telephony.NetworkScanRequest;
import android.telephony.RadioAccessFamily;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyHistogram;
import android.telephony.UiccSlotInfo;
import android.telephony.VisualVoicemailSmsFilterSettings;
import android.telephony.ims.aidl.IImsConfig;
import android.telephony.ims.aidl.IImsMmTelFeature;
import android.telephony.ims.aidl.IImsRcsFeature;
import android.telephony.ims.aidl.IImsRegistration;
import com.android.ims.internal.IImsServiceFeatureCallback;
import java.util.List;

public interface ITelephony extends IInterface {

    public static abstract class Stub extends Binder implements ITelephony {
        private static final String DESCRIPTOR = "com.android.internal.telephony.ITelephony";
        static final int TRANSACTION_answerRingingCall = 5;
        static final int TRANSACTION_answerRingingCallForSubscriber = 6;
        static final int TRANSACTION_call = 2;
        static final int TRANSACTION_canChangeDtmfToneLength = 140;
        static final int TRANSACTION_carrierActionReportDefaultNetworkStatus = 173;
        static final int TRANSACTION_carrierActionSetMeteredApnsEnabled = 171;
        static final int TRANSACTION_carrierActionSetRadioEnabled = 172;
        static final int TRANSACTION_checkCarrierPrivilegesForPackage = 124;
        static final int TRANSACTION_checkCarrierPrivilegesForPackageAnyPhone = 125;
        static final int TRANSACTION_dial = 1;
        static final int TRANSACTION_disableDataConnectivity = 39;
        static final int TRANSACTION_disableIms = 100;
        static final int TRANSACTION_disableLocationUpdates = 36;
        static final int TRANSACTION_disableLocationUpdatesForSubscriber = 37;
        static final int TRANSACTION_disableVisualVoicemailSmsFilter = 68;
        static final int TRANSACTION_enableDataConnectivity = 38;
        static final int TRANSACTION_enableIms = 99;
        static final int TRANSACTION_enableLocationUpdates = 34;
        static final int TRANSACTION_enableLocationUpdatesForSubscriber = 35;
        static final int TRANSACTION_enableVideoCalling = 138;
        static final int TRANSACTION_enableVisualVoicemailSmsFilter = 67;
        static final int TRANSACTION_endCall = 3;
        static final int TRANSACTION_endCallForSubscriber = 4;
        static final int TRANSACTION_factoryReset = 154;
        static final int TRANSACTION_getActivePhoneType = 48;
        static final int TRANSACTION_getActivePhoneTypeForSlot = 49;
        static final int TRANSACTION_getActiveVisualVoicemailSmsFilterSettings = 70;
        static final int TRANSACTION_getAidForAppType = 163;
        static final int TRANSACTION_getAllCellInfo = 82;
        static final int TRANSACTION_getAllowedCarriers = 168;
        static final int TRANSACTION_getCalculatedPreferredNetworkType = 96;
        static final int TRANSACTION_getCallState = 44;
        static final int TRANSACTION_getCallStateForSlot = 45;
        static final int TRANSACTION_getCarrierIdListVersion = 186;
        static final int TRANSACTION_getCarrierPackageNamesForIntentAndPhone = 126;
        static final int TRANSACTION_getCarrierPrivilegeStatus = 122;
        static final int TRANSACTION_getCarrierPrivilegeStatusForUid = 123;
        static final int TRANSACTION_getCdmaEriIconIndex = 50;
        static final int TRANSACTION_getCdmaEriIconIndexForSubscriber = 51;
        static final int TRANSACTION_getCdmaEriIconMode = 52;
        static final int TRANSACTION_getCdmaEriIconModeForSubscriber = 53;
        static final int TRANSACTION_getCdmaEriText = 54;
        static final int TRANSACTION_getCdmaEriTextForSubscriber = 55;
        static final int TRANSACTION_getCdmaMdn = 120;
        static final int TRANSACTION_getCdmaMin = 121;
        static final int TRANSACTION_getCdmaPrlVersion = 165;
        static final int TRANSACTION_getCellLocation = 41;
        static final int TRANSACTION_getCellNetworkScanResults = 109;
        static final int TRANSACTION_getClientRequestStats = 176;
        static final int TRANSACTION_getDataActivationState = 61;
        static final int TRANSACTION_getDataActivity = 46;
        static final int TRANSACTION_getDataEnabled = 115;
        static final int TRANSACTION_getDataNetworkType = 75;
        static final int TRANSACTION_getDataNetworkTypeForSubscriber = 76;
        static final int TRANSACTION_getDataState = 47;
        static final int TRANSACTION_getDefaultSim = 84;
        static final int TRANSACTION_getDeviceId = 149;
        static final int TRANSACTION_getDeviceSoftwareVersionForSlot = 152;
        static final int TRANSACTION_getEmergencyCallbackMode = 180;
        static final int TRANSACTION_getEsn = 164;
        static final int TRANSACTION_getForbiddenPlmns = 179;
        static final int TRANSACTION_getImeiForSlot = 150;
        static final int TRANSACTION_getImsConfig = 104;
        static final int TRANSACTION_getImsRegTechnologyForMmTel = 148;
        static final int TRANSACTION_getImsRegistration = 103;
        static final int TRANSACTION_getImsService = 107;
        static final int TRANSACTION_getLine1AlphaTagForDisplay = 129;
        static final int TRANSACTION_getLine1NumberForDisplay = 128;
        static final int TRANSACTION_getLocaleFromDefaultSim = 155;
        static final int TRANSACTION_getLteOnCdmaMode = 80;
        static final int TRANSACTION_getLteOnCdmaModeForSubscriber = 81;
        static final int TRANSACTION_getMeidForSlot = 151;
        static final int TRANSACTION_getMergedSubscriberIds = 130;
        static final int TRANSACTION_getMmTelFeatureAndListen = 101;
        static final int TRANSACTION_getNeighboringCellInfo = 43;
        static final int TRANSACTION_getNetworkCountryIsoForPhone = 42;
        static final int TRANSACTION_getNetworkType = 73;
        static final int TRANSACTION_getNetworkTypeForSubscriber = 74;
        static final int TRANSACTION_getPackagesWithCarrierPrivileges = 162;
        static final int TRANSACTION_getPcscfAddress = 118;
        static final int TRANSACTION_getPreferredNetworkType = 97;
        static final int TRANSACTION_getRadioAccessFamily = 137;
        static final int TRANSACTION_getRcsFeatureAndListen = 102;
        static final int TRANSACTION_getServiceStateForSubscriber = 157;
        static final int TRANSACTION_getSignalStrength = 181;
        static final int TRANSACTION_getSubIdForPhoneAccount = 153;
        static final int TRANSACTION_getSubscriptionCarrierId = 169;
        static final int TRANSACTION_getSubscriptionCarrierName = 170;
        static final int TRANSACTION_getTelephonyHistograms = 166;
        static final int TRANSACTION_getTetherApnRequired = 98;
        static final int TRANSACTION_getUiccSlotsInfo = 182;
        static final int TRANSACTION_getVisualVoicemailPackageName = 66;
        static final int TRANSACTION_getVisualVoicemailSettings = 65;
        static final int TRANSACTION_getVisualVoicemailSmsFilterSettings = 69;
        static final int TRANSACTION_getVoiceActivationState = 60;
        static final int TRANSACTION_getVoiceMessageCount = 62;
        static final int TRANSACTION_getVoiceMessageCountForSubscriber = 63;
        static final int TRANSACTION_getVoiceNetworkTypeForSubscriber = 77;
        static final int TRANSACTION_getVoicemailRingtoneUri = 158;
        static final int TRANSACTION_getVtDataUsage = 174;
        static final int TRANSACTION_handlePinMmi = 24;
        static final int TRANSACTION_handlePinMmiForSubscriber = 26;
        static final int TRANSACTION_handleUssdRequest = 25;
        static final int TRANSACTION_hasIccCard = 78;
        static final int TRANSACTION_hasIccCardUsingSlotIndex = 79;
        static final int TRANSACTION_iccCloseLogicalChannel = 87;
        static final int TRANSACTION_iccExchangeSimIO = 90;
        static final int TRANSACTION_iccOpenLogicalChannel = 85;
        static final int TRANSACTION_iccOpenLogicalChannelP2 = 86;
        static final int TRANSACTION_iccTransmitApduBasicChannel = 89;
        static final int TRANSACTION_iccTransmitApduLogicalChannel = 88;
        static final int TRANSACTION_invokeOemRilRequestRaw = 133;
        static final int TRANSACTION_isConcurrentVoiceAndDataAllowed = 64;
        static final int TRANSACTION_isDataConnectivityPossible = 40;
        static final int TRANSACTION_isDataEnabled = 117;
        static final int TRANSACTION_isHearingAidCompatibilitySupported = 143;
        static final int TRANSACTION_isIdle = 12;
        static final int TRANSACTION_isIdleForSubscriber = 13;
        static final int TRANSACTION_isImsRegistered = 144;
        static final int TRANSACTION_isOffhook = 8;
        static final int TRANSACTION_isOffhookForSubscriber = 9;
        static final int TRANSACTION_isRadioOn = 14;
        static final int TRANSACTION_isRadioOnForSubscriber = 15;
        static final int TRANSACTION_isResolvingImsBinding = 105;
        static final int TRANSACTION_isRinging = 11;
        static final int TRANSACTION_isRingingForSubscriber = 10;
        static final int TRANSACTION_isTtyModeSupported = 142;
        static final int TRANSACTION_isUserDataEnabled = 116;
        static final int TRANSACTION_isVideoCallingEnabled = 139;
        static final int TRANSACTION_isVideoTelephonyAvailable = 147;
        static final int TRANSACTION_isVoicemailVibrationEnabled = 160;
        static final int TRANSACTION_isVolteAvailable = 146;
        static final int TRANSACTION_isWifiCallingAvailable = 145;
        static final int TRANSACTION_isWorldPhone = 141;
        static final int TRANSACTION_needMobileRadioShutdown = 134;
        static final int TRANSACTION_needsOtaServiceProvisioning = 56;
        static final int TRANSACTION_nvReadItem = 92;
        static final int TRANSACTION_nvResetConfig = 95;
        static final int TRANSACTION_nvWriteCdmaPrl = 94;
        static final int TRANSACTION_nvWriteItem = 93;
        static final int TRANSACTION_refreshUiccProfile = 187;
        static final int TRANSACTION_requestModemActivityInfo = 156;
        static final int TRANSACTION_requestNetworkScan = 110;
        static final int TRANSACTION_sendDialerSpecialCode = 72;
        static final int TRANSACTION_sendEnvelopeWithStatus = 91;
        static final int TRANSACTION_sendVisualVoicemailSmsForSubscriber = 71;
        static final int TRANSACTION_setAllowedCarriers = 167;
        static final int TRANSACTION_setCarrierTestOverride = 185;
        static final int TRANSACTION_setCellInfoListRate = 83;
        static final int TRANSACTION_setDataActivationState = 59;
        static final int TRANSACTION_setDataEnabledProperties = 177;
        static final int TRANSACTION_setImsRegistrationState = 119;
        static final int TRANSACTION_setImsService = 106;
        static final int TRANSACTION_setLine1NumberForDisplayForSubscriber = 127;
        static final int TRANSACTION_setNetworkSelectionModeAutomatic = 108;
        static final int TRANSACTION_setNetworkSelectionModeManual = 112;
        static final int TRANSACTION_setOperatorBrandOverride = 131;
        static final int TRANSACTION_setPolicyDataEnabled = 175;
        static final int TRANSACTION_setPreferredNetworkType = 113;
        static final int TRANSACTION_setRadio = 29;
        static final int TRANSACTION_setRadioCapability = 136;
        static final int TRANSACTION_setRadioForSubscriber = 30;
        static final int TRANSACTION_setRadioIndicationUpdateMode = 184;
        static final int TRANSACTION_setRadioPower = 31;
        static final int TRANSACTION_setRoamingOverride = 132;
        static final int TRANSACTION_setSimPowerStateForSlot = 178;
        static final int TRANSACTION_setUserDataEnabled = 114;
        static final int TRANSACTION_setVoiceActivationState = 58;
        static final int TRANSACTION_setVoiceMailNumber = 57;
        static final int TRANSACTION_setVoicemailRingtoneUri = 159;
        static final int TRANSACTION_setVoicemailVibrationEnabled = 161;
        static final int TRANSACTION_shutdownMobileRadios = 135;
        static final int TRANSACTION_silenceRinger = 7;
        static final int TRANSACTION_stopNetworkScan = 111;
        static final int TRANSACTION_supplyPin = 16;
        static final int TRANSACTION_supplyPinForSubscriber = 17;
        static final int TRANSACTION_supplyPinReportResult = 20;
        static final int TRANSACTION_supplyPinReportResultForSubscriber = 21;
        static final int TRANSACTION_supplyPuk = 18;
        static final int TRANSACTION_supplyPukForSubscriber = 19;
        static final int TRANSACTION_supplyPukReportResult = 22;
        static final int TRANSACTION_supplyPukReportResultForSubscriber = 23;
        static final int TRANSACTION_switchSlots = 183;
        static final int TRANSACTION_toggleRadioOnOff = 27;
        static final int TRANSACTION_toggleRadioOnOffForSubscriber = 28;
        static final int TRANSACTION_updateServiceLocation = 32;
        static final int TRANSACTION_updateServiceLocationForSubscriber = 33;

        private static class Proxy implements ITelephony {
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

            public void dial(String number) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(number);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void call(String callingPackage, String number) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeString(number);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean endCall() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
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

            public boolean endCallForSubscriber(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
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

            public void answerRingingCall() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void answerRingingCallForSubscriber(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void silenceRinger() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isOffhook(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
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

            public boolean isOffhookForSubscriber(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
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

            public boolean isRingingForSubscriber(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
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

            public boolean isRinging(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    boolean z = false;
                    this.mRemote.transact(11, _data, _reply, 0);
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

            public boolean isIdle(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
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

            public boolean isIdleForSubscriber(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
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

            public boolean isRadioOn(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
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

            public boolean isRadioOnForSubscriber(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
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

            public boolean supplyPin(String pin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pin);
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

            public boolean supplyPinForSubscriber(int subId, String pin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(pin);
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

            public boolean supplyPuk(String puk, String pin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(puk);
                    _data.writeString(pin);
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

            public boolean supplyPukForSubscriber(int subId, String puk, String pin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(puk);
                    _data.writeString(pin);
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

            public int[] supplyPinReportResult(String pin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pin);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int[] supplyPinReportResultForSubscriber(int subId, String pin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(pin);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int[] supplyPukReportResult(String puk, String pin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(puk);
                    _data.writeString(pin);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int[] supplyPukReportResultForSubscriber(int subId, String puk, String pin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(puk);
                    _data.writeString(pin);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean handlePinMmi(String dialString) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(dialString);
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

            public void handleUssdRequest(int subId, String ussdRequest, ResultReceiver wrappedCallback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(ussdRequest);
                    if (wrappedCallback != null) {
                        _data.writeInt(1);
                        wrappedCallback.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean handlePinMmiForSubscriber(int subId, String dialString) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(dialString);
                    boolean z = false;
                    this.mRemote.transact(26, _data, _reply, 0);
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

            public void toggleRadioOnOff() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void toggleRadioOnOffForSubscriber(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setRadio(boolean turnOn) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(turnOn);
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

            public boolean setRadioForSubscriber(int subId, boolean turnOn) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(turnOn);
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

            public boolean setRadioPower(boolean turnOn) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(turnOn);
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

            public void updateServiceLocation() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void updateServiceLocationForSubscriber(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void enableLocationUpdates() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void enableLocationUpdatesForSubscriber(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void disableLocationUpdates() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void disableLocationUpdatesForSubscriber(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean enableDataConnectivity() throws RemoteException {
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

            public boolean disableDataConnectivity() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(39, _data, _reply, 0);
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

            public boolean isDataConnectivityPossible(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
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

            public Bundle getCellLocation(String callingPkg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Bundle _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPkg);
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

            public String getNetworkCountryIsoForPhone(int phoneId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(phoneId);
                    this.mRemote.transact(42, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<NeighboringCellInfo> getNeighboringCellInfo(String callingPkg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPkg);
                    this.mRemote.transact(43, _data, _reply, 0);
                    _reply.readException();
                    List<NeighboringCellInfo> _result = _reply.createTypedArrayList(NeighboringCellInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getCallState() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(44, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getCallStateForSlot(int slotIndex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    this.mRemote.transact(45, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getDataActivity() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(46, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getDataState() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(47, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getActivePhoneType() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(48, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getActivePhoneTypeForSlot(int slotIndex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    this.mRemote.transact(49, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getCdmaEriIconIndex(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(50, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getCdmaEriIconIndexForSubscriber(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(51, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getCdmaEriIconMode(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(52, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getCdmaEriIconModeForSubscriber(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(53, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getCdmaEriText(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(54, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getCdmaEriTextForSubscriber(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(55, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean needsOtaServiceProvisioning() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(56, _data, _reply, 0);
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

            public boolean setVoiceMailNumber(int subId, String alphaTag, String number) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(alphaTag);
                    _data.writeString(number);
                    boolean z = false;
                    this.mRemote.transact(57, _data, _reply, 0);
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

            public void setVoiceActivationState(int subId, int activationState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(activationState);
                    this.mRemote.transact(58, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setDataActivationState(int subId, int activationState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(activationState);
                    this.mRemote.transact(59, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getVoiceActivationState(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(60, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getDataActivationState(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(61, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getVoiceMessageCount() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(62, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getVoiceMessageCountForSubscriber(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(63, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isConcurrentVoiceAndDataAllowed(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    boolean z = false;
                    this.mRemote.transact(64, _data, _reply, 0);
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

            public Bundle getVisualVoicemailSettings(String callingPackage, int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Bundle _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(subId);
                    this.mRemote.transact(65, _data, _reply, 0);
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

            public String getVisualVoicemailPackageName(String callingPackage, int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(subId);
                    this.mRemote.transact(66, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void enableVisualVoicemailSmsFilter(String callingPackage, int subId, VisualVoicemailSmsFilterSettings settings) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(subId);
                    if (settings != null) {
                        _data.writeInt(1);
                        settings.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(67, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void disableVisualVoicemailSmsFilter(String callingPackage, int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(subId);
                    this.mRemote.transact(68, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public VisualVoicemailSmsFilterSettings getVisualVoicemailSmsFilterSettings(String callingPackage, int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    VisualVoicemailSmsFilterSettings _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(subId);
                    this.mRemote.transact(69, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (VisualVoicemailSmsFilterSettings) VisualVoicemailSmsFilterSettings.CREATOR.createFromParcel(_reply);
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

            public VisualVoicemailSmsFilterSettings getActiveVisualVoicemailSmsFilterSettings(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    VisualVoicemailSmsFilterSettings _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(70, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (VisualVoicemailSmsFilterSettings) VisualVoicemailSmsFilterSettings.CREATOR.createFromParcel(_reply);
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

            public void sendVisualVoicemailSmsForSubscriber(String callingPackage, int subId, String number, int port, String text, PendingIntent sentIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(subId);
                    _data.writeString(number);
                    _data.writeInt(port);
                    _data.writeString(text);
                    if (sentIntent != null) {
                        _data.writeInt(1);
                        sentIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(71, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void sendDialerSpecialCode(String callingPackageName, String inputCode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackageName);
                    _data.writeString(inputCode);
                    this.mRemote.transact(72, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getNetworkType() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(73, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getNetworkTypeForSubscriber(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(74, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getDataNetworkType(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(75, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getDataNetworkTypeForSubscriber(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(76, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getVoiceNetworkTypeForSubscriber(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(77, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean hasIccCard() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(78, _data, _reply, 0);
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

            public boolean hasIccCardUsingSlotIndex(int slotIndex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    boolean z = false;
                    this.mRemote.transact(79, _data, _reply, 0);
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

            public int getLteOnCdmaMode(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(80, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getLteOnCdmaModeForSubscriber(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(81, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<CellInfo> getAllCellInfo(String callingPkg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPkg);
                    this.mRemote.transact(82, _data, _reply, 0);
                    _reply.readException();
                    List<CellInfo> _result = _reply.createTypedArrayList(CellInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setCellInfoListRate(int rateInMillis) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(rateInMillis);
                    this.mRemote.transact(83, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getDefaultSim() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(84, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IccOpenLogicalChannelResponse iccOpenLogicalChannel(int subId, String callingPackage, String AID, int p2) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    IccOpenLogicalChannelResponse _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeString(AID);
                    _data.writeInt(p2);
                    this.mRemote.transact(85, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (IccOpenLogicalChannelResponse) IccOpenLogicalChannelResponse.CREATOR.createFromParcel(_reply);
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

            public IccOpenLogicalChannelResponse iccOpenLogicalChannelP2(int subId, String AID, byte p2) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    IccOpenLogicalChannelResponse _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(AID);
                    _data.writeByte(p2);
                    this.mRemote.transact(86, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (IccOpenLogicalChannelResponse) IccOpenLogicalChannelResponse.CREATOR.createFromParcel(_reply);
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

            public boolean iccCloseLogicalChannel(int subId, int channel) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(channel);
                    boolean z = false;
                    this.mRemote.transact(87, _data, _reply, 0);
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

            public String iccTransmitApduLogicalChannel(int subId, int channel, int cla, int instruction, int p1, int p2, int p3, String data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(channel);
                    _data.writeInt(cla);
                    _data.writeInt(instruction);
                    _data.writeInt(p1);
                    _data.writeInt(p2);
                    _data.writeInt(p3);
                    _data.writeString(data);
                    this.mRemote.transact(88, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String iccTransmitApduBasicChannel(int subId, String callingPackage, int cla, int instruction, int p1, int p2, int p3, String data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    _data.writeInt(cla);
                    _data.writeInt(instruction);
                    _data.writeInt(p1);
                    _data.writeInt(p2);
                    _data.writeInt(p3);
                    _data.writeString(data);
                    this.mRemote.transact(89, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public byte[] iccExchangeSimIO(int subId, int fileID, int command, int p1, int p2, int p3, String filePath) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(fileID);
                    _data.writeInt(command);
                    _data.writeInt(p1);
                    _data.writeInt(p2);
                    _data.writeInt(p3);
                    _data.writeString(filePath);
                    this.mRemote.transact(90, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String sendEnvelopeWithStatus(int subId, String content) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(content);
                    this.mRemote.transact(91, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String nvReadItem(int itemID) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(itemID);
                    this.mRemote.transact(92, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean nvWriteItem(int itemID, String itemValue) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(itemID);
                    _data.writeString(itemValue);
                    boolean z = false;
                    this.mRemote.transact(93, _data, _reply, 0);
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

            public boolean nvWriteCdmaPrl(byte[] preferredRoamingList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(preferredRoamingList);
                    boolean z = false;
                    this.mRemote.transact(94, _data, _reply, 0);
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

            public boolean nvResetConfig(int resetType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(resetType);
                    boolean z = false;
                    this.mRemote.transact(95, _data, _reply, 0);
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

            public int getCalculatedPreferredNetworkType(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(96, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getPreferredNetworkType(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(97, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getTetherApnRequired() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(98, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void enableIms(int slotId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    this.mRemote.transact(99, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void disableIms(int slotId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    this.mRemote.transact(100, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IImsMmTelFeature getMmTelFeatureAndListen(int slotId, IImsServiceFeatureCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(101, _data, _reply, 0);
                    _reply.readException();
                    IImsMmTelFeature _result = android.telephony.ims.aidl.IImsMmTelFeature.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IImsRcsFeature getRcsFeatureAndListen(int slotId, IImsServiceFeatureCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(102, _data, _reply, 0);
                    _reply.readException();
                    IImsRcsFeature _result = android.telephony.ims.aidl.IImsRcsFeature.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IImsRegistration getImsRegistration(int slotId, int feature) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    _data.writeInt(feature);
                    this.mRemote.transact(103, _data, _reply, 0);
                    _reply.readException();
                    IImsRegistration _result = android.telephony.ims.aidl.IImsRegistration.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public IImsConfig getImsConfig(int slotId, int feature) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    _data.writeInt(feature);
                    this.mRemote.transact(104, _data, _reply, 0);
                    _reply.readException();
                    IImsConfig _result = android.telephony.ims.aidl.IImsConfig.Stub.asInterface(_reply.readStrongBinder());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isResolvingImsBinding() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(105, _data, _reply, 0);
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

            public boolean setImsService(int slotId, boolean isCarrierImsService, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    _data.writeInt(isCarrierImsService);
                    _data.writeString(packageName);
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

            public String getImsService(int slotId, boolean isCarrierImsService) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotId);
                    _data.writeInt(isCarrierImsService);
                    this.mRemote.transact(107, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setNetworkSelectionModeAutomatic(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(108, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public CellNetworkScanResult getCellNetworkScanResults(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    CellNetworkScanResult _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(109, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (CellNetworkScanResult) CellNetworkScanResult.CREATOR.createFromParcel(_reply);
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

            public int requestNetworkScan(int subId, NetworkScanRequest request, Messenger messenger, IBinder binder) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    if (request != null) {
                        _data.writeInt(1);
                        request.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (messenger != null) {
                        _data.writeInt(1);
                        messenger.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(binder);
                    this.mRemote.transact(110, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void stopNetworkScan(int subId, int scanId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(scanId);
                    this.mRemote.transact(111, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setNetworkSelectionModeManual(int subId, String operatorNumeric, boolean persistSelection) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(operatorNumeric);
                    _data.writeInt(persistSelection);
                    boolean z = false;
                    this.mRemote.transact(112, _data, _reply, 0);
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

            public boolean setPreferredNetworkType(int subId, int networkType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(networkType);
                    boolean z = false;
                    this.mRemote.transact(113, _data, _reply, 0);
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

            public void setUserDataEnabled(int subId, boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(enable);
                    this.mRemote.transact(114, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean getDataEnabled(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    boolean z = false;
                    this.mRemote.transact(115, _data, _reply, 0);
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

            public boolean isUserDataEnabled(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    boolean z = false;
                    this.mRemote.transact(116, _data, _reply, 0);
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

            public boolean isDataEnabled(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    boolean z = false;
                    this.mRemote.transact(117, _data, _reply, 0);
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

            public String[] getPcscfAddress(String apnType, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(apnType);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(118, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setImsRegistrationState(boolean registered) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(registered);
                    this.mRemote.transact(119, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getCdmaMdn(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(120, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getCdmaMin(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(121, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getCarrierPrivilegeStatus(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(122, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getCarrierPrivilegeStatusForUid(int subId, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(uid);
                    this.mRemote.transact(123, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int checkCarrierPrivilegesForPackage(String pkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    this.mRemote.transact(124, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int checkCarrierPrivilegesForPackageAnyPhone(String pkgName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkgName);
                    this.mRemote.transact(125, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<String> getCarrierPackageNamesForIntentAndPhone(Intent intent, int phoneId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(phoneId);
                    this.mRemote.transact(126, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setLine1NumberForDisplayForSubscriber(int subId, String alphaTag, String number) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(alphaTag);
                    _data.writeString(number);
                    boolean z = false;
                    this.mRemote.transact(127, _data, _reply, 0);
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

            public String getLine1NumberForDisplay(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(128, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getLine1AlphaTagForDisplay(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(129, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String[] getMergedSubscriberIds(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(130, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setOperatorBrandOverride(int subId, String brand) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(brand);
                    boolean z = false;
                    this.mRemote.transact(131, _data, _reply, 0);
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

            public boolean setRoamingOverride(int subId, List<String> gsmRoamingList, List<String> gsmNonRoamingList, List<String> cdmaRoamingList, List<String> cdmaNonRoamingList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeStringList(gsmRoamingList);
                    _data.writeStringList(gsmNonRoamingList);
                    _data.writeStringList(cdmaRoamingList);
                    _data.writeStringList(cdmaNonRoamingList);
                    boolean z = false;
                    this.mRemote.transact(132, _data, _reply, 0);
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

            public int invokeOemRilRequestRaw(byte[] oemReq, byte[] oemResp) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(oemReq);
                    if (oemResp == null) {
                        _data.writeInt(-1);
                    } else {
                        _data.writeInt(oemResp.length);
                    }
                    this.mRemote.transact(133, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    _reply.readByteArray(oemResp);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean needMobileRadioShutdown() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(134, _data, _reply, 0);
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

            public void shutdownMobileRadios() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(135, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setRadioCapability(RadioAccessFamily[] rafs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedArray(rafs, 0);
                    this.mRemote.transact(136, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getRadioAccessFamily(int phoneId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(phoneId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(137, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void enableVideoCalling(boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enable);
                    this.mRemote.transact(138, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isVideoCallingEnabled(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    boolean z = false;
                    this.mRemote.transact(139, _data, _reply, 0);
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

            public boolean canChangeDtmfToneLength() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(140, _data, _reply, 0);
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

            public boolean isWorldPhone() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(141, _data, _reply, 0);
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

            public boolean isTtyModeSupported() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(142, _data, _reply, 0);
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

            public boolean isHearingAidCompatibilitySupported() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(143, _data, _reply, 0);
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

            public boolean isImsRegistered(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    boolean z = false;
                    this.mRemote.transact(144, _data, _reply, 0);
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

            public boolean isWifiCallingAvailable(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    boolean z = false;
                    this.mRemote.transact(145, _data, _reply, 0);
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

            public boolean isVolteAvailable(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    boolean z = false;
                    this.mRemote.transact(146, _data, _reply, 0);
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

            public boolean isVideoTelephonyAvailable(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    boolean z = false;
                    this.mRemote.transact(147, _data, _reply, 0);
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

            public int getImsRegTechnologyForMmTel(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(148, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getDeviceId(String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(149, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getImeiForSlot(int slotIndex, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(150, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getMeidForSlot(int slotIndex, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(151, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getDeviceSoftwareVersionForSlot(int slotIndex, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(152, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getSubIdForPhoneAccount(PhoneAccount phoneAccount) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (phoneAccount != null) {
                        _data.writeInt(1);
                        phoneAccount.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(153, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void factoryReset(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(154, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getLocaleFromDefaultSim() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(155, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void requestModemActivityInfo(ResultReceiver result) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (result != null) {
                        _data.writeInt(1);
                        result.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(156, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            public ServiceState getServiceStateForSubscriber(int subId, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    ServiceState _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(157, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ServiceState) ServiceState.CREATOR.createFromParcel(_reply);
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

            public Uri getVoicemailRingtoneUri(PhoneAccountHandle accountHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Uri _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (accountHandle != null) {
                        _data.writeInt(1);
                        accountHandle.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(158, _data, _reply, 0);
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

            public void setVoicemailRingtoneUri(String callingPackage, PhoneAccountHandle phoneAccountHandle, Uri uri) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    if (phoneAccountHandle != null) {
                        _data.writeInt(1);
                        phoneAccountHandle.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (uri != null) {
                        _data.writeInt(1);
                        uri.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(159, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isVoicemailVibrationEnabled(PhoneAccountHandle accountHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (accountHandle != null) {
                        _data.writeInt(1);
                        accountHandle.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(160, _data, _reply, 0);
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

            public void setVoicemailVibrationEnabled(String callingPackage, PhoneAccountHandle phoneAccountHandle, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    if (phoneAccountHandle != null) {
                        _data.writeInt(1);
                        phoneAccountHandle.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(enabled);
                    this.mRemote.transact(161, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<String> getPackagesWithCarrierPrivileges() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(162, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getAidForAppType(int subId, int appType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(appType);
                    this.mRemote.transact(163, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getEsn(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(164, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getCdmaPrlVersion(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(165, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<TelephonyHistogram> getTelephonyHistograms() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(166, _data, _reply, 0);
                    _reply.readException();
                    List<TelephonyHistogram> _result = _reply.createTypedArrayList(TelephonyHistogram.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int setAllowedCarriers(int slotIndex, List<CarrierIdentifier> carriers) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    _data.writeTypedList(carriers);
                    this.mRemote.transact(167, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<CarrierIdentifier> getAllowedCarriers(int slotIndex) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    this.mRemote.transact(168, _data, _reply, 0);
                    _reply.readException();
                    List<CarrierIdentifier> _result = _reply.createTypedArrayList(CarrierIdentifier.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getSubscriptionCarrierId(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(169, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getSubscriptionCarrierName(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(170, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void carrierActionSetMeteredApnsEnabled(int subId, boolean visible) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(visible);
                    this.mRemote.transact(171, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void carrierActionSetRadioEnabled(int subId, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(enabled);
                    this.mRemote.transact(172, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void carrierActionReportDefaultNetworkStatus(int subId, boolean report) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(report);
                    this.mRemote.transact(173, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public NetworkStats getVtDataUsage(int subId, boolean perUidStats) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    NetworkStats _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(perUidStats);
                    this.mRemote.transact(174, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (NetworkStats) NetworkStats.CREATOR.createFromParcel(_reply);
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

            public void setPolicyDataEnabled(boolean enabled, int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enabled);
                    _data.writeInt(subId);
                    this.mRemote.transact(175, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<ClientRequestStats> getClientRequestStats(String callingPackage, int subid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(subid);
                    this.mRemote.transact(176, _data, _reply, 0);
                    _reply.readException();
                    List<ClientRequestStats> _result = _reply.createTypedArrayList(ClientRequestStats.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setDataEnabledProperties(String appName, boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(appName);
                    _data.writeInt(enable);
                    this.mRemote.transact(177, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setSimPowerStateForSlot(int slotIndex, int state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(slotIndex);
                    _data.writeInt(state);
                    this.mRemote.transact(178, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String[] getForbiddenPlmns(int subId, int appType, String callingPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(appType);
                    _data.writeString(callingPackage);
                    this.mRemote.transact(179, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean getEmergencyCallbackMode(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    boolean z = false;
                    this.mRemote.transact(180, _data, _reply, 0);
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

            public SignalStrength getSignalStrength(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    SignalStrength _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(181, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (SignalStrength) SignalStrength.CREATOR.createFromParcel(_reply);
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

            public UiccSlotInfo[] getUiccSlotsInfo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(182, _data, _reply, 0);
                    _reply.readException();
                    UiccSlotInfo[] _result = (UiccSlotInfo[]) _reply.createTypedArray(UiccSlotInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean switchSlots(int[] physicalSlots) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeIntArray(physicalSlots);
                    boolean z = false;
                    this.mRemote.transact(183, _data, _reply, 0);
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

            public void setRadioIndicationUpdateMode(int subId, int filters, int mode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(filters);
                    _data.writeInt(mode);
                    this.mRemote.transact(184, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setCarrierTestOverride(int subId, String mccmnc, String imsi, String iccid, String gid1, String gid2, String plmn, String spn) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(mccmnc);
                    _data.writeString(imsi);
                    _data.writeString(iccid);
                    _data.writeString(gid1);
                    _data.writeString(gid2);
                    _data.writeString(plmn);
                    _data.writeString(spn);
                    this.mRemote.transact(185, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getCarrierIdListVersion(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(186, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void refreshUiccProfile(int subId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    this.mRemote.transact(187, _data, _reply, 0);
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

        public static ITelephony asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof ITelephony)) {
                return new Proxy(obj);
            }
            return (ITelephony) iin;
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
                ResultReceiver _arg2 = null;
                boolean _arg1 = false;
                boolean _result;
                boolean _result2;
                boolean _result3;
                int[] _result4;
                int _arg0;
                String _arg12;
                int _arg02;
                String _result5;
                int _result6;
                IBinder _arg22;
                String _result7;
                byte[] _result8;
                PhoneAccountHandle _arg03;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(descriptor);
                        dial(data.readString());
                        reply.writeNoException();
                        return true;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        call(data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 3:
                        parcel.enforceInterface(descriptor);
                        _result = endCall();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        _arg1 = endCallForSubscriber(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        answerRingingCall();
                        reply.writeNoException();
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        answerRingingCallForSubscriber(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        silenceRinger();
                        reply.writeNoException();
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        _arg1 = isOffhook(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        _result2 = isOffhookForSubscriber(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 10:
                        parcel.enforceInterface(descriptor);
                        _result2 = isRingingForSubscriber(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        _arg1 = isRinging(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 12:
                        parcel.enforceInterface(descriptor);
                        _arg1 = isIdle(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 13:
                        parcel.enforceInterface(descriptor);
                        _result2 = isIdleForSubscriber(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        _arg1 = isRadioOn(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        _result2 = isRadioOnForSubscriber(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 16:
                        parcel.enforceInterface(descriptor);
                        _arg1 = supplyPin(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        _result2 = supplyPinForSubscriber(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 18:
                        parcel.enforceInterface(descriptor);
                        _result2 = supplyPuk(data.readString(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 19:
                        parcel.enforceInterface(descriptor);
                        _result3 = supplyPukForSubscriber(data.readInt(), data.readString(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 20:
                        parcel.enforceInterface(descriptor);
                        int[] _result9 = supplyPinReportResult(data.readString());
                        reply.writeNoException();
                        parcel2.writeIntArray(_result9);
                        return true;
                    case 21:
                        parcel.enforceInterface(descriptor);
                        _result4 = supplyPinReportResultForSubscriber(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeIntArray(_result4);
                        return true;
                    case 22:
                        parcel.enforceInterface(descriptor);
                        _result4 = supplyPukReportResult(data.readString(), data.readString());
                        reply.writeNoException();
                        parcel2.writeIntArray(_result4);
                        return true;
                    case 23:
                        parcel.enforceInterface(descriptor);
                        int[] _result10 = supplyPukReportResultForSubscriber(data.readInt(), data.readString(), data.readString());
                        reply.writeNoException();
                        parcel2.writeIntArray(_result10);
                        return true;
                    case 24:
                        parcel.enforceInterface(descriptor);
                        _arg1 = handlePinMmi(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 25:
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readInt();
                        _arg12 = data.readString();
                        if (data.readInt() != 0) {
                            _arg2 = (ResultReceiver) ResultReceiver.CREATOR.createFromParcel(parcel);
                        }
                        handleUssdRequest(_arg0, _arg12, _arg2);
                        reply.writeNoException();
                        return true;
                    case 26:
                        parcel.enforceInterface(descriptor);
                        _result2 = handlePinMmiForSubscriber(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 27:
                        parcel.enforceInterface(descriptor);
                        toggleRadioOnOff();
                        reply.writeNoException();
                        return true;
                    case 28:
                        parcel.enforceInterface(descriptor);
                        toggleRadioOnOffForSubscriber(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 29:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        _arg1 = setRadio(_arg1);
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 30:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        _result2 = setRadioForSubscriber(_arg02, _arg1);
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 31:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        _arg1 = setRadioPower(_arg1);
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 32:
                        parcel.enforceInterface(descriptor);
                        updateServiceLocation();
                        reply.writeNoException();
                        return true;
                    case 33:
                        parcel.enforceInterface(descriptor);
                        updateServiceLocationForSubscriber(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 34:
                        parcel.enforceInterface(descriptor);
                        enableLocationUpdates();
                        reply.writeNoException();
                        return true;
                    case 35:
                        parcel.enforceInterface(descriptor);
                        enableLocationUpdatesForSubscriber(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 36:
                        parcel.enforceInterface(descriptor);
                        disableLocationUpdates();
                        reply.writeNoException();
                        return true;
                    case 37:
                        parcel.enforceInterface(descriptor);
                        disableLocationUpdatesForSubscriber(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 38:
                        parcel.enforceInterface(descriptor);
                        _result = enableDataConnectivity();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 39:
                        parcel.enforceInterface(descriptor);
                        _result = disableDataConnectivity();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 40:
                        parcel.enforceInterface(descriptor);
                        _arg1 = isDataConnectivityPossible(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 41:
                        parcel.enforceInterface(descriptor);
                        Bundle _result11 = getCellLocation(data.readString());
                        reply.writeNoException();
                        if (_result11 != null) {
                            parcel2.writeInt(1);
                            _result11.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 42:
                        parcel.enforceInterface(descriptor);
                        _result5 = getNetworkCountryIsoForPhone(data.readInt());
                        reply.writeNoException();
                        parcel2.writeString(_result5);
                        return true;
                    case 43:
                        parcel.enforceInterface(descriptor);
                        List<NeighboringCellInfo> _result12 = getNeighboringCellInfo(data.readString());
                        reply.writeNoException();
                        parcel2.writeTypedList(_result12);
                        return true;
                    case 44:
                        parcel.enforceInterface(descriptor);
                        _arg02 = getCallState();
                        reply.writeNoException();
                        parcel2.writeInt(_arg02);
                        return true;
                    case 45:
                        parcel.enforceInterface(descriptor);
                        _arg0 = getCallStateForSlot(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg0);
                        return true;
                    case 46:
                        parcel.enforceInterface(descriptor);
                        _arg02 = getDataActivity();
                        reply.writeNoException();
                        parcel2.writeInt(_arg02);
                        return true;
                    case 47:
                        parcel.enforceInterface(descriptor);
                        _arg02 = getDataState();
                        reply.writeNoException();
                        parcel2.writeInt(_arg02);
                        return true;
                    case 48:
                        parcel.enforceInterface(descriptor);
                        _arg02 = getActivePhoneType();
                        reply.writeNoException();
                        parcel2.writeInt(_arg02);
                        return true;
                    case 49:
                        parcel.enforceInterface(descriptor);
                        _arg0 = getActivePhoneTypeForSlot(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg0);
                        return true;
                    case 50:
                        parcel.enforceInterface(descriptor);
                        _arg0 = getCdmaEriIconIndex(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_arg0);
                        return true;
                    case 51:
                        parcel.enforceInterface(descriptor);
                        _result6 = getCdmaEriIconIndexForSubscriber(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return true;
                    case 52:
                        parcel.enforceInterface(descriptor);
                        _arg0 = getCdmaEriIconMode(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_arg0);
                        return true;
                    case 53:
                        parcel.enforceInterface(descriptor);
                        _result6 = getCdmaEriIconModeForSubscriber(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return true;
                    case 54:
                        parcel.enforceInterface(descriptor);
                        _result5 = getCdmaEriText(data.readString());
                        reply.writeNoException();
                        parcel2.writeString(_result5);
                        return true;
                    case 55:
                        parcel.enforceInterface(descriptor);
                        _arg12 = getCdmaEriTextForSubscriber(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeString(_arg12);
                        return true;
                    case 56:
                        parcel.enforceInterface(descriptor);
                        _result = needsOtaServiceProvisioning();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 57:
                        parcel.enforceInterface(descriptor);
                        _result3 = setVoiceMailNumber(data.readInt(), data.readString(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 58:
                        parcel.enforceInterface(descriptor);
                        setVoiceActivationState(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 59:
                        parcel.enforceInterface(descriptor);
                        setDataActivationState(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 60:
                        parcel.enforceInterface(descriptor);
                        _result6 = getVoiceActivationState(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return true;
                    case 61:
                        parcel.enforceInterface(descriptor);
                        _result6 = getDataActivationState(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return true;
                    case 62:
                        parcel.enforceInterface(descriptor);
                        _arg02 = getVoiceMessageCount();
                        reply.writeNoException();
                        parcel2.writeInt(_arg02);
                        return true;
                    case 63:
                        parcel.enforceInterface(descriptor);
                        _arg0 = getVoiceMessageCountForSubscriber(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg0);
                        return true;
                    case 64:
                        parcel.enforceInterface(descriptor);
                        _arg1 = isConcurrentVoiceAndDataAllowed(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 65:
                        parcel.enforceInterface(descriptor);
                        Bundle _result13 = getVisualVoicemailSettings(data.readString(), data.readInt());
                        reply.writeNoException();
                        if (_result13 != null) {
                            parcel2.writeInt(1);
                            _result13.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 66:
                        parcel.enforceInterface(descriptor);
                        _arg12 = getVisualVoicemailPackageName(data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeString(_arg12);
                        return true;
                    case 67:
                        VisualVoicemailSmsFilterSettings _arg23;
                        parcel.enforceInterface(descriptor);
                        _result5 = data.readString();
                        _result6 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg23 = (VisualVoicemailSmsFilterSettings) VisualVoicemailSmsFilterSettings.CREATOR.createFromParcel(parcel);
                        }
                        enableVisualVoicemailSmsFilter(_result5, _result6, _arg23);
                        reply.writeNoException();
                        return true;
                    case 68:
                        parcel.enforceInterface(descriptor);
                        disableVisualVoicemailSmsFilter(data.readString(), data.readInt());
                        return true;
                    case 69:
                        parcel.enforceInterface(descriptor);
                        VisualVoicemailSmsFilterSettings _result14 = getVisualVoicemailSmsFilterSettings(data.readString(), data.readInt());
                        reply.writeNoException();
                        if (_result14 != null) {
                            parcel2.writeInt(1);
                            _result14.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 70:
                        parcel.enforceInterface(descriptor);
                        VisualVoicemailSmsFilterSettings _result15 = getActiveVisualVoicemailSmsFilterSettings(data.readInt());
                        reply.writeNoException();
                        if (_result15 != null) {
                            parcel2.writeInt(1);
                            _result15.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 71:
                        parcel.enforceInterface(descriptor);
                        String _arg04 = data.readString();
                        int _arg13 = data.readInt();
                        String _arg24 = data.readString();
                        int _arg3 = data.readInt();
                        String _arg4 = data.readString();
                        if (data.readInt() != 0) {
                            _arg22 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel);
                        }
                        sendVisualVoicemailSmsForSubscriber(_arg04, _arg13, _arg24, _arg3, _arg4, _arg22);
                        reply.writeNoException();
                        return true;
                    case 72:
                        parcel.enforceInterface(descriptor);
                        sendDialerSpecialCode(data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 73:
                        parcel.enforceInterface(descriptor);
                        _arg02 = getNetworkType();
                        reply.writeNoException();
                        parcel2.writeInt(_arg02);
                        return true;
                    case 74:
                        parcel.enforceInterface(descriptor);
                        _result6 = getNetworkTypeForSubscriber(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return true;
                    case 75:
                        parcel.enforceInterface(descriptor);
                        _arg0 = getDataNetworkType(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_arg0);
                        return true;
                    case 76:
                        parcel.enforceInterface(descriptor);
                        _result6 = getDataNetworkTypeForSubscriber(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return true;
                    case 77:
                        parcel.enforceInterface(descriptor);
                        _result6 = getVoiceNetworkTypeForSubscriber(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return true;
                    case 78:
                        parcel.enforceInterface(descriptor);
                        _result = hasIccCard();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 79:
                        parcel.enforceInterface(descriptor);
                        _arg1 = hasIccCardUsingSlotIndex(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 80:
                        parcel.enforceInterface(descriptor);
                        _arg0 = getLteOnCdmaMode(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_arg0);
                        return true;
                    case 81:
                        parcel.enforceInterface(descriptor);
                        _result6 = getLteOnCdmaModeForSubscriber(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return true;
                    case 82:
                        parcel.enforceInterface(descriptor);
                        List<CellInfo> _result16 = getAllCellInfo(data.readString());
                        reply.writeNoException();
                        parcel2.writeTypedList(_result16);
                        return true;
                    case 83:
                        parcel.enforceInterface(descriptor);
                        setCellInfoListRate(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 84:
                        parcel.enforceInterface(descriptor);
                        _arg02 = getDefaultSim();
                        reply.writeNoException();
                        parcel2.writeInt(_arg02);
                        return true;
                    case 85:
                        parcel.enforceInterface(descriptor);
                        IccOpenLogicalChannelResponse _result17 = iccOpenLogicalChannel(data.readInt(), data.readString(), data.readString(), data.readInt());
                        reply.writeNoException();
                        if (_result17 != null) {
                            parcel2.writeInt(1);
                            _result17.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 86:
                        parcel.enforceInterface(descriptor);
                        IccOpenLogicalChannelResponse _result18 = iccOpenLogicalChannelP2(data.readInt(), data.readString(), data.readByte());
                        reply.writeNoException();
                        if (_result18 != null) {
                            parcel2.writeInt(1);
                            _result18.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 87:
                        parcel.enforceInterface(descriptor);
                        _result2 = iccCloseLogicalChannel(data.readInt(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 88:
                        parcel.enforceInterface(descriptor);
                        _result7 = iccTransmitApduLogicalChannel(data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeString(_result7);
                        return true;
                    case 89:
                        parcel.enforceInterface(descriptor);
                        _result7 = iccTransmitApduBasicChannel(data.readInt(), data.readString(), data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeString(_result7);
                        return true;
                    case 90:
                        parcel.enforceInterface(descriptor);
                        _result8 = iccExchangeSimIO(data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeByteArray(_result8);
                        return true;
                    case 91:
                        parcel.enforceInterface(descriptor);
                        _arg12 = sendEnvelopeWithStatus(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeString(_arg12);
                        return true;
                    case 92:
                        parcel.enforceInterface(descriptor);
                        _result5 = nvReadItem(data.readInt());
                        reply.writeNoException();
                        parcel2.writeString(_result5);
                        return true;
                    case 93:
                        parcel.enforceInterface(descriptor);
                        _result2 = nvWriteItem(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 94:
                        parcel.enforceInterface(descriptor);
                        _arg1 = nvWriteCdmaPrl(data.createByteArray());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 95:
                        parcel.enforceInterface(descriptor);
                        _arg1 = nvResetConfig(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 96:
                        parcel.enforceInterface(descriptor);
                        _arg0 = getCalculatedPreferredNetworkType(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_arg0);
                        return true;
                    case 97:
                        parcel.enforceInterface(descriptor);
                        _arg0 = getPreferredNetworkType(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg0);
                        return true;
                    case 98:
                        parcel.enforceInterface(descriptor);
                        _arg02 = getTetherApnRequired();
                        reply.writeNoException();
                        parcel2.writeInt(_arg02);
                        return true;
                    case 99:
                        parcel.enforceInterface(descriptor);
                        enableIms(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 100:
                        parcel.enforceInterface(descriptor);
                        disableIms(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 101:
                        parcel.enforceInterface(descriptor);
                        IImsMmTelFeature _result19 = getMmTelFeatureAndListen(data.readInt(), com.android.ims.internal.IImsServiceFeatureCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        if (_result19 != null) {
                            _arg22 = _result19.asBinder();
                        }
                        parcel2.writeStrongBinder(_arg22);
                        return true;
                    case 102:
                        parcel.enforceInterface(descriptor);
                        IImsRcsFeature _result20 = getRcsFeatureAndListen(data.readInt(), com.android.ims.internal.IImsServiceFeatureCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        if (_result20 != null) {
                            _arg22 = _result20.asBinder();
                        }
                        parcel2.writeStrongBinder(_arg22);
                        return true;
                    case 103:
                        parcel.enforceInterface(descriptor);
                        IImsRegistration _result21 = getImsRegistration(data.readInt(), data.readInt());
                        reply.writeNoException();
                        if (_result21 != null) {
                            _arg22 = _result21.asBinder();
                        }
                        parcel2.writeStrongBinder(_arg22);
                        return true;
                    case 104:
                        parcel.enforceInterface(descriptor);
                        IImsConfig _result22 = getImsConfig(data.readInt(), data.readInt());
                        reply.writeNoException();
                        if (_result22 != null) {
                            _arg22 = _result22.asBinder();
                        }
                        parcel2.writeStrongBinder(_arg22);
                        return true;
                    case 105:
                        parcel.enforceInterface(descriptor);
                        _result = isResolvingImsBinding();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 106:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        _result3 = setImsService(_arg02, _arg1, data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 107:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        _arg12 = getImsService(_arg02, _arg1);
                        reply.writeNoException();
                        parcel2.writeString(_arg12);
                        return true;
                    case 108:
                        parcel.enforceInterface(descriptor);
                        setNetworkSelectionModeAutomatic(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 109:
                        parcel.enforceInterface(descriptor);
                        CellNetworkScanResult _result23 = getCellNetworkScanResults(data.readInt());
                        reply.writeNoException();
                        if (_result23 != null) {
                            parcel2.writeInt(1);
                            _result23.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 110:
                        NetworkScanRequest _arg14;
                        Messenger _arg25;
                        parcel.enforceInterface(descriptor);
                        _arg0 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg14 = (NetworkScanRequest) NetworkScanRequest.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg14 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg25 = (Messenger) Messenger.CREATOR.createFromParcel(parcel);
                        }
                        int _result24 = requestNetworkScan(_arg0, _arg14, _arg25, data.readStrongBinder());
                        reply.writeNoException();
                        parcel2.writeInt(_result24);
                        return true;
                    case 111:
                        parcel.enforceInterface(descriptor);
                        stopNetworkScan(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 112:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readInt();
                        _arg12 = data.readString();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        _result3 = setNetworkSelectionModeManual(_arg02, _arg12, _arg1);
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 113:
                        parcel.enforceInterface(descriptor);
                        _result2 = setPreferredNetworkType(data.readInt(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 114:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        setUserDataEnabled(_arg02, _arg1);
                        reply.writeNoException();
                        return true;
                    case 115:
                        parcel.enforceInterface(descriptor);
                        _arg1 = getDataEnabled(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 116:
                        parcel.enforceInterface(descriptor);
                        _arg1 = isUserDataEnabled(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 117:
                        parcel.enforceInterface(descriptor);
                        _arg1 = isDataEnabled(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 118:
                        parcel.enforceInterface(descriptor);
                        String[] _result25 = getPcscfAddress(data.readString(), data.readString());
                        reply.writeNoException();
                        parcel2.writeStringArray(_result25);
                        return true;
                    case 119:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        setImsRegistrationState(_arg1);
                        reply.writeNoException();
                        return true;
                    case 120:
                        parcel.enforceInterface(descriptor);
                        _result5 = getCdmaMdn(data.readInt());
                        reply.writeNoException();
                        parcel2.writeString(_result5);
                        return true;
                    case 121:
                        parcel.enforceInterface(descriptor);
                        _result5 = getCdmaMin(data.readInt());
                        reply.writeNoException();
                        parcel2.writeString(_result5);
                        return true;
                    case 122:
                        parcel.enforceInterface(descriptor);
                        _arg0 = getCarrierPrivilegeStatus(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg0);
                        return true;
                    case 123:
                        parcel.enforceInterface(descriptor);
                        _result6 = getCarrierPrivilegeStatusForUid(data.readInt(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return true;
                    case 124:
                        parcel.enforceInterface(descriptor);
                        _arg0 = checkCarrierPrivilegesForPackage(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_arg0);
                        return true;
                    case 125:
                        parcel.enforceInterface(descriptor);
                        _arg0 = checkCarrierPrivilegesForPackageAnyPhone(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_arg0);
                        return true;
                    case 126:
                        Intent _arg05;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg05 = (Intent) Intent.CREATOR.createFromParcel(parcel);
                        }
                        List<String> _result26 = getCarrierPackageNamesForIntentAndPhone(_arg05, data.readInt());
                        reply.writeNoException();
                        parcel2.writeStringList(_result26);
                        return true;
                    case 127:
                        parcel.enforceInterface(descriptor);
                        _result3 = setLine1NumberForDisplayForSubscriber(data.readInt(), data.readString(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 128:
                        parcel.enforceInterface(descriptor);
                        _arg12 = getLine1NumberForDisplay(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeString(_arg12);
                        return true;
                    case 129:
                        parcel.enforceInterface(descriptor);
                        _arg12 = getLine1AlphaTagForDisplay(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeString(_arg12);
                        return true;
                    case 130:
                        parcel.enforceInterface(descriptor);
                        String[] _result27 = getMergedSubscriberIds(data.readString());
                        reply.writeNoException();
                        parcel2.writeStringArray(_result27);
                        return true;
                    case 131:
                        parcel.enforceInterface(descriptor);
                        _result2 = setOperatorBrandOverride(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 132:
                        parcel.enforceInterface(descriptor);
                        _result = setRoamingOverride(data.readInt(), data.createStringArrayList(), data.createStringArrayList(), data.createStringArrayList(), data.createStringArrayList());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 133:
                        byte[] _arg15;
                        parcel.enforceInterface(descriptor);
                        _result8 = data.createByteArray();
                        _arg0 = data.readInt();
                        if (_arg0 < 0) {
                            _arg15 = null;
                        } else {
                            _arg15 = new byte[_arg0];
                        }
                        int _result28 = invokeOemRilRequestRaw(_result8, _arg15);
                        reply.writeNoException();
                        parcel2.writeInt(_result28);
                        parcel2.writeByteArray(_arg15);
                        return true;
                    case 134:
                        parcel.enforceInterface(descriptor);
                        _result = needMobileRadioShutdown();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 135:
                        parcel.enforceInterface(descriptor);
                        shutdownMobileRadios();
                        reply.writeNoException();
                        return true;
                    case 136:
                        parcel.enforceInterface(descriptor);
                        setRadioCapability((RadioAccessFamily[]) parcel.createTypedArray(RadioAccessFamily.CREATOR));
                        reply.writeNoException();
                        return true;
                    case 137:
                        parcel.enforceInterface(descriptor);
                        _result6 = getRadioAccessFamily(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return true;
                    case 138:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        enableVideoCalling(_arg1);
                        reply.writeNoException();
                        return true;
                    case 139:
                        parcel.enforceInterface(descriptor);
                        _arg1 = isVideoCallingEnabled(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 140:
                        parcel.enforceInterface(descriptor);
                        _result = canChangeDtmfToneLength();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 141:
                        parcel.enforceInterface(descriptor);
                        _result = isWorldPhone();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 142:
                        parcel.enforceInterface(descriptor);
                        _result = isTtyModeSupported();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 143:
                        parcel.enforceInterface(descriptor);
                        _result = isHearingAidCompatibilitySupported();
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 144:
                        parcel.enforceInterface(descriptor);
                        _arg1 = isImsRegistered(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 145:
                        parcel.enforceInterface(descriptor);
                        _arg1 = isWifiCallingAvailable(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 146:
                        parcel.enforceInterface(descriptor);
                        _arg1 = isVolteAvailable(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 147:
                        parcel.enforceInterface(descriptor);
                        _arg1 = isVideoTelephonyAvailable(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 148:
                        parcel.enforceInterface(descriptor);
                        _arg0 = getImsRegTechnologyForMmTel(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg0);
                        return true;
                    case 149:
                        parcel.enforceInterface(descriptor);
                        _result5 = getDeviceId(data.readString());
                        reply.writeNoException();
                        parcel2.writeString(_result5);
                        return true;
                    case 150:
                        parcel.enforceInterface(descriptor);
                        _arg12 = getImeiForSlot(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeString(_arg12);
                        return true;
                    case 151:
                        parcel.enforceInterface(descriptor);
                        _arg12 = getMeidForSlot(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeString(_arg12);
                        return true;
                    case 152:
                        parcel.enforceInterface(descriptor);
                        _arg12 = getDeviceSoftwareVersionForSlot(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeString(_arg12);
                        return true;
                    case 153:
                        PhoneAccount _arg06;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg06 = (PhoneAccount) PhoneAccount.CREATOR.createFromParcel(parcel);
                        }
                        _arg0 = getSubIdForPhoneAccount(_arg06);
                        reply.writeNoException();
                        parcel2.writeInt(_arg0);
                        return true;
                    case 154:
                        parcel.enforceInterface(descriptor);
                        factoryReset(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 155:
                        parcel.enforceInterface(descriptor);
                        _result7 = getLocaleFromDefaultSim();
                        reply.writeNoException();
                        parcel2.writeString(_result7);
                        return true;
                    case 156:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg2 = (ResultReceiver) ResultReceiver.CREATOR.createFromParcel(parcel);
                        }
                        requestModemActivityInfo(_arg2);
                        return true;
                    case 157:
                        parcel.enforceInterface(descriptor);
                        ServiceState _result29 = getServiceStateForSubscriber(data.readInt(), data.readString());
                        reply.writeNoException();
                        if (_result29 != null) {
                            parcel2.writeInt(1);
                            _result29.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 158:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (PhoneAccountHandle) PhoneAccountHandle.CREATOR.createFromParcel(parcel);
                        }
                        Uri _result30 = getVoicemailRingtoneUri(_arg03);
                        reply.writeNoException();
                        if (_result30 != null) {
                            parcel2.writeInt(1);
                            _result30.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 159:
                        PhoneAccountHandle _arg16;
                        Uri _arg26;
                        parcel.enforceInterface(descriptor);
                        _result5 = data.readString();
                        if (data.readInt() != 0) {
                            _arg16 = (PhoneAccountHandle) PhoneAccountHandle.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg16 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg26 = (Uri) Uri.CREATOR.createFromParcel(parcel);
                        }
                        setVoicemailRingtoneUri(_result5, _arg16, _arg26);
                        reply.writeNoException();
                        return true;
                    case 160:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (PhoneAccountHandle) PhoneAccountHandle.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = isVoicemailVibrationEnabled(_arg03);
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 161:
                        parcel.enforceInterface(descriptor);
                        _arg12 = data.readString();
                        if (data.readInt() != 0) {
                            _arg03 = (PhoneAccountHandle) PhoneAccountHandle.CREATOR.createFromParcel(parcel);
                        }
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        setVoicemailVibrationEnabled(_arg12, _arg03, _arg1);
                        reply.writeNoException();
                        return true;
                    case 162:
                        parcel.enforceInterface(descriptor);
                        List<String> _result31 = getPackagesWithCarrierPrivileges();
                        reply.writeNoException();
                        parcel2.writeStringList(_result31);
                        return true;
                    case 163:
                        parcel.enforceInterface(descriptor);
                        _arg12 = getAidForAppType(data.readInt(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeString(_arg12);
                        return true;
                    case 164:
                        parcel.enforceInterface(descriptor);
                        _result5 = getEsn(data.readInt());
                        reply.writeNoException();
                        parcel2.writeString(_result5);
                        return true;
                    case 165:
                        parcel.enforceInterface(descriptor);
                        _result5 = getCdmaPrlVersion(data.readInt());
                        reply.writeNoException();
                        parcel2.writeString(_result5);
                        return true;
                    case 166:
                        parcel.enforceInterface(descriptor);
                        List<TelephonyHistogram> _result32 = getTelephonyHistograms();
                        reply.writeNoException();
                        parcel2.writeTypedList(_result32);
                        return true;
                    case 167:
                        parcel.enforceInterface(descriptor);
                        _result6 = setAllowedCarriers(data.readInt(), parcel.createTypedArrayList(CarrierIdentifier.CREATOR));
                        reply.writeNoException();
                        parcel2.writeInt(_result6);
                        return true;
                    case 168:
                        parcel.enforceInterface(descriptor);
                        List<CarrierIdentifier> _result33 = getAllowedCarriers(data.readInt());
                        reply.writeNoException();
                        parcel2.writeTypedList(_result33);
                        return true;
                    case 169:
                        parcel.enforceInterface(descriptor);
                        _arg0 = getSubscriptionCarrierId(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg0);
                        return true;
                    case 170:
                        parcel.enforceInterface(descriptor);
                        _result5 = getSubscriptionCarrierName(data.readInt());
                        reply.writeNoException();
                        parcel2.writeString(_result5);
                        return true;
                    case 171:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        carrierActionSetMeteredApnsEnabled(_arg02, _arg1);
                        reply.writeNoException();
                        return true;
                    case 172:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        carrierActionSetRadioEnabled(_arg02, _arg1);
                        reply.writeNoException();
                        return true;
                    case 173:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        carrierActionReportDefaultNetworkStatus(_arg02, _arg1);
                        reply.writeNoException();
                        return true;
                    case 174:
                        parcel.enforceInterface(descriptor);
                        NetworkStats _result34 = getVtDataUsage(data.readInt(), data.readInt() != 0);
                        reply.writeNoException();
                        if (_result34 != null) {
                            parcel2.writeInt(1);
                            _result34.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 175:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        setPolicyDataEnabled(_arg1, data.readInt());
                        reply.writeNoException();
                        return true;
                    case 176:
                        parcel.enforceInterface(descriptor);
                        List<ClientRequestStats> _result35 = getClientRequestStats(data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeTypedList(_result35);
                        return true;
                    case 177:
                        parcel.enforceInterface(descriptor);
                        _result7 = data.readString();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        setDataEnabledProperties(_result7, _arg1);
                        reply.writeNoException();
                        return true;
                    case 178:
                        parcel.enforceInterface(descriptor);
                        setSimPowerStateForSlot(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 179:
                        parcel.enforceInterface(descriptor);
                        String[] _result36 = getForbiddenPlmns(data.readInt(), data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeStringArray(_result36);
                        return true;
                    case 180:
                        parcel.enforceInterface(descriptor);
                        _arg1 = getEmergencyCallbackMode(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 181:
                        parcel.enforceInterface(descriptor);
                        SignalStrength _result37 = getSignalStrength(data.readInt());
                        reply.writeNoException();
                        if (_result37 != null) {
                            parcel2.writeInt(1);
                            _result37.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 182:
                        parcel.enforceInterface(descriptor);
                        UiccSlotInfo[] _result38 = getUiccSlotsInfo();
                        reply.writeNoException();
                        parcel2.writeTypedArray(_result38, 1);
                        return true;
                    case 183:
                        parcel.enforceInterface(descriptor);
                        _arg1 = switchSlots(data.createIntArray());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 184:
                        parcel.enforceInterface(descriptor);
                        setRadioIndicationUpdateMode(data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 185:
                        parcel.enforceInterface(descriptor);
                        setCarrierTestOverride(data.readInt(), data.readString(), data.readString(), data.readString(), data.readString(), data.readString(), data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 186:
                        parcel.enforceInterface(descriptor);
                        _arg0 = getCarrierIdListVersion(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg0);
                        return true;
                    case 187:
                        parcel.enforceInterface(descriptor);
                        refreshUiccProfile(data.readInt());
                        reply.writeNoException();
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            parcel2.writeString(descriptor);
            return true;
        }
    }

    void answerRingingCall() throws RemoteException;

    void answerRingingCallForSubscriber(int i) throws RemoteException;

    void call(String str, String str2) throws RemoteException;

    boolean canChangeDtmfToneLength() throws RemoteException;

    void carrierActionReportDefaultNetworkStatus(int i, boolean z) throws RemoteException;

    void carrierActionSetMeteredApnsEnabled(int i, boolean z) throws RemoteException;

    void carrierActionSetRadioEnabled(int i, boolean z) throws RemoteException;

    int checkCarrierPrivilegesForPackage(String str) throws RemoteException;

    int checkCarrierPrivilegesForPackageAnyPhone(String str) throws RemoteException;

    void dial(String str) throws RemoteException;

    boolean disableDataConnectivity() throws RemoteException;

    void disableIms(int i) throws RemoteException;

    void disableLocationUpdates() throws RemoteException;

    void disableLocationUpdatesForSubscriber(int i) throws RemoteException;

    void disableVisualVoicemailSmsFilter(String str, int i) throws RemoteException;

    boolean enableDataConnectivity() throws RemoteException;

    void enableIms(int i) throws RemoteException;

    void enableLocationUpdates() throws RemoteException;

    void enableLocationUpdatesForSubscriber(int i) throws RemoteException;

    void enableVideoCalling(boolean z) throws RemoteException;

    void enableVisualVoicemailSmsFilter(String str, int i, VisualVoicemailSmsFilterSettings visualVoicemailSmsFilterSettings) throws RemoteException;

    boolean endCall() throws RemoteException;

    boolean endCallForSubscriber(int i) throws RemoteException;

    void factoryReset(int i) throws RemoteException;

    int getActivePhoneType() throws RemoteException;

    int getActivePhoneTypeForSlot(int i) throws RemoteException;

    VisualVoicemailSmsFilterSettings getActiveVisualVoicemailSmsFilterSettings(int i) throws RemoteException;

    String getAidForAppType(int i, int i2) throws RemoteException;

    List<CellInfo> getAllCellInfo(String str) throws RemoteException;

    List<CarrierIdentifier> getAllowedCarriers(int i) throws RemoteException;

    int getCalculatedPreferredNetworkType(String str) throws RemoteException;

    int getCallState() throws RemoteException;

    int getCallStateForSlot(int i) throws RemoteException;

    int getCarrierIdListVersion(int i) throws RemoteException;

    List<String> getCarrierPackageNamesForIntentAndPhone(Intent intent, int i) throws RemoteException;

    int getCarrierPrivilegeStatus(int i) throws RemoteException;

    int getCarrierPrivilegeStatusForUid(int i, int i2) throws RemoteException;

    int getCdmaEriIconIndex(String str) throws RemoteException;

    int getCdmaEriIconIndexForSubscriber(int i, String str) throws RemoteException;

    int getCdmaEriIconMode(String str) throws RemoteException;

    int getCdmaEriIconModeForSubscriber(int i, String str) throws RemoteException;

    String getCdmaEriText(String str) throws RemoteException;

    String getCdmaEriTextForSubscriber(int i, String str) throws RemoteException;

    String getCdmaMdn(int i) throws RemoteException;

    String getCdmaMin(int i) throws RemoteException;

    String getCdmaPrlVersion(int i) throws RemoteException;

    Bundle getCellLocation(String str) throws RemoteException;

    CellNetworkScanResult getCellNetworkScanResults(int i) throws RemoteException;

    List<ClientRequestStats> getClientRequestStats(String str, int i) throws RemoteException;

    int getDataActivationState(int i, String str) throws RemoteException;

    int getDataActivity() throws RemoteException;

    boolean getDataEnabled(int i) throws RemoteException;

    int getDataNetworkType(String str) throws RemoteException;

    int getDataNetworkTypeForSubscriber(int i, String str) throws RemoteException;

    int getDataState() throws RemoteException;

    int getDefaultSim() throws RemoteException;

    String getDeviceId(String str) throws RemoteException;

    String getDeviceSoftwareVersionForSlot(int i, String str) throws RemoteException;

    boolean getEmergencyCallbackMode(int i) throws RemoteException;

    String getEsn(int i) throws RemoteException;

    String[] getForbiddenPlmns(int i, int i2, String str) throws RemoteException;

    String getImeiForSlot(int i, String str) throws RemoteException;

    IImsConfig getImsConfig(int i, int i2) throws RemoteException;

    int getImsRegTechnologyForMmTel(int i) throws RemoteException;

    IImsRegistration getImsRegistration(int i, int i2) throws RemoteException;

    String getImsService(int i, boolean z) throws RemoteException;

    String getLine1AlphaTagForDisplay(int i, String str) throws RemoteException;

    String getLine1NumberForDisplay(int i, String str) throws RemoteException;

    String getLocaleFromDefaultSim() throws RemoteException;

    int getLteOnCdmaMode(String str) throws RemoteException;

    int getLteOnCdmaModeForSubscriber(int i, String str) throws RemoteException;

    String getMeidForSlot(int i, String str) throws RemoteException;

    String[] getMergedSubscriberIds(String str) throws RemoteException;

    IImsMmTelFeature getMmTelFeatureAndListen(int i, IImsServiceFeatureCallback iImsServiceFeatureCallback) throws RemoteException;

    List<NeighboringCellInfo> getNeighboringCellInfo(String str) throws RemoteException;

    String getNetworkCountryIsoForPhone(int i) throws RemoteException;

    int getNetworkType() throws RemoteException;

    int getNetworkTypeForSubscriber(int i, String str) throws RemoteException;

    List<String> getPackagesWithCarrierPrivileges() throws RemoteException;

    String[] getPcscfAddress(String str, String str2) throws RemoteException;

    int getPreferredNetworkType(int i) throws RemoteException;

    int getRadioAccessFamily(int i, String str) throws RemoteException;

    IImsRcsFeature getRcsFeatureAndListen(int i, IImsServiceFeatureCallback iImsServiceFeatureCallback) throws RemoteException;

    ServiceState getServiceStateForSubscriber(int i, String str) throws RemoteException;

    SignalStrength getSignalStrength(int i) throws RemoteException;

    int getSubIdForPhoneAccount(PhoneAccount phoneAccount) throws RemoteException;

    int getSubscriptionCarrierId(int i) throws RemoteException;

    String getSubscriptionCarrierName(int i) throws RemoteException;

    List<TelephonyHistogram> getTelephonyHistograms() throws RemoteException;

    int getTetherApnRequired() throws RemoteException;

    UiccSlotInfo[] getUiccSlotsInfo() throws RemoteException;

    String getVisualVoicemailPackageName(String str, int i) throws RemoteException;

    Bundle getVisualVoicemailSettings(String str, int i) throws RemoteException;

    VisualVoicemailSmsFilterSettings getVisualVoicemailSmsFilterSettings(String str, int i) throws RemoteException;

    int getVoiceActivationState(int i, String str) throws RemoteException;

    int getVoiceMessageCount() throws RemoteException;

    int getVoiceMessageCountForSubscriber(int i) throws RemoteException;

    int getVoiceNetworkTypeForSubscriber(int i, String str) throws RemoteException;

    Uri getVoicemailRingtoneUri(PhoneAccountHandle phoneAccountHandle) throws RemoteException;

    NetworkStats getVtDataUsage(int i, boolean z) throws RemoteException;

    boolean handlePinMmi(String str) throws RemoteException;

    boolean handlePinMmiForSubscriber(int i, String str) throws RemoteException;

    void handleUssdRequest(int i, String str, ResultReceiver resultReceiver) throws RemoteException;

    boolean hasIccCard() throws RemoteException;

    boolean hasIccCardUsingSlotIndex(int i) throws RemoteException;

    boolean iccCloseLogicalChannel(int i, int i2) throws RemoteException;

    byte[] iccExchangeSimIO(int i, int i2, int i3, int i4, int i5, int i6, String str) throws RemoteException;

    IccOpenLogicalChannelResponse iccOpenLogicalChannel(int i, String str, String str2, int i2) throws RemoteException;

    IccOpenLogicalChannelResponse iccOpenLogicalChannelP2(int i, String str, byte b) throws RemoteException;

    String iccTransmitApduBasicChannel(int i, String str, int i2, int i3, int i4, int i5, int i6, String str2) throws RemoteException;

    String iccTransmitApduLogicalChannel(int i, int i2, int i3, int i4, int i5, int i6, int i7, String str) throws RemoteException;

    int invokeOemRilRequestRaw(byte[] bArr, byte[] bArr2) throws RemoteException;

    boolean isConcurrentVoiceAndDataAllowed(int i) throws RemoteException;

    boolean isDataConnectivityPossible(int i) throws RemoteException;

    boolean isDataEnabled(int i) throws RemoteException;

    boolean isHearingAidCompatibilitySupported() throws RemoteException;

    boolean isIdle(String str) throws RemoteException;

    boolean isIdleForSubscriber(int i, String str) throws RemoteException;

    boolean isImsRegistered(int i) throws RemoteException;

    boolean isOffhook(String str) throws RemoteException;

    boolean isOffhookForSubscriber(int i, String str) throws RemoteException;

    boolean isRadioOn(String str) throws RemoteException;

    boolean isRadioOnForSubscriber(int i, String str) throws RemoteException;

    boolean isResolvingImsBinding() throws RemoteException;

    boolean isRinging(String str) throws RemoteException;

    boolean isRingingForSubscriber(int i, String str) throws RemoteException;

    boolean isTtyModeSupported() throws RemoteException;

    boolean isUserDataEnabled(int i) throws RemoteException;

    boolean isVideoCallingEnabled(String str) throws RemoteException;

    boolean isVideoTelephonyAvailable(int i) throws RemoteException;

    boolean isVoicemailVibrationEnabled(PhoneAccountHandle phoneAccountHandle) throws RemoteException;

    boolean isVolteAvailable(int i) throws RemoteException;

    boolean isWifiCallingAvailable(int i) throws RemoteException;

    boolean isWorldPhone() throws RemoteException;

    boolean needMobileRadioShutdown() throws RemoteException;

    boolean needsOtaServiceProvisioning() throws RemoteException;

    String nvReadItem(int i) throws RemoteException;

    boolean nvResetConfig(int i) throws RemoteException;

    boolean nvWriteCdmaPrl(byte[] bArr) throws RemoteException;

    boolean nvWriteItem(int i, String str) throws RemoteException;

    void refreshUiccProfile(int i) throws RemoteException;

    void requestModemActivityInfo(ResultReceiver resultReceiver) throws RemoteException;

    int requestNetworkScan(int i, NetworkScanRequest networkScanRequest, Messenger messenger, IBinder iBinder) throws RemoteException;

    void sendDialerSpecialCode(String str, String str2) throws RemoteException;

    String sendEnvelopeWithStatus(int i, String str) throws RemoteException;

    void sendVisualVoicemailSmsForSubscriber(String str, int i, String str2, int i2, String str3, PendingIntent pendingIntent) throws RemoteException;

    int setAllowedCarriers(int i, List<CarrierIdentifier> list) throws RemoteException;

    void setCarrierTestOverride(int i, String str, String str2, String str3, String str4, String str5, String str6, String str7) throws RemoteException;

    void setCellInfoListRate(int i) throws RemoteException;

    void setDataActivationState(int i, int i2) throws RemoteException;

    void setDataEnabledProperties(String str, boolean z) throws RemoteException;

    void setImsRegistrationState(boolean z) throws RemoteException;

    boolean setImsService(int i, boolean z, String str) throws RemoteException;

    boolean setLine1NumberForDisplayForSubscriber(int i, String str, String str2) throws RemoteException;

    void setNetworkSelectionModeAutomatic(int i) throws RemoteException;

    boolean setNetworkSelectionModeManual(int i, String str, boolean z) throws RemoteException;

    boolean setOperatorBrandOverride(int i, String str) throws RemoteException;

    void setPolicyDataEnabled(boolean z, int i) throws RemoteException;

    boolean setPreferredNetworkType(int i, int i2) throws RemoteException;

    boolean setRadio(boolean z) throws RemoteException;

    void setRadioCapability(RadioAccessFamily[] radioAccessFamilyArr) throws RemoteException;

    boolean setRadioForSubscriber(int i, boolean z) throws RemoteException;

    void setRadioIndicationUpdateMode(int i, int i2, int i3) throws RemoteException;

    boolean setRadioPower(boolean z) throws RemoteException;

    boolean setRoamingOverride(int i, List<String> list, List<String> list2, List<String> list3, List<String> list4) throws RemoteException;

    void setSimPowerStateForSlot(int i, int i2) throws RemoteException;

    void setUserDataEnabled(int i, boolean z) throws RemoteException;

    void setVoiceActivationState(int i, int i2) throws RemoteException;

    boolean setVoiceMailNumber(int i, String str, String str2) throws RemoteException;

    void setVoicemailRingtoneUri(String str, PhoneAccountHandle phoneAccountHandle, Uri uri) throws RemoteException;

    void setVoicemailVibrationEnabled(String str, PhoneAccountHandle phoneAccountHandle, boolean z) throws RemoteException;

    void shutdownMobileRadios() throws RemoteException;

    void silenceRinger() throws RemoteException;

    void stopNetworkScan(int i, int i2) throws RemoteException;

    boolean supplyPin(String str) throws RemoteException;

    boolean supplyPinForSubscriber(int i, String str) throws RemoteException;

    int[] supplyPinReportResult(String str) throws RemoteException;

    int[] supplyPinReportResultForSubscriber(int i, String str) throws RemoteException;

    boolean supplyPuk(String str, String str2) throws RemoteException;

    boolean supplyPukForSubscriber(int i, String str, String str2) throws RemoteException;

    int[] supplyPukReportResult(String str, String str2) throws RemoteException;

    int[] supplyPukReportResultForSubscriber(int i, String str, String str2) throws RemoteException;

    boolean switchSlots(int[] iArr) throws RemoteException;

    void toggleRadioOnOff() throws RemoteException;

    void toggleRadioOnOffForSubscriber(int i) throws RemoteException;

    void updateServiceLocation() throws RemoteException;

    void updateServiceLocationForSubscriber(int i) throws RemoteException;
}
