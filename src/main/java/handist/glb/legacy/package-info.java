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
/**
 * Package containing the 'legacy' Global Load Balancer design of the original
 * X10 implementation.
 * <p>
 * The global load balancer expects computation that implements the
 * {@link handist.glb.legacy.Bag} interface.
 * <p>
 * Two implementations of the global load balancer are provided.
 * {@link handist.glb.legacy.LoopGLBProcessor} uses a directed-loop strategy
 * between places while {@link handist.glb.legacy.GenericGLBProcessor} can
 * handle any user-defined strategy. Both these implementations are presented as
 * a computing service which users of the library can obtain from the factory
 * class {@link handist.glb.legacy.GLBProcessorFactory}.
 *
 * @author Patrick Finnerty
 *
 */
package handist.glb.legacy;
