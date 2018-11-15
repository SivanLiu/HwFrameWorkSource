package com.android.server.pm;

import android.util.ArrayMap;
import android.util.ArraySet;
import java.io.PrintWriter;

public class ShortcutNonPersistentUser {
    private final ArraySet<String> mHostPackageSet = new ArraySet();
    private final ArrayMap<String, String> mHostPackages = new ArrayMap();
    private final ShortcutService mService;
    private final int mUserId;

    public ShortcutNonPersistentUser(ShortcutService service, int userId) {
        this.mService = service;
        this.mUserId = userId;
    }

    public int getUserId() {
        return this.mUserId;
    }

    public void setShortcutHostPackage(String type, String packageName) {
        if (packageName != null) {
            this.mHostPackages.put(type, packageName);
        } else {
            this.mHostPackages.remove(type);
        }
        this.mHostPackageSet.clear();
        for (int i = 0; i < this.mHostPackages.size(); i++) {
            this.mHostPackageSet.add((String) this.mHostPackages.valueAt(i));
        }
    }

    public boolean hasHostPackage(String packageName) {
        return this.mHostPackageSet.contains(packageName);
    }

    public void dump(PrintWriter pw, String prefix, DumpFilter filter) {
        if (filter.shouldDumpDetails() && this.mHostPackages.size() > 0) {
            pw.print(prefix);
            pw.print("Non-persistent: user ID:");
            pw.println(this.mUserId);
            pw.print(prefix);
            pw.println("  Host packages:");
            for (int i = 0; i < this.mHostPackages.size(); i++) {
                pw.print(prefix);
                pw.print("    ");
                pw.print((String) this.mHostPackages.keyAt(i));
                pw.print(": ");
                pw.println((String) this.mHostPackages.valueAt(i));
            }
            pw.println();
        }
    }
}
