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
import java.util.Arrays;
import libcore.io.IoUtils;

class ZygoteConnection {
    private static final String TAG = "Zygote";
    private static final int[][] intArray2d = ((int[][]) Array.newInstance(int.class, new int[]{0, 0}));
    private final String abiList;
    private boolean isEof;
    private final LocalSocket mSocket;
    private final DataOutputStream mSocketOutStream;
    private final BufferedReader mSocketReader;
    private final Credentials peer;

    static class Arguments {
        boolean abiListQuery;
        String[] apiBlacklistExemptions;
        String appDataDir;
        boolean capabilitiesSpecified;
        long effectiveCapabilities;
        int gid = 0;
        boolean gidSpecified;
        int[] gids;
        int hiddenApiAccessLogSampleRate = -1;
        String instructionSet;
        String invokeWith;
        int mountExternal = 0;
        String niceName;
        long permittedCapabilities;
        boolean preloadDefault;
        String preloadPackage;
        String preloadPackageCacheKey;
        String preloadPackageLibFileName;
        String preloadPackageLibs;
        String[] remainingArgs;
        ArrayList<int[]> rlimits;
        int runtimeFlags;
        String seInfo;
        boolean seInfoSpecified;
        boolean startChildZygote;
        int targetSdkVersion;
        boolean targetSdkVersionSpecified;
        int uid = 0;
        boolean uidSpecified;

        Arguments(String[] args) throws IllegalArgumentException {
            parseArgs(args);
        }

        private void parseArgs(String[] args) throws IllegalArgumentException {
            int i;
            int i2;
            boolean seenRuntimeArgs = false;
            int curArg = 0;
            boolean expectRuntimeArgs = true;
            while (true) {
                i = 0;
                if (curArg >= args.length) {
                    break;
                }
                String arg = args[curArg];
                if (arg.equals("--")) {
                    curArg++;
                    break;
                }
                if (!arg.startsWith("--setuid=")) {
                    if (!arg.startsWith("--setgid=")) {
                        if (!arg.startsWith("--target-sdk-version=")) {
                            if (!arg.equals("--runtime-args")) {
                                if (!arg.startsWith("--runtime-flags=")) {
                                    if (!arg.startsWith("--seinfo=")) {
                                        if (!arg.startsWith("--capabilities=")) {
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
                                                                                            if (!arg.equals("--start-child-zygote")) {
                                                                                                if (!arg.equals("--set-api-blacklist-exemptions")) {
                                                                                                    if (!arg.startsWith("--hidden-api-log-sampling-rate=")) {
                                                                                                        break;
                                                                                                    }
                                                                                                    String rateStr = arg.substring(arg.indexOf(61) + 1);
                                                                                                    try {
                                                                                                        this.hiddenApiAccessLogSampleRate = Integer.parseInt(rateStr);
                                                                                                        expectRuntimeArgs = false;
                                                                                                    } catch (NumberFormatException nfe) {
                                                                                                        StringBuilder stringBuilder = new StringBuilder();
                                                                                                        stringBuilder.append("Invalid log sampling rate: ");
                                                                                                        stringBuilder.append(rateStr);
                                                                                                        throw new IllegalArgumentException(stringBuilder.toString(), nfe);
                                                                                                    }
                                                                                                }
                                                                                                this.apiBlacklistExemptions = (String[]) Arrays.copyOfRange(args, curArg + 1, args.length);
                                                                                                curArg = args.length;
                                                                                                expectRuntimeArgs = false;
                                                                                            } else {
                                                                                                this.startChildZygote = true;
                                                                                            }
                                                                                        } else {
                                                                                            this.preloadDefault = true;
                                                                                            expectRuntimeArgs = false;
                                                                                        }
                                                                                    } else {
                                                                                        curArg++;
                                                                                        this.preloadPackage = args[curArg];
                                                                                        curArg++;
                                                                                        this.preloadPackageLibs = args[curArg];
                                                                                        curArg++;
                                                                                        this.preloadPackageLibFileName = args[curArg];
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
                                                        } else if (this.niceName == null) {
                                                            this.niceName = arg.substring(arg.indexOf(61) + 1);
                                                        } else {
                                                            throw new IllegalArgumentException("Duplicate arg specified");
                                                        }
                                                    } else if (this.invokeWith == null) {
                                                        curArg++;
                                                        try {
                                                            this.invokeWith = args[curArg];
                                                        } catch (IndexOutOfBoundsException e) {
                                                            throw new IllegalArgumentException("--invoke-with requires argument");
                                                        }
                                                    } else {
                                                        throw new IllegalArgumentException("Duplicate arg specified");
                                                    }
                                                } else if (this.gids == null) {
                                                    String[] params = arg.substring(arg.indexOf(61) + 1).split(",");
                                                    this.gids = new int[params.length];
                                                    for (i2 = params.length - 1; i2 >= 0; i2--) {
                                                        this.gids[i2] = Integer.parseInt(params[i2]);
                                                    }
                                                } else {
                                                    throw new IllegalArgumentException("Duplicate arg specified");
                                                }
                                            }
                                            String[] limitStrings = arg.substring(arg.indexOf(61) + 1).split(",");
                                            if (limitStrings.length == 3) {
                                                int[] rlimitTuple = new int[limitStrings.length];
                                                while (i < limitStrings.length) {
                                                    rlimitTuple[i] = Integer.parseInt(limitStrings[i]);
                                                    i++;
                                                }
                                                if (this.rlimits == null) {
                                                    this.rlimits = new ArrayList();
                                                }
                                                this.rlimits.add(rlimitTuple);
                                            } else {
                                                throw new IllegalArgumentException("--rlimit= should have 3 comma-delimited ints");
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
                                    this.runtimeFlags = Integer.parseInt(arg.substring(arg.indexOf(61) + 1));
                                }
                            } else {
                                seenRuntimeArgs = true;
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
            } else if (expectRuntimeArgs) {
                if (seenRuntimeArgs) {
                    this.remainingArgs = new String[(args.length - curArg)];
                    System.arraycopy(args, curArg, this.remainingArgs, 0, this.remainingArgs.length);
                } else {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unexpected argument : ");
                    stringBuilder2.append(args.length > curArg ? args[curArg] : "args.length <= curArg");
                    throw new IllegalArgumentException(stringBuilder2.toString());
                }
            }
            if (this.startChildZygote) {
                boolean seenChildSocketArg = false;
                String[] strArr = this.remainingArgs;
                i2 = strArr.length;
                while (i < i2) {
                    if (strArr[i].startsWith(Zygote.CHILD_ZYGOTE_SOCKET_NAME_ARG)) {
                        seenChildSocketArg = true;
                        break;
                    }
                    i++;
                }
                if (!seenChildSocketArg) {
                    throw new IllegalArgumentException("--start-child-zygote specified without --zygote-socket=");
                }
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
        Throwable th;
        Arguments parsedArgs = null;
        try {
            String[] args = readArgumentList();
            FileDescriptor[] descriptors = this.mSocket.getAncillaryFileDescriptors();
            if (args == null) {
                this.isEof = true;
                return null;
            } else if (args == null || args.length != 2 || args[0] == null || !args[0].equals("updateHwThemeZipsAndIcons")) {
                int[] fdsToIgnore = null;
                FileDescriptor childPipeFd = null;
                parsedArgs = new Arguments(args);
                int pid;
                if (parsedArgs.abiListQuery) {
                    handleAbiListQuery();
                    return null;
                } else if (parsedArgs.preloadDefault) {
                    handlePreload();
                    return null;
                } else if (parsedArgs.preloadPackage != null) {
                    handlePreloadPackage(parsedArgs.preloadPackage, parsedArgs.preloadPackageLibs, parsedArgs.preloadPackageLibFileName, parsedArgs.preloadPackageCacheKey);
                    return null;
                } else if (parsedArgs.apiBlacklistExemptions != null) {
                    handleApiBlacklistExemptions(parsedArgs.apiBlacklistExemptions);
                    return null;
                } else if (parsedArgs.hiddenApiAccessLogSampleRate != -1) {
                    handleHiddenApiAccessLogSampleRate(parsedArgs.hiddenApiAccessLogSampleRate);
                    return null;
                } else if (parsedArgs.permittedCapabilities == 0 && parsedArgs.effectiveCapabilities == 0) {
                    applyUidSecurityPolicy(parsedArgs, this.peer);
                    applyInvokeWithSecurityPolicy(parsedArgs, this.peer);
                    applyDebuggerSystemProperty(parsedArgs);
                    applyInvokeWithSystemProperty(parsedArgs);
                    int[][] rlimits = null;
                    if (parsedArgs.rlimits != null) {
                        rlimits = (int[][]) parsedArgs.rlimits.toArray(intArray2d);
                    }
                    int[] fdsToIgnore2 = null;
                    if (parsedArgs.invokeWith != null) {
                        try {
                            FileDescriptor[] pipeFds = Os.pipe2(OsConstants.O_CLOEXEC);
                            fdsToIgnore = pipeFds[1];
                            childPipeFd = pipeFds[0];
                            Os.fcntlInt(fdsToIgnore, OsConstants.F_SETFD, 0);
                            fdsToIgnore2 = new int[]{fdsToIgnore.getInt$(), childPipeFd.getInt$()};
                        } catch (ErrnoException errnoEx) {
                            throw new IllegalStateException("Unable to set up pipe for invoke-with", errnoEx);
                        }
                    }
                    FileDescriptor serverPipeFd = childPipeFd;
                    childPipeFd = fdsToIgnore;
                    FileDescriptor childPipeFd2 = fdsToIgnore2;
                    int[] fdsToClose = new int[]{-1, -1};
                    FileDescriptor fd = this.mSocket.getFileDescriptor();
                    if (fd != null) {
                        fdsToClose[0] = fd.getInt$();
                    }
                    fd = zygoteServer.getServerSocketFileDescriptor();
                    if (fd != null) {
                        fdsToClose[1] = fd.getInt$();
                    }
                    FileDescriptor fd2 = null;
                    pid = -1;
                    FileDescriptor[] descriptors2 = descriptors;
                    descriptors = serverPipeFd;
                    int pid2 = Zygote.forkAndSpecialize(parsedArgs.uid, parsedArgs.gid, parsedArgs.gids, parsedArgs.runtimeFlags, rlimits, parsedArgs.mountExternal, parsedArgs.seInfo, parsedArgs.niceName, fdsToClose, childPipeFd2, parsedArgs.startChildZygote, parsedArgs.instructionSet, parsedArgs.appDataDir);
                    FileDescriptor[] fileDescriptorArr;
                    if (pid2 == 0) {
                        try {
                            zygoteServer.setForkChild();
                            zygoteServer.closeServerSocket();
                            IoUtils.closeQuietly(descriptors);
                        } catch (Throwable th2) {
                            th = th2;
                            fileDescriptorArr = descriptors2;
                            IoUtils.closeQuietly(childPipeFd);
                            IoUtils.closeQuietly(descriptors);
                            throw th;
                        }
                        try {
                            try {
                                Runnable handleChildProc = handleChildProc(parsedArgs, descriptors2, childPipeFd, parsedArgs.startChildZygote);
                                IoUtils.closeQuietly(childPipeFd);
                                IoUtils.closeQuietly(null);
                                return handleChildProc;
                            } catch (Throwable th3) {
                                th = th3;
                                descriptors = null;
                                IoUtils.closeQuietly(childPipeFd);
                                IoUtils.closeQuietly(descriptors);
                                throw th;
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            fileDescriptorArr = descriptors2;
                            descriptors = null;
                            IoUtils.closeQuietly(childPipeFd);
                            IoUtils.closeQuietly(descriptors);
                            throw th;
                        }
                    }
                    fileDescriptorArr = descriptors2;
                    try {
                        IoUtils.closeQuietly(childPipeFd);
                        childPipeFd = null;
                        handleParentProc(pid2, fileDescriptorArr, descriptors);
                        IoUtils.closeQuietly(null);
                        IoUtils.closeQuietly(descriptors);
                        return null;
                    } catch (Throwable th5) {
                        th = th5;
                        IoUtils.closeQuietly(childPipeFd);
                        IoUtils.closeQuietly(descriptors);
                        throw th;
                    }
                } else {
                    String[] strArr = args;
                    pid = -1;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Client may not specify capabilities: permitted=0x");
                    stringBuilder.append(Long.toHexString(parsedArgs.permittedCapabilities));
                    stringBuilder.append(", effective=0x");
                    stringBuilder.append(Long.toHexString(parsedArgs.effectiveCapabilities));
                    throw new ZygoteSecurityException(stringBuilder.toString());
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
                    } catch (IOException ex) {
                        Log.e(TAG, "Error writing to command socket", ex);
                    }
                } catch (Exception ex2) {
                    Log.e(TAG, "updateHwThemeZipsAndSomeIcons", ex2);
                    this.mSocketOutStream.writeInt(1);
                    this.mSocketOutStream.writeBoolean(false);
                } catch (Throwable th6) {
                    Throwable th7 = th6;
                    try {
                        this.mSocketOutStream.writeInt(1);
                        this.mSocketOutStream.writeBoolean(false);
                    } catch (IOException ex3) {
                        Log.e(TAG, "Error writing to command socket", ex3);
                    }
                }
                return null;
            }
        } catch (IOException ex32) {
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

    private void handleApiBlacklistExemptions(String[] exemptions) {
        try {
            ZygoteInit.setApiBlacklistExemptions(exemptions);
            this.mSocketOutStream.writeInt(0);
        } catch (IOException ioe) {
            throw new IllegalStateException("Error writing to command socket", ioe);
        }
    }

    private void handleHiddenApiAccessLogSampleRate(int percent) {
        try {
            ZygoteInit.setHiddenApiAccessLogSampleRate(percent);
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

    protected void handlePreloadPackage(String packagePath, String libsPath, String libFileName, String cacheKey) {
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
            if (argc <= 1024) {
                String[] result = new String[argc];
                int i = 0;
                while (i < argc) {
                    result[i] = this.mSocketReader.readLine();
                    if (result[i] != null) {
                        i++;
                    } else {
                        throw new IOException("truncated request");
                    }
                }
                return result;
            }
            throw new IOException("max arg count exceeded");
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
            args.runtimeFlags |= 1;
        }
    }

    private static void applyInvokeWithSecurityPolicy(Arguments args, Credentials peer) throws ZygoteSecurityException {
        int peerUid = peer.getUid();
        if (args.invokeWith != null && peerUid != 0 && (args.runtimeFlags & 1) == 0) {
            throw new ZygoteSecurityException("Peer is permitted to specify anexplicit invoke-with wrapper command only for debuggableapplications.");
        }
    }

    public static void applyInvokeWithSystemProperty(Arguments args) {
        if (args.invokeWith == null && args.niceName != null) {
            String property = new StringBuilder();
            property.append("wrap.");
            property.append(args.niceName);
            args.invokeWith = SystemProperties.get(property.toString());
            if (args.invokeWith != null && args.invokeWith.length() == 0) {
                args.invokeWith = null;
            }
        }
    }

    private Runnable handleChildProc(Arguments parsedArgs, FileDescriptor[] descriptors, FileDescriptor pipeFd, boolean isZygote) {
        closeSocket();
        if (descriptors != null) {
            int i = 0;
            try {
                Os.dup2(descriptors[0], OsConstants.STDIN_FILENO);
                Os.dup2(descriptors[1], OsConstants.STDOUT_FILENO);
                Os.dup2(descriptors[2], OsConstants.STDERR_FILENO);
                int length = descriptors.length;
                while (i < length) {
                    IoUtils.closeQuietly(descriptors[i]);
                    i++;
                }
            } catch (ErrnoException ex) {
                Log.e(TAG, "Error reopening stdio", ex);
            }
        }
        if (parsedArgs.niceName != null) {
            Process.setArgV0(parsedArgs.niceName);
        }
        Trace.traceEnd(64);
        if (parsedArgs.invokeWith != null) {
            WrapperInit.execApplication(parsedArgs.invokeWith, parsedArgs.niceName, parsedArgs.targetSdkVersion, VMRuntime.getCurrentInstructionSet(), pipeFd, parsedArgs.remainingArgs);
            throw new IllegalStateException("WrapperInit.execApplication unexpectedly returned");
        } else if (isZygote) {
            return ZygoteInit.childZygoteInit(parsedArgs.targetSdkVersion, parsedArgs.remainingArgs, null);
        } else {
            return ZygoteInit.zygoteInit(parsedArgs.targetSdkVersion, parsedArgs.remainingArgs, null);
        }
    }

    private void handleParentProc(int pid, FileDescriptor[] descriptors, FileDescriptor pipeFd) {
        int poll;
        int pid2;
        int i = pid;
        FileDescriptor[] fileDescriptorArr = descriptors;
        FileDescriptor fileDescriptor = pipeFd;
        if (i > 0) {
            setChildPgid(pid);
        }
        int i2 = 0;
        if (fileDescriptorArr != null) {
            for (FileDescriptor fd : fileDescriptorArr) {
                IoUtils.closeQuietly(fd);
            }
        }
        boolean usingWrapper = false;
        if (fileDescriptor != null && i > 0) {
            int innerPid = -1;
            try {
                StructPollfd[] fds = new StructPollfd[]{new StructPollfd()};
                byte[] data = new byte[4];
                int remainingSleepTime = 30000;
                int dataIndex = 0;
                long startTime = System.nanoTime();
                while (dataIndex < data.length && remainingSleepTime > 0) {
                    fds[i2].fd = fileDescriptor;
                    fds[i2].events = (short) OsConstants.POLLIN;
                    fds[i2].revents = i2;
                    fds[i2].userData = null;
                    poll = Os.poll(fds, remainingSleepTime);
                    remainingSleepTime = 30000 - ((int) ((System.nanoTime() - startTime) / 1000000));
                    if (poll > 0) {
                        if ((fds[0].revents & OsConstants.POLLIN) == 0) {
                            break;
                        }
                        int readBytes = Os.read(fileDescriptor, data, dataIndex, 1);
                        if (readBytes >= 0) {
                            dataIndex += readBytes;
                        } else {
                            throw new RuntimeException("Some error");
                        }
                    } else if (poll == 0) {
                        Log.w(TAG, "Timed out waiting for child.");
                    }
                    i2 = 0;
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
                i2 = innerPid;
                while (i2 > 0 && i2 != i) {
                    i2 = Process.getParentPid(i2);
                }
                String str;
                StringBuilder stringBuilder;
                if (i2 > 0) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Wrapped process has pid ");
                    stringBuilder.append(innerPid);
                    Log.i(str, stringBuilder.toString());
                    pid2 = innerPid;
                    usingWrapper = true;
                    this.mSocketOutStream.writeInt(pid2);
                    this.mSocketOutStream.writeBoolean(usingWrapper);
                }
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Wrapped process reported a pid that is not a child of the process that we forked: childPid=");
                stringBuilder.append(i);
                stringBuilder.append(" innerPid=");
                stringBuilder.append(innerPid);
                Log.w(str, stringBuilder.toString());
            }
        }
        pid2 = i;
        try {
            this.mSocketOutStream.writeInt(pid2);
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
