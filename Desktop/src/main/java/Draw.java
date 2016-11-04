import edu.princeton.cs.algs4.StdDraw;

import java.util.ArrayList;

/**
 * Created by zonglin on 11/4/16.
 */
public class Draw {
    public static final int LINE = 0;
    public static final int HISTOGRAM = 1;

    public void draw(Drawable data[], int mode) throws Exception{
        int n = data.length;
        if (mode == HISTOGRAM) {
            for (int i = 0; i < n; i++) {
                double x = 1.0 * i / n;
                double y = data[i].getData() * 2.0;
                double rw = 0.5 / n;
                double rh = y;
                StdDraw.filledRectangle(x, y, rw, rh);
            }
        } else if (mode == LINE) {
            StdDraw.setXscale(0, n);
            StdDraw.setYscale(0, 1);
            StdDraw.setPenRadius(0.01);
            for (int i = 0; i < n; i++) {
                StdDraw.point(i, data[i].getData());
            }
        } else {
            throw new Exception("Invalid drawing mode");
        }
    }
}

interface Drawable {
    double getData();
}
