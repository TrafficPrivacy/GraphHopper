import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.PathWrapper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.Parameters;

/**
 * Created by zonglin on 11/3/16.
 */
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
