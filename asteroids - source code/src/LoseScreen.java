import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import effects.ParticleEffect;
import fromics.Background;
import fromics.Frindow;
import fromics.Group;
import fromics.Linkable;

public class LoseScreen extends Background {
	public static final int FADE_TIME = 100;
	public static final int ENEMY_DESTROY_COOLDOWN = 5;
	public static final String LEADERBOARD_FILE_PATH = "C:\\Users\\" + System.getProperty("user.name") + "\\AppData\\Local\\Asteroids\\";
	public static final String LEADERBOARD_FILE_NAME = "leaderboard";
	
	private Set<Integer> prevKeysPressed;
	private Set<Integer> keysPressed;
	private List<Score> leaderboard;
	private String playerName;
	private boolean gettingName;
	private int fadeTimer;
	private int enemyDestroyClock;
	private  File leaderboardFile;
	
	private final int playerScore;
	
	public LoseScreen(Frindow observer, Set<Integer> keys, List<Linkable> initialObjs, int playerScore) {
		super(observer);
		File leaderboardPath = new File(LEADERBOARD_FILE_PATH);
		if(!leaderboardPath.exists()) {
			leaderboardPath.mkdirs();
		}
		leaderboardFile = new File(LEADERBOARD_FILE_PATH + LEADERBOARD_FILE_NAME);
		if(!leaderboardFile.isFile()) {
			try {
				leaderboardFile.createNewFile();
			} catch (IOException e) {e.printStackTrace();}
		}
		leaderboard = new LinkedList<>();
		keysPressed = keys;
		gettingName = true;
		fadeTimer = FADE_TIME;
		enemyDestroyClock = 0;
		loadLeaderboard();
		prevKeysPressed = new TreeSet<>();
		this.playerScore = playerScore;
		playerName = "";
		for(Linkable l : initialObjs) {
			link(l);
		}
	}
	
	@SuppressWarnings("unchecked")
	public boolean update() {
		if(!gettingName) return false;
		Set<Integer> newKeysPressed = new TreeSet<>();
		enemyDestroyClock++;
		if(fadeTimer > 0) fadeTimer--;
		if(enemyDestroyClock == ENEMY_DESTROY_COOLDOWN) {
			Group<Enemy> enemies = null;
			for(Linkable l : linked) {
				if(l.getClass().isAssignableFrom(Group.class)) {
					enemies = (Group<Enemy>)l;
				}
			}
			if(enemies.numLinked() > 0) {
				List<Enemy> newEnemies = new LinkedList<>();
				Enemy next = enemies.getLinkedE().remove(0);
				for(Enemy e : next.destroy()) {
					newEnemies.add(e);
				}
				this.link(new ParticleEffect(next.X(), next.Y(), 50, 1, 200));
				for(Linkable l : newEnemies) {
					enemies.linkE((Enemy)l);
				}
				enemyDestroyClock = 0;
			}
		}
		
		for(int i : keysPressed) {
			if(!prevKeysPressed.contains(i)) {
				newKeysPressed.add(i);
			}
		}
		boolean upperCase = keysPressed.contains(KeyEvent.VK_SHIFT);
		if(!newKeysPressed.contains(KeyEvent.VK_ENTER) && gettingName) {
			for(int i : newKeysPressed) {
				if(i == KeyEvent.VK_BACK_SPACE) {
					if(playerName.length() > 0) {
						playerName = playerName.substring(0, playerName.length() - 1);
					}
				} else if(i != KeyEvent.VK_SHIFT){
					playerName += upperCase ? KeyEvent.getKeyText(i) : KeyEvent.getKeyText(i).toLowerCase();
				}
			}
		} else {
			gettingName = false;
		}
		prevKeysPressed = new TreeSet<>();
		for(int i : keysPressed) {
			prevKeysPressed.add(i);
		}
		return false;
	}
	
	public boolean nextScreen() {
		if(!gettingName) fadeTimer++;
		return !gettingName && fadeTimer >= FADE_TIME;
	}
	
	public void close() {
		saveLeaderboard();
	}
	
	private class Score implements Comparable<Score> {
		String name;
		int points;
		
		public Score(String name, int points) {
			this.name = name;
			this.points = points;
		}

		@Override
		public int compareTo(LoseScreen.Score o) {
			return o.points - this.points;
		}
	}
	
	private void loadLeaderboard() {
		Queue<Score> leaderboardOrderer = new PriorityQueue<>();;
		FileInputStream fileIn = null;
		try {
			fileIn = new FileInputStream(leaderboardFile);
		} catch(FileNotFoundException e) {System.out.println(e.getMessage());}
		try {
			if(fileIn.available() < 4) {
				leaderboardOrderer.add(new Score("joey", 10));
			} else {
				while(fileIn.available() > 0) {
					int scoreLength = fileIn.read();
					int score = readInt(scoreLength, fileIn);
					
					int nameLength = fileIn.read();
					String name = "";
					for(int i = 0; i < nameLength; i++) {
						name += (char)fileIn.read();
					}
					leaderboardOrderer.add(new Score(name, score));
				}
			}
		} catch(IOException e) {System.out.println(e.getMessage());}
		
		while(!leaderboardOrderer.isEmpty()) {
			leaderboard.add(leaderboardOrderer.remove());
		}
	}
	
	public void saveLeaderboard() {
		leaderboard.add(new Score(playerName, playerScore));
		FileOutputStream fileOut = null;
		try {
			fileOut = new FileOutputStream(leaderboardFile);
		} catch(FileNotFoundException e) {System.out.println(e.getMessage());}
		Iterator<Score> sItr = leaderboard.iterator();
		while(sItr.hasNext()) {
			Score next = sItr.next();
			String name = next.name;
			int points = next.points;
			char[] nameArray = name.toCharArray();
			try {
				int nLength = 0;
				for(int n = points; n > 0; n /= 256, nLength++);
				fileOut.write(nLength);
				writeInt(points, fileOut);
				
				fileOut.write(name.length());
				for(int i = 0; i < name.length(); i++) {
					fileOut.write(nameArray[i]);
				}
			} catch(IOException e) {System.out.println(e.getMessage());}
		}
		
	}
	
	private static void writeInt(int num, FileOutputStream out) throws IOException {
		for(int n = num; n > 0; n /= 256) {
			out.write(n % 256);
		}
	}
	
	private static int readInt(int bytes, FileInputStream in) {
		int num = 0;
		try {
			for(int i = 0; i < bytes; i++) {
				num += Math.pow(256, i) * in.read();
			}
		} catch(IOException e) {
			System.out.println(e.getMessage());
		}
		return num;
	}
	
	@Override
	public void drawAll(Graphics g) {
		Object[] currentLinked = linked.toArray();
		for(Object l : currentLinked) {
			((Linkable)l).drawAll(g);
		}
		draw(g, 0, 0, 0);
	}
	
	@Override
	protected void draw(Graphics g, double xOff, double yOff, double angOff) {
		g.setColor(new Color(0f, 0f, 0f, (float)(FADE_TIME - fadeTimer) / (float)FADE_TIME));
		g.fillRect((int)X() - 100, (int)Y() - 300, 200, 600);
		
		g.setColor(new Color(1f, 1f, 1f, (float)(FADE_TIME - fadeTimer) / (float)FADE_TIME));
		g.drawRect((int)X() - 100, (int)Y() - 300, 200, 600);
		g.drawLine((int)X() - 100, (int)Y() + 200, (int)X() + 100, (int)Y() + 200);
		
		g.setFont(new Font("Cambria Math", Font.PLAIN, 15));
		int scores = 0;
		Iterator<Score> sItr = leaderboard.iterator();
		while(sItr.hasNext()) {
			scores++;
			if(scores > 26) break;
			int drawHeight = scores * 19 + 30;
			Score next = sItr.next();
			String pointString = Integer.toString(next.points);
			String name = next.name;
			if(name.length() > 10) {
				name = name.substring(0, 11);
			}
			pointString = "0".repeat(Math.max(5 - pointString.length(), 0)) + pointString;
			g.drawString(name, (int)X() - 90, drawHeight);
			g.drawString(pointString, (int)X() + 40, drawHeight);
			g.drawLine((int)X() - 10, drawHeight - 5, (int)X() + 10, drawHeight - 5);
		}
		g.drawString("Please enter your name below:", (int)X() - 95, (int)Y() + 220);
		g.drawString(playerName, (int)X() - 95, (int)Y() + 240);
		
	}

}
