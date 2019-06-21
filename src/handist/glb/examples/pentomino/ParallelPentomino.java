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

import handist.glb.examples.pentomino.Pentomino.PentominoType;
import handist.glb.multiworker.GLBcomputer;
import handist.glb.multiworker.GLBfactory;

/**
 * Launcher for a parallel Pentomino exploration
 *
 * @author Patrick Finnerty
 *
 */
public class ParallelPentomino {

  /**
   * Launches a parallel exploration of Pentomino problem using the
   * multithreaded global load balancer.
   *
   * @param args
   *          width and height of the board to use
   */
  public static void main(String[] args) {
    int width;
    int height;
    boolean symmetriesOff = true;

    final GLBcomputer computer;
    try {
      computer = GLBfactory.setupGLB();
      width = Integer.parseInt(args[0]);
      height = Integer.parseInt(args[1]);
      symmetriesOff = Boolean.parseBoolean(args[2]);
    } catch (final Exception e) {
      e.printStackTrace();
      return;
    }

    System.out.println(computer.getConfiguration());

    final Pentomino p = new Pentomino(PentominoType.STANDARD, width, height);
    p.init(PentominoType.STANDARD, symmetriesOff);

    int counter = 0;
    for (final Piece piece : p.pieces) {
      piece.printVariations(width + Board.SENTINEL);
      counter += piece.variations();
    }
    System.err.println(counter);
    final Answer ans = computer.compute(p, () -> new Answer(),
        () -> new Pentomino(PentominoType.STANDARD, width, height));

    System.out.println(
        "Solution to H:" + height + " W:" + width + "  " + ans.solutions);
    System.out.println("Tree nodes:" + ans.nodes);
    computer.getLog().print(System.out);
  }
}
