package es.uniovi.ssii.rb;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openmarkov.core.exception.IncompatibleEvidenceException;
import org.openmarkov.core.exception.InvalidStateException;
import org.openmarkov.core.exception.NodeNotFoundException;
import org.openmarkov.core.exception.NotEvaluableNetworkException;
import org.openmarkov.core.exception.UnexpectedInferenceException;
import org.openmarkov.core.inference.tasks.Propagation;
import org.openmarkov.core.model.network.EvidenceCase;
import org.openmarkov.core.model.network.Finding;
import org.openmarkov.core.model.network.ProbNet;
import org.openmarkov.core.model.network.Variable;
import org.openmarkov.core.model.network.potential.TablePotential;
import org.openmarkov.gui.dialog.io.NetsIO;
import org.openmarkov.inference.likelihoodWeighting.LogicSampling;

/**
 * Models the problem of obtaining the following probability: <br> <br> 
 * 
 * P( Dyspnoea?=no | Has lung cancer=yes, Visit to Asia?=no )
 * 
 * @author 
 * 				Hugo Fonseca Díaz (UO258318)
 */
public class UO258318 {

	// Probability Network
	private ProbNet probNet;

	// Constructor
	public UO258318(String fileName) throws Exception {
		probNet = NetsIO.openNetworkFile("src/main/resources/networks/" + fileName).getProbNet();
	}

	// Getters and setters
	public ProbNet getProbNet() {
		return probNet;
	}

	public void setProbNet(ProbNet probNet) {
		this.probNet = probNet;
	}

	/**
	 * Calculates a Logic Sampling Inference for a given evidence and
	 * variables of interest.
	 * 
	 * @param variablesOfInterest
	 * 			Variables of interest of the case.
	 * @param evidence
	 * 			Evidence of the case.
	 * @return
	 * 			The time it took for the inference to finish.
	 */
	public long LSInference(List<Variable> variablesOfInterest, EvidenceCase evidence) {
		LogicSampling propagation = null;
		try {
			propagation = new LogicSampling(probNet);
		} catch (NotEvaluableNetworkException e) {
			e.printStackTrace();
		}
		// Approximation algorithms require a sample size specification
		propagation.setSampleSize(5000);
		return measureTime(variablesOfInterest, evidence, propagation);
	}

	/**
	 * Gets the evidence for the current case. In this problem, there are two findings,
	 * "Has lung cancer" = yes and "Visit to Asia?" = no.
	 * 
	 * @return
	 * 			The evidence for the current case.
	 */
	private EvidenceCase getEvidence() {
		EvidenceCase evidence = new EvidenceCase();
		try {
			// Has lung cancer = YES
			Variable lungCancer = getProbNet().getVariable("Has lung cancer");
			evidence.addFinding(getProbNet(), lungCancer.getName(), 
					lungCancer.getState("yes").getName());
			// Visit to Asia? = NO
			Variable visitAsia = getProbNet().getVariable("Visit to Asia?");
			evidence.addFinding(getProbNet(), visitAsia.getName(),
					visitAsia.getState("no").getName());
		} catch (NodeNotFoundException e) {
			e.printStackTrace();
		} catch (InvalidStateException e) {
			e.printStackTrace();
		} catch (IncompatibleEvidenceException e) {
			e.printStackTrace();
		}
		return evidence;
	}

	/**
	 * Returns the variables of interest for the case. In this problem there is
	 * only one variable of interest. That variable is "Dysponea?".
	 * 
	 * @return
	 * 			A list of variables of interest for the case.
	 */
	private List<Variable> getVariablesOfInterest() {
		List<Variable> variablesOfInterest = new ArrayList<Variable>();
		try {
			Variable dyspnoea = getProbNet().getVariable("Dyspnoea?");
			variablesOfInterest.add(dyspnoea);
		} catch (NodeNotFoundException e) {
			e.printStackTrace();
		}
		return variablesOfInterest;
	}

	/**
	 * Measures the time for the inference to perform its calculations. The time
	 * is expressed in nanoseconds.
	 * 
	 * @param variablesOfInterest
	 * 			The variables of interest for the case.
	 * @param evidence
	 * 			The evidence of the case.
	 * @param propagation
	 * 			The propagation used for solving the inference.
	 * @return
	 * 			The time for the inference to finish the execution in nanoseconds.
	 */
	private long measureTime(List<Variable> variablesOfInterest, EvidenceCase evidence, Propagation propagation) {
		propagation.setVariablesOfInterest(variablesOfInterest);
		propagation.setPostResolutionEvidence(evidence);

		System.out.print("Variable elimination\n");
		long startTime = System.nanoTime();
		try {
			Map<Variable, TablePotential> posteriorProbabilities = propagation.getPosteriorValues();
			printProbabilities(evidence, variablesOfInterest, posteriorProbabilities);
		} catch (IncompatibleEvidenceException | NotEvaluableNetworkException e) {
			e.printStackTrace();
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
		} catch (UnexpectedInferenceException e) {
			e.printStackTrace();
		}
		long endTime = System.nanoTime();

		printTime(endTime - startTime);

		return (endTime - startTime);
	}

	/**
	 * Prints the probabilities for the case.
	 * 
	 * @param evidence
	 * 			The evidence of the case.
	 * @param variablesOfInterest
	 * 			The variables of interest for the case.
	 * @param posteriorProbabilities
	 * 			The posterior probabilities of the case.
	 */
	public static void printProbabilities(EvidenceCase evidence, List<Variable> variablesOfInterest,
			Map<Variable, TablePotential> posteriorProbabilities) {

		String evidenceString = "";
		for (Finding finding : evidence.getFindings()) {
			evidenceString += " " + finding.getVariable() + "=" + finding.getState();
		}

		for (Variable variable : variablesOfInterest) {
			TablePotential posteriorProbabilitiesPotential = posteriorProbabilities.get(variable);
			System.out.format("P( %s=%s | %s) = %.5f\n", variable, variable.getStates()[0].getName(), evidenceString,
					posteriorProbabilitiesPotential.values[0]);
		}
	}

	/**
	 * Prints the execution time.
	 * 
	 * @param nanoseconds
	 * 			Execution time in nanoseconds.
	 */
	public static void printTime(long nanoseconds) {
		System.out.format("Total time: %.2f miliseconds\n", nanoseconds / 1000000.0);
	}

	public static void main(String[] args) throws Exception {
		// An instance of the class is created
		UO258318 obj = new UO258318("asia.pgmx");

		// General info about the network
		System.out.format("Network \"%s\" with %d nodes and %d links\n", obj.getProbNet().getName(),
				obj.getProbNet().getNumNodes(), obj.getProbNet().getLinks().size());

		// The evidence and variables of interest for the case are retrieved
		EvidenceCase evidence = obj.getEvidence();
		List<Variable> variablesOfInterest = obj.getVariablesOfInterest();

		// The Logic Sampling Inference is calculated
		obj.LSInference(variablesOfInterest, evidence);
	}

}
