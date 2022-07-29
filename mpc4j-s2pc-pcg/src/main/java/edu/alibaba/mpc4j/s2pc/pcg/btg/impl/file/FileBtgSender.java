package edu.alibaba.mpc4j.s2pc.pcg.btg.impl.file;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prg.Prg;
import edu.alibaba.mpc4j.common.tool.crypto.prg.PrgFactory;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pcg.btg.AbstractBtgParty;
import edu.alibaba.mpc4j.s2pc.pcg.btg.BooleanTriple;
import org.bouncycastle.util.encoders.Hex;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 文件BTG发送方。
 *
 * @author Weiran Liu
 * @date 2022/5/22
 */
public class FileBtgSender extends AbstractBtgParty {
    /**
     * 文件名分隔符
     */
    private static final String FILE_NAME_SEPARATOR = "_";
    /**
     * 文件路径
     */
    private final String filePath;
    /**
     * 布尔三元组
     */
    private BooleanTriple booleanTriple;

    public FileBtgSender(Rpc senderRpc, Party receiverParty, FileBtgConfig config) {
        super(FileBtgPtoDesc.getInstance(), senderRpc, receiverParty, config);
        filePath = config.getFilePath();
    }

    @Override
    public void init(int maxRoundNum, int updateNum) throws MpcAbortException {
        setInitInput(maxRoundNum, updateNum);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        String senderBtgFileName = getFileName();
        File senderBtgFile = new File(filePath + File.separator + senderBtgFileName);
        if (!senderBtgFile.exists()) {
            // 如果没有文件，则生成
            int maxUpdateByteNum = CommonUtils.getByteLength(updateNum);
            Kdf kdf = KdfFactory.createInstance(envType);
            Prg prg = PrgFactory.createInstance(envType, maxUpdateByteNum);
            // 生成三元组
            byte[] a0Key = kdf.deriveKey(new byte[CommonConstants.BLOCK_BYTE_LENGTH]);
            byte[] a1Key = kdf.deriveKey(a0Key);
            byte[] b0Key = kdf.deriveKey(a1Key);
            byte[] b1Key = kdf.deriveKey(b0Key);
            byte[] c0Key = kdf.deriveKey(b1Key);
            // 生成a0、b0、c0
            byte[] a0 = prg.extendToBytes(a0Key);
            BytesUtils.reduceByteArray(a0, maxRoundNum);
            byte[] b0 = prg.extendToBytes(b0Key);
            BytesUtils.reduceByteArray(b0, maxRoundNum);
            byte[] c0 = prg.extendToBytes(c0Key);
            BytesUtils.reduceByteArray(c0, maxRoundNum);
            try {
                FileWriter fileWriter = new FileWriter(senderBtgFile);
                PrintWriter printWriter = new PrintWriter(fileWriter, true);
                String a0String = Hex.toHexString(a0);
                printWriter.println(a0String);
                String b0String = Hex.toHexString(b0);
                printWriter.println(b0String);
                String c0String = Hex.toHexString(c0);
                printWriter.println(c0String);
                printWriter.close();
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
                throw new IllegalStateException("Cannot write data into file " + senderBtgFile.getAbsolutePath());
            }
        }
        stopWatch.stop();
        long generateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 0/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), generateTime);

        try {
            stopWatch.start();
            // 开始读取
            InputStreamReader inputStreamReader = new InputStreamReader(
                new FileInputStream(senderBtgFile), StandardCharsets.UTF_8
            );
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String a0String = bufferedReader.readLine();
            byte[] a0 = Hex.decode(a0String);
            String b0String = bufferedReader.readLine();
            byte[] b0 = Hex.decode(b0String);
            String c0String = bufferedReader.readLine();
            byte[] c0 = Hex.decode(c0String);
            booleanTriple = BooleanTriple.create(updateNum, a0, b0, c0);
            bufferedReader.close();
            inputStreamReader.close();
            stopWatch.stop();
            long readTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Send. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), readTime);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Cannot read data from file " + senderBtgFile.getAbsolutePath());
        }
        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public BooleanTriple generate(int num) throws MpcAbortException {
        setPtoInput(num);
        info("{}{} Send. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        BooleanTriple senderOutput = booleanTriple.split(num);
        stopWatch.stop();
        long tripleTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), tripleTime);

        info("{}{} Send. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return senderOutput;
    }

    private String getFileName() {
        return "BTG"
            + FILE_NAME_SEPARATOR + maxRoundNum
            + FILE_NAME_SEPARATOR + updateNum
            + FILE_NAME_SEPARATOR + ownParty().getPartyId() + ".txt";
    }
}
