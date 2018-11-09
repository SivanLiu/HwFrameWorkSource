package com.android.server.coverage;

import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseOutputStream;
import android.os.ResultReceiver;
import android.os.ShellCallback;
import android.os.ShellCommand;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import org.jacoco.agent.rt.RT;

public class CoverageService extends Binder {
    public static final String COVERAGE_SERVICE = "coverage";
    public static final boolean ENABLED;

    private static class CoverageCommand extends ShellCommand {
        private CoverageCommand() {
        }

        public int onCommand(String cmd) {
            if ("dump".equals(cmd)) {
                return onDump();
            }
            if ("reset".equals(cmd)) {
                return onReset();
            }
            return handleDefaultCommands(cmd);
        }

        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("Coverage commands:");
            pw.println("  help");
            pw.println("    Print this help text.");
            pw.println("  dump [FILE]");
            pw.println("    Dump code coverage to FILE.");
            pw.println("  reset");
            pw.println("    Reset coverage information.");
        }

        private int onDump() {
            IOException e;
            Throwable th;
            Throwable th2 = null;
            String dest = getNextArg();
            if (dest == null) {
                dest = "/data/local/tmp/coverage.ec";
            } else {
                File f = new File(dest);
                if (f.isDirectory()) {
                    dest = new File(f, "coverage.ec").getAbsolutePath();
                }
            }
            ParcelFileDescriptor fd = openOutputFileForSystem(dest);
            if (fd == null) {
                return -1;
            }
            BufferedOutputStream bufferedOutputStream = null;
            try {
                BufferedOutputStream output = new BufferedOutputStream(new AutoCloseOutputStream(fd));
                try {
                    output.write(RT.getAgent().getExecutionData(false));
                    output.flush();
                    getOutPrintWriter().println(String.format("Dumped coverage data to %s", new Object[]{dest}));
                    if (output != null) {
                        try {
                            output.close();
                        } catch (Throwable th3) {
                            th2 = th3;
                        }
                    }
                    if (th2 == null) {
                        return 0;
                    }
                    try {
                        throw th2;
                    } catch (IOException e2) {
                        e = e2;
                        bufferedOutputStream = output;
                    }
                } catch (Throwable th4) {
                    th = th4;
                    bufferedOutputStream = output;
                    if (bufferedOutputStream != null) {
                        try {
                            bufferedOutputStream.close();
                        } catch (Throwable th5) {
                            if (th2 == null) {
                                th2 = th5;
                            } else if (th2 != th5) {
                                th2.addSuppressed(th5);
                            }
                        }
                    }
                    if (th2 == null) {
                        throw th;
                    }
                    try {
                        throw th2;
                    } catch (IOException e3) {
                        e = e3;
                        getErrPrintWriter().println("Failed to dump coverage data: " + e.getMessage());
                        return -1;
                    }
                }
            } catch (Throwable th6) {
                th = th6;
                if (bufferedOutputStream != null) {
                    bufferedOutputStream.close();
                }
                if (th2 == null) {
                    throw th2;
                }
                throw th;
            }
        }

        private int onReset() {
            RT.getAgent().reset();
            getOutPrintWriter().println("Reset coverage data");
            return 0;
        }
    }

    static {
        boolean shouldEnable = true;
        try {
            Class.forName("org.jacoco.agent.rt.RT");
        } catch (ClassNotFoundException e) {
            shouldEnable = false;
        }
        ENABLED = shouldEnable;
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        new CoverageCommand().exec(this, in, out, err, args, callback, resultReceiver);
    }
}
