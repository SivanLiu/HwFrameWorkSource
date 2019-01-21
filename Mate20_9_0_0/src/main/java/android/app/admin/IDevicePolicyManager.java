package android.app.admin;

import android.app.IApplicationThread;
import android.app.IServiceConnection;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.IPackageDataObserver;
import android.content.pm.ParceledListSlice;
import android.content.pm.StringParceledListSlice;
import android.graphics.Bitmap;
import android.net.ProxyInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.PersistableBundle;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.UserHandle;
import android.security.keymaster.KeymasterCertificateChain;
import android.security.keystore.ParcelableKeyGenParameterSpec;
import android.telephony.data.ApnSetting;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;

public interface IDevicePolicyManager extends IInterface {

    public static abstract class Stub extends Binder implements IDevicePolicyManager {
        private static final String DESCRIPTOR = "android.app.admin.IDevicePolicyManager";
        static final int TRANSACTION_addCrossProfileIntentFilter = 111;
        static final int TRANSACTION_addCrossProfileWidgetProvider = 167;
        static final int TRANSACTION_addOverrideApn = 258;
        static final int TRANSACTION_addPersistentPreferredActivity = 99;
        static final int TRANSACTION_approveCaCert = 85;
        static final int TRANSACTION_bindDeviceAdminServiceAsUser = 234;
        static final int TRANSACTION_checkProvisioningPreCondition = 189;
        static final int TRANSACTION_choosePrivateKeyAlias = 91;
        static final int TRANSACTION_clearApplicationUserData = 246;
        static final int TRANSACTION_clearCrossProfileIntentFilters = 112;
        static final int TRANSACTION_clearDeviceOwner = 69;
        static final int TRANSACTION_clearPackagePersistentPreferredActivities = 100;
        static final int TRANSACTION_clearProfileOwner = 76;
        static final int TRANSACTION_clearResetPasswordToken = 241;
        static final int TRANSACTION_clearSystemUpdatePolicyFreezePeriodRecord = 178;
        static final int TRANSACTION_createAdminSupportIntent = 124;
        static final int TRANSACTION_createAndManageUser = 127;
        static final int TRANSACTION_enableSystemApp = 134;
        static final int TRANSACTION_enableSystemAppWithIntent = 135;
        static final int TRANSACTION_enforceCanManageCaCerts = 84;
        static final int TRANSACTION_forceRemoveActiveAdmin = 55;
        static final int TRANSACTION_forceSecurityLogs = 220;
        static final int TRANSACTION_forceUpdateUserSetupComplete = 226;
        static final int TRANSACTION_generateKeyPair = 89;
        static final int TRANSACTION_getAccountTypesWithManagementDisabled = 138;
        static final int TRANSACTION_getAccountTypesWithManagementDisabledAsUser = 139;
        static final int TRANSACTION_getActiveAdmins = 51;
        static final int TRANSACTION_getAffiliationIds = 214;
        static final int TRANSACTION_getAlwaysOnVpnPackage = 98;
        static final int TRANSACTION_getApplicationRestrictions = 103;
        static final int TRANSACTION_getApplicationRestrictionsManagingPackage = 105;
        static final int TRANSACTION_getAutoTimeRequired = 171;
        static final int TRANSACTION_getBindDeviceAdminTargetUsers = 235;
        static final int TRANSACTION_getBluetoothContactSharingDisabled = 163;
        static final int TRANSACTION_getBluetoothContactSharingDisabledForUser = 164;
        static final int TRANSACTION_getCameraDisabled = 44;
        static final int TRANSACTION_getCertInstallerPackage = 96;
        static final int TRANSACTION_getCrossProfileCallerIdDisabled = 156;
        static final int TRANSACTION_getCrossProfileCallerIdDisabledForUser = 157;
        static final int TRANSACTION_getCrossProfileContactsSearchDisabled = 159;
        static final int TRANSACTION_getCrossProfileContactsSearchDisabledForUser = 160;
        static final int TRANSACTION_getCrossProfileWidgetProviders = 169;
        static final int TRANSACTION_getCurrentFailedPasswordAttempts = 25;
        static final int TRANSACTION_getDelegatePackages = 94;
        static final int TRANSACTION_getDelegatedScopes = 93;
        static final int TRANSACTION_getDeviceOwnerComponent = 66;
        static final int TRANSACTION_getDeviceOwnerLockScreenInfo = 79;
        static final int TRANSACTION_getDeviceOwnerName = 68;
        static final int TRANSACTION_getDeviceOwnerOrganizationName = 209;
        static final int TRANSACTION_getDeviceOwnerUserId = 70;
        static final int TRANSACTION_getDisallowedSystemApps = 249;
        static final int TRANSACTION_getDoNotAskCredentialsOnBoot = 181;
        static final int TRANSACTION_getEndUserSessionMessage = 255;
        static final int TRANSACTION_getForceEphemeralUsers = 173;
        static final int TRANSACTION_getGlobalProxyAdmin = 37;
        static final int TRANSACTION_getKeepUninstalledPackages = 191;
        static final int TRANSACTION_getKeyguardDisabledFeatures = 48;
        static final int TRANSACTION_getLastBugReportRequestTime = 238;
        static final int TRANSACTION_getLastNetworkLogRetrievalTime = 239;
        static final int TRANSACTION_getLastSecurityLogRetrievalTime = 237;
        static final int TRANSACTION_getLockTaskFeatures = 144;
        static final int TRANSACTION_getLockTaskPackages = 141;
        static final int TRANSACTION_getLongSupportMessage = 199;
        static final int TRANSACTION_getLongSupportMessageForUser = 201;
        static final int TRANSACTION_getMandatoryBackupTransport = 230;
        static final int TRANSACTION_getMaximumFailedPasswordsForWipe = 28;
        static final int TRANSACTION_getMaximumTimeToLock = 31;
        static final int TRANSACTION_getMeteredDataDisabledPackages = 257;
        static final int TRANSACTION_getOrganizationColor = 205;
        static final int TRANSACTION_getOrganizationColorForUser = 206;
        static final int TRANSACTION_getOrganizationName = 208;
        static final int TRANSACTION_getOrganizationNameForUser = 210;
        static final int TRANSACTION_getOverrideApns = 261;
        static final int TRANSACTION_getOwnerInstalledCaCerts = 245;
        static final int TRANSACTION_getPasswordExpiration = 21;
        static final int TRANSACTION_getPasswordExpirationTimeout = 20;
        static final int TRANSACTION_getPasswordHistoryLength = 18;
        static final int TRANSACTION_getPasswordMinimumLength = 4;
        static final int TRANSACTION_getPasswordMinimumLetters = 10;
        static final int TRANSACTION_getPasswordMinimumLowerCase = 8;
        static final int TRANSACTION_getPasswordMinimumNonLetter = 16;
        static final int TRANSACTION_getPasswordMinimumNumeric = 12;
        static final int TRANSACTION_getPasswordMinimumSymbols = 14;
        static final int TRANSACTION_getPasswordMinimumUpperCase = 6;
        static final int TRANSACTION_getPasswordQuality = 2;
        static final int TRANSACTION_getPendingSystemUpdate = 183;
        static final int TRANSACTION_getPermissionGrantState = 187;
        static final int TRANSACTION_getPermissionPolicy = 185;
        static final int TRANSACTION_getPermittedAccessibilityServices = 114;
        static final int TRANSACTION_getPermittedAccessibilityServicesForUser = 115;
        static final int TRANSACTION_getPermittedCrossProfileNotificationListeners = 122;
        static final int TRANSACTION_getPermittedInputMethods = 118;
        static final int TRANSACTION_getPermittedInputMethodsForCurrentUser = 119;
        static final int TRANSACTION_getProfileOwner = 72;
        static final int TRANSACTION_getProfileOwnerName = 73;
        static final int TRANSACTION_getProfileWithMinimumFailedPasswordsForWipe = 26;
        static final int TRANSACTION_getRemoveWarning = 53;
        static final int TRANSACTION_getRequiredStrongAuthTimeout = 33;
        static final int TRANSACTION_getRestrictionsProvider = 108;
        static final int TRANSACTION_getScreenCaptureDisabled = 46;
        static final int TRANSACTION_getSecondaryUsers = 133;
        static final int TRANSACTION_getShortSupportMessage = 197;
        static final int TRANSACTION_getShortSupportMessageForUser = 200;
        static final int TRANSACTION_getStartUserSessionMessage = 254;
        static final int TRANSACTION_getStorageEncryption = 40;
        static final int TRANSACTION_getStorageEncryptionStatus = 41;
        static final int TRANSACTION_getSystemUpdatePolicy = 177;
        static final int TRANSACTION_getTransferOwnershipBundle = 251;
        static final int TRANSACTION_getTrustAgentConfiguration = 166;
        static final int TRANSACTION_getUserProvisioningState = 211;
        static final int TRANSACTION_getUserRestrictions = 110;
        static final int TRANSACTION_getWifiMacAddress = 194;
        static final int TRANSACTION_hasDeviceOwner = 67;
        static final int TRANSACTION_hasGrantedPolicy = 56;
        static final int TRANSACTION_hasUserSetupCompleted = 77;
        static final int TRANSACTION_installCaCert = 82;
        static final int TRANSACTION_installExistingPackage = 136;
        static final int TRANSACTION_installKeyPair = 87;
        static final int TRANSACTION_isAccessibilityServicePermittedByAdmin = 116;
        static final int TRANSACTION_isActivePasswordSufficient = 22;
        static final int TRANSACTION_isAdminActive = 50;
        static final int TRANSACTION_isAffiliatedUser = 215;
        static final int TRANSACTION_isApplicationHidden = 126;
        static final int TRANSACTION_isBackupServiceEnabled = 228;
        static final int TRANSACTION_isCaCertApproved = 86;
        static final int TRANSACTION_isCallerApplicationRestrictionsManagingPackage = 106;
        static final int TRANSACTION_isCurrentInputMethodSetByOwner = 244;
        static final int TRANSACTION_isDeviceProvisioned = 223;
        static final int TRANSACTION_isDeviceProvisioningConfigApplied = 224;
        static final int TRANSACTION_isEphemeralUser = 236;
        static final int TRANSACTION_isInputMethodPermittedByAdmin = 120;
        static final int TRANSACTION_isLockTaskPermitted = 142;
        static final int TRANSACTION_isLogoutEnabled = 248;
        static final int TRANSACTION_isManagedProfile = 192;
        static final int TRANSACTION_isMasterVolumeMuted = 151;
        static final int TRANSACTION_isMeteredDataDisabledPackageForUser = 264;
        static final int TRANSACTION_isNetworkLoggingEnabled = 232;
        static final int TRANSACTION_isNotificationListenerServicePermitted = 123;
        static final int TRANSACTION_isOverrideApnEnabled = 263;
        static final int TRANSACTION_isPackageSuspended = 81;
        static final int TRANSACTION_isProfileActivePasswordSufficientForParent = 23;
        static final int TRANSACTION_isProvisioningAllowed = 188;
        static final int TRANSACTION_isRemovingAdmin = 174;
        static final int TRANSACTION_isResetPasswordTokenActive = 242;
        static final int TRANSACTION_isSecurityLoggingEnabled = 217;
        static final int TRANSACTION_isSeparateProfileChallengeAllowed = 202;
        static final int TRANSACTION_isSystemOnlyUser = 193;
        static final int TRANSACTION_isUninstallBlocked = 154;
        static final int TRANSACTION_isUninstallInQueue = 221;
        static final int TRANSACTION_isUsingUnifiedPassword = 24;
        static final int TRANSACTION_lockNow = 34;
        static final int TRANSACTION_logoutUser = 132;
        static final int TRANSACTION_notifyLockTaskModeChanged = 152;
        static final int TRANSACTION_notifyPendingSystemUpdate = 182;
        static final int TRANSACTION_packageHasActiveAdmins = 52;
        static final int TRANSACTION_reboot = 195;
        static final int TRANSACTION_removeActiveAdmin = 54;
        static final int TRANSACTION_removeCrossProfileWidgetProvider = 168;
        static final int TRANSACTION_removeKeyPair = 88;
        static final int TRANSACTION_removeOverrideApn = 260;
        static final int TRANSACTION_removeUser = 128;
        static final int TRANSACTION_reportFailedFingerprintAttempt = 61;
        static final int TRANSACTION_reportFailedPasswordAttempt = 59;
        static final int TRANSACTION_reportKeyguardDismissed = 63;
        static final int TRANSACTION_reportKeyguardSecured = 64;
        static final int TRANSACTION_reportPasswordChanged = 58;
        static final int TRANSACTION_reportSuccessfulFingerprintAttempt = 62;
        static final int TRANSACTION_reportSuccessfulPasswordAttempt = 60;
        static final int TRANSACTION_requestBugreport = 42;
        static final int TRANSACTION_resetPassword = 29;
        static final int TRANSACTION_resetPasswordWithToken = 243;
        static final int TRANSACTION_retrieveNetworkLogs = 233;
        static final int TRANSACTION_retrievePreRebootSecurityLogs = 219;
        static final int TRANSACTION_retrieveSecurityLogs = 218;
        static final int TRANSACTION_setAccountManagementDisabled = 137;
        static final int TRANSACTION_setActiveAdmin = 49;
        static final int TRANSACTION_setActivePasswordState = 57;
        static final int TRANSACTION_setAffiliationIds = 213;
        static final int TRANSACTION_setAlwaysOnVpnPackage = 97;
        static final int TRANSACTION_setApplicationHidden = 125;
        static final int TRANSACTION_setApplicationRestrictions = 102;
        static final int TRANSACTION_setApplicationRestrictionsManagingPackage = 104;
        static final int TRANSACTION_setAutoTimeRequired = 170;
        static final int TRANSACTION_setBackupServiceEnabled = 227;
        static final int TRANSACTION_setBluetoothContactSharingDisabled = 162;
        static final int TRANSACTION_setCameraDisabled = 43;
        static final int TRANSACTION_setCertInstallerPackage = 95;
        static final int TRANSACTION_setCrossProfileCallerIdDisabled = 155;
        static final int TRANSACTION_setCrossProfileContactsSearchDisabled = 158;
        static final int TRANSACTION_setDefaultSmsApplication = 101;
        static final int TRANSACTION_setDelegatedScopes = 92;
        static final int TRANSACTION_setDeviceOwner = 65;
        static final int TRANSACTION_setDeviceOwnerLockScreenInfo = 78;
        static final int TRANSACTION_setDeviceProvisioningConfigApplied = 225;
        static final int TRANSACTION_setEndUserSessionMessage = 253;
        static final int TRANSACTION_setForceEphemeralUsers = 172;
        static final int TRANSACTION_setGlobalProxy = 36;
        static final int TRANSACTION_setGlobalSetting = 145;
        static final int TRANSACTION_setKeepUninstalledPackages = 190;
        static final int TRANSACTION_setKeyPairCertificate = 90;
        static final int TRANSACTION_setKeyguardDisabled = 179;
        static final int TRANSACTION_setKeyguardDisabledFeatures = 47;
        static final int TRANSACTION_setLockTaskFeatures = 143;
        static final int TRANSACTION_setLockTaskPackages = 140;
        static final int TRANSACTION_setLogoutEnabled = 247;
        static final int TRANSACTION_setLongSupportMessage = 198;
        static final int TRANSACTION_setMandatoryBackupTransport = 229;
        static final int TRANSACTION_setMasterVolumeMuted = 150;
        static final int TRANSACTION_setMaximumFailedPasswordsForWipe = 27;
        static final int TRANSACTION_setMaximumTimeToLock = 30;
        static final int TRANSACTION_setMeteredDataDisabledPackages = 256;
        static final int TRANSACTION_setNetworkLoggingEnabled = 231;
        static final int TRANSACTION_setOrganizationColor = 203;
        static final int TRANSACTION_setOrganizationColorForUser = 204;
        static final int TRANSACTION_setOrganizationName = 207;
        static final int TRANSACTION_setOverrideApnsEnabled = 262;
        static final int TRANSACTION_setPackagesSuspended = 80;
        static final int TRANSACTION_setPasswordExpirationTimeout = 19;
        static final int TRANSACTION_setPasswordHistoryLength = 17;
        static final int TRANSACTION_setPasswordMinimumLength = 3;
        static final int TRANSACTION_setPasswordMinimumLetters = 9;
        static final int TRANSACTION_setPasswordMinimumLowerCase = 7;
        static final int TRANSACTION_setPasswordMinimumNonLetter = 15;
        static final int TRANSACTION_setPasswordMinimumNumeric = 11;
        static final int TRANSACTION_setPasswordMinimumSymbols = 13;
        static final int TRANSACTION_setPasswordMinimumUpperCase = 5;
        static final int TRANSACTION_setPasswordQuality = 1;
        static final int TRANSACTION_setPermissionGrantState = 186;
        static final int TRANSACTION_setPermissionPolicy = 184;
        static final int TRANSACTION_setPermittedAccessibilityServices = 113;
        static final int TRANSACTION_setPermittedCrossProfileNotificationListeners = 121;
        static final int TRANSACTION_setPermittedInputMethods = 117;
        static final int TRANSACTION_setProfileEnabled = 74;
        static final int TRANSACTION_setProfileName = 75;
        static final int TRANSACTION_setProfileOwner = 71;
        static final int TRANSACTION_setRecommendedGlobalProxy = 38;
        static final int TRANSACTION_setRequiredStrongAuthTimeout = 32;
        static final int TRANSACTION_setResetPasswordToken = 240;
        static final int TRANSACTION_setRestrictionsProvider = 107;
        static final int TRANSACTION_setScreenCaptureDisabled = 45;
        static final int TRANSACTION_setSecureSetting = 147;
        static final int TRANSACTION_setSecurityLoggingEnabled = 216;
        static final int TRANSACTION_setShortSupportMessage = 196;
        static final int TRANSACTION_setStartUserSessionMessage = 252;
        static final int TRANSACTION_setStatusBarDisabled = 180;
        static final int TRANSACTION_setStorageEncryption = 39;
        static final int TRANSACTION_setSystemSetting = 146;
        static final int TRANSACTION_setSystemUpdatePolicy = 176;
        static final int TRANSACTION_setTime = 148;
        static final int TRANSACTION_setTimeZone = 149;
        static final int TRANSACTION_setTrustAgentConfiguration = 165;
        static final int TRANSACTION_setUninstallBlocked = 153;
        static final int TRANSACTION_setUserIcon = 175;
        static final int TRANSACTION_setUserProvisioningState = 212;
        static final int TRANSACTION_setUserRestriction = 109;
        static final int TRANSACTION_startManagedQuickContact = 161;
        static final int TRANSACTION_startUserInBackground = 130;
        static final int TRANSACTION_stopUser = 131;
        static final int TRANSACTION_switchUser = 129;
        static final int TRANSACTION_transferOwnership = 250;
        static final int TRANSACTION_uninstallCaCerts = 83;
        static final int TRANSACTION_uninstallPackageWithActiveAdmins = 222;
        static final int TRANSACTION_updateOverrideApn = 259;
        static final int TRANSACTION_wipeDataWithReason = 35;

        private static class Proxy implements IDevicePolicyManager {
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

            public void setPasswordQuality(ComponentName who, int quality, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(quality);
                    _data.writeInt(parent);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getPasswordQuality(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    _data.writeInt(parent);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setPasswordMinimumLength(ComponentName who, int length, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(length);
                    _data.writeInt(parent);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getPasswordMinimumLength(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    _data.writeInt(parent);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setPasswordMinimumUpperCase(ComponentName who, int length, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(length);
                    _data.writeInt(parent);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getPasswordMinimumUpperCase(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    _data.writeInt(parent);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setPasswordMinimumLowerCase(ComponentName who, int length, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(length);
                    _data.writeInt(parent);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getPasswordMinimumLowerCase(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    _data.writeInt(parent);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setPasswordMinimumLetters(ComponentName who, int length, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(length);
                    _data.writeInt(parent);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getPasswordMinimumLetters(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    _data.writeInt(parent);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setPasswordMinimumNumeric(ComponentName who, int length, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(length);
                    _data.writeInt(parent);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getPasswordMinimumNumeric(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    _data.writeInt(parent);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setPasswordMinimumSymbols(ComponentName who, int length, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(length);
                    _data.writeInt(parent);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getPasswordMinimumSymbols(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    _data.writeInt(parent);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setPasswordMinimumNonLetter(ComponentName who, int length, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(length);
                    _data.writeInt(parent);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getPasswordMinimumNonLetter(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    _data.writeInt(parent);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setPasswordHistoryLength(ComponentName who, int length, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(length);
                    _data.writeInt(parent);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getPasswordHistoryLength(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    _data.writeInt(parent);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setPasswordExpirationTimeout(ComponentName who, long expiration, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeLong(expiration);
                    _data.writeInt(parent);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public long getPasswordExpirationTimeout(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    _data.writeInt(parent);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public long getPasswordExpiration(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    _data.writeInt(parent);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isActivePasswordSufficient(int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    _data.writeInt(parent);
                    boolean z = false;
                    this.mRemote.transact(22, _data, _reply, 0);
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

            public boolean isProfileActivePasswordSufficientForParent(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    boolean z = false;
                    this.mRemote.transact(23, _data, _reply, 0);
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

            public boolean isUsingUnifiedPassword(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(24, _data, _reply, 0);
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

            public int getCurrentFailedPasswordAttempts(int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    _data.writeInt(parent);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getProfileWithMinimumFailedPasswordsForWipe(int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    _data.writeInt(parent);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setMaximumFailedPasswordsForWipe(ComponentName admin, int num, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(num);
                    _data.writeInt(parent);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getMaximumFailedPasswordsForWipe(ComponentName admin, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    _data.writeInt(parent);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean resetPassword(String password, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(password);
                    _data.writeInt(flags);
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

            public void setMaximumTimeToLock(ComponentName who, long timeMs, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeLong(timeMs);
                    _data.writeInt(parent);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public long getMaximumTimeToLock(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    _data.writeInt(parent);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setRequiredStrongAuthTimeout(ComponentName who, long timeMs, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeLong(timeMs);
                    _data.writeInt(parent);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public long getRequiredStrongAuthTimeout(ComponentName who, int userId, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userId);
                    _data.writeInt(parent);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void lockNow(int flags, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(flags);
                    _data.writeInt(parent);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void wipeDataWithReason(int flags, String wipeReasonForUser) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(flags);
                    _data.writeString(wipeReasonForUser);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public ComponentName setGlobalProxy(ComponentName admin, String proxySpec, String exclusionList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    ComponentName _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(proxySpec);
                    _data.writeString(exclusionList);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ComponentName) ComponentName.CREATOR.createFromParcel(_reply);
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

            public ComponentName getGlobalProxyAdmin(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    ComponentName _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ComponentName) ComponentName.CREATOR.createFromParcel(_reply);
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

            public void setRecommendedGlobalProxy(ComponentName admin, ProxyInfo proxyInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (proxyInfo != null) {
                        _data.writeInt(1);
                        proxyInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(38, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int setStorageEncryption(ComponentName who, boolean encrypt) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(encrypt);
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean getStorageEncryption(ComponentName who, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    this.mRemote.transact(40, _data, _reply, 0);
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

            public int getStorageEncryptionStatus(String callerPackage, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callerPackage);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(41, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean requestBugreport(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(42, _data, _reply, 0);
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

            public void setCameraDisabled(ComponentName who, boolean disabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(disabled);
                    this.mRemote.transact(43, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean getCameraDisabled(ComponentName who, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    this.mRemote.transact(44, _data, _reply, 0);
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

            public void setScreenCaptureDisabled(ComponentName who, boolean disabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(disabled);
                    this.mRemote.transact(45, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean getScreenCaptureDisabled(ComponentName who, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    this.mRemote.transact(46, _data, _reply, 0);
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

            public void setKeyguardDisabledFeatures(ComponentName who, int which, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(which);
                    _data.writeInt(parent);
                    this.mRemote.transact(47, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getKeyguardDisabledFeatures(ComponentName who, int userHandle, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    _data.writeInt(parent);
                    this.mRemote.transact(48, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setActiveAdmin(ComponentName policyReceiver, boolean refreshing, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (policyReceiver != null) {
                        _data.writeInt(1);
                        policyReceiver.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(refreshing);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(49, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isAdminActive(ComponentName policyReceiver, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (policyReceiver != null) {
                        _data.writeInt(1);
                        policyReceiver.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    this.mRemote.transact(50, _data, _reply, 0);
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

            public List<ComponentName> getActiveAdmins(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(51, _data, _reply, 0);
                    _reply.readException();
                    List<ComponentName> _result = _reply.createTypedArrayList(ComponentName.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean packageHasActiveAdmins(String packageName, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userHandle);
                    boolean z = false;
                    this.mRemote.transact(52, _data, _reply, 0);
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

            public void getRemoveWarning(ComponentName policyReceiver, RemoteCallback result, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (policyReceiver != null) {
                        _data.writeInt(1);
                        policyReceiver.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (result != null) {
                        _data.writeInt(1);
                        result.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    this.mRemote.transact(53, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeActiveAdmin(ComponentName policyReceiver, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (policyReceiver != null) {
                        _data.writeInt(1);
                        policyReceiver.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    this.mRemote.transact(54, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void forceRemoveActiveAdmin(ComponentName policyReceiver, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (policyReceiver != null) {
                        _data.writeInt(1);
                        policyReceiver.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    this.mRemote.transact(55, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean hasGrantedPolicy(ComponentName policyReceiver, int usesPolicy, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (policyReceiver != null) {
                        _data.writeInt(1);
                        policyReceiver.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(usesPolicy);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(56, _data, _reply, 0);
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

            public void setActivePasswordState(PasswordMetrics metrics, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (metrics != null) {
                        _data.writeInt(1);
                        metrics.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    this.mRemote.transact(57, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void reportPasswordChanged(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(58, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void reportFailedPasswordAttempt(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(59, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void reportSuccessfulPasswordAttempt(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(60, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void reportFailedFingerprintAttempt(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(61, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void reportSuccessfulFingerprintAttempt(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(62, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void reportKeyguardDismissed(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(63, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void reportKeyguardSecured(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(64, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setDeviceOwner(ComponentName who, String ownerName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(ownerName);
                    _data.writeInt(userId);
                    this.mRemote.transact(65, _data, _reply, 0);
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

            public ComponentName getDeviceOwnerComponent(boolean callingUserOnly) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    ComponentName _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(callingUserOnly);
                    this.mRemote.transact(66, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ComponentName) ComponentName.CREATOR.createFromParcel(_reply);
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

            public boolean hasDeviceOwner() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
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

            public String getDeviceOwnerName() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(68, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void clearDeviceOwner(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(69, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getDeviceOwnerUserId() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(70, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setProfileOwner(ComponentName who, String ownerName, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(ownerName);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(71, _data, _reply, 0);
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

            public ComponentName getProfileOwner(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    ComponentName _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(72, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ComponentName) ComponentName.CREATOR.createFromParcel(_reply);
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

            public String getProfileOwnerName(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(73, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setProfileEnabled(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(74, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setProfileName(ComponentName who, String profileName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(profileName);
                    this.mRemote.transact(75, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void clearProfileOwner(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(76, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean hasUserSetupCompleted() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(77, _data, _reply, 0);
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

            public void setDeviceOwnerLockScreenInfo(ComponentName who, CharSequence deviceOwnerInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (deviceOwnerInfo != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(deviceOwnerInfo, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(78, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public CharSequence getDeviceOwnerLockScreenInfo() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    CharSequence _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(79, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(_reply);
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

            public String[] setPackagesSuspended(ComponentName admin, String callerPackage, String[] packageNames, boolean suspended) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callerPackage);
                    _data.writeStringArray(packageNames);
                    _data.writeInt(suspended);
                    this.mRemote.transact(80, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isPackageSuspended(ComponentName admin, String callerPackage, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callerPackage);
                    _data.writeString(packageName);
                    this.mRemote.transact(81, _data, _reply, 0);
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

            public boolean installCaCert(ComponentName admin, String callerPackage, byte[] certBuffer) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callerPackage);
                    _data.writeByteArray(certBuffer);
                    this.mRemote.transact(82, _data, _reply, 0);
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

            public void uninstallCaCerts(ComponentName admin, String callerPackage, String[] aliases) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callerPackage);
                    _data.writeStringArray(aliases);
                    this.mRemote.transact(83, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void enforceCanManageCaCerts(ComponentName admin, String callerPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callerPackage);
                    this.mRemote.transact(84, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean approveCaCert(String alias, int userHandle, boolean approval) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(alias);
                    _data.writeInt(userHandle);
                    _data.writeInt(approval);
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

            public boolean isCaCertApproved(String alias, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(alias);
                    _data.writeInt(userHandle);
                    boolean z = false;
                    this.mRemote.transact(86, _data, _reply, 0);
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

            public boolean installKeyPair(ComponentName who, String callerPackage, byte[] privKeyBuffer, byte[] certBuffer, byte[] certChainBuffer, String alias, boolean requestAccess, boolean isUserSelectable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callerPackage);
                    _data.writeByteArray(privKeyBuffer);
                    _data.writeByteArray(certBuffer);
                    _data.writeByteArray(certChainBuffer);
                    _data.writeString(alias);
                    _data.writeInt(requestAccess);
                    _data.writeInt(isUserSelectable);
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

            public boolean removeKeyPair(ComponentName who, String callerPackage, String alias) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callerPackage);
                    _data.writeString(alias);
                    this.mRemote.transact(88, _data, _reply, 0);
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

            public boolean generateKeyPair(ComponentName who, String callerPackage, String algorithm, ParcelableKeyGenParameterSpec keySpec, int idAttestationFlags, KeymasterCertificateChain attestationChain) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callerPackage);
                    _data.writeString(algorithm);
                    if (keySpec != null) {
                        _data.writeInt(1);
                        keySpec.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(idAttestationFlags);
                    this.mRemote.transact(89, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    if (_reply.readInt() != 0) {
                        attestationChain.readFromParcel(_reply);
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setKeyPairCertificate(ComponentName who, String callerPackage, String alias, byte[] certBuffer, byte[] certChainBuffer, boolean isUserSelectable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callerPackage);
                    _data.writeString(alias);
                    _data.writeByteArray(certBuffer);
                    _data.writeByteArray(certChainBuffer);
                    _data.writeInt(isUserSelectable);
                    this.mRemote.transact(90, _data, _reply, 0);
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

            public void choosePrivateKeyAlias(int uid, Uri uri, String alias, IBinder aliasCallback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    if (uri != null) {
                        _data.writeInt(1);
                        uri.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(alias);
                    _data.writeStrongBinder(aliasCallback);
                    this.mRemote.transact(91, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setDelegatedScopes(ComponentName who, String delegatePackage, List<String> scopes) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(delegatePackage);
                    _data.writeStringList(scopes);
                    this.mRemote.transact(92, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<String> getDelegatedScopes(ComponentName who, String delegatePackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(delegatePackage);
                    this.mRemote.transact(93, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<String> getDelegatePackages(ComponentName who, String scope) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(scope);
                    this.mRemote.transact(94, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setCertInstallerPackage(ComponentName who, String installerPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(installerPackage);
                    this.mRemote.transact(95, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getCertInstallerPackage(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(96, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setAlwaysOnVpnPackage(ComponentName who, String vpnPackage, boolean lockdown) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(vpnPackage);
                    _data.writeInt(lockdown);
                    this.mRemote.transact(97, _data, _reply, 0);
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

            public String getAlwaysOnVpnPackage(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(98, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void addPersistentPreferredActivity(ComponentName admin, IntentFilter filter, ComponentName activity) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (filter != null) {
                        _data.writeInt(1);
                        filter.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (activity != null) {
                        _data.writeInt(1);
                        activity.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(99, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void clearPackagePersistentPreferredActivities(ComponentName admin, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(100, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setDefaultSmsApplication(ComponentName admin, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(101, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setApplicationRestrictions(ComponentName who, String callerPackage, String packageName, Bundle settings) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callerPackage);
                    _data.writeString(packageName);
                    if (settings != null) {
                        _data.writeInt(1);
                        settings.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(102, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public Bundle getApplicationRestrictions(ComponentName who, String callerPackage, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Bundle _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callerPackage);
                    _data.writeString(packageName);
                    this.mRemote.transact(103, _data, _reply, 0);
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

            public boolean setApplicationRestrictionsManagingPackage(ComponentName admin, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(104, _data, _reply, 0);
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

            public String getApplicationRestrictionsManagingPackage(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(105, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isCallerApplicationRestrictionsManagingPackage(String callerPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callerPackage);
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

            public void setRestrictionsProvider(ComponentName who, ComponentName provider) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (provider != null) {
                        _data.writeInt(1);
                        provider.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(107, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public ComponentName getRestrictionsProvider(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    ComponentName _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(108, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ComponentName) ComponentName.CREATOR.createFromParcel(_reply);
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

            public void setUserRestriction(ComponentName who, String key, boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(key);
                    _data.writeInt(enable);
                    this.mRemote.transact(109, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public Bundle getUserRestrictions(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Bundle _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(110, _data, _reply, 0);
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

            public void addCrossProfileIntentFilter(ComponentName admin, IntentFilter filter, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (filter != null) {
                        _data.writeInt(1);
                        filter.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(flags);
                    this.mRemote.transact(111, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void clearCrossProfileIntentFilters(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(112, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setPermittedAccessibilityServices(ComponentName admin, List packageList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeList(packageList);
                    this.mRemote.transact(113, _data, _reply, 0);
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

            public List getPermittedAccessibilityServices(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(114, _data, _reply, 0);
                    _reply.readException();
                    ArrayList _result = _reply.readArrayList(getClass().getClassLoader());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List getPermittedAccessibilityServicesForUser(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(115, _data, _reply, 0);
                    _reply.readException();
                    ArrayList _result = _reply.readArrayList(getClass().getClassLoader());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isAccessibilityServicePermittedByAdmin(ComponentName admin, String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(116, _data, _reply, 0);
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

            public boolean setPermittedInputMethods(ComponentName admin, List packageList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeList(packageList);
                    this.mRemote.transact(117, _data, _reply, 0);
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

            public List getPermittedInputMethods(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(118, _data, _reply, 0);
                    _reply.readException();
                    ArrayList _result = _reply.readArrayList(getClass().getClassLoader());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List getPermittedInputMethodsForCurrentUser() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(119, _data, _reply, 0);
                    _reply.readException();
                    ArrayList _result = _reply.readArrayList(getClass().getClassLoader());
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isInputMethodPermittedByAdmin(ComponentName admin, String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(120, _data, _reply, 0);
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

            public boolean setPermittedCrossProfileNotificationListeners(ComponentName admin, List<String> packageList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStringList(packageList);
                    this.mRemote.transact(121, _data, _reply, 0);
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

            public List<String> getPermittedCrossProfileNotificationListeners(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(122, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isNotificationListenerServicePermitted(String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    boolean z = false;
                    this.mRemote.transact(123, _data, _reply, 0);
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

            public Intent createAdminSupportIntent(String restriction) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Intent _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(restriction);
                    this.mRemote.transact(124, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Intent) Intent.CREATOR.createFromParcel(_reply);
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

            public boolean setApplicationHidden(ComponentName admin, String callerPackage, String packageName, boolean hidden) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callerPackage);
                    _data.writeString(packageName);
                    _data.writeInt(hidden);
                    this.mRemote.transact(125, _data, _reply, 0);
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

            public boolean isApplicationHidden(ComponentName admin, String callerPackage, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callerPackage);
                    _data.writeString(packageName);
                    this.mRemote.transact(126, _data, _reply, 0);
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

            public UserHandle createAndManageUser(ComponentName who, String name, ComponentName profileOwner, PersistableBundle adminExtras, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    UserHandle _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(name);
                    if (profileOwner != null) {
                        _data.writeInt(1);
                        profileOwner.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (adminExtras != null) {
                        _data.writeInt(1);
                        adminExtras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(flags);
                    this.mRemote.transact(127, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (UserHandle) UserHandle.CREATOR.createFromParcel(_reply);
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

            public boolean removeUser(ComponentName who, UserHandle userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (userHandle != null) {
                        _data.writeInt(1);
                        userHandle.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(128, _data, _reply, 0);
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

            public boolean switchUser(ComponentName who, UserHandle userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (userHandle != null) {
                        _data.writeInt(1);
                        userHandle.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(129, _data, _reply, 0);
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

            public int startUserInBackground(ComponentName who, UserHandle userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (userHandle != null) {
                        _data.writeInt(1);
                        userHandle.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(130, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int stopUser(ComponentName who, UserHandle userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (userHandle != null) {
                        _data.writeInt(1);
                        userHandle.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(131, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int logoutUser(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(132, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<UserHandle> getSecondaryUsers(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(133, _data, _reply, 0);
                    _reply.readException();
                    List<UserHandle> _result = _reply.createTypedArrayList(UserHandle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void enableSystemApp(ComponentName admin, String callerPackage, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callerPackage);
                    _data.writeString(packageName);
                    this.mRemote.transact(134, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int enableSystemAppWithIntent(ComponentName admin, String callerPackage, Intent intent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callerPackage);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(135, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean installExistingPackage(ComponentName admin, String callerPackage, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callerPackage);
                    _data.writeString(packageName);
                    this.mRemote.transact(136, _data, _reply, 0);
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

            public void setAccountManagementDisabled(ComponentName who, String accountType, boolean disabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(accountType);
                    _data.writeInt(disabled);
                    this.mRemote.transact(137, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String[] getAccountTypesWithManagementDisabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(138, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String[] getAccountTypesWithManagementDisabledAsUser(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    this.mRemote.transact(139, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setLockTaskPackages(ComponentName who, String[] packages) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStringArray(packages);
                    this.mRemote.transact(140, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String[] getLockTaskPackages(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(141, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isLockTaskPermitted(String pkg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
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

            public void setLockTaskFeatures(ComponentName who, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(flags);
                    this.mRemote.transact(143, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getLockTaskFeatures(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(144, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setGlobalSetting(ComponentName who, String setting, String value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(setting);
                    _data.writeString(value);
                    this.mRemote.transact(145, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setSystemSetting(ComponentName who, String setting, String value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(setting);
                    _data.writeString(value);
                    this.mRemote.transact(146, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setSecureSetting(ComponentName who, String setting, String value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(setting);
                    _data.writeString(value);
                    this.mRemote.transact(147, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setTime(ComponentName who, long millis) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeLong(millis);
                    this.mRemote.transact(148, _data, _reply, 0);
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

            public boolean setTimeZone(ComponentName who, String timeZone) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(timeZone);
                    this.mRemote.transact(149, _data, _reply, 0);
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

            public void setMasterVolumeMuted(ComponentName admin, boolean on) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(on);
                    this.mRemote.transact(150, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isMasterVolumeMuted(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(151, _data, _reply, 0);
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

            public void notifyLockTaskModeChanged(boolean isEnabled, String pkg, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(isEnabled);
                    _data.writeString(pkg);
                    _data.writeInt(userId);
                    this.mRemote.transact(152, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setUninstallBlocked(ComponentName admin, String callerPackage, String packageName, boolean uninstallBlocked) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callerPackage);
                    _data.writeString(packageName);
                    _data.writeInt(uninstallBlocked);
                    this.mRemote.transact(153, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isUninstallBlocked(ComponentName admin, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(154, _data, _reply, 0);
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

            public void setCrossProfileCallerIdDisabled(ComponentName who, boolean disabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(disabled);
                    this.mRemote.transact(155, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean getCrossProfileCallerIdDisabled(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(156, _data, _reply, 0);
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

            public boolean getCrossProfileCallerIdDisabledForUser(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    boolean z = false;
                    this.mRemote.transact(157, _data, _reply, 0);
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

            public void setCrossProfileContactsSearchDisabled(ComponentName who, boolean disabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(disabled);
                    this.mRemote.transact(158, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean getCrossProfileContactsSearchDisabled(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(159, _data, _reply, 0);
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

            public boolean getCrossProfileContactsSearchDisabledForUser(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    boolean z = false;
                    this.mRemote.transact(160, _data, _reply, 0);
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

            public void startManagedQuickContact(String lookupKey, long contactId, boolean isContactIdIgnored, long directoryId, Intent originalIntent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(lookupKey);
                    _data.writeLong(contactId);
                    _data.writeInt(isContactIdIgnored);
                    _data.writeLong(directoryId);
                    if (originalIntent != null) {
                        _data.writeInt(1);
                        originalIntent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(161, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setBluetoothContactSharingDisabled(ComponentName who, boolean disabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(disabled);
                    this.mRemote.transact(162, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean getBluetoothContactSharingDisabled(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(163, _data, _reply, 0);
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

            public boolean getBluetoothContactSharingDisabledForUser(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    boolean z = false;
                    this.mRemote.transact(164, _data, _reply, 0);
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

            public void setTrustAgentConfiguration(ComponentName admin, ComponentName agent, PersistableBundle args, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (agent != null) {
                        _data.writeInt(1);
                        agent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (args != null) {
                        _data.writeInt(1);
                        args.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(parent);
                    this.mRemote.transact(165, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<PersistableBundle> getTrustAgentConfiguration(ComponentName admin, ComponentName agent, int userId, boolean parent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (agent != null) {
                        _data.writeInt(1);
                        agent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userId);
                    _data.writeInt(parent);
                    this.mRemote.transact(166, _data, _reply, 0);
                    _reply.readException();
                    List<PersistableBundle> _result = _reply.createTypedArrayList(PersistableBundle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean addCrossProfileWidgetProvider(ComponentName admin, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(167, _data, _reply, 0);
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

            public boolean removeCrossProfileWidgetProvider(ComponentName admin, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(168, _data, _reply, 0);
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

            public List<String> getCrossProfileWidgetProviders(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(169, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setAutoTimeRequired(ComponentName who, boolean required) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(required);
                    this.mRemote.transact(170, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean getAutoTimeRequired() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(171, _data, _reply, 0);
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

            public void setForceEphemeralUsers(ComponentName who, boolean forceEpehemeralUsers) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(forceEpehemeralUsers);
                    this.mRemote.transact(172, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean getForceEphemeralUsers(ComponentName who) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(173, _data, _reply, 0);
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

            public boolean isRemovingAdmin(ComponentName adminReceiver, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (adminReceiver != null) {
                        _data.writeInt(1);
                        adminReceiver.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    this.mRemote.transact(174, _data, _reply, 0);
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

            public void setUserIcon(ComponentName admin, Bitmap icon) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (icon != null) {
                        _data.writeInt(1);
                        icon.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(175, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setSystemUpdatePolicy(ComponentName who, SystemUpdatePolicy policy) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (policy != null) {
                        _data.writeInt(1);
                        policy.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(176, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public SystemUpdatePolicy getSystemUpdatePolicy() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    SystemUpdatePolicy _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(177, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (SystemUpdatePolicy) SystemUpdatePolicy.CREATOR.createFromParcel(_reply);
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

            public void clearSystemUpdatePolicyFreezePeriodRecord() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(178, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setKeyguardDisabled(ComponentName admin, boolean disabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(disabled);
                    this.mRemote.transact(179, _data, _reply, 0);
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

            public boolean setStatusBarDisabled(ComponentName who, boolean disabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (who != null) {
                        _data.writeInt(1);
                        who.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(disabled);
                    this.mRemote.transact(180, _data, _reply, 0);
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

            public boolean getDoNotAskCredentialsOnBoot() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(181, _data, _reply, 0);
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

            public void notifyPendingSystemUpdate(SystemUpdateInfo info) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (info != null) {
                        _data.writeInt(1);
                        info.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(182, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public SystemUpdateInfo getPendingSystemUpdate(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    SystemUpdateInfo _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(183, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (SystemUpdateInfo) SystemUpdateInfo.CREATOR.createFromParcel(_reply);
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

            public void setPermissionPolicy(ComponentName admin, String callerPackage, int policy) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callerPackage);
                    _data.writeInt(policy);
                    this.mRemote.transact(184, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getPermissionPolicy(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(185, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setPermissionGrantState(ComponentName admin, String callerPackage, String packageName, String permission, int grantState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callerPackage);
                    _data.writeString(packageName);
                    _data.writeString(permission);
                    _data.writeInt(grantState);
                    this.mRemote.transact(186, _data, _reply, 0);
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

            public int getPermissionGrantState(ComponentName admin, String callerPackage, String packageName, String permission) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callerPackage);
                    _data.writeString(packageName);
                    _data.writeString(permission);
                    this.mRemote.transact(187, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isProvisioningAllowed(String action, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(action);
                    _data.writeString(packageName);
                    boolean z = false;
                    this.mRemote.transact(188, _data, _reply, 0);
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

            public int checkProvisioningPreCondition(String action, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(action);
                    _data.writeString(packageName);
                    this.mRemote.transact(189, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setKeepUninstalledPackages(ComponentName admin, String callerPackage, List<String> packageList) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callerPackage);
                    _data.writeStringList(packageList);
                    this.mRemote.transact(190, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<String> getKeepUninstalledPackages(ComponentName admin, String callerPackage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(callerPackage);
                    this.mRemote.transact(191, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isManagedProfile(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(192, _data, _reply, 0);
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

            public boolean isSystemOnlyUser(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(193, _data, _reply, 0);
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

            public String getWifiMacAddress(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(194, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void reboot(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(195, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setShortSupportMessage(ComponentName admin, CharSequence message) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (message != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(message, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(196, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public CharSequence getShortSupportMessage(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    CharSequence _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(197, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(_reply);
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

            public void setLongSupportMessage(ComponentName admin, CharSequence message) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (message != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(message, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(198, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public CharSequence getLongSupportMessage(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    CharSequence _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(199, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(_reply);
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

            public CharSequence getShortSupportMessageForUser(ComponentName admin, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    CharSequence _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    this.mRemote.transact(200, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(_reply);
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

            public CharSequence getLongSupportMessageForUser(ComponentName admin, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    CharSequence _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userHandle);
                    this.mRemote.transact(201, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(_reply);
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

            public boolean isSeparateProfileChallengeAllowed(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    boolean z = false;
                    this.mRemote.transact(202, _data, _reply, 0);
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

            public void setOrganizationColor(ComponentName admin, int color) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(color);
                    this.mRemote.transact(203, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setOrganizationColorForUser(int color, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(color);
                    _data.writeInt(userId);
                    this.mRemote.transact(204, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getOrganizationColor(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(205, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getOrganizationColorForUser(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(206, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setOrganizationName(ComponentName admin, CharSequence title) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (title != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(title, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(207, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public CharSequence getOrganizationName(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    CharSequence _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(208, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(_reply);
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

            public CharSequence getDeviceOwnerOrganizationName() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    CharSequence _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(209, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(_reply);
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

            public CharSequence getOrganizationNameForUser(int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    CharSequence _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(210, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(_reply);
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

            public int getUserProvisioningState() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(211, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setUserProvisioningState(int state, int userHandle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(state);
                    _data.writeInt(userHandle);
                    this.mRemote.transact(212, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setAffiliationIds(ComponentName admin, List<String> ids) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStringList(ids);
                    this.mRemote.transact(213, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<String> getAffiliationIds(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(214, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isAffiliatedUser() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(215, _data, _reply, 0);
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

            public void setSecurityLoggingEnabled(ComponentName admin, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(enabled);
                    this.mRemote.transact(216, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isSecurityLoggingEnabled(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(217, _data, _reply, 0);
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

            public ParceledListSlice retrieveSecurityLogs(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    ParceledListSlice _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(218, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ParceledListSlice) ParceledListSlice.CREATOR.createFromParcel(_reply);
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

            public ParceledListSlice retrievePreRebootSecurityLogs(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    ParceledListSlice _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(219, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ParceledListSlice) ParceledListSlice.CREATOR.createFromParcel(_reply);
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

            public long forceSecurityLogs() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(220, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isUninstallInQueue(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    boolean z = false;
                    this.mRemote.transact(221, _data, _reply, 0);
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

            public void uninstallPackageWithActiveAdmins(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(222, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isDeviceProvisioned() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(223, _data, _reply, 0);
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

            public boolean isDeviceProvisioningConfigApplied() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(224, _data, _reply, 0);
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

            public void setDeviceProvisioningConfigApplied() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(225, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void forceUpdateUserSetupComplete() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(226, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setBackupServiceEnabled(ComponentName admin, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(enabled);
                    this.mRemote.transact(227, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isBackupServiceEnabled(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(228, _data, _reply, 0);
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

            public boolean setMandatoryBackupTransport(ComponentName admin, ComponentName backupTransportComponent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (backupTransportComponent != null) {
                        _data.writeInt(1);
                        backupTransportComponent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(229, _data, _reply, 0);
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

            public ComponentName getMandatoryBackupTransport() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    ComponentName _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(230, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ComponentName) ComponentName.CREATOR.createFromParcel(_reply);
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

            public void setNetworkLoggingEnabled(ComponentName admin, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(enabled);
                    this.mRemote.transact(231, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isNetworkLoggingEnabled(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(232, _data, _reply, 0);
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

            public List<NetworkEvent> retrieveNetworkLogs(ComponentName admin, long batchToken) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeLong(batchToken);
                    this.mRemote.transact(233, _data, _reply, 0);
                    _reply.readException();
                    List<NetworkEvent> _result = _reply.createTypedArrayList(NetworkEvent.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean bindDeviceAdminServiceAsUser(ComponentName admin, IApplicationThread caller, IBinder token, Intent service, IServiceConnection connection, int flags, int targetUserId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    IBinder iBinder = null;
                    _data.writeStrongBinder(caller != null ? caller.asBinder() : null);
                    _data.writeStrongBinder(token);
                    if (service != null) {
                        _data.writeInt(1);
                        service.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (connection != null) {
                        iBinder = connection.asBinder();
                    }
                    _data.writeStrongBinder(iBinder);
                    _data.writeInt(flags);
                    _data.writeInt(targetUserId);
                    this.mRemote.transact(234, _data, _reply, 0);
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

            public List<UserHandle> getBindDeviceAdminTargetUsers(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(235, _data, _reply, 0);
                    _reply.readException();
                    List<UserHandle> _result = _reply.createTypedArrayList(UserHandle.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isEphemeralUser(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(236, _data, _reply, 0);
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

            public long getLastSecurityLogRetrievalTime() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(237, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public long getLastBugReportRequestTime() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(238, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public long getLastNetworkLogRetrievalTime() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(239, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setResetPasswordToken(ComponentName admin, byte[] token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeByteArray(token);
                    this.mRemote.transact(240, _data, _reply, 0);
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

            public boolean clearResetPasswordToken(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(241, _data, _reply, 0);
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

            public boolean isResetPasswordTokenActive(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(242, _data, _reply, 0);
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

            public boolean resetPasswordWithToken(ComponentName admin, String password, byte[] token, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(password);
                    _data.writeByteArray(token);
                    _data.writeInt(flags);
                    this.mRemote.transact(243, _data, _reply, 0);
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

            public boolean isCurrentInputMethodSetByOwner() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(244, _data, _reply, 0);
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

            public StringParceledListSlice getOwnerInstalledCaCerts(UserHandle user) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    StringParceledListSlice _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (user != null) {
                        _data.writeInt(1);
                        user.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(245, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (StringParceledListSlice) StringParceledListSlice.CREATOR.createFromParcel(_reply);
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

            public void clearApplicationUserData(ComponentName admin, String packageName, IPackageDataObserver callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(246, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setLogoutEnabled(ComponentName admin, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(enabled);
                    this.mRemote.transact(247, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isLogoutEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(248, _data, _reply, 0);
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

            public List<String> getDisallowedSystemApps(ComponentName admin, int userId, String provisioningAction) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userId);
                    _data.writeString(provisioningAction);
                    this.mRemote.transact(249, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void transferOwnership(ComponentName admin, ComponentName target, PersistableBundle bundle) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (target != null) {
                        _data.writeInt(1);
                        target.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (bundle != null) {
                        _data.writeInt(1);
                        bundle.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(250, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public PersistableBundle getTransferOwnershipBundle() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    PersistableBundle _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(251, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (PersistableBundle) PersistableBundle.CREATOR.createFromParcel(_reply);
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

            public void setStartUserSessionMessage(ComponentName admin, CharSequence startUserSessionMessage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (startUserSessionMessage != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(startUserSessionMessage, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(252, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setEndUserSessionMessage(ComponentName admin, CharSequence endUserSessionMessage) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (endUserSessionMessage != null) {
                        _data.writeInt(1);
                        TextUtils.writeToParcel(endUserSessionMessage, _data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(253, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public CharSequence getStartUserSessionMessage(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    CharSequence _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(254, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(_reply);
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

            public CharSequence getEndUserSessionMessage(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    CharSequence _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(255, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(_reply);
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

            public List<String> setMeteredDataDisabledPackages(ComponentName admin, List<String> packageNames) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStringList(packageNames);
                    this.mRemote.transact(256, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<String> getMeteredDataDisabledPackages(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(257, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int addOverrideApn(ComponentName admin, ApnSetting apnSetting) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (apnSetting != null) {
                        _data.writeInt(1);
                        apnSetting.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(258, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean updateOverrideApn(ComponentName admin, int apnId, ApnSetting apnSetting) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(apnId);
                    if (apnSetting != null) {
                        _data.writeInt(1);
                        apnSetting.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(259, _data, _reply, 0);
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

            public boolean removeOverrideApn(ComponentName admin, int apnId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(apnId);
                    this.mRemote.transact(260, _data, _reply, 0);
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

            public List<ApnSetting> getOverrideApns(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(261, _data, _reply, 0);
                    _reply.readException();
                    List<ApnSetting> _result = _reply.createTypedArrayList(ApnSetting.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setOverrideApnsEnabled(ComponentName admin, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(enabled);
                    this.mRemote.transact(262, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isOverrideApnEnabled(ComponentName admin) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(263, _data, _reply, 0);
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

            public boolean isMeteredDataDisabledPackageForUser(ComponentName admin, String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (admin != null) {
                        _data.writeInt(1);
                        admin.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(264, _data, _reply, 0);
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

        public static IDevicePolicyManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IDevicePolicyManager)) {
                return new Proxy(obj);
            }
            return (IDevicePolicyManager) iin;
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
                boolean _arg5 = false;
                ComponentName _arg0 = null;
                int _arg1;
                int _result;
                long _arg12;
                long _result2;
                int _arg02;
                boolean _result3;
                boolean _result4;
                int _result5;
                ComponentName _result6;
                ComponentName _result7;
                boolean _result8;
                boolean _result9;
                String _result10;
                String _result11;
                CharSequence _arg13;
                String _arg14;
                String _arg2;
                Uri _arg15;
                List<String> _result12;
                String _arg16;
                List _result13;
                List<String> _result14;
                boolean _result15;
                UserHandle _arg17;
                List<UserHandle> _result16;
                String[] _result17;
                ComponentName _arg18;
                PersistableBundle _arg22;
                SystemUpdatePolicy _arg19;
                CharSequence _result18;
                CharSequence _result19;
                ParceledListSlice _result20;
                long _result21;
                ApnSetting _arg110;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setPasswordQuality(_arg0, _arg1, _arg5);
                        reply.writeNoException();
                        return true;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        _result = getPasswordQuality(_arg0, _arg1, _arg5);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 3:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setPasswordMinimumLength(_arg0, _arg1, _arg5);
                        reply.writeNoException();
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        _result = getPasswordMinimumLength(_arg0, _arg1, _arg5);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setPasswordMinimumUpperCase(_arg0, _arg1, _arg5);
                        reply.writeNoException();
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        _result = getPasswordMinimumUpperCase(_arg0, _arg1, _arg5);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setPasswordMinimumLowerCase(_arg0, _arg1, _arg5);
                        reply.writeNoException();
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        _result = getPasswordMinimumLowerCase(_arg0, _arg1, _arg5);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setPasswordMinimumLetters(_arg0, _arg1, _arg5);
                        reply.writeNoException();
                        return true;
                    case 10:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        _result = getPasswordMinimumLetters(_arg0, _arg1, _arg5);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setPasswordMinimumNumeric(_arg0, _arg1, _arg5);
                        reply.writeNoException();
                        return true;
                    case 12:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        _result = getPasswordMinimumNumeric(_arg0, _arg1, _arg5);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 13:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setPasswordMinimumSymbols(_arg0, _arg1, _arg5);
                        reply.writeNoException();
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        _result = getPasswordMinimumSymbols(_arg0, _arg1, _arg5);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setPasswordMinimumNonLetter(_arg0, _arg1, _arg5);
                        reply.writeNoException();
                        return true;
                    case 16:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        _result = getPasswordMinimumNonLetter(_arg0, _arg1, _arg5);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setPasswordHistoryLength(_arg0, _arg1, _arg5);
                        reply.writeNoException();
                        return true;
                    case 18:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        _result = getPasswordHistoryLength(_arg0, _arg1, _arg5);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 19:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg12 = data.readLong();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setPasswordExpirationTimeout(_arg0, _arg12, _arg5);
                        reply.writeNoException();
                        return true;
                    case 20:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        _result2 = getPasswordExpirationTimeout(_arg0, _arg1, _arg5);
                        reply.writeNoException();
                        parcel2.writeLong(_result2);
                        return true;
                    case 21:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        _result2 = getPasswordExpiration(_arg0, _arg1, _arg5);
                        reply.writeNoException();
                        parcel2.writeLong(_result2);
                        return true;
                    case 22:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        _result3 = isActivePasswordSufficient(_arg02, _arg5);
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 23:
                        parcel.enforceInterface(descriptor);
                        _result4 = isProfileActivePasswordSufficientForParent(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 24:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result4 = isUsingUnifiedPassword(_arg0);
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 25:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        _result5 = getCurrentFailedPasswordAttempts(_arg02, _arg5);
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case 26:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        _result5 = getProfileWithMinimumFailedPasswordsForWipe(_arg02, _arg5);
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case 27:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setMaximumFailedPasswordsForWipe(_arg0, _arg1, _arg5);
                        reply.writeNoException();
                        return true;
                    case 28:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        _result = getMaximumFailedPasswordsForWipe(_arg0, _arg1, _arg5);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 29:
                        parcel.enforceInterface(descriptor);
                        _result3 = resetPassword(data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 30:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg12 = data.readLong();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setMaximumTimeToLock(_arg0, _arg12, _arg5);
                        reply.writeNoException();
                        return true;
                    case 31:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        _result2 = getMaximumTimeToLock(_arg0, _arg1, _arg5);
                        reply.writeNoException();
                        parcel2.writeLong(_result2);
                        return true;
                    case 32:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg12 = data.readLong();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setRequiredStrongAuthTimeout(_arg0, _arg12, _arg5);
                        reply.writeNoException();
                        return true;
                    case 33:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        _result2 = getRequiredStrongAuthTimeout(_arg0, _arg1, _arg5);
                        reply.writeNoException();
                        parcel2.writeLong(_result2);
                        return true;
                    case 34:
                        parcel.enforceInterface(descriptor);
                        _arg02 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        lockNow(_arg02, _arg5);
                        reply.writeNoException();
                        return true;
                    case 35:
                        parcel.enforceInterface(descriptor);
                        wipeDataWithReason(data.readInt(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 36:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result6 = setGlobalProxy(_arg0, data.readString(), data.readString());
                        reply.writeNoException();
                        if (_result6 != null) {
                            parcel2.writeInt(1);
                            _result6.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 37:
                        parcel.enforceInterface(descriptor);
                        _result7 = getGlobalProxyAdmin(data.readInt());
                        reply.writeNoException();
                        if (_result7 != null) {
                            parcel2.writeInt(1);
                            _result7.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 38:
                        ProxyInfo _arg111;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result7 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _result7 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg111 = (ProxyInfo) ProxyInfo.CREATOR.createFromParcel(parcel);
                        }
                        setRecommendedGlobalProxy(_result7, _arg111);
                        reply.writeNoException();
                        return true;
                    case 39:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        _result5 = setStorageEncryption(_arg0, _arg5);
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case 40:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result3 = getStorageEncryption(_arg0, data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 41:
                        parcel.enforceInterface(descriptor);
                        _result5 = getStorageEncryptionStatus(data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case 42:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result4 = requestBugreport(_arg0);
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 43:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setCameraDisabled(_arg0, _arg5);
                        reply.writeNoException();
                        return true;
                    case 44:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result3 = getCameraDisabled(_arg0, data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 45:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setScreenCaptureDisabled(_arg0, _arg5);
                        reply.writeNoException();
                        return true;
                    case 46:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result3 = getScreenCaptureDisabled(_arg0, data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 47:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setKeyguardDisabledFeatures(_arg0, _arg1, _arg5);
                        reply.writeNoException();
                        return true;
                    case 48:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        _result = getKeyguardDisabledFeatures(_arg0, _arg1, _arg5);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 49:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setActiveAdmin(_arg0, _arg5, data.readInt());
                        reply.writeNoException();
                        return true;
                    case 50:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result3 = isAdminActive(_arg0, data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 51:
                        parcel.enforceInterface(descriptor);
                        List<ComponentName> _result22 = getActiveAdmins(data.readInt());
                        reply.writeNoException();
                        parcel2.writeTypedList(_result22);
                        return true;
                    case 52:
                        parcel.enforceInterface(descriptor);
                        _result3 = packageHasActiveAdmins(data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 53:
                        RemoteCallback _arg112;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result7 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _result7 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg112 = (RemoteCallback) RemoteCallback.CREATOR.createFromParcel(parcel);
                        }
                        getRemoveWarning(_result7, _arg112, data.readInt());
                        reply.writeNoException();
                        return true;
                    case 54:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        removeActiveAdmin(_arg0, data.readInt());
                        reply.writeNoException();
                        return true;
                    case 55:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        forceRemoveActiveAdmin(_arg0, data.readInt());
                        reply.writeNoException();
                        return true;
                    case 56:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result8 = hasGrantedPolicy(_arg0, data.readInt(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result8);
                        return true;
                    case 57:
                        PasswordMetrics _arg03;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (PasswordMetrics) PasswordMetrics.CREATOR.createFromParcel(parcel);
                        }
                        setActivePasswordState(_arg03, data.readInt());
                        reply.writeNoException();
                        return true;
                    case 58:
                        parcel.enforceInterface(descriptor);
                        reportPasswordChanged(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 59:
                        parcel.enforceInterface(descriptor);
                        reportFailedPasswordAttempt(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 60:
                        parcel.enforceInterface(descriptor);
                        reportSuccessfulPasswordAttempt(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 61:
                        parcel.enforceInterface(descriptor);
                        reportFailedFingerprintAttempt(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 62:
                        parcel.enforceInterface(descriptor);
                        reportSuccessfulFingerprintAttempt(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 63:
                        parcel.enforceInterface(descriptor);
                        reportKeyguardDismissed(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 64:
                        parcel.enforceInterface(descriptor);
                        reportKeyguardSecured(data.readInt());
                        reply.writeNoException();
                        return true;
                    case 65:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result8 = setDeviceOwner(_arg0, data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result8);
                        return true;
                    case 66:
                        parcel.enforceInterface(descriptor);
                        _result7 = getDeviceOwnerComponent(data.readInt() != 0);
                        reply.writeNoException();
                        if (_result7 != null) {
                            parcel2.writeInt(1);
                            _result7.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 67:
                        parcel.enforceInterface(descriptor);
                        _result9 = hasDeviceOwner();
                        reply.writeNoException();
                        parcel2.writeInt(_result9);
                        return true;
                    case 68:
                        parcel.enforceInterface(descriptor);
                        _result10 = getDeviceOwnerName();
                        reply.writeNoException();
                        parcel2.writeString(_result10);
                        return true;
                    case 69:
                        parcel.enforceInterface(descriptor);
                        clearDeviceOwner(data.readString());
                        reply.writeNoException();
                        return true;
                    case 70:
                        parcel.enforceInterface(descriptor);
                        _arg02 = getDeviceOwnerUserId();
                        reply.writeNoException();
                        parcel2.writeInt(_arg02);
                        return true;
                    case 71:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result8 = setProfileOwner(_arg0, data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result8);
                        return true;
                    case 72:
                        parcel.enforceInterface(descriptor);
                        _result7 = getProfileOwner(data.readInt());
                        reply.writeNoException();
                        if (_result7 != null) {
                            parcel2.writeInt(1);
                            _result7.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 73:
                        parcel.enforceInterface(descriptor);
                        _result11 = getProfileOwnerName(data.readInt());
                        reply.writeNoException();
                        parcel2.writeString(_result11);
                        return true;
                    case 74:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        setProfileEnabled(_arg0);
                        reply.writeNoException();
                        return true;
                    case 75:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        setProfileName(_arg0, data.readString());
                        reply.writeNoException();
                        return true;
                    case 76:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        clearProfileOwner(_arg0);
                        reply.writeNoException();
                        return true;
                    case 77:
                        parcel.enforceInterface(descriptor);
                        _result9 = hasUserSetupCompleted();
                        reply.writeNoException();
                        parcel2.writeInt(_result9);
                        return true;
                    case 78:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result7 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _result7 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg13 = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
                        }
                        setDeviceOwnerLockScreenInfo(_result7, _arg13);
                        reply.writeNoException();
                        return true;
                    case 79:
                        parcel.enforceInterface(descriptor);
                        _arg13 = getDeviceOwnerLockScreenInfo();
                        reply.writeNoException();
                        if (_arg13 != null) {
                            parcel2.writeInt(1);
                            TextUtils.writeToParcel(_arg13, parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 80:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result11 = data.readString();
                        String[] _arg23 = data.createStringArray();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        String[] _result23 = setPackagesSuspended(_arg0, _result11, _arg23, _arg5);
                        reply.writeNoException();
                        parcel2.writeStringArray(_result23);
                        return true;
                    case 81:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result8 = isPackageSuspended(_arg0, data.readString(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result8);
                        return true;
                    case 82:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result8 = installCaCert(_arg0, data.readString(), data.createByteArray());
                        reply.writeNoException();
                        parcel2.writeInt(_result8);
                        return true;
                    case 83:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        uninstallCaCerts(_arg0, data.readString(), data.createStringArray());
                        reply.writeNoException();
                        return true;
                    case 84:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        enforceCanManageCaCerts(_arg0, data.readString());
                        reply.writeNoException();
                        return true;
                    case 85:
                        parcel.enforceInterface(descriptor);
                        _result10 = data.readString();
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        _result8 = approveCaCert(_result10, _arg1, _arg5);
                        reply.writeNoException();
                        parcel2.writeInt(_result8);
                        return true;
                    case 86:
                        parcel.enforceInterface(descriptor);
                        _result3 = isCaCertApproved(data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 87:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result9 = installKeyPair(_arg0, data.readString(), data.createByteArray(), data.createByteArray(), data.createByteArray(), data.readString(), data.readInt() != 0, data.readInt() != 0);
                        reply.writeNoException();
                        parcel2.writeInt(_result9);
                        return true;
                    case 88:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result8 = removeKeyPair(_arg0, data.readString(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result8);
                        return true;
                    case 89:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result7 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _result7 = null;
                        }
                        _arg14 = data.readString();
                        _arg2 = data.readString();
                        if (data.readInt() != 0) {
                            _arg15 = (ParcelableKeyGenParameterSpec) ParcelableKeyGenParameterSpec.CREATOR.createFromParcel(parcel);
                        }
                        ParcelableKeyGenParameterSpec _arg3 = _arg15;
                        int _arg4 = data.readInt();
                        KeymasterCertificateChain _arg52 = new KeymasterCertificateChain();
                        KeymasterCertificateChain _arg53 = _arg52;
                        _result9 = generateKeyPair(_result7, _arg14, _arg2, _arg3, _arg4, _arg52);
                        reply.writeNoException();
                        parcel2.writeInt(_result9);
                        parcel2.writeInt(1);
                        _arg53.writeToParcel(parcel2, 1);
                        return true;
                    case 90:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg15 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        Uri _arg04 = _arg15;
                        _arg14 = data.readString();
                        _arg2 = data.readString();
                        byte[] _arg32 = data.createByteArray();
                        byte[] _arg42 = data.createByteArray();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        _result9 = setKeyPairCertificate(_arg04, _arg14, _arg2, _arg32, _arg42, _arg5);
                        reply.writeNoException();
                        parcel2.writeInt(_result9);
                        return true;
                    case 91:
                        parcel.enforceInterface(descriptor);
                        _arg1 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg15 = (Uri) Uri.CREATOR.createFromParcel(parcel);
                        }
                        choosePrivateKeyAlias(_arg1, _arg15, data.readString(), data.readStrongBinder());
                        reply.writeNoException();
                        return true;
                    case 92:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        setDelegatedScopes(_arg0, data.readString(), data.createStringArrayList());
                        reply.writeNoException();
                        return true;
                    case 93:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result12 = getDelegatedScopes(_arg0, data.readString());
                        reply.writeNoException();
                        parcel2.writeStringList(_result12);
                        return true;
                    case 94:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result12 = getDelegatePackages(_arg0, data.readString());
                        reply.writeNoException();
                        parcel2.writeStringList(_result12);
                        return true;
                    case 95:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        setCertInstallerPackage(_arg0, data.readString());
                        reply.writeNoException();
                        return true;
                    case 96:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result11 = getCertInstallerPackage(_arg0);
                        reply.writeNoException();
                        parcel2.writeString(_result11);
                        return true;
                    case 97:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result11 = data.readString();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        _result8 = setAlwaysOnVpnPackage(_arg0, _result11, _arg5);
                        reply.writeNoException();
                        parcel2.writeInt(_result8);
                        return true;
                    case 98:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result11 = getAlwaysOnVpnPackage(_arg0);
                        reply.writeNoException();
                        parcel2.writeString(_result11);
                        return true;
                    case 99:
                        IntentFilter _arg113;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result7 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _result7 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg113 = (IntentFilter) IntentFilter.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg113 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        addPersistentPreferredActivity(_result7, _arg113, _arg0);
                        reply.writeNoException();
                        return true;
                    case 100:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        clearPackagePersistentPreferredActivities(_arg0, data.readString());
                        reply.writeNoException();
                        return true;
                    case 101:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        setDefaultSmsApplication(_arg0, data.readString());
                        reply.writeNoException();
                        return true;
                    case 102:
                        Bundle _arg33;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result7 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _result7 = null;
                        }
                        _arg16 = data.readString();
                        String _arg24 = data.readString();
                        if (data.readInt() != 0) {
                            _arg33 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        }
                        setApplicationRestrictions(_result7, _arg16, _arg24, _arg33);
                        reply.writeNoException();
                        return true;
                    case 103:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        Bundle _result24 = getApplicationRestrictions(_arg0, data.readString(), data.readString());
                        reply.writeNoException();
                        if (_result24 != null) {
                            parcel2.writeInt(1);
                            _result24.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 104:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result3 = setApplicationRestrictionsManagingPackage(_arg0, data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 105:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result11 = getApplicationRestrictionsManagingPackage(_arg0);
                        reply.writeNoException();
                        parcel2.writeString(_result11);
                        return true;
                    case 106:
                        parcel.enforceInterface(descriptor);
                        _result4 = isCallerApplicationRestrictionsManagingPackage(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 107:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result7 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _result7 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        setRestrictionsProvider(_result7, _arg0);
                        reply.writeNoException();
                        return true;
                    case 108:
                        parcel.enforceInterface(descriptor);
                        _result7 = getRestrictionsProvider(data.readInt());
                        reply.writeNoException();
                        if (_result7 != null) {
                            parcel2.writeInt(1);
                            _result7.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 109:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result11 = data.readString();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setUserRestriction(_arg0, _result11, _arg5);
                        reply.writeNoException();
                        return true;
                    case 110:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        Bundle _result25 = getUserRestrictions(_arg0);
                        reply.writeNoException();
                        if (_result25 != null) {
                            parcel2.writeInt(1);
                            _result25.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 111:
                        IntentFilter _arg114;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result7 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _result7 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg114 = (IntentFilter) IntentFilter.CREATOR.createFromParcel(parcel);
                        }
                        addCrossProfileIntentFilter(_result7, _arg114, data.readInt());
                        reply.writeNoException();
                        return true;
                    case 112:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        clearCrossProfileIntentFilters(_arg0);
                        reply.writeNoException();
                        return true;
                    case 113:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result8 = setPermittedAccessibilityServices(_arg0, parcel.readArrayList(getClass().getClassLoader()));
                        reply.writeNoException();
                        parcel2.writeInt(_result8);
                        return true;
                    case 114:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result13 = getPermittedAccessibilityServices(_arg0);
                        reply.writeNoException();
                        parcel2.writeList(_result13);
                        return true;
                    case 115:
                        parcel.enforceInterface(descriptor);
                        _result13 = getPermittedAccessibilityServicesForUser(data.readInt());
                        reply.writeNoException();
                        parcel2.writeList(_result13);
                        return true;
                    case 116:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result8 = isAccessibilityServicePermittedByAdmin(_arg0, data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result8);
                        return true;
                    case 117:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result8 = setPermittedInputMethods(_arg0, parcel.readArrayList(getClass().getClassLoader()));
                        reply.writeNoException();
                        parcel2.writeInt(_result8);
                        return true;
                    case 118:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result13 = getPermittedInputMethods(_arg0);
                        reply.writeNoException();
                        parcel2.writeList(_result13);
                        return true;
                    case 119:
                        parcel.enforceInterface(descriptor);
                        List _result26 = getPermittedInputMethodsForCurrentUser();
                        reply.writeNoException();
                        parcel2.writeList(_result26);
                        return true;
                    case 120:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result8 = isInputMethodPermittedByAdmin(_arg0, data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result8);
                        return true;
                    case 121:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result3 = setPermittedCrossProfileNotificationListeners(_arg0, data.createStringArrayList());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 122:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result14 = getPermittedCrossProfileNotificationListeners(_arg0);
                        reply.writeNoException();
                        parcel2.writeStringList(_result14);
                        return true;
                    case 123:
                        parcel.enforceInterface(descriptor);
                        _result3 = isNotificationListenerServicePermitted(data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 124:
                        parcel.enforceInterface(descriptor);
                        Intent _result27 = createAdminSupportIntent(data.readString());
                        reply.writeNoException();
                        if (_result27 != null) {
                            parcel2.writeInt(1);
                            _result27.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 125:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result11 = data.readString();
                        _arg16 = data.readString();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        _result15 = setApplicationHidden(_arg0, _result11, _arg16, _arg5);
                        reply.writeNoException();
                        parcel2.writeInt(_result15);
                        return true;
                    case 126:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result8 = isApplicationHidden(_arg0, data.readString(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result8);
                        return true;
                    case 127:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result7 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _result7 = null;
                        }
                        _arg14 = data.readString();
                        if (data.readInt() != 0) {
                            _result6 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _result6 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg17 = (PersistableBundle) PersistableBundle.CREATOR.createFromParcel(parcel);
                        }
                        _arg17 = createAndManageUser(_result7, _arg14, _result6, _arg17, data.readInt());
                        reply.writeNoException();
                        if (_arg17 != null) {
                            parcel2.writeInt(1);
                            _arg17.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 128:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result7 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _result7 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg17 = (UserHandle) UserHandle.CREATOR.createFromParcel(parcel);
                        }
                        _result3 = removeUser(_result7, _arg17);
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 129:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result7 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _result7 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg17 = (UserHandle) UserHandle.CREATOR.createFromParcel(parcel);
                        }
                        _result3 = switchUser(_result7, _arg17);
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 130:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result7 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _result7 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg17 = (UserHandle) UserHandle.CREATOR.createFromParcel(parcel);
                        }
                        _result5 = startUserInBackground(_result7, _arg17);
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case 131:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result7 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _result7 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg17 = (UserHandle) UserHandle.CREATOR.createFromParcel(parcel);
                        }
                        _result5 = stopUser(_result7, _arg17);
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case 132:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = logoutUser(_arg0);
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 133:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result16 = getSecondaryUsers(_arg0);
                        reply.writeNoException();
                        parcel2.writeTypedList(_result16);
                        return true;
                    case 134:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        enableSystemApp(_arg0, data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 135:
                        Intent _arg25;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result7 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _result7 = null;
                        }
                        _arg16 = data.readString();
                        if (data.readInt() != 0) {
                            _arg25 = (Intent) Intent.CREATOR.createFromParcel(parcel);
                        }
                        _result = enableSystemAppWithIntent(_result7, _arg16, _arg25);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 136:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result8 = installExistingPackage(_arg0, data.readString(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result8);
                        return true;
                    case 137:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result11 = data.readString();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setAccountManagementDisabled(_arg0, _result11, _arg5);
                        reply.writeNoException();
                        return true;
                    case 138:
                        parcel.enforceInterface(descriptor);
                        String[] _result28 = getAccountTypesWithManagementDisabled();
                        reply.writeNoException();
                        parcel2.writeStringArray(_result28);
                        return true;
                    case 139:
                        parcel.enforceInterface(descriptor);
                        _result17 = getAccountTypesWithManagementDisabledAsUser(data.readInt());
                        reply.writeNoException();
                        parcel2.writeStringArray(_result17);
                        return true;
                    case 140:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        setLockTaskPackages(_arg0, data.createStringArray());
                        reply.writeNoException();
                        return true;
                    case 141:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result17 = getLockTaskPackages(_arg0);
                        reply.writeNoException();
                        parcel2.writeStringArray(_result17);
                        return true;
                    case 142:
                        parcel.enforceInterface(descriptor);
                        _result4 = isLockTaskPermitted(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 143:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        setLockTaskFeatures(_arg0, data.readInt());
                        reply.writeNoException();
                        return true;
                    case 144:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = getLockTaskFeatures(_arg0);
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 145:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        setGlobalSetting(_arg0, data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 146:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        setSystemSetting(_arg0, data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 147:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        setSecureSetting(_arg0, data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 148:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result8 = setTime(_arg0, data.readLong());
                        reply.writeNoException();
                        parcel2.writeInt(_result8);
                        return true;
                    case 149:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result3 = setTimeZone(_arg0, data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 150:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setMasterVolumeMuted(_arg0, _arg5);
                        reply.writeNoException();
                        return true;
                    case 151:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result4 = isMasterVolumeMuted(_arg0);
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 152:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        notifyLockTaskModeChanged(_arg5, data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 153:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result11 = data.readString();
                        _arg16 = data.readString();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setUninstallBlocked(_arg0, _result11, _arg16, _arg5);
                        reply.writeNoException();
                        return true;
                    case 154:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result3 = isUninstallBlocked(_arg0, data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 155:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setCrossProfileCallerIdDisabled(_arg0, _arg5);
                        reply.writeNoException();
                        return true;
                    case 156:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result4 = getCrossProfileCallerIdDisabled(_arg0);
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 157:
                        parcel.enforceInterface(descriptor);
                        _result4 = getCrossProfileCallerIdDisabledForUser(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 158:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setCrossProfileContactsSearchDisabled(_arg0, _arg5);
                        reply.writeNoException();
                        return true;
                    case 159:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result4 = getCrossProfileContactsSearchDisabled(_arg0);
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 160:
                        parcel.enforceInterface(descriptor);
                        _result4 = getCrossProfileContactsSearchDisabledForUser(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 161:
                        parcel.enforceInterface(descriptor);
                        _arg2 = data.readString();
                        long _arg115 = data.readLong();
                        _result15 = data.readInt() != 0;
                        long _arg34 = data.readLong();
                        if (data.readInt() != 0) {
                            _arg0 = (Intent) Intent.CREATOR.createFromParcel(parcel);
                        }
                        startManagedQuickContact(_arg2, _arg115, _result15, _arg34, _arg0);
                        reply.writeNoException();
                        return true;
                    case 162:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setBluetoothContactSharingDisabled(_arg0, _arg5);
                        reply.writeNoException();
                        return true;
                    case 163:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result4 = getBluetoothContactSharingDisabled(_arg0);
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 164:
                        parcel.enforceInterface(descriptor);
                        _result4 = getBluetoothContactSharingDisabledForUser(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 165:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result7 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _result7 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg18 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg18 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg22 = (PersistableBundle) PersistableBundle.CREATOR.createFromParcel(parcel);
                        }
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setTrustAgentConfiguration(_result7, _arg18, _arg22, _arg5);
                        reply.writeNoException();
                        return true;
                    case 166:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result7 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _result7 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result5 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        List<PersistableBundle> _result29 = getTrustAgentConfiguration(_result7, _arg0, _result5, _arg5);
                        reply.writeNoException();
                        parcel2.writeTypedList(_result29);
                        return true;
                    case 167:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result3 = addCrossProfileWidgetProvider(_arg0, data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 168:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result3 = removeCrossProfileWidgetProvider(_arg0, data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 169:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result14 = getCrossProfileWidgetProviders(_arg0);
                        reply.writeNoException();
                        parcel2.writeStringList(_result14);
                        return true;
                    case 170:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setAutoTimeRequired(_arg0, _arg5);
                        reply.writeNoException();
                        return true;
                    case 171:
                        parcel.enforceInterface(descriptor);
                        _result9 = getAutoTimeRequired();
                        reply.writeNoException();
                        parcel2.writeInt(_result9);
                        return true;
                    case 172:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setForceEphemeralUsers(_arg0, _arg5);
                        reply.writeNoException();
                        return true;
                    case 173:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result4 = getForceEphemeralUsers(_arg0);
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 174:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result3 = isRemovingAdmin(_arg0, data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 175:
                        Bitmap _arg116;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result7 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _result7 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg116 = (Bitmap) Bitmap.CREATOR.createFromParcel(parcel);
                        }
                        setUserIcon(_result7, _arg116);
                        reply.writeNoException();
                        return true;
                    case 176:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result7 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _result7 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg19 = (SystemUpdatePolicy) SystemUpdatePolicy.CREATOR.createFromParcel(parcel);
                        }
                        setSystemUpdatePolicy(_result7, _arg19);
                        reply.writeNoException();
                        return true;
                    case 177:
                        parcel.enforceInterface(descriptor);
                        _arg19 = getSystemUpdatePolicy();
                        reply.writeNoException();
                        if (_arg19 != null) {
                            parcel2.writeInt(1);
                            _arg19.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 178:
                        parcel.enforceInterface(descriptor);
                        clearSystemUpdatePolicyFreezePeriodRecord();
                        reply.writeNoException();
                        return true;
                    case 179:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        _result3 = setKeyguardDisabled(_arg0, _arg5);
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 180:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        _result3 = setStatusBarDisabled(_arg0, _arg5);
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 181:
                        parcel.enforceInterface(descriptor);
                        _result9 = getDoNotAskCredentialsOnBoot();
                        reply.writeNoException();
                        parcel2.writeInt(_result9);
                        return true;
                    case 182:
                        SystemUpdateInfo _arg05;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg05 = (SystemUpdateInfo) SystemUpdateInfo.CREATOR.createFromParcel(parcel);
                        }
                        notifyPendingSystemUpdate(_arg05);
                        reply.writeNoException();
                        return true;
                    case 183:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        SystemUpdateInfo _result30 = getPendingSystemUpdate(_arg0);
                        reply.writeNoException();
                        if (_result30 != null) {
                            parcel2.writeInt(1);
                            _result30.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 184:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        setPermissionPolicy(_arg0, data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 185:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = getPermissionPolicy(_arg0);
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 186:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result9 = setPermissionGrantState(_arg0, data.readString(), data.readString(), data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result9);
                        return true;
                    case 187:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        int _result31 = getPermissionGrantState(_arg0, data.readString(), data.readString(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result31);
                        return true;
                    case 188:
                        parcel.enforceInterface(descriptor);
                        _result3 = isProvisioningAllowed(data.readString(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 189:
                        parcel.enforceInterface(descriptor);
                        _result5 = checkProvisioningPreCondition(data.readString(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case 190:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        setKeepUninstalledPackages(_arg0, data.readString(), data.createStringArrayList());
                        reply.writeNoException();
                        return true;
                    case 191:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result12 = getKeepUninstalledPackages(_arg0, data.readString());
                        reply.writeNoException();
                        parcel2.writeStringList(_result12);
                        return true;
                    case 192:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result4 = isManagedProfile(_arg0);
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 193:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result4 = isSystemOnlyUser(_arg0);
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 194:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result11 = getWifiMacAddress(_arg0);
                        reply.writeNoException();
                        parcel2.writeString(_result11);
                        return true;
                    case 195:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        reboot(_arg0);
                        reply.writeNoException();
                        return true;
                    case 196:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result7 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _result7 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg13 = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
                        }
                        setShortSupportMessage(_result7, _arg13);
                        reply.writeNoException();
                        return true;
                    case 197:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result18 = getShortSupportMessage(_arg0);
                        reply.writeNoException();
                        if (_result18 != null) {
                            parcel2.writeInt(1);
                            TextUtils.writeToParcel(_result18, parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 198:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result7 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _result7 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg13 = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
                        }
                        setLongSupportMessage(_result7, _arg13);
                        reply.writeNoException();
                        return true;
                    case 199:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result18 = getLongSupportMessage(_arg0);
                        reply.writeNoException();
                        if (_result18 != null) {
                            parcel2.writeInt(1);
                            TextUtils.writeToParcel(_result18, parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 200:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result19 = getShortSupportMessageForUser(_arg0, data.readInt());
                        reply.writeNoException();
                        if (_result19 != null) {
                            parcel2.writeInt(1);
                            TextUtils.writeToParcel(_result19, parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 201:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result19 = getLongSupportMessageForUser(_arg0, data.readInt());
                        reply.writeNoException();
                        if (_result19 != null) {
                            parcel2.writeInt(1);
                            TextUtils.writeToParcel(_result19, parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 202:
                        parcel.enforceInterface(descriptor);
                        _result4 = isSeparateProfileChallengeAllowed(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 203:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        setOrganizationColor(_arg0, data.readInt());
                        reply.writeNoException();
                        return true;
                    case 204:
                        parcel.enforceInterface(descriptor);
                        setOrganizationColorForUser(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 205:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _arg1 = getOrganizationColor(_arg0);
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 206:
                        parcel.enforceInterface(descriptor);
                        _arg1 = getOrganizationColorForUser(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 207:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result7 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _result7 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg13 = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
                        }
                        setOrganizationName(_result7, _arg13);
                        reply.writeNoException();
                        return true;
                    case 208:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result18 = getOrganizationName(_arg0);
                        reply.writeNoException();
                        if (_result18 != null) {
                            parcel2.writeInt(1);
                            TextUtils.writeToParcel(_result18, parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 209:
                        parcel.enforceInterface(descriptor);
                        _arg13 = getDeviceOwnerOrganizationName();
                        reply.writeNoException();
                        if (_arg13 != null) {
                            parcel2.writeInt(1);
                            TextUtils.writeToParcel(_arg13, parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 210:
                        parcel.enforceInterface(descriptor);
                        _result18 = getOrganizationNameForUser(data.readInt());
                        reply.writeNoException();
                        if (_result18 != null) {
                            parcel2.writeInt(1);
                            TextUtils.writeToParcel(_result18, parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 211:
                        parcel.enforceInterface(descriptor);
                        _arg02 = getUserProvisioningState();
                        reply.writeNoException();
                        parcel2.writeInt(_arg02);
                        return true;
                    case 212:
                        parcel.enforceInterface(descriptor);
                        setUserProvisioningState(data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 213:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        setAffiliationIds(_arg0, data.createStringArrayList());
                        reply.writeNoException();
                        return true;
                    case 214:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result14 = getAffiliationIds(_arg0);
                        reply.writeNoException();
                        parcel2.writeStringList(_result14);
                        return true;
                    case 215:
                        parcel.enforceInterface(descriptor);
                        _result9 = isAffiliatedUser();
                        reply.writeNoException();
                        parcel2.writeInt(_result9);
                        return true;
                    case 216:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setSecurityLoggingEnabled(_arg0, _arg5);
                        reply.writeNoException();
                        return true;
                    case 217:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result4 = isSecurityLoggingEnabled(_arg0);
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 218:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result20 = retrieveSecurityLogs(_arg0);
                        reply.writeNoException();
                        if (_result20 != null) {
                            parcel2.writeInt(1);
                            _result20.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 219:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result20 = retrievePreRebootSecurityLogs(_arg0);
                        reply.writeNoException();
                        if (_result20 != null) {
                            parcel2.writeInt(1);
                            _result20.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 220:
                        parcel.enforceInterface(descriptor);
                        _result21 = forceSecurityLogs();
                        reply.writeNoException();
                        parcel2.writeLong(_result21);
                        return true;
                    case 221:
                        parcel.enforceInterface(descriptor);
                        _result4 = isUninstallInQueue(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 222:
                        parcel.enforceInterface(descriptor);
                        uninstallPackageWithActiveAdmins(data.readString());
                        reply.writeNoException();
                        return true;
                    case 223:
                        parcel.enforceInterface(descriptor);
                        _result9 = isDeviceProvisioned();
                        reply.writeNoException();
                        parcel2.writeInt(_result9);
                        return true;
                    case 224:
                        parcel.enforceInterface(descriptor);
                        _result9 = isDeviceProvisioningConfigApplied();
                        reply.writeNoException();
                        parcel2.writeInt(_result9);
                        return true;
                    case 225:
                        parcel.enforceInterface(descriptor);
                        setDeviceProvisioningConfigApplied();
                        reply.writeNoException();
                        return true;
                    case 226:
                        parcel.enforceInterface(descriptor);
                        forceUpdateUserSetupComplete();
                        reply.writeNoException();
                        return true;
                    case 227:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setBackupServiceEnabled(_arg0, _arg5);
                        reply.writeNoException();
                        return true;
                    case 228:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result4 = isBackupServiceEnabled(_arg0);
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 229:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result7 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _result7 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result3 = setMandatoryBackupTransport(_result7, _arg0);
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 230:
                        parcel.enforceInterface(descriptor);
                        _arg0 = getMandatoryBackupTransport();
                        reply.writeNoException();
                        if (_arg0 != null) {
                            parcel2.writeInt(1);
                            _arg0.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 231:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setNetworkLoggingEnabled(_arg0, _arg5);
                        reply.writeNoException();
                        return true;
                    case 232:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result4 = isNetworkLoggingEnabled(_arg0);
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 233:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        List<NetworkEvent> _result32 = retrieveNetworkLogs(_arg0, data.readLong());
                        reply.writeNoException();
                        parcel2.writeTypedList(_result32);
                        return true;
                    case 234:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result7 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _result7 = null;
                        }
                        IApplicationThread _arg117 = android.app.IApplicationThread.Stub.asInterface(data.readStrongBinder());
                        IBinder _arg26 = data.readStrongBinder();
                        if (data.readInt() != 0) {
                            _arg0 = (Intent) Intent.CREATOR.createFromParcel(parcel);
                        }
                        _result9 = bindDeviceAdminServiceAsUser(_result7, _arg117, _arg26, _arg0, android.app.IServiceConnection.Stub.asInterface(data.readStrongBinder()), data.readInt(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result9);
                        return true;
                    case 235:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result16 = getBindDeviceAdminTargetUsers(_arg0);
                        reply.writeNoException();
                        parcel2.writeTypedList(_result16);
                        return true;
                    case 236:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result4 = isEphemeralUser(_arg0);
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 237:
                        parcel.enforceInterface(descriptor);
                        _result21 = getLastSecurityLogRetrievalTime();
                        reply.writeNoException();
                        parcel2.writeLong(_result21);
                        return true;
                    case 238:
                        parcel.enforceInterface(descriptor);
                        _result21 = getLastBugReportRequestTime();
                        reply.writeNoException();
                        parcel2.writeLong(_result21);
                        return true;
                    case 239:
                        parcel.enforceInterface(descriptor);
                        _result21 = getLastNetworkLogRetrievalTime();
                        reply.writeNoException();
                        parcel2.writeLong(_result21);
                        return true;
                    case 240:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result3 = setResetPasswordToken(_arg0, data.createByteArray());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 241:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result4 = clearResetPasswordToken(_arg0);
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 242:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result4 = isResetPasswordTokenActive(_arg0);
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 243:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result15 = resetPasswordWithToken(_arg0, data.readString(), data.createByteArray(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result15);
                        return true;
                    case 244:
                        parcel.enforceInterface(descriptor);
                        _result9 = isCurrentInputMethodSetByOwner();
                        reply.writeNoException();
                        parcel2.writeInt(_result9);
                        return true;
                    case 245:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg17 = (UserHandle) UserHandle.CREATOR.createFromParcel(parcel);
                        }
                        StringParceledListSlice _result33 = getOwnerInstalledCaCerts(_arg17);
                        reply.writeNoException();
                        if (_result33 != null) {
                            parcel2.writeInt(1);
                            _result33.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 246:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        clearApplicationUserData(_arg0, data.readString(), android.content.pm.IPackageDataObserver.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 247:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setLogoutEnabled(_arg0, _arg5);
                        reply.writeNoException();
                        return true;
                    case 248:
                        parcel.enforceInterface(descriptor);
                        _result9 = isLogoutEnabled();
                        reply.writeNoException();
                        parcel2.writeInt(_result9);
                        return true;
                    case 249:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        List<String> _result34 = getDisallowedSystemApps(_arg0, data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeStringList(_result34);
                        return true;
                    case 250:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result7 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _result7 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg18 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg18 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg22 = (PersistableBundle) PersistableBundle.CREATOR.createFromParcel(parcel);
                        }
                        transferOwnership(_result7, _arg18, _arg22);
                        reply.writeNoException();
                        return true;
                    case 251:
                        parcel.enforceInterface(descriptor);
                        _arg22 = getTransferOwnershipBundle();
                        reply.writeNoException();
                        if (_arg22 != null) {
                            parcel2.writeInt(1);
                            _arg22.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 252:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result7 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _result7 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg13 = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
                        }
                        setStartUserSessionMessage(_result7, _arg13);
                        reply.writeNoException();
                        return true;
                    case 253:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result7 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _result7 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg13 = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
                        }
                        setEndUserSessionMessage(_result7, _arg13);
                        reply.writeNoException();
                        return true;
                    case 254:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result18 = getStartUserSessionMessage(_arg0);
                        reply.writeNoException();
                        if (_result18 != null) {
                            parcel2.writeInt(1);
                            TextUtils.writeToParcel(_result18, parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 255:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result18 = getEndUserSessionMessage(_arg0);
                        reply.writeNoException();
                        if (_result18 != null) {
                            parcel2.writeInt(1);
                            TextUtils.writeToParcel(_result18, parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 256:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result12 = setMeteredDataDisabledPackages(_arg0, data.createStringArrayList());
                        reply.writeNoException();
                        parcel2.writeStringList(_result12);
                        return true;
                    case 257:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result14 = getMeteredDataDisabledPackages(_arg0);
                        reply.writeNoException();
                        parcel2.writeStringList(_result14);
                        return true;
                    case 258:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result7 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _result7 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg110 = (ApnSetting) ApnSetting.CREATOR.createFromParcel(parcel);
                        }
                        _result5 = addOverrideApn(_result7, _arg110);
                        reply.writeNoException();
                        parcel2.writeInt(_result5);
                        return true;
                    case 259:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _result7 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        } else {
                            _result7 = null;
                        }
                        _result5 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg110 = (ApnSetting) ApnSetting.CREATOR.createFromParcel(parcel);
                        }
                        _result8 = updateOverrideApn(_result7, _result5, _arg110);
                        reply.writeNoException();
                        parcel2.writeInt(_result8);
                        return true;
                    case 260:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result3 = removeOverrideApn(_arg0, data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 261:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        List<ApnSetting> _result35 = getOverrideApns(_arg0);
                        reply.writeNoException();
                        parcel2.writeTypedList(_result35);
                        return true;
                    case 262:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        if (data.readInt() != 0) {
                            _arg5 = true;
                        }
                        setOverrideApnsEnabled(_arg0, _arg5);
                        reply.writeNoException();
                        return true;
                    case 263:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result4 = isOverrideApnEnabled(_arg0);
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 264:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (ComponentName) ComponentName.CREATOR.createFromParcel(parcel);
                        }
                        _result8 = isMeteredDataDisabledPackageForUser(_arg0, data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result8);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            parcel2.writeString(descriptor);
            return true;
        }
    }

    void addCrossProfileIntentFilter(ComponentName componentName, IntentFilter intentFilter, int i) throws RemoteException;

    boolean addCrossProfileWidgetProvider(ComponentName componentName, String str) throws RemoteException;

    int addOverrideApn(ComponentName componentName, ApnSetting apnSetting) throws RemoteException;

    void addPersistentPreferredActivity(ComponentName componentName, IntentFilter intentFilter, ComponentName componentName2) throws RemoteException;

    boolean approveCaCert(String str, int i, boolean z) throws RemoteException;

    boolean bindDeviceAdminServiceAsUser(ComponentName componentName, IApplicationThread iApplicationThread, IBinder iBinder, Intent intent, IServiceConnection iServiceConnection, int i, int i2) throws RemoteException;

    int checkProvisioningPreCondition(String str, String str2) throws RemoteException;

    void choosePrivateKeyAlias(int i, Uri uri, String str, IBinder iBinder) throws RemoteException;

    void clearApplicationUserData(ComponentName componentName, String str, IPackageDataObserver iPackageDataObserver) throws RemoteException;

    void clearCrossProfileIntentFilters(ComponentName componentName) throws RemoteException;

    void clearDeviceOwner(String str) throws RemoteException;

    void clearPackagePersistentPreferredActivities(ComponentName componentName, String str) throws RemoteException;

    void clearProfileOwner(ComponentName componentName) throws RemoteException;

    boolean clearResetPasswordToken(ComponentName componentName) throws RemoteException;

    void clearSystemUpdatePolicyFreezePeriodRecord() throws RemoteException;

    Intent createAdminSupportIntent(String str) throws RemoteException;

    UserHandle createAndManageUser(ComponentName componentName, String str, ComponentName componentName2, PersistableBundle persistableBundle, int i) throws RemoteException;

    void enableSystemApp(ComponentName componentName, String str, String str2) throws RemoteException;

    int enableSystemAppWithIntent(ComponentName componentName, String str, Intent intent) throws RemoteException;

    void enforceCanManageCaCerts(ComponentName componentName, String str) throws RemoteException;

    void forceRemoveActiveAdmin(ComponentName componentName, int i) throws RemoteException;

    long forceSecurityLogs() throws RemoteException;

    void forceUpdateUserSetupComplete() throws RemoteException;

    boolean generateKeyPair(ComponentName componentName, String str, String str2, ParcelableKeyGenParameterSpec parcelableKeyGenParameterSpec, int i, KeymasterCertificateChain keymasterCertificateChain) throws RemoteException;

    String[] getAccountTypesWithManagementDisabled() throws RemoteException;

    String[] getAccountTypesWithManagementDisabledAsUser(int i) throws RemoteException;

    List<ComponentName> getActiveAdmins(int i) throws RemoteException;

    List<String> getAffiliationIds(ComponentName componentName) throws RemoteException;

    String getAlwaysOnVpnPackage(ComponentName componentName) throws RemoteException;

    Bundle getApplicationRestrictions(ComponentName componentName, String str, String str2) throws RemoteException;

    String getApplicationRestrictionsManagingPackage(ComponentName componentName) throws RemoteException;

    boolean getAutoTimeRequired() throws RemoteException;

    List<UserHandle> getBindDeviceAdminTargetUsers(ComponentName componentName) throws RemoteException;

    boolean getBluetoothContactSharingDisabled(ComponentName componentName) throws RemoteException;

    boolean getBluetoothContactSharingDisabledForUser(int i) throws RemoteException;

    boolean getCameraDisabled(ComponentName componentName, int i) throws RemoteException;

    String getCertInstallerPackage(ComponentName componentName) throws RemoteException;

    boolean getCrossProfileCallerIdDisabled(ComponentName componentName) throws RemoteException;

    boolean getCrossProfileCallerIdDisabledForUser(int i) throws RemoteException;

    boolean getCrossProfileContactsSearchDisabled(ComponentName componentName) throws RemoteException;

    boolean getCrossProfileContactsSearchDisabledForUser(int i) throws RemoteException;

    List<String> getCrossProfileWidgetProviders(ComponentName componentName) throws RemoteException;

    int getCurrentFailedPasswordAttempts(int i, boolean z) throws RemoteException;

    List<String> getDelegatePackages(ComponentName componentName, String str) throws RemoteException;

    List<String> getDelegatedScopes(ComponentName componentName, String str) throws RemoteException;

    ComponentName getDeviceOwnerComponent(boolean z) throws RemoteException;

    CharSequence getDeviceOwnerLockScreenInfo() throws RemoteException;

    String getDeviceOwnerName() throws RemoteException;

    CharSequence getDeviceOwnerOrganizationName() throws RemoteException;

    int getDeviceOwnerUserId() throws RemoteException;

    List<String> getDisallowedSystemApps(ComponentName componentName, int i, String str) throws RemoteException;

    boolean getDoNotAskCredentialsOnBoot() throws RemoteException;

    CharSequence getEndUserSessionMessage(ComponentName componentName) throws RemoteException;

    boolean getForceEphemeralUsers(ComponentName componentName) throws RemoteException;

    ComponentName getGlobalProxyAdmin(int i) throws RemoteException;

    List<String> getKeepUninstalledPackages(ComponentName componentName, String str) throws RemoteException;

    int getKeyguardDisabledFeatures(ComponentName componentName, int i, boolean z) throws RemoteException;

    long getLastBugReportRequestTime() throws RemoteException;

    long getLastNetworkLogRetrievalTime() throws RemoteException;

    long getLastSecurityLogRetrievalTime() throws RemoteException;

    int getLockTaskFeatures(ComponentName componentName) throws RemoteException;

    String[] getLockTaskPackages(ComponentName componentName) throws RemoteException;

    CharSequence getLongSupportMessage(ComponentName componentName) throws RemoteException;

    CharSequence getLongSupportMessageForUser(ComponentName componentName, int i) throws RemoteException;

    ComponentName getMandatoryBackupTransport() throws RemoteException;

    int getMaximumFailedPasswordsForWipe(ComponentName componentName, int i, boolean z) throws RemoteException;

    long getMaximumTimeToLock(ComponentName componentName, int i, boolean z) throws RemoteException;

    List<String> getMeteredDataDisabledPackages(ComponentName componentName) throws RemoteException;

    int getOrganizationColor(ComponentName componentName) throws RemoteException;

    int getOrganizationColorForUser(int i) throws RemoteException;

    CharSequence getOrganizationName(ComponentName componentName) throws RemoteException;

    CharSequence getOrganizationNameForUser(int i) throws RemoteException;

    List<ApnSetting> getOverrideApns(ComponentName componentName) throws RemoteException;

    StringParceledListSlice getOwnerInstalledCaCerts(UserHandle userHandle) throws RemoteException;

    long getPasswordExpiration(ComponentName componentName, int i, boolean z) throws RemoteException;

    long getPasswordExpirationTimeout(ComponentName componentName, int i, boolean z) throws RemoteException;

    int getPasswordHistoryLength(ComponentName componentName, int i, boolean z) throws RemoteException;

    int getPasswordMinimumLength(ComponentName componentName, int i, boolean z) throws RemoteException;

    int getPasswordMinimumLetters(ComponentName componentName, int i, boolean z) throws RemoteException;

    int getPasswordMinimumLowerCase(ComponentName componentName, int i, boolean z) throws RemoteException;

    int getPasswordMinimumNonLetter(ComponentName componentName, int i, boolean z) throws RemoteException;

    int getPasswordMinimumNumeric(ComponentName componentName, int i, boolean z) throws RemoteException;

    int getPasswordMinimumSymbols(ComponentName componentName, int i, boolean z) throws RemoteException;

    int getPasswordMinimumUpperCase(ComponentName componentName, int i, boolean z) throws RemoteException;

    int getPasswordQuality(ComponentName componentName, int i, boolean z) throws RemoteException;

    SystemUpdateInfo getPendingSystemUpdate(ComponentName componentName) throws RemoteException;

    int getPermissionGrantState(ComponentName componentName, String str, String str2, String str3) throws RemoteException;

    int getPermissionPolicy(ComponentName componentName) throws RemoteException;

    List getPermittedAccessibilityServices(ComponentName componentName) throws RemoteException;

    List getPermittedAccessibilityServicesForUser(int i) throws RemoteException;

    List<String> getPermittedCrossProfileNotificationListeners(ComponentName componentName) throws RemoteException;

    List getPermittedInputMethods(ComponentName componentName) throws RemoteException;

    List getPermittedInputMethodsForCurrentUser() throws RemoteException;

    ComponentName getProfileOwner(int i) throws RemoteException;

    String getProfileOwnerName(int i) throws RemoteException;

    int getProfileWithMinimumFailedPasswordsForWipe(int i, boolean z) throws RemoteException;

    void getRemoveWarning(ComponentName componentName, RemoteCallback remoteCallback, int i) throws RemoteException;

    long getRequiredStrongAuthTimeout(ComponentName componentName, int i, boolean z) throws RemoteException;

    ComponentName getRestrictionsProvider(int i) throws RemoteException;

    boolean getScreenCaptureDisabled(ComponentName componentName, int i) throws RemoteException;

    List<UserHandle> getSecondaryUsers(ComponentName componentName) throws RemoteException;

    CharSequence getShortSupportMessage(ComponentName componentName) throws RemoteException;

    CharSequence getShortSupportMessageForUser(ComponentName componentName, int i) throws RemoteException;

    CharSequence getStartUserSessionMessage(ComponentName componentName) throws RemoteException;

    boolean getStorageEncryption(ComponentName componentName, int i) throws RemoteException;

    int getStorageEncryptionStatus(String str, int i) throws RemoteException;

    SystemUpdatePolicy getSystemUpdatePolicy() throws RemoteException;

    PersistableBundle getTransferOwnershipBundle() throws RemoteException;

    List<PersistableBundle> getTrustAgentConfiguration(ComponentName componentName, ComponentName componentName2, int i, boolean z) throws RemoteException;

    int getUserProvisioningState() throws RemoteException;

    Bundle getUserRestrictions(ComponentName componentName) throws RemoteException;

    String getWifiMacAddress(ComponentName componentName) throws RemoteException;

    boolean hasDeviceOwner() throws RemoteException;

    boolean hasGrantedPolicy(ComponentName componentName, int i, int i2) throws RemoteException;

    boolean hasUserSetupCompleted() throws RemoteException;

    boolean installCaCert(ComponentName componentName, String str, byte[] bArr) throws RemoteException;

    boolean installExistingPackage(ComponentName componentName, String str, String str2) throws RemoteException;

    boolean installKeyPair(ComponentName componentName, String str, byte[] bArr, byte[] bArr2, byte[] bArr3, String str2, boolean z, boolean z2) throws RemoteException;

    boolean isAccessibilityServicePermittedByAdmin(ComponentName componentName, String str, int i) throws RemoteException;

    boolean isActivePasswordSufficient(int i, boolean z) throws RemoteException;

    boolean isAdminActive(ComponentName componentName, int i) throws RemoteException;

    boolean isAffiliatedUser() throws RemoteException;

    boolean isApplicationHidden(ComponentName componentName, String str, String str2) throws RemoteException;

    boolean isBackupServiceEnabled(ComponentName componentName) throws RemoteException;

    boolean isCaCertApproved(String str, int i) throws RemoteException;

    boolean isCallerApplicationRestrictionsManagingPackage(String str) throws RemoteException;

    boolean isCurrentInputMethodSetByOwner() throws RemoteException;

    boolean isDeviceProvisioned() throws RemoteException;

    boolean isDeviceProvisioningConfigApplied() throws RemoteException;

    boolean isEphemeralUser(ComponentName componentName) throws RemoteException;

    boolean isInputMethodPermittedByAdmin(ComponentName componentName, String str, int i) throws RemoteException;

    boolean isLockTaskPermitted(String str) throws RemoteException;

    boolean isLogoutEnabled() throws RemoteException;

    boolean isManagedProfile(ComponentName componentName) throws RemoteException;

    boolean isMasterVolumeMuted(ComponentName componentName) throws RemoteException;

    boolean isMeteredDataDisabledPackageForUser(ComponentName componentName, String str, int i) throws RemoteException;

    boolean isNetworkLoggingEnabled(ComponentName componentName) throws RemoteException;

    boolean isNotificationListenerServicePermitted(String str, int i) throws RemoteException;

    boolean isOverrideApnEnabled(ComponentName componentName) throws RemoteException;

    boolean isPackageSuspended(ComponentName componentName, String str, String str2) throws RemoteException;

    boolean isProfileActivePasswordSufficientForParent(int i) throws RemoteException;

    boolean isProvisioningAllowed(String str, String str2) throws RemoteException;

    boolean isRemovingAdmin(ComponentName componentName, int i) throws RemoteException;

    boolean isResetPasswordTokenActive(ComponentName componentName) throws RemoteException;

    boolean isSecurityLoggingEnabled(ComponentName componentName) throws RemoteException;

    boolean isSeparateProfileChallengeAllowed(int i) throws RemoteException;

    boolean isSystemOnlyUser(ComponentName componentName) throws RemoteException;

    boolean isUninstallBlocked(ComponentName componentName, String str) throws RemoteException;

    boolean isUninstallInQueue(String str) throws RemoteException;

    boolean isUsingUnifiedPassword(ComponentName componentName) throws RemoteException;

    void lockNow(int i, boolean z) throws RemoteException;

    int logoutUser(ComponentName componentName) throws RemoteException;

    void notifyLockTaskModeChanged(boolean z, String str, int i) throws RemoteException;

    void notifyPendingSystemUpdate(SystemUpdateInfo systemUpdateInfo) throws RemoteException;

    boolean packageHasActiveAdmins(String str, int i) throws RemoteException;

    void reboot(ComponentName componentName) throws RemoteException;

    void removeActiveAdmin(ComponentName componentName, int i) throws RemoteException;

    boolean removeCrossProfileWidgetProvider(ComponentName componentName, String str) throws RemoteException;

    boolean removeKeyPair(ComponentName componentName, String str, String str2) throws RemoteException;

    boolean removeOverrideApn(ComponentName componentName, int i) throws RemoteException;

    boolean removeUser(ComponentName componentName, UserHandle userHandle) throws RemoteException;

    void reportFailedFingerprintAttempt(int i) throws RemoteException;

    void reportFailedPasswordAttempt(int i) throws RemoteException;

    void reportKeyguardDismissed(int i) throws RemoteException;

    void reportKeyguardSecured(int i) throws RemoteException;

    void reportPasswordChanged(int i) throws RemoteException;

    void reportSuccessfulFingerprintAttempt(int i) throws RemoteException;

    void reportSuccessfulPasswordAttempt(int i) throws RemoteException;

    boolean requestBugreport(ComponentName componentName) throws RemoteException;

    boolean resetPassword(String str, int i) throws RemoteException;

    boolean resetPasswordWithToken(ComponentName componentName, String str, byte[] bArr, int i) throws RemoteException;

    List<NetworkEvent> retrieveNetworkLogs(ComponentName componentName, long j) throws RemoteException;

    ParceledListSlice retrievePreRebootSecurityLogs(ComponentName componentName) throws RemoteException;

    ParceledListSlice retrieveSecurityLogs(ComponentName componentName) throws RemoteException;

    void setAccountManagementDisabled(ComponentName componentName, String str, boolean z) throws RemoteException;

    void setActiveAdmin(ComponentName componentName, boolean z, int i) throws RemoteException;

    void setActivePasswordState(PasswordMetrics passwordMetrics, int i) throws RemoteException;

    void setAffiliationIds(ComponentName componentName, List<String> list) throws RemoteException;

    boolean setAlwaysOnVpnPackage(ComponentName componentName, String str, boolean z) throws RemoteException;

    boolean setApplicationHidden(ComponentName componentName, String str, String str2, boolean z) throws RemoteException;

    void setApplicationRestrictions(ComponentName componentName, String str, String str2, Bundle bundle) throws RemoteException;

    boolean setApplicationRestrictionsManagingPackage(ComponentName componentName, String str) throws RemoteException;

    void setAutoTimeRequired(ComponentName componentName, boolean z) throws RemoteException;

    void setBackupServiceEnabled(ComponentName componentName, boolean z) throws RemoteException;

    void setBluetoothContactSharingDisabled(ComponentName componentName, boolean z) throws RemoteException;

    void setCameraDisabled(ComponentName componentName, boolean z) throws RemoteException;

    void setCertInstallerPackage(ComponentName componentName, String str) throws RemoteException;

    void setCrossProfileCallerIdDisabled(ComponentName componentName, boolean z) throws RemoteException;

    void setCrossProfileContactsSearchDisabled(ComponentName componentName, boolean z) throws RemoteException;

    void setDefaultSmsApplication(ComponentName componentName, String str) throws RemoteException;

    void setDelegatedScopes(ComponentName componentName, String str, List<String> list) throws RemoteException;

    boolean setDeviceOwner(ComponentName componentName, String str, int i) throws RemoteException;

    void setDeviceOwnerLockScreenInfo(ComponentName componentName, CharSequence charSequence) throws RemoteException;

    void setDeviceProvisioningConfigApplied() throws RemoteException;

    void setEndUserSessionMessage(ComponentName componentName, CharSequence charSequence) throws RemoteException;

    void setForceEphemeralUsers(ComponentName componentName, boolean z) throws RemoteException;

    ComponentName setGlobalProxy(ComponentName componentName, String str, String str2) throws RemoteException;

    void setGlobalSetting(ComponentName componentName, String str, String str2) throws RemoteException;

    void setKeepUninstalledPackages(ComponentName componentName, String str, List<String> list) throws RemoteException;

    boolean setKeyPairCertificate(ComponentName componentName, String str, String str2, byte[] bArr, byte[] bArr2, boolean z) throws RemoteException;

    boolean setKeyguardDisabled(ComponentName componentName, boolean z) throws RemoteException;

    void setKeyguardDisabledFeatures(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setLockTaskFeatures(ComponentName componentName, int i) throws RemoteException;

    void setLockTaskPackages(ComponentName componentName, String[] strArr) throws RemoteException;

    void setLogoutEnabled(ComponentName componentName, boolean z) throws RemoteException;

    void setLongSupportMessage(ComponentName componentName, CharSequence charSequence) throws RemoteException;

    boolean setMandatoryBackupTransport(ComponentName componentName, ComponentName componentName2) throws RemoteException;

    void setMasterVolumeMuted(ComponentName componentName, boolean z) throws RemoteException;

    void setMaximumFailedPasswordsForWipe(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setMaximumTimeToLock(ComponentName componentName, long j, boolean z) throws RemoteException;

    List<String> setMeteredDataDisabledPackages(ComponentName componentName, List<String> list) throws RemoteException;

    void setNetworkLoggingEnabled(ComponentName componentName, boolean z) throws RemoteException;

    void setOrganizationColor(ComponentName componentName, int i) throws RemoteException;

    void setOrganizationColorForUser(int i, int i2) throws RemoteException;

    void setOrganizationName(ComponentName componentName, CharSequence charSequence) throws RemoteException;

    void setOverrideApnsEnabled(ComponentName componentName, boolean z) throws RemoteException;

    String[] setPackagesSuspended(ComponentName componentName, String str, String[] strArr, boolean z) throws RemoteException;

    void setPasswordExpirationTimeout(ComponentName componentName, long j, boolean z) throws RemoteException;

    void setPasswordHistoryLength(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setPasswordMinimumLength(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setPasswordMinimumLetters(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setPasswordMinimumLowerCase(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setPasswordMinimumNonLetter(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setPasswordMinimumNumeric(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setPasswordMinimumSymbols(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setPasswordMinimumUpperCase(ComponentName componentName, int i, boolean z) throws RemoteException;

    void setPasswordQuality(ComponentName componentName, int i, boolean z) throws RemoteException;

    boolean setPermissionGrantState(ComponentName componentName, String str, String str2, String str3, int i) throws RemoteException;

    void setPermissionPolicy(ComponentName componentName, String str, int i) throws RemoteException;

    boolean setPermittedAccessibilityServices(ComponentName componentName, List list) throws RemoteException;

    boolean setPermittedCrossProfileNotificationListeners(ComponentName componentName, List<String> list) throws RemoteException;

    boolean setPermittedInputMethods(ComponentName componentName, List list) throws RemoteException;

    void setProfileEnabled(ComponentName componentName) throws RemoteException;

    void setProfileName(ComponentName componentName, String str) throws RemoteException;

    boolean setProfileOwner(ComponentName componentName, String str, int i) throws RemoteException;

    void setRecommendedGlobalProxy(ComponentName componentName, ProxyInfo proxyInfo) throws RemoteException;

    void setRequiredStrongAuthTimeout(ComponentName componentName, long j, boolean z) throws RemoteException;

    boolean setResetPasswordToken(ComponentName componentName, byte[] bArr) throws RemoteException;

    void setRestrictionsProvider(ComponentName componentName, ComponentName componentName2) throws RemoteException;

    void setScreenCaptureDisabled(ComponentName componentName, boolean z) throws RemoteException;

    void setSecureSetting(ComponentName componentName, String str, String str2) throws RemoteException;

    void setSecurityLoggingEnabled(ComponentName componentName, boolean z) throws RemoteException;

    void setShortSupportMessage(ComponentName componentName, CharSequence charSequence) throws RemoteException;

    void setStartUserSessionMessage(ComponentName componentName, CharSequence charSequence) throws RemoteException;

    boolean setStatusBarDisabled(ComponentName componentName, boolean z) throws RemoteException;

    int setStorageEncryption(ComponentName componentName, boolean z) throws RemoteException;

    void setSystemSetting(ComponentName componentName, String str, String str2) throws RemoteException;

    void setSystemUpdatePolicy(ComponentName componentName, SystemUpdatePolicy systemUpdatePolicy) throws RemoteException;

    boolean setTime(ComponentName componentName, long j) throws RemoteException;

    boolean setTimeZone(ComponentName componentName, String str) throws RemoteException;

    void setTrustAgentConfiguration(ComponentName componentName, ComponentName componentName2, PersistableBundle persistableBundle, boolean z) throws RemoteException;

    void setUninstallBlocked(ComponentName componentName, String str, String str2, boolean z) throws RemoteException;

    void setUserIcon(ComponentName componentName, Bitmap bitmap) throws RemoteException;

    void setUserProvisioningState(int i, int i2) throws RemoteException;

    void setUserRestriction(ComponentName componentName, String str, boolean z) throws RemoteException;

    void startManagedQuickContact(String str, long j, boolean z, long j2, Intent intent) throws RemoteException;

    int startUserInBackground(ComponentName componentName, UserHandle userHandle) throws RemoteException;

    int stopUser(ComponentName componentName, UserHandle userHandle) throws RemoteException;

    boolean switchUser(ComponentName componentName, UserHandle userHandle) throws RemoteException;

    void transferOwnership(ComponentName componentName, ComponentName componentName2, PersistableBundle persistableBundle) throws RemoteException;

    void uninstallCaCerts(ComponentName componentName, String str, String[] strArr) throws RemoteException;

    void uninstallPackageWithActiveAdmins(String str) throws RemoteException;

    boolean updateOverrideApn(ComponentName componentName, int i, ApnSetting apnSetting) throws RemoteException;

    void wipeDataWithReason(int i, String str) throws RemoteException;
}
