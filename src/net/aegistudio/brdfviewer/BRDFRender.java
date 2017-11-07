package net.aegistudio.brdfviewer;

public interface BRDFRender {
	public class BRDFFragment {
		public boolean discarded = true;
		
		public final BRDFVector3d normal = new BRDFVector3d();
		
		public final BRDFVector3d half = new BRDFVector3d();
		
		public final BRDFVector3d view = new BRDFVector3d();
		
		public final BRDFVector3d light = new BRDFVector3d();
		
		public double lightDistance;
		
		public double thetaHalf, thetaDiff, phiDiff;
	}
	
	public void renderDirectional(
			int viewportWidth, int viewportHeight,
			BRDFFragment[][] viewportFragments,
			double lightX, double lightY, double lightZ);
	
	public static double length(double[] vector) {
		return Math.sqrt(vector[0] * vector[0]
				+ vector[1] * vector[1] 
				+ vector[2] * vector[2]);
	}
	
	public static double normalize(double[] vector) {
		double distance = length(vector);
		vector[0] /= distance; 
		vector[1] /= distance; 
		vector[2] /= distance;
		return distance;
	}
}