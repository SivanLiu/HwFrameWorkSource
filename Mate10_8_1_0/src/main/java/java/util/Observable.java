package java.util;

public class Observable {
    private boolean changed = false;
    private Vector<Observer> obs = new Vector();

    public synchronized void addObserver(Observer o) {
        if (o == null) {
            throw new NullPointerException();
        } else if (!this.obs.contains(o)) {
            this.obs.addElement(o);
        }
    }

    public synchronized void deleteObserver(Observer o) {
        this.obs.removeElement(o);
    }

    public void notifyObservers() {
        notifyObservers(null);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void notifyObservers(Object arg) {
        synchronized (this) {
            if (hasChanged()) {
                Object[] arrLocal = this.obs.toArray();
                clearChanged();
            }
        }
    }

    public synchronized void deleteObservers() {
        this.obs.removeAllElements();
    }

    protected synchronized void setChanged() {
        this.changed = true;
    }

    protected synchronized void clearChanged() {
        this.changed = false;
    }

    public synchronized boolean hasChanged() {
        return this.changed;
    }

    public synchronized int countObservers() {
        return this.obs.size();
    }
}
