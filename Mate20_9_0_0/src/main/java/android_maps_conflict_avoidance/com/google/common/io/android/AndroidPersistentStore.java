package android_maps_conflict_avoidance.com.google.common.io.android;

import android.content.Context;
import android_maps_conflict_avoidance.com.google.common.io.BasePersistentStore;
import android_maps_conflict_avoidance.com.google.common.io.PersistentStore;
import android_maps_conflict_avoidance.com.google.common.io.PersistentStore.PersistentStoreException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class AndroidPersistentStore extends BasePersistentStore implements PersistentStore {
    private Context context;
    private final Set<String> fileLockNames = Collections.synchronizedSet(new HashSet());

    public AndroidPersistentStore(Context c) {
        this.context = c;
    }

    private static String makeFilename(String s) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DATA_");
        stringBuilder.append(s);
        return stringBuilder.toString();
    }

    private static String unMakeFilename(String filename) {
        if (filename.startsWith("DATA_")) {
            return filename.substring(5);
        }
        return null;
    }

    public boolean deleteBlock(String name) {
        return this.context.deleteFile(makeFilename(name));
    }

    public void deleteAllBlocks(String namePrefix) {
        if (this.context != null) {
            String[] list = this.context.fileList();
            if (list != null) {
                int i = 0;
                while (i < list.length) {
                    if (list[i] != null && list[i].startsWith(makeFilename(namePrefix))) {
                        this.context.deleteFile(list[i]);
                    }
                    i++;
                }
            }
        }
    }

    public int writeBlockX(byte[] data, String name) throws PersistentStoreException {
        if (data == null) {
            try {
                data = new byte[0];
            } catch (FileNotFoundException e) {
                throw new PersistentStoreException(e.getMessage(), -1);
            } catch (IOException e2) {
                throw new PersistentStoreException(e2.getMessage(), -1);
            }
        }
        FileNotFoundException e22 = this.context.openFileOutput(makeFilename(name), 0);
        e22.write(data);
        e22.close();
        return 4096 * (1 + ((data.length - 1) / 4096));
    }

    public int writeBlock(byte[] data, String name) {
        try {
            return writeBlockX(data, name);
        } catch (PersistentStoreException e) {
            return e.getType();
        }
    }

    public byte[] readBlock(String name) {
        try {
            FileInputStream fis = this.context.openFileInput(makeFilename(name));
            int length = fis.available();
            byte[] data = new byte[length];
            fis.read(data, 0, length);
            fis.close();
            return data;
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e2) {
            return null;
        }
    }

    public String[] listBlocks(String namePrefix) {
        String[] list = this.context.fileList();
        String[] temp = new String[list.length];
        int j = 0;
        for (String realName : list) {
            String realName2 = unMakeFilename(realName2);
            if (realName2 != null && realName2.startsWith(namePrefix)) {
                int j2 = j + 1;
                temp[j] = realName2;
                j = j2;
            }
        }
        String[] names = new String[j];
        System.arraycopy(temp, 0, names, 0, j);
        return names;
    }
}
