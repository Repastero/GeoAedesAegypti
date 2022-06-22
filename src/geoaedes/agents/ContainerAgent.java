package geoaedes.agents;

import static repast.simphony.essentials.RepastEssentials.AddAgentToContext;
import static repast.simphony.essentials.RepastEssentials.GetTickCount;

import java.util.ArrayList;
import java.util.List;

import geoaedes.DataSet;
import geoaedes.InfectionReport;
import geoaedes.Weather;
import repast.simphony.engine.schedule.ScheduledMethod;

public class ContainerAgent {
	private static int agentIDCounter = 0;
	private int agentID = agentIDCounter++;
	
	/** Lista de acuaticos en contenedor (huevos, larvas y pupas) */
	private List<AquaticAgent> aquaticsList = new ArrayList<AquaticAgent>();

	/** Puntero a Building donde esta el Container */
	private BuildingAgent building;
	/** Flag contenedor en interiores */
	private boolean indoor;
	
	/** Area (cm^2) y altura (cm) */
	private double[] dimensiones = {0d,0d};
	/** Cantidad actual de agua (mm) */
	private double mmWater;
	
	/** Capacidad de acarreo de larvas y pupas */
	private int	carryingCapacity;
	/** Capacidad de acarreo de huevos */
	private int	carryingCapacityEggs;
	
	/** Cantidad actual de huevos */
	private int eggsAmount;
	/** Cantidad actual de larvas y pupas */
	private int aquaticAmount;

	public ContainerAgent(BuildingAgent building, boolean isIndoors, double area, double height) {
		this.building = building;
		this.indoor = isIndoors;
		this.dimensiones[0] = area;
		this.dimensiones[1] = height;
		if (this.indoor) {
			// Valor inicial de agua
			this.mmWater = dimensiones[1] * 10; //paso de cm de altura a mm
			updateCarryingCapacity();
		}
		updateCarryingCapacityEggs();
	}
	
	public static void initAgentID() {
		agentIDCounter = 0;
	}
	
	/**
	 * Actualiza la cantidad de agua de Containers al exterior y actualiza el estado de sus Acuaticos.
	 */
	@ScheduledMethod(start = 0, interval = 360, priority = 0.92)
	public void updateDay() {
		if (!indoor) {
			updateMmWater();
			updateCarryingCapacity();
			// Reporta la cantidad de agua en contenedor interior
			InfectionReport.increaseWater(mmWater);
		}
		// Eliminar los acuaticos que mueran
		aquaticsList.removeIf(aqua -> !aqua.updateLife());
		// Reporta si el contenedor es positivo (tiene huevos)
		if (isPositive())	InfectionReport.addPositiveContainers();
	}
	
	/**
	 * Actualiza la capacidad de acarreo de Larvas y Pupas, segun la cantidad de agua.
	 */
	private void updateCarryingCapacity() {
		carryingCapacity = obtainCarryingCapacity(mmWater);
	}
	
	public  int	getCarryingCapacity() {
		return carryingCapacity;
	}
	
	public  int getAgentID() {
		return agentID;
	}
	
	public  BuildingAgent getBuilding() {
		return building;
	}
	
	public double getArea() {
		return dimensiones[0];
	}
	
	public double getHeight() {
		return dimensiones[1];
	}
	
	public  double getMmWater() {
		return mmWater;
	}
	
	public	boolean containsWater() {
		return (mmWater > 0d);
	}
	
	public  int getEggsAmount() {
		return eggsAmount;
	}
	
	public void increaseEggsAmount() {
		++eggsAmount;
	}
	
	public void decreaseEggsAmount() {
		--eggsAmount;
	}

	public  int getAquaticAmount() {
		return aquaticAmount;
	}
	
	public void increaseAquaticAmount() {
		++aquaticAmount;
	}
	
	public void decreaseAquaticAmount() {
		--aquaticAmount;
	}
	
	/** @return <b>true</b> si el container esta en interior. */
	public  boolean isIndoors() {
		return indoor;
	}
	
	/** @return <b>true</b> si el container tiene huevos. */
	public  boolean isPositive() {
		return (eggsAmount != 0);
	}
	
	private int obtainCarryingCapacity(double mmWater) {
		// Capacidad de acarreo = (0.1*Agua[mm] = Agua[cm])*(Area = [cm^2])*(70 individuos por litro)
		return (int) (0.1 * mmWater * dimensiones[0] * 0.001 * DataSet.CONTAINER_CARRYING_CAPACITY);
	}
	public  int	getCarryingCapacityEggs() {
		return carryingCapacityEggs;
	}
	
	private void updateCarryingCapacityEggs() {
		int cc = carryingCapacity;
		if (!indoor)
			cc = obtainCarryingCapacity(dimensiones[1] * 10) >> 1; // capacidad de acarreo media
		// El valor originalmente de capacidad de acarreo de huevos era:
		// capacidad de acarreo de actuaticos por 2 a por 5 (cc*2 - cc*5).
		carryingCapacityEggs = cc;
	}

	/**
	 * Resta el agua evaporada y suma la de la precipitacion del dia.
	 */
	private void updateMmWater() {
		if (mmWater > 0d && GetTickCount() != 0d) {
			mmWater -= Weather.getEvaporationRate();
			if (mmWater < 0d) // mm de agua
				mmWater = 0d;
		}
		mmWater += Weather.getPrecipitation();
		if (mmWater > dimensiones[1] * 10) { //contenedor lleno
			mmWater = dimensiones[1] * 10;
		}
	}
	
	/** @return <b>true</b> si tiene agua y espacio para mas huevos. */
	public boolean canCarryMoreEggs() {
		// TODO ver: segun Silvina pueden poner huevos en contenedores sin agua tambien
		return (containsWater() && getEggsAmount() + 10 < getCarryingCapacityEggs());
	}
	
	/**
	 * Agrega nuevo Acuatico al contenedor e incrementa su cantidad.
	 * @param aquatic  instancia de objecto Acuatico a agregar
	 * @return <code>true</code> si sobra espacio para ese tipo de Actuatico
	 */
	public boolean addAquatic(AquaticAgent aquatic) {
		aquaticsList.add(aquatic);
		if (aquatic.getLifeCicle() == 0) { // Huevo
			if (++eggsAmount >= carryingCapacityEggs)
				return false;
		}
		else { // Larva o pupa
			if (++aquaticAmount >= carryingCapacity)
				return false;
		}
		return true;
	}
	
	/**
	 * Crea un nuevo agente Mosquito y lo agrega al contexto, en la mismo edificio que el Container.<p>
	 * El Acuatico al completar todas las fases, emerge como Mosquito adulto;
	 * se eliminar la referencia del Acuatico de la lista en {@link #updateDay()}.
	 */
	public void emergeMosquito() {
		MosquitoAgent adult = new MosquitoAgent(building);
		AddAgentToContext("GeoAedesAegypti", adult);
	}
}
