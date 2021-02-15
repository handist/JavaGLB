/*******************************************************************************
 * This file is part of the Handy Tools for Distributed Computing project
 * HanDist (https:/github.com/handist)
 *
 * This file is licensed to You under the Eclipse Public License (EPL);
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 	https://www.opensource.org/licenses/eclipse-1.0.php
 *
 * (C) copyright CS29 Fine 2018-2021
 ******************************************************************************/
package handist.glb.examples.nqueens;

import java.util.LinkedList;

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
 * Launcher of a multithreaded and distributed computation for the N-Queens
 * problem.
 *
 * @author Patrick Finnerty
 */
public class ParallelNQueens {

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
    opts.addOption("w", "warmup", true,
        "warmup size, setting this option will activate the warmup");
    return opts;
  }

  /**
   * Launches a parallel execution of the N-Queens problem using the global load
   * balancer.
   *
   * Arguments include the size of the problem and the number of repetitions
   * desired. Optionally the user can launch a warm-up before the computation,
   * the size of which is the third argument. If only two arguments are
   * specified, no warm-up will be performed.
   *
   * @param args
   *          size of the problem and number of repetitions. Optionally the size
   *          of the warm-up to perform.
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
      formatter.printHelp(
          "java [...] ParallelNQueens -n <integer> [-w <integer>]",
          programOptions);
      return;
    }

    final int size = Integer.parseInt(cmd.getOptionValue('n'));
    final int repetitions = 1;
    final int warmupSize = Integer.parseInt(cmd.getOptionValue('w', "1"));

    GLBcomputer computer = null;
    try {
      computer = GLBfactory.setupGLB();
    } catch (final Exception e) {
      System.err.println("Error during GLB setup");
      e.printStackTrace();
      return;
    }

    if (warmupSize > 0) {
      final int SIZE = warmupSize;
      System.err.println("Starting Warm-up with " + SIZE + "-Queens");
      final Logger warmupLog = computer.warmup(() -> {
        final NQueens problem = new NQueens(SIZE);
        problem.init();
        final NQueens warmupBag = new NQueens();
        warmupBag.reserve = new LinkedList<>();
        warmupBag.reserve.add(problem);
        return warmupBag;
      }, () -> new Answer(SIZE), () -> new NQueens(SIZE),
          () -> new NQueens(SIZE));
      System.out.println("WARMUP TIME; "
          + (warmupLog.initializationTime + warmupLog.computationTime) / 1e9
          + ";");
      System.err.println("Warm-up Logs");
      warmupLog.print(System.err);
      System.err.println();
    }

    System.err.println(
        "N=" + size + "; Configuration: " + computer.getConfiguration());
    System.err.println();
    System.err.println("Run;Solutions;TreeNodes;Init time(s);"
        + "Computation time(s);Gathering time(s);");
    for (int i = 0; i < repetitions; i++) {

      final NQueens problem = new NQueens(size);
      problem.init();
      final NQueens initialBag = new NQueens();
      initialBag.reserve = new LinkedList<>();
      initialBag.reserve.add(problem);

      final int SIZE = size; // variable needs to be final for serialization of
                             // the lambda in the NQueens constructor

      final Answer result = computer.compute(initialBag, () -> new Answer(SIZE),
          () -> new NQueens(SIZE));

      final Logger log = computer.getLog();

      long treeSize = 0;
      for (final long n : result.nodes) {
        treeSize += n;
      }

      System.err.println(
          i + "/" + repetitions + ";" + result.solutions + ";" + treeSize + ";"
              + log.initializationTime / 1e9 + ";" + log.computationTime / 1e9
              + ";" + log.resultGatheringTime / 1e9 + ";");
      // System.err.println("Run " + i + " of " + repetitions);
      // System.err.print("Nodes; ");
      // for (int j = 0; j < SIZE; j++) {
      // System.err.print(result.nodes[j] + ";");
      // }
      // System.err.println();
      // System.err.print("Branch; ");
      // for (int j = 0; j < SIZE; j++) {
      // System.err.print(result.branch[j] + ";");
      // }
      // System.err.println();
      System.out.println("COMPUTATION TIME;" + log.computationTime / 1e9 + ";");
      System.out.println();
      log.print(System.out);
    }
  }
}
