package de.tuebingen.sfs.psl.gui;

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
        return isConstraint && dissatisfaction > 0.00001;
    }

    boolean isViolatedCounterfactualConstraint() {
        return isConstraint && counterfactualDissatisfaction > 0.00001;
    }

    public String getDisplayableDissatisfaction() {
        if (isConstraint) {
            if (isViolatedConstraint()) {
//                return "∞";
                return String.format("∞ [%.4f]", dissatisfaction);
            }
            return "0.0000";
        }
        return String.format("%.4f", dissatisfaction);
    }

    public String getDisplayableCounterfactualDissatisfaction() {
        if (isConstraint) {
            if (isViolatedCounterfactualConstraint()) {
//                return "∞";
                return String.format("∞ [%.4f]", counterfactualDissatisfaction);
            }
            return "0.0000";
        }
        return String.format("%.4f", counterfactualDissatisfaction);
    }

    public boolean isActive() {
        return active;
    }

    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        if (isConstraint) {
            sb.append("CONSTRAINT");
        } else {
            sb.append("RULE");
        }
        sb.append(" / ");
        if (dissatisfaction > 0.00001) {
            sb.append(" ");
            sb.append("DIS");
        }
        sb.append("SATISFIED / COUNTERFACTUAL ");
        if (counterfactualDissatisfaction > 0.00001) {
            sb.append("DIS");
        }
        sb.append("SATISFIED ");
        return sb.toString();
    }

    @Override
    public int compareTo(Explanation other) {
        if (other == null) {
            return -1;
        }
        // Ranking:
        // 1. Fixed explanations like
        // "This atom's belief value cannot be higher because it already has the highest possible score (1.0)"
        // or "The value of this atom was fixed before the inference."
        if (isFixed && !other.isFixed) {
            return -1;
        }
        // 2. Violated constraints
        if (isViolatedConstraint()) {
            if (!other.isConstraint) {
                return -1;
            }
            if (other.isViolatedConstraint()) {
                return 0;
            }
            return -1;
        }
        // 3. Dissatisfied rules, ordered by distance to satisfaction
        if (!isConstraint && dissatisfaction > 0) {
            if (!other.isConstraint) {
                return Double.compare(dissatisfaction, other.dissatisfaction);
            }
        }
        // 4. Constraints that would be violated in a counterfactual scenario
        if (isViolatedCounterfactualConstraint()) {
            if (!other.isConstraint) {
                return -1;
            }
            if (other.isViolatedCounterfactualConstraint()) {
                return 0;
            }
            return -1;
        }
        // 5. Rules that would be dissatisfied in a counterfactual scenario,
        // ordered by counterfactual distance to satisfaction
        if (!isConstraint && counterfactualDissatisfaction > 0) {
            if (!other.isConstraint) {
                return Double.compare(counterfactualDissatisfaction, other.counterfactualDissatisfaction);
            }
        }
        // 6. All remaining constraints
        if (isConstraint) {
            return other.isConstraint ? 0 : -1;
        }
        // 7. All remaining rules
        return other.isConstraint ? 1 : 0;
    }


}
