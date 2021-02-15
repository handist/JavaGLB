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
 * This is not a tuner as originally intended by this mechanism of the Global
 * Load Balancer. Instead of analyzing the logs of the computation in progress
 * to adjust some parameters, this class logs the number of times the intra- bag
 * has been emptied.
 *
 * The logging mechanism for the tuner is abused to log not the new value taken
 * by the grain size but rather the number of times the intra-bag of
 * {@link GLBcomputer} was emptied during the last tuning interval.
 *
 * @author Patrick Finnerty
 *
 */
public class EmptyQueuedLog implements Tuner, Serializable {

  /** Serial Version UID */
  private static final long serialVersionUID = -7136650908831277345L;
  /** Last value of {@link PlaceLogger#intraQueueEmptied} read by this class */
  long lastEmptyCount;

  @Override
  public long placeLaunched(PlaceLogger l, Configuration c) {
    final long stamp = System.nanoTime();
    lastEmptyCount = l.intraQueueEmptied.get();
    return stamp;
  }

  @Override
  public long tune(PlaceLogger l, Configuration c, GLBcomputer g) {
    final long stamp = System.nanoTime();
    final long newCount = l.intraQueueEmptied.get();
    l.NvalueTuned(stamp, (int) (newCount - lastEmptyCount));
    lastEmptyCount = newCount;
    return stamp;
  }

}
