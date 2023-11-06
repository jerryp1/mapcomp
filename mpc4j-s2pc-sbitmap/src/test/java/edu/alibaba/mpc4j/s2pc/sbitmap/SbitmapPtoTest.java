package edu.alibaba.mpc4j.s2pc.sbitmap;

import edu.alibaba.mpc4j.common.data.DatasetManager;
import edu.alibaba.mpc4j.common.data.classification.Weather;
import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.s2pc.sbitmap.main.SbitmapConfig;
import edu.alibaba.mpc4j.s2pc.sbitmap.utils.SbitmapMainUtils;
import edu.alibaba.mpc4j.s2pc.sbitmap.pto.SbitmapPtoParty;
import edu.alibaba.mpc4j.s2pc.sbitmap.main.SbitmapTaskType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import smile.data.DataFrame;

import java.util.ArrayList;
import java.util.Collection;

import static edu.alibaba.mpc4j.common.tool.CommonConstants.BLOCK_BIT_LENGTH;

/**
 * Sbitmap protocol test.
 *
 * @author Li Peng
 * @date 2023/8/15
 */
@RunWith(Parameterized.class)
public class SbitmapPtoTest extends AbstractTwoPartyPtoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SbitmapPtoTest.class);

    private static Zl DEFAULT_ZL = ZlFactory.createInstance(EnvType.STANDARD, BLOCK_BIT_LENGTH);

    static {
        DatasetManager.setPathPrefix("../data/");
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // 两分类任务，特征均为枚举值
        configurations.add(new Object[]{"Weather", Weather.data, SbitmapTaskType.SET_OPERATIONS, new int[]{0,1,2,3}, new int[]{4,5,6,7,8}});
//        // 三分类任务，特征均为float
//        configurations.add(new Object[]{"Iris",Iris.data});
//        // 多分类任务，特征均为double
//        configurations.add(new Object[]{"PenDigits", PenDigits.data});
//        // 二分类任务，较大规模数据
//        configurations.add(
//            new Object[]{"BreastCancer", BreastCancer.data}
//        );


        return configurations;
    }

    /**
     * 数据集名称
     */
    private final String name;
    /**
     * 数据
     */
    private DataFrame senderDataFrame;
    /**
     * 数据
     */
    private  DataFrame receiverDataFrame;
    /**
     * 主机
     */
    private final SbitmapPtoParty sender;
    /**
     * 从机
     */
    private final SbitmapPtoParty receiver;

    public SbitmapPtoTest(String name, DataFrame dataframe, SbitmapTaskType taskType, int[] senderColumns, int[] receiverColumns) {
        super(name);
        this.name = name;
        // set parties
        sender = SbitmapMainUtils.createParty(taskType, secondRpc, firstRpc.ownParty(), new SbitmapConfig.Builder(dataframe.schema(), DEFAULT_ZL).build());
        receiver = SbitmapMainUtils.createParty(taskType, firstRpc, secondRpc.ownParty(), new SbitmapConfig.Builder(dataframe.schema(), DEFAULT_ZL).build());
        // set data
        setData(dataframe, senderColumns, receiverColumns);
   }

    private void setData(DataFrame dataFrame, int[] senderColumns, int[] receiverColumns) {
        senderDataFrame = SbitmapMainUtils.setDataset(
            dataFrame, senderColumns, SbitmapMainUtils.selectRows(dataFrame.nrows(), sender.getRpc().ownParty().getPartyId()));
        receiverDataFrame = SbitmapMainUtils.setDataset(
            dataFrame, receiverColumns, SbitmapMainUtils.selectRows(dataFrame.nrows(), receiver.getRpc().ownParty().getPartyId()));
    }

    @Before
    @Override
    public void connect() {
        super.connect();
//        try {
//            sender.init();
//            receiver.init();
//        } catch (MpcAbortException e) {
//            e.printStackTrace();
//        }
    }

    @After
    @Override
    public void disconnect() {
        sender.stop();
        receiver.stop();
        super.disconnect();
    }

//    @Test
//    public void testLargeEpsilonLdpTraining() {
//        testLdpTraining(OpBoostTestUtils.LARGE_EPSILON);
//    }
//
//    @Test
//    public void testDefaultEpsilonLdpTraining() {
//        testLdpTraining(OpBoostTestUtils.DEFAULT_EPSILON);
//    }
//
//    @Test
//    public void testSmallEpsilonLdpTraining() {
//        testLdpTraining(OpBoostTestUtils.SMALL_EPSILON);
//    }

    @Test
    public void testFullSecurePto() {

        // 执行协议
        SbitmapPartyThread senderThread = new SbitmapPartyThread(sender, senderDataFrame,
            new SbitmapConfig.Builder(senderDataFrame.schema(), DEFAULT_ZL).build(), receiverDataFrame.size());
        SbitmapPartyThread receiverThread = new SbitmapPartyThread(receiver, receiverDataFrame,
            new SbitmapConfig.Builder(receiverDataFrame.schema(), DEFAULT_ZL).build(), senderDataFrame.size());

        // 等待线程停止
        try {
            senderThread.start();
            receiverThread.start();
            senderThread.join();
            receiverThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
