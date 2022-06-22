package geoaedes.agents;

import java.awt.Color;
import java.util.Random;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Esta clase se utiliza para representar en mapa la ubicacion del infectado.
 */
public class InfectedHumanAgent {
	
	private int humanAgentID;
	private Coordinate currentCoordinate;
	private Color randomColor;
	private boolean hidden = false;

	public InfectedHumanAgent(int agentID, Coordinate coordinate) {
		this.humanAgentID = agentID;
		this.currentCoordinate = coordinate;
		this.initRandomColor();
	}
	
	private void initRandomColor() {
		final Random rand = new Random();
		final float r = rand.nextFloat() / 2f + 0.5f;
		final float g = rand.nextFloat() / 2f + 0.5f;
		final float b = rand.nextFloat() / 2f + 0.5f;
		this.randomColor = new Color(r, g, b);
	}

	public int getAgentID() {
		return humanAgentID;
	}
	
	public Coordinate getCurrentCoordinate() {
		return currentCoordinate;
	}

	public void setCurrentCoordinate(Coordinate currentCoordinate) {
		this.currentCoordinate = currentCoordinate;
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}
	
	public Color getColor() {
		return randomColor;
	}
}
