package edu.alibaba.mpc4j.dp.stream.heavyhitter.utils;

import java.util.ArrayList;
import java.util.Map;

/**
 * The server context with Local Differential Privacy used in the HeavyGuardian-based solutions.
 *
 * @author Weiran Liu
 * @date 2022/11/23
 */
public class HgLdpHhServerContext implements LdpHhServerContext {
    /**
     * the budgets
     */
    private final ArrayList<Map<String, Double>> budgets;

    public HgLdpHhServerContext(ArrayList<Map<String, Double>> budgets) {
        this.budgets = budgets;
    }

    public Map<String, Double> getBudget(int budgetIndex) {
        return budgets.get(budgetIndex);
    }
}
