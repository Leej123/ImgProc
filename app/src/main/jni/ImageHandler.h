//
// Created by Administrator on 2017/6/13 0013.
//
#pragma once
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/objdetect/objdetect.hpp>
#include <opencv2/opencv.hpp>
#include <opencv/cv.h>
#include <iostream>

using namespace cv;
using namespace std;

#ifndef IMGPROC_IMAGEHANDLER_H
#define IMGPROC_IMAGEHANDLER_H
#ifdef __cplusplus
extern "C" {
#endif

//图像压缩后的宽度，高度等比缩小
#define COMPRESS_WIDTH  400

class ImageHandler
{

public:
    ImageHandler(void);
    ~ImageHandler(void);
    void Load(String filename);
    float RecognitionHumanFace(Mat, float);
    vector<Vec4i> DetectLines(Mat, bool);
    vector<Vec4i> DetectRoadLines(Mat);
private:
    CascadeClassifier faceCascade;
    int CalculateDistance(vector<Rect>, float);

    /**
     * 匹配模型的文件
     */
    String filename;

    /**
     * 匹配模型文件的目录。日志也将保存在该目录下
     */
    String directory;
};



#ifdef __cplusplus
}
#endif
#endif //IMGPROC_IMAGEHANDLER_H
