package game.gui;

import game.StepsGame;
import javafx.animation.FadeTransition;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.util.*;

import static game.gui.Piece.URI_BASE;

public class Board extends Application {
    private static final int BOARD_WIDTH = 933;
    private static final int BOARD_HEIGHT = 700;
    private static final int PIECE_SIDE_LENGTH = 280;

    private final Group root = new Group();
    private final Group controls = new Group();
    private final Group pieces = new Group();
    private final Group draggablePieces = new Group();

    // Message on completion
    private final Text completionText = new Text("Congratulations!");

    // Message on no hint
    private final Text noHintText = new Text("You're on a wrong way...");

    private Text info = new Text("Press or hold / \nto get hint.\n\nPress P to pause\nor play music.");

    //show the difficulty now
    private final Text DiffText = new Text();

    private MediaPlayer mp;

    // For retrieving the answers
    static StepsGame sg;

    // Encodes all states of the game
    public static String currentPlacements = "";
    private static String[] finalPlacements;
    private String initialPlacements = "";

    private static List<Location> pegs = new ArrayList<>();
    private static List<Location> noPeg = new ArrayList<>();

    private final Button newGameBtn = new Button("New");
    private final Slider diffSlider = new Slider();
    private final Button visableHintBtn = new Button("Hint");
    private static Piece visableHint = null;
    private static boolean hinting = false;
    private int hintTimer = 0;
    private final int TIME_TO_DISPLAY_HINT = 20;

    // Contols whether or not button to create a new game and
    // slider to adjust difficulty are visible
    private boolean controlVisibility = true;

    // Difficulty of the game
    // 0-Starter  1-Junior  2-Expert  3-Master  4-Wizard
    private int difficulty = 0;


    /**
     * The draggable pieces with mouse action listeners.
     */
    class DraggablePiece extends Piece {

        int homeX, homeY;  // the position in the window where the mask should be when not on the board
        double mouseX, mouseY;  // the last known mouse positions (used when dragging)
        boolean dragging = false;  // keep tracking whether the piece is being dragging

        /**
         * Creates a new view that represents a piece placement.
         *
         * @param placement a valid placement string
         */
        public DraggablePiece(String placement) {
            super(placement);

            // scroll to change orientation
            setOnScroll(event -> {
                if (canRotate()) {
                    rotate(event.getDeltaY() < 0);
                    event.consume();
                }
            });


            // right click for flipping over the piece
            setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.SECONDARY) {
                    if (canFlip()) {
                        flip();
                        event.consume();
                    }
                }
            });

            // Get the layout of the piece and set it to normal size.
            setOnMousePressed(event -> {
                if (event.getButton() == MouseButton.PRIMARY && canBeDragged()) {
                    mouseX = event.getSceneX();
                    mouseY = event.getSceneY();
                    if (getResized()) {
                        setLayoutX(mouseX - 110);
                        setLayoutY(mouseY - 140);
                        resizeToNormal();
                    } else {
                        setLayoutX(getLayoutX() + event.getSceneX() - mouseX);
                        setLayoutY(getLayoutY() + event.getSceneY() - mouseY);
                    }
                }
            });

            setOnMouseDragged(event -> {
                if (event.getButton() == MouseButton.PRIMARY) {
                    if (canBeDragged()) {
                        takeOutPieceFromBoard();
                        setLayoutX(getLayoutX() + event.getSceneX() - mouseX);
                        setLayoutY(getLayoutY() + event.getSceneY() - mouseY);
                        mouseX = event.getSceneX();
                        mouseY = event.getSceneY();
                        toFront();
                        dragging = true;
                        event.consume();
                    }
                }
            });

            setOnMouseReleased(event -> {
                if (dragging && event.getButton() == MouseButton.PRIMARY) {
                    dragging = false;
                    Location location = getNearestLocation();
                    if (location != null) {
                        this.placement = this.placement.substring(0, 2) + location.getId();
                        // Test whether the nearest location is valid
                        if (StepsGame.isPlacementSequenceValid(currentPlacements + this.placement)) {
                            currentPlacements += this.placement;
                            root.getChildren().remove(visableHint);
                            setLayoutX(location.getX() - PIECE_SIDE_LENGTH / 2);
                            setLayoutY(location.getY() - PIECE_SIDE_LENGTH / 2);
                            toFront();
                            // Show completion message if finished
                            if (currentPlacements.length() == 24) {
                                visableHintBtn.setDisable(true);
                                showCompletion();
                            }
                        } else {
                            // Set the piece back and resize it to small
                            setLayoutX(getHomeX());
                            setLayoutY(getHomeY());
                            resizeToSmall();
                        }
                    } else {
                        // Can't find a nearest location, set the piece back and resize it to small
                        setLayoutX(getHomeX());
                        setLayoutY(getHomeY());
                        resizeToSmall();
                    }
                } else if (event.getButton() == MouseButton.PRIMARY && canBeDragged()) {
                    // Resize it to small after single click
                    setLayoutX(getHomeX());
                    setLayoutY(getHomeY());
                    resizeToSmall();
                }
            });
        }

        public int getHomeX() {
            return homeX;
        }

        public void setHomeX(int homeX) {
            this.homeX = homeX;
        }

        public int getHomeY() {
            return homeY;
        }

        public void setHomeY(int homeY) {
            this.homeY = homeY;
        }

        /**
         * Test whether the piece can rotate.
         *
         * @return True if it can be rotated
         */
        private boolean canRotate() {
            return !currentPlacements.contains(placement);
        }

        /**
         * Rotate the piece by scroll up or down.
         */
        private void rotate(boolean scrollDown) {
            double degree = scrollDown ? getRotate() + 90 : getRotate() + 270;
            setRotate(degree % 360);
            int rotate = (int) getRotate() / 90;
            char secondChar = placement.charAt(1);
            secondChar = secondChar < 'E' ? (char) (rotate + 'A') : (char) (rotate + 'E');
            setPlacement(placement.substring(0, 1) + String.valueOf(secondChar));
        }

        /**
         * Check whether a piece can flip over.
         *
         * @return True if it hasn't been placed on the board
         */
        private boolean canFlip() {
            return !currentPlacements.contains(placement);
        }

        /**
         * Flip over a piece and reset its image.
         */
        private void flip() {
            char secondChar = placement.charAt(1);
            secondChar = secondChar < 'E' ? (char) (secondChar + 4) : (char) (secondChar - 4);
            setPlacement(placement.substring(0, 1) + String.valueOf(secondChar));
            int index = placement.charAt(1) - 'A';
            setImage(new Image(Board.class.getResource(URI_BASE + placement.substring(0, 1) + (index < 4 ? "A" : "E") + ".png").toString()));
            setRotate(90 * (index < 4 ? placement.charAt(1) - 'A' : placement.charAt(1) - 'E'));
        }

        /**
         * Check whether a placed piece can be dragged.
         *
         * @return True if it hasn't been placed or obstructed by any other piece.
         */
        private boolean canBeDragged() {
            if (!currentPlacements.contains(placement.substring(0, 2)))
                return true;
            String withPosition = "";
            for (int i = 0; i < currentPlacements.length() / 3; i++) {
                String piece = currentPlacements.substring(3 * i, 3 * i + 3);
                if (piece.substring(0, 2).equals(placement.substring(0, 2)))
                    withPosition = piece;
            }
            String tempCurrent = currentPlacements;
            return StepsGame.isPlacementSequenceValid(tempCurrent.replace(withPosition, "") + withPosition);
        }

        /**
         * Take a piece away of the board.
         * Used in getting a piece back to unplaced area.
         */
        private void takeOutPieceFromBoard() {
            if (currentPlacements.contains(placement.substring(0, 2)))
                currentPlacements = currentPlacements.replace(placement, "");
        }


        /**
         * Get the nearest location among 50 locations for the piece which is being dragged.
         *
         * @return A valid location if close enough
         */
        private Location getNearestLocation() {
            Location nearest = null;
            double min = 1024;
            double distance;
            List<Location> list = placement.charAt(1) < 'E' ? pegs : noPeg;
            for (Location location : list) {
                distance = getDistance(location.getX(), location.getY());
                if (distance < min) {
                    min = distance;
                    nearest = location;
                }
            }
            return min < 30 ? nearest : null;
        }

        /**
         * Get the distance bewteen the piece and a pair of x and y coordinates.
         *
         * @param x x coordinate
         * @param y y coordinate
         * @return the distance
         */
        private double getDistance(double x, double y) {
            return Math.sqrt(Math.pow(getLayoutX() + PIECE_SIDE_LENGTH / 2 - x, 2) + Math.pow(getLayoutY() + PIECE_SIDE_LENGTH / 2 - y, 2));
        }
    }


    /**
     * Get the location from the placement of a piece by comparing the third digit with id of 50 locations.
     *
     * @param placement The 3 digits placement string
     * @return A location object for the piece
     */
    public Location getPieceLocation(String placement) {
        String pl = placement.substring(2, 3);
        List<Location> list = placement.charAt(1) < 'E' ? pegs : noPeg;
        for (Location location : list) {
            if (pl.equals(location.getId()))
                return location;
        }
        return null;
    }


    /**
     * Select a hint from viable pieces (Task 6).
     *
     * @return True if existing any hint.
     */
    boolean setVisableHint() {
        String randomFinal = finalPlacements[new Random().nextInt(finalPlacements.length)];
        Set<String> viablePieces = StepsGame.getViablePiecePlacements(currentPlacements, randomFinal);
        try {
            Iterator it = viablePieces.iterator();
            if (it.hasNext())
                visableHint = new Piece((String) it.next());
            else
                return false;
        } catch (NullPointerException e) {
            return false;
        }
        Location location = getPieceLocation(visableHint.placement);
        visableHint.setLayoutX(location.getX() - PIECE_SIDE_LENGTH / 2);
        visableHint.setLayoutY(location.getY() - PIECE_SIDE_LENGTH / 2);
        return true;
    }

    /**
     * Use two different ways to show hint on the board.
     * 1. The hint flashes on the board 3 times.
     * 2. The hint fades out within 3 seconds using FadeTransition.
     */
    void showVisableHint() {
        // Implement hints
        if (setVisableHint()) {
            root.getChildren().add(visableHint);
            if (Math.random() > 0.5) {
                FadeTransition ft = new FadeTransition(Duration.millis(850), visableHint);
                ft.setFromValue(1);
                ft.setToValue(0);
                ft.setCycleCount(3);
                ft.setOnFinished(event -> {
                    visableHintBtn.setDisable(false);
                    newGameBtn.setDisable(false);
                });
                ft.play();
                // Alternative way to implement this effect
                /*Runnable showHint = () -> {
                    try {
                        visableHint.setVisible(true);
                        Thread.sleep(800);
                        visableHint.setVisible(false);
                        Thread.sleep(600);
                        visableHint.setVisible(true);
                        Thread.sleep(800);
                        visableHint.setVisible(false);
                        visableHintBtn.setDisable(false);
                        newGameBtn.setDisable(false);
                    } catch (InterruptedException e) {
                        visableHint.setVisible(false);
                        visableHintBtn.setDisable(false);
                        newGameBtn.setDisable(false);
                    }
                };
                new Thread(showHint).start();*/
            } else {
                // The hint fades out in 3 seconds
                FadeTransition ft = new FadeTransition(Duration.millis(2800), visableHint);
                ft.setFromValue(1);
                ft.setToValue(0);
                ft.setOnFinished(event -> {
                    visableHintBtn.setDisable(false);
                    newGameBtn.setDisable(false);
                });
                ft.play();
            }
        } else {
            noHint();
        }
    }

    /**
     * Show no hint message with fading out effect.
     */
    private void noHint() {
        noHintText.toFront();
        FadeTransition ft = new FadeTransition(Duration.millis(2500), noHintText);
        ft.setFromValue(1);
        ft.setToValue(0);
        ft.setOnFinished(event -> {
            visableHintBtn.setDisable(false);
            newGameBtn.setDisable(false);
            noHintText.toBack();
        });
        ft.play();
    }


    /**
     * Make starting pieces according to the difficulty level selected.
     */
    private void makePieces() {
        int offsetY = 110;
        ArrayList pis = new ArrayList();
        String notPlacedPieces = findNotPlacedPieces();
        for (int i = 'A'; i <= 'H'; i++) {
            if (notPlacedPieces.contains(String.valueOf((char) (i)))) {
                pis.add(String.valueOf((char) (i)));
            }
        }
        for (int i = 0; i < pis.size(); i++) {
            DraggablePiece p = new DraggablePiece(pis.get(i) + "A");
            p.setLayoutX(800);
            p.setLayoutY(i * offsetY);
            p.setHomeX(800);
            p.setHomeY(i * offsetY);
            p.resizeToSmall();
            draggablePieces.getChildren().add(p);
        }
        root.getChildren().add(draggablePieces);
    }


    /**
     * Initialize the starting placements
     */
    private void initStartingPlacements() {
        // Implement starting placements
        String startingPlacements = "";
        try {
            InputStream is = Board.class.getResourceAsStream("assets/starting");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            int countlns = countLines("assets/starting");
            int select = 0;
            Random rand = new Random();

            // Generate interesting starting placements
            switch (difficulty) {
                case 0:
                    select = rand.nextInt(24);
                    break;
                case 1:
                    select = rand.nextInt(24) + 24;
                    break;
                case 2:
                    select = rand.nextInt(24) + 48;
                    break;
                case 3:
                    select = rand.nextInt(24) + 72;
                    break;
                case 4:
                    select = rand.nextInt(24) + 96;
                    if (select > countlns) {
                        select--;
                    }
                    break;
            }
            for (int i = 0; i <= select; i++) {
                startingPlacements = br.readLine();
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (int i = 0; i < startingPlacements.length() / 3; i++) {
            String piece = startingPlacements.substring(3 * i, 3 * i + 3);
            Piece p = new Piece(piece);
            double y = 0;
            double x = 0;
            String pl = piece.substring(2, 3);
            List<Location> list = piece.charAt(1) < 'E' ? pegs : noPeg;
            for (Location location : list) {
                if (pl.equals(location.getId())) {
                    x = location.getX() - PIECE_SIDE_LENGTH / 2;
                    y = location.getY() - PIECE_SIDE_LENGTH / 2;
                    break;
                }
            }
            p.setLayoutY(y);
            p.setLayoutX(x);
            p.toFront();

            pieces.getChildren().add(p);
        }
        root.getChildren().add(pieces);
        currentPlacements = startingPlacements;
        initialPlacements = startingPlacements;
    }

    /**
     * Counts the number of lines in a file
     * Used to allow for non-hardcoded adjustments to starting positions
     * Through getting the maximum number of choices allowed
     */
    public static int countLines(String filename) {
        try {
            LineNumberReader reader = new LineNumberReader(new InputStreamReader(Board.class.getResourceAsStream(filename)));
            int cnt = 0;
            String lineRead = "";
            while ((lineRead = reader.readLine()) != null) {
            }

            cnt = reader.getLineNumber();
            reader.close();
            return cnt;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }


    /**
     * Initialize 50 locations by setting their ID and x, y coordinates
     */
    private void initLocations() {
        double x = 0;
        double y = 0;
        double offsetX = 89;
        double offsetY = 89;

        int row = 0;
        for (int i = 0; i < 50; i++) {
            int asciiIndex = (i < 25) ? i + 'A' : i - 25 + 'a';
            String id = String.valueOf((char) asciiIndex);
            if (i % 10 == 0 && i > 0) {
                x = 0;
                y += 70;
                row++;
            }
            if (row % 2 == 0 && i % 2 == 0)
                pegs.add(new Location(id, x + offsetX, y + offsetY));
            else if (row % 2 == 1 && i % 2 == 1)
                pegs.add(new Location(id, x + offsetX, y + offsetY));
            else
                noPeg.add(new Location(id, x + offsetX, y + offsetY));
            x += 70;
        }

    }

    /**
     * Constructs the buttons and sliders
     */
    private void makeControls() {
        ImageView background = new ImageView(new Image(this.getClass().getResource(URI_BASE + "board.png").toString()));
        background.setPreserveRatio(false);
        background.setFitWidth(765);
        background.setFitHeight(417);
        background.setX(20);
        background.setY(20);
        background.toBack();

        root.getChildren().add(background);

        diffSlider.setVisible(controlVisibility);
        diffSlider.setTranslateX(-20);
        diffSlider.setTranslateY(0);
        diffSlider.setPrefWidth(230);
        diffSlider.setMin(0);
        diffSlider.setMax(4);
        diffSlider.setValue(0);
        diffSlider.setMajorTickUnit(1);
        diffSlider.setMinorTickCount(0);
        diffSlider.setSnapToTicks(true);
        diffSlider.setShowTickLabels(true);
        diffSlider.setShowTickMarks(true);

        newGameBtn.setVisible(controlVisibility);
        double r = 40;
        newGameBtn.setShape(new Circle(r));
        newGameBtn.setMinSize(2 * r, 2 * r);
        newGameBtn.setMaxSize(2 * r, 2 * r);
        newGameBtn.setTranslateX(250);
        newGameBtn.setTranslateY(-130);
        newGameBtn.setOnMouseClicked(event -> {
            difficulty = (int) (diffSlider.getValue());
            newGame();
        });

        visableHintBtn.setTranslateX(250);
        visableHintBtn.setTranslateY(-20);
        visableHintBtn.setShape(new Circle(r));
        visableHintBtn.setMinSize(2 * r, 2 * r);
        visableHintBtn.setMaxSize(2 * r, 2 * r);
        visableHintBtn.setOnMouseClicked(e -> {
            newGameBtn.setDisable(true);
            visableHintBtn.setDisable(true);
            showVisableHint();
        });

        controls.setLayoutX(90);
        controls.setLayoutY(600);

        controls.getChildren().addAll(newGameBtn, diffSlider, visableHintBtn);

        Rectangle hintRec = new Rectangle(255, 255);
        hintRec.setFill(Color.BEIGE);
        hintRec.setArcHeight(35);
        hintRec.setArcWidth(35);
        hintRec.setLayoutX(480);
        hintRec.setLayoutY(440);
        hintRec.toBack();
        InnerShadow is = new InnerShadow();
        is.setOffsetX(2.0f);
        is.setOffsetY(2.0f);
        hintRec.setEffect(is);
        root.getChildren().add(hintRec);

        info.setLayoutX(520);
        info.setLayoutY(520);
        info.setEffect(new GaussianBlur(2));
        info.setFill(Color.BURLYWOOD);
        info.setTextAlignment(TextAlignment.CENTER);
        info.setFont(Font.font("Arial", FontWeight.SEMI_BOLD, 25));
        root.getChildren().add(info);

        root.getChildren().add(controls);
    }

    /**
     * Returns a String of all non-placed pieces
     * This is encoded in the form of a String
     * With each letter corresponding to one of
     * The Eight Pieces
     */
    public static String findNotPlacedPieces() {
        String notPlaced = "ABCDEFGH";
        StringBuilder sb = new StringBuilder(notPlaced);

        String placements = currentPlacements;
        for (int i = 0; i < placements.length() / 3; i++) {
            String piece = placements.substring(3 * i, 3 * i + 3);
            if (notPlaced.contains(String.valueOf(piece.charAt(0)))) {
                sb.deleteCharAt(sb.indexOf(String.valueOf(piece.charAt(0))));
            }
        }
        return sb.toString();
    }

    /**
     * Make completion message
     */
    private void makeCompletion() {
        DropShadow ds = new DropShadow();
        ds.setOffsetY(4.0f);
        ds.setColor(Color.color(0.4f, 0.4f, 0.4f));
        completionText.setFill(Color.SEASHELL);
        completionText.setEffect(ds);
        completionText.setCache(true);
        completionText.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 80));
        completionText.setLayoutX(120);
        completionText.setLayoutY(255);
        completionText.setTextAlignment(TextAlignment.CENTER);
        root.getChildren().add(completionText);
    }

    /**
     * Make no hint message
     */
    private void makeHintText() {
        DropShadow ds = new DropShadow();
        ds.setOffsetY(4.0f);
        ds.setColor(Color.color(0.4f, 0.4f, 0.4f));
        noHintText.setFill(Color.AQUAMARINE);
        noHintText.setEffect(ds);
        noHintText.setCache(true);
        noHintText.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 60));
        noHintText.setLayoutX(70);
        noHintText.setLayoutY(255);
        noHintText.setTextAlignment(TextAlignment.CENTER);
        root.getChildren().add(noHintText);
    }


    /**
     * Show the completion message
     */
    private void showCompletion() {
        completionText.toFront();
        completionText.setOpacity(1);
    }


    /**
     * Hide the completion message
     */
    private void hideCompletion() {
        completionText.toBack();
        completionText.setOpacity(0);
    }
    /**
     * Show the difficulty level
     */
    private void ShowDiffText() {
        DropShadow ds = new DropShadow();
        ds.setOffsetY(1.0f);
        ds.setColor(Color.GRAY);
        if (difficulty == 0) {
            DiffText.setText("Starter");
        } else if (difficulty == 1) {
            DiffText.setText("Junior");
        } else if (difficulty == 2) {
            DiffText.setText("Expert");
        } else if (difficulty == 3) {
            DiffText.setText("Master");
        } else if (difficulty == 4) {
            DiffText.setText("Wizard");
        }
        DiffText.setFill(Color.BLACK);
        DiffText.setEffect(ds);
        DiffText.setCache(true);
        DiffText.setFont(Font.font("Arial", FontWeight.EXTRA_BOLD, 70));
        DiffText.setLayoutX(40);
        DiffText.setLayoutY(530);
        DiffText.setTextAlignment(TextAlignment.CENTER);
        root.getChildren().add(DiffText);
    }

    private void hideHintText() {
        noHintText.setOpacity(0);
    }

    /**
     * Start a new game, resetting everything as necessary
     */
    private void newGame() {
        // Implement a basic playable Steps Game in JavaFX that only allows pieces to be placed in valid places
        resetPieces();

        hideCompletion();

        hideHintText();

        initStartingPlacements();

        makePieces();

        ShowDiffText();

        visableHintBtn.setDisable(false);

        sg = new StepsGame(initialPlacements);
        finalPlacements = sg.returnFinalPositions();
    }

    /**
     * Removes the pieces and draggable pieces from the game
     * Clears both the aforementioned collections
     * Done to prevent overloading instances
     */
    private void resetPieces() {
        root.getChildren().remove(pieces);
        root.getChildren().remove(draggablePieces);
        pieces.getChildren().clear();
        draggablePieces.getChildren().clear();
        root.getChildren().remove(DiffText);
    }

    /**
     * Background music player.
     */
    private void makeMusic() {
        Media media = new Media(Board.class.getResource(URI_BASE + "mx3.mp3").toString());
        mp = new MediaPlayer(media);
        mp.setAutoPlay(true);
        mp.setVolume(0.9);
        mp.setOnEndOfMedia(() -> mp.seek(Duration.ZERO));
    }

    private void playMusic() {
        mp.play();
    }

    private void pauseMusic() {
        mp.pause();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("IQ-Steps");
        primaryStage.getIcons().add(new Image(this.getClass().getResource(URI_BASE + "AA.png").toString()));
        primaryStage.centerOnScreen();

        Scene scene = new Scene(root, BOARD_WIDTH, BOARD_HEIGHT, Color.BEIGE);
        Popup hint = new Popup();

        initLocations();
        makeControls();
        makeCompletion();
        makeHintText();
        makeMusic();
        newGame();
        playMusic();

        scene.setOnKeyPressed(ke -> {
            if (ke.getCode() == KeyCode.SLASH) {
                // For showing hint on the board
                /*
                 Checks to see if the current placements are less than the allowed amount and that the / key has been held down less than 20 ticks
                 If < 20 ticks we display the hard hint
                 If > 20 ticks we display the easy hint
                 If placements are = to the allowed placements we congratulate them
                 */
                if (!hinting && currentPlacements.length() < 24 && hintTimer >= TIME_TO_DISPLAY_HINT) {
                    hinting = true;
                    newGameBtn.setDisable(true);
                    visableHintBtn.setDisable(true);
                    showVisableHint();
                }
                if (checkNextHint() && currentPlacements.length() < 24) {
                    if (setVisableHint()) {
                        if (hintTimer < TIME_TO_DISPLAY_HINT) {
                            Piece p = new Piece(visableHint.getPlacement());
                            hint.getContent().add(p);
                            hint.setX(primaryStage.getX() + 475);
                            hint.setY(primaryStage.getY() + 455);
                            hint.show(primaryStage);
                        } else {
                            hint.getContent().clear();
                        }
                    } else {
                        noHint();
                    }


                } else if (currentPlacements.length() >= 24) {
                    Text finishedMessage = new Text();
                    finishedMessage.setFont(new Font(20));
                    finishedMessage.setText("You have already completed the puzzle!");
                    hint.getContent().add(finishedMessage);
                    hint.show(primaryStage);
                    hint.setX(PIECE_SIDE_LENGTH * 2);
                    hint.setY(PIECE_SIDE_LENGTH * 2 + 10);
                }
                hintTimer++;
            }

            else if (ke.getCode() == KeyCode.P) {
                if (mp.getStatus() == MediaPlayer.Status.PLAYING) {
                    pauseMusic();
                } else if (mp.getStatus() == MediaPlayer.Status.PAUSED) {
                    playMusic();
                }
            }
        });

        scene.setOnKeyReleased(ke -> {
            if (ke.getCode() == KeyCode.SLASH) {
                hint.getContent().clear();
                hint.hide();
                visableHint = null;
                root.getChildren().remove(visableHint);
                hinting = false;  // reset the hinting condition
                hintTimer = 0;
            }
        });

        primaryStage.setResizable(true);
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    // Tests to see if we can procure a next hint
    // Used to signify whether to show a piece or indicate
    // The removal of a piece
    private boolean checkNextHint() {
        if (finalPlacements.length > 0) {
            return true;
        }
        return false;
    }
}
