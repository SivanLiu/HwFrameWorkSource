package com.android.timezone.distro;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

public final class FileUtils {
    private FileUtils() {
    }

    public static File createSubFile(File parentDir, String name) throws IOException {
        File subFile = new File(parentDir, name).getCanonicalFile();
        if (subFile.getPath().startsWith(parentDir.getCanonicalPath())) {
            return subFile;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(name);
        stringBuilder.append(" must exist beneath ");
        stringBuilder.append(parentDir);
        stringBuilder.append(". Canonicalized subpath: ");
        stringBuilder.append(subFile);
        throw new IOException(stringBuilder.toString());
    }

    public static void ensureDirectoriesExist(File dir, boolean makeWorldReadable) throws IOException {
        LinkedList<File> dirs = new LinkedList();
        File currentDir = dir;
        do {
            dirs.addFirst(currentDir);
            currentDir = currentDir.getParentFile();
        } while (currentDir != null);
        Iterator it = dirs.iterator();
        while (it.hasNext()) {
            File dirToCheck = (File) it.next();
            StringBuilder stringBuilder;
            if (dirToCheck.exists()) {
                if (!dirToCheck.isDirectory()) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(dirToCheck);
                    stringBuilder.append(" exists but is not a directory");
                    throw new IOException(stringBuilder.toString());
                }
            } else if (!dirToCheck.mkdir()) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to create directory: ");
                stringBuilder.append(dir);
                throw new IOException(stringBuilder.toString());
            } else if (makeWorldReadable) {
                makeDirectoryWorldAccessible(dirToCheck);
            }
        }
    }

    public static void makeDirectoryWorldAccessible(File directory) throws IOException {
        StringBuilder stringBuilder;
        if (directory.isDirectory()) {
            makeWorldReadable(directory);
            if (!directory.setExecutable(true, false)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to make ");
                stringBuilder.append(directory);
                stringBuilder.append(" world-executable");
                throw new IOException(stringBuilder.toString());
            }
            return;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(directory);
        stringBuilder.append(" must be a directory");
        throw new IOException(stringBuilder.toString());
    }

    public static void makeWorldReadable(File file) throws IOException {
        if (!file.setReadable(true, false)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to make ");
            stringBuilder.append(file);
            stringBuilder.append(" world-readable");
            throw new IOException(stringBuilder.toString());
        }
    }

    public static void rename(File from, File to) throws IOException {
        ensureFileDoesNotExist(to);
        if (!from.renameTo(to)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to rename ");
            stringBuilder.append(from);
            stringBuilder.append(" to ");
            stringBuilder.append(to);
            throw new IOException(stringBuilder.toString());
        }
    }

    public static void ensureFileDoesNotExist(File file) throws IOException {
        if (!file.exists()) {
            return;
        }
        if (file.isFile()) {
            doDelete(file);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(file);
        stringBuilder.append(" is not a file");
        throw new IOException(stringBuilder.toString());
    }

    public static void doDelete(File file) throws IOException {
        if (!file.delete()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unable to delete: ");
            stringBuilder.append(file);
            throw new IOException(stringBuilder.toString());
        }
    }

    public static boolean isSymlink(File file) throws IOException {
        return file.getCanonicalPath().equals(new File(file.getParentFile().getCanonicalFile(), file.getName()).getPath()) ^ 1;
    }

    public static void deleteRecursive(File toDelete) throws IOException {
        if (toDelete.isDirectory()) {
            for (File file : toDelete.listFiles()) {
                if (!file.isDirectory() || isSymlink(file)) {
                    doDelete(file);
                } else {
                    deleteRecursive(file);
                }
            }
            String[] remainingFiles = toDelete.list();
            if (remainingFiles.length != 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to delete files: ");
                stringBuilder.append(Arrays.toString(remainingFiles));
                throw new IOException(stringBuilder.toString());
            }
        }
        doDelete(toDelete);
    }

    public static boolean filesExist(File rootDir, String... fileNames) {
        for (String fileName : fileNames) {
            if (!new File(rootDir, fileName).exists()) {
                return false;
            }
        }
        return true;
    }

    /* JADX WARNING: Missing block: B:10:0x001d, code:
            if (r1 != null) goto L_0x001f;
     */
    /* JADX WARNING: Missing block: B:12:?, code:
            r0.close();
     */
    /* JADX WARNING: Missing block: B:13:0x0023, code:
            r3 = move-exception;
     */
    /* JADX WARNING: Missing block: B:14:0x0024, code:
            r1.addSuppressed(r3);
     */
    /* JADX WARNING: Missing block: B:15:0x0028, code:
            r0.close();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static byte[] readBytes(File file, int maxBytes) throws IOException {
        if (maxBytes > 0) {
            FileInputStream in = new FileInputStream(file);
            byte[] max = new byte[maxBytes];
            int bytesRead = in.read(max, 0, maxBytes);
            byte[] toReturn = new byte[bytesRead];
            System.arraycopy(max, 0, toReturn, 0, bytesRead);
            in.close();
            return toReturn;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("maxBytes ==");
        stringBuilder.append(maxBytes);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public static void createEmptyFile(File file) throws IOException {
        new FileOutputStream(file, false).close();
    }
}
