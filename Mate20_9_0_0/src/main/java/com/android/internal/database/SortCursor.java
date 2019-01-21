package com.android.internal.database;

import android.database.AbstractCursor;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.util.Log;
import java.lang.reflect.Array;

public class SortCursor extends AbstractCursor {
    private static final String TAG = "SortCursor";
    private final int ROWCACHESIZE = 64;
    private int[][] mCurRowNumCache;
    private Cursor mCursor;
    private int[] mCursorCache = new int[64];
    private Cursor[] mCursors;
    private int mLastCacheHit = -1;
    private DataSetObserver mObserver = new DataSetObserver() {
        public void onChanged() {
            SortCursor.this.mPos = -1;
        }

        public void onInvalidated() {
            SortCursor.this.mPos = -1;
        }
    };
    private int[] mRowNumCache = new int[64];
    private int[] mSortColumns;

    public SortCursor(Cursor[] cursors, String sortcolumn) {
        this.mCursors = cursors;
        int length = this.mCursors.length;
        this.mSortColumns = new int[length];
        int j = 0;
        for (int i = 0; i < length; i++) {
            if (this.mCursors[i] != null) {
                this.mCursors[i].registerDataSetObserver(this.mObserver);
                this.mCursors[i].moveToFirst();
                this.mSortColumns[i] = this.mCursors[i].getColumnIndexOrThrow(sortcolumn);
            }
        }
        this.mCursor = null;
        String smallest = "";
        while (j < length) {
            if (!(this.mCursors[j] == null || this.mCursors[j].isAfterLast())) {
                String current = this.mCursors[j].getString(this.mSortColumns[j]);
                if (this.mCursor == null || (current != null && current.compareToIgnoreCase(smallest) < 0)) {
                    smallest = current != null ? current : smallest;
                    this.mCursor = this.mCursors[j];
                }
            }
            j++;
        }
        for (j = this.mRowNumCache.length - 1; j >= 0; j--) {
            this.mRowNumCache[j] = -2;
        }
        this.mCurRowNumCache = (int[][]) Array.newInstance(int.class, new int[]{64, length});
    }

    public int getCount() {
        int count = 0;
        int length = this.mCursors.length;
        for (int i = 0; i < length; i++) {
            if (this.mCursors[i] != null) {
                count += this.mCursors[i].getCount();
            }
        }
        return count;
    }

    public boolean onMove(int oldPosition, int newPosition) {
        if (oldPosition == newPosition) {
            return true;
        }
        int cache_entry = newPosition % 64;
        int i = 0;
        int which;
        if (this.mRowNumCache[cache_entry] == newPosition) {
            which = this.mCursorCache[cache_entry];
            this.mCursor = this.mCursors[which];
            if (this.mCursor == null) {
                Log.w(TAG, "onMove: cache results in a null cursor.");
                return false;
            }
            this.mCursor.moveToPosition(this.mCurRowNumCache[cache_entry][which]);
            this.mLastCacheHit = cache_entry;
            return true;
        }
        int i2;
        this.mCursor = null;
        which = this.mCursors.length;
        if (this.mLastCacheHit >= 0) {
            for (int i3 = 0; i3 < which; i3++) {
                if (this.mCursors[i3] != null) {
                    this.mCursors[i3].moveToPosition(this.mCurRowNumCache[this.mLastCacheHit][i3]);
                }
            }
        }
        if (newPosition < oldPosition || oldPosition == -1) {
            for (i2 = 0; i2 < which; i2++) {
                if (this.mCursors[i2] != null) {
                    this.mCursors[i2].moveToFirst();
                }
            }
            oldPosition = 0;
        }
        if (oldPosition < 0) {
            oldPosition = 0;
        }
        int smallestIdx = -1;
        i2 = oldPosition;
        while (i2 <= newPosition) {
            String smallest = "";
            int smallestIdx2 = -1;
            smallestIdx = 0;
            while (smallestIdx < which) {
                if (!(this.mCursors[smallestIdx] == null || this.mCursors[smallestIdx].isAfterLast())) {
                    String current = this.mCursors[smallestIdx].getString(this.mSortColumns[smallestIdx]);
                    if (smallestIdx2 < 0 || current.compareToIgnoreCase(smallest) < 0) {
                        smallest = current;
                        smallestIdx2 = smallestIdx;
                    }
                }
                smallestIdx++;
            }
            if (i2 == newPosition) {
                smallestIdx = smallestIdx2;
                break;
            }
            if (this.mCursors[smallestIdx2] != null) {
                this.mCursors[smallestIdx2].moveToNext();
            }
            i2++;
            smallestIdx = smallestIdx2;
        }
        this.mCursor = this.mCursors[smallestIdx];
        this.mRowNumCache[cache_entry] = newPosition;
        this.mCursorCache[cache_entry] = smallestIdx;
        while (i < which) {
            if (this.mCursors[i] != null) {
                this.mCurRowNumCache[cache_entry][i] = this.mCursors[i].getPosition();
            }
            i++;
        }
        this.mLastCacheHit = -1;
        return true;
    }

    public String getString(int column) {
        return this.mCursor.getString(column);
    }

    public short getShort(int column) {
        return this.mCursor.getShort(column);
    }

    public int getInt(int column) {
        return this.mCursor.getInt(column);
    }

    public long getLong(int column) {
        return this.mCursor.getLong(column);
    }

    public float getFloat(int column) {
        return this.mCursor.getFloat(column);
    }

    public double getDouble(int column) {
        return this.mCursor.getDouble(column);
    }

    public int getType(int column) {
        return this.mCursor.getType(column);
    }

    public boolean isNull(int column) {
        return this.mCursor.isNull(column);
    }

    public byte[] getBlob(int column) {
        return this.mCursor.getBlob(column);
    }

    public String[] getColumnNames() {
        if (this.mCursor != null) {
            return this.mCursor.getColumnNames();
        }
        int length = this.mCursors.length;
        for (int i = 0; i < length; i++) {
            if (this.mCursors[i] != null) {
                return this.mCursors[i].getColumnNames();
            }
        }
        throw new IllegalStateException("No cursor that can return names");
    }

    public void deactivate() {
        int length = this.mCursors.length;
        for (int i = 0; i < length; i++) {
            if (this.mCursors[i] != null) {
                this.mCursors[i].deactivate();
            }
        }
    }

    public void close() {
        int length = this.mCursors.length;
        for (int i = 0; i < length; i++) {
            if (this.mCursors[i] != null) {
                this.mCursors[i].close();
            }
        }
    }

    public void registerDataSetObserver(DataSetObserver observer) {
        int length = this.mCursors.length;
        for (int i = 0; i < length; i++) {
            if (this.mCursors[i] != null) {
                this.mCursors[i].registerDataSetObserver(observer);
            }
        }
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
        int length = this.mCursors.length;
        for (int i = 0; i < length; i++) {
            if (this.mCursors[i] != null) {
                this.mCursors[i].unregisterDataSetObserver(observer);
            }
        }
    }

    public boolean requery() {
        int length = this.mCursors.length;
        int i = 0;
        while (i < length) {
            if (this.mCursors[i] != null && !this.mCursors[i].requery()) {
                return false;
            }
            i++;
        }
        return true;
    }
}
