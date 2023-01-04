package edu.alibaba.mpc4j.dp.stream.heavyhitter.fo.de;

import edu.alibaba.mpc4j.dp.stream.heavyhitter.HeavyHitterStructure;

import java.util.Map;

/**
 * The Heavy Hitter structure with Local Differential Privacy used in the direct encoding solution.
 *
 * @author Weiran Liu
 * @date 2022/11/23
 */
public class DeHeavyHitterStructure implements HeavyHitterStructure {
    /**
     * The budget
     */
    private final Map<String, Double> budget;

    public DeHeavyHitterStructure(Map<String, Double> budget) {
        this.budget = budget;
    }

    public Map<String, Double> getBudget() {
        return budget;
    }
}
