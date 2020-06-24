/*
 *  This file is part of the Handy Tools for Distributed Computing project
 *  HanDist (https://github.com/handist)
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) copyright CS29 Fine 2018-2019.
 */
package handist.glb.util;

/**
 * Work stealing preferred channel for a GLBProcessor. When a place runs out of
 * work, the {@link LifelineStrategy} implementation determines which places the
 * thief passively steals work from. Those places are the called the
 * 'lifelines'.
 * <p>
 * To be valid, a {@link LifelineStrategy} needs to satisfy several properties
 * that are easily explained in terms of graphs.
 * <p>
 * Consider the oriented graph whose vertices are the places of the system and
 * where an edge from vertex {@code A} to {@code B} means that {@code A} has a
 * lifeline on {@code B} (A will steal from B). A valid {@link LifelineStrategy}
 * consists in a connected graph, i.e. there must be a path (in one or several
 * jumps) from each place to every other place. If this is not the case, some of
 * the places could starve since they could enter a state in which they will not
 * able to steal any work, defeating the purpose of the load balancer.
 * <p>
 * One implementation of this interface is provided in the library and used as
 * the default: {@link LifelineStrategy}.
 *
 * @author Patrick Finnerty
 *
 */
public interface LifelineStrategy {

  /**
   * Gives the list of nodes that place {@code thief} can steal work from.
   *
   * @param thief
   *          id of the place stealing work
   * @param nbPlaces
   *          number of places in the system
   * @return array containing the ids of the places place {@code thief} should
   *         steal from
   */
  public int[] lifeline(int thief, int nbPlaces);

  /**
   * Gives the list of places that can steal work from place {@code target}.
   *
   * @param target
   *          id of the place victim of steals
   * @param nbPlaces
   *          number of places in the system
   * @return array containing the ids of the places that can steal work from
   *         place {@code target}
   */
  public int[] reverseLifeline(int target, int nbPlaces);
}
