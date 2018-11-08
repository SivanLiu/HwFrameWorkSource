package libcore.tzdata.shared2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class TimeZoneDistro {
    private static final int BUFFER_SIZE = 8192;
    public static final String DISTRO_VERSION_FILE_NAME = "distro_version";
    public static final String ICU_DATA_FILE_NAME = "icu/icu_tzdata.dat";
    private static final long MAX_GET_ENTRY_CONTENTS_SIZE = 131072;
    public static final String TZDATA_FILE_NAME = "tzdata";
    public static final String TZLOOKUP_FILE_NAME = "tzlookup.xml";
    private final byte[] bytes;

    public TimeZoneDistro(byte[] bytes) {
        this.bytes = bytes;
    }

    public byte[] getBytes() {
        return this.bytes;
    }

    public DistroVersion getDistroVersion() throws DistroException, IOException {
        byte[] contents = getEntryContents(new ByteArrayInputStream(this.bytes), DISTRO_VERSION_FILE_NAME);
        if (contents != null) {
            return DistroVersion.fromBytes(contents);
        }
        throw new DistroException("Distro version file entry not found");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static byte[] getEntryContents(InputStream is, String entryName) throws IOException {
        Throwable th;
        Throwable th2;
        Throwable th3;
        Throwable th4 = null;
        ZipInputStream zipInputStream = null;
        try {
            ZipEntry entry;
            ZipInputStream zipInputStream2 = new ZipInputStream(is);
            do {
                try {
                    entry = zipInputStream2.getNextEntry();
                    if (entry != null) {
                    } else {
                        if (zipInputStream2 != null) {
                            try {
                                zipInputStream2.close();
                            } catch (Throwable th5) {
                                th4 = th5;
                            }
                        }
                        if (th4 == null) {
                            return null;
                        }
                        throw th4;
                    }
                } catch (Throwable th6) {
                    th = th6;
                    zipInputStream = zipInputStream2;
                    th2 = null;
                }
            } while (!entryName.equals(entry.getName()));
            if (entry.getSize() > MAX_GET_ENTRY_CONTENTS_SIZE) {
                throw new IOException("Entry " + entryName + " too large: " + entry.getSize());
            }
            byte[] buffer = new byte[8192];
            th2 = null;
            ByteArrayOutputStream byteArrayOutputStream = null;
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                while (true) {
                    try {
                        int count = zipInputStream2.read(buffer);
                        if (count == -1) {
                            break;
                        }
                        baos.write(buffer, 0, count);
                    } catch (Throwable th7) {
                        th = th7;
                        byteArrayOutputStream = baos;
                    }
                }
                byte[] toByteArray = baos.toByteArray();
                if (baos != null) {
                    baos.close();
                }
                if (th2 != null) {
                    throw th2;
                } else {
                    if (zipInputStream2 != null) {
                        try {
                            zipInputStream2.close();
                        } catch (Throwable th8) {
                            th4 = th8;
                        }
                    }
                    if (th4 == null) {
                        return toByteArray;
                    }
                    throw th4;
                }
            } catch (Throwable th9) {
                th = th9;
                if (byteArrayOutputStream != null) {
                    byteArrayOutputStream.close();
                }
                if (th2 != null) {
                    throw th2;
                } else {
                    throw th;
                }
            }
        } catch (Throwable th10) {
            th = th10;
            th2 = null;
            if (zipInputStream != null) {
                try {
                    zipInputStream.close();
                } catch (Throwable th42) {
                    if (th2 == null) {
                        th2 = th42;
                    } else if (th2 != th42) {
                        th2.addSuppressed(th42);
                    }
                }
            }
            if (th2 != null) {
                throw th2;
            }
            throw th;
        }
    }

    public void extractTo(File targetDir) throws IOException {
        extractZipSafely(new ByteArrayInputStream(this.bytes), targetDir, true);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static void extractZipSafely(InputStream is, File targetDir, boolean makeWorldReadable) throws IOException {
        Throwable th;
        FileUtils.ensureDirectoriesExist(targetDir, makeWorldReadable);
        Throwable th2 = null;
        ZipInputStream zipInputStream = null;
        Throwable th3;
        Throwable th4;
        try {
            ZipInputStream zipInputStream2 = new ZipInputStream(is);
            try {
                FileOutputStream fileOutputStream;
                byte[] buffer = new byte[8192];
                while (true) {
                    ZipEntry entry = zipInputStream2.getNextEntry();
                    if (entry == null) {
                        break;
                    }
                    File entryFile = FileUtils.createSubFile(targetDir, entry.getName());
                    if (entry.isDirectory()) {
                        FileUtils.ensureDirectoriesExist(entryFile, makeWorldReadable);
                    } else {
                        if (!entryFile.getParentFile().exists()) {
                            FileUtils.ensureDirectoriesExist(entryFile.getParentFile(), makeWorldReadable);
                        }
                        th3 = null;
                        fileOutputStream = null;
                        try {
                            FileOutputStream fos = new FileOutputStream(entryFile);
                            while (true) {
                                try {
                                    int count = zipInputStream2.read(buffer);
                                    if (count == -1) {
                                        break;
                                    }
                                    fos.write(buffer, 0, count);
                                } catch (Throwable th5) {
                                    th4 = th5;
                                    fileOutputStream = fos;
                                }
                            }
                            fos.getFD().sync();
                            if (fos != null) {
                                fos.close();
                            }
                            if (th3 != null) {
                                throw th3;
                            } else if (makeWorldReadable) {
                                FileUtils.makeWorldReadable(entryFile);
                            }
                        } catch (Throwable th6) {
                            th4 = th6;
                        }
                    }
                }
                if (zipInputStream2 != null) {
                    try {
                        zipInputStream2.close();
                    } catch (Throwable th7) {
                        th2 = th7;
                    }
                }
                if (th2 != null) {
                    throw th2;
                }
                return;
                try {
                    throw th4;
                } catch (Throwable th32) {
                    th = th32;
                    th32 = th4;
                    th4 = th;
                }
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
                if (th32 != null) {
                    throw th32;
                } else {
                    throw th4;
                }
            } catch (Throwable th8) {
                th4 = th8;
                zipInputStream = zipInputStream2;
                th32 = null;
            }
        } catch (Throwable th9) {
            th4 = th9;
            th32 = null;
            if (zipInputStream != null) {
                try {
                    zipInputStream.close();
                } catch (Throwable th22) {
                    if (th32 == null) {
                        th32 = th22;
                    } else if (th32 != th22) {
                        th32.addSuppressed(th22);
                    }
                }
            }
            if (th32 != null) {
                throw th32;
            }
            throw th4;
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return Arrays.equals(this.bytes, ((TimeZoneDistro) o).bytes);
    }
}
