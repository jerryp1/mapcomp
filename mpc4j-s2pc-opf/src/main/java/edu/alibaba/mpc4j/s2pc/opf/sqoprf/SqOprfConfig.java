package edu.alibaba.mpc4j.s2pc.opf.sqoprf;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * @author Qixian Zhou
 * @date 2023/4/11
 */
public interface SqOprfConfig extends MultiPartyPtoConfig  {


    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    SqOprfFactory.SqOprfType getPtoType();

}
