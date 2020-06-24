/**
 *
 */
package handist.glb.examples.nqueens;

import handist.glb.GLBcomputer;
import handist.glb.GLBfactory;
import handist.glb.Logger;
import handist.glb.examples.pentomino.Answer;

/**
 * Launcher for a parallel exploration of the N-Queens problem using the global
 * load balancer library
 *
 * @author Patrick Finnerty
 *
 */
public class ParallelBitNQueens {

  /**
   * Launches a parallel exploration of the N-Queens problem
   *
   * @param args
   *               size of the problem
   */
  public static void main(String[] args) {
    int n = 5;
    if (args.length >= 1) {
      n = Integer.parseInt(args[0]);
    }

    GLBcomputer c;
    try {
      c = GLBfactory.setupGLB();
    } catch (final ReflectiveOperationException e) {
      System.err.println("Error while setting up the GLB.");
      e.printStackTrace();
      return;
    }
    System.out.println(c.getConfiguration());

    final BitNQueens problem = new BitNQueens(n);
    problem.initParallel();

    System.out.println(n + "-queens computation launched");

    final int N = n; // Needs to be final for Serialization in the Lambda
    final Answer a = c.compute(problem, () -> new Answer(N),
        () -> new BitNQueens(N));

    final Logger l = c.getLog();

    long totalNodesExplored = 0;
    for (final long nodes : a.nodes) {
      totalNodesExplored += nodes;
    }

    System.out.println("Solutions;" + a.solutions + "; Nodes Explored;"
        + totalNodesExplored + ";");

    System.out.println("Computation time (s);"
        + (l.initializationTime + l.computationTime + l.resultGatheringTime)
            / 1e9
        + ";");

    l.print(System.err);
  }
}
