import com.graphhopper.PathWrapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static java.lang.System.exit;

public class Main {

    private static final double RADIUS = 0.007;
    private static final int NUM_OF_DOTS = 100;
    private static final double THRESHOLD = 0.3;
    private static Object lock;
    private static Object ppLock;
    private static ArrayList<PathWrapper> result;
    private static ArrayList<ResultHolder> mResult;
    private static int mSemaphor = 0;
    private static int mPPSemaphor = 0;
    private static Counter[] mData;


    private static GeoPoint dot_generator(GeoPoint origin, double maxradii) {
        double radii = Math.random() * maxradii;
        double angle = Math.random() * Math.PI * 2;
        GeoPoint result = new GeoPoint(origin.getLatitude() + radii * Math.sin(angle),
                                origin.getLongitude() + radii * Math.cos(angle) / 2);
        return result;
    }

    public static void main(String args[]) throws Exception{
        if (args.length != 3){
            throw new Exception("Arguments not enough. Requires 3");
        }
        final CSVParser parser;
        File csvFile = new File(args[1]);
        System.out.println(csvFile);
        System.out.println(args[0]);
        lock = new Object();
        ppLock = new Object();
        RoutingTest engine = new RoutingTest(args[0]);
        int counter = 0;
        int totalNumber = Integer.parseInt(args[2]);
        mResult = new ArrayList<ResultHolder>();
        mData = new Counter[100];
        for (int i = 0; i < 100; i++) {
            mData[i] = new Counter(totalNumber);
        }
        try {
            long stopwatch = System.currentTimeMillis();
            parser = CSVParser.parse(csvFile, StandardCharsets.US_ASCII, CSVFormat.EXCEL);
            for (final CSVRecord record : parser) {
                mSemaphor = 0;
                if (record.size() > 5 && !record.get(5).contains("p")) {
                    counter ++;
                    result = new ArrayList<PathWrapper>();
                    GeoPoint start = new GeoPoint(Double.parseDouble(record.get(6)), Double.parseDouble(record.get(5)));
                    GeoPoint end   = new GeoPoint(Double.parseDouble(record.get(8)), Double.parseDouble(record.get(7)));
                    for (int i = 0; i < NUM_OF_DOTS; i++) {
                        RoutingRunnable routingTest = new RoutingRunnable(record.getRecordNumber(),
                                dot_generator(start, RADIUS),
                                dot_generator(end, RADIUS),
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
                        synchronized (lock) {
                            if (mSemaphor == NUM_OF_DOTS) break;
                        }
                    }
                    PostProcessing pp = new PostProcessing(THRESHOLD, result, new Trackable<PostProcessing>() {
                        public void doneCallBack(PostProcessing object) {
                            double p = object.getPercentOverlapping();
//                            ResultHolder rh = new ResultHolder(object.getIndex(), p);
                            mData[(int)(p * 100)].increment();
                            synchronized (ppLock) {
//                                mResult.add(rh);
                                mPPSemaphor ++;
                            }
                        }

                        public void startCallBack(PostProcessing object) {
                        }
                    }, start, end, engine, record.getRecordNumber());
                    new Thread(pp).start();
                    if (counter % 100 == 0) {
                        long temp = System.currentTimeMillis();
                        System.out.println(counter + "\t\t" + (temp - stopwatch) + " ms");
                        stopwatch = temp;
                        if (counter == totalNumber) break;
                    }
                }
            }
            while(true) {
                Thread.sleep(50);
                synchronized (ppLock) {
                    if (mPPSemaphor == totalNumber) break;
                }
            }
//            Collections.sort(mResult, new Comparator<ResultHolder>() {
//                public int compare(ResultHolder o1, ResultHolder o2) {
//                    return o1.compareTo(o2);
//                }
//            });

            new Draw().draw(mData, Draw.HISTOGRAM);
            for (int i = 1; i < mData.length; i++) {
                mData[i].setData(mData[i].getUnweightedData() + mData[i - 1].getUnweightedData());
                System.out.println(mData[i]);
            }
            new Draw().draw(mData, Draw.LINE);

        } catch (Exception e) {
            System.err.println(e);
            exit(1);
        }
    }

    private static class Counter implements Drawable {
        private double mData;
        private double mTotal;

        public Counter(double total) {
            mTotal = total;
            mData = 0.0;
        }

        public void increment() {
            mData ++;
        }

        public void setData(double data) {
            mData = data;
        }

        public double getData() {
            return mData / mTotal;
        }

        public double getUnweightedData() {
            return mData;
        }

        @Override
        public String toString() {
            return "Counter{" +
                    "mData=" + mData +
                    ", mTotal=" + mTotal +
                    '}';
        }
    }

    private static class ResultHolder implements Comparable<ResultHolder>{
        protected final long mIndex;
        protected final double mOverlapping;

        public ResultHolder(long mIndex, double mOverlapping) {
            this.mIndex = mIndex;
            this.mOverlapping = mOverlapping;
        }

        public int compareTo(ResultHolder other) {
            if (other.mOverlapping == mOverlapping)
                return 0;
            return mOverlapping > other.mOverlapping ? 1 : -1;
        }
    }
}
