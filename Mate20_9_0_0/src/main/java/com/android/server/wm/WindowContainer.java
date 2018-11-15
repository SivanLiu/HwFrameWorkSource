package com.android.server.wm;

import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.HwPCUtils;
import android.util.Pools.SynchronizedPool;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.MagnificationSpec;
import android.view.SurfaceControl;
import android.view.SurfaceControl.Builder;
import android.view.SurfaceControl.Transaction;
import android.view.SurfaceSession;
import android.view.WindowManager;
import com.android.internal.util.ToBooleanFunction;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.Predicate;

class WindowContainer<E extends WindowContainer> extends ConfigurationContainer<E> implements Comparable<WindowContainer>, Animatable {
    static final int ANIMATION_LAYER_BOOSTED = 1;
    static final int ANIMATION_LAYER_HOME = 2;
    static final int ANIMATION_LAYER_STANDARD = 0;
    static final int POSITION_BOTTOM = Integer.MIN_VALUE;
    static final int POSITION_TOP = Integer.MAX_VALUE;
    private static final String TAG = "WindowManager";
    protected final WindowList<E> mChildren = new WindowList();
    private boolean mCommittedReparentToAnimationLeash;
    private final SynchronizedPool<ForAllWindowsConsumerWrapper> mConsumerWrapperPool = new SynchronizedPool(3);
    WindowContainerController mController;
    private Display mDefaultDisplay;
    private DisplayInfo mDefaultDisplayInfo = new DisplayInfo();
    int mHeight;
    private int mLastLayer = 0;
    private SurfaceControl mLastRelativeToLayer = null;
    protected final Point mLastSurfacePosition = new Point();
    float mLazyScale = 0.75f;
    protected int mOrientation = -1;
    private WindowContainer<WindowContainer> mParent = null;
    protected final Transaction mPendingTransaction;
    protected final WindowManagerService mService;
    protected final SurfaceAnimator mSurfaceAnimator;
    protected SurfaceControl mSurfaceControl;
    private final LinkedList<WindowContainer> mTmpChain1 = new LinkedList();
    private final LinkedList<WindowContainer> mTmpChain2 = new LinkedList();
    private final Point mTmpPos = new Point();
    private int mTreeWeight = 1;
    int mWidth;
    private WindowManager mWindowManager;

    @interface AnimationLayer {
    }

    private final class ForAllWindowsConsumerWrapper implements ToBooleanFunction<WindowState> {
        private Consumer<WindowState> mConsumer;

        private ForAllWindowsConsumerWrapper() {
        }

        void setConsumer(Consumer<WindowState> consumer) {
            this.mConsumer = consumer;
        }

        public boolean apply(WindowState w) {
            this.mConsumer.accept(w);
            return false;
        }

        void release() {
            this.mConsumer = null;
            WindowContainer.this.mConsumerWrapperPool.release(this);
        }
    }

    WindowContainer(WindowManagerService service) {
        this.mService = service;
        this.mPendingTransaction = service.mTransactionFactory.make();
        this.mSurfaceAnimator = new SurfaceAnimator(this, new -$$Lambda$yVRF8YoeNdTa8GR1wDStVsHu8xM(this), service);
        this.mWindowManager = (WindowManager) this.mService.mContext.getSystemService("window");
    }

    protected final WindowContainer getParent() {
        return this.mParent;
    }

    protected int getChildCount() {
        return this.mChildren.size();
    }

    protected E getChildAt(int index) {
        return (WindowContainer) this.mChildren.get(index);
    }

    public void onConfigurationChanged(Configuration newParentConfig) {
        super.onConfigurationChanged(newParentConfig);
        updateSurfacePosition();
        scheduleAnimation();
    }

    protected final void setParent(WindowContainer<WindowContainer> parent) {
        this.mParent = parent;
        if (this.mParent != null) {
            onConfigurationChanged(this.mParent.getConfiguration());
            onMergedOverrideConfigurationChanged();
        }
        onParentSet();
    }

    void onParentSet() {
        if (this.mParent != null) {
            if (this.mSurfaceControl == null) {
                this.mSurfaceControl = makeSurface().build();
                getPendingTransaction().show(this.mSurfaceControl);
                updateSurfacePosition();
            } else {
                reparentSurfaceControl(getPendingTransaction(), this.mParent.mSurfaceControl);
            }
            this.mParent.assignChildLayers();
            scheduleAnimation();
        }
    }

    protected void addChild(E child, Comparator<E> comparator) {
        if (child.getParent() == null) {
            int positionToAdd = -1;
            if (comparator != null) {
                int count = this.mChildren.size();
                for (int i = 0; i < count; i++) {
                    if (comparator.compare(child, (WindowContainer) this.mChildren.get(i)) < 0) {
                        positionToAdd = i;
                        break;
                    }
                }
            }
            if (positionToAdd == -1) {
                this.mChildren.add(child);
            } else {
                this.mChildren.add(positionToAdd, child);
            }
            onChildAdded(child);
            child.setParent(this);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("addChild: container=");
        stringBuilder.append(child.getName());
        stringBuilder.append(" is already a child of container=");
        stringBuilder.append(child.getParent().getName());
        stringBuilder.append(" can't add to container=");
        stringBuilder.append(getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    void addChild(E child, int index) {
        if (child.getParent() == null) {
            this.mChildren.add(index, child);
            onChildAdded(child);
            child.setParent(this);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("addChild: container=");
        stringBuilder.append(child.getName());
        stringBuilder.append(" is already a child of container=");
        stringBuilder.append(child.getParent().getName());
        stringBuilder.append(" can't add to container=");
        stringBuilder.append(getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private void onChildAdded(WindowContainer child) {
        this.mTreeWeight += child.mTreeWeight;
        WindowContainer parent = getParent();
        while (parent != null) {
            parent.mTreeWeight += child.mTreeWeight;
            parent = parent.getParent();
        }
    }

    void removeChild(E child) {
        if (this.mChildren.remove(child)) {
            onChildRemoved(child);
            child.setParent(null);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("removeChild: container=");
        stringBuilder.append(child.getName());
        stringBuilder.append(" is not a child of container=");
        stringBuilder.append(getName());
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private void onChildRemoved(WindowContainer child) {
        this.mTreeWeight -= child.mTreeWeight;
        WindowContainer parent = getParent();
        while (parent != null) {
            parent.mTreeWeight -= child.mTreeWeight;
            parent = parent.getParent();
        }
    }

    void removeImmediately() {
        while (!this.mChildren.isEmpty()) {
            WindowContainer child = (WindowContainer) this.mChildren.peekLast();
            child.removeImmediately();
            if (this.mChildren.remove(child)) {
                onChildRemoved(child);
            }
        }
        if (this.mSurfaceControl != null) {
            this.mPendingTransaction.destroy(this.mSurfaceControl);
            if (this.mParent != null) {
                this.mParent.getPendingTransaction().merge(this.mPendingTransaction);
            }
            this.mSurfaceControl = null;
            scheduleAnimation();
        }
        if (this.mParent != null) {
            this.mParent.removeChild(this);
        }
        if (this.mController != null) {
            setController(null);
        }
    }

    int getPrefixOrderIndex() {
        if (this.mParent == null) {
            return 0;
        }
        return this.mParent.getPrefixOrderIndex(this);
    }

    private int getPrefixOrderIndex(WindowContainer child) {
        int order = 0;
        for (int i = 0; i < this.mChildren.size(); i++) {
            WindowContainer childI = (WindowContainer) this.mChildren.get(i);
            if (child == childI) {
                break;
            }
            order += childI.mTreeWeight;
        }
        if (this.mParent != null) {
            order += this.mParent.getPrefixOrderIndex(this);
        }
        return order + 1;
    }

    void removeIfPossible() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((WindowContainer) this.mChildren.get(i)).removeIfPossible();
        }
    }

    boolean hasChild(E child) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            E current = (WindowContainer) this.mChildren.get(i);
            if (current == child || current.hasChild(child)) {
                return true;
            }
        }
        return false;
    }

    void positionChildAt(int position, E child, boolean includingParents) {
        StringBuilder stringBuilder;
        if (child.getParent() != this) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("removeChild: container=");
            stringBuilder.append(child.getName());
            stringBuilder.append(" is not a child of container=");
            stringBuilder.append(getName());
            stringBuilder.append(" current parent=");
            stringBuilder.append(child.getParent());
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if ((position >= 0 || position == Integer.MIN_VALUE) && (position <= this.mChildren.size() || position == Integer.MAX_VALUE)) {
            if (position >= this.mChildren.size() - 1) {
                position = Integer.MAX_VALUE;
            } else if (position == 0) {
                position = Integer.MIN_VALUE;
            }
            if (position == Integer.MIN_VALUE) {
                if (this.mChildren.peekFirst() != child) {
                    this.mChildren.remove(child);
                    this.mChildren.addFirst(child);
                }
                if (includingParents && getParent() != null) {
                    getParent().positionChildAt(Integer.MIN_VALUE, this, true);
                }
            } else if (position != Integer.MAX_VALUE) {
                this.mChildren.remove(child);
                this.mChildren.add(position, child);
            } else {
                if (this.mChildren.peekLast() != child) {
                    this.mChildren.remove(child);
                    this.mChildren.add(child);
                }
                if (includingParents && getParent() != null) {
                    getParent().positionChildAt(Integer.MAX_VALUE, this, true);
                }
            }
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("positionAt: invalid position=");
            stringBuilder.append(position);
            stringBuilder.append(", children number=");
            stringBuilder.append(this.mChildren.size());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public void onOverrideConfigurationChanged(Configuration overrideConfiguration) {
        int diff = diffOverrideBounds(overrideConfiguration.windowConfiguration.getBounds());
        super.onOverrideConfigurationChanged(overrideConfiguration);
        if (this.mParent != null) {
            this.mParent.onDescendantOverrideConfigurationChanged();
        }
        if (diff != 0) {
            if ((diff & 2) == 2) {
                onResize();
            } else {
                onMovedByResize();
            }
        }
    }

    void onDescendantOverrideConfigurationChanged() {
        if (this.mParent != null) {
            this.mParent.onDescendantOverrideConfigurationChanged();
        }
    }

    void onDisplayChanged(DisplayContent dc) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((WindowContainer) this.mChildren.get(i)).onDisplayChanged(dc);
        }
    }

    void setWaitingForDrawnIfResizingChanged() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((WindowContainer) this.mChildren.get(i)).setWaitingForDrawnIfResizingChanged();
        }
    }

    void onResize() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((WindowContainer) this.mChildren.get(i)).onParentResize();
        }
    }

    void onParentResize() {
        if (!hasOverrideBounds()) {
            onResize();
        }
    }

    void onMovedByResize() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((WindowContainer) this.mChildren.get(i)).onMovedByResize();
        }
    }

    void resetDragResizingChangeReported() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((WindowContainer) this.mChildren.get(i)).resetDragResizingChangeReported();
        }
    }

    void forceWindowsScaleableInTransaction(boolean force) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((WindowContainer) this.mChildren.get(i)).forceWindowsScaleableInTransaction(force);
        }
    }

    boolean isSelfOrChildAnimating() {
        if (isSelfAnimating()) {
            return true;
        }
        for (int j = this.mChildren.size() - 1; j >= 0; j--) {
            if (((WindowContainer) this.mChildren.get(j)).isSelfOrChildAnimating()) {
                return true;
            }
        }
        return false;
    }

    boolean isAnimating() {
        return isSelfAnimating() || (this.mParent != null && this.mParent.isAnimating());
    }

    boolean isAppAnimating() {
        for (int j = this.mChildren.size() - 1; j >= 0; j--) {
            if (((WindowContainer) this.mChildren.get(j)).isAppAnimating()) {
                return true;
            }
        }
        return false;
    }

    boolean isSelfAnimating() {
        return this.mSurfaceAnimator.isAnimating();
    }

    void sendAppVisibilityToClients() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((WindowContainer) this.mChildren.get(i)).sendAppVisibilityToClients();
        }
    }

    boolean hasContentToDisplay() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            if (((WindowContainer) this.mChildren.get(i)).hasContentToDisplay()) {
                return true;
            }
        }
        return false;
    }

    boolean isVisible() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            if (((WindowContainer) this.mChildren.get(i)).isVisible()) {
                return true;
            }
        }
        return false;
    }

    boolean isOnTop() {
        return getParent().getTopChild() == this && getParent().isOnTop();
    }

    E getTopChild() {
        return (WindowContainer) this.mChildren.peekLast();
    }

    boolean checkCompleteDeferredRemoval() {
        boolean stillDeferringRemoval = false;
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            stillDeferringRemoval |= ((WindowContainer) this.mChildren.get(i)).checkCompleteDeferredRemoval();
        }
        return stillDeferringRemoval;
    }

    void checkAppWindowsReadyToShow() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((WindowContainer) this.mChildren.get(i)).checkAppWindowsReadyToShow();
        }
    }

    void onAppTransitionDone() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((WindowContainer) this.mChildren.get(i)).onAppTransitionDone();
        }
    }

    void setOrientation(int orientation) {
        this.mOrientation = orientation;
    }

    int getOrientation() {
        return getOrientation(this.mOrientation);
    }

    int getOrientation(int candidate) {
        if (!fillsParent()) {
            return -2;
        }
        if (this.mOrientation != -2 && this.mOrientation != -1) {
            return this.mOrientation;
        }
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowContainer wc = (WindowContainer) this.mChildren.get(i);
            int orientation = wc.getOrientation(candidate == 3 ? 3 : -2);
            if (orientation == 3) {
                candidate = orientation;
            } else if (orientation != -2 && (wc.fillsParent() || orientation != -1)) {
                return orientation;
            }
        }
        return candidate;
    }

    boolean fillsParent() {
        return false;
    }

    void switchUser() {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((WindowContainer) this.mChildren.get(i)).switchUser();
        }
    }

    boolean forAllWindows(ToBooleanFunction<WindowState> callback, boolean traverseTopToBottom) {
        int i;
        if (traverseTopToBottom) {
            for (i = this.mChildren.size() - 1; i >= 0; i--) {
                if (((WindowContainer) this.mChildren.get(i)).forAllWindows((ToBooleanFunction) callback, traverseTopToBottom)) {
                    return true;
                }
            }
        } else {
            i = this.mChildren.size();
            for (int i2 = 0; i2 < i; i2++) {
                if (((WindowContainer) this.mChildren.get(i2)).forAllWindows((ToBooleanFunction) callback, traverseTopToBottom)) {
                    return true;
                }
            }
        }
        return false;
    }

    void forAllWindows(Consumer<WindowState> callback, boolean traverseTopToBottom) {
        ToBooleanFunction wrapper = obtainConsumerWrapper(callback);
        forAllWindows(wrapper, traverseTopToBottom);
        wrapper.release();
    }

    void forAllTasks(Consumer<Task> callback) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            ((WindowContainer) this.mChildren.get(i)).forAllTasks(callback);
        }
    }

    WindowState getWindow(Predicate<WindowState> callback) {
        for (int i = this.mChildren.size() - 1; i >= 0; i--) {
            WindowState w = ((WindowContainer) this.mChildren.get(i)).getWindow(callback);
            if (w != null) {
                return w;
            }
        }
        return null;
    }

    public int compareTo(WindowContainer other) {
        if (this == other) {
            return 0;
        }
        int i = -1;
        if (this.mParent == null || this.mParent != other.mParent) {
            LinkedList<WindowContainer> thisParentChain = this.mTmpChain1;
            LinkedList<WindowContainer> otherParentChain = this.mTmpChain2;
            try {
                getParents(thisParentChain);
                other.getParents(otherParentChain);
                WindowContainer commonAncestor = null;
                WindowContainer thisTop = (WindowContainer) thisParentChain.peekLast();
                WindowContainer otherTop = (WindowContainer) otherParentChain.peekLast();
                while (thisTop != null && otherTop != null && thisTop == otherTop) {
                    commonAncestor = (WindowContainer) thisParentChain.removeLast();
                    otherParentChain.removeLast();
                    thisTop = (WindowContainer) thisParentChain.peekLast();
                    otherTop = (WindowContainer) otherParentChain.peekLast();
                }
                if (commonAncestor == null) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("No in the same hierarchy this=");
                    stringBuilder.append(thisParentChain);
                    stringBuilder.append(" other=");
                    stringBuilder.append(otherParentChain);
                    throw new IllegalArgumentException(stringBuilder.toString());
                } else if (commonAncestor == this) {
                    return -1;
                } else {
                    if (commonAncestor == other) {
                        this.mTmpChain1.clear();
                        this.mTmpChain2.clear();
                        return 1;
                    }
                    WindowList<WindowContainer> list = commonAncestor.mChildren;
                    if (list.indexOf(thisParentChain.peekLast()) > list.indexOf(otherParentChain.peekLast())) {
                        i = 1;
                    }
                    this.mTmpChain1.clear();
                    this.mTmpChain2.clear();
                    return i;
                }
            } finally {
                this.mTmpChain1.clear();
                this.mTmpChain2.clear();
            }
        } else {
            WindowList<WindowContainer> list2 = this.mParent.mChildren;
            if (list2.indexOf(this) > list2.indexOf(other)) {
                i = 1;
            }
            return i;
        }
    }

    private void getParents(LinkedList<WindowContainer> parents) {
        parents.clear();
        WindowContainer current = this;
        do {
            parents.addLast(current);
            current = current.mParent;
        } while (current != null);
    }

    WindowContainerController getController() {
        return this.mController;
    }

    void setController(WindowContainerController controller) {
        if (this.mController == null || controller == null) {
            if (controller != null) {
                controller.setContainer(this);
            } else if (this.mController != null) {
                this.mController.setContainer(null);
            }
            this.mController = controller;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Can't set controller=");
        stringBuilder.append(this.mController);
        stringBuilder.append(" for container=");
        stringBuilder.append(this);
        stringBuilder.append(" Already set to=");
        stringBuilder.append(this.mController);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    Builder makeSurface() {
        return getParent().makeChildSurface(this);
    }

    Builder makeChildSurface(WindowContainer child) {
        WindowContainer p = getParent();
        if (p != null) {
            return p.makeChildSurface(child).setParent(this.mSurfaceControl);
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WindowContainer makeChildSurface get null parent ");
        stringBuilder.append(this);
        Slog.e(str, stringBuilder.toString());
        if (this instanceof TaskStack) {
            return this.mService.getDefaultDisplayContentLocked().mTaskStackContainers.makeChildSurface(child).setParent(this.mSurfaceControl);
        }
        return null;
    }

    public SurfaceControl getParentSurfaceControl() {
        WindowContainer parent = getParent();
        if (parent == null) {
            return null;
        }
        return parent.getSurfaceControl();
    }

    boolean shouldMagnify() {
        if (this.mSurfaceControl == null) {
            return false;
        }
        for (int i = 0; i < this.mChildren.size(); i++) {
            if (!((WindowContainer) this.mChildren.get(i)).shouldMagnify()) {
                return false;
            }
        }
        return true;
    }

    SurfaceSession getSession() {
        if (getParent() != null) {
            return getParent().getSession();
        }
        return null;
    }

    void assignLayer(Transaction t, int layer) {
        boolean changed = (layer == this.mLastLayer && this.mLastRelativeToLayer == null) ? false : true;
        if (this.mSurfaceControl != null && changed) {
            setLayer(t, layer);
            this.mLastLayer = layer;
            this.mLastRelativeToLayer = null;
        }
    }

    void assignRelativeLayer(Transaction t, SurfaceControl relativeTo, int layer) {
        boolean changed = (layer == this.mLastLayer && this.mLastRelativeToLayer == relativeTo) ? false : true;
        if (this.mSurfaceControl != null && changed) {
            setRelativeLayer(t, relativeTo, layer);
            this.mLastLayer = layer;
            this.mLastRelativeToLayer = relativeTo;
        }
    }

    protected void setLayer(Transaction t, int layer) {
        this.mSurfaceAnimator.setLayer(t, layer);
    }

    protected void setRelativeLayer(Transaction t, SurfaceControl relativeTo, int layer) {
        this.mSurfaceAnimator.setRelativeLayer(t, relativeTo, layer);
    }

    protected void reparentSurfaceControl(Transaction t, SurfaceControl newParent) {
        this.mSurfaceAnimator.reparent(t, newParent);
    }

    void assignChildLayers(Transaction t) {
        int j;
        int j2 = 0;
        int layer = 0;
        for (j = 0; j < this.mChildren.size(); j++) {
            WindowContainer wc = (WindowContainer) this.mChildren.get(j);
            wc.assignChildLayers(t);
            if (!wc.needsZBoost()) {
                int layer2 = layer + 1;
                wc.assignLayer(t, layer);
                layer = layer2;
            }
        }
        while (true) {
            j = j2;
            if (j < this.mChildren.size()) {
                WindowContainer wc2 = (WindowContainer) this.mChildren.get(j);
                if (wc2.needsZBoost()) {
                    int layer3 = layer + 1;
                    wc2.assignLayer(t, layer);
                    layer = layer3;
                }
                j2 = j + 1;
            } else {
                return;
            }
        }
    }

    void assignChildLayers() {
        assignChildLayers(getPendingTransaction());
        scheduleAnimation();
    }

    boolean needsZBoost() {
        for (int i = 0; i < this.mChildren.size(); i++) {
            if (((WindowContainer) this.mChildren.get(i)).needsZBoost()) {
                return true;
            }
        }
        return false;
    }

    public void writeToProto(ProtoOutputStream proto, long fieldId, boolean trim) {
        long token = proto.start(fieldId);
        super.writeToProto(proto, 1146756268033L, trim);
        proto.write(1120986464258L, this.mOrientation);
        proto.write(1133871366147L, isVisible());
        this.mSurfaceAnimator.writeToProto(proto, 1146756268036L);
        proto.end(token);
    }

    private ForAllWindowsConsumerWrapper obtainConsumerWrapper(Consumer<WindowState> consumer) {
        ForAllWindowsConsumerWrapper wrapper = (ForAllWindowsConsumerWrapper) this.mConsumerWrapperPool.acquire();
        if (wrapper == null) {
            wrapper = new ForAllWindowsConsumerWrapper();
        }
        wrapper.setConsumer(consumer);
        return wrapper;
    }

    void applyMagnificationSpec(Transaction t, MagnificationSpec spec) {
        if (shouldMagnify()) {
            t.setMatrix(this.mSurfaceControl, spec.scale, 0.0f, 0.0f, spec.scale).setPosition(this.mSurfaceControl, spec.offsetX, spec.offsetY);
            return;
        }
        for (int i = 0; i < this.mChildren.size(); i++) {
            ((WindowContainer) this.mChildren.get(i)).applyMagnificationSpec(t, spec);
        }
    }

    void prepareSurfaces() {
        SurfaceControl.mergeToGlobalTransaction(getPendingTransaction());
        this.mCommittedReparentToAnimationLeash = this.mSurfaceAnimator.hasLeash();
        for (int i = 0; i < this.mChildren.size(); i++) {
            ((WindowContainer) this.mChildren.get(i)).prepareSurfaces();
        }
    }

    boolean hasCommittedReparentToAnimationLeash() {
        return this.mCommittedReparentToAnimationLeash;
    }

    void scheduleAnimation() {
        if (this.mParent != null) {
            this.mParent.scheduleAnimation();
        }
    }

    public SurfaceControl getSurfaceControl() {
        return this.mSurfaceControl;
    }

    public Transaction getPendingTransaction() {
        return this.mPendingTransaction;
    }

    void startAnimation(Transaction t, AnimationAdapter anim, boolean hidden) {
        this.mSurfaceAnimator.startAnimation(t, anim, hidden);
    }

    void transferAnimation(WindowContainer from) {
        this.mSurfaceAnimator.transferAnimation(from.mSurfaceAnimator);
    }

    void cancelAnimation() {
        this.mSurfaceAnimator.cancelAnimation();
    }

    public Builder makeAnimationLeash() {
        return makeSurface();
    }

    public SurfaceControl getAnimationLeashParent() {
        return getParentSurfaceControl();
    }

    SurfaceControl getAppAnimationLayer(@AnimationLayer int animationLayer) {
        WindowContainer parent = getParent();
        if (parent != null) {
            return parent.getAppAnimationLayer(animationLayer);
        }
        return null;
    }

    public void commitPendingTransaction() {
        scheduleAnimation();
    }

    void reassignLayer(Transaction t) {
        WindowContainer parent = getParent();
        if (parent != null) {
            parent.assignChildLayers(t);
        }
    }

    public void onAnimationLeashCreated(Transaction t, SurfaceControl leash) {
        this.mLastLayer = -1;
        reassignLayer(t);
    }

    public void onAnimationLeashDestroyed(Transaction t) {
        this.mLastLayer = -1;
        reassignLayer(t);
    }

    protected void onAnimationFinished() {
    }

    AnimationAdapter getAnimation() {
        return this.mSurfaceAnimator.getAnimation();
    }

    void startDelayingAnimationStart() {
        this.mSurfaceAnimator.startDelayingAnimationStart();
    }

    void endDelayingAnimationStart() {
        this.mSurfaceAnimator.endDelayingAnimationStart();
    }

    public int getSurfaceWidth() {
        return this.mSurfaceControl.getWidth();
    }

    public int getSurfaceHeight() {
        return this.mSurfaceControl.getHeight();
    }

    void dump(PrintWriter pw, String prefix, boolean dumpAll) {
        if (this.mSurfaceAnimator.isAnimating()) {
            pw.print(prefix);
            pw.println("ContainerAnimator:");
            SurfaceAnimator surfaceAnimator = this.mSurfaceAnimator;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("  ");
            surfaceAnimator.dump(pw, stringBuilder.toString());
        }
    }

    void updateSurfacePosition() {
        if (this.mSurfaceControl != null) {
            getRelativePosition(this.mTmpPos);
            if ((this instanceof TaskStack) && inMultiWindowMode() && !HwPCUtils.isPcDynamicStack(((TaskStack) this).mStackId)) {
                updatedisplayinfo();
                float ratio = 1.0f;
                float pendingX = 0.0f;
                float pendingY = 0.0f;
                if (this.mService.getLazyMode() == 1) {
                    ratio = this.mLazyScale;
                    pendingY = ((float) this.mHeight) * (1.0f - this.mLazyScale);
                } else if (this.mService.getLazyMode() == 2) {
                    ratio = this.mLazyScale;
                    pendingX = ((float) this.mWidth) * (1.0f - this.mLazyScale);
                    pendingY = ((float) this.mHeight) * (1.0f - this.mLazyScale);
                }
                this.mTmpPos.x = (int) ((((float) this.mTmpPos.x) * ratio) + pendingX);
                this.mTmpPos.y = (int) ((((float) this.mTmpPos.y) * ratio) + pendingY);
            }
            if (!(this.mService == null || this.mService.mHwWMSEx == null)) {
                Rect bound = new Rect(this.mTmpPos.x, this.mTmpPos.y, -1, -1);
                this.mService.mHwWMSEx.updateDimPositionForPCMode(this, bound);
                this.mTmpPos.x = bound.left;
                this.mTmpPos.y = bound.top;
            }
            if (!this.mTmpPos.equals(this.mLastSurfacePosition)) {
                getPendingTransaction().setPosition(this.mSurfaceControl, (float) this.mTmpPos.x, (float) this.mTmpPos.y);
                this.mLastSurfacePosition.set(this.mTmpPos.x, this.mTmpPos.y);
            }
        }
    }

    private void updatedisplayinfo() {
        this.mDefaultDisplay = this.mWindowManager.getDefaultDisplay();
        this.mDefaultDisplay.getDisplayInfo(this.mDefaultDisplayInfo);
        boolean isPortrait = this.mDefaultDisplayInfo.logicalHeight > this.mDefaultDisplayInfo.logicalWidth;
        this.mWidth = isPortrait ? this.mDefaultDisplayInfo.logicalWidth : this.mDefaultDisplayInfo.logicalHeight;
        this.mHeight = isPortrait ? this.mDefaultDisplayInfo.logicalHeight : this.mDefaultDisplayInfo.logicalWidth;
    }

    void getRelativePosition(Point outPos) {
        Rect bounds = getBounds();
        outPos.set(bounds.left, bounds.top);
        WindowContainer parent = getParent();
        if (parent != null) {
            Rect parentBounds = parent.getBounds();
            outPos.offset(-parentBounds.left, -parentBounds.top);
        }
    }

    Dimmer getDimmer() {
        if (this.mParent == null) {
            return null;
        }
        return this.mParent.getDimmer();
    }

    int getLayer() {
        return this.mLastLayer;
    }
}
