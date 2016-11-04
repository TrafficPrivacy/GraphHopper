import edu.princeton.cs.algs4.StdDraw;

import java.util.ArrayList;

/**
 * Created by zonglin on 11/4/16.
 */
public class Draw {
    public static final int LINE = 0;
    public static final int HISTOGRAM = 1;

    public void draw(ArrayList <Drawable> data, int mode) {
        if (mode == HISTOGRAM) {
            int n = data.size();
            for (int i = 0; i < n; i++) {
                double x = 1.0 * i / n;
                double y = data.get(i).getData();
                double rw = 0.5 / n;
                double rh = y;
                StdDraw.filledRectangle(x, y, rw, rh);
            }
        }
    }
}

interface Drawable {
    double getData();
}
