package net.aegistudio.brdfviewer;

import java.io.*;
import java.util.function.BiConsumer;

public class BRDFData {
	public final double[][] samples;
	public final int dimThetaHalf, dimThetaDiff, dimPhiDiff;

	public BRDFData(int dimThetaHalf, 
		int dimThetaDiff, int dimPhiDiff) {
		this.dimThetaHalf = dimThetaHalf;
		this.dimThetaDiff = dimThetaDiff;
		this.dimPhiDiff = dimPhiDiff;
		this.samples = new double[dimThetaHalf 
			* dimThetaDiff * dimPhiDiff][3];
	}

	public static BRDFData open(File file, 
			BiConsumer<Integer, Integer> progressNotifier) 
			throws IOException {
		
		try(FileInputStream fileInput = new FileInputStream(file);
			DataInputStream dataInput = new DataInputStream(fileInput)) {

			// Read metadata.
			int dimThetaHalf = Integer.reverseBytes(dataInput.readInt());
			int dimThetaDiff = Integer.reverseBytes(dataInput.readInt());
			int dimPhiDiff = Integer.reverseBytes(dataInput.readInt());
			int dimChannel = dimThetaHalf * dimThetaDiff * dimPhiDiff;
			BRDFData result = new BRDFData(dimThetaHalf, 
				dimThetaDiff, dimPhiDiff);
	
			// Traverse through R/G/B channels.
			for(int channel = 0; channel < 3; ++ channel) {
				// Just place the data by channels.
				for(int j = 0; j < dimChannel; ++ j) {
					result.samples[j][channel] = Double.longBitsToDouble(
							Long.reverseBytes(dataInput.readLong()));
					
					int progressCurrent = channel * dimChannel + j;
					if(progressCurrent % 1000 == 0) progressNotifier
						.accept(progressCurrent, 3 * dimChannel);
				}
			}
	
			progressNotifier.accept(1, 1);
			return result;
		}
	}
	
	private void fetchSample(double ratio, double thetaHalfValue, 
			double thetaDiffValue, double phiDiffValue, BRDFVector3d color) {
		
		thetaHalfValue = Math.max(0, Math.min(thetaHalfValue, dimThetaHalf - 1));
		thetaDiffValue = Math.max(0, Math.min(thetaDiffValue, dimThetaDiff - 1));
		phiDiffValue = Math.max(0, Math.min(phiDiffValue, dimPhiDiff - 1));
		
		int offset = ((int)thetaHalfValue) * dimThetaDiff * dimPhiDiff +
				((int)thetaDiffValue) * dimPhiDiff + (int)phiDiffValue;
		
		double sample0 = samples[offset][0] * 1.00 / 1500.0;
		double sample1 = samples[offset][1] * 1.15 / 1500.0;
		double sample2 = samples[offset][2] * 1.66 / 1500.0;
		
		if(sample0 < 0.0) sample0 = Double.NaN;
		if(sample1 < 0.0) sample1 = Double.NaN;
		if(sample2 < 0.0) sample2 = Double.NaN;
		
		color.x += ratio * sample0;
		color.y += ratio * sample1;
		color.z += ratio * sample2;
	}
	
	public void fetch(double thetaHalf, double thetaDiff, 
			double phiDiff, BRDFVector3d color) {
		
		// Calculate ratio.
		double thetaHalfRatio = thetaHalf / (Math.PI * 0.5);
		double thetaDiffRatio = thetaDiff / (Math.PI * 0.5);
		double phiDiffRatio = phiDiff / (Math.PI);
		thetaHalfRatio = Math.sqrt(thetaHalfRatio);
		
		// Fetch data from sample.
		//int thetaHalfIndex = (int)(dimThetaHalf * thetaHalfRatio);
		//int thetaDiffIndex = (int)(dimThetaDiff * thetaDiffRatio);
		//int phiDiffIndex = (int)(dimPhiDiff * phiDiffRatio);
		double thetaHalfIndex = (dimThetaHalf * thetaHalfRatio);
//		double thetaDiffIndex = dimThetaDiff - (dimThetaDiff * thetaDiffRatio) - 1.0;
		double thetaDiffIndex = (dimThetaDiff * thetaDiffRatio);
		double phiDiffIndex = (dimPhiDiff * phiDiffRatio);
		
		// Calculate the in-band offset.
		//int offset = thetaHalfIndex * dimThetaDiff * dimPhiDiff +
		//		thetaDiffIndex * dimPhiDiff + phiDiffIndex;
		
		// Copy the color to the result.
		color.x = color.y = color.z = 0.0;
		fetchSample(0.25, Math.floor(thetaHalfIndex), 
				Math.floor(thetaDiffIndex), 
				Math.floor(phiDiffIndex), color);
		fetchSample(0.25, Math.floor(thetaHalfIndex), 
				Math.ceil(thetaDiffIndex), 
				Math.floor(phiDiffIndex), color);
		fetchSample(0.25, Math.floor(thetaHalfIndex), 
				Math.ceil(thetaDiffIndex), 
				Math.ceil(phiDiffIndex), color);
		fetchSample(0.25, Math.floor(thetaHalfIndex), 
				Math.floor(thetaDiffIndex), 
				Math.ceil(phiDiffIndex), color);
	}
}
