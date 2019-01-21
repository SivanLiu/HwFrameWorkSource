package android_maps_conflict_avoidance.com.google.googlenav.map;

import android_maps_conflict_avoidance.com.google.common.Config;
import android_maps_conflict_avoidance.com.google.common.Log;
import android_maps_conflict_avoidance.com.google.common.io.IoUtil;
import android_maps_conflict_avoidance.com.google.common.io.PersistentStore;
import android_maps_conflict_avoidance.com.google.common.io.PersistentStore.PersistentStoreException;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

class MapFlashService implements MapTileStorage {
    private int catalogRecordBytes = 0;
    private boolean catalogUpdatedSinceLastWrite;
    private final Vector flashRecords = new Vector();
    private int highestRecordId = 0;
    private long lastChangedTime;
    private final MapService mapService;
    private int maxFlashSize;
    private int maxRecordBlocks;
    private boolean needsScavenge;
    private long nextPersistTime;
    private final String recordStoreBaseName;
    private final PersistentStore store = Config.getInstance().getPersistentStore();
    private int textSize = -1;
    private int tileEdition = -1;
    private final Hashtable tileToRecordMap = new Hashtable();

    MapFlashService(MapService mapService, String recordStoreBaseName, int maxFlashSize, int maxRecordStores) {
        this.mapService = mapService;
        this.recordStoreBaseName = recordStoreBaseName;
        this.maxFlashSize = maxFlashSize - 2000;
        long now = Config.getInstance().getClock().relativeTimeMillis();
        this.lastChangedTime = now;
        this.nextPersistTime = 2113 + now;
        this.maxRecordBlocks = maxRecordStores - 1;
        this.catalogUpdatedSinceLastWrite = true;
        readCatalog();
        this.needsScavenge = true;
    }

    int getNumBlocks() {
        return this.flashRecords.size();
    }

    String recordBlockName(int recordId) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.recordStoreBaseName);
        stringBuilder.append('_');
        stringBuilder.append(recordId);
        return stringBuilder.toString();
    }

    String recordBlockName(FlashRecord flashRecord) {
        return recordBlockName(flashRecord.getRecordId());
    }

    public synchronized void close(boolean saveState) {
        if (saveState) {
            try {
                writeCache();
                writeCatalog();
            } catch (IOException e) {
                Log.logThrowable("FLASH", e);
            }
        }
        return;
    }

    private synchronized void readCatalog() {
        int formatVersion = 10;
        try {
            this.catalogUpdatedSinceLastWrite = true;
            byte[] directory = this.store.readBlock(this.recordStoreBaseName);
            if (directory != null) {
                DataInput is = IoUtil.createDataInputFromBytes(directory);
                formatVersion = is.readInt();
                if (formatVersion == 10) {
                    is.readBoolean();
                    this.tileEdition = is.readShort();
                    this.textSize = is.readShort();
                    int numEntries = is.readInt();
                    for (int entry = 0; entry < numEntries; entry++) {
                        addToFlashCatalog(FlashRecord.readFromCatalog(is));
                    }
                    this.catalogRecordBytes = directory.length;
                    this.catalogUpdatedSinceLastWrite = false;
                }
            }
        } catch (IOException e) {
            Log.logThrowable("FLASH", e);
        }
        if (this.catalogUpdatedSinceLastWrite) {
            eraseAll();
        }
        if (formatVersion != 10) {
            this.catalogUpdatedSinceLastWrite = true;
        }
    }

    synchronized boolean writeCatalog() throws IOException {
        boolean isOk = true;
        if (!this.catalogUpdatedSinceLastWrite) {
            return true;
        }
        int numEntries = this.flashRecords.size();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(10);
        dos.writeBoolean(false);
        dos.writeShort(this.tileEdition);
        dos.writeShort(this.textSize);
        dos.writeInt(numEntries);
        for (int entry = 0; entry < numEntries; entry++) {
            getFlashRecord(entry).writeToCatalog(dos);
        }
        baos.close();
        byte[] directory = baos.toByteArray();
        try {
            this.store.writeBlockX(directory, this.recordStoreBaseName);
        } catch (PersistentStoreException e) {
            handlePersistentStoreWriteException(e, true);
            isOk = false;
        }
        this.catalogRecordBytes = directory.length;
        this.catalogUpdatedSinceLastWrite = false;
        return isOk;
    }

    private void handlePersistentStoreWriteException(PersistentStoreException e, boolean catalog) {
        int curFlashSize = getSize();
        int curNumRecordBlocks = getNumBlocks();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("FLASH ");
        stringBuilder.append(curFlashSize);
        stringBuilder.append("B ");
        stringBuilder.append(curNumRecordBlocks);
        stringBuilder.append("R");
        stringBuilder.append(catalog ? " catalog" : "");
        Log.logQuietThrowable(stringBuilder.toString(), e);
        if (e.getType() != -2) {
            return;
        }
        if (canCreateAnEmptyRecordStore()) {
            this.maxFlashSize = curFlashSize - 1000;
        } else {
            this.maxRecordBlocks = curNumRecordBlocks;
        }
    }

    public synchronized void eraseAll() {
        this.tileToRecordMap.clear();
        this.flashRecords.removeAllElements();
        this.catalogRecordBytes = 0;
        this.highestRecordId = 0;
        this.catalogUpdatedSinceLastWrite = false;
        this.store.deleteAllBlocks(this.recordStoreBaseName);
    }

    synchronized boolean scavengeCatalog() {
        boolean wasOk;
        wasOk = true;
        String[] rsNames = this.store.listBlocks(this.recordStoreBaseName);
        for (int i = getNumBlocks() - 1; i >= 0; i--) {
            FlashRecord flashRecord = (FlashRecord) this.flashRecords.elementAt(i);
            if (!removeNameFromArray(recordBlockName(flashRecord), rsNames)) {
                wasOk = false;
                removeFromFlashCatalog(flashRecord, i);
            }
        }
        boolean catalogInFlash = false;
        if (rsNames != null) {
            catalogInFlash = removeNameFromArray(this.recordStoreBaseName, rsNames);
            for (String rsName : rsNames) {
                if (rsName != null) {
                    wasOk = false;
                    this.store.deleteBlock(rsName);
                }
            }
        }
        if (getNumBlocks() > 0 && !catalogInFlash) {
            wasOk = false;
        }
        return wasOk;
    }

    private static boolean removeNameFromArray(String name, String[] array) {
        if (array == null) {
            return false;
        }
        for (int i = 0; i < array.length; i++) {
            if (name.equals(array[i])) {
                array[i] = null;
                return true;
            }
        }
        return false;
    }

    private int findRecordIndexByID(int recordID) {
        int numEntries = this.flashRecords.size();
        for (int i = 0; i < numEntries; i++) {
            if (((FlashRecord) this.flashRecords.elementAt(i)).getRecordId() == recordID) {
                return i;
            }
        }
        return -1;
    }

    private FlashRecord getFlashRecord(int index) {
        return (FlashRecord) this.flashRecords.elementAt(index);
    }

    /*  JADX ERROR: ConcurrentModificationException in pass: EliminatePhiNodes
        java.util.ConcurrentModificationException
        	at java.util.ArrayList$Itr.checkForComodification(ArrayList.java:909)
        	at java.util.ArrayList$Itr.next(ArrayList.java:859)
        	at jadx.core.dex.visitors.ssa.EliminatePhiNodes.replaceMerge(EliminatePhiNodes.java:114)
        	at jadx.core.dex.visitors.ssa.EliminatePhiNodes.replaceMergeInstructions(EliminatePhiNodes.java:68)
        	at jadx.core.dex.visitors.ssa.EliminatePhiNodes.visit(EliminatePhiNodes.java:31)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    int doPersist(java.util.Hashtable r27) throws java.io.IOException {
        /*
        r26 = this;
        r1 = r26;
        r2 = r27;
        r0 = 2;
        r3 = new int[r0];
        r3 = {-1, -1};
        r4 = new android_maps_conflict_avoidance.com.google.googlenav.map.FlashRecord[r0];
        r5 = 0;
        r6 = 0;
        r4[r6] = r5;
        r7 = 1;
        r4[r7] = r5;
        r5 = android_maps_conflict_avoidance.com.google.common.Config.getInstance();
        r5 = r5.getClock();
        r8 = r5.currentTimeMillis();
        monitor-enter(r26);
        r5 = r1.needsScavenge;	 Catch:{ all -> 0x01e7 }
        if (r5 == 0) goto L_0x0029;	 Catch:{ all -> 0x01e7 }
    L_0x0024:
        r26.scavengeCatalog();	 Catch:{ all -> 0x01e7 }
        r1.needsScavenge = r6;	 Catch:{ all -> 0x01e7 }
    L_0x0029:
        r5 = r1.maxFlashSize;	 Catch:{ all -> 0x01e7 }
        r10 = r26.getSize();	 Catch:{ all -> 0x01e7 }
        r5 = r5 - r10;
        r10 = r5;
        r11 = 72000; // 0x11940 float:1.00893E-40 double:3.55727E-319;
        if (r5 < r11) goto L_0x0048;
    L_0x0036:
        r12 = r26.getNumBlocks();	 Catch:{ all -> 0x0044 }
        r13 = r1.maxRecordBlocks;	 Catch:{ all -> 0x0044 }
        if (r12 < r13) goto L_0x003f;	 Catch:{ all -> 0x0044 }
    L_0x003e:
        goto L_0x0048;	 Catch:{ all -> 0x0044 }
    L_0x003f:
        r10 = 72000; // 0x11940 float:1.00893E-40 double:3.55727E-319;	 Catch:{ all -> 0x0044 }
        goto L_0x00b6;	 Catch:{ all -> 0x0044 }
    L_0x0044:
        r0 = move-exception;	 Catch:{ all -> 0x0044 }
        r6 = r5;	 Catch:{ all -> 0x0044 }
        goto L_0x01e9;	 Catch:{ all -> 0x0044 }
    L_0x0048:
        r12 = -1;	 Catch:{ all -> 0x0044 }
        r13 = -1;	 Catch:{ all -> 0x0044 }
        r14 = -9223372036854775808;	 Catch:{ all -> 0x0044 }
        r16 = -9223372036854775808;	 Catch:{ all -> 0x0044 }
        r18 = r26.getNumBlocks();	 Catch:{ all -> 0x0044 }
        r19 = r18;	 Catch:{ all -> 0x0044 }
        r20 = r14;	 Catch:{ all -> 0x0044 }
        r14 = r13;	 Catch:{ all -> 0x0044 }
        r13 = r12;	 Catch:{ all -> 0x0044 }
        r12 = r6;	 Catch:{ all -> 0x0044 }
    L_0x0059:
        r15 = -1;	 Catch:{ all -> 0x0044 }
        r0 = r19;	 Catch:{ all -> 0x0044 }
        if (r12 >= r0) goto L_0x008a;	 Catch:{ all -> 0x0044 }
    L_0x005e:
        r18 = r1.getFlashRecord(r12);	 Catch:{ all -> 0x0044 }
        r22 = r18;	 Catch:{ all -> 0x0044 }
        r7 = r22;	 Catch:{ all -> 0x0044 }
        r18 = r7.getScore(r8);	 Catch:{ all -> 0x0044 }
        if (r14 == r15) goto L_0x0070;	 Catch:{ all -> 0x0044 }
    L_0x006c:
        r22 = (r18 > r16 ? 1 : (r18 == r16 ? 0 : -1));	 Catch:{ all -> 0x0044 }
        if (r22 <= 0) goto L_0x0083;	 Catch:{ all -> 0x0044 }
    L_0x0070:
        if (r13 == r15) goto L_0x007b;	 Catch:{ all -> 0x0044 }
    L_0x0072:
        r15 = (r18 > r20 ? 1 : (r18 == r20 ? 0 : -1));	 Catch:{ all -> 0x0044 }
        if (r15 <= 0) goto L_0x0077;	 Catch:{ all -> 0x0044 }
    L_0x0076:
        goto L_0x007b;	 Catch:{ all -> 0x0044 }
    L_0x0077:
        r14 = r12;	 Catch:{ all -> 0x0044 }
        r15 = r18;	 Catch:{ all -> 0x0044 }
        goto L_0x0081;	 Catch:{ all -> 0x0044 }
    L_0x007b:
        r14 = r13;	 Catch:{ all -> 0x0044 }
        r15 = r20;	 Catch:{ all -> 0x0044 }
        r13 = r12;	 Catch:{ all -> 0x0044 }
        r20 = r18;	 Catch:{ all -> 0x0044 }
    L_0x0081:
        r16 = r15;	 Catch:{ all -> 0x0044 }
    L_0x0083:
        r12 = r12 + 1;	 Catch:{ all -> 0x0044 }
        r19 = r0;	 Catch:{ all -> 0x0044 }
        r0 = 2;	 Catch:{ all -> 0x0044 }
        r7 = 1;	 Catch:{ all -> 0x0044 }
        goto L_0x0059;	 Catch:{ all -> 0x0044 }
    L_0x008a:
        if (r13 == r15) goto L_0x009b;	 Catch:{ all -> 0x0044 }
    L_0x008c:
        r3[r6] = r13;	 Catch:{ all -> 0x0044 }
        r7 = r1.getFlashRecord(r13);	 Catch:{ all -> 0x0044 }
        r4[r6] = r7;	 Catch:{ all -> 0x0044 }
        r7 = r4[r6];	 Catch:{ all -> 0x0044 }
        r7 = r7.getDataSize();	 Catch:{ all -> 0x0044 }
        r10 = r10 + r7;	 Catch:{ all -> 0x0044 }
    L_0x009b:
        if (r10 >= r11) goto L_0x00af;	 Catch:{ all -> 0x0044 }
    L_0x009d:
        if (r14 == r15) goto L_0x00af;	 Catch:{ all -> 0x0044 }
    L_0x009f:
        r7 = 1;	 Catch:{ all -> 0x0044 }
        r3[r7] = r14;	 Catch:{ all -> 0x0044 }
        r12 = r1.getFlashRecord(r14);	 Catch:{ all -> 0x0044 }
        r4[r7] = r12;	 Catch:{ all -> 0x0044 }
        r12 = r4[r7];	 Catch:{ all -> 0x0044 }
        r7 = r12.getDataSize();	 Catch:{ all -> 0x0044 }
        r10 = r10 + r7;	 Catch:{ all -> 0x0044 }
    L_0x00af:
        r7 = java.lang.Math.min(r10, r11);	 Catch:{ all -> 0x0044 }
        r0 = r7;	 Catch:{ all -> 0x0044 }
        r10 = r0;	 Catch:{ all -> 0x0044 }
    L_0x00b6:
        monitor-exit(r26);	 Catch:{ all -> 0x0044 }
        r0 = 6000; // 0x1770 float:8.408E-42 double:2.9644E-320;
        if (r10 >= r0) goto L_0x00d3;
    L_0x00bb:
        r0 = r4[r6];
        if (r0 == 0) goto L_0x00d1;
    L_0x00bf:
        r0 = r1.store;
        r7 = r4[r6];
        r7 = r1.recordBlockName(r7);
        r0.deleteBlock(r7);
        r0 = r4[r6];
        r6 = r3[r6];
        r1.removeFromFlashCatalog(r0, r6);
    L_0x00d1:
        r7 = 1;
        return r7;
    L_0x00d3:
        r7 = 1;
        monitor-enter(r27);
        r11 = r1.mapService;	 Catch:{ all -> 0x01d4 }
        r11.setMapCacheLocked(r7);	 Catch:{ all -> 0x01d4 }
        r7 = r1.fillNewRecord(r2, r10);	 Catch:{ all -> 0x01d4 }
        r11 = r1.mapService;	 Catch:{ all -> 0x01cf }
        r11.setMapCacheLocked(r6);	 Catch:{ all -> 0x01cf }
        monitor-exit(r27);	 Catch:{ all -> 0x01cf }
        r11 = r7.getDataSize();
        if (r11 < r0) goto L_0x01c8;
    L_0x00ec:
        r12 = -1;
        monitor-enter(r26);
        r0 = r1.maxFlashSize;	 Catch:{ all -> 0x01c1 }
        r13 = r26.getSize();	 Catch:{ all -> 0x01c1 }
        r5 = r0 - r13;
        r0 = 2;
        r13 = new boolean[r0];	 Catch:{ all -> 0x01bb }
        r13 = {0, 0};
        r0 = r13;
        r13 = 0;
        r14 = r13;
        r13 = r6;
    L_0x0100:
        r15 = 2;
        if (r13 >= r15) goto L_0x0134;
    L_0x0103:
        r16 = r4[r13];	 Catch:{ all -> 0x012f }
        if (r16 == 0) goto L_0x0127;	 Catch:{ all -> 0x012f }
    L_0x0107:
        r15 = r4[r13];	 Catch:{ all -> 0x012f }
        r15 = r15.isSaved();	 Catch:{ all -> 0x012f }
        if (r15 == 0) goto L_0x0127;	 Catch:{ all -> 0x012f }
    L_0x010f:
        r15 = r4[r13];	 Catch:{ all -> 0x012f }
        r15 = r15.getScore(r8);	 Catch:{ all -> 0x012f }
        r17 = r7.getScore(r8);	 Catch:{ all -> 0x012f }
        r15 = (r15 > r17 ? 1 : (r15 == r17 ? 0 : -1));	 Catch:{ all -> 0x012f }
        if (r15 <= 0) goto L_0x0127;	 Catch:{ all -> 0x012f }
    L_0x011d:
        r15 = 1;	 Catch:{ all -> 0x012f }
        r0[r13] = r15;	 Catch:{ all -> 0x012f }
        r15 = r4[r13];	 Catch:{ all -> 0x012f }
        r15 = r15.getDataSize();	 Catch:{ all -> 0x012f }
        r14 = r14 + r15;
    L_0x0127:
        r15 = r5 + r14;
        if (r11 > r15) goto L_0x012c;
    L_0x012b:
        goto L_0x0134;
    L_0x012c:
        r13 = r13 + 1;
        goto L_0x0100;
    L_0x012f:
        r0 = move-exception;
        r24 = r10;
        goto L_0x01c4;
    L_0x0134:
        r13 = r26.getNumBlocks();	 Catch:{ all -> 0x01bb }
        r15 = r5 + r14;	 Catch:{ all -> 0x01bb }
        if (r11 <= r15) goto L_0x0143;	 Catch:{ all -> 0x01bb }
    L_0x013c:
        r6 = 2;	 Catch:{ all -> 0x01bb }
        r23 = r5;	 Catch:{ all -> 0x01bb }
        r24 = r10;	 Catch:{ all -> 0x01bb }
        goto L_0x01a7;	 Catch:{ all -> 0x01bb }
    L_0x0143:
        r15 = r0[r6];	 Catch:{ all -> 0x01bb }
        if (r15 == 0) goto L_0x0195;
    L_0x0147:
        if (r11 > r5) goto L_0x0153;
    L_0x0149:
        r15 = r1.maxRecordBlocks;	 Catch:{ all -> 0x012f }
        if (r13 < r15) goto L_0x014e;
    L_0x014d:
        goto L_0x0153;
    L_0x014e:
        r23 = r5;
        r24 = r10;
        goto L_0x0199;
    L_0x0153:
        r15 = 4;
        r23 = r5;
        r5 = r4[r6];	 Catch:{ all -> 0x018e }
        r5 = r5.getRecordId();	 Catch:{ all -> 0x018e }
        r12 = r5;	 Catch:{ all -> 0x018e }
        r5 = r4[r6];	 Catch:{ all -> 0x018e }
        r24 = r10;
        r10 = r3[r6];	 Catch:{ all -> 0x018b }
        r1.removeFromFlashCatalog(r5, r10);	 Catch:{ all -> 0x018b }
        r5 = 1;	 Catch:{ all -> 0x018b }
        r10 = r0[r5];	 Catch:{ all -> 0x018b }
        if (r10 == 0) goto L_0x0189;	 Catch:{ all -> 0x018b }
    L_0x016b:
        r6 = r3[r6];	 Catch:{ all -> 0x018b }
        r10 = r3[r5];	 Catch:{ all -> 0x018b }
        if (r6 >= r10) goto L_0x0176;	 Catch:{ all -> 0x018b }
    L_0x0171:
        r6 = r3[r5];	 Catch:{ all -> 0x018b }
        r6 = r6 - r5;	 Catch:{ all -> 0x018b }
        r3[r5] = r6;	 Catch:{ all -> 0x018b }
    L_0x0176:
        r5 = r1.store;	 Catch:{ all -> 0x018b }
        r6 = 1;	 Catch:{ all -> 0x018b }
        r10 = r4[r6];	 Catch:{ all -> 0x018b }
        r10 = r1.recordBlockName(r10);	 Catch:{ all -> 0x018b }
        r5.deleteBlock(r10);	 Catch:{ all -> 0x018b }
        r5 = r4[r6];	 Catch:{ all -> 0x018b }
        r6 = r3[r6];	 Catch:{ all -> 0x018b }
        r1.removeFromFlashCatalog(r5, r6);	 Catch:{ all -> 0x018b }
    L_0x0189:
        r6 = r15;
        goto L_0x01a7;
    L_0x018b:
        r0 = move-exception;
        r6 = r15;
        goto L_0x01b8;
    L_0x018e:
        r0 = move-exception;
        r24 = r10;
        r6 = r15;
        r5 = r23;
        goto L_0x01c4;
    L_0x0195:
        r23 = r5;
        r24 = r10;
    L_0x0199:
        r5 = r1.maxRecordBlocks;	 Catch:{ all -> 0x01b7 }
        if (r13 >= r5) goto L_0x01a6;	 Catch:{ all -> 0x01b7 }
    L_0x019d:
        r6 = 3;	 Catch:{ all -> 0x01b7 }
        r5 = r1.highestRecordId;	 Catch:{ all -> 0x01b7 }
        r10 = 1;	 Catch:{ all -> 0x01b7 }
        r5 = r5 + r10;	 Catch:{ all -> 0x01b7 }
        r1.highestRecordId = r5;	 Catch:{ all -> 0x01b7 }
        r12 = r5;	 Catch:{ all -> 0x01b7 }
        goto L_0x01a7;	 Catch:{ all -> 0x01b7 }
    L_0x01a6:
        r6 = 5;	 Catch:{ all -> 0x01b7 }
    L_0x01a7:
        monitor-exit(r26);	 Catch:{ all -> 0x01b7 }
        if (r12 < 0) goto L_0x01b3;
    L_0x01aa:
        r0 = r7.createDataEntry(r2);
        if (r0 == 0) goto L_0x01b3;
    L_0x01b0:
        r1.persistRecord(r7, r0, r12);
        r5 = r23;
        goto L_0x01ca;
    L_0x01b7:
        r0 = move-exception;
    L_0x01b8:
        r5 = r23;
        goto L_0x01c4;
    L_0x01bb:
        r0 = move-exception;
        r23 = r5;
        r24 = r10;
        goto L_0x01c4;
    L_0x01c1:
        r0 = move-exception;
        r24 = r10;
    L_0x01c4:
        monitor-exit(r26);	 Catch:{ all -> 0x01c6 }
        throw r0;
    L_0x01c6:
        r0 = move-exception;
        goto L_0x01c4;
    L_0x01c8:
        r24 = r10;
    L_0x01ca:
        r0 = r6;
        r26.writeCatalog();
        return r0;
    L_0x01cf:
        r0 = move-exception;
        r24 = r10;
        r11 = r2;
        goto L_0x01e3;
    L_0x01d4:
        r0 = move-exception;
        r7 = r1;
        r11 = r2;
        r12 = r7.mapService;	 Catch:{ all -> 0x01dd }
        r12.setMapCacheLocked(r6);	 Catch:{ all -> 0x01dd }
        throw r0;	 Catch:{ all -> 0x01dd }
    L_0x01dd:
        r0 = move-exception;
        r25 = r7;
        r7 = r1;
        r1 = r25;
    L_0x01e3:
        monitor-exit(r27);	 Catch:{ all -> 0x01e5 }
        throw r0;
    L_0x01e5:
        r0 = move-exception;
        goto L_0x01e3;
    L_0x01e7:
        r0 = move-exception;
        r10 = r6;
    L_0x01e9:
        monitor-exit(r26);	 Catch:{ all -> 0x01eb }
        throw r0;
    L_0x01eb:
        r0 = move-exception;
        goto L_0x01e9;
        */
        throw new UnsupportedOperationException("Method not decompiled: android_maps_conflict_avoidance.com.google.googlenav.map.MapFlashService.doPersist(java.util.Hashtable):int");
    }

    private synchronized void persistRecord(FlashRecord newRecord, byte[] newRecordData, int recordId) {
        if (this.catalogRecordBytes == 0) {
            this.store.writeBlock(new byte[0], this.recordStoreBaseName);
        }
        try {
            newRecord.writeRecord(recordBlockName(recordId), recordId, newRecordData);
            addToFlashCatalog(newRecord);
        } catch (PersistentStoreException e) {
            handlePersistentStoreWriteException(e, false);
        } catch (IllegalStateException e2) {
            Log.logThrowable("FLASH", e2);
        }
    }

    private boolean canCreateAnEmptyRecordStore() {
        String rsName = new StringBuilder();
        rsName.append(this.recordStoreBaseName);
        rsName.append("_Test");
        rsName = rsName.toString();
        try {
            this.store.writeBlockX(new byte[0], rsName);
            this.store.deleteBlock(rsName);
            return true;
        } catch (PersistentStoreException e) {
            return false;
        }
    }

    private FlashRecord fillNewRecord(Hashtable mapCache, int maxDataSize) {
        FlashRecord newRecord = new FlashRecord();
        int newDataSize = 1;
        Tile[] sortedMemoryTiles = this.mapService.getSortedCacheList();
        for (int tileIndex = sortedMemoryTiles.length - 1; tileIndex >= 0; tileIndex--) {
            Tile tile = sortedMemoryTiles[tileIndex];
            if (this.tileToRecordMap.get(tile) == null) {
                MapTile mapTile = (MapTile) mapCache.get(tile);
                if (mapTile.isComplete()) {
                    FlashEntry newEntry = new FlashEntry(mapTile);
                    int entrySize = newEntry.getByteSize();
                    if (newDataSize + entrySize <= maxDataSize && newRecord.addEntry(newEntry)) {
                        newDataSize += entrySize;
                    }
                }
            }
        }
        return newRecord;
    }

    private void addToFlashCatalog(FlashRecord newRecord) {
        int numEntries = newRecord.numEntries();
        this.catalogUpdatedSinceLastWrite = true;
        this.highestRecordId = Math.max(this.highestRecordId, newRecord.getRecordId());
        this.flashRecords.addElement(newRecord);
        for (int i = 0; i < numEntries; i++) {
            this.tileToRecordMap.put(newRecord.getEntry(i).getTile(), newRecord);
        }
    }

    private void removeFromFlashCatalog(FlashRecord flashRecord, int elementIndex) {
        if (flashRecord.isSaved()) {
            int numEntries = flashRecord.numEntries();
            this.catalogUpdatedSinceLastWrite = true;
            flashRecord.setUnsaved();
            this.flashRecords.removeElementAt(elementIndex);
            for (int i = 0; i < numEntries; i++) {
                this.tileToRecordMap.remove(flashRecord.getEntry(i).getTile());
            }
        }
    }

    private FlashEntry getFlashEntry(Tile location) {
        FlashRecord record = (FlashRecord) this.tileToRecordMap.get(location);
        return record == null ? null : record.getEntry(location);
    }

    synchronized int getFlashRecordsSize() {
        int size;
        size = 0;
        for (int index = 0; index < this.flashRecords.size(); index++) {
            size += getFlashRecord(index).getDataSize();
        }
        return size;
    }

    public synchronized int getSize() {
        return this.catalogRecordBytes + getFlashRecordsSize();
    }

    public MapTile getMapTile(Tile tile) {
        MapTile mapTile = null;
        FlashEntry flashEntry = getFlashEntry(tile);
        if (flashEntry != null) {
            mapTile = loadFlashRecordTile(flashEntry.getFlashRecord(), tile);
            if (mapTile != null) {
                flashEntry.setLastAccessTime(Config.getInstance().getClock().currentTimeMillis());
            }
        }
        return mapTile;
    }

    public void mapChanged() {
        this.lastChangedTime = Config.getInstance().getClock().relativeTimeMillis();
    }

    public boolean writeCache() throws IOException {
        long startTime = Config.getInstance().getClock().relativeTimeMillis();
        Hashtable mapCache = this.mapService.getMapCache();
        if (this.nextPersistTime >= startTime || this.lastChangedTime + 1500 >= startTime) {
            return true;
        }
        try {
            int status = doPersist(mapCache);
            boolean z = status == 3 || status == 4;
            boolean cachingStillActive = z;
            this.nextPersistTime = Config.getInstance().getClock().relativeTimeMillis() + 2113;
            return cachingStillActive;
        } catch (Throwable th) {
            this.nextPersistTime = Config.getInstance().getClock().relativeTimeMillis() + 2113;
        }
    }

    private MapTile loadFlashRecordTile(FlashRecord flashRecord, Tile desiredTile) {
        MapTile mapTile = flashRecord.loadTile(recordBlockName(flashRecord), desiredTile);
        if (mapTile == null) {
            synchronized (this) {
                int recordId = flashRecord.getRecordId();
                removeFromFlashCatalog(flashRecord, findRecordIndexByID(recordId));
                this.store.deleteBlock(recordBlockName(recordId));
            }
        }
        return mapTile;
    }

    public boolean setTileEditionAndTextSize(int newTileEdition, int newTextSize) {
        boolean changed = ((newTileEdition == this.tileEdition || this.tileEdition == -1) && (newTextSize == this.textSize || this.textSize == -1)) ? false : true;
        this.tileEdition = newTileEdition;
        this.textSize = newTextSize;
        if (changed) {
            eraseAll();
            this.catalogUpdatedSinceLastWrite = true;
        }
        return changed;
    }
}
