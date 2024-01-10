package ru.itskekoff.ai.worker;

import jakarta.annotation.PostConstruct;
import org.datavec.image.loader.NativeImageLoader;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
public class AIWorker {

    private NativeImageLoader loader;
    private ComputationGraph net;

    @PostConstruct
    public void initAI() throws IOException {
        InputStream modelStream = getClass().getResourceAsStream("/model.zip");
        if (modelStream == null) {
            throw new IOException("Model file not found");
        } else {
            net = ModelSerializer.restoreComputationGraph(modelStream);
            loader = new NativeImageLoader(64L, 64L, 1L);
        }
    }

    public String getAnswer(byte[] imageBytes) {
        try {
            InputStream is = new ByteArrayInputStream(imageBytes);
            BufferedImage image = ImageIO.read(is);
            INDArray matrix = loader.asRowVector(image).muli(0.003921569f);
            matrix = matrix.reshape(1L, 1L, 64L, 64L);
            INDArray[] arrays = net.output(matrix);
            StringBuilder strBuilder = new StringBuilder();
            for (int a = 1; a < arrays[0].argMax(1).toIntVector()[0] + 1; ++a) {
                strBuilder.append(arrays[a].argMax(1).toIntVector()[0]);
            }
            return strBuilder.toString();
        } catch (IOException e) {
            return null;
        }
    }
}