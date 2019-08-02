/**
 *
 */
package handist.glb.examples.exactcover;

import java.util.LinkedList;

import handist.glb.examples.pentomino.Answer;
import handist.glb.multiworker.GLBcomputer;
import handist.glb.multiworker.GLBfactory;
import handist.glb.multiworker.Logger;

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
    int repetitions;
    GLBcomputer computer = null;
    try {
      size = Integer.parseInt(args[0]);
    } catch (final Exception e) {
      System.err.println("Error while parsing the arguments");
      System.err.println("Args <N> [rep]");
      return;
    }
    try {
      repetitions = Integer.parseInt(args[1]);
    } catch (final Exception e) {
      repetitions = 10;
    }

    try {
      computer = GLBfactory.setupGLB();
    } catch (final Exception e) {
      System.err.println("Error during GLB setup");
      e.printStackTrace();
      return;
    }

    System.out
        .println("N=" + size + "Configuration: " + computer.getConfiguration());
    System.out.println("Run;Solutions;TreeNodes;Init time(s);"
        + "Computation time(s);Gathering time(s);");
    for (int i = 0; i < repetitions; i++) {

      final NQueens problem = new NQueens(size);
      problem.init();
      final NQueens initialBag = new NQueens();
      initialBag.reserve = new LinkedList<>();
      initialBag.reserve.add(problem);

      final int SIZE = size; // variable needs to be final for serialization of
                             // the lambda in the NQueens constructor

      final Answer result = computer.compute(initialBag, () -> new Answer(),
          () -> new NQueens(SIZE));

      final Logger log = computer.getLog();

      System.out.println(i + "/" + repetitions + ";" + result.solutions + ";"
          + result.nodes + ";" + log.initializationTime / 1e9 + ";"
          + log.computationTime / 1e9 + ";" + log.resultGatheringTime / 1e9
          + ";");
      System.err.println("Run " + i + " of " + repetitions);
      log.print(System.err);
    }
  }
}
