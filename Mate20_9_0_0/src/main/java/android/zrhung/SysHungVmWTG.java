package android.zrhung;

import android.os.Process;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Slog;
import android.util.ZRHung;
import android.util.ZRHung.HungConfig;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SysHungVmWTG extends ZrHungImpl {
    private static final String BINDER_TRANS_PATH = "/sys/kernel/debug/binder/transactions";
    private static final String BINDER_TRANS_PATH_TRACING = "/sys/kernel/tracing/binder/transactions";
    static final int COMPLETED = 0;
    static final int OVERDUE = 3;
    static final String TAG = "ZrHung.SysHungVmWTG";
    static final int WAITED_HALF = 2;
    static final int WAITING = 1;
    private boolean configReady = false;
    private String[] daemonsToCheck = null;
    private boolean enable = false;
    private boolean waitedHalf = false;

    public SysHungVmWTG(String wpName) {
        super(wpName);
        Slog.i(TAG, "Init..");
    }

    public boolean check(ZrHungData args) {
        if (!this.configReady) {
            HungConfig cfg = getConfig();
            if (cfg == null || cfg.status != 0) {
                Slog.e(TAG, "Failed to get config from zrhung");
                return false;
            }
            String[] configs = cfg.value.split(",");
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Config from zrhung:");
            stringBuilder.append(cfg.value);
            Slog.i(str, stringBuilder.toString());
            this.enable = configs[0].equals("1");
            this.daemonsToCheck = (String[]) Arrays.copyOfRange(configs, 1, configs.length);
            this.configReady = true;
        }
        if (!this.enable) {
            return false;
        }
        Slog.w(TAG, "System blocked, run checking!");
        int[] pids = Process.getPidsForCommands(this.daemonsToCheck);
        int length = pids.length;
        for (int i = 0; i < length; i++) {
            if (findBadProcess(pids[i])) {
                String cmd = new StringBuilder();
                cmd.append("p=");
                cmd.append(Process.myPid());
                cmd.append(",p=");
                cmd.append(pids[i]);
                cmd = cmd.toString();
                String msg = new StringBuilder();
                msg.append("Found process blocked: PID:");
                msg.append(pids[i]);
                msg.append(" cmd: ");
                msg.append(this.daemonsToCheck[i]);
                msg = msg.toString();
                Slog.e(TAG, msg);
                ZRHung.sendHungEvent((short) 22, cmd, msg);
                if (args.getInt("waitState") == 3) {
                    Slog.e(TAG, "Daemon(s) blocked for over 60s, reboot to recover!");
                    SystemClock.sleep(2000);
                    lowLevelReboot("Daemon(s) blocked");
                    return true;
                }
            }
        }
        return false;
    }

    private int readTransactionLines(ArrayList<String> transLines) {
        int lineCount = 0;
        BufferedReader buff = null;
        File file = new File(BINDER_TRANS_PATH);
        if (!file.exists()) {
            Slog.w(TAG, "file not exists : /sys/kernel/debug/binder/transactions");
            file = new File(BINDER_TRANS_PATH_TRACING);
            if (!file.exists()) {
                Slog.w(TAG, "file not exists : /sys/kernel/tracing/binder/transactions");
                return -1;
            }
        }
        try {
            buff = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
            String regEx = "outgoing transaction";
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

    private boolean findBadProcess(int pid) {
        ArrayList<String> transLines = new ArrayList();
        if (readTransactionLines(transLines) > 0) {
            String regEx = "outgoing transaction .+ from (\\d+):(\\d+) to (\\d+):(\\d+) code ([0-9a-f]+)";
            Pattern pattern = Pattern.compile("outgoing transaction .+ from (\\d+):(\\d+) to (\\d+):(\\d+) code ([0-9a-f]+)");
            int size = transLines.size();
            for (int i = 0; i < size; i++) {
                Matcher matcher = pattern.matcher((String) transLines.get(i));
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("search blocked process: pid = ");
                stringBuilder.append(pid);
                Slog.i(str, stringBuilder.toString());
                if (matcher.find() && pid == Integer.parseInt(matcher.group(3))) {
                    return true;
                }
            }
        }
        return false;
    }

    private void lowLevelReboot(String reason) {
        Slog.w(TAG, "Low level reboot!");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("reboot,");
        stringBuilder.append(reason);
        SystemProperties.set("sys.powerctl", stringBuilder.toString());
    }
}
