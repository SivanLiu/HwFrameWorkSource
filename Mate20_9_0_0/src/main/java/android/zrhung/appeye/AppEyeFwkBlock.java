package android.zrhung.appeye;

import android.os.FreezeScreenScene;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;
import android.util.Slog;
import android.util.ZRHung;
import android.util.ZRHung.HungConfig;
import android.zrhung.ZrHungData;
import android.zrhung.ZrHungImpl;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppEyeFwkBlock extends ZrHungImpl implements Runnable {
    private static final int COMPLETED = 0;
    private static final String[] INTEREST_PROCESSES = new String[]{"surfaceflinger", "android.hardware.graphics.composer@2.1-service", "android.hardware.graphics.composer@2.2-service"};
    private static final int OVERDUE = 1;
    private static final String TAG = "AppEyeFwkBlock";
    private static AppEyeFwkBlock sAppEyeFwkBlock;
    private String BINDER_TRANS_PATH = "/sys/kernel/debug/binder/transactions";
    private String HUAWEI_BINDER_TRANS_PATH = "/sys/kernel/debug/binder/transaction_proc";
    private long checkInterval;
    private boolean configReady;
    private boolean enabled;
    private boolean freezeHappened;
    private Thread mAppEyeFwkBlockThread;
    private ArrayList<HandlerChecker> mHandlerCheckers = new ArrayList();
    int mLastBlockedTid = -1;
    private HandlerChecker mMonitorChecker;
    private HandlerThread mMonitorThread;

    public final class HandlerChecker implements Runnable {
        private boolean mCompleted;
        public Object mCurrentMonitor;
        private final Handler mHandler;
        public final ArrayList<Object> mMonitors = new ArrayList();
        private final String mName;

        HandlerChecker(Handler handler, String name) {
            this.mHandler = handler;
            this.mName = name;
            this.mCompleted = true;
        }

        public void addMonitor(Object monitor) {
            this.mMonitors.add(monitor);
        }

        public void scheduleCheckLocked() {
            if (this.mMonitors.size() == 0 && this.mHandler.getLooper().getQueue().isPolling()) {
                this.mCompleted = true;
            } else if (this.mCompleted) {
                this.mCompleted = false;
                this.mCurrentMonitor = null;
                this.mHandler.postAtFrontOfQueue(this);
            }
        }

        public int getCompletionStateLocked() {
            if (this.mCompleted) {
                return 0;
            }
            return 1;
        }

        public String describeBlockedStateLocked() {
            StringBuilder stringBuilder;
            if (this.mCurrentMonitor == null) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Blocked in handler on ");
                stringBuilder.append(this.mName);
                stringBuilder.append(" (");
                stringBuilder.append(this.mHandler.getLooper().getThread().getName());
                stringBuilder.append(")");
                return stringBuilder.toString();
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Blocked in monitor ");
            stringBuilder.append(this.mCurrentMonitor.getClass().getName());
            return stringBuilder.toString();
        }

        public void doMonitor(Object monitor) {
            synchronized (monitor) {
            }
        }

        public void run() {
            int size = this.mMonitors.size();
            for (int i = 0; i < size; i++) {
                synchronized (AppEyeFwkBlock.this) {
                    this.mCurrentMonitor = this.mMonitors.get(i);
                }
                doMonitor(this.mCurrentMonitor);
            }
            synchronized (AppEyeFwkBlock.this) {
                this.mCompleted = true;
                this.mCurrentMonitor = null;
            }
        }
    }

    public static synchronized AppEyeFwkBlock getInstance() {
        AppEyeFwkBlock appEyeFwkBlock;
        synchronized (AppEyeFwkBlock.class) {
            if (sAppEyeFwkBlock == null) {
                sAppEyeFwkBlock = new AppEyeFwkBlock();
            }
            appEyeFwkBlock = sAppEyeFwkBlock;
        }
        return appEyeFwkBlock;
    }

    private AppEyeFwkBlock() {
        super(TAG);
        Slog.i(TAG, "Create AppEyeFwkBlock");
        this.mMonitorThread = new HandlerThread("monitor thread");
        this.mMonitorThread.start();
        this.mMonitorChecker = new HandlerChecker(this.mMonitorThread.getThreadHandler(), "monitor thread");
        this.mHandlerCheckers.add(this.mMonitorChecker);
        this.mAppEyeFwkBlockThread = null;
        this.configReady = false;
        this.enabled = true;
        this.checkInterval = 6000;
        this.freezeHappened = false;
    }

    private void addMonitor(Object monitor) {
        synchronized (this) {
            if (monitor != null) {
                try {
                    this.mMonitorChecker.addMonitor(monitor);
                } catch (Throwable th) {
                }
            }
        }
    }

    private int evaluateCheckerCompletionLocked() {
        int state = 0;
        int checkerSize = this.mHandlerCheckers.size();
        for (int i = 0; i < checkerSize; i++) {
            HandlerChecker hc = (HandlerChecker) this.mHandlerCheckers.get(i);
            state = state > hc.getCompletionStateLocked() ? state : hc.getCompletionStateLocked();
        }
        return state;
    }

    public boolean start(ZrHungData args) {
        if (sAppEyeFwkBlock == null) {
            return false;
        }
        this.mAppEyeFwkBlockThread = new Thread(sAppEyeFwkBlock);
        this.mAppEyeFwkBlockThread.start();
        return true;
    }

    public boolean check(ZrHungData args) {
        Object monitor = args.get("monitor");
        if (monitor != null) {
            addMonitor(monitor);
        }
        return true;
    }

    private void getConfigure() {
        if (!this.configReady) {
            HungConfig cfg = ZRHung.getHungConfig(this.mWpId);
            if (cfg == null) {
                Slog.w(TAG, "Failed to get config from zrhung");
                this.configReady = true;
            } else if (cfg.status != 0) {
                if (cfg.status == -1 || cfg.status == -2) {
                    Slog.w(TAG, "config is not support or there is no config");
                    this.configReady = true;
                    this.enabled = false;
                }
            } else if (cfg.value == null) {
                Slog.w(TAG, "Failed to get config from zrhung");
                this.configReady = true;
            } else {
                String[] configs = cfg.value.split(",");
                if (configs == null) {
                    Slog.e(TAG, "Failed to parse HungConfig");
                    this.configReady = true;
                    return;
                }
                this.enabled = configs[0].equals("1");
                if (configs.length > 1) {
                    long time = 0;
                    try {
                        time = Long.valueOf(configs[1]).longValue();
                    } catch (NumberFormatException e) {
                        Slog.w(TAG, "the config string cannot be parsed as long");
                    }
                    if (time != 0) {
                        this.checkInterval = time;
                    }
                }
                this.configReady = true;
            }
        }
    }

    private void reportFreeze() {
        try {
            int i;
            int checkerSize = this.mHandlerCheckers.size();
            StringBuilder sb = new StringBuilder();
            ZrHungData data = new ZrHungData();
            StringBuilder cmd = new StringBuilder();
            if (Log.HWINFO) {
                cmd.append("B,");
                cmd.append("p=");
                cmd.append(Process.myPid());
            }
            int i2 = 0;
            int curLockholderPid = 0;
            for (i = 0; i < checkerSize; i++) {
                HandlerChecker hc = (HandlerChecker) this.mHandlerCheckers.get(i);
                if (hc.getCompletionStateLocked() == 1) {
                    sb.append(hc.describeBlockedStateLocked());
                    sb.append("\n");
                }
                if (hc.mCurrentMonitor != null) {
                    int curBlockedTid = Thread.getLockOwnerThreadId(hc.mCurrentMonitor);
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("current locked tid is:");
                    stringBuilder.append(curBlockedTid);
                    Log.i(str, stringBuilder.toString());
                    if (this.mLastBlockedTid > 0 && this.mLastBlockedTid == curBlockedTid) {
                        curLockholderPid = catchBadproc(this.mLastBlockedTid);
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("locked pid is:");
                        stringBuilder.append(curLockholderPid);
                        Log.d(str, stringBuilder.toString());
                        data.putInt(FreezeScreenScene.PID_PARAM, curLockholderPid);
                        if (Log.HWINFO && curLockholderPid > 0) {
                            cmd.append(",");
                            cmd.append("p=");
                            cmd.append(curLockholderPid);
                        }
                        this.freezeHappened = true;
                    }
                    this.mLastBlockedTid = curBlockedTid;
                }
            }
            if (System.DEBUG) {
                i = INTEREST_PROCESSES.length;
                while (i2 < i) {
                    cmd.append(",n=");
                    cmd.append(INTEREST_PROCESSES[i2]);
                    i2++;
                }
                cmd.append(",P=");
                cmd.append(Process.myPid());
            }
            Slog.w(TAG, sb.toString());
            if (this.freezeHappened) {
                sendAppEyeEvent((short) 271, data, cmd.toString(), sb.toString());
            }
        } catch (Exception ex) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("sendAppEyeEvent exception: ");
            stringBuilder2.append(ex);
            Log.d(str2, stringBuilder2.toString());
        }
    }

    public void run() {
        while (true) {
            synchronized (this) {
                try {
                    getConfigure();
                } catch (Exception e) {
                    Slog.w(TAG, "getconfig exception");
                }
                if (this.enabled) {
                    int i;
                    int checkerSize = this.mHandlerCheckers.size();
                    for (i = 0; i < checkerSize; i++) {
                        ((HandlerChecker) this.mHandlerCheckers.get(i)).scheduleCheckLocked();
                    }
                    try {
                        wait(this.checkInterval);
                        i = evaluateCheckerCompletionLocked();
                        if (i == 0) {
                            this.freezeHappened = false;
                            this.mLastBlockedTid = -1;
                        } else if (i == 1) {
                            Slog.w(TAG, "systemserver freeze happend");
                            if (this.freezeHappened) {
                                Slog.w(TAG, "freeze happened agin, don't repeat report");
                            } else {
                                reportFreeze();
                            }
                        }
                    } catch (InterruptedException e2) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("error msg :");
                        stringBuilder.append(e2.getMessage());
                        Slog.w(str, stringBuilder.toString());
                    }
                } else {
                    Slog.w(TAG, "the function is not enabled, quit");
                    return;
                }
            }
        }
    }

    public int getLockOwnerPid(Object lock) {
        if (lock == null) {
            return -1;
        }
        int lock_holder_tid = Thread.getLockOwnerThreadId(lock);
        int pid = catchBadproc(lock_holder_tid);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("blocking in thread:");
        stringBuilder.append(lock_holder_tid);
        stringBuilder.append(" remote:");
        stringBuilder.append(pid);
        Log.i(str, stringBuilder.toString());
        return pid;
    }

    private int parseTransactionLine(String str, int tid, Pattern pattern) {
        Matcher matcher = pattern.matcher(str);
        if (!matcher.find()) {
            return -1;
        }
        String str2 = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("parseTransactionLine1 : ");
        stringBuilder.append(tid);
        stringBuilder.append(" ");
        stringBuilder.append(matcher.group(1));
        stringBuilder.append(":");
        stringBuilder.append(matcher.group(2));
        stringBuilder.append(" ");
        stringBuilder.append(matcher.group(3));
        stringBuilder.append(":");
        stringBuilder.append(matcher.group(4));
        Slog.i(str2, stringBuilder.toString());
        if (tid == Integer.parseInt(matcher.group(2))) {
            return Integer.parseInt(matcher.group(3));
        }
        return -1;
    }

    public int catchBadproc(int tid) {
        BufferedReader buff = null;
        File file = new File(this.HUAWEI_BINDER_TRANS_PATH);
        int index = 0;
        int ret = -1;
        boolean isHwTransLog = true;
        if (!file.exists()) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("file not exists : ");
            stringBuilder.append(this.HUAWEI_BINDER_TRANS_PATH);
            Slog.w(str, stringBuilder.toString());
            file = new File(this.BINDER_TRANS_PATH);
            isHwTransLog = false;
            if (!file.exists()) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("file not exists : ");
                stringBuilder.append(this.BINDER_TRANS_PATH);
                Slog.w(str, stringBuilder.toString());
                return -1;
            }
        }
        try {
            buff = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            String regEx = "[\\s\\S]*\t(\\d+):(\\d+) to (\\d+):(\\d+) code ([0-9a-f]+) wait:(\\d+).(\\d+) s";
            if (!isHwTransLog) {
                regEx = "outgoing transaction .+ from (\\d+):(\\d+) to (\\d+):(\\d+) code ([0-9a-f]+)";
            }
            Pattern pattern = Pattern.compile(regEx);
            String readLine = buff.readLine();
            while (readLine != null) {
                ret = parseTransactionLine(readLine, tid, pattern);
                if (ret > 0) {
                    try {
                        buff.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return ret;
                }
                readLine = buff.readLine();
                index++;
            }
            try {
                buff.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        } catch (FileNotFoundException e3) {
            e3.printStackTrace();
            if (buff != null) {
                buff.close();
            }
        } catch (IOException e4) {
            e4.printStackTrace();
            if (buff != null) {
                try {
                    buff.close();
                } catch (IOException e42) {
                    e42.printStackTrace();
                }
            }
        } catch (Throwable th) {
            if (buff != null) {
                try {
                    buff.close();
                } catch (IOException e22) {
                    e22.printStackTrace();
                }
            }
        }
        return ret;
    }
}
