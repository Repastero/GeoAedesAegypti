package geoaedes.agents;

import java.util.List;

import geoaedes.BuildingManager;
import geoaedes.DataSet;
import geoaedes.InfectionReport;
import geoaedes.LifeCicle;
import geoaedes.Utils;
import geoaedes.Weather;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedulableAction;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.random.RandomHelper;
import repast.simphony.util.SimUtilities;

public class MosquitoAgent extends LifeCicle {
	/** Puntero al contexto */
	private static Context<Object> context;
	/** Puntero al building manager */
	private static BuildingManager bm;
	/** Puntero al scheduler */
	private static ISchedule schedule;
	
	private static int agentIDCounter = 0;
	private static int agentCount = 0;
	private int agentID = ++agentIDCounter;
	
	/** Puntero parcela actual */
	private BuildingAgent currentBuilding;
	/** Puntero humano objetivo actual */
	private HumanAgent prey;
	/** Puntero ultima accion programada (ritmo circadiano) */
	private ISchedulableAction lastScheduledAction;
	
	/** Cantidad de picaduras en ultimo ciclo circadiano */
	private int bitesLastCycle = 0;
	/** Cantidad de ciclos circadiano activos en el dia */
	private int activeCycles = 0;
	
	/** Contador de intervalos de inactividad restantes */
	private int inactiveIntervals;
	/** Contador de intervalos de actividad restantes */
	private int activeIntervals;
	/** Flag para conocer si estuvo activo el ultimo ciclo */
	private boolean prevCycleActive;
	
	/** Tiempo de vida en dias */
	private double	lifespan;
	/** Cantidad media de picaduras */
	private static int bitesMeanAmount;
	/** Cantidad necesarias de picaduras en fase de alimentacion */
	private int bitesQuota;
	/** Contador de cantidad restantes de picaduras para finalizar alimentacion */
	private int bitesRemQuota;
	/** Intentos restantes de oviposicion */
	private int ovipositionTries;
	
	// ESTADOS //
	private boolean incubating;	// Contagiado
	private boolean infected;	// Infeccioso
	
	private boolean changeGTPhase	= true;		// Flag para indicar que debe cambiar de fase
	private boolean feedingPhase	= false;	// Primera fase ciclo gonotrofico
	private boolean digestingPhase	= false;	// Segunda fase ciclo gonotrofico
	private boolean breedingPhase	= false;	// Tercera fase ciclo gonotrofico
	
	public MosquitoAgent(BuildingAgent building) {
		this.setCurrentBuilding(building);
		++agentCount;
		// Asigna tiempo de vida inicial
		lifespan = Utils.getStdNormalDeviate(DataSet.ADULT_LIFESPAN_MEAN, DataSet.ADULT_LIFESPAN_MEAN);
		// Asigna cantidad de picaduras (media +-1)
		bitesQuota = Utils.getStdNormalDeviate(bitesMeanAmount, 1);
		// Iniciar contador de cantidad de picaduras
		bitesRemQuota = bitesQuota;
	}
	
	public static void initAgent(Context<Object> mainContext, BuildingManager buildingManager, int bitesMean) {
		agentIDCounter = 0;
		agentCount = 0;
		
		bm = buildingManager;
		bitesMeanAmount = bitesMean;
		
		context = mainContext;
		schedule = RunEnvironment.getInstance().getCurrentSchedule();
	}
	
	public boolean	getInfected() {
		return infected;
	}
	
	public static int getCount() {
		return agentCount;
	}
	
	public BuildingAgent getCurrentBuilding() {
		return currentBuilding;
	}

	public void setCurrentBuilding(BuildingAgent newBuilding) {
		if (currentBuilding != null)
			currentBuilding.decreaseMosquitoesAmount();
		currentBuilding = newBuilding;
		currentBuilding.increaseMosquitoesAmount();
	}
	
	/**
	 * Actualiza la probabilidad de muerte, resta 1 dia de vida y continua el ciclo Gonotrofico (si corresponde).<p>
	 * Three phases of one gonotrophic cycle:<p>
	 * <ul>
	 * <li> The searching for a host and the obtaining of the blood-meal. {@link #beginFirstGTPhase()}
	 * <li> Digestion of the blood and egg formation. {@link #beginSecondGTPhase()}
	 * <li> The search for breeding places and ovipositions. {@link #beginThirdGTPhase()}
	 * </ul>
	 */
	@ScheduledMethod(start = 0, interval = 360, priority = 0.91)
	@Override
	public void updateLife() {
		lifespan -= Weather.getDDAdult();
		// Si muere por alcanzar el tiempo de vida
		// o por muerte subita
		if (lifespan <= 0d || RandomHelper.nextDoubleFromTo(0d, 1d) < getDeathRate()) {
			this.eliminate();
			return;
		}
		
		// Si hace falta cambiar de etapa
		if (changeGTPhase) {
			changeGTPhase = false;
			if (feedingPhase) {
				feedingPhase = false;
				beginSecondGTPhase();
				digestingPhase = true;
			}
			else if (digestingPhase) {
				digestingPhase = false;
				beginThirdGTPhase();
				breedingPhase = true;
			}
			else {
				// Inicio del ciclo gonotrofico
				breedingPhase = false;
				beginFirstGTPhase();
				feedingPhase = true;
			}
		}
	}
	
	/**
	 * Cada 1 tick elije aleatoriamente una probabilidad y la compara<p>
	 * con el Ciclo Circardiano, para saber si le toca buscar Humanos para picar.<p>
	 * Si pasa 3 ticks estando activo y sin encontrar presas, se muda a otra parcela.
	 */
	private void beginFirstGTPhase() {
		// Reinicia cantidad de picaduras
		bitesRemQuota = bitesQuota;
		//
		activeIntervals = 30;
		inactiveIntervals = 0;
		prevCycleActive = false;
		bitesLastCycle = 0;
		// Antes de comenzar a picar, se fija previamente a partir de que hora esta activo
		int activeStartTime = (int) schedule.getTickCount();
		int circadianCycle = (activeStartTime % 360) / 30; // 0 ... 11
		while (RandomHelper.nextDoubleFromTo(0d, 1d) >= DataSet.ADULT_CIRCADIAN_RHYTHMS[circadianCycle]) {
			// No activo
			if (++circadianCycle >= 12)
				circadianCycle = 0;
			activeStartTime += 30;
		}
		ScheduleParameters params = ScheduleParameters.createRepeating(activeStartTime, 1, .7d); // 1 tick = 2 minutos				
		lastScheduledAction = schedule.schedule(params, this, "seekAndBite");
	}
	
	/**
	 * Cambia de ciclo circadiano.<p>
	 * MaxiC: la funcion de probabilidad de actividad esta definida en un rango de 12 horas.<p>
	 * MaxiC: distribucion de probabilidad de actividad calculda por IvanG.<p>
	 * Ema: Ivan G se basa en el paper: Jones, M. D. R. (1981). The programming of circadian flight activity<br>
	 * in relation to mating and the gonotrophic cycle in the mosquito.
	 */
	private void nextCircadianCycle() {
		final int circadianCycle = ((int) schedule.getTickCount() % 360) / 30; // 0 ... 11
		final double udato = RandomHelper.nextDoubleFromTo(0d, 1d);
		if (udato < DataSet.ADULT_CIRCADIAN_RHYTHMS[circadianCycle]) {
			if (circadianCycle == 0) { // Inicio del dia
				activeCycles = 0;
				prevCycleActive = false;
				prey = null;
			}
			else if (bitesLastCycle == 0 && prevCycleActive) {
				if (++activeCycles == 3) {
					activeCycles = 0;
					// Mudar ->
					setCurrentBuilding(bm.findPlaceWithHumans(currentBuilding));
				}
			}
			prevCycleActive = true;
			bitesLastCycle = 0;
			activeIntervals = 30; // por hasta una hora sigue buscando / picando
		}
		else {
			prevCycleActive = false;
			activeCycles = 0;
			inactiveIntervals = 30; // por una hora no vuelve a picar
		}
	}
	
	/**
	 * Busca una presa en el inmueble, una vez encontrada procede a picar.<p>
	 * Al cumplir la cantidad de picaduras, pasa a la proxima fase.
	 */
	public void seekAndBite() {
		if (changeGTPhase) {
			// Ya cumplio con la cantidad de picaduras o paso el tick de activo
			return;
		}
		
		if (activeIntervals == 0 && inactiveIntervals == 0)
			nextCircadianCycle();
		
		// Ciclo activo
		if (activeIntervals > 0) {
			if (prey != null) {
				if (prey.getCurrentBuilding() == currentBuilding) {
					++bitesLastCycle;
					// Si el mosquito o el humano estan infectados
					if (infected || (prey.isInfected() && !incubating)) {
						// Hay probabilidad de contagio / infeccion
						if (RandomHelper.nextDoubleFromTo(0d, 100d) <= DataSet.INFECTION_RATE) {
							if (infected)	prey.setExposed();
							else 			startEIPeriod();
						}
					}
					// Resta la cantidad de picaduras disponibles
					if (--bitesRemQuota == 0) {
						changeGTPhase = true;
						stopLastSchAction();
						return;
					}
					// Probabilidad que cambie de presa
					if (RandomHelper.nextIntFromTo(1, 100) <= DataSet.ADULT_SWITCH_TARGET_CHANCE)
						prey = null;
				}
				else
					prey = null;
			}
			else if (currentBuilding.getHumansAmount() != 0) {
				// Metros cuadrados ocupados por Humano / area construida
				double probFind = (currentBuilding.getHumansAmount() * 2d) / currentBuilding.getCoveredArea();
				if (probFind >= RandomHelper.nextDoubleFromTo(0d, 1d))
					prey = currentBuilding.getRandomHuman();
			}
			--activeIntervals;
		}
		// Ciclo Inactivo
		else
			--inactiveIntervals;
	}
	

	/**
	 * Inicia el tiempo de digestion (gestacion o embrionacion).
	 */
	private void beginSecondGTPhase() {
		// Periodo de gestacion
		int digestingEndTime = (int) schedule.getTickCount() + DataSet.ADULT_DIGESTION_PERIOD;
		digestingEndTime = ((int) Math.ceil(digestingEndTime / 360)) * 360;
		ScheduleParameters params = ScheduleParameters.createOneTime(digestingEndTime, ScheduleParameters.FIRST_PRIORITY);
		lastScheduledAction = schedule.schedule(params, this, "endSecondGTPhase");
	}

	/**
	 * Indica el final del tiempo de digestion (gestacion o embrionacion).
	 */
	public void endSecondGTPhase() {
		// En el proximo "updateLife" cambia de fase
		changeGTPhase = true;
	}
	
	/**
	 * Busca Containers apropiados para la ovoposicion en el inmueble actual.<p>
	 * Si encuentra, deposita los Huevos, incrementa el numero de ovoposiciones y cambia de fase.<p>
	 * Si no encuentra donde ovipositar, se traslada a un nuevo inmueble e intenta nuevamente cada intervalo especificado.
	 * @see DataSet.ADULTO_INTERVALO_BUSQUEDA_CONTENEDOR
	 */
	private void beginThirdGTPhase() {
		// Reinicia los intentos de busqueda para ovipositar
		ovipositionTries = DataSet.ADULT_CONTAINER_SEARCH_TRIES;
		if (layEggsInWater() || --ovipositionTries == 0) {
			// Si encuentra al instante donde ovipositar,
			// saltea programar la accion de seguir buscando. 
			changeGTPhase = true;
			// Fin del ciclo gonotrofico
		}
		else {
			setCurrentBuilding(bm.findPlaceWithContainers(currentBuilding));
			//
			final int containerSearchInterval = DataSet.ADULT_CONTAINER_SEARCH_INTERVAL * 360;
			ScheduleParameters params = ScheduleParameters.createRepeating(
					schedule.getTickCount() + containerSearchInterval, 
					containerSearchInterval, .67);
			lastScheduledAction = schedule.schedule(params, this, "continueThirdGTPhase");
		}
	}
	
	/**
	 * Repite la accion de intentar ovipositar hasta lograrlo o se cumpla el tiempo limite.
	 * @see DataSet.ADULTO_INTENTOS_BUSQUEDA_CONTENEDOR
	 */
	public void continueThirdGTPhase() {
		if (changeGTPhase) {
			return;
		}
		// Si encuentra donde ovipositar o
		// se pasa el tiempo maximo de busqueda
		else if (layEggsInWater() || --ovipositionTries == 0) {
			// Finaliza la accion y fase
			stopLastSchAction();
			changeGTPhase = true;
			// Fin del ciclo gonotrofico
		}
	}
	
	/**
	 * Intenta detener la ultima accion programada.
	 */
	private void stopLastSchAction() {
		if (!removeLastSchAction()) {
			ScheduleParameters params = ScheduleParameters.createOneTime(schedule.getTickCount() + 0.1);
			schedule.schedule(params, this, "removeLastSchAction");
		}
	}
	
	/**
	 * Eliminar la ultima accion programada
	 * @return <b>true</b> si se elimino la accion
	 */
	public boolean removeLastSchAction() {
		if (schedule.removeAction(lastScheduledAction)) {
			lastScheduledAction = null;
			return true;
		}
		return false;
	}
	
	/**
	 * Programa infectar el Mosquito una vez que termine el tiempo de incubacion (extrinsic incubation period).<p>
	 * The extrinsic incubation period is the time taken by an organism to complete its development in the intermediate host.
	 */
	private void startEIPeriod() {
		incubating = true;
		// Ema: modificada de Ecuacion estudiada por Helmersson, 2012.Mathematical Modeling of Dengue-Temperature Effect on Vectorial Capacity. UniversitetUMEA.
		final int eIPeriod = (4 + (int) Math.exp(4 - 0.123 * Weather.getTemperature())) * 360;	// dias * 360 ticks
		// Schedule one shot para terminar el periodo de incubacion y setear mosquito como infectado
		ScheduleParameters params = ScheduleParameters.createOneTime(schedule.getTickCount() + eIPeriod, ScheduleParameters.FIRST_PRIORITY);
		schedule.schedule(params , this , "setInfected");
	}
	
	/**
	 * Metodo invocado por SchedulableAction creada en: {@link #startEIPeriod()}
	 */
	public void setInfected() {
		if (incubating) { // Chequeo por las dudas 
			infected = true;
			InfectionReport.addInfectedMosquito();
		}
	}
	
	/**
	 * Infectar sin periodo de incubacion.
	 */
	public void infectTransmitter() {
		incubating = true;
		setInfected();
	}

	/**
	 * Busca aleatoriamente en los Containers del inmueble, si tienen agua y capacidad de acarreo.<br>
	 * Deposita la cantidad posible o disponible de huevos en los contenedores seleccionados.
	 * @see DataSet.ADULT_EGGS_CTNR_MEAN
	 * @see DataSet.ADULT_EGGS_PRODUCTION
	 * @return <code>true</code> si encuentra donde ovipositar
	 */	
	private boolean layEggsInWater() {
		final List<ContainerAgent> availContainers = currentBuilding.getAvailableContainers();
		if (availContainers.isEmpty()) // no hay disponibles en parcela
			return false;
		
		// Si hay mas de 1 contenedor disponible, deposita en cantidad media +-1
		int maxContainers = Utils.getStdNormalDeviate(DataSet.ADULT_EGGS_CTNR_MEAN, 1);
		// Minimo 1 contenedor, por las dudas este mal seteado
		if (maxContainers < 1)	maxContainers = 1;
		// Si hay menos disponibles en parcela, usa esa cantidad
		if (maxContainers > availContainers.size())
			maxContainers = availContainers.size();
		// Si hay mas disponibles en parcela, mezcla el orden
		else if (maxContainers < availContainers.size())
			SimUtilities.shuffle(availContainers, RandomHelper.getUniform());
		
		int eggsToLay = DataSet.ADULT_EGGS_PRODUCTION;
		ContainerAgent container;
		if (maxContainers == 1) {
			// Agrega los huevos en un solo contenedor hasta
			// quedar sin huevos o sin capacidad en contenedor. 
			container = availContainers.get(0);
			for (; eggsToLay > 0; eggsToLay--) {
				if (!container.addAquatic(new AquaticAgent(container))) {
					break; // container lleno
				}
			}
		}
		else {
			// Agrega un huevo en cada contenedor hasta
			// quedar sin huevos o sin capacidad en contenedores. 
			do {
				for (int i = maxContainers - 1; i >= 0; i--) {
					container = availContainers.get(i);
					--eggsToLay;
					if (!container.addAquatic(new AquaticAgent(container))) {
						// container lleno
						if (--maxContainers > 0) { // queda mas de 1
							if (i != maxContainers) { // no es el ultimo
								// mueve a la ultima posicion
								availContainers.set(i, availContainers.get(maxContainers));
							}
						}
					}
				}
			} while (maxContainers > 0 && eggsToLay > 0);
		}
		// TODO ver: que hacer si "eggsToLay" es muy alto (mudar?)
		return true;
	}

	@Override
	public double getDeathRate() {
		return Weather.getDDRAdult();
	}

	@Override
	public void eliminate() {
		changeGTPhase = true; // Para pausar la etapa actual
		incubating = false;
		//
		currentBuilding.decreaseMosquitoesAmount();
		if (infected)
			InfectionReport.removeInfectedMosquito();
		//
		if (lastScheduledAction != null)
			stopLastSchAction();
		context.remove(this);
		--agentCount;
	}
}
