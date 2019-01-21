package android.database;

import java.util.ArrayList;

public abstract class Observable<T> {
    protected final ArrayList<T> mObservers = new ArrayList();

    public void registerObserver(T observer) {
        if (observer != null) {
            synchronized (this.mObservers) {
                if (this.mObservers.contains(observer)) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Observer ");
                    stringBuilder.append(observer);
                    stringBuilder.append(" is already registered.");
                    throw new IllegalStateException(stringBuilder.toString());
                }
                this.mObservers.add(observer);
            }
            return;
        }
        throw new IllegalArgumentException("The observer is null.");
    }

    public void unregisterObserver(T observer) {
        if (observer != null) {
            synchronized (this.mObservers) {
                int index = this.mObservers.indexOf(observer);
                if (index != -1) {
                    this.mObservers.remove(index);
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Observer ");
                    stringBuilder.append(observer);
                    stringBuilder.append(" was not registered.");
                    throw new IllegalStateException(stringBuilder.toString());
                }
            }
            return;
        }
        throw new IllegalArgumentException("The observer is null.");
    }

    public void unregisterAll() {
        synchronized (this.mObservers) {
            this.mObservers.clear();
        }
    }
}
