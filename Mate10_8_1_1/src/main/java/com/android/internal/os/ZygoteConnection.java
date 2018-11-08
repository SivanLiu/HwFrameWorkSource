package com.android.internal.os;

import android.net.Credentials;
import android.net.LocalSocket;
import android.os.FactoryTest;
import android.os.Process;
import android.os.SystemProperties;
import android.os.Trace;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructPollfd;
import android.util.Log;
import android.util.TimeUtils;
import dalvik.system.VMRuntime;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import libcore.io.IoUtils;

class ZygoteConnection {
    private static final String TAG = "Zygote";
    private static final int[][] intArray2d = ((int[][]) Array.newInstance(Integer.TYPE, new int[]{0, 0}));
    private final String abiList;
    private boolean isEof;
    private final LocalSocket mSocket;
    private final DataOutputStream mSocketOutStream;
    private final BufferedReader mSocketReader;
    private final Credentials peer;

    static class Arguments {
        boolean abiListQuery;
        String appDataDir;
        boolean capabilitiesSpecified;
        int debugFlags;
        long effectiveCapabilities;
        int gid = 0;
        boolean gidSpecified;
        int[] gids;
        String instructionSet;
        String invokeWith;
        int mountExternal = 0;
        String niceName;
        long permittedCapabilities;
        boolean preloadDefault;
        String preloadPackage;
        String preloadPackageCacheKey;
        String preloadPackageLibs;
        String[] remainingArgs;
        ArrayList<int[]> rlimits;
        String seInfo;
        boolean seInfoSpecified;
        int targetSdkVersion;
        boolean targetSdkVersionSpecified;
        int uid = 0;
        boolean uidSpecified;

        Arguments(String[] args) throws IllegalArgumentException {
            parseArgs(args);
        }

        private void parseArgs(String[] args) throws IllegalArgumentException {
            int curArg = 0;
            boolean seenRuntimeArgs = false;
            while (curArg < args.length) {
                String arg = args[curArg];
                if (arg.equals("--")) {
                    curArg++;
                    break;
                }
                if (!arg.startsWith("--setuid=")) {
                    if (!arg.startsWith("--setgid=")) {
                        if (!arg.startsWith("--target-sdk-version=")) {
                            if (!arg.equals("--enable-jdwp")) {
                                if (!arg.equals("--enable-safemode")) {
                                    if (!arg.equals("--enable-checkjni")) {
                                        if (!arg.equals("--generate-debug-info")) {
                                            if (!arg.equals("--always-jit")) {
                                                if (!arg.equals("--native-debuggable")) {
                                                    if (!arg.equals("--java-debuggable")) {
                                                        if (!arg.equals("--enable-jni-logging")) {
                                                            if (!arg.equals("--enable-assert")) {
                                                                if (!arg.equals("--runtime-args")) {
                                                                    if (!arg.startsWith("--seinfo=")) {
                                                                        if (!arg.startsWith("--capabilities=")) {
                                                                            int i;
                                                                            if (!arg.startsWith("--rlimit=")) {
                                                                                if (!arg.startsWith("--setgroups=")) {
                                                                                    if (!arg.equals("--invoke-with")) {
                                                                                        if (!arg.startsWith("--nice-name=")) {
                                                                                            if (!arg.equals("--mount-external-default")) {
                                                                                                if (!arg.equals("--mount-external-read")) {
                                                                                                    if (!arg.equals("--mount-external-write")) {
                                                                                                        if (!arg.equals("--query-abi-list")) {
                                                                                                            if (!arg.startsWith("--instruction-set=")) {
                                                                                                                if (!arg.startsWith("--app-data-dir=")) {
                                                                                                                    if (!arg.equals("--preload-package")) {
                                                                                                                        if (!arg.equals("--preload-default")) {
                                                                                                                            break;
                                                                                                                        }
                                                                                                                        this.preloadDefault = true;
                                                                                                                    } else {
                                                                                                                        curArg++;
                                                                                                                        this.preloadPackage = args[curArg];
                                                                                                                        curArg++;
                                                                                                                        this.preloadPackageLibs = args[curArg];
                                                                                                                        curArg++;
                                                                                                                        this.preloadPackageCacheKey = args[curArg];
                                                                                                                    }
                                                                                                                } else {
                                                                                                                    this.appDataDir = arg.substring(arg.indexOf(61) + 1);
                                                                                                                }
                                                                                                            } else {
                                                                                                                this.instructionSet = arg.substring(arg.indexOf(61) + 1);
                                                                                                            }
                                                                                                        } else {
                                                                                                            this.abiListQuery = true;
                                                                                                        }
                                                                                                    } else {
                                                                                                        this.mountExternal = 3;
                                                                                                    }
                                                                                                } else {
                                                                                                    this.mountExternal = 2;
                                                                                                }
                                                                                            } else {
                                                                                                this.mountExternal = 1;
                                                                                            }
                                                                                        } else if (this.niceName != null) {
                                                                                            throw new IllegalArgumentException("Duplicate arg specified");
                                                                                        } else {
                                                                                            this.niceName = arg.substring(arg.indexOf(61) + 1);
                                                                                        }
                                                                                    } else if (this.invokeWith != null) {
                                                                                        throw new IllegalArgumentException("Duplicate arg specified");
                                                                                    } else {
                                                                                        curArg++;
                                                                                        try {
                                                                                            this.invokeWith = args[curArg];
                                                                                        } catch (IndexOutOfBoundsException e) {
                                                                                            throw new IllegalArgumentException("--invoke-with requires argument");
                                                                                        }
                                                                                    }
                                                                                } else if (this.gids != null) {
                                                                                    throw new IllegalArgumentException("Duplicate arg specified");
                                                                                } else {
                                                                                    String[] params = arg.substring(arg.indexOf(61) + 1).split(",");
                                                                                    this.gids = new int[params.length];
                                                                                    for (i = params.length - 1; i >= 0; i--) {
                                                                                        this.gids[i] = Integer.parseInt(params[i]);
                                                                                    }
                                                                                }
                                                                            } else {
                                                                                String[] limitStrings = arg.substring(arg.indexOf(61) + 1).split(",");
                                                                                if (limitStrings.length != 3) {
                                                                                    throw new IllegalArgumentException("--rlimit= should have 3 comma-delimited ints");
                                                                                }
                                                                                int[] rlimitTuple = new int[limitStrings.length];
                                                                                for (i = 0; i < limitStrings.length; i++) {
                                                                                    rlimitTuple[i] = Integer.parseInt(limitStrings[i]);
                                                                                }
                                                                                if (this.rlimits == null) {
                                                                                    this.rlimits = new ArrayList();
                                                                                }
                                                                                this.rlimits.add(rlimitTuple);
                                                                            }
                                                                        } else if (this.capabilitiesSpecified) {
                                                                            throw new IllegalArgumentException("Duplicate arg specified");
                                                                        } else {
                                                                            this.capabilitiesSpecified = true;
                                                                            String[] capStrings = arg.substring(arg.indexOf(61) + 1).split(",", 2);
                                                                            if (capStrings.length == 1) {
                                                                                this.effectiveCapabilities = Long.decode(capStrings[0]).longValue();
                                                                                this.permittedCapabilities = this.effectiveCapabilities;
                                                                            } else {
                                                                                this.permittedCapabilities = Long.decode(capStrings[0]).longValue();
                                                                                this.effectiveCapabilities = Long.decode(capStrings[1]).longValue();
                                                                            }
                                                                        }
                                                                    } else if (this.seInfoSpecified) {
                                                                        throw new IllegalArgumentException("Duplicate arg specified");
                                                                    } else {
                                                                        this.seInfoSpecified = true;
                                                                        this.seInfo = arg.substring(arg.indexOf(61) + 1);
                                                                    }
                                                                } else {
                                                                    seenRuntimeArgs = true;
                                                                }
                                                            } else {
                                                                this.debugFlags |= 4;
                                                            }
                                                        } else {
                                                            this.debugFlags |= 16;
                                                        }
                                                    } else {
                                                        this.debugFlags |= 256;
                                                    }
                                                } else {
                                                    this.debugFlags |= 128;
                                                }
                                            } else {
                                                this.debugFlags |= 64;
                                            }
                                        } else {
                                            this.debugFlags |= 32;
                                        }
                                    } else {
                                        this.debugFlags |= 2;
                                    }
                                } else {
                                    this.debugFlags |= 8;
                                }
                            } else {
                                this.debugFlags |= 1;
                            }
                        } else if (this.targetSdkVersionSpecified) {
                            throw new IllegalArgumentException("Duplicate target-sdk-version specified");
                        } else {
                            this.targetSdkVersionSpecified = true;
                            this.targetSdkVersion = Integer.parseInt(arg.substring(arg.indexOf(61) + 1));
                        }
                    } else if (this.gidSpecified) {
                        throw new IllegalArgumentException("Duplicate arg specified");
                    } else {
                        this.gidSpecified = true;
                        this.gid = Integer.parseInt(arg.substring(arg.indexOf(61) + 1));
                    }
                } else if (this.uidSpecified) {
                    throw new IllegalArgumentException("Duplicate arg specified");
                } else {
                    this.uidSpecified = true;
                    this.uid = Integer.parseInt(arg.substring(arg.indexOf(61) + 1));
                }
                curArg++;
            }
            if (this.abiListQuery) {
                if (args.length - curArg > 0) {
                    throw new IllegalArgumentException("Unexpected arguments after --query-abi-list.");
                }
            } else if (this.preloadPackage != null) {
                if (args.length - curArg > 0) {
                    throw new IllegalArgumentException("Unexpected arguments after --preload-package.");
                }
            } else if (!this.preloadDefault) {
                if (seenRuntimeArgs) {
                    this.remainingArgs = new String[(args.length - curArg)];
                    System.arraycopy(args, curArg, this.remainingArgs, 0, this.remainingArgs.length);
                    return;
                }
                throw new IllegalArgumentException("Unexpected argument : " + (args.length > curArg ? args[curArg] : "args.length <= curArg"));
            }
        }
    }

    ZygoteConnection(LocalSocket socket, String abiList) throws IOException {
        this.mSocket = socket;
        this.abiList = abiList;
        this.mSocketOutStream = new DataOutputStream(socket.getOutputStream());
        this.mSocketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()), 256);
        this.mSocket.setSoTimeout(1000);
        try {
            this.peer = this.mSocket.getPeerCredentials();
            this.isEof = false;
        } catch (IOException ex) {
            Log.e(TAG, "Cannot read peer credentials", ex);
            throw ex;
        }
    }

    public DataOutputStream getDataOutputStream() {
        return this.mSocketOutStream;
    }

    FileDescriptor getFileDesciptor() {
        return this.mSocket.getFileDescriptor();
    }

    Runnable processOneCommand(ZygoteServer zygoteServer) {
        try {
            String[] args = readArgumentList();
            FileDescriptor[] descriptors = this.mSocket.getAncillaryFileDescriptors();
            if (args == null) {
                this.isEof = true;
                return null;
            } else if (args == null || args.length != 2 || args[0] == null || !args[0].equals("updateHwThemeZipsAndIcons")) {
                FileDescriptor fileDescriptor = null;
                FileDescriptor fileDescriptor2 = null;
                Arguments arguments = new Arguments(args);
                if (arguments.abiListQuery) {
                    handleAbiListQuery();
                    return null;
                } else if (arguments.preloadDefault) {
                    handlePreload();
                    return null;
                } else if (arguments.preloadPackage != null) {
                    handlePreloadPackage(arguments.preloadPackage, arguments.preloadPackageLibs, arguments.preloadPackageCacheKey);
                    return null;
                } else if (arguments.permittedCapabilities == 0 && arguments.effectiveCapabilities == 0) {
                    Runnable handleChildProc;
                    applyUidSecurityPolicy(arguments, this.peer);
                    applyInvokeWithSecurityPolicy(arguments, this.peer);
                    applyDebuggerSystemProperty(arguments);
                    applyInvokeWithSystemProperty(arguments);
                    int[][] iArr = null;
                    if (arguments.rlimits != null) {
                        iArr = (int[][]) arguments.rlimits.toArray(intArray2d);
                    }
                    int[] iArr2 = null;
                    if (arguments.invokeWith != null) {
                        try {
                            FileDescriptor[] pipeFds = Os.pipe2(OsConstants.O_CLOEXEC);
                            fileDescriptor = pipeFds[1];
                            fileDescriptor2 = pipeFds[0];
                            Os.fcntlInt(fileDescriptor, OsConstants.F_SETFD, 0);
                            iArr2 = new int[]{fileDescriptor.getInt$(), fileDescriptor2.getInt$()};
                        } catch (Throwable errnoEx) {
                            throw new IllegalStateException("Unable to set up pipe for invoke-with", errnoEx);
                        }
                    }
                    int[] fdsToClose = new int[]{-1, -1};
                    FileDescriptor fd = this.mSocket.getFileDescriptor();
                    if (fd != null) {
                        fdsToClose[0] = fd.getInt$();
                    }
                    fd = zygoteServer.getServerSocketFileDescriptor();
                    if (fd != null) {
                        fdsToClose[1] = fd.getInt$();
                    }
                    int pid = Zygote.forkAndSpecialize(arguments.uid, arguments.gid, arguments.gids, arguments.debugFlags, iArr, arguments.mountExternal, arguments.seInfo, arguments.niceName, fdsToClose, iArr2, arguments.instructionSet, arguments.appDataDir);
                    if (pid == 0) {
                        try {
                            zygoteServer.setForkChild();
                            zygoteServer.closeServerSocket();
                            IoUtils.closeQuietly(fileDescriptor2);
                            fileDescriptor2 = null;
                            handleChildProc = handleChildProc(arguments, descriptors, fileDescriptor);
                        } finally {
                            IoUtils.closeQuietly(fileDescriptor);
                            IoUtils.closeQuietly(fileDescriptor2);
                        }
                    } else {
                        IoUtils.closeQuietly(fileDescriptor);
                        fileDescriptor = null;
                        handleParentProc(pid, descriptors, fileDescriptor2);
                        IoUtils.closeQuietly(null);
                        IoUtils.closeQuietly(fileDescriptor2);
                        return null;
                    }
                    return handleChildProc;
                } else {
                    throw new ZygoteSecurityException("Client may not specify capabilities: permitted=0x" + Long.toHexString(arguments.permittedCapabilities) + ", effective=0x" + Long.toHexString(arguments.effectiveCapabilities));
                }
            } else {
                int currentUserId = 0;
                try {
                    if (args[1] != null && args[1].startsWith("setuid=")) {
                        currentUserId = Integer.parseInt(args[1].substring(args[1].indexOf(61) + 1));
                    }
                    ZygoteInit.clearHwThemeZipsAndSomeIcons();
                    ZygoteInit.preloadHwThemeZipsAndSomeIcons(currentUserId);
                    try {
                        this.mSocketOutStream.writeInt(1);
                        this.mSocketOutStream.writeBoolean(false);
                    } catch (Throwable ex) {
                        Log.e(TAG, "Error writing to command socket", ex);
                    }
                } catch (Throwable ex2) {
                    Log.e(TAG, "updateHwThemeZipsAndSomeIcons", ex2);
                } catch (Throwable th) {
                    try {
                        this.mSocketOutStream.writeInt(1);
                        this.mSocketOutStream.writeBoolean(false);
                    } catch (Throwable ex3) {
                        Log.e(TAG, "Error writing to command socket", ex3);
                    }
                }
                return null;
            }
        } catch (Throwable ex32) {
            throw new IllegalStateException("IOException on command socket", ex32);
        }
    }

    private void handleAbiListQuery() {
        try {
            byte[] abiListBytes = this.abiList.getBytes(StandardCharsets.US_ASCII);
            this.mSocketOutStream.writeInt(abiListBytes.length);
            this.mSocketOutStream.write(abiListBytes);
        } catch (IOException ioe) {
            throw new IllegalStateException("Error writing to command socket", ioe);
        }
    }

    private void handlePreload() {
        try {
            if (isPreloadComplete()) {
                this.mSocketOutStream.writeInt(1);
                return;
            }
            preload();
            this.mSocketOutStream.writeInt(0);
        } catch (IOException ioe) {
            throw new IllegalStateException("Error writing to command socket", ioe);
        }
    }

    protected void preload() {
        ZygoteInit.lazyPreload();
    }

    protected boolean isPreloadComplete() {
        return ZygoteInit.isPreloadComplete();
    }

    protected DataOutputStream getSocketOutputStream() {
        return this.mSocketOutStream;
    }

    protected void handlePreloadPackage(String packagePath, String libsPath, String cacheKey) {
        throw new RuntimeException("Zyogte does not support package preloading");
    }

    void closeSocket() {
        try {
            this.mSocket.close();
        } catch (IOException ex) {
            Log.e(TAG, "Exception while closing command socket in parent", ex);
        }
    }

    boolean isClosedByPeer() {
        return this.isEof;
    }

    private String[] readArgumentList() throws IOException {
        try {
            String s = this.mSocketReader.readLine();
            if (s == null) {
                return null;
            }
            int argc = Integer.parseInt(s);
            if (argc > 1024) {
                throw new IOException("max arg count exceeded");
            }
            String[] result = new String[argc];
            for (int i = 0; i < argc; i++) {
                result[i] = this.mSocketReader.readLine();
                if (result[i] == null) {
                    throw new IOException("truncated request");
                }
            }
            return result;
        } catch (NumberFormatException e) {
            Log.e(TAG, "invalid Zygote wire format: non-int at argc");
            throw new IOException("invalid wire format");
        }
    }

    private static void applyUidSecurityPolicy(Arguments args, Credentials peer) throws ZygoteSecurityException {
        if (peer.getUid() == 1000) {
            if ((FactoryTest.getMode() == 0) && args.uidSpecified && args.uid < 1000) {
                throw new ZygoteSecurityException("System UID may not launch process with UID < 1000");
            }
        }
        if (!args.uidSpecified) {
            args.uid = peer.getUid();
            args.uidSpecified = true;
        }
        if (!args.gidSpecified) {
            args.gid = peer.getGid();
            args.gidSpecified = true;
        }
    }

    public static void applyDebuggerSystemProperty(Arguments args) {
        if (RoSystemProperties.DEBUGGABLE) {
            args.debugFlags |= 1;
        }
    }

    private static void applyInvokeWithSecurityPolicy(Arguments args, Credentials peer) throws ZygoteSecurityException {
        int peerUid = peer.getUid();
        if (args.invokeWith != null && peerUid != 0 && (args.debugFlags & 1) == 0) {
            throw new ZygoteSecurityException("Peer is permitted to specify anexplicit invoke-with wrapper command only for debuggableapplications.");
        }
    }

    public static void applyInvokeWithSystemProperty(Arguments args) {
        if (args.invokeWith == null && args.niceName != null) {
            args.invokeWith = SystemProperties.get("wrap." + args.niceName);
            if (args.invokeWith != null && args.invokeWith.length() == 0) {
                args.invokeWith = null;
            }
        }
    }

    private Runnable handleChildProc(Arguments parsedArgs, FileDescriptor[] descriptors, FileDescriptor pipeFd) {
        closeSocket();
        if (descriptors != null) {
            try {
                Os.dup2(descriptors[0], OsConstants.STDIN_FILENO);
                Os.dup2(descriptors[1], OsConstants.STDOUT_FILENO);
                Os.dup2(descriptors[2], OsConstants.STDERR_FILENO);
                for (FileDescriptor fd : descriptors) {
                    IoUtils.closeQuietly(fd);
                }
            } catch (ErrnoException ex) {
                Log.e(TAG, "Error reopening stdio", ex);
            }
        }
        if (parsedArgs.niceName != null) {
            Process.setArgV0(parsedArgs.niceName);
        }
        Trace.traceEnd(64);
        if (parsedArgs.invokeWith == null) {
            return ZygoteInit.zygoteInit(parsedArgs.targetSdkVersion, parsedArgs.remainingArgs, null);
        }
        WrapperInit.execApplication(parsedArgs.invokeWith, parsedArgs.niceName, parsedArgs.targetSdkVersion, VMRuntime.getCurrentInstructionSet(), pipeFd, parsedArgs.remainingArgs);
        throw new IllegalStateException("WrapperInit.execApplication unexpectedly returned");
    }

    private void handleParentProc(int pid, FileDescriptor[] descriptors, FileDescriptor pipeFd) {
        if (pid > 0) {
            setChildPgid(pid);
        }
        if (descriptors != null) {
            for (FileDescriptor fd : descriptors) {
                IoUtils.closeQuietly(fd);
            }
        }
        boolean usingWrapper = false;
        if (pipeFd != null && pid > 0) {
            int innerPid = -1;
            try {
                StructPollfd[] fds = new StructPollfd[]{new StructPollfd()};
                byte[] data = new byte[4];
                int remainingSleepTime = 30000;
                int dataIndex = 0;
                long startTime = System.nanoTime();
                while (dataIndex < data.length && remainingSleepTime > 0) {
                    fds[0].fd = pipeFd;
                    fds[0].events = (short) OsConstants.POLLIN;
                    fds[0].revents = (short) 0;
                    fds[0].userData = null;
                    int res = Os.poll(fds, remainingSleepTime);
                    remainingSleepTime = 30000 - ((int) ((System.nanoTime() - startTime) / TimeUtils.NANOS_PER_MS));
                    if (res > 0) {
                        if ((fds[0].revents & OsConstants.POLLIN) == 0) {
                            break;
                        }
                        int readBytes = Os.read(pipeFd, data, dataIndex, 1);
                        if (readBytes < 0) {
                            throw new RuntimeException("Some error");
                        }
                        dataIndex += readBytes;
                    } else if (res == 0) {
                        Log.w(TAG, "Timed out waiting for child.");
                    } else {
                        continue;
                    }
                }
                if (dataIndex == data.length) {
                    innerPid = new DataInputStream(new ByteArrayInputStream(data)).readInt();
                }
                if (innerPid == -1) {
                    Log.w(TAG, "Error reading pid from wrapped process, child may have died");
                }
            } catch (Exception ex) {
                Log.w(TAG, "Error reading pid from wrapped process, child may have died", ex);
            }
            if (innerPid > 0) {
                int parentPid = innerPid;
                while (parentPid > 0 && parentPid != pid) {
                    parentPid = Process.getParentPid(parentPid);
                }
                if (parentPid > 0) {
                    Log.i(TAG, "Wrapped process has pid " + innerPid);
                    pid = innerPid;
                    usingWrapper = true;
                } else {
                    Log.w(TAG, "Wrapped process reported a pid that is not a child of the process that we forked: childPid=" + pid + " innerPid=" + innerPid);
                }
            }
        }
        try {
            this.mSocketOutStream.writeInt(pid);
            this.mSocketOutStream.writeBoolean(usingWrapper);
        } catch (IOException ex2) {
            throw new IllegalStateException("Error writing to command socket", ex2);
        }
    }

    private void setChildPgid(int pid) {
        try {
            Os.setpgid(pid, Os.getpgid(this.peer.getPid()));
        } catch (ErrnoException e) {
            Log.i(TAG, "Zygote: setpgid failed. This is normal if peer is not in our session");
        }
    }
}
