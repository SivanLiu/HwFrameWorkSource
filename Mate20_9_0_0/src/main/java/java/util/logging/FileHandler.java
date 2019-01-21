package java.util.logging;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Set;

public class FileHandler extends StreamHandler {
    private static final int MAX_LOCKS = 100;
    private static final Set<String> locks = new HashSet();
    private boolean append;
    private int count;
    private File[] files;
    private int limit;
    private FileChannel lockFileChannel;
    private String lockFileName;
    private MeteredStream meter;
    private String pattern;

    private static class InitializationErrorManager extends ErrorManager {
        Exception lastException;

        private InitializationErrorManager() {
        }

        /* synthetic */ InitializationErrorManager(AnonymousClass1 x0) {
            this();
        }

        public void error(String msg, Exception ex, int code) {
            this.lastException = ex;
        }
    }

    private class MeteredStream extends OutputStream {
        final OutputStream out;
        int written;

        MeteredStream(OutputStream out, int written) {
            this.out = out;
            this.written = written;
        }

        public void write(int b) throws IOException {
            this.out.write(b);
            this.written++;
        }

        public void write(byte[] buff) throws IOException {
            this.out.write(buff);
            this.written += buff.length;
        }

        public void write(byte[] buff, int off, int len) throws IOException {
            this.out.write(buff, off, len);
            this.written += len;
        }

        public void flush() throws IOException {
            this.out.flush();
        }

        public void close() throws IOException {
            this.out.close();
        }
    }

    private void open(File fname, boolean append) throws IOException {
        int len = 0;
        if (append) {
            len = (int) fname.length();
        }
        this.meter = new MeteredStream(new BufferedOutputStream(new FileOutputStream(fname.toString(), append)), len);
        setOutputStream(this.meter);
    }

    private void configure() {
        LogManager manager = LogManager.getLogManager();
        String cname = getClass().getName();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(cname);
        stringBuilder.append(".pattern");
        this.pattern = manager.getStringProperty(stringBuilder.toString(), "%h/java%u.log");
        stringBuilder = new StringBuilder();
        stringBuilder.append(cname);
        stringBuilder.append(".limit");
        this.limit = manager.getIntProperty(stringBuilder.toString(), 0);
        if (this.limit < 0) {
            this.limit = 0;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(cname);
        stringBuilder.append(".count");
        this.count = manager.getIntProperty(stringBuilder.toString(), 1);
        if (this.count <= 0) {
            this.count = 1;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(cname);
        stringBuilder.append(".append");
        this.append = manager.getBooleanProperty(stringBuilder.toString(), false);
        stringBuilder = new StringBuilder();
        stringBuilder.append(cname);
        stringBuilder.append(".level");
        setLevel(manager.getLevelProperty(stringBuilder.toString(), Level.ALL));
        stringBuilder = new StringBuilder();
        stringBuilder.append(cname);
        stringBuilder.append(".filter");
        setFilter(manager.getFilterProperty(stringBuilder.toString(), null));
        stringBuilder = new StringBuilder();
        stringBuilder.append(cname);
        stringBuilder.append(".formatter");
        setFormatter(manager.getFormatterProperty(stringBuilder.toString(), new XMLFormatter()));
        try {
            stringBuilder = new StringBuilder();
            stringBuilder.append(cname);
            stringBuilder.append(".encoding");
            setEncoding(manager.getStringProperty(stringBuilder.toString(), null));
        } catch (Exception e) {
            try {
                setEncoding(null);
            } catch (Exception e2) {
            }
        }
    }

    public FileHandler() throws IOException, SecurityException {
        checkPermission();
        configure();
        openFiles();
    }

    public FileHandler(String pattern) throws IOException, SecurityException {
        if (pattern.length() >= 1) {
            checkPermission();
            configure();
            this.pattern = pattern;
            this.limit = 0;
            this.count = 1;
            openFiles();
            return;
        }
        throw new IllegalArgumentException();
    }

    public FileHandler(String pattern, boolean append) throws IOException, SecurityException {
        if (pattern.length() >= 1) {
            checkPermission();
            configure();
            this.pattern = pattern;
            this.limit = 0;
            this.count = 1;
            this.append = append;
            openFiles();
            return;
        }
        throw new IllegalArgumentException();
    }

    public FileHandler(String pattern, int limit, int count) throws IOException, SecurityException {
        if (limit < 0 || count < 1 || pattern.length() < 1) {
            throw new IllegalArgumentException();
        }
        checkPermission();
        configure();
        this.pattern = pattern;
        this.limit = limit;
        this.count = count;
        openFiles();
    }

    public FileHandler(String pattern, int limit, int count, boolean append) throws IOException, SecurityException {
        if (limit < 0 || count < 1 || pattern.length() < 1) {
            throw new IllegalArgumentException();
        }
        checkPermission();
        configure();
        this.pattern = pattern;
        this.limit = limit;
        this.count = count;
        this.append = append;
        openFiles();
    }

    private boolean isParentWritable(Path path) {
        Path parent = path.getParent();
        if (parent == null) {
            parent = path.toAbsolutePath().getParent();
        }
        return parent != null && Files.isWritable(parent);
    }

    /* JADX WARNING: Missing block: B:53:?, code skipped:
            locks.add(r14.lockFileName);
     */
    /* JADX WARNING: Missing block: B:55:0x00c4, code skipped:
            r14.files = new java.io.File[r14.count];
            r5 = 0;
     */
    /* JADX WARNING: Missing block: B:57:0x00cd, code skipped:
            if (r5 >= r14.count) goto L_0x00dc;
     */
    /* JADX WARNING: Missing block: B:58:0x00cf, code skipped:
            r14.files[r5] = generate(r14.pattern, r5, r4);
            r5 = r5 + 1;
     */
    /* JADX WARNING: Missing block: B:60:0x00de, code skipped:
            if (r14.append == false) goto L_0x00e8;
     */
    /* JADX WARNING: Missing block: B:61:0x00e0, code skipped:
            open(r14.files[0], true);
     */
    /* JADX WARNING: Missing block: B:62:0x00e8, code skipped:
            rotate();
     */
    /* JADX WARNING: Missing block: B:63:0x00eb, code skipped:
            r2 = r1.lastException;
     */
    /* JADX WARNING: Missing block: B:64:0x00ed, code skipped:
            if (r2 == null) goto L_0x0116;
     */
    /* JADX WARNING: Missing block: B:66:0x00f1, code skipped:
            if ((r2 instanceof java.io.IOException) != false) goto L_0x0112;
     */
    /* JADX WARNING: Missing block: B:68:0x00f5, code skipped:
            if ((r2 instanceof java.lang.SecurityException) == false) goto L_0x00fb;
     */
    /* JADX WARNING: Missing block: B:70:0x00fa, code skipped:
            throw ((java.lang.SecurityException) r2);
     */
    /* JADX WARNING: Missing block: B:71:0x00fb, code skipped:
            r5 = new java.lang.StringBuilder();
            r5.append("Exception: ");
            r5.append(r2);
     */
    /* JADX WARNING: Missing block: B:72:0x0111, code skipped:
            throw new java.io.IOException(r5.toString());
     */
    /* JADX WARNING: Missing block: B:74:0x0115, code skipped:
            throw ((java.io.IOException) r2);
     */
    /* JADX WARNING: Missing block: B:75:0x0116, code skipped:
            setErrorManager(new java.util.logging.ErrorManager());
     */
    /* JADX WARNING: Missing block: B:76:0x011e, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void openFiles() throws IOException {
        LogManager.getLogManager().checkPermission();
        if (this.count >= 1) {
            if (this.limit < 0) {
                this.limit = 0;
            }
            InitializationErrorManager em = new InitializationErrorManager();
            setErrorManager(em);
            int unique = -1;
            while (true) {
                unique++;
                if (unique <= MAX_LOCKS) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(generate(this.pattern, 0, unique).toString());
                    stringBuilder.append(".lck");
                    this.lockFileName = stringBuilder.toString();
                    synchronized (locks) {
                        if (locks.contains(this.lockFileName)) {
                        } else {
                            Path lockFilePath = Paths.get(this.lockFileName, new String[0]);
                            int retries = -1;
                            FileChannel channel = null;
                            boolean fileCreated = false;
                            while (channel == null) {
                                int retries2 = retries + 1;
                                if (retries >= 1) {
                                    break;
                                }
                                try {
                                    channel = FileChannel.open(lockFilePath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                                    fileCreated = true;
                                } catch (FileAlreadyExistsException e) {
                                    if (Files.isRegularFile(lockFilePath, LinkOption.NOFOLLOW_LINKS) && isParentWritable(lockFilePath)) {
                                        try {
                                            channel = FileChannel.open(lockFilePath, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
                                        } catch (NoSuchFileException e2) {
                                        } catch (IOException e3) {
                                        }
                                    }
                                }
                                retries = retries2;
                            }
                            if (channel != null) {
                                boolean available;
                                this.lockFileChannel = channel;
                                try {
                                    available = this.lockFileChannel.tryLock() != null;
                                } catch (IOException e4) {
                                    available = fileCreated;
                                } catch (OverlappingFileLockException e5) {
                                    available = false;
                                }
                                if (available) {
                                    break;
                                }
                                this.lockFileChannel.close();
                            }
                        }
                    }
                } else {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Couldn't get lock for ");
                    stringBuilder2.append(this.pattern);
                    throw new IOException(stringBuilder2.toString());
                }
            }
        }
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("file count = ");
        stringBuilder3.append(this.count);
        throw new IllegalArgumentException(stringBuilder3.toString());
    }

    private File generate(String pattern, int generation, int unique) throws IOException {
        StringBuilder stringBuilder;
        File file = null;
        String word = "";
        int ix = 0;
        boolean sawg = false;
        boolean sawu = false;
        while (ix < pattern.length()) {
            char ch = pattern.charAt(ix);
            ix++;
            char ch2 = 0;
            if (ix < pattern.length()) {
                ch2 = Character.toLowerCase(pattern.charAt(ix));
            }
            if (ch == '/') {
                File file2;
                if (file == null) {
                    file2 = new File(word);
                } else {
                    file2 = new File(file, word);
                }
                file = file2;
                word = "";
            } else {
                StringBuilder stringBuilder2;
                if (ch == '%') {
                    if (ch2 == 't') {
                        String tmpDir = System.getProperty("java.io.tmpdir");
                        if (tmpDir == null) {
                            tmpDir = System.getProperty("user.home");
                        }
                        file = new File(tmpDir);
                        ix++;
                        word = "";
                    } else if (ch2 == 'h') {
                        file = new File(System.getProperty("user.home"));
                        ix++;
                        word = "";
                    } else if (ch2 == 'g') {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(word);
                        stringBuilder2.append(generation);
                        word = stringBuilder2.toString();
                        sawg = true;
                        ix++;
                    } else if (ch2 == 'u') {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(word);
                        stringBuilder2.append(unique);
                        word = stringBuilder2.toString();
                        sawu = true;
                        ix++;
                    } else if (ch2 == '%') {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(word);
                        stringBuilder2.append("%");
                        word = stringBuilder2.toString();
                        ix++;
                    }
                }
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(word);
                stringBuilder2.append(ch);
                word = stringBuilder2.toString();
            }
        }
        if (this.count > 1 && !sawg) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(word);
            stringBuilder.append(".");
            stringBuilder.append(generation);
            word = stringBuilder.toString();
        }
        if (unique > 0 && !sawu) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(word);
            stringBuilder.append(".");
            stringBuilder.append(unique);
            word = stringBuilder.toString();
        }
        if (word.length() <= 0) {
            return file;
        }
        if (file == null) {
            return new File(word);
        }
        return new File(file, word);
    }

    private synchronized void rotate() {
        Level oldLevel = getLevel();
        setLevel(Level.OFF);
        super.close();
        for (int i = this.count - 2; i >= 0; i--) {
            File f1 = this.files[i];
            File f2 = this.files[i + 1];
            if (f1.exists()) {
                if (f2.exists()) {
                    f2.delete();
                }
                f1.renameTo(f2);
            }
        }
        try {
            open(this.files[0], false);
        } catch (IOException ix) {
            reportError(null, ix, 4);
        }
        setLevel(oldLevel);
    }

    /* JADX WARNING: Missing block: B:13:0x0024, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void publish(LogRecord record) {
        if (isLoggable(record)) {
            super.publish(record);
            flush();
            if (this.limit > 0 && this.meter.written >= this.limit) {
                AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    public Object run() {
                        FileHandler.this.rotate();
                        return null;
                    }
                });
            }
        }
    }

    public synchronized void close() throws SecurityException {
        super.close();
        if (this.lockFileName != null) {
            try {
                this.lockFileChannel.close();
            } catch (Exception e) {
            }
            synchronized (locks) {
                locks.remove(this.lockFileName);
            }
            new File(this.lockFileName).delete();
            this.lockFileName = null;
            this.lockFileChannel = null;
        }
    }
}
