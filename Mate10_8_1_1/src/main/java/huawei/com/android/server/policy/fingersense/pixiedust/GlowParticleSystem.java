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
        PathMeasure pm = new PathMeasure(path, false);
        PathMeasure pmPrev = new PathMeasure(path, false);
        while (pm.nextContour()) {
            pmPrev.nextContour();
        }
        float[] pathCoordinates = new float[]{GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO, GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO};
        float contourLength = pmPrev.getLength();
        float distFromEnd = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
        float lifetimeOffset = (float) timeBetweenTouches;
        float timeDelta = ((float) timeBetweenTouches) / ((float) Math.ceil((double) (contourLength / DISTANCE_BETWEEN_PARTICLES)));
        for (GlowParticle p : this.particles) {
            try {
                if (p.isDead() && distFromEnd <= contourLength) {
                    pmPrev.getPosTan(distFromEnd, pathCoordinates, null);
                    p.rebirth((double) pathCoordinates[0], (double) pathCoordinates[1], lifetimeOffset);
                    distFromEnd += DISTANCE_BETWEEN_PARTICLES;
                    lifetimeOffset -= timeDelta;
                }
            } catch (NullPointerException e) {
            }
        }
    }
}
