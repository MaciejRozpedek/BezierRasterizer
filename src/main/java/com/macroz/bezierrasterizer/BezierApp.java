package com.macroz.bezierrasterizer;

import com.macroz.bezierrasterizer.logic.MeshGenerator;
import com.macroz.bezierrasterizer.logic.SceneTransformer;
import com.macroz.bezierrasterizer.logic.Rasterizer;
import com.macroz.bezierrasterizer.model.Mesh;
import com.macroz.bezierrasterizer.model.Triangle;
import com.macroz.bezierrasterizer.model.Vertex;
import org.joml.Vector3f;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class BezierApp extends JFrame {

	private final Mesh mesh;
	private final CanvasPanel canvasPanel;
	private final Rasterizer rasterizer;

	// Simulation parameters
	private int precision = 10;
	private int alpha = 0; // Z rotation
	private int beta = 0;  // X rotation

	// UI State
	private boolean showPolygon = true;
	private boolean showMesh = true;
	private boolean showFill = false;
	private boolean useTexture = false;
	private boolean useNormalMap = false;
	private Color selectedColor = new Color(100, 100, 255); // Default object color

	// Lighting parameters
	private float kd = 0.6f;
	private float ks = 0.4f;
	private float m = 20.0f;
	private float lightZ = 200.0f;

	public BezierApp() {
		super("Bezier Surface Renderer - L-Z");
		this.mesh = new Mesh();

		initDefaultControlPoints();
		
		// Initial generation and transformation
		MeshGenerator.triangulate(mesh, precision);
		SceneTransformer.transform(mesh, alpha, beta);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1200, 900);
		setLayout(new BorderLayout());
		rasterizer = new Rasterizer(800, 600);
		rasterizer.setObjectColor(selectedColor);

		canvasPanel = new CanvasPanel();
		add(canvasPanel, BorderLayout.CENTER);

		JPanel controlPanel = createControlPanel();
		add(controlPanel, BorderLayout.EAST);

		setLocationRelativeTo(null);
		setVisible(true);
	}

	private void initDefaultControlPoints() {
		mesh.setControlPoint(0, 0, -200, -200, 50);
		mesh.setControlPoint(0, 1, -100, -70, -30);
		mesh.setControlPoint(0, 2, -300, 80, 10);
		mesh.setControlPoint(0, 3, -190, 200, 100);

		mesh.setControlPoint(1, 0, -80, -80, 80);
		mesh.setControlPoint(1, 1, 40, -20, 150);
		mesh.setControlPoint(1, 2, -160, 120, 20);
		mesh.setControlPoint(1, 3, -90, 300, -50);

		mesh.setControlPoint(2, 0, 70, -300, 10);
		mesh.setControlPoint(2, 1, 180, -100, 100);
		mesh.setControlPoint(2, 2, -40, 0, -100);
		mesh.setControlPoint(2, 3, 110, 100, 0);

		mesh.setControlPoint(3, 0, 180, -190, -20);
		mesh.setControlPoint(3, 1, 320, -80, 50);
		mesh.setControlPoint(3, 2, 80, 60, 30);
		mesh.setControlPoint(3, 3, 200, 200, 70);
	}

	private void updateTransform() {
		SceneTransformer.transform(mesh, alpha, beta);
		canvasPanel.repaint();
	}

	private JPanel createControlPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setPreferredSize(new Dimension(300, 0));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		panel.add(new JLabel("Triangulation Precision:"));
		JSlider precisionSlider = new JSlider(2, 60, precision);
		precisionSlider.setMinorTickSpacing(1);
		precisionSlider.setMajorTickSpacing(10);
		precisionSlider.setPaintTicks(true);
		precisionSlider.setPaintLabels(true);
		precisionSlider.addChangeListener(e -> {
			if (!precisionSlider.getValueIsAdjusting()) {
				precision = precisionSlider.getValue();
				MeshGenerator.triangulate(mesh, precision);
				updateTransform();
			}
		});
		panel.add(precisionSlider);
		panel.add(Box.createVerticalStrut(20));

		panel.add(new JLabel("Alpha (Z-Axis):"));
		JSlider alphaSlider = new JSlider(-180, 180, alpha);
		alphaSlider.setMajorTickSpacing(45);
		alphaSlider.setPaintTicks(true);
		alphaSlider.setPaintLabels(true);
		alphaSlider.addChangeListener(e -> {
			alpha = alphaSlider.getValue();
			updateTransform();
		});
		panel.add(alphaSlider);

		panel.add(new JLabel("Beta (X-Axis):"));
		JSlider betaSlider = new JSlider(-180, 180, beta);
		betaSlider.setMajorTickSpacing(45);
		betaSlider.setPaintTicks(true);
		betaSlider.setPaintLabels(true);
		betaSlider.addChangeListener(e -> {
			beta = betaSlider.getValue();
			updateTransform();
		});
		panel.add(betaSlider);
		panel.add(Box.createVerticalStrut(20));

		panel.add(new JSeparator());
		panel.add(new JLabel("Lighting (Phong):"));
		panel.add(Box.createVerticalStrut(20));

		panel.add(new JLabel("Kd (Diffuse):"));
		JSlider kdSlider = new JSlider(0, 100, (int)(kd * 100));
		kdSlider.setMajorTickSpacing(20);
		kdSlider.setPaintTicks(true);
		kdSlider.setPaintLabels(true);
		kdSlider.addChangeListener(e -> {
			kd = kdSlider.getValue() / 100.0f;
			canvasPanel.repaint();
		});
		panel.add(kdSlider);
		panel.add(Box.createVerticalStrut(20));

		panel.add(new JLabel("Ks (Specular):"));
		JSlider ksSlider = new JSlider(0, 100, (int)(ks * 100));
		ksSlider.setMajorTickSpacing(20);
		ksSlider.setPaintTicks(true);
		ksSlider.setPaintLabels(true);
		ksSlider.addChangeListener(e -> {
			ks = ksSlider.getValue() / 100.0f;
			canvasPanel.repaint();
		});
		panel.add(ksSlider);
		panel.add(Box.createVerticalStrut(20));

		panel.add(new JLabel("m (Shininess):"));
		JSlider mSlider = new JSlider(1, 100, (int)m);
		mSlider.setMajorTickSpacing(20);
		mSlider.setPaintTicks(true);
		mSlider.setPaintLabels(true);
		mSlider.addChangeListener(e -> {
			m = (float) mSlider.getValue();
			canvasPanel.repaint();
		});
		panel.add(mSlider);
		panel.add(Box.createVerticalStrut(20));

		panel.add(new JLabel("Light Z:"));
		JSlider lightZSlider = new JSlider(50, 500, (int)lightZ);
		lightZSlider.setMajorTickSpacing(100);
		lightZSlider.setPaintTicks(true);
		lightZSlider.setPaintLabels(true);
		lightZSlider.addChangeListener(e -> {
			lightZ = (float) lightZSlider.getValue();
			canvasPanel.repaint();
		});
		panel.add(lightZSlider);

		panel.add(Box.createVerticalStrut(20));
		panel.add(new JSeparator());

		panel.add(new JLabel("Object Material:"));
		panel.add(Box.createVerticalStrut(10));
		JButton btnColor = new JButton("Select Color");
		btnColor.addActionListener(e -> {
			Color c = JColorChooser.showDialog(this, "Choose Object Color", selectedColor);
			if (c != null) {
				selectedColor = c;
				rasterizer.setObjectColor(c);
				canvasPanel.repaint();
			}
		});
		panel.add(btnColor);

		JCheckBox chkTexture = new JCheckBox("Use Texture", useTexture);
		chkTexture.addActionListener(e -> {
			useTexture = chkTexture.isSelected();
			rasterizer.setUseTexture(useTexture);
			canvasPanel.repaint();
		});
		panel.add(chkTexture);

		JButton btnLoadTex = new JButton("Load Texture...");
		btnLoadTex.addActionListener(e -> loadTextureAction());
		panel.add(btnLoadTex);

		JCheckBox chkNormal = new JCheckBox("Use Normal Map", useNormalMap);
		chkNormal.addActionListener(e -> {
			useNormalMap = chkNormal.isSelected();
			rasterizer.setUseNormalMap(useNormalMap);
			canvasPanel.repaint();
		});
		panel.add(chkNormal);

		JButton btnLoadNorm = new JButton("Load Normal Map...");
		btnLoadNorm.addActionListener(e -> loadNormalMapAction());
		panel.add(btnLoadNorm);

		panel.add(Box.createVerticalStrut(10));
		panel.add(new JSeparator());

		panel.add(new JLabel("Display Options:"));
		JCheckBox chkPolygon = new JCheckBox("Show Bezier Polygon", showPolygon);
		chkPolygon.addActionListener(e -> {
			showPolygon = chkPolygon.isSelected();
			canvasPanel.repaint();
		});
		panel.add(chkPolygon);

		JCheckBox chkMesh = new JCheckBox("Show Triangle Mesh", showMesh);
		chkMesh.addActionListener(e -> {
			showMesh = chkMesh.isSelected();
			canvasPanel.repaint();
		});
		panel.add(chkMesh);

		JCheckBox chkFill = new JCheckBox("Show Filled Triangles", showFill);
		chkFill.addActionListener(e -> {
			showFill = chkFill.isSelected();
			canvasPanel.repaint();
		});
		panel.add(chkFill);

		return panel;
	}

	private void loadTextureAction() {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new FileNameExtensionFilter("Images", "jpg", "png", "bmp"));
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				BufferedImage img = ImageIO.read(chooser.getSelectedFile());
				rasterizer.setTexture(img);
				canvasPanel.repaint();
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(this, "Error loading texture: " + ex.getMessage());
			}
		}
	}

	private void loadNormalMapAction() {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new FileNameExtensionFilter("Images", "jpg", "png", "bmp"));
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			try {
				BufferedImage img = ImageIO.read(chooser.getSelectedFile());
				rasterizer.setNormalMap(img);
				canvasPanel.repaint();
			} catch (IOException ex) {
				JOptionPane.showMessageDialog(this, "Error loading normal map: " + ex.getMessage());
			}
		}
	}


	private class CanvasPanel extends JPanel {
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);

			if (getWidth() != rasterizer.getImage().getWidth() ||
				getHeight() != rasterizer.getImage().getHeight()) {
				rasterizer.resize(getWidth(), getHeight());
			}

			Graphics2D g2d = (Graphics2D) g;

			if (showFill) {
				rasterizer.setLightingParams(kd, ks, m, lightZ);
				rasterizer.setLightPosition(0, 0); // Placeholder: animation not yet implemented
				rasterizer.setUseTexture(useTexture);
				rasterizer.setUseNormalMap(useNormalMap);

				rasterizer.clear();
				rasterizer.render(mesh.getTriangles());
				g2d.drawImage(rasterizer.getImage(), 0, 0, null);
			} else {
				g2d.setColor(Color.LIGHT_GRAY);
				g2d.fillRect(0, 0, getWidth(), getHeight());
			}

			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			// Coordinate System Transformation
			g2d.translate(getWidth() / 2, getHeight() / 2);
			g2d.scale(1, -1);

			drawAxes(g2d);

			if (showPolygon) {
				drawControlPolygon(g2d);
			}

			if (showMesh) {
				drawMeshWireframe(g2d);
			}
		}

		private void drawAxes(Graphics2D g2d) {
			g2d.setStroke(new BasicStroke(2));
			g2d.setColor(Color.RED);
			g2d.drawLine(0, 0, 100, 0); // X
			g2d.setColor(Color.GREEN);
			g2d.drawLine(0, 0, 0, 100); // Y

			g2d.setColor(Color.BLUE);
			g2d.fillOval(-2, -2, 4, 4); // Z
		}

		private void drawControlPolygon(Graphics2D g2d) {
			g2d.setColor(new Color(0, 100, 0)); // Dark Green
			g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			Vector3f[] cps = mesh.getTransformedControlPoints();

			for (int i = 0; i < 4; i++) {
				Path2D path = new Path2D.Float();
				Vector3f start = cps[i * 4];
				path.moveTo(start.x, start.y);
				for (int j = 1; j < 4; j++) {
					Vector3f p = cps[i * 4 + j];
					path.lineTo(p.x, p.y);
				}
				g2d.draw(path);
			}

			for (int j = 0; j < 4; j++) {
				Path2D path = new Path2D.Float();
				Vector3f start = cps[j];
				path.moveTo(start.x, start.y);
				for (int i = 1; i < 4; i++) {
					Vector3f p = cps[i * 4 + j];
					path.lineTo(p.x, p.y);
				}
				g2d.draw(path);
			}

			g2d.setColor(Color.GREEN);
			for(Vector3f p : cps) {
				g2d.fillRect((int)p.x - 2, (int)p.y - 2, 5, 5);
			}
		}

		private void drawMeshWireframe(Graphics2D g2d) {
			g2d.setColor(Color.BLUE);
			g2d.setStroke(new BasicStroke(1));

			for (Triangle t : mesh.getTriangles()) {
				Vertex v1 = t.vertices[0];
				Vertex v2 = t.vertices[1];
				Vertex v3 = t.vertices[2];

				Path2D path = new Path2D.Float();
				path.moveTo(v1.transformedPosition.x, v1.transformedPosition.y);
				path.lineTo(v2.transformedPosition.x, v2.transformedPosition.y);
				path.lineTo(v3.transformedPosition.x, v3.transformedPosition.y);
				path.closePath();

				g2d.draw(path);
			}
		}
	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			// ignore
		}
		SwingUtilities.invokeLater(BezierApp::new);
	}
}