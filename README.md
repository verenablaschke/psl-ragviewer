# PSL RAGviewer

This package contains code for inspecting the inference results of a Probabilistic Soft Logic (PSL) model by navigating its rule-atom graph (RAG).
The `FactWindow` is a JavaFX-based atom browser that explains each atom's inferred value based on the ground rules in which appears.
The associated rules can be shown in the form of rule groundings or as natural language explanations that express how the context atom's value fits into the rule's reasoning pattern, or how it violates it.
To create a PSL RAG that is inspectable this way, use the code in [psl-infrastructure](https://github.com/jdellert/psl-infrastructure), which provides a Java-based API for the [PSL framework](https://psl.linqs.org/) as well as the code necessary for creating natural language verbalization templates of rules and predicates.


```
import de.tuebingen.sfs.psl.engine.InferenceResult;
import de.tuebingen.sfs.psl.engine.ProblemManager;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.gui.StandaloneFactWindowLauncher;

public class SampleApp {

	public static void main(String[] args) {
		ProblemManager problemManager = ProblemManager.defaultProblemManager();

		// Create custom classes SampleConfig, SampleProblem, and SampleIdeaGenerator that extend the de.tuebingen.sfs.psl.engine classes PslProblemConfig, PslProblem, and IdeaGenerator, respectively.
		SampleConfig config = new SampleConfig();
		// Use the PslProblem class to define the rules and predicates:
		SampleProblem problem = new SampleProblem(config);
		SampleIdeaGenerator ideaGen = new SampleIdeaGenerator(problem);
		// Add all (target and observation) atoms that should be used for the inference:
		ideaGen.generateAtoms();
		// Run the inference:
		InferenceResult result = problemManager.registerAndRunProblem(problem);
		// Inspect the results:
		ConstantRenderer constantRenderer = null;
		boolean sortSidebar = true;
		boolean printExplanationsToConsole = true;
		StandaloneFactWindowLauncher.launchWithData(constantRenderer, problem, result, sortSidebar, printExplanationsToConsole);
	}

}
```