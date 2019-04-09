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
 * Factory class used to provide computation service instances to the programer.
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
   *           {@link Configuration#APGAS_GLBM_LIFELINESTRATEGY} could not be
   *           used
   */
  public static GLBcomputer setupGLB() throws ReflectiveOperationException {
    final int workUnit = Integer
        .parseInt(System.getProperty(Configuration.APGAS_GLBM_WORKUNIT,
            Configuration.APGAS_GLBM_DEFAULT_WORKUNIT));

    final int randomSteals = Integer
        .parseInt(System.getProperty(Configuration.APGAS_GLBM_RANDOMSTEALS,
            Configuration.APGAS_GLBM_DEFAULT_RANDOMSTEALS));

    LifelineStrategy s = null;
    try {
      final String lifelineStrategy = System.getProperty(
          Configuration.APGAS_GLBM_LIFELINESTRATEGY,
          Configuration.APGAS_GLBM_DEFAULT_LIFELINESTRATEGY);
      s = (LifelineStrategy) Class.forName(lifelineStrategy).newInstance();
    } catch (InstantiationException | IllegalAccessException
        | ClassNotFoundException e) {
      throw (e);
    }
    final LifelineStrategy strategy = s;

    final int nbWorkers = Integer
        .parseInt(System.getProperty(Configuration.APGAS_GLBM_WORKERS,
            String.valueOf(Runtime.getRuntime().availableProcessors())));

    return PlaceLocalObject.make(places(),
        () -> new GLBcomputer(workUnit, randomSteals, strategy, nbWorkers));
  }
}
