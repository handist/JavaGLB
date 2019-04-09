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
package handist.glb.legacy;

import static apgas.Constructs.*;

import java.io.Serializable;

import apgas.Configuration;
import apgas.util.PlaceLocalObject;
import handist.glb.util.LifelineStrategy;

/**
 * Factory class for {@link GLBProcessor}s. Sets up GLBProcessors instances to
 * make them readily available for computation before returning them to the
 * user.
 * <p>
 * The distributed nature of the computation implies some amount of preparation
 * before the computation van begin. More specifically, a distributed object
 * needs to be setup, with an instance of the object present on each of the
 * places. To avoid possible misuse by the user of the library, we provide a
 * factory method for the kind of global load balancer they want to use in their
 * distributed computation.
 *
 * @author Patrick Finnerty
 *
 */
public class GLBProcessorFactory {

  /**
   * Default number of places on which the computation is going to take place
   */
  public static final String DEFAULT_PLACE_COUNT = "4";

  /**
   * Creates a LoopGLBProcessor (factory method)
   * <p>
   * The returned LoopGLBProcessor will follow the provided configuration i.e. :
   * <ul>
   * <li>The amount of work to be processed by {@link Bag#process(int)} before
   * dealing with potential thieves
   * <li>The number of random steal attempts performed before turning to the the
   * lifeline-steal scheme.
   * </ul>
   *
   *
   * @param workUnit
   *          work amount processed by a place before dealing with thieves,
   *          <em>strictly positive</em>
   * @param stealAttempts
   *          number of steal attempts performed by a place before halting,
   *          <em>positive or nil</em>
   * @return a new computing instance
   */
  public static GLBProcessor LoopGLBProcessor(int workUnit, int stealAttempts) {
    if (System.getProperty(Configuration.APGAS_PLACES) == null) {
      System.setProperty(Configuration.APGAS_PLACES, DEFAULT_PLACE_COUNT);
    }

    final LoopGLBProcessor glb = PlaceLocalObject.make(places(),
        () -> new LoopGLBProcessor(workUnit, stealAttempts));
    return glb;
  }

  /**
   * Creates a generic GLBProcessor following the given workUnit stealAttempts
   * and lifeline strategy provided.
   *
   * @param <S>
   *          serializable lifeline strategy type
   * @param workUnit
   *          amount of work to process before distributing work
   * @param stealAttempts
   *          number of random steals attempted before resulting to the lifeline
   *          scheme
   * @param strategy
   *          the lifelines strategy to be used in this GLBProcessor instance
   * @return a new computing instance
   */
  public static <S extends LifelineStrategy & Serializable> GLBProcessor GLBProcessor(
      int workUnit, int stealAttempts, S strategy) {
    if (System.getProperty(Configuration.APGAS_PLACES) == null) {
      System.setProperty(Configuration.APGAS_PLACES, DEFAULT_PLACE_COUNT);
    }

    final GLBProcessor glb = PlaceLocalObject.make(places(),
        () -> new GenericGLBProcessor(workUnit, stealAttempts, strategy));
    return glb;
  }
}
