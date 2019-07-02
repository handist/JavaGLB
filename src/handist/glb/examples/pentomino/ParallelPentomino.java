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
    final GLBcomputer computer;
    try {
      width = Integer.parseInt(args[0]);
      height = Integer.parseInt(args[1]);

    } catch (final Exception e) {
      System.err.println("Error parsing arguments");
      System.err.println("Arguments are <WIDTH> <HEIGHT>");
      return;
    }
    try {
      computer = GLBfactory.setupGLB();
    } catch (final Exception e) {
      System.err.println("Error during GLB setup");
      e.printStackTrace();
      return;
    }

    if (width * height != 60 && width * height != 90) {
      System.err.println("Wrong board size: H=" + height + " W=" + width);
      return;
    }

    System.out.println(computer.getConfiguration());

    final Pentomino p = new Pentomino(width, height);
    p.init();
    final Answer ans = computer.compute(p, () -> new Answer(),
        () -> new Pentomino(width, height));

    System.out.println(
        "Solution to H:" + height + " W:" + width + "  " + ans.solutions);
    System.out.println("Tree nodes:" + ans.nodes);
    computer.getLog().print(System.out);
  }
}
