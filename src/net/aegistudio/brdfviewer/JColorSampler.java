package net.aegistudio.brdfviewer;

import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class JColorSampler extends JPanel {
	private static final long serialVersionUID = 1L;

	private final JTextField 
			redSample = new JTextField(), 
			greenSample = new JTextField(), 
			blueSample = new JTextField();

	public JColorSampler(Dimension infoTagSize, Dimension inputSize) {
		GridLayout layout = new GridLayout(0, 1);
		layout.setHgap(0);
		setLayout(layout);
		setBorder(null);
		
		// Insert the sample boxes.
		JPanel	redPanel = new JPanel(), 
				greenPanel = new JPanel(), 
				bluePanel = new JPanel();
		JPanel[] samplePanels = { redPanel, greenPanel, bluePanel };
		
		JLabel	redLabel = new JLabel("<html>\u03c1<sub>Red</sub></html>"),
				greenLabel = new JLabel("<html>\u03c1<sub>Green</sub></html>"),
				blueLabel = new JLabel("<html>\u03c1<sub>Blue</sub></html>");
		JLabel[] sampleLabels = { redLabel, greenLabel, blueLabel };
		
		JTextField[] sampleFields = { redSample, greenSample, blueSample };
		
		for(int i = 0; i < 3; ++ i) {
			samplePanels[i].setBorder(null);
			sampleLabels[i].setPreferredSize(infoTagSize);
			sampleFields[i].setPreferredSize(inputSize);
			sampleLabels[i].setHorizontalAlignment(JLabel.RIGHT);
			sampleFields[i].setHorizontalAlignment(JTextField.RIGHT);
			sampleFields[i].setEnabled(false);
			samplePanels[i].add(sampleLabels[i]);
			samplePanels[i].add(sampleFields[i]);
			add(samplePanels[i]);
		}
	}
	
	public void setColorSample(BRDFVector3d sample) {
		if(sample == null) {
			redSample.setText("");
			greenSample.setText("");
			blueSample.setText("");
		}
		else {
			redSample.setText(String.format("%.3f", sample.x));
			greenSample.setText(String.format("%.3f", sample.y));
			blueSample.setText(String.format("%.3f", sample.z));
		}
	}
}
