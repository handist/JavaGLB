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

import org.junit.BeforeClass;
import org.junit.Test;

import apgas.Configuration;
import handist.glb.GLBcomputer;
import handist.glb.GLBfactory;
import handist.glb.examples.pentomino.Answer;

/**
 * Tests the distributed verion of the Bit-mask implementation of the NQueens
 * problem.
 *
 * @author Patrick
 *
 */
public class TestParallelBitNQueens2Hosts {

  /** Size of the problem used */
  public static final int PROBLEM_SIZE = 10;
  /** Solution expected */
  public static final int SOLUTIONS_TO_10QUEENS = 724;

  /**
   * Sets the number of places to use to 1.
   */
  @BeforeClass
  public static void setupBefore() {
    System.setProperty(Configuration.APGAS_PLACES, "2");
    System.setProperty(Configuration.APGAS_THREADS, "2");
  }

  /**
   * Launches the {@link ParallelBitNQueens} computation with one host throws
   * Exception if such an exception is thrown by the GLB
   *
   * @throws Exception
   *           if such exception is thrown during GLB setup / execution
   */
  @Test
  public void testParallelNQueensSingleHost() throws Exception {
    final GLBcomputer c = GLBfactory.setupGLB();

    final BitNQueens problem = new BitNQueens(PROBLEM_SIZE);
    problem.initParallel();

    final Answer a = c.compute(problem, () -> new Answer(PROBLEM_SIZE),
        () -> new BitNQueens(PROBLEM_SIZE));

    c.getLog().print(System.out);

    assertEquals(SOLUTIONS_TO_10QUEENS, a.solutions);
  }

}
