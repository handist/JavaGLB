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
package handist.glb.examples.uts;

import java.io.PrintStream;
import java.io.Serializable;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import handist.glb.Bag;
import handist.glb.Configuration;
import handist.glb.GLBcomputer;
import handist.glb.GLBfactory;
import handist.glb.Logger;
import handist.glb.tuning.MergeEmptyTuner;
import handist.glb.tuning.SplitMergeTuner;
import handist.glb.util.SerializableSupplier;

/**
 * Implementation of an Unbalanced Tree Search computation. This implementation
 * is a variant of {@link MultiworkerUTS} in which the split method of the
 * intr-bag gives its entire contents away. This splitting strategy is not
 * recommended for high scalability but was instead used to compare the
 * robustness of tuning mechanisms {@link SplitMergeTuner} and
 * {@link MergeEmptyTuner} against variations in problem implementations.
 *
 * @author Patrick Finnerty
 */
public class MultiworkerUTSsplit1
        implements Bag<MultiworkerUTSsplit1, Sum>, Serializable {

    /** Branching factor */
    protected final double den;

    /** Serial Version UID */
    protected static final long serialVersionUID = 4654891201916215845L;

    /**
     * Returns the SHA-1 {@link MessageDigest} to be used to generate the seed
     * of the tree.
     *
     * @return a {@link MessageDigest} instance
     */
    private static MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (final NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Prepares the various options that can be given to the program
     *
     * @return an {@link Options} instance containing all the possible options
     *         that can be given to the main program
     */
    private static Options commandOptions() {
        final Options opts = new Options();
        opts.addRequiredOption("b", "branch", true,
                "Branching factor, i.e. average number of children of each node.");
        opts.addRequiredOption("d", "depth", true,
                "Depth of the Exploration to perform");
        opts.addOption("w", "warmup", true,
                "warmup depth, setting this option will activate the warmup");
        return opts;
    }

    /**
     * Launches a distributed computation of the Unbalanced Tree Search using
     * the multiworker Global Load Balancer.
     *
     * @param args
     *            tree depth (positive integer), number of repetitions to
     *            perform (positive integer), show advanced computation
     *            statistics ("true" or "false)
     */
    public static void main(String[] args) {
        final Options programOptions = commandOptions();
        final CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(programOptions, args);
        } catch (final ParseException e1) {
            System.err.println(e1.getLocalizedMessage());
            final HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(
                    "java [...] MultiworkerUTS -d <integer> -b <integer> [-w <integer>]",
                    programOptions);
            return;
        }

        final int branchingFactor = Integer.parseInt(cmd.getOptionValue('b'));
        final int depth = Integer.parseInt(cmd.getOptionValue('d'));
        final int warmupSize = Integer.parseInt(cmd.getOptionValue('w', "0"));
        final int repetitions = 1;

        final SerializableSupplier<MultiworkerUTSsplit1> warmupSupplier = () -> {
            final MultiworkerUTSsplit1 warmup = new MultiworkerUTSsplit1(64,
                    branchingFactor);
            warmup.seed(19, depth - 2);
            return warmup;
        };

        GLBcomputer glb;
        try {
            glb = GLBfactory.setupGLB();

            final Configuration conf = glb.getConfiguration();

            if (warmupSize > 0) {
                final Logger warmupLog = glb.warmup(warmupSupplier,
                        () -> new Sum(0),
                        () -> new MultiworkerUTSsplit1(64, branchingFactor),
                        () -> new MultiworkerUTSsplit1(64, branchingFactor));
                System.out
                        .println("WARMUP TIME; "
                                + (warmupLog.initializationTime
                                        + warmupLog.computationTime) / 1e9
                                + ";");
                System.err.println("Warm-up Logs");
                warmupLog.print(System.err);
                System.err.println();
            }
            System.err.println("UTS Depth: " + depth + " " + conf);

            for (int i = 0; i < repetitions; i++) {
                final MultiworkerUTSsplit1 taskBag = new MultiworkerUTSsplit1(
                        64, branchingFactor);
                taskBag.seed(19, depth);

                final Sum s = glb.compute(taskBag, () -> new Sum(0),
                        () -> new MultiworkerUTSsplit1(64, branchingFactor));
                final Logger log = glb.getLog();
                System.err.println("Run " + i + "/" + repetitions + ";" + s.sum
                        + ";" + log.computationTime / 1e9 + ";");

                System.out.println(
                        "COMPUTATION TIME;" + log.computationTime / 1e9 + ";");
                System.out.println();
                log.print(System.out);
            }
        } catch (final ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    /**
     * Keeps track of the current position in the arrays.
     */
    int currentDepth;

    /** Array keeping track of the current depth of the node */
    int[] depth;

    /**
     * Counts the number of nodes explored.
     */
    long exploredNodes;

    /** Array containing the splittable hash used to generate the tree */
    byte[] hash;

    /**
     * Array containing the lower id of the next node to be explored at each
     * level in the tree. The actual number of leaves remaining to be explored
     * at each level is given by computing the difference between {@link #lower}
     * and {@link #upper} at a given index.
     */
    int[] lower;

    /**
     * {@link MessageDigest} instance held by this {@link Bag} instance. This
     * member is set to transient so as not to be serialized when instances of
     * this class are transfered from one place to another to perform load
     * balance.
     */
    transient MessageDigest md;

    /**
     * Array containing the upper id of the next node to be explored at each
     * level in the tree. The actual number of leaves remaining to be explored
     * at each level is given by computing the difference between {@link #lower}
     * and {@link #upper} at a given index. When exploring the tree, the node
     * with the highest id is always chosen first. When all the recursive
     * children of this node have been explored, the value in the {@link #upper}
     * array is decremented and the next node is explored (provided
     * {@link #lower} at that index is inferior to {@link #upper}).
     */
    int[] upper;

    /**
     * Initializes a new instance able to hold a tree exploration of depth the
     * specified parameter without needing to increase the size of the various
     * arrays used in the implementation.
     *
     * @param initialSize
     *            depth of the tree exploration
     * @param b
     *            branching factor
     */
    public MultiworkerUTSsplit1(int initialSize, int b) {
        hash = new byte[initialSize * 20 + 4];
        depth = new int[initialSize];
        lower = new int[initialSize];
        upper = new int[initialSize];

        exploredNodes = 0;
        final double branching = b;
        den = Math.log(branching / (1.0 + branching));
        md = getMessageDigest();
    }

    /**
     * Creates an empty instance with arrays prepared for the given size and the
     * density function pre-computed provided as parameter
     *
     * @param initialSize
     *            initial size of the arrays to create (corresponds to the depth
     *            of the exploration the instance can handle)
     * @param density
     *            pre-computed value for member {@link #den}
     */
    protected MultiworkerUTSsplit1(int initialSize, double density) {
        hash = new byte[initialSize * 20 + 4];
        depth = new int[initialSize];
        lower = new int[initialSize];
        upper = new int[initialSize];

        exploredNodes = 0;
        den = density;
        md = getMessageDigest();
    }

    /**
     * Generates the seed and the children nodes of node being currently
     * explored.
     *
     * @param d
     *            maximum depth of the tree to explore
     * @param md
     *            the {@link MessageDigest} used to generate the tree seed
     * @throws DigestException
     *             if the {@link MessageDigest} throws an exception when called
     */
    private void digest(int d, MessageDigest md) throws DigestException {
        // Creates more space in the arrays if need be
        if (currentDepth >= depth.length) {
            grow();
        }
        ++exploredNodes; // We are exploring one node (expanding its child
                         // nodes)

        // Writes onto array hash on the next 20 cells (=bytes)
        final int offset = currentDepth * 20;
        md.digest(hash, offset, 20);

        // Determine the number of child nodes based on the generated seed

        // v is the pseudo-random positive integer made out of the 4 bytes in
        // the
        // hash array generated by the message digest just above
        final int v = ((0x7f & hash[offset + 16]) << 24)
                | ((0xff & hash[offset + 17]) << 16)
                | ((0xff & hash[offset + 18]) << 8)
                | (0xff & hash[offset + 19]);

        final int n = (int) (Math.log(1.0 - v / 2147483648.0) / den);
        // 2.147.483.648 is written as 1 followed by 63 zeros in binary : -1.
        // v / 2.147.483.648 is then in the range (-2147483647,0]
        // n is then a positive integer, sometimes = 0, sometimes greater.
        if (n > 0) {
            if (d > 1) { // Bound for the tree depth
                // We create node size
                depth[currentDepth] = d - 1;
                lower[currentDepth] = 0;
                upper[currentDepth] = n;
                currentDepth++;
            } else {
                exploredNodes += n;
            }
        }
    }

    /**
     * Explores one node on the tree and returns.
     *
     * @param md
     *            the {@link MessageDigest} instance to be used to generate the
     *            tree
     * @throws DigestException
     *             if the provided {@link MessageDigest} throws an exception
     */
    public void expand(MessageDigest md) throws DigestException {
        final int top = currentDepth - 1;

        final int d = depth[top];
        final int l = lower[top];
        final int u = upper[top] - 1;
        if (u == l) {
            currentDepth = top; // We go back to the top node, we have explored
                                // all
                                // nodes on the top + 1 level
        } else {
            upper[top] = u; // We decrement the child nodes of top (the current
                            // node's
                            // parent node) : we have finished exploring all the
                            // child
                            // nodes of the current node
        }

        // Setting up a new 'seed' to explore the current node's child nodes
        final int offset = top * 20;
        hash[offset + 20] = (byte) (u >> 24);
        hash[offset + 21] = (byte) (u >> 16);
        hash[offset + 22] = (byte) (u >> 8);
        hash[offset + 23] = (byte) u;
        md.update(hash, offset, 24); // seed takes into account both the parent
                                     // seed
                                     // and 'u'
        digest(d, md);
    }

    /**
     * Increases the size of the arrays used in the implementation.
     */
    private void grow() {
        final int n = depth.length * 2;
        final byte[] h = new byte[n * 20 + 4];
        final int[] d = new int[n];
        final int[] l = new int[n];
        final int[] u = new int[n];
        System.arraycopy(hash, 0, h, 0, currentDepth * 20);
        System.arraycopy(depth, 0, d, 0, currentDepth);
        System.arraycopy(lower, 0, l, 0, currentDepth);
        System.arraycopy(upper, 0, u, 0, currentDepth);
        hash = h;
        depth = d;
        lower = l;
        upper = u;
    }

    /*
     * (non-Javadoc)
     *
     * @see apgas.glbm.Bag#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return currentDepth < 1;
    }

    /**
     * Indicates if the DepthFirstSearch exploration of the tree can be split.
     * This criteria is deemed satisfactorily met when at a certain point in the
     * current branch exploration, there remains at least 2 leaves, that is is
     * the difference between {@link #lower} and {@link #upper} at a certain
     * index is greater or equal to 2.
     */
    @Override
    public boolean isSplittable() {
        for (int i = 0; i < currentDepth; ++i) {
            if ((upper[i] - lower[i]) >= 2) {
                return true;
            }
        }
        return false;
    }

    /**
     * Stitches the given tree exploration to the current state. The arrays used
     * to contain the tree exploration information will increase in size to
     * accommodate the given tree if necessary.
     */
    @Override
    public void merge(MultiworkerUTSsplit1 b) {
        final int s = currentDepth + b.currentDepth;
        while (s > depth.length) {
            grow();
        }
        System.arraycopy(b.hash, 0, hash, currentDepth * 20,
                b.currentDepth * 20);
        System.arraycopy(b.depth, 0, depth, currentDepth, b.currentDepth);
        System.arraycopy(b.lower, 0, lower, currentDepth, b.currentDepth);
        System.arraycopy(b.upper, 0, upper, currentDepth, b.currentDepth);
        currentDepth = s;
        exploredNodes += b.exploredNodes;
    }

    /**
     * Prints the current status of this instance to the provided output stream.
     *
     * @param out
     *            output to which the state of the tree needs to be written to
     */
    public void print(PrintStream out) {
        out.println(String.format("Index :  %1$2d", currentDepth));

        out.print("Hash  : ");
        for (int i = 0; i < currentDepth; i++) {
            for (int j = 0; j < 10; j++) {
                out.print(String.format("%1$h", hash[i * 20 + j]));
            }
            out.print(" ");
        }
        out.println();

        out.print("Depth : ");
        for (int i = 0; i < currentDepth; i++) {
            out.print(String.format(" %1$2d", depth[i]));
        }
        out.println();

        out.print("Upper : ");
        for (int i = 0; i < currentDepth; i++) {
            out.print(String.format(" %1$2d", upper[i]));
        }
        out.println();

        out.print("Lower : ");
        for (int i = 0; i < currentDepth; i++) {
            out.print(String.format(" %1$2d", lower[i]));
        }
        out.println();
    }

    /**
     * Performs node exploration until either the "work-amount" of nodes is
     * explored or the tree exploration is finished. The second parameter is
     * unused. There is no requirement for workers to share information during
     * the tree exploration. The parameter is present to match the computation
     * abstraction of the multi-worker GLB design which allows this possibility
     */
    @Override
    public void process(int workAmount, Sum shared) {
        while (!isEmpty() && workAmount > 0) {
            try {
                expand(md);
            } catch (final DigestException e) {
                e.printStackTrace();
            }
            workAmount--;
        }

    }

    /**
     * Plants the seed of the tree. Needs to be called before the tree
     * exploration is started.
     *
     * @param seed
     *            an integer used as seed
     * @param depth
     *            maximum depth of the intended exploration
     */
    public void seed(int seed, int depth) {
        try {
            for (int i = 0; i < 16; ++i) {
                hash[i] = 0;
            }
            hash[16] = (byte) (seed >> 24);
            hash[17] = (byte) (seed >> 16);
            hash[18] = (byte) (seed >> 8);
            hash[19] = (byte) seed;
            md.update(hash, 0, 20);
            digest(depth, md);
        } catch (final DigestException e) {
        }
    }

    /**
     * Splits the tree exploration by giving half of the leaves remaining to
     * explore to an instance which is then returned.
     * <p>
     * If the boolean flag takeAll is true, always gives away all of the bag's
     * contents. This makes the intra-bag and the inter-bag give all their
     * contents whenever the split method is called on these instances.
     */
    @Override
    public MultiworkerUTSsplit1 split(boolean takeAll) {
        int s = 0;
        int t = 0;
        for (int i = 0; i < currentDepth; ++i) {
            final int nodesRemaining = upper[i] - lower[i];
            if (nodesRemaining >= 1) {
                if (nodesRemaining >= 2) {
                    ++s;
                }
                ++t;
            }
        }
        final MultiworkerUTSsplit1 split;
        if (takeAll /* && s == 0 */) {
            // Special case where the bag cannot be split. The whole content of
            // this
            // bag is given away as a result.
            split = new MultiworkerUTSsplit1(t, den);
            for (int i = 0; i < currentDepth; ++i) {
                final int p = upper[i] - lower[i];
                if (p >= 1) { // Copy only the nodes available for exploration
                    System.arraycopy(hash, i * 20, split.hash,
                            split.currentDepth * 20, 20);
                    split.depth[split.currentDepth] = depth[i];
                    split.upper[split.currentDepth] = upper[i];
                    split.lower[split.currentDepth++] = lower[i];
                }
            }
            currentDepth = 0; // This bag is now empty
        } else {
            // Split the bag as per usual
            split = new MultiworkerUTSsplit1(s, den);
            for (int i = 0; i < currentDepth; ++i) {
                final int p = upper[i] - lower[i];
                if (p >= 2) {
                    System.arraycopy(hash, i * 20, split.hash,
                            split.currentDepth * 20, 20);
                    split.depth[split.currentDepth] = depth[i];
                    split.upper[split.currentDepth] = upper[i];
                    split.lower[split.currentDepth++] = upper[i] -= p / 2;
                }
            }
        }
        return split;
    }

    /**
     * Adds the number of nodes explored as a result of the
     * {@link #process(int, Sum)} method by this instance into the given
     * {@link Sum}.
     */
    @Override
    public void submit(Sum r) {
        r.sum += exploredNodes;
    }

}
