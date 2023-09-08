package cloudsim.ext.datacenter;
import java.util.*;

import cloudsim.ext.Constants;
import cloudsim.ext.event.CloudSimEvent;
import cloudsim.ext.event.CloudSimEventListener;
import cloudsim.ext.event.CloudSimEvents;

public class honeyBee extends VmLoadBalancer implements CloudSimEventListener {
	private int cutoff = 1;
	private int scoutBee = -1;
	private Map<Integer, VirtualMachineState> vmStatesList;
	Map<Integer, Integer> vmAllocationCounts = new HashMap<Integer, Integer> ();
	Map<Integer, Integer> fitness = new HashMap<Integer, Integer> ();
	
	
	public honeyBee(DatacenterController dcb){
		this.vmStatesList = dcb.getVmStatesList();
		dcb.addCloudSimEventListener(this);
	}


	@Override
	public int getNextAvailableVm(){
		int vmId = -1;
		vmId = getScoutBee();
		scoutBee = vmId;
		allocatedVm(vmId);
		System.out.println("allocated "+vmId);
		return vmId;
	}
	
	public void cloudSimEventFired(CloudSimEvent e) {
		if (e.getId() == CloudSimEvents.EVENT_CLOUDLET_ALLOCATED_TO_VM){
			int vmId = (Integer) e.getParameter(Constants.PARAM_VM_ID);
			int countCloudlets;
			if(vmAllocationCounts.get(vmId)==null)
				countCloudlets = 0;
			else
				countCloudlets = vmAllocationCounts.get(vmId);
			vmAllocationCounts.put(vmId,countCloudlets+1);
			if(vmAllocationCounts.get(vmId)>cutoff)
				vmStatesList.put(vmId, VirtualMachineState.BUSY);
		} else if (e.getId() == CloudSimEvents.EVENT_VM_FINISHED_CLOUDLET){
			int vmId = (Integer) e.getParameter(Constants.PARAM_VM_ID);
			int countCloudlets = vmAllocationCounts.get(vmId);
			vmAllocationCounts.put(vmId,countCloudlets-1);
			if(vmAllocationCounts.get(vmId)<cutoff)
				vmStatesList.put(vmId, VirtualMachineState.AVAILABLE);
		}
	}
		
	private boolean isSendScoutBees(int scoutBee)
	{
		if((vmAllocationCounts.get(scoutBee)==null)||(vmAllocationCounts.get(scoutBee) < cutoff))
			return false;
		else
			return true;
	}

	private int pso() {
		final int SWARM_SIZE = 50;
		final int MAX_ITERATION = 100;

		// Initialize swarm with random positions and velocities
		List<Particle> swarm = new ArrayList<Particle>();
		for (int i = 0; i < SWARM_SIZE; i++) {
			int pos = new Random().nextInt(vmStatesList.size());
			int vel = new Random().nextInt(vmStatesList.size()) - vmStatesList.size() / 2;
			swarm.add(new Particle(pos, vel));
		}

		// Initialize global best particle
		Particle gBest = swarm.get(0);

		// PSO main loop
		for (int i = 0; i < MAX_ITERATION; i++) {
			// Evaluate fitness of each particle
			for (Particle p : swarm) {
				int fitness = calculateFitness(vmAllocationCounts.get(p.pos));
				if (fitness > p.bestFitness) {
					p.bestPos = p.pos;
					p.bestFitness = fitness;
				}
				if (fitness > gBest.bestFitness) {
					gBest = new Particle(p.pos, fitness, p.pos, fitness);
				}
			}

			// Update velocities and positions of each particle
			for (Particle p : swarm) {
				double r1 = Math.random();
				double r2 = Math.random();
				int newVel = (int) (p.vel + r1 * (p.bestPos - p.pos) + r2 * (gBest.bestPos - p.pos));
				p.pos += newVel;
				p.vel = newVel;
				if (p.pos < 0) {
					p.pos = 0;
					p.vel = 0;
				} else if (p.pos >= vmStatesList.size()) {
					p.pos = vmStatesList.size() - 1;
					p.vel = 0;
				}
			}
		}

		// Return best particle's position
		return gBest.bestPos;
	}

	/**
	 * Particle class for PSO algorithm.
	 */
	private class Particle {
		int pos; // Current position
		int vel; // Current velocity
		int bestPos; // Best position found by this particle
		int bestFitness; // Fitness value of best position found by this particle

		public Particle(int pos, int vel) {
			this(pos, calculateFitness(vmAllocationCounts.get(pos)), pos, calculateFitness(vmAllocationCounts.get(pos)));
			this.vel = vel;
		}

		public Particle(int pos, int fitness, int bestPos, int bestFitness) {
			this.pos = pos;
			this.bestPos = bestPos;
			this.bestFitness = bestFitness;
		}
	}

	/* This will return food source */
	int getScoutBee() {
		if (scoutBee == -1) {
			if (vmStatesList.size() > 0) {
				return pso();
			} else {
				return -1;
			}
		} else {
			if (isSendScoutBees(scoutBee) == false) {
				return scoutBee;
			} else {
				SendEmployedBees();
				return SendOnlookerBees();
			}
		}
	}

	
	int MemorizeBestSource() 
	{
		return waggleDance();
	}
	
	/* These are the bees which will observe Waggle Dance and give us best source */
	int SendOnlookerBees()
	{
		return MemorizeBestSource();
	}
	
	// Calculation to get the fitness value of VM
	void calculation()
	{
		  int i;
		  /*Employed Bee Phase*/
		  for (i=0;i<vmStatesList.size();i++)
		  {
			   if(vmAllocationCounts.get(i)==null)
				   fitness.put(i, 0);
			   else
				   fitness.put(i, calculateFitness(vmAllocationCounts.get(i)));
		  }
	}
	
	int calculateFitness(int solValue)
	{
		solValue = 1/(1000-solValue);
		return solValue;	//// tasklength/VM capacity
	}
	
	// Bees went in search & finding all the fitness
	void SendEmployedBees()
	{
	  fitness.clear();
	  calculation();
	}

	
	// By waggle Dance, we are getting best VM available
	private int waggleDance() 
	{
		int Min, i=0;
		Min = 0;
		int global = fitness.get(0);
		for(i=1;i<vmStatesList.size();i++)
		{
				if(fitness.get(i)< global)
				{
					global = fitness.get(i);
					Min = i;
				}
		}
		return Min;
	}
}