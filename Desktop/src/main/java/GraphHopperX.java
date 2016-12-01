import com.graphhopper.GraphHopper;
import com.graphhopper.routing.util.DefaultEdgeFilter;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.shapes.GHPoint;

import java.util.List;

/**
 * Created by Zonglin Li on 11/26/2016.
 */
public class GraphHopperX extends GraphHopper {

    public GraphHopperX setMapDataFileLocation(String fileLocation) {
        super.setDataReaderFile(fileLocation);
        return this;
    }

    public List<GHPoint> nearestPoints(GHPoint center, double radius, int number, String vehicle) {
        Graph graph = super.getGraphHopperStorage();
        LocationIndex locationIndex = super.getLocationIndex();
        FlagEncoder encoder = super.getEncodingManager().getEncoder(vehicle);
        QueryResult queryResult = locationIndex.findClosest(center.lat, center.lon, new DefaultEdgeFilter(encoder));
        EdgeExplorer outEdgeExplorer = graph.createEdgeExplorer(new DefaultEdgeFilter(encoder, false, true));
        int closestNode = queryResult.getClosestNode();
        return null;
    }

}
