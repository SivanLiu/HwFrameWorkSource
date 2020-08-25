package com.android.server.location;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.location.Location;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.telephony.CellLocation;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Xml;
import com.android.server.intellicom.common.SmartDualCardConsts;
import com.android.server.location.HwCryptoUtility;
import com.android.server.rms.iaware.feature.DevSchedFeatureRT;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class HwLocalLocationManager implements IHwLocalLocationManager {
    private static final int AP_INVALID = 1;
    private static final int AP_VALID = 0;
    private static final String[] BSSID_COLUMNS = {COLUMN_LATITUDE, COLUMN_LONGITUDE, COLUMN_ACCURACY, COLUMN_AP_VALID, COLUMN_LOC_SOURC};
    private static final String BSSID_SELECTION = "id_hash=?";
    public static final String BSSID_TABLE = "bssID";
    private static final byte[] C2 = {-89, 82, 3, 85, -88, -104, 57, -10, -103, 108, -88, 122, -38, -12, -55, -2};
    private static final String[] CELLID_COLUMNS = {COLUMN_LATITUDE, COLUMN_LONGITUDE, COLUMN_ACCURACY, COLUMN_LOC_SOURC};
    public static final String CELLID_TABLE = "cellID";
    private static final String CELL_RECENT_QUERY_SELECTION = "SELECT * FROM cell_fix_info ORDER BY time DESC LIMIT 1";
    private static final String CELL_SELECTION = "id_hash=? and areacode=?";
    private static final String COLUMN_ACCURACY = "accuracy";
    private static final String COLUMN_AP_VALID = "ap_valid";
    private static final String COLUMN_AREACODE = "areacode";
    private static final String COLUMN_BSSID_INFO = "wifi_bssid";
    private static final String COLUMN_CELL_INFO = "cellid";
    private static final String COLUMN_ID_HASH = "id_hash";
    private static final String COLUMN_LATITUDE = "latitude";
    private static final String COLUMN_LOC_SOURC = "loc_source";
    private static final String COLUMN_LONGITUDE = "longitude";
    private static final String COLUMN_PRESET = "perset";
    private static final String COLUMN_TIME = "time";
    public static final String CREATE_BSSID_TABLE = "CREATE TABLE IF NOT EXISTS  bssid_fix_info(wifi_bssid VARCHAR(128) , id_hash VARCHAR(128) PRIMARY KEY,ap_valid INT DEFAULT (0) , latitude VARCHAR(128), longitude VARCHAR(128), accuracy FLOAT, loc_source VARCHAR(32) ,perset INT DEFAULT (0) , time INT (64) )";
    public static final String CREATE_CELLID_TABLE = "CREATE TABLE IF NOT EXISTS  cell_fix_info(cellid VARCHAR(128) , id_hash VARCHAR(128) PRIMARY KEY,areacode INT (64) , latitude VARCHAR(128), longitude VARCHAR(128), accuracy FLOAT, loc_source VARCHAR(32) ,perset INT DEFAULT (0) , time INT (64) )";
    private static final int DATA_NO_PRESET = 0;
    private static final int DATA_PRESET = 1;
    public static final boolean DUG = true;
    private static final String EXTRA_KEY_LOC_SOURCE = "key_loc_source";
    private static final String EXTRA_KEY_LOC_TABLEID = "key_loc_tableID";
    private static final int FIX_TIME_OUT = 2000;
    private static final String INVAILD_BSSID = "00:00:00:00:00:00";
    private static final int INVAILD_CELLID = -1;
    public static final String LOCAL_PROVIDER = "local_database";
    public static final String LOCATION_DB_NAME = "local_location.db";
    public static final String MASTER_PASSWORD = getKey(HwLocalLocationProvider.C1, C2, HwLocalLocationDBHelper.C3);
    private static final int MAX_COLUMN = 10000;
    private static final int MAX_INTERVALTIME = 600000;
    public static final int MSG_REPORT_LOCATION = 1;
    private static final String PERMISSION_INJECT_LOCATION = "com.huawei.android.permission.INJECT_LOCATION";
    public static final String TABLE_BSSID_NAME = "bssid_fix_info";
    public static final String TABLE_CELLID_NAME = "cell_fix_info";
    public static final String TAG = "HwLocalLocationProvider";
    private static final int TWO_MINUTES = 120000;
    public static final int VERSION = 2;
    private static final String XML_PATH = "app_name";
    private static final String XML_ROOT_NAME = "time_priority";
    private static final String XML_TAG_NAME = "app_name";
    private BroadcastReceiver bootCompleteReceiver = new BroadcastReceiver() {
        /* class com.android.server.location.HwLocalLocationManager.AnonymousClass1 */

        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
                HwLocalLocationManager.this.mTelephonyManager.listen(HwLocalLocationManager.this.mCellIdChangedListener, 16);
            }
        }
    };
    /* access modifiers changed from: private */
    public volatile boolean isLocating;
    private volatile boolean isRegister;
    /* access modifiers changed from: private */
    public CellIdChangedListener mCellIdChangedListener;
    /* access modifiers changed from: private */
    public Context mContext;
    /* access modifiers changed from: private */
    public volatile int mCurrentAreaCode;
    private Location mCurrentBestLocation;
    /* access modifiers changed from: private */
    public volatile String mCurrentBssId;
    /* access modifiers changed from: private */
    public volatile int mCurrentCellId;
    /* access modifiers changed from: private */
    public volatile Handler mHandler;
    /* access modifiers changed from: private */
    public Intent mInjectIntent;
    private IntentFilter mIntentFilter;
    private int mLastAreaCode;
    private String mLastBssId;
    private int mLastCellid;
    private HwLocalLocationDBHelper mLocalLocationDB;
    /* access modifiers changed from: private */
    public final Object mLock = new Object();
    /* access modifiers changed from: private */
    public volatile Message mMessage;
    /* access modifiers changed from: private */
    public volatile LocalFixTask mQueryLocationTask;
    /* access modifiers changed from: private */
    public Timer mQueryLocationTimer;
    /* access modifiers changed from: private */
    public TelephonyManager mTelephonyManager;
    private List<String> mTimePriorityAPPNames;
    /* access modifiers changed from: private */
    public WifiManager mWifiManager;
    private BroadcastReceiver wifiBroadcastReceiver = new BroadcastReceiver() {
        /* class com.android.server.location.HwLocalLocationManager.AnonymousClass2 */

        public void onReceive(Context context, Intent intent) {
            if (SmartDualCardConsts.SYSTEM_STATE_NAME_WIFI_NETWORK_STATE_CHANGED.equals(intent.getAction())) {
                NetworkInfo netinfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                if (netinfo != null && netinfo.isConnected()) {
                    String unused = HwLocalLocationManager.this.mCurrentBssId = intent.getStringExtra("bssid");
                } else if (!HwLocalLocationManager.this.mWifiManager.isWifiEnabled()) {
                    String unused2 = HwLocalLocationManager.this.mCurrentBssId = null;
                } else {
                    List<ScanResult> scanResults = HwLocalLocationManager.this.mWifiManager.getScanResults();
                    if (scanResults == null || scanResults.isEmpty() || scanResults.get(0) == null) {
                        String unused3 = HwLocalLocationManager.this.mCurrentBssId = null;
                    } else {
                        String unused4 = HwLocalLocationManager.this.mCurrentBssId = scanResults.get(0).BSSID;
                    }
                }
            }
        }
    };

    public HwLocalLocationManager(Context context) {
        this.mContext = context;
        initialize();
    }

    private synchronized void initialize() {
        this.mCurrentCellId = -1;
        this.mCurrentAreaCode = -1;
        this.mCurrentBssId = null;
        this.mLastBssId = null;
        this.mLastCellid = -1;
        this.mLastAreaCode = -1;
        this.mHandler = new LocalLocationHandler();
        this.mTimePriorityAPPNames = new ArrayList();
        this.mLocalLocationDB = new HwLocalLocationDBHelper(this.mContext);
        initTimePriorityAPPNames();
        insertPersetData();
        this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService("phone");
        this.mCellIdChangedListener = new CellIdChangedListener();
        this.mWifiManager = (WifiManager) this.mContext.getSystemService(DevSchedFeatureRT.WIFI_FEATURE);
        this.mIntentFilter = new IntentFilter(SmartDualCardConsts.SYSTEM_STATE_NAME_WIFI_NETWORK_STATE_CHANGED);
        this.mIntentFilter.addCategory("android.net.wifi.STATE_CHANGE@hwBrExpand@WifiNetStatus=WIFICON|WifiNetStatus=WIFIDSCON");
        registerListen();
    }

    public synchronized void registerListen() {
        LBSLog.i(TAG, false, "registerListen, isRegister=%{public}b", Boolean.valueOf(this.isRegister));
        if (!this.isRegister) {
            this.mTelephonyManager.listen(this.mCellIdChangedListener, 16);
            this.mContext.registerReceiver(this.bootCompleteReceiver, new IntentFilter("android.intent.action.BOOT_COMPLETED"));
            this.mContext.registerReceiver(this.wifiBroadcastReceiver, this.mIntentFilter);
            this.isRegister = true;
        }
    }

    public synchronized void unregisterListen() {
        LBSLog.i(TAG, false, "unregisterListen, isRegister=%{public}b", Boolean.valueOf(this.isRegister));
        if (this.isRegister) {
            this.mTelephonyManager.listen(this.mCellIdChangedListener, 0);
            this.mContext.unregisterReceiver(this.bootCompleteReceiver);
            this.mContext.unregisterReceiver(this.wifiBroadcastReceiver);
            this.isRegister = false;
        }
    }

    public synchronized void requestLocation() {
        LBSLog.i(TAG, false, "HwLocalLocationManager requestLocation", new Object[0]);
        if (this.isLocating) {
            LBSLog.i(TAG, false, "isLocating ,return", new Object[0]);
        } else if (-1 == getCellID()) {
            LBSLog.i(TAG, false, "cellid is null,return", new Object[0]);
        } else {
            this.isLocating = true;
            this.mQueryLocationTimer = new Timer();
            this.mQueryLocationTimer.schedule(new LocationTimerTask(), 2000);
            this.mQueryLocationTask = new LocalFixTask();
            this.mQueryLocationTask.execute(new Void[0]);
        }
    }

    public void closedb() {
        this.mLocalLocationDB.closedb();
    }

    public void updataLocationDB(Location loc) {
        if (loc == null || !loc.isComplete()) {
            LBSLog.w(TAG, false, "loc is null or not complete, can not updata to DB", new Object[0]);
        } else if (isValidLocation(loc) && !LOCAL_PROVIDER.equals(loc.getProvider())) {
            if (isBetterLocation(loc, this.mCurrentBestLocation)) {
                LBSLog.i(TAG, false, "this loc is Better than Last Location", new Object[0]);
                Location location = this.mCurrentBestLocation;
                if (location == null) {
                    this.mCurrentBestLocation = new Location(loc);
                } else {
                    location.set(loc);
                }
                this.mCurrentCellId = getCellID();
                if (-1 != this.mCurrentCellId) {
                    new RefreshCellInfoDBTask(loc, this.mCurrentCellId).execute(new Void[0]);
                }
                this.mCurrentBssId = getBSSID();
                String tempBssId = this.mCurrentBssId;
                if (tempBssId != null && !tempBssId.isEmpty()) {
                    new RefreshBssIDDBTask(loc, 0, tempBssId).execute(new Void[0]);
                    return;
                }
                return;
            }
            if (isCellInfoChange()) {
                LBSLog.i(TAG, false, "cellid has changed,RefreshCellInfoDB", new Object[0]);
                new RefreshCellInfoDBTask(loc, this.mCurrentCellId).execute(new Void[0]);
            }
            if (isBssIdChange()) {
                LBSLog.i(TAG, false, "bssid has changed,RefreshBssIDDB", new Object[0]);
                new RefreshBssIDDBTask(loc, 0, this.mCurrentBssId).execute(new Void[0]);
            }
        }
    }

    private synchronized boolean isCellInfoChange() {
        getCellID();
        if (this.mCurrentAreaCode != -1) {
            if (this.mLastAreaCode != -1) {
                if (this.mLastCellid != -1 && this.mLastAreaCode != -1 && this.mLastCellid == this.mCurrentCellId && this.mLastAreaCode == this.mCurrentAreaCode) {
                    return false;
                }
                this.mLastAreaCode = this.mCurrentAreaCode;
                this.mLastCellid = this.mCurrentCellId;
                return true;
            }
        }
        return false;
    }

    private boolean isBssIdChange() {
        this.mCurrentBssId = getBSSID();
        String tempBssId = this.mCurrentBssId;
        if (tempBssId == null || tempBssId.isEmpty()) {
            return false;
        }
        String str = this.mLastBssId;
        if (str != null && str.equals(tempBssId)) {
            return false;
        }
        this.mLastBssId = tempBssId;
        return true;
    }

    public boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            return true;
        }
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > 120000;
        boolean isSignificantlyOlder = timeDelta < -120000;
        boolean isNewer = timeDelta > 0;
        if (isSignificantlyNewer) {
            return true;
        }
        if (isSignificantlyOlder) {
            return false;
        }
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 100;
        boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());
        if (isMoreAccurate) {
            return true;
        }
        if (isNewer && !isLessAccurate) {
            return true;
        }
        if (!isNewer || isSignificantlyLessAccurate || !isFromSameProvider) {
            return false;
        }
        return true;
    }

    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    private int getCellID() {
        return this.mCurrentCellId;
    }

    private synchronized String getBSSID() {
        if (!this.mWifiManager.isWifiEnabled()) {
            this.mCurrentBssId = null;
        } else if (this.mCurrentBssId == null) {
            WifiInfo info = this.mWifiManager.getConnectionInfo();
            if (info != null) {
                this.mCurrentBssId = info.getBSSID();
            } else {
                List<ScanResult> scanResults = this.mWifiManager.getScanResults();
                if (scanResults == null || scanResults.isEmpty() || scanResults.get(0) == null) {
                    this.mCurrentBssId = null;
                } else {
                    this.mCurrentBssId = scanResults.get(0).BSSID;
                }
            }
        }
        return this.mCurrentBssId;
    }

    public boolean isValidLocation(Location loc) {
        return true;
    }

    private int getindex(String[] columns, String column) {
        if (columns == null || columns.length <= 0) {
            return -1;
        }
        for (int i = columns.length - 1; i >= 0; i--) {
            if (columns[i].equals(column)) {
                return i;
            }
        }
        return -1;
    }

    /* access modifiers changed from: private */
    public void refreshLocToCellTable(Location loc, int cellid) {
        if (-1 != cellid) {
            if (isCellIdExist(cellid)) {
                LBSLog.i(TAG, false, "cellid is Exist, update  cellinfo to DB", new Object[0]);
                updatetLocToCellIdTable(loc, cellid);
                return;
            }
            LBSLog.i(TAG, false, "cellid is not Exist, insert cellinfo to DB", new Object[0]);
            if (10000 < getDBNumber(TABLE_CELLID_NAME)) {
                LBSLog.e(TAG, false, "DB Number > %{public}d , first delete Top data", 10000);
                deleteTopData(TABLE_CELLID_NAME);
            }
            insertLocToDB(TABLE_CELLID_NAME, loc, cellid + "", false);
        }
    }

    /* access modifiers changed from: private */
    public void refreshLocToBssTable(Location loc, int apValid, String id) {
        if (id != null && !id.isEmpty() && !id.equals(INVAILD_BSSID)) {
            if (apValid == 1) {
                updatetLocToBssIdTable(loc, id, apValid);
            }
            if (isBssidExist(id)) {
                updatetLocToBssIdTable(loc, id, 0);
                return;
            }
            if (10000 < getDBNumber(TABLE_BSSID_NAME)) {
                deleteTopData(TABLE_BSSID_NAME);
            }
            insertLocToDB(TABLE_BSSID_NAME, loc, id, false);
        }
    }

    /* access modifiers changed from: private */
    public Location queryLocFormDB() {
        LBSLog.i(TAG, false, "start query Loc Form DB ", new Object[0]);
        Location locCellID = queryLocFormCellIdTable(getCellID());
        if (locCellID != null) {
            LBSLog.i(TAG, false, "query Loc Form cellid", new Object[0]);
            Location locBssID = queryLocFormBssIdTable(getBSSID());
            if (locBssID == null) {
                LBSLog.w(TAG, false, "query Loc Form bssid , locBssID = null.  return locCellID", new Object[0]);
                return locCellID;
            } else if (locCellID.distanceTo(locBssID) < 5000.0f) {
                LBSLog.w(TAG, false, "location disrance < 5000 M == %{public}d m", Float.valueOf(locCellID.distanceTo(locBssID)));
                return locBssID;
            } else {
                LBSLog.w(TAG, false, "location disrance >5000 M == %{public}d m", Float.valueOf(locCellID.distanceTo(locBssID)));
                new RefreshBssIDDBTask(locBssID, 1, this.mCurrentBssId).execute(new Void[0]);
                return locCellID;
            }
        } else {
            LBSLog.w(TAG, false, "queryLoc cellid is  null", new Object[0]);
            return null;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:11:0x0047, code lost:
        if (r1 != null) goto L_0x0049;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0049, code lost:
        r1.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:18:0x0058, code lost:
        if (0 == 0) goto L_0x005b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x005b, code lost:
        return 0;
     */
    private int getDBNumber(String tablename) {
        Cursor cursor = null;
        try {
            HwLocalLocationDBHelper hwLocalLocationDBHelper = this.mLocalLocationDB;
            cursor = hwLocalLocationDBHelper.rawQuery("SELECT COUNT (*) FROM " + tablename, null);
            if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                int count = cursor.getInt(0) + 1;
                LBSLog.i(TAG, false, "%{public}s count is %{public}d", tablename, Integer.valueOf(count));
                cursor.close();
                return count;
            }
        } catch (Exception e) {
            LBSLog.e(TAG, false, "query Exception", new Object[0]);
        } catch (Throwable th) {
            if (0 != 0) {
                cursor.close();
            }
            throw th;
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0054, code lost:
        if (r3 != null) goto L_0x0056;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0056, code lost:
        r3.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x0067, code lost:
        if (0 == 0) goto L_0x006a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x006a, code lost:
        return false;
     */
    private boolean isCellIdExist(int id) {
        if (id == -1 || this.mCurrentAreaCode == -1) {
            return false;
        }
        Cursor cursor = null;
        try {
            cursor = this.mLocalLocationDB.query(TABLE_CELLID_NAME, CELLID_COLUMNS, CELL_SELECTION, new String[]{HwCryptoUtility.Sha256Encrypt.encrypt(id + "", ""), this.mCurrentAreaCode + ""});
            if (cursor != null && cursor.getCount() > 0) {
                cursor.close();
                return true;
            }
        } catch (Exception e) {
            LBSLog.e(TAG, false, "query db error in isCellIdExist", new Object[0]);
        } catch (Throwable th) {
            if (0 != 0) {
                cursor.close();
            }
            throw th;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:39:? A[RETURN, SYNTHETIC] */
    public Location queryLocFormRecentCell() {
        String str;
        LBSLog.i(TAG, false, "query loc Form Recent Cell", new Object[0]);
        Cursor cursor = null;
        try {
            cursor = this.mLocalLocationDB.rawQuery(CELL_RECENT_QUERY_SELECTION, null);
            if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst()) {
                String[] columnNames = cursor.getColumnNames();
                long intervalTime = System.currentTimeMillis() - cursor.getLong(getindex(columnNames, COLUMN_TIME));
                if (intervalTime > 0 && intervalTime < 600000) {
                    Location loc = new Location(LOCAL_PROVIDER);
                    String encryptedLat = cursor.getString(getindex(BSSID_COLUMNS, COLUMN_LATITUDE));
                    String encryptedLong = cursor.getString(getindex(BSSID_COLUMNS, COLUMN_LONGITUDE));
                    String decryptedLat = HwCryptoUtility.AESLocalDbCrypto.decrypt(MASTER_PASSWORD, encryptedLat);
                    String decryptedLong = HwCryptoUtility.AESLocalDbCrypto.decrypt(MASTER_PASSWORD, encryptedLong);
                    double lat = Double.parseDouble(decryptedLat);
                    double longi = Double.parseDouble(decryptedLong);
                    loc.setLatitude(lat);
                    try {
                        loc.setLongitude(longi);
                        loc.setAccuracy(cursor.getFloat(getindex(columnNames, COLUMN_ACCURACY)));
                        loc.setTime(System.currentTimeMillis());
                        loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                        Bundle b = new Bundle();
                        b.putString("key_loc_source", cursor.getString(getindex(columnNames, COLUMN_LOC_SOURC)));
                        b.putString("key_loc_tableID", CELLID_TABLE);
                        loc.setExtras(b);
                        Object[] objArr = {loc.toString()};
                        str = TAG;
                        try {
                            LBSLog.i(str, false, "queryLocFormRecentCell loc  == %{public}s", objArr);
                            cursor.close();
                            return loc;
                        } catch (Exception e) {
                            try {
                                LBSLog.e(str, false, "query Exception", new Object[0]);
                                return null;
                            } finally {
                                if (cursor != null) {
                                    cursor.close();
                                }
                            }
                        }
                    } catch (Exception e2) {
                        str = TAG;
                        LBSLog.e(str, false, "query Exception", new Object[0]);
                        if (cursor == null) {
                            return null;
                        }
                        return null;
                    }
                }
            }
            if (cursor == null) {
                return null;
            }
        } catch (Exception e3) {
            str = TAG;
            LBSLog.e(str, false, "query Exception", new Object[0]);
            return null;
        }
        return null;
    }

    /* JADX WARNING: Code restructure failed: missing block: B:35:0x010e, code lost:
        r2 = com.android.server.location.HwLocalLocationManager.TAG;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:43:0x0128, code lost:
        r0 = th;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:53:0x013e, code lost:
        r8.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:56:?, code lost:
        return null;
     */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Removed duplicated region for block: B:43:0x0128 A[ExcHandler: all (th java.lang.Throwable), PHI: r8 
      PHI: (r8v4 'cursor' android.database.Cursor) = (r8v1 'cursor' android.database.Cursor), (r8v6 'cursor' android.database.Cursor), (r8v6 'cursor' android.database.Cursor) binds: [B:11:0x0053, B:20:0x00ac, B:21:?] A[DONT_GENERATE, DONT_INLINE], Splitter:B:11:0x0053] */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x013e  */
    /* JADX WARNING: Removed duplicated region for block: B:56:? A[RETURN, SYNTHETIC] */
    public Location queryLocFormCellIdTable(int id) {
        String str;
        LBSLog.i(TAG, false, "query loc Form Cell Table", new Object[0]);
        if (-1 == id) {
            return queryLocFormRecentCell();
        }
        String cellidStrSHA256 = null;
        try {
            cellidStrSHA256 = HwCryptoUtility.Sha256Encrypt.encrypt(id + "", "");
        } catch (Exception e) {
            LBSLog.e(TAG, false, "query Exception", new Object[0]);
        }
        Cursor cursor = null;
        try {
            cursor = this.mLocalLocationDB.query(TABLE_CELLID_NAME, CELLID_COLUMNS, CELL_SELECTION, new String[]{cellidStrSHA256, this.mCurrentAreaCode + ""});
            if (cursor == null || cursor.getCount() <= 0) {
                Location queryLocFormRecentCell = queryLocFormRecentCell();
                if (cursor != null) {
                    cursor.close();
                }
                return queryLocFormRecentCell;
            } else if (cursor.moveToFirst()) {
                Location loc = new Location(LOCAL_PROVIDER);
                String encryptedLat = cursor.getString(getindex(BSSID_COLUMNS, COLUMN_LATITUDE));
                String encryptedLong = cursor.getString(getindex(BSSID_COLUMNS, COLUMN_LONGITUDE));
                String decryptedLat = HwCryptoUtility.AESLocalDbCrypto.decrypt(MASTER_PASSWORD, encryptedLat);
                String decryptedLong = HwCryptoUtility.AESLocalDbCrypto.decrypt(MASTER_PASSWORD, encryptedLong);
                double lat = Double.parseDouble(decryptedLat);
                double longi = Double.parseDouble(decryptedLong);
                loc.setLatitude(lat);
                loc.setLongitude(longi);
                loc.setAccuracy(cursor.getFloat(getindex(CELLID_COLUMNS, COLUMN_ACCURACY)));
                try {
                    loc.setTime(System.currentTimeMillis());
                    loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                    Bundle b = new Bundle();
                    b.putString("key_loc_source", cursor.getString(getindex(CELLID_COLUMNS, COLUMN_LOC_SOURC)));
                    b.putString("key_loc_tableID", CELLID_TABLE);
                    loc.setExtras(b);
                    Object[] objArr = new Object[0];
                    str = TAG;
                    try {
                        LBSLog.i(str, false, "queryLocFormCellIdTable success", objArr);
                        cursor.close();
                        return loc;
                    } catch (Exception e2) {
                        try {
                            LBSLog.e(str, false, "query Exception", new Object[0]);
                            if (cursor == null) {
                            }
                            cursor.close();
                            return null;
                        } catch (Throwable th) {
                            th = th;
                            if (cursor != null) {
                            }
                            throw th;
                        }
                    }
                } catch (Exception e3) {
                    str = TAG;
                    LBSLog.e(str, false, "query Exception", new Object[0]);
                    if (cursor == null) {
                    }
                    cursor.close();
                    return null;
                } catch (Throwable th2) {
                    th = th2;
                    if (cursor != null) {
                    }
                    throw th;
                }
            } else {
                cursor.close();
                return null;
            }
        } catch (Exception e4) {
            str = TAG;
            LBSLog.e(str, false, "query Exception", new Object[0]);
            if (cursor == null) {
            }
            cursor.close();
            return null;
        } catch (Throwable th3) {
        }
    }

    private void insertLocToDB(String table, Location loc, String id, boolean isperset) {
        if (loc == null || !loc.isComplete()) {
            LBSLog.w(TAG, false, "loc is null or not complete, can not insert to DB", new Object[0]);
            return;
        }
        ContentValues values = new ContentValues();
        try {
            values.put(COLUMN_ID_HASH, HwCryptoUtility.Sha256Encrypt.encrypt(id, ""));
            String str = MASTER_PASSWORD;
            String encryptedLong = HwCryptoUtility.AESLocalDbCrypto.encrypt(str, loc.getLongitude() + "");
            String str2 = MASTER_PASSWORD;
            String encryptedLat = HwCryptoUtility.AESLocalDbCrypto.encrypt(str2, loc.getLatitude() + "");
            if (TABLE_CELLID_NAME.equals(table)) {
                values.put(COLUMN_CELL_INFO, "");
                values.put(COLUMN_AREACODE, Integer.valueOf(this.mCurrentAreaCode));
            } else if (TABLE_BSSID_NAME.equals(table)) {
                values.put(COLUMN_BSSID_INFO, "");
                values.put(COLUMN_AP_VALID, (Integer) 0);
            } else {
                return;
            }
            if (isperset) {
                values.put(COLUMN_PRESET, (Integer) 1);
            }
            values.put(COLUMN_ACCURACY, Float.valueOf(loc.getAccuracy()));
            values.put(COLUMN_LATITUDE, encryptedLat);
            values.put(COLUMN_LONGITUDE, encryptedLong);
            values.put(COLUMN_TIME, Long.valueOf(System.currentTimeMillis()));
            values.put(COLUMN_LOC_SOURC, loc.getProvider());
            this.mLocalLocationDB.insert(table, values);
            LBSLog.i(TAG, false, "insertLocToDB success", new Object[0]);
        } catch (Exception e) {
            LBSLog.e(TAG, false, "query Exception", new Object[0]);
        }
    }

    private void deleteTopData(String table) {
        StringBuffer sql = new StringBuffer();
        sql.append("DELETE FROM ");
        sql.append(table);
        sql.append(" WHERE ");
        sql.append(COLUMN_TIME);
        sql.append(" = (SELECT ");
        sql.append(COLUMN_TIME);
        sql.append(" FROM ");
        sql.append(table);
        sql.append(" WHERE ");
        sql.append(COLUMN_PRESET);
        sql.append(" = ");
        sql.append(0);
        sql.append(" ORDER BY ");
        sql.append(COLUMN_TIME);
        sql.append(" ASC LIMIT 1 ) ");
        this.mLocalLocationDB.execSQL(sql.toString());
    }

    private void updatetLocToCellIdTable(Location loc, int id) {
        LBSLog.i(TAG, false, "update loc to cell database", new Object[0]);
        try {
            String str = MASTER_PASSWORD;
            String encryptedLong = HwCryptoUtility.AESLocalDbCrypto.encrypt(str, loc.getLongitude() + "");
            String str2 = MASTER_PASSWORD;
            String encryptedLat = HwCryptoUtility.AESLocalDbCrypto.encrypt(str2, loc.getLatitude() + "");
            String idStrSHA256 = HwCryptoUtility.Sha256Encrypt.encrypt(id + "", "");
            ContentValues values = new ContentValues(3);
            values.put(COLUMN_ACCURACY, Float.valueOf(loc.getAccuracy()));
            values.put(COLUMN_LATITUDE, encryptedLat);
            values.put(COLUMN_LONGITUDE, encryptedLong);
            values.put(COLUMN_LOC_SOURC, loc.getProvider());
            values.put(COLUMN_TIME, Long.valueOf(System.currentTimeMillis()));
            this.mLocalLocationDB.update(TABLE_CELLID_NAME, values, CELL_SELECTION, new String[]{idStrSHA256 + "", this.mCurrentAreaCode + ""});
        } catch (Exception e) {
            LBSLog.e(TAG, false, "query Exception", new Object[0]);
        }
    }

    private void updatetLocToBssIdTable(Location loc, String id, int apValid) {
        LBSLog.i(TAG, false, "update loc to bssid database", new Object[0]);
        try {
            String str = MASTER_PASSWORD;
            String encryptedLong = HwCryptoUtility.AESLocalDbCrypto.encrypt(str, loc.getLongitude() + "");
            String str2 = MASTER_PASSWORD;
            String encryptedLat = HwCryptoUtility.AESLocalDbCrypto.encrypt(str2, loc.getLatitude() + "");
            String idStrSHA256 = HwCryptoUtility.Sha256Encrypt.encrypt(id, "");
            ContentValues values = new ContentValues();
            if (apValid == 1) {
                values.put(COLUMN_AP_VALID, Integer.valueOf(apValid));
            } else {
                values.put(COLUMN_ACCURACY, Float.valueOf(loc.getAccuracy()));
                values.put(COLUMN_LATITUDE, encryptedLat);
                values.put(COLUMN_LONGITUDE, encryptedLong);
            }
            values.put(COLUMN_LOC_SOURC, loc.getProvider());
            values.put(COLUMN_TIME, Long.valueOf(System.currentTimeMillis()));
            this.mLocalLocationDB.update(TABLE_BSSID_NAME, values, BSSID_SELECTION, new String[]{idStrSHA256});
        } catch (Exception e) {
            LBSLog.e(TAG, false, "query Exception", new Object[0]);
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:12:0x0033, code lost:
        if (r3 != null) goto L_0x0035;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:13:0x0035, code lost:
        r3.close();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x0044, code lost:
        if (0 == 0) goto L_0x0047;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x0047, code lost:
        return false;
     */
    private boolean isBssidExist(String bssid) {
        if (bssid == null || bssid.isEmpty()) {
            LBSLog.w(TAG, false, "bssid is null,isBssidExist is false", new Object[0]);
            return false;
        }
        String bssidStrSHA256 = HwCryptoUtility.Sha256Encrypt.encrypt(bssid, "");
        Cursor cursor = null;
        try {
            cursor = this.mLocalLocationDB.query(TABLE_BSSID_NAME, BSSID_COLUMNS, BSSID_SELECTION, new String[]{bssidStrSHA256});
            if (cursor != null && cursor.getCount() > 0) {
                cursor.close();
                return true;
            }
        } catch (Exception e) {
            LBSLog.e(TAG, false, "query db error in isBssidExist", new Object[0]);
        } catch (Throwable th) {
            if (0 != 0) {
                cursor.close();
            }
            throw th;
        }
    }

    private Location queryLocFormBssIdTable(String bssid) {
        if (bssid == null || bssid.isEmpty()) {
            LBSLog.w(TAG, false, "bssid is null,not query", new Object[0]);
            return null;
        }
        Cursor cursor = null;
        try {
            String bssidStrSHA256 = HwCryptoUtility.Sha256Encrypt.encrypt(bssid, "");
            cursor = this.mLocalLocationDB.query(TABLE_BSSID_NAME, BSSID_COLUMNS, BSSID_SELECTION, new String[]{bssidStrSHA256});
            if (cursor != null && cursor.getCount() > 0) {
                if (cursor.moveToFirst()) {
                    if (1.0f == cursor.getFloat(getindex(BSSID_COLUMNS, COLUMN_AP_VALID))) {
                        cursor.close();
                        return null;
                    }
                    Location loc = new Location(LOCAL_PROVIDER);
                    String encryptedLat = cursor.getString(getindex(BSSID_COLUMNS, COLUMN_LATITUDE));
                    String encryptedLong = cursor.getString(getindex(BSSID_COLUMNS, COLUMN_LONGITUDE));
                    String decryptedLat = HwCryptoUtility.AESLocalDbCrypto.decrypt(MASTER_PASSWORD, encryptedLat);
                    String decryptedLong = HwCryptoUtility.AESLocalDbCrypto.decrypt(MASTER_PASSWORD, encryptedLong);
                    double lat = Double.parseDouble(decryptedLat);
                    double longi = Double.parseDouble(decryptedLong);
                    loc.setLatitude(lat);
                    loc.setLongitude(longi);
                    loc.setAccuracy(cursor.getFloat(getindex(BSSID_COLUMNS, COLUMN_ACCURACY)));
                    loc.setTime(System.currentTimeMillis());
                    loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                    Bundle b = new Bundle();
                    b.putString("key_loc_source", cursor.getString(getindex(BSSID_COLUMNS, COLUMN_LOC_SOURC)));
                    b.putString("key_loc_tableID", BSSID_TABLE);
                    loc.setExtras(b);
                    LBSLog.i(TAG, false, "queryLocFormBssIdTable success", new Object[0]);
                    cursor.close();
                    return loc;
                }
            }
            if (cursor == null) {
                return null;
            }
        } catch (Exception e) {
            LBSLog.e(TAG, false, "query Exception", new Object[0]);
            if (0 == 0) {
                return null;
            }
        } catch (Throwable th) {
            if (0 != 0) {
                cursor.close();
            }
            throw th;
        }
        cursor.close();
        return null;
    }

    private final class LocalLocationHandler extends Handler {
        private LocalLocationHandler() {
        }

        public void handleMessage(Message msg) {
            Location loc;
            super.handleMessage(msg);
            if (msg.what == 1 && (loc = (Location) msg.obj) != null) {
                Intent unused = HwLocalLocationManager.this.mInjectIntent = new Intent("action_inject_location");
                HwLocalLocationManager.this.mInjectIntent.putExtra("key_location", loc);
                HwLocalLocationManager.this.mContext.sendBroadcast(HwLocalLocationManager.this.mInjectIntent, HwLocalLocationManager.PERMISSION_INJECT_LOCATION);
                LBSLog.i(HwLocalLocationManager.TAG, false, "sendBroadcast ACTION_INJECT_LOCATION", new Object[0]);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public class LocalFixTask extends AsyncTask<Void, Void, Location> {
        LocalFixTask() {
        }

        /* access modifiers changed from: protected */
        public Location doInBackground(Void... params) {
            return HwLocalLocationManager.this.queryLocFormDB();
        }

        /* access modifiers changed from: protected */
        public void onPostExecute(Location result) {
            super.onPostExecute((Object) result);
            synchronized (HwLocalLocationManager.this.mLock) {
                if (HwLocalLocationManager.this.mQueryLocationTimer != null) {
                    HwLocalLocationManager.this.mQueryLocationTimer.cancel();
                    Timer unused = HwLocalLocationManager.this.mQueryLocationTimer = null;
                }
                if (result != null) {
                    LBSLog.i(HwLocalLocationManager.TAG, false, "has query Loc Form DB and send msg to Hander", new Object[0]);
                    Message unused2 = HwLocalLocationManager.this.mMessage = HwLocalLocationManager.this.mHandler.obtainMessage();
                    HwLocalLocationManager.this.mMessage.what = 1;
                    HwLocalLocationManager.this.mMessage.obj = result;
                    HwLocalLocationManager.this.mHandler.sendMessage(HwLocalLocationManager.this.mMessage);
                }
                boolean unused3 = HwLocalLocationManager.this.isLocating = false;
            }
            cancel(false);
        }
    }

    class RefreshCellInfoDBTask extends AsyncTask<Void, Void, Location> {
        int cellId;
        Location loc;

        public RefreshCellInfoDBTask(Location loc2, int cellId2) {
            this.loc = loc2;
            this.cellId = cellId2;
        }

        /* access modifiers changed from: protected */
        public Location doInBackground(Void... params) {
            HwLocalLocationManager.this.refreshLocToCellTable(this.loc, this.cellId);
            return null;
        }

        /* access modifiers changed from: protected */
        public void onPostExecute(Location result) {
            super.onPostExecute((Object) result);
            cancel(false);
        }
    }

    class RefreshBssIDDBTask extends AsyncTask<Void, Void, Location> {
        int apValid;
        String bssId;
        Location loc;

        public RefreshBssIDDBTask(Location loc2, int apValid2, String bssId2) {
            this.loc = loc2;
            this.bssId = bssId2;
            this.apValid = apValid2;
        }

        /* access modifiers changed from: protected */
        public Location doInBackground(Void... params) {
            HwLocalLocationManager.this.refreshLocToBssTable(this.loc, this.apValid, this.bssId);
            return null;
        }

        /* access modifiers changed from: protected */
        public void onPostExecute(Location result) {
            super.onPostExecute((Object) result);
            cancel(false);
        }
    }

    /* access modifiers changed from: private */
    public class CellIdChangedListener extends PhoneStateListener {
        private CellIdChangedListener() {
        }

        public void onCellLocationChanged(CellLocation location) {
            super.onCellLocationChanged(location);
            if (HwLocalLocationManager.this.mTelephonyManager != null) {
                int type = HwLocalLocationManager.this.mTelephonyManager.getCurrentPhoneType();
                if (type != 1) {
                    if (type != 2) {
                        int unused = HwLocalLocationManager.this.mCurrentCellId = -1;
                        int unused2 = HwLocalLocationManager.this.mCurrentAreaCode = -1;
                    } else if (location instanceof CdmaCellLocation) {
                        try {
                            CdmaCellLocation cdmaCellLocation = (CdmaCellLocation) location;
                            int unused3 = HwLocalLocationManager.this.mCurrentCellId = cdmaCellLocation.getBaseStationId();
                            int unused4 = HwLocalLocationManager.this.mCurrentAreaCode = cdmaCellLocation.getNetworkId();
                        } catch (Exception e) {
                            LBSLog.e(HwLocalLocationManager.TAG, false, "CdmaCellLocation Type Cast Exception", new Object[0]);
                        }
                    }
                } else if (location instanceof GsmCellLocation) {
                    try {
                        GsmCellLocation gsmCellLocation = (GsmCellLocation) location;
                        int unused5 = HwLocalLocationManager.this.mCurrentCellId = gsmCellLocation.getCid();
                        int unused6 = HwLocalLocationManager.this.mCurrentAreaCode = gsmCellLocation.getLac();
                    } catch (Exception e2) {
                        LBSLog.e(HwLocalLocationManager.TAG, false, "GsmCellLocation Type Cast Exception", new Object[0]);
                    }
                }
            }
        }

        public void onServiceStateChanged(ServiceState serviceState) {
            super.onServiceStateChanged(serviceState);
            if (serviceState.getState() != 0) {
                int unused = HwLocalLocationManager.this.mCurrentCellId = -1;
                int unused2 = HwLocalLocationManager.this.mCurrentAreaCode = -1;
            }
        }
    }

    class LocationTimerTask extends TimerTask {
        LocationTimerTask() {
        }

        public void run() {
            synchronized (HwLocalLocationManager.this.mLock) {
                if (HwLocalLocationManager.this.mQueryLocationTimer != null) {
                    HwLocalLocationManager.this.mQueryLocationTimer.cancel();
                    Timer unused = HwLocalLocationManager.this.mQueryLocationTimer = null;
                }
                if (HwLocalLocationManager.this.mQueryLocationTask != null && !HwLocalLocationManager.this.mQueryLocationTask.isCancelled()) {
                    HwLocalLocationManager.this.mQueryLocationTask.cancel(true);
                    LocalFixTask unused2 = HwLocalLocationManager.this.mQueryLocationTask = null;
                }
                boolean unused3 = HwLocalLocationManager.this.isLocating = false;
            }
        }
    }

    private List parserXML(String filepath) {
        if (filepath == null || filepath.isEmpty()) {
            return null;
        }
        List<String> list = new ArrayList<>();
        InputStream inputStream = null;
        try {
            InputStream inputStream2 = this.mContext.openFileInput(filepath);
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(inputStream2, "utf-8");
            for (int type = parser.getEventType(); type != 1; type = parser.next()) {
                if (type != 0) {
                    if (type != 2) {
                        if (type != 3) {
                        }
                    } else if ("app_name".equals(parser.getName())) {
                        parser.next();
                        list.add(parser.getText());
                    }
                }
            }
            if (inputStream2 != null) {
                try {
                    inputStream2.close();
                } catch (IOException e) {
                    LBSLog.e(TAG, false, e.getMessage(), new Object[0]);
                }
            }
        } catch (FileNotFoundException e2) {
            LBSLog.e(TAG, false, "FileNotFoundException in parserXML", new Object[0]);
            if (0 != 0) {
                inputStream.close();
            }
        } catch (XmlPullParserException e3) {
            LBSLog.e(TAG, false, "XmlPullParserException in parserXML", new Object[0]);
            if (0 != 0) {
                inputStream.close();
            }
        } catch (IOException e4) {
            LBSLog.e(TAG, false, "FileNotFoundException in parserXML", new Object[0]);
            if (0 != 0) {
                inputStream.close();
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    inputStream.close();
                } catch (IOException e5) {
                    LBSLog.e(TAG, false, e5.getMessage(), new Object[0]);
                }
            }
            throw th;
        }
        return list;
    }

    public byte getLocationRequestType(String appName) {
        if (appName == null || appName.isEmpty()) {
            return 0;
        }
        for (String str : this.mTimePriorityAPPNames) {
            if (appName.contains(str)) {
                LBSLog.w(TAG, false, "%{public}s location request is LOCATION_TYPE_TIME_PRIORITY", appName);
                return 1;
            }
        }
        return -1;
    }

    private void insertPersetData() {
        try {
            this.mLocalLocationDB.beginTransaction();
            this.mLocalLocationDB.setTransactionSuccessful();
            this.mLocalLocationDB.endTransaction();
        } catch (Exception e) {
            LBSLog.e(TAG, false, "local_location.db abnormal, can't open or write!", new Object[0]);
        }
    }

    private void initTimePriorityAPPNames() {
        this.mTimePriorityAPPNames.add("Map");
        this.mTimePriorityAPPNames.add("map");
        this.mTimePriorityAPPNames.add("Weather");
        this.mTimePriorityAPPNames.add("weather");
        this.mTimePriorityAPPNames.add("camera");
        this.mTimePriorityAPPNames.add("Camera");
        this.mTimePriorityAPPNames.add("tencent");
        this.mTimePriorityAPPNames.add("mall");
        this.mTimePriorityAPPNames.add("UC");
        this.mTimePriorityAPPNames.add("weibo");
        this.mTimePriorityAPPNames.add("mall");
        this.mTimePriorityAPPNames.add("gallery");
        this.mTimePriorityAPPNames.add("Gallery");
        this.mTimePriorityAPPNames.add("location");
        this.mTimePriorityAPPNames.add("Location");
        this.mTimePriorityAPPNames.add("baidu");
        this.mTimePriorityAPPNames.add("gps");
        this.mTimePriorityAPPNames.add("navi");
        this.mTimePriorityAPPNames.add("Navi");
        this.mTimePriorityAPPNames.add("didi");
        this.mTimePriorityAPPNames.add("funcity");
        this.mTimePriorityAPPNames.add("news");
        this.mTimePriorityAPPNames.add("News");
    }

    private static String getKey(byte[] c1, byte[] c2, byte[] c3) {
        return new String(right(xor(c1, left(xor(c3, left(c2, 2)), 6)), 4), Charset.defaultCharset());
    }

    private static byte[] right(byte[] source, int count) {
        byte[] temp = (byte[]) source.clone();
        for (int i = 0; i < count; i++) {
            byte m = temp[temp.length - 1];
            for (int j = temp.length - 1; j > 0; j--) {
                temp[j] = temp[j - 1];
            }
            temp[0] = m;
        }
        return temp;
    }

    private static byte[] left(byte[] source, int count) {
        byte[] temp = (byte[]) source.clone();
        for (int i = 0; i < count; i++) {
            byte m = temp[0];
            for (int j = 0; j < temp.length - 1; j++) {
                temp[j] = temp[j + 1];
            }
            temp[temp.length - 1] = m;
        }
        return temp;
    }

    private static byte[] xor(byte[] m, byte[] n) {
        byte[] temp = new byte[m.length];
        for (int i = 0; i < m.length; i++) {
            temp[i] = (byte) (m[i] ^ n[i]);
        }
        return temp;
    }
}
