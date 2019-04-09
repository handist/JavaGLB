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
/**
 * Package containing some utilities common to all Load Balancers.
 * <p>
 * Class {@link handist.glb.util.Fold} is the result abstraction of the
 * distributed computation. The programmer will need to implement this
 * interface.
 * <p>
 * Class {@link handist.glb.util.LifelineStrategy} is the abstraction used to
 * describe the lifelines that places can establish on one another. Class
 * {@link handist.glb.util.HypercubeStrategy} is an implementation of that
 * interface used by default in the load balancers.
 * <p>
 * {@link handist.glb.util.SerializableSupplier} is an interface used to lift
 * some type inference difficulties with some Java compilers. The programmer
 * does not need to concern with that Functional interface.
 *
 * @author Patrick Finnerty
 *
 */
package handist.glb.util;