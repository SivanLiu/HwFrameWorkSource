package com.huawei.odmf.core;

import android.support.annotation.NonNull;
import com.huawei.odmf.exception.ODMFConcurrentModificationException;
import com.huawei.odmf.exception.ODMFIllegalArgumentException;
import com.huawei.odmf.exception.ODMFIllegalStateException;
import com.huawei.odmf.exception.ODMFNullPointerException;
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

public class LazyList<E> implements List<E> {
    private static final int arraySize = 500;
    private ManagedObject baseObj;
    private String entityName;
    private List<E> lazyInsertList;
    private List<E> lazyRemoveList;
    /* access modifiers changed from: private */
    public int listSize;
    /* access modifiers changed from: private */
    public transient int modCount;
    private AObjectContext objectContext;
    private List<ObjectId> objectIdList;
    private int relationshipIndex;

    public LazyList(List<ObjectId> objectIdList2, ObjectContext objectContext2, String entityName2) {
        this(objectIdList2, objectContext2, entityName2, null);
    }

    public LazyList(List<ObjectId> objectIdList2, ObjectContext objectContext2, String entityName2, ManagedObject baseObj2) {
        this.objectIdList = null;
        this.objectContext = null;
        this.listSize = 0;
        this.modCount = 0;
        this.baseObj = null;
        this.relationshipIndex = -1;
        if (objectContext2 == null || objectIdList2 == null || entityName2 == null) {
            throw new ODMFIllegalArgumentException("When new LazyList, at least one parameter is null");
        }
        this.objectIdList = objectIdList2;
        this.objectContext = (AObjectContext) objectContext2;
        this.entityName = entityName2;
        this.listSize = objectIdList2.size();
        if (baseObj2 != null) {
            this.baseObj = baseObj2;
            this.lazyInsertList = new ArrayList();
            this.lazyRemoveList = new ArrayList();
        }
    }

    public ManagedObject getBaseObj() {
        return this.baseObj;
    }

    public int getRelationshipIndex() {
        return this.relationshipIndex;
    }

    public void setRelationshipIndex(int relationshipIndex2) {
        this.relationshipIndex = relationshipIndex2;
    }

    @Override // java.util.List
    public E get(int index) {
        if (index >= 0 && index < this.listSize) {
            return (E) getObject(this.objectIdList.get(index));
        }
        throw new IndexOutOfBoundsException("index < 0 || index >= listSize");
    }

    public int size() {
        return this.listSize;
    }

    public boolean isEmpty() {
        return this.listSize == 0;
    }

    @Override // java.util.List, java.util.Collection
    public boolean add(E object) {
        addObj(this.listSize, object);
        addUpdate();
        insertAdd((Object) object);
        if (getRelationshipIndex() < 0 || this.baseObj == null) {
            return true;
        }
        ((AManagedObject) this.baseObj).setRelationshipUpdateSignsTrue(getRelationshipIndex());
        return true;
    }

    @Override // java.util.List
    public void add(int index, E object) {
        if (index < 0 || index > this.listSize) {
            throw new IndexOutOfBoundsException("index < 0 || index > listSize()");
        }
        addObj(index, object);
        addUpdate();
        insertAdd((Object) object);
        if (getRelationshipIndex() >= 0 && this.baseObj != null) {
            ((AManagedObject) this.baseObj).setRelationshipUpdateSignsTrue(getRelationshipIndex());
        }
    }

    public void addObj(int index, E object) {
        if (index < 0 || index > this.listSize) {
            throw new IndexOutOfBoundsException("index < 0 || index > listSize()");
        }
        checkValue(object);
        this.objectIdList.add(index, object.getObjectId());
        this.modCount++;
        this.listSize++;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: java.util.ArrayList */
    /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: java.util.ArrayList */
    /* JADX DEBUG: Multi-variable search result rejected for r0v0, resolved type: java.lang.Object */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // java.util.List, java.util.Collection
    public boolean addAll(@NonNull Collection<? extends E> collection) {
        JudgeUtils.checkNull(collection);
        List<ObjectId> objectIds = new ArrayList<>();
        List<E> objects = new ArrayList<>();
        for (Object object : collection) {
            checkValue(object);
            objectIds.add(((ManagedObject) object).getObjectId());
            objects.add(object);
        }
        addObjAll(this.listSize, objectIds);
        addUpdate();
        insertAdd((List) objects);
        return true;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: java.util.ArrayList */
    /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: java.util.ArrayList */
    /* JADX DEBUG: Multi-variable search result rejected for r0v0, resolved type: java.lang.Object */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // java.util.List
    public boolean addAll(int index, @NonNull Collection<? extends E> collection) {
        JudgeUtils.checkNull(collection);
        if (index < 0 || index > this.listSize) {
            throw new IndexOutOfBoundsException("Invalid index " + index + ", listSize is " + this.listSize);
        }
        List<ObjectId> objectIds = new ArrayList<>();
        List<E> objects = new ArrayList<>();
        for (Object object : collection) {
            checkValue(object);
            objectIds.add(((ManagedObject) object).getObjectId());
            objects.add(object);
        }
        addObjAll(index, objectIds);
        addUpdate();
        insertAdd((List) objects);
        return true;
    }

    private void addObjAll(int index, Collection<ObjectId> collection) {
        this.objectIdList.addAll(index, collection);
        this.listSize += collection.size();
        this.modCount++;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r3v0, resolved type: java.lang.Object */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // java.util.List
    public boolean remove(Object object) {
        checkValue(object);
        if (!this.objectIdList.remove(((ManagedObject) object).getObjectId())) {
            return false;
        }
        this.listSize--;
        this.modCount++;
        addUpdate();
        insertRemove(object);
        if (getRelationshipIndex() >= 0 && this.baseObj != null) {
            ((AManagedObject) this.baseObj).setRelationshipUpdateSignsTrue(getRelationshipIndex());
        }
        return true;
    }

    @Override // java.util.List
    public E remove(int index) {
        if (index < 0 || index >= this.listSize) {
            throw new IndexOutOfBoundsException();
        }
        this.listSize--;
        this.modCount++;
        addUpdate();
        E object = (E) getObject(this.objectIdList.remove(index));
        insertRemove(object);
        if (getRelationshipIndex() >= 0 && this.baseObj != null) {
            ((AManagedObject) this.baseObj).setRelationshipUpdateSignsTrue(getRelationshipIndex());
        }
        return object;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r6v0, resolved type: com.huawei.odmf.core.LazyList<E> */
    /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: java.lang.Object */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // java.util.List, java.util.Collection
    public boolean removeAll(@NonNull Collection<?> collection) {
        JudgeUtils.checkNull(collection);
        boolean flag = false;
        for (Object object : collection) {
            try {
                checkValue(object);
                if (this.objectIdList.remove(((ManagedObject) object).getObjectId())) {
                    this.listSize--;
                    this.modCount++;
                    if (!flag) {
                        flag = true;
                    }
                    insertRemove(object);
                }
            } catch (ODMFIllegalArgumentException | ODMFNullPointerException e) {
            }
        }
        if (!flag) {
            return true;
        }
        addUpdate();
        return true;
    }

    public boolean contains(Object object) {
        JudgeUtils.checkInstance(object);
        return this.objectIdList.contains(((ManagedObject) object).getObjectId());
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

    /* JADX DEBUG: Multi-variable search result rejected for r5v0, resolved type: com.huawei.odmf.core.LazyList<E> */
    /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: java.util.ArrayList */
    /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: com.huawei.odmf.core.ManagedObject */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // java.util.List, java.util.Collection
    public boolean retainAll(@NonNull Collection<?> collection) {
        JudgeUtils.checkNull(collection);
        List<ObjectId> list = new ArrayList<>();
        for (ObjectId id : this.objectIdList) {
            ManagedObject object = getObject(id);
            if (!collection.contains(object)) {
                list.add(id);
                insertRemove(object);
            }
        }
        if (list.isEmpty()) {
            return false;
        }
        this.objectIdList.removeAll(list);
        this.modCount += list.size();
        this.listSize -= list.size();
        addUpdate();
        return true;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r2v0, resolved type: com.huawei.odmf.core.LazyList<E> */
    /* JADX DEBUG: Multi-variable search result rejected for r1v8, resolved type: com.huawei.odmf.core.ManagedObject */
    /* JADX WARN: Multi-variable type inference failed */
    public void clear() {
        for (int i = 0; i < this.listSize; i++) {
            insertRemove(getObject(this.objectIdList.get(i)));
        }
        this.objectIdList.clear();
        this.modCount++;
        this.listSize = 0;
        addUpdate();
    }

    @Override // java.util.List
    public E set(int index, E object) {
        if (index < 0 || index >= this.listSize) {
            throw new IndexOutOfBoundsException("index < 0 || index >= listSize()");
        }
        checkValue(object);
        addUpdate();
        E obj = (E) getObject(this.objectIdList.set(index, object.getObjectId()));
        insertRemove(obj);
        insertAdd((Object) object);
        return obj;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: java.util.ArrayList */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // java.util.List
    @NonNull
    public List<E> subList(int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex > toIndex || toIndex > this.listSize) {
            throw new IndexOutOfBoundsException("Invalid index ");
        }
        ArrayList arrayList = new ArrayList();
        for (int i = fromIndex; i < toIndex; i++) {
            arrayList.add(getObject(this.objectIdList.get(i)));
        }
        return arrayList;
    }

    public int indexOf(Object object) {
        checkValue(object);
        return this.objectIdList.indexOf(((ManagedObject) object).getObjectId());
    }

    public int lastIndexOf(Object object) {
        checkValue(object);
        return this.objectIdList.lastIndexOf(((ManagedObject) object).getObjectId());
    }

    @Override // java.util.List
    @NonNull
    public ListIterator<E> listIterator() {
        if (this.objectIdList != null) {
            return new LazyListIterator(0);
        }
        throw new ODMFIllegalStateException("objectIdList has not been initialized.");
    }

    @Override // java.util.List
    @NonNull
    public ListIterator<E> listIterator(int index) {
        if (this.objectIdList != null) {
            return new LazyListIterator(index);
        }
        throw new ODMFIllegalStateException("objectIdList has not been initialized.");
    }

    @Override // java.util.List, java.util.Collection, java.lang.Iterable
    @NonNull
    public Iterator<E> iterator() {
        if (this.objectIdList != null) {
            return new LazyIterator();
        }
        throw new ODMFIllegalStateException("objectIdList has not been initialized.");
    }

    /* JADX DEBUG: Multi-variable search result rejected for r1v0, resolved type: java.util.ArrayList */
    /* JADX WARN: Multi-variable type inference failed */
    @NonNull
    public Object[] toArray() {
        if (this.listSize > arraySize) {
            throw new ODMFUnsupportedOperationException("This operation is not supported, returning a lots of objects will result in out of memory");
        }
        List<ManagedObject> list = new ArrayList<>();
        int size = this.objectIdList.size();
        for (int i = 0; i < size; i++) {
            list.add(getObject(this.objectIdList.get(i)));
        }
        return list.toArray();
    }

    /* JADX DEBUG: Multi-variable search result rejected for r5v0, resolved type: T[] */
    /* JADX DEBUG: Multi-variable search result rejected for r1v1, resolved type: T[] */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // java.util.List, java.util.Collection
    @NonNull
    public <T> T[] toArray(T[] array) {
        if (array.length >= this.listSize) {
            for (int i = 0; i < this.listSize; i++) {
                array[i] = getObject(this.objectIdList.get(i));
            }
            return array;
        } else if (this.listSize > arraySize) {
            throw new ODMFUnsupportedOperationException("This operation is not supported, returning a lots of objects will result in out of memory");
        } else {
            T[] newArray = (T[]) ((Object[]) Array.newInstance(array.getClass().getComponentType(), this.listSize));
            for (int i2 = 0; i2 < this.listSize; i2++) {
                newArray[i2] = getObject(this.objectIdList.get(i2));
            }
            return newArray;
        }
    }

    private class LazyIterator implements Iterator<E> {
        int cursor;
        int expectedModCount;
        int lazyLastRet;
        int lazyLimit;

        private LazyIterator() {
            this.lazyLimit = LazyList.this.listSize;
            this.expectedModCount = LazyList.this.modCount;
            this.lazyLastRet = -1;
            this.cursor = 0;
        }

        public boolean hasNext() {
            return this.cursor < this.lazyLimit;
        }

        @Override // java.util.Iterator
        public E next() {
            if (LazyList.this.modCount != this.expectedModCount) {
                throw new ODMFConcurrentModificationException();
            }
            int i = this.cursor;
            if (i >= this.lazyLimit) {
                throw new NoSuchElementException();
            }
            this.cursor = i + 1;
            this.lazyLastRet = i;
            return (E) LazyList.this.get(this.lazyLastRet);
        }

        public void remove() {
            if (this.lazyLastRet < 0) {
                throw new ODMFIllegalStateException();
            } else if (LazyList.this.modCount != this.expectedModCount) {
                throw new ODMFConcurrentModificationException();
            } else {
                try {
                    LazyList.this.remove(this.lazyLastRet);
                    this.cursor = this.lazyLastRet;
                    this.lazyLastRet = -1;
                    this.expectedModCount = LazyList.this.modCount;
                    this.lazyLimit--;
                } catch (IndexOutOfBoundsException e) {
                    throw new ODMFConcurrentModificationException();
                }
            }
        }
    }

    private class LazyListIterator extends LazyIterator implements ListIterator<E> {
        LazyListIterator(int index) {
            super();
            if (index < 0 || index > this.lazyLimit) {
                throw new IndexOutOfBoundsException();
            }
            this.cursor = index;
        }

        public boolean hasPrevious() {
            return this.cursor != 0;
        }

        @Override // java.util.ListIterator
        public E previous() {
            if (LazyList.this.modCount != this.expectedModCount) {
                throw new ODMFConcurrentModificationException();
            }
            int i = this.cursor - 1;
            if (i < 0) {
                throw new NoSuchElementException();
            }
            this.cursor = i;
            this.lazyLastRet = i;
            return (E) LazyList.this.get(this.lazyLastRet);
        }

        public int nextIndex() {
            return this.cursor;
        }

        public int previousIndex() {
            return this.cursor - 1;
        }

        @Override // java.util.ListIterator
        public void set(E e) {
            if (this.lazyLastRet < 0) {
                throw new ODMFIllegalStateException();
            } else if (LazyList.this.modCount != this.expectedModCount) {
                throw new ODMFConcurrentModificationException();
            } else {
                try {
                    LazyList.this.set(this.lazyLastRet, e);
                } catch (IndexOutOfBoundsException e2) {
                    throw new ODMFConcurrentModificationException();
                }
            }
        }

        @Override // java.util.ListIterator
        public void add(E object) {
            if (LazyList.this.modCount != this.expectedModCount) {
                throw new ODMFConcurrentModificationException();
            }
            try {
                int i = this.cursor;
                LazyList.this.add(i, object);
                this.cursor = i + 1;
                this.lazyLastRet = -1;
                this.expectedModCount = LazyList.this.modCount;
                this.lazyLimit++;
            } catch (IndexOutOfBoundsException e) {
                throw new ODMFConcurrentModificationException();
            }
        }
    }

    private ManagedObject getObject(ObjectId id) {
        if (id == null) {
            return null;
        }
        return this.objectContext.get(id);
    }

    private void checkValue(Object object) {
        JudgeUtils.checkInstance(object);
        checkEntity((ManagedObject) object);
    }

    private void checkEntity(ManagedObject element) {
        if (!this.entityName.equals(element.getEntityName())) {
            throw new ODMFIllegalArgumentException(JudgeUtils.INCOMPATIBLE_OBJECTS_NOT_ALLOWED_MESSAGE);
        }
    }

    private void addUpdate() {
        if (this.baseObj != null) {
            this.objectContext.update(this.baseObj);
        }
    }

    public boolean clearModify() {
        if (this.baseObj == null) {
            return false;
        }
        this.lazyInsertList.clear();
        this.lazyRemoveList.clear();
        return true;
    }

    private void insertAdd(E object) {
        if (this.lazyInsertList != null && !JudgeUtils.isContainedObject(this.lazyInsertList, object)) {
            this.lazyInsertList.add(object);
        }
    }

    private void insertAdd(List<E> list) {
        if (this.lazyInsertList != null) {
            int size = list.size();
            for (int i = 0; i < size; i++) {
                if (!JudgeUtils.isContainedObject(this.lazyInsertList, list.get(i))) {
                    this.lazyInsertList.add(list.get(i));
                }
            }
        }
    }

    private void insertRemove(E object) {
        if (this.lazyRemoveList != null && !JudgeUtils.isContainedObject(this.lazyRemoveList, object)) {
            this.lazyRemoveList.add(object);
        }
    }

    public List<E> getInsertList() {
        return this.lazyInsertList;
    }

    public List<E> getRemoveList() {
        return this.lazyRemoveList;
    }
}
