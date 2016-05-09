package mpr.control;

import static org.junit.Assert.assertTrue;

import java.awt.Point;

import org.junit.Test;


public class TestObliqueMprBuilder {

	@Test
	public void testObliqueCalculations() {
		Point p1 = new Point(100, 412);
		Point p2 = new Point(412, 100);
		DefaultObliqueMprBuilder mpr = new DefaultObliqueMprBuilder();
		Point[] oblique = mpr.oblique(p1, p2);
		for (int i = 1; i < oblique.length; ++i) {
			p1 = oblique[i - 1];
			p2 = oblique[i];
			assertTrue(p1.x < p2.x);
			assertTrue(p1.y > p2.y);
		}
		int half = oblique.length / 2;
		assertTrue(oblique[half].x == oblique[half].y); 
	}

}
