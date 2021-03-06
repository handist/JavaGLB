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
package handist.glb;

import java.io.PrintStream;

import handist.glb.PlaceLogger.TunerStamp;

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
     * Adds the given {@link PlaceLogger} instance to the logs of each place.
     * The idle time of the place logger is adjusted to match the total time of
     * the computation (during the computation phase, each place starts slightly
     * after the beginning of the first place as some time is needed for the
     * computation to propagate across all places).
     *
     * @param l
     *            the {@link PlaceLogger} instance of a certain place in the
     *            computation.
     */
    synchronized void addPlaceLogger(PlaceLogger l) {
        final long loggerElapsed = l.lastEventTimeStamp - l.startTimeStamp;
        final long idleCorrection = computationTime - loggerElapsed;
        l.time[0] += idleCorrection;

        placeLogs[l.place] = l;
    }

    /**
     * Displays the computation runtime information on the provided output
     * stream in a <em>CSV</em> format.
     *
     * @param out
     *            the output stream on which the information is to be displayed
     */
    public void print(PrintStream out) {
        out.println("Initialization time (s);" + initializationTime / 1e9);
        out.println("Computation time (s); " + computationTime / 1e9);
        out.println("Result gathering (s); " + resultGatheringTime / 1e9);

        // Print the general counters for each place
        out.println(
                "Place;Worker Spawns;IntraQueueSplit;IntraQueueFedByWorker;IntraQueueFedByLifeline;IntraQueueEmptied;"
                        + "InterQueueSplit;InterQueueFed;InterQueueEmptied;"
                        + "Rdm Steals Attempted;Rdm Steals Successes;"
                        + "Rdm Steals Received;Rdm Steals Suffered;"
                        + "Lifeline Steals Attempts;Lifeline Steals Success;"
                        + "Lifeline Steals Received;Lifeline Steals Suffered;"
                        + "Lifeline Thread Active(s);Lifeline Thread Holding(s);"
                        + "Lifeline Thread Inactive(s);Lifeline Thread Woken Up;"
                        + "Information Sent; Information Received; Worker Yielding;");

        for (final PlaceLogger l : placeLogs) {
            out.println(l.place + ";" + l.workerSpawned + ";"
                    + l.intraQueueSplit + ";" + l.intraQueueFedByWorker + ";"
                    + l.intraQueueFedByLifeline + ";" + l.intraQueueEmptied
                    + ";" + l.interQueueSplit + ";" + l.interQueueFed + ";"
                    + l.interQueueEmptied + ";" + l.stealsAttempted + ";"
                    + l.stealsSuccess + ";" + l.stealsReceived + ";"
                    + l.stealsSuffered + ";" + l.lifelineStealsAttempted + ";"
                    + l.lifelineStealsSuccess + ";" + l.lifelineStealsReceived
                    + ";" + l.lifelineStealsSuffered + ";"
                    + l.lifelineThreadActive / 1e9 + ";"
                    + l.lifelineThreadHold / 1e9 + ";"
                    + l.lifelineThreadInactive / 1e9 + ";"
                    + l.lifelineThreadWokenUp + ";" + l.communicationSent + ";"
                    + l.communicationReceived + ";" + l.yieldingTime / 1e9
                    + ";");

        }
        out.println();

        // Print the time spent with all the workers on each place
        out.println("WORKER DATA");
        out.println("Nb of worker spawned");
        out.print("Place;");
        for (int i = 0; i < placeLogs[0].time.length; i++) {
            out.print(i + ";");
        }
        out.println();

        for (final PlaceLogger l : placeLogs) {
            out.print(l.place + ";");
            for (final long i : l.time) {
                out.print(i / 1e9 + ";");
            }
            out.println();
        }

        out.println("Nb of worker stealing");
        out.print("Place;");
        for (int i = 0; i < placeLogs[0].timeStealing.length; i++) {
            out.print(i + ";");
        }
        out.println();
        for (final PlaceLogger l : placeLogs) {
            out.print(l.place + ";");
            for (final long i : l.timeStealing) {
                out.print(i / 1e9 + ";");
            }
            out.println();
        }

        if (System.getProperty(Configuration.GLBM_TUNERCLASS, null) != null) {
            out.println("TUNER DATA");
            printTunerDataAsCSV(out);
        }
    }

    /**
     * Prints the tuner data as a formatted CSV format The data is printed in
     * columns with the time stamps normalized on the first stamp of each place
     *
     * @param out
     *            the output stream to which the output needs to be written
     */
    public void printTunerDataAsCSV(PrintStream out) {
        // Assemble the data
        final int places = placeLogs.length;
        final TunerStamp[][] data = new TunerStamp[places][0];
        for (int i = 0; i < places; i++) {
            data[i] = placeLogs[i].tuning;
        }

        // Array of long of the first stamp of each place
        final long[] firstStamp = new long[places];
        for (int i = 0; i < places; i++) {
            firstStamp[i] = data[i][0].stamp;
        }
        out.println();

        // Print the column headers
        String lineToPrint = "";
        for (int i = 0; i < places; i++) {
            final String place = "Place(" + i + ")";
            lineToPrint += place + " stamp;" + place + " grain;";
        }

        // Print each data point line after line
        int line = 0;
        final int[] lastGrainValue = new int[placeLogs.length];
        int hasData;
        do {
            out.println(lineToPrint); // Trick to avoid printing empty line of
                                      // ";;;;;"
            lineToPrint = "";
            hasData = places;
            for (int i = 0; i < places; i++) {
                if (data[i].length <= line || data[i][line] == null) {
                    // No data left for this place
                    hasData--;
                    lineToPrint += ";;";
                } else {
                    // Print the data
                    final TunerStamp toPrint = data[i][line];
                    final long stamp = toPrint.stamp - firstStamp[i];
                    final int grain = toPrint.n;
                    lastGrainValue[i] = grain;
                    lineToPrint += (stamp / 1e9) + ";" + grain + ";";
                }
            }
            line++;
        } while (hasData > 0);

        // Print the last value taken with timestamp being the total time of the
        // computation as the final data point
        for (final int lastGrain : lastGrainValue) {
            out.print(computationTime / 1e9 + ";" + lastGrain + ";");
        }
    }

    /**
     * Constructor (package visibility)
     * <p>
     * Initializes a Logger instance with the computation and result time
     * specified as parameter. It is assumed the result gathering starts
     * directly after the computation.
     *
     * @param initStart
     *            initialization start in nanosecond
     * @param computationStart
     *            starting timestamp in nanosecond
     * @param computationEnd
     *            end of computation timestamp in nanosecond
     * @param resultGatheringEnd
     *            end of result gathring timestamp in nanosecond
     * @param placeCount
     *            number of places in the system
     */
    Logger(long initStart, long computationStart, long computationEnd,
            long resultGatheringEnd, int placeCount) {
        initializationTime = computationStart - initStart;
        computationTime = computationEnd - computationStart;
        resultGatheringTime = resultGatheringEnd - computationEnd;
        placeLogs = new PlaceLogger[placeCount];
    }
}
