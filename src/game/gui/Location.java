package game.gui;

/**
 * To keep the letter (A-Y a-y) and x, y coordinates of all the valid locations.
 */
public class Location {

    private String id; // A-Y a-y
    private double x, y;

    public Location(String id, double x, double y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }
}