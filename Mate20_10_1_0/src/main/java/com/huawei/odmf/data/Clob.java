package com.huawei.odmf.data;

import android.os.Parcel;
import android.os.Parcelable;
import com.huawei.odmf.exception.ODMFIllegalArgumentException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class Clob implements java.sql.Clob, Parcelable {
    public static final Creator<Clob> CREATOR = new Creator<Clob>() {
        /* class com.huawei.odmf.data.Clob.AnonymousClass1 */

        @Override // android.os.Parcelable.Creator
        public Clob createFromParcel(Parcel in) {
            return new Clob(in);
        }

        @Override // android.os.Parcelable.Creator
        public Clob[] newArray(int size) {
            return new Clob[size];
        }
    };
    private StringBuffer buffer;

    public Clob(String charData) {
        this.buffer = new StringBuffer(charData);
    }

    public Clob(Parcel in) {
        this.buffer = new StringBuffer(in.readString());
    }

    @Override // java.sql.Clob
    public long length() throws SQLException {
        if (this.buffer != null) {
            return (long) this.buffer.length();
        }
        return 0;
    }

    @Override // java.sql.Clob
    public String getSubString(long startPos, int length) throws SQLException {
        int startPosition = ((int) startPos) - 1;
        if (startPosition < 0) {
            throw new ODMFIllegalArgumentException();
        }
        int endPosition = startPosition + length;
        if (this.buffer == null) {
            return null;
        }
        if (((long) endPosition) <= length()) {
            return this.buffer.substring(startPosition, endPosition);
        }
        throw new ODMFIllegalArgumentException();
    }

    @Override // java.sql.Clob
    public Reader getCharacterStream() throws SQLException {
        if (this.buffer != null) {
            return new StringReader(this.buffer.toString());
        }
        return null;
    }

    @Override // java.sql.Clob
    public InputStream getAsciiStream() throws SQLException {
        if (this.buffer != null) {
            return new ByteArrayInputStream(this.buffer.toString().getBytes(StandardCharsets.UTF_8));
        }
        return null;
    }

    @Override // java.sql.Clob
    public long position(String stringToFind, long startPos) throws SQLException {
        int startPosition = ((int) startPos) - 1;
        if (startPosition < 0) {
            throw new ODMFIllegalArgumentException();
        } else if (this.buffer == null) {
            return -1;
        } else {
            if (startPosition > this.buffer.length()) {
                throw new ODMFIllegalArgumentException();
            }
            int index = this.buffer.indexOf(stringToFind, startPosition);
            if (index == -1) {
                return -1;
            }
            return (long) (index + 1);
        }
    }

    @Override // java.sql.Clob
    public long position(java.sql.Clob clob, long startPos) throws SQLException {
        return position(clob.getSubString(1, (int) clob.length()), startPos);
    }

    @Override // java.sql.Clob
    public int setString(long pos, String str) throws SQLException {
        int startPosition = ((int) pos) - 1;
        if (startPosition < 0) {
            throw new ODMFIllegalArgumentException();
        } else if (str == null) {
            throw new ODMFIllegalArgumentException();
        } else {
            int strLength = str.length();
            this.buffer.replace(startPosition, startPosition + strLength, str);
            return strLength;
        }
    }

    @Override // java.sql.Clob
    public int setString(long pos, String str, int offset, int len) throws SQLException {
        int startPosition = ((int) pos) - 1;
        if (startPosition < 0) {
            throw new ODMFIllegalArgumentException();
        } else if (str == null) {
            throw new ODMFIllegalArgumentException();
        } else {
            try {
                String replaceString = str.substring(offset, offset + len);
                this.buffer.replace(startPosition, replaceString.length() + startPosition, replaceString);
                return len;
            } catch (StringIndexOutOfBoundsException e) {
                throw new ODMFIllegalArgumentException();
            }
        }
    }

    @Override // java.sql.Clob
    public OutputStream setAsciiStream(long indexToWriteAt) throws SQLException {
        int startPosition = ((int) indexToWriteAt) - 1;
        if (startPosition < 0) {
            throw new ODMFIllegalArgumentException();
        }
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        if (this.buffer != null) {
            bytesOut.write(this.buffer.toString().getBytes(StandardCharsets.UTF_8), 0, startPosition);
        }
        return bytesOut;
    }

    @Override // java.sql.Clob
    public Writer setCharacterStream(long indexToWriteAt) throws SQLException {
        int startPosition = ((int) indexToWriteAt) - 1;
        if (startPosition < 0) {
            throw new ODMFIllegalArgumentException();
        }
        CharArrayWriter writer = new CharArrayWriter();
        if (startPosition > 0 && this.buffer != null) {
            writer.write(this.buffer.toString(), 0, startPosition);
        }
        return writer;
    }

    @Override // java.sql.Clob
    public void truncate(long length) throws SQLException {
        if (this.buffer == null) {
            return;
        }
        if (length > ((long) this.buffer.length())) {
            throw new ODMFIllegalArgumentException();
        }
        this.buffer.delete(((int) length) - 1, this.buffer.length());
    }

    @Override // java.sql.Clob
    public void free() throws SQLException {
        this.buffer = null;
    }

    @Override // java.sql.Clob
    public Reader getCharacterStream(long pos, long length) throws SQLException {
        return new StringReader(getSubString(pos, (int) length));
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        Clob clob = (Clob) object;
        if (this.buffer != null) {
            return this.buffer.toString().equals(clob.buffer.toString());
        }
        return clob.buffer == null;
    }

    public int hashCode() {
        if (this.buffer != null) {
            return this.buffer.toString().hashCode();
        }
        return 0;
    }

    public String toString() {
        return this.buffer.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.buffer.toString());
    }
}
