package game;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Test objective:
 * Determine whether the permutation of a string array is correct
 */
public class PermutationTest {
    @Rule
    public Timeout globalTimeout = Timeout.millis(100);

    final static String[][] strArray = {
            {},
            {"A"},
            {"A", "B"},
            {"A", "B", "C"}
    };

    final static String[][] permutation = {
            {},
            {"A"},
            {"AB", "BA"},
            {"ABC", "ACB", "BCA", "BAC", "CAB", "CBA"}
    };


    @Test
    public void testPermutation() {
        List<String> list1 = null;
        List<String> list2 = null;
        for (int i = 0; i < strArray.length; i++) {
            list1 = new ArrayList<>();
            StepsGame.permutation(strArray[i], 0, strArray[i].length - 1, list1);
            list2 = new ArrayList<>();
            for (int j = 0; j < permutation[i].length; j++)
                list2.add(permutation[i][j]);
            assertTrue("Got false permutation result. ", list1.containsAll(list2) && list2.containsAll(list1));
        }

    }

}
