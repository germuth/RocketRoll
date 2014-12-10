package ca.gerumth.spaceprototype;

public class Geometry {

	public static class Point {
		int x;
		int y;
		double xx;
		double yy;
		public Point(int xx, int yy) {
			x = xx;
			y = yy;
		}
		public Point(double d, double e) {
			// TODO Auto-generated constructor stub
			xx = d;
			yy = e;
		}
	}
	
	private static double distance(double x1, double y1, double x2, double y2){
		return Math.sqrt( Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2) );
	}
	
	public static class LineSegment {
		Point a;
		Point b;
		public LineSegment(Point aa, Point bb){
			a = aa;
			b = bb;
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
		//http://i.stack.imgur.com/P556i.png
		private boolean intersects(LineSegment ls) {
			Point A = ls.a;
			Point B = ls.b;
			Point C = this.getCenter();
			//Project AC vector onto AB, resulting in AD
//			Point AB = new Point(B.x-A.x, B.y-A.y);
//			Point AC = new Point(C.x-A.x, C.y-A.y);
//			Point AD = AB * (AB dot AC) / (AB dot AB);
			double CF = ((B.x - A.x) * (C.x - A.x) + (B.y - A.y) * (C.y - A.y))
					/ ((B.x - A.x) ^ 2 + (B.y - A.y) ^ 2);
			double Dx = A.x + (B.x - A.x) * CF;
			double Dy = A.y + (B.y - A.y) * CF;
			
			double dist = distance(C.x, C.y, Dx, Dy);
			return dist <= this.r;
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
