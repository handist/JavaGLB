/*
 *  This file is part of the Handy Tools for Distributed Computing project
 *  HanDist (https://github.com/handist)
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  (C) copyright CS29 Fine 2018-2019.
 */
package handist.glb.examples.nqueens;

/**
 * Class used both as column header and cell in the {@link QCoverMatrix}.
 *
 * In both use cases, members {@link #left}, {@link #right}, {@link #up} and
 * {@link #down} are used to make the doubly linked data structure.
 * <p>
 * <em>If an instance is used as a cell in the matrix</em>
 * <ul>
 * <li>Members {@link #size} and {@link #name} remain unused.
 * <li>Member {@link #column} points to the QCell instance used as column header
 * of which this cell is a member of.
 * </ul>
 * <p>
 * <em>If an instance is used a column header</em>
 *
 * <p>
 * Using the same class for two different roles is bad practice but in this case
 * it makes programming the exact cover problem more elegant.
 *
 * @author Patrick Finnerty
 *
 */
public class QCell {

  /**
   * Factory method for a QCell instance used as a regular cell in rows of the
   * {@link QCoverMatrix}. Pointers {@link #left}, {@link #right}, {@link #up}
   * and {@link #down} are left uninitialized. Pointer {@link #column} is also
   * left to {@code null}.
   * <p>
   * There is no use for a cell to have a name, therefore {@link #name} is left
   * to {@code null}. Likewise the {@link #size} value is left to the default 0
   * but is unused under normal operation.
   *
   * @param i
   *          index of the returned cell in the array containing all the
   *          {@link QCell} of the matrix
   * @return a new {@link QCell} instance meant to be used as a cell
   *
   */
  public static QCell newCell(int i) {
    final QCell cell = new QCell();
    cell.index = i;
    return cell;
  }

  /**
   * Factory method for a QCell instance used as a column header. Initializes
   * all fields except pointers {@link #left}, {@link #right}.
   *
   * @param string
   *          name to give to the column
   * @param i
   *          index of the {@link QCell} instance in {@link QCoverMatrix#cells}
   * @return a new {@link QCell} instance meant to be used as a column header
   */
  public static QCell newColumn(String string, int i) {
    final QCell column = new QCell();
    column.name = string;
    column.index = i;
    column.size = 0;
    column.up = column;
    column.down = column;
    return column;
  }

  /**
   * Pointer to the cell used as header for the column in which this instance is
   * located
   */
  QCell column;

  /** Pointer to the cell located under this instance */
  QCell down;

  /**
   * Index in the array containing all the cells of the {@link QCoverMatrix}.
   */
  int index;

  /** Pointer to the cell located to the left of this instance */
  QCell left;

  /** Name given to the column */
  String name;

  /** Pointer to the cell located to the right of this instance */
  QCell right;

  /**
   * Number of cells in the column
   */
  int size;

  /** Pointer to the cell located above this instance */
  QCell up;

  /**
   * Declaration of the constructor as Private to prevent usage. Instead,
   * factory methods {@link #newCell(int)} and {@link #newColumn(String, int)}
   * are used.
   */
  private QCell() {
  }

  /**
   * Covers all the columns in which the other cells in the row are involved.
   * This method is called to reflect the fact that the row to which this cell
   * belongs to is chosen as a candidate to cover the matrix. Calling this
   * method hides the column that are covered by this row as well as all the
   * other rows that could have covered these columns and can therefore not be
   * chosen anymore.
   */
  public void chooseRow() {
    QCell c = right;
    while (c != this) {
      c.column.coverColumn();
      c = c.right;
    }
  }

  /**
   * Covers this column, removing all the rows that can potentially cover this
   * column from the matrix.
   */
  void coverColumn() {
    left.right = right;
    right.left = left;
    QCell row = down;

    while (row != this) {
      row.coverRow();

      row = row.down;
    }
  }

  /**
   * Makes all the elements in the row this cell belongs to fade from the cover
   * matrix by linking the element above and below in their respective column.
   * This method is used to discard rows that would cover a column already
   * covered by an other row.
   */
  public void coverRow() {
    QCell r = right;
    while (r != this) {
      r.up.down = r.down;
      r.down.up = r.up;
      r.column.size--;
      r = r.right;
    }
  }

  /**
   * Inserts a cell into the column
   *
   * @param c
   *          cell to insert in the doubly linked list of the column
   */
  public void insert(QCell c) {
    size++;
    c.column = this;

    c.up = up;
    c.down = this;
    c.up.down = c;
    up = c;

  }

  /**
   * Indicates if this instance is a column header or a regular cell of the
   * matrix
   *
   * @return {@code true} if this instance is a column header, {@code false}
   *         otherwise
   */
  public boolean isHeader() {
    return column == null;
  }

  /**
   * Reverts the effects of {@link #coverColumn()}
   */
  void recoverColumn() {
    QCell row = up;
    while (row != this) {
      row.uncoverRow();
      row = row.up;
    }

    left.right = this;
    right.left = this;
  }

  /*
   * Returns a String containing in one line the columns of the matrix that are
   * covered by the row this cell belongs to.
   */
  @Override
  public String toString() {
    if (isHeader()) {
      return name + ":" + size;
    } else {
      String s = column.name;
      for (QCell r = right; r != this; r = r.right) {
        s += " " + r.column.name;
      }
      return s;
    }

  }

  /**
   * Makes all the columns that were covered by this row appear. Reverts the
   * effects of {@link #chooseRow()}.
   */
  public void unchooseRow() {
    QCell c = left;
    while (c != this) {
      c.column.recoverColumn();
      c = c.left;
    }
  }

  /**
   * Reverts the effects of {@link #coverRow()}.
   */
  public void uncoverRow() {
    QCell l = left;
    while (l != this) {
      l.up.down = l;
      l.down.up = l;
      l.column.size++;
      l = l.left;
    }
  }
}
