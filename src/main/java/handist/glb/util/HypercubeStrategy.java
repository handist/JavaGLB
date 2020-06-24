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

import java.io.Serializable;

/**
 * Lifeline strategy implementing an hypercube lifeline strategy among places.
 * <p>
 * A lifeline between two places exists if the edit distance between two place's
 * id's written in binary is 1.
 *
 * @author Patrick Finnerty
 *
 */
public class HypercubeStrategy implements LifelineStrategy, Serializable {

  /** Serial Version UID */
  private static final long serialVersionUID = 5106410194659222967L;

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.LifelineStrategy#lifeline(int, int)
   */
  @Override
  public int[] lifeline(final int home, final int nbPlaces) {
    int count = 0;
    int mask = 1;
    int l;
    while ((l = home ^ mask) < nbPlaces) {
      count++;
      mask *= 2;
    }

    final int toReturn[] = new int[count];

    mask = 1;
    int index = 0;
    while ((l = home ^ mask) < nbPlaces) {
      toReturn[index] = l;
      index++;
      mask *= 2;
    }

    return toReturn;
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.LifelineStrategy#reverseLifeline(int, int)
   */
  @Override
  public int[] reverseLifeline(int target, int nbPlaces) {
    return lifeline(target, nbPlaces);
  }

}
