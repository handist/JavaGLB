/**
 *
 */
package handist.glb.examples.tsp;

import handist.glb.multiworker.GLBcomputer;
import handist.glb.multiworker.GLBfactory;

/**
 * Launcher for a distributed computation of the TSP
 *
 * @author Patrick Finnerty
 *
 */
public class GlobalTsp {

  // public static void test(TspProblem problem, TspBag b) {
  // final TspBag split = new TspBag(problem);
  // final Travel result = new Travel(0);
  // split.merge(b.split(false));
  //
  // long start = System.nanoTime();
  // while (!b.isEmpty()) {
  // b.process(300, result);
  // }
  // final long firstBag = System.nanoTime() - start;
  //
  // start = System.nanoTime();
  // while (!split.isEmpty()) {
  // split.process(300, result);
  // }
  // final long secondBag = System.nanoTime() - start;
  //
  // System.err.println(firstBag / 1e9 + " " + secondBag / 1e9);
  // }

  /**
   * Main method, launches a prallel exploration of the TSP problem using the
   * global load balancer
   *
   * @param args
   *          file from which to read the problem from, (optional) subset number
   *          of cities to use
   */
  public static void main(String[] args) {
    TspBag bag;
    TspProblem problem = null;
    GLBcomputer computer;
    if (args.length == 0) {
      System.err.println("GlobalTsp: error in program arguments");
      System.err.println("\t <TSP problem file> [Nb cities subset]");
      return;
    }
    try {
      if (args.length == 2) {
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

    bag.init();

    // test(problem, bag);

    System.out.println(problem);

    final TspProblem pb = problem;
    final Travel t = computer.compute(bag, () -> new Travel(),
        () -> new TspBag(pb));

    System.out.println(t);

    for (int i = 0; i < t.nbSolution; i++) {
      final byte[] path = t.bestPaths[i];
      for (final byte b : path) {
        System.out.print(b + " ");
      }
      System.out.println();
      byte prev = 0;
      int pathLength = 0;
      int j = 0;
      for (; j < path.length; j++) {
        final int cost = problem.adjacencyMatrix[prev][path[j]];
        prev = path[j];
        pathLength += cost;
      }
      // pathLength += problem.adjacencyMatrix[path[j - 1]][0];
      System.out.println("Path length check: " + pathLength);
    }

    t.printExploredNodes();
    computer.getLog().print(System.err);

  }
}
