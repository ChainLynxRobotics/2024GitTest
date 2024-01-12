 import java.awt.image.BufferedImage;

import fromics.Background;
import fromics.Frindow;
import fromics.Manager;

public class Game extends Manager{
	private Frindow win;
	private Player ship;
	
	public static void main(String[] args) {
		new Game(new Frindow(BufferedImage.TYPE_INT_RGB, 1200, 700));
	}
	
	public Game(Frindow observer) {
		super(observer);
		win = observer;
		screens = new Background[3];
		win.init(3, this);
		initGame();
		startLoop();
	}
	
	private void initGame() {
		screens[0] = new TitleScreen(win, screens[2], win.getKeys().codes);
	}
	
	protected void initScreen(int n) {
		switch(n) {
			case 0 :
				initGame();
				break;
			case 1:
				ship = new Player(win.getKeys().codes);
				screens[1] = new GameScreen(win, ship, screens[0].getLinked());
				break;
			case 2:
				screens[2] = new LoseScreen(win, win.getKeys().codes, screens[1].getLinked(), ((GameScreen)screens[1]).score());
				break;
			default:
				
		}
	}
	
}
