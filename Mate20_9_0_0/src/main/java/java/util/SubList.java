package java.util;

/* compiled from: AbstractList */
class SubList<E> extends AbstractList<E> {
    private final AbstractList<E> l;
    private final int offset;
    private int size;

    SubList(AbstractList<E> list, int fromIndex, int toIndex) {
        StringBuilder stringBuilder;
        if (fromIndex < 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("fromIndex = ");
            stringBuilder.append(fromIndex);
            throw new IndexOutOfBoundsException(stringBuilder.toString());
        } else if (toIndex > list.size()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("toIndex = ");
            stringBuilder.append(toIndex);
            throw new IndexOutOfBoundsException(stringBuilder.toString());
        } else if (fromIndex <= toIndex) {
            this.l = list;
            this.offset = fromIndex;
            this.size = toIndex - fromIndex;
            this.modCount = this.l.modCount;
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("fromIndex(");
            stringBuilder.append(fromIndex);
            stringBuilder.append(") > toIndex(");
            stringBuilder.append(toIndex);
            stringBuilder.append(")");
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public E set(int index, E element) {
        rangeCheck(index);
        checkForComodification();
        return this.l.set(this.offset + index, element);
    }

    public E get(int index) {
        rangeCheck(index);
        checkForComodification();
        return this.l.get(this.offset + index);
    }

    public int size() {
        checkForComodification();
        return this.size;
    }

    public void add(int index, E element) {
        rangeCheckForAdd(index);
        checkForComodification();
        this.l.add(this.offset + index, element);
        this.modCount = this.l.modCount;
        this.size++;
    }

    public E remove(int index) {
        rangeCheck(index);
        checkForComodification();
        E result = this.l.remove(this.offset + index);
        this.modCount = this.l.modCount;
        this.size--;
        return result;
    }

    protected void removeRange(int fromIndex, int toIndex) {
        checkForComodification();
        this.l.removeRange(this.offset + fromIndex, this.offset + toIndex);
        this.modCount = this.l.modCount;
        this.size -= toIndex - fromIndex;
    }

    public boolean addAll(Collection<? extends E> c) {
        return addAll(this.size, c);
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        rangeCheckForAdd(index);
        int cSize = c.size();
        if (cSize == 0) {
            return false;
        }
        checkForComodification();
        this.l.addAll(this.offset + index, c);
        this.modCount = this.l.modCount;
        this.size += cSize;
        return true;
    }

    public Iterator<E> iterator() {
        return listIterator();
    }

    public ListIterator<E> listIterator(final int index) {
        checkForComodification();
        rangeCheckForAdd(index);
        return new ListIterator<E>() {
            private final ListIterator<E> i = SubList.this.l.listIterator(index + SubList.this.offset);

            public boolean hasNext() {
                return nextIndex() < SubList.this.size;
            }

            public E next() {
                if (hasNext()) {
                    return this.i.next();
                }
                throw new NoSuchElementException();
            }

            public boolean hasPrevious() {
                return previousIndex() >= 0;
            }

            public E previous() {
                if (hasPrevious()) {
                    return this.i.previous();
                }
                throw new NoSuchElementException();
            }

            public int nextIndex() {
                return this.i.nextIndex() - SubList.this.offset;
            }

            public int previousIndex() {
                return this.i.previousIndex() - SubList.this.offset;
            }

            public void remove() {
                this.i.remove();
                SubList.this.modCount = SubList.this.l.modCount;
                SubList.this.size = SubList.this.size - 1;
            }

            public void set(E e) {
                this.i.set(e);
            }

            public void add(E e) {
                this.i.add(e);
                SubList.this.modCount = SubList.this.l.modCount;
                SubList.this.size = SubList.this.size + 1;
            }
        };
    }

    public List<E> subList(int fromIndex, int toIndex) {
        return new SubList(this, fromIndex, toIndex);
    }

    private void rangeCheck(int index) {
        if (index < 0 || index >= this.size) {
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
        }
    }

    private void rangeCheckForAdd(int index) {
        if (index < 0 || index > this.size) {
            throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
        }
    }

    private String outOfBoundsMsg(int index) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Index: ");
        stringBuilder.append(index);
        stringBuilder.append(", Size: ");
        stringBuilder.append(this.size);
        return stringBuilder.toString();
    }

    private void checkForComodification() {
        if (this.modCount != this.l.modCount) {
            throw new ConcurrentModificationException();
        }
    }
}
