package java.beans;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectInputStream.GetField;
import java.io.ObjectOutputStream;
import java.io.ObjectOutputStream.PutField;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Map.Entry;

public class PropertyChangeSupport implements Serializable {
    private static final ObjectStreamField[] serialPersistentFields = new ObjectStreamField[]{new ObjectStreamField("children", Hashtable.class), new ObjectStreamField("source", Object.class), new ObjectStreamField("propertyChangeSupportSerializedDataVersion", Integer.TYPE)};
    static final long serialVersionUID = 6401253773779951803L;
    private PropertyChangeListenerMap map = new PropertyChangeListenerMap();
    private Object source;

    private static final class PropertyChangeListenerMap extends ChangeListenerMap<PropertyChangeListener> {
        private static final PropertyChangeListener[] EMPTY = new PropertyChangeListener[0];

        private PropertyChangeListenerMap() {
        }

        protected PropertyChangeListener[] newArray(int length) {
            if (length > 0) {
                return new PropertyChangeListener[length];
            }
            return EMPTY;
        }

        protected PropertyChangeListener newProxy(String name, PropertyChangeListener listener) {
            return new PropertyChangeListenerProxy(name, listener);
        }

        public final PropertyChangeListener extract(PropertyChangeListener listener) {
            while (listener instanceof PropertyChangeListenerProxy) {
                listener = (PropertyChangeListener) ((PropertyChangeListenerProxy) listener).getListener();
            }
            return listener;
        }
    }

    public PropertyChangeSupport(Object sourceBean) {
        if (sourceBean != null) {
            this.source = sourceBean;
            return;
        }
        throw new NullPointerException();
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (listener != null) {
            if (listener instanceof PropertyChangeListenerProxy) {
                PropertyChangeListenerProxy proxy = (PropertyChangeListenerProxy) listener;
                addPropertyChangeListener(proxy.getPropertyName(), (PropertyChangeListener) proxy.getListener());
            } else {
                this.map.add(null, listener);
            }
        }
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (listener != null) {
            if (listener instanceof PropertyChangeListenerProxy) {
                PropertyChangeListenerProxy proxy = (PropertyChangeListenerProxy) listener;
                removePropertyChangeListener(proxy.getPropertyName(), (PropertyChangeListener) proxy.getListener());
            } else {
                this.map.remove(null, listener);
            }
        }
    }

    public PropertyChangeListener[] getPropertyChangeListeners() {
        return (PropertyChangeListener[]) this.map.getListeners();
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (listener != null && propertyName != null) {
            listener = this.map.extract(listener);
            if (listener != null) {
                this.map.add(propertyName, listener);
            }
        }
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        if (listener != null && propertyName != null) {
            listener = this.map.extract(listener);
            if (listener != null) {
                this.map.remove(propertyName, listener);
            }
        }
    }

    public PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        return (PropertyChangeListener[]) this.map.getListeners(propertyName);
    }

    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (oldValue == null || newValue == null || !oldValue.equals(newValue)) {
            firePropertyChange(new PropertyChangeEvent(this.source, propertyName, oldValue, newValue));
        }
    }

    public void firePropertyChange(String propertyName, int oldValue, int newValue) {
        if (oldValue != newValue) {
            firePropertyChange(propertyName, Integer.valueOf(oldValue), Integer.valueOf(newValue));
        }
    }

    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        if (oldValue != newValue) {
            firePropertyChange(propertyName, Boolean.valueOf(oldValue), Boolean.valueOf(newValue));
        }
    }

    public void firePropertyChange(PropertyChangeEvent event) {
        Object oldValue = event.getOldValue();
        Object newValue = event.getNewValue();
        if (oldValue == null || newValue == null || !oldValue.equals(newValue)) {
            String name = event.getPropertyName();
            PropertyChangeListener[] named = null;
            PropertyChangeListener[] common = (PropertyChangeListener[]) this.map.get(null);
            if (name != null) {
                named = (PropertyChangeListener[]) this.map.get(name);
            }
            fire(common, event);
            fire(named, event);
        }
    }

    private static void fire(PropertyChangeListener[] listeners, PropertyChangeEvent event) {
        if (listeners != null) {
            for (PropertyChangeListener listener : listeners) {
                listener.propertyChange(event);
            }
        }
    }

    public void fireIndexedPropertyChange(String propertyName, int index, Object oldValue, Object newValue) {
        if (oldValue == null || newValue == null || !oldValue.equals(newValue)) {
            firePropertyChange(new IndexedPropertyChangeEvent(this.source, propertyName, oldValue, newValue, index));
        }
    }

    public void fireIndexedPropertyChange(String propertyName, int index, int oldValue, int newValue) {
        if (oldValue != newValue) {
            fireIndexedPropertyChange(propertyName, index, Integer.valueOf(oldValue), Integer.valueOf(newValue));
        }
    }

    public void fireIndexedPropertyChange(String propertyName, int index, boolean oldValue, boolean newValue) {
        if (oldValue != newValue) {
            fireIndexedPropertyChange(propertyName, index, Boolean.valueOf(oldValue), Boolean.valueOf(newValue));
        }
    }

    public boolean hasListeners(String propertyName) {
        return this.map.hasListeners(propertyName);
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        Object children = null;
        PropertyChangeListener[] listeners = null;
        synchronized (this.map) {
            for (Entry<String, PropertyChangeListener[]> entry : this.map.getEntries()) {
                String property = (String) entry.getKey();
                if (property == null) {
                    listeners = (PropertyChangeListener[]) entry.getValue();
                } else {
                    if (children == null) {
                        children = new Hashtable();
                    }
                    PropertyChangeSupport pcs = new PropertyChangeSupport(this.source);
                    pcs.map.set(null, (PropertyChangeListener[]) entry.getValue());
                    children.put(property, pcs);
                }
            }
        }
        PutField fields = s.putFields();
        fields.put("children", children);
        fields.put("source", this.source);
        fields.put("propertyChangeSupportSerializedDataVersion", 2);
        s.writeFields();
        if (listeners != null) {
            for (PropertyChangeListener l : listeners) {
                if (l instanceof Serializable) {
                    s.writeObject(l);
                }
            }
        }
        s.writeObject(null);
    }

    private void readObject(ObjectInputStream s) throws ClassNotFoundException, IOException {
        this.map = new PropertyChangeListenerMap();
        GetField fields = s.readFields();
        Hashtable<String, PropertyChangeSupport> children = (Hashtable) fields.get("children", null);
        this.source = fields.get("source", null);
        fields.get("propertyChangeSupportSerializedDataVersion", 2);
        while (true) {
            Object readObject = s.readObject();
            Object listenerOrNull = readObject;
            if (readObject == null) {
                break;
            }
            this.map.add(null, (PropertyChangeListener) listenerOrNull);
        }
        if (children != null) {
            for (Entry<String, PropertyChangeSupport> entry : children.entrySet()) {
                for (PropertyChangeListener listener : ((PropertyChangeSupport) entry.getValue()).getPropertyChangeListeners()) {
                    this.map.add((String) entry.getKey(), listener);
                }
            }
        }
    }
}
