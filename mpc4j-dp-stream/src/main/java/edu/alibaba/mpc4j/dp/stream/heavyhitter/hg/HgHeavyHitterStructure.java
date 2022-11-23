package edu.alibaba.mpc4j.dp.stream.heavyhitter.hg;

import edu.alibaba.mpc4j.dp.stream.heavyhitter.HeavyHitterStructure;

import java.util.ArrayList;
import java.util.Map;

/**
 * The Heavy Hitter structure with Local Differential Privacy used in the HeavyGuardian-based solutions.
 *
 * @author Weiran Liu
 * @date 2022/11/23
 */
public class HgHeavyHitterStructure implements HeavyHitterStructure {
    /**
     * the budgets
     */
    private final ArrayList<Map<String, Double>> budgets;

    public HgHeavyHitterStructure(ArrayList<Map<String, Double>> budgets) {
        this.budgets = budgets;
    }

    public Map<String, Double> getBudget(int budgetIndex) {
        return budgets.get(budgetIndex);
    }
}
