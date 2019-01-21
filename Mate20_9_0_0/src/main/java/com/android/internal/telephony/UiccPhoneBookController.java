package com.android.internal.telephony;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.telephony.Rlog;
import com.android.internal.telephony.uicc.AdnRecord;
import java.util.List;

public class UiccPhoneBookController extends AbstractUiccPhoneBookController {
    private static final String TAG = "UiccPhoneBookController";
    private Phone[] mPhone;

    public UiccPhoneBookController(Phone[] phone) {
        if (ServiceManager.getService("simphonebook") == null) {
            ServiceManager.addService("simphonebook", this);
        }
        this.mPhone = phone;
    }

    public boolean updateAdnRecordsInEfBySearch(int efid, String oldTag, String oldPhoneNumber, String newTag, String newPhoneNumber, String pin2) throws RemoteException {
        return updateAdnRecordsInEfBySearchForSubscriber(getDefaultSubscription(), efid, oldTag, oldPhoneNumber, newTag, newPhoneNumber, pin2);
    }

    public boolean updateAdnRecordsInEfBySearchForSubscriber(int subId, int efid, String oldTag, String oldPhoneNumber, String newTag, String newPhoneNumber, String pin2) throws RemoteException {
        IccPhoneBookInterfaceManager iccPbkIntMgr = getIccPhoneBookInterfaceManager(subId);
        if (iccPbkIntMgr != null) {
            return iccPbkIntMgr.updateAdnRecordsInEfBySearch(efid, oldTag, oldPhoneNumber, newTag, newPhoneNumber, pin2);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateAdnRecordsInEfBySearch iccPbkIntMgr is null for Subscription:");
        stringBuilder.append(subId);
        Rlog.e(str, stringBuilder.toString());
        return false;
    }

    public boolean updateAdnRecordsInEfByIndex(int efid, String newTag, String newPhoneNumber, int index, String pin2) throws RemoteException {
        return updateAdnRecordsInEfByIndexForSubscriber(getDefaultSubscription(), efid, newTag, newPhoneNumber, index, pin2);
    }

    public boolean updateAdnRecordsInEfByIndexForSubscriber(int subId, int efid, String newTag, String newPhoneNumber, int index, String pin2) throws RemoteException {
        IccPhoneBookInterfaceManager iccPbkIntMgr = getIccPhoneBookInterfaceManager(subId);
        if (iccPbkIntMgr != null) {
            return iccPbkIntMgr.updateAdnRecordsInEfByIndex(efid, newTag, newPhoneNumber, index, pin2);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateAdnRecordsInEfByIndex iccPbkIntMgr is null for Subscription:");
        stringBuilder.append(subId);
        Rlog.e(str, stringBuilder.toString());
        return false;
    }

    public int[] getAdnRecordsSize(int efid) throws RemoteException {
        return getAdnRecordsSizeForSubscriber(getDefaultSubscription(), efid);
    }

    public int[] getAdnRecordsSizeForSubscriber(int subId, int efid) throws RemoteException {
        IccPhoneBookInterfaceManager iccPbkIntMgr = getIccPhoneBookInterfaceManager(subId);
        if (iccPbkIntMgr != null) {
            return iccPbkIntMgr.getAdnRecordsSize(efid);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getAdnRecordsSize iccPbkIntMgr is null for Subscription:");
        stringBuilder.append(subId);
        Rlog.e(str, stringBuilder.toString());
        return null;
    }

    public List<AdnRecord> getAdnRecordsInEf(int efid) throws RemoteException {
        return getAdnRecordsInEfForSubscriber(getDefaultSubscription(), efid);
    }

    public List<AdnRecord> getAdnRecordsInEfForSubscriber(int subId, int efid) throws RemoteException {
        IccPhoneBookInterfaceManager iccPbkIntMgr = getIccPhoneBookInterfaceManager(subId);
        if (iccPbkIntMgr != null) {
            return iccPbkIntMgr.getAdnRecordsInEf(efid);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getAdnRecordsInEf iccPbkIntMgr isnull for Subscription:");
        stringBuilder.append(subId);
        Rlog.e(str, stringBuilder.toString());
        return null;
    }

    private IccPhoneBookInterfaceManager getIccPhoneBookInterfaceManager(int subId) {
        String str;
        StringBuilder stringBuilder;
        try {
            return this.mPhone[SubscriptionController.getInstance().getPhoneId(subId)].getIccPhoneBookInterfaceManager();
        } catch (NullPointerException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Exception is :");
            stringBuilder.append(e.toString());
            stringBuilder.append(" For subscription :");
            stringBuilder.append(subId);
            Rlog.e(str, stringBuilder.toString());
            e.printStackTrace();
            return null;
        } catch (ArrayIndexOutOfBoundsException e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Exception is :");
            stringBuilder.append(e2.toString());
            stringBuilder.append(" For subscription :");
            stringBuilder.append(subId);
            Rlog.e(str, stringBuilder.toString());
            e2.printStackTrace();
            return null;
        }
    }

    private int getDefaultSubscription() {
        return PhoneFactory.getDefaultSubscription();
    }

    public int getAlphaTagEncodingLength(String alphaTag) {
        IccPhoneBookInterfaceManager iccPbkIntMgr = getIccPhoneBookInterfaceManager(getDefaultSubscription());
        if (iccPbkIntMgr != null) {
            return iccPbkIntMgr.getAlphaTagEncodingLength(alphaTag);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getAlphaTagEncodingLength iccPbkIntMgr isnull for Subscription:");
        stringBuilder.append(getDefaultSubscription());
        Rlog.e(str, stringBuilder.toString());
        return 0;
    }

    public IccPhoneBookInterfaceManager getIccPhoneBookInterfaceManagerHw(int subId) {
        return getIccPhoneBookInterfaceManager(subId);
    }

    public int getDefaultSubscriptionHw() {
        return getDefaultSubscription();
    }
}
