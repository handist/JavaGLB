/**
 *
 */
package handist.glb.util;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author Patrick Finnerty
 *
 */
public class DonutCube implements LifelineStrategy, Serializable {

  /**
   *
   */
  private static final long serialVersionUID = 2672915712249094774L;

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.util.LifelineStrategy#lifeline(int, int)
   */
  @Override
  public int[] lifeline(int thief, int nbPlaces) {
    int count = 0;
    boolean hasZero = false;
    int mask = 1;
    int l;
    while ((l = thief ^ mask) < nbPlaces) {
      if (l == 0) {
        hasZero = true;
      }
      count++;
      mask *= 2;
    }

    final int toReturn[];
    int index;

    if (thief == 0 || hasZero) {
      toReturn = new int[count];
      index = 0;
    } else {
      toReturn = new int[count + 1];
      toReturn[0] = 0;
      index = 1;
    }

    mask = 1;
    while ((l = thief ^ mask) < nbPlaces) {
      toReturn[index++] = l;
      mask *= 2;
    }

    return toReturn;
  }

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.util.LifelineStrategy#reverseLifeline(int, int)
   */
  @Override
  public int[] reverseLifeline(int target, int nbPlaces) {
    int[] thieves;

    if (target == 0) {
      // All other places have a lifeline on 0
      thieves = new int[nbPlaces - 1];
      Arrays.fill(thieves, 0, nbPlaces - 1, 1);
      Arrays.parallelPrefix(thieves, (a, b) -> a + b);
    } else {
      int count = 0;
      int mask = 1;
      int l;
      while ((l = target ^ mask) < nbPlaces) {
        count++;
        mask *= 2;
      }

      thieves = new int[count];

      mask = 1;
      int index = 0;
      while ((l = target ^ mask) < nbPlaces) {
        thieves[index] = l;
        index++;
        mask *= 2;
      }

    }
    return thieves;
  }

  /**
   * Prints a 2D array of integers
   *
   * @param a
   *          the table to print
   */
  public static void PRINTER(int[][] a) {
    for (int i = 0; i < a.length; i++) {
      System.out.print(i + " : [");
      for (final int j : a[i]) {
        System.out.print(j + ", ");
      }
      System.out.println(" ]");
    }
  }

  /**
   * Checks that the DonutCube is a well defined strategy, meaning that the
   * result of methods {@link #lifeline(int, int)} and
   * {@link #reverseLifeline(int, int)} are consistent with one another.
   *
   * @param args
   *          number of places in the system to verify
   */
  public static void main(String args[]) {
    final int PLACES = Integer.parseInt(args[0]);

    final DonutCube cube = new DonutCube();

    final int[][] lifelines = new int[PLACES][0];
    final int[][] reverse = new int[PLACES][0];

    for (int i = 0; i < PLACES; i++) {
      lifelines[i] = cube.lifeline(i, PLACES);
      reverse[i] = cube.reverseLifeline(i, PLACES);
    }

    System.out.println("------ LIFELINES ------");
    PRINTER(lifelines);
    System.out.println("------- REVERSE -------");
    PRINTER(reverse);

    for (int l = 0; l < lifelines.length; l++) {
      // We check each lifeline array
      final int[] life = lifelines[l];
      for (final int remote : life) {
        final int[] toCheck = reverse[remote];
        boolean ok = false;
        for (final int candidate : toCheck) {
          if (candidate == l) {
            ok = true;
            break;
          }
        }
        if (!ok) {
          System.err.println("Place " + l + " has lifeline " + remote
              + " which has no counterpart in " + remote + " reverse lifeline");
        }

      }

    }
  }
}
