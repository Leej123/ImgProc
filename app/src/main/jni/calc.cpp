//
// Created by Administrator on 2017/7/26 0026.
//

#include "calc.h"

/**
 * 创建链表
 * @param lines
 * @return
 */
Line* HoughLinkListCreate(vector<Vec4i> lines) {
    Line *head, *p, *s;
    p = NULL;
    head = NULL;
    for (size_t i = 0; i < lines.size(); i ++) {
        Vec4i point = lines[i];

        s = (Line*) malloc(sizeof(Line));
        if (s == NULL) {
            if (head != NULL) {
                FreeLink(head);
            }
            return NULL;
        }

        if (point[1] > point[3]) {
            s->p1 = Point(point[0], point[1]);
            s->p2 = Point(point[2], point[3]);
        } else {
            s->p1 = Point(point[2], point[3]);
            s->p2 = Point(point[0], point[1]);
        }

        s->angle = GetAngle(s->p1, s->p2);
        s->length = GetDistance(s->p1, s->p2);
        if (p != NULL) {
            p->next = s;
        }
        p = s;
        if (head == NULL) {
            head = p;
        }
    }
    if (p != NULL)
        p->next = NULL;
    return head;
}

Line* AngleThresh(Line* head, double angleThresh) {
    if (NULL == head)
    {
        return NULL;
    }

    if (NULL == head->next)
    {
        if (true == AngleOk(head->angle, angleThresh))
        {
        }
        else
        {
            head=DeleteNode(head,head);           //删除头节点
            return NULL;
        }
        return head;
    }

    Line * p = head;
    Line pBak = *head;


    while(NULL != p)
    {

        if ((int )p->angle == 59)
        {
            int k = 0;
        }

        if (true == AngleOk(p->angle,angleThresh))
        {
            p = p->next;
        }
        else
        {
            pBak = * p;

            head = DeleteNode(head,p);      //删除p节点,返回剩下的链表
            p = pBak.next;
        }
    }

    return head;
}

/**
 * 链表排序，按偏移向量的长度排序
 * @param head
 * @return
 */
Line* LinkLengthSort(Line* st) {
    if(NULL == st)
    {
        return NULL;
    }

    Line * h=NULL , *t=NULL, *maxp=NULL, *head=NULL, *end=NULL;
    double maxn;
    h=st;                                       // h ：动态头指针

    while(h !=NULL)                             //只要当前链中不为空就循环
    {
        t=h;                                    // t ：临时指针
        maxn=t->length; maxp=t;                  //把当前t中的值作为最大
        while (t->next !=NULL)                  //只要t后面还有节点就循环
        {
            t=t->next ;		    	            //t往后移动一个
            if (t->length> maxn)                 //如果t中的值大于maxn，则记下其值和位置
            {
                maxn=t->length;
                maxp=t;
            }
        }                                       //找出当前头开始在链中最大节点 maxp

        h=DeleteNodeForSort(h,maxp);               //删除maxp节点,返回剩下的链表
        maxp->next=NULL;			            //maxp  的下节点设为空
        if (head==NULL) {head=maxp;end=maxp;}   //第一个就放入头中，并记下尾部位置
        else {end->next=maxp;end=end->next;}    //非第一个就把maxp接在尾部
    }                                           //继续循环

    return head;
}

bool AngleOk(double angle, double angleThresh) {
    bool flag = false;
    if (angle == 180)
        return false;

    if (angle < 180) {
        if (fabsf(angle - 90) < angleThresh) {
            flag = true;
        } else {
            flag = false;
        }
    } else {
        if (fabsf(angle - 270) < angleThresh) {
            flag = true;
        } else {
            flag = false;
        }
    }

    return flag;
}

bool GetCrossPoint(Line &line1, Line &line2, CrossPoint &point) {
    GetLinePara(line1);
    GetLinePara(line2);

    double d=line1.a*line2.b-line2.a*line1.b;

    if (0 == d)
    {
        return false;
    }

    point.x=(line1.b*line2.c-line2.b*line1.c)/d;
    point.y=(line1.c*line2.a-line2.c*line1.a)/d;

    return true;
}

/**
 * 删除一个节点，返回剩下的链表首地址
 * @return
 */
Line* DeleteNode(Line* head, Line* node) {
    Line * t;
    if (head == node)                         //如果maxp 等于 头
    {
        t= node->next;					 //就把下一个返回
        free(node);
        node = NULL;
        return t;
    }
    else								//否则
    {
        t = head;
        while(t->next != node ) {t = t->next;}   //找到maxp 的前节点t
        t->next = node->next ;                //删除maxp，maxp后面的接到t后面
        free(node);
        node = NULL;
        return head;							  //链首依然是h ,返回
    }
}

/**
 * 删除一个节点，返回剩下的链表首地址
 * @param head
 * @param node
 * @return
 */
Line* DeleteNodeForSort(Line* head, Line* node) {
    Line * t;

    if (head==node)                                //如果maxp 等于 头，
    {
        t= node-> next;					        //就把下一个返回
        node-> next=NULL;
        return t;
    }
    else								        //否则
    {   t=head;
        while(t->next!=node ) {t=t->next;}      //找到maxp 的前节点t
        t->next = node->next ;                  //删除maxp，maxp后面的接到t后面
        node->next = NULL;
        return head;							    //链首依然是h ,返回
    }
}

int GetListLength(Line* head) {
    int n = 0;
    Line* node;
    node = head;
    while (node != NULL) {
        node = node->next;
        n ++;
    }
    return n;
}

Point HalfPoint(Point point1, Point point2) {
    Point point;
    point.x = (point1.x + point2.x) / 2;
    point.y = (point1.y + point2.y) / 2;
    return point;
}

bool FindRoadLine(Mat* gray, Mat* canny, Mat* cannyColor, Point center, Line* pLines, Line scanLine, int num) {
    int lineNum = 0;
    CrossPoint * pPoints = NULL;
    pPoints = (CrossPoint *)malloc(num * sizeof(CrossPoint));
    if (pPoints == NULL) {
        return false;
    }
    memset(pPoints,0,(num * sizeof(CrossPoint)));
    double sad = CalculateBlock(gray, center,center,3);

    CrossPoint * temp = pPoints;
    for (int j=0; j< num; j++)
    {
        if (true == GetCrossPoint(pLines[j],scanLine,*temp))
        {
            int maxP = MAX(pLines[j].p1.x,pLines[j].p2.x);
            int minP = MIN(pLines[j].p1.x,pLines[j].p2.x);

            if ((temp->x < maxP)
                && (temp->x > minP))
            {
                temp->line = &pLines[j];
                circle(*cannyColor, Point(temp->x, temp->y), 4, Scalar(0, 0, 255), 2, 8, 0);
                lineNum ++;
                temp++;
            }

        }
    }

    PointQuickSort(pPoints,0,(lineNum-1));

    Line *maxLine1 = NULL,*maxLine2 = NULL;
    double sadMax=0,MaxSad = 0;
    maxLine1 = pPoints[0].line;

    for (int j=0; j< lineNum; j++)
    {
        sad = CalculateBlock(gray, center,cvPoint(pPoints[j].x,pPoints[j].y),3);
        pPoints[j].sad = sad;

        if (sad > sadMax)
        {
            maxLine1 = pPoints[j].line;
            sadMax = sad;
        }
    }

    MaxSad = 0;
    maxLine2 = maxLine1;

    for (int j=0; j< lineNum; j++)
    {
        if ((pPoints[j].sad > MaxSad) && (pPoints[j].sad != sadMax))
        {
            maxLine2 = pPoints[j].line;
            MaxSad = sad;
        }
    }

    if (NULL != maxLine1)
    {
        maxLine1->roadVal ++;
    }

    if (NULL != maxLine2)
    {
        maxLine2->roadVal ++;
    }

    free(pPoints);
    return true;
}

/************************************************************************
*函数名：        GetAngle
*
*函数作用：      已知2个坐标点，求从 0------->x 逆时针需旋转多少角度到该位置
*
*					|
*					|
*					|
*					|
*------------------------------------> x
*					| 0
*					|
*					|
*					|
*                   v
*					y
*
*函数参数：
*CvPoint2D32f point1  - 起点
*CvPoint2D32f point2  - 终点
*
*函数返回值：
*double         向量OA，从 0------->x 逆时针需旋转多少角度到该位置
**************************************************************************/
double GetAngle(Point pointO, Point pointA) {
    double angle = 0;
    CvPoint point;
    double temp;

    point = cvPoint((pointA.x - pointO.x), (pointA.y - pointO.y));//pointAdd(pointA,pointMultiply(pointO,-1));

    if ((0==point.x) && (0==point.y))
    {
        return 0;
    }

    if (0==point.x)
    {
        angle = 90;
        return angle;
    }

    if (0==point.y)
    {
        angle = 0;
        return angle;
    }

    temp = fabsf(float(point.y)/float(point.x));
    temp = atan(temp);
    temp = temp*180/CV_PI ;

    if ((0<point.x)&&(0<point.y))
    {
        angle = 360 - temp;
        return angle;
    }

    if ((0>point.x)&&(0<point.y))
    {
        angle = 360 - (180 - temp);
        return angle;
    }

    if ((0<point.x)&&(0>point.y))
    {
        angle = temp;
        return angle;
    }

    if ((0>point.x)&&(0>point.y))
    {
        angle = 180 - temp;
        return angle;
    }
    return -1;
}

/**
 *
 * @param gray
 * @param point1
 * @param point2
 * @param size 用奇数
 * @return
 */
double CalculateBlock(Mat* gray, Point point1, Point point2, int size) {
    double sum = 0;
//    CvScalar temp1;
//    CvScalar temp2;
//    Vec3i temp1;
//    Vec3i temp2;

    int m = point1.x - size / 2;
    int n = point1.y - size / 2;
    int m2 = point2.x - size / 2;
    int n2 = point2.y - size / 2;

    int t1, t2;
    uchar* data = gray->data;
    for (int i = 0; i < size; i ++) {
        for (int j = 0; j < size; j ++) {
//            temp1 = cvGet2D(gray, (n + j), (m + i));
//            temp2 = cvGet2D(gray, (n2 + j), (m2 + i));
//            temp1 = gray->at<Vec3b>(n + j, m + i);
//            temp2 = gray->at<Vec3b>(n2 + j, m2 + i);

            t1 = data[(n + j) * gray->cols + (m + i)];
            t2 = data[(n2 + j) * gray->cols + (m2 + i)];

//            sum = sum + powf(temp1.val[0] - temp2.val[0], 2);
            sum = sum + powf(t1 - t2, 2);
        }
    }

    sum = 1.0 * sum / size * size;
    sum = sqrtf(sum);
    return sum;
}

/************************************************************************
*函数名：        getDistance
*
*函数作用：      获取两点之间的距离
*
*函数参数：
*CvPoint2D32f point1  - 起点
*CvPoint2D32f point2  - 终点
*
*函数返回值：
*double           两点之间的距离
**************************************************************************/
double GetDistance(Point point1, Point point2) {
    double distance;
    distance = powf((point1.x - point2.x), 2) + powf((point1.y - point2.y), 2);
    return sqrtf(distance);
}

/**
 * 释放链表内存
 * @param head
 */
void FreeLink(Line* head) {
    Line* next;
    Line* cur;
    cur = head;
    while (cur->next != NULL) {
        next = cur->next;
        free(cur);
        cur = next;
    }

    if (cur != NULL) {
        free(cur);
    }

    cur = NULL;
    next = NULL;
    head = NULL;
}

void GetLinePara(Line &line) {
    line.a=line.p1.y-line.p2.y;
    line.b=line.p2.x-line.p1.x;
    line.c=line.p1.x*line.p2.y-line.p1.y*line.p2.x;
}

int ArrayPoint(CrossPoint pair[], int p, int r) {
    int e=rand()%(r-p+1)+p;
    CrossPoint tem;
    tem=pair[e];
    pair[e]=pair[r];
    pair[r]=tem;
    double x=pair[r].x;
    int i=p-1;

    for (int j=p;j<r;j++)
    {
        if (pair[j].x <= x)
        {
            tem=pair[i+1];
            pair[i+1]=pair[j];
            pair[j]=tem;
            i++;
        }
    }

    tem=pair[r];
    pair[r]=pair[i+1];
    pair[i+1]=tem;

    return i+1;
}

void PointQuickSort(CrossPoint pair[], int p, int r) {
    if (p<r)
    {
        int q=ArrayPoint(pair,p,r);
        PointQuickSort(pair,p,q-1);
        PointQuickSort(pair,q+1,r);
    }
}

