package com.android.server.rms.iaware.cpu;

import android.iawareperf.UniPerf;
import android.os.Bundle;
import android.rms.iaware.AwareLog;
import android.rms.iaware.IAwaredConnection;
import android.util.ArrayMap;
import com.android.server.location.HwLogRecordManager;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

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
    private static final String TAG = "IAwareMode";
    private static final Object mLock = new Object();
    private static IAwareMode sInstance = null;
    private boolean mEasEnable = false;
    private Map<String, LevelMng> mGameEnterLevelMngs = new ArrayMap();
    private Map<String, LevelMng> mGameSceneLevelMngs = new ArrayMap();

    public static class LevelCmdId {
        public int mBoostCmdId = -1;
        public int mLongTermCmdId = -1;
    }

    private static class LevelInfo {
        public boolean isBoosted;
        public int mLevel;

        private LevelInfo() {
            this.mLevel = -1;
            this.isBoosted = false;
        }
    }

    private class LevelMng {
        protected Bundle mCurConfig;
        Map<Integer, LevelCmdId> mLevelMap;
        protected String mType;

        private LevelMng() {
            this.mType = "";
        }

        protected void initConfig(Map<Integer, LevelCmdId> levelMap) {
            this.mLevelMap = levelMap;
        }

        protected LevelInfo getLevelInfo(String str) {
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

        protected int parseInt(String intStr) {
            try {
                return Integer.parseInt(intStr);
            } catch (NumberFormatException e) {
                String str = IAwareMode.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("parse level failed:");
                stringBuilder.append(intStr);
                AwareLog.e(str, stringBuilder.toString());
                return -1;
            }
        }

        protected void doConfig(Bundle bundle) {
            if (bundle == null) {
                String str = IAwareMode.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("bundle is null, type:");
                stringBuilder.append(this.mType);
                AwareLog.w(str, stringBuilder.toString());
                return;
            }
            LevelInfo levelInfo = getLevelInfo(bundle.getString(this.mType));
            if (levelInfo != null) {
                int level = levelInfo.mLevel;
                if (level == 0) {
                    if (this.mCurConfig != null) {
                        applyConfig(getLevelInfo(this.mCurConfig.getString(this.mType)), false);
                        this.mCurConfig = null;
                    }
                } else if (level > 0) {
                    if (this.mCurConfig != null) {
                        applyConfig(getLevelInfo(this.mCurConfig.getString(this.mType)), false);
                    }
                    applyConfig(levelInfo, true);
                    this.mCurConfig = bundle;
                } else {
                    String str2 = IAwareMode.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("level ");
                    stringBuilder2.append(level);
                    stringBuilder2.append(" is invalid for type:");
                    stringBuilder2.append(this.mType);
                    AwareLog.e(str2, stringBuilder2.toString());
                }
            }
        }

        protected void applyConfig(LevelInfo levelInfo, boolean enter) {
            if (levelInfo != null) {
                int cmdId = getCmdId(levelInfo.mLevel, levelInfo.isBoosted);
                int i = -1;
                if (cmdId == -1) {
                    String str = IAwareMode.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("can not find cmdId for type ");
                    stringBuilder.append(this.mType);
                    stringBuilder.append(" and level ");
                    stringBuilder.append(levelInfo.mLevel);
                    AwareLog.w(str, stringBuilder.toString());
                    return;
                }
                String str2 = IAwareMode.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("game level ");
                stringBuilder2.append(levelInfo.mLevel);
                stringBuilder2.append(",cmdid:");
                stringBuilder2.append(cmdId);
                stringBuilder2.append(",type:");
                stringBuilder2.append(this.mType);
                stringBuilder2.append(",enter:");
                stringBuilder2.append(enter);
                AwareLog.d(str2, stringBuilder2.toString());
                UniPerf instance = UniPerf.getInstance();
                String str3 = "";
                int[] iArr = new int[1];
                if (enter) {
                    i = 0;
                }
                iArr[0] = i;
                instance.uniPerfEvent(cmdId, str3, iArr);
            }
        }

        protected int getCmdId(int level, boolean isBoosted) {
            int i = -1;
            if (this.mLevelMap == null) {
                String str = IAwareMode.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("levelmap is not initialized for type:");
                stringBuilder.append(this.mType);
                AwareLog.e(str, stringBuilder.toString());
                return -1;
            }
            LevelCmdId cmdId = (LevelCmdId) this.mLevelMap.get(Integer.valueOf(level));
            if (cmdId != null) {
                i = isBoosted ? cmdId.mBoostCmdId : cmdId.mLongTermCmdId;
            }
            return i;
        }
    }

    private class BigCPULevelMng extends LevelMng {
        public BigCPULevelMng() {
            super();
            this.mType = IAwareMode.LEVEL_TYPE_BCPU;
        }
    }

    private class DDRLevelMng extends LevelMng {
        public DDRLevelMng() {
            super();
            this.mType = IAwareMode.LEVEL_TYPE_DDR;
        }
    }

    private class EASLevelMng extends LevelMng {
        public EASLevelMng() {
            super();
            this.mType = IAwareMode.LEVEL_TYPE_EAS;
        }

        public void doConfig(Bundle bundle) {
            if (IAwareMode.this.mEasEnable) {
                super.doConfig(bundle);
            }
            String levelStr = bundle.getString(this.mType);
            LevelInfo levelInfo = getLevelInfo(levelStr);
            if (levelStr != null) {
                if (levelInfo.mLevel == 0 && SchedLevelBoost.getInstance().mIsLcpuLimited.get()) {
                    if (bundle.getInt(IAwareMode.GAME_EXIT_TAG, -1) == 0) {
                        AwareLog.w(IAwareMode.TAG, "proc level0 but little cpu is limited");
                        SchedLevelBoost.getInstance().enterSchedLevelBoost();
                    }
                    return;
                }
                if (levelInfo.mLevel != -1) {
                    int i = 0;
                    int cmdId = getCmdId(levelInfo.mLevel, false);
                    if (cmdId != -1) {
                        int pid = bundle.getInt("pid", -1);
                        String str;
                        if (pid <= 0) {
                            str = IAwareMode.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("pid is invalid:");
                            stringBuilder.append(pid);
                            AwareLog.w(str, stringBuilder.toString());
                            return;
                        }
                        ArrayList<Integer> tids = parseTid(bundle.getString("tid"));
                        if (tids != null) {
                            int tidSize = tids.size();
                            ByteBuffer buffer = ByteBuffer.allocate(20 + (tidSize * 4));
                            buffer.putInt(CPUFeature.MSG_GAME_SCENE_LEVEL);
                            buffer.putInt(levelInfo.mLevel);
                            buffer.putInt(cmdId);
                            buffer.putInt(pid);
                            buffer.putInt(tidSize);
                            while (i < tidSize) {
                                buffer.putInt(((Integer) tids.get(i)).intValue());
                                i++;
                            }
                            sendToIawared(buffer);
                            str = IAwareMode.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("game level ");
                            stringBuilder2.append(levelInfo.mLevel);
                            stringBuilder2.append(",cmdid:");
                            stringBuilder2.append(cmdId);
                            stringBuilder2.append("type:");
                            stringBuilder2.append(this.mType);
                            stringBuilder2.append(",pid:");
                            stringBuilder2.append(pid);
                            stringBuilder2.append(",tid:");
                            stringBuilder2.append(tids);
                            AwareLog.d(str, stringBuilder2.toString());
                        }
                    }
                }
            }
        }

        private ArrayList<Integer> parseTid(String tidStr) {
            if (tidStr == null) {
                return null;
            }
            ArrayList<Integer> tids = new ArrayList();
            for (String part : tidStr.split(HwLogRecordManager.VERTICAL_ESC_SEPARATE)) {
                int tidInt = parseInt(part);
                if (tidInt > 0) {
                    tids.add(Integer.valueOf(tidInt));
                }
            }
            return tids;
        }

        private void sendToIawared(ByteBuffer buffer) {
            if (!(buffer == null || IAwaredConnection.getInstance().sendPacket(buffer.array(), 0, buffer.position()))) {
                AwareLog.e(IAwareMode.TAG, "send to iawared failed");
            }
        }
    }

    private class GPULevelMng extends LevelMng {
        public GPULevelMng() {
            super();
            this.mType = IAwareMode.LEVEL_TYPE_GPU;
        }
    }

    private class GovLevelMng extends LevelMng {
        public GovLevelMng() {
            super();
            this.mType = IAwareMode.LEVEL_TYPE_GOV;
        }
    }

    private class LittleCPULevelMng extends LevelMng {
        public LittleCPULevelMng() {
            super();
            this.mType = IAwareMode.LEVEL_TYPE_LCPU;
        }
    }

    public static IAwareMode getInstance() {
        IAwareMode iAwareMode;
        synchronized (mLock) {
            if (sInstance == null) {
                sInstance = new IAwareMode();
            }
            iAwareMode = sInstance;
        }
        return iAwareMode;
    }

    private IAwareMode() {
        this.mGameSceneLevelMngs.put(LEVEL_TYPE_BCPU, new BigCPULevelMng());
        this.mGameSceneLevelMngs.put(LEVEL_TYPE_LCPU, new LittleCPULevelMng());
        this.mGameSceneLevelMngs.put(LEVEL_TYPE_GPU, new GPULevelMng());
        this.mGameSceneLevelMngs.put(LEVEL_TYPE_DDR, new DDRLevelMng());
        this.mGameSceneLevelMngs.put(LEVEL_TYPE_GOV, new GovLevelMng());
        this.mGameSceneLevelMngs.put(LEVEL_TYPE_EAS, new EASLevelMng());
        this.mGameEnterLevelMngs.put(LEVEL_TYPE_GOV, new GovLevelMng());
    }

    void enable(int mode) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("enable curmode=");
        stringBuilder.append(mode);
        AwareLog.d(str, stringBuilder.toString());
    }

    void modeChange(int mode) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("modeChange curmode=");
        stringBuilder.append(mode);
        AwareLog.d(str, stringBuilder.toString());
    }

    public void initLevelMap(int mapTag, String type, Map<Integer, LevelCmdId> levelMap) {
        if (type == null || levelMap == null) {
            AwareLog.e(TAG, "type or levelmap is null");
            return;
        }
        LevelMng levelMng;
        if (mapTag == 0) {
            levelMng = (LevelMng) this.mGameSceneLevelMngs.get(type);
            if (levelMng != null) {
                levelMng.initConfig(levelMap);
            }
        } else if (mapTag == 1) {
            levelMng = (LevelMng) this.mGameEnterLevelMngs.get(type);
            if (levelMng != null) {
                levelMng.initConfig(levelMap);
            }
        }
    }

    private void doLevelConfig(Map<String, LevelMng> levelMngs, Bundle bundle) {
        for (Entry<String, LevelMng> entry : levelMngs.entrySet()) {
            LevelMng levelMng = (LevelMng) entry.getValue();
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
}
