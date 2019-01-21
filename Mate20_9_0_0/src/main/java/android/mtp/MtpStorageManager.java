package android.mtp;

import android.media.MediaFile;
import android.os.FileObserver;
import android.os.storage.StorageVolume;
import android.util.Log;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;

public class MtpStorageManager {
    private static final int IN_IGNORED = 32768;
    private static final int IN_ISDIR = 1073741824;
    private static final int IN_ONLYDIR = 16777216;
    private static final int IN_Q_OVERFLOW = 16384;
    private static final String TAG = MtpStorageManager.class.getSimpleName();
    public static final boolean sDebug = false;
    private volatile boolean mCheckConsistency = false;
    private Thread mConsistencyThread = new Thread(new -$$Lambda$MtpStorageManager$HocvlaKIXOtuA3p8uOP9PA7UJtw(this));
    private MtpNotifier mMtpNotifier;
    private int mNextObjectId = 1;
    private int mNextStorageId = 1;
    private HashMap<Integer, MtpObject> mObjects = new HashMap();
    private HashMap<Integer, MtpObject> mRoots = new HashMap();
    private Set<String> mSubdirectories;

    public static abstract class MtpNotifier {
        public abstract void sendObjectAdded(int i);

        public abstract void sendObjectRemoved(int i);
    }

    public static class MtpObject {
        private HashMap<String, MtpObject> mChildren;
        private int mId;
        private boolean mIsDir;
        private String mName;
        private FileObserver mObserver = null;
        private MtpOperation mOp;
        private MtpObject mParent;
        private MtpObjectState mState = MtpObjectState.NORMAL;
        private int mStorageId;
        private boolean mVisited = false;

        MtpObject(String name, int id, MtpObject parent, boolean isDir) {
            this.mId = id;
            this.mName = name;
            this.mParent = parent;
            HashMap hashMap = null;
            this.mIsDir = isDir;
            this.mOp = MtpOperation.NONE;
            if (this.mIsDir) {
                hashMap = new HashMap();
            }
            this.mChildren = hashMap;
            if (parent != null) {
                this.mStorageId = parent.getStorageId();
            } else {
                this.mStorageId = id;
            }
        }

        public void setStorageId(int id) {
            this.mStorageId = id;
        }

        public String getName() {
            return this.mName;
        }

        public int getId() {
            return this.mId;
        }

        public boolean isDir() {
            return this.mIsDir;
        }

        public int getFormat() {
            return this.mIsDir ? MtpConstants.FORMAT_ASSOCIATION : MediaFile.getFormatCode(this.mName, null);
        }

        public int getStorageId() {
            return this.mStorageId;
        }

        public long getModifiedTime() {
            return getPath().toFile().lastModified() / 1000;
        }

        public MtpObject getParent() {
            return this.mParent;
        }

        public MtpObject getRoot() {
            return isRoot() ? this : this.mParent.getRoot();
        }

        public long getSize() {
            return this.mIsDir ? 0 : getPath().toFile().length();
        }

        public Path getPath() {
            return isRoot() ? Paths.get(this.mName, new String[0]) : this.mParent.getPath().resolve(this.mName);
        }

        public boolean isRoot() {
            return this.mParent == null;
        }

        private void setName(String name) {
            this.mName = name;
        }

        private void setId(int id) {
            this.mId = id;
        }

        private boolean isVisited() {
            return this.mVisited;
        }

        private void setParent(MtpObject parent) {
            this.mParent = parent;
            if (parent != null) {
                this.mStorageId = parent.getStorageId();
            } else {
                this.mStorageId = this.mId;
            }
        }

        private void setDir(boolean dir) {
            if (dir != this.mIsDir) {
                this.mIsDir = dir;
                this.mChildren = this.mIsDir ? new HashMap() : null;
            }
        }

        private void setVisited(boolean visited) {
            this.mVisited = visited;
        }

        private MtpObjectState getState() {
            return this.mState;
        }

        private void setState(MtpObjectState state) {
            this.mState = state;
            if (this.mState == MtpObjectState.NORMAL) {
                this.mOp = MtpOperation.NONE;
            }
        }

        private MtpOperation getOperation() {
            return this.mOp;
        }

        private void setOperation(MtpOperation op) {
            this.mOp = op;
        }

        private FileObserver getObserver() {
            return this.mObserver;
        }

        private void setObserver(FileObserver observer) {
            this.mObserver = observer;
        }

        private void addChild(MtpObject child) {
            this.mChildren.put(child.getName(), child);
        }

        private MtpObject getChild(String name) {
            return (MtpObject) this.mChildren.get(name);
        }

        private Collection<MtpObject> getChildren() {
            return this.mChildren.values();
        }

        private boolean exists() {
            return getPath().toFile().exists();
        }

        private MtpObject copy(boolean recursive) {
            MtpObject copy = new MtpObject(this.mName, this.mId, this.mParent, this.mIsDir);
            copy.mIsDir = this.mIsDir;
            copy.mVisited = this.mVisited;
            copy.mState = this.mState;
            copy.mChildren = this.mIsDir ? new HashMap() : null;
            if (recursive && this.mIsDir) {
                for (MtpObject child : this.mChildren.values()) {
                    MtpObject childCopy = child.copy(true);
                    childCopy.setParent(copy);
                    copy.addChild(childCopy);
                }
            }
            return copy;
        }
    }

    private enum MtpObjectState {
        NORMAL,
        FROZEN,
        FROZEN_ADDED,
        FROZEN_REMOVED,
        FROZEN_ONESHOT_ADD,
        FROZEN_ONESHOT_DEL
    }

    private enum MtpOperation {
        NONE,
        ADD,
        RENAME,
        COPY,
        DELETE
    }

    private class MtpObjectObserver extends FileObserver {
        MtpObject mObject;

        MtpObjectObserver(MtpObject object) {
            super(object.getPath().toString(), 16778184);
            this.mObject = object;
        }

        /* JADX WARNING: Missing block: B:38:0x00a1, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onEvent(int event, String path) {
            synchronized (MtpStorageManager.this) {
                if ((event & 16384) != 0) {
                    try {
                        Log.e(MtpStorageManager.TAG, "Received Inotify overflow event!");
                    } catch (Throwable th) {
                    }
                }
                MtpObject obj = this.mObject.getChild(path);
                if ((event & 128) == 0 && (event & 256) == 0) {
                    if ((event & 8) == 0) {
                        String access$000;
                        StringBuilder stringBuilder;
                        if ((event & 64) == 0) {
                            if ((event & 512) == 0) {
                                if ((32768 & event) != 0) {
                                    if (this.mObject.mObserver != null) {
                                        this.mObject.mObserver.stopWatching();
                                    }
                                    this.mObject.mObserver = null;
                                } else {
                                    access$000 = MtpStorageManager.TAG;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Got unrecognized event ");
                                    stringBuilder.append(path);
                                    stringBuilder.append(" ");
                                    stringBuilder.append(event);
                                    Log.w(access$000, stringBuilder.toString());
                                }
                            }
                        }
                        if (obj == null) {
                            access$000 = MtpStorageManager.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Object was null in event ");
                            stringBuilder.append(path);
                            Log.w(access$000, stringBuilder.toString());
                            return;
                        }
                        MtpStorageManager.this.handleRemovedObject(obj);
                    }
                }
                MtpStorageManager.this.handleAddedObject(this.mObject, path, (1073741824 & event) != 0);
            }
        }

        public void finalize() {
        }
    }

    public MtpStorageManager(MtpNotifier notifier, Set<String> subdirectories) {
        this.mMtpNotifier = notifier;
        this.mSubdirectories = subdirectories;
        if (this.mCheckConsistency) {
            this.mConsistencyThread.start();
        }
    }

    public static /* synthetic */ void lambda$new$0(MtpStorageManager mtpStorageManager) {
        while (mtpStorageManager.mCheckConsistency) {
            try {
                Thread.sleep(15000);
                if (mtpStorageManager.checkConsistency()) {
                    Log.v(TAG, "Cache is consistent");
                } else {
                    Log.w(TAG, "Cache is not consistent");
                }
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    public synchronized void close() {
        for (MtpObject obj : Stream.concat(this.mRoots.values().stream(), this.mObjects.values().stream())) {
            if (obj.getObserver() != null) {
                obj.getObserver().stopWatching();
                obj.setObserver(null);
            }
        }
        if (this.mCheckConsistency) {
            this.mCheckConsistency = false;
            this.mConsistencyThread.interrupt();
            try {
                this.mConsistencyThread.join();
            } catch (InterruptedException e) {
            }
        }
    }

    public synchronized void setSubdirectories(Set<String> subDirs) {
        this.mSubdirectories = subDirs;
    }

    public synchronized MtpStorage addMtpStorage(StorageVolume volume) {
        MtpStorage storage;
        int storageId = ((getNextStorageId() & 65535) << 16) + 1;
        storage = new MtpStorage(volume, storageId);
        this.mRoots.put(Integer.valueOf(storageId), new MtpObject(storage.getPath(), storageId, null, true));
        return storage;
    }

    public synchronized MtpStorage addMtpStorage(StorageVolume volume, int storageId) {
        MtpStorage storage;
        MtpObject root = new MtpObject(volume.getPath(), storageId, null, true);
        storage = new MtpStorage(volume, storageId);
        this.mRoots.put(Integer.valueOf(storageId), root);
        return storage;
    }

    public synchronized void removeMtpStorage(MtpStorage storage) {
        removeObjectFromCache(getStorageRoot(storage.getStorageId()), true, true);
    }

    private synchronized boolean isSpecialSubDir(MtpObject obj) {
        boolean z;
        z = (!obj.getParent().isRoot() || this.mSubdirectories == null || this.mSubdirectories.contains(obj.getName())) ? false : true;
        return z;
    }

    public synchronized MtpObject getByPath(String path) {
        MtpObject obj = null;
        for (MtpObject root : this.mRoots.values()) {
            if (path.startsWith(root.getName())) {
                obj = root;
                path = path.substring(root.getName().length());
            }
        }
        String[] split = path.split("/");
        int length = split.length;
        int i = 0;
        while (i < length) {
            String name = split[i];
            if (obj != null) {
                if (obj.isDir()) {
                    if (!"".equals(name)) {
                        if (!obj.isVisited()) {
                            getChildren(obj);
                        }
                        obj = obj.getChild(name);
                    }
                    i++;
                }
            }
            return null;
        }
        return obj;
    }

    public synchronized MtpObject getObject(int id) {
        if (id == 0 || id == -1) {
            Log.w(TAG, "Can't get root storages with getObject()");
            return null;
        } else if (this.mObjects.containsKey(Integer.valueOf(id))) {
            return (MtpObject) this.mObjects.get(Integer.valueOf(id));
        } else {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Id ");
            stringBuilder.append(id);
            stringBuilder.append(" doesn't exist");
            Log.w(str, stringBuilder.toString());
            return null;
        }
    }

    public MtpObject getStorageRoot(int id) {
        if (this.mRoots.containsKey(Integer.valueOf(id))) {
            return (MtpObject) this.mRoots.get(Integer.valueOf(id));
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("StorageId ");
        stringBuilder.append(id);
        stringBuilder.append(" doesn't exist");
        Log.w(str, stringBuilder.toString());
        return null;
    }

    private int getNextObjectId() {
        int ret = this.mNextObjectId;
        this.mNextObjectId = (int) (((long) this.mNextObjectId) + 1);
        return ret;
    }

    private int getNextStorageId() {
        int i = this.mNextStorageId;
        this.mNextStorageId = i + 1;
        return i;
    }

    public synchronized Stream<MtpObject> getObjects(int parent, int format, int storageId) {
        boolean recursive = parent == 0;
        if (parent == -1) {
            parent = 0;
        }
        if (storageId == -1 && parent == 0) {
            ArrayList<Stream<MtpObject>> streamList = new ArrayList();
            for (MtpObject root : this.mRoots.values()) {
                streamList.add(getObjects(root, format, recursive));
            }
            return (Stream) Stream.of(streamList).flatMap(-$$Lambda$JdUL9ZP9AzcttUlxZCHq6-pfTzU.INSTANCE).reduce(-$$Lambda$MtpStorageManager$QdR1YPNkK9RX4bISfNvQAOnGxGE.INSTANCE).orElseGet(-$$Lambda$MtpStorageManager$TsWypJRYDhxg01Bfs_tm2d_H9zU.INSTANCE);
        }
        MtpObject obj = parent == 0 ? getStorageRoot(storageId) : getObject(parent);
        if (obj == null) {
            return null;
        }
        return getObjects(obj, format, recursive);
    }

    /* JADX WARNING: Missing block: B:22:0x0069, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized Stream<MtpObject> getObjects(MtpObject parent, int format, boolean rec) {
        Collection<MtpObject> children = getChildren(parent);
        if (children == null) {
            return null;
        }
        Stream<MtpObject> ret = Stream.of(children).flatMap(-$$Lambda$72U6ffwsZ0Sm2BXYilXSg7hTsO8.INSTANCE);
        if (format != 0) {
            ret = ret.filter(new -$$Lambda$MtpStorageManager$DVPwWM5hkC_B_cgO9AF8IKzObmQ(format));
        }
        if (rec) {
            ArrayList<Stream<MtpObject>> streamList = new ArrayList();
            streamList.add(ret);
            for (MtpObject o : children) {
                if (o.isDir()) {
                    streamList.add(getObjects(o, format, true));
                }
            }
            ret = (Stream) Stream.of(streamList).filter(-$$Lambda$MtpStorageManager$ZX5EBcSdO0MZYnMFDwTJpRFAOd0.INSTANCE).flatMap(-$$Lambda$JdUL9ZP9AzcttUlxZCHq6-pfTzU.INSTANCE).reduce(-$$Lambda$MtpStorageManager$QdR1YPNkK9RX4bISfNvQAOnGxGE.INSTANCE).orElseGet(-$$Lambda$MtpStorageManager$TsWypJRYDhxg01Bfs_tm2d_H9zU.INSTANCE);
        }
    }

    static /* synthetic */ boolean lambda$getObjects$1(int format, MtpObject o) {
        return o.getFormat() == format;
    }

    private synchronized Collection<MtpObject> getChildren(MtpObject object) {
        DirectoryStream<Path> stream;
        Throwable th;
        Throwable th2;
        if (object != null) {
            if (object.isDir()) {
                if (!object.isVisited()) {
                    Path dir = object.getPath();
                    if (object.getObserver() != null) {
                        Log.e(TAG, "Observer is not null!");
                    }
                    object.setObserver(new MtpObjectObserver(object));
                    object.getObserver().startWatching();
                    try {
                        stream = Files.newDirectoryStream(dir);
                        try {
                            for (Path file : stream) {
                                addObjectToCache(object, file.getFileName().toString(), file.toFile().isDirectory());
                            }
                            if (stream != null) {
                                $closeResource(null, stream);
                            }
                            object.setVisited(true);
                        } catch (Throwable th22) {
                            Throwable th3 = th22;
                            th22 = th;
                            th = th3;
                        }
                    } catch (IOException | DirectoryIteratorException e) {
                        Log.e(TAG, e.toString());
                        object.getObserver().stopWatching();
                        object.setObserver(null);
                        return null;
                    }
                }
                return object.getChildren();
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Can't find children of ");
        stringBuilder.append(object == null ? "null" : Integer.valueOf(object.getId()));
        Log.w(str, stringBuilder.toString());
        return null;
        if (stream != null) {
            $closeResource(th22, stream);
        }
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

    private synchronized MtpObject addObjectToCache(MtpObject parent, String newName, boolean isDir) {
        if (!parent.isRoot() && getObject(parent.getId()) != parent) {
            return null;
        }
        if (parent.getChild(newName) != null) {
            return null;
        }
        if (this.mSubdirectories != null && parent.isRoot() && !this.mSubdirectories.contains(newName)) {
            return null;
        }
        MtpObject obj = new MtpObject(newName, getNextObjectId(), parent, isDir);
        this.mObjects.put(Integer.valueOf(obj.getId()), obj);
        parent.addChild(obj);
        return obj;
    }

    /* JADX WARNING: Removed duplicated region for block: B:17:0x003d  */
    /* JADX WARNING: Removed duplicated region for block: B:11:0x0026  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x005c  */
    /* JADX WARNING: Removed duplicated region for block: B:33:0x0082  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized boolean removeObjectFromCache(MtpObject removed, boolean removeGlobal, boolean recursive) {
        boolean ret;
        if (!removed.isRoot()) {
            if (!removed.getParent().mChildren.remove(removed.getName(), removed)) {
                ret = false;
                boolean z;
                if (!removed.isRoot()) {
                    z = this.mRoots.remove(Integer.valueOf(removed.getId()), removed) && ret;
                    ret = z;
                } else if (removeGlobal) {
                    z = this.mObjects.remove(Integer.valueOf(removed.getId()), removed) && ret;
                    ret = z;
                }
                if (removed.getObserver() != null) {
                    removed.getObserver().stopWatching();
                    removed.setObserver(null);
                }
                if (removed.isDir() && recursive) {
                    for (MtpObject child : new ArrayList(removed.getChildren())) {
                        boolean z2 = removeObjectFromCache(child, removeGlobal, true) && ret;
                        ret = z2;
                    }
                }
            }
        }
        ret = true;
        if (!removed.isRoot()) {
        }
        if (removed.getObserver() != null) {
        }
        while (r4.hasNext()) {
        }
        return ret;
    }

    private synchronized void handleAddedObject(MtpObject parent, String path, boolean isDir) {
        DirectoryStream<Path> stream;
        Throwable th;
        Throwable th2;
        MtpOperation op = MtpOperation.NONE;
        MtpObject obj = parent.getChild(path);
        if (obj != null) {
            String str;
            StringBuilder stringBuilder;
            MtpObjectState state = obj.getState();
            op = obj.getOperation();
            if (!(obj.isDir() == isDir || state == MtpObjectState.FROZEN_REMOVED)) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Inconsistent directory info! ");
                stringBuilder.append(obj.getPath());
                Log.d(str, stringBuilder.toString());
            }
            obj.setDir(isDir);
            switch (state) {
                case FROZEN:
                case FROZEN_REMOVED:
                    obj.setState(MtpObjectState.FROZEN_ADDED);
                    break;
                case FROZEN_ONESHOT_ADD:
                    obj.setState(MtpObjectState.NORMAL);
                    this.mMtpNotifier.sendObjectAdded(obj.getId());
                    break;
                case NORMAL:
                case FROZEN_ADDED:
                    return;
                default:
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Unexpected state in add ");
                    stringBuilder.append(path);
                    stringBuilder.append(" ");
                    stringBuilder.append(state);
                    Log.w(str, stringBuilder.toString());
                    break;
            }
        }
        obj = addObjectToCache(parent, path, isDir);
        if (obj != null) {
            if (!isDir) {
                if (obj.getSize() == 0) {
                    obj.setState(MtpObjectState.FROZEN_ONESHOT_ADD);
                }
            }
            this.mMtpNotifier.sendObjectAdded(obj.getId());
        } else {
            return;
        }
        if (isDir) {
            if (op != MtpOperation.RENAME) {
                if (op == MtpOperation.COPY && !obj.isVisited()) {
                    return;
                }
                if (obj.getObserver() != null) {
                    Log.e(TAG, "Observer is not null!");
                    return;
                }
                obj.setObserver(new MtpObjectObserver(obj));
                obj.getObserver().startWatching();
                obj.setVisited(true);
                try {
                    stream = Files.newDirectoryStream(obj.getPath());
                    try {
                        for (Path file : stream) {
                            handleAddedObject(obj, file.getFileName().toString(), file.toFile().isDirectory());
                        }
                        if (stream != null) {
                            $closeResource(null, stream);
                        }
                    } catch (Throwable th22) {
                        Throwable th3 = th22;
                        th22 = th;
                        th = th3;
                    }
                } catch (IOException | DirectoryIteratorException e) {
                    Log.e(TAG, e.toString());
                    obj.getObserver().stopWatching();
                    obj.setObserver(null);
                }
            } else {
                return;
            }
        }
        if (stream != null) {
            $closeResource(th22, stream);
        }
        throw th;
        return;
    }

    private synchronized void handleRemovedObject(MtpObject obj) {
        MtpObjectState state = obj.getState();
        MtpOperation op = obj.getOperation();
        int i = AnonymousClass1.$SwitchMap$android$mtp$MtpStorageManager$MtpObjectState[state.ordinal()];
        boolean z = true;
        if (i != 1) {
            switch (i) {
                case 4:
                    if (removeObjectFromCache(obj, true, true)) {
                        this.mMtpNotifier.sendObjectRemoved(obj.getId());
                        break;
                    }
                    break;
                case 5:
                    obj.setState(MtpObjectState.FROZEN_REMOVED);
                    break;
                case 6:
                    if (op == MtpOperation.RENAME) {
                        z = false;
                    }
                    removeObjectFromCache(obj, z, false);
                    break;
                default:
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Got unexpected object remove for ");
                    stringBuilder.append(obj.getName());
                    Log.e(str, stringBuilder.toString());
                    break;
            }
        }
        obj.setState(MtpObjectState.FROZEN_REMOVED);
    }

    public void flushEvents() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
        }
    }

    public synchronized void dump() {
        for (Integer key : this.mObjects.keySet()) {
            int key2 = key.intValue();
            MtpObject obj = (MtpObject) this.mObjects.get(Integer.valueOf(key2));
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(key2);
            stringBuilder.append(" | ");
            stringBuilder.append(obj.getParent() == null ? Integer.valueOf(obj.getParent().getId()) : "null");
            stringBuilder.append(" | ");
            stringBuilder.append(obj.getName());
            stringBuilder.append(" | ");
            stringBuilder.append(obj.isDir() ? "dir" : "obj");
            stringBuilder.append(" | ");
            stringBuilder.append(obj.isVisited() ? "v" : "nv");
            stringBuilder.append(" | ");
            stringBuilder.append(obj.getState());
            Log.i(str, stringBuilder.toString());
        }
    }

    public synchronized boolean checkConsistency() {
        boolean ret;
        ret = true;
        for (MtpObject obj : Stream.concat(this.mRoots.values().stream(), this.mObjects.values().stream())) {
            String str;
            StringBuilder stringBuilder;
            if (!obj.exists()) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Object doesn't exist ");
                stringBuilder.append(obj.getPath());
                stringBuilder.append(" ");
                stringBuilder.append(obj.getId());
                Log.w(str, stringBuilder.toString());
                ret = false;
            }
            if (obj.getState() != MtpObjectState.NORMAL) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Object ");
                stringBuilder.append(obj.getPath());
                stringBuilder.append(" in state ");
                stringBuilder.append(obj.getState());
                Log.w(str, stringBuilder.toString());
                ret = false;
            }
            if (obj.getOperation() != MtpOperation.NONE) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Object ");
                stringBuilder.append(obj.getPath());
                stringBuilder.append(" in operation ");
                stringBuilder.append(obj.getOperation());
                Log.w(str, stringBuilder.toString());
                ret = false;
            }
            if (!(obj.isRoot() || this.mObjects.get(Integer.valueOf(obj.getId())) == obj)) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Object ");
                stringBuilder.append(obj.getPath());
                stringBuilder.append(" is not in map correctly");
                Log.w(str, stringBuilder.toString());
                ret = false;
            }
            if (obj.getParent() != null) {
                if (obj.getParent().isRoot() && obj.getParent() != this.mRoots.get(Integer.valueOf(obj.getParent().getId()))) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Root parent is not in root mapping ");
                    stringBuilder.append(obj.getPath());
                    Log.w(str, stringBuilder.toString());
                    ret = false;
                }
                if (!(obj.getParent().isRoot() || obj.getParent() == this.mObjects.get(Integer.valueOf(obj.getParent().getId())))) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Parent is not in object mapping ");
                    stringBuilder.append(obj.getPath());
                    Log.w(str, stringBuilder.toString());
                    ret = false;
                }
                if (obj.getParent().getChild(obj.getName()) != obj) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Child does not exist in parent ");
                    stringBuilder.append(obj.getPath());
                    Log.w(str, stringBuilder.toString());
                    ret = false;
                }
            }
            if (obj.isDir()) {
                if (obj.isVisited() == (obj.getObserver() == null)) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(obj.getPath());
                    stringBuilder.append(" is ");
                    stringBuilder.append(obj.isVisited() ? "" : "not ");
                    stringBuilder.append(" visited but observer is ");
                    stringBuilder.append(obj.getObserver());
                    Log.w(str, stringBuilder.toString());
                    ret = false;
                }
                if (!obj.isVisited() && obj.getChildren().size() > 0) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(obj.getPath());
                    stringBuilder.append(" is not visited but has children");
                    Log.w(str, stringBuilder.toString());
                    ret = false;
                }
                DirectoryStream<Path> stream;
                try {
                    String str2;
                    StringBuilder stringBuilder2;
                    stream = Files.newDirectoryStream(obj.getPath());
                    Set<String> files = new HashSet();
                    for (Path file : stream) {
                        if (obj.isVisited() && obj.getChild(file.getFileName().toString()) == null && (this.mSubdirectories == null || !obj.isRoot() || this.mSubdirectories.contains(file.getFileName().toString()))) {
                            str2 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("File exists in fs but not in children ");
                            stringBuilder2.append(file);
                            Log.w(str2, stringBuilder2.toString());
                            ret = false;
                        }
                        files.add(file.toString());
                    }
                    for (MtpObject child : obj.getChildren()) {
                        if (!files.contains(child.getPath().toString())) {
                            str2 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("File in children doesn't exist in fs ");
                            stringBuilder2.append(child.getPath());
                            Log.w(str2, stringBuilder2.toString());
                            ret = false;
                        }
                        if (child != this.mObjects.get(Integer.valueOf(child.getId()))) {
                            str2 = TAG;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Child is not in object map ");
                            stringBuilder2.append(child.getPath());
                            Log.w(str2, stringBuilder2.toString());
                            ret = false;
                        }
                    }
                    if (stream != null) {
                        $closeResource(null, stream);
                    }
                } catch (IOException | DirectoryIteratorException e) {
                    Log.w(TAG, e.toString());
                    ret = false;
                } catch (Throwable th) {
                    if (stream != null) {
                        $closeResource(r6, stream);
                    }
                }
            }
        }
        return ret;
    }

    public synchronized int beginSendObject(MtpObject parent, String name, int format) {
        if (!parent.isDir()) {
            return -1;
        }
        if (parent.isRoot() && this.mSubdirectories != null && !this.mSubdirectories.contains(name)) {
            return -1;
        }
        getChildren(parent);
        MtpObject obj = addObjectToCache(parent, name, format == MtpConstants.FORMAT_ASSOCIATION ? true : null);
        if (obj == null) {
            return -1;
        }
        obj.setState(MtpObjectState.FROZEN);
        obj.setOperation(MtpOperation.ADD);
        return obj.getId();
    }

    public synchronized boolean endSendObject(MtpObject obj, boolean succeeded) {
        return generalEndAddObject(obj, succeeded, true);
    }

    public synchronized boolean beginRenameObject(MtpObject obj, String newName) {
        if (obj.isRoot()) {
            return false;
        }
        if (isSpecialSubDir(obj)) {
            return false;
        }
        if (obj.getParent().getChild(newName) != null) {
            return false;
        }
        MtpObject oldObj = obj.copy(false);
        obj.setName(newName);
        obj.getParent().addChild(obj);
        oldObj.getParent().addChild(oldObj);
        return generalBeginRenameObject(oldObj, obj);
    }

    public synchronized boolean endRenameObject(MtpObject obj, String oldName, boolean success) {
        MtpObject oldObj;
        MtpObject parent = obj.getParent();
        oldObj = parent.getChild(oldName);
        if (!success) {
            MtpObject temp = oldObj;
            MtpObjectState oldState = oldObj.getState();
            temp.setName(obj.getName());
            temp.setState(obj.getState());
            oldObj = obj;
            oldObj.setName(oldName);
            oldObj.setState(oldState);
            obj = temp;
            parent.addChild(obj);
            parent.addChild(oldObj);
        }
        return generalEndRenameObject(oldObj, obj, success);
    }

    public synchronized boolean beginRemoveObject(MtpObject obj) {
        boolean z;
        z = (obj.isRoot() || isSpecialSubDir(obj) || !generalBeginRemoveObject(obj, MtpOperation.DELETE)) ? false : true;
        return z;
    }

    public synchronized boolean endRemoveObject(MtpObject obj, boolean success) {
        boolean z;
        boolean ret = true;
        z = false;
        if (obj.isDir()) {
            Iterator it = new ArrayList(obj.getChildren()).iterator();
            while (it.hasNext()) {
                MtpObject child = (MtpObject) it.next();
                if (child.getOperation() == MtpOperation.DELETE) {
                    boolean z2 = endRemoveObject(child, success) && ret;
                    ret = z2;
                }
            }
        }
        if (generalEndRemoveObject(obj, success, true) && ret) {
            z = true;
        }
        return z;
    }

    /* JADX WARNING: Missing block: B:26:0x0047, code skipped:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean beginMoveObject(MtpObject obj, MtpObject newParent) {
        if (obj.isRoot()) {
            return false;
        }
        if (isSpecialSubDir(obj)) {
            return false;
        }
        getChildren(newParent);
        if (newParent.getChild(obj.getName()) != null) {
            return false;
        }
        if (obj.getStorageId() != newParent.getStorageId()) {
            boolean z = true;
            MtpObject newObj = obj.copy(true);
            newObj.setParent(newParent);
            newParent.addChild(newObj);
            if (!(generalBeginRemoveObject(obj, MtpOperation.RENAME) && generalBeginCopyObject(newObj, false))) {
                z = false;
            }
        } else {
            MtpObject oldObj = obj.copy(false);
            obj.setParent(newParent);
            oldObj.getParent().addChild(oldObj);
            obj.getParent().addChild(obj);
            return generalBeginRenameObject(oldObj, obj);
        }
    }

    /* JADX WARNING: Missing block: B:13:0x0029, code skipped:
            return r2;
     */
    /* JADX WARNING: Missing block: B:22:0x0058, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized boolean endMoveObject(MtpObject oldParent, MtpObject newParent, String name, boolean success) {
        MtpObject oldObj = oldParent.getChild(name);
        MtpObject newObj = newParent.getChild(name);
        boolean z = false;
        if (oldObj != null) {
            if (newObj != null) {
                if (oldParent.getStorageId() != newObj.getStorageId()) {
                    boolean ret = endRemoveObject(oldObj, success);
                    if (generalEndCopyObject(newObj, success, true) && ret) {
                        z = true;
                    }
                } else {
                    if (!success) {
                        MtpObject temp = oldObj;
                        MtpObjectState oldState = oldObj.getState();
                        temp.setParent(newObj.getParent());
                        temp.setState(newObj.getState());
                        oldObj = newObj;
                        oldObj.setParent(oldParent);
                        oldObj.setState(oldState);
                        newObj = temp;
                        newObj.getParent().addChild(newObj);
                        oldParent.addChild(oldObj);
                    }
                    return generalEndRenameObject(oldObj, newObj, success);
                }
            }
        }
    }

    public synchronized int beginCopyObject(MtpObject object, MtpObject newParent) {
        String name = object.getName();
        if (!newParent.isDir()) {
            return -1;
        }
        if (newParent.isRoot() && this.mSubdirectories != null && !this.mSubdirectories.contains(name)) {
            return -1;
        }
        getChildren(newParent);
        if (newParent.getChild(name) != null) {
            return -1;
        }
        MtpObject newObj = object.copy(object.isDir());
        newParent.addChild(newObj);
        newObj.setParent(newParent);
        if (!generalBeginCopyObject(newObj, true)) {
            return -1;
        }
        return newObj.getId();
    }

    public synchronized boolean endCopyObject(MtpObject object, boolean success) {
        return generalEndCopyObject(object, success, false);
    }

    private synchronized boolean generalEndAddObject(MtpObject obj, boolean succeeded, boolean removeGlobal) {
        int i = AnonymousClass1.$SwitchMap$android$mtp$MtpStorageManager$MtpObjectState[obj.getState().ordinal()];
        if (i != 5) {
            switch (i) {
                case 1:
                    if (succeeded) {
                        obj.setState(MtpObjectState.FROZEN_ONESHOT_ADD);
                        break;
                    } else if (!removeObjectFromCache(obj, removeGlobal, false)) {
                        return false;
                    }
                    break;
                case 2:
                    if (removeObjectFromCache(obj, removeGlobal, false)) {
                        if (succeeded) {
                            this.mMtpNotifier.sendObjectRemoved(obj.getId());
                            break;
                        }
                    }
                    return false;
                    break;
                default:
                    return false;
            }
        }
        obj.setState(MtpObjectState.NORMAL);
        if (!succeeded) {
            MtpObject parent = obj.getParent();
            if (!removeObjectFromCache(obj, removeGlobal, false)) {
                return false;
            }
            handleAddedObject(parent, obj.getName(), obj.isDir());
        }
        return true;
    }

    private synchronized boolean generalEndRemoveObject(MtpObject obj, boolean success, boolean removeGlobal) {
        int i = AnonymousClass1.$SwitchMap$android$mtp$MtpStorageManager$MtpObjectState[obj.getState().ordinal()];
        if (i != 5) {
            switch (i) {
                case 1:
                    if (!success) {
                        obj.setState(MtpObjectState.NORMAL);
                        break;
                    }
                    obj.setState(MtpObjectState.FROZEN_ONESHOT_DEL);
                    break;
                case 2:
                    if (removeObjectFromCache(obj, removeGlobal, false)) {
                        if (!success) {
                            this.mMtpNotifier.sendObjectRemoved(obj.getId());
                            break;
                        }
                    }
                    return false;
                    break;
                default:
                    return false;
            }
        }
        obj.setState(MtpObjectState.NORMAL);
        if (success) {
            MtpObject parent = obj.getParent();
            if (!removeObjectFromCache(obj, removeGlobal, false)) {
                return false;
            }
            handleAddedObject(parent, obj.getName(), obj.isDir());
        }
        return true;
    }

    private synchronized boolean generalBeginRenameObject(MtpObject fromObj, MtpObject toObj) {
        fromObj.setState(MtpObjectState.FROZEN);
        toObj.setState(MtpObjectState.FROZEN);
        fromObj.setOperation(MtpOperation.RENAME);
        toObj.setOperation(MtpOperation.RENAME);
        return true;
    }

    private synchronized boolean generalEndRenameObject(MtpObject fromObj, MtpObject toObj, boolean success) {
        boolean z;
        z = generalEndAddObject(toObj, success, success) && generalEndRemoveObject(fromObj, success, success ^ 1);
        return z;
    }

    private synchronized boolean generalBeginRemoveObject(MtpObject obj, MtpOperation op) {
        obj.setState(MtpObjectState.FROZEN);
        obj.setOperation(op);
        if (obj.isDir()) {
            for (MtpObject child : obj.getChildren()) {
                generalBeginRemoveObject(child, op);
            }
        }
        return true;
    }

    private synchronized boolean generalBeginCopyObject(MtpObject obj, boolean newId) {
        obj.setState(MtpObjectState.FROZEN);
        obj.setOperation(MtpOperation.COPY);
        if (newId) {
            obj.setId(getNextObjectId());
            this.mObjects.put(Integer.valueOf(obj.getId()), obj);
        }
        if (obj.isDir()) {
            for (MtpObject child : obj.getChildren()) {
                if (!generalBeginCopyObject(child, newId)) {
                    return false;
                }
            }
        }
        return true;
    }

    private synchronized boolean generalEndCopyObject(MtpObject obj, boolean success, boolean addGlobal) {
        boolean z;
        boolean z2;
        if (success && addGlobal) {
            this.mObjects.put(Integer.valueOf(obj.getId()), obj);
        }
        boolean ret = true;
        z = false;
        if (obj.isDir()) {
            Iterator it = new ArrayList(obj.getChildren()).iterator();
            while (it.hasNext()) {
                MtpObject child = (MtpObject) it.next();
                if (child.getOperation() == MtpOperation.COPY) {
                    boolean z3 = generalEndCopyObject(child, success, addGlobal) && ret;
                    ret = z3;
                }
            }
        }
        if (!success) {
            if (addGlobal) {
                z2 = false;
                if (generalEndAddObject(obj, success, z2) && ret) {
                    z = true;
                }
            }
        }
        z2 = true;
        z = true;
        return z;
    }
}
