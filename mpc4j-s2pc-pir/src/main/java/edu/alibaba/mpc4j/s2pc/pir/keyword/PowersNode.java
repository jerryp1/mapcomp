package edu.alibaba.mpc4j.s2pc.pir.keyword;

/**
 * 幂次方节点。
 * 存储计算高幂次方的路径。
 *
 * @author Liqiang Peng
 * @date 2022/5/25
 */
public class PowersNode {
    /**
     * 幂次方
     */
    public int power;
    /**
     * 深度
     */
    public int depth;
    /**
     * 左父幂次方
     */
    public int leftParentPower = 0;
    /**
     * 右父幂次方
     */
    public int rightParentPower = 0;

    /**
     * 构造函数。
     *
     * @param power 幂次方。
     * @param depth 深度。
     */
    public PowersNode(int power, int depth) {
        this.power = power;
        this.depth = depth;
    }

    /**
     * 构造函数。
     *
     * @param power            幂次方。
     * @param depth            深度。
     * @param leftParentPower  左父幂次方。
     * @param rightParentPower 右父幂次方。
     */
    public PowersNode(int power, int depth, int leftParentPower, int rightParentPower) {
        this.power = power;
        this.depth = depth;
        this.leftParentPower = leftParentPower;
        this.rightParentPower = rightParentPower;
    }
}
