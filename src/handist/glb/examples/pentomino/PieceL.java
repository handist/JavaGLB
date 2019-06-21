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
 * L pentomino
 *
 * @author Patrick Finnerty
 *
 */
public class PieceL extends Piece {

  /** Serial version UID */
  private static final long serialVersionUID = -5244591650743453450L;

  /**
   * Displays the piece variation
   *
   * @param args
   *          not used
   */
  public static void main(String[] args) {
    System.out.println("Large board");
    System.out.println("Standard");
    new PieceL(PieceType.STANDARD, 10, 6).printVariations(10);
    System.out.println("Upside");
    new PieceL(PieceType.UPSIDE, 15, 6).printVariations(15);
    System.out.println("Flipside");
    new PieceL(PieceType.FLIPSIDE, 15, 6).printVariations(15);
    System.out.println("Shallow board");
    System.out.println("Standard");
    new PieceL(PieceType.STANDARD, 20, 3).printVariations(20);
    System.out.println("Upside");
    new PieceL(PieceType.UPSIDE, 30, 3).printVariations(30);
    System.out.println("Flipside");
    new PieceL(PieceType.FLIPSIDE, 30, 3).printVariations(30);
  }

  /** Variations of this piece */
  @SuppressWarnings("javadoc")
  int[] first, second, third, fourth, fifth, sixth, seventh, eigth;

  /** Number of variations of the created piece */
  int vars = 8;

  /**
   * Builds the L piece with its variations
   *
   * @param type
   *          type of the piece to create
   * @param width
   *          width of the board played, including the sentinels
   * @param height
   *          height of the board
   */
  public PieceL(PieceType type, int width, int height) {
    final int[] a = { 0, 1, 2, 3, width + 3 };
    final int[] b = { 0, 1, width, 2 * width, 3 * width };

    final int[] c = { 0, width, width + 1, width + 2, width + 3 };
    final int[] d = { 1, width + 1, 2 * width + 1, 3 * width, 3 * width + 1 };
    final int[] e = { 3, width, width + 1, width + 2, width + 3 };
    final int[] f = { 0, width, 2 * width, 3 * width, (3 * width) + 1 };
    final int[] g = { 0, 1, 2, 3, width };
    final int[] h = { 0, 1, width + 1, (2 * width) + 1, (3 * width) + 1 };

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
    if (height < 4) {
      vars /= 2;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.examples.pentomino.Piece#getChar()
   */
  @Override
  public char getChar() {
    return 'L';
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
