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

/**
 * Root object of the cover matrix. T
 *
 * @author Patrick Finnerty
 *
 */
public class QCoverMatrix {

  /**
   * Array containing all the cells of this cover matrix, including the column
   * headers. This allows us to refer to specific cells of the matrix by their
   * index in this array rather than using pointers. This allows us to avoid
   * copying the entire cover matrix when transferring work in the distributed
   * execution.
   */
  QCell[] cells;

  /** Placeholder to connect to the matrix's column headers */
  QCell root;

  /**
   * Index at which the columns that represent the diagonals and anti-diagonals
   * in the board start. As it is not possible to cover all the diagonals and
   * anti-diagonals of the chess board in the N-Queens problem, the
   * corresponding columns in the cover matrix are allowed to remain uncovered.
   * As a result, only the columns of the matrix that correspond to the ranks
   * and files of the board need to be covered.
   *
   * @see #hasHope()
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
   * of cells in the columns linked lists. Any discrepancy is printed to the
   * standard error output.
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

  /**
   * Build a String showing all the columns that have yet to be covered in the
   * matrix.
   */
  @Override
  public String toString() {
    String s = "";
    QCell c = root.right;
    while (c != root) {
      s += c + " ";
      c = c.right;
    }
    return s;

  }

  /**
   * Indicates if there is hope of finding a solution to the matrix. Checks that
   * every column has at least one row still under it. If one column is found to
   * have no row that can cover it, returns false. Otherwise returns true.
   * <p>
   * Note that in this special implementation dedicated to the N-Queens problem,
   * only the matrix columns corresponding to the rows and columns of the board
   * are checked. Matrix columns for the diagonals and anti-diagonals are not
   * checked as they are considered "additional constraints" and solutions to
   * the N-Queens problem cannot by nature cover all of the (anti-)diagonals.
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
