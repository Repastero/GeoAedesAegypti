package geoaedes;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * Propiedades de places y metodos para leer archivo de salida de markov.
 */
public final class PlaceProperty {
	/** Tipo secundario */
	private String googleMapsType;
	/** Tipo primario */
	private String googlePlaceType;
	/** Indice tipo actividad (0,1,2,3) */
	private int activityState;
	/** Area total */
	private int buildingArea;
	/** Porcentaje area cubierta */
	private int buildingCoveredArea;
	/** Trabajadores por lugar */
	private int workersPerPlace;
	/** Trabajadores por area */
	private int workersPerArea;
	
	public PlaceProperty(String gmapsType, String gplaceType) {
		this.googleMapsType = gmapsType;
		this.googlePlaceType = gplaceType;
	}
	
	public PlaceProperty(String gmapsType, String gplaceType, int type, int avgArea, int avgCoveredArea, int workersPlace, int workersArea) {
		this.googleMapsType = gmapsType;
		this.googlePlaceType = gplaceType;
		this.activityState = type;
		this.buildingArea = avgArea;
		this.buildingCoveredArea = avgCoveredArea;
		this.workersPerPlace = workersPlace;
		this.workersPerArea = workersArea;
	}
	
	public PlaceProperty(String gmapsType, String gplaceType, PlaceProperty pp) {
		this(gmapsType, gplaceType, pp.activityState, pp.buildingArea, pp.buildingCoveredArea, pp.workersPerPlace, pp.workersPerArea);
	}
	
	/**
	 * Lee el archivo de salida de markov y carga las propiedades de cada tipo de places.
	 * @param filePath ruta archivo
	 * @return mapa con propiedades por cada tipo de place
	 */
	public static Map<String, PlaceProperty> loadPlacesProperties(String filePath) {
		final String[] headers = {"Google_Maps_type","Google_Place_type","Activity_type","Area","Covered_area","Workers_place","Workers_area"};
		PlaceProperty placeProperty;
		String gMapsType;
		String gPlaceType;
		Map<String, PlaceProperty> placesProperty = new HashMap<>();
		boolean headerFound = false;
		String[] nextLine;
		int[] dataIndexes = {};
		// Leo todas las lineas 
		List<String[]> fileLines = Utils.readCSVFile(filePath, ',', 0);
		if (fileLines == null) {
			System.err.println("Error al leer archivo de Places: " + filePath);
			return placesProperty;
		}
		for (Iterator<String[]> it = fileLines.iterator(); it.hasNext();) {
			nextLine = it.next();
			try {
				// Como tiene varias columnas es preferible leer el header
				if (!headerFound) {
					dataIndexes = Utils.readHeader(headers, nextLine);
					headerFound = true;
					continue;
				}
				gMapsType = nextLine[dataIndexes[0]]; // Type de Google Maps
				gPlaceType = nextLine[dataIndexes[1]]; // Type de Google Places o custom
				// Si es Type principal, debe tener todos los atributos
				if (gMapsType.equals(gPlaceType)) {
					placeProperty = new PlaceProperty(gMapsType, gPlaceType);
					try {
						placeProperty.setActivityState	(Integer.valueOf(nextLine[dataIndexes[2]]));
						placeProperty.setBuildingArea	(Integer.valueOf(nextLine[dataIndexes[3]]));
						placeProperty.setBuildingCArea	(Integer.valueOf(nextLine[dataIndexes[4]]));
						placeProperty.setWorkersPerPlace(Integer.valueOf(nextLine[dataIndexes[5]]));
						placeProperty.setWorkersPerArea	(Integer.valueOf(nextLine[dataIndexes[6]]));
					} catch (NumberFormatException e) {
						System.out.println(String.join(", ", nextLine));
					}
				}
				// Si es type secundario, pueden variar el area y trabajadores
				else {
					// Crear copia del type primario
					placeProperty = new PlaceProperty(gMapsType, gPlaceType, placesProperty.get(gPlaceType));
					try {
						if (!StringUtils.isBlank(nextLine[dataIndexes[3]]))
							placeProperty.setBuildingArea	(Integer.valueOf(nextLine[dataIndexes[3]]));
						if (!StringUtils.isBlank(nextLine[dataIndexes[4]]))
							placeProperty.setBuildingCArea	(Integer.valueOf(nextLine[dataIndexes[4]]));
						if (!StringUtils.isBlank(nextLine[dataIndexes[5]]))
							placeProperty.setWorkersPerPlace(Integer.valueOf(nextLine[dataIndexes[5]]));
						if (!StringUtils.isBlank(nextLine[dataIndexes[6]]))
							placeProperty.setWorkersPerArea	(Integer.valueOf(nextLine[dataIndexes[6]]));
					} catch (NumberFormatException e) {
						System.out.println(String.join(", ", nextLine));
					}
				}
				placesProperty.put(gMapsType, placeProperty);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return placesProperty;
	}

	public String getGoogleMapsType() {
		return googleMapsType;
	}

	public String getGooglePlaceType() {
		return googlePlaceType;
	}

	public int getActivityState() {
		return activityState;
	}

	public int getBuildingArea() {
		return buildingArea;
	}

	public int getBuildingCArea() {
		return buildingCoveredArea;
	}

	public int getWorkersPerPlace() {
		return workersPerPlace;
	}

	public int getWorkersPerArea() {
		return workersPerArea;
	}

	public void setGoogleMapsType(String googleMapsType) {
		this.googleMapsType = googleMapsType;
	}

	public void setGooglePlaceType(String googlePlaceType) {
		this.googlePlaceType = googlePlaceType;
	}

	public void setActivityState(int activityState) {
		this.activityState = activityState;
	}

	public void setBuildingArea(int buildingArea) {
		this.buildingArea = buildingArea;
	}

	public void setBuildingCArea(int buildingCoveredArea) {
		this.buildingCoveredArea = buildingCoveredArea;
	}

	public void setWorkersPerPlace(int workersPlace) {
		this.workersPerPlace = workersPlace;
	}

	public void setWorkersPerArea(int workersArea) {
		workersPerArea = workersArea;
	}
}
