package android_maps_conflict_avoidance.com.google.common.io.android;

import android.util.Log;
import android_maps_conflict_avoidance.com.google.common.io.BasePersistentStore;
import android_maps_conflict_avoidance.com.google.common.io.PersistentStore;
import android_maps_conflict_avoidance.com.google.common.io.PersistentStore.PersistentStoreException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class AndroidFixedPersistentStore extends BasePersistentStore implements PersistentStore {
    private File baseFile;

    public AndroidFixedPersistentStore(String basePath) {
        this.baseFile = new File(basePath);
        StringBuilder stringBuilder;
        if (!this.baseFile.isDirectory()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Directory ");
            stringBuilder.append(basePath);
            stringBuilder.append(" must already exist");
            throw new IllegalStateException(stringBuilder.toString());
        } else if (!this.baseFile.canWrite()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Directory ");
            stringBuilder.append(basePath);
            stringBuilder.append(" must be writeable");
            throw new IllegalStateException(stringBuilder.toString());
        } else if (!this.baseFile.canRead()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Directory ");
            stringBuilder.append(basePath);
            stringBuilder.append(" must be readable");
            throw new IllegalStateException(stringBuilder.toString());
        }
    }

    private String makeFilename(String s) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getPrefix());
        stringBuilder.append(s);
        return stringBuilder.toString();
    }

    private File makeFile(String s) {
        return new File(makeFilename(s));
    }

    private String unMakeFilename(String filename) {
        if (filename.startsWith(getPrefix())) {
            return filename.substring(getPrefix().length());
        }
        return null;
    }

    public boolean deleteBlock(String name) {
        return makeFile(name).delete();
    }

    public void deleteAllBlocks(String namePrefix) {
        File[] files = this.baseFile.getAbsoluteFile().listFiles();
        int i = 0;
        while (i < files.length) {
            if (files[i].getAbsolutePath().startsWith(makeFilename(namePrefix)) && !files[i].delete()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Couldn't delete file: ");
                stringBuilder.append(files[i].getAbsolutePath());
                Log.w("Fixed_Persistence_Store", stringBuilder.toString());
            }
            i++;
        }
    }

    public int writeBlockX(byte[] data, String name) throws PersistentStoreException {
        try {
            FileOutputStream fos = new FileOutputStream(makeFile(name));
            fos.write(data);
            fos.close();
            return 4096 * (1 + ((data.length - 1) / 4096));
        } catch (FileNotFoundException e) {
            throw new PersistentStoreException(e.getMessage(), -1);
        } catch (IOException e2) {
            throw new PersistentStoreException(e2.getMessage(), -1);
        }
    }

    public int writeBlock(byte[] data, String name) {
        StringBuilder stringBuilder;
        try {
            FileOutputStream fos = new FileOutputStream(makeFile(name));
            fos.write(data);
            fos.close();
            return 4096 * (1 + ((data.length - 1) / 4096));
        } catch (FileNotFoundException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Couldn't write block:  ");
            stringBuilder.append(e.getMessage());
            Log.w("Fixed_Persistence_Store", stringBuilder.toString());
            return -1;
        } catch (IOException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Couldn't write block:  ");
            stringBuilder.append(e2.getMessage());
            Log.w("Fixed_Persistence_Store", stringBuilder.toString());
            return -1;
        }
    }

    public byte[] readBlock(String name) {
        StringBuilder stringBuilder;
        try {
            FileInputStream fis = new FileInputStream(makeFile(name));
            int length = fis.available();
            byte[] data = new byte[length];
            if (fis.read(data, 0, length) < length) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Didn't read full file:  ");
                stringBuilder2.append(name);
                Log.w("Fixed_Persistence_Store", stringBuilder2.toString());
            }
            fis.close();
            return data;
        } catch (FileNotFoundException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Couldn't find file:  ");
            stringBuilder.append(e.getMessage());
            Log.w("Fixed_Persistence_Store", stringBuilder.toString());
            return null;
        } catch (IOException e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Couldn't read file:  ");
            stringBuilder.append(e2.getMessage());
            Log.w("Fixed_Persistence_Store", stringBuilder.toString());
            return null;
        }
    }

    public String[] listBlocks(String namePrefix) {
        File[] files = this.baseFile.getAbsoluteFile().listFiles();
        String[] temp = new String[files.length];
        int j = 0;
        for (int i = 0; i < files.length; i++) {
            if (files[i].getAbsolutePath().startsWith(makeFilename(namePrefix))) {
                String realName = unMakeFilename(files[i].getAbsolutePath());
                if (realName != null) {
                    int j2 = j + 1;
                    temp[j] = realName;
                    j = j2;
                }
            }
        }
        String[] names = new String[j];
        System.arraycopy(temp, 0, names, 0, j);
        return names;
    }

    protected String getPrefix() {
        return new File(this.baseFile, "FIXED_DATA_").getAbsolutePath();
    }
}
