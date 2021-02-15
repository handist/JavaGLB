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
package handist.glb.tuning;

import java.io.Serializable;

import handist.glb.Configuration;
import handist.glb.GLBcomputer;
import handist.glb.PlaceLogger;

/**
 * Tuner relying on the percentage of time with full active workers and the
 * ratio of merge/emptied intra-bag to adjust the grain size of the
 * multithreaded global load balancer
 *
 *
 * @author Patrick Finnerty
 *
 */
public class Newtuner12 implements Tuner, Serializable {

  /** Serial Version UID */
  private static final long serialVersionUID = -2434717487578453824L;

  /**
   * Value of member {@link PlaceLogger#intraQueueFedByWorker} added to
   * {@link PlaceLogger#intraQueueFedByLifeline} the last time the tuner was
   * called. Is used to determine how many actions were performed in the
   * interval between the last call to the tuner.
   */
  long oldIntraQueueFed;

  /**
   * Value of member {@link PlaceLogger#intraQueueEmptied} the last time the
   * tuner was called. Is used to determine how many actions were performed
   * during the last time interval.
   */
  long oldIntraQueueEmptied;

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
   * {@link #tune(PlaceLogger, Configuration, GLBcomputer)} method. Possible
   * values taken by this member are therefore:
   * <ul>
   * <li><em>-1</em> if the last time the
   * {@link #tune(PlaceLogger, Configuration, GLBcomputer)} method was called
   * the value of parameter N was judged to be too large
   * <li><em>0</em> if the last time the tuner was called the value was neither
   * judged to be too small nor too large, or if both indicators indicated that
   * it was too large and too small, or if the value of n was modified during
   * the last {@link #tune(PlaceLogger, Configuration, GLBcomputer)} call
   * <li><em>1</em> if the last time the
   * {@link #tune(PlaceLogger, Configuration, GLBcomputer)} method was called,
   * the value of N was judged to be too small.
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
    oldIntraQueueFed = l.intraQueueFedByWorker.get()
        + l.intraQueueFedByLifeline.get();
    oldIntraQueueEmptied = l.intraQueueEmptied.get();
    synchronized (l) {
      oldMaxWorkerAccumulatedTime = l.time[l.time.length - 1];
    }
    lastDecision = 0;

    l.NvalueTuned(lastCallTimestamp, c.n);

    return lastCallTimestamp;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * handist.glb.multiworker.Tuner#tune(handist.glb.multiworker.PlaceLogger,
   * handist.glb.multiworker.Configuration)
   */
  @Override
  public long tune(PlaceLogger l, Configuration c, GLBcomputer g) {
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

    final long newEmpty = l.intraQueueEmptied.get();
    final long newFeed = l.intraQueueFedByWorker.get()
        + l.intraQueueFedByLifeline.get();

    // Computing the indicators chosen
    final long empty = newEmpty - oldIntraQueueEmptied;
    final long feed = newFeed - oldIntraQueueFed;

    // CRITERIA GRAIN TOO SMALL : feed/empty > 1.2
    final boolean nTooSmall = feed * 10 > empty * 12;

    final long elapsed = stamp - lastCallTimestamp;
    final boolean nTooLarge = timeMaxWorker * 10 < 9 * elapsed;

    // Decision based on the criteria
    final int oldValue = c.n;
    int newValue = oldValue;
    if ((nTooSmall && nTooLarge) || (!nTooSmall && !nTooLarge)) {
      // Unconclusive, wait for more data
      lastDecision = 0;
    } else if (nTooSmall) {
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
    oldIntraQueueEmptied = newEmpty;
    oldMaxWorkerAccumulatedTime = maxWorkerStamp;

    return lastCallTimestamp;
  }
}
