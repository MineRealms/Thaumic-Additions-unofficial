package dev.tc4port.thaumadditions.client.render;

/**
 * The four-dimensional hypercube 1.12 turned inside its entity summoner.
 *
 * <p>Twenty-four square faces, each with its four corners at {@code ±0.5} in x, y, z and w. The
 * machine's turn is applied as a rotation in the <em>yw</em> plane — a plane the third dimension
 * does not have — and the result is projected back down to three by dividing by {@code w + 1},
 * which is what makes a cube seem to turn inside out as it spins.</p>
 *
 * <p>The faces are transcribed from {@code Tesseract.java}; everything here is arithmetic, so it
 * is shared, and only the drawing lives per node.</p>
 */
public final class Tesseract {
    /** Twenty-four faces, four points each. */
    public static final int FACES = 24;

    /** One sign per corner axis, per face, per corner: {@code ±1}, halved when used. */
    private static final int[][][] FACES_BY_SIGN = {
            {{-1, -1, -1, -1}, {1, -1, -1, -1}, {1, -1, 1, -1}, {-1, -1, 1, -1}},
            {{-1, 1, -1, -1}, {1, 1, -1, -1}, {1, 1, 1, -1}, {-1, 1, 1, -1}},
            {{-1, -1, -1, -1}, {1, -1, -1, -1}, {1, 1, -1, -1}, {-1, 1, -1, -1}},
            {{-1, -1, 1, -1}, {1, -1, 1, -1}, {1, 1, 1, -1}, {-1, 1, 1, -1}},
            {{-1, -1, -1, -1}, {-1, 1, -1, -1}, {-1, 1, 1, -1}, {-1, -1, 1, -1}},
            {{1, -1, -1, -1}, {1, 1, -1, -1}, {1, 1, 1, -1}, {1, -1, 1, -1}},
            {{-1, -1, -1, 1}, {1, -1, -1, 1}, {1, -1, 1, 1}, {-1, -1, 1, 1}},
            {{-1, 1, -1, 1}, {1, 1, -1, 1}, {1, 1, 1, 1}, {-1, 1, 1, 1}},
            {{-1, -1, -1, 1}, {1, -1, -1, 1}, {1, 1, -1, 1}, {-1, 1, -1, 1}},
            {{-1, -1, 1, 1}, {1, -1, 1, 1}, {1, 1, 1, 1}, {-1, 1, 1, 1}},
            {{-1, -1, -1, 1}, {-1, 1, -1, 1}, {-1, 1, 1, 1}, {-1, -1, 1, 1}},
            {{1, -1, -1, 1}, {1, 1, -1, 1}, {1, 1, 1, 1}, {1, -1, 1, 1}},
            {{-1, -1, -1, -1}, {-1, 1, -1, -1}, {-1, 1, -1, 1}, {-1, -1, -1, 1}},
            {{-1, -1, 1, -1}, {-1, 1, 1, -1}, {-1, 1, 1, 1}, {-1, -1, 1, 1}},
            {{-1, -1, -1, -1}, {-1, 1, -1, -1}, {-1, 1, 1, -1}, {-1, -1, 1, -1}},
            {{-1, -1, -1, 1}, {-1, 1, -1, 1}, {-1, 1, 1, 1}, {-1, -1, 1, 1}},
            {{-1, -1, -1, -1}, {-1, -1, 1, -1}, {-1, -1, 1, 1}, {-1, -1, -1, 1}},
            {{-1, 1, -1, -1}, {-1, 1, 1, -1}, {-1, 1, 1, 1}, {-1, 1, -1, 1}},
            {{1, -1, -1, -1}, {1, 1, -1, -1}, {1, 1, -1, 1}, {1, -1, -1, 1}},
            {{1, -1, 1, -1}, {1, 1, 1, -1}, {1, 1, 1, 1}, {1, -1, 1, 1}},
            {{1, -1, -1, -1}, {1, 1, -1, -1}, {1, 1, 1, -1}, {1, -1, 1, -1}},
            {{1, -1, -1, 1}, {1, 1, -1, 1}, {1, 1, 1, 1}, {1, -1, 1, 1}},
            {{1, -1, -1, -1}, {1, -1, 1, -1}, {1, -1, 1, 1}, {1, -1, -1, 1}},
            {{1, 1, -1, -1}, {1, 1, 1, -1}, {1, 1, 1, 1}, {1, 1, -1, 1}},
    };

    private Tesseract() {
    }

    /**
     * The corners of every face, turned and projected: {@code [face * 4 + corner][xyz]}.
     *
     * <p>The turn is upstream's {@code rotYW} — {@code x' = cos·x + sin·w} and
     * {@code w' = cos·w − sin·x}, with y and z untouched — followed by the projection that gives a
     * hypercube its look: every point divided by its own {@code w + 1}.</p>
     */
    public static float[][] project(double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        float[][] out = new float[FACES * 4][3];
        for (int face = 0; face < FACES; face++) {
            for (int corner = 0; corner < 4; corner++) {
                int[] sign = FACES_BY_SIGN[face][corner];
                double x = sign[0] * 0.5D;
                double y = sign[1] * 0.5D;
                double z = sign[2] * 0.5D;
                double w = sign[3] * 0.5D;

                double turnedX = cos * x + sin * w;
                double turnedW = cos * w - sin * x;
                double scalar = 1.0D / (turnedW + 1.0D);

                int at = face * 4 + corner;
                out[at][0] = (float) (turnedX * scalar);
                out[at][1] = (float) (y * scalar);
                out[at][2] = (float) (z * scalar);
            }
        }
        return out;
    }
}
