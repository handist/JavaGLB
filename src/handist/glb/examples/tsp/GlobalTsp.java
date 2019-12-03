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
package handist.glb.examples.tsp;

import handist.glb.multiworker.GLBcomputer;
import handist.glb.multiworker.GLBfactory;

/**
 * Launcher for a distributed computation of the TSP using the global load
 * balancer.
 *
 * @author Patrick Finnerty
 *
 */
public class GlobalTsp {

  /**
   * Main method, launches a parallel exploration of the TSP problem using the
   * global load balancer
   *
   * @param args
   *          file from which to read the problem from. Optionally, the subset
   *          number of cities of the matrix to use (by default all) and whether
   *          to use the whisper mechanism to share the bound between hosts
   *          (true by default).
   */
  public static void main(String[] args) {
    // Argument parsing
    TspBag bag;
    TspProblem problem = null;
    GLBcomputer computer;
    boolean whisper = true;
    if (args.length == 0) {
      System.err.println("GlobalTsp: error in program arguments");
      System.err.println("\t <TSP problem file> [Nb cities subset]");
      return;
    }
    try {
      if (args.length == 3) {
        whisper = Boolean.parseBoolean(args[2]);
      }
      if (args.length >= 2) {
        problem = TspParser.parseFile(args[0], Integer.parseInt(args[1]));
      } else if (args.length == 1) {
        problem = TspParser.parseFile(args[0]);
      }
      bag = new TspBag(problem);
      computer = GLBfactory.setupGLB();
    } catch (final Exception e) {
      e.printStackTrace();
      return;
    }

    // Initialization
    bag.init();
    final TspBag transport = new TspBag();
    transport.reserve.add(bag);
    System.out.println(problem);

    final TspProblem pb = problem;

    // Computation
    TspResult t;
    if (whisper) {
      t = computer.computeWhisperedResult(transport, () -> new TspResult(),
          () -> new TspBag(pb), () -> new TspWhisperer());
    } else {
      t = computer.compute(transport, () -> new TspResult(),
          () -> new TspBag(pb));
    }

    // Print the solution on stdout
    System.out.println(t);
    // Print information about the distributed computation on stderr
    computer.getLog().print(System.err);

  }
}
