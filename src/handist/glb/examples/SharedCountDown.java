/*
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
package handist.glb.examples;

import java.io.Serializable;

import handist.glb.multiworker.Bag;
import handist.glb.multiworker.GLBcomputer;
import handist.glb.multiworker.GLBfactory;

/**
 * Class {@link SharedCountDown} is the same implementation as class
 * {@link CountDown} but adapted to the multiworker computation abstraction.
 *
 * @author Patrick Finnerty
 *
 */
public class SharedCountDown
    implements Bag<SharedCountDown, SharedSum>, Serializable {

  /** Serial Version UID */
  private static final long serialVersionUID = 1035264870911201806L;

  /** Minimum countdown that can be split */
  private final long MINIMUM_SPLIT = 100000;

  /** Counter for countdown */
  long countDown;

  /*
   * (non-Javadoc)
   *
   * @see apgas.glbm.Bag#isEmpty()
   */
  @Override
  public boolean isEmpty() {
    return countDown == 0;
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glbm.Bag#isSlittable()
   */
  @Override
  public boolean isSplittable() {
    return countDown > MINIMUM_SPLIT;
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glbm.Bag#merge(apgas.glbm.Bag)
   */
  @Override
  public void merge(SharedCountDown b) {
    countDown += b.countDown;
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glbm.Bag#process(int)
   */
  @Override
  public void process(int workAmount, SharedSum s) {
    while (workAmount > 0 && countDown > 0) {
      workAmount--;
      countDown--;
      s.add(1);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glbm.Bag#split(boolean)
   */
  @Override
  public SharedCountDown split(boolean takeAll) {
    SharedCountDown toReturn;
    if (!isSplittable() && takeAll) {
      toReturn = new SharedCountDown(countDown);
      countDown = 0;
    } else {
      final long givenAmount = countDown / 2;
      countDown -= givenAmount;
      toReturn = new SharedCountDown(givenAmount);
    }
    return toReturn;
  }

  /*
   * (non-Javadoc)
   *
   * @see apgas.glbm.Bag#submit(apgas.glb.Fold)
   */
  @Override
  public void submit(SharedSum r) {
  }

  /**
   * Displays remaining and computed amount in the bag
   */
  @Override
  public String toString() {
    return "Remaining;" + countDown + ";";
  }

  /**
   * Constructor
   *
   * Allows to specify the initial number from which the countdown has to start
   *
   * @param value
   *          the initial countdown value
   */
  public SharedCountDown(long value) {
    countDown = value;
  }

  /**
   * Main method
   * <p>
   * This program may take one optional argument: an integer used to set the
   * size of the countdown to perform.
   *
   * @param args
   *          arguments for this program
   */
  public static void main(String[] args) {
    long bagSize = 20;

    try {
      bagSize = Integer.parseInt(args[0]);
    } catch (final Exception e) {
      System.err.println(
          "[WARNING] Could not parse size of initial bag, using default value "
              + bagSize);
    }

    GLBcomputer glb;
    try {
      glb = GLBfactory.setupGLB();
      final long EXPECTED_RESULT = bagSize; // 2147483648l *

      final long result = glb.compute(new SharedCountDown(EXPECTED_RESULT),
          () -> new SharedSum(0), () -> new SharedCountDown(0)).get();
      System.out.println("Result from computation: " + result + " Expected: "
          + EXPECTED_RESULT + " Correct: " + (result == EXPECTED_RESULT)
          + " Difference (Expected - result) " + (EXPECTED_RESULT - result));
      glb.getLog().print(System.err);
    } catch (final ReflectiveOperationException e) {
      e.printStackTrace();
    }

  }
}
