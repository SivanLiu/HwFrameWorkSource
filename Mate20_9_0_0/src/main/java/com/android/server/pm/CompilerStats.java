package com.android.server.pm;

import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.Log;
import com.android.internal.util.FastPrintWriter;
import com.android.internal.util.IndentingPrintWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import libcore.io.IoUtils;

class CompilerStats extends AbstractStatsBase<Void> {
    private static final int COMPILER_STATS_VERSION = 1;
    private static final String COMPILER_STATS_VERSION_HEADER = "PACKAGE_MANAGER__COMPILER_STATS__";
    private final Map<String, PackageStats> packageStats = new HashMap();

    static class PackageStats {
        private final Map<String, Long> compileTimePerCodePath = new ArrayMap(2);
        private final String packageName;

        public PackageStats(String packageName) {
            this.packageName = packageName;
        }

        public String getPackageName() {
            return this.packageName;
        }

        public long getCompileTime(String codePath) {
            String storagePath = getStoredPathFromCodePath(codePath);
            synchronized (this.compileTimePerCodePath) {
                Long l = (Long) this.compileTimePerCodePath.get(storagePath);
                if (l == null) {
                    return 0;
                }
                long longValue = l.longValue();
                return longValue;
            }
        }

        public void setCompileTime(String codePath, long compileTimeInMs) {
            String storagePath = getStoredPathFromCodePath(codePath);
            synchronized (this.compileTimePerCodePath) {
                if (compileTimeInMs <= 0) {
                    this.compileTimePerCodePath.remove(storagePath);
                } else {
                    this.compileTimePerCodePath.put(storagePath, Long.valueOf(compileTimeInMs));
                }
            }
        }

        private static String getStoredPathFromCodePath(String codePath) {
            return codePath.substring(codePath.lastIndexOf(File.separatorChar) + 1);
        }

        public void dump(IndentingPrintWriter ipw) {
            synchronized (this.compileTimePerCodePath) {
                if (this.compileTimePerCodePath.size() == 0) {
                    ipw.println("(No recorded stats)");
                } else {
                    for (Entry<String, Long> e : this.compileTimePerCodePath.entrySet()) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(" ");
                        stringBuilder.append((String) e.getKey());
                        stringBuilder.append(" - ");
                        stringBuilder.append(e.getValue());
                        ipw.println(stringBuilder.toString());
                    }
                }
            }
        }
    }

    public CompilerStats() {
        super("package-cstats.list", "CompilerStats_DiskWriter", false);
    }

    public PackageStats getPackageStats(String packageName) {
        PackageStats packageStats;
        synchronized (this.packageStats) {
            packageStats = (PackageStats) this.packageStats.get(packageName);
        }
        return packageStats;
    }

    public void setPackageStats(String packageName, PackageStats stats) {
        synchronized (this.packageStats) {
            this.packageStats.put(packageName, stats);
        }
    }

    public PackageStats createPackageStats(String packageName) {
        PackageStats newStats;
        synchronized (this.packageStats) {
            newStats = new PackageStats(packageName);
            this.packageStats.put(packageName, newStats);
        }
        return newStats;
    }

    public PackageStats getOrCreatePackageStats(String packageName) {
        synchronized (this.packageStats) {
            PackageStats existingStats = (PackageStats) this.packageStats.get(packageName);
            if (existingStats != null) {
                return existingStats;
            }
            PackageStats createPackageStats = createPackageStats(packageName);
            return createPackageStats;
        }
    }

    public void deletePackageStats(String packageName) {
        synchronized (this.packageStats) {
            this.packageStats.remove(packageName);
        }
    }

    public void write(Writer out) {
        FastPrintWriter fpw = new FastPrintWriter(out);
        fpw.print(COMPILER_STATS_VERSION_HEADER);
        fpw.println(1);
        synchronized (this.packageStats) {
            for (PackageStats pkg : this.packageStats.values()) {
                synchronized (pkg.compileTimePerCodePath) {
                    if (!pkg.compileTimePerCodePath.isEmpty()) {
                        fpw.println(pkg.getPackageName());
                        for (Entry<String, Long> e : pkg.compileTimePerCodePath.entrySet()) {
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("-");
                            stringBuilder.append((String) e.getKey());
                            stringBuilder.append(":");
                            stringBuilder.append(e.getValue());
                            fpw.println(stringBuilder.toString());
                        }
                    }
                }
            }
        }
        fpw.flush();
    }

    public boolean read(Reader r) {
        synchronized (this.packageStats) {
            this.packageStats.clear();
            try {
                BufferedReader in = new BufferedReader(r);
                String versionLine = in.readLine();
                if (versionLine == null) {
                    throw new IllegalArgumentException("No version line found.");
                } else if (versionLine.startsWith(COMPILER_STATS_VERSION_HEADER)) {
                    int version = Integer.parseInt(versionLine.substring(COMPILER_STATS_VERSION_HEADER.length()));
                    if (version == 1) {
                        String s;
                        StringBuilder stringBuilder;
                        PackageStats currentPackage = new PackageStats("fake package");
                        while (true) {
                            String readLine = in.readLine();
                            s = readLine;
                            if (readLine == null) {
                            } else if (s.startsWith("-")) {
                                int colonIndex = s.indexOf(58);
                                if (colonIndex == -1 || colonIndex == 1) {
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Could not parse data ");
                                    stringBuilder.append(s);
                                } else {
                                    currentPackage.setCompileTime(s.substring(1, colonIndex), Long.parseLong(s.substring(colonIndex + 1)));
                                }
                            } else {
                                currentPackage = getOrCreatePackageStats(s);
                            }
                        }
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Could not parse data ");
                        stringBuilder.append(s);
                        throw new IllegalArgumentException(stringBuilder.toString());
                    }
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Unexpected version: ");
                    stringBuilder2.append(version);
                    throw new IllegalArgumentException(stringBuilder2.toString());
                } else {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Invalid version line: ");
                    stringBuilder3.append(versionLine);
                    throw new IllegalArgumentException(stringBuilder3.toString());
                }
            } catch (Exception e) {
                Log.e("PackageManager", "Error parsing compiler stats", e);
                return false;
            }
        }
        return true;
    }

    void writeNow() {
        writeNow(null);
    }

    boolean maybeWriteAsync() {
        return maybeWriteAsync(null);
    }

    protected void writeInternal(Void data) {
        AtomicFile file = getFile();
        FileOutputStream f = null;
        try {
            f = file.startWrite();
            OutputStreamWriter osw = new OutputStreamWriter(f);
            write(osw);
            osw.flush();
            file.finishWrite(f);
        } catch (IOException e) {
            if (f != null) {
                file.failWrite(f);
            }
            Log.e("PackageManager", "Failed to write compiler stats", e);
        }
    }

    void read() {
        read((Void) null);
    }

    protected void readInternal(Void data) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(getFile().openRead()));
            read(in);
        } catch (FileNotFoundException e) {
        } catch (Throwable th) {
            IoUtils.closeQuietly(in);
        }
        IoUtils.closeQuietly(in);
    }
}
