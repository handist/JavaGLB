/**
 *
 */
package handist.glb.examples.tsp;

import java.io.Serializable;

/**
 * Class used to gather all the information extracted from the text files
 * containing TSP problems. This class is meant to hold assymetric Travelling
 * Salesman Problems.
 *
 * @author Patrick Finnerty
 *
 */
public class TspProblem implements Serializable {

  /**
   * Serial Version UID
   */
  private static final long serialVersionUID = -3211705073425191281L;

  /**
   * Path to the file that was used to generate this instance
   */
  public final String filePath;

  /**
   * Name given to this instance
   */
  public final String tspName;

  /**
   * Adjacency matrix of the considered problem
   */
  public int[][] adjacencyMatrix;

  /**
   * Indicates the minimum cost to expect when `index + 1` steps are needed.
   */
  public int[] boundFunction;

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    String s = tspName + " from " + filePath + " with " + adjacencyMatrix.length
        + " cities" + "\r\n" + "Bound: ";
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
