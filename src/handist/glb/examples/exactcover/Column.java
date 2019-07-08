/**
 *
 */
package handist.glb.examples.exactcover;

/**
 * Header used to maintain
 *
 * @author Patrick Finnerty
 *
 */
public class Column extends Cell {

  /** Name given to the column */
  String name;

  /** Number of cells in the column */
  int size;

  /**
   * Constructor
   *
   * @param n
   *          name to be given to the column
   */
  public Column(String n) {
    super();
    name = n;
    size = 0;
  }

  /**
   * Covers this column, removing all the rows that can potentially cover this
   * column from the matrix.
   */
  void coverColumn() {
    left.right = right;
    right.left = left;
    Cell row = down;

    while (row != this) {
      row.coverRow();

      row = row.down;
    }
  }

  /**
   * Reverts the effects of {@link #coverColumn()}
   */
  void recoverColumn() {
    Cell row = up;
    while (row != this) {
      row.uncoverRow();
      row = row.up;
    }

    left.right = this;
    right.left = this;
  }

  /**
   * Inserts a cell into the column
   *
   * @param c
   *          cell to insert in the doubly linked list of the column
   */
  public void insert(Cell c) {
    size++;
    c.column = this;

    c.up = up;
    c.down = this;
    c.up.down = c;
    up = c;

  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return name + ":" + size;
  }
}
