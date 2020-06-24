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
 * Package containing the multi-worker Global Load Balancer. The main components
 * of the library are present in the following classes:
 * <ul>
 * <li>Class {@link handist.glb.Bag} is the computation abstraction and presents
 * the requirements for a successful computation.
 * <li>Class {@link handist.glb.Configuration} holds the tuning capabilities of
 * the load balance algorithm offered to the programmer.
 * <li>Class {@link handist.glb.GLBcomputer} implements the work stealing
 * scheme.
 * <li>Class {@link handist.glb.GLBfactory} provides the factory methods to
 * setup the distributed computation service.
 * <li>Classes {@link handist.glb.Logger} and {@link handist.glb.PlaceLogger}
 * log the runtime information of the distributed computation.
 * </ul>
 *
 * Some additional mechanism are also integrated with the library. Interface
 * {@link handist.glb.Whisperer} allows the programmer to propagate information
 * between the hosts. It is also possible for the programmer to dynamically
 * change some parameters of the global load balancer by implementing the
 * {@link handist.glb.tuning.Tuner} interface. More details about this last
 * feature can be found in package {@link handist.glb.tuning}.
 *
 * @author Patrick Finnerty
 *
 */
package handist.glb;