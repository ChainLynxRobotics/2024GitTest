import java.awt.Color;
import java.awt.Graphics;

import fromics.PointCollider;

public class Bullet extends PointCollider {
	public static final int LIFETIME = 150;
	
	private final double ang;
	private final double spd;
	private final double maxX;
	private final double maxY;
	private int life;

	public Bullet(double x, double y,  double ang, double spd, int maxX, int maxY) {
		super(x, y);
		this.ang = ang;
		this.spd = spd;
		this.maxY = maxY;
		this.maxX = maxX;
		life = LIFETIME;
	}
	
	public boolean isValid() {
		return life >= 0;
	}
	
	@Override
	public boolean update() {
		add(Math.cos(ang) * spd, Math.sin(ang) * spd);
		if(Math.abs(X()) + 10 > maxX) {
			vals[0] *= -0.99;
		}
		if(Math.abs(Y()) + 20 > maxY) {
			vals[1] *= -0.99;
		}
		life--;
		if(life < 0) return true;
		return false;
	}

	@Override
	protected void draw(Graphics g, double xOff, double yOff, double angOff) {
		g.setColor(Color.WHITE);
		int size = 3;
		double[] relativeY = {0, 0};
		double[] relativeX = {-1, 1};
		double[] xLocs = new double[relativeX.length];
		double[] yLocs = new double[relativeX.length];
		for(int i = 0; i < relativeX.length; i++) {
			xLocs[i] = ((Math.cos(-ang - angOff)*relativeX[i] + Math.sin(-ang - angOff)*relativeY[i]) * size + X() + xOff);
			yLocs[i] = ((Math.sin(ang + angOff)*relativeX[i] + Math.cos(ang + angOff)*relativeY[i]) * size + Y() + yOff);
		}
		g.drawLine((int)xLocs[0], (int)yLocs[0], (int)xLocs[1], (int)yLocs[1]);
	}

}
