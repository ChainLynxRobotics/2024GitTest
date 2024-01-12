import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import effects.ParticleEffect;
import fromics.Background;
import fromics.Frindow;
import fromics.Group;
import fromics.Linkable;
import fromics.Point;

public class GameScreen extends Background {
	public static final int START_LIVES = 3;
	public static final int INITIAL_WAVE_ASTEROIDS = 3;
	public static final int ATTACK_DELAY = 300;
	public static final int FADE_OUT_TIME = 100;
	public static final int WAVE_TRANSITION_TIME = 400;
	public static final int WAVE_TEXT_FADE_TIME = 40;
	public static final int FONT_SIZE = 20;
	public static final Function<Point, Point> SPAWN_LOC_FUNCTION = (Point bounds) -> {
		int side = (int)Math.floor(4 * Math.random());
		int x = side % 2 == 0 ? (int)((side - 1) * bounds.X()) : 
			(int)(Math.random() * bounds.X() - (bounds.X() / 2));
		int y = side % 2 == 1 ? (int)((side - 2) * bounds.Y()): 
			(int)(Math.random() * bounds.Y() - (bounds.Y() / 2));
		return new Point(x, y);
	};
	@FunctionalInterface
	public static interface WaveModFunc {
		public String[] modify(GameScreen game);
	}
	public static final WaveModFunc[] WAVE_MODS = {(GameScreen game) -> {
			String modName = "+1 Asteroid Per Attack";
			game.waveAsteroids++;
			String[] modText = new String[1];
			modText[0] = modName;
			return modText;
		}, (GameScreen game) -> {
			double waveSplitProportion = 1.5 / (game.attacks + 1);
			String modName = "+1 Attacks Per Wave";
			String modDesc = (int)100 * (waveSplitProportion) + "% Asteroids Per Attack";
			game.waveAsteroids = (int)Math.ceil(game.waveAsteroids * waveSplitProportion);
			game.attacks++;
			String[] modText = new String[2];
			modText[0] = modName;
			modText[1] = modDesc;
			return modText;
		}, (GameScreen game) -> {
			String modName = "UFO Fire Rate x1.5";
			game.UFOStats.fireRateMod *= 1.5;
			String[] modText = new String[1];
			modText[0] = modName;
			return modText;
		}, (GameScreen game) -> {
			String modName = "Small UFO Chance +0.2";
			game.UFOStats.smallUFOChance += 0.2;
			String[] modText = new String[1];
			modText[0] = modName;
			return modText;
		}
	};
	public static final int[] WAVE_MOD_CHANCES = {8, 1, 3, 5};
	
	private final List<Integer> waveModChoosingList;
	
	private Group<Enemy> enemies;
	private Group<Bullet> bullets;
	private Player ship;
	private UFOInfo UFOStats;
	private int wave;
	private int curAttack;
	private int attacks;
	private int attackTimer;
	private int waveAsteroids;
	private int waveTransitionTimer;
	private String[] waveTransitionText;
	private String[] playerTransitionText;
	private boolean waveTransitioning;
	private int points;
	private int lives;
	private int fadeOutTimer;
	
	@SuppressWarnings("unchecked")
	public GameScreen(Frindow observer, Player ship, List<Linkable> initalObjs) {
		super(observer);
		lives = START_LIVES;
		waveAsteroids = INITIAL_WAVE_ASTEROIDS;
		attacks = 1;
		bullets = new Group<>(this);
		link(ship);
		this.ship = ship;
		UFOStats = new UFOInfo();
		curAttack = 1;
		fadeOutTimer = 0;
		waveTransitioning = false;
		setX(observer.getWidth() / 2);
		setY(observer.getHeight() / 2);
		points = 0;
		for(Linkable l : initalObjs) {
			link(l);
			if(l.getClass().isAssignableFrom(Group.class)) {
				enemies = (Group<Enemy>)l;
			}
		}
		if(enemies == null) {
			enemies = new Group<>(this);
		}
		waveModChoosingList = new ArrayList<>();
		for(int i = 0; i < WAVE_MOD_CHANCES.length; i++) {
			for(int j = 0; j < WAVE_MOD_CHANCES[i]; j++) {
				waveModChoosingList.add(i);
			}
		}
	}
	
	public boolean nextScreen() {
		return fadeOutTimer >= FADE_OUT_TIME;
	}
	
	public int score() {
		return points;
	}
	
	private void modify() {
		waveTransitionText = WAVE_MODS[waveModChoosingList.get((int)(Math.random() * waveModChoosingList.size()))].modify(this);
		playerTransitionText = ship.modify();
	}
	
	private void spawnAttack() {
		int width = observer.getWidth();
		int height = observer.getHeight();
		for(int i = 0; i < waveAsteroids; i++) {
			enemies.linkE(Asteroid.newAsteroid(width, height));
		}
		enemies.linkE(UFO.newUFO(width, height, points, ship, UFOStats));
	}
	
	@Override
	public boolean update() {
		if(lives <= 0) {
			fadeOutTimer++;
			return false;
		}
		if((bullets.numLinked() < Player.MAX_BULLETS || ship.spaceBlast()) && ship.hasBullet()) {
			bullets.linkE(ship.getBullet());
		}
		Iterator<Bullet> bCheck = bullets.getLinkedE().iterator();
		while(bCheck.hasNext()) {
			Bullet next = bCheck.next();
			if(!next.isValid()) {
				bCheck.remove();
			}
		}
		
		if(curAttack < attacks) {
			if(attackTimer == 0) {
				attackTimer = ATTACK_DELAY;
				spawnAttack();
				curAttack++;
			} else {
				attackTimer--;
			}
		}
		
		if(waveTransitioning) {
			waveTransitionTimer--;
			if(waveTransitionTimer == 0) {
				waveTransitioning = false;
				curAttack = 0;
			} else {
				return false;
			}
		}
		
		if(enemies.numLinked() <= 0 && curAttack == attacks) {
			wave++;
			waveTransitionTimer = WAVE_TRANSITION_TIME;
			waveTransitioning = true;
			modify();
			return false;
		}
		
		Iterator<Enemy> astit = enemies.iterator();
		List<Enemy> newAsteroids = new LinkedList<>();
		while(astit.hasNext()) {
			Enemy a = astit.next();
			Iterator<Bullet> blit = bullets.iterator();
			while(blit.hasNext()) {
				Bullet b = blit.next();
				Enemy[] childAsteroids = null;
				
				if(a.check(b)) {
					childAsteroids = a.destroy();
					int prevPoints = points;
					points += a.getPoints() + wave;
					if(points % 10000 < prevPoints % 10000) lives++;
					astit.remove();
					blit.remove();
					link(new ParticleEffect(a.X(), a.Y(), 50, 1, 200));
				}
				
				if(childAsteroids != null) {
					for(Enemy add : childAsteroids) {
						newAsteroids.add(add);
					}
				}
			}
			
			if(a.check(ship) && ship.isAlive()) {
				link(new ParticleEffect(ship.X(), ship.Y(), 250, 2, 300));
				lives--;
				ship.kill();
				if(lives <= 0) {
					unlink(ship);
				}
			}
		}
		for(Enemy a : newAsteroids) {
			enemies.linkE(a);
		}
		return false;
	}
	
	@Override
	protected void draw(Graphics g, double x, double y, double ang) {
		Font textFont = new Font("Cambria Math", Font.PLAIN, FONT_SIZE);
		g.setFont(textFont);
		String pointString = Integer.toString(points);
		pointString = "0".repeat(5 - pointString.length()) + pointString;
		if(Player.uiFade) {
			g.setColor(new Color(1f, 1f, 1f, Player.getFade()));
		} else {
			g.setColor(new Color(1f, 1f, 1f, (float)(FADE_OUT_TIME - fadeOutTimer) / (float)FADE_OUT_TIME));
		}
		g.drawString(pointString, 10, 20);
		
		for(int i = 0; i < lives; i++) {
			Player.drawPlayer(g, 20 * i + 17, 40, -Math.PI / 2);
		}
		
		for(int i = 0; i < Player.MAX_BULLETS - bullets.numLinked(); i++) {
			g.drawRect(8, 65 + 20 * i, 20, 10);
		}
		g.drawRect(3, 60, 30, Player.MAX_BULLETS * 20);
		
		if(waveTransitioning) {
			if(waveTransitionTimer > WAVE_TRANSITION_TIME / 2) {
				float textColor = Math.max(Math.min(Math.min((float)((WAVE_TRANSITION_TIME - waveTransitionTimer)) / (float)WAVE_TEXT_FADE_TIME,(float)(waveTransitionTimer - WAVE_TRANSITION_TIME / 2) / (float)(WAVE_TEXT_FADE_TIME)), 1f), 0f);
				g.setColor(new Color(textColor, textColor, textColor));
				for(int i = 0; i < waveTransitionText.length; i++) {
					String text = waveTransitionText[i];
					int sWidth = g.getFontMetrics(textFont).stringWidth(text);
					g.drawString(text, (int)(X() - (sWidth / 2)), (int)(Y() + 20 * i));
				}
			} else {
				float textColor = Math.max(Math.min(Math.min((float)waveTransitionTimer / (float)WAVE_TEXT_FADE_TIME,(float)(WAVE_TRANSITION_TIME / 2 - waveTransitionTimer) / (float)(WAVE_TEXT_FADE_TIME)), 1f), 0f);
				g.setColor(new Color(textColor, textColor, textColor));
				for(int i = 0; i < playerTransitionText.length; i++) {
					String text = playerTransitionText[i];
					int sWidth = g.getFontMetrics(textFont).stringWidth(text);
					g.drawString(text, (int)(X() - (sWidth / 2)), (int)(Y() + 20 * i));
				}
			}
		}
	}
	
	public static  class UFOInfo {
		double fireRateMod;
		double smallUFOChance;
		
		public UFOInfo() {
			fireRateMod = 1;
			smallUFOChance = 0;
		}
	}

}
