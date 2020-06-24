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

import java.io.Serializable;

/**
 * Class used to contain all the information about a TSP problem, including the
 * adjacency matrix, the bound, the file from which the instance was built etc.
 *
 * @author Patrick Finnerty
 *
 */
public class TspProblem implements Serializable {

  /** Serial Version UID */
  private static final long serialVersionUID = -3211705073425191281L;

  /** Path to the file that was used to generate this instance */
  public final String filePath;

  /** Name given to this instance */
  public final String tspName;

  /** Adjacency matrix of the considered problem */
  public int[][] adjacencyMatrix;

  /** Indicates the minimum cost to expect when `index + 1` steps are needed */
  public int[] boundFunction;

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    String s = tspName + " from " + filePath + " with " + adjacencyMatrix.length
        + " cities" + "\r\n";
    s += "Matrix\r\n";
    for (int i = 0; i < adjacencyMatrix.length; i++) {
      final int[] row = adjacencyMatrix[i];
      for (int j = 0; j < row.length; j++) {
        if (i == j) {
          s += "0 ";
        } else {
          s += row[j] + " ";
        }
      }
      s += "\r\n";
    }

    s += "Bound: ";
    for (final int i : boundFunction) {
      s += " " + i;
    }
    return s;
  }

  /**
   * Constructor
   * <p>
   * Constructs a TspProblem instance which will then be able to use to build
   * {@link TspBag} instances.
   *
   * @param file
   *          path to the file containing the problem in the file system
   * @param name
   *          name to give to the TSP problem considered
   * @param adjMatrix
   *          the adjacency matrix of the problem considered. It must be a
   *          square matrix.
   * @param bound
   *          array to be possibly used by the workers when they perform the
   *          computation
   */
  public TspProblem(String file, String name, int[][] adjMatrix, int[] bound) {
    filePath = file;
    tspName = name;
    adjacencyMatrix = adjMatrix;
    boundFunction = bound;
  }
}
