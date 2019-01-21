package android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.net.wifi.WifiEnterpriseConfig;
import android.telecom.Logging.Session;
import android.util.AttributeSet;
import android.util.LogPrinter;
import android.util.Pair;
import android.util.Printer;
import android.view.Gravity;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.RemoteViews.RemoteView;
import com.android.internal.R;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RemoteView
public class GridLayout extends ViewGroup {
    private static final int ALIGNMENT_MODE = 6;
    public static final int ALIGN_BOUNDS = 0;
    public static final int ALIGN_MARGINS = 1;
    public static final Alignment BASELINE = new Alignment() {
        int getGravityOffset(View view, int cellDelta) {
            return 0;
        }

        public int getAlignmentValue(View view, int viewSize, int mode) {
            if (view.getVisibility() == 8) {
                return 0;
            }
            int baseline = view.getBaseline();
            return baseline == -1 ? Integer.MIN_VALUE : baseline;
        }

        public Bounds getBounds() {
            return new Bounds() {
                private int size;

                protected void reset() {
                    super.reset();
                    this.size = Integer.MIN_VALUE;
                }

                protected void include(int before, int after) {
                    super.include(before, after);
                    this.size = Math.max(this.size, before + after);
                }

                protected int size(boolean min) {
                    return Math.max(super.size(min), this.size);
                }

                protected int getOffset(GridLayout gl, View c, Alignment a, int size, boolean hrz) {
                    return Math.max(0, super.getOffset(gl, c, a, size, hrz));
                }
            };
        }
    };
    public static final Alignment BOTTOM = TRAILING;
    private static final int CAN_STRETCH = 2;
    public static final Alignment CENTER = new Alignment() {
        int getGravityOffset(View view, int cellDelta) {
            return cellDelta >> 1;
        }

        public int getAlignmentValue(View view, int viewSize, int mode) {
            return viewSize >> 1;
        }
    };
    private static final int COLUMN_COUNT = 3;
    private static final int COLUMN_ORDER_PRESERVED = 4;
    private static final int DEFAULT_ALIGNMENT_MODE = 1;
    static final int DEFAULT_CONTAINER_MARGIN = 0;
    private static final int DEFAULT_COUNT = Integer.MIN_VALUE;
    private static final boolean DEFAULT_ORDER_PRESERVED = true;
    private static final int DEFAULT_ORIENTATION = 0;
    private static final boolean DEFAULT_USE_DEFAULT_MARGINS = false;
    public static final Alignment END = TRAILING;
    public static final Alignment FILL = new Alignment() {
        int getGravityOffset(View view, int cellDelta) {
            return 0;
        }

        public int getAlignmentValue(View view, int viewSize, int mode) {
            return Integer.MIN_VALUE;
        }

        public int getSizeInCell(View view, int viewSize, int cellSize) {
            return cellSize;
        }
    };
    public static final int HORIZONTAL = 0;
    private static final int INFLEXIBLE = 0;
    private static final Alignment LEADING = new Alignment() {
        int getGravityOffset(View view, int cellDelta) {
            return 0;
        }

        public int getAlignmentValue(View view, int viewSize, int mode) {
            return 0;
        }
    };
    public static final Alignment LEFT = createSwitchingAlignment(START, END);
    static final Printer LOG_PRINTER = new LogPrinter(3, GridLayout.class.getName());
    static final int MAX_SIZE = 100000;
    static final Printer NO_PRINTER = new Printer() {
        public void println(String x) {
        }
    };
    private static final int ORIENTATION = 0;
    public static final Alignment RIGHT = createSwitchingAlignment(END, START);
    private static final int ROW_COUNT = 1;
    private static final int ROW_ORDER_PRESERVED = 2;
    public static final Alignment START = LEADING;
    public static final Alignment TOP = LEADING;
    private static final Alignment TRAILING = new Alignment() {
        int getGravityOffset(View view, int cellDelta) {
            return cellDelta;
        }

        public int getAlignmentValue(View view, int viewSize, int mode) {
            return viewSize;
        }
    };
    public static final int UNDEFINED = Integer.MIN_VALUE;
    static final Alignment UNDEFINED_ALIGNMENT = new Alignment() {
        int getGravityOffset(View view, int cellDelta) {
            return Integer.MIN_VALUE;
        }

        public int getAlignmentValue(View view, int viewSize, int mode) {
            return Integer.MIN_VALUE;
        }
    };
    static final int UNINITIALIZED_HASH = 0;
    private static final int USE_DEFAULT_MARGINS = 5;
    public static final int VERTICAL = 1;
    int mAlignmentMode;
    int mDefaultGap;
    final Axis mHorizontalAxis;
    int mLastLayoutParamsHashCode;
    int mOrientation;
    Printer mPrinter;
    boolean mUseDefaultMargins;
    final Axis mVerticalAxis;

    public static abstract class Alignment {
        abstract int getAlignmentValue(View view, int i, int i2);

        abstract int getGravityOffset(View view, int i);

        Alignment() {
        }

        int getSizeInCell(View view, int viewSize, int cellSize) {
            return viewSize;
        }

        Bounds getBounds() {
            return new Bounds();
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface AlignmentMode {
    }

    static final class Arc {
        public final Interval span;
        public boolean valid = true;
        public final MutableInt value;

        public Arc(Interval span, MutableInt value) {
            this.span = span;
            this.value = value;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(this.span);
            stringBuilder.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            stringBuilder.append(!this.valid ? "+>" : Session.SUBSESSION_SEPARATION_CHAR);
            stringBuilder.append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
            stringBuilder.append(this.value);
            return stringBuilder.toString();
        }
    }

    static final class Assoc<K, V> extends ArrayList<Pair<K, V>> {
        private final Class<K> keyType;
        private final Class<V> valueType;

        private Assoc(Class<K> keyType, Class<V> valueType) {
            this.keyType = keyType;
            this.valueType = valueType;
        }

        public static <K, V> Assoc<K, V> of(Class<K> keyType, Class<V> valueType) {
            return new Assoc(keyType, valueType);
        }

        public void put(K key, V value) {
            add(Pair.create(key, value));
        }

        public PackedMap<K, V> pack() {
            int N = size();
            Object[] keys = (Object[]) Array.newInstance(this.keyType, N);
            Object[] values = (Object[]) Array.newInstance(this.valueType, N);
            for (int i = 0; i < N; i++) {
                keys[i] = ((Pair) get(i)).first;
                values[i] = ((Pair) get(i)).second;
            }
            return new PackedMap(keys, values, null);
        }
    }

    final class Axis {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private static final int COMPLETE = 2;
        private static final int NEW = 0;
        private static final int PENDING = 1;
        public Arc[] arcs;
        public boolean arcsValid;
        PackedMap<Interval, MutableInt> backwardLinks;
        public boolean backwardLinksValid;
        public int definedCount;
        public int[] deltas;
        PackedMap<Interval, MutableInt> forwardLinks;
        public boolean forwardLinksValid;
        PackedMap<Spec, Bounds> groupBounds;
        public boolean groupBoundsValid;
        public boolean hasWeights;
        public boolean hasWeightsValid;
        public final boolean horizontal;
        public int[] leadingMargins;
        public boolean leadingMarginsValid;
        public int[] locations;
        public boolean locationsValid;
        private int maxIndex;
        boolean orderPreserved;
        private MutableInt parentMax;
        private MutableInt parentMin;
        public int[] trailingMargins;
        public boolean trailingMarginsValid;

        static {
            Class cls = GridLayout.class;
        }

        /* synthetic */ Axis(GridLayout x0, boolean x1, AnonymousClass1 x2) {
            this(x1);
        }

        private Axis(boolean horizontal) {
            this.definedCount = Integer.MIN_VALUE;
            this.maxIndex = Integer.MIN_VALUE;
            this.groupBoundsValid = false;
            this.forwardLinksValid = false;
            this.backwardLinksValid = false;
            this.leadingMarginsValid = false;
            this.trailingMarginsValid = false;
            this.arcsValid = false;
            this.locationsValid = false;
            this.hasWeightsValid = false;
            this.orderPreserved = true;
            this.parentMin = new MutableInt(0);
            this.parentMax = new MutableInt(-100000);
            this.horizontal = horizontal;
        }

        private int calculateMaxIndex() {
            int result = -1;
            int N = GridLayout.this.getChildCount();
            for (int i = 0; i < N; i++) {
                LayoutParams params = GridLayout.this.getLayoutParams(GridLayout.this.getChildAt(i));
                Interval span = (this.horizontal ? params.columnSpec : params.rowSpec).span;
                result = Math.max(Math.max(Math.max(result, span.min), span.max), span.size());
            }
            return result == -1 ? Integer.MIN_VALUE : result;
        }

        private int getMaxIndex() {
            if (this.maxIndex == Integer.MIN_VALUE) {
                this.maxIndex = Math.max(0, calculateMaxIndex());
            }
            return this.maxIndex;
        }

        public int getCount() {
            return Math.max(this.definedCount, getMaxIndex());
        }

        public void setCount(int count) {
            if (count != Integer.MIN_VALUE && count < getMaxIndex()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(this.horizontal ? "column" : "row");
                stringBuilder.append("Count must be greater than or equal to the maximum of all grid indices (and spans) defined in the LayoutParams of each child");
                GridLayout.handleInvalidParams(stringBuilder.toString());
            }
            this.definedCount = count;
        }

        public boolean isOrderPreserved() {
            return this.orderPreserved;
        }

        public void setOrderPreserved(boolean orderPreserved) {
            this.orderPreserved = orderPreserved;
            invalidateStructure();
        }

        private PackedMap<Spec, Bounds> createGroupBounds() {
            Assoc<Spec, Bounds> assoc = Assoc.of(Spec.class, Bounds.class);
            int N = GridLayout.this.getChildCount();
            for (int i = 0; i < N; i++) {
                LayoutParams lp = GridLayout.this.getLayoutParams(GridLayout.this.getChildAt(i));
                Spec spec = this.horizontal ? lp.columnSpec : lp.rowSpec;
                assoc.put(spec, spec.getAbsoluteAlignment(this.horizontal).getBounds());
            }
            return assoc.pack();
        }

        private void computeGroupBounds() {
            Bounds[] values = this.groupBounds.values;
            for (Bounds reset : values) {
                reset.reset();
            }
            int i = 0;
            int N = GridLayout.this.getChildCount();
            while (i < N) {
                View c = GridLayout.this.getChildAt(i);
                LayoutParams lp = GridLayout.this.getLayoutParams(c);
                Spec spec = this.horizontal ? lp.columnSpec : lp.rowSpec;
                ((Bounds) this.groupBounds.getValue(i)).include(GridLayout.this, c, spec, this, GridLayout.this.getMeasurementIncludingMargin(c, this.horizontal) + (spec.weight == 0.0f ? 0 : getDeltas()[i]));
                i++;
            }
        }

        public PackedMap<Spec, Bounds> getGroupBounds() {
            if (this.groupBounds == null) {
                this.groupBounds = createGroupBounds();
            }
            if (!this.groupBoundsValid) {
                computeGroupBounds();
                this.groupBoundsValid = true;
            }
            return this.groupBounds;
        }

        private PackedMap<Interval, MutableInt> createLinks(boolean min) {
            Assoc<Interval, MutableInt> result = Assoc.of(Interval.class, MutableInt.class);
            Spec[] keys = getGroupBounds().keys;
            int N = keys.length;
            for (int i = 0; i < N; i++) {
                result.put(min ? keys[i].span : keys[i].span.inverse(), new MutableInt());
            }
            return result.pack();
        }

        private void computeLinks(PackedMap<Interval, MutableInt> links, boolean min) {
            MutableInt[] spans = links.values;
            int i = 0;
            for (MutableInt reset : spans) {
                reset.reset();
            }
            Bounds[] bounds = getGroupBounds().values;
            while (i < bounds.length) {
                int size = bounds[i].size(min);
                MutableInt valueHolder = (MutableInt) links.getValue(i);
                valueHolder.value = Math.max(valueHolder.value, min ? size : -size);
                i++;
            }
        }

        private PackedMap<Interval, MutableInt> getForwardLinks() {
            if (this.forwardLinks == null) {
                this.forwardLinks = createLinks(true);
            }
            if (!this.forwardLinksValid) {
                computeLinks(this.forwardLinks, true);
                this.forwardLinksValid = true;
            }
            return this.forwardLinks;
        }

        private PackedMap<Interval, MutableInt> getBackwardLinks() {
            if (this.backwardLinks == null) {
                this.backwardLinks = createLinks(false);
            }
            if (!this.backwardLinksValid) {
                computeLinks(this.backwardLinks, false);
                this.backwardLinksValid = true;
            }
            return this.backwardLinks;
        }

        private void include(List<Arc> arcs, Interval key, MutableInt size, boolean ignoreIfAlreadyPresent) {
            if (key.size() != 0) {
                if (ignoreIfAlreadyPresent) {
                    for (Arc arc : arcs) {
                        if (arc.span.equals(key)) {
                            return;
                        }
                    }
                }
                arcs.add(new Arc(key, size));
            }
        }

        private void include(List<Arc> arcs, Interval key, MutableInt size) {
            include(arcs, key, size, true);
        }

        Arc[][] groupArcsByFirstVertex(Arc[] arcs) {
            int i;
            int N = getCount() + 1;
            Arc[][] result = new Arc[N][];
            int[] sizes = new int[N];
            int i2 = 0;
            for (Arc arc : arcs) {
                int i3 = arc.span.min;
                sizes[i3] = sizes[i3] + 1;
            }
            for (i = 0; i < sizes.length; i++) {
                result[i] = new Arc[sizes[i]];
            }
            Arrays.fill(sizes, 0);
            i = arcs.length;
            while (i2 < i) {
                Arc arc2 = arcs[i2];
                int i4 = arc2.span.min;
                Arc[] arcArr = result[i4];
                int i5 = sizes[i4];
                sizes[i4] = i5 + 1;
                arcArr[i5] = arc2;
                i2++;
            }
            return result;
        }

        private Arc[] topologicalSort(final Arc[] arcs) {
            return new Object() {
                static final /* synthetic */ boolean $assertionsDisabled = false;
                Arc[][] arcsByVertex = Axis.this.groupArcsByFirstVertex(arcs);
                int cursor = (this.result.length - 1);
                Arc[] result = new Arc[arcs.length];
                int[] visited = new int[(Axis.this.getCount() + 1)];

                static {
                    Class cls = GridLayout.class;
                }

                void walk(int loc) {
                    switch (this.visited[loc]) {
                        case 0:
                            this.visited[loc] = 1;
                            for (Arc arc : this.arcsByVertex[loc]) {
                                walk(arc.span.max);
                                Arc[] arcArr = this.result;
                                int i = this.cursor;
                                this.cursor = i - 1;
                                arcArr[i] = arc;
                            }
                            this.visited[loc] = 2;
                            return;
                        default:
                            return;
                    }
                }

                Arc[] sort() {
                    int N = this.arcsByVertex.length;
                    for (int loc = 0; loc < N; loc++) {
                        walk(loc);
                    }
                    return this.result;
                }
            }.sort();
        }

        private Arc[] topologicalSort(List<Arc> arcs) {
            return topologicalSort((Arc[]) arcs.toArray(new Arc[arcs.size()]));
        }

        private void addComponentSizes(List<Arc> result, PackedMap<Interval, MutableInt> links) {
            for (int i = 0; i < ((Interval[]) links.keys).length; i++) {
                include(result, ((Interval[]) links.keys)[i], ((MutableInt[]) links.values)[i], false);
            }
        }

        private Arc[] createArcs() {
            int i;
            List mins = new ArrayList();
            List maxs = new ArrayList();
            addComponentSizes(mins, getForwardLinks());
            addComponentSizes(maxs, getBackwardLinks());
            if (this.orderPreserved) {
                for (i = 0; i < getCount(); i++) {
                    include(mins, new Interval(i, i + 1), new MutableInt(0));
                }
            }
            i = getCount();
            include(mins, new Interval(0, i), this.parentMin, false);
            include(maxs, new Interval(i, 0), this.parentMax, false);
            return (Arc[]) GridLayout.append(topologicalSort(mins), topologicalSort(maxs));
        }

        private void computeArcs() {
            getForwardLinks();
            getBackwardLinks();
        }

        public Arc[] getArcs() {
            if (this.arcs == null) {
                this.arcs = createArcs();
            }
            if (!this.arcsValid) {
                computeArcs();
                this.arcsValid = true;
            }
            return this.arcs;
        }

        private boolean relax(int[] locations, Arc entry) {
            if (!entry.valid) {
                return false;
            }
            Interval span = entry.span;
            int u = span.min;
            int v = span.max;
            int candidate = locations[u] + entry.value.value;
            if (candidate <= locations[v]) {
                return false;
            }
            locations[v] = candidate;
            return true;
        }

        private void init(int[] locations) {
            Arrays.fill(locations, 0);
        }

        private String arcsToString(List<Arc> arcs) {
            String var = this.horizontal ? "x" : "y";
            StringBuilder result = new StringBuilder();
            boolean first = true;
            for (Arc arc : arcs) {
                String stringBuilder;
                if (first) {
                    first = false;
                } else {
                    result = result.append(", ");
                }
                int src = arc.span.min;
                int dst = arc.span.max;
                int value = arc.value.value;
                StringBuilder stringBuilder2;
                if (src < dst) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(var);
                    stringBuilder2.append(dst);
                    stringBuilder2.append("-");
                    stringBuilder2.append(var);
                    stringBuilder2.append(src);
                    stringBuilder2.append(">=");
                    stringBuilder2.append(value);
                    stringBuilder = stringBuilder2.toString();
                } else {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(var);
                    stringBuilder2.append(src);
                    stringBuilder2.append("-");
                    stringBuilder2.append(var);
                    stringBuilder2.append(dst);
                    stringBuilder2.append("<=");
                    stringBuilder2.append(-value);
                    stringBuilder = stringBuilder2.toString();
                }
                result.append(stringBuilder);
            }
            return result.toString();
        }

        private void logError(String axisName, Arc[] arcs, boolean[] culprits0) {
            List<Arc> culprits = new ArrayList();
            List<Arc> removed = new ArrayList();
            for (int c = 0; c < arcs.length; c++) {
                Arc arc = arcs[c];
                if (culprits0[c]) {
                    culprits.add(arc);
                }
                if (!arc.valid) {
                    removed.add(arc);
                }
            }
            Printer printer = GridLayout.this.mPrinter;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(axisName);
            stringBuilder.append(" constraints: ");
            stringBuilder.append(arcsToString(culprits));
            stringBuilder.append(" are inconsistent; permanently removing: ");
            stringBuilder.append(arcsToString(removed));
            stringBuilder.append(". ");
            printer.println(stringBuilder.toString());
        }

        private boolean solve(Arc[] arcs, int[] locations) {
            return solve(arcs, locations, true);
        }

        private boolean solve(Arc[] arcs, int[] locations, boolean modifyOnError) {
            String axisName = this.horizontal ? "horizontal" : "vertical";
            int N = getCount() + 1;
            boolean[] originalCulprits = null;
            for (int p = 0; p < arcs.length; p++) {
                int j;
                init(locations);
                int i = 0;
                while (i < N) {
                    boolean changed = false;
                    for (Arc relax : arcs) {
                        changed |= relax(locations, relax);
                    }
                    if (changed) {
                        i++;
                    } else {
                        if (originalCulprits != null) {
                            logError(axisName, arcs, originalCulprits);
                        }
                        return true;
                    }
                }
                if (!modifyOnError) {
                    return false;
                }
                int i2;
                boolean[] culprits = new boolean[arcs.length];
                for (i2 = 0; i2 < N; i2++) {
                    int length = arcs.length;
                    for (j = 0; j < length; j++) {
                        culprits[j] = culprits[j] | relax(locations, arcs[j]);
                    }
                }
                if (p == 0) {
                    originalCulprits = culprits;
                }
                for (i2 = 0; i2 < arcs.length; i2++) {
                    if (culprits[i2]) {
                        Arc arc = arcs[i2];
                        if (arc.span.min >= arc.span.max) {
                            arc.valid = false;
                            break;
                        }
                    }
                }
            }
            return true;
        }

        private void computeMargins(boolean leading) {
            int[] margins = leading ? this.leadingMargins : this.trailingMargins;
            int N = GridLayout.this.getChildCount();
            for (int i = 0; i < N; i++) {
                View c = GridLayout.this.getChildAt(i);
                if (c.getVisibility() != 8) {
                    LayoutParams lp = GridLayout.this.getLayoutParams(c);
                    Interval span = (this.horizontal ? lp.columnSpec : lp.rowSpec).span;
                    int index = leading ? span.min : span.max;
                    margins[index] = Math.max(margins[index], GridLayout.this.getMargin1(c, this.horizontal, leading));
                }
            }
        }

        public int[] getLeadingMargins() {
            if (this.leadingMargins == null) {
                this.leadingMargins = new int[(getCount() + 1)];
            }
            if (!this.leadingMarginsValid) {
                computeMargins(true);
                this.leadingMarginsValid = true;
            }
            return this.leadingMargins;
        }

        public int[] getTrailingMargins() {
            if (this.trailingMargins == null) {
                this.trailingMargins = new int[(getCount() + 1)];
            }
            if (!this.trailingMarginsValid) {
                computeMargins(false);
                this.trailingMarginsValid = true;
            }
            return this.trailingMargins;
        }

        private boolean solve(int[] a) {
            return solve(getArcs(), a);
        }

        private boolean computeHasWeights() {
            int N = GridLayout.this.getChildCount();
            for (int i = 0; i < N; i++) {
                View child = GridLayout.this.getChildAt(i);
                if (child.getVisibility() != 8) {
                    LayoutParams lp = GridLayout.this.getLayoutParams(child);
                    if ((this.horizontal ? lp.columnSpec : lp.rowSpec).weight != 0.0f) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean hasWeights() {
            if (!this.hasWeightsValid) {
                this.hasWeights = computeHasWeights();
                this.hasWeightsValid = true;
            }
            return this.hasWeights;
        }

        public int[] getDeltas() {
            if (this.deltas == null) {
                this.deltas = new int[GridLayout.this.getChildCount()];
            }
            return this.deltas;
        }

        private void shareOutDelta(int totalDelta, float totalWeight) {
            Arrays.fill(this.deltas, 0);
            int N = GridLayout.this.getChildCount();
            for (int i = 0; i < N; i++) {
                View c = GridLayout.this.getChildAt(i);
                if (c.getVisibility() != 8) {
                    LayoutParams lp = GridLayout.this.getLayoutParams(c);
                    float weight = (this.horizontal ? lp.columnSpec : lp.rowSpec).weight;
                    if (weight != 0.0f) {
                        int delta = Math.round((((float) totalDelta) * weight) / totalWeight);
                        this.deltas[i] = delta;
                        totalDelta -= delta;
                        totalWeight -= weight;
                    }
                }
            }
        }

        private void solveAndDistributeSpace(int[] a) {
            Arrays.fill(getDeltas(), 0);
            solve(a);
            boolean validSolution = true;
            int deltaMax = (this.parentMin.value * GridLayout.this.getChildCount()) + 1;
            if (deltaMax >= 2) {
                int deltaMin = 0;
                float totalWeight = calculateTotalWeight();
                int validDelta = -1;
                while (deltaMin < deltaMax) {
                    int delta = (int) ((((long) deltaMin) + ((long) deltaMax)) / 2);
                    invalidateValues();
                    shareOutDelta(delta, totalWeight);
                    validSolution = solve(getArcs(), a, false);
                    if (validSolution) {
                        validDelta = delta;
                        deltaMin = delta + 1;
                    } else {
                        deltaMax = delta;
                    }
                }
                if (validDelta > 0 && !validSolution) {
                    invalidateValues();
                    shareOutDelta(validDelta, totalWeight);
                    solve(a);
                }
            }
        }

        private float calculateTotalWeight() {
            float totalWeight = 0.0f;
            int N = GridLayout.this.getChildCount();
            for (int i = 0; i < N; i++) {
                View c = GridLayout.this.getChildAt(i);
                if (c.getVisibility() != 8) {
                    LayoutParams lp = GridLayout.this.getLayoutParams(c);
                    totalWeight += (this.horizontal ? lp.columnSpec : lp.rowSpec).weight;
                }
            }
            return totalWeight;
        }

        private void computeLocations(int[] a) {
            if (hasWeights()) {
                solveAndDistributeSpace(a);
            } else {
                solve(a);
            }
            if (!this.orderPreserved) {
                int a0 = a[0];
                int N = a.length;
                for (int i = 0; i < N; i++) {
                    a[i] = a[i] - a0;
                }
            }
        }

        public int[] getLocations() {
            if (this.locations == null) {
                this.locations = new int[(getCount() + 1)];
            }
            if (!this.locationsValid) {
                computeLocations(this.locations);
                this.locationsValid = true;
            }
            return this.locations;
        }

        private int size(int[] locations) {
            return locations[getCount()];
        }

        private void setParentConstraints(int min, int max) {
            this.parentMin.value = min;
            this.parentMax.value = -max;
            this.locationsValid = false;
        }

        private int getMeasure(int min, int max) {
            setParentConstraints(min, max);
            return size(getLocations());
        }

        public int getMeasure(int measureSpec) {
            int mode = MeasureSpec.getMode(measureSpec);
            int size = MeasureSpec.getSize(measureSpec);
            if (mode == Integer.MIN_VALUE) {
                return getMeasure(0, size);
            }
            if (mode == 0) {
                return getMeasure(0, 100000);
            }
            if (mode != 1073741824) {
                return 0;
            }
            return getMeasure(size, size);
        }

        public void layout(int size) {
            setParentConstraints(size, size);
            getLocations();
        }

        public void invalidateStructure() {
            this.maxIndex = Integer.MIN_VALUE;
            this.groupBounds = null;
            this.forwardLinks = null;
            this.backwardLinks = null;
            this.leadingMargins = null;
            this.trailingMargins = null;
            this.arcs = null;
            this.locations = null;
            this.deltas = null;
            this.hasWeightsValid = false;
            invalidateValues();
        }

        public void invalidateValues() {
            this.groupBoundsValid = false;
            this.forwardLinksValid = false;
            this.backwardLinksValid = false;
            this.leadingMarginsValid = false;
            this.trailingMarginsValid = false;
            this.arcsValid = false;
            this.locationsValid = false;
        }
    }

    static class Bounds {
        public int after;
        public int before;
        public int flexibility;

        /* synthetic */ Bounds(AnonymousClass1 x0) {
            this();
        }

        private Bounds() {
            reset();
        }

        protected void reset() {
            this.before = Integer.MIN_VALUE;
            this.after = Integer.MIN_VALUE;
            this.flexibility = 2;
        }

        protected void include(int before, int after) {
            this.before = Math.max(this.before, before);
            this.after = Math.max(this.after, after);
        }

        protected int size(boolean min) {
            if (min || !GridLayout.canStretch(this.flexibility)) {
                return this.before + this.after;
            }
            return 100000;
        }

        protected int getOffset(GridLayout gl, View c, Alignment a, int size, boolean horizontal) {
            return this.before - a.getAlignmentValue(c, size, gl.getLayoutMode());
        }

        protected final void include(GridLayout gl, View c, Spec spec, Axis axis, int size) {
            this.flexibility &= spec.getFlexibility();
            boolean horizontal = axis.horizontal;
            int before = spec.getAbsoluteAlignment(axis.horizontal).getAlignmentValue(c, size, gl.getLayoutMode());
            include(before, size - before);
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Bounds{before=");
            stringBuilder.append(this.before);
            stringBuilder.append(", after=");
            stringBuilder.append(this.after);
            stringBuilder.append('}');
            return stringBuilder.toString();
        }
    }

    static final class Interval {
        public final int max;
        public final int min;

        public Interval(int min, int max) {
            this.min = min;
            this.max = max;
        }

        int size() {
            return this.max - this.min;
        }

        Interval inverse() {
            return new Interval(this.max, this.min);
        }

        public boolean equals(Object that) {
            if (this == that) {
                return true;
            }
            if (that == null || getClass() != that.getClass()) {
                return false;
            }
            Interval interval = (Interval) that;
            if (this.max == interval.max && this.min == interval.min) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (31 * this.min) + this.max;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[");
            stringBuilder.append(this.min);
            stringBuilder.append(", ");
            stringBuilder.append(this.max);
            stringBuilder.append("]");
            return stringBuilder.toString();
        }
    }

    static final class MutableInt {
        public int value;

        public MutableInt() {
            reset();
        }

        public MutableInt(int value) {
            this.value = value;
        }

        public void reset() {
            this.value = Integer.MIN_VALUE;
        }

        public String toString() {
            return Integer.toString(this.value);
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Orientation {
    }

    static final class PackedMap<K, V> {
        public final int[] index;
        public final K[] keys;
        public final V[] values;

        /* synthetic */ PackedMap(Object[] x0, Object[] x1, AnonymousClass1 x2) {
            this(x0, x1);
        }

        private PackedMap(K[] keys, V[] values) {
            this.index = createIndex(keys);
            this.keys = compact(keys, this.index);
            this.values = compact(values, this.index);
        }

        public V getValue(int i) {
            return this.values[this.index[i]];
        }

        private static <K> int[] createIndex(K[] keys) {
            int size = keys.length;
            int[] result = new int[size];
            Map<K, Integer> keyToIndex = new HashMap();
            for (int i = 0; i < size; i++) {
                K key = keys[i];
                Integer index = (Integer) keyToIndex.get(key);
                if (index == null) {
                    index = Integer.valueOf(keyToIndex.size());
                    keyToIndex.put(key, index);
                }
                result[i] = index.intValue();
            }
            return result;
        }

        private static <K> K[] compact(K[] a, int[] index) {
            int size = a.length;
            Object[] result = (Object[]) Array.newInstance(a.getClass().getComponentType(), GridLayout.max2(index, -1) + 1);
            for (int i = 0; i < size; i++) {
                result[index[i]] = a[i];
            }
            return result;
        }
    }

    public static class Spec {
        static final float DEFAULT_WEIGHT = 0.0f;
        static final Spec UNDEFINED = GridLayout.spec(Integer.MIN_VALUE);
        final Alignment alignment;
        final Interval span;
        final boolean startDefined;
        final float weight;

        /* synthetic */ Spec(boolean x0, int x1, int x2, Alignment x3, float x4, AnonymousClass1 x5) {
            this(x0, x1, x2, x3, x4);
        }

        private Spec(boolean startDefined, Interval span, Alignment alignment, float weight) {
            this.startDefined = startDefined;
            this.span = span;
            this.alignment = alignment;
            this.weight = weight;
        }

        private Spec(boolean startDefined, int start, int size, Alignment alignment, float weight) {
            this(startDefined, new Interval(start, start + size), alignment, weight);
        }

        private Alignment getAbsoluteAlignment(boolean horizontal) {
            if (this.alignment != GridLayout.UNDEFINED_ALIGNMENT) {
                return this.alignment;
            }
            if (this.weight != 0.0f) {
                return GridLayout.FILL;
            }
            return horizontal ? GridLayout.START : GridLayout.BASELINE;
        }

        final Spec copyWriteSpan(Interval span) {
            return new Spec(this.startDefined, span, this.alignment, this.weight);
        }

        final Spec copyWriteAlignment(Alignment alignment) {
            return new Spec(this.startDefined, this.span, alignment, this.weight);
        }

        final int getFlexibility() {
            return (this.alignment == GridLayout.UNDEFINED_ALIGNMENT && this.weight == 0.0f) ? 0 : 2;
        }

        public boolean equals(Object that) {
            if (this == that) {
                return true;
            }
            if (that == null || getClass() != that.getClass()) {
                return false;
            }
            Spec spec = (Spec) that;
            if (this.alignment.equals(spec.alignment) && this.span.equals(spec.span)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (31 * this.span.hashCode()) + this.alignment.hashCode();
        }
    }

    public static class LayoutParams extends MarginLayoutParams {
        private static final int BOTTOM_MARGIN = 6;
        private static final int COLUMN = 1;
        private static final int COLUMN_SPAN = 4;
        private static final int COLUMN_WEIGHT = 6;
        private static final int DEFAULT_COLUMN = Integer.MIN_VALUE;
        private static final int DEFAULT_HEIGHT = -2;
        private static final int DEFAULT_MARGIN = Integer.MIN_VALUE;
        private static final int DEFAULT_ROW = Integer.MIN_VALUE;
        private static final Interval DEFAULT_SPAN = new Interval(Integer.MIN_VALUE, -2147483647);
        private static final int DEFAULT_SPAN_SIZE = DEFAULT_SPAN.size();
        private static final int DEFAULT_WIDTH = -2;
        private static final int GRAVITY = 0;
        private static final int LEFT_MARGIN = 3;
        private static final int MARGIN = 2;
        private static final int RIGHT_MARGIN = 5;
        private static final int ROW = 2;
        private static final int ROW_SPAN = 3;
        private static final int ROW_WEIGHT = 5;
        private static final int TOP_MARGIN = 4;
        public Spec columnSpec;
        public Spec rowSpec;

        private LayoutParams(int width, int height, int left, int top, int right, int bottom, Spec rowSpec, Spec columnSpec) {
            super(width, height);
            this.rowSpec = Spec.UNDEFINED;
            this.columnSpec = Spec.UNDEFINED;
            setMargins(left, top, right, bottom);
            this.rowSpec = rowSpec;
            this.columnSpec = columnSpec;
        }

        public LayoutParams(Spec rowSpec, Spec columnSpec) {
            this(-2, -2, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, rowSpec, columnSpec);
        }

        public LayoutParams() {
            this(Spec.UNDEFINED, Spec.UNDEFINED);
        }

        public LayoutParams(android.view.ViewGroup.LayoutParams params) {
            super(params);
            this.rowSpec = Spec.UNDEFINED;
            this.columnSpec = Spec.UNDEFINED;
        }

        public LayoutParams(MarginLayoutParams params) {
            super(params);
            this.rowSpec = Spec.UNDEFINED;
            this.columnSpec = Spec.UNDEFINED;
        }

        public LayoutParams(LayoutParams source) {
            super((MarginLayoutParams) source);
            this.rowSpec = Spec.UNDEFINED;
            this.columnSpec = Spec.UNDEFINED;
            this.rowSpec = source.rowSpec;
            this.columnSpec = source.columnSpec;
        }

        public LayoutParams(Context context, AttributeSet attrs) {
            super(context, attrs);
            this.rowSpec = Spec.UNDEFINED;
            this.columnSpec = Spec.UNDEFINED;
            reInitSuper(context, attrs);
            init(context, attrs);
        }

        private void reInitSuper(Context context, AttributeSet attrs) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ViewGroup_MarginLayout);
            try {
                int margin = a.getDimensionPixelSize(2, Integer.MIN_VALUE);
                this.leftMargin = a.getDimensionPixelSize(3, margin);
                this.topMargin = a.getDimensionPixelSize(4, margin);
                this.rightMargin = a.getDimensionPixelSize(5, margin);
                this.bottomMargin = a.getDimensionPixelSize(6, margin);
            } finally {
                a.recycle();
            }
        }

        private void init(Context context, AttributeSet attrs) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GridLayout_Layout);
            try {
                int gravity = a.getInt(0, 0);
                this.columnSpec = GridLayout.spec(a.getInt(1, Integer.MIN_VALUE), a.getInt(4, DEFAULT_SPAN_SIZE), GridLayout.getAlignment(gravity, true), a.getFloat(8.4E-45f, 0.0f));
                this.rowSpec = GridLayout.spec(a.getInt(2, Integer.MIN_VALUE), a.getInt(3, DEFAULT_SPAN_SIZE), GridLayout.getAlignment(gravity, false), a.getFloat(5, 0.0f));
            } finally {
                a.recycle();
            }
        }

        public void setGravity(int gravity) {
            this.rowSpec = this.rowSpec.copyWriteAlignment(GridLayout.getAlignment(gravity, false));
            this.columnSpec = this.columnSpec.copyWriteAlignment(GridLayout.getAlignment(gravity, true));
        }

        protected void setBaseAttributes(TypedArray attributes, int widthAttr, int heightAttr) {
            this.width = attributes.getLayoutDimension(widthAttr, -2);
            this.height = attributes.getLayoutDimension(heightAttr, -2);
        }

        final void setRowSpecSpan(Interval span) {
            this.rowSpec = this.rowSpec.copyWriteSpan(span);
        }

        final void setColumnSpecSpan(Interval span) {
            this.columnSpec = this.columnSpec.copyWriteSpan(span);
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            LayoutParams that = (LayoutParams) o;
            if (this.columnSpec.equals(that.columnSpec) && this.rowSpec.equals(that.rowSpec)) {
                return true;
            }
            return false;
        }

        public int hashCode() {
            return (31 * this.rowSpec.hashCode()) + this.columnSpec.hashCode();
        }
    }

    public GridLayout(Context context) {
        this(context, null);
    }

    public GridLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GridLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public GridLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.mHorizontalAxis = new Axis(this, true, null);
        this.mVerticalAxis = new Axis(this, false, null);
        this.mOrientation = 0;
        this.mUseDefaultMargins = false;
        this.mAlignmentMode = 1;
        this.mLastLayoutParamsHashCode = 0;
        this.mPrinter = LOG_PRINTER;
        this.mDefaultGap = context.getResources().getDimensionPixelOffset(17105015);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.GridLayout, defStyleAttr, defStyleRes);
        try {
            setRowCount(a.getInt(1, Integer.MIN_VALUE));
            setColumnCount(a.getInt(3, Integer.MIN_VALUE));
            setOrientation(a.getInt(0, 0));
            setUseDefaultMargins(a.getBoolean(5, false));
            setAlignmentMode(a.getInt(6, 1));
            setRowOrderPreserved(a.getBoolean(2, true));
            setColumnOrderPreserved(a.getBoolean(4, true));
        } finally {
            a.recycle();
        }
    }

    public int getOrientation() {
        return this.mOrientation;
    }

    public void setOrientation(int orientation) {
        if (this.mOrientation != orientation) {
            this.mOrientation = orientation;
            invalidateStructure();
            requestLayout();
        }
    }

    public int getRowCount() {
        return this.mVerticalAxis.getCount();
    }

    public void setRowCount(int rowCount) {
        this.mVerticalAxis.setCount(rowCount);
        invalidateStructure();
        requestLayout();
    }

    public int getColumnCount() {
        return this.mHorizontalAxis.getCount();
    }

    public void setColumnCount(int columnCount) {
        this.mHorizontalAxis.setCount(columnCount);
        invalidateStructure();
        requestLayout();
    }

    public boolean getUseDefaultMargins() {
        return this.mUseDefaultMargins;
    }

    public void setUseDefaultMargins(boolean useDefaultMargins) {
        this.mUseDefaultMargins = useDefaultMargins;
        requestLayout();
    }

    public int getAlignmentMode() {
        return this.mAlignmentMode;
    }

    public void setAlignmentMode(int alignmentMode) {
        this.mAlignmentMode = alignmentMode;
        requestLayout();
    }

    public boolean isRowOrderPreserved() {
        return this.mVerticalAxis.isOrderPreserved();
    }

    public void setRowOrderPreserved(boolean rowOrderPreserved) {
        this.mVerticalAxis.setOrderPreserved(rowOrderPreserved);
        invalidateStructure();
        requestLayout();
    }

    public boolean isColumnOrderPreserved() {
        return this.mHorizontalAxis.isOrderPreserved();
    }

    public void setColumnOrderPreserved(boolean columnOrderPreserved) {
        this.mHorizontalAxis.setOrderPreserved(columnOrderPreserved);
        invalidateStructure();
        requestLayout();
    }

    public Printer getPrinter() {
        return this.mPrinter;
    }

    public void setPrinter(Printer printer) {
        this.mPrinter = printer == null ? NO_PRINTER : printer;
    }

    static int max2(int[] a, int valueIfEmpty) {
        int result = valueIfEmpty;
        for (int max : a) {
            result = Math.max(result, max);
        }
        return result;
    }

    static <T> T[] append(T[] a, T[] b) {
        Object[] result = (Object[]) Array.newInstance(a.getClass().getComponentType(), a.length + b.length);
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    static Alignment getAlignment(int gravity, boolean horizontal) {
        int flags = (gravity & (horizontal ? 7 : 112)) >> (horizontal ? 0 : 4);
        if (flags == 1) {
            return CENTER;
        }
        if (flags == 3) {
            return horizontal ? LEFT : TOP;
        } else if (flags == 5) {
            return horizontal ? RIGHT : BOTTOM;
        } else if (flags == 7) {
            return FILL;
        } else {
            if (flags == Gravity.START) {
                return START;
            }
            if (flags != Gravity.END) {
                return UNDEFINED_ALIGNMENT;
            }
            return END;
        }
    }

    private int getDefaultMargin(View c, boolean horizontal, boolean leading) {
        if (c.getClass() == Space.class) {
            return 0;
        }
        return this.mDefaultGap / 2;
    }

    private int getDefaultMargin(View c, boolean isAtEdge, boolean horizontal, boolean leading) {
        return getDefaultMargin(c, horizontal, leading);
    }

    private int getDefaultMargin(View c, LayoutParams p, boolean horizontal, boolean leading) {
        boolean isAtEdge = false;
        if (!this.mUseDefaultMargins) {
            return 0;
        }
        Spec spec = horizontal ? p.columnSpec : p.rowSpec;
        Axis axis = horizontal ? this.mHorizontalAxis : this.mVerticalAxis;
        Interval span = spec.span;
        boolean leading1 = (horizontal && isLayoutRtl()) ? !leading : leading;
        if (leading1 ? span.min != 0 : span.max != axis.getCount()) {
            isAtEdge = true;
        }
        return getDefaultMargin(c, isAtEdge, horizontal, leading);
    }

    int getMargin1(View view, boolean horizontal, boolean leading) {
        LayoutParams lp = getLayoutParams(view);
        int margin = horizontal ? leading ? lp.leftMargin : lp.rightMargin : leading ? lp.topMargin : lp.bottomMargin;
        return margin == Integer.MIN_VALUE ? getDefaultMargin(view, lp, horizontal, leading) : margin;
    }

    private int getMargin(View view, boolean horizontal, boolean leading) {
        if (this.mAlignmentMode == 1) {
            return getMargin1(view, horizontal, leading);
        }
        Axis axis = horizontal ? this.mHorizontalAxis : this.mVerticalAxis;
        int[] margins = leading ? axis.getLeadingMargins() : axis.getTrailingMargins();
        LayoutParams lp = getLayoutParams(view);
        Spec spec = horizontal ? lp.columnSpec : lp.rowSpec;
        return margins[leading ? spec.span.min : spec.span.max];
    }

    private int getTotalMargin(View child, boolean horizontal) {
        return getMargin(child, horizontal, true) + getMargin(child, horizontal, false);
    }

    private static boolean fits(int[] a, int value, int start, int end) {
        if (end > a.length) {
            return false;
        }
        for (int i = start; i < end; i++) {
            if (a[i] > value) {
                return false;
            }
        }
        return true;
    }

    private static void procrusteanFill(int[] a, int start, int end, int value) {
        int length = a.length;
        Arrays.fill(a, Math.min(start, length), Math.min(end, length), value);
    }

    private static void setCellGroup(LayoutParams lp, int row, int rowSpan, int col, int colSpan) {
        lp.setRowSpecSpan(new Interval(row, row + rowSpan));
        lp.setColumnSpecSpan(new Interval(col, col + colSpan));
    }

    private static int clip(Interval minorRange, boolean minorWasDefined, int count) {
        int size = minorRange.size();
        if (count == 0) {
            return size;
        }
        return Math.min(size, count - (minorWasDefined ? Math.min(minorRange.min, count) : 0));
    }

    private void validateLayoutParams() {
        GridLayout gridLayout = this;
        int count = 0;
        boolean horizontal = gridLayout.mOrientation == 0;
        Axis axis = horizontal ? gridLayout.mHorizontalAxis : gridLayout.mVerticalAxis;
        if (axis.definedCount != Integer.MIN_VALUE) {
            count = axis.definedCount;
        }
        int major = 0;
        int minor = 0;
        int[] maxSizes = new int[count];
        int i = 0;
        int N = getChildCount();
        while (i < N) {
            int N2;
            LayoutParams lp = (LayoutParams) gridLayout.getChildAt(i).getLayoutParams();
            Spec majorSpec = horizontal ? lp.rowSpec : lp.columnSpec;
            Interval majorRange = majorSpec.span;
            boolean majorWasDefined = majorSpec.startDefined;
            int majorSpan = majorRange.size();
            if (majorWasDefined) {
                major = majorRange.min;
            }
            Spec minorSpec = horizontal ? lp.columnSpec : lp.rowSpec;
            Interval minorRange = minorSpec.span;
            boolean minorWasDefined = minorSpec.startDefined;
            Axis axis2 = axis;
            axis = clip(minorRange, minorWasDefined, count);
            if (minorWasDefined) {
                minor = minorRange.min;
            }
            if (count != 0) {
                if (!majorWasDefined || !minorWasDefined) {
                    while (true) {
                        N2 = N;
                        if (fits(maxSizes, major, minor, minor + axis)) {
                            break;
                        }
                        if (minorWasDefined) {
                            major++;
                        } else if (minor + axis <= count) {
                            minor++;
                        } else {
                            minor = 0;
                            major++;
                        }
                        N = N2;
                    }
                } else {
                    N2 = N;
                }
                procrusteanFill(maxSizes, minor, minor + axis, major + majorSpan);
            } else {
                N2 = N;
            }
            if (horizontal) {
                setCellGroup(lp, major, majorSpan, minor, axis);
            } else {
                setCellGroup(lp, minor, axis, major, majorSpan);
            }
            minor += axis;
            i++;
            axis = axis2;
            N = N2;
            gridLayout = this;
        }
    }

    private void invalidateStructure() {
        this.mLastLayoutParamsHashCode = 0;
        this.mHorizontalAxis.invalidateStructure();
        this.mVerticalAxis.invalidateStructure();
        invalidateValues();
    }

    private void invalidateValues() {
        if (this.mHorizontalAxis != null && this.mVerticalAxis != null) {
            this.mHorizontalAxis.invalidateValues();
            this.mVerticalAxis.invalidateValues();
        }
    }

    protected void onSetLayoutParams(View child, android.view.ViewGroup.LayoutParams layoutParams) {
        super.onSetLayoutParams(child, layoutParams);
        if (!checkLayoutParams(layoutParams)) {
            handleInvalidParams("supplied LayoutParams are of the wrong type");
        }
        invalidateStructure();
    }

    final LayoutParams getLayoutParams(View c) {
        return (LayoutParams) c.getLayoutParams();
    }

    private static void handleInvalidParams(String msg) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(msg);
        stringBuilder.append(". ");
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private void checkLayoutParams(LayoutParams lp, boolean horizontal) {
        String groupName = horizontal ? "column" : "row";
        Interval span = (horizontal ? lp.columnSpec : lp.rowSpec).span;
        if (span.min != Integer.MIN_VALUE && span.min < 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(groupName);
            stringBuilder.append(" indices must be positive");
            handleInvalidParams(stringBuilder.toString());
        }
        int count = (horizontal ? this.mHorizontalAxis : this.mVerticalAxis).definedCount;
        if (count != Integer.MIN_VALUE) {
            StringBuilder stringBuilder2;
            if (span.max > count) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(groupName);
                stringBuilder2.append(" indices (start + span) mustn't exceed the ");
                stringBuilder2.append(groupName);
                stringBuilder2.append(" count");
                handleInvalidParams(stringBuilder2.toString());
            }
            if (span.size() > count) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(groupName);
                stringBuilder2.append(" span mustn't exceed the ");
                stringBuilder2.append(groupName);
                stringBuilder2.append(" count");
                handleInvalidParams(stringBuilder2.toString());
            }
        }
    }

    protected boolean checkLayoutParams(android.view.ViewGroup.LayoutParams p) {
        if (!(p instanceof LayoutParams)) {
            return false;
        }
        LayoutParams lp = (LayoutParams) p;
        checkLayoutParams(lp, true);
        checkLayoutParams(lp, false);
        return true;
    }

    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    protected LayoutParams generateLayoutParams(android.view.ViewGroup.LayoutParams lp) {
        if (sPreserveMarginParamsInLayoutParamConversion) {
            if (lp instanceof LayoutParams) {
                return new LayoutParams((LayoutParams) lp);
            }
            if (lp instanceof MarginLayoutParams) {
                return new LayoutParams((MarginLayoutParams) lp);
            }
        }
        return new LayoutParams(lp);
    }

    private void drawLine(Canvas graphics, int x1, int y1, int x2, int y2, Paint paint) {
        int i = x1;
        int i2 = y1;
        int i3 = x2;
        int i4 = y2;
        if (isLayoutRtl()) {
            int width = getWidth();
            graphics.drawLine((float) (width - i), (float) i2, (float) (width - i3), (float) i4, paint);
            return;
        }
        graphics.drawLine((float) i, (float) i2, (float) i3, (float) i4, paint);
    }

    protected void onDebugDrawMargins(Canvas canvas, Paint paint) {
        LayoutParams lp = new LayoutParams();
        for (int i = 0; i < getChildCount(); i++) {
            View c = getChildAt(i);
            lp.setMargins(getMargin1(c, true, true), getMargin1(c, false, true), getMargin1(c, true, false), getMargin1(c, false, false));
            lp.onDebugDraw(c, canvas, paint);
        }
    }

    protected void onDebugDraw(Canvas canvas) {
        int length;
        int length2;
        int x;
        int length3;
        Paint paint = new Paint();
        paint.setStyle(Style.STROKE);
        paint.setColor(Color.argb(50, 255, 255, 255));
        Insets insets = getOpticalInsets();
        int top = getPaddingTop() + insets.top;
        int left = getPaddingLeft() + insets.left;
        int right = (getWidth() - getPaddingRight()) - insets.right;
        int bottom = (getHeight() - getPaddingBottom()) - insets.bottom;
        int[] xs = this.mHorizontalAxis.locations;
        if (xs != null) {
            length = xs.length;
            int i = 0;
            while (true) {
                length2 = length;
                if (i >= length2) {
                    break;
                }
                x = left + xs[i];
                length3 = length2;
                drawLine(canvas, x, top, x, bottom, paint);
                i++;
                length = length3;
            }
        }
        int[] ys = this.mVerticalAxis.locations;
        if (ys != null) {
            length = ys.length;
            length2 = 0;
            while (true) {
                int length4 = length;
                if (length2 >= length4) {
                    break;
                }
                x = top + ys[length2];
                length3 = length4;
                int i2 = length2;
                drawLine(canvas, left, x, right, x, paint);
                length2 = i2 + 1;
                length = length3;
            }
        }
        super.onDebugDraw(canvas);
    }

    public void onViewAdded(View child) {
        super.onViewAdded(child);
        invalidateStructure();
    }

    public void onViewRemoved(View child) {
        super.onViewRemoved(child);
        invalidateStructure();
    }

    protected void onChildVisibilityChanged(View child, int oldVisibility, int newVisibility) {
        super.onChildVisibilityChanged(child, oldVisibility, newVisibility);
        if (oldVisibility == 8 || newVisibility == 8) {
            invalidateStructure();
        }
    }

    private int computeLayoutParamsHashCode() {
        int result = 1;
        int N = getChildCount();
        for (int i = 0; i < N; i++) {
            View c = getChildAt(i);
            if (c.getVisibility() != 8) {
                result = (31 * result) + ((LayoutParams) c.getLayoutParams()).hashCode();
            }
        }
        return result;
    }

    private void consistencyCheck() {
        if (this.mLastLayoutParamsHashCode == 0) {
            validateLayoutParams();
            this.mLastLayoutParamsHashCode = computeLayoutParamsHashCode();
        } else if (this.mLastLayoutParamsHashCode != computeLayoutParamsHashCode()) {
            this.mPrinter.println("The fields of some layout parameters were modified in between layout operations. Check the javadoc for GridLayout.LayoutParams#rowSpec.");
            invalidateStructure();
            consistencyCheck();
        }
    }

    private void measureChildWithMargins2(View child, int parentWidthSpec, int parentHeightSpec, int childWidth, int childHeight) {
        child.measure(ViewGroup.getChildMeasureSpec(parentWidthSpec, getTotalMargin(child, true), childWidth), ViewGroup.getChildMeasureSpec(parentHeightSpec, getTotalMargin(child, false), childHeight));
    }

    private void measureChildrenWithMargins(int widthSpec, int heightSpec, boolean firstPass) {
        int N = getChildCount();
        int i = 0;
        while (true) {
            int N2 = N;
            if (i < N2) {
                View c = getChildAt(i);
                if (c.getVisibility() != 8) {
                    LayoutParams lp = getLayoutParams(c);
                    if (firstPass) {
                        measureChildWithMargins2(c, widthSpec, heightSpec, lp.width, lp.height);
                    } else {
                        boolean horizontal = this.mOrientation == 0;
                        Spec spec = horizontal ? lp.columnSpec : lp.rowSpec;
                        if (spec.getAbsoluteAlignment(horizontal) == FILL) {
                            Interval span = spec.span;
                            int[] locations = (horizontal ? this.mHorizontalAxis : this.mVerticalAxis).getLocations();
                            int viewSize = (locations[span.max] - locations[span.min]) - getTotalMargin(c, horizontal);
                            if (horizontal) {
                                measureChildWithMargins2(c, widthSpec, heightSpec, viewSize, lp.height);
                            } else {
                                measureChildWithMargins2(c, widthSpec, heightSpec, lp.width, viewSize);
                            }
                        }
                    }
                }
                i++;
                N = N2;
            } else {
                return;
            }
        }
    }

    static int adjust(int measureSpec, int delta) {
        return MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(measureSpec + delta), MeasureSpec.getMode(measureSpec));
    }

    protected void onMeasure(int widthSpec, int heightSpec) {
        int widthSansPadding;
        int heightSansPadding;
        consistencyCheck();
        invalidateValues();
        int hPadding = getPaddingLeft() + getPaddingRight();
        int vPadding = getPaddingTop() + getPaddingBottom();
        int widthSpecSansPadding = adjust(widthSpec, -hPadding);
        int heightSpecSansPadding = adjust(heightSpec, -vPadding);
        measureChildrenWithMargins(widthSpecSansPadding, heightSpecSansPadding, true);
        if (this.mOrientation == 0) {
            widthSansPadding = this.mHorizontalAxis.getMeasure(widthSpecSansPadding);
            measureChildrenWithMargins(widthSpecSansPadding, heightSpecSansPadding, false);
            heightSansPadding = this.mVerticalAxis.getMeasure(heightSpecSansPadding);
        } else {
            heightSansPadding = this.mVerticalAxis.getMeasure(heightSpecSansPadding);
            measureChildrenWithMargins(widthSpecSansPadding, heightSpecSansPadding, false);
            widthSansPadding = this.mHorizontalAxis.getMeasure(widthSpecSansPadding);
        }
        setMeasuredDimension(View.resolveSizeAndState(Math.max(widthSansPadding + hPadding, getSuggestedMinimumWidth()), widthSpec, 0), View.resolveSizeAndState(Math.max(heightSansPadding + vPadding, getSuggestedMinimumHeight()), heightSpec, 0));
    }

    private int getMeasurement(View c, boolean horizontal) {
        return horizontal ? c.getMeasuredWidth() : c.getMeasuredHeight();
    }

    final int getMeasurementIncludingMargin(View c, boolean horizontal) {
        if (c.getVisibility() == 8) {
            return 0;
        }
        return getMeasurement(c, horizontal) + getTotalMargin(c, horizontal);
    }

    public void requestLayout() {
        super.requestLayout();
        invalidateValues();
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        GridLayout gridLayout = this;
        consistencyCheck();
        int targetWidth = right - left;
        int targetHeight = bottom - top;
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();
        gridLayout.mHorizontalAxis.layout((targetWidth - paddingLeft) - paddingRight);
        gridLayout.mVerticalAxis.layout((targetHeight - paddingTop) - paddingBottom);
        int[] hLocations = gridLayout.mHorizontalAxis.getLocations();
        int[] vLocations = gridLayout.mVerticalAxis.getLocations();
        int N = getChildCount();
        int i = 0;
        while (true) {
            int N2 = N;
            if (i < N2) {
                int N3;
                int i2;
                View c = gridLayout.getChildAt(i);
                if (c.getVisibility() == 8) {
                    N3 = N2;
                    i2 = i;
                } else {
                    LayoutParams lp = gridLayout.getLayoutParams(c);
                    Spec columnSpec = lp.columnSpec;
                    Spec rowSpec = lp.rowSpec;
                    Interval colSpan = columnSpec.span;
                    Interval rowSpan = rowSpec.span;
                    int x1 = hLocations[colSpan.min];
                    int y1 = vLocations[rowSpan.min];
                    int cellWidth = hLocations[colSpan.max] - x1;
                    int cellHeight = vLocations[rowSpan.max] - y1;
                    int pWidth = gridLayout.getMeasurement(c, true);
                    int pHeight = gridLayout.getMeasurement(c, false);
                    Alignment hAlign = columnSpec.getAbsoluteAlignment(true);
                    Alignment vAlign = rowSpec.getAbsoluteAlignment(false);
                    Bounds boundsX = (Bounds) gridLayout.mHorizontalAxis.getGroupBounds().getValue(i);
                    Bounds boundsY = (Bounds) gridLayout.mVerticalAxis.getGroupBounds().getValue(i);
                    int gravityOffsetX = hAlign.getGravityOffset(c, cellWidth - boundsX.size(true));
                    int gravityOffsetY = vAlign.getGravityOffset(c, cellHeight - boundsY.size(true));
                    int leftMargin = gridLayout.getMargin(c, true, true);
                    Bounds boundsY2 = boundsY;
                    int topMargin = gridLayout.getMargin(c, false, true);
                    int rightMargin = gridLayout.getMargin(c, true, false);
                    int sumMarginsX = leftMargin + rightMargin;
                    int sumMarginsY = topMargin + gridLayout.getMargin(c, false, false);
                    GridLayout gridLayout2 = gridLayout;
                    View c2 = c;
                    N3 = N2;
                    i2 = i;
                    int alignmentOffsetX = boundsX.getOffset(gridLayout2, c, hAlign, pWidth + sumMarginsX, 1);
                    View c3 = c2;
                    int alignmentOffsetY = boundsY2.getOffset(gridLayout2, c3, vAlign, pHeight + sumMarginsY, false);
                    N = hAlign.getSizeInCell(c3, pWidth, cellWidth - sumMarginsX);
                    int height = vAlign.getSizeInCell(c3, pHeight, cellHeight - sumMarginsY);
                    N2 = (x1 + gravityOffsetX) + alignmentOffsetX;
                    if (isLayoutRtl()) {
                        i = (((targetWidth - N) - paddingRight) - rightMargin) - N2;
                    } else {
                        i = (paddingLeft + leftMargin) + N2;
                    }
                    alignmentOffsetY = (((paddingTop + y1) + gravityOffsetY) + alignmentOffsetY) + topMargin;
                    if (!(N == c3.getMeasuredWidth() && height == c3.getMeasuredHeight())) {
                        c3.measure(MeasureSpec.makeMeasureSpec(N, 1073741824), MeasureSpec.makeMeasureSpec(height, 1073741824));
                    }
                    c3.layout(i, alignmentOffsetY, i + N, alignmentOffsetY + height);
                }
                i = i2 + 1;
                N = N3;
                gridLayout = this;
            } else {
                return;
            }
        }
    }

    public CharSequence getAccessibilityClassName() {
        return GridLayout.class.getName();
    }

    public static Spec spec(int start, int size, Alignment alignment, float weight) {
        return new Spec(start != Integer.MIN_VALUE, start, size, alignment, weight, null);
    }

    public static Spec spec(int start, Alignment alignment, float weight) {
        return spec(start, 1, alignment, weight);
    }

    public static Spec spec(int start, int size, float weight) {
        return spec(start, size, UNDEFINED_ALIGNMENT, weight);
    }

    public static Spec spec(int start, float weight) {
        return spec(start, 1, weight);
    }

    public static Spec spec(int start, int size, Alignment alignment) {
        return spec(start, size, alignment, 0.0f);
    }

    public static Spec spec(int start, Alignment alignment) {
        return spec(start, 1, alignment);
    }

    public static Spec spec(int start, int size) {
        return spec(start, size, UNDEFINED_ALIGNMENT);
    }

    public static Spec spec(int start) {
        return spec(start, 1);
    }

    private static Alignment createSwitchingAlignment(final Alignment ltr, final Alignment rtl) {
        return new Alignment() {
            int getGravityOffset(View view, int cellDelta) {
                return (!view.isLayoutRtl() ? ltr : rtl).getGravityOffset(view, cellDelta);
            }

            public int getAlignmentValue(View view, int viewSize, int mode) {
                return (!view.isLayoutRtl() ? ltr : rtl).getAlignmentValue(view, viewSize, mode);
            }
        };
    }

    static boolean canStretch(int flexibility) {
        return (flexibility & 2) != 0;
    }
}
