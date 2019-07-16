/**
 *
 */
package handist.glb.examples.tsp;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

import handist.glb.multiworker.Bag;

/**
 * TSP implementation. It can handle problem with up to {@link Byte#MAX_VALUE}
 * cities as a byte is used to identify cities.
 *
 * @author Patrick Finnerty
 *
 */
public class TspBag implements Bag<TspBag, Travel>, Serializable {

  /**
   *
   */
  private static final long serialVersionUID = -8573121302967870892L;

  /**
   * Contains the information of how much going from city A (first index) to
   * city B (second index) costs.
   */
  public transient final int[][] ADJ_MATRIX;

  /**
   * Number of cities in the considered problem
   */
  public transient final int TOTAL_NB_CITIES;

  /**
   * Bound indicating the minimum of completing the path when one has `N+1` hops
   * left to go where N is the index in {@link #BOUND_FUNCTION}.
   */
  public transient final int[] BOUND_FUNCTION;

  /**
   * Size of the path currently being explored. The information concerning the
   * last nodes being explored are therefore located at "index-1" in arrays
   * {@link #cost}, {@link #path} and {@link #nextNodes}.
   */
  int index;

  /**
   * Cost array, keeps track of the distance needed to reach the node located at
   * the same index in array {@link #path}.
   */
  int cost[];

  /**
   * Path array, keeps track of which node we are currently located at. Its
   * value at index 0 is always 0 as we arbitrarly decided that all round trips
   * were started (and finished) at 0.
   */
  byte path[];

  /**
   * At each index, the map keys are the nodes remaining to go to in order to
   * complete a path. If a node and its subtree is or has been explored, its id
   * is mapped to false. If it (and its subtree) remains to be explored, it is
   * mapped to true.
   */
  NextNode nextNodes[][];

  /**
   *
   * Keeps track of the lower range of nodes remaining to explore in the
   * {@link NextNode} at the same index. The next node to explore at depth 0 is
   * nextNodes[0][low[0], provided high[0] > low[0].
   */
  int low[];

  /**
   * Keeps track of the upper bound of the range of nodes reaming to explore at
   * each 'index' in array {@link #nextNodes}. Nodes remaining to explore are at
   * indexes strictly lower than what is contained by array {@link #high}.
   */
  int high[];

  /**
   * Counts the number of nodes explored by this bag. This counter is
   * incremented in method {@link #exploreOne(Travel)} every time it is called.
   */
  transient VeryLong nodeCounter;

  /**
   * Steps back in the exploration until an unexplored leaf is found or the bag
   * is found to be empty.
   */
  void stepBack() {
    while (0 < index && low[index - 1] == high[index - 1]) {
      index--;
    }
  }

  /**
   * Explores the current
   *
   * @param shared
   *          the shared object between workers in which the optimum solution
   *          found so far is stored
   */
  void exploreOne(Travel shared) {
    nodeCounter.increment();
    final NextNode candidates[] = nextNodes[index - 1]; // for elegance
                                                        // henceforth

    if (candidates.length == 0) {
      // There are no candidates, we are at the end of a path

      // We add the cost of the return to city 0
      final int totalCost = cost[index - 1] + ADJ_MATRIX[path[index - 1]][0];
      if (totalCost < shared.bestSolutionCost) {
        shared.updateBestSolution(totalCost,
            Arrays.copyOfRange(path, index - TOTAL_NB_CITIES, index));
      }
      stepBack();
      return;
    }

    if (low[index - 1] == high[index - 1]) {
      // There were unexplored candidates left, stepback
      stepBack();
      return;
    }

    final NextNode next = candidates[low[index - 1]]; // next is the candidate

    final int nextCost = cost[index - 1] + next.cost;
    if (nextCost
        + BOUND_FUNCTION[TOTAL_NB_CITIES - index] < shared.bestSolutionCost) {
      // We may find a better solution, we keep exploring with that node

      cost[index] = nextCost;
      path[index] = next.node;

      // We carry forward all the remaining candidates in the two following for
      // loops
      int j = 0; // index for nextNodes[index] array
      for (int i = 0; i < low[index - 1]; i++) {
        final NextNode toExplore = candidates[i];
        nextNodes[index][j++].set(toExplore.node,
            ADJ_MATRIX[next.node][toExplore.node]);
      }
      // The chosen candidate is skipped in the candidates array
      for (int i = low[index - 1] + 1; i < nextNodes[index - 1].length; i++) {
        final NextNode toExplore = candidates[i];
        nextNodes[index][j++].set(toExplore.node,
            ADJ_MATRIX[next.node][toExplore.node]);
      }

      Arrays.sort(nextNodes[index]); // Sort the array

      low[index - 1]++;// next will not be a candidate anymore
      low[index] = 0;
      high[index] = j;
      index++;
    } else {
      // No chance of finding a better solution, we backtrack
      low[index - 1] = high[index - 1];
      stepBack();
    }
  }

  /**
   * Prepares this instance to run the TSP from the very start
   */
  public void init() {
    index = 1;
    path[0] = 0;
    cost[0] = 0;
    int j = 0;
    for (byte city = 1; city < TOTAL_NB_CITIES; city++, j++) {
      nextNodes[0][j].set(city, ADJ_MATRIX[0][city]);
    }
    Arrays.sort(nextNodes[0]);
    low[0] = 0;
    high[0] = nextNodes[0].length;
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glbm.Bag#isEmpty()
   */
  @Override
  public boolean isEmpty() {
    return index <= 0;
  }

  /**
   * A TSP is considered to be splittable if there are at least 2 unexplred
   * leaves in it.
   */
  @Override
  public boolean isSplittable() {
    int leafCount = 0;
    for (int i = 0; i < index; i++) {
      leafCount += (high[i] - low[i]);
      if (2 <= leafCount) {
        return true;
      }

    }
    return false;
  }

  public int leaves() {
    int leafCount = 0;
    for (int i = 0; i < index; i++) {
      leafCount += high[i] - low[i];
    }
    return leafCount;
  }

  /**
   * Merging is done by stitching the incoming TSPBag instance arrays to the
   * {@link #cost}, {@link #path} and {@link #nextNodes} arrays starting from
   * index {@link #index} onwards. The size of the three arrays will be
   * increased if necessary.
   */
  @Override
  public void merge(TspBag b) {
    // Check if there is enough space in the arrays to store the incoming bag
    if (nextNodes.length - index < b.index) {
      // Increase the size of the array
      cost = Arrays.copyOf(cost, cost.length + b.index);
      path = Arrays.copyOf(path, path.length + b.index);
      nextNodes = Arrays.copyOf(nextNodes, nextNodes.length + b.index);
      low = Arrays.copyOf(low, low.length + b.index);
      high = Arrays.copyOf(high, high.length + b.index);
    }

    // Append the content of b into this instance
    for (int i = 0; i < b.index; i++) {
      cost[index] = b.cost[i];
      path[index] = b.path[i];
      nextNodes[index] = b.nextNodes[i];
      low[index] = b.low[i];
      high[index] = b.high[i];
      index++;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glbm.Bag#process(int, apgas.glb.Fold)
   */
  @Override
  public void process(int workAmount, Travel sharedObject) {
    while (0 < workAmount && !isEmpty()) {
      exploreOne(sharedObject);
      workAmount--;
    }
  }

  /**
   * Computes the whole TSP sequentially
   *
   * @param result
   *          the instance into which the result is going to be stored
   */
  public void run(final Travel result) {
    while (!isEmpty()) {
      process(Integer.MAX_VALUE, result);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glbm.Bag#split(boolean)
   */
  @Override
  public TspBag split(boolean takeAll) {
    if (index <= 0 || (!isSplittable() && !takeAll) || isEmpty()) {
      return new TspBag(0, TOTAL_NB_CITIES);
    }

    int start;
    for (start = index - 1; 0 < start; start--) {
      if (path[start] == 0) {
        break;
      }
    }
    final int depthOfCut = index - start;
    final TspBag toReturn = new TspBag(depthOfCut, TOTAL_NB_CITIES);

    boolean alternator = true;
    for (int i = start, j = 0; i < index; i++, j++) {
      // Copy cost, path and nextNodes arrays
      toReturn.path[j] = path[i];
      toReturn.cost[j] = cost[i];
      for (int k = 0; k < toReturn.nextNodes[j].length; k++) {
        toReturn.nextNodes[j][k] = new NextNode(nextNodes[i][k]);
      }

      // Split is made with arrays low and high
      final int leavesCount = high[i] - low[i];
      // middle index of the remaining candidates is picked
      int splitIndex = (high[i] + low[i]) / 2;
      if (leavesCount % 2 == 1) {
        if (alternator) {
          splitIndex++;
        }
        alternator = !alternator;
      }

      toReturn.high[j] = splitIndex;
      toReturn.low[j] = low[i];
      low[i] = splitIndex;
    }
    toReturn.index = depthOfCut;
    toReturn.stepBack();
    stepBack();

    return toReturn;
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glbm.Bag#submit(apgas.glb.Fold)
   */
  @Override
  public void submit(Travel r) {
    r.nodesExplored.add(nodeCounter);
  }

  /**
   * Initializes an empty bag with the given problem.
   *
   * @param problem
   *          the problem considered
   */
  public TspBag(TspProblem problem) {
    nodeCounter = new VeryLong();
    ADJ_MATRIX = problem.adjacencyMatrix;
    TOTAL_NB_CITIES = ADJ_MATRIX.length;
    BOUND_FUNCTION = problem.boundFunction;

    index = 0;
    cost = new int[TOTAL_NB_CITIES];
    path = new byte[TOTAL_NB_CITIES];
    nextNodes = new NextNode[TOTAL_NB_CITIES][0];
    for (int i = 0; i < TOTAL_NB_CITIES; i++) {
      nextNodes[i] = new NextNode[TOTAL_NB_CITIES - i - 1];
      for (int j = 0; j < nextNodes[i].length; j++) {
        nextNodes[i][j] = new NextNode();
      }
    }
    low = new int[TOTAL_NB_CITIES];
    high = new int[TOTAL_NB_CITIES];
  }

  /**
   * Constructor for TSPBag instance that are going to be used to transport
   * splits from one place to an other. The arrays will be initialized with the
   * minimum size needed and the transient terms will not be initialized
   *
   * @param nbNodes
   *          number of nodes in the path which is going to be split from
   * @param nbCities
   *          of cities in the current TSP problem
   */
  protected TspBag(int pathLength, int nbCities) {
    // Initialization to avoid warning on non initialized final members
    ADJ_MATRIX = null;
    BOUND_FUNCTION = null;
    TOTAL_NB_CITIES = nbCities;

    index = 0;
    cost = new int[pathLength];
    path = new byte[pathLength];
    nextNodes = new NextNode[pathLength][0];
    for (int i = 0; i < pathLength; i++) {
      nextNodes[i] = new NextNode[nbCities - i - 1];
    }
    low = new int[pathLength];
    high = new int[pathLength];
  }

  /**
   * Launches a sequential execution of the TSP
   *
   * @param args
   *          path to the input problem file
   */
  public static void main(String[] args) {
    final Travel result = new Travel(0);

    TspProblem problem;
    try {
      if (args.length == 2) {
        problem = TspParser.parseFile(args[0], Integer.parseInt(args[1]));
      } else {
        problem = TspParser.parseFile(args[0]);
      }
    } catch (final IOException e) {
      e.printStackTrace();
      return;
    }
    final TspBag bag = new TspBag(problem);

    bag.init();
    long computationTime = System.nanoTime();
    bag.run(result);
    computationTime = System.nanoTime() - computationTime;

    System.out.println(problem);
    System.out.println("Computation took " + computationTime / 1e9 + "s");
    System.out.println("Final result is " + result);
    System.out.println("Number of nodes explored: " + bag.nodeCounter);
  }

}
