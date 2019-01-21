package com.android.internal.telephony.uicc;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Handler;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.TelephonyComponentFactory;
import com.android.internal.telephony.cat.CatService;
import com.android.internal.telephony.uicc.IccCardApplicationStatus.AppType;
import com.android.internal.telephony.uicc.IccCardStatus.CardState;
import com.android.internal.telephony.uicc.IccCardStatus.PinState;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;

public class UiccCard extends AbstractUiccCard {
    protected static final boolean DBG = true;
    public static final String EXTRA_ICC_CARD_ADDED = "com.android.internal.telephony.uicc.ICC_CARD_ADDED";
    protected static final String LOG_TAG = "UiccCard";
    protected String mCardId;
    private CardState mCardState;
    private CommandsInterface mCi;
    private Context mContext;
    protected Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what != 30) {
                super.handleMessage(msg);
                return;
            }
            UiccCard.this.log("EVENT_CARD_UIM_LOCK");
            UiccCard.this.bCardUimLocked = true;
            UiccCard.this.displayUimTipDialog(UiccCard.this.mContext, 33685795);
        }
    };
    private String mIccid;
    private final Object mLock;
    private final int mPhoneId;
    private UiccProfile mUiccProfile;

    public UiccCard(Context c, CommandsInterface ci, IccCardStatus ics, int phoneId, Object lock) {
        log("Creating");
        this.mCardState = ics.mCardState;
        this.mPhoneId = phoneId;
        this.mLock = lock;
        update(c, ci, ics);
        if (this.mPhoneId == 0) {
            ci.registerForUimLockcard(this.mHandler, 30, Integer.valueOf(0));
            ci.getIccCardStatus(null);
        }
    }

    public void dispose() {
        synchronized (this.mLock) {
            log("Disposing card");
            if (this.mUiccProfile != null) {
                this.mUiccProfile.dispose();
            }
            this.mUiccProfile = null;
            if (this.mPhoneId == 0) {
                this.mCi.unregisterForUimLockcard(this.mHandler);
            }
        }
    }

    public void update(Context c, CommandsInterface ci, IccCardStatus ics) {
        synchronized (this.mLock) {
            this.mCardState = ics.mCardState;
            this.mContext = c;
            this.mCi = ci;
            this.mIccid = ics.iccid;
            updateCardId();
            if (this.mUiccProfile == null) {
                this.mUiccProfile = TelephonyComponentFactory.getInstance().makeUiccProfile(this.mContext, this.mCi, ics, this.mPhoneId, this, this.mLock);
            } else {
                this.mUiccProfile.update(this.mContext, this.mCi, ics);
            }
        }
    }

    public CatService getCatService() {
        if (this.mUiccProfile != null) {
            return this.mUiccProfile.getCatService();
        }
        return null;
    }

    protected void finalize() {
        log("UiccCard finalized");
    }

    protected void updateCardId() {
        this.mCardId = this.mIccid;
    }

    @Deprecated
    public void registerForCarrierPrivilegeRulesLoaded(Handler h, int what, Object obj) {
        synchronized (this.mLock) {
            if (this.mUiccProfile != null) {
                this.mUiccProfile.registerForCarrierPrivilegeRulesLoaded(h, what, obj);
            } else {
                loge("registerForCarrierPrivilegeRulesLoaded Failed!");
            }
        }
    }

    @Deprecated
    public void unregisterForCarrierPrivilegeRulesLoaded(Handler h) {
        synchronized (this.mLock) {
            if (this.mUiccProfile != null) {
                this.mUiccProfile.unregisterForCarrierPrivilegeRulesLoaded(h);
            } else {
                loge("unregisterForCarrierPrivilegeRulesLoaded Failed!");
            }
        }
    }

    @Deprecated
    public boolean isApplicationOnIcc(AppType type) {
        synchronized (this.mLock) {
            if (this.mUiccProfile != null) {
                boolean isApplicationOnIcc = this.mUiccProfile.isApplicationOnIcc(type);
                return isApplicationOnIcc;
            }
            return false;
        }
    }

    public CardState getCardState() {
        return this.mCardState;
    }

    @Deprecated
    public PinState getUniversalPinState() {
        synchronized (this.mLock) {
            PinState universalPinState;
            if (this.mUiccProfile != null) {
                universalPinState = this.mUiccProfile.getUniversalPinState();
                return universalPinState;
            }
            universalPinState = PinState.PINSTATE_UNKNOWN;
            return universalPinState;
        }
    }

    @Deprecated
    public UiccCardApplication getApplication(int family) {
        synchronized (this.mLock) {
            if (this.mUiccProfile != null) {
                UiccCardApplication application = this.mUiccProfile.getApplication(family);
                return application;
            }
            return null;
        }
    }

    @Deprecated
    public UiccCardApplication getApplicationIndex(int index) {
        synchronized (this.mLock) {
            if (this.mUiccProfile != null) {
                UiccCardApplication applicationIndex = this.mUiccProfile.getApplicationIndex(index);
                return applicationIndex;
            }
            return null;
        }
    }

    @Deprecated
    public UiccCardApplication getApplicationByType(int type) {
        synchronized (this.mLock) {
            if (this.mUiccProfile != null) {
                UiccCardApplication applicationByType = this.mUiccProfile.getApplicationByType(type);
                return applicationByType;
            }
            return null;
        }
    }

    @Deprecated
    public boolean resetAppWithAid(String aid) {
        synchronized (this.mLock) {
            if (this.mUiccProfile != null) {
                boolean resetAppWithAid = this.mUiccProfile.resetAppWithAid(aid);
                return resetAppWithAid;
            }
            return false;
        }
    }

    @Deprecated
    public void iccOpenLogicalChannel(String AID, int p2, Message response) {
        if (this.mUiccProfile != null) {
            this.mUiccProfile.iccOpenLogicalChannel(AID, p2, response);
        } else {
            loge("iccOpenLogicalChannel Failed!");
        }
    }

    @Deprecated
    public void iccCloseLogicalChannel(int channel, Message response) {
        if (this.mUiccProfile != null) {
            this.mUiccProfile.iccCloseLogicalChannel(channel, response);
        } else {
            loge("iccCloseLogicalChannel Failed!");
        }
    }

    @Deprecated
    public void iccTransmitApduLogicalChannel(int channel, int cla, int command, int p1, int p2, int p3, String data, Message response) {
        if (this.mUiccProfile != null) {
            this.mUiccProfile.iccTransmitApduLogicalChannel(channel, cla, command, p1, p2, p3, data, response);
        } else {
            loge("iccTransmitApduLogicalChannel Failed!");
        }
    }

    @Deprecated
    public void iccTransmitApduBasicChannel(int cla, int command, int p1, int p2, int p3, String data, Message response) {
        if (this.mUiccProfile != null) {
            this.mUiccProfile.iccTransmitApduBasicChannel(cla, command, p1, p2, p3, data, response);
        } else {
            loge("iccTransmitApduBasicChannel Failed!");
        }
    }

    @Deprecated
    public void iccExchangeSimIO(int fileID, int command, int p1, int p2, int p3, String pathID, Message response) {
        if (this.mUiccProfile != null) {
            this.mUiccProfile.iccExchangeSimIO(fileID, command, p1, p2, p3, pathID, response);
        } else {
            loge("iccExchangeSimIO Failed!");
        }
    }

    @Deprecated
    public void sendEnvelopeWithStatus(String contents, Message response) {
        if (this.mUiccProfile != null) {
            this.mUiccProfile.sendEnvelopeWithStatus(contents, response);
        } else {
            loge("sendEnvelopeWithStatus Failed!");
        }
    }

    @Deprecated
    public int getNumApplications() {
        if (this.mUiccProfile != null) {
            return this.mUiccProfile.getNumApplications();
        }
        return 0;
    }

    public int getPhoneId() {
        return this.mPhoneId;
    }

    public UiccProfile getUiccProfile() {
        return this.mUiccProfile;
    }

    @Deprecated
    public boolean areCarrierPriviligeRulesLoaded() {
        if (this.mUiccProfile != null) {
            return this.mUiccProfile.areCarrierPriviligeRulesLoaded();
        }
        return false;
    }

    @Deprecated
    public boolean hasCarrierPrivilegeRules() {
        if (this.mUiccProfile != null) {
            return this.mUiccProfile.hasCarrierPrivilegeRules();
        }
        return false;
    }

    @Deprecated
    public int getCarrierPrivilegeStatus(Signature signature, String packageName) {
        if (this.mUiccProfile != null) {
            return this.mUiccProfile.getCarrierPrivilegeStatus(signature, packageName);
        }
        return -1;
    }

    @Deprecated
    public int getCarrierPrivilegeStatus(PackageManager packageManager, String packageName) {
        if (this.mUiccProfile != null) {
            return this.mUiccProfile.getCarrierPrivilegeStatus(packageManager, packageName);
        }
        return -1;
    }

    @Deprecated
    public int getCarrierPrivilegeStatus(PackageInfo packageInfo) {
        if (this.mUiccProfile != null) {
            return this.mUiccProfile.getCarrierPrivilegeStatus(packageInfo);
        }
        return -1;
    }

    @Deprecated
    public int getCarrierPrivilegeStatusForCurrentTransaction(PackageManager packageManager) {
        if (this.mUiccProfile != null) {
            return this.mUiccProfile.getCarrierPrivilegeStatusForCurrentTransaction(packageManager);
        }
        return -1;
    }

    @Deprecated
    public List<String> getCarrierPackageNamesForIntent(PackageManager packageManager, Intent intent) {
        if (this.mUiccProfile != null) {
            return this.mUiccProfile.getCarrierPackageNamesForIntent(packageManager, intent);
        }
        return null;
    }

    @Deprecated
    public boolean setOperatorBrandOverride(String brand) {
        if (this.mUiccProfile != null) {
            return this.mUiccProfile.setOperatorBrandOverride(brand);
        }
        return false;
    }

    @Deprecated
    public String getOperatorBrandOverride() {
        if (this.mUiccProfile != null) {
            return this.mUiccProfile.getOperatorBrandOverride();
        }
        return null;
    }

    public String getIccId() {
        if (this.mIccid != null) {
            return this.mIccid;
        }
        if (this.mUiccProfile != null) {
            return this.mUiccProfile.getIccId();
        }
        return null;
    }

    public String getCardId() {
        if (this.mCardId != null) {
            return this.mCardId;
        }
        if (this.mUiccProfile != null) {
            return this.mUiccProfile.getIccId();
        }
        return null;
    }

    private void log(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    private void loge(String msg) {
        Rlog.e(LOG_TAG, msg);
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("UiccCard:");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" mCi=");
        stringBuilder.append(this.mCi);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" mCardState=");
        stringBuilder.append(this.mCardState);
        pw.println(stringBuilder.toString());
        pw.println();
        if (this.mUiccProfile != null) {
            this.mUiccProfile.dump(fd, pw, args);
        }
    }

    public int getGsmUmtsSubscriptionAppIndex() {
        if (this.mUiccProfile != null) {
            return this.mUiccProfile.getGsmUmtsSubscriptionAppIndex();
        }
        return -1;
    }

    public int getCdmaSubscriptionAppIndex() {
        if (this.mUiccProfile != null) {
            return this.mUiccProfile.getCdmaSubscriptionAppIndex();
        }
        return -1;
    }

    public boolean getCardUimLocked() {
        return this.bCardUimLocked;
    }
}
