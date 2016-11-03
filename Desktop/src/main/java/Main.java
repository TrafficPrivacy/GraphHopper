import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.PathWrapper;
import com.graphhopper.json.geo.Point;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.DouglasPeucker;
import com.graphhopper.util.Parameters.*;
import com.graphhopper.util.PointList;
import com.sun.org.apache.xalan.internal.xsltc.cmdline.getopt.GetOptsException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static java.lang.System.exit;

public class Main {

    private static final double RADIUS = 0.0007;
    private static final int NUM_OF_DOTS = 100;
    private static Object lock;
    private static ArrayList<PathWrapper> result;
    public  static int mSemaphor = 0;

    private static GeoPoint dot_generator(GeoPoint origin, double maxradii) {
        double radii = Math.random() * maxradii;
        double angle = Math.random() * Math.PI * 2;
        GeoPoint result = new GeoPoint(origin.getLatitude() + radii * Math.sin(angle), origin.getLongitude() + radii * Math.cos(angle) / 2);
        return result;
    }

    public static void main(String args[]) {
        final CSVParser parser;
        File csvFile = new File(args[1]);
        System.out.println(csvFile);
        System.out.println(args[0]);
        lock = new Object();
        RoutingTest engine = new RoutingTest(args[0]);
        try {
            parser = CSVParser.parse(csvFile, StandardCharsets.US_ASCII, CSVFormat.EXCEL);
            for (final CSVRecord record : parser) {
                if (record.size() > 5 && !record.get(5).contains("p")) {
                    result = new ArrayList<PathWrapper>();
                    GeoPoint start = new GeoPoint(Double.parseDouble(record.get(6)), Double.parseDouble(record.get(5)));
                    GeoPoint end   = new GeoPoint(Double.parseDouble(record.get(8)), Double.parseDouble(record.get(7)));
                    System.out.println("===========================================================");
                    RoutingRunnable testRouting = new RoutingRunnable(record.getRecordNumber(), start, end,
                            new Trackable<RoutingRunnable>() {
                                public void doneCallBack(RoutingRunnable object) {
                                    result.add(object.getOutput());
                                    synchronized (lock) {
                                        mSemaphor --;
                                    }
                                }

                                public void startCallBack(RoutingRunnable object) {
                                    synchronized (lock) {
                                        mSemaphor ++;
                                    }
                                }
                            }, engine);
                    new Thread(testRouting).start();
                    Thread.sleep(100);
                    while (true) {
                        synchronized (lock) {
                            if (mSemaphor == 0) break;
                        }
                    }
                    System.out.println(result.size());
                    for (int i = 0; i < 100; i++) {
                        RoutingRunnable routingTest = new RoutingRunnable(record.getRecordNumber(), dot_generator(start, RADIUS), dot_generator(end, RADIUS),
                                new Trackable<RoutingRunnable>() {
                                    public void doneCallBack(RoutingRunnable object) {
                                        result.add(object.getOutput());
                                        synchronized (lock) {
                                            mSemaphor --;
                                        }
                                    }

                                    public void startCallBack(RoutingRunnable object) {
                                        synchronized (lock) {
                                            mSemaphor ++;
                                        }
                                    }
                                }, engine);
                        new Thread(routingTest).start();
                    }
                    Thread.sleep(1000);
                    while (true) {
                        synchronized (lock) {
                            if (mSemaphor == 0) break;
                        }
                    }
                    System.out.println(result.size());
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println(e);
            exit(1);
        }
//
//        /** test **/
//        RoutingTest rt = new RoutingTest(args[0]);
//        System.out.println("osm file: " + args[0]);
//        System.out.println(rt.calcPath(new GeoPoint(40.6951789855957, -73.93058013916016), new GeoPoint(40.72904586791992, -74.00005340576172)));
    }

    private static class ResultHoler {
        protected final int mIndex;
        protected final double mOverlapping;

        public ResultHoler(int mIndex, double mOverlapping) {
            this.mIndex = mIndex;
            this.mOverlapping = mOverlapping;
        }
    }
}

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

class GeoPoint {
    private final double mLatitude;
    private final double mLongitude;

    public GeoPoint(final double latitude, final double longitude) {
        mLatitude = latitude;
        mLongitude = longitude;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public int getLatitudeE6() {
        return (int) (mLatitude * 10000);
    }

    public int getLongitudeE6() {
        return (int) (mLongitude * 10000);
    }

    public String toString() {
        String result = "(";
        result += mLatitude + ", " + mLongitude + ")";
        return result;
    }
}

class Pair {
    public GeoPoint mDota, mDotb;
    public Pair(GeoPoint dota, GeoPoint dotb) {
        mDota = dota;
        mDotb = dotb;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pair)) return false;
        Pair other = (Pair) o;
        return other.mDota.equals(mDota) && other.mDotb.equals(mDotb);
    }

    @Override
    public int hashCode() {
        int result = (mDota.getLatitudeE6() + mDota.getLongitudeE6()) / 37 +
                (mDotb.getLatitudeE6() + mDotb.getLongitudeE6()) / 31;
        return result;
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
                setAlgorithm(Algorithms.DIJKSTRA_BI);
        req.getHints().
                put(Routing.INSTRUCTIONS, "false");
        System.out.println("start = " + from + " end = " + to);
        GHResponse resp = mHopper.route(req);
        try {
            return resp.getBest();
        } catch (Exception e) {
            return new PathWrapper();
        }
    }
}
