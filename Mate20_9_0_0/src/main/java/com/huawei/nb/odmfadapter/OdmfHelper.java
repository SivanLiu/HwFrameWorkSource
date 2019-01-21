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

    public OdmfHelper(ObjectContext objectContext) {
        this.objectContext = objectContext;
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
        List results = new ArrayList();
        while (wrappedCursor.moveToNext()) {
            try {
                ManagedObject object = (ManagedObject) constructor.newInstance(new Object[]{wrappedCursor});
                object.setState(4);
                object.setObjectContext(this.objectContext);
                results.add(object);
            } catch (InstantiationException e) {
                try {
                    DSLog.e("Failed to read entity %s from cursor.", entityName);
                    return null;
                } finally {
                    wrappedCursor.close();
                }
            } catch (IllegalAccessException e2) {
                IllegalAccessException illegalAccessException = e2;
                DSLog.e("Failed to read entity %s from cursor.", entityName);
                return null;
            } catch (InvocationTargetException e3) {
                InvocationTargetException invocationTargetException = e3;
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
        List results = new ArrayList();
        while (cursor.moveToNext()) {
            try {
                ManagedObject object = (ManagedObject) constructor.newInstance(new Object[]{cursor});
                object.setState(4);
                object.setObjectContext(this.objectContext);
                results.add(object);
            } catch (InstantiationException e) {
                try {
                    DSLog.e("Failed to read entity %s from cursor.", entityName);
                    return null;
                } finally {
                    cursor.close();
                }
            } catch (IllegalAccessException e2) {
                IllegalAccessException illegalAccessException = e2;
                DSLog.e("Failed to read entity %s from cursor.", entityName);
                return null;
            } catch (InvocationTargetException e3) {
                InvocationTargetException invocationTargetException = e3;
                DSLog.e("Failed to read entity %s from cursor.", entityName);
                return null;
            }
        }
        cursor.close();
        return results;
    }

    public void resetObjectContext(List<? extends ManagedObject> objects) {
        if (objects == null) {
            DSLog.e("Failed to reset objectContext, error: null input parameters.", new Object[0]);
            return;
        }
        for (ManagedObject managedObject : objects) {
            managedObject.setObjectContext(this.objectContext);
        }
    }

    public Cursor wrapCursor(Object cursor) {
        BulkCursorToCursorAdaptor adaptor = new BulkCursorToCursorAdaptor();
        adaptor.initialize((BulkCursorDescriptor) cursor);
        return adaptor;
    }

    @Nullable
    private Constructor getConstructorOfClass(String name) {
        Constructor constructor = null;
        try {
            return Class.forName(name).getConstructor(new Class[]{Cursor.class});
        } catch (ClassNotFoundException e) {
            DSLog.e("Failed to find Class %s.", name);
            return constructor;
        } catch (NoSuchMethodException e2) {
            DSLog.e("Failed to find the constructor %s.", name);
            return constructor;
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
            String uriString = generateUriString(((AManagedObject) entities.get(0)).getDatabaseName());
            for (T obj : entities) {
                obj.setUriString(uriString);
            }
        }
    }

    private String generateUriString(String dbName) {
        return "odmf://com.huawei.odmf/" + dbName;
    }
}
