package game;

import game.gui.Board;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * This class provides the text interface for the Steps Game
 * <p>
 * The game is based directly on Smart Games' IQ-Steps game
 * (http://www.smartgames.eu/en/smartgames/iq-steps)
 */
public class StepsGame {

    private String[] finalPositions;

    public StepsGame(String currentPositions) {
        this.finalPositions = getSolutions(currentPositions);
    }

    // Initialize the board which has 50 locations
    private static int[] board = new int[50];

    /*
      State strings of the following piece placements: AA, AE, BA, BE, CA, CE ... HA, HE.
      0: vacant,  1: bottom ring,  2: upper ring
     */
    public static final String[] states = {"120212100", "012121002", "020012021", "010120210", "020012120", "010120012", "020210021", "010021210", "020210120", "010021012", "001012120", "200120012", "021012120", "210120012", "021210021", "210021210"};


    /**
     * Determine whether a piece placement is well-formed according to the following:
     * - it consists of exactly three characters
     * - the first character is in the range A .. H (shapes)
     * - the second character is in the range A .. H (orientations)
     * - the third character is in the range A .. Y and a .. y (locations)
     *
     * @param piecePlacement A string describing a piece placement
     * @return True if the piece placement is well-formed
     */
    static boolean isPiecePlacementWellFormed(String piecePlacement) {
        // Determine whether a piece placement is well-formed
        // The length of each piece placement must be 3
        if (piecePlacement.length() != 3)
            return false;
        char[] chars = piecePlacement.toCharArray();
        /*
          Check if the characters aren't out of valid range in ASCII code
          A: 65  H: 72  Y: 89  a: 97  y: 121
         */
        if ((int) chars[0] < 65 || (int) chars[0] > 72 || (int) chars[1] < 65 || (int) chars[1] > 72)
            return false;
        if ((int) chars[2] < 65 || (int) chars[2] > 121 || ((int) chars[2] > 89 && (int) chars[2] < 97))
            return false;
        return true;
    }


    /**
     * Determine whether a placement string is well-formed:
     * - it consists of exactly N three-character piece placements (where N = 1 .. 8);
     * - each piece placement is well-formed
     * - no shape appears more than once in the placement
     *
     * @param placement A string describing a placement of one or more pieces
     * @return True if the placement is well-formed
     */
    static boolean isPlacementWellFormed(String placement) {
        // Determine whether a placement is well-formed
        if (placement == null || placement.equals("") || placement.length() % 3 != 0)
            return false;
        // Record whether the piece has appeared. 0: Never, 1: Has appeared
        int[] appear = new int[8];
        for (int i = 0; i < placement.length() / 3; i++) {
            String piece = placement.substring(3 * i, 3 * i + 3);
            if (!isPiecePlacementWellFormed(piece))
                return false;
            // 'shape' is the distance between this piece and 'A'
            int shape = piece.charAt(0) - 65;
            if (appear[shape] == 1)
                // This piece has appeared before
                return false;
            else
                // This piece has not appeared before, update it
                appear[shape] = 1;
        }
        return true;
    }


    /**
     * Determine whether a placement sequence is valid.  To be valid, the placement
     * sequence must be well-formed and each piece placement must be a valid placement
     * (with the pieces ordered according to the order in which they are played).
     *
     * @param placement A placement sequence string
     * @return True if the placement sequence is valid
     */
    public static boolean isPlacementSequenceValid(String placement) {
        // Determine whether a placement sequence is valid
        // Reset the board
        Arrays.fill(board, 0);
        if (!isPlacementWellFormed(placement))
            return false;
        for (int i = 0; i < placement.length() / 3; i++) {
            String piece = placement.substring(3 * i, 3 * i + 3);
            int homeCoordinate = piece.charAt(2) - ((int) piece.charAt(2) < 90 ? 65 : 72);
            int firstCharIndex = piece.charAt(0) - 65;
            int secondCharIndex = piece.charAt(1) - 65;
            int pieceIndex = 2 * firstCharIndex + (secondCharIndex < 4 ? 0 : 1);
            String rawState = states[pieceIndex];
            String state = getExactState(rawState, secondCharIndex);
            if (isOffBoard(homeCoordinate, state))
                return false;
            if (!checkCollision(homeCoordinate, state))
                return false;
        }

        return true;
    }


    /**
     * Get the state of piece placement after rotating.
     *
     * @param rawState   The state string before rotating
     * @param secondChar To determine how many degrees the piece need to rotate
     * @return The state string after rotating if needed
     */
    public static String getExactState(String rawState, int secondChar) {
        // Get how many times the piece need to rotate clockwise.  0&4: No, 1&5: 90°, 2&6: 180°, 3&7: 270°
        int rotateTime = secondChar - (secondChar < 4 ? 0 : 4);
        if (rotateTime == 1 || rotateTime == 5)
            // Rotate 90° clockwise
            return rawState.substring(6, 7) + rawState.substring(3, 4) + rawState.substring(0, 1) + rawState.substring(7, 8) + rawState.substring(4, 5) + rawState.substring(1, 2) + rawState.substring(8, 9) + rawState.substring(5, 6) + rawState.substring(2, 3);
        else if (rotateTime == 2 || rotateTime == 6)
            // Rotate 180° clockwise
            return new StringBuilder(rawState).reverse().toString();
        else if (rotateTime == 3 || rotateTime == 7)
            // Rotate 270° clockwise
            return rawState.substring(2, 3) + rawState.substring(5, 6) + rawState.substring(8, 9) + rawState.substring(1, 2) + rawState.substring(4, 5) + rawState.substring(7, 8) + rawState.substring(0, 1) + rawState.substring(3, 4) + rawState.substring(6, 7);
        else
            // Don't need to rotate
            return rawState;
    }


    /**
     * Check whether any ring gets out of the board.
     *
     * @param home  The home coordinate of the piece
     * @param state To describe the state of current piece
     * @return True if any ring gets out of the board
     */
    private static boolean isOffBoard(int home, String state) {
        if (home < 1 || home > 48 || home == 9 || home == 40)
            return true;
        // ASCII code of '0' is 48
        int upleft = state.charAt(0) - 48;
        int up = state.charAt(1) - 48;
        int upright = state.charAt(2) - 48;
        int left = state.charAt(3) - 48;
        int right = state.charAt(5) - 48;
        int downleft = state.charAt(6) - 48;
        int down = state.charAt(7) - 48;
        int downright = state.charAt(8) - 48;
        if (upleft == 0 && up == 0 && upright == 0) {
            if (!(home > 0 && home < 39 && home % 10 != 0 && home % 10 != 9))
                return true;
        } else if (downleft == 0 && down == 0 && downright == 0) {
            if (!(home > 10 && home < 49 && home % 10 != 0 && home % 10 != 9))
                return true;
        } else if (upright == 0 && right == 0 && downright == 0) {
            if (!(home > 10 && home < 40 && home % 10 != 0))
                return true;
        } else if (upleft == 0 && left == 0 && downleft == 0) {
            if (!(home > 9 && home < 39 && home % 10 != 9))
                return true;
        } else if (!(home > 10 && home < 39 && home % 10 != 0 && home % 10 != 9))
            return true;
        return false;
    }


    /**
     * Check collision among piece placements:
     * - Each of the bottom rings must be placed onto vacant pegs (pegs that are not occupied by already played pieces).
     * - None of the bottom rings may be obstructed by the upper layer of pieces already played. Notice that for each peg, it may be obstructed by up to four upper-level locations.
     * - Each of the upper rings must be placed onto vacant locations (locations that are not occupied by already played pieces).
     * State map:
     * 0: vacant,  1: bottom ring,  2: upper ring， 3：be obstructed by upper ring
     * －－－－－－－－－－－－－－－－－
     * | upleft |  up   | upright |
     * |  left  | center|  right  |
     * |downleft| down  |downright|
     * －－－－－－－－－－－－－－－－－
     *
     * @param home  The home coordinate of the piece
     * @param state To describe the state of current piece placement
     * @return True if no collision between current piece placement and any other piece placements
     */
    private static boolean checkCollision(int home, String state) {
        int upleft = state.charAt(0) - 48;
        int up = state.charAt(1) - 48;
        int upright = state.charAt(2) - 48;
        int left = state.charAt(3) - 48;
        int center = state.charAt(4) - 48;
        int right = state.charAt(5) - 48;
        int downleft = state.charAt(6) - 48;
        int down = state.charAt(7) - 48;
        int downright = state.charAt(8) - 48;

        if (upleft == 1)
            if (board[home - 11] != 0)
                return false;
            else
                board[home - 11] = 1;
        else if (upleft == 2) {
            if (board[home - 11] == 2)
                return false;
            else {
                board[home - 11] = 2;
                if ((home - 11) % 10 != 0 && board[home - 11 - 1] == 0)
                    board[home - 12] = 3;
                if ((home - 11) > 9 && board[home - 11 - 10] == 0)
                    board[home - 11 - 10] = 3;
                if (up == 0 && board[home - 10] == 0)
                    board[home - 10] = 3;
                if (left == 0 && board[home - 1] == 0)
                    board[home - 1] = 3;
            }
        }
        if (up == 1)
            if (board[home - 10] != 0)
                return false;
            else
                board[home - 10] = 1;
        else if (up == 2) {
            if (board[home - 10] == 2)
                return false;
            else {
                board[home - 10] = 2;
                if ((home - 10) % 10 != 0 && board[home - 11] == 0 && upleft == 0)
                    board[home - 11] = 3;
                if ((home - 10) > 9 && board[home - 10 - 10] == 0)
                    board[home - 10 - 10] = 3;
                if ((home - 10) % 10 != 9 && board[home - 9] == 0 && upright == 0)
                    board[home - 9] = 3;
            }
        }
        if (upright == 1)
            if (board[home - 9] != 0)
                return false;
            else
                board[home - 9] = 1;
        else if (upright == 2) {
            if (board[home - 9] == 2)
                return false;
            else {
                board[home - 9] = 2;
                if (up == 0 && board[home - 10] == 0)
                    board[home - 10] = 3;
                if ((home - 9) > 9 && board[home - 9 - 10] == 0)
                    board[home - 9 - 10] = 3;
                if ((home - 9) % 10 != 9 && board[home - 9 + 1] == 0)
                    board[home - 9 + 1] = 3;
                if (right == 0 && board[home + 1] == 0)
                    board[home + 1] = 3;
            }
        }
        if (left == 1)
            if (board[home - 1] != 0)
                return false;
            else
                board[home - 1] = 1;
        else if (left == 2) {
            if (board[home - 1] == 2)
                return false;
            else {
                board[home - 1] = 2;
                if ((home - 1) % 10 != 0 && board[home - 1 - 1] == 0)
                    board[home - 1 - 1] = 3;
                if ((home - 1) > 9 && board[home - 1 - 10] == 0 && upleft == 0)
                    board[home - 1 - 10] = 3;
                if ((home - 1) < 40 && board[home - 1 + 10] == 0 && downleft == 0)
                    board[home - 1 + 10] = 3;
            }
        }
        if (center == 1)
            if (board[home] != 0)
                return false;
            else
                board[home] = 1;
        else if (center == 2) {
            if (board[home] == 2)
                return false;
            else {
                board[home] = 2;
                if (home % 10 != 0 && board[home - 1] == 0 && left == 0)
                    board[home - 1] = 3;
                if (home > 9 && board[home - 10] == 0 && up == 0)
                    board[home - 10] = 3;
                if (home < 40 && board[home + 10] == 0 && down == 0)
                    board[home + 10] = 3;
                if (home % 10 != 9 && board[home + 1] == 0 && right == 0)
                    board[home + 1] = 3;
            }
        }
        if (right == 1)
            if (board[home + 1] != 0)
                return false;
            else
                board[home + 1] = 1;
        else if (right == 2) {
            if (board[home + 1] == 2)
                return false;
            else {
                board[home + 1] = 2;
                if ((home + 1) > 9 && board[home - 9] == 0 && upright == 0)
                    board[home - 9] = 3;
                if ((home + 1) % 10 != 9 && board[home + 1 + 1] == 0)
                    board[home + 1 + 1] = 3;
                if ((home + 1) < 40 && board[home + 1 + 10] == 0 && downright == 0)
                    board[home + 1 + 10] = 3;
            }
        }
        if (downleft == 1)
            if (board[home + 9] != 0)
                return false;
            else
                board[home + 9] = 1;
        else if (downleft == 2) {
            if (board[home + 9] == 2)
                return false;
            else {
                board[home + 9] = 2;
                if ((home + 9) % 10 != 0 && board[home + 9 - 1] == 0)
                    board[home + 9 - 1] = 3;
                if (board[home - 1] == 0 && left == 0)
                    board[home - 1] = 3;
                if (board[home + 10] == 0 && down == 0)
                    board[home + 10] = 3;
                if ((home + 9) < 40 && board[home + 9 + 10] == 0)
                    board[home + 9 + 10] = 3;
            }
        }
        if (down == 1)
            if (board[home + 10] != 0)
                return false;
            else
                board[home + 10] = 1;
        else if (down == 2) {
            if (board[home + 10] == 2)
                return false;
            else {
                board[home + 10] = 2;
                if ((home + 10) % 10 != 0 && board[home + 9] == 0 && downleft == 0)
                    board[home + 9] = 3;
                if ((home + 10) % 10 != 9 && board[home + 11] == 0 && downright == 0)
                    board[home + 11] = 3;
                if ((home + 10) < 40 && board[home + 10 + 10] == 0)
                    board[home + 10 + 10] = 3;
            }
        }
        if (downright == 1)
            if (board[home + 11] != 0)
                return false;
            else
                board[home + 11] = 1;
        else if (downright == 2) {
            if (board[home + 11] == 2)
                return false;
            else {
                board[home + 11] = 2;
                if (board[home + 10] == 0 && down == 0)
                    board[home + 10] = 3;
                if (board[home + 1] == 0 && right == 0)
                    board[home + 1] = 3;
                if ((home + 11) % 10 != 9 && board[home + 11 + 1] == 0)
                    board[home + 11 + 1] = 3;
                if ((home + 11) < 40 && board[home + 11 + 10] == 0)
                    board[home + 11 + 10] = 3;
            }
        }
        return true;
    }


    /**
     * Given a string describing a placement of pieces and a string describing
     * an (unordered) objective, return a set of all possible next viable
     * piece placements.   A viable piece placement must be a piece that is
     * not already placed (ie not in the placement string), and which will not
     * obstruct any other unplaced piece.
     *
     * @param placement A valid sequence of piece placements where each piece placement is drawn from the objective
     * @param objective A valid game objective, but not necessarily a valid placement string
     * @return An set of viable piece placements
     */
    public static Set<String> getViablePiecePlacements(String placement, String objective) {
        // Determine the correct order of piece placements
        HashMap<String, String> map = new HashMap<>();
        for (int i = 0; i < placement.length() / 3; i++) {
            String piece = placement.substring(3 * i, 3 * i + 3);
            if (!objective.contains(piece))
                return null;
            map.put(piece, "");
        }

        String[] rest = new String[(objective.length() - placement.length()) / 3];
        int index = 0;

        for (int i = 0; i < objective.length() / 3; i++) {
            String piece = objective.substring(3 * i, 3 * i + 3);
            if (map.get(piece) == null) {
                rest[index] = piece;
                index++;
            }
        }

        List<String> list = new ArrayList<>();
        permutation(rest, 0, rest.length - 1, list);

        Set<String> viable = new TreeSet<>();
        for (String s : list)
            if (isPlacementSequenceValid(placement + s))
                viable.add(s.substring(0, 3));

        return viable;
    }

    /**
     * Get all possible permutations of all the rest piece placements.
     *
     * @param str   All the remaining piece placements
     * @param start To start finding all permutations
     * @param end   To stop recursion
     * @param list  Save the results within this list
     */
    public static void permutation(String[] str, int start, int end, List<String> list) {
        if (start == end) {
            StringBuilder sb = new StringBuilder();
            for (String s : str)
                sb.append(s);
            list.add(sb.toString());
        } else {
            String temp;
            for (int i = start; i <= end; i++) {
                temp = str[start];
                str[start] = str[i];
                str[i] = temp;

                permutation(str, start + 1, end, list);

                temp = str[start];
                str[start] = str[i];
                str[i] = temp;
            }
        }
    }

    /**
     * Return an array of all unique (unordered) solutions to the game, given a
     * starting placement.   A given unique solution may have more than one than
     * one placement sequence, however, only a single (unordered) solution should
     * be returned for each such case.
     *
     * @param placement A valid piece placement string.
     * @return An array of strings, each describing a unique unordered solution to
     * the game given the starting point provided by placement.
     */
    static String[] getSolutions(String placement) {
        // Determine all solutions to the game, given a particular starting placement
        List<String> list = new ArrayList<>();
        try {
            InputStream is = Board.class.getResourceAsStream("assets/solutions");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String allSolutions;
            while ((allSolutions = br.readLine()) != null) {
                if (allSolutions.startsWith(placement)) {
                    list.add(allSolutions);
                }
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String[] solutions = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            solutions[i] = list.get(i);
        }
        return solutions;
    }

    public String[] returnFinalPositions() {
        return this.finalPositions;
    }
}
