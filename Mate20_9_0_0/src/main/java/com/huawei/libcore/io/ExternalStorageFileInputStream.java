package com.huawei.libcore.io;

import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class ExternalStorageFileInputStream extends FileInputStream {
    public ExternalStorageFileInputStream(String name) throws FileNotFoundException {
        this(name != null ? new ExternalStorageFile(name) : null);
    }

    public ExternalStorageFileInputStream(File file) throws FileNotFoundException {
        super(file instanceof ExternalStorageFile ? ((ExternalStorageFile) file).getInternalFile() : file);
        try {
            Os.access(file.getAbsolutePath(), OsConstants.F_OK);
        } catch (ErrnoException e) {
        }
    }

    public ExternalStorageFileInputStream(FileDescriptor fdObj) {
        super(fdObj, false);
    }
}
