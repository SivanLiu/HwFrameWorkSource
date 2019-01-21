package com.android.internal.telephony;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.Telephony.CarrierId;
import android.provider.Telephony.CarrierId.All;
import android.provider.Telephony.Carriers;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.SubscriptionManager.OnSubscriptionsChangedListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.LocalLog;
import com.android.internal.telephony.metrics.TelephonyMetrics;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccController;
import com.android.internal.telephony.uicc.UiccProfile;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CarrierIdentifier extends Handler {
    private static final int CARRIER_ID_DB_UPDATE_EVENT = 6;
    private static final Uri CONTENT_URL_PREFER_APN = Uri.withAppendedPath(Carriers.CONTENT_URI, "preferapn");
    private static final boolean DBG = true;
    private static final int ICC_CHANGED_EVENT = 4;
    private static final String LOG_TAG = CarrierIdentifier.class.getSimpleName();
    private static final String OPERATOR_BRAND_OVERRIDE_PREFIX = "operator_branding_";
    private static final int PREFER_APN_UPDATE_EVENT = 5;
    private static final int SIM_ABSENT_EVENT = 2;
    private static final int SIM_LOAD_EVENT = 1;
    private static final int SPN_OVERRIDE_EVENT = 3;
    private static final boolean VDBG = Rlog.isLoggable(LOG_TAG, 2);
    private int mCarrierId = -1;
    private final LocalLog mCarrierIdLocalLog = new LocalLog(20);
    private List<CarrierMatchingRule> mCarrierMatchingRulesOnMccMnc = new ArrayList();
    private String mCarrierName;
    private final ContentObserver mContentObserver = new ContentObserver(this) {
        public void onChange(boolean selfChange, Uri uri) {
            StringBuilder stringBuilder;
            if (CarrierIdentifier.CONTENT_URL_PREFER_APN.equals(uri.getLastPathSegment())) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("onChange URI: ");
                stringBuilder.append(uri);
                CarrierIdentifier.logd(stringBuilder.toString());
                CarrierIdentifier.this.sendEmptyMessage(5);
            } else if (All.CONTENT_URI.equals(uri)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("onChange URI: ");
                stringBuilder.append(uri);
                CarrierIdentifier.logd(stringBuilder.toString());
                CarrierIdentifier.this.sendEmptyMessage(6);
            }
        }
    };
    private Context mContext;
    private IccRecords mIccRecords;
    private final SubscriptionsChangedListener mOnSubscriptionsChangedListener = new SubscriptionsChangedListener(this, null);
    private Phone mPhone;
    private String mPreferApn;
    private String mSpn = "";
    private final TelephonyManager mTelephonyMgr;
    private UiccProfile mUiccProfile;

    private static class CarrierMatchingRule {
        private static final int SCORE_APN = 1;
        private static final int SCORE_GID1 = 16;
        private static final int SCORE_GID2 = 8;
        private static final int SCORE_ICCID_PREFIX = 32;
        private static final int SCORE_IMSI_PREFIX = 64;
        private static final int SCORE_INVALID = -1;
        private static final int SCORE_MCCMNC = 128;
        private static final int SCORE_PLMN = 4;
        private static final int SCORE_SPN = 2;
        private String mApn;
        private int mCid;
        private String mGid1;
        private String mGid2;
        private String mIccidPrefix;
        private String mImsiPrefixPattern;
        private String mMccMnc;
        private String mName;
        private String mPlmn;
        private int mScore = 0;
        private String mSpn;

        CarrierMatchingRule(String mccmnc, String imsiPrefixPattern, String iccidPrefix, String gid1, String gid2, String plmn, String spn, String apn, int cid, String name) {
            this.mMccMnc = mccmnc;
            this.mImsiPrefixPattern = imsiPrefixPattern;
            this.mIccidPrefix = iccidPrefix;
            this.mGid1 = gid1;
            this.mGid2 = gid2;
            this.mPlmn = plmn;
            this.mSpn = spn;
            this.mApn = apn;
            this.mCid = cid;
            this.mName = name;
        }

        public void match(CarrierMatchingRule subscriptionRule) {
            this.mScore = 0;
            if (this.mMccMnc != null) {
                if (CarrierIdentifier.equals(subscriptionRule.mMccMnc, this.mMccMnc, false)) {
                    this.mScore += 128;
                } else {
                    this.mScore = -1;
                    return;
                }
            }
            if (this.mImsiPrefixPattern != null) {
                if (imsiPrefixMatch(subscriptionRule.mImsiPrefixPattern, this.mImsiPrefixPattern)) {
                    this.mScore += 64;
                } else {
                    this.mScore = -1;
                    return;
                }
            }
            if (this.mIccidPrefix != null) {
                if (iccidPrefixMatch(subscriptionRule.mIccidPrefix, this.mIccidPrefix)) {
                    this.mScore += 32;
                } else {
                    this.mScore = -1;
                    return;
                }
            }
            if (this.mGid1 != null) {
                if (CarrierIdentifier.equals(subscriptionRule.mGid1, this.mGid1, true)) {
                    this.mScore += 16;
                } else {
                    this.mScore = -1;
                    return;
                }
            }
            if (this.mGid2 != null) {
                if (CarrierIdentifier.equals(subscriptionRule.mGid2, this.mGid2, true)) {
                    this.mScore += 8;
                } else {
                    this.mScore = -1;
                    return;
                }
            }
            if (this.mPlmn != null) {
                if (CarrierIdentifier.equals(subscriptionRule.mPlmn, this.mPlmn, true)) {
                    this.mScore += 4;
                } else {
                    this.mScore = -1;
                    return;
                }
            }
            if (this.mSpn != null) {
                if (CarrierIdentifier.equals(subscriptionRule.mSpn, this.mSpn, true)) {
                    this.mScore += 2;
                } else {
                    this.mScore = -1;
                    return;
                }
            }
            if (this.mApn != null) {
                if (CarrierIdentifier.equals(subscriptionRule.mApn, this.mApn, true)) {
                    this.mScore++;
                } else {
                    this.mScore = -1;
                }
            }
        }

        private boolean imsiPrefixMatch(String imsi, String prefixXPattern) {
            if (TextUtils.isEmpty(prefixXPattern)) {
                return true;
            }
            if (TextUtils.isEmpty(imsi) || imsi.length() < prefixXPattern.length()) {
                return false;
            }
            int i = 0;
            while (i < prefixXPattern.length()) {
                if (prefixXPattern.charAt(i) != 'x' && prefixXPattern.charAt(i) != 'X' && prefixXPattern.charAt(i) != imsi.charAt(i)) {
                    return false;
                }
                i++;
            }
            return true;
        }

        private boolean iccidPrefixMatch(String iccid, String prefix) {
            if (iccid == null || prefix == null) {
                return false;
            }
            return iccid.startsWith(prefix);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[CarrierMatchingRule] - mccmnc: ");
            stringBuilder.append(this.mMccMnc);
            stringBuilder.append(" gid1: ");
            stringBuilder.append(this.mGid1);
            stringBuilder.append(" gid2: ");
            stringBuilder.append(this.mGid2);
            stringBuilder.append(" plmn: ");
            stringBuilder.append(this.mPlmn);
            stringBuilder.append(" imsi_prefix: ");
            stringBuilder.append(this.mImsiPrefixPattern);
            stringBuilder.append(" iccid_prefix");
            stringBuilder.append(this.mIccidPrefix);
            stringBuilder.append(" spn: ");
            stringBuilder.append(this.mSpn);
            stringBuilder.append(" apn: ");
            stringBuilder.append(this.mApn);
            stringBuilder.append(" name: ");
            stringBuilder.append(this.mName);
            stringBuilder.append(" cid: ");
            stringBuilder.append(this.mCid);
            stringBuilder.append(" score: ");
            stringBuilder.append(this.mScore);
            return stringBuilder.toString();
        }
    }

    private class SubscriptionsChangedListener extends OnSubscriptionsChangedListener {
        final AtomicInteger mPreviousSubId;

        private SubscriptionsChangedListener() {
            this.mPreviousSubId = new AtomicInteger(-1);
        }

        /* synthetic */ SubscriptionsChangedListener(CarrierIdentifier x0, AnonymousClass1 x1) {
            this();
        }

        public void onSubscriptionsChanged() {
            int subId = CarrierIdentifier.this.mPhone.getSubId();
            if (this.mPreviousSubId.getAndSet(subId) != subId) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("SubscriptionListener.onSubscriptionInfoChanged subId: ");
                stringBuilder.append(this.mPreviousSubId);
                CarrierIdentifier.logd(stringBuilder.toString());
                if (SubscriptionManager.isValidSubscriptionId(subId)) {
                    CarrierIdentifier.this.sendEmptyMessage(1);
                } else {
                    CarrierIdentifier.this.sendEmptyMessage(2);
                }
            }
        }
    }

    public CarrierIdentifier(Phone phone) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Creating CarrierIdentifier[");
        stringBuilder.append(phone.getPhoneId());
        stringBuilder.append("]");
        logd(stringBuilder.toString());
        this.mContext = phone.getContext();
        this.mPhone = phone;
        this.mTelephonyMgr = TelephonyManager.from(this.mContext);
        this.mContext.getContentResolver().registerContentObserver(CONTENT_URL_PREFER_APN, false, this.mContentObserver);
        this.mContext.getContentResolver().registerContentObserver(All.CONTENT_URI, false, this.mContentObserver);
        SubscriptionManager.from(this.mContext).addOnSubscriptionsChangedListener(this.mOnSubscriptionsChangedListener);
        UiccController.getInstance().registerForIccChanged(this, 4, null);
    }

    public void handleMessage(Message msg) {
        StringBuilder stringBuilder;
        if (VDBG) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("handleMessage: ");
            stringBuilder.append(msg.what);
            logd(stringBuilder.toString());
        }
        String spn;
        StringBuilder stringBuilder2;
        switch (msg.what) {
            case 1:
            case 6:
                this.mSpn = this.mTelephonyMgr.getSimOperatorNameForPhone(this.mPhone.getPhoneId());
                this.mPreferApn = getPreferApn();
                loadCarrierMatchingRulesOnMccMnc();
                return;
            case 2:
                this.mCarrierMatchingRulesOnMccMnc.clear();
                this.mSpn = null;
                this.mPreferApn = null;
                updateCarrierIdAndName(-1, null);
                return;
            case 3:
                spn = this.mTelephonyMgr.getSimOperatorNameForPhone(this.mPhone.getPhoneId());
                if (!equals(this.mSpn, spn, true)) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("[updateSpn] from:");
                    stringBuilder2.append(this.mSpn);
                    stringBuilder2.append(" to:");
                    stringBuilder2.append(spn);
                    logd(stringBuilder2.toString());
                    this.mSpn = spn;
                    matchCarrier();
                    return;
                }
                return;
            case 4:
                IccRecords newIccRecords = UiccController.getInstance().getIccRecords(this.mPhone.getPhoneId(), 1);
                if (this.mIccRecords != newIccRecords) {
                    if (this.mIccRecords != null) {
                        logd("Removing stale icc objects.");
                        this.mIccRecords.unregisterForRecordsLoaded(this);
                        this.mIccRecords.unregisterForRecordsOverride(this);
                        this.mIccRecords = null;
                    }
                    if (newIccRecords != null) {
                        logd("new Icc object");
                        newIccRecords.registerForRecordsLoaded(this, 1, null);
                        newIccRecords.registerForRecordsOverride(this, 1, null);
                        this.mIccRecords = newIccRecords;
                    }
                }
                UiccProfile uiccProfile = UiccController.getInstance().getUiccProfileForPhone(this.mPhone.getPhoneId());
                if (this.mUiccProfile != uiccProfile) {
                    if (this.mUiccProfile != null) {
                        logd("unregister operatorBrandOverride");
                        this.mUiccProfile.unregisterForOperatorBrandOverride(this);
                        this.mUiccProfile = null;
                    }
                    if (uiccProfile != null) {
                        logd("register operatorBrandOverride");
                        uiccProfile.registerForOpertorBrandOverride(this, 3, null);
                        this.mUiccProfile = uiccProfile;
                        return;
                    }
                    return;
                }
                return;
            case 5:
                spn = getPreferApn();
                if (!equals(this.mPreferApn, spn, true)) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("[updatePreferApn] from:");
                    stringBuilder2.append(this.mPreferApn);
                    stringBuilder2.append(" to:");
                    stringBuilder2.append(spn);
                    logd(stringBuilder2.toString());
                    this.mPreferApn = spn;
                    matchCarrier();
                    return;
                }
                return;
            default:
                stringBuilder = new StringBuilder();
                stringBuilder.append("invalid msg: ");
                stringBuilder.append(msg.what);
                loge(stringBuilder.toString());
                return;
        }
    }

    private void loadCarrierMatchingRulesOnMccMnc() {
        Cursor cursor;
        try {
            cursor = this.mContext.getContentResolver().query(All.CONTENT_URI, null, "mccmnc=?", new String[]{this.mTelephonyMgr.getSimOperatorNumericForPhone(this.mPhone.getPhoneId())}, null);
            if (cursor != null) {
                if (VDBG) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("[loadCarrierMatchingRules]- ");
                    stringBuilder.append(cursor.getCount());
                    stringBuilder.append(" Records(s) in DB mccmnc: ");
                    stringBuilder.append(mccmnc);
                    logd(stringBuilder.toString());
                }
                this.mCarrierMatchingRulesOnMccMnc.clear();
                while (cursor.moveToNext()) {
                    this.mCarrierMatchingRulesOnMccMnc.add(makeCarrierMatchingRule(cursor));
                }
                matchCarrier();
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception ex) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("[loadCarrierMatchingRules]- ex: ");
            stringBuilder2.append(ex);
            loge(stringBuilder2.toString());
        } catch (Throwable th) {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /* JADX WARNING: Missing block: B:16:0x0095, code skipped:
            if (r0 == null) goto L_0x00a3;
     */
    /* JADX WARNING: Missing block: B:20:0x009e, code skipped:
            if (r0 != null) goto L_0x00a0;
     */
    /* JADX WARNING: Missing block: B:21:0x00a0, code skipped:
            r0.close();
     */
    /* JADX WARNING: Missing block: B:23:0x00a4, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private String getPreferApn() {
        ContentResolver contentResolver = this.mContext.getContentResolver();
        Uri uri = Carriers.CONTENT_URI;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("preferapn/subId/");
        stringBuilder.append(this.mPhone.getSubId());
        Cursor cursor = contentResolver.query(Uri.withAppendedPath(uri, stringBuilder.toString()), new String[]{"apn"}, null, null, null);
        if (cursor != null) {
            try {
                if (VDBG) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("[getPreferApn]- ");
                    stringBuilder2.append(cursor.getCount());
                    stringBuilder2.append(" Records(s) in DB");
                    logd(stringBuilder2.toString());
                }
                if (cursor.moveToNext()) {
                    String apn = cursor.getString(cursor.getColumnIndexOrThrow("apn"));
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("[getPreferApn]- ");
                    stringBuilder.append(apn);
                    logd(stringBuilder.toString());
                    if (cursor != null) {
                        cursor.close();
                    }
                    return apn;
                }
            } catch (Exception ex) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("[getPreferApn]- exception: ");
                stringBuilder.append(ex);
                loge(stringBuilder.toString());
            } catch (Throwable th) {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    private void updateCarrierIdAndName(int cid, String name) {
        StringBuilder stringBuilder;
        boolean update = false;
        if (!equals(name, this.mCarrierName, true)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("[updateCarrierName] from:");
            stringBuilder.append(this.mCarrierName);
            stringBuilder.append(" to:");
            stringBuilder.append(name);
            logd(stringBuilder.toString());
            this.mCarrierName = name;
            update = true;
        }
        if (cid != this.mCarrierId) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("[updateCarrierId] from:");
            stringBuilder.append(this.mCarrierId);
            stringBuilder.append(" to:");
            stringBuilder.append(cid);
            logd(stringBuilder.toString());
            this.mCarrierId = cid;
            update = true;
        }
        if (update) {
            LocalLog localLog = this.mCarrierIdLocalLog;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("[updateCarrierIdAndName] cid:");
            stringBuilder2.append(this.mCarrierId);
            stringBuilder2.append(" name:");
            stringBuilder2.append(this.mCarrierName);
            localLog.log(stringBuilder2.toString());
            Intent intent = new Intent("android.telephony.action.SUBSCRIPTION_CARRIER_IDENTITY_CHANGED");
            intent.putExtra("android.telephony.extra.CARRIER_ID", this.mCarrierId);
            intent.putExtra("android.telephony.extra.CARRIER_NAME", this.mCarrierName);
            intent.putExtra("android.telephony.extra.SUBSCRIPTION_ID", this.mPhone.getSubId());
            this.mContext.sendBroadcast(intent);
            ContentValues cv = new ContentValues();
            cv.put("carrier_id", Integer.valueOf(this.mCarrierId));
            cv.put("carrier_name", this.mCarrierName);
            this.mContext.getContentResolver().update(Uri.withAppendedPath(CarrierId.CONTENT_URI, Integer.toString(this.mPhone.getSubId())), cv, null, null);
        }
    }

    private CarrierMatchingRule makeCarrierMatchingRule(Cursor cursor) {
        return new CarrierMatchingRule(cursor.getString(cursor.getColumnIndexOrThrow("mccmnc")), cursor.getString(cursor.getColumnIndexOrThrow("imsi_prefix_xpattern")), cursor.getString(cursor.getColumnIndexOrThrow("iccid_prefix")), cursor.getString(cursor.getColumnIndexOrThrow("gid1")), cursor.getString(cursor.getColumnIndexOrThrow("gid2")), cursor.getString(cursor.getColumnIndexOrThrow("plmn")), cursor.getString(cursor.getColumnIndexOrThrow("spn")), cursor.getString(cursor.getColumnIndexOrThrow("apn")), cursor.getInt(cursor.getColumnIndexOrThrow("carrier_id")), cursor.getString(cursor.getColumnIndexOrThrow("carrier_name")));
    }

    private void matchCarrier() {
        if (SubscriptionManager.isValidSubscriptionId(this.mPhone.getSubId())) {
            String mccmnc = this.mTelephonyMgr.getSimOperatorNumericForPhone(this.mPhone.getPhoneId());
            String iccid = this.mPhone.getIccSerialNumber();
            String gid1 = this.mPhone.getGroupIdLevel1();
            String gid2 = this.mPhone.getGroupIdLevel2();
            String imsi = this.mPhone.getSubscriberId();
            String plmn = this.mPhone.getPlmn();
            String spn = this.mSpn;
            String apn = this.mPreferApn;
            if (VDBG) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("[matchCarrier] mnnmnc:");
                stringBuilder.append(mccmnc);
                stringBuilder.append(" gid1: ");
                stringBuilder.append(gid1);
                stringBuilder.append(" gid2: ");
                stringBuilder.append(gid2);
                stringBuilder.append(" imsi: ");
                stringBuilder.append(Rlog.pii(LOG_TAG, imsi));
                stringBuilder.append(" iccid: ");
                stringBuilder.append(Rlog.pii(LOG_TAG, iccid));
                stringBuilder.append(" plmn: ");
                stringBuilder.append(plmn);
                stringBuilder.append(" spn: ");
                stringBuilder.append(spn);
                stringBuilder.append(" apn: ");
                stringBuilder.append(apn);
                logd(stringBuilder.toString());
            }
            CarrierMatchingRule subscriptionRule = new CarrierMatchingRule(mccmnc, imsi, iccid, gid1, gid2, plmn, spn, apn, -1, null);
            int maxScore = -1;
            CarrierMatchingRule maxRule = null;
            for (CarrierMatchingRule rule : this.mCarrierMatchingRulesOnMccMnc) {
                rule.match(subscriptionRule);
                if (rule.mScore > maxScore) {
                    maxScore = rule.mScore;
                    maxRule = rule;
                }
            }
            String str = null;
            StringBuilder stringBuilder2;
            if (maxScore == -1) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("[matchCarrier - no match] cid: -1 name: ");
                stringBuilder2.append(null);
                logd(stringBuilder2.toString());
                updateCarrierIdAndName(-1, null);
            } else {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("[matchCarrier] cid: ");
                stringBuilder2.append(maxRule.mCid);
                stringBuilder2.append(" name: ");
                stringBuilder2.append(maxRule.mName);
                logd(stringBuilder2.toString());
                updateCarrierIdAndName(maxRule.mCid, maxRule.mName);
            }
            String unknownGid1ToLog = ((maxScore & 16) != 0 || TextUtils.isEmpty(subscriptionRule.mGid1)) ? null : subscriptionRule.mGid1;
            if ((maxScore == -1 || (maxScore & 16) == 0) && !TextUtils.isEmpty(subscriptionRule.mMccMnc)) {
                str = subscriptionRule.mMccMnc;
            }
            TelephonyMetrics.getInstance().writeCarrierIdMatchingEvent(this.mPhone.getPhoneId(), getCarrierListVersion(), this.mCarrierId, str, unknownGid1ToLog);
            return;
        }
        logd("[matchCarrier]skip before sim records loaded");
    }

    public int getCarrierListVersion() {
        Cursor cursor = this.mContext.getContentResolver().query(Uri.withAppendedPath(All.CONTENT_URI, "get_version"), null, null, null);
        cursor.moveToFirst();
        return cursor.getInt(0);
    }

    public int getCarrierId() {
        return this.mCarrierId;
    }

    public String getCarrierName() {
        return this.mCarrierName;
    }

    private static boolean equals(String a, String b, boolean ignoreCase) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return ignoreCase ? a.equalsIgnoreCase(b) : a.equals(b);
    }

    private static void logd(String str) {
        Rlog.d(LOG_TAG, str);
    }

    private static void loge(String str) {
        Rlog.e(LOG_TAG, str);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        IndentingPrintWriter ipw = new IndentingPrintWriter(pw, "  ");
        ipw.println("mCarrierIdLocalLogs:");
        ipw.increaseIndent();
        this.mCarrierIdLocalLog.dump(fd, pw, args);
        ipw.decreaseIndent();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mCarrierId: ");
        stringBuilder.append(this.mCarrierId);
        ipw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mCarrierName: ");
        stringBuilder.append(this.mCarrierName);
        ipw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("version: ");
        stringBuilder.append(getCarrierListVersion());
        ipw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mCarrierMatchingRules on mccmnc: ");
        stringBuilder.append(this.mTelephonyMgr.getSimOperatorNumericForPhone(this.mPhone.getPhoneId()));
        ipw.println(stringBuilder.toString());
        ipw.increaseIndent();
        for (CarrierMatchingRule rule : this.mCarrierMatchingRulesOnMccMnc) {
            ipw.println(rule.toString());
        }
        ipw.decreaseIndent();
        stringBuilder = new StringBuilder();
        stringBuilder.append("mSpn: ");
        stringBuilder.append(this.mSpn);
        ipw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mPreferApn: ");
        stringBuilder.append(this.mPreferApn);
        ipw.println(stringBuilder.toString());
        ipw.flush();
    }
}
