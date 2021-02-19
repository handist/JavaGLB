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

import java.io.Serializable;
import java.util.LinkedList;

import handist.glb.util.Fold;

/**
 * Class containing the result produced by the TSP. It is also used during the
 * computation to share the best solution found so far between the workers on
 * the same host.
 *
 * @author Patrick Finnerty
 *
 */
public class TspResult implements Fold<TspResult>, Serializable {

    /** Serial Version UID */
    private static final long serialVersionUID = 8446727496903964353L;

    /**
     * Cost of the best solution found so far.
     */
    public volatile int bestSolution;

    /**
     * All the paths found to have the same "minimum" length.
     */
    public LinkedList<Byte[]> paths;

    /**
     * Tracks if a smaller value of {@link #bestSolution} was found since the
     * last time method {@link #getInformation()} was called.
     */
    private boolean newValueToShare;

    /**
     * Counts the number of nodes in the exploration trasversed by workers
     */
    public long nodesExplored;

    /*
     * (non-Javadoc)
     *
     * @see handist.glb.util.Fold#fold(handist.glb.util.Fold)
     */
    @Override
    public void fold(TspResult r) {
        if (bestSolution == r.bestSolution) {
            paths.addAll(r.paths);
        } else if (r.bestSolution < bestSolution) {
            bestSolution = r.bestSolution;
            paths = r.paths;
        }
        nodesExplored += r.nodesExplored;
    }

    /**
     * Indicates if this instance contains a better bound then the value held
     * the least time method {@link #getInformation()} was called.
     *
     * @return {@code true} if as better bound value was found, {@code false}
     *         otherwise
     */
    boolean hasValueToShare() {
        return newValueToShare;
    }

    /**
     * Returns the bound contained by this instance. In practice, is only called
     * after a call to method {@link #hasValueToShare()} returned true.
     *
     * @return The bound contained by this instance
     */
    public synchronized Integer getInformation() {
        newValueToShare = false;
        return new Integer(bestSolution);
    }

    /**
     * If the provided parameter has a bound value lower than the value
     * currently held by {@link #bestSolution}, updates {@link #bestSolution}
     * with this new value. Otherwise, does not do anything.
     *
     * @param info
     *            value of the bound coming from a remote process
     */
    synchronized void integrateInformation(Integer info) {
        if (info < bestSolution) {
            bestSolution = info;
            paths.clear();
            newValueToShare = true;
        }
    }

    /**
     * Allows workers to place a new solution into the shared data structure.
     *
     * @param length
     *            length of the proposed solution
     * @param path
     *            array representing the path between the cities
     */
    synchronized void updateBestSolution(int length, Byte[] path) {
        if (length < bestSolution) {
            bestSolution = length;
            paths.clear();
            paths.add(path);
            newValueToShare = true;
        } else if (length == bestSolution) {
            paths.add(path);
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String s = "Length;" + bestSolution + ";PathsCount;" + paths.size()
                + ";";
        for (final Byte[] path : paths) {
            // final byte[] path = bestPaths[i];
            s += "\r\n";
            for (final Byte b : path) {

                s += b + " ";
            }
        }
        s += "\r\nNodesExplored;" + nodesExplored + ";";
        return s;
    }

    /**
     * Default constructor
     */
    public TspResult() {
        bestSolution = Integer.MAX_VALUE;
        paths = new LinkedList<>();
        newValueToShare = false;
        nodesExplored = 0;
    }

}
