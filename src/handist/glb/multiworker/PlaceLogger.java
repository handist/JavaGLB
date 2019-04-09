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
package handist.glb.multiworker;

import java.io.PrintStream;
import java.io.Serializable;

/**
 * Class used to log the runtime of the multithread worker at each place.
 * <p>
 * It implements the {@link Serializable} interface to be transported
 * successfully after the computation has taken place.
 *
 * @author Patrick Finnerty
 *
 */
public class PlaceLogger implements Serializable {

  /** Generated Serial Version UID */
  private static final long serialVersionUID = 2764081210591528731L;

  /** Indicates if the logger was reset */
  boolean isReset = true;

  /** Records the number of times some work was put into the inter queue */
  public long interQueueFed = 0;
  /** Records the number of times some work was taken from the inter queue */
  public long interQueueSplit = 0;
  /** Records the number of times some work was put into the intra queue */
  public long intraQueueFed = 0;
  /** Records the number of times some work was taken from the intra queue */
  public long intraQueueSplit = 0;

  /** Time stamp of the last event that was recorded */
  long lastEventTimeStamp;

  /* Trackers for lifeline steals */
  /** Number of lifeline steals attempted by this place */
  public long lifelineStealsAttempted = 0;
  /** Number of lifeline steals other places attempted on this place */
  public long lifelineStealsReceived = 0;
  /** Number of lifeline steals attempted by this place that were successful */
  public long lifelineStealsSuccess = 0;
  /**
   * Number of lifeline steals attempted by other places on this place that were
   * successful
   */
  public long lifelineStealsSuffered = 0;

  /**
   * Time stamp used during login to track the activity of the lifeline answer
   * thread
   */
  long lifelineThreadTimestamp;

  /**
   * Accumulated amount of time in nanoseconds during which the
   * {@link GLBcomputer#lifelineAnswerThread()} was active.
   */
  public long lifelineThreadActive = 0;

  /**
   * Accumulated amount of time in nanoseconds during which the
   * {@link GLBcomputer#lifelineAnswerThread()} waited to be scheduled in the
   * ForkJoinPool before becoming active again.
   */
  public long lifelineThreadHold = 0;

  /**
   * Accumulated amount of time in nanoseconds during which the
   * {@link GLBcomputer#lifelineAnswerThread()} was inactive.
   */
  public long lifelineThreadInactive = 0;

  /**
   * Counts the number of times the {@link GLBcomputer#lifelineAnswerThread()}
   * went through the <em>Active</em>\/<em>Inactive</em>\/<em>Hold</em> cycle.
   */
  public int lifelineThreadWokenUp = 0;

  /**
   * Integer identifier of the place this logger is responsible for
   */
  public int place;

  /**
   * Time stamp of when the place starts computing. Is used for a priori
   * correction in {@link Logger#addPlaceLogger(PlaceLogger)}.
   */
  long startTimeStamp;

  /* Trackers for random steals */
  /** Number of random steals attempted by this place */
  public long stealsAttempted = 0;
  /** Number of random steals that other places attempted on this place */
  public long stealsReceived = 0;
  /** Number of random steals attempted by this place that were successful */
  public long stealsSuccess = 0;
  /**
   * Number of random steals that other places successfully attempted on this
   * place
   */
  public long stealsSuffered = 0;

  /** Counter of the time spent yielding by workers on this place */
  public long yieldingTime = 0;

  /** Timestamp used to track the yielding of a worker on the place */
  long lastYield;

  /** Tracks the time spent by the place running 'index' number of workers */
  long time[];

  /** Indicates the number of workers currently running on the place. */
  int workerCount = 0;

  /**
   * Called when the lifeline answer thread becomes active again after being on
   * hold.
   * <p>
   * As part of the lifecycle of the lifeline answer thread of
   * {@link GLBcomputer}, this method is called when the holding state of the
   * thread ends.
   */
  void lifelineAnswerThreadActive() {
    synchronized (this) {
      final long stamp = System.nanoTime();
      lifelineThreadHold += stamp - lifelineThreadTimestamp;
      lifelineThreadTimestamp = stamp;
      // System.out.println(place + " Lifeline Answer");
    }
  }

  /**
   * Called when the lifeline answer thread ends its activity.
   */
  void lifelineAnswerThreadEnded() {
    synchronized (this) {
      lifelineThreadActive += System.nanoTime() - lifelineThreadTimestamp;
      // System.out.println(place + " Lifeline Stopped");
    }
  }

  /**
   * Called when a worker decides to wake up the lifeline answer thread. The
   * lifeline answer thread is now not inactive anymore but in a holding state
   * until it is effectively scheduled in the thread pool.
   */
  void lifelineAnswerThreadHold() {
    synchronized (this) {
      final long stamp = System.nanoTime();
      lifelineThreadInactive += stamp - lifelineThreadTimestamp;
      lifelineThreadTimestamp = stamp;
      // System.out.println(place + " Lifeline Hold");
    }
  }

  /**
   * Called when the lifeline answer thread becomes inactive.
   */
  void lifelineAnswerThreadInactive() {
    synchronized (this) {
      final long stamp = System.nanoTime();
      lifelineThreadActive += stamp - lifelineThreadTimestamp;
      lifelineThreadTimestamp = stamp;
      // System.out.println(place + " Lifeline Inactive");
    }
  }

  /**
   * Called when a new {@link GLBcomputer#lifelineAnswerThread()}activity is
   * started.
   */
  void lifelineAnswerThreadStarted() {
    synchronized (this) {
      lifelineThreadTimestamp = System.nanoTime();
    }
  }

  /**
   * Signals that a new worker has started working on the place.
   */
  void workerStarted() {
    synchronized (this) {
      final long stamp = System.nanoTime();
      if (isReset) {
        lastEventTimeStamp = stamp;
        startTimeStamp = stamp;
        isReset = false;
      }
      time[workerCount] += (stamp - lastEventTimeStamp);
      lastEventTimeStamp = stamp;
      workerCount++;
      // System.out.println(place + " Worker Started : " + workerCount);
    }
  }

  /**
   * Signals that a worker has stopped running on the place.
   */
  void workerStopped() {
    synchronized (this) {
      final long stamp = System.nanoTime();
      time[workerCount] += stamp - lastEventTimeStamp;
      lastEventTimeStamp = stamp;
      workerCount--;
      // System.out.println(place + " Worker Stopped : " + workerCount);
    }
  }

  /**
   * Method called when a worker starts yielding to allow for other activities
   * to be run by the place.
   */
  void workerYieldStart() {
    synchronized (this) {
      final long stamp = System.nanoTime();
      time[workerCount] += stamp - lastEventTimeStamp;
      lastEventTimeStamp = stamp;
      workerCount--;
      lastYield = stamp;
      // System.out.println(place + " Worker Yield : " + workerCount);
    }
  }

  /**
   * Method called when a worker that was yielding resumes its normal execution.
   */
  void workerYieldStop() {
    synchronized (this) {
      final long stamp = System.nanoTime();
      time[workerCount] += (stamp - lastEventTimeStamp);
      lastEventTimeStamp = stamp;
      workerCount++;
      yieldingTime += stamp - lastYield;
      // System.out.println(place + " Worker Resumed : " + workerCount);
    }
  }

  /**
   * Prints some basic information on the output specified as parameter
   *
   * @param out
   *          the output on which the information is going to be displayed
   */
  public void print(PrintStream out) {
    out.println("PLACE " + place);
    out.println("-------------- Random Steals -----------------");
    out.println(
        "This place succeeded   " + stealsSuccess + "/" + stealsAttempted);
    out.println(
        "Other places succeeded " + stealsSuffered + "/" + stealsReceived);
    out.println("------------- Lifeline Steals ----------------");
    out.println("This place succeeded     " + lifelineStealsSuccess + "/"
        + lifelineStealsAttempted);
    out.println("Other places succeeded   " + lifelineStealsSuffered + "/"
        + lifelineStealsReceived);
    out.println("------------- Lifeline Thread ----------------");
    out.println("Active   (s) " + lifelineThreadActive / 1e9);
    out.println("Inactive (s) " + lifelineThreadInactive / 1e9);
    out.println("On Hold  (s) " + lifelineThreadHold / 1e9);
    out.println("Woken up a total of " + lifelineThreadWokenUp + " times");
    out.println("------------------ Runtime -------------------");
    for (int i = 0; i < time.length; i++) {
      out.println("Time spent with " + i + " workers (s): " + (time[i] / 1e9));
    }
    out.println("Time spent yielding (s) " + yieldingTime / 1e9);
    out.println("----------------------------------------------");
  }

  /**
   * Constructor
   *
   * Sets up a PlaceLogger for runtime tracking.
   *
   * @param maxWorkers
   *          the maximum number of concurrent workers allowed for the place
   * @param placeId
   *          integer identifier of the place this PlaceLogger instance is
   *          recording activity for
   */
  public PlaceLogger(int maxWorkers, int placeId) {
    place = placeId;
    time = new long[maxWorkers + 1];
  }
}
