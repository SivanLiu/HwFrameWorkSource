package android.ddm;

import android.os.Debug;
import android.os.Process;
import android.os.UserHandle;
import dalvik.system.VMRuntime;
import java.nio.ByteBuffer;
import org.apache.harmony.dalvik.ddmc.Chunk;
import org.apache.harmony.dalvik.ddmc.ChunkHandler;
import org.apache.harmony.dalvik.ddmc.DdmServer;

public class DdmHandleHello extends ChunkHandler {
    public static final int CHUNK_FEAT = type("FEAT");
    public static final int CHUNK_HELO = type("HELO");
    public static final int CHUNK_WAIT = type("WAIT");
    private static final String[] FRAMEWORK_FEATURES = new String[]{"opengl-tracing", "view-hierarchy"};
    private static DdmHandleHello mInstance = new DdmHandleHello();

    private DdmHandleHello() {
    }

    public static void register() {
        DdmServer.registerHandler(CHUNK_HELO, mInstance);
        DdmServer.registerHandler(CHUNK_FEAT, mInstance);
    }

    public void connected() {
    }

    public void disconnected() {
    }

    public Chunk handleChunk(Chunk request) {
        int type = request.type;
        if (type == CHUNK_HELO) {
            return handleHELO(request);
        }
        if (type == CHUNK_FEAT) {
            return handleFEAT(request);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown packet ");
        stringBuilder.append(ChunkHandler.name(type));
        throw new RuntimeException(stringBuilder.toString());
    }

    private Chunk handleHELO(Chunk request) {
        int serverProtoVers = wrapChunk(request).getInt();
        String vmName = System.getProperty("java.vm.name", "?");
        String vmVersion = System.getProperty("java.vm.version", "?");
        String vmIdent = new StringBuilder();
        vmIdent.append(vmName);
        vmIdent.append(" v");
        vmIdent.append(vmVersion);
        vmIdent = vmIdent.toString();
        String appName = DdmHandleAppName.getAppName();
        VMRuntime vmRuntime = VMRuntime.getRuntime();
        String instructionSetDescription = vmRuntime.is64Bit() ? "64-bit" : "32-bit";
        String vmInstructionSet = vmRuntime.vmInstructionSet();
        if (vmInstructionSet != null && vmInstructionSet.length() > 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(instructionSetDescription);
            stringBuilder.append(" (");
            stringBuilder.append(vmInstructionSet);
            stringBuilder.append(")");
            instructionSetDescription = stringBuilder.toString();
        }
        String vmFlags = new StringBuilder();
        vmFlags.append("CheckJNI=");
        vmFlags.append(vmRuntime.isCheckJniEnabled() ? "true" : "false");
        vmFlags = vmFlags.toString();
        boolean isNativeDebuggable = vmRuntime.isNativeDebuggable();
        ByteBuffer out = ByteBuffer.allocate(((((28 + (vmIdent.length() * 2)) + (appName.length() * 2)) + (instructionSetDescription.length() * 2)) + (vmFlags.length() * 2)) + 1);
        out.order(ChunkHandler.CHUNK_ORDER);
        out.putInt(1);
        out.putInt(Process.myPid());
        out.putInt(vmIdent.length());
        out.putInt(appName.length());
        putString(out, vmIdent);
        putString(out, appName);
        out.putInt(UserHandle.myUserId());
        out.putInt(instructionSetDescription.length());
        putString(out, instructionSetDescription);
        out.putInt(vmFlags.length());
        putString(out, vmFlags);
        out.put((byte) isNativeDebuggable);
        Chunk reply = new Chunk(CHUNK_HELO, out);
        if (Debug.waitingForDebugger()) {
            sendWAIT(0);
        }
        return reply;
    }

    private Chunk handleFEAT(Chunk request) {
        int i;
        int i2;
        String[] vmFeatures = Debug.getVmFeatureList();
        int size = 4 + ((vmFeatures.length + FRAMEWORK_FEATURES.length) * 4);
        for (i = vmFeatures.length - 1; i >= 0; i--) {
            size += vmFeatures[i].length() * 2;
        }
        for (i = FRAMEWORK_FEATURES.length - 1; i >= 0; i--) {
            size += FRAMEWORK_FEATURES[i].length() * 2;
        }
        ByteBuffer out = ByteBuffer.allocate(size);
        out.order(ChunkHandler.CHUNK_ORDER);
        out.putInt(vmFeatures.length + FRAMEWORK_FEATURES.length);
        for (i2 = vmFeatures.length - 1; i2 >= 0; i2--) {
            out.putInt(vmFeatures[i2].length());
            putString(out, vmFeatures[i2]);
        }
        for (i2 = FRAMEWORK_FEATURES.length - 1; i2 >= 0; i2--) {
            out.putInt(FRAMEWORK_FEATURES[i2].length());
            putString(out, FRAMEWORK_FEATURES[i2]);
        }
        return new Chunk(CHUNK_FEAT, out);
    }

    public static void sendWAIT(int reason) {
        DdmServer.sendChunk(new Chunk(CHUNK_WAIT, new byte[]{(byte) reason}, 0, 1));
    }
}
