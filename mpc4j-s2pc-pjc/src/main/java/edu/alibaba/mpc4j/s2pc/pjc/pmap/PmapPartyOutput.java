package edu.alibaba.mpc4j.s2pc.pjc.pmap;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

import java.util.*;
import java.util.stream.Collectors;

/**
 * PMAP服务端/客户端输出。
 *
 * @author Feng Han
 * @date 2023/10/23
 */
public class PmapPartyOutput<T> {
    public enum MapType{
        MAP,
        PSI,
        PID,
    }
    /**
     * valid element list
     */
    private final List<T> elementList;
    /**
     * 位置映射map
     */
    private final Map<Integer, T> indexMap;
    /**
     * 表示对应位置元素是否在交集中的flag
     */
    private final SquareZ2Vector equalFlag;

    /**
     * 构造PID服务端输出。
     *
     * @param elementList 有效的数据列表，即map input的keys
     * @param indexMap    从位置到
     */
    public PmapPartyOutput(MapType mapType, List<T> elementList, Map<Integer, T> indexMap, SquareZ2Vector equalFlag) {
        MathPreconditions.checkPositive("elementList.size()", elementList.size());
        this.elementList = elementList;
        switch (mapType){
            case PID:
            case MAP:
                MathPreconditions.checkGreaterOrEqual("indexMap.size()", indexMap.size(), elementList.size());
                break;
            case PSI:
                MathPreconditions.checkGreaterOrEqual("indexMap.size()", elementList.size(), indexMap.size());
        }
        // 验证index映射中的object都在elementList中
        List<T> mapList = indexMap.values().stream().filter(Objects::nonNull).collect(Collectors.toList());
        Set<T> mapSet = new HashSet<>(mapList);
        assert elementList.size() == mapList.size();
        for (T value : elementList) {
            assert mapSet.contains(value);
        }
        this.indexMap = indexMap;
        if(equalFlag != null){
            MathPreconditions.checkEqual("indexMap.size()", "equalSign.bitNum()", indexMap.size(), equalFlag.bitNum());
        }
        this.equalFlag = equalFlag;
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
     * 返回equalFlag
     *
     * @return equalFlag
     */
    public SquareZ2Vector getEqualFlag() {
        return equalFlag;
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
