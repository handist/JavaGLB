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

/**
 * Class used to represent the choices that can be made at each level of the
 * exploration, each instance of this class representing one possible city to
 * visit.
 * <p>
 * In this implementation of the TSP, we use the nearest neighbor heuristic
 * throughout the computation, meaning that in hope of finding a better round
 * trip we attempt to visit the cities are close to the current location first.
 * For elegance, we therefore sort the candidates according to their cost.
 *
 * @author Patrick Finnerty
 *
 */
public class NextNode implements Serializable, Comparable<NextNode> {

    /** Serial Version UID */
    private static final long serialVersionUID = 575325357408222188L;

    /** Cost to get to {@link #node} from the current position */
    int cost;

    /** Id of the next city this instance represents */
    byte node;

    /**
     * Default constructor
     */
    public NextNode() {
    }

    /**
     * Copy constructor
     *
     * @param n
     *            the existing instance to be copied
     */
    public NextNode(NextNode n) {
        node = n.node;
        cost = n.cost;
    }

    /**
     * Compares this instance with the one given as parameter. The ordering is
     * made following the usual integer ordering on member {@link #cost}.
     */
    @Override
    public int compareTo(NextNode o) {
        return cost - o.cost;
    }

    /**
     * Sets both values of the {@link NextNode} class. To be meaningful, both
     * parameters must be positive or nil.
     *
     * @param n
     *            identifier of the city to get to
     * @param c
     *            cost to get to the node
     */
    public void set(byte n, int c) {
        node = n;
        cost = c;
    }

    @Override
    public String toString() {
        return node + "(" + cost + ")";
    }
}
