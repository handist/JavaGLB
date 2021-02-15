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
package handist.glb.tuning;

import java.io.Serializable;

import handist.glb.Configuration;
import handist.glb.GLBcomputer;
import handist.glb.PlaceLogger;

/**
 * Suggestion to use 10x smaller grain for host 0 compared to the other host
 * which use the provided value.
 *
 * @author Patrick
 *
 */
public class KamadaTuner implements Serializable, Tuner {

  /** Serial Version UID */
  private static final long serialVersionUID = 7940156771631748231L;

  @Override
  public long placeLaunched(PlaceLogger l, Configuration c) {
    final long stamp = System.nanoTime();
    c.t = 1000000000;
    if (l.place == 0) {
      c.n = c.originalN / 10;
    }
    l.NvalueTuned(stamp, c.n);
    return stamp;
  }

  @Override
  public long tune(PlaceLogger l, Configuration c, GLBcomputer g) {
    return System.nanoTime();
  }
}
