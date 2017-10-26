//
// Created by Administrator on 2017/6/14 0014.
//
#include <memory>
#include <ctime>
#include <iostream>
#include <fstream>
using namespace std;

#ifndef IMGPROC_RECORDLOG_H
#define IMGPROC_RECORDLOG_H

class RecordLog
{
public:
    static RecordLog* Inst(string dir) {
        RecordLog::dir = dir;
        if (m_pInstance == NULL)
        {
            pthread_mutex_lock(&s_mutex);
            if (m_pInstance == NULL)
            {
                m_pInstance = new RecordLog();
            }
            pthread_mutex_unlock(&s_mutex);
        }
        ++ s_counter;
        return m_pInstance;
    }

    void ReleaseInstance(void)
    {
        if (s_counter)
        {
            pthread_mutex_lock(&s_mutex);
            if (s_counter && --s_counter == 0)
            {
                delete this;
            }
            pthread_mutex_unlock(&s_mutex);
        }
    }

    void Log(bool, string msg);
private:
    RecordLog(void) {}
    ~RecordLog(void)
    {
        m_pInstance = NULL;
    }
    static RecordLog* m_pInstance;
    static pthread_mutex_t s_mutex;
    static size_t s_counter;
    static string dir;
};

RecordLog* RecordLog::m_pInstance = NULL;
pthread_mutex_t RecordLog::s_mutex = PTHREAD_MUTEX_INITIALIZER;
size_t RecordLog::s_counter = 0;
string RecordLog::dir = "";

void RecordLog::Log(bool valid, string msg)
{
    string filename = "valid.log";
    if (!valid)
    {
        filename = "none.log";
    }
    string path = RecordLog::dir + filename;
    ofstream ofs;
    time_t t = time(0);
    char tmp[64];
    strftime(tmp, sizeof(tmp), "\t[%Y.%m.%d %X %A]", localtime(&t));
    ofs.open(path, ofstream::app);
    ofs.write(msg.c_str(), msg.size());
    ofs << tmp << '\n';
    ofs.close();
}

#endif //IMGPROC_RECORDLOG_H
