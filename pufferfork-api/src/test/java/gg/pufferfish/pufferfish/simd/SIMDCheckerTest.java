package gg.pufferfish.pufferfish.simd;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorSpecies;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"deprecation", "removal", "unchecked"})
class SIMDCheckerTest {

    /*
     * Modify tests that depend on java version on java updates!
     */

    private Logger mockLogger;

    @BeforeEach
    void setup() {
        mockLogger = mock(Logger.class);
    }

    @Test
    void canEnable_returnsFalse_forOutOfRangeJavaVersion() {
        try (MockedStatic<SIMDDetection> mockedDetection = Mockito.mockStatic(SIMDDetection.class)) {
            mockedDetection.when(SIMDDetection::getJavaVersion).thenReturn(16);
            SIMDChecker checker = new SIMDChecker(IntVector.SPECIES_PREFERRED, FloatVector.SPECIES_PREFERRED);
            boolean result = checker.canEnable(mockLogger);
            assertFalse(result);
            mockedDetection.verify(SIMDDetection::getJavaVersion, atLeastOnce());
            verifyNoInteractions(mockLogger);
        }
        try (MockedStatic<SIMDDetection> mockedDetection = Mockito.mockStatic(SIMDDetection.class)) {
            mockedDetection.when(SIMDDetection::getJavaVersion).thenReturn(22);
            SIMDChecker checker = new SIMDChecker(IntVector.SPECIES_PREFERRED, FloatVector.SPECIES_PREFERRED);
            boolean result = checker.canEnable(mockLogger);
            assertFalse(result);
            verifyNoInteractions(mockLogger);
        }
    }

    @Test
    void canEnable_logsInfo_andReturnsTrue_whenSupported() {
        try (MockedStatic<SIMDDetection> mockedDetection = Mockito.mockStatic(SIMDDetection.class)) {
            mockedDetection.when(SIMDDetection::getJavaVersion).thenReturn(21);

            SIMDChecker checker = new SIMDChecker(IntVector.SPECIES_PREFERRED, FloatVector.SPECIES_PREFERRED);
            boolean result = checker.canEnable(mockLogger);

            assertTrue(result);

            verify(mockLogger, times(2)).info(contains("Max SIMD vector size on this system is"));
            verify(mockLogger, never()).warn(anyString());
        }
    }

    @Test
    void canEnable_warns_ifElementSizeTooSmall() {
        try (MockedStatic<SIMDDetection> mockedDetection = Mockito.mockStatic(SIMDDetection.class)) {
            mockedDetection.when(SIMDDetection::getJavaVersion).thenReturn(21);
            VectorSpecies<Integer> ISPEC = mock(VectorSpecies.class);
            VectorSpecies<Float> FSPEC = mock(VectorSpecies.class);
            Logger logger = mock(Logger.class);

            when(ISPEC.vectorBitSize()).thenReturn(256);
            when(FSPEC.vectorBitSize()).thenReturn(256);
            when(ISPEC.elementSize()).thenReturn(1);
            when(FSPEC.elementSize()).thenReturn(1);

            SIMDChecker checker = new SIMDChecker(ISPEC, FSPEC);

            boolean canEnable = checker.canEnable(logger);

            assertFalse(canEnable);
            verify(logger).warn("SIMD is not properly supported on this system!");
        }
    }

    @Test
    void canEnable_returnsTrue_forValidElementSizes() {
        try (MockedStatic<SIMDDetection> mockedDetection = Mockito.mockStatic(SIMDDetection.class)) {
            mockedDetection.when(SIMDDetection::getJavaVersion).thenReturn(21);
            VectorSpecies<Integer> ISPEC = mock(VectorSpecies.class);
            VectorSpecies<Float> FSPEC = mock(VectorSpecies.class);
            Logger logger = mock(Logger.class);

            when(ISPEC.vectorBitSize()).thenReturn(256);
            when(FSPEC.vectorBitSize()).thenReturn(256);
            when(ISPEC.elementSize()).thenReturn(4);
            when(FSPEC.elementSize()).thenReturn(4);

            SIMDChecker checker = new SIMDChecker(ISPEC, FSPEC);

            boolean canEnable = checker.canEnable(logger);

            assertTrue(canEnable);
            verify(logger).info("Max SIMD vector size on this system is 256 bits (int)");
            verify(logger).info("Max SIMD vector size on this system is 256 bits (float)");
        }
    }

    @Test
    void canEnable_returnsFalse_onException() {
        try (MockedStatic<SIMDDetection> mockedDetection = Mockito.mockStatic(SIMDDetection.class)) {
            mockedDetection.when(SIMDDetection::getJavaVersion).thenThrow(new RuntimeException("fail"));

            SIMDChecker checker = new SIMDChecker(IntVector.SPECIES_PREFERRED, FloatVector.SPECIES_PREFERRED);
            boolean result = checker.canEnable(mockLogger);

            assertFalse(result);
            verifyNoInteractions(mockLogger);
        }
    }

    /* Checks if there is no exception happening
     * Basically no-op; the only purpose is that check
     */
    @Test
    void canEnable_worksWithRealVectorSpecies() {
        Logger logger = mock(Logger.class);
        
        VectorSpecies<Integer> ISPEC = IntVector.SPECIES_PREFERRED;
        VectorSpecies<Float> FSPEC = FloatVector.SPECIES_PREFERRED;

        SIMDChecker checker = new SIMDChecker(ISPEC, FSPEC);

        assertDoesNotThrow(() -> {
            boolean enabled = checker.canEnable(logger);
        });
    }
}