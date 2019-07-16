/**
 *
 */
package handist.glb.examples.tsp;

import java.io.Serializable;

/**
 * @author Patrick Finnerty
 *
 */
public class NextNode implements Serializable, Comparable<NextNode> {

  /**
   * Serial Version UID
   */
  private static final long serialVersionUID = 575325357408222188L;

  /**
   * Id of the next node
   */
  byte node;

  /**
   * Cost to get to {@link #node}
   */
  int cost;

  /**
   * Sets both values of the {@link NextNode} class. To be meaningful, both
   * parameters must be positive or nil.
   *
   * @param n
   *          identifier of the node to get to
   * @param c
   *          cost to get to the node
   */
  public void set(byte n, int c) {
    node = n;
    cost = c;
  }

  /**
   * Compares this instance with the one given as parameter. The ordering is
   * made following the usual integer ordering using member {@link #cost}.
   */
  @Override
  public int compareTo(NextNode o) {
    return cost - o.cost;
  }

  @Override
  public String toString() {
    return node + "(" + cost + ")";
  }

  /**
   * Default constructor
   */
  public NextNode() {
  }

  /**
   * Copy constructor
   *
   * @param n
   *          the existing instance to be copied
   */
  public NextNode(NextNode n) {
    node = n.node;
    cost = n.cost;
  }
}
