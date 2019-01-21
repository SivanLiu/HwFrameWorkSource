package com.huawei.libcore.io;

import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

public class ExternalStorageRandomAccessFile extends RandomAccessFile {
    public ExternalStorageRandomAccessFile(String name, String mode) throws FileNotFoundException {
        this(name != null ? new ExternalStorageFile(name) : null, mode);
    }

    public ExternalStorageRandomAccessFile(File file, String mode) throws FileNotFoundException {
        super(file instanceof ExternalStorageFile ? ((ExternalStorageFile) file).getInternalFile() : file, mode);
        try {
            Os.access(file.getAbsolutePath(), OsConstants.F_OK);
        } catch (ErrnoException e) {
        }
    }
}
