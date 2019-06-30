package game;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import static org.junit.Assert.assertTrue;

/**
 * Test objective:
 * Determine whether the evaluation of the true state is correct:
 *  - one state consists of 8 digits (only 0, 1, 2);
 *  - 0: vacant,  1: bottom ring,  2: upper ring
 */

public class ExactStateTest {
    @Rule
    public Timeout globalTimeout = Timeout.millis(100);

    final static String[][] rawStates = {
            {"AA", "120212100"}, // AA
            {"BB", "020012021"}, // BA
            {"CC", "020012120"}, // CA
            {"DD", "020210021"}, // DA
            {"EH", "010021012"}, // EE
            {"FG", "200120012"}, // FE
            {"GF", "210120012"}, // GE
            {"HE", "210021210"}  // HE
    };

    final static String[] exactStates = {
            "120212100", // AA
            "000212120", // BB
            "021210020", // CC
            "001212020", // DD
            "012121000", // EH
            "210021002", // FG
            "012121200", // GF
            "210021210"  // HE
    };

    @Test
    public void testGetExactState() {
        for (int i = 0; i < rawStates.length; i++) {
            String getState = StepsGame.getExactState(rawStates[i][1], rawStates[i][0].charAt(1) - 'A');
            assertTrue("The correct state of piece " + rawStates[i][0] + " is " + exactStates[i] + ", but got " + getState, getState.equals(exactStates[i]));
        }
    }
}
