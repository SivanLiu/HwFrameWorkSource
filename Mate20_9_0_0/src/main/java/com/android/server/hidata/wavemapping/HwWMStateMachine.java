package com.android.server.hidata.wavemapping;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import com.android.server.hidata.appqoe.HwAPPQoEManager;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.hidata.wavemapping.cons.ParamManager;
import com.android.server.hidata.wavemapping.cons.WMStateCons;
import com.android.server.hidata.wavemapping.dao.EnterpriseApDAO;
import com.android.server.hidata.wavemapping.dao.LocationDAO;
import com.android.server.hidata.wavemapping.dao.MobileApDAO;
import com.android.server.hidata.wavemapping.dao.RegularPlaceDAO;
import com.android.server.hidata.wavemapping.dataprovider.BehaviorReceiver;
import com.android.server.hidata.wavemapping.dataprovider.FrequentLocation;
import com.android.server.hidata.wavemapping.dataprovider.HwWmpCallbackImpl;
import com.android.server.hidata.wavemapping.dataprovider.WifiDataProvider;
import com.android.server.hidata.wavemapping.entity.ApInfo;
import com.android.server.hidata.wavemapping.entity.ClusterResult;
import com.android.server.hidata.wavemapping.entity.HwWmpAppInfo;
import com.android.server.hidata.wavemapping.entity.ParameterInfo;
import com.android.server.hidata.wavemapping.entity.RecognizeResult;
import com.android.server.hidata.wavemapping.entity.RegularPlaceInfo;
import com.android.server.hidata.wavemapping.entity.UiInfo;
import com.android.server.hidata.wavemapping.modelservice.ModelService5;
import com.android.server.hidata.wavemapping.service.ActiveCollectDecision;
import com.android.server.hidata.wavemapping.service.RecognizeService;
import com.android.server.hidata.wavemapping.service.UiService;
import com.android.server.hidata.wavemapping.statehandler.CollectFingersHandler;
import com.android.server.hidata.wavemapping.statehandler.CollectUserFingersHandler;
import com.android.server.hidata.wavemapping.util.CellStateMonitor;
import com.android.server.hidata.wavemapping.util.LogUtil;
import com.android.server.hidata.wavemapping.util.TimeUtil;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class HwWMStateMachine extends StateMachine {
    public static final String TAG;
    private static HwWMStateMachine hwWMStateMachine;
    private int SUBSTATE_ACTIVITY = 1;
    private int SUBSTATE_LASTRECG = 0;
    private CollectFingersHandler collectFingersHandler;
    private RegularPlaceInfo cur_mainApPlaceInfo = null;
    private RegularPlaceInfo cur_place = null;
    private RecognizeResult cur_preLable = null;
    private HandlerThread handlerThread;
    private boolean initFinish = false;
    private RecognizeResult last_preLable = new RecognizeResult();
    private HwAPPQoEManager mAPPQoEManager = null;
    private ActiveCollectDecision mActiveCollectHandler = null;
    private CellStateMonitor mCellStateMonitor = null;
    private CollectTrainingState mCollectTrainingState = new CollectTrainingState();
    private CollectUserFingersHandler mCollectUserFingersHandler = null;
    private Context mCtx;
    private State mDefaultState = new DefaultState();
    private FrequentLocation mFrequentLocation;
    private FrequentLocationState mFrequentLocationState = new FrequentLocationState();
    private Handler mHandler;
    private HwWmpCallbackImpl mHwWmpCallbackImpl = null;
    private LocatingState mLocatingState = new LocatingState();
    private LocationDAO mLocationDAO;
    private PositionState mPositionState = new PositionState();
    private RecognitionState mRecognitionState = new RecognitionState();
    private long mScanEndTime = 0;
    private WifiManager mWifiManager;
    private BehaviorReceiver mbehaviorReceiver;
    public ModelService5 modelService = null;
    private ParameterInfo param;
    private RegularPlaceDAO regularPlaceDAO;
    private HashMap<String, RegularPlaceInfo> rgLocations;
    private String toastInfo;
    private UiInfo uiInfo;
    private UiService uiService;
    private WifiDataProvider wifiDataProvider;
    private List<ScanResult> wifiList = null;

    class CollectTrainingState extends State {
        public final String TAG;
        private RegularPlaceInfo regularPlaceInfo;

        CollectTrainingState() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("WMapping.");
            stringBuilder.append(CollectTrainingState.class.getSimpleName());
            this.TAG = stringBuilder.toString();
        }

        public void enter() {
            LogUtil.d("enter mCollectTrainingState");
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 64) {
                LogUtil.d("MSG_WIFI_UPDATE_SCAN_RESULT002, wifi scan complete");
                HwWMStateMachine.this.mActiveCollectHandler.startBgScan();
                if (HwWMStateMachine.this.collectFingersHandler.checkDataSatisfiedToTraining(HwWMStateMachine.this.cur_place)) {
                    HwWMStateMachine.this.sendMessage(80);
                }
            } else if (i == 110) {
                LogUtil.d("MSG_AR_MOVE, user go to other space");
                HwWMStateMachine.this.mActiveCollectHandler.stopBgScan();
                HwWMStateMachine.this.mActiveCollectHandler.stopStallScan();
                HwWMStateMachine.this.mActiveCollectHandler.stopOut4gRecgScan();
                return false;
            } else if (i == 141 || i == 211) {
                LogUtil.d("Into MSG_APP_DATA_STALL");
                if (HwWMStateMachine.this.mActiveCollectHandler != null) {
                    HwWMStateMachine.this.mActiveCollectHandler.startStallScan();
                }
            } else {
                switch (i) {
                    case 80:
                        HwWMStateMachine.this.trainModels(HwWMStateMachine.this.cur_place.getPlace());
                        break;
                    case WMStateCons.MSG_BUILDMODEL_COMPLETED /*81*/:
                        LogUtil.d("MSG_FINGER_NUM_SATISFIED, training criteria is satisfied");
                        if (HwWMStateMachine.this.cur_place != null && HwWMStateMachine.this.cur_place.getPlace() != null) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("    current location = ");
                            stringBuilder.append(HwWMStateMachine.this.cur_place.getPlace());
                            LogUtil.d(stringBuilder.toString());
                            if (!HwWMStateMachine.this.getBuildModelResult()) {
                                HwWMStateMachine.this.transitionTo(HwWMStateMachine.this.mCollectTrainingState);
                                break;
                            }
                            HwWMStateMachine.this.transitionTo(HwWMStateMachine.this.mLocatingState);
                            break;
                        }
                        LogUtil.e(" current location == null");
                        HwWMStateMachine.this.transitionTo(HwWMStateMachine.this.mDefaultState);
                        break;
                        break;
                    default:
                        return false;
                }
            }
            return true;
        }

        public void exit() {
            LogUtil.d("exit mCollectTrainingState");
            HwWMStateMachine.this.mActiveCollectHandler.stopBgScan();
            HwWMStateMachine.this.mActiveCollectHandler.stopStallScan();
            if (HwWMStateMachine.this.getHandler().hasMessages(64)) {
                HwWMStateMachine.this.removeMessages(64);
            }
        }
    }

    class DefaultState extends State {
        public final String TAG;

        DefaultState() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("WMapping.");
            stringBuilder.append(DefaultState.class.getSimpleName());
            this.TAG = stringBuilder.toString();
        }

        public void enter() {
            LogUtil.d("enter DefaultState");
        }

        public boolean processMessage(Message message) {
            StringBuilder stringBuilder;
            Message message2 = message;
            if (HwWMStateMachine.this.initFinish) {
                int i = message2.what;
                Bundle bundle;
                String curLocation;
                int freqLoc;
                if (i == 20) {
                    try {
                        LogUtil.i("Into MSG_ADD_FREQ_LOCATION_TOOL");
                        bundle = message.getData();
                        if (bundle == null || bundle.get("LOCATION") == null) {
                            LogUtil.w(" no bundle location");
                        } else {
                            curLocation = bundle.get("LOCATION").toString();
                            freqLoc = -1;
                            if (curLocation.equals(Constant.NAME_FREQLOCATION_HOME)) {
                                freqLoc = 0;
                            } else if (curLocation.equals(Constant.NAME_FREQLOCATION_OFFICE)) {
                                freqLoc = 1;
                            }
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("MSG_ADD_FREQ_LOCATION_TOOL,curLocation=");
                            stringBuilder2.append(curLocation);
                            LogUtil.d(stringBuilder2.toString());
                            HwWMStateMachine.this.mFrequentLocation.updateWaveMapping(freqLoc, 0);
                        }
                    } catch (Exception e) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("processMessage:");
                        stringBuilder.append(e);
                        LogUtil.e(stringBuilder.toString());
                    }
                } else if (i == 30) {
                    LogUtil.d("Into MSG_IN_FREQ_LOCATION");
                    try {
                        bundle = message.getData();
                        if (bundle == null || bundle.get("LOCATION") == null) {
                            LogUtil.w(" no bundle location");
                        } else {
                            curLocation = bundle.get("LOCATION").toString();
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("MSG_IN_FREQ_LOCATION,curLocation=");
                            stringBuilder.append(curLocation);
                            LogUtil.d(stringBuilder.toString());
                            if (HwWMStateMachine.this.cur_place == null || HwWMStateMachine.this.cur_place.getPlace() == null) {
                                LogUtil.d(" cur_place == null");
                                HwWMStateMachine.this.getCur_place(curLocation);
                                if (HwWMStateMachine.this.cur_place.getPlace() == null) {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append(" getPlace error:");
                                    stringBuilder.append(HwWMStateMachine.this.cur_place.toString());
                                    LogUtil.e(stringBuilder.toString());
                                    HwWMStateMachine.this.transitionTo(HwWMStateMachine.this.mDefaultState);
                                } else if (!HwWMStateMachine.this.cur_place.getPlace().equals(curLocation)) {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append(" getPlace:");
                                    stringBuilder.append(HwWMStateMachine.this.cur_place.toString());
                                    stringBuilder.append(" not the same as input location:");
                                    stringBuilder.append(curLocation);
                                    LogUtil.e(stringBuilder.toString());
                                    HwWMStateMachine.this.transitionTo(HwWMStateMachine.this.mDefaultState);
                                }
                            } else if (HwWMStateMachine.this.cur_place.getPlace().equals(curLocation)) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(" the same location - KEEP current state, current ");
                                stringBuilder.append(HwWMStateMachine.this.cur_place.toString());
                                LogUtil.d(stringBuilder.toString());
                            }
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(" cur_place.getPlace()=");
                            stringBuilder.append(HwWMStateMachine.this.cur_place.getPlace());
                            LogUtil.i(stringBuilder.toString());
                            HwWMStateMachine.this.collectFingersHandler = new CollectFingersHandler(HwWMStateMachine.this.mCtx, HwWMStateMachine.this.cur_place.getPlace(), HwWMStateMachine.this.getHandler());
                            HwWMStateMachine.this.mCollectUserFingersHandler.assignSpaceExp2Space(HwWMStateMachine.this.last_preLable);
                            HwWMStateMachine.this.mCollectUserFingersHandler.setFreqLocation(HwWMStateMachine.this.cur_place.getPlace());
                            if (HwWMStateMachine.this.cur_place.getState() == 4) {
                                HwWMStateMachine.this.transitionTo(HwWMStateMachine.this.mLocatingState);
                            } else {
                                HwWMStateMachine.this.transitionTo(HwWMStateMachine.this.mCollectTrainingState);
                            }
                        }
                    } catch (Exception e2) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("WMStateCons.MSG_IN_FREQ_LOCATION:");
                        stringBuilder.append(e2.getMessage());
                        LogUtil.e(stringBuilder.toString());
                    }
                } else if (i == 110) {
                    LogUtil.d("into MSG_AR_MOVE");
                    HwWMStateMachine.this.SUBSTATE_ACTIVITY = 2;
                } else if (i == 113) {
                    LogUtil.d("into MSG_AR_STATION");
                    HwWMStateMachine.this.SUBSTATE_ACTIVITY = 1;
                } else if (i == 115) {
                    LogUtil.d("Into MSG_USER_DATA_ACTION");
                    if (HwWMStateMachine.this.mCollectUserFingersHandler != null) {
                        HwWMStateMachine.this.mCollectUserFingersHandler.updateSourceNetwork();
                    }
                } else if (i == 120) {
                    LogUtil.d("into MSG_QUERY_HISQOE, query saved history App QoE");
                    bundle = message.getData();
                    if (bundle == null || !bundle.containsKey("FULLID")) {
                        LogUtil.w(" no bundle");
                        IWaveMappingCallback cb = message2.obj;
                        if (cb != null) {
                            cb.onWaveMappingRespondCallback(0, 0, 0, true, false);
                        }
                    } else {
                        freqLoc = bundle.getInt("FULLID");
                        int UID = bundle.getInt("UID");
                        int net = bundle.getInt("NW");
                        int ArbNet = bundle.getInt("ArbNW");
                        IWaveMappingCallback cb2 = message2.obj;
                        if (cb2 != null) {
                            cb2.onWaveMappingRespondCallback(UID, 0, ArbNet, true, false);
                        }
                    }
                } else if (i == 130) {
                    LogUtil.i("into MSG_SYS_SHUTDOWN, go back Default");
                    HwWMStateMachine.this.transitionTo(HwWMStateMachine.this.mDefaultState);
                } else if (i != 210) {
                    switch (i) {
                        case WMStateCons.MSG_CONNECTIVITY_CHANGE /*91*/:
                            LogUtil.d("Into MSG_CONNECTIVITY_CHANGE");
                            if (HwWMStateMachine.this.mCollectUserFingersHandler != null) {
                                HwWMStateMachine.this.mCollectUserFingersHandler.checkConnectivityState();
                                break;
                            }
                            break;
                        case WMStateCons.MSG_SUPPLICANT_COMPLETE /*92*/:
                            LogUtil.i("Into MSG_SUPPLICANT_COMPLETE");
                            if (HwWMStateMachine.this.mCollectUserFingersHandler != null) {
                                HwWMStateMachine.this.mCollectUserFingersHandler.updateWifiDurationForAp(true);
                                break;
                            }
                            break;
                        case WMStateCons.MSG_CELL_CHANGE /*93*/:
                        case WMStateCons.MSG_CELL_IN_SERVICE /*95*/:
                            LogUtil.i("Into MSG_CELL_CHANGE");
                            if (HwWMStateMachine.this.mCollectUserFingersHandler != null) {
                                HwWMStateMachine.this.mCollectUserFingersHandler.updateMobileDurationForCell(true);
                                HwWMStateMachine.this.mCollectUserFingersHandler.checkOutOf4GCoverage(false);
                                break;
                            }
                            break;
                        case WMStateCons.MSG_SIM_STATE_CHANGE /*94*/:
                            LogUtil.i("Into MSG_SIM_STATE_CHANGE");
                            if (HwWMStateMachine.this.mCollectUserFingersHandler != null) {
                                HwWMStateMachine.this.mCollectUserFingersHandler.assignSpaceExp2Space(HwWMStateMachine.this.last_preLable);
                                HwWMStateMachine.this.mCollectUserFingersHandler.setCurrScrbId();
                                break;
                            }
                            break;
                        case WMStateCons.MSG_CELL_OUT_OF_SERVICE /*96*/:
                            LogUtil.i("Into MSG_CELL_OUT_OF_SERVICE");
                            if (HwWMStateMachine.this.mCollectUserFingersHandler != null) {
                                HwWMStateMachine.this.mCollectUserFingersHandler.updateMobileDurationForCell(true);
                                break;
                            }
                            break;
                        default:
                            HwWmpAppInfo mAppInfo;
                            switch (i) {
                                case 200:
                                    LogUtil.d("Into MSG_APP_STATE_START");
                                    mAppInfo = message2.obj;
                                    if (mAppInfo != null) {
                                        if (HwWMStateMachine.this.mCollectUserFingersHandler != null) {
                                            HwWMStateMachine.this.mCollectUserFingersHandler.startAppCollect(mAppInfo);
                                            break;
                                        }
                                    }
                                    LogUtil.e(" no app messages");
                                    break;
                                    break;
                                case 201:
                                    LogUtil.d("Into MSG_APP_STATE_END");
                                    mAppInfo = message2.obj;
                                    if (mAppInfo != null) {
                                        if (HwWMStateMachine.this.mCollectUserFingersHandler != null) {
                                            HwWMStateMachine.this.mCollectUserFingersHandler.endAppCollect(mAppInfo);
                                            break;
                                        }
                                    }
                                    LogUtil.e(" no app messages");
                                    break;
                                    break;
                                case 202:
                                    LogUtil.d("Into MSG_APP_STATE_NWUPDATE");
                                    mAppInfo = message2.obj;
                                    if (mAppInfo != null) {
                                        if (HwWMStateMachine.this.mCollectUserFingersHandler != null) {
                                            HwWMStateMachine.this.mCollectUserFingersHandler.updateAppNetwork(mAppInfo.getAppName(), mAppInfo.getConMgrNetworkType());
                                            break;
                                        }
                                    }
                                    LogUtil.e(" no app messages");
                                    break;
                                    break;
                                default:
                                    StringBuilder stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("into default, msg:");
                                    stringBuilder3.append(message2.what);
                                    LogUtil.i(stringBuilder3.toString());
                                    break;
                            }
                    }
                } else {
                    LogUtil.d("Into MSG_APP_QOE_EVENT");
                    bundle = message.getData();
                    if (bundle == null) {
                        LogUtil.e(" no bundle messages");
                    } else if (HwWMStateMachine.this.mCollectUserFingersHandler != null) {
                        HwWMStateMachine.this.mCollectUserFingersHandler.updateAppQoE(bundle.getString("APPNAME"), bundle.getInt("QOE"));
                    }
                }
                return true;
            }
            LogUtil.w(" StateMachine NOT initial");
            return true;
        }

        public void exit() {
            LogUtil.d("exit DefaultState");
        }
    }

    class FrequentLocationState extends State {
        public final String TAG;

        FrequentLocationState() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("WMapping.");
            stringBuilder.append(FrequentLocationState.class.getSimpleName());
            this.TAG = stringBuilder.toString();
        }

        public void enter() {
            LogUtil.d("enter mFrequentLocationState");
            HwWMStateMachine.this.wifiDataProvider.start();
            if (HwWMStateMachine.this.cur_place != null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("cur_place=");
                stringBuilder.append(HwWMStateMachine.this.cur_place.getPlace());
                LogUtil.i(stringBuilder.toString());
                HwWMStateMachine.this.collectFingersHandler.startCollect();
                HwWMStateMachine.this.mCellStateMonitor.startMonitor();
                return;
            }
            LogUtil.e(" empty current location, back to DefaultState");
            HwWMStateMachine.this.transitionTo(HwWMStateMachine.this.mDefaultState);
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            StringBuilder stringBuilder;
            if (i == 50) {
                LogUtil.d("In to Other Location, MSG_OUT_FREQ_LOCATION");
                if (HwWMStateMachine.this.uiInfo != null) {
                    HwWMStateMachine.this.uiInfo.setFinger_batch_num(0);
                    HwWMStateMachine.this.uiInfo.setStage(0);
                    HwWMStateMachine.this.uiInfo.setSsid(Constant.NAME_FREQLOCATION_OTHER);
                }
                HwWMStateMachine.this.transitionTo(HwWMStateMachine.this.mDefaultState);
            } else if (i == 55) {
                LogUtil.i("In to Other Location, MSG_LEAVE_FREQ_LOCATION_TOOL");
                if (!(HwWMStateMachine.this.cur_place == null || HwWMStateMachine.this.cur_place.getPlace() == null)) {
                    i = 1;
                    if (HwWMStateMachine.this.cur_place.getPlace().equals(Constant.NAME_FREQLOCATION_HOME)) {
                        i = 0;
                    }
                    HwWMStateMachine.this.mFrequentLocation.updateWaveMapping(i, 1);
                }
            } else if (i == 60) {
                LogUtil.d("Into MSG_TOOL_FORCE_TRAINING, force training by tool");
                if (HwWMStateMachine.this.cur_place == null || HwWMStateMachine.this.cur_place.getPlace() == null) {
                    LogUtil.e(" current location == null");
                    HwWMStateMachine.this.transitionTo(HwWMStateMachine.this.mDefaultState);
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("  force training by tool, current location = ");
                    stringBuilder.append(HwWMStateMachine.this.cur_place.getPlace());
                    LogUtil.d(stringBuilder.toString());
                    HwWMStateMachine.this.trainModels(HwWMStateMachine.this.cur_place.getPlace());
                }
            } else if (i == 62) {
                LogUtil.d("MSG_WIFI_UPDATE_SCAN_RESULT01, wifi scan complete");
                try {
                    HwWMStateMachine.this.wifiList = HwWMStateMachine.this.mWifiManager.getScanResults();
                    if (HwWMStateMachine.this.wifiList == null) {
                        LogUtil.d("MSG_WIFI_UPDATE_SCAN_RESULT, wifiList=null");
                    } else if (HwWMStateMachine.this.cur_place == null) {
                        LogUtil.d("MSG_WIFI_UPDATE_SCAN_RESULT, cur_place=null");
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("MSG_WIFI_UPDATE_SCAN_RESULT, cur_place=");
                        stringBuilder.append(HwWMStateMachine.this.cur_place.toString());
                        stringBuilder.append(", wifiList.size=");
                        stringBuilder.append(HwWMStateMachine.this.wifiList.size());
                        LogUtil.i(stringBuilder.toString());
                        HwWMStateMachine.this.collectFingersHandler.processWifiData(HwWMStateMachine.this.wifiList, HwWMStateMachine.this.cur_place);
                    }
                } catch (Exception e) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("MSG_WIFI_UPDATE_SCAN_RESULT,e");
                    stringBuilder2.append(e.getMessage());
                    LogUtil.e(stringBuilder2.toString());
                }
            } else if (i != 141) {
                return false;
            } else {
                LogUtil.d("Into MSG_BACK_4G_COVERAGE, stop active scan timer");
                HwWMStateMachine.this.mActiveCollectHandler.stopOut4gRecgScan();
            }
            return true;
        }

        public void exit() {
            LogUtil.d("exit mFrequentLocationState");
            if (!HwWMStateMachine.this.collectFingersHandler.stopCollect()) {
                LogUtil.e(" Stop Collection Failure");
            }
            HwWMStateMachine.this.cur_place = null;
            HwWMStateMachine.this.mCollectUserFingersHandler.assignSpaceExp2Space(HwWMStateMachine.this.last_preLable);
            HwWMStateMachine.this.mCollectUserFingersHandler.setFreqLocation(Constant.NAME_FREQLOCATION_OTHER);
            HwWMStateMachine.this.mCollectUserFingersHandler.resetOut4GBeginTime();
            HwWMStateMachine.this.wifiDataProvider.stop();
            if (HwWMStateMachine.this.getHandler().hasMessages(62)) {
                HwWMStateMachine.this.removeMessages(62);
            }
        }
    }

    class LocatingState extends State {
        public final String TAG;
        private RecognizeService mainApRecognizeService;

        LocatingState() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("WMapping.");
            stringBuilder.append(LocatingState.class.getSimpleName());
            this.TAG = stringBuilder.toString();
        }

        public void enter() {
            LogUtil.d("enter mLocatingState");
            RecognizeResult recognizeResult = new RecognizeResult();
            recognizeResult.setRgResult("0");
            recognizeResult.setAllApModelName(HwWMStateMachine.this.cur_place.getModelName());
            recognizeResult.setMainApRgResult("0");
            recognizeResult.setMainApModelName(HwWMStateMachine.this.cur_mainApPlaceInfo.getModelName());
            HwWMStateMachine.this.setCur_preLable(recognizeResult);
            HwWMStateMachine.this.mCollectUserFingersHandler.assignSpaceExp2Space(HwWMStateMachine.this.cur_preLable);
            HwWMStateMachine.this.mCollectUserFingersHandler.recognizeActions(HwWMStateMachine.this.cur_preLable);
            HwWMStateMachine.this.last_preLable = HwWMStateMachine.this.cur_preLable.normalizeCopy();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" save last results:");
            stringBuilder.append(HwWMStateMachine.this.last_preLable.toString());
            LogUtil.i(stringBuilder.toString());
            if (HwWMStateMachine.this.modelService != null) {
                this.mainApRecognizeService = new RecognizeService(HwWMStateMachine.this.modelService);
            } else {
                LogUtil.d("enter mLocatingState,null == modelService");
                HwWMStateMachine.this.modelService = ModelService5.getInstance(HwWMStateMachine.this.getHandler());
                this.mainApRecognizeService = new RecognizeService(HwWMStateMachine.this.modelService);
            }
            if (1 == HwWMStateMachine.this.SUBSTATE_LASTRECG) {
                HwWMStateMachine.this.mLocationDAO.accCHRSpaceLeavebyFreqLoc(HwWMStateMachine.this.cur_place.getPlace());
            }
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 113) {
                LogUtil.d("into MSG_AR_STATION, stand in a certain space");
                HwWMStateMachine.this.transitionTo(HwWMStateMachine.this.mLocatingState);
                return false;
            } else if (i != 140) {
                return false;
            } else {
                LogUtil.d("into MSG_CHECK_4G_COVERAGE, check 4G coverage by mainAp result");
                StringBuilder stringBuilder;
                try {
                    if (2 == HwWMStateMachine.this.SUBSTATE_ACTIVITY) {
                        LogUtil.d(" Activity Substate Moving: restart timer");
                        HwWMStateMachine.this.mActiveCollectHandler.stopOut4gRecgScan();
                    } else if (1 == HwWMStateMachine.this.SUBSTATE_ACTIVITY) {
                        if (this.mainApRecognizeService == null) {
                            LogUtil.e(" null == recognizeService");
                        } else {
                            RecognizeResult recognizeResult = this.mainApRecognizeService.identifyLocationByMainAp(HwWMStateMachine.this.cur_mainApPlaceInfo);
                            if (recognizeResult == null) {
                                LogUtil.d(" mainAp recognizeResult failure, recognizeResult = null");
                                HwWMStateMachine.this.mActiveCollectHandler.startOut4gRecgScan();
                            } else if (recognizeResult.getMainApRgResult().contains(Constant.RESULT_UNKNOWN)) {
                                LogUtil.d(" mainAp recognizeResult failure, result == unknonw");
                                HwWMStateMachine.this.mActiveCollectHandler.startOut4gRecgScan();
                            } else {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(" found the mainAp space:");
                                stringBuilder.append(recognizeResult.toString());
                                LogUtil.d(stringBuilder.toString());
                                if (!HwWMStateMachine.this.mCollectUserFingersHandler.determine4gCoverage(recognizeResult.normalizeCopy())) {
                                    HwWMStateMachine.this.mActiveCollectHandler.startOut4gRecgScan();
                                }
                            }
                        }
                    }
                    HwWMStateMachine.this.mCollectUserFingersHandler.checkOutOf4GCoverage(true);
                } catch (Exception e) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("WMStateCons.MSG_RECOGNITIONSTATE_WIFI_UPDATE_SCAN_RESULT,LocatingState,e:");
                    stringBuilder.append(e.getMessage());
                    LogUtil.e(stringBuilder.toString());
                }
                return true;
            }
        }

        public void exit() {
            LogUtil.d("exit mLocatingState");
            HwWMStateMachine.this.SUBSTATE_LASTRECG = 1;
        }
    }

    class PositionState extends State {
        public final String TAG;

        PositionState() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("WMapping.");
            stringBuilder.append(PositionState.class.getSimpleName());
            this.TAG = stringBuilder.toString();
        }

        public void enter() {
            LogUtil.d("enter mPositionState");
            HwWMStateMachine.this.last_preLable = HwWMStateMachine.this.cur_preLable.normalizeCopy();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" save last results:");
            stringBuilder.append(HwWMStateMachine.this.last_preLable.toString());
            LogUtil.d(stringBuilder.toString());
            HwWMStateMachine.this.mCollectUserFingersHandler.recognizeActions(HwWMStateMachine.this.cur_preLable);
            HwWMStateMachine.this.mActiveCollectHandler.stopOut4gRecgScan();
            HwWMStateMachine.this.mLocationDAO.accCHRSpaceChangebyFreqLoc(HwWMStateMachine.this.cur_place.getPlace());
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i != 110) {
                if (i == 120) {
                    LogUtil.d("into MSG_QUERY_HISQOE, query saved history App QoE");
                    Bundle bundle = message.getData();
                    IWaveMappingCallback cb;
                    if (bundle == null || !bundle.containsKey("FULLID")) {
                        LogUtil.w(" no bundle");
                        cb = (IWaveMappingCallback) message.obj;
                        if (cb != null) {
                            cb.onWaveMappingRespondCallback(0, 0, 0, true, false);
                        }
                    } else {
                        cb = message.obj;
                        HwWMStateMachine.this.mCollectUserFingersHandler.queryAppQoebyTargetNw(HwWMStateMachine.this.cur_preLable, bundle.getInt("FULLID"), bundle.getInt("UID"), bundle.getInt("NW"), cb);
                    }
                } else if (i != 140) {
                    return false;
                } else {
                    LogUtil.d("into MSG_CHECK_4G_COVERAGE, check 4G coverage by current results");
                    HwWMStateMachine.this.mCollectUserFingersHandler.determine4gCoverage(HwWMStateMachine.this.last_preLable);
                    HwWMStateMachine.this.mCollectUserFingersHandler.checkOutOf4GCoverage(true);
                }
                return true;
            }
            LogUtil.d("into MSG_AR_MOVE, user go to other space");
            HwWMStateMachine.this.transitionTo(HwWMStateMachine.this.mLocatingState);
            return false;
        }

        public void exit() {
            LogUtil.d("exit mPositionState");
            HwWMStateMachine.this.mCollectUserFingersHandler.assignSpaceExp2Space(HwWMStateMachine.this.last_preLable);
            HwWMStateMachine.this.mCollectUserFingersHandler.checkOutOf4GCoverage(true);
            HwWMStateMachine.this.SUBSTATE_LASTRECG = 1;
        }
    }

    class RecognitionState extends State {
        public final String TAG;
        private RecognizeService mRecognizeService;

        RecognitionState() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("WMapping.");
            stringBuilder.append(RecognitionState.class.getSimpleName());
            this.TAG = stringBuilder.toString();
        }

        public void enter() {
            LogUtil.d("enter mRecognitionState");
            if (HwWMStateMachine.this.modelService != null) {
                this.mRecognizeService = new RecognizeService(HwWMStateMachine.this.modelService);
                return;
            }
            LogUtil.d("enter mLocatingState,null == modelService");
            HwWMStateMachine.this.modelService = ModelService5.getInstance(HwWMStateMachine.this.getHandler());
            this.mRecognizeService = new RecognizeService(HwWMStateMachine.this.modelService);
        }

        public boolean processMessage(Message message) {
            int i = message.what;
            if (i == 63) {
                LogUtil.d("LocatingState:MSG_WIFI_UPDATE_SCAN_RESULT, wifi scan complete");
                StringBuilder stringBuilder;
                try {
                    if (this.mRecognizeService == null) {
                        LogUtil.e(" null == recognizeService");
                    } else {
                        RecognizeResult recognizeResult = this.mRecognizeService.identifyLocation(HwWMStateMachine.this.cur_place, HwWMStateMachine.this.cur_mainApPlaceInfo, HwWMStateMachine.this.wifiList).normalizeCopy();
                        if (recognizeResult.getRgResult().contains("0")) {
                            LogUtil.d(" recognizeResult failure, result = unknonw");
                            HwWMStateMachine.this.setCur_preLable(recognizeResult);
                            UiService.getUiInfo().setToast("RecognitionState.");
                            HwWMStateMachine.this.uiService;
                            UiService.sendMsgToUi();
                            HwWMStateMachine.this.transitionTo(HwWMStateMachine.this.mLocatingState);
                        } else {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(" found the space:");
                            stringBuilder.append(recognizeResult.toString());
                            LogUtil.d(stringBuilder.toString());
                            if (HwWMStateMachine.this.cur_preLable != null && HwWMStateMachine.this.cur_preLable.cmpResults(recognizeResult)) {
                                LogUtil.i(" new space");
                                HwWMStateMachine.this.setCur_preLable(recognizeResult);
                                HwWMStateMachine.this.transitionTo(HwWMStateMachine.this.mPositionState);
                            }
                            UiService.getUiInfo().setToast("RecognitionState.");
                            UiInfo uiInfo = UiService.getUiInfo();
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append(recognizeResult.getRgResult());
                            stringBuilder2.append(Constant.RESULT_SEPERATE);
                            stringBuilder2.append(recognizeResult.getMainApRgResult());
                            uiInfo.setPreLabel(stringBuilder2.toString());
                            HwWMStateMachine.this.uiService;
                            UiService.sendMsgToUi();
                        }
                    }
                } catch (Exception e) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("WMStateCons.MSG_RECOGNITIONSTATE_WIFI_UPDATE_SCAN_RESULT,LocatingState,e:");
                    stringBuilder.append(e.getMessage());
                    LogUtil.e(stringBuilder.toString());
                }
            } else if (i != 100) {
                return false;
            } else {
                LogUtil.d("into MSG_MODEL_UNQUALIFIED, space model too old");
                if (HwWMStateMachine.this.cur_place == null || HwWMStateMachine.this.cur_place.getPlace() == null) {
                    LogUtil.e(" current location == null");
                    HwWMStateMachine.this.transitionTo(HwWMStateMachine.this.mDefaultState);
                } else {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("    current location = ");
                    stringBuilder3.append(HwWMStateMachine.this.cur_place.getPlace());
                    LogUtil.d(stringBuilder3.toString());
                    HwWMStateMachine.this.transitionTo(HwWMStateMachine.this.mCollectTrainingState);
                }
            }
            return true;
        }

        public void exit() {
            LogUtil.d("exit mRecognitionState");
            HwWMStateMachine.this.mActiveCollectHandler.stopOut4gRecgScan();
            if (HwWMStateMachine.this.getHandler().hasMessages(63)) {
                HwWMStateMachine.this.removeMessages(63);
            }
        }
    }

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(HwWMStateMachine.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    private void setCur_preLable(RecognizeResult preLable) {
        String pure_preLable = "0";
        String mainAp_preLale = "0";
        if (preLable == null) {
            LogUtil.w("setCur_preLable failure,null == cur_preLable");
            this.cur_preLable = new RecognizeResult();
            this.cur_preLable.setRgResult(pure_preLable);
            this.cur_preLable.setMainApRgResult(mainAp_preLale);
        } else {
            this.cur_preLable = preLable;
            if (preLable.getRgResult() == null) {
                LogUtil.w(" preLable.getRgResult == null");
            } else {
                pure_preLable = preLable.getRgResult();
            }
            if (preLable.getMainApRgResult() == null) {
                LogUtil.w(" preLable.getMainApRgResult == null");
            } else {
                mainAp_preLale = preLable.getMainApRgResult();
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" setCur_preLable: mainAp=");
        stringBuilder.append(mainAp_preLale);
        stringBuilder.append(", allAp=");
        stringBuilder.append(pure_preLable);
        LogUtil.d(stringBuilder.toString());
        UiInfo uiInfo = UiService.getUiInfo();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(pure_preLable);
        stringBuilder2.append(Constant.RESULT_SEPERATE);
        stringBuilder2.append(mainAp_preLale);
        uiInfo.setPreLabel(stringBuilder2.toString());
    }

    public static HwWMStateMachine getInstance(Context context) {
        if (hwWMStateMachine == null) {
            hwWMStateMachine = new HwWMStateMachine(context);
        }
        return hwWMStateMachine;
    }

    private HwWMStateMachine(Context context) {
        super("HwWMStateMachine");
        LogUtil.d("HwWMStateMachine  start..");
        this.mCtx = context;
        Context context2 = this.mCtx;
        Context context3 = this.mCtx;
        this.mWifiManager = (WifiManager) context2.getSystemService("wifi");
        addState(this.mDefaultState);
        addState(this.mFrequentLocationState, this.mDefaultState);
        addState(this.mCollectTrainingState, this.mFrequentLocationState);
        addState(this.mRecognitionState, this.mFrequentLocationState);
        addState(this.mPositionState, this.mRecognitionState);
        addState(this.mLocatingState, this.mRecognitionState);
        setInitialState(this.mDefaultState);
        start();
    }

    public Handler getStateMachineHandler() {
        return getHandler();
    }

    public void init() {
        LogUtil.d("init begin.");
        try {
            this.param = ParamManager.getInstance().getParameterInfo();
            this.uiService = new UiService(getHandler());
            UiService uiService = this.uiService;
            this.uiInfo = UiService.getUiInfo();
            this.modelService = ModelService5.getInstance(getHandler());
            this.wifiDataProvider = WifiDataProvider.getInstance(this.mCtx, getHandler());
            this.mbehaviorReceiver = new BehaviorReceiver(getHandler());
            this.mActiveCollectHandler = new ActiveCollectDecision(this.mCtx, this.mbehaviorReceiver);
            this.mCellStateMonitor = new CellStateMonitor(this.mCtx, getHandler());
            this.regularPlaceDAO = new RegularPlaceDAO();
            if (this.rgLocations == null || this.rgLocations.size() == 0) {
                this.rgLocations = this.regularPlaceDAO.findAllLocations();
                for (Entry entry : this.rgLocations.entrySet()) {
                    String key = (String) entry.getKey();
                    RegularPlaceInfo val = (RegularPlaceInfo) entry.getValue();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("rgLocations val:");
                    stringBuilder.append(val.toString());
                    LogUtil.d(stringBuilder.toString());
                }
            }
            if (!cleanExpireMobileApEnterpriseAps()) {
                Log.e(TAG, " cleanExpireMobileApEnterpriseAps failure.");
            }
            this.mCollectUserFingersHandler = CollectUserFingersHandler.getInstance(getHandler());
            this.mHwWmpCallbackImpl = HwWmpCallbackImpl.getInstance(getHandler());
            this.mAPPQoEManager = HwAPPQoEManager.createHwAPPQoEManager(this.mCtx);
            this.mAPPQoEManager.registerAppQoECallback(this.mHwWmpCallbackImpl, false);
            this.mFrequentLocation = FrequentLocation.getInstance(getHandler());
            if (!(this.mFrequentLocation == null || this.mFrequentLocation.isConnected())) {
                this.mFrequentLocation.connectService(this.mCtx);
            }
            this.initFinish = true;
            if (this.mFrequentLocation != null) {
                this.mFrequentLocation.queryFrequentLocationState();
            }
            this.mLocationDAO = new LocationDAO();
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("init exception.");
            stringBuilder2.append(e.getMessage());
            LogUtil.e(stringBuilder2.toString());
        }
    }

    public void handleShutDown() {
        sendMessage(130);
    }

    private boolean cleanExpireMobileApEnterpriseAps() {
        try {
            MobileApDAO mobileApDAO = new MobileApDAO();
            EnterpriseApDAO enterpriseApDAO = new EnterpriseApDAO();
            List<ApInfo> mobileAps = mobileApDAO.findAllAps();
            TimeUtil timeUtil = new TimeUtil();
            String updataDate = timeUtil.getSomeDay(new Date(), -30);
            if (mobileAps.size() > 0) {
                for (ApInfo apInfo : mobileAps) {
                    String lastDate = timeUtil.changeDateFormat(apInfo.getUptime());
                    if (lastDate == null || updataDate.compareTo(lastDate) > 0) {
                        mobileApDAO.remove(apInfo.getSsid(), apInfo.getMac());
                    }
                }
            }
            List<ApInfo> enterpriseAps = enterpriseApDAO.findAllAps();
            if (enterpriseAps.size() > 0) {
                for (ApInfo apInfo2 : enterpriseAps) {
                    String lastDate2 = timeUtil.changeDateFormat(apInfo2.getUptime());
                    if (lastDate2 == null || updataDate.compareTo(lastDate2) > 0) {
                        enterpriseApDAO.remove(apInfo2.getSsid());
                    }
                }
            }
            return true;
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("cleanExpireMobileApEnterpriseAps:");
            stringBuilder.append(e);
            LogUtil.e(stringBuilder.toString());
            return false;
        }
    }

    public void getCur_place(String freqLoc) {
        if (freqLoc == null) {
            LogUtil.d("getCur_place: null == curLocation");
            return;
        }
        RegularPlaceInfo placeInfo = this.regularPlaceDAO.findBySsid(freqLoc, false);
        if (placeInfo == null) {
            placeInfo = new RegularPlaceInfo(freqLoc, 3, 1, 0, 0, 0, 0, "", false);
            if (!this.regularPlaceDAO.insert(placeInfo)) {
                LogUtil.e(" insert into common current place Failure.");
            }
        }
        this.cur_place = placeInfo;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getCur_place, cur_place:");
        stringBuilder.append(this.cur_place.toString());
        LogUtil.d(stringBuilder.toString());
        RegularPlaceInfo mainApPlaceInfo = this.regularPlaceDAO.findBySsid(freqLoc, true);
        if (mainApPlaceInfo == null) {
            mainApPlaceInfo = new RegularPlaceInfo(freqLoc, 3, 1, 0, 0, 0, 0, "", true);
            if (!this.regularPlaceDAO.insert(mainApPlaceInfo)) {
                LogUtil.e(" insert into mainAp current place Failure.");
            }
        }
        this.cur_mainApPlaceInfo = mainApPlaceInfo;
        stringBuilder = new StringBuilder();
        stringBuilder.append("getCur_place,mainApPlaceInfo:");
        stringBuilder.append(this.cur_mainApPlaceInfo.toString());
        LogUtil.d(stringBuilder.toString());
        if (this.mCollectUserFingersHandler != null) {
            this.mCollectUserFingersHandler.setModelVer(this.cur_place.getModelName(), this.cur_mainApPlaceInfo.getModelName());
        }
    }

    public void trainModels(String freqLoc) {
        if (this.modelService == null) {
            LogUtil.e("trainModels null == modelService");
        } else if (freqLoc == null || freqLoc.equals("")) {
            LogUtil.e("trainModels null == freqLoc");
        } else {
            Handler modelServiceHandler = this.modelService.getmHandler();
            if (modelServiceHandler == null) {
                LogUtil.e("trainModels null == modelServiceHandler");
                return;
            }
            Bundle bundle = new Bundle();
            bundle.putString(Constant.NAME_FREQLACATION, freqLoc);
            Message buildModelMsg = Message.obtain(modelServiceHandler, 1);
            buildModelMsg.setData(bundle);
            modelServiceHandler.sendMessage(buildModelMsg);
        }
    }

    public boolean getBuildModelResult() {
        if (this.modelService == null) {
            LogUtil.e("getBuildModelResult null == modelService");
            return false;
        }
        ClusterResult result = this.modelService.getClusterResult();
        if (result == null || result.getPlace() == null || result.getPlace().equals("")) {
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" ,startTraining result:");
        stringBuilder.append(result.toString());
        LogUtil.d(stringBuilder.toString());
        UiInfo uiInfo = UiService.getUiInfo();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(result.getCluster_num());
        stringBuilder2.append(Constant.RESULT_SEPERATE);
        stringBuilder2.append(result.getMainAp_cluster_num());
        uiInfo.setCluster_num(stringBuilder2.toString());
        UiService uiService = this.uiService;
        UiService.sendMsgToUi();
        if (result.getCluster_num() <= 0) {
            LogUtil.d(" getCluster_num ,still unqualified, return to CollectTrainingState");
            return false;
        } else if (this.cur_place == null) {
            LogUtil.d(" cur_place == null, return to CollectTrainingState");
            return false;
        } else {
            getCur_place(this.cur_place.getPlace());
            UiService.getUiInfo().setStage(4);
            UiService uiService2 = this.uiService;
            UiService.sendMsgToUi();
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(" update current location success, train model OK, location :");
            stringBuilder3.append(result.getPlace());
            LogUtil.d(stringBuilder3.toString());
            return true;
        }
    }
}
