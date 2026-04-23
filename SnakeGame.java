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
            Point newFood;
            do {
                newFood = new Point((int)(Math.random() * GRID_WIDTH), (int)(Math.random() * GRID_HEIGHT));
            } while (snake.contains(newFood) || (aiSnake != null && aiSnake.contains(newFood)) || newFood.equals(food));
            food = newFood;
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

            // Background
            g.setColor(Color.DARK_GRAY);
            g.fillRect(0, 0, getWidth(), getHeight());

            // Grid lines
            g.setColor(Color.GRAY);
            for(int i=0; i<=GRID_WIDTH; i++) {
                g.drawLine(i*CELL_SIZE, 0, i*CELL_SIZE, getHeight());
            }
            for(int i=0; i<=GRID_HEIGHT; i++) {
                g.drawLine(0, i*CELL_SIZE, getWidth(), i*CELL_SIZE);
            }

            // Snake
            g.setColor(Color.GREEN);
            for(Point p : snake) {
                g.fillRect(p.x * CELL_SIZE, p.y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }

            // Food
            g.setColor(Color.RED);
            g.fillRect(food.x * CELL_SIZE, food.y * CELL_SIZE, CELL_SIZE, CELL_SIZE);

            // Score
            g.setColor(Color.WHITE);
            g.drawString("Score: " + score + " Foods: " + foodEaten + " Level: " + level, 10, 20);

            // Level Up Message
            if(levelUpMessage != null) {
                g.setColor(Color.YELLOW);
                g.setFont(new Font("Arial", Font.BOLD, 20));
                FontMetrics fm = g.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(levelUpMessage)) / 2;
                int y = getHeight() / 2 - 50;
                g.drawString(levelUpMessage, x, y);
            }

            // Game Over
            if(gameOver) {
                g.setColor(Color.RED);
                FontMetrics fm = g.getFontMetrics();
                String msg1 = "Game Over! Score: " + score;
                String msg2 = "Press R to restart";
                int x1 = (getWidth() - fm.stringWidth(msg1)) / 2;
                int x2 = (getWidth() - fm.stringWidth(msg2)) / 2;
                int y = getHeight() / 2;
                g.drawString(msg1, x1, y);
                g.drawString(msg2, x2, y + 20);
            }
        }
    }
}