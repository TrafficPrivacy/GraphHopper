import com.graphhopper.PathWrapper;
import com.graphhopper.geohash.SpatialKeyAlgo;
import com.graphhopper.json.geo.Point;
import com.graphhopper.util.shapes.GHPoint;
import com.graphhopper.util.shapes.GHPoint3D;
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

import org.json.*;
import com.google.gson.Gson;

import java.io.FileWriter;
import java.io.IOException;


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

    private static ArrayList<Integer> Path(int u, int v, int[][] next) {
        ArrayList<Integer> path = new ArrayList<Integer>();

        if (next[u][v] == -1) {
            path = new ArrayList<Integer>();
            return path;
        }

        path.add(u);
        while (u != v) {
            u = (int)next[u][v];
            path.add(u);
            System.out.print("u: " + u + ", ");
        }
        System.out.println();

        return path;
    }


    public static void main(String args[]) throws Exception {
        long starttime = System.currentTimeMillis();

        if (args.length != 3) {
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

        long setgraphtime = System.currentTimeMillis();

        ///////////////////////////////////////////////
        //ALGORITHM START
        //////////////////////////////////////////////
        double queryLat = 40.744695434570313;
        double queryLon = -73.980371704101563;

        queryLat = 40.743;
        queryLon = -73.98;

        double queryLat2 = 40.734695434570313;
        double queryLon2 = -73.990371704101563;

        queryLat2 = 40.734;
        queryLon2 = -73.99;

        QueryGraph queryGraph = new QueryGraph(ghStorage);
        RoutingTemplate routingTemplate;
        Weighting weighting = new ShortestWeighting(encoder);
        TraversalMode tMode = TraversalMode.EDGE_BASED_2DIR; //TraversalMode.NODE_BASED;

        GHPoint startpoint = new GHPoint(queryLat, queryLon);
        GHPoint endpoint = new GHPoint(queryLat2, queryLon2);
        List<GHPoint> points = Arrays.asList(startpoint, endpoint);
        List<QueryResult> qResults = lookup(points, encoder, locationIndex);
        queryGraph.lookup(qResults);

        QueryResult fromQResult = qResults.get(0);
        QueryResult toQResult = qResults.get(1);

        GHPoint3D closestStartPoint = fromQResult.getSnappedPoint();
        GHPoint3D closestEndPoint = toQResult.getSnappedPoint();

        System.out.println(closestStartPoint);
        System.out.println(closestEndPoint);

        queryLat = closestStartPoint.getLat();
        queryLon = closestStartPoint.getLon();

        queryLat2 = closestEndPoint.getLat();
        queryLon2 = closestEndPoint.getLon();


        //////////////

        List<GHPoint> circum_points = engine.BFSCircum(queryLat, queryLon, edgeFilter);

        System.out.println("Circum points: " + circum_points.size());

        Hashtable<GHPoint, Integer> hash_circum = new Hashtable<GHPoint, Integer>();

        for (int i = 0; i < circum_points.size(); i++) {
            //System.out.println(adj_points.get(i));
            hash_circum.put(circum_points.get(i), i);
        }
        //List<GHPoint> area_points = engine.BFSArea(queryLat, queryLon, edgeFilter);
        BFSTest.AdjMatrixReturn adjmatrix_return = engine.BFSAdjMatrix(queryLat, queryLon, edgeFilter);

        List<GHPoint> adj_points = adjmatrix_return.points;
        List<BFSTest.AdjMatrixEdge> adj_edges = adjmatrix_return.edges;

        System.out.println("points length: " + adj_points.size() + ", edge length: " + adj_edges.size());

        Hashtable<GHPoint, Integer> hash_points = new Hashtable<GHPoint, Integer>();
        Hashtable<Integer, GHPoint> hash_points_reverse = new Hashtable<Integer, GHPoint>();


        for (int i = 0; i < adj_points.size(); i++) {
            //System.out.println(adj_points.get(i));
            hash_points.put(adj_points.get(i), i);
            hash_points_reverse.put(i, adj_points.get(i));
        }

        double[][] adj_matrix = new double[adj_points.size()][adj_points.size()];

        int[][] next_matrix = new int[adj_points.size()][adj_points.size()];

        for (int i = 0; i < adj_points.size(); i++) {
            for (int j = 0; j < adj_points.size(); j++) {
                if (i == j) {
                    adj_matrix[i][j] = 0;
                } else {
                    adj_matrix[i][j] = 99999;
                }
                next_matrix[i][j] = -1;
            }
        }

        for (int i = 0; i < adj_edges.size(); i++) {
            GHPoint start = adj_edges.get(i).start;
            GHPoint end = adj_edges.get(i).end;
            int start_index = -1;
            int end_index = -1;
            if (hash_points.containsKey(start)) {
                start_index = hash_points.get(start);
                //System.out.println(hash_points.get(start));
            } else {
                //System.out.println(adj_edges.get(i).edge_len);
            }
            if (hash_points.containsKey(end)) {
                end_index = hash_points.get(end);
                //System.out.println(hash_points.get(end));
            } else {
                //System.out.println(adj_edges.get(i).edge_len);
            }

            if (start_index >= 0 && end_index >= 0) {
                adj_matrix[start_index][end_index] = adj_edges.get(i).edge_len;
                //this is important!
                //adj_matrix[end_index][start_index] = adj_edges.get(i).edge_len;

                next_matrix[start_index][end_index] = end_index;

                //next_matrix[end_index][start_index] = start_index;


            }
            //System.out.println(adj_edges.get(i).start + ", " + adj_edges.get(i).end + ", " + adj_edges.get(i).edge_len);
        }


        FloydWarshall fw = new FloydWarshall(adj_points.size());
        fw.setGraph(adj_matrix);
        fw.setNextGraph(next_matrix);

        FloydWarshall.FW_ret fw_ret = fw.floydWarshall();


        double[][] fw_matrix = fw_ret.distances;
        int[][] fw_next = fw_ret.next;


        for (int i = 0; i < fw_next.length; i++) {
            int[] row = fw_next[i];
            for (int j = 0; j < row.length; j++) {
                //ArrayList<Integer> path = Path(i, j, fw_next);

                //if(path.size() > 0) {
                //    System.out.println("Path: " + path);
                //}

            }
        }

        long warshalltime1 = System.currentTimeMillis();

        /////////////////////////////////////////

        /*
        for(int i = 0; i < fw_matrix.length; i++){
            double [] row = fw_matrix[i];
            for(int j = 0; j < row.length; j++) {
                System.out.print(row[j] + ", ");
            }
            System.out.println("\n");
        }*/

        //final double queryLat2 = 40.744695434570313;
        //final double queryLon2 = -73.980371704101563;
        //40.734695434570313, -73.990371704101563


        /////////////////////////////////////////

        List<GHPoint> circum_points2 = engine.BFSCircum(queryLat2, queryLon2, edgeFilter);

        //what did i say i was gonna do about the dots?  Find the dots in BFS
        //return pairs that are edges
        System.out.println("Circum points2: " + circum_points2.size());

        Hashtable<GHPoint, Integer> hash_circum2 = new Hashtable<GHPoint, Integer>();

        for (int i = 0; i < circum_points2.size(); i++) {
            if (hash_circum.containsKey(circum_points2.get(i))) {
                System.out.println("Overlap");
            }
            hash_circum2.put(circum_points2.get(i), i);
        }
        BFSTest.AdjMatrixReturn adjmatrix_return2 = engine.BFSAdjMatrix(queryLat2, queryLon2, edgeFilter);

        List<GHPoint> adj_points2 = adjmatrix_return2.points;
        List<BFSTest.AdjMatrixEdge> adj_edges2 = adjmatrix_return2.edges;

        System.out.println("points length: " + adj_points2.size() + ", edge length: " + adj_edges2.size());

        Hashtable<GHPoint, Integer> hash_points2 = new Hashtable<GHPoint, Integer>();
        Hashtable<Integer, GHPoint> hash_points_reverse2 = new Hashtable<Integer, GHPoint>();


        for (int i = 0; i < adj_points2.size(); i++) {
            //System.out.println(adj_points2.get(i));
            hash_points2.put(adj_points2.get(i), i);
            hash_points_reverse2.put(i, adj_points2.get(i));
        }

        double[][] adj_matrix2 = new double[adj_points2.size()][adj_points2.size()];
        int[][] next_matrix2 = new int[adj_points2.size()][adj_points2.size()];

        for (int i = 0; i < adj_points2.size(); i++) {
            for (int j = 0; j < adj_points2.size(); j++) {
                if (i == j) {
                    adj_matrix2[i][j] = 0;
                } else {
                    adj_matrix2[i][j] = 99999;
                }
                next_matrix2[i][j] = -1;
            }
        }

        for (int i = 0; i < adj_edges2.size(); i++) {
            GHPoint start2 = adj_edges2.get(i).start;
            GHPoint end2 = adj_edges2.get(i).end;
            int start_index2 = -1;
            int end_index2 = -1;
            if (hash_points2.containsKey(start2)) {
                start_index2 = hash_points2.get(start2);
            }

            if (hash_points2.containsKey(end2)) {
                end_index2 = hash_points2.get(end2);
            }


            if (start_index2 >= 0 && end_index2 >= 0) {
                adj_matrix2[start_index2][end_index2] = adj_edges2.get(i).edge_len;
                //this is important!
                //adj_matrix2[end_index2][start_index2] = adj_edges2.get(i).edge_len;
                next_matrix2[start_index2][end_index2] = end_index2;
                //next_matrix2[end_index2][start_index2] = start_index2;

            }
        }

        FloydWarshall fw2 = new FloydWarshall(adj_points2.size());
        fw2.setGraph(adj_matrix2);
        fw2.setNextGraph(next_matrix2);

        FloydWarshall.FW_ret fw_ret2 = fw2.floydWarshall();

        double[][] fw_matrix2 = fw_ret2.distances;
        int[][] fw_next2 = fw_ret2.next;



        /*
        for(int i = 0; i < fw_matrix2.length; i++){
            double [] row = fw_matrix2[i];
            for(int j = 0; j < row.length; j++) {
                System.out.print(row[j] + ", ");
            }
            System.out.println("\n");
        }
        */

        /*
        for(int i = 0; i < fw_matrix2.length; i++){
            double [] row = fw_matrix2[i];
            for(int j = 0; j < row.length; j++) {
                ArrayList<Integer> path = Path(i, j, fw_next2);
                if(path.size() > 0) {
                    System.out.println("Path: " + path.size());
                }
            }
        }
        */


        long warshalltime2 = System.currentTimeMillis();


        /////////////////////////////////////////


        Double circum_dist[][] = new Double[circum_points.size()][circum_points2.size()];
        JSONArray circum_path[][] = new JSONArray[circum_points.size()][circum_points2.size()];

        /*
        QueryGraph queryGraph = new QueryGraph(ghStorage);
        RoutingTemplate routingTemplate;
        Weighting weighting = new ShortestWeighting(encoder);
        TraversalMode tMode = TraversalMode.EDGE_BASED_2DIR;

        GHPoint startpoint = new GHPoint(40.734695434570313, -73.990371704101563);
        GHPoint endpoint = new GHPoint(40.735695434570313, -73.995371704101563);
        List<GHPoint> points = Arrays.asList(startpoint, endpoint);
        List<QueryResult> qResults = lookup(points, encoder, locationIndex);
        queryGraph.lookup(qResults);

        RoutingAlgorithm bidir_algo = new DijkstraBidirectionRef(queryGraph, encoder, weighting, tMode);
         */


        //bmf_algo = new BellmanFord(queryGraph, encoder, weighting, tMode);


        DistanceCalc distCalc = Helper.DIST_PLANE;

        for (int i = 0; i < circum_points.size(); i++) {
            for (int j = 0; j < circum_points2.size(); j++) {
                RoutingAlgorithm bidir_algo = new DijkstraBidirectionRef(queryGraph, encoder, weighting, tMode);


                GHPoint point1 = circum_points.get(i);
                GHPoint point2 = circum_points2.get(j);


                points = Arrays.asList(point1, point2);
                qResults = lookup(points, encoder, locationIndex);
                fromQResult = qResults.get(0);
                toQResult = qResults.get(1);

                long dijkstra_start_time = System.currentTimeMillis();
                List<Path> tmpPathList = bidir_algo.calcPaths(fromQResult.getClosestNode(), toQResult.getClosestNode());
                long dijkstra_end_time = System.currentTimeMillis();

                long dijkstra_start_time2 = System.currentTimeMillis();
                List<Path> tmpPathList2 = bidir_algo.calcPaths(fromQResult.getClosestNode(), toQResult.getClosestNode());
                long dijkstra_end_time2 = System.currentTimeMillis();


                Path tmpPath = tmpPathList.get(0);
                double dist = tmpPath.getDistance();

                //double dist = distCalc.calcDist(point1.getLat(), point1.getLon(), point2.getLat(), point2.getLon());
                circum_dist[i][j] = dist;

                ArrayList<GHPoint> path_arraylist = new ArrayList<GHPoint>();
                PointList intermediate_path_list = tmpPath.calcPoints();

                for(int k = 1; k < intermediate_path_list.size()-1; k++){
                    GHPoint curr_point = new GHPoint(intermediate_path_list.getLat(k), intermediate_path_list.getLon(k));
                    path_arraylist.add(curr_point);

                }

                JSONArray path_arraylist_json = new JSONArray(path_arraylist);
                //System.out.println(path_arraylist_json);
                circum_path[i][j] = path_arraylist_json;

                long dijkstra_run_time = dijkstra_end_time - dijkstra_start_time;
                long dijkstra_run_time2 = dijkstra_end_time2 - dijkstra_start_time2;

                //System.out.println("Dijkstra run time: " + dijkstra_run_time + ", Dijkstra run time2: " + dijkstra_run_time2);


            }
        }

        /*
        for(int i = 0; i < circum_dist.length; i++){
            double [] row = circum_dist[i];
            for(int j = 0; j < row.length; j++) {
                System.out.print(row[j] + ", ");
            }
            System.out.println("\n");
        }
        */

        ///////////////////////////////////////////////

        /*FINAL MATRIX CREATION: USE JSON
         NEED: circum_dist, fw_matrix, fw_matrix2, hash_points_reverse, hash_points_reverse2, hash_points, hash_points2
        */
        JSONArray circum_dist_json = new JSONArray(circum_dist);
        System.out.println(circum_dist_json);
        JSONArray fw_matrix_json = new JSONArray(fw_matrix);

        JSONArray fw_matrix2_json = new JSONArray(fw_matrix2);
        JSONArray fw_next_json = new JSONArray(fw_next);
        JSONArray fw_next2_json = new JSONArray(fw_next2);
        //System.out.println("circum_points: " + circum_points.get(0));
        JSONArray circum_points_json = new JSONArray(circum_points);
        JSONArray circum_points2_json = new JSONArray(circum_points2);
        JSONObject hash_points_reverse_json = new JSONObject(hash_points_reverse);
        JSONObject hash_points_reverse2_json = new JSONObject(hash_points_reverse2);
        JSONObject hash_points_json = new JSONObject(hash_points);
        JSONObject hash_points2_json = new JSONObject(hash_points2);

        JSONArray circum_path_json = new JSONArray(circum_path);


        JSONObject total_json = new JSONObject();
        total_json.put("circum_dist", circum_dist_json);
        total_json.put("fw_matrix", fw_matrix_json);
        total_json.put("fw_matrix2", fw_matrix2_json);
        total_json.put("fw_next", fw_next_json);
        total_json.put("fw_next2", fw_next2_json);
        total_json.put("circum_points", circum_points_json);
        total_json.put("circum_points2", circum_points2_json);
        total_json.put("hash_points_reverse", hash_points_reverse_json);
        total_json.put("hash_points_reverse2", hash_points_reverse2_json);
        total_json.put("hash_points", hash_points_json);
        total_json.put("hash_points2", hash_points2_json);
        total_json.put("circum_path", circum_path_json);

        FileWriter file = new FileWriter("/Users/Edward_Chou/Documents/json_file.txt");
        file.write(total_json.toString());
        System.out.println("Successfully Copied JSON Object to File...");


        main2(total_json);

        //System.out.println("total json: " + total_json);



        long dijkstratimestart = System.currentTimeMillis();

        GHPoint check_start_point = hash_points_reverse.get(15);

        GHPoint check_end_point = hash_points_reverse2.get(34);


        points = Arrays.asList(check_start_point, check_end_point);
        qResults = lookup(points, encoder, locationIndex);

        fromQResult = qResults.get(0);
        toQResult = qResults.get(1);


        RoutingAlgorithm bidir_algo = new DijkstraBidirectionRef(queryGraph, encoder, weighting, tMode);
        //bmf_algo = new BellmanFord(queryGraph, encoder, weighting, tMode);
        List<Path> tmpPathList = bidir_algo.calcPaths(fromQResult.getClosestNode(), toQResult.getClosestNode());
        System.out.println(tmpPathList);
        Path tmpPath = tmpPathList.get(0);
        PointList tmpPointList = tmpPath.calcPoints();

        long dijkstratimeend = System.currentTimeMillis();

        long dijkstratime = dijkstratimeend - dijkstratimestart;
        System.out.println("dijkstra time: " + dijkstratime);


        System.out.println("");

        System.out.print("Dijkstra path: ");


        double temp_len = 0;
        for(int i = 0; i < tmpPointList.size(); i++){
            GHPoint checkpoint = new GHPoint(tmpPointList.getLat(i), tmpPointList.getLon(i));

            //System.out.println(tmpPointList.getLat(i) + "," +  tmpPointList.getLon(i) + "; ");

        }
        System.out.println("");



    }



    private static void main2(JSONObject total_json) {


        JSONObject hash_points_reverse_json = (JSONObject)total_json.get("hash_points_reverse");
        JSONObject hash_points_reverse2_json = (JSONObject)total_json.get("hash_points_reverse2");
        JSONObject hash_points_json = (JSONObject)total_json.get("hash_points");
        JSONObject hash_points2_json = (JSONObject)total_json.get("hash_points2");


        hash_points_reverse_json = (JSONObject)total_json.get("hash_points_reverse");
        hash_points_reverse2_json = (JSONObject)total_json.get("hash_points_reverse2");
        hash_points_json = (JSONObject)total_json.get("hash_points");
        hash_points2_json = (JSONObject)total_json.get("hash_points2");


        JSONArray circum_dist_json_array = (JSONArray)total_json.get("circum_dist");
        JSONArray circum_dist_json_testrow = (JSONArray) circum_dist_json_array.get(0);
        Double[][] circum_dist = new Double[circum_dist_json_array.length()][circum_dist_json_testrow.length()];
        //circum_dist = new Double[circum_dist_json_array.length()][circum_dist_json_testrow.length()];

        for(int i = 0; i < circum_dist_json_array.length(); i++) {
            JSONArray circum_dist_json_row = (JSONArray) circum_dist_json_array.get(i);
            for (int j = 0; j < circum_dist_json_row.length(); j++) {
                Double elem = (Double)circum_dist_json_row.get(j);
                circum_dist[i][j] = elem;
            }
        }

        JSONArray fw_matrix_json_array = (JSONArray)total_json.get("fw_matrix");
        JSONArray fw_matrix_json_testrow = (JSONArray) fw_matrix_json_array.get(0);
        double[][] fw_matrix = new double[fw_matrix_json_array.length()][fw_matrix_json_testrow.length()];
        //fw_matrix = new double[fw_matrix_json_array.length()][fw_matrix_json_testrow.length()];

        for(int i = 0; i < fw_matrix_json_array.length(); i++) {
            JSONArray fw_matrix_json_row = (JSONArray) fw_matrix_json_array.get(i);
            for (int j = 0; j < fw_matrix_json_row.length(); j++) {
                Double elem = (Double) fw_matrix_json_row.get(j);
                fw_matrix[i][j] = elem;
            }
        }

        JSONArray fw_matrix2_json_array = (JSONArray)total_json.get("fw_matrix2");
        JSONArray fw_matrix2_json_testrow = (JSONArray) fw_matrix2_json_array.get(0);
        double[][] fw_matrix2 = new double[fw_matrix2_json_array.length()][fw_matrix2_json_testrow.length()];
        //fw_matrix2 = new double[fw_matrix2_json_array.length()][fw_matrix2_json_testrow.length()];

        for(int i = 0; i < fw_matrix2_json_array.length(); i++) {
            JSONArray fw_matrix2_json_row = (JSONArray) fw_matrix2_json_array.get(i);
            for (int j = 0; j < fw_matrix2_json_row.length(); j++) {
                Double elem = (Double)fw_matrix2_json_row.get(j);
                fw_matrix2[i][j] = elem;
            }
        }

        JSONArray fw_next_json_array = (JSONArray)total_json.get("fw_next");
        JSONArray fw_next_json_testrow = (JSONArray) fw_next_json_array.get(0);
        int[][] fw_next = new int[fw_next_json_array.length()][fw_next_json_testrow.length()];
        //fw_next = new int[fw_next_json_array.length()][fw_next_json_testrow.length()];

        for(int i = 0; i < fw_next_json_array.length(); i++) {
            JSONArray fw_next_json_row = (JSONArray) fw_next_json_array.get(i);
            for (int j = 0; j < fw_next_json_row.length(); j++) {
                Integer elem = (Integer) fw_next_json_row.get(j);
                fw_next[i][j] = elem;
            }
        }
        JSONArray fw_next2_json_array = (JSONArray)total_json.get("fw_next2");
        JSONArray fw_next2_json_testrow = (JSONArray) fw_next2_json_array.get(0);
        int[][] fw_next2 = new int[fw_next2_json_array.length()][fw_next2_json_testrow.length()];
        //fw_next2 = new int[fw_next2_json_array.length()][fw_next2_json_testrow.length()];

        for(int i = 0; i < fw_next2_json_array.length(); i++) {
            JSONArray fw_next2_json_row = (JSONArray) fw_next2_json_array.get(i);
            for (int j = 0; j < fw_next2_json_row.length(); j++) {
                Integer elem = (Integer) fw_next2_json_row.get(j);
                fw_next2[i][j] = elem;
            }
        }

        JSONArray circum_points_json_array = (JSONArray)total_json.get("circum_points");
        List<GHPoint> circum_points = new ArrayList<GHPoint>();
        //circum_points = new ArrayList<GHPoint>();

        for(int i = 0; i < circum_points_json_array.length(); i++){
            JSONObject retrieve_point = (JSONObject)circum_points_json_array.get(i);
            GHPoint retrieve_ghpoint = new GHPoint((Double)retrieve_point.get("lat"), (Double)retrieve_point.get("lon"));
            circum_points.add(retrieve_ghpoint);
        }

        JSONArray circum_points2_json_array = (JSONArray)total_json.get("circum_points2");
        List<GHPoint> circum_points2 = new ArrayList<GHPoint>();
        //circum_points2 = new ArrayList<GHPoint>();

        for(int i = 0; i < circum_points2_json_array.length(); i++){
            JSONObject retrieve_point = (JSONObject)circum_points2_json_array.get(i);
            GHPoint retrieve_ghpoint = new GHPoint((Double)retrieve_point.get("lat"), (Double)retrieve_point.get("lon"));
            circum_points2.add(retrieve_ghpoint);
        }



        JSONArray circum_path_json_array = (JSONArray)total_json.get("circum_path");
        JSONArray circum_path_json_testrow = (JSONArray) circum_path_json_array.get(0);
        JSONArray[][] circum_path = new JSONArray[circum_path_json_array.length()][circum_path_json_testrow.length()];
        //circum_path = new JSONArray[circum_path_json_array.length()][circum_path_json_testrow.length()];

        for(int i = 0; i < circum_path_json_array.length(); i++) {
            JSONArray circum_path_json_row = (JSONArray) circum_path_json_array.get(i);
            for (int j = 0; j < circum_path_json_row.length(); j++) {
                JSONArray elem = (JSONArray) circum_path_json_row.get(j);
                //System.out.println(elem);
                circum_path[i][j] = elem;
            }
        }



        /*
        GHPoint circum_point_test = circum_points.get(5);
        int circum_idx_test = hash_points.get(circum_point_test);
        int circum_idx_test2 = (Integer)hash_points_json.get(circum_point_test.toString());

        System.out.println("circum_idx: " + circum_idx_test + ", circum_idx2: " + circum_idx_test2);

        System.out.println(hash_points_reverse.get(15));
        JSONObject retrieve_point = (JSONObject)hash_points_reverse_json.get("15");
        GHPoint retrieve_ghpoint = new GHPoint((Double)retrieve_point.get("lat"), (Double)retrieve_point.get("lon"));
        System.out.println(retrieve_ghpoint);

         */

        int final_size = circum_points.size() + circum_points2.size() + 2;
        System.out.println("final size: " + final_size);
        double final_matrix[][] = new double[final_size][final_size];

        for(int i = 0; i < final_size; i++){
            for(int j = 0; j < final_size; j++){
                if(i == j){
                    final_matrix[i][j] = 0;
                }
                else {
                    final_matrix[i][j] = 99999;
                }
            }
        }

        /*
           0a 1a 2a 3a 0b 1b 2b 3b pa pb
        0a
        1a
        2a
        3a
        0b
        1b
        2b
        3b
        pa
        pb

         */

        for(int i = 0; i < circum_points.size(); i++){
            for(int j = 0; j < circum_points2.size(); j++){
                final_matrix[i][j + circum_points.size()] = circum_dist[i][j];
                //final_matrix[j+ circum_points.size()][i] = circum_dist[i][j];

            }
        }

        //source = 40.74581468282226,-73.98226277490548
        //dest = 40.73461906763805,-73.98621549426501
        int source_idx = 15;
        int end_idx = 34;

        Hashtable<Integer, GHPoint> bellmanford_hash1 = new Hashtable<Integer, GHPoint>();
        Hashtable<Integer, GHPoint> bellmanford_hash2 = new Hashtable<Integer, GHPoint>();

        for(int i = 0; i < circum_points.size(); i++){
            GHPoint circum_point = circum_points.get(i);
            int circum_idx = (Integer)hash_points_json.get(circum_point.toString()); //int circum_idx = hash_points.get(circum_point);
            //System.out.println(circum_idx);
            final_matrix[final_size-2][i] = fw_matrix[source_idx][circum_idx];
            bellmanford_hash1.put(i, circum_point);
            //final_matrix[i][final_size-2] =fw_matrix[source_idx][circum_idx];
        }

        for(int i = 0; i < circum_points2.size(); i++){
            GHPoint circum_point = circum_points2.get(i);
            int circum_idx = (Integer)hash_points2_json.get(circum_point.toString());//int circum_idx = hash_points2.get(circum_point);
            //System.out.println(circum_idx);
            //final_matrix[final_size-1][i + circum_points.size()] = fw_matrix2[end_idx][circum_idx];
            final_matrix[i + circum_points.size()][final_size-1] = fw_matrix2[end_idx][circum_idx];
            bellmanford_hash2.put(i, circum_point);


        }
        /*
        for(int i = 0; i < final_matrix.length; i++){
            double [] row = final_matrix[i];
            for(int j = 0; j < row.length; j++) {
                System.out.print(row[j] + ", ");
            }
            System.out.println("\n");
        }
        */
        int source = final_size-2;
        int dest = final_size-1;


        BellmanFord bellmanford = new BellmanFord(final_size);
        BellmanFord.BF_ret bf_ret =bellmanford.BellmanFordEvaluation(source, final_matrix);

        double [] distances = bf_ret.distances;
        double [] predecessors = bf_ret.predecessors;

        for (int vertex = 0; vertex < distances.length; vertex++) {
            System.out.println("distance of source  " + source + " to "
                    + vertex + " is " + distances[vertex]);
        }
        for (int i = 0; i < predecessors.length; i++) {
            System.out.print(predecessors[i] + " ");
        }
        System.out.println();



        int counter = 0;

        //calc route
        ArrayList<Double> route = new ArrayList<Double>();
        route.add((double)dest);

        int temp_dest = dest;
        while(predecessors[temp_dest] > -1){
            route.add(predecessors[temp_dest]);
            temp_dest = (int)predecessors[temp_dest];
        }
        for(int i = 0; i < route.size(); i ++){
            double elem = route.get(route.size() - i - 1);
            System.out.println(elem);
        }

        /////
        //Bellman Distance Calc
        double temp_bellman_point1 = route.get(2);
        double temp_bellman_point2 = route.get(1);

        double tempdist1 = final_matrix[source][(int)temp_bellman_point1];
        double tempdist2 = final_matrix[(int)temp_bellman_point1][(int)temp_bellman_point2];
        double tempdist3 = final_matrix[(int)temp_bellman_point2][dest];

        double bellman_dist_calc = tempdist1 + tempdist2 + tempdist3;
        System.out.println("bellman dist calc: " + bellman_dist_calc);

        double bellman_point1 = route.get(2);
        double bellman_point2 = route.get(1) - circum_points.size();

        //System.out.println(intermediate_path);

        System.out.println(bellman_point1 + ", " +  bellman_point2);

        GHPoint intermediate1 = bellmanford_hash1.get((int)bellman_point1);
        GHPoint intermediate2 = bellmanford_hash2.get((int)bellman_point2);

        System.out.println("intermediate: " + intermediate1 + ", " +  intermediate2);

        int circum_index1 = (Integer)hash_points_json.get(intermediate1.toString());//int circum_index1 = hash_points.get(intermediate1);
        int circum_index2 = (Integer)hash_points2_json.get(intermediate2.toString());//int circum_index2 = hash_points2.get(intermediate2);

        System.out.println("circum indices: " + circum_index1 + ", " + circum_index2);

        double temptempdist1 = fw_matrix[source_idx][circum_index1];
        double temptempdist3 = fw_matrix2[circum_index2][end_idx];

        System.out.println("temp dist1: " + tempdist1 + ", temptempdist1: " + temptempdist1);
        System.out.println("temp dist3: " + tempdist3 + ", temptempdist3: " + temptempdist3);


//////////
        ArrayList<GHPoint> final_path = new ArrayList<GHPoint>();


        ArrayList<Integer> path1 = Path(source_idx, circum_index1, fw_next);
        System.out.println("path1: " + path1);


        for(int i = 0; i < path1.size(); i++){
            //GHPoint curr_point = hash_points_reverse.get(path1.get(i));
            JSONObject retrieve_point = (JSONObject)hash_points_reverse_json.get(path1.get(i) + "");
            GHPoint curr_point = new GHPoint((Double)retrieve_point.get("lat"), (Double)retrieve_point.get("lon"));

            //System.out.println(curr_point);
            final_path.add(curr_point);

        }
        System.out.println();


        JSONArray intermediate_path = circum_path[(int)bellman_point1][(int)bellman_point2];
        System.out.println(intermediate_path);

        for(int i = 1; i < intermediate_path.length()-1; i++){
            JSONObject retrieve_obj = (JSONObject)intermediate_path.get(i);
            Double curr_lat = (Double)retrieve_obj.get("lat");
            Double curr_lon = (Double)retrieve_obj.get("lon");

            GHPoint curr_point = new GHPoint(curr_lat, curr_lon);
            //System.out.println(curr_point);
            final_path.add(curr_point);

        }


        ArrayList<Integer> path2 = Path(circum_index2, end_idx, fw_next2);
        for(int i = 0; i < path2.size(); i++){
            //GHPoint curr_point = hash_points_reverse2.get(path2.get(i));
            JSONObject retrieve_point = (JSONObject)hash_points_reverse2_json.get(path2.get(i) + "");
            GHPoint curr_point = new GHPoint((Double)retrieve_point.get("lat"), (Double)retrieve_point.get("lon"));

            final_path.add(curr_point);
            //System.out.println(curr_point);
        }
        System.out.println("path2: " + path2);





        long bellmantime = System.currentTimeMillis();


        //int source_idx = 10; fw_matrix
        //int end_idx = 72; fw_matrix2

        //GHPoint check_start_point = hash_points_reverse.get(source_idx);
        JSONObject retrieve_point = (JSONObject)hash_points_reverse_json.get(source_idx + "");
        GHPoint check_start_point_new= new GHPoint((Double)retrieve_point.get("lat"), (Double)retrieve_point.get("lon"));

        //GHPoint check_end_point = hash_points_reverse2.get(end_idx);
        retrieve_point = (JSONObject)hash_points_reverse2_json.get(end_idx + "");
        GHPoint check_end_point_new = new GHPoint((Double)retrieve_point.get("lat"), (Double)retrieve_point.get("lon"));

        //double check_dist = distCalc.calcDist(check_start_point.getLat(), check_start_point.getLon(), check_end_point.getLat(), check_end_point.getLon());
        //System.out.println("Check dist: " + check_dist);


        //double total_time = bellmantime - starttime;
        //System.out.println("Run time: " + total_time);


        ////////

       System.out.println("Start: " + check_start_point_new.getLat() + ", " + check_start_point_new.getLon() + ", End: " + check_end_point_new.getLat() + ", " + check_end_point_new.getLon());


        ///////////

        /*

        for(int i = 0; i < 16; i++){
            GHPoint checkpoint = new GHPoint(tmpPointList.getLat(i), tmpPointList.getLon(i));
            if(!hash_points.containsKey(checkpoint)){
                System.out.print(tmpPointList.getLat(i) + "," +  tmpPointList.getLon(i) + "; ");
            }

            //System.out.print(tmpPointList.getLat(i) + "," +  tmpPointList.getLon(i) + "; ");
        }


        System.out.println("");


        for(int i = 46; i < 52; i++){
            GHPoint checkpoint = new GHPoint(tmpPointList.getLat(i), tmpPointList.getLon(i));
            if(!hash_points2.containsKey(checkpoint)){
                System.out.print(tmpPointList.getLat(i) + "," +  tmpPointList.getLon(i) + "; ");
            }
            //System.out.print(tmpPointList.getLat(i) + "," +  tmpPointList.getLon(i) + "; ");
        }

        System.out.println("");
        */
        System.out.print("Our path: ");

        GHPoint tempPoint = final_path.get(0);
        for(int i = 0; i < final_path.size(); i++){

            GHPoint newPoint = final_path.get(i);
            //temp_len += distCalc.calcDist(newPoint.getLat(), newPoint.getLon(), tempPoint.getLat(), tempPoint.getLon());

            tempPoint = newPoint;

            System.out.println(final_path.get(i) + "; ");
        }
        System.out.println();
        //System.out.println("temp_len: " + temp_len );





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
            //System.out.println("lookup ViaRoutingTemplate");
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

