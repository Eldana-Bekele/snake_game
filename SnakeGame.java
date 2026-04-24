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
        private enum PowerUpType { FREEZE }
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
        private PowerUpType activePowerUpType = null;
        private Timer freezeTimer;
        private long freezeEndTime = 0;
        private java.util.List<Point> snowflakes = new java.util.ArrayList<>();
        private Timer powerUpTimer;
        private boolean gameStarted = false;
        private int startCountdown = 3;
        private Timer countdownTimer;

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
            
            // Power-up spawning timer - spawns continuously at level 12+
            powerUpTimer = new Timer(3000, e -> {
                if(level >= 12 && !gameOver && gameStarted) {
                    spawnPowerUp();
                }
            });
            powerUpTimer.start();
            
            startCountdown = 3;
            gameStarted = false;
            countdownTimer = new Timer(1000, e -> {
                startCountdown--;
                repaint();
                if(startCountdown <= 0) {
                    gameStarted = true;
                    countdownTimer.stop();
                    countdownTimer = null;
                    repaint();
                }
            });
            countdownTimer.setRepeats(true);
            countdownTimer.start();
        }

        private void spawnFood() {
            do {
                food = new Point((int)(Math.random() * GRID_WIDTH), (int)(Math.random() * GRID_HEIGHT));
            } while (snake.contains(food) || (aiSnake != null && aiSnake.contains(food)));
        }

        private void spawnPowerUp() {
            Point p = null;
            boolean valid = false;
            while (!valid) {
                p = new Point((int)(Math.random() * GRID_WIDTH), (int)(Math.random() * GRID_HEIGHT));
                final Point temp = p;
                valid = !snake.contains(p) && (aiSnake == null || !aiSnake.contains(p)) && !p.equals(food) && !powerUps.stream().anyMatch(pu -> pu.position.equals(temp));
            }
            powerUps.add(new PowerUp(p, PowerUpType.FREEZE));
        }

        private void move() {
            if(gameOver || !gameStarted) return;
            Point head = snake.get(snake.size() - 1);
            Point newHead = new Point(head.x, head.y);
            switch(direction) {
                case UP: newHead.y--; break;
                case DOWN: newHead.y++; break;
                case LEFT: newHead.x--; break;
                case RIGHT: newHead.x++; break;
            }

            // Check for power-ups
            for(PowerUp pu : new java.util.ArrayList<>(powerUps)) {
                if(newHead.equals(pu.position)) {
                    applyPowerUp(pu, newHead);
                    powerUps.remove(pu);
                    score++;
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
                freezeActive = false;
                activePowerUpType = null;
                freezeEndTime = 0;
                snowflakes.clear();
                if(freezeTimer != null) {
                    freezeTimer.stop();
                    freezeTimer = null;
                }
                if(foodEaten >= 3) {
                    foodEaten = 0;
                    level++;
                    if(level == 10) {
                        aiSnake = new java.util.ArrayList<>();
                        aiSnake.add(new Point(1,1));
                        aiSnake.add(new Point(2,1));
                        aiSnake.add(new Point(3,1));
                        speed = 150;
                    } else if(level > 10 && level < 12) {
                        speed = Math.max(100, speed - 5);
                    } else if(level >= 12) {
                        speed = Math.max(80, speed - 5);
                    } else if(level == 5) {
                        speed = 150;
                        spawnPowerUp();
                    } else if(level > 5 && level < 10) {
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
                if(level >= 12) spawnPowerUp();
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
                activePowerUpType = PowerUpType.FREEZE;
                snowflakes.clear();
                for(int i = 0; i < 50; i++) {
                    snowflakes.add(new Point((int)(Math.random() * getWidth()), (int)(Math.random() * getHeight())));
                }
                freezeEndTime = System.currentTimeMillis() + 5000;
                if(freezeTimer != null) freezeTimer.stop();
                freezeTimer = new Timer(5000, e -> {
                    freezeActive = false;
                    activePowerUpType = null;
                    snowflakes.clear();
                    repaint();
                });
                freezeTimer.setRepeats(false);
                freezeTimer.start();
            }
        }

        private void gameOver() {
            gameOver = true;
            timer.stop();
            powerUpTimer.stop();
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
            if(countdownTimer != null) {
                countdownTimer.stop();
                countdownTimer = null;
            }
            freezeActive = false;
            activePowerUpType = null;
            freezeEndTime = 0;
            startCountdown = 3;
            gameStarted = false;
            snowflakes.clear();
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
            countdownTimer = new Timer(1000, e -> {
                startCountdown--;
                repaint();
                if(startCountdown <= 0) {
                    gameStarted = true;
                    countdownTimer.stop();
                    countdownTimer = null;
                    repaint();
                }
            });
            countdownTimer.setRepeats(true);
            countdownTimer.start();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Gradient background
            GradientPaint background = new GradientPaint(0, 0, new Color(10, 15, 40), 0, getHeight(), new Color(5, 10, 25));
            g2d.setPaint(background);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // Subtle grid texture
            g2d.setStroke(new BasicStroke(1f));
            g2d.setColor(new Color(40, 55, 90, 90));
            for(int i = 0; i <= GRID_WIDTH; i++) {
                g2d.drawLine(i * CELL_SIZE, 0, i * CELL_SIZE, getHeight());
            }
            for(int i = 0; i <= GRID_HEIGHT; i++) {
                g2d.drawLine(0, i * CELL_SIZE, getWidth(), i * CELL_SIZE);
            }

            // Glow behind snake head
            Point head = snake.get(snake.size() - 1);
            RadialGradientPaint headGlow = new RadialGradientPaint(new Point(head.x * CELL_SIZE + CELL_SIZE/2, head.y * CELL_SIZE + CELL_SIZE/2), CELL_SIZE,
                    new float[]{0f, 1f}, new Color[]{new Color(120, 255, 120, 160), new Color(0,0,0,0)});
            g2d.setPaint(headGlow);
            g2d.fillOval(head.x * CELL_SIZE - CELL_SIZE/2 + 2, head.y * CELL_SIZE - CELL_SIZE/2 + 2, CELL_SIZE * 2 - 4, CELL_SIZE * 2 - 4);
            g2d.setPaint(null);

            // Snake
            for(int i = 0; i < snake.size(); i++) {
                Point p = snake.get(i);
                if(i == snake.size() - 1) {
                    GradientPaint headPaint = new GradientPaint(p.x * CELL_SIZE, p.y * CELL_SIZE, new Color(180, 255, 120),
                            p.x * CELL_SIZE + CELL_SIZE, p.y * CELL_SIZE + CELL_SIZE, new Color(60, 190, 50));
                    g2d.setPaint(headPaint);
                } else {
                    GradientPaint bodyPaint = new GradientPaint(p.x * CELL_SIZE, p.y * CELL_SIZE, new Color(20, 120, 40),
                            p.x * CELL_SIZE + CELL_SIZE, p.y * CELL_SIZE + CELL_SIZE, new Color(0, 180, 80));
                    g2d.setPaint(bodyPaint);
                }
                g2d.fillRoundRect(p.x * CELL_SIZE + 3, p.y * CELL_SIZE + 3, CELL_SIZE - 6, CELL_SIZE - 6, 12, 12);
                g2d.setColor(new Color(0, 0, 0, 90));
                g2d.drawRoundRect(p.x * CELL_SIZE + 3, p.y * CELL_SIZE + 3, CELL_SIZE - 6, CELL_SIZE - 6, 12, 12);
            }

            // Snake head eyes
            g2d.setColor(Color.BLACK);
            g2d.fillOval(head.x * CELL_SIZE + 10, head.y * CELL_SIZE + 10, 6, 8);
            g2d.fillOval(head.x * CELL_SIZE + 16, head.y * CELL_SIZE + 10, 6, 8);
            g2d.setColor(Color.WHITE);
            g2d.fillOval(head.x * CELL_SIZE + 12, head.y * CELL_SIZE + 12, 3, 4);
            g2d.fillOval(head.x * CELL_SIZE + 18, head.y * CELL_SIZE + 12, 3, 4);

            // AI Snake (if exists)
            if(aiSnake != null) {
                for(Point p : aiSnake) {
                    GradientPaint aiPaint = new GradientPaint(p.x * CELL_SIZE, p.y * CELL_SIZE, new Color(90, 160, 255),
                            p.x * CELL_SIZE + CELL_SIZE, p.y * CELL_SIZE + CELL_SIZE, new Color(20, 80, 190));
                    g2d.setPaint(aiPaint);
                    g2d.fillRoundRect(p.x * CELL_SIZE + 3, p.y * CELL_SIZE + 3, CELL_SIZE - 6, CELL_SIZE - 6, 12, 12);
                    g2d.setColor(new Color(0, 0, 0, 70));
                    g2d.drawRoundRect(p.x * CELL_SIZE + 3, p.y * CELL_SIZE + 3, CELL_SIZE - 6, CELL_SIZE - 6, 12, 12);
                }
            }
            g2d.setPaint(null);

            // Food (Apple)
            int foodX = food.x * CELL_SIZE;
            int foodY = food.y * CELL_SIZE;
            g2d.setColor(new Color(220, 40, 50));
            g2d.fillOval(foodX + 4, foodY + 4, CELL_SIZE - 8, CELL_SIZE - 8);
            g2d.setColor(new Color(180, 20, 30));
            g2d.fillOval(foodX + 6, foodY + 6, CELL_SIZE - 12, CELL_SIZE - 12);
            g2d.setColor(new Color(255, 220, 220, 150));
            g2d.fillOval(foodX + 8, foodY + 8, 8, 8);
            g2d.setColor(new Color(100, 55, 20));
            g2d.fillRoundRect(foodX + CELL_SIZE/2 - 3, foodY + 1, 6, 10, 3, 3);
            g2d.setColor(new Color(50, 160, 60));
            g2d.fillArc(foodX + CELL_SIZE/2 - 8, foodY - 4, 16, 14, 0, 180);

            // Power-ups
            for(PowerUp pu : powerUps) {
                g2d.setColor(new Color(100, 240, 255, 120));
                g2d.fillOval(pu.position.x * CELL_SIZE + 1, pu.position.y * CELL_SIZE + 1, CELL_SIZE - 2, CELL_SIZE - 2);
                g2d.setPaint(new RadialGradientPaint(new Point(pu.position.x * CELL_SIZE + CELL_SIZE/2, pu.position.y * CELL_SIZE + CELL_SIZE/2), CELL_SIZE,
                        new float[]{0f, 0.7f, 1f}, new Color[]{new Color(160, 255, 255, 220), new Color(40, 200, 255, 180), new Color(0, 80, 140, 0)}));
                g2d.fillOval(pu.position.x * CELL_SIZE + 4, pu.position.y * CELL_SIZE + 4, CELL_SIZE - 8, CELL_SIZE - 8);
                g2d.setPaint(null);
                g2d.setColor(new Color(255, 255, 255, 180));
                g2d.fillOval(pu.position.x * CELL_SIZE + 10, pu.position.y * CELL_SIZE + 8, 6, 6);
            }

            // Snowflakes effect when frozen
            if(freezeActive) {
                g2d.setColor(new Color(255, 255, 255, 200));
                for(Point snowflake : snowflakes) {
                    g2d.fillOval(snowflake.x, snowflake.y, 4, 4);
                }
            }

            // HUD background panel
            g2d.setColor(new Color(0, 0, 0, 140));
            g2d.fillRoundRect(8, 8, 340, 50, 20, 20);
            g2d.setColor(new Color(255, 255, 255, 180));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawRoundRect(8, 8, 340, 50, 20, 20);

            // Score
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2d.drawString("Score: " + score + "   Foods: " + foodEaten + "   Level: " + level, 18, 29);

            if(level >= 12) {
                g2d.setColor(new Color(220, 240, 255, 230));
                g2d.setFont(new Font("SansSerif", Font.PLAIN, 12));
                g2d.drawString("Legend: Blue orb = Freeze apple 5s", 18, 45);
            }

            // Freeze countdown timer
            if(freezeActive && freezeEndTime > 0) {
                long timeRemaining = (freezeEndTime - System.currentTimeMillis()) / 1000 + 1;
                if(timeRemaining < 0) timeRemaining = 0;
                g2d.setColor(new Color(120, 220, 255));
                g2d.setFont(new Font("SansSerif", Font.BOLD, 26));
                g2d.drawString("Freeze: " + timeRemaining + "s", getWidth() - 190, 38);
            }

            // Countdown to start
            if(!gameStarted && startCountdown > 0) {
                g2d.setFont(new Font("SansSerif", Font.BOLD, 80));
                FontMetrics fm = g2d.getFontMetrics();
                String countdownText = String.valueOf(startCountdown);
                int x = (getWidth() - fm.stringWidth(countdownText)) / 2;
                int y = (getHeight() - fm.getAscent()) / 2 + fm.getAscent();
                g2d.setColor(new Color(0, 0, 0, 200));
                g2d.drawString(countdownText, x + 4, y + 4);
                g2d.setColor(new Color(100, 255, 100));
                g2d.drawString(countdownText, x, y);
            }

            // Level Up Message
            if(levelUpMessage != null) {
                g2d.setFont(new Font("SansSerif", Font.BOLD, 48));
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(levelUpMessage)) / 2;
                int y = getHeight() / 2;
                g2d.setColor(new Color(0, 0, 0, 180));
                g2d.drawString(levelUpMessage, x + 4, y + 4);
                g2d.setColor(new Color(255, 220, 70));
                g2d.drawString(levelUpMessage, x, y);
            }

            // Game Over
            if(gameOver) {
                g2d.setFont(new Font("SansSerif", Font.BOLD, 48));
                FontMetrics fm = g2d.getFontMetrics();
                String msg1 = "Game Over!";
                String msg2 = "Score: " + score;
                String msg3 = "Press R to Restart";
                int x1 = (getWidth() - fm.stringWidth(msg1)) / 2;
                int x2 = (getWidth() - fm.stringWidth(msg2)) / 2;
                int x3 = (getWidth() - fm.stringWidth(msg3)) / 2;
                int y = getHeight() / 2 - 50;
                g2d.setColor(new Color(0, 0, 0, 190));
                g2d.drawString(msg1, x1 + 4, y + 4);
                g2d.drawString(msg2, x2 + 4, y + 54);
                g2d.drawString(msg3, x3 + 4, y + 104);
                g2d.setColor(new Color(255, 80, 80));
                g2d.drawString(msg1, x1, y);
                g2d.setColor(new Color(255, 160, 160));
                g2d.drawString(msg2, x2, y + 50);
                g2d.drawString(msg3, x3, y + 100);
            }
        }
    }
}