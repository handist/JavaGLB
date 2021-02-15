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
package handist.glb.examples.pentomino;

import java.io.Serializable;

/**
 * Abstract class representing a piece of the Pentomino puzzle.
 * <p>
 * Each piece is described as an integer array of relative indices that the
 * piece will cover in the single matrix representing the board. In cases where
 * the piece of the puzzle can be oriented in multiple directions and turned
 * upside down to yield different shapes, method {@link #variations()} should
 * reflect how many of these positions the piece can take while method
 * {@link #getVariation(int)} should return the array corresponding to each of
 * these possible orientations.
 *
 * @author Patrick Finnerty
 *
 */
public abstract class Piece implements Serializable {

  /**
   * Enumerator for the various ways a Piece may be used. This enumerator is
   * used in the constructor of the chiral pentomino pieces. Depending on the
   * kind of pentomino being explored (, only some variations of the piece may
   * be desired.
   *
   * @author Patrick Finnerty
   *
   */
  public enum PieceType {
    /** Piece of the pentomino with only the upside-down positions */
    FLIPSIDE,
    /** Standard Pentomino piece */
    STANDARD,
    /** Piece of the pentomino with only the face up variations */
    UPSIDE
  }

  /** Serial version UID */
  private static final long serialVersionUID = 335315376773627613L;

  /**
   * Returns a String that represents the shape described by the array given as
   * parameter.
   *
   * @param a
   *          array of indices covered by the piece on the array used to
   *          describe the board
   * @param width
   *          width of the board on which the piece was built to fit
   * @return potentially multiple line string that can be printed on the console
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

  /**
   * Gives the character used to describe the piece when it is placed on the
   * board.
   *
   * @return a character used to recognize the piece
   */
  public abstract char getChar();

  /**
   * Returns the i'th variation of this piece
   *
   * @param i
   *          the number of the variation whose array description is desired, 0
   *          &le; i &lt; {@link #variations()}.
   * @return the piece as the offset indices that are filled by this part when
   *         placed on the board
   */
  public abstract int[] getVariation(int i);

  /**
   * Prints all the variations of the piece on the standard output
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

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "" + getChar();
  }

  /**
   * Returns the number of different ways in which this piece can be positioned.
   *
   * @return the number of variants of this piece can have
   */
  public abstract int variations();
}
