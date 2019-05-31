/**
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
package handist.glb.multiworker;

import java.io.Serializable;

/**
 * Class {@link NoTuning} is the default class used for tuning of the
 * {@link GLBcomputer} parameters. It does not modify any parameter used in the
 * computation. The only impact it has consists in setting the elapsed time for
 * the tuner to be called again to the maximum possible value, effectively
 * delaying any call to this class {@link #tune(PlaceLogger, Configuration)}
 * method for as long as possible.
 *
 * @author Patrick Finnerty
 *
 */
public class NoTuning implements Tuner, Serializable {

  /** Serial Version UID */
  private static final long serialVersionUID = 1150561968739707398L;

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.multiworker.Tuner#placeLaunched(handist.glb.multiworker.
   * PlaceLogger)
   */
  @Override
  public long placeLaunched(PlaceLogger l, Configuration c) {
    return System.nanoTime();
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * handist.glb.multiworker.Tuner#tune(handist.glb.multiworker.PlaceLogger,
   * handist.glb.multiworker.GLBcomputer)
   */
  @Override
  public long tune(PlaceLogger l, Configuration c) {
    // Set the interval to its maximum possible value to avoid being called as
    // much as possible
    c.t = Long.MAX_VALUE;
    return System.nanoTime();
  }

}
