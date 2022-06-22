package geoaedes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import geoaedes.agents.BuildingAgent;
import geoaedes.agents.HumanAgent;
import geoaedes.agents.InfectedHumanAgent;
import geoaedes.agents.InfectionSpotAgent;
import geoaedes.agents.WorkplaceAgent;
import repast.simphony.context.Context;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.gis.Geography;

/**
 * Contiene los places de trabajo, ocio y otros.<p>
 * Implementa metodos para asignar places segun la actividad del humano.<p>
 * Tambien controla la creacion y desplazamiento del indicador de humano infectado en mapa.
 */
public final class BuildingManager {
	private static Context<Object> mainContext; // Para agregar indicador de humano infectado
	private static Geography<Object> geography; // Para agregar indicador de humano infectado en mapa
	private static GeometryFactory geometryFactory = new GeometryFactory(); // Para crear punto al azar
	
	/** Manzanas con parcelas habitables por mosquitos */
	private Map<Integer, List<BuildingAgent>> blocksMap = new HashMap<>();
	
	/** Lugares de entretenimiento / otros, separados por seccional */
	private Map<String, List<List<WorkplaceAgent>>> placesMap = new HashMap<>();
	/** Lugares unicamente de trabajo */
	private Map<String, List<WorkplaceAgent>> workplacesMap = new HashMap<>();
	/** Indicadores en mapa, de humanos infectados */
	private Map<Integer, InfectedHumanAgent> infectedHumans = new HashMap<>();
	/** Indicadores en mapa, de hogar de humanos infectados */
	private Map<Integer, InfectionSpotAgent> infectionSpots = new HashMap<>();
	
	/** Cantidad de Places de cada tipo en cada seccional */
	private Map<String, int[]> placesCount = new HashMap<>();
	/** Suma total de Places de cada tipo */
	private Map<String, Integer> placesTotal = new HashMap<>();
	
	/** Temporal - Places de entretenimiento */
	private List<PlaceProperty> enterPropList = new ArrayList<PlaceProperty>();
	/** Temporal - Places de otros */
	private List<PlaceProperty> otherPropList = new ArrayList<PlaceProperty>();
	
	/** Types que estan disponibles en el SHP de places */
	private String[] entertainmentTypes;
	/** Types que estan disponibles en el SHP de places */
	private String[] otherTypes;
	
	private int sectoralsCount; // Puntero
	
	/**
	 * Reinicia colecciones de Places, y guarda referencia de SubContext y cantidad de seccionales
	 * @param subContext sub contexto municipio
	 * @param sectorals cantidad de seccionales en municipio
	 */
	public BuildingManager() {
		blocksMap.clear(); // Por si cambio el SHP entre corridas
		placesMap.clear(); // Por si cambio el SHP entre corridas
		workplacesMap.clear(); // Por si cambio el SHP entre corridas
		infectedHumans.clear(); // Por si quedo algun infeccioso
		//
		enterPropList.clear(); // Por las dudas
		otherPropList.clear(); // Por las dudas
		//
		placesCount.clear(); // Por las dudas
		placesTotal.clear(); // Por las dudas
		// 
		this.sectoralsCount = DataSet.SECTORALS_COUNT;
	}
	
	public static void setMainContextAndGeography(Context<Object> con, Geography<Object> geo) {
		mainContext = con;
		geography = geo;
	}
	
	/**
	 * Agrega a manzana una parcela donde pueden habitar mosquitos
	 * @param blockId id manzana
	 * @param building parcela
	 */
	public void addBuildingToBlock(int blockId, BuildingAgent building) {
		if (!blocksMap.containsKey(blockId)) {
			blocksMap.put(blockId, new ArrayList<BuildingAgent>());
		}
		blocksMap.get(blockId).add(building);
	}
	
	/**
	 * Agregar el Workplace a la lista que pertenece segun la actividad.
	 * @param secIndex indice seccional
	 * @param build WorkplaceAgent a agregar
	 * @param prop PlaceProperty de place
	 */
	public void addPlace(int secIndex, WorkplaceAgent build, PlaceProperty prop) {
		String type = prop.getGooglePlaceType();
		// Chequear si ya se agregaron de este tipo
		if (placesMap.containsKey(type)) {
			// Agregar el nuevo Building
			placesMap.get(type).get(secIndex).add(build);
			// Sumar 1 mas a la seccional que corresponda
			++placesCount.get(type)[secIndex];
		}
		// Si es el primer Place de su tipo, inicializar listas que van en Maps
		else {
			// Crear una lista del nuevo tipo de Place, para cada seccional
			List<List<WorkplaceAgent>> buildList = new ArrayList<List<WorkplaceAgent>>(sectoralsCount);
			for (int i = 0; i < sectoralsCount; i++) {
				buildList.add(new ArrayList<WorkplaceAgent>());
			}
			// Agregar el nuevo Building
			buildList.get(secIndex).add(build);
			placesMap.put(type, buildList);
			// Sumar 1 mas a la seccional que corresponda
			placesCount.put(type, new int[sectoralsCount]);
			++placesCount.get(type)[secIndex];
			//
			if (prop.getActivityState() == 2) // Ocio
				enterPropList.add(prop);
			else
				otherPropList.add(prop);
		}
	}
	
	/**
	 * Agregar el Workplace a la lista que pertenece segun Type de lugar de trabajo.
	 * @param type tipo de place
	 * @param build WorkplaceAgent a agregar
	 */
	public void addWorkplace(String type, WorkplaceAgent build) {
		// Chequear si ya se agregaron de este tipo
		if (workplacesMap.containsKey(type)) {
			workplacesMap.get(type).add(build);
		}
		// Si es el primer Workplace de su tipo, inicializar lista que va en Map
		else {
			List<WorkplaceAgent> buildList = new ArrayList<WorkplaceAgent>();
			buildList.add(build);
			workplacesMap.put(type, buildList);
		}
	}
	
	/**
	 * Garga cada sub tipo y las guarda en los array dados.
	 * @param propList lista de PlaceProperty
	 * @param types tipos principales de places
	 */
	private void fillActivitiesType(List<PlaceProperty> propList, String[] types) {
		PlaceProperty pp;
		for (int i = 0; i < propList.size(); i++) {
			pp = propList.get(i);
			types[i] = pp.getGooglePlaceType();
		}
	}
	
	/**
	 * Crea arrays con los subtipos de cada actividad.
	 */
	public void createActivitiesTypeList() {
		// Guarda los subtipos de actividades tipo ocio
		entertainmentTypes = new String[enterPropList.size()];
		fillActivitiesType(enterPropList, entertainmentTypes);
		// Guarda los subtipos de actividades tipo otros
		otherTypes = new String[otherPropList.size()];
		fillActivitiesType(otherPropList, otherTypes);
		
		// Ya no tienen uso estas listas temporales
		enterPropList.clear();
		otherPropList.clear();
		
		// Guarda en otro Map la suma total de cada tipo de Place
		placesCount.forEach((key, value) -> {
			int placesSum = value[0];
			for (int i = value.length - 1; i > 0; i--)
				placesSum += value[i];
			placesTotal.put(key, placesSum);
		});
	}
	
	/**
	 * Selecciona de todas las actividades disponibles, una a realizar. 
	 * @param types tipos de actividades (places)
	 * @return <b>String</b> tipo de actividad (place)
	 */
	public String findNewPlaceType(String[] types) {
        final int rndIdx = RandomHelper.nextIntFromTo(0, types.length-1);
    	return types[rndIdx];
	}
	
	/**
	 * Busca una actividad a realizar segun parametros y retorna un nuevo place.
	 * @param secIndex indice seccional
	 * @param state indice estado markov
	 * @param human agente humano
	 * @param currentBuilding parcela actual o null
	 * @return <b>BuildingAgent</b> o <b>null</b>
	 */
	public BuildingAgent findRandomPlace(int secIndex, int state, HumanAgent human, BuildingAgent currentBuilding) {
		BuildingAgent randomPlace;
		String newActivity;
		if (state == 2) // Entretenimiento
			newActivity = findNewPlaceType(entertainmentTypes);
		else // Otros
			newActivity = findNewPlaceType(otherTypes);
		
		boolean getOut = false;
		if (currentBuilding == null) {
			// Si no esta en un Building, va a una de las seccionales aleatoriamente
			secIndex = RandomHelper.nextIntFromTo(0, sectoralsCount - 1);
		}
		else if (currentBuilding.getSectoralIndex() == secIndex) {
			// Si esta en otro barrio se queda, hasta volver a casa
			if (RandomHelper.nextIntFromTo(1, 100) <= DataSet.TRAVEL_OUTSIDE_CHANCE)
				getOut = true;
		}
		
		// Busca el place de salida de acuerdo a disponibilidad
		int[] placeSecCount = placesCount.get(newActivity); // cantidad de places por seccional segun actividad
		// Si la actividad no esta disponible en la seccional actual, viaja afuera
		if ((!getOut) && (placeSecCount[secIndex] == 0)) {
			getOut = true;
		}
		randomPlace = getRandomPlace(newActivity, getOut, placesTotal.get(newActivity), placeSecCount, secIndex);
    	return randomPlace;
	}
	
	/**
	 * Selecciona un place al azar, de acuerdo a los parametros dados.
	 * @param activity tipo de actividad
	 * @param switchSectoral cambiar de seccional
	 * @param totalPCount cantidad total de places
	 * @param sectoralPCount cantidad de places por seccional
	 * @param currentSectoral indice de seccional actual
	 * @return <b>BuildingAgent</b>
	 */
	private BuildingAgent getRandomPlace(String activity, boolean switchSectoral, int totalPCount, int[] sectoralPCount, int currentSectoral) {
		int rndPlaceIndex;
    	if (switchSectoral) {
    		// Si le toca cambiar de seccional, busca a que seccional ir
    		int placesSum = totalPCount - sectoralPCount[currentSectoral]; // restar la cantidad de la seccional donde esta
    		rndPlaceIndex = RandomHelper.nextIntFromTo(0, placesSum - 1);
    		for (int i = 0; i < sectoralPCount.length; i++) {
    			if (i == currentSectoral) // saltea su propia seccional
    				continue;
    			// La seccional que tenga mayor cantidad de places de la actividad, tiene mayor chance
	    		if (rndPlaceIndex < sectoralPCount[i]) {
	    			currentSectoral = i; // seccional seleccionada
	    			break;
	    		}
	    		rndPlaceIndex -= sectoralPCount[i];
    		}
    	}
    	else {
    		// Busca un lugar aleatorio en la seccional donde esta
    	 	rndPlaceIndex = RandomHelper.nextIntFromTo(0, sectoralPCount[currentSectoral] - 1);
    	}
    	return placesMap.get(activity).get(currentSectoral).get(rndPlaceIndex);
	}
	
	
	/**
	 * Busca aleatoriamente en la distancia de vuelo todos las parcelas con humanos,<p>
	 * guarda hasta 4 candidatos y elige el mas poblado.
	 * @param currentBuilding  edificio actual donde se encuentra el Humano, o null
	 * @see DataSet.ADULT_MAX_TRAVEL_DISTANCE
	 * @return BuildingAgent nuevo si encontro, o el mismo enviado como parametro
	 */
	public BuildingAgent findPlaceWithHumans(BuildingAgent currentBuilding) {
		BuildingAgent foundedPlace = null;
		BuildingAgent target;
		//
		
		List<BuildingAgent> blockBuildings = blocksMap.get(currentBuilding.getBlock());
		// Si es el unico edifico en la manzana (no deberia) retorna el actual
		if (blockBuildings.size() == 1)
			return currentBuilding;
		
		// Buscar al azar hasta 4 buildings con humanos, en el radio de vuelo
		List<BuildingAgent> targets = new ArrayList<BuildingAgent>();
		int[] ciIndexes = IntStream.range(0, blockBuildings.size()).toArray();
		int indexesCount = ciIndexes.length - 1;
		int randomIndex;
		while (indexesCount >= 0) {
			randomIndex = RandomHelper.nextIntFromTo(0, indexesCount);
			target = blockBuildings.get(ciIndexes[randomIndex]);
			ciIndexes[randomIndex] = ciIndexes[indexesCount--];
			//
			if (!target.equals(currentBuilding)) {
				if (target.getHumansAmount() > 0) {
					final double distance = currentBuilding.getCoordinate().distance(target.getCoordinate());
					if (distance < DataSet.ADULT_MAX_TRAVEL_DISTANCE) {
						targets.add(target);
						// Elige maximo 4 targets
						if (targets.size() == 4)
							break;
					}
				}
			}
		}
		
		if (!targets.isEmpty()) {
			// Si encuentra edificios con humanos, los ordena por cantidad ascendente de humanos
			targets.sort(Comparator.comparingInt(BuildingAgent::getHumansAmount));
			foundedPlace = targets.get(targets.size()-1);
		}
		else {
			// Si no encuentra lugares con humanos, se queda donde esta
	    	foundedPlace = currentBuilding;
	    }
	    return foundedPlace;
	}
	
	/**
	 * Busca en la distancia de vuelo, el building mas cercano que tenga containers al exterior.
	 * @param currentBuilding  edificio actual donde se encuentra el Mosquito
	 * @see DataSet.ADULT_MAX_TRAVEL_DISTANCE
	 * @return BuildingAgent nuevo si encontro, o el mismo enviado como parametro
	 */
	public BuildingAgent findPlaceWithContainers(BuildingAgent currentBuilding) {
		BuildingAgent targetBuilding = null;
		double minDistance = DataSet.ADULT_MAX_TRAVEL_DISTANCE;
		
		List<BuildingAgent> targets = blocksMap.get(currentBuilding.getBlock());
		// Si es el unico edifico en la manzana (no deberia) retorna el actual
		if (targets.size() == 1)
			return currentBuilding;
		
		// Busca building cercano que tenga containers al exterior
		for (BuildingAgent target : targets) {
			if (target.equals(currentBuilding))
				continue;
			if (target.getOutdoorCntrAmount() > 0) {
				final double distance = currentBuilding.getCoordinate().distance(target.getCoordinate());
				if (distance < minDistance) {
					minDistance = distance;
					targetBuilding = target;
				}
			}
		}
		// Si no encuentra containers afuera, selecciona al azar
		if (targetBuilding == null) {
			do {
				targetBuilding = targets.get(RandomHelper.nextIntFromTo(0, targets.size()-1));
			} while (targetBuilding.equals(currentBuilding));
		}
	    return targetBuilding;
	}
	
	/**
	 * Crear una geometria con circunferencia de 50 metros aprox (en zona Parana).
	 * @param coordinate coordenadas del centro del circulo
	 * @return Geometry
	 */
	private Geometry createCircleGeometry(Coordinate coordinate) {
		Point pointGeom = geometryFactory.createPoint(coordinate);
		return pointGeom.buffer(0.00045d);
	}
	
	/**
	 * Crea un circulo en las coordenadas del building donde vive el infeccioso.
	 * @param buildingID id de building hogar infeccioso
	 * @param coordinate coordinadas del hogar
	 */
	public void createInfectionSpot(int buildingID, Coordinate coordinate) {
		InfectionSpotAgent infectionSpot = infectionSpots.get(buildingID);
		// Si ya existe marcador, se actualiza el contador
		if (infectionSpot != null) {
			infectionSpot.addInfected();
		}
		// Si no existe, se crea nuevo marcador
		else {
			infectionSpot = new InfectionSpotAgent(buildingID, 1);
			final Geometry geomCircle = createCircleGeometry(coordinate);
			geography.move(infectionSpot, geomCircle);
			mainContext.add(infectionSpot);
			//
			infectionSpots.put(buildingID, infectionSpot);
		}
	}
	
	/**
	 * Eliminar indicador de hogar de humano infectado.
	 * @param buildingID id de building hogar infeccioso
	 */
	public void deleteInfectionSpot(int buildingID) {
		InfectionSpotAgent infSpot = infectionSpots.get(buildingID);
		if (infSpot != null) {
			// Si queda un solo infectado, elimina marcador
			if (infSpot.getInfectedCount() == 1) {
				infectionSpots.remove(buildingID, infSpot);
				geography.move(infSpot, null); // sin location
				mainContext.remove(infSpot);
			}
			// Si quedan mas de un infectado, actualiza contador
			else
				infSpot.removeInfected();
		}
	}
	
	/**
	 * Crea un circulo en las coordenadas del building donde se encuentra el infeccioso.
	 * @param agentID id de humano infeccioso
	 * @param coordinate coordinadas actuales
	 */
	public void createInfectedHuman(int agentID, Coordinate coordinate) {
		InfectedHumanAgent infectedHuman = new InfectedHumanAgent(agentID, coordinate);
		final Geometry geomCircle = createCircleGeometry(coordinate);
		geography.move(infectedHuman, geomCircle);
		mainContext.add(infectedHuman);
		//
		infectedHumans.put(agentID, infectedHuman);
	}
	
	/**
	 * Mueve el punto del infeccioso a las coordenadas del nuevo building.
	 * @param agentID id de humano infeccioso
	 * @param newCoordinate coordinadas actuales
	 */
	public void moveInfectedHuman(int agentID, Coordinate newCoordinate) {
		InfectedHumanAgent infHuman = infectedHumans.get(agentID);
		// Si aun no tiene marcador, se crea desde cero
		if (infHuman == null) {
			createInfectedHuman(agentID, newCoordinate);
			return;
		}
		// Si la posicion del marcador es reciente, se traslada a la nueva
		if (!infHuman.isHidden()) {
			double lonShift = newCoordinate.x - infHuman.getCurrentCoordinate().x;
			double latShift = newCoordinate.y - infHuman.getCurrentCoordinate().y;
			geography.moveByDisplacement(infHuman, lonShift, latShift);
		}
		// Si dejo de trackear al humano, crea nuevo marcador (geometria)
		else {
			infHuman.setHidden(false);
			//
			final Geometry geomCircle = createCircleGeometry(newCoordinate);
			geography.move(infHuman, geomCircle);
			mainContext.add(infHuman);
		}
		infHuman.setCurrentCoordinate(newCoordinate);
	}
	
	/**
	 * Eliminar indicador de humano infectado.
	 * @param agentID id de humano previamente infectado
	 */
	public void deleteInfectedHuman(int agentID) {
		InfectedHumanAgent infHuman = infectedHumans.remove(agentID);
		if (infHuman != null) { // si es viajero, puede ser que nunca se creo el marcador
			geography.move(infHuman, null); // sin location
			mainContext.remove(infHuman);
		}
	}

	/**
	 * Ocultar indicador de humano infectado
	 * @param agentID id de humano infeccioso
	 */
	public void hideInfectedHuman(int agentID) {
		InfectedHumanAgent infHuman = infectedHumans.get(agentID);
		if (infHuman != null) {
			infHuman.setHidden(true);
			mainContext.remove(infHuman);
		}
	}
	
	/**
	 * Matriz de 4x4x4 - Probabilidades sobre 1000. 4 periodos del dia X 4 posiciones actuales X 4 probabilidades de lugares.<p>
	 * <i>MaxiF: La probabilidad de la cadena de markov de movimiento temporal es un arreglo que:
	 * probabilidadTMMC[P,i,j], donde P es el periodo del dia (8-11 11-14 14-17 17-20hs)
	 * i es el nodo de donde sale, y j es el nodo a donde va.<p>
	 * El nodo 0 es la casa, el 1 es el trabajo/estudio, el 2 es ocio, el 3 es otros (supermercados, farmacias, etc)
	 * Ej: probabilidadTMMC[1][1][2] es la probabilidad de que en el periodo 1 salga del trabajo 1 al lugar de ocio 2</i>
	 */
	public static final int DEFAULT_TMMC[][][] = {
			{ {100,700,100,100},{ 25,925, 25, 25},{100,700,100,100},{100,700,100,100} },
			{ {925, 25, 25, 25},{800,  0,100,100},{800,  0,100,100},{800,  0,100,100} },
			{ { 25,925, 25, 25},{ 25,925, 25, 25},{ 25,925, 25, 25},{ 25,925, 25, 25} },
			{ {925, 25, 25, 25},{ 50, 50,450,450},{700,  0,300,  0},{700,  0,  0,300} }
	};
}
