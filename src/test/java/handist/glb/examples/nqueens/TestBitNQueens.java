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

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Test class for {@link BitNQueens}.
 *
 * @author Patrick
 *
 */
public class TestBitNQueens {

  /** Size of the problem used */
  public static final int PROBLEM_SIZE = 5;
  /** Solution expected */
  public static final int SOLUTIONS = 10;

  /**
   * Checks that a sequential execution gives the proper result
   */
  @Test
  public void testBitNQueens() {
    final BitNQueens problem = new BitNQueens(PROBLEM_SIZE);
    problem.init();

    while (!problem.isEmpty()) {
      problem.step();
    }
    assertEquals(SOLUTIONS, problem.solutionsFound);
  }

  /**
   * Checks that whatever the split made, computation will lead to the same
   * result
   */
  @Test
  public void testSplitting() {
    for (int firstStep = 0; firstStep < 260; firstStep++) {
      // 261 steps makes the first worker do everything, at which point this
      // test becomes moot
      final BitNQueens firstWorker = new BitNQueens(PROBLEM_SIZE);
      final BitNQueens secondWorker = new BitNQueens(PROBLEM_SIZE);
      final BitNQueens reserve = new BitNQueens(PROBLEM_SIZE);

      firstWorker.init();
      firstWorker.process(firstStep, null); // BitNQueens does not use the
      // shared object
      while (!firstWorker.isSplittable() && !firstWorker.isEmpty()) {
        firstWorker.process(1, null);
      }
      if (firstWorker.isSplittable()) {
        final BitNQueens split1 = firstWorker.split(false);
        reserve.merge(split1);

        // Finish the computation
        while (!firstWorker.isEmpty()) {
          firstWorker.process(1, null);
        }

        while (!reserve.isEmpty()) {
          final BitNQueens split = reserve.split(true);
          secondWorker.merge(split);
          while (!secondWorker.isEmpty()) {
            secondWorker.process(1, null);
          }
        }
      } else {
        System.out.println("Making " + firstStep
            + " initial steps makes the first worker do everything");
      }

      final int resultFound = firstWorker.solutionsFound
          + secondWorker.solutionsFound;
      assertEquals("If making " + firstStep + " first steps, things go wrong.",
          SOLUTIONS, resultFound);
    }
  }
}
