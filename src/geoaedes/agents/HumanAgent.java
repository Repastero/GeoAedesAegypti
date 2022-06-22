package geoaedes.agents;

import geoaedes.BuildingManager;
import geoaedes.DataSet;
import geoaedes.InfectionReport;
import geoaedes.Utils;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.random.RandomHelper;

public class HumanAgent {
	/** Puntero al building manager */
	protected static BuildingManager bm;
	
	/** Parcela actual o null si en exterior */
	private BuildingAgent currentBuilding = null;
	/** Parcela hogar o null si extranjero */
	private BuildingAgent homePlace;
	/** Parcela trabajo o null si en exterior o desempleado */
	private BuildingAgent workPlace;
	
	/** Indice de seccional origen */
	private int sectoralIndex;
	/** Indice estado de markov donde esta (0 es la casa, 1 es el trabajo/estudio, 2 es ocio, 3 es otros) */
	private int currentState = 0;
	/** Humano extranjero */
	private boolean foreignTraveler = false;
	/** Humano turista */
	private boolean touristTraveler = false;
	
	/** Puntero a ISchedule para programar acciones */
	protected static ISchedule schedule;
	/** Contador Id de agente */
	private static int agentIDCounter = 0;
	/** Id de agente */
	private int agentID = ++agentIDCounter;
	
	/** Matriz de markov locales */
	private static int[][][] localTMMC;
	
	// ESTADOS //
	private boolean incubating;	// Contagiado
	private boolean infected;	// Infeccioso
	private boolean recovered;	// Recuperado

	public HumanAgent(int secHomeIndex, BuildingAgent home, BuildingAgent work) {
		this.currentBuilding = home; // Inicia en su casa
		this.sectoralIndex = secHomeIndex;
		this.homePlace = home;
		this.workPlace = work;
	}
	
	public HumanAgent(int secHomeIndex, BuildingAgent home, BuildingAgent work, boolean foreign, boolean tourist) {
		this(secHomeIndex, home, work);
		this.foreignTraveler = foreign;
		this.touristTraveler = tourist;
	}
	
	public static void initAgentID(BuildingManager buildingManager) {
		agentIDCounter = 0;
		
		bm = buildingManager;
		schedule = RunEnvironment.getInstance().getCurrentSchedule();
		localTMMC = BuildingManager.DEFAULT_TMMC;
	}
	
	/** @return {@link HumanAgent#agentID} */
	public int getAgentID() {
		return agentID;
	}
    
	public BuildingAgent getCurrentBuilding() {
		return currentBuilding;
	}
	
	/**
	 * Si no ha sido infectado anteriormente, programa infectar el Humano una vez que termine el tiempo de incubacion (intrinsic incubation period).<p>
	 * The intrinsic incubation period is the time taken by an organism to complete its development in the definitive host.
	 */
	public void setExposed() {
		// Chequea si no esta incubando, infectado o recuperado
		if (!recovered && !incubating && !infected) {
			incubating = true;
			// Schedule one shot para terminar el periodo de incubacion y setear humano como infectado
			int incPeriod = Utils.getStdNormalDeviate(DataSet.INCUBATION_PERIOD_MEAN, DataSet.INCUBATION_PERIOD_DEVIATION);
			ScheduleParameters scheduleParams = ScheduleParameters.createOneTime(schedule.getTickCount() + incPeriod, ScheduleParameters.FIRST_PRIORITY);
			schedule.schedule(scheduleParams, this, "setInfectious", false);
		}
	}
	
	/**
	 * Si no ha sido infectado anteriormente, termina de infectar el Humano<p>
	 * y programa recuperarlo cuando termine el tiempo de contagio.
	 * @param initial <b>true</b> si es de los primeros infectados
	 */
	public void setInfectious(boolean initial) {
		if (!recovered) {
			incubating = false;
			infected = true;
			InfectionReport.addInfectedHuman();
			if (!initial)
				InfectionReport.addDailyCase();
			// Se marca la casa del infectado - no donde se infecto
			if (homePlace != null)
				bm.createInfectionSpot(homePlace.getId(), homePlace.getCoordinate());
			// Se marca el infectado por el periodo de infeccion
			if (currentBuilding != null) {
				currentBuilding.increaseInfectedHumans();
				bm.createInfectedHuman(agentID, currentBuilding.getCoordinate());
			}
			// Schedule one shot para terminar el periodo de contagio y setear humano como recuperado
			int contPeriod = Utils.getStdNormalDeviate(DataSet.CONTAGIOUS_PERIOD_MEAN, DataSet.CONTAGIOUS_PERIOD_DEVIATION);
			ScheduleParameters scheduleParams = ScheduleParameters.createOneTime(schedule.getTickCount() + contPeriod, ScheduleParameters.FIRST_PRIORITY);
			schedule.schedule(scheduleParams, this, "setRecovered");
		}
	}
	
	/** @return <b>true</b> si es infeccioso. */
    public boolean isInfected() {
    	return infected;
    }
	
	/**
	 * Finaliza el periodo de contagio del virus en el Humano.
	 */
	public void setRecovered() {
		if (infected) {
			infected = false;
			InfectionReport.removeInfectedHuman();
			recovered = true;
			InfectionReport.addRecoveredHuman();
			// Se elimina la marca en hogar del infectado
			if (homePlace != null)
				bm.deleteInfectionSpot(homePlace.getId());
			if (currentBuilding != null)
				currentBuilding.decreaseInfectedHumans();
			bm.deleteInfectedHuman(agentID);
		}
	}
	
	/**
	 * Selecciona la parcela donde realizar la nueva actividad.
	 * @param prevStateIndex indice de estado previo
	 * @param stateIndex indice de nuevo estado
	 * @return <b>BuildingAgent</b> o <b>null</b>
	 */
	public BuildingAgent switchActivity(int prevStateIndex, int stateIndex) {
		BuildingAgent newBuilding;
        switch (stateIndex) {
	    	case 0: // 0 Casa
	    		newBuilding = homePlace;
	    		break;
	    	case 1: // 1 Trabajo / Estudio
	    		newBuilding = workPlace;
	    		break;
	    	default: // 2 Ocio / 3 Otros (supermercados, farmacias, etc)
	    		newBuilding = bm.findRandomPlace(sectoralIndex, stateIndex, this, currentBuilding);
	    		break;
        }
        return newBuilding;
	}
	
    /**
    * Cambia el inmueble actual, segun TMMC (Timed mobility markov chains).
    */
	//@ScheduledMethod(start = 0, interval = 3, priority = 0.5)
    public void switchLocation() {
		final int p = ((int) schedule.getTickCount() % 360) / 90; // 0 1 2 3 - 360 ticks = 12 horas
        int r = RandomHelper.nextIntFromTo(1, 1000);
        int i = 0;
        // Recorre la matriz correspondiente al periodo del dia y estado actual
        while (r > localTMMC[p][currentState][i]) {
        	// Avanza el indice de estado, si es posible restar probabilidad
        	r -= localTMMC[p][currentState][i];
        	++i;
        }
        
        // Si el nuevo lugar es de distinto tipo, lo cambia
        if (currentState != i) {
        	BuildingAgent tempBuilding = switchActivity(currentState, i);
        	relocate(tempBuilding, i);
        }
    }
    
	/**
	 * Cambia la ubicacion del agente e indice de estado.
	 * @param newBuilding nueva parcela o null
	 * @param newState indice nuevo estado
	 */
	private void relocate(BuildingAgent newBuilding, int newState) {
    	// Lo remueve si esta dentro de una parcela
        if (currentBuilding != null) {
        	currentBuilding.removeHuman(this);
        	currentBuilding = null;
        }
        currentState = newState;
        
    	// Si el nuevo lugar es una parcela
    	if (newBuilding != null) {
    		if (newBuilding.insertHuman(this)) {
    			currentBuilding = newBuilding;
    			if (infected)
    				bm.moveInfectedHuman(agentID, currentBuilding.getCoordinate());
    		}
    	}
   	}
}
