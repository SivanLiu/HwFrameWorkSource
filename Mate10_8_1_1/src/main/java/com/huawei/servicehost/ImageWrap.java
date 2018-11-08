package com.huawei.servicehost;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.util.Log;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

public class ImageWrap implements Parcelable {
    public static final Creator<ImageWrap> CREATOR = new Creator<ImageWrap>() {
        public ImageWrap createFromParcel(Parcel in) {
            long nativePtr = ImageWrap.nativeReadFromParcel(in);
            if (nativePtr != 0) {
                return new ImageWrap(nativePtr);
            }
            return null;
        }

        public ImageWrap[] newArray(int size) {
            return new ImageWrap[size];
        }
    };
    private static final boolean DEBUG = false;
    private static final String TAG = "ImageWrap";
    private long mNativePtr;

    private static native void nativeDispose(long j);

    private static native long nativeGetCapacity(long j);

    private static native ByteBuffer nativeGetData(long j);

    private static native long nativeInit();

    private static native long nativeReadFromParcel(Parcel parcel);

    private static native void nativeSetData(long j, ByteBuffer byteBuffer);

    private static native void nativeWriteToParcel(long j, Parcel parcel);

    static {
        System.loadLibrary("ServiceHost_jni");
    }

    protected void finalize() throws Throwable {
        try {
            release();
        } finally {
            super.finalize();
        }
    }

    private ImageWrap() {
        this.mNativePtr = nativeInit();
    }

    private ImageWrap(long nativePtr) {
        this.mNativePtr = nativePtr;
    }

    private ImageWrap(ImageWrap val) {
    }

    public void swap(ImageWrap val) {
        long nPtr = val.mNativePtr;
        val.mNativePtr = this.mNativePtr;
        this.mNativePtr = nPtr;
    }

    public void release() {
        if (this.mNativePtr != 0) {
            nativeDispose(this.mNativePtr);
            this.mNativePtr = 0;
        }
    }

    public long getCapacity() {
        if (this.mNativePtr != 0) {
            return nativeGetCapacity(this.mNativePtr);
        }
        Log.e(TAG, "invalid ImageWrap.");
        return 0;
    }

    public ByteBuffer getData() {
        if (this.mNativePtr != 0) {
            return nativeGetData(this.mNativePtr);
        }
        Log.e(TAG, "invalid ImageWrap.");
        return null;
    }

    public void setData(ByteBuffer byteBuffer) {
        if (this.mNativePtr != 0) {
            nativeSetData(this.mNativePtr, byteBuffer);
        }
    }

    public void writeDataToFile(String file) {
        RuntimeException e;
        Exception e2;
        Throwable th;
        ByteBuffer data = getData();
        if (data == null) {
            Log.e(TAG, "getData failed.");
            return;
        }
        BufferedOutputStream bufferedOutputStream = null;
        try {
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
            try {
                byte[] b = new byte[data.capacity()];
                data.rewind();
                data.get(b);
                out.write(b);
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                bufferedOutputStream = out;
            } catch (RuntimeException e3) {
                e = e3;
                bufferedOutputStream = out;
                e.printStackTrace();
                if (bufferedOutputStream != null) {
                    try {
                        bufferedOutputStream.close();
                    } catch (IOException e12) {
                        e12.printStackTrace();
                    }
                }
            } catch (Exception e4) {
                e2 = e4;
                bufferedOutputStream = out;
                try {
                    e2.printStackTrace();
                    if (bufferedOutputStream != null) {
                        try {
                            bufferedOutputStream.close();
                        } catch (IOException e122) {
                            e122.printStackTrace();
                        }
                    }
                } catch (Throwable th2) {
                    th = th2;
                    if (bufferedOutputStream != null) {
                        try {
                            bufferedOutputStream.close();
                        } catch (IOException e1222) {
                            e1222.printStackTrace();
                        }
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                bufferedOutputStream = out;
                if (bufferedOutputStream != null) {
                    bufferedOutputStream.close();
                }
                throw th;
            }
        } catch (RuntimeException e5) {
            e = e5;
            e.printStackTrace();
            if (bufferedOutputStream != null) {
                bufferedOutputStream.close();
            }
        } catch (Exception e6) {
            e2 = e6;
            e2.printStackTrace();
            if (bufferedOutputStream != null) {
                bufferedOutputStream.close();
            }
        }
    }

    public ByteBuffer readDataFromFile(String file) {
        FileNotFoundException e;
        IOException e2;
        Throwable th;
        FileInputStream fileInputStream = null;
        MappedByteBuffer byteBuffer = null;
        FileChannel fileChannel = null;
        try {
            FileInputStream fileInputStream2 = new FileInputStream(file);
            try {
                fileChannel = fileInputStream2.getChannel();
                byteBuffer = fileChannel.map(MapMode.READ_ONLY, 0, fileChannel.size());
                if (fileChannel != null) {
                    try {
                        fileChannel.close();
                    } catch (Exception e3) {
                        e3.printStackTrace();
                    }
                }
                if (fileInputStream2 != null) {
                    fileInputStream2.close();
                }
                fileInputStream = fileInputStream2;
            } catch (FileNotFoundException e4) {
                e = e4;
                fileInputStream = fileInputStream2;
                e.printStackTrace();
                if (fileChannel != null) {
                    try {
                        fileChannel.close();
                    } catch (Exception e32) {
                        e32.printStackTrace();
                    }
                }
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                setData(byteBuffer);
                return byteBuffer;
            } catch (IOException e5) {
                e2 = e5;
                fileInputStream = fileInputStream2;
                try {
                    e2.printStackTrace();
                    if (fileChannel != null) {
                        try {
                            fileChannel.close();
                        } catch (Exception e322) {
                            e322.printStackTrace();
                        }
                    }
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                    setData(byteBuffer);
                    return byteBuffer;
                } catch (Throwable th2) {
                    th = th2;
                    if (fileChannel != null) {
                        try {
                            fileChannel.close();
                        } catch (Exception e3222) {
                            e3222.printStackTrace();
                            throw th;
                        }
                    }
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                fileInputStream = fileInputStream2;
                if (fileChannel != null) {
                    fileChannel.close();
                }
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                throw th;
            }
        } catch (FileNotFoundException e6) {
            e = e6;
            e.printStackTrace();
            if (fileChannel != null) {
                fileChannel.close();
            }
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            setData(byteBuffer);
            return byteBuffer;
        } catch (IOException e7) {
            e2 = e7;
            e2.printStackTrace();
            if (fileChannel != null) {
                fileChannel.close();
            }
            if (fileInputStream != null) {
                fileInputStream.close();
            }
            setData(byteBuffer);
            return byteBuffer;
        }
        setData(byteBuffer);
        return byteBuffer;
    }

    public boolean equals(Object o) {
        boolean z = DEBUG;
        if (!(o instanceof ImageWrap)) {
            return DEBUG;
        }
        if (this.mNativePtr == ((ImageWrap) o).mNativePtr) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        return (int) ((this.mNativePtr * 31) + 17);
    }

    public String toString() {
        return "";
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        if (this.mNativePtr == 0) {
            throw new IllegalStateException("This ImageWrap has been destroyed and cannot be written to a parcel.");
        }
        nativeWriteToParcel(this.mNativePtr, out);
    }

    public void readFromParcel(Parcel in) {
        long nativePtr = nativeReadFromParcel(in);
        if (nativePtr != 0) {
            this.mNativePtr = nativePtr;
        } else {
            Log.e(TAG, "ImageWrap readFromParcel failed.");
            throw new IllegalStateException("ImageWrap readFromParcel failed.");
        }
    }
}
