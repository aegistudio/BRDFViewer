package net.aegistudio.brdfviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.aegistudio.brdfviewer.BRDFRender.BRDFFragment;

public class BRDFRenderer extends JPanel implements BRDFPerspective {
	private static final long serialVersionUID = 1L;

	private final BRDFRender render;
	private BRDFRender.BRDFFragment[][] renderFragments;
	private BufferedImage renderImage;
	
	private int viewportX = 512, viewportY = 512;
	private double lightX = 0.0, lightY = 0.0, lightZ = 1.0;
	private double cursorX = 0.5, cursorY = 0.5;
	
	private static final Dimension INFOTAG_SIZE = new Dimension(80, 25);
	private static final Dimension INPUT_SIZE = new Dimension(120, 25);
	
	private final JComponent brdfComponent = new JComponent() {
		private static final long serialVersionUID = 1L;
		boolean shouldObserve = false;
		boolean observeState = false;
		private double previousLightX, previousLightY, previousLightZ;
		{
			this.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent me) {
					observeState = me.getButton() == MouseEvent.BUTTON1;
					handleMouse(me);
					shouldObserve = true;
				}
				
				public void mouseReleased(MouseEvent me) {
					handleMouse(me);
					shouldObserve = false;
				}
			});
			
			this.addMouseMotionListener(new MouseAdapter() {
				public void mouseDragged(MouseEvent me) {
					if(shouldObserve)
						handleMouse(me);
				}
			});
		}

		@Override
		public void paint(Graphics g) {
			g.fillRect(0, 0, getWidth(), getHeight());
			if(host == null) return;
			if(host.getData() == null) return;
			
			boolean causedByResizing = renderImage == null 
					|| renderImage.getWidth() != viewportX 
					|| renderImage.getHeight() != viewportY;
			if(causedByResizing) {
				renderImage = new BufferedImage(viewportX, 
						viewportY, BufferedImage.TYPE_3BYTE_BGR);
				renderFragments = new BRDFRender.BRDFFragment
						[viewportX][viewportY];
				for(int i = 0; i < viewportX; ++ i)
					for(int j = 0; j < viewportY; ++ j)
						renderFragments[i][j] = new BRDFRender.BRDFFragment(); 
			}
			
			boolean causedByUpdating = 
					   previousLightX != lightX 
					|| previousLightY != lightY
					|| previousLightZ != lightZ;
			if(causedByResizing || causedByUpdating) {
				host.detachModalWork(BRDFRenderer.this, "Rendering plane...", notifier -> {
					previousLightX = lightX;
					previousLightY = lightY;
					previousLightZ = lightZ;
					render.renderDirectional(false,
							viewportX, viewportY, 
							renderFragments, previousLightX, 
							previousLightY, previousLightZ);
					
					BRDFVector3d colorTuple = new BRDFVector3d();
					for(int i = 0; i < viewportX; ++ i) {
						for(int j = 0; j < viewportY; ++ j) {
							if(!renderFragments[i][j].discarded) {
								host.getData().fetch(
									renderFragments[i][j].thetaHalf,
									renderFragments[i][j].thetaDiff,
									renderFragments[i][j].phiDiff, colorTuple);
								colorTuple.clamp();
								renderImage.setRGB(i, j, colorTuple.asRGB());
							}
							else renderImage.setRGB(i, j, 0);
						}
						notifier.accept(i, viewportX);
					}
					notifier.accept(1, 1);
					informationUpdate();
				});
			}
			
			if(renderImage != null) g.drawImage(renderImage, 0, 0, null);
			
			// Render the origin lengend.
			int originNearPos = 1, originFarPos = 8;
			int originNearMin = 3, originFarMin = 10;
			int originCenterX = (int)(viewportX * 0.5);
			int originCenterY = (int)(viewportY * 0.5);
			g.setColor(Color.green);
			g.drawLine(originCenterX + originNearPos, originCenterY + originNearPos, 
					originCenterX + originNearPos, originCenterY + originFarPos);
			g.drawLine(originCenterX - originNearMin, originCenterY + originNearPos, 
					originCenterX - originNearMin, originCenterY + originFarPos);
			g.drawLine(originCenterX + originNearPos, originCenterY - originNearMin, 
					originCenterX + originNearPos, originCenterY - originFarMin);
			g.drawLine(originCenterX - originNearMin, originCenterY - originNearMin, 
					originCenterX - originNearMin, originCenterY - originFarMin);
			
			g.drawLine(originCenterX + originNearPos, originCenterY + originNearPos, 
					originCenterX + originFarPos, originCenterY + originNearPos);
			g.drawLine(originCenterX - originNearMin, originCenterY + originNearPos, 
					originCenterX - originFarMin, originCenterY + originNearPos);
			g.drawLine(originCenterX + originNearPos, originCenterY - originNearMin, 
					originCenterX + originFarPos, originCenterY - originNearMin);
			g.drawLine(originCenterX - originNearMin, originCenterY - originNearMin, 
					originCenterX - originFarMin, originCenterY - originNearMin);
			
			// Render the light direction legend.
			int lightNear = 5, lightFar = 10;
			int lightIncNear = 3, lightIncFar = 7;
			float viewportMin = Math.min(viewportX - 1, viewportY - 1);
			int lightCenterX = (int)(viewportMin * (0.5 + lightX));
			int lightCenterY = (int)(viewportMin * (0.5 + lightY));
			g.setColor(Color.green);
			g.fillRect(lightCenterX - 1, lightCenterY - 1, 3, 3);
			g.drawLine(lightCenterX + lightNear, lightCenterY, 
					lightCenterX + lightFar, lightCenterY);
			g.drawLine(lightCenterX - lightNear, lightCenterY, 
					lightCenterX - lightFar, lightCenterY);
			g.drawLine(lightCenterX, lightCenterY + lightNear, 
					lightCenterX, lightCenterY + lightFar);
			g.drawLine(lightCenterX, lightCenterY - lightNear, 
					lightCenterX, lightCenterY - lightFar);
			
			g.drawLine(lightCenterX + lightIncNear, lightCenterY + lightIncNear, 
					lightCenterX + lightIncFar, lightCenterY + lightIncFar);
			g.drawLine(lightCenterX + lightIncNear, lightCenterY - lightIncNear, 
					lightCenterX + lightIncFar, lightCenterY - lightIncFar);
			g.drawLine(lightCenterX - lightIncNear, lightCenterY + lightIncNear, 
					lightCenterX - lightIncFar, lightCenterY + lightIncFar);
			g.drawLine(lightCenterX - lightIncNear, lightCenterY - lightIncNear, 
					lightCenterX - lightIncFar, lightCenterY - lightIncFar);
			
			// Render the picking cursor legend.
			int cursorNear = 5, cursorFar = 10;
			int cursorCenterX = (int)(cursorX * (viewportX - 1));
			int cursorCenterY = (int)(cursorY * (viewportY - 1));
			
			g.setColor(Color.green);
			g.drawLine(cursorCenterX + cursorNear, cursorCenterY + 0, 
					cursorCenterX + cursorFar, cursorCenterY + 0);
			g.drawLine(cursorCenterX - cursorNear, cursorCenterY + 0, 
					cursorCenterX - cursorFar, cursorCenterY + 0);
			g.drawLine(cursorCenterX + 0, cursorCenterY + cursorNear, 
					cursorCenterX + 0, cursorCenterY + cursorFar);
			g.drawLine(cursorCenterX + 0, cursorCenterY - cursorNear, 
					cursorCenterX + 0, cursorCenterY - cursorFar);
			
		}
		
		private void handleMouse(MouseEvent me) {
			int cursorX = Math.max(0, Math.min(me.getX(), viewportX - 1));
			int cursorY = Math.max(0, Math.min(me.getY(), viewportY - 1));
			if(observeState) {
				// Left mouse button.
				selectSample(cursorX, cursorY, 
						renderFragments[cursorX][cursorY]);
				BRDFRenderer.this.cursorX = 1.0 * cursorX / (viewportX - 1);
				BRDFRenderer.this.cursorY = 1.0 * cursorY / (viewportY - 1);
			}
			else {
				// Update light position.
				double minViewport = Math.min(
						viewportX - 1, viewportY - 1);
				lightX = (cursorX - viewportX / 2) / minViewport;
				lightY = (cursorY - viewportY / 2) / minViewport;
			}
			repaint();
		}
	};
	
	private final JScrollPane componentScroll;
	private Object initializedObject = new Object();
	private final JColorSampler colorSampler = 
			new JColorSampler(INFOTAG_SIZE, INPUT_SIZE);
	
	public BRDFRenderer(BRDFRender render) {
		this.render = render;
		this.setLayout(new BorderLayout());
		this.add(this.componentScroll 
				= new JScrollPane(brdfComponent), 
				BorderLayout.CENTER);
		
		// The panel on the right.
		JPanel eastPanel = new JPanel();
		eastPanel.setLayout(new BorderLayout());
		this.add(eastPanel, BorderLayout.EAST);
		
		// The panel contains the light source inputs.
		JPanel slicingPanel = new JPanel();
		slicingPanel.setPreferredSize(
				new Dimension(220, 0));
		eastPanel.add(slicingPanel, BorderLayout.CENTER);
		
		// The panel contains the information output and hint inputs.
		JPanel informationPanel = new JPanel();
		informationPanel.setPreferredSize(
				new Dimension(220, 160));
		FlowLayout informationLayout = new FlowLayout();
		informationLayout.setVgap(0);
		informationPanel.setLayout(informationLayout);
		eastPanel.add(informationPanel, BorderLayout.SOUTH);
		
		// Insert the sample boxes.
		informationPanel.add(colorSampler);
	}
	
	private BRDFHost host;
	@Override
	public void setHost(BRDFHost host) {
		this.host = host;
	}
	
	@Override
	public void updateAngle(BRDFPerspective perspective,
			double thetaHalf, double thetaDiff, double phiDiff) {
		// Do nothing, we can just sample internal data.
	}
	
	private void forceUpdate() {
		this.brdfComponent.setPreferredSize(
				new Dimension(viewportX, viewportY));
		this.renderImage = null;
		this.componentScroll.updateUI();
		
		informationUpdate();
		
		repaint();
	}
	
	private void informationUpdate() {
		if(this.initializedObject != null 
				&& this.renderFragments != null) {
			int x = (int)(this.cursorX * (this.viewportX - 1));
			int y = (int)(this.cursorY * (this.viewportY - 1));
			
			BRDFFragment current = this.renderFragments[x][y];
			
			BRDFVector3d colorSample = new BRDFVector3d();
			host.getData().fetch(current.thetaHalf, 
					current.thetaDiff, current.phiDiff, colorSample);
			colorSampler.setColorSample(colorSample);
		}
	}
	
	@Override
	public void updateData(BRDFData data) {
		forceUpdate();
	}
	
	@Override
	public Component getComponent() {
		return this;
	}
	
	private void selectSample(int x, int y, BRDFFragment sample) {
		host.broadcastAngleUpdate(this, sample.thetaHalf, 
				sample.thetaDiff, sample.phiDiff);
		informationUpdate();
	}
}
