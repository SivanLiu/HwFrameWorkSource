package com.android.internal.telephony;

import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import com.android.internal.telephony.IIccPhoneBook.Stub;
import com.android.internal.telephony.uicc.AdnRecord;
import java.util.HashMap;
import java.util.List;

public class HwCustIccProviderUtilsImpl extends HwCustIccProviderUtils {
    private static final String DATA_FIXED_NUMBER = "*99#";
    private static final int FDN_EXISTS = 101;
    private static final int FDN_EXISTS_SUB = 102;
    private static final String FDN_NUM_VALUE = "exists";
    private static final boolean HWDBG = true;
    private static final String TAG = "HwCustIccProviderUtilsImpl";
    private boolean FDN_PRELOAD_CACHE = SystemProperties.getBoolean("ro.config.fdn.preload", HWDBG);
    private HashMap<String, String> fdnMap1 = new HashMap();
    private HashMap<String, String> fdnMap2 = new HashMap();
    private boolean isPSAllowedByFdn1 = false;
    private boolean isPSAllowedByFdn2 = false;

    public void addURI(UriMatcher uriMatcher) {
        uriMatcher.addURI("icc", "fdn/exits_query", FDN_EXISTS);
        uriMatcher.addURI("icc", "fdn/exits_query/subId/#", FDN_EXISTS_SUB);
    }

    /* JADX WARNING: Removed duplicated region for block: B:31:0x0062 A:{RETURN} */
    /* JADX WARNING: Removed duplicated region for block: B:45:0x00bf  */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x0063  */
    /* JADX WARNING: Missing block: B:17:0x002d, code skipped:
            if (r10.fdnMap2.isEmpty() != false) goto L_0x002f;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public Cursor handleCustQuery(UriMatcher uriMatcher, Uri url, String[] selectionArgs, String[] addressColumns) {
        int subId;
        int urlMatchVal = uriMatcher.match(url);
        Cursor cursor = null;
        switch (urlMatchVal) {
            case FDN_EXISTS /*101*/:
                subId = SubscriptionManager.getDefaultSubId();
                break;
            case FDN_EXISTS_SUB /*102*/:
                subId = getRequestSubId(url);
                break;
            default:
                return null;
        }
        List<AdnRecord> adnRecords = null;
        if (subId == 0) {
            try {
                if (!this.fdnMap1.isEmpty()) {
                }
                IIccPhoneBook iccIpb = Stub.asInterface(ServiceManager.getService("simphonebook"));
                if (iccIpb == null) {
                    return null;
                }
                adnRecords = iccIpb.getAdnRecordsInEfForSubscriber(subId, 28475);
                if (adnRecords == null) {
                    return null;
                }
                fdnCacheProcess(adnRecords, 28475, (long) subId);
                switch (urlMatchVal) {
                    case FDN_EXISTS /*101*/:
                        log("fddn FDN_EXISTS number: xxxx");
                        if (this.fdnMap1.get(selectionArgs[0]) != null) {
                            cursor = new MatrixCursor(addressColumns, 1);
                        }
                        return cursor;
                    case FDN_EXISTS_SUB /*102*/:
                        Cursor cursor2 = null;
                        String exists = null;
                        if (subId == 0) {
                            exists = (String) this.fdnMap1.get(selectionArgs[0]);
                            if (this.fdnMap1.get(selectionArgs[0]) != null) {
                                cursor = new MatrixCursor(addressColumns, 1);
                            }
                            cursor2 = cursor;
                        } else if (subId == 1) {
                            exists = (String) this.fdnMap2.get(selectionArgs[0]);
                            if (this.fdnMap2.get(selectionArgs[0]) != null) {
                                cursor = new MatrixCursor(addressColumns, 1);
                            }
                            cursor2 = cursor;
                        }
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("fddn FDN_EXISTS_SUB subId:");
                        stringBuilder.append(subId);
                        stringBuilder.append(" ,number: xxxx ,is exists:");
                        stringBuilder.append(exists);
                        log(stringBuilder.toString());
                        return cursor2;
                    default:
                        return null;
                }
            } catch (RemoteException ex) {
                log(ex.toString());
                return null;
            } catch (SecurityException ex2) {
                log(ex2.toString());
                return null;
            }
        }
        if (subId == 1) {
        }
        switch (urlMatchVal) {
            case FDN_EXISTS /*101*/:
                break;
            case FDN_EXISTS_SUB /*102*/:
                break;
            default:
                break;
        }
    }

    public void fdnCacheProcess(List<AdnRecord> adnRecords, int efType, long subId) {
        if (adnRecords != null) {
            int N = adnRecords.size();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("fddn loadFromEf FDN_PRELOAD_CACHE:");
            stringBuilder.append(this.FDN_PRELOAD_CACHE);
            stringBuilder.append(" ,subId");
            stringBuilder.append(subId);
            stringBuilder.append(" ,efType:");
            stringBuilder.append(efType);
            log(stringBuilder.toString());
            fdnCacheReset(efType, subId);
            for (int i = 0; i < N; i++) {
                fdnCacheLoad(efType, ((AdnRecord) adnRecords.get(i)).getNumber(), subId);
            }
            fdnCacheLoaded(efType, subId);
        }
    }

    private void fdnCacheReset(int efType, long subId) {
        if (this.FDN_PRELOAD_CACHE && 28475 == efType) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("fddn fdnCacheReset subId:");
            stringBuilder.append(subId);
            log(stringBuilder.toString());
            if (subId == 0) {
                this.isPSAllowedByFdn1 = false;
                this.fdnMap1.clear();
            } else if (subId == 1) {
                this.isPSAllowedByFdn2 = false;
                this.fdnMap2.clear();
            }
        }
    }

    private void fdnCacheLoad(int efType, String number, long subId) {
        if (this.FDN_PRELOAD_CACHE && 28475 == efType) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("fddn fdnCacheLoad number: xxxx ,subId:");
            stringBuilder.append(subId);
            log(stringBuilder.toString());
            if (subId == 0) {
                this.fdnMap1.put(number, FDN_NUM_VALUE);
                if (DATA_FIXED_NUMBER.equals(number)) {
                    this.isPSAllowedByFdn1 = HWDBG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("fddn fdnCacheLoad data fixed number found in card ");
                    stringBuilder.append(subId);
                    log(stringBuilder.toString());
                }
            } else if (subId == 1) {
                this.fdnMap2.put(number, FDN_NUM_VALUE);
                if (DATA_FIXED_NUMBER.equals(number)) {
                    this.isPSAllowedByFdn2 = HWDBG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("fddn fdnCacheLoad data fixed number found in card ");
                    stringBuilder.append(subId);
                    log(stringBuilder.toString());
                }
            }
        }
    }

    private void fdnCacheLoaded(int efType, long subId) {
        if (this.FDN_PRELOAD_CACHE && 28475 == efType) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("fddn fdnCacheLoaded subId:");
            stringBuilder.append(subId);
            stringBuilder.append(" ,isPSAllowedByFdn1:");
            stringBuilder.append(this.isPSAllowedByFdn1);
            stringBuilder.append(" ,isPSAllowedByFdn2:");
            stringBuilder.append(this.isPSAllowedByFdn2);
            log(stringBuilder.toString());
            if (subId == 0 && this.isPSAllowedByFdn1) {
                SystemProperties.set(HwCustTelephonyProperties.PROPERTY_FDN_PS_FLAG_EXISTS_SUB1, "true");
            } else if (subId == 0 && !this.isPSAllowedByFdn1) {
                SystemProperties.set(HwCustTelephonyProperties.PROPERTY_FDN_PS_FLAG_EXISTS_SUB1, "false");
            } else if (subId == 1 && this.isPSAllowedByFdn2) {
                SystemProperties.set(HwCustTelephonyProperties.PROPERTY_FDN_PS_FLAG_EXISTS_SUB2, "true");
            } else if (subId == 1 && !this.isPSAllowedByFdn2) {
                SystemProperties.set(HwCustTelephonyProperties.PROPERTY_FDN_PS_FLAG_EXISTS_SUB2, "false");
            }
        }
    }

    private int getRequestSubId(Uri url) {
        try {
            return Integer.parseInt(url.getLastPathSegment());
        } catch (NumberFormatException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown URL ");
            stringBuilder.append(url);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private void log(String message) {
        Rlog.d(TAG, message);
    }
}
