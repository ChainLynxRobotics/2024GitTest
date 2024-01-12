import java.awt.Color;
import java.awt.Graphics;
import java.util.Random;
import java.util.function.Supplier;

import fromics.Point;

public class Asteroid extends Enemy {
	public static final double[][] TYPE_X_VALS = {{-3.75, -3.75, -2, 2, 3.75, 3, 3.75, 2, 0, -2}};
	public static final double[][] TYPE_Y_VALS = {{2, -1.5, -3.2, -3.2, -1.5, 0, 2, 3.5, 2, 3.5}};
	public static final double[] DRAW_SIZES = {4, 9, 14};
	public static final int[] SPLIT_FACTORS = {0, 3, 3};
	public static final int SPLIT_SEPARATION = 5;
	private static final int points = 10;
	public static final Supplier<Double> SPD_SUPPLIER = () -> {
		double min = 0.2;
		double max = 2;
		double spd = Math.random() * (max - min) + min;
		return spd;
	};
	private Random r;
	private int type;
	private int sizeCat;
	private Point velocity;
	private double rotSpeed;
	
	@Override
	public boolean update() {
		ang += rotSpeed;
		add(velocity);
		super.update();
		return false;
	}
	
	public Asteroid(double x, double y, Point velocity) {
		this(x, y, DRAW_SIZES.length - 1, velocity);
	}
	
	private Asteroid(double x, double y, int size, Point velocity) {
		super(x, y);
		r = new Random();
		rotSpeed = (r.nextDouble() - 0.5) / 15;
		type = r.nextInt(TYPE_X_VALS.length);
		sizeCat = size;
		this.size = (int)DRAW_SIZES[sizeCat];
		this.velocity = velocity;
		init(TYPE_X_VALS[type], TYPE_Y_VALS[type], DRAW_SIZES[sizeCat]);
	}

	@Override
	protected void draw(Graphics g, double xOff, double yOff, double angOff) {
//		System.out.println("(" + getAbsX() + "," + getAbsY() + ")");
//		System.out.println(getMaxBounds());
		g.setColor(Color.WHITE);
//		g.drawOval((int)getAbsX(), (int)getAbsY(), 5, 5);
		drawCollider(g, xOff, yOff, angOff + ang);
	}

	public Asteroid[] destroy() {
		Asteroid[] newAsteroids = new Asteroid[SPLIT_FACTORS[sizeCat]];
		for(int i = 0; i < SPLIT_FACTORS[sizeCat]; i++) {
			double spd = SPD_SUPPLIER.get();
			double dir = Math.random() * 2 * Math.PI;
			newAsteroids[i] = (new Asteroid(X() + r.nextDouble(-1, 1), Y() + r.nextDouble(-1, 1), 
					sizeCat - 1, velocity.copy().add(Math.cos(dir) * spd, Math.sin(dir) * spd)));
			newAsteroids[i].parent = this.parent;
			for(int j = 0; j < SPLIT_SEPARATION; j++) {
				newAsteroids[i].update();
			}
		}
		return newAsteroids;
	}
	
	public static Asteroid newAsteroid(int sWidth, int sHeight) {
		Point loc = GameScreen.SPAWN_LOC_FUNCTION.apply(new Point(sWidth, sHeight));
		double dir = Math.random() * 2 * Math.PI;
		double spd = Asteroid.SPD_SUPPLIER.get();
		Point aVelocity = new Point(Math.cos(dir) * spd, Math.sin(dir) * spd);
		return new Asteroid(loc.X(), loc.Y(), aVelocity);
	}
	
	public int getPoints() {
		return points;
	}
}
