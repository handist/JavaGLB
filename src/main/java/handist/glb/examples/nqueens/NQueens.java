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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;

import handist.glb.Bag;
import handist.glb.examples.pentomino.Answer;

/**
 * Implementation of the N-Queens problem using the dancing links data
 * structure.
 *
 * The main method of this class will launch a sequential execution of the
 * problem. To launch a multithreaded and distributed execution using the global
 * load balancer library, consider using class {@link ParallelNQueens}'s main
 * method.
 *
 * @author Patrick Finnerty
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
   * @return a constructed {@link QCoverMatrix} instance that models the
   *         N-Queens problem
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
   * the problem to solve. With a second (optional) parameter, the user can
   * specify the number of times the problem will be performed.
   *
   * @param args
   *          size of the problem, optionally the number of repetitions
   */
  public static void main(String[] args) {
    int N;
    int repetitions;
    try {
      N = Integer.parseInt(args[0]);
    } catch (final Exception e) {
      System.err.println("Error while parsing the arguments");
      System.err.println("Args <N>");
      return;
    }
    try {
      repetitions = Integer.parseInt(args[1]);
    } catch (final Exception e) {
      repetitions = 1;
    }

    if (N <= 4) {
      System.err.println(
          "This implementation can only handle N-Queens problem of size 5 and above");
      return;
    }

    System.out.println("N=" + N);
    System.out.println("Run;Solutions;Nodes;Init (s);Computation (s);");
    for (int i = 0; i < repetitions; i++) {
      final long initStart = System.nanoTime();
      final NQueens problem = new NQueens(N);
      problem.init();
      final long compStart = System.nanoTime();
      problem.toCompletion();
      final long compEnd = System.nanoTime();

      final long init = compStart - initStart;
      final long comp = compEnd - compStart;

      long treeSize = 0;
      for (final long n : problem.nodeCount) {
        treeSize += n;
      }

      System.out.println(i + "/" + repetitions + ";" + problem.solutionCount
          + ";" + treeSize + ";" + init / 1e9 + ";" + comp / 1e9 + ";");
    }
  }

  /**
   * Counts the number of branches at each level in the exploration tree
   */
  long[] branchCount;

  /**
   * Current depth of the exploration in progress. Corresponds to the index in
   * the various arrays used to describe the exploration. A value of {@code -1}
   * indicates that the exploration is finished.
   */
  int depth;

  /**
   * Higher bound of the options left to explore at each level of the
   * exploration.
   *
   * @see #low
   */
  int[] high;

  /**
   * Lower bound of the options at each level of the exploration. With array
   * {@link #high}, it describes an interval of branches in the exploration tree
   * that remain to explore at each level.
   */
  int[] low;

  /**
   * Model of the problem. Is made transient so that instances of
   * {@link NQueens} used to carry information between {@link #split(boolean)}
   * and {@link #merge(NQueens)} calls do not serialize this field.
   */
  transient QCoverMatrix matrix;

  /** Size of the problem to solve */
  int N;

  /**
   * Keeps track of the next branch of the exploration tree to take when
   * backtracking.
   */
  int[] nextNode;

  /**
   * Counts the number of nodes at each level in the exploration tree
   */
  long[] nodeCount;

  /**
   * Reserve of {@link NQueens} exploration fragments that have yet to be
   * explored. When a {@link #merge(NQueens)} operation is performed on an
   * instance which already contains an exploration tree, the received work is
   * put into this reserve.
   *
   * @see #merge(NQueens)
   */
  Deque<NQueens> reserve;

  /** Counts the number of solutions to the N-Queens problem. */
  transient long solutionCount = 0;

  /**
   * Stack used to keep the id of the rows of the cover matrix used (equivalent
   * to a piece being placed on the board).
   */
  int[] stack;

  /**
   * Default constructor. Does not initialize any member. Used in the
   * {@link #split(boolean)} method to use an instance which will contain only
   * the minimum amount of information.
   */
  NQueens() {
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
    nextNode = new int[N];
    low = new int[N];
    high = new int[N];

    solutionCount = 0;
    nodeCount = new long[size];
    branchCount = new long[size];
  }

  /**
   * Returns the number of branches left to explore at the current depth.
   *
   * @return the number of choices left at the current level of exploration
   */
  private int choiceLeft() {
    return high[depth] - low[depth];
  }

  /**
   * Returns the number of branches to explore at the specified level.
   *
   * @param level
   *          the level to consider
   * @return the number of rows that are still candidate for exploration
   */
  private int choiceLeft(int level) {
    return high[level] - low[level];
  }

  /**
   * Set this instance to be in a state at which it contains the whole problem.
   */
  public void init() {
    depth = 0;
    low[0] = 0;
    high[0] = N;
    final QCell c = matrix.root.right;
    stack[0] = c.index;
    nextNode[0] = c.down.index;
  }

  /**
   * A N-Queens exploration is empty if the current exploration is finished and
   * if there are no partial exploration left in the {@link #reserve}.
   *
   * @return true if this instance does not contain any work, false otherwise
   */
  @Override
  public boolean isEmpty() {
    return depth < 0 && reserve.isEmpty();
  }

  /**
   * Indicates if this instance of N-Queens can be split, that is if work can be
   * taken from this instance without emptying it completely.
   *
   * @return true if it is possible to split this instance while leaving some
   *         amount of work in this instance, false otherwise.
   */
  @Override
  public boolean isSplittable() {
    return reserve.size() > 1 || treeSplittable();
  }

  /**
   * Merges the content of the given instance into this instance.
   */
  @Override
  public void merge(NQueens b) {
    if (depth == -1) {
      restore(b.reserve.poll());
    }
    reserve.addAll(b.reserve);

  }

  /**
   * Prints the whole stack of the current exploration
   */
  public void printStack() {
    for (int i = 0; i < depth; i++) {
      System.out.println(matrix.cells[stack[i]]);
    }
    System.out.println();
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
        restore(reserve.pop());
      } else {
        step();
      }
      workAmount--;
    }
  }

  /**
   * Takes a NQueens partial exploration and converts this instance to reflect
   * the state of this instance. After calling this method, this instance will
   * be able to continue the exploration that was given as parameter.
   *
   * @param loot
   *          the partial exploration to convert this instance to
   */
  void restore(NQueens loot) {
    stack = loot.stack;
    low = loot.low;
    high = loot.high;
    depth = loot.depth;
    nextNode = loot.nextNode;

    // Put the matrix in the state it should be
    for (int i = 0; i <= depth; i++) {
      final QCell row = matrix.cells[stack[i]];
      if (!row.isHeader()) {

        row.column.coverColumn();
        row.chooseRow();
      }
    }
  }

  /**
   * Split the current {@link NQueens} instance. If this instance has some
   * partial exploration in its {@link #reserve}, half of the reserve is given
   * away. If not, half of the leaf nodes of this current exploration are given
   * away.
   */
  @Override
  public NQueens split(boolean takeAll) {
    final NQueens toReturn = new NQueens();
    toReturn.reserve = new LinkedList<>();

    if (reserve.size() > 1
        || (reserve.size() == 1 && (treeSplittable() || takeAll))) {
      int toSteal = (reserve.size() + 1) / 2;
      while (toSteal > 0) {
        toReturn.reserve.add(reserve.poll());
        toSteal--;
      }
    } else {
      // We need to split the current exploration
      final NQueens loot = new NQueens();
      loot.N = N;
      loot.depth = -1;
      loot.low = new int[N];
      loot.high = new int[N];
      loot.stack = Arrays.copyOf(stack, N);
      loot.nextNode = Arrays.copyOf(nextNode, N);

      boolean alternator = takeAll;

      for (int level = 0; level <= depth; level++) {
        final int options = choiceLeft(level);

        if (options > 1) {
          loot.high[level] = high[level];
          loot.low[level] = high[level] -= options / 2;

          QCell c = matrix.cells[nextNode[level]];
          for (int gap = loot.low[level] - low[level]; gap > 0; gap--) {
            c = c.down;
          }
          loot.nextNode[level] = c.index;

          loot.depth = level;

        } else if (options == 0) {
          loot.high[level] = high[level];
          loot.low[level] = high[level];
        } else if (options == 1) {
          if (alternator) {

            loot.low[level] = low[level];
            loot.high[level] = high[level];

            high[level]--; // This removes the node from this instance
            loot.depth = level;

            if (depth == level) {
              // The deepest leaf of this instance was given away. We need to
              // backtrack all the way and restore the matrix
              while (depth >= 0 && choiceLeft() <= 0) {
                final QCell row = matrix.cells[stack[depth]];
                if (!row.isHeader()) {
                  row.unchooseRow();
                  row.column.recoverColumn();
                }
                depth--;
              }
            }
          }
          alternator = !alternator;
        }
      }
      toReturn.reserve.add(loot);
    }
    return toReturn;
  }

  /**
   * Performs one step in the exploration of the exact cover problem
   */
  private void step() {
    if (choiceLeft() > 0) {
      nodeCount[depth]++;
      // We pick the next choice
      final QCell oldChoice = matrix.cells[stack[depth]];

      if (!oldChoice.isHeader()) {
        // The check removes the case where it is actually the first time we are
        // trying to fill this column
        oldChoice.unchooseRow();
      } else {
        oldChoice.coverColumn();
      }
      final QCell next = matrix.cells[nextNode[depth]];
      nextNode[depth] = next.down.index;

      // Place the choice on the stack
      stack[depth] = next.index;
      low[depth]++;

      // Try to apply the choice on the matrix
      next.chooseRow();
      if (matrix.hasHope()) {
        branchCount[depth]++;
        depth++; // It is still possible to find a solution, we continue the
                 // exploration

        if (depth == N) {
          // We managed to cover all the files and rank !
          solutionCount++;
          // printStack();

          depth--;
        } else {
          // Prepare the next level
          low[depth] = 0;
          final QCell c = matrix.root.right;
          high[depth] = c.size;
          stack[depth] = c.index;
          nextNode[depth] = c.down.index;
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
   * Puts the number of nodes explored and branches found in the exploration
   * that was performed by this instance as well as the number of solutions
   * found by this instance into the {@link Answer} instance provided.
   */
  @Override
  public void submit(Answer r) {
    r.solutions += solutionCount;
    for (int i = 0; i < nodeCount.length; i++) {
      r.nodes[i] += nodeCount[i];
      r.branch[i] += branchCount[i];
    }
  }

  /**
   * Computes the contained exploration until completion sequentially.
   */
  public void toCompletion() {
    while (0 <= depth) {
      step();
    }
  }

  /**
   * Helper method for {@link #isSplittable()} which checks if the current
   * exploration contains at least two leaf nodes, in which case the exploration
   * tree can be split.
   *
   * @return true if the current exploration tree can be split, false otherwise.
   */
  private boolean treeSplittable() {
    int leaves = 0;
    for (int i = 0; i <= depth; i++) {
      leaves += choiceLeft(i);

      if (leaves > 1) {
        return true;
      }
    }
    return false;
  }

}
