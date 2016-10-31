import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.PathWrapper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.search.Geocoding;
import com.graphhopper.util.Parameters.*;
import com.graphhopper.util.PointList;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.lang.System.exit;
import static java.lang.System.in;

public class Main {

    private static final double RADIUS = 0.0007;
    private static final int NUM_OF_DOTS = 100;
    private static Object lock;

    private GeoPoint dot_generator(GeoPoint origin, double maxradii) {
        double radii = Math.random() * maxradii;
        double angle = Math.random() * Math.PI * 2;
        GeoPoint result = new GeoPoint(origin.getLatitude() + radii * Math.sin(angle), origin.getLongitude() + radii * Math.cos(angle) / 2);
        return result;
    }

    public static void main(String args[]) {
        CSVParser parser;
        File csvFile = new File(args[1]);
        System.out.println(csvFile);
        try {
            parser = CSVParser.parse(csvFile, StandardCharsets.US_ASCII, CSVFormat.EXCEL);
            for (CSVRecord record : parser) {
                if (record.size() > 5)
                    System.out.println(record.getRecordNumber() + "\t" + record.get(5) + "\t" + record.get(6) + "\t" + record.get(7) + "\t" + record.get(8));
                if (record.getRecordNumber() > 100) {
                    exit(0);
                }
            }
        } catch (Exception e) {
            System.err.println(e);
            exit(1);
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
    private PathWrapper mPath;
    private List<PathWrapper> mOtherPaths;

    public PostProcessing(double threshold, PathWrapper origPath, List<PathWrapper> otherPaths) {
        mThreshold = threshold;
        mPath = origPath;
        mOtherPaths = otherPaths;
    }

    public void run() {

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

    public long getLatitudeE8() {
        return (long) (mLatitude * 1000000);
    }

    public long getLongitudeE8() {
        return (long) (mLongitude * 1000000);
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
        GHResponse resp = mHopper.route(req);
        return resp.getBest();
    }
}
