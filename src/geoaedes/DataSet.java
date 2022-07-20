package geoaedes;

/**
 * Atributos estaticos y finales comunes a todos los agentes.
 */
public final class DataSet {
	/** Probabilidad de muerte diaria en Huevos */
	public static final double	EGG_DEATH_RATE		= 0.005; // 0.5% (antes 1%)
	/** Probabilidad de muerte diaria en Adultos */
	public static final double	ADULT_DEATH_RATE	= 0.07; // 7%
	
	/** Habilitar mortalidad en acuaticos */
	public static final boolean	ENABLE_AQUATIC_MORTALITY	= true;

	/** Valor de beta base - chance infeccion / contagio */
	public static final double	INFECTION_RATE	= 60d;
	
	/** Cantidad de huevos por oviposicion */
	public static final int		ADULT_EGGS_PRODUCTION		= 35;	// 70 - mitad hembras
	/** Cantidad media de contenedores usados en oviposicion */
	public static final int		ADULT_EGGS_CTNR_MEAN		= 2;	// minimo 1 - no se usa en inicializacion
	
	/** Vida media del mosquito adulto en dias - 40 a 50 dias segun Silvina */
	public static final int		ADULT_LIFESPAN_MEAN			= 13;	// 13 dias
	/** Desvio std del tiempo de vida del mosq. adulto en dias */
	public static final int		ADULT_LIFESPAN_DEVIATION	= 2;	// 2 dias
	
	/** Cantidad de horas entre desplazamiento y busqueda de container con agua */
	public static final int		ADULT_CONTAINER_SEARCH_INTERVAL	= 4;// 4 horas
	/** Cantidad de intentos de oviposicion (desplazamiento y busqueda) */
	public static final int		ADULT_CONTAINER_SEARCH_TRIES= 2;	// minimo 1
	/** Probabilidad de cambiar humano entre picaduras */
	public static final int		ADULT_SWITCH_TARGET_CHANCE	= 20;	// sobre 100
	
	/** Tiempo de embrionacion en ticks - 2 dias (Otero 2006) - 1 dia Silvina */
	public static final int		ADULT_DIGESTION_PERIOD		= 2*360; // ticks entre alimentacion y ovoposicion
	/** Distancia maxima lineal que puede volar el mosquito (en un solo viaje) */
	public static final double	ADULT_MAX_TRAVEL_DISTANCE	= 0.00045;	// 50 metros segun Silvina (0.0009 son aprox 100m en pna)
	/** Probabilidades por hora de estar activo segun ciclos del ritmo circadiano */
	public static final double[]ADULT_CIRCADIAN_RHYTHMS		= 
		{0.533305, 0.851309, 0.748998, 0.470186, 0.196326, 0.046506, 0.077455, 0.283536, 0.596751, 0.886741, 0.960782, 0.563789}; // % sobre 1
	
	/** Altura media de huevos iniciales en contenedores */
	public static final double	INITIAL_EGG_ELEVATION_MEAN 		= 8d; // cm
	/** Desvio std de altura de huevos iniciales en contenedores */
	public static final double	INITIAL_EGG_ELEVATION_DEVIATION	= 1d; // cm
	
	/** Por debajo de esta temperatura no continua el desarrollo del huevo */
	public static final double	DIAPAUSE_MIN_TEMPERATURE	= 16d; // (antes 16)
	/** Por arriba de esta temperatura no continua el desarrollo del huevo */
	public static final double	DIAPAUSE_MAX_TEMPERATURE	= 30d; // (antes 30)
	/** Cantidad de horas diarias que se exponen los contenedores al sol */
	public static final double	PEAK_SUN_HOURS	= 3;	// 3 horas de sol media
	
	/** Cantidad media de contenedores por casa, por seccional */
	public static final double[]CONTAINERS_PER_HOUSE_MEAN		= 
		{1.6, 1.6, 1.6, 1.6, 1.6, 1.6, 1.6, 1.6, 1.6, 1.6, 1.6, 1.6, 1.6, 1.6, 1.6, 1.6, 1.6, 1.6};
	/** Desvio std de contenedores por casa, por seccional */
	public static final double[]CONTAINERS_PER_HOUSE_DEVIATION	= 
		{0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 0.5};
	/** Porcentaje de contenedores en interiores y de agua constante */
	public static final int		CONTAINER_INDOOR_CHANCE		= 20;	// 20%
	/** Porcentaje inicial de containers al exterior ocupados */
	public static final int		CONTAINER_OCCUPANCY_RATE	= 4;	// 4%
	/** Altura media de contenedores */
	public static final int		CONTAINER_HEIGHT_MEAN		= 15;
	/** Desvio std de altura de contenedores */
	public static final int		CONTAINER_HEIGHT_DEVIATION	= 5;
	/** Area media de contenedores */
	public static final double	CONTAINER_AREA_MEAN 		= 212.5;
	/** Desvio std de area de contenedores */
	public static final double	CONTAINER_AREA_DEVIATION	= 72.5;
	/** Capacidad de acarreo de acuaticos */
	public static final int		CONTAINER_CARRYING_CAPACITY	= 35;	// 70 larvas por litro - mitad hembras
		
	/** Duracion media de periodo de incubacion del virus en ticks */
	public static final int		INCUBATION_PERIOD_MEAN		= 5*360;// 5 dias
	/** Desvio std de periodo de incubacion del virus en ticks */
	public static final int		INCUBATION_PERIOD_DEVIATION	= 1*360;// 1 dia desvio standard
	
	/** Duracion media de periodo de contagio del virus en ticks */
	public static final int		CONTAGIOUS_PERIOD_MEAN		= 6*360;// 6 dias (antes 6.5)
	/** Desvio std de periodo de contagio del virus en ticks */
	public static final int		CONTAGIOUS_PERIOD_DEVIATION	= 1*360;// 1 dia desvio standard
	
	//// Datos Poblacionales y de viviendas ////
	/** Cantidad maxima de humanos por m2 (minimo 1) */
	public static final int HUMANS_PER_SQUARE_METER	= 4;
	
	/** Cantidad de humanos locales (estimado 2021) */
	public static final int LOCAL_HUMANS			= 276975;
	/** Cantidad de humanos locales que trabajan/estudian fuera */
	public static final int LOCAL_TRAVELER_HUMANS	= 0; //(int) (LOCAL_HUMANS * 5) / 100; TODO hace falta implementar?
	/** Cantidad de humanos visitantes */
	public static final int FOREIGN_TRAVELER_HUMANS = 0; // TODO hace falta implementar?

	/** Porcentaje de la poblacion en cada seccional */
	public static final double[] SECTORALS_POPULATION = 
		{6.6, 5.5, 3.8, 6.3, 5.1, 1.9, 1, 11.9, 2.2, 3, 6.1, 9.5, 10.5, 1, 8.3, 6.5, 8.6, 2.2}; // Fuente Abelardo | Padron Electoral
	/** Cantidad de seccionales */
	public static final int SECTORALS_COUNT = SECTORALS_POPULATION.length;

	/** Area en m2 para hogares - por seccional */
	public static final int[] HOME_BUILDING_AREA = 
		{80, 80, 80, 80, 100, 120, 120, 110, 120, 120, 120, 110, 90, 90, 110, 110, 90, 90}; // Fuente Catastro
	/** Area construida en m2 para hogares - por seccional */
	public static final int[] HOME_BUILDING_COVERED_AREA = 
		{100, 100, 100, 100, 120, 140, 140, 130, 140, 140, 140, 130, 140, 140, 130, 130, 130, 130}; // Fuente Catastro

	/** Humanos con hogar dentro y trabajo/estudio fuera - Inventado */
	public static final double LOCAL_HUMANS_PER_AGE_GROUP	= 20;
	/** Humanos con hogar fuera y trabajo/estudio dentro - Inventado */
	public static final double FOREIGN_HUMANS_PER_AGE_GROUP	= 25;

	/** % de estudiantes, trabajadores e inactivos (ama de casa/jubilado/pensionado/otros) */
	public static final int[] OCCUPATION_CHANCE = {31, 39, 30}; // Fuente "El mapa del trabajo argentino 2019" - CEPE | INDEC - EPH 2020

	/** Porcentaje que trabaja en el hogar */
	public static final int WORKING_FROM_HOME	= 6;
	/** Porcentaje que trabaja al exterior */
	public static final int WORKING_OUTDOORS	= 15;
	
	/** % sobre 100 de que al realizar actividades de ocio u otros salga del contexto */
	public static final int TRAVEL_OUTSIDE_CHANCE = 40;
	
	/** Distribucion porcentual de turistas que ingresan con Dengue por cada mes (ene - dic) */
	public static final int[] TRAVELERS_MONTHLY_PCT	= 
		{ 6, 8,12,10,10,12,18,12, 4, 2, 2, 4}; // total 100%
	/** Distribucion porcentual de personas que reingresan con Dengue por cada mes (ene - dic) */
	public static final int[] TOURISTS_MONTHLY_PCT	= 
		{ 4,20,22,32,10, 2, 4, 0, 0, 0, 2, 4}; // total 100%
}
