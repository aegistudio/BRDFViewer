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

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

public class BRDFSlice extends JPanel implements BRDFPerspective {
	
	private static final long serialVersionUID = 1L;
	
	protected static final int SLICEMODE_THETAHALF = 0;
	protected static final int SLICEMODE_THETADIFF = 1;
	protected static final int SLICEMODE_PHIDIFF = 2;
	protected int sliceMode;
	
	private final JCheckBox smoothDisplay 
		= new JCheckBox("Smooth sample points");
	
	private int granularity = 3;
	private double thetaHalf = 0.0, 
			thetaDiff = 0.0, phiDiff = 0.0;
	private double strideThetaHalf, 
			strideThetaDiff, stridePhiDiff;
	
	private BufferedImage sliceImage;
	
	private final JComponent displayComponent = new JComponent() {
		private static final long serialVersionUID = 1L;
		
		private double previousThetaHalf, 
			previousThetaDiff, previousPhiDiff;
		
		public void paint(Graphics g) {
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());
		
			// Render the click instruction legend.
			g.setColor(Color.green);
			int instructionHeight = (int)g.getFontMetrics()
					.getStringBounds("PLACEHOLDER", g).getHeight();
			g.drawString("Left Click: Query information at point", 
					5, getHeight() - 1 * instructionHeight);
			
			// Render the BRDF's image slice.
			if(host.getData() != null) {
				// Render slice data.
				BRDFVector3d colorTuple = new BRDFVector3d();

				boolean causedByNewImage = false;
				if(	sliceImage == null
				||	sliceImage.getWidth() != getXSpan() * granularity
				||	sliceImage.getHeight() != getYSpan() * granularity) {
					sliceImage = new BufferedImage(
							getXSpan() * granularity, getYSpan() * granularity, 
							BufferedImage.TYPE_3BYTE_BGR);
					causedByNewImage = true;
				}
				
				boolean causedByAngleChange = 
						(sliceMode == SLICEMODE_THETAHALF && previousThetaHalf != thetaHalf) ||
						(sliceMode == SLICEMODE_THETADIFF && previousThetaDiff != thetaDiff) ||
						(sliceMode == SLICEMODE_PHIDIFF && previousPhiDiff != phiDiff);

				if(causedByNewImage || causedByAngleChange) {
					previousThetaHalf = thetaHalf;
					previousThetaDiff = thetaDiff;
					previousPhiDiff = phiDiff;
					
					if(smoothDisplay.isSelected()) 
						// Perform modal work only if smoothing.
						host.detachModalWork(BRDFSlice.this, 
							"Rendering slice...", progress -> {
							double granularityStride = 1.0 / granularity;
							// Render by every single samples.
							for(int i = 0; i < getXSpan() * granularity; ++ i) {
								for(int j = 0; j < getYSpan() * granularity; ++ j) {
									fetchTuple(i * granularityStride, 
											j * granularityStride, colorTuple);
									sliceImage.setRGB(i, j, colorTuple.asRGB());
								}
								progress.accept(i, getXSpan());
							}
							
							progress.accept(1, 1);
						});
					else {
						Graphics ig = sliceImage.getGraphics();
						// Render according to span only.
						for(int i = 0; i < getXSpan(); ++ i) {
							for(int j = 0; j < getYSpan(); ++ j) {
								fetchTuple(i, j, colorTuple);
								
								Color dataColor = new Color((float)colorTuple.x, 
										(float)colorTuple.y, (float)colorTuple.z);
								ig.setColor(dataColor);
								ig.fillRect(i * granularity, j * granularity, 
										granularity, granularity);
							}
						}
					}
				}
				
				if(sliceImage == null) return;
				g.drawImage(sliceImage, 0, 0, null);
				
				// Render cursor overlay.
				double xPos = 0, yPos = 0;
				double phiDiffRatio = phiDiff / Math.PI;
				double thetaDiffRatio = thetaDiff / (0.5 * Math.PI);
				double thetaHalfRatio = thetaHalf / (0.5 * Math.PI);
				switch(sliceMode) {
					case SLICEMODE_THETAHALF:
						xPos = phiDiffRatio;
						yPos = thetaDiffRatio;
					break;
					
					case SLICEMODE_THETADIFF:
						xPos = phiDiffRatio;
						yPos = thetaHalfRatio;
					break;
					
					case SLICEMODE_PHIDIFF:
						xPos = thetaDiffRatio;
						yPos = thetaHalfRatio;
					break;
				}
				
				int xCenter = (int)(xPos * getXSpan() * granularity);
				int yCenter = (int)(yPos * getYSpan() * granularity);
				int near = 5, far = 10;
				g.setColor(Color.green);
				g.drawLine(xCenter + near, yCenter + 0, xCenter + far, yCenter + 0);
				g.drawLine(xCenter - near, yCenter + 0, xCenter - far, yCenter + 0);
				g.drawLine(xCenter + 0, yCenter + near, xCenter + 0, yCenter + far);
				g.drawLine(xCenter + 0, yCenter - near, xCenter + 0, yCenter - far);
			}
		}
		
		private void onMouseEvent(MouseEvent me) {
			if(host.getData() == null) return;
			int x = Math.max(0, Math.min(me.getX(), 
					granularity * getXSpan() - 1));
			int y = Math.max(0, Math.min(me.getY(), 
					granularity * getYSpan() - 1));
			notifySliceInput(1.0 * x / (granularity * getXSpan()), 
					1.0 * y / (granularity * getYSpan()));
		}
		
		{
			this.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					onMouseEvent(me);
				}
			});
			
			this.addMouseMotionListener(new MouseAdapter() {
				public void mouseDragged(MouseEvent me) {
					onMouseEvent(me);
				}
			});
		}
	};
	
	private final JScrollPane displayScroll;
	private static final Dimension INFOTAG_SIZE = new Dimension(80, 25);
	private static final Dimension INPUT_SIZE = new Dimension(120, 25);
	
	private final JDegreeField thetaHalfField = 
			new JDegreeField(Math.PI * 0.5, INPUT_SIZE);
	private final JRadioButton thetaHalfSliceLabel;
	
	private final JRadioButton thetaDiffSliceLabel;
	private final JDegreeField thetaDiffField = 
			new JDegreeField(Math.PI * 0.5, INPUT_SIZE);
	
	private final JRadioButton phiDiffSliceLabel;
	private final JDegreeField phiDiffField = 
			new JDegreeField(Math.PI, INPUT_SIZE);
	
	private Object initializedObject = new Object();
	private final JColorSampler colorSample 
		= new JColorSampler(INFOTAG_SIZE, INPUT_SIZE);
	
	public BRDFSlice() {
		this.setLayout(new BorderLayout());
		this.add(this.displayScroll = 
				new JScrollPane(this.displayComponent), 
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
		
		ButtonGroup buttonGroup = new ButtonGroup();
		
		// Insert the theta-half slice option.
		JPanel thetaHalfSlicing = new JPanel();
		slicingPanel.add(thetaHalfSlicing);
		
		thetaHalfSliceLabel = new JRadioButton(
				"<html><i>\u03b8</i><sub>h</sub></html>");
		thetaHalfSliceLabel.setHorizontalAlignment(JLabel.RIGHT);
		thetaHalfSliceLabel.setPreferredSize(INFOTAG_SIZE);
		thetaHalfSliceLabel.addActionListener(
				a -> switchSliceMode(SLICEMODE_THETAHALF));
		thetaHalfSlicing.add(thetaHalfSliceLabel);
		buttonGroup.add(thetaHalfSliceLabel);

		thetaHalfField.addChangeListener(a -> {
			if(host != null) host.broadcastAngleUpdate(null, 
					thetaHalfField.getValue(), thetaDiff, phiDiff);
		});
		thetaHalfSlicing.add(thetaHalfField);
		
		// Insert the theta-diff slice option.
		JPanel thetaDiffSlicing = new JPanel();
		slicingPanel.add(thetaDiffSlicing);
		
		thetaDiffSliceLabel = new JRadioButton(
				"<html><i>\u03b8</i><sub>d</sub></html>");
		thetaDiffSliceLabel.setHorizontalAlignment(JLabel.RIGHT);
		thetaDiffSliceLabel.setPreferredSize(INFOTAG_SIZE);
		thetaDiffSlicing.add(thetaDiffSliceLabel);
		thetaDiffSliceLabel.addActionListener(
				a -> switchSliceMode(SLICEMODE_THETADIFF));
		buttonGroup.add(thetaDiffSliceLabel);

		thetaDiffField.addChangeListener(a -> {
			if(host != null) host.broadcastAngleUpdate(null, 
					thetaHalf, thetaDiffField.getValue(), phiDiff);
		});
		thetaDiffSlicing.add(thetaDiffField);
		
		// Insert the phi-diff slice option.
		JPanel phiDiffSlicing = new JPanel();
		slicingPanel.add(phiDiffSlicing);
		
		phiDiffSliceLabel = new JRadioButton(
				"<html><i>\u03d5</i><sub>d</sub></html>");
		phiDiffSliceLabel.setHorizontalAlignment(JLabel.RIGHT);
		phiDiffSliceLabel.setPreferredSize(INFOTAG_SIZE);
		phiDiffSlicing.add(phiDiffSliceLabel);
		phiDiffSliceLabel.addActionListener(
				a -> switchSliceMode(SLICEMODE_PHIDIFF));
		buttonGroup.add(phiDiffSliceLabel);

		phiDiffField.addChangeListener(a -> {
			if(host != null) host.broadcastAngleUpdate(null, 
					thetaHalf, thetaDiff, phiDiffField.getValue());
		});
		phiDiffSlicing.add(phiDiffField);
		
		// The lower information panel.
		JPanel informationPanel = new JPanel();
		informationPanel.setPreferredSize(
				new Dimension(220, 160));
		FlowLayout informationLayout = new FlowLayout();
		informationLayout.setVgap(0);
		informationPanel.setLayout(informationLayout);
		eastPanel.add(informationPanel, BorderLayout.SOUTH);
		
		// Insert the sample boxes.
		informationPanel.add(colorSample);
		
		// Insert the granularity option.
		JPanel granularity = new JPanel();
		JLabel granularityLabel = new JLabel("Point Size");
		granularityLabel.setPreferredSize(INFOTAG_SIZE);
		granularityLabel.setHorizontalAlignment(JLabel.RIGHT);
		granularity.add(granularityLabel);

		JSpinner granularityInput = new JSpinner(
				new SpinnerNumberModel(3, 1, 10, 1));
		granularityInput.addChangeListener(l -> 
			setGranularity((Integer)granularityInput.getValue()));
		granularityInput.setPreferredSize(INPUT_SIZE);
		granularity.add(granularityInput);
		informationPanel.add(granularity);
		
		smoothDisplay.setSelected(false);
		smoothDisplay.addActionListener(c -> 
			{ sliceImage = null; repaint(); } );
		informationPanel.add(smoothDisplay);
		
		// Select the slice mode.
		thetaHalfSliceLabel.setSelected(true);
		switchSliceMode(SLICEMODE_THETAHALF);
		updateAngle(this, 0, 0, Math.PI * 0.5);
	}
	
	private int getXSpan() {
		return sliceMode == SLICEMODE_PHIDIFF?
				host.getData().dimThetaDiff : host.getData().dimPhiDiff;
	}
	
	private int getYSpan() {
		return sliceMode == SLICEMODE_THETAHALF?
				host.getData().dimThetaDiff : host.getData().dimThetaHalf;
	}
	
	private Dimension recalculate() {
		if(host == null) return null;
		if(host.getData() == null) return null;
		
		return new Dimension(
				granularity * getXSpan(), 
				granularity * getYSpan());
	}
	
	public void setGranularity(int size) {
		if(size < 1) size = 1;
		if(size > 10) size = 10;
		this.granularity = size;
		this.displayComponent.setPreferredSize(recalculate());
		this.displayScroll.updateUI();
		repaint();
	}

	@Override
	public void updateData(BRDFData data) {
		this.sliceImage = null;
		
		this.strideThetaHalf = 0.5 * Math.PI / data.dimThetaHalf;
		this.strideThetaDiff = 0.5 * Math.PI / data.dimThetaDiff;
		this.stridePhiDiff = Math.PI / data.dimPhiDiff;
		
		this.displayComponent
			.setPreferredSize(recalculate());
		this.displayScroll.updateUI();
		repaint();
	}

	@Override
	public Component getComponent() {
		return this;
	}
	
	public void switchSliceMode(int mode) {
		this.sliceMode = mode;
		this.thetaHalfField.setEnabled(
				mode == SLICEMODE_THETAHALF);
		this.thetaDiffField.setEnabled(
				mode == SLICEMODE_THETADIFF);
		this.phiDiffField.setEnabled(
				mode == SLICEMODE_PHIDIFF);
		
		sliceImage = null;
		this.displayComponent
			.setPreferredSize(recalculate());
		this.displayScroll.updateUI();
		repaint();
	}

	@Override
	public void updateAngle(BRDFPerspective perspective,
			double thetaHalf, double thetaDiff, double phiDiff) {
		
		this.thetaHalfField.setValue(
				this.thetaHalf = thetaHalf);
		this.thetaDiffField.setValue(
				this.thetaDiff = thetaDiff);
		this.phiDiffField.setValue(
				this.phiDiff = phiDiff);
		
		if(isVisible()) repaint();
	}
	
	public void repaint() {
		super.repaint();
		if(initializedObject == null) return;
		
		BRDFVector3d sample = null;
		if(host != null && host.getData() != null) {
			sample = new BRDFVector3d();
			host.getData().fetch(thetaHalf, thetaDiff, phiDiff, sample);
		}
		colorSample.setColorSample(sample);
	}

	private BRDFHost host;
	@Override
	public void setHost(BRDFHost host) {
		this.host = host;
	}
	
	private void fetchTuple(double i, double j, BRDFVector3d tuple) {
		switch(sliceMode) {
			case SLICEMODE_THETAHALF:
				host.getData().fetch(thetaHalf, j * strideThetaDiff, 
						i * stridePhiDiff, tuple);
				break;
				
			case SLICEMODE_THETADIFF:
				host.getData().fetch(j * strideThetaHalf, thetaDiff, 
						i * stridePhiDiff, tuple);
				break;
				
			case SLICEMODE_PHIDIFF:
				host.getData().fetch(j * strideThetaHalf, 
						i * strideThetaDiff, phiDiff, tuple);
				break;
		}
		tuple.clamp();
	}
	
	private void notifySliceInput(double xStride, double yStride) {
		switch(sliceMode) {
			case SLICEMODE_THETAHALF:
				host.broadcastAngleUpdate(null, thetaHalf, 
						yStride * Math.PI * 0.5, xStride * Math.PI);
				break;
				
			case SLICEMODE_THETADIFF:
				host.broadcastAngleUpdate(null, yStride * Math.PI * 0.5, 
						thetaDiff, xStride * Math.PI);
				break;
				
			case SLICEMODE_PHIDIFF:
				host.broadcastAngleUpdate(null, yStride * Math.PI * 0.5, 
						xStride * Math.PI * 0.5, phiDiff);
				break;
		}
	}
}
