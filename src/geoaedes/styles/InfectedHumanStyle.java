package geoaedes.styles;

import java.awt.Color;

import geoaedes.agents.InfectedHumanAgent;
import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwind.render.SurfaceShape;
import repast.simphony.visualization.gis3D.style.SurfaceShapeStyle;

public class InfectedHumanStyle implements SurfaceShapeStyle<InfectedHumanAgent>{
	
	@Override
	public SurfaceShape getSurfaceShape(InfectedHumanAgent object, SurfaceShape shape) {
		// SurfaceShape is null on first call.
		if (shape == null) {
			shape = new SurfacePolygon();
		}
		return shape;
	}

	@Override
	public Color getFillColor(InfectedHumanAgent obj) {
		return obj.getColor();
	}

	@Override
	public double getFillOpacity(InfectedHumanAgent obj) {
		return .5d;
	}

	@Override
	public Color getLineColor(InfectedHumanAgent obj) {
		return Color.BLACK;
	}

	@Override
	public double getLineOpacity(InfectedHumanAgent obj) {
		return .5d;
	}

	@Override
	public double getLineWidth(InfectedHumanAgent obj) {
		return 1d;
	}

}
