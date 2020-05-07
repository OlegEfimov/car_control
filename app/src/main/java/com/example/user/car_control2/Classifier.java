/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.example.user.car_control2;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.SystemClock;
import android.os.Trace;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.ByteBuffer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.PriorityQueue;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
//import org.tensorflow.lite.examples.classification.env.Logger;
//import org.tensorflow.lite.examples.classification.tflite.Classifier.Device;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.nnapi.NnApiDelegate;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
// import org.tensorflow.lite.support.common.TensorProcessor;
// import org.tensorflow.lite.support.image.ImageProcessor;
// import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeOp.ResizeMethod;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import com.example.user.car_control2.ClassifierFloatMobileNet;


/** A classifier specialized to label images using TensorFlow Lite. */
public abstract class Classifier {

  private static final int SENSORS_NUMBER = 10;  // The maximum length of an input sentence.
  private static final String SIMPLE_SPACE_OR_PUNCTUATION = " |\\,|\\;|\\!|\\?";
  /** The model type used for classification. */
  public enum Model {
    FLOAT,
    QUANTIZED,
  }

  /** The runtime device type used for executing classification. */
  public enum Device {
    CPU,
    NNAPI,
    GPU
  }

  /** Number of results to show in the UI. */
  private static final int MAX_RESULTS = 3;

  /** The loaded TensorFlow Lite model. */
  private MappedByteBuffer tfliteModel;

  /** Image size along the x axis. */
  // private final int imageSizeX;

  /** Image size along the y axis. */
  // private final int imageSizeY;

  /** Optional GPU delegate for accleration. */
  private GpuDelegate gpuDelegate = null;

  /** Optional NNAPI delegate for accleration. */
  private NnApiDelegate nnApiDelegate = null;

  /** An instance of the driver class to run model inference with Tensorflow Lite. */
  protected Interpreter tflite;

  /** Options for configuring the Interpreter. */
  private final Interpreter.Options tfliteOptions = new Interpreter.Options();

  /** Labels corresponding to the output of the vision model. */
  private List<String> labels;

  /** Input image TensorBuffer. */
  private TensorBuffer inputBuffer;

  /** Output probability TensorBuffer. */
  private final TensorBuffer outputBuffer;

  /** Processer to apply post processing of the output probability. */
  // private final TensorProcessor probabilityProcessor;

  /**
   * Creates a classifier with the provided configuration.
   *
   * @param activity The current Activity.
   * @return A classifier with the desired configuration.
   */
  public static Classifier create(Activity activity)
      throws IOException {
    return new ClassifierFloatMobileNet(activity);
  }

  /** An immutable result returned by a Classifier describing what was recognized. */
  public static class Recognition {
    /**
     * A unique identifier for what has been recognized. Specific to the class, not the instance of
     * the object.
     */
    private final String id;

    /** Display name for the recognition. */
    private final String title;

    /**
     * A sortable score for how good the recognition is relative to others. Higher should be better.
     */
    private final Float confidence;

    /** Optional location within the source image for the location of the recognized object. */
    private RectF location;

    public Recognition(
        final String id, final String title, final Float confidence, final RectF location) {
      this.id = id;
      this.title = title;
      this.confidence = confidence;
      this.location = location;
    }

    public String getId() {
      return id;
    }

    public String getTitle() {
      return title;
    }

    public Float getConfidence() {
      return confidence;
    }

    public RectF getLocation() {
      return new RectF(location);
    }

    public void setLocation(RectF location) {
      this.location = location;
    }

    @Override
    public String toString() {
      String resultString = "";
      if (id != null) {
        resultString += "[" + id + "] ";
      }

      if (title != null) {
        resultString += title + " ";
      }

      if (confidence != null) {
        resultString += String.format("(%.1f%%) ", confidence * 100.0f);
      }

      if (location != null) {
        resultString += location + " ";
      }

      return resultString.trim();
    }
  }

  /** Initializes a {@code Classifier}. */
  protected Classifier(Activity activity) throws IOException {
    tfliteModel = FileUtil.loadMappedFile(activity, getModelPath());
    tflite = new Interpreter(tfliteModel);

    // Reads type and shape of input and output tensors, respectively.
    int inputTensorIndex = 0;
    int[] inputShape = tflite.getInputTensor(inputTensorIndex).shape(); // {1, height, width, 3}
    DataType inputDataType = tflite.getInputTensor(inputTensorIndex).dataType();
    int outputTensorIndex = 0;
    int[] outputShape =
        tflite.getOutputTensor(outputTensorIndex).shape(); // {1, NUM_CLASSES}
    DataType outputDataType = tflite.getOutputTensor(outputTensorIndex).dataType();

    inputBuffer = TensorBuffer.createFixedSize(inputShape, inputDataType);
    outputBuffer = TensorBuffer.createFixedSize(outputShape, outputDataType);
  }

  float[][] convertMessage(String message) {
    float[] tmp = new float[SENSORS_NUMBER];
    List<String> array = Arrays.asList(message.split(SIMPLE_SPACE_OR_PUNCTUATION));

    int index = 0;
    for (String word : array) {
      if (index >= SENSORS_NUMBER) {
        break;
      }
      tmp[index++] = Float.parseFloat(word);
    }
    // wrapping.
    float[][] ans = {tmp};
    return ans;
  }

  // private float[] convertMessage(String message) {
    // float[][] input = tokenizeInputText(message);
    // String str = message;
      // String delimiter = ",";
      // String[] tempStr;
      // List<String> tempStr = new ArrayList<>();
      // tempStr = str.split(delimiter);
      // float[][] tempValue = new float[1][tempStr.length];
      // for(int i =0; i < tempStr.length ; i++)
          // tempValue[i] = Float.parseFloat(tempStr[i]);
      // return input;
  // }

  /** Runs inference and returns the classification results. */
  public float[][] getAction(String message) {
  // public float[] getAction(String message) {
    // TensorBuffer input = convertMessage(message);
    // float[] input = convertMessage(message);
    float[][] input = convertMessage(message);
    float[][] output = new float[1][1];
    tflite.run(input, output);
    // tflite.run(input, outputBuffer.getBuffer());
    // return outputBuffer;
    return output;
  }

  /** Closes the interpreter and model to release resources. */
  public void close() {
    if (tflite != null) {
      tflite.close();
      tflite = null;
    }
    if (gpuDelegate != null) {
      gpuDelegate.close();
      gpuDelegate = null;
    }
    if (nnApiDelegate != null) {
      nnApiDelegate.close();
      nnApiDelegate = null;
    }
    tfliteModel = null;
  }

  /** Get the image size along the x axis. */
  // public int getImageSizeX() {
  //   return imageSizeX;
  // }

  /** Get the image size along the y axis. */
  // public int getImageSizeY() {
  //   return imageSizeY;
  // }

  /** Loads input image, and applies preprocessing. */
  // private TensorImage loadImage(final Bitmap bitmap, int sensorOrientation) {
  //   // Loads bitmap into a TensorImage.
  //   inputBuffer.load(bitmap);

  //   // Creates processor for the TensorImage.
  //   int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
  //   int numRoration = sensorOrientation / 90;
  //   // TODO(b/143564309): Fuse ops inside ImageProcessor.
  //   ImageProcessor imageProcessor =
  //       new ImageProcessor.Builder()
  //           .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
  //           .add(new ResizeOp(imageSizeX, imageSizeY, ResizeMethod.NEAREST_NEIGHBOR))
  //           .add(new Rot90Op(numRoration))
  //           .add(getPreprocessNormalizeOp())
  //           .build();
  //   return imageProcessor.process(inputBuffer);
  // }

  /** Gets the top-k results. */
  private static List<Recognition> getTopKProbability(Map<String, Float> labelProb) {
    // Find the best classifications.
    PriorityQueue<Recognition> pq =
        new PriorityQueue<>(
            MAX_RESULTS,
            new Comparator<Recognition>() {
              @Override
              public int compare(Recognition lhs, Recognition rhs) {
                // Intentionally reversed to put high confidence at the head of the queue.
                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
              }
            });

    for (Map.Entry<String, Float> entry : labelProb.entrySet()) {
      pq.add(new Recognition("" + entry.getKey(), entry.getKey(), entry.getValue(), null));
    }

    final ArrayList<Recognition> recognitions = new ArrayList<>();
    int recognitionsSize = Math.min(pq.size(), MAX_RESULTS);
    for (int i = 0; i < recognitionsSize; ++i) {
      recognitions.add(pq.poll());
    }
    return recognitions;
  }

  /** Gets the name of the model file stored in Assets. */
  protected abstract String getModelPath();

  /** Gets the name of the label file stored in Assets. */
  protected abstract String getLabelPath();

  /** Gets the TensorOperator to nomalize the input image in preprocessing. */
  protected abstract TensorOperator getPreprocessNormalizeOp();

  /**
   * Gets the TensorOperator to dequantize the output probability in post processing.
   *
   * <p>For quantized model, we need de-quantize the prediction with NormalizeOp (as they are all
   * essentially linear transformation). For float model, de-quantize is not required. But to
   * uniform the API, de-quantize is added to float model too. Mean and std are set to 0.0f and
   * 1.0f, respectively.
   */
  protected abstract TensorOperator getPostprocessNormalizeOp();
}
