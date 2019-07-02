/**
 *
 */
package handist.glb.examples.exactcover;

import handist.glb.examples.pentomino.Answer;
import handist.glb.multiworker.GLBcomputer;
import handist.glb.multiworker.GLBfactory;

/**
 * @author Patrick Finnerty
 *
 */
public class ParallelNQueens {

  /**
   * Launches a parallel execution of the N-Queens problem
   *
   * @param args
   *          size of the problem. If not specified, value 8 will be used
   */
  public static void main(String[] args) {
    int size = 8;
    GLBcomputer computer = null;
    try {
      size = Integer.parseInt(args[0]);
      computer = GLBfactory.setupGLB();
    } catch (final Exception e) {
      e.printStackTrace();
      return;
    }

    final NQueens problem = new NQueens(size);
    final int SIZE = size; // variable needs to be final for serialization of
                           // the lambda in the NQueens constructor

    problem.init();
    final Answer result = computer.compute(problem, () -> new Answer(),
        () -> new NQueens(SIZE));

    System.out
        .println(size + "-Queens has " + result.solutions + " solutions.");
    System.out.println(
        "There were " + result.nodes + " nodes in the exploration tree");

    System.out.println("Solutions;" + result.solutions);
    System.out.println("Tree nodes" + result.nodes);
    computer.getLog().print(System.err);
  }
}
