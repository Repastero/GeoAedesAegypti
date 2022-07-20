package geoaedes.agents;

import geoaedes.DataSet;
import geoaedes.Egg;
import geoaedes.Larva;
import geoaedes.LifeCicle;
import geoaedes.Pupa;
import repast.simphony.random.RandomHelper;

public class AquaticAgent {
	private static int agentIDCounter = 0;
	private static int agentCount = 0;
	private int agentID = ++agentIDCounter;
	
	private LifeCicle lifeCicle;
	private ContainerAgent container;
	
	public AquaticAgent() { };
	
	/**
	 * Crear nuevo Huevo, sin tiempo de desarrollo
	 * @param container  contenedor en el cual se deposita Huevo
	 */
	public AquaticAgent(ContainerAgent container) {
		++agentCount;
		this.lifeCicle = new Egg(container);
		this.container = container;
	}
	
	/**
	 * Crear nuevo Acuatico
	 * @param container  contenedor que alberga el Acuatico
	 * @param lifeStage  tipo de Acuatico: 1 Huevo, 2 Larva, 3 Pupa
	 * @param life  tiempo de desarrollo del Acuatico
	 * @param eggElevation  en caso de crear Huevo, altura del mismo en Container
	 */
	public AquaticAgent(ContainerAgent container, int lifeStage, double life, double eggElevation) {
		++agentCount;
		switch (lifeStage) {
			case 0:
				// Limitar altura del Huevo en contenedor, a altura del contenedor menos 1 (pasa de cm a mm)
				eggElevation = (eggElevation < container.getHeight()) ? eggElevation * 10 : (container.getHeight()-1) * 10;
				this.lifeCicle = new Egg(life, container, eggElevation);
				break;
			case 1:
				this.lifeCicle = new Larva(life, container);
				break;
			default: // 2
				this.lifeCicle = new Pupa(life);
				break;
		}
		this.container = container;
	}
	
	public static void initAgentID() {
		agentIDCounter = 0;
		agentCount = 0;
		// Reinicio la cantidad de los actuaticos
		Egg.initCounter();
		Larva.initCounter();
		Pupa.initCounter();
	}
	
	public ContainerAgent getContainer() {
		return container;
	}
	
	public void setContainer(ContainerAgent container) {
		this.container = container;
	}
	
	public static int getCount() {
		return agentCount;
	}
	
	public static int getEggsCount() {
		return Egg.getCount();
	}
	
	public static int getLarvasCount() {
		return Larva.getCount();
	}
	
	public static int getPupasCount() {
		return Pupa.getCount();
	}
	
	/**
	 * Actualiza el tiempo de desarrollo, la probabilidad de muerte y cambia de estado si corresponde.
	 * @return <code>true</code> si sigue con vida el actuatico
	 */
	public boolean updateLife() {
		double rnd = RandomHelper.nextDoubleFromTo(0d, 1d);
		if (DataSet.ENABLE_AQUATIC_MORTALITY && rnd < lifeCicle.getDeathRate()) {
			eliminate();
			return false;
		}
		else {
			// Acuatico sobrevive
			if (lifeCicle.getLife() >= 0.95) {
				nextLifeCicle();
				if (lifeCicle == null) // Si emergio el mosquito adulto - muere el Acuatico
					return false;
			}
			lifeCicle.updateLife();
		}
		return true;
	}

	public int getLifeCicle() {
		if (lifeCicle instanceof Egg)
			return 0;
		else if (lifeCicle instanceof Larva)
			return 1;
		else
			return 2;
	}
	
	/**
	 * Cambia de estado Acuatico: Huevo(0) => Larva(1) => Pupa(2)
	 */
	private void nextLifeCicle() {
		if (lifeCicle instanceof Egg)
			hatch();
		else if (lifeCicle instanceof Larva)
			molt();
		else // (lifeCicle instanceof Pupa)
			emerge();
	}
	
	/**
	 * Mata Acuatico en Container.
	 */
	public void eliminate() {
		// Acuatico muere
		if (getLifeCicle() == 0) { // Huevo
			container.decreaseEggsAmount();
		}
		else // Larva o Pupa
			container.decreaseAquaticAmount();
		lifeCicle.eliminate();
		--agentCount;
	}

	/**
	 * Huevo a Larva.
	 */
	private void hatch() {
		// Descuenta un Huevo y aumenta un Acuatico
		container.decreaseEggsAmount();
		container.increaseAquaticAmount();
		//
		lifeCicle.eliminate();
		lifeCicle = new Larva(container);
	}

	/**
	 * Larva a Pupa.
	 */
	private void molt() {
		// Se considera como Acuatico - no hace falta incrementar cantidad
		lifeCicle.eliminate();
		lifeCicle = new Pupa();
	}

	/**
	 * Pupa a Adulto.
	 */
	private void emerge() {
		// Descuenta un Acuatico y crea un Mosquito adulto
		container.decreaseAquaticAmount();
		container.emergeMosquito();
		//
		lifeCicle.eliminate();
		lifeCicle = null;
		--agentCount;
	}
}
