package edu.alibaba.mpc4j.s2pc.pjc.pmap;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

import java.util.*;
import java.util.stream.Collectors;

/**
 * PMAP output
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
    private final MapType mapType;
    /**
     * valid element list
     */
    private final List<T> elementList;
    /**
     * map between index and element
     */
    private final Map<Integer, T> indexMap;
    /**
     * the flag indicating whether the elements are in intersection
     */
    private final SquareZ2Vector equalFlag;

    /**
     * constructing output of pmao
     *
     * @param elementList the list of valid ids
     * @param indexMap    map between index and element
     */
    public PmapPartyOutput(MapType mapType, List<T> elementList, Map<Integer, T> indexMap, SquareZ2Vector equalFlag) {
        MathPreconditions.checkPositive("elementList.size()", elementList.size());
        this.elementList = elementList;
        this.mapType = mapType;
        switch (mapType){
            case PID:
            case MAP:
                MathPreconditions.checkGreaterOrEqual("indexMap.size()", indexMap.size(), elementList.size());
                break;
            case PSI:
                MathPreconditions.checkGreaterOrEqual("indexMap.size()", elementList.size(), indexMap.size());
        }
        // Verify that all elements in indexMap are from elementList
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
     * return the type of map
     */
    public MapType getMapType() {
        return mapType;
    }

    /**
     * return the indexMap
     *
     */
    public Map<Integer, T> getIndexMap() {
        return indexMap;
    }

    /**
     * return equalFlag
     */
    public SquareZ2Vector getEqualFlag() {
        return equalFlag;
    }

    /**
     * return the element of {index}
     *
     * @param index index
     * @return      the corresponding element
     */
    public T getIndex(int index) {
        return indexMap.get(index);
    }
}
