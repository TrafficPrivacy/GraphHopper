import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.PathWrapper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.Parameters;
import org.mapsforge.core.model.LatLong;

import java.awt.*;

/**
 * Created by zonglin on 1/25/17.
 */
public class TestRun {

    private static PathWrapper mResult;
    private static GraphHopperOSM mHopper;
    private static MapUI mMapUI;

    private static final LatLong START = new LatLong(41.8339025, -88.2305774);
    private static final LatLong END   = new LatLong(41.9741032, -87.870702);
    private static final double RADIUS = 0.1;

    /**
     * First argument is .osm data location. Second is the .map data location. Third is the graphhopper
     * directory location
     * @param args
     */
    public static void main(String args[]) {
        mHopper = new GraphHopperOSM();
        mHopper.setOSMFile(args[0]);
        mHopper.forDesktop();
        mHopper.setGraphHopperLocation(args[2]);
        mHopper.setEncodingManager(new EncodingManager("car"));
        mHopper.importOrLoad();
        mMapUI = new MapUI(args[1], "test");
        mMapUI.setVisible(true);
        PathWrapper mainPath = calcPath(START, END);
        mMapUI.setMainPath(mainPath.getPoints());
        mMapUI.showUpdate();
        for (int i = 0; i < 100; i++) {
            PathWrapper path = calcPath(dot_generator(START, RADIUS), dot_generator(END, RADIUS));
            try {
                mMapUI.addPath(path.getPoints());
            } catch (Exception e) {
                continue;
            }
        }
        mMapUI.showUpdate();
        mMapUI.createCircle(START, Color.LIGHT_GRAY.getRGB(), 10000.0f);
        mMapUI.createCircle(END, Color.LIGHT_GRAY.getRGB(), 10000.0f);
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

}
