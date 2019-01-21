package android.telephony.euicc;

import android.annotation.SystemApi;
import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.service.euicc.EuiccProfileInfo;
import android.util.Log;
import com.android.internal.telephony.euicc.IAuthenticateServerCallback;
import com.android.internal.telephony.euicc.ICancelSessionCallback;
import com.android.internal.telephony.euicc.IDeleteProfileCallback;
import com.android.internal.telephony.euicc.IDisableProfileCallback;
import com.android.internal.telephony.euicc.IEuiccCardController;
import com.android.internal.telephony.euicc.IEuiccCardController.Stub;
import com.android.internal.telephony.euicc.IGetAllProfilesCallback;
import com.android.internal.telephony.euicc.IGetDefaultSmdpAddressCallback;
import com.android.internal.telephony.euicc.IGetEuiccChallengeCallback;
import com.android.internal.telephony.euicc.IGetEuiccInfo1Callback;
import com.android.internal.telephony.euicc.IGetEuiccInfo2Callback;
import com.android.internal.telephony.euicc.IGetProfileCallback;
import com.android.internal.telephony.euicc.IGetRulesAuthTableCallback;
import com.android.internal.telephony.euicc.IGetSmdsAddressCallback;
import com.android.internal.telephony.euicc.IListNotificationsCallback;
import com.android.internal.telephony.euicc.ILoadBoundProfilePackageCallback;
import com.android.internal.telephony.euicc.IPrepareDownloadCallback;
import com.android.internal.telephony.euicc.IRemoveNotificationFromListCallback;
import com.android.internal.telephony.euicc.IResetMemoryCallback;
import com.android.internal.telephony.euicc.IRetrieveNotificationCallback;
import com.android.internal.telephony.euicc.IRetrieveNotificationListCallback;
import com.android.internal.telephony.euicc.ISetDefaultSmdpAddressCallback;
import com.android.internal.telephony.euicc.ISetNicknameCallback;
import com.android.internal.telephony.euicc.ISwitchToProfileCallback;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

@SystemApi
public class EuiccCardManager {
    public static final int CANCEL_REASON_END_USER_REJECTED = 0;
    public static final int CANCEL_REASON_POSTPONED = 1;
    public static final int CANCEL_REASON_PPR_NOT_ALLOWED = 3;
    public static final int CANCEL_REASON_TIMEOUT = 2;
    public static final int RESET_OPTION_DELETE_FIELD_LOADED_TEST_PROFILES = 2;
    public static final int RESET_OPTION_DELETE_OPERATIONAL_PROFILES = 1;
    public static final int RESET_OPTION_RESET_DEFAULT_SMDP_ADDRESS = 4;
    public static final int RESULT_EUICC_NOT_FOUND = -2;
    public static final int RESULT_OK = 0;
    public static final int RESULT_UNKNOWN_ERROR = -1;
    private static final String TAG = "EuiccCardManager";
    private final Context mContext;

    @Retention(RetentionPolicy.SOURCE)
    public @interface CancelReason {
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface ResetOption {
    }

    public interface ResultCallback<T> {
        void onComplete(int i, T t);
    }

    public EuiccCardManager(Context context) {
        this.mContext = context;
    }

    private IEuiccCardController getIEuiccCardController() {
        return Stub.asInterface(ServiceManager.getService("euicc_card_controller"));
    }

    public void requestAllProfiles(String cardId, final Executor executor, final ResultCallback<EuiccProfileInfo[]> callback) {
        try {
            getIEuiccCardController().getAllProfiles(this.mContext.getOpPackageName(), cardId, new IGetAllProfilesCallback.Stub() {
                public void onComplete(int resultCode, EuiccProfileInfo[] profiles) {
                    executor.execute(new -$$Lambda$EuiccCardManager$1$WCF3YSMl2TGGvaCq1GRblRP0j8M(callback, resultCode, profiles));
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling getAllProfiles", e);
            throw e.rethrowFromSystemServer();
        }
    }

    public void requestProfile(String cardId, String iccid, final Executor executor, final ResultCallback<EuiccProfileInfo> callback) {
        try {
            getIEuiccCardController().getProfile(this.mContext.getOpPackageName(), cardId, iccid, new IGetProfileCallback.Stub() {
                public void onComplete(int resultCode, EuiccProfileInfo profile) {
                    executor.execute(new -$$Lambda$EuiccCardManager$2$TyPTPQ9XsUKfhC8yZUgq-jP-Ugs(callback, resultCode, profile));
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling getProfile", e);
            throw e.rethrowFromSystemServer();
        }
    }

    public void disableProfile(String cardId, String iccid, boolean refresh, final Executor executor, final ResultCallback<Void> callback) {
        try {
            getIEuiccCardController().disableProfile(this.mContext.getOpPackageName(), cardId, iccid, refresh, new IDisableProfileCallback.Stub() {
                public void onComplete(int resultCode) {
                    executor.execute(new -$$Lambda$EuiccCardManager$3$rixBHO-K-K3SJ1SVCAj8_82SxFE(callback, resultCode));
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling disableProfile", e);
            throw e.rethrowFromSystemServer();
        }
    }

    public void switchToProfile(String cardId, String iccid, boolean refresh, final Executor executor, final ResultCallback<EuiccProfileInfo> callback) {
        try {
            getIEuiccCardController().switchToProfile(this.mContext.getOpPackageName(), cardId, iccid, refresh, new ISwitchToProfileCallback.Stub() {
                public void onComplete(int resultCode, EuiccProfileInfo profile) {
                    executor.execute(new -$$Lambda$EuiccCardManager$4$yTPBL-lMjfIGHQUa-JxPKPLvVR8(callback, resultCode, profile));
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling switchToProfile", e);
            throw e.rethrowFromSystemServer();
        }
    }

    public void setNickname(String cardId, String iccid, String nickname, final Executor executor, final ResultCallback<Void> callback) {
        try {
            getIEuiccCardController().setNickname(this.mContext.getOpPackageName(), cardId, iccid, nickname, new ISetNicknameCallback.Stub() {
                public void onComplete(int resultCode) {
                    executor.execute(new -$$Lambda$EuiccCardManager$5$Tw9Ac3hC3rh6YoO0o4ip_fVYWq0(callback, resultCode));
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling setNickname", e);
            throw e.rethrowFromSystemServer();
        }
    }

    public void deleteProfile(String cardId, String iccid, final Executor executor, final ResultCallback<Void> callback) {
        try {
            getIEuiccCardController().deleteProfile(this.mContext.getOpPackageName(), cardId, iccid, new IDeleteProfileCallback.Stub() {
                public void onComplete(int resultCode) {
                    executor.execute(new -$$Lambda$EuiccCardManager$6$K-EQz--QgHyjI0itfruTgIG7hos(callback, resultCode));
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling deleteProfile", e);
            throw e.rethrowFromSystemServer();
        }
    }

    public void resetMemory(String cardId, int options, final Executor executor, final ResultCallback<Void> callback) {
        try {
            getIEuiccCardController().resetMemory(this.mContext.getOpPackageName(), cardId, options, new IResetMemoryCallback.Stub() {
                public void onComplete(int resultCode) {
                    executor.execute(new -$$Lambda$EuiccCardManager$7$W9T937HBG-sD8BsVWGQ6kDb28dk(callback, resultCode));
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling resetMemory", e);
            throw e.rethrowFromSystemServer();
        }
    }

    public void requestDefaultSmdpAddress(String cardId, final Executor executor, final ResultCallback<String> callback) {
        try {
            getIEuiccCardController().getDefaultSmdpAddress(this.mContext.getOpPackageName(), cardId, new IGetDefaultSmdpAddressCallback.Stub() {
                public void onComplete(int resultCode, String address) {
                    executor.execute(new -$$Lambda$EuiccCardManager$8$A_S7upEqW6mofzD1_YkLBP5INOM(callback, resultCode, address));
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling getDefaultSmdpAddress", e);
            throw e.rethrowFromSystemServer();
        }
    }

    public void requestSmdsAddress(String cardId, final Executor executor, final ResultCallback<String> callback) {
        try {
            getIEuiccCardController().getSmdsAddress(this.mContext.getOpPackageName(), cardId, new IGetSmdsAddressCallback.Stub() {
                public void onComplete(int resultCode, String address) {
                    executor.execute(new -$$Lambda$EuiccCardManager$9$cPEHH7JlllMuvBHJOu0A2hY4QyU(callback, resultCode, address));
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling getSmdsAddress", e);
            throw e.rethrowFromSystemServer();
        }
    }

    public void setDefaultSmdpAddress(String cardId, String defaultSmdpAddress, final Executor executor, final ResultCallback<Void> callback) {
        try {
            getIEuiccCardController().setDefaultSmdpAddress(this.mContext.getOpPackageName(), cardId, defaultSmdpAddress, new ISetDefaultSmdpAddressCallback.Stub() {
                public void onComplete(int resultCode) {
                    executor.execute(new -$$Lambda$EuiccCardManager$10$tNYkM-c2PgDDurtC-iDXbvWa5_8(callback, resultCode));
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling setDefaultSmdpAddress", e);
            throw e.rethrowFromSystemServer();
        }
    }

    public void requestRulesAuthTable(String cardId, final Executor executor, final ResultCallback<EuiccRulesAuthTable> callback) {
        try {
            getIEuiccCardController().getRulesAuthTable(this.mContext.getOpPackageName(), cardId, new IGetRulesAuthTableCallback.Stub() {
                public void onComplete(int resultCode, EuiccRulesAuthTable rat) {
                    executor.execute(new -$$Lambda$EuiccCardManager$11$IPX2CweBQhOCbcMAQ3yyU-N8fjQ(callback, resultCode, rat));
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling getRulesAuthTable", e);
            throw e.rethrowFromSystemServer();
        }
    }

    public void requestEuiccChallenge(String cardId, final Executor executor, final ResultCallback<byte[]> callback) {
        try {
            getIEuiccCardController().getEuiccChallenge(this.mContext.getOpPackageName(), cardId, new IGetEuiccChallengeCallback.Stub() {
                public void onComplete(int resultCode, byte[] challenge) {
                    executor.execute(new -$$Lambda$EuiccCardManager$12$0dAgJQ0nijxpb9xsSsFqNuBkchU(callback, resultCode, challenge));
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling getEuiccChallenge", e);
            throw e.rethrowFromSystemServer();
        }
    }

    public void requestEuiccInfo1(String cardId, final Executor executor, final ResultCallback<byte[]> callback) {
        try {
            getIEuiccCardController().getEuiccInfo1(this.mContext.getOpPackageName(), cardId, new IGetEuiccInfo1Callback.Stub() {
                public void onComplete(int resultCode, byte[] info) {
                    executor.execute(new -$$Lambda$EuiccCardManager$13$Kd-aeGG9po3MXhUohSTDAoC8kqI(callback, resultCode, info));
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling getEuiccInfo1", e);
            throw e.rethrowFromSystemServer();
        }
    }

    public void requestEuiccInfo2(String cardId, final Executor executor, final ResultCallback<byte[]> callback) {
        try {
            getIEuiccCardController().getEuiccInfo2(this.mContext.getOpPackageName(), cardId, new IGetEuiccInfo2Callback.Stub() {
                public void onComplete(int resultCode, byte[] info) {
                    executor.execute(new -$$Lambda$EuiccCardManager$14$v9-1WsmNGIOXMEjPL4FGhZERO18(callback, resultCode, info));
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling getEuiccInfo2", e);
            throw e.rethrowFromSystemServer();
        }
    }

    public void authenticateServer(String cardId, String matchingId, byte[] serverSigned1, byte[] serverSignature1, byte[] euiccCiPkIdToBeUsed, byte[] serverCertificate, Executor executor, ResultCallback<byte[]> callback) {
        RemoteException e;
        final Executor executor2;
        final ResultCallback<byte[]> resultCallback;
        try {
            executor2 = executor;
            resultCallback = callback;
            try {
                getIEuiccCardController().authenticateServer(this.mContext.getOpPackageName(), cardId, matchingId, serverSigned1, serverSignature1, euiccCiPkIdToBeUsed, serverCertificate, new IAuthenticateServerCallback.Stub() {
                    public void onComplete(int resultCode, byte[] response) {
                        executor2.execute(new -$$Lambda$EuiccCardManager$15$_sAstfKdFkraAjC1faT-C0t_PgM(resultCallback, resultCode, response));
                    }
                });
            } catch (RemoteException e2) {
                e = e2;
            }
        } catch (RemoteException e3) {
            e = e3;
            executor2 = executor;
            resultCallback = callback;
            Log.e(TAG, "Error calling authenticateServer", e);
            throw e.rethrowFromSystemServer();
        }
    }

    public void prepareDownload(String cardId, byte[] hashCc, byte[] smdpSigned2, byte[] smdpSignature2, byte[] smdpCertificate, final Executor executor, final ResultCallback<byte[]> callback) {
        try {
            getIEuiccCardController().prepareDownload(this.mContext.getOpPackageName(), cardId, hashCc, smdpSigned2, smdpSignature2, smdpCertificate, new IPrepareDownloadCallback.Stub() {
                public void onComplete(int resultCode, byte[] response) {
                    executor.execute(new -$$Lambda$EuiccCardManager$16$HzYHHKtPhCzsbbDlkzDxayy5kVM(callback, resultCode, response));
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling prepareDownload", e);
            throw e.rethrowFromSystemServer();
        }
    }

    public void loadBoundProfilePackage(String cardId, byte[] boundProfilePackage, final Executor executor, final ResultCallback<byte[]> callback) {
        try {
            getIEuiccCardController().loadBoundProfilePackage(this.mContext.getOpPackageName(), cardId, boundProfilePackage, new ILoadBoundProfilePackageCallback.Stub() {
                public void onComplete(int resultCode, byte[] response) {
                    executor.execute(new -$$Lambda$EuiccCardManager$17$uqb-wFIaxTmLnjUZZIY6J_DjHD0(callback, resultCode, response));
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling loadBoundProfilePackage", e);
            throw e.rethrowFromSystemServer();
        }
    }

    public void cancelSession(String cardId, byte[] transactionId, int reason, final Executor executor, final ResultCallback<byte[]> callback) {
        try {
            getIEuiccCardController().cancelSession(this.mContext.getOpPackageName(), cardId, transactionId, reason, new ICancelSessionCallback.Stub() {
                public void onComplete(int resultCode, byte[] response) {
                    executor.execute(new -$$Lambda$EuiccCardManager$18$2cHWlkpkAsYqhkpHbNv-QMqc0Ng(callback, resultCode, response));
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling cancelSession", e);
            throw e.rethrowFromSystemServer();
        }
    }

    public void listNotifications(String cardId, int events, final Executor executor, final ResultCallback<EuiccNotification[]> callback) {
        try {
            getIEuiccCardController().listNotifications(this.mContext.getOpPackageName(), cardId, events, new IListNotificationsCallback.Stub() {
                public void onComplete(int resultCode, EuiccNotification[] notifications) {
                    executor.execute(new -$$Lambda$EuiccCardManager$19$Gjn1FcgJf1Gqq6yBzVQLrvqlyg0(callback, resultCode, notifications));
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling listNotifications", e);
            throw e.rethrowFromSystemServer();
        }
    }

    public void retrieveNotificationList(String cardId, int events, final Executor executor, final ResultCallback<EuiccNotification[]> callback) {
        try {
            getIEuiccCardController().retrieveNotificationList(this.mContext.getOpPackageName(), cardId, events, new IRetrieveNotificationListCallback.Stub() {
                public void onComplete(int resultCode, EuiccNotification[] notifications) {
                    executor.execute(new -$$Lambda$EuiccCardManager$20$BvkqzlF_5oeo0InlIzG65QhyNT0(callback, resultCode, notifications));
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling retrieveNotificationList", e);
            throw e.rethrowFromSystemServer();
        }
    }

    public void retrieveNotification(String cardId, int seqNumber, final Executor executor, final ResultCallback<EuiccNotification> callback) {
        try {
            getIEuiccCardController().retrieveNotification(this.mContext.getOpPackageName(), cardId, seqNumber, new IRetrieveNotificationCallback.Stub() {
                public void onComplete(int resultCode, EuiccNotification notification) {
                    executor.execute(new -$$Lambda$EuiccCardManager$21$srrmNYPqPTZF4uUZIcVq86p1JpU(callback, resultCode, notification));
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling retrieveNotification", e);
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeNotificationFromList(String cardId, int seqNumber, final Executor executor, final ResultCallback<Void> callback) {
        try {
            getIEuiccCardController().removeNotificationFromList(this.mContext.getOpPackageName(), cardId, seqNumber, new IRemoveNotificationFromListCallback.Stub() {
                public void onComplete(int resultCode) {
                    executor.execute(new -$$Lambda$EuiccCardManager$22$JpF6e8fcw8874GZTA7KUvqsNhY8(callback, resultCode));
                }
            });
        } catch (RemoteException e) {
            Log.e(TAG, "Error calling removeNotificationFromList", e);
            throw e.rethrowFromSystemServer();
        }
    }
}
