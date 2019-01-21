package com.android.timezone.distro;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class TimeZoneDistro {
    private static final int BUFFER_SIZE = 8192;
    public static final String DISTRO_VERSION_FILE_NAME = "distro_version";
    public static final String FILE_NAME = "distro.zip";
    public static final String ICU_DATA_FILE_NAME = "icu/icu_tzdata.dat";
    private static final long MAX_GET_ENTRY_CONTENTS_SIZE = 131072;
    public static final String TZDATA_FILE_NAME = "tzdata";
    public static final String TZLOOKUP_FILE_NAME = "tzlookup.xml";
    private final InputStream inputStream;

    public TimeZoneDistro(byte[] bytes) {
        this(new ByteArrayInputStream(bytes));
    }

    public TimeZoneDistro(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public DistroVersion getDistroVersion() throws DistroException, IOException {
        byte[] contents = getEntryContents(this.inputStream, DISTRO_VERSION_FILE_NAME);
        if (contents != null) {
            return DistroVersion.fromBytes(contents);
        }
        throw new DistroException("Distro version file entry not found");
    }

    /* JADX WARNING: Missing block: B:9:0x0021, code skipped:
            if (r3.getSize() > MAX_GET_ENTRY_CONTENTS_SIZE) goto L_0x0051;
     */
    /* JADX WARNING: Missing block: B:10:0x0023, code skipped:
            r4 = new byte[8192];
            r5 = new java.io.ByteArrayOutputStream();
     */
    /* JADX WARNING: Missing block: B:12:?, code skipped:
            r6 = r0.read(r4);
            r7 = r6;
     */
    /* JADX WARNING: Missing block: B:13:0x0032, code skipped:
            if (r6 == -1) goto L_0x0039;
     */
    /* JADX WARNING: Missing block: B:14:0x0034, code skipped:
            r5.write(r4, 0, r7);
     */
    /* JADX WARNING: Missing block: B:15:0x0039, code skipped:
            r6 = r5.toByteArray();
     */
    /* JADX WARNING: Missing block: B:17:?, code skipped:
            $closeResource(null, r5);
     */
    /* JADX WARNING: Missing block: B:18:0x0040, code skipped:
            $closeResource(null, r0);
     */
    /* JADX WARNING: Missing block: B:19:0x0043, code skipped:
            return r6;
     */
    /* JADX WARNING: Missing block: B:20:0x0044, code skipped:
            r6 = th;
     */
    /* JADX WARNING: Missing block: B:21:0x0045, code skipped:
            r7 = null;
     */
    /* JADX WARNING: Missing block: B:25:0x0049, code skipped:
            r7 = move-exception;
     */
    /* JADX WARNING: Missing block: B:26:0x004a, code skipped:
            r9 = r7;
            r7 = r6;
            r6 = r9;
     */
    /* JADX WARNING: Missing block: B:30:0x0051, code skipped:
            r5 = new java.lang.StringBuilder();
            r5.append("Entry ");
            r5.append(r11);
            r5.append(" too large: ");
            r5.append(r3.getSize());
     */
    /* JADX WARNING: Missing block: B:31:0x0073, code skipped:
            throw new java.io.IOException(r5.toString());
     */
    /* JADX WARNING: Missing block: B:38:0x007d, code skipped:
            $closeResource(r1, r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static byte[] getEntryContents(InputStream is, String entryName) throws IOException {
        ZipInputStream zipInputStream = new ZipInputStream(is);
        while (true) {
            ZipEntry nextEntry = zipInputStream.getNextEntry();
            ZipEntry entry = nextEntry;
            if (nextEntry != null) {
                if (entryName.equals(entry.getName())) {
                    break;
                }
            } else {
                $closeResource(null, zipInputStream);
                return null;
            }
        }
        $closeResource(r7, baos);
        throw th;
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

    public void extractTo(File targetDir) throws IOException {
        extractZipSafely(this.inputStream, targetDir, true);
    }

    /* JADX WARNING: Missing block: B:16:0x004a, code skipped:
            r6.getFD().sync();
     */
    /* JADX WARNING: Missing block: B:18:?, code skipped:
            $closeResource(null, r6);
     */
    /* JADX WARNING: Missing block: B:19:0x0054, code skipped:
            if (r13 == false) goto L_0x0059;
     */
    /* JADX WARNING: Missing block: B:20:0x0056, code skipped:
            com.android.timezone.distro.FileUtils.makeWorldReadable(r5);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static void extractZipSafely(InputStream is, File targetDir, boolean makeWorldReadable) throws IOException {
        Throwable th;
        Throwable th2;
        FileUtils.ensureDirectoriesExist(targetDir, makeWorldReadable);
        ZipInputStream zipInputStream = new ZipInputStream(is);
        try {
            FileOutputStream fos;
            byte[] buffer = new byte[8192];
            while (true) {
                ZipEntry nextEntry = zipInputStream.getNextEntry();
                ZipEntry entry = nextEntry;
                if (nextEntry != null) {
                    File entryFile = FileUtils.createSubFile(targetDir, entry.getName());
                    if (entry.isDirectory()) {
                        FileUtils.ensureDirectoriesExist(entryFile, makeWorldReadable);
                    } else {
                        if (!entryFile.getParentFile().exists()) {
                            FileUtils.ensureDirectoriesExist(entryFile.getParentFile(), makeWorldReadable);
                        }
                        fos = new FileOutputStream(entryFile);
                        while (true) {
                            try {
                                int read = zipInputStream.read(buffer);
                                int count = read;
                                if (read == -1) {
                                    break;
                                }
                                fos.write(buffer, 0, count);
                            } catch (Throwable th22) {
                                Throwable th3 = th22;
                                th22 = th;
                                th = th3;
                            }
                        }
                    }
                } else {
                    $closeResource(null, zipInputStream);
                    return;
                }
            }
            $closeResource(th22, fos);
            throw th;
        } catch (Throwable th4) {
            $closeResource(th, zipInputStream);
        }
    }
}
