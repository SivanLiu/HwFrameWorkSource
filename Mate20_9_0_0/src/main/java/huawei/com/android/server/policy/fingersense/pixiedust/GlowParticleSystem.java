package huawei.com.android.server.policy.fingersense.pixiedust;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.PathMeasure;
import com.android.server.gesture.GestureNavConst;

public class GlowParticleSystem {
    static final float DISTANCE_BETWEEN_PARTICLES = 5.0f;
    GlowParticle[] particles;

    GlowParticleSystem(int n, Bitmap sprite, int maxEmit) {
        this.particles = new GlowParticle[n];
        for (int i = 0; i < n; i++) {
            this.particles[i] = new GlowParticle(sprite);
        }
    }

    void setColor(int color) {
        for (GlowParticle p : this.particles) {
            try {
                p.setColor(color);
            } catch (NullPointerException e) {
            }
        }
    }

    void update() {
        for (GlowParticle p : this.particles) {
            try {
                p.update();
            } catch (NullPointerException e) {
            }
        }
    }

    void draw(Canvas c) {
        for (GlowParticle p : this.particles) {
            try {
                p.draw(c);
            } catch (NullPointerException e) {
            }
        }
    }

    void addParticles(Path path, long timeBetweenTouches) {
        PathMeasure pmPrev;
        Path path2 = path;
        long j = timeBetweenTouches;
        PathMeasure pm = new PathMeasure(path2, false);
        PathMeasure pmPrev2 = new PathMeasure(path2, false);
        while (true) {
            pmPrev = pmPrev2;
            if (!pm.nextContour()) {
                break;
            }
            pmPrev.nextContour();
            pmPrev2 = pmPrev;
        }
        float[] pathCoordinates = new float[]{GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO};
        float contourLength = pmPrev.getLength();
        float lifetimeOffset = (float) j;
        float timeDelta = ((float) j) / ((float) Math.ceil((double) (contourLength / DISTANCE_BETWEEN_PARTICLES)));
        GlowParticle[] glowParticleArr = this.particles;
        int length = glowParticleArr.length;
        float distFromEnd = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        float lifetimeOffset2 = lifetimeOffset;
        int lifetimeOffset3 = 0;
        while (lifetimeOffset3 < length) {
            GlowParticle p = glowParticleArr[lifetimeOffset3];
            int i;
            try {
                int i2;
                if (!p.isDead() || distFromEnd > contourLength) {
                    i = 0;
                    lifetimeOffset3++;
                    i2 = i;
                    path2 = path;
                    j = timeBetweenTouches;
                } else {
                    pmPrev.getPosTan(distFromEnd, pathCoordinates, null);
                    i = 0;
                    try {
                        p.rebirth((double) pathCoordinates[0], (double) pathCoordinates[1], lifetimeOffset2);
                        distFromEnd += DISTANCE_BETWEEN_PARTICLES;
                        lifetimeOffset2 -= timeDelta;
                    } catch (NullPointerException e) {
                    }
                    lifetimeOffset3++;
                    i2 = i;
                    path2 = path;
                    j = timeBetweenTouches;
                }
            } catch (NullPointerException e2) {
                i = 0;
            }
        }
    }
}
