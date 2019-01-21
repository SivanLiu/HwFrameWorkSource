package android.view.inputmethod;

import android.os.Parcel;
import android.util.Slog;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class InputMethodSubtypeArray {
    private static final String TAG = "InputMethodSubtypeArray";
    private volatile byte[] mCompressedData;
    private final int mCount;
    private volatile int mDecompressedSize;
    private volatile InputMethodSubtype[] mInstance;
    private final Object mLockObject = new Object();

    public InputMethodSubtypeArray(List<InputMethodSubtype> subtypes) {
        if (subtypes == null) {
            this.mCount = 0;
            return;
        }
        this.mCount = subtypes.size();
        this.mInstance = (InputMethodSubtype[]) subtypes.toArray(new InputMethodSubtype[this.mCount]);
    }

    public InputMethodSubtypeArray(Parcel source) {
        this.mCount = source.readInt();
        if (this.mCount > 0) {
            this.mDecompressedSize = source.readInt();
            this.mCompressedData = source.createByteArray();
        }
    }

    public void writeToParcel(Parcel dest) {
        if (this.mCount == 0) {
            dest.writeInt(this.mCount);
            return;
        }
        byte[] compressedData = this.mCompressedData;
        int decompressedSize = this.mDecompressedSize;
        if (compressedData == null && decompressedSize == 0) {
            synchronized (this.mLockObject) {
                compressedData = this.mCompressedData;
                decompressedSize = this.mDecompressedSize;
                if (compressedData == null && decompressedSize == 0) {
                    byte[] decompressedData = marshall(this.mInstance);
                    compressedData = compress(decompressedData);
                    if (compressedData == null) {
                        decompressedSize = -1;
                        Slog.i(TAG, "Failed to compress data.");
                    } else {
                        decompressedSize = decompressedData.length;
                    }
                    this.mDecompressedSize = decompressedSize;
                    this.mCompressedData = compressedData;
                }
            }
        }
        if (compressedData == null || decompressedSize <= 0) {
            Slog.i(TAG, "Unexpected state. Behaving as an empty array.");
            dest.writeInt(0);
        } else {
            dest.writeInt(this.mCount);
            dest.writeInt(decompressedSize);
            dest.writeByteArray(compressedData);
        }
    }

    public InputMethodSubtype get(int index) {
        if (index < 0 || this.mCount <= index) {
            throw new ArrayIndexOutOfBoundsException();
        }
        InputMethodSubtype[] instance = this.mInstance;
        if (instance == null) {
            synchronized (this.mLockObject) {
                instance = this.mInstance;
                if (instance == null) {
                    byte[] decompressedData = decompress(this.mCompressedData, this.mDecompressedSize);
                    this.mCompressedData = null;
                    this.mDecompressedSize = 0;
                    if (decompressedData != null) {
                        instance = unmarshall(decompressedData);
                    } else {
                        Slog.e(TAG, "Failed to decompress data. Returns null as fallback.");
                        instance = new InputMethodSubtype[this.mCount];
                    }
                    this.mInstance = instance;
                }
            }
        }
        return instance[index];
    }

    public int getCount() {
        return this.mCount;
    }

    private static byte[] marshall(InputMethodSubtype[] array) {
        Parcel parcel = null;
        try {
            parcel = Parcel.obtain();
            parcel.writeTypedArray(array, 0);
            byte[] marshall = parcel.marshall();
            return marshall;
        } finally {
            if (parcel != null) {
                parcel.recycle();
            }
        }
    }

    private static InputMethodSubtype[] unmarshall(byte[] data) {
        Parcel parcel = null;
        try {
            parcel = Parcel.obtain();
            parcel.unmarshall(data, 0, data.length);
            parcel.setDataPosition(0);
            InputMethodSubtype[] inputMethodSubtypeArr = (InputMethodSubtype[]) parcel.createTypedArray(InputMethodSubtype.CREATOR);
            return inputMethodSubtypeArr;
        } finally {
            if (parcel != null) {
                parcel.recycle();
            }
        }
    }

    private static byte[] compress(byte[] data) {
        Throwable th;
        Throwable th2;
        Throwable th3;
        try {
            ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
            Throwable th4;
            try {
                GZIPOutputStream zipper = new GZIPOutputStream(resultStream);
                try {
                    zipper.write(data);
                    zipper.finish();
                    byte[] toByteArray = resultStream.toByteArray();
                    $closeResource(null, zipper);
                    $closeResource(null, resultStream);
                    return toByteArray;
                } catch (Throwable th22) {
                    th3 = th22;
                    th22 = th;
                    th = th3;
                }
                $closeResource(th, resultStream);
                throw th4;
                $closeResource(th22, zipper);
                throw th;
            } catch (Throwable th5) {
                th3 = th5;
                th5 = th4;
                th4 = th3;
            }
        } catch (Exception e) {
            Slog.e(TAG, "Failed to compress the data.", e);
            return null;
        }
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    private static byte[] decompress(byte[] data, int expectedSize) {
        Throwable th;
        Throwable th2;
        Throwable th3;
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
            Throwable th4;
            try {
                GZIPInputStream unzipper = new GZIPInputStream(inputStream);
                try {
                    byte[] result = new byte[expectedSize];
                    int totalReadBytes = 0;
                    while (totalReadBytes < result.length) {
                        int readBytes = unzipper.read(result, totalReadBytes, result.length - totalReadBytes);
                        if (readBytes < 0) {
                            break;
                        }
                        totalReadBytes += readBytes;
                    }
                    if (expectedSize != totalReadBytes) {
                        $closeResource(null, unzipper);
                        $closeResource(null, inputStream);
                        return null;
                    }
                    $closeResource(null, unzipper);
                    $closeResource(null, inputStream);
                    return result;
                } catch (Throwable th22) {
                    th3 = th22;
                    th22 = th;
                    th = th3;
                }
                $closeResource(th, inputStream);
                throw th4;
                $closeResource(th22, unzipper);
                throw th;
            } catch (Throwable th5) {
                th3 = th5;
                th5 = th4;
                th4 = th3;
            }
        } catch (Exception e) {
            Slog.e(TAG, "Failed to decompress the data.", e);
            return null;
        }
    }
}
