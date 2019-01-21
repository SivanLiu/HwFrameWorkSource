package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.BadParcelableException;
import android.os.Build;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.ims.ImsConferenceState;
import android.telephony.ims.ImsExternalCallState;
import android.telephony.ims.ImsReasonInfo;
import com.android.ims.ImsCall;
import com.android.internal.telephony.PhoneInternalInterface.SuppService;
import com.android.internal.telephony.gsm.SuppServiceNotification;
import com.android.internal.telephony.imsphone.ImsExternalCallTracker;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.imsphone.ImsPhoneCall;
import com.android.internal.telephony.test.TestConferenceEventPackageParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class TelephonyTester {
    private static final String ACTION_RESET = "reset";
    private static final String ACTION_TEST_CONFERENCE_EVENT_PACKAGE = "com.android.internal.telephony.TestConferenceEventPackage";
    private static final String ACTION_TEST_DIALOG_EVENT_PACKAGE = "com.android.internal.telephony.TestDialogEventPackage";
    private static final String ACTION_TEST_HANDOVER_FAIL = "com.android.internal.telephony.TestHandoverFail";
    private static final String ACTION_TEST_SERVICE_STATE = "com.android.internal.telephony.TestServiceState";
    private static final String ACTION_TEST_SUPP_SRVC_FAIL = "com.android.internal.telephony.TestSuppSrvcFail";
    private static final String ACTION_TEST_SUPP_SRVC_NOTIFICATION = "com.android.internal.telephony.TestSuppSrvcNotification";
    private static final boolean DBG = true;
    private static final String EXTRA_ACTION = "action";
    private static final String EXTRA_CANPULL = "canPull";
    private static final String EXTRA_CODE = "code";
    private static final String EXTRA_DATA_RAT = "data_rat";
    private static final String EXTRA_DATA_REG_STATE = "data_reg_state";
    private static final String EXTRA_DATA_ROAMING_TYPE = "data_roaming_type";
    private static final String EXTRA_DIALOGID = "dialogId";
    private static final String EXTRA_FAILURE_CODE = "failureCode";
    private static final String EXTRA_FILENAME = "filename";
    private static final String EXTRA_NUMBER = "number";
    private static final String EXTRA_SENDPACKAGE = "sendPackage";
    private static final String EXTRA_STARTPACKAGE = "startPackage";
    private static final String EXTRA_STATE = "state";
    private static final String EXTRA_TYPE = "type";
    private static final String EXTRA_VOICE_RAT = "voice_rat";
    private static final String EXTRA_VOICE_REG_STATE = "voice_reg_state";
    private static final String EXTRA_VOICE_ROAMING_TYPE = "voice_roaming_type";
    private static List<ImsExternalCallState> mImsExternalCallStates = null;
    private String LOG_TAG = "TelephonyTester";
    protected BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            try {
                TelephonyTester telephonyTester = TelephonyTester.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("sIntentReceiver.onReceive: action=");
                stringBuilder.append(action);
                telephonyTester.log(stringBuilder.toString());
                if (action.equals(TelephonyTester.this.mPhone.getActionDetached())) {
                    TelephonyTester.this.log("simulate detaching");
                    TelephonyTester.this.mPhone.getServiceStateTracker().mDetachedRegistrants.notifyRegistrants();
                } else if (action.equals(TelephonyTester.this.mPhone.getActionAttached())) {
                    TelephonyTester.this.log("simulate attaching");
                    TelephonyTester.this.mPhone.getServiceStateTracker().mAttachedRegistrants.notifyRegistrants();
                } else if (action.equals(TelephonyTester.ACTION_TEST_CONFERENCE_EVENT_PACKAGE)) {
                    TelephonyTester.this.log("inject simulated conference event package");
                    TelephonyTester.this.handleTestConferenceEventPackage(context, intent.getStringExtra(TelephonyTester.EXTRA_FILENAME));
                } else if (action.equals(TelephonyTester.ACTION_TEST_DIALOG_EVENT_PACKAGE)) {
                    TelephonyTester.this.log("handle test dialog event package intent");
                    TelephonyTester.this.handleTestDialogEventPackageIntent(intent);
                } else if (action.equals(TelephonyTester.ACTION_TEST_SUPP_SRVC_FAIL)) {
                    TelephonyTester.this.log("handle test supp svc failed intent");
                    TelephonyTester.this.handleSuppServiceFailedIntent(intent);
                } else if (action.equals(TelephonyTester.ACTION_TEST_HANDOVER_FAIL)) {
                    TelephonyTester.this.log("handle handover fail test intent");
                    TelephonyTester.this.handleHandoverFailedIntent();
                } else if (action.equals(TelephonyTester.ACTION_TEST_SUPP_SRVC_NOTIFICATION)) {
                    TelephonyTester.this.log("handle supp service notification test intent");
                    TelephonyTester.this.sendTestSuppServiceNotification(intent);
                } else if (action.equals(TelephonyTester.ACTION_TEST_SERVICE_STATE)) {
                    TelephonyTester.this.log("handle test service state changed intent");
                    TelephonyTester.this.mServiceStateTestIntent = intent;
                    TelephonyTester.this.mPhone.getServiceStateTracker().sendEmptyMessage(2);
                } else {
                    telephonyTester = TelephonyTester.this;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("onReceive: unknown action=");
                    stringBuilder.append(action);
                    telephonyTester.log(stringBuilder.toString());
                }
            } catch (BadParcelableException e) {
                Rlog.w(TelephonyTester.this.LOG_TAG, e);
            }
        }
    };
    private Phone mPhone;
    private Intent mServiceStateTestIntent;

    TelephonyTester(Phone phone) {
        this.mPhone = phone;
        if (Build.IS_DEBUGGABLE) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(this.mPhone.getActionDetached());
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("register for intent action=");
            stringBuilder.append(this.mPhone.getActionDetached());
            log(stringBuilder.toString());
            filter.addAction(this.mPhone.getActionAttached());
            stringBuilder = new StringBuilder();
            stringBuilder.append("register for intent action=");
            stringBuilder.append(this.mPhone.getActionAttached());
            log(stringBuilder.toString());
            if (this.mPhone.getPhoneType() == 5) {
                log("register for intent action=com.android.internal.telephony.TestConferenceEventPackage");
                filter.addAction(ACTION_TEST_CONFERENCE_EVENT_PACKAGE);
                filter.addAction(ACTION_TEST_DIALOG_EVENT_PACKAGE);
                filter.addAction(ACTION_TEST_SUPP_SRVC_FAIL);
                filter.addAction(ACTION_TEST_HANDOVER_FAIL);
                filter.addAction(ACTION_TEST_SUPP_SRVC_NOTIFICATION);
                mImsExternalCallStates = new ArrayList();
            } else {
                filter.addAction(ACTION_TEST_SERVICE_STATE);
                log("register for intent action=com.android.internal.telephony.TestServiceState");
            }
            phone.getContext().registerReceiver(this.mIntentReceiver, filter, null, this.mPhone.getHandler());
        }
    }

    void dispose() {
        if (Build.IS_DEBUGGABLE) {
            this.mPhone.getContext().unregisterReceiver(this.mIntentReceiver);
        }
    }

    private void log(String s) {
        Rlog.d(this.LOG_TAG, s);
    }

    private void handleSuppServiceFailedIntent(Intent intent) {
        ImsPhone imsPhone = this.mPhone;
        if (imsPhone != null) {
            imsPhone.notifySuppServiceFailed(SuppService.values()[intent.getIntExtra(EXTRA_FAILURE_CODE, 0)]);
        }
    }

    private void handleHandoverFailedIntent() {
        ImsPhone imsPhone = this.mPhone;
        if (imsPhone != null) {
            ImsPhoneCall imsPhoneCall = imsPhone.getForegroundCall();
            if (imsPhoneCall != null) {
                ImsCall imsCall = imsPhoneCall.getImsCall();
                if (imsCall != null) {
                    imsCall.getImsCallSessionListenerProxy().callSessionHandoverFailed(imsCall.getCallSession(), 14, 18, new ImsReasonInfo());
                }
            }
        }
    }

    private void handleTestConferenceEventPackage(Context context, String fileName) {
        ImsPhone imsPhone = this.mPhone;
        if (imsPhone != null) {
            ImsPhoneCall imsPhoneCall = imsPhone.getForegroundCall();
            if (imsPhoneCall != null) {
                ImsCall imsCall = imsPhoneCall.getImsCall();
                if (imsCall != null) {
                    File packageFile = new File(context.getFilesDir(), fileName);
                    try {
                        ImsConferenceState imsConferenceState = new TestConferenceEventPackageParser(new FileInputStream(packageFile)).parse();
                        if (imsConferenceState != null) {
                            imsCall.conferenceStateUpdated(imsConferenceState);
                        }
                    } catch (FileNotFoundException e) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Test conference event package file not found: ");
                        stringBuilder.append(packageFile.getAbsolutePath());
                        log(stringBuilder.toString());
                    }
                }
            }
        }
    }

    private void handleTestDialogEventPackageIntent(Intent intent) {
        ImsPhone imsPhone = this.mPhone;
        if (imsPhone != null) {
            ImsExternalCallTracker externalCallTracker = imsPhone.getExternalCallTracker();
            if (externalCallTracker != null) {
                if (intent.hasExtra(EXTRA_STARTPACKAGE)) {
                    mImsExternalCallStates.clear();
                } else if (intent.hasExtra(EXTRA_SENDPACKAGE)) {
                    externalCallTracker.refreshExternalCallState(mImsExternalCallStates);
                    mImsExternalCallStates.clear();
                } else if (intent.hasExtra(EXTRA_DIALOGID)) {
                    mImsExternalCallStates.add(new ImsExternalCallState(intent.getIntExtra(EXTRA_DIALOGID, 0), Uri.parse(intent.getStringExtra(EXTRA_NUMBER)), intent.getBooleanExtra(EXTRA_CANPULL, true), intent.getIntExtra(EXTRA_STATE, 1), 2, false));
                }
            }
        }
    }

    private void sendTestSuppServiceNotification(Intent intent) {
        if (intent.hasExtra(EXTRA_CODE) && intent.hasExtra(EXTRA_TYPE)) {
            int code = intent.getIntExtra(EXTRA_CODE, -1);
            int type = intent.getIntExtra(EXTRA_TYPE, -1);
            ImsPhone imsPhone = this.mPhone;
            if (imsPhone != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Test supp service notification:");
                stringBuilder.append(code);
                log(stringBuilder.toString());
                SuppServiceNotification suppServiceNotification = new SuppServiceNotification();
                suppServiceNotification.code = code;
                suppServiceNotification.notificationType = type;
                imsPhone.notifySuppSvcNotification(suppServiceNotification);
            }
        }
    }

    void overrideServiceState(ServiceState ss) {
        if (this.mServiceStateTestIntent != null && ss != null) {
            if (this.mServiceStateTestIntent.hasExtra(EXTRA_ACTION) && ACTION_RESET.equals(this.mServiceStateTestIntent.getStringExtra(EXTRA_ACTION))) {
                log("Service state override reset");
                return;
            }
            StringBuilder stringBuilder;
            if (this.mServiceStateTestIntent.hasExtra(EXTRA_VOICE_REG_STATE)) {
                ss.setVoiceRegState(this.mServiceStateTestIntent.getIntExtra(EXTRA_VOICE_REG_STATE, 4));
                stringBuilder = new StringBuilder();
                stringBuilder.append("Override voice reg state with ");
                stringBuilder.append(ss.getVoiceRegState());
                log(stringBuilder.toString());
            }
            if (this.mServiceStateTestIntent.hasExtra(EXTRA_DATA_REG_STATE)) {
                ss.setDataRegState(this.mServiceStateTestIntent.getIntExtra(EXTRA_DATA_REG_STATE, 4));
                stringBuilder = new StringBuilder();
                stringBuilder.append("Override data reg state with ");
                stringBuilder.append(ss.getDataRegState());
                log(stringBuilder.toString());
            }
            if (this.mServiceStateTestIntent.hasExtra(EXTRA_VOICE_RAT)) {
                ss.setRilVoiceRadioTechnology(this.mServiceStateTestIntent.getIntExtra(EXTRA_VOICE_RAT, 0));
                stringBuilder = new StringBuilder();
                stringBuilder.append("Override voice rat with ");
                stringBuilder.append(ss.getRilVoiceRadioTechnology());
                log(stringBuilder.toString());
            }
            if (this.mServiceStateTestIntent.hasExtra(EXTRA_DATA_RAT)) {
                ss.setRilDataRadioTechnology(this.mServiceStateTestIntent.getIntExtra(EXTRA_DATA_RAT, 0));
                stringBuilder = new StringBuilder();
                stringBuilder.append("Override data rat with ");
                stringBuilder.append(ss.getRilDataRadioTechnology());
                log(stringBuilder.toString());
            }
            if (this.mServiceStateTestIntent.hasExtra(EXTRA_VOICE_ROAMING_TYPE)) {
                ss.setVoiceRoamingType(this.mServiceStateTestIntent.getIntExtra(EXTRA_VOICE_ROAMING_TYPE, 1));
                stringBuilder = new StringBuilder();
                stringBuilder.append("Override voice roaming type with ");
                stringBuilder.append(ss.getVoiceRoamingType());
                log(stringBuilder.toString());
            }
            if (this.mServiceStateTestIntent.hasExtra(EXTRA_DATA_ROAMING_TYPE)) {
                ss.setDataRoamingType(this.mServiceStateTestIntent.getIntExtra(EXTRA_DATA_ROAMING_TYPE, 1));
                stringBuilder = new StringBuilder();
                stringBuilder.append("Override data roaming type with ");
                stringBuilder.append(ss.getDataRoamingType());
                log(stringBuilder.toString());
            }
        }
    }
}
