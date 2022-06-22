package geoaedes.agents;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Representa las parcelas donde los Humanos trabajan o estudian.
 */
public class WorkplaceAgent extends BuildingAgent {
	protected int[][] workPositions;
	protected int workPositionsCount;
	protected String workplaceType;
	protected int activityType;
	/** Cantidad maxima de trabajadores en lugar de trabajo */
	protected int vacancies = 4;
	/** Capacidad maxima (metros util * humanos por metro cuadrado) */
	private int maximumCapacity;
	
	public WorkplaceAgent(int sectoralIndex, Coordinate coord, int id, int blockId, String workType, int activityType, int area, int coveredArea, int workersPlace, int workersArea) {
		super(sectoralIndex, coord, id, blockId, 'E', workType, area, (area * coveredArea) / 100, true);
		
		this.workplaceType = workType;
		this.activityType = activityType;
		if (workersPlace > 0)
			this.vacancies = workersPlace;
		else if (workersArea > 0)
			this.vacancies = (getNumberOfSpots() / workersArea)+1;
		else
			System.err.println("Sin cupo de trabajadores de Workplace: " + workplaceType);
	}
	
	public int getVacancy() {
		return vacancies;
	}
	
	public boolean vacancyAvailable() {
		return (vacancies > 0);
	}
	
	public void reduceVacancies() {
		--this.vacancies;
	}
	
	public int getMaximumCapacity() {
		return maximumCapacity;
	}
	
	public void setMaximumCapacity(int maximumCapacity) {
		if (getCapacity() > maximumCapacity)
			setCapacity(maximumCapacity);
		this.maximumCapacity = maximumCapacity;
	}
}
