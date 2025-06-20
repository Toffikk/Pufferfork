package gg.pufferfish.pufferfish.simd;

import org.slf4j.Logger;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorSpecies;

/**
 * Basically, java is annoying and we have to push this out to its own class.
 */
@Deprecated
public class SIMDChecker {
	private final VectorSpecies<Integer> ISPEC;
    private final VectorSpecies<Float> FSPEC;
	
	
    public SIMDChecker(VectorSpecies<Integer> ISPEC, VectorSpecies<Float> FSPEC) {
        this.ISPEC = ISPEC;
        this.FSPEC = FSPEC;
    }

	@Deprecated
	public boolean canEnable(Logger logger) {
		try {
			if (SIMDDetection.getJavaVersion() < 17 || SIMDDetection.getJavaVersion() > 21) {
				return false;
			} else {
				SIMDDetection.testRun = true;
				
				
				logger.info("Max SIMD vector size on this system is " + ISPEC.vectorBitSize() + " bits (int)");
				logger.info("Max SIMD vector size on this system is " + FSPEC.vectorBitSize() + " bits (float)");
				
				if (ISPEC.elementSize() < 2 || FSPEC.elementSize() < 2) {
					logger.warn("SIMD is not properly supported on this system!");
					return false;
				}

				return true;
			}
		} catch (NoClassDefFoundError | Exception ignored) {} // Basically, we don't do anything. This lets us detect if it's not functional and disable it.
		return false;
	}
	
}
