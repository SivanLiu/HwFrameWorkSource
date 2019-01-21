package android.inputmethodservice;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.util.Xml;
import com.android.internal.R;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import org.xmlpull.v1.XmlPullParserException;

public class Keyboard {
    public static final int EDGE_BOTTOM = 8;
    public static final int EDGE_LEFT = 1;
    public static final int EDGE_RIGHT = 2;
    public static final int EDGE_TOP = 4;
    private static final int GRID_HEIGHT = 5;
    private static final int GRID_SIZE = 50;
    private static final int GRID_WIDTH = 10;
    public static final int KEYCODE_ALT = -6;
    public static final int KEYCODE_CANCEL = -3;
    public static final int KEYCODE_DELETE = -5;
    public static final int KEYCODE_DONE = -4;
    public static final int KEYCODE_MODE_CHANGE = -2;
    public static final int KEYCODE_SHIFT = -1;
    private static float SEARCH_DISTANCE = 1.8f;
    static final String TAG = "Keyboard";
    private static final String TAG_KEY = "Key";
    private static final String TAG_KEYBOARD = "Keyboard";
    private static final String TAG_ROW = "Row";
    private int mCellHeight;
    private int mCellWidth;
    private int mDefaultHeight;
    private int mDefaultHorizontalGap;
    private int mDefaultVerticalGap;
    private int mDefaultWidth;
    private int mDisplayHeight;
    private int mDisplayWidth;
    private int[][] mGridNeighbors;
    private int mKeyHeight;
    private int mKeyWidth;
    private int mKeyboardMode;
    private List<Key> mKeys;
    private CharSequence mLabel;
    private List<Key> mModifierKeys;
    private int mProximityThreshold;
    private int[] mShiftKeyIndices;
    private Key[] mShiftKeys;
    private boolean mShifted;
    private int mTotalHeight;
    private int mTotalWidth;
    private ArrayList<Row> rows;

    public static class Key {
        private static final int[] KEY_STATE_NORMAL = new int[0];
        private static final int[] KEY_STATE_NORMAL_OFF = new int[]{16842911};
        private static final int[] KEY_STATE_NORMAL_ON = new int[]{16842911, 16842912};
        private static final int[] KEY_STATE_PRESSED = new int[]{16842919};
        private static final int[] KEY_STATE_PRESSED_OFF = new int[]{16842919, 16842911};
        private static final int[] KEY_STATE_PRESSED_ON = new int[]{16842919, 16842911, 16842912};
        public int[] codes;
        public int edgeFlags;
        public int gap;
        public int height;
        public Drawable icon;
        public Drawable iconPreview;
        private Keyboard keyboard;
        public CharSequence label;
        public boolean modifier;
        public boolean on;
        public CharSequence popupCharacters;
        public int popupResId;
        public boolean pressed;
        public boolean repeatable;
        public boolean sticky;
        public CharSequence text;
        public int width;
        public int x;
        public int y;

        public Key(Row parent) {
            this.keyboard = parent.parent;
            this.height = parent.defaultHeight;
            this.width = parent.defaultWidth;
            this.gap = parent.defaultHorizontalGap;
            this.edgeFlags = parent.rowEdgeFlags;
        }

        public Key(Resources res, Row parent, int x, int y, XmlResourceParser parser) {
            this(parent);
            this.x = x;
            this.y = y;
            TypedArray a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.Keyboard);
            this.width = Keyboard.getDimensionOrFraction(a, 0, this.keyboard.mDisplayWidth, parent.defaultWidth);
            this.height = Keyboard.getDimensionOrFraction(a, 1, this.keyboard.mDisplayHeight, parent.defaultHeight);
            this.gap = Keyboard.getDimensionOrFraction(a, 2, this.keyboard.mDisplayWidth, parent.defaultHorizontalGap);
            a.recycle();
            a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.Keyboard_Key);
            this.x += this.gap;
            TypedValue codesValue = new TypedValue();
            a.getValue(0, codesValue);
            if (codesValue.type == 16 || codesValue.type == 17) {
                this.codes = new int[]{codesValue.data};
            } else if (codesValue.type == 3) {
                this.codes = parseCSV(codesValue.string.toString());
            }
            this.iconPreview = a.getDrawable(7);
            if (this.iconPreview != null) {
                this.iconPreview.setBounds(0, 0, this.iconPreview.getIntrinsicWidth(), this.iconPreview.getIntrinsicHeight());
            }
            this.popupCharacters = a.getText(2);
            this.popupResId = a.getResourceId(1, 0);
            this.repeatable = a.getBoolean(6, false);
            this.modifier = a.getBoolean(4, false);
            this.sticky = a.getBoolean(5, false);
            this.edgeFlags = a.getInt(3, 0);
            this.edgeFlags |= parent.rowEdgeFlags;
            this.icon = a.getDrawable(10);
            if (this.icon != null) {
                this.icon.setBounds(0, 0, this.icon.getIntrinsicWidth(), this.icon.getIntrinsicHeight());
            }
            this.label = a.getText(9);
            this.text = a.getText(8);
            if (this.codes == null && !TextUtils.isEmpty(this.label)) {
                this.codes = new int[]{this.label.charAt(0)};
            }
            a.recycle();
        }

        public void onPressed() {
            this.pressed ^= 1;
        }

        public void onReleased(boolean inside) {
            this.pressed ^= 1;
            if (this.sticky && inside) {
                this.on ^= 1;
            }
        }

        int[] parseCSV(String value) {
            int count = 0;
            int lastIndex = 0;
            if (value.length() > 0) {
                while (true) {
                    count++;
                    int indexOf = value.indexOf(",", lastIndex + 1);
                    lastIndex = indexOf;
                    if (indexOf <= 0) {
                        break;
                    }
                }
            }
            int[] values = new int[count];
            count = 0;
            StringTokenizer st = new StringTokenizer(value, ",");
            while (st.hasMoreTokens()) {
                int count2 = count + 1;
                try {
                    values[count] = Integer.parseInt(st.nextToken());
                } catch (NumberFormatException e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Error parsing keycodes ");
                    stringBuilder.append(value);
                    Log.e("Keyboard", stringBuilder.toString());
                }
                count = count2;
            }
            return values;
        }

        public boolean isInside(int x, int y) {
            return (x >= this.x || (((this.edgeFlags & 1) > 0) && x <= this.x + this.width)) && ((x < this.x + this.width || (((this.edgeFlags & 2) > 0) && x >= this.x)) && ((y >= this.y || (((this.edgeFlags & 4) > 0) && y <= this.y + this.height)) && (y < this.y + this.height || (((this.edgeFlags & 8) > 0) && y >= this.y))));
        }

        public int squaredDistanceFrom(int x, int y) {
            int xDist = (this.x + (this.width / 2)) - x;
            int yDist = (this.y + (this.height / 2)) - y;
            return (xDist * xDist) + (yDist * yDist);
        }

        public int[] getCurrentDrawableState() {
            int[] states = KEY_STATE_NORMAL;
            if (this.on) {
                if (this.pressed) {
                    return KEY_STATE_PRESSED_ON;
                }
                return KEY_STATE_NORMAL_ON;
            } else if (this.sticky) {
                if (this.pressed) {
                    return KEY_STATE_PRESSED_OFF;
                }
                return KEY_STATE_NORMAL_OFF;
            } else if (this.pressed) {
                return KEY_STATE_PRESSED;
            } else {
                return states;
            }
        }
    }

    public static class Row {
        public int defaultHeight;
        public int defaultHorizontalGap;
        public int defaultWidth;
        ArrayList<Key> mKeys = new ArrayList();
        public int mode;
        private Keyboard parent;
        public int rowEdgeFlags;
        public int verticalGap;

        public Row(Keyboard parent) {
            this.parent = parent;
        }

        public Row(Resources res, Keyboard parent, XmlResourceParser parser) {
            this.parent = parent;
            TypedArray a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.Keyboard);
            this.defaultWidth = Keyboard.getDimensionOrFraction(a, 0, parent.mDisplayWidth, parent.mDefaultWidth);
            this.defaultHeight = Keyboard.getDimensionOrFraction(a, 1, parent.mDisplayHeight, parent.mDefaultHeight);
            this.defaultHorizontalGap = Keyboard.getDimensionOrFraction(a, 2, parent.mDisplayWidth, parent.mDefaultHorizontalGap);
            this.verticalGap = Keyboard.getDimensionOrFraction(a, 3, parent.mDisplayHeight, parent.mDefaultVerticalGap);
            a.recycle();
            a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.Keyboard_Row);
            this.rowEdgeFlags = a.getInt(0, 0);
            this.mode = a.getResourceId(1, 0);
        }
    }

    public Keyboard(Context context, int xmlLayoutResId) {
        this(context, xmlLayoutResId, 0);
    }

    public Keyboard(Context context, int xmlLayoutResId, int modeId, int width, int height) {
        this.mShiftKeys = new Key[]{null, null};
        this.mShiftKeyIndices = new int[]{-1, -1};
        this.rows = new ArrayList();
        this.mDisplayWidth = width;
        this.mDisplayHeight = height;
        this.mDefaultHorizontalGap = 0;
        this.mDefaultWidth = this.mDisplayWidth / 10;
        this.mDefaultVerticalGap = 0;
        this.mDefaultHeight = this.mDefaultWidth;
        this.mKeys = new ArrayList();
        this.mModifierKeys = new ArrayList();
        this.mKeyboardMode = modeId;
        loadKeyboard(context, context.getResources().getXml(xmlLayoutResId));
    }

    public Keyboard(Context context, int xmlLayoutResId, int modeId) {
        this.mShiftKeys = new Key[]{null, null};
        this.mShiftKeyIndices = new int[]{-1, -1};
        this.rows = new ArrayList();
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        this.mDisplayWidth = dm.widthPixels;
        this.mDisplayHeight = dm.heightPixels;
        this.mDefaultHorizontalGap = 0;
        this.mDefaultWidth = this.mDisplayWidth / 10;
        this.mDefaultVerticalGap = 0;
        this.mDefaultHeight = this.mDefaultWidth;
        this.mKeys = new ArrayList();
        this.mModifierKeys = new ArrayList();
        this.mKeyboardMode = modeId;
        loadKeyboard(context, context.getResources().getXml(xmlLayoutResId));
    }

    public Keyboard(Context context, int layoutTemplateResId, CharSequence characters, int columns, int horizontalPadding) {
        this(context, layoutTemplateResId);
        int y = 0;
        int column = 0;
        this.mTotalWidth = 0;
        Row row = new Row(this);
        row.defaultHeight = this.mDefaultHeight;
        row.defaultWidth = this.mDefaultWidth;
        row.defaultHorizontalGap = this.mDefaultHorizontalGap;
        row.verticalGap = this.mDefaultVerticalGap;
        row.rowEdgeFlags = 12;
        int i = columns;
        int maxColumns = i == -1 ? Integer.MAX_VALUE : i;
        int x = 0;
        for (int i2 = 0; i2 < characters.length(); i2++) {
            char c = characters.charAt(i2);
            if (column >= maxColumns || (this.mDefaultWidth + x) + horizontalPadding > this.mDisplayWidth) {
                x = 0;
                y += this.mDefaultVerticalGap + this.mDefaultHeight;
                column = 0;
            }
            Key key = new Key(row);
            key.x = x;
            key.y = y;
            key.label = String.valueOf(c);
            key.codes = new int[]{c};
            column++;
            x += key.width + key.gap;
            this.mKeys.add(key);
            row.mKeys.add(key);
            if (x > this.mTotalWidth) {
                this.mTotalWidth = x;
            }
        }
        CharSequence charSequence = characters;
        this.mTotalHeight = this.mDefaultHeight + y;
        this.rows.add(row);
    }

    final void resize(int newWidth, int newHeight) {
        int numRows = this.rows.size();
        for (int rowIndex = 0; rowIndex < numRows; rowIndex++) {
            int keyIndex;
            Row row = (Row) this.rows.get(rowIndex);
            int numKeys = row.mKeys.size();
            int totalWidth = 0;
            int totalGap = 0;
            for (keyIndex = 0; keyIndex < numKeys; keyIndex++) {
                Key key = (Key) row.mKeys.get(keyIndex);
                if (keyIndex > 0) {
                    totalGap += key.gap;
                }
                totalWidth += key.width;
            }
            if (totalGap + totalWidth > newWidth) {
                float scaleFactor = ((float) (newWidth - totalGap)) / ((float) totalWidth);
                int x = 0;
                for (keyIndex = 0; keyIndex < numKeys; keyIndex++) {
                    Key key2 = (Key) row.mKeys.get(keyIndex);
                    key2.width = (int) (((float) key2.width) * scaleFactor);
                    key2.x = x;
                    x += key2.width + key2.gap;
                }
            }
        }
        this.mTotalWidth = newWidth;
    }

    public List<Key> getKeys() {
        return this.mKeys;
    }

    public List<Key> getModifierKeys() {
        return this.mModifierKeys;
    }

    protected int getHorizontalGap() {
        return this.mDefaultHorizontalGap;
    }

    protected void setHorizontalGap(int gap) {
        this.mDefaultHorizontalGap = gap;
    }

    protected int getVerticalGap() {
        return this.mDefaultVerticalGap;
    }

    protected void setVerticalGap(int gap) {
        this.mDefaultVerticalGap = gap;
    }

    protected int getKeyHeight() {
        return this.mDefaultHeight;
    }

    protected void setKeyHeight(int height) {
        this.mDefaultHeight = height;
    }

    protected int getKeyWidth() {
        return this.mDefaultWidth;
    }

    protected void setKeyWidth(int width) {
        this.mDefaultWidth = width;
    }

    public int getHeight() {
        return this.mTotalHeight;
    }

    public int getMinWidth() {
        return this.mTotalWidth;
    }

    public boolean setShifted(boolean shiftState) {
        for (Key shiftKey : this.mShiftKeys) {
            if (shiftKey != null) {
                shiftKey.on = shiftState;
            }
        }
        if (this.mShifted == shiftState) {
            return false;
        }
        this.mShifted = shiftState;
        return true;
    }

    public boolean isShifted() {
        return this.mShifted;
    }

    public int[] getShiftKeyIndices() {
        return this.mShiftKeyIndices;
    }

    public int getShiftKeyIndex() {
        return this.mShiftKeyIndices[0];
    }

    private void computeNearestNeighbors() {
        this.mCellWidth = ((getMinWidth() + 10) - 1) / 10;
        this.mCellHeight = ((getHeight() + 5) - 1) / 5;
        this.mGridNeighbors = new int[50][];
        int[] indices = new int[this.mKeys.size()];
        int gridWidth = this.mCellWidth * 10;
        int gridHeight = 5 * this.mCellHeight;
        int x = 0;
        while (x < gridWidth) {
            int y = 0;
            while (y < gridHeight) {
                int count = 0;
                for (int i = 0; i < this.mKeys.size(); i++) {
                    Key key = (Key) this.mKeys.get(i);
                    if (key.squaredDistanceFrom(x, y) < this.mProximityThreshold || key.squaredDistanceFrom((this.mCellWidth + x) - 1, y) < this.mProximityThreshold || key.squaredDistanceFrom((this.mCellWidth + x) - 1, (this.mCellHeight + y) - 1) < this.mProximityThreshold || key.squaredDistanceFrom(x, (this.mCellHeight + y) - 1) < this.mProximityThreshold) {
                        int count2 = count + 1;
                        indices[count] = i;
                        count = count2;
                    }
                }
                int[] cell = new int[count];
                System.arraycopy(indices, 0, cell, 0, count);
                this.mGridNeighbors[((y / this.mCellHeight) * 10) + (x / this.mCellWidth)] = cell;
                y += this.mCellHeight;
            }
            x += this.mCellWidth;
        }
    }

    public int[] getNearestKeys(int x, int y) {
        if (this.mGridNeighbors == null) {
            computeNearestNeighbors();
        }
        if (x >= 0 && x < getMinWidth() && y >= 0 && y < getHeight()) {
            int index = ((y / this.mCellHeight) * 10) + (x / this.mCellWidth);
            if (index < 50) {
                return this.mGridNeighbors[index];
            }
        }
        return new int[0];
    }

    protected Row createRowFromXml(Resources res, XmlResourceParser parser) {
        return new Row(res, this, parser);
    }

    protected Key createKeyFromXml(Resources res, Row parent, int x, int y, XmlResourceParser parser) {
        return new Key(res, parent, x, y, parser);
    }

    private void loadKeyboard(Context context, XmlResourceParser parser) {
        Exception e;
        StringBuilder stringBuilder;
        XmlResourceParser xmlResourceParser = parser;
        Resources res = context.getResources();
        boolean inRow = false;
        int row = 0;
        int x = 0;
        int y = 0;
        Key key = null;
        Row currentRow = null;
        boolean inKey = false;
        boolean skipRow = false;
        while (true) {
            boolean skipRow2 = skipRow;
            try {
                int next = parser.next();
                int event = next;
                boolean z = true;
                if (next == 1) {
                    break;
                } else if (event == 2) {
                    String tag = parser.getName();
                    int i;
                    if (TAG_ROW.equals(tag)) {
                        boolean inRow2 = true;
                        x = 0;
                        try {
                            currentRow = createRowFromXml(res, xmlResourceParser);
                            this.rows.add(currentRow);
                            if (currentRow.mode == 0 || currentRow.mode == this.mKeyboardMode) {
                                z = false;
                            }
                            boolean z2 = z;
                            if (z2) {
                                try {
                                    skipToEndOfRow(xmlResourceParser);
                                    inRow2 = false;
                                } catch (Exception e2) {
                                    e = e2;
                                    inRow = true;
                                    skipRow2 = z2;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("Parse error:");
                                    stringBuilder.append(e);
                                    Log.e("Keyboard", stringBuilder.toString());
                                    e.printStackTrace();
                                    this.mTotalHeight = y - this.mDefaultVerticalGap;
                                }
                            }
                            inRow = inRow2;
                            skipRow = z2;
                        } catch (Exception e3) {
                            e = e3;
                            inRow = true;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Parse error:");
                            stringBuilder.append(e);
                            Log.e("Keyboard", stringBuilder.toString());
                            e.printStackTrace();
                            this.mTotalHeight = y - this.mDefaultVerticalGap;
                        }
                    } else if (TAG_KEY.equals(tag)) {
                        int i2 = 1;
                        i = event;
                        try {
                            Key key2 = createKeyFromXml(res, currentRow, x, y, xmlResourceParser);
                            try {
                                this.mKeys.add(key2);
                                if (key2.codes[0] == -1) {
                                    for (int i3 = 0; i3 < this.mShiftKeys.length; i3++) {
                                        if (this.mShiftKeys[i3] == null) {
                                            this.mShiftKeys[i3] = key2;
                                            this.mShiftKeyIndices[i3] = this.mKeys.size() - 1;
                                            break;
                                        }
                                    }
                                    this.mModifierKeys.add(key2);
                                } else if (key2.codes[0] == -6) {
                                    this.mModifierKeys.add(key2);
                                }
                                currentRow.mKeys.add(key2);
                                key = key2;
                                skipRow = skipRow2;
                                inKey = true;
                            } catch (Exception e4) {
                                e = e4;
                                key = key2;
                                inKey = true;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Parse error:");
                                stringBuilder.append(e);
                                Log.e("Keyboard", stringBuilder.toString());
                                e.printStackTrace();
                                this.mTotalHeight = y - this.mDefaultVerticalGap;
                            }
                        } catch (Exception e5) {
                            e = e5;
                            inKey = true;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("Parse error:");
                            stringBuilder.append(e);
                            Log.e("Keyboard", stringBuilder.toString());
                            e.printStackTrace();
                            this.mTotalHeight = y - this.mDefaultVerticalGap;
                        }
                    } else {
                        i = event;
                        if ("Keyboard".equals(tag)) {
                            parseKeyboardAttributes(res, xmlResourceParser);
                        }
                        skipRow = skipRow2;
                    }
                } else {
                    if (event == 3) {
                        if (inKey) {
                            inKey = false;
                            int x2 = x + (key.gap + key.width);
                            try {
                                if (x2 > this.mTotalWidth) {
                                    this.mTotalWidth = x2;
                                }
                                x = x2;
                            } catch (Exception e6) {
                                e = e6;
                                x = x2;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Parse error:");
                                stringBuilder.append(e);
                                Log.e("Keyboard", stringBuilder.toString());
                                e.printStackTrace();
                                this.mTotalHeight = y - this.mDefaultVerticalGap;
                            }
                        } else if (inRow) {
                            inRow = false;
                            y += currentRow.verticalGap;
                            y += currentRow.defaultHeight;
                            row++;
                        }
                    }
                    skipRow = skipRow2;
                }
            } catch (Exception e7) {
                e = e7;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Parse error:");
                stringBuilder.append(e);
                Log.e("Keyboard", stringBuilder.toString());
                e.printStackTrace();
                this.mTotalHeight = y - this.mDefaultVerticalGap;
            }
        }
        this.mTotalHeight = y - this.mDefaultVerticalGap;
    }

    private void skipToEndOfRow(XmlResourceParser parser) throws XmlPullParserException, IOException {
        while (true) {
            int next = parser.next();
            int event = next;
            if (next == 1) {
                return;
            }
            if (event == 3 && parser.getName().equals(TAG_ROW)) {
                return;
            }
        }
    }

    private void parseKeyboardAttributes(Resources res, XmlResourceParser parser) {
        TypedArray a = res.obtainAttributes(Xml.asAttributeSet(parser), R.styleable.Keyboard);
        this.mDefaultWidth = getDimensionOrFraction(a, 0, this.mDisplayWidth, this.mDisplayWidth / 10);
        this.mDefaultHeight = getDimensionOrFraction(a, 1, this.mDisplayHeight, 50);
        this.mDefaultHorizontalGap = getDimensionOrFraction(a, 2, this.mDisplayWidth, 0);
        this.mDefaultVerticalGap = getDimensionOrFraction(a, 3, this.mDisplayHeight, 0);
        this.mProximityThreshold = (int) (((float) this.mDefaultWidth) * SEARCH_DISTANCE);
        this.mProximityThreshold *= this.mProximityThreshold;
        a.recycle();
    }

    static int getDimensionOrFraction(TypedArray a, int index, int base, int defValue) {
        TypedValue value = a.peekValue(index);
        if (value == null) {
            return defValue;
        }
        if (value.type == 5) {
            return a.getDimensionPixelOffset(index, defValue);
        }
        if (value.type == 6) {
            return Math.round(a.getFraction(index, base, base, (float) defValue));
        }
        return defValue;
    }
}
