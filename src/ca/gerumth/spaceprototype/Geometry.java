package ca.gerumth.spaceprototype;

public class Geometry {

	public static class Point {
		double x;
		double y;
		public Point(double d, double e) {
			// TODO Auto-generated constructor stub
			x = d;
			y = e;
		}
		public Point add(double val){
			return new Point(x + val, y + val);
		}
		public Point add(Point other){
			return new Point(x + other.x, y + other.y);
		}
		public Point subtract(Point other){
			return new Point(x - other.x, y - other.y);
		}
		public Point multiply(Point other){
			return new Point(x * other.x, y * other.y);
		}
		public double dotProduct(Point other){
			return x*other.x + y*other.y;
		}
	}

	private static double distance(Point a, Point b){
		return distance(a.x, a.y, b.x, b.y);
	}
	
	private static double distance(double x1, double y1, double x2, double y2){
		return Math.sqrt( Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2) );
	}
	
	private static double distSquared(double x1, double y1, double x2, double y2){
		return Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2);
	}
	
	public static class LineSegment {
		Point a;
		Point b;
		public LineSegment(Point aa, Point bb){
			a = aa;
			b = bb;
		}
//		public void truncateLine(double length){
//			double lengthAB = Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2);
//			lengthAB = Math.sqrt(lengthAB);
//			
//			double xdelta = b.x - a.x;
//			double ydelta = b.y - a.y;
//			this.b = new Point(xdelta * length/lengthAB, ydelta * length/lengthAB);
//		}
		public void extendLine(double length){
			double lengthAB = Math.pow(a.x - b.x, 2) + Math.pow(a.y - b.y, 2);
			lengthAB = Math.sqrt(lengthAB);

			double x = b.x + (b.x - a.x) / (lengthAB / length);
			double y = b.y + (b.y - a.y) / (lengthAB / length);
			//
			
//			double x = b.x + length * Math.cos(Math.atan2(b.y, b.x));
//			double y = b.y + length * Math.sin(Math.atan2(b.y, b.x));
			
			this.b = new Point(x, y);
//			return new LineSegment(a, new Point(x, y));
		}
	}
	
	public static class Polygon {
		Point[] points;
		public Polygon(Point[] arr){
			points = arr;
		}
		//test whether point lies within polygon
		public boolean contains(Point test) { 
			int i, j;
			boolean result = false;
			for (i = 0, j = points.length - 1; i < points.length; j = i++) {
				if ((points[i].y > test.y) != (points[j].y > test.y)
						&& (test.x < (points[j].x - points[i].x) * (test.y - points[i].y)
								/ (points[j].y - points[i].y) + points[i].x)) {
					result = !result;
				}
			}
			return result;
		}
	}

	public static class Circle {
		int x;
		int y;
		double r;
		
		public Circle(int xx, int yy, double rr) {
			x = xx;
			y = yy;
			r = rr;
		}
		
		public Point getCenter(){
			return new Point(x, y);
		}
		
		//intersection between circle and line segment
		//compare distance from line segment to center point
		//if larger/smaller than radius for intersects
		//http://i.stack.imgur.com/P556i.png
		//http://stackoverflow.com/questions/849211/shortest-distance-between-a-point-and-a-line-segment
		private boolean intersects(LineSegment ls) {
			Point v = ls.a;
			Point w = ls.b;
			Point p = this.getCenter();
			
			// Consider the line extending the segment, parameterized as v + t (w - v).
			// We find projection of point p onto the line. 
			// It falls where t = [(p-v) . (w-v)] / |w-v|^2
			
			double lengthSquared = distSquared(v.x, v.y, w.x, w.y);
			double t = ( p.subtract(v) ).dotProduct( w.subtract(v) ) / lengthSquared; 
			
			//distance between circle center and line
			double distance = 0.0;
			
			if(t < 0.0){
				//center is beyond v
				distance = distance(p, v);
			}else if(t > 1.0){
				//center is beyond w
				distance = distance(p, w);
			}else{
				Point projection = ( v.add(t) ).multiply(w.subtract(v));
				distance = distance(p, projection);
			}
			
			return distance <= this.r;
		}


		//intersection between polygon and circle
		public boolean intersects(Polygon poly) {
			//There are two cases when a circle intersects a polygon
			//1) The circles center lies within the polygon
			//2) One of the Edges of the Rectangle intersects the circle
			
			//1) can be solved with detecting whether a polygon contains a point
			//2) can be solved by checking for intersection between a line segment and
			//circle for each edge of the polygon
			
//			if(poly.points[0].x != 225){
//				System.out.println("DEBUG");
//			}
			boolean intersect = false;
			//case 1
			intersect |= poly.contains(this.getCenter());
			
			//case 2
			for(int i = 0; i < poly.points.length - 1; i++){
				intersect |= this.intersects(new LineSegment(poly.points[i], poly.points[i+1]));
			}
			//loop around, last element to first
			intersect |= this.intersects(new LineSegment(poly.points[poly.points.length - 1], poly.points[0]));
			
			return intersect;
		}
	}

}
