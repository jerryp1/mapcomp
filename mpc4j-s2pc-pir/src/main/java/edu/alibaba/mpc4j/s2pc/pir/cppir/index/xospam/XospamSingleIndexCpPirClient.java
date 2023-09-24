package edu.alibaba.mpc4j.s2pc.pir.cppir.index.xospam;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prp.Prp;
import edu.alibaba.mpc4j.common.tool.crypto.prp.PrpFactory;
import edu.alibaba.mpc4j.common.tool.crypto.stream.StreamCipher;
import edu.alibaba.mpc4j.common.tool.crypto.stream.StreamCipherFactory;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.AbstractSingleIndexCpPirClient;
import edu.alibaba.mpc4j.s2pc.pir.cppir.index.xospam.XospamSingleIndexCpPirDesc.PtoStep;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * XOSPAM client-specific preprocessing PIR client.
 *
 * @author Weiran Liu
 * @date 2023/9/24
 */
public class XospamSingleIndexCpPirClient extends AbstractSingleIndexCpPirClient {
    /**
     * stream cipher
     */
    private final StreamCipher streamCipher;
    /**
     * row num
     */
    private int rowNum;
    /**
     * column num
     */
    private int columnNum;
    /**
     * value encrypted key 1
     */
    private byte[] vk1;
    /**
     * value encrypted key 2
     */
    private byte[] vk2;
    /**
     * med PRP
     */
    private Prp medPrp;
    /**
     * final PRP
     */
    private Prp finalPrp;
    /**
     * local cache entries
     */
    private TIntObjectMap<byte[]> localCacheEntries;

    public XospamSingleIndexCpPirClient(Rpc clientRpc, Party serverParty, XospamSingleIndexCpPirConfig config) {
        super(XospamSingleIndexCpPirDesc.getInstance(), clientRpc, serverParty, config);
        streamCipher = StreamCipherFactory.createInstance(envType);
    }

    @Override
    public void init(int n, int l) throws MpcAbortException {
        setInitInput(n, l);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        rowNum = XospamSingleIndexCpPirUtils.getRowNum(n);
        columnNum = XospamSingleIndexCpPirUtils.getColumnNum(n);
        assert rowNum * columnNum >= n
            : "RowNum * ColumnNum must be greater than or equal to n (" + n + "): " + rowNum * columnNum;
        stopWatch.stop();
        long paramTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(
            PtoState.INIT_STEP, 0, 1, paramTime,
            String.format(
                "Client sets params: n = %d, RowNum = %d, ColumnNum = %d, n (pad) = %d",
                n, rowNum, columnNum, rowNum * columnNum
            )
        );

        // preprocessing
        preprocessing();

        logPhaseInfo(PtoState.INIT_END);
    }

    private void preprocessing() throws MpcAbortException {
        stopWatch.start();
        // init keys
        byte[] ik1 = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(ik1);
        medPrp = PrpFactory.createInstance(envType);
        medPrp.setKey(ik1);
        byte[] ik2 = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(ik2);
        finalPrp = PrpFactory.createInstance(envType);
        finalPrp.setKey(ik2);
        vk1 = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(vk1);
        vk2 = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
        secureRandom.nextBytes(vk2);
        localCacheEntries = new TIntObjectHashMap<>();
        stopWatch.stop();
        long allocateTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 3, allocateTime, "Client init keys");

        stopWatch.start();
        // stream receiving rows
        for (int iRow = 0; iRow < rowNum; iRow++) {
            int iFinalRow = iRow;
            DataPacketHeader rowRequestHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_ROW_STREAM_DATABASE_REQUEST.ordinal(), extraInfo,
                otherParty().getPartyId(), rpc.ownParty().getPartyId()
            );
            List<byte[]> rowRequestPayload = rpc.receive(rowRequestHeader).getPayload();

            MpcAbortPreconditions.checkArgument(rowRequestPayload.size() == 1);
            byte[] rowDataByteArray = rowRequestPayload.get(0);
            MpcAbortPreconditions.checkArgument(rowDataByteArray.length == byteL * columnNum);
            // split rows
            ByteBuffer rowByteBuffer = ByteBuffer.wrap(rowDataByteArray);
            byte[][] rowDataArray = new byte[columnNum][byteL];
            for (int iColumn = 0; iColumn < columnNum; iColumn++) {
                rowByteBuffer.get(rowDataArray[iColumn]);
            }
            byte[][] medKeyArray = new byte[columnNum][];
            byte[][] medValueArray = new byte[columnNum][];
            IntStream iColumnIndexStream = IntStream.range(0, columnNum);
            iColumnIndexStream = parallel ? iColumnIndexStream.parallel() : iColumnIndexStream;
            iColumnIndexStream.forEach(iColumn -> {
                // med key
                int key = iFinalRow * columnNum + iColumn;
                byte[] keyBytes = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH)
                    .putInt(CommonConstants.BLOCK_BYTE_LENGTH - Integer.BYTES, key)
                    .array();
                medKeyArray[iColumn] = medPrp.prp(keyBytes);
                // med value
                byte[] iv = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(iv);
                medValueArray[iColumn] = streamCipher.ivEncrypt(vk1, iv, rowDataArray[iColumn]);
            });
            // send response
            ByteBuffer medByteBuffer = ByteBuffer.allocate((CommonConstants.BLOCK_BYTE_LENGTH * 2 + byteL) * columnNum);
            for (int iColumn = 0; iColumn < columnNum; iColumn++) {
                medByteBuffer.put(medKeyArray[iColumn]);
                medByteBuffer.put(medValueArray[iColumn]);
            }
            List<byte[]> rowResponsePayload = Collections.singletonList(medByteBuffer.array());
            DataPacketHeader rowResponseHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_MED_STREAM_DATABASE_RESPONSE.ordinal(), extraInfo,
                rpc.ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(rowResponseHeader, rowResponsePayload));
            extraInfo++;
        }
        stopWatch.stop();
        long rowTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 2, 3, rowTime, "Client handles " + rowNum + " rows");

        stopWatch.start();
        for (int iColumn = 0; iColumn < columnNum; iColumn++) {
            DataPacketHeader columnRequestHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_COLUMN_STREAM_DATABASE_REQUEST.ordinal(), extraInfo,
                otherParty().getPartyId(), rpc.ownParty().getPartyId()
            );
            List<byte[]> columnRequestPayload = rpc.receive(columnRequestHeader).getPayload();

            MpcAbortPreconditions.checkArgument(columnRequestPayload.size() == 1);
            byte[] columnDataByteArray = columnRequestPayload.get(0);
            // each request contains encrypted key + random IV + encrypted value
            MpcAbortPreconditions.checkArgument(
                columnDataByteArray.length == (CommonConstants.BLOCK_BYTE_LENGTH * 2 + byteL) * rowNum
            );
            // split columns
            ByteBuffer columnByteBuffer = ByteBuffer.wrap(columnDataByteArray);
            byte[][] columnKeyArray = new byte[rowNum][CommonConstants.BLOCK_BYTE_LENGTH];
            byte[][] columnValueArray = new byte[rowNum][CommonConstants.BLOCK_BYTE_LENGTH + byteL];
            for (int iRow = 0; iRow < rowNum; iRow++) {
                columnByteBuffer.get(columnKeyArray[iRow]);
                columnByteBuffer.get(columnValueArray[iRow]);
            }
            byte[][] finalKeyArray = new byte[rowNum][];
            byte[][] finalValueArray = new byte[rowNum][];
            IntStream iRowIndexStream = IntStream.range(0, rowNum);
            iRowIndexStream = parallel ? iRowIndexStream.parallel() : iRowIndexStream;
            iRowIndexStream.forEach(iRow -> {
                // final key
                finalKeyArray[iRow] = finalPrp.prp(columnKeyArray[iRow]);
                // final value
                byte[] iv = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                secureRandom.nextBytes(iv);
                finalValueArray[iRow] = streamCipher.ivEncrypt(vk2, iv, columnValueArray[iRow]);
            });
            // send response
            ByteBuffer finalByteBuffer = ByteBuffer.allocate((CommonConstants.BLOCK_BYTE_LENGTH * 3 + byteL) * rowNum);
            for (int iRow = 0; iRow < rowNum; iRow++) {
                finalByteBuffer.put(finalKeyArray[iRow]);
                finalByteBuffer.put(finalValueArray[iRow]);
            }
            List<byte[]> finalResponsePayload = Collections.singletonList(finalByteBuffer.array());
            DataPacketHeader finalResponseHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_FINAL_STREAM_DATABASE_RESPONSE.ordinal(), extraInfo,
                rpc.ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(finalResponseHeader, finalResponsePayload));
            extraInfo++;
        }
        stopWatch.stop();
        long streamTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 3, 3, streamTime, "Client handles " + columnNum + " columns");
    }

    @Override
    public byte[] pir(int x) throws MpcAbortException {
        setPtoInput(x);

        if (localCacheEntries.containsKey(x)) {
            return requestLocalQuery(x);
        } else {
            return requestActualQuery(x);
        }
    }

    private byte[] requestLocalQuery(int x) throws MpcAbortException {
        requestEmptyQuery();
        return localCacheEntries.get(x);
    }

    private void requestEmptyQuery() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        DataPacketHeader queryRequestHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(queryRequestHeader, new LinkedList<>()));
        stopWatch.stop();
        long queryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, queryTime, "Client requests empty query");

        DataPacketHeader queryResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> queryResponsePayload = rpc.receive(queryResponseHeader).getPayload();

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(queryResponsePayload.size() == 0);
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, responseTime, "Client handles empty response");

        logPhaseInfo(PtoState.PTO_END);
    }

    private byte[] requestActualQuery(int x) throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        // PRP x two times
        byte[] keyBytes = ByteBuffer.allocate(CommonConstants.BLOCK_BYTE_LENGTH)
            .putInt(CommonConstants.BLOCK_BYTE_LENGTH - Integer.BYTES, x)
            .array();
        byte[] medKey = medPrp.prp(keyBytes);
        byte[] finalKey = finalPrp.prp(medKey);
        List<byte[]> queryRequestPayload = Collections.singletonList(finalKey);
        DataPacketHeader queryRequestHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
            rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(queryRequestHeader, queryRequestPayload));
        stopWatch.stop();
        long queryTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, queryTime, "Client requests query");

        DataPacketHeader queryResponseHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
            otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> queryResponsePayload = rpc.receive(queryResponseHeader).getPayload();

        stopWatch.start();
        MpcAbortPreconditions.checkArgument(queryResponsePayload.size() == 1);
        byte[] responseByteArray = queryResponsePayload.get(0);
        MpcAbortPreconditions.checkArgument(responseByteArray.length == CommonConstants.BLOCK_BYTE_LENGTH * 2 + byteL);
        // final decrypt
        byte[] ivMedValue = streamCipher.ivDecrypt(vk2, responseByteArray);
        // med decrypt
        byte[] value = streamCipher.ivDecrypt(vk1, ivMedValue);
        // add x to the local cache
        localCacheEntries.put(x, value);
        extraInfo++;
        stopWatch.stop();
        long responseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, responseTime, "Client handles response");

        logPhaseInfo(PtoState.PTO_END);
        return value;
    }
}
