package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.util.SparseArray;
import android.util.SparseIntArray;

public class RatRatcheter {
    private static final String LOG_TAG = "RilRatcheter";
    private BroadcastReceiver mConfigChangedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.telephony.action.CARRIER_CONFIG_CHANGED".equals(intent.getAction())) {
                RatRatcheter.this.resetRatFamilyMap();
            }
        }
    };
    private final Phone mPhone;
    private final SparseArray<SparseIntArray> mRatFamilyMap = new SparseArray();

    public RatRatcheter(Phone phone) {
        this.mPhone = phone;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        phone.getContext().registerReceiverAsUser(this.mConfigChangedReceiver, UserHandle.ALL, intentFilter, null, null);
        resetRatFamilyMap();
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int ratchetRat(int oldRat, int newRat) {
        synchronized (this.mRatFamilyMap) {
            SparseIntArray oldFamily = (SparseIntArray) this.mRatFamilyMap.get(oldRat);
            if (oldFamily == null) {
                return newRat;
            }
            SparseIntArray newFamily = (SparseIntArray) this.mRatFamilyMap.get(newRat);
            if (newFamily != oldFamily) {
                return newRat;
            } else if (newFamily.get(oldRat, -1) <= newFamily.get(newRat, -1)) {
                oldRat = newRat;
            }
        }
    }

    public void ratchetRat(ServiceState oldSS, ServiceState newSS) {
        int newVoiceRat = ratchetRat(oldSS.getRilVoiceRadioTechnology(), newSS.getRilVoiceRadioTechnology());
        int newDataRat = ratchetRat(oldSS.getRilDataRadioTechnology(), newSS.getRilDataRadioTechnology());
        boolean newUsingCA = false;
        if (newSS.getRilDataRadioTechnology() == 14 || newSS.getRilDataRadioTechnology() == 19) {
            if (oldSS.isUsingCarrierAggregation()) {
                newUsingCA = true;
            } else {
                newUsingCA = newSS.isUsingCarrierAggregation();
            }
        }
        newSS.setRilVoiceRadioTechnology(newVoiceRat);
        newSS.setRilDataRadioTechnology(newDataRat);
        newSS.setIsUsingCarrierAggregation(newUsingCA);
    }

    private void resetRatFamilyMap() {
        synchronized (this.mRatFamilyMap) {
            this.mRatFamilyMap.clear();
            CarrierConfigManager configManager = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
            if (configManager == null) {
                return;
            }
            PersistableBundle b = configManager.getConfig();
            if (b == null) {
                return;
            }
            String[] ratFamilies = b.getStringArray("ratchet_rat_families");
            if (ratFamilies == null) {
                return;
            }
            for (String ratFamily : ratFamilies) {
                String[] rats = ratFamily.split(",");
                if (rats.length >= 2) {
                    SparseIntArray currentFamily = new SparseIntArray(rats.length);
                    int i = 0;
                    int length = rats.length;
                    int pos = 0;
                    while (i < length) {
                        String ratString = rats[i];
                        try {
                            int ratInt = Integer.parseInt(ratString.trim());
                            if (this.mRatFamilyMap.get(ratInt) != null) {
                                Rlog.e(LOG_TAG, "RAT listed twice: " + ratString);
                                break;
                            }
                            int pos2 = pos + 1;
                            currentFamily.put(ratInt, pos);
                            this.mRatFamilyMap.put(ratInt, currentFamily);
                            i++;
                            pos = pos2;
                        } catch (NumberFormatException e) {
                            Rlog.e(LOG_TAG, "NumberFormatException on " + ratString);
                        }
                    }
                    continue;
                }
            }
        }
    }
}
