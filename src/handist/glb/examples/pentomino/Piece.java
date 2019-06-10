/**
 *
 */
package handist.glb.examples.pentomino;

import java.io.Serializable;

/**
 * Class representing a piece of the pentomino problem
 *
 * @author Patrick Finnerty
 *
 */
public abstract class Piece implements Comparable<Piece>, Serializable {

  /** Serial version UID */
  private static final long serialVersionUID = 335315376773627613L;

  /**
   * Returns the number of different ways in which this piece can be positioned
   *
   * @return the number of variants of thie piece
   */
  public abstract int variations();

  /**
   * Returns the ith variation of thie piece
   *
   * @param nb
   *          the ith desired variation
   * @return the piece as the offset indices that are filled by this part when
   *         placed on the board
   */
  public abstract int[] getVariation(int nb);

  /**
   * Prints all the variations of the piece on stdout
   *
   * @param width
   *          the width of the board on which the pieces are meant to be placed
   */
  public void printVariations(int width) {
    for (int i = 0; i < variations(); i++) {
      System.out.println("----------" + i + "------------");
      System.out.println(Piece.showPiece(getVariation(i), width));
    }
  }

  /**
   * Returns a printable String that represents the piece.
   *
   * @param a
   *          array
   * @param width
   *          width of the board
   * @return string that can be printed
   */
  protected static String showPiece(int[] a, int width) {
    String s = "";
    int index = 0;
    int printColumn = 0;
    int lineNb = 0;

    while (index < a.length) {
      int nextPrint = a[index];
      if (width <= nextPrint) {
        final int line = nextPrint / width;
        nextPrint = nextPrint % width;
        if (line > lineNb) {
          printColumn = 0;
          s += "\r\n";
          lineNb++;
        }

      }

      while (printColumn < nextPrint) {
        s += " ";
        printColumn++;
      }
      s += "#";
      printColumn++;

      index++;
    }

    return s;
  }

  @Override
  public String toString() {
    return "" + getChar();
  }

  /**
   * Gives a character used to describe the piece
   *
   * @return a character
   */
  public abstract char getChar();

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(Piece p) {
    return getChar() - p.getChar();
  }
}
