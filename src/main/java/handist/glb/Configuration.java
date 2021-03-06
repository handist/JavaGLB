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
package handist.glb;

import java.io.Serializable;

import handist.glb.tuning.Tuner;
import handist.glb.util.HypercubeStrategy;

/**
 * Configuration class holds the environment variables that can be used to tune
 * the multiworker Global Load Balancer. It also contains the settings that are
 * available for tuning on each place in the computation. An instance of this
 * class will also be used to carry the GLB configuration at the user's demand
 * when calling the {@link GLBcomputer#getConfiguration()} method.
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
 * -Doption_string=value
 * </pre>
 *
 * arguments to the java program. For instance, to launch a computation with 4
 * concurrent workers on each host and choosing your custom lifeline strategy,
 * you would use the following comand:
 *
 * <pre>
 * java -Dglb.workers=4 -Dglb.lifelinestrategy=com.custom.MyLifelineStrategy &lt;-- other arguments --&gt;
 * </pre>
 *
 * @author Patrick Finnerty
 */
public final class Configuration implements Serializable {

    /** Serial Version UID */
    private static final long serialVersionUID = 5247073810003721146L;

    /**
     * Number of concurrent workers to use for the multi-worker GLB on each
     * host. If not set, the value returned by
     * {@link Runtime#availableProcessors()} will be used.
     */
    public static final String GLBM_WORKERS = "glb.workers";

    /**
     * Size of the chunk of work performed by each worker without interruption
     * before checking the runtime and possibly performing load-balance
     * operations.
     */
    public static final String GLBM_WORKUNIT = "glb.workunit";

    /** Default value if {@link #GLBM_WORKUNIT} is not defined. */
    public static final String GLBM_DEFAULT_WORKUNIT = "511";

    /**
     * Property for the class to use as lifeline strategy. Classes should be
     * specified with their fully qualified domain name.
     */
    public static final String GLBM_LIFELINESTRATEGY = "glb.lifelinestrategy";

    /** Default class to use as lifeline strategy */
    public static final String GLBM_DEFAULT_LIFELINESTRATEGY = HypercubeStrategy.class
            .getCanonicalName();

    /**
     * Property to set for the number of steals made on random hosts to make
     * before stealing from the lifelines
     */
    public static final String GLBM_RANDOMSTEALS = "glb.randomsteals";

    /**
     * Default number of random steals made by the GLB before changing to the
     * lifeline steals
     */
    public static final String GLBM_DEFAULT_RANDOMSTEALS = "1";

    /**
     * Property to set the interval at which the tuner should be called by the
     * runtime to adjust the parameters during the computation. Should be a
     * whole number in nanoseconds.
     */
    public static final String GLBM_TUNING_INTERVAL = "glb.tuninginterval";

    /**
     * Default setting for property {@link #GLBM_TUNING_INTERVAL}: 0.1 second
     */
    public static final String GLBM_DEFAULT_TUNING_INTERVAL = "100000000";

    /**
     * Setting for the interval at which the information contained in the shared
     * object in one process is checked for propagation to neighboring
     * processes. Only relevant if calling
     * {@link GLBcomputer#computeWhisperedResult}.
     */
    public static final String GLBM_WHISPERING_INTERVAL = "glb.whisperinterval";

    /**
     * Property used to set the class which is going to be used to tune the
     * parameters of the GLB during execution. If not specified, no tuner will
     * be spawned.
     */
    public static final String GLBM_TUNERCLASS = "glb.tuner";

    /**
     * Default interval at which new information stored in the shared result
     * instance is whispered to other processes. Default value is 500ms.
     *
     * @see GLBcomputer#communicatorThread()
     */
    public static final String GLBM_DEFAULT_WHISPERING_INTERVAL = "500000000";

    /**
     * Period in nanoseconds at which the whisper mechanism may propagate
     * information between hosts during the computation.
     */
    public final long whisperInterval;

    /**
     * Original value set to {@link #n} as dictated by setting parameter
     * {@value #GLBM_WORKUNIT} at launch time. The actual value used during the
     * computation may be different if the tuning mechanism changes that value.
     */
    public final int originalN;

    /**
     * Original value set to {@link #t} as defined when setting property
     * {@value #GLBM_TUNING_INTERVAL}. The actual value used during the
     * computation may be different if the tuning mechanism changes that value.
     */
    public final long originalT;

    /**
     * Original value set to {@link #w} as defined by the user when setting
     * property {@value #GLBM_RANDOMSTEALS}. The actual value used during the
     * computation may be different if the tuning mechanism changes that value.
     */
    public final int originalW;

    /**
     * Work unit used on the computation. May be changed from its original value
     * by {@link Tuner} implementations. The actual value used during the
     * computation may be different if the tuning mechanism changes that value.
     */
    volatile public int n;

    /**
     * Number of places used in the computation. Is constant and cannot be
     * modified during the computation.
     */
    public final int p;

    /**
     * Number of nanoseconds required to elapse between two calls to the
     * parameter tuning method
     * {@link Tuner#tune(PlaceLogger, Configuration, GLBcomputer)}. This value
     * may be changed by the tuner.
     */
    public long t;

    /**
     * Class in charge of dynamically adjusting the parameters of the global
     * load balancer.
     */
    public final String tuner;

    /**
     * Number of workers used on the computation at each place. Is fixed and
     * cannot be changed during the computation. The number of concurrent
     * workers can be set using {@value #GLBM_WORKERS}. If not specified, the
     * value returned by {@link Runtime#availableProcessors()} will be used.
     */
    public final int x;

    /**
     * Number of random steals to perform by the GLB when it runs out of work
     */
    volatile public int w;

    /**
     * Class that provides the lifeline strategy used by the GLB
     */
    public final String z;

    /**
     * Resets the configuration parameters that may have been modified during a
     * computation by the {@link Tuner} implementation to their original values.
     */
    public void reset() {
        n = originalN;
        t = originalT;
        w = originalW;
    }

    /**
     * Returns the main configuration element written in a single line.
     *
     * @return String containing a printable message presenting the settings
     */
    @Override
    public String toString() {
        return "Places: " + p + " Workers per place: " + x
                + " Initial Work Unit: " + originalN
                + " Initial Random Steals: " + originalW
                + " Lifeline Strategy: " + z + " Tuner: " + tuner
                + " Initial Tuning Interval: " + originalT;
    }

    /**
     * Protected constructor to avoid instance creation from outside the
     * library.
     * <p>
     * This constructor is used by {@link GLBcomputer#getConfiguration()} when
     * returning an instance that contains all the information about the
     * conditions in which the GLB is set up.
     *
     * @param places
     *            number of places used in the computation
     * @param workers
     *            number of concurrent workers at each place
     * @param workUnit
     *            amount of work performed by each worker before checking the
     *            runtime
     * @param randomSteal
     *            maximum number of unsuccessful random steals before the place
     *            turns to the lifeline scheme to get some work
     * @param lifeline
     *            string of the class which provides the lifeline strategy to
     *            the GLB
     * @param tuningTimeout
     *            number of nanoseconds that need to elapse between two tuning
     *            of the GLB parameters
     * @param tunerClass
     *            fully qualified domain space of the class implementing the
     *            parameter tuning
     * @param whispererInterval
     *            interval at which the whisperer is called to possibly
     *            propagate the information contained in the local shared object
     *            to other processes
     */
    protected Configuration(int places, int workers, int workUnit,
            int randomSteal, String lifeline, long tuningTimeout,
            String tunerClass, long whispererInterval) {
        p = places;
        x = workers;
        n = workUnit;
        w = randomSteal;
        z = lifeline;
        t = tuningTimeout;
        originalN = n;
        originalT = tuningTimeout;
        originalW = w;
        tuner = tunerClass;
        whisperInterval = whispererInterval;
    }
}
