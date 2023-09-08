package cloudsim.ext.datacenter;
import java.util.*;

import cloudsim.ext.Constants;
import cloudsim.ext.event.CloudSimEvent;
import cloudsim.ext.event.CloudSimEventListener;
import cloudsim.ext.event.CloudSimEvents;

public class honeyBee extends VmLoadBalancer implements CloudSimEventListener {

	private double initialTemperature = 100;
	private double coolingRate = 0.03;
	private double currentTemperature = initialTemperature;
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
	public int getNextAvailableVm() {
		int vmId = -1;
		vmId = getScoutBee();
		allocatedVm(vmId);

		currentTemperature *= 1 - coolingRate;
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

	/* This will return food source */
	int getScoutBee()
	{
		if(scoutBee==-1)
		{
			if(vmStatesList.size()>0)
				return 0;
			else
				return -1;
		}
		else
		{
			if(isSendScoutBees(scoutBee)==false)
				return scoutBee;
			else
			{
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

	void SendEmployedBees()
	{
		fitness.clear();
		calculation();
	}

	// Calculation to get the fitness value of VM
	private void calculation() {
		int i;
		/* Employed Bee Phase */
		for (i = 0; i < vmStatesList.size(); i++) {
			if (vmAllocationCounts.get(i) == null)
				fitness.put(i, 0);
			else
				fitness.put(i, calculateFitness(vmAllocationCounts.get(i)));
		}

		/* Simulated Annealing */
		int currentSolutionFitness = fitness.get(scoutBee);
		int neighborSolutionFitness = fitness.get(getNeighborSolution(scoutBee));
		int deltaE = neighborSolutionFitness - currentSolutionFitness;

		if (deltaE < 0) {
// neighbor solution is better, accept it
			scoutBee = getNeighborSolution(scoutBee);
		} else {
// neighbor solution is worse, calculate acceptance probability
			double acceptanceProbability = Math.exp(-deltaE / currentTemperature);
			if (acceptanceProbability > Math.random()) {
				scoutBee = getNeighborSolution(scoutBee);
			}
		}
	}

	private int getNeighborSolution(int solution) {
		int neighbor = solution + (int) Math.round((Math.random() * 2) - 1);
		if (neighbor < 0 || neighbor >= vmStatesList.size()) {
			neighbor = solution;
		}
		return neighbor;
	}
	int calculateFitness(int solValue)
	{
		solValue = 1/(1000-solValue);
		return solValue;	//// tasklength/VM capacity
	}

	// Bees went in search & finding all the fitness



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