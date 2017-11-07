package net.aegistudio.brdfviewer;

public class BRDFVector3d {
	public double x, y, z;
	
	public double length() {
		return Math.sqrt(dot(this));
	}
	
	public double normalize() {
		double modulus = length();
		x /= modulus; y /= modulus; z /= modulus;
		return modulus;
	}
	
	public double dot(BRDFVector3d b) {
		return x * b.x + y * b.y + z * b.z;
	}
	
	public void cross(BRDFVector3d a, BRDFVector3d b) {
		x = a.y * b.z - a.z * b.y;
		y = a.z * b.x - a.x * b.z;
		z = a.x * b.y - a.y * b.x;
	}
	
	public void linearAdd(BRDFVector3d a, double k, BRDFVector3d b) {
		x = a.x + b.x * k;
		y = a.y + b.y * k;
		z = a.z + b.z * k;
	}
	
	public void clamp() {
		x = Math.max(0, Math.min(1, x));
		y = Math.max(0, Math.min(1, y));
		z = Math.max(0, Math.min(1, z));
	}
	
	public int asRGB() {
		return 	  (((int)(z * 255) & 0x0ff) << 0)
				+ (((int)(y * 255) & 0x0ff) << 8)
				+ (((int)(x * 255) & 0x0ff) << 16);
	}
}
