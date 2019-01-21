package com.huawei.okhttp3.internal.cache;

import com.huawei.okhttp3.internal.Util;
import com.huawei.okhttp3.internal.io.FileSystem;
import com.huawei.okhttp3.internal.platform.Platform;
import com.huawei.okio.BufferedSink;
import com.huawei.okio.Okio;
import com.huawei.okio.Sink;
import com.huawei.okio.Source;
import java.io.Closeable;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.Flushable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public final class DiskLruCache implements Closeable, Flushable {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    static final long ANY_SEQUENCE_NUMBER = -1;
    private static final String CLEAN = "CLEAN";
    private static final String DIRTY = "DIRTY";
    static final String JOURNAL_FILE = "journal";
    static final String JOURNAL_FILE_BACKUP = "journal.bkp";
    static final String JOURNAL_FILE_TEMP = "journal.tmp";
    static final Pattern LEGAL_KEY_PATTERN = Pattern.compile("[a-z0-9_-]{1,120}");
    static final String MAGIC = "libcore.io.DiskLruCache";
    private static final String READ = "READ";
    private static final String REMOVE = "REMOVE";
    static final String VERSION_1 = "1";
    private final int appVersion;
    private final Runnable cleanupRunnable = new Runnable() {
        public void run() {
            synchronized (DiskLruCache.this) {
                if (((DiskLruCache.this.initialized ^ 1) | DiskLruCache.this.closed) != 0) {
                    return;
                }
                try {
                    DiskLruCache.this.trimToSize();
                } catch (IOException e) {
                    DiskLruCache.this.mostRecentTrimFailed = true;
                }
                try {
                    if (DiskLruCache.this.journalRebuildRequired()) {
                        DiskLruCache.this.rebuildJournal();
                        DiskLruCache.this.redundantOpCount = 0;
                    }
                } catch (IOException e2) {
                    DiskLruCache.this.mostRecentRebuildFailed = true;
                    DiskLruCache.this.journalWriter = Okio.buffer(Okio.blackhole());
                }
            }
            return;
        }
    };
    boolean closed;
    final File directory;
    private final Executor executor;
    final FileSystem fileSystem;
    boolean hasJournalErrors;
    boolean initialized;
    private final File journalFile;
    private final File journalFileBackup;
    private final File journalFileTmp;
    BufferedSink journalWriter;
    final LinkedHashMap<String, Entry> lruEntries = new LinkedHashMap(0, 0.75f, true);
    private long maxSize;
    boolean mostRecentRebuildFailed;
    boolean mostRecentTrimFailed;
    private long nextSequenceNumber = 0;
    int redundantOpCount;
    private long size = 0;
    final int valueCount;

    public final class Editor {
        private boolean done;
        final Entry entry;
        final boolean[] written;

        Editor(Entry entry) {
            this.entry = entry;
            this.written = entry.readable ? null : new boolean[DiskLruCache.this.valueCount];
        }

        void detach() {
            if (this.entry.currentEditor == this) {
                for (int i = 0; i < DiskLruCache.this.valueCount; i++) {
                    try {
                        DiskLruCache.this.fileSystem.delete(this.entry.dirtyFiles[i]);
                    } catch (IOException e) {
                    }
                }
                this.entry.currentEditor = null;
            }
        }

        /* JADX WARNING: Missing block: B:18:0x0029, code skipped:
            return null;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public Source newSource(int index) {
            synchronized (DiskLruCache.this) {
                if (this.done) {
                    throw new IllegalStateException();
                } else if (this.entry.readable && this.entry.currentEditor == this) {
                    try {
                        Source source = DiskLruCache.this.fileSystem.source(this.entry.cleanFiles[index]);
                        return source;
                    } catch (FileNotFoundException e) {
                        return null;
                    }
                }
            }
        }

        public Sink newSink(int index) {
            synchronized (DiskLruCache.this) {
                if (this.done) {
                    throw new IllegalStateException();
                } else if (this.entry.currentEditor != this) {
                    Sink blackhole = Okio.blackhole();
                    return blackhole;
                } else {
                    if (!this.entry.readable) {
                        this.written[index] = true;
                    }
                    try {
                        AnonymousClass1 anonymousClass1 = new FaultHidingSink(DiskLruCache.this.fileSystem.sink(this.entry.dirtyFiles[index])) {
                            protected void onException(IOException e) {
                                synchronized (DiskLruCache.this) {
                                    Editor.this.detach();
                                }
                            }
                        };
                        return anonymousClass1;
                    } catch (FileNotFoundException e) {
                        return Okio.blackhole();
                    }
                }
            }
        }

        public void commit() throws IOException {
            synchronized (DiskLruCache.this) {
                if (this.done) {
                    throw new IllegalStateException();
                }
                if (this.entry.currentEditor == this) {
                    DiskLruCache.this.completeEdit(this, true);
                }
                this.done = true;
            }
        }

        public void abort() throws IOException {
            synchronized (DiskLruCache.this) {
                if (this.done) {
                    throw new IllegalStateException();
                }
                if (this.entry.currentEditor == this) {
                    DiskLruCache.this.completeEdit(this, false);
                }
                this.done = true;
            }
        }

        public void abortUnlessCommitted() {
            synchronized (DiskLruCache.this) {
                if (!this.done && this.entry.currentEditor == this) {
                    try {
                        DiskLruCache.this.completeEdit(this, false);
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    private final class Entry {
        final File[] cleanFiles;
        Editor currentEditor;
        final File[] dirtyFiles;
        final String key;
        final long[] lengths;
        boolean readable;
        long sequenceNumber;

        Entry(String key) {
            this.key = key;
            this.lengths = new long[DiskLruCache.this.valueCount];
            this.cleanFiles = new File[DiskLruCache.this.valueCount];
            this.dirtyFiles = new File[DiskLruCache.this.valueCount];
            StringBuilder fileBuilder = new StringBuilder(key).append('.');
            int truncateTo = fileBuilder.length();
            for (int i = 0; i < DiskLruCache.this.valueCount; i++) {
                fileBuilder.append(i);
                this.cleanFiles[i] = new File(DiskLruCache.this.directory, fileBuilder.toString());
                fileBuilder.append(".tmp");
                this.dirtyFiles[i] = new File(DiskLruCache.this.directory, fileBuilder.toString());
                fileBuilder.setLength(truncateTo);
            }
        }

        void setLengths(String[] strings) throws IOException {
            if (strings.length == DiskLruCache.this.valueCount) {
                int i = 0;
                while (i < strings.length) {
                    try {
                        this.lengths[i] = Long.parseLong(strings[i]);
                        i++;
                    } catch (NumberFormatException e) {
                        throw invalidLengths(strings);
                    }
                }
                return;
            }
            throw invalidLengths(strings);
        }

        void writeLengths(BufferedSink writer) throws IOException {
            for (long length : this.lengths) {
                writer.writeByte(32).writeDecimalLong(length);
            }
        }

        private IOException invalidLengths(String[] strings) throws IOException {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unexpected journal line: ");
            stringBuilder.append(Arrays.toString(strings));
            throw new IOException(stringBuilder.toString());
        }

        Snapshot snapshot() {
            if (Thread.holdsLock(DiskLruCache.this)) {
                Source[] sources = new Source[DiskLruCache.this.valueCount];
                long[] lengths = (long[]) this.lengths.clone();
                int i = 0;
                int i2 = 0;
                while (i2 < DiskLruCache.this.valueCount) {
                    try {
                        sources[i2] = DiskLruCache.this.fileSystem.source(this.cleanFiles[i2]);
                        i2++;
                    } catch (FileNotFoundException e) {
                        while (true) {
                            int i3 = i;
                            if (i3 >= DiskLruCache.this.valueCount || sources[i3] == null) {
                                try {
                                    DiskLruCache.this.removeEntry(this);
                                } catch (IOException e2) {
                                }
                                return null;
                            }
                            Util.closeQuietly(sources[i3]);
                            i = i3 + 1;
                        }
                    }
                }
                return new Snapshot(this.key, this.sequenceNumber, sources, lengths);
            }
            throw new AssertionError();
        }
    }

    public final class Snapshot implements Closeable {
        private final String key;
        private final long[] lengths;
        private final long sequenceNumber;
        private final Source[] sources;

        Snapshot(String key, long sequenceNumber, Source[] sources, long[] lengths) {
            this.key = key;
            this.sequenceNumber = sequenceNumber;
            this.sources = sources;
            this.lengths = lengths;
        }

        public String key() {
            return this.key;
        }

        public Editor edit() throws IOException {
            return DiskLruCache.this.edit(this.key, this.sequenceNumber);
        }

        public Source getSource(int index) {
            return this.sources[index];
        }

        public long getLength(int index) {
            return this.lengths[index];
        }

        public void close() {
            for (Closeable in : this.sources) {
                Util.closeQuietly(in);
            }
        }
    }

    DiskLruCache(FileSystem fileSystem, File directory, int appVersion, int valueCount, long maxSize, Executor executor) {
        this.fileSystem = fileSystem;
        this.directory = directory;
        this.appVersion = appVersion;
        this.journalFile = new File(directory, JOURNAL_FILE);
        this.journalFileTmp = new File(directory, JOURNAL_FILE_TEMP);
        this.journalFileBackup = new File(directory, JOURNAL_FILE_BACKUP);
        this.valueCount = valueCount;
        this.maxSize = maxSize;
        this.executor = executor;
    }

    public synchronized void initialize() throws IOException {
        if (!this.initialized) {
            if (this.fileSystem.exists(this.journalFileBackup)) {
                if (this.fileSystem.exists(this.journalFile)) {
                    this.fileSystem.delete(this.journalFileBackup);
                } else {
                    this.fileSystem.rename(this.journalFileBackup, this.journalFile);
                }
            }
            if (this.fileSystem.exists(this.journalFile)) {
                try {
                    readJournal();
                    processJournal();
                    this.initialized = true;
                    return;
                } catch (IOException journalIsCorrupt) {
                    Platform platform = Platform.get();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("DiskLruCache ");
                    stringBuilder.append(this.directory);
                    stringBuilder.append(" is corrupt: ");
                    stringBuilder.append(journalIsCorrupt.getMessage());
                    stringBuilder.append(", removing");
                    platform.log(5, stringBuilder.toString(), journalIsCorrupt);
                    delete();
                    this.closed = false;
                }
            }
            rebuildJournal();
            this.initialized = true;
        }
    }

    public static DiskLruCache create(FileSystem fileSystem, File directory, int appVersion, int valueCount, long maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        } else if (valueCount > 0) {
            return new DiskLruCache(fileSystem, directory, appVersion, valueCount, maxSize, new ThreadPoolExecutor(0, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue(), Util.threadFactory("OkHttp DiskLruCache", true)));
        } else {
            throw new IllegalArgumentException("valueCount <= 0");
        }
    }

    private void readJournal() throws IOException {
        Closeable source = Okio.buffer(this.fileSystem.source(this.journalFile));
        int lineCount;
        try {
            String magic = source.readUtf8LineStrict();
            String version = source.readUtf8LineStrict();
            String appVersionString = source.readUtf8LineStrict();
            String valueCountString = source.readUtf8LineStrict();
            String blank = source.readUtf8LineStrict();
            if (MAGIC.equals(magic) && VERSION_1.equals(version) && Integer.toString(this.appVersion).equals(appVersionString) && Integer.toString(this.valueCount).equals(valueCountString) && "".equals(blank)) {
                lineCount = 0;
                while (true) {
                    readJournalLine(source.readUtf8LineStrict());
                    lineCount++;
                }
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unexpected journal header: [");
                stringBuilder.append(magic);
                stringBuilder.append(", ");
                stringBuilder.append(version);
                stringBuilder.append(", ");
                stringBuilder.append(valueCountString);
                stringBuilder.append(", ");
                stringBuilder.append(blank);
                stringBuilder.append("]");
                throw new IOException(stringBuilder.toString());
            }
        } catch (EOFException e) {
            this.redundantOpCount = lineCount - this.lruEntries.size();
            if (source.exhausted()) {
                this.journalWriter = newJournalWriter();
            } else {
                rebuildJournal();
            }
            Util.closeQuietly(source);
        } catch (Throwable th) {
            Util.closeQuietly(source);
        }
    }

    private BufferedSink newJournalWriter() throws FileNotFoundException {
        return Okio.buffer(new FaultHidingSink(this.fileSystem.appendingSink(this.journalFile)) {
            static final /* synthetic */ boolean $assertionsDisabled = false;

            static {
                Class cls = DiskLruCache.class;
            }

            protected void onException(IOException e) {
                DiskLruCache.this.hasJournalErrors = true;
            }
        });
    }

    private void readJournalLine(String line) throws IOException {
        int firstSpace = line.indexOf(32);
        if (firstSpace != -1) {
            String key;
            int keyBegin = firstSpace + 1;
            int secondSpace = line.indexOf(32, keyBegin);
            if (secondSpace == -1) {
                key = line.substring(keyBegin);
                if (firstSpace == REMOVE.length() && line.startsWith(REMOVE)) {
                    this.lruEntries.remove(key);
                    return;
                }
            }
            key = line.substring(keyBegin, secondSpace);
            Entry entry = (Entry) this.lruEntries.get(key);
            if (entry == null) {
                entry = new Entry(key);
                this.lruEntries.put(key, entry);
            }
            if (secondSpace != -1 && firstSpace == CLEAN.length() && line.startsWith(CLEAN)) {
                String[] parts = line.substring(secondSpace + 1).split(" ");
                entry.readable = true;
                entry.currentEditor = null;
                entry.setLengths(parts);
            } else if (secondSpace == -1 && firstSpace == DIRTY.length() && line.startsWith(DIRTY)) {
                entry.currentEditor = new Editor(entry);
            } else if (!(secondSpace == -1 && firstSpace == READ.length() && line.startsWith(READ))) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unexpected journal line: ");
                stringBuilder.append(line);
                throw new IOException(stringBuilder.toString());
            }
            return;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("unexpected journal line: ");
        stringBuilder2.append(line);
        throw new IOException(stringBuilder2.toString());
    }

    private void processJournal() throws IOException {
        this.fileSystem.delete(this.journalFileTmp);
        Iterator<Entry> i = this.lruEntries.values().iterator();
        while (i.hasNext()) {
            Entry entry = (Entry) i.next();
            int t = 0;
            int t2;
            if (entry.currentEditor == null) {
                while (true) {
                    t2 = t;
                    if (t2 >= this.valueCount) {
                        break;
                    }
                    this.size += entry.lengths[t2];
                    t = t2 + 1;
                }
            } else {
                entry.currentEditor = null;
                while (true) {
                    t2 = t;
                    if (t2 >= this.valueCount) {
                        break;
                    }
                    this.fileSystem.delete(entry.cleanFiles[t2]);
                    this.fileSystem.delete(entry.dirtyFiles[t2]);
                    t = t2 + 1;
                }
                i.remove();
            }
        }
    }

    synchronized void rebuildJournal() throws IOException {
        if (this.journalWriter != null) {
            this.journalWriter.close();
        }
        BufferedSink writer = Okio.buffer(this.fileSystem.sink(this.journalFileTmp));
        try {
            writer.writeUtf8(MAGIC).writeByte(10);
            writer.writeUtf8(VERSION_1).writeByte(10);
            writer.writeDecimalLong((long) this.appVersion).writeByte(10);
            writer.writeDecimalLong((long) this.valueCount).writeByte(10);
            writer.writeByte(10);
            for (Entry entry : this.lruEntries.values()) {
                if (entry.currentEditor != null) {
                    writer.writeUtf8(DIRTY).writeByte(32);
                    writer.writeUtf8(entry.key);
                    writer.writeByte(10);
                } else {
                    writer.writeUtf8(CLEAN).writeByte(32);
                    writer.writeUtf8(entry.key);
                    entry.writeLengths(writer);
                    writer.writeByte(10);
                }
            }
            if (this.fileSystem.exists(this.journalFile)) {
                this.fileSystem.rename(this.journalFile, this.journalFileBackup);
            }
            this.fileSystem.rename(this.journalFileTmp, this.journalFile);
            this.fileSystem.delete(this.journalFileBackup);
            this.journalWriter = newJournalWriter();
            this.hasJournalErrors = false;
            this.mostRecentRebuildFailed = false;
        } finally {
            writer.close();
        }
    }

    /* JADX WARNING: Missing block: B:16:0x004d, code skipped:
            return r2;
     */
    /* JADX WARNING: Missing block: B:18:0x004f, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized Snapshot get(String key) throws IOException {
        initialize();
        checkNotClosed();
        validateKey(key);
        Entry entry = (Entry) this.lruEntries.get(key);
        if (entry != null) {
            if (entry.readable) {
                Snapshot snapshot = entry.snapshot();
                if (snapshot == null) {
                    return null;
                }
                this.redundantOpCount++;
                this.journalWriter.writeUtf8(READ).writeByte(32).writeUtf8(key).writeByte(10);
                if (journalRebuildRequired()) {
                    this.executor.execute(this.cleanupRunnable);
                }
            }
        }
    }

    public Editor edit(String key) throws IOException {
        return edit(key, ANY_SEQUENCE_NUMBER);
    }

    /* JADX WARNING: Missing block: B:9:0x0022, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    synchronized Editor edit(String key, long expectedSequenceNumber) throws IOException {
        initialize();
        checkNotClosed();
        validateKey(key);
        Entry entry = (Entry) this.lruEntries.get(key);
        if (expectedSequenceNumber == ANY_SEQUENCE_NUMBER || (entry != null && entry.sequenceNumber == expectedSequenceNumber)) {
            if (entry != null) {
                if (entry.currentEditor != null) {
                    return null;
                }
            }
            if (!this.mostRecentTrimFailed) {
                if (!this.mostRecentRebuildFailed) {
                    this.journalWriter.writeUtf8(DIRTY).writeByte(32).writeUtf8(key).writeByte(10);
                    this.journalWriter.flush();
                    if (this.hasJournalErrors) {
                        return null;
                    }
                    if (entry == null) {
                        entry = new Entry(key);
                        this.lruEntries.put(key, entry);
                    }
                    Editor editor = new Editor(entry);
                    entry.currentEditor = editor;
                    return editor;
                }
            }
            this.executor.execute(this.cleanupRunnable);
            return null;
        }
    }

    public File getDirectory() {
        return this.directory;
    }

    public synchronized long getMaxSize() {
        return this.maxSize;
    }

    public synchronized void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
        if (this.initialized) {
            this.executor.execute(this.cleanupRunnable);
        }
    }

    public synchronized long size() throws IOException {
        initialize();
        return this.size;
    }

    /* JADX WARNING: Missing block: B:44:0x00f5, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    synchronized void completeEdit(Editor editor, boolean success) throws IOException {
        Entry entry = editor.entry;
        if (entry.currentEditor == editor) {
            int i = 0;
            if (success && !entry.readable) {
                int i2 = 0;
                while (i2 < this.valueCount) {
                    if (!editor.written[i2]) {
                        editor.abort();
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Newly created entry didn't create value for index ");
                        stringBuilder.append(i2);
                        throw new IllegalStateException(stringBuilder.toString());
                    } else if (this.fileSystem.exists(entry.dirtyFiles[i2])) {
                        i2++;
                    } else {
                        editor.abort();
                        return;
                    }
                }
            }
            while (i < this.valueCount) {
                File dirty = entry.dirtyFiles[i];
                if (!success) {
                    this.fileSystem.delete(dirty);
                } else if (this.fileSystem.exists(dirty)) {
                    File clean = entry.cleanFiles[i];
                    this.fileSystem.rename(dirty, clean);
                    long oldLength = entry.lengths[i];
                    long newLength = this.fileSystem.size(clean);
                    entry.lengths[i] = newLength;
                    this.size = (this.size - oldLength) + newLength;
                }
                i++;
            }
            this.redundantOpCount++;
            entry.currentEditor = null;
            if ((entry.readable | success) != 0) {
                entry.readable = true;
                this.journalWriter.writeUtf8(CLEAN).writeByte(32);
                this.journalWriter.writeUtf8(entry.key);
                entry.writeLengths(this.journalWriter);
                this.journalWriter.writeByte(10);
                if (success) {
                    long j = this.nextSequenceNumber;
                    this.nextSequenceNumber = 1 + j;
                    entry.sequenceNumber = j;
                }
            } else {
                this.lruEntries.remove(entry.key);
                this.journalWriter.writeUtf8(REMOVE).writeByte(32);
                this.journalWriter.writeUtf8(entry.key);
                this.journalWriter.writeByte(10);
            }
            this.journalWriter.flush();
            if (this.size > this.maxSize || journalRebuildRequired()) {
                this.executor.execute(this.cleanupRunnable);
            }
        } else {
            throw new IllegalStateException();
        }
    }

    boolean journalRebuildRequired() {
        return this.redundantOpCount >= 2000 && this.redundantOpCount >= this.lruEntries.size();
    }

    /* JADX WARNING: Missing block: B:14:0x0028, code skipped:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean remove(String key) throws IOException {
        initialize();
        checkNotClosed();
        validateKey(key);
        Entry entry = (Entry) this.lruEntries.get(key);
        if (entry == null) {
            return false;
        }
        boolean removed = removeEntry(entry);
        if (removed && this.size <= this.maxSize) {
            this.mostRecentTrimFailed = false;
        }
    }

    boolean removeEntry(Entry entry) throws IOException {
        if (entry.currentEditor != null) {
            entry.currentEditor.detach();
        }
        for (int i = 0; i < this.valueCount; i++) {
            this.fileSystem.delete(entry.cleanFiles[i]);
            this.size -= entry.lengths[i];
            entry.lengths[i] = 0;
        }
        this.redundantOpCount++;
        this.journalWriter.writeUtf8(REMOVE).writeByte(32).writeUtf8(entry.key).writeByte(10);
        this.lruEntries.remove(entry.key);
        if (journalRebuildRequired()) {
            this.executor.execute(this.cleanupRunnable);
        }
        return true;
    }

    public synchronized boolean isClosed() {
        return this.closed;
    }

    private synchronized void checkNotClosed() {
        if (isClosed()) {
            throw new IllegalStateException("cache is closed");
        }
    }

    public synchronized void flush() throws IOException {
        if (this.initialized) {
            checkNotClosed();
            trimToSize();
            this.journalWriter.flush();
        }
    }

    public synchronized void close() throws IOException {
        if (this.initialized) {
            if (!this.closed) {
                for (Entry entry : (Entry[]) this.lruEntries.values().toArray(new Entry[this.lruEntries.size()])) {
                    if (entry.currentEditor != null) {
                        entry.currentEditor.abort();
                    }
                }
                trimToSize();
                this.journalWriter.close();
                this.journalWriter = null;
                this.closed = true;
                return;
            }
        }
        this.closed = true;
    }

    void trimToSize() throws IOException {
        while (this.size > this.maxSize) {
            removeEntry((Entry) this.lruEntries.values().iterator().next());
        }
        this.mostRecentTrimFailed = false;
    }

    public void delete() throws IOException {
        close();
        this.fileSystem.deleteContents(this.directory);
    }

    public synchronized void evictAll() throws IOException {
        initialize();
        for (Entry entry : (Entry[]) this.lruEntries.values().toArray(new Entry[this.lruEntries.size()])) {
            removeEntry(entry);
        }
        this.mostRecentTrimFailed = false;
    }

    private void validateKey(String key) {
        if (!LEGAL_KEY_PATTERN.matcher(key).matches()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("keys must match regex [a-z0-9_-]{1,120}: \"");
            stringBuilder.append(key);
            stringBuilder.append("\"");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public synchronized Iterator<Snapshot> snapshots() throws IOException {
        initialize();
        return new Iterator<Snapshot>() {
            final Iterator<Entry> delegate = new ArrayList(DiskLruCache.this.lruEntries.values()).iterator();
            Snapshot nextSnapshot;
            Snapshot removeSnapshot;

            public boolean hasNext() {
                if (this.nextSnapshot != null) {
                    return true;
                }
                synchronized (DiskLruCache.this) {
                    if (DiskLruCache.this.closed) {
                        return false;
                    }
                    while (this.delegate.hasNext()) {
                        Snapshot snapshot = ((Entry) this.delegate.next()).snapshot();
                        if (snapshot != null) {
                            this.nextSnapshot = snapshot;
                            return true;
                        }
                    }
                    return false;
                }
            }

            public Snapshot next() {
                if (hasNext()) {
                    this.removeSnapshot = this.nextSnapshot;
                    this.nextSnapshot = null;
                    return this.removeSnapshot;
                }
                throw new NoSuchElementException();
            }

            public void remove() {
                if (this.removeSnapshot != null) {
                    try {
                        DiskLruCache.this.remove(this.removeSnapshot.key);
                    } catch (IOException e) {
                    } catch (Throwable th) {
                        this.removeSnapshot = null;
                    }
                    this.removeSnapshot = null;
                    return;
                }
                throw new IllegalStateException("remove() before next()");
            }
        };
    }
}
