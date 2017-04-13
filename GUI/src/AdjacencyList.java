import java.util.ArrayList;
import java.util.HashMap;

public class AdjacencyList<Node>{
    private HashMap<Node, ArrayList<Node>> mTable;

    public AdjacencyList() {
        mTable = new HashMap<Node, ArrayList<Node>>();
    }

    public AdjacencyList<Node> insertNode(Node n) {
        if (!mTable.containsKey(n))
            mTable.put(n, new ArrayList<Node>());
        return this;
    }

    public AdjacencyList<Node> insertEdge(Node from, Node to) {
        if (!mTable.containsKey(from)) {
            insertNode(from);
        }
        if (!mTable.containsKey(to)) {
            insertNode(to);
        }
        if (!mTable.get(from).contains(to))
            mTable.get(from).add(to);
        return this;
    }

    public AdjacencyList<Node> removeNode(Node n) {
        if (mTable.containsKey(n)) {
            mTable.remove(n);
            for (Node object : mTable.keySet()) {
                ArrayList<Node> list = mTable.get(object);
                list.remove(n);
            }
        }
        return this;
    }

    public ArrayList<Node> getNeighbors(Node n) {
        if (mTable.containsKey(n)) {
            return mTable.get(n);
        } else {
            return null;
        }
    }

    public boolean hasNode(Node n) {
        return mTable.containsKey(n);
    }
}
