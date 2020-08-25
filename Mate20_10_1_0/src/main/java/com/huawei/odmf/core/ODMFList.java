package com.huawei.odmf.core;

import android.support.annotation.NonNull;
import com.huawei.odmf.exception.ODMFConcurrentModificationException;
import com.huawei.odmf.exception.ODMFIllegalArgumentException;
import com.huawei.odmf.exception.ODMFIllegalStateException;
import com.huawei.odmf.exception.ODMFUnsupportedOperationException;
import com.huawei.odmf.user.api.ObjectContext;
import com.huawei.odmf.utils.JudgeUtils;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

public class ODMFList<E> implements List<E> {
    private static final int arraySize = 500;
    private ObjectContext ctx;
    private String entityName;
    private List<E> insertList;
    /* access modifiers changed from: private */
    public int listSize;
    private List<E> objList;
    private ManagedObject odmfBaseObj;
    /* access modifiers changed from: private */
    public transient int odmfModCount;
    private int relationshipIndex;
    private List<E> removeList;

    public ODMFList(ObjectContext ctx2, String entityName2) {
        this(ctx2, entityName2, null);
    }

    public ODMFList(ObjectContext ctx2, String entityName2, ManagedObject baseObj) {
        this.listSize = 0;
        this.odmfModCount = 0;
        this.odmfBaseObj = null;
        this.relationshipIndex = -1;
        if (ctx2 == null || entityName2 == null) {
            throw new ODMFIllegalArgumentException("When new ODMFList, at least one input parameter is null");
        }
        this.ctx = ctx2;
        this.entityName = entityName2;
        this.objList = new ArrayList();
        if (baseObj != null) {
            this.odmfBaseObj = baseObj;
            this.insertList = new ArrayList();
            this.removeList = new ArrayList();
        }
    }

    public ManagedObject getBaseObj() {
        return this.odmfBaseObj;
    }

    public int getRelationshipIndex() {
        return this.relationshipIndex;
    }

    public void setRelationshipIndex(int relationshipIndex2) {
        this.relationshipIndex = relationshipIndex2;
    }

    public int size() {
        return this.listSize;
    }

    public boolean isEmpty() {
        return this.listSize == 0;
    }

    public boolean contains(Object object) {
        JudgeUtils.checkNull(object);
        return this.objList.contains(object);
    }

    @Override // java.util.List, java.util.Collection
    public boolean add(E element) {
        addObj(this.listSize, element);
        addUpdate();
        insertAdd((Object) element);
        if (getRelationshipIndex() < 0 || this.odmfBaseObj == null) {
            return true;
        }
        ((AManagedObject) this.odmfBaseObj).setRelationshipUpdateSignsTrue(getRelationshipIndex());
        return true;
    }

    @Override // java.util.List
    public void add(int index, E element) {
        addObj(index, element);
        addUpdate();
        insertAdd((Object) element);
        if (getRelationshipIndex() >= 0 && this.odmfBaseObj != null) {
            ((AManagedObject) this.odmfBaseObj).setRelationshipUpdateSignsTrue(getRelationshipIndex());
        }
    }

    public void addObj(int index, E element) {
        if (index < 0 || index > this.listSize) {
            throw new IndexOutOfBoundsException("index < 0 || index > listSize()");
        }
        checkValue(element);
        this.objList.add(index, element);
        this.odmfModCount++;
        this.listSize++;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: java.util.ArrayList */
    /* JADX DEBUG: Multi-variable search result rejected for r0v0, resolved type: java.lang.Object */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // java.util.List, java.util.Collection
    public boolean addAll(@NonNull Collection<? extends E> collection) {
        JudgeUtils.checkNull(collection);
        List<E> list = new ArrayList<>();
        for (Object obj : collection) {
            checkValue(obj);
            list.add(obj);
        }
        addObjAll(this.listSize, list);
        addUpdate();
        insertAdd((List) list);
        return true;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: java.util.ArrayList */
    /* JADX DEBUG: Multi-variable search result rejected for r0v0, resolved type: java.lang.Object */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // java.util.List
    public boolean addAll(int index, @NonNull Collection<? extends E> collection) {
        JudgeUtils.checkNull(collection);
        if (index < 0 || index > this.listSize) {
            throw new IndexOutOfBoundsException("Invalid index " + index + ", listSize is " + this.listSize);
        }
        List<E> list = new ArrayList<>();
        for (Object obj : collection) {
            checkValue(obj);
            list.add(obj);
        }
        addObjAll(index, list);
        addUpdate();
        insertAdd((List) list);
        return true;
    }

    public void addObjAll(int index, Collection<? extends E> collection) {
        this.objList.addAll(index, collection);
        this.listSize += collection.size();
        this.odmfModCount++;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: java.lang.Object */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // java.util.List
    public boolean remove(Object object) {
        checkValue(object);
        if (!this.objList.remove(object)) {
            return false;
        }
        this.listSize--;
        this.odmfModCount++;
        addUpdate();
        insertRemove(object);
        if (getRelationshipIndex() >= 0 && this.odmfBaseObj != null) {
            ((AManagedObject) this.odmfBaseObj).setRelationshipUpdateSignsTrue(getRelationshipIndex());
        }
        return true;
    }

    @Override // java.util.List
    public E remove(int index) {
        if (index < 0 || index >= this.listSize) {
            throw new IndexOutOfBoundsException();
        }
        E remove = this.objList.remove(index);
        this.listSize--;
        this.odmfModCount++;
        insertRemove(remove);
        addUpdate();
        if (getRelationshipIndex() >= 0 && this.odmfBaseObj != null) {
            ((AManagedObject) this.odmfBaseObj).setRelationshipUpdateSignsTrue(getRelationshipIndex());
        }
        return remove;
    }

    @Override // java.util.List, java.util.Collection
    public boolean containsAll(@NonNull Collection<?> collection) {
        JudgeUtils.checkNull(collection);
        Iterator<?> it = collection.iterator();
        while (it.hasNext()) {
            if (!contains(it.next())) {
                return false;
            }
        }
        return true;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r5v0, resolved type: com.huawei.odmf.core.ODMFList<E> */
    /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: java.lang.Object */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // java.util.List, java.util.Collection
    public boolean removeAll(@NonNull Collection<?> collection) {
        JudgeUtils.checkNull(collection);
        boolean flag = false;
        for (Object object : collection) {
            try {
                checkValue(object);
                if (this.objList.remove(object)) {
                    this.listSize--;
                    this.odmfModCount++;
                    if (!flag) {
                        flag = true;
                    }
                    insertRemove(object);
                }
            } catch (ODMFIllegalArgumentException e) {
            }
        }
        if (!flag) {
            return true;
        }
        addUpdate();
        return true;
    }

    @Override // java.util.List, java.util.Collection
    public boolean retainAll(@NonNull Collection<?> collection) {
        JudgeUtils.checkNull(collection);
        List<E> list = new ArrayList<>();
        int size = this.objList.size();
        for (int i = 0; i < size; i++) {
            E element = this.objList.get(i);
            if (!collection.contains(element)) {
                list.add(element);
                insertRemove(element);
            }
        }
        if (list.isEmpty()) {
            return false;
        }
        this.objList.removeAll(list);
        this.odmfModCount += list.size();
        this.listSize -= list.size();
        addUpdate();
        return true;
    }

    public void clear() {
        for (int i = 0; i < this.listSize; i++) {
            insertRemove(this.objList.get(i));
        }
        this.objList.clear();
        this.listSize = 0;
        this.odmfModCount++;
        addUpdate();
    }

    @Override // java.util.List
    public E get(int index) {
        if (index >= 0 && index < this.listSize) {
            return this.objList.get(index);
        }
        throw new IndexOutOfBoundsException("index < 0 || index >= listSize");
    }

    @Override // java.util.List
    public E set(int index, E element) {
        if (index < 0 || index >= this.listSize) {
            throw new IndexOutOfBoundsException("index < 0 || index > listSize");
        }
        checkValue(element);
        E set = this.objList.set(index, element);
        addUpdate();
        insertRemove(set);
        insertAdd((Object) element);
        return set;
    }

    public int indexOf(Object object) {
        JudgeUtils.checkNull(object);
        return this.objList.indexOf(object);
    }

    public int lastIndexOf(Object object) {
        JudgeUtils.checkNull(object);
        return this.objList.lastIndexOf(object);
    }

    @NonNull
    public Object[] toArray() {
        if (this.listSize <= arraySize) {
            return this.objList.toArray();
        }
        throw new ODMFUnsupportedOperationException("This operation is not supported, returning a lots of objects will result in out of memory");
    }

    /* JADX DEBUG: Multi-variable search result rejected for r5v0, resolved type: T[] */
    /* JADX DEBUG: Multi-variable search result rejected for r1v1, resolved type: T[] */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // java.util.List, java.util.Collection
    @NonNull
    public <T> T[] toArray(@NonNull T[] array) {
        if (array.length >= this.listSize) {
            for (int i = 0; i < this.listSize; i++) {
                array[i] = this.objList.get(i);
            }
            return array;
        }
        T[] newArray = (T[]) ((Object[]) Array.newInstance(array.getClass().getComponentType(), this.listSize));
        for (int i2 = 0; i2 < this.listSize; i2++) {
            newArray[i2] = this.objList.get(i2);
        }
        return newArray;
    }

    @Override // java.util.List
    @NonNull
    public List<E> subList(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex > toIndex || toIndex > this.listSize) {
            throw new IndexOutOfBoundsException("Invalid index ");
        }
        ArrayList list = new ArrayList();
        for (int i = fromIndex; i < toIndex; i++) {
            list.add(this.objList.get(i));
        }
        return list;
    }

    @Override // java.util.List, java.util.Collection, java.lang.Iterable
    @NonNull
    public Iterator<E> iterator() {
        if (this.objList != null) {
            return new ODMFItr();
        }
        throw new ODMFIllegalStateException("ODMFList has not been initialized.");
    }

    private class ODMFItr implements Iterator<E> {
        int cursor;
        int expectedModCount;
        int odmfLastRet;
        int odmfLimit;

        private ODMFItr() {
            this.odmfLimit = ODMFList.this.listSize;
            this.expectedModCount = ODMFList.this.odmfModCount;
            this.odmfLastRet = -1;
            this.cursor = 0;
        }

        public boolean hasNext() {
            return this.cursor < this.odmfLimit;
        }

        @Override // java.util.Iterator
        public E next() {
            checkConcurrentModification();
            int i = this.cursor;
            if (i >= this.odmfLimit) {
                throw new NoSuchElementException();
            }
            this.cursor = i + 1;
            this.odmfLastRet = i;
            return (E) ODMFList.this.get(this.odmfLastRet);
        }

        public void remove() {
            if (this.odmfLastRet < 0) {
                throw new ODMFIllegalStateException();
            }
            checkConcurrentModification();
            try {
                ODMFList.this.remove(this.odmfLastRet);
                this.cursor = this.odmfLastRet;
                this.odmfLastRet = -1;
                this.expectedModCount = ODMFList.this.odmfModCount;
                this.odmfLimit--;
            } catch (IndexOutOfBoundsException e) {
                throw new ODMFConcurrentModificationException();
            }
        }

        /* access modifiers changed from: package-private */
        public void checkConcurrentModification() {
            if (ODMFList.this.odmfModCount != this.expectedModCount) {
                throw new ODMFConcurrentModificationException();
            }
        }
    }

    @Override // java.util.List
    @NonNull
    public ListIterator<E> listIterator() {
        if (this.objList != null) {
            return new ODMFListItr(0);
        }
        throw new ODMFIllegalStateException("ODMFList has not been initialized.");
    }

    @Override // java.util.List
    @NonNull
    public ListIterator<E> listIterator(int index) {
        if (this.objList != null) {
            return new ODMFListItr(index);
        }
        throw new ODMFIllegalStateException("ODMFList has not been initialized.");
    }

    private class ODMFListItr extends ODMFItr implements ListIterator<E> {
        ODMFListItr(int index) {
            super();
            if (index < 0 || index > this.odmfLimit) {
                throw new IndexOutOfBoundsException();
            }
            this.cursor = index;
        }

        public boolean hasPrevious() {
            return this.cursor != 0;
        }

        @Override // java.util.ListIterator
        public E previous() {
            checkConcurrentModification();
            int i = this.cursor - 1;
            if (i < 0) {
                throw new NoSuchElementException();
            }
            this.cursor = i;
            this.odmfLastRet = i;
            return (E) ODMFList.this.get(this.odmfLastRet);
        }

        public int nextIndex() {
            return this.cursor;
        }

        public int previousIndex() {
            return this.cursor - 1;
        }

        @Override // java.util.ListIterator
        public void set(E element) {
            if (this.odmfLastRet < 0) {
                throw new ODMFIllegalStateException();
            }
            checkConcurrentModification();
            try {
                ODMFList.this.set(this.odmfLastRet, element);
            } catch (IndexOutOfBoundsException e) {
                throw new ODMFConcurrentModificationException();
            }
        }

        @Override // java.util.ListIterator
        public void add(E element) {
            checkConcurrentModification();
            try {
                int i = this.cursor;
                ODMFList.this.add(i, element);
                this.cursor = i + 1;
                this.odmfLastRet = -1;
                this.expectedModCount = ODMFList.this.odmfModCount;
                this.odmfLimit++;
            } catch (IndexOutOfBoundsException e) {
                throw new ODMFConcurrentModificationException();
            }
        }
    }

    private void checkValue(Object object) {
        JudgeUtils.checkInstance(object);
        checkEntity((ManagedObject) object);
    }

    private void checkEntity(ManagedObject managedObject) {
        JudgeUtils.checkNull(managedObject);
        if (!this.entityName.equals(managedObject.getEntityName())) {
            throw new ODMFIllegalArgumentException(JudgeUtils.INCOMPATIBLE_OBJECTS_NOT_ALLOWED_MESSAGE);
        }
    }

    private void addUpdate() {
        if (this.odmfBaseObj != null) {
            this.ctx.update(this.odmfBaseObj);
        }
    }

    public boolean clearModify() {
        if (this.odmfBaseObj == null) {
            return false;
        }
        this.insertList.clear();
        this.removeList.clear();
        return true;
    }

    private void insertAdd(E element) {
        if (this.insertList != null && !JudgeUtils.isContainedObject(this.insertList, element)) {
            this.insertList.add(element);
        }
    }

    private void insertAdd(List<E> list) {
        if (this.insertList != null) {
            int size = list.size();
            for (int i = 0; i < size; i++) {
                if (!JudgeUtils.isContainedObject(this.insertList, list.get(i))) {
                    this.insertList.add(list.get(i));
                }
            }
        }
    }

    private void insertRemove(E element) {
        if (this.removeList != null && !JudgeUtils.isContainedObject(this.removeList, element)) {
            this.removeList.add(element);
        }
    }

    public List<E> getInsertList() {
        return this.insertList;
    }

    public List<E> getRemoveList() {
        return this.removeList;
    }
}
