package android.os;

import com.android.internal.util.FastPrintWriter;
import java.io.BufferedInputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

public abstract class ShellCommand {
    static final boolean DEBUG = false;
    static final String TAG = "ShellCommand";
    private int mArgPos;
    private String[] mArgs;
    private String mCmd;
    private String mCurArgData;
    private FileDescriptor mErr;
    private FastPrintWriter mErrPrintWriter;
    private FileOutputStream mFileErr;
    private FileInputStream mFileIn;
    private FileOutputStream mFileOut;
    private FileDescriptor mIn;
    private InputStream mInputStream;
    private FileDescriptor mOut;
    private FastPrintWriter mOutPrintWriter;
    private ResultReceiver mResultReceiver;
    private ShellCallback mShellCallback;
    private Binder mTarget;

    public abstract int onCommand(String str);

    public abstract void onHelp();

    public void init(Binder target, FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, int firstArgPos) {
        this.mTarget = target;
        this.mIn = in;
        this.mOut = out;
        this.mErr = err;
        this.mArgs = args;
        this.mShellCallback = callback;
        this.mResultReceiver = null;
        this.mCmd = null;
        this.mArgPos = firstArgPos;
        this.mCurArgData = null;
        this.mFileIn = null;
        this.mFileOut = null;
        this.mFileErr = null;
        this.mOutPrintWriter = null;
        this.mErrPrintWriter = null;
        this.mInputStream = null;
    }

    /* JADX WARNING: Missing block: B:16:0x0042, code skipped:
            if (r9.mResultReceiver != null) goto L_0x0044;
     */
    /* JADX WARNING: Missing block: B:17:0x0044, code skipped:
            r9.mResultReceiver.send(r2, null);
     */
    /* JADX WARNING: Missing block: B:29:0x0070, code skipped:
            if (r9.mResultReceiver == null) goto L_0x00ad;
     */
    /* JADX WARNING: Missing block: B:40:0x00aa, code skipped:
            if (r9.mResultReceiver == null) goto L_0x00ad;
     */
    /* JADX WARNING: Missing block: B:41:0x00ad, code skipped:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int exec(Binder target, FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) {
        int start;
        String cmd;
        PrintWriter eout;
        String[] strArr = args;
        if (strArr == null || strArr.length <= 0) {
            start = 0;
            cmd = null;
        } else {
            cmd = strArr[0];
            start = 1;
        }
        init(target, in, out, err, strArr, callback, start);
        this.mCmd = cmd;
        this.mResultReceiver = resultReceiver;
        int res = -1;
        try {
            res = onCommand(this.mCmd);
            if (this.mOutPrintWriter != null) {
                this.mOutPrintWriter.flush();
            }
            if (this.mErrPrintWriter != null) {
                this.mErrPrintWriter.flush();
            }
        } catch (SecurityException e) {
            eout = getErrPrintWriter();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Security exception: ");
            stringBuilder.append(e.getMessage());
            eout.println(stringBuilder.toString());
            eout.println();
            e.printStackTrace(eout);
            if (this.mOutPrintWriter != null) {
                this.mOutPrintWriter.flush();
            }
            if (this.mErrPrintWriter != null) {
                this.mErrPrintWriter.flush();
            }
        } catch (Throwable th) {
            if (this.mOutPrintWriter != null) {
                this.mOutPrintWriter.flush();
            }
            if (this.mErrPrintWriter != null) {
                this.mErrPrintWriter.flush();
            }
            if (this.mResultReceiver != null) {
                this.mResultReceiver.send(res, null);
            }
        }
    }

    public ResultReceiver adoptResultReceiver() {
        ResultReceiver rr = this.mResultReceiver;
        this.mResultReceiver = null;
        return rr;
    }

    public FileDescriptor getOutFileDescriptor() {
        return this.mOut;
    }

    public OutputStream getRawOutputStream() {
        if (this.mFileOut == null) {
            this.mFileOut = new FileOutputStream(this.mOut);
        }
        return this.mFileOut;
    }

    public PrintWriter getOutPrintWriter() {
        if (this.mOutPrintWriter == null) {
            this.mOutPrintWriter = new FastPrintWriter(getRawOutputStream());
        }
        return this.mOutPrintWriter;
    }

    public FileDescriptor getErrFileDescriptor() {
        return this.mErr;
    }

    public OutputStream getRawErrorStream() {
        if (this.mFileErr == null) {
            this.mFileErr = new FileOutputStream(this.mErr);
        }
        return this.mFileErr;
    }

    public PrintWriter getErrPrintWriter() {
        if (this.mErr == null) {
            return getOutPrintWriter();
        }
        if (this.mErrPrintWriter == null) {
            this.mErrPrintWriter = new FastPrintWriter(getRawErrorStream());
        }
        return this.mErrPrintWriter;
    }

    public FileDescriptor getInFileDescriptor() {
        return this.mIn;
    }

    public InputStream getRawInputStream() {
        if (this.mFileIn == null) {
            this.mFileIn = new FileInputStream(this.mIn);
        }
        return this.mFileIn;
    }

    public InputStream getBufferedInputStream() {
        if (this.mInputStream == null) {
            this.mInputStream = new BufferedInputStream(getRawInputStream());
        }
        return this.mInputStream;
    }

    public ParcelFileDescriptor openFileForSystem(String path, String mode) {
        try {
            ParcelFileDescriptor pfd = getShellCallback().openFile(path, "u:r:system_server:s0", mode);
            if (pfd != null) {
                return pfd;
            }
        } catch (RuntimeException e) {
            PrintWriter errPrintWriter = getErrPrintWriter();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failure opening file: ");
            stringBuilder.append(e.getMessage());
            errPrintWriter.println(stringBuilder.toString());
        }
        PrintWriter errPrintWriter2 = getErrPrintWriter();
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Error: Unable to open file: ");
        stringBuilder2.append(path);
        errPrintWriter2.println(stringBuilder2.toString());
        getErrPrintWriter().println("Consider using a file under /data/local/tmp/");
        return null;
    }

    public String getNextOption() {
        String prev;
        if (this.mCurArgData != null) {
            prev = this.mArgs[this.mArgPos - 1];
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("No argument expected after \"");
            stringBuilder.append(prev);
            stringBuilder.append("\"");
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (this.mArgPos >= this.mArgs.length) {
            return null;
        } else {
            prev = this.mArgs[this.mArgPos];
            if (!prev.startsWith("-")) {
                return null;
            }
            this.mArgPos++;
            if (prev.equals("--")) {
                return null;
            }
            if (prev.length() <= 1 || prev.charAt(1) == '-') {
                this.mCurArgData = null;
                return prev;
            } else if (prev.length() > 2) {
                this.mCurArgData = prev.substring(2);
                return prev.substring(0, 2);
            } else {
                this.mCurArgData = null;
                return prev;
            }
        }
    }

    public String getNextArg() {
        if (this.mCurArgData != null) {
            String arg = this.mCurArgData;
            this.mCurArgData = null;
            return arg;
        } else if (this.mArgPos >= this.mArgs.length) {
            return null;
        } else {
            String[] strArr = this.mArgs;
            int i = this.mArgPos;
            this.mArgPos = i + 1;
            return strArr[i];
        }
    }

    public String peekNextArg() {
        if (this.mCurArgData != null) {
            return this.mCurArgData;
        }
        if (this.mArgPos < this.mArgs.length) {
            return this.mArgs[this.mArgPos];
        }
        return null;
    }

    public String getNextArgRequired() {
        String arg = getNextArg();
        if (arg != null) {
            return arg;
        }
        String prev = this.mArgs[this.mArgPos - 1];
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Argument expected after \"");
        stringBuilder.append(prev);
        stringBuilder.append("\"");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public ShellCallback getShellCallback() {
        return this.mShellCallback;
    }

    public int handleDefaultCommands(String cmd) {
        if ("dump".equals(cmd)) {
            String[] newArgs = new String[(this.mArgs.length - 1)];
            System.arraycopy(this.mArgs, 1, newArgs, 0, this.mArgs.length - 1);
            this.mTarget.doDump(this.mOut, getOutPrintWriter(), newArgs);
            return 0;
        }
        if (cmd == null || "help".equals(cmd) || "-h".equals(cmd)) {
            onHelp();
        } else {
            PrintWriter outPrintWriter = getOutPrintWriter();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown command: ");
            stringBuilder.append(cmd);
            outPrintWriter.println(stringBuilder.toString());
        }
        return -1;
    }

    protected String[] getArgs() {
        return this.mArgs;
    }

    protected int getArgPos() {
        return this.mArgPos;
    }
}
