package android.database.sqlite;

import java.util.function.Supplier;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$RBWjWVyGrOTsQrLCYzJ_G8Uk25Q implements Supplier {
    private final /* synthetic */ SQLiteDatabase f$0;

    public /* synthetic */ -$$Lambda$RBWjWVyGrOTsQrLCYzJ_G8Uk25Q(SQLiteDatabase sQLiteDatabase) {
        this.f$0 = sQLiteDatabase;
    }

    public final Object get() {
        return this.f$0.createSession();
    }
}
