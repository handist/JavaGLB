/**
 *
 */
package handist.glb.examples.tsp;

/**
 * Launcher for a distributed computation of the TSP
 *
 * @author Patrick Finnerty
 *
 */
public class GlobalTsp {

  public static void test(TspProblem problem, TspBag b) {
    final TspBag split = new TspBag(problem);
    final Travel result = new Travel(0);
    split.merge(b.split(false));

    long start = System.nanoTime();
    while (!b.isEmpty()) {
      b.process(300, result);
    }
    final long firstBag = System.nanoTime() - start;

    start = System.nanoTime();
    while (!split.isEmpty()) {
      split.process(300, result);
    }
    final long secondBag = System.nanoTime() - start;

    System.err.println(firstBag / 1e9 + " " + secondBag / 1e9);
  }

  public static void main(String[] args) {
    TspBag bag;
    TspProblem problem;
    TspGlbComputer computer;
    try {
      if (args.length == 2) {
        problem = TspParser.parseFile(args[0], Integer.parseInt(args[1]));
      } else {

        problem = TspParser.parseFile(args[0]);
      }
      bag = new TspBag(problem);
      computer = TspGlbFactory.setupGLB();
    } catch (final Exception e) {
      e.printStackTrace();
      return;
    }

    bag.init();

    // test(problem, bag);

    System.out.println(problem);
    final Travel t = computer.compute(bag, () -> new TspBag(problem));

    System.out.println(t);
    t.printExploredNodes();
    computer.getLog().print(System.err);

  }
}
