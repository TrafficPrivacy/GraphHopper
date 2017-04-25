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

import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.weighting.Weighting;
import com.graphhopper.storage.Graph;
import com.graphhopper.storage.SPTEntry;
import com.graphhopper.util.EdgeExplorer;
import com.graphhopper.util.EdgeIterator;
import com.graphhopper.util.Parameters;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.Scanner;


import java.util.PriorityQueue;

/**
 * Implements a single source shortest path algorithm
 * http://en.wikipedia.org/wiki/Dijkstra's_algorithm
 * <p>
 *
 * @author Peter Karich
 */


public class BellmanFord {
    private double distances[];
    private double predecessor[];

    private int numberofvertices;
    public static final int MAX_VALUE = 99999;

    public BellmanFord(int numberofvertices) {
        this.numberofvertices = numberofvertices;
        distances = new double[numberofvertices];
        predecessor = new double[numberofvertices];

    }

    public class BF_ret{
        double[] distances;
        double[] predecessors;
    }

    public BF_ret BellmanFordEvaluation(int source, double adjacencymatrix[][]) {
        for (int node = 0; node < numberofvertices; node++) {
            distances[node] = MAX_VALUE;
            predecessor[node] = -1;
        }

        distances[source] = 0;
        for (int node = 1; node <= numberofvertices - 1; node++) {
            for (int sourcenode = 0; sourcenode < numberofvertices; sourcenode++) {
                for (int destinationnode = 0; destinationnode < numberofvertices; destinationnode++) {
                    if (adjacencymatrix[sourcenode][destinationnode] != MAX_VALUE) {
                        if (distances[destinationnode] > distances[sourcenode] + adjacencymatrix[sourcenode][destinationnode]) {
                            distances[destinationnode] = distances[sourcenode]
                                    + adjacencymatrix[sourcenode][destinationnode];
                            //if (destinationnode != sourcenode) {
                            predecessor[destinationnode] = sourcenode;

                        }
                            //System.out.println("Predecessor of " + destinationnode + " is " + sourcenode);
                            //edge is already traversed

                    }
                }
            }
        }

        for (int sourcenode = 0; sourcenode < numberofvertices; sourcenode++) {
            for (int destinationnode = 0; destinationnode < numberofvertices; destinationnode++) {
                if (adjacencymatrix[sourcenode][destinationnode] != MAX_VALUE) {
                    if (distances[destinationnode] > distances[sourcenode]
                            + adjacencymatrix[sourcenode][destinationnode])
                        System.out.println("The Graph contains negative egde cycle");
                }
            }
        }

        BF_ret bf_ret = new BF_ret();
        bf_ret.distances = distances;
        bf_ret.predecessors = predecessor;
        return bf_ret;

    }

    public static void main (String[] args){

        int numberofvertices = 6;
        //int source = 2;

        double adjacencymatrix[][] = new double[][]{
                {0, 4, 99999, 99999,-1, 99999},
                {99999, 0,-1, 99999,-2, 99999},
                {99999, 99999, 0, 99999, 99999, 99999},
                {99999, 99999, 99999, 0, 99999, 99999},
                {99999, 99999, 99999,-5, 0, 3},
                {99999, 99999, 99999, 99999, 99999, 0}
        };

        /*
        int numberofvertices = 8;
        //int source = 2;

        double adjacencymatrix[][] = new double[][]{
                {0, 4, 99999, 99999, 99999, 99999, 99999, 99999, 99999},
                {4, 0, 8, 99999, 99999, 99999, 99999, 99999, 99999},
                {99999, 8, 0, 7, 0, 4, 99999, 99999, 99999},
                {99999, 99999, 7, 0, 9, 14, 99999, 99999, 99999},
                {99999, 99999, 0, 9, 0, 10, 99999, 99999, 99999},
                {99999, 99999, 4, 14, 10, 0, 2, 99999, 99999},
                {99999, 99999, 99999, 99999, 99999, 2, 0, 1, 6},
                {99999, 99999, 99999, 99999, 99999, 99999, 1, 0, 7},
                {99999, 99999, 99999, 99999, 99999, 99999, 6, 7, 0}
        };
        */


        System.out.println("Enter the number of vertices");

        int source = 0;

        BellmanFord bellmanford = new BellmanFord(numberofvertices);
        BellmanFord.BF_ret bf_ret = bellmanford.BellmanFordEvaluation(source, adjacencymatrix);
        double [] distances = bf_ret.distances;
        double [] predecessors = bf_ret.predecessors;

        for (int vertex = 0; vertex < distances.length; vertex++) {
            System.out.println("distance of source  " + source + " to "
                    + vertex + " is " + distances[vertex]);
        }
        int dest = numberofvertices-1;

        int counter = 0;
        for (int vertex = 0; vertex < predecessors.length; vertex++) {
            System.out.println("predecessor of vertex  " + vertex + " is " + predecessors[vertex]);
        }
    }
}


 /*
        double adjacencymatrix[][] = new double[][]{
                {999, 999, 999, 999, 999, 999, 999},
                {999, 0, 4, 999, 999,-1, 999},
                {999, 999, 0,-1, 999,-2, 999},
                {999, 999, 999, 0, 999, 999, 999},
                {999, 999, 999, 999, 0, 999, 999},
                {999, 999, 999, 999,-5, 0, 3},
                {999, 999, 999, 999, 999, 999, 0}
        };
        */
