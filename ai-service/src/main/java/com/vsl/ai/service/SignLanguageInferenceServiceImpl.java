package com.vsl.ai.service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.vsl.ai.dto.EvaluationResponse;
import com.vsl.common.exception.AppException;
import com.vsl.common.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Hien thuc {@link SignLanguageInferenceService} bang ONNX Runtime + JavaCV (STATELESS - KHONG ghi DB).
 *
 * Be tu monolith cu (SignLanguageInferenceService) sang microservice:
 *   - BO toan bo phan ghi DB (AttemptHistory / UserProgress / Achievement) -> learning-service lam.
 *   - loadVocabClasses() doc DB -> thay bang {@code validIds} do learning-service truyen qua request
 *     (cac expectedId cua tu CO VIDEO mau). Nho vay ai-service khong can DB.
 *
 * Model: MViTv2-Small (35M params, 135MB)
 *   Input  "video_frames"  : float32 [1, 3, 16, 224, 224]
 *   Output "action_logits" : float32 [1, 1000]
 *
 * Luong xu ly:
 *   1. Luu video tam len dia
 *   2. Trich 16 frames cach deu (FFmpegFrameGrabber) trong cua so [startFrac, endFrac]
 *   3. Resize canh ngan 224 + center-crop 224x224, BGR -> RGB, chuan hoa Kinetics
 *   4. Flatten theo thu tu C->T->H->W thanh float[2408448]
 *   5. Chay ONNX inference (thread-safe, OrtSession dung chung)
 *   6. Softmax MASKED (chi tren validIds) + tim rank cua expectedId
 *   7. Xoa file tam (trong finally, khong phu thuoc ket qua)
 */
@Slf4j
@Service
public class SignLanguageInferenceServiceImpl implements SignLanguageInferenceService {

    // -------------------- ONNX model constants --------------------
    private static final String INPUT_NODE   = "video_frames";
    private static final String OUTPUT_NODE  = "action_logits";
    private static final int REQUIRED_FRAMES = 16;
    private static final int INPUT_SIZE      = 224;
    private static final int NUM_CLASSES     = 1000;

    // Chuan hoa Kinetics chuan (MViTv2/PyTorchVideo): val = (pixel/255 - mean) / std
    // Dung chung cho ca 3 kenh RGB.
    private static final float NORM_MEAN = 0.45f;
    private static final float NORM_STD  = 0.225f;

    // Actual node names resolved from model at startup (may differ from constants above)
    private String resolvedInputNode  = INPUT_NODE;
    private String resolvedOutputNode = OUTPUT_NODE;

    // -------------------- Business logic thresholds --------------------
    private static final float CORRECT_CONFIDENCE_THRESHOLD = 0.5f;
    private static final int   ALMOST_CORRECT_MAX_RANK      = 5;

    @Value("${ai.model.path:./models/mvitv2_small.onnx}")
    private String modelPath;

    // -------------------- ONNX runtime (singleton, thread-safe) --------------------
    private OrtEnvironment ortEnvironment;
    private OrtSession     ortSession;

    /**
     * Load OrtEnvironment va OrtSession mot lan duy nhat khi app khoi dong.
     * Neu model chua ton tai, ghi log warning va de ortSession = null.
     * Cac request se nhan loi AI_MODEL_NOT_LOADED thay vi crash app.
     */
    @PostConstruct
    public void initModel() {
        File modelFile = new File(modelPath);
        if (!modelFile.exists()) {
            log.warn("ONNX model not found at '{}'. AI evaluation will return AI_MODEL_NOT_LOADED.", modelPath);
            return;
        }
        try {
            ortEnvironment = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
            opts.setInterOpNumThreads(1);
            // intra=1 de TAT DINH (bit-exact): >1 thread khien partial-sum cua conv/matmul
            // cong vao theo thu tu khac nhau giua cac lan chay -> confidence rung o bit thap.
            // Single-thread cham hon chut nhung cho cung 1 video luon ra dung 1 ket qua.
            opts.setIntraOpNumThreads(1);
            // ALL_OPT: full graph optimization (nhanh nhat). Model da duoc xu ly bang
            // symbolic shape inference (scripts/fix_onnx_model.py) nen Loop subgraph
            // co shape ro rang -> chay tot voi moi muc optimization.
            opts.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
            // BAT BUOC tat memory-pattern: model co Loop node (pooling-attention) shape dong.
            // Memory-pattern "hoc" layout bo nho tu lan chay DAU roi tai dung buffer cho cac
            // lan sau -> voi Loop node, ke hoach do KHONG hop le tu run thu 2 -> output rac
            // (trieu chung: lan dau dung 98.7%, cac lan sau deu sai id=785/0.22%).
            opts.setMemoryPatternOptimization(false);
            ortSession = ortEnvironment.createSession(modelPath, opts);

            // Log actual node names — verify they match our INPUT_NODE / OUTPUT_NODE constants
            log.info("ONNX model loaded from '{}':", modelPath);
            ortSession.getInputInfo().forEach((name, info) ->
                    log.info("  Input  node: '{}' shape={}", name, info));
            ortSession.getOutputInfo().forEach((name, info) ->
                    log.info("  Output node: '{}' shape={}", name, info));

            // Resolve actual node names; fall back to first node if constants don't match
            if (!ortSession.getInputNames().contains(INPUT_NODE)) {
                resolvedInputNode = ortSession.getInputNames().iterator().next();
                log.warn("Expected input node '{}' not found — using '{}' from model",
                        INPUT_NODE, resolvedInputNode);
            }
            if (!ortSession.getOutputNames().contains(OUTPUT_NODE)) {
                resolvedOutputNode = ortSession.getOutputNames().iterator().next();
                log.warn("Expected output node '{}' not found — using '{}' from model",
                        OUTPUT_NODE, resolvedOutputNode);
            }
            log.info("Ready: input='{}', output='{}'", resolvedInputNode, resolvedOutputNode);

        } catch (Throwable e) {
            log.error("Failed to load ONNX model [{}]: {} — AI evaluation unavailable.",
                    e.getClass().getSimpleName(), e.getMessage());
        }
    }

    /** Dong OrtSession va OrtEnvironment khi app shutdown. */
    @PreDestroy
    public void destroyModel() {
        try {
            if (ortSession != null)     ortSession.close();
            if (ortEnvironment != null) ortEnvironment.close();
            log.info("ONNX resources released.");
        } catch (OrtException e) {
            log.warn("Error while closing ONNX resources: {}", e.getMessage());
        }
    }

    // ========================= PUBLIC API =========================

    /**
     * Chay tren aiTaskExecutor (thread pool rieng biet) de khong chiem Tomcat thread.
     * Spring MVC giu request "mo" cho den khi CompletableFuture hoan thanh.
     * Ket qua tra ve qua CompletableFuture.completedFuture(); exception tu dong
     * duoc boc vao exceptionally-completed Future va duoc GlobalExceptionHandler xu ly.
     *
     * @param validIds cac expectedId cua tu CO VIDEO mau (do learning-service truyen sang).
     *                 Chi cham diem trong tap nay (softmax MASKED). Rong/null -> fallback softmax 1000 lop.
     */
    @Override
    @Async("aiTaskExecutor")
    public CompletableFuture<EvaluationResponse> evaluate(MultipartFile videoFile, int expectedId,
                                                          float startFrac, float endFrac,
                                                          Collection<Integer> validIds) {
        validateVideoFile(videoFile);

        if (ortSession == null) {
            throw new AppException(ErrorCode.AI_MODEL_NOT_LOADED);
        }

        File tempFile = null;
        try {
            // 1. Luu video xuong file tam (MultipartFile con hop le: Spring giu request song)
            tempFile = File.createTempFile("vsl_practice_", ".mp4");
            videoFile.transferTo(tempFile);
            log.debug("Saved practice video to temp: {} ({} bytes)", tempFile.getName(), tempFile.length());

            // 2. Trich 16 frames cach deu nhau TRONG cua so dong tac [startFrac, endFrac]
            Mat[] frames = extractSampledFrames(tempFile, startFrac, endFrac);

            // 3. Tao tensor float[1,3,16,224,224] va flatten
            float[] tensorData = buildTensorData(frames); // frames[i].release() duoc goi ben trong

            // 4. ONNX inference voi try-with-resources (C++ tu don rac)
            float[] logits = runInference(tensorData);

            // 5. Softmax CHI TREN cac tu CO VIDEO (validIds) - bo qua cac lop app khong dung.
            //    Khong cham "cam" canh tranh voi 1000 lop -> hang & do tin phan anh dung
            //    pham vi app, khong bi cac lop nhieu keo xuong.
            Set<Integer> validClasses = normalizeValidIds(validIds);
            float[] probabilities = softmaxMasked(logits, validClasses);
            logTopPredictions(probabilities, validClasses, 5);
            EvaluationResponse response = buildResponse(probabilities, expectedId);

            log.info("Evaluation done: expectedId={}, status={}, confidence={}%, rank={}",
                    expectedId, response.getStatus(), response.getConfidence(), response.getRank());
            return CompletableFuture.completedFuture(response);

        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Evaluation failed for expectedId={}: {}", expectedId, e.getMessage(), e);
            throw new AppException(ErrorCode.AI_INFERENCE_ERROR, "Không thể xử lý video: " + e.getMessage());
        } finally {
            // BAT BUOC: xoa file tam de bao ve quyen rieng tu
            if (tempFile != null && tempFile.exists()) {
                boolean deleted = tempFile.delete();
                if (!deleted) {
                    log.warn("Could not delete temp file: {}. Schedule for JVM exit.", tempFile.getAbsolutePath());
                    tempFile.deleteOnExit();
                }
            }
        }
    }

    // ========================= FRAME EXTRACTION =========================

    /**
     * Trich 16 frames cach deu tu video bang decode TUAN TU (deterministic).
     *
     * KHONG dung setVideoFrameNumber (seek theo keyframe): no khong tat dinh
     * (cung 1 video, 2 lan goi ra 2 bo frame khac nhau -> argmax 453 vs 463) va
     * lech voi pipeline train/Python. Decode tuan tu toan bo roi lay mau deu cho
     * ket qua khop chinh xac Python read_all() + uniform sample (vd 72 frame ->
     * index [0,4,9,...,67]). Voi video luyen tap ngan, chi phi decode khong dang ke.
     *
     * Neu tong frames < 16: nhan ban frame cuoi den du 16.
     */
    private Mat[] extractSampledFrames(File videoFile, float startFrac, float endFrac) throws Exception {
        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();

        // try-with-resources: grabber.close() goi ca stop() va release() trong moi truong hop,
        // ke ca khi stop() nem exception — tranh leak con tro C++ cua JNI.
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile)) {
            try {
                grabber.start();
                return sequentialGrabAndSample(grabber, converter, startFrac, endFrac);
            } catch (AppException e) {
                throw e;
            } catch (Exception e) {
                throw new AppException(ErrorCode.VIDEO_CORRUPT,
                        "FFmpeg không mở được video: " + e.getMessage());
            }
        }
    }

    /**
     * Grab toan bo roi lay 16 mau deu TRONG cua so dong tac [startFrac, endFrac].
     * Cat sat dong tac giup 16 frame khong bi loang boi doan tay dung yen -> MViTv2
     * nhan dien on dinh & chinh xac hon (khop phan bo luc train). startFrac/endFrac
     * mac dinh 0..1 (ca video) -> giu nguyen hanh vi cu neu frontend khong gui.
     */
    private Mat[] sequentialGrabAndSample(FFmpegFrameGrabber grabber,
                                           OpenCVFrameConverter.ToMat converter,
                                           float startFrac, float endFrac) throws Exception {
        List<Mat> buffer = new ArrayList<>();
        try {
            Frame f;
            while ((f = grabber.grabImage()) != null) {
                Mat mat = converter.convert(f);
                if (mat != null && !mat.isNull()) {
                    buffer.add(mat.clone());
                }
            }
        } catch (Exception e) {
            // Giai phong buffer da load truoc khi re-throw
            buffer.forEach(Mat::release);
            throw e;
        }

        if (buffer.isEmpty()) {
            throw new AppException(ErrorCode.VIDEO_NO_FRAMES);
        }

        int bufSize = buffer.size();

        // Cua so dong tac trong buffer. Cua so qua hep / khong hop le -> dung ca video.
        float s = Math.max(0f, Math.min(1f, startFrac));
        float e = Math.max(0f, Math.min(1f, endFrac));
        if (e - s < 0.05f) { s = 0f; e = 1f; }
        int lo = Math.max(0, Math.min(bufSize - 1, Math.round(s * bufSize)));
        int hi = Math.max(lo + 1, Math.min(bufSize, Math.round(e * bufSize)));
        int win = hi - lo;

        // Lay 16 mau deu TRONG cua so; CLONE (khong tieu thu) roi giai phong toan bo buffer.
        // Neu cua so < 16 frame: nhan ban frame cuoi cua cua so cho du 16.
        Mat[] frames = new Mat[REQUIRED_FRAMES];
        for (int i = 0; i < REQUIRED_FRAMES; i++) {
            int idx = (win < REQUIRED_FRAMES)
                    ? lo + Math.min(i, win - 1)
                    : lo + (int) ((long) i * win / REQUIRED_FRAMES);
            frames[i] = buffer.get(idx).clone();
        }
        buffer.forEach(Mat::release);

        log.debug("Sampled 16 frames from window [{},{}) of {} decoded frames", lo, hi, bufSize);
        return frames;
    }

    // ========================= TENSOR CONSTRUCTION =========================

    /**
     * Chuyen 16 Mat frames thanh float[2_408_448] theo thu tu CTHW.
     *
     * Shape: [1, C=3, T=16, H=224, W=224]
     * Index : c*(T*H*W) + f*(H*W) + y*W + x
     *
     * Moi frame (BGR tu OpenCV) duoc:
     *   1. Resize canh ngan ve 224 (giu ty le), center-crop 224x224 (chuan MViTv2)
     *   2. Chuyen sang RGB
     *   3. Chuan hoa Kinetics: (pixel/255 - 0.45) / 0.225
     *   4. .release() ngay lap tuc sau khi lay du lieu
     */
    private float[] buildTensorData(Mat[] frames) {
        final int T = REQUIRED_FRAMES, H = INPUT_SIZE, W = INPUT_SIZE, C = 3;
        float[] tensor = new float[C * T * H * W];

        for (int f = 0; f < T; f++) {
            if (frames[f] == null) {
                // Frame null sau fill => ghi 0 (black frame)
                continue;
            }
            Mat resized = new Mat();
            Mat cropped = null;
            Mat rgb     = new Mat();
            try {
                // Resize canh ngan ve INPUT_SIZE, giu nguyen ty le (tranh bop meo video doc/ngang)
                int srcH = frames[f].rows(), srcW = frames[f].cols();
                double scale = (double) INPUT_SIZE / Math.min(srcH, srcW);
                int nh = (int) Math.round(srcH * scale);
                int nw = (int) Math.round(srcW * scale);
                opencv_imgproc.resize(frames[f], resized, new Size(nw, nh));

                // Center-crop 224x224
                int x0 = Math.max(0, (nw - W) / 2);
                int y0 = Math.max(0, (nh - H) / 2);
                cropped = new Mat(resized, new Rect(x0, y0, W, H));

                opencv_imgproc.cvtColor(cropped, rgb, opencv_imgproc.COLOR_BGR2RGB);

                byte[] pixels = new byte[H * W * C]; // HWC layout
                rgb.data().get(pixels);

                // Vong lap: C -> T -> H -> W (CTHW)
                for (int c = 0; c < C; c++) {
                    for (int y = 0; y < H; y++) {
                        for (int x = 0; x < W; x++) {
                            int srcIdx = (y * W + x) * C + c;          // HWC -> lay kenh c
                            int dstIdx = c * (T * H * W)
                                       + f * (H * W)
                                       + y * W
                                       + x;
                            float scaled = (pixels[srcIdx] & 0xFF) / 255.0f;
                            tensor[dstIdx] = (scaled - NORM_MEAN) / NORM_STD;
                        }
                    }
                }
            } finally {
                // BAT BUOC giai phong Mat ngay trong vong lap de chong leak RAM
                resized.release();
                if (cropped != null) cropped.release();
                rgb.release();
                frames[f].release();
            }
        }
        return tensor;
    }

    // ========================= ONNX INFERENCE =========================

    /**
     * Chay inference va tra ve mang logits thu, float[1000].
     * Boc OnnxTensor va OrtSession.Result bang try-with-resources.
     */
    private float[] runInference(float[] tensorData) throws OrtException {
        long[] shape = {1L, 3L, REQUIRED_FRAMES, INPUT_SIZE, INPUT_SIZE};

        try (OnnxTensor inputTensor = OnnxTensor.createTensor(
                ortEnvironment, FloatBuffer.wrap(tensorData), shape);
             OrtSession.Result result = ortSession.run(Map.of(resolvedInputNode, inputTensor))) {

            float[][] output = (float[][]) result.get(resolvedOutputNode)
                    .orElseThrow(() -> new OrtException("Output node '" + resolvedOutputNode + "' not found in model"))
                    .getValue();

            // Clone truoc khi result.close() giai phong bo nho native
            return output[0].clone();
        }
    }

    // ========================= MATH HELPERS =========================

    /**
     * Softmax on-the-fly voi max-subtraction de tranh overflow numerics.
     */
    private float[] softmax(float[] logits) {
        float max = Float.NEGATIVE_INFINITY;
        for (float v : logits) if (v > max) max = v;

        float[] probs = new float[logits.length];
        float sum = 0f;
        for (int i = 0; i < logits.length; i++) {
            probs[i] = (float) Math.exp(logits[i] - max);
            sum += probs[i];
        }
        for (int i = 0; i < probs.length; i++) probs[i] /= sum;
        return probs;
    }

    /**
     * Chuan hoa tap validIds nhan tu request: bo null, bo trung, bo id ngoai [0, NUM_CLASSES).
     * Giu thu tu on dinh (LinkedHashSet) de log de doc.
     */
    private Set<Integer> normalizeValidIds(Collection<Integer> validIds) {
        Set<Integer> set = new LinkedHashSet<>();
        if (validIds != null) {
            for (Integer id : validIds) {
                if (id != null && id >= 0 && id < NUM_CLASSES) {
                    set.add(id);
                }
            }
        }
        return set;
    }

    /**
     * Softmax CHI tren cac lop hop le (tu co video); cac lop khac tra ve xac suat 0.
     * Nho vay rank/confidence tinh trong pham vi cac tu co video, khong bi cac lop nhieu.
     */
    private float[] softmaxMasked(float[] logits, Set<Integer> validIds) {
        if (validIds.isEmpty()) return softmax(logits); // fallback: khong co validIds -> nhu cu

        float max = Float.NEGATIVE_INFINITY;
        for (int id : validIds) {
            if (id >= 0 && id < logits.length && logits[id] > max) max = logits[id];
        }
        float[] probs = new float[logits.length]; // mac dinh 0 cho lop khong hop le
        float sum = 0f;
        for (int id : validIds) {
            if (id < 0 || id >= logits.length) continue;
            float e = (float) Math.exp(logits[id] - max);
            probs[id] = e;
            sum += e;
        }
        if (sum > 0f) {
            for (int id : validIds) if (id >= 0 && id < logits.length) probs[id] /= sum;
        }
        return probs;
    }

    /** Log top-K du doan (CHI gom cac lop hop le) de doi chieu. */
    private void logTopPredictions(float[] probs, Set<Integer> validIds, int k) {
        if (validIds.isEmpty()) return;
        List<Integer> ids = new ArrayList<>(validIds);
        ids.sort((a, b) -> Float.compare(probs[b], probs[a]));
        StringBuilder sb = new StringBuilder("Top " + k + " (chi trong " + validIds.size() + " lop co video):");
        for (int i = 0; i < Math.min(k, ids.size()); i++) {
            int id = ids.get(i);
            sb.append(String.format(" #%d[id=%d %.1f%%]", i + 1, id, probs[id] * 100));
        }
        log.info(sb.toString());
    }

    /**
     * Tim thu hang cua expectedId (1 = xac suat cao nhat).
     * O(n) don gian, khong sort toan bo mang.
     */
    private int findRank(float[] probs, int expectedId) {
        if (expectedId < 0 || expectedId >= probs.length) {
            log.warn("expectedId={} out of range [0,{}). Model has {} classes.",
                    expectedId, probs.length, probs.length);
            return probs.length;
        }
        float target = probs[expectedId];
        int rank = 1;
        for (float p : probs) {
            if (p > target) rank++;
        }
        return rank;
    }

    /** Tim index co xac suat cao nhat. */
    private int findTopPredictedId(float[] probs) {
        int best = 0;
        for (int i = 1; i < probs.length; i++) {
            if (probs[i] > probs[best]) best = i;
        }
        return best;
    }

    // ========================= BUSINESS LOGIC =========================

    private EvaluationResponse buildResponse(float[] probs, int expectedId) {
        int topPredictedId = findTopPredictedId(probs);
        int rank           = findRank(probs, expectedId);
        double confidence  = (expectedId >= 0 && expectedId < probs.length)
                ? Math.round(probs[expectedId] * 10000.0) / 100.0
                : 0.0;

        String status;
        String message;

        if (rank == 1 && probs[expectedId] > CORRECT_CONFIDENCE_THRESHOLD) {
            status  = "CORRECT";
            message = "Chính xác! Bạn thực hiện ký hiệu rất tốt.";
        } else if (rank <= ALMOST_CORRECT_MAX_RANK) {
            status  = "ALMOST_CORRECT";
            message = "Gần đúng! Hãy chú ý hơn đến tư thế tay và góc độ cơ thể.";
        } else {
            status  = "INCORRECT";
            message = "Chưa đúng. Hãy xem lại video mẫu và thử lại từ đầu.";
        }

        return EvaluationResponse.builder()
                .status(status)
                .message(message)
                .confidence(confidence)
                .predictedId(topPredictedId)
                .rank(rank)
                .build();
    }

    // ========================= VALIDATION =========================

    private void validateVideoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.VIDEO_EMPTY);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new AppException(ErrorCode.VIDEO_INVALID_TYPE);
        }
    }
}
