package com.android.server.wm;

import android.graphics.Matrix;
import android.graphics.Rect;
import android.view.Surface.OutOfResourcesException;
import android.view.SurfaceControl;
import android.view.SurfaceControl.Transaction;
import java.io.PrintWriter;

public class BlackFrame {
    final BlackSurface[] mBlackSurfaces = new BlackSurface[4];
    final boolean mForceDefaultOrientation;
    final Rect mInnerRect;
    final Rect mOuterRect;
    final float[] mTmpFloats = new float[9];
    final Matrix mTmpMatrix = new Matrix();

    class BlackSurface {
        final int layer;
        final int left;
        final SurfaceControl surface;
        final int top;

        BlackSurface(Transaction transaction, int layer, int l, int t, int r, int b, DisplayContent dc) throws OutOfResourcesException {
            this.left = l;
            this.top = t;
            this.layer = layer;
            this.surface = dc.makeOverlay().setName("BlackSurface").setSize(r - l, b - t).setColorLayer(true).setParent(null).build();
            transaction.setAlpha(this.surface, 1.0f);
            transaction.setLayer(this.surface, layer);
            transaction.show(this.surface);
        }

        void setAlpha(Transaction t, float alpha) {
            t.setAlpha(this.surface, alpha);
        }

        void setMatrix(Transaction t, Matrix matrix) {
            BlackFrame.this.mTmpMatrix.setTranslate((float) this.left, (float) this.top);
            BlackFrame.this.mTmpMatrix.postConcat(matrix);
            BlackFrame.this.mTmpMatrix.getValues(BlackFrame.this.mTmpFloats);
            t.setPosition(this.surface, BlackFrame.this.mTmpFloats[2], BlackFrame.this.mTmpFloats[5]);
            t.setMatrix(this.surface, BlackFrame.this.mTmpFloats[0], BlackFrame.this.mTmpFloats[3], BlackFrame.this.mTmpFloats[1], BlackFrame.this.mTmpFloats[4]);
        }

        void clearMatrix(Transaction t) {
            t.setMatrix(this.surface, 1.0f, 0.0f, 0.0f, 1.0f);
        }
    }

    public void printTo(String prefix, PrintWriter pw) {
        pw.print(prefix);
        pw.print("Outer: ");
        this.mOuterRect.printShortString(pw);
        pw.print(" / Inner: ");
        this.mInnerRect.printShortString(pw);
        pw.println();
        for (int i = 0; i < this.mBlackSurfaces.length; i++) {
            BlackSurface bs = this.mBlackSurfaces[i];
            pw.print(prefix);
            pw.print("#");
            pw.print(i);
            pw.print(": ");
            pw.print(bs.surface);
            pw.print(" left=");
            pw.print(bs.left);
            pw.print(" top=");
            pw.println(bs.top);
        }
    }

    public BlackFrame(Transaction t, Rect outer, Rect inner, int layer, DisplayContent dc, boolean forceDefaultOrientation) throws OutOfResourcesException {
        Rect rect = outer;
        Rect rect2 = inner;
        boolean success = false;
        this.mForceDefaultOrientation = forceDefaultOrientation;
        this.mOuterRect = new Rect(rect);
        boolean rect3 = new Rect(rect2);
        this.mInnerRect = rect3;
        try {
            if (rect.top < rect2.top) {
                this.mBlackSurfaces[0] = new BlackSurface(t, layer, rect.left, rect.top, rect2.right, rect2.top, dc);
            }
            if (rect.left < rect2.left) {
                this.mBlackSurfaces[1] = new BlackSurface(t, layer, rect.left, rect2.top, rect2.left, rect.bottom, dc);
            }
            if (rect.bottom > rect2.bottom) {
                this.mBlackSurfaces[2] = new BlackSurface(t, layer, rect2.left, rect2.bottom, rect.right, rect.bottom, dc);
            }
            if (rect.right > rect2.right) {
                this.mBlackSurfaces[3] = new BlackSurface(t, layer, rect2.right, rect.top, rect.right, rect2.bottom, dc);
            }
            rect3 = true;
        } finally {
            if (!(
/*
Method generation error in method: com.android.server.wm.BlackFrame.<init>(android.view.SurfaceControl$Transaction, android.graphics.Rect, android.graphics.Rect, int, com.android.server.wm.DisplayContent, boolean):void, dex: 
jadx.core.utils.exceptions.CodegenException: Error generate insn: ?: MERGE  (r13_1 'success' boolean) = (r13_0 'success' boolean), (r0_16 'rect3' boolean) in method: com.android.server.wm.BlackFrame.<init>(android.view.SurfaceControl$Transaction, android.graphics.Rect, android.graphics.Rect, int, com.android.server.wm.DisplayContent, boolean):void, dex: 
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:228)
	at jadx.core.codegen.InsnGen.addArg(InsnGen.java:101)
	at jadx.core.codegen.ConditionGen.wrap(ConditionGen.java:94)
	at jadx.core.codegen.ConditionGen.addCompare(ConditionGen.java:116)
	at jadx.core.codegen.ConditionGen.add(ConditionGen.java:56)
	at jadx.core.codegen.ConditionGen.wrap(ConditionGen.java:83)
	at jadx.core.codegen.ConditionGen.addNot(ConditionGen.java:143)
	at jadx.core.codegen.ConditionGen.add(ConditionGen.java:64)
	at jadx.core.codegen.ConditionGen.add(ConditionGen.java:45)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:116)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:57)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:93)
	at jadx.core.codegen.RegionGen.makeTryCatch(RegionGen.java:298)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:173)
	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:321)
	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:259)
	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:221)
	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:111)
	at jadx.core.codegen.ClassGen.makeClass(ClassGen.java:77)
	at jadx.core.codegen.CodeGen.visit(CodeGen.java:10)
	at jadx.core.ProcessClass.process(ProcessClass.java:38)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
Caused by: jadx.core.utils.exceptions.CodegenException: MERGE can be used only in fallback mode
	at jadx.core.codegen.InsnGen.fallbackOnlyInsn(InsnGen.java:539)
	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:511)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:213)
	... 28 more

*/

    public void kill() {
        if (this.mBlackSurfaces != null) {
            for (int i = 0; i < this.mBlackSurfaces.length; i++) {
                if (this.mBlackSurfaces[i] != null) {
                    this.mBlackSurfaces[i].surface.destroy();
                    this.mBlackSurfaces[i] = null;
                }
            }
        }
    }

    public void hide(Transaction t) {
        if (this.mBlackSurfaces != null) {
            for (int i = 0; i < this.mBlackSurfaces.length; i++) {
                if (this.mBlackSurfaces[i] != null) {
                    t.hide(this.mBlackSurfaces[i].surface);
                }
            }
        }
    }

    public void setAlpha(Transaction t, float alpha) {
        for (int i = 0; i < this.mBlackSurfaces.length; i++) {
            if (this.mBlackSurfaces[i] != null) {
                this.mBlackSurfaces[i].setAlpha(t, alpha);
            }
        }
    }

    public void setMatrix(Transaction t, Matrix matrix) {
        for (int i = 0; i < this.mBlackSurfaces.length; i++) {
            if (this.mBlackSurfaces[i] != null) {
                this.mBlackSurfaces[i].setMatrix(t, matrix);
            }
        }
    }

    public void clearMatrix(Transaction t) {
        for (int i = 0; i < this.mBlackSurfaces.length; i++) {
            if (this.mBlackSurfaces[i] != null) {
                this.mBlackSurfaces[i].clearMatrix(t);
            }
        }
    }
}
