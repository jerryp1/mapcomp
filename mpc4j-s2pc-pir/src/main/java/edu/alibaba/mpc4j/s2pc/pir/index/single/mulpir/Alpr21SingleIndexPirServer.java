package edu.alibaba.mpc4j.s2pc.pir.index.single.mulpir;

import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.crypto.matrix.database.NaiveDatabase;
import edu.alibaba.mpc4j.s2pc.pir.PirUtils;
import edu.alibaba.mpc4j.s2pc.pir.index.single.AbstractSingleIndexPirServer;
import edu.alibaba.mpc4j.s2pc.pir.index.single.SingleIndexPirParams;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Qixian Zhou
 * @date 2023/5/29
 */
public class Alpr21SingleIndexPirServer extends AbstractSingleIndexPirServer {

    static {
        System.loadLibrary(CommonConstants.MPC4J_NATIVE_FHE_NAME);
    }

    /**
     * SEAL PIR params
     */
    private Alpr21SingleIndexPirParams params;
    /**
     * Galois Keys
     */
    private byte[] galoisKeys;
    /**
     * Relin keys
     */
    private byte[] relinKeys;
    /**
     * element size per BFV plaintext
     */
    private int elementSizeOfPlaintext;
    /**
     * BFV plaintext size
     */
    private int plaintextSize;
    /**
     * dimension size
     */
    private int[] dimensionSize;
    /**
     * BFV plaintext in NTT form
     */
    private List<byte[][]> encodedDatabase;

    public Alpr21SingleIndexPirServer(Rpc serverRpc, Party clientParty, Alpr21SingleIndexPirConfig config) {
        super(Alpr21SingleIndexPirPtoDesc.getInstance(), serverRpc, clientParty, config);
    }

    @Override
    public void init(SingleIndexPirParams indexPirParams, NaiveDatabase database) throws MpcAbortException {
        assert (indexPirParams instanceof Alpr21SingleIndexPirParams);
        params = (Alpr21SingleIndexPirParams) indexPirParams;
        logPhaseInfo(PtoState.INIT_BEGIN);

        // receive Galois keys and Relin keys
        DataPacketHeader clientPublicKeysHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), Alpr21SingleIndexPirPtoDesc.PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
                otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> publicKeyPayload = rpc.receive(clientPublicKeysHeader).getPayload();

        stopWatch.start();
        handleClientPublicKeysPayload(publicKeyPayload);
        encodedDatabase = serverSetup(database);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void init(NaiveDatabase database) throws MpcAbortException {
        params = Alpr21SingleIndexPirParams.DEFAULT_PARAMS;
        logPhaseInfo(PtoState.INIT_BEGIN);

        // receive Galois keys
        DataPacketHeader clientPublicKeysHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), Alpr21SingleIndexPirPtoDesc.PtoStep.CLIENT_SEND_PUBLIC_KEYS.ordinal(), extraInfo,
                otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> publicKeyPayload = rpc.receive(clientPublicKeysHeader).getPayload();

        stopWatch.start();
        handleClientPublicKeysPayload(publicKeyPayload);
        encodedDatabase = serverSetup(database);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void pir() throws MpcAbortException {
        setPtoInput();
        logPhaseInfo(PtoState.PTO_BEGIN);

        DataPacketHeader clientQueryHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), Alpr21SingleIndexPirPtoDesc.PtoStep.CLIENT_SEND_QUERY.ordinal(), extraInfo,
                otherParty().getPartyId(), rpc.ownParty().getPartyId()
        );
        List<byte[]> clientQueryPayload = new ArrayList<>(rpc.receive(clientQueryHeader).getPayload());
        // receive query
        stopWatch.start();
        List<byte[]> serverResponsePayload = generateResponse(clientQueryPayload, encodedDatabase);

        DataPacketHeader serverResponseHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), Alpr21SingleIndexPirPtoDesc.PtoStep.SERVER_SEND_RESPONSE.ordinal(), extraInfo,
                rpc.ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverResponseHeader, serverResponsePayload));
        stopWatch.stop();
        long genResponseTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 1, genResponseTime, "Client generates reply");

        logPhaseInfo(PtoState.PTO_END);
    }

    @Override
    public void setPublicKey(List<byte[]> clientPublicKeysPayload) throws MpcAbortException {
        if (params == null) {
            params = Alpr21SingleIndexPirParams.DEFAULT_PARAMS;
        }
        MpcAbortPreconditions.checkArgument(clientPublicKeysPayload.size() == 2);
        this.galoisKeys = clientPublicKeysPayload.remove(0);
        this.relinKeys = clientPublicKeysPayload.remove(0);
    }

    /**
     * server handle client public keys.
     *
     * @param clientPublicKeysPayload public keys.
     * @throws MpcAbortException the protocol failure aborts.
     */
    public void handleClientPublicKeysPayload(List<byte[]> clientPublicKeysPayload) throws MpcAbortException {
        MpcAbortPreconditions.checkArgument(clientPublicKeysPayload.size() == 2);
        this.galoisKeys = clientPublicKeysPayload.remove(0);
        this.relinKeys = clientPublicKeysPayload.remove(0);
    }

    /**
     * server setup.
     *
     * @return encoded database.
     */
    public List<byte[][]> serverSetup(NaiveDatabase database) {

        int maxPartitionByteLength = params.getPolyModulusDegree() * params.getPlainModulusBitLength() / Byte.SIZE;
        setInitInput(database, database.getByteL(), maxPartitionByteLength);

        elementSizeOfPlaintext = PirUtils.elementSizeOfPlaintext(
                partitionByteLength, params.getPolyModulusDegree(), params.getPlainModulusBitLength()
        );
        plaintextSize = (int) Math.ceil((double) num / this.elementSizeOfPlaintext);
        dimensionSize = PirUtils.computeDimensionLength(plaintextSize, params.getDimension());

        // encode database
        IntStream intStream = IntStream.range(0, databases.length);
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream.mapToObj(this::preprocessDatabase).collect(Collectors.toList());
    }

    /**
     * server handle client query.
     *
     * @param clientQueryPayload client query.
     * @return server response.
     * @throws MpcAbortException the protocol failure aborts.
     */
    public List<byte[]> generateResponse(List<byte[]> clientQueryPayload, List<byte[][]> encodedDatabase) throws MpcAbortException {
        // query ciphertext number should be ceil(dim_sum/N)
        int expectQueryPayloadSize = (Arrays.stream(dimensionSize).sum() % params.getPolyModulusDegree() == 0)
                ? Arrays.stream(dimensionSize).sum() / params.getPolyModulusDegree()
                : Arrays.stream(dimensionSize).sum() / params.getPolyModulusDegree() + 1;

        MpcAbortPreconditions.checkArgument(clientQueryPayload.size() == expectQueryPayloadSize);

        IntStream intStream = IntStream.range(0, databases.length);
        intStream = parallel ? intStream.parallel() : intStream;
        return intStream
                .mapToObj(i -> Alpr21SingleIndexPirNativeUtils.generateReply(
                        params.getEncryptionParams(), galoisKeys, relinKeys, clientQueryPayload, encodedDatabase.get(i), dimensionSize)
                )
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * database preprocess.
     *
     * @param partitionSingleIndex partition index.
     * @return BFV plaintexts in NTT form.
     */
    private byte[][] preprocessDatabase(int partitionSingleIndex) {
        byte[] combinedBytes = new byte[num * partitionByteLength];
        IntStream.range(0, num).forEach(rowSingleIndex -> {
            byte[] element = databases[partitionSingleIndex].getBytesData(rowSingleIndex);
            System.arraycopy(element, 0, combinedBytes, rowSingleIndex * partitionByteLength, partitionByteLength);
        });
        // number of FV plaintexts needed to create the d-dimensional matrix
        int prod = Arrays.stream(dimensionSize).reduce(1, (a, b) -> a * b);
        assert (plaintextSize <= prod);
        List<long[]> coeffsList = new ArrayList<>();
        int byteSizeOfPlaintext = elementSizeOfPlaintext * partitionByteLength;
        int totalByteSize = num * partitionByteLength;
        int usedCoeffSize = elementSizeOfPlaintext *
                ((int) Math.ceil(Byte.SIZE * partitionByteLength / (double) params.getPlainModulusBitLength()));
        assert (usedCoeffSize <= params.getPolyModulusDegree())
                : "coefficient num must be less than or equal to polynomial degree";
        // 字节转换为多项式系数
        int offset = 0;
        for (int i = 0; i < plaintextSize; i++) {
            int processByteSize;
            if (totalByteSize <= offset) {
                break;
            } else if (totalByteSize < offset + byteSizeOfPlaintext) {
                processByteSize = totalByteSize - offset;
            } else {
                processByteSize = byteSizeOfPlaintext;
            }
            assert (processByteSize % partitionByteLength == 0);
            // Get the coefficients of the elements that will be packed in plaintext i
            long[] coeffs = PirUtils.convertBytesToCoeffs(
                    params.getPlainModulusBitLength(), offset, processByteSize, combinedBytes
            );
            assert (coeffs.length <= usedCoeffSize);
            offset += processByteSize;
            long[] paddingCoeffsArray = new long[params.getPolyModulusDegree()];
            System.arraycopy(coeffs, 0, paddingCoeffsArray, 0, coeffs.length);
            // Pad the rest with 1s
            IntStream.range(coeffs.length, params.getPolyModulusDegree()).forEach(j -> paddingCoeffsArray[j] = 1L);
            coeffsList.add(paddingCoeffsArray);
        }
        // Add padding plaintext to make database a matrix
        int currentPlaintextSize = coeffsList.size();
        assert (currentPlaintextSize <= plaintextSize);
        IntStream.range(0, (prod - currentPlaintextSize))
                .mapToObj(i -> IntStream.range(0, params.getPolyModulusDegree()).mapToLong(i1 -> 1L).toArray())
                .forEach(coeffsList::add);
        return Alpr21SingleIndexPirNativeUtils.nttTransform(params.getEncryptionParams(), coeffsList).toArray(new byte[0][]);
    }

}
