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
public class CoverMatrix {

  /** Link to the last column of the matrix */
  Column root;

  /** Array containing the first cell of each line */
  Cell[] firstCells;

  /**
   * Constructor for the CoverMatrix.
   *
   * The constructed matrix will contain one column for each element in the
   * array of names given to the column. Each array in the collection of rows
   * must indicate the index of the column in which a cell must be created. The
   * indices in the arrays should be sorted by increasing number.
   *
   * @param columnNames
   *          array containing the names of each individual column of the matrix
   * @param rows
   *          collection of rows to be created
   */
  public CoverMatrix(String[] columnNames, int[][] rows) {
    // Create the root header
    root = new Column("root");
    root.left = root;
    root.right = root;

    // First we create the column headers
    final Column[] columns = new Column[columnNames.length];
    for (int i = 0; i < columnNames.length; i++) {
      final Column c = new Column(columnNames[i]);
      c.left = root.left;
      c.right = root;
      c.left.right = c;
      c.right.left = c;

      columns[i] = c; // Easier access for the row insertions later
    }

    // We now insert rows
    firstCells = new Cell[rows.length];
    for (int r = 0; r < rows.length; r++) {
      final int[] indices = rows[r];

      // Create doubly linked cells for the new row
      final Cell first = new Cell();
      firstCells[r] = first;
      Cell lastCreated = first;
      for (int cellsToCreate = indices.length
          - 1; cellsToCreate > 0; cellsToCreate--) {
        final Cell newCell = new Cell();
        lastCreated.right = newCell;
        newCell.left = lastCreated;

        lastCreated = newCell;
      }
      lastCreated.right = first;
      first.left = lastCreated;

      // Put the cells in the proper column
      Cell toPlace = first;
      for (final int index : indices) {
        columns[index].insert(toPlace);
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
    for (Column c = (Column) root.right; c != root; c = (Column) c.right) {
      int counter = 0;
      for (Cell r = c.down; r != c; r = r.down) {
        counter++;
      }
      if (counter != c.size) {
        System.err.println("Column " + c + " has a problem");
      }
    }

  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    String s = "";
    Cell c = root.right;
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
   * Indicates if there are columns left to cover in the matrix
   *
   * @return true if there are still columns left to cover
   */
  public boolean hasColumns() {
    return root.right != root;
  }
}
