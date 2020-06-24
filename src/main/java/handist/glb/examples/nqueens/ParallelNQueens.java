/*
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
package handist.glb.examples.nqueens;

import java.util.LinkedList;

import handist.glb.GLBcomputer;
import handist.glb.GLBfactory;
import handist.glb.Logger;
import handist.glb.examples.pentomino.Answer;

/**
 * Launcher of a multithreaded and distributed computation for the N-Queens
 * problem.
 *
 * @author Patrick Finnerty
 *
 */
public class ParallelNQueens {

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
   *               size of the problem and number of repetitions. Optionally the
   *               size of the warm-up to perform.
   */
  public static void main(String[] args) {
    int size;
    int repetitions;
    int warmupSize = 0;
    GLBcomputer computer = null;
    try {
      size = Integer.parseInt(args[0]);
    } catch (final Exception e) {
      System.err.println("Error while parsing the arguments");
      System.err.println("Args <N> [rep] [warmup size]");
      return;
    }
    try {
      repetitions = Integer.parseInt(args[1]);
    } catch (final Exception e) {
      repetitions = 1;
    }
    try {
      warmupSize = Integer.parseInt(args[2]);
    } catch (final Exception e) {
    }

    try {
      computer = GLBfactory.setupGLB();
    } catch (final Exception e) {
      System.err.println("Error during GLB setup");
      e.printStackTrace();
      return;
    }

    if (warmupSize > 0) {
      final int SIZE = warmupSize;
      System.out.println("Starting Warm-up with " + SIZE + "-Queens");
      final Logger warmupLog = computer.warmup(() -> {
        final NQueens problem = new NQueens(SIZE);
        problem.init();
        final NQueens warmupBag = new NQueens();
        warmupBag.reserve = new LinkedList<>();
        warmupBag.reserve.add(problem);
        return warmupBag;
      }, () -> new Answer(SIZE), () -> new NQueens(SIZE),
          () -> new NQueens(SIZE));
      System.out.println("Warmup Time (s); "
          + (warmupLog.initializationTime + warmupLog.computationTime) / 1e9);
      System.err.println("Warm-up Logs");
      warmupLog.print(System.err);
    }

    System.out.println(
        "N=" + size + "; Configuration: " + computer.getConfiguration());
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

      final Answer result = computer.compute(initialBag, () -> new Answer(SIZE),
          () -> new NQueens(SIZE));

      final Logger log = computer.getLog();

      long treeSize = 0;
      for (final long n : result.nodes) {
        treeSize += n;
      }

      System.out.println(
          i + "/" + repetitions + ";" + result.solutions + ";" + treeSize + ";"
              + log.initializationTime / 1e9 + ";" + log.computationTime / 1e9
              + ";" + log.resultGatheringTime / 1e9 + ";");
      System.err.println("Run " + i + " of " + repetitions);
      System.err.print("Nodes; ");
      for (int j = 0; j < SIZE; j++) {
        System.err.print(result.nodes[j] + ";");
      }
      System.err.println();
      System.err.print("Branch; ");
      for (int j = 0; j < SIZE; j++) {
        System.err.print(result.branch[j] + ";");
      }
      System.err.println();
      log.print(System.err);
    }
  }
}
