/**
 *
 */
package handist.glb.examples.exactcover;

/**
 * Root object of the cover matrix. This class provides all the services to the
 * user.
 *
 * @author Patrick Finnerty
 *
 */
public class QCoverMatrix {

  /**
   * Array containing all the cells of this cover matrix, including the column
   * headers.
   */
  QCell[] cells;

  /** Placeholder for the column headers */
  QCell root;

  /**
   * Index at which the columns that represent the diagonals and antidiagonals
   * in the board start
   */
  private final int columnCheck;

  /**
   * Constructor for the QCoverMatrix.
   *
   * The constructed matrix will contain one column for each element in the
   * array of names given to the column. Each array in the collection of rows
   * must indicate the index of the column in which a cell must be created. The
   * indices in the arrays should be sorted by increasing number.
   *
   * @param columnNames
   *          array containing the names of each individual column of the matrix
   * @param columnCheckLimit
   *          indicates that only the n'th first columns in the matrix
   *          absolutely have to be covered ; setting the value to the length of
   *          array columnNames will enforce solutions to the matrix cover all
   *          columns
   *
   * @param rows
   *          collection of rows to be created
   */
  public QCoverMatrix(String[] columnNames, int columnCheckLimit,
      int[][] rows) {
    // First, we compute the necessary length for array "cells"
    // We then initialize it
    int totalNbCells = columnNames.length;
    columnCheck = columnCheckLimit;
    for (final int[] row : rows) {
      totalNbCells += row.length;
    }
    cells = new QCell[totalNbCells];
    int cellsIndex = 0;

    // Create the root header
    root = QCell.newColumn("root", -1);
    root.left = root;
    root.right = root;

    // First we create the column headers
    for (int i = 0; i < columnNames.length; i++) {
      final QCell c = QCell.newColumn(columnNames[i], cellsIndex);
      cells[cellsIndex++] = c;
      c.left = root.left;
      c.right = root;
      c.left.right = c;
      c.right.left = c;

    }

    // We now insert rows
    for (int r = 0; r < rows.length; r++) {
      final int[] indices = rows[r];

      // Create doubly linked cells for the new row
      final QCell first = QCell.newCell(cellsIndex);
      cells[cellsIndex++] = first;
      QCell lastCreated = first;
      for (int cellsToCreate = indices.length
          - 1; cellsToCreate > 0; cellsToCreate--) {
        final QCell newCell = QCell.newCell(cellsIndex);
        cells[cellsIndex++] = newCell;
        lastCreated.right = newCell;
        newCell.left = lastCreated;

        lastCreated = newCell;
      }
      lastCreated.right = first;
      first.left = lastCreated;

      // Put the cells in the proper column
      QCell toPlace = first;
      for (final int index : indices) {
        cells[index].insert(toPlace);
        toPlace = toPlace.right;
      }
    }
  }

  /**
   * Checks if the size of the remaining columns are consistent with the number
   * of cells in the columns linked lists.
   *
   */
  void checkConsistency() {
    for (QCell c = root.right; c != root; c = c.right) {
      int counter = 0;
      for (QCell r = c.down; r != c; r = r.down) {
        counter++;
      }
      if (counter != c.size) {
        System.err.println("Column " + c + " has a problem");
      }
    }

  }

  /**
   * Indicates if there are columns left to cover in the matrix
   *
   * @return true if there are still columns left to cover
   */
  public boolean hasColumns() {
    return root.right != root;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    String s = "";
    QCell c = root.right;
    while (c != root) {
      s += c + " ";
      c = c.right;
    }
    /*
     * s += "\r\n";
     *
     * c = root.right; while (c != root) { Cell row = c.down; while (row != c) {
     * s += row + "\r\n"; row = row.down; } s += "___\r\n"; c = c.right; }
     */

    return s;

  }

  /**
   * Indicates if there is hope of finding a solution to the matrix. Checks that
   * every column has at least one row still under it. If one column is found to
   * have now row that can cover it, returns false. Otherwise returns true.
   * <p>
   * Note that in this special implementation for the N-Queens problem, only the
   * matrix columns corresponding to the rows and columns of the board are
   * checked. Matrix columns for the diagonals and antidiagonals are not checked
   * as they are considered additional contraints and solutions to the N-Queens
   * problem cannot by nature cover all the (anti-)diagonals.
   *
   * @return true is there is a chance of finding a solution to the matrix,
   *         false otherwise
   */
  public boolean hasHope() {
    QCell col = root.right;
    while (col != root && col.index < columnCheck) {
      if (col.size <= 0) {
        return false;
      }
      col = col.right;
    }
    return true;
  }
}
