package geoaedes;

import geoaedes.agents.ContainerAgent;

public class Egg extends LifeCicle {
	private static int agentCount = 0;

	private ContainerAgent container; // para saber nivel del agua
	/** Altura del huevo en contenedor */
	private double elevation;

	public Egg(double life, ContainerAgent container, double elevation) {
		super(life);
		++agentCount;
		this.container = container;
		this.elevation = elevation;
	}
	
	public Egg(ContainerAgent container) {
		this(0d, container, container.getMmWater());
	}
	
	public static void initCounter() {
		agentCount = 0;
	}
	
	public static int getCount() {
		return agentCount;
	}

	@Override
	public void updateLife() {
		double desMeta = Weather.getDDEgg();
		super.updateLife(desMeta);
		if (super.getLife() >= 0.95d)
			enableHatching();
	}

	/**
	 * No permite eclosionar al Huevo hasta que el nivel de agua no supere el nivel inicial.<p>
	 * Si el contenedor es intradomiciliario, el Huevo eclosiona al cumplir su desarrollo.
	 */
	private boolean enableHatching() {
		if (!container.isIndoors() && (container.getMmWater() <= elevation)) {
			super.setLife(0.94d);
			return false;
		}
		return true;
	 }

	@Override
	public double getDeathRate() {
		return Weather.getDDREgg();
	}

	@Override
	public void eliminate() {
		--agentCount;
	}
}