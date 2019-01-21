package java.util;

public abstract class AbstractSequentialList<E> extends AbstractList<E> {
    public abstract ListIterator<E> listIterator(int i);

    protected AbstractSequentialList() {
    }

    public E get(int index) {
        try {
            return listIterator(index).next();
        } catch (NoSuchElementException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Index: ");
            stringBuilder.append(index);
            throw new IndexOutOfBoundsException(stringBuilder.toString());
        }
    }

    public E set(int index, E element) {
        try {
            ListIterator<E> e = listIterator(index);
            E oldVal = e.next();
            e.set(element);
            return oldVal;
        } catch (NoSuchElementException e2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Index: ");
            stringBuilder.append(index);
            throw new IndexOutOfBoundsException(stringBuilder.toString());
        }
    }

    public void add(int index, E element) {
        try {
            listIterator(index).add(element);
        } catch (NoSuchElementException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Index: ");
            stringBuilder.append(index);
            throw new IndexOutOfBoundsException(stringBuilder.toString());
        }
    }

    public E remove(int index) {
        try {
            ListIterator<E> e = listIterator(index);
            E outCast = e.next();
            e.remove();
            return outCast;
        } catch (NoSuchElementException e2) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Index: ");
            stringBuilder.append(index);
            throw new IndexOutOfBoundsException(stringBuilder.toString());
        }
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        boolean modified = false;
        try {
            ListIterator<E> e1 = listIterator(index);
            for (Object add : c) {
                e1.add(add);
                modified = true;
            }
            return modified;
        } catch (NoSuchElementException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Index: ");
            stringBuilder.append(index);
            throw new IndexOutOfBoundsException(stringBuilder.toString());
        }
    }

    public Iterator<E> iterator() {
        return listIterator();
    }
}
