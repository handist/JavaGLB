/**
 *
 */
package handist.glb.examples.exactcover;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;

import handist.glb.examples.pentomino.Answer;
import handist.glb.multiworker.Bag;

/**
 * Implementation of the N-Queens problem
 *
 * @author Patrick Finnerty
 *
 */
public class NQueens implements Bag<NQueens, Answer>, Serializable {

  /** Serial Verion UID */
  private static final long serialVersionUID = -5534970349376619082L;

  /**
   * Generates a CoverMatrix instance of the N-Queens problem of the specified
   * size.
   *
   * @param size
   *          the number of queens to place, also equal to the width and height
   *          of the board used
   * @return a constructed {@link CoverMatrix} instance that models the N-Queens
   *         problem
   */
  public static QCoverMatrix generateCoverMatrix(int size) {
    final int nb_Diagonals = (size * 2) - 3;

    final String[] columns = new String[(size * 2) + (nb_Diagonals * 2)];
    int colIndex = 0;

    for (int i = 0; i < size; i++) {
      columns[colIndex++] = new String("F" + i);
    }
    final int rankOffset = colIndex;
    for (int i = 0; i < size; i++) {
      columns[colIndex++] = new String("R" + i);
    }
    final int diagonalOffset = colIndex - 1;
    for (int i = 1; i < (2 * size) - 2; i++) {
      columns[colIndex++] = new String("D" + i);
    }

    final int antiDiagonalOffset = colIndex - 1;
    for (int i = 1; i < (2 * size) - 2; i++) {
      columns[colIndex++] = new String("A" + i);
    }

    // Generating the possible placement of the NQueens
    final int[][] rows = new int[size * size][0];
    int rowsIndex = 0;
    for (int i = 0; i < size; i++) { // i is the file
      for (int j = 0; j < size; j++) { // j is the rank
        final int[] row;
        if ((i == 0 && j == 0) || (i == size - 1 && j == size - 1)) {
          // Diagonal is not needed
          row = new int[3];
          row[0] = i;
          row[1] = j + rankOffset;
          row[2] = size - 1 - i + j + antiDiagonalOffset;
        } else if ((i == size - 1 && j == 0) || (i == 0 && j == size - 1)) {
          // Antidiagonal is not needed
          row = new int[3];
          row[0] = i;
          row[1] = j + rankOffset;
          row[2] = i + j + diagonalOffset;

        } else {
          row = new int[4];
          row[0] = i;
          row[1] = j + rankOffset;
          row[2] = i + j + diagonalOffset;
          row[3] = size - 1 + i - j + antiDiagonalOffset;
        }
        rows[rowsIndex++] = row;
      }
    }
    return new QCoverMatrix(columns, diagonalOffset + 1, rows);
  }

  /**
   * Starts a N-Queens computation, N being the parameter to specify the size of
   * the problem to solve
   *
   * @param args
   *          size of the board
   */
  public static void main(String[] args) {
    int N = 8;
    try {
      N = Integer.parseInt(args[0]);
    } catch (final Exception e) {
    }

    if (N <= 4) {
      System.err.println(
          "This implementation can only handle N-Queens problem of size 5 and above");
      return;
    }

    final NQueens problem = new NQueens(N);
    problem.init();
    problem.toCompletion();

    System.out.println(problem.solutionCount);
    System.out.println(problem.nodeCount);

  }

  /** Depth of the exploration in progress */
  int depth;

  /**
   * Higher bound of the options left to explore at each level of the
   * exploration.
   *
   * @see #low
   */
  int[] high;

  /**
   * Lower bound of the options in terms of choice at each level of the
   * exploration. With array {@link #high}, it describes an interval of options
   * to explore at each level
   */
  int[] low;

  /** Models the problem */
  transient QCoverMatrix matrix;

  /** Size of the problem to solve */
  int N;
  /**
   * Counts the number of nodes in the tree explored in the exact cover problem
   */
  long nodeCount;

  /** Reserve of NQuens exploration fragments that have yet to be explore */
  transient Deque<NQueens> reserve;

  /** Counts the number of solutions to the N-Queens problem */
  transient long solutionCount = 0;

  /**
   * Stack used to keep the id of the rows of the cover matrix used (=piece
   * placed on the board)
   */
  int[] stack;

  /**
   * Array containing the id of the next option to explore in the backrack
   * exploration at each level. Used to differentiate branches when the
   * exploration is split for load-balancing purposes.
   */
  // int[] nextChoice;

  /**
   * Default constructor. Does not initialize any member or collection. Used in
   * the {@link #split(boolean)} method to create an instance which will be
   * transfered with the minimum number of information.
   */
  private NQueens() {
  }

  /**
   * Constructs a new {@link NQueens} problem instance with the specified size
   *
   * @param size
   *          the width of the board on which we are trying to place queens.
   */
  public NQueens(int size) {
    N = size;
    matrix = generateCoverMatrix(size);
    reserve = new LinkedList<>();
    depth = -1;
    stack = new int[N];
    // nextChoice = new int[N];
    low = new int[N];
    high = new int[N];

    solutionCount = 0;
    nodeCount = 0;
  }

  /**
   * Set this instance to be in a state at which it will compute the whole
   * problem
   */
  public void init() {
    depth = 0;
    low[0] = 0;
    high[0] = N;
    final QCell c = matrix.root.right;
    // nextChoice[0] = c.index;
    stack[0] = c.index;
  }

  /**
   * Computes this instance to completion
   */
  public void toCompletion() {
    while (0 <= depth) {
      step();
    }
  }

  /**
   * Performs one step in the exploration of the exact cover problem
   */
  private void step() {
    if (choiceLeft() > 0) {
      nodeCount++;
      // We pick the next choice
      final QCell oldChoice = matrix.cells[stack[depth]];

      if (!oldChoice.isHeader()) {
        // The check removes the case where it is actually the first time we are
        // trying to fill this column
        oldChoice.unchooseRow();
      } else {
        oldChoice.coverColumn();
      }
      final QCell next = oldChoice.down;

      // Place the choice on the stack
      stack[depth] = next.index;
      low[depth]++;

      // Try to apply the choice on the matrix
      next.chooseRow();
      if (matrix.hasHope()) {
        depth++; // It is still possible to find a solution, we continue the
                 // exploration

        if (depth == N) {
          // We managed to cover all the files and rank !
          solutionCount++;
          printStack();

          depth--;
        } else {
          // Prepare the next level
          low[depth] = 0;
          final QCell c = matrix.root.right;
          high[depth] = c.size;
          stack[depth] = c.index;
        }
      } // end of if
      // Choosing the considered row resulted in a column not capable of being
      // covered later. The next possible row will be tested at the current
      // level in the next call to this method
    } else {
      // We have explored all the options of the current level
      // We uncover the column of the current level and backtrack
      final QCell oldChoice = matrix.cells[stack[depth]];
      final QCell column = oldChoice.column;
      oldChoice.unchooseRow();
      column.recoverColumn();

      depth--;
    }
  }

  /**
   * Prints the whole stack
   */
  private void printStack() {
    for (int i = 0; i < depth; i++) {
      System.out.println(matrix.cells[stack[i]]);
    }
    System.out.println();
  }

  /**
   * Returns the number of options left for the choice at the current depth.
   *
   * @return the number of choices left at the current level of exploration
   */
  private int choiceLeft() {
    return high[depth] - low[depth];
  }

  /**
   * Returns the number of options left to explore at the specified level.
   *
   * @param level
   *          the level to consider
   * @return the number of rows that are still candidate for exploration
   */
  private int choiceLeft(int level) {
    return high[level] - low[level];
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
    if (matrix == null) {
      return false;
    }
    int leaves = 0;
    for (int i = 0; i <= depth; i++) {
      leaves += choiceLeft(i);

      if (leaves > 1) {
        return true;
      }
    }
    return false;
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.multiworker.Bag#merge(handist.glb.multiworker.Bag)
   */
  @Override
  public void merge(NQueens b) {
    reserve.add(b);
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.multiworker.Bag#process(int, handist.glb.util.Fold)
   */
  @Override
  public void process(int workAmount, Answer sharedObject) {
    while (workAmount > 0 && !isEmpty()) {
      if (depth < 0) {
        takeFromReserve();
      } else {
        step();
      }
      workAmount--;
    }
  }

  /**
   * Takes a NQueens partial exploration stored in the {@link #reserve} and
   * converts this instance to reflect the state of what was polled from the
   * {@link #reserve}.
   */
  void takeFromReserve() {
    final NQueens loot = reserve.pop();
    stack = loot.stack;
    low = loot.low;
    high = loot.high;
    depth = loot.depth;

    // Put the matrix in the state it should be
    for (int i = 0; i <= depth; i++) {
      final QCell row = matrix.cells[stack[i]];
      if (!row.isHeader()) {

        row.column.coverColumn();
        row.chooseRow();
      }
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.multiworker.Bag#split(boolean)
   */
  @Override
  public NQueens split(boolean takeAll) {
    NQueens loot = null;
    if (reserve != null) {
      loot = reserve.pollFirst();
    }

    if (loot == null) {
      // We need to split the current exploration
      loot = new NQueens();
      loot.N = N;
      loot.depth = -1;
      loot.low = new int[N];
      loot.high = new int[N];
      loot.stack = Arrays.copyOf(stack, N);

      for (int level = 0; level <= depth; level++) {

        final int options = choiceLeft(level);

        if (options > 1) {
          loot.high[level] = high[level];
          loot.low[level] = high[level] -= options / 2;

          QCell c = matrix.cells[stack[level]];
          for (int gap = loot.low[level] - low[level]; gap > 0; gap--) {
            c = c.down;
          }
          loot.stack[level] = c.index;

          loot.depth = level;
          return loot;
        } else if (options == 1 && takeAll) {
          loot.low[level] = low[level];
          loot.high[level] = high[level];

          high[level]--; // This removes the node from this instance
          loot.depth = level;
          return loot;
        }
      }
    }

    return loot;
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.multiworker.Bag#submit(handist.glb.util.Fold)
   */
  @Override
  public void submit(Answer r) {
    r.nodes += nodeCount;
    r.solutions += solutionCount;
  }

}
