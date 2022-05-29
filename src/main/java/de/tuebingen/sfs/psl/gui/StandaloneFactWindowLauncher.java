package de.tuebingen.sfs.psl.gui;

import java.util.Map;

import de.tuebingen.sfs.psl.engine.InferenceResult;
import de.tuebingen.sfs.psl.engine.PslProblem;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.talk.ConstantRenderer;
import de.tuebingen.sfs.psl.talk.TalkingPredicate;
import de.tuebingen.sfs.psl.talk.TalkingRuleOrConstraint;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class StandaloneFactWindowLauncher extends Application {
    protected static ConstantRenderer renderer = null;
    protected static PslProblem pslProblem = null;
    protected static RuleAtomGraph rag = null;
    protected static Map<String, Double> result;
    protected static FactWindow fWindow = null;
    protected static Stage selectedItemStage = null;
    protected static boolean sortSidebar = false;
    protected static boolean printExplanationPanesToConsole = false;
    protected static Map<String, TalkingPredicate> talkingPreds = null;
    protected static Map<String, TalkingRuleOrConstraint> talkingRules = null;

    public static void launchWithData(PslProblem pslProb, RuleAtomGraph graph, Map<String, Double> valueMap) {
        pslProblem = pslProb;
        rag = graph;
        result = valueMap;
        new Thread() {
            @Override
            public void run() {
                javafx.application.Application.launch(StandaloneFactWindowLauncher.class);
            }
        }.start();
    }

    public static void launchWithData(InferenceResult inferenceResult, Map<String, TalkingPredicate> talkingPredsMap,
                                      Map<String, TalkingRuleOrConstraint> talkingRulesMap, boolean sortSidebarDesc,
                                      boolean printPaneContentsToConsole) {
        talkingPreds = talkingPredsMap;
        talkingRules = talkingRulesMap;
        rag = inferenceResult.getRag();
        result = inferenceResult.getInferenceValues();
        sortSidebar = sortSidebarDesc;
        printExplanationPanesToConsole = printPaneContentsToConsole;
        new Thread() {
            @Override
            public void run() {
                javafx.application.Application.launch(StandaloneFactWindowLauncher.class);
            }
        }.start();
    }

    public static void launchWithData(InferenceResult inferenceResult, Map<String, TalkingPredicate> talkingPredsMap,
                                      Map<String, TalkingRuleOrConstraint> talkingRulesMap) {
        launchWithData(inferenceResult, talkingPredsMap, talkingRulesMap, true, false);
    }

    public static void launchWithData(ConstantRenderer constantRenderer, PslProblem pslProb,
                                      InferenceResult inferenceResult, boolean sortSidebarDesc,
                                      boolean printPaneContentsToConsole) {
        renderer = constantRenderer;
        pslProblem = pslProb;
        if (inferenceResult != null) {
            rag = inferenceResult.getRag();
            result = inferenceResult.getInferenceValues();
        }
        sortSidebar = sortSidebarDesc;
        printExplanationPanesToConsole = printPaneContentsToConsole;
        new Thread() {
            @Override
            public void run() {
                javafx.application.Application.launch(StandaloneFactWindowLauncher.class);
            }
        }.start();
    }

    public static void launchWithData(PslProblem pslProb, InferenceResult inferenceResult) {
        launchWithData(null, pslProb, inferenceResult);
    }

    public static void launchWithData(ConstantRenderer constantRenderer, PslProblem pslProb,
                                      InferenceResult inferenceResult) {
        launchWithData(constantRenderer, pslProb, inferenceResult, true, false);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        if (selectedItemStage == null) {
            selectedItemStage = new Stage();
        }
        if (fWindow == null) {
            fWindow = createFactWindow();
        }
        FXMLLoader fWindowloader = new FXMLLoader(getClass().getResource("/fx/FactWindow.fxml"));
        fWindowloader.setController(fWindow);
        fWindow.setStage(selectedItemStage);
        fWindowloader.load();
        Parent fWindowParent = fWindowloader.getRoot();
        Scene fWindowScene = new Scene(fWindowParent);
        fWindowScene.getStylesheets()
                .add(getClass().getResource("/fx/facts.css").toExternalForm());
        selectedItemStage.setScene(fWindowScene);
        selectedItemStage.setTitle("Inference results");
        selectedItemStage.show();
    }

    public FactWindow createFactWindow() {
        return new FactWindow(renderer, pslProblem, null, rag, result, talkingPreds, talkingRules,
                sortSidebar, printExplanationPanesToConsole, null);
    }

}
