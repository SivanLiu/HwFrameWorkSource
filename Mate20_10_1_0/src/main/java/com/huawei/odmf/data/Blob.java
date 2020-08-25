package com.huawei.odmf.data;

import android.os.Parcel;
import android.os.Parcelable;
import com.huawei.odmf.exception.ODMFIllegalArgumentException;
import com.huawei.odmf.exception.ODMFRuntimeException;
import com.huawei.odmf.exception.ODMFUnsupportedOperationException;
import com.huawei.odmf.utils.LOG;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.Arrays;

public class Blob implements java.sql.Blob, Parcelable {
    public static final Creator<Blob> CREATOR = new Creator<Blob>() {
        /* class com.huawei.odmf.data.Blob.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public Blob createFromParcel(Parcel in) {
            return new Blob(in);
        }

        @Override // android.os.Parcelable.Creator
        public Blob[] newArray(int size) {
            return new Blob[size];
        }
    };
    private byte[] binaryData;
    private boolean isClosed;
    private final Object lock;

    public Blob() {
        this.binaryData = new byte[0];
        this.isClosed = false;
        this.lock = new Object();
    }

    public Blob(byte[] blob) {
        this.binaryData = new byte[0];
        this.isClosed = false;
        this.lock = new Object();
        if (blob != null) {
            this.binaryData = Arrays.copyOf(blob, blob.length);
        }
    }

    public Blob(Parcel in) {
        this.binaryData = new byte[0];
        this.isClosed = false;
        this.lock = new Object();
        int length = in.readInt();
        if (length != -1) {
            this.binaryData = new byte[length];
            in.readByteArray(this.binaryData);
            return;
        }
        this.binaryData = null;
    }

    private byte[] getBinaryData() {
        byte[] bArr;
        synchronized (this.lock) {
            bArr = this.binaryData;
        }
        return bArr;
    }

    private void checkClosed() throws SQLException {
        synchronized (this.lock) {
            if (this.isClosed) {
                throw new ODMFRuntimeException("The blob is not initialized");
            }
        }
    }

    @Override // java.sql.Blob
    public long length() throws SQLException {
        checkClosed();
        return (long) getBinaryData().length;
    }

    @Override // java.sql.Blob
    public byte[] getBytes(long pos, int length) throws SQLException {
        checkClosed();
        if (pos < 1) {
            throw new ODMFIllegalArgumentException();
        }
        int index = ((int) pos) - 1;
        if (index > this.binaryData.length) {
            throw new ODMFIllegalArgumentException();
        } else if (index + length > this.binaryData.length) {
            throw new ODMFIllegalArgumentException();
        } else {
            byte[] newData = new byte[length];
            System.arraycopy(getBinaryData(), index, newData, 0, length);
            return newData;
        }
    }

    @Override // java.sql.Blob
    public InputStream getBinaryStream() throws SQLException {
        checkClosed();
        return new ByteArrayInputStream(getBinaryData());
    }

    @Override // java.sql.Blob
    public long position(byte[] pattern, long start) throws SQLException {
        throw new ODMFUnsupportedOperationException();
    }

    @Override // java.sql.Blob
    public long position(java.sql.Blob pattern, long start) throws SQLException {
        throw new ODMFUnsupportedOperationException();
    }

    @Override // java.sql.Blob
    public int setBytes(long writeAt, byte[] bytes) throws SQLException {
        checkClosed();
        return setBytes(writeAt, bytes, 0, bytes.length);
    }

    @Override // java.sql.Blob
    public int setBytes(long writeAt, byte[] bytes, int offset, int length) throws SQLException {
        checkClosed();
        OutputStream bytesOut = setBinaryStream(writeAt);
        try {
            bytesOut.write(bytes, offset, length);
            this.binaryData = ((ByteArrayOutputStream) bytesOut).toByteArray();
            try {
                bytesOut.close();
            } catch (IOException e) {
                LOG.logE("An error occurred when close an outputStream in Blob");
            }
        } catch (IOException e2) {
            LOG.logE("An error occurred when write to Blob use an outputStream");
            try {
                bytesOut.close();
            } catch (IOException e3) {
                LOG.logE("An error occurred when close an outputStream in Blob");
            }
        } catch (Throwable th) {
            try {
                bytesOut.close();
            } catch (IOException e4) {
                LOG.logE("An error occurred when close an outputStream in Blob");
            }
            throw th;
        }
        return length;
    }

    @Override // java.sql.Blob
    public OutputStream setBinaryStream(long indexToWriteAt) throws SQLException {
        checkClosed();
        if (indexToWriteAt < 1) {
            throw new ODMFIllegalArgumentException();
        }
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        bytesOut.write(this.binaryData, 0, (int) (indexToWriteAt - 1));
        return bytesOut;
    }

    @Override // java.sql.Blob
    public void truncate(long len) throws SQLException {
        checkClosed();
        if (len < 0) {
            throw new ODMFIllegalArgumentException();
        } else if (len > ((long) this.binaryData.length)) {
            throw new ODMFIllegalArgumentException();
        } else {
            byte[] newData = new byte[((int) len)];
            System.arraycopy(getBinaryData(), 0, newData, 0, (int) len);
            this.binaryData = newData;
        }
    }

    @Override // java.sql.Blob
    public void free() throws SQLException {
        synchronized (this.lock) {
            this.binaryData = null;
            this.isClosed = true;
        }
    }

    @Override // java.sql.Blob
    public InputStream getBinaryStream(long pos, long length) throws SQLException {
        checkClosed();
        if (pos < 1) {
            throw new ODMFIllegalArgumentException();
        }
        int index = ((int) pos) - 1;
        if (index > this.binaryData.length) {
            throw new ODMFIllegalArgumentException();
        } else if (((long) index) + length <= ((long) this.binaryData.length)) {
            return new ByteArrayInputStream(getBinaryData(), index, (int) length);
        } else {
            throw new ODMFIllegalArgumentException();
        }
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        return Arrays.equals(getBinaryData(), ((Blob) object).getBinaryData());
    }

    public int hashCode() {
        return Arrays.hashCode(this.binaryData);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        if (this.binaryData != null) {
            out.writeInt(this.binaryData.length);
            out.writeByteArray(this.binaryData);
            return;
        }
        out.writeInt(-1);
    }
}
