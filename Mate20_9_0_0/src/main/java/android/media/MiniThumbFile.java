package android.media;

import android.app.backup.FullBackup;
import android.app.job.JobInfo;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Hashtable;

public class MiniThumbFile {
    public static final int BYTES_PER_MINTHUMB = 10000;
    private static final int HEADER_SIZE = 13;
    private static final int MINI_THUMB_DATA_FILE_VERSION = 4;
    private static final String TAG = "MiniThumbFile";
    private static final Hashtable<String, MiniThumbFile> sThumbFiles = new Hashtable();
    private long MAX_THUMB_COUNT = 5000;
    private ByteBuffer mBuffer;
    private final HashMap<Long, FileChannel> mChannels = new HashMap();
    private ByteBuffer mEmptyBuffer;
    private final HashMap<Long, RandomAccessFile> mMiniThumbFiles = new HashMap();
    private Uri mUri;

    public static synchronized void reset() {
        synchronized (MiniThumbFile.class) {
            for (MiniThumbFile file : sThumbFiles.values()) {
                file.deactivate();
            }
            sThumbFiles.clear();
        }
    }

    public static synchronized MiniThumbFile instance(Uri uri) {
        MiniThumbFile file;
        synchronized (MiniThumbFile.class) {
            String type = (String) uri.getPathSegments().get(1);
            file = (MiniThumbFile) sThumbFiles.get(type);
            if (file == null) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("content://media/external/");
                stringBuilder.append(type);
                stringBuilder.append("/media");
                file = new MiniThumbFile(Uri.parse(stringBuilder.toString()));
                sThumbFiles.put(type, file);
            }
        }
        return file;
    }

    private String randomAccessFilePath(int version) {
        String directoryName = new StringBuilder();
        directoryName.append(Environment.getExternalStorageDirectory().toString());
        directoryName.append("/DCIM/.thumbnails");
        directoryName = directoryName.toString();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(directoryName);
        stringBuilder.append("/.thumbdata");
        stringBuilder.append(version);
        stringBuilder.append("-");
        stringBuilder.append(this.mUri.hashCode());
        return stringBuilder.toString();
    }

    private void removeOldFile() {
        File oldFile = new File(randomAccessFilePath(3));
        if (oldFile.exists()) {
            try {
                oldFile.delete();
            } catch (SecurityException e) {
            }
        }
    }

    private RandomAccessFile miniThumbDataFile(long id) {
        long fileindex = getFileIndex(id);
        RandomAccessFile miniThumbFile = (RandomAccessFile) this.mMiniThumbFiles.get(Long.valueOf(fileindex));
        if (miniThumbFile == null) {
            removeOldFile();
            String path = randomAccessFilePath(4, id);
            File directory = new File(path).getParentFile();
            if (!(directory.isDirectory() || directory.mkdirs())) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unable to create .thumbnails directory ");
                stringBuilder.append(directory.toString());
                Log.e(str, stringBuilder.toString());
            }
            File f = new File(path);
            try {
                miniThumbFile = new RandomAccessFile(f, "rw");
            } catch (IOException e) {
                try {
                    miniThumbFile = new RandomAccessFile(f, FullBackup.ROOT_TREE_TOKEN);
                } catch (IOException e2) {
                }
            }
            if (miniThumbFile != null) {
                FileChannel channel = miniThumbFile.getChannel();
                this.mMiniThumbFiles.put(Long.valueOf(fileindex), miniThumbFile);
                this.mChannels.put(Long.valueOf(fileindex), channel);
            }
        }
        return miniThumbFile;
    }

    private MiniThumbFile(Uri uri) {
        this.mUri = uri;
        this.mBuffer = ByteBuffer.allocateDirect(10000);
        this.mEmptyBuffer = ByteBuffer.allocateDirect(10000);
    }

    public synchronized void deactivate() {
        for (Long key : this.mMiniThumbFiles.keySet()) {
            RandomAccessFile miniThumbFile = (RandomAccessFile) this.mMiniThumbFiles.get(key);
            FileChannel channel = (FileChannel) this.mChannels.get(key);
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException ex) {
                    Log.e(TAG, "deactivate: IOException!", ex);
                }
            }
            if (miniThumbFile != null) {
                miniThumbFile.close();
            }
        }
        this.mMiniThumbFiles.clear();
        this.mChannels.clear();
    }

    public synchronized long getMagic(long id) {
        long j;
        if (miniThumbDataFile(id) != null) {
            long pos = getFilePos(id);
            FileLock lock = null;
            try {
                this.mBuffer.clear();
                this.mBuffer.limit(9);
                FileChannel fileChannel = getFileChannel(id);
                FileChannel channel = fileChannel;
                lock = fileChannel.lock(pos, 9, true);
                if (channel.read(this.mBuffer, pos) == 9) {
                    this.mBuffer.position(0);
                    if (this.mBuffer.get() == (byte) 1) {
                        j = this.mBuffer.getLong();
                        if (lock != null) {
                            try {
                                lock.release();
                            } catch (IOException e) {
                            }
                        }
                    }
                }
                if (lock != null) {
                    try {
                        lock.release();
                    } catch (IOException e2) {
                    }
                }
            } catch (IOException ex) {
                Log.v(TAG, "Got exception checking file magic: ", ex);
                if (lock != null) {
                    lock.release();
                }
            } catch (RuntimeException ex2) {
                try {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Got exception when reading magic, id = ");
                    stringBuilder.append(id);
                    stringBuilder.append(", disk full or mount read-only? ");
                    stringBuilder.append(ex2.getClass());
                    Log.e(str, stringBuilder.toString());
                    if (lock != null) {
                        lock.release();
                    }
                } catch (Throwable th) {
                    if (lock != null) {
                        try {
                            lock.release();
                        } catch (IOException e3) {
                        }
                    }
                }
            }
        }
        return 0;
        return j;
    }

    public synchronized void eraseMiniThumb(long id) {
        if (miniThumbDataFile(id) != null) {
            long pos = getFilePos(id);
            FileChannel channel = getFileChannel(id);
            FileLock lock = null;
            try {
                this.mBuffer.clear();
                this.mBuffer.limit(9);
                lock = channel.lock(pos, JobInfo.MIN_BACKOFF_MILLIS, false);
                if (channel.read(this.mBuffer, pos) == 9) {
                    this.mBuffer.position(0);
                    if (this.mBuffer.get() == (byte) 1) {
                        if (this.mBuffer.getLong() == 0) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("no thumbnail for id ");
                            stringBuilder.append(id);
                            Log.i(str, stringBuilder.toString());
                            if (lock != null) {
                                try {
                                    lock.release();
                                } catch (IOException e) {
                                }
                            }
                        } else {
                            channel.write(this.mEmptyBuffer, pos);
                        }
                    }
                }
                if (lock != null) {
                    try {
                        lock.release();
                    } catch (IOException e2) {
                    }
                }
            } catch (IOException ex) {
                Log.v(TAG, "Got exception checking file magic: ", ex);
                if (lock != null) {
                    lock.release();
                }
                return;
            } catch (RuntimeException ex2) {
                try {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Got exception when reading magic, id = ");
                    stringBuilder2.append(id);
                    stringBuilder2.append(", disk full or mount read-only? ");
                    stringBuilder2.append(ex2.getClass());
                    Log.e(str2, stringBuilder2.toString());
                    if (lock != null) {
                        lock.release();
                    }
                    return;
                } catch (Throwable th) {
                    if (lock != null) {
                        try {
                            lock.release();
                        } catch (IOException e3) {
                        }
                    }
                }
            }
        }
    }

    public synchronized void saveMiniThumbToFile(byte[] data, long id, long magic) throws IOException {
        String str;
        StringBuilder stringBuilder;
        byte[] bArr = data;
        long j = id;
        synchronized (this) {
            if (miniThumbDataFile(j) == null) {
                return;
            }
            long pos = getFilePos(j);
            FileLock lock = null;
            if (bArr != null) {
                try {
                    if (bArr.length <= 9987) {
                        this.mBuffer.clear();
                        this.mBuffer.put((byte) 1);
                        this.mBuffer.putLong(magic);
                        this.mBuffer.putInt(bArr.length);
                        this.mBuffer.put(bArr);
                        this.mBuffer.flip();
                        FileChannel channel = getFileChannel(j);
                        lock = channel.lock(pos, JobInfo.MIN_BACKOFF_MILLIS, false);
                        channel.write(this.mBuffer, pos);
                    } else if (lock != null) {
                        try {
                            lock.release();
                        } catch (IOException e) {
                        }
                    }
                } catch (IOException ex) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("couldn't save mini thumbnail data for ");
                    stringBuilder.append(j);
                    stringBuilder.append("; ");
                    Log.e(str, stringBuilder.toString(), ex);
                    throw ex;
                } catch (RuntimeException ex2) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("couldn't save mini thumbnail data for ");
                    stringBuilder.append(j);
                    stringBuilder.append("; disk full or mount read-only? ");
                    stringBuilder.append(ex2.getClass());
                    Log.e(str, stringBuilder.toString());
                    if (lock != null) {
                        try {
                            lock.release();
                        } catch (IOException e2) {
                        }
                    }
                    return;
                } catch (Throwable th) {
                    Throwable th2 = th;
                    if (lock != null) {
                        try {
                            lock.release();
                        } catch (IOException e3) {
                        }
                    }
                }
            }
            if (lock != null) {
                lock.release();
            }
        }
        return;
    }

    public synchronized byte[] getMiniThumbFromFile(long id, byte[] data) {
        String str;
        StringBuilder stringBuilder;
        long j = id;
        byte[] bArr = data;
        synchronized (this) {
            if (miniThumbDataFile(id) == null) {
                return null;
            }
            long pos = getFilePos(id);
            FileLock lock = null;
            try {
                this.mBuffer.clear();
                FileChannel channel = getFileChannel(id);
                FileChannel channel2 = channel;
                lock = channel.lock(pos, JobInfo.MIN_BACKOFF_MILLIS, 1);
                int size = channel2.read(this.mBuffer, pos);
                if (size > 13) {
                    this.mBuffer.position(0);
                    byte flag = this.mBuffer.get();
                    long magic = this.mBuffer.getLong();
                    int length = this.mBuffer.getInt();
                    if (size >= 13 + length && length != 0 && magic != 0 && flag == (byte) 1 && bArr.length >= length) {
                        this.mBuffer.get(bArr, 0, length);
                        if (lock != null) {
                            try {
                                lock.release();
                            } catch (IOException e) {
                            }
                        }
                    }
                }
                if (lock != null) {
                    try {
                        lock.release();
                    } catch (IOException e2) {
                    }
                }
            } catch (IOException ex) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("got exception when reading thumbnail id=");
                stringBuilder.append(j);
                stringBuilder.append(", exception: ");
                stringBuilder.append(ex);
                Log.w(str, stringBuilder.toString());
                if (lock != null) {
                    lock.release();
                }
                return null;
            } catch (RuntimeException ex2) {
                try {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Got exception when reading thumbnail, id = ");
                    stringBuilder.append(j);
                    stringBuilder.append(", disk full or mount read-only? ");
                    stringBuilder.append(ex2.getClass());
                    Log.e(str, stringBuilder.toString());
                    if (lock != null) {
                        lock.release();
                    }
                    return null;
                } catch (Throwable th) {
                    Throwable th2 = th;
                    if (lock != null) {
                        try {
                            lock.release();
                        } catch (IOException e3) {
                        }
                    }
                }
            }
        }
        return bArr;
    }

    private long getFileIndex(long id) {
        return id / this.MAX_THUMB_COUNT;
    }

    private FileChannel getFileChannel(long id) {
        return (FileChannel) this.mChannels.get(Long.valueOf(getFileIndex(id)));
    }

    private long getFilePos(long id) {
        return (id % this.MAX_THUMB_COUNT) * JobInfo.MIN_BACKOFF_MILLIS;
    }

    private String randomAccessFilePath(int version, long id) {
        long fileindex = getFileIndex(id);
        String directoryName = new StringBuilder();
        directoryName.append(Environment.getExternalStorageDirectory().toString());
        directoryName.append("/DCIM/.thumbnails");
        directoryName = directoryName.toString();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(directoryName);
        stringBuilder.append("/.thumbdata");
        stringBuilder.append(version);
        stringBuilder.append("-");
        stringBuilder.append(this.mUri.hashCode());
        stringBuilder.append("_");
        stringBuilder.append(fileindex);
        return stringBuilder.toString();
    }
}
