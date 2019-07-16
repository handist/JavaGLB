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

/**
 * Logger class for a distributed computation. Keeps information about the
 * global computation time and contains the specifics of each place. The runtime
 * information of each place are kept in instances of {@link PlaceLogger} class.
 *
 * @author Patrick Finnerty
 *
 */
public class Logger {

  /** Elapsed time during initialization */
  public long initializationTime;

  /** Elapsed computation time in nanosecond */
  public long computationTime;

  /** Array containing the {@link PlaceLogger} instance of each place */
  public PlaceLogger placeLogs[];

  /** Elapsed result gathering time in nanosecond */
  public long resultGatheringTime;

  /**
   * Adds the given {@link PlaceLogger} instance to the logs of each place. The
   * idle time of the place logger is adjusted to match the total time of the
   * computation (during the computation phase, each place starts slightly after
   * the beginning of the first place as some time is needed for the computation
   * to propagate across all places).
   *
   * @param l
   *          the {@link PlaceLogger} instance of a certain place in the
   *          computation.
   */
  synchronized void addPlaceLogger(PlaceLogger l) {
    final long loggerElapsed = l.lastEventTimeStamp - l.startTimeStamp;
    final long idleCorrection = computationTime - loggerElapsed;
    l.time[0] += idleCorrection;

    placeLogs[l.place] = l;
  }

  /**
   * Displays the computation runtime information on the provided output stream
   * in a <em>CSV</em> format.
   *
   * @param out
   *          the output stream on which the information is to be displayed
   */
  public void print(PrintStream out) {
    out.println("Computation time (s); " + computationTime / 1e9);
    out.println("Result gathering (s); " + resultGatheringTime / 1e9);
    out.print(
        "Place;Worker Spawns;IntraQueueSplit;IntraQueueFed;InterQueueSplit;InterQueueFed;"
            + "Rdm Steals Attempted;Rdm Steals Successes;"
            + "Rdm Steals Received;Rdm Steals Suffered;"
            + "Lifeline Steals Attempts;Lifeline Steals Success;"
            + "Lifeline Steals Received;Lifeline Steals Suffered;"
            + "Lifeline Thread Active(s);Lifeline Thread Holding(s);"
            + "Lifeline Thread Inactive(s);Lifeline Thread Woken Up;"
            + "Worker Yielding;");
    for (int i = 0; i < placeLogs[0].time.length; i++) {
      out.print(i + " workers(s);");
    }
    out.println();

    for (final PlaceLogger l : placeLogs) {
      out.print(l.place + ";" + l.workerSpawned + ";" + l.intraQueueSplit + ";"
          + l.intraQueueFed + ";" + l.interQueueSplit + ";" + l.interQueueFed
          + ";" + l.stealsAttempted + ";" + l.stealsSuccess + ";"
          + l.stealsReceived + ";" + l.stealsSuffered + ";"
          + l.lifelineStealsAttempted + ";" + l.lifelineStealsSuccess + ";"
          + l.lifelineStealsReceived + ";" + l.lifelineStealsSuffered + ";"
          + l.lifelineThreadActive / 1e9 + ";" + l.lifelineThreadHold / 1e9
          + ";" + l.lifelineThreadInactive / 1e9 + ";" + l.lifelineThreadWokenUp
          + ";" + l.yieldingTime / 1e9 + ";");

      for (final long i : l.time) {
        out.print(i / 1e9 + ";");
      }
      out.println();
    }

    out.println("Tuner data");
    for (int i = 0; i < placeLogs.length; i++) {
      out.print("Place " + i + ";Stamp;");
      final PlaceLogger pl = placeLogs[i];
      for (final PlaceLogger.TunerStamp ts : pl.tuning) {
        out.print(ts.stamp + ";");
      }
      out.println();
      out.print(";Value;");
      for (final PlaceLogger.TunerStamp ts : pl.tuning) {
        out.print(ts.n + ";");
      }
      out.println();
    }

  }

  /**
   * Constructor (package visibility)
   * <p>
   * Initializes a Logger instance with the computation and result time
   * specified as parameter. It is assumed the result gathering starts directly
   * after the computation.
   *
   * @param initStart
   *          initialization start in nanosecond
   * @param computationStart
   *          starting timestamp in nanosecond
   * @param computationEnd
   *          end of computation timestamp in nanosecond
   * @param resultGatheringEnd
   *          end of result gathring timestamp in nanosecond
   * @param placeCount
   *          number of places in the system
   */
  Logger(long initStart, long computationStart, long computationEnd,
      long resultGatheringEnd, int placeCount) {
    initializationTime = computationStart - initStart;
    computationTime = computationEnd - computationStart;
    resultGatheringTime = resultGatheringEnd - computationEnd;
    placeLogs = new PlaceLogger[placeCount];
  }
}
