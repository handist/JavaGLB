/**
 *
 */
package handist.glb;

import java.io.Serializable;

import handist.glb.examples.tsp.TspWhisperer;
import handist.glb.util.Fold;

/**
 * Implementation of this interface can be used by the programmer to propagate
 * information between java process during the computation. It is intended as a
 * complement to the shared instance held on each host.
 * <p>
 * By calling the {@link GLBcomputer#computeWhisperedResult} method, the
 * programmer can activate this mechanism and have their implementation of this
 * interface called periodically to check if the local shared object has some
 * value to share. If so, the information to be shared with remote hosts will be
 * obtained by a call to {@link #getInformation(Fold)} and sent to remote hosts.
 * When the information reaches remote hosts, it will be the responsibility of
 * this class of integrating it into the local instance through a call to
 * {@link #integrateInformation(Serializable, Fold)}.
 * <p>
 * One use example of this mechanism is that of the branch and bound TSP
 * exploration presented in {@link handist.glb.examples.tsp.TspBag}. The best
 * bound found so far (an Integer) is communicated to hosts of the distributed
 * computation by the {@link TspWhisperer} implementation of this interface.
 *
 * @author     Patrick Finnerty
 * @param  <I>
 *               Type of the information which is carried from a java process to
 *               an other
 * @param  <R>
 *               Type of the result object which is shared between workers on a
 *               host
 *
 */
public interface Whisperer<I extends Serializable, R extends Fold<R> & Serializable> {

  /**
   * Should indicate if some information contained by the parameter given is
   * worth sharing with the other java processes taking part in the distributed
   * computation.
   *
   * @param  source
   *                  shared object from which the information (if any) would
   *                  come from
   *
   * @return        true is the instance has information to share with other
   *                hosts, false otherwise
   */
  public boolean hasValueToShare(R source);

  /**
   * Gives the information that is worth sharing with other hosts.
   *
   * @param  source
   *                  shared object instance from which the information is going
   *                  to come from
   * @return        instance of class I containing the information which will be
   *                sent to other compute nodes
   */
  public I getInformation(R source);

  /**
   * Integrates the information coming from remote hosts into this local
   * instance.
   *
   * @param info
   *                      the information to integrate in to this local
   *                      instance.
   * @param destination
   *                      shared object in which the information is destined to
   *                      be integrated into
   */
  public void integrateInformation(I info, R destination);

}
