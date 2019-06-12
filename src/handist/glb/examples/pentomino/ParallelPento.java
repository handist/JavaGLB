/**
 *
 */
package handist.glb.examples.pentomino;

import handist.glb.examples.Sum;
import handist.glb.multiworker.GLBcomputer;
import handist.glb.multiworker.GLBfactory;

/**
 * @author Patrick Finnerty
 *
 */
public class ParallelPento {

  /**
   * @param args
   */
  public static void main(String[] args) {
    int width;
    int height;
    final GLBcomputer computer;
    try {
      computer = GLBfactory.setupGLB();
      width = Integer.parseInt(args[0]);
      height = Integer.parseInt(args[1]);

    } catch (final Exception e) {
      e.printStackTrace();
      return;
    }

    final Pentomino p = new Pentomino(width, height);
    p.init();
    final Sum s = computer.compute(p, () -> new Sum(0),
        () -> new Pentomino(width, height));

    System.out
        .println("Solution to H:" + height + " W:" + width + "  " + s.sum);
    computer.getLog().print(System.err);
  }

}
