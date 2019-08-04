/**
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
package handist.glb.examples.pentomino;

import java.io.Serializable;

import handist.glb.util.Fold;

/**
 * Class contains two information as the result of the {@link Pentomino}
 * computation. The number of solutions to the given board and the number of
 * nodes in the search tree that corresponds to the search made.
 *
 * @author Patrick Finnerty
 *
 */
public class Answer implements Fold<Answer>, Serializable {

  /** Serial version UID */
  private static final long serialVersionUID = 3877827129899016979L;

  /** Counter for the number of solutions to the pentomino problem */
  public long solutions;

  /** Counter for the number of nodes in the search tree */
  public long[] nodes;
  /** Counter for the number of branches that yield a subtree at each level */
  public long[] branch;

  /**
   * Constructor
   */
  public Answer(int size) {
    solutions = 0;
    nodes = new long[size];
    branch = new long[size];
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.util.Fold#fold(handist.glb.util.Fold)
   */
  @Override
  public void fold(Answer r) {
    solutions += r.solutions;
    for (int i = 0; i < nodes.length; i++) {
      nodes[i] += r.nodes[i];
      branch[i] += r.branch[i];
    }

  }

}
