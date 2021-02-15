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
package handist.glb.examples.pentomino;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import handist.glb.GLBcomputer;
import handist.glb.GLBfactory;
import handist.glb.Logger;
import handist.glb.examples.pentomino.Pentomino.PentominoType;

/**
 * Launcher for a parallel Pentomino exploration
 *
 * @author Patrick Finnerty
 *
 */
public class ParallelPentomino {
  /**
   * Prepares the various options that can be given to the program
   *
   * @return an {@link Options} instance containing all the possible options
   *         that can be given to the main program
   */
  private static Options commandOptions() {
    final Options opts = new Options();
    opts.addRequiredOption("w", "width", true, "board width (6, 9 or 10)");
    opts.addRequiredOption("h", "height", true, "board height (6, 9 or 10)");
    opts.addOption("k", "keep-symmetries", false,
        "keep the symmetries of the problem (removed by default)");
    return opts;
  }

  /**
   * Launches a parallel exploration of Pentomino problem using the
   * multithreaded global load balancer.
   *
   * @param args
   *          <em>width</em> and <em>height</em> of the board to use and whether
   *          symmetries in the problem should be removed (boolean)
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
          "java [...] ParallelPentomino -w <integer> -h <integer> [-k]",
          programOptions);
      return;
    }

    final int width = Integer.parseInt(cmd.getOptionValue('w'));
    final int height = Integer.parseInt(cmd.getOptionValue('h'));
    final boolean removeSymmetries = !cmd.hasOption('k');

    final GLBcomputer computer;
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
      System.err
          .println("Board surface should be either 60 or 90 square tiles");
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
    System.err.print("ARGS: ");
    for (final String s : args) {
      System.err.print(s + " ");
    }
    System.err.println();
    System.err.println(computer.getConfiguration());

    // Initialize the problem
    final Pentomino p = new Pentomino(type, width, height);
    p.init(type, removeSymmetries, specificPositions);
    final int treeDepth = p.NB_PIECE;

    // Launch the computation
    final Answer ans = computer.compute(p, () -> new Answer(treeDepth),
        () -> new Pentomino(type), () -> new Pentomino(type, width, height));

    // Print the solution
    System.err.println(
        "Solution to H:" + height + " W:" + width + "; " + ans.solutions + ";");
    System.err.println("Tree nodes; " + ans.nodes + ";");

    // Output to stdout
    final Logger log = computer.getLog();
    System.out.println("COMPUTATION TIME;" + log.computationTime / 1e9 + ";");
    System.out.println();
    log.print(System.out);

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
