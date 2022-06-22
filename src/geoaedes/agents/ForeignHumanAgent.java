package geoaedes.agents;

/**
 * Agente Humano extranjero que puede entrar y salir del contexto.
 */
public class ForeignHumanAgent extends HumanAgent {
	
	//-private boolean inContext = true;
	
	public ForeignHumanAgent(int secHomeIndex, BuildingAgent job) {
		super(secHomeIndex, null, job, true, false);
		// TODO hace falta implementar?
	}
	/*
	public void addToContext() {
		context.add(this);
		inContext = true;
	}
	
	@Override
	public void removeFromContext() {
		if (inContext) {
			super.removeFromContext();
			inContext = false;
		}
	}
	
	@Override
	public void addRecoveredToContext() {
		// Si se recupera, vuelve al dia siguiente
		double newDayTick = Math.ceil(schedule.getTickCount() / 360) * 360;
		
        // Schedule one shot
		ScheduleParameters params = ScheduleParameters.createOneTime(newDayTick, ScheduleParameters.FIRST_PRIORITY);
		schedule.schedule(params, this, "addToContext");
	}
	
	@Override
	public void setInfectious(boolean asyntomatic, boolean initial) {
		if (asyntomatic) // para que no sume como nuevo expuesto
			exposed = true;
		super.setInfectious(asyntomatic, initial);
	}
	*/
}
