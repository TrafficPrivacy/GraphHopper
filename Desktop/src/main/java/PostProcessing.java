import com.graphhopper.PathWrapper;
import com.graphhopper.util.PointList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by zonglin on 11/3/16.
 */
class PostProcessing implements Runnable {

    private Trackable<PostProcessing> mCallback;
    private final double mThreshold;
    private List<PathWrapper> mPaths;
    private double mOverlapping;
    private GeoPoint mStart, mEnd;
    private RoutingTest mEngine;
    private final long mIndex;

    /**
     *
     * @param threshold
     * @param paths the first element should be the original path
     */
    public PostProcessing(double threshold, List<PathWrapper> paths, Trackable<PostProcessing> callBack,
                          GeoPoint start, GeoPoint end, final RoutingTest routeEngine, long index) {
        mThreshold = threshold;
        mPaths = paths;
        mCallback = callBack;
        mStart = start;
        mEnd = end;
        mEngine = routeEngine;
        mIndex = index;
    }

    public void run() {
        mCallback.startCallBack(this);
        ArrayList<HashSet<Pair>> pathSets = new ArrayList<HashSet<Pair>>();
        /** Convert every path to set **/
        mPaths.add(0, mEngine.calcPath(mStart, mEnd));
        for (PathWrapper path : mPaths) {
            HashSet<Pair> current = new HashSet<Pair>();
            pathSets.add(current);
            if (path.hasErrors()){
                mOverlapping = 0;
                mCallback.doneCallBack(this);
                return;
            }
            PointList points = path.getPoints();
            if (points.getSize() > 1) {
                GeoPoint first = new GeoPoint(points.getLatitude(0), points.getLongitude(0));
                GeoPoint second;
                for (int i = 1; i < points.getSize(); i++) {
                    second = new GeoPoint(points.getLatitude(i), points.getLongitude(i));
                    current.add(new Pair(first, second));
                    first = second;
                }
            }
        }
        HashSet<Pair> origPathSet = pathSets.get(0);
        pathSets.remove(0);
        /** compare each segment to each generated paths **/
        int totalGenerated = pathSets.size();
        double numOverlap = 0;
        for (Pair segment : origPathSet) {
            int overlapNum = 0;
            for (HashSet<Pair> set : pathSets) {
                if (set.contains(segment))
                    overlapNum ++;
            }
            numOverlap += (overlapNum + 0.0) / totalGenerated > mThreshold ? 1 : 0;
        }
        mOverlapping = (numOverlap + 0.0) / origPathSet.size();
        mCallback.doneCallBack(this);
    }

    public double getPercentOverlapping() {
        return mOverlapping;
    }

    public long getIndex() {
        return mIndex;
    }
}