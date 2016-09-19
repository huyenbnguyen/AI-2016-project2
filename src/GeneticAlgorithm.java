import java.util.Collections;
import java.util.List;
import java.util.Random;

public class GeneticAlgorithm extends Algorithm {
	public static final int CUT_POINT_INDEX = 3;
	public static final int MUTATION_PROBABILITY = 30;
	public static final int CULLING_PERCENTAGE = 40; // the percentage of the population to be kept based on fitness score
	public static final double MAX_ERROR = 2000;

	public Individual bestIndividual;
	public Population population;

	public GeneticAlgorithm(double time, double startingVal, double targetVal, List<Action> actions) {
		super(time, startingVal, targetVal, actions);
		this.bestIndividual = null;
		this.population = new Population(this.problem.actions.size());
		population.initialize();
//		population.print(this.problem.startingNum);
	}

	@Override
	public StateNodeList search() {
		return null;
	}

	public void geneticAlgorithm() {
		System.out.println("time = " + this.timeLimit);
		Population currPopulation = this.population;
		int populationSize = this.population.getInitialSize();
		System.out.println("INITIAL POPULATION");
		for (int i = 0; i < populationSize; i++) {
			System.out.println(population.getIndividualSet().get(i));
		}

		long startTimer = System.currentTimeMillis();
		while  ((System.currentTimeMillis() - startTimer) < this.timeLimit) {
		//for (int x = 0;x < 10;x++){

			currPopulation = fitnessCalc(currPopulation);
			currPopulation = chooseBestIndividuals(currPopulation);
			int coupleNum = (int) Math.floor(currPopulation.getIndividualSet().size() / 2);
			for (int i = 0; i < coupleNum; i++) {
				currPopulation = reproduce(currPopulation);
			}
			currPopulation = addRandomIndividuals(currPopulation);
			System.out.println("CURRENT SIZE = " + currPopulation.getIndividualSet().size());
			currPopulation = mutate(currPopulation);

			// DEBUG
			System.out.println("NEW POPULATION");
			for (int i = 0; i < populationSize; i++) {
				System.out.println(population.getIndividualSet().get(i));
			}
		}
		this.population = currPopulation;
		this.bestIndividual = findBest(currPopulation);
//		this.bestIndividual.print(this.problem.startingNum);
	}

	private Individual findBest(Population population) {
		population.print(this.problem.startingNum);
		double bestVal = Double.MAX_VALUE;
		Individual bestIndividual = null;
		for(int i = 0; i < population.individualSet.size(); i++){
			Individual currIndividual = population.individualSet.get(i);
			double currVal = currIndividual.evaluateState(this.problem.startingNum,this.problem.actions);
			double difference = Math.abs(currVal - this.problem.targetNum);

			if(difference < bestVal){
				bestVal = difference;
				bestIndividual = currIndividual;
			}
		}
		return bestIndividual;
	}

	/*
	 * Mutate all the individuals in the population
	 */
	private Population mutate(Population population) {
		Random rand = new Random();
		for(int i = 0; i < population.individualSet.size(); i++) {
			if (mutationHappens()) {
				Individual individual = population.getIndividualSet().get(i);
				int mutate_index = rand.nextInt(Individual.MAX_DIGITS_LENGTH);
				individual.digits[mutate_index] = rand.nextInt(this.problem.actions.size());
			}
		}
		return population;
	}


	private boolean fit(Population ppl) {
		for(int i = 0; i < ppl.getIndividualSet().size(); i++){
			if(Math.abs(ppl.individualSet.get(i).evaluateState(this.problem.startingNum,this.problem.actions) - this.problem.targetNum) < MAX_ERROR){
				return true;
			}
		}
		return false;
	 }


	/*
	 * Create all the children...
	 *
	 */
	private Population reproduce(Population population) {
		Individual new_child_1 = new Individual();
		Individual new_child_2 = new Individual();

		Population childrenPopulation = new Population(0);

		// choosing random parents
		Individual x = randomSelection(population);
		Individual y = randomSelection(population);

		//System.out.println("x = " + x);
		//System.out.println("y = " + y);

		// create new_child_1
		for (int i = 0; i < CUT_POINT_INDEX; i++){
			new_child_1.digits[i] = x.digits[i];
		}
		for (int i = CUT_POINT_INDEX; i < Individual.MAX_DIGITS_LENGTH; i++){
			new_child_1.digits[i] = y.digits[i];
		}

		// create new_child_2
		for (int i = 0; i < CUT_POINT_INDEX; i++){
			new_child_2.digits[i] = y.digits[i];
		}
		for (int i = CUT_POINT_INDEX; i < Individual.MAX_DIGITS_LENGTH; i++){
			new_child_2.digits[i] = x.digits[i];
		}

		population.add(new_child_1);
		population.add(new_child_2);

		population.kill(x);
		population.kill(y);

		return population;
	}

	private boolean mutationHappens() {
		Random rand = new Random();
		int randNum = rand.nextInt(100);
		return randNum < MUTATION_PROBABILITY;
	}

	private Individual randomSelection(Population ppl) {
		Random rand = new Random();
		int selectIndex = rand.nextInt(ppl.getIndividualSet().size());
		return ppl.getIndividualSet().get(selectIndex);
	}

	public void printBestIndividual(){
		if(this.bestIndividual == null){
			return;
		}
		double startingNum = this.problem.startingNum;
		double sum = 0;
		for(int i = 0; i < Individual.MAX_DIGITS_LENGTH; i++){
			int actionIndex = this.bestIndividual.digits[i];
			if(actionIndex > 0){
				Action currAction = this.problem.actions.get(actionIndex);
				sum = currAction.getOperationResult(startingNum);
				System.out.println(startingNum + " " + currAction.printOperation() + " = "+ sum);
				startingNum = sum;
			}
		}
		this.error = Math.abs(sum - this.problem.startingNum);
	}

	private Population fitnessCalc(Population population) {
		for(int i = 0; i < population.individualSet.size(); i++){
			Individual currIndividual = population.individualSet.get(i);
			double currVal = currIndividual.evaluateState(this.problem.startingNum,this.problem.actions);
			int fitnessScore = (int) Math.round(Math.abs(currVal - this.problem.targetNum));
			population.individualSet.get(i).setFitnessScore(fitnessScore);
		}
		return population;
	}

	private Population chooseBestIndividuals(Population population) {
		Population newPopulation = new Population(population.getIndividualSet().size());
		int newPopulationSize = Math.round(population.getIndividualSet().size() * CULLING_PERCENTAGE / 100);
		System.out.println("new population size = " + newPopulationSize);

		// sort the population based on fitness score
		// the individual with the lowest fitness score (ie. its value is closest to the goal)
		// will be the first individual in the population
		Collections.sort(population.getIndividualSet());

		// choose the best individuals based on CULLING_PERCENTAGE
		// that make up the new population
		for(int i = 0; i < newPopulationSize; i++){
			Individual currIndividual = population.getIndividualSet().get(i);
			newPopulation.add(currIndividual);
		}

		System.out.println("best individuals");
		for (int i = 0; i < newPopulationSize; i++) {
			System.out.println(newPopulation.getIndividualSet().get(i));
		}

		return newPopulation;
	}

	private Population addRandomIndividuals(Population population) {
		int populationSize = population.getIndividualSet().size();
		int originalPopulationSize = population.getInitialSize();
		int individualNum = originalPopulationSize - populationSize;
		for (int i = 0; i < individualNum; i++) {
			Random rand = new Random();
			int randNum = rand.nextInt(populationSize);
			population.add(population.getIndividualSet().get(randNum));
		}
		return population;
	}
}
