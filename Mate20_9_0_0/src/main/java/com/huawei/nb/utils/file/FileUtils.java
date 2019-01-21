package com.huawei.nb.utils.file;

import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.text.TextUtils;
import com.huawei.nb.utils.logger.DSLog;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryFlag;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class FileUtils {
    private static final String PATH_WHITE_LIST = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890-=[];\\',./ ~!@#$%^&*()_+\"{}|:<>?";
    private static final Pattern pattern = Pattern.compile("(.*([/\\\\]{1}[\\.\\.]{1,2}|[\\.\\.]{1,2}[/\\\\]{1}|\\.\\.).*|\\.)");

    public static File getFile(String filePath) throws IOException {
        return getFile(filePath, null);
    }

    public static File getFile(String filePath, String fileName) throws IOException {
        if (TextUtils.isEmpty(filePath) || !isSafePath(filePath)) {
            throw new IOException("Invalid file path!");
        }
        String replacedPath = checkFile(filePath);
        if (TextUtils.isEmpty(fileName)) {
            return new File(replacedPath);
        }
        if (isSafePath(fileName)) {
            return new File(replacedPath, fileName);
        }
        throw new IOException("Invalid file path!");
    }

    private static boolean isSafePath(String filePath) {
        boolean isNotSafe = pattern.matcher(filePath).matches();
        if (isNotSafe) {
            DSLog.e("Invalid file path : " + filePath, new Object[0]);
        }
        if (isNotSafe) {
            return false;
        }
        return true;
    }

    private static String checkFile(String filePath) {
        if (filePath == null) {
            return null;
        }
        StringBuffer tmpStrBuf = new StringBuffer();
        int filePathLength = filePath.length();
        int whiteFilePathLength = PATH_WHITE_LIST.length();
        for (int i = 0; i < filePathLength; i++) {
            for (int j = 0; j < whiteFilePathLength; j++) {
                if (filePath.charAt(i) == PATH_WHITE_LIST.charAt(j)) {
                    tmpStrBuf.append(PATH_WHITE_LIST.charAt(j));
                    break;
                }
            }
        }
        return tmpStrBuf.toString();
    }

    public static FileAttribute<Set<PosixFilePermission>> getDefaultFileAttribute(File file, boolean isReadShare) {
        return getDefaultFileAttribute(file, isReadShare, false);
    }

    public static FileAttribute<Set<PosixFilePermission>> getDefaultFileAttribute(File file, boolean isReadShare, boolean isWriteShare) {
        Path path = file.toPath();
        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            return PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString((isReadShare ? "rw-r" : "rw--") + (isWriteShare ? "w----" : "-----")));
        }
        UserPrincipal user = null;
        try {
            user = path.getFileSystem().getUserPrincipalLookupService().lookupPrincipalByName(System.getProperty("user.name"));
        } catch (IOException e) {
            DSLog.e("FileUtils getDefaultFileAttribute IOException.", new Object[0]);
        }
        AclEntryPermission[] permList = new AclEntryPermission[]{AclEntryPermission.READ_DATA, AclEntryPermission.READ_ATTRIBUTES, AclEntryPermission.READ_NAMED_ATTRS, AclEntryPermission.READ_ACL, AclEntryPermission.WRITE_DATA, AclEntryPermission.DELETE, AclEntryPermission.APPEND_DATA, AclEntryPermission.WRITE_ATTRIBUTES, AclEntryPermission.WRITE_NAMED_ATTRS, AclEntryPermission.WRITE_ACL, AclEntryPermission.SYNCHRONIZE};
        Set<AclEntryPermission> perms = EnumSet.noneOf(AclEntryPermission.class);
        for (AclEntryPermission perm : permList) {
            perms.add(perm);
        }
        if (user == null) {
            return null;
        }
        final AclEntry entry = AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(user).setPermissions(perms).setFlags(new AclEntryFlag[]{AclEntryFlag.FILE_INHERIT, AclEntryFlag.DIRECTORY_INHERIT}).build();
        return new FileAttribute<List<AclEntry>>() {
            public String name() {
                return "acl:acl";
            }

            public List<AclEntry> value() {
                ArrayList<AclEntry> l = new ArrayList();
                l.add(entry);
                return l;
            }
        };
    }

    private static int getDirMode(boolean isGroupReadShare, boolean isGroupWriteShare) {
        int mode = (OsConstants.S_IRUSR | OsConstants.S_IWUSR) | OsConstants.S_IXUSR;
        if (isGroupReadShare) {
            mode |= OsConstants.S_IRGRP;
        }
        if (isGroupWriteShare) {
            mode |= OsConstants.S_IWGRP;
        }
        if (isGroupReadShare && isGroupWriteShare) {
            return mode | OsConstants.S_IXGRP;
        }
        return mode;
    }

    private static int getFileMode(boolean isGroupReadShare, boolean isGroupWriteShare) {
        int mode = OsConstants.S_IRUSR | OsConstants.S_IWUSR;
        if (isGroupReadShare) {
            mode |= OsConstants.S_IRGRP;
        }
        if (isGroupWriteShare) {
            return mode | OsConstants.S_IWGRP;
        }
        return mode;
    }

    public static boolean mkdir(File file, int uid, int gid, boolean isReadShare, boolean isWriteShare) {
        if (!file.mkdir()) {
            return false;
        }
        if (uid < 0 && gid < 0) {
            return false;
        }
        try {
            String path = file.getCanonicalPath();
            Os.chown(path, uid, gid);
            Os.chmod(path, getDirMode(isReadShare, isWriteShare));
            return true;
        } catch (ErrnoException | IOException e) {
            if (file.delete()) {
                return false;
            }
            DSLog.w("mkdir a dir with wrong uid/gid remains.", new Object[0]);
            return false;
        }
    }

    public static boolean mkdirs(File file, int uid, int gid, boolean isReadShare, boolean isWriteShare) {
        boolean z = true;
        if (file.exists()) {
            return false;
        }
        if (mkdir(file, uid, gid, isReadShare, isWriteShare)) {
            return true;
        }
        try {
            File canonFile = file.getCanonicalFile();
            File parent = canonFile.getParentFile();
            if (parent == null || !((parent.exists() || mkdirs(parent, uid, gid, isReadShare, isWriteShare)) && mkdir(canonFile, uid, gid, isReadShare, isWriteShare))) {
                z = false;
            }
            return z;
        } catch (IOException e) {
            return false;
        }
    }

    public static OutputStream openOutputStream(File file) throws IOException {
        return openOutputStream(file, -1, -1, false, false);
    }

    public static OutputStream openOutputStream(File file, int uid, int gid, boolean isReadShare, boolean isWriteShare) throws IOException {
        boolean alreadyExists = file.exists();
        try {
            String path = file.getCanonicalPath();
            File safeFile = getFile(path);
            Path safePath = safeFile.toPath();
            FileAttribute<Set<PosixFilePermission>> attribute = getDefaultFileAttribute(safeFile, isReadShare, isWriteShare);
            Files.newByteChannel(safePath, EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE), new FileAttribute[]{attribute}).close();
            OutputStream outputStream = Files.newOutputStream(safePath, new OpenOption[0]);
            if (!alreadyExists) {
                setFileAccessPermission(path, uid, gid, isReadShare, isWriteShare);
            }
            return outputStream;
        } catch (ErrnoException | IOException e) {
            closeCloseable(null);
            throw new IOException(e.getMessage());
        }
    }

    public static FileInputStream openInputStream(File file) throws IOException {
        return new FileInputStream(getFile(file.getCanonicalPath()));
    }

    public static void setFileAccessPermission(String path, int uid, int gid, boolean isReadShare, boolean isWriteShare) throws ErrnoException {
        if (uid >= 0 || gid >= 0) {
            Os.chown(path, uid, gid);
            Os.chmod(path, getFileMode(isReadShare, isWriteShare));
        }
    }

    public static File getOutputFile(String path, int uid, int gid, boolean isReadShare, boolean isWriteShare) throws IOException {
        File file = getFile(path);
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (!(parent == null || mkdirs(parent, uid, gid, isReadShare, isWriteShare) || parent.isDirectory())) {
                throw new IOException("Directory '" + parent + "' could not be created");
            }
        } else if (file.isDirectory()) {
            throw new IOException("File '" + file + "' exists but is a directory");
        } else if (!file.canWrite()) {
            throw new IOException("File '" + file + "' cannot be written to");
        }
        return file;
    }

    public static String canonicalize(String path) {
        try {
            return getFile(path).getCanonicalPath();
        } catch (IOException e) {
            return null;
        }
    }

    public static void closeCloseable(Closeable closable) {
        if (closable != null) {
            try {
                closable.close();
            } catch (IOException e) {
                DSLog.e("closeClosable failed: " + e.getMessage(), new Object[0]);
            }
        }
    }

    public static List<File> getAllFileInDir(String pathName) {
        List<File> array = new ArrayList();
        try {
            getAllFile(pathName, array);
        } catch (IOException e) {
            DSLog.w(" IOException happened while checkLogDataFiles.err:" + e.getMessage(), new Object[0]);
        }
        return array;
    }

    public static void getAllFile(String pathName, List<File> files) throws IOException {
        File dirFile = new File(pathName);
        if (files == null || !dirFile.exists()) {
            DSLog.e(" pathName is not exists or List null.", new Object[0]);
        } else if (dirFile.isDirectory()) {
            File[] fileList = dirFile.listFiles();
            if (fileList != null) {
                for (File file : fileList) {
                    files.add(file);
                    if (file.isDirectory()) {
                        getAllFile(file.getCanonicalPath(), files);
                    }
                }
            }
        } else {
            if (dirFile.isFile()) {
                files.add(dirFile);
            }
            DSLog.e(" pathName is not a dir nor file.", new Object[0]);
        }
    }

    public static boolean deleteDir(File dataFileDir) {
        if (dataFileDir.isDirectory()) {
            String[] children = dataFileDir.list();
            if (children != null && children.length > 0) {
                for (String file : children) {
                    if (!deleteDir(new File(dataFileDir, file))) {
                        return false;
                    }
                }
            }
        }
        return dataFileDir.delete();
    }
}
