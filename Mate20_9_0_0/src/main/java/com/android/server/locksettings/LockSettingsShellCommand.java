package com.android.server.locksettings;

import android.app.ActivityManager;
import android.content.Context;
import android.os.RemoteException;
import android.os.ShellCommand;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternUtils.RequestThrottledException;
import java.io.PrintWriter;

class LockSettingsShellCommand extends ShellCommand {
    private static final String COMMAND_CLEAR = "clear";
    private static final String COMMAND_GET_DISABLED = "get-disabled";
    private static final String COMMAND_SET_DISABLED = "set-disabled";
    private static final String COMMAND_SET_PASSWORD = "set-password";
    private static final String COMMAND_SET_PATTERN = "set-pattern";
    private static final String COMMAND_SET_PIN = "set-pin";
    private static final String COMMAND_SP = "sp";
    private static final String COMMAND_VERIFY = "verify";
    private final Context mContext;
    private int mCurrentUserId;
    private final LockPatternUtils mLockPatternUtils;
    private String mNew = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
    private String mOld = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;

    LockSettingsShellCommand(Context context, LockPatternUtils lockPatternUtils) {
        this.mContext = context;
        this.mLockPatternUtils = lockPatternUtils;
    }

    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int onCommand(String cmd) {
        StringBuilder stringBuilder;
        try {
            this.mCurrentUserId = ActivityManager.getService().getCurrentUser().id;
            parseArgs();
            if (!checkCredential()) {
                return -1;
            }
            int i;
            switch (cmd.hashCode()) {
                case -2044327643:
                    if (cmd.equals(COMMAND_SET_PATTERN)) {
                        i = 0;
                        break;
                    }
                case -1473704173:
                    if (cmd.equals(COMMAND_GET_DISABLED)) {
                        i = 7;
                        break;
                    }
                case -819951495:
                    if (cmd.equals(COMMAND_VERIFY)) {
                        i = 6;
                        break;
                    }
                case 3677:
                    if (cmd.equals(COMMAND_SP)) {
                        i = 4;
                        break;
                    }
                case 75288455:
                    if (cmd.equals(COMMAND_SET_DISABLED)) {
                        i = 5;
                        break;
                    }
                case 94746189:
                    if (cmd.equals(COMMAND_CLEAR)) {
                        i = 3;
                        break;
                    }
                case 1021333414:
                    if (cmd.equals(COMMAND_SET_PASSWORD)) {
                        i = 1;
                        break;
                    }
                case 1983832490:
                    if (cmd.equals(COMMAND_SET_PIN)) {
                        i = 2;
                        break;
                    }
                default:
                    i = -1;
                    break;
            }
            switch (i) {
                case 0:
                    runSetPattern();
                    break;
                case 1:
                    runSetPassword();
                    break;
                case 2:
                    runSetPin();
                    break;
                case 3:
                    runClear();
                    break;
                case 4:
                    runChangeSp();
                    break;
                case 5:
                    runSetDisabled();
                    break;
                case 6:
                    runVerify();
                    break;
                case 7:
                    runGetDisabled();
                    break;
                default:
                    PrintWriter errPrintWriter = getErrPrintWriter();
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown command: ");
                    stringBuilder.append(cmd);
                    errPrintWriter.println(stringBuilder.toString());
                    break;
            }
            return 0;
        } catch (Exception e) {
            PrintWriter errPrintWriter2 = getErrPrintWriter();
            stringBuilder = new StringBuilder();
            stringBuilder.append("Error while executing command: ");
            stringBuilder.append(cmd);
            errPrintWriter2.println(stringBuilder.toString());
            e.printStackTrace(getErrPrintWriter());
            return -1;
        }
    }

    private void runVerify() {
        getOutPrintWriter().println("Lock credential verified successfully");
    }

    public void onHelp() {
    }

    private void parseArgs() {
        while (true) {
            String nextOption = getNextOption();
            String opt = nextOption;
            if (nextOption == null) {
                this.mNew = getNextArg();
                return;
            } else if ("--old".equals(opt)) {
                this.mOld = getNextArgRequired();
            } else if ("--user".equals(opt)) {
                this.mCurrentUserId = Integer.parseInt(getNextArgRequired());
            } else {
                PrintWriter errPrintWriter = getErrPrintWriter();
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown option: ");
                stringBuilder.append(opt);
                errPrintWriter.println(stringBuilder.toString());
                throw new IllegalArgumentException();
            }
        }
    }

    private void runChangeSp() {
        if (this.mNew != null) {
            if ("1".equals(this.mNew)) {
                this.mLockPatternUtils.enableSyntheticPassword();
                getOutPrintWriter().println("Synthetic password enabled");
            } else if ("0".equals(this.mNew)) {
                this.mLockPatternUtils.disableSyntheticPassword();
                getOutPrintWriter().println("Synthetic password disabled");
            }
        }
        getOutPrintWriter().println(String.format("SP Enabled = %b", new Object[]{Boolean.valueOf(this.mLockPatternUtils.isSyntheticPasswordEnabled())}));
    }

    private void runSetPattern() throws RemoteException {
        this.mLockPatternUtils.saveLockPattern(LockPatternUtils.stringToPattern(this.mNew), this.mOld, this.mCurrentUserId);
        PrintWriter outPrintWriter = getOutPrintWriter();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Pattern set to '");
        stringBuilder.append(this.mNew);
        stringBuilder.append("'");
        outPrintWriter.println(stringBuilder.toString());
    }

    private void runSetPassword() throws RemoteException {
        this.mLockPatternUtils.saveLockPassword(this.mNew, this.mOld, 262144, this.mCurrentUserId);
        PrintWriter outPrintWriter = getOutPrintWriter();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Password set to '");
        stringBuilder.append(this.mNew);
        stringBuilder.append("'");
        outPrintWriter.println(stringBuilder.toString());
    }

    private void runSetPin() throws RemoteException {
        this.mLockPatternUtils.saveLockPassword(this.mNew, this.mOld, 131072, this.mCurrentUserId);
        PrintWriter outPrintWriter = getOutPrintWriter();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Pin set to '");
        stringBuilder.append(this.mNew);
        stringBuilder.append("'");
        outPrintWriter.println(stringBuilder.toString());
    }

    private void runClear() throws RemoteException {
        this.mLockPatternUtils.clearLock(this.mOld, this.mCurrentUserId);
        getOutPrintWriter().println("Lock credential cleared");
    }

    private void runSetDisabled() throws RemoteException {
        boolean disabled = Boolean.parseBoolean(this.mNew);
        this.mLockPatternUtils.setLockScreenDisabled(disabled, this.mCurrentUserId);
        PrintWriter outPrintWriter = getOutPrintWriter();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Lock screen disabled set to ");
        stringBuilder.append(disabled);
        outPrintWriter.println(stringBuilder.toString());
    }

    private void runGetDisabled() {
        getOutPrintWriter().println(this.mLockPatternUtils.isLockScreenDisabled(this.mCurrentUserId));
    }

    private boolean checkCredential() throws RemoteException {
        boolean havePassword = this.mLockPatternUtils.isLockPasswordEnabled(this.mCurrentUserId);
        boolean havePattern = this.mLockPatternUtils.isLockPatternEnabled(this.mCurrentUserId);
        if (!havePassword && !havePattern) {
            return true;
        }
        if (this.mLockPatternUtils.isManagedProfileWithUnifiedChallenge(this.mCurrentUserId)) {
            getOutPrintWriter().println("Profile uses unified challenge");
            return false;
        }
        RequestThrottledException e;
        if (havePassword) {
            try {
                e = this.mLockPatternUtils.checkPassword(this.mOld, this.mCurrentUserId);
            } catch (RequestThrottledException e2) {
                getOutPrintWriter().println("Request throttled");
                return false;
            }
        }
        e = this.mLockPatternUtils.checkPattern(LockPatternUtils.stringToPattern(this.mOld), this.mCurrentUserId);
        if (e == null) {
            if (!this.mLockPatternUtils.isManagedProfileWithUnifiedChallenge(this.mCurrentUserId)) {
                this.mLockPatternUtils.reportFailedPasswordAttempt(this.mCurrentUserId);
            }
            PrintWriter outPrintWriter = getOutPrintWriter();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Old password '");
            stringBuilder.append(this.mOld);
            stringBuilder.append("' didn't match");
            outPrintWriter.println(stringBuilder.toString());
        }
        return e;
    }
}
