/**
*
*/
package handist.glb.examples.tsp;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Helper class used to parse files that contain a TSP problem
 *
 * @author Patrick Finnerty
 *
 */
public class TspParser {

  /**
   * Creates a {@link TspProblem} instance from a file.
   * <p>
   * This method will work if the file follows the format of the assymetric
   * travelling salesman problem files available for download <a href=
   * "https://www.iwr.uni-heidelberg.de/groups/comopt/software/TSPLIB95/atsp/">here</a>.
   *
   * @param path
   *          path to the file
   * @return a built {@link TspProblem} as extracted from the file.
   * @throws IOException
   *           if an exception occurs during the file parsing
   */
  public static TspProblem parseFile(String path) throws IOException {
    final File file = new File(path);
    final Scanner sc = new Scanner(file);

    sc.skip("NAME: ");
    final String name = sc.next();
    sc.nextLine();
    sc.nextLine();
    sc.nextLine();
    sc.skip("DIMENSION: ");
    final int nbNodes = sc.nextInt();

    final int[][] matrix = new int[nbNodes][nbNodes];
    final int[] allCosts = new int[nbNodes * nbNodes];
    sc.nextLine();
    sc.nextLine();
    sc.nextLine();
    sc.nextLine();

    for (int i = 0; i < nbNodes; i++) {
      for (int j = 0; j < nbNodes; j++) {
        if (i == j) {
          matrix[i][j] = Integer.MAX_VALUE;
          sc.nextInt(); // Value is ignored
        } else {
          matrix[i][j] = sc.nextInt();
        }
        allCosts[i * nbNodes + j] = matrix[i][j];
      }
    }
    sc.close();

    /**
     * Extracting a bound function giving the minimum cost left to complete a
     * path
     */
    Arrays.sort(allCosts);
    final int[] bound = Arrays.copyOfRange(allCosts, 0, nbNodes);
    Arrays.parallelPrefix(bound, (a, b) -> a + b);

    return new TspProblem(path, name, matrix, bound);
  }

  /**
   * Buils a {@link TspProblem} instance from the given file, limiting the
   * number of cities to the second parameter.
   * <p>
   *
   *
   * @param path
   *          path to the file to parse the problem from
   * @param citySubset
   *          number of cities to keep in the created {@link TspProblem}
   *          instance. Must be positive and smaller or equal to the number of
   *          cities available in the file.
   * @return a {@link TspProblem} instance restricted to the number of cities
   *         given as parameter
   */
  public static TspProblem parseFile(String path, int citySubset)
      throws IOException {
    final TspProblem problem = parseFile(path);

    if (problem.adjacencyMatrix.length <= citySubset) {
      return problem;
    }

    final int[] costs = new int[citySubset * citySubset];

    // Truncate the adjacency matrix
    int i = 0;
    problem.adjacencyMatrix = Arrays.copyOf(problem.adjacencyMatrix,
        citySubset);
    for (int[] dist : problem.adjacencyMatrix) {
      dist = Arrays.copyOf(dist, citySubset);
      for (final int a : dist) {
        costs[i] = a;
        i++;
      }
    }

    // Compute the bound again
    Arrays.sort(costs);
    final int[] bound = Arrays.copyOf(costs, citySubset);
    Arrays.parallelPrefix(bound, (a, b) -> a + b);

    problem.boundFunction = bound;

    return problem;
  }
}
