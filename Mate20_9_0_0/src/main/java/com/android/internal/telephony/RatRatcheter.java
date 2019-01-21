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
import java.util.Arrays;

public class RatRatcheter {
    private static final String LOG_TAG = "RilRatcheter";
    private BroadcastReceiver mConfigChangedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.telephony.action.CARRIER_CONFIG_CHANGED".equals(intent.getAction())) {
                RatRatcheter.this.resetRatFamilyMap();
            }
        }
    };
    private boolean mDataRatchetEnabled = true;
    private final Phone mPhone;
    private final SparseArray<SparseIntArray> mRatFamilyMap = new SparseArray();
    private boolean mVoiceRatchetEnabled = true;

    public static boolean updateBandwidths(int[] bandwidths, ServiceState serviceState) {
        if (bandwidths == null || Arrays.stream(bandwidths).sum() <= Arrays.stream(serviceState.getCellBandwidths()).sum()) {
            return false;
        }
        serviceState.setCellBandwidths(bandwidths);
        return true;
    }

    public RatRatcheter(Phone phone) {
        this.mPhone = phone;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.telephony.action.CARRIER_CONFIG_CHANGED");
        phone.getContext().registerReceiverAsUser(this.mConfigChangedReceiver, UserHandle.ALL, intentFilter, null, null);
        resetRatFamilyMap();
    }

    /* JADX WARNING: Missing block: B:16:0x002a, code skipped:
            return r5;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int ratchetRat(int oldRat, int newRat) {
        synchronized (this.mRatFamilyMap) {
            SparseIntArray oldFamily = (SparseIntArray) this.mRatFamilyMap.get(oldRat);
            if (oldFamily == null) {
                return newRat;
            }
            SparseIntArray newFamily = (SparseIntArray) this.mRatFamilyMap.get(newRat);
            if (newFamily != oldFamily) {
                return newRat;
            }
            int i = newFamily.get(oldRat, -1) > newFamily.get(newRat, -1) ? oldRat : newRat;
        }
    }

    public void ratchet(ServiceState oldSS, ServiceState newSS, boolean locationChange) {
        if (!locationChange && isSameRatFamily(oldSS, newSS)) {
            updateBandwidths(oldSS.getCellBandwidths(), newSS);
        }
        boolean z = false;
        if (locationChange) {
            this.mVoiceRatchetEnabled = false;
            this.mDataRatchetEnabled = false;
            return;
        }
        boolean newUsingCA = false;
        if (newSS.getRilDataRadioTechnology() == 14 || newSS.getRilDataRadioTechnology() == 19) {
            if (oldSS.isUsingCarrierAggregation() || newSS.isUsingCarrierAggregation() || newSS.getCellBandwidths().length > 1) {
                z = true;
            }
            newUsingCA = z;
        }
        if (this.mVoiceRatchetEnabled) {
            newSS.setRilVoiceRadioTechnology(ratchetRat(oldSS.getRilVoiceRadioTechnology(), newSS.getRilVoiceRadioTechnology()));
        } else if (oldSS.getRilVoiceRadioTechnology() != newSS.getRilVoiceRadioTechnology()) {
            this.mVoiceRatchetEnabled = true;
        }
        if (this.mDataRatchetEnabled) {
            newSS.setRilDataRadioTechnology(ratchetRat(oldSS.getRilDataRadioTechnology(), newSS.getRilDataRadioTechnology()));
        } else if (oldSS.getRilDataRadioTechnology() != newSS.getRilDataRadioTechnology()) {
            this.mDataRatchetEnabled = true;
        }
        newSS.setIsUsingCarrierAggregation(newUsingCA);
    }

    /* JADX WARNING: Missing block: B:15:0x0038, code skipped:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isSameRatFamily(ServiceState ss1, ServiceState ss2) {
        synchronized (this.mRatFamilyMap) {
            if (ss1.getRilDataRadioTechnology() == ss2.getRilDataRadioTechnology()) {
                return true;
            }
            boolean z = false;
            if (this.mRatFamilyMap.get(ss1.getRilDataRadioTechnology()) == null) {
                return false;
            } else if (this.mRatFamilyMap.get(ss1.getRilDataRadioTechnology()) == this.mRatFamilyMap.get(ss2.getRilDataRadioTechnology())) {
                z = true;
            }
        }
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
            for (String split : ratFamilies) {
                String split2;
                String[] rats = split2.split(",");
                if (rats.length >= 2) {
                    SparseIntArray currentFamily = new SparseIntArray(rats.length);
                    int length = rats.length;
                    int pos = 0;
                    int pos2 = 0;
                    while (pos2 < length) {
                        String ratString = rats[pos2];
                        try {
                            int ratInt = Integer.parseInt(ratString.trim());
                            if (this.mRatFamilyMap.get(ratInt) != null) {
                                split2 = LOG_TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("RAT listed twice: ");
                                stringBuilder.append(ratString);
                                Rlog.e(split2, stringBuilder.toString());
                                break;
                            }
                            int pos3 = pos + 1;
                            currentFamily.put(ratInt, pos);
                            this.mRatFamilyMap.put(ratInt, currentFamily);
                            pos2++;
                            pos = pos3;
                        } catch (NumberFormatException e) {
                            String str = LOG_TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("NumberFormatException on ");
                            stringBuilder2.append(ratString);
                            Rlog.e(str, stringBuilder2.toString());
                        }
                    }
                    continue;
                }
            }
        }
    }
}
