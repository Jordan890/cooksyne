package com.cartandcook.adapters.ailocal;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.cartandcook.core.ai.FoodClassifier;
import com.cartandcook.core.ai.VisionResult;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "cartcook.ai.mode", havingValue = "local", matchIfMissing = true)
@ConfigurationProperties(prefix = "cartcook.ai.local")
public class LocalFoodClassifier implements FoodClassifier {

    private String modelPath;

    private OrtEnvironment env;
    private OrtSession session;

    public String getModelPath() { return modelPath; }
    public void setModelPath(String modelPath) { this.modelPath = modelPath; }

    @PostConstruct
    public void init() throws OrtException {
        env = OrtEnvironment.getEnvironment();
        session = env.createSession(modelPath, new OrtSession.SessionOptions());
    }

    @Override
    public VisionResult classify(byte[] imageBytes) throws IOException {
        // 1️⃣ Load image
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));

        // 2️⃣ Preprocess image to float array [1,3,224,224]
        float[] inputData = preprocessImage(img);

        // 3️⃣ Run inference
        try (OnnxTensor tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputData), new long[]{1, 3, 224, 224})) {
            float[][] result = (float[][]) session.run(Map.of("input", tensor)).get(0).getValue();

            // 4️⃣ Convert float array to predictions
            List<VisionResult.Prediction> predList = new ArrayList<>();
            for (int i = 0; i < result[0].length; i++) {
                predList.add(
                        VisionResult.Prediction.builder()
                                .label("class" + i) // replace with real class names if available
                                .confidence(result[0][i])
                                .build()
                );
            }

            // 5️⃣ Build VisionResult
            return VisionResult.builder()
                    .predictions(predList)
                    .build();
        } catch (OrtException e) {
            throw new RuntimeException(e);
        }
    }

    private float[] preprocessImage(BufferedImage img) {
        // TODO: resize to 224x224, normalize pixels 0..1, convert to float[3*224*224]
        return new float[3 * 224 * 224];
    }
}



