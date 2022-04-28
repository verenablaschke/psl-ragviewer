package de.tuebingen.sfs.psl.gui;

import de.tuebingen.sfs.eie.shared.talk.EtinenConstantRenderer;

public class EtinenStandaloneFactWindowLauncher extends StandaloneFactWindowLauncher {

	@Override
	public FactWindow createFactWindow() {
		if (pslProblem == null)
			return new FactWindow(renderer, rag, talkingPreds, talkingRules, result, sortSidebar,
					printExplanationPanesToConsole);
		return new EtinenFactWindow((EtinenConstantRenderer) renderer, pslProblem, rag, result, sortSidebar,
				printExplanationPanesToConsole);
	}
}
