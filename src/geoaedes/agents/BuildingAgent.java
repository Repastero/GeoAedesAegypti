package geoaedes.agents;

import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collections;

import com.vividsolutions.jts.geom.Coordinate;

import geoaedes.DataSet;
import repast.simphony.random.RandomHelper;

/**
 * Clase base de parcelas.
 */
public class BuildingAgent {
	// Atributos del GIS
	private int sectoralIndex;
	private Coordinate coordinate;
	private int id;
	private int block;
	private char state;
	private String type;
	private int area;
	private int coveredArea;
	// Atributos de dimensiones
	private int capacity;
	private boolean outdoor;
	/** Si el inmueble esta en un pizo elevado o area sin construir */
	private boolean mosquitoesExcluded = false;
	/** Para la grafica del SLD */
	private int mosquitoesAmount = 0;
	private int infectedHumans = 0;
	/** Lista de HumanAgent en el inmueble  */
	private List<HumanAgent> humansList = new ArrayList<HumanAgent>();
	/** Lista de ContainerAgent en el inmueble  */
	private List<ContainerAgent> containersList = new ArrayList<ContainerAgent>();
	
	private int indoorContainers = 0;
	private int outdoorContainers = 0;

	public BuildingAgent(int secIndex, Coordinate coord, int id, int blockId, char condition, String type, int area, int coveredArea) {
		this.sectoralIndex = secIndex;
		this.coordinate = coord;
		this.id = id;
		this.block = blockId;
		this.state = condition;
		this.type = type;
		this.area = area;
		this.coveredArea = coveredArea;
		
		setCapacity();
	}
	
	public BuildingAgent(int secIndex, Coordinate coord, int id, int blockId, char condition, String type, int area, int coveredArea, boolean isWorkplace) {
		// Constructor Home/Workplace
		this(secIndex, coord, id, blockId, condition, type, area, coveredArea);
	}
	
	private void setCapacity() {
		int realArea;
		// Si es espacio verde tomo toda el area
		if (coveredArea > 0) {
			realArea = coveredArea;
			outdoor = false;
			mosquitoesExcluded = (state == 'D'); // departamento
		}
		else {
			realArea = area;
			outdoor = true;
			mosquitoesExcluded = false;
		}
		capacity = realArea * DataSet.HUMANS_PER_SQUARE_METER;
	}
	
	/** @return <b>true</b> si NO se permiten mosquitos (acuaticos y adultos) en building. */
	public boolean isMosquitoesExcluded() {
		return mosquitoesExcluded;
	}
	
	/** @return cantidad de contenedores. */
	public int getAmountOfContainers() {
		return containersList.size();
	}
	
	/** @return HumanAgent en parcela. */
	public HumanAgent getRandomHuman() {
		return humansList.get(RandomHelper.nextIntFromTo(0, humansList.size()-1));
	}
	
	public boolean insertHuman(HumanAgent human) {
		if (humansList.size() >= capacity) { 
			return false;
		}
		humansList.add(human);
		if (human.isInfected())
			++infectedHumans;
		return true;
	}

	public void removeHuman(HumanAgent human) {
		humansList.remove(human);
		if (human.isInfected())
			--infectedHumans;
	}

	/** @return lista de ContainerAgent disponibles para oviposicion. */
	public List<ContainerAgent> getAvailableContainers() {
		return containersList.stream()
				.filter(ctnr -> ((ContainerAgent) ctnr).canCarryMoreEggs())
				.collect(Collectors.toList());
	}
	
	/**
	 * Mezcla y retorna los containers en el inmueble.
	 * @return lista de ContainerAgent
	 */
	public List<ContainerAgent> getShuffledContainers() {
		Collections.shuffle(containersList);
		return containersList;
	}
	
	public void insertContainer(ContainerAgent container) {
		containersList.add(container);
		if (container.isIndoors())
			++indoorContainers;
		else
			++outdoorContainers;
	}
	
	public void removeContainer(ContainerAgent container) {
		if (container.isIndoors())
			--indoorContainers;
		else
			--outdoorContainers;
		containersList.remove(container);
	}
	
	public int getIndoorCntrAmount() {
		return indoorContainers;
	}
	
	public int getOutdoorCntrAmount() {
		return outdoorContainers;
	}
	
	public int getMosquitoesAmount() {
		return mosquitoesAmount;
	}
	
	public void increaseMosquitoesAmount() {
		++mosquitoesAmount;
	}
	
	public void decreaseMosquitoesAmount() {
		--mosquitoesAmount;
	}
	
	public int getInfectedHumans() {
		return infectedHumans;
	}
	
	public void increaseInfectedHumans() {
		++infectedHumans;
	}
	
	public void decreaseInfectedHumans() {
		--infectedHumans;
	}
	
	public int getSectoralIndex() {
		return sectoralIndex;
	}
	
	public Coordinate getCoordinate() {
		return coordinate;
	}
	
	public int getId() {
		return id;
	}
	
	public int getBlock() {
		return block;
	}
	
	public String getType() {
		return type;
	}
	
	public double getArea() {
		return area;
	}
	
	public double getCoveredArea() {
		return coveredArea;
	}
	
	public int getNumberOfSpots() {
		return coveredArea;
	}
	
	public int getHumansAmount() {
		return humansList.size();
	}
	
	public int getCapacity() {
		return capacity;
	}
	
	protected void setCapacity(int limit) {
		this.capacity = limit;
	}
	
	public boolean isOutdoor() {
		return outdoor;
	}
}
