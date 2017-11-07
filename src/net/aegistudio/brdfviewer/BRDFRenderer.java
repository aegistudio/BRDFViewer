package net.aegistudio.brdfviewer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
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
	
//	private static final Dimension INFOTAG_SIZE = new Dimension(80, 25);
//	private static final Dimension INPUT_SIZE = new Dimension(120, 25);
	
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
				});
			}
			
			if(renderImage != null) g.drawImage(renderImage, 0, 0, null);
		}
		
		private void handleMouse(MouseEvent me) {
			int cursorX = Math.max(0, Math.min(me.getX(), viewportX - 1));
			int cursorY = Math.max(0, Math.min(me.getY(), viewportY - 1));
			if(observeState) {
				// Left mouse button.
				selectSample(cursorX, cursorY, 
						renderFragments[cursorX][cursorY]);
			}
			else {
				// Update light position.
				double minViewport = Math.min(
						viewportX - 1, viewportY - 1);
				lightX = (cursorX - viewportX / 2) / minViewport;
				lightY = (cursorY - viewportY / 2) / minViewport;
				repaint();
			}
		}
	};
	
	private final JScrollPane componentScroll;
	
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
		
		// The panel contains the angle inputs.
		JPanel slicingPanel = new JPanel();
		slicingPanel.setPreferredSize(
				new Dimension(220, 0));
		eastPanel.add(slicingPanel, BorderLayout.CENTER);
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
		repaint();
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
	}
}
