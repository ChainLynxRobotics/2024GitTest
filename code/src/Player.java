import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import fromics.Linkable;
import fromics.Point;
import fromics.PolygonCollider;

public class Player extends PolygonCollider {
	public static final double THRUST_RATE = 0.1;
	public static final double ROT_RATE = 0.004;
	public static final double FRICTION = 0.997;
	public static final double ROT_FRICTION = 0.94;
	public static final int SHOT_COOLDOWN = 5;
	public static final int SIZE = 10;
	public static final double BULLET_SPEED = 10;
	public static final int MAX_BULLETS = 4;
	public static final double[] X_VALS = {-1, -1, 1};
	public static final double[] Y_VALS = {-0.75, 0.75, 0};
	public static final int RESPAWN_TIME = 200;
	public static final int FADE_IN_TIME = 200;
	public static final double MAX_SPEED_SQRD = 36;
	public static final int LOOP_OFF = 60;
	public static final double PARTICLE_CHANCE = 0.75;
	public static final double PARTICLE_SPD_VARIATION = 0.5;
	public static final double PARTICLE_LOC_VARIATION = 5;
	public static final double PARTICLE_KICKBACK = 20;
	public static final boolean HOLD_SHOOT_AT_START = false;
	public static final boolean SPACE_BLAST = false;
	public static final double DEFAULT_SHOT_RECOIL = 0.1;
	
	@FunctionalInterface
	public static interface PlayerModFunc {
		public String[] modify(Player p);
	}
	public static final PlayerModFunc[] WAVE_MODS = {(Player p) -> {
			String modName;
			if(!p.aimingLaser) {
				modName = "Aiming Laser";
				p.aimingLaser = true;
			} else {
				modName = "Aiming Laser Length +50";
				p.aimingLaserLength += 10;
			}
			String[] modText = new String[1];
			modText[0] = modName;
			return modText;
		}, (Player p) -> {
			String modName = "Space Blast!";
			String modDesc = "No Thrust, All Recoil, No Bullet Limit";
			String modDescAdendum = "Also, No Max Speed";
			p.spaceBlast = true;
			p.shotRecoil += 1;
			String[] modText = new String[3];
			modText[0] = modName;
			modText[1] = modDesc;
			modText[2] = modDescAdendum;
			return modText;
		}
	};
	public static final int[] PLAYER_MOD_CHANCES = {10, 1};
	
	private final List<Integer> playerModChoosingList;
	
	private double rotSpd;
	private boolean spaceBlast;
	private double shotRecoil;
	private boolean aimingLaser;
	private double aimingLaserLength;
	private Point spdVec;
	private int shotTimer;
	private int respawnTimer;
	private Set<Integer> keys;
	private boolean newBullet;
	private boolean holdShoot;
	private boolean holding;
	private static int fadeTimer;
	public static boolean uiFade;
	
	public Player(Set<Integer> keys) {
		super(0, 0);
		aimingLaser = false;
		aimingLaserLength = 50;
		holdShoot = HOLD_SHOOT_AT_START;
		fadeTimer = FADE_IN_TIME;
		shotRecoil = DEFAULT_SHOT_RECOIL;
		spdVec = new Point(0, 0);
		rotSpd = 0;
		shotTimer = 0;
		newBullet = false;
		this.keys = keys;
		uiFade = true;
		playerModChoosingList = new ArrayList<>();
		for(int i = 0; i < PLAYER_MOD_CHANCES.length; i++) {
			for(int j = 0; j < PLAYER_MOD_CHANCES[i]; j++) {
				playerModChoosingList.add(i);
			}
		}
		init(X_VALS, Y_VALS, SIZE);
	}
	
	public Point getVelocity() {
		return spdVec.copy();
	}
	
	public String[] modify() {
		return WAVE_MODS[playerModChoosingList.get((int)(Math.random() * playerModChoosingList.size()))].modify(this);
	}
	
	public void kill() {
		respawnTimer = RESPAWN_TIME;
		spdVec = new Point(0, 0);
		setX(0);
		setY(0);
	}
	
	public boolean isAlive() {
		return respawnTimer == 0 && fadeTimer == 0;
	}
	
	public static float getFade() {
		return ((float)FADE_IN_TIME - (float)fadeTimer) / (float)FADE_IN_TIME;
	}
	
	public boolean update() {
		if(respawnTimer != 0) {
			respawnTimer--;
			fadeTimer = FADE_IN_TIME;
			return false;
		}
		if(fadeTimer != 0) {
			fadeTimer--;
		} else {
			uiFade = false;
		}
		Point maxVals = getMaxBounds();
		int maxX = (int)maxVals.X() + LOOP_OFF;
		int maxY = (int)maxVals.Y() + LOOP_OFF;
		add(spdVec.X(), spdVec.Y());
		if(Math.abs(X()) > maxX) {
			vals[0] *= -0.99;
		}
		if(Math.abs(Y()) > maxY) {
			vals[1] *= -0.99;
		}
		ang += rotSpd;
		if((keys.contains(KeyEvent.VK_UP) || keys.contains(KeyEvent.VK_W)) && !spaceBlast) {
			Point accel = new Point(Math.cos(-ang) * THRUST_RATE, Math.sin(ang) * THRUST_RATE);
			if(Math.random() <= PARTICLE_CHANCE) {
				double spdVariationAngle = Math.random() * 2 * Math.PI;
				double spdVariationMag = Math.random() * PARTICLE_SPD_VARIATION;
				double locVariationAngle = Math.random() * 2 * Math.PI;
				double locVariationMag = Math.random() * PARTICLE_LOC_VARIATION;
				link(new ThrustParticle(X() + Math.cos(locVariationAngle) * locVariationMag, Y() + Math.sin(locVariationAngle) * locVariationMag, accel.copy().mult(-spdVariationMag * PARTICLE_KICKBACK).add(spdVariationMag * Math.cos(spdVariationAngle), spdVariationMag * Math.sin(spdVariationAngle)).add(spdVec)));
			}
			Point newSpd = spdVec.copy().add(accel);
			if(newSpd.sMag() < MAX_SPEED_SQRD) {
				spdVec = newSpd;
			}
		} 
		if(keys.contains(KeyEvent.VK_LEFT) || keys.contains(KeyEvent.VK_A)) {
			rotSpd -= ROT_RATE;
		}
		if(keys.contains(KeyEvent.VK_RIGHT) || keys.contains(KeyEvent.VK_D)) {
			rotSpd += ROT_RATE;
		}
		if(keys.contains(KeyEvent.VK_SPACE)) {
			if(shotTimer <= 0 && (holdShoot || !holding)) {
				newBullet = true;
				spdVec.add(-Math.cos(ang) * shotRecoil, -Math.sin(ang) * shotRecoil);
				shotTimer = SHOT_COOLDOWN;
			}
			holding = true;
		} else {
			holding = false;
		}
		spdVec.mult(FRICTION);
		rotSpd *= ROT_FRICTION;
		shotTimer = Math.max(shotTimer - 1, 0);
		return false;
	}
	
	public boolean spaceBlast() {
		return spaceBlast;
	}
	
	@Override
	protected void draw(Graphics g, double xOff, double yOff, double angOff) {
		if(respawnTimer == 0) {
			g.setColor(Color.BLACK);
			fillPoints(g, xOff + X(), yOff + Y(), angOff + ang, SIZE, X_VALS, Y_VALS);
			float fade = getFade();
			g.setColor(new Color(fade, fade, fade));
			drawPlayer(g, xOff + X(), yOff + Y(), angOff + ang);
			if(aimingLaser) {
				g.drawLine((int)(X() + xOff), (int)(Y() + yOff), (int)(X() + Math.cos(ang) * aimingLaserLength + xOff), (int)(Y() + Math.sin(ang) * aimingLaserLength + yOff));
			}
		}
	}
	
	public static void drawPlayer(Graphics g, double x, double y, double ang) {
		if(uiFade) {
			float fade = getFade();
			g.setColor(new Color(fade, fade, fade));
		}
		double size = SIZE;
		double arcSize = size * 1.8;
		double[] relativeY = {-0.75, 0.75, 0, 0};
		double[] relativeX = {-1, -1, 1, -1.45};
		double[] xLocs = new double[relativeX.length];
		double[] yLocs = new double[relativeX.length];
		
		
		for(int i = 0; i < relativeX.length; i++) {
			xLocs[i] = ((Math.cos(-ang)*relativeX[i] + Math.sin(-ang)*relativeY[i]) * size + x);
			yLocs[i] = ((Math.sin(ang)*relativeX[i] + Math.cos(ang)*relativeY[i]) * size + y);
		}
		g.drawLine((int)xLocs[0], (int)yLocs[0], (int)xLocs[2], (int)yLocs[2]);
		g.drawLine((int)xLocs[1], (int)yLocs[1], (int)xLocs[2], (int)yLocs[2]);
		g.drawArc((int)(xLocs[3] - arcSize / 2), (int)(yLocs[3] - arcSize / 2), 
				(int)arcSize, (int)arcSize, (int)((Math.toDegrees(-ang) + 55) % 360), -100);
	}

	@Override
	public double getAbsAng() {
		return parent.getAbsAng();
	}

	public Bullet getBullet() {
		int maxX = (int)getMaxBounds().X() + 40;
		int maxY = (int)getMaxBounds().Y() + 60;
		return new Bullet(X(), Y(), ang, Player.BULLET_SPEED, maxX, maxY);
	}

	public boolean hasBullet() {
		if(newBullet) {
			newBullet = false;
			return true;
		}
		return false;
	}
	
	private class ThrustParticle extends Linkable {
		public static final double SPD_MULT = 0.7;
		public static final int LIFETIME = 35;
		
		private final Point vec;
		private int life;

		public ThrustParticle(double x, double y, Point vec) {
			super(x, y);
			this.vec = vec.copy().mult(SPD_MULT);
			life = LIFETIME;
		}
		
		public boolean update() {
			add(vec);
			life--;
			return life <= 0;
		}

		@Override
		protected void draw(Graphics g, double xOff, double yOff, double angOff) {
			float val = 1f - (float)(LIFETIME - life) / LIFETIME;
			g.setColor(new Color(val, val, val));
			g.drawRect((int)(X() + xOff - parent.X()), (int)(Y() + yOff - parent.Y()), 1, 1);
		}
		
	}
}
