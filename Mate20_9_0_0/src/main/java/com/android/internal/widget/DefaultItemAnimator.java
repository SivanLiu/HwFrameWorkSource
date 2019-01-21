package com.android.internal.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.ViewPropertyAnimator;
import com.android.internal.widget.RecyclerView.ViewHolder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DefaultItemAnimator extends SimpleItemAnimator {
    private static final boolean DEBUG = false;
    private static TimeInterpolator sDefaultInterpolator;
    ArrayList<ViewHolder> mAddAnimations = new ArrayList();
    ArrayList<ArrayList<ViewHolder>> mAdditionsList = new ArrayList();
    ArrayList<ViewHolder> mChangeAnimations = new ArrayList();
    ArrayList<ArrayList<ChangeInfo>> mChangesList = new ArrayList();
    ArrayList<ViewHolder> mMoveAnimations = new ArrayList();
    ArrayList<ArrayList<MoveInfo>> mMovesList = new ArrayList();
    private ArrayList<ViewHolder> mPendingAdditions = new ArrayList();
    private ArrayList<ChangeInfo> mPendingChanges = new ArrayList();
    private ArrayList<MoveInfo> mPendingMoves = new ArrayList();
    private ArrayList<ViewHolder> mPendingRemovals = new ArrayList();
    ArrayList<ViewHolder> mRemoveAnimations = new ArrayList();

    private static class ChangeInfo {
        public int fromX;
        public int fromY;
        public ViewHolder newHolder;
        public ViewHolder oldHolder;
        public int toX;
        public int toY;

        private ChangeInfo(ViewHolder oldHolder, ViewHolder newHolder) {
            this.oldHolder = oldHolder;
            this.newHolder = newHolder;
        }

        ChangeInfo(ViewHolder oldHolder, ViewHolder newHolder, int fromX, int fromY, int toX, int toY) {
            this(oldHolder, newHolder);
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ChangeInfo{oldHolder=");
            stringBuilder.append(this.oldHolder);
            stringBuilder.append(", newHolder=");
            stringBuilder.append(this.newHolder);
            stringBuilder.append(", fromX=");
            stringBuilder.append(this.fromX);
            stringBuilder.append(", fromY=");
            stringBuilder.append(this.fromY);
            stringBuilder.append(", toX=");
            stringBuilder.append(this.toX);
            stringBuilder.append(", toY=");
            stringBuilder.append(this.toY);
            stringBuilder.append('}');
            return stringBuilder.toString();
        }
    }

    private static class MoveInfo {
        public int fromX;
        public int fromY;
        public ViewHolder holder;
        public int toX;
        public int toY;

        MoveInfo(ViewHolder holder, int fromX, int fromY, int toX, int toY) {
            this.holder = holder;
            this.fromX = fromX;
            this.fromY = fromY;
            this.toX = toX;
            this.toY = toY;
        }
    }

    public void runPendingAnimations() {
        boolean removalsPending = this.mPendingRemovals.isEmpty() ^ 1;
        boolean movesPending = this.mPendingMoves.isEmpty() ^ 1;
        boolean changesPending = this.mPendingChanges.isEmpty() ^ 1;
        boolean additionsPending = this.mPendingAdditions.isEmpty() ^ 1;
        if (removalsPending || movesPending || additionsPending || changesPending) {
            Runnable mover;
            Iterator it = this.mPendingRemovals.iterator();
            while (it.hasNext()) {
                animateRemoveImpl((ViewHolder) it.next());
            }
            this.mPendingRemovals.clear();
            if (movesPending) {
                final ArrayList<MoveInfo> moves = new ArrayList();
                moves.addAll(this.mPendingMoves);
                this.mMovesList.add(moves);
                this.mPendingMoves.clear();
                mover = new Runnable() {
                    public void run() {
                        Iterator it = moves.iterator();
                        while (it.hasNext()) {
                            MoveInfo moveInfo = (MoveInfo) it.next();
                            DefaultItemAnimator.this.animateMoveImpl(moveInfo.holder, moveInfo.fromX, moveInfo.fromY, moveInfo.toX, moveInfo.toY);
                        }
                        moves.clear();
                        DefaultItemAnimator.this.mMovesList.remove(moves);
                    }
                };
                if (removalsPending) {
                    ((MoveInfo) moves.get(0)).holder.itemView.postOnAnimationDelayed(mover, getRemoveDuration());
                } else {
                    mover.run();
                }
            }
            if (changesPending) {
                final ArrayList<ChangeInfo> changes = new ArrayList();
                changes.addAll(this.mPendingChanges);
                this.mChangesList.add(changes);
                this.mPendingChanges.clear();
                mover = new Runnable() {
                    public void run() {
                        Iterator it = changes.iterator();
                        while (it.hasNext()) {
                            DefaultItemAnimator.this.animateChangeImpl((ChangeInfo) it.next());
                        }
                        changes.clear();
                        DefaultItemAnimator.this.mChangesList.remove(changes);
                    }
                };
                if (removalsPending) {
                    ((ChangeInfo) changes.get(0)).oldHolder.itemView.postOnAnimationDelayed(mover, getRemoveDuration());
                } else {
                    mover.run();
                }
            }
            if (additionsPending) {
                final ArrayList<ViewHolder> additions = new ArrayList();
                additions.addAll(this.mPendingAdditions);
                this.mAdditionsList.add(additions);
                this.mPendingAdditions.clear();
                mover = new Runnable() {
                    public void run() {
                        Iterator it = additions.iterator();
                        while (it.hasNext()) {
                            DefaultItemAnimator.this.animateAddImpl((ViewHolder) it.next());
                        }
                        additions.clear();
                        DefaultItemAnimator.this.mAdditionsList.remove(additions);
                    }
                };
                if (removalsPending || movesPending || changesPending) {
                    long changeDuration = 0;
                    long removeDuration = removalsPending ? getRemoveDuration() : 0;
                    long moveDuration = movesPending ? getMoveDuration() : 0;
                    if (changesPending) {
                        changeDuration = getChangeDuration();
                    }
                    ((ViewHolder) additions.get(0)).itemView.postOnAnimationDelayed(mover, Math.max(moveDuration, changeDuration) + removeDuration);
                } else {
                    mover.run();
                }
            }
        }
    }

    public boolean animateRemove(ViewHolder holder) {
        resetAnimation(holder);
        this.mPendingRemovals.add(holder);
        return true;
    }

    private void animateRemoveImpl(final ViewHolder holder) {
        final View view = holder.itemView;
        final ViewPropertyAnimator animation = view.animate();
        this.mRemoveAnimations.add(holder);
        animation.setDuration(getRemoveDuration()).alpha(0.0f).setListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animator) {
                DefaultItemAnimator.this.dispatchRemoveStarting(holder);
            }

            public void onAnimationEnd(Animator animator) {
                animation.setListener(null);
                view.setAlpha(1.0f);
                DefaultItemAnimator.this.dispatchRemoveFinished(holder);
                DefaultItemAnimator.this.mRemoveAnimations.remove(holder);
                DefaultItemAnimator.this.dispatchFinishedWhenDone();
            }
        }).start();
    }

    public boolean animateAdd(ViewHolder holder) {
        resetAnimation(holder);
        holder.itemView.setAlpha(0.0f);
        this.mPendingAdditions.add(holder);
        return true;
    }

    void animateAddImpl(final ViewHolder holder) {
        final View view = holder.itemView;
        final ViewPropertyAnimator animation = view.animate();
        this.mAddAnimations.add(holder);
        animation.alpha(1.0f).setDuration(getAddDuration()).setListener(new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animator) {
                DefaultItemAnimator.this.dispatchAddStarting(holder);
            }

            public void onAnimationCancel(Animator animator) {
                view.setAlpha(1.0f);
            }

            public void onAnimationEnd(Animator animator) {
                animation.setListener(null);
                DefaultItemAnimator.this.dispatchAddFinished(holder);
                DefaultItemAnimator.this.mAddAnimations.remove(holder);
                DefaultItemAnimator.this.dispatchFinishedWhenDone();
            }
        }).start();
    }

    public boolean animateMove(ViewHolder holder, int fromX, int fromY, int toX, int toY) {
        ViewHolder viewHolder = holder;
        View view = viewHolder.itemView;
        int fromX2 = (int) (((float) fromX) + viewHolder.itemView.getTranslationX());
        int fromY2 = (int) (((float) fromY) + viewHolder.itemView.getTranslationY());
        resetAnimation(holder);
        int deltaX = toX - fromX2;
        int deltaY = toY - fromY2;
        if (deltaX == 0 && deltaY == 0) {
            dispatchMoveFinished(holder);
            return false;
        }
        if (deltaX != 0) {
            view.setTranslationX((float) (-deltaX));
        }
        if (deltaY != 0) {
            view.setTranslationY((float) (-deltaY));
        }
        ArrayList arrayList = this.mPendingMoves;
        MoveInfo moveInfo = r0;
        MoveInfo moveInfo2 = new MoveInfo(viewHolder, fromX2, fromY2, toX, toY);
        arrayList.add(moveInfo);
        return true;
    }

    void animateMoveImpl(ViewHolder holder, int fromX, int fromY, int toX, int toY) {
        ViewHolder viewHolder = holder;
        View view = viewHolder.itemView;
        int deltaX = toX - fromX;
        int deltaY = toY - fromY;
        if (deltaX != 0) {
            view.animate().translationX(0.0f);
        }
        if (deltaY != 0) {
            view.animate().translationY(0.0f);
        }
        ViewPropertyAnimator animation = view.animate();
        this.mMoveAnimations.add(viewHolder);
        final ViewHolder viewHolder2 = viewHolder;
        final int i = deltaX;
        AnonymousClass6 anonymousClass6 = r0;
        final View view2 = view;
        ViewPropertyAnimator duration = animation.setDuration(getMoveDuration());
        final int i2 = deltaY;
        final ViewPropertyAnimator viewPropertyAnimator = animation;
        AnonymousClass6 anonymousClass62 = new AnimatorListenerAdapter() {
            public void onAnimationStart(Animator animator) {
                DefaultItemAnimator.this.dispatchMoveStarting(viewHolder2);
            }

            public void onAnimationCancel(Animator animator) {
                if (i != 0) {
                    view2.setTranslationX(0.0f);
                }
                if (i2 != 0) {
                    view2.setTranslationY(0.0f);
                }
            }

            public void onAnimationEnd(Animator animator) {
                viewPropertyAnimator.setListener(null);
                DefaultItemAnimator.this.dispatchMoveFinished(viewHolder2);
                DefaultItemAnimator.this.mMoveAnimations.remove(viewHolder2);
                DefaultItemAnimator.this.dispatchFinishedWhenDone();
            }
        };
        duration.setListener(anonymousClass6).start();
    }

    public boolean animateChange(ViewHolder oldHolder, ViewHolder newHolder, int fromX, int fromY, int toX, int toY) {
        ViewHolder viewHolder = oldHolder;
        ViewHolder viewHolder2 = newHolder;
        if (viewHolder == viewHolder2) {
            return animateMove(viewHolder, fromX, fromY, toX, toY);
        }
        float prevTranslationX = viewHolder.itemView.getTranslationX();
        float prevTranslationY = viewHolder.itemView.getTranslationY();
        float prevAlpha = viewHolder.itemView.getAlpha();
        resetAnimation(oldHolder);
        int deltaX = (int) (((float) (toX - fromX)) - prevTranslationX);
        int deltaY = (int) (((float) (toY - fromY)) - prevTranslationY);
        viewHolder.itemView.setTranslationX(prevTranslationX);
        viewHolder.itemView.setTranslationY(prevTranslationY);
        viewHolder.itemView.setAlpha(prevAlpha);
        if (viewHolder2 != null) {
            resetAnimation(viewHolder2);
            viewHolder2.itemView.setTranslationX((float) (-deltaX));
            viewHolder2.itemView.setTranslationY((float) (-deltaY));
            viewHolder2.itemView.setAlpha(0.0f);
        }
        ChangeInfo changeInfo = r7;
        ArrayList arrayList = this.mPendingChanges;
        ChangeInfo changeInfo2 = new ChangeInfo(viewHolder, viewHolder2, fromX, fromY, toX, toY);
        arrayList.add(changeInfo);
        return true;
    }

    void animateChangeImpl(final ChangeInfo changeInfo) {
        final ViewPropertyAnimator oldViewAnim;
        ViewHolder holder = changeInfo.oldHolder;
        View newView = null;
        final View view = holder == null ? null : holder.itemView;
        ViewHolder newHolder = changeInfo.newHolder;
        if (newHolder != null) {
            newView = newHolder.itemView;
        }
        if (view != null) {
            oldViewAnim = view.animate().setDuration(getChangeDuration());
            this.mChangeAnimations.add(changeInfo.oldHolder);
            oldViewAnim.translationX((float) (changeInfo.toX - changeInfo.fromX));
            oldViewAnim.translationY((float) (changeInfo.toY - changeInfo.fromY));
            oldViewAnim.alpha(0.0f).setListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animator) {
                    DefaultItemAnimator.this.dispatchChangeStarting(changeInfo.oldHolder, true);
                }

                public void onAnimationEnd(Animator animator) {
                    oldViewAnim.setListener(null);
                    view.setAlpha(1.0f);
                    view.setTranslationX(0.0f);
                    view.setTranslationY(0.0f);
                    DefaultItemAnimator.this.dispatchChangeFinished(changeInfo.oldHolder, true);
                    DefaultItemAnimator.this.mChangeAnimations.remove(changeInfo.oldHolder);
                    DefaultItemAnimator.this.dispatchFinishedWhenDone();
                }
            }).start();
        }
        if (newView != null) {
            oldViewAnim = newView.animate();
            this.mChangeAnimations.add(changeInfo.newHolder);
            oldViewAnim.translationX(0.0f).translationY(0.0f).setDuration(getChangeDuration()).alpha(1.0f).setListener(new AnimatorListenerAdapter() {
                public void onAnimationStart(Animator animator) {
                    DefaultItemAnimator.this.dispatchChangeStarting(changeInfo.newHolder, false);
                }

                public void onAnimationEnd(Animator animator) {
                    oldViewAnim.setListener(null);
                    newView.setAlpha(1.0f);
                    newView.setTranslationX(0.0f);
                    newView.setTranslationY(0.0f);
                    DefaultItemAnimator.this.dispatchChangeFinished(changeInfo.newHolder, false);
                    DefaultItemAnimator.this.mChangeAnimations.remove(changeInfo.newHolder);
                    DefaultItemAnimator.this.dispatchFinishedWhenDone();
                }
            }).start();
        }
    }

    private void endChangeAnimation(List<ChangeInfo> infoList, ViewHolder item) {
        for (int i = infoList.size() - 1; i >= 0; i--) {
            ChangeInfo changeInfo = (ChangeInfo) infoList.get(i);
            if (endChangeAnimationIfNecessary(changeInfo, item) && changeInfo.oldHolder == null && changeInfo.newHolder == null) {
                infoList.remove(changeInfo);
            }
        }
    }

    private void endChangeAnimationIfNecessary(ChangeInfo changeInfo) {
        if (changeInfo.oldHolder != null) {
            endChangeAnimationIfNecessary(changeInfo, changeInfo.oldHolder);
        }
        if (changeInfo.newHolder != null) {
            endChangeAnimationIfNecessary(changeInfo, changeInfo.newHolder);
        }
    }

    private boolean endChangeAnimationIfNecessary(ChangeInfo changeInfo, ViewHolder item) {
        boolean oldItem = false;
        if (changeInfo.newHolder == item) {
            changeInfo.newHolder = null;
        } else if (changeInfo.oldHolder != item) {
            return false;
        } else {
            changeInfo.oldHolder = null;
            oldItem = true;
        }
        item.itemView.setAlpha(1.0f);
        item.itemView.setTranslationX(0.0f);
        item.itemView.setTranslationY(0.0f);
        dispatchChangeFinished(item, oldItem);
        return true;
    }

    public void endAnimation(ViewHolder item) {
        View view = item.itemView;
        view.animate().cancel();
        int i = this.mPendingMoves.size();
        while (true) {
            i--;
            if (i < 0) {
                break;
            } else if (((MoveInfo) this.mPendingMoves.get(i)).holder == item) {
                view.setTranslationY(0.0f);
                view.setTranslationX(0.0f);
                dispatchMoveFinished(item);
                this.mPendingMoves.remove(i);
            }
        }
        endChangeAnimation(this.mPendingChanges, item);
        if (this.mPendingRemovals.remove(item)) {
            view.setAlpha(1.0f);
            dispatchRemoveFinished(item);
        }
        if (this.mPendingAdditions.remove(item)) {
            view.setAlpha(1.0f);
            dispatchAddFinished(item);
        }
        for (i = this.mChangesList.size() - 1; i >= 0; i--) {
            ArrayList<ChangeInfo> changes = (ArrayList) this.mChangesList.get(i);
            endChangeAnimation(changes, item);
            if (changes.isEmpty()) {
                this.mChangesList.remove(i);
            }
        }
        for (i = this.mMovesList.size() - 1; i >= 0; i--) {
            ArrayList<MoveInfo> moves = (ArrayList) this.mMovesList.get(i);
            int j = moves.size() - 1;
            while (j >= 0) {
                if (((MoveInfo) moves.get(j)).holder == item) {
                    view.setTranslationY(0.0f);
                    view.setTranslationX(0.0f);
                    dispatchMoveFinished(item);
                    moves.remove(j);
                    if (moves.isEmpty()) {
                        this.mMovesList.remove(i);
                    }
                } else {
                    j--;
                }
            }
        }
        for (i = this.mAdditionsList.size() - 1; i >= 0; i--) {
            ArrayList<ViewHolder> additions = (ArrayList) this.mAdditionsList.get(i);
            if (additions.remove(item)) {
                view.setAlpha(1.0f);
                dispatchAddFinished(item);
                if (additions.isEmpty()) {
                    this.mAdditionsList.remove(i);
                }
            }
        }
        this.mRemoveAnimations.remove(item);
        this.mAddAnimations.remove(item);
        this.mChangeAnimations.remove(item);
        this.mMoveAnimations.remove(item);
        dispatchFinishedWhenDone();
    }

    private void resetAnimation(ViewHolder holder) {
        if (sDefaultInterpolator == null) {
            sDefaultInterpolator = new ValueAnimator().getInterpolator();
        }
        holder.itemView.animate().setInterpolator(sDefaultInterpolator);
        endAnimation(holder);
    }

    public boolean isRunning() {
        return (this.mPendingAdditions.isEmpty() && this.mPendingChanges.isEmpty() && this.mPendingMoves.isEmpty() && this.mPendingRemovals.isEmpty() && this.mMoveAnimations.isEmpty() && this.mRemoveAnimations.isEmpty() && this.mAddAnimations.isEmpty() && this.mChangeAnimations.isEmpty() && this.mMovesList.isEmpty() && this.mAdditionsList.isEmpty() && this.mChangesList.isEmpty()) ? false : true;
    }

    void dispatchFinishedWhenDone() {
        if (!isRunning()) {
            dispatchAnimationsFinished();
        }
    }

    public void endAnimations() {
        int i;
        for (i = this.mPendingMoves.size() - 1; i >= 0; i--) {
            MoveInfo item = (MoveInfo) this.mPendingMoves.get(i);
            View view = item.holder.itemView;
            view.setTranslationY(0.0f);
            view.setTranslationX(0.0f);
            dispatchMoveFinished(item.holder);
            this.mPendingMoves.remove(i);
        }
        for (i = this.mPendingRemovals.size() - 1; i >= 0; i--) {
            dispatchRemoveFinished((ViewHolder) this.mPendingRemovals.get(i));
            this.mPendingRemovals.remove(i);
        }
        for (i = this.mPendingAdditions.size() - 1; i >= 0; i--) {
            ViewHolder item2 = (ViewHolder) this.mPendingAdditions.get(i);
            item2.itemView.setAlpha(1.0f);
            dispatchAddFinished(item2);
            this.mPendingAdditions.remove(i);
        }
        for (i = this.mPendingChanges.size() - 1; i >= 0; i--) {
            endChangeAnimationIfNecessary((ChangeInfo) this.mPendingChanges.get(i));
        }
        this.mPendingChanges.clear();
        if (isRunning()) {
            int i2;
            int i3;
            for (i2 = this.mMovesList.size() - 1; i2 >= 0; i2--) {
                ArrayList<MoveInfo> moves = (ArrayList) this.mMovesList.get(i2);
                for (int j = moves.size() - 1; j >= 0; j--) {
                    MoveInfo moveInfo = (MoveInfo) moves.get(j);
                    View view2 = moveInfo.holder.itemView;
                    view2.setTranslationY(0.0f);
                    view2.setTranslationX(0.0f);
                    dispatchMoveFinished(moveInfo.holder);
                    moves.remove(j);
                    if (moves.isEmpty()) {
                        this.mMovesList.remove(moves);
                    }
                }
            }
            for (i3 = this.mAdditionsList.size() - 1; i3 >= 0; i3--) {
                ArrayList<ViewHolder> additions = (ArrayList) this.mAdditionsList.get(i3);
                for (int j2 = additions.size() - 1; j2 >= 0; j2--) {
                    ViewHolder item3 = (ViewHolder) additions.get(j2);
                    item3.itemView.setAlpha(1.0f);
                    dispatchAddFinished(item3);
                    additions.remove(j2);
                    if (additions.isEmpty()) {
                        this.mAdditionsList.remove(additions);
                    }
                }
            }
            for (i3 = this.mChangesList.size() - 1; i3 >= 0; i3--) {
                ArrayList<ChangeInfo> changes = (ArrayList) this.mChangesList.get(i3);
                for (i2 = changes.size() - 1; i2 >= 0; i2--) {
                    endChangeAnimationIfNecessary((ChangeInfo) changes.get(i2));
                    if (changes.isEmpty()) {
                        this.mChangesList.remove(changes);
                    }
                }
            }
            cancelAll(this.mRemoveAnimations);
            cancelAll(this.mMoveAnimations);
            cancelAll(this.mAddAnimations);
            cancelAll(this.mChangeAnimations);
            dispatchAnimationsFinished();
        }
    }

    void cancelAll(List<ViewHolder> viewHolders) {
        for (int i = viewHolders.size() - 1; i >= 0; i--) {
            ((ViewHolder) viewHolders.get(i)).itemView.animate().cancel();
        }
    }

    public boolean canReuseUpdatedViewHolder(ViewHolder viewHolder, List<Object> payloads) {
        return !payloads.isEmpty() || super.canReuseUpdatedViewHolder(viewHolder, payloads);
    }
}
