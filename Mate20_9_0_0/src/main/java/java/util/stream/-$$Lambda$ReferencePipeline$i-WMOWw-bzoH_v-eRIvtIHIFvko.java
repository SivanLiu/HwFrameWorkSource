package java.util.stream;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$ReferencePipeline$i-WMOWw-bzoH_v-eRIvtIHIFvko implements Consumer {
    private final /* synthetic */ BiConsumer f$0;
    private final /* synthetic */ Object f$1;

    public /* synthetic */ -$$Lambda$ReferencePipeline$i-WMOWw-bzoH_v-eRIvtIHIFvko(BiConsumer biConsumer, Object obj) {
        this.f$0 = biConsumer;
        this.f$1 = obj;
    }

    public final void accept(Object obj) {
        this.f$0.accept(this.f$1, obj);
    }
}
