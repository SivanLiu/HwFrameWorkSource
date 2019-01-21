package android.zrhung.appeye;

import android.os.FreezeScreenScene;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Slog;
import android.util.SparseIntArray;
import android.util.ZRHung;
import android.util.ZRHung.HungConfig;
import android.zrhung.ZrHungData;
import android.zrhung.ZrHungImpl;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import libcore.io.IoUtils;

public class AppEyeBinderBlock extends ZrHungImpl {
    public static final String CONSTANTPATH = "/sys/kernel/debug/binder/proc/";
    private static final int MAX_BINDER_CALL_DEPTH = 2;
    public static final int PROCESS_ERROR = -1;
    public static final int PROCESS_IS_NATIVE = 1;
    public static final int PROCESS_NOT_NATIVE = 0;
    static final String TAG = "AppEyeBinderBlock";
    private static AppEyeBinderBlock mInstance;
    private static final Object mLock = new Object();
    private static final SparseIntArray mPidMap = new SparseIntArray();
    private String BINDER_TRANS_PATH = "/sys/kernel/debug/binder/transactions";
    private String HUAWEI_BINDER_TRANS_PATH = "/sys/kernel/debug/binder/transaction_proc";
    private int mBlockSourcePid = -1;
    private boolean mConfiged = false;
    private boolean mEnableMinimizeDumpList = false;
    private HashMap<Integer, Set<Integer>> mExpiredBinderPidLists = new HashMap();
    private HandlerThread mHanderThread = new HandlerThread("writingThread");
    private ArrayList<String> mInterestedNativeStack = new ArrayList();
    private StringBuffer sb = new StringBuffer();

    public class ReadTransactionThread implements Runnable {
        public void run() {
            AppEyeBinderBlock.this.readTransaction();
        }
    }

    private AppEyeBinderBlock(String wpName) {
        super(wpName);
        this.mHanderThread.start();
    }

    public static AppEyeBinderBlock getInstance(String wpName) {
        if (mInstance == null) {
            synchronized (mLock) {
                if (mInstance == null) {
                    mInstance = new AppEyeBinderBlock(wpName);
                }
            }
        }
        return mInstance;
    }

    private void getConfigure() {
        if (!this.mConfiged) {
            HungConfig cfg = ZRHung.getHungConfig((short) 267);
            if (cfg == null) {
                Slog.w(TAG, "Failed to get config from zrhung");
                this.mConfiged = true;
            } else if (cfg.status != 0) {
                if (cfg.status == -1 || cfg.status == -2) {
                    Slog.w(TAG, "config is not support or there is no config");
                    this.mConfiged = true;
                    this.mEnableMinimizeDumpList = false;
                }
            } else if (cfg.value == null) {
                Slog.w(TAG, "Failed to get config from zrhung");
                this.mConfiged = true;
            } else {
                String[] configs = cfg.value.split(",");
                if (configs.length < 1) {
                    Slog.e(TAG, "Wrong Config size");
                    this.mConfiged = true;
                    return;
                }
                this.mEnableMinimizeDumpList = configs[0].equals("1");
                this.mConfiged = true;
                initInterestedNativeProcessList(configs);
            }
        }
    }

    private void initInterestedNativeProcessList(String[] configs) {
        if (configs != null && configs.length >= 2) {
            for (int i = 1; i < configs.length; i++) {
                this.mInterestedNativeStack.add(configs[i]);
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:26:0x0059  */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0055  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0049  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x0059  */
    /* JADX WARNING: Removed duplicated region for block: B:25:0x0055  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0049  */
    /* JADX WARNING: Missing block: B:16:0x0037, code skipped:
            if (r1.equals("addBinderPid") != false) goto L_0x0045;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean check(ZrHungData args) {
        boolean z = false;
        if (args == null) {
            return false;
        }
        if (!this.mConfiged) {
            getConfigure();
        }
        String method = args.getString("method");
        int hashCode = method.hashCode();
        if (hashCode != -1394102005) {
            if (hashCode != -1062463520) {
                if (hashCode == 1366434877 && method.equals("readTransactionInSubThread")) {
                    z = true;
                    switch (z) {
                        case false:
                            ArrayList<Integer> notNativeList = args.getIntegerArrayList("notnativepids");
                            ArrayList<Integer> nativeList = args.getIntegerArrayList("nativepids");
                            hashCode = args.getInt(FreezeScreenScene.PID_PARAM);
                            int tid = args.getInt("tid");
                            this.mBlockSourcePid = hashCode;
                            clearDumpStackPidListIfNeeded(notNativeList, nativeList, hashCode, tid, this.mEnableMinimizeDumpList);
                            addBinderPid(notNativeList, nativeList, hashCode, tid);
                            break;
                        case true:
                            readTransactionInSubThread();
                            break;
                        case true:
                            String path = args.getString("path");
                            if (path != null) {
                                writeTransactionToTrace(path);
                                break;
                            }
                            break;
                    }
                    return true;
                }
            }
        } else if (method.equals("writeTransactionToTrace")) {
            z = true;
            switch (z) {
                case false:
                    break;
                case true:
                    break;
                case true:
                    break;
            }
            return true;
        }
        z = true;
        switch (z) {
            case false:
                break;
            case true:
                break;
            case true:
                break;
        }
        return true;
    }

    private void clearDumpStackPidListIfNeeded(ArrayList<Integer> notNativeList, ArrayList<Integer> nativeList, int pid, int tid, boolean isNeedClearList) {
        if (pid != Process.myPid()) {
            if (isNeedClearList) {
                if (nativeList != null) {
                    nativeList.clear();
                    updateNativeDumpStackPidList(nativeList);
                }
                if (notNativeList != null) {
                    notNativeList.clear();
                    notNativeList.add(Integer.valueOf(pid));
                    notNativeList.add(Integer.valueOf(Process.myPid()));
                }
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" isNeedClearDumpBackStackTracePidList:");
            stringBuilder.append(isNeedClearList);
            Slog.i(str, stringBuilder.toString());
        }
    }

    private void updateNativeDumpStackPidList(ArrayList<Integer> nativeList) {
        String[] nativeProcs = new String[this.mInterestedNativeStack.size()];
        this.mInterestedNativeStack.toArray(nativeProcs);
        int[] pidList = Process.getPidsForCommands(nativeProcs);
        if (pidList != null) {
            for (int pid : pidList) {
                nativeList.add(Integer.valueOf(pid));
            }
        }
    }

    public void addBinderPid(ArrayList<Integer> notNativeList, ArrayList<Integer> nativeList, int pid, int tid) {
        try {
            ArrayList<Integer> serverPidList = new ArrayList();
            serverPidList.addAll(getIndirectBlockedBinderPidList(pid));
            int length = serverPidList.size();
            for (int i = 0; i < length; i++) {
                if (isNativeProcess(((Integer) serverPidList.get(i)).intValue()) == 1) {
                    nativeList.add((Integer) serverPidList.get(i));
                } else if (isNativeProcess(((Integer) serverPidList.get(i)).intValue()) == 0) {
                    notNativeList.add((Integer) serverPidList.get(i));
                }
            }
            removeReduntantPids(notNativeList);
            removeReduntantPids(nativeList);
        } catch (Exception ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("exception info ex:");
            stringBuilder.append(ex);
            Slog.d(str, stringBuilder.toString());
        }
    }

    private Set<Integer> getIndirectBlockedBinderPidList(int pid) {
        Set<Integer> blockedBinderPidList = new HashSet();
        this.mExpiredBinderPidLists.clear();
        parseTransactionLogs(pid);
        blockedBinderPidList.add(Integer.valueOf(pid));
        for (int i = 0; i < 2; i++) {
            for (Integer pid_from : new HashSet(blockedBinderPidList)) {
                Set<Integer> pids = (Set) this.mExpiredBinderPidLists.get(Integer.valueOf(pid_from.intValue()));
                if (pids != null) {
                    blockedBinderPidList.addAll(pids);
                }
            }
        }
        blockedBinderPidList.remove(Integer.valueOf(pid));
        blockedBinderPidList.remove(Integer.valueOf(Process.myPid()));
        return blockedBinderPidList;
    }

    private void parseTransactionLogs(int pid) {
        String str;
        IOException iOException;
        IOException e;
        StringBuilder stringBuilder;
        IOException iOException2;
        InputStream in = null;
        Reader reader = null;
        String regEx = "";
        if (this.mExpiredBinderPidLists.size() == 0) {
        }
        String regEx2 = null;
        File file = new File(this.HUAWEI_BINDER_TRANS_PATH);
        if (file.exists()) {
            regEx = "([\\s\\S]*)\t(\\d+):(\\d+) to (\\d+):(\\d+) code ([0-9a-f]+) wait:(\\d+).(\\d+) s";
            regEx2 = true;
        } else {
            file = new File(this.BINDER_TRANS_PATH);
            if (file.exists()) {
                regEx = "(outgoing) transaction .+ from (\\d+):(\\d+) to (\\d+):(\\d+) code ([0-9a-f]+) .+";
            } else {
                str = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("file not exists : ");
                stringBuilder2.append(this.BINDER_TRANS_PATH);
                Slog.w(str, stringBuilder2.toString());
                return;
            }
        }
        boolean isHuaweiLog = regEx2;
        regEx2 = regEx;
        try {
            in = new FileInputStream(file);
            reader = new InputStreamReader(in, "UTF-8");
            BufferedReader buff = new BufferedReader(reader);
            regEx = buff.readLine();
            Pattern pattern = Pattern.compile(regEx2);
            while (regEx != null) {
                Matcher matcher = pattern.matcher(regEx);
                String from_pid = null;
                String to_pid = null;
                String costTime = null;
                if (matcher.find()) {
                    from_pid = matcher.group(2);
                    to_pid = matcher.group(4);
                    if (isHuaweiLog) {
                        costTime = matcher.group(7);
                    }
                }
                if (from_pid == null) {
                } else if (to_pid == null) {
                    String str2 = regEx;
                } else {
                    int fromPid = Integer.parseInt(from_pid);
                    int toPid = Integer.parseInt(to_pid);
                    if (isHuaweiLog && costTime != null) {
                        int time_cost = Integer.parseInt(costTime);
                        if (this.mBlockSourcePid != fromPid) {
                            if (time_cost < 1) {
                            }
                            updateExpiredBinderPidList(fromPid, toPid);
                        }
                    }
                    updateExpiredBinderPidList(fromPid, toPid);
                }
                regEx = buff.readLine();
            }
            try {
                reader.close();
            } catch (IOException e2) {
                iOException = e2;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("exception info e:");
                stringBuilder.append(e2);
                Slog.e(str, stringBuilder.toString());
            }
            try {
                in.close();
            } catch (IOException e3) {
                e2 = e3;
                iOException = e2;
                str = TAG;
                stringBuilder = new StringBuilder();
            }
        } catch (Exception e4) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("exception info e:");
            stringBuilder.append(e4);
            Slog.e(str, stringBuilder.toString());
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e22) {
                    iOException = e22;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("exception info e:");
                    stringBuilder.append(e22);
                    Slog.e(str, stringBuilder.toString());
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e5) {
                    e22 = e5;
                    iOException = e22;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (Throwable th) {
            BufferedReader bufferedReader = null;
            Throwable buff2 = th;
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e222) {
                    iOException2 = e222;
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("exception info e:");
                    stringBuilder3.append(e222);
                    Slog.e(str3, stringBuilder3.toString());
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e2222) {
                    iOException2 = e2222;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("exception info e:");
                    stringBuilder.append(e2222);
                    Slog.e(TAG, stringBuilder.toString());
                }
            }
        }
        stringBuilder.append("exception info e:");
        stringBuilder.append(e2222);
        Slog.e(str, stringBuilder.toString());
    }

    public static int isNativeProcess(int pid) {
        if (pid <= 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("pid less than 0, pid is ");
            stringBuilder.append(pid);
            Slog.w(str, stringBuilder.toString());
            return -1;
        }
        int fatherPid = getFatherPid(pid);
        if (fatherPid <= 0 || !isZygoteProcess(fatherPid)) {
            return 1;
        }
        return 0;
    }

    private static int getFatherPid(int pid) {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("/proc/");
            stringBuilder.append(pid);
            stringBuilder.append("/stat");
            String lineString = IoUtils.readFileAsString(stringBuilder.toString());
            if (lineString == null) {
                return -1;
            }
            int beginNumber = lineString.indexOf(")") + 4;
            return Integer.parseInt(lineString.substring(beginNumber, lineString.indexOf(" ", beginNumber)));
        } catch (IOException ex) {
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("exception info e:");
            stringBuilder2.append(ex);
            Slog.e(str, stringBuilder2.toString());
            return -1;
        }
    }

    private static boolean isZygoteProcess(int pid) {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("/proc/");
            stringBuilder.append(pid);
            stringBuilder.append("/stat");
            String lineString = IoUtils.readFileAsString(stringBuilder.toString());
            if (lineString != null) {
                int beginNumber = lineString.indexOf("(") + 1;
                if (lineString.substring(beginNumber, lineString.indexOf(")", beginNumber)).equals("main")) {
                    return true;
                }
            }
        } catch (IOException ex) {
            String str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("exception info e:");
            stringBuilder2.append(ex);
            Slog.e(str, stringBuilder2.toString());
        }
        return false;
    }

    public static String readProcName(String pid) {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("/proc/");
            stringBuilder.append(pid);
            stringBuilder.append("/comm");
            String content = IoUtils.readFileAsString(stringBuilder.toString());
            if (content != null) {
                String[] segments = content.split("\n");
                if (segments.length > 0 && segments[0].trim().length() > 0) {
                    return segments[0];
                }
            }
            return "unknown";
        } catch (IOException e) {
            return "unknown";
        }
    }

    private void updateExpiredBinderPidList(int fromPid, int toPid) {
        Set<Integer> set = (Set) this.mExpiredBinderPidLists.get(Integer.valueOf(fromPid));
        if (set == null) {
            set = new HashSet();
        }
        set.add(Integer.valueOf(toPid));
        this.mExpiredBinderPidLists.put(Integer.valueOf(fromPid), set);
    }

    private static String getBlockedBinderInfo(String str, SparseIntArray PidMap) {
        Matcher matcher = Pattern.compile("([\\s\\S]*)\t(\\d+):(\\d+) to (\\d+):(\\d+) code ([0-9a-f]+) wait:(\\d+).(\\d+) s").matcher(str);
        if (!matcher.find()) {
            return null;
        }
        String stringBuilder;
        String from_pid = matcher.group(2);
        String from_tid = matcher.group(3);
        String to_pid = matcher.group(4);
        String to_tid = matcher.group(5);
        if (PidMap != null) {
            PidMap.put(Integer.parseInt(from_pid), Integer.parseInt(from_tid));
            PidMap.put(Integer.parseInt(to_pid), Integer.parseInt(to_tid));
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        if (matcher.group(1).length() > 0) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("[");
            stringBuilder3.append(matcher.group(1));
            stringBuilder3.append("]\t");
            stringBuilder = stringBuilder3.toString();
        } else {
            stringBuilder = "\t";
        }
        stringBuilder2.append(stringBuilder);
        stringBuilder2.append(from_pid);
        stringBuilder2.append(":");
        stringBuilder2.append(from_tid);
        stringBuilder2.append("(");
        stringBuilder2.append(readProcName(from_pid));
        stringBuilder2.append(":");
        stringBuilder2.append(readProcName(from_tid));
        stringBuilder2.append(") -> ");
        stringBuilder2.append(to_pid);
        stringBuilder2.append(":");
        stringBuilder2.append(to_tid);
        stringBuilder2.append("(");
        stringBuilder2.append(readProcName(to_pid));
        stringBuilder2.append(":");
        stringBuilder2.append(readProcName(to_tid));
        stringBuilder2.append(") code ");
        stringBuilder2.append(matcher.group(6));
        stringBuilder2.append(" wait:");
        stringBuilder2.append(matcher.group(7));
        stringBuilder2.append(".");
        stringBuilder2.append(matcher.group(8));
        stringBuilder2.append(" s\n");
        return stringBuilder2.toString();
    }

    private static String getHuaweiBinderProcInfo(String str) {
        Matcher matcher = Pattern.compile("(\\d+)\t([\\s\\S]*)\t(\\d+)\t(\\d+)\t(\\d+)\t(\\d+)\t(\\d+)").matcher(str);
        String Suffix = "\t< -- ";
        boolean needSuffix = false;
        if (!matcher.find()) {
            return null;
        }
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        String str2;
        int pid = Integer.parseInt(matcher.group(1));
        String context = matcher.group(2);
        int requested_threads = Integer.parseInt(matcher.group(3));
        int requested_threads_started = Integer.parseInt(matcher.group(4));
        int max_threads = Integer.parseInt(matcher.group(5));
        int ready_threads = Integer.parseInt(matcher.group(6));
        int free_async_space = Integer.parseInt(matcher.group(7));
        if (ready_threads + requested_threads == 0 && requested_threads_started >= max_threads && max_threads != 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(Suffix);
            stringBuilder.append("no binder thread");
            Suffix = stringBuilder.toString();
            needSuffix = true;
        }
        if (free_async_space < 102400) {
            if (needSuffix) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(Suffix);
                stringBuilder3.append(" & ");
                Suffix = stringBuilder3.toString();
            }
            needSuffix = true;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(Suffix);
            stringBuilder2.append("binder memory < 100KB");
            Suffix = stringBuilder2.toString();
        }
        if (mPidMap.get(pid, -1) < 0 && free_async_space >= 102400) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(pid);
            stringBuilder2.append("");
            if (!readProcName(stringBuilder2.toString()).equals("system_server")) {
                return null;
            }
        }
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(pid);
        stringBuilder2.append("\t");
        if (context.length() >= 8) {
            str2 = context;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append(context);
            stringBuilder.append("\t");
            str2 = stringBuilder.toString();
        }
        stringBuilder2.append(str2);
        stringBuilder2.append("\t");
        stringBuilder2.append(max_threads);
        stringBuilder2.append("\t\t");
        stringBuilder2.append(ready_threads);
        stringBuilder2.append("\t\t");
        stringBuilder2.append(requested_threads);
        stringBuilder2.append("\t\t\t");
        stringBuilder2.append(requested_threads_started);
        stringBuilder2.append("\t\t\t\t\t");
        stringBuilder2.append(free_async_space);
        stringBuilder2.append(needSuffix ? Suffix : "");
        stringBuilder2.append("\n");
        return stringBuilder2.toString();
    }

    private static String getTransactionLine(String str) {
        String regEx = "outgoing transaction .+ from (\\d+):(\\d+) to (\\d+):(\\d+) code ([0-9a-f]+) .+";
        Matcher matcher = Pattern.compile("outgoing transaction .+ from (\\d+):(\\d+) to (\\d+):(\\d+) code ([0-9a-f]+) .+").matcher(str);
        if (!matcher.find()) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\t");
        stringBuilder.append(matcher.group(1));
        stringBuilder.append(":");
        stringBuilder.append(matcher.group(2));
        stringBuilder.append("(");
        stringBuilder.append(readProcName(matcher.group(1)));
        stringBuilder.append(":");
        stringBuilder.append(readProcName(matcher.group(2)));
        stringBuilder.append(") -> ");
        stringBuilder.append(matcher.group(3));
        stringBuilder.append(":");
        stringBuilder.append(matcher.group(4));
        stringBuilder.append("(");
        stringBuilder.append(readProcName(matcher.group(3)));
        stringBuilder.append(":");
        stringBuilder.append(readProcName(matcher.group(4)));
        stringBuilder.append(") code: ");
        stringBuilder.append(matcher.group(5));
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }

    public void readTransactionInSubThread() {
        try {
            this.mHanderThread.getThreadHandler().post(new ReadTransactionThread());
        } catch (Exception ex) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("exception info:");
            stringBuilder.append(ex);
            Slog.d(str, stringBuilder.toString());
        }
    }

    public void writeTransactionToTrace(String tracesPath) {
        IOException e;
        String str;
        StringBuilder stringBuilder;
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(tracesPath, true);
            out.write(this.sb.toString().getBytes("UTF-8"));
            try {
                out.close();
            } catch (IOException e2) {
                e = e2;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("exception info e:");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            }
        } catch (Exception ex) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Exception is:");
            stringBuilder.append(ex);
            Slog.e(str, stringBuilder.toString());
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e3) {
                    e = e3;
                    str = TAG;
                    stringBuilder = new StringBuilder();
                }
            }
        } catch (Throwable th) {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e4) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("exception info e:");
                    stringBuilder.append(e4);
                    Slog.e(TAG, stringBuilder.toString());
                }
            }
        }
    }

    public void readTransaction() {
        BufferedReader buff_ht;
        StringBuilder stringBuilder;
        String str;
        StringBuilder stringBuilder2;
        IOException iOException;
        BufferedReader bufferedReader;
        IOException iOException2;
        String str2;
        StringBuilder stringBuilder3;
        Slog.d(TAG, "read binder transaction begin");
        this.sb.setLength(0);
        int index = 0;
        boolean huaweiTransactionFileExist = true;
        mPidMap.clear();
        this.sb.append("\n----- binder transactions -----\n");
        SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyy-MM-dd  HH:mm:ss");
        String date = new StringBuilder();
        date.append(sDateFormat.format(new Date()));
        date.append("\n");
        this.sb.append(date.toString());
        File file_huawei_transaction = new File(this.HUAWEI_BINDER_TRANS_PATH);
        if (!file_huawei_transaction.exists()) {
            String str3 = TAG;
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("file not exists : ");
            stringBuilder4.append(this.HUAWEI_BINDER_TRANS_PATH);
            Slog.w(str3, stringBuilder4.toString());
            huaweiTransactionFileExist = false;
        }
        String str4;
        if (huaweiTransactionFileExist) {
            InputStream in_ht = null;
            Reader reader_ht = null;
            str4 = null;
            BufferedReader buff_ht2 = null;
            String readLine_ht;
            try {
                in_ht = new FileInputStream(file_huawei_transaction);
                reader_ht = new InputStreamReader(in_ht, "UTF-8");
                buff_ht2 = new BufferedReader(reader_ht);
                readLine_ht = buff_ht2.readLine();
                boolean hasPringTableTitle = false;
                int countOfBlockedBinder = 0;
                this.sb.append("blocked binder transactions:\n");
                while (readLine_ht != null) {
                    String ret;
                    if (hasPringTableTitle) {
                        ret = getHuaweiBinderProcInfo(readLine_ht);
                        if (ret != null) {
                            this.sb.append(ret);
                        }
                    } else {
                        ret = getBlockedBinderInfo(readLine_ht, countOfBlockedBinder < 200 ? mPidMap : str4);
                        if (ret != null) {
                            if (countOfBlockedBinder < 200) {
                                this.sb.append(ret);
                            }
                            countOfBlockedBinder++;
                        } else {
                            if (countOfBlockedBinder > 200) {
                                this.sb.append("Too many transactions(other ");
                                this.sb.append(String.valueOf(countOfBlockedBinder - 200));
                                this.sb.append(")...\n");
                            }
                            this.sb.append("binder thread count, and memory info:\n");
                            this.sb.append("\npid\tcontext\t\tmax_threads\tready_threads\trequested_threads\trequested_threads_started\tfree async space(byte)\n");
                            hasPringTableTitle = true;
                        }
                    }
                    readLine_ht = buff_ht2.readLine();
                    index++;
                    str4 = null;
                }
                this.sb.append("----- end binder transactions -----\n");
                try {
                    in_ht.close();
                    reader_ht.close();
                    buff_ht2.close();
                } catch (IOException e) {
                    buff_ht = e;
                    readLine_ht = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("exception info e:");
                    stringBuilder.append(buff_ht);
                    Slog.e(readLine_ht, stringBuilder.toString());
                    mPidMap.clear();
                    Slog.d(TAG, "read binder transaction end");
                }
            } catch (IOException e2) {
                readLine_ht = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("exception info e:");
                stringBuilder.append(e2);
                Slog.e(readLine_ht, stringBuilder.toString());
                if (in_ht != null) {
                    try {
                        in_ht.close();
                        in_ht = null;
                    } catch (IOException e3) {
                        buff_ht = e3;
                        readLine_ht = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("exception info e:");
                        stringBuilder.append(buff_ht);
                        Slog.e(readLine_ht, stringBuilder.toString());
                        mPidMap.clear();
                        Slog.d(TAG, "read binder transaction end");
                    }
                }
                if (reader_ht != null) {
                    reader_ht.close();
                    IOException reader_ht2 = null;
                }
                if (buff_ht2 != null) {
                    buff_ht2.close();
                }
            } catch (Throwable th) {
                int i = 0;
                index = th;
                if (in_ht != null) {
                    try {
                        in_ht.close();
                        in_ht = null;
                    } catch (IOException e22) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("exception info e:");
                        stringBuilder.append(e22);
                        Slog.e(TAG, stringBuilder.toString());
                    }
                }
                if (reader_ht != null) {
                    reader_ht.close();
                    reader_ht = null;
                }
                if (buff_ht2 != null) {
                    buff_ht2.close();
                    IOException buff_ht3 = null;
                }
            }
        } else {
            BufferedReader buff = null;
            InputStream in = null;
            Reader reader = null;
            File file = new File(this.BINDER_TRANS_PATH);
            if (file.exists()) {
                try {
                    Thread.sleep(1600);
                } catch (InterruptedException e4) {
                    InterruptedException interruptedException = e4;
                    str = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("exception info e:");
                    stringBuilder2.append(e4);
                    Slog.e(str, stringBuilder2.toString());
                }
            } else {
                str4 = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("file not exists : ");
                stringBuilder.append(this.BINDER_TRANS_PATH);
                Slog.w(str4, stringBuilder.toString());
            }
            try {
                in = new FileInputStream(file);
                reader = new InputStreamReader(in, "UTF-8");
                buff = new BufferedReader(reader);
                this.sb.append("blocked binder transactions:\n");
                str4 = buff.readLine();
                while (str4 != null) {
                    str = getTransactionLine(str4);
                    if (str != null) {
                        this.sb.append(str);
                    }
                    str4 = buff.readLine();
                    index++;
                }
                this.sb.append("----- end binder transactions -----\n");
                try {
                    in.close();
                } catch (IOException e222) {
                    iOException = e222;
                    str = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("exception info e:");
                    stringBuilder2.append(e222);
                    Slog.e(str, stringBuilder2.toString());
                }
                try {
                    reader.close();
                } catch (IOException e2222) {
                    iOException = e2222;
                    str = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("exception info e:");
                    stringBuilder2.append(e2222);
                    Slog.e(str, stringBuilder2.toString());
                }
                try {
                    buff.close();
                } catch (IOException e5) {
                    buff_ht = e5;
                    bufferedReader = buff_ht;
                    str = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("exception info e:");
                    stringBuilder2.append(buff_ht);
                    Slog.e(str, stringBuilder2.toString());
                    mPidMap.clear();
                    Slog.d(TAG, "read binder transaction end");
                }
            } catch (FileNotFoundException e6) {
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("exception info e:");
                stringBuilder2.append(e6);
                Slog.e(str, stringBuilder2.toString());
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e22222) {
                        iOException = e22222;
                        str = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("exception info e:");
                        stringBuilder2.append(e22222);
                        Slog.e(str, stringBuilder2.toString());
                    }
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e222222) {
                        iOException = e222222;
                        str = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("exception info e:");
                        stringBuilder2.append(e222222);
                        Slog.e(str, stringBuilder2.toString());
                    }
                }
                if (buff != null) {
                    try {
                        buff.close();
                    } catch (IOException e7) {
                        buff_ht = e7;
                        bufferedReader = buff_ht;
                        str = TAG;
                        stringBuilder2 = new StringBuilder();
                    }
                }
            } catch (IOException e2222222) {
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("exception info e:");
                stringBuilder2.append(e2222222);
                Slog.e(str, stringBuilder2.toString());
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e22222222) {
                        iOException = e22222222;
                        str = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("exception info e:");
                        stringBuilder2.append(e22222222);
                        Slog.e(str, stringBuilder2.toString());
                    }
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e222222222) {
                        iOException = e222222222;
                        str = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("exception info e:");
                        stringBuilder2.append(e222222222);
                        Slog.e(str, stringBuilder2.toString());
                    }
                }
                if (buff != null) {
                    try {
                        buff.close();
                    } catch (IOException e8) {
                        buff_ht = e8;
                        bufferedReader = buff_ht;
                        str = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("exception info e:");
                        stringBuilder2.append(buff_ht);
                        Slog.e(str, stringBuilder2.toString());
                        mPidMap.clear();
                        Slog.d(TAG, "read binder transaction end");
                    }
                }
            } catch (Throwable th2) {
                int i2 = 0;
                index = th2;
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e2222222222) {
                        iOException2 = e2222222222;
                        str2 = TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("exception info e:");
                        stringBuilder3.append(e2222222222);
                        Slog.e(str2, stringBuilder3.toString());
                    }
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e22222222222) {
                        iOException2 = e22222222222;
                        str2 = TAG;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("exception info e:");
                        stringBuilder3.append(e22222222222);
                        Slog.e(str2, stringBuilder3.toString());
                    }
                }
                if (buff != null) {
                    try {
                        buff.close();
                    } catch (IOException e222222222222) {
                        iOException2 = e222222222222;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("exception info e:");
                        stringBuilder2.append(e222222222222);
                        Slog.e(TAG, stringBuilder2.toString());
                    }
                }
            }
        }
        mPidMap.clear();
        Slog.d(TAG, "read binder transaction end");
    }

    private void removeReduntantPids(ArrayList<Integer> pids) {
        ArrayList<Integer> filteredList = new ArrayList();
        if (pids != null) {
            Iterator it = pids.iterator();
            while (it.hasNext()) {
                Integer pid = (Integer) it.next();
                if (!filteredList.contains(pid)) {
                    filteredList.add(pid);
                }
            }
            pids.clear();
            pids.addAll(filteredList);
        }
    }
}
