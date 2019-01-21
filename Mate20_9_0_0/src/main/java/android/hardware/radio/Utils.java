package android.hardware.radio;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

final class Utils {
    private static final String TAG = "BroadcastRadio.utils";

    Utils() {
    }

    static void writeStringMap(Parcel dest, Map<String, String> map) {
        if (map == null) {
            dest.writeInt(0);
            return;
        }
        dest.writeInt(map.size());
        for (Entry<String, String> entry : map.entrySet()) {
            dest.writeString((String) entry.getKey());
            dest.writeString((String) entry.getValue());
        }
    }

    static Map<String, String> readStringMap(Parcel in) {
        int size = in.readInt();
        Map<String, String> map = new HashMap();
        while (true) {
            int size2 = size - 1;
            if (size <= 0) {
                return map;
            }
            map.put(in.readString(), in.readString());
            size = size2;
        }
    }

    static void writeStringIntMap(Parcel dest, Map<String, Integer> map) {
        if (map == null) {
            dest.writeInt(0);
            return;
        }
        dest.writeInt(map.size());
        for (Entry<String, Integer> entry : map.entrySet()) {
            dest.writeString((String) entry.getKey());
            dest.writeInt(((Integer) entry.getValue()).intValue());
        }
    }

    static Map<String, Integer> readStringIntMap(Parcel in) {
        int size = in.readInt();
        Map<String, Integer> map = new HashMap();
        while (true) {
            int size2 = size - 1;
            if (size <= 0) {
                return map;
            }
            map.put(in.readString(), Integer.valueOf(in.readInt()));
            size = size2;
        }
    }

    static <T extends Parcelable> void writeSet(Parcel dest, Set<T> set) {
        if (set == null) {
            dest.writeInt(0);
            return;
        }
        dest.writeInt(set.size());
        set.stream().forEach(new -$$Lambda$Utils$Cu3trYWUZE7O75pNHuKMUbHskAY(dest));
    }

    static <T> Set<T> createSet(Parcel in, Creator<T> c) {
        int size = in.readInt();
        Set<T> set = new HashSet();
        while (true) {
            int size2 = size - 1;
            if (size <= 0) {
                return set;
            }
            set.add(in.readTypedObject(c));
            size = size2;
        }
    }

    static void writeIntSet(Parcel dest, Set<Integer> set) {
        if (set == null) {
            dest.writeInt(0);
            return;
        }
        dest.writeInt(set.size());
        set.stream().forEach(new -$$Lambda$Utils$CpgxAbBJVMfl2IUCmgGvKDeq9_U(dest));
    }

    static Set<Integer> createIntSet(Parcel in) {
        return createSet(in, new Creator<Integer>() {
            public Integer createFromParcel(Parcel in) {
                return Integer.valueOf(in.readInt());
            }

            public Integer[] newArray(int size) {
                return new Integer[size];
            }
        });
    }

    static <T extends Parcelable> void writeTypedCollection(Parcel dest, Collection<T> coll) {
        ArrayList<T> list = null;
        if (coll != null) {
            if (coll instanceof ArrayList) {
                list = (ArrayList) coll;
            } else {
                list = new ArrayList(coll);
            }
        }
        dest.writeTypedList(list);
    }

    static void close(ICloseHandle handle) {
        try {
            handle.close();
        } catch (RemoteException ex) {
            ex.rethrowFromSystemServer();
        }
    }
}
