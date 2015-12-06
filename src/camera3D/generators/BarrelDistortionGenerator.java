package camera3D.generators;

import camera3D.generators.StereoscopicGenerator;

/**
 * Optimized implementation of the barrel distortion algorithm.
 * 
 * Builds a lookup table to map pixels in sketch to the distorted view that will
 * be countered by the lenses.
 * 
 * Defaults to settings for an Oculus Rift's lenses.
 * 
 * Much thanks to this guy for explaining barrel distortion:
 * 
 * https://www.youtube.com/watch?v=B7qrgrrHry0
 * 
 * And to this guy for pointing out that the radius (r) must be normalized:
 * 
 * http://stackoverflow.com/questions/28130618/what-ist-the-correct-oculus-rift-
 * barrel-distortion-radius-function
 * 
 * @author James Schmitz
 *
 */

public class BarrelDistortionGenerator extends StereoscopicGenerator {

	private int width;
	private int height;

	private float pow2;
	private float pow4;
	private float zoom;

	private int[] arrayIndex;
	private int[] pixelMapping;

	public BarrelDistortionGenerator(int width, int height) {
		this.width = width;
		this.height = height;

		this.pow2 = 0.22f;
		this.pow4 = 0.24f;
		this.zoom = 1;

		calculatePixelMaps(pow2, pow4, zoom);
	}

	public BarrelDistortionGenerator setZoom(float zoom) {
		this.zoom = zoom;

		calculatePixelMaps(pow2, pow4, zoom);

		return this;
	}

	public BarrelDistortionGenerator setBarrelDistortionCoefficients(
			float pow2, float pow4) {
		this.pow2 = pow2;
		this.pow4 = pow4;

		calculatePixelMaps(pow2, pow4, zoom);

		return this;
	}

	public BarrelDistortionGenerator setDivergence(float divergence) {
		super.setDivergence(divergence);

		return this;
	}

	private void calculatePixelMaps(float pow2, float pow4, float zoom) {
		arrayIndex = new int[width * height];
		pixelMapping = new int[width * height];

		int xCenter = width / 2;
		int yCenter = height / 2;
		double rMax = Math
				.sqrt(Math.pow(xCenter / 2, 2) + Math.pow(yCenter, 2));

		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				double r = Math.sqrt(Math.pow(x - xCenter, 2)
						+ Math.pow(y - yCenter, 2));
				double theta = Math.atan2((y - yCenter), (x - xCenter));
				double rNorm = r / rMax;
				double rPrime = (1 / zoom)
						* r
						* (1 + pow2 * Math.pow(rNorm, 2) + pow4
								* Math.pow(rNorm, 4));
				double xPrime = rPrime * Math.cos(theta) + xCenter;
				double yPrime = rPrime * Math.sin(theta) + yCenter;

				if (xPrime < xCenter - width / 4
						|| xPrime >= xCenter + width / 4 || yPrime < 0
						|| yPrime >= height) {
					// black void
					if (x >= width / 4 && x < xCenter)
						arrayIndex[y * width + x - width / 4] = -1;
					if (x + width / 4 < width)
						arrayIndex[y * width + x + width / 4] = -1;
				} else {
					// right
					arrayIndex[y * width + x + width / 4] = 0;
					pixelMapping[y * width + x + width / 4] = ((int) Math
							.floor(yPrime) * width)
							+ ((int) Math.floor(xPrime));
					// left
					if (x < xCenter + width / 4) {
						arrayIndex[y * width + x - width / 4] = 1;
						pixelMapping[y * width + x - width / 4] = ((int) Math
								.floor(yPrime) * width)
								+ ((int) Math.floor(xPrime));
					}
				}
			}
		}
	}

	public void generateCompositeFrame(int[] pixelDest, int[][] pixelStorage) {
		for (int i = 0; i < pixelDest.length; ++i) {
			if (arrayIndex[i] >= 0)
				pixelDest[i] = pixelStorage[arrayIndex[i]][pixelMapping[i]];
			else
				pixelDest[i] = 0;
		}
	}
}