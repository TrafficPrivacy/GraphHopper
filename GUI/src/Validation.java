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
        mMainPath = new ArrayList<LatLong>();
        mToDist = new ArrayList<ArrayList<LatLong>>();
        mFromSouce = new ArrayList<ArrayList<LatLong>>();
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
        mFromSouce.add(newArray);
    }

    public boolean run() {
        ArrayList<HashSet<Pair>> toSet = new ArrayList<HashSet<Pair>>();
        ArrayList<HashSet<Pair>> fromSet = new ArrayList<HashSet<Pair>>();

        // Convert two sets
        for (ArrayList<LatLong> path : mToDist) {
            LatLong first = path.get(0);
            LatLong second;
            HashSet<Pair> set = new HashSet<Pair>();
            for (int i = 1; i < path.size(); i++) {
                second = path.get(i);
                Pair newPair = new Pair(first, second);
                set.add(newPair);
            }
            toSet.add(set);
        }

        for (ArrayList<LatLong> path : mFromSouce) {
            LatLong first = path.get(0);
            LatLong second;
            HashSet<Pair> set = new HashSet<Pair>();
            for (int i = 1; i < path.size(); i++) {
                second = path.get(i);
                Pair newPair = new Pair(first, second);
                set.add(newPair);
                first = second;
            }
            fromSet.add(set);
        }

        LatLong first = mMainPath.get(0);
        LatLong second;
        for (int i = 1; i < mMainPath.size(); i++) {
            second = mMainPath.get(i);
            Pair newPair = new Pair(first, second);

            int counter = 0;
            // compare with the from set
            for (HashSet<Pair> set : fromSet) {
                if (set.contains(newPair))
                    counter ++;
            }
            System.out.println("counter = " + counter + " from set size = " + fromSet.size());
            if (!((counter + 0.0) / fromSet.size() >= mThreshold))
                return false;

            // compare with the to set
            counter = 0;
            for (HashSet<Pair> set : toSet) {
                if (set.contains(newPair))
                    counter ++;
            }
            System.out.println("counter = " + counter + " to set size = " + toSet.size());
            if (!((counter + 0.0) / toSet.size() >= mThreshold))
                return false;

            first = second;
        }

        return true;
    }
}
