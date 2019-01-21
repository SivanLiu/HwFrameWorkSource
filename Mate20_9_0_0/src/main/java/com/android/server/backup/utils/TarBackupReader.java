package com.android.server.backup.utils;

import android.app.backup.IBackupManagerMonitor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManagerInternal;
import android.content.pm.Signature;
import android.util.Slog;
import com.android.server.backup.BackupManagerService;
import com.android.server.backup.FileMetadata;
import com.android.server.backup.restore.RestorePolicy;
import com.android.server.usb.descriptors.UsbDescriptor;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class TarBackupReader {
    private static final int TAR_HEADER_LENGTH_FILESIZE = 12;
    private static final int TAR_HEADER_LENGTH_MODE = 8;
    private static final int TAR_HEADER_LENGTH_MODTIME = 12;
    private static final int TAR_HEADER_LENGTH_PATH = 100;
    private static final int TAR_HEADER_LENGTH_PATH_PREFIX = 155;
    private static final int TAR_HEADER_LONG_RADIX = 8;
    private static final int TAR_HEADER_OFFSET_FILESIZE = 124;
    private static final int TAR_HEADER_OFFSET_MODE = 100;
    private static final int TAR_HEADER_OFFSET_MODTIME = 136;
    private static final int TAR_HEADER_OFFSET_PATH = 0;
    private static final int TAR_HEADER_OFFSET_PATH_PREFIX = 345;
    private static final int TAR_HEADER_OFFSET_TYPE_CHAR = 156;
    private final BytesReadListener mBytesReadListener;
    private final InputStream mInputStream;
    private IBackupManagerMonitor mMonitor;
    private byte[] mWidgetData = null;

    public TarBackupReader(InputStream inputStream, BytesReadListener bytesReadListener, IBackupManagerMonitor monitor) {
        this.mInputStream = inputStream;
        this.mBytesReadListener = bytesReadListener;
        this.mMonitor = monitor;
    }

    public FileMetadata readTarHeaders() throws IOException {
        byte[] block = new byte[512];
        FileMetadata info = null;
        if (readTarHeader(block)) {
            try {
                StringBuilder stringBuilder;
                info = new FileMetadata();
                info.size = extractRadix(block, TAR_HEADER_OFFSET_FILESIZE, 12, 8);
                info.mtime = extractRadix(block, 136, 12, 8);
                info.mode = extractRadix(block, 100, 8, 8);
                info.path = extractString(block, TAR_HEADER_OFFSET_PATH_PREFIX, TAR_HEADER_LENGTH_PATH_PREFIX);
                String path = extractString(block, 0, 100);
                if (path.length() > 0) {
                    if (info.path.length() > 0) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(info.path);
                        stringBuilder.append('/');
                        info.path = stringBuilder.toString();
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(info.path);
                    stringBuilder.append(path);
                    info.path = stringBuilder.toString();
                }
                int typeChar = block[TAR_HEADER_OFFSET_TYPE_CHAR];
                if (typeChar == 120) {
                    boolean gotHeader = readPaxExtendedHeader(info);
                    if (gotHeader) {
                        gotHeader = readTarHeader(block);
                    }
                    if (gotHeader) {
                        typeChar = block[TAR_HEADER_OFFSET_TYPE_CHAR];
                    } else {
                        throw new IOException("Bad or missing pax header");
                    }
                }
                if (typeChar == 0) {
                    return null;
                }
                String str;
                if (typeChar == 48) {
                    info.type = 1;
                } else if (typeChar == 53) {
                    info.type = 2;
                    if (info.size != 0) {
                        Slog.w(BackupManagerService.TAG, "Directory entry with nonzero size in header");
                        info.size = 0;
                    }
                } else {
                    str = BackupManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown tar entity type: ");
                    stringBuilder.append(typeChar);
                    Slog.e(str, stringBuilder.toString());
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown entity type ");
                    stringBuilder.append(typeChar);
                    throw new IOException(stringBuilder.toString());
                }
                if ("shared/".regionMatches(0, info.path, 0, "shared/".length())) {
                    info.path = info.path.substring("shared/".length());
                    info.packageName = BackupManagerService.SHARED_BACKUP_AGENT_PACKAGE;
                    info.domain = "shared";
                    str = BackupManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("File in shared storage: ");
                    stringBuilder.append(info.path);
                    Slog.i(str, stringBuilder.toString());
                } else if ("apps/".regionMatches(0, info.path, 0, "apps/".length())) {
                    info.path = info.path.substring("apps/".length());
                    int slash = info.path.indexOf(47);
                    StringBuilder stringBuilder2;
                    if (slash >= 0) {
                        info.packageName = info.path.substring(0, slash);
                        info.path = info.path.substring(slash + 1);
                        if (!(info.path.equals(BackupManagerService.BACKUP_MANIFEST_FILENAME) || info.path.equals(BackupManagerService.BACKUP_METADATA_FILENAME))) {
                            slash = info.path.indexOf(47);
                            if (slash >= 0) {
                                info.domain = info.path.substring(0, slash);
                                info.path = info.path.substring(slash + 1);
                            } else {
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Illegal semantic path in non-manifest ");
                                stringBuilder2.append(info.path);
                                throw new IOException(stringBuilder2.toString());
                            }
                        }
                    }
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Illegal semantic path in ");
                    stringBuilder2.append(info.path);
                    throw new IOException(stringBuilder2.toString());
                }
            } catch (IOException e) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Parse error in header: ");
                stringBuilder3.append(e.getMessage());
                Slog.e(BackupManagerService.TAG, stringBuilder3.toString());
                throw e;
            }
        }
        return info;
    }

    private static int readExactly(InputStream in, byte[] buffer, int offset, int size) throws IOException {
        if (size > 0) {
            int soFar = 0;
            while (soFar < size) {
                int nRead = in.read(buffer, offset + soFar, size - soFar);
                if (nRead <= 0) {
                    break;
                }
                soFar += nRead;
            }
            return soFar;
        }
        throw new IllegalArgumentException("size must be > 0");
    }

    public Signature[] readAppManifestAndReturnSignatures(FileMetadata info) throws IOException {
        String str;
        StringBuilder stringBuilder;
        IllegalArgumentException e;
        if (info.size <= 65536) {
            byte[] buffer = new byte[((int) info.size)];
            if (((long) readExactly(this.mInputStream, buffer, 0, (int) info.size)) == info.size) {
                this.mBytesReadListener.onBytesRead(info.size);
                String[] str2 = new String[1];
                try {
                    int offset = extractLine(buffer, 0, str2);
                    int version = Integer.parseInt(str2[0]);
                    String manifestPackage;
                    if (version == 1) {
                        offset = extractLine(buffer, offset, str2);
                        manifestPackage = str2[0];
                        if (manifestPackage.equals(info.packageName)) {
                            offset = extractLine(buffer, offset, str2);
                            info.version = (long) Integer.parseInt(str2[0]);
                            offset = extractLine(buffer, offset, str2);
                            Integer.parseInt(str2[0]);
                            offset = extractLine(buffer, offset, str2);
                            info.installerPackageName = str2[0].length() > 0 ? str2[0] : null;
                            offset = extractLine(buffer, offset, str2);
                            info.hasApk = str2[0].equals("1");
                            offset = extractLine(buffer, offset, str2);
                            int numSigs = Integer.parseInt(str2[0]);
                            if (numSigs > 0) {
                                Signature[] sigs = new Signature[numSigs];
                                int offset2 = offset;
                                offset = 0;
                                while (offset < numSigs) {
                                    try {
                                        offset2 = extractLine(buffer, offset2, str2);
                                        sigs[offset] = new Signature(str2[0]);
                                        offset++;
                                    } catch (NumberFormatException e2) {
                                        str = BackupManagerService.TAG;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("Corrupt restore manifest for package ");
                                        stringBuilder.append(info.packageName);
                                        Slog.w(str, stringBuilder.toString());
                                        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 46, null, 3, BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", info.packageName));
                                        return null;
                                    } catch (IllegalArgumentException e3) {
                                        e = e3;
                                        offset = offset2;
                                        Slog.w(BackupManagerService.TAG, e.getMessage());
                                        return null;
                                    }
                                }
                                return sigs;
                            }
                            str = BackupManagerService.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Missing signature on backed-up package ");
                            stringBuilder2.append(info.packageName);
                            Slog.i(str, stringBuilder2.toString());
                            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 42, null, 3, BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", info.packageName));
                        } else {
                            str = BackupManagerService.TAG;
                            StringBuilder stringBuilder3 = new StringBuilder();
                            stringBuilder3.append("Expected package ");
                            stringBuilder3.append(info.packageName);
                            stringBuilder3.append(" but restore manifest claims ");
                            stringBuilder3.append(manifestPackage);
                            Slog.i(str, stringBuilder3.toString());
                            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 43, null, 3, BackupManagerMonitorUtils.putMonitoringExtra(BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", info.packageName), "android.app.backup.extra.LOG_MANIFEST_PACKAGE_NAME", manifestPackage));
                        }
                    } else {
                        manifestPackage = BackupManagerService.TAG;
                        StringBuilder stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("Unknown restore manifest version ");
                        stringBuilder4.append(version);
                        stringBuilder4.append(" for package ");
                        stringBuilder4.append(info.packageName);
                        Slog.i(manifestPackage, stringBuilder4.toString());
                        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 44, null, 3, BackupManagerMonitorUtils.putMonitoringExtra(BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", info.packageName), "android.app.backup.extra.LOG_EVENT_PACKAGE_VERSION", (long) version));
                    }
                } catch (NumberFormatException e4) {
                    str = BackupManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Corrupt restore manifest for package ");
                    stringBuilder.append(info.packageName);
                    Slog.w(str, stringBuilder.toString());
                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 46, null, 3, BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", info.packageName));
                    return null;
                } catch (IllegalArgumentException e5) {
                    e = e5;
                    Slog.w(BackupManagerService.TAG, e.getMessage());
                    return null;
                }
                return null;
            }
            throw new IOException("Unexpected EOF in manifest");
        }
        StringBuilder stringBuilder5 = new StringBuilder();
        stringBuilder5.append("Restore manifest too big; corrupt? size=");
        stringBuilder5.append(info.size);
        throw new IOException(stringBuilder5.toString());
    }

    public RestorePolicy chooseRestorePolicy(PackageManager packageManager, boolean allowApks, FileMetadata info, Signature[] signatures, PackageManagerInternal pmi) {
        if (signatures == null) {
            return RestorePolicy.IGNORE;
        }
        RestorePolicy policy = RestorePolicy.IGNORE;
        try {
            PackageInfo pkgInfo = packageManager.getPackageInfo(info.packageName, 134217728);
            String str;
            StringBuilder stringBuilder;
            if ((32768 & pkgInfo.applicationInfo.flags) != 0) {
                if (pkgInfo.applicationInfo.uid < 10000) {
                    if (pkgInfo.applicationInfo.backupAgentName == null) {
                        str = BackupManagerService.TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Package ");
                        stringBuilder.append(info.packageName);
                        stringBuilder.append(" is system level with no agent");
                        Slog.w(str, stringBuilder.toString());
                        this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 38, pkgInfo, 2, null);
                    }
                }
                if (!AppBackupUtils.signaturesMatch(signatures, pkgInfo, pmi)) {
                    str = BackupManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Restore manifest signatures do not match installed application for ");
                    stringBuilder.append(info.packageName);
                    Slog.w(str, stringBuilder.toString());
                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 37, pkgInfo, 3, null);
                } else if ((pkgInfo.applicationInfo.flags & 131072) != 0) {
                    Slog.i(BackupManagerService.TAG, "Package has restoreAnyVersion; taking data");
                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 34, pkgInfo, 3, null);
                    policy = RestorePolicy.ACCEPT;
                } else if (pkgInfo.getLongVersionCode() >= info.version) {
                    Slog.i(BackupManagerService.TAG, "Sig + version match; taking data");
                    policy = RestorePolicy.ACCEPT;
                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 35, pkgInfo, 3, null);
                } else if (allowApks) {
                    str = BackupManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Data version ");
                    stringBuilder.append(info.version);
                    stringBuilder.append(" is newer than installed version ");
                    stringBuilder.append(pkgInfo.getLongVersionCode());
                    stringBuilder.append(" - requiring apk");
                    Slog.i(str, stringBuilder.toString());
                    policy = RestorePolicy.ACCEPT_IF_APK;
                } else {
                    str = BackupManagerService.TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Data requires newer version ");
                    stringBuilder.append(info.version);
                    stringBuilder.append("; ignoring");
                    Slog.i(str, stringBuilder.toString());
                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 36, pkgInfo, 3, BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_OLD_VERSION", info.version));
                    policy = RestorePolicy.IGNORE;
                }
            } else {
                str = BackupManagerService.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Restore manifest from ");
                stringBuilder.append(info.packageName);
                stringBuilder.append(" but allowBackup=false");
                Slog.i(str, stringBuilder.toString());
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 39, pkgInfo, 3, null);
            }
        } catch (NameNotFoundException e) {
            if (allowApks) {
                String str2 = BackupManagerService.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Package ");
                stringBuilder2.append(info.packageName);
                stringBuilder2.append(" not installed; requiring apk in dataset");
                Slog.i(str2, stringBuilder2.toString());
                policy = RestorePolicy.ACCEPT_IF_APK;
            } else {
                policy = RestorePolicy.IGNORE;
            }
            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 40, null, 3, BackupManagerMonitorUtils.putMonitoringExtra(BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", info.packageName), "android.app.backup.extra.LOG_POLICY_ALLOW_APKS", allowApks));
        }
        if (policy == RestorePolicy.ACCEPT_IF_APK && !info.hasApk) {
            String str3 = BackupManagerService.TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Cannot restore package ");
            stringBuilder3.append(info.packageName);
            stringBuilder3.append(" without the matching .apk");
            Slog.i(str3, stringBuilder3.toString());
            this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 41, null, 3, BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", info.packageName));
        }
        return policy;
    }

    public void skipTarPadding(long size) throws IOException {
        long partial = (size + 512) % 512;
        if (partial > 0) {
            int needed = 512 - ((int) partial);
            if (readExactly(this.mInputStream, new byte[needed], 0, needed) == needed) {
                this.mBytesReadListener.onBytesRead((long) needed);
                return;
            }
            throw new IOException("Unexpected EOF in padding");
        }
    }

    public void readMetadata(FileMetadata info) throws IOException {
        if (info.size <= 65536) {
            byte[] buffer = new byte[((int) info.size)];
            if (((long) readExactly(this.mInputStream, buffer, 0, (int) info.size)) == info.size) {
                this.mBytesReadListener.onBytesRead(info.size);
                String[] str = new String[1];
                int offset = extractLine(buffer, 0, str);
                int version = Integer.parseInt(str[0]);
                String pkg;
                if (version == 1) {
                    offset = extractLine(buffer, offset, str);
                    pkg = str[0];
                    if (info.packageName.equals(pkg)) {
                        ByteArrayInputStream bin = new ByteArrayInputStream(buffer, offset, buffer.length - offset);
                        DataInputStream in = new DataInputStream(bin);
                        while (bin.available() > 0) {
                            int token = in.readInt();
                            int size = in.readInt();
                            StringBuilder stringBuilder;
                            if (size > 65536) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Datum ");
                                stringBuilder.append(Integer.toHexString(token));
                                stringBuilder.append(" too big; corrupt? size=");
                                stringBuilder.append(info.size);
                                throw new IOException(stringBuilder.toString());
                            } else if (token != BackupManagerService.BACKUP_WIDGET_METADATA_TOKEN) {
                                String str2 = BackupManagerService.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Ignoring metadata blob ");
                                stringBuilder.append(Integer.toHexString(token));
                                stringBuilder.append(" for ");
                                stringBuilder.append(info.packageName);
                                Slog.i(str2, stringBuilder.toString());
                                in.skipBytes(size);
                            } else {
                                this.mWidgetData = new byte[size];
                                in.read(this.mWidgetData);
                            }
                        }
                        return;
                    }
                    String str3 = BackupManagerService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Metadata mismatch: package ");
                    stringBuilder2.append(info.packageName);
                    stringBuilder2.append(" but widget data for ");
                    stringBuilder2.append(pkg);
                    Slog.w(str3, stringBuilder2.toString());
                    this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 47, null, 3, BackupManagerMonitorUtils.putMonitoringExtra(BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", info.packageName), "android.app.backup.extra.LOG_WIDGET_PACKAGE_NAME", pkg));
                    return;
                }
                pkg = BackupManagerService.TAG;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("Unsupported metadata version ");
                stringBuilder3.append(version);
                Slog.w(pkg, stringBuilder3.toString());
                this.mMonitor = BackupManagerMonitorUtils.monitorEvent(this.mMonitor, 48, null, 3, BackupManagerMonitorUtils.putMonitoringExtra(BackupManagerMonitorUtils.putMonitoringExtra(null, "android.app.backup.extra.LOG_EVENT_PACKAGE_NAME", info.packageName), "android.app.backup.extra.LOG_EVENT_PACKAGE_VERSION", (long) version));
                return;
            }
            throw new IOException("Unexpected EOF in widget data");
        }
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("Metadata too big; corrupt? size=");
        stringBuilder4.append(info.size);
        throw new IOException(stringBuilder4.toString());
    }

    private static int extractLine(byte[] buffer, int offset, String[] outStr) throws IOException {
        int end = buffer.length;
        if (offset < end) {
            int pos = offset;
            while (pos < end && buffer[pos] != (byte) 10) {
                pos++;
            }
            outStr[0] = new String(buffer, offset, pos - offset);
            return pos + 1;
        }
        throw new IOException("Incomplete data");
    }

    private boolean readTarHeader(byte[] block) throws IOException {
        int got = readExactly(this.mInputStream, block, 0, 512);
        if (got == 0) {
            return false;
        }
        if (got >= 512) {
            this.mBytesReadListener.onBytesRead(512);
            return true;
        }
        throw new IOException("Unable to read full block header");
    }

    private boolean readPaxExtendedHeader(FileMetadata info) throws IOException {
        if (info.size <= 32768) {
            byte[] data = new byte[(((int) ((info.size + 511) >> 9)) * 512)];
            int offset = 0;
            if (readExactly(this.mInputStream, data, 0, data.length) >= data.length) {
                this.mBytesReadListener.onBytesRead((long) data.length);
                int contentSize = (int) info.size;
                do {
                    int offset2 = offset;
                    offset = offset2 + 1;
                    while (offset < contentSize && data[offset] != (byte) 32) {
                        offset++;
                    }
                    if (offset < contentSize) {
                        int linelen = (int) extractRadix(data, offset2, offset - offset2, 10);
                        int key = offset + 1;
                        int eol = (offset2 + linelen) - 1;
                        offset = key + 1;
                        while (data[offset] != (byte) 61 && offset <= eol) {
                            offset++;
                        }
                        if (offset <= eol) {
                            String keyStr = new String(data, key, offset - key, "UTF-8");
                            String valStr = new String(data, offset + 1, (eol - offset) - 1, "UTF-8");
                            if ("path".equals(keyStr)) {
                                info.path = valStr;
                            } else if ("size".equals(keyStr)) {
                                info.size = Long.parseLong(valStr);
                            } else {
                                String str = BackupManagerService.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Unhandled pax key: ");
                                stringBuilder.append(key);
                                Slog.i(str, stringBuilder.toString());
                            }
                            offset = offset2 + linelen;
                        } else {
                            throw new IOException("Invalid pax declaration");
                        }
                    }
                    throw new IOException("Invalid pax data");
                } while (offset < contentSize);
                return true;
            }
            throw new IOException("Unable to read full pax header");
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("Suspiciously large pax header size ");
        stringBuilder2.append(info.size);
        stringBuilder2.append(" - aborting");
        Slog.w(BackupManagerService.TAG, stringBuilder2.toString());
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("Sanity failure: pax header size ");
        stringBuilder3.append(info.size);
        throw new IOException(stringBuilder3.toString());
    }

    private static long extractRadix(byte[] data, int offset, int maxChars, int radix) throws IOException {
        int end = offset + maxChars;
        long value = 0;
        int i = offset;
        while (i < end) {
            byte b = data[i];
            if (b == (byte) 0 || b == (byte) 32) {
                break;
            } else if (b < UsbDescriptor.DESCRIPTORTYPE_ENDPOINT_COMPANION || b > (48 + radix) - 1) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Invalid number in header: '");
                stringBuilder.append((char) b);
                stringBuilder.append("' for radix ");
                stringBuilder.append(radix);
                throw new IOException(stringBuilder.toString());
            } else {
                value = (((long) radix) * value) + ((long) (b - 48));
                i++;
            }
        }
        return value;
    }

    private static String extractString(byte[] data, int offset, int maxChars) throws IOException {
        int end = offset + maxChars;
        int eos = offset;
        while (eos < end && data[eos] != (byte) 0) {
            eos++;
        }
        return new String(data, offset, eos - offset, "US-ASCII");
    }

    private static void hexLog(byte[] block) {
        int offset = 0;
        int todo = block.length;
        StringBuilder buf = new StringBuilder(64);
        while (todo > 0) {
            buf.append(String.format("%04x   ", new Object[]{Integer.valueOf(offset)}));
            int numThisLine = 16;
            if (todo <= 16) {
                numThisLine = todo;
            }
            for (int i = 0; i < numThisLine; i++) {
                buf.append(String.format("%02x ", new Object[]{Byte.valueOf(block[offset + i])}));
            }
            Slog.i("hexdump", buf.toString());
            buf.setLength(0);
            todo -= numThisLine;
            offset += numThisLine;
        }
    }

    public IBackupManagerMonitor getMonitor() {
        return this.mMonitor;
    }

    public byte[] getWidgetData() {
        return this.mWidgetData;
    }
}
