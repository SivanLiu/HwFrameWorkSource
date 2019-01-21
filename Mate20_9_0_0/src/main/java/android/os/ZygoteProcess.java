package android.os;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.LocalSocketAddress.Namespace;
import android.net.wifi.WifiScanLog;
import android.os.Process.ProcessStartResult;
import android.util.Log;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.Preconditions;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ZygoteProcess {
    private static final String LOG_TAG = "ZygoteProcess";
    static final int ZYGOTE_RETRY_MILLIS = 500;
    private List<String> mApiBlacklistExemptions;
    private int mHiddenApiAccessLogSampleRate;
    private final Object mLock;
    private final LocalSocketAddress mSecondarySocket;
    private final LocalSocketAddress mSocket;
    private ZygoteState primaryZygoteState;
    private ZygoteState secondaryZygoteState;

    public static class ZygoteState {
        final List<String> abiList;
        final DataInputStream inputStream;
        boolean mClosed;
        final LocalSocket socket;
        final BufferedWriter writer;

        private ZygoteState(LocalSocket socket, DataInputStream inputStream, BufferedWriter writer, List<String> abiList) {
            this.socket = socket;
            this.inputStream = inputStream;
            this.writer = writer;
            this.abiList = abiList;
        }

        public static ZygoteState connect(LocalSocketAddress address) throws IOException {
            LocalSocket zygoteSocket = new LocalSocket();
            try {
                zygoteSocket.connect(address);
                DataInputStream zygoteInputStream = new DataInputStream(zygoteSocket.getInputStream());
                BufferedWriter zygoteWriter = new BufferedWriter(new OutputStreamWriter(zygoteSocket.getOutputStream()), 256);
                String abiListString = ZygoteProcess.getAbiList(zygoteWriter, zygoteInputStream);
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Process: zygote socket ");
                stringBuilder.append(address.getNamespace());
                stringBuilder.append("/");
                stringBuilder.append(address.getName());
                stringBuilder.append(" opened, supported ABIS: ");
                stringBuilder.append(abiListString);
                Log.i("Zygote", stringBuilder.toString());
                return new ZygoteState(zygoteSocket, zygoteInputStream, zygoteWriter, Arrays.asList(abiListString.split(",")));
            } catch (IOException ex) {
                Log.e("Zygote", "I/O exception when connect", ex);
                try {
                    zygoteSocket.close();
                } catch (IOException ignore) {
                    Log.e("Zygote", "I/O exception on routine close when connect fail", ignore);
                }
                throw ex;
            }
        }

        boolean matches(String abi) {
            return this.abiList.contains(abi);
        }

        public void close() {
            try {
                this.socket.close();
            } catch (IOException ex) {
                Log.e(ZygoteProcess.LOG_TAG, "I/O exception on routine close", ex);
            }
            this.mClosed = true;
        }

        boolean isClosed() {
            return this.mClosed;
        }
    }

    public ZygoteProcess(String primarySocket, String secondarySocket) {
        this(new LocalSocketAddress(primarySocket, Namespace.RESERVED), new LocalSocketAddress(secondarySocket, Namespace.RESERVED));
    }

    public ZygoteProcess(LocalSocketAddress primarySocket, LocalSocketAddress secondarySocket) {
        this.mLock = new Object();
        this.mApiBlacklistExemptions = Collections.emptyList();
        this.mSocket = primarySocket;
        this.mSecondarySocket = secondarySocket;
    }

    public LocalSocketAddress getPrimarySocketAddress() {
        return this.mSocket;
    }

    public final ProcessStartResult start(String processClass, String niceName, int uid, int gid, int[] gids, int runtimeFlags, int mountExternal, int targetSdkVersion, String seInfo, String abi, String instructionSet, String appDataDir, String invokeWith, String[] zygoteArgs) {
        try {
            return startViaZygote(processClass, niceName, uid, gid, gids, runtimeFlags, mountExternal, targetSdkVersion, seInfo, abi, instructionSet, appDataDir, invokeWith, false, zygoteArgs);
        } catch (ZygoteStartFailedEx ex) {
            ZygoteStartFailedEx zygoteStartFailedEx = ex;
            Log.e(LOG_TAG, "Starting VM process through Zygote failed");
            throw new RuntimeException("Starting VM process through Zygote failed", ex);
        }
    }

    @GuardedBy("mLock")
    private static String getAbiList(BufferedWriter writer, DataInputStream inputStream) throws IOException {
        writer.write("1");
        writer.newLine();
        writer.write("--query-abi-list");
        writer.newLine();
        writer.flush();
        byte[] bytes = new byte[inputStream.readInt()];
        inputStream.readFully(bytes);
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    @GuardedBy("mLock")
    private static ProcessStartResult zygoteSendArgsAndGetResult(ZygoteState zygoteState, ArrayList<String> args) throws ZygoteStartFailedEx {
        try {
            int sz = args.size();
            int i = 0;
            int i2 = 0;
            while (i2 < sz) {
                if (((String) args.get(i2)).indexOf(10) < 0) {
                    i2++;
                } else {
                    throw new ZygoteStartFailedEx("embedded newlines not allowed");
                }
            }
            BufferedWriter writer = zygoteState.writer;
            DataInputStream inputStream = zygoteState.inputStream;
            writer.write(Integer.toString(args.size()));
            writer.newLine();
            while (i < sz) {
                writer.write((String) args.get(i));
                writer.newLine();
                i++;
            }
            writer.flush();
            ProcessStartResult result = new ProcessStartResult();
            result.pid = inputStream.readInt();
            result.usingWrapper = inputStream.readBoolean();
            if (result.pid >= 0) {
                return result;
            }
            throw new ZygoteStartFailedEx("fork() failed");
        } catch (IOException ex) {
            zygoteState.close();
            throw new ZygoteStartFailedEx(ex);
        }
    }

    private ProcessStartResult startViaZygote(String processClass, String niceName, int uid, int gid, int[] gids, int runtimeFlags, int mountExternal, int targetSdkVersion, String seInfo, String abi, String instructionSet, String appDataDir, String invokeWith, boolean startChildZygote, String[] extraArgs) throws ZygoteStartFailedEx {
        int sz;
        ProcessStartResult zygoteSendArgsAndGetResult;
        String str = niceName;
        int[] iArr = gids;
        int i = mountExternal;
        String str2 = seInfo;
        String str3 = instructionSet;
        String str4 = appDataDir;
        String str5 = invokeWith;
        String[] strArr = extraArgs;
        ArrayList<String> argsForZygote = new ArrayList();
        argsForZygote.add("--runtime-args");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("--setuid=");
        stringBuilder.append(uid);
        argsForZygote.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("--setgid=");
        stringBuilder.append(gid);
        argsForZygote.add(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("--runtime-flags=");
        stringBuilder.append(runtimeFlags);
        argsForZygote.add(stringBuilder.toString());
        if (i == 1) {
            argsForZygote.add("--mount-external-default");
        } else if (i == 2) {
            argsForZygote.add("--mount-external-read");
        } else if (i == 3) {
            argsForZygote.add("--mount-external-write");
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("--target-sdk-version=");
        stringBuilder.append(targetSdkVersion);
        argsForZygote.add(stringBuilder.toString());
        if (iArr != null && iArr.length > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("--setgroups=");
            sz = iArr.length;
            int i2 = 0;
            while (true) {
                i = i2;
                if (i >= sz) {
                    break;
                }
                int sz2;
                if (i != 0) {
                    sz2 = sz;
                    sb.append(44);
                } else {
                    sz2 = sz;
                }
                sb.append(iArr[i]);
                i2 = i + 1;
                sz = sz2;
                i = mountExternal;
            }
            argsForZygote.add(sb.toString());
        }
        if (str != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("--nice-name=");
            stringBuilder.append(str);
            argsForZygote.add(stringBuilder.toString());
        }
        if (str2 != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("--seinfo=");
            stringBuilder.append(str2);
            argsForZygote.add(stringBuilder.toString());
        }
        if (str3 != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("--instruction-set=");
            stringBuilder.append(str3);
            argsForZygote.add(stringBuilder.toString());
        }
        if (str4 != null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("--app-data-dir=");
            stringBuilder.append(str4);
            argsForZygote.add(stringBuilder.toString());
        }
        if (str5 != null) {
            argsForZygote.add("--invoke-with");
            argsForZygote.add(str5);
        }
        if (startChildZygote) {
            argsForZygote.add("--start-child-zygote");
        }
        argsForZygote.add(processClass);
        if (strArr != null) {
            sz = strArr.length;
            int i3 = 0;
            while (i3 < sz) {
                int i4 = sz;
                argsForZygote.add(strArr[i3]);
                i3++;
                sz = i4;
            }
        }
        synchronized (this.mLock) {
            zygoteSendArgsAndGetResult = zygoteSendArgsAndGetResult(openZygoteSocketIfNeeded(abi), argsForZygote);
        }
        return zygoteSendArgsAndGetResult;
    }

    public void close() {
        if (this.primaryZygoteState != null) {
            this.primaryZygoteState.close();
        }
        if (this.secondaryZygoteState != null) {
            this.secondaryZygoteState.close();
        }
    }

    public void establishZygoteConnectionForAbi(String abi) {
        try {
            synchronized (this.mLock) {
                openZygoteSocketIfNeeded(abi);
            }
        } catch (ZygoteStartFailedEx ex) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to connect to zygote for abi: ");
            stringBuilder.append(abi);
            throw new RuntimeException(stringBuilder.toString(), ex);
        }
    }

    public boolean setApiBlacklistExemptions(List<String> exemptions) {
        boolean ok;
        synchronized (this.mLock) {
            this.mApiBlacklistExemptions = exemptions;
            ok = maybeSetApiBlacklistExemptions(this.primaryZygoteState, true);
            if (ok) {
                ok = maybeSetApiBlacklistExemptions(this.secondaryZygoteState, true);
            }
        }
        return ok;
    }

    public void setHiddenApiAccessLogSampleRate(int rate) {
        synchronized (this.mLock) {
            this.mHiddenApiAccessLogSampleRate = rate;
            maybeSetHiddenApiAccessLogSampleRate(this.primaryZygoteState);
            maybeSetHiddenApiAccessLogSampleRate(this.secondaryZygoteState);
        }
    }

    @GuardedBy("mLock")
    private boolean maybeSetApiBlacklistExemptions(ZygoteState state, boolean sendIfEmpty) {
        if (state == null || state.isClosed()) {
            Slog.e(LOG_TAG, "Can't set API blacklist exemptions: no zygote connection");
            return false;
        } else if (!sendIfEmpty && this.mApiBlacklistExemptions.isEmpty()) {
            return true;
        } else {
            try {
                int i;
                state.writer.write(Integer.toString(this.mApiBlacklistExemptions.size() + 1));
                state.writer.newLine();
                state.writer.write("--set-api-blacklist-exemptions");
                state.writer.newLine();
                for (i = 0; i < this.mApiBlacklistExemptions.size(); i++) {
                    state.writer.write((String) this.mApiBlacklistExemptions.get(i));
                    state.writer.newLine();
                }
                state.writer.flush();
                i = state.inputStream.readInt();
                if (i != 0) {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to set API blacklist exemptions; status ");
                    stringBuilder.append(i);
                    Slog.e(str, stringBuilder.toString());
                }
                return true;
            } catch (IOException ioe) {
                Slog.e(LOG_TAG, "Failed to set API blacklist exemptions", ioe);
                this.mApiBlacklistExemptions = Collections.emptyList();
                return false;
            }
        }
    }

    /* JADX WARNING: Missing block: B:13:0x006c, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void maybeSetHiddenApiAccessLogSampleRate(ZygoteState state) {
        if (state != null && !state.isClosed() && this.mHiddenApiAccessLogSampleRate != -1) {
            try {
                state.writer.write(Integer.toString(1));
                state.writer.newLine();
                BufferedWriter bufferedWriter = state.writer;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("--hidden-api-log-sampling-rate=");
                stringBuilder.append(Integer.toString(this.mHiddenApiAccessLogSampleRate));
                bufferedWriter.write(stringBuilder.toString());
                state.writer.newLine();
                state.writer.flush();
                int status = state.inputStream.readInt();
                if (status != 0) {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Failed to set hidden API log sampling rate; status ");
                    stringBuilder2.append(status);
                    Slog.e(str, stringBuilder2.toString());
                }
            } catch (IOException ioe) {
                Slog.e(LOG_TAG, "Failed to set hidden API log sampling rate", ioe);
            }
        }
    }

    @GuardedBy("mLock")
    private ZygoteState openZygoteSocketIfNeeded(String abi) throws ZygoteStartFailedEx {
        Preconditions.checkState(Thread.holdsLock(this.mLock), "ZygoteProcess lock not held");
        if (this.primaryZygoteState == null || this.primaryZygoteState.isClosed()) {
            try {
                this.primaryZygoteState = ZygoteState.connect(this.mSocket);
                maybeSetApiBlacklistExemptions(this.primaryZygoteState, false);
                maybeSetHiddenApiAccessLogSampleRate(this.primaryZygoteState);
            } catch (IOException ioe) {
                throw new ZygoteStartFailedEx("Error connecting to primary zygote", ioe);
            }
        }
        if (this.primaryZygoteState.matches(abi)) {
            return this.primaryZygoteState;
        }
        if (this.secondaryZygoteState == null || this.secondaryZygoteState.isClosed()) {
            try {
                this.secondaryZygoteState = ZygoteState.connect(this.mSecondarySocket);
                maybeSetApiBlacklistExemptions(this.secondaryZygoteState, false);
                maybeSetHiddenApiAccessLogSampleRate(this.secondaryZygoteState);
            } catch (IOException ioe2) {
                throw new ZygoteStartFailedEx("Error connecting to secondary zygote", ioe2);
            }
        }
        if (this.secondaryZygoteState.matches(abi)) {
            return this.secondaryZygoteState;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unsupported zygote ABI: ");
        stringBuilder.append(abi);
        throw new ZygoteStartFailedEx(stringBuilder.toString());
    }

    public boolean preloadPackageForAbi(String packagePath, String libsPath, String libFileName, String cacheKey, String abi) throws ZygoteStartFailedEx, IOException {
        boolean z;
        synchronized (this.mLock) {
            ZygoteState state = openZygoteSocketIfNeeded(abi);
            state.writer.write(WifiScanLog.EVENT_KEY5);
            state.writer.newLine();
            state.writer.write("--preload-package");
            state.writer.newLine();
            state.writer.write(packagePath);
            state.writer.newLine();
            state.writer.write(libsPath);
            state.writer.newLine();
            state.writer.write(libFileName);
            state.writer.newLine();
            state.writer.write(cacheKey);
            state.writer.newLine();
            state.writer.flush();
            z = state.inputStream.readInt() == 0;
        }
        return z;
    }

    public boolean preloadDefault(String abi) throws ZygoteStartFailedEx, IOException {
        boolean z;
        synchronized (this.mLock) {
            ZygoteState state = openZygoteSocketIfNeeded(abi);
            state.writer.write("1");
            state.writer.newLine();
            state.writer.write("--preload-default");
            state.writer.newLine();
            state.writer.flush();
            z = state.inputStream.readInt() == 0;
        }
        return z;
    }

    public static void waitForConnectionToZygote(String socketName) {
        waitForConnectionToZygote(new LocalSocketAddress(socketName, Namespace.RESERVED));
    }

    public static void waitForConnectionToZygote(LocalSocketAddress address) {
        int n = 20;
        while (n >= 0) {
            try {
                ZygoteState.connect(address).close();
                return;
            } catch (IOException ioe) {
                String str = LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Got error connecting to zygote, retrying. msg= ");
                stringBuilder.append(ioe.getMessage());
                Log.w(str, stringBuilder.toString());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                n--;
            }
        }
        String str2 = LOG_TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Failed to connect to Zygote through socket ");
        stringBuilder2.append(address.getName());
        Slog.wtf(str2, stringBuilder2.toString());
    }

    public ChildZygoteProcess startChildZygote(String processClass, String niceName, int uid, int gid, int[] gids, int runtimeFlags, String seInfo, String abi, String instructionSet) {
        StringBuilder stringBuilder = new StringBuilder();
        String str = processClass;
        stringBuilder.append(str);
        stringBuilder.append("/");
        stringBuilder.append(UUID.randomUUID().toString());
        LocalSocketAddress serverAddress = new LocalSocketAddress(stringBuilder.toString());
        String[] strArr = new String[1];
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("--zygote-socket=");
        stringBuilder2.append(serverAddress.getName());
        strArr[0] = stringBuilder2.toString();
        try {
            return new ChildZygoteProcess(serverAddress, startViaZygote(str, niceName, uid, gid, gids, runtimeFlags, 0, 0, seInfo, abi, instructionSet, null, null, true, strArr).pid);
        } catch (ZygoteStartFailedEx ex) {
            ZygoteStartFailedEx zygoteStartFailedEx = ex;
            throw new RuntimeException("Starting child-zygote through Zygote failed", ex);
        }
    }

    public final boolean updateHwThemeZipsAndSomeIcons(int currentUserId) {
        boolean ret = true;
        String abi64 = null;
        String abi32 = null;
        if (Build.SUPPORTED_64_BIT_ABIS.length > 0) {
            abi64 = Build.SUPPORTED_64_BIT_ABIS[0];
        }
        if (Build.SUPPORTED_32_BIT_ABIS.length > 0) {
            abi32 = Build.SUPPORTED_32_BIT_ABIS[0];
        }
        ArrayList<String> argsForZygote = new ArrayList();
        argsForZygote.add("updateHwThemeZipsAndIcons");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setuid=");
        stringBuilder.append(currentUserId);
        argsForZygote.add(stringBuilder.toString());
        if (abi64 != null && abi64.equals("arm64-v8a")) {
            ret = zygoteSendArgsForUpdateHwThemes(abi64, argsForZygote);
        }
        String str = LOG_TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("updateHwThemeZipsAndSomeIcons abi=, ");
        stringBuilder2.append(abi32);
        Log.i(str, stringBuilder2.toString());
        if (abi32 == null || !abi32.equals("armeabi-v7a")) {
            return ret;
        }
        return zygoteSendArgsForUpdateHwThemes(abi32, argsForZygote);
    }

    private final boolean zygoteSendArgsForUpdateHwThemes(String abi, ArrayList<String> argsForZygote) {
        synchronized (this.mLock) {
            if (abi != null) {
                try {
                    zygoteSendArgsAndGetResult(openZygoteSocketIfNeeded(abi), argsForZygote);
                    return true;
                } catch (ZygoteStartFailedEx e) {
                    String str = LOG_TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("zygoteSendArgsForUpdateHwThemes  abi ");
                    stringBuilder.append(abi);
                    stringBuilder.append(" fail");
                    Log.e(str, stringBuilder.toString());
                    return false;
                } catch (Throwable th) {
                }
            }
        }
    }
}
