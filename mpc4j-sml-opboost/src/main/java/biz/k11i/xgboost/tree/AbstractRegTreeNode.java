package biz.k11i.xgboost.tree;

import ai.h2o.algos.tree.INode;
import biz.k11i.xgboost.fvec.FVec;

import java.io.Serializable;

/**
 * Abstract Regression Tree Node.
 *
 * @author Honza Sterba, Weiran Liu
 * @date 2021/10/08
 */
public abstract class AbstractRegTreeNode implements INode<FVec>, Serializable {
    private static final long serialVersionUID = 7314487558338026230L;

    /**
     * Gets index of node's parent.
     *
     * @return Index of node's parent.
     */
    public abstract int getParentIndex();

    /**
     * Gets index of node's left child node.
     *
     * @return Index of node's left child node.
     */
    public abstract int getLeftChildIndex();

    /**
     * Gets index of node's right child node.
     *
     * @return Index of node's right child node.
     */
    public abstract int getRightChildIndex();

    /**
     * Gets split condition on the node.
     *
     * @return Split condition on the node, if the node is a split node. Leaf nodes have this value set to NaN.
     */
    public abstract float getSplitCondition();

    /**
     * Replaces split condition on the node.
     */
    public abstract void replaceSplitCondition(float splitCondition);

    /**
     * Gets predicted value on the leaf node.
     *
     * @return Predicted value on the leaf node, if the node is leaf. Otherwise NaN.
     */
    public abstract float getLeafValue();

    /**
     * Gets if default direction for unrecognized values is the LEFT child.
     *
     * @return True if default direction for unrecognized values is the LEFT child, otherwise false.
     */
    public abstract boolean defaultLeft();

    /**
     * Gets index of domain category used to split on the node.
     *
     * @return Index of domain category used to split on the node.
     */
    public abstract int getSplitIndex();
}
