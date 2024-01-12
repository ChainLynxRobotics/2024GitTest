import fromics.Point;
import fromics.PolygonCollider;

public abstract class Enemy extends PolygonCollider{

	protected Enemy(double x, double y) {
		super(x, y);
	}
	
	@Override
	public boolean update() {
		Point maxVals = parent.getMaxBounds();
		int maxX = (int)maxVals.X() + 50;
		int maxY = (int)maxVals.Y() + 60;
		loop(maxX, maxY);
		return false;
	}
	
	public abstract Enemy[] destroy();
	public abstract int getPoints();

}
