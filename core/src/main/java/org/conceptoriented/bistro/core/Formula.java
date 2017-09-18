package org.conceptoriented.bistro.core;

import org.conceptoriented.bistro.core.Expression;

public interface Formula extends Expression {
    public static String Exp4j = "org.conceptoriented.bistro.core.formula.FormulaExp4J";
    public static String Evalex = "org.conceptoriented.bistro.core.formula.FormulaEvalex";
    public static String Mathparser = "org.conceptoriented.bistro.core.formula.FormulaMathparser";
    public static String JavaScript = "org.conceptoriented.bistro.core.formula.FormulaJavaScript";

}