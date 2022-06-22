package geoaedes.agents;

import com.vividsolutions.jts.geom.Coordinate;

import geoaedes.DataSet;

/**
 * Clase hogar. Contiene la lista de habitantes y setea cuarentena preventiva.
 */
public class HomeAgent extends BuildingAgent {
	
	public HomeAgent(int sectoralIdx, Coordinate coord, int id, int blockId, char condition) {
		super(sectoralIdx, coord, id, blockId, condition, "home", DataSet.HOME_BUILDING_AREA[sectoralIdx], DataSet.HOME_BUILDING_COVERED_AREA[sectoralIdx], false);
	}
}
