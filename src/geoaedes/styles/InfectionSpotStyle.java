package geoaedes.styles;

import java.awt.Color;

import geoaedes.agents.InfectionSpotAgent;
import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwind.render.SurfaceShape;
import repast.simphony.visualization.gis3D.style.SurfaceShapeStyle;

public class InfectionSpotStyle implements SurfaceShapeStyle<InfectionSpotAgent>{

	private double opacity = 0d;
	
	@Override
	public SurfaceShape getSurfaceShape(InfectionSpotAgent object, SurfaceShape shape) {
		// SurfaceShape is null on first call.
		if (shape == null) {
			shape = new SurfacePolygon();
		}
		return shape;
	}

	@Override
	public Color getFillColor(InfectionSpotAgent obj) {
		return Color.RED;
	}

	@Override
	public double getFillOpacity(InfectionSpotAgent obj) {
		// Calculo opacity aca y se usa para LineOpacity tambien
		opacity = obj.getInfectedCount() / 5d;
		if (opacity > 1d)
			return 1d;
		return opacity;
	}

	@Override
	public Color getLineColor(InfectionSpotAgent obj) {
		return Color.BLACK;
	}

	@Override
	public double getLineOpacity(InfectionSpotAgent obj) {
		return opacity;
	}

	@Override
	public double getLineWidth(InfectionSpotAgent obj) {
		return 1d;
	}

}
