package android.view;

import android.content.res.Resources.NotFoundException;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.util.Log;
import com.huawei.hsm.permission.StubController;
import java.util.ArrayList;
import java.util.List;

public class HwAdCleaner implements IHwAdCleaner {
    private static final int CODE_GET_AD_KEY_LIST = 1016;
    private static final String DESCRIPTOR_HW_AD_CLEANER = "android.view.HwAdCleaner";
    private static final String TAG = "HwAdCleaner";
    private static final int VIEW_IS_ADVIEW = 0;
    private static final int VIEW_IS_NOT_ADVIEW_OR_DISABLE = 1;
    private static final int VIEW_IS_NOT_CHECKED = -1;
    private static HwAdCleaner mInstance = null;
    private List<String> mAdIdList;
    private List<String> mAdViewList;
    private String mApkName;
    private boolean mEnableAdChecked;
    private boolean mEnableDebug;
    private boolean mInit;
    private boolean mRulesHasRead;

    private HwAdCleaner() {
        this.mAdViewList = null;
        this.mAdIdList = null;
        this.mApkName = "none";
        this.mEnableAdChecked = true;
        this.mEnableDebug = false;
        this.mRulesHasRead = false;
        this.mInit = true;
        this.mAdViewList = new ArrayList();
        this.mAdIdList = new ArrayList();
        int prop = SystemProperties.getInt("enable.view.adcleaner", 1);
        if (prop == 0) {
            this.mEnableAdChecked = false;
        } else if (2 == prop) {
            this.mEnableDebug = true;
            Log.d(TAG, "1.HwAdCleaner was created!");
        }
    }

    public static synchronized HwAdCleaner getDefault() {
        HwAdCleaner hwAdCleaner;
        synchronized (HwAdCleaner.class) {
            if (mInstance == null) {
                mInstance = new HwAdCleaner();
            }
            hwAdCleaner = mInstance;
        }
        return hwAdCleaner;
    }

    public void readRulesInThread() {
        new Thread("readAdFilterRules") {
            public void run() {
                if (HwAdCleaner.this.mEnableDebug) {
                    String str = HwAdCleaner.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("  2. Thread readAdFilterRules was created!  mApkName = ");
                    stringBuilder.append(HwAdCleaner.this.mApkName);
                    Log.d(str, stringBuilder.toString());
                }
                HwAdCleaner.this.mEnableAdChecked = HwAdCleaner.this.doReadRulesByBinder();
                HwAdCleaner.this.mRulesHasRead = true;
            }
        }.start();
    }

    public boolean doReadRulesByBinder() {
        Parcel _data = Parcel.obtain();
        Parcel _reply = Parcel.obtain();
        IBinder b = ServiceManager.getService("network_management");
        if (b != null) {
            try {
                if (this.mEnableDebug) {
                    Log.d(TAG, "     3. getService NETWORKMANAGEMENT_SERVICE success!");
                }
                _data.writeInterfaceToken(DESCRIPTOR_HW_AD_CLEANER);
                _data.writeString(this.mApkName);
                b.transact(1016, _data, _reply, 0);
                this.mAdViewList.clear();
                this.mAdIdList.clear();
                this.mAdViewList = _reply.createStringArrayList();
                this.mAdIdList = _reply.createStringArrayList();
                if (this.mEnableDebug) {
                    int i;
                    String str;
                    StringBuilder stringBuilder;
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("       4.1 mAdViewList size = ");
                    stringBuilder2.append(this.mAdViewList.size());
                    Log.d(str2, stringBuilder2.toString());
                    for (i = 0; i < this.mAdViewList.size(); i++) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("          i=");
                        stringBuilder.append(i);
                        stringBuilder.append(", value = ");
                        stringBuilder.append((String) this.mAdViewList.get(i));
                        Log.d(str, stringBuilder.toString());
                    }
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("       4.2 mAdIdList size = ");
                    stringBuilder2.append(this.mAdIdList.size());
                    Log.d(str2, stringBuilder2.toString());
                    for (i = 0; i < this.mAdIdList.size(); i++) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("          i=");
                        stringBuilder.append(i);
                        stringBuilder.append(", value = ");
                        stringBuilder.append((String) this.mAdIdList.get(i));
                        Log.d(str, stringBuilder.toString());
                    }
                }
                _reply.readException();
            } catch (RemoteException localRemoteException) {
                Log.d(TAG, "RemoteException ");
                localRemoteException.printStackTrace();
            } catch (Throwable th) {
                _reply.recycle();
                _data.recycle();
            }
        }
        _reply.recycle();
        _data.recycle();
        if (this.mAdViewList.isEmpty() && this.mAdIdList.isEmpty()) {
            return false;
        }
        return true;
    }

    public int checkAdCleaner(View inView, String appName, int uid) {
        if (!this.mEnableAdChecked) {
            return 1;
        }
        if (appName == null || inView == null) {
            Log.e(TAG, "err: AppName/inview is null !");
            return -1;
        }
        if (this.mInit) {
            this.mInit = false;
            this.mApkName = appName;
            if (this.mApkName.contains(StubController.APP_GOOGLE) || this.mApkName.contains("huawei")) {
                this.mEnableAdChecked = false;
                return 1;
            }
            readRulesInThread();
        }
        if (!this.mRulesHasRead) {
            return -1;
        }
        String viewIdName;
        String viewClsName = inView.getClass().getName();
        try {
            viewIdName = inView.getContext().getResources().getResourceEntryName(inView.getId());
        } catch (NotFoundException e) {
            viewIdName = "none";
        }
        if (this.mEnableDebug) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("             viewName = ");
            stringBuilder.append(viewClsName);
            stringBuilder.append("@");
            stringBuilder.append(viewIdName);
            Log.d(str, stringBuilder.toString());
        }
        if (isViewMatched(viewClsName, viewIdName)) {
            return 0;
        }
        if (viewIdName == null || this.mAdIdList == null || !this.mAdIdList.contains(viewIdName)) {
            return 1;
        }
        if (this.mEnableDebug) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("           5.Id = ");
            stringBuilder2.append(viewIdName);
            stringBuilder2.append(" matched !");
            Log.d(str2, stringBuilder2.toString());
        }
        return 0;
    }

    private boolean isViewMatched(String viewClsName, String viewIdName) {
        if (!(viewClsName == null || this.mAdViewList == null)) {
            for (int i = 0; i < this.mAdViewList.size(); i++) {
                StringBuilder stringBuilder;
                String temp = (String) this.mAdViewList.get(i);
                boolean isMatched = false;
                if (temp.contains("@")) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(viewClsName);
                    stringBuilder.append("@");
                    stringBuilder.append(viewIdName);
                    if (temp.equals(stringBuilder.toString())) {
                        isMatched = true;
                    }
                } else if (temp.equals(viewClsName)) {
                    isMatched = true;
                }
                if (isMatched) {
                    if (this.mEnableDebug) {
                        String str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("           5.View = ");
                        stringBuilder.append(temp);
                        stringBuilder.append(" matched !");
                        Log.d(str, stringBuilder.toString());
                    }
                    return true;
                }
            }
        }
        return false;
    }
}
