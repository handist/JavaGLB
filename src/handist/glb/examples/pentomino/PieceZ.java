/**
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
package handist.glb.examples.pentomino;

/**
 * Z pentomino
 *
 * @author Patrick Finnerty
 *
 */
public class PieceZ extends Piece {

  /** Serial version UID */
  private static final long serialVersionUID = -5244591650743453450L;

  /**
   * Displays the piece variation
   *
   * @param args
   *          not used
   */
  public static void main(String[] args) {
    new PieceZ(PieceType.STANDARD, 10).printVariations(10);
  }

  /** Variations of this piece */
  @SuppressWarnings("javadoc")
  int[] first, second, third, fourth;

  /** Number of variations of this piece */
  int vars = 4;

  /**
   * Builds the F piece with its variations
   *
   * @param type
   *          type of piece to create
   * @param width
   *          width of the board played
   */
  public PieceZ(PieceType type, int width) {
    final int[] f = { 0, 1, width + 1, 2 * width + 1, 2 * width + 2 };
    final int[] s = { 2, width, width + 1, width + 2, 2 * width };

    final int[] t = { 1, 2, width + 1, 2 * width, 2 * width + 1 };
    final int[] fo = { 0, width, width + 1, width + 2, 2 * width + 2 };

    switch (type) {
    case STANDARD:
      first = f;
      second = s;
      third = t;
      fourth = fo;
      break;
    case UPSIDE:
      first = f;
      second = s;
      break;
    case FLIPSIDE:
      first = t;
      second = fo;
      break;
    default:
      break;
    }

    if (type != PieceType.STANDARD) {
      vars = 2;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.examples.pentomino.Piece#getChar()
   */
  @Override
  public char getChar() {
    return 'Z';
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.examples.pentomino.Piece#getVariation(int)
   */
  @Override
  public int[] getVariation(int nb) {
    switch (nb) {
    case 0:
      return first;
    case 1:
      return second;
    case 2:
      return third;
    case 3:
      return fourth;
    default:
      return null;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.examples.pentomino.Piece#variations()
   */
  @Override
  public int variations() {
    return vars;
  }
}
