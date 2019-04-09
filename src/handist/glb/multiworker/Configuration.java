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

/**
 * Configuration class holds the environment variables that can be used to tune
 * the multiworker Global Load Balancer. An instance of this class will also be
 * used to carry the GLB configuration on the user's demand with the
 * {@link GLBcomputer#getConfiguration()} method.
 *
 * Parameters that can be adjusted for the GLB include:
 * <ul>
 * <li>The number of concurrent workers on each place (default the number
 * returned by {@link Runtime#availableProcessors()})</li>
 * <li>The size of the chunk of tasks to be performed by workers (default
 * {@code 511})</li>
 * <li>The class responsible for providing the lifeline strategy (default
 * {@link handist.glb.util.HypercubeStrategy})</li>
 * <li>The number of random steals performed by a place that runs out of work
 * before using hte lifeline stealing scheme (default {@code 1})
 * </ul>
 *
 * Those settings can be set using the command line when launching the program
 * by adding
 *
 * <pre>
 * -Doption=value
 * </pre>
 *
 * arguments to the java program. For instance, to launch a computation with 4
 * concurrent workers on each host and choosing your custom lifeline strategy,
 * you would type the following :
 *
 * <pre>
 * java -Dglb.workers=4 -Dglb.lifelinestrategy=com.custom.MyLifelineStrategy &lt;-- other arguments --&gt;
 * </pre>
 *
 * @author Patrick Finnerty
 *
 *
 *
 */
public final class Configuration {

  /**
   * Number of concurrent workers to use for the multi-worker GLB on each host.
   * If not set, the value returned by {@link Runtime#availableProcessors()}
   * will be used.
   */
  public static final String APGAS_GLBM_WORKERS = "glb.workers";

  /**
   * Size of the chunk of work performed by each worker without interruption
   * before checking the runtime and possibly performing load-balance
   * operations.
   */
  public static final String APGAS_GLBM_WORKUNIT = "glb.workunit";

  /** Default value if {@link #APGAS_GLBM_WORKUNIT} is not defined. */
  public static final String APGAS_GLBM_DEFAULT_WORKUNIT = "511";

  /**
   * Property for the class to use as lifeline strategy. Classes should be
   * specified with their fully qualified domain name.
   */
  public static final String APGAS_GLBM_LIFELINESTRATEGY = "glb.lifelinestrategy";

  /** Default class to use as lifeline strategy */
  public static final String APGAS_GLBM_DEFAULT_LIFELINESTRATEGY = "fine.glb.util.HypercubeStrategy";

  /**
   * Property to set for the number of steals made on random hosts to make
   * before stealing from the lifelines
   */
  public static final String APGAS_GLBM_RANDOMSTEALS = "glb.randomsteals";

  /**
   * Default number of random steals made by the GLB before changing to the
   * lifeline steals
   */
  public static final String APGAS_GLBM_DEFAULT_RANDOMSTEALS = "1";

  /**
   * Work unit used on the computation
   */
  public int n;

  /**
   * Number of places used in the computation
   */
  public int p;

  /**
   * Number of workers used on the computation at each place
   */
  public int x;

  /**
   * Number of random steals to perform by the GLB when it runs out of work
   */
  public int w;

  /**
   * Class that provides the lifeline strategy used by the GLB
   */
  public String z;

  /**
   * Protected constructor to avoid instance creation from outside the library.
   * <p>
   * This constructor is used by {@link GLBcomputer#getConfiguration()} when
   * returning an instance that contains all the information about the
   * conditions in which the GLB is set up.
   *
   * @param places
   *          number of places used in the computation
   * @param workers
   *          number of concurrent workers at each place
   * @param workUnit
   *          amount of work performed by each worker before checking the
   *          runtime
   * @param randomSteal
   *          maximum number of unsuccessful random steals before the place
   *          turns to the lifeline scheme to get some work
   * @param lifeline
   *          string of the class which provides the lifeline strategy to the
   *          GLB
   */
  protected Configuration(int places, int workers, int workUnit,
      int randomSteal, String lifeline) {
    p = places;
    x = workers;
    n = workUnit;
    w = randomSteal;
    z = lifeline;
  }
}
