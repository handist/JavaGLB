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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import handist.glb.GLBcomputer;
import handist.glb.GLBfactory;
import handist.glb.Logger;

/**
 * Launcher for a distributed computation of the TSP using the global load
 * balancer.
 *
 * @author Patrick Finnerty
 *
 */
public class GlobalTsp {

  /**
   * Prepares the various options that can be given to the program
   *
   * @return an {@link Options} instance containing all the possible options
   *         that can be given to the main program
   */
  private static Options commandOptions() {
    final Options opts = new Options();
    opts.addRequiredOption("f", "problem-file", true,
        "file containing the problem");
    opts.addOption("s", "subset", true,
        "subset of cities to take for the problem");
    opts.addOption("q", "quiet", false,
        "disables the whisper of newly found bounds between hosts");
    return opts;
  }

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
    final Options programOptions = commandOptions();
    final CommandLineParser parser = new DefaultParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse(programOptions, args);
    } catch (final ParseException e1) {
      System.err.println(e1.getLocalizedMessage());
      final HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp(
          "java [...] GlobalTSP -f <problem file> [-s <integer>] [-q]",
          programOptions);
      return;
    }

    // Argument parsing

    if (args.length == 0) {
      System.err.println("\t <TSP problem file> [Nb cities subset]");
      return;
    }

    TspProblem problem;
    TspBag bag;
    GLBcomputer computer;

    final boolean whisper = !cmd.hasOption('q');
    final String filePath = cmd.getOptionValue('f');
    try {
      if (cmd.hasOption('s')) {
        problem = TspParser.parseFile(filePath,
            Integer.parseInt(cmd.getOptionValue('s')));
      } else {
        problem = TspParser.parseFile(filePath);
      }
    } catch (final Exception e) {
      e.printStackTrace();
      return;
    }
    try {
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
    System.err.println(t);
    // Print information about the distributed computation on stderr
    final Logger log = computer.getLog();
    System.out.println("COMPUTATION TIME;" + log.computationTime / 1e9 + ";");
    System.out.println();
    log.print(System.out);

  }
}
