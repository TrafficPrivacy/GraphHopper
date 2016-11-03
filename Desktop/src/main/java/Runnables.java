import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.PathWrapper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.PointList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

interface Trackable <T>{
    void doneCallBack(T object);
    void startCallBack(T object);
}

class PostProcessing implements Runnable {

    private Trackable<PostProcessing> mCallback;
    private final double mThreshold;
    private List<PathWrapper> mPaths;
    private double mOverlapping;

    /**
     *
     * @param threshold
     * @param paths the first element should be the original path
     */
    public PostProcessing(double threshold, List<PathWrapper> paths) {
        mThreshold = threshold;
        mPaths = paths;
    }

    public void run() {
        ArrayList<HashSet<Pair>> pathSets = new ArrayList<HashSet<Pair>>();
        mCallback.startCallBack(this);
        /** Convert every path to map **/
        for (PathWrapper path : mPaths) {
            HashSet<Pair> current = new HashSet<Pair>();
            pathSets.add(current);
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
        /** compare each segment to each generated paths **/
        int totalGenerated = pathSets.size() - 1;
        int numOverlap = 0;
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
}

class RoutingRunnable implements Runnable {
    private Trackable<RoutingRunnable> mCallback;
    private GeoPoint mStart, mEnd;
    private RoutingTest mRoutingEngine;
    private PathWrapper mOutput;
    private final long mIndex;

    public RoutingRunnable (long index, GeoPoint start, GeoPoint end, Trackable<RoutingRunnable> callBack,
                            final RoutingTest routeEngine) {
        mCallback = callBack;
        mStart = start;
        mEnd = end;
        mRoutingEngine = routeEngine;
        mIndex = index;
    }

    public void run() {
        mCallback.startCallBack(this);
        mOutput = mRoutingEngine.calcPath(mStart, mEnd);
        mCallback.doneCallBack(this);
    }

    public PathWrapper getOutput() {
        return mOutput;
    }

    public long getIndex() {
        return mIndex;
    }
}

class RoutingTest {
    private GraphHopperOSM mHopper;

    public RoutingTest(String OSMfileLocation) {
        mHopper = new GraphHopperOSM();
        mHopper.setOSMFile(OSMfileLocation);
        mHopper.forDesktop();
        mHopper.setGraphHopperLocation("/mnt/Programming/Research/Graphhopper/Desktop/data/graphhoper");
        mHopper.setEncodingManager(new EncodingManager("car"));
        mHopper.importOrLoad();
    }

    public PathWrapper calcPath(GeoPoint from, GeoPoint to) {
        GHRequest req = new GHRequest(from.getLatitude(), from.getLongitude(),
                to.getLatitude(), to.getLongitude()).
                setAlgorithm(Parameters.Algorithms.DIJKSTRA_BI);
        req.getHints().
                put(Parameters.Routing.INSTRUCTIONS, "false");
        System.out.println("start = " + from + " end = " + to);
        GHResponse resp = mHopper.route(req);
        try {
            return resp.getBest();
        } catch (Exception e) {
            return new PathWrapper();
        }
    }
}
