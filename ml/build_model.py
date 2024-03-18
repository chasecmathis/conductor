import os
import tensorflow as tf

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
    model = tf.keras.models.Sequential()
    # Input layer
    model.add(tf.keras.layers.Rescaling(1. / 255, input_shape=(img_height, img_width, 1)))
    model.add(tf.keras.layers.Conv2D(filters=16, kernel_size=(5, 5), strides=(4, 4), activation="relu"))
    model.add(tf.keras.layers.MaxPooling2D(pool_size=(3, 3), strides=(2, 2)))
    model.add(tf.keras.layers.BatchNormalization())

    # Convolutional layers
    model.add(tf.keras.layers.Conv2D(filters=32, kernel_size=(5, 5), padding="same", activation="relu"))
    model.add(tf.keras.layers.MaxPooling2D(pool_size=(3, 3), strides=(2, 2)))
    model.add(tf.keras.layers.BatchNormalization())

    model.add(tf.keras.layers.Conv2D(filters=64, kernel_size=(5, 5), padding="same", activation="relu"))
    model.add(tf.keras.layers.MaxPooling2D(pool_size=(3, 3), strides=(2, 2)))
    model.add(tf.keras.layers.BatchNormalization())

    # # Fully connected layers
    model.add(tf.keras.layers.Flatten())
    model.add(tf.keras.layers.Dense(1024, activation="relu"))
    model.add(tf.keras.layers.Dropout(0.5))
    model.add(tf.keras.layers.Dense(512, activation="relu"))
    model.add(tf.keras.layers.Dropout(0.5))
    model.add(tf.keras.layers.Dense(num_classes, activation="softmax"))

    print(model.summary())

    return model


def train_model(model, train_dataset, test_dataset):
    # Compile the model
    model.compile(optimizer=tf.keras.optimizers.Adam(learning_rate=0.001),
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


train_dataset, test_dataset = load_dataset()
model = get_model()
train_model(model, train_dataset, test_dataset)

converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

# Save the model.
with open('gesture.tflite', 'wb') as f:
    f.write(tflite_model)
