package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.internal.telephony.vsim.HwVSimConstants;

public class HwPhoneProxyReference {
    private static final String CHINA_OPERATOR_MCC = "460";
    protected static final String LOG_TAG = "HwPhoneProxy";
    private BroadcastHelper broadcastHelper;
    private boolean firstQueryDone = false;
    private Context mContext;
    private GsmCdmaPhone mPhoneProxy;

    private class BroadcastHelper {
        private static final int MIN_MATCH = 7;
        private GlobalParamsAdaptor globalParamsAdaptor;
        private BroadcastReceiver mPhoneProxyReceiver;

        public BroadcastHelper() {
            this.globalParamsAdaptor = new GlobalParamsAdaptor(HwPhoneProxyReference.this.mPhoneProxy.getPhoneId());
            this.mPhoneProxyReceiver = new BroadcastReceiver(HwPhoneProxyReference.this) {
                public void onReceive(Context context, Intent intent) {
                    int phoneId;
                    StringBuilder stringBuilder;
                    if (TelephonyManager.getDefault().isMultiSimEnabled()) {
                        phoneId = intent.getIntExtra("phone", 0);
                        if (phoneId != HwPhoneProxyReference.this.mPhoneProxy.getPhoneId()) {
                            HwPhoneProxyReference hwPhoneProxyReference = HwPhoneProxyReference.this;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("ignore HwPhoneProxy BroadcastHelper onReceive action = ");
                            stringBuilder.append(intent.getAction());
                            stringBuilder.append("with phoneId = ");
                            stringBuilder.append(phoneId);
                            hwPhoneProxyReference.logd(stringBuilder.toString());
                            return;
                        }
                    }
                    HwPhoneProxyReference hwPhoneProxyReference2 = HwPhoneProxyReference.this;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("HwPhoneProxy BroadcastHelper onReceive action = ");
                    stringBuilder.append(intent.getAction());
                    hwPhoneProxyReference2.logd(stringBuilder.toString());
                    if ("com.huawei.intent.action.ACTION_SIM_RECORDS_READY".equals(intent.getAction())) {
                        String mccmnc = intent.getStringExtra("mccMnc");
                        int simId = HwPhoneProxyReference.this.mPhoneProxy.getPhoneId();
                        HwPhoneProxyReference hwPhoneProxyReference3;
                        StringBuilder stringBuilder2;
                        if (TelephonyManager.getDefault().isMultiSimEnabled()) {
                            hwPhoneProxyReference3 = HwPhoneProxyReference.this;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("RoamingBroker.getRBOperatorNumeric begin:");
                            stringBuilder2.append(mccmnc);
                            hwPhoneProxyReference3.logd(stringBuilder2.toString());
                            if (HwTelephonyFactory.getHwPhoneManager().isRoamingBrokerActivated(Integer.valueOf(simId))) {
                                mccmnc = HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerOperatorNumeric(Integer.valueOf(simId));
                            }
                            hwPhoneProxyReference3 = HwPhoneProxyReference.this;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("RoamingBroker.getRBOperatorNumeric end:");
                            stringBuilder2.append(mccmnc);
                            hwPhoneProxyReference3.logd(stringBuilder2.toString());
                        } else {
                            hwPhoneProxyReference3 = HwPhoneProxyReference.this;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("RoamingBroker.getRBOperatorNumeric begin:");
                            stringBuilder2.append(mccmnc);
                            hwPhoneProxyReference3.logd(stringBuilder2.toString());
                            if (HwTelephonyFactory.getHwPhoneManager().isRoamingBrokerActivated()) {
                                mccmnc = HwTelephonyFactory.getHwPhoneManager().getRoamingBrokerOperatorNumeric();
                            }
                            hwPhoneProxyReference3 = HwPhoneProxyReference.this;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("RoamingBroker.getRBOperatorNumeric end:");
                            stringBuilder2.append(mccmnc);
                            hwPhoneProxyReference3.logd(stringBuilder2.toString());
                        }
                        String imsi = intent.getStringExtra(HwVSimConstants.ENABLE_PARA_IMSI);
                        BroadcastHelper.this.globalParamsAdaptor.checkPrePostPay(mccmnc, imsi, HwPhoneProxyReference.this.getContext());
                        if (imsi == null || imsi.length() < 7 || imsi.length() > 15) {
                            Rlog.e(HwPhoneProxyReference.LOG_TAG, "invalid IMSI");
                        } else if (imsi.substring(0, 7).equals("2400768")) {
                            BroadcastHelper.this.globalParamsAdaptor.checkGlobalEccNum("24205", HwPhoneProxyReference.this.getContext());
                        } else {
                            BroadcastHelper.this.globalParamsAdaptor.checkGlobalEccNum(mccmnc, HwPhoneProxyReference.this.getContext());
                        }
                        BroadcastHelper.this.globalParamsAdaptor.checkGlobalAutoMatchParam(mccmnc, HwPhoneProxyReference.this.getContext());
                        BroadcastHelper.this.globalParamsAdaptor.checkAgpsServers(mccmnc);
                        BroadcastHelper.this.globalParamsAdaptor.checkCustLongVMNum(mccmnc, HwPhoneProxyReference.this.getContext());
                    } else if ("android.intent.action.SERVICE_STATE".equals(intent.getAction())) {
                        int phoneCount = TelephonyManager.getDefault().getPhoneCount();
                        String networkOperator = null;
                        String networkCountryIso = null;
                        for (phoneId = 0; phoneId < phoneCount; phoneId++) {
                            networkCountryIso = TelephonyManager.getDefault().getNetworkCountryIso(phoneId);
                            networkOperator = TelephonyManager.getDefault().getNetworkOperator(phoneId);
                            if (!TextUtils.isEmpty(networkCountryIso)) {
                                break;
                            }
                        }
                        boolean rplmnChanged = false;
                        String lastNetworkOperator = SystemProperties.get("gsm.hw.operator.numeric.old", "");
                        if (!(TextUtils.isEmpty(lastNetworkOperator) || TextUtils.isEmpty(networkOperator) || networkOperator.equals(lastNetworkOperator))) {
                            rplmnChanged = true;
                            HwPhoneProxyReference hwPhoneProxyReference4 = HwPhoneProxyReference.this;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("ACTION_SERVICE_STATE_CHANGED, network operator changed from ");
                            stringBuilder3.append(lastNetworkOperator);
                            stringBuilder3.append(" to ");
                            stringBuilder3.append(networkOperator);
                            hwPhoneProxyReference4.logd(stringBuilder3.toString());
                        }
                        if (!TextUtils.isEmpty(networkOperator)) {
                            SystemProperties.set("gsm.hw.operator.numeric.old", networkOperator);
                        }
                        SystemProperties.set("gsm.hw.operator.iso-country", networkCountryIso);
                        SystemProperties.set("gsm.hw.operator.numeric", networkOperator);
                        boolean isNetworkRoaming = false;
                        for (int i = 0; i < phoneCount; i++) {
                            isNetworkRoaming = TelephonyManager.getDefault().isNetworkRoaming(i);
                            if (isNetworkRoaming) {
                                break;
                            }
                        }
                        SystemProperties.set("gsm.hw.operator.isroaming", isNetworkRoaming ? "true" : "false");
                        if (SystemProperties.getBoolean("gsm.hw.operator.isroaming", false) && !TextUtils.isEmpty(networkOperator) && (!HwPhoneProxyReference.this.firstQueryDone || rplmnChanged)) {
                            HwPhoneProxyReference.this.firstQueryDone = true;
                            BroadcastHelper.this.globalParamsAdaptor.queryRoamingNumberMatchRuleByNetwork(networkOperator, HwPhoneProxyReference.this.getContext());
                            BroadcastHelper.this.globalParamsAdaptor.checkValidityOfRoamingNumberMatchRule();
                        }
                    }
                }
            };
        }

        public void init() {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("com.huawei.intent.action.ACTION_SIM_RECORDS_READY");
            intentFilter.addAction("android.intent.action.SERVICE_STATE");
            HwPhoneProxyReference.this.getContext().registerReceiver(this.mPhoneProxyReceiver, intentFilter);
            HwPhoneProxyReference.this.logd("HwPhoneProxy BroadcastHelper register complelte");
        }
    }

    public HwPhoneProxyReference(GsmCdmaPhone phoneProxy, Context context) {
        this.mContext = context;
        this.mPhoneProxy = phoneProxy;
        this.broadcastHelper = new BroadcastHelper();
        this.broadcastHelper.init();
    }

    private Context getContext() {
        return this.mContext;
    }

    private void logd(String string) {
        Rlog.d(LOG_TAG, string);
    }
}
