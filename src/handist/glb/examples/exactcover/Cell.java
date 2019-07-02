/**
 *
 */
package handist.glb.examples.exactcover;

/**
 * @author Patrick Finnerty
 *
 */
public class Cell {

  /** Link towards the next left cell on the same line */
  Cell left;

  /** Link towards the next right cell on the same line */
  Cell right;

  /** Link towards the next cell up in the same column */
  Cell up;

  /** Link towards the next cell down in the same column */
  Cell down;

  /** Pointer to the {@link Column} this cell belongs to */
  Column column;

  /**
   * Creates an empty cell
   */
  public Cell() {
    left = this;
    right = this;
    up = this;
    down = this;
    column = null;
  }

  /**
   * Makes all the elements in the row fade from the matrix by linking the
   * element above and below in their respective column
   */
  public void coverRow() {
    Cell r = right;
    while (r != this) {
      r.up.down = r.down;
      r.down.up = r.up;
      r.column.size--;
      r = r.right;
    }
  }

  /**
   * Reverts the effects of {@link #coverRow()}
   */
  public void uncoverRow() {
    Cell l = left;
    while (l != this) {
      l.up.down = l;
      l.down.up = l;
      l.column.size++;
      l = l.left;
    }
  }

  /**
   * Covers all the columns in which the other cells in the row are involved
   */
  public void chooseRow() {
    Cell c = right;
    while (c != this) {
      c.column.coverColumn();
      c = c.right;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    String s = column.name + " ";
    Cell r = right;
    while (r != this) {
      s += r.column.name + " ";
      r = r.right;
    }

    return s;
  }

  /**
   * Makes all the columns that were covered by this row appear. Reverts the
   * effects of {@link #chooseRow()}.
   */
  public void unchooseRow() {
    Cell c = left;
    while (c != this) {
      c.column.recoverColumn();
      c = c.left;
    }
  }
}
