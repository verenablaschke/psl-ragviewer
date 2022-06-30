# PSL RAGviewer

This package contains code for inspecting the inference results of a Probabilistic Soft Logic (PSL) model by navigating its rule-atom graph (RAG).

The `FactWindow` is a JavaFX-based atom browser that explains each atom's inferred value based on the ground rules in which appears.
The associated rules can be shown in the form of rule groundings or as textual explanations that express how the context atom's value fits into the rule's reasoning pattern, or how it violates it.
To create a PSL RAG that is inspectable this way, use the code in the [psl-infrastructure](https://github.com/jdellert/psl-infrastructure) package, which provides a Java API for the [PSL framework](https://psl.linqs.org/) as well as the code necessary for creating natural language verbalization templates of rules and predicates.

## Usage
``` Java
import de.tuebingen.sfs.psl.engine.InferenceResult;
import de.tuebingen.sfs.psl.engine.ProblemManager;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.gui.StandaloneFactWindowLauncher;

public class MyApp {

	public static void main(String[] args) {
		ProblemManager problemManager = ProblemManager.defaultProblemManager();

		// Create custom classes MyConfig, MyProblem, and MyIdeaGenerator
		// that extend the de.tuebingen.sfs.psl.engine classes
		// PslProblemConfig, PslProblem, and IdeaGenerator, respectively.
		MyConfig config = new MyConfig();
		// Use the PslProblem class to define the rules and predicates:
		MyProblem problem = new MyProblem(config);
		MyIdeaGenerator ideaGen = new MyIdeaGenerator(problem);
		// Add all (target and observation) atoms that should be used for the inference:
		ideaGen.generateAtoms();
		// Run the inference:
		InferenceResult result = problemManager.registerAndRunProblem(problem);
		// Inspect the results:
		ConstantRenderer constantRenderer = null;
		boolean sortSidebar = true;
		boolean printExplanationsToConsole = false;
		StandaloneFactWindowLauncher.launchWithData(constantRenderer, problem, result, sortSidebar, printExplanationsToConsole);
	}

}
```

## Example

For a very simple example problem, please run the [sample application](https://github.com/verenablaschke/psl-ragviewer/blob/master/src/main/java/de/tuebingen/sfs/psl/gui/examples/livesknows/EntryClass.java).
The logic behind the corresponding psl-infrastructure class is explained in that package's [wiki](https://github.com/jdellert/psl-infrastructure/wiki/Example:-Lives-&-Knows).

To run the sample code, you need to create a new IntelliJ project into which you clone both psl-infrastructure and psl-ragviewer.
IntelliJ should recognize both of them as Java modules.
(Make sure each module's src/main/java and src/main/resources directories are added as module roots!
If there are build problems, open the Java compiler settings via `File > Settings > Build, Execution, Deployment > Compiler > Java Compiler` and **de**select `Use '--release' option for cross-compilation`.)