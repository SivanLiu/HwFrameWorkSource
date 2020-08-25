package com.huawei.nb.odmfadapter;

import android.database.Cursor;
import android.support.annotation.Nullable;
import com.huawei.nb.query.bulkcursor.BulkCursorDescriptor;
import com.huawei.nb.query.bulkcursor.BulkCursorToCursorAdaptor;
import com.huawei.nb.utils.logger.DSLog;
import com.huawei.odmf.core.AManagedObject;
import com.huawei.odmf.core.ManagedObject;
import com.huawei.odmf.user.api.ObjectContext;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class OdmfHelper {
    private ObjectContext objectContext;

    public OdmfHelper(ObjectContext objectContext2) {
        this.objectContext = objectContext2;
    }

    public List parseCursor(String entityName, Object cursor) {
        if (entityName == null || cursor == null || !(cursor instanceof BulkCursorDescriptor)) {
            DSLog.e("Failed to parse cursor, error: null input parameters.", new Object[0]);
            return null;
        }
        Constructor constructor = getConstructorOfClass(entityName);
        if (constructor == null) {
            DSLog.e("Failed to parse cursor, error: null object constructor.", new Object[0]);
            return null;
        }
        Cursor wrappedCursor = wrapCursor(cursor);
        List<ManagedObject> results = new ArrayList<>(wrappedCursor.getCount());
        while (wrappedCursor.moveToNext()) {
            try {
                if (constructor.newInstance(wrappedCursor) instanceof ManagedObject) {
                    ManagedObject object = (ManagedObject) constructor.newInstance(wrappedCursor);
                    object.setState(4);
                    object.setObjectContext(this.objectContext);
                    results.add(object);
                }
            } catch (InstantiationException e) {
                try {
                    DSLog.e("Failed to read entity %s from cursor.", entityName);
                    return null;
                } finally {
                    wrappedCursor.close();
                }
            } catch (IllegalAccessException e2) {
                DSLog.e("Failed to read entity %s from cursor.", entityName);
                return null;
            } catch (InvocationTargetException e3) {
                DSLog.e("Failed to read entity %s from cursor.", entityName);
                return null;
            }
        }
        wrappedCursor.close();
        return results;
    }

    public List parseNativeCursor(String entityName, Cursor cursor) {
        if (entityName == null || cursor == null) {
            DSLog.e("Failed to parse native cursor, error: null input parameters.", new Object[0]);
            return null;
        }
        Constructor constructor = getConstructorOfClass(entityName);
        if (constructor == null) {
            DSLog.e("Failed to parse native cursor, error: null object constructor.", new Object[0]);
            return null;
        }
        List<ManagedObject> results = new ArrayList<>();
        while (cursor.moveToNext()) {
            try {
                if (constructor.newInstance(cursor) instanceof ManagedObject) {
                    ManagedObject object = (ManagedObject) constructor.newInstance(cursor);
                    object.setState(4);
                    object.setObjectContext(this.objectContext);
                    results.add(object);
                }
            } catch (InstantiationException e) {
                try {
                    DSLog.e("Failed to read entity %s from cursor.", entityName);
                    return null;
                } finally {
                    cursor.close();
                }
            } catch (IllegalAccessException e2) {
                DSLog.e("Failed to read entity %s from cursor.", entityName);
                return null;
            } catch (InvocationTargetException e3) {
                DSLog.e("Failed to read entity %s from cursor.", entityName);
                return null;
            }
        }
        cursor.close();
        return results;
    }

    public Cursor wrapCursor(Object cursor) {
        if (cursor == null) {
            return null;
        }
        BulkCursorToCursorAdaptor adaptor = new BulkCursorToCursorAdaptor();
        adaptor.initialize((BulkCursorDescriptor) cursor);
        return adaptor;
    }

    @Nullable
    private Constructor getConstructorOfClass(String name) {
        try {
            return Class.forName(name).getConstructor(Cursor.class);
        } catch (ClassNotFoundException e) {
            DSLog.e("Failed to find Class %s.", name);
            return null;
        } catch (NoSuchMethodException e2) {
            DSLog.e("Failed to find the constructor %s.", name);
            return null;
        }
    }

    public List assignObjectContext(List rawObjects) {
        if (rawObjects != null) {
            for (Object object : rawObjects) {
                if (object instanceof ManagedObject) {
                    ((ManagedObject) object).setObjectContext(this.objectContext);
                }
            }
        }
        return rawObjects;
    }

    public <T extends AManagedObject> void presetUriString(List<T> entities) {
        if (entities != null && !entities.isEmpty()) {
            String uriString = generateUriString(entities.get(0).getDatabaseName());
            for (T obj : entities) {
                obj.setUriString(uriString);
            }
        }
    }

    private String generateUriString(String dbName) {
        return "odmf://com.huawei.odmf/" + dbName;
    }
}
