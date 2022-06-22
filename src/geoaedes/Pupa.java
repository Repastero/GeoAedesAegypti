package geoaedes;

public class Pupa extends LifeCicle {
	private static int agentCount = 0;
	
	public Pupa(double life) {
		super(life);
		++agentCount;
	}
	
	public Pupa() {
		this(0d);
	}
	
	public static void initCounter() {
		agentCount = 0;
	}
	
	public static int getCount() {
		return agentCount;
	}

	@Override
	public void updateLife() {
		double desMeta = Weather.getDDPupa();
		super.updateLife(desMeta);
	}
	
	@Override
	public double getDeathRate() {
		if (getLife() < 0.95d)
			return Weather.getDDRPupa();
		else // si va emerger seteo la mortalidad por emergencia - [Otero et al 2006]
			return 0.17d;
	}
	
	@Override
	public void eliminate() {
		--agentCount;
	}
}
