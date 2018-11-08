package com.huawei.android.telephony;

import android.content.Intent;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import com.android.internal.telephony.IIccPhoneBook;
import com.android.internal.telephony.IIccPhoneBook.Stub;

public final class IIccPhoneBookManagerEx {
    public static final String SLOT_ID = "subscription";
    private static final String TAG = "IIccPhoneBookManagerEx";
    private static IIccPhoneBookManagerEx sInstance = new IIccPhoneBookManagerEx();

    private IIccPhoneBookManagerEx() {
    }

    public static IIccPhoneBookManagerEx getDefault() {
        return sInstance;
    }

    public int[] getRecordsSize(int subscription) throws RemoteException {
        try {
            IIccPhoneBook iccIpb = Stub.asInterface(ServiceManager.getService("simphonebook"));
            if (iccIpb != null) {
                return iccIpb.getRecordsSizeUsingSubIdHW(subscription);
            }
        } catch (SecurityException ex) {
            Log.d(TAG, ex.toString());
        }
        return new int[0];
    }

    public int getSpareExt1Count(int subscription) throws RemoteException {
        int spareExt1count = -1;
        try {
            IIccPhoneBook iccIpb = Stub.asInterface(ServiceManager.getService("simphonebook"));
            if (iccIpb != null) {
                spareExt1count = iccIpb.getSpareExt1CountUsingSubIdHW(subscription);
            }
        } catch (SecurityException ex) {
            Log.d(TAG, ex.toString());
        }
        return spareExt1count;
    }

    public int[] getAdnRecordsSizeOnSubscription(int efid, int subscription) throws RemoteException {
        try {
            IIccPhoneBook iccIpb = Stub.asInterface(ServiceManager.getService("simphonebook"));
            if (iccIpb != null) {
                return iccIpb.getAdnRecordsSizeForSubscriber(subscription, efid);
            }
        } catch (SecurityException ex) {
            Log.d(TAG, ex.toString());
        }
        return new int[0];
    }

    public int getSoltIdInSimStateChangeIntent(Intent intent) {
        if (intent != null) {
            return intent.getIntExtra("subscription", -1);
        }
        return -1;
    }

    public int[] getRecordsSize() throws RemoteException {
        try {
            IIccPhoneBook iccIpb = Stub.asInterface(ServiceManager.getService("simphonebook"));
            if (iccIpb != null) {
                return iccIpb.getRecordsSizeHW();
            }
        } catch (SecurityException ex) {
            Log.d(TAG, ex.toString());
        }
        return new int[0];
    }

    public int[] getAdnRecordsSize(int efid) throws RemoteException {
        try {
            IIccPhoneBook iccIpb = Stub.asInterface(ServiceManager.getService("simphonebook"));
            if (iccIpb != null) {
                return iccIpb.getAdnRecordsSize(efid);
            }
        } catch (SecurityException ex) {
            Log.d(TAG, ex.toString());
        }
        return new int[0];
    }

    public int getAlphaTagEncodingLength(String alphaTag) throws RemoteException {
        try {
            IIccPhoneBook iccIpb = Stub.asInterface(ServiceManager.getService("simphonebook"));
            if (iccIpb != null) {
                return iccIpb.getAlphaTagEncodingLength(alphaTag);
            }
        } catch (SecurityException ex) {
            Log.d(TAG, ex.toString());
        }
        return -1;
    }

    public String getSecretCodeSubString(String input) {
        return IIccPhoneBookManagerChipsEx.getDefault().getSecretCodeSubString(input);
    }
}
