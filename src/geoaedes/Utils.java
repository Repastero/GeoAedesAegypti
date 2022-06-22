package geoaedes;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;

import au.com.bytecode.opencsv.CSVReader;
import repast.simphony.random.RandomHelper;

/**
 * Metodos de utilidad.
 */
public final class Utils {
	
	/**
	 * Genera un valor aleatorio de la distribucion normal y lo limita al desvio.
	 * @param mean media
	 * @param std desvio estandar
	 * @return <b>int</b> valor aleatorio
	 */
	public static int getStdNormalDeviate(int mean, int std) {
		int rnd = RandomHelper.createNormal(mean, std).nextInt();
		rnd = (rnd > mean+std) ? mean+std : (rnd < mean-std ? mean-std: rnd);
		return rnd;
	}
	
	/**
	 * Genera un valor aleatorio de la distribucion normal y lo limita al desvio.
	 * @param mean media
	 * @param std desvio estandar
	 * @return <b>double</b> valor aleatorio
	 */
	public static double getStdNormalDeviate(double mean, double std) {
		double rnd = RandomHelper.createNormal(mean, std).nextDouble();
		rnd = (rnd > mean+std) ? mean+std : (rnd < mean-std ? mean-std: rnd);
		return rnd;
	}
	
	/**
	 * Limita el valor dado al desvio estandard.
	 * @param value valor aleatorio
	 * @param mean media
	 * @param std esvio estandar
	 * @return <b>double</b> valor aleatorio limitado
	 */
	public static double limitStandardDeviation(double value, double mean, double std) {
		return (value > mean+std) ? mean+std : (value < mean-std ? mean-std: value);
	}
	
	/**
	 * Lee archivo CSV segun parametros dados.
	 * @param file ruta archivo
	 * @param charSeparator separador de valores
	 * @param skipLines saltearse primeras lineas 
	 * @return <b>LinkedList</b> con lineas y valores por columna
	 */
	public static List<String[]> readCSVFile(String file, char charSeparator, int skipLines) {
		List<String[]> lines = null;
		try {
			FileReader fileReader = new FileReader(file);
			CSVReader csvReader = new CSVReader(fileReader, charSeparator, '\'', skipLines);
			lines = csvReader.readAll();
			fileReader.close();
			csvReader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lines;
	}
	
	/**
	 * Busca la posicion del valor dado en array.
	 * @param value String buscado
	 * @param rows array de Strings
	 * @return <b>int</b> indice o -1 si no se encuentra
	 */
	public static int findIndex(String value, String[] rows) {
		for (int idx = 0; idx < rows.length; idx++) {
			if (rows[idx].equals(value))
				return idx;
		}
		return -1;
	}
	
	/**
	 * Busca el indice de columna de cada header.
	 * @param headers array de Strings con los headers
	 * @param rows array de Strings con linea header
	 * @return <b>int[]</b> indice de headers
	 * @throws Exception si faltan headers
	 */
	public static int[] readHeader(String[] headers, String[] rows) throws Exception {
		int[] indexes = new int[headers.length];
		int i,j;
		for (i = 0; i < headers.length; i++) {
			j = findIndex(headers[i], rows);
			if (j == -1)
				throw new Exception("Falta Header: " + headers[i]);
			indexes[i] = j;
		}
		return indexes;
	}
	
	/**
	 * Lee el archivo GIS y retorna sus items en una lista.
	 * @param filename  ruta del archivo shape
	 * @return lista de <b>SimpleFeatures</b>
	 */
	public static List<SimpleFeature> loadFeaturesFromShapefile(String filename) {
		URL url = null;
		try {
			url = new File(filename).toURI().toURL();
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
		//
		List<SimpleFeature> features = new ArrayList<SimpleFeature>();
		ShapefileDataStore store = new ShapefileDataStore(url);
		SimpleFeatureIterator fiter = null;
		try {
			fiter = store.getFeatureSource().getFeatures().features();
			while (fiter.hasNext()) {
				features.add(fiter.next());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			// liberar recursos
			if (fiter != null)
				fiter.close();
			store.dispose();
		}
		//
		return features;
	}
}
