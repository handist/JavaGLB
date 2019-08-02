/**
 *
 */
package handist.glb.examples.tsp;

import java.io.Serializable;
import java.sql.Time;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import handist.glb.util.Fold;

/**
 * Class Travel is the result produced by a TSP computation.
 * <p>
 * When used with the GLB, an instance of this class is shared between all the
 * workers of one place. The workers share the "best" solution in member
 * {@link #bestSolutionCost}.
 *
 * @author Patrick Finnerty
 *
 */
public class Travel implements Fold<Travel>, Serializable {

  /** Serial version UID */
  private static final long serialVersionUID = 252756140949485666L;

  /**
   * Collection of ordered City identifiers (array) indicate the best paths
   * found so far. Before any computation begins, this collection will be empty.
   * If a remote place found a better solution and this instance is updated with
   * a better bound, this array will be cleared again.
   */
  byte[][] bestPaths;

  /**
   * Cost of the best solution found so far. This value is meant to be read by
   * many workers concurrently but its update when a better value has been found
   * needs to be done synchronously through method
   * {@link #updateBestSolution(int, byte[])}. This ensures that if a better
   * solution was found by two workers at the same time, only the best one will
   * be kept.
   */
  public volatile int bestSolutionCost;

  /** Counts the number of nodes in the exploration tree */
  transient Map<Integer, Long> exploredCount;

  /** int that keeps track of the place on which this instance was initiated */
  int home = apgas.Constructs.here().id;

  /** Counts the number of solutions with the same length */
  int nbSolution;

  /**
   * Counter in which workers will add up the number of nodes they each explored
   */
  public long nodesExplored;

  /**
   * Constructor
   * <p>
   * Initializes member {@link #bestSolutionCost} to {@link Integer#MAX_VALUE}
   * and member {@link #bestPath} to <code>null</code>.
   */
  public Travel() {
    bestSolutionCost = Integer.MAX_VALUE;
    nodesExplored = 0;
    exploredCount = new HashMap<>();
    bestPaths = new byte[10][0];
  }

  /**
   * @param paths
   */
  private void addAllSolutions(Travel t) {
    for (int i = 0; i < t.nbSolution; i++) {
      final byte[] path = t.bestPaths[i];
      addSolution(path);
    }
  }

  private void addSolution(byte[] path) {
    if (nbSolution == bestPaths.length) {
      bestPaths = Arrays.copyOf(bestPaths, bestPaths.length * 2);
    }
    bestPaths[nbSolution] = path;
    nbSolution++;
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glb.Fold#fold(apgas.glb.Fold)
   */
  @Override
  public void fold(Travel r) {
    if (r.bestSolutionCost < bestSolutionCost) {
      bestSolutionCost = r.bestSolutionCost;
      bestPaths = r.bestPaths;
      nbSolution = 0;
    } else if (r.bestSolutionCost == bestSolutionCost) {
      addAllSolutions(r);
    }

    exploredCount.put(r.home, r.nodesExplored);
  }

  /**
   * Prints on the standard output the number of nodes explored by each
   * individual place.
   */
  public void printExploredNodes() {
    long total = 0;
    System.err.println("0;" + nodesExplored + ";");
    total += nodesExplored;
    for (final Entry<Integer, Long> vl : exploredCount.entrySet()) {
      System.err.println(vl);
      total += vl.getValue();
    }

    System.err.println("Total;" + total);
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    String s = "Length: " + bestSolutionCost + " Paths: " + nbSolution;
    for (int i = 0; i < nbSolution; i++) {
      final byte[] path = bestPaths[i];
      s += "\r\n";
      for (final byte b : path) {

        s += b + " ";
      }
    }
    return s;
  }

  /**
   * Method that workers will call when they think that they found a better
   * solution than the one currently held by this place. Due to possible
   * concurrent accesses, this method is made synchronized.
   *
   * @param cost
   *          Cost of the path found by a worker
   * @param path
   *          the path, array containing the citie's ids
   */
  public synchronized void updateBestSolution(int cost, byte[] path) {
    if (cost < bestSolutionCost) {
      System.out.println(
          new Time(System.currentTimeMillis()) + " new optimum " + cost);
      bestSolutionCost = cost;
      nbSolution = 0;
    }
    addSolution(path);
  }

}
