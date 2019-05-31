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
package handist.glb.multiworker;

import static apgas.Constructs.*;

import apgas.util.PlaceLocalObject;
import handist.glb.util.LifelineStrategy;

/**
 * Factory class used to provide computation service instances to the
 * programmer.
 *
 * @author Patrick Finnerty
 *
 */
public final class GLBfactory {

  /**
   * Sets up GLBcomputer instances at each place and returns an instance to
   * which computation can be submitted.
   *
   * @return computing service instance
   * @throws ReflectiveOperationException
   *           if the class to be used for
   *           {@link Configuration#GLBM_LIFELINESTRATEGY} could not be used
   */
  public static GLBcomputer setupGLB() throws ReflectiveOperationException {
    final int workUnit = Integer.parseInt(System.getProperty(
        Configuration.GLBM_WORKUNIT, Configuration.GLBM_DEFAULT_WORKUNIT));

    final int randomSteals = Integer
        .parseInt(System.getProperty(Configuration.GLBM_RANDOMSTEALS,
            Configuration.GLBM_DEFAULT_RANDOMSTEALS));

    LifelineStrategy s = null;
    try {
      final String lifelineStrategy = System.getProperty(
          Configuration.GLBM_LIFELINESTRATEGY,
          Configuration.GLBM_DEFAULT_LIFELINESTRATEGY);
      s = (LifelineStrategy) Class.forName(lifelineStrategy).newInstance();
    } catch (InstantiationException | IllegalAccessException
        | ClassNotFoundException e) {
      throw (e);
    }
    final LifelineStrategy strategy = s;

    final int nbWorkers = Integer
        .parseInt(System.getProperty(Configuration.GLBM_WORKERS,
            String.valueOf(Runtime.getRuntime().availableProcessors())));

    final long tuningInterval = Long
        .parseLong(System.getProperty(Configuration.GLBM_TUNING_INTERVAL,
            Configuration.GLBM_DEFAULT_TUNING_INTERVAL));

    Tuner t = null;
    try {
      final String tuner = System.getProperty(Configuration.GLBM_TUNERCLASS,
          Configuration.GLBM_DEFAULT_TUNERCLASS);
      t = (Tuner) Class.forName(tuner).newInstance();
    } catch (InstantiationException | IllegalAccessException
        | ClassNotFoundException e) {
      throw (e);
    }
    final Tuner tuner = t;

    return PlaceLocalObject.make(places(), () -> new GLBcomputer(workUnit,
        randomSteals, strategy, nbWorkers, tuningInterval, tuner));
  }
}
