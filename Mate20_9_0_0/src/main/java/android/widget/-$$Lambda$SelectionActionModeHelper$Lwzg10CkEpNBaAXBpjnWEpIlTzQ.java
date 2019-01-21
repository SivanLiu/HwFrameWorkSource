package android.widget;

import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SelectionActionModeHelper$Lwzg10CkEpNBaAXBpjnWEpIlTzQ implements Consumer {
    private final /* synthetic */ SelectionActionModeHelper f$0;

    public /* synthetic */ -$$Lambda$SelectionActionModeHelper$Lwzg10CkEpNBaAXBpjnWEpIlTzQ(SelectionActionModeHelper selectionActionModeHelper) {
        this.f$0 = selectionActionModeHelper;
    }

    public final void accept(Object obj) {
        this.f$0.invalidateActionMode((SelectionResult) obj);
    }
}
