package game;

import game.gui.Board;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.util.Random;

import static org.junit.Assert.assertTrue;

/**
 * Used to check to see if the finding of unplaced pieces returns the correct value
 */
public class FindNotPlacedPiecesTest {
    @Rule
    public Timeout globalTimeout = Timeout.millis(100);

    String[] states = {"ABc", "ABcBAd", "ABcBAdCDe", "ABcBAdCDeDEa", "ABcBAdCDeDEaEDb","ABcBAdCDeDEaEDbFAA", "ABcBAdCDeDEaEDbFAAGAD", "ABcBAdCDeDEaEDbFAAGADHAd"};
    String[] pieces = {"ABc", "BBc", "CBc", "DBc", "EBc", "FBc", "GBc", "HBc"};

    @Test
    public void testAllStates(){
        String[] solutions = {"BCDEFGH", "CDEFGH", "DEFGH", "EFGH", "FGH", "GH", "H", ""};
        String currentSolution;
        for(int i = 0; i < states.length; i++){
            Board.currentPlacements = states[i];
            currentSolution = Board.findNotPlacedPieces();
            assertTrue("This is not correct it should be " + solutions[i] + " not, " + currentSolution, solutions[i].equals(currentSolution));
        }
    }

    @Test
    public void testRandomisedStates(){
        int random = new Random().nextInt(7);
        String solution = "ABCDEFGH";
        String placement = "";
        String foundSolution;
        String[] currentPieces = TestUtility.shufflePieces(pieces, random);

        for(int i = 0; i < currentPieces.length; i++){
            placement += currentPieces[i];
            solution = TestUtility.removeCharacter(solution, String.valueOf(currentPieces[i].charAt(0)));
        }
        Board.currentPlacements = placement;
        foundSolution = Board.findNotPlacedPieces();

        assertTrue("This is not correct it should be " + solution + " not, " + foundSolution, solution.equals(foundSolution));
    }
}
