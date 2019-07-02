/**
 *
 */
package handist.glb.examples.exactcover;

import java.util.Stack;

/**
 * @author Patrick Finnerty
 *
 */
public class Test {

  /** Matrix being the subject of the test */
  CoverMatrix matrix;

  /** Keeps track of the number of solutions to the exact cover problem */
  long numberSolutions;

  /** Keeps track of the rows that were selected to cover the matrix */
  Stack<Cell> stack;

  /**
   * Chooses the next column in the matrix to be covered and returns it. Returns
   * null if a column cannot be covered.
   *
   * @return the column header of the chosen column
   */
  Column chooseColumn() {
    Column c = (Column) matrix.root.right;
    Column chosen = c;
    int min = c.size;
    c = (Column) c.right;
    while (c != matrix.root) {
      if (c.size < min) {
        chosen = c;
        min = chosen.size;
      }
      c = (Column) c.right;
    }
    // If no row can cover the column, return null
    if (min == 0) {
      return null;
    }
    return chosen;
  }

  /**
   * Finds all the solutions to exactly cover {@link #matrix}
   *
   * @param step
   *          parameter containing the depth of the search
   */
  void findSolutions(int step) {
    System.err.println(matrix);
    System.err.println(printStack());
    matrix.checkConsistency();
    if (matrix.hasColumns()) {
      Column c = null;
      if ((c = chooseColumn()) != null) {
        System.err.println("Chosen column " + c);
        c.coverColumn();
        System.err.println(matrix);
        Cell r = c.down;
        do {
          // We select row 'r'
          stack.add(r);
          r.chooseRow();
          matrix.checkConsistency();
          // Recursive call
          System.err.println("Row chosen: [" + r + "]");
          // System.err.println(matrix);
          findSolutions(step + 1);

          // We unselect the row
          r.unchooseRow();
          matrix.checkConsistency();
          stack.pop();

          r = r.down;
        } while (r != c);
        // All possible rows for the given column were tested, we backtrack
        c.recoverColumn();
      } else {
        // Cul de sac, backtrack
      }
    } else {
      // We found a solution
      numberSolutions++;
      System.out.println("Solution " + printStack());
    }
  }

  /**
   * Formats the stack to a printable String format
   *
   * @return printable String representing the stack
   */
  String printStack() {
    String s = "";
    for (final Cell c : stack) {
      s += "[" + c + "]";
    }

    return s;
  }

  /**
   * Main
   *
   * @param args
   *          unused
   */
  public static void main(String[] args) {
    final String[] columnNames = { "a", "b", "c", "d", "e" };
    final int[] row0 = { 2, 4 };
    final int[] row1 = { 0, 1 };
    final int[] row2 = { 1, 2 };
    final int[] row3 = { 3, 4 };
    final int[] row4 = { 0, 1, 2 };
    final int[] row5 = { 0, 1, 3, 4 };
    final int[] row6 = { 0, 1, 2, 3, 4 };
    final int[] row7 = { 2, 3, 4 };
    final int[] row8 = { 2 };

    final int[][] rows = new int[9][0];
    rows[0] = row0;
    rows[1] = row1;
    rows[2] = row2;
    rows[3] = row3;
    rows[4] = row4;
    rows[5] = row5;
    rows[6] = row6;
    rows[7] = row7;
    rows[8] = row8;

    final CoverMatrix matrix = new CoverMatrix(columnNames, rows);
    for (final Cell c : matrix.firstCells) {
      System.out.println(c);
    }
    System.out.println(matrix);
    matrix.checkConsistency();
    final Test exactCover = new Test(matrix);
    exactCover.findSolutions(0);

  }

  /**
   * Constructor for the test
   *
   * @param m
   *          the matrix which is going to be tested
   */
  public Test(CoverMatrix m) {
    matrix = m;
    numberSolutions = 0;
    stack = new Stack<>();
  }
}
