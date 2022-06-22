package geoaedes;

import java.util.Iterator;
import java.util.List;

import repast.simphony.engine.schedule.ScheduleParameters;
import repast.simphony.engine.schedule.ScheduledMethod;

/**
 * La clase {@code Weather} contiene metodos para obtener los valores relacionados con el clima del dia
 * (temperatura, humedad, precipitacion, viento y evaporacion).<p>
 * Tambien calcula diariamente los valores de Desarrollo Metabolico y Probabilidad de Mortalidad de Acuaticos;
 * e implementa metodos para acceder a los mismos.
 */
public class Weather {
	/** Contador anual */
	private static int currentYear;
	/** Contador diario */
	private static int dayOfTheYear;
	/** Cantidad dias anuales */
	private static int daysInYear;
	
	/** Desarrollo diario de Huevos */
	private static double ddEgg;
	/** Desarrollo diario de Larvas */
	private static double ddLarva;
	/** Desarrollo diario de Pupas */
	private static double ddPupa;
	/** Desarrollo diario de Adultos */
	private static double ddAdult;
	
	/** Probabilidad de muerte diaria en Larvas y Pupas */
	private static double ddrAquatic;
	
	/** Temperatura del dia actual */
	private static double dailyTemperature;
	/** Precipitacion del dia actual */
	private static double dailyPrecipitation;
	/** Humedad del dia actual */
	private static double dailyHumidity;
	/** Velocidad del viento del dia actual */
	private static double dailyWindSpeed;
	/** Evaporacion en ml del dia actual */
	private static double dailyEvaporationRate;
	
	/** Temperaturas diarias anuales (exterior) */
	private static final double[] TEMPERATURES	= new double[366];
	/** Precipitaciones diarias anuales */
	private static final double[] PRECIPITATIONS= new double[366];
	/** Humedad relativa diarias anuales */
	private static final double[] HUMIDITIES	= new double[366];
	/** Velocidad media en km/h diarias anuales */
	private static final double[] WIND_SPEEDS	= new double[366];
	
	/** @return {@link Weather#dailyTemperature} */
	public 	static double getTemperature()		{ return dailyTemperature; }
	/** @return {@link Weather#dailyTemperature} en Kelvin */
	public 	static double getTemperatureK()		{ return dailyTemperature + 273; }
	/** @return {@link Weather#dailyPrecipitation} */
	public  static double getPrecipitation()	{ return dailyPrecipitation; }
	/** @return {@link Weather#dailyHumidity} */
	public  static double getHumidity()			{ return dailyHumidity; }
	/** @return {@link Weather#dailyWindSpeed} */
	public  static double getWindSpeed()		{ return dailyWindSpeed; }
	/** @return {@link Weather#dailyEvaporationRate} */
	public  static double getEvaporationRate()	{ return dailyEvaporationRate; }
	
	/** @return {@link Weather#ddEgg} */
	public  static double getDDEgg()	{ return ddEgg; }
	/** @return {@link Weather#ddLarva} */
	public  static double getDDLarva()	{ return ddLarva; }
	/** @return {@link Weather#ddPupa} */
	public  static double getDDPupa()	{ return ddPupa; }
	/** @return {@link Weather#ddAdult} */
	public  static double getDDAdult()	{ return ddAdult; }
	
	/** @return {@link DataSet#EGG_DEATH_RATE} */
	public  static double getDDREgg()	{ return DataSet.EGG_DEATH_RATE; }
	/** @return {@link Weather#ddrAquatic} */
	public  static double getDDRLarva()	{ return ddrAquatic; }
	/** @return {@link Weather#ddrAquatic} */
	public  static double getDDRPupa()	{ return ddrAquatic; }
	/** @return {@link DataSet#ADULT_DEATH_RATE} */
	public  static double getDDRAdult()	{ return DataSet.ADULT_DEATH_RATE; }
	
	/** @return {@link Weather#dayOfTheYear} */
	public	static int getCurrentDay()	{ return dayOfTheYear; }
	
	/**
	 * @param startYear ano de inicio (2000 o +)
	 */
	public Weather(int startYear) {
		currentYear = startYear;
		// La simulacion comienza el 1ero de Julio
		dayOfTheYear = isLeapYear(startYear) ? 183 : 182;
		initWeather();
	}
	
	/**
	 * Lee de archivo csv los datos de temperatura media del ano actual.
	 */
	public static void initWeather() {
		loadWeatherData(currentYear); // Leer temp del ano inicio
		// Suma anos si la cantidad de dias sobrepasa a la del ano
		while (dayOfTheYear >= daysInYear) {
			dayOfTheYear -= daysInYear;
			// Lee archivo de proximo ano
			loadWeatherData(++currentYear);
		}
		// Setear las chances de infeccion del dia inicial
		updateWeatherValues();
	}
	
	@ScheduledMethod(start = 360, interval = 360, priority = ScheduleParameters.FIRST_PRIORITY)
	public static void updateDailyWeather() {
		// Ultimo dia del ano, lee los datos del ano siguiente
		if (++dayOfTheYear == daysInYear) {
			dayOfTheYear = 0;
			loadWeatherData(++currentYear);
		}
		// Setear las chances de infeccion del dia actual
		updateWeatherValues();
	}
	
	/**
	 * Setea los valore diarios del clima y actualiza el valor de agua evaporada (Containers), 
	 * desarrollo metabolico (Acuaticos) y tasa de mortalidad (Acuaticos).
	 */
	public static void updateWeatherValues() {
		dailyTemperature = TEMPERATURES[dayOfTheYear];
		dailyPrecipitation = PRECIPITATIONS[dayOfTheYear];
		dailyHumidity = HUMIDITIES[dayOfTheYear];
		dailyWindSpeed = WIND_SPEEDS[dayOfTheYear];
		
		updateDailyEvaporationRate();
		updateDailyAquaticDevelopment();
		updateDailyAdultDevelopment();
		updateDailyAquaticDeathRate();
	}
	
	/**
	 * Calcula evaporacion diaria de agua en contenedores en ml.
	 */
	public static void updateDailyEvaporationRate() {
		final double T = getTemperatureK();
		final double P  = Math.pow(10, 8.07131d - 1730.63d / (233.426d + getTemperature())); // presion del vapor. log(P) = 8.07131 - ((1730.63)/(233.426 + T))
		final double U  = Math.pow(getWindSpeed() / 1.61d, 0.78d); // U  Velocidad del viento [kmph/1.61],
		final double H  = DataSet.PEAK_SUN_HOURS; // H  Exposicion Solar
		final double MW = 6.87217184640305d; // = Math.pow(18.01528d, 2d/3d) // MW Peso molecular del agua [g/mol]
		final double A  = 201.06193d; // = pi*8^2 // Superficie expuesta del contenedor en centimetros cuadrados
		final double K  = 260d; // constante
		// Ema: A Model to Predict Evaporation Rates in Habitats Used by Container-Dwelling Mosquitoes, Bartlett-Healy (2011)
		//-final double evaporationMl = (U*MW*A*H*P*H)/(k*T); // evaporation rate [ml/dia] <- formula original
		// TODO ver: modifique la formula original para que tenga en cuenta tambien la humedad, se podria mejorar!
		final double evaporationMl = (U*MW*A*H*P*H)/(K*T)/(getHumidity()/50); // evaporation rate [ml/dia]
		dailyEvaporationRate = evaporationMl;
	}
	
	/**
	 * Calcula desarrollo diario por cada fase de acuaticos.
	 */
	private static void updateDailyAquaticDevelopment() {
		final double R = 0.98588d;
		double Rdk, Ha, Hh, T12;
		// Huevo
		Rdk = 0.24d;
		Ha  = 10798d;
		Hh  = 100000d;
		T12 = 14184d;
		if (getTemperature() < DataSet.DIAPAUSE_MIN_TEMPERATURE ||
			getTemperature() > DataSet.DIAPAUSE_MAX_TEMPERATURE)
			ddEgg = 0d; // diapausa
		else
			ddEgg = getDailyDevelopment(R, Rdk, Ha, Hh, T12);
		// Larva
		Rdk = 0.2088d;
		Ha  = 26018d;
		Hh  = 55990d;
		T12 = 304.6d;
		ddLarva = getDailyDevelopment(R, Rdk, Ha, Hh, T12);
		// Pupa
		Rdk = 0.384d;
		Ha  = 14931d;
		Hh  = -472379d;
		T12 = 148d;
		ddPupa = getDailyDevelopment(R, Rdk, Ha, Hh, T12);
	}
	
	/**
	 * Calcula desarrollo diario en acuaticos.
	 * @return proporcion de desarrollo
	 */
	private static double getDailyDevelopment(double R, double Rdk, double Ha, double Hh, double T12) {
		//Ema: [3]Schoofield, R.M., Sharpe, P.J.H., Magnuson, C.E., 1981. Non-linear regression of biological temperature-dependent rate models based on absolute reaction-rate theory.
		final double T = getTemperatureK();
		final double desMetabolico = Rdk * (T/298) * Math.exp(Ha/R * (1d/298 - 1/T)) / (1 + Math.exp(Hh/R * (1/T12  - 1/T)));
		return desMetabolico;
	}
	
	/**
	 * Calcula desarrollo diario en adultos en base a temperatura y humedad.
	 */
	private static void updateDailyAdultDevelopment() {
		final double temp = Weather.getTemperature();
		final double hum  = Weather.getHumidity();
		// MaxiF: Genero una normal con medias y std segun el trabajo de Ivan P. basado en un paper.
		// MaxiF: Esta normal se usa para obtener la vida del mosquito.
		// Ema: Ivan P se basa en Lewis (1933) OBSERVATIONS ON AEDES AEGYPTI, L. (DIPT. CULIC.) UNDER CONTROLLED ATMOSPHERIC CONDITIONS.
		ddAdult = DataSet.ADULT_LIFESPAN_MEAN / (33.29 - 2.0307*temp - 0.03654*hum + 0.04054*temp*temp + 0.001703*temp*hum + 0.0004375*hum*hum); 
	}
	
	/**
	 * Calcula mortalidad diaria de acuaticos en base a temperatura.
	 */
	private static void updateDailyAquaticDeathRate() {
		// Ema: Marcelo Otero, Hernan G Solari, and Nicolas Schweigmann. A stochastic population dynamics model for
		// Aedes aegypti: formulation and application to a city with temperate climate. Bulletin of mathematical biology,(2006)
		ddrAquatic = 0.01 + 0.9725 * Math.exp(-(getTemperature()-5) / 2.7035);
	}
	
	/**
	 * Lee el archivo de temperaturas medias, humedad, precipitacion y viento. Guarda los valores en arrays. 
	 * @param file ruta de archivo csv
	 * @param index posicion inicial de arrays
	 * @param dayFrom desde que dia leer
	 * @param dayTo hasta que dia leer
	 */
	private static void readWeatherFile(String file, int index, int dayFrom, int dayTo) {
		List<String[]> fileLines = Utils.readCSVFile(file, ';', 1 + dayFrom); // Ignoro header
		if (fileLines == null) {
			System.err.println("Error al leer archivo de clima: " + file);
			return;
		}
		int i = dayFrom;
		String[] line; 
		for (Iterator<String[]> it = fileLines.iterator(); it.hasNext();) {
			line = it.next();
			try {
				TEMPERATURES[index] = Double.valueOf(line[0]);
				HUMIDITIES[index] = Double.valueOf(line[1]);
				PRECIPITATIONS[index] = Double.valueOf(line[2]);
				WIND_SPEEDS[index] = Double.valueOf(line[3]);
				++index;
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
			if (++i > dayTo)
				break;
		}
	}
	
	private static boolean isLeapYear(int year) {
		return ((year % 4 == 0) && (year % 100 != 0)) || (year % 400 == 0);
	}
	
	private static void loadWeatherData(int year) {
		// Setear dias en ano, por si toca bisiesto
		daysInYear = isLeapYear(year) ? 366 : 355;
		// Leer los valores de todo el ano
		String weatherFile = String.format("./data/%d-weather.csv", year);
		readWeatherFile(weatherFile, 0, 0, daysInYear);
	}
}
