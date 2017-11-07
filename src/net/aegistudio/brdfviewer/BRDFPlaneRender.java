package net.aegistudio.brdfviewer;

public class BRDFPlaneRender implements BRDFRender {
	@Override
	public void renderDirectional(boolean symmetricPhi,
			int viewportWidth, int viewportHeight, 
			BRDFFragment[][] viewportFragments,
			double lightX, double lightY, double lightZ) {
		
		int viewportStride = Math.min(viewportWidth, viewportHeight);
		for(int i = 0; i < viewportWidth; ++ i)
			for(int j = 0; j < viewportHeight; ++ j) {
				BRDFFragment current = viewportFragments[i][j];
				current.discarded = false;
				current.normal.x = current.normal.y = 0.0;
				current.normal.z = 1.0;
				
				// Calculate the view direction.
				current.view.x = 1.0 * (i - viewportWidth / 2) / viewportStride;
				current.view.y = 1.0 * (j - viewportHeight / 2) / viewportStride;
				current.view.z = 1.0;
				current.view.normalize();
				
				// Calculate the light direction.
				current.light.x = lightX;
				current.light.y = lightY;
				current.light.z = lightZ;
				current.light.normalize();
				current.lightDistance = 1.0;
				
				// Calculate the half vector.
				current.half.x = current.view.x + current.light.x;
				current.half.y = current.view.y + current.light.y;
				current.half.z = current.view.z + current.light.z;
				current.half.normalize();
				
				// Calculate the half-diff directions.
				double nDotH = current.normal.dot(current.half);
				current.thetaHalf = Math.acos(nDotH);
				if(nDotH <= 0.0) { current.discarded = true; return; }
				
				double hDotL = current.half.dot(current.light);
				current.thetaDiff = Math.acos(hDotL);
				
				if(nDotH >= 1.0 || hDotL >= 1.0) 
					current.phiDiff = 0.5 * Math.PI;
				else {
					BRDFVector3d projHalfX = new BRDFVector3d();
					projHalfX.linearAdd(current.half, -1.0 / nDotH, current.normal);
					projHalfX.normalize();
					
					BRDFVector3d projLight = new BRDFVector3d();
					projLight.linearAdd(current.light, -hDotL, current.half);
					projLight.normalize();
					
					BRDFVector3d projHalfY = new BRDFVector3d();
					projHalfY.cross(projHalfX, current.half);
					projHalfY.normalize();
					
					if(symmetricPhi) current.phiDiff = Math.acos(Math.min(
							Math.max(projHalfX.dot(projLight), 0.0), 1.0));
					else current.phiDiff = Math.atan2(
							projHalfY.dot(projLight), projHalfX.dot(projLight));
					
					if(current.phiDiff < 0.0) 
						current.phiDiff += Math.PI;
					if(current.phiDiff >= Math.PI) 
						current.phiDiff -= Math.PI;
				}
			}
	}

}
