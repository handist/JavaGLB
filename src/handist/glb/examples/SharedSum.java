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
package handist.glb.examples;

import java.io.Serializable;

import handist.glb.util.Fold;

/**
 *
 * Implementation of the {@link Fold} interface that performs the addition on
 * {@code long} integers. The class also implements interface
 * {@link Serializable} in order to be used by the GLB library.
 *
 * @author Patrick Finnerty
 *
 */
public class SharedSum implements Fold<SharedSum>, Serializable {

  /** Serial Version UID */
  private static final long serialVersionUID = 3582168956043482749L;

  /** Integer in which the sum is performed */
  long sum;

  /**
   * Returns the current value of the {@link SharedSum} instance. Method is
   * protected against concurrent accesses and/or modifications done through
   * method {@link #add(long)}.
   *
   * @return the value held by this instance at the time of calling as a long
   *         integer
   */
  public synchronized long get() {
    return sum;
  }

  /**
   * Adds the value of the provided parameter to the value held by this
   * instance. Method is protected against concurrent accesses by avoid improper
   * read and writes.
   *
   * @param l
   *          the value to add to the current instance
   */
  public synchronized void add(long l) {
    sum += l;
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.Fold#fold(apgas.glb.Fold)
   */
  @Override
  public void fold(SharedSum f) {
    add(f.get());
  }

  /**
   * Constructor
   *
   * @param s
   *          initial value for the sum
   */
  public SharedSum(long s) {
    sum = s;
  }

}
