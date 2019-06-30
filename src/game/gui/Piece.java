package game.gui;


import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class Piece extends ImageView {

    private static final int SQUARE_SIZE = 60;
    private static final int PIECE_IMAGE_SIZE = (int) ((3 * SQUARE_SIZE) * 1.33);
    public static final String URI_BASE = "assets/";
    protected String placement;
    protected boolean resized;

    /**
     * Creates a new view that represents a piece placement.
     *
     * @param placement a valid placement string
     */
    public Piece(String placement) {
        this.placement = placement;
        int index = placement.charAt(1) - 65;
        setImage(new Image(Board.class.getResource(URI_BASE + placement.substring(0, 1) + (index < 4 ? "A" : "E") + ".png").toString()));
        setRotate(90 * (index < 4 ? placement.charAt(1) - 65 : placement.charAt(1) - 69));
    }


    public void setPlacement(String placement) {
        this.placement = placement;
    }

    public String getPlacement() {
        return this.placement;
    }

    public void setResized(boolean resize) {
        this.resized = resize;
    }

    public boolean getResized() {
        return this.resized;
    }

    public void resizeToSmall() {
        setPreserveRatio(true);
        setFitHeight(125);
        setFitWidth(125);
        this.resized = true;
    }

    public void resizeToNormal() {
        setPreserveRatio(true);
        setFitHeight(280);
        setFitWidth(280);
        this.resized = false;
    }
}
