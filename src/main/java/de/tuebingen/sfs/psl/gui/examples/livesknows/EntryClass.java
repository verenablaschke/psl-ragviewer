/*
 * Copyright 2018–2022 University of Tübingen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tuebingen.sfs.psl.gui.examples.livesknows;

import de.tuebingen.sfs.psl.engine.DatabaseManager;
import de.tuebingen.sfs.psl.engine.InferenceResult;
import de.tuebingen.sfs.psl.engine.ProblemManager;
import de.tuebingen.sfs.psl.engine.RuleAtomGraph;
import de.tuebingen.sfs.psl.examples.livesknows.SampleConstantRenderer;
import de.tuebingen.sfs.psl.examples.livesknows.SampleIdeaGenerator;
import de.tuebingen.sfs.psl.examples.livesknows.SamplePslProblem;
import de.tuebingen.sfs.psl.gui.StandaloneFactWindowLauncher;
import de.tuebingen.sfs.psl.talk.ConstantRenderer;
import de.tuebingen.sfs.psl.util.data.Pair;
import de.tuebingen.sfs.psl.util.log.InferenceLogger;

import java.util.Collections;

public class EntryClass {

    public static void main(String[] args) {
        Pair<SamplePslProblem, InferenceResult> problemAndResult = de.tuebingen.sfs.psl.examples.livesknows.EntryClass.runInference();
        SamplePslProblem problem = problemAndResult.first;
        InferenceResult result = problemAndResult.second;

        // Inspect the results in the GUI:
        boolean printExplanations = true;
        boolean sortSidebar = true;
        ConstantRenderer renderer = new SampleConstantRenderer();
        StandaloneFactWindowLauncher.launchWithData(renderer, problem, result, sortSidebar, printExplanations);
    }
}
