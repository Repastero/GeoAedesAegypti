package geoaedes;

import geoaedes.agents.MosquitoAgent;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;

public class InfectionReport {
	/** Cantidad maxima de mosquitos adultos en simultaneo */
	private static int adultsLimit;
	
	/** Cantidad actual de Humanos infectados */
	private static int infectedHumans; // Acumulado
	/** Cantidad total de Humanos recuperados de infeccion */
	private static int recoveredHumans; // Acumulado
	/** Cantidad de nuevos casos de infeccion */
	private static int dailyCases; // Instantaneo
	
	/** Cantidad actual de Mosquitos adultos infectados */
	private static int infectedMosquitoes; // Acumulado
	/** Cantidad total de Mosquitos adultos infectados en simulacion */
	private static int totalInfectedMosquitoes; // Acumulado
	
	/** Cantidad actual de Containers positivos */
	private static int positiveContainers; // Instantaneo
	
	/** Cantidad total actual de agua en contenedores al exterior */
	private static double outdoorContainersWater; // Instantaneo
	/** Cantidad actual de contenedores al exterior */
	private static int outdoorContainers; // Instantaneo
	
	/** Indice maximo de adultos por contenedor positivo */
	private static double maxAdultsPerContainer; // Acumulado
	
	public InfectionReport(int maxAdults) {
		adultsLimit = maxAdults;
		
		infectedHumans = 0;
		recoveredHumans = 0;
		
		infectedMosquitoes = 0;
		totalInfectedMosquitoes = 0;
		
		maxAdultsPerContainer = 0d;
	}
	
	/**
	 * Reinicio diariamente la cantidad de nuevos casos de Humanos infectados y las picaduras de Mosquitos.
	 */
	@ScheduledMethod(start = 0, interval = 360, priority = 1)
	public void inicializadorDiario() {
		dailyCases = 0;
		
		positiveContainers = 0;
		
		outdoorContainersWater = 0;
		outdoorContainers = 0;
		
		// Termina la simulacion si se supera el limite de adultos (si existe)
		if (adultsLimit != 0 && adultsLimit < MosquitoAgent.getCount()) {
			System.out.println("Simulacion finalizada por limite de mosquitos adultos: "+adultsLimit);
			RunEnvironment.getInstance().endRun();
		}
	}
	
	public static void addInfectedHuman() {
		++infectedHumans;
	}
	
	public static void removeInfectedHuman() {
		--infectedHumans;
	}
	
	public static void addRecoveredHuman() {
		++recoveredHumans;
	}
	
	public static void addDailyCase() {
		++dailyCases;
	}
	
	public static void addInfectedMosquito() {
		++infectedMosquitoes;
		++totalInfectedMosquitoes;
	}
	
	public static void removeInfectedMosquito() {
		--infectedMosquitoes;
	}
	
	public static void addPositiveContainers() {
		++positiveContainers;
	}
	
	public static void increaseWater(double waterMM) {
		outdoorContainersWater += waterMM;
		++outdoorContainers;
	}
	
	// Getters para usar en reportes de Repast Simphony
	public static int getDailyCases()			{ return dailyCases; }
	public static int getInfectedHumans()		{ return infectedHumans; }
	public static int getRecoveredHumans()		{ return recoveredHumans; }
	
	public static int getInfectedMosquitoes()		{ return infectedMosquitoes; }
	public static int getTotalInfectedMosquitoes()	{ return totalInfectedMosquitoes; }
	
	public static int getPositiveContainers() 		{ return positiveContainers; }
	public static double getMaxAdultsPerContainer()	{ return maxAdultsPerContainer; }
	
	/** Agua promedio restante en contenedores en interiores */
	public static double getAdultsPerContainer() {
		final double adultsPerCon = (positiveContainers != 0) ? (double) MosquitoAgent.getCount() / positiveContainers : 0d;
		if (adultsPerCon > maxAdultsPerContainer)
			maxAdultsPerContainer = adultsPerCon;
		return adultsPerCon;
	}
	
	/** Agua promedio restante en contenedores al exterior */
	public static double getAVGRemainingWater()	{
		return (outdoorContainersWater > 0) ? outdoorContainersWater / outdoorContainers : 0d;
	}
}
