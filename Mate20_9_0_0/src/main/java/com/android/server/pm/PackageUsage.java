package com.android.server.pm;

import android.content.pm.PackageParser.Package;
import android.os.FileUtils;
import android.util.AtomicFile;
import android.util.Log;
import com.android.server.os.HwBootFail;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import libcore.io.IoUtils;

class PackageUsage extends AbstractStatsBase<Map<String, Package>> {
    private static final String USAGE_FILE_MAGIC = "PACKAGE_USAGE__VERSION_";
    private static final String USAGE_FILE_MAGIC_VERSION_1 = "PACKAGE_USAGE__VERSION_1";
    private boolean mIsHistoricalPackageUsageAvailable = true;

    PackageUsage() {
        super("package-usage.list", "PackageUsage_DiskWriter", true);
    }

    boolean isHistoricalPackageUsageAvailable() {
        return this.mIsHistoricalPackageUsageAvailable;
    }

    protected void writeInternal(Map<String, Package> packages) {
        AtomicFile file = getFile();
        try {
            FileOutputStream f = file.startWrite();
            BufferedOutputStream out = new BufferedOutputStream(f);
            FileUtils.setPermissions(file.getBaseFile().getPath(), 416, 1000, 1032);
            StringBuilder sb = new StringBuilder();
            sb.append(USAGE_FILE_MAGIC_VERSION_1);
            sb.append(10);
            out.write(sb.toString().getBytes(StandardCharsets.US_ASCII));
            for (Package pkg : packages.values()) {
                if (pkg.getLatestPackageUseTimeInMills() != 0) {
                    int i = 0;
                    sb.setLength(0);
                    sb.append(pkg.packageName);
                    long[] jArr = pkg.mLastPackageUsageTimeInMills;
                    int length = jArr.length;
                    while (i < length) {
                        long usageTimeInMillis = jArr[i];
                        sb.append(' ');
                        sb.append(usageTimeInMillis);
                        i++;
                    }
                    sb.append(10);
                    out.write(sb.toString().getBytes(StandardCharsets.US_ASCII));
                }
            }
            out.flush();
            file.finishWrite(f);
        } catch (IOException e) {
            if (null != null) {
                file.failWrite(null);
            }
            Log.e("PackageManager", "Failed to write package usage times", e);
        }
    }

    protected void readInternal(Map<String, Package> packages) {
        BufferedInputStream in = null;
        try {
            in = new BufferedInputStream(getFile().openRead());
            StringBuffer sb = new StringBuffer();
            String firstLine = readLine(in, sb);
            if (firstLine != null) {
                if (USAGE_FILE_MAGIC_VERSION_1.equals(firstLine)) {
                    readVersion1LP(packages, in, sb);
                } else {
                    readVersion0LP(packages, in, sb, firstLine);
                }
            }
        } catch (FileNotFoundException e) {
            this.mIsHistoricalPackageUsageAvailable = false;
        } catch (IOException e2) {
            Log.w("PackageManager", "Failed to read package usage times", e2);
        } catch (NullPointerException e3) {
            Log.w("PackageManager", "error NullPointerException", e3);
            HwBootFail.brokenFileBootFail(83886087, "/data/system/package-usage.list", new Throwable());
        } catch (Throwable th) {
            IoUtils.closeQuietly(null);
        }
        IoUtils.closeQuietly(in);
    }

    private void readVersion0LP(Map<String, Package> packages, InputStream in, StringBuffer sb, String firstLine) throws IOException {
        String line = firstLine;
        while (line != null) {
            String[] tokens = line.split(" ");
            if (tokens.length == 2) {
                int reason = 0;
                Package pkg = (Package) packages.get(tokens[0]);
                if (pkg != null) {
                    long timestamp = parseAsLong(tokens[1]);
                    while (reason < 8) {
                        pkg.mLastPackageUsageTimeInMills[reason] = timestamp;
                        reason++;
                    }
                }
                line = readLine(in, sb);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to parse ");
                stringBuilder.append(line);
                stringBuilder.append(" as package-timestamp pair.");
                throw new IOException(stringBuilder.toString());
            }
        }
    }

    private void readVersion1LP(Map<String, Package> packages, InputStream in, StringBuffer sb) throws IOException {
        while (true) {
            String readLine = readLine(in, sb);
            String line = readLine;
            if (readLine != null) {
                String[] tokens = line.split(" ");
                if (tokens.length == 9) {
                    int reason = 0;
                    Package pkg = (Package) packages.get(tokens[0]);
                    if (pkg != null) {
                        while (reason < 8) {
                            pkg.mLastPackageUsageTimeInMills[reason] = parseAsLong(tokens[reason + 1]);
                            reason++;
                        }
                    }
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to parse ");
                    stringBuilder.append(line);
                    stringBuilder.append(" as a timestamp array.");
                    throw new IOException(stringBuilder.toString());
                }
            }
            return;
        }
    }

    private long parseAsLong(String token) throws IOException {
        try {
            return Long.parseLong(token);
        } catch (NumberFormatException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Failed to parse ");
            stringBuilder.append(token);
            stringBuilder.append(" as a long.");
            throw new IOException(stringBuilder.toString(), e);
        }
    }

    private String readLine(InputStream in, StringBuffer sb) throws IOException {
        return readToken(in, sb, 10);
    }

    private String readToken(InputStream in, StringBuffer sb, char endOfToken) throws IOException {
        sb.setLength(0);
        while (true) {
            char ch = in.read();
            if (ch == 65535) {
                if (sb.length() == 0) {
                    return null;
                }
                throw new IOException("Unexpected EOF");
            } else if (ch == endOfToken) {
                return sb.toString();
            } else {
                sb.append((char) ch);
            }
        }
    }
}
