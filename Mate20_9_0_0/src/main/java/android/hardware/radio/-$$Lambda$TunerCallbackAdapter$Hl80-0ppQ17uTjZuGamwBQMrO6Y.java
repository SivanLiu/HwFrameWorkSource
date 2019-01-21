package android.hardware.radio;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TunerCallbackAdapter$Hl80-0ppQ17uTjZuGamwBQMrO6Y implements OnCloseListener {
    private final /* synthetic */ TunerCallbackAdapter f$0;
    private final /* synthetic */ ProgramList f$1;
    private final /* synthetic */ OnCloseListener f$2;

    public /* synthetic */ -$$Lambda$TunerCallbackAdapter$Hl80-0ppQ17uTjZuGamwBQMrO6Y(TunerCallbackAdapter tunerCallbackAdapter, ProgramList programList, OnCloseListener onCloseListener) {
        this.f$0 = tunerCallbackAdapter;
        this.f$1 = programList;
        this.f$2 = onCloseListener;
    }

    public final void onClose() {
        TunerCallbackAdapter.lambda$setProgramListObserver$0(this.f$0, this.f$1, this.f$2);
    }
}
