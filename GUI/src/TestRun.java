import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.PathWrapper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.util.Parameters;
import org.lwjgl.Sys;
import org.mapsforge.core.model.LatLong;

import java.awt.*;
import java.util.ArrayList;

/**
 * Created by zonglin on 1/25/17.
 */
public class TestRun {

    private static PathWrapper mResult;
    private static GraphHopperOSM mHopper;
    private static MapUI mMapUI;
    private static Surroundings mSurroundings;

    private static final LatLong START = new LatLong(40.074985,-88.252401);
    private static final LatLong END   = new LatLong(40.115233,-88.235407);
    private static final double RADIUS = 1000;  // in meters
    private static final double THRESHOLD = 0.0;

    /**
     * First argument is .osm data location. Second is the .map data location. Third is the graphhopper
     * directory location. Fourth is whether run the GUI for terminal (gui/terminal)
     * @param args
     */
    public static void main(String args[]) {
        EncodingManager em = new EncodingManager("car");
        mHopper = new GraphHopperOSM();
        mHopper.setOSMFile(args[0]);
        mHopper.forDesktop();
        mHopper.setGraphHopperLocation(args[2]);
        mHopper.setEncodingManager(em);
        mHopper.importOrLoad();

        mSurroundings = new Surroundings(mHopper.getGraphHopperStorage(), mHopper.getLocationIndex(), em.getEncoder("car"));
        ArrayList<LatLong> sources = surrounding(START, RADIUS);
        ArrayList<LatLong> targets = surrounding(END, RADIUS);
        System.out.println("source size = " + sources.size());
        System.out.println("target size = " + targets.size());

        if (args[3].equals("gui")) {
            mMapUI = new MapUI(args[1], "test");
            mMapUI.setVisible(true);
            PathWrapper mainPath = calcPath(START, END);
            mMapUI.setMainPath(mainPath.getPoints());
            mMapUI.showUpdate();

            // for test
            for (LatLong dot : sources) {
                mMapUI.createDot(dot, new java.awt.Color(6, 0, 133, 255).getRGB(), 6);
            }

            for (LatLong dot : targets) {
                mMapUI.createDot(dot, new java.awt.Color(6, 0, 133, 255).getRGB(), 6);
            }

            /* Pair by pair */
//            for (int i = 0; i < (sources.size() > targets.size() ? targets.size() : sources.size()); i++) {
//    //            PathWrapper path = calcPath(dot_generator(START, RADIUS), dot_generator(END, RADIUS));
//    //            PathWrapper path = calcPath(dot_generator(START, RADIUS), END);
//    //            PathWrapper path = calcPath(START, END);
//                PathWrapper path = calcPath(sources.get(i), targets.get(i));
//                try {
//                    mMapUI.addPath(path.getPoints());
//                } catch (Exception e) {
//                    continue;
//                }
//            }

            /* All pairs */
            for (int i = 0; i < sources.size(); i++) {
                for (int j = 0; j < targets.size(); j++) {
                    LatLong source = sources.get(i);
                    LatLong target = targets.get(j);
                    PathWrapper path = calcPath(source, target);
                    try {
                        mMapUI.addPath(path.getPoints());
                    } catch (Exception e) {
                        System.out.println("exception. No path");
                        continue;
                    }
                }
            }

            System.out.println("================");
            System.out.println("Finished routing");
            System.out.println("================");

            mMapUI.showUpdate();
            mMapUI.createCircle(START, Color.LIGHT_GRAY.getRGB(), (float) RADIUS);
            mMapUI.createCircle(END, Color.LIGHT_GRAY.getRGB(), (float) RADIUS);

            System.out.println("================");
            System.out.println("Finished drawing");
            System.out.println("================");
        }

        if (args[3].equals("terminal")) {

            Validation validation = new Validation(THRESHOLD, calcPath(START, END).getPoints());

            // from generated to single target
            for (LatLong source : sources) {
                PathWrapper path = calcPath(source, END);
                validation.addToDist(path.getPoints());
            }

            // from single source to generated targets
            for (LatLong target : targets) {
                PathWrapper path = calcPath(START, target);
                validation.addFromSouce(path.getPoints());
            }

            boolean hasPath = validation.run();
            System.out.println(hasPath ? "Has path" : "Doesn't have path");

        }
    }

    public static PathWrapper calcPath(LatLong from, LatLong to) {
        GHRequest req = new GHRequest(from.latitude, from.longitude,
                to.latitude, to.longitude).
                setAlgorithm(Parameters.Algorithms.DIJKSTRA_BI);
        req.getHints().
                put(Parameters.Routing.INSTRUCTIONS, "false");
//        System.out.println("start = " + from + " end = " + to);
        GHResponse resp = mHopper.route(req);
        try {
            return resp.getBest();
        } catch (Exception e) {
            return new PathWrapper();
        }
    }

    private static LatLong dot_generator(LatLong origin, double maxradii) {
        double radii = Math.random() * maxradii;
        double angle = Math.random() * Math.PI * 2;
        LatLong result = new LatLong(origin.latitude + radii * Math.sin(angle) / 2,
                origin.longitude + radii * Math.cos(angle));
        return result;
    }

    private static ArrayList<LatLong> surrounding(LatLong origin, double distance) {
        AdjacencyList tree = mSurroundings.getSurrounding(origin.latitude, origin.longitude, distance);
        return tree.getNodes();
    }

}
