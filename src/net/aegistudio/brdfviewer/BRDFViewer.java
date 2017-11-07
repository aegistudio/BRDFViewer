package net.aegistudio.brdfviewer;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import net.aegistudio.brdfviewer.BRDFPerspective.BRDFHost;

public class BRDFViewer extends JFrame implements BRDFHost {
	private static final long serialVersionUID = 1L;
	private static final String lookAndFeelID = 
			"com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel";
	
	private BRDFData brdfData = null;
	private BRDFSlice brdfSlice = new BRDFSlice();
	private BRDFRenderer brdfPlane = new BRDFRenderer(new BRDFPlaneRender());
	private BRDFPerspective[] brdfPerspectives = { brdfSlice, brdfPlane };

	private JTextField brdfFileName = new JTextField("(None)");
	private String brdfProgressAction = null;
	private JProgressBar brdfLoadProgress = new JProgressBar() {
		private static final long serialVersionUID = 1L;

		public void paint(Graphics g) {
			super.paint(g);
			synchronized(BRDFViewer.this) {
				if(brdfProgressAction != null) {
					g.setColor(Color.BLACK);
					Rectangle2D border = g.getFontMetrics()
							.getStringBounds(brdfProgressAction, g);
					g.drawString(brdfProgressAction, 
							(int)((getWidth() - border.getWidth()) * 0.5), 
							(int)((getHeight() + border.getHeight()) * 0.5));
				}
			}
		}
	};
	
	public BRDFViewer(String title) {
		super(title);
		for(BRDFPerspective brdfPerspective : brdfPerspectives) 
			brdfPerspective.setHost(this);
		
		JTabbedPane tabbedPanel = new JTabbedPane();
		add(tabbedPanel, BorderLayout.CENTER);
		
		// Insert the function slice into the tabbed pane.
		tabbedPanel.add(brdfSlice, "Function Slice");
		tabbedPanel.add(brdfPlane, "Plane Render");
		
		// Insert the lower state pane.
		JPanel lowerPanel = new JPanel();
		lowerPanel.setLayout(new BorderLayout());
		add(lowerPanel, BorderLayout.SOUTH);
		
		// Insert the option panel.
		JPanel optionPanel = new JPanel();
		FlowLayout optionLayout = new FlowLayout();
		optionLayout.setVgap(0);
		optionPanel.setLayout(optionLayout);
		optionPanel.setBorder(null);
		lowerPanel.add(optionPanel, BorderLayout.WEST);
		
		// Insert the open file option.
		JButton openFile = new JButton("Open BRDF...");
		openFile.addActionListener(a -> {
			// Prepare for new file choosing operation.
			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileFilter(new FileFilter() {
				@Override
				public boolean accept(File f) {
					return f.isDirectory() 
						|| f.getName().endsWith(".binary")
						|| f.getName().endsWith(".binary.ioc");
				}

				@Override
				public String getDescription() {
					return "MERL-BRDF file (*.binary; *.binary.ioc)";
				}
			});
			
			// Pop-up file choosing dialog for input.
			if(fileChooser.showOpenDialog(BRDFViewer.this) 
					== JFileChooser.APPROVE_OPTION && 
				fileChooser.getSelectedFile() != null) 
					this.detachModalWork(null, "Loading file...", progress -> {
				
					// Perform file loading progress.
					try {
						// A MERL-BRDF file is selected.
						brdfData = BRDFData.open(fileChooser
								.getSelectedFile(), progress);
						for(BRDFPerspective perspective : brdfPerspectives)
							perspective.updateData(brdfData);
						repaint();
						
						String fileName = fileChooser.getSelectedFile().getName();
						brdfFileName.setText(fileName.substring(
								1 + fileName.lastIndexOf('/'), 
								fileName.length() - ".binary".length()));
					}
					catch(IOException e) {
						JOptionPane.showConfirmDialog(BRDFViewer.this, 
								"Cannot open specified file " + fileChooser
								.getSelectedFile().getName() + "!", "Error", 
								JOptionPane.YES_OPTION, JOptionPane.ERROR_MESSAGE);
					}
				});
		});
		optionPanel.add(openFile);
		
		brdfFileName.setEditable(false);
		brdfFileName.setPreferredSize(new Dimension(100, 27));
		optionPanel.add(brdfFileName);
		
		lowerPanel.add(brdfLoadProgress, BorderLayout.CENTER);
		lowerPanel.setPreferredSize(new Dimension(0, 27));
	}
	
	private void notifyLoad(int current, int total) {
		this.brdfLoadProgress.setValue(current);
		this.brdfLoadProgress.setMaximum(total);
	}
	
	public static void main(String[] arguments) {
		try {UIManager.setLookAndFeel(lookAndFeelID);}
		catch(Exception e) {}
		
		BRDFViewer brdfViewer = new BRDFViewer("BRDF Viewer");
		brdfViewer.setLocationRelativeTo(null);
		brdfViewer.setSize(new Dimension(830, 500));
		brdfViewer.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		brdfViewer.setVisible(true);
	}

	@Override
	public void broadcastAngleUpdate(BRDFPerspective thiz, 
			double thetaHalf, double thetaDiff, double phiDiff) {
	
		for(BRDFPerspective perspective : brdfPerspectives) 
			perspective.updateAngle(thiz, thetaHalf, thetaDiff, phiDiff);
	}

	@Override
	public void detachModalWork(BRDFPerspective thiz, String tips,
			Consumer<BiConsumer<Integer, Integer>> progressNotifierCallback) {
		
		//synchronized(modalWorkLock) {}
		// Turn the UI into modal state before the work.
		this.setEnabled(false);
		
		new Thread(() -> {
			synchronized(BRDFViewer.this) {
				brdfProgressAction = tips;
			}
			
			//synchronized(modalWorkLock) {
				try {
					progressNotifierCallback.accept(this::notifyLoad);
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			//}
			
			// Reset modal state after the work.
			synchronized(BRDFViewer.this) {
				brdfProgressAction = null;
			}
			BRDFViewer.this.setEnabled(true);
			BRDFViewer.this.repaint();
		}).start();
	}

	@Override
	public BRDFData getData() {
		return this.brdfData;
	}
}
