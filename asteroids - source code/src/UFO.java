import java.awt.Color;
import java.awt.Graphics;

import fromics.Point;

public class UFO extends Enemy {
	public static final double[] X_POINTS = {-4, -2, 2, 4, 2, 1.732, 1, 0, -1, -1.732, -2};
	public static final double[] Y_POINTS = {0, -2, -2, 0, 1, 2, 2.732, 3, 2.732, 2, 1};
	public static final int[] POINTS = {300, 200};
	public static final int DEF_SHOT_TIME = 100;
	public static final double BULLET_SPEED = 10;
	public static final int MAX_SHIFT = 250;
	public static final int MIN_SHIFT = 10;
	public static final double[] SHIFT_CHANCES = {0.02, 0.01};
	public static final int UFO_TYPE_POINT_BOUNDARY = 40000;
	public static final int[] TYPE_SIZES = {3, 5};
	public static final double[] TYPE_XSPEEDS = {4, 3};
	public static final double PORTHOLE_LOC = 2.1;
	public static final int PORTHOLES = 5;
	public static final double PORTHOLE_RAD = 0.27;
	public static final double PORTHOLE_HEIGHT = 0.75;
	
	private int shotTimer;
	private final int shotTime;
	private final int type;
	private Player ship;
	private double xSpd;
	private double shiftSpd;
	private int shiftTimer;

	protected UFO(double x, double y, int points, Player player, GameScreen.UFOInfo info) {
		super(x, y);
		shiftTimer = 0;
		shotTime = (int)(DEF_SHOT_TIME / info.fireRateMod);
		shotTimer = shotTime / 2;
		if(Math.random() < info.smallUFOChance ) {
			type = 0;
		} else {
			type = 1;
		}
		xSpd = TYPE_XSPEEDS[type];
		size = TYPE_SIZES[type];
		this.ship = player;
		init(X_POINTS, Y_POINTS, size);
	}
	
	public boolean update() {
		move();
		super.update();
		if(shotTimer == 0) {
			shotTimer = shotTime;
			double ang = 0;
			if(type == 0) {
				ang = target(ship, ship.getVelocity());
			} else {
				ang = Math.random() * 2 * Math.PI;
			}
			Point speedVec = new Point(Math.cos(ang) * BULLET_SPEED, Math.sin(ang) * BULLET_SPEED);
			parent.link(new EnemyBullet(X(), Y(), speedVec));
		} else {
			shotTimer--;
		}
		return false;
	}
	
	private void move() {
		if(shiftTimer == 0) {
			if(Math.random() < SHIFT_CHANCES[type]) {
				shiftTimer = (int)(Math.random() * (MAX_SHIFT - MIN_SHIFT) + MIN_SHIFT);
				shiftSpd = Math.random() < 0.5 ? xSpd : -xSpd;
			}
			add(xSpd, 0);
		} else {
			shiftTimer--;
			add(xSpd, shiftSpd);
		}
	}
	
	private double target(Point target, Point targetSpd) {
		Point maxBounds = getMaxBounds().add(Player.LOOP_OFF, Player.LOOP_OFF).mult(2);
		Double shotAng = 0.0;
		double hitTime = -1;
		for(int i = -1; i <= 1; i++) {
			for(int j = -1; j <= 1; j++) {
				//target location accounting for shooting through the edge of the screen
				Point newTarget = target.copy().add(i * maxBounds.X(), j * maxBounds.Y());
				//angle to the player from this UFO
				double targetDir = Math.atan2(newTarget.Y() - this.Y(), newTarget.X() - this.X());
				//total speed of the player
				double pSpeed = targetSpd.mag();
				//direction of player movement
				double pDir = targetSpd.ang();
				//direction to shoot in
				double newShotAng = Math.asin(pSpeed * Math.sin(pDir - targetDir + Math.PI + (Math.PI * Math.signum(targetSpd.X()) + 1) / 2) / BULLET_SPEED) + targetDir;
				//time it would take to hit the player
				double newHitTime = (this.X() - newTarget.X()) / (targetSpd.X() - (Math.cos(newShotAng) * BULLET_SPEED));
				if(!Double.isNaN(newShotAng) && (newHitTime < hitTime || hitTime < 0)) {
					hitTime = newHitTime;
					shotAng = newShotAng;
				}
			}
		}
		return shotAng.isNaN() ? 0 : shotAng;
	}

	@Override
	public int getPoints() {
		return POINTS[type];
	}

	@Override
	public Enemy[] destroy() {
		return new Enemy[0];
	}
	
	public void draw(Graphics g, double xOff, double yOff, double angOff) {
		g.setColor(Color.WHITE);
		drawCollider(g, xOff, yOff, 0);
		g.drawLine((int)(X_POINTS[0] * size + X() + xOff), (int)(Y_POINTS[0] * size + Y() + yOff), (int)(X_POINTS[3] * size + X() + xOff), (int)(Y_POINTS[3] * size + Y() + yOff));
		g.drawLine((int)(X_POINTS[4] * size + X() + xOff), (int)(-Y_POINTS[4] * size + Y() + yOff), (int)(X_POINTS[X_POINTS.length - 1] * size + X() + xOff), (int)(-Y_POINTS[Y_POINTS.length - 1] * size + Y() + yOff));
		for(double i = -PORTHOLE_LOC * size; i <= PORTHOLE_LOC * size; i += size * PORTHOLE_LOC * 2.0 / (PORTHOLES - 1)) {
			g.drawOval((int)(i - size * PORTHOLE_RAD + X() + xOff), (int)((size * (PORTHOLE_HEIGHT - PORTHOLE_RAD)) + Y() + yOff), (int)(size * PORTHOLE_RAD * 2), (int)(size * PORTHOLE_RAD * 2));
		}
	}
	
	public static UFO newUFO(int sWidth, int sHeight, int points, Player player, GameScreen.UFOInfo info) {
		Point loc = GameScreen.SPAWN_LOC_FUNCTION.apply(new Point(sWidth, sHeight));
		return new UFO(loc.X(), loc.Y(), points, player, info);
	}
	
	private class EnemyBullet extends Enemy {
		public static final int BULLET_SIZE = 1;
		public static final int MAX_LOOPS = 2;
		
		Point vec;
		int loops;

		protected EnemyBullet(double x, double y, Point vel) {
			super(x, y);
			this.vec = vel;
			loops = 0;
			double[] xVals = new double[2];
			xVals[0] = 0;
			xVals[1] = vec.X();
			double[] yVals = new double[2];
			yVals[0] = 0;
			yVals[1] = vec.Y();
			init(xVals, yVals, BULLET_SIZE);
		}
		
		public boolean update() {
			add(vec);
			Point maxVals = parent.getMaxBounds();
			int maxX = (int)maxVals.X() + 50;
			int maxY = (int)maxVals.Y() + 60;
			if(Math.abs(X()) > maxX) {
				vals[0] *= -0.99;
				loops++;
				while(Math.abs(X()) > maxX) {
					vals[0] *= 0.99;
				}
			}
			if(Math.abs(Y()) > maxY) {
				vals[1] *= -0.99;
				loops++;
				while(Math.abs(Y()) > maxY) {
					vals[1] *= 0.99;
				}
			}
			return loops > MAX_LOOPS;
		}

		@Override
		public Enemy[] destroy() {
			return new Enemy[0];
		}

		@Override
		public int getPoints() {
			return 1000;
		}
		
		@Override
		public void draw(Graphics g, double xOff, double yOff, double angOff) {
			g.setColor(Color.WHITE);
			g.drawLine((int)(X() + xOff), (int)(Y() + yOff), (int)(X() + xOff - BULLET_SIZE * vec.X()), (int)(Y() + yOff - BULLET_SIZE * vec.Y()));
		}
		
	}

}