import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class WorldDungeon {
    public static void main(String[] args) {
        JFrame frame = new JFrame("World Dungeon");
        frame.setSize(1600, 1200);  
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  
        CustomPanel panel = new CustomPanel();
        frame.add(panel);  
        frame.setVisible(true);  
    }
}

class CustomPanel extends JPanel implements MouseWheelListener {
    private double scale = 1.0;
    private int translateX = 0;
    private int translateY = 0;
    
    // Grid cell size
    private final int GRID_SIZE = 5;
    
    // Dungeon circle parameters
    private final int DIAMETER = 100000;
    private final int CENTER_X = 0;
    private final int CENTER_Y = 0;
    private final int RADIUS = DIAMETER / 2;
    
    // Wall density control (increase for more walls)
    private double wallDensity = 10.0;
    
    // Container for randomly generated walls
    private ArrayList<Wall> walls = new ArrayList<>();
    
    public CustomPanel() {
        addMouseWheelListener(this);
        generateWalls();
    }
    
    // Generate random walls within the dungeon circle.
    private void generateWalls() {
        Random rand = new Random();
        int baseWallCount = 5000;
        int numWalls = (int)(wallDensity * baseWallCount);
        
        for (int i = 0; i < numWalls; i++) {
            int x, y;
            do {
                x = rand.nextInt(2 * RADIUS) - RADIUS;
                y = rand.nextInt(2 * RADIUS) - RADIUS;
            } while (!isInsideCircle(x, y, CENTER_X, CENTER_Y, RADIUS));
            
            int startX = snap(x);
            int startY = snap(y);
            
            boolean horizontal = rand.nextBoolean();
            int lengthInCells = 1 + rand.nextInt(10);
            int length = lengthInCells * GRID_SIZE;
            
            int endX = startX;
            int endY = startY;
            if (horizontal) {
                endX = startX + length;
            } else {
                endY = startY + length;
            }
            
            if (!isInsideCircle(endX, endY, CENTER_X, CENTER_Y, RADIUS)) {
                if (horizontal) {
                    endX = startX - length;
                } else {
                    endY = startY - length;
                }
            }
            
            walls.add(new Wall(startX, startY, endX, endY));
        }
    }
    
    // Snap a coordinate to the grid.
    private int snap(int coordinate) {
        return Math.round(coordinate / (float)GRID_SIZE) * 2;
    }
    
    // Check if a point (x,y) is within a circle.
    private boolean isInsideCircle(int x, int y, int centerX, int centerY, int radius) {
        int dx = x - centerX;
        int dy = y - centerY;
        return dx * dx + dy * dy <= radius * radius;
    }
    
    // Check if at least one endpoint of a wall is in the extended region.
    private boolean inExtendedRegion(Wall wall, int extStartX, int extEndX, int extStartY, int extEndY) {
        return (wall.x1 >= extStartX && wall.x1 <= extEndX && wall.y1 >= extStartY && wall.y1 <= extEndY) ||
               (wall.x2 >= extStartX && wall.x2 <= extEndX && wall.y2 >= extStartY && wall.y2 <= extEndY);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.translate(translateX, translateY);
        g2d.scale(scale, scale);
        
        // Draw dungeon circle.
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(27.0f));
        g2d.drawOval(CENTER_X - RADIUS - 1, CENTER_Y - RADIUS - 1, DIAMETER + 2, DIAMETER + 2);
        
        // Draw a light outline circle.
        g2d.setColor(Color.GRAY);
        g2d.setStroke(new BasicStroke(1.0f));
        g2d.drawOval(CENTER_X - RADIUS, CENTER_Y - RADIUS, DIAMETER, DIAMETER);
        
        // Set up variables for the extended region (only computed if the grid is drawn).
        Rectangle visibleRect = getVisibleRect();
        int gridStartX = 0, gridEndX = 0, gridStartY = 0, gridEndY = 0;
        int extStartX = 0, extEndX = 0, extStartY = 0, extEndY = 0;
        boolean hasExtendedRegion = false;
        
        int frameWidth = getWidth();
        int frameHeight = getHeight();
        double gridThreshold = 25.00 * Math.min(frameWidth, frameHeight) / 10000;
        if (scale > gridThreshold) {
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.setStroke(new BasicStroke(0.5f));
            
            gridStartX = ((int)(visibleRect.getMinX() - translateX / scale) / GRID_SIZE) * GRID_SIZE;
            gridEndX = ((int)(visibleRect.getMaxX() - translateX / scale) / GRID_SIZE) * GRID_SIZE;
            gridStartY = ((int)(visibleRect.getMinY() - translateY / scale) / GRID_SIZE) * GRID_SIZE;
            gridEndY = ((int)(visibleRect.getMaxY() - translateY / scale) / GRID_SIZE) * GRID_SIZE;
            
            for (int i = gridStartX; i <= gridEndX; i += GRID_SIZE) {
                g2d.drawLine(i, gridStartY, i, gridEndY);
            }
            for (int j = gridStartY; j <= gridEndY; j += GRID_SIZE) {
                g2d.drawLine(gridStartX, j, gridEndX, j);
            }
            
            int midX = (gridStartX + gridEndX) / 2;
            int midY = (gridStartY + gridEndY) / 2;
            int halfWidth = (gridEndX - gridStartX) / 2;
            int halfHeight = (gridEndY - gridStartY) / 2;
            
            // Extended region now uses 2x the half‑dimensions.
            int extHalfWidth = halfWidth * 2;
            int extHalfHeight = halfHeight * 2;
            extStartX = midX - extHalfWidth;
            extEndX = midX + extHalfWidth;
            extStartY = midY - extHalfHeight;
            extEndY = midY + extHalfHeight;
            
            hasExtendedRegion = true;
        }
        
        // Draw walls.
        // Walls in the inner 75% of the dungeon circle are always drawn.
        // Walls outside that inner core are drawn only if at least one endpoint is within the extended region.
        g2d.setColor(Color.BLUE);
        g2d.setStroke(new BasicStroke(4.0f));
        int innerRadius = (int)(RADIUS * 0.75);
        for (Wall wall : walls) {
            boolean inInner = isInsideCircle(wall.x1, wall.y1, CENTER_X, CENTER_Y, innerRadius) ||
                              isInsideCircle(wall.x2, wall.y2, CENTER_X, CENTER_Y, innerRadius);
            boolean inExtended = hasExtendedRegion && inExtendedRegion(wall, extStartX, extEndX, extStartY, extEndY);
            if (inInner || inExtended) {
                g2d.drawLine(wall.x1, wall.y1, wall.x2, wall.y2);
            }
        }
    }
    
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        double oldScale = scale;
        if (e.getWheelRotation() < 0) {
            scale *= 1.1;
        } else {
            scale /= 1.1;
        }
        double scaleChange = scale / oldScale;
        translateX = (int) (e.getX() - scaleChange * (e.getX() - translateX));
        translateY = (int) (e.getY() - scaleChange * (e.getY() - translateY));
        revalidate();
        repaint();
    }
    
    // Simple inner class representing a wall.
    private class Wall {
        int x1, y1, x2, y2;
        Wall(int x1, int y1, int x2, int y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
    }
}
