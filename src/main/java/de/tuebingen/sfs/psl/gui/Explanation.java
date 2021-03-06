/*
 *     Copyright 2018–2022 University of Tübingen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tuebingen.sfs.psl.gui;

import de.tuebingen.sfs.psl.engine.RuleAtomGraph;

public class Explanation implements Comparable<Explanation> {

    private String text;
    private boolean isFixed = false;
    private boolean isConstraint = false;
    private double dissatisfaction = -1.0;
    private double counterfactualDissatisfaction = -1.0;
    private boolean active = false;

    public static Explanation createFixedExplanation(String text) {
        Explanation expl = new Explanation();
        expl.text = text;
        expl.isFixed = true;
        expl.active = true;
        return expl;
    }

    public static Explanation createRuleExplanation(String text, double dissatisfaction, boolean active) {
        Explanation expl = new Explanation();
        expl.text = text;
        expl.dissatisfaction = dissatisfaction;
        expl.active = active;
        return expl;
    }

    public static Explanation createConstraintExplanation(String text, double dissatisfaction, boolean active) {
        Explanation expl = createRuleExplanation(text, dissatisfaction, active);
        expl.isConstraint = true;
        return expl;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isFixed() {
        return isFixed;
    }

    public boolean isConstraint() {
        return isConstraint;
    }

    public void setCounterfactualDissatisfaction(double counterfactualDissatisfaction) {
        this.counterfactualDissatisfaction = counterfactualDissatisfaction;
    }

    boolean isViolatedConstraint() {
        return isConstraint && isDissatisfied();
    }

    boolean isViolatedCounterfactualConstraint() {
        return isConstraint && counterfactualIsDissatisfied();
    }

    boolean isDissatisfied() {
        return dissatisfaction > RuleAtomGraph.DISSATISFACTION_PRECISION;
    }

    boolean counterfactualIsDissatisfied() {
        return counterfactualDissatisfaction > RuleAtomGraph.DISSATISFACTION_PRECISION;
    }

    public String getDisplayableDissatisfaction() {
        if (isViolatedConstraint()) {
            // "Lightning mood" emoji
            return "\uD83D\uDDF2(∞ [" + FactWindow.formatValue(dissatisfaction) + "])";
        }
        if (dissatisfaction > RuleAtomGraph.DISSATISFACTION_PRECISION) {
            return "\uD83D\uDDF2(" + FactWindow.formatValue(dissatisfaction) + ")";
        }
        return FactWindow.formatValue(0.0);
    }

    public String getDisplayableCounterfactualDissatisfaction() {
        if (isConstraint) {
            if (isViolatedCounterfactualConstraint()) {
                return "∞ [" + FactWindow.formatValue(counterfactualDissatisfaction) + "]";
            }
            return FactWindow.formatValue(0.0);
        }
        return FactWindow.formatValue(counterfactualDissatisfaction);
    }

    public boolean isActive() {
        return active;
    }

    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        if (isFixed) {
            sb.append("FIXED ");
        }
        if (isConstraint) {
            sb.append("CONSTRAINT");
        } else {
            sb.append("RULE");
        }
        sb.append(" / ");
        if (isDissatisfied()) {
            sb.append("DIS");
        }
        sb.append("SATISFIED / COUNTERFACTUAL ");
        if (counterfactualIsDissatisfied()) {
            sb.append("DIS");
        }
        sb.append("SATISFIED ");
        return sb.toString();
    }

    private int comparisonScore() {
        if (isFixed) {
            return -10;
        }
        if (isViolatedConstraint()) {
            return -9;
        }
        if (!isConstraint && isDissatisfied()) {
            return -8;
        }
        if (isViolatedCounterfactualConstraint()) {
            return -7;
        }
        if (!isConstraint && counterfactualIsDissatisfied()) {
            return -6;
        }
        if (active) {
            if (isConstraint) {
                return -5;
            }
            return -4;
        }
        if (isConstraint) {
            return -3;
        }
        return 0;
    }

    @Override
    public int compareTo(Explanation other) {
        if (other == null) {
            return -1;
        }
        int compScore = comparisonScore();
        int surfaceComp = Integer.compare(compScore, other.comparisonScore());
        if (surfaceComp != 0) {
            return surfaceComp;
        }

        if (compScore == -10) {
            // Both are fixed.
            return 0;
        }
        if (compScore == -9 || compScore == -8) {
            // Both are violated constraints or dissatisfied weighted rules.
            if (Math.abs(dissatisfaction - other.dissatisfaction) < RuleAtomGraph.DISSATISFACTION_PRECISION) {
                return Double.compare(-counterfactualDissatisfaction, -other.dissatisfaction);
            }
            return Double.compare(-dissatisfaction, -other.dissatisfaction);
        }
        if (compScore == -7 || compScore == -6) {
            // Both are satisfied constraints or rules with dissatisfied counterfactuals.
            return Double.compare(-counterfactualDissatisfaction, -other.dissatisfaction);
        }

        // Nothing left to compare.
        return 0;
    }


}
