package com.huawei.odmf.utils;

import com.huawei.odmf.core.ManagedObject;
import com.huawei.odmf.exception.ODMFIllegalArgumentException;
import java.util.List;

public class JudgeUtils {
    public static final String INCOMPATIBLE_OBJECTS_NOT_ALLOWED_MESSAGE = "The element is incompatible with this LazyList";
    public static final String NULL_OBJECTS_NOT_ALLOWED_MESSAGE = "The specific object is null";

    private JudgeUtils() {
    }

    public static void checkNull(Object object) {
        if (object == null) {
            throw new ODMFIllegalArgumentException(NULL_OBJECTS_NOT_ALLOWED_MESSAGE);
        }
    }

    public static void checkInstance(Object object) {
        if (!(object instanceof ManagedObject)) {
            throw new ODMFIllegalArgumentException(INCOMPATIBLE_OBJECTS_NOT_ALLOWED_MESSAGE);
        }
    }

    public static boolean isContainedObject(List list, Object object) {
        ManagedObject obj = (ManagedObject) object;
        int size = list.size();
        for (int i = 0; i < size; i++) {
            if (list.get(i).equals(obj)) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkVersion(String version) {
        return version.matches("[0-9]+(\\.[0-9]+)*");
    }
}
