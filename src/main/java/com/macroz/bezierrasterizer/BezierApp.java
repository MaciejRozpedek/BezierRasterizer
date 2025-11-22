package com.macroz.bezierrasterizer;

import com.macroz.bezierrasterizer.logic.MeshGenerator;
import com.macroz.bezierrasterizer.model.Mesh;
import com.macroz.bezierrasterizer.model.Triangle;
import com.macroz.bezierrasterizer.model.Vertex;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

public class BezierApp extends JFrame {

	private final Mesh mesh;
	private final CanvasPanel canvasPanel;

	private int precision = 10;

	public BezierApp() {
		super("Bezier Surface Renderer - L-Z");
		this.mesh = new Mesh();

		// 1. Initialize default control points (to ensure something is visible initially)
		initDefaultControlPoints();

		// 2. Generate the mesh
		MeshGenerator.triangulate(mesh, precision);
		updateTransformedVertices(); // Update vertex positions (no rotation applied yet)

		// 3. Configure the GUI
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1000, 800);
		setLayout(new BorderLayout());

		// Drawing panel (center)
		canvasPanel = new CanvasPanel();
		add(canvasPanel, BorderLayout.CENTER);

		// Control panel (right side)
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
	
	private void updateTransformedVertices() {
		for (Triangle t : mesh.getTriangles()) {
			for (Vertex v : t.vertices) {
				v.resetTransformed();
			}
		}
	}

	private JPanel createControlPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setPreferredSize(new Dimension(250, 0));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		panel.add(new JLabel("Dokładność triangulacji:"));
		JSlider precisionSlider = new JSlider(2, 100, precision);
		precisionSlider.setMajorTickSpacing(10);
		precisionSlider.setPaintTicks(true);
		precisionSlider.setPaintLabels(true);
		precisionSlider.addChangeListener(e -> {
			if (!precisionSlider.getValueIsAdjusting()) {
				precision = precisionSlider.getValue();
				MeshGenerator.triangulate(mesh, precision);
				updateTransformedVertices();
				canvasPanel.repaint();
			}
		});
		panel.add(precisionSlider);

		return panel;
	}

	private class CanvasPanel extends JPanel {
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;

			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			g2d.setColor(Color.LIGHT_GRAY);
			g2d.fillRect(0, 0, getWidth(), getHeight());

			// Transformation of the coordinate system
			g2d.translate(getWidth() / 2, getHeight() / 2);
			g2d.scale(1, -1);

			drawAxes(g2d);

			// Drawing the mesh
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

		private void drawAxes(Graphics2D g2d) {
			g2d.setStroke(new BasicStroke(2));
			// X axis (Red)
			g2d.setColor(Color.RED);
			g2d.drawLine(0, 0, 100, 0);
			// Y axis (Green)
			g2d.setColor(Color.GREEN);
			g2d.drawLine(0, 0, 0, 100);
		}
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(BezierApp::new);
	}
}