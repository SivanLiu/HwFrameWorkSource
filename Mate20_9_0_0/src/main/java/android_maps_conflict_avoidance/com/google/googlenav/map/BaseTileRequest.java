package android_maps_conflict_avoidance.com.google.googlenav.map;

import android_maps_conflict_avoidance.com.google.common.Config;
import android_maps_conflict_avoidance.com.google.common.util.StopwatchStats;
import android_maps_conflict_avoidance.com.google.common.util.text.TextUtil;
import android_maps_conflict_avoidance.com.google.googlenav.GmmLogger;
import android_maps_conflict_avoidance.com.google.googlenav.datarequest.BaseDataRequest;
import android_maps_conflict_avoidance.com.google.googlenav.labs.LocalLanguageTileLab;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Vector;

public abstract class BaseTileRequest extends BaseDataRequest {
    protected final long createTime = Config.getInstance().getClock().relativeTimeMillis();
    private final int requestType;
    private StopwatchStats stopwatchStatsTile;
    private int textSize;
    private int writeLatency;

    protected abstract void handleEndOfResponse(int i);

    protected abstract boolean processDownloadedTile(int i, Tile tile, byte[] bArr) throws IOException;

    protected abstract void setTileEditionAndTextSize(int i, int i2);

    protected BaseTileRequest(int requestType, byte flags) {
        this.requestType = requestType;
        String stopwatchName = new StringBuilder();
        stopwatchName.append("tile-");
        stopwatchName.append(formatTileTypesForLog(1 << flags));
        this.stopwatchStatsTile = new StopwatchStats(stopwatchName.toString(), "t", (short) 22);
        this.stopwatchStatsTile.start();
    }

    public int getRequestType() {
        return this.requestType;
    }

    protected void writeRequestForTiles(Tile[] tileList, DataOutput dos) throws IOException {
        if (this.requestType == 26) {
            dos.writeShort(tileList.length);
            this.textSize = MapTile.getTextSize();
            dos.writeShort(this.textSize);
            dos.writeShort(256);
            long format = 2607;
            if (LocalLanguageTileLab.INSTANCE.isActive()) {
                format = 2607 | 8192;
            }
            dos.writeLong(format);
        }
        for (Tile tile : tileList) {
            tile.write(dos);
        }
        this.writeLatency = (int) (Config.getInstance().getClock().relativeTimeMillis() - this.createTime);
    }

    public boolean readResponseData(DataInput dis) throws IOException {
        DataInput tileIndex = 0;
        try {
            int firstByteLatency = (int) (Config.getInstance().getClock().relativeTimeMillis() - this.createTime);
            setTileEditionAndTextSize(dis.readUnsignedShort(), this.textSize);
            int tileCount = 0;
            if (this.requestType == 26) {
                int responseCode = dis.readUnsignedByte();
                if (responseCode == 0) {
                    tileCount = dis.readUnsignedShort();
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Server returned: ");
                    stringBuilder.append(responseCode);
                    throw new IOException(stringBuilder.toString());
                }
            }
            tileIndex = 0;
            int totalSize = 0;
            int tileTypes = 0;
            while (tileIndex < tileCount) {
                Tile location = Tile.read(dis);
                byte[] imageBytes = readImageData(dis);
                if (processDownloadedTile(tileIndex, location, imageBytes)) {
                    break;
                }
                totalSize += imageBytes.length;
                tileTypes |= 1 << location.getFlags();
                tileIndex++;
            }
            int lastByteLatency = (int) (Config.getInstance().getClock().relativeTimeMillis() - this.createTime);
            this.stopwatchStatsTile.stop();
            GmmLogger.logTimingTileLatency(formatTileTypesForLog(tileTypes), this.writeLatency, firstByteLatency, lastByteLatency, tileCount, totalSize);
        } finally {
            DataInput dis2 = 
/*
Method generation error in method: android_maps_conflict_avoidance.com.google.googlenav.map.BaseTileRequest.readResponseData(java.io.DataInput):boolean, dex: 
jadx.core.utils.exceptions.CodegenException: Error generate insn: 0x009b: MERGE  (r3_1 'dis2' java.io.DataInput) = (r20_0 'dis' java.io.DataInput), (r2_4 'tileIndex' java.io.DataInput) in method: android_maps_conflict_avoidance.com.google.googlenav.map.BaseTileRequest.readResponseData(java.io.DataInput):boolean, dex: 
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:228)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:205)
	at jadx.core.codegen.RegionGen.makeSimpleBlock(RegionGen.java:100)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:50)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:93)
	at jadx.core.codegen.RegionGen.makeTryCatch(RegionGen.java:298)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:173)
	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:321)
	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:259)
	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:221)
	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:111)
	at jadx.core.codegen.ClassGen.makeClass(ClassGen.java:77)
	at jadx.core.codegen.CodeGen.visit(CodeGen.java:10)
	at jadx.core.ProcessClass.process(ProcessClass.java:38)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
Caused by: jadx.core.utils.exceptions.CodegenException: MERGE can be used only in fallback mode
	at jadx.core.codegen.InsnGen.fallbackOnlyInsn(InsnGen.java:539)
	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:511)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:222)
	... 21 more

*/

    private byte[] readImageData(DataInput dis) throws IOException {
        byte[] imageBytes = new byte[dis.readUnsignedShort()];
        dis.readFully(imageBytes);
        return imageBytes;
    }

    private static String formatTileTypesForLog(int tileTypes) {
        Vector result = new Vector();
        if ((tileTypes & 4) != 0) {
            result.addElement("m");
        }
        if ((tileTypes & 8) != 0) {
            result.addElement("s");
        }
        if ((tileTypes & 64) != 0) {
            result.addElement("h");
        }
        if ((tileTypes & 128) != 0) {
            result.addElement("n");
        }
        if ((tileTypes & 16) != 0) {
            result.addElement("t");
        }
        return TextUtil.join(result, ",");
    }
}
