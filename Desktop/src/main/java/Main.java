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
import java.time.Clock;
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
            long init_time = System.currentTimeMillis();
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
                    long time_1 = System.currentTimeMillis();
                    for (int i = 0; i < 100; i++) {
                        RoutingRunnable routingTest = new RoutingRunnable(record.getRecordNumber(), dot_generator(start, RADIUS), dot_generator(end, RADIUS),
                                new Trackable<RoutingRunnable>() {
                                    public void doneCallBack(RoutingRunnable object) {
                                        synchronized (lock) {
                                            result.add(object.getOutput());
                                            mSemaphor ++;
                                        }
                                    }

                                    public void startCallBack(RoutingRunnable object) {
                                    }
                                }, engine);
                        new Thread(routingTest).start();
                    }
                    while (true) {
                        Thread.sleep(50);
                        synchronized (lock) {
                            if (mSemaphor == 100) break;
                        }
                    }
                    long time_2 = System.currentTimeMillis();
                    System.out.println("result size = " + result.size());
                    System.out.println(time_1 - init_time);
                    System.out.println(time_2 - init_time);
                    System.out.println(time_2 - time_1);
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
