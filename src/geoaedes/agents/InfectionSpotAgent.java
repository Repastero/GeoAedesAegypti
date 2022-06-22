package geoaedes.agents;

/**
 * Esta clase se utiliza para representar en mapa la ubicacion del hogar del infectado.
 */
public class InfectionSpotAgent {
	
	private int buildingId;
	private int infectedCount;

	public InfectionSpotAgent(int id, int infected) {
		this.buildingId = id;
		this.infectedCount = infected;
	}

	public int getBuildingId() {
		return buildingId;
	}

	public int getInfectedCount() {
		return infectedCount;
	}

	public void addInfected() {
		++this.infectedCount;
	}
	
	public void removeInfected() {
		--this.infectedCount;
	}
}
