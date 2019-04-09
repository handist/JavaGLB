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
 * Package containing the multi-worker Global Load Balancer. Class
 * {@link handist.glb.multiworker.Bag} is the computation abstraction and
 * presents the requirements for a successful computation.
 * <p>
 * Class {@link handist.glb.multiworker.Configuration} holds the tuning
 * capabilities of the load balance algorithm offered to the programmer.
 * <p>
 * Class {@link handist.glb.multiworker.GLBcomputer} implements the algorithm
 * with all its routines.
 * <p>
 * Class {@link handist.glb.multiworker.GLBfactory} provides the factory methods
 * to be use to obtain the distrbuted computation service.
 * <p>
 * Finally, classes {@link handist.glb.multiworker.Logger} and
 * {@link handist.glb.multiworker.PlaceLogger} log the runtime information of
 * the distributed computation.
 *
 * @author Patrick Finnerty
 *
 */
package handist.glb.multiworker;