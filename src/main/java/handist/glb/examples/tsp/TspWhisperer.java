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
package handist.glb.examples.tsp;

import java.io.Serializable;

import handist.glb.Whisperer;

/**
 * Ad-hoc developed class that whose responsibility is to communicate the best
 * bound found locally with remote hosts.
 *
 * @author Patrick Finnerty
 *
 */
public class TspWhisperer
    implements Serializable, Whisperer<Integer, TspResult> {

  /** Serial Version UID */
  private static final long serialVersionUID = -1561511124422451657L;

  /*
   * (non-Javadoc)
   *
   * @see
   * handist.glb.examples.tsp.Whisperer#hasValueToShare(handist.glb.util.Fold)
   */
  @Override
  public boolean hasValueToShare(TspResult source) {
    return source.hasValueToShare();
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * handist.glb.examples.tsp.Whisperer#getInformation(handist.glb.util.Fold)
   */
  @Override
  public Integer getInformation(TspResult source) {
    return source.getInformation();
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.examples.tsp.Whisperer#integrateInformation(java.io.
   * Serializable, handist.glb.util.Fold)
   */
  @Override
  public void integrateInformation(Integer info, TspResult destination) {
    destination.integrateInformation(info);
  }

}
