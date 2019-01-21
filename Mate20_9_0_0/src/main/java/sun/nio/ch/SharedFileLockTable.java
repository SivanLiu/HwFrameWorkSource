package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.channels.Channel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/* compiled from: FileLockTable */
class SharedFileLockTable extends FileLockTable {
    static final /* synthetic */ boolean $assertionsDisabled = false;
    private static ConcurrentHashMap<FileKey, List<FileLockReference>> lockMap = new ConcurrentHashMap();
    private static ReferenceQueue<FileLock> queue = new ReferenceQueue();
    private final Channel channel;
    private final FileKey fileKey;

    /* compiled from: FileLockTable */
    private static class FileLockReference extends WeakReference<FileLock> {
        private FileKey fileKey;

        FileLockReference(FileLock referent, ReferenceQueue<FileLock> queue, FileKey key) {
            super(referent, queue);
            this.fileKey = key;
        }

        FileKey fileKey() {
            return this.fileKey;
        }
    }

    SharedFileLockTable(Channel channel, FileDescriptor fd) throws IOException {
        this.channel = channel;
        this.fileKey = FileKey.create(fd);
    }

    public void add(FileLock fl) throws OverlappingFileLockException {
        Throwable th;
        List<FileLockReference> list = (List) lockMap.get(this.fileKey);
        while (true) {
            List<FileLockReference> list2;
            if (list == null) {
                list2 = new ArrayList(2);
                synchronized (list2) {
                    list = (List) lockMap.putIfAbsent(this.fileKey, list2);
                    if (list == null) {
                        list2.add(new FileLockReference(fl, queue, this.fileKey));
                        break;
                    }
                }
            }
            synchronized (list) {
                try {
                    list2 = (List) lockMap.get(this.fileKey);
                    if (list == list2) {
                        checkList(list, fl.position(), fl.size());
                        list.add(new FileLockReference(fl, queue, this.fileKey));
                    } else {
                        try {
                        } catch (Throwable th2) {
                            Throwable th3 = th2;
                            List<FileLockReference> list3 = list2;
                            th = th3;
                            while (true) {
                                try {
                                    break;
                                } catch (Throwable th4) {
                                    th = th4;
                                }
                            }
                            throw th;
                        }
                    }
                } catch (Throwable th5) {
                    th = th5;
                    while (true) {
                        break;
                    }
                    throw th;
                }
            }
            list = list2;
        }
        removeStaleEntries();
    }

    private void removeKeyIfEmpty(FileKey fk, List<FileLockReference> list) {
        if (list.isEmpty()) {
            lockMap.remove(fk);
        }
    }

    public void remove(FileLock fl) {
        List<FileLockReference> list = (List) lockMap.get(this.fileKey);
        if (list != null) {
            synchronized (list) {
                for (int index = 0; index < list.size(); index++) {
                    FileLockReference ref = (FileLockReference) list.get(index);
                    if (((FileLock) ref.get()) == fl) {
                        ref.clear();
                        list.remove(index);
                        break;
                    }
                }
            }
        }
    }

    public List<FileLock> removeAll() {
        List<FileLock> result = new ArrayList();
        List<FileLockReference> list = (List) lockMap.get(this.fileKey);
        if (list != null) {
            synchronized (list) {
                int index = 0;
                while (index < list.size()) {
                    FileLockReference ref = (FileLockReference) list.get(index);
                    FileLock lock = (FileLock) ref.get();
                    if (lock == null || lock.acquiredBy() != this.channel) {
                        index++;
                    } else {
                        ref.clear();
                        list.remove(index);
                        result.add(lock);
                    }
                }
                removeKeyIfEmpty(this.fileKey, list);
            }
        }
        return result;
    }

    public void replace(FileLock fromLock, FileLock toLock) {
        List<FileLockReference> list = (List) lockMap.get(this.fileKey);
        synchronized (list) {
            for (int index = 0; index < list.size(); index++) {
                FileLockReference ref = (FileLockReference) list.get(index);
                if (((FileLock) ref.get()) == fromLock) {
                    ref.clear();
                    list.set(index, new FileLockReference(toLock, queue, this.fileKey));
                    break;
                }
            }
        }
    }

    private void checkList(List<FileLockReference> list, long position, long size) throws OverlappingFileLockException {
        for (FileLockReference ref : list) {
            FileLock fl = (FileLock) ref.get();
            if (fl != null && fl.overlaps(position, size)) {
                throw new OverlappingFileLockException();
            }
        }
    }

    private void removeStaleEntries() {
        while (true) {
            FileLockReference fileLockReference = (FileLockReference) queue.poll();
            Object ref = fileLockReference;
            if (fileLockReference != null) {
                FileKey fk = ref.fileKey();
                List<FileLockReference> list = (List) lockMap.get(fk);
                if (list != null) {
                    synchronized (list) {
                        list.remove(ref);
                        removeKeyIfEmpty(fk, list);
                    }
                }
            } else {
                return;
            }
        }
    }
}
