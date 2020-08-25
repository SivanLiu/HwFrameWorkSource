package com.android.server.rms.iaware.cpu;

import android.iawareperf.UniPerf;
import android.os.Bundle;
import android.rms.iaware.AwareLog;
import android.rms.iaware.IAwaredConnection;
import android.util.ArrayMap;
import com.android.server.location.HwLogRecordManager;
import com.android.server.rms.iaware.feature.SceneRecogFeature;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;

public class IAwareMode {
    private static final String BOOST_TAG = "boost";
    private static final int ENTER_GAME = 0;
    private static final int EXIT_GAME = 1;
    public static final int GAME_ENTER_TAG = 1;
    private static final String GAME_EXIT_TAG = "isGameExit";
    public static final int GAME_SCENE_TAG = 0;
    public static final int INVALID_INT = -1;
    private static final int LEVEL_EXIT = 0;
    private static final String LEVEL_TYPE_BCPU = "bcpu";
    private static final String LEVEL_TYPE_DDR = "ddr";
    private static final String LEVEL_TYPE_EAS = "eas";
    private static final String LEVEL_TYPE_GOV = "gov";
    private static final String LEVEL_TYPE_GPU = "gpu";
    private static final String LEVEL_TYPE_LCPU = "lcpu";
    private static final Object MLOCK = new Object();
    private static final String TAG = "IAwareMode";
    private static IAwareMode sInstance = null;
    /* access modifiers changed from: private */
    public boolean mEasEnable = false;
    private Map<String, LevelMng> mGameEnterLevelMngs = new ArrayMap();
    private Map<String, LevelMng> mGameSceneLevelMngs = new ArrayMap();

    private class LevelMng {
        protected Bundle mCurConfig;
        Map<Integer, LevelCmdId> mLevelMap;
        protected String mType;

        private LevelMng() {
            this.mType = "";
        }

        /* access modifiers changed from: protected */
        public void initConfig(Map<Integer, LevelCmdId> levelMap) {
            this.mLevelMap = levelMap;
        }

        /* access modifiers changed from: protected */
        public LevelInfo getLevelInfo(String str) {
            if (str == null) {
                return null;
            }
            LevelInfo levelInfo = new LevelInfo();
            if (str.contains(IAwareMode.BOOST_TAG)) {
                String[] strParts = str.split(HwLogRecordManager.VERTICAL_ESC_SEPARATE);
                if (strParts.length != 2) {
                    return null;
                }
                levelInfo.mLevel = parseInt(strParts[0]);
                levelInfo.isBoosted = true;
            } else {
                levelInfo.mLevel = parseInt(str);
            }
            return levelInfo;
        }

        /* access modifiers changed from: protected */
        public int parseInt(String intStr) {
            try {
                return Integer.parseInt(intStr);
            } catch (NumberFormatException e) {
                AwareLog.e(IAwareMode.TAG, "parse level failed:" + intStr);
                return -1;
            }
        }

        /* access modifiers changed from: protected */
        public void doConfig(Bundle bundle) {
            if (bundle == null) {
                AwareLog.w(IAwareMode.TAG, "bundle is null, type:" + this.mType);
                return;
            }
            LevelInfo levelInfo = getLevelInfo(bundle.getString(this.mType));
            if (levelInfo != null) {
                int level = levelInfo.mLevel;
                if (level == 0) {
                    Bundle bundle2 = this.mCurConfig;
                    if (bundle2 != null) {
                        applyConfig(getLevelInfo(bundle2.getString(this.mType)), false);
                        this.mCurConfig = null;
                    }
                } else if (level > 0) {
                    Bundle bundle3 = this.mCurConfig;
                    if (bundle3 != null) {
                        applyConfig(getLevelInfo(bundle3.getString(this.mType)), false);
                    }
                    applyConfig(levelInfo, true);
                    this.mCurConfig = bundle;
                } else {
                    AwareLog.e(IAwareMode.TAG, "level " + level + " is invalid for type:" + this.mType);
                }
            }
        }

        /* access modifiers changed from: protected */
        public void applyConfig(LevelInfo levelInfo, boolean enter) {
            if (levelInfo != null) {
                int cmdId = getCmdId(levelInfo.mLevel, levelInfo.isBoosted);
                int i = -1;
                if (cmdId == -1) {
                    AwareLog.w(IAwareMode.TAG, "can not find cmdId for type " + this.mType + " and level " + levelInfo.mLevel);
                    return;
                }
                AwareLog.d(IAwareMode.TAG, "game level " + levelInfo.mLevel + ",cmdid:" + cmdId + ",type:" + this.mType + ",enter:" + enter);
                UniPerf instance = UniPerf.getInstance();
                int[] iArr = new int[1];
                if (enter) {
                    i = 0;
                }
                iArr[0] = i;
                instance.uniPerfEvent(cmdId, "", iArr);
            }
        }

        /* access modifiers changed from: protected */
        public int getCmdId(int level, boolean isBoosted) {
            Map<Integer, LevelCmdId> map = this.mLevelMap;
            if (map == null) {
                AwareLog.e(IAwareMode.TAG, "levelmap is not initialized for type:" + this.mType);
                return -1;
            }
            LevelCmdId cmdId = map.get(Integer.valueOf(level));
            if (cmdId == null) {
                return -1;
            }
            return isBoosted ? cmdId.mBoostCmdId : cmdId.mLongTermCmdId;
        }
    }

    private class NormalLevelMng extends LevelMng {
        NormalLevelMng(String type) {
            super();
            this.mType = type;
        }
    }

    private class EASLevelMng extends LevelMng {
        EASLevelMng() {
            super();
            this.mType = IAwareMode.LEVEL_TYPE_EAS;
        }

        @Override // com.android.server.rms.iaware.cpu.IAwareMode.LevelMng
        public void doConfig(Bundle bundle) {
            LevelInfo levelInfo;
            int cmdId;
            if (IAwareMode.this.mEasEnable) {
                super.doConfig(bundle);
            }
            if (bundle != null && (levelInfo = getLevelInfo(bundle.getString(this.mType))) != null && levelInfo.mLevel != -1 && (cmdId = getCmdId(levelInfo.mLevel, false)) != -1) {
                int pid = bundle.getInt(SceneRecogFeature.DATA_PID, -1);
                if (pid <= 0) {
                    AwareLog.w(IAwareMode.TAG, "pid is invalid:" + pid);
                    return;
                }
                ArrayList<Integer> tids = parseTid(bundle.getString("tid"));
                int tidSize = tids.size();
                if (tidSize > 0) {
                    ByteBuffer buffer = ByteBuffer.allocate((tidSize * 4) + 20);
                    buffer.putInt(CPUFeature.MSG_GAME_SCENE_LEVEL);
                    buffer.putInt(levelInfo.mLevel);
                    buffer.putInt(cmdId);
                    buffer.putInt(pid);
                    buffer.putInt(tidSize);
                    for (int i = 0; i < tidSize; i++) {
                        buffer.putInt(tids.get(i).intValue());
                    }
                    sendToIawared(buffer);
                    AwareLog.d(IAwareMode.TAG, "game level " + levelInfo.mLevel + ",cmdid:" + cmdId + "type:" + this.mType + ",pid:" + pid + ",tid:" + tids);
                }
            }
        }

        private ArrayList<Integer> parseTid(String tidStr) {
            ArrayList<Integer> tids = new ArrayList<>();
            if (tidStr == null) {
                return tids;
            }
            for (String part : tidStr.split(HwLogRecordManager.VERTICAL_ESC_SEPARATE)) {
                int tidInt = parseInt(part);
                if (tidInt > 0) {
                    tids.add(Integer.valueOf(tidInt));
                }
            }
            return tids;
        }

        private void sendToIawared(ByteBuffer buffer) {
            if (buffer != null && !IAwaredConnection.getInstance().sendPacket(buffer.array(), 0, buffer.position())) {
                AwareLog.e(IAwareMode.TAG, "send to iawared failed");
            }
        }
    }

    public static IAwareMode getInstance() {
        IAwareMode iAwareMode;
        synchronized (MLOCK) {
            if (sInstance == null) {
                sInstance = new IAwareMode();
            }
            iAwareMode = sInstance;
        }
        return iAwareMode;
    }

    private IAwareMode() {
        this.mGameSceneLevelMngs.put(LEVEL_TYPE_BCPU, new NormalLevelMng(LEVEL_TYPE_BCPU));
        this.mGameSceneLevelMngs.put(LEVEL_TYPE_LCPU, new NormalLevelMng(LEVEL_TYPE_LCPU));
        this.mGameSceneLevelMngs.put(LEVEL_TYPE_GPU, new NormalLevelMng(LEVEL_TYPE_GPU));
        this.mGameSceneLevelMngs.put(LEVEL_TYPE_DDR, new NormalLevelMng(LEVEL_TYPE_DDR));
        this.mGameSceneLevelMngs.put(LEVEL_TYPE_GOV, new NormalLevelMng(LEVEL_TYPE_GOV));
        this.mGameSceneLevelMngs.put(LEVEL_TYPE_EAS, new EASLevelMng());
        this.mGameEnterLevelMngs.put(LEVEL_TYPE_GOV, new NormalLevelMng(LEVEL_TYPE_GOV));
    }

    public void initLevelMap(int mapTag, String type, Map<Integer, LevelCmdId> levelMap) {
        if (type == null || levelMap == null) {
            AwareLog.e(TAG, "type or levelmap is null");
        } else if (mapTag == 0) {
            LevelMng levelMng = this.mGameSceneLevelMngs.get(type);
            if (levelMng != null) {
                levelMng.initConfig(levelMap);
            }
        } else if (mapTag == 1) {
            LevelMng levelMng2 = this.mGameEnterLevelMngs.get(type);
            if (levelMng2 != null) {
                levelMng2.initConfig(levelMap);
            }
        } else {
            AwareLog.d(TAG, "invalid tag:" + mapTag);
        }
    }

    private void doLevelConfig(Map<String, LevelMng> levelMngs, Bundle bundle) {
        for (Map.Entry<String, LevelMng> entry : levelMngs.entrySet()) {
            LevelMng levelMng = entry.getValue();
            if (levelMng != null) {
                levelMng.doConfig(bundle);
            }
        }
    }

    private void doSceneLevelConfig(Bundle bundle) {
        doLevelConfig(this.mGameSceneLevelMngs, bundle);
    }

    private void doGameEnterLevelConfig(Bundle bundle) {
        doLevelConfig(this.mGameEnterLevelMngs, bundle);
    }

    public void gameLevel(Bundle bundle) {
        if (bundle == null) {
            AwareLog.e(TAG, "game level bundle is null");
            return;
        }
        AwareLog.d(TAG, "adjust game level");
        doSceneLevelConfig(bundle);
    }

    public void gameEnter(Bundle bundle, boolean isEasEnable) {
        this.mEasEnable = isEasEnable;
        if (bundle == null) {
            AwareLog.e(TAG, "game enter bundle is null");
            return;
        }
        AwareLog.d(TAG, "adjust game enter");
        doGameEnterLevelConfig(bundle);
    }

    static class LevelCmdId {
        protected int mBoostCmdId = -1;
        protected int mLongTermCmdId = -1;

        LevelCmdId() {
        }
    }

    static class LevelInfo {
        protected boolean isBoosted = false;
        protected int mLevel = -1;

        LevelInfo() {
        }
    }
}
