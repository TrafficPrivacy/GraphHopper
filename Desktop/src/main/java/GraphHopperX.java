import com.graphhopper.GraphHopper;

/**
 * Created by Zonglin Li on 11/26/2016.
 */
public class GraphHopperX extends GraphHopper {

    public GraphHopperX setMapDataFileLocation(String fileLocation) {
        super.setDataReaderFile(fileLocation);
        return this;
    }

}
