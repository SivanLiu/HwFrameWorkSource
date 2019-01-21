package com.huawei.nb.container;

import com.huawei.nb.environment.Disposable;
import com.huawei.nb.exception.ExceptionHelper;
import com.huawei.nb.validation.ObjectValidation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public final class DisposableContainer implements Disposable, Container<Disposable> {
    volatile boolean disposed;
    HashSet<Disposable> resources;

    public DisposableContainer() {
        this.resources = new HashSet();
    }

    public DisposableContainer(Disposable... resources) {
        ObjectValidation.verifyNotNull(resources, "resources is null");
        this.resources = new HashSet(resources.length + 1);
        for (Disposable d : resources) {
            ObjectValidation.verifyNotNull(d, "Disposable item is null");
            this.resources.add(d);
        }
    }

    public DisposableContainer(Iterable<? extends Disposable> resources) {
        ObjectValidation.verifyNotNull(resources, "resources is null");
        this.resources = new HashSet();
        for (Disposable d : resources) {
            ObjectValidation.verifyNotNull(d, "Disposable item is null");
            this.resources.add(d);
        }
    }

    public void dispose() {
        if (!this.disposed) {
            synchronized (this) {
                if (this.disposed) {
                    return;
                }
                this.disposed = true;
                HashSet<Disposable> set = this.resources;
                this.resources = null;
                dispose(set);
            }
        }
    }

    public boolean isDisposed() {
        return this.disposed;
    }

    public boolean add(Disposable d) {
        ObjectValidation.verifyNotNull(d, "d is null");
        synchronized (this) {
            if (this.disposed) {
                d.dispose();
                return false;
            }
            HashSet<Disposable> set = this.resources;
            if (set == null) {
                set = new HashSet();
                this.resources = set;
            }
            set.add(d);
            return true;
        }
    }

    public boolean addAll(Disposable... ds) {
        boolean z = false;
        ObjectValidation.verifyNotNull(ds, "ds is null");
        synchronized (this) {
            int length;
            Disposable d;
            if (this.disposed) {
                for (Disposable d2 : ds) {
                    d2.dispose();
                }
            } else {
                HashSet<Disposable> set = this.resources;
                if (set == null) {
                    set = new HashSet(ds.length + 1);
                    this.resources = set;
                }
                length = ds.length;
                int i;
                while (i < length) {
                    d2 = ds[i];
                    ObjectValidation.verifyNotNull(d2, "d is null");
                    set.add(d2);
                    i++;
                }
                z = true;
            }
        }
        return z;
    }

    public boolean remove(Disposable d) {
        if (!delete(d)) {
            return false;
        }
        d.dispose();
        return true;
    }

    /* JADX WARNING: Missing block: B:22:?, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean delete(Disposable d) {
        ObjectValidation.verifyNotNull(d, "Disposable item is null");
        if (this.disposed) {
            return false;
        }
        synchronized (this) {
            if (this.disposed) {
                return false;
            }
            HashSet<Disposable> set = this.resources;
            if (set == null || !set.remove(d)) {
            } else {
                return true;
            }
        }
    }

    public void clear() {
        if (!this.disposed) {
            synchronized (this) {
                if (this.disposed) {
                    return;
                }
                HashSet<Disposable> set = this.resources;
                this.resources = null;
                dispose(set);
            }
        }
    }

    public int size() {
        int i = 0;
        if (!this.disposed) {
            synchronized (this) {
                if (this.disposed) {
                } else {
                    HashSet<Disposable> set = this.resources;
                    if (set != null) {
                        i = set.size();
                    }
                }
            }
        }
        return i;
    }

    void dispose(HashSet<Disposable> set) {
        if (set != null) {
            List<Throwable> errors = null;
            Iterator it = set.iterator();
            while (it.hasNext()) {
                Object o = it.next();
                if (o instanceof Disposable) {
                    try {
                        ((Disposable) o).dispose();
                    } catch (Throwable ex) {
                        ExceptionHelper.throwIfFatal(ex);
                        if (errors == null) {
                            errors = new ArrayList();
                        }
                        errors.add(ex);
                    }
                }
            }
        }
    }

    public Iterator<Disposable> iterator() {
        Iterator it;
        synchronized (this) {
            it = this.resources.iterator();
        }
        return it;
    }
}
