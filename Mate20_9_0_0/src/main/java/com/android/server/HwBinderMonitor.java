package com.android.server;

import android.annotation.SuppressLint;
import android.os.Process;
import android.util.Slog;
import com.android.server.HwServiceFactory.IHwBinderMonitor;
import com.android.server.pm.auth.HwCertification;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import libcore.io.IoUtils;

public class HwBinderMonitor implements IHwBinderMonitor {
    public static final int CATCH_BADPROC_BY_PID = 2;
    public static final int CATCH_BADPROC_BY_TID = 1;
    public static final String CONSTANTPATH = "/sys/kernel/debug/binder/proc/";
    private static final String CONSTANTPATH_TRACING = "/sys/kernel/tracing/binder/proc/";
    static final int NATIVE_SCORE = 0;
    public static final int PROCESS_ERROR = -1;
    public static final int PROCESS_IS_NATIVE = 1;
    public static final int PROCESS_NOT_NATIVE = 0;
    static final String TAG = "HwBinderMonitor";
    private String BINDER_TRANS_PATH = "/sys/kernel/debug/binder/transactions";
    private String BINDER_TRANS_PATH_TRACING = "/sys/kernel/tracing/binder/transactions";

    @SuppressLint({"AvoidMethodInForLoop"})
    public void addBinderPid(ArrayList<Integer> list, int pid) {
        ArrayList<Integer> serverPidList = getNotNativeServerPidList(pid);
        for (int i = 0; i < serverPidList.size(); i++) {
            if (!list.contains(serverPidList.get(i))) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("pid: ");
                stringBuilder.append(serverPidList.get(i));
                Slog.i(str, stringBuilder.toString());
                list.add((Integer) serverPidList.get(i));
            }
        }
    }

    private int isNativeProcess(int pid) {
        String str;
        if (pid <= 0) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("pid less than 0, pid is ");
            stringBuilder.append(pid);
            Slog.w(str, stringBuilder.toString());
            return -1;
        }
        str = getAdjForPid(pid);
        if (str == null) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("no such oom_score file and pid is");
            stringBuilder2.append(pid);
            Slog.w(str2, stringBuilder2.toString());
            return -1;
        }
        try {
            if (Integer.parseInt(str.trim()) == 0) {
                return 1;
            }
            return 0;
        } catch (NumberFormatException e) {
            String str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("isNativeProcess NumberFormatException: ");
            stringBuilder3.append(e.toString());
            Slog.e(str3, stringBuilder3.toString());
            return -1;
        }
    }

    private final String getAdjForPid(int pid) {
        String[] outStrings = new String[1];
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("/proc/");
        stringBuilder.append(pid);
        stringBuilder.append("/oom_score");
        Process.readProcFile(stringBuilder.toString(), new int[]{4128}, outStrings, null, null);
        return outStrings[0];
    }

    private ArrayList<Integer> getServerPidList(int bClientPid) {
        ArrayList<Integer> serverPidList = new ArrayList();
        String pid = String.valueOf(bClientPid);
        BufferedReader buff = null;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(CONSTANTPATH);
        stringBuilder.append(pid);
        File file = new File(stringBuilder.toString());
        if (!file.exists()) {
            Slog.w(TAG, "file not exists : /sys/kernel/debug/binder/proc/");
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(CONSTANTPATH_TRACING);
            stringBuilder2.append(pid);
            file = new File(stringBuilder2.toString());
        }
        if (file.exists()) {
            try {
                buff = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
                for (String readLine = buff.readLine(); readLine != null; readLine = buff.readLine()) {
                    if (isOutGoingLine(readLine, pid)) {
                        serverPidList.add(Integer.valueOf(getServerPid(readLine)));
                    }
                }
                try {
                    buff.close();
                } catch (IOException e) {
                }
            } catch (FileNotFoundException e2) {
                if (buff != null) {
                    buff.close();
                }
            } catch (IOException e3) {
                if (buff != null) {
                    try {
                        buff.close();
                    } catch (IOException e4) {
                    }
                }
            } catch (Throwable th) {
                if (buff != null) {
                    try {
                        buff.close();
                    } catch (IOException e5) {
                    }
                }
            }
        }
        return serverPidList;
    }

    private boolean isOutGoingLine(String str, String pid) {
        String regEx = new StringBuilder();
        regEx.append("outgoing transaction .+ from ");
        regEx.append(pid);
        regEx.append(":");
        return Pattern.compile(regEx.toString()).matcher(str).find();
    }

    private String getServerPid(String str) {
        int beginNum = str.indexOf(HwCertification.KEY_DATE_TO) + 3;
        return str.substring(beginNum, str.indexOf(":", beginNum));
    }

    public ArrayList<Integer> getNotNativeServerPidList(int bClientPid) {
        int serverPid = 0;
        ArrayList<Integer> serverPidList = new ArrayList();
        ArrayList<Integer> tmpServerPidList = getServerPidList(bClientPid);
        if (tmpServerPidList.size() > 0) {
            serverPid = ((Integer) tmpServerPidList.get(0)).intValue();
        }
        if (isNativeProcess(serverPid) == 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("print stack of serverPid: ");
            stringBuilder.append(serverPid);
            Slog.w(str, stringBuilder.toString());
            serverPidList.add(Integer.valueOf(serverPid));
        }
        serverPidList.addAll(getNotNativeFinalServerPidList(serverPid));
        return serverPidList;
    }

    @SuppressLint({"AvoidMethodInForLoop"})
    private ArrayList<Integer> getNotNativeFinalServerPidList(int serverPid) {
        ArrayList<Integer> finalServerPidList = new ArrayList();
        ArrayList<Integer> allServerPidList = getServerPidList(serverPid);
        int i = 0;
        while (i < allServerPidList.size()) {
            if (isNativeProcess(((Integer) allServerPidList.get(i)).intValue()) == 0 && !finalServerPidList.contains(allServerPidList.get(i))) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("print stack of finalServerPid: ");
                stringBuilder.append(allServerPidList.get(i));
                Slog.w(str, stringBuilder.toString());
                finalServerPidList.add((Integer) allServerPidList.get(i));
            }
            i++;
        }
        return finalServerPidList;
    }

    private static String readProcName(String pid) {
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

    private static String getTransactionLine(String str) {
        String regEx = "outgoing transaction .+ from (\\d+):(\\d+) to (\\d+):(\\d+) code ([0-9a-f]+)";
        Matcher matcher = Pattern.compile("outgoing transaction .+ from (\\d+):(\\d+) to (\\d+):(\\d+) code ([0-9a-f]+)").matcher(str);
        if (!matcher.find()) {
            return null;
        }
        StringBuilder stringBuilder = new StringBuilder();
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

    public void writeTransactonToTrace(String tracesPath) {
        String str;
        BufferedReader buff = null;
        File file = new File(this.BINDER_TRANS_PATH);
        InputStream in = null;
        FileOutputStream out = null;
        Reader reader = null;
        int index = 0;
        if (!file.exists()) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("file not exists : ");
            stringBuilder.append(this.BINDER_TRANS_PATH);
            Slog.w(str, stringBuilder.toString());
            file = new File(this.BINDER_TRANS_PATH_TRACING);
            if (!file.exists()) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("file not exists : ");
                stringBuilder.append(this.BINDER_TRANS_PATH_TRACING);
                Slog.w(str, stringBuilder.toString());
                return;
            }
        }
        try {
            Thread.sleep(1600);
        } catch (InterruptedException e) {
        }
        try {
            in = new FileInputStream(file);
            reader = new InputStreamReader(in, "UTF-8");
            buff = new BufferedReader(reader);
            out = new FileOutputStream(tracesPath, true);
            out.write("\n----- binder transactions -----\n".getBytes("UTF-8"));
            str = buff.readLine();
            while (str != null) {
                String transaction = getTransactionLine(str);
                if (transaction != null) {
                    out.write(transaction.getBytes("UTF-8"));
                }
                str = buff.readLine();
                index++;
            }
            out.write("\n----- end binder transactions -----\n".getBytes("UTF-8"));
            try {
                in.close();
            } catch (IOException e2) {
            }
            try {
                reader.close();
            } catch (IOException e3) {
            }
            try {
                buff.close();
            } catch (IOException e4) {
            }
            try {
                out.close();
            } catch (IOException e5) {
            }
        } catch (FileNotFoundException e6) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e7) {
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e8) {
                }
            }
            if (buff != null) {
                try {
                    buff.close();
                } catch (IOException e9) {
                }
            }
            if (out != null) {
                out.close();
            }
        } catch (IOException e10) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e11) {
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e12) {
                }
            }
            if (buff != null) {
                try {
                    buff.close();
                } catch (IOException e13) {
                }
            }
            if (out != null) {
                out.close();
            }
        } catch (Throwable th) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e14) {
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e15) {
                }
            }
            if (buff != null) {
                try {
                    buff.close();
                } catch (IOException e16) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e17) {
                }
            }
        }
    }

    private static int parseTransactionLine(String str, int tid) {
        String regEx = "outgoing transaction .+ from (\\d+):(\\d+) to (\\d+):(\\d+) code ([0-9a-f]+)";
        Matcher matcher = Pattern.compile("outgoing transaction .+ from (\\d+):(\\d+) to (\\d+):(\\d+) code ([0-9a-f]+)").matcher(str);
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

    public int catchBadproc(int id, int ops) {
        ArrayList<String> transLines = new ArrayList();
        int ret = -1;
        if (readTransactionLines(transLines) > 0) {
            for (int i = 0; i < transLines.size(); i++) {
                String tl = (String) transLines.get(i);
                String str;
                StringBuilder stringBuilder;
                if (ops == 1) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("search blocked process by tid ");
                    stringBuilder.append(id);
                    Slog.i(str, stringBuilder.toString());
                    ret = parseTransactionLine(tl, id);
                } else if (ops == 2) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("search blocked process by pid ");
                    stringBuilder.append(id);
                    Slog.i(str, stringBuilder.toString());
                    ret = findPidInTransactionLine(tl, id);
                }
                if (ret > 0) {
                    return ret;
                }
            }
        }
        return ret;
    }

    private int readTransactionLines(ArrayList<String> transLines) {
        int lineCount = 0;
        BufferedReader buff = null;
        File file = new File(this.BINDER_TRANS_PATH);
        String regEx;
        if (file.exists()) {
            try {
                buff = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
                regEx = "outgoing transaction";
                Pattern pattern = Pattern.compile("outgoing transaction");
                for (String readLine = buff.readLine(); readLine != null; readLine = buff.readLine()) {
                    if (pattern.matcher(readLine).find()) {
                        transLines.add(readLine);
                        lineCount++;
                    }
                }
                try {
                    buff.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (FileNotFoundException e2) {
                e2.printStackTrace();
                if (buff != null) {
                    buff.close();
                }
            } catch (IOException e3) {
                e3.printStackTrace();
                if (buff != null) {
                    buff.close();
                }
            } catch (Throwable th) {
                if (buff != null) {
                    try {
                        buff.close();
                    } catch (IOException e4) {
                        e4.printStackTrace();
                    }
                }
            }
            return lineCount;
        }
        regEx = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("file not exists : ");
        stringBuilder.append(this.BINDER_TRANS_PATH);
        Slog.w(regEx, stringBuilder.toString());
        return -1;
    }

    private int findPidInTransactionLine(String transLine, int pid) {
        String regEx = "outgoing transaction .+ from (\\d+):(\\d+) to (\\d+):(\\d+) code ([0-9a-f]+)";
        Matcher matcher = Pattern.compile("outgoing transaction .+ from (\\d+):(\\d+) to (\\d+):(\\d+) code ([0-9a-f]+)").matcher(transLine);
        if (matcher.find()) {
            int pid_to = Integer.parseInt(matcher.group(3));
            if (pid_to == pid) {
                return pid_to;
            }
        }
        return -1;
    }
}
