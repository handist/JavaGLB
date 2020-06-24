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
 * F pentomino piece
 *
 * @author Patrick Finnerty
 *
 */
public class PieceF extends Piece {

  /** Serial version UID */
  private static final long serialVersionUID = -5244591650743453450L;

  /**
   * Displays the piece's variations
   *
   * @param args
   *          no argument
   */
  public static void main(String[] args) {
    new PieceF(PieceType.STANDARD, 10).printVariations(10);
  }

  /** Variations of this piece */
  @SuppressWarnings("javadoc")
  int[] first, second, third, fourth, fifth, sixth, seventh, eigth;

  /** Number of variations of this piece */
  int vars = 8;

  /**
   * Builds the F piece with its variations
   *
   * @param type
   *          type of the piece to create
   * @param width
   *          width of the board played
   */
  public PieceF(PieceType type, final int width) {
    final int[] f = { 1, 2, width, width + 1, (2 * width) + 1 };
    final int[] s = { 0, width, width + 1, width + 2, (2 * width) + 1 };

    final int[] t = { 1, width + 1, width + 2, 2 * width, 2 * width + 1 };
    final int[] fo = { 1, width, width + 1, width + 2, 2 * width + 2 };
    final int[] fi = { 0, 1, width + 1, width + 2, (2 * width) + 1 };
    final int[] si = { 2, width, width + 1, width + 2, (2 * width) + 1 };
    final int[] se = { 1, width, width + 1, (2 * width) + 1, (2 * width) + 2 };
    final int[] e = { 1, width, width + 1, width + 2, 2 * width };

    switch (type) {
    case STANDARD:
      first = f;
      second = s;
      third = t;
      fourth = fo;
      fifth = fi;
      sixth = si;
      seventh = se;
      eigth = e;
      break;
    case UPSIDE:
      first = f;
      second = s;
      third = t;
      fourth = fo;
      break;
    case FLIPSIDE:
      first = fi;
      second = si;
      third = se;
      fourth = e;
      break;
    default:
    }
    if (type != PieceType.STANDARD) {
      vars = 4;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.examples.pentomino.Piece#getChar()
   */
  @Override
  public char getChar() {
    return 'F';
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
    case 4:
      return fifth;
    case 5:
      return sixth;
    case 6:
      return seventh;
    case 7:
      return eigth;
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
