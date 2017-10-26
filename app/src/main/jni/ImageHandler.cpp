//
// Created by Administrator on 2017/6/13 0013.
//
#include "ImageHandler.h"
#include "androidlog.h"
#include "RecordLog.h"
#include "calc.h"

#define  MAX_VERTICAL_HEIGHT	50
#define  MIN_HORIZON_WIDTH		50
#define  CONSTANT_MULTIPLY		3500

void PrintLines(Line* head) {
    Line* node = head;
    int count = 0;
    while (node != NULL) {
        LOGD("P1(%d,%d)P2(%d,%d)L(%f)A(%f)LR(%d)", node->p1.x, node->p1.y, node->p2.x, node->p2.y, node->length,node->angle,node->leftOrRight);
        count ++;
        node = node->next;
    }
    LOGD("Total Lines:%d", count);
}

bool FilterInvalidEyes(vector<Rect> faces, vector<Rect>& result);

ImageHandler::ImageHandler()
{
    filename = "";
}

ImageHandler::~ImageHandler()
{

}

/**
 * 加载匹配模型
 * @param filename
 */
void ImageHandler::Load(String filename)
{
    if (this->filename.compare(filename) != 0)
    {
        //使用人脸识别时使用（加载匹配模型）
        if (!faceCascade.load(filename))
        {
            LOGD("--(!)Error loading: haar.xml");
        }

        this->filename = filename;
        size_t index = filename.find_last_of('/');
        directory = filename.substr(0, index + 1);
    }
}

float ImageHandler::RecognitionHumanFace(Mat sourceFrame, float focalLength)
{
    vector<Rect> faces;
    Mat faceGray;

    //灰度处理（彩色图像变为黑白）
    cvtColor(sourceFrame, faceGray, CV_RGB2GRAY);
    //灰度图象直方图均衡化（归一化图像亮度和增强对比度）
    equalizeHist(faceGray, faceGray);
    //瞳孔识别
    faceCascade.detectMultiScale(faceGray, faces, 1.1, 3, 0 | CV_HAAR_SCALE_IMAGE, Size(30, 30), Size(300, 300));

    vector<Rect> eyes(2);
    bool invalidFlag = FilterInvalidEyes(faces, eyes);
    if (!invalidFlag)
    {
        return -1;
    }

//    for(vector<Rect>::const_iterator r = eyes.begin(); r != eyes.end(); r ++) {
//        Point center(cvRound(r->x + r->width * 0.5), cvRound(r->y + r->height * 0.5));
//        circle(sourceFrame, center, 3, Scalar(255, 0, 255), 3);
//    }

    int distance = CalculateDistance(eyes, focalLength);

    return distance;
}

//距离计算
int ImageHandler::CalculateDistance(vector<Rect> eyes, float focalLength)
{
    int cameraDistance = abs(eyes[0].x - eyes[1].x);
    float m = CONSTANT_MULTIPLY * focalLength / 4;
    int distance = cvRound(m / cameraDistance);
    return distance;
}

bool FilterInvalidEyes(vector<Rect> faces, vector<Rect>& result)
{
    bool flag = false;
    int iCount = faces.size();
    if (iCount < 2) return flag;

    Rect one, other;
    int minDiff = 10000, tempDiff = 0;
    for (vector<Rect>::const_iterator r = faces.begin(); r != faces.end(); r ++)
    {
        for (vector<Rect>::const_iterator t = faces.begin(); t != faces.end(); t ++)
        {
            if (r == t) continue;
            tempDiff = abs(r->y - t->y);
            if (tempDiff < minDiff && tempDiff <= MAX_VERTICAL_HEIGHT && abs(r->x - t->x) >= MIN_HORIZON_WIDTH)
            {
                flag = true;
                one = *r;
                other = *t;
                minDiff = tempDiff;
            }
        }
    }

    if (flag)
    {
        result.clear();
        result.push_back(one);
        result.push_back(other);
    }

    return flag;
}

vector<Vec4i> ImageHandler::DetectLines(Mat source, bool rotation)
{
    //过滤白色图案
    Mat imgHsv, imgThreshold;
    cvtColor(source,imgHsv,CV_BGR2HSV);
    inRange(imgHsv, Scalar(0,0,221), Scalar(180, 30, 255), imgThreshold);

    //imwrite(directory + "white.jpg", imgThreshold);

    Mat shapeOperateKernel;
    morphologyEx(imgThreshold, imgThreshold,MORPH_OPEN, shapeOperateKernel);
    morphologyEx(imgThreshold, imgThreshold,MORPH_CLOSE, shapeOperateKernel);
    Canny(imgThreshold, imgThreshold, 50, 200, 3);

    //imwrite(directory + "canny.jpg", imgThreshold);

    //找出线条
    double deltaRho = 1;	//线条半径分辨率
    double deltaTheta = CV_PI/180;	//角度分辨率
    int minLinePoints = 50;	//直线上最小的点数
    double minLineLen = 100;	//最小线段长度
    double maxLineGap = 10;	//最大直线间隙

    vector<Vec4i> lines, resultLines;
    Point offset;
    if (rotation) {
        Mat roi(imgThreshold, Rect(0, source.rows / 2, source.cols, source.rows / 2));

        Size wholeSize;

        roi.locateROI(wholeSize, offset);

        HoughLinesP(roi, lines, deltaRho, deltaTheta, minLinePoints, minLineLen, maxLineGap);
    } else {
        Mat roi(imgThreshold, Rect(0, 0, source.cols * 0.8, source.rows));

        Size wholeSize;


        roi.locateROI(wholeSize, offset);

        HoughLinesP(roi, lines, deltaRho, deltaTheta, minLinePoints, minLineLen, maxLineGap);
    }



    //绘制线条
//    if (lines.size() > 0) {
//        for(size_t i=0; i<lines.size(); i++) {
//            Vec4i point = lines[i];
//            line(source, Point(point[0],point[1]), Point(point[2],point[3]), Scalar(255));
//        }
//
//        String name = directory + "line.jpg";
//        imwrite(name, source);
//
//        return lines;
//    }

    if (lines.size() > 0) {
        for (size_t i = 0; i < lines.size(); i ++) {
            Vec4i point = lines[i];
            point[0] = point[0] + offset.x;
            point[1] = point[1] + offset.y;
            point[2] = point[2] + offset.x;
            point[3] = point[3] + offset.y;
            resultLines.push_back(point);
        }
    }

    return resultLines;
}

vector<Vec4i> ImageHandler::DetectRoadLines(Mat source) {
    vector<Vec4i> result;

    Mat gray, dst, colorDst;

    cvtColor(source, gray, CV_BGR2GRAY);

    Canny(gray, dst, 50, 200, 3);
    cvtColor(dst, colorDst, CV_GRAY2BGR);

    vector<Vec4i> lines;
    double deltaRho = 1;	//线条半径分辨率
    double deltaTheta = CV_PI/180;	//角度分辨率
    int minLinePoints = 80;	//直线上最小的点数
    double minLineLen = 30;	//最小线段长度
    double maxLineGap = 5;	//最大直线间隙

    HoughLinesP(dst, lines, deltaRho, deltaTheta, minLinePoints, minLineLen, maxLineGap);

    Line* angleFilt = HoughLinkListCreate(lines);

    if (angleFilt == NULL) {
        return result;
    }

    angleFilt = AngleThresh(angleFilt, 75);
    angleFilt = LinkLengthSort(angleFilt);

    Line* pTemp = angleFilt;
    int lineNum = MIN(GetListLength(angleFilt), 10);
    Line* pLines = NULL;

    pLines = (Line*) malloc(lineNum * sizeof(Line));
    if (pLines == NULL) {
        FreeLink(angleFilt);
        return result;
    }

    memset(pLines, 0, (lineNum * sizeof(Line)));

    Point center;
    center = Point(source.cols / 1.8, source.rows / 1.5);
    circle(source, center, 4, Scalar(0, 0, 255), 5, 8, 0);

    for (int j=0; j< lineNum; j++)
    {
        pLines[j].p1 = pTemp->p1;
        pLines[j].p2 = pTemp->p2;

        if ((pTemp->angle < 180) || (180 == pTemp->angle))
        {
            pLines[j].angle = pTemp->angle;
        }
        else
        {
            pLines[j].angle = pTemp->angle - 180;
        }

        if(HalfPoint(pLines[j].p1, pLines[j].p2).x < center.x)
        {
            pLines[j].leftOrRight = 0;
        }
        else
        {
            pLines[j].leftOrRight = 1;
        }
        pTemp = pTemp->next;
    }

    Line scanLine;
    for (int i = 0; i < source.rows; i ++) {
        scanLine.p1 = Point(0, i);
        scanLine.p2 = Point(source.cols, i);

        FindRoadLine(&gray, &dst, &colorDst, center, pLines, scanLine, lineNum);
    }

    Line *maxLine1 = NULL, *maxLine2 = NULL;
    double valMax1 = 0, valMax2 = 0;

    for (int j=0; j< lineNum; j++)
    {
        if ((pLines[j].roadVal > valMax1) && (0 == pLines[j].leftOrRight))
        {
            maxLine1 = &pLines[j];
            valMax1 = pLines[j].roadVal;
        }

        if ((pLines[j].roadVal > valMax2) && (1 == pLines[j].leftOrRight))
        {
            maxLine2 = &pLines[j];
            valMax2 = pLines[j].roadVal;
        }
    }

    Line line1, line2, line3;
    line1.p1 = Point(0, 0);
    line1.p2 = Point(0, source.rows);
    line2.p1 = Point(0, 0);
    line2.p2 = Point(source.cols, 0);
    line3.p1 = Point(source.cols, 0);
    line3.p2 = Point(source.cols, source.rows);

    CrossPoint point1, point2;

    if (maxLine1 != NULL) {
        if (GetCrossPoint(*maxLine1,line1,point1)
                && GetCrossPoint(*maxLine1,line2,point2)) {
            line(source, Point(point1.x, point1.y), Point(point2.x, point2.y), Scalar(0, 0, 255), 5);
        }
    }

    if (maxLine2 != NULL) {
        if (GetCrossPoint(*maxLine2,line3,point1)
                && GetCrossPoint(*maxLine2,line2,point2)) {
            line(source, Point(point1.x, point1.y), Point(point2.x, point2.y), Scalar(0, 0, 255), 5);
        }
    }

//    imwrite(directory + "/line.jpg", source);
//    imwrite(directory + "/canny.jpg", colorDst);
    free(pLines);
    FreeLink(angleFilt);


    if (maxLine1 != NULL) {
        Vec4i line;
        line[0] = maxLine1->p1.x;
        line[1] = maxLine1->p1.y;
        line[2] = maxLine1->p2.x;
        line[3] = maxLine1->p2.y;
        result.push_back(line);
    }

    if (maxLine2 != NULL) {
        Vec4i line;
        line[0] = maxLine2->p1.x;
        line[1] = maxLine2->p1.y;
        line[2] = maxLine2->p2.x;
        line[3] = maxLine2->p2.y;
        result.push_back(line);
    }

    return result;
}




