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

/**
 * Y pentomino piece
 *
 * @author Patrick Finnerty
 *
 */
public class PieceY extends Piece {

  /** Serial version UID */
  private static final long serialVersionUID = -5244591650743453450L;

  /**
   * Displays the piece's variations
   *
   * @param args
   *          no argument
   */
  public static void main(String[] args) {
    System.out.println("Large board");
    System.out.println("Standard");
    new PieceY(PieceType.STANDARD, 10).printVariations(10);
    System.out.println("Upside");
    new PieceY(PieceType.UPSIDE, 15).printVariations(15);
    System.out.println("Flipside");
    new PieceY(PieceType.FLIPSIDE, 15).printVariations(15);
    System.out.println("Shallow board");
    System.out.println("Standard");
    new PieceY(PieceType.STANDARD, 20).printVariations(20);
    System.out.println("Upside");
    new PieceY(PieceType.UPSIDE, 30).printVariations(30);
    System.out.println("Flipside");
    new PieceY(PieceType.FLIPSIDE, 30).printVariations(30);
  }

  /** Variations of this piece */
  @SuppressWarnings("javadoc")
  int[] first, second, third, fourth, fifth, sixth, seventh, eigth;

  /** Number of piece variations */
  int vars = 8;

  /**
   * Builds the F piece with its variations
   *
   * @param type
   *          type of the piece to create
   * @param width
   *          width of the board played
   *
   */
  public PieceY(PieceType type, int width) {
    final int[] a = { 0, 1, 2, 3, width + 1 };
    final int[] b = { 0, width, 2 * width, 2 * width + 1, 3 * width };

    final int[] c = { 2, width, width + 1, width + 2, width + 3 };
    final int[] d = { 1, width, width + 1, 2 * width + 1, 3 * width + 1 };
    final int[] e = { 1, width, width + 1, width + 2, width + 3 };
    final int[] f = { 0, width, width + 1, 2 * width, 3 * width };
    final int[] g = { 0, 1, 2, 3, width + 2 };
    final int[] h = { 1, width + 1, 2 * width, 2 * width + 1, 3 * width + 1 };

    switch (type) {
    case STANDARD:
      first = a;
      second = c;
      third = e;
      fourth = g;
      fifth = b;
      sixth = d;
      seventh = f;
      eigth = h;
      break;
    case UPSIDE:
      first = a;
      second = c;
      third = b;
      fourth = d;
      break;
    case FLIPSIDE:
      first = e;
      second = g;
      third = f;
      fourth = h;
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
    return 'Y';
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
