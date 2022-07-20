package geoaedes;

import java.net.JarURLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import geoaedes.agents.AquaticAgent;
import geoaedes.agents.BuildingAgent;
import geoaedes.agents.ContainerAgent;
import geoaedes.agents.ForeignHumanAgent;
import geoaedes.agents.HomeAgent;
import geoaedes.agents.HumanAgent;
import geoaedes.agents.MosquitoAgent;
import geoaedes.agents.WorkplaceAgent;
import repast.simphony.context.Context;
import repast.simphony.context.space.gis.GeographyFactoryFinder;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.parameter.Parameters;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.gis.Geography;
import repast.simphony.space.gis.GeographyParameters;

/**
 * Contexto principal de modelo.
 */
public class ContextCreator implements ContextBuilder<Object> {
	private ISchedule schedule; // Puntero
	private Context<Object> context; // Puntero
	private Geography<Object> geography; // Puntero
	
	// Parametros de simulacion
	/** Ano simulacion, para calcular leer datos climaticos (minimo 2010) */
	private int simulationStartYear;
	/** Cantidad de mosquitos adultos vivos que debe superarse para finalizar simulacion (0 = Infinito) */
	private int adultMosquitosLimit;
	/** Fraccion de poblacion convertida en viajeros locales infectados */
	private double fracOfInfHumans;
	/** Fraccion de poblacion convertida en turistas infectados */
	private double fracOfInfMosquitoes;
	/** Cantidad inicial de huevos */
	private int startingEggsAmount;
	/** Cantidad inicial de mosquitos adultos */
	private int startingAdultsAmount;
	/** Cantidad inicial de contenedores positivos */
	private int startingPosCtnrAmount;
	/** Cantidad media de picaduras por ciclo */
	private int mosquitoBitesMean;
	/** Cantidad de corridas para hacer en batch */
	private int simulationRun;
	/** Dias en que se eliminan contenedores */
	private int[] containersRemoveDays;
	/** Porcentaje (en base a cant. inicial) de contenedores eliminados por dia */
	private int[] containersRemovePct;
	/** Dias en que se agregan contenedores */
	private int[] containersAddDays;
	/** Porcentaje (en base a cant. inicial) de contenedores agregados por dia */
	private int[] containersAddPct;
	//
	
	/** Indice semanal anual */
	private int   simumationWeekIndex;
	/** Cantidad de nuevos humanos infectados semanalmente */
	private int[] humansInfEveryWeek;
	/** Cantidad de nuevos mosquitos infectados semanalmente */
	private int[] mosquitoesInfEveryWeek;
	
	/** Tiempo inicio de simulacion */
	private long simulationStartTime;
	
	/** Cantidad de humanos que viven en contexto */
	private int localHumansCount;
	/** Cantidad de humanos que viven fuera del contexto */
	private int foreignHumansCount;
	
	private int lastHomeId;	// Para no repetir ids, al crear hogares ficticios
	private List<List<HomeAgent>> homePlaces;	// Lista de hogares en cada seccional
	private List<WorkplaceAgent> workPlaces = new ArrayList<WorkplaceAgent>();		// Lista de lugares de trabajo
	private List<WorkplaceAgent> schoolPlaces = new ArrayList<WorkplaceAgent>();	// Lista de lugares de estudio
	
	/** Buildings en general, donde pueden habitar mosquitos */
	private List<BuildingAgent> susceptibleBuildings = new ArrayList<BuildingAgent>();
	/** Indice inicial y final de places en susceptibleBuildings */
	private int[] susceptiblePlacesIdx;
	/** Containers en interiores */
	private List<ContainerAgent> indoorContainers = new ArrayList<ContainerAgent>();
	/** Containers en exteriores */
	private List<ContainerAgent> outdoorContainers = new ArrayList<ContainerAgent>();
	
	private Map<String, PlaceProperty> placesProperty = new HashMap<>(); // Lista de atributos de cada tipo de Place
	private BuildingManager buildingManager;
	
	/** Cantidad inicial de contenedores exteriores */
	private int outdoorCtnrCount;
	/** Total de estudiantes */
	private int studentCount;
	/** Total de trabajadores */
	private int workerCount;
	/** Total de inactivos */
	private int inactiveCount;
	/** Contador de ocupaciones */
	private int[] occupationCount;
	/** Contador de empleos faltantes */
	private int unemployedCount;
	/** Contador de bancos de estudios faltantes */
	private int unschooledCount;
	
	/** Flag para imprimir valores de inicializacion */
	static final boolean DEBUG_MSG = false;
	
	public ContextCreator() {
		// Para corridas en batch imprime fecha de compilacion
		printJarVersion(this.getClass());
	}
	
	@Override
	public Context<Object> build(Context<Object> context) {
		this.context = context;
		simulationStartTime = System.currentTimeMillis();
		
		schedule = RunEnvironment.getInstance().getCurrentSchedule();
		// Programa metodo para inicio de simulacion - para medir duracion
		ScheduleParameters params = ScheduleParameters.createOneTime(0, ScheduleParameters.FIRST_PRIORITY);
		schedule.schedule(params, this, "startSimulation");
		// Programa metodo para fin de simulacion - para imprimir reporte final
		params = ScheduleParameters.createAtEnd(ScheduleParameters.LAST_PRIORITY);
		schedule.schedule(params, this, "printSimulationDuration");
		schedule.schedule(params, this, "cleanupLists");
		
		// Crear la proyeccion para almacenar los agentes GIS (EPSG:4326).
		GeographyParameters<Object> geoParams = new GeographyParameters<Object>();
		this.geography = GeographyFactoryFinder.createGeographyFactory(null).createGeography("Geography", context, geoParams);
		
		setBachParameters(); // Lee parametros de simulacion
		// La simulacion termina a los 365 dias
		RunEnvironment.getInstance().endAt(365 * 360);
		
		// Crea BuildingManager para esta ciudad
		BuildingManager.setMainContextAndGeography(context, geography);
		buildingManager = new BuildingManager();
		
		// Lee y carga en lista las parcelas hogares
		homePlaces = new ArrayList<List<HomeAgent>>(DataSet.SECTORALS_COUNT);
		loadParcelsShapefile();
		
		if (placesProperty.isEmpty()) // Para no volver a leer si se reinicia simulacion, o si se crea otra instancia
			placesProperty = PlaceProperty.loadPlacesProperties("./data/parana-places.csv");
		// Lee y carga en BuildingManager los places
		loadPlacesShapefile();
		
		// Una vez cargados los places, crea lista de actividades disponibles
		buildingManager.createActivitiesTypeList();
		
		context.add(new InfectionReport(adultMosquitosLimit)); // Unicamente para la grafica en Repast Simphony
		context.add(new Weather(simulationStartYear)); // Para leer datos climaticos diarios y calcular tasa de desarrollo
		context.add(new AquaticAgent()); // Unicamente para la grafica en Repast Simphony
		
		// Inicializa los agentes del modelo
		initHumans();		// Humanos habitantes de pna
		initContainers();	// Contenedores in/outdoor
		initEggs();			// Acuaticos huevos
		initMosquitoes();	// Mosquitos adultos
		
		// Limpia listas temporales
		homePlaces.clear();
		workPlaces.clear();
		schoolPlaces.clear();
		
		// Programar ingreso de infectados
		scheduleInfectedArrival();
		// Programar des/cacharrizacion
		scheduleContainersControl();
		
		// Programar movimiento de Humanos
		params = ScheduleParameters.createRepeating(0, 3, 0.5); // valor por defecto - (3 ticks = 6 minutos)
		//params = ScheduleParameters.createRepeating(0, 9, 0.5); // valor para acelerar la simulacion - (9 ticks = 18 minutos)
		schedule.schedule(params, this, "switchHumanLocation");
		
		return context;
	}
	
	public void switchHumanLocation() {
		// Forma aleatoria
		//context.getRandomObjectsAsStream(HumanAgent.class, Long.MAX_VALUE)
		//	.forEach(h -> ((HumanAgent) h).switchLocation());
		// Forma secuencial (mas rapida)
		context.getObjectsAsStream(HumanAgent.class)
			.forEach(h -> ((HumanAgent) h).switchLocation());
	}
	
	public void startSimulation() {
		simulationStartTime = System.currentTimeMillis();
	}
	
	public void printSimulationDuration() {
		System.out.printf("Humanos Recuperados: %d | Mosquitos Infectados: %d%n", 
				InfectionReport.getRecoveredHumans(), InfectionReport.getTotalInfectedMosquitoes());
		System.out.printf("Acuaticos Iniciales: %d | Finales: %d | Huevos: %d%n",
				startingEggsAmount, AquaticAgent.getCount(), AquaticAgent.getEggsCount());
		System.out.printf("Cont. pos. Iniciales: %d | Finales: %d | Max. Adultos por cont.: %.2f%n",
				startingPosCtnrAmount, InfectionReport.getPositiveContainers(), InfectionReport.getMaxAdultsPerContainer());
		final long simTime = System.currentTimeMillis() - simulationStartTime;
		System.out.printf("Año: %4d | Simulacion: %4d | Seed: %d | Tiempo: %.2f horas%n",
				simulationStartYear, simulationRun, RandomHelper.getSeed(), (simTime / (double)(1000*60*60)));
	}
	
	public void cleanupLists() {
		// Limpiar lista de susceptible e indice places
		susceptibleBuildings.clear();
		susceptiblePlacesIdx = null;
		// Limpiar listas de contenedores
		indoorContainers.clear();
		outdoorContainers.clear();
	}
	
	/**
	 * Infecta Humanos y Mosquitos aleatoriamente, segun la semana actual.<p>
	 * Actualiza indice de semana de simulacion.
	 */
	public void infectRandos() {
		// Humanos
		int newInfected = humansInfEveryWeek[simumationWeekIndex];
		if (newInfected > 0)
			context.getRandomObjectsAsStream(HumanAgent.class, newInfected)
				.forEach(h -> ((HumanAgent) h).setInfectious(true));
		// Mosquitos adultos
		newInfected = mosquitoesInfEveryWeek[simumationWeekIndex];
		if (newInfected > 0)
			addInfectedMosquitos(newInfected);
		if (++simumationWeekIndex >= 48)
			simumationWeekIndex = 0;
	}
	
	/**
	 * Lee los parametros seteados en Repast Simphony, guardados en "parameters.xml".
	 */
	private void setBachParameters() {
		Parameters params = RunEnvironment.getInstance().getParameters();
		simulationStartYear	= ((Integer) params.getValue("anoInicioSimulacion")).intValue();
		adultMosquitosLimit	= ((Integer) params.getValue("cantidadMosquitosLimite")).intValue();
		fracOfInfHumans		= ((Double)  params.getValue("fracHumanosInfectados")).doubleValue();
		fracOfInfMosquitoes	= ((Double)  params.getValue("fracMosquitosInfectados")).doubleValue();
		startingEggsAmount	= ((Integer) params.getValue("cantHuevosIniciales")).intValue();
		startingAdultsAmount= ((Integer) params.getValue("cantAdultosIniciales")).intValue();
		mosquitoBitesMean	= ((Integer) params.getValue("cantMediaPicaduras")).intValue();
		simulationRun		= ((Integer) params.getValue("corridas")).intValue();
		// Parametros de eliminacion y creacion de cacharros //
		String tempStr		= ((String) params.getValue("diasEliminarContenedores"));
		containersRemoveDays= Utils.getIntArrayFromString(tempStr, " ");
		tempStr				= ((String) params.getValue("porcEliminarContenedores"));
		containersRemovePct	= Utils.getIntArrayFromString(tempStr, " ");
		tempStr				= ((String) params.getValue("diasAgregarContenedores"));
		containersAddDays	= Utils.getIntArrayFromString(tempStr, " ");
		tempStr				= ((String) params.getValue("porcAgregarContenedores"));
		containersAddPct	= Utils.getIntArrayFromString(tempStr, " ");
	}
	
	/**
	 * Toma las parcelas del shapefile, crea los hogares y los posiciona en el mapa.
	 */
	private void loadParcelsShapefile() {
		List<SimpleFeature> features = Utils.loadFeaturesFromShapefile("./data/parcels.shp");
		// Reinicia la lista de hogares y el id
		lastHomeId = 0;
		// Crea la lista de hogares para cada seccional 
		homePlaces.clear();
		for (int i = 0; i < DataSet.SECTORALS_COUNT; i++) {
			homePlaces.add(new ArrayList<HomeAgent>());
		}
		
		int id, sectoral, block, sectoralIdx;
		String strCondition;
		char condition;
		HomeAgent tempBuilding = null;
		for (SimpleFeature feature : features) {
			Geometry geom = (Geometry) feature.getDefaultGeometry();
			if (geom == null || !geom.isValid()) {
				System.err.println("Parcel invalid geometry: " + feature.getID());
				continue;
			}
			if (geom instanceof Point) {
				id			= (int) feature.getAttribute("id");
				//ogcId		= (int) feature.getAttribute("ogc_fid");
				sectoral	= (int) feature.getAttribute("sec");
				block		= (int) feature.getAttribute("manz");
				strCondition = (String) feature.getAttribute("estado");
				if (!strCondition.isEmpty())
					condition	= strCondition.charAt(0);
				else
					condition	= 'E';
				// Guarda la ultima ID de parcela, para crear ficticias
				if (id > lastHomeId)
					lastHomeId = id;
				
				// Crea el HomeAgent con los datos obtenidos
				sectoralIdx = sectoral - 1;
				tempBuilding = new HomeAgent(sectoralIdx, geom.getCoordinate(), id, block, condition);
				homePlaces.get(sectoralIdx).add(tempBuilding);
				if (!tempBuilding.isMosquitoesExcluded()) {
					susceptibleBuildings.add(tempBuilding);
					buildingManager.addBuildingToBlock(block, tempBuilding);
				}
				context.add(tempBuilding);
				geography.move(tempBuilding, geom);
			}
			else {
				System.err.println("Error creating agent for " + geom);
			}
		}
		features.clear();
	}
	
	/**
	 * Toma los places del shapefile, crea los lugares de trabajo y los posiciona en el mapa.<p>
	 * Carga en BuildingManager los places segun su tipo.
	 */
	private void loadPlacesShapefile() {
		List<SimpleFeature> features = Utils.loadFeaturesFromShapefile("./data/places.shp");
		// Reinicia las listas de lugares de trabajo y estudio
		workPlaces.clear();
		schoolPlaces.clear();
		// Para no crear otra lista con Places susceptibles,
		// reuso la de Buildings, y almaceno el indice ini y fin de Places.
		susceptiblePlacesIdx = new int[] {susceptibleBuildings.size(), 0};
		// Variables temporales
		PlaceProperty placeProp;
		PlaceProperty placeSecProp;
		int sectoral, block;
		String type;
		String[] types;
		int sectoralIdx = 0;
		int buildingArea;
		//
		@SuppressWarnings("unused")
		int schoolVacancies = 0, workVacancies = 0;
		for (SimpleFeature feature : features) {
			Geometry geom = (Geometry) feature.getDefaultGeometry();
			if (geom == null || !geom.isValid()) {
				System.err.println("Place invalid geometry: " + feature.getID() + (int) feature.getAttribute("id"));
				continue;
			}
			if (geom instanceof Point) {
				sectoral= (int) feature.getAttribute("sec");
				block	= (int) feature.getAttribute("manz");
				type	= (String) feature.getAttribute("type");
				// Separar types y tomar el primero
				types = type.split("\\+");
				placeProp = placesProperty.get(types[0]);
				if (placeProp == null) {
					System.out.println("Type de Place desconocido: " + types[0]);
					continue;
				}
				type = types[0];
				buildingArea = placeProp.getBuildingArea();
				
				// Si tiene 2 types se suma el area del segundo
				if (types.length > 1) {
					placeSecProp = placesProperty.get(types[1]);
					if (placeSecProp != null) {
						buildingArea += placeSecProp.getBuildingArea();
					}
					else {
						System.out.println("Type secundario de Place desconocido: " + types[1]);
					}
				}
				sectoralIdx = sectoral - 1;
				// Crear Agente con los atributos el Place
				WorkplaceAgent tempWorkspace = new WorkplaceAgent(sectoralIdx, geom.getCoordinate(), ++lastHomeId, block, type, placeProp.getActivityState(),
						buildingArea, placeProp.getBuildingCArea(), placeProp.getWorkersPerPlace(), placeProp.getWorkersPerArea());
				
				// Agrupar el Place con el resto del mismo type
				if (placeProp.getActivityState() == 1) { // trabajo / estudio
					if (type.contains("primary_school") || type.contains("secondary_school") || type.contains("university")) {
						schoolPlaces.add(tempWorkspace);
						schoolVacancies += tempWorkspace.getVacancy();
					}
					else {
						workPlaces.add(tempWorkspace);
						workVacancies += tempWorkspace.getVacancy();
					}
					// Si es lugar sin atencion al publico, se agrega a la lista de lugares de trabajo/estudio
					buildingManager.addWorkplace(type, tempWorkspace);
				}
				else { // ocio, otros
					workPlaces.add(tempWorkspace);
					workVacancies += tempWorkspace.getVacancy();
					// Si es lugar con atencion al publico, se agrega a la lista de actividades
					buildingManager.addPlace(sectoralIdx, tempWorkspace, placeProp);
				}
				if (!tempWorkspace.isMosquitoesExcluded()) {
					susceptibleBuildings.add(tempWorkspace);
					buildingManager.addBuildingToBlock(block, tempWorkspace);
				}
				// Agregar al contexto
				context.add(tempWorkspace);
				geography.move(tempWorkspace, geom);
			}
			else {
				System.err.println("Error creating agent for " + geom);
			}
		}
		features.clear();
		
		// Si no se agregaron nuevos Buildings, el indice inicial de Places es cero
		if (susceptibleBuildings.size() == susceptiblePlacesIdx[0])
			susceptiblePlacesIdx[0] = 0;
		susceptiblePlacesIdx[1] = susceptibleBuildings.size() - 1;
		
		if (DEBUG_MSG) {
			System.out.println("CUPO ESTUDIANTES TER/UNI: " + schoolVacancies);
			System.out.println("CUPO TRABAJADORES: " + workVacancies);
		}
	}
	
	/**
	 * Crea Humano con los parametros dados y lo agrega al contexto.
	 * @param secIndex indice seccional
	 * @param home parcela hogar
	 * @param work parcela trabajo o null
	 * @return nuevo agente <b>HumanAgent</b>
	 */
	private HumanAgent createHuman(int secIndex, BuildingAgent home, BuildingAgent work) {
		HumanAgent tempHuman = new HumanAgent(secIndex, home, work);
		context.add(tempHuman);
		return tempHuman;
	}
	
	/**
	 * Crea Humanos extranjeros, sin hogar pero con lugar de trabajo.
	 * @param work parcela trabajo
	 * @return nuevo agente <b>HumanAgent</b>
	 */
	private HumanAgent createForeignHuman(BuildingAgent work) {
		int secIndex = 0;
		// Se le asigna una posicion fija en el trabajo, si es que trabaja
		if (work instanceof WorkplaceAgent) {
			// Si tiene trabajo se le asigna como hogar la seccional del lugar donde trabaja
			secIndex = work.getSectoralIndex();
		}
		HumanAgent tempHuman = new ForeignHumanAgent(secIndex, work);
		context.add(tempHuman);
		return tempHuman;
	}

	/**
	 * Crea Humanos y asigna lugar de trabajo y vivienda.
	 */
	private void initHumans() {
		int i, j, homeIdx;
		final int sectoralsCount = DataSet.SECTORALS_COUNT;
		int[] locals = new int[sectoralsCount];
		int[] localTravelers = new int[sectoralsCount];
		int[] homePlacesCount = new int[sectoralsCount];
		
		HomeAgent tempHome = null;
		BuildingAgent tempJob = null;
		unemployedCount = 0;
		unschooledCount = 0;
		
		HumanAgent.initAgentID(buildingManager); // Reinicio contador de IDs
		loadHumansAmount(locals, localTravelers);
		loadOccupationalNumbers();
		
		// Guarda la cantidad de Home por seccional,
		// para asignar inicialmente un Human por Home
		for (i = 0; i < sectoralsCount; i++)
			homePlacesCount[i] = homePlaces.get(i).size();
		
		Uniform disUniHomesIndex;
		// Primero se crean los locales, pero que trabajan o estudian fuera
		for (i = 0; i < sectoralsCount; i++) {
			disUniHomesIndex = RandomHelper.createUniform(0, homePlaces.get(i).size()-1);
			for (j = 0; j < localTravelers[i]; j++) {
				// Asigna Home sin ocupar, de lo contrario selecciona al azar
				if (homePlacesCount[i] > 0)
					homeIdx = --homePlacesCount[i];
				else
					homeIdx = disUniHomesIndex.nextInt();
				tempHome = homePlaces.get(i).get(homeIdx);
				//
				createHuman(i, tempHome, null);
				// Se resta primero la capacidad de estudiantes y por ultimo trabajadores
				if (occupationCount[0] > 0)
					--occupationCount[0];
				else if (occupationCount[1] > 0)
					--occupationCount[1];
			}
		}
		
		// Segundo se crean los 100% locales
		for (i = 0; i < sectoralsCount; i++) {
			disUniHomesIndex = RandomHelper.createUniform(0, homePlaces.get(i).size()-1);
			for (j = 0; j < locals[i]; j++) {
				// Asigna Home sin ocupar, de lo contrario selecciona al azar
				if (homePlacesCount[i] > 0)
					homeIdx = --homePlacesCount[i];
				else
					homeIdx = disUniHomesIndex.nextInt();
				tempHome = homePlaces.get(i).get(homeIdx);
				tempJob = findWorkingPlace(tempHome);
				//
				createHuman(i, tempHome, tempJob);
			}
		}
		
		// Por ultimo se crean los extranjeros, si ya no quedan cupos de trabajo trabajan fuera
		for (i = 0; i < DataSet.FOREIGN_TRAVELER_HUMANS; i++) {
			tempJob = findWorkingPlace();
			createForeignHuman(tempJob);
		}
		
		if (DEBUG_MSG) {
			if (unschooledCount != 0)
				System.out.println("CUPO ESTUDIANTES FALTANTES: " + unschooledCount);
			if (unemployedCount != 0)
				System.out.println("CUPO TRABAJADORES FALTANTES: " + unemployedCount);
		}
	}
	
	/**
	 * Calcula la cantidad de humanos locales, locales viajeros y extranjeros; por seccional.
	 * @param locals array locales
	 * @param localTravelers array locales que trabajan afuera
	 */
	private void loadHumansAmount(int[] locals, int[] localTravelers) {
		// Inicia cantidad de humanos e indices
		localHumansCount = 0;
		// Calcula la cantidad de humanos en el contexto de acuerdo a la poblacion del municipio y la distribucion por seccional
		for (int i = 0; i < DataSet.SECTORALS_COUNT; i++) {
			// Guarda la cantidad de humanos que viven en contexto
			locals[i] = (int) (DataSet.LOCAL_HUMANS * DataSet.SECTORALS_POPULATION[i]) / 100;
			localHumansCount += locals[i];
			localTravelers[i] = (int) (DataSet.LOCAL_TRAVELER_HUMANS * DataSet.SECTORALS_POPULATION[i]) / 100;
			locals[i] -= localTravelers[i];
		}
	}
	
	/**
	 * Calcula la cantidad de estudiantes, trabajadores e inactivos locales.
	 */
	private void loadOccupationalNumbers() {
		studentCount = (int) (localHumansCount * DataSet.OCCUPATION_CHANCE[0]) / 100;
		workerCount = (int) (localHumansCount * DataSet.OCCUPATION_CHANCE[1]) / 100;
		inactiveCount = localHumansCount - studentCount - workerCount;
		occupationCount = new int[] {studentCount, workerCount, inactiveCount};
		
		if (DEBUG_MSG) {
			System.out.println("ESTUDIANTES LOCALES: " + studentCount);
			System.out.println("TRABAJADORES LOCALES: " + workerCount);
			System.out.println("INACTIVOS LOCALES: " + inactiveCount);
		}
	}
	
	/**
	 * Busca lugar de trabajo/estudio si hay cupo.
	 * @param home hogar de humano
	 * @return <b>BuildingAgent</b> o <b>null</b>
	 */
	private BuildingAgent findWorkingPlace(BuildingAgent home) {
		BuildingAgent workplace = null;
		int rndOcc = RandomHelper.nextIntFromTo(1, localHumansCount);
        int occ = 0;
        // Recorre la matriz de ocupaciones
        while (rndOcc > occupationCount[occ]) {
        	// Avanza el indice de ocupacion, si es posible restar probabilidad
        	rndOcc -= occupationCount[occ];
        	++occ;
        }
        --occupationCount[occ];
        --localHumansCount;
        
		if (occ == 0) { // estudiante
    		workplace = findWorkplace(schoolPlaces);
        	if (workplace == null) {
        		workplace = home;
        		++unschooledCount;
        	}
		}
		else if (occ == 1) { // trabajor
        	int wp = RandomHelper.nextIntFromTo(1, 100);
        	// Primero ver si tiene un trabajo convencional
        	if (wp <= DataSet.WORKING_FROM_HOME + DataSet.WORKING_OUTDOORS) {
        		// Si no, puede trabajar en la casa o al exterior
        		wp = RandomHelper.nextIntFromTo(1, DataSet.WORKING_FROM_HOME + DataSet.WORKING_OUTDOORS);
        		if (wp <= DataSet.WORKING_FROM_HOME)
        			workplace = home;
        		else
        			workplace = null;
        	}
        	else {
	        	workplace = findWorkplace(workPlaces);
	        	if (workplace == null)
	        		++unemployedCount;
        	}
		}
		else { // inactivo
			workplace = home;
		}
		//
		return workplace;
	}
	
	/**
	 * Busca lugar de trabajo/estudio para extranjeros.
	 * @return <b>BuildingAgent</b> o <b>null</b>
	 */
	private BuildingAgent findWorkingPlace() {
		BuildingAgent workplace = null; // inactivo por defecto
		int rndOcc = RandomHelper.nextIntFromTo(1, DataSet.OCCUPATION_CHANCE[0] + DataSet.OCCUPATION_CHANCE[1]);
	    int occ = 0;
	    // Recorre la matriz de ocupaciones
	    while (rndOcc > DataSet.OCCUPATION_CHANCE[occ]) {
	    	// Avanza el indice de ocupacion, si es posible restar probabilidad
	    	rndOcc -= DataSet.OCCUPATION_CHANCE[occ];
	    	++occ;
	    }
	    
		if (occ == 0) { // estudiante
			workplace = findWorkplace(schoolPlaces);
		}
		else if (occ == 1) { // trabajor
	        workplace = findWorkplace(workPlaces);
		}
		//
		return workplace;
	}
	
	/**
	 * Busca y resta una posicion de trabajador en la lista de lugares.
	 * @param wpList lista de WorkplaceAgents
	 * @return <b>WorkplaceAgent</b> o <b>null</b>
	 */
	private WorkplaceAgent findWorkplace(List<WorkplaceAgent> wpList) {
		int index;
		WorkplaceAgent workplace = null;
		if (!wpList.isEmpty()) {
			index = RandomHelper.nextIntFromTo(0, wpList.size()-1);
			workplace = wpList.get(index);
			workplace.reduceVacancies();
			if (!workplace.vacancyAvailable())
				wpList.remove(index);
		}
		return workplace;
	}
	
	/**
	 * Crea contenedores en cada parcela donde se permita el Aedes.
	 * @param context
	 * @see DataSet.CONTAINERS_PER_HOUSE_MEAN
	 */
	private void initContainers() {
		ContainerAgent.initAgentID(); // Reiniciar ID de contenedores
		indoorContainers.clear();
		outdoorContainers.clear();
		
		double areaMean   = DataSet.CONTAINER_AREA_MEAN;
		double areaStd    = DataSet.CONTAINER_AREA_DEVIATION;
		double heightMean = DataSet.CONTAINER_HEIGHT_MEAN;
		double heightStd  = DataSet.CONTAINER_HEIGHT_DEVIATION;
		
		// Dist. normal cantidad containers por Home
		final Normal ndContainersPerHouse[] = new Normal[DataSet.SECTORALS_COUNT];
	    // Dist. uniforme chance de container en interiores
	    final Uniform udContainerIndoor = RandomHelper.createUniform(1, 100);
	    // Dist. normal area de containers
	    final Normal ndContainerArea   = RandomHelper.createNormal(areaMean, areaStd);
	    // Dist. normal altura de containers
	    final Normal ndContainerHeight = RandomHelper.createNormal(heightMean, heightStd);
	    
		for (int i = 0; i < DataSet.SECTORALS_COUNT; i++)
			ndContainersPerHouse[i] = RandomHelper.createNormal(DataSet.CONTAINERS_PER_HOUSE_MEAN[i], DataSet.CONTAINERS_PER_HOUSE_DEVIATION[i]);
		
		susceptibleBuildings.forEach(tempBuilding -> {
    		// La dist. normal de cantidad de contenedores no la limito (puede dar menor a 0)
	    	for (int i = ndContainersPerHouse[tempBuilding.getSectoralIndex()].nextInt(); i > 0; i--) {
	    		final boolean contIndoor = (udContainerIndoor.nextInt() <= DataSet.CONTAINER_INDOOR_CHANCE);
				double contArea = ndContainerArea.nextDouble();
				double contHeight = ndContainerHeight.nextDouble();
				// Limito el valor de area y altura a la media + desvio
				// por que si no dan valores negativos y no entra ni un huevo 
				contArea = Utils.limitStandardDeviation(contArea, areaMean, areaStd);
				contHeight = Utils.limitStandardDeviation(contHeight, heightMean, heightStd);
				//
				ContainerAgent container = new ContainerAgent(tempBuilding, contIndoor, contArea, contHeight);
				tempBuilding.insertContainer(container);
				if (contIndoor)	indoorContainers.add(container);
				else			outdoorContainers.add(container);
				context.add(container);
			}
		});
		outdoorCtnrCount = outdoorContainers.size();
		if (DEBUG_MSG) {
			System.out.println("CANTIDAD CONTAINERS INTERIOR: " + indoorContainers.size());
			System.out.println("CANTIDAD CONTAINERS EXTERIOR: " + outdoorCtnrCount);
		}
	}
	
	private void initEggs() {
		AquaticAgent.initAgentID(); // Reiniciar ID de acuaticos
		
		int[] ciIndexes = IntStream.range(0, outdoorContainers.size()).toArray();
		int indexesCount = outdoorContainers.size()-1;
		int randomIndex;
		
		ContainerAgent container = null;
		double eggLife, eggContainerHeight;
		startingPosCtnrAmount = outdoorContainers.size() * DataSet.CONTAINER_OCCUPANCY_RATE / 100;
		final int aquaticPerContainer = startingEggsAmount / startingPosCtnrAmount;
		//
		int layedEggs = 0; 
		do {
			if (indexesCount >= 0) { // Si quedan contenedores inside
				randomIndex = RandomHelper.nextIntFromTo(0, indexesCount);
				container = outdoorContainers.get(ciIndexes[randomIndex]);
				ciIndexes[randomIndex] = ciIndexes[indexesCount--];
			}
			else {
				System.out.printf("FALTAN CONTAINERS PARA HUEVOS INICIALES: %d / %d%n", layedEggs, startingEggsAmount);
				break; // faltaron containers
			}
			//
			int aquaticsCreated = 0;
			// Agrega acuaticos hasta que se pasen del promedio, o no entren mas
			eggLife = RandomHelper.nextDoubleFromTo(.0, .5); // Toda la camada (ovoposicion) tiene la misma edad
			eggContainerHeight = Utils.getStdNormalDeviate(DataSet.INITIAL_EGG_ELEVATION_MEAN, DataSet.INITIAL_EGG_ELEVATION_DEVIATION); // cm
			for (int j = 0; j < DataSet.ADULT_EGGS_PRODUCTION; j++) {
				++aquaticsCreated;
				if (!container.addAquatic(new AquaticAgent(container, 0, eggLife, eggContainerHeight))) {
					break; // container lleno
				}
				if (aquaticsCreated >= aquaticPerContainer)
					break; // tope de huevos por container
			}
			layedEggs += aquaticsCreated;
		} while (layedEggs < startingEggsAmount);
	}
	
	/**
	 * Crea mosquitos adultos en las mismas parcelas que tienen contenedores adentro o en parcelas aleatorias. 
	 */
	private void initMosquitoes() {
		MosquitoAgent.initAgent(context, buildingManager, mosquitoBitesMean); // Reiniciar ID de mosquitos
		MosquitoAgent tempMosquito;
		int[] ciIndexes = IntStream.range(0, indoorContainers.size()).toArray();
		int indexesCount = indoorContainers.size()-1;
		int randomIndex;
		BuildingAgent building;
		
		for (int i = 0; i < startingAdultsAmount; i++) {
			if (indexesCount >= 0) { // Si quedan contenedores inside
				randomIndex = RandomHelper.nextIntFromTo(0, indexesCount);
				building = indoorContainers.get(ciIndexes[randomIndex]).getBuilding();
				ciIndexes[randomIndex] = ciIndexes[indexesCount--];
			}
			else { // Si no hay mas contenedores inside, busca un building random
				randomIndex = RandomHelper.nextIntFromTo(0, susceptibleBuildings.size()-1);
				building = susceptibleBuildings.get(randomIndex);
			}
			tempMosquito = new MosquitoAgent(building);
			context.add(tempMosquito);
		}
	}
	
	/**
	 * Calcula la cantidad de nuevos infectados por semana y
	 * retorna el indice de semana y los dias de offset de inicio de simulacion.
	 * @param infectedCount cantidad anual a infectar
	 * @param monthlyPct porcentajes mensuales
	 * @param infEveryWeek infectados semanales
	 * @return <b>int[]</b> con incide semana y dias de offset de inicio
	 */
	private int[] getInfArrivalPerWeek(int infectedCount, int[] monthlyPct, int[] infEveryWeek) {
		// Calcular cantidad por semana
		int weekIdx = 0; // Indice semanal
		for (int pct : monthlyPct) {
			int infMonthly = (int) Math.round((infectedCount * pct) / 100.0);
			int infWeekly = infMonthly >> 2;
			infEveryWeek[weekIdx++] = infWeekly;
			infMonthly -= infWeekly;
			infEveryWeek[weekIdx++] = infWeekly;
			infMonthly -= infWeekly;
			infEveryWeek[weekIdx++] = infWeekly;
			infMonthly -= infWeekly;
			infEveryWeek[weekIdx++] = infMonthly;
		}
		
		// Calcular indice anual de semana y dias de offset, segun fecha de inicio
		int startingDate = Weather.getCurrentDay();
		int startingWeek = (int) Math.round(startingDate / 7.6); // dias a semanas
		// Calcular dia de offset
		int startingDaysOffset = (int) (startingWeek * 7.6) - (startingDate - 1);
		if (startingDaysOffset < 0) {
			startingDaysOffset += 7;
			++startingWeek;
		}
		// Limitar indice a 48 semanas anuales
		if (startingWeek >= 48)
			startingWeek -= 48;
		return new int[] {startingWeek, startingDaysOffset};
	}
	
	/**
	 * Calcula la cantidad de nuevos infectados por semana
	 * para todo el año y programa su ingreso semanal.
	 */
	private void scheduleInfectedArrival() {
		// Obtener cantidad de humanos infectados por semana
		int infHumansCount = (int) Math.round(fracOfInfHumans * DataSet.LOCAL_HUMANS);
		humansInfEveryWeek = new int[48]; // semanal
		getInfArrivalPerWeek(infHumansCount, DataSet.TRAVELERS_MONTHLY_PCT, humansInfEveryWeek);
		// Obtener cantidad de mosquitos adultos infectados por semana
		int infMosquitoesCount = (int) Math.round(fracOfInfMosquitoes * DataSet.LOCAL_HUMANS);
		mosquitoesInfEveryWeek = new int[48]; // semanal
		int[] startDays = getInfArrivalPerWeek(infMosquitoesCount, DataSet.TOURISTS_MONTHLY_PCT, mosquitoesInfEveryWeek);
		// Programar metodo de ingreso semanal de infectados
		simumationWeekIndex = startDays[0];
		ScheduleParameters params = ScheduleParameters.createRepeating(
				startDays[1] * 360,
				7.6 * 360, ScheduleParameters.FIRST_PRIORITY);
		schedule.schedule(params, this, "infectRandos");
	}
	
	/**
	 * Programar des/cacharrizacion segun los dias y porcentajes leidos en parametros.
	 */
	private void scheduleContainersControl() {
		ScheduleParameters params;
		// Programa las acciones de descacharrizacion
		if (containersRemoveDays.length == containersRemovePct.length) {
			for (int i = 0; i < containersRemoveDays.length; i++) {
				params = ScheduleParameters.createOneTime(containersRemoveDays[i] * 360, ScheduleParameters.FIRST_PRIORITY);
				schedule.schedule(params, this, "removeContainers", i);
			}
		}
		else
			System.err.println("Cantidad de dias en que se eliminan contenedores != porcentaje");
		// Programa las acciones de cacharrizacion
		if (containersAddDays.length == containersAddPct.length) {
			for (int i = 0; i < containersAddDays.length; i++) {
				params = ScheduleParameters.createOneTime(containersAddDays[i] * 360, ScheduleParameters.FIRST_PRIORITY);
				schedule.schedule(params, this, "addContainers", i);
			}
		}
		else
			System.err.println("Cantidad de dias en que se agregan contenedores != porcentaje");
	}
	
	/**
	 * Elimina aleatoriamente contenedores al exterior, matando los acuaticos que contengan.
	 * @param index en el listado de dias
	 */
	public void removeContainers(int index) {
		int ctnrCount = outdoorContainers.size();
		int rndIndex;
		ContainerAgent container;
		// Paso porcentaje a cantidad
		containersRemovePct[index] *= outdoorCtnrCount / 100;
		while (containersRemovePct[index] > 0 && ctnrCount > 0) {
			rndIndex = RandomHelper.nextIntFromTo(0, ctnrCount - 1);
			container = outdoorContainers.remove(rndIndex);
	    	container.remove();
	    	--containersRemovePct[index];
	    	--ctnrCount;
		}
		if (DEBUG_MSG)
			System.out.println("Cantidad de contenedores al exterior reducida: " + outdoorContainers.size());
	}
	
	/**
	 * Crea nuevos contenedores al exterior y los distribuye aleatoriamente en parcelas.
	 * @param index en el listado de dias
	 */
	public void addContainers(int index) {
		final double areaMean   = DataSet.CONTAINER_AREA_MEAN;
		final double areaStd    = DataSet.CONTAINER_AREA_DEVIATION;
		final double heightMean = DataSet.CONTAINER_HEIGHT_MEAN;
		final double heightStd  = DataSet.CONTAINER_HEIGHT_DEVIATION;
		
	    // Dist. normal area de containers
	    final Normal ndContainerArea   = RandomHelper.createNormal(areaMean, areaStd);
	    // Dist. normal altura de containers
	    final Normal ndContainerHeight = RandomHelper.createNormal(heightMean, heightStd);
		
		// Pasar porcentaje a cantidad
		containersAddPct[index] *= outdoorCtnrCount / 100;
		final int buildingsCount = susceptibleBuildings.size()-1;
		for (int i = 0; i < containersAddPct[index]; i++) {
			int randomIndex = RandomHelper.nextIntFromTo(0, buildingsCount);
			BuildingAgent building = susceptibleBuildings.get(randomIndex);
			//
			double contArea   = ndContainerArea.nextDouble();
			double contHeight = ndContainerHeight.nextDouble();
			// Limito el valor de area y altura a la media + desvio
			// por que si no dan valores negativos y no entra ni un huevo 
			contArea   = Utils.limitStandardDeviation(contArea, areaMean, areaStd);
			contHeight = Utils.limitStandardDeviation(contHeight, heightMean, heightStd);
			//
			ContainerAgent container = new ContainerAgent(building, false, contArea, contHeight);
			building.insertContainer(container);
			outdoorContainers.add(container);
			context.add(container);
		}
		if (DEBUG_MSG)
			System.out.println("Cantidad de contenedores al exterior incrementada: " + outdoorContainers.size());
	}
	
	/**
	 * Crear mosquitos adultos infectados en workplaces habitables aleatorios.
	 * @param amount cantidad a crear
	 */
	private void addInfectedMosquitos(int amount) {
		MosquitoAgent adultMosquito;
		BuildingAgent building;
		int randomIndex;
		while (amount > 0) {
			randomIndex = RandomHelper.nextIntFromTo(susceptiblePlacesIdx[0], susceptiblePlacesIdx[1]);
			building = susceptibleBuildings.get(randomIndex);
			adultMosquito = new MosquitoAgent(building);
			adultMosquito.infectTransmitter();
			context.add(adultMosquito);
			--amount;
		}
	}
	
	/**
	 * Imprime la hora de compilacion del jar (si existe).
	 * @param cl clase actual
	 */
	private static void printJarVersion(Class<?> cl) {
	    try {
	        String rn = cl.getName().replace('.', '/') + ".class";
	        JarURLConnection j = (JarURLConnection) cl.getClassLoader().getResource(rn).openConnection();
	        long totalMS = j.getJarFile().getEntry("META-INF/MANIFEST.MF").getTime();
	        // Convierte de ms a formato fecha hora
			SimpleDateFormat sdFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
			System.out.println("Fecha y hora de compilacion: " + sdFormat.format(totalMS));
	    } catch (Exception e) {
	    	// Si no es jar, no imprime hora de compilacion
	    }
	}
}
