package com.android.server.wm;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TaskPositioningController$WvS6bGwsoNKniWwQXf4LtUhPblY implements Runnable {
    private final /* synthetic */ TaskPositioningController f$0;
    private final /* synthetic */ int f$1;
    private final /* synthetic */ int f$2;
    private final /* synthetic */ DisplayContent f$3;

    public /* synthetic */ -$$Lambda$TaskPositioningController$WvS6bGwsoNKniWwQXf4LtUhPblY(TaskPositioningController taskPositioningController, int i, int i2, DisplayContent displayContent) {
        this.f$0 = taskPositioningController;
        this.f$1 = i;
        this.f$2 = i2;
        this.f$3 = displayContent;
    }

    public final void run() {
        TaskPositioningController.lambda$handleTapOutsideTask$0(this.f$0, this.f$1, this.f$2, this.f$3);
    }
}
