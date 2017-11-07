package net.aegistudio.brdfviewer;

import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;

public class JDegreeField extends JPanel {
	private static final long serialVersionUID = 1L;

	protected final double max;
	protected final JSpinner spinner;
	protected final JLabel output;
	
	public JDegreeField(double max, Dimension inputSize) {
		this.max = max;
		setBorder(null);
		setLayout(new GridLayout(0, 1));
		
		// The spinner input field.
		spinner = new JSpinner(new SpinnerNumberModel(
				0.0, 0.0, 0.999, 0.001));
		spinner.setPreferredSize(inputSize);
		add(spinner);
		
		// The value transforming field.
		output = new JLabel();
		output.setHorizontalAlignment(JLabel.RIGHT);
		add(output);
		updateOutput();
	}
	
	public void setValue(double value) {
		spinner.setValue(((int)1000.0 * value / max) / 1000.0);
		updateOutput();
		repaint();
	}
	
	public double getValue() {
		return ((Double)spinner.getValue()) * max;
	}
	
	protected void updateOutput() {
		output.setText(String.format(
				"<html>Degree: %.3f<br>Radian: %.3f</html>", 
				getValue() * 180.0 / Math.PI, getValue()));
	}
	
	public void setEnabled(boolean enabled) {
		spinner.setEnabled(enabled);
	}
	
	public void addChangeListener(ChangeListener listener) {
		spinner.addChangeListener(listener);
	}
}
