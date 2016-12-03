import com.graphhopper.PathWrapper;
import com.graphhopper.geohash.SpatialKeyAlgo;
import com.graphhopper.util.shapes.GHPoint;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.procedure.TIntProcedure;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.lang.reflect.Array;
//import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import java.util.*;

import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.routing.util.DefaultEdgeFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.*;
import com.graphhopper.routing.weighting.*;
import com.graphhopper.routing.util.*;
import com.graphhopper.storage.*;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.util.BreadthFirstSearch;
import com.graphhopper.coll.GHBitSet;
import com.graphhopper.coll.GHTBitSet;
import com.graphhopper.util.*;
import com.graphhopper.routing.template.*;


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

    //System.out.println("findClosest LocationIndexTree");
    /*
    private LocationIndex createLocationIndex(Directory dir) {
        int preciseIndexResolution = 300;
        int maxRegionSearch = 4;

        GraphHopperStorage ghStorage;
        dir = ghStorage.getDirectory();
        LocationIndexTree tmpIndex = new LocationIndexTree(ghStorage, dir);
        tmpIndex.setResolution(preciseIndexResolution);
        tmpIndex.setMaxRegionSearch(maxRegionSearch);

        return tmpIndex;
    }
    */



    public static void main(String args[]) throws Exception{
        if (args.length != 3){
            throw new Exception("Arguments not enough. Requires 3");
        }

        System.out.println("Going to do BFS Instead");

        String OSMfileLocation = "new-york-latest.osm.pbf";

        GraphHopperOSM mHopper;
        mHopper = new GraphHopperOSM();

        mHopper.setOSMFile(OSMfileLocation);
        mHopper.forDesktop();
        mHopper.setGraphHopperLocation("/Users/Edward_Chou/Downloads/gh-Zonglin/data");
        mHopper.setEncodingManager(new EncodingManager("car"));
        mHopper.importOrLoad(); //important one

        LocationIndex locationIndex = mHopper.getLocationIndex();
        EncodingManager encodingManager = mHopper.getEncodingManager();

        String vehicle = encodingManager.fetchEdgeEncoders().get(0).toString();
        FlagEncoder encoder = encodingManager.getEncoder(vehicle);

        EdgeFilter edgeFilter = new DefaultEdgeFilter(encoder);

        QueryResult startQR = locationIndex.findClosest(40.734695434570313, -73.990371704101563, edgeFilter);

        GraphHopperStorage ghStorage = mHopper.getGraphHopperStorage();
        Directory dir = ghStorage.getDirectory();


        int preciseIndexResolution = 300;
        int maxRegionSearch = 4;

        BFSTest engine = new BFSTest(ghStorage, dir);
        engine.setResolution(preciseIndexResolution);
        engine.setMaxRegionSearch(maxRegionSearch);
        engine.prepareAlgo();


        final double queryLat = 40.734695434570313;
        final double queryLon = -73.990371704101563;

        List<GHPoint> circum_points = engine.BFSCircum(40.734695434570313, -73.990371704101563, edgeFilter);
        List<GHPoint> area_points = engine.BFSArea(40.734695434570313, -73.990371704101563, edgeFilter);
        System.out.println("Circum points: " + circum_points.size());
        System.out.println("Area points: " + area_points.size());

        /*
        public static final String DIJKSTRA_BI = "dijkstrabi";
        public static final String DIJKSTRA = "dijkstra";
        public static final String DIJKSTRA_ONE_TO_MANY = "dijkstra_one_to_many";
        */
        String algoStr = "dijkstrabi";
        TraversalMode tMode = TraversalMode.EDGE_BASED_2DIR; //TraversalMode.NODE_BASED;


        QueryGraph queryGraph = new QueryGraph(ghStorage);
        RoutingTemplate routingTemplate;
        Weighting weighting = new ShortestWeighting(encoder);

        GHPoint startpoint = new GHPoint(40.734695434570313, -73.990371704101563);
        GHPoint endpoint = new GHPoint(40.735695434570313, -73.995371704101563);
        List<GHPoint> points = Arrays.asList(startpoint, endpoint);
        List<QueryResult> qResults = lookup(points, encoder, locationIndex);
        queryGraph.lookup(qResults);

        RoutingAlgorithm onetomany_algo = new DijkstraOneToMany(queryGraph, encoder, weighting, tMode);
        RoutingAlgorithm bidir_algo = new DijkstraBidirectionRef(queryGraph, encoder, weighting, tMode);
        List<Path> tmpPathList;

        List<Path> onetomany_list;

        for(int k = 0; k < circum_points.size(); k++) {
            //GHPoint endpoint = new GHPoint(40.735695434570313, -73.995371704101563);
            endpoint = circum_points.get(k);

            //List<GHPoint> points = new ArrayList<GHPoint>(2);
            //points[0] = startpoint;
            points = Arrays.asList(startpoint, endpoint);
            qResults = lookup(points, encoder, locationIndex);
            //weighting = createTurnWeighting(queryGraph, encoder, weighting, tMode);
            //QueryResult res = locationIndex.findClosest(point.lat, point.lon, edgeFilter);
            QueryResult fromQResult = qResults.get(0);
            QueryResult toQResult = qResults.get(1);

            bidir_algo = new DijkstraBidirectionRef(queryGraph, encoder, weighting, tMode);

            if(k == 1) {
                //onetomany_list = onetomany_algo.calcPaths(fromQResult.getClosestNode(), toQResult.getClosestNode());
            }

            tmpPathList = bidir_algo.calcPaths(fromQResult.getClosestNode(), toQResult.getClosestNode());
            //System.out.println("HELLO");
            if (tmpPathList.isEmpty())
                throw new IllegalStateException("At least one path has to be returned for " + fromQResult + " -> " + toQResult);
            else {
                System.out.println("Not empty!");
            }
            for (int i = 0; i < tmpPathList.size(); i++) {
                Path p = tmpPathList.get(i);
                System.out.println(p.toDetailsString());
                PointList pl = p.calcPoints();
                for (int j = 0; j < pl.getSize(); j++) {
                    double c_lat = pl.getLat(j);
                    double c_lon = pl.getLon(j);
                    //System.out.println("Lat: " + c_lat + ", Lon: " + c_lon);
                }
            }
        }



        //altPaths = routingTemplate.calcPaths(queryGraph, tmpAlgoFactory, algoOpts);
        //List<Path> calcPaths(QueryGraph queryGraph, RoutingAlgorithmFactory algoFactory, AlgorithmOptions algoOpts);



    /*
        AlgorithmOptions algoOpts = AlgorithmOptions.start().
                algorithm(algoStr).traversalMode(tMode).flagEncoder(encoder).weighting(weighting).
                maxVisitedNodes(maxVisitedNodesForRequest).
                hints(hints).
                build();
    */



        //EdgeExplorer explorer = graph.createEdgeExplorer();

        ////////////////////////////////////////////




        //QueryResult startQR = locationIndex.findClosest(start.lat, start.lon, edgeFilter);


        //GeoPoint start = new GeoPoint(40.734695434570313, -73.990371704101563);

        /*
        final CSVParser parser;
        File csvFile = new File(args[1]);
        System.out.println(csvFile);
        System.out.println(args[0]);
        lock = new Object();
        ppLock = new Object();
        RoutingTest engine = new RoutingTest(args[0]);
        int counter = 0;
        System.out.println(args[2]);
        int totalNumber = Integer.parseInt(args[2]);
        mResult = new ArrayList<ResultHolder>();
        mData = new Counter[100];
        for (int i = 0; i < 100; i++) {
            mData[i] = new Counter(totalNumber);
        }
//        try {
            long stopwatch = System.currentTimeMillis();
            parser = CSVParser.parse(csvFile, StandardCharsets.US_ASCII, CSVFormat.EXCEL);
            for (final CSVRecord record : parser) {
                mSemaphor = 0;
                if (record.size() > 5 && !record.get(5).contains("p")) {
                    counter ++;
                    result = new ArrayList<PathWrapper>();
                    GeoPoint start = new GeoPoint(Double.parseDouble(record.get(6)), Double.parseDouble(record.get(5)));
                    GeoPoint end   = new GeoPoint(Double.parseDouble(record.get(10)), Double.parseDouble(record.get(9)));
                    for (int i = 0; i < 100; i++) {
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
                            if (mSemaphor == 100) break;
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

//        } catch (Exception e) {
//            System.err.println(e);
//            exit(1);
//        }
*/

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
    public static List<QueryResult> lookup(List<GHPoint> points, FlagEncoder encoder, LocationIndex locationIndex) {
        if (points.size() < 2)
            throw new IllegalArgumentException("At least 2 points have to be specified, but was:" + points.size());

        EdgeFilter edgeFilter = new DefaultEdgeFilter(encoder);

        List<QueryResult> queryResults = new ArrayList<QueryResult>(points.size());
        for (int placeIndex = 0; placeIndex < points.size(); placeIndex++) {
            GHPoint point = points.get(placeIndex);
            System.out.println("lookup ViaRoutingTemplate");
            QueryResult res = locationIndex.findClosest(point.lat, point.lon, edgeFilter);

            queryResults.add(res);
        }

        return queryResults;
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

