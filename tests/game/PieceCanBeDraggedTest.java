package game;

import game.gui.Board;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test objective:
 * Determine whether one of the current placed pieces can be dragged:
 *  - if the piece is obstructed by other pieces, it should not be dragged
 */
public class PieceCanBeDraggedTest {

    private String placement;

    @Rule
    public Timeout globalTimeout = Timeout.millis(100);

    @Test
    public void testCanBeDragged() {
        Board.currentPlacements = "BGSAHQEFBGCgCDNHFlDAiFHn";
        placement = "BGS";
        assertFalse("Test piece " + placement + " shouldn't can be dragged, but was.", canBeDragged());
        placement = "AHQ";
        assertFalse("Test piece " + placement + " shouldn't can be dragged, but was.", canBeDragged());
        placement = "FHn";
        assertTrue("Test piece " + placement + " should can be dragged, but was not.", canBeDragged());
        placement = "DNH";
        assertTrue("Test piece " + placement + " should can be dragged, but was not.", canBeDragged());

        Board.currentPlacements = "";
        placement = "BGS";
        assertTrue("Test piece " + placement + " shouldn can be dragged, but was not.", canBeDragged());
    }


    boolean canBeDragged() {
        if (!Board.currentPlacements.contains(placement.substring(0, 2)))
            return true;
        String position = placement.substring(2, 3);
        String withPosition = "";
        for (int i = 0; i < Board.currentPlacements.length() / 3; i++) {
            String piece = Board.currentPlacements.substring(3 * i, 3 * i + 3);
            if (piece.substring(0, 2).equals(placement.substring(0, 2)))
                withPosition = piece;
        }
        String tempCurrent = Board.currentPlacements;
        return StepsGame.isPlacementSequenceValid(tempCurrent.replace(withPosition, "") + withPosition);
    }

}
