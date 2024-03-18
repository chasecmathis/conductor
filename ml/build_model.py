import os
import tensorflow as tf
import tf_keras as keras
import cv2
import numpy as np

os.environ["TF_USE_LEGACY_KERAS"]="1"
train_data = os.path.join('data/asl_alphabet_train/asl_alphabet_train/')
num_classes = 29
batch_size = 32
img_height, img_width = 200, 200


def load_dataset():
    train_dataset = tf.keras.utils.image_dataset_from_directory(
        train_data,
        color_mode="grayscale",
        interpolation="area",
        validation_split=0.15,
        seed=13,
        subset="training",
        image_size=(img_height, img_width),
        batch_size=batch_size
    )

    test_dataset = tf.keras.utils.image_dataset_from_directory(
        train_data,
        color_mode="grayscale",
        interpolation="area",
        validation_split=0.15,
        seed=13,
        subset="validation",
        image_size=(img_height, img_width),
        batch_size=batch_size
    )

    print(train_dataset)
    return train_dataset, test_dataset


def get_model():
    model = keras.models.Sequential()
    # Input layer
    model.add(keras.layers.Rescaling(1. / 255, input_shape=(img_height, img_width, 1)))
    model.add(keras.layers.Conv2D(filters=32, kernel_size=(5, 5), strides=(4, 4), activation="relu"))
    model.add(keras.layers.MaxPooling2D(pool_size=(3, 3), strides=(2, 2)))
    model.add(keras.layers.BatchNormalization())

    # Convolutional layers
    model.add(keras.layers.Conv2D(filters=64, kernel_size=(5, 5), padding="same", activation="relu"))
    model.add(keras.layers.MaxPooling2D(pool_size=(3, 3), strides=(2, 2)))
    model.add(keras.layers.BatchNormalization())

    model.add(keras.layers.Conv2D(filters=96, kernel_size=(5, 5), padding="same", activation="relu"))
    model.add(keras.layers.Conv2D(filters=96, kernel_size=(5, 5), padding="same", activation="relu"))
    model.add(keras.layers.Conv2D(filters=64, kernel_size=(5, 5), padding="same", activation="relu"))
    model.add(keras.layers.MaxPooling2D(pool_size=(3, 3), strides=(2, 2)))
    model.add(keras.layers.BatchNormalization())

    # # # Fully connected layers
    model.add(keras.layers.Flatten())
    model.add(keras.layers.Dense(1024, activation="relu"))
    model.add(keras.layers.Dropout(0.5))
    model.add(keras.layers.Dense(512, activation="relu"))
    model.add(keras.layers.Dropout(0.5))
    model.add(keras.layers.Dense(num_classes, activation="softmax"))

    print(model.summary())

    return model


def train_model(model, train_dataset, test_dataset):
    # Compile the model
    model.compile(optimizer=tf.keras.optimizers.legacy.Adam(learning_rate=0.001),
                  loss='sparse_categorical_crossentropy',
                  metrics=['accuracy'])

    # Train the model
    model.fit(train_dataset,
              epochs=15,
              validation_data=test_dataset,
              callbacks=[tf.keras.callbacks.EarlyStopping(monitor='val_accuracy', patience=4, min_delta=0.001)]
              )

    # Evaluate the model
    test_loss, test_acc = model.evaluate(test_dataset)
    print('\nTest accuracy:', test_acc)

    return


def run_interference(filename):
    # Load the image
    image = cv2.imread(filename, cv2.IMREAD_GRAYSCALE)  # Load the image in grayscale

    # Resize the image to match the input shape of the model
    image_resized = cv2.resize(image, (200, 200))

    # Normalize the image pixel values to be in the range [0, 1]
    image_normalized = image_resized / 255.0

    # Add batch dimension to match the model input shape [1, 200, 200, 1]
    image_input = image_normalized.reshape(1, 200, 200, 1)

    # Load the TFLite model
    interpreter = tf.lite.Interpreter(model_path="gesture.tflite")
    interpreter.allocate_tensors()

    # Get input and output details
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    # Set the input tensor with the preprocessed image input
    interpreter.set_tensor(input_details[0]['index'], image_input.astype(np.float32))

    # Run inference
    interpreter.invoke()

    # Get the output tensor
    output_data = interpreter.get_tensor(output_details[0]['index'])

    # Get the predicted class index
    predicted_class_index = np.argmax(output_data)

    # Print the predicted class index
    print("Predicted class index: ", train_dataset.class_names[predicted_class_index])
    return

train_dataset, test_dataset = load_dataset()
model = get_model()
train_model(model, train_dataset, test_dataset)

converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

# Save the model.
with open('gesture.tflite', 'wb') as f:
    f.write(tflite_model)

# run_interference("data/asl_alphabet_test/asl_alphabet_test/A_test.jpg")