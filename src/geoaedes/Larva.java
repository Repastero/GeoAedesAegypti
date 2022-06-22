package geoaedes;

import geoaedes.agents.ContainerAgent;

public class Larva extends LifeCicle {
	private static int agentCount = 0;

	private ContainerAgent container; // para saber cantidad y capacidad de acuaticos
	
	public Larva(double life, ContainerAgent container) {
		super(life);
		++agentCount;
		this.container = container;
	}
	
	public Larva(ContainerAgent container) {
		this(0d, container);
	}
	
	public static void initCounter() {
		agentCount = 0;
	}
	
	public static int getCount() {
		return agentCount;
	}

	@Override
	public void updateLife() {
		double desMeta = Weather.getDDLarva();
		super.updateLife(desMeta);
	}

	@Override
	public double getDeathRate() {
		if (!mortalidadCapacidadAcarreo())
			return Weather.getDDRLarva();
		else // muere si no hay capacidad en contenedor
			return 1d;
	}

	/**
	 * Chequea si hay capacidad en el contenedor para que la Larva se alimente y desarrolle.
	 * @return <code>true</code> si se debe aplicar la mortalidad por limite de capacidad
	 */
	private boolean mortalidadCapacidadAcarreo() {
		final int aquaticAmountInContainer = container.getAquaticAmount(); // obtengo cuantos Aedes acuaticos tiene ese contenedor
		final int carryingCapacity = container.getCarryingCapacity(); // obtengo la capacidad de acarreo que tiene ese contenedor
		return (aquaticAmountInContainer > carryingCapacity); // si hay menos de los permitido,
	}

	@Override
	public void eliminate() {
		--agentCount;
	}
}
