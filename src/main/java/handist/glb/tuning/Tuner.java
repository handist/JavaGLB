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

import handist.glb.Configuration;
import handist.glb.GLBcomputer;
import handist.glb.PlaceLogger;

/**
 * Interface {@link Tuner} presents the method required for a dynamic parameter
 * tuning implementation of the {@link GLBcomputer}.
 * <p>
 * The various parameters presented in the {@link Configuration} class except
 * for the lifeline strategy can be modified during the computation by
 * implementations of this interface. Method
 * {@link #tune(PlaceLogger, Configuration, GLBcomputer)} will be called when
 * the time defined by {@value Configuration#GLBM_TUNING_INTERVAL} elapses to
 * provide an opportunity to modify the parameters currently in use by the
 * {@link GLBcomputer} class.
 *
 * @author Patrick Finnerty
 *
 */
public interface Tuner {

  /**
   * Method called when the place that this class is in charge of starts
   * computing. The {@link PlaceLogger} instance is provided to let the
   * {@link Tuner} record some information present in the logger (such as
   * timestamps or other information) at the time the place starting working
   * again.
   *
   * @param l
   *          the {@link PlaceLogger} instance which contains the runtime
   *          information about the place
   * @param c
   *          the {@link Configuration} instance used by the GLB
   * @return the timestamp at which the method was called using
   *         {@link System#nanoTime()}
   */
  public long placeLaunched(PlaceLogger l, Configuration c);

  /**
   * Method called when the parameters of the GLB should be tuned. The
   * {@link PlaceLogger} instance provides information about the runtime that
   * can be used to make decision. The available members of the
   * {@link Configuration} instance can be modified by this method. Those
   * modified values will be in use by the {@link GLBcomputer} as soon as they
   * are modified.
   * <p>
   * This method should return the result of a call to {@link System#nanoTime()}
   * made within the call to tune. This allows the GLB to block the thread that
   * runs the tuner until it is time to run the tuner again.
   *
   *
   * @param l
   *          contains information about the runtime of this place
   * @param c
   *          instance in which modifications to the parameters of the GLB can
   *          be registered
   * @param g
   *          local computer on which the tuner may act
   * @return the timestamp at which the method was called using
   *         {@link System#nanoTime()}
   */
  public long tune(PlaceLogger l, Configuration c, GLBcomputer g);

}
