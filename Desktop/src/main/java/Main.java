import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.PathWrapper;
import com.graphhopper.reader.osm.GraphHopperOSM;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.util.Parameters.*;
import com.graphhopper.util.PointList;

public class Main {

    public static void main(String args[]) {
        System.out.println(args[0]);
        PathWrapper response = new RoutingTest(args[0]).calcPath(41.3680905, -89.0375749, 41.584162, -88.4113542);
        PointList tmp = response.getPoints();
        for (int i = 0; i < response.getPoints().getSize(); i++) {
            System.out.println(tmp.getLatitude(i) + ", " + tmp.getLongitude(i));
        }
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

    public PathWrapper calcPath(final double fromLat, final double fromLon,
                                final double toLat, final double toLon) {
        GHRequest req = new GHRequest(fromLat, fromLon, toLat, toLon).
                setAlgorithm(Algorithms.DIJKSTRA_BI);
        req.getHints().
                put(Routing.INSTRUCTIONS, "false");
        GHResponse resp = mHopper.route(req);
        return resp.getBest();
    }
}
