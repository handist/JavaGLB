/**
 *
 */
package handist.glb.examples.tsp;

import java.io.Serializable;

/**
 * Custom class that implements an integer on two {@code long} integers.
 *
 * @author Patrick Finnerty
 *
 */
public class VeryLong implements Serializable {

  /**
   * Serial Version UID
   */
  private static final long serialVersionUID = -8541019226705680493L;

  /**
   * Low weight, kept be positive at all times. If during computation this
   * member turns negative, it will be put back into a positive state and member
   * {@link #high} will be updated accordingly.
   */
  private long low;

  /**
   * High weight
   */
  private long high;

  /**
   * Adds the specified h and l values to members {@link #high} and {@link #low}
   * respectively.
   *
   * @param h
   *          high order long to add
   * @param l
   *          low order long to add
   */
  public void add(long h, long l) {
    if (l < 0) {
      throw new IllegalArgumentException(
          "Argument l should be positive or nil, value was " + l);
    }
    high += h;
    low += l;
    if (low < 0) {
      high += 1;
      low -= Long.MIN_VALUE; // This will cause an overflow and put member
                             // low in the correct (positive) range
    }
  }

  /**
   * Adds the given {@link VeryLong} to the current instance. The instance given
   * as parameter will not be modified in any way by this method.
   *
   * @param vl
   *          instance to add to {@code this}
   */
  public void add(VeryLong vl) {
    add(vl.high, vl.low);
  }

  /**
   * Substracts 1 to this instance and returns.
   * <p>
   * In the implementation, 1 is substracted to member {@link #low}. If
   * {@link #low} becomes negative as a result, {@link #high} is decremented and
   * {@link #low} is set to {@link Long#MAX_VALUE}.
   */
  public void decrement() {
    low--;
    if (low < 0) {
      high--;
      low = Long.MAX_VALUE;
    }
  }

  /**
   * Adds 1 to this instance and returns.
   * <p>
   * In the implementation, 1 is added to member {@link #low}. If that causes an
   * overflow, {@link #high} is incremented by 1 and {@link #low} is set to 0.
   * No checks are done with regards to member {@link #high} overflowing.
   */
  public void increment() {
    low++;
    if (low < 0) {
      high++;
      low = 0;
    }
  }

  /**
   * Sets the {@link #high} and {@link #low} members to the specified values.
   *
   * @param h
   *          value to set to {@link #high}
   * @param l
   *          value to set to {@link #low}, should be positive or nil.
   */
  public void setValue(long h, long l) {
    if (l < 0) {
      throw new IllegalArgumentException(
          "Argument l should be positive or nil, value was " + l);
    }
    high = h;
    low = l;
  }

  /**
   * Displays a {@link VeryLong} in the following format: "{@link #high}M
   * {@link #low}m"
   */
  @Override
  public String toString() {
    return high + "M " + low + "m";
  }

  /**
   * Constructor
   * <p>
   * Initializes a {@link VeryLong} number to inital value 0.
   */
  public VeryLong() {
    new VeryLong(0l, 0l);
  }

  /**
   * Initializes a {@link VeryLong} instance with the specified initial values
   * for high order and low order long used by this class.
   *
   * @param h
   *          high order term
   * @param l
   *          low order term, must be positive
   */
  public VeryLong(long h, long l) {
    if (l < 0) {
      throw new IllegalArgumentException(
          "Argument l should be positive or nil, value was " + l);
    }
    low = l;
    high = h;
  }

  /**
   * Displays some basic behaviors of {@link VeryLong} type on the standard
   * output.
   *
   * @param args
   *          unused
   */
  public static void main(String[] args) {

    final VeryLong vl = new VeryLong();
    System.out.println("Default 0 value:   " + vl);
    vl.increment();
    System.out.println("Value 1:           " + vl);
    vl.decrement();
    vl.decrement();
    System.out.println("Value -1:          " + vl);

    vl.setValue(0, Long.MAX_VALUE);
    System.out.println("Before overflow:   " + vl);
    vl.increment();
    System.out.println("After overflow:    " + vl);
    vl.decrement();
    System.out.println("Reverse operation: " + vl);
    vl.setValue(Long.MAX_VALUE, Long.MAX_VALUE);

    System.out.println("Maximum value:     " + vl);
    vl.increment();
    System.out.println("Maximum overflow:  " + vl);
    vl.setValue(Long.MIN_VALUE, 0l);
    System.out.println("Minimum value:     " + vl);
    vl.decrement();
    System.out.println("Minimum overflow:  " + vl);

    try {
      System.out.println("Wrong argument constructor");
      new VeryLong(0l, -1l);
    } catch (final Exception e) {
      System.out.println(e);
    }

    try {
      System.out.println("Wrong argument setValue");
      vl.setValue(0l, -1l);
    } catch (final Exception e) {
      System.out.println(e);
    }

    final VeryLong a = new VeryLong(2l, 2l);
    final VeryLong b = new VeryLong(1l, 1l);
    System.out.println("Addition of " + a + " and " + b);
    a.add(b);
    System.out.println("Result: " + a);

    final VeryLong c = new VeryLong(0l, 0l);
    c.decrement();
    c.decrement();
    System.out.println("Adding " + a + " to " + c + " (which is -2)");
    c.add(a);
    System.out.println("Result: " + c);
  }
}
