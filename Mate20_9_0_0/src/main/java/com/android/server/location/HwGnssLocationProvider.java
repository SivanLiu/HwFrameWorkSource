package com.android.server.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.location.ILocationManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.os.WorkSource;
import android.provider.Settings.Secure;
import android.text.TextUtils;
import android.util.Log;
import com.huawei.cust.HwCfgFilePolicy;
import com.huawei.cust.HwCustUtils;
import com.huawei.utils.reflect.EasyInvokeFactory;
import java.util.Map;
import java.util.Properties;

public class HwGnssLocationProvider extends GnssLocationProvider {
    private static final int AGPS_TYPE_SUPL = 1;
    private static final String GNSS_NAVIGATING_FLAG = "hw_higeo_gnss_Navigating";
    private static final int GNSS_REQ_CHANGE_START = 1;
    private static final int GNSS_REQ_CHANGE_STOP = 2;
    private static final String LOCATION_MAP_BAIDU_PACKAGE = "com.baidu.BaiduMap";
    private static final String LOCATION_MAP_FLP_PACKAGE = "com.amap.android.ams";
    private static final String LOCATION_MAP_GAODE_PACKAGE = "com.autonavi.minimap";
    private static final String LOCATION_MAP_GOOGLE_PACKAGE = "com.google.android.apps.maps";
    private static final String LOCATION_MAP_WAZE_PACKAGE = "com.waze";
    private static final String MAPS_LOCATION_FLAG = "hw_higeo_maps_location";
    private static final String TAG = "HwGnssLocationProvider";
    private boolean AUTO_ACC_Enable = SystemProperties.getBoolean("ro.config.hw_auto_acc_enable", false);
    private int isLastExistMapLocation;
    private String isLocalDBEnabled;
    private BroadcastHelper mBroadcastHelper = new BroadcastHelper();
    private Context mContext;
    HwCustGpsLocationProvider mCust = ((HwCustGpsLocationProvider) HwCustUtils.createObj(HwCustGpsLocationProvider.class, new Object[]{this}));
    private Properties mHwProperties;
    private int mPreferAccuracy;
    private GpsLocationProviderUtils utils = ((GpsLocationProviderUtils) EasyInvokeFactory.getInvokeUtils(GpsLocationProviderUtils.class));

    private class BroadcastHelper {
        BroadcastReceiver innerBroadcastReciever;

        public BroadcastHelper() {
            this.innerBroadcastReciever = new BroadcastReceiver(HwGnssLocationProvider.this) {
                public void onReceive(Context context, Intent intent) {
                    if (context == null || intent == null) {
                        Log.d(HwGnssLocationProvider.TAG, "context or intent is null,return.");
                        return;
                    }
                    if ("android.intent.action.SIM_STATE_CHANGED".equals(intent.getAction())) {
                        String state = intent.getStringExtra("ss");
                        String str = HwGnssLocationProvider.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(" onReceive action state = ");
                        stringBuilder.append(state);
                        Log.d(str, stringBuilder.toString());
                        if ("LOADED".equals(state)) {
                            BroadcastHelper.this.checkAndSetAGpsParameter();
                        }
                    }
                }
            };
        }

        public void init() {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.SIM_STATE_CHANGED");
            HwGnssLocationProvider.this.mContext.registerReceiver(this.innerBroadcastReciever, intentFilter);
        }

        private void checkAndSetAGpsParameter() {
            Map agpsConfigMap = HwCfgFilePolicy.getFileConfig("xml/agps_config.xml");
            String supl_port = "";
            String supl_host = "";
            if (agpsConfigMap != null) {
                String str;
                StringBuilder stringBuilder;
                try {
                    Map portHostMap = (Map) agpsConfigMap.get("portHostKey");
                    if (portHostMap != null) {
                        supl_port = (String) portHostMap.get("supl_port");
                        supl_host = (String) portHostMap.get("supl_host");
                    }
                    if (!TextUtils.isEmpty(supl_port) && !TextUtils.isEmpty(supl_host)) {
                        HwGnssLocationProvider.this.utils.setSuplServerHost(HwGnssLocationProvider.this, supl_host);
                        HwGnssLocationProvider.this.utils.setSuplServerPort(HwGnssLocationProvider.this, Integer.parseInt(supl_port));
                        str = HwGnssLocationProvider.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("checkAGpsServer mSuplServerHost = ");
                        stringBuilder.append(HwGnssLocationProvider.this.utils.getSuplServerHost(HwGnssLocationProvider.this));
                        stringBuilder.append(" mSuplServerPort = ");
                        stringBuilder.append(HwGnssLocationProvider.this.utils.getSuplServerPort(HwGnssLocationProvider.this));
                        Log.d(str, stringBuilder.toString());
                        HwGnssLocationProvider.this.utils.native_set_agps_server(HwGnssLocationProvider.this, 1, supl_host, HwGnssLocationProvider.this.utils.getSuplServerPort(HwGnssLocationProvider.this));
                    }
                } catch (ClassCastException e) {
                    Log.e(HwGnssLocationProvider.TAG, "ClassCastException occured", e);
                } catch (NumberFormatException e2) {
                    str = HwGnssLocationProvider.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to parse supl_port: ");
                    stringBuilder.append(supl_port);
                    Log.e(str, stringBuilder.toString(), e2);
                }
            }
        }
    }

    enum MapNavigatingType {
        NOT_MAP,
        WHITE_MAP,
        BLACK_MAP
    }

    public HwGnssLocationProvider(Context context, ILocationManager ilocationManager, Looper looper) {
        super(context, ilocationManager, looper);
        this.mContext = context;
        this.mBroadcastHelper.init();
        this.mHwProperties = this.utils.getProperties(this);
        if (this.mHwProperties != null) {
            this.isLocalDBEnabled = this.mHwProperties.getProperty("LOCAL_DB");
            if (this.isLocalDBEnabled == null) {
                this.isLocalDBEnabled = "true";
            }
        }
    }

    public boolean isLocalDBEnabled() {
        return "true".equals(this.isLocalDBEnabled);
    }

    public void initDefaultApnObserver(Handler handler) {
        this.utils.setDefaultApnObserver(this, new ContentObserver(handler) {
            public void onChange(boolean selfChange) {
                HwGnssLocationProvider.this.utils.setDefaultApn(HwGnssLocationProvider.this, HwGnssLocationProvider.this.utils.getDefaultApn(HwGnssLocationProvider.this));
            }
        });
    }

    public void reportContextStatus(int status) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("reportContextStatus status = ");
        stringBuilder.append(status);
        Log.d(str, stringBuilder.toString());
        Intent intent = new Intent("huawei.android.location.DRIVER_STATUS");
        intent.putExtra("status", status);
        this.mContext.sendBroadcast(intent);
    }

    public int getPreferred_accuracy() {
        if (!this.AUTO_ACC_Enable) {
            return 0;
        }
        int accuracy_set = 0;
        if (existLocationMap()) {
            accuracy_set = 200;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getPreferred_accuracy:");
        stringBuilder.append(accuracy_set);
        Log.d(str, stringBuilder.toString());
        this.mPreferAccuracy = accuracy_set;
        return accuracy_set;
    }

    public boolean shouldReStartNavi() {
        if (!this.AUTO_ACC_Enable) {
            return false;
        }
        if ((this.mPreferAccuracy == 200 || !existLocationMap()) && (this.mPreferAccuracy != 200 || existLocationMap())) {
            return false;
        }
        return true;
    }

    private boolean existLocationMap() {
        WorkSource workSource = getWorkSource();
        if (workSource != null) {
            int num_uName = workSource.size();
            int i = 0;
            while (i < num_uName) {
                if (workSource.getName(i).equals(LOCATION_MAP_GAODE_PACKAGE) || workSource.getName(i).equals(LOCATION_MAP_FLP_PACKAGE) || workSource.getName(i).equals(LOCATION_MAP_BAIDU_PACKAGE)) {
                    Log.d(TAG, "existLocationMap:true");
                    return true;
                }
                i++;
            }
        }
        Log.d(TAG, "existLocationMap:false");
        return false;
    }

    public void handleGnssRequirementsChange(int reson) {
        if (reson == 1) {
            handleMapLocation(existLocationMapsForHigeo());
        } else if (reson == 2) {
            handleMapLocation(MapNavigatingType.NOT_MAP.ordinal());
        }
    }

    private void handleMapLocation(int mapNavigatingType) {
        int new_state = mapNavigatingType;
        String str;
        StringBuilder stringBuilder;
        if (this.isLastExistMapLocation != new_state) {
            this.isLastExistMapLocation = new_state;
            Secure.putInt(this.mContext.getContentResolver(), MAPS_LOCATION_FLAG, new_state);
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("handleGnssRequirementsChange,existLocationMap = ");
            stringBuilder.append(new_state);
            Log.d(str, stringBuilder.toString());
            return;
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("existLocationMap state is not change,ignor! isLastExistMapLocation:");
        stringBuilder.append(this.isLastExistMapLocation);
        Log.d(str, stringBuilder.toString());
    }

    public void handleGnssNavigatingStateChange(boolean start) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleGnssNavigateState,start : ");
        stringBuilder.append(start);
        Log.d(str, stringBuilder.toString());
    }

    public void startNavigatingPreparedHook() {
        if (this.mCust != null) {
            this.utils.setPositionMode(this, this.mCust.setPostionMode(this.utils.getPositionMode(this)));
        }
    }

    public boolean sendExtraCommandHook(String command, boolean result) {
        boolean hwResult = result;
        if (this.mCust == null) {
            return hwResult;
        }
        hwResult = this.mCust.sendPostionModeCommand(result, command);
        this.utils.setPositionMode(this, this.mCust.setPostionMode(this.utils.getPositionMode(this)));
        return hwResult;
    }

    public void handleUpdateNetworkStateHook(NetworkInfo info) {
        boolean isRoaming = false;
        if (info != null) {
            isRoaming = info.isRoaming();
        }
        if (this.mCust != null) {
            this.mCust.setRoaming(isRoaming);
        }
    }

    private int existLocationMapsForHigeo() {
        WorkSource workSource = getWorkSource();
        if (workSource != null) {
            int num_uName = workSource.size();
            int i = 0;
            while (i < num_uName) {
                if ("com.google.android.apps.maps".equals(workSource.getName(i))) {
                    Log.d(TAG, "existLocationMapsForHigeo: black map");
                    return MapNavigatingType.BLACK_MAP.ordinal();
                } else if (LOCATION_MAP_GAODE_PACKAGE.equals(workSource.getName(i)) || LOCATION_MAP_FLP_PACKAGE.equals(workSource.getName(i)) || LOCATION_MAP_WAZE_PACKAGE.equals(workSource.getName(i)) || LOCATION_MAP_BAIDU_PACKAGE.equals(workSource.getName(i))) {
                    Log.d(TAG, "existLocationMapsForHigeo: white map");
                    return MapNavigatingType.WHITE_MAP.ordinal();
                } else {
                    i++;
                }
            }
        }
        return MapNavigatingType.NOT_MAP.ordinal();
    }

    protected String getSvType(int svidWithFlag) {
        String result = "unknown";
        switch ((svidWithFlag >> 4) & 15) {
            case 1:
                result = "gps";
                break;
            case 2:
                result = "sbas";
                break;
            case 3:
                result = "glonass";
                break;
            case 4:
                result = "qzss";
                break;
            case 5:
                result = "beidou";
                break;
            case 6:
                result = "galileo";
                break;
        }
        return String.format("%-7s", new Object[]{result});
    }
}
