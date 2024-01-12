import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.util.Set;

import effects.ColoredStarEffect;
import fromics.Background;
import fromics.Frindow;
import fromics.Group;
import fromics.Linkable;
import fromics.Point;

public class TitleScreen extends Background {
	public static final int MESSAGE_TIME = 176;
	public static final int COIN_TIME = 90;
	public static final int FADE_IN_TIME = 100;
	
	private  Group<Enemy> enemies;
	private int messageTimer;
	private int coinTimer;
	private boolean coinFalling;
	private Coin c;
	private int fadeInTimer;
	private Set<Integer> keyCodes;
	
	public TitleScreen(Frindow observer, Linkable previous, Set<Integer> keyCodes) {
		super(observer);
		this.keyCodes = keyCodes;
		coinFalling = false;
		coinTimer = COIN_TIME;
		messageTimer = MESSAGE_TIME;
		fadeInTimer = FADE_IN_TIME;
		if(previous != null) {
			for(Linkable l : previous.getLinked()) {
				if(l.getClass().isAssignableFrom(ColoredStarEffect.class)) {
					link(l);
				}
			}
			fadeInTimer = 0;
		} else  {
			link(new ColoredStarEffect(observer.getWidth(), observer.getHeight(), 42, 3200, 42, 1, 1500));
		}
		enemies = new Group<Enemy>(this);
		int width = observer.getWidth();
		int height = observer.getHeight();
		for(int i = 0; i < GameScreen.INITIAL_WAVE_ASTEROIDS; i++) {
			enemies.linkE(Asteroid.newAsteroid(width, height));
		}
		for(int i = 0; i < 1; i++) {
			enemies.linkE(UFO.newUFO(width, height, coinTimer, null, new GameScreen.UFOInfo()));
		}
		c = new Coin(0, 0);
		link(c);
	}
	
	public void startTransition() {
		if(!coinFalling) {
			coinFalling = true;
		}
	}
	
	@Override
	public boolean update() {
		if(fadeInTimer < FADE_IN_TIME) {
			fadeInTimer++;
		}
		messageTimer = (messageTimer + 1) % MESSAGE_TIME;
		if(coinFalling && coinTimer > 0) {
			coinTimer--;
		}
		if(keyCodes.contains(KeyEvent.VK_SPACE)) {
			startTransition();
		} 
		return false;
	}
	
	public boolean nextScreen() {
		return coinTimer <= 0 && coinFalling;
		
	}
	
	@Override
	public void draw(Graphics g, double xOff, double yOff, double angOff) {
		g.setColor(new Color(1f, 1f, 1f, Math.min(1f - (float)(FADE_IN_TIME - fadeInTimer) / (float)FADE_IN_TIME,1f - (float)(COIN_TIME - coinTimer) / (float)COIN_TIME)));
		Font titleFont = new Font("Cambria Math", Font.PLAIN, 100);
		g.setFont(titleFont);
		g.drawString("ASTEROIDS", observer.getWidth() / 2 - 250, observer.getHeight() / 2 - 100);
		if(messageTimer < MESSAGE_TIME / 2) {
			g.setFont(titleFont.deriveFont(25f));
			g.drawString("insert coin to start", observer.getWidth() / 2 - 100, observer.getHeight() / 2 + 100);
		}
	}
	
	private class Coin extends Linkable {
		public static final int DETAIL = 10;
		public static final int SIZE = 50;
		public static final int CENTER_WIDTH = 20;
		public static final int CENTER_HEIGHT = 30;
		public static final double GRAVITY = 0.2;
		public static final double INITIAL_VELOCITY = -5;
		final Point[] coinCorners;
		final Point[] centerCorners;
		private double velocity;
		
		public boolean update() {
			if(coinFalling) {
				add(0, velocity);
				velocity += GRAVITY;
			}
			return getAbsY() + Y() < 0;
		}

		public Coin(double x, double y) {
			super(x, y);
			velocity = INITIAL_VELOCITY;
			coinCorners = new Point[DETAIL];
			centerCorners = new Point[DETAIL];
			for(double i = 0, j = 0; i < DETAIL; i++, j += 2 * Math.PI / DETAIL) {
				coinCorners[(int)i] = new Point(Math.cos(j) * SIZE, Math.sin(j) * SIZE);
				centerCorners[(int)i] = new Point(Math.cos(j - Math.PI / DETAIL) * CENTER_WIDTH, Math.sin(j - Math.PI / DETAIL) * CENTER_WIDTH + (Math.signum(Math.sin(j - Math.PI / DETAIL)) * (CENTER_HEIGHT - CENTER_WIDTH)));
			}
		}

		@Override
		protected void draw(Graphics g, double xOff, double yOff, double angOff) {
			g.setColor(new Color(1f, 1f, 1f, 1f - (float)(FADE_IN_TIME - fadeInTimer) / (float)FADE_IN_TIME));
			Point[] smallCenter = new Point[DETAIL];
			for(int i = 0; i < DETAIL; i++) smallCenter[i] = centerCorners[i].copy().mult(0.85);
			Point[] smallBorder = new Point[DETAIL];
			for(int i = 0; i < DETAIL; i++) smallBorder[i] = coinCorners[i].copy().mult(0.95);
			g.setColor(Color.black);
			fillPoints(g, xOff + X(), yOff + Y(), angOff, 1, coinCorners);
			g.setColor(Color.WHITE);
			drawPoints(g, xOff + X(), yOff + Y(), angOff, 1, coinCorners);
			drawPoints(g, xOff + X(), yOff + Y(), angOff, 1, centerCorners);
			drawPoints(g, xOff + X(), yOff + Y(), angOff, 1, smallBorder);
			drawPoints(g, xOff + X(), yOff + Y(), angOff, 1, smallCenter);
		}
		
	}

}
