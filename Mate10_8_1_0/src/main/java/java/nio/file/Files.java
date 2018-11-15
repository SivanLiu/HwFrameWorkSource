package java.nio.file;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.spi.FileSystemProvider;
import java.nio.file.spi.FileTypeDetector;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Spliterators;
import java.util.function.BiPredicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import sun.nio.fs.DefaultFileTypeDetector;

public final class Files {
    static final /* synthetic */ boolean -assertionsDisabled = (Files.class.desiredAssertionStatus() ^ 1);
    private static final /* synthetic */ int[] -java-nio-file-FileTreeWalker$EventTypeSwitchesValues = null;
    private static final int BUFFER_SIZE = 8192;
    private static final int MAX_BUFFER_SIZE = 2147483639;

    private static class AcceptAllFilter implements Filter<Path> {
        static final AcceptAllFilter FILTER = new AcceptAllFilter();

        private AcceptAllFilter() {
        }

        public boolean accept(Path entry) {
            return true;
        }
    }

    private static class FileTypeDetectors {
        static final FileTypeDetector defaultFileTypeDetector = createDefaultFileTypeDetector();
        static final List<FileTypeDetector> installeDetectors = loadInstalledDetectors();

        private FileTypeDetectors() {
        }

        private static FileTypeDetector createDefaultFileTypeDetector() {
            return (FileTypeDetector) AccessController.doPrivileged(new PrivilegedAction<FileTypeDetector>() {
                public FileTypeDetector run() {
                    return DefaultFileTypeDetector.create();
                }
            });
        }

        private static List<FileTypeDetector> loadInstalledDetectors() {
            return (List) AccessController.doPrivileged(new PrivilegedAction<List<FileTypeDetector>>() {
                public List<FileTypeDetector> run() {
                    List<FileTypeDetector> list = new ArrayList();
                    for (FileTypeDetector detector : ServiceLoader.load(FileTypeDetector.class, ClassLoader.getSystemClassLoader())) {
                        list.add(detector);
                    }
                    return list;
                }
            });
        }
    }

    private static /* synthetic */ int[] -getjava-nio-file-FileTreeWalker$EventTypeSwitchesValues() {
        if (-java-nio-file-FileTreeWalker$EventTypeSwitchesValues != null) {
            return -java-nio-file-FileTreeWalker$EventTypeSwitchesValues;
        }
        int[] iArr = new int[EventType.values().length];
        try {
            iArr[EventType.END_DIRECTORY.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[EventType.ENTRY.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[EventType.START_DIRECTORY.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        -java-nio-file-FileTreeWalker$EventTypeSwitchesValues = iArr;
        return iArr;
    }

    private Files() {
    }

    private static FileSystemProvider provider(Path path) {
        return path.getFileSystem().provider();
    }

    private static Runnable asUncheckedRunnable(Closeable c) {
        return new java.nio.file.-$Lambda$iV0HzPWaaR68t7NV87hCwF49CFs.AnonymousClass1((byte) 0, c);
    }

    static /* synthetic */ void lambda$-java_nio_file_Files_3831(Closeable c) {
        try {
            c.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static InputStream newInputStream(Path path, OpenOption... options) throws IOException {
        return provider(path).newInputStream(path, options);
    }

    public static OutputStream newOutputStream(Path path, OpenOption... options) throws IOException {
        return provider(path).newOutputStream(path, options);
    }

    public static SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        return provider(path).newByteChannel(path, options, attrs);
    }

    public static SeekableByteChannel newByteChannel(Path path, OpenOption... options) throws IOException {
        Set<OpenOption> set = new HashSet(options.length);
        Collections.addAll(set, options);
        return newByteChannel(path, set, new FileAttribute[0]);
    }

    public static DirectoryStream<Path> newDirectoryStream(Path dir) throws IOException {
        return provider(dir).newDirectoryStream(dir, AcceptAllFilter.FILTER);
    }

    public static DirectoryStream<Path> newDirectoryStream(Path dir, String glob) throws IOException {
        if (glob.equals("*")) {
            return newDirectoryStream(dir);
        }
        FileSystem fs = dir.getFileSystem();
        final PathMatcher matcher = fs.getPathMatcher("glob:" + glob);
        return fs.provider().newDirectoryStream(dir, new Filter<Path>() {
            public boolean accept(Path entry) {
                return matcher.matches(entry.getFileName());
            }
        });
    }

    public static DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
        return provider(dir).newDirectoryStream(dir, filter);
    }

    public static Path createFile(Path path, FileAttribute<?>... attrs) throws IOException {
        newByteChannel(path, EnumSet.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE), attrs).close();
        return path;
    }

    public static Path createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        provider(dir).createDirectory(dir, attrs);
        return dir;
    }

    public static Path createDirectories(Path dir, FileAttribute<?>... attrs) throws IOException {
        try {
            createAndCheckIsDirectory(dir, attrs);
            return dir;
        } catch (FileAlreadyExistsException x) {
            throw x;
        } catch (IOException e) {
            SecurityException se = null;
            try {
                dir = dir.toAbsolutePath();
            } catch (SecurityException x2) {
                se = x2;
            }
            Path parent = dir.getParent();
            while (parent != null) {
                try {
                    provider(parent).checkAccess(parent, new AccessMode[0]);
                    break;
                } catch (NoSuchFileException e2) {
                    parent = parent.getParent();
                }
            }
            if (parent != null) {
                Path child = parent;
                for (Path name : parent.relativize(dir)) {
                    child = child.resolve(name);
                    createAndCheckIsDirectory(child, attrs);
                }
                return dir;
            } else if (se == null) {
                throw new FileSystemException(dir.toString(), null, "Unable to determine if root directory exists");
            } else {
                throw se;
            }
        }
    }

    private static void createAndCheckIsDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        try {
            createDirectory(dir, attrs);
        } catch (FileAlreadyExistsException x) {
            if (!isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) {
                throw x;
            }
        }
    }

    public static Path createTempFile(Path dir, String prefix, String suffix, FileAttribute<?>... attrs) throws IOException {
        return TempFileHelper.createTempFile((Path) Objects.requireNonNull(dir), prefix, suffix, attrs);
    }

    public static Path createTempFile(String prefix, String suffix, FileAttribute<?>... attrs) throws IOException {
        return TempFileHelper.createTempFile(null, prefix, suffix, attrs);
    }

    public static Path createTempDirectory(Path dir, String prefix, FileAttribute<?>... attrs) throws IOException {
        return TempFileHelper.createTempDirectory((Path) Objects.requireNonNull(dir), prefix, attrs);
    }

    public static Path createTempDirectory(String prefix, FileAttribute<?>... attrs) throws IOException {
        return TempFileHelper.createTempDirectory(null, prefix, attrs);
    }

    public static Path createSymbolicLink(Path link, Path target, FileAttribute<?>... attrs) throws IOException {
        provider(link).createSymbolicLink(link, target, attrs);
        return link;
    }

    public static Path createLink(Path link, Path existing) throws IOException {
        provider(link).createLink(link, existing);
        return link;
    }

    public static void delete(Path path) throws IOException {
        provider(path).delete(path);
    }

    public static boolean deleteIfExists(Path path) throws IOException {
        return provider(path).deleteIfExists(path);
    }

    public static Path copy(Path source, Path target, CopyOption... options) throws IOException {
        FileSystemProvider provider = provider(source);
        if (provider(target) == provider) {
            provider.copy(source, target, options);
        } else {
            CopyMoveHelper.copyToForeignTarget(source, target, options);
        }
        return target;
    }

    public static Path move(Path source, Path target, CopyOption... options) throws IOException {
        FileSystemProvider provider = provider(source);
        if (provider(target) == provider) {
            provider.move(source, target, options);
        } else {
            CopyMoveHelper.moveToForeignTarget(source, target, options);
        }
        return target;
    }

    public static Path readSymbolicLink(Path link) throws IOException {
        return provider(link).readSymbolicLink(link);
    }

    public static FileStore getFileStore(Path path) throws IOException {
        return provider(path).getFileStore(path);
    }

    public static boolean isSameFile(Path path, Path path2) throws IOException {
        return provider(path).isSameFile(path, path2);
    }

    public static boolean isHidden(Path path) throws IOException {
        return provider(path).isHidden(path);
    }

    public static String probeContentType(Path path) throws IOException {
        for (FileTypeDetector detector : FileTypeDetectors.installeDetectors) {
            String result = detector.probeContentType(path);
            if (result != null) {
                return result;
            }
        }
        return FileTypeDetectors.defaultFileTypeDetector.probeContentType(path);
    }

    public static <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        return provider(path).getFileAttributeView(path, type, options);
    }

    public static <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        return provider(path).readAttributes(path, (Class) type, options);
    }

    public static Path setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        provider(path).setAttribute(path, attribute, value, options);
        return path;
    }

    public static Object getAttribute(Path path, String attribute, LinkOption... options) throws IOException {
        if (attribute.indexOf(42) >= 0 || attribute.indexOf(44) >= 0) {
            throw new IllegalArgumentException(attribute);
        }
        Map<String, Object> map = readAttributes(path, attribute, options);
        if (-assertionsDisabled || map.size() == 1) {
            int pos = attribute.indexOf(58);
            String name = pos == -1 ? attribute : pos == attribute.length() ? "" : attribute.substring(pos + 1);
            return map.get(name);
        }
        throw new AssertionError();
    }

    public static Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        return provider(path).readAttributes(path, attributes, options);
    }

    public static Set<PosixFilePermission> getPosixFilePermissions(Path path, LinkOption... options) throws IOException {
        return ((PosixFileAttributes) readAttributes(path, PosixFileAttributes.class, options)).permissions();
    }

    public static Path setPosixFilePermissions(Path path, Set<PosixFilePermission> perms) throws IOException {
        PosixFileAttributeView view = (PosixFileAttributeView) getFileAttributeView(path, PosixFileAttributeView.class, new LinkOption[0]);
        if (view == null) {
            throw new UnsupportedOperationException();
        }
        view.setPermissions(perms);
        return path;
    }

    public static UserPrincipal getOwner(Path path, LinkOption... options) throws IOException {
        FileOwnerAttributeView view = (FileOwnerAttributeView) getFileAttributeView(path, FileOwnerAttributeView.class, options);
        if (view != null) {
            return view.getOwner();
        }
        throw new UnsupportedOperationException();
    }

    public static Path setOwner(Path path, UserPrincipal owner) throws IOException {
        FileOwnerAttributeView view = (FileOwnerAttributeView) getFileAttributeView(path, FileOwnerAttributeView.class, new LinkOption[0]);
        if (view == null) {
            throw new UnsupportedOperationException();
        }
        view.setOwner(owner);
        return path;
    }

    public static boolean isSymbolicLink(Path path) {
        try {
            return readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).isSymbolicLink();
        } catch (IOException e) {
            return -assertionsDisabled;
        }
    }

    public static boolean isDirectory(Path path, LinkOption... options) {
        try {
            return readAttributes(path, BasicFileAttributes.class, options).isDirectory();
        } catch (IOException e) {
            return -assertionsDisabled;
        }
    }

    public static boolean isRegularFile(Path path, LinkOption... options) {
        try {
            return readAttributes(path, BasicFileAttributes.class, options).isRegularFile();
        } catch (IOException e) {
            return -assertionsDisabled;
        }
    }

    public static FileTime getLastModifiedTime(Path path, LinkOption... options) throws IOException {
        return readAttributes(path, BasicFileAttributes.class, options).lastModifiedTime();
    }

    public static Path setLastModifiedTime(Path path, FileTime time) throws IOException {
        ((BasicFileAttributeView) getFileAttributeView(path, BasicFileAttributeView.class, new LinkOption[0])).setTimes(time, null, null);
        return path;
    }

    public static long size(Path path) throws IOException {
        return readAttributes(path, BasicFileAttributes.class, new LinkOption[0]).size();
    }

    private static boolean followLinks(LinkOption... options) {
        boolean followLinks = true;
        int i = 0;
        int length = options.length;
        while (i < length) {
            LinkOption opt = options[i];
            if (opt == LinkOption.NOFOLLOW_LINKS) {
                followLinks = -assertionsDisabled;
                i++;
            } else if (opt == null) {
                throw new NullPointerException();
            } else {
                throw new AssertionError((Object) "Should not get here");
            }
        }
        return followLinks;
    }

    public static boolean exists(Path path, LinkOption... options) {
        try {
            if (followLinks(options)) {
                provider(path).checkAccess(path, new AccessMode[0]);
            } else {
                readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            }
            return true;
        } catch (IOException e) {
            return -assertionsDisabled;
        }
    }

    public static boolean notExists(Path path, LinkOption... options) {
        try {
            if (followLinks(options)) {
                provider(path).checkAccess(path, new AccessMode[0]);
            } else {
                readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            }
            return -assertionsDisabled;
        } catch (NoSuchFileException e) {
            return true;
        } catch (IOException e2) {
            return -assertionsDisabled;
        }
    }

    private static boolean isAccessible(Path path, AccessMode... modes) {
        try {
            provider(path).checkAccess(path, modes);
            return true;
        } catch (IOException e) {
            return -assertionsDisabled;
        }
    }

    public static boolean isReadable(Path path) {
        return isAccessible(path, AccessMode.READ);
    }

    public static boolean isWritable(Path path) {
        return isAccessible(path, AccessMode.WRITE);
    }

    public static boolean isExecutable(Path path) {
        return isAccessible(path, AccessMode.EXECUTE);
    }

    public static Path walkFileTree(Path start, Set<FileVisitOption> options, int maxDepth, FileVisitor<? super Path> visitor) throws IOException {
        Throwable th;
        Throwable th2 = null;
        FileTreeWalker fileTreeWalker = null;
        try {
            FileTreeWalker walker = new FileTreeWalker(options, maxDepth);
            try {
                Event ev = walker.walk(start);
                while (true) {
                    FileVisitResult result;
                    switch (-getjava-nio-file-FileTreeWalker$EventTypeSwitchesValues()[ev.type().ordinal()]) {
                        case 1:
                            result = visitor.postVisitDirectory(ev.file(), ev.ioeException());
                            if (result == FileVisitResult.SKIP_SIBLINGS) {
                                result = FileVisitResult.CONTINUE;
                                break;
                            }
                            break;
                        case 2:
                            IOException ioe = ev.ioeException();
                            if (ioe != null) {
                                result = visitor.visitFileFailed(ev.file(), ioe);
                                break;
                            } else if (-assertionsDisabled || ev.attributes() != null) {
                                result = visitor.visitFile(ev.file(), ev.attributes());
                                break;
                            } else {
                                throw new AssertionError();
                            }
                            break;
                        case 3:
                            result = visitor.preVisitDirectory(ev.file(), ev.attributes());
                            if (result == FileVisitResult.SKIP_SUBTREE || result == FileVisitResult.SKIP_SIBLINGS) {
                                walker.pop();
                                break;
                            }
                        default:
                            throw new AssertionError((Object) "Should not get here");
                    }
                    if (Objects.requireNonNull(result) != FileVisitResult.CONTINUE) {
                        if (result == FileVisitResult.TERMINATE) {
                            if (walker != null) {
                                try {
                                    walker.close();
                                } catch (Throwable th3) {
                                    th2 = th3;
                                }
                            }
                            if (th2 != null) {
                                return start;
                            }
                            throw th2;
                        } else if (result == FileVisitResult.SKIP_SIBLINGS) {
                            walker.skipRemainingSiblings();
                        }
                    }
                    ev = walker.next();
                    if (ev == null) {
                        if (walker != null) {
                            walker.close();
                        }
                        if (th2 != null) {
                            return start;
                        }
                        throw th2;
                    }
                }
            } catch (Throwable th4) {
                th = th4;
                fileTreeWalker = walker;
            }
        } catch (Throwable th5) {
            th = th5;
            if (fileTreeWalker != null) {
                try {
                    fileTreeWalker.close();
                } catch (Throwable th6) {
                    if (th2 == null) {
                        th2 = th6;
                    } else if (th2 != th6) {
                        th2.addSuppressed(th6);
                    }
                }
            }
            if (th2 != null) {
                throw th2;
            }
            throw th;
        }
    }

    public static Path walkFileTree(Path start, FileVisitor<? super Path> visitor) throws IOException {
        return walkFileTree(start, EnumSet.noneOf(FileVisitOption.class), Integer.MAX_VALUE, visitor);
    }

    public static BufferedReader newBufferedReader(Path path, Charset cs) throws IOException {
        return new BufferedReader(new InputStreamReader(newInputStream(path, new OpenOption[0]), cs.newDecoder()));
    }

    public static BufferedReader newBufferedReader(Path path) throws IOException {
        return newBufferedReader(path, StandardCharsets.UTF_8);
    }

    public static BufferedWriter newBufferedWriter(Path path, Charset cs, OpenOption... options) throws IOException {
        return new BufferedWriter(new OutputStreamWriter(newOutputStream(path, options), cs.newEncoder()));
    }

    public static BufferedWriter newBufferedWriter(Path path, OpenOption... options) throws IOException {
        return newBufferedWriter(path, StandardCharsets.UTF_8, options);
    }

    private static long copy(InputStream source, OutputStream sink) throws IOException {
        long nread = 0;
        byte[] buf = new byte[8192];
        while (true) {
            int n = source.read(buf);
            if (n <= 0) {
                return nread;
            }
            sink.write(buf, 0, n);
            nread += (long) n;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static long copy(InputStream in, Path target, CopyOption... options) throws IOException {
        int i = 0;
        Throwable th = null;
        Objects.requireNonNull(in);
        boolean replaceExisting = -assertionsDisabled;
        int length = options.length;
        while (i < length) {
            Object opt = options[i];
            if (opt == StandardCopyOption.REPLACE_EXISTING) {
                replaceExisting = true;
                i++;
            } else if (opt == null) {
                throw new NullPointerException("options contains 'null'");
            } else {
                throw new UnsupportedOperationException(opt + " not supported");
            }
        }
        SecurityException se = null;
        if (replaceExisting) {
            try {
                deleteIfExists(target);
            } catch (SecurityException x) {
                se = x;
            }
        }
        try {
            OutputStream ostream = newOutputStream(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            OutputStream out = ostream;
            long copy = copy(in, ostream);
            if (ostream != null) {
                try {
                    ostream.close();
                } catch (Throwable th2) {
                    th = th2;
                }
            }
            if (th == null) {
                return copy;
            }
            throw th;
        } catch (FileAlreadyExistsException x2) {
            if (se != null) {
                throw se;
            }
            throw x2;
        }
    }

    public static long copy(Path source, OutputStream out) throws IOException {
        Throwable th;
        Throwable th2 = null;
        Objects.requireNonNull(out);
        InputStream inputStream = null;
        try {
            inputStream = newInputStream(source, new OpenOption[0]);
            long copy = copy(inputStream, out);
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Throwable th3) {
                    th2 = th3;
                }
            }
            if (th2 == null) {
                return copy;
            }
            throw th2;
        } catch (Throwable th22) {
            Throwable th4 = th22;
            th22 = th;
            th = th4;
        }
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Throwable th5) {
                if (th22 == null) {
                    th22 = th5;
                } else if (th22 != th5) {
                    th22.addSuppressed(th5);
                }
            }
        }
        if (th22 != null) {
            throw th22;
        }
        throw th;
    }

    private static byte[] read(InputStream source, int initialSize) throws IOException {
        int capacity = initialSize;
        byte[] buf = new byte[initialSize];
        int nread = 0;
        while (true) {
            int n = source.read(buf, nread, capacity - nread);
            if (n <= 0) {
                if (n < 0) {
                    break;
                }
                n = source.read();
                if (n < 0) {
                    break;
                }
                if (capacity <= MAX_BUFFER_SIZE - capacity) {
                    capacity = Math.max(capacity << 1, 8192);
                } else if (capacity == MAX_BUFFER_SIZE) {
                    throw new OutOfMemoryError("Required array size too large");
                } else {
                    capacity = MAX_BUFFER_SIZE;
                }
                buf = Arrays.copyOf(buf, capacity);
                int nread2 = nread + 1;
                buf[nread] = (byte) n;
                nread = nread2;
            } else {
                nread += n;
            }
        }
        return capacity == nread ? buf : Arrays.copyOf(buf, nread);
    }

    public static byte[] readAllBytes(Path path) throws IOException {
        Throwable th;
        Throwable th2;
        Throwable th3 = null;
        SeekableByteChannel seekableByteChannel = null;
        InputStream inputStream = null;
        try {
            seekableByteChannel = newByteChannel(path, new OpenOption[0]);
            inputStream = Channels.newInputStream((ReadableByteChannel) seekableByteChannel);
            long size = seekableByteChannel.size();
            if (size > 2147483639) {
                throw new OutOfMemoryError("Required array size too large");
            }
            byte[] read = read(inputStream, (int) size);
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Throwable th4) {
                    th3 = th4;
                }
            }
            if (seekableByteChannel != null) {
                try {
                    seekableByteChannel.close();
                } catch (Throwable th5) {
                    th = th5;
                    if (th3 != null) {
                        if (th3 != th) {
                            th3.addSuppressed(th);
                            th = th3;
                        }
                    }
                }
            }
            th = th3;
            if (th == null) {
                return read;
            }
            throw th;
        } catch (Throwable th32) {
            Throwable th6 = th32;
            th32 = th;
            th = th6;
        }
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (Throwable th7) {
                th2 = th7;
                if (th32 != null) {
                    if (th32 != th2) {
                        th32.addSuppressed(th2);
                        th2 = th32;
                    }
                }
            }
        }
        th2 = th32;
        if (seekableByteChannel != null) {
            try {
                seekableByteChannel.close();
            } catch (Throwable th8) {
                th32 = th8;
                if (th2 != null) {
                    if (th2 != th32) {
                        th2.addSuppressed(th32);
                        th32 = th2;
                    }
                }
            }
        }
        th32 = th2;
        if (th32 != null) {
            throw th32;
        }
        throw th;
    }

    public static List<String> readAllLines(Path path, Charset cs) throws IOException {
        Throwable th;
        Throwable th2 = null;
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = newBufferedReader(path, cs);
            List<String> result = new ArrayList();
            while (true) {
                String line = bufferedReader.readLine();
                if (line == null) {
                    break;
                }
                result.add(line);
            }
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (Throwable th3) {
                    th2 = th3;
                }
            }
            if (th2 == null) {
                return result;
            }
            throw th2;
        } catch (Throwable th22) {
            Throwable th4 = th22;
            th22 = th;
            th = th4;
        }
        if (bufferedReader != null) {
            try {
                bufferedReader.close();
            } catch (Throwable th5) {
                if (th22 == null) {
                    th22 = th5;
                } else if (th22 != th5) {
                    th22.addSuppressed(th5);
                }
            }
        }
        if (th22 != null) {
            throw th22;
        }
        throw th;
    }

    public static List<String> readAllLines(Path path) throws IOException {
        return readAllLines(path, StandardCharsets.UTF_8);
    }

    public static Path write(Path path, byte[] bytes, OpenOption... options) throws IOException {
        Throwable th;
        Throwable th2 = null;
        Objects.requireNonNull(bytes);
        OutputStream outputStream = null;
        try {
            outputStream = newOutputStream(path, options);
            int len = bytes.length;
            int rem = len;
            while (rem > 0) {
                int n = Math.min(rem, 8192);
                outputStream.write(bytes, len - rem, n);
                rem -= n;
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Throwable th3) {
                    th2 = th3;
                }
            }
            if (th2 == null) {
                return path;
            }
            throw th2;
        } catch (Throwable th22) {
            Throwable th4 = th22;
            th22 = th;
            th = th4;
        }
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (Throwable th5) {
                if (th22 == null) {
                    th22 = th5;
                } else if (th22 != th5) {
                    th22.addSuppressed(th5);
                }
            }
        }
        if (th22 != null) {
            throw th22;
        }
        throw th;
    }

    public static Path write(Path path, Iterable<? extends CharSequence> lines, Charset cs, OpenOption... options) throws IOException {
        Throwable th;
        Throwable th2 = null;
        Objects.requireNonNull(lines);
        BufferedWriter bufferedWriter = null;
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(newOutputStream(path, options), cs.newEncoder()));
            try {
                for (CharSequence line : lines) {
                    writer.append(line);
                    writer.newLine();
                }
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (Throwable th3) {
                        th2 = th3;
                    }
                }
                if (th2 == null) {
                    return path;
                }
                throw th2;
            } catch (Throwable th4) {
                th = th4;
                bufferedWriter = writer;
                if (bufferedWriter != null) {
                    try {
                        bufferedWriter.close();
                    } catch (Throwable th5) {
                        if (th2 == null) {
                            th2 = th5;
                        } else if (th2 != th5) {
                            th2.addSuppressed(th5);
                        }
                    }
                }
                if (th2 == null) {
                    throw th;
                }
                throw th2;
            }
        } catch (Throwable th6) {
            th = th6;
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
            if (th2 == null) {
                throw th2;
            }
            throw th;
        }
    }

    public static Path write(Path path, Iterable<? extends CharSequence> lines, OpenOption... options) throws IOException {
        return write(path, lines, StandardCharsets.UTF_8, options);
    }

    public static Stream<Path> list(Path dir) throws IOException {
        DirectoryStream<Path> ds = newDirectoryStream(dir);
        try {
            final Iterator<Path> delegate = ds.iterator();
            return (Stream) StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<Path>() {
                public boolean hasNext() {
                    try {
                        return delegate.hasNext();
                    } catch (DirectoryIteratorException e) {
                        throw new UncheckedIOException(e.getCause());
                    }
                }

                public Path next() {
                    try {
                        return (Path) delegate.next();
                    } catch (DirectoryIteratorException e) {
                        throw new UncheckedIOException(e.getCause());
                    }
                }
            }, 1), -assertionsDisabled).onClose(asUncheckedRunnable(ds));
        } catch (Throwable e) {
            try {
                ds.close();
            } catch (IOException ex) {
                e.addSuppressed(ex);
            } catch (Throwable th) {
            }
            throw e;
        }
    }

    public static Stream<Path> walk(Path start, int maxDepth, FileVisitOption... options) throws IOException {
        Iterator iterator = new FileTreeIterator(start, maxDepth, options);
        try {
            Stream stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 1), -assertionsDisabled);
            iterator.getClass();
            return ((Stream) stream.onClose(new java.nio.file.-$Lambda$iV0HzPWaaR68t7NV87hCwF49CFs.AnonymousClass1((byte) 2, iterator))).map(-$Lambda$iV0HzPWaaR68t7NV87hCwF49CFs.$INST$1);
        } catch (Throwable e) {
            iterator.close();
            throw e;
        }
    }

    public static Stream<Path> walk(Path start, FileVisitOption... options) throws IOException {
        return walk(start, Integer.MAX_VALUE, options);
    }

    public static Stream<Path> find(Path start, int maxDepth, BiPredicate<Path, BasicFileAttributes> matcher, FileVisitOption... options) throws IOException {
        Iterator iterator = new FileTreeIterator(start, maxDepth, options);
        try {
            Stream stream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 1), -assertionsDisabled);
            iterator.getClass();
            return ((Stream) stream.onClose(new java.nio.file.-$Lambda$iV0HzPWaaR68t7NV87hCwF49CFs.AnonymousClass1((byte) 1, iterator))).filter(new java.nio.file.-$Lambda$iV0HzPWaaR68t7NV87hCwF49CFs.AnonymousClass2(matcher)).map(-$Lambda$iV0HzPWaaR68t7NV87hCwF49CFs.$INST$0);
        } catch (Throwable e) {
            iterator.close();
            throw e;
        }
    }

    public static Stream<String> lines(Path path, Charset cs) throws IOException {
        BufferedReader br = newBufferedReader(path, cs);
        try {
            return (Stream) br.lines().onClose(asUncheckedRunnable(br));
        } catch (Throwable e) {
            try {
                br.close();
            } catch (IOException ex) {
                e.addSuppressed(ex);
            } catch (Throwable th) {
            }
            throw e;
        }
    }

    public static Stream<String> lines(Path path) throws IOException {
        return lines(path, StandardCharsets.UTF_8);
    }
}
