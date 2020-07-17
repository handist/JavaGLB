/**
 *
 */
package handist.glb.examples.nqueens;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;

import handist.glb.Bag;
import handist.glb.examples.pentomino.Answer;

/**
 * Implementation of the N-Queens search using a bit-mask implementation.
 *
 * @author Patrick Finnerty
 *
 */
public class BitNQueens implements Bag<BitNQueens, Answer>, Serializable {

  /** Serial Version UID */
  private static final long serialVersionUID = -7839265072845647786L;

  /**
   * Main method. Launches a sequential exploration of the N-Queens problem of
   * the specified size.
   *
   * @param args
   *          one argument: size of the problem
   */
  public static void main(String args[]) {
    int n = 5;
    if (args.length >= 1) {
      n = Integer.parseInt(args[0]);
    }

    final BitNQueens problem = new BitNQueens(n);
    problem.init();

    System.out.println("Launching a sequential " + n + "-queens computation");

    long computationTime = System.nanoTime();

    while (!problem.isEmpty()) {
      problem.step();
    }

    computationTime = System.nanoTime() - computationTime;

    long totalNodesExplored = 0;
    for (final long nodes : problem.nodesExplored) {
      totalNodesExplored += nodes;
    }

    System.out.println("Solutions;" + problem.solutionsFound + ";");
    System.out.println("Nodes explored;" + totalNodesExplored + ";");
    System.out.println("Computation time (s);" + computationTime / 1e9 + ";");
  }

  /** Current depth of the exploration */
  int depth;

  /** Size of the problem at hand */
  final int N;

  /** Counter for the number of nodes explored during the computation */
  private long nodesExplored[];

  /** Reserve of exploration to perform */
  Deque<BitNQueens> reserve;

  /** Number of solutions to the N-Queens problem */
  int solutionsFound;

  /**
   * Stack that describes the placement of each individual queen on the board
   * (anti-diagonal mask)
   */
  int[] stackAntiDiagonal;

  /**
   * Stack that describes the placement of each individual queen on the board
   * (line mask)
   */
  int[] stackColumn;

  /**
   * Stack that describes the placement of each individual queen on the board
   * (diagonal mask)
   */
  int[] stackDiagonal;

  /**
   * Keeps track of the mask used at each level
   */
  int[] stackMask;

  /**
   * Array of the next option that remains to be attempted at each level of the
   * exploration tree.
   */
  int[] treeLowerBound;

  /**
   * Array of the options in that do not have to be explored at each level of
   * the exploration tree.
   */
  int[] treeUpperBound;

  /**
   * Private constructor used create instances that will be used as recipients
   * for a fragment of the computation
   */
  private BitNQueens() {
    N = 42;
  }

  /**
   * Constructor for the BitNQueens
   *
   * @param boardSize
   *          width of the board considered
   */
  public BitNQueens(int boardSize) {
    N = boardSize;
    treeLowerBound = new int[boardSize];
    treeUpperBound = new int[boardSize];
    stackColumn = new int[boardSize];
    stackAntiDiagonal = new int[boardSize];
    stackDiagonal = new int[boardSize];
    stackMask = new int[boardSize];
    nodesExplored = new long[boardSize];
    reserve = new LinkedList<>();
  }

  /**
   * Resets this instance to start the whole computation. For computation using
   * the GLB, you should call {@link #initParallel()} instead of this method.
   */
  public void init() {
    depth = 0;
    stackColumn[0] = 0;
    stackDiagonal[0] = 0;
    stackAntiDiagonal[0] = 0;
    stackMask[0] = 0;
    treeLowerBound[0] = 0;
    treeUpperBound[0] = N;
    solutionsFound = 0;
  }

  /**
   * Initializes an instance to make it ready for a parallel exploration using
   * the GLB.
   */
  public void initParallel() {
    init();
    reserve.add(this);
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.multiworker.Bag#isEmpty()
   */
  @Override
  public boolean isEmpty() {
    return depth < 0 && reserve.isEmpty();
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.multiworker.Bag#isSplittable()
   */
  @Override
  public boolean isSplittable() {
    return !reserve.isEmpty() || treeSplittable();
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.multiworker.Bag#merge(handist.glb.multiworker.Bag)
   */
  @Override
  public void merge(BitNQueens b) {
    if (depth < 0) {
      restore(b.reserve.poll());
    } else {
      reserve.addAll(b.reserve);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.multiworker.Bag#process(int, handist.glb.util.Fold)
   */
  @Override
  public void process(int workAmount, Answer sharedObject) {
    while (!isEmpty() && workAmount-- > 0) {
      if (depth < 0) {
        restore(reserve.poll());
      } else {
        step();
      }
    }
  }

  /**
   * Takes the provided instance and updates the member of this instance to
   * continue the exploration of the provided instance
   *
   * @param poll
   *          instance whose exploration is to continue
   */
  private void restore(BitNQueens c) {
    stackAntiDiagonal = c.stackAntiDiagonal;
    stackColumn = c.stackColumn;
    stackDiagonal = c.stackDiagonal;
    stackMask = c.stackMask;
    depth = c.depth;
    treeLowerBound = c.treeLowerBound;
    treeUpperBound = c.treeUpperBound;
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.multiworker.Bag#split(boolean)
   */
  @Override
  public BitNQueens split(boolean takeAll) {
    final BitNQueens toReturn = new BitNQueens();
    toReturn.reserve = new LinkedList<>();

    if (reserve.size() > 1
        || (reserve.size() == 1 && (treeSplittable() || takeAll))) {
      int toSteal = (reserve.size() + 1) / 2;
      while (toSteal > 0) {
        toReturn.reserve.add(reserve.poll());
        toSteal--;
      }
    } else {
      final BitNQueens loot = new BitNQueens();
      loot.stackAntiDiagonal = Arrays.copyOf(stackAntiDiagonal, N);
      loot.stackColumn = Arrays.copyOf(stackColumn, N);
      loot.stackDiagonal = Arrays.copyOf(stackDiagonal, N);
      loot.stackMask = Arrays.copyOf(stackMask, N);
      loot.treeUpperBound = Arrays.copyOf(treeUpperBound, N);

      int lootDepth = -1;
      int newDepth = -1;

      // Split the exploration tree
      for (int i = 0; i <= depth; i++) {
        final int options = treeUpperBound[i] - treeLowerBound[i];
        if (options >= 2) {
          treeUpperBound[i] -= options / 2;
          newDepth = i; // There is at least one node left on this level
          lootDepth = i;
        } else if (options == 1) {
          if (takeAll) {
            treeUpperBound[i]--; // Makes it equal to treeLowerBound[i];
            lootDepth = i;
          } else {
            newDepth = i; // The single node remaining is not given away
          }
        }
      }
      loot.treeLowerBound = Arrays.copyOf(treeUpperBound, N);

      loot.depth = lootDepth;
      depth = newDepth; // This makes the current instance backtrack to the last
      // level where there are nodes left in the exploration
      // tree (or -1 if all nodes were given away)
      toReturn.reserve.add(loot);
    }

    return toReturn;
  }

  /**
   * Performs one step in the backtrack search algorithm
   */
  public void step() {
    // Check if there are branches left to explore at the current level
    if (treeLowerBound[depth] < treeUpperBound[depth]) {
      // We pick the next candidate and create the corresponding mask for the
      // candidate queen
      final int mask = 1 << treeLowerBound[depth];
      treeLowerBound[depth]++; // This option at the current level is explored
      nodesExplored[depth]++;

      // We attempt to place it on the board
      if (0 == (mask & stackMask[depth])) {

        // Check if placing the queen would result in a solution
        if (depth == N - 1) {
          // We found a solution
          solutionsFound++;
        } else {
          // The candidate can be placed. We integrate it to the board and write
          // it to the stack
          stackColumn[depth + 1] = stackColumn[depth] | mask;
          stackAntiDiagonal[depth + 1] = (stackAntiDiagonal[depth] | mask) << 1;
          stackDiagonal[depth + 1] = (stackDiagonal[depth] | mask) >>> 1;
          stackMask[depth + 1] = stackDiagonal[depth + 1]
              | stackAntiDiagonal[depth + 1] | stackColumn[depth + 1];

          // We prepare the next level of exploration (allow all options)
          depth++;
          treeLowerBound[depth] = 0;
          treeUpperBound[depth] = N;
        }
      }
    } else {
      // There are no branches left to explore, we backtrack one level and
      // remove the previous queen placement
      depth--;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.multiworker.Bag#submit(handist.glb.util.Fold)
   */
  @Override
  public void submit(Answer r) {
    r.solutions += solutionsFound;
    for (int i = 0; i < N; i++) {
      r.nodes[i] += nodesExplored[i];
    }
  }

  @Override
  public String toString() {
    final String upper = Arrays.toString(treeUpperBound);
    final String lower = Arrays.toString(treeLowerBound);
    final int[] branchesLeft = new int[treeUpperBound.length];
    for (int i = 0; i < branchesLeft.length; i++) {
      branchesLeft[i] = treeUpperBound[i] - treeLowerBound[i];
    }
    final String branches = Arrays.toString(branchesLeft);
    final String nl = System.lineSeparator();
    return "Upper:" + upper + nl + "Lower:" + lower + nl + "Branches:"
        + branches + nl;

  }

  /**
   * Indicates if the current exploration has enough nodes to be split in
   * several exploration trees
   *
   * @return true if the current exploration
   */
  private boolean treeSplittable() {
    for (int i = 0; i <= depth; i++) {
      if (treeUpperBound[i] - treeLowerBound[i] >= 2) {
        return true;
      }
    }

    return false;
  }
}
