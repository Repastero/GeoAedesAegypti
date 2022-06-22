package geoaedes;

public abstract class LifeCicle {
	/** Porcentaje de desarrollo de la fase actual. */
	private double life = 0d;
	
	/**
	 * Incrementa porcentaje de desarrollo, de acuerdo a fase actual.
	 */
	public abstract void updateLife();
	
	/**
	 * Mortalidad diaria, de acuerdo a fase actual.
	 * @return porcentaje de mortalidad
	 */
	public abstract double getDeathRate();
	
	/**
	 * Resta el Agente de la cantidad total.
	 * @param agentID ID del Agente
	 */
	public abstract void eliminate();
	
	public LifeCicle() { }
	
	public LifeCicle(double value) {
		this.life = value;
	};
	
	/**
	 * @return Porcentaje de desarrollo de la fase actual.
	 */
	public double getLife() {
		return life;
	}
	
	/**
	 * Cambia el porcentaje de desarrollo de la fase actual.
	 * @param value nuevo porcentaje de desarrollo
	 */
	public void setLife(double value) {
		life = value;
	}
	
	/**
	 * Actualiza el porcentaje de desarrollo de la fase actual.
	 * @param value porcentaje de desarrollo a sumar
	 */
	public void updateLife(double value) {
		life += value;
	}
}
