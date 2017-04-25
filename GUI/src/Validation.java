import com.graphhopper.util.PointList;
import org.mapsforge.core.model.LatLong;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by zonglin on 4/25/17.
 */
public class Validation {
    private double mThreshold;
    private ArrayList<LatLong> mMainPath;
    private HashSet<Pair> mResults;
    private ArrayList<ArrayList<LatLong>> mToDist;
    private ArrayList<ArrayList<LatLong>> mFromSouce;

    public Validation(double threshold, PointList mainPath) {
        mThreshold = threshold;
        for (int i = 0; i < mainPath.size(); i++) {
            mMainPath.add(new LatLong(mainPath.getLatitude(i), mainPath.getLongitude(i)));
        }
    }

    public void addToDist(PointList toDist) {
        ArrayList<LatLong> newArray = new ArrayList<LatLong>();
        for (int i = 0; i < toDist.size(); i++) {
            newArray.add(new LatLong(toDist.getLatitude(i), toDist.getLongitude(i)));
        }
        mToDist.add(newArray);
    }

    public void addFromSouce(PointList fromSouce) {
        ArrayList<LatLong> newArray = new ArrayList<LatLong>();
        for (int i = 0; i < fromSouce.size(); i++) {
            newArray.add(new LatLong(fromSouce.getLatitude(i), fromSouce.getLongitude(i)));
        }
        mToDist.add(newArray);
    }

    public void run() {
        
    }
}
