/*
 *  Licensed to GraphHopper GmbH under one or more contributor
 *  license agreements. See the NOTICE file distributed with this work for
 *  additional information regarding copyright ownership.
 *
 *  GraphHopper GmbH licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except in
 *  compliance with the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
import com.graphhopper.routing.util.AllEdgesIterator;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.SPTEntry;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.Parameters;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.PriorityQueue;

/**
 * Implements a single source shortest path algorithm
 * http://en.wikipedia.org/wiki/Dijkstra's_algorithm
 * <p>
 *
 * @author Peter Karich
 */

import java.util.Arrays;

/*
 * The Floyd-Warshall algorithm is used to find the shortest path between
 * all pairs of nodes in a weighted graph with either positive or negative
 * edge weights but without negative edge cycles.
 *
 * The running time of the algorithm is O(n^3), being n the number of nodes in
 * the graph.
 *
 * This implementation is self contained and has no external dependencies. It
 * does not try to be a model of good Java OOP, but a simple self contained
 * implementation of the algorithm.
 */

import java.util.Arrays;

public class FloydWarshall {

    // graph represented by an adjacency matrix
    private double[][] graph;
    private double[][] nextgraph;


    private boolean negativeCycle;



    public FloydWarshall(int n) {
        this.graph = new double[n][n];
        this.nextgraph = new double[n][n];
        initGraph();
    }

    public void initGraph() {
        for (int i = 0; i < graph.length; i++) {
            for (int j = 0; j < graph.length; j++) {
                if (i == j) {
                    graph[i][j] = 0;
                } else {
                    graph[i][j] = Double.POSITIVE_INFINITY;
                }
                nextgraph[i][j] = -1;
            }
        }
    }

    public void setGraph(double adjacencymatrix[][]) {

        graph = adjacencymatrix;
        /*
        double fw_matrix[][] = adjacencymatrix;
        for(int i = 0; i < fw_matrix.length; i++){
            double [] row = fw_matrix[i];
            for(int j = 0; j < row.length; j++) {
                System.out.print(row[j] + ", ");
            }
            System.out.println("\n");
        }
        */
    }
    public void setNextGraph(double adjacencymatrix[][]) {

        nextgraph = adjacencymatrix;

    }

    public boolean hasNegativeCycle() {
        return this.negativeCycle;
    }

    // utility function to add edges to the adjacencyList
    public void addEdge(int from, int to, double weight) {
        graph[from][to] = weight;
    }

    public class FW_ret{
        double[][] distances;
        double[][] next;
    }

    // all-pairs shortest path
    public FW_ret floydWarshall() {
        double[][] distances;
        double[][] next;
        int n = graph.length;
        distances = Arrays.copyOf(graph, n);
        next = Arrays.copyOf(nextgraph, n);

        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    if(distances[i][j] > distances[i][k] + distances[k][j]) {
                        distances[i][j] = distances[i][k] + distances[k][j];
                        next[i][j] = next[i][k];
                        //next[i][k] = next[k][j];

                    }

                    //System.out.println(distances[i][j]);
                }
            }

            if (distances[k][k] < 0.0) {
                this.negativeCycle = true;
            }
        }
        /*
        for(int i = 0; i < distances.length; i++){
            double [] row = distances[i];
            for(int j = 0; j < row.length; j++) {
                System.out.print(row[j] + ", ");
            }
            System.out.println("\n");
        }
        */
        FW_ret fw = new FW_ret();
        fw.distances = distances;
        fw.next = next;
        return fw;
    }

    public static void main (String[] args){
        double adjacencymatrix[][] = new double[][]{
                {0, 4, 0, 0, 0, 0, 0, 8, 0},
                {4, 0, 8, 0, 0, 0, 0, 11, 0},
                {0, 8, 0, 7, 0, 4, 0, 0, 2},
                {0, 0, 7, 0, 9, 14, 0, 0, 0},
                {0, 0, 0, 9, 0, 10, 0, 0, 0},
                {0, 0, 4, 14, 10, 0, 2, 0, 0},
                {0, 0, 0, 0, 0, 2, 0, 1, 6},
                {8, 11, 0, 0, 0, 0, 1, 0, 7},
                {0, 0, 2, 0, 0, 0, 6, 7, 0}
        };


        FloydWarshall fw = new FloydWarshall(9);
        fw.setGraph(adjacencymatrix);
        fw.floydWarshall();
    }

}