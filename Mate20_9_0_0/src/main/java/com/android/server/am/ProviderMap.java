package com.android.server.am;

import android.content.ComponentName;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.SparseArray;
import com.android.internal.os.TransferPipe;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.DumpUtils;
import com.android.server.utils.PriorityDump;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;

public final class ProviderMap {
    private static final boolean DBG = false;
    private static final String TAG = "ProviderMap";
    private final ActivityManagerService mAm;
    private final SparseArray<HashMap<ComponentName, ContentProviderRecord>> mProvidersByClassPerUser = new SparseArray();
    private final SparseArray<HashMap<String, ContentProviderRecord>> mProvidersByNamePerUser = new SparseArray();
    private final HashMap<ComponentName, ContentProviderRecord> mSingletonByClass = new HashMap();
    private final HashMap<String, ContentProviderRecord> mSingletonByName = new HashMap();

    ProviderMap(ActivityManagerService am) {
        this.mAm = am;
    }

    ContentProviderRecord getProviderByName(String name) {
        return getProviderByName(name, -1);
    }

    ContentProviderRecord getProviderByName(String name, int userId) {
        ContentProviderRecord record = (ContentProviderRecord) this.mSingletonByName.get(name);
        if (record != null) {
            return record;
        }
        return (ContentProviderRecord) getProvidersByName(userId).get(name);
    }

    ContentProviderRecord getProviderByClass(ComponentName name) {
        return getProviderByClass(name, -1);
    }

    ContentProviderRecord getProviderByClass(ComponentName name, int userId) {
        ContentProviderRecord record = (ContentProviderRecord) this.mSingletonByClass.get(name);
        if (record != null) {
            return record;
        }
        return (ContentProviderRecord) getProvidersByClass(userId).get(name);
    }

    void putProviderByName(String name, ContentProviderRecord record) {
        if (record.singleton) {
            this.mSingletonByName.put(name, record);
        } else {
            getProvidersByName(UserHandle.getUserId(record.appInfo.uid)).put(name, record);
        }
    }

    void putProviderByClass(ComponentName name, ContentProviderRecord record) {
        if (record.singleton) {
            this.mSingletonByClass.put(name, record);
        } else {
            getProvidersByClass(UserHandle.getUserId(record.appInfo.uid)).put(name, record);
        }
    }

    void removeProviderByName(String name, int userId) {
        if (this.mSingletonByName.containsKey(name)) {
            this.mSingletonByName.remove(name);
        } else if (userId >= 0) {
            HashMap<String, ContentProviderRecord> map = getProvidersByName(userId);
            map.remove(name);
            if (map.size() == 0) {
                this.mProvidersByNamePerUser.remove(userId);
            }
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Bad user ");
            stringBuilder.append(userId);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    void removeProviderByClass(ComponentName name, int userId) {
        if (this.mSingletonByClass.containsKey(name)) {
            this.mSingletonByClass.remove(name);
        } else if (userId >= 0) {
            HashMap<ComponentName, ContentProviderRecord> map = getProvidersByClass(userId);
            map.remove(name);
            if (map.size() == 0) {
                this.mProvidersByClassPerUser.remove(userId);
            }
        } else {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Bad user ");
            stringBuilder.append(userId);
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    private HashMap<String, ContentProviderRecord> getProvidersByName(int userId) {
        if (userId >= 0) {
            HashMap<String, ContentProviderRecord> map = (HashMap) this.mProvidersByNamePerUser.get(userId);
            if (map != null) {
                return map;
            }
            HashMap<String, ContentProviderRecord> newMap = new HashMap();
            this.mProvidersByNamePerUser.put(userId, newMap);
            return newMap;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Bad user ");
        stringBuilder.append(userId);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    HashMap<ComponentName, ContentProviderRecord> getProvidersByClass(int userId) {
        if (userId >= 0) {
            HashMap<ComponentName, ContentProviderRecord> map = (HashMap) this.mProvidersByClassPerUser.get(userId);
            if (map != null) {
                return map;
            }
            HashMap<ComponentName, ContentProviderRecord> newMap = new HashMap();
            this.mProvidersByClassPerUser.put(userId, newMap);
            return newMap;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Bad user ");
        stringBuilder.append(userId);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private boolean collectPackageProvidersLocked(String packageName, Set<String> filterByClasses, boolean doit, boolean evenPersistent, HashMap<ComponentName, ContentProviderRecord> providers, ArrayList<ContentProviderRecord> result) {
        boolean didSomething = false;
        for (ContentProviderRecord provider : providers.values()) {
            boolean sameComponent = packageName == null || (provider.info.packageName.equals(packageName) && (filterByClasses == null || filterByClasses.contains(provider.name.getClassName())));
            if (sameComponent && (provider.proc == null || evenPersistent || !provider.proc.persistent)) {
                if (!doit) {
                    return true;
                }
                didSomething = true;
                result.add(provider);
            }
        }
        return didSomething;
    }

    boolean collectPackageProvidersLocked(String packageName, Set<String> filterByClasses, boolean doit, boolean evenPersistent, int userId, ArrayList<ContentProviderRecord> result) {
        int i = userId;
        boolean didSomething = false;
        if (i == -1 || i == 0) {
            didSomething = collectPackageProvidersLocked(packageName, (Set) filterByClasses, doit, evenPersistent, this.mSingletonByClass, (ArrayList) result);
        }
        if (!doit && didSomething) {
            return true;
        }
        if (i == -1) {
            int i2 = 0;
            while (true) {
                int i3 = i2;
                if (i3 >= this.mProvidersByClassPerUser.size()) {
                    break;
                }
                if (collectPackageProvidersLocked(packageName, (Set) filterByClasses, doit, evenPersistent, (HashMap) this.mProvidersByClassPerUser.valueAt(i3), (ArrayList) result)) {
                    if (!doit) {
                        return true;
                    }
                    didSomething = true;
                }
                i2 = i3 + 1;
            }
        } else {
            HashMap<ComponentName, ContentProviderRecord> items = getProvidersByClass(i);
            if (items != null) {
                didSomething |= collectPackageProvidersLocked(packageName, (Set) filterByClasses, doit, evenPersistent, (HashMap) items, (ArrayList) result);
            }
        }
        return didSomething;
    }

    private boolean dumpProvidersByClassLocked(PrintWriter pw, boolean dumpAll, String dumpPackage, String header, boolean needSep, HashMap<ComponentName, ContentProviderRecord> map) {
        boolean written = false;
        for (Entry<ComponentName, ContentProviderRecord> e : map.entrySet()) {
            ContentProviderRecord r = (ContentProviderRecord) e.getValue();
            if (dumpPackage == null || dumpPackage.equals(r.appInfo.packageName)) {
                if (needSep) {
                    pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    needSep = false;
                }
                if (header != null) {
                    pw.println(header);
                    header = null;
                }
                written = true;
                pw.print("  * ");
                pw.println(r);
                r.dump(pw, "    ", dumpAll);
            }
        }
        return written;
    }

    private boolean dumpProvidersByNameLocked(PrintWriter pw, String dumpPackage, String header, boolean needSep, HashMap<String, ContentProviderRecord> map) {
        boolean written = false;
        for (Entry<String, ContentProviderRecord> e : map.entrySet()) {
            ContentProviderRecord r = (ContentProviderRecord) e.getValue();
            if (dumpPackage == null || dumpPackage.equals(r.appInfo.packageName)) {
                if (needSep) {
                    pw.println(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                    needSep = false;
                }
                if (header != null) {
                    pw.println(header);
                    header = null;
                }
                written = true;
                pw.print("  ");
                pw.print((String) e.getKey());
                pw.print(": ");
                pw.println(r.toShortString());
            }
        }
        return written;
    }

    boolean dumpProvidersLocked(PrintWriter pw, boolean dumpAll, String dumpPackage) {
        boolean needSep = false;
        if (this.mSingletonByClass.size() > 0) {
            needSep = false | dumpProvidersByClassLocked(pw, dumpAll, dumpPackage, "  Published single-user content providers (by class):", false, this.mSingletonByClass);
        }
        int i = 0;
        boolean needSep2 = needSep;
        for (int i2 = 0; i2 < this.mProvidersByClassPerUser.size(); i2++) {
            HashMap<ComponentName, ContentProviderRecord> map = (HashMap) this.mProvidersByClassPerUser.valueAt(i2);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("  Published user ");
            stringBuilder.append(this.mProvidersByClassPerUser.keyAt(i2));
            stringBuilder.append(" content providers (by class):");
            needSep2 |= dumpProvidersByClassLocked(pw, dumpAll, dumpPackage, stringBuilder.toString(), needSep2, map);
        }
        if (!dumpAll) {
            return needSep2;
        }
        needSep = dumpProvidersByNameLocked(pw, dumpPackage, "  Single-user authority to provider mappings:", needSep2, this.mSingletonByName) | needSep2;
        while (i < this.mProvidersByNamePerUser.size()) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("  User ");
            stringBuilder2.append(this.mProvidersByNamePerUser.keyAt(i));
            stringBuilder2.append(" authority to provider mappings:");
            needSep |= dumpProvidersByNameLocked(pw, dumpPackage, stringBuilder2.toString(), needSep, (HashMap) this.mProvidersByNamePerUser.valueAt(i));
            i++;
        }
        return needSep;
    }

    private ArrayList<ContentProviderRecord> getProvidersForName(String name) {
        ArrayList<ContentProviderRecord> allProviders = new ArrayList();
        ArrayList<ContentProviderRecord> ret = new ArrayList();
        Predicate<ContentProviderRecord> filter = DumpUtils.filterRecord(name);
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                allProviders.addAll(this.mSingletonByClass.values());
                for (int i = 0; i < this.mProvidersByClassPerUser.size(); i++) {
                    allProviders.addAll(((HashMap) this.mProvidersByClassPerUser.valueAt(i)).values());
                }
                CollectionUtils.addIf(allProviders, ret, filter);
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
        ret.sort(Comparator.comparing(-$$Lambda$HKoBBTwYfMTyX1rzuzxIXu0s2cc.INSTANCE));
        return ret;
    }

    protected boolean dumpProvider(FileDescriptor fd, PrintWriter pw, String name, String[] args, int opti, boolean dumpAll) {
        ArrayList<ContentProviderRecord> providers = getProvidersForName(name);
        int i = 0;
        if (providers.size() <= 0) {
            return false;
        }
        boolean needSep = false;
        while (true) {
            int i2 = i;
            if (i2 >= providers.size()) {
                return true;
            }
            if (needSep) {
                pw.println();
            }
            dumpProvider(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS, fd, pw, (ContentProviderRecord) providers.get(i2), args, dumpAll);
            i = i2 + 1;
            needSep = true;
        }
    }

    private void dumpProvider(String prefix, FileDescriptor fd, PrintWriter pw, ContentProviderRecord r, String[] args, boolean dumpAll) {
        int length = args.length;
        int i = 0;
        while (i < length) {
            String s = args[i];
            if (dumpAll || !s.contains(PriorityDump.PROTO_ARG)) {
                i++;
            } else {
                if (!(r.proc == null || r.proc.thread == null)) {
                    dumpToTransferPipe(null, fd, pw, r, args);
                }
                return;
            }
        }
        String innerPrefix = new StringBuilder();
        innerPrefix.append(prefix);
        innerPrefix.append("  ");
        innerPrefix = innerPrefix.toString();
        synchronized (this.mAm) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                pw.print(prefix);
                pw.print("PROVIDER ");
                pw.print(r);
                pw.print(" pid=");
                if (r.proc != null) {
                    pw.println(r.proc.pid);
                } else {
                    pw.println("(not running)");
                }
                if (dumpAll) {
                    r.dump(pw, innerPrefix, true);
                }
            } finally {
                while (true) {
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
            }
        }
        if (!(r.proc == null || r.proc.thread == null)) {
            pw.println("    Client:");
            pw.flush();
            dumpToTransferPipe("      ", fd, pw, r, args);
        }
    }

    protected boolean dumpProviderProto(FileDescriptor fd, PrintWriter pw, String name, String[] args) {
        String[] newArgs = (String[]) Arrays.copyOf(args, args.length + 1);
        newArgs[args.length] = PriorityDump.PROTO_ARG;
        ArrayList<ContentProviderRecord> providers = getProvidersForName(name);
        if (providers.size() <= 0) {
            return false;
        }
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= providers.size()) {
                return false;
            }
            ContentProviderRecord r = (ContentProviderRecord) providers.get(i2);
            if (r.proc == null || r.proc.thread == null) {
                i = i2 + 1;
            } else {
                dumpToTransferPipe(null, fd, pw, r, newArgs);
                return true;
            }
        }
    }

    private void dumpToTransferPipe(String prefix, FileDescriptor fd, PrintWriter pw, ContentProviderRecord r, String[] args) {
        TransferPipe tp;
        try {
            tp = new TransferPipe();
            r.proc.thread.dumpProvider(tp.getWriteFd(), r.provider.asBinder(), args);
            tp.setBufferPrefix(prefix);
            tp.go(fd, 2000);
            tp.kill();
        } catch (IOException ex) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("      Failure while dumping the provider: ");
            stringBuilder.append(ex);
            pw.println(stringBuilder.toString());
        } catch (RemoteException e) {
            pw.println("      Got a RemoteException while dumping the service");
        } catch (Throwable th) {
            tp.kill();
        }
    }
}
