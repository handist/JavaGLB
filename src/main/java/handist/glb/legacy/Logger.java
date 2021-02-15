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
package handist.glb.legacy;

import java.io.PrintStream;
import java.io.Serializable;

/**
 * Class Logger
 * <p>
 * Class used to track the runtime of the global load balancer execution. It is
 * inspired by the Logger class of the <a href=
 * "https://github.com/x10-lang/x10/tree/master/x10.runtime/src-x10/x10/glb">X10
 * Global Load Balancer library</a>.
 *
 * @author Patrick Finnerty
 *
 */
public class Logger implements Serializable {

  /** Serial Version UID */
  private static final long serialVersionUID = 6482746530921995488L;

  /* Trackers for random steals */
  /** Number of random steals attempted */
  public long stealsAttempted = 0;
  /** Number of successful random steals */
  public long stealsSuccess = 0;
  /** Number of random steals received */
  public long stealsReceived = 0;
  /** Number of random steals received successful */
  public long stealsSuffered = 0;

  /* Trackers for lifeline steals */
  /** Number of lifeline steals attempted */
  public long lifelineStealsAttempted = 0;
  /** Number of successful lifeline steals */
  public long lifelineStealsSuccess = 0;
  /** Number of lifeline steals received */
  public long lifelineStealsReceived = 0;
  /** Number of lifeline steals received successful */
  public long lifelineStealsSuffered = 0;

  /** Timing variables */
  public long lastStartStopTimeStamp = -1;
  /** Counts the time spent alive */
  public long timeAlive = 0;
  /** Counts the time spent idling */
  public long timeDead = 0;
  /** Timestamp of when the computation first started on this place */
  public long startTime = 0;

  /**
   * Static method used in producing outputs. It converts a time-interval
   * measured in nanoseconds to String-displayed time-interval in milliseconds.
   *
   * @param nanoInterval
   *          time interval in nanoseconds
   * @return the millisecond time interval as a String to be displayed
   */
  private static String nanoToMilliSeconds(long nanoInterval) {
    return String.valueOf(nanoInterval / 1e6);
  }

  /**
   * Resets all members of this instance to make it fit for recording the events
   * of an other distributed computation.
   */
  public void reset() {
    stealsAttempted = 0;
    stealsSuccess = 0;
    stealsReceived = 0;
    stealsSuffered = 0;

    lifelineStealsAttempted = 0;
    lifelineStealsSuccess = 0;
    lifelineStealsReceived = 0;
    lifelineStealsSuffered = 0;

    lastStartStopTimeStamp = -1;
    timeAlive = 0;
    timeDead = 0;
    startTime = 0;
  }

  /**
   * To be called when a worker thread (re)starts working on a place.
   */
  public void startLive() {
    final long time = System.nanoTime();
    if (startTime == 0) {
      startTime = time;
    }
    if (lastStartStopTimeStamp >= 0) {
      timeDead += time - lastStartStopTimeStamp;
    }
    lastStartStopTimeStamp = time;
  }

  /**
   * To be called when a worker thread stops, either because it is waiting for a
   * steal answer or has run out of tasks.
   */
  public void stopLive() {
    final long time = System.nanoTime();
    timeAlive += time - lastStartStopTimeStamp;
    lastStartStopTimeStamp = time;
  }

  /**
   * Prints to the given output the recorded statistics of this instance. They
   * are written in a CSV format.
   *
   * @param o
   *          the output to which the information is to be written.
   */
  public void print(PrintStream o) {
    o.println("Steal Type;Success;Failed;Total;");
    o.println("Random Steals;" + stealsSuccess + ";"
        + (stealsAttempted - stealsSuccess) + ";" + stealsAttempted + ";");
    o.println("Lifeline Steals;" + lifelineStealsSuccess + ";"
        + (lifelineStealsAttempted - lifelineStealsSuccess) + ";"
        + lifelineStealsAttempted + ";");
    o.println("Activity;Time (ms);");
    o.println("Alive;" + nanoToMilliSeconds(timeAlive) + ";");
    o.println("Dead;" + nanoToMilliSeconds(timeDead) + ";");
    o.println("Total;" + nanoToMilliSeconds(timeAlive + timeDead) + ";");

  }

}
