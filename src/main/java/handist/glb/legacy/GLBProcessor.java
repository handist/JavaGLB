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
package handist.glb.legacy;

import java.io.Serializable;

import handist.glb.util.Fold;

/**
 * Computing service abstraction used as an indirection layer with the two
 * implementing classes that exist in this package. Provides two methods to the
 * programmer. Method <em>compute</em> provides a distributed computation
 * service. Method {@link #getLogger()} provides information about the
 * performance of the last computation that took place.
 * <p>
 * The work that can be handled by the GLBProcessor is an implementation of
 * interface {@link Bag}. More information about the restrictions on the kind of
 * computation that can be handled by the {@link GLBProcessor} can be found the
 * {@link Bag} class description.
 *
 * @author Patrick Finnerty
 *
 */
public interface GLBProcessor {

    /**
     * Computes the computation given as parameter {@code bag} and returns the
     * result in an instance of the provided second parameter {@code result}.
     * <p>
     * The {@link GLBProcessor} will handle the work splitting and distribution
     * as well as the load-balancing operations that need take place during the
     * computation.
     *
     * @param <R>
     *            result type of the computation
     * @param <B>
     *            initial bag type, producing result of type <em>R</em>
     *
     * @param bag
     *            {@link Bag} to be processed
     * @param result
     *            neutral element instance of the result produced by the
     *            computation
     * @return computation result
     */
    public <R extends Fold<R> & Serializable, B extends Bag<B, R> & Serializable> R compute(
            B bag, R result);

    /**
     * Gives back the object which stores all the information about the runtime
     * of the last computation performed by the Global Load Balancer.
     *
     * @return array of {@link Logger}, one instance per place.
     * @see Logger
     */
    public Logger[] getLogger();
}
