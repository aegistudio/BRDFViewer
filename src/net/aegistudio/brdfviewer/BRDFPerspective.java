package net.aegistudio.brdfviewer;

import java.awt.Component;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface BRDFPerspective {
	public interface BRDFHost {
		public BRDFData getData();
		
		public void broadcastAngleUpdate(BRDFPerspective thiz, 
				double thetaHalf, double thetaDiff, double phiDiff);
		
		public void detachModalWork(BRDFPerspective thiz, String tips, 
				Consumer<BiConsumer<Integer, Integer>> progressNotifierCallback);
	}
	
	public void setHost(BRDFHost host);
	
	public void updateAngle(BRDFPerspective source,
			double thetaHalf, double thetaDiff, double phiDiff);
	
	public void updateData(BRDFData data);
	
	public Component getComponent();
}
