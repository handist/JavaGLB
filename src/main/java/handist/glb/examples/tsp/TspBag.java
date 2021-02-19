/*******************************************************************************
 * This file is part of the Handy Tools for Distributed Computing project
 * HanDist (https:/github.com/handist)
 *
 * This file is licensed to You under the Eclipse Public License (EPL);
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 	https://www.opensource.org/licenses/eclipse-1.0.php
 *
 * (C) copyright CS29 Fine 2018-2021
 ******************************************************************************/
package handist.glb.examples.tsp;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;

import handist.glb.Bag;

/**
 * TSP implementation following the global load balancer's {@link Bag}
 * interface.
 *
 * @author Patrick Finnerty
 *
 */
public class TspBag implements Bag<TspBag, TspResult>, Serializable {

    /** Serial Version UID */
    private static final long serialVersionUID = -8573121302967870892L;

    /**
     * Launches a sequential execution of the TSP
     *
     * @param args
     *            path to the input problem file, (optionally) an integer to use
     *            only a subset of the problem given as first parameter
     */
    public static void main(String[] args) {
        TspProblem problem;
        try {
            if (args.length == 2) {
                problem = TspParser.parseFile(args[0],
                        Integer.parseInt(args[1]));
            } else if (args.length == 1) {
                problem = TspParser.parseFile(args[0]);
            } else {
                System.out.println("TSP computation");
                System.out.println(
                        "\t<PATH TO PROBLEM FILE> [subset of cities to use]");
                return;
            }
        } catch (final IOException e) {
            e.printStackTrace();
            return;
        }

        // Initialization
        final TspBag bag = new TspBag(problem);
        System.out.println(problem);
        bag.init();
        final TspResult result = new TspResult();
        // Computation
        long computationTime = System.nanoTime();
        bag.run(result);
        computationTime = System.nanoTime() - computationTime;
        // Print the results
        System.out.println(problem);
        System.out.println("Computation took " + computationTime / 1e9 + "s");
        System.out.println("Final result is " + result);
        System.out.println("Number of nodes explored: " + bag.nodeCounter);
    }

    /**
     * Adjacency matrix. Contains the information of how much going from city A
     * (first index) to city B (second index) costs.
     */
    public transient final int[][] ADJ_MATRIX;

    /**
     * Bound indicating the minimum length needed completing the route when one
     * has `N+1` hops left to go where N is the index in
     * {@link #BOUND_FUNCTION}.
     */
    public transient final int[] BOUND_FUNCTION;

    /**
     * Cost array, keeps track of the distance needed to reach the node located
     * at the same index in array {@link #path}.
     */
    int cost[];

    /**
     * Keeps track of the upper bound of the range of nodes reaming to explore
     * at each 'index' in array {@link #nextNodes}. Nodes remaining to explore
     * are at indexes strictly lower than what is contained by array
     * {@link #high}.
     */
    int high[];

    /**
     * Size of the path currently being explored. The information concerning the
     * last nodes being explored are therefore located at "index-1" in arrays
     * {@link #cost}, {@link #path} and {@link #nextNodes}.
     */
    int index;

    /**
     *
     * Keeps track of the lower range of nodes remaining to explore in the
     * {@link NextNode} at the same index. The next node to explore at depth 0
     * is nextNodes[0][low[0], provided low[0] $lt; high[0].
     */
    int low[];

    /**
     * At each depth, keeps track of the various options for the next city to
     * visit.
     */
    NextNode nextNodes[][];

    /**
     * Counts the number of nodes explored by this bag. This counter is
     * incremented in method {@link #exploreOne(TspResult)} every time it is
     * called.
     */
    transient long nodeCounter;

    /**
     * Path array, keeps track of which city the exploration is currently
     * located at. Its value at index 0 is always 0 as we arbitrarily decided
     * that all round trips start (and finish) at 0.
     */
    byte path[];

    /**
     * Queue in which partial exploration that have yet to be explored are
     * contained.
     */
    Deque<TspBag> reserve;

    /**
     * Number of cities in the considered problem
     */
    public transient final int TOTAL_NB_CITIES;

    /**
     * Constructor for the {@link TspBag} instance used to contain only the
     * queue of partial explorations.
     */
    protected TspBag() {
        BOUND_FUNCTION = null;
        ADJ_MATRIX = null;
        TOTAL_NB_CITIES = 0;
        reserve = new LinkedList<>();
    }

    /**
     * Constructor for TSPBag instance that are going to be used to transport
     * splits from one place to an other. The arrays will be initialized with
     * the minimum size needed and the transient terms will not be initialized
     *
     * @param nbCities
     *            of cities in the current TSP problem
     */
    protected TspBag(int nbCities) {
        // Initialization to avoid warning on non initialized final members
        ADJ_MATRIX = null;
        BOUND_FUNCTION = null;
        TOTAL_NB_CITIES = nbCities;

        index = 0;
        cost = new int[nbCities];
        path = new byte[nbCities];
        nextNodes = new NextNode[nbCities][0];
        for (int i = 0; i < nbCities; i++) {
            nextNodes[i] = new NextNode[nbCities - i - 1];
        }
        low = new int[nbCities];
        high = new int[nbCities];
    }

    /**
     * Initializes an empty bag with the given problem.
     *
     * @param problem
     *            the problem considered
     */
    public TspBag(TspProblem problem) {
        nodeCounter = 0;
        ADJ_MATRIX = problem.adjacencyMatrix;
        TOTAL_NB_CITIES = ADJ_MATRIX.length;
        BOUND_FUNCTION = problem.boundFunction;

        index = 0;
        cost = new int[TOTAL_NB_CITIES];
        path = new byte[TOTAL_NB_CITIES];
        nextNodes = new NextNode[TOTAL_NB_CITIES][0];
        for (int i = 0; i < TOTAL_NB_CITIES; i++) {
            nextNodes[i] = new NextNode[TOTAL_NB_CITIES - i - 1];
            for (int j = 0; j < nextNodes[i].length; j++) {
                nextNodes[i][j] = new NextNode();
            }
        }
        low = new int[TOTAL_NB_CITIES];
        high = new int[TOTAL_NB_CITIES];
        reserve = new LinkedList<>();
    }

    /**
     * Steps back in the exploration until an unexplored leaf is found or the
     * bag is found to be empty.
     */
    void backtrack() {
        while (0 < index && low[index - 1] == high[index - 1]) {
            index--;
        }
    }

    /**
     * Performs one step in the current exploration.
     *
     * @param shared
     *            the shared object between workers in which the optimum
     *            solution found so far is stored
     */
    void exploreOne(TspResult shared) {
        nodeCounter++;
        final NextNode candidates[] = nextNodes[index - 1]; // for elegance
                                                            // henceforth

        if (candidates.length == 0) {
            // There are no candidates, we are at the end of a path

            // We add the cost of the return to city 0
            final int totalCost = cost[index - 1]
                    + ADJ_MATRIX[path[index - 1]][0];
            if (totalCost <= shared.bestSolution) {
                final Byte[] pathFound = new Byte[TOTAL_NB_CITIES];
                for (int j = 0, i = index
                        - TOTAL_NB_CITIES; i < index; i++, j++) {
                    pathFound[j] = new Byte(path[i]);
                }
                // Arrays.copyOfRange(path, index - TOTAL_NB_CITIES, index);

                shared.updateBestSolution(totalCost, pathFound);
            }
            backtrack();
            return;
        }

        if (low[index - 1] == high[index - 1]) {
            // There were no unexplored candidates left, stepback
            backtrack();
            return;
        }

        final NextNode next = candidates[low[index - 1]]; // next is the
                                                          // candidate

        final int nextCost = cost[index - 1] + next.cost;
        if (nextCost + BOUND_FUNCTION[TOTAL_NB_CITIES
                - index] <= shared.bestSolution) {
            // We may find a better solution, we keep exploring with that node

            cost[index] = nextCost;
            path[index] = next.node;

            // We carry forward all the remaining candidates in the two
            // following for
            // loops
            int j = 0; // index for nextNodes[index] array
            for (int i = 0; i < low[index - 1]; i++) {
                final NextNode toExplore = candidates[i];
                nextNodes[index][j++].set(toExplore.node,
                        ADJ_MATRIX[next.node][toExplore.node]);
            }
            // The chosen candidate is skipped in the candidates array
            for (int i = low[index - 1]
                    + 1; i < nextNodes[index - 1].length; i++) {
                final NextNode toExplore = candidates[i];
                nextNodes[index][j++].set(toExplore.node,
                        ADJ_MATRIX[next.node][toExplore.node]);
            }

            Arrays.sort(nextNodes[index]); // Sort the array

            low[index - 1]++;// next will not be a candidate anymore
            low[index] = 0;
            high[index] = j;
            index++;
        } else {
            // No chance of finding a better solution, we backtrack
            low[index - 1] = high[index - 1];
            backtrack();
        }
    }

    /**
     * Prepares this instance to run the TSP exploration from the very start
     */
    public void init() {
        index = 1;
        path[0] = 0;
        cost[0] = 0;
        int j = 0;
        for (byte city = 1; city < TOTAL_NB_CITIES; city++, j++) {
            nextNodes[0][j].set(city, ADJ_MATRIX[0][city]);
        }
        Arrays.sort(nextNodes[0]);
        low[0] = 0;
        high[0] = nextNodes[0].length;
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glbm.Bag#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return index <= 0 && reserve.isEmpty();
    }

    /*
     * (non-Javadoc)
     *
     * @see handist.glb.multiworker.Bag#isSplittable()
     */
    @Override
    public boolean isSplittable() {
        return reserve.size() > 1 || treeSplittable();
    }

    /**
     * Counts the number of leaves left unexplored in the exploration tree at
     * the current level
     *
     * @return the number of leaves in the tree
     */
    public int leaves() {
        int leafCount = 0;
        for (int i = 0; i < index; i++) {
            leafCount += high[i] - low[i];
        }
        return leafCount;
    }

    /**
     * Merging is done by stitching the incoming TSPBag instance arrays to the
     * {@link #cost}, {@link #path} and {@link #nextNodes} arrays starting from
     * index {@link #index} onwards. The size of the three arrays will be
     * increased if necessary.
     */
    @Override
    public void merge(TspBag b) {
        if (index <= 0) {
            restore(b.reserve.poll());
        }

        reserve.addAll(b.reserve);
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glbm.Bag#process(int, apgas.glb.Fold)
     */
    @Override
    public void process(int workAmount, TspResult sharedObject) {
        while (0 < workAmount && !isEmpty()) {
            if (index <= 0) {
                restore(reserve.pop());
            } else {
                exploreOne(sharedObject);
            }
            workAmount--;
        }
    }

    /**
     * Restores a partial exploration given as parameter into the current
     * instance.
     *
     * @param exploration
     *            partial exploration to be continued in the current instance
     */
    private void restore(TspBag exploration) {
        cost = exploration.cost;
        high = exploration.high;
        index = exploration.index;
        low = exploration.low;
        nextNodes = exploration.nextNodes;
        path = exploration.path;
    }

    /**
     * Computes the whole TSP sequentially
     *
     * @param result
     *            the instance into which the result is going to be stored
     */
    public void run(final TspResult result) {
        while (!isEmpty()) {
            process(Integer.MAX_VALUE, result);
        }
    }

    /**
     * The splitting strategy consists in giving away half of the work reserve
     * if it contains anything. If the work reserve is empty, the partial
     * exploration contained by this instance is split.
     *
     * @see #splitTheReserve()
     * @see #splitCurrentExploration(boolean)
     */
    @Override
    public TspBag split(boolean takeAll) {
        if (reserve.size() > 1
                || (reserve.size() == 1 && (treeSplittable() || takeAll))) {
            return splitTheReserve();
        } else {
            return splitCurrentExploration(takeAll);
        }
    }

    /**
     * Split the current exploration and returns a new TspBag instance
     * containing the work
     *
     * @param takeAll
     *            In the case the exploration cannot be split, indicates if all
     *            the instance contents should be given away.
     * @return a new TspBag instance containing some work
     */
    public TspBag splitCurrentExploration(boolean takeAll) {
        if (index <= 0 || (!isSplittable() && !takeAll) || isEmpty()) {
            return new TspBag();
        }

        final TspBag toReturn = new TspBag(TOTAL_NB_CITIES);

        boolean alternator = true;
        for (int i = 0; i < TOTAL_NB_CITIES; i++) {
            // Copy cost, path and nextNodes arrays
            toReturn.path[i] = path[i];
            toReturn.cost[i] = cost[i];
            for (int k = 0; k < toReturn.nextNodes[i].length; k++) {
                toReturn.nextNodes[i][k] = new NextNode(nextNodes[i][k]);
            }

            // Split is made with arrays low and high
            final int leavesCount = high[i] - low[i];
            // middle index of the remaining candidates is picked
            int splitIndex = (high[i] + low[i]) / 2;
            if (leavesCount % 2 == 1) {
                if (alternator) {
                    splitIndex++;
                }
                alternator = !alternator;
            }

            toReturn.high[i] = splitIndex;
            toReturn.low[i] = low[i];
            low[i] = splitIndex;
        }
        toReturn.index = index;
        toReturn.backtrack();
        backtrack();

        final TspBag transport = new TspBag();
        transport.reserve.add(toReturn);
        return transport;
    }

    /**
     * Split work from this instance by giving away half the content of the
     * partial exploration reserve
     *
     * @return a TspBag instance containing
     */
    public TspBag splitTheReserve() {
        final TspBag toReturn = new TspBag();
        int toSteal = (reserve.size() + 1) / 2;
        while (toSteal > 0) {
            toReturn.reserve.add(reserve.poll());
            toSteal--;
        }
        return toReturn;
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glbm.Bag#submit(apgas.glb.Fold)
     */
    @Override
    public void submit(TspResult r) {
        r.nodesExplored += nodeCounter;
    }

    /**
     * A TSP exploration is considered to be splittable if there are at least 2
     * unexplored leaves in it.
     *
     * @return {@code true} if the current partial exploration contains enough
     *         leaves to be split
     */
    public boolean treeSplittable() {
        int leafCount = 0;
        for (int i = 0; i < index; i++) {
            leafCount += (high[i] - low[i]);
            if (2 <= leafCount) {
                return true;
            }

        }
        return false;
    }

}
