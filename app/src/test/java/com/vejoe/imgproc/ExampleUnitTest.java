package com.vejoe.imgproc;

import com.vejoe.utils.Tools;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        String str = String.format("%02d:%02d:%02d", 23, 45, 1);
        System.out.print(str);
    }

    @Test
    public void timeFormat() {
        long time = System.currentTimeMillis();
        String str = Tools.getDateTimeFromMillisecond(time);
        System.out.println(str);

        float length = 100.123f;
        float seconds = 100.123f;
        str = String.format("%.1f（厘米）持续时间：%.1f（秒）", length, seconds);
        System.out.println(str);
    }

    @Test
    public void testPhoneNumber() {
        //"[1]"代表第1位为数字1，"[358]"代表第二位可以为3、5、8中的一个，"\\d{9}"代表后面是可以是0～9的数字，有9位。
        String telRegex = "[1][34578]\\d{9}" ;

        String phoneNumber = "13202144567";
        boolean match = phoneNumber.matches(telRegex);
        System.out.println("match:" + match);
    }
}