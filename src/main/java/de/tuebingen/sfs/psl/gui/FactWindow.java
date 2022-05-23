package de.tuebingen.sfs.psl.gui;

import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.ConstantRenderer;
import de.tuebingen.sfs.psl.talk.TalkingPredicate;
import de.tuebingen.sfs.psl.talk.TalkingRule;
import de.tuebingen.sfs.psl.util.color.ColorUtils;
import de.tuebingen.sfs.psl.util.color.HslColor;
import de.tuebingen.sfs.psl.util.data.RankingEntry;
import de.tuebingen.sfs.psl.util.data.Tuple;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;
import org.h2.util.StringUtils;
import org.linqs.psl.model.rule.GroundRule;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FactWindow {

    protected static final Pattern ATOM_START_PATTERN = Pattern.compile("\\w{4,}\\(");
    // Display choices
    protected boolean showOnlyRagAtoms;
    protected boolean showRuleVerbalization = true;
    protected Set<String> ragAtoms;
    protected Set<String> scoreMapAtoms;
    protected Set<String> unusedAtoms;
    protected Set<String> nonpersistedAtoms;
    protected boolean reloadingSidebar = false;
    protected PslProblem pslProblem;
    protected Map<String, TalkingPredicate> talkingPreds;
    protected Map<String, TalkingRule> talkingRules;
    @FXML
    protected HBox detailBox; // used by EtinenFactWindow
    @FXML
    protected Button back;
    @FXML
    protected Button fwrd;
    @FXML
    protected TextFlow atomTitle;
    @FXML
    protected TextFlow atomVerbalizationPane;
    @FXML
    protected VBox whyNotBox;
    @FXML
    protected ScrollPane whyNotScrollPane;
    @FXML
    protected VBox whyBox;
    @FXML
    protected ScrollPane whyScrollPane;
    @FXML
    protected Label beliefValueLabel;
    @FXML
    protected Label sortLabel;
    @FXML
    protected CheckMenuItem unusedAtomsChoice;
    @FXML
    protected CheckMenuItem internalRuleRepChoice;
    @FXML
    protected CheckMenuItem distToSatisfactionChoice;
    @FXML
    protected CheckMenuItem counterfactualChoice;
    // JavaFX
    protected Text atomText;
    protected VBox container;
    protected Stage stage;
    protected Stage selectedItemStage;
    protected ScrollPane scrollAtomPane;
    @FXML
    protected ListView<String> displayedAtomsListView;
    @FXML
    protected GridPane atomFilter;
    // Observables
    protected ObservableList<String> displayedAtoms;
    protected StringProperty currentAtom = new SimpleStringProperty();
    protected DoubleProperty currentScore = new SimpleDoubleProperty(-1.0);
    protected RuleAtomGraph graph;
    // Java variables
    protected Stack<String> nextAtoms;
    protected Stack<String> previousAtoms;
    protected String activeProblem; // TODO move into EtinenFactWindow
    protected boolean presortSidebar;
    protected boolean displayDistToSatisfaction;
    protected boolean displayCounterfactual;
    protected boolean printPaneContentsToConsole;
    // Maps for atom elements
    protected Map<String, Double> scoreMap;
    protected Map<String, String> atomToPredicate;
    protected Map<String, List<String>> atomToDisplayArguments;
    // For correctly displaying EtinenTalkingRules/Predicates.
    protected ConstantRenderer constantRenderer;
    protected Map<String, String> encodedAtomToSurface = new HashMap<>();
    protected Map<String, String> surfaceAtomToEncoded = new HashMap<>();

    protected FactWindow() {
        // For subclasses.
    }

    public FactWindow(PslProblem pslProblem, RuleAtomGraph graph, Map<String, Double> scoreMap) {
        this(pslProblem, graph, scoreMap, true);
    }

    public FactWindow(PslProblem pslProblem, RuleAtomGraph graph, Map<String, Double> scoreMap,
                      boolean showOnlyRagAtoms) {
        this(null, pslProblem, null, graph, scoreMap, null, null, null, null, showOnlyRagAtoms);
    }

    public FactWindow(ConstantRenderer renderer, RuleAtomGraph rag, Map<String, TalkingPredicate> talkingPreds,
                      Map<String, TalkingRule> talkingRules, Map<String, Double> result, boolean presortSidebar,
                      boolean printPaneContentsToConsole) {
        this(renderer, null, null, rag, result, talkingPreds, talkingRules, presortSidebar, printPaneContentsToConsole,
                null);
    }

    // 2 options:
    // - StandaloneFactViewer: pslProblem, graph, scoreMap
    // - RagViewer: graph, scoreMap, talkingPreds, talkingRules
    protected FactWindow(ConstantRenderer constantRenderer, PslProblem pslProblem, String atomName, RuleAtomGraph graph,
                         Map<String, Double> scoreMap, Map<String, TalkingPredicate> talkingPreds,
                         Map<String, TalkingRule> talkingRules, Boolean presortSidebar, Boolean printPaneContentsToConsole,
                         Boolean showOnlyRagAtoms) {
        this.pslProblem = pslProblem; // Can be null.
        this.graph = graph;
        this.scoreMap = scoreMap;

        if (atomName == null)
            atomName = "";
        atomToPredicate = null;

        if (pslProblem == null) {
            this.talkingPreds = talkingPreds;
            this.talkingRules = talkingRules;
        } else {
            this.talkingPreds = pslProblem.getTalkingPredicates();
            this.talkingRules = pslProblem.getTalkingRules();
        }
        System.err.println(this.talkingPreds);

        this.presortSidebar = presortSidebar != null && presortSidebar;
        this.printPaneContentsToConsole = printPaneContentsToConsole != null && printPaneContentsToConsole;
        this.showOnlyRagAtoms = showOnlyRagAtoms == null || showOnlyRagAtoms;

        updateAtoms();
        setRenderer(constantRenderer); // Can be null.
        setCurrentAtom(atomName);
    }

    public static double getToneForAtom(String atomRepresentation, Map<String, Double> scoreMap) {
        // System.err.println("getToneForAtom(" + atomRepresentation + ")");
        if (scoreMap == null)
            return 0.0;
        else {
            Double alpha = scoreMap.get(atomRepresentation);
            if (alpha == null)
                return 0.0;
            if (alpha >= 0 && alpha <= 1)
                return 1.0 - alpha;
            return alpha;
        }

    }

    // MorphView: Copy this method.
    public static String getBackgroundColorForScore(HslColor baseColor, Map<String, Double> scoreMap, String atom) {
        double toneForAtom = getToneForAtom(atom, scoreMap);
        return getBackgroundColorForScore(baseColor, toneForAtom);
    }

    public static String getBackgroundColorForScore(HslColor baseColor, double toneForAtom) {
        if (toneForAtom == Double.POSITIVE_INFINITY) {
            return "#000000";
        } else if (toneForAtom == 2.0) {
            return "#006400";
        } else if (toneForAtom == -1.0) {
            return "#ff8c00";
        } else if (toneForAtom == Double.NEGATIVE_INFINITY) {
            return "#ffffff";
        } else {
            Color resultColor = baseColor.adjustLuminance(
                    baseColor.getLuminance() + (float) (toneForAtom * (100 - baseColor.getLuminance())));
            return ColorUtils.colorHTML(resultColor);
        }
    }

    // TODO move into EtinenFactWindow
    protected static void disableButton(Button button) {
        button.getStyleClass().add("btn-disabled");
        button.setDisable(true);
    }

    // TODO move into EtinenFactWindow
    protected static void enableButton(Button button) {
        button.getStyleClass().remove("btn-disabled");
        button.setDisable(false);
    }

    protected void updateAtoms() {
        ragAtoms = graph.getAtomNodes();
        scoreMapAtoms = scoreMap.keySet();
        unusedAtoms = new TreeSet<>();
        unusedAtoms.addAll(scoreMapAtoms);
        unusedAtoms.removeAll(ragAtoms);
        nonpersistedAtoms = new TreeSet<>();
        nonpersistedAtoms.addAll(ragAtoms);
        nonpersistedAtoms.removeAll(scoreMapAtoms);
    }

    public Set<String> setRenderer(ConstantRenderer constantRenderer) {
        this.constantRenderer = constantRenderer;
        atomToPredicate = new TreeMap<>();
        atomToDisplayArguments = new TreeMap<>();
        encodedAtomToSurface = new TreeMap<>();
        surfaceAtomToEncoded = new TreeMap<>();
        System.err.println("Start setting up the atom maps.");
        Set<String> allAtoms = new TreeSet<>();
        allAtoms.addAll(scoreMapAtoms);
        allAtoms.addAll(ragAtoms);
        if (constantRenderer == null) {
            for (String atom : allAtoms) {
                encodedAtomToSurface.put(atom, atom);
                surfaceAtomToEncoded.put(atom, atom);
                List<String> atomElems = getAtomElements(atom);
                atomToPredicate.put(atom, atomElems.get(0));
                atomToDisplayArguments.put(atom, atomElems.subList(1, atomElems.size()));
            }
        } else {
            for (String atom : allAtoms) {
                processAtom(atom);
            }
        }
        System.err.println("DONE setting up the atom maps.");
        return allAtoms;
    }

    protected void processAtom(String atom) {
        List<String> atomElems = getAtomElements(atom);
        TalkingPredicate pred = getTalkingPredicate(atomElems.get(0));
        String surface = atom;
        List<String> args = atomElems.subList(1, atomElems.size());
        if (constantRenderer != null && pred != null) {
            args = pred.retrieveArguments(constantRenderer, args.toArray(new String[atomElems.size() - 1]));
            surface = atomElems.get(0) + "(" + String.join(", ", args) + ")";
        }
        encodedAtomToSurface.put(atom, surface);
        surfaceAtomToEncoded.put(surface, atom);
        atomToPredicate.put(atom, atomElems.get(0));
        atomToDisplayArguments.put(atom, args);
    }

    public void update(String atom) {
        update(atom, false);
    }

    public void update(String atom, boolean forceUpdate) {
        setCurrentAtom(atom);
        updateInfo();
    }

    public void updateInfo() {
        ScoreService scoreService = new ScoreService();
        scoreService.start();
    }

    public FactWindow getWindow() {
        return this;
    }

    @FXML
    public void initialize() {
        update(currentAtom.get());

        scrollAtomPane = new ScrollPane();
        scrollAtomPane.setPannable(true);
        scrollAtomPane.setFitToWidth(true);

        beliefValueLabel.setFont(Font.font("Verdana", FontWeight.BOLD, 14));
        atomText = updateAtomText(new Text());
        // tf.getChildren().add(atomText);
        // atomVerbalizationPane.getChildren().add(atomText);
        // onFormSelection(currentAtom.get());

        // Display options in the MenuBar
        unusedAtomsChoice.setSelected(!showOnlyRagAtoms);
        unusedAtomsChoice.setOnAction((ActionEvent event) -> {
            showOnlyRagAtoms = !unusedAtomsChoice.isSelected();
            reloadingSidebar = true;
            updateInfo();
            reloadingSidebar = false;
        });
        internalRuleRepChoice.setSelected(false);
        internalRuleRepChoice.setOnAction((ActionEvent event) -> {
            showRuleVerbalization = !internalRuleRepChoice.isSelected();
            setFacts(graph);
        });
        distToSatisfactionChoice.setSelected(false);
        distToSatisfactionChoice.setOnAction((ActionEvent event) -> {
            displayDistToSatisfaction = distToSatisfactionChoice.isSelected();
            setFacts(graph);
        });
        counterfactualChoice.setSelected(false);
        counterfactualChoice.setOnAction((ActionEvent event) -> {
            displayCounterfactual = counterfactualChoice.isSelected();
            setFacts(graph);
        });

        previousAtoms = new Stack<>();
        back.setMaxWidth(Double.MAX_VALUE);
        back.setOnAction(event -> {
            if (!previousAtoms.empty()) {
                nextAtoms.push(currentAtom.get());
                onFormSelection(previousAtoms.pop(), false);
            }
        });

        nextAtoms = new Stack<>();
        fwrd.setMaxWidth(Double.MAX_VALUE);
        fwrd.setOnAction(event -> {
            if (!nextAtoms.empty()) {
                previousAtoms.push(currentAtom.get());
                onFormSelection(nextAtoms.pop(), false);
            }
        });

        // Set various display possibilities for atom belief
        beliefValueLabel.textProperty().bind(Bindings.when(currentAtom.isEmpty()).then("")
                .otherwise(Bindings.when(currentScore.greaterThan(1.0)).then("+")
                        .otherwise(Bindings.when(currentScore.lessThan(0.0)).then("-")
                                .otherwise(Bindings.format(Locale.ENGLISH, "%.2f %%", currentScore.multiply(100))))));

    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    protected TalkingPredicate getTalkingPredicate(String predicateName) {
        String encoded = getInternalForm(predicateName);
        if (pslProblem != null)
            return pslProblem.getTalkingPredicates().get(encoded);
        return talkingPreds.get(encoded);
    }

    protected Map<String, TalkingRule> getTalkingRules() {
        if (pslProblem != null)
            return pslProblem.getTalkingRules();
        return talkingRules;
    }

    protected List<String> getAtomElements(String atomString) {
        String[] elems = atomString.split("\\(");
        String[] args = elems[1].substring(0, elems[1].length() - 1).split(", ");
        List<String> atomElems = new ArrayList<>(args.length + 1);
        atomElems.add(elems[0]);
        atomElems.addAll(Arrays.asList(args));
        return atomElems;
    }

    // Switch to the form of the atom that should be displayed to the user
    // (Based on the EtinenConstantRenderer)
    protected String getDisplayForm(String atom) {
        return encodedAtomToSurface.getOrDefault(atom, atom);
    }

    // To be sure you're using the correct version of an atom string,
    // use the following methods to interact with some of the instance variables.

    // Switch to the internally encoded form of the atom (with
    // IndexedObjectStore-encoded arguments).
    // Use this when interacting with any of the atom-based maps (or use the
    // dedicated functions below)
    // and when using Actions/Events or when passing atom information to the
    // GuiPresenter.
    protected String getInternalForm(String atom) {
        return surfaceAtomToEncoded.getOrDefault(atom, atom);
    }

    protected void setCurrentAtom(String atom) {
        if (atom == null || atom.isEmpty())
            currentAtom.set("");
        else
            currentAtom.set(getDisplayForm(atom));
    }

    protected Double getScore(String atom) {
        Double score = scoreMap.get(getInternalForm(atom));
        if (score == null)
            // Non-persisted atom
            score = graph.getValue(atom);
        return score;
    }

    protected List<String> getDisplayArguments(String atom) {
        List<String> args = atomToDisplayArguments.get(getInternalForm(atom));
        if (args == null) {
            processAtom(atom);
            args = atomToDisplayArguments.get(getInternalForm(atom));
            System.err.println("Encountered new atom " + atom
                    + " and updated the atom maps accordingly. (This should not have happened.)");
        }
        return args;
    }

    protected String getPredicate(String atom) {
        String pred = atomToPredicate.get(getInternalForm(atom));
        if (pred == null) {
            processAtom(atom);
            pred = atomToPredicate.get(getInternalForm(atom));
            System.err.println("Encountered new atom " + atom
                    + " and updated the atom maps accordingly. (This should not have happened.)");
        }
        return pred;
    }

    protected Text updateAtomText(Text atomText) {
        atomText.textProperty().bind(currentAtom);
        // atomText.setFont(Font.font("Verdana", FontWeight.NORMAL, 14));
        atomText.setStyle("-fx-font-style: italic");
        atomText.setStyle("-fx-font: 20 arial");
        return atomText;
    }

    public void setFacts(RuleAtomGraph rag) {
        whyBox.getChildren().clear();
        whyNotBox.getChildren().clear();

        List<String> notUnderPressureList = new ArrayList<>();
        for (RankingEntry<String> s : rag.rankGroundingsByPressure(Double.NEGATIVE_INFINITY)) {
            notUnderPressureList.add(s.key);
        }
        String currentAtom = getInternalForm(this.currentAtom.get());

        List<RankingEntry<Pair<String, Boolean>>> whyRules = new ArrayList<>();
        List<RankingEntry<Pair<String, Boolean>>> whyNotRules = new ArrayList<>();

        // If the atom's value wasn't inferred, we can hardcode the explanation:
        if (graph.isFixed(currentAtom)) {
            whyRules.add(new RankingEntry<>(new Pair<>("The value of this atom was fixed before the inference.", true), -1));
            setExplanationBox(whyRules, whyBox);
            whyNotRules.add(new RankingEntry<>(new Pair<>("The value of this atom was fixed before the inference.", true), -1));
            setExplanationBox(whyNotRules, whyNotBox);
            return;
        }

        for (Tuple tup : rag.getOutgoingLinks(currentAtom)) {
            String rule = tup.get(1);
            if (notUnderPressureList.contains(rule)) {
                String ruleName = rule.substring(0, rule.indexOf('['));
                List<Tuple> atomToStatus = rag.getLinkedAtomsForGroundingWithLinkStatusAsList(rule);
                String[] groundAtoms = new String[atomToStatus.size()];
                int currentAtomIndex = -1;
                for (int i = 0; i < groundAtoms.length; i++) {
                    groundAtoms[i] = atomToStatus.get(i).get(0);
                    if (groundAtoms[i].equals(currentAtom))
                        currentAtomIndex = i;
                }
                String status = atomToStatus.get(currentAtomIndex).get(1);
                if (rag.isEqualityRule(rule) || status.equals("+")) {
                    whyRules.add(generateWeightedExplanation(ruleName, rule, currentAtom, rag, true));
                }
                if (rag.isEqualityRule(rule) || status.equals("-")) {
                    whyNotRules.add(generateWeightedExplanation(ruleName, rule, currentAtom, rag, false));
                }
            }
        }

        if (printPaneContentsToConsole) {
            if (currentAtom.isEmpty()) {
                System.out.println("[Empty atom.]");
            } else {
                System.out.println("Atom: " + getPredicate(currentAtom) + getDisplayArguments(currentAtom));
            }
            System.out.println("WHY ? (WHY NOT LESS LIKELY / Rules pushing belief upwards)");
        }
        setExplanationBox(whyRules, whyBox);
        if (printPaneContentsToConsole) {
            System.out.println("WHY NOT ? (WHY NOT MORE LIKELY / Rules pushing belief downwards)");
        }
        setExplanationBox(whyNotRules, whyNotBox);
        if (printPaneContentsToConsole) {
            System.out.println("---------\n");
        }
    }

    protected RankingEntry<Pair<String, Boolean>> generateWeightedExplanation(String ruleName, String groundingName,
                                                                              String contextAtom, RuleAtomGraph rag, boolean whyExplanation) {
        String explanation = generateExplanation(talkingRules, ruleName, groundingName, contextAtom, rag, whyExplanation);
        double dist = rag.distanceToSatisfaction(groundingName);
        boolean influence = rag.putsPressureOnGrounding(contextAtom, groundingName);
        double score = scoreMap.get(contextAtom);
        if ((Math.abs(score - 1.0) < 0.0000000001 && !whyExplanation) || (Math.abs(score - 0.0) < 0.0000000001 && whyExplanation)) {
            return new RankingEntry<>(new Pair<>(explanation, true), dist * 10);
        }
        Double counterfactualDist = graph.getCounterfactual(contextAtom, groundingName);
        if (displayCounterfactual && (score > 0.0 || score < 1.0)) {
            if (counterfactualDist == null) {
                double[] counterfactualDistances = rag.getCounterfactualsForEqualityRule(contextAtom, groundingName);
                if (whyExplanation) {
                    explanation += generateCounterfactualExplanation(counterfactualDistances[0], dist, score - RuleAtomGraph.COUNTERFACTUAL_OFFSET, contextAtom);
                    counterfactualDist = counterfactualDistances[0];
                } else {
                    explanation += generateCounterfactualExplanation(counterfactualDistances[1], dist, score + RuleAtomGraph.COUNTERFACTUAL_OFFSET, contextAtom);
                    counterfactualDist = counterfactualDistances[1];
                }
            } else {
                double counterfactualAtomVal = score + (whyExplanation ? -1.0 : 1.0) * RuleAtomGraph.COUNTERFACTUAL_OFFSET;
                explanation += generateCounterfactualExplanation(counterfactualDist, dist, counterfactualAtomVal, contextAtom);
            }
        }
        if (displayDistToSatisfaction) {
            explanation = String.format("%.4f: %s", dist, explanation);
        }
        if (counterfactualDist == null) {
            counterfactualDist = 0.0;
        }
        return new RankingEntry<>(new Pair<>(explanation, influence), dist * 10 + counterfactualDist);
    }

    protected String generateCounterfactualExplanation(double counterfactualDist, double dist, double counterfactualAtomVal, String contextAtom) {
        String counterfactualExpl;
        double distDiff = counterfactualDist - dist;
        if (showRuleVerbalization) {
            StringBuilder sb = new StringBuilder();
            // TODO talking pred!!
            sb.append("\nIf ").append(contextAtom).append(" had had a value of ").append(String.format("%.2f", 100 * counterfactualAtomVal));
            sb.append(" %, then the distance to satisfaction would have been ");
            sb.append(String.format("%.4f", counterfactualDist)).append(".");
            counterfactualExpl = sb.toString();
        } else {
            counterfactualExpl = String.format("\n  %.4f: if score is %.2f", counterfactualDist, 100 * counterfactualAtomVal);
        }
        if (Math.abs(distDiff) > 0.0000001) {
            counterfactualExpl += String.format(" (%s%.4f)", distDiff >= 0 ? "+" : "-", +distDiff);
        }
        return counterfactualExpl;
    }

    protected String generateExplanation(Map<String, TalkingRule> talkingRules, String ruleName, String groundingName,
                                         String contextAtom, RuleAtomGraph rag, boolean whyExplanation) {
        if (!showRuleVerbalization) {
            StringBuilder sb = new StringBuilder();
            GroundRule rule = graph.getRuleForGrounding(groundingName);
            sb.append(pslProblem.getNameForRule(rule.getRule())).append(": ");
            String ruleStr = rule.toString().replaceAll("', '", "','");
            for (String element : ruleStr.split("\\s+")) {
                Matcher matcher = ATOM_START_PATTERN.matcher(element);
                if (matcher.find()) {
                    element = element.replaceAll("','", "', '");
                    String pred = TalkingPredicate.getPredNameFromAllCaps(element.split("\\(")[0]);
                    String[] argsWithApostrophe = TalkingPredicate.extractArgs(element);
                    String[] args = new String[argsWithApostrophe.length];
                    for (int i = 0; i < args.length; i++) {
                        args[i] = argsWithApostrophe[i].substring(1, argsWithApostrophe[i].length() - 1);
                    }
                    String atom = pred + "(" + de.tuebingen.sfs.psl.util.data.StringUtils.join(args, ", ") + ")";
                    sb.append("\\url[").append(getDisplayForm(atom)).append("]{");
                    sb.append(atom).append("} [");
                    Double belief = scoreMap.get(atom);
                    if (belief == null) {
                        sb.append("???");
                    } else {
                        sb.append("%.2f".formatted(belief));
                    }
                    sb.append("] ");
                } else {
                    sb.append(element);
                }
                sb.append(" ");
            }
            if (sb.length() > 0)
                sb.deleteCharAt(sb.length() - 1);
            return sb.toString();
        }

        if (constantRenderer == null)
            return talkingRules.get(ruleName).generateExplanation(groundingName, contextAtom, rag, whyExplanation);
        return talkingRules.get(ruleName).generateExplanation(constantRenderer, groundingName, contextAtom, rag,
                whyExplanation);
    }

    protected void setExplanationBox(List<RankingEntry<Pair<String, Boolean>>> entries, VBox vbox) {
        vbox.getChildren().clear();
        Collections.sort(entries, Collections.reverseOrder());
        for (RankingEntry<Pair<String, Boolean>> entry : entries) {
            String explanation = entry.key.first;
            TextFlow textFlow = new TextFlow();
            AtomVerbalizationRenderer.fillTextFlow(explanation, textFlow, FactWindow.this);
            textFlow.prefWidthProperty().bind(whyNotScrollPane.widthProperty().subtract(40));

            ContextMenu menu = new ContextMenu();
            MenuItem copyItem = new MenuItem("Copy explanation to clipboard");
            menu.getItems().add(copyItem);
            final String finalExplanation = explanation;
            copyItem.setOnAction((ActionEvent e) -> {
                final Clipboard clipboard = Clipboard.getSystemClipboard();
                final ClipboardContent content = new ClipboardContent();
                content.putString(finalExplanation);
                clipboard.setContent(content);
            });
            textFlow.setOnContextMenuRequested(event -> menu.show(textFlow, event.getScreenX(), event.getScreenY()));

            if (!entry.key.second) {
                textFlow.setOpacity(0.5);
            }

            vbox.getChildren().add(textFlow);

            // Add a horizontal line as separator
            final Separator separator = new Separator(Orientation.HORIZONTAL);
            separator.prefWidthProperty().bind(textFlow.widthProperty());
            vbox.getChildren().add(separator);

            if (printPaneContentsToConsole) {
                System.out.println(explanation);
            }
        }
        // Remove the last separator
        int nChildren = vbox.getChildren().size();
        if (nChildren > 1) {
            vbox.getChildren().remove(nChildren - 1, nChildren);
        }
    }

    /**
     * A method for opening CogsetWindow on the form selection
     */
    public void onFormSelection(String atomString) {
        onFormSelection(atomString, true);
    }

    public String onFormSelection(String atomString, boolean addToHistory) {
        if (addToHistory) {
            previousAtoms.push(currentAtom.get());
            nextAtoms.clear();
        }
        String internalForm = getInternalForm(atomString);
        setCurrentAtom(getDisplayForm(atomString));
        Double score = getScore(currentAtom.get());
        currentScore.setValue(score);
        String pred = getPredicate(internalForm);
        TalkingPredicate tPred = getTalkingPredicate(pred);
        String verbalization = verbalizeAtom(tPred, internalForm, score);
        atomVerbalizationPane.getChildren().clear();
        AtomVerbalizationRenderer.fillTextFlow(verbalization, atomVerbalizationPane, this);
        // atomVerbalizationPane.setText("TODO: generate text based on TalkingPredicate
        // object.");
        setFacts(graph);
        return internalForm;
    }

    protected String verbalizeAtom(TalkingPredicate tPred, String internalForm, Double score) {
        if (internalForm.isEmpty())
            return "";
        if (constantRenderer != null) {
            List<String> args = getAtomElements(internalForm);
            args = args.subList(1, args.size());
            return tPred.verbalizeIdeaAsSentence(constantRenderer, score, args.toArray(new String[args.size()]));
        }
        List<String> args = getDisplayArguments(internalForm);
        return tPred.verbalizeIdeaAsSentence(score, args.toArray(new String[args.size()]));
    }

    public static class ExitHandler implements EventHandler<ActionEvent> {
        Stage stage;

        public ExitHandler(Stage stage) {
            this.stage = stage;
        }

        @Override
        public void handle(ActionEvent arg0) {
            stage.close();
        }
    }

    public class GraphTask extends Task<RuleAtomGraph> {
        @Override
        protected RuleAtomGraph call() {
            return getRag();
        }
    }

    protected RuleAtomGraph getRag() {
        return graph;
    }

    public class ScoreTask extends Task<Map<String, Double>> {
        @Override
        protected Map<String, Double> call() {
            return getScoreMap();
        }
    }

    protected Map<String, Double> getScoreMap() {
        return scoreMap;
    }

    public class ScoreService extends javafx.concurrent.Service<Map<String, Double>> {
        ScoreTask task;
        int boxInd;
        int ordinal;

        public Task<Map<String, Double>> createTask() {
            if (container != null) {
                container.getChildren().clear();
            }

            sortLabel.setPrefWidth(Double.MAX_VALUE);
            task = new ScoreTask();
            ordinal = 0;
            task.setOnSucceeded((WorkerStateEvent e) -> {
                scoreMap = task.getValue();
                displayedAtoms = FXCollections.observableArrayList();

                if (!currentAtom.get().isEmpty()) {
                    onFormSelection(currentAtom.get(), false);
                }
                String firstSelected = "";
                Set<String> sidebarAtoms = new TreeSet<>(ragAtoms);
                sidebarAtoms.removeAll(nonpersistedAtoms);
                if (!showOnlyRagAtoms)
                    sidebarAtoms.addAll(scoreMapAtoms);
                for (String atom : sidebarAtoms) {
                    if (!graph.renderAtomInGui(atom)) {
                        continue;
                    }
                    atom = getDisplayForm(atom);
                    if (atom.equals(currentAtom.get())) {
                        firstSelected = atom;
                    }
                    displayedAtoms.add(atom);
                }
                if (reloadingSidebar)
                    firstSelected = currentAtom.get();

                // listView.setPrefHeight(Double.MAX_VALUE);
                FilteredList<String> filteredData = new FilteredList<>(displayedAtoms, s -> true);
                displayedAtomsListView.setItems(filteredData);

                Text atomText = updateAtomText(new Text());
                atomTitle.getChildren().clear();
                atomTitle.getChildren().add(atomText);

                if (!firstSelected.trim().isEmpty()) {
                    if (displayedAtoms.contains(firstSelected)) {
                        displayedAtomsListView.getSelectionModel().select(firstSelected);
                        displayedAtomsListView.scrollTo(firstSelected);
                    }
                    String encoded = getInternalForm(firstSelected);
                    TalkingPredicate tPred = getTalkingPredicate(getPredicate(encoded));
                    Double score = getScore(firstSelected);
                    String verbalization = verbalizeAtom(tPred, encoded, score);
                    atomVerbalizationPane.getChildren().clear();
                    atomVerbalizationPane.setStyle("-fx-padding: 3 0 0 0;");
                    AtomVerbalizationRenderer.fillTextFlow(verbalization, atomVerbalizationPane, getWindow());
                }

                // listView.set .setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
                // FOR the morpheme tabs, also copy the methods
                displayedAtomsListView.setCellFactory(param -> new ListCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);

                        if (empty || item == null) {
                            setText(null);
                            setGraphic(null);
                            setStyle("-fx-background-color:white;");
                        } else {
                            if (displayedAtoms.contains(getDisplayForm(item))) {
                                String encoded = getInternalForm(item);
                                boolean deleted = isDeleted(encoded);
                                String backgroundColor;
                                if (!showOnlyRagAtoms && unusedAtoms.contains(encoded))
                                    backgroundColor = "#bebebe"; // grey
                                else
                                    backgroundColor = getBackgroundColorForScore(
                                            graph.atomToBaseColor(encoded, deleted), scoreMap, encoded);
                                setStyle("-fx-background-color:" + backgroundColor + ";");
                                getStyleClass().remove("white-font");
                                getStyleClass().add("black-font");
                                getStyleClass().add("lw-cell-no-border");
                                double belief = scoreMap.get(encoded);
                                if (deleted) {
                                    getStyleClass().add("table-row-deleted");
                                } else if (belief > 1.0 || belief < 0.0) {
                                    getStyleClass().remove("black-font");
                                    getStyleClass().add("white-font");
                                }
                                String displayForm = getDisplayForm(item);
                                if (priorApplied(encoded))
                                    displayForm = "(+) " + displayForm;
                                setText(displayForm);
                                Label fixedRep = new Label();
                                if (graph.isFixed(encoded)) {
                                    fixedRep.setText("⚫");
                                    fixedRep.setOpacity(0.4);
                                } else {
                                    fixedRep.setText("⚪");
                                }
                                fixedRep.getStyleClass().add("black-font");
                                setGraphic(fixedRep);
                            }

                        }
                    }

                    @Override
                    public void updateSelected(boolean selected) {
                        super.updateSelected(selected);
                        if (selected) {
                            getStyleClass().remove("lw-cell-no-border");
                            getStyleClass().add("lw-cell-border");
                        } else {
                            getStyleClass().remove("lw-cell-border");
                            getStyleClass().add("lw-cell-no-border");
                        }
                    }
                });

                displayedAtomsListView.getSelectionModel().selectedItemProperty()
                        .addListener((observable, oldValue, newValue) -> {
                            if (selectedItemStage != null) {
                                selectedItemStage.close();
                            }
                            if (newValue != null) {
                                onFormSelection(newValue);
                            }
                        });

                ImageView sortIcon = new ImageView(
                        new Image(getClass().getResource("/sort.png").toExternalForm()));
                ImageView sortIconUp = new ImageView(
                        new Image(getClass().getResource("/sort-up.png").toExternalForm()));
                ImageView sortIconDown = new ImageView(
                        new Image(getClass().getResource("/sort-down.png").toExternalForm()));

                Label arrow = new Label("");
                arrow.setGraphic(sortIcon);
                sortLabel.setText("");
                sortLabel.setGraphic(arrow);
                sortLabel.setContentDisplay(ContentDisplay.LEFT);

                // Initial "not sorted" comparator
                Comparator<String> nullComparator = Comparator.comparing(String::toString);
                Comparator<String> comparator = Comparator.comparing(String::toString, (f1, f2) -> getScore(f1).compareTo(getScore(f2)));

                // Mouse events for clicking the sorting symbol
                sortLabel.setOnMouseClicked((MouseEvent event) -> {
                    if (ordinal == 0) {
                        // change to ascending
                        displayedAtoms.sort(comparator);
                        ordinal = 1;
                        arrow.setGraphic(sortIconUp);
                    } else if (ordinal == 1) {
                        // change to descending
                        displayedAtoms.sort(comparator.reversed());
                        ordinal = 2;
                        arrow.setGraphic(sortIconDown);
                    } else {
                        // change to the initial state
                        displayedAtoms.sort(nullComparator);
                        arrow.setGraphic(sortIcon);
                        ordinal = 0;
                    }
                });

                if (presortSidebar) {
                    displayedAtoms.sort(comparator.reversed());
                    ordinal = 2;
                    arrow.setGraphic(sortIconDown);
                }

                // The (nested) list of options for specifying a predicate and its arguments.
                atomFilter.getChildren().clear();
                ColumnConstraints cc = new ColumnConstraints();
                cc.setPercentWidth(100);
                atomFilter.getColumnConstraints().add(cc);

                // Initial set-up.
                ComboBox<String> predNameBox = new ComboBox<>();
                predNameBox.setPrefWidth(Double.MAX_VALUE);
                SortedSet<String> sortedPredNames = displayedAtoms.stream().map(FactWindow.this::getPredicate).sorted().collect(Collectors.toCollection(TreeSet::new));
                predNameBox.getItems().add(""); // default value
                predNameBox.getItems().addAll(sortedPredNames);
                // cb.setEditable(true);
                atomFilter.add(predNameBox, 0, 0);

                predNameBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
                    // Filter by predicate.
                    if (newValue == null || newValue.isEmpty()) {
                        filteredData.setPredicate(s -> true);
                    } else {
                        filteredData.setPredicate(s -> getPredicate(s).equals(newValue));
                    }

                    // If a predicate name has been selected,
                    // infer the number of args from the filtered list of atoms.
                    int argNum = 0;
                    if (!filteredData.isEmpty() && predNameBox.getSelectionModel().getSelectedItem() != null
                            && !predNameBox.getSelectionModel().getSelectedItem().isEmpty()) {
                        argNum = getDisplayArguments(filteredData.get(0)).size();
                    }

                    // Add the predNameBox again.
                    atomFilter.getChildren().clear();
                    double percentages = 100.0 / (argNum + 1);
                    atomFilter.getColumnConstraints().clear();
                    for (int i = 0; i < argNum + 1; i++) {
                        ColumnConstraints cc1 = new ColumnConstraints();
                        cc1.setPercentWidth(percentages);
                        atomFilter.getColumnConstraints().add(cc1);
                    }
                    atomFilter.add(predNameBox, 0, 0);

                    // Add all applicable argument boxes.
                    boxInd = 0;
                    List<ComboBox<String>> predArgBoxes = new ArrayList<>();
                    for (int i = 0; i < argNum; i++) {
                        boxInd = i + 1;
                        ComboBox<String> predArgBox = new ComboBox<>();
                        predArgBoxes.add(predArgBox);
                        predArgBox.setPrefWidth(Double.MAX_VALUE);
                        atomFilter.add(predArgBox, boxInd, 0);

                        // Figure out the possible values and sort them.
                        boolean isNumeric = true;
                        SortedSet<String> argValues = filteredData.stream()
                                .map(a -> getDisplayArguments(a).get(boxInd - 1)).collect(Collectors.toCollection(TreeSet::new));
                        for (String s : argValues) {
                            if (!StringUtils.isNumber(s)) {
                                isNumeric = false;
                                break;
                            }
                        }
                        predArgBox.getItems().add(""); // default value
                        if (isNumeric) {
                            SortedSet<Integer> argValuesInts = argValues.stream().map(Integer::parseInt).collect(Collectors.toCollection(TreeSet::new));
                            for (Integer o : argValuesInts) {
                                predArgBox.getItems().add(o + "");
                            }
                        } else {
                            predArgBox.getItems().addAll(argValues);
                        }
                    }

                    // Filter by argument(s).
                    for (ComboBox<String> predArgBox : predArgBoxes) {
                        predArgBox.getSelectionModel().selectedItemProperty()
                                .addListener((observable1, oldValue1, newValue1) -> {
                                    Map<Integer, String> selectedArguments = new HashMap<>();
                                    for (ComboBox<String> combo2 : predArgBoxes) {
                                        if (combo2.getSelectionModel().getSelectedItem() != null
                                                && !combo2.getSelectionModel().getSelectedItem().isEmpty()) {
                                            selectedArguments.put(predArgBoxes.indexOf(combo2),
                                                    combo2.getSelectionModel().getSelectedItem());
                                        } else {
                                            selectedArguments.remove(predArgBoxes.indexOf(combo2));
                                        }
                                    }
                                    filteredData.setPredicate(item -> {
                                        // Check the predicate name first, then the arguments.
                                        if (!getPredicate(item).startsWith(
                                                predNameBox.getSelectionModel().getSelectedItem())) {
                                            return false;
                                        }
                                        for (Map.Entry<Integer, String> selected : selectedArguments
                                                .entrySet()) {
                                            if (!getDisplayArguments(item).get(selected.getKey())
                                                    .equals(selected.getValue())) {
                                                return false;
                                            }
                                        }
                                        return true;
                                    });

                                });
                    }
                });

                GraphService factsService = new GraphService();
                factsService.start();
            });
            return task;
        }
    }

    // Override me!
    protected boolean isDeleted(String encodedAtom) {
        return false;
    }

    // Override me!
    protected boolean priorApplied(String encodedAtom) {
        return false;
    }

    public class GraphService extends javafx.concurrent.Service<RuleAtomGraph> {
        GraphTask task;

        public Task<RuleAtomGraph> createTask() {
            task = new GraphTask();
            task.setOnSucceeded((WorkerStateEvent e) -> {
                graph = task.getValue();
                setFacts(graph);
            });
            return task;
        }
    }

    public static class Pair<T, U> {
        public T first;
        public U second;

        public Pair(T first, U second) {
            this.first = first;
            this.second = second;
        }

        public String toString() {
            return "(" + first + "," + second + ")";
        }

        public int hashCode() {
            return toString().hashCode();
        }

        public boolean equals(Object o) {
            if (o instanceof Pair<?, ?> otherPair) {
                return (otherPair.first.equals(first) && otherPair.second.equals(second));
            }
            return false;
        }
    }

}
