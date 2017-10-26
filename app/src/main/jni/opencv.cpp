#include "com_vejoe_opencv_OpenCV.h"
#include "ImageHandler.h"

using namespace std;
using namespace cv;

ImageHandler imgHandler;

String directory;

JNIEXPORT jstring JNICALL
Java_com_vejoe_opencv_OpenCV_getStringFromNative(JNIEnv *env, jclass type) {

    // TODO

    string s = "Hello world from native.";

    return env->NewStringUTF(s.data());
}

JNIEXPORT void JNICALL Java_com_vejoe_opencv_OpenCV_init
        (JNIEnv *env, jclass cls, jstring path)
{
    const char* file = env->GetStringUTFChars(path, NULL);
    String filename(file);
    imgHandler.Load(filename);
    size_t index = filename.find_last_of('/');
    directory = filename.substr(0, index + 1);
    env->ReleaseStringUTFChars(path, file);
}

JNIEXPORT jfloat JNICALL Java_com_vejoe_opencv_OpenCV_detectEyesDistance
        (JNIEnv *env, jobject, jbyteArray array, jint width, jint height, jint cameraType, jfloat focalLength, jboolean needRoation)
{
    jbyte* data = env->GetByteArrayElements(array, NULL);
    //将yuv420sp转rgb
    Mat img(height + height / 2, width, CV_8UC1, (unsigned char*) data);
    Mat imgRgb;
    cvtColor(img, imgRgb, CV_YUV2BGR_NV21, 4);

    //imwrite(directory + "orig.jpg", imgRgb);

    int compressHeight = 1;
    if (needRoation) {
        if (cameraType == 0) // 后置摄像头
        {
            rotate(imgRgb, imgRgb, ROTATE_90_CLOCKWISE);
        }
        else
        {
            rotate(imgRgb, imgRgb, ROTATE_90_COUNTERCLOCKWISE);
        }
        compressHeight = (int) (width * COMPRESS_WIDTH / height); // 旋转之后，高宽互调
    } else {
        compressHeight = (int) (COMPRESS_WIDTH * height / width);
    }

    //imwrite(directory + "rotate.jpg", imgRgb);

    Mat source;

    resize(imgRgb, source, Size(COMPRESS_WIDTH, compressHeight));

    float distance = imgHandler.RecognitionHumanFace(source, focalLength);
//    float distance = 0;
//    imgHandler.DetectLines(source);
    env->ReleaseByteArrayElements(array, data, 0);
    return needRoation? distance : distance * 0.8;
}

JNIEXPORT jintArray JNICALL Java_com_vejoe_opencv_OpenCV_detectLines
        (JNIEnv *env, jobject obj, jbyteArray array, jint width, jint height, jboolean needRotation)
{
    jbyte* data = env->GetByteArrayElements(array, NULL);
    //将yuv420sp转rgb
    Mat img(height + height / 2, width, CV_8UC1, (unsigned char*) data);
    Mat imgRgb;
    cvtColor(img, imgRgb, CV_YUV2BGR_NV21, 4);

    rotate(imgRgb, imgRgb, ROTATE_90_CLOCKWISE);//后置摄像头
    int compressHeight = (int) (width * COMPRESS_WIDTH / height); // 旋转之后，高宽互调

    Mat source;

    resize(imgRgb, source, Size(COMPRESS_WIDTH, compressHeight));

    vector<Vec4i> lines;
    int i = 1;
    if (i == 0) {
        lines = imgHandler.DetectRoadLines(source);
    } else {
        lines = imgHandler.DetectLines(source, needRotation);
    }

    if (lines.size() > 0) {
        int size = lines.size() * 4;
        int* points = (int *) malloc(size * sizeof(int));
        if (points != NULL) {
            float ratio = COMPRESS_WIDTH / (float) height;// 压缩比
            int index = 0;
            for(size_t i = 0; i < lines.size(); i++) {
                Vec4i point = lines[i];
                points[index ++] = (int) (point[0] / ratio);
                points[index ++] = (int) (point[1] / ratio);
                points[index ++] = (int) (point[2] / ratio);
                points[index ++] = (int) (point[3] / ratio);
            }

            jintArray ps = env->NewIntArray(size);
            env->SetIntArrayRegion(ps, 0, size, points);

            delete points;

            return ps;
        }
    }

    return nullptr;
}
