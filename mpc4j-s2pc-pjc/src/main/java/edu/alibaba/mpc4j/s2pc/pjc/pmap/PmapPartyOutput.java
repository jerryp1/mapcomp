package edu.alibaba.mpc4j.s2pc.pjc.pmap;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import java.util.*;
import java.util.stream.Collectors;

/**
 * PMAP服务端/客户端输出。
 *
 * @author Feng Han
 * @date 2023/10/23
 */
public class PmapPartyOutput<T> {
    /**
     * valid element list
     */
    private final List<T> elementList;
    /**
     * 位置映射map
     */
    private final Map<Integer, T> indexMap;

    /**
     * 构造PID服务端输出。
     *
     * @param elementList 有效的数据列表，即map input的keys
     * @param indexMap    从位置到
     */
    public PmapPartyOutput(List<T> elementList, Map<Integer, T> indexMap) {
        MathPreconditions.checkPositive("elementList.size()", elementList.size());
        this.elementList = elementList;
        MathPreconditions.checkGreaterOrEqual("indexMap.size()", indexMap.size(), elementList.size());
        // 验证index映射中的object都在elementList中
        Set<T> mapSet = indexMap.values().stream().filter(Objects::nonNull).collect(Collectors.toSet());
        assert elementList.size() == mapSet.size();
        for (T value : elementList) {
            assert mapSet.contains(value);
        }
        this.indexMap = indexMap;
    }

    /**
     * 返回元素列表
     *
     * @return 元素列表
     */
    public List<T> getElementList() {
        return elementList;
    }

    /**
     * 返回位置映射map
     *
     * @return 位置映射map
     */
    public Map<Integer, T> getIndexMap() {
        return indexMap;
    }

    /**
     * 返回指定位置对应的元素
     *
     * @param index 需要索引的index
     * @return 对应的元素
     */
    public T getIndex(int index) {
        return indexMap.get(index);
    }
}
