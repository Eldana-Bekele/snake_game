import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.*;
import javax.swing.Timer;
import java.awt.event.*;

public class SnakeGame {

    public static void main(String[] args) {
        JFrame frame = new JFrame("Snake");
        frame.setSize(600, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        GamePanel gamePanel = new GamePanel();
        frame.add(gamePanel);

        frame.setVisible(true);
    }

    static class GamePanel extends JPanel {
        private static final int CELL_SIZE = 30;
        private static final int GRID_WIDTH = 20;
        private static final int GRID_HEIGHT = 20;
        private java.util.List<Point> snake;
        private enum Direction { UP, DOWN, LEFT, RIGHT }
        private Direction direction = Direction.RIGHT;
        private Timer timer;
        private Point food;
        private int score = 0;
        private boolean gameOver = false;
        private int level = 1;
        private int speed = 150;
        private int foodEaten = 0;
        private String levelUpMessage = null;
        private Timer levelUpTimer;
        private int moveCounter = 0;
        private java.util.List<Point> aiSnake;
        private enum PowerUpType { FREEZE, PORTAL }
        private static class PowerUp {
            Point position;
            PowerUpType type;
            PowerUp(Point p, PowerUpType t) {
                position = p;
                type = t;
            }
        }
        private java.util.List<PowerUp> powerUps = new java.util.ArrayList<>();
        private boolean freezeActive = false;
        private Timer freezeTimer;

        public GamePanel() {
            snake = new java.util.ArrayList<>();
            snake.add(new Point(8, 10));
            snake.add(new Point(9, 10));
            snake.add(new Point(10, 10));
            spawnFood();

            setFocusable(true);
            requestFocusInWindow();

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if(gameOver && e.getKeyCode() == KeyEvent.VK_R) {
                        reset();
                        return;
                    }
                    switch(e.getKeyCode()) {
                        case KeyEvent.VK_UP:
                            if(direction != Direction.DOWN) direction = Direction.UP;
                            break;
                        case KeyEvent.VK_DOWN:
                            if(direction != Direction.UP) direction = Direction.DOWN;
                            break;
                        case KeyEvent.VK_LEFT:
                            if(direction != Direction.RIGHT) direction = Direction.LEFT;
                            break;
                        case KeyEvent.VK_RIGHT:
                            if(direction != Direction.LEFT) direction = Direction.RIGHT;
                            break;
                    }
                }
            });

            timer = new Timer(150, e -> move());
            timer.start();
        }

        private void spawnFood() {
            do {
                food = new Point((int)(Math.random() * GRID_WIDTH), (int)(Math.random() * GRID_HEIGHT));
            } while (snake.contains(food) || (aiSnake != null && aiSnake.contains(food)));
        }

        private void spawnPowerUp() {
            PowerUpType type = Math.random() < 0.5 ? PowerUpType.FREEZE : PowerUpType.PORTAL;
            Point p;
            do {
                p = new Point((int)(Math.random() * GRID_WIDTH), (int)(Math.random() * GRID_HEIGHT));
            } while (snake.contains(p) || (aiSnake != null && aiSnake.contains(p)) || p.equals(food) || powerUps.stream().anyMatch(pu -> pu.position.equals(p)));
            powerUps.add(new PowerUp(p, type));
        }

        private void move() {
            if(gameOver) return;
            Point head = snake.get(snake.size() - 1);
            Point newHead = new Point(head.x, head.y);
            switch(direction) {
                case UP: newHead.y--; break;
                case DOWN: newHead.y++; break;
                case LEFT: newHead.x--; break;
                case RIGHT: newHead.x++; break;
            }

            // Check for power-ups
            boolean atePowerUp = false;
            for(PowerUp pu : new java.util.ArrayList<>(powerUps)) {
                if(newHead.equals(pu.position)) {
                    applyPowerUp(pu, newHead);
                    powerUps.remove(pu);
                    score++;
                    atePowerUp = true;
                }
            }

            // Check collision
            if(newHead.x < 0 || newHead.x >= GRID_WIDTH || newHead.y < 0 || newHead.y >= GRID_HEIGHT || snake.contains(newHead) || (aiSnake != null && aiSnake.contains(newHead))) {
                gameOver();
                return;
            }

            snake.add(newHead);
            if(newHead.equals(food)) {
                score++;
                foodEaten++;
                if(foodEaten >= 3) {
                    foodEaten = 0;
                    level++;
                    if(level == 10) {
                        aiSnake = new java.util.ArrayList<>();
                        aiSnake.add(new Point(1,1));
                        aiSnake.add(new Point(2,1));
                        aiSnake.add(new Point(3,1));
                    }
                    if(level == 5) {
                        speed = 150;
                    } else {
                        speed = Math.max(50, speed - 20);
                    }
                    timer.setDelay(speed);
                    levelUpMessage = "Level Up! Level " + level;
                    if(levelUpTimer != null) levelUpTimer.stop();
                    levelUpTimer = new Timer(2000, e -> { levelUpMessage = null; repaint(); });
                    levelUpTimer.setRepeats(false);
                    levelUpTimer.start();
                }
                spawnFood();
                if(level >= 5 && Math.random() < 0.2) spawnPowerUp();
            } else {
                snake.remove(0);
            }
            moveCounter++;
            if(level >= 5 && moveCounter % 10 == 0) {
                moveFood();
            }
            if(level >= 10) {
                moveAISnake();
            }
            repaint();
        }

        private void moveAISnake() {
            if(aiSnake == null || aiSnake.isEmpty()) return;
            Point head = aiSnake.get(aiSnake.size() - 1);
            int dx = Integer.compare(food.x, head.x);
            int dy = Integer.compare(food.y, head.y);
            Point newHead;
            if(dx != 0) {
                newHead = new Point(head.x + dx, head.y);
            } else if(dy != 0) {
                newHead = new Point(head.x, head.y + dy);
            } else {
                return; // on food
            }
            if(newHead.x < 0 || newHead.x >= GRID_WIDTH || newHead.y < 0 || newHead.y >= GRID_HEIGHT) return;
            if(snake.contains(newHead) || aiSnake.contains(newHead)) return;
            aiSnake.add(newHead);
            if(newHead.equals(food)) {
                spawnFood();
            } else {
                aiSnake.remove(0);
            }
        }

        private void moveFood() {
            if(freezeActive) return;
            Point newFood;
            do {
                newFood = new Point((int)(Math.random() * GRID_WIDTH), (int)(Math.random() * GRID_HEIGHT));
            } while (snake.contains(newFood) || (aiSnake != null && aiSnake.contains(newFood)) || newFood.equals(food));
            food = newFood;
        }

        private void applyPowerUp(PowerUp pu, Point newHead) {
            if(pu.type == PowerUpType.FREEZE) {
                freezeActive = true;
                if(freezeTimer != null) freezeTimer.stop();
                freezeTimer = new Timer(5000, e -> {
                    freezeActive = false;
                });
                freezeTimer.setRepeats(false);
                freezeTimer.start();
            } else if(pu.type == PowerUpType.PORTAL) {
                // Teleport snake head to food position
                newHead.setLocation(food.x, food.y);
                spawnFood();
            }
        }

        private void gameOver() {
            gameOver = true;
            timer.stop();
            repaint();
        }

        private void reset() {
            snake.clear();
            snake.add(new Point(8, 10));
            snake.add(new Point(9, 10));
            snake.add(new Point(10, 10));
            direction = Direction.RIGHT;
            score = 0;
            gameOver = false;
            foodEaten = 0;
            levelUpMessage = null;
            moveCounter = 0;
            if(levelUpTimer != null) {
                levelUpTimer.stop();
                levelUpTimer = null;
            }
            if(freezeTimer != null) {
                freezeTimer.stop();
                freezeTimer = null;
            }
            freezeActive = false;
            powerUps.clear();
            if(aiSnake != null) aiSnake.clear();
            if(level >= 10) {
                aiSnake = new java.util.ArrayList<>();
                aiSnake.add(new Point(1,1));
                aiSnake.add(new Point(2,1));
                aiSnake.add(new Point(3,1));
            }
            spawnFood();
            timer.start();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Background
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // Grid lines (optional, make subtle)
            g2d.setColor(new Color(50, 50, 50));
            for(int i=0; i<=GRID_WIDTH; i++) {
                g2d.drawLine(i*CELL_SIZE, 0, i*CELL_SIZE, getHeight());
            }
            for(int i=0; i<=GRID_HEIGHT; i++) {
                g2d.drawLine(0, i*CELL_SIZE, getWidth(), i*CELL_SIZE);
            }

            // Snake
            for(int i = 0; i < snake.size(); i++) {
                Point p = snake.get(i);
                if(i == snake.size() - 1) {
                    // Head
                    g2d.setColor(Color.GREEN);
                } else {
                    // Body
                    g2d.setColor(new Color(0, 150, 0));
                }
                g2d.fillRoundRect(p.x * CELL_SIZE + 2, p.y * CELL_SIZE + 2, CELL_SIZE - 4, CELL_SIZE - 4, 10, 10);
            }

            // AI Snake (if exists)
            if(aiSnake != null) {
                g2d.setColor(Color.BLUE);
                for(Point p : aiSnake) {
                    g2d.fillRoundRect(p.x * CELL_SIZE + 2, p.y * CELL_SIZE + 2, CELL_SIZE - 4, CELL_SIZE - 4, 10, 10);
                }
            }

            // Food (Apple)
            g2d.setColor(Color.RED);
            g2d.fillOval(food.x * CELL_SIZE + 2, food.y * CELL_SIZE + 2, CELL_SIZE - 4, CELL_SIZE - 4);
            g2d.setColor(new Color(139, 69, 19)); // Brown stem
            g2d.fillRect(food.x * CELL_SIZE + CELL_SIZE/2 - 2, food.y * CELL_SIZE, 4, 6);

            // Power-ups
            for(PowerUp pu : powerUps) {
                if(pu.type == PowerUpType.FREEZE) {
                    g2d.setColor(Color.CYAN);
                    g2d.fillOval(pu.position.x * CELL_SIZE, pu.position.y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                    g2d.setColor(Color.BLUE);
                    g2d.fillOval(pu.position.x * CELL_SIZE + 3, pu.position.y * CELL_SIZE + 3, CELL_SIZE - 6, CELL_SIZE - 6);
                } else {
                    g2d.setColor(Color.ORANGE);
                    g2d.fillOval(pu.position.x * CELL_SIZE, pu.position.y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                    g2d.setColor(Color.YELLOW);
                    g2d.fillOval(pu.position.x * CELL_SIZE + 3, pu.position.y * CELL_SIZE + 3, CELL_SIZE - 6, CELL_SIZE - 6);
                }
            }

            // Score
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2d.drawString("Score: " + score + " | Foods: " + foodEaten + " | Level: " + level, 10, 25);

            if(level >= 5) {
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
                g2d.drawString("Legend: Blue = Freeze Apple | Gold = Portal to Apple", 10, 45);
            }

            // Level Up Message
            if(levelUpMessage != null) {
                g2d.setColor(Color.YELLOW);
                g2d.setFont(new Font("SansSerif", Font.BOLD, 48));
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(levelUpMessage)) / 2;
                int y = getHeight() / 2;
                // Shadow for effect
                g2d.setColor(Color.BLACK);
                g2d.drawString(levelUpMessage, x + 2, y + 2);
                g2d.setColor(Color.YELLOW);
                g2d.drawString(levelUpMessage, x, y);
            }

            // Game Over
            if(gameOver) {
                g2d.setColor(Color.RED);
                g2d.setFont(new Font("SansSerif", Font.BOLD, 48));
                FontMetrics fm = g2d.getFontMetrics();
                String msg1 = "Game Over!";
                String msg2 = "Score: " + score;
                String msg3 = "Press R to Restart";
                int x1 = (getWidth() - fm.stringWidth(msg1)) / 2;
                int x2 = (getWidth() - fm.stringWidth(msg2)) / 2;
                int x3 = (getWidth() - fm.stringWidth(msg3)) / 2;
                int y = getHeight() / 2 - 50;
                // Shadow
                g2d.setColor(Color.BLACK);
                g2d.drawString(msg1, x1 + 2, y + 2);
                g2d.drawString(msg2, x2 + 2, y + 52);
                g2d.drawString(msg3, x3 + 2, y + 102);
                g2d.setColor(Color.RED);
                g2d.drawString(msg1, x1, y);
                g2d.drawString(msg2, x2, y + 50);
                g2d.drawString(msg3, x3, y + 100);
            }
        }
    }
}