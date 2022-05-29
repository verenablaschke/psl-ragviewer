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
        if (isConstraint) {
            if (isViolatedConstraint()) {
                return "∞ [" + String.format(FactWindow.SCORE_FORMAT, dissatisfaction) + "]";
            }
            return String.format(FactWindow.SCORE_FORMAT, 0.0);
        }
        return String.format(FactWindow.SCORE_FORMAT, dissatisfaction);
    }

    public String getDisplayableCounterfactualDissatisfaction() {
        if (isConstraint) {
            if (isViolatedCounterfactualConstraint()) {
                return "∞ [" + String.format(FactWindow.SCORE_FORMAT, counterfactualDissatisfaction) + "]";
            }
            return String.format(FactWindow.SCORE_FORMAT, 0.0);
        }
        return String.format(FactWindow.SCORE_FORMAT, counterfactualDissatisfaction);
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
        if (isConstraint) {
            if (active) {
                return -5;
            }
            return -4;
        }
        if (active) {
            return -3;
        }
        return 0;
    }

    @Override
    public int compareTo(Explanation other) {
        if (other == null) {
            return -1;
        }
        return Integer.compare(comparisonScore(), other.comparisonScore());
    }


}
