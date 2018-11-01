package tutorial3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adapted from Boojum's Python noise function
 * (https://gamedev.stackexchange.com/questions/23625/how-do-you-generate-tileable-perlin-noise)
 */

public class Noise {

    private static final int MAX_PERM = 256;

    private int[] perm;
    private double[][] dirs;

    public Noise() {
        List<Integer> permList = new ArrayList<>();
        for(int i=0; i<MAX_PERM; ++i) {
            permList.add(i);
        }
        Collections.shuffle(permList);

        this.perm = new int[MAX_PERM * 2];
        for(int i=0; i<MAX_PERM; ++i) {
            perm[i] = permList.get(i);
            perm[i + MAX_PERM] = permList.get(i);
        }


        this.dirs = new double[MAX_PERM][2];
        for(int i=0; i<MAX_PERM; ++i) {
            dirs[i][0] = Math.cos(i * 2.0 * Math.PI / ((float)MAX_PERM));
            dirs[i][1] = Math.sin(i * 2.0 * Math.PI / ((float)MAX_PERM));
        }
    }

    public float noise(float x, float y, int per) {
        int intX = (int)x;
        int intY = (int)y;
        return (float)(
                surflet(x, y, intX, intY, per) +
                        surflet(x, y, intX + 1, intY, per) +
                        surflet(x, y, intX, intY + 1, per) +
                        surflet(x, y, intX + 1, intY + 1, per));
    }

    private double surflet(float x, float y, int gridX, int gridY, int per) {
        double distX = Math.abs(x - gridX);
        double distY = Math.abs(y - gridY);
        double polyX = 1 - 6 * Math.pow(distX, 5) + 15 * Math.pow(distX, 4) - 10 * Math.pow(distX, 3);
        double polyY = 1 - 6 * Math.pow(distY, 5) + 15 * Math.pow(distY, 4) - 10 * Math.pow(distY, 3);
        int hashed = perm[perm[gridX % per] + gridY % per];
        double grad = (x - gridX) * dirs[hashed][0] + (y - gridY) * dirs[hashed][1];
        return polyX * polyY * grad;
    }
}