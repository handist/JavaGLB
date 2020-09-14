/**
 *
 */
package handist.glb.examples.nqueens;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

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
   * Prepares the various options that can be given to the program
   *
   * @return an {@link Options} instance containing all the possible options
   *         that can be given to the main program
   */
  private static Options commandOptions() {
    final Options opts = new Options();
    opts.addRequiredOption("n", "boardwidth", true,
        "board width (size of the problem)");
    return opts;
  }

  /**
   * Launches a parallel exploration of the N-Queens problem
   *
   * @param args
   *          size of the problem
   */
  public static void main(String[] args) {
    final Options programOptions = commandOptions();
    final CommandLineParser parser = new DefaultParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse(programOptions, args);
    } catch (final ParseException e1) {
      System.err.println(e1.getLocalizedMessage());
      final HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("java [...] ParallelBitNQueens -n <integer>",
          programOptions);
      return;
    }

    final int n = Integer.parseInt(cmd.getOptionValue('n'));

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
