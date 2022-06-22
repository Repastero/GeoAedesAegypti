package geoaedes.styles;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.image.BufferedImage;

import geoaedes.agents.WorkplaceAgent;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.render.BasicWWTexture;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Offset;
import gov.nasa.worldwind.render.PatternFactory;
import gov.nasa.worldwind.render.WWTexture;
import repast.simphony.visualization.gis3D.PlaceMark;
import repast.simphony.visualization.gis3D.style.MarkStyle;

/**
 * Representacion grafica de WorkplaceAgents
 */
public class WorkspaceStyle implements MarkStyle<WorkplaceAgent>{
	
	private static WWTexture workplaceTexture = null;
	private static WWTexture greenPlaceTexture = null;
	
	public WorkspaceStyle() {
		/**
		 * Use of a map to store textures significantly reduces CPU and memory use
		 * since the same texture can be reused.  Textures can be created for different
		 * agent states and re-used when needed.
		 */
		BufferedImage image = PatternFactory.createPattern(PatternFactory.PATTERN_SQUARE, new Dimension(6, 6), .75f, Color.GREEN, Color.GREEN);
		greenPlaceTexture = new BasicWWTexture(image);
		image = PatternFactory.createPattern(PatternFactory.PATTERN_SQUARE, new Dimension(6, 6), .75f, new Color(0xFFFFFF), Color.BLUE);
		workplaceTexture = new BasicWWTexture(image);
	}
	
	@Override
	public double getLineWidth(WorkplaceAgent obj) {
		return 0;
	}

	@Override
	public WWTexture getTexture(WorkplaceAgent object, WWTexture texture) {
		// WWTexture is null on first call.
		if (object.isOutdoor())
			return greenPlaceTexture;
		else
			return workplaceTexture;
	}

	@Override
	public PlaceMark getPlaceMark(WorkplaceAgent object, PlaceMark mark) {
		// PlaceMark is null on first call.
		if (mark == null) {
			mark = new PlaceMark();
			/**
			 * The Altitude mode determines how the mark appears using the elevation.
			 *   WorldWind.ABSOLUTE places the mark at elevation relative to sea level
			 *   WorldWind.RELATIVE_TO_GROUND places the mark at elevation relative to ground elevation
			 *   WorldWind.CLAMP_TO_GROUND places the mark at ground elevation
			 */
			mark.setAltitudeMode(WorldWind.RELATIVE_TO_GROUND);
			mark.setLineEnabled(false);
		}
		return mark;
	}

	@Override
	public Offset getIconOffset(WorkplaceAgent obj) {
		return Offset.CENTER;
	}

	@Override
	public double getElevation(WorkplaceAgent obj) {
		return 0;
	}

	@Override
	public double getScale(WorkplaceAgent obj) {
		return 1d;
	}

	@Override
	public double getHeading(WorkplaceAgent obj) {
		return 0;
	}

	@Override
	public String getLabel(WorkplaceAgent obj) {
		return null;
	}

	@Override
	public Color getLabelColor(WorkplaceAgent obj) {
		return null;
	}

	@Override
	public Font getLabelFont(WorkplaceAgent obj) {
		return null;
	}

	@Override
	public Offset getLabelOffset(WorkplaceAgent obj) {
		return null;
	}

	@Override
	public Material getLineMaterial(WorkplaceAgent obj, Material lineMaterial) {
		return null;
	}
}
