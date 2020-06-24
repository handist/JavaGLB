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
package handist.glb.tuning;

import java.io.Serializable;

import handist.glb.Configuration;
import handist.glb.GLBcomputer;
import handist.glb.PlaceLogger;

/**
 * Tuner for parameter {@link Configuration#n} of the multithreaded global load
 * balancer.
 * <h2>General functioning</h2> The purpose of this class is to dynamically
 * adjust the grain size of the computation so that a balance is found between
 * excessive overhead and starvation.
 * <p>
 * The tuner evaluates too indicators relying on the data available in the
 * {@link PlaceLogger}. If one of the indicator indicates that the value should
 * be increased (resp. decreased) in two successive calls to the
 * {@link #tune(PlaceLogger, Configuration)} method, the value is doubled (resp.
 * halved). This confirmation mechanism helps counteract the fact that the
 * indicators are not perfect and can sometimes come to the wrong conclusion.
 *
 * <h2>Indicators</h2>
 *
 * The indicator that the value of parameter {@link Configuration#n} is too
 * small is the ratio between the number of times some work was put into and
 * stolen from member intraPlaceQueue of {@link GLBcomputer}. If there are less
 * steals than twice the number of feeds into the queue, parameter N is judged
 * to be too small. This criteria relies on the assumption that the splitting
 * method implemented by the user always gives half of its content. In cases
 * where the grain size is too small, the shared queue of the load balancer gets
 * redundantly fed. Under the assumption that the queue gives away half of its
 * content, multiple feeding operation will be stolen away with comparatively
 * fewer steal operations.
 * <p>
 * The indicator used to identify situations where the parameter N is too large
 * relies on the amount of time spent with the maximum number of workers. If
 * less than 90% of the time since the last call to the tuner was spent with the
 * maximum number of workers, the value of {@link Configuration#n} is judged to
 * be too large.
 * <h2>Data gathering</h2> The tuner keeps track of the values held by the
 * {@link PlaceLogger} in its own members. When the
 * {@link #tune(PlaceLogger, Configuration)} method is called, the difference
 * between the current (new) values of the {@link PlaceLogger} is computed. This
 * makes the {@link NtunerReedBush3} make its decision on the most recent logger
 * data.
 *
 * <h2>Value increase and decrease</h2> When the decision is made to decrease
 * the value of {@link Configuration#n}, we divide it by 2 and add 1. This
 * guarantees that the value remains strictly positive. When the decision is
 * made to increase the value of {@link Configuration#n}, its current value is
 * doubled. We then perform a check to ensure that we did not overflow the
 * maximum possible value for the {@code int} type. If there was overflow, we
 * set the value to {@value Integer#MAX_VALUE}.
 *
 *
 * @author Patrick Finnerty
 *
 */
public class NtunerReedBush3 implements Tuner, Serializable {

  /** Serial Version UID */
  private static final long serialVersionUID = -2434717487578453824L;

  private static final int NOINFOCYCLELIMIT = Integer
      .parseInt(System.getProperty("glb.rb3", "10"));;

  /**
   * Value of member {@link PlaceLogger#interQueueFed} the last time the tuner
   * was called. Is used to determine how many actions were performed in the
   * interval between the last call to the tuner.
   */
  long oldIntraQueueFed;

  /**
   * Value of member {@link PlaceLogger#interQueueSplit} the last time the tuner
   * was called. Is used to determine how many actions were performed during the
   * last time interval.
   */
  long oldIntraQueueSplit;

  /**
   * Value of member {@link PlaceLogger#time} at the last index which counts the
   * accumulated time spent with the maximum number of workers.
   */
  long oldMaxWorkerAccumulatedTime;

  /**
   * Timestamp of the last time the tuner was called.
   */
  long lastCallTimestamp;

  /**
   * Contains the last interpretation that was made by the tuner on the value of
   * parameter N.
   * <p>
   * When an adjustment to the tuner is made or when neither or both of the
   * indicators are true, the value is set back to 0 by
   * {@link #tune(PlaceLogger, Configuration)} method. Possible values taken by
   * this member are therefore:
   * <ul>
   * <li><em>-1</em> if the last time the
   * {@link #tune(PlaceLogger, Configuration)} method was called the value of
   * parameter N was judged to be too large
   * <li><em>0</em> if the last time the tuner was called the value was neither
   * judged to be too small nor too large, or if both indicators indicated that
   * it was too large and too small, or if the value of n was modified during
   * the last {@link #tune(PlaceLogger, Configuration)} call
   * <li><em>1</em> if the last time the
   * {@link #tune(PlaceLogger, Configuration)} method was called, the value of N
   * was judged to be too small.
   * </ul>
   *
   */
  byte lastDecision;

  /*
   * (non-Javadoc)
   *
   * @see handist.glb.multiworker.Tuner#placeLaunched(handist.glb.multiworker.
   * PlaceLogger, handist.glb.multiworker.Configuration)
   */
  @Override
  public long placeLaunched(PlaceLogger l, Configuration c) {
    lastCallTimestamp = System.nanoTime();
    oldIntraQueueFed = l.intraQueueFed.get();
    oldIntraQueueSplit = l.intraQueueSplit.get();
    synchronized (l) {
      oldMaxWorkerAccumulatedTime = l.time[l.time.length - 1];
    }
    lastDecision = 0;

    l.NvalueTuned(lastCallTimestamp, c.n);

    return lastCallTimestamp;
  }

  /** Counts the number of consecutive times neither criteria triggers */
  int consecutiveNoCriteriaRaised;

  /*
   * (non-Javadoc)
   *
   * @see
   * handist.glb.multiworker.Tuner#tune(handist.glb.multiworker.PlaceLogger,
   * handist.glb.multiworker.Configuration)
   */
  @Override
  public long tune(PlaceLogger l, Configuration c) {
    // Computing the data required
    final long stamp = System.nanoTime();
    long maxWorkerStamp;
    boolean atMaximum;
    synchronized (l) {
      maxWorkerStamp = l.time[l.time.length - 1];
      atMaximum = l.workerCount == c.x;
    }
    long timeMaxWorker = maxWorkerStamp - oldMaxWorkerAccumulatedTime;
    if (atMaximum) {
      timeMaxWorker += (stamp - l.lastEventTimeStamp);
    }

    final long newSplit = l.intraQueueSplit.get();
    final long newFeed = l.intraQueueFed.get();

    // Computing the indicators chosen
    final long split = newSplit - oldIntraQueueSplit;
    final long feed = newFeed - oldIntraQueueFed;
    final boolean nTooSmall = split < feed * 2;

    final long elapsed = stamp - lastCallTimestamp;
    final boolean nTooLarge = timeMaxWorker * 10 < 9 * elapsed;

    // Decision based on the criteria
    final int oldValue = c.n;
    int newValue = oldValue;
    if (nTooSmall && nTooLarge) {
      // Contradiction, wait for more
      consecutiveNoCriteriaRaised = 0;
      lastDecision = 0;
    } else if (!nTooSmall && !nTooLarge) {
      // All seem well, neither criteria triggered.
      consecutiveNoCriteriaRaised++;
      lastDecision = 0;

      // If no criteria was raised for enough consecutive times, increase the
      // grain
      if (consecutiveNoCriteriaRaised >= NOINFOCYCLELIMIT && l.place == 0) {
        newValue *= 2;
        // Handle the overflow risk
        if (newValue <= 0) {
          newValue = Integer.MAX_VALUE;
        }

        consecutiveNoCriteriaRaised = 0;
      }
    } else if (nTooSmall) {
      consecutiveNoCriteriaRaised = 0;
      if (lastDecision == 1) {
        // Multiply n by 2
        newValue *= 2;
        lastDecision = 0;
        // Handle the overflow risk
        if (newValue <= 0) {
          newValue = Integer.MAX_VALUE;
        }
      } else {
        lastDecision = 1;
      }
    } else if (nTooLarge) {
      consecutiveNoCriteriaRaised = 0;
      if (lastDecision < 0) {
        // Divide n by 2, add 1 to be sure it doesn't turn to 0
        newValue = (oldValue / 2) + 1;
        lastDecision = 0;
      } else {
        lastDecision = -1;
      }
    }

    if (newValue != oldValue) {
      // In some cases the value is already at the minimum level and the
      // operation does not actually change it. To avoid unnecessary logs we
      // perform this little check.
      c.n = newValue;
      l.NvalueTuned(stamp, newValue);
    }

    // Saving the current values for the next check
    lastCallTimestamp = stamp;
    oldIntraQueueFed = newFeed;
    oldIntraQueueSplit = newSplit;
    oldMaxWorkerAccumulatedTime = maxWorkerStamp;

    return lastCallTimestamp;
  }
}
