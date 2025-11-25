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
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class BezierApp extends JFrame {

	private final Mesh mesh;
	private final CanvasPanel canvasPanel;
	private final Rasterizer rasterizer;
	private final Timer animationTimer;

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
	private Color selectedLightColor = Color.WHITE;

	// Lighting parameters
	private float kd = 0.6f;
	private float ks = 0.4f;
	private float m = 20.0f;
	private float lightZ = 200.0f;

	private enum LightMode {
		STATIC, ANIMATION, MULTI_16
	}

	private LightMode currentLightMode = LightMode.STATIC;

	// Animation state
	private float animAngle = 0.0f;

	// CP Animation state
	private boolean isAnimatingCP = false;
	private float animCpAngle = 0.0f;
	private float originalCpZ = 0.0f;

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
		rasterizer.setLightColor(selectedLightColor);

		rasterizer.setLightPosition(0, 0);

		loadDefaultTexture();
		useTexture = false;
		rasterizer.setUseTexture(useTexture);

		canvasPanel = new CanvasPanel();
		add(canvasPanel, BorderLayout.CENTER);

		JPanel controlPanel = createControlPanel();
		JScrollPane scrollPane = new JScrollPane(controlPanel);
		scrollPane.setPreferredSize(new Dimension(300, 0));
		add(scrollPane, BorderLayout.EAST);

		// Setup Animation Timer (~ 60 FPS)
		animationTimer = new Timer(16, e -> updateAnimation());

		setLocationRelativeTo(null);
		setVisible(true);
	}

	private void loadDefaultTexture() {
		try {
			BufferedImage img = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/monkey.jpg")));
			if (img != null) {
				rasterizer.setTexture(img);
				useTexture = true;
				rasterizer.setUseTexture(true);
			}
		} catch (Exception e) {
			System.err.println("Could not load default texture 'monkey.jpg': " + e.getMessage());
		}

		try {
			BufferedImage normImg = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/normal_map.jpg")));
			if (normImg != null) {
				rasterizer.setNormalMap(normImg);
				useNormalMap = true;
				rasterizer.setUseNormalMap(true);
			}
		} catch (Exception e) {
			System.err.println("Could not load default normal map 'normal_map.jpg': " + e.getMessage());
		}
	}

	private void updateAnimation() {
		boolean needsRepaint = false;

		if (currentLightMode == LightMode.ANIMATION) {
			animAngle += 0.05f;
			float radius = 200 + 50 * (float) Math.sin(animAngle * 0.3f);
			float lx = radius * (float) Math.cos(animAngle);
			float ly = radius * (float) Math.sin(animAngle);
			rasterizer.setLightPosition(lx, ly);
			needsRepaint = true;
		}

		if (isAnimatingCP) {
			animCpAngle += 0.1f;
			float newZ = originalCpZ + 200.0f * (float) Math.sin(animCpAngle);

			Vector3f p0 = mesh.getControlPoints()[0];
			mesh.setControlPoint(0, 0, p0.x, p0.y, newZ);

			MeshGenerator.triangulate(mesh, precision);
			SceneTransformer.transform(mesh, alpha, beta);
			needsRepaint = true;
		}

		if (needsRepaint) {
			canvasPanel.repaint();
		}
	}

	private void checkTimerState() {
		boolean animateLight = (currentLightMode == LightMode.ANIMATION);
		if (animateLight || isAnimatingCP) {
			if (!animationTimer.isRunning()) animationTimer.start();
		} else {
			animationTimer.stop();
		}
	}

	private void setupLightMode(LightMode mode) {
		currentLightMode = mode;
		switch (mode) {
			case STATIC:
				rasterizer.setLightPosition(0, 0);
				canvasPanel.repaint();
				break;
			case ANIMATION:
				// handled in updateAnimation
				break;
			case MULTI_16:
				List<Vector3f> gridLights = new ArrayList<>();
				// Grid 4x4 from -300 to 300
				for (int i = 0; i < 4; i++) {
					for (int j = 0; j < 4; j++) {
						float x = -300 + i * 200;
						float y = -300 + j * 200;
						gridLights.add(new Vector3f(x, y, 0));
					}
				}
				rasterizer.setMultiLights(gridLights);
				canvasPanel.repaint();
				break;
		}
		checkTimerState();
	}

	private void initDefaultControlPoints() {
		mesh.setControlPoint(0, 0, -200, -200, 50);
		mesh.setControlPoint(0, 1, -200, -67, 50);
		mesh.setControlPoint(0, 2, -200, 67, -50);
		mesh.setControlPoint(0, 3, -200, 200, -175);

		mesh.setControlPoint(1, 0, -67, -200, -125);
		mesh.setControlPoint(1, 1, -67, -67, -175);
		mesh.setControlPoint(1, 2, -67, 67, -100);
		mesh.setControlPoint(1, 3, -67, 200, 75);

		mesh.setControlPoint(2, 0, 67, -200, -125);
		mesh.setControlPoint(2, 1, 67, -67, -175);
		mesh.setControlPoint(2, 2, 67, 67, -100);
		mesh.setControlPoint(2, 3, 67, 200, 75);

		mesh.setControlPoint(3, 0, 200, -200, 50);
		mesh.setControlPoint(3, 1, 200, -67, 50);
		mesh.setControlPoint(3, 2, 200, 67, -50);
		mesh.setControlPoint(3, 3, 200, 200, -175);

		this.originalCpZ = mesh.getControlPoints()[0].z;
	}

	private void updateTransform() {
		SceneTransformer.transform(mesh, alpha, beta);
		canvasPanel.repaint();
	}

	private JPanel createControlPanel() {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		panel.add(new JLabel("Control Points:"));
		JButton btnLoadFile = new JButton("Load from File (.txt)");
		btnLoadFile.addActionListener(e -> loadControlPointsFromFile());
		panel.add(btnLoadFile);

		JCheckBox chkAnimCP = new JCheckBox("Animate Point (0,0) Z-Axis", isAnimatingCP);
		chkAnimCP.addActionListener(e -> {
			isAnimatingCP = chkAnimCP.isSelected();
			checkTimerState();
		});
		panel.add(chkAnimCP);

		panel.add(Box.createVerticalStrut(5));

		panel.add(new JLabel("Triangulation Precision:"));
		JSlider precisionSlider = new JSlider(2, 302, precision);
		precisionSlider.setMinorTickSpacing(1);
		precisionSlider.setMajorTickSpacing(50);
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
		panel.add(Box.createVerticalStrut(5));

		panel.add(new JLabel("Alpha (Z-Axis):"));
		JSlider alphaSlider = new JSlider(-180, 180, alpha);
		alphaSlider.setMajorTickSpacing(90);
		alphaSlider.setPaintTicks(true);
		alphaSlider.setPaintLabels(true);
		alphaSlider.addChangeListener(e -> {
			alpha = alphaSlider.getValue();
			updateTransform();
		});
		panel.add(alphaSlider);

		panel.add(new JLabel("Beta (X-Axis):"));
		JSlider betaSlider = new JSlider(-180, 180, beta);
		betaSlider.setMajorTickSpacing(90);
		betaSlider.setPaintTicks(true);
		betaSlider.setPaintLabels(true);
		betaSlider.addChangeListener(e -> {
			beta = betaSlider.getValue();
			updateTransform();
		});
		panel.add(betaSlider);
		panel.add(Box.createVerticalStrut(5));

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
		panel.add(Box.createVerticalStrut(5));

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
		panel.add(Box.createVerticalStrut(5));

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
		panel.add(Box.createVerticalStrut(5));

		panel.add(new JLabel("Light Z:"));
		JSlider lightZSlider = new JSlider(50, 500, (int)lightZ);
		lightZSlider.setMajorTickSpacing(100);
		lightZSlider.setPaintTicks(true);
		lightZSlider.setPaintLabels(true);
		lightZSlider.addChangeListener(e -> {
			lightZ = (float) lightZSlider.getValue();
			rasterizer.setLightingParams(kd, ks, m, lightZ);
			canvasPanel.repaint();
		});
		panel.add(lightZSlider);

		panel.add(Box.createVerticalStrut(5));
		panel.add(new JSeparator());

		JButton btnLightColor = new JButton("Select Light Color");
		btnLightColor.addActionListener(e -> {
			Color c = JColorChooser.showDialog(this, "Light Color", selectedLightColor);
			if (c != null) {
				selectedLightColor = c;
				rasterizer.setLightColor(c);
				canvasPanel.repaint();
			}
		});
		panel.add(btnLightColor);

		panel.add(new JLabel("Light Mode:"));
		JRadioButton rbStatic = new JRadioButton("Static (Center)", true);
		JRadioButton rbAnim = new JRadioButton("Animation (Spiral)", false);
		JRadioButton rbMulti = new JRadioButton("16 Lights (Grid)", false);

		ButtonGroup bgLight = new ButtonGroup();
		bgLight.add(rbStatic);
		bgLight.add(rbAnim);
		bgLight.add(rbMulti);

		rbStatic.addActionListener(e -> setupLightMode(LightMode.STATIC));
		rbAnim.addActionListener(e -> setupLightMode(LightMode.ANIMATION));
		rbMulti.addActionListener(e -> setupLightMode(LightMode.MULTI_16));

		panel.add(rbStatic);
		panel.add(rbAnim);
		panel.add(rbMulti);

		panel.add(new JSeparator());

		panel.add(new JLabel("Object Mode:"));
		JRadioButton rbSolid = new JRadioButton("Solid Color", !useTexture);
		JRadioButton rbTexture = new JRadioButton("Texture", useTexture);
		ButtonGroup bgMode = new ButtonGroup();
		bgMode.add(rbSolid);
		bgMode.add(rbTexture);

		rbSolid.addActionListener(e -> {
			useTexture = false;
			rasterizer.setUseTexture(false);
			canvasPanel.repaint();
		});
		rbTexture.addActionListener(e -> {
			useTexture = true;
			rasterizer.setUseTexture(true);
			canvasPanel.repaint();
		});

		panel.add(rbSolid);
		JButton btnColor = new JButton("Select Color");
		btnColor.addActionListener(e -> {
			Color c = JColorChooser.showDialog(this, "Choose Object Color", selectedColor);
			if (c != null) {
				selectedColor = c;
				rasterizer.setObjectColor(c);
				if(rbSolid.isSelected()) canvasPanel.repaint();
			}
		});
		panel.add(btnColor);

		panel.add(rbTexture);
		JButton btnLoadTex = new JButton("Load Texture...");
		btnLoadTex.addActionListener(e -> loadTextureAction());
		panel.add(btnLoadTex);

		panel.add(Box.createVerticalStrut(5));
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

		panel.add(Box.createVerticalStrut(5));
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

	private void loadControlPointsFromFile() {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt"));
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			File file = chooser.getSelectedFile();
			try (Scanner scanner = new Scanner(file).useLocale(Locale.US)) {
				for (int i = 0; i < 16; i++) {
					if (scanner.hasNextFloat()) {
						float x = scanner.nextFloat();
						float y = scanner.nextFloat();
						float z = scanner.nextFloat();
						mesh.setControlPoint(i / 4, i % 4, x, y, z);
					}
				}
				originalCpZ = mesh.getControlPoints()[0].z;
				MeshGenerator.triangulate(mesh, precision);
				updateTransform();
				JOptionPane.showMessageDialog(this, "Points loaded successfully!");
			} catch (Exception ex) {
				JOptionPane.showMessageDialog(this, "Error loading file: " + ex.getMessage());
			}
		}
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
				path.moveTo(cps[i * 4].x, cps[i * 4].y);
				for (int j = 1; j < 4; j++) {
					path.lineTo(cps[i * 4 + j].x, cps[i * 4 + j].y);
				}
				g2d.draw(path);
			}

			for (int j = 0; j < 4; j++) {
				Path2D path = new Path2D.Float();
				path.moveTo(cps[j].x, cps[j].y);
				for (int i = 1; i < 4; i++) {
					path.lineTo(cps[i * 4 + j].x, cps[i * 4 + j].y);
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