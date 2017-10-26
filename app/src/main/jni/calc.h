//
// Created by Administrator on 2017/7/26 0026.
//
#pragma once

#ifndef IMGPROC_CALC_H
#define IMGPROC_CALC_H

#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/objdetect/objdetect.hpp>
#include <opencv2/opencv.hpp>
#include <opencv/cv.h>
#include <iostream>

using namespace cv;
using namespace std;

typedef struct _Line {
    Point p1, p2;
    double a, b, c;
    double angle;
    double length;
    int roadVal;
    /**
     * 0左边，1右边
     */
    int leftOrRight;
    _Line* next;
}Line;

typedef struct _CrossPoint {
    Line* line;
    int x;
    int y;
    double sad;
}CrossPoint;

#ifdef __cplusplus
extern "C" {
#endif

Line* HoughLinkListCreate(vector<Vec4i> lines);
Line* AngleThresh(Line*, double);
Line* LinkLengthSort(Line*);
Line* DeleteNode(Line*, Line*);
Line* DeleteNodeForSort(Line*, Line*);
Point HalfPoint(Point, Point);
bool FindRoadLine(Mat* gray, Mat* canny, Mat* cannyColor, Point center, Line* pLines, Line scanLine, int num);
bool AngleOk(double, double);
bool GetCrossPoint(Line &line1, Line &line2, CrossPoint &point);
double GetAngle(Point, Point);
double GetDistance(Point, Point);
double CalculateBlock(Mat* gray, Point point1, Point point2, int size);
int GetListLength(Line*);
int ArrayPoint(CrossPoint pair[], int p, int r);
void FreeLink(Line*);
void GetLinePara(Line &line);
void PointQuickSort(CrossPoint pair[], int p, int r);

#ifdef __cplusplus
}
#endif

#endif //IMGPROC_CALC_H
