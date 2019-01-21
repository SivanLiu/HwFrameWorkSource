package android.ddm;

import android.util.Log;
import org.apache.harmony.dalvik.ddmc.Chunk;
import org.apache.harmony.dalvik.ddmc.ChunkHandler;
import org.apache.harmony.dalvik.ddmc.DdmServer;

public class DdmHandleNativeHeap extends ChunkHandler {
    public static final int CHUNK_NHGT = type("NHGT");
    private static DdmHandleNativeHeap mInstance = new DdmHandleNativeHeap();

    private native byte[] getLeakInfo();

    private DdmHandleNativeHeap() {
    }

    public static void register() {
        DdmServer.registerHandler(CHUNK_NHGT, mInstance);
    }

    public void connected() {
    }

    public void disconnected() {
    }

    public Chunk handleChunk(Chunk request) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Handling ");
        stringBuilder.append(name(request.type));
        stringBuilder.append(" chunk");
        Log.i("ddm-nativeheap", stringBuilder.toString());
        int type = request.type;
        if (type == CHUNK_NHGT) {
            return handleNHGT(request);
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Unknown packet ");
        stringBuilder2.append(ChunkHandler.name(type));
        throw new RuntimeException(stringBuilder2.toString());
    }

    private Chunk handleNHGT(Chunk request) {
        byte[] data = getLeakInfo();
        if (data == null) {
            return createFailChunk(1, "Something went wrong");
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Sending ");
        stringBuilder.append(data.length);
        stringBuilder.append(" bytes");
        Log.i("ddm-nativeheap", stringBuilder.toString());
        return new Chunk(ChunkHandler.type("NHGT"), data, 0, data.length);
    }
}
