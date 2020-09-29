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
package handist.glb;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class used to log the runtime of the multithread global load balncer at each
 * place of the distributed computation.
 * <p>
 * It implements the {@link Serializable} interface to be transported
 * successfully after the computation has taken place to be gathered in a single
 * {@link Logger} instance.
 *
 * @author Patrick Finnerty
 * @see Logger
 */
public class PlaceLogger implements Serializable {

  /**
   * Wrapper class used to carry the information of when the tuner chooses to
   * modify the value of {@link Configuration#n}.
   *
   * @author Patrick Finnerty
   *
   */
  class TunerStamp implements Serializable {

    /** Serial version UID */
    private static final long serialVersionUID = -5482168027300527929L;

    /** New value used hereinafter */
    final int n;
    /** Timestamp at which the value of parameter "n" was changed */
    final long stamp;

    /**
     * Constructor
     *
     * @param timestamp
     *          stamp at which the value was changed
     * @param value
     *          new value chosen by the tuner
     */
    public TunerStamp(long timestamp, int value) {
      stamp = timestamp;
      n = value;
    }
  }

  /** Generated Serial Version UID */
  private static final long serialVersionUID = 2764081210591528731L;

  /**
   * Counter for the number of times a place receives information from remote
   * hosts through the {@link Whisperer} mechanism. It is protected against
   * concurrent accesses as a single host may receive multiple information from
   * several hosts at the same time.
   */
  public AtomicLong communicationReceived = new AtomicLong(0);

  /**
   * Counter for the number of times a place shared local information with
   * remote hosts through the {@link Whisperer} mechanism. This member is not
   * protected against concurrent accesses as only a single thread is
   * susceptible to increments this counter at a time.
   */
  public Long communicationSent = new Long(0);

  /** Counter for the number of times the inter queue was emptied */
  public long interQueueEmptied = 0;

  /** Records the number of times some work was put into the inter queue */
  public AtomicLong interQueueFed = new AtomicLong(0);

  /** Records the number of times some work was taken from the inter queue */
  public AtomicLong interQueueSplit = new AtomicLong(0);

  /** Counter for the number of times the inter queue was emptied */
  public long intraQueueEmptied = 0;

  /** Records the number of times some work was put into the intra queue */
  public AtomicLong intraQueueFed = new AtomicLong(0);

  /** Records the number of times some work was taken from the intra queue */
  public AtomicLong intraQueueSplit = new AtomicLong(0);

  /** Indicates if the logger was reset */
  private boolean isReset = true;

  /** Time stamp of the last event that was recorded */
  public long lastEventTimeStamp;

  /** Time stamp of the last event regarding worker stealing was recorded */
  public long lastWorkerStealingTimeStamp;

  /** Timestamp used to track the yielding of a worker on the place */
  private long lastYield;

  /** Number of lifeline steals attempted by this place */
  public AtomicLong lifelineStealsAttempted = new AtomicLong(0);

  /** Number of lifeline steals other places attempted on this place */
  public AtomicLong lifelineStealsReceived = new AtomicLong(0);

  /** Number of lifeline steals attempted by this place that were successful */
  public AtomicLong lifelineStealsSuccess = new AtomicLong(0);

  /**
   * Number of lifeline steals attempted by other places on this place that were
   * successful
   */
  public AtomicLong lifelineStealsSuffered = new AtomicLong(0);

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
   * Time stamp used during login to track the activity of the lifeline answer
   * thread
   */
  private long lifelineThreadTimestamp;

  /**
   * Counts the number of times the {@link GLBcomputer#lifelineAnswerThread()}
   * went through the <em>Active</em>\/<em>Inactive</em>\/<em>Hold</em> cycle.
   */
  public int lifelineThreadWokenUp = 0;

  /**
   * Integer identifier of the place this logger is responsible for
   */
  public final int place;

  /**
   * Time stamp of when the place starts computing. Is used for a priori
   * correction in {@link Logger#addPlaceLogger(PlaceLogger)}.
   */
  long startTimeStamp;

  /** Number of random steals attempted by this place */
  public AtomicLong stealsAttempted = new AtomicLong(0);

  /** Number of random steals that other places attempted on this place */
  public AtomicLong stealsReceived = new AtomicLong(0);

  /** Number of random steals attempted by this place that were successful */
  public AtomicLong stealsSuccess = new AtomicLong(0);

  /**
   * Number of random steals that other places successfully attempted on this
   * place
   */
  public AtomicLong stealsSuffered = new AtomicLong(0);

  /**
   * Array that Tracks the time spent by the place running 'index' number of
   * workers. This information is gathered by computing the difference between
   * timestamps obtained by calling {@link System#nanoTime()}.
   *
   * @see #workerStarted()
   * @see #workerStopped()
   */
  public long time[];

  /**
   * Array that indicates the time spent by the place with 'index' workers
   * stealing from the queues.
   *
   * @see #workerStealing()
   * @see #workerResumed()
   */
  public long timeStealing[];

  /**
   * Contains instances of class {@link TunerStamp} for each time the place's
   * tuner changes the values used.
   */
  TunerStamp[] tuning = new TunerStamp[100];

  /** Next free place in {@link #tuning} array */
  int tuningIndex = 0;

  /** Indicates the number of workers tasks currently running on the place. */
  public int workerCount = 0;

  /** Counts the number of times a worker was spawned */
  public long workerSpawned = 0;

  /**
   * Indicates how many of the worker tasks spawned are not working but actually
   * stealing work from the shared queues. At any given time, the number of
   * workers that are actually working is given by the difference between
   * {@link #workerCount} and {@link #workerStealingCount}.
   */
  public int workerStealingCount = 0;

  /** Counter of the time spent yielding by workers on this place */
  public long yieldingTime = 0;

  /**
   * Constructor
   *
   * Sets up a PlaceLogger for runtime tracking.
   *
   * @param placeConfig
   *          Configuration instance containing the information of the
   *          parameters used for the {@link GLBcomputer} during the computation
   * @param placeId
   *          integer identifier of the place this PlaceLogger instance is
   *          recording activity for
   */
  public PlaceLogger(Configuration placeConfig, int placeId) {
    place = placeId;
    time = new long[placeConfig.x + 1];
    timeStealing = new long[placeConfig.x + 1];
  }

  /**
   * Called when the lifeline answer thread becomes active again after being on
   * hold.
   * <p>
   * As part of the lifecycle of the lifeline answer thread of
   * {@link GLBcomputer}, this method is called when the holding state of the
   * thread ends.
   */
  synchronized void lifelineAnswerThreadActive() {
    final long stamp = System.nanoTime();
    lifelineThreadHold += stamp - lifelineThreadTimestamp;
    lifelineThreadTimestamp = stamp;
    lifelineThreadWokenUp++;
  }

  /**
   * Called when the lifeline answer thread ends its activity.
   */
  synchronized void lifelineAnswerThreadEnded() {
    final long stamp = System.nanoTime();
    lifelineThreadActive += stamp - lifelineThreadTimestamp;

  }

  /**
   * Called when a worker decides to wake up the lifeline answer thread. The
   * lifeline answer thread is now not inactive anymore but in a holding state
   * until it is effectively scheduled in the thread pool.
   */
  synchronized void lifelineAnswerThreadHold() {
    final long stamp = System.nanoTime();
    lifelineThreadInactive += stamp - lifelineThreadTimestamp;
    lifelineThreadTimestamp = stamp;
  }

  /**
   * Called when the lifeline answer thread becomes inactive.
   */
  synchronized void lifelineAnswerThreadInactive() {
    final long stamp = System.nanoTime();
    lifelineThreadActive += stamp - lifelineThreadTimestamp;
    lifelineThreadTimestamp = stamp;
  }

  /**
   * Called when a new {@link GLBcomputer#lifelineAnswerThread()}activity is
   * started.
   */
  synchronized void lifelineAnswerThreadStarted() {
    lifelineThreadTimestamp = System.nanoTime();
  }

  /**
   * To be called when the tuner changes the value of {@link Configuration#n}.
   *
   * @param timestamp
   *          time stamp of when the modification was done
   * @param newValue
   *          new value decided by the tuner
   */
  public void NvalueTuned(long timestamp, int newValue) {
    if (tuning.length == tuningIndex) {
      tuning = Arrays.copyOf(tuning, tuningIndex * 2);
    }
    tuning[tuningIndex++] = new TunerStamp(timestamp, newValue);
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
   * Signals that a worker that was stealing work from the shared queue was able
   * to steal some work and will now resume its computation
   */
  synchronized void workerResumed() {
    final long stamp = System.nanoTime();
    timeStealing[workerStealingCount] += stamp - lastWorkerStealingTimeStamp;
    lastWorkerStealingTimeStamp = stamp;
    workerStealingCount--;
  }

  /**
   * Signals that an extra worker has started working on the place.
   */
  synchronized void workerStarted() {
    final long stamp = System.nanoTime();
    if (isReset) {
      lastEventTimeStamp = stamp;
      lastWorkerStealingTimeStamp = stamp;
      startTimeStamp = stamp;
      isReset = false;
    }
    time[workerCount] += (stamp - lastEventTimeStamp);
    lastEventTimeStamp = stamp;
    workerSpawned++;
    workerCount++;
  }

  /**
   * Signals that a worker has ran out of work and will now attempt to steal
   * from the shared queue.
   */
  synchronized void workerStealing() {
    final long stamp = System.nanoTime();
    timeStealing[workerStealingCount] += stamp - lastWorkerStealingTimeStamp;
    lastWorkerStealingTimeStamp = stamp;
    workerStealingCount++;
  }

  /**
   * Signals that a worker on the place has stopped running.
   */
  synchronized void workerStopped() {
    final long stamp = System.nanoTime();
    time[workerCount] += stamp - lastEventTimeStamp;
    timeStealing[workerStealingCount] += stamp - lastWorkerStealingTimeStamp;
    lastEventTimeStamp = stamp;
    lastWorkerStealingTimeStamp = stamp;
    workerCount--;
    workerStealingCount--;
  }

  /**
   * Method called when a worker starts yielding to allow for other activities
   * to be run by the place.
   */
  synchronized void workerYieldStart() {
    final long stamp = System.nanoTime();
    time[workerCount] += stamp - lastEventTimeStamp;
    lastEventTimeStamp = stamp;
    workerCount--;
    lastYield = stamp;
  }

  /**
   * Method called when a worker that was yielding resumes its normal execution.
   */
  synchronized void workerYieldStop() {
    final long stamp = System.nanoTime();
    time[workerCount] += (stamp - lastEventTimeStamp);
    lastEventTimeStamp = stamp;
    workerCount++;
    yieldingTime += stamp - lastYield;
  }

}