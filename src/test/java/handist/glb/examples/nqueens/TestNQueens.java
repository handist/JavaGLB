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
 * Small test to check that the {@link NQueens} class produces the intended
 * results. The test contained here is purely sequential
 *
 * @author Patrick Finnerty
 *
 */
public class TestNQueens {

    /** Size of the problem used */
    public static final int PROBLEM_SIZE = 10;
    /** Solution expected */
    public static final int SOLUTIONS_TO_10QUEENS = 724;

    /**
     * Launches a sequential NQueens and checks the result
     */
    @Test
    public void test() {
        final NQueens problem = new NQueens(PROBLEM_SIZE);
        problem.init();
        problem.toCompletion();
        assertEquals(SOLUTIONS_TO_10QUEENS, problem.solutionCount);
    }

}
