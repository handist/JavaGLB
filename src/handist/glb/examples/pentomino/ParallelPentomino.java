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

import handist.glb.GLBcomputer;
import handist.glb.GLBfactory;
import handist.glb.examples.pentomino.Pentomino.PentominoType;

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
   *               <em>width</em> and <em>height</em> of the board to use and
   *               whether symmetries in the problem should be removed (boolean)
   */
  public static void main(String[] args) {
    int width;
    int height;
    boolean removeSymmetries;

    final GLBcomputer computer;
    try {
      width = Integer.parseInt(args[0]);
      height = Integer.parseInt(args[1]);
      removeSymmetries = Boolean.parseBoolean(args[2]);
    } catch (final Exception e) {
      System.err.println("Error parsing arguments");
      System.err.println("Arguments are <WIDTH> <HEIGHT> <Symmetry Removal>");
      return;
    }

    try {
      computer = GLBfactory.setupGLB();
    } catch (final Exception e) {
      System.err.println("Error during GLB setup");
      e.printStackTrace();
      return;
    }

    PentominoType type;
    if (width * height == 60) {
      type = PentominoType.STANDARD;
    } else if (width * height == 90) {
      type = PentominoType.ONE_SIDED;
    } else {
      System.err.println("Wrong board size: H=" + height + " W=" + width);
      return;
    }

    int[] specificPositions;
    if (args.length > 3) {
      // Parse the additional arguments determining the positions of the X piece
      // to be tried. This helps make the problem considerably smaller by
      // reducing the size of the exploration tree.
      final int nbPositions = args.length - 3;
      specificPositions = new int[nbPositions];
      for (int i = 0; i < nbPositions; i++) {
        specificPositions[i] = Integer.parseInt(args[3 + i]);
      }
    } else {
      specificPositions = null;
    }

    // Print the arguments and the configuration of the GLB
    System.out.print("ARGS: ");
    for (final String s : args) {
      System.out.print(s + " ");
    }
    System.out.println();
    System.out.println(computer.getConfiguration());

    // Initialize the problem
    final Pentomino p = new Pentomino(type, width, height);
    p.init(type, removeSymmetries, specificPositions);
    final int treeDepth = p.NB_PIECE;

    // Launch the computation
    final Answer ans = computer.compute(p, () -> new Answer(treeDepth),
        () -> new Pentomino(type), () -> new Pentomino(type, width, height));

    // Print the solution
    System.out.println(
        "Solution to H:" + height + " W:" + width + "; " + ans.solutions + ";");
    System.out.println("Tree nodes; " + ans.nodes + ";");
    computer.getLog().print(System.out);

    // Print more detailed information on the std error output
    System.err.print("Nodes; ");
    for (int j = 0; j < treeDepth; j++) {
      System.err.print(ans.nodes[j] + ";");
    }
    System.err.println();
    System.err.print("Branch; ");
    for (int j = 0; j < treeDepth; j++) {
      System.err.print(ans.branch[j] + ";");
    }
    System.err.println();
  }
}
