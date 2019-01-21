package java.util.prefs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import sun.util.locale.BaseLocale;
import sun.util.logging.PlatformLogger;

public class FileSystemPreferences extends AbstractPreferences {
    private static final int EACCES = 13;
    private static final int EAGAIN = 11;
    private static final String[] EMPTY_STRING_ARRAY = new String[0];
    private static final int ERROR_CODE = 1;
    private static int INIT_SLEEP_TIME = 50;
    private static final int LOCK_HANDLE = 0;
    private static int MAX_ATTEMPTS = 5;
    private static final int USER_READ_WRITE = 384;
    private static final int USER_RWX = 448;
    private static final int USER_RWX_ALL_RX = 493;
    private static final int USER_RW_ALL_READ = 420;
    private static boolean isSystemRootModified = false;
    private static boolean isSystemRootWritable;
    private static boolean isUserRootModified = false;
    private static boolean isUserRootWritable;
    static File systemLockFile;
    static Preferences systemRoot;
    private static File systemRootDir;
    private static int systemRootLockHandle = 0;
    private static File systemRootModFile;
    private static long systemRootModTime;
    static File userLockFile;
    static Preferences userRoot = null;
    private static File userRootDir;
    private static int userRootLockHandle = 0;
    private static File userRootModFile;
    private static long userRootModTime;
    final List<Change> changeLog = new ArrayList();
    private final File dir;
    private final boolean isUserNode;
    private long lastSyncTime = 0;
    NodeCreate nodeCreate = null;
    private Map<String, String> prefsCache = null;
    private final File prefsFile;
    private final File tmpFile;

    private abstract class Change {
        abstract void replay();

        private Change() {
        }

        /* synthetic */ Change(FileSystemPreferences x0, AnonymousClass1 x1) {
            this();
        }
    }

    private class NodeCreate extends Change {
        private NodeCreate() {
            super(FileSystemPreferences.this, null);
        }

        /* synthetic */ NodeCreate(FileSystemPreferences x0, AnonymousClass1 x1) {
            this();
        }

        void replay() {
        }
    }

    private class Put extends Change {
        String key;
        String value;

        Put(String key, String value) {
            super(FileSystemPreferences.this, null);
            this.key = key;
            this.value = value;
        }

        void replay() {
            FileSystemPreferences.this.prefsCache.put(this.key, this.value);
        }
    }

    private class Remove extends Change {
        String key;

        Remove(String key) {
            super(FileSystemPreferences.this, null);
            this.key = key;
        }

        void replay() {
            FileSystemPreferences.this.prefsCache.remove(this.key);
        }
    }

    private static native int chmod(String str, int i);

    private static native int[] lockFile0(String str, int i, boolean z);

    private static native int unlockFile0(int i);

    private static PlatformLogger getLogger() {
        return PlatformLogger.getLogger("java.util.prefs");
    }

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                FileSystemPreferences.syncWorld();
            }
        });
    }

    static synchronized Preferences getUserRoot() {
        Preferences preferences;
        synchronized (FileSystemPreferences.class) {
            if (userRoot == null) {
                setupUserRoot();
                userRoot = new FileSystemPreferences(true);
            }
            preferences = userRoot;
        }
        return preferences;
    }

    private static void setupUserRoot() {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                FileSystemPreferences.userRootDir = new File(System.getProperty("java.util.prefs.userRoot", System.getProperty("user.home")), ".java/.userPrefs");
                if (!FileSystemPreferences.userRootDir.exists()) {
                    if (FileSystemPreferences.userRootDir.mkdirs()) {
                        try {
                            FileSystemPreferences.chmod(FileSystemPreferences.userRootDir.getCanonicalPath(), FileSystemPreferences.USER_RWX);
                        } catch (IOException e) {
                            FileSystemPreferences.getLogger().warning("Could not change permissions on userRoot directory. ");
                        }
                        FileSystemPreferences.getLogger().info("Created user preferences directory.");
                    } else {
                        FileSystemPreferences.getLogger().warning("Couldn't create user preferences directory. User preferences are unusable.");
                    }
                }
                FileSystemPreferences.isUserRootWritable = FileSystemPreferences.userRootDir.canWrite();
                String USER_NAME = System.getProperty("user.name");
                File access$000 = FileSystemPreferences.userRootDir;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(".user.lock.");
                stringBuilder.append(USER_NAME);
                FileSystemPreferences.userLockFile = new File(access$000, stringBuilder.toString());
                access$000 = FileSystemPreferences.userRootDir;
                stringBuilder = new StringBuilder();
                stringBuilder.append(".userRootModFile.");
                stringBuilder.append(USER_NAME);
                FileSystemPreferences.userRootModFile = new File(access$000, stringBuilder.toString());
                if (!FileSystemPreferences.userRootModFile.exists()) {
                    try {
                        FileSystemPreferences.userRootModFile.createNewFile();
                        int result = FileSystemPreferences.chmod(FileSystemPreferences.userRootModFile.getCanonicalPath(), FileSystemPreferences.USER_READ_WRITE);
                        if (result != 0) {
                            PlatformLogger access$200 = FileSystemPreferences.getLogger();
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Problem creating userRoot mod file. Chmod failed on ");
                            stringBuilder.append(FileSystemPreferences.userRootModFile.getCanonicalPath());
                            stringBuilder.append(" Unix error code ");
                            stringBuilder.append(result);
                            access$200.warning(stringBuilder.toString());
                        }
                    } catch (IOException e2) {
                        FileSystemPreferences.getLogger().warning(e2.toString());
                    }
                }
                FileSystemPreferences.userRootModTime = FileSystemPreferences.userRootModFile.lastModified();
                return null;
            }
        });
    }

    static synchronized Preferences getSystemRoot() {
        Preferences preferences;
        synchronized (FileSystemPreferences.class) {
            if (systemRoot == null) {
                setupSystemRoot();
                systemRoot = new FileSystemPreferences(false);
            }
            preferences = systemRoot;
        }
        return preferences;
    }

    private static void setupSystemRoot() {
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                FileSystemPreferences.systemRootDir = new File(System.getProperty("java.util.prefs.systemRoot", "/etc/.java"), ".systemPrefs");
                if (!FileSystemPreferences.systemRootDir.exists()) {
                    FileSystemPreferences.systemRootDir = new File(System.getProperty("java.home"), ".systemPrefs");
                    if (!FileSystemPreferences.systemRootDir.exists()) {
                        if (FileSystemPreferences.systemRootDir.mkdirs()) {
                            FileSystemPreferences.getLogger().info("Created system preferences directory in java.home.");
                            try {
                                FileSystemPreferences.chmod(FileSystemPreferences.systemRootDir.getCanonicalPath(), FileSystemPreferences.USER_RWX_ALL_RX);
                            } catch (IOException e) {
                            }
                        } else {
                            FileSystemPreferences.getLogger().warning("Could not create system preferences directory. System preferences are unusable.");
                        }
                    }
                }
                FileSystemPreferences.isSystemRootWritable = FileSystemPreferences.systemRootDir.canWrite();
                FileSystemPreferences.systemLockFile = new File(FileSystemPreferences.systemRootDir, ".system.lock");
                FileSystemPreferences.systemRootModFile = new File(FileSystemPreferences.systemRootDir, ".systemRootModFile");
                if (!FileSystemPreferences.systemRootModFile.exists() && FileSystemPreferences.isSystemRootWritable) {
                    try {
                        FileSystemPreferences.systemRootModFile.createNewFile();
                        int result = FileSystemPreferences.chmod(FileSystemPreferences.systemRootModFile.getCanonicalPath(), FileSystemPreferences.USER_RW_ALL_READ);
                        if (result != 0) {
                            PlatformLogger access$200 = FileSystemPreferences.getLogger();
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Chmod failed on ");
                            stringBuilder.append(FileSystemPreferences.systemRootModFile.getCanonicalPath());
                            stringBuilder.append(" Unix error code ");
                            stringBuilder.append(result);
                            access$200.warning(stringBuilder.toString());
                        }
                    } catch (IOException e2) {
                        FileSystemPreferences.getLogger().warning(e2.toString());
                    }
                }
                FileSystemPreferences.systemRootModTime = FileSystemPreferences.systemRootModFile.lastModified();
                return null;
            }
        });
    }

    private void replayChanges() {
        int n = this.changeLog.size();
        for (int i = 0; i < n; i++) {
            ((Change) this.changeLog.get(i)).replay();
        }
    }

    private static void syncWorld() {
        Preferences userRt;
        Preferences systemRt;
        PlatformLogger logger;
        StringBuilder stringBuilder;
        synchronized (FileSystemPreferences.class) {
            userRt = userRoot;
            systemRt = systemRoot;
        }
        if (userRt != null) {
            try {
                userRt.flush();
            } catch (BackingStoreException e) {
                logger = getLogger();
                stringBuilder = new StringBuilder();
                stringBuilder.append("Couldn't flush user prefs: ");
                stringBuilder.append(e);
                logger.warning(stringBuilder.toString());
            }
        }
        if (systemRt != null) {
            try {
                systemRt.flush();
            } catch (BackingStoreException e2) {
                logger = getLogger();
                stringBuilder = new StringBuilder();
                stringBuilder.append("Couldn't flush system prefs: ");
                stringBuilder.append(e2);
                logger.warning(stringBuilder.toString());
            }
        }
    }

    private FileSystemPreferences(boolean user) {
        super(null, "");
        this.isUserNode = user;
        this.dir = user ? userRootDir : systemRootDir;
        this.prefsFile = new File(this.dir, "prefs.xml");
        this.tmpFile = new File(this.dir, "prefs.tmp");
    }

    public FileSystemPreferences(String path, File lockFile, boolean isUserNode) {
        super(null, "");
        this.isUserNode = isUserNode;
        this.dir = new File(path);
        this.prefsFile = new File(this.dir, "prefs.xml");
        this.tmpFile = new File(this.dir, "prefs.tmp");
        this.newNode = this.dir.exists() ^ 1;
        if (this.newNode) {
            this.prefsCache = new TreeMap();
            this.nodeCreate = new NodeCreate(this, null);
            this.changeLog.add(this.nodeCreate);
        }
        File parentFile;
        StringBuilder stringBuilder;
        if (isUserNode) {
            userLockFile = lockFile;
            parentFile = lockFile.getParentFile();
            stringBuilder = new StringBuilder();
            stringBuilder.append(lockFile.getName());
            stringBuilder.append(".rootmod");
            userRootModFile = new File(parentFile, stringBuilder.toString());
            return;
        }
        systemLockFile = lockFile;
        parentFile = lockFile.getParentFile();
        stringBuilder = new StringBuilder();
        stringBuilder.append(lockFile.getName());
        stringBuilder.append(".rootmod");
        systemRootModFile = new File(parentFile, stringBuilder.toString());
    }

    private FileSystemPreferences(FileSystemPreferences parent, String name) {
        super(parent, name);
        this.isUserNode = parent.isUserNode;
        this.dir = new File(parent.dir, dirName(name));
        this.prefsFile = new File(this.dir, "prefs.xml");
        this.tmpFile = new File(this.dir, "prefs.tmp");
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            public Void run() {
                FileSystemPreferences.this.newNode = FileSystemPreferences.this.dir.exists() ^ 1;
                return null;
            }
        });
        if (this.newNode) {
            this.prefsCache = new TreeMap();
            this.nodeCreate = new NodeCreate(this, null);
            this.changeLog.add(this.nodeCreate);
        }
    }

    public boolean isUserNode() {
        return this.isUserNode;
    }

    protected void putSpi(String key, String value) {
        initCacheIfNecessary();
        this.changeLog.add(new Put(key, value));
        this.prefsCache.put(key, value);
    }

    protected String getSpi(String key) {
        initCacheIfNecessary();
        return (String) this.prefsCache.get(key);
    }

    protected void removeSpi(String key) {
        initCacheIfNecessary();
        this.changeLog.add(new Remove(key));
        this.prefsCache.remove(key);
    }

    private void initCacheIfNecessary() {
        if (this.prefsCache == null) {
            try {
                loadCache();
            } catch (Exception e) {
                this.prefsCache = new TreeMap();
            }
        }
    }

    private void loadCache() throws BackingStoreException {
        Map<String, String> m = new TreeMap();
        long newLastSyncTime = 0;
        FileInputStream fis;
        try {
            newLastSyncTime = this.prefsFile.lastModified();
            fis = new FileInputStream(this.prefsFile);
            XmlSupport.importMap(fis, m);
            $closeResource(null, fis);
        } catch (Exception e) {
            PlatformLogger logger;
            StringBuilder stringBuilder;
            if (e instanceof InvalidPreferencesFormatException) {
                logger = getLogger();
                stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid preferences format in ");
                stringBuilder.append(this.prefsFile.getPath());
                logger.warning(stringBuilder.toString());
                this.prefsFile.renameTo(new File(this.prefsFile.getParentFile(), "IncorrectFormatPrefs.xml"));
                m = new TreeMap();
            } else if (e instanceof FileNotFoundException) {
                logger = getLogger();
                stringBuilder = new StringBuilder();
                stringBuilder.append("Prefs file removed in background ");
                stringBuilder.append(this.prefsFile.getPath());
                logger.warning(stringBuilder.toString());
            } else {
                logger = getLogger();
                stringBuilder = new StringBuilder();
                stringBuilder.append("Exception while reading cache: ");
                stringBuilder.append(e.getMessage());
                logger.warning(stringBuilder.toString());
                throw new BackingStoreException(e);
            }
        } catch (Throwable th) {
            $closeResource(r4, fis);
        }
        this.prefsCache = m;
        this.lastSyncTime = newLastSyncTime;
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    private void writeBackCache() throws BackingStoreException {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                public Void run() throws BackingStoreException {
                    FileOutputStream fos;
                    try {
                        StringBuilder stringBuilder;
                        if (!FileSystemPreferences.this.dir.exists()) {
                            if (!FileSystemPreferences.this.dir.mkdirs()) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append(FileSystemPreferences.this.dir);
                                stringBuilder.append(" create failed.");
                                throw new BackingStoreException(stringBuilder.toString());
                            }
                        }
                        fos = new FileOutputStream(FileSystemPreferences.this.tmpFile);
                        XmlSupport.exportMap(fos, FileSystemPreferences.this.prefsCache);
                        fos.close();
                        if (FileSystemPreferences.this.tmpFile.renameTo(FileSystemPreferences.this.prefsFile)) {
                            return null;
                        }
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Can't rename ");
                        stringBuilder.append(FileSystemPreferences.this.tmpFile);
                        stringBuilder.append(" to ");
                        stringBuilder.append(FileSystemPreferences.this.prefsFile);
                        throw new BackingStoreException(stringBuilder.toString());
                    } catch (Exception e) {
                        if (e instanceof BackingStoreException) {
                            throw ((BackingStoreException) e);
                        }
                        throw new BackingStoreException(e);
                    } catch (Throwable th) {
                        r1.addSuppressed(th);
                    }
                }
            });
        } catch (PrivilegedActionException e) {
            throw ((BackingStoreException) e.getException());
        }
    }

    protected String[] keysSpi() {
        initCacheIfNecessary();
        return (String[]) this.prefsCache.keySet().toArray(new String[this.prefsCache.size()]);
    }

    protected String[] childrenNamesSpi() {
        return (String[]) AccessController.doPrivileged(new PrivilegedAction<String[]>() {
            public String[] run() {
                List<String> result = new ArrayList();
                File[] dirContents = FileSystemPreferences.this.dir.listFiles();
                if (dirContents != null) {
                    for (int i = 0; i < dirContents.length; i++) {
                        if (dirContents[i].isDirectory()) {
                            result.add(FileSystemPreferences.nodeName(dirContents[i].getName()));
                        }
                    }
                }
                return (String[]) result.toArray(FileSystemPreferences.EMPTY_STRING_ARRAY);
            }
        });
    }

    protected AbstractPreferences childSpi(String name) {
        return new FileSystemPreferences(this, name);
    }

    public void removeNode() throws BackingStoreException {
        synchronized ((isUserNode() ? userLockFile : systemLockFile)) {
            if (lockFile(false)) {
                try {
                    super.removeNode();
                } finally {
                    unlockFile();
                }
            } else {
                throw new BackingStoreException("Couldn't get file lock.");
            }
        }
    }

    protected void removeNodeSpi() throws BackingStoreException {
        try {
            AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {
                public Void run() throws BackingStoreException {
                    if (FileSystemPreferences.this.changeLog.contains(FileSystemPreferences.this.nodeCreate)) {
                        FileSystemPreferences.this.changeLog.remove(FileSystemPreferences.this.nodeCreate);
                        FileSystemPreferences.this.nodeCreate = null;
                        return null;
                    } else if (!FileSystemPreferences.this.dir.exists()) {
                        return null;
                    } else {
                        FileSystemPreferences.this.prefsFile.delete();
                        FileSystemPreferences.this.tmpFile.delete();
                        File[] junk = FileSystemPreferences.this.dir.listFiles();
                        if (junk.length != 0) {
                            PlatformLogger access$200 = FileSystemPreferences.getLogger();
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Found extraneous files when removing node: ");
                            stringBuilder.append(Arrays.asList(junk));
                            access$200.warning(stringBuilder.toString());
                            for (File delete : junk) {
                                delete.delete();
                            }
                        }
                        if (FileSystemPreferences.this.dir.delete()) {
                            return null;
                        }
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Couldn't delete dir: ");
                        stringBuilder2.append(FileSystemPreferences.this.dir);
                        throw new BackingStoreException(stringBuilder2.toString());
                    }
                }
            });
        } catch (PrivilegedActionException e) {
            throw ((BackingStoreException) e.getException());
        }
    }

    public synchronized void sync() throws BackingStoreException {
        boolean shared;
        if (isUserNode()) {
            shared = false;
        } else {
            shared = isSystemRootWritable ^ 1;
        }
        synchronized ((isUserNode() ? userLockFile : systemLockFile)) {
            if (lockFile(shared)) {
                final Long newModTime = (Long) AccessController.doPrivileged(new PrivilegedAction<Long>() {
                    public Long run() {
                        long nmt;
                        boolean z = false;
                        if (FileSystemPreferences.this.isUserNode()) {
                            nmt = FileSystemPreferences.userRootModFile.lastModified();
                            if (FileSystemPreferences.userRootModTime == nmt) {
                                z = true;
                            }
                            FileSystemPreferences.isUserRootModified = z;
                        } else {
                            nmt = FileSystemPreferences.systemRootModFile.lastModified();
                            if (FileSystemPreferences.systemRootModTime == nmt) {
                                z = true;
                            }
                            FileSystemPreferences.isSystemRootModified = z;
                        }
                        return new Long(nmt);
                    }
                });
                try {
                    super.sync();
                    AccessController.doPrivileged(new PrivilegedAction<Void>() {
                        public Void run() {
                            if (FileSystemPreferences.this.isUserNode()) {
                                FileSystemPreferences.userRootModTime = newModTime.longValue() + 1000;
                                FileSystemPreferences.userRootModFile.setLastModified(FileSystemPreferences.userRootModTime);
                            } else {
                                FileSystemPreferences.systemRootModTime = newModTime.longValue() + 1000;
                                FileSystemPreferences.systemRootModFile.setLastModified(FileSystemPreferences.systemRootModTime);
                            }
                            return null;
                        }
                    });
                } finally {
                    unlockFile();
                }
            } else {
                throw new BackingStoreException("Couldn't get file lock.");
            }
        }
    }

    protected void syncSpi() throws BackingStoreException {
        syncSpiPrivileged();
    }

    private void syncSpiPrivileged() throws BackingStoreException {
        if (isRemoved()) {
            throw new IllegalStateException("Node has been removed");
        } else if (this.prefsCache != null) {
            long lastModifiedTime;
            if (!isUserNode() ? !isSystemRootModified : !isUserRootModified) {
                lastModifiedTime = this.prefsFile.lastModified();
                if (lastModifiedTime != this.lastSyncTime) {
                    loadCache();
                    replayChanges();
                    this.lastSyncTime = lastModifiedTime;
                }
            } else if (!(this.lastSyncTime == 0 || this.dir.exists())) {
                this.prefsCache = new TreeMap();
                replayChanges();
            }
            if (!this.changeLog.isEmpty()) {
                writeBackCache();
                lastModifiedTime = this.prefsFile.lastModified();
                if (this.lastSyncTime <= lastModifiedTime) {
                    this.lastSyncTime = 1000 + lastModifiedTime;
                    this.prefsFile.setLastModified(this.lastSyncTime);
                }
                this.changeLog.clear();
            }
        }
    }

    public void flush() throws BackingStoreException {
        if (!isRemoved()) {
            sync();
        }
    }

    protected void flushSpi() throws BackingStoreException {
    }

    private static boolean isDirChar(char ch) {
        return (ch <= 31 || ch >= 127 || ch == '/' || ch == '.' || ch == '_') ? false : true;
    }

    private static String dirName(String nodeName) {
        int i = 0;
        int n = nodeName.length();
        while (i < n) {
            if (isDirChar(nodeName.charAt(i))) {
                i++;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(BaseLocale.SEP);
                stringBuilder.append(Base64.byteArrayToAltBase64(byteArray(nodeName)));
                return stringBuilder.toString();
            }
        }
        return nodeName;
    }

    private static byte[] byteArray(String s) {
        int len = s.length();
        byte[] result = new byte[(2 * len)];
        int j = 0;
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            int j2 = j + 1;
            result[j] = (byte) (c >> 8);
            j = j2 + 1;
            result[j2] = (byte) c;
        }
        return result;
    }

    private static String nodeName(String dirName) {
        int i = 0;
        if (dirName.charAt(0) != '_') {
            return dirName;
        }
        byte[] a = Base64.altBase64ToByteArray(dirName.substring(1));
        StringBuffer result = new StringBuffer(a.length / 2);
        while (i < a.length) {
            int i2 = i + 1;
            int i3 = i2 + 1;
            result.append((char) (((a[i] & 255) << 8) | (a[i2] & 255)));
            i = i3;
        }
        return result.toString();
    }

    private boolean lockFile(boolean shared) throws SecurityException {
        boolean usernode = isUserNode();
        File lockFile = usernode ? userLockFile : systemLockFile;
        long sleepTime = (long) INIT_SLEEP_TIME;
        int errorCode = 0;
        int i = 0;
        while (i < MAX_ATTEMPTS) {
            try {
                int[] result = lockFile0(lockFile.getCanonicalPath(), usernode ? USER_READ_WRITE : USER_RW_ALL_READ, shared);
                errorCode = result[1];
                if (result[0] != 0) {
                    if (usernode) {
                        userRootLockHandle = result[0];
                    } else {
                        systemRootLockHandle = result[0];
                    }
                    return true;
                }
            } catch (IOException e) {
            }
            try {
                Thread.sleep(sleepTime);
                sleepTime *= 2;
                i++;
            } catch (InterruptedException e2) {
                checkLockFile0ErrorCode(errorCode);
                return false;
            }
        }
        checkLockFile0ErrorCode(errorCode);
        return false;
    }

    private void checkLockFile0ErrorCode(int errorCode) throws SecurityException {
        StringBuilder stringBuilder;
        if (errorCode == 13) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Could not lock ");
            stringBuilder.append(isUserNode() ? "User prefs." : "System prefs.");
            stringBuilder.append(" Lock file access denied.");
            throw new SecurityException(stringBuilder.toString());
        } else if (errorCode != 11) {
            PlatformLogger logger = getLogger();
            stringBuilder = new StringBuilder();
            stringBuilder.append("Could not lock ");
            stringBuilder.append(isUserNode() ? "User prefs. " : "System prefs.");
            stringBuilder.append(" Unix error code ");
            stringBuilder.append(errorCode);
            stringBuilder.append(".");
            logger.warning(stringBuilder.toString());
        }
    }

    private void unlockFile() {
        boolean usernode = isUserNode();
        File file;
        if (usernode) {
            file = userLockFile;
        } else {
            file = systemLockFile;
        }
        int lockHandle = usernode ? userRootLockHandle : systemRootLockHandle;
        if (lockHandle == 0) {
            PlatformLogger logger = getLogger();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unlock: zero lockHandle for ");
            stringBuilder.append(usernode ? "user" : "system");
            stringBuilder.append(" preferences.)");
            logger.warning(stringBuilder.toString());
            return;
        }
        int result = unlockFile0(lockHandle);
        if (result != 0) {
            PlatformLogger logger2 = getLogger();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Could not drop file-lock on ");
            stringBuilder2.append(isUserNode() ? "user" : "system");
            stringBuilder2.append(" preferences. Unix error code ");
            stringBuilder2.append(result);
            stringBuilder2.append(".");
            logger2.warning(stringBuilder2.toString());
            if (result == 13) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Could not unlock");
                stringBuilder2.append(isUserNode() ? "User prefs." : "System prefs.");
                stringBuilder2.append(" Lock file access denied.");
                throw new SecurityException(stringBuilder2.toString());
            }
        }
        if (isUserNode()) {
            userRootLockHandle = 0;
        } else {
            systemRootLockHandle = 0;
        }
    }
}
