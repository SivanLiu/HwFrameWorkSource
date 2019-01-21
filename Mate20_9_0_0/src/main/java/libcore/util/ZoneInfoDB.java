package libcore.util;

import android.system.ErrnoException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import libcore.io.BufferIterator;
import libcore.io.MemoryMappedFile;

public final class ZoneInfoDB {
    private static final TzData DATA = TzData.loadTzDataWithFallback(TimeZoneDataFiles.getTimeZoneFilePaths(TZDATA_FILE));
    public static final String TZDATA_FILE = "tzdata";

    public static class TzData implements AutoCloseable {
        private static final int CACHE_SIZE = 1;
        public static final int SIZEOF_INDEX_ENTRY = 52;
        private static final int SIZEOF_TZINT = 4;
        private static final int SIZEOF_TZNAME = 40;
        private int[] byteOffsets;
        private final BasicLruCache<String, ZoneInfo> cache = new BasicLruCache<String, ZoneInfo>(1) {
            protected ZoneInfo create(String id) {
                try {
                    return TzData.this.makeTimeZoneUncached(id);
                } catch (IOException e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to load timezone for ID=");
                    stringBuilder.append(id);
                    throw new IllegalStateException(stringBuilder.toString(), e);
                }
            }
        };
        private boolean closed;
        private String[] ids;
        private MemoryMappedFile mappedFile;
        private int[] rawUtcOffsetsCache;
        private String version;
        private String zoneTab;

        public static TzData loadTzDataWithFallback(String... paths) {
            for (String path : paths) {
                TzData tzData = new TzData();
                if (tzData.loadData(path)) {
                    return tzData;
                }
            }
            System.logE("Couldn't find any tzdata file!");
            return createFallback();
        }

        public static TzData loadTzData(String path) {
            TzData tzData = new TzData();
            if (tzData.loadData(path)) {
                return tzData;
            }
            return null;
        }

        private static TzData createFallback() {
            TzData tzData = new TzData();
            tzData.populateFallback();
            return tzData;
        }

        private TzData() {
        }

        public BufferIterator getBufferIterator(String id) {
            checkNotClosed();
            int index = Arrays.binarySearch(this.ids, id);
            if (index < 0) {
                return null;
            }
            int byteOffset = this.byteOffsets[index];
            BufferIterator it = this.mappedFile.bigEndianIterator();
            it.skip(byteOffset);
            return it;
        }

        private void populateFallback() {
            this.version = "missing";
            this.zoneTab = "# Emergency fallback data.\n";
            this.ids = new String[]{"GMT"};
            int[] iArr = new int[1];
            this.rawUtcOffsetsCache = iArr;
            this.byteOffsets = iArr;
        }

        private boolean loadData(String path) {
            try {
                this.mappedFile = MemoryMappedFile.mmapRO(path);
                try {
                    readHeader();
                    return true;
                } catch (Exception ex) {
                    close();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("tzdata file \"");
                    stringBuilder.append(path);
                    stringBuilder.append("\" was present but invalid!");
                    System.logE(stringBuilder.toString(), ex);
                    return false;
                }
            } catch (ErrnoException e) {
                return false;
            }
        }

        private void readHeader() throws IOException {
            BufferIterator it = this.mappedFile.bigEndianIterator();
            try {
                byte[] tzdata_version = new byte[12];
                it.readByteArray(tzdata_version, 0, tzdata_version.length);
                if (new String(tzdata_version, 0, 6, StandardCharsets.US_ASCII).equals(ZoneInfoDB.TZDATA_FILE) && tzdata_version[11] == (byte) 0) {
                    this.version = new String(tzdata_version, 6, 5, StandardCharsets.US_ASCII);
                    int fileSize = this.mappedFile.size();
                    int index_offset = it.readInt();
                    validateOffset(index_offset, fileSize);
                    int data_offset = it.readInt();
                    validateOffset(data_offset, fileSize);
                    int zonetab_offset = it.readInt();
                    validateOffset(zonetab_offset, fileSize);
                    if (index_offset >= data_offset || data_offset >= zonetab_offset) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Invalid offset: index_offset=");
                        stringBuilder.append(index_offset);
                        stringBuilder.append(", data_offset=");
                        stringBuilder.append(data_offset);
                        stringBuilder.append(", zonetab_offset=");
                        stringBuilder.append(zonetab_offset);
                        stringBuilder.append(", fileSize=");
                        stringBuilder.append(fileSize);
                        throw new IOException(stringBuilder.toString());
                    }
                    readIndex(it, index_offset, data_offset);
                    readZoneTab(it, zonetab_offset, fileSize - zonetab_offset);
                    return;
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("bad tzdata magic: ");
                stringBuilder2.append(Arrays.toString(tzdata_version));
                throw new IOException(stringBuilder2.toString());
            } catch (IndexOutOfBoundsException e) {
                throw new IOException("Invalid read from data file", e);
            }
        }

        private static void validateOffset(int offset, int size) throws IOException {
            if (offset < 0 || offset >= size) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid offset=");
                stringBuilder.append(offset);
                stringBuilder.append(", size=");
                stringBuilder.append(size);
                throw new IOException(stringBuilder.toString());
            }
        }

        private void readZoneTab(BufferIterator it, int zoneTabOffset, int zoneTabSize) {
            byte[] bytes = new byte[zoneTabSize];
            it.seek(zoneTabOffset);
            it.readByteArray(bytes, 0, bytes.length);
            this.zoneTab = new String(bytes, 0, bytes.length, StandardCharsets.US_ASCII);
        }

        private void readIndex(BufferIterator it, int indexOffset, int dataOffset) throws IOException {
            it.seek(indexOffset);
            byte[] idBytes = new byte[40];
            int indexSize = dataOffset - indexOffset;
            if (indexSize % 52 == 0) {
                int entryCount = indexSize / 52;
                this.byteOffsets = new int[entryCount];
                this.ids = new String[entryCount];
                int i = 0;
                while (i < entryCount) {
                    it.readByteArray(idBytes, 0, idBytes.length);
                    this.byteOffsets[i] = it.readInt();
                    int[] iArr = this.byteOffsets;
                    iArr[i] = iArr[i] + dataOffset;
                    if (it.readInt() >= 44) {
                        it.skip(4);
                        int len = 0;
                        while (idBytes[len] != (byte) 0 && len < idBytes.length) {
                            len++;
                        }
                        StringBuilder stringBuilder;
                        if (len != 0) {
                            this.ids[i] = new String(idBytes, 0, len, StandardCharsets.US_ASCII);
                            if (i <= 0 || this.ids[i].compareTo(this.ids[i - 1]) > 0) {
                                i++;
                            } else {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Index not sorted or contains multiple entries with the same ID, index=");
                                stringBuilder.append(i);
                                stringBuilder.append(", ids[i]=");
                                stringBuilder.append(this.ids[i]);
                                stringBuilder.append(", ids[i - 1]=");
                                stringBuilder.append(this.ids[i - 1]);
                                throw new IOException(stringBuilder.toString());
                            }
                        }
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Invalid ID at index=");
                        stringBuilder.append(i);
                        throw new IOException(stringBuilder.toString());
                    }
                    throw new IOException("length in index file < sizeof(tzhead)");
                }
                return;
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Index size is not divisible by 52, indexSize=");
            stringBuilder2.append(indexSize);
            throw new IOException(stringBuilder2.toString());
        }

        public void validate() throws IOException {
            checkNotClosed();
            String[] availableIDs = getAvailableIDs();
            int length = availableIDs.length;
            int i = 0;
            while (i < length) {
                String id = availableIDs[i];
                if (makeTimeZoneUncached(id) != null) {
                    i++;
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unable to find data for ID=");
                    stringBuilder.append(id);
                    throw new IOException(stringBuilder.toString());
                }
            }
        }

        ZoneInfo makeTimeZoneUncached(String id) throws IOException {
            BufferIterator it = getBufferIterator(id);
            if (it == null) {
                return null;
            }
            return ZoneInfo.readTimeZone(id, it, System.currentTimeMillis());
        }

        public String[] getAvailableIDs() {
            checkNotClosed();
            return (String[]) this.ids.clone();
        }

        public String[] getAvailableIDs(int rawUtcOffset) {
            checkNotClosed();
            List<String> matches = new ArrayList();
            int[] rawUtcOffsets = getRawUtcOffsets();
            for (int i = 0; i < rawUtcOffsets.length; i++) {
                if (rawUtcOffsets[i] == rawUtcOffset) {
                    matches.add(this.ids[i]);
                }
            }
            return (String[]) matches.toArray(new String[matches.size()]);
        }

        private synchronized int[] getRawUtcOffsets() {
            if (this.rawUtcOffsetsCache != null) {
                return this.rawUtcOffsetsCache;
            }
            this.rawUtcOffsetsCache = new int[this.ids.length];
            for (int i = 0; i < this.ids.length; i++) {
                this.rawUtcOffsetsCache[i] = ((ZoneInfo) this.cache.get(this.ids[i])).getRawOffset();
            }
            return this.rawUtcOffsetsCache;
        }

        public String getVersion() {
            checkNotClosed();
            return this.version;
        }

        public String getZoneTab() {
            checkNotClosed();
            return this.zoneTab;
        }

        public ZoneInfo makeTimeZone(String id) throws IOException {
            checkNotClosed();
            ZoneInfo zoneInfo = (ZoneInfo) this.cache.get(id);
            return zoneInfo == null ? null : (ZoneInfo) zoneInfo.clone();
        }

        public boolean hasTimeZone(String id) throws IOException {
            checkNotClosed();
            return this.cache.get(id) != null;
        }

        public void close() {
            if (!this.closed) {
                this.closed = true;
                this.ids = null;
                this.byteOffsets = null;
                this.rawUtcOffsetsCache = null;
                this.cache.evictAll();
                if (this.mappedFile != null) {
                    try {
                        this.mappedFile.close();
                    } catch (ErrnoException e) {
                    }
                    this.mappedFile = null;
                }
            }
        }

        private void checkNotClosed() throws IllegalStateException {
            if (this.closed) {
                throw new IllegalStateException("TzData is closed");
            }
        }

        protected void finalize() throws Throwable {
            try {
                close();
            } finally {
                super.finalize();
            }
        }

        public static String getRulesVersion(File tzDataFile) throws IOException {
            Throwable th;
            FileInputStream is = new FileInputStream(tzDataFile);
            try {
                byte[] tzdataVersion = new byte[12];
                int bytesRead = is.read(tzdataVersion, 0, 12);
                if (bytesRead != 12) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("File too short: only able to read ");
                    stringBuilder.append(bytesRead);
                    stringBuilder.append(" bytes.");
                    throw new IOException(stringBuilder.toString());
                } else if (new String(tzdataVersion, 0, 6, StandardCharsets.US_ASCII).equals(ZoneInfoDB.TZDATA_FILE) && tzdataVersion[11] == (byte) 0) {
                    String str = new String(tzdataVersion, 6, 5, StandardCharsets.US_ASCII);
                    is.close();
                    return str;
                } else {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("bad tzdata magic: ");
                    stringBuilder2.append(Arrays.toString(tzdataVersion));
                    throw new IOException(stringBuilder2.toString());
                }
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
        }
    }

    private ZoneInfoDB() {
    }

    public static TzData getInstance() {
        return DATA;
    }
}
