package com.lx.opencvuilts;

import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * 智能选区帮助类
 */

public class ScannerUtils {

    /**
     * 扫描图片，并返回检测到的矩形的四个顶点的坐标
     */
    public static Point[] scanPoint(Mat srcMat) {

        //图像预处理
        Mat scanImage = preProcessImage(srcMat);

        //获取最大矩形
        Point[] resultArr = null;

        //检测到的轮廓
        List<MatOfPoint> contours = new ArrayList<>();

        //提取边框
        Imgproc.findContours(scanImage, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);

        //按面积排序，最后只取面积最大的那个
        Collections.sort(contours, new Comparator<MatOfPoint>() {
            @Override
            public int compare(MatOfPoint matOfPoint1, MatOfPoint matOfPoint2) {
                double oneArea = Math.abs(Imgproc.contourArea(matOfPoint1));
                double twoArea = Math.abs(Imgproc.contourArea(matOfPoint2));
                return Double.compare(twoArea, oneArea);
            }
        });

        if (contours.size() > 0) {
            //  取面积最大的
            MatOfPoint2f contour2f = new MatOfPoint2f(contours.get(0).toArray());
            double arc = Imgproc.arcLength(contour2f, true);

            MatOfPoint2f outDpMat = new MatOfPoint2f();
            Imgproc.approxPolyDP(contour2f, outDpMat, 0.02 * arc, true);//  多边形逼近
            //  筛选去除相近的点
            MatOfPoint2f selectMat = selectPoint(outDpMat, 1);

            double area = Math.abs(Imgproc.contourArea(selectMat));

            Log.d("扫描", area + "");

            if (area > 11000) {
                if (selectMat.toArray().length == 4) {
                    resultArr = selectMat.toArray();
                }
            }
        }
        //  对最终检测出的四个点进行排序：左上、右上、右下、左下
        if (resultArr != null) {
            Point[] result = sortPointClockwise(resultArr);
            Point[] rs = new Point[result.length];
            for (int i = 0; i < result.length; i++) {
                Point p = new Point((int) result[i].x, (int) result[i].y);
                rs[i] = p;
            }
            return rs;
        } else {
            return null;
        }
    }

    /**
     * 对图像进行预处理：灰度化、高斯模糊、Canny边缘检测
     */
    public static Mat preProcessImage(Mat image) {

        Mat grayMat = new Mat();
        Imgproc.cvtColor(image, grayMat, Imgproc.COLOR_RGB2GRAY);   //  注意RGB和BGR，影响很大

        Mat blurMat = new Mat();
        Imgproc.GaussianBlur(grayMat, blurMat, new Size(5, 5), 0);

        Mat cannyMat = new Mat();
        Imgproc.Canny(blurMat, cannyMat, 0, 5);

        Mat thresholdMat = new Mat();
        Imgproc.threshold(cannyMat, thresholdMat, 0, 255, Imgproc.THRESH_OTSU);

        return thresholdMat;
    }

    /**
     * 过滤掉距离相近的点
     */
    private static MatOfPoint2f selectPoint(MatOfPoint2f outDpMat, int selectTimes) {
        List<Point> pointList = new ArrayList<>();
        pointList.addAll(outDpMat.toList());
        if (pointList.size() > 4) {
            double arc = Imgproc.arcLength(outDpMat, true);
            for (int i = pointList.size() - 1; i >= 0; i--) {
                if (pointList.size() == 4) {
                    Point[] resultPoints = new Point[pointList.size()];
                    for (int j = 0; j < pointList.size(); j++) {
                        resultPoints[j] = pointList.get(j);
                    }
                    return new MatOfPoint2f(resultPoints);
                }

                if (i != pointList.size() - 1) {
                    Point itor = pointList.get(i);
                    Point lastP = pointList.get(i + 1);

                    double pointLength = Math.sqrt(Math.pow(itor.x - lastP.x, 2) + Math.pow(itor.y - lastP.y, 2));
                    if (pointLength < arc * 0.01 * selectTimes && pointList.size() > 4) {
                        pointList.remove(i);
                    }
                }
            }

            if (pointList.size() > 4) {
                //  要手动逐个强转
                Point[] againPoints = new Point[pointList.size()];
                for (int i = 0; i < pointList.size(); i++) {
                    againPoints[i] = pointList.get(i);
                }
                return selectPoint(new MatOfPoint2f(againPoints), selectTimes + 1);
            }
        }

        return outDpMat;
    }

    /**
     * 对顶点进行排序
     */
    private static Point[] sortPointClockwise(Point[] points) {
        if (points.length != 4) {
            return points;
        }

        Point unFoundPoint = new Point();
        Point[] result = {unFoundPoint, unFoundPoint, unFoundPoint, unFoundPoint};

        long minDistance = -1;
        for (Point point : points) {
            long distance = (long) (point.x * point.x + point.y * point.y);
            if (minDistance == -1 || distance < minDistance) {
                result[0] = point;
                minDistance = distance;
            }
        }

        if (result[0] != unFoundPoint) {
            Point leftTop = result[0];
            Point[] p1 = new Point[3];
            int i = 0;
            for (Point point : points) {
                if (point.x == leftTop.x && point.y == leftTop.y) {
                    continue;
                }
                p1[i] = point;
                i++;
            }
            if ((pointSideLine(leftTop, p1[0], p1[1]) * pointSideLine(leftTop, p1[0], p1[2])) < 0) {
                result[2] = p1[0];
            } else if ((pointSideLine(leftTop, p1[1], p1[0]) * pointSideLine(leftTop, p1[1], p1[2])) < 0) {
                result[2] = p1[1];
            } else if ((pointSideLine(leftTop, p1[2], p1[0]) * pointSideLine(leftTop, p1[2], p1[1])) < 0) {
                result[2] = p1[2];
            }
        }

        if (result[0] != unFoundPoint && result[2] != unFoundPoint) {
            Point leftTop = result[0];
            Point rightBottom = result[2];
            Point[] p1 = new Point[2];
            int i = 0;
            for (Point point : points) {
                if (point.x == leftTop.x && point.y == leftTop.y) {
                    continue;
                }
                if (point.x == rightBottom.x && point.y == rightBottom.y) {
                    continue;
                }
                p1[i] = point;
                i++;
            }
            if (pointSideLine(leftTop, rightBottom, p1[0]) > 0) {
                result[1] = p1[0];
                result[3] = p1[1];
            } else {
                result[1] = p1[1];
                result[3] = p1[0];
            }
        }

        if (result[0] != unFoundPoint && result[1] != unFoundPoint && result[2] != unFoundPoint
                && result[3] != unFoundPoint) {
            return result;
        }

        return points;
    }


    private static double pointSideLine(Point lineP1, Point lineP2, Point point) {
        double x1 = lineP1.x;
        double y1 = lineP1.y;
        double x2 = lineP2.x;
        double y2 = lineP2.y;
        double x = point.x;
        double y = point.y;
        return (x - x1) * (y2 - y1) - (y - y1) * (x2 - x1);
    }
}
