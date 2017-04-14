import com.graphhopper.routing.QueryGraph;
import com.graphhopper.routing.util.DefaultEdgeFilter;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.weighting.ShortestWeighting;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.NodeAccess;
import com.graphhopper.storage.index.LocationIndex;
import com.graphhopper.storage.index.QueryResult;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import org.mapsforge.core.model.LatLong;

import java.util.HashMap;
import java.util.PriorityQueue;

public class Surroundings {
    private GraphHopperStorage mGhStore;
    private LocationIndex mIndex;
    private FlagEncoder mEncoder;
    private QueryGraph mQueryGraph;

    private Weighting mWeighting;
    private EdgeExplorer mInEdgeExplorer;
    private EdgeExplorer mOutEdgeExplorer;

    public Surroundings(GraphHopperStorage ghStore, LocationIndex index, FlagEncoder encoder) {
        mGhStore = ghStore;
        mIndex = index;
        mEncoder = encoder;
        mWeighting = new ShortestWeighting(mEncoder);
        mOutEdgeExplorer = ghStore.createEdgeExplorer(new DefaultEdgeFilter(mEncoder, false, true));
        mInEdgeExplorer = ghStore.createEdgeExplorer(new DefaultEdgeFilter(mEncoder, true, false));
    }

    public AdjacencyList getSurrounding(double latitude, double longitude, double distance) {
        QueryResult closest = mIndex.findClosest(latitude, longitude, EdgeFilter.ALL_EDGES);
        System.out.println("closest ID = " + closest.getClosestNode());
        return DijkstraSSSP(closest.getClosestNode(), distance);
    }

    private AdjacencyList DijkstraSSSP(int start, double distBound) {
        AdjacencyList<LatLong> spTree = new AdjacencyList<LatLong>();
        HashMap<Integer, NodeWrapper> nodeReference = new HashMap<Integer, NodeWrapper>();
        PriorityQueue<NodeWrapper> queue = new PriorityQueue<NodeWrapper>();
        /** Dijkstra **/
        nodeReference.put(start, new NodeWrapper(start, 0, start));
        queue.add(nodeReference.get(start));
        while (!queue.isEmpty()) {
            NodeWrapper current = queue.poll();
            EdgeIterator iter = mOutEdgeExplorer.setBaseNode(current.mNodeID);
            while(iter.next()) {
                int nextID = iter.getAdjNode();
                double tempDist = current.mDistance + iter.getDistance();
                if (nodeReference.containsKey(nextID)) {
                    NodeWrapper next = nodeReference.get(nextID);
                    if (next.mDistance > tempDist) {
                        queue.remove(next);
                        next.mDistance = tempDist;
                        next.mParent = current.mNodeID;
                        queue.add(next);
                    }
                } else {
                    if (tempDist < distBound) {
                        NodeWrapper next = new NodeWrapper(nextID, tempDist, current.mNodeID);
                        nodeReference.put(nextID, next);
                        queue.add(next);
                    }
                }
            }
        }

        /** convert to shortest path tree **/
        NodeAccess nodeAccess = mGhStore.getNodeAccess();
        for (NodeWrapper node : nodeReference.values()) {
            if (node.mParent != node.mNodeID) {
                LatLong parent = new LatLong(nodeAccess.getLat(node.mParent), nodeAccess.getLon(node.mParent));
                LatLong current = new LatLong(nodeAccess.getLat(node.mNodeID), nodeAccess.getLon(node.mNodeID));
                spTree.insertEdge(parent, current);
            }
        }
        return spTree;
    }

    private class NodeWrapper implements Comparable{
        public double mDistance;
        public final int mNodeID;
        public int mParent;

        public NodeWrapper(int mID, double distance, int parent) {
            mNodeID = mID;
            mDistance = distance;
            mParent = parent;
        }

        public int compareTo(Object o) {
            NodeWrapper n = (NodeWrapper) o;
            if (mDistance == n.mDistance)
                return 0;
            if (mDistance < n.mDistance)
                return -1;
            return 1;
        }
    }
}
