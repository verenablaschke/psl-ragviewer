// TODO move into etinen-gui module
package de.tuebingen.sfs.psl.gui;

import java.util.Map;
import java.util.Set;

import de.tuebingen.sfs.eie.core.DataModelManipulator;
import de.tuebingen.sfs.eie.gui.actions.EditOperationHandler;
import de.tuebingen.sfs.eie.gui.actions.atoms.AtomDeletionAction;
import de.tuebingen.sfs.eie.gui.actions.atoms.AtomFixationAction;
import de.tuebingen.sfs.eie.gui.actions.atoms.AtomInsertionAction;
import de.tuebingen.sfs.eie.gui.actions.atoms.AtomManipulationAction;
import de.tuebingen.sfs.eie.gui.actions.atoms.AtomReleaseAction;
import de.tuebingen.sfs.eie.gui.actions.atoms.AtomUserPriorAction;
import de.tuebingen.sfs.eie.gui.events.actions.ActionPerformedEvent;
import de.tuebingen.sfs.eie.gui.events.selection.AtomSelectionEvent;
import de.tuebingen.sfs.eie.gui.listeners.ButtonAppliedListener;
import de.tuebingen.sfs.eie.gui.listeners.ButtonModeSwitchListener;
import de.tuebingen.sfs.eie.gui.view.GuiPresenter;
import de.tuebingen.sfs.eie.shared.events.EtinenController;
import de.tuebingen.sfs.eie.shared.events.EtinenEvent;
import de.tuebingen.sfs.eie.shared.events.EtinenEventType;
import de.tuebingen.sfs.eie.shared.events.EtinenListener;
import de.tuebingen.sfs.eie.shared.talk.EtinenConstantRenderer;
import de.tuebingen.sfs.psl.engine.DatabaseManager;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.ConstantRenderer;
import de.tuebingen.sfs.psl.talk.TalkingPredicate;
import de.tuebingen.sfs.psl.talk.TalkingRule;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Tooltip;

public class EtinenFactWindow extends FactWindow implements EtinenListener {

	private GuiPresenter presenter;
	private EditOperationHandler opHandler;
	private EtinenController controller;

	public EtinenFactWindow(GuiPresenter presenter, String atomName, String problemId, boolean showOnlyRagAtoms) {
		this(presenter, null, null, problemId, atomName, null, null, null, null, null, null, showOnlyRagAtoms);
	}

	public EtinenFactWindow(EtinenConstantRenderer renderer, PslProblem pslProblem, RuleAtomGraph rag,
			Map<String, Double> result, boolean presortSidebar, boolean printPaneContentsToConsole) {
		this(null, renderer, pslProblem, null, null, rag, result, null, null, presortSidebar,
				printPaneContentsToConsole, null);
	}

	private EtinenFactWindow(GuiPresenter presenter, EtinenConstantRenderer constantRenderer, PslProblem pslProblem,
			String problemId, String atomName, RuleAtomGraph graph, Map<String, Double> scoreMap,
			Map<String, TalkingPredicate> talkingPreds, Map<String, TalkingRule> talkingRules, Boolean presortSidebar,
			Boolean printPaneContentsToConsole, Boolean showOnlyRagAtoms) {
		this.pslProblem = pslProblem; // Can be null.
		if (presenter == null) {
			this.presenter = null;
			this.opHandler = null;
			this.controller = new EtinenController();
			this.constantRenderer = constantRenderer; // Can be null.
		} else {
			this.presenter = presenter;
			this.opHandler = presenter.getInstance().getOperationHandler();
			this.controller = presenter.getController();
			this.constantRenderer = presenter.getConstantRenderer();
		}

		// The graph and score map can be null if the presenter is set.
		// In that case, update() below will retrieve their values.'
		this.graph = graph;
		this.scoreMap = scoreMap;

		if (atomName == null)
			atomName = "";
		atomToPredicate = null;
		// The problemId can be null.
		// Using null as first argument to properly set the current atom later,
		// once the renderer has been set.
		update(null, problemId);

		// Only non-null if neither a presenter nor a PSL problem is given.
		this.talkingPreds = talkingPreds;
		this.talkingRules = talkingRules;

		this.presortSidebar = presortSidebar == null ? false : presortSidebar;
		this.printPaneContentsToConsole = printPaneContentsToConsole == null ? false : printPaneContentsToConsole;
		this.showOnlyRagAtoms = showOnlyRagAtoms == null ? true : showOnlyRagAtoms;

		// If atomToPredicate is non-null, the methods below were already called in
		// update() above.
		if (atomToPredicate == null) {
			updateAtoms();
			setRenderer(this.constantRenderer);
		}

		setCurrentAtom(atomName);
	}

	@Override
	public Set<String> setRenderer(ConstantRenderer constantRenderer) {
		Set<String> allAtoms = super.setRenderer(constantRenderer);
		if (constantRenderer != null) {
			for (String atom : allAtoms) {
				processAtom(atom);
			}
		}
		return allAtoms;
	}

	@Override
	public void update(String atom, String problemId, boolean forceUpdate, Functionality mode) {
		setCurrentAtom(atom);
		setMode(mode);
		if (presenter != null && (forceUpdate || !problemId.equals(activeProblem))) {
			activeProblem = problemId;
			graph = presenter.getLastRuleAtomGraph(problemId);
			scoreMap = presenter.getLastValueMap(problemId);
			updateAtoms();
			setRenderer(constantRenderer);
		}
		if (graph != null && scoreMap != null)
			updateInfo();
	}

	@FXML
	@Override
	public void initialize() {
		if (presenter != null) {
			stage.setOnHidden(e -> {
				presenter.setFactWindowClosed(true);
			});
		}
		super.initialize();
		if (presenter != null) {
			DatabaseManager dbManager = presenter.getInstance().getDbManager();
			DataModelManipulator theoryManipulator = presenter.getManipulator();

			confirm.setOnAction(event -> {
				String atomString = getInternalForm(currentAtom.get());
				opHandler.execute(new AtomFixationAction(dbManager, theoryManipulator, activeProblem, atomString,
						getScore(atomString), 1.0, (mode == Functionality.FULL)));
				onFormSelection(atomString, false);
			});
			isConfirmed.addListener(new ButtonAppliedListener(confirm));

			push.setOnAction(event -> {
				String atomString = getInternalForm(currentAtom.get());
				opHandler.execute(new AtomUserPriorAction(isPushed.get(), presenter, activeProblem, atomString, 1.0,
						(mode == Functionality.FULL)));
				onFormSelection(atomString, false);
			});
			isPushed.addListener(new ButtonModeSwitchListener(push, "Apply prior", "Remove prior"));

			release.setOnAction(event -> {
				String atomString = getInternalForm(currentAtom.get());
				opHandler.execute(new AtomReleaseAction(dbManager, theoryManipulator, activeProblem, atomString,
						getScore(atomString), (mode == Functionality.FULL)));
				onFormSelection(atomString, false);
			});
			isTarget.addListener(new ButtonAppliedListener(release));

			reject.setOnAction(event -> {
				String atomString = getInternalForm(currentAtom.get());
				opHandler.execute(new AtomFixationAction(dbManager, theoryManipulator, activeProblem, atomString,
						getScore(atomString), 0.0, (mode == Functionality.FULL)));
				onFormSelection(atomString, false);
			});
			isRejected.addListener(new ButtonAppliedListener(reject));

			delete.setOnAction(event -> {
				String atomString = getInternalForm(currentAtom.get());
				AtomManipulationAction action = (isDeleted.get())
						? new AtomInsertionAction(presenter, activeProblem, atomString, (mode == Functionality.FULL))
						: new AtomDeletionAction(presenter, activeProblem, atomString, getScore(atomString),
								(mode == Functionality.FULL));
				opHandler.execute(action);
				onFormSelection(atomString, false);
			});
			isDeleted.addListener(new ButtonModeSwitchListener(delete, "Delete", "Reinsert"));

			buttonsDisabled.addListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
					if (newValue) {
						disableButton(confirm);
						disableButton(push);
						disableButton(release);
						disableButton(reject);
						disableButton(delete);
					} else {
						enableButton(confirm);
						if (mode == Functionality.FULL) {
							enableButton(push);
							enableButton(release);
						}
						enableButton(reject);
						enableButton(delete);
					}
				}
			});

			confirm.setTooltip(new Tooltip("Confirm & fixate"));
			push.setTooltip(new Tooltip("Apply prior"));
			release.setTooltip(new Tooltip("Release"));
			reject.setTooltip(new Tooltip("Reject & fixate"));
			delete.setTooltip(new Tooltip("Delete"));
		}

	}

	@Override
	public void processEvent(EtinenEvent event) {
		if (event.getType() == EtinenEventType.ACTION_PERFORMED) {
			ActionPerformedEvent apEvent = (ActionPerformedEvent) event;
			if (apEvent.getAction() instanceof AtomManipulationAction) {
				displayedAtomsListView.refresh();
			}
		}
	}

	@Override
	protected TalkingPredicate getTalkingPredicate(String predicateName) {
		String encoded = getInternalForm(predicateName);
		if (presenter != null)
			return presenter.getTalkingPredicates(activeProblem).get(encoded);
		return super.getTalkingPredicate(predicateName);
	}

	@Override
	protected Map<String, TalkingRule> getTalkingRules() {
		if (presenter != null)
			return presenter.getTalkingRules(activeProblem);
		return super.getTalkingRules();
	}

	@Override
	protected RuleAtomGraph getRag() {
		if (presenter != null) {
			RuleAtomGraph rag = presenter.getLastRuleAtomGraph(activeProblem);
			return rag;
		}
		return graph;
	}

	@Override
	protected Map<String, Double> getScoreMap() {
		if (presenter != null)
			return presenter.getLastValueMap(activeProblem);
		return scoreMap;
	}

	@Override
	public String onFormSelection(String atomString, Functionality mode, boolean addToHistory) {
		String internalForm = super.onFormSelection(atomString, mode, addToHistory);
		if (presenter != null) {
			isTarget.setValue(presenter.isTarget(activeProblem, internalForm));
			isDeleted.setValue(presenter.isDeleted(activeProblem, internalForm));
			isPushed.setValue(presenter.priorApplied(activeProblem, internalForm));
			setMode(mode);
			buttonsDisabled.setValue(graph.preventUserInteraction(getPredicate(internalForm))
					|| nonpersistedAtoms.contains(internalForm));
		}
		controller.processEvent(new AtomSelectionEvent(internalForm));
		return internalForm;
	}

	@Override
	protected boolean isDeleted(String encodedAtom) {
		return presenter != null && presenter.isDeleted(activeProblem, encodedAtom);
	}

	@Override
	protected boolean priorApplied(String encodedAtom) {
		return presenter != null && presenter.priorApplied(activeProblem, encodedAtom);
	}

}
